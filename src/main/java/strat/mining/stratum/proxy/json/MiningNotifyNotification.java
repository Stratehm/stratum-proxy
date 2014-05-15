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
public class MiningNotifyNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "mining.notify";

	@JsonIgnore
	private String jobId;
	@JsonIgnore
	private String previousHash;
	@JsonIgnore
	private String coinbase1;
	@JsonIgnore
	private String coinbase2;
	@JsonIgnore
	private List<String> merkleBranches;
	@JsonIgnore
	private String bitcoinVersion;
	@JsonIgnore
	private String networkDifficultyBits;
	@JsonIgnore
	private String currentNTime;
	@JsonIgnore
	private Boolean cleanJobs;

	public MiningNotifyNotification() {
		super(METHOD_NAME);
	}

	public MiningNotifyNotification(JsonRpcNotification notification) {
		super(notification);
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getPreviousHash() {
		return previousHash;
	}

	public void setPreviousHash(String previousHash) {
		this.previousHash = previousHash;
	}

	public String getCoinbase1() {
		return coinbase1;
	}

	public void setCoinbase1(String coinbase1) {
		this.coinbase1 = coinbase1;
	}

	public String getCoinbase2() {
		return coinbase2;
	}

	public void setCoinbase2(String coinbase2) {
		this.coinbase2 = coinbase2;
	}

	public List<String> getMerkleBranches() {
		return merkleBranches;
	}

	public void setMerkleBranches(List<String> merkleBranches) {
		this.merkleBranches = merkleBranches;
	}

	public String getBitcoinVersion() {
		return bitcoinVersion;
	}

	public void setBitcoinVersion(String bitcoinVersion) {
		this.bitcoinVersion = bitcoinVersion;
	}

	public String getNetworkDifficultyBits() {
		return networkDifficultyBits;
	}

	public void setNetworkDifficultyBits(String networkDifficultyBits) {
		this.networkDifficultyBits = networkDifficultyBits;
	}

	public String getCurrentNTime() {
		return currentNTime;
	}

	public void setCurrentNTime(String currentNTime) {
		this.currentNTime = currentNTime;
	}

	public Boolean getCleanJobs() {
		return cleanJobs;
	}

	public void setCleanJobs(Boolean cleanJobs) {
		this.cleanJobs = cleanJobs;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(jobId);
			params.add(previousHash);
			params.add(coinbase1);
			params.add(coinbase2);
			params.add(merkleBranches);
			params.add(bitcoinVersion);
			params.add(networkDifficultyBits);
			params.add(currentNTime);
			params.add(cleanJobs);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			jobId = getParamsObjectAtIndex(0);
			previousHash = getParamsObjectAtIndex(1);
			coinbase1 = getParamsObjectAtIndex(2);
			coinbase2 = getParamsObjectAtIndex(3);
			merkleBranches = getParamsObjectAtIndex(4);
			bitcoinVersion = getParamsObjectAtIndex(5);
			networkDifficultyBits = getParamsObjectAtIndex(6);
			currentNTime = getParamsObjectAtIndex(7);
			cleanJobs = getParamsObjectAtIndex(8);
		}
	}
}
