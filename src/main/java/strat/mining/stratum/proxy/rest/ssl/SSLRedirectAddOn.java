package strat.mining.stratum.proxy.rest.ssl;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * An add on to grizzly to register the SSLRedirect filter
 * 
 * @author VMDev
 *
 */
public class SSLRedirectAddOn implements AddOn {

	@Override
	public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
		final int transportFilterIdx = builder.indexOfType(TransportFilter.class);

		if (transportFilterIdx >= 0) {
			builder.set(transportFilterIdx + 1, new SSLRedirectFilter());
		}

	}
}
