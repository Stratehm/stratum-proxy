package strat.mining.stratum.proxy.pool;

import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningAuthorizeResponse;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;
import strat.mining.stratum.proxy.network.Connection;

public class PoolConnection extends Connection {

	private static final Logger LOGGER = LoggerFactory.getLogger(PoolConnection.class);

	private Pool pool;

	public PoolConnection(Pool pool, Socket socket) {
		super(socket);
		this.pool = pool;
	}

	@Override
	protected void onNotificationReceived(JsonRpcNotification notification) {
		switch (notification.getMethod()) {
		case MiningNotifyNotification.METHOD_NAME:
			MiningNotifyNotification notify = new MiningNotifyNotification(notification);
			pool.processNotify(notify);
			break;
		case MiningSetDifficultyNotification.METHOD_NAME:
			MiningSetDifficultyNotification setDiff = new MiningSetDifficultyNotification(notification);
			pool.processSetDifficulty(setDiff);
			break;

		default:
			LOGGER.warn("Unknown notification type. methodName: {}, params: {}", notification.getMethod(), notification.getParams());
			break;
		}
	}

	@Override
	protected void onResponseReceived(JsonRpcRequest request, JsonRpcResponse response) {
		switch (request.getMethod()) {
		case MiningAuthorizeRequest.METHOD_NAME:
			MiningAuthorizeRequest auhtorizeRequest = new MiningAuthorizeRequest(request);
			MiningAuthorizeResponse authorizeResponse = new MiningAuthorizeResponse(response);
			pool.processAuthorizeResponse(auhtorizeRequest, authorizeResponse);
			break;

		case MiningSubscribeRequest.METHOD_NAME:
			MiningSubscribeRequest subscribeRequest = new MiningSubscribeRequest(request);
			MiningSubscribeResponse subscribeResponse = new MiningSubscribeResponse(response);
			pool.processSubscribeResponse(subscribeRequest, subscribeResponse);
			break;

		case MiningSubmitRequest.METHOD_NAME:
			MiningSubmitRequest submitRequest = new MiningSubmitRequest(request);
			MiningSubmitResponse submitResponse = new MiningSubmitResponse(response);
			pool.processSubmitResponse(submitRequest, submitResponse);
			break;

		default:
			LOGGER.warn("Unknown response type. methodName: {}, result: {}", request.getMethod(), response.getResult());
			break;
		}
	}

	@Override
	protected void onRequestReceived(JsonRpcRequest request) {
		// No request should be received from the pool
		LOGGER.warn("Request received from pool {}. This should not happen. methodName: {}, id: {}, params: {}", pool.getHost(), request.getMethod(),
				request.getId(), request.getParams());
	}

	@Override
	protected void onParsingError(String line, Throwable throwable) {
		LOGGER.error("{}. JSON-RPC parsing error with line: {}.", getConnectionName(), line, throwable);
	}

	@Override
	protected void onDisconnect(Throwable cause) {
		pool.onDisconnect(cause);
	}

	@Override
	protected String getConnectionName() {
		return "Pool: " + pool.getHost();
	}

}
