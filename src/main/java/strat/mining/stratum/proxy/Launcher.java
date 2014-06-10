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
import java.net.URL;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.ProxyResources;
import strat.mining.stratum.proxy.worker.GetworkRequestHandler;

public class Launcher {

	public static Logger LOGGER = null;

	public static final String THREAD_MONITOR = "";

	private static StratumProxyManager stratumProxyManager;

	private static HttpServer apiHttpServer;

	private static HttpServer getWorkHttpServer;

	private static String version;

	public static void main(String[] args) {

		// List<String> merkleBranches = new ArrayList<String>();
		// merkleBranches.add("32bac6b596b722100e6d0d5a451ec78b4252161603e5c140ce61ce29f1451ff9");
		// merkleBranches.add("0808cdd8a165d9151856258b7ce65c476220afc669e56076f5f7b541099de3d4");
		// merkleBranches.add("8ecfcf4d911ae073f90af0d63cafc37daf35947f766310df4a37e67339713ee4");
		// merkleBranches.add("3b33a4f5d3a406c5760415cd2c71442e082dffb261f1f1abcb905180143d7b63");
		// GetworkJobTemplate arf = new GetworkJobTemplate("1850", "00000002",
		// "72417428ad46bd3265c270b1d1c2dee6723136c98e3cff7be0b72b8ed00e012f",
		// "5396f810", "1b0616be", merkleBranches,
		// "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff230362e608062f503253482f0412f8965308",
		// "092f7374726174756d2f000000000100c6362a010000001976a914c8f58075fdf2ba12619f34d15385567e5a1cb99488ac00000000",
		// "0861e04900");
		//
		// arf.setDifficulty(700, true);
		//
		// arf.getData("000001");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (LOGGER != null) {
					if (stratumProxyManager != null) {
						LOGGER.info("User requested shutdown... Gracefuly kill all connections...");
						stratumProxyManager.stopListeningIncomingConnections();
						stratumProxyManager.stopPools();
						stratumProxyManager.closeAllWorkerConnections();
					}
					if (apiHttpServer != null) {
						apiHttpServer.shutdownNow();
					}
					if (getWorkHttpServer != null) {
						getWorkHttpServer.shutdownNow();
					}
					LOGGER.info("Shutdown !");
				}
			}
		});

		CommandLineOptions cliParser = CommandLineOptions.getInstance();
		try {
			cliParser.parseArguments(args);

			if (cliParser.isHelpRequested()) {
				cliParser.printUsage();
			} else if (cliParser.isVersionRequested()) {
				String version = "stratum-proxy by Stratehm. GPLv3 Licence. Version " + Constants.VERSION;
				System.out.println(version);
			} else {

				// Initialize the logging system
				initLogging(cliParser);

				// Initialize the proxy manager
				initProxyManager(cliParser);

				// Initialize the Getwork system
				initGetwork(cliParser);

				// Initialize the rest services
				initRestServices(cliParser);

				// Wait the end of the program
				waitInfinite();
			}

		} catch (CmdLineException e) {
			if (LOGGER != null) {
				LOGGER.error("Failed to parse arguments.", e);
			} else {
				System.out.println("Failed to start the proxy: ");
				e.printStackTrace();
			}
			cliParser.printUsage();
		} catch (IOException e1) {
			LOGGER.error("Failed to start the stratum proxy.", e1);
		}

	}

	/**
	 * Initialize the REST services.
	 * 
	 * @param cliParser
	 */
	private static void initRestServices(CommandLineOptions cliParser) {
		URI baseUri = UriBuilder.fromUri("http://" + cliParser.getRestBindAddress()).port(cliParser.getRestListenPort()).build();
		ResourceConfig config = new ResourceConfig(ProxyResources.class);
		config.register(JacksonFeature.class);
		apiHttpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
	}

	/**
	 * Initialize the Getwork system.
	 * 
	 * @param cliParser
	 */
	private static void initGetwork(CommandLineOptions cliParser) {
		URI baseUri = UriBuilder.fromUri("http://" + cliParser.getGetworkBindAddress()).port(cliParser.getGetworkListenPort()).build();
		apiHttpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri);
		ServerConfiguration serverConfiguration = apiHttpServer.getServerConfiguration();
		serverConfiguration.addHttpHandler(new GetworkRequestHandler(stratumProxyManager), "/", Constants.DEFAULT_GETWORK_LONG_POLLING_URL);
	}

	/**
	 * Initialize the proxy manager
	 * 
	 * @param cliParser
	 * @throws IOException
	 * @throws CmdLineException
	 */
	private static void initProxyManager(CommandLineOptions cliParser) throws IOException, CmdLineException {
		List<Pool> pools = cliParser.getPools();
		LOGGER.info("Using pools: {}.", pools);

		stratumProxyManager = new StratumProxyManager(pools);

		// Connect to the pools.
		stratumProxyManager.startPools();

		// Start to accept incoming workers connections
		stratumProxyManager.startListeningIncomingConnections(cliParser.getStratumBindAddress(), cliParser.getStratumListeningPort());
	}

	/**
	 * Initialize the logging system
	 * 
	 * @param cliParser
	 */
	private static void initLogging(CommandLineOptions cliParser) {
		// Set the directory used for logging.
		System.setProperty("log.directory.path", cliParser.getLogDirectory().getAbsolutePath());

		Level logLevel = cliParser.getLogLevel();
		String logLevelMessage = null;
		if (logLevel == null) {
			logLevel = Level.INFO;
			logLevelMessage = "LogLevel not set, using INFO.";
		} else {
			logLevelMessage = "Using " + logLevel.toString() + " LogLevel.";
		}
		LogManager.getRootLogger().setLevel(logLevel);
		LOGGER = LoggerFactory.getLogger(Launcher.class);
		LOGGER.info(logLevelMessage);
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

	/**
	 * Return the stratum proxy manager
	 * 
	 * @return
	 */
	public static StratumProxyManager getStratumProxyManager() {
		return stratumProxyManager;
	}

	/**
	 * Return the version of the program
	 * 
	 * @return
	 */
	public static String getVersion() {
		if (version == null) {
			version = "Dev";

			Class<Launcher> clazz = Launcher.class;
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			if (classPath.startsWith("jar")) {
				// Class not from JAR
				String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";

				try {
					Manifest manifest = new Manifest(new URL(manifestPath).openStream());
					Attributes attr = manifest.getMainAttributes();
					version = attr.getValue("Implementation-Version");
				} catch (IOException e) {
					// Do nothing, just return Unknown as version
				}
			}
		}

		return version;
	}
}
