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
package strat.mining.stratum.proxy.utils.mining;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;

import org.glassfish.grizzly.http.util.HexUtils;

import strat.mining.stratum.proxy.utils.ArrayUtils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

/**
 * An utility class for hashing.
 * 
 * @author Strat
 * 
 */
public final class SHA256HashingUtils {

	public static final BigDecimal DIFFICULTY_1_TARGET = new BigDecimal(new BigInteger(
			HexUtils.convert("00000000ffff0000000000000000000000000000000000000000000000000000")));

	// Array to prepand to a byte array to build a positive bigInteger
	private static final byte[] BIG_INTEGER_FAKE_SIGN_ARRAY = new byte[] { (byte) 0 };

	// The following constants are used to compute midstates.
	private static long A0 = 0x6a09e667L;
	private static long B0 = 0xbb67ae85L;
	private static long C0 = 0x3c6ef372L;
	private static long D0 = 0xa54ff53aL;
	private static long E0 = 0x510e527fL;
	private static long F0 = 0x9b05688cL;
	private static long G0 = 0x1f83d9abL;
	private static long H0 = 0x5be0cd19L;

	private static long K[] = { 0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L, 0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L, 0xd807aa98L,
			0x12835b01L, 0x243185beL, 0x550c7dc3L, 0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L, 0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L,
			0x240ca1ccL, 0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL, 0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L, 0xc6e00bf3L,
			0xd5a79147L, 0x06ca6351L, 0x14292967L, 0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L, 0x650a7354L, 0x766a0abbL, 0x81c2c92eL,
			0x92722c85L, 0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L, 0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L, 0x19a4c116L,
			0x1e376c08L, 0x2748774cL, 0x34b0bcb5L, 0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L, 0x748f82eeL, 0x78a5636fL, 0x84c87814L,
			0x8cc70208L, 0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L, };

	/**
	 * Compute the SHA256 midstate of the given 64 bytes of data and return the
	 * 32 bytes midstate. This algorithm is the Java implementation of the
	 * Python implementation got from the Slush0 stratum proxy.
	 * 
	 * @param data
	 * @return
	 */
	public static final byte[] midstateSHA256(byte[] data) {
		if (data.length != 64) {
			throw new IndexOutOfBoundsException("Data must be 64 bytes long");
		}

		LinkedList<Long> w = new LinkedList<>();
		for (int i = 0; i < 16; i++) {
			int dataIndex = i * 4;
			w.add(Longs.fromBytes((byte) 0, (byte) 0, (byte) 0, (byte) 0, data[dataIndex + 3], data[dataIndex + 2], data[dataIndex + 1],
					data[dataIndex]));
		}

		long a = A0;
		long b = B0;
		long c = C0;
		long d = D0;
		long e = E0;
		long f = F0;
		long g = G0;
		long h = H0;

		for (long k : K) {
			long s0 = rotateRight(a, 2) ^ rotateRight(a, 13) ^ rotateRight(a, 22);
			long s1 = rotateRight(e, 6) ^ rotateRight(e, 11) ^ rotateRight(e, 25);
			long ma = (a & b) ^ (a & c) ^ (b & c);
			long ch = (e & f) ^ ((~e) & g);

			h = addu32(h, w.get(0), k, ch, s1);
			d = addu32(d, h);
			h = addu32(h, ma, s0);

			long tempa = a;
			a = h;
			h = g;
			g = f;
			f = e;
			e = d;
			d = c;
			c = b;
			b = tempa;

			long w1 = w.get(1);
			long w14 = w.get(14);
			s0 = rotateRight(w1, 7) ^ rotateRight(w1, 18) ^ (w1 >> 3);
			s1 = rotateRight(w14, 17) ^ rotateRight(w14, 19) ^ (w14 >> 10);
			w.add(addu32(w.get(0), s0, w.get(9), s1));
			w.remove(0);
		}

		a = addu32(a, A0);
		b = addu32(b, B0);
		c = addu32(c, C0);
		d = addu32(d, D0);
		e = addu32(e, E0);
		f = addu32(f, F0);
		g = addu32(g, G0);
		h = addu32(h, H0);

		byte[] result = new byte[32];
		byte[] bytes = Longs.toByteArray(a);
		result[0] = bytes[7];
		result[1] = bytes[6];
		result[2] = bytes[5];
		result[3] = bytes[4];

		bytes = Longs.toByteArray(b);
		result[4] = bytes[7];
		result[5] = bytes[6];
		result[6] = bytes[5];
		result[7] = bytes[4];

		bytes = Longs.toByteArray(c);
		result[8] = bytes[7];
		result[9] = bytes[6];
		result[10] = bytes[5];
		result[11] = bytes[4];

		bytes = Longs.toByteArray(d);
		result[12] = bytes[7];
		result[13] = bytes[6];
		result[14] = bytes[5];
		result[15] = bytes[4];

		bytes = Longs.toByteArray(e);
		result[16] = bytes[7];
		result[17] = bytes[6];
		result[18] = bytes[5];
		result[19] = bytes[4];

		bytes = Longs.toByteArray(f);
		result[20] = bytes[7];
		result[21] = bytes[6];
		result[22] = bytes[5];
		result[23] = bytes[4];

		bytes = Longs.toByteArray(g);
		result[24] = bytes[7];
		result[25] = bytes[6];
		result[26] = bytes[5];
		result[27] = bytes[4];

		bytes = Longs.toByteArray(h);
		result[28] = bytes[7];
		result[29] = bytes[6];
		result[30] = bytes[5];
		result[31] = bytes[4];

		return result;
	}

	/**
	 * Used by the midstate calculation
	 * 
	 * @param i
	 * @param p
	 * @return
	 */
	private static final long rotateRight(long i, int p) {
		p &= 0x1F; // p mod 32
		return i >> p | ((i << (32 - p)) & 0xFFFFFFFFL);
	}

	/**
	 * Used by the midstate calculation
	 * 
	 * @param toAdd
	 * @return
	 */
	private static long addu32(Long... toAdd) {
		long result = 0;
		for (int i = 0; i < toAdd.length; i++) {
			result += toAdd[i];
		}

		return result & 0xFFFFFFFFL;
	}

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

	/**
	 * Return true if the block header SHA256 hash is below the given target.
	 * 
	 * @param target
	 * @param blockHeader
	 * 
	 * @return
	 */
	public static boolean isBlockHeaderSHA256HashBelowTarget(String blockHeader, BigInteger target) {
		BigInteger hash = getBlockHeaderHash(blockHeader);

		// Check that the hash is less than the target. (The hash is valid if
		// hash < target)
		boolean isBelowTarget = hash.compareTo(target) < 0;

		return isBelowTarget;
	}

	/**
	 * Compute the hash of the given block header
	 * 
	 * @param blockHeader
	 * @return
	 */
	public static BigInteger getBlockHeaderHash(String blockHeader) {
		// The block header is just composed of the 80 first bytes (the
		// remaining is just padding)
		byte[] blockHeaderBin = HexUtils.convert(blockHeader.substring(0, 160));

		// LittleEndian to BigEndian
		blockHeaderBin = ArrayUtils.swapBytes(blockHeaderBin, 4);

		// Compute the hash
		byte[] hashIntegerBytes = doubleSha256Hash(blockHeaderBin);

		// Convert the hashInteger to a 256 bits (32 bytes) bytes array
		byte[] hashBytes256Bits = new byte[32];
		Arrays.fill(hashBytes256Bits, (byte) 0);
		ArrayUtils.copyInto(hashIntegerBytes, hashBytes256Bits, 32 - hashIntegerBytes.length);

		// Then swap bytes from big-endian to little-endian
		hashBytes256Bits = ArrayUtils.swapBytes(hashBytes256Bits, 4);

		// And reverse the order of 4-bytes words (big-endian to little-endian
		// 256-bits integer)
		hashBytes256Bits = ArrayUtils.reverseWords(hashBytes256Bits, 4);

		// Build the integer (with a 0 value byte to fake an unsigned int (sign
		// bit to 0)
		BigInteger hashInteger = new BigInteger(org.apache.commons.lang.ArrayUtils.addAll(BIG_INTEGER_FAKE_SIGN_ARRAY, hashBytes256Bits));

		return hashInteger;
	}

	/**
	 * Compute and return the real difficulty of this share.
	 * 
	 * @return
	 */
	public static Double getRealShareDifficulty(String blockHeader) {
		BigInteger realDifficulty = BigInteger.ZERO;
		BigInteger hash = getBlockHeaderHash(blockHeader);
		realDifficulty = DIFFICULTY_1_TARGET.toBigInteger().divide(hash);
		return realDifficulty.doubleValue();
	}

}
