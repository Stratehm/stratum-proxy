package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MiningAuthorizeRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "mining.authorize";

	private List<Object> params;
	@JsonIgnore
	private String username;
	@JsonIgnore
	private String password;

	public MiningAuthorizeRequest() {
		super();
		setMethod(METHOD_NAME);
	}

	public MiningAuthorizeRequest(JsonRpcRequest request) {
		super(request);
		setMethod(METHOD_NAME);
		setParams(request.getParams());
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public List<Object> getParams() {
		if (params == null) {
			params = new ArrayList<Object>();
			params.add(username);
			params.add(password);
		}
		return params;
	}

	@Override
	public void setParams(List<Object> params) {
		this.params = params;
		if (params != null) {
			username = (String) params.get(0);
			password = (String) params.get(1);
		}
	}

}
