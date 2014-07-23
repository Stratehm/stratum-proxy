package strat.mining.stratum.proxy.utils;

import java.nio.charset.Charset;

import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.utils.Pair;
import org.glassfish.jersey.internal.util.Base64;

import strat.mining.stratum.proxy.exception.NoCredentialsException;

public final class HttpUtils {

	/**
	 * Return the credentials of the request.
	 * 
	 * @param request
	 * @return
	 * @throws NoCredentialsException
	 */
	public static Pair<String, String> getCredentials(Request request) throws NoCredentialsException {
		String authorization = request.getAuthorization();
		return getCredentials(authorization);
	}

	/**
	 * Return the credentials of the given header.
	 * 
	 * @param header
	 * @return
	 * @throws NoCredentialsException
	 */
	public static Pair<String, String> getCredentials(HttpHeader header) throws NoCredentialsException {
		String authorization = header.getHeader("Authorization");
		return getCredentials(authorization);
	}

	/**
	 * Return the credentials based on the header value.
	 * 
	 * @param authorizationHeaderValue
	 * @return
	 * @throws NoCredentialsException
	 */
	private static Pair<String, String> getCredentials(String authorizationHeaderValue) throws NoCredentialsException {
		Pair<String, String> credentials = new Pair<String, String>();
		if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Basic")) {
			// Authorization: Basic base64credentials
			String base64Credentials = authorizationHeaderValue.substring("Basic".length()).trim();
			String credentialsString = new String(Base64.decode(base64Credentials.getBytes()), Charset.forName("UTF-8"));
			// credentials = username:password
			String[] values = credentialsString.split(":", 2);

			credentials.setFirst(values[0]);
			credentials.setSecond(values[1]);
		} else {
			throw new NoCredentialsException();
		}
		return credentials;
	}

}
