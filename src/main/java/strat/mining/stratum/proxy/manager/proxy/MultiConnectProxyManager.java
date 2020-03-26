package strat.mining.stratum.proxy.manager.proxy;

import lombok.extern.slf4j.Slf4j;
import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.manager.strategy.PoolSwitchingStrategyFactory;
import strat.mining.stratum.proxy.manager.strategy.PoolSwitchingStrategyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;
import strat.mining.stratum.proxy.worker.WorkerConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MultiConnectProxyManager extends ProxyManager {

    private ServerSocket serverSocket;

    private boolean closeRequested = false;

    private static MultiConnectProxyManager instance;

    private MultiConnectProxyManager() {
        super();
    }

    public static MultiConnectProxyManager getInstance() {
        if (instance == null) {
            instance = new MultiConnectProxyManager();
        }
        return instance;
    }

    /**
     * Start listening incoming connections on the given interface and port. If
     * bindInterface is null, bind to 0.0.0.0
     *
     * @param bindInterface
     * @param port
     * @throws IOException
     */
    public void startListeningIncomingConnections(String bindInterface, Integer port) throws IOException {
        if (bindInterface == null) {
            serverSocket = new ServerSocket(port, 0);
        } else {
            serverSocket = new ServerSocket(port, 0, InetAddress.getByName(bindInterface));
        }
        log.info("ServerSocket opened on {}.", serverSocket.getLocalSocketAddress());

        // Do not log the error if a close has been requested
        // (as the error is expected ans is part of the shutdown
        // process)
        Thread listeningThread = new Thread() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                    Socket incomingConnectionSocket = null;
                    try {
                        log.debug("Waiting for incoming connection on {}...", serverSocket.getLocalSocketAddress());
                        incomingConnectionSocket = serverSocket.accept();
                        incomingConnectionSocket.setTcpNoDelay(true);
                        incomingConnectionSocket.setKeepAlive(true);
                        log.info("New connection on {} from {}.", serverSocket.getLocalSocketAddress(),
                                incomingConnectionSocket.getRemoteSocketAddress());

                        StratumWorkerConnection workerConnection = new StratumWorkerConnection(incomingConnectionSocket, MultiConnectProxyManager.this);
                        workerConnection.setSamplingHashesPeriod(ConfigurationManager.getInstance().getConnectionHashrateSamplingPeriod());
                        workerConnection.startReading();
                    } catch (Exception e) {
                        // Do not log the error if a close has been requested
                        // (as the error is expected ans is part of the shutdown
                        // process)
                        if (!closeRequested) {
                            log.error("Error on the server socket {}.", serverSocket.getLocalSocketAddress(), e);
                        }
                    }
                }

                log.info("Stop to listen incoming connection on {}.", serverSocket.getLocalSocketAddress());
            }
        };
        listeningThread.setName("StratumProxyManagerSeverSocketListener");
        listeningThread.setDaemon(true);
        listeningThread.start();
    }

    @Override
    public Pool onSubscribeRequest(WorkerConnection connection, MiningSubscribeRequest request) throws NoPoolAvailableException {
        Pool pool = poolSwitchingStrategyManager.getPoolForConnection(connection);

        Set<WorkerConnection> workerConnections = getPoolWorkerConnections(pool);
        workerConnections.add(connection);
        this.workerConnections.add(connection);
        log.info("New WorkerConnection {} subscribed. {} connections active on pool {}.", connection.getConnectionName(),
                workerConnections.size(), pool.getName());

        return pool;
    }

    /**
     * Switch the given connection to the given pool.
     *
     * @param connection
     * @param newPool
     */
    @Override
    public void switchPoolForConnection(WorkerConnection connection, Pool newPool) throws TooManyWorkersException,
            ChangeExtranonceNotSupportedException {
        // If the old pool is the same as the new pool, do nothing.
        if (!newPool.equals(connection.getPool())) {
            // Remove the connection from the old pool connection list.
            Set<WorkerConnection> oldPoolConnections = getPoolWorkerConnections(connection.getPool());
            if (oldPoolConnections != null) {
                oldPoolConnections.remove(connection);
            }

            // Then rebind the connection to this pool. An exception is thrown
            // if the rebind fails since the connection does not support the
            // extranonce change.
            connection.rebindToPool(newPool);

            // And finally add the worker connection to the pool's worker
            // connections
            Set<WorkerConnection> newPoolConnections = getPoolWorkerConnections(newPool);
            newPoolConnections.add(connection);
        }
    }

}
