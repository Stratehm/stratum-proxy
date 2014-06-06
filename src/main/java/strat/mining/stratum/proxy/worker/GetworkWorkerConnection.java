package strat.mining.stratum.proxy.worker;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HexUtils;
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
import strat.mining.stratum.proxy.utils.AtomicBigInteger;
import strat.mining.stratum.proxy.utils.HashrateUtils;

public class GetworkWorkerConnection implements WorkerConnection {

	private static final byte[] ZERO_BIG_INTEGER_BYTES = { 0 };

	private Deque<Share> lastAcceptedShares;
	private Deque<Share> lastRejectedShares;

	private Pool pool;

	private List<Pair<Request, Response>> longPollingRequest;

	private Set<String> authorizedUsername;

	private InetAddress remoteAddress;

	private Integer samplingHashesPeriod = Constants.DEFAULT_WORKER_CONNECTION_HASHRATE_SAMPLING_PERIOD * 1000;

	private GetworkJobTemplate currentJob;

	private String extranonce1Tail;
	private long extranonce2MaxValue;
	private AtomicBigInteger extranonce2Counter;

	// Contains the merkleRoot as key and extranonce2/jobId as value.
	private Map<String, Pair<String, String>> extranonce2AndJobIdByMerkleRoot;

	public GetworkWorkerConnection(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		this.longPollingRequest = Collections.synchronizedList(new ArrayList<Pair<Request, Response>>());
		this.authorizedUsername = Collections.synchronizedSet(new HashSet<String>());
		this.lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		this.lastRejectedShares = new ConcurrentLinkedDeque<Share>();
		this.extranonce2Counter = new AtomicBigInteger(ZERO_BIG_INTEGER_BYTES);
		this.extranonce2AndJobIdByMerkleRoot = Collections.synchronizedMap(new HashMap<String, Pair<String, String>>());
	}

	@Override
	public void close() {
		if (pool != null) {
			if (extranonce1Tail != null) {
				pool.releaseTail(extranonce1Tail);
			}
		}

		// TODO close all LongPolling requests.
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
		if (pool != null) {
			if (extranonce1Tail != null) {
				pool.releaseTail(extranonce1Tail);
			}
		}

		pool = newPool;
		extranonce1Tail = newPool.getFreeTail();
		updateCurrentJobTemplateFromStratumJob(getPool().getCurrentStratumJob());
	}

	@Override
	public void onPoolExtranonceChange() throws ChangeExtranonceNotSupportedException {
		updateCurrentJobTemplateFromStratumJob(getPool().getCurrentStratumJob());
	}

	@Override
	public void onPoolDifficultyChanged(MiningSetDifficultyNotification notification) {
		// TODO Update connection and send long polling response
		// TODO retrieve the isScrypt value
		currentJob.setDifficulty(notification.getDifficulty(), false);

	}

	@Override
	public void onPoolNotify(MiningNotifyNotification notification) {
		updateCurrentJobTemplateFromStratumJob(notification);
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

	/**
	 * Update the current job template from the stratum notification.
	 */
	private void updateCurrentJobTemplateFromStratumJob(MiningNotifyNotification notification) {
		// Update the job only if a clean job is requested and if the connection
		// is bound to a pool.
		if (pool != null && notification.getCleanJobs()) {
			extranonce2Counter.set(0);
			extranonce2MaxValue = (int) Math.pow(2, 8 * pool.getWorkerExtranonce2Size()) - 1;
			currentJob = new GetworkJobTemplate(notification.getJobId(), notification.getBitcoinVersion(), notification.getPreviousHash(),
					notification.getCurrentNTime(), notification.getNetworkDifficultyBits(), notification.getMerkleBranches(),
					notification.getCoinbase1(), notification.getCoinbase2(), getPool().getExtranonce1() + extranonce1Tail);
		} else {
			currentJob.setJobId(notification.getJobId());
			currentJob.setBits(notification.getNetworkDifficultyBits());
			currentJob.setTime(notification.getCurrentNTime());
			currentJob.setMerkleBranches(notification.getMerkleBranches());
		}
	}

	/**
	 * Return unique data for getwork requests.
	 * 
	 * @return
	 */
	public String getGetworkData() {
		// Reset the counter if the max value is reached
		if (extranonce2Counter.get().compareTo(BigInteger.valueOf(extranonce2MaxValue)) >= 0) {
			extranonce2Counter.set(ZERO_BIG_INTEGER_BYTES);
		}
		byte[] extranonce2 = extranonce2Counter.incrementAndGet().toByteArray();
		String extranonce2String = HexUtils.convert(extranonce2);
		// The pair contains the merkleRoot on the left and the data on the
		// right.
		Pair<String, String> data = currentJob.getData(extranonce2String);

		// Save the merkleroot with the extranonce2/jobId value
		extranonce2AndJobIdByMerkleRoot.put(data.getFirst(), new Pair<String, String>(extranonce2String, currentJob.getJobId()));

		return data.getSecond();
	}

	/**
	 * Return the target of the current data.
	 * 
	 * @return
	 */
	public String getGetworkTarget() {
		return currentJob.getTarget();
	}

	/**
	 * Submit the work the pool.
	 * 
	 * @param workerName
	 * @param data
	 * @return an error message is submit has failed, or null if the share is
	 *         accepted.
	 */
	public String submitWork(String workerName, String data) {
		String errorMessage = null;

		GetworkJobSubmit jobSubmit = new GetworkJobSubmit(data);
		Pair<String, String> extranonce2JobId = extranonce2AndJobIdByMerkleRoot.get(jobSubmit.getMerkleRoot());

		MiningSubmitRequest submitRequest = new MiningSubmitRequest();
		submitRequest.setWorkerName(workerName);
		submitRequest.setExtranonce2(extranonce2JobId.getFirst());
		submitRequest.setJobId(extranonce2JobId.getSecond());
		submitRequest.setNonce(jobSubmit.getNonce());
		submitRequest.setNtime(jobSubmit.getTime());

		// TODO submit request

		return errorMessage;
	}
}
