/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014-2015  Stratehm (stratehm@hotmail.com)
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
public class PoolDetailsDTO {

	private String name;
	private String host;
	private String username;
	private String password;
	private Boolean isReady;
	private Boolean isEnabled;
	private Boolean isStable;
	private String isReadySince;
	private Boolean isActive;
	private String isActiveSince;

	private String difficulty;
	private String extranonce1;
	private Integer extranonce2Size;
	private Integer workerExtranonce2Size;

	private Integer numberOfWorkerConnections;

	private Integer priority;
	private Integer weight;

	private Double acceptedDifficulty;
	private Double rejectedDifficulty;

	private Boolean isExtranonceSubscribeEnabled;

	private Long acceptedHashesPerSeconds;
	private Long rejectedHashesPerSeconds;

	private String lastStopCause;
	private String lastStopDate;

	private Boolean appendWorkerNames;
	private String workerNamesSeparator;
	private Boolean useWorkerPassword;

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

	public Boolean getIsReady() {
		return isReady;
	}

	public void setIsReady(Boolean isReady) {
		this.isReady = isReady;
	}

	public Boolean getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public Boolean getIsStable() {
		return isStable;
	}

	public void setIsStable(Boolean isStable) {
		this.isStable = isStable;
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

	public String getIsReadySince() {
		return isReadySince;
	}

	public void setIsReadySince(String isReadySince) {
		this.isReadySince = isReadySince;
	}

	public Double getAcceptedDifficulty() {
		return acceptedDifficulty;
	}

	public void setAcceptedDifficulty(Double acceptedDifficulty) {
		this.acceptedDifficulty = acceptedDifficulty;
	}

	public Double getRejectedDifficulty() {
		return rejectedDifficulty;
	}

	public void setRejectedDifficulty(Double rejectedDifficulty) {
		this.rejectedDifficulty = rejectedDifficulty;
	}

	public Boolean getIsExtranonceSubscribeEnabled() {
		return isExtranonceSubscribeEnabled;
	}

	public void setIsExtranonceSubscribeEnabled(Boolean isExtranonceSubscribeEnabled) {
		this.isExtranonceSubscribeEnabled = isExtranonceSubscribeEnabled;
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

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getIsActiveSince() {
		return isActiveSince;
	}

	public void setIsActiveSince(String isActiveSince) {
		this.isActiveSince = isActiveSince;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public String getLastStopCause() {
		return lastStopCause;
	}

	public void setLastStopCause(String lastStopCause) {
		this.lastStopCause = lastStopCause;
	}

	public String getLastStopDate() {
		return lastStopDate;
	}

	public void setLastStopDate(String lastStopDate) {
		this.lastStopDate = lastStopDate;
	}

	public Boolean getAppendWorkerNames() {
		return appendWorkerNames;
	}

	public void setAppendWorkerNames(Boolean appendWorkerNames) {
		this.appendWorkerNames = appendWorkerNames;
	}

	public String getWorkerNamesSeparator() {
		return workerNamesSeparator;
	}

	public void setWorkerNamesSeparator(String workerNamesSeparator) {
		this.workerNamesSeparator = workerNamesSeparator;
	}

	public Boolean getUseWorkerPassword() {
		return useWorkerPassword;
	}

	public void setUseWorkerPassword(Boolean useWorkerPassword) {
		this.useWorkerPassword = useWorkerPassword;
	}

}
