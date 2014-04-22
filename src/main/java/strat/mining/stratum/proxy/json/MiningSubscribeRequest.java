package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

public class MiningSubscribeRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "mining.subscribe";

	public MiningSubscribeRequest() {
		super(METHOD_NAME);
	}

	public MiningSubscribeRequest(JsonRpcRequest request) {
		super(request);
	}

	@Override
	public List<Object> getParams() {
		ArrayList<Object> params = new ArrayList<Object>();
		params.add("JStratumProxy");
		return params;
	}

	@Override
	public void setParams(List<Object> params) {
		// Do nothing since there is no mutable parameters for this request
	}

}
