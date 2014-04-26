package strat.mining.stratum.proxy.manager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ResponseReceivedCallback;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSetExtranonceNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * Manage connections (Pool and Worker) and build some stats.
 * 
 * @author Strat
 * 
 */
public class StratumProxyManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(StratumProxyManager.class);

	private ServerSocket serverSocket;
	private Thread listeningThread;

	private List<Pool> pools;

	private List<WorkerConnection> workerConnections;

	private Map<String, User> users;

	private Map<Pool, List<WorkerConnection>> poolWorkerConnections;

	public StratumProxyManager(List<Pool> pools) {
		this.pools = Collections.synchronizedList(new ArrayList<Pool>(pools));
		this.workerConnections = Collections.synchronizedList(new ArrayList<WorkerConnection>());
		this.users = Collections.synchronizedMap(new HashMap<String, User>());
		this.poolWorkerConnections = Collections.synchronizedMap(new HashMap<Pool, List<WorkerConnection>>());
	}

	/**
	 * Start all pools.
	 */
	public void startPools() {
		synchronized (pools) {
			for (Pool pool : pools) {
				try {
					pool.startPool(this);
				} catch (Exception e) {
					LOGGER.error("Failed to start the pool {}.", pool, e);
				}
			}
		}
	}

	/**
	 * Stop all pools
	 */
	public void stopPools() {
		synchronized (pools) {
			for (Pool pool : pools) {
				pool.stopPool();
			}
		}
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
		LOGGER.info("ServerSocket opened on {}.", serverSocket.getLocalSocketAddress());

		listeningThread = new Thread() {
			public void run() {
				while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
					Socket incomingConnectionSocket = null;
					try {
						LOGGER.debug("Waiting for incoming connection on {}...", serverSocket.getLocalSocketAddress());
						incomingConnectionSocket = serverSocket.accept();
						incomingConnectionSocket.setTcpNoDelay(true);
						incomingConnectionSocket.setKeepAlive(true);
						LOGGER.info("New connection on {} from {}.", serverSocket.getLocalSocketAddress(),
								incomingConnectionSocket.getRemoteSocketAddress());
						WorkerConnection workerConnection = new WorkerConnection(incomingConnectionSocket, StratumProxyManager.this);
						workerConnection.startReading();
					} catch (Exception e) {
						LOGGER.error("Error on the server socket {}.", serverSocket.getLocalSocketAddress(), e);
					}
				}

				LOGGER.info("Stop to listen incoming connection on {}.", serverSocket.getLocalSocketAddress());
			}
		};

		listeningThread.start();
	}

	/**
	 * Stop to listen incoming connections
	 */
	public void stopListeningIncomingConnections() {
		if (serverSocket != null) {
			LOGGER.info("Closing the server socket on {}.", serverSocket.getLocalSocketAddress());
			try {
				serverSocket.close();
			} catch (Exception e) {
				LOGGER.error("Failed to close serverSocket on {}.", serverSocket.getLocalSocketAddress(), e);
			}
		}
	}

	/**
	 * Close all existing workerConnections
	 */
	public void closeAllWorkerConnections() {
		synchronized (workerConnections) {
			for (WorkerConnection connection : workerConnections) {
				connection.close();
			}
		}
	}

	/**
	 * To call when a subscribe request is received on a worker connection.
	 * Return the pool on which the connection is bound.
	 * 
	 * @param connection
	 * @param request
	 */
	public Pool onSubscribeRequest(WorkerConnection connection, MiningSubscribeRequest request) throws NoPoolAvailableException {
		Pool pool = getPoolForNewConnection();

		List<WorkerConnection> workerConnections = poolWorkerConnections.get(pool);
		if (workerConnections == null) {
			workerConnections = Collections.synchronizedList(new ArrayList<WorkerConnection>());
			poolWorkerConnections.put(pool, workerConnections);
		}
		workerConnections.add(connection);
		LOGGER.info("New WorkerConnection {} subscribed. {} connections active.", connection.getConnectionName(), workerConnections.size());

		return pool;
	}

	/**
	 * To call when an authorize request is received.
	 * 
	 * @param connection
	 * @param request
	 */
	public boolean onAuthorizeRequest(WorkerConnection connection, MiningAuthorizeRequest request) {
		boolean isAuthorized = true;
		User user = users.get(request.getUsername());
		if (user == null) {
			user = new User();
			users.put(request.getUsername(), user);
		}
		return isAuthorized;
	}

	/**
	 * To call when a submit request is received from a worker connection.
	 * 
	 * 
	 * @param workerConnection
	 * @param workerRequest
	 */
	public void onSubmitRequest(final WorkerConnection workerConnection, final MiningSubmitRequest workerRequest) {
		MiningSubmitRequest poolRequest = new MiningSubmitRequest();
		poolRequest.setExtranonce2(workerRequest.getExtranonce2());
		poolRequest.setJobId(workerRequest.getJobId());
		poolRequest.setNonce(workerRequest.getNonce());
		poolRequest.setNtime(workerRequest.getNtime());
		poolRequest.setWorkerName(workerConnection.getPool().getUsername());

		workerConnection.getPool().submitShare(poolRequest, new ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>() {
			public void onResponseReceived(MiningSubmitRequest request, MiningSubmitResponse response) {
				workerConnection.onPoolSubmitResponse(workerRequest, response);
			}
		});
	}

	/**
	 * Called when a pool set the difficulty.
	 * 
	 * @param pool
	 * @param setDifficulty
	 */
	public void onPoolSetDifficulty(Pool pool, MiningSetDifficultyNotification setDifficulty) {
		LOGGER.info("Set difficulty {} on pool {}.", setDifficulty.getDifficulty(), pool.getHost());

		MiningSetDifficultyNotification notification = new MiningSetDifficultyNotification();
		notification.setDifficulty(setDifficulty.getDifficulty());

		List<WorkerConnection> connections = poolWorkerConnections.get(pool);

		if (connections == null) {
			LOGGER.debug("No worker connections on pool {}. Do not send setDifficulty.", pool.getHost());
		} else {
			synchronized (connections) {
				for (WorkerConnection connection : connections) {
					connection.sendNotification(notification);
				}
			}
		}
	}

	/**
	 * Called when a pool set the extranonce
	 * 
	 * @param pool
	 * @param setExtranonce
	 */
	public void onPoolSetExtranonce(Pool pool, MiningSetExtranonceNotification setExtranonce) {
		LOGGER.info("Set the extranonce on pool {}.", pool.getHost());

		List<WorkerConnection> connections = poolWorkerConnections.get(pool);

		if (connections == null) {
			LOGGER.debug("No worker connections on pool {}. Do not send setExtranonce.", pool.getHost());
		} else {
			synchronized (connections) {
				for (WorkerConnection connection : connections) {
					connection.onPoolExtranonceChange();
				}
			}
		}
	}

	/**
	 * Called when a pool send a notify request.
	 * 
	 * @param pool
	 * @param setDifficulty
	 */
	public void onPoolNotify(Pool pool, MiningNotifyNotification notify) {
		if (notify.getCleanJobs()) {
			LOGGER.info("New block detected on pool {}.", pool.getHost());
		}

		MiningNotifyNotification notification = new MiningNotifyNotification();
		notification.setBitcoinVersion(notify.getBitcoinVersion());
		notification.setCleanJobs(notify.getCleanJobs());
		notification.setCoinbase1(notify.getCoinbase1());
		notification.setCoinbase2(notify.getCoinbase2());
		notification.setCurrentNTime(notify.getCurrentNTime());
		notification.setJobId(notify.getJobId());
		notification.setMerkleBranches(notify.getMerkleBranches());
		notification.setNetworkDifficultyBits(notify.getNetworkDifficultyBits());
		notification.setPreviousHash(notify.getPreviousHash());

		List<WorkerConnection> connections = poolWorkerConnections.get(pool);

		if (connections == null) {
			LOGGER.debug("No worker connections on pool {}. Do not send notify.", pool.getHost());
		} else {
			synchronized (connections) {
				for (WorkerConnection connection : connections) {
					connection.sendNotification(notification);
				}
			}
		}
	}

	/**
	 * Return the pool to which the worker connection is linked.
	 * 
	 * @param connection
	 * @return
	 */
	protected Pool getPoolForNewConnection() throws NoPoolAvailableException {
		Pool result = null;
		synchronized (pools) {
			for (Pool pool : pools) {
				if (pool.isActive() && pool.isEnabled()) {
					result = pool;
					break;
				}
			}
		}

		if (result == null) {
			throw new NoPoolAvailableException("No pool available. " + pools);
		}

		return result;
	}

	/**
	 * Called when a worker is disconnected.
	 * 
	 * @param workerConnection
	 * @param cause
	 */
	public void onWorkerDisconnection(WorkerConnection workerConnection, Throwable cause) {
		List<WorkerConnection> connections = poolWorkerConnections.get(workerConnection.getPool());
		if (connections != null) {
			connections.remove(workerConnection);
		}
		LOGGER.info("Worker connection {} closed. {} connections active. Cause: {}", workerConnection.getConnectionName(), connections == null ? 0
				: connections.size(), cause.getMessage());
	}

	/**
	 * Called by pool when its state changes
	 */
	public void onPoolStateChange(final Pool pool) {
		if (pool.isActive()) {
			LOGGER.warn("Pool {} is UP again.", pool.getHost());
			// TODO maybe move worker connections to this pool
		} else {
			LOGGER.warn("Pool {} is DOWN. Moving connections to another one.", pool.getHost());
			Thread moveWorkersThread = new Thread() {
				public void run() {
					List<WorkerConnection> connections = poolWorkerConnections.get(pool);
					if (connections != null) {
						synchronized (connections) {
							for (WorkerConnection connection : connections) {
								switchPoolForConnection(connection);
							}
						}
					}
				}
			};
			moveWorkersThread.start();
		}
	}

	/**
	 * Switch the given connection to another pool. The pool is selected through
	 * the selectPool(connection) method.
	 * 
	 * @param connection
	 */
	private void switchPoolForConnection(WorkerConnection connection) {
		Pool newPool = null;
		try {
			// Select the new pool for the connection
			newPool = selectPool(connection);
			// Then rebind the connection to this pool
			connection.rebindToPool(newPool);
			// And finally add the worker connection to the pool's worker
			// connections
			List<WorkerConnection> newPoolConnections = poolWorkerConnections.get(newPool);
			newPoolConnections.add(connection);
		} catch (NoPoolAvailableException e) {
			// If no more pool available, close the connection.
			LOGGER.warn("Closing connection {} since no more pool is active.", connection.getConnectionName());
			connection.close();
		} catch (TooManyWorkersException e) {
			// If no more free space on the pool close the connection.
			LOGGER.warn("Closing connection {} since no more space on the pool {}.", connection.getConnectionName(), newPool.getHost());
			connection.close();
		}
	}

	/**
	 * Called when a worker extranonce change has failed. If so, close the
	 * connection and remove it.
	 */
	public void onWorkerChangeExtranonceFailure(WorkerConnection connection) {
		onWorkerDisconnection(connection, new Exception("The workerConnection " + connection.getConnectionName()
				+ " does not support setExtranonce notification."));
	}

	/**
	 * Return the pool to which the given connection should be bound.
	 * 
	 * @param connection
	 * @return
	 * @throws NoPoolAvailableException
	 */
	private Pool selectPool(WorkerConnection connection) throws NoPoolAvailableException {
		// TODO to improve. At the moment, just return the first active.
		return getPoolForNewConnection();
	}

}
