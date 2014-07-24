package strat.mining.stratum.proxy.rest.dto;

import java.util.Map;

public class PoolSwitchingStrategyDTO {

	private String name;
	private String description;
	private Map<String, String> details;
	private Map<String, String> configurationParameters;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, String> getDetails() {
		return details;
	}

	public void setDetails(Map<String, String> details) {
		this.details = details;
	}

	public Map<String, String> getConfiguration() {
		return configurationParameters;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configurationParameters = configuration;
	}

}
