package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GetworkRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "getwork";

	@JsonIgnore
	private String data;

	public GetworkRequest() {
		super(METHOD_NAME);
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			if (data != null) {
				params.add(data);
			}
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null && params.size() > 0) {
			data = (String) params.get(0);
		}
	}

}
