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
			task.setExpectedExecutionTime(System.currentTimeMillis() + delay);
			// Wake up the scheduler.
			synchronized (waitingTasks) {
				waitingTasks.add(task);
				waitingTasks.notifyAll();
			}
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
							}

							waitingTasks.wait(timeToWait);

						} else {
							nextTask = waitingTasks.pollFirst();

							// Run the task only if it is not cancelled.
							if (!nextTask.isCancelled()) {
								executor.execute(nextTask);
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

		public void cancel() {
			isCancelled = true;
			synchronized (Timer.getInstance().waitingTasks) {
				Timer.getInstance().waitingTasks.remove(this);
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

	}

}
