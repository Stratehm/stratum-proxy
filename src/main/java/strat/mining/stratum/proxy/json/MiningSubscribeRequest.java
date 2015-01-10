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

import strat.mining.stratum.proxy.constant.Constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningSubscribeRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "mining.subscribe";

	public MiningSubscribeRequest() {
		super(METHOD_NAME);
	}

	public MiningSubscribeRequest(JsonRpcRequest request) {
		super(request);
	}

	@Override
	public List<Object> getParams() {
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(Constants.VERSION);
		return params;
	}

	@Override
	public void setParams(List<Object> params) {
		// Do nothing since there is no mutable parameters for this request
	}

}
