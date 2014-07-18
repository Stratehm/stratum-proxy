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
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ResponseReceivedCallback;
import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.NotConnectedException;
import strat.mining.stratum.proxy.exception.NotFoundException;
import strat.mining.stratum.proxy.exception.PoolStartException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.exception.UnsupportedPoolSwitchingStrategyException;
import strat.mining.stratum.proxy.json.JsonRpcError;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSetExtranonceNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.manager.strategy.PoolSwitchingStrategyFactory;
import strat.mining.stratum.proxy.manager.strategy.PoolSwitchingStrategyManager;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.model.User;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.dto.AddPoolDTO;
import strat.mining.stratum.proxy.rest.dto.AddressDTO;
import strat.mining.stratum.proxy.rest.dto.ConnectionIdentifierDTO;
import strat.mining.stratum.proxy.rest.dto.UserNameDTO;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * Manage connections (Pool and Worker) and build some stats.
 * 
 * @author Strat
 * 
 */
public class ProxyManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyManager.class);

	private static ProxyManager instance;

	private ServerSocket serverSocket;
	private Thread listeningThread;

	private List<Pool> pools;

	private List<WorkerConnection> workerConnections;

	private Map<String, User> users;

	private Map<Pool, List<WorkerConnection>> poolWorkerConnections;

	private boolean closeRequested = false;

	private AuthorizationManager stratumAuthorizationManager;

	private volatile PoolSwitchingStrategyManager poolSwitchingStrategyManager;

	private PoolSwitchingStrategyFactory poolSwitchingStrategyFactory;

	private ProxyManager() {
		this.stratumAuthorizationManager = new AuthorizationManager();
		this.pools = Collections.synchronizedList(new ArrayList<Pool>());
		this.workerConnections = new CopyOnWriteArrayList<WorkerConnection>();
		this.users = Collections.synchronizedMap(new HashMap<String, User>());
		this.poolWorkerConnections = Collections.synchronizedMap(new HashMap<Pool, List<WorkerConnection>>());
		this.poolSwitchingStrategyFactory = new PoolSwitchingStrategyFactory(this);

		setPoolSwitchingStrategy(ConfigurationManager.getInstance().getPoolSwitchingStrategy());
	}

	public static ProxyManager getInstance() {
		if (instance == null) {
			instance = new ProxyManager();
		}
		return instance;
	}

	/**
	 * Start all pools.
	 */
	public void startPools(List<Pool> pools) {
		this.pools = Collections.synchronizedList(new ArrayList<Pool>(pools));
		synchronized (pools) {
			for (Pool pool : pools) {
				try {
					if (pool.isEnabled()) {
						pool.startPool(this);
					} else {
						LOGGER.warn("Do not start pool {} since it is disabled.", pool.getName());
					}
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
						StratumWorkerConnection workerConnection = new StratumWorkerConnection(incomingConnectionSocket, ProxyManager.this);
						workerConnection.setSamplingHashesPeriod(ConfigurationManager.getInstance().getConnectionHashrateSamplingPeriod());
						workerConnection.startReading();
					} catch (Exception e) {
						// Do not log the error if a close has been requested
						// (as the error is expected ans is part of the shutdown
						// process)
						if (!closeRequested) {
							LOGGER.error("Error on the server socket {}.", serverSocket.getLocalSocketAddress(), e);
						}
					}
				}

				LOGGER.info("Stop to listen incoming connection on {}.", serverSocket.getLocalSocketAddress());
			}
		};
		listeningThread.setName("StratumProxyManagerSeverSocketListener");
		listeningThread.setDaemon(true);
		listeningThread.start();
	}

	/**
	 * Stop to listen incoming connections
	 */
	public void stopListeningIncomingConnections() {
		if (serverSocket != null) {
			LOGGER.info("Closing the server socket on {}.", serverSocket.getLocalSocketAddress());
			try {
				closeRequested = true;
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
		for (WorkerConnection connection : workerConnections) {
			connection.close();
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
		Pool pool = poolSwitchingStrategyManager.getPoolForConnection(connection);

		List<WorkerConnection> workerConnections = poolWorkerConnections.get(pool);
		if (workerConnections == null) {
			workerConnections = new CopyOnWriteArrayList<WorkerConnection>();
			poolWorkerConnections.put(pool, workerConnections);
		}
		workerConnections.add(connection);
		this.workerConnections.add(connection);
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
	public void onAuthorizeRequest(WorkerConnection connection, MiningAuthorizeRequest request) throws AuthorizationException {
		// Check that the worker is authorized on this proxy
		stratumAuthorizationManager.checkAuthorization(connection, request);

		// Authorize the worker on the pool. Block until the authorization is
		// done.
		connection.getPool().authorizeWorker(request);

		linkConnectionToUser(connection, request);
	}

	/**
	 * Link the connection to the user
	 * 
	 * @param connection
	 * @param request
	 */
	private void linkConnectionToUser(WorkerConnection connection, MiningAuthorizeRequest request) {
		User user = users.get(request.getUsername());
		if (user == null) {
			user = new User(request.getUsername());
			user.setSamplingHashesPeriod(ConfigurationManager.getInstance().getUserHashrateSamplingPeriod());
			users.put(request.getUsername(), user);
		}
		user.addConnection(connection);
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
				workerConnection.getPool().submitShare(workerRequest, new ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>() {
					public void onResponseReceived(MiningSubmitRequest request, MiningSubmitResponse response) {
						updateShareLists(workerRequest, response, workerConnection);
						workerConnection.onPoolSubmitResponse(workerRequest, response);
					}
				});

			}
		} else {
			LOGGER.warn("REJECTED share. Share submit from {}@{} dropped since pool {} is inactive.", workerRequest.getWorkerName(),
					workerConnection.getConnectionName(), workerConnection.getPool());

			// Notify the worker that the target pool is no more active
			MiningSubmitResponse fakePoolResponse = new MiningSubmitResponse();
			fakePoolResponse.setId(workerRequest.getId());
			fakePoolResponse.setIsAccepted(false);
			JsonRpcError error = new JsonRpcError();
			error.setCode(JsonRpcError.ErrorCode.UNKNOWN.getCode());
			error.setMessage("The target pool is no more active.");
			fakePoolResponse.setErrorRpc(error);
			workerConnection.onPoolSubmitResponse(workerRequest, fakePoolResponse);
		}
	}

	/**
	 * Update the share lists of all pools, users and worker connections.
	 * 
	 * @param request
	 * @param response
	 * @param workerConnection
	 */
	private void updateShareLists(MiningSubmitRequest request, MiningSubmitResponse response, WorkerConnection workerConnection) {
		if (workerConnection.getPool() != null) {
			Share share = new Share();
			share.setDifficulty(workerConnection.getPool().getDifficulty());
			share.setTime(System.currentTimeMillis());

			boolean isAccepted = response.getIsAccepted() != null && response.getIsAccepted();

			workerConnection.updateShareLists(share, isAccepted);

			workerConnection.getPool().updateShareLists(share, isAccepted);

			User user = users.get(request.getWorkerName());
			if (user != null) {
				user.updateShareLists(share, isAccepted);
			}
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
			for (WorkerConnection connection : connections) {
				connection.onPoolDifficultyChanged(notification);
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
			for (WorkerConnection connection : connections) {
				try {
					connection.onPoolExtranonceChange();
				} catch (ChangeExtranonceNotSupportedException e) {
					connection.close();
					onWorkerDisconnection(connection, new Exception("The workerConnection " + connection.getConnectionName()
							+ " does not support setExtranonce notification."));
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
			for (WorkerConnection connection : connections) {
				connection.onPoolNotify(notification);
			}
		}
	}

	/**
	 * Called when a worker is disconnected.
	 * 
	 * @param workerConnection
	 * @param cause
	 */
	public void onWorkerDisconnection(final WorkerConnection workerConnection, final Throwable cause) {
		List<WorkerConnection> connections = poolWorkerConnections.get(workerConnection.getPool());
		if (connections != null) {
			connections.remove(workerConnection);
		}
		ProxyManager.this.workerConnections.remove(workerConnection);
		LOGGER.info("Worker connection {} closed. {} connections active on pool {}. Cause: {}", workerConnection.getConnectionName(),
				connections == null ? 0 : connections.size(), workerConnection.getPool() != null ? workerConnection.getPool().getName() : "None",
				cause != null ? cause.getMessage() : "Unknown");
	}

	/**
	 * Called by pool when its state changes
	 */
	public void onPoolStateChange(Pool pool) {
		if (pool.isActive()) {
			LOGGER.warn("Pool {} is UP.", pool.getName());
			poolSwitchingStrategyManager.onPoolUp(pool);
		} else {
			LOGGER.warn("Pool {} is DOWN. Moving connections to another one.", pool.getName());
			poolSwitchingStrategyManager.onPoolDown(pool);
		}
	}

	/**
	 * Called when a pool is now stable.
	 * 
	 * @param pool
	 */
	public void onPoolStable(Pool pool) {
		LOGGER.warn("Pool {} is STABLE.", pool.getName());
		poolSwitchingStrategyManager.onPoolStable(pool);
	}

	/**
	 * Switch the given connection to the given pool.
	 * 
	 * @param connection
	 * @param newPool
	 */
	public void switchPoolForConnection(WorkerConnection connection, Pool newPool) throws TooManyWorkersException,
			ChangeExtranonceNotSupportedException {
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

		Pool pool = getPool(poolName);
		LOGGER.info("Changing pool {} priority from {} to {}.", pool.getName(), pool.getPriority(), newPriority);
		pool.setPriority(newPriority);

		poolSwitchingStrategyManager.onPoolUpdated(pool);
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
			pool.setEnabled(isEnabled, this);
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

	/**
	 * Return a list of all worker connections.
	 * 
	 * @return
	 */
	public List<WorkerConnection> getWorkerConnections() {
		return Collections.unmodifiableList(workerConnections);
	}

	/**
	 * Return all authorized users.
	 * 
	 * @return
	 */
	public List<User> getUsers() {
		List<User> result = new ArrayList<>(users.size());
		synchronized (users) {
			result.addAll(users.values());
		}
		return result;
	}

	/**
	 * Add the pool described in the given poolDTO
	 * 
	 * @param addPoolDTO
	 * @return
	 * @throws URISyntaxException
	 * @throws PoolStartException
	 * @throws SocketException
	 */
	public Pool addPool(AddPoolDTO addPoolDTO) throws BadParameterException, SocketException, PoolStartException, URISyntaxException {

		LOGGER.debug("Trying to add pool {}.", addPoolDTO);

		checkAddPoolParameters(addPoolDTO);

		Pool poolToAdd = new Pool(addPoolDTO.getPoolName(), addPoolDTO.getPoolHost(), addPoolDTO.getUsername(), addPoolDTO.getPassword());

		// By default, does not enable extranonce subscribe.
		poolToAdd.setExtranonceSubscribeEnabled(addPoolDTO.getEnableExtranonceSubscribe() != null && addPoolDTO.getEnableExtranonceSubscribe());

		poolToAdd.setAppendWorkerNames(addPoolDTO.getAppendWorkerNames() != null ? addPoolDTO.getAppendWorkerNames() : false);
		poolToAdd.setWorkerSeparator(addPoolDTO.getWorkerNameSeparator() != null ? addPoolDTO.getWorkerNameSeparator()
				: Constants.DEFAULT_WORKER_NAME_SEPARTOR);
		poolToAdd.setUseWorkerPassword(addPoolDTO.getUseWorkerPassword() != null ? addPoolDTO.getUseWorkerPassword() : false);

		if (addPoolDTO.getPriority() != null) {
			poolToAdd.setPriority(addPoolDTO.getPriority());
		}

		// Add the pool to the pool list
		pools.add(poolToAdd);

		LOGGER.info("Pool added {}.", addPoolDTO);

		try {
			poolToAdd.setEnabled(addPoolDTO.getIsEnabled() == null || addPoolDTO.getIsEnabled());
		} catch (Exception e) {
			throw new PoolStartException("Failed to enable the created pool with name " + poolToAdd.getName()
					+ ". This should not happen. Surely a BUUUUGGGG !!!!", e);
		}

		if (poolToAdd.isEnabled()) {
			poolToAdd.startPool(this);
		}

		poolSwitchingStrategyManager.onPoolAdded(poolToAdd);

		return poolToAdd;
	}

	/**
	 * Remove the pool with the given name.
	 * 
	 * @param poolName
	 * @throws NoPoolAvailableException
	 */
	public void removePool(String poolName) throws NoPoolAvailableException {
		Pool pool = getPool(poolName);
		if (pool == null) {
			throw new NoPoolAvailableException("Pool with name " + poolName + " is not found");
		}

		pool.stopPool();
		pools.remove(pool);

		poolSwitchingStrategyManager.onPoolRemoved(pool);

		poolWorkerConnections.remove(pool);

		LOGGER.info("Pool {} removed.", poolName);
	}

	/**
	 * Check that all parameters to add the pool are presents and valid.
	 * 
	 * @param addPoolDTO
	 * @throws URISyntaxException
	 */
	private void checkAddPoolParameters(AddPoolDTO addPoolDTO) throws BadParameterException, URISyntaxException {

		if (addPoolDTO.getPoolHost() == null || addPoolDTO.getPoolHost().trim().isEmpty()) {
			throw new BadParameterException("Pool host is empty.");
		}

		new URI("stratum+tcp://" + addPoolDTO.getPoolHost().trim());

		if (addPoolDTO.getUsername() == null || addPoolDTO.getUsername().trim().isEmpty()) {
			throw new BadParameterException("Username is empty.");
		}

		if (addPoolDTO.getPassword() == null || addPoolDTO.getPassword().trim().isEmpty()) {
			throw new BadParameterException("Password is empty.");
		}
	}

	/**
	 * Kick the given user. Kill all the connections where the user has been
	 * seen.
	 * 
	 * @param username
	 * @throws NotConnectedException
	 * @throws NotFoundException
	 */
	public void kickUser(UserNameDTO username) throws BadParameterException, NotConnectedException, NotFoundException {
		if (username.getUsername() != null && !username.getUsername().trim().isEmpty()) {
			User user = users.get(username.getUsername());
			if (user != null) {
				List<WorkerConnection> connections = user.getWorkerConnections();
				if (connections != null && !connections.isEmpty()) {
					for (WorkerConnection connection : connections) {
						connection.close();
						onWorkerDisconnection(connection, new Exception("Connection closed since user " + username.getUsername()
								+ " has been kicked."));
					}
				} else {
					throw new NotConnectedException("The user " + user.getName() + " has no connections.");
				}
			} else {
				throw new NotFoundException("User " + username.getUsername() + " not found.");
			}
		} else {
			throw new BadParameterException("Invalid username: " + username.getUsername());
		}
	}

	/**
	 * Ban the given user until the next proxy restart
	 * 
	 * @param username
	 * @throws NotFoundException
	 * @throws NotConnectedException
	 */
	public void banUser(UserNameDTO username) throws BadParameterException {
		try {
			kickUser(username);
		} catch (NotConnectedException | NotFoundException e) {
			// Nothing to do. We don't mind
		}
		stratumAuthorizationManager.banUser(username);
	}

	/**
	 * Unban the given user.
	 * 
	 * @param username
	 * @throws NotFoundException
	 */
	public void unbanUser(UserNameDTO username) throws NotFoundException {
		stratumAuthorizationManager.unbanUser(username);
	}

	/**
	 * Return the list of banned users.
	 * 
	 * @return
	 */
	public List<String> getBannedUsers() {
		return stratumAuthorizationManager.getBannedUsers();
	}

	/**
	 * Kick the connection on the given address and port
	 * 
	 * @param connection
	 * @throws BadParameterException
	 * @throws NotFoundException
	 */
	public void kickConnection(ConnectionIdentifierDTO connection) throws BadParameterException, NotFoundException {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(connection.getAddress());
		} catch (Exception e) {
			throw new BadParameterException("Invalid address: " + connection.getAddress() + ". " + e.getMessage(), e);
		}

		if (connection.getPort() == null || connection.getPort() < 1 || connection.getPort() > 65535) {
			throw new BadParameterException("Invalid port number: " + connection.getPort());
		}

		WorkerConnection connectionToKick = null;

		for (WorkerConnection workerConnection : workerConnections) {
			if (address.equals(workerConnection.getRemoteAddress()) && connection.getPort().equals(workerConnection.getRemotePort())) {
				connectionToKick = workerConnection;
				break;
			}
		}

		if (connectionToKick == null) {
			throw new NotFoundException("No connection found with address " + connection.getAddress() + " and port number " + connection.getPort());
		}

		connectionToKick.close();
		onWorkerDisconnection(connectionToKick, new Exception("Connection kicked"));

	}

	/**
	 * Kick all the connections with the given address
	 * 
	 * @param address
	 * @throws NotConnectedException
	 * @throws NotFoundException
	 */
	public void kickAddress(AddressDTO address) throws BadParameterException, NotFoundException {
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getByName(address.getAddress());
		} catch (Exception e) {
			throw new BadParameterException("Invalid address: " + inetAddress.getAddress() + ". " + e.getMessage(), e);
		}

		List<WorkerConnection> connectionsToKick = new ArrayList<WorkerConnection>();

		for (WorkerConnection workerConnection : workerConnections) {
			if (inetAddress.equals(workerConnection.getRemoteAddress())) {
				connectionsToKick.add(workerConnection);
			}
		}

		if (connectionsToKick.size() < 1) {
			throw new NotFoundException("No connection found with address " + inetAddress.getAddress());
		}

		for (WorkerConnection connectionToKick : connectionsToKick) {
			connectionToKick.close();
			onWorkerDisconnection(connectionToKick, new Exception("Connection kicked"));
		}
	}

	/**
	 * Ban the given address until the next proxy restart
	 * 
	 * @param address
	 * @throws NotFoundException
	 * @throws NotConnectedException
	 */
	public void banAddress(AddressDTO address) throws BadParameterException {
		try {
			kickAddress(address);
		} catch (NotFoundException e) {
			// Nothing to do. We don't mind
		}
		try {
			stratumAuthorizationManager.banAddress(address);
		} catch (UnknownHostException e) {
			throw new BadParameterException("Failed to ban address " + address.getAddress() + ". " + e.getMessage());
		}
	}

	/**
	 * Unban the given address.
	 * 
	 * @param username
	 * @throws NotFoundException
	 * @throws BadParameterException
	 */
	public void unbanAddress(AddressDTO address) throws NotFoundException, BadParameterException {
		try {
			stratumAuthorizationManager.unbanAddress(address);
		} catch (UnknownHostException e) {
			throw new BadParameterException("Failed to unban address " + address.getAddress() + ". " + e.getMessage());
		}
	}

	/**
	 * Return the list of banned addresses.
	 * 
	 * @return
	 */
	public List<String> getBannedAddresses() {
		return stratumAuthorizationManager.getBannedAddresses();
	}

	/**
	 * Change the pool switching strategy used.
	 * 
	 * @param strategyName
	 * @throws NotFoundException
	 */
	public void setPoolSwitchingStrategy(String strategyName) throws UnsupportedPoolSwitchingStrategyException {
		if (poolSwitchingStrategyManager == null || !poolSwitchingStrategyManager.getName().equalsIgnoreCase(strategyName)) {
			if (poolSwitchingStrategyManager != null) {
				poolSwitchingStrategyManager.stop();
			}
			poolSwitchingStrategyManager = poolSwitchingStrategyFactory.getPoolSwitchingStrategyManagerByName(strategyName);
		}
	}

}
