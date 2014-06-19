package strat.mining.stratum.proxy.rest.dto;

import java.util.List;

public class HashrateHistoryDTO {

	private String name;
	private List<HashrateDTO> hashrates;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<HashrateDTO> getHashrates() {
		return hashrates;
	}

	public void setHashrates(List<HashrateDTO> hashrates) {
		this.hashrates = hashrates;
	}

}
