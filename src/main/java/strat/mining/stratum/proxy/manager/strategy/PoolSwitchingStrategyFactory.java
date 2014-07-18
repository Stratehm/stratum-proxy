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
}
