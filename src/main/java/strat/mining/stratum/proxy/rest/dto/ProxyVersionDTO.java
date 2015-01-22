package strat.mining.stratum.proxy.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyVersionDTO {

	private String proxyVersion;
	private String fullName;

	public String getProxyVersion() {
		return proxyVersion;
	}

	public void setProxyVersion(String proxyVersion) {
		this.proxyVersion = proxyVersion;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

}
