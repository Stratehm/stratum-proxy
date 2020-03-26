package strat.mining.stratum.proxy.manager.proxy;

import strat.mining.stratum.proxy.exception.*;
import strat.mining.stratum.proxy.json.*;
import strat.mining.stratum.proxy.model.User;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.dto.*;
import strat.mining.stratum.proxy.worker.WorkerConnection;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.List;

public interface ProxyManagerInterface {

    void startPools(List<Pool> pools);

    void stopPools();

    void startListeningIncomingConnections(String bindInterface, Integer port) throws IOException;

    void stopListeningIncomingConnections();

    void closeAllWorkerConnections();

    Pool onSubscribeRequest(WorkerConnection connection, MiningSubscribeRequest request) throws NoPoolAvailableException;

    void onAuthorizeRequest(WorkerConnection connection, MiningAuthorizeRequest request) throws AuthorizationException;

    void onSubmitRequest(WorkerConnection workerConnection, MiningSubmitRequest workerRequest);

    void onPoolSetDifficulty(Pool pool, MiningSetDifficultyNotification setDifficulty);

    void onPoolSetExtranonce(Pool pool, MiningSetExtranonceNotification setExtranonce);

    void onPoolNotify(Pool pool, MiningNotifyNotification notify) throws NoPoolAvailableException, ChangeExtranonceNotSupportedException, TooManyWorkersException;

    void onPoolShowMessage(Pool pool, ClientShowMessageNotification showMessage);

    void onWorkerDisconnection(WorkerConnection workerConnection, Throwable cause);

    void onPoolStateChange(Pool pool);

    void onPoolStable(Pool pool);

    void switchPoolForConnection(WorkerConnection connection, Pool newPool) throws TooManyWorkersException,
            ChangeExtranonceNotSupportedException;

    void setPoolPriority(String poolName, int newPriority) throws NoPoolAvailableException, BadParameterException;

    void setPoolEnabled(String poolName, boolean isEnabled) throws NoPoolAvailableException, Exception;

    Pool getPool(String poolName);

    List<Pool> getPools();

    List<strat.mining.stratum.proxy.pool.Quota> getQuotas();

    int getNumberOfWorkerConnectionsOnPool(String poolName);

    List<WorkerConnection> getWorkerConnections();

    List<User> getUsers();

    Pool addPool(AddPoolDTO addPoolDTO) throws BadParameterException, SocketException, PoolStartException, URISyntaxException;

    void removePool(String poolName, Boolean keepHistory) throws NoPoolAvailableException;

    void kickUser(UserNameDTO username) throws BadParameterException, NotConnectedException, NotFoundException;

    void banUser(UserNameDTO username) throws BadParameterException;

    void unbanUser(UserNameDTO username) throws NotFoundException;

    List<String> getBannedUsers();

    void kickConnection(ConnectionIdentifierDTO connection) throws BadParameterException, NotFoundException;

    void kickAddress(AddressDTO address) throws BadParameterException, NotFoundException;

    void banAddress(AddressDTO address) throws BadParameterException;

    void unbanAddress(AddressDTO address) throws NotFoundException, BadParameterException;

    List<String> getBannedAddresses();

    void setPoolSwitchingStrategy(String strategyName) throws UnsupportedPoolSwitchingStrategyException;

    void updatePool(UpdatePoolDTO poolToUpdate) throws NotFoundException, SocketException, PoolStartException, URISyntaxException,
            BadParameterException;

    void changeWorkerPool(WorkerConnection connection) throws NoPoolAvailableException, ChangeExtranonceNotSupportedException, TooManyWorkersException;

    void changeAllWorkersPool();
}
