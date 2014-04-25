package strat.mining.stratum.proxy.constant;

public class Constants {

	public static final String DEFAULT_USERNAME = "19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi";
	public static final String DEFAULT_PASSWORD = "x";
	public static final Integer DEFAULT_NB_CONNECTIONS = 1;
	public static final Integer DEFAULT_LISTENING_PORT = 3333;

	public static final Integer DEFAULT_POOL_PORT = 3333;
	public static final Integer DEFAULT_POOL_RECONNECT_DELAY = 5000;

	// In milli seconds. The time to wait the subscribe request before closing
	// the connection.
	public static final Integer DEFAULT_SUBSCRIBE_RECEIVE_TIMEOUT = 10000;

	// The size of a tail in bytes
	public static final Integer DEFAULT_EXTRANONCE1_TAIL_SIZE = 2;

}
