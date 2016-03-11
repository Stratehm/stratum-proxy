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
package strat.mining.stratum.proxy.rest;

import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.database.DatabaseManager;
import strat.mining.stratum.proxy.database.model.HashrateModel;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.NotConnectedException;
import strat.mining.stratum.proxy.exception.NotFoundException;
import strat.mining.stratum.proxy.exception.PoolStartException;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.model.User;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.authentication.PubliclyAvailable;
import strat.mining.stratum.proxy.rest.dto.AddPoolDTO;
import strat.mining.stratum.proxy.rest.dto.AddressDTO;
import strat.mining.stratum.proxy.rest.dto.AuthenticationDetails;
import strat.mining.stratum.proxy.rest.dto.ChangePriorityDTO;
import strat.mining.stratum.proxy.rest.dto.ConnectionIdentifierDTO;
import strat.mining.stratum.proxy.rest.dto.HashrateDTO;
import strat.mining.stratum.proxy.rest.dto.HashrateHistoryDTO;
import strat.mining.stratum.proxy.rest.dto.LogEntry;
import strat.mining.stratum.proxy.rest.dto.LogLevelDTO;
import strat.mining.stratum.proxy.rest.dto.PoolDetailsDTO;
import strat.mining.stratum.proxy.rest.dto.PoolNameDTO;
import strat.mining.stratum.proxy.rest.dto.ProxyVersionDTO;
import strat.mining.stratum.proxy.rest.dto.RemovePoolDTO;
import strat.mining.stratum.proxy.rest.dto.StatusDTO;
import strat.mining.stratum.proxy.rest.dto.SummaryDTO;
import strat.mining.stratum.proxy.rest.dto.TimestampDTO;
import strat.mining.stratum.proxy.rest.dto.UpdatePoolDTO;
import strat.mining.stratum.proxy.rest.dto.UserDetailsDTO;
import strat.mining.stratum.proxy.rest.dto.UserNameDTO;
import strat.mining.stratum.proxy.rest.dto.WorkerConnectionDTO;
import strat.mining.stratum.proxy.worker.StratumWorkerConnection;
import strat.mining.stratum.proxy.worker.WorkerConnection;

@Path("/")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "/", description = "API to manage the proxy")
public class ProxyResources {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResources.class);

	private ProxyManager stratumProxyManager = ProxyManager.getInstance();

	private DatabaseManager databaseManager = DatabaseManager.getInstance();

	private strat.mining.stratum.proxy.manager.LogManager logManager = strat.mining.stratum.proxy.manager.LogManager.getInstance();

	/**
	 * Return the details concerning authentication. rights.
	 * 
	 * @return
	 */
	@GET
	@Path("misc/authentication/details")
	@PubliclyAvailable
	public Response getAuthenticationDetails() {
		AuthenticationDetails result = new AuthenticationDetails();
		result.setAuthenticationNeededForWriteAccess(StringUtils.isNotBlank(ConfigurationManager.getInstance().getApiUser()) && ConfigurationManager.getInstance().getApiReadOnlyAccessEnabled());

		return Response.status(Response.Status.OK).entity(result).build();
	}

	/**
	 * Get the list of connected users
	 * 
	 * @return
	 * 
	 */
	@GET
	@Path("user/list")
	@ApiOperation(value = "Get the list of users that has been connected at least once since the proxy is started.", response = UserDetailsDTO.class, responseContainer = "List")
	@PubliclyAvailable
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
	 * 
	 */
	@POST
	@Path("user/kick")
	@ApiOperation(value = "Kick the given username. Kill all connections where the user has been seen (WARN: this may kill connections supporting other users)", response = StatusDTO.class)
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
	 * 
	 */
	@POST
	@Path("user/ban")
	@ApiOperation(value = "Ban the given username until the proxy restart. The user will not be authorized to reconnect.", response = StatusDTO.class)
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
	 * 
	 */
	@POST
	@Path("user/unban")
	@ApiOperation(value = "Unban a user.", response = StatusDTO.class)
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
	@ApiOperation(value = "List all banned users.", response = String.class, responseContainer = "List")
	public Response listBannedUsers() {
		List<String> banedUsers = stratumProxyManager.getBannedUsers();

		Response response = Response.status(Response.Status.OK).entity(banedUsers).build();

		return response;
	}

	/**
	 * Return the list of all worker connections
	 * 
	 * @return
	 * 
	 */
	@GET
	@Path("connection/list")
	@ApiOperation(value = "Return the list of all worker connections.", response = WorkerConnectionDTO.class, responseContainer = "List")
	@PubliclyAvailable
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
	 * 
	 * @inputType strat.mining.stratum.proxy.model.ConnectionIdentifierDTO
	 * @responseType strat.mining.stratum.proxy.model.StatusDTO
	 */
	@POST
	@Path("connection/kick")
	@ApiOperation(value = "Kill the connection with the given address and port.", response = StatusDTO.class)
	public Response kickConnection(ConnectionIdentifierDTO connection) {

		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.kickConnection(connection);
			status.setStatus(StatusDTO.DONE_STATUS);
		} catch (BadParameterException | NotFoundException e) {
			LOGGER.error("Failed to kick the connection with address {} and port {}.", connection.getAddress(), connection.getPort(), e);
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage("Failed to kick the connection with address " + connection.getAddress() + " and port " + connection.getPort() + ". " + e.getMessage());
		}

		Response response = Response.status(Response.Status.OK).entity(status).build();

		return response;
	}

	/**
	 * Ban the given ip address.
	 */
	@POST
	@Path("address/ban")
	@ApiOperation(value = "Ban the given ip address.", response = StatusDTO.class)
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
	@ApiOperation(value = "Unban the given ip address.", response = StatusDTO.class)
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
	@ApiOperation(value = "Kick all connections with the given address.", response = StatusDTO.class)
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
	@ApiOperation(value = "List all banned addresses.", response = String.class, responseContainer = "List")
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
	@ApiOperation(value = "Return the list of all pools.", response = PoolDetailsDTO.class, responseContainer = "List")
	@PubliclyAvailable
	public Response getPoolsList() {

		List<Pool> pools = stratumProxyManager.getPools();

		List<PoolDetailsDTO> result = new ArrayList<>();
		if (pools != null) {
			for (Pool pool : pools) {
				PoolDetailsDTO poolDTO = convertPoolToDTO(pool);
				result.add(poolDTO);
			}

			Collections.sort(result, new Comparator<PoolDetailsDTO>() {
				public int compare(PoolDetailsDTO o1, PoolDetailsDTO o2) {
					return o1.getPriority().compareTo(o2.getPriority());
				}
			});
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
	@ApiOperation(value = "Add a pool.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 500, message = "Failed to add the pool.", response = StatusDTO.class),
			@ApiResponse(code = 500, message = "Pool added but not started.", response = StatusDTO.class) })
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
	@ApiOperation(value = "Remove a pool.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 404, message = "Pool not found.", response = StatusDTO.class) })
	public Response removePool(RemovePoolDTO dto) {

		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.removePool(dto.getPoolName(), dto.getKeepHistory());

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
	@ApiOperation(value = "Disable a pool.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 500, message = "Failed to start the pool.", response = StatusDTO.class), @ApiResponse(code = 404, message = "Pool not found.", response = StatusDTO.class) })
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
	@ApiOperation(value = "Enable a pool.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 404, message = "Pool not found.", response = StatusDTO.class), @ApiResponse(code = 500, message = "Failed to start the pool.", response = StatusDTO.class) })
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
	@ApiOperation(value = "Change the priority of a pool.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 404, message = "Pool not found.", response = StatusDTO.class), @ApiResponse(code = 400, message = "Bad parameter sent.", response = StatusDTO.class) })
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

	/**
	 * Update the pool with the given name with the given details.
	 * 
	 * @param poolToUpdate
	 * @return
	 */
	@POST
	@Path("pool/update")
	@ApiOperation(value = "Update a pool.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 500, message = "Failed to update the pool.", response = StatusDTO.class), @ApiResponse(code = 400, message = "Bad parameter sent.", response = StatusDTO.class),
			@ApiResponse(code = 404, message = "Pool not found.", response = StatusDTO.class) })
	public Response updatePool(UpdatePoolDTO poolToUpdate) {
		Response response = null;
		StatusDTO status = new StatusDTO();

		try {
			stratumProxyManager.updatePool(poolToUpdate);

			status.setStatus(StatusDTO.DONE_STATUS);
			response = Response.status(Response.Status.OK).entity(status).build();
		} catch (BadParameterException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.BAD_REQUEST).entity(status).build();
		} catch (PoolStartException | SocketException | URISyntaxException e) {
			status.setStatus(StatusDTO.PARTIALLY_DONE_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(status).build();
		} catch (NotFoundException e) {
			status.setStatus(StatusDTO.FAILED_STATUS);
			status.setMessage(e.getMessage());
			response = Response.status(Response.Status.NOT_FOUND).entity(status).build();
		}

		return response;
	}

	@POST
	@Path("log/level")
	@ApiOperation(value = "Change the log level.", response = StatusDTO.class)
	@ApiResponses({ @ApiResponse(code = 400, message = "Log level not known.", response = StatusDTO.class) })
	public Response setLogLevel(LogLevelDTO logLevel) {
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

	@GET
	@Path("log/level")
	@ApiOperation(value = "Return the log level.", response = LogLevelDTO.class)
	public Response getLogLevel() {

		LogLevelDTO logLevel = new LogLevelDTO();
		Level level = LogManager.getRootLogger().getLevel();
		logLevel.setLogLevel(level.toString());

		return Response.status(Response.Status.OK).entity(logLevel).build();

	}

	@POST
	@Path("hashrate/user")
	@ApiOperation(value = "Return the hashrate history of a user.", response = HashrateModel.class, responseContainer = "List")
	@PubliclyAvailable
	public Response getUserHashrateHistory(UserNameDTO username,
			@ApiParam(required = false, name = "start", value = "Allow to filter returned values on the given start timestamp (UTC from Epoch in seconds)") @QueryParam("start") Long startTimestamp,
			@ApiParam(required = false, name = "end", value = "Allow to filter returned values on the given end timestamp (UTC from Epoch in seconds)") @QueryParam("end") Long endTimestamp) {
		HashrateHistoryDTO result = new HashrateHistoryDTO();
		List<HashrateModel> userHashrates = databaseManager.getUserHashrate(username.getUsername(), startTimestamp, endTimestamp);
		result.setName(username.getUsername());
		result.setHashrates(convertHashrateToDTO(userHashrates));

		Response response = Response.status(Response.Status.OK).entity(result).build();

		return response;
	}

	@POST
	@Path("hashrate/pool")
	@ApiOperation(value = "Return the hashrate history of a pool.", response = HashrateModel.class, responseContainer = "List")
	@ApiResponses({ @ApiResponse(code = 500, message = "Error during pool hashrate history response build."), @ApiResponse(code = 404, message = "Pool not found.") })
	@PubliclyAvailable
	public Response getPoolHashrateHistory(PoolNameDTO poolName,
			@ApiParam(required = false, name = "start", value = "Allow to filter returned values on the given start timestamp (UTC from Epoch in seconds)") @QueryParam("start") Long startTimestamp,
			@ApiParam(required = false, name = "end", value = "Allow to filter returned values on the given end timestamp (UTC from Epoch in seconds)") @QueryParam("end") Long endTimestamp) {
		HashrateHistoryDTO result = new HashrateHistoryDTO();
		Response response = null;
		Pool pool = stratumProxyManager.getPool(poolName.getPoolName());

		if (pool != null) {
			try {
				List<HashrateModel> poolHashrates = databaseManager.getPoolHashrate(pool.getHost(), startTimestamp, endTimestamp);
				result.setName(poolName.getPoolName());
				result.setHashrates(convertHashrateToDTO(poolHashrates));
				response = Response.status(Response.Status.OK).entity(result).build();
			} catch (Exception e) {
				LOGGER.error("Error during pool hashrate history response build.", e);
				response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
		} else {
			response = Response.status(Response.Status.NOT_FOUND).build();
		}

		return response;
	}

	@POST
	@Path("log/since")
	@ApiOperation(value = "Return log message since the given timestamp.", response = LogEntry.class, responseContainer = "List")
	@ApiResponses({ @ApiResponse(code = 400, message = "Timestamp is empty.") })
	public Response getLogSince(TimestampDTO timestamp) {
		Response response = null;
		if (timestamp != null) {
			List<Entry<Long, String>> logs = logManager.getLogSince(timestamp.getTimestamp());
			List<LogEntry> logEntries = new ArrayList<LogEntry>(logs.size());

			for (Entry<Long, String> logEntry : logs) {
				LogEntry logEntryDto = new LogEntry();
				logEntryDto.setTimestamp(logEntry.getKey());
				logEntryDto.setMessage(logEntry.getValue());
				logEntries.add(logEntryDto);
			}

			response = Response.status(Response.Status.OK).entity(logEntries).build();
		} else {
			response = Response.status(Response.Status.BAD_REQUEST).build();
		}

		return response;
	}

	@GET
	@Path("misc/version")
	@ApiOperation(value = "Return the version of the proxy.", response = ProxyVersionDTO.class)
	@PubliclyAvailable
	public Response getProxyVersion() {
		Response response = null;

		ProxyVersionDTO dto = new ProxyVersionDTO();
		dto.setProxyVersion(ConfigurationManager.getVersion());
		dto.setFullName(Constants.VERSION);
		response = Response.status(Response.Status.OK).entity(dto).build();

		return response;
	}

	/**
	 * Return a summary of the current state of the proxy
	 * 
	 * @return
	 */
	@GET
	@Path("summary")
	@ApiOperation(value = "Return a summary of the current state of the proxy.", response = SummaryDTO.class)
	@PubliclyAvailable
	public Response getSummary() {

		SummaryDTO summary = new SummaryDTO();
		// Look for the active pool.
		for (Pool pool : stratumProxyManager.getPools()) {
			if (pool.isActive()) {
				summary.setCurrentPoolName(pool.getName());
				summary.setHashrate(Double.valueOf(pool.getAcceptedHashesPerSeconds() + pool.getRejectedHashesPerSeconds()).longValue());
				summary.setAcceptedHashrate(Double.valueOf(pool.getAcceptedHashesPerSeconds()).longValue());
				summary.setRejectedHashrate(Double.valueOf(pool.getRejectedHashesPerSeconds()).longValue());
				summary.setTotalErrors(pool.getNumberOfDisconnections());
				summary.setPoolUptime(pool.getUptime());
				break;
			}
		}

		Response response = Response.status(Response.Status.OK).entity(summary).build();

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
		PoolDetailsDTO result = new PoolDetailsDTO();

		result.setDifficulty(pool.getDifficulty() != null ? pool.getDifficulty().toString() : null);
		result.setExtranonce1(pool.getExtranonce1());
		result.setExtranonce2Size(pool.getExtranonce2Size());
		result.setHost(pool.getHost());
		result.setIsReady(pool.isReady());
		result.setIsEnabled(pool.isEnabled());
		result.setIsStable(pool.isStable());
		result.setIsActive(pool.isActive());
		result.setName(pool.getName());
		result.setNumberOfWorkerConnections(pool.getNumberOfWorkersConnections());
		result.setPassword(pool.getPassword() == null ? "" : pool.getPassword());
		result.setUsername(pool.getUsername() == null ? "" : pool.getUsername());
		result.setWorkerExtranonce2Size(pool.getWorkerExtranonce2Size());
		result.setPriority(pool.getPriority());
		result.setWeight(pool.getWeight());
		result.setAcceptedDifficulty(pool.getAcceptedDifficulty());
		result.setIsReadySince(pool.getReadySince() != null ? pool.getReadySince().getTime() : null);
		result.setIsActiveSince(pool.getActiveSince() != null ? pool.getActiveSince().getTime() : null);
		result.setRejectedDifficulty(pool.getRejectedDifficulty());
		result.setIsExtranonceSubscribeEnabled(pool.isExtranonceSubscribeEnabled());
		result.setAcceptedHashesPerSeconds(Double.valueOf(pool.getAcceptedHashesPerSeconds()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(pool.getRejectedHashesPerSeconds()).longValue());
		result.setLastStopCause(pool.getLastStopCause());
		result.setLastStopDate(pool.getLastStopDate() != null ? pool.getLastStopDate().getTime() : null);
		result.setNumberOfDisconnections(pool.getNumberOfDisconnections());
		result.setUptime(pool.getUptime());
		result.setAppendWorkerNames(pool.isAppendWorkerNames());
		result.setWorkerNamesSeparator(pool.getWorkerSeparator() == null ? "" : pool.getWorkerSeparator());
		result.setUseWorkerPassword(pool.isUseWorkerPassword());
		result.setLastPoolMessage(pool.getLastPoolMessage());

		return result;
	}

	private WorkerConnectionDTO convertWorkerConnectionToDTO(WorkerConnection connection) {
		WorkerConnectionDTO result = new WorkerConnectionDTO();
		result.setRemoteHost(connection.getRemoteAddress().getHostAddress());
		result.setRemotePort(Integer.toString(connection.getRemotePort()));
		result.setAcceptedHashesPerSeconds(Double.valueOf(connection.getAcceptedHashrate()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(connection.getRejectedHashrate()).longValue());
		result.setPoolName(connection.getPool().getName());
		result.setAuthorizedUsers(new ArrayList<>(connection.getAuthorizedWorkers().keySet()));
		result.setIsActiveSince(connection.getActiveSince().getTime());
		result.setWorkerVersion(connection.getWorkerVersion());

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
		UserDetailsDTO result = new UserDetailsDTO();
		result.setName(user.getName());
		result.setFirstConnectionDate(user.getCreationTime().getTime());
		result.setLastShareSubmitted(user.getLastShareSubmitted() != null ? user.getLastShareSubmitted().getTime() : null);
		result.setAcceptedHashesPerSeconds(Double.valueOf(user.getAcceptedHashrate()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(user.getRejectedHashrate()).longValue());
		result.setAcceptedDifficulty(user.getAcceptedDifficulty());
		result.setRejectedDifficulty(user.getRejectedDifficulty());
		result.setAcceptedShareNumber(user.getAcceptedShareNumber());
		result.setRejectedShareNumber(user.getRejectedShareNumber());

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
