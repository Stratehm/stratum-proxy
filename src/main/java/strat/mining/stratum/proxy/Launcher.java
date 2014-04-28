package strat.mining.stratum.proxy;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.ProxyResources;

public class Launcher {

	public static Logger LOGGER = null;

	public static final String THREAD_MONITOR = "";

	private static StratumProxyManager stratumProxyManager;

	private static HttpServer httpServer;

	public static void main(String[] args) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				LOGGER.info("User requested shutdown... Gracefuly kill all connections...");
				if (stratumProxyManager != null) {
					stratumProxyManager.stopListeningIncomingConnections();
					stratumProxyManager.stopPools();
					stratumProxyManager.closeAllWorkerConnections();
				}
				if (httpServer != null) {
					httpServer.shutdownNow();
				}
				LOGGER.info("Shutdown !");
			}
		});

		CommandLineOptions cliParser = new CommandLineOptions();
		try {
			cliParser.parseArguments(args);

			// Initialize the logging system
			initLogging(cliParser);

			// Initialize the proxy manager
			initProxyManager(cliParser);

			// Initialize the rest services
			initRestServices(cliParser);

			// Wait the end of the program
			waitInfinite();

		} catch (CmdLineException e) {
			LOGGER.error("Failed to parse arguments.", e);
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
		httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
	}

	/**
	 * Initialize the proxy manager
	 * 
	 * @param cliParser
	 * @throws IOException
	 */
	private static void initProxyManager(CommandLineOptions cliParser) throws IOException {
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
}
