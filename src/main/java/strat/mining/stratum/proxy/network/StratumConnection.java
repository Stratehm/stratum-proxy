package strat.mining.stratum.proxy.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;
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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class StratumConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(StratumConnection.class);

	private Socket socket;
	private Thread readThread;
	private ObjectMapper objectMapper;

	private Map<Long, JsonRpcRequest> sentRequestIds;

	private DataOutputStream outputStream;

	// Indicate if an error should be thrown on socket disconnection. (False if
	// the disconnect is a user request)
	private Boolean throwDisconnectError;

	private boolean disconnectOnParsingError;

	public StratumConnection(Socket socket) {
		this.socket = socket;
		this.objectMapper = new ObjectMapper();
		this.sentRequestIds = Collections.synchronizedMap(new HashMap<Long, JsonRpcRequest>());
		this.throwDisconnectError = true;
		this.disconnectOnParsingError = false;
	}

	/**
	 * Send a request to the remote host.
	 * 
	 * @param line
	 */
	public void sendRequest(JsonRpcRequest request) {
		try {
			sentRequestIds.put(request.getId(), request);
			String json = objectMapper.writeValueAsString(request);
			json = json.replaceAll("\":", "\": ").replaceAll(",\"", ", \"");

			LOGGER.debug("{}. Send request: {}", getConnectionName(), json);
			byte[] stringBytes = (json + "\n").getBytes("UTF-8");
			ensureStream().write(stringBytes, 0, stringBytes.length);
			ensureStream().flush();
		} catch (IOException e) {
			onDisconnectWithError(e);
		}
	}

	/**
	 * Send a response to the remote host.
	 * 
	 * @param line
	 */
	public void sendResponse(JsonRpcResponse response) {
		try {
			String json = objectMapper.writeValueAsString(response);
			json = json.replaceAll("\":", "\": ").replaceAll(",\"", ", \"");

			LOGGER.debug("{}. Send response: {}", getConnectionName(), json);
			byte[] stringBytes = (json + "\n").getBytes("UTF-8");
			ensureStream().write(stringBytes, 0, stringBytes.length);
			ensureStream().flush();
		} catch (IOException e) {
			onDisconnectWithError(e);
		}
	}

	/**
	 * Send a notification to the remote host.
	 * 
	 * @param notification
	 */
	public void sendNotification(JsonRpcNotification notification) {
		try {
			String json = objectMapper.writeValueAsString(notification);
			json = json.replaceAll("\":", "\": ").replaceAll(",\"", ", \"");

			LOGGER.debug("{}. Send notification: {}", getConnectionName(), json);
			byte[] stringBytes = (json + "\n").getBytes("UTF-8");
			ensureStream().write(stringBytes, 0, stringBytes.length);
			ensureStream().flush();
		} catch (IOException e) {
			onDisconnectWithError(e);
		}
	}

	/**
	 * Start reading lines from the connection.
	 */
	public void startReading() {
		readThread = new Thread() {
			public void run() {
				if (socket != null && socket.isConnected() && !socket.isClosed()) {
					LOGGER.debug("Start reading on connection {}.", getConnectionName());
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

						String line = reader.readLine();
						while (line != null && !Thread.currentThread().isInterrupted()) {
							onLineRead(line);
							line = reader.readLine();
						}

						throw new IOException("EOF on inputStream.");

					} catch (Exception e) {
						if (throwDisconnectError) {
							onDisconnectWithError(e);
						}
					} finally {
						close();
					}
				}
			}
		};
		readThread.setName(getConnectionName() + "-Thread");
		readThread.start();
	}

	/**
	 * Close the connection
	 */
	public void close() {
		LOGGER.debug("Closing connection {}...", getConnectionName());
		throwDisconnectError = false;
		readThread.interrupt();
		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.error("Failed ot close connection {}.", getConnectionName(), e);
		}
	}

	/**
	 * Return the output stream of the connection. If it is not available, then
	 * throw an exception.
	 * 
	 * @return
	 * @throws Exception
	 */
	private DataOutputStream ensureStream() throws IOException {
		if (socket.isConnected() && !socket.isClosed()) {
			outputStream = new DataOutputStream(socket.getOutputStream());
		} else {
			throw new IOException("Socket not connected.");
		}
		return outputStream;
	}

	/**
	 * Parse the JSON-RPC command and call on message Received
	 * 
	 * @param line
	 */
	private void onLineRead(String line) {
		try {
			LOGGER.debug("{}. Line read: {}", getConnectionName(), line);
			try {
				JsonRpcRequest request = objectMapper.readValue(line, JsonRpcRequest.class);

				// If there is an id, it may be a request or a response
				if (request.getId() != null) {
					// If there is a method name, it is a request.
					if (request.getMethod() != null) {
						onRequestReceived(request);
					} else {
						// Else it is a response
						JsonRpcResponse response = objectMapper.readValue(line, JsonRpcResponse.class);
						request = sentRequestIds.remove(request.getId());
						if (request != null) {
							onResponseReceived(request, response);
						} else {
							LOGGER.debug("Drop response since no request has been sent with the id {}.", response.getId());
						}
					}
				} else {
					// Else it is a notification
					onNotificationReceived(new JsonRpcNotification(request));
				}
			} catch (JsonMappingException e) {
				if (disconnectOnParsingError) {
					throw e;
				} else {
					LOGGER.error("JSON-RPC Parsing error with line: {}", line, e);
				}
			}

		} catch (Exception e) {
			onParsingError(line, e);
		}
	}

	/**
	 * Called when a notification is received from the remote host.
	 */
	protected void onNotificationReceived(JsonRpcNotification notification) {
		switch (notification.getMethod()) {
		case MiningNotifyNotification.METHOD_NAME:
			MiningNotifyNotification notify = new MiningNotifyNotification(notification);
			onNotify(notify);
			break;
		case MiningSetDifficultyNotification.METHOD_NAME:
			MiningSetDifficultyNotification setDiff = new MiningSetDifficultyNotification(notification);
			onSetDifficulty(setDiff);
			break;
		case MiningSetExtranonceNotification.METHOD_NAME:
			MiningSetExtranonceNotification setExtranonce = new MiningSetExtranonceNotification(notification);
			onSetExtranonce(setExtranonce);
			break;

		default:
			LOGGER.warn("Unknown notification type. methodName: {}, params: {}", notification.getMethod(), notification.getParams());
			break;
		}
	}

	/**
	 * Called when a response message is received from a previous request
	 */
	protected void onResponseReceived(JsonRpcRequest request, JsonRpcResponse response) {
		switch (request.getMethod()) {
		case MiningAuthorizeRequest.METHOD_NAME:
			MiningAuthorizeRequest auhtorizeRequest = new MiningAuthorizeRequest(request);
			MiningAuthorizeResponse authorizeResponse = new MiningAuthorizeResponse(response);
			onAuthorizeResponse(auhtorizeRequest, authorizeResponse);
			break;

		case MiningSubscribeRequest.METHOD_NAME:
			MiningSubscribeRequest subscribeRequest = new MiningSubscribeRequest(request);
			MiningSubscribeResponse subscribeResponse = new MiningSubscribeResponse(response);
			onSubscribeResponse(subscribeRequest, subscribeResponse);
			break;

		case MiningSubmitRequest.METHOD_NAME:
			MiningSubmitRequest submitRequest = new MiningSubmitRequest(request);
			MiningSubmitResponse submitResponse = new MiningSubmitResponse(response);
			onSubmitResponse(submitRequest, submitResponse);
			break;

		case MiningExtranonceSubscribeRequest.METHOD_NAME:
			MiningExtranonceSubscribeRequest subscribeExtranonceRequest = new MiningExtranonceSubscribeRequest(request);
			MiningExtranonceSubscribeResponse subscribeExtranonceResponse = new MiningExtranonceSubscribeResponse(response);
			onExtranonceSubscribeResponse(subscribeExtranonceRequest, subscribeExtranonceResponse);
			break;

		default:
			LOGGER.warn("Unknown response type. methodName: {}, result: {}", request.getMethod(), response.getResult());
			break;
		}
	}

	/**
	 * Called when a request is received from the remote host
	 */
	protected void onRequestReceived(JsonRpcRequest request) {
		switch (request.getMethod()) {
		case MiningAuthorizeRequest.METHOD_NAME:
			MiningAuthorizeRequest auhtorizeRequest = new MiningAuthorizeRequest(request);
			onAuthorizeRequest(auhtorizeRequest);
			break;

		case MiningSubscribeRequest.METHOD_NAME:
			MiningSubscribeRequest subscribeRequest = new MiningSubscribeRequest(request);
			onSubscribeRequest(subscribeRequest);
			break;

		case MiningSubmitRequest.METHOD_NAME:
			MiningSubmitRequest submitRequest = new MiningSubmitRequest(request);
			onSubmitRequest(submitRequest);
			break;

		case MiningExtranonceSubscribeRequest.METHOD_NAME:
			MiningExtranonceSubscribeRequest extranonceSubscribeRequest = new MiningExtranonceSubscribeRequest(request);
			onExtranonceSubscribeRequest(extranonceSubscribeRequest);
			break;

		default:
			LOGGER.warn("Unknown request type. methodName: {}, id: {}, params: {}", request.getMethod(), request.getId(), request.getParams());
			break;
		}
	}

	/**
	 * Called when a notify is received
	 */
	protected abstract void onNotify(MiningNotifyNotification notify);

	/**
	 * Called when a setDifficulty is received
	 */
	protected abstract void onSetDifficulty(MiningSetDifficultyNotification setDifficulty);

	/**
	 * Called when a setExtranonce is received
	 */
	protected abstract void onSetExtranonce(MiningSetExtranonceNotification setExtranonce);

	/**
	 * Called when a authorize request is received
	 */
	protected abstract void onAuthorizeRequest(MiningAuthorizeRequest request);

	/**
	 * Called when a subscribe request is received
	 */
	protected abstract void onSubscribeRequest(MiningSubscribeRequest request);

	/**
	 * Called when a submit request is received
	 */
	protected abstract void onSubmitRequest(MiningSubmitRequest request);

	/**
	 * Called when a extranonce subcribe request is received
	 */
	protected abstract void onExtranonceSubscribeRequest(MiningExtranonceSubscribeRequest request);

	/**
	 * Called when a extranonce subscribe response is received
	 */
	protected abstract void onExtranonceSubscribeResponse(MiningExtranonceSubscribeRequest request, MiningExtranonceSubscribeResponse response);

	/**
	 * Called when a authorize response is received
	 */
	protected abstract void onAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response);

	/**
	 * Called when a subscribe response is received
	 */
	protected abstract void onSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response);

	/**
	 * Called when a submit response is received
	 */
	protected abstract void onSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response);

	/**
	 * Called when a parsing error occurs
	 */
	protected abstract void onParsingError(String line, Throwable throwable);

	/**
	 * Called when a disconnection is detected.
	 * 
	 * @param cause
	 */
	protected abstract void onDisconnectWithError(Throwable cause);

	/**
	 * Return the name of the connection
	 * 
	 * @return
	 */
	public String getConnectionName() {
		return socket != null ? socket.getRemoteSocketAddress().toString() : "Undefined";
	}
}
