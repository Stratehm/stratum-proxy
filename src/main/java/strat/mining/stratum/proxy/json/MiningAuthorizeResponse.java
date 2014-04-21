package strat.mining.stratum.proxy.json;

public class MiningAuthorizeResponse extends JsonRpcResponse {

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
