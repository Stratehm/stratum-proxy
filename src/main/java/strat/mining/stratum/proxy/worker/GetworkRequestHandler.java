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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.callback.ConnectionClosedCallback;
import strat.mining.stratum.proxy.callback.LongPollingCallback;
import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.AuthorizationException;
import strat.mining.stratum.proxy.exception.ChangeExtranonceNotSupportedException;
import strat.mining.stratum.proxy.exception.NoCredentialsException;
import strat.mining.stratum.proxy.exception.NoPoolAvailableException;
import strat.mining.stratum.proxy.exception.TooManyWorkersException;
import strat.mining.stratum.proxy.json.GetworkRequest;
import strat.mining.stratum.proxy.json.GetworkResponse;
import strat.mining.stratum.proxy.json.JsonRpcError;
import strat.mining.stratum.proxy.json.JsonRpcResponse;
import strat.mining.stratum.proxy.json.MiningAuthorizeRequest;
import strat.mining.stratum.proxy.json.MiningSubmitResponse;
import strat.mining.stratum.proxy.json.MiningSubscribeRequest;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.HttpUtils;
import strat.mining.stratum.proxy.utils.mining.SHA256HashingUtils;
import strat.mining.stratum.proxy.utils.mining.ScryptHashingUtils;
import strat.mining.stratum.proxy.worker.GetworkJobTemplate.GetworkRequestResult;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetworkRequestHandler extends HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetworkRequestHandler.class);

	private ProxyManager manager;

	private static ObjectMapper jsonUnmarshaller = new ObjectMapper();;

	private Map<InetAddress, GetworkWorkerConnection> workerConnections;

	public GetworkRequestHandler() {
		this.manager = ProxyManager.getInstance();
		this.workerConnections = Collections.synchronizedMap(new HashMap<InetAddress, GetworkWorkerConnection>());

	}

	@Override
	public void service(final Request request, final Response response) throws Exception {
		response.setHeader("X-Mining-Extensions", "longpoll");
		response.setHeader("X-Roll-Ntime", "1");
		response.setHeader("Content-Type", "application/json");

		String content = null;
		Object jsonRpcRequestId = 0L;
		try {
			content = new BufferedReader(request.getReader()).readLine();
			LOGGER.trace("New request from {}: {}", request.getRemoteAddr(), content);

			setRequestCredentials(request);

			final GetworkWorkerConnection workerConnection = getWorkerConnection(request);
			final GetworkRequest getworkRequest = jsonUnmarshaller.readValue(content, GetworkRequest.class);
			jsonRpcRequestId = getworkRequest.getId();

			if (!request.getRequestURI().equalsIgnoreCase(Constants.DEFAULT_GETWORK_LONG_POLLING_URL)) {
				// Basic getwork Request
				response.setHeader("X-Long-Polling", Constants.DEFAULT_GETWORK_LONG_POLLING_URL);

				if (getworkRequest.getData() != null) {
					// If data are presents, it is a submit request
					LOGGER.debug("New getwork submit request from user {}@{}.", request.getAttribute("username"),
							workerConnection.getConnectionName());
					processGetworkSubmit(request, response, workerConnection, getworkRequest);
				} else {
					// Else it is a getwork request
					LOGGER.debug("New getwork request from user {}@{}.", request.getAttribute("username"), workerConnection.getConnectionName());
					processGetworkRequest(request, response, workerConnection, getworkRequest);
				}

			} else {
				// Long polling getwork request
				LOGGER.debug("New getwork long-polling request from user {}@{}.", request.getAttribute("username"),
						workerConnection.getConnectionName());
				processLongPollingRequest(request, response, workerConnection, getworkRequest);
			}

		} catch (NoCredentialsException e) {
			LOGGER.warn("Request from {} without credentials. Returning 401 Unauthorized.", request.getRemoteAddr());
			response.setHeader(Header.WWWAuthenticate, "Basic realm=\"stratum-proxy\"");
			response.setStatus(HttpStatus.UNAUTHORIZED_401);
			setResponseError(jsonRpcRequestId, response, "Credentials needed.");
		} catch (AuthorizationException e) {
			LOGGER.warn("Authorization failed for getwork request from {}. {}", request.getRemoteAddr(), e.getMessage());
			response.setStatus(HttpStatus.FORBIDDEN_403);
			setResponseError(jsonRpcRequestId, response, "Bad user/password.");
		} catch (JsonParseException | JsonMappingException e) {
			LOGGER.error("Unsupported request content from {}: {}", request.getRemoteAddr(), content, e);
			response.setStatus(HttpStatus.BAD_REQUEST_400);
			setResponseError(jsonRpcRequestId, response, "Request parsing failed.");
		} catch (NoPoolAvailableException e) {
			LOGGER.warn("Getwork request rejected from {}: No pool available.", request.getRemoteAddr());
			response.setStatus(HttpStatus.NO_CONTENT_204);
			setResponseError(jsonRpcRequestId, response, "No pool available.");
		}

	}

	/**
	 * Process the request as a long-polling one.
	 * 
	 * @param request
	 * @param response
	 * @param workerConnection
	 * @param getworkRequest
	 */
	private void processLongPollingRequest(final Request request, final Response response, final GetworkWorkerConnection workerConnection,
			final GetworkRequest getworkRequest) {
		// Prepare the callback of long polling
		final LongPollingCallback longPollingCallback = new LongPollingCallback() {
			public void onLongPollingResume() {
				try {
					// Once the worker connection call the callback, process the
					// getwork request to fill the response.
					processGetworkRequest(request, response, workerConnection, getworkRequest);

					// Then resume the response to send it to the miner.
					response.resume();
				} catch (Exception e) {
					LOGGER.error("Failed to send long-polling response to {}@{}.", request.getAttribute("username"),
							workerConnection.getConnectionName(), e);
				}
			}

			public void onLongPollingCancel(String causeMessage) {
				response.setStatus(HttpStatus.NO_CONTENT_204);
				setResponseError(getworkRequest.getId(), response, causeMessage);
				response.resume();
			}
		};

		// Suspend the response for at least 70 seconds (miners should cancel
		// the request after 60 seconds)
		// If the request is cancelled or failed, remove hte callback from the
		// worker connection since there is no need to call it.
		response.suspend(70, TimeUnit.SECONDS, new CompletionHandler<Response>() {
			public void updated(Response result) {
			}

			public void failed(Throwable throwable) {
				LOGGER.error("Long-polling request of {}@{} failed. Cause: {}", request.getAttribute("username"),
						workerConnection.getConnectionName(), throwable.getMessage());
				workerConnection.removeLongPollingCallback(longPollingCallback);
			}

			public void completed(Response result) {
			}

			public void cancelled() {
				LOGGER.error("Long-polling request of {}@{} cancelled.", request.getAttribute("username"), workerConnection.getConnectionName());
				workerConnection.removeLongPollingCallback(longPollingCallback);
			}
		});

		// Add the callback to the worker connection
		workerConnection.addLongPollingCallback(longPollingCallback);
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
	protected void processGetworkRequest(Request request, Response response, GetworkWorkerConnection workerConnection, GetworkRequest getworkRequest)
			throws JsonProcessingException, IOException {

		GetworkRequestResult requestResult = workerConnection.getGetworkData();
		// Return the getwork data
		GetworkResponse jsonResponse = new GetworkResponse();
		jsonResponse.setId(getworkRequest.getId());
		jsonResponse.setData(requestResult.getData());
		jsonResponse.setTarget(requestResult.getTarget());
		jsonResponse.setHash1(requestResult.getHash1());
		jsonResponse.setMidstate(requestResult.getMidstate());

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
		MiningSubmitResponse jsonResponse = new MiningSubmitResponse();
		jsonResponse.setId(getworkRequest.getId());

		// If the share check is enabled, check if the SHA256 share is
		// below the target. If so, submit the share. Else do not submit.
		String errorMessage = null;

		// Validate the share if the option is set.
		boolean isShareValid = true;
		if (ConfigurationManager.getInstance().isValidateGetworkShares()) {
			if (ConfigurationManager.getInstance().isScrypt()) {
				isShareValid = !ScryptHashingUtils.isBlockHeaderScryptHashBelowTarget(getworkRequest.getData(), workerConnection.getGetworkTarget());
			} else {
				isShareValid = !SHA256HashingUtils.isBlockHeaderSHA256HashBelowTarget(getworkRequest.getData(), workerConnection.getGetworkTarget());
			}
		}

		if (!isShareValid) {
			errorMessage = "Share is above the target (proxy check)";
			LOGGER.debug("Share submitted by {}@{} is above the target. The share is not submitted to the pool.",
					(String) request.getAttribute("username"), request.getRemoteAddr());
		} else {
			// Submit only if the share is not above the target
			errorMessage = workerConnection.submitWork((String) request.getAttribute("username"), getworkRequest.getData());
		}

		// If there is an error message, the share submit has
		// failed/been rejected
		if (errorMessage != null) {
			response.setHeader("X-Reject-Reason", errorMessage);
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
	private synchronized GetworkWorkerConnection getWorkerConnection(Request request) throws UnknownHostException, NoPoolAvailableException,
			TooManyWorkersException, ChangeExtranonceNotSupportedException, NoCredentialsException, AuthorizationException {
		final InetAddress address = InetAddress.getByName(request.getRemoteAddr());

		GetworkWorkerConnection workerConnection = workerConnections.get(address);
		// If the worker connection is null, try to create it.
		if (workerConnection == null) {
			LOGGER.debug("No existing getwork connections for address {}. Create it.", request.getRemoteAddr());
			workerConnection = new GetworkWorkerConnection(address, manager, new ConnectionClosedCallback() {
				public void onConnectionClosed(WorkerConnection connection) {
					// When the connection is closed, remove it from the
					// connection list.
					workerConnections.remove(address);
					LOGGER.debug("Getwork connection {} removed from Getwork handler.", connection.getConnectionName());
				}
			});

			MiningSubscribeRequest subscribeRequest = new MiningSubscribeRequest();
			Pool pool = manager.onSubscribeRequest(workerConnection, subscribeRequest);
			workerConnection.rebindToPool(pool);

			workerConnections.put(address, workerConnection);
		}

		try {
			checkAuthorization(workerConnection, request);
			workerConnection.addAuthorizedUsername((String) request.getAttribute("username"), (String) request.getAttribute("password"));
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
		Pair<String, String> credentials = HttpUtils.getCredentials(request);
		request.setAttribute("username", credentials.getFirst());
		request.setAttribute("password", credentials.getSecond());
	}

	/**
	 * Set the error object for the JSON rpc response.
	 */
	private void setResponseError(Object jsonRpcRequestId, Response response, String errorMessage) {
		JsonRpcResponse jsonResponse = new JsonRpcResponse();
		jsonResponse.setId(jsonRpcRequestId);
		jsonResponse.setResult(null);

		JsonRpcError error = new JsonRpcError();
		error.setCode(0);
		error.setMessage(errorMessage);
		error.setTraceback(null);
		jsonResponse.setErrorRpc(error);

		try {
			String result = jsonUnmarshaller.writeValueAsString(jsonResponse);
			response.getOutputBuffer().write(result);
		} catch (Exception e) {
			LOGGER.error("Failed to send an error response to {}.", response.getRequest().getRemoteAddr(), e);
		}
	}

}
