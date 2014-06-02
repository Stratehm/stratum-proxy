package strat.mining.stratum.proxy.worker;

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
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.pool.Pool;

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

		try {
			setRequestCredentials(request);

			GetworkWorkerConnection workerConnection = getWorkerConnection(request);
			response.setHeader("X-Long-Polling", Constants.DEFAULT_LONG_POLLING_URL);

			if (request.getRequestURI().equalsIgnoreCase(Constants.DEFAULT_GETWORK_URL)) {
				// TODO getwork
				GetworkRequest getworkRequest = jsonUnmarshaller.readValue(request.getInputStream(), GetworkRequest.class);

				System.out.println(getworkRequest);
			} else {
				// TODO Long polling
			}

		} catch (NoCredentialsException e) {
			LOGGER.warn("Request form {} without credentials. Returning 401 Unauthorized.", request.getRemoteAddr());
			response.setHeader(Header.WWWAuthenticate, "Basic realm=\"stratum-proxy\"");
			response.setStatus(HttpStatus.UNAUTHORIZED_401);
		} catch (AuthorizationException e) {
			LOGGER.warn("Authorization failed for getwork request from {}. {}", request.getRemoteAddr(), e.getMessage());
		}

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
			workerConnection = new GetworkWorkerConnection(address);

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
