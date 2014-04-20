package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcError {

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
