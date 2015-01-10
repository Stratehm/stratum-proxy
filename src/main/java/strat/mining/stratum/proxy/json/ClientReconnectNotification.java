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
package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientReconnectNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "client.reconnect";

	private String host;
	private Integer port;

	public ClientReconnectNotification() {
		super(METHOD_NAME);
	}

	public ClientReconnectNotification(JsonRpcNotification notification) {
		super(notification);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(host);
			params.add(port);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			host = getParamsObjectAtIndex(0);
			if (getParamsObjectAtIndex(1) != null) {
				// Some pools send the port as an integer, other as a string.
				if (getParamsObjectAtIndex(1) instanceof Number) {
					port = ((Number) getParamsObjectAtIndex(1)).intValue();
				} else if (getParamsObjectAtIndex(1) instanceof String) {
					port = Integer.valueOf((String) getParamsObjectAtIndex(1));
				}
			} else {
				port = null;
			}

		}
	}

}
