/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
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
package strat.mining.stratum.proxy;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.database.DatabaseManager;
import strat.mining.stratum.proxy.manager.HashrateRecorder;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.ProxyResources;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.worker.GetworkRequestHandler;

public class Launcher {

	public static Logger LOGGER = null;

	public static final String THREAD_MONITOR = "";

	private static HttpServer apiHttpServer;

	private static HttpServer getWorkHttpServer;

	public static void main(String[] args) {

		initShutdownHook();

		try {
			ConfigurationManager configurationManager = ConfigurationManager.getInstance();
			configurationManager.loadConfiguration(args);
			LOGGER = LoggerFactory.getLogger(Launcher.class);

			// Start initialization of the database manager
			initDatabaseManager();

			// Initialize the proxy manager
			initProxyManager(configurationManager);

			// Initialize the Getwork system
			initGetwork(configurationManager);

			// Initialize the rest services
			initRestServices(configurationManager);

			// Initialize the hashrate recorder
			initHashrateRecorder();

			// Wait the end of the program
			waitInfinite();

		} catch (Exception e) {
			if (LOGGER != null) {
				LOGGER.error("Failed to start the proxy.", e);
			} else {
				System.out.println("Failed to start the proxy: ");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initialize the shutdown hook to close gracefully all connections.
	 */
	private static void initShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {

				// Start a timer task that will exit the program after 1 second
				// if the cleanup is not over.
				Timer.getInstance().schedule(new Timer.Task() {
					public void run() {
						if (LOGGER != null) {
							LOGGER.error("Force killing of the proxy...");
						} else {
							System.err.println("Force killing of the proxy...");
						}
						System.exit(0);
					}
				}, 1000);

				if (StratumProxyManager.getInstance() != null) {
					if (LOGGER != null) {
						LOGGER.info("User requested shutdown... Gracefuly kill all connections...");
					} else {
						System.out.println("User requested shutdown... Gracefuly kill all connections...");
					}
					StratumProxyManager.getInstance().stopListeningIncomingConnections();
					StratumProxyManager.getInstance().closeAllWorkerConnections();
					StratumProxyManager.getInstance().stopPools();
				}
				if (apiHttpServer != null) {
					apiHttpServer.shutdownNow();
				}
				if (getWorkHttpServer != null) {
					getWorkHttpServer.shutdownNow();
				}
				if (LOGGER != null) {
					LOGGER.info("Shutdown !");
				} else {
					System.out.println("Shutdown !");
				}
			}
		});
	}

	/**
	 * Initialize the hashrate recoder.
	 */
	private static void initHashrateRecorder() {
		HashrateRecorder.getInstance().startCapture();
	}

	/**
	 * Initialize the database manager.
	 */
	private static void initDatabaseManager() {
		// Just get the instance to create it.
		DatabaseManager.getInstance();
	}

	/**
	 * Initialize the REST services.
	 * 
	 * @param configurationManager
	 */
	private static void initRestServices(ConfigurationManager configurationManager) {
		URI baseUri = UriBuilder.fromUri("http://" + configurationManager.getRestBindAddress()).port(configurationManager.getRestListenPort())
				.build();
		ResourceConfig config = new ResourceConfig(ProxyResources.class);
		config.register(JacksonFeature.class);
		apiHttpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
	}

	/**
	 * Initialize the Getwork system.
	 * 
	 * @param configurationManager
	 */
	private static void initGetwork(ConfigurationManager configurationManager) {
		URI baseUri = UriBuilder.fromUri("http://" + configurationManager.getGetworkBindAddress()).port(configurationManager.getGetworkListenPort())
				.build();
		getWorkHttpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri);
		ServerConfiguration serverConfiguration = getWorkHttpServer.getServerConfiguration();
		serverConfiguration.addHttpHandler(new GetworkRequestHandler(), "/", Constants.DEFAULT_GETWORK_LONG_POLLING_URL);
	}

	/**
	 * Initialize the proxy manager
	 * 
	 * @param configurationManager
	 * @throws IOException
	 * @throws CmdLineException
	 */
	private static void initProxyManager(ConfigurationManager configurationManager) throws IOException, CmdLineException {
		List<Pool> pools = configurationManager.getPools();
		LOGGER.info("Using pools: {}.", pools);

		// Start the pools.
		StratumProxyManager.getInstance().startPools(pools);

		// Start to accept incoming workers connections
		StratumProxyManager.getInstance().startListeningIncomingConnections(configurationManager.getStratumBindAddress(),
				configurationManager.getStratumListeningPort());
	}

	/**
	 * Wait and never return
	 */
	private static void waitInfinite() {
		try {
			synchronized (THREAD_MONITOR) {
				THREAD_MONITOR.wait();
			}
		} catch (Exception e) {
			LOGGER.info("Closing proxy...");
		}
	}

}
