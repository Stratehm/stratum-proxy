package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MiningNotifyNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "mining.notify";

	private List<Object> params;

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
		super();
		setMethod(METHOD_NAME);
	}

	public MiningNotifyNotification(MiningNotifyNotification notification) {
		super(notification);
		setMethod(METHOD_NAME);
		setParams(notification.getParams());
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
		if (params == null) {
			params = new ArrayList<Object>();
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
		return params;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setParams(List<Object> params) {
		this.params = params;
		if (params != null) {
			jobId = (String) params.get(0);
			previousHash = (String) params.get(0);
			coinbase1 = (String) params.get(0);
			coinbase2 = (String) params.get(0);
			merkleBranches = (List<String>) params.get(0);
			bitcoinVersion = (String) params.get(0);
			networkDifficultyBits = (String) params.get(0);
			currentNTime = (String) params.get(0);
			cleanJobs = (Boolean) params.get(0);
		}
	}
}
