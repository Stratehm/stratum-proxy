/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014-2015  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
 */
package strat.mining.stratum.proxy.json;

public class JsonRpcError {

	public enum ErrorCode {

		UNKNOWN(20), JOB_NOT_FOUND(21), DUPLICATE_SHARE(22), LOW_DIFFICULTY_SHARE(23), UNAUTHORIZED_WORKER(24), NOT_SUBSCRIBED(25);

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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonRpcError [code=");
		builder.append(code);
		builder.append(", message=");
		builder.append(message);
		builder.append(", traceback=");
		builder.append(traceback);
		builder.append("]");
		return builder.toString();
	}

}
