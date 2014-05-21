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

import java.util.List;

public class UserDetailsDTO {

	private String name;
	private String firstConnectionDate;
	private String lastShareSubmitted;
	private Long acceptedHashesPerSeconds;
	private Long rejectedHashesPerSeconds;
	private List<WorkerConnectionDTO> connections;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstConnectionDate() {
		return firstConnectionDate;
	}

	public void setFirstConnectionDate(String firstConnectionDate) {
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

	public String getLastShareSubmitted() {
		return lastShareSubmitted;
	}

	public void setLastShareSubmitted(String lastShareSubmitted) {
		this.lastShareSubmitted = lastShareSubmitted;
	}

}
