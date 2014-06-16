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
package strat.mining.stratum.proxy.callback;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Callback of long polling requests
 * 
 * @author Strat
 * 
 */
public abstract class LongPollingCallback {

	private static AtomicLong callbackCounter = new AtomicLong(0);

	private long id;

	public LongPollingCallback() {
		id = callbackCounter.getAndIncrement();
	}

	/**
	 * Called when a long polling request has to be replied.
	 * 
	 * @param jsonResponse
	 */
	public abstract void onLongPollingResume();

	/**
	 * Called when a long polling request has to be cancelled.
	 */
	public abstract void onLongPollingCancel(String causeMessage);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LongPollingCallback other = (LongPollingCallback) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
