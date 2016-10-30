package org.starexec.util;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LogUtil {
	private Logger log;

	private static final String METHOD_SEPERATOR = " - ";

	public LogUtil(Logger log) {
		this.log = log;
	}

	public void logStopWatch(Level level, String method, String message, StopWatch stopWatch) {
		log.log(level, prefix(method)+message+stopWatch.toString());
	}

	public void entry(String method) {
		log.debug(prefix(method)+"Entering method "+method+".");
	}

	public void exit(String method) {
		log.debug(prefix(method)+"Leaving method "+method+".");
	}

	public void debug(String method, String message) {
		log.debug(prefix(method)+message);
	}

	public void info(String method, String message) {
		log.info(prefix(method)+message);
	}

	public void trace(String method, String message) {
		log.trace(prefix(method)+message);
	}

	public void warn(String method, String message) {
		log.warn(prefix(method)+message);
	}

	public void warn(String method, String message, Throwable t) {
		log.warn(prefix(method)+message, t);
	}

	public void error(String method, String message) {
		log.error(prefix(method)+message);
	}

	public void error(String method, String message, Throwable t) {
		log.error(prefix(method)+message, t);
	}

	public <T extends Throwable> void logAndThrow(String method, String message, T t) throws T {
		error(method, "Caught and throwing " + t.getClass().getSimpleName() + ": " + message, t);
	}

	public <T extends Throwable> void logAndThrow(String method, T t) throws T {
		logAndThrow(method, "", t);
	}

		public <T extends Throwable> void logException(String method, String message, T t) {
		warn(method, "Caught " + t.getClass().getSimpleName() + ": " + message, t);
	}

	public <T extends Throwable> void logException(String method, T t) {
		logException(method, "", t);
	}

	public void fatal(String method, String message) {
		log.fatal(prefix(method)+message);
	}

	/**
	 * Builds the prefix that appears at the beginning of each message.
	 */
	private static String prefix(String method) {
		return method+METHOD_SEPERATOR;
	}

}
