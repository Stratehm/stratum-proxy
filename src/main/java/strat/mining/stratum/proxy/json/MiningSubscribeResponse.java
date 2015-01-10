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
public class MiningSubscribeResponse extends JsonRpcResponse {

	@JsonIgnore
	private List<Object> subscriptionDetails;
	@JsonIgnore
	private String extranonce1;
	@JsonIgnore
	private Integer extranonce2Size;

	public MiningSubscribeResponse() {
		super();
	}

	public MiningSubscribeResponse(JsonRpcResponse response) {
		super(response);
	}

	public List<Object> getSubscriptionDetails() {
		return subscriptionDetails;
	}

	public void setSubscriptionDetails(List<Object> subscriptionDetails) {
		this.subscriptionDetails = subscriptionDetails;
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
	public Object getResult() {
		if (super.getResult() == null) {
			List<Object> result = new ArrayList<Object>();
			super.setResult(result);
			result.add(subscriptionDetails);
			result.add(extranonce1);
			result.add(extranonce2Size);
		}
		return super.getResult();
	}

	@Override
	public void setResult(Object result) {
		super.setResult(result);
		if (result != null) {
			subscriptionDetails = getResultObjectAtIndex(0);
			extranonce1 = getResultObjectAtIndex(1);
			extranonce2Size = getResultObjectAtIndex(2);
		}
	}
}
