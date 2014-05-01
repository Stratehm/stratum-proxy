package strat.mining.stratum.proxy.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoolDetailsDTO {

	private String name;
	private String host;
	private String username;
	private String password;
	private Boolean isActive;
	private Boolean isEnabled;
	private String isActiveSince;

	private String difficulty;
	private String extranonce1;
	private Integer extranonce2Size;
	private Integer workerExtranonce2Size;

	private Integer numberOfWorkerConnections;

	private Integer priority;

	private Long acceptedDifficulty;
	private Long rejectedDifficulty;

	private Boolean isExtranonceSubscribeEnabled;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Boolean getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
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

	public Integer getWorkerExtranonce2Size() {
		return workerExtranonce2Size;
	}

	public void setWorkerExtranonce2Size(Integer workerExtranonce2Size) {
		this.workerExtranonce2Size = workerExtranonce2Size;
	}

	public Integer getNumberOfWorkerConnections() {
		return numberOfWorkerConnections;
	}

	public void setNumberOfWorkerConnections(Integer numberOfWorkerConnections) {
		this.numberOfWorkerConnections = numberOfWorkerConnections;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getIsActiveSince() {
		return isActiveSince;
	}

	public void setIsActiveSince(String isActiveSince) {
		this.isActiveSince = isActiveSince;
	}

	public Long getAcceptedDifficulty() {
		return acceptedDifficulty;
	}

	public void setAcceptedDifficulty(Long acceptedDifficulty) {
		this.acceptedDifficulty = acceptedDifficulty;
	}

	public Long getRejectedDifficulty() {
		return rejectedDifficulty;
	}

	public void setRejectedDifficulty(Long rejectedDifficulty) {
		this.rejectedDifficulty = rejectedDifficulty;
	}

	public Boolean getIsExtranonceSubscribeEnabled() {
		return isExtranonceSubscribeEnabled;
	}

	public void setIsExtranonceSubscribeEnabled(Boolean isExtranonceSubscribeEnabled) {
		this.isExtranonceSubscribeEnabled = isExtranonceSubscribeEnabled;
	}

}
