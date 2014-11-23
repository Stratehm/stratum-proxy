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
package strat.mining.stratum.proxy.pool;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ResponseReceivedCallback;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.PoolStartException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.ClientReconnectNotification;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningAuthorizeResponse;
import strat.mining.stratum.proxy.json.MiningExtranonceSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningExtranonceSubscribeResponse;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSetExtranonceNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.utils.Timer.Task;
import strat.mining.stratum.proxy.utils.mining.HashrateUtils;

import com.google.common.util.concurrent.AtomicDouble;

public class Pool {

	private static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);

	private ProxyManager manager;

	private String name;
	private String host;
	private URI uri;
	private String username;
	private String password;

	private Double difficulty;
	private String extranonce1;
	private Integer extranonce2Size;

	private Date readySince;
	private boolean isReady;
	private boolean isEnabled;
	private boolean isStable;
	private boolean isFirstRun;
	private boolean isAppendWorkerNames;
	private boolean isUseWorkerPassword;
	private boolean isActive;
	private Date activeSince;

	private String workerSeparator;

	// Contains all available tails in Hexa format.
	private Deque<String> tails;

	private PoolConnection connection;

	private MiningNotifyNotification currentJob;

	private Task reconnectTask;
	private Task notifyTimeoutTask;
	private Task stabilityTestTask;
	private Task subscribeResponseTimeoutTask;

	private Boolean isExtranonceSubscribeEnabled = false;

	private Integer numberOfSubmit = 1;

	private Integer priority;
	private Integer weight;

	private AtomicDouble acceptedDifficulty;
	private AtomicDouble rejectedDifficulty;

	private Deque<Share> lastAcceptedShares;
	private Deque<Share> lastRejectedShares;
	// Time of sampling shares to calculate hash rate
	private Integer samplingHashratePeriod = Constants.DEFAULT_POOL_HASHRATE_SAMPLING_PERIOD * 1000;

	private Integer connectionRetryDelay = Constants.DEFAULT_POOL_CONNECTION_RETRY_DELAY;
	private Integer reconnectStabilityPeriod = Constants.DEFAULT_POOL_RECONNECTION_STABILITY_PERIOD;
	private Integer noNotifyTimeout = Constants.DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT;

	private Boolean isRejectReconnect = false;

	// Store the callbacks to call when the pool responds to a submit request.
	private Map<Long, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>> submitCallbacks;

	// Store the callbacks to call when the pool responds to worker authorize
	// request.
	private Map<Long, ResponseReceivedCallback<MiningAuthorizeRequest, MiningAuthorizeResponse>> authorizeCallbacks;

	private Set<String> authorizedWorkers;
	private Map<String, CountDownLatch> pendingAuthorizeRequests;

	private String lastStopCause;
	private Date lastStopDate;

	public Pool(String name, String host, String username, String password) {
		super();
		this.name = name == null || name.isEmpty() ? host : name;
		this.host = host;
		this.username = username;
		this.password = password;
		this.isReady = false;
		this.isEnabled = true;
		this.isStable = false;
		this.isFirstRun = true;

		acceptedDifficulty = new AtomicDouble(0);
		rejectedDifficulty = new AtomicDouble(0);

		this.tails = buildTails();
		this.submitCallbacks = Collections.synchronizedMap(new HashMap<Long, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>>());
		this.authorizeCallbacks = Collections
				.synchronizedMap(new HashMap<Long, ResponseReceivedCallback<MiningAuthorizeRequest, MiningAuthorizeResponse>>());
		this.lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		this.lastRejectedShares = new ConcurrentLinkedDeque<Share>();
		this.authorizedWorkers = Collections.synchronizedSet(new HashSet<String>());
		this.pendingAuthorizeRequests = Collections.synchronizedMap(new HashMap<String, CountDownLatch>());
	}

	public synchronized void startPool(ProxyManager manager) throws PoolStartException, URISyntaxException, SocketException {
		if (manager != null) {
			if (!isEnabled) {
				throw new PoolStartException("Do not start the pool " + getName() + " since it is disabled.");
			}

			this.manager = manager;
			if (connection == null) {
				LOGGER.debug("Starting pool {}...", getName());
				uri = new URI("stratum+tcp://" + host);
				if (uri.getPort() < 0) {
					UriBuilder.fromUri(uri).port(Constants.DEFAULT_POOL_PORT);
				}
				Socket socket = new Socket();
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);

				try {
					socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort() > -1 ? uri.getPort() : Constants.DEFAULT_POOL_PORT));
					connection = new PoolConnection(this, socket);
					connection.startReading();

					MiningSubscribeRequest request = new MiningSubscribeRequest();
					startSubscribeTimeoutTimer();
					connection.sendRequest(request);
				} catch (IOException e) {
					LOGGER.error("Failed to connect the pool {}.", getName(), e);
					stopPool("Connection failed: " + e.getMessage());
					retryConnect(true);
				}
			}
		} else {
			throw new PoolStartException("Do not start pool " + getName() + " since manager is null.");
		}
	}

	/**
	 * Start the timer which check the subscribe response timeout
	 */
	private void startSubscribeTimeoutTimer() {
		subscribeResponseTimeoutTask = new Timer.Task() {
			public void run() {
				LOGGER.warn("Subscribe response timeout. Stopping the pool");
				stopPool("Pool subscribe response timed out.");
				retryConnect(true);
			}
		};
		subscribeResponseTimeoutTask.setName("SubscribeTimeoutTask-" + getName());
		Timer.getInstance().schedule(subscribeResponseTimeoutTask, 5000);
	}

	/**
	 * Stop the timer which check the subscribe response timeout
	 */
	private void stopSubscribeTimeoutTimer() {
		if (subscribeResponseTimeoutTask != null) {
			subscribeResponseTimeoutTask.cancel();
			subscribeResponseTimeoutTask = null;
		}
	}

	public synchronized void stopPool(String cause) {
		if (cause != null) {
			this.lastStopCause = cause;
			lastStopDate = new Date();
		}

		if (connection != null) {
			cancelTimers();
			authorizedWorkers.clear();

			isReady = false;
			isStable = false;
			manager.onPoolStateChange(this);
			LOGGER.debug("Stopping pool {}...", getName());
			if (connection != null) {
				connection.close();
				connection = null;
			}
			LOGGER.info("Pool {} stopped.", getName());
		}
	}

	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public URI getUri() {
		return uri;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Enable/Disable the pool. Use the already bound manager.
	 * 
	 * @param isEnabled
	 * @throws URISyntaxException
	 * @throws SocketException
	 * @throws Exception
	 */
	public void setEnabled(boolean isEnabled) throws PoolStartException, SocketException, URISyntaxException {
		setEnabled(isEnabled, manager);
	}

	/**
	 * Enable/Disable the pool. Throw an exception if cannot enable the pool.
	 * Use the given manager to start the pool.
	 * 
	 * @param isEnabled
	 * @throws URISyntaxException
	 * @throws SocketException
	 * @throws Exception
	 */
	public void setEnabled(boolean isEnabled, ProxyManager manager) throws PoolStartException, SocketException, URISyntaxException {
		if (this.isEnabled != isEnabled) {
			this.isEnabled = isEnabled;
			if (isEnabled) {
				startPool(manager);
			} else {
				stopPool("Pool disabled by user.");
			}
		}
	}

	public String getExtranonce1() {
		return extranonce1;
	}

	public Integer getExtranonce2Size() {
		return extranonce2Size;
	}

	public boolean isReady() {
		return isReady;
	}

	public boolean isStable() {
		return isStable;
	}

	public Double getDifficulty() {
		return difficulty;
	}

	public void processNotify(MiningNotifyNotification notify) {
		resetNotifyTimeoutTimer();
		currentJob = notify;
		manager.onPoolNotify(this, notify);

		// Set the clean job flag on the current job. Is needed for new workers
		// coming between 2 notify. They will be notifyed with the current job
		// and the flag has to be true for them.
		currentJob.setCleanJobs(true);
	}

	public void processSetDifficulty(MiningSetDifficultyNotification setDifficulty) {
		difficulty = setDifficulty.getDifficulty();
		manager.onPoolSetDifficulty(this, setDifficulty);
	}

	public void processClientReconnect(ClientReconnectNotification clientReconnect) {
		// If the pool just ask a reconnection (no host specified), just restart
		// the pool.
		if (clientReconnect.getHost() == null || clientReconnect.getHost().isEmpty()) {
			LOGGER.info("Received client.reconnect from pool {}.", getName());
			stopPool("Pool asked reconnection.");
			try {
				startPool(manager);
			} catch (Exception e) {
				LOGGER.error("Failed to restart the pool {} after a client.reconnect notification.", getName(), e);
				retryConnect(true);
			}
		} else {
			// Build the new requested URI.
			UriBuilder builder = UriBuilder.fromUri("stratum+tcp://" + clientReconnect.getHost());
			builder.port(clientReconnect.getPort() != null ? clientReconnect.getPort() : Constants.DEFAULT_POOL_PORT);
			URI newUri = builder.build();
			// Reject the reconnect request if the reconnect is on a different
			// host and isRejectReconnect is true
			if (isRejectReconnect && !uri.getHost().equalsIgnoreCase(newUri.getHost())) {
				LOGGER.warn(
						"Stopping the pool {} after a client.reconnect notification with requested host {} and port {} since option --pool-no-reconnect-different-host is true and host is different.",
						getName(), clientReconnect.getHost(), clientReconnect.getPort());
				stopPool("Pool asked reconnection on untrusted host. (" + newUri.toString() + ")");
				retryConnect(true);
			} else {
				// Else reconnect to the new host/port
				LOGGER.warn("Reconnect the pool {} to the host {} and port {}.", getName(), newUri.getHost(), newUri.getPort());
				stopPool("Pool asked reconnection on " + newUri.toString());
				host = newUri.getHost() + ":" + newUri.getPort();
				try {
					startPool(manager);
				} catch (Exception e) {
					LOGGER.error("Failed to restart the pool {} after a client.reconnect notification.", getName(), e);
					retryConnect(true);
				}
			}
		}
	}

	public void processSetExtranonce(MiningSetExtranonceNotification setExtranonce) {
		extranonce1 = setExtranonce.getExtranonce1();

		if (extranonce2Size - Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE < 1) {
			// If the extranonce2size is not big enough, we cannot generate
			// unique extranonce for workers, so deactivate the pool.
			LOGGER.error("The extranonce2Size for the pool {} is to low. Size: {}, mininum needed {}.", getName(), extranonce2Size,
					Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1);
			stopPool("Pool asked extranonce change with too small extranonce2 size (" + extranonce2Size + ". Minimum needed is "
					+ (Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1));
			retryConnect(true);
		} else {
			extranonce2Size = setExtranonce.getExtranonce2Size();
			// If extrnaonce is OK, notify the manager.
			manager.onPoolSetExtranonce(this, setExtranonce);
		}

	}

	public void processSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response) {
		stopSubscribeTimeoutTimer();
		extranonce1 = response.getExtranonce1();
		extranonce2Size = response.getExtranonce2Size();

		if (extranonce2Size - Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE < 1) {
			// If the extranonce2size is not big enough, we cannot generate
			// unique extranonce for workers, so deactivate the pool.
			LOGGER.error("The extranonce2Size for the pool {} is too low. Size: {}, mininum needed {}.", getName(), extranonce2Size,
					Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1);
			stopPool("The pool extranonce2 size is too low (" + extranonce2Size + "). Minimum is " + (Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1));
			retryConnect(true);
		} else {
			sendSubscribeExtranonceRequest();

			// Start the notify timeout timer
			resetNotifyTimeoutTimer();

			// If appendWorkerNames is true, do not try to authorize the pool
			// username. Workers will be authorized on connection. So, just
			// declare the pool as ready.
			if (isAppendWorkerNames) {
				setPoolAsReady();
			} else {
				// Send the authorize request if worker names are not appended.
				MiningAuthorizeRequest authorizeRequest = new MiningAuthorizeRequest();
				authorizeRequest.setUsername(username);
				authorizeRequest.setPassword(password);
				connection.sendRequest(authorizeRequest);
			}
		}
	}

	/**
	 * Send an extranonce subscribe request to the pool.
	 */
	private void sendSubscribeExtranonceRequest() {
		if (isExtranonceSubscribeEnabled) {
			// Else try to subscribe to extranonce change notification
			MiningExtranonceSubscribeRequest extranonceRequest = new MiningExtranonceSubscribeRequest();
			connection.sendRequest(extranonceRequest);
		}
	}

	public void processSubscribeExtranonceResponse(MiningExtranonceSubscribeRequest request, MiningExtranonceSubscribeResponse response) {
		if (response.getIsSubscribed()) {
			LOGGER.info("Extranonce change subscribed on pool {}.", getName());
		} else {
			LOGGER.info("Failed to subscribe to extranonce change on pool {}. Error: {}", getName(), response.getJsonError());
		}
	}

	public void processAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		// If the appendWorkerNames is true, the proxy does not request an
		// authorization with the configuraed pool username but will request
		// authorization for each newly connected workers.
		if (isAppendWorkerNames) {
			ResponseReceivedCallback<MiningAuthorizeRequest, MiningAuthorizeResponse> callback = authorizeCallbacks.get(response.getId());
			if (isAuthorized(request, response)) {
				// If authorized, add it in the authorized user list.
				authorizedWorkers.add(request.getUsername());
			}
			// Then call the callback.
			if (callback != null) {
				callback.onResponseReceived(request, response);
			} else {
				LOGGER.warn("Received an unexpected authorize response.", response);
			}
		} else {
			// If the appendWorkerName is false and the authorization succeed,
			// then set the pool as started
			if (isAuthorized(request, response)) {
				setPoolAsReady();
			} else {
				LOGGER.error("Stopping pool {} since user {} is not authorized. {}", getName(), username, response.getJsonError());
				String errorMessage = "User " + username + " not authorized.";
				if (response.getJsonError() != null) {
					errorMessage += " " + response.getJsonError().toString();
				}
				stopPool(errorMessage);
				retryConnect(true);
			}
		}
	}

	/**
	 * Return true if the authorize response is positive. Else, return false.
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	private boolean isAuthorized(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		// Check the P2Pool authorization. Authorized if the the result is null
		// and there is no error.
		boolean isP2PoolAuthorized = (response.getIsAuthorized() == null && response.getError() == null);

		// Check if the user is authorized in the response.
		boolean isAuthorized = isP2PoolAuthorized || (response.getIsAuthorized() != null && response.getIsAuthorized());

		return isAuthorized;
	}

	/**
	 * Set the pool as ready.
	 */
	private void setPoolAsReady() {
		LOGGER.info("Pool {} started", getName());
		this.isReady = true;
		readySince = new Date();
		testStability();
		isFirstRun = false;
		manager.onPoolStateChange(this);
	}

	public void processSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
		ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse> callback = submitCallbacks.remove(response.getId());
		callback.onResponseReceived(request, response);
	}

	/**
	 * Update the share lists with the given share. Used to compute the pool
	 * hashrate.
	 * 
	 * @param share
	 * @param isAccepted
	 */
	public void updateShareLists(Share share, boolean isAccepted) {
		if (isAccepted) {
			acceptedDifficulty.addAndGet(getDifficulty());
			lastAcceptedShares.add(share);
		} else {
			rejectedDifficulty.addAndGet(getDifficulty());
			lastRejectedShares.add(share);
		}

		purgeShareLists();
	}

	/**
	 * Purge all share lists from the old shares.
	 * 
	 * @param shareList
	 * @param share
	 */
	private void purgeShareLists() {
		HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashratePeriod);
		HashrateUtils.purgeShareList(lastRejectedShares, samplingHashratePeriod);
	}

	/**
	 * Send a submit request and return the submit response.
	 * 
	 * @param workerRequest
	 * @return
	 */
	public void submitShare(MiningSubmitRequest workerRequest, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse> callback) {
		MiningSubmitRequest poolRequest = new MiningSubmitRequest();
		poolRequest.setExtranonce2(workerRequest.getExtranonce2());
		poolRequest.setJobId(workerRequest.getJobId());
		poolRequest.setNonce(workerRequest.getNonce());
		poolRequest.setNtime(workerRequest.getNtime());

		if (isAppendWorkerNames) {
			poolRequest.setWorkerName(username + workerSeparator + workerRequest.getWorkerName());
		} else {
			poolRequest.setWorkerName(username);
		}

		submitCallbacks.put(poolRequest.getId(), callback);
		connection.sendRequest(poolRequest);
	}

	public void onDisconnectWithError(Throwable cause) {
		LOGGER.error("Disconnect of pool {}.", this, cause);

		String causeMessage = null;
		// If it is an EOFException, do not log any messages since an error has
		// surely occured on a request.
		if (!(cause instanceof EOFException)) {
			causeMessage = cause.getMessage();
		} else {
			// If it is an EOFException, log it only if a previous cause has not
			// been defined. If a cause is already defined before this one, it
			// is the real cause and this one is just the pool disconnection due
			// to the previous cause.
			// So set the EOFException message if the exception has happened
			// more than 1 second after the previous cause.
			if (lastStopDate == null || System.currentTimeMillis() > lastStopDate.getTime() + 1000) {
				causeMessage = cause.getMessage();
			}
		}

		stopPool(causeMessage);

		retryConnect(true);
	}

	/**
	 * Return a free tail for this pool.
	 * 
	 * @return
	 * @throws TooManyWorkersException
	 */
	public String getFreeTail() throws TooManyWorkersException {
		if (tails.size() > 0) {
			return tails.poll();
		} else {
			throw new TooManyWorkersException("No more tails available on pool " + getName());
		}
	}

	/**
	 * Release the given tail.
	 * 
	 * @param tail
	 */
	public void releaseTail(String tail) {
		if (tail != null && !tails.contains(tail)) {
			tails.add(tail);
		}
	}

	/**
	 * Return the extranonce2 size
	 * 
	 * @return
	 */
	public Integer getWorkerExtranonce2Size() {
		return extranonce2Size != null ? extranonce2Size - Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE : 0;
	}

	private Deque<String> buildTails() {
		Deque<String> result = new ConcurrentLinkedDeque<String>();
		int nbTails = (int) Math.pow(2, Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE * 8);
		int tailNbChars = Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE * 2;
		for (int i = 0; i < nbTails; i++) {
			String tail = Integer.toHexString(i);

			if (tail.length() > Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE * 2) {
				tail = tail.substring(0, tailNbChars);
			} else {
				while (tail.length() < Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE * 2) {
					tail = "0" + tail;
				}
			}

			result.add(tail);
		}
		return result;
	}

	public MiningNotifyNotification getCurrentStratumJob() {
		return currentJob;
	}

	/**
	 * If delayFirstRetry is false, the connect retry will happen immediatly
	 * 
	 * @param delayFirstRetry
	 */
	private synchronized void retryConnect(boolean delayFirstRetry) {
		if (connectionRetryDelay > 0) {
			if (reconnectTask != null) {
				reconnectTask.cancel();
				reconnectTask = null;
			}
			LOGGER.info("Trying reconnect of pool {} in {} seconds.", getName(), delayFirstRetry ? connectionRetryDelay : 0.001);
			reconnectTask = new Task() {
				public void run() {
					try {
						LOGGER.info("Trying reconnect of pool {}...", getName());
						startPool(manager);
					} catch (Exception e) {
						LOGGER.error("Failed to restart the pool {}.", getName(), e);
					}
				}
			};
			reconnectTask.setName("ReconnectTask-" + getName());
			Timer.getInstance().schedule(reconnectTask, delayFirstRetry ? connectionRetryDelay * 1000 : 1);
		} else {
			LOGGER.warn("Do not try to reconnect pool {} since --pool-connection-retry-delay is {}.", getName(), connectionRetryDelay);
		}
	}

	public Boolean isExtranonceSubscribeEnabled() {
		return isExtranonceSubscribeEnabled;
	}

	public void setExtranonceSubscribeEnabled(Boolean isExtranonceSubscribeEnabled) {
		this.isExtranonceSubscribeEnabled = isExtranonceSubscribeEnabled;
	}

	public Integer getNumberOfSubmit() {
		return numberOfSubmit;
	}

	public void setNumberOfSubmit(Integer numberOfSubmit) {
		this.numberOfSubmit = numberOfSubmit;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public int getNumberOfWorkersConnections() {
		return manager != null ? manager.getNumberOfWorkerConnectionsOnPool(getName()) : 0;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Pool [name=");
		builder.append(name);
		builder.append(", host=");
		builder.append(host);
		builder.append(", username=");
		builder.append(username);
		builder.append(", password=");
		builder.append(password);
		builder.append(", readySince=");
		builder.append(readySince);
		builder.append(", isReady=");
		builder.append(isReady);
		builder.append(", isEnabled=");
		builder.append(isEnabled);
		builder.append(", isStable=");
		builder.append(isStable);
		builder.append(", priority=");
		builder.append(priority);
		builder.append(", weight=");
		builder.append(weight);
		builder.append("]");
		return builder.toString();
	}

	public Double getAcceptedDifficulty() {
		return acceptedDifficulty.get();
	}

	public Double getRejectedDifficulty() {
		return rejectedDifficulty.get();
	}

	public Date getReadySince() {
		return readySince;
	}

	public Boolean getIsExtranonceSubscribeEnabled() {
		return isExtranonceSubscribeEnabled;
	}

	public void setConnectionRetryDelay(Integer connectionRetryDelay) {
		this.connectionRetryDelay = connectionRetryDelay;
	}

	public void setReconnectStabilityPeriod(Integer reconnectStabilityPeriod) {
		this.reconnectStabilityPeriod = reconnectStabilityPeriod;
	}

	public void setNoNotifyTimeout(Integer noNotifyTimeout) {
		this.noNotifyTimeout = noNotifyTimeout;
	}

	public void setRejectReconnect(Boolean isRejectReconnect) {
		this.isRejectReconnect = isRejectReconnect;
	}

	public void setSamplingHashratePeriod(Integer samplingHashratePeriod) {
		this.samplingHashratePeriod = samplingHashratePeriod * 1000;
		purgeShareLists();
	}

	/**
	 * Reset the notify timeoutTimer
	 */
	private void resetNotifyTimeoutTimer() {
		if (noNotifyTimeout > 0) {
			if (notifyTimeoutTask != null) {
				notifyTimeoutTask.cancel();
				notifyTimeoutTask = null;
			}

			notifyTimeoutTask = new Task() {
				public void run() {
					LOGGER.warn("No mining.notify received from pool {} for {} ms. Stopping the pool...", getName(), noNotifyTimeout);
					// If we have not received notify notification since DEALY,
					// stop the pool and try to reconnect.
					stopPool("No work notification for " + noNotifyTimeout / 1000 + " seconds.");
					retryConnect(false);
				}
			};
			notifyTimeoutTask.setName("NotifyTimeoutTask-" + getName());
			Timer.getInstance().schedule(notifyTimeoutTask, noNotifyTimeout * 1000);
		}
	}

	/**
	 * Start a timer which call the onPoolStable manager function if no
	 * disconnection happens before its timeout.
	 */
	private void testStability() {
		if (reconnectStabilityPeriod > 0) {
			if (!isFirstRun) {
				LOGGER.info("Testing stability of pool {} for {} seconds.", getName(), reconnectStabilityPeriod);
				if (stabilityTestTask != null) {
					stabilityTestTask.cancel();
					stabilityTestTask = null;
				}

				stabilityTestTask = new Task() {
					public void run() {
						setStable();
						if (stabilityTestTask != null) {
							stabilityTestTask.cancel();
							stabilityTestTask = null;
						}
					}
				};
				stabilityTestTask.setName("StabilityTestTask-" + getName());
				Timer.getInstance().schedule(stabilityTestTask, reconnectStabilityPeriod * 1000);
			} else {
				LOGGER.debug("Pool {} declared as stable since since first start.", getName());
				setStable();
			}
		} else {
			LOGGER.info("Pool {} declared as stable since no stability test period configured", getName());
			setStable();
		}
	}

	/**
	 * Mark the pool as stable
	 */
	private void setStable() {
		if (!isStable) {
			isStable = true;
			manager.onPoolStable(this);
		}
	}

	/**
	 * Cancel all active timers
	 */
	private synchronized void cancelTimers() {
		LOGGER.debug("Cancel all timers of pool {}.", getName());

		if (stabilityTestTask != null) {
			stabilityTestTask.cancel();
			stabilityTestTask = null;
		}

		if (reconnectTask != null) {
			reconnectTask.cancel();
			reconnectTask = null;
		}

		if (notifyTimeoutTask != null) {
			notifyTimeoutTask.cancel();
			notifyTimeoutTask = null;
		}

		if (subscribeResponseTimeoutTask != null) {
			subscribeResponseTimeoutTask.cancel();
			subscribeResponseTimeoutTask = null;
		}
	}

	public double getAcceptedHashesPerSeconds() {
		return HashrateUtils.getHashrateFromShareList(lastAcceptedShares, samplingHashratePeriod);
	}

	public double getRejectedHashesPerSeconds() {
		return HashrateUtils.getHashrateFromShareList(lastRejectedShares, samplingHashratePeriod);
	}

	/**
	 * Authorize the given worker on the pool. Throws an exception if the worker
	 * is not authorized on the pool. This method blocs until the response is
	 * received from the pool.
	 * 
	 * @param workerRequest
	 * @param callback
	 */
	public void authorizeWorker(MiningAuthorizeRequest workerRequest) throws AuthorizationException {
		// Authorize the worker only if isAppendWorkerNames is true. If true, it
		// means that each worker has to be authorized. If false, the
		// authorization has already been done with the configured username.
		if (isAppendWorkerNames) {
			String finalUserName = username + workerSeparator + workerRequest.getUsername();

			// If the worker is already authorized, do nothing
			if (authorizedWorkers.contains(finalUserName)) {
				LOGGER.debug("Worker {} already authorized on the pool {}.", finalUserName, getName());
			} else {
				LOGGER.debug("Authorize worker {} on pool {}.", finalUserName, getName());

				// Create a latch to wait the authorization response.
				CountDownLatch responseLatch = null;
				boolean sendRequest = false;

				// Synchronized to be sure that if too requests are processed at
				// the same time, only one will be performed.
				synchronized (pendingAuthorizeRequests) {
					if (!pendingAuthorizeRequests.containsKey(finalUserName)) {
						responseLatch = new CountDownLatch(1);
						pendingAuthorizeRequests.put(finalUserName, responseLatch);
						sendRequest = true;
					} else {
						responseLatch = pendingAuthorizeRequests.get(finalUserName);
					}
				}

				if (sendRequest) {
					try {
						// Response wrapper used to store the pool response
						// values
						final MiningAuthorizeResponse poolResponseWrapper = new MiningAuthorizeResponse();

						MiningAuthorizeRequest poolRequest = new MiningAuthorizeRequest();
						poolRequest.setUsername(finalUserName);
						poolRequest.setPassword(isUseWorkerPassword ? workerRequest.getPassword() : this.password);
						// Prepare the callback to call when response is
						// received.
						final CountDownLatch closureLatch = responseLatch;
						authorizeCallbacks.put(poolRequest.getId(), new ResponseReceivedCallback<MiningAuthorizeRequest, MiningAuthorizeResponse>() {
							public void onResponseReceived(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
								// Recopy values to allow blocked thread
								// to access the
								// response values.
								poolResponseWrapper.setId(response.getId());
								poolResponseWrapper.setResult(response.getResult());

								// Unblock the blocked thread.
								closureLatch.countDown();
							}
						});

						// Send the request.
						connection.sendRequest(poolRequest);

						// Wait for the response
						waitForAuthorizeResponse(finalUserName, responseLatch);

						// Check the response values
						if (poolResponseWrapper.getIsAuthorized() == null || !poolResponseWrapper.getIsAuthorized()) {
							// If the worker is not authorized, throw an
							// exception.
							throw new AuthorizationException("Worker " + finalUserName + " is not authorized on pool " + getName() + ". Cause: "
									+ (poolResponseWrapper.getJsonError() != null ? poolResponseWrapper.getJsonError() : "none."));
						}
					} finally {
						// Once the request is over, remove the latch for this
						// username.
						synchronized (pendingAuthorizeRequests) {
							pendingAuthorizeRequests.remove(finalUserName);
						}
					}
				} else {
					// Wait for the response
					waitForAuthorizeResponse(finalUserName, responseLatch);

					if (!authorizedWorkers.contains(finalUserName)) {
						throw new AuthorizationException("Worker " + finalUserName + " not authorized on pool " + getName()
								+ " after a delegated request. See the delegated request response in the logs for more details.");
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param userName
	 * @param latch
	 * @throws AuthorizationException
	 */
	private void waitForAuthorizeResponse(String userName, CountDownLatch latch) throws AuthorizationException {
		// Wait for the response for 5 seconds max.
		boolean isTimeout = false;
		try {
			isTimeout = !latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.info("Interruption on pool {} during authorization for user {}.", getName(), userName);
			throw new AuthorizationException("Interruption of authorization of user " + userName + " on pool " + getName(), e);
		}

		if (isTimeout) {
			LOGGER.warn("Timeout of worker {} authorization on pool {}.", userName, getName());
			throw new AuthorizationException("Timeout of worker " + userName + " authorization on pool " + getName());
		}
	}

	public void setAppendWorkerNames(boolean isAppendWorkerNames) {
		if (isReady) {
			throw new IllegalStateException("The pool is ready. Stop the pool before updating the extranonceSubscribeEnabled.");
		}
		this.isAppendWorkerNames = isAppendWorkerNames;
	}

	public void setUseWorkerPassword(boolean isUseWorkerPassword) {
		this.isUseWorkerPassword = isUseWorkerPassword;
	}

	public void setWorkerSeparator(String workerSeparator) {
		this.workerSeparator = workerSeparator;
	}

	public void setIsActive(boolean isActive) {
		if (isActive != this.isActive) {
			this.activeSince = isActive ? new Date() : null;
		}
		this.isActive = isActive;
	}

	public boolean isActive() {
		return this.isActive;
	}

	public Date getActiveSince() {
		return this.activeSince;
	}

	public String getLastStopCause() {
		return lastStopCause;
	}

	public Date getLastStopDate() {
		return lastStopDate;
	}

	public void setHost(String host) {
		if (isReady) {
			throw new IllegalStateException("The pool is ready. Stop the pool before updating the host.");
		}
		this.host = host;
	}

	public void setUsername(String username) {
		if (isReady) {
			throw new IllegalStateException("The pool is ready. Stop the pool before updating the username.");
		}
		this.username = username;
	}

	public void setPassword(String password) {
		if (isReady) {
			throw new IllegalStateException("The pool is ready. Stop the pool before updating the password.");
		}
		this.password = password;
	}

	public void setIsExtranonceSubscribeEnabled(Boolean isExtranonceSubscribeEnabled) {
		if (isReady) {
			throw new IllegalStateException("The pool is ready. Stop the pool before updating the extranonceSubscribeEnabled.");
		}
		this.isExtranonceSubscribeEnabled = isExtranonceSubscribeEnabled;

	}

	public boolean isAppendWorkerNames() {
		return isAppendWorkerNames;
	}

	public boolean isUseWorkerPassword() {
		return isUseWorkerPassword;
	}

	public String getWorkerSeparator() {
		return workerSeparator;
	}

}
