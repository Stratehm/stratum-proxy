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
package strat.mining.stratum.proxy.utils;

/**
 * Arrays utility class.
 * 
 * @author Strat
 * 
 */
public final class ArrayUtils {

	/**
	 * Return an array which contains the same data as dataToSwap but with byte
	 * reversed. The word length is used to know on which base the swap has to
	 * be done.
	 * 
	 * The wordByteLength has to be a multiple of the length of the given
	 * dataToSwap, else an IndexOutOfBoundException is thrown.
	 * 
	 * For example, with parameters: dataToSwap = B0 B1 B2 B3 B4 B5 B6 B7
	 * 
	 * wordByteLength=1 return B0 B1 B2 B3 B4 B5 B6 B7
	 * 
	 * wordByteLength=2 return B1 B0 B3 B2 B5 B4 B7 B6
	 * 
	 * wordByteLength=4 return B3 B2 B1 B0 B7 B6 B5 B4
	 * 
	 * wordByteLength=8 return B7 B6 B5 B4 B3 B2 B1 B0
	 * 
	 * @param dataToSwap
	 * @param wordByteLength
	 * @return
	 */
	public static final byte[] swapBytes(byte[] dataToSwap, int wordByteLength) throws IndexOutOfBoundsException {
		byte[] result = null;
		if (dataToSwap != null) {

			if (wordByteLength < 1 || dataToSwap.length % wordByteLength > 0) {
				throw new IndexOutOfBoundsException("The wordByteLength is not a multiple of input data. wordByteLength=" + wordByteLength
						+ ", inputDataSize=" + dataToSwap.length);
			}

			result = new byte[dataToSwap.length];

			for (int i = 0; i < dataToSwap.length; i += wordByteLength) {
				for (int resultOffset = 0, inputOffset = wordByteLength - 1; resultOffset < wordByteLength; resultOffset++, inputOffset--) {
					result[i + resultOffset] = dataToSwap[i + inputOffset];
				}
			}
		}

		return result;
	}

	/**
	 * Reverse the order of the words of the given dataToSwap array based on the
	 * wordByteLength.
	 * 
	 * For example, with parameters: dataToSwap = B0 B1 B2 B3 B4 B5 B6 B7
	 * 
	 * wordByteLength=1 return B7 B6 B5 B4 B3 B2 B1 B0
	 * 
	 * wordByteLength=2 return B6 B7 B4 B5 B2 B3 B0 B1
	 * 
	 * wordByteLength=4 return B4 B5 B6 B7 B0 B1 B2 B3
	 * 
	 * wordByteLength=8 return B0 B1 B2 B3 B4 B5 B6 B7
	 * 
	 * @param dataToSwap
	 * @param wordByteLength
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public static final byte[] reverseWords(byte[] dataToSwap, int wordByteLength) throws IndexOutOfBoundsException {
		byte[] result = null;
		if (dataToSwap != null) {

			if (wordByteLength < 1 || dataToSwap.length % wordByteLength > 0) {
				throw new IndexOutOfBoundsException("The wordByteLength is not a multiple of input data. wordByteLength=" + wordByteLength
						+ ", inputDataSize=" + dataToSwap.length);
			}

			result = new byte[dataToSwap.length];

			for (int inputIndex = 0, resultIndex = dataToSwap.length - wordByteLength; inputIndex < dataToSwap.length; inputIndex += wordByteLength, resultIndex -= wordByteLength) {
				for (int i = 0; i < wordByteLength; i++) {
					result[resultIndex + i] = dataToSwap[inputIndex + i];
				}
			}
		}

		return result;
	}

	/**
	 * Copy the toCopy array into the into Array. The copy start at the
	 * intoStartIndex in the into array.
	 * 
	 * Copy as far as possible. (For example, copy will be full if toCopy is
	 * longer than (into - intoStartIndex)).
	 * 
	 * @param toCopy
	 * @param into
	 * @param intoStartIndex
	 */
	public static final void copyInto(byte[] toCopy, byte[] into, int intoStartIndex) {
		int maxCopyIndex = into.length - intoStartIndex;
		for (int i = 0; i < toCopy.length && i <= maxCopyIndex; i++) {
			into[i + intoStartIndex] = toCopy[i];
		}
	}
}
