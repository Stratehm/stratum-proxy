package strat.mining.stratum.proxy.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoCredentialsException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.GetworkRequest;
import strat.mining.stratum.proxy.json.GetworkResponse;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.pool.Pool;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetworkRequestHandler extends HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetworkRequestHandler.class);

	private StratumProxyManager manager;

	private ObjectMapper jsonUnmarshaller;

	private Map<InetAddress, GetworkWorkerConnection> workerConnections;

	public GetworkRequestHandler(StratumProxyManager manager) {
		this.manager = manager;
		this.jsonUnmarshaller = new ObjectMapper();
		this.workerConnections = Collections.synchronizedMap(new HashMap<InetAddress, GetworkWorkerConnection>());

	}

	@Override
	public void service(Request request, Response response) throws Exception {
		// response.setHeader("X-Mining-Extensions", "longpoll");

		String content = null;
		try {
			content = new BufferedReader(request.getReader()).readLine();
			LOGGER.trace("New request from {}: {}", request.getRemoteAddr(), content);

			setRequestCredentials(request);

			GetworkWorkerConnection workerConnection = getWorkerConnection(request);

			if (!request.getRequestURI().equalsIgnoreCase(Constants.DEFAULT_GETWORK_LONG_POLLING_URL)) {
				response.setHeader("X-Long-Polling", Constants.DEFAULT_GETWORK_LONG_POLLING_URL);
				// Basic getwork request
				GetworkRequest getworkRequest = jsonUnmarshaller.readValue(content, GetworkRequest.class);
				// If data are presents, it is a submit request
				if (getworkRequest.getData() != null) {
					processGetworkSubmit(request, response, workerConnection, getworkRequest);
				} else {
					// Else it is a getwork request
					processBasicGetworkRequest(request, response, workerConnection, getworkRequest);
				}

			} else {
				LOGGER.debug("New getwork long-polling request from user {}@{}.", request.getAttribute("username"),
						workerConnection.getConnectionName());
				// TODO Long polling
				// Block on the workerConnection getLongPollData.
			}

		} catch (NoCredentialsException e) {
			LOGGER.warn("Request from {} without credentials. Returning 401 Unauthorized.", request.getRemoteAddr());
			response.setHeader(Header.WWWAuthenticate, "Basic realm=\"stratum-proxy\"");
			response.setStatus(HttpStatus.UNAUTHORIZED_401);
		} catch (AuthorizationException e) {
			LOGGER.warn("Authorization failed for getwork request from {}. {}", request.getRemoteAddr(), e.getMessage());
		} catch (JsonParseException | JsonMappingException e) {
			LOGGER.error("Unsupported request content from {}: {}", request.getRemoteAddr(), content, e);
		}

	}

	/**
	 * Process a basic (non long-polling) getwork request.
	 * 
	 * @param request
	 * @param response
	 * @param workerConnection
	 * @param getworkRequest
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	protected void processBasicGetworkRequest(Request request, Response response, GetworkWorkerConnection workerConnection,
			GetworkRequest getworkRequest) throws JsonProcessingException, IOException {
		LOGGER.debug("New getwork request from user {}@{}.", request.getAttribute("username"), workerConnection.getConnectionName());

		// Return the getwork data
		GetworkResponse jsonResponse = new GetworkResponse();
		jsonResponse.setId(getworkRequest.getId());
		jsonResponse.setData(workerConnection.getGetworkData());
		jsonResponse.setTarget(workerConnection.getGetworkTarget());

		String result = jsonUnmarshaller.writeValueAsString(jsonResponse);
		LOGGER.debug("Returning response to {}@{}: {}", request.getAttribute("username"), request.getRemoteAddr(), result);
		response.getOutputBuffer().write(result);
	}

	/**
	 * Process a getwork share submission.
	 * 
	 * @param request
	 * @param response
	 * @param workerConnection
	 * @param getworkRequest
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	protected void processGetworkSubmit(Request request, Response response, GetworkWorkerConnection workerConnection, GetworkRequest getworkRequest)
			throws JsonProcessingException, IOException {
		LOGGER.debug("New getwork submit request from user {}@{}.", request.getAttribute("username"), workerConnection.getConnectionName());

		MiningSubmitResponse jsonResponse = new MiningSubmitResponse();
		jsonResponse.setId(getworkRequest.getId());

		String errorMesage = workerConnection.submitWork((String) request.getAttribute("username"), getworkRequest.getData());
		// If there is an error message, the share submit has
		// failed/been rejected
		if (errorMesage != null) {
			response.setHeader("X-Reject-Reason", errorMesage);
			jsonResponse.setIsAccepted(false);
		} else {
			jsonResponse.setIsAccepted(true);
		}

		String result = jsonUnmarshaller.writeValueAsString(jsonResponse);
		LOGGER.debug("Returning response to {}@{}: {}", request.getAttribute("username"), request.getRemoteAddr(), result);
		response.getOutputBuffer().write(result);
	}

	/**
	 * Check if the request is authorized. If not authorized, throw an
	 * exception. The response status code and headers are modified.
	 * 
	 * @param request
	 * @throws AuthorizationException
	 * @throws NoCredentialsException
	 */
	private void checkAuthorization(GetworkWorkerConnection connection, Request request) throws AuthorizationException, NoCredentialsException {
		MiningAuthorizeRequest authorizeRequest = new MiningAuthorizeRequest();
		authorizeRequest.setUsername((String) request.getAttribute("username"));
		authorizeRequest.setPassword((String) request.getAttribute("password"));
		manager.onAuthorizeRequest(connection, authorizeRequest);
	}

	/**
	 * Return the worker connection of the request.
	 * 
	 * @param request
	 * @return
	 * @throws ChangeExtranonceNotSupportedException
	 * @throws TooManyWorkersException
	 * @throws NoCredentialsException
	 * @throws AuthorizationException
	 */
	private GetworkWorkerConnection getWorkerConnection(Request request) throws UnknownHostException, NoPoolAvailableException,
			TooManyWorkersException, ChangeExtranonceNotSupportedException, NoCredentialsException, AuthorizationException {
		InetAddress address = InetAddress.getByName(request.getRemoteAddr());

		GetworkWorkerConnection workerConnection = workerConnections.get(address);
		if (workerConnection == null) {
			LOGGER.debug("No existing getwork connections for address {}.", request.getRemoteAddr());
			workerConnection = new GetworkWorkerConnection(address, manager);

			MiningSubscribeRequest subscribeRequest = new MiningSubscribeRequest();
			Pool pool = manager.onSubscribeRequest(workerConnection, subscribeRequest);
			workerConnection.rebindToPool(pool);

			workerConnections.put(address, workerConnection);
		}

		try {
			checkAuthorization(workerConnection, request);
			workerConnection.addAuthorizedUsername((String) request.getAttribute("username"));
		} catch (AuthorizationException e) {
			workerConnections.remove(address);
			manager.onWorkerDisconnection(workerConnection, e);
			throw e;
		}

		return workerConnection;
	}

	/**
	 * Set the username/password of the request in the request attributes if
	 * they exist. Else, throw an exception.
	 * 
	 * @param request
	 * @return
	 */
	private void setRequestCredentials(Request request) throws NoCredentialsException {
		String authorization = request.getAuthorization();
		if (authorization != null && authorization.startsWith("Basic")) {
			// Authorization: Basic base64credentials
			String base64Credentials = authorization.substring("Basic".length()).trim();
			String credentialsString = new String(Base64.decode(base64Credentials.getBytes()), Charset.forName("UTF-8"));
			// credentials = username:password
			String[] values = credentialsString.split(":", 2);

			request.setAttribute("username", values[0]);
			request.setAttribute("password", values[1]);

		} else {
			throw new NoCredentialsException();
		}
	}
}
