package strat.mining.stratum.proxy.utils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

/**
 * An utility class for hashing.
 * 
 * @author Strat
 * 
 */
public final class HashingUtils {

	/**
	 * Apply a single sha256 round over the given data.
	 * 
	 * @param data
	 * @return
	 */
	public static final byte[] sha256Hash(byte[] data) {
		HashCode hashBytes = Hashing.sha256().hashBytes(data);
		return hashBytes.asBytes();
	}

	/**
	 * Apply two sha256 round over the given data.
	 * 
	 * @param data
	 * @return
	 */
	public static final byte[] doubleSha256Hash(byte[] data) {
		return sha256Hash(sha256Hash(data));
	}

}
