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

	private static final int NONCE_POSITION = 152;
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
