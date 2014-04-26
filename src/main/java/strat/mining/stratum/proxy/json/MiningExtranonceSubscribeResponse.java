package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningExtranonceSubscribeResponse extends JsonRpcResponse {

	@JsonIgnore
	private Boolean isSubscribed;

	public MiningExtranonceSubscribeResponse() {
		super();
	}

	public MiningExtranonceSubscribeResponse(JsonRpcResponse response) {
		super(response);
	}

	public Boolean getIsSubscribed() {
		return isSubscribed;
	}

	public void setIsSubscribed(Boolean isSubscribed) {
		this.isSubscribed = isSubscribed;
	}

	@Override
	public Object getResult() {
		if (super.getResult() == null) {
			List<Object> result = new ArrayList<Object>();
			super.setResult(result);
			result.add(isSubscribed);
		}
		return super.getResult();
	}

	@Override
	public void setResult(Object result) {
		super.setResult(result);
		if (result != null) {
			isSubscribed = (Boolean) result;
		}
	}

}
