package strat.mining.stratum.proxy.rest.authentication;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;

public class AuthenticationAddOn implements AddOn {

	@Override
	public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
		// Get the index of HttpServerFilter in the HttpServerFilter filter
		// chain
		final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

		if (httpServerFilterIdx >= 0) {
			// Insert the AuthenticationFilter right after HttpServerFilter
			builder.add(httpServerFilterIdx + 1, new AuthenticationFilter());
		}
	}

}
