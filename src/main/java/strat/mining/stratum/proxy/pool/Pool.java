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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ResponseReceivedCallback;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.hashrate.HashrateUtils;
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
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.model.Share;

import com.google.common.util.concurrent.AtomicDouble;

public class Pool implements Comparable<Pool> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);

	private StratumProxyManager manager;

	private String name;
	private String host;
	private URI uri;
	private String username;
	private String password;

	private Double difficulty;
	private String extranonce1;
	private Integer extranonce2Size;

	private Date activeSince;
	private boolean isActive;
	private boolean isEnabled;
	private boolean isStable;

	// Contains all available tails in Hexa format.
	private Deque<String> tails;

	private PoolConnection connection;

	private MiningNotifyNotification currentJob;

	private Timer reconnectTimer;
	private Timer notifyTimeoutTimer;
	private Timer stabilityTestTimer;

	private Boolean isExtranonceSubscribeEnabled = true;

	private Integer numberOfSubmit = 1;

	private Integer priority;

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

	public Pool(String name, String host, String username, String password) {
		super();
		this.name = name == null ? host : name;
		this.host = host;
		this.username = username;
		this.password = password;
		this.isActive = false;
		this.isEnabled = true;
		this.isStable = true;

		acceptedDifficulty = new AtomicDouble(0);
		rejectedDifficulty = new AtomicDouble(0);

		this.tails = buildTails();
		this.submitCallbacks = Collections.synchronizedMap(new HashMap<Long, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>>());
		this.lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		this.lastRejectedShares = new ConcurrentLinkedDeque<Share>();
	}

	public void startPool(StratumProxyManager manager) throws Exception {
		if (manager != null) {
			this.manager = manager;
			if (connection == null) {
				LOGGER.info("Starting pool {}...", getName());
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
					connection.sendRequest(request);
				} catch (IOException e) {
					LOGGER.warn("Failed to connect the pool {}.", getName());
					retryConnect();
				}
			}
		} else {
			throw new Exception("Do not start pool since manager is null.");
		}
	}

	public void stopPool() {
		if (connection != null) {
			cancelTimers();

			isActive = false;
			isStable = false;
			manager.onPoolStateChange(this);
			LOGGER.info("Stopping pool {}...", getName());
			connection.close();
			connection = null;
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
	 * Enable/Disable the pool. Throw an exception if cannot enable the pool.
	 * 
	 * @param isEnabled
	 * @throws Exception
	 */
	public void setEnabled(boolean isEnabled) throws Exception {
		if (this.isEnabled != isEnabled) {
			this.isEnabled = isEnabled;
			if (isEnabled) {
				startPool(manager);
			} else {
				stopPool();
			}
		}
	}

	public String getExtranonce1() {
		return extranonce1;
	}

	public Integer getExtranonce2Size() {
		return extranonce2Size;
	}

	public boolean isActive() {
		return isActive;
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
			stopPool();
			try {
				startPool(manager);
			} catch (Exception e) {
				LOGGER.error("Failed to restart the pool {} after a client.reconnect notification.", getName(), e);
				retryConnect();
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
				stopPool();
				retryConnect();
			} else {
				// Else reconnect to the new host/port
				LOGGER.warn("Reconnect the pool {} to the host {} and port {}.", getName(), newUri.getHost(), newUri.getPort());
				stopPool();
				host = newUri.getHost() + ":" + newUri.getPort();
				try {
					startPool(manager);
				} catch (Exception e) {
					LOGGER.error("Failed to restart the pool {} after a client.reconnect notification.", getName(), e);
					retryConnect();
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
			stopPool();
			retryConnect();
		} else {
			extranonce2Size = setExtranonce.getExtranonce2Size();
			// If extrnaonce is OK, notify the manager.
			manager.onPoolSetExtranonce(this, setExtranonce);
		}

	}

	public void processSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response) {
		extranonce1 = response.getExtranonce1();
		extranonce2Size = response.getExtranonce2Size();

		if (extranonce2Size - Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE < 1) {
			// If the extranonce2size is not big enough, we cannot generate
			// unique extranonce for workers, so deactivate the pool.
			LOGGER.error("The extranonce2Size for the pool {} is to low. Size: {}, mininum needed {}.", getName(), extranonce2Size,
					Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1);
			stopPool();
			retryConnect();
		} else {
			if (isExtranonceSubscribeEnabled) {
				// Else try to subscribe to extranonce change notification
				MiningExtranonceSubscribeRequest extranonceRequest = new MiningExtranonceSubscribeRequest();
				connection.sendRequest(extranonceRequest);
			}

			// Start the notify timeout timer
			resetNotifyTimeoutTimer();

			// And send the authorize request
			MiningAuthorizeRequest authorizeRequest = new MiningAuthorizeRequest();
			authorizeRequest.setUsername(username);
			authorizeRequest.setPassword(password);
			connection.sendRequest(authorizeRequest);
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
		if (response.getIsAuthorized()) {
			LOGGER.info("Pool {} started", getName());
			this.isActive = true;
			activeSince = new Date();
			testStability();
			manager.onPoolStateChange(this);
		} else {
			LOGGER.error("Stopping pool {} since user {} is not authorized.", getName(), username);
			stopPool();
			retryConnect();
		}
	}

	public void processSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
		if (response.getIsAccepted() != null) {
			if (response.getIsAccepted()) {
				acceptedDifficulty.addAndGet(getDifficulty());
			} else {
				rejectedDifficulty.addAndGet(getDifficulty());
			}

			updateShareLists(request, response);
		}
		ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse> callback = submitCallbacks.remove(response.getId());
		callback.onResponseReceived(request, response);
	}

	/**
	 * Update the share lists to calculate hash rates.
	 * 
	 * @param request
	 * @param response
	 */
	private void updateShareLists(MiningSubmitRequest request, MiningSubmitResponse response) {
		Share share = new Share();
		share.setDifficulty(getDifficulty());
		share.setTime(System.currentTimeMillis());

		if (response.getIsAccepted()) {
			// Add the new share
			lastAcceptedShares.add(share);
		} else {
			// Add the new share
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
	 * @param request
	 * @return
	 */
	public void submitShare(MiningSubmitRequest request, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse> callback) {
		submitCallbacks.put(request.getId(), callback);
		connection.sendRequest(request);
	}

	public void onDisconnectWithError(Throwable cause) {
		LOGGER.error("Disconnect of pool {}.", this, cause);
		stopPool();

		retryConnect();
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
		return extranonce2Size - Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE;
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

	public MiningNotifyNotification getCurrentJob() {
		return currentJob;
	}

	private void retryConnect() {
		if (connectionRetryDelay > 0) {
			LOGGER.info("Trying reconnect of pool {} in {} seconds.", getName(), connectionRetryDelay);
			reconnectTimer = new Timer("ReconnectTimer-" + getName());
			reconnectTimer.schedule(new TimerTask() {
				public void run() {
					try {
						LOGGER.info("Trying reconnect of pool {}...", getName());
						startPool(manager);
					} catch (Exception e) {
						LOGGER.error("Failed to restart the pool {}.", getName(), e);
					}
				}
			}, connectionRetryDelay * 1000);
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

	public int getNumberOfWorkersConnections() {
		return manager.getNumberOfWorkerConnectionsOnPool(getName());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Pool [name=");
		builder.append(name);
		builder.append(", host=");
		builder.append(host);
		builder.append(", uri=");
		builder.append(uri);
		builder.append(", username=");
		builder.append(username);
		builder.append(", password=");
		builder.append(password);
		builder.append(", difficulty=");
		builder.append(difficulty);
		builder.append(", extranonce1=");
		builder.append(extranonce1);
		builder.append(", extranonce2Size=");
		builder.append(extranonce2Size);
		builder.append(", activeSince=");
		builder.append(activeSince);
		builder.append(", isActive=");
		builder.append(isActive);
		builder.append(", isEnabled=");
		builder.append(isEnabled);
		builder.append(", isExtranonceSubscribeEnabled=");
		builder.append(isExtranonceSubscribeEnabled);
		builder.append(", numberOfSubmit=");
		builder.append(numberOfSubmit);
		builder.append(", priority=");
		builder.append(priority);
		builder.append(", acceptedDifficulty=");
		builder.append(acceptedDifficulty);
		builder.append(", rejectedDifficulty=");
		builder.append(rejectedDifficulty);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int compareTo(Pool o) {
		return getPriority().compareTo(o.getPriority());
	}

	public Double getAcceptedDifficulty() {
		return acceptedDifficulty.get();
	}

	public Double getRejectedDifficulty() {
		return rejectedDifficulty.get();
	}

	public Date getActiveSince() {
		return activeSince;
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
			if (notifyTimeoutTimer != null) {
				notifyTimeoutTimer.cancel();
				notifyTimeoutTimer = null;
			}

			notifyTimeoutTimer = new Timer("NotifyTimeoutTimer-" + getName());
			notifyTimeoutTimer.schedule(new TimerTask() {
				public void run() {
					LOGGER.warn("No mining.notify received from pool {} for {} ms. Stopping the pool...", getName(), noNotifyTimeout);
					// If we have not received notify notification since DEALY,
					// stop the pool and try to reconnect.
					stopPool();
					retryConnect();
				}
			}, noNotifyTimeout * 1000);
		}
	}

	/**
	 * Start a timer which call the onPoolStable manager function if no
	 * disconnection happens before its timeout.
	 */
	private void testStability() {
		if (reconnectStabilityPeriod > 0) {
			LOGGER.info("Testing stability of pool {} for {} seconds.", getName(), reconnectStabilityPeriod);
			if (stabilityTestTimer != null) {
				stabilityTestTimer.cancel();
				stabilityTestTimer = null;
			}

			stabilityTestTimer = new Timer();
			stabilityTestTimer.schedule(new TimerTask() {
				public void run() {
					setStable();
				}
			}, reconnectStabilityPeriod * 1000);
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
	private void cancelTimers() {
		if (stabilityTestTimer != null) {
			stabilityTestTimer.cancel();
			stabilityTestTimer = null;
		}

		if (reconnectTimer != null) {
			reconnectTimer.cancel();
			reconnectTimer = null;
		}

		if (notifyTimeoutTimer != null) {
			notifyTimeoutTimer.cancel();
			notifyTimeoutTimer = null;
		}
	}

	public double getAcceptedHashesPerSeconds() {
		return HashrateUtils.getHashrateFromShareList(lastAcceptedShares, samplingHashratePeriod);
	}

	public double getRejectedHashesPerSeconds() {
		return HashrateUtils.getHashrateFromShareList(lastRejectedShares, samplingHashratePeriod);
	}

}
