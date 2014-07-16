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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * A pool switching manager which uses a priority based strategy to manage pool
 * failover.
 * 
 * @author Strat
 * 
 */
public class PriorityFailoverStrategyManager implements PoolSwitchingStrategyManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PriorityFailoverStrategyManager.class);

	private ProxyManager proxyManager;

	private Pool currentPool;

	public PriorityFailoverStrategyManager(ProxyManager proxyManager) {
		this.proxyManager = proxyManager;

		checkConnectionsBinding();
	}

	@Override
	public void onPoolAdded(Pool pool) {
		// Set by default the priority to the lowest over all pools.
		if (pool.getPriority() == null) {
			int minPriority = getMinimumPoolPriority();
			pool.setPriority(minPriority + 1);
		}

		checkConnectionsBinding();

	}

	@Override
	public void onPoolRemoved(Pool pool) {
		checkConnectionsBinding();
	}

	@Override
	public void onPoolUpdated(Pool poolUpdated) {
		List<Pool> pools = proxyManager.getPools();
		int newPriority = poolUpdated.getPriority();
		for (Pool pool : pools) {
			// Move the priority of other pools with lower or
			// equals priority
			if (pool.getPriority() >= newPriority && !pool.equals(poolUpdated)) {
				pool.setPriority(pool.getPriority() + 1);
			}
		}

		checkConnectionsBinding();
	}

	@Override
	public void onPoolDown(Pool pool) {
		checkConnectionsBinding();
	}

	@Override
	public void onPoolUp(Pool pool) {
		// Nothing to do.
	}

	@Override
	public void onPoolStable(Pool pool) {
		checkConnectionsBinding();

	}

	@Override
	public Pool getPoolForConnection(WorkerConnection connection) throws NoPoolAvailableException {
		if (currentPool == null) {
			computeCurrentPool();
		}
		return currentPool;
	}

	/**
	 * Return the minimal priority over all pools.
	 * 
	 * @param addPoolDTO
	 * @return
	 */
	private int getMinimumPoolPriority() {
		int minPriority = 0;
		List<Pool> pools = proxyManager.getPools();
		for (Pool pool : pools) {
			if (pool.getPriority() > minPriority) {
				minPriority = pool.getPriority();
			}
		}
		return minPriority;
	}

	/**
	 * Check if connections are bound to the good pool. If not, rebind the
	 * worker connection to the pool with highest priority.
	 */
	private void checkConnectionsBinding() {
		LOGGER.info("Check all worker connections binding.");
		List<WorkerConnection> workerConnections = proxyManager.getWorkerConnections();
		// Try to rebind connections only if there is at least one conenction.
		if (workerConnections.size() > 0) {
			try {
				computeCurrentPool();

				for (WorkerConnection connection : workerConnections) {
					// If the connection is not bound to the poolToBind, switch
					// the pool.
					if (!connection.getPool().equals(currentPool)) {
						try {
							proxyManager.switchPoolForConnection(connection, currentPool);
						} catch (TooManyWorkersException e) {
							LOGGER.warn("Failed to rebind worker connection {} on pool {}. Too many workers on this pool.",
									connection.getConnectionName(), currentPool.getName());
						} catch (ChangeExtranonceNotSupportedException e) {
							LOGGER.info("Close connection {} since the on-the-fly extranonce change is not supported.",
									connection.getConnectionName(), currentPool.getName());
							connection.close();
							proxyManager.onWorkerDisconnection(connection, e);
						}
					}
				}

			} catch (NoPoolAvailableException e) {
				LOGGER.error("Failed to rebind workers connections. No pool is available. Closing all workers connections.", e);
				// If no more pool available, close all worker connections
				proxyManager.closeAllWorkerConnections();
				currentPool = null;
			}
		}
	}

	/**
	 * Compute the and set the current pool. Based on the pool priority and pool
	 * state.
	 * 
	 * @param connection
	 * @return
	 */
	private void computeCurrentPool() throws NoPoolAvailableException {
		List<Pool> pools = proxyManager.getPools();
		Collections.sort(pools, new Comparator<Pool>() {
			public int compare(Pool o1, Pool o2) {
				return o1.getPriority().compareTo(o2.getPriority());
			}
		});
		for (Pool pool : pools) {
			if (pool.isActive() && pool.isEnabled() && pool.isStable()) {
				currentPool = pool;
				break;
			}
		}

		if (currentPool == null) {
			throw new NoPoolAvailableException("No pool available. " + pools);
		}
	}

}
