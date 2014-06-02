package strat.mining.stratum.proxy.worker;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.utils.Pair;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.HashrateUtils;

public class GetworkWorkerConnection implements WorkerConnection {

	private Deque<Share> lastAcceptedShares;
	private Deque<Share> lastRejectedShares;

	private Pool pool;

	private List<Pair<Request, Response>> longPollingRequest;

	private Set<String> authorizedUsername;

	private InetAddress remoteAddress;

	private Integer samplingHashesPeriod = Constants.DEFAULT_WORKER_CONNECTION_HASHRATE_SAMPLING_PERIOD * 1000;

	public GetworkWorkerConnection(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		this.longPollingRequest = Collections.synchronizedList(new ArrayList<Pair<Request, Response>>());
		this.authorizedUsername = Collections.synchronizedSet(new HashSet<String>());
		lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		lastRejectedShares = new ConcurrentLinkedDeque<Share>();
	}

	@Override
	public void close() {

	}

	@Override
	public String getConnectionName() {
		return getRemoteAddress().toString();
	}

	@Override
	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public Integer getRemotePort() {
		return null;
	}

	@Override
	public Pool getPool() {
		return pool;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public void rebindToPool(Pool newPool) throws TooManyWorkersException, ChangeExtranonceNotSupportedException {
		pool = newPool;
	}

	@Override
	public void onPoolExtranonceChange() throws ChangeExtranonceNotSupportedException {
		// TODO Update connection and send long polling response

	}

	@Override
	public void onPoolDifficultyChanged(MiningSetDifficultyNotification notification) {
		// TODO Update connection and send long polling response

	}

	@Override
	public void onPoolNotify(MiningNotifyNotification notification) {
		GetworkJobTemplate job = new GetworkJobTemplate();
		Long bitcoinVersion = Long.valueOf(notification.getBitcoinVersion(), 16);
		job.setVersion(bitcoinVersion);

		Long currentTime = Long.valueOf(notification.getCurrentNTime(), 16);
		job.setTime(currentTime);

		Long nBits = Long.valueOf(notification.getNetworkDifficultyBits(), 16);
		job.setBits(nBits);

		notification.getMerkleBranches();

	}

	@Override
	public void onPoolSubmitResponse(MiningSubmitRequest workerRequest, MiningSubmitResponse poolResponse) {
		// TODO nothing todo

	}

	/**
	 * Add an authorized username on this connection.
	 * 
	 * @param username
	 */
	public void addAuthorizedUsername(String username) {
		authorizedUsername.add(username);
	}

	/**
	 * Add a long polling request.
	 * 
	 * @param request
	 * @param response
	 */
	public void addLongPollingRequest(Request request, Response response) {

	}

	@Override
	public void updateShareLists(Share share, boolean isAccepted) {
		if (isAccepted) {
			lastAcceptedShares.addLast(share);
			HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		} else {
			lastRejectedShares.addLast(share);
			HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		}
	}

	/**
	 * Return the of accepted hashes per seconds of the user.
	 * 
	 * @return
	 */
	public double getAcceptedHashrate() {
		HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastAcceptedShares, samplingHashesPeriod);
	}

	/**
	 * Return the number of rejected hashes per seconds of the user.
	 * 
	 * @return
	 */
	public double getRejectedHashrate() {
		HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastRejectedShares, samplingHashesPeriod);
	}

	public void setSamplingHashesPeriod(Integer samplingHashesPeriod) {
		this.samplingHashesPeriod = samplingHashesPeriod * 1000;
	}

}
