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
package strat.mining.stratum.proxy.pool;

import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.Launcher;
import strat.mining.stratum.proxy.json.ClientGetVersionRequest;
import strat.mining.stratum.proxy.json.ClientGetVersionResponse;
import strat.mining.stratum.proxy.json.ClientReconnectNotification;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningAuthorizeResponse;
import strat.mining.stratum.proxy.json.MiningExtranonceSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningExtranonceSubscribeResponse;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSetExtranonceNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeResponse;
import strat.mining.stratum.proxy.network.StratumConnection;

public class PoolConnection extends StratumConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(PoolConnection.class);

	private Pool pool;

	public PoolConnection(Pool pool, Socket socket) {
		super(socket);
		this.pool = pool;
	}

	@Override
	protected void onParsingError(String line, Throwable throwable) {
		LOGGER.error("{}. JSON-RPC parsing error with line: {}.", getConnectionName(), line, throwable);
	}

	@Override
	protected void onDisconnectWithError(Throwable cause) {
		pool.onDisconnectWithError(cause);
	}

	@Override
	public String getConnectionName() {
		return "Pool-" + pool.getName();
	}

	@Override
	protected void onNotify(MiningNotifyNotification notify) {
		pool.processNotify(notify);
	}

	@Override
	protected void onSetDifficulty(MiningSetDifficultyNotification setDifficulty) {
		pool.processSetDifficulty(setDifficulty);
	}

	@Override
	protected void onSetExtranonce(MiningSetExtranonceNotification setExtranonce) {
		pool.processSetExtranonce(setExtranonce);
	}

	@Override
	protected void onClientReconnect(ClientReconnectNotification clientReconnect) {
		pool.processClientReconnect(clientReconnect);
	}

	@Override
	protected void onAuthorizeRequest(MiningAuthorizeRequest request) {
		LOGGER.warn("Pool {} received an Authorize request. This should not happen.", pool.getName());
	}

	@Override
	protected void onSubscribeRequest(MiningSubscribeRequest request) {
		LOGGER.warn("Pool {} received a Subscribe request. This should not happen.", pool.getName());
	}

	@Override
	protected void onSubmitRequest(MiningSubmitRequest request) {
		LOGGER.warn("Pool {} received an Submit request. This should not happen.", pool.getName());
	}

	@Override
	protected void onExtranonceSubscribeRequest(MiningExtranonceSubscribeRequest request) {
		LOGGER.warn("Pool {} received an Extranonce Subscribe request. This should not happen.", pool.getName());
	}

	@Override
	protected void onGetVersionRequest(ClientGetVersionRequest request) {
		LOGGER.debug("Pool {} reply to GetVersion request.", pool.getName());
		ClientGetVersionResponse response = new ClientGetVersionResponse();
		response.setId(request.getId());
		response.setVersion(Launcher.getVersion());
		sendResponse(response);
	}

	@Override
	protected void onAuthorizeResponse(MiningAuthorizeRequest request, MiningAuthorizeResponse response) {
		pool.processAuthorizeResponse(request, response);
	}

	@Override
	protected void onSubscribeResponse(MiningSubscribeRequest request, MiningSubscribeResponse response) {
		pool.processSubscribeResponse(request, response);
	}

	@Override
	protected void onExtranonceSubscribeResponse(MiningExtranonceSubscribeRequest request, MiningExtranonceSubscribeResponse response) {
		pool.processSubscribeExtranonceResponse(request, response);
	}

	@Override
	protected void onSubmitResponse(MiningSubmitRequest request, MiningSubmitResponse response) {
		pool.processSubmitResponse(request, response);
	}

	@Override
	protected void onGetVersionResponse(ClientGetVersionRequest request, ClientGetVersionResponse response) {
		LOGGER.warn("Pool {} received a GetVersion response. This should not happen.", pool.getName());
	}
}
