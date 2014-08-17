/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
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

import java.util.ArrayList;
import java.util.List;

import strat.mining.stratum.proxy.constant.Constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A response to a request.
 * 
 * @author strat
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcResponse {

	@JsonInclude(Include.NON_NULL)
	private String jsonrpc;
	private Long id;
	private Object error;
	private Object result;

	public JsonRpcResponse() {
	}

	public JsonRpcResponse(JsonRpcResponse response) {
		this.id = response.id;
		this.error = response.error;
		this.setResult(response.getResult());
	}

	public String getJsonrpc() {
		return jsonrpc;
	}

	public void setJsonrpc(String jsonrpc) {
		this.jsonrpc = jsonrpc;
	}

	public void setJsonRpc2() {
		setJsonrpc(Constants.JSON_RPC_2_VERSION);
	}

	@JsonIgnore
	public boolean isJsonRpc2() {
		return jsonrpc != null && jsonrpc.equalsIgnoreCase(Constants.JSON_RPC_2_VERSION);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Object getError() {
		return error;
	}

	public void setError(Object errorObject) {
		error = errorObject;
	}

	@SuppressWarnings("unchecked")
	@JsonIgnore
	public JsonRpcError getJsonError() {
		JsonRpcError errorObject = new JsonRpcError();
		if (error != null) {
			if (!isJsonRpc2()) {
				List<Object> errorList = (List<Object>) error;
				errorObject.setCode(errorList.size() > 0 && errorList.get(0) != null ? (Integer) errorList.get(0) : null);
				errorObject.setMessage(errorList.size() > 1 && errorList.get(1) != null ? (String) errorList.get(1) : null);
				errorObject.setTraceback(errorList.size() > 2 && errorList.get(2) != null ? errorList.get(2) : null);
			} else {
				return (JsonRpcError) error;
			}
		}
		return errorObject;
	}

	public void setErrorRpc(JsonRpcError errorObject) {
		if (!isJsonRpc2()) {
			List<Object> errorList = new ArrayList<Object>();
			errorList.add(errorObject.getCode());
			errorList.add(errorObject.getMessage());
			errorList.add(errorObject.getTraceback());
			error = errorList;
		} else {
			error = errorObject;
		}
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getResultObjectAtIndex(int index) {
		T resultObject = null;
		if (result instanceof List) {
			List<Object> resultList = (List<Object>) result;
			resultObject = resultList.size() > index && resultList.get(index) != null ? (T) resultList.get(index) : null;
		}
		return resultObject;
	}
}
