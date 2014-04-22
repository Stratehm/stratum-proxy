package strat.mining.stratum.proxy.worker;

import java.net.Socket;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;
import strat.mining.stratum.proxy.network.Connection;

public class WorkerConnection extends Connection {

	public WorkerConnection(Socket socket) {
		super(socket);
	}

	@Override
	protected void onNotificationReceived(JsonRpcNotification notification) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResponseReceived(JsonRpcRequest request, JsonRpcResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onRequestReceived(JsonRpcRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onParsingError(String line, Throwable throwable) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onDisconnect(Throwable cause) {
		// TODO Auto-generated method stub

	}

	@Override
	protected String getConnectionName() {
		// TODO Auto-generated method stub
		return null;
	}

}
