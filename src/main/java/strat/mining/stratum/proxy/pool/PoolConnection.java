package strat.mining.stratum.proxy.pool;

import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		return "Pool-" + pool.getHost();
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
	protected void onAuthorizeRequest(MiningAuthorizeRequest request) {
		LOGGER.warn("Pool {} received an Authorize request. This should not happen.", pool.getHost());
	}

	@Override
	protected void onSubscribeRequest(MiningSubscribeRequest request) {
		LOGGER.warn("Pool {} received a Subscribe request. This should not happen.", pool.getHost());
	}

	@Override
	protected void onSubmitRequest(MiningSubmitRequest request) {
		LOGGER.warn("Pool {} received an Submit request. This should not happen.", pool.getHost());
	}

	@Override
	protected void onExtranonceSubscribeRequest(MiningExtranonceSubscribeRequest request) {
		LOGGER.warn("Pool {} received an Extranonce Subscribe request. This should not happen.", pool.getHost());
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

}
