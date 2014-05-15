/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
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
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ResponseReceivedCallback;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.JsonRpcError;
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
		Pool pool = getHighestPriorityActivePool();

		List<WorkerConnection> workerConnections = poolWorkerConnections.get(pool);
		if (workerConnections == null) {
			workerConnections = Collections.synchronizedList(new ArrayList<WorkerConnection>());
			poolWorkerConnections.put(pool, workerConnections);
		}
		workerConnections.add(connection);
		LOGGER.info("New WorkerConnection {} subscribed. {} connections active on pool {}.", connection.getConnectionName(),
				workerConnections.size(), pool.getName());

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
		if (workerConnection.getPool() != null && workerConnection.getPool().isActive()) {
			for (int i = 0; i < workerConnection.getPool().getNumberOfSubmit(); i++) {
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
		} else {
			LOGGER.warn("Share submit from {}@{} dropped since pool {} is inactive.", workerRequest.getWorkerName(),
					workerConnection.getConnectionName(), workerConnection.getPool());

			// Notify the worker that the target pool is no more active
			MiningSubmitResponse fakePoolResponse = new MiningSubmitResponse();
			fakePoolResponse.setId(workerRequest.getId());
			fakePoolResponse.setIsAccepted(false);
			JsonRpcError error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNKNOWN.getCode());
			error.setMessage("The traget pool is no more active.");
			fakePoolResponse.setErrorRpc(error);
			workerConnection.onPoolSubmitResponse(workerRequest, fakePoolResponse);
		}
	}

	/**
	 * Called when a pool set the difficulty.
	 * 
	 * @param pool
	 * @param setDifficulty
	 */
	public void onPoolSetDifficulty(Pool pool, MiningSetDifficultyNotification setDifficulty) {
		LOGGER.info("Set difficulty {} on pool {}.", setDifficulty.getDifficulty(), pool.getName());

		MiningSetDifficultyNotification notification = new MiningSetDifficultyNotification();
		notification.setDifficulty(setDifficulty.getDifficulty());

		List<WorkerConnection> connections = poolWorkerConnections.get(pool);

		if (connections == null) {
			LOGGER.debug("No worker connections on pool {}. Do not send setDifficulty.", pool.getName());
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
		LOGGER.info("Set the extranonce on pool {}.", pool.getName());

		List<WorkerConnection> connections = poolWorkerConnections.get(pool);

		if (connections == null) {
			LOGGER.debug("No worker connections on pool {}. Do not send setExtranonce.", pool.getName());
		} else {
			// Use an external list to store connection that should be
			// closed to avoid concurrent modification exception during
			// iteration over the connection list (since if we close a
			// connection, the same thread remove the connection during
			// the iteration).
			List<WorkerConnection> connectionsToDisconnect = new ArrayList<WorkerConnection>();
			synchronized (connections) {
				for (WorkerConnection connection : connections) {
					try {
						connection.onPoolExtranonceChange();
					} catch (ChangeExtranonceNotSupportedException e) {
						connectionsToDisconnect.add(connection);
					}
				}
			}

			// Once all connections are notified, close and remove all
			// connections that does not support the extranonce change on the
			// fly
			for (WorkerConnection workerConnection : connectionsToDisconnect) {
				workerConnection.close();
				onWorkerDisconnection(workerConnection, new Exception("The workerConnection " + workerConnection.getConnectionName()
						+ " does not support setExtranonce notification."));
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
			LOGGER.info("New block detected on pool {}.", pool.getName());
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
			LOGGER.debug("No worker connections on pool {}. Do not send notify.", pool.getName());
		} else {
			synchronized (connections) {
				for (WorkerConnection connection : connections) {
					connection.sendNotification(notification);
				}
			}
		}
	}

	/**
	 * Return the active pool with the highest priority (0).
	 * 
	 * @param connection
	 * @return
	 */
	protected Pool getHighestPriorityActivePool() throws NoPoolAvailableException {
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
	public void onWorkerDisconnection(final WorkerConnection workerConnection, final Throwable cause) {
		// Launch a thread to remove the connection. Done to avoid a concurrent
		// modification exception which could happen if the disconnection
		// happens during a connection list iteration.
		Thread removeThread = new Thread() {
			public void run() {
				List<WorkerConnection> connections = poolWorkerConnections.get(workerConnection.getPool());
				if (connections != null) {
					connections.remove(workerConnection);
				}
				LOGGER.info("Worker connection {} closed. {} connections active on pool {}. Cause: {}", workerConnection.getConnectionName(),
						connections == null ? 0 : connections.size(), workerConnection.getPool() != null ? workerConnection.getPool().getName()
								: "None", cause != null ? cause.getMessage() : "Unknown");
			}
		};
		removeThread.start();
	}

	/**
	 * Called by pool when its state changes
	 */
	public void onPoolStateChange(Pool pool) {
		if (pool.isActive()) {
			LOGGER.warn("Pool {} is UP.", pool.getName());
			rebindAllWorkerConnections();
		} else {
			LOGGER.warn("Pool {} is DOWN. Moving connections to another one.", pool.getName());
			switchPoolConnections(pool);
		}
	}

	/**
	 * Rebind all the workers connection to the pool with highest priority.
	 */
	public void rebindAllWorkerConnections() {
		LOGGER.info("Rebind all worker connections.");
		synchronized (pools) {
			// For each pools, rebind the connections
			for (Pool pool : pools) {
				switchPoolConnections(pool);
			}
		}
	}

	/**
	 * Switch all the connections of the given pool to another pool.
	 * 
	 * @param pool
	 */
	private void switchPoolConnections(final Pool pool) {
		Thread moveWorkersThread = new Thread() {
			public void run() {
				LOGGER.info("Switching all connections of pool {}.", pool.getName());

				List<WorkerConnection> connections = poolWorkerConnections.get(pool);
				if (connections != null) {
					// Use an external map to store connection that should be
					// closed to avoid concurrent modification exception during
					// iteration over the connection list (since if we close a
					// connection, the same thread remove the connection during
					// the iteration).
					Map<WorkerConnection, Throwable> connectionToClose = new HashMap<WorkerConnection, Throwable>();
					List<WorkerConnection> tempConnectionsList = null;
					synchronized (connections) {
						tempConnectionsList = new ArrayList<>(connections);
					}

					for (WorkerConnection connection : tempConnectionsList) {
						try {
							switchPoolForConnection(connection);
						} catch (Exception e) {
							// If an exception occurs, close the
							// connection
							connectionToClose.put(connection, e);
						}
					}

					for (Entry<WorkerConnection, Throwable> entry : connectionToClose.entrySet()) {
						entry.getKey().close();
						onWorkerDisconnection(entry.getKey(), entry.getValue());
					}
				}
			}
		};
		moveWorkersThread.start();
	}

	/**
	 * Switch the given connection to another pool. The pool is selected through
	 * the selectPool(connection) method.
	 * 
	 * @param connection
	 */
	private void switchPoolForConnection(WorkerConnection connection) throws NoPoolAvailableException, TooManyWorkersException,
			ChangeExtranonceNotSupportedException {
		// Select the new pool for the connection
		Pool newPool = selectPool(connection);

		// If the old pool is the same as the new pool, do nothing.
		if (!newPool.equals(connection.getPool())) {
			// Remove the connection from the old pool connection list.
			List<WorkerConnection> oldPoolConnections = poolWorkerConnections.get(connection.getPool());
			if (oldPoolConnections != null) {
				oldPoolConnections.remove(connection);
			}

			// Then rebind the connection to this pool
			connection.rebindToPool(newPool);
			// And finally add the worker connection to the pool's worker
			// connections
			List<WorkerConnection> newPoolConnections = poolWorkerConnections.get(newPool);
			newPoolConnections.add(connection);
		}
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
		return getHighestPriorityActivePool();
	}

	/**
	 * Set the priority of the pool with the given name and rebind worker
	 * connections based on this new priority.
	 * 
	 * @param poolName
	 * @param newPriority
	 * @throws BadParameterException
	 */
	public void setPoolPriority(String poolName, int newPriority) throws NoPoolAvailableException, BadParameterException {
		if (getPool(poolName) == null) {
			throw new NoPoolAvailableException("Pool with name " + poolName + " not found");
		}

		if (newPriority < 0) {
			throw new BadParameterException("The priority has to be higher or equal to 0");
		}

		synchronized (pools) {
			if (pools != null) {
				for (Pool pool : pools) {
					// Set the new priority to the pool with the given name.
					if (pool.getName().equals(poolName)) {
						LOGGER.info("Changing pool {} priority from {} to {}.", pool.getName(), pool.getPriority(), newPriority);
						pool.setPriority(newPriority);
					} else if (pool.getPriority() >= newPriority) {
						// And move the priority of pools with lower or
						// equals priority
						pool.setPriority(pool.getPriority() + 1);
					}
				}
			}
			Collections.sort(pools);
		}
		rebindAllWorkerConnections();
	}

	/**
	 * Disable/Enable the pool with the given name
	 * 
	 * @param poolName
	 * @param isEnabled
	 * @throws NoPoolAvailableException
	 */
	public void setPoolEnabled(String poolName, boolean isEnabled) throws NoPoolAvailableException, Exception {
		Pool pool = getPool(poolName);
		if (pool == null) {
			throw new NoPoolAvailableException("Pool with name " + poolName + " is not found");
		}

		if (pool.isEnabled() != isEnabled) {
			LOGGER.info("Set pool {} {}", pool.getName(), isEnabled ? "enabled" : "disabled");
			pool.setEnabled(isEnabled);
		}
	}

	/**
	 * Return the pool based on the pool name.
	 * 
	 * @param poolHost
	 * @return
	 */
	public Pool getPool(String poolName) {
		Pool result = null;
		synchronized (pools) {
			for (Pool pool : pools) {
				if (pool.getName().toString().equals(poolName)) {
					result = pool;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Return all pools managed by this manager.
	 * 
	 * @return
	 */
	public List<Pool> getPools() {
		List<Pool> result = new ArrayList<>();
		synchronized (pools) {
			result.addAll(pools);
		}
		return result;
	}

	/**
	 * Return the number of worker connections on the pool with the given name.
	 * 
	 * @param poolName
	 * @return
	 */
	public int getNumberOfWorkerConnectionsOnPool(String poolName) {
		Pool pool = getPool(poolName);
		List<WorkerConnection> connections = poolWorkerConnections.get(pool);
		return connections == null ? 0 : connections.size();
	}

}
