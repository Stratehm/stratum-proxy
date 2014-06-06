package strat.mining.stratum.proxy.worker;

/**
 * A getwork job to submit.
 * 
 * @author Strat
 * 
 */
public class GetworkJobSubmit {

	private static final int MERKLE_ROOT_POSITION = 72;
	private static final int MERKLE_ROOT_LENGTH = 64;

	private static final int TIME_POSITION = 136;
	private static final int TIME_LENGTH = 8;

	private static final int NONCE_POSITION = 256;
	private static final int NONCE_LENGTH = 8;

	private String merkleRoot;
	private String time;
	private String nonce;

	/**
	 * Create a getwork Job based on a Hex string. Extract all needed data.
	 * 
	 * @param data
	 */
	public GetworkJobSubmit(String data) {

		merkleRoot = data.substring(MERKLE_ROOT_POSITION, MERKLE_ROOT_POSITION + MERKLE_ROOT_LENGTH);
		time = data.substring(TIME_POSITION, TIME_POSITION + TIME_LENGTH);
		nonce = data.substring(NONCE_POSITION, NONCE_POSITION + NONCE_LENGTH);
	}

	public String getMerkleRoot() {
		return merkleRoot;
	}

	public String getTime() {
		return time;
	}

	public String getNonce() {
		return nonce;
	}

}
