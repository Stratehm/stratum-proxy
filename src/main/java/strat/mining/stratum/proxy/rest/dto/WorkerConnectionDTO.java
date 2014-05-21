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

public class WorkerConnectionDTO {

	private String remoteHost;
	private List<String> authorizedUsers;
	private Long acceptedHashesPerSeconds;
	private Long rejectedHashesPerSeconds;
	private String isActiveSince;
	private String poolName;

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public List<String> getAuthorizedUsers() {
		return authorizedUsers;
	}

	public void setAuthorizedUsers(List<String> authorizedUsers) {
		this.authorizedUsers = authorizedUsers;
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

	public String getIsActiveSince() {
		return isActiveSince;
	}

	public void setIsActiveSince(String isActiveSince) {
		this.isActiveSince = isActiveSince;
	}

	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

}
