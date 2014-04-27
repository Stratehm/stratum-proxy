package strat.mining.stratum.proxy.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.pool.Pool;

/**
 * Parse and stores the parameters given through command line
 * 
 * @author strat
 * 
 */
public class CommandLineOptions {

	private CmdLineParser parser;

	@Option(name = "-h", aliases = { "--host" }, usage = "Hosts of the stratum servers (only the host, not the protocol), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolHosts;

	@Option(name = "-u", aliases = { "--user" }, usage = "User names used to connect to the servers (WARN: my BTC donation address by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolUsers;

	@Option(name = "-p", aliases = { "--password" }, usage = "Passwords used for the users (x by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolPasswords;

	@Option(name = "--log-directory", usage = "The directory where logs will be written", handler = FileOptionHandler.class)
	private File logDirectory;

	@Option(name = "--log-level", usage = "The level of log: OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE. Default is INFO", handler = LogLevelOptionHandler.class)
	private Level logLevel;

	@Option(name = "--listen-port", usage = "The port number to listen incoming connections. (3333 by default")
	private Integer listeningPort = Constants.DEFAULT_LISTENING_PORT;

	@Option(name = "--listen-address", usage = "The address to bind to listen incoming connections. (0.0.0.0 by default)")
	private String bindAddress;

	private List<Pool> pools;

	public CommandLineOptions() {
		parser = new CmdLineParser(this);
	}

	public void parseArguments(String... args) throws CmdLineException {
		parser.parseArgument(args);
	}

	/**
	 * Return the list of pools given through the command line at startup
	 * 
	 * @return
	 */
	public List<Pool> getPools() {
		if (pools == null) {
			pools = new ArrayList<Pool>();
			if (poolHosts != null) {
				int index = 0;
				for (String poolHost : poolHosts) {
					String username = Constants.DEFAULT_USERNAME;
					String password = Constants.DEFAULT_PASSWORD;

					if (poolUsers != null && poolUsers.size() >= index) {
						username = poolUsers.get(index);
					}

					if (poolPasswords != null && poolPasswords.size() >= index) {
						password = poolPasswords.get(index);
					}

					Pool pool = new Pool(poolHost, username, password);
					pools.add(pool);

					index++;
				}
			}
		}
		return pools;
	}

	public File getLogDirectory() {
		File result = logDirectory;
		if (result == null || !result.isDirectory() || result.exists()) {
			System.err.println("Log directory not set or available. Use the tmp OS directory.");
			result = new File(System.getProperty("java.io.tmpdir"));
		}
		return result;
	}

	public void printUsage() {
		parser.printUsage(System.out);
	}

	public Integer getListeningPort() {
		return listeningPort;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public Level getLogLevel() {
		return logLevel;
	}

}
