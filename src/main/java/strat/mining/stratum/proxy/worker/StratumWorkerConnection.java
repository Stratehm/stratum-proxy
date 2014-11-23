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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
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
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.network.StratumConnection;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.utils.Timer.Task;
import strat.mining.stratum.proxy.utils.mining.DifficultyUtils;
import strat.mining.stratum.proxy.utils.mining.WorkerConnectionHashrateDelegator;

public class StratumWorkerConnection extends StratumConnection implements WorkerConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerConnection.class);

	private Pool pool;

	private ProxyManager manager;

	private Task subscribeTimeoutTask;
	private Integer subscribeReceiveTimeout = Constants.DEFAULT_SUBSCRIBE_RECEIVE_TIMEOUT;

	private Date isActiveSince;

	// The tail is the salt which is added to extranonce1 and which is unique by
	// connection.
	private String extranonce1Tail;
	private Integer extranonce2Size;

	private Map<String, String> authorizedWorkers;

	private boolean isSetExtranonceNotificationSupported = false;

	private WorkerConnectionHashrateDelegator workerHashrateDelegator;

	private Boolean logRealShareDifficulty = ConfigurationManager.getInstance().getLogRealShareDifficulty();
	private GetworkJobTemplate currentHeader;

	public StratumWorkerConnection(Socket socket, ProxyManager manager) {
		super(socket);
		this.manager = manager;
		this.authorizedWorkers = Collections.synchronizedMap(new HashMap<String, String>());
		this.workerHashrateDelegator = new WorkerConnectionHashrateDelegator();
	}

	@Override
	public void startReading() {
		super.startReading();
		subscribeTimeoutTask = new Task() {
			public void run() {
				LOGGER.warn("No subscribe request received from {} in {} ms. Closing connection.", getConnectionName(), subscribeReceiveTimeout);
				// Close the connection if subscribe request is not received at
				// time.
				close();
			}
		};
		subscribeTimeoutTask.setName("SubscribeTimeoutTask-" + getConnectionName());
		Timer.getInstance().schedule(subscribeTimeoutTask, subscribeReceiveTimeout);
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
			authorizedWorkers.put(request.getUsername(), request.getPassword());
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
		if (subscribeTimeoutTask != null) {
			subscribeTimeoutTask.cancel();
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
		if (authorizedWorkers.get(request.getWorkerName()) != null) {
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

		MiningExtranonceSubscribeResponse response = new MiningExtranonceSubscribeResponse();
		response.setId(request.getId());
		response.setResult(Boolean.TRUE);
		sendResponse(response);
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
		String difficultyString = pool != null ? Double.toString(pool.getDifficulty()) : "Unknown";

		if (logRealShareDifficulty) {
			Double realDifficulty = DifficultyUtils.getRealShareDifficulty(currentHeader, extranonce1Tail, workerRequest.getExtranonce2(),
					workerRequest.getNtime(), workerRequest.getNonce());
			difficultyString = Double.toString(realDifficulty) + "/" + difficultyString;
		}

		if (poolResponse.getIsAccepted() != null && poolResponse.getIsAccepted()) {
			LOGGER.info("Accepted share (diff: {}) from {}@{} on {}. Yeah !!!!", difficultyString, workerRequest.getWorkerName(),
					getConnectionName(), pool.getName());
		} else {
			LOGGER.info("REJECTED share (diff: {}) from {}@{} on {}. Booo !!!!. Error: {}", difficultyString, workerRequest.getWorkerName(),
					getConnectionName(), pool.getName(), poolResponse.getJsonError());
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
			extranonce2Size = pool.getWorkerExtranonce2Size();
			MiningSetExtranonceNotification extranonceNotif = new MiningSetExtranonceNotification();
			extranonceNotif.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
			extranonceNotif.setExtranonce2Size(extranonce2Size);
			sendNotification(extranonceNotif);

			if (logRealShareDifficulty) {
				updateBlockExtranonce();
			}
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
			updateBlockExtranonce();
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
		MiningNotifyNotification notify = pool.getCurrentStratumJob();
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

	@Override
	public Map<String, String> getAuthorizedWorkers() {
		Map<String, String> result = null;
		synchronized (authorizedWorkers) {
			result = new HashMap<>(authorizedWorkers);
		}
		return result;
	}

	@Override
	public double getAcceptedHashrate() {
		return workerHashrateDelegator.getAcceptedHashrate();
	}

	@Override
	public double getRejectedHashrate() {
		return workerHashrateDelegator.getRejectedHashrate();
	}

	@Override
	public void updateShareLists(Share share, boolean isAccepted) {
		workerHashrateDelegator.updateShareLists(share, isAccepted);
	}

	@Override
	public void setSamplingHashesPeriod(Integer samplingHashesPeriod) {
		workerHashrateDelegator.setSamplingHashesPeriod(samplingHashesPeriod);
	}

	@Override
	public Date getActiveSince() {
		return isActiveSince;
	}

	public boolean isSetExtranonceNotificationSupported() {
		return isSetExtranonceNotificationSupported;
	}

	@Override
	public void onPoolDifficultyChanged(MiningSetDifficultyNotification notification) {
		if (logRealShareDifficulty) {
			updateBlockDifficulty();
		}
		sendNotification(notification);
	}

	@Override
	public void onPoolNotify(MiningNotifyNotification notification) {
		if (logRealShareDifficulty) {
			updateBlockHeader(notification);
		}
		sendNotification(notification);
	}

	/**
	 * Update the block header based on the notification
	 * 
	 * @param notification
	 */
	private void updateBlockHeader(MiningNotifyNotification notification) {
		LOGGER.debug("Update getwork job for connection {}.", getConnectionName());
		// Update the job only if a clean job is requested and if the connection
		// is bound to a pool.
		if (pool != null) {
			currentHeader = new GetworkJobTemplate(notification.getJobId(), notification.getBitcoinVersion(), notification.getPreviousHash(),
					notification.getCurrentNTime(), notification.getNetworkDifficultyBits(), notification.getMerkleBranches(),
					notification.getCoinbase1(), notification.getCoinbase2(), getPool().getExtranonce1() + extranonce1Tail);
			currentHeader.setDifficulty(pool.getDifficulty(), ConfigurationManager.getInstance().isScrypt());
		}
	}

	/**
	 * Update the difficulty to solve for the current block
	 */
	private void updateBlockDifficulty() {
		if (currentHeader != null) {
			currentHeader.setDifficulty(pool.getDifficulty(), ConfigurationManager.getInstance().isScrypt());
		}
	}

	/**
	 * Update the extranonce1 value of the current block.
	 */
	private void updateBlockExtranonce() {
		if (currentHeader != null) {
			currentHeader.setExtranonce1(getPool().getExtranonce1() + extranonce1Tail);
		}
	}
}
