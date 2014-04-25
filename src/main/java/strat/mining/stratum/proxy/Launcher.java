package strat.mining.stratum.proxy;

import java.io.IOException;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.manager.StratumProxyManager;
import strat.mining.stratum.proxy.pool.Pool;

public class Launcher {

	public static Logger LOGGER = null;

	public static final String THREAD_MONITOR = "";

	private static StratumProxyManager manager;

	public static void main(String[] args) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				LOGGER.info("User requested shutdown... Gracefuly kill all connections...");
				if (manager != null) {
					manager.stopListeningIncomingConnections();
					manager.stopPools();
					manager.closeAllWorkerConnections();
				}
				LOGGER.info("Shutdown !");
			}
		});

		CommandLineOptions cliParser = new CommandLineOptions();
		try {
			cliParser.parseArguments(args);

			// Set the directory used for logging.
			System.setProperty("log.directory.path", cliParser.getLogDirectory().getAbsolutePath());

			LOGGER = LoggerFactory.getLogger(Launcher.class);

			List<Pool> pools = cliParser.getPools();
			LOGGER.info("Using pools: {}.", pools);

			manager = new StratumProxyManager(pools);
			manager.startPools();

			manager.startListeningIncomingConnections(cliParser.getBindAddress(), cliParser.getListeningPort());

			try {
				synchronized (THREAD_MONITOR) {
					THREAD_MONITOR.wait();
				}
			} catch (Exception e) {
				LOGGER.info("Closing pools...");

			}

		} catch (CmdLineException e) {
			LOGGER.error("Failed to parse arguments.", e);
			cliParser.printUsage();
		} catch (IOException e1) {
			LOGGER.error("Failed to start the stratum proxy.", e1);
		}

	}
}
