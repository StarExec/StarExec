package org.starexec.logger;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by agieg on 2/10/2017.
 */
public abstract class BaseStarLogger {
    protected final Logger log;
    protected final String name;
    private static final String methodSeparator = " - ";

    protected BaseStarLogger(Class clazz) {

        log = LoggerFactory.getLogger(clazz);
        this.name = clazz.getName();
    }

    protected BaseStarLogger(String name) {
        log = LoggerFactory.getLogger(name);
        this.name = name;
    }

    protected BaseStarLogger(Logger log) {
        this.log = log;
        this.name = log.getName();
    }

    public static void turnOffLogging() {
	((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.OFF);
    }

    public String getName() {
        return log.getName();
    }

    public void setLevel(StarLevel level) {
        ((ch.qos.logback.classic.Logger)log).setLevel(level.get());
    }

    public void entry(String method) {
        log.debug(prefix(method) + "Entering method " + method + ".");
    }
    public void exit(String method) {
        log.debug(prefix(method)+"Leaving method "+method+".");
    }

    public void debug(final String message) {
        log(StarLevel.DEBUG, null, message, null);
    }
    public void debug(final String method, final String message) {
        log(StarLevel.DEBUG, method, message, null);
    }
    public void debug(final String message, final Throwable t) {
        log(StarLevel.DEBUG, null, message, t);
    }
    public void debug(final String method, final String message, final Throwable t) {
        log(StarLevel.DEBUG, method, message, t);
    }

    public void info(final String method, final String message) {
        log(StarLevel.INFO, method, message, null);
    }
    public void info(final String message) {
        log(StarLevel.INFO, null, message, null);
    }
    public void info(final String message, final Throwable t) {
        log(StarLevel.INFO, null, message, t);
    }
    public void info(final String method, final String message, final Throwable t) {
        log(StarLevel.INFO, method, message, t);
    }

    public void trace(final String method, final String message) {
        log(StarLevel.TRACE, method, message, null);
    }
    public void trace(final String message) {
        log(StarLevel.TRACE, null, message, null);
    }
    public void trace(final String message, final Throwable t) {
        log(StarLevel.TRACE, null, message, t);
    }
    public void trace(final String method, final String message, final Throwable t) {
        log(StarLevel.TRACE, method, message, t);
    }


    public void warn(final String method, final String message) {
        log(StarLevel.WARN, method, message, null);
    }
    public void warn(final String message) {
        log(StarLevel.WARN, null, message, null);
    }
    public void warn(final String method, final String message, final Throwable t) {
        log(StarLevel.WARN, method, message, t);
    }
    public void warn(final String message, final Throwable t) {
        log(StarLevel.WARN, null, message, t);
    }

    public void error(final String method, final String message) {
        log(StarLevel.ERROR, method, message, null);
    }
    public void error(final String message) {
        log(StarLevel.ERROR, null, message, null);
    }
    public void error(final String method, final String message, final Throwable t) {
        log(StarLevel.ERROR, method, message, t);
    }
    public void error(final String message, final Throwable t) {
        log(StarLevel.ERROR, null, message, t);
    }

    public void fatal(final String method, final String message, final Throwable t) {
        log(StarLevel.ERROR, method, message, t);
    }
    public void fatal(final String method, final String message) {
        log(StarLevel.ERROR, method, message, null);
    }
    public void fatal(final String message) {
        log(StarLevel.ERROR, null, message, null);
    }
    public void fatal(final String message, final Throwable t) {
        log(StarLevel.ERROR, null, message, t);
    }

    protected void sendToLogger(StarLevel level, String message) {
	switch (level) {
	case ERROR:
	    log.error(message);
	    break;
	case WARN:
	    log.warn(message);
	    break;
	case INFO:
	    log.info(message);
	    break;
	case DEBUG:
	    log.debug(message);
	    break;
	case TRACE:
	    log.trace(message);
	    break;
	}
   }
    protected String getMessage(final String method, final String message, final Throwable t) {
	return (method == null ? message : prefix(method)+message + (t == null ? "" : t.toString()));
    }

    protected abstract void log(StarLevel level, String method, String message, Throwable t);

    /**
     * Builds the prefix that appears at the beginning of each message.
     */
    protected static String prefix(String method) {
        return method+methodSeparator;
    }
}
