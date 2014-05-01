package strat.mining.stratum.proxy.constant;

public class Constants {

	public static final String DEFAULT_USERNAME = "19wv8FQKv3NkwTdzBCQn1AGsb9ghqBPWXi";
	public static final String DEFAULT_PASSWORD = "x";
	public static final Integer DEFAULT_STRATUM_LISTENING_PORT = 3333;
	public static final Integer DEFAULT_REST_LISTENING_PORT = 8888;
	public static final String DEFAULT_REST_LISTENING_ADDRESS = "0.0.0.0";

	public static final Integer DEFAULT_POOL_PORT = 3333;
	public static final Integer DEFAULT_POOL_RECONNECT_DELAY = 5000;
	public static final Integer DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT = 120000;

	// In milli seconds. The time to wait the subscribe request before closing
	// the connection.
	public static final Integer DEFAULT_SUBSCRIBE_RECEIVE_TIMEOUT = 10000;

	// The size of a tail in bytes
	public static final Integer DEFAULT_EXTRANONCE1_TAIL_SIZE = 1;

	public static final String ERROR_MESSAGE_SUBSCRIBE_EXTRANONCE = "Method 'subscribe' not found for service 'mining.extranonce'";

}
