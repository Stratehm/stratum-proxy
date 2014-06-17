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
package strat.mining.stratum.proxy.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.Launcher;
import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.configuration.model.Configuration;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.pool.Pool;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manage the configuration
 * 
 * @author Strat
 * 
 */
public class ConfigurationManager {

	private static Logger logger;

	private static ConfigurationManager instance;

	private static String version;

	private File configurationFile;

	private List<Pool> pools;

	private File logDirectory;
	private Level logLevel = Level.INFO;

	private Integer numberOfSubmit = 1;
	private Integer stratumListeningPort = Constants.DEFAULT_STRATUM_LISTENING_PORT;
	private String stratumBindAddress = Constants.DEFAULT_STRATUM_LISTENING_ADDRESS;
	private Integer restListenPort = Constants.DEFAULT_REST_LISTENING_PORT;
	private String restBindAddress = Constants.DEFAULT_REST_LISTENING_ADDRESS;
	private Integer getworkListenPort = Constants.DEFAULT_GETWORK_LISTENING_PORT;
	private String getworkBindAddress = Constants.DEFAULT_GETWORK_LISTENING_ADDRESS;

	private Integer poolConnectionRetryDelay = Constants.DEFAULT_POOL_CONNECTION_RETRY_DELAY;
	private Integer poolReconnectStabilityPeriod = Constants.DEFAULT_POOL_RECONNECTION_STABILITY_PERIOD;;
	private Integer poolNoNotifyTimeout = Constants.DEFAULT_NOTIFY_NOTIFICATION_TIMEOUT;
	private boolean isRejectReconnect = false;

	private Integer poolHashrateSamplingPeriod = Constants.DEFAULT_POOL_HASHRATE_SAMPLING_PERIOD;
	private Integer userHashrateSamplingPeriod = Constants.DEFAULT_USER_HASHRATE_SAMPLING_PERIOD;
	private Integer connectionHashrateSamplingPeriod = Constants.DEFAULT_WORKER_CONNECTION_HASHRATE_SAMPLING_PERIOD;
	private boolean isScrypt = false;

	private ObjectMapper jsonParser;

	public static ConfigurationManager getInstance() {
		if (instance == null) {
			instance = new ConfigurationManager();
		}
		return instance;
	}

	public ConfigurationManager() {
		this.jsonParser = new ObjectMapper();
	}

	/**
	 * Try to load the configuration from command line or from configuration
	 * file. Return an execption if configuration cannot be loaded.
	 * 
	 * @param cliArguments
	 * @return
	 */
	public void loadConfiguration(String[] cliArguments) throws Exception {

		CommandLineOptions cliParser = new CommandLineOptions();
		// Parse the command line options
		cliParser.parseArguments(cliArguments);

		if (cliParser.isHelpRequested()) {
			cliParser.printUsage();
		} else if (cliParser.isVersionRequested()) {
			String version = "stratum-proxy by Stratehm. GPLv3 Licence. Version " + Constants.VERSION;
			System.out.println(version);
		} else {

			// If -f is specified, use the configuration file
			if (cliParser.getConfigurationFile() != null) {
				// Use configuration file.
				configurationFile = cliParser.getConfigurationFile();
				if (configurationFile.isFile() && configurationFile.canRead()) {
					useConfigurationFile();
				} else {
					throw new FileNotFoundException("Configuration file " + configurationFile.getAbsolutePath()
							+ " does not exist or is not readable.");
				}
			} else {
				// Use parameters of the command line.
				useCommandLine(cliParser);
			}
		}
	}

	/**
	 * Set the configuration based on the configuration file.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws BadParameterException
	 */
	private void useConfigurationFile() throws JsonParseException, JsonMappingException, IOException, BadParameterException {
		Configuration configuration = jsonParser.readValue(configurationFile, Configuration.class);

		logLevel = configuration.getLogLevel() != null ? Level.toLevel(configuration.getLogLevel()) : logLevel;
		logDirectory = configuration.getLogDirectory() != null ? new File(configuration.getLogDirectory()) : logDirectory;
		// Initialize the logging system
		initLogging();

		stratumListeningPort = configuration.getStratumListenPort() != null ? configuration.getStratumListenPort() : stratumListeningPort;
		stratumBindAddress = configuration.getStratumListenAddress() != null ? configuration.getStratumListenAddress() : stratumBindAddress;
		restListenPort = configuration.getApiListenPort() != null ? configuration.getApiListenPort() : restListenPort;
		restBindAddress = configuration.getApiListenAddress() != null ? configuration.getApiListenAddress() : restBindAddress;
		getworkListenPort = configuration.getGetworkListenPort() != null ? configuration.getGetworkListenPort() : getworkListenPort;
		getworkBindAddress = configuration.getGetworkListenAddress() != null ? configuration.getGetworkListenAddress() : getworkBindAddress;

		poolConnectionRetryDelay = configuration.getPoolConnectionRetryDelay() != null ? configuration.getPoolConnectionRetryDelay()
				: poolConnectionRetryDelay;
		poolReconnectStabilityPeriod = configuration.getPoolReconnectStabilityPeriod() != null ? configuration.getPoolReconnectStabilityPeriod()
				: poolReconnectStabilityPeriod;
		poolNoNotifyTimeout = configuration.getPoolNoNotifyTimeout() != null ? configuration.getPoolNoNotifyTimeout() : poolNoNotifyTimeout;
		isRejectReconnect = configuration.getRejectReconnectOnDifferentHost() != null ? configuration.getRejectReconnectOnDifferentHost()
				: isRejectReconnect;

		poolHashrateSamplingPeriod = configuration.getPoolHashrateSamplingPeriod() != null ? configuration.getPoolHashrateSamplingPeriod()
				: poolHashrateSamplingPeriod;
		userHashrateSamplingPeriod = configuration.getUserHashrateSamplingPeriod() != null ? configuration.getUserHashrateSamplingPeriod()
				: userHashrateSamplingPeriod;
		connectionHashrateSamplingPeriod = configuration.getConnectionHashrateSamplingPeriod() != null ? configuration
				.getConnectionHashrateSamplingPeriod() : connectionHashrateSamplingPeriod;
		isScrypt = configuration.getIsScrypt() != null ? configuration.getIsScrypt() : isScrypt;

		buildPoolsFromConfigurationFile(configuration);
	}

	/**
	 * Build the pool list from the configuration file.
	 * 
	 * @param configuration
	 */
	private void buildPoolsFromConfigurationFile(Configuration configuration) throws BadParameterException {
		pools = new ArrayList<Pool>();

		if (configuration.getPools() != null && configuration.getPools().size() > 0) {
			int counter = 0;
			for (strat.mining.stratum.proxy.configuration.model.Pool confPool : configuration.getPools()) {

				String poolName = confPool.getHost();
				String poolHost = null;
				String username = null;
				String password = Constants.DEFAULT_PASSWORD;
				Boolean isExtranonceSubscribe = Boolean.FALSE;
				Boolean isAppendWorkerNames = Boolean.FALSE;
				String workerNameSeparator = Constants.DEFAULT_WORKER_NAME_SEPARTOR;
				Boolean useWorkerPassword = Boolean.FALSE;

				if (confPool.getName() != null && !confPool.getName().trim().isEmpty()) {
					poolName = confPool.getName();
				}

				if (confPool.getHost() != null && !confPool.getHost().trim().isEmpty()) {
					poolHost = confPool.getHost();
				} else {
					throw new BadParameterException("Missing host for the pool number " + (counter + 1) + " in configuration file.");
				}

				if (confPool.getUser() != null && !confPool.getUser().trim().isEmpty()) {
					username = confPool.getUser();
				} else {
					throw new BadParameterException("Missing username for the pool with host " + poolHost + " in configuration file.");
				}

				if (confPool.getPassword() != null) {
					username = confPool.getPassword();
				}

				if (confPool.getEnableExtranonceSubscribe() != null) {
					isExtranonceSubscribe = confPool.getEnableExtranonceSubscribe();
				}

				if (confPool.getAppendWorkerNames() != null) {
					isAppendWorkerNames = confPool.getAppendWorkerNames();
				}

				if (confPool.getWorkerNameSeparator() != null) {
					workerNameSeparator = confPool.getWorkerNameSeparator();
				}

				if (confPool.getUseWorkerPassword() != null) {
					useWorkerPassword = confPool.getUseWorkerPassword();
				}

				Pool pool = new Pool(poolName, poolHost, username, password);
				pool.setExtranonceSubscribeEnabled(isExtranonceSubscribe);
				pool.setNumberOfSubmit(numberOfSubmit);
				pool.setPriority(counter);
				pool.setConnectionRetryDelay(poolConnectionRetryDelay);
				pool.setReconnectStabilityPeriod(poolReconnectStabilityPeriod);
				pool.setNoNotifyTimeout(poolNoNotifyTimeout);
				pool.setRejectReconnect(isRejectReconnect);
				pool.setSamplingHashratePeriod(poolHashrateSamplingPeriod);
				pool.setAppendWorkerNames(isAppendWorkerNames);
				pool.setWorkerSeparator(workerNameSeparator);
				pool.setUseWorkerPassword(useWorkerPassword);
				pools.add(pool);

				counter++;
			}
		}
	}

	/**
	 * Set the configuration based on the command line arguments.
	 * 
	 * @param cliParser
	 */
	private void useCommandLine(CommandLineOptions cliParser) throws Exception {

		logLevel = cliParser.getLogLevel();
		logDirectory = cliParser.getLogDirectory();
		// Initialize the logging system
		initLogging();

		numberOfSubmit = cliParser.getNumberOfSubmit() != null ? cliParser.getNumberOfSubmit() : numberOfSubmit;
		stratumListeningPort = cliParser.getStratumListeningPort() != null ? cliParser.getStratumListeningPort() : stratumListeningPort;
		stratumBindAddress = cliParser.getStratumBindAddress() != null ? cliParser.getStratumBindAddress() : stratumBindAddress;
		restListenPort = cliParser.getRestListenPort() != null ? cliParser.getRestListenPort() : restListenPort;
		restBindAddress = cliParser.getRestBindAddress() != null ? cliParser.getRestBindAddress() : restBindAddress;
		getworkListenPort = cliParser.getGetworkListenPort() != null ? cliParser.getGetworkListenPort() : getworkListenPort;
		getworkBindAddress = cliParser.getGetworkBindAddress() != null ? cliParser.getGetworkBindAddress() : getworkBindAddress;

		poolConnectionRetryDelay = cliParser.getPoolConnectionRetryDelay() != null ? cliParser.getPoolConnectionRetryDelay()
				: poolConnectionRetryDelay;
		poolReconnectStabilityPeriod = cliParser.getPoolReconnectStabilityPeriod() != null ? cliParser.getPoolReconnectStabilityPeriod()
				: poolReconnectStabilityPeriod;
		poolNoNotifyTimeout = cliParser.getPoolNoNotifyTimeout() != null ? cliParser.getPoolNoNotifyTimeout() : poolNoNotifyTimeout;
		isRejectReconnect = cliParser.isRejectReconnect() != null ? cliParser.isRejectReconnect() : isRejectReconnect;

		poolHashrateSamplingPeriod = cliParser.getPoolHashrateSamplingPeriod() != null ? cliParser.getPoolHashrateSamplingPeriod()
				: poolHashrateSamplingPeriod;
		userHashrateSamplingPeriod = cliParser.getUserHashrateSamplingPeriod() != null ? cliParser.getUserHashrateSamplingPeriod()
				: userHashrateSamplingPeriod;
		connectionHashrateSamplingPeriod = cliParser.getConnectionHashrateSamplingPeriod() != null ? cliParser.getConnectionHashrateSamplingPeriod()
				: connectionHashrateSamplingPeriod;
		isScrypt = cliParser.isScrypt() != null ? cliParser.isScrypt() : isScrypt;

		buildPoolsFromCommandLine(cliParser);
	}

	/**
	 * Return the list of pools given through the command line at startup
	 * 
	 * @return
	 * @throws CmdLineException
	 */
	public List<Pool> getPools() {
		return pools;
	}

	/**
	 * Build the pools based on the command line parameters
	 * 
	 * @param cliParser
	 * @throws Exception
	 */
	private void buildPoolsFromCommandLine(CommandLineOptions cliParser) throws Exception {
		pools = new ArrayList<Pool>();
		if (cliParser.getPoolHosts() != null) {
			int index = 0;

			if (cliParser.getPoolHosts().size() > 0 && cliParser.getPoolUsers() != null && cliParser.getPoolUsers().size() > 0
					&& cliParser.getPoolPasswords() != null && cliParser.getPoolPasswords().size() > 0) {

				for (String poolHost : cliParser.getPoolHosts()) {
					String poolName = poolHost;
					String username = Constants.DEFAULT_USERNAME;
					String password = Constants.DEFAULT_PASSWORD;
					Boolean isExtranonceSubscribe = Boolean.FALSE;
					Boolean isAppendWorkerNames = Boolean.FALSE;
					String workerNameSeparator = Constants.DEFAULT_WORKER_NAME_SEPARTOR;
					Boolean useWorkerPassword = Boolean.FALSE;

					if (cliParser.getPoolNames() != null && cliParser.getPoolNames().size() > index) {
						poolName = cliParser.getPoolNames().get(index);
					}

					if (cliParser.getPoolUsers() != null && cliParser.getPoolUsers().size() > index) {
						username = cliParser.getPoolUsers().get(index);
					} else {
						username = cliParser.getPoolUsers().get(cliParser.getPoolUsers().size() - 1);
						logger.warn("No user defined for pool {}. Using {}.", poolName, username);
					}

					if (cliParser.getPoolPasswords() != null && cliParser.getPoolPasswords().size() > index) {
						password = cliParser.getPoolPasswords().get(index);
					} else {
						password = cliParser.getPoolPasswords().get(cliParser.getPoolPasswords().size() - 1);
						logger.warn("No password defined for pool {}. Using {}.", poolName, password);
					}

					if (cliParser.getIsExtranonceSubscribeEnabled() != null && cliParser.getIsExtranonceSubscribeEnabled().size() > index) {
						isExtranonceSubscribe = cliParser.getIsExtranonceSubscribeEnabled().get(index);
					}

					if (cliParser.getPoolsAppendWorkerNames() != null && cliParser.getPoolsAppendWorkerNames().size() > index) {
						isAppendWorkerNames = cliParser.getPoolsAppendWorkerNames().get(index);
					}

					if (cliParser.getPoolsUseWorkerPassword() != null && cliParser.getPoolsUseWorkerPassword().size() > index) {
						useWorkerPassword = cliParser.getPoolsUseWorkerPassword().get(index);
					}

					if (cliParser.getPoolsWorkerNameSeparator() != null && cliParser.getPoolsWorkerNameSeparator().size() > index) {
						workerNameSeparator = cliParser.getPoolsWorkerNameSeparator().get(index);
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
					pool.setAppendWorkerNames(isAppendWorkerNames);
					pool.setWorkerSeparator(workerNameSeparator);
					pool.setUseWorkerPassword(useWorkerPassword);
					pools.add(pool);

					index++;
				}
			} else {
				throw new BadParameterException("At least one user/password (with -u and -p options) has to be provided if a pool host is specified.");
			}

		}
	}

	/**
	 * 
	 * @return
	 */
	public File getLogDirectory() {
		return logDirectory;
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

	public Integer getGetworkListenPort() {
		return getworkListenPort;
	}

	public String getGetworkBindAddress() {
		return getworkBindAddress;
	}

	public boolean isScrypt() {
		return isScrypt;
	}

	public File getConfigurationFile() {
		return configurationFile;
	}

	/**
	 * Initialize the logging system
	 * 
	 * @param cliParser
	 */
	private void initLogging() {
		if (logDirectory == null || !logDirectory.isDirectory() || !logDirectory.exists()) {
			System.err.println("Log directory not set or available. Use the tmp OS directory.");
			logDirectory = new File(System.getProperty("java.io.tmpdir"));
		}

		// Set the directory used for logging.
		System.setProperty("log.directory.path", logDirectory.getAbsolutePath());

		String logLevelMessage = null;
		if (logLevel == null) {
			logLevel = Level.INFO;
			logLevelMessage = "LogLevel not set, using INFO.";
		} else {
			logLevelMessage = "Using " + logLevel.toString() + " LogLevel.";
		}
		LogManager.getRootLogger().setLevel(logLevel);
		logger = LoggerFactory.getLogger(Launcher.class);
		logger.info(logLevelMessage);
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
