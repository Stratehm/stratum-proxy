package strat.mining.stratum.proxy.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class JsonRpcNotification extends JsonRpcRequest {

	// The id of a notification is always null.
	private String id = null;
	private String method;

	public JsonRpcNotification() {
	}

	public JsonRpcNotification(JsonRpcNotification request) {
		this.method = request.method;
	}

	public String getId() {
		return id;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
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
