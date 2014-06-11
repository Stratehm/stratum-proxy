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
	 * Called when a long polling request has to be replied. jsonResponse
	 * 
	 * @param jsonResponse
	 */
	public abstract void onLongPollingOver();

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
