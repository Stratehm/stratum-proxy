package strat.mining.stratum.proxy.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;

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

public class Pool {

	private static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);

	private StratumProxyManager manager;

	private String host;
	private String username;
	private String password;

	private Integer difficulty;
	private String extranonce1;
	private Integer extranonce2Size;

	private boolean isActive;
	private boolean isEnabled;

	// Contains all available tails in Hexa format.
	private Deque<String> tails;

	private PoolConnection connection;

	private MiningNotifyNotification currentJob;

	private Timer reconnectTimer;

	private Boolean isExtranonceSubscribeEnabled = true;

	// Store the callbacks to call when the pool responds to a submit request.
	private Map<Long, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>> submitCallbacks;

	public Pool(String host, String username, String password) {
		super();
		this.host = host;
		this.username = username;
		this.password = password;
		this.isActive = false;
		this.isEnabled = true;

		this.tails = buildTails();
		this.submitCallbacks = Collections.synchronizedMap(new HashMap<Long, ResponseReceivedCallback<MiningSubmitRequest, MiningSubmitResponse>>());
	}

	public void startPool(StratumProxyManager manager) throws Exception {
		if (manager != null) {
			this.manager = manager;
			if (connection == null) {
				LOGGER.info("Starting pool {}...", getHost());
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
					LOGGER.warn("Failed to connect the pool {}.", getHost());
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
			LOGGER.info("Stopping pool {}...", getHost());
			connection.close();
			connection = null;
			LOGGER.info("Pool {} stopped.", getHost());
		}
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
		currentJob = notify;
		manager.onPoolNotify(this, notify);
		notify.setCleanJobs(true);
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
			LOGGER.error("The extranonce2Size for the pool {} is to low. Size: {}, mininum needed {}.", getHost(), extranonce2Size,
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
			LOGGER.error("The extranonce2Size for the pool {} is to low. Size: {}, mininum needed {}.", getHost(), extranonce2Size,
					Constants.DEFAULT_EXTRANONCE1_TAIL_SIZE + 1);
			stopPool();
		} else {
			if (isExtranonceSubscribeEnabled) {
				// Else try to subscribe to extranonce change notification
				MiningExtranonceSubscribeRequest extranonceRequest = new MiningExtranonceSubscribeRequest();
				connection.sendRequest(extranonceRequest);
			}

			// And send the authorize request
			MiningAuthorizeRequest authorizeRequest = new MiningAuthorizeRequest();
			authorizeRequest.setUsername(username);
			authorizeRequest.setPassword(password);
			connection.sendRequest(authorizeRequest);
		}
	}

	public void processSubscribeExtranonceResponse(MiningExtranonceSubscribeRequest request, MiningExtranonceSubscribeResponse response) {
		if (response.getIsSubscribed()) {
			LOGGER.info("Extranonce change subscribed on pool {}.", getHost());
		} else {
			LOGGER.info("Failed to subscribe to extranonce change on pool {}. Error: {}", getHost(), response.getJsonError());
		}
	}

	public void processAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		if (response.getIsAuthorized()) {
			LOGGER.info("Pool {} started", getHost());
			this.isActive = true;
			manager.onPoolStateChange(this);
		} else {
			LOGGER.error("Stopping pool {} since user {} is not authorized.", getHost(), username);
			stopPool();
		}
	}

	public void processSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
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
			throw new TooManyWorkersException("No more tails available on pool " + getHost());
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
		LOGGER.info("Trying reconnect of pool {} in {} ms.", getHost(), Constants.DEFAULT_POOL_RECONNECT_DELAY);
		reconnectTimer = new Timer();
		reconnectTimer.schedule(new TimerTask() {
			public void run() {
				try {
					LOGGER.info("Trying reconnect of pool {}...", getHost());
					startPool(manager);
				} catch (Exception e) {
					LOGGER.error("Failed to restart the pool {}.", getHost(), e);
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Pool [host=");
		builder.append(host);
		builder.append(", username=");
		builder.append(username);
		builder.append(", password=");
		builder.append(password);
		builder.append(", isEnabled=");
		builder.append(isEnabled);
		builder.append(", isActive=");
		builder.append(isActive);
		builder.append("]");
		return builder.toString();
	}

}
