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
package strat.mining.stratum.proxy.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.json.ClientGetVersionRequest;
import strat.mining.stratum.proxy.json.ClientGetVersionResponse;
import strat.mining.stratum.proxy.json.ClientReconnectNotification;
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
		readThread.setDaemon(true);
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
		case ClientReconnectNotification.METHOD_NAME:
			ClientReconnectNotification clientReconnect = new ClientReconnectNotification(notification);
			onClientReconnect(clientReconnect);
			break;

		default:
			LOGGER.warn("Unknown notification type on connection {}. methodName: {}, params: {}", getConnectionName(), notification.getMethod(),
					notification.getParams());
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

		case ClientGetVersionRequest.METHOD_NAME:
			ClientGetVersionRequest getVersionRequest = new ClientGetVersionRequest(request);
			ClientGetVersionResponse getVersionResponse = new ClientGetVersionResponse(response);
			onGetVersionResponse(getVersionRequest, getVersionResponse);
			break;

		default:
			LOGGER.warn("Unknown response type on connection {}. methodName: {}, result: {}", getConnectionName(), request.getMethod(),
					response.getResult());
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

		case ClientGetVersionRequest.METHOD_NAME:
			ClientGetVersionRequest getVersionRequest = new ClientGetVersionRequest(request);
			onGetVersionRequest(getVersionRequest);
			break;

		// Following requests are notifications, but some pools does not respect
		// the stratum protocol and send notifications as requests.
		case ClientReconnectNotification.METHOD_NAME:
			ClientReconnectNotification clientReconnect = new ClientReconnectNotification();
			clientReconnect.setParams(request.getParams());
			onClientReconnect(clientReconnect);
			break;

		case MiningNotifyNotification.METHOD_NAME:
			MiningNotifyNotification notify = new MiningNotifyNotification();
			notify.setParams(request.getParams());
			onNotify(notify);
			break;
		case MiningSetDifficultyNotification.METHOD_NAME:
			MiningSetDifficultyNotification setDiff = new MiningSetDifficultyNotification();
			setDiff.setParams(request.getParams());
			onSetDifficulty(setDiff);
			break;
		case MiningSetExtranonceNotification.METHOD_NAME:
			MiningSetExtranonceNotification setExtranonce = new MiningSetExtranonceNotification();
			setExtranonce.setParams(request.getParams());
			onSetExtranonce(setExtranonce);
			break;

		default:
			LOGGER.warn("Unknown request type on connection {}. methodName: {}, id: {}, params: {}", getConnectionName(), request.getMethod(),
					request.getId(), request.getParams());
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
	 * Called when a clientReconnect is received
	 */
	protected abstract void onClientReconnect(ClientReconnectNotification clientReconnect);

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
	 * Called when a client getVersion request is received
	 */
	protected abstract void onGetVersionRequest(ClientGetVersionRequest request);

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
	 * Called when a client getVersion response is received
	 */
	protected abstract void onGetVersionResponse(ClientGetVersionRequest request, ClientGetVersionResponse response);

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

	/**
	 * Return true if the connection is established.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return !socket.isClosed() && socket.isConnected();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((socket == null) ? 0 : socket.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StratumConnection other = (StratumConnection) obj;
		if (socket == null) {
			if (other.socket != null)
				return false;
		} else if (!socket.toString().equals(other.socket == null ? null : other.socket.toString()))
			return false;
		return true;
	}

	/**
	 * Return the remote address. Return null if not connected.
	 * 
	 * @return
	 */
	public InetAddress getRemoteAddress() {
		return socket.getInetAddress();
	}

	/**
	 * Return the remote port. Return null if not connected.
	 * 
	 * @return
	 */
	public Integer getRemotePort() {
		return socket.getPort();
	}

}
