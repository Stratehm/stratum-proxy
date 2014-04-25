package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningExtranonceSubscribeRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "mining.extranonce.subscribe";

	public MiningExtranonceSubscribeRequest() {
		super(METHOD_NAME);
	}

	public MiningExtranonceSubscribeRequest(JsonRpcRequest request) {
		super(request);
	}

}
