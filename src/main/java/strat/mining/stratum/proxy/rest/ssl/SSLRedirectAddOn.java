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
package strat.mining.stratum.proxy.rest.ssl;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * An add on to grizzly to register the SSLRedirect filter
 * 
 * @author VMDev
 *
 */
public class SSLRedirectAddOn implements AddOn {

	@Override
	public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
		final int transportFilterIdx = builder.indexOfType(TransportFilter.class);

		if (transportFilterIdx >= 0) {
			builder.set(transportFilterIdx + 1, new SSLRedirectFilter());
		}

	}
}
