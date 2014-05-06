package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A response to a request.
 * 
 * @author strat
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcResponse {

	private Long id;
	private List<Object> error;
	private Object result;

	public JsonRpcResponse() {
	}

	public JsonRpcResponse(JsonRpcResponse response) {
		this.id = response.id;
		this.error = response.error;
		this.setResult(response.getResult());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Object> getError() {
		return error;
	}

	@JsonIgnore
	public JsonRpcError getJsonError() {
		JsonRpcError errorObject = new JsonRpcError();
		if (error != null) {
			errorObject.setCode(error.size() > 0 && error.get(0) != null ? (Integer) error.get(0) : null);
			errorObject.setMessage(error.size() > 1 && error.get(1) != null ? (String) error.get(1) : null);
			errorObject.setTraceback(error.size() > 2 && error.get(2) != null ? error.get(2) : null);
		}
		return errorObject;
	}

	public void setError(List<Object> errorObject) {
		error = errorObject;
	}

	public void setErrorRpc(JsonRpcError errorObject) {
		error = new ArrayList<Object>();
		error.add(errorObject.getCode());
		error.add(errorObject.getMessage());
		error.add(errorObject.getTraceback());
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getResultObjectAtIndex(int index) {
		T resultObject = null;
		if (result instanceof List) {
			List<Object> resultList = (List<Object>) result;
			resultObject = resultList.size() > index && resultList.get(index) != null ? (T) resultList.get(index) : null;
		}
		return resultObject;
	}
}
