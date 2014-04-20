package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A response to a request.
 * 
 * @author strat
 * 
 * @param <T>
 *            the result type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class JsonRpcResponse<T> {

	private String id;
	private T result;
	private JsonRpcError error;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}

	public JsonRpcError getError() {
		return error;
	}

	public void setError(JsonRpcError error) {
		this.error = error;
	}

}
