package strat.mining.stratum.proxy.rest.authentication;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;

public class AuthenticationAddOn implements AddOn {

	@Override
	public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
		// Get the index of HttpCodecFilter in the HttpServerFilter filter chain
		final int httpCodecFilterIdx = builder.indexOfType(HttpServerFilter.class);

		if (httpCodecFilterIdx >= 0) {
			// Insert the AuthenticationFilter right after HttpServerFilter
			builder.add(httpCodecFilterIdx + 1, new AuthenticationFilter());
		}
	}

}
