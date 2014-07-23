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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.RollingFileAppender;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import strat.mining.stratum.proxy.Launcher;
import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.configuration.model.Configuration;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.exception.BadParameterException;
import strat.mining.stratum.proxy.manager.strategy.PriorityFailoverStrategyManager;
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
	private Level apiLogLevel = null;
	private boolean disableLogAppend = false;

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

	private File databaseDirectory;
	private Integer hashrateDatabaseSamplingPeriod = Constants.DEFAULT_HASHRATE_DATABASE_SAMPLING_PERIOD;
	private Integer hashrateDatabaseHistoryDepth = Constants.DEFAULT_HASHRATE_DATABASE_HISTORY_DEPTH;

	private boolean noMidsate = false;
	private boolean validateSha26GetworkShares = false;

	private String poolSwitchingStrategy = PriorityFailoverStrategyManager.NAME;

	private Integer weightedRoundRobinRoundDuration = Constants.DEFAULT_WEIGHTED_ROUND_ROBIN_ROUND_DURATION;

	private boolean disableGetwork = false;
	private boolean disableStratum = false;
	private boolean disableApi = false;

	private String apiUser;
	private String apiPassword;

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
			initDatabaseDirectory();
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
		apiLogLevel = configuration.getApiLogLevel() != null ? Level.toLevel(configuration.getApiLogLevel()) : apiLogLevel;
		logDirectory = configuration.getLogDirectory() != null && !configuration.getLogDirectory().trim().isEmpty() ? new File(
				configuration.getLogDirectory()) : logDirectory;
		disableLogAppend = configuration.isDisableLogAppend() != null ? configuration.isDisableLogAppend() : disableLogAppend;
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

		databaseDirectory = configuration.getDatabaseDirectory() != null && !configuration.getDatabaseDirectory().trim().isEmpty() ? new File(
				configuration.getDatabaseDirectory()) : databaseDirectory;
		hashrateDatabaseSamplingPeriod = configuration.getHashrateDatabaseSamplingPeriod() != null ? configuration
				.getHashrateDatabaseSamplingPeriod() : hashrateDatabaseSamplingPeriod;
		hashrateDatabaseHistoryDepth = configuration.getHashrateDatabaseHistoryDepth() != null ? configuration.getHashrateDatabaseHistoryDepth()
				: hashrateDatabaseHistoryDepth;
		noMidsate = configuration.getNoMidstate() != null ? configuration.getNoMidstate() : noMidsate;
		validateSha26GetworkShares = configuration.getValidateSha26GetworkShares() != null ? configuration.getValidateSha26GetworkShares()
				: validateSha26GetworkShares;

		poolSwitchingStrategy = configuration.getPoolSwitchingStrategy() != null ? configuration.getPoolSwitchingStrategy() : poolSwitchingStrategy;
		weightedRoundRobinRoundDuration = configuration.getWeightedRoundRobinRoundDuration() != null ? configuration
				.getWeightedRoundRobinRoundDuration() * 60000 : weightedRoundRobinRoundDuration;

		disableGetwork = configuration.isDisableGetwork() != null ? configuration.isDisableGetwork() : disableGetwork;
		disableStratum = configuration.isDisableStratum() != null ? configuration.isDisableStratum() : disableStratum;
		disableApi = configuration.isDisableApi() != null ? configuration.isDisableApi() : disableApi;

		apiUser = configuration.getApiUser() != null ? configuration.getApiUser() : apiUser;
		if (apiUser != null && apiUser.trim().isEmpty()) {
			apiUser = null;
		}

		apiPassword = configuration.getApiPassword() != null ? configuration.getApiPassword() : apiPassword;
		if (apiPassword != null && apiPassword.trim().isEmpty()) {
			apiPassword = null;
		}

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
				Boolean isEnabled = Boolean.TRUE;
				Integer poolWeight = Constants.DEFAULT_POOL_WEIGHT;

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
					password = confPool.getPassword();
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

				if (confPool.getIsEnabled() != null) {
					isEnabled = confPool.getIsEnabled();
				}

				if (confPool.getWeight() != null) {
					poolWeight = confPool.getWeight();
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
				pool.setWeight(poolWeight);
				try {
					pool.setEnabled(isEnabled);
				} catch (Exception e) {
					// Should never happens. Else, it is a bug.
					System.err.println("Error during creation of pool " + pool.getName() + " (cause: enabled value)");
					e.printStackTrace();
				}
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
		apiLogLevel = cliParser.getApiLogLevel();
		disableLogAppend = cliParser.isDisableLogAppend() != null ? cliParser.isDisableLogAppend() : disableLogAppend;
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

		databaseDirectory = cliParser.getDatabaseDirectory() != null ? cliParser.getDatabaseDirectory() : databaseDirectory;
		hashrateDatabaseSamplingPeriod = cliParser.getHashrateDatabaseSamplingPeriod() != null ? cliParser.getHashrateDatabaseSamplingPeriod()
				: hashrateDatabaseSamplingPeriod;
		hashrateDatabaseHistoryDepth = cliParser.getHashrateDatabaseHistoryDepth() != null ? cliParser.getHashrateDatabaseHistoryDepth()
				: hashrateDatabaseHistoryDepth;
		noMidsate = cliParser.isNoMidstate() != null ? cliParser.isNoMidstate() : noMidsate;
		validateSha26GetworkShares = cliParser.isValidateSha26GetworkShares() != null ? cliParser.isValidateSha26GetworkShares()
				: validateSha26GetworkShares;

		poolSwitchingStrategy = cliParser.getPoolSwitchingStrategy() != null ? cliParser.getPoolSwitchingStrategy() : poolSwitchingStrategy;
		weightedRoundRobinRoundDuration = cliParser.getWeightedRoundRobinRoundDuration() != null ? cliParser.getWeightedRoundRobinRoundDuration() * 60000
				: weightedRoundRobinRoundDuration;

		disableGetwork = cliParser.isDisableGetwork() != null ? cliParser.isDisableGetwork() : disableGetwork;
		disableStratum = cliParser.isDisableStratum() != null ? cliParser.isDisableStratum() : disableStratum;
		disableApi = cliParser.isDisableApi() != null ? cliParser.isDisableApi() : disableApi;

		apiUser = cliParser.getApiUser() != null ? cliParser.getApiUser() : apiUser;
		if (apiUser != null && apiUser.trim().isEmpty()) {
			apiUser = null;
		}

		apiPassword = cliParser.getApiPassword() != null ? cliParser.getApiPassword() : apiPassword;
		if (apiPassword != null && apiPassword.trim().isEmpty()) {
			apiPassword = null;
		}

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
					Integer poolWeight = Constants.DEFAULT_POOL_WEIGHT;

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

					if (cliParser.getPoolsWeight() != null && cliParser.getPoolsWeight().size() > index) {
						poolWeight = cliParser.getPoolsWeight().get(index);
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
					pool.setWeight(poolWeight);
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
		if (logDirectory == null) {
			System.err.println("Log directory not set. Use the tmp OS directory.");
			logDirectory = new File(System.getProperty("java.io.tmpdir"));
		} else {
			if (!logDirectory.exists()) {
				System.err.println("Log directory " + logDirectory.getAbsolutePath() + "does not exist. Create it.");
				boolean isCreated = logDirectory.mkdirs();
				if (!isCreated) {
					System.err.println("Failed to create the log directory " + logDirectory.getAbsolutePath() + ". Use the tmp OS directory.");
					logDirectory = new File(System.getProperty("java.io.tmpdir"));
				}
			}
		}

		System.out.println("Use log directory " + logDirectory.getAbsolutePath());

		// Set the directory used for logging.
		System.setProperty("log.directory.path", logDirectory.getAbsolutePath());

		// If disableLogAppend is true, do not use the rollingFileAppender
		// but a simple fileAppender with append to false.
		if (disableLogAppend) {
			RollingFileAppender rollingFileAppender = (RollingFileAppender) LogManager.getRootLogger().getAppender("file");
			LogManager.getRootLogger().removeAppender(rollingFileAppender);
			try {
				FileAppender fileAppender = new FileAppender(rollingFileAppender.getLayout(), rollingFileAppender.getFile(), false);
				fileAppender.setName(rollingFileAppender.getName());
				LogManager.getRootLogger().addAppender(fileAppender);
			} catch (IOException e) {
				System.out.println("Failed to create the log appender with append=false.");
				e.printStackTrace();
			}
		}

		// Set the log level.
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

		// Set the API log level
		if (apiLogLevel == null) {
			logger.info("API log level not set. API logging disabled.");
		} else {
			if (apiLogLevel == Level.OFF) {
				logger.info("API log level set to OFF. API logging disabled.");
			} else {
				logger.info("API log level set to {}.", apiLogLevel.toString());
				// Remove existing handlers attached to j.u.l root logger
				SLF4JBridgeHandler.removeHandlersForRootLogger();

				// Add SLF4JBridgeHandler to j.u.l's root logger
				SLF4JBridgeHandler.install();

				// Set the log level of the log4j logger to match the log level
				// of the JUL logger.
				LogManager.getLogger("org.glassfish").setLevel(apiLogLevel);

				java.util.logging.LogManager.getLogManager().getLogger("").setLevel(getJULLevelFromLOG4JLevel(apiLogLevel));
			}

		}

	}

	/**
	 * Check that the database directory is set and exists.. If not set, use the
	 * default directory. If does not exist, create it.
	 * 
	 * @throws FileNotFoundException
	 *             if the database directory cannot be created.
	 */
	private void initDatabaseDirectory() throws FileNotFoundException {
		// When database directory is not given by the user.
		if (databaseDirectory == null) {
			databaseDirectory = new File(getInstallDirectory(), "database");

			logger.info("Database directory not specified. Using default one: {}.", databaseDirectory.getAbsolutePath());

			if (!databaseDirectory.exists()) {
				logger.info("Default database directory does not exist. Create directory: {}.", databaseDirectory.getAbsolutePath());
				if (!databaseDirectory.mkdirs()) {
					throw new FileNotFoundException("Failed to create the directory " + databaseDirectory.getAbsolutePath());
				}
			}
		} else {
			logger.info("Using database directory: {}.", databaseDirectory.getAbsolutePath());
			if (!databaseDirectory.exists()) {
				logger.info("Database directory does not exist. Create directory: {}.", databaseDirectory.getAbsolutePath());
				if (!databaseDirectory.mkdirs()) {
					throw new FileNotFoundException("Failed to create the directory " + databaseDirectory.getAbsolutePath());
				}
			}
		}
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
					version = "Unknown";
				}
			}
		}

		return version;
	}

	/**
	 * Return the installation directory of the application.
	 */
	public static final String getInstallDirectory() {
		return getJarFile().getParent();
	}

	/**
	 * Return the path of the application jar file.
	 * 
	 * @return
	 */
	public static final String getJarFilePath() {
		return getJarFile().getAbsolutePath();
	}

	/**
	 * Return the jar file.
	 * 
	 * @return
	 */
	private static File getJarFile() {
		String path = ConfigurationManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = null;
		try {
			decodedPath = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			String errorMessage = "Failed to get the installation directory.";
			if (logger != null) {
				logger.error(errorMessage, e);
			} else {
				System.err.println(e);
				e.printStackTrace();
			}
		}
		return new File(decodedPath);
	}

	/**
	 * Return the Java Util Logging level from the Log4J level.
	 * 
	 * @param level
	 * @return
	 */
	private static java.util.logging.Level getJULLevelFromLOG4JLevel(Level level) {
		java.util.logging.Level result = java.util.logging.Level.INFO;
		if (Level.ALL == level) {
			result = java.util.logging.Level.ALL;
		} else if (Level.FATAL == level) {
			result = java.util.logging.Level.SEVERE;
		} else if (Level.ERROR == level) {
			result = java.util.logging.Level.SEVERE;
		} else if (Level.WARN == level) {
			result = java.util.logging.Level.WARNING;
		} else if (Level.INFO == level) {
			result = java.util.logging.Level.INFO;
		} else if (Level.DEBUG == level) {
			result = java.util.logging.Level.FINE;
		} else if (Level.TRACE == level) {
			result = java.util.logging.Level.FINEST;
		} else if (Level.OFF == level) {
			result = java.util.logging.Level.OFF;
		}
		return result;
	}

	/**
	 * Return true only if the application is running from the jar file.
	 * 
	 * @return
	 */
	public static boolean isRunningFromJar() {
		String className = ConfigurationManager.class.getName().replace('.', '/');
		String classJar = ConfigurationManager.class.getResource("/" + className + ".class").toString();
		return classJar.startsWith("jar:");
	}

	public File getDatabaseDirectory() {
		return databaseDirectory;
	}

	public Integer getHashrateDatabaseSamplingPeriod() {
		return hashrateDatabaseSamplingPeriod;
	}

	public Integer getHashrateDatabaseHistoryDepth() {
		return hashrateDatabaseHistoryDepth;
	}

	public boolean isNoMidsate() {
		return noMidsate;
	}

	public boolean isValidateSha26GetworkShares() {
		return validateSha26GetworkShares;
	}

	public String getPoolSwitchingStrategy() {
		return poolSwitchingStrategy;
	}

	public Integer getWeightedRoundRobinRoundDuration() {
		return weightedRoundRobinRoundDuration;
	}

	public boolean isDisableGetwork() {
		return disableGetwork;
	}

	public boolean isDisableStratum() {
		return disableStratum;
	}

	public boolean isDisableApi() {
		return disableApi;
	}

	public boolean isDisableLogAppend() {
		return disableLogAppend;
	}

	public String getApiUser() {
		return apiUser;
	}

	public String getApiPassword() {
		return apiPassword;
	}

}
