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
public class MiningSubmitRequest extends JsonRpcRequest {

	public static final String METHOD_NAME = "mining.submit";

	@JsonIgnore
	private String workerName;
	@JsonIgnore
	private String jobId;
	@JsonIgnore
	private String extranonce2;
	@JsonIgnore
	private String ntime;
	@JsonIgnore
	private String nonce;

	public MiningSubmitRequest() {
		super(METHOD_NAME);
	}

	public MiningSubmitRequest(JsonRpcRequest request) {
		super(request);
	}

	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getExtranonce2() {
		return extranonce2;
	}

	public void setExtranonce2(String extranonce2) {
		this.extranonce2 = extranonce2;
	}

	public String getNtime() {
		return ntime;
	}

	public void setNtime(String ntime) {
		this.ntime = ntime;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			ArrayList<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(workerName);
			params.add(jobId);
			params.add(extranonce2);
			params.add(ntime);
			params.add(nonce);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			workerName = (String) params.get(0);
			jobId = (String) params.get(1);
			extranonce2 = (String) params.get(2);
			ntime = (String) params.get(3);
			nonce = (String) params.get(4);
		}
	}

}
