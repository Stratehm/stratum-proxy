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
package strat.mining.stratum.proxy.test;

import java.math.BigInteger;

import org.glassfish.grizzly.http.util.HexUtils;

import strat.mining.stratum.proxy.utils.ArrayUtils;
import strat.mining.stratum.proxy.utils.mining.ScryptHashingUtils;

public class Test {

	public static void main(String[] args) {
		// List<String> merkleBranches = new ArrayList<String>();
		// merkleBranches.add("32bac6b596b722100e6d0d5a451ec78b4252161603e5c140ce61ce29f1451ff9");
		// merkleBranches.add("0808cdd8a165d9151856258b7ce65c476220afc669e56076f5f7b541099de3d4");
		// merkleBranches.add("8ecfcf4d911ae073f90af0d63cafc37daf35947f766310df4a37e67339713ee4");
		// merkleBranches.add("3b33a4f5d3a406c5760415cd2c71442e082dffb261f1f1abcb905180143d7b63");
		// GetworkJobTemplate arf = new GetworkJobTemplate("1850", "00000002",
		// "72417428ad46bd3265c270b1d1c2dee6723136c98e3cff7be0b72b8ed00e012f",
		// "5396f810", "1b0616be", merkleBranches,
		// "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff230362e608062f503253482f0412f8965308",
		// "092f7374726174756d2f000000000100c6362a010000001976a914c8f58075fdf2ba12619f34d15385567e5a1cb99488ac00000000",
		// "0861e04900");
		//
		// arf.setDifficulty(700, true);
		//
		// long nanoTime = System.nanoTime();
		// for (int i = 0; i < 100000; i++) {
		// GetworkRequestResult data = arf.getData("000001");
		// }
		// nanoTime = System.nanoTime() - nanoTime;
		// System.out.println("Time: " + (nanoTime / 1000000) + " ms");

		// String splittedData =
		// "0000000206586cdb5fd4a83a94a352104ca66e9b558f996d23c54e720000000000000000c54c4a2c242902deca483072c9ed7ba89a0b210ca4351fc6e651c164";
		// byte[] hexSplitted = HexUtils.convert(splittedData);
		// // hexSplitted = ArrayUtils.reverseWords(hexSplitted, 4);
		//
		// byte[] hexHash = HashingUtils.midstateSHA256Python(hexSplitted);
		// String midstateComputed = HexUtils.convert(hexHash);
		// String midstate =
		// "ca9a8af983d2639900381eafe0b724d2ac3dd108f1e216af8cf2eefcc83b7854";

		// String target =
		// "00000000000000000000000000000000000000000000000000f0ff0f00000000";
		// BigInteger targetInteger = new BigInteger(HexUtils.convert(target));
		// boolean result = SHA256HashingUtils
		// .isBlockHeaderSHA256HashBelowTarget(
		// "00000002628786d847d7f2a8fca81a6efecc417927728df111ad8bd200000000000000008940c01a70f63c3d68b32e1df36c737f14851923fa45afb4a04dd36da2e2229153aab2eb1851aba267d0f624000000800000000000000000000000000000000000000000000000000000000000000000000000000000000080020000",
		// targetInteger);

		// String littleEndianBlockHeaderHex =
		// "01000000ae178934851bfa0e83ccb6a3fc4bfddff3641e104b6c4680c31509074e699be2bd672d8d2199ef37a59678f92443083e3b85edef8b45c71759371f823bab59a97126614f44d5001d45920180";
		// byte[] binaryBlockHeader =
		// HexUtils.convert(littleEndianBlockHeaderHex);
		// byte[] scryptHash = ScryptHashingUtils.scryptHash(binaryBlockHeader);
		//
		// long start = System.currentTimeMillis();
		// for (int i = 0; i < 1000; i++) {
		// scryptHash = ScryptHashingUtils.scryptHash(binaryBlockHeader);
		// }
		// System.out.println(System.currentTimeMillis() - start);

		String blockHeaderHex = "00000001c611fe9368eff46ba671b9510a0a4e27d845d5071b782695bf42326b5759dbafba46e9ccf3bd7c4f1efdb822237220527a83abaf263e6200ce46cc940008ebe6540c547c1b6ab9edfdbeb299";

		byte[] binaryBlockHeader = HexUtils.convert(blockHeaderHex);
		byte[] littleEndianHeaderBinary = ArrayUtils.swapBytes(binaryBlockHeader, 4);

		byte[] scryptHash = ScryptHashingUtils.scryptHash(littleEndianHeaderBinary);

		String rawHash = HexUtils.convert(scryptHash);
		System.out.println(rawHash);

		String bigEndian = HexUtils.convert(ArrayUtils.swapBytes(scryptHash, 4));
		System.out.println(bigEndian);

		String bigEndianReversed = HexUtils.convert(ArrayUtils.reverseWords(ArrayUtils.swapBytes(scryptHash, 4), 4));
		System.out.println(bigEndianReversed);

		BigInteger hashInt = new BigInteger(org.apache.commons.lang.ArrayUtils.addAll(new byte[] { (byte) 0 },
				ArrayUtils.reverseWords(ArrayUtils.swapBytes(scryptHash, 4), 4)));

		BigInteger diff1Int = ScryptHashingUtils.DIFFICULTY_1_TARGET.toBigInteger();

		BigInteger diff = diff1Int.divide(hashInt);

	}
}
