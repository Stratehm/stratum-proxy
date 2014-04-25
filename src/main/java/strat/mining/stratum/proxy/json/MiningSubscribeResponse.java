package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningSubscribeResponse extends JsonRpcResponse {

	@JsonIgnore
	private List<Object> subscriptionDetails;
	@JsonIgnore
	private String extranonce1;
	@JsonIgnore
	private Integer extranonce2Size;

	public MiningSubscribeResponse() {
		super();
	}

	public MiningSubscribeResponse(JsonRpcResponse response) {
		super(response);
	}

	public List<Object> getSubscriptionDetails() {
		return subscriptionDetails;
	}

	public void setSubscriptionDetails(List<Object> subscriptionDetails) {
		this.subscriptionDetails = subscriptionDetails;
	}

	public String getExtranonce1() {
		return extranonce1;
	}

	public void setExtranonce1(String extranonce1) {
		this.extranonce1 = extranonce1;
	}

	public Integer getExtranonce2Size() {
		return extranonce2Size;
	}

	public void setExtranonce2Size(Integer extranonce2Size) {
		this.extranonce2Size = extranonce2Size;
	}

	@Override
	public Object getResult() {
		if (super.getResult() == null) {
			List<Object> result = new ArrayList<Object>();
			super.setResult(result);
			result.add(subscriptionDetails);
			result.add(extranonce1);
			result.add(extranonce2Size);
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
			extranonce1 = (String) resultList.get(1);
			extranonce2Size = (Integer) resultList.get(2);
		}
	}

}
