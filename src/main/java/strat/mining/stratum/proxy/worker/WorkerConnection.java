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

import java.util.Date;
import java.util.Map;

import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.ClientShowMessageNotification;
import strat.mining.stratum.proxy.json.MiningNotifyNotification;
import strat.mining.stratum.proxy.json.MiningSetDifficultyNotification;
import strat.mining.stratum.proxy.json.MiningSubmitRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.model.Share;
import strat.mining.stratum.proxy.network.Connection;
import strat.mining.stratum.proxy.pool.Pool;

public interface WorkerConnection extends Connection {

    /**
     * Return the pool on which this connection is bound.
     * 
     * @return
     */
    public Pool getPool();

    /**
     * Return true if the connection is connected
     * 
     * @return
     */
    public boolean isConnected();

    /**
     * Reset the connection with the parameters of the new pool. May close the
     * connection if setExtranonce is not supported.
     * 
     * @param newPool
     * @throws TooManyWorkersException
     * @throws ChangeExtranonceNotSupportedException
     */
    public void rebindToPool(Pool newPool) throws TooManyWorkersException, ChangeExtranonceNotSupportedException;

    /**
     * Called when the pool change its extranonce. Send the extranonce change to
     * the worker. Throw an exception if the extranonce change is not supported
     * on the fly.
     */
    public void onPoolExtranonceChange() throws ChangeExtranonceNotSupportedException;

    /**
     * Called when the pool difficulty has changed
     * 
     * @param notification
     */
    public void onPoolDifficultyChanged(MiningSetDifficultyNotification notification);

    /**
     * Called when the pool has sent a new notify notification.
     * 
     * @param notification
     */
    public void onPoolNotify(MiningNotifyNotification notification);

    /**
     * Called when the pool has sent a new message.
     * 
     * @param showMessage
     */
    public void onPoolShowMessage(ClientShowMessageNotification showMessage);

    /**
     * Update the shares lists with the given share to compute hashrate
     * 
     * @param share
     * @param isAccepted
     */
    public void updateShareLists(Share share, boolean isAccepted);

    /**
     * Called when the pool has answered to a submit request.
     * 
     * @param workerRequest
     * @param poolResponse
     */
    public void onPoolSubmitResponse(MiningSubmitRequest workerRequest, MiningSubmitResponse poolResponse);

    /**
     * Set the sampling period to compute the hashrate of the connection. he
     * period is in seconds.
     * 
     * @param samplingHashesPeriod
     */
    public void setSamplingHashesPeriod(Integer samplingHashesPeriod);

    /**
     * Return the number of rejected hashes per seconds of the connection.
     * 
     * @return
     */
    public double getRejectedHashrate();

    /**
     * Return the of accepted hashes per seconds of the connection.
     * 
     * @return
     */
    public double getAcceptedHashrate();

    /**
     * Return a read-only map of users/passwords that are authorized on this
     * connection.
     * 
     * @return
     */
    public Map<String, String> getAuthorizedWorkers();

    /**
     * Return the of activation of this connection
     * 
     * @return
     */
    public Date getActiveSince();

    /**
     * Return the worker version.
     * 
     * @return
     */
    public String getWorkerVersion();
}
