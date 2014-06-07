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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HexUtils;
import org.glassfish.grizzly.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.AtomicBigInteger;
import strat.mining.stratum.proxy.utils.HashrateUtils;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.utils.Timer.Task;

public class GetworkWorkerConnection implements WorkerConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetworkWorkerConnection.class);

	private static final byte[] ZERO_BIG_INTEGER_BYTES = { 0 };

	private StratumProxyManager manager;

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

	private Map<Long, CountDownLatch> submitResponseLatches;
	private Map<Long, MiningSubmitResponse> submitResponses;

	// Task executed when no getwork requests have been received during the
	// timeout delay.
	private Task getworkTimeoutTask;
	private Integer getworkTimeoutDelay = Constants.DEFAULT_GETWORK_CONNECTION_TIMEOUT;

	public GetworkWorkerConnection(InetAddress remoteAddress, StratumProxyManager manager) {
		this.manager = manager;
		this.remoteAddress = remoteAddress;
		this.longPollingRequest = Collections.synchronizedList(new ArrayList<Pair<Request, Response>>());
		this.authorizedUsername = Collections.synchronizedSet(new HashSet<String>());
		this.lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		this.lastRejectedShares = new ConcurrentLinkedDeque<Share>();
		this.extranonce2Counter = new AtomicBigInteger(ZERO_BIG_INTEGER_BYTES);
		this.extranonce2AndJobIdByMerkleRoot = Collections.synchronizedMap(new HashMap<String, Pair<String, String>>());
		this.submitResponseLatches = Collections.synchronizedMap(new HashMap<Long, CountDownLatch>());
		this.submitResponses = Collections.synchronizedMap(new HashMap<Long, MiningSubmitResponse>());

		// Start the getwork timeout
		resetGetworkTimeoutTask();
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

	/**
	 * To call when this connection is disconnected with error.
	 * 
	 * @param e
	 */
	private void closeWithError(Exception e) {
		close();
		manager.onWorkerDisconnection(this, e);
	}

	@Override
	public String getConnectionName() {
		return "Getwork-" + getRemoteAddress().toString();
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
		currentJob.setDifficulty(notification.getDifficulty(), CommandLineOptions.getInstance().isScrypt());

	}

	@Override
	public void onPoolNotify(MiningNotifyNotification notification) {
		updateCurrentJobTemplateFromStratumJob(notification);
	}

	@Override
	public void onPoolSubmitResponse(MiningSubmitRequest workerRequest, MiningSubmitResponse poolResponse) {
		// Get the latch for the response.
		CountDownLatch responseLatch = submitResponseLatches.remove(poolResponse.getId());

		// If no latches, the response is maybe in timeout or not expected.
		if (responseLatch != null) {
			// Save the response with the id.
			submitResponses.put(poolResponse.getId(), poolResponse);
			// Then awake the thread waiting for the response.
			responseLatch.countDown();
		}
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
		response.suspend();

	}

	/**
	 * Wake up and send response to all long polling requests.
	 */
	private void wakeUpLongPollingRequests() {
		synchronized (longPollingRequest) {
			for (Pair<Request, Response> pair : longPollingRequest) {
				// TODO fill the long polling request
				pair.getSecond().resume();
			}
			longPollingRequest.clear();
		}
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
			currentJob.setDifficulty(pool.getDifficulty(), CommandLineOptions.getInstance().isScrypt());
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
		resetGetworkTimeoutTask();

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

		// Create the latch to wait the submit response.
		CountDownLatch responseLatch = new CountDownLatch(1);
		submitResponseLatches.put(submitRequest.getId(), responseLatch);

		// Save the latch with the request id.
		manager.onSubmitRequest(this, submitRequest);

		try {
			// Wait for the response for 1 second max.
			boolean isTimeout = responseLatch.await(1, TimeUnit.SECONDS);
			if (isTimeout) {
				errorMessage = "MAYBE accepted share. Timeout on submit.";
				// Remove the latch since no response has been received.
				submitResponseLatches.remove(submitRequest.getId());
			} else {
				MiningSubmitResponse response = submitResponses.remove(submitRequest.getId());

				if (response.getIsAccepted() != null && response.getIsAccepted()) {
					LOGGER.info("Accepted share (diff: {}) from {}@{} on {}. Yeah !!!!", pool != null ? pool.getDifficulty() : "Unknown",
							submitRequest.getWorkerName(), getConnectionName(), pool.getName());
				} else {
					LOGGER.info("REJECTED share (diff: {}) from {}@{} on {}. Booo !!!!. Error: {}", pool != null ? pool.getDifficulty() : "Unknown",
							submitRequest.getWorkerName(), getConnectionName(), pool.getName(), response.getJsonError());
				}
			}
		} catch (Exception e) {
			// Nothing to do.
		}

		return errorMessage;
	}

	/**
	 * Reset the timeout of the getwork request.
	 */
	private void resetGetworkTimeoutTask() {
		if (getworkTimeoutTask != null) {
			getworkTimeoutTask.cancel();
		}
		this.getworkTimeoutTask = new Task() {
			public void run() {
				closeWithError(new TimeoutException("No getwork request on this connection since " + getworkTimeoutDelay + " seconds."));
			}
		};
		Timer.getInstance().schedule(getworkTimeoutTask, 1000 * getworkTimeoutDelay);
	}
}
