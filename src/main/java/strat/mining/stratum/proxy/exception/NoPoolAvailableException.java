package strat.mining.stratum.proxy.exception;

public class NoPoolAvailableException extends Exception {

	public NoPoolAvailableException() {
		super();
	}

	public NoPoolAvailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoPoolAvailableException(String message) {
		super(message);
	}

	public NoPoolAvailableException(Throwable cause) {
		super(cause);
	}

}
