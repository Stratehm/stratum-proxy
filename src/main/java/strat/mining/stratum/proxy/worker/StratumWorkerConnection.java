/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
 */
package strat.mining.stratum.proxy.worker;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.ClientGetVersionRequest;
import strat.mining.stratum.proxy.json.ClientGetVersionResponse;
import strat.mining.stratum.proxy.json.ClientReconnectNotification;
import strat.mining.stratum.proxy.json.JsonRpcError;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningAuthorizeResponse;
import strat.mining.stratum.proxy.json.MiningExtranonceSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningExtranonceSubscribeResponse;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSetExtranonceNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.network.StratumConnection;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.HashrateUtils;

public class StratumWorkerConnection extends StratumConnection implements WorkerConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerConnection.class);

	private Pool pool;

	private StratumProxyManager manager;

	private Timer subscribeTimeoutTimer;
	private Integer subscribeReceiveTimeout = Constants.DEFAULT_SUBSCRIBE_RECEIVE_TIMEOUT;

	private Date isActiveSince;

	// The tail is the salt which is added to extranonce1 and which is unique by
	// connection.
	private String extranonce1Tail;
	private Integer extranonce2Size;

	private Set<String> authorizedWorkers;

	private boolean isSetExtranonceNotificationSupported = false;

	private Deque<Share> lastAcceptedShares;
	private Deque<Share> lastRejectedShares;
	private Integer samplingHashesPeriod = Constants.DEFAULT_WORKER_CONNECTION_HASHRATE_SAMPLING_PERIOD * 1000;

	public StratumWorkerConnection(Socket socket, StratumProxyManager manager) {
		super(socket);
		this.manager = manager;
		this.authorizedWorkers = Collections.synchronizedSet(new HashSet<String>());
		lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		lastRejectedShares = new ConcurrentLinkedDeque<Share>();
	}

	@Override
	public void startReading() {
		super.startReading();
		subscribeTimeoutTimer = new Timer();
		subscribeTimeoutTimer.schedule(new TimerTask() {
			public void run() {
				LOGGER.warn("No subscribe request received from {} in {} ms. Closing connection.", getConnectionName(), subscribeReceiveTimeout);
				// Close the connection if subscribe request is not received at
				// time.
				close();
			}
		}, subscribeReceiveTimeout);
	}

	@Override
	protected void onParsingError(String line, Throwable throwable) {
		LOGGER.error("Parsing error on worker connection {}. Failed to parse line {}.", getConnectionName(), line, throwable);
	}

	@Override
	protected void onDisconnectWithError(Throwable cause) {
		manager.onWorkerDisconnection(this, cause);
	}

	@Override
	protected void onNotify(MiningNotifyNotification notify) {
		// Do nothing, should never happen
	}

	@Override
	protected void onSetDifficulty(MiningSetDifficultyNotification setDifficulty) {
		// Do nothing, should never happen
	}

	@Override
	protected void onSetExtranonce(MiningSetExtranonceNotification setExtranonce) {
		// Do nothing, should never happen
	}

	@Override
	protected void onClientReconnect(ClientReconnectNotification clientReconnect) {
		// Do nothing, should never happen
	}

	@Override
	protected void onAuthorizeRequest(MiningAuthorizeRequest request) {
		MiningAuthorizeResponse response = new MiningAuthorizeResponse();
		response.setId(request.getId());

		try {
			// Throws an exception if the worker is not authorized
			manager.onAuthorizeRequest(this, request);
			response.setIsAuthorized(true);
			authorizedWorkers.add(request.getUsername());
		} catch (AuthorizationException e) {
			response.setIsAuthorized(false);
			JsonRpcError error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNAUTHORIZED_WORKER.getCode());
			error.setMessage("The worker is not authorized. " + e.getMessage());
			response.setErrorRpc(error);
			LOGGER.warn("User connection not authorized. {}", e.getMessage());
		}

		sendResponse(response);
	}

	@Override
	protected void onSubscribeRequest(MiningSubscribeRequest request) {
		// Once the subscribe request is received, cancel the timeout timer.
		if (subscribeTimeoutTimer != null) {
			subscribeTimeoutTimer.cancel();
		}

		JsonRpcError error = null;
		try {
			pool = manager.onSubscribeRequest(this, request);
		} catch (NoPoolAvailableException e) {
			LOGGER.error("No pool available for the connection {}. Sending error and close the connection.", getConnectionName());
			error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNKNOWN.getCode());
			error.setMessage("No pool available on this proxy.");
		}

		if (error == null) {
			try {
				extranonce1Tail = pool.getFreeTail();
				extranonce2Size = pool.getWorkerExtranonce2Size();
			} catch (TooManyWorkersException e) {
				LOGGER.error("Too many connections on pool {} for the connection {}. Sending error and close the connection.", pool.getName(),
						getConnectionName(), e);
				error = new JsonRpcError();
				error.setCode(JsonRpcError.ErrorCode.UNKNOWN.getCode());
				error.setMessage("Too many connection on the pool.");
			}
		}

		// Send the subscribe response
		MiningSubscribeResponse response = new MiningSubscribeResponse();
		response.setId(request.getId());
		if (error != null) {
			response.setErrorRpc(error);
		} else {
			response.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
			response.setExtranonce2Size(extranonce2Size);
			response.setSubscriptionDetails(getSubscibtionDetails());
			isActiveSince = new Date();
		}

		sendResponse(response);

		// If the subscribe succeed, send the initial notifications (difficulty
		// and notify).
		if (error == null) {
			sendInitialNotifications();
		}
	}

	@Override
	protected void onSubmitRequest(MiningSubmitRequest request) {
		MiningSubmitResponse response = new MiningSubmitResponse();
		response.setId(request.getId());
		JsonRpcError error = null;
		if (authorizedWorkers.contains(request.getWorkerName())) {
			// Modify the request to add the tail of extranonce1 to the
			// submitted extranonce2
			request.setExtranonce2(extranonce1Tail + request.getExtranonce2());
			manager.onSubmitRequest(this, request);
		} else {
			error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNAUTHORIZED_WORKER.getCode());
			error.setMessage("Submit failed. Worker not authorized on this connection.");
			response.setErrorRpc(error);
			sendResponse(response);
		}
	}

	@Override
	protected void onExtranonceSubscribeRequest(MiningExtranonceSubscribeRequest request) {
		this.isSetExtranonceNotificationSupported = true;
	}

	@Override
	protected void onGetVersionRequest(ClientGetVersionRequest request) {
		LOGGER.warn("Worker {} send a GetVersion request. This should not happen.", getConnectionName());
	}

	/**
	 * Called when the pool has answered to a submit request.
	 * 
	 * @param workerRequest
	 * @param poolResponse
	 */
	public void onPoolSubmitResponse(MiningSubmitRequest workerRequest, MiningSubmitResponse poolResponse) {
		if (poolResponse.getIsAccepted() != null && poolResponse.getIsAccepted()) {
			LOGGER.info("Accepted share (diff: {}) from {}@{} on {}. Yeah !!!!", pool != null ? pool.getDifficulty() : "Unknown",
					workerRequest.getWorkerName(), getConnectionName(), pool.getName());
		} else {
			LOGGER.info("REJECTED share (diff: {}) from {}@{} on {}. Booo !!!!. Error: {}", pool != null ? pool.getDifficulty() : "Unknown",
					workerRequest.getWorkerName(), getConnectionName(), pool.getName(), poolResponse.getJsonError());
		}

		MiningSubmitResponse workerResponse = new MiningSubmitResponse();
		workerResponse.setId(workerRequest.getId());
		workerResponse.setIsAccepted(poolResponse.getIsAccepted());
		workerResponse.setError(poolResponse.getError());

		sendResponse(workerResponse);
	}

	/**
	 * Called when the pool change its extranonce. Send the extranonce change to
	 * the worker. Throw an exception if the extranonce change is not supported
	 * on the fly.
	 */
	public void onPoolExtranonceChange() throws ChangeExtranonceNotSupportedException {
		if (isSetExtranonceNotificationSupported) {
			MiningSetExtranonceNotification extranonceNotif = new MiningSetExtranonceNotification();
			extranonceNotif.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
			extranonceNotif.setExtranonce2Size(extranonce2Size);
			sendNotification(extranonceNotif);
		} else {
			// If the extranonce change is not supported by the worker, then
			// throw an exception
			throw new ChangeExtranonceNotSupportedException("Change extranonce not supported.");
		}
	}

	@Override
	protected void onAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		LOGGER.warn("Worker {} send an Authorize response. This should not happen.", getConnectionName());
	}

	@Override
	protected void onSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response) {
		LOGGER.warn("Worker {} send a Subscribe response. This should not happen.", getConnectionName());
	}

	@Override
	protected void onSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
		LOGGER.warn("Worker {} send a Submit response. This should not happen.", getConnectionName());
	}

	@Override
	protected void onExtranonceSubscribeResponse(MiningExtranonceSubscribeRequest request, MiningExtranonceSubscribeResponse response) {
		LOGGER.warn("Worker {} send an Extranonce subscribe response. This should not happen.", getConnectionName());
	}

	@Override
	protected void onGetVersionResponse(ClientGetVersionRequest request, ClientGetVersionResponse response) {
		// Nothing to do...yet.
	}

	/**
	 * Build a list of subscription details.
	 * 
	 * @return
	 */
	private List<Object> getSubscibtionDetails() {
		List<Object> details = new ArrayList<Object>();
		List<Object> setDifficultySubscribe = new ArrayList<>();
		setDifficultySubscribe.add(MiningSetDifficultyNotification.METHOD_NAME);
		setDifficultySubscribe.add("b4b6693b72a50c7116db18d6497cac52");
		details.add(setDifficultySubscribe);
		List<Object> notifySubscribe = new ArrayList<Object>();
		notifySubscribe.add(MiningNotifyNotification.METHOD_NAME);
		notifySubscribe.add("ae6812eb4cd7735a302a8a9dd95cf71f");
		details.add(notifySubscribe);
		return details;
	}

	/**
	 * Return the pool on which this connection is bound.
	 * 
	 * @return
	 */
	public Pool getPool() {
		return pool;
	}

	/**
	 * Send the first notifications to the worker. The setDifficulty and the
	 * current job.
	 */
	private void sendInitialNotifications() {
		// Send the setExtranonce notif
		if (isSetExtranonceNotificationSupported) {
			MiningSetExtranonceNotification extranonceNotif = new MiningSetExtranonceNotification();
			extranonceNotif.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
			extranonceNotif.setExtranonce2Size(extranonce2Size);
			sendNotification(extranonceNotif);
			LOGGER.debug("Initial extranonce sent to {}.", getConnectionName());
		}

		// Send the difficulty if available
		Double difficulty = pool.getDifficulty();
		if (difficulty != null) {
			MiningSetDifficultyNotification setDifficulty = new MiningSetDifficultyNotification();
			setDifficulty.setDifficulty(difficulty);
			sendNotification(setDifficulty);
			LOGGER.debug("Initial difficulty sent to {}.", getConnectionName());
		}

		// Then send the first job if available.
		MiningNotifyNotification notify = pool.getCurrentJob();
		if (notify != null) {
			sendNotification(notify);
			LOGGER.debug("Initial job sent to {}.", getConnectionName());
		}

	}

	/**
	 * Reset the connection with the parameters of the new pool. May close the
	 * connection if setExtranonce is not supported.
	 * 
	 * @param newPool
	 * @throws TooManyWorkersException
	 * @throws ChangeExtranonceNotSupportedException
	 */
	public void rebindToPool(Pool newPool) throws TooManyWorkersException, ChangeExtranonceNotSupportedException {
		if (isSetExtranonceNotificationSupported) {
			LOGGER.info("Rebind connection {} from pool {} to pool {} with setExtranonce notification.", getConnectionName(), pool.getName(),
					newPool.getName());
			// Release the old extranonce
			pool.releaseTail(extranonce1Tail);

			// Then retrieve a free tail from the new pool.
			extranonce1Tail = newPool.getFreeTail();
			extranonce2Size = newPool.getWorkerExtranonce2Size();
			pool = newPool;

			// Finally, send all notifications to the worker
			sendInitialNotifications();

		} else {
			// If set extranonce not supported, throw an exception
			throw new ChangeExtranonceNotSupportedException("Change extranonce not supported.");
		}
	}

	@Override
	public void close() {
		super.close();
		if (pool != null) {
			pool.releaseTail(extranonce1Tail);
		}
	}

	/**
	 * Return a read-only set of users that are authorized on this connection.
	 * 
	 * @return
	 */
	public Set<String> getAuthorizedWorkers() {
		return Collections.unmodifiableSet(authorizedWorkers);
	}

	@Override
	public double getAcceptedHashrate() {
		HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastAcceptedShares, samplingHashesPeriod);
	}

	@Override
	public double getRejectedHashrate() {
		HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastRejectedShares, samplingHashesPeriod);
	}

	/**
	 * Update the shares lists with the given share to compute hashrate
	 * 
	 * @param share
	 * @param isAccepted
	 */
	public void updateShareLists(Share share, boolean isAccepted) {
		if (isAccepted) {
			lastAcceptedShares.addLast(share);
			HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		} else {
			lastRejectedShares.addLast(share);
			HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		}
	}

	@Override
	public void setSamplingHashesPeriod(Integer samplingHashesPeriod) {
		this.samplingHashesPeriod = samplingHashesPeriod * 1000;
	}

	public Date getActiveSince() {
		return isActiveSince;
	}

	public boolean isSetExtranonceNotificationSupported() {
		return isSetExtranonceNotificationSupported;
	}

	@Override
	public void onPoolDifficultyChanged(MiningSetDifficultyNotification notification) {
		sendNotification(notification);
	}

	@Override
	public void onPoolNotify(MiningNotifyNotification notification) {
		sendNotification(notification);
	}

}
