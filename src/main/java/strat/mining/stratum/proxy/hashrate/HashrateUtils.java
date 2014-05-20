package strat.mining.stratum.proxy.hashrate;

import java.util.Deque;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.model.Share;

public final class HashrateUtils {

	/**
	 * Compute the hashrate from the given share list and the given sampling
	 * period.
	 * 
	 * @param shareList
	 * @return
	 */
	public static double getHashrateFromShareList(Deque<Share> shareList, int samplingHashesPeriod) {
		double totalDifficultyInSamplingPeriod = 0;
		// Purge the accepted shares list to work on fresh data
		purgeShareList(shareList, samplingHashesPeriod);
		for (Share share : shareList) {
			totalDifficultyInSamplingPeriod += share.getDifficulty();
		}

		double hashesPerSeconds = (totalDifficultyInSamplingPeriod / (samplingHashesPeriod / 1000))
				* Constants.AVERAGE_NUMBER_OF_HASHES_PER_SHARE_AT_DIFFICULTY_ONE;
		return hashesPerSeconds;
	}

	/**
	 * Purge the given share list form old shares
	 * 
	 * @param shareList
	 */
	public static void purgeShareList(Deque<Share> shareList, int samplingHashesPeriod) {
		long currentTime = System.currentTimeMillis();
		boolean isDelayReached = false;
		Share oldShare = shareList.peekFirst();

		while (!isDelayReached && oldShare != null) {
			// If the first share is too old, remove it.
			if (oldShare.getTime() + samplingHashesPeriod < currentTime) {
				shareList.pollFirst();
			} else {
				// Else the first share has not been removed, so stop the
				// loop.
				isDelayReached = true;
			}

			oldShare = shareList.peekFirst();
		}
	}
}
