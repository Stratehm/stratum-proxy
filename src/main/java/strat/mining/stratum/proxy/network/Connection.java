package strat.mining.stratum.proxy.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Connection {

	private Socket socket;
	private Thread readThread;
	private ObjectMapper objectMapper;

	private AtomicInteger nextRequestId;

	private Map<String, JsonRpcRequest> sentRequestIds;

	private BufferedWriter writer;

	public Connection(Socket socket) {
		this.socket = socket;
		this.objectMapper = new ObjectMapper();
		this.sentRequestIds = Collections
				.synchronizedMap(new HashMap<String, JsonRpcRequest>());
		this.nextRequestId = new AtomicInteger(1);
	}

	/**
	 * Send a request to the remote host.
	 * 
	 * @param line
	 */
	public void sendRequest(JsonRpcRequest request) throws Exception {
		if (request.getId() == null) {
			request.setId(Integer.toString(nextRequestId.getAndIncrement()));
		}
		sentRequestIds.put(request.getId(), request);
		String json = objectMapper.writeValueAsString(request);

		ensureWriter().write(json + "\n");
		ensureWriter().flush();
	}

	/**
	 * Send a response to the remote host.
	 * 
	 * @param line
	 */
	public void sendResponse(JsonRpcResponse response) throws Exception {
		String json = objectMapper.writeValueAsString(response);

		ensureWriter().write(json + "\n");
		ensureWriter().flush();
	}

	/**
	 * Send a notification to the remote host.
	 * 
	 * @param notification
	 */
	public void sendNotification(JsonRpcNotification notification)
			throws Exception {
		String json = objectMapper.writeValueAsString(notification);

		ensureWriter().write(json + "\n");
		ensureWriter().flush();
	}

	/**
	 * Start reading lines from the connection.
	 */
	public void startReading() {
		readThread = new Thread() {
			public void run() {
				if (socket != null && socket.isConnected()
						&& !socket.isClosed()) {
					try {
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(socket.getInputStream()));

						String line = reader.readLine();
						while (line != null
								&& !Thread.currentThread().isInterrupted()) {
							onLineRead(line);
							line = reader.readLine();
						}

					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
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
	}

	/**
	 * Close the connection
	 */
	public void close() {
		readThread.interrupt();
		try {
			socket.close();
		} catch (IOException e) {
			// Do nothing
		}
	}

	/**
	 * Return the writer of the connection. If it is not available, then throw
	 * an exception.
	 * 
	 * @return
	 * @throws Exception
	 */
	private BufferedWriter ensureWriter() throws Exception {
		if (socket.isConnected() && !socket.isClosed()) {
			writer = new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream()));
		} else {
			throw new IOException("Socket not connected.");
		}
		return writer;
	}

	/**
	 * Parse the JSON-RPC command and call on message Received
	 * 
	 * @param line
	 */
	private void onLineRead(String line) {
		try {
			JsonRpcRequest request = objectMapper.readValue(line,
					JsonRpcRequest.class);

			// If there is an id, it may be a request or a response
			if (request.getId() != null) {
				// If there is a method name, it is a request.
				if (request.getMethod() != null) {
					onRequestReceived(request);
				} else {
					// Else it is a response
					JsonRpcResponse response = objectMapper.readValue(line,
							JsonRpcResponse.class);
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
	protected abstract void onNotificationReceived(
			JsonRpcNotification notification);

	/**
	 * Called when a response message is received from a previous request
	 */
	protected abstract void onResponseReceived(JsonRpcRequest request,
			JsonRpcResponse response);

	/**
	 * Called when a request is received from the remote host
	 */
	protected abstract void onRequestReceived(JsonRpcRequest request);

	/**
	 * Called when a parsing error occurs
	 */
	protected abstract void onParsingError(String line, Throwable throwable);
}
