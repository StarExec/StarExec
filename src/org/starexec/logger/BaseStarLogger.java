package org.starexec.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by agieg on 2/10/2017.
 */
public abstract class BaseStarLogger {
    protected final Logger log;
    private static final String methodSeparator = " - ";

    protected BaseStarLogger(Class clazz) {
        log = Logger.getLogger(clazz);
    }

    protected BaseStarLogger(String name) {
        log = Logger.getLogger(name);
    }

    protected BaseStarLogger(Logger log) {
        this.log = log;
    }



    public static void turnOffLogging() {
        Logger.getRootLogger().setLevel(Level.OFF);
    }

    public String getName() {
        return log.getName();
    }


    public void setLevel(StarLevel level) {
        log.setLevel(level.get());
    }


    public void entry(String method) {
        log.debug(prefix(method) + "Entering method " + method + ".");
    }
    public void exit(String method) {
        log.debug(prefix(method)+"Leaving method "+method+".");
    }

    public void debug(final String message) {
        log(StarLevel.DEBUG, null, message, null, true);
    }
    public void debug(final String method, final String message) {
        log(StarLevel.DEBUG, method, message, null, true);
    }
    public void debug(final String message, final Throwable t) {
        log(StarLevel.DEBUG, null, message, t, true);
    }
    public void debug(final String method, final String message, final Throwable t) {
        log(StarLevel.DEBUG, method, message, t, true);
    }

    public void info(final String method, final String message) {
        log(StarLevel.INFO, method, message, null, true);
    }
    public void info(final String message) {
        log(StarLevel.INFO, null, message, null, true);
    }
    public void info(final String message, final Throwable t) {
        log(StarLevel.INFO, null, message, t, true);
    }
    public void info(final String method, final String message, final Throwable t) {
        log(StarLevel.INFO, method, message, t, true);
    }

    public void trace(final String method, final String message) {
        log(StarLevel.TRACE, method, message, null, true);
    }
    public void trace(final String message) {
        log(StarLevel.TRACE, null, message, null, true);
    }
    public void trace(final String message, final Throwable t) {
        log(StarLevel.TRACE, null, message, t, true);
    }
    public void trace(final String method, final String message, final Throwable t) {
        log(StarLevel.TRACE, method, message, t, true);
    }


    public void warn(final String method, final String message) {
        log(StarLevel.WARN, method, message, null, true);
    }
    public void warn(final String message) {
        log(StarLevel.WARN, null, message, null, true);
    }
    public void warn(final String method, final String message, final Throwable t) {
        log(StarLevel.WARN, method, message, t, true);
    }
    public void warn(final String message, final Throwable t) {
        log(StarLevel.WARN, null, message, t, true);
    }

    public void error(final String method, final String message) {
        log(StarLevel.ERROR, method, message, null, true);
    }
    public void error(final String message) {
        log(StarLevel.ERROR, null, message, null, true);
    }
    public void error(final String method, final String message, final Throwable t) {
        log(StarLevel.ERROR, method, message, t, true);
    }
    public void error(final String message, final Throwable t) {
        log(StarLevel.ERROR, null, message, t, true);
    }

    public void fatal(final String method, final String message, final Throwable t) {
        log(StarLevel.FATAL, method, message, t, true);
    }
    public void fatal(final String method, final String message) {
        log(StarLevel.FATAL, method, message, null, true);
    }
    public void fatal(final String message) {
        log(StarLevel.FATAL, null, message, null, true);
    }
    public void fatal(final String message, final Throwable t) {
        log(StarLevel.FATAL, null, message, t, true);
    }

    protected abstract void log(StarLevel level, String method, String message, Throwable t, boolean save);

    /**
     * Builds the prefix that appears at the beginning of each message.
     */
    protected static String prefix(String method) {
        return method+methodSeparator;
    }
}
