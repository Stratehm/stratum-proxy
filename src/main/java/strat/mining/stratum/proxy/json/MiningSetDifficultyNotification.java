package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningSetDifficultyNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "mining.set_difficulty";

	@JsonIgnore
	private Integer difficulty;

	public MiningSetDifficultyNotification() {
		super(METHOD_NAME);
	}

	public MiningSetDifficultyNotification(JsonRpcNotification notification) {
		super(notification);
	}

	public Integer getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Integer difficulty) {
		this.difficulty = difficulty;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(difficulty);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			difficulty = ((Number) getParamsObjectAtIndex(0)).intValue();
		}
	}

}
