package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcError {

	public enum ErrorCode {

		UNKNOWN(20), JOB_NOT_FOUND(21), DUPLICATE_SHARE(22), LOW_DIFFICULTY_SHARE(
				23), UNAUTHORIZED_WORKER(24), NOT_SUBSCRIBED(25);

		private int code;

		private ErrorCode(int code) {
			this.code = code;
		}

		public int getCode() {
			return this.code;
		}

		public static ErrorCode getErrorFromCode(int errorCode) {
			ErrorCode result = UNKNOWN;
			for (ErrorCode error : ErrorCode.values()) {
				if (error.getCode() == errorCode) {
					result = error;
				}
			}
			return result;
		}

	}

	private Integer code;
	private String message;
	private Object traceback;

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getTraceback() {
		return traceback;
	}

	public void setTraceback(Object traceback) {
		this.traceback = traceback;
	}

}
