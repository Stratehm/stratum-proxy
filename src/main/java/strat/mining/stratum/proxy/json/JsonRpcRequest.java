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

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import strat.mining.stratum.proxy.constant.Constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest {

	private static final AtomicLong nextRequestId = new AtomicLong(0);

	@JsonInclude(Include.NON_NULL)
	private String jsonrpc;
	private Object id;
	private String method;
	private List<Object> params;

	protected JsonRpcRequest() {
		Long nextId = nextRequestId.getAndIncrement();
		// Use an integer as id instead of a long when possible. Else, it is
		// harder to match a response with a request since Jackson will parse
		// response with Integer when needed, and long if the value overflow an
		// integer. (Thus, in request maps, Long of the request will never match
		// with the Integer id of the response, even if values are the same).
		if (Integer.MAX_VALUE >= nextId) {
			id = nextId.intValue();
		} else {
			id = nextId;
		}
	}

	public JsonRpcRequest(String method) {
		this();
		this.method = method;
	}

	public JsonRpcRequest(JsonRpcRequest request) {
		this.id = request.id;
		this.method = request.method;
		this.setParams(request.getParams());
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
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

	@JsonIgnore
	public boolean isJsonRpc2() {
		return jsonrpc != null && jsonrpc.equalsIgnoreCase(Constants.JSON_RPC_2_VERSION);
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

}
