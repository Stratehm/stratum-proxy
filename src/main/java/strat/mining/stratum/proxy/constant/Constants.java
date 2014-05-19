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
package strat.mining.stratum.proxy.constant;

import strat.mining.stratum.proxy.Launcher;

public class Constants {

	public static final String DEFAULT_USERNAME = "19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi";
	public static final String DEFAULT_PASSWORD = "x";
	public static final Integer DEFAULT_STRATUM_LISTENING_PORT = 3333;
	public static final Integer DEFAULT_REST_LISTENING_PORT = 8888;
	public static final String DEFAULT_REST_LISTENING_ADDRESS = "0.0.0.0";

	public static final Integer DEFAULT_POOL_PORT = 3333;
	public static final Integer DEFAULT_POOL_CONNECTION_RETRY_DELAY = 5;
	public static final Integer DEFAULT_POOL_RECONNECTION_STABILITY_PERIOD = 5;
	public static final Integer DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT = 120;

	// In milli seconds. The time to wait the subscribe request before closing
	// the connection.
	public static final Integer DEFAULT_SUBSCRIBE_RECEIVE_TIMEOUT = 10000;

	// The size of a tail in bytes
	public static final Integer DEFAULT_EXTRANONCE1_TAIL_SIZE = 1;

	public static final String ERROR_MESSAGE_SUBSCRIBE_EXTRANONCE = "Method 'subscribe' not found for service 'mining.extranonce'";

	public static final String VERSION = "stratehm-stratum-proxy-" + Launcher.getVersion();

}
