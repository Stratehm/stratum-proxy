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

	private static final Integer TASK_EXECUTION_DELAY = 1000;

	// The duration of a round. (In milliseconds)
	private static Integer roundDuration = ConfigurationManager.getInstance().getWeightedRoundRobinRoundDuration();

	private Map<Pool, AtomicLong> poolsRunningTimes;

	private volatile int totalWeight = 0;

	private PoolRunningTimeUpdater poolRunningTimeUpdater;
	private CheckConnectionBindindsTask checkConnectionBinfingsDelayTask;

	// All counters will be reset at this date (the round is over)
	private Long endOfRoundTime;
	private Long startOfRoundTime;

	public WeightedRoundRobinStrategyManager(ProxyManager proxyManager) {
		super(proxyManager);
		this.startOfRoundTime = System.currentTimeMillis();
		this.poolsRunningTimes = Collections.synchronizedMap(new HashMap<Pool, AtomicLong>());

		resetRound();

		// Start the pool running time updater.
		this.poolRunningTimeUpdater = new PoolRunningTimeUpdater();
		Timer.getInstance().schedule(poolRunningTimeUpdater, TASK_EXECUTION_DELAY);
	}

	/**
	 * Compute the total weight.
	 */
	private void computeTotalWeight(List<Pool> pools) {
		totalWeight = 0;

		for (Pool pool : pools) {
			if (pool.isActive()) {
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
		if (poolRunningTimeUpdater != null) {
			poolRunningTimeUpdater.cancel();
		}
	}

	/**
	 * Compute and set the current pool.
	 */
	protected void computeCurrentPool() throws NoPoolAvailableException {
		List<Pool> pools = getProxyManager().getPools();
		LOGGER.debug("Compute current pool with pools: {}", pools);

		computeTotalWeight(pools);
		Pool newPool = null;

		Collections.sort(pools, Collections.reverseOrder(new Comparator<Pool>() {
			public int compare(Pool o1, Pool o2) {
				return o1.getWeight().compareTo(o2.getWeight());
			}
		}));

		if (LOGGER.isDebugEnabled()) {
			synchronized (poolsRunningTimes) {
				LOGGER.debug("Running times: {}", poolsRunningTimes);
			}
		}

		Long remainingMiningTime = 0L;
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

		if (newPool == null) {
			throw new NoPoolAvailableException("No pool available. " + pools);
		} else {
			if (checkConnectionBinfingsDelayTask != null) {
				checkConnectionBinfingsDelayTask.cancel();
			} else {
				checkConnectionBinfingsDelayTask = new CheckConnectionBindindsTask();
			}
			Timer.getInstance().schedule(checkConnectionBinfingsDelayTask, remainingMiningTime);
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
		return System.currentTimeMillis() > endOfRoundTime;
	}

	/**
	 * Reset the round
	 * 
	 * @return
	 */
	private void resetRound() {
		long currentTime = System.currentTimeMillis();
		startOfRoundTime = currentTime;
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

		checkConnectionsBinding();
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
	 * The task that update the running times of pools and reset the manager
	 * when the round is over.
	 * 
	 * @author Strat
	 * 
	 */
	private class PoolRunningTimeUpdater extends Timer.Task {

		private Long lastExecutionTime = System.currentTimeMillis();

		public PoolRunningTimeUpdater() {
			setName("PoolRunningTimeUpdater");
		}

		public void run() {
			if (isRoundOver()) {
				resetRound();
			}

			long currentTime = System.currentTimeMillis();
			Long timeSinceLastExecution = currentTime - lastExecutionTime;

			incrementCurrentPoolRunningTime(timeSinceLastExecution);
			Timer.getInstance().schedule(this, TASK_EXECUTION_DELAY);
		}

	}

	/**
	 * The task that schedule a pool switch if needed.
	 * 
	 * @author Strat
	 * 
	 */
	private class CheckConnectionBindindsTask extends Timer.Task {

		public CheckConnectionBindindsTask() {
			setName("CheckConnectionBinfindsDelayTask");
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
