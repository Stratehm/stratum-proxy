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
package strat.mining.stratum.proxy.manager.strategy;

import strat.mining.stratum.proxy.exception.UnsupportedPoolSwitchingStrategyException;
import strat.mining.stratum.proxy.manager.ProxyManager;

public class PoolSwitchingStrategyFactory {

	private ProxyManager proxyManager;

	public PoolSwitchingStrategyFactory(ProxyManager proxyManager) {
		this.proxyManager = proxyManager;
	}

	public PoolSwitchingStrategyManager getPoolSwitchingStrategyManagerByName(String name) throws UnsupportedPoolSwitchingStrategyException {
		PoolSwitchingStrategyManager result = null;
		if (PriorityFailoverStrategyManager.NAME.equalsIgnoreCase(name)) {
			result = getPriorityFailoverStrategyManager();
		} else if (WeightedRoundRobinStrategyManager.NAME.equalsIgnoreCase(name)) {
			result = getWeightedRoundRobinStrategyManager();
		} else if (RandomStrategyManager.NAME.equalsIgnoreCase(name)) {
			result = getRandomStrategyManager();
		} else if (MultiPoolStrategyManager.NAME.equalsIgnoreCase(name)){
			result = getMultiPoolStrategyManager();
		} else {
			throw new UnsupportedPoolSwitchingStrategyException("No pool switching strategy found with name " + name
					+ ". Available strategy are: priorityFailover, weightedRoundRobin");
		}
		return result;
	}

	private PriorityFailoverStrategyManager getPriorityFailoverStrategyManager() {
		return new PriorityFailoverStrategyManager(proxyManager);
	}

	private WeightedRoundRobinStrategyManager getWeightedRoundRobinStrategyManager() {
		return new WeightedRoundRobinStrategyManager(proxyManager);
	}

	private RandomStrategyManager getRandomStrategyManager() {
		return new RandomStrategyManager(proxyManager);
	}

	private MultiPoolStrategyManager getMultiPoolStrategyManager() {
		return new MultiPoolStrategyManager(proxyManager);
	}
}
