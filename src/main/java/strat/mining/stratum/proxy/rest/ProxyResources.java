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

import strat.mining.stratum.proxy.Launcher;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.model.User;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.dto.ChangePriorityDTO;
import strat.mining.stratum.proxy.rest.dto.LogLevelDTO;
import strat.mining.stratum.proxy.rest.dto.PoolDetailsDTO;
import strat.mining.stratum.proxy.rest.dto.PoolNameDTO;
import strat.mining.stratum.proxy.rest.dto.UserDetailsDTO;
import strat.mining.stratum.proxy.rest.dto.WorkerConnectionDTO;
import strat.mining.stratum.proxy.worker.WorkerConnection;

@Path("proxy")
@Produces("application/json")
@Consumes("application/json")
public class ProxyResources {

	private static final String API_DATE_FORMAT = "dd-MM-yy HH:mm:ss Z";

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResources.class);

	private StratumProxyManager stratumProxyManager = Launcher.getStratumProxyManager();

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
	 * Return the details of the user with the given name.
	 * 
	 * @return
	 */
	@GET
	@Path("user/details/{userName}")
	public Response getUserDetails() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

	/**
	 * Kick the given username
	 * 
	 * @return
	 */
	@POST
	@Path("user/kick/")
	public Response kickUser() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

	/**
	 * Ban the given username
	 * 
	 * @return
	 */
	@POST
	@Path("user/ban/")
	public Response banUser() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

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
	 * Kick the connection with the given address and port.
	 * 
	 * @return
	 */
	@POST
	@Path("connection/kick")
	public Response kickConnection() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

	/**
	 * Ban the given address
	 */
	@POST
	@Path("ip/ban")
	public Response banConnection() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

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
	public Response addPool() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

	/**
	 * Remove the pool with the given name
	 * 
	 * @return
	 */
	@POST
	@Path("pool/remove")
	public Response removePool() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

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

		try {
			stratumProxyManager.setPoolEnabled(poolName.getPoolName(), false);

			response = Response.status(Response.Status.OK).entity("Done").build();
		} catch (NoPoolAvailableException e) {
			response = Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (Exception e) {
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to start the pool. " + e.getMessage()).build();
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

		try {
			stratumProxyManager.setPoolEnabled(poolName.getPoolName(), true);

			response = Response.status(Response.Status.OK).entity("Done").build();
		} catch (NoPoolAvailableException e) {
			response = Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (Exception e) {
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to start the pool. " + e.getMessage()).build();
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

		try {
			stratumProxyManager.setPoolPriority(parameters.getPoolName(), parameters.getPriority());
			response = Response.status(Response.Status.OK).entity("Done").build();
		} catch (NoPoolAvailableException e) {
			response = Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (BadParameterException e) {
			response = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}

		return response;
	}

	@POST
	@Path("log/level")
	public Response changeLogLevel(LogLevelDTO logLevel) {
		Response response = null;

		try {
			Level newLevel = getLogLevel(logLevel.getLogLevel());
			LogManager.getRootLogger().setLevel(newLevel);
			LOGGER.info("Changing logLevel to {}", logLevel.getLogLevel());
			response = Response.status(Response.Status.OK).entity("Done").build();
		} catch (BadParameterException e) {
			response = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
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

		result.setDifficulty(pool.getDifficulty().toString());
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
		result.setIsActiveSince(simpleDateFormat.format(pool.getActiveSince()));
		result.setRejectedDifficulty(pool.getRejectedDifficulty());
		result.setIsExtranonceSubscribeEnabled(pool.isExtranonceSubscribeEnabled());
		result.setAcceptedHashesPerSeconds(Double.valueOf(pool.getAcceptedHashesPerSeconds()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(pool.getRejectedHashesPerSeconds()).longValue());

		return result;
	}

	private WorkerConnectionDTO convertWorkerConnectionToDTO(WorkerConnection connection) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(API_DATE_FORMAT);
		WorkerConnectionDTO result = new WorkerConnectionDTO();
		result.setAuthorizedUsers(new ArrayList<>(connection.getAuthorizedWorkers()));
		result.setIsActiveSince(simpleDateFormat.format(connection.getActiveSince()));
		result.setRemoteHost(connection.getConnectionName());
		result.setAcceptedHashesPerSeconds(Double.valueOf(connection.getAcceptedHashrate()).longValue());
		result.setRejectedHashesPerSeconds(Double.valueOf(connection.getRejectedHashrate()).longValue());

		return result;
	}

	private UserDetailsDTO convertUserToDTO(User user) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(API_DATE_FORMAT);
		UserDetailsDTO result = new UserDetailsDTO();
		result.setName(user.getName());
		result.setCreationDate(simpleDateFormat.format(user.getCreationTime()));
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

}
