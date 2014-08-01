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
package strat.mining.stratum.proxy.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePoolDTO {

	private String poolName;
	private String poolHost;
	private String username;
	private String password;
	private Integer priority;
	private Integer weight;
	private Boolean enableExtranonceSubscribe;
	private Boolean appendWorkerNames;
	private String workerNameSeparator;
	private Boolean useWorkerPassword;

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public String getPoolHost() {
		return poolHost;
	}

	public void setPoolHost(String poolHost) {
		this.poolHost = poolHost;
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

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
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

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AddPoolDTO [poolName=");
		builder.append(poolName);
		builder.append(", poolHost=");
		builder.append(poolHost);
		builder.append(", username=");
		builder.append(username);
		builder.append(", password=");
		builder.append(password);
		builder.append(", priority=");
		builder.append(priority);
		builder.append(", weight=");
		builder.append(weight);
		builder.append(", enableExtranonceSubscribe=");
		builder.append(enableExtranonceSubscribe);
		builder.append(", appendWorkerNames=");
		builder.append(appendWorkerNames);
		builder.append(", workerNameSeparator=");
		builder.append(workerNameSeparator);
		builder.append(", useWorkerPassword=");
		builder.append(useWorkerPassword);
		builder.append("]");
		return builder.toString();
	}

}
