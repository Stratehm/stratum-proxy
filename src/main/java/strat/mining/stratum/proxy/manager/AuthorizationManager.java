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
package strat.mining.stratum.proxy.manager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.NotFoundException;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.rest.dto.AddressDTO;
import strat.mining.stratum.proxy.rest.dto.UserNameDTO;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * Manage the authorization of users.
 * 
 * @author strat
 * 
 */
public class AuthorizationManager {

	private Set<String> bannedUserNames;
	private Set<InetAddress> bannedAddresses;

	public AuthorizationManager() {
		bannedUserNames = Collections.synchronizedSet(new HashSet<String>());
		bannedAddresses = Collections.synchronizedSet(new HashSet<InetAddress>());
	}

	/**
	 * Check if the given user and password are authorized to connect. Throws an
	 * exception if the user is not authorized.
	 * 
	 * @param username
	 * @param password
	 * 
	 */
	public void checkAuthorization(WorkerConnection connection, MiningAuthorizeRequest request) throws AuthorizationException {
		if (bannedUserNames.contains(request.getUsername())) {
			throw new AuthorizationException("The user " + request.getUsername() + " is banned.");
		} else if (bannedAddresses.contains(connection.getRemoteAddress())) {
			throw new AuthorizationException("The address " + connection.getRemoteAddress() + " is banned.");
		}
	}

	/**
	 * Ban the given user until the next proxy restart
	 * 
	 * @param username
	 */
	public void banUser(UserNameDTO username) {
		bannedUserNames.add(username.getUsername());
	}

	/**
	 * Unban the given user.
	 * 
	 * @param username
	 * @throws NotFoundException
	 */
	public void unbanUser(UserNameDTO username) throws NotFoundException {
		if (!bannedUserNames.remove(username.getUsername())) {
			throw new NotFoundException("User " + username.getUsername() + " not banned. Cannot unban.");
		}
	}

	/**
	 * Return the list of banned users.
	 * 
	 * @return
	 */
	public List<String> getBannedUsers() {
		List<String> result = new ArrayList<String>();
		synchronized (bannedUserNames) {
			result.addAll(bannedUserNames);
		}
		return result;
	}

	/**
	 * Ban the given address until the next proxy restart. The address can be an
	 * IP (v4 or v6) or a hostname.
	 * 
	 * @param username
	 * @return true if the address has been banned.
	 * @throws UnknownHostException
	 */
	public void banAddress(AddressDTO address) throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getByName(address.getAddress());
		bannedAddresses.add(inetAddress);
	}

	/**
	 * Unban the given address. The address can be an IP (v4 or v6) or a
	 * hostname.
	 * 
	 * @param username
	 * @throws UnknownHostException
	 */
	public void unbanAddress(AddressDTO address) throws UnknownHostException, NotFoundException {
		InetAddress inetAddress = InetAddress.getByName(address.getAddress());
		if (!bannedAddresses.remove(inetAddress)) {
			throw new NotFoundException("Address " + inetAddress.toString() + " not banned. Cannot unban.");
		}
	}

	/**
	 * Return the list of banned addresses.
	 * 
	 * @return
	 */
	public List<String> getBannedAddresses() {
		List<String> result = new ArrayList<String>();
		synchronized (bannedAddresses) {
			for (InetAddress address : bannedAddresses) {
				result.add(address.toString());
			}
		}
		return result;
	}
}
