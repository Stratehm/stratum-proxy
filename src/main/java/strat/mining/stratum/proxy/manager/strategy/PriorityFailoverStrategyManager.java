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
import java.util.List;
import java.util.Map;

import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;

/**
 * A pool switching manager which uses a priority based strategy to manage pool
 * failover.
 * 
 * @author Strat
 * 
 */
public class PriorityFailoverStrategyManager extends MonoCurrentPoolStrategyManager {

	public static final String NAME = "priorityFailover";

	public static final String DESCRIPTION = "Always mine on the highest priority pool (highest priority is 0). Only switch pool when current pool fails or when a higher priority pool is back.";

	public PriorityFailoverStrategyManager(ProxyManager proxyManager) {
		super(proxyManager);

		checkConnectionsBinding();
	}

	@Override
	public void onPoolAdded(Pool pool) {
		// Set by default the priority to the lowest over all pools.
		if (pool.getPriority() == null) {
			int minPriority = getMinimumPoolPriority();
			pool.setPriority(minPriority + 1);
		}

		super.onPoolAdded(pool);

	}

	@Override
	public void onPoolUpdated(Pool poolUpdated) {
		List<Pool> pools = getProxyManager().getPools();
		int newPriority = poolUpdated.getPriority();
		for (Pool pool : pools) {
			// Move the priority of other pools with lower or
			// equals priority
			if (pool.getPriority() >= newPriority && !pool.equals(poolUpdated)) {
				pool.setPriority(pool.getPriority() + 1);
			}
		}

		super.onPoolUpdated(poolUpdated);
	}

	/**
	 * Return the minimal priority over all pools.
	 * 
	 * @param addPoolDTO
	 * @return
	 */
	private int getMinimumPoolPriority() {
		int minPriority = 0;
		List<Pool> pools = getProxyManager().getPools();
		for (Pool pool : pools) {
			if (pool.getPriority() > minPriority) {
				minPriority = pool.getPriority();
			}
		}
		return minPriority;
	}

	/**
	 * Compute the and set the current pool. Based on the pool priority and pool
	 * state.
	 * 
	 * @param connection
	 * @return
	 */
	@Override
	protected void computeCurrentPool() throws NoPoolAvailableException {
		List<Pool> pools = getProxyManager().getPools();
		Pool newPool = null;
		Collections.sort(pools, new Comparator<Pool>() {
			public int compare(Pool o1, Pool o2) {
				return o1.getPriority().compareTo(o2.getPriority());
			}
		});
		for (Pool pool : pools) {
			if (pool.isReady() && pool.isEnabled() && pool.isStable()) {
				newPool = pool;
				break;
			}
		}

		if (newPool == null) {
			throw new NoPoolAvailableException("No pool available. " + pools);
		} else {
			setCurrentPool(newPool);
		}
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Map<String, String> getDetails() {
		return super.getDetails();
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public void setParameter(String parameterKey, String value) {
		// No parameters can be changed.
	}

}
