package strat.mining.stratum.proxy.manager.strategy;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import strat.mining.stratum.proxy.dto.ConnectionQuota;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.network.StratumConnection;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.pool.Quota;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;
import strat.mining.stratum.proxy.worker.WorkerConnection;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Log4j
public class MultiPoolStrategyManager extends MonoCurrentPoolStrategyManager {

    private static final Integer NOMINAL_VALUE_TIME_SECONDS = 90;

    private static final Integer DELAY = 10000;

    public static final String NAME = "multiPool";

    public static final String DESCRIPTION = "";

    private Map<StratumConnection, ConnectionQuota> connections;

    private ProxyManager proxyManager;

    public MultiPoolStrategyManager(ProxyManager proxyManager) {
        super(proxyManager);
        this.proxyManager = proxyManager;
        this.connections = new HashMap<>();
    }

    @Override
    public void onPoolAdded(Pool pool) {
    }

    @Override
    public void onPoolUpdated(Pool poolUpdated) {
    }

    /**
     * @return
     */
    @Override
    protected void computeCurrentPool() throws NoPoolAvailableException {
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

    public Pool getPoolForConnection(WorkerConnection connection) throws NoPoolAvailableException {
        StratumWorkerConnection conn = (StratumWorkerConnection) connection;
        try {
            Quota firstQuota = getFirstQuota();
            Pool pool = firstQuota.getPool();
            this.connections.put(conn, ConnectionQuota.builder()
                    .quota(firstQuota)
                    .date(LocalDateTime.now())
                    .position(0)
                    .build());
            conn.setNextPool(getNextQuota(0).getPool());
            log.info("Running thread");
            run(conn); // TMP. Move somewhere
            return pool;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }

        throw new NoPoolAvailableException();
    }

    private void run(StratumWorkerConnection connection) {
        TimerTask timerTask = new TimerTask() {
            @SneakyThrows
            public void run() {
                log.info("MultiPool tick");
                // get connection current quota
                ConnectionQuota connectionQuota = connections.get(connection);
                if (isToSwitch(connectionQuota)) {
                    log.info("Switching pool");
                    try {
                        connection.close();
                        proxyManager.switchPoolForConnection(connection, connection.getNextPool());
                    } catch (ChangeExtranonceNotSupportedException e) {
                        log.info("Close connection since the on-the-fly extranonce change is not supported.");
                        connection.close();
                        proxyManager.onWorkerDisconnection(connection, e);
                        proxyManager.switchPoolForConnection(connection, connection.getNextPool());
                    }

                    int position = connectionQuota.getPosition();
                    Pool nextPool = getNextQuota(position).getPool();
                    connection.setNextPool(nextPool);
                    connectionQuota.setDate(LocalDateTime.now());
                    connectionQuota.setPosition((++position));
                    log.info("Current pool: " + connection.getPool().getName());
                    log.info("Next pool: " + nextPool.getName());
                }
            }
        };
        Thread thread = new Thread(() -> new Timer().scheduleAtFixedRate(timerTask, DELAY, 5000));
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isToSwitch(ConnectionQuota connectionQuota) {
        LocalDateTime date = connectionQuota.getDate();
        LocalDateTime current = LocalDateTime.now();
        Quota quota = connectionQuota.getQuota();
        int timeToWork = (NOMINAL_VALUE_TIME_SECONDS * quota.getQuota()) / 100;
        log.info("TimeToWork: " + timeToWork);

        return current.isAfter(date.plusSeconds(timeToWork));
    }

    private Quota getFirstQuota() throws Exception {
        if (proxyManager.getQuotas().size() == 0) {
            throw new Exception("Quotas == 0");
        }
        return proxyManager.getQuotas().get(0);
    }

    private Quota getNextQuota(int position) throws Exception {
        if (proxyManager.getQuotas().size() == 0) {
            throw new Exception("Quotas == 0");
        }
        if (proxyManager.getQuotas().size() <= position) {
            return proxyManager.getQuotas().get(0);
        }

        return proxyManager.getQuotas().get(position);
    }
}
