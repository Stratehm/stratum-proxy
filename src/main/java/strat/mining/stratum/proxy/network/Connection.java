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
package strat.mining.stratum.proxy.network;

import java.net.InetAddress;

public interface Connection {

	/**
	 * Close the connection
	 */
	public void close();

	/**
	 * Return the connection name.
	 * 
	 * @return
	 */
	public String getConnectionName();

	/**
	 * Return the remote address.
	 * 
	 * @return
	 */
	public InetAddress getRemoteAddress();

	/**
	 * Return the remote port.
	 * 
	 * @return
	 */
	public Integer getRemotePort();

}
