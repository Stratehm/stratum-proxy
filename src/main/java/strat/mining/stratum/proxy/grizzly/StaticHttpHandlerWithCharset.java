package strat.mining.stratum.proxy.grizzly;

import java.util.Set;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This HttpHandler set the charset option of the content type header of
 * responses (if the response content is text).
 *
 */
public class StaticHttpHandlerWithCharset extends StaticHttpHandler {

	private final Logger LOGGER = LoggerFactory.getLogger(StaticHttpHandler.class);

	public StaticHttpHandlerWithCharset() {
		super();
	}

	public StaticHttpHandlerWithCharset(Set<String> docRoots) {
		super(docRoots);
	}

	public StaticHttpHandlerWithCharset(String... docRoots) {
		super(docRoots);
	}

	@Override
	protected boolean handle(String uri, Request request, Response response) throws Exception {
		LOGGER.trace("Requested resource: {}.", uri);

		// Set the content type before calling the
		// super.handle method to set the character
		// encoding. Since the content type will already be
		// set, the super.handle method will not override
		// this content type.
		setContentType(response, uri);

		long time = System.currentTimeMillis();
		boolean found = super.handle(uri, request, response);

		time = System.currentTimeMillis() - time;
		if (found) {
			LOGGER.trace("Resource sent in {} ms: {}.", time, uri);
		} else {
			LOGGER.trace("Resource not found: {}.", uri);
		}
		return found;
	}

	/**
	 * Set the content type (with charset) in the given response header. The
	 * content type is guessed from the requested file extension.
	 * 
	 * @param response
	 * @param resourcePath
	 */
	private void setContentType(Response response, String resourcePath) {
		pickupContentType(response, resourcePath);
		if (response.getContentType() != null && response.getContentType().startsWith("text/")) {
			response.setCharacterEncoding("UTF-8");
		}
	}

}
