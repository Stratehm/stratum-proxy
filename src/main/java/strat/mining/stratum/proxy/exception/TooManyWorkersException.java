package strat.mining.stratum.proxy.exception;

public class TooManyWorkersException extends Exception {

	public TooManyWorkersException() {
		super();
	}

	public TooManyWorkersException(String message, Throwable cause) {
		super(message, cause);
	}

	public TooManyWorkersException(String message) {
		super(message);
	}

	public TooManyWorkersException(Throwable cause) {
		super(cause);
	}

}
