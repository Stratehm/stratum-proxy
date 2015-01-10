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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetworkResponse extends JsonRpcResponse {

	private Result result;

	public GetworkResponse() {
		super();
		result = new Result();
	}

	public GetworkResponse(JsonRpcResponse response) {
		super(response);
		result = new Result();
	}

	@JsonIgnore
	public String getMidstate() {
		return result.midstate;
	}

	public void setMidstate(String midstate) {
		result.midstate = midstate;
	}

	@JsonIgnore
	public String getData() {
		return result.data;
	}

	public void setData(String data) {
		result.data = data;
	}

	@JsonIgnore
	public String getHash1() {
		return result.hash1;
	}

	public void setHash1(String hash1) {
		result.hash1 = hash1;
	}

	@JsonIgnore
	public String getTarget() {
		return result.target;
	}

	public void setTarget(String target) {
		result.target = target;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	/**
	 * Represent the data of the Getwork response
	 * 
	 * @author Strat
	 * 
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonInclude(Include.NON_NULL)
	public class Result {
		private String midstate;
		private String data;
		private String hash1;
		private String target;

		public String getMidstate() {
			return midstate;
		}

		public void setMidstate(String midstate) {
			this.midstate = midstate;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public String getHash1() {
			return hash1;
		}

		public void setHash1(String hash1) {
			this.hash1 = hash1;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}
	}

}
