package strat.mining.stratum.proxy.utils;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.model.Share;

/**
 * Manage the worker connection hashrate
 * 
 * @author Strat
 * 
 */
public class WorkerConnectionHashrateDelegator {

	private Deque<Share> lastAcceptedShares;
	private Deque<Share> lastRejectedShares;
	private Integer samplingHashesPeriod = Constants.DEFAULT_WORKER_CONNECTION_HASHRATE_SAMPLING_PERIOD * 1000;

	public WorkerConnectionHashrateDelegator() {
		lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		lastRejectedShares = new ConcurrentLinkedDeque<Share>();
	}

	/**
	 * Return the of accepted hashes per seconds of the connection.
	 * 
	 * @return
	 */
	public double getAcceptedHashrate() {
		HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastAcceptedShares, samplingHashesPeriod);
	}

	/**
	 * Return the number of rejected hashes per seconds of the connection.
	 * 
	 * @return
	 */
	public double getRejectedHashrate() {
		HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastRejectedShares, samplingHashesPeriod);
	}

	/**
	 * Update the shares lists with the given share to compute hashrate
	 * 
	 * @param share
	 * @param isAccepted
	 */
	public void updateShareLists(Share share, boolean isAccepted) {
		if (isAccepted) {
			lastAcceptedShares.addLast(share);
			HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		} else {
			lastRejectedShares.addLast(share);
			HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		}
	}

	/**
	 * Set the sampling period to compute the hashrate of the connection. he
	 * period is in seconds.
	 * 
	 * @param samplingHashesPeriod
	 */
	public void setSamplingHashesPeriod(Integer samplingHashesPeriod) {
		this.samplingHashesPeriod = samplingHashesPeriod * 1000;
	}

}
