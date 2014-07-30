package strat.mining.stratum.proxy.rest.ssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Grizzly filter which redirect HTTP requests to HTTPS if SSL is enabled.
 * 
 * @author VMDev
 *
 */
public class SSLRedirectFilter extends SSLBaseFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(SSLRedirectFilter.class);

	public SSLRedirectFilter() {
		super();
	}

	public SSLRedirectFilter(SSLEngineConfigurator serverSSLEngineConfigurator, boolean renegotiateOnClientAuthWant) {
		super(serverSSLEngineConfigurator, renegotiateOnClientAuthWant);
	}

	public SSLRedirectFilter(SSLEngineConfigurator serverSSLEngineConfigurator) {
		super(serverSSLEngineConfigurator);
	}

	@Override
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		Buffer buffer = (Buffer) ctx.getMessage();

		// Try to parse the buffer as string. If it fails,it is not http.
		try {
			String request = buffer.toStringContent();

			// Just test GET and POST since the proxy services does not use
			// anything else.
			if (request.startsWith("GET") || request.startsWith("POST")) {
				// If it is HTTP,
				return redirectToHttps(ctx, request);
			}
		} catch (Exception e) {
			// Do nothing. If the parsing failes, it is not an http request. So
			// just continue.
		}

		// If we are here, it is not an http request. So continue the
		// filterchain execution.
		return super.handleRead(ctx);

	}

	@Override
	public TransportFilter createOptimizedTransportFilter(final TransportFilter childFilter) {
		return childFilter;
	}

	/**
	 * Modify the filterChain context to reply a redirect to HTTPS.
	 * 
	 * @param rawRequest
	 * @return
	 */
	private NextAction redirectToHttps(FilterChainContext ctx, String rawRequest) throws IOException {
		HttpRequest request = new HttpRequest(rawRequest);
		String location = buildLocationFromRequest(ctx, request);

		StringBuilder builder = new StringBuilder("HTTP/1.1 301 Moved Permanently\r\nLocation: ");
		builder.append(location);
		builder.append("\r\nCache-Control: no-cache\r\n");
		builder.append("\r\n");

		byte[] rawByteBuffer = builder.toString().getBytes();
		Buffer buffer = new ByteBufferWrapper(ByteBuffer.wrap(rawByteBuffer));
		ctx.write(buffer, true);
		ctx.flush(null);
		ctx.getConnection().closeSilently();

		return ctx.getForkAction();
	}

	/**
	 * Build the redirection location based on the http request
	 * 
	 * @param rawRequest
	 * @return
	 */
	private String buildLocationFromRequest(FilterChainContext ctx, HttpRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("https://");

		String host = request.getHeaders().get("Host");
		result.append(host);

		InetSocketAddress localAddress = (InetSocketAddress) ctx.getConnection().getLocalAddress();
		result.append(":").append(localAddress.getPort());

		result.append(request.getResourcePath());

		return result.toString();
	}

	/**
	 * Contains all the details of an HTTP request
	 * 
	 * @author Stratehm
	 *
	 */
	public class HttpRequest {

		private String method;
		private String resourcePath;
		private String protocol;
		private Map<String, String> headers;

		public HttpRequest(String rawRequest) throws IOException {
			headers = new HashMap<String, String>();
			BufferedReader requestReader = new BufferedReader(new StringReader(rawRequest));
			String line = requestReader.readLine();
			String[] splittedMethod = line.split(" ");
			if (splittedMethod.length != 3) {
				throw new IOException("Failed to parse the HTTP request " + rawRequest + ". METHOD line has only " + splittedMethod.length
						+ " members.");
			}
			method = splittedMethod[0].trim();
			resourcePath = URLDecoder.decode(splittedMethod[1].trim(), "UTF-8");
			protocol = splittedMethod[2].trim();

			line = requestReader.readLine();
			while (line != null) {
				String[] splittedLine = line.split(":");
				if (splittedLine.length >= 2) {
					String headerName = splittedLine[0].trim();
					String headerValue = splittedLine[1].trim();
					headers.put(headerName, headerValue);
				}
				line = requestReader.readLine();
			}
		}

		public String getMethod() {
			return method;
		}

		public String getResourcePath() {
			return resourcePath;
		}

		public String getProtocol() {
			return protocol;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

	}

}
