package strat.mining.stratum.proxy.callback;

import strat.mining.stratum.proxy.json.JsonRpcRequest;
import strat.mining.stratum.proxy.json.JsonRpcResponse;

public interface ResponseReceivedCallback<R extends JsonRpcRequest, T extends JsonRpcResponse> {

	/**
	 * Called when a response is received for the given request.
	 * 
	 * @param request
	 * @param response
	 */
	public void onResponseReceived(R request, T response);

}
