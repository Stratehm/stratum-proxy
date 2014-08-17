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
package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningExtranonceSubscribeResponse extends JsonRpcResponse {

	@JsonIgnore
	private Boolean isSubscribed;

	public MiningExtranonceSubscribeResponse() {
		super();
	}

	public MiningExtranonceSubscribeResponse(JsonRpcResponse response) {
		super(response);
	}

	public Boolean getIsSubscribed() {
		return isSubscribed;
	}

	public void setIsSubscribed(Boolean isSubscribed) {
		this.isSubscribed = isSubscribed;
	}

	@Override
	public Object getResult() {
		if (super.getResult() == null) {
			List<Object> result = new ArrayList<Object>();
			super.setResult(result);
			result.add(isSubscribed);
		}
		return super.getResult();
	}

	@Override
	public void setResult(Object result) {
		super.setResult(result);
		if (result != null) {
			isSubscribed = (Boolean) result;
		}
	}

}
