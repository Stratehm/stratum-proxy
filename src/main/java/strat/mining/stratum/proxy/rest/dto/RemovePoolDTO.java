package strat.mining.stratum.proxy.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemovePoolDTO {

	private String poolName;
	private Boolean keepHistory;

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public Boolean getKeepHistory() {
		return keepHistory;
	}

	public void setKeepHistory(Boolean keepHistory) {
		this.keepHistory = keepHistory;
	}

}
