/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
 */
package strat.mining.stratum.proxy.configuration.model;

public class Pool {

	private String name;
	private String host;
	// Not empty if appendWorkersName false
	private String user;
	// Not empty if useWorferPassword false
	private String password;

	private Boolean enableExtranonceSubscribe;
	private Boolean appendWorkerNames;
	private String workerNameSeparator;
	private Boolean useWorkerPassword;
	private Integer weight;

	private Boolean isEnabled;

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

	public Boolean getUseWorkerPassword() {
		return useWorkerPassword;
	}

	public void setUseWorkerPassword(Boolean useWorkerPassword) {
		this.useWorkerPassword = useWorkerPassword;
	}

	public Boolean getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

}
