package strat.mining.stratum.proxy.worker;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.JsonRpcError;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningAuthorizeResponse;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSetExtranonceNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.network.StratumConnection;
import strat.mining.stratum.proxy.pool.Pool;

public class WorkerConnection extends StratumConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerConnection.class);

	private Pool pool;

	private StratumProxyManager manager;

	private Timer subscribeTimeoutTimer;
	private Integer subscribeReceiveTimeout = Constants.DEFAULT_SUBSCRIBE_RECEIVE_TIMEOUT;

	// The tail is the salt which is added to extranonce1 and which is unique by
	// connection.
	private String extranonce1Tail;
	private Integer extranonce2Size;

	private Set<String> authorizedWorkers;

	private boolean isSetExtranonceNotificationSupported = false;

	public WorkerConnection(Socket socket, StratumProxyManager manager) {
		super(socket);
		this.manager = manager;
		this.authorizedWorkers = Collections.synchronizedSet(new HashSet<String>());
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
	protected void onAuthorizeRequest(MiningAuthorizeRequest request) {
		MiningAuthorizeResponse response = new MiningAuthorizeResponse();
		response.setId(request.getId());

		if (manager.onAuthorizeRequest(this, request)) {
			response.setIsAuthorized(true);
			authorizedWorkers.add(request.getUsername());
		} else {
			response.setIsAuthorized(false);
			JsonRpcError error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNAUTHORIZED_WORKER.getCode());
			error.setMessage("The worker is not authorized");
			response.setErrorRpc(error);
		}

		sendResponse(response);
	}

	@Override
	protected void onSubscribeRequest(MiningSubscribeRequest request) {
		// Once the subscribe request is received, cancel the timeout timer.
		subscribeTimeoutTimer.cancel();

		JsonRpcError error = null;
		try {
			pool = manager.onSubscribeRequest(this, request);
		} catch (NoPoolAvailableException e) {
			LOGGER.error("No pool available for the connection {}. Sending error and close the connection.", getConnectionName(), e);
			error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNKNOWN.getCode());
			error.setMessage("No pool available on this proxy.");
		}

		if (error == null) {
			try {
				extranonce1Tail = pool.getFreeTail();
				extranonce2Size = pool.getWorkerExtranonce2Size();
			} catch (TooManyWorkersException e) {
				LOGGER.error("Too many connections on pool {} for the connection {}. Sending error and close the connection.", pool.getHost(),
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

	/**
	 * Called when the pool has answered to a submit request.
	 * 
	 * @param workerRequest
	 * @param poolResponse
	 */
	public void onPoolSubmitResponse(MiningSubmitRequest workerRequest, MiningSubmitResponse poolResponse) {
		if (poolResponse.getIsAccepted()) {
			LOGGER.info("Accepted share from {} on {}. Yeah !!!!", getConnectionName(), pool.getHost());
		} else {
			LOGGER.info("REJECTED share from {} on {}. Booo !!!!. (errorCode: {}, message: {})", getConnectionName(), pool.getHost(), poolResponse
					.getJsonError().getCode(), poolResponse.getJsonError().getMessage());
		}

		MiningSubmitResponse workerResponse = new MiningSubmitResponse();
		workerResponse.setId(workerRequest.getId());
		workerResponse.setIsAccepted(poolResponse.getIsAccepted());
		workerResponse.setError(poolResponse.getError());

		sendResponse(workerResponse);
	}

	/**
	 * Called when the pool change its extranonce. Send the extranonce change to
	 * the worker.
	 */
	public void onPoolExtranonceChange() {
		if (isSetExtranonceNotificationSupported) {
			MiningSetExtranonceNotification extranonceNotif = new MiningSetExtranonceNotification();
			extranonceNotif.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
			extranonceNotif.setExtranonce2Size(extranonce2Size);
			sendNotification(extranonceNotif);
		} else {
			// If the extranonce change is not supported by the worker, then
			// notify the manager
			manager.onWorkerChangeExtranonceFailure(this);
		}
	}

	@Override
	protected void onAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		// Do nothing, should never happen
	}

	@Override
	protected void onSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response) {
		// Do nothing, should never happen
	}

	@Override
	protected void onSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
		// Do nothing, should never happen
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
		Integer difficulty = pool.getDifficulty();
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
	 */
	public void rebindToPool(Pool newPool) throws TooManyWorkersException {
		if (isSetExtranonceNotificationSupported) {
			// Release the old extranonce
			pool.releaseTail(extranonce1Tail);

			// Then retrieve a free tail from the new pool.
			extranonce1Tail = newPool.getFreeTail();
			extranonce2Size = newPool.getWorkerExtranonce2Size();
			pool = newPool;

			// Finally, send all notifications to the worker
			sendInitialNotifications();

		} else {
			// If set extranonce not supported, notify the manager
			manager.onWorkerChangeExtranonceFailure(this);
		}
	}

	@Override
	public void close() {
		super.close();
		pool.releaseTail(extranonce1Tail);
	}

}
