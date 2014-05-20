package strat.mining.stratum.proxy.rest.dto;

import java.util.List;

public class UserDetailsDTO {

	private String name;
	private String creationDate;
	private String lastShareSubmitted;
	private Long acceptedHashesPerSeconds;
	private Long rejectedHashesPerSeconds;
	private List<WorkerConnectionDTO> connections;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public Long getAcceptedHashesPerSeconds() {
		return acceptedHashesPerSeconds;
	}

	public void setAcceptedHashesPerSeconds(Long acceptedHashesPerSeconds) {
		this.acceptedHashesPerSeconds = acceptedHashesPerSeconds;
	}

	public Long getRejectedHashesPerSeconds() {
		return rejectedHashesPerSeconds;
	}

	public void setRejectedHashesPerSeconds(Long rejectedHashesPerSeconds) {
		this.rejectedHashesPerSeconds = rejectedHashesPerSeconds;
	}

	public List<WorkerConnectionDTO> getConnections() {
		return connections;
	}

	public void setConnections(List<WorkerConnectionDTO> connections) {
		this.connections = connections;
	}

	public String getLastShareSubmitted() {
		return lastShareSubmitted;
	}

	public void setLastShareSubmitted(String lastShareSubmitted) {
		this.lastShareSubmitted = lastShareSubmitted;
	}

}
