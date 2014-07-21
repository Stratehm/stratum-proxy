package strat.mining.stratum.proxy.manager.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * An abstract PoolSwitchingStrategyManager that allows one pool max to be
 * active at the same time.
 * 
 * @author Strat
 * 
 */
public abstract class MonoCurrentPoolStrategyManager implements PoolSwitchingStrategyManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(MonoCurrentPoolStrategyManager.class);

	private ProxyManager proxyManager;

	private Pool currentPool;

	public MonoCurrentPoolStrategyManager(ProxyManager proxyManager) {
		this.proxyManager = proxyManager;
	}

	/**
	 * Check if connections are bound to the good pool. If not, rebind the
	 * worker connection to the pool with highest priority.
	 */
	protected void checkConnectionsBinding() {
		LOGGER.debug("Check all worker connections binding.");
		List<WorkerConnection> workerConnections = proxyManager.getWorkerConnections();
		// Try to rebind connections only if there is at least one connection.
		if (workerConnections.size() > 0) {
			try {
				Pool oldCurrentPool = currentPool;
				computeCurrentPool();

				if (oldCurrentPool != currentPool) {
					LOGGER.info("Switching worker connections from pool {} to pool {}.", oldCurrentPool.getName(), currentPool.getName());
					for (WorkerConnection connection : workerConnections) {
						// If the connection is not bound to the poolToBind,
						// switch the pool.
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
				}

			} catch (NoPoolAvailableException e) {
				LOGGER.error("Failed to rebind workers connections. No pool is available. Closing all workers connections.", e);
				// If no more pool available, close all worker connections
				proxyManager.closeAllWorkerConnections();
				currentPool = null;
			}
		}
	}

	@Override
	public void onPoolAdded(Pool pool) {
		checkConnectionsBinding();
	}

	@Override
	public void onPoolRemoved(Pool pool) {
		checkConnectionsBinding();
	}

	@Override
	public void onPoolUpdated(Pool poolUpdated) {
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

	/**
	 * Compute and set the current pool.
	 * 
	 * @throws NoPoolAvailableException
	 */
	protected abstract void computeCurrentPool() throws NoPoolAvailableException;

	@Override
	public Pool getPoolForConnection(WorkerConnection connection) throws NoPoolAvailableException {
		if (currentPool == null) {
			computeCurrentPool();
		}
		return currentPool;
	}

	/**
	 * Return the current pool.
	 * 
	 * @return
	 */
	protected Pool getCurrentPool() {
		return currentPool;
	}

	/**
	 * Set the current pool.
	 */
	protected void setCurrentPool(Pool pool) {
		LOGGER.debug("Current pool: {}", pool.getName());
		if (pool != currentPool && currentPool != null) {
			currentPool.setIsActive(false);
		}
		if (pool != null) {
			pool.setIsActive(true);
		}
		currentPool = pool;
	}

	/**
	 * Return the proxy manager
	 * 
	 * @return
	 */
	protected ProxyManager getProxyManager() {
		return proxyManager;
	}

	@Override
	public Map<String, String> getConfigurationParameters() {
		// Return an empty map since there is no parameters
		return new HashMap<String, String>();
	}

	@Override
	public void stop() {
		// Nothing to do.
	}

}
