package strat.mining.stratum.proxy.cli;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
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

	@Option(name = "-h", aliases = { "--host" }, usage = "Hosts of the stratum servers (only the host, not the protocol), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolHosts;

	@Option(name = "-u", aliases = { "--user" }, usage = "User names used to connect to the servers (WARN: my BTC donation address by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolUsers;

	@Option(name = "-p", aliases = { "--password" }, usage = "Passwords used for the users (x by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolPasswords;

	@Option(name = "-c", aliases = { "--nb-connections" }, usage = "The number of connections opened on the stratum server. May be useful with pools that have problems with heavy hashrate on a single connection. 1 by default. Space separated", handler = StringArrayOptionHandler.class)
	private List<Integer> poolConnectionNumbers;

	@Option(name = "-f", aliases = { "--failover-on-jsonrpc-error" }, usage = "True if the proxy has to failover when a JSON-RPC error happens. False by default")
	private Boolean failoverOnJsonRpcError = false;

	private List<Pool> pools;

	public void parseArguments(String... args) throws CmdLineException {
		CmdLineParser parser = new CmdLineParser(this);
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
					Integer nbConnections = Constants.DEFAULT_NB_CONNECTIONS;

					if (poolUsers != null && poolUsers.size() >= index) {
						username = poolUsers.get(index);
					}

					if (poolPasswords != null && poolPasswords.size() >= index) {
						password = poolPasswords.get(index);
					}

					if (poolConnectionNumbers != null
							&& poolConnectionNumbers.size() >= index) {
						nbConnections = poolConnectionNumbers.get(index) > 0 ? poolConnectionNumbers
								.get(index) : nbConnections;
					}

					Pool pool = new Pool(poolHost, username, password,
							nbConnections);
					pools.add(pool);

					index++;
				}
			}
		}
		return pools;
	}

}
