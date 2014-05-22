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
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.pool.Pool;

/**
 * Parse and stores the parameters given through command line
 * 
 * @author strat
 * 
 */
public class CommandLineOptions {

	private static Logger LOGGER = null;

	private static CommandLineOptions instance = new CommandLineOptions();

	private CmdLineParser parser;

	@Option(name = "-n", aliases = { "--pool-names" }, usage = "Names of the pools. Space separated. (Default to host)", handler = StringArrayOptionHandler.class, metaVar = "name1 [name2] [name3]...")
	private List<String> poolNames;

	@Option(name = "-h", aliases = { "--pool-hosts" }, usage = "Hosts of the stratum servers (only the host, not the protocol since only stratum+tcp is supported), space separated. If at least one pool is specified, -u and -p has to be specified too.", handler = StringArrayOptionHandler.class, metaVar = "host1 [host2] [host3]...")
	private List<String> poolHosts;

	@Option(name = "-u", aliases = { "--pool-users" }, usage = "User names used to connect to the servers, space separated. If there are more pools than users, the last user will be used for reamining pools.", handler = StringArrayOptionHandler.class, metaVar = "user1 [user2] [user3]...")
	private List<String> poolUsers;

	@Option(name = "-p", aliases = { "--pool-passwords" }, usage = "Passwords used for the users, space separated. If there are more pools than passwords, the last password will be used for reamining pools.", handler = StringArrayOptionHandler.class, metaVar = "pass1 [pass2] [pass3]...")
	private List<String> poolPasswords;

	@Option(name = "--set-extranonce-subscribe", usage = "Enable/Disable the extranonce subscribe request on pool (default to false), space separated.", handler = BooleanArrayOptionHandler.class, metaVar = "boolean1 [boolean2] [boolean3]...")
	private List<Boolean> isExtranonceSubscribeEnabled;

	@Option(name = "--log-directory", usage = "The directory where logs will be written", handler = FileOptionHandler.class, metaVar = "directory")
	private File logDirectory;

	@Option(name = "--log-level", usage = "The level of log: OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE. Default is INFO", handler = LogLevelOptionHandler.class, metaVar = "LEVEL")
	private Level logLevel;

	@Option(name = "--stratum-listen-port", usage = "The port number to listen incoming connections. (3333 by default)", metaVar = "portNumber")
	private Integer stratumListeningPort = Constants.DEFAULT_STRATUM_LISTENING_PORT;

	@Option(name = "--stratum-listen-address", usage = "The address to bind to listen incoming connections. (0.0.0.0 by default)", metaVar = "ipAddress")
	private String stratumBindAddress;

	@Option(name = "--number-of-submit", usage = "The number of submit for each share. (Only for debug use)", metaVar = "number")
	private Integer numberOfSubmit = 1;

	@Option(name = "--rest-listen-port", usage = "The port number to listen REST requests. (8888 by default)", metaVar = "portNumber")
	private Integer restListenPort = Constants.DEFAULT_REST_LISTENING_PORT;

	@Option(name = "--rest-listen-address", usage = "The address to bind to listen REST requests. (0.0.0.0 by default)", metaVar = "ipAddress")
	private String restBindAddress = Constants.DEFAULT_REST_LISTENING_ADDRESS;

	@Option(name = "--version", usage = "Print the version.", handler = BooleanOptionHandler.class)
	private boolean isVersionRequested;

	@Option(name = "--help", usage = "Print this help.", handler = BooleanOptionHandler.class)
	private boolean isHelpRequested;

	@Option(name = "--pool-connection-retry-delay", usage = "Delay in seconds before retry to connect to an inactive pool. (5 seconds by default). 0 to disable retry.")
	private Integer poolConnectionRetryDelay = Constants.DEFAULT_POOL_CONNECTION_RETRY_DELAY;

	@Option(name = "--pool-reconnect-stability-period", usage = "Delay in seconds before declaring the pool as stable and workers could be moved on this pool. (30 seconds by default). 0 to disable.")
	private Integer poolReconnectStabilityPeriod = Constants.DEFAULT_POOL_RECONNECTION_STABILITY_PERIOD;;

	@Option(name = "--pool-no-notify-timeout", usage = "Delay in seconds to declare a pool as inactive if no mining.notify request received since the last one. (120 seconds by default). 0 to disable.")
	private Integer poolNoNotifyTimeout = Constants.DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT;

	@Option(name = "--pool-no-reconnect-different-host", usage = "Do not accept client.reconnect if connection on a different host is requested. Still accept reconnection on another port on the same host. If not set, accept all reconnection requests.", handler = BooleanOptionHandler.class)
	private boolean isRejectReconnect;

	@Option(name = "--pool-hashrate-sampling-period", usage = "The sampling period in seconds used to calculate hashrate on pools. (600 seconds by default)")
	private Integer poolHashrateSamplingPeriod = Constants.DEFAULT_POOL_HASHRATE_SAMPLING_PERIOD;

	@Option(name = "--user-hashrate-sampling-period", usage = "The sampling period in seconds used to calculate hashrate for connected users. (600 seconds by default)")
	private Integer userHashrateSamplingPeriod = Constants.DEFAULT_USER_HASHRATE_SAMPLING_PERIOD;

	@Option(name = "--connection-hashrate-sampling-period", usage = "The sampling period in seconds used to calculate hashrate on workers conections. (600 seconds by default)")
	private Integer connectionHashrateSamplingPeriod = Constants.DEFAULT_WORKER_CONNECTION_HASHRATE_SAMPLING_PERIOD;

	private List<Pool> pools;

	public static CommandLineOptions getInstance() {
		if (instance == null) {
			instance = new CommandLineOptions();
		}
		return instance;
	}

	private CommandLineOptions() {
		parser = new CmdLineParser(this);
	}

	public void parseArguments(String... args) throws CmdLineException {
		parser.parseArgument(args);
	}

	/**
	 * Return the list of pools given through the command line at startup
	 * 
	 * @return
	 * @throws CmdLineException
	 */
	public List<Pool> getPools() throws CmdLineException {
		if (pools == null) {
			pools = new ArrayList<Pool>();
			if (poolHosts != null) {
				int index = 0;

				if (poolHosts.size() > 0 && poolUsers != null && poolUsers.size() > 0 && poolPasswords != null && poolPasswords.size() > 0) {

					for (String poolHost : poolHosts) {
						String poolName = poolHost;
						String username = Constants.DEFAULT_USERNAME;
						String password = Constants.DEFAULT_PASSWORD;
						Boolean isExtranonceSubscribe = Boolean.TRUE;

						if (poolNames != null && poolNames.size() > index) {
							poolName = poolNames.get(index);
						}

						if (poolUsers != null && poolUsers.size() > index) {
							username = poolUsers.get(index);
						} else {
							username = poolUsers.get(poolUsers.size() - 1);
							getLogger().warn("No user defined for pool {}. Using {}.", poolName, username);
						}

						if (poolPasswords != null && poolPasswords.size() > index) {
							password = poolPasswords.get(index);
						} else {
							password = poolPasswords.get(poolPasswords.size() - 1);
							getLogger().warn("No password defined for pool {}. Using {}.", poolName, password);
						}

						if (isExtranonceSubscribeEnabled != null && isExtranonceSubscribeEnabled.size() > index) {
							isExtranonceSubscribe = isExtranonceSubscribeEnabled.get(index);
						}

						Pool pool = new Pool(poolName, poolHost, username, password);
						pool.setExtranonceSubscribeEnabled(isExtranonceSubscribe);
						pool.setNumberOfSubmit(numberOfSubmit);
						pool.setPriority(index);
						pool.setConnectionRetryDelay(poolConnectionRetryDelay);
						pool.setReconnectStabilityPeriod(poolReconnectStabilityPeriod);
						pool.setNoNotifyTimeout(poolNoNotifyTimeout);
						pool.setRejectReconnect(isRejectReconnect);
						pool.setSamplingHashratePeriod(poolHashrateSamplingPeriod);
						pools.add(pool);

						index++;
					}
				} else {
					throw new CmdLineException(parser,
							"At least one user/password (with -u and -p options) has to be provided if a pool host is specified.");
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

	public boolean isVersionRequested() {
		return isVersionRequested;
	}

	public boolean isHelpRequested() {
		return isHelpRequested;
	}

	public Integer getPoolConnectionRetryDelay() {
		return poolConnectionRetryDelay;
	}

	public Integer getPoolNoNotifyTimeout() {
		return poolNoNotifyTimeout;
	}

	public boolean isRejectReconnect() {
		return isRejectReconnect;
	}

	public Integer getUserHashrateSamplingPeriod() {
		return userHashrateSamplingPeriod;
	}

	public Integer getConnectionHashrateSamplingPeriod() {
		return connectionHashrateSamplingPeriod;
	}

	private static Logger getLogger() {
		if (LOGGER == null) {
			LOGGER = LoggerFactory.getLogger(CommandLineOptions.class);
		}
		return LOGGER;
	}

}
