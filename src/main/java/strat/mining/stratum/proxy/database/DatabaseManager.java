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
package strat.mining.stratum.proxy.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.database.model.HashrateModel;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.query.Predicate;

public class DatabaseManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

	private static DatabaseManager instance;

	private ObjectContainer poolDatabase;
	private ObjectContainer userDatabase;

	private DatabaseManager() throws FileNotFoundException {
		LOGGER.info("Starting DatabaseManager...");
		File databaseDirectory = new File(ConfigurationManager.getInstance().getDatabaseDirectory().getAbsolutePath());
		if (!databaseDirectory.exists()) {
			LOGGER.debug("Creating the folder {}.", databaseDirectory.getAbsoluteFile());
			if (!databaseDirectory.getParentFile().mkdirs()) {
				throw new FileNotFoundException("Failed to create the databse directory " + databaseDirectory.getAbsolutePath());
			}
		}

		// Remove the old neodatis files if they exist.
		removeNeodatisFiles(databaseDirectory);

		File poolDatabaseFile = new File(databaseDirectory, "dbpools");
		File userDatabaseFile = new File(databaseDirectory, "dbusers");

		poolDatabase = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), poolDatabaseFile.getAbsolutePath());
		userDatabase = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), userDatabaseFile.getAbsolutePath());

		LOGGER.info("DatabaseManager started.");
	}

	/**
	 * This method is synchronized to avoid getting an instance which is still
	 * in creation. (The first databases creation may be long)
	 * 
	 * @return
	 */
	public static DatabaseManager getInstance() {
		if (instance == null) {
			try {
				instance = new DatabaseManager();
			} catch (FileNotFoundException e) {
				LOGGER.error("Failed to initialize the DatabaseManager. Application will exit.", e);
				System.exit(1);
			}
		}
		return instance;
	}

	/**
	 * Close the database manager.
	 */
	public static void close() {
		if (instance != null) {
			LOGGER.info("Close databases.");
			instance.closeAllDBs();
			instance = null;
		}
	}

	/**
	 * Close all the databases
	 */
	protected void closeAllDBs() {
		poolDatabase.close();
		userDatabase.close();
	}

	/**
	 * Return all hashrate records for the given poolHost. Return null if the
	 * poolHost does not exist. Return an empty list if no records are found.
	 * 
	 * @param poolHost
	 * @return
	 */
	public List<HashrateModel> getPoolHashrate(final String poolHost) {
		return poolDatabase.query(new Predicate<HashrateModel>() {
			public boolean match(HashrateModel hashrateModel) {
				return hashrateModel.getName().equals(poolHost);
			}
		});
	}

	/**
	 * Return all hashrate records for the given username. Return null if the
	 * username does not exist. Return an empty list if no records are found.
	 * 
	 * @param poolHost
	 * @return
	 */
	public List<HashrateModel> getUserHashrate(final String username) {
		return userDatabase.query(new Predicate<HashrateModel>() {
			public boolean match(HashrateModel hashrateModel) {
				return hashrateModel.getName().equals(username);
			}
		});
	}

	/**
	 * Insert the given hashrate for the given poolHost
	 * 
	 * @param poolHost
	 * @param model
	 */
	public void insertPoolHashrate(HashrateModel model) {
		poolDatabase.store(model);
		poolDatabase.commit();
	}

	/**
	 * Insert the given hashrate for the given poolHost
	 * 
	 * @param username
	 * @param model
	 */
	public void insertUserHashrate(HashrateModel model) {
		userDatabase.store(model);
		userDatabase.commit();
	}

	/**
	 * Delete all pools hashrate records which are older than the given date.
	 * 
	 * @param username
	 * @param olderThan
	 */
	public void deleteOldPoolsHashrate(final Long olderThan) {
		List<HashrateModel> hashrates = poolDatabase.query(new Predicate<HashrateModel>() {
			public boolean match(HashrateModel hashrateModel) {
				return hashrateModel.getCaptureTime() < olderThan;
			}
		});
		for (HashrateModel model : hashrates) {
			poolDatabase.delete(model);
		}

		poolDatabase.commit();
	}

	/**
	 * Delete all users hashrate records which are older than the given date.
	 * 
	 * @param username
	 * @param olderThan
	 */
	public void deleteOldUsersHashrate(final Long olderThan) {
		List<HashrateModel> hashrates = userDatabase.query(new Predicate<HashrateModel>() {
			public boolean match(HashrateModel hashrateModel) {
				return hashrateModel.getCaptureTime() < olderThan;
			}
		});
		for (HashrateModel model : hashrates) {
			userDatabase.delete(model);
		}

		userDatabase.commit();
	}

	/**
	 * Remove the old Neodatis database files to avoid conflict with db4o
	 * 
	 * @param databaseDirectory
	 */
	private void removeNeodatisFiles(File databaseDirectory) {
		File poolDatabaseFile = new File(databaseDirectory, "pools");
		File userDatabaseFile = new File(databaseDirectory, "users");

		if (poolDatabaseFile.exists() && poolDatabaseFile.isFile()) {
			boolean isRemoved = poolDatabaseFile.delete();
			if (isRemoved) {
				LOGGER.info("Old Neodatis pools database file {} removed.", poolDatabaseFile.getAbsolutePath());
			} else {
				LOGGER.warn("Failed to remove the old Neodatis pools database file {}. You can remove it manually.",
						poolDatabaseFile.getAbsolutePath());
			}
		}

		if (userDatabaseFile.exists() && userDatabaseFile.isFile()) {
			boolean isRemoved = userDatabaseFile.delete();
			if (isRemoved) {
				LOGGER.info("Old Neodatis users database file {} removed.", userDatabaseFile.getAbsolutePath());
			} else {
				LOGGER.warn("Failed to remove the old Neodatis users database file {}. You can remove it manually.",
						userDatabaseFile.getAbsolutePath());
			}
		}
	}

}
