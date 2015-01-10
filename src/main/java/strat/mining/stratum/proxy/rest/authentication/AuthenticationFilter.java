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
package strat.mining.stratum.proxy.rest.authentication;

import java.io.IOException;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.utils.Pair;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.exception.NoCredentialsException;
import strat.mining.stratum.proxy.utils.HttpUtils;

public class AuthenticationFilter extends BaseFilter {

	private static final String WWW_AUTHENTICATE_HEADR = "WWW-Authenticate";

	private static final String API_USER = ConfigurationManager.getInstance().getApiUser();
	private static final String API_PASSWORD = ConfigurationManager.getInstance().getApiPassword();

	@Override
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		NextAction nextAction = ctx.getInvokeAction();

		// Check the credentials only if an API_USER is set.
		if (API_USER != null) {
			HttpContent httpContent = ctx.getMessage();
			final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();

			try {
				Pair<String, String> credentials = HttpUtils.getCredentials(request);

				// If the the user match the API_USER and if the API_PASSWORD is
				// not set or the password math the API password, then grant the
				// access
				if (API_USER.equals(credentials.getFirst()) && (API_PASSWORD == null || API_PASSWORD.equals(credentials.getSecond()))) {
					nextAction = ctx.getInvokeAction();
				} else {
					nextAction = sendUnauthorizedResponse(ctx, request);
				}

			} catch (NoCredentialsException e) {
				// If there is no credentials, return an unauthorized response.
				nextAction = sendUnauthorizedResponse(ctx, request);
			}
		}

		return nextAction;
	}

	/**
	 * Reply immediately a 401 unauthorized response.
	 * 
	 * @param ctx
	 */
	private NextAction sendUnauthorizedResponse(FilterChainContext ctx, HttpRequestPacket request) {
		final HttpResponsePacket response = request.getResponse();
		response.setStatus(HttpStatus.UNAUTHORIZED_401);
		response.addHeader(WWW_AUTHENTICATE_HEADR, "Basic realm=stratum-proxy");

		ctx.write(HttpContent.builder(response).content(Buffers.EMPTY_BUFFER).last(true).build());
		return ctx.getStopAction();
	}
}
