/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014-2015  Stratehm (stratehm@hotmail.com)
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
package strat.mining.stratum.proxy.manager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.database.DatabaseManager;
import strat.mining.stratum.proxy.database.model.HashrateModel;
import strat.mining.stratum.proxy.model.User;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.utils.Timer.Task;

/**
 * Manage the save of pools and users hashrate history in the database.
 * 
 * @author Strat
 * 
 */
public class HashrateRecorder {

	private static final Logger LOGGER = LoggerFactory.getLogger(HashrateRecorder.class);

	private static HashrateRecorder instance;

	private static Integer CAPTURE_PERIOD = ConfigurationManager.getInstance().getHashrateDatabaseSamplingPeriod() * 1000;
	private static Long OLD_RECORDS_OFFSET = ConfigurationManager.getInstance().getHashrateDatabaseHistoryDepth() * 86400L * 1000;

	private ProxyManager stratumProxyManager;
	private DatabaseManager databaseManager;

	private Task captureTask;

	private HashrateRecorder() {
		stratumProxyManager = ProxyManager.getInstance();
		databaseManager = DatabaseManager.getInstance();
	}

	public static HashrateRecorder getInstance() {
		if (instance == null) {
			instance = new HashrateRecorder();
		}
		return instance;
	}

	/**
	 * Start to capture the hashrate of users and pools periodicly.
	 */
	public void startCapture() {
		stopCapture();
		scheduleTask();
	}

	/**
	 * Schedule the capture task.
	 */
	private void scheduleTask() {
		LOGGER.debug("Scheduling the hashrate capture task in {} ms.", CAPTURE_PERIOD);
		captureTask = new Task() {
			public void run() {
				try {
					capture();
				} catch (Exception e) {
					LOGGER.error("Error during hashrate capture.", e);
				}
				scheduleTask();
			}
		};
		captureTask.setName("HashrateRecoderTask");
		Timer.getInstance().schedule(captureTask, CAPTURE_PERIOD);
	}

	/**
	 * Stop the capture task.
	 */
	public void stopCapture() {
		if (captureTask != null) {
			LOGGER.debug("Stopping hashrate capture.");
			captureTask.cancel();
			captureTask = null;
		}
	}

	/**
	 * Capture the hashrate of users and pools and store them in database.
	 */
	private void capture() {
		Long captureTime = System.currentTimeMillis();
		Long toDeleteBefore = captureTime - OLD_RECORDS_OFFSET;
		List<Pool> pools = stratumProxyManager.getPools();
		if (pools != null) {
			LOGGER.debug("Capturing pools hashrate.");
			for (Pool pool : pools) {
				HashrateModel hashrate = new HashrateModel();
				hashrate.setName(pool.getHost());
				hashrate.setAcceptedHashrate(Double.valueOf(pool.getAcceptedHashesPerSeconds()).longValue());
				hashrate.setRejectedHashrate(Double.valueOf(pool.getRejectedHashesPerSeconds()).longValue());
				hashrate.setCaptureTime(captureTime);

				databaseManager.insertPoolHashrate(hashrate);
			}
		}

		List<User> users = stratumProxyManager.getUsers();
		if (users != null) {
			LOGGER.debug("Capturing users hashrate.");
			for (User user : users) {
				HashrateModel hashrate = new HashrateModel();
				hashrate.setName(user.getName());
				hashrate.setAcceptedHashrate(Double.valueOf(user.getAcceptedHashrate()).longValue());
				hashrate.setRejectedHashrate(Double.valueOf(user.getRejectedHashrate()).longValue());
				hashrate.setCaptureTime(captureTime);

				databaseManager.insertUserHashrate(hashrate);
			}
		}

		LOGGER.debug("Delete old pools hashrate records.");
		databaseManager.deleteOldPoolsHashrate(toDeleteBefore);

		LOGGER.debug("Delete old users hashrate records.");
		databaseManager.deleteOldUsersHashrate(toDeleteBefore);
	}

}
