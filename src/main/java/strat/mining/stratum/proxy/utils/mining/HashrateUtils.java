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

import java.util.Deque;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
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

        double averageNumberOfHashesPerShareAtDifficultyOne = Math.pow(2,
                32 - (int) (Math.log(ConfigurationManager.getInstance().getHashesPerShareDiff1Divider()) / Math.log(2)));

        double hashesPerSeconds = (totalDifficultyInSamplingPeriod / (samplingHashesPeriod / 1000)) * averageNumberOfHashesPerShareAtDifficultyOne;
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
