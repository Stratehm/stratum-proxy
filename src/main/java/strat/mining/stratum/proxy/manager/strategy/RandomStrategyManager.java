package strat.mining.stratum.proxy.manager.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.manager.proxy.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.worker.WorkerConnection;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomStrategyManager extends PriorityFailoverStrategyManager{

    public static final String NAME = "randomStrategy";

    public static final String DESCRIPTION = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomStrategyManager.class);

    public RandomStrategyManager(ProxyManager proxyManager) {
        super(proxyManager);
    }

    /**
     * Compute and set the current pool.
     */
    protected void computeCurrentPool() throws NoPoolAvailableException {
        List<Pool> pools = getProxyManager().getPools();
        if (pools.isEmpty()) {
            throw new NoPoolAvailableException("No pool available. " + pools);
        } else {
            Pool pool = pools.get(ThreadLocalRandom.current().nextInt(pools.size()));
            setCurrentPool(pool);
        }
    }

    public Pool getPoolForConnection(WorkerConnection connection) throws NoPoolAvailableException {
        computeCurrentPool();

        return getCurrentPool();
    }
}
