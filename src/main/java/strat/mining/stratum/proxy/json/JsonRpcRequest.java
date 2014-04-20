package strat.mining.stratum.proxy.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class JsonRpcRequest {

	private String id;
	private String method;

	public JsonRpcRequest() {
	}

	public JsonRpcRequest(JsonRpcRequest request) {
		this.id = request.id;
		this.method = request.method;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMethod() {
		return method;
	}

	protected void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Return the parameters
	 * 
	 * @return
	 */
	public abstract List<Object> getParams();

	/**
	 * Set the parameters
	 * 
	 * @return
	 */
	public abstract void setParams(List<Object> params);

}
