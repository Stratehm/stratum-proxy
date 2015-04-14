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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailsDTO {

    private String name;
    private Long firstConnectionDate;
    private Long lastShareSubmitted;
    private Long acceptedHashesPerSeconds;
    private Long rejectedHashesPerSeconds;
    private Double acceptedDifficulty;
    private Double rejectedDifficulty;
    private Long acceptedShareNumber;
    private Long rejectedShareNumber;
    private List<WorkerConnectionDTO> connections;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getFirstConnectionDate() {
        return firstConnectionDate;
    }

    public void setFirstConnectionDate(Long firstConnectionDate) {
        this.firstConnectionDate = firstConnectionDate;
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

    public List<WorkerConnectionDTO> getConnections() {
        return connections;
    }

    public void setConnections(List<WorkerConnectionDTO> connections) {
        this.connections = connections;
    }

    public Long getLastShareSubmitted() {
        return lastShareSubmitted;
    }

    public void setLastShareSubmitted(Long lastShareSubmitted) {
        this.lastShareSubmitted = lastShareSubmitted;
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

    public Long getAcceptedShareNumber() {
        return acceptedShareNumber;
    }

    public void setAcceptedShareNumber(Long acceptedShareNumber) {
        this.acceptedShareNumber = acceptedShareNumber;
    }

    public Long getRejectedShareNumber() {
        return rejectedShareNumber;
    }

    public void setRejectedShareNumber(Long rejectedShareNumber) {
        this.rejectedShareNumber = rejectedShareNumber;
    }

}
