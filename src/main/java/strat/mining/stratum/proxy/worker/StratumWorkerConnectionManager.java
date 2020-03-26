package strat.mining.stratum.proxy.worker;

import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;
import strat.mining.stratum.proxy.manager.proxy.ProxyManagerFactory;
import strat.mining.stratum.proxy.manager.proxy.ProxyManagerInterface;
import strat.mining.stratum.proxy.network.StratumConnection;
import strat.mining.stratum.proxy.pool.Pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StratumWorkerConnectionManager {

    public void onSubscribeRequest(MiningSubscribeRequest subscribeRequest, StratumConnection connection) {
        ProxyManagerInterface proxyManager = ProxyManagerFactory.getInstance().getProxy();
        List<Pool> pools = proxyManager.getPools();
        Map<Pool, MiningSubscribeResponse> responses = new HashMap<>();
        int size = pools.size();
        int i = 0;
        for (Pool pool: pools) {
            boolean latest = i++ == size - 1;
            MiningSubscribeResponse response = connection.onSubscribeRequest(subscribeRequest, pool, latest);
            responses.put(pool, response);
        }

        connection.setResponseList(responses);
    }
}
