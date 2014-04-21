package strat.mining.stratum.proxy.pool;

import java.net.Socket;

import strat.mining.stratum.proxy.json.JsonRpcNotification;
import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;
import strat.mining.stratum.proxy.network.Connection;

public class PoolConnection extends Connection {

	private Pool pool;

	public PoolConnection(Pool pool, Socket socket) {
		super(socket);
		this.pool = pool;
	}

	@Override
	protected void onNotificationReceived(JsonRpcNotification notification) {

	}

	@Override
	protected void onResponseReceived(JsonRpcRequest request,
			JsonRpcResponse response) {

	}

	@Override
	protected void onRequestReceived(JsonRpcRequest request) {

	}

	@Override
	protected void onParsingError(String line, Throwable throwable) {

	}

}
