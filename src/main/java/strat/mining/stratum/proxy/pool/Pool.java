package strat.mining.stratum.proxy.pool;

import java.net.Socket;
import java.net.URI;

public class Pool {

	public static final Integer DEFAULT_POOL_PORT = 3333;

	private String host;
	private String username;
	private String password;
	private Integer nbConnections;

	private String extranonce1;
	private Integer extranonce2Size;

	private boolean isActive;
	private boolean isEnabled;

	private PoolConnection connection;

	public Pool(String host, String username, String password,
			Integer nbConnections) {
		super();
		this.host = host;
		this.username = username;
		this.password = password;
		this.nbConnections = nbConnections;
		this.isActive = false;
		this.isEnabled = true;
	}

	public void startPool() throws Exception {
		URI uri = new URI("stratum+tcp://" + host);
		Socket socket = new Socket(uri.getHost(),
				uri.getPort() > -1 ? uri.getPort() : DEFAULT_POOL_PORT);
		connection = new PoolConnection(this, socket);
	}

	public void stopPool() {
		if (connection != null) {
			connection.close();
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

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
		// TODO close the pool connection or connect it.
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

}
