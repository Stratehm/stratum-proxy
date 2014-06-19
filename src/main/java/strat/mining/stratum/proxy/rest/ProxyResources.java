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
package strat.mining.stratum.proxy.rest;

import java.net.SocketException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.database.DatabaseManager;
import strat.mining.stratum.proxy.database.model.HashrateModel;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.NotConnectedException;
import strat.mining.stratum.proxy.exception.NotFoundException;
import strat.mining.stratum.proxy.exception.PoolStartException;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.model.User;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.dto.AddPoolDTO;
import strat.mining.stratum.proxy.rest.dto.AddressDTO;
import strat.mining.stratum.proxy.rest.dto.ChangePriorityDTO;
import strat.mining.stratum.proxy.rest.dto.ConnectionIdentifierDTO;
import strat.mining.stratum.proxy.rest.dto.HashrateDTO;
import strat.mining.stratum.proxy.rest.dto.HashrateHistoryDTO;
import strat.mining.stratum.proxy.rest.dto.LogLevelDTO;
import strat.mining.stratum.proxy.rest.dto.PoolDetailsDTO;
import strat.mining.stratum.proxy.rest.dto.PoolNameDTO;
import strat.mining.stratum.proxy.rest.dto.StatusDTO;
import strat.mining.stratum.proxy.rest.dto.UserDetailsDTO;
import strat.mining.stratum.proxy.rest.dto.UserNameDTO;
import strat.mining.stratum.proxy.rest.dto.WorkerConnectionDTO;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;
import strat.mining.stratum.proxy.worker.WorkerConnection;

@Path("proxy")
@Produces("application/json")
@Consumes("application/json")
public class ProxyResources {

	private static final String API_DATE_FORMAT = "dd-MM-yy HH:mm:ss Z";

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResources.class);

	private StratumProxyManager stratumProxyManager = StratumProxyManager.getInstance();

	private DatabaseManager databaseManager = DatabaseManager.getInstance();

	/**
	 * Get the list of connected users
	 * 
	 * @return
	 */
	@GET
	@Path("user/list")
	public Response getUsersList() {

		List<User> users = stratumProxyManager.getUsers();

		List<UserDetailsDTO> result = new ArrayList<>();
		if (users != null) {
			for (User user : users) {
				result.add(convertUserToDTO(user));
			}
		}

		Response response = Response.status(Response.Status.OK).entity(result).build();

		return response;
	}

	/**
	 * Kick the given username. Kill all connections where the user has been
	 * seen (WARN: this may kill connections supporting other users)
	 * 
	 * @return
	 */
	@POST
	@Path("user/kick")
	public Response kickUser(UserNameDTO username) {

		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.kickUser(username);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException | NotConnectedException | NotFoundException e) {
			LOGGER.error("Failed to kick the user {}.", e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to kick the user " + username.getUsername() + ". " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * Ban the given username until the proxy restart. The user will not be
	 * authorized to reconnect.
	 * 
	 * @return
	 */
	@POST
	@Path("user/ban")
	public Response banUser(UserNameDTO username) {
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.banUser(username);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException e) {
			LOGGER.error("Failed to ban the user {}.", e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to ban the user " + username.getUsername() + ". " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * Unban the user.
	 * 
	 * @return
	 */
	@POST
	@Path("user/unban")
	public Response unbanUser(UserNameDTO username) {
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.unbanUser(username);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (NotFoundException e) {
			LOGGER.error("Cannot unban user {}.", username.getUsername(), e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Cannot unban user " + username.getUsername() + ". " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * List all banned users
	 * 
	 * @return
	 */
	@GET
	@Path("user/ban/list")
	public Response listBannedUsers() {
		List<String> banedUsers = stratumProxyManager.getBannedUsers();

		Response response = Response.status(Response.Status.OK).entity(banedUsers).build();

		return response;
	}

	/**
	 * Return the list of all worker connections
	 * 
	 * @return
	 */
	@GET
	@Path("connection/list")
	public Response getConnectionsList() {

		List<WorkerConnection> workerConnections = stratumProxyManager.getWorkerConnections();

		List<WorkerConnectionDTO> result = new ArrayList<>();
		if (workerConnections != null) {
			for (WorkerConnection connection : workerConnections) {
				result.add(convertWorkerConnectionToDTO(connection));
			}
		}

		Response response = Response.status(Response.Status.OK).entity(result).build();

		return response;
	}

	/**
	 * kill the connection with the given address and port.
	 * 
	 * @return
	 */
	@POST
	@Path("connection/kick")
	public Response kickConnection(ConnectionIdentifierDTO connection) {

		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.kickConnection(connection);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException | NotFoundException e) {
			LOGGER.error("Failed to kick the connection with address {} and port {}.", connection.getAddress(), connection.getPort(), e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to kick the connection with address " + connection.getAddress() + " and port " + connection.getPort() + ". "
					+ e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * Ban the given ip address.
	 */
	@POST
	@Path("address/ban")
	public Response banIp(AddressDTO address) {

		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.banAddress(address);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException e) {
			LOGGER.error("Failed to ban the user {}.", e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to ban the address " + address.getAddress() + " connections. " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * Unban the given ip address.
	 */
	@POST
	@Path("address/unban")
	public Response unbanAddress(AddressDTO address) {

		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.unbanAddress(address);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException | NotFoundException e) {
			LOGGER.error("Cannot unban address {}.", address.getAddress(), e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Cannot unban address " + address.getAddress() + ". " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * Kick all connections with the given address.
	 */
	@POST
	@Path("address/kick")
	public Response kickAddress(AddressDTO address) {

		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.kickAddress(address);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException | NotFoundException e) {
			LOGGER.error("Failed to kick the connections with address {}", address.getAddress(), e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to kick the connections with address " + address.getAddress() + ". " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * List all banned addresses
	 */
	@POST
	@Path("address/ban/list")
	public Response listBannedAddress(AddressDTO address) {

		List<String> banedAddresses = stratumProxyManager.getBannedAddresses();

		Response response = Response.status(Response.Status.OK).entity(banedAddresses).build();

		return response;
	}

	/**
	 * Return the list of all pools
	 * 
	 * @return
	 */
	@GET
	@Path("pool/list")
	public Response getPoolsList() {

		List<Pool> pools = stratumProxyManager.getPools();

		List<PoolDetailsDTO> result = new ArrayList<>();
		if (pools != null) {
			for (Pool pool : pools) {
				result.add(convertPoolToDTO(pool));
			}
		}

		Response response = Response.status(Response.Status.OK).entity(result).build();

		return response;

	}

	/**
	 * Add the given pool
	 * 
	 * @return
	 */
	@POST
	@Path("pool/add")
	public Response addPool(AddPoolDTO addPoolDTO) {

		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.addPool(addPoolDTO);

			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (BadParameterException | SocketException | URISyntaxException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to add the pool. " + e.getMessage());
			LOGGER.error("Failed to add pool {}.", addPoolDTO, e);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(status).build();
		} catch (PoolStartException e) {
			status.setStatus(StatusDTO.PARTIALLY_DONE_STATUS);
			status.setMessage("Pool added but not started. " + e.getMessage());
			LOGGER.error("Failed to start pool {}.", addPoolDTO, e);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(status).build();
		}

		return response;
	}

	/**
	 * Remove the pool with the given name
	 * 
	 * @return
	 */
	@POST
	@Path("pool/remove")
	public Response removePool(PoolNameDTO poolName) {

		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.removePool(poolName.getPoolName());

			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (NoPoolAvailableException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.NOT_FOUND).entity(status).build();
		}

		return response;
	}

	/**
	 * Stop the given pool
	 * 
	 * @return
	 */
	@POST
	@Path("pool/disable")
	public Response disablePool(PoolNameDTO poolName) {

		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.setPoolEnabled(poolName.getPoolName(), false);

			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (NoPoolAvailableException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.NOT_FOUND).entity(status).build();
		} catch (Exception e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to start the pool. " + e.getMessage());
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(status).build();
		}

		return response;
	}

	/**
	 * Start the given pool
	 * 
	 * @return
	 */
	@POST
	@Path("pool/enable")
	public Response enablePool(PoolNameDTO poolName) {

		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.setPoolEnabled(poolName.getPoolName(), true);

			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (NoPoolAvailableException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.NOT_FOUND).entity(status).build();
		} catch (Exception e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to start the pool. " + e.getMessage());
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(status).build();
		}

		return response;
	}

	/**
	 * The priority of the given pool.
	 * 
	 * @return
	 */
	@POST
	@Path("pool/priority")
	public Response setPoolPriority(ChangePriorityDTO parameters) {

		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.setPoolPriority(parameters.getPoolName(), parameters.getPriority());

			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (NoPoolAvailableException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.NOT_FOUND).entity(status).build();
		} catch (BadParameterException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.BAD_REQUEST).entity(status).build();
		}

		return response;
	}

	@POST
	@Path("log/level")
	public Response changeLogLevel(LogLevelDTO logLevel) {
		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			Level newLevel = getLogLevel(logLevel.getLogLevel());
			LogManager.getRootLogger().setLevel(newLevel);
			LOGGER.info("Changing logLevel to {}", logLevel.getLogLevel());
			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (BadParameterException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.BAD_REQUEST).entity(status).build();
		}

		return response;
	}

	@POST
	@Path("hashrate/user")
	public Response getUserHashrateHistory(UserNameDTO username) {
		HashrateHistoryDTO result = new HashrateHistoryDTO();
		List<HashrateModel> userHashrates = databaseManager.getUserHashrate(username.getUsername());
		result.setName(username.getUsername());
		result.setHashrates(convertHashrateToDTO(userHashrates));

		Response response = Response.status(Response.Status.OK).entity(result).build();

		return response;
	}

	@POST
	@Path("hashrate/pool")
	public Response getPoolHashrateHistory(PoolNameDTO poolName) {
		HashrateHistoryDTO result = new HashrateHistoryDTO();
		Response response = null;
		Pool pool = stratumProxyManager.getPool(poolName.getPoolName());

		if (pool != null) {
			List<HashrateModel> poolHashrates = databaseManager.getPoolHashrate(pool.getHost());
			result.setName(poolName.getPoolName());
			result.setHashrates(convertHashrateToDTO(poolHashrates));
			response = Response.status(Response.Status.OK).entity(result).build();
		} else {
			response = Response.status(Response.Status.NOT_FOUND).build();
		}

		return response;
	}

	/**
	 * Return the log level from the level name. Throw an exception if the level
	 * does not exist.
	 * 
	 * @param logLevel
	 * @return
	 */
	private Level getLogLevel(String logLevel) throws BadParameterException {
		Level result = null;
		if ("FATAL".equalsIgnoreCase(logLevel)) {
			result = Level.FATAL;
		} else if ("ERROR".equalsIgnoreCase(logLevel)) {
			result = Level.ERROR;
		} else if ("WARN".equalsIgnoreCase(logLevel)) {
			result = Level.WARN;
		} else if ("INFO".equalsIgnoreCase(logLevel)) {
			result = Level.INFO;
		} else if ("DEBUG".equalsIgnoreCase(logLevel)) {
			result = Level.DEBUG;
		} else if ("TRACE".equalsIgnoreCase(logLevel)) {
			result = Level.TRACE;
		} else if ("OFF".equalsIgnoreCase(logLevel)) {
			result = Level.OFF;
		} else {
			throw new BadParameterException("Unknown LogLevel " + logLevel + ". Valid are: FATAL, ERROR, WARN, INFO, DEBUG, TRACE, OFF");
		}

		return result;
	}

	private PoolDetailsDTO convertPoolToDTO(Pool pool) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(API_DATE_FORMAT);
		PoolDetailsDTO result = new PoolDetailsDTO();

		result.setDifficulty(pool.getDifficulty() != null ? pool.getDifficulty().toString() : null);
		result.setExtranonce1(pool.getExtranonce1());
		result.setExtranonce2Size(pool.getExtranonce2Size());
		if (pool.getUri() != null) {
			result.setHost(pool.getUri().toString());
		} else {
			result.setHost(pool.getHost());
		}
		result.setIsActive(pool.isActive());
		result.setIsEnabled(pool.isEnabled());
		result.setIsStable(pool.isStable());
		result.setName(pool.getName());
		result.setNumberOfWorkerConnections(pool.getNumberOfWorkersConnections());
		result.setPassword(pool.getPassword());
		result.setUsername(pool.getUsername());
		result.setWorkerExtranonce2Size(pool.getWorkerExtranonce2Size());
		result.setPriority(pool.getPriority());
		result.setAcceptedDifficulty(pool.getAcceptedDifficulty());
		result.setIsActiveSince(pool.getActiveSince() != null ? simpleDateFormat.format(pool.getActiveSince()) : null);
		result.setRejectedDifficulty(pool.getRejectedDifficulty());
		result.setIsExtranonceSubscribeEnabled(pool.isExtranonceSubscribeEnabled());
		result.setAcceptedHashesPerSeconds(Double.valueOf(pool.getAcceptedHashesPerSeconds()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(pool.getRejectedHashesPerSeconds()).longValue());

		return result;
	}

	private WorkerConnectionDTO convertWorkerConnectionToDTO(WorkerConnection connection) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(API_DATE_FORMAT);
		WorkerConnectionDTO result = new WorkerConnectionDTO();
		result.setRemoteHost(connection.getRemoteAddress().toString());
		result.setAcceptedHashesPerSeconds(Double.valueOf(connection.getAcceptedHashrate()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(connection.getRejectedHashrate()).longValue());
		result.setPoolName(connection.getPool().getName());
		result.setAuthorizedUsers(new ArrayList<>(connection.getAuthorizedWorkers()));
		result.setIsActiveSince(simpleDateFormat.format(connection.getActiveSince()));

		if (connection instanceof StratumWorkerConnection) {
			StratumWorkerConnection stratumConnection = (StratumWorkerConnection) connection;
			result.setConnectionType("tcp+stratum");
			result.setIsExtranonceNotificationSupported(stratumConnection.isSetExtranonceNotificationSupported());
		} else {
			result.setConnectionType("getwork");
		}

		return result;
	}

	private UserDetailsDTO convertUserToDTO(User user) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(API_DATE_FORMAT);
		UserDetailsDTO result = new UserDetailsDTO();
		result.setName(user.getName());
		result.setFirstConnectionDate(simpleDateFormat.format(user.getCreationTime()));
		result.setLastShareSubmitted(user.getLastShareSubmitted() != null ? simpleDateFormat.format(user.getLastShareSubmitted()) : null);
		result.setAcceptedHashesPerSeconds(Double.valueOf(user.getAcceptedHashrate()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(user.getRejectedHashrate()).longValue());

		List<WorkerConnectionDTO> connections = new ArrayList<>(user.getWorkerConnections().size());
		for (WorkerConnection connection : user.getWorkerConnections()) {
			connections.add(convertWorkerConnectionToDTO(connection));
		}
		result.setConnections(connections);

		return result;
	}

	private List<HashrateDTO> convertHashrateToDTO(List<HashrateModel> userHashrates) {
		List<HashrateDTO> result = new ArrayList<>();

		if (userHashrates != null) {
			for (HashrateModel model : userHashrates) {
				HashrateDTO dto = new HashrateDTO();
				dto.setAcceptedHashrate(model.getAcceptedHashrate());
				dto.setRejectedHashrate(model.getRejectedHashrate());
				dto.setCaptureTimeUTC(model.getCaptureTime() / 1000);

				result.add(dto);
			}
		}
		return result;
	}

}
