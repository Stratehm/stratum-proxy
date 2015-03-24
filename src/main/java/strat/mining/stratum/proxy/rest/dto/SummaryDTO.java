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
public class SummaryDTO {

    private String currentPoolName;
    private Long hashrate;
    private Long acceptedHashrate;
    private Long rejectedHashrate;
    private Integer totalErrors;
    private Long poolUptime;

    public String getCurrentPoolName() {
        return currentPoolName;
    }

    public void setCurrentPoolName(String currentPoolName) {
        this.currentPoolName = currentPoolName;
    }

    public Long getHashrate() {
        return hashrate;
    }

    public void setHashrate(Long hashrate) {
        this.hashrate = hashrate;
    }

    public Long getAcceptedHashrate() {
        return acceptedHashrate;
    }

    public void setAcceptedHashrate(Long acceptedHashrate) {
        this.acceptedHashrate = acceptedHashrate;
    }

    public Long getRejectedHashrate() {
        return rejectedHashrate;
    }

    public void setRejectedHashrate(Long rejectedHashrate) {
        this.rejectedHashrate = rejectedHashrate;
    }

    public Integer getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(Integer totalErrors) {
        this.totalErrors = totalErrors;
    }

    public Long getPoolUptime() {
        return poolUptime;
    }

    public void setPoolUptime(Long poolUptime) {
        this.poolUptime = poolUptime;
    }

}
