package strat.mining.stratum.proxy.manager.proxy;

import lombok.extern.slf4j.Slf4j;
import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
}
