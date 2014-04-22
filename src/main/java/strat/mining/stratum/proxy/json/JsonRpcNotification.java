package strat.mining.stratum.proxy.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcNotification {

	// The id of a notification is always null.
	private Integer id = null;
	private String method;
	private List<Object> params;

	public JsonRpcNotification(String method) {
		this.method = method;
	}

	public JsonRpcNotification(JsonRpcNotification request) {
		this.method = request.method;
		this.setParams(request.getParams());
	}

	public JsonRpcNotification(JsonRpcRequest request) {
		this.method = request.getMethod();
		this.setParams(request.getParams());
	}

	public Integer getId() {
		return id;
	}

	public String getMethod() {
		return method;
	}

	public List<Object> getParams() {
		return params;
	}

	public void setParams(List<Object> params) {
		this.params = params;
	}

}
