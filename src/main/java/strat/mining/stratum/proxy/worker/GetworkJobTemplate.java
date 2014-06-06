package strat.mining.stratum.proxy.worker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.glassfish.grizzly.http.util.HexUtils;
import org.glassfish.grizzly.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.utils.HashingUtils;

/**
 * The template of a Getwork job built from stratum notify values.
 * 
 * @author Strat
 * 
 */
public class GetworkJobTemplate {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetworkJobTemplate.class);

	private static final BigDecimal DIFFICULTY_1_TARGET = new BigDecimal(new BigInteger(
			HexUtils.convert("00000000ffff0000000000000000000000000000000000000000000000000000")));

	private static final BigDecimal DIFFICULTY_1_TARGET_SCRYPT = new BigDecimal(new BigInteger(
			HexUtils.convert("0000ffff00000000000000000000000000000000000000000000000000000000")));

	private static final String DEFAULT_TARGET = "00000000ffff0000000000000000000000000000000000000000000000000000";

	// A fake merkle root hash used to fill the templateData
	private static final byte[] FAKE_MERKLE_ROOT = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// Padding used to fill the block header until 128 bytes (always the same
	// bytes)
	private static final byte[] BLOCK_HEADER_PADDING = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x02, (byte) 0x00,
			(byte) 0x00 };

	// The default value for the nonce.
	private static final byte[] DEFAULT_NONCE = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	// The first index of the merkle root hash in the block header
	private static final int MERKLE_ROOT_BLOCK_HEADER_POSITION = 36;

	private String jobId;

	private byte[] version;
	private byte[] hashPrevBlock;
	private byte[] time;
	private byte[] bits;

	// Stratum parameters
	private List<String> merkleBranches;
	private String coinbase1;
	private String coinbase2;
	private String extranonce1;

	private byte[] templateData;

	// Flag to true when templateData has to be updated.
	private boolean isDataDirty = true;

	private double difficulty;
	private String target;

	public GetworkJobTemplate(String jobId, String version, String hashPrevBlock, String time, String bits, List<String> merkleBranches,
			String coinbase1, String coinbase2, String extranonce1) {
		this.jobId = jobId;
		this.merkleBranches = merkleBranches;
		this.coinbase1 = coinbase1;
		this.coinbase2 = coinbase2;
		this.extranonce1 = extranonce1;

		this.hashPrevBlock = HexUtils.convert(hashPrevBlock);
		this.version = HexUtils.convert(version);
		this.time = HexUtils.convert(time);
		this.bits = HexUtils.convert(bits);

		this.target = DEFAULT_TARGET;

		computeTemplateData();
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public void setVersion(String version) {
		this.version = HexUtils.convert(version);
		isDataDirty = true;
	}

	public void setHashPrevBlock(String hashPrevBlock) {
		this.hashPrevBlock = HexUtils.convert(hashPrevBlock);
		isDataDirty = true;
	}

	public void setTime(String time) {
		this.time = HexUtils.convert(time);
		isDataDirty = true;
	}

	public void setBits(String bits) {
		this.bits = HexUtils.convert(bits);
		isDataDirty = true;
	}

	public void setMerkleBranches(List<String> merkleBranches) {
		if (merkleBranches != null && merkleBranches.size() > 0) {
			this.merkleBranches = merkleBranches;
		}
	}

	public void setCoinbase1(String coinbase1) {
		this.coinbase1 = coinbase1;
	}

	public void setCoinbase2(String coinbase2) {
		this.coinbase2 = coinbase2;
	}

	/**
	 * Compute the template data array.
	 */
	private void computeTemplateData() {
		if (isDataDirty) {
			isDataDirty = false;
			// The block header is 128 Bytes long
			// 80 bytes of useful data and others as padding.
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128);

			try {
				// Build the template block header
				byteStream.write(version);
				byteStream.write(hashPrevBlock);
				byteStream.write(FAKE_MERKLE_ROOT);
				byteStream.write(time);
				byteStream.write(bits);
				byteStream.write(DEFAULT_NONCE);
				byteStream.write(BLOCK_HEADER_PADDING);

				templateData = byteStream.toByteArray();
			} catch (IOException e) {
				LOGGER.error(
						"Failed to update GetworkJobTemplate. version: {}, hashPrevBlock: {}, merkleRoot: {}, time: {}, bits: {}, nonce: {}, padding: {}.",
						version, hashPrevBlock, FAKE_MERKLE_ROOT, time, bits, DEFAULT_NONCE, BLOCK_HEADER_PADDING, e);
			}
		}

	}

	/**
	 * Return the merkleRoot and the data of this job based on the coinbase2
	 * value.
	 * 
	 * @param extranonce2
	 * @return The merkleRoot in the left member of the pair and the data in the
	 *         right member.
	 */
	public Pair<String, String> getData(String extranonce2) {
		computeTemplateData();

		// Build the merkleRoot with the given extranonce2
		byte[] bigEndianMerkleRootHash = buildMerkleRootHash(extranonce2);

		// Byte swap the merkleRoot with 4-bytes words.
		byte[] littleEndianMerkleRootHash = strat.mining.stratum.proxy.utils.ArrayUtils.swapBytes(bigEndianMerkleRootHash, 4);

		// Then build the data
		byte[] data = buildData(littleEndianMerkleRootHash);

		return new Pair<String, String>(HexUtils.convert(littleEndianMerkleRootHash), HexUtils.convert(data));
	}

	/**
	 * Build the getwork data based on the given littleEndianMerkleRootHash.
	 * 
	 * @param littleEndianMerkleRootHash
	 * @return
	 */
	private byte[] buildData(byte[] littleEndianMerkleRootHash) {
		// Clone the templateData
		byte[] data = ArrayUtils.clone(templateData);

		// Then copy the merkleRoot into the data.
		strat.mining.stratum.proxy.utils.ArrayUtils.copyInto(littleEndianMerkleRootHash, data, MERKLE_ROOT_BLOCK_HEADER_POSITION);
		return data;
	}

	/**
	 * Return the merkleRoot based on the extranonce2. The merkleRoot hash is in
	 * BigEndian. So all 32 bits word should be converted in LittleEndian.
	 * 
	 * @param extranonce2
	 * @return
	 */
	private byte[] buildMerkleRootHash(String extranonce2) {
		byte[] merkleRoot = buildCoinbaseHash(extranonce2);

		for (String merkleBranch : merkleBranches) {
			merkleRoot = HashingUtils.doubleSha256Hash(ArrayUtils.addAll(merkleRoot, HexUtils.convert(merkleBranch)));
		}

		return merkleRoot;
	}

	/**
	 * 
	 * @param extranonce2
	 * @return
	 */
	private byte[] buildCoinbaseHash(String extranonce2) {
		String coinbaseString = coinbase1 + extranonce1 + extranonce2 + coinbase2;
		byte[] rawCoinbase = HexUtils.convert(coinbaseString);
		return HashingUtils.doubleSha256Hash(rawCoinbase);
	}

	public double getDifficulty() {
		return difficulty;
	}

	/**
	 * Set the difficulty of the job and compute the target. If scrypt, divide
	 * the target by 2^16.
	 * 
	 * @param difficulty
	 * @param isScrypt
	 */
	public void setDifficulty(double difficulty, boolean isScrypt) {
		this.difficulty = difficulty;
		computeTarget(difficulty, isScrypt);
	}

	public String getTarget() {
		return target;
	}

	/**
	 * Compute the target based on the difficulty.
	 * 
	 * @param difficulty
	 * @param isScrypt
	 */
	private void computeTarget(double difficulty, boolean isScrypt) {
		BigDecimal difficulty1 = isScrypt ? DIFFICULTY_1_TARGET_SCRYPT : DIFFICULTY_1_TARGET;
		BigDecimal targetNumber = difficulty1.divide(BigDecimal.valueOf(difficulty));
		byte[] bigEndianTargetBytes = targetNumber.toBigInteger().toByteArray();

		// Build the target on 32 Bytes
		byte[] littleEndianTargetBytes = new byte[32];
		strat.mining.stratum.proxy.utils.ArrayUtils.copyInto(bigEndianTargetBytes, littleEndianTargetBytes, 32 - bigEndianTargetBytes.length);
		// Then swap bytes from big-endian to little-endian
		littleEndianTargetBytes = strat.mining.stratum.proxy.utils.ArrayUtils.swapBytes(littleEndianTargetBytes, 4);
		this.target = HexUtils.convert(littleEndianTargetBytes);
	}
}
