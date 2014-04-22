package strat.mining.stratum.proxy.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest {

	private Integer id;
	private String method;
	private List<Object> params;

	protected JsonRpcRequest() {
	}

	public JsonRpcRequest(String method) {
		this.method = method;
	}

	public JsonRpcRequest(JsonRpcRequest request) {
		this.id = request.id;
		this.method = request.method;
		this.setParams(request.getParams());
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
