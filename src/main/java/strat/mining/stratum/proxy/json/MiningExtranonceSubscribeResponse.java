package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningExtranonceSubscribeResponse extends JsonRpcResponse {

	@JsonIgnore
	private List<Object> subscriptionDetails;

	public MiningExtranonceSubscribeResponse() {
		super();
	}

	public MiningExtranonceSubscribeResponse(JsonRpcResponse response) {
		super(response);
	}

	public List<Object> getSubscriptionDetails() {
		return subscriptionDetails;
	}

	public void setSubscriptionDetails(List<Object> subscriptionDetails) {
		this.subscriptionDetails = subscriptionDetails;
	}

	@Override
	public Object getResult() {
		if (super.getResult() == null) {
			List<Object> result = new ArrayList<Object>();
			super.setResult(result);
			result.add(subscriptionDetails);
		}
		return super.getResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setResult(Object result) {
		super.setResult(result);
		if (result != null) {
			List<Object> resultList = (List<Object>) result;
			subscriptionDetails = (List<Object>) resultList.get(0);
		}
	}

}
