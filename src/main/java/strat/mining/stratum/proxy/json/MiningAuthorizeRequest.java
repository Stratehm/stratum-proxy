package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningAuthorizeRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "mining.authorize";

	@JsonIgnore
	private String username;
	@JsonIgnore
	private String password;

	public MiningAuthorizeRequest() {
		super(METHOD_NAME);
	}

	public MiningAuthorizeRequest(JsonRpcRequest request) {
		super(request);
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
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(username);
			params.add(password);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			username = (String) params.get(0);
			password = (String) params.get(1);
		}
	}

}
