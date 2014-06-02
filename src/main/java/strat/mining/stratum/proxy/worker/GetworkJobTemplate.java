package strat.mining.stratum.proxy.worker;

public class GetworkJobTemplate {

	private Long version;
	private String hashPrevBlock;
	private String hashMerkleRoot;
	private Long time;
	private Long bits;
	private Long nonce;

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getHashPrevBlock() {
		return hashPrevBlock;
	}

	public void setHashPrevBlock(String hashPrevBlock) {
		this.hashPrevBlock = hashPrevBlock;
	}

	public String getHashMerkleRoot() {
		return hashMerkleRoot;
	}

	public void setHashMerkleRoot(String hashMerkleRoot) {
		this.hashMerkleRoot = hashMerkleRoot;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Long getBits() {
		return bits;
	}

	public void setBits(Long bits) {
		this.bits = bits;
	}

	public Long getNonce() {
		return nonce;
	}

	public void setNonce(Long nonce) {
		this.nonce = nonce;
	}

}
