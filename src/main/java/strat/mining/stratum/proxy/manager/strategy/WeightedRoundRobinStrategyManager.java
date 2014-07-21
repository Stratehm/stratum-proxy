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
package strat.mining.stratum.proxy.manager.strategy;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.Timer;

/**
 * A pool switching manager which uses a weighted round robin algotrithm.
 * 
 * @author Strat
 * 
 */
public class WeightedRoundRobinStrategyManager extends MonoCurrentPoolStrategyManager {

	public static final String NAME = "weightedRoundRobin";

	private static final Logger LOGGER = LoggerFactory.getLogger(WeightedRoundRobinStrategyManager.class);

	// The duration of a round. (In milliseconds)
	private static Integer roundDuration = ConfigurationManager.getInstance().getWeightedRoundRobinRoundDuration();

	private Map<Pool, AtomicLong> poolsRunningTimes;

	private volatile int totalWeight = 0;

	private CheckConnectionBindingsTask checkConnectionBindingsTask;

	// All counters will be reset at this date (the round is over)
	private Long endOfRoundTime;
	private Long startOfRoundTime;

	// Keep the time of the last pool switching function execution time.
	private Long lastExecutionTime;

	public WeightedRoundRobinStrategyManager(ProxyManager proxyManager) {
		super(proxyManager);
		this.startOfRoundTime = System.currentTimeMillis();
		this.poolsRunningTimes = Collections.synchronizedMap(new HashMap<Pool, AtomicLong>());

		resetRound();
	}

	/**
	 * Compute the total weight.
	 */
	private void computeTotalWeight(List<Pool> pools) {
		totalWeight = 0;

		for (Pool pool : pools) {
			if (pool.isReady()) {
				totalWeight += pool.getWeight();
			}
		}
		LOGGER.debug("Compute total weight: {}", totalWeight);
	}

	@Override
	public void onPoolAdded(Pool pool) {
		super.onPoolAdded(pool);
	}

	@Override
	public void onPoolRemoved(Pool pool) {
		unregisterPool(pool);
		super.onPoolRemoved(pool);
	}

	@Override
	public void onPoolUpdated(Pool pool) {
		super.onPoolUpdated(pool);
	}

	@Override
	public void onPoolDown(Pool pool) {
		super.onPoolDown(pool);
	}

	@Override
	public void onPoolStable(Pool pool) {
		totalWeight += pool.getWeight();
		super.onPoolStable(pool);
	}

	@Override
	public void stop() {
		if (checkConnectionBindingsTask != null) {
			checkConnectionBindingsTask.cancel();
		}
	}

	/**
	 * Compute and set the current pool.
	 */
	protected void computeCurrentPool() throws NoPoolAvailableException {
		// Cancel the task which call this function. It will be rescheduled at
		// the end of this function.
		if (checkConnectionBindingsTask != null) {
			checkConnectionBindingsTask.cancel();
		}

		List<Pool> pools = getProxyManager().getPools();
		LOGGER.debug("Compute current pool with pools: {}", pools);

		// Compute the total weight of all ready pools.
		computeTotalWeight(pools);

		// Sort the pools by weight. The highest weight will be active first.
		Collections.sort(pools, Collections.reverseOrder(new Comparator<Pool>() {
			public int compare(Pool o1, Pool o2) {
				return o1.getWeight().compareTo(o2.getWeight());
			}
		}));

		// Compute the time the current pool has mined. The time of mining is
		// the time between the last execution of this function and now.
		Long currentTime = System.currentTimeMillis();
		if (lastExecutionTime == null) {
			lastExecutionTime = currentTime;
		}
		Long timeSinceLastExecution = currentTime - lastExecutionTime;
		incrementCurrentPoolRunningTime(timeSinceLastExecution);
		lastExecutionTime = currentTime;

		// If the round is over, reset all counters.
		if (isRoundOver()) {
			LOGGER.debug("Round is over. Reset the round.");
			resetRound();
		}

		if (LOGGER.isDebugEnabled()) {
			synchronized (poolsRunningTimes) {
				for (Entry<Pool, AtomicLong> entry : poolsRunningTimes.entrySet()) {
					LOGGER.debug("Running times: {}={}", entry.getKey().getName(), entry.getValue().get());
				}
			}
		}

		// Then define the next pool to mine by computing the remaining time to
		// mine for each pool. The first pool that has not yet end its mining
		// time is the next pool to mine (can be the same as the current one).
		Long remainingMiningTime = 0L;
		Pool newPool = null;
		for (Pool pool : pools) {
			AtomicLong poolRunningTime = poolsRunningTimes.get(pool);
			if (poolRunningTime == null) {
				initPool(pool);
				poolRunningTime = poolsRunningTimes.get(pool);
			}
			Long expectedRunningTime = getExpectedExecutionTimeForPool(pool);
			remainingMiningTime = expectedRunningTime - poolRunningTime.get();
			LOGGER.debug("Expected running time for pool {}: {} ms", pool.getName(), expectedRunningTime);
			// If the pool has not ended its mining time, it is the pool that
			// will mine next (it may be the same as the current one)
			if (remainingMiningTime > 1) {
				newPool = pool;
				break;
			}
		}

		// If the new pool is null, there is no more pool available.
		if (newPool == null) {
			throw new NoPoolAvailableException("No pool available. " + pools);
		} else {
			// Else, call this function once again when the current pool mining
			// time is over.
			Timer.getInstance().schedule(new CheckConnectionBindingsTask(), remainingMiningTime);
			setCurrentPool(newPool);
		}

	}

	/**
	 * Return the time (in milliseconds) the given pool has to be the current
	 * one.
	 * 
	 * @param pool
	 * @return
	 */
	private Long getExpectedExecutionTimeForPool(Pool pool) {
		return (long) (((float) pool.getWeight() / (float) totalWeight) * roundDuration);
	}

	/**
	 * Return true if the round is over
	 * 
	 * @return
	 */
	private boolean isRoundOver() {
		return System.currentTimeMillis() >= endOfRoundTime;
	}

	/**
	 * Reset the round
	 * 
	 * @return
	 */
	private void resetRound() {
		LOGGER.debug("Reset the round.");
		startOfRoundTime = System.currentTimeMillis();
		endOfRoundTime = startOfRoundTime + roundDuration;

		// Reset all running time counters of pools
		List<Pool> pools = getProxyManager().getPools();
		for (Pool pool : pools) {
			AtomicLong runningTime = poolsRunningTimes.get(pool);
			if (runningTime != null) {
				runningTime.set(0);
			} else {
				initPool(pool);
			}
		}
	}

	/**
	 * Update the current pool running time.
	 */
	private void incrementCurrentPoolRunningTime(Long incrementValue) {
		if (getCurrentPool() != null) {
			AtomicLong runningTime = poolsRunningTimes.get(getCurrentPool());
			if (runningTime != null) {
				runningTime.addAndGet(incrementValue);
			} else {
				initPool(getCurrentPool());
			}
		}
	}

	/**
	 * Initialize the given pool.
	 */
	private void initPool(Pool pool) {
		poolsRunningTimes.put(pool, new AtomicLong(0));
	}

	/**
	 * Unregister the given pool.
	 * 
	 * @param pool
	 */
	private void unregisterPool(Pool pool) {
		poolsRunningTimes.remove(pool);
	}

	/**
	 * The task that schedule a pool switch if needed.
	 * 
	 * @author Strat
	 * 
	 */
	private class CheckConnectionBindingsTask extends Timer.Task {

		public CheckConnectionBindingsTask() {
			setName("CheckConnectionBindingsTask");
		}

		public void run() {
			checkConnectionsBinding();
		}

	}

	@Override
	public String getName() {
		return NAME;
	}

}
