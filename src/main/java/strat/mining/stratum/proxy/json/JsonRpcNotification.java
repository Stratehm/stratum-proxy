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

import java.util.List;

import strat.mining.stratum.proxy.constant.Constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcNotification {

	@JsonInclude(Include.NON_NULL)
	private String jsonrpc;
	// The id of a notification is always null.
	private Integer id = null;
	private String method;
	private List<Object> params;

	public JsonRpcNotification(String method) {
		this.method = method;
	}

	public JsonRpcNotification(JsonRpcNotification request) {
		this.method = request.method;
		this.setParams(request.getParams());
	}

	public JsonRpcNotification(JsonRpcRequest request) {
		this.method = request.getMethod();
		this.setParams(request.getParams());
	}

	public Integer getId() {
		return id;
	}

	public String getMethod() {
		return method;
	}

	public List<Object> getParams() {
		return params;
	}

	public void setParams(List<Object> params) {
		this.params = params;
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

	@SuppressWarnings("unchecked")
	protected <T> T getParamsObjectAtIndex(int index) {
		T resultObject = null;
		if (params instanceof List) {
			List<Object> resultList = (List<Object>) params;
			resultObject = resultList.size() > index && resultList.get(index) != null ? (T) resultList.get(index) : null;
		}
		return resultObject;
	}

}
