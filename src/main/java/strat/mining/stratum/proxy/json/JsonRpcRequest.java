package strat.mining.stratum.proxy.json;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest {

	private static final AtomicLong nextRequestId = new AtomicLong(0);

	private Long id;
	private String method;
	private List<Object> params;

	protected JsonRpcRequest() {
		id = nextRequestId.getAndIncrement();
	}

	public JsonRpcRequest(String method) {
		this();
		this.method = method;
	}

	public JsonRpcRequest(JsonRpcRequest request) {
		this.id = request.id;
		this.method = request.method;
		this.setParams(request.getParams());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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
