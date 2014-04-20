package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MiningSetDifficultyNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "mining.set_difficulty";

	private List<Object> params;

	@JsonIgnore
	private Integer difficulty;

	public MiningSetDifficultyNotification() {
		super();
		setMethod(METHOD_NAME);
	}

	public MiningSetDifficultyNotification(JsonRpcNotification notification) {
		super(notification);
		setId(METHOD_NAME);
		setParams(notification.getParams());
	}

	@Override
	public List<Object> getParams() {
		if (params == null) {
			params = new ArrayList<Object>();
			params.add(difficulty);
		}
		return params;
	}

	@Override
	public void setParams(List<Object> params) {
		this.params = params;
		if (params != null) {
			difficulty = (Integer) params.get(0);
		}
	}

}
