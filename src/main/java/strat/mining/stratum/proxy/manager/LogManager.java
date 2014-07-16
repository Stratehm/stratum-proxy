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
package strat.mining.stratum.proxy.manager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import com.google.common.collect.Maps;

/**
 * Manages the log of the application.
 * 
 * @author Strat
 * 
 */
public class LogManager extends AppenderSkeleton {

	private static LogManager instance;

	private int historyDepth = 1000;

	private NavigableMap<Long, String> logEntries;

	private LogAppenderThread appenderThread;

	public static LogManager getInstance() {
		if (instance == null) {
			instance = new LogManager();
		}
		return instance;
	}

	public LogManager() {
		// initialize the instance variable here since log4j will instantiate
		// this manager (even if constructor is private)
		instance = this;
		logEntries = Maps.synchronizedNavigableMap(new TreeMap<Long, String>());
		appenderThread = new LogAppenderThread();
		appenderThread.setName("LogAppenderThread");
		appenderThread.start();
	}

	@Override
	public void close() {
		appenderThread.interrupt();
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	@Override
	protected void append(LoggingEvent event) {
		appenderThread.appendLog(event);
	}

	/**
	 * Return the list of log lines since the given timestamp (exclusive).
	 * 
	 * @param timestamp
	 * @return
	 */
	public List<Map.Entry<Long, String>> getLogSince(Long timestamp) {
		List<Map.Entry<Long, String>> result = new ArrayList<>();
		NavigableMap<Long, String> tailMap = logEntries;
		if (timestamp != null) {
			tailMap = logEntries.tailMap(timestamp, false);
		}

		// Synchronize on logEntries to avoid event add during the tail map
		// iteration (tail map is backported by the original map)
		synchronized (logEntries) {
			for (Entry<Long, String> entry : tailMap.entrySet()) {
				result.add(entry);
			}
		}
		return result;
	}

	/**
	 * A thread that will append the logs in the manager history. A thread is
	 * used to avoid blocking log4j.
	 * 
	 * @author Strat
	 * 
	 */
	private class LogAppenderThread extends Thread {

		private Deque<LoggingEvent> eventQueue;

		public LogAppenderThread() {
			eventQueue = new ConcurrentLinkedDeque<>();
		}

		@Override
		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					LoggingEvent event = eventQueue.poll();
					// If the queue is not empty, add the event.
					if (event != null) {
						String formattedLogLine = getLayout().format(event);

						// Remove the first log entry if the size of the map is
						// too
						// large.
						if (logEntries.size() >= historyDepth && logEntries.size() > 0) {
							logEntries.remove(logEntries.firstKey());
						}

						logEntries.put(event.getTimeStamp(), formattedLogLine);
					} else {
						// Else wait for an event.
						synchronized (eventQueue) {
							eventQueue.wait();
						}
					}
				}
			} catch (InterruptedException e) {
			}

		}

		/**
		 * Append a log event to the thread that will insert it in the manager
		 * history.
		 * 
		 * @param event
		 */
		public void appendLog(LoggingEvent event) {
			eventQueue.add(event);
			synchronized (eventQueue) {
				eventQueue.notifyAll();
			}
		}
	}

}
