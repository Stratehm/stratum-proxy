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
package strat.mining.stratum.proxy.utils;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Timer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Timer.class);

	private static Timer instance;

	private ExecutorService executor;

	private NavigableSet<Task> waitingTasks;

	private Scheduler scheduler;

	private Timer() {
		waitingTasks = new TreeSet<Task>(new Comparator<Task>() {
			public int compare(Task o1, Task o2) {
				return o1.getExpectedExecutionTime().compareTo(o2.getExpectedExecutionTime());
			}
		});
		scheduler = new Scheduler();
		executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("TimerExecutorThread-%s").setDaemon(true).build());

		Thread timerThread = new Thread(scheduler, "TimerSchedulerThread");
		timerThread.setDaemon(true);
		timerThread.start();
	}

	public static Timer getInstance() {
		if (instance == null) {
			instance = new Timer();
		}
		return instance;
	}

	/**
	 * Schedule the given task to execute in delay milliseconds.
	 * 
	 * @param task
	 * @param delay
	 */
	public void schedule(Task task, long delay) {
		// Check that the task is not null and delay is valid.
		if (task != null && delay >= 0) {
			LOGGER.debug("Scheduling of task {} in {} ms.", task.getName(), delay);
			task.setExpectedExecutionTime(System.currentTimeMillis() + delay);
			LOGGER.trace("Expected execution time of task {}: {}.", task.getName(), task.getExpectedExecutionTime());
			// Wake up the scheduler.
			synchronized (waitingTasks) {
				waitingTasks.add(task);
				LOGGER.trace("Task added => Waking up the scheduler.", task.getName(), task.getExpectedExecutionTime());
				waitingTasks.notifyAll();
			}
		} else {
			LOGGER.info("Failed to schedule task {} in {} ms.", task != null ? task.getName() : "null", delay);
		}
	}

	/**
	 * The scheduler of the timer.
	 * 
	 * @author Strat
	 * 
	 */
	protected class Scheduler implements Runnable {

		public void run() {
			while (true) {
				Task nextTask = null;
				long currentTime = System.currentTimeMillis();
				try {
					synchronized (waitingTasks) {
						LOGGER.trace("Looking for next task to execute: {}", waitingTasks);
						try {
							nextTask = waitingTasks.first();
						} catch (NoSuchElementException e) {
						}

						if (nextTask == null || nextTask.getExpectedExecutionTime() > currentTime) {
							// Wait for a new task add if no task is present
							// or wait for the delay before the execution of the
							// next task.
							long timeToWait = 500;
							if (nextTask != null) {
								timeToWait = nextTask.getExpectedExecutionTime() - currentTime;
								LOGGER.trace("Next task to execute {}: waiting for {} ms.", timeToWait);
							} else {
								LOGGER.trace("No task in the queue. Waiting for {} ms.", timeToWait);
							}

							waitingTasks.wait(timeToWait);

						} else {
							nextTask = waitingTasks.pollFirst();

							LOGGER.trace("Task to execute now: {}.", nextTask.getName());

							// Run the task only if it is not cancelled.
							if (!nextTask.isCancelled()) {
								LOGGER.trace("Executing task {} now.", nextTask.getName());
								executor.execute(nextTask);
							} else {
								LOGGER.trace("Task {} cancelled. Do not execute.", nextTask.getName());
							}
						}
					}

				} catch (InterruptedException e) {
				} catch (Exception e) {
					LOGGER.error("Unexpected error in TimerSchedulerThread", e);
				}

			}
		}
	}

	/**
	 * A task that can be run by the {@link Timer}
	 * 
	 * @author Strat
	 * 
	 */
	public static abstract class Task implements Runnable {

		volatile boolean isCancelled = false;

		private Long expectedExecutionTime;

		private String name;

		public void cancel() {
			LOGGER.debug("Cancelling the task {}.", getName());
			isCancelled = true;
			synchronized (Timer.getInstance().waitingTasks) {
				Timer.getInstance().waitingTasks.remove(this);
				LOGGER.debug("Task {} removed.", getName());
			}
		}

		public boolean isCancelled() {
			return isCancelled;
		}

		public Long getExpectedExecutionTime() {
			return expectedExecutionTime;
		}

		public void setExpectedExecutionTime(Long expectedExecutionTime) {
			this.expectedExecutionTime = expectedExecutionTime;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
