package strat.mining.stratum.proxy.manager.strategy;

import strat.mining.stratum.proxy.exception.UnsupportedPoolSwitchingStrategyException;
import strat.mining.stratum.proxy.manager.ProxyManager;

public final class PoolSwitchingStrategyFactory {

	public static PoolSwitchingStrategyManager getPoolSwitchingStrategyManagerByName(String name) throws UnsupportedPoolSwitchingStrategyException {
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

	private static PriorityFailoverStrategyManager getPriorityFailoverStrategyManager() {
		return new PriorityFailoverStrategyManager(ProxyManager.getInstance());
	}

	private static WeightedRoundRobinStrategyManager getWeightedRoundRobinStrategyManager() {
		return new WeightedRoundRobinStrategyManager(ProxyManager.getInstance());
	}
}
