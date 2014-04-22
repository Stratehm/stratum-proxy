package strat.mining.stratum.proxy.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Connection {

	private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	private Socket socket;
	private Thread readThread;
	private ObjectMapper objectMapper;

	private AtomicInteger nextRequestId;

	private Map<Integer, JsonRpcRequest> sentRequestIds;

	private DataOutputStream outputStream;

	public Connection(Socket socket) {
		this.socket = socket;
		this.objectMapper = new ObjectMapper();
		this.sentRequestIds = Collections.synchronizedMap(new HashMap<Integer, JsonRpcRequest>());
		this.nextRequestId = new AtomicInteger(0);
	}

	/**
	 * Send a request to the remote host.
	 * 
	 * @param line
	 */
	public void sendRequest(JsonRpcRequest request) {
		try {
			if (request.getId() == null) {
				request.setId(nextRequestId.getAndIncrement());
			}
			sentRequestIds.put(request.getId(), request);
			String json = objectMapper.writeValueAsString(request);

			LOGGER.debug("{}. Send request: {}", getConnectionName(), json);
			byte[] stringBytes = (json + "\n").getBytes("UTF-8");
			ensureStream().write(stringBytes, 0, stringBytes.length);
			ensureStream().flush();
		} catch (IOException e) {
			onDisconnect(e);
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

			LOGGER.debug("{}. Send response: {}", getConnectionName(), json);
			byte[] stringBytes = (json + "\n").getBytes("UTF-8");
			ensureStream().write(stringBytes, 0, stringBytes.length);
			ensureStream().flush();
		} catch (IOException e) {
			onDisconnect(e);
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

			LOGGER.debug("{}. Send notification: {}", getConnectionName(), json);
			byte[] stringBytes = (json + "\n").getBytes("UTF-8");
			ensureStream().write(stringBytes, 0, stringBytes.length);
			ensureStream().flush();
		} catch (IOException e) {
			onDisconnect(e);
		}
	}

	/**
	 * Start reading lines from the connection.
	 */
	public void startReading() {
		readThread = new Thread() {
			public void run() {
				if (socket != null && socket.isConnected() && !socket.isClosed()) {
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

						String line = reader.readLine();
						while (line != null && !Thread.currentThread().isInterrupted()) {
							onLineRead(line);
							line = reader.readLine();
						}

					} catch (Exception e) {
						onDisconnect(e);
					} finally {
						try {
							socket.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		};
		readThread.start();
	}

	/**
	 * Close the connection
	 */
	public void close() {
		LOGGER.debug("Closing connection {}...", getConnectionName());
		readThread.interrupt();
		try {
			socket.close();
		} catch (IOException e) {
			// Do nothing
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
					onResponseReceived(request, response);
				}
			} else {
				// Else it is a notification
				onNotificationReceived(new JsonRpcNotification(request));
			}

		} catch (Exception e) {
			onParsingError(line, e);
		}
	}

	/**
	 * Called when a notification is received from the remote host.
	 */
	protected abstract void onNotificationReceived(JsonRpcNotification notification);

	/**
	 * Called when a response message is received from a previous request
	 */
	protected abstract void onResponseReceived(JsonRpcRequest request, JsonRpcResponse response);

	/**
	 * Called when a request is received from the remote host
	 */
	protected abstract void onRequestReceived(JsonRpcRequest request);

	/**
	 * Called when a parsing error occurs
	 */
	protected abstract void onParsingError(String line, Throwable throwable);

	/**
	 * Called when a disconnection is detected.
	 * 
	 * @param cause
	 */
	protected abstract void onDisconnect(Throwable cause);

	/**
	 * Return the name of the connection
	 * 
	 * @return
	 */
	protected abstract String getConnectionName();
}
