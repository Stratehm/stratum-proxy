package strat.mining.stratum.proxy.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.Launcher;
import strat.mining.stratum.proxy.manager.StratumProxyManager;

@Path("proxy")
@Produces("application/json")
@Consumes("application/json")
public class ProxyResources {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResources.class);

	private StratumProxyManager stratumProxyManager = Launcher.getStratumProxyManager();

	/**
	 * Get the list of connected users
	 * 
	 * @return
	 */
	@GET
	@Path("user/list")
	public Response getUsersList(@HeaderParam("user-agent") String userAgent, @PathParam("exchangePlace") String exchangePlace,
			@PathParam("currencyCode") String currencyCode) {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

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

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

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

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

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
	@Path("pool/stop")
	public Response stopPool() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

	/**
	 * Start the given pool
	 * 
	 * @return
	 */
	@POST
	@Path("pool/start")
	public Response startPool() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

	/**
	 * The priority of the given pool.
	 * 
	 * @return
	 */
	@POST
	@Path("pool/priority")
	public Response setPoolPriority() {

		Response response = null;

		response = Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();

		return response;
	}

}
