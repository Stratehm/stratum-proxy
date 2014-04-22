package strat.mining.stratum.proxy.pool;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningAuthorizeResponse;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;

public class Pool {

	private static final Logger LOGGER = LoggerFactory.getLogger(Pool.class);

	public static final Integer DEFAULT_POOL_PORT = 3333;

	private String host;
	private String username;
	private String password;
	private Integer nbConnections;

	private Integer difficulty;
	private String extranonce1;
	private Integer extranonce2Size;

	private boolean isActive;
	private boolean isEnabled;

	private PoolConnection connection;

	public Pool(String host, String username, String password, Integer nbConnections) {
		super();
		this.host = host;
		this.username = username;
		this.password = password;
		this.nbConnections = nbConnections;
		this.isActive = false;
		this.isEnabled = true;
	}

	public void startPool() throws Exception {
		if (connection == null) {
			LOGGER.info("Starting pool {}...", getHost());
			URI uri = new URI("stratum+tcp://" + host);
			Socket socket = new Socket();
			socket.setKeepAlive(true);
			socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort() > -1 ? uri.getPort() : DEFAULT_POOL_PORT));
			connection = new PoolConnection(this, socket);
			connection.startReading();

			MiningSubscribeRequest request = new MiningSubscribeRequest();
			connection.sendRequest(request);
		}
	}

	public void stopPool() {
		if (connection != null) {
			LOGGER.info("Stopping pool {}...", getHost());
			connection.close();
			connection = null;
			isActive = false;
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

	public Integer getNbConnections() {
		return nbConnections;
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
				startPool();
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

	}

	public void processSetDifficulty(MiningSetDifficultyNotification setDifficulty) {
		difficulty = setDifficulty.getDifficulty();
	}

	public void processSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response) {
		extranonce1 = response.getExtranonce1();
		extranonce2Size = response.getExtranonce2Size();

		MiningAuthorizeRequest authorizeRequest = new MiningAuthorizeRequest();
		authorizeRequest.setUsername(username);
		authorizeRequest.setPassword(password);
		connection.sendRequest(authorizeRequest);
	}

	public void processAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		if (response.getIsAuthorized()) {
			LOGGER.info("Pool {} started", getHost());
			this.isActive = true;
		} else {
			System.out.println("Stopping pool since user not authorized.");
			stopPool();
		}
	}

	public void processSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {

	}

	public void onDisconnect(Throwable cause) {
		LOGGER.error("Disconnect of pool {}.", this, cause);
		stopPool();
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
		builder.append(", nbConnections=");
		builder.append(nbConnections);
		builder.append(", isEnabled=");
		builder.append(isEnabled);
		builder.append("]");
		return builder.toString();
	}

}
