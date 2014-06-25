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

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

public class Configuration {

	private String logDirectory;
	private String logLevel;

	private Integer stratumListenPort;
	private String stratumListenAddress;
	private Integer getworkListenPort;
	private String getworkListenAddress;
	private Integer apiListenPort;
	private String apiListenAddress;

	private Integer poolConnectionRetryDelay;
	private Integer poolReconnectStabilityPeriod;
	private Integer poolNoNotifyTimeout;
	private Boolean rejectReconnectOnDifferentHost;

	private Integer poolHashrateSamplingPeriod;
	private Integer userHashrateSamplingPeriod;
	private Integer connectionHashrateSamplingPeriod;

	private String databaseDirectory;
	private Integer hashrateDatabaseSamplingPeriod;
	private Integer hashrateDatabaseHistoryDepth;

	private Boolean isScrypt;
	private Boolean noMidstate;

	@Valid
	private List<Pool> pools;

	public String getLogDirectory() {
		return logDirectory;
	}

	public void setLogDirectory(String logDirectory) {
		this.logDirectory = logDirectory;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public Integer getStratumListenPort() {
		return stratumListenPort;
	}

	public void setStratumListenPort(Integer stratumListenPort) {
		this.stratumListenPort = stratumListenPort;
	}

	public String getStratumListenAddress() {
		return stratumListenAddress;
	}

	public void setStratumListenAddress(String stratumListenAddress) {
		this.stratumListenAddress = stratumListenAddress;
	}

	public Integer getGetworkListenPort() {
		return getworkListenPort;
	}

	public void setGetworkListenPort(Integer getworkListenPort) {
		this.getworkListenPort = getworkListenPort;
	}

	public String getGetworkListenAddress() {
		return getworkListenAddress;
	}

	public void setGetworkListenAddress(String getworkListenAddress) {
		this.getworkListenAddress = getworkListenAddress;
	}

	public Integer getApiListenPort() {
		return apiListenPort;
	}

	public void setApiListenPort(Integer apiListenPort) {
		this.apiListenPort = apiListenPort;
	}

	public String getApiListenAddress() {
		return apiListenAddress;
	}

	public void setApiListenAddress(String apiListenAddress) {
		this.apiListenAddress = apiListenAddress;
	}

	public Integer getPoolConnectionRetryDelay() {
		return poolConnectionRetryDelay;
	}

	public void setPoolConnectionRetryDelay(Integer poolConnectionRetryDelay) {
		this.poolConnectionRetryDelay = poolConnectionRetryDelay;
	}

	public Integer getPoolReconnectStabilityPeriod() {
		return poolReconnectStabilityPeriod;
	}

	public void setPoolReconnectStabilityPeriod(Integer poolReconnectStabilityPeriod) {
		this.poolReconnectStabilityPeriod = poolReconnectStabilityPeriod;
	}

	public Integer getPoolNoNotifyTimeout() {
		return poolNoNotifyTimeout;
	}

	public void setPoolNoNotifyTimeout(Integer poolNoNotifyTimeout) {
		this.poolNoNotifyTimeout = poolNoNotifyTimeout;
	}

	public Boolean getRejectReconnectOnDifferentHost() {
		return rejectReconnectOnDifferentHost;
	}

	public void setRejectReconnectOnDifferentHost(Boolean rejectReconnectOnDifferentHost) {
		this.rejectReconnectOnDifferentHost = rejectReconnectOnDifferentHost;
	}

	public Integer getPoolHashrateSamplingPeriod() {
		return poolHashrateSamplingPeriod;
	}

	public void setPoolHashrateSamplingPeriod(Integer poolHashrateSamplingPeriod) {
		this.poolHashrateSamplingPeriod = poolHashrateSamplingPeriod;
	}

	public Integer getUserHashrateSamplingPeriod() {
		return userHashrateSamplingPeriod;
	}

	public void setUserHashrateSamplingPeriod(Integer userHashrateSamplingPeriod) {
		this.userHashrateSamplingPeriod = userHashrateSamplingPeriod;
	}

	public Integer getConnectionHashrateSamplingPeriod() {
		return connectionHashrateSamplingPeriod;
	}

	public void setConnectionHashrateSamplingPeriod(Integer connectionHashrateSamplingPeriod) {
		this.connectionHashrateSamplingPeriod = connectionHashrateSamplingPeriod;
	}

	public Boolean getIsScrypt() {
		return isScrypt;
	}

	public void setIsScrypt(Boolean isScrypt) {
		this.isScrypt = isScrypt;
	}

	public List<Pool> getPools() {
		return pools;
	}

	public void setPools(List<Pool> pools) {
		if (pools == null) {
			pools = new ArrayList<>();
		}
		this.pools = pools;
	}

	public String getDatabaseDirectory() {
		return databaseDirectory;
	}

	public void setDatabaseDirectory(String databaseDirectory) {
		this.databaseDirectory = databaseDirectory;
	}

	public Integer getHashrateDatabaseSamplingPeriod() {
		return hashrateDatabaseSamplingPeriod;
	}

	public void setHashrateDatabaseSamplingPeriod(Integer hashrateDatabaseSamplingPeriod) {
		this.hashrateDatabaseSamplingPeriod = hashrateDatabaseSamplingPeriod;
	}

	public Integer getHashrateDatabaseHistoryDepth() {
		return hashrateDatabaseHistoryDepth;
	}

	public void setHashrateDatabaseHistoryDepth(Integer hashrateDatabaseHistoryDepth) {
		this.hashrateDatabaseHistoryDepth = hashrateDatabaseHistoryDepth;
	}

	public Boolean getNoMidstate() {
		return noMidstate;
	}

	public void setNoMidstate(Boolean noMidstate) {
		this.noMidstate = noMidstate;
	}

}
