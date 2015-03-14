package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningSuggestDifficultyRequest extends JsonRpcRequest {

    public static final String METHOD_NAME = "mining.suggest_difficulty";

    @JsonIgnore
    private Double suggestedDifficulty;

    public MiningSuggestDifficultyRequest() {
        super(METHOD_NAME);
    }

    public MiningSuggestDifficultyRequest(JsonRpcRequest request) {
        super(request);
    }

    public Double getSuggestedDifficulty() {
        return suggestedDifficulty;
    }

    public void setSuggestedDifficulty(Double suggestedDifficulty) {
        this.suggestedDifficulty = suggestedDifficulty;
    }

    @Override
    public List<Object> getParams() {
        if (super.getParams() == null) {
            ArrayList<Object> params = new ArrayList<Object>();
            super.setParams(params);
            params.add(suggestedDifficulty);
        }
        return super.getParams();
    }

    @Override
    public void setParams(List<Object> params) {
        super.setParams(params);
        if (params != null) {
            suggestedDifficulty = (Double) params.get(0);
        }
    }

}
