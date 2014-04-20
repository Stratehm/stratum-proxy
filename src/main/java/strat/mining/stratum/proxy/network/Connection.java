package strat.mining.stratum.proxy.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;

public abstract class Connection {

	private Socket socket;
	private Thread readThread;

	public Connection(Socket socket) {
		this.socket = socket;
	}

	/**
	 * Send a request to the remote host.
	 * 
	 * @param line
	 */
	public void sendRequest(JsonRpcRequest request) {

	}

	/**
	 * Send a response to the remote host.
	 * 
	 * @param line
	 */
	public void sendResponse(JsonRpcResponse response) {

	}

	/**
	 * Send a notification to the remote host.
	 * 
	 * @param notification
	 */
	public void sentNotification(JsonRpcNotification notification) {

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
	 * Parse the JSON-RPC command and call on message Received
	 * 
	 * @param line
	 */
	private void onLineRead(String line) {

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
}
