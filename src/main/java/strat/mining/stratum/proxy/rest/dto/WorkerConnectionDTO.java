package strat.mining.stratum.proxy.rest.dto;

import java.util.List;

public class WorkerConnectionDTO {

	private String remoteHost;
	private List<String> authorizedUsers;
	private Long acceptedHashesPerSeconds;
	private Long rejectedHashesPerSeconds;
	private String isActiveSince;

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public List<String> getAuthorizedUsers() {
		return authorizedUsers;
	}

	public void setAuthorizedUsers(List<String> authorizedUsers) {
		this.authorizedUsers = authorizedUsers;
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

	public String getIsActiveSince() {
		return isActiveSince;
	}

	public void setIsActiveSince(String isActiveSince) {
		this.isActiveSince = isActiveSince;
	}

}
