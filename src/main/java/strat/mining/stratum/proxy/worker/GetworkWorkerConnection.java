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
package strat.mining.stratum.proxy.worker;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.http.util.HexUtils;
import org.glassfish.grizzly.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ConnectionClosedCallback;
import strat.mining.stratum.proxy.callback.LongPollingCallback;
import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.ClientShowMessageNotification;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.manager.proxy.ProxyManager;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.ArrayUtils;
import strat.mining.stratum.proxy.utils.AtomicBigInteger;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.utils.Timer.Task;
import strat.mining.stratum.proxy.utils.mining.DifficultyUtils;
import strat.mining.stratum.proxy.utils.mining.WorkerConnectionHashrateDelegator;
import strat.mining.stratum.proxy.worker.GetworkJobTemplate.GetworkRequestResult;

public class GetworkWorkerConnection implements WorkerConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetworkWorkerConnection.class);

    private static final byte[] ZERO_BIG_INTEGER_BYTES = { 0 };

    private ProxyManager manager;

    private Pool pool;

    private Set<LongPollingCallback> longPollingCallbacks;

    private Map<String, String> authorizedWorkers;

    private InetAddress remoteAddress;

    private volatile GetworkJobTemplate currentJob;

    private String extranonce1Tail;
    private long extranonce2MaxValue;
    private AtomicBigInteger extranonce2Counter;

    // Contains the merkleRoot as key and extranonce2/jobId as value.
    private Map<String, Pair<String, String>> extranonce2AndJobIdByMerkleRoot;

    private Map<Object, CountDownLatch> submitResponseLatches;
    private Map<Object, MiningSubmitResponse> submitResponses;

    private Boolean logRealShareDifficulty = ConfigurationManager.getInstance().getLogRealShareDifficulty();

    // Task executed when no getwork requests have been received during the
    // timeout delay.
    private Task getworkTimeoutTask;
    private Integer getworkTimeoutDelay = Constants.DEFAULT_GETWORK_CONNECTION_TIMEOUT;

    private WorkerConnectionHashrateDelegator workerHashrateDelegator;

    private Date isActiveSince;

    private ConnectionClosedCallback connectionClosedCallback;

    public GetworkWorkerConnection(InetAddress remoteAddress, ProxyManager manager, ConnectionClosedCallback connectionClosedCallback) {
        this.manager = manager;
        this.remoteAddress = remoteAddress;
        this.longPollingCallbacks = Collections.synchronizedSet(new HashSet<LongPollingCallback>());
        this.authorizedWorkers = Collections.synchronizedMap(new HashMap<String, String>());
        this.extranonce2Counter = new AtomicBigInteger(ZERO_BIG_INTEGER_BYTES);
        this.extranonce2AndJobIdByMerkleRoot = Collections.synchronizedMap(new HashMap<String, Pair<String, String>>());
        this.submitResponseLatches = Collections.synchronizedMap(new HashMap<Object, CountDownLatch>());
        this.submitResponses = Collections.synchronizedMap(new HashMap<Object, MiningSubmitResponse>());

        this.workerHashrateDelegator = new WorkerConnectionHashrateDelegator();
        this.isActiveSince = new Date();

        this.connectionClosedCallback = connectionClosedCallback;

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

        // Cancel all long polling requests
        cancelAllLongPolling();

        if (connectionClosedCallback != null) {
            connectionClosedCallback.onConnectionClosed(this);
        }
    }

    /**
     * Cancel all pending long polling requests
     */
    private void cancelAllLongPolling() {
        synchronized (longPollingCallbacks) {
            for (LongPollingCallback callback : longPollingCallbacks) {
                callback.onLongPollingCancel("Worker connection closed");
            }
            longPollingCallbacks.clear();
        }
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
        return "Getwork-" + getRemoteAddress().getHostAddress();
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
        currentJob.setDifficulty(notification.getDifficulty(), ConfigurationManager.getInstance().isScrypt());
        callLongPollingCallbacks();
    }

    @Override
    public void onPoolNotify(MiningNotifyNotification notification) {
        updateCurrentJobTemplateFromStratumJob(notification);
    }

    @Override
    public void onPoolSubmitResponse(MiningSubmitRequest workerRequest, MiningSubmitResponse poolResponse) {
        // Get the latch for the response.
        CountDownLatch responseLatch = submitResponseLatches.remove(workerRequest.getId());

        // If no latches, the response is maybe in timeout or not expected.
        if (responseLatch != null) {
            // Save the response with the id.
            submitResponses.put(workerRequest.getId(), poolResponse);
            // Then awake the thread waiting for the response.
            responseLatch.countDown();
        }
    }

    /**
     * Add an authorized username on this connection.
     * 
     * @param username
     */
    public void addAuthorizedUsername(String username, String password) {
        authorizedWorkers.put(username, password);
    }

    /**
     * Add a long polling Callback.
     * 
     * @param callback
     */
    public void addLongPollingCallback(LongPollingCallback callback) {
        longPollingCallbacks.add(callback);
    }

    /**
     * Remove the given long polling callback
     * 
     * @param callback
     */
    public void removeLongPollingCallback(LongPollingCallback callback) {
        longPollingCallbacks.remove(callback);
    }

    /**
     * Call all the registered callbacks for long-polling.
     */
    private void callLongPollingCallbacks() {
        synchronized (longPollingCallbacks) {
            for (LongPollingCallback callback : longPollingCallbacks) {
                callback.onLongPollingResume();
            }
            longPollingCallbacks.clear();
        }
    }

    @Override
    public double getAcceptedHashrate() {
        return workerHashrateDelegator.getAcceptedHashrate();
    }

    @Override
    public double getRejectedHashrate() {
        return workerHashrateDelegator.getRejectedHashrate();
    }

    @Override
    public void updateShareLists(Share share, boolean isAccepted) {
        workerHashrateDelegator.updateShareLists(share, isAccepted);
    }

    @Override
    public void setSamplingHashesPeriod(Integer samplingHashesPeriod) {
        workerHashrateDelegator.setSamplingHashesPeriod(samplingHashesPeriod);
    }

    /**
     * Update the current job template from the stratum notification.
     */
    private void updateCurrentJobTemplateFromStratumJob(MiningNotifyNotification notification) {
        LOGGER.debug("Update getwork job for connection {}.", getConnectionName());
        // Update the job only if a clean job is requested and if the connection
        // is bound to a pool.
        if (pool != null && notification.getCleanJobs()) {
            extranonce2Counter.set(0);
            extranonce2MaxValue = (int) Math.pow(2, 8 * pool.getWorkerExtranonce2Size()) - 1;
            currentJob = new GetworkJobTemplate(notification.getJobId(), notification.getBitcoinVersion(), notification.getPreviousHash(),
                    notification.getCurrentNTime(), notification.getNetworkDifficultyBits(), notification.getMerkleBranches(),
                    notification.getCoinbase1(), notification.getCoinbase2(), getPool().getExtranonce1() + extranonce1Tail);
            currentJob.setDifficulty(pool.getDifficulty(), ConfigurationManager.getInstance().isScrypt());

            // Reset all extranonce2 stuff
            extranonce2Counter.set(BigInteger.ZERO);
            extranonce2AndJobIdByMerkleRoot.clear();
        } else {
            currentJob.setJobId(notification.getJobId());
            currentJob.setCoinbase1(notification.getCoinbase1());
            currentJob.setCoinbase2(notification.getCoinbase2());
            currentJob.setHashPrevBlock(notification.getPreviousHash());
            currentJob.setVersion(notification.getBitcoinVersion());
            currentJob.setBits(notification.getNetworkDifficultyBits());
            currentJob.setTime(notification.getCurrentNTime());
            currentJob.setMerkleBranches(notification.getMerkleBranches());
        }

        callLongPollingCallbacks();
    }

    /**
     * Return unique data for getwork requests.
     * 
     * @return
     */
    public GetworkRequestResult getGetworkData() {
        resetGetworkTimeoutTask();

        // Retrieve a nex extranonce2 for this conenction
        String extranonce2String = getExtranonce2();

        GetworkRequestResult data = currentJob.getData(extranonce2String);

        // Save the merkleroot with the extranonce2/jobId value
        extranonce2AndJobIdByMerkleRoot.put(data.getMerkleRoot(), new Pair<String, String>(extranonce2String, currentJob.getJobId()));

        return data;
    }

    /**
     * Return a new extranonce2 for this connection
     * 
     * @return
     */
    protected String getExtranonce2() {
        // Reset the counter if the max value is reached
        if (extranonce2Counter.get().compareTo(BigInteger.valueOf(extranonce2MaxValue)) >= 0) {
            extranonce2Counter.set(ZERO_BIG_INTEGER_BYTES);
        }

        // Get the extranonce2
        byte[] extranonce2 = extranonce2Counter.incrementAndGet().toByteArray();

        // Then add padding to the extranonce2
        byte[] extranonce2Padded = new byte[pool.getWorkerExtranonce2Size()];
        ArrayUtils.copyInto(extranonce2, extranonce2Padded, extranonce2Padded.length - extranonce2.length);

        String extranonce2String = HexUtils.convert(extranonce2Padded);
        return extranonce2String;
    }

    /**
     * Return the target of the current data.
     * 
     * @return
     */
    public BigInteger getGetworkTarget() {
        return currentJob.getTargetInteger();
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
        submitRequest.setExtranonce2(extranonce1Tail + extranonce2JobId.getFirst());
        submitRequest.setJobId(extranonce2JobId.getSecond());
        submitRequest.setNtime(jobSubmit.getTime());
        submitRequest.setNonce(jobSubmit.getNonce());

        // Create the latch to wait the submit response.
        CountDownLatch responseLatch = new CountDownLatch(1);
        // Save the latch with the request id.
        submitResponseLatches.put(submitRequest.getId(), responseLatch);

        manager.onSubmitRequest(this, submitRequest);

        try {
            // Wait for the response for 1 second max.
            boolean isTimeout = !responseLatch.await(1, TimeUnit.SECONDS);
            if (isTimeout) {
                errorMessage = "MAYBE accepted share. Timeout on submit.";
                // Remove the latch since no response has been received.
                submitResponseLatches.remove(submitRequest.getId());
                LOGGER.warn("Share MAYBE accepted (diff: {}) from {}@{}. (Timeout on submit request on pool {}", pool != null ? pool.getDifficulty()
                        : "Unknown", submitRequest.getWorkerName(), getConnectionName(), pool.getName());
            } else {
                MiningSubmitResponse response = submitResponses.remove(submitRequest.getId());

                // Build the real difficulty string if enabled. Else, just
                // display the pool difficulty
                String difficultyString = pool != null ? Double.toString(pool.getDifficulty()) : "Unknown";
                if (logRealShareDifficulty) {
                    Double realShareDifficulty = DifficultyUtils.getRealShareDifficulty(currentJob, extranonce1Tail, submitRequest.getExtranonce2(),
                            submitRequest.getNtime(), submitRequest.getNonce());
                    difficultyString = Double.toString(realShareDifficulty) + "/" + difficultyString;
                }

                if (response.getIsAccepted() != null && response.getIsAccepted()) {
                    LOGGER.info("Accepted share (diff: {}) from {}@{} on {}. Yeah !!!!", difficultyString, submitRequest.getWorkerName(),
                            getConnectionName(), pool.getName());
                } else {
                    LOGGER.info("REJECTED share (diff: {}) from {}@{} on {}. Booo !!!!. Error: {}", difficultyString, submitRequest.getWorkerName(),
                            getConnectionName(), pool.getName(), response.getJsonError());
                    errorMessage = response.getJsonError() != null && response.getJsonError().getMessage() != null ? response.getJsonError()
                            .getMessage() : "Unknown";
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
            getworkTimeoutTask = null;
        }
        this.getworkTimeoutTask = new Task() {
            public void run() {
                closeWithError(new TimeoutException("No getwork request on this connection since " + getworkTimeoutDelay + " seconds."));
            }
        };
        getworkTimeoutTask.setName("GetworkTimeoutTask-" + getConnectionName());
        Timer.getInstance().schedule(getworkTimeoutTask, 1000 * getworkTimeoutDelay);
    }

    @Override
    public Map<String, String> getAuthorizedWorkers() {
        Map<String, String> result = null;
        synchronized (authorizedWorkers) {
            result = new HashMap<>(authorizedWorkers);
        }
        return result;
    }

    @Override
    public Date getActiveSince() {
        return isActiveSince;
    }

    @Override
    public String getWorkerVersion() {
        // The worker version cannot be known with Getwork.
        return null;
    }

    @Override
    public void onPoolShowMessage(ClientShowMessageNotification showMessage) {
        // Not supported with Getwork.

    }
}
