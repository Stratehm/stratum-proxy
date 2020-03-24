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
package strat.mining.stratum.proxy.configuration.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

public class Configuration {

    private String logDirectory;
    private String logLevel;
    private String apiLogLevel;

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
    private Boolean validateGetworkShares;

    private String poolSwitchingStrategy;
    private Integer weightedRoundRobinRoundDuration;

    private Boolean disableGetwork;
    private Boolean disableStratum;
    private Boolean disableApi;
    private Boolean disableLogAppend;

    private String apiUser;
    private String apiPassword;
    private Boolean apiReadOnlyAccessEnabled;

    private Boolean apiEnableSsl;

    private Boolean logRealShareDifficulty;

    private Integer workerNumberLimit;

    private String ipVersion;

    private Double suggestedPoolDifficulty;

    @Valid
    private List<Pool> pools;

    @Valid
    private List<Quota> quotas;

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

    public String getApiLogLevel() {
        return apiLogLevel;
    }

    public void setApiLogLevel(String apiLogLevel) {
        this.apiLogLevel = apiLogLevel;
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

    public Boolean getValidateGetworkShares() {
        return validateGetworkShares;
    }

    public void setValidateGetworkShares(Boolean validateGetworkShares) {
        this.validateGetworkShares = validateGetworkShares;
    }

    public String getPoolSwitchingStrategy() {
        return poolSwitchingStrategy;
    }

    public void setPoolSwitchingStrategy(String poolSwitchingStrategy) {
        this.poolSwitchingStrategy = poolSwitchingStrategy;
    }

    public Integer getWeightedRoundRobinRoundDuration() {
        return weightedRoundRobinRoundDuration;
    }

    public void setWeightedRoundRobinRoundDuration(Integer weightedRoundRobinRoundDuration) {
        this.weightedRoundRobinRoundDuration = weightedRoundRobinRoundDuration;
    }

    public Boolean isDisableGetwork() {
        return disableGetwork;
    }

    public void setDisableGetwork(Boolean disableGetwork) {
        this.disableGetwork = disableGetwork;
    }

    public Boolean isDisableStratum() {
        return disableStratum;
    }

    public void setDisableStratum(Boolean disableStratum) {
        this.disableStratum = disableStratum;
    }

    public Boolean isDisableApi() {
        return disableApi;
    }

    public void setDisableApi(Boolean disableApi) {
        this.disableApi = disableApi;
    }

    public Boolean isDisableLogAppend() {
        return disableLogAppend;
    }

    public void setDisableLogAppend(Boolean disableLogAppend) {
        this.disableLogAppend = disableLogAppend;
    }

    public String getApiUser() {
        return apiUser;
    }

    public void setApiUser(String apiUser) {
        this.apiUser = apiUser;
    }

    public String getApiPassword() {
        return apiPassword;
    }

    public void setApiPassword(String apiPassword) {
        this.apiPassword = apiPassword;
    }

    public Boolean getApiReadOnlyAccessEnabled() {
        return apiReadOnlyAccessEnabled;
    }

    public void setApiReadOnlyAccessEnabled(Boolean apiReadOnlyAccessEnabled) {
        this.apiReadOnlyAccessEnabled = apiReadOnlyAccessEnabled;
    }

    public Integer getWorkerNumberLimit() {
        return workerNumberLimit;
    }

    public void setWorkerNumberLimit(Integer workerNumberLimit) {
        this.workerNumberLimit = workerNumberLimit;
    }

    public Boolean getDisableGetwork() {
        return disableGetwork;
    }

    public Boolean getDisableStratum() {
        return disableStratum;
    }

    public Boolean getDisableApi() {
        return disableApi;
    }

    public Boolean getDisableLogAppend() {
        return disableLogAppend;
    }

    public Boolean getApiEnableSsl() {
        return apiEnableSsl;
    }

    public void setApiEnableSsl(Boolean apiEnableSsl) {
        this.apiEnableSsl = apiEnableSsl;
    }

    public Boolean getLogRealShareDifficulty() {
        return logRealShareDifficulty;
    }

    public void setLogRealShareDifficulty(Boolean logRealShareDifficulty) {
        this.logRealShareDifficulty = logRealShareDifficulty;
    }

    public String getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(String ipVersion) {
        this.ipVersion = ipVersion;
    }

    public Double getSuggestedPoolDifficulty() {
        return suggestedPoolDifficulty;
    }

    public void setSuggestedPoolDifficulty(Double suggestedPoolDifficulty) {
        this.suggestedPoolDifficulty = suggestedPoolDifficulty;
    }

    public List<Quota> getQuotas() {
        return quotas;
    }

    public void setQuotas(List<Quota> quotas) {
        if (quotas == null) {
            quotas = new ArrayList<>();
        }
        this.quotas = quotas;
    }
}
