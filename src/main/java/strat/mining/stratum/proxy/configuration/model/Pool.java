package strat.mining.stratum.proxy.configuration.model;

import org.hibernate.validator.constraints.NotEmpty;

public class Pool {

	private String name;
	@NotEmpty
	private String host;
	// Not empty if appendWorkersName false
	private String user;
	// Not empty if useWorferPassword false
	private String password;

	private Boolean enableExtranonceSubscribe;
	private Boolean appendWorkerNames;
	private String workerNameSeparator;
	private String useWorkerPassword;

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

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getEnableExtranonceSubscribe() {
		return enableExtranonceSubscribe;
	}

	public void setEnableExtranonceSubscribe(Boolean enableExtranonceSubscribe) {
		this.enableExtranonceSubscribe = enableExtranonceSubscribe;
	}

	public Boolean getAppendWorkerNames() {
		return appendWorkerNames;
	}

	public void setAppendWorkerNames(Boolean appendWorkerNames) {
		this.appendWorkerNames = appendWorkerNames;
	}

	public String getWorkerNameSeparator() {
		return workerNameSeparator;
	}

	public void setWorkerNameSeparator(String workerNameSeparator) {
		this.workerNameSeparator = workerNameSeparator;
	}

	public String getUseWorkerPassword() {
		return useWorkerPassword;
	}

	public void setUseWorkerPassword(String useWorkerPassword) {
		this.useWorkerPassword = useWorkerPassword;
	}

}
