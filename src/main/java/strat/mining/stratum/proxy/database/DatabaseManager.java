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
import java.util.ArrayList;
import java.util.List;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.database.model.HashrateModel;

public class DatabaseManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

	private static DatabaseManager instance;

	private ODB poolDatabase;
	private ODB userDatabase;

	private DatabaseManager() throws FileNotFoundException {
		LOGGER.info("Starting DatabaseManager...");
		File databaseDirectory = new File(ConfigurationManager.getInstance().getDatabaseDirectory().getAbsolutePath());
		if (!databaseDirectory.exists()) {
			LOGGER.debug("Creating the folder {}.", databaseDirectory.getAbsoluteFile());
			if (!databaseDirectory.getParentFile().mkdirs()) {
				throw new FileNotFoundException("Failed to create the databse directory " + databaseDirectory.getAbsolutePath());
			}
		}

		File poolDatabaseFile = new File(databaseDirectory, "pools");
		File userDatabaseFile = new File(databaseDirectory, "users");

		poolDatabase = ODBFactory.open(poolDatabaseFile.getAbsolutePath());
		userDatabase = ODBFactory.open(userDatabaseFile.getAbsolutePath());

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
	 * Return all hashrate records for the given poolHost. Return null if the
	 * poolHost does not exist. Return an empty list if no records are found.
	 * 
	 * @param poolHost
	 * @return
	 */
	public List<HashrateModel> getPoolHashrate(String poolHost) {
		IQuery query = new CriteriaQuery(HashrateModel.class, Where.equal("name", poolHost));
		Objects<HashrateModel> hashrates = poolDatabase.getObjects(query);
		List<HashrateModel> result = new ArrayList<>(hashrates);
		return result;
	}

	/**
	 * Return all hashrate records for the given username. Return null if the
	 * username does not exist. Return an empty list if no records are found.
	 * 
	 * @param poolHost
	 * @return
	 */
	public List<HashrateModel> getUserHashrate(String username) {
		IQuery query = new CriteriaQuery(HashrateModel.class, Where.equal("name", username));
		Objects<HashrateModel> hashrates = userDatabase.getObjects(query);
		List<HashrateModel> result = new ArrayList<>(hashrates);
		return result;
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
		poolDatabase.commit();
	}

	/**
	 * Delete all pools hashrate records which are older than the given date.
	 * 
	 * @param username
	 * @param olderThan
	 */
	public void deleteOldPoolsHashrate(Long olderThan) {
		IQuery query = new CriteriaQuery(HashrateModel.class, Where.lt("captureTime", olderThan));
		Objects<HashrateModel> hashrates = poolDatabase.getObjects(query);
		if (hashrates != null) {
			for (HashrateModel model : hashrates) {
				poolDatabase.delete(model);
			}
		}
		poolDatabase.commit();
	}

	/**
	 * Delete all users hashrate records which are older than the given date.
	 * 
	 * @param username
	 * @param olderThan
	 */
	public void deleteOldUsersHashrate(Long olderThan) {
		IQuery query = new CriteriaQuery(HashrateModel.class, Where.lt("captureTime", olderThan));
		Objects<HashrateModel> hashrates = userDatabase.getObjects(query);
		if (hashrates != null) {
			for (HashrateModel model : hashrates) {
				poolDatabase.delete(model);
			}
		}
		poolDatabase.commit();
	}

}
