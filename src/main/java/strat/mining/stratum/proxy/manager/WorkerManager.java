package strat.mining.stratum.proxy.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.*;
import strat.mining.stratum.proxy.network.StratumConnection;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.pool.Quota;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;

import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Log4j
public class WorkerManager {
    private static WorkerManager instance;

    private ProxyManager proxyManager;

    private Map<StratumConnection, Map<Quota, LocalDateTime>> connections;

    public WorkerManager() {
        this.proxyManager = ProxyManager.getInstance();
    }

    public static WorkerManager getInstance() {
        if (instance == null) {
            instance = new WorkerManager();
        }
        return instance;
    }

    public void onSubscribeRequest(MiningSubscribeRequest request, StratumConnection stratumConnection) {
        Pool pool;
        try {
            StratumWorkerConnection connection = (StratumWorkerConnection) stratumConnection;
            Quota firstQuota = getFirstQuota();
            pool = firstQuota.getPool();
            String extranonce1Tail = pool.getFreeTail();
            Integer extranonce2Size = pool.getWorkerExtranonce2Size();
            // Send the subscribe response
            MiningSubscribeResponse response = new MiningSubscribeResponse();
            response.setId(request.getId());

            response.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
            response.setExtranonce2Size(extranonce2Size);
            response.setSubscriptionDetails(getSubscibtionDetails());

            connection.sendResponse(response);
            connection.setNextPool(getNextQuota(0).getPool());
            //switchPoolForConnection
            Map<Quota, LocalDateTime> data = new HashMap<>();
            data.put(firstQuota, LocalDateTime.now());
            this.connections.put(connection, data);
            // If the subscribe succeed, send the initial notifications (difficulty
            // and notify).
            connection.sendInitialNotifications();
            connection.sendGetVersion();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Build a list of subscription details.
     *
     * @return
     */
    private List<Object> getSubscibtionDetails() {
        List<Object> details = new ArrayList<>();
        List<Object> setDifficultySubscribe = new ArrayList<>();
        setDifficultySubscribe.add(MiningSetDifficultyNotification.METHOD_NAME);
        setDifficultySubscribe.add("b4b6693b72a50c7116db18d6497cac52");
        details.add(setDifficultySubscribe);
        List<Object> notifySubscribe = new ArrayList<>();
        notifySubscribe.add(MiningNotifyNotification.METHOD_NAME);
        notifySubscribe.add("ae6812eb4cd7735a302a8a9dd95cf71f");
        details.add(notifySubscribe);
        return details;
    }

    private Quota getFirstQuota() throws Exception {
        if (proxyManager.getQuotas().size() == 0) {
            throw new Exception("Quotas == 0");
        }
        return proxyManager.getQuotas().get(0);
    }

    private Quota getNextQuota(int currentPosition) throws Exception {
        if (proxyManager.getQuotas().size() == 0) {
            throw new Exception("Quotas == 0");
        }
        if (proxyManager.getQuotas().size() < ++currentPosition) {
            return proxyManager.getQuotas().get(0);
        }

        return proxyManager.getQuotas().get(currentPosition);
    }
}
