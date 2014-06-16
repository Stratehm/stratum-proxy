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
