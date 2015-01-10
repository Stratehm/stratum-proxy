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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningSetExtranonceNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "mining.set_extranonce";

	@JsonIgnore
	private String extranonce1;
	@JsonIgnore
	private Integer extranonce2Size;

	public MiningSetExtranonceNotification() {
		super(METHOD_NAME);
	}

	public MiningSetExtranonceNotification(JsonRpcNotification notification) {
		super(notification);
	}

	public String getExtranonce1() {
		return extranonce1;
	}

	public void setExtranonce1(String extranonce1) {
		this.extranonce1 = extranonce1;
	}

	public Integer getExtranonce2Size() {
		return extranonce2Size;
	}

	public void setExtranonce2Size(Integer extranonce2Size) {
		this.extranonce2Size = extranonce2Size;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(extranonce1);
			params.add(extranonce2Size);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			extranonce1 = getParamsObjectAtIndex(0);
			extranonce2Size = ((Number) getParamsObjectAtIndex(1)).intValue();
		}
	}

}
