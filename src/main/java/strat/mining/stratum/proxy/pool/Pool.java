package strat.mining.stratum.proxy.pool;


public class Pool {

	private String host;
	private String username;
	private String password;
	private Integer nbConnections;

	public Pool(String host, String username, String password,
			Integer nbConnections) {
		super();
		this.host = host;
		this.username = username;
		this.password = password;
		this.nbConnections = nbConnections;
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

}
