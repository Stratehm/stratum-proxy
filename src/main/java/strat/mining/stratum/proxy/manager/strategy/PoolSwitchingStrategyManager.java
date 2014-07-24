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

import java.util.Map;

import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * Represent a manager that manages the switch of connections between different
 * pools.
 * 
 * @author Strat
 * 
 */
public interface PoolSwitchingStrategyManager {

	/**
	 * Stop the strategy manager
	 */
	public void stop();

	/**
	 * Called when a pool is added.
	 * 
	 * @param pool
	 */
	public void onPoolAdded(Pool pool);

	/**
	 * Called when a pool is removed.
	 * 
	 * @param pool
	 */
	public void onPoolRemoved(Pool pool);

	/**
	 * Called when a pool is updated.
	 * 
	 * @param pool
	 */
	public void onPoolUpdated(Pool pool);

	/**
	 * Called when a pool goes DOWN.
	 * 
	 * @param pool
	 */
	public void onPoolDown(Pool pool);

	/**
	 * Called when a pool goes UP. (Should not be used since it is called before
	 * the stability test. onPoolStable should be used instead)
	 * 
	 * @param pool
	 */
	public void onPoolUp(Pool pool);

	/**
	 * Called when a pool is declared as stable.
	 * 
	 * @param pool
	 */
	public void onPoolStable(Pool pool);

	/**
	 * Return the pool to which the connection has to be bound.
	 * 
	 * @param connection
	 * @throws NoPoolAvailableException
	 */
	public Pool getPoolForConnection(WorkerConnection connection) throws NoPoolAvailableException;

	/**
	 * Return the parameters used to configure this strategy manager.
	 * 
	 * @return
	 */
	public Map<String, String> getConfigurationParameters();

	/**
	 * Return the details of this strategy.
	 * 
	 * @return
	 */
	public Map<String, String> getDetails();

	/**
	 * Return the name of this strategy.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Return the description of the strategy.
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * Set the value for the given parameter.
	 * 
	 * @param parameterKey
	 * @param value
	 */
	public void setParameter(String parameterKey, String value);

}
