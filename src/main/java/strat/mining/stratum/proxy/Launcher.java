package strat.mining.stratum.proxy;

import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.pool.Pool;

public class Launcher {

	public static Logger LOGGER = null;

	public static final String THREAD_MONITOR = "";

	public static void main(String[] args) {

		CommandLineOptions cliParser = new CommandLineOptions();
		try {
			cliParser.parseArguments(args);

			// Set the directory used for logging.
			System.setProperty("log.directory.path", cliParser.getLogDirectory().getAbsolutePath());

			LOGGER = LoggerFactory.getLogger(Launcher.class);

			List<Pool> pools = cliParser.getPools();
			LOGGER.info("Using pools: {}.", pools);
			for (Pool pool : pools) {
				try {
					pool.startPool();
				} catch (Exception e) {
					LOGGER.error("Failed to start the pool {}.", pool, e);
				}
			}

		} catch (CmdLineException e) {
			LOGGER.error("Failed to parse arguments.", e);
			cliParser.printUsage();
		}

		try {
			synchronized (THREAD_MONITOR) {
				THREAD_MONITOR.wait();
			}
		} catch (Exception e) {
			LOGGER.info("Closing pools...");
			List<Pool> pools = cliParser.getPools();
			for (Pool pool : pools) {
				pool.stopPool();
			}
		}
	}
}
