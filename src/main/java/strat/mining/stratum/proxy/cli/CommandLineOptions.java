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

	@Option(name = "-n", aliases = { "--pool-names" }, usage = "Names of the pools. Space separated. (Default to host)")
	private List<String> poolNames;

	@Option(name = "-h", aliases = { "--pool-hosts" }, usage = "Hosts of the stratum servers (only the host, not the protocol), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolHosts;

	@Option(name = "-u", aliases = { "--pool-users" }, usage = "User names used to connect to the servers (WARN: my BTC donation address by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolUsers;

	@Option(name = "-p", aliases = { "--pool-passwords" }, usage = "Passwords used for the users (x by default), space separated", handler = StringArrayOptionHandler.class)
	private List<String> poolPasswords;

	@Option(name = "--set-extranonce-subscribe", usage = "Enable/Disable the extranonce subscribe request on pool (default to true), space separated.", handler = BooleanArrayOptionHandler.class)
	private List<Boolean> isExtranonceSubscribeEnabled;

	@Option(name = "--log-directory", usage = "The directory where logs will be written", handler = FileOptionHandler.class)
	private File logDirectory;

	@Option(name = "--log-level", usage = "The level of log: OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE. Default is INFO", handler = LogLevelOptionHandler.class)
	private Level logLevel;

	@Option(name = "--stratum-listen-port", usage = "The port number to listen incoming connections. (3333 by default)")
	private Integer stratumListeningPort = Constants.DEFAULT_STRATUM_LISTENING_PORT;

	@Option(name = "--stratum-listen-address", usage = "The address to bind to listen incoming connections. (0.0.0.0 by default)")
	private String stratumBindAddress;

	@Option(name = "--number-of-submit", usage = "The number of submit for each share. (Only for debug use)")
	private Integer numberOfSubmit = 1;

	@Option(name = "--rest-listen-port", usage = "The port number to listen REST requests. (8888 by default)")
	private Integer restListenPort = Constants.DEFAULT_REST_LISTENING_PORT;

	@Option(name = "--rest-listen-address", usage = "The address to bind to listen incoming connections. (0.0.0.0 by default)")
	private String restBindAddress = Constants.DEFAULT_REST_LISTENING_ADDRESS;

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
					String poolName = poolHost;
					String username = Constants.DEFAULT_USERNAME;
					String password = Constants.DEFAULT_PASSWORD;
					Boolean isExtranonceSubscribe = Boolean.TRUE;

					if (poolNames != null && poolNames.size() >= index) {
						poolName = poolNames.get(index);
					}

					if (poolUsers != null && poolUsers.size() >= index) {
						username = poolUsers.get(index);
					}

					if (poolPasswords != null && poolPasswords.size() >= index) {
						password = poolPasswords.get(index);
					}

					if (isExtranonceSubscribeEnabled != null && isExtranonceSubscribeEnabled.size() >= index) {
						isExtranonceSubscribe = isExtranonceSubscribeEnabled.get(index);
					}

					Pool pool = new Pool(poolName, poolHost, username, password);
					pool.setExtranonceSubscribeEnabled(isExtranonceSubscribe);
					pool.setNumberOfSubmit(numberOfSubmit);
					pool.setPriority(index);
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

	public Integer getStratumListeningPort() {
		return stratumListeningPort;
	}

	public String getStratumBindAddress() {
		return stratumBindAddress;
	}

	public Integer getRestListenPort() {
		return restListenPort;
	}

	public String getRestBindAddress() {
		return restBindAddress;
	}

	public Level getLogLevel() {
		return logLevel;
	}

}
