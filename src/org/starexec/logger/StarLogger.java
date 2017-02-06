package org.starexec.logger;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class StarLogger {
    private Logger log;
    private static final String methodSeparator = " - ";

    private StarLogger(Class clazz) {
        log = Logger.getLogger(clazz);
    }
    StarLogger(Logger log) {
        this.log = log;
    }

    public static StarLogger getLogger(Class clazz) {
        return new StarLogger(clazz);
    }
    public static StarLogger getLogger(String name) {
        return new StarLogger(Logger.getLogger(name));
    }

    public static List<StarLogger> getCurrentLoggers() {
        Enumeration loggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
        List<StarLogger> currentLoggers = new ArrayList<>();
        while (loggers.hasMoreElements()) {
            currentLoggers.add(new StarLogger((Logger)loggers.nextElement()));
        }
        return currentLoggers;
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

    public static StarLogger getRootLogger() {
        return new StarLogger(Logger.getRootLogger());
    }


    public void logDebugStopWatch(String method, String message, StopWatch stopWatch) {
        log.debug(prefix(method)+message+stopWatch.toString());
    }

    public void entry(String method) {
        log.debug(prefix(method) + "Entering method " + method + ".");
    }

    public void exit(String method) {
        log.debug(prefix(method)+"Leaving method "+method+".");
    }

    public void debug(String method, String message) {
        log.debug(prefix(method)+message);
    }

    public void debug(Object message, Throwable t) {
        log.debug(message, t);
    }

    public void debug(Object message) {
        log.debug(message);
    }

    public void info(String method, String message) {
        log.info(prefix(method)+message);
    }
    public void info(Object message) {
        log.debug(message);
    }
    public void info(Object message, Throwable t) {
        log.info(message, t);
    }

    public void trace(String method, String message) {
        log.trace(prefix(method)+message);
    }
    public void trace(Object message) {
        log.trace(message);
    }
    public void trace(Object message, Throwable t) {
        log.trace(message, t);
    }

    public void warn(String method, String message) {
        log.warn(prefix(method)+message);
    }
    public void warn(Object message) {
        log.warn(message);
    }

    public void warn(String method, String message, Throwable t) {
        log.warn(prefix(method)+message, t);
    }
    public void warn(Object message, Throwable t) {
        log.warn(message, t);
    }

    public void error(String method, String message) {
        log.error(prefix(method)+message);
    }
    public void error(Object message) {
        log.error(message);
    }

    public void error(String method, String message, Throwable t) {
        log.error(prefix(method)+message, t);
    }
    public void error(Object message, Throwable t) {
        log.error(message, t);
    }

    public void fatal(String method, String message) {
        log.fatal(prefix(method)+message);
    }
    public void fatal(Object message) {
        log.fatal(message);
    }
    public void fatal(Object message, Throwable t) {
        log.fatal(message, t);
    }

    /**
     * Builds the prefix that appears at the beginning of each message.
     */
    private static String prefix(String method) {
        return method+methodSeparator;
    }
}
