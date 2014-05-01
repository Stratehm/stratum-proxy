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
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ResponseReceivedCallback;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
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

public class Pool implements Comparable<Pool> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);

	private StratumProxyManager manager;

	private String name;
	private String host;
	private String username;
	private String password;

	private Integer difficulty;
	private String extranonce1;
	private Integer extranonce2Size;

	private Date activeSince;
	private boolean isActive;
	private boolean isEnabled;

	// Contains all available tails in Hexa format.
	private Deque<String> tails;

	private PoolConnection connection;

	private MiningNotifyNotification currentJob;

	private Timer reconnectTimer;
	private Timer notifyTimeoutTimer;

	private Boolean isExtranonceSubscribeEnabled = true;

	private Integer numberOfSubmit = 1;

	private Integer priority;

	private AtomicLong acceptedDifficulty;
	private AtomicLong rejectedDifficulty;

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

		acceptedDifficulty = new AtomicLong(0);
		rejectedDifficulty = new AtomicLong(0);

		this.tails = buildTails();
		this.submitCallbacks = Collections.synchronizedMap(new HashMap<Long, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>>());
	}

	public void startPool(StratumProxyManager manager) throws Exception {
		if (manager != null) {
			this.manager = manager;
			if (connection == null) {
				LOGGER.info("Starting pool {}...", getName());
				URI uri = new URI("stratum+tcp://" + host);
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
			isActive = false;
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

	public Integer getDifficulty() {
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

	public void processSetExtranonce(MiningSetExtranonceNotification setExtranonce) {
		extranonce1 = setExtranonce.getExtranonce1();

		if (extranonce2Size - Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE < 1) {
			// If the extranonce2size is not big enough, we cannot generate
			// unique extranonce for workers, so deactivate the pool.
			LOGGER.error("The extranonce2Size for the pool {} is to low. Size: {}, mininum needed {}.", getName(), extranonce2Size,
					Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1);
			stopPool();
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
			manager.onPoolStateChange(this);
		} else {
			LOGGER.error("Stopping pool {} since user {} is not authorized.", getName(), username);
			stopPool();
		}
	}

	public void processSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
		if (response.getIsAccepted() != null && response.getIsAccepted()) {
			acceptedDifficulty.addAndGet(getDifficulty());
		} else {
			rejectedDifficulty.addAndGet(getDifficulty());
		}
		ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse> callback = submitCallbacks.remove(response.getId());
		callback.onResponseReceived(request, response);
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
		LOGGER.info("Trying reconnect of pool {} in {} ms.", getName(), Constants.DEFAULT_POOL_RECONNECT_DELAY);
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
		}, Constants.DEFAULT_POOL_RECONNECT_DELAY);
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

	public Long getAcceptedDifficulty() {
		return acceptedDifficulty.get();
	}

	public Long getRejectedDifficulty() {
		return rejectedDifficulty.get();
	}

	public Date getActiveSince() {
		return activeSince;
	}

	public Boolean getIsExtranonceSubscribeEnabled() {
		return isExtranonceSubscribeEnabled;
	}

	/**
	 * Reset the notify timeoutTimer
	 */
	private void resetNotifyTimeoutTimer() {
		if (notifyTimeoutTimer != null) {
			notifyTimeoutTimer.cancel();
		}

		notifyTimeoutTimer = new Timer("NotifyTimeoutTimer-" + getName());
		notifyTimeoutTimer.schedule(new TimerTask() {
			public void run() {
				LOGGER.warn("No mining.notify received from pool {} for {} ms. Stopping the pool...", getName(),
						Constants.DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT);
				// If we have not received notify notification since DEALY,
				// stop the pool and try to reconnect.
				stopPool();
				retryConnect();
			}
		}, Constants.DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT);
	}
}
