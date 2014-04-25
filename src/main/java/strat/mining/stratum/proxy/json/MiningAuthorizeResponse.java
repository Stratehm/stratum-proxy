package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningAuthorizeResponse extends JsonRpcResponse {

	@JsonIgnore
	private Boolean isAuthorized;

	public MiningAuthorizeResponse() {
		super();
	}

	public MiningAuthorizeResponse(JsonRpcResponse response) {
		super(response);
	}

	public Boolean getIsAuthorized() {
		return isAuthorized;
	}

	public void setIsAuthorized(Boolean isAuthorized) {
		this.isAuthorized = isAuthorized;
	}

	@Override
	public Object getResult() {
		return isAuthorized;
	}

	@Override
	public void setResult(Object result) {
		this.isAuthorized = (Boolean) result;
	}

}
