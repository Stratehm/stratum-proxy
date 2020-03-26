package strat.mining.stratum.proxy.worker;

import lombok.extern.slf4j.Slf4j;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.*;
import strat.mining.stratum.proxy.manager.proxy.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;

import java.net.Socket;
import java.util.Date;
import java.util.Map;

@Slf4j
public class MultiPoolStratumWorkerConnection extends StratumWorkerConnection {

    private Map<Pool, MiningSubscribeResponse> responseList;

    private boolean readyToChangePool = false;

    public MultiPoolStratumWorkerConnection(Socket socket, ProxyManager manager) {
        super(socket, manager);
    }

    public MiningSubscribeResponse onSubscribeRequest(MiningSubscribeRequest request, Pool pool, boolean latest) {
        // Once the subscribe request is received, cancel the timeout timer.
        if (subscribeTimeoutTask != null) {
            subscribeTimeoutTask.cancel();
        }

        JsonRpcError error = null;
        try {
            extranonce1Tail = pool.getFreeTail();
            extranonce2Size = pool.getWorkerExtranonce2Size();
        } catch (TooManyWorkersException e) {
            log.error("Too many connections on pool {} for the connection {}. Sending error and close the connection.", pool.getName(),
                    getConnectionName(), e);
            error = new JsonRpcError();
            error.setCode(JsonRpcError.ErrorCode.UNKNOWN.getCode());
            error.setMessage("Too many connections on the pool.");
        }

        // Send the subscribe response
        MiningSubscribeResponse response = new MiningSubscribeResponse();
        response.setId(request.getId());
        if (error != null) {
            response.setErrorRpc(error);
        } else {
            response.setExtranonce1(pool.getExtranonce1() + extranonce1Tail);
            response.setExtranonce2Size(extranonce2Size);
            response.setSubscriptionDetails(getSubscibtionDetails());
            isActiveSince = new Date();
        }

        sendResponse(response);

        // If the subscribe succeed, send the initial notifications (difficulty
        // and notify).
        if (error == null && latest) {
            sendInitialNotifications();
            sendGetVersion();
        }

        return response;
    }

    public void setResponseList(Map<Pool, MiningSubscribeResponse> responses) {
        this.responseList = responses;
    }

    @Override
    protected void onSubmitRequest(MiningSubmitRequest request) {
        MiningSubmitResponse response = new MiningSubmitResponse();
        response.setId(request.getId());
        JsonRpcError error = null;

        if (authorizedWorkers.get(request.getWorkerName()) != null) {
            // Modify the request to add the tail of extranonce1 to the
            // submitted extranonce2
            request.setExtranonce2(extranonce1Tail + request.getExtranonce2());

            boolean isShareValid = true;
            if (validateShare) {
                isShareValid = checkTarget(request);
            }

            if (isShareValid) {
                manager.onSubmitRequest(this, request);
                this.readyToChangePool = true; // Todo: also another workers should change pool
            } else {
                error = new JsonRpcError();
                error.setCode(JsonRpcError.ErrorCode.LOW_DIFFICULTY_SHARE.getCode());
                error.setMessage("Share is above the target (proxy check)");
                response.setErrorRpc(error);
                log.debug("Share submitted by {}@{} is above the target. The share is not submitted to the pool.",
                        request.getWorkerName(), getConnectionName());
                sendResponse(response);
            }
        } else {
            error = new JsonRpcError();
            error.setCode(JsonRpcError.ErrorCode.UNAUTHORIZED_WORKER.getCode());
            error.setMessage("Submit failed. Worker not authorized on this connection.");
            response.setErrorRpc(error);
            sendResponse(response);
        }
    }

    public boolean isReadyToChangePool() {
        return readyToChangePool;
    }

    public void setReadyToChangePool(boolean readyToChangePool) {
        this.readyToChangePool = readyToChangePool;
    }
}


