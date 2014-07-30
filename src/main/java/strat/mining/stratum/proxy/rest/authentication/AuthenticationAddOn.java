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
package strat.mining.stratum.proxy.rest.authentication;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;

public class AuthenticationAddOn implements AddOn {

	@Override
	public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
		// Get the index of HttpServerFilter in the HttpServerFilter filter
		// chain
		final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

		if (httpServerFilterIdx >= 0) {
			// Insert the AuthenticationFilter right after HttpServerFilter
			builder.add(httpServerFilterIdx + 1, new AuthenticationFilter());
		}
	}

}
