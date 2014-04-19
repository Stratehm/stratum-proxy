package strat.mining.stratum.proxy.cli;

import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

/**
 * Parse and stores the parameters given through command line
 * 
 * @author strat
 * 
 */
public class CommandLineOptions {

	@Option(name = "-h", aliases = { "--host" }, usage = "Hosts of the stratum servers (only the host, not the protocol), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolAddresses;

	@Option(name = "-u", aliases = { "--user" }, usage = "User names used to connect to the servers (WARN: my BTC donation address by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolUsers;

	@Option(name = "-p", aliases = { "--password" }, usage = "Passwords used for the users (x by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolPasswords;

	@Option(name = "-c", aliases = { "--nb-connections" }, usage = "The number of connections opened on the stratum server. May be useful with pools that have problems with heavy hashrate on a single connection. 1 by default. Space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolConnectionNumbers;

	@Option(name = "-f", aliases = { "--failover-on-jsonrpc-error" }, usage = "True if the proxy has to failover when a JSON-RPC error happens. False by default")
	private Boolean failoverOnJsonRpcError = false;

	public void arf(String... args) {
		CmdLineParser parser = new CmdLineParser(this);
	}

}
