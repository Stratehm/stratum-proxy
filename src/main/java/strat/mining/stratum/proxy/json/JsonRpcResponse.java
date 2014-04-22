package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A response to a request.
 * 
 * @author strat
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcResponse {

	private Integer id;
	private JsonRpcError error;
	private Object result;

	public JsonRpcResponse() {
	}

	public JsonRpcResponse(JsonRpcResponse response) {
		this.id = response.id;
		this.error = response.error;
		this.setResult(response.getResult());
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public JsonRpcError getError() {
		return error;
	}

	public void setError(JsonRpcError error) {
		this.error = error;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
