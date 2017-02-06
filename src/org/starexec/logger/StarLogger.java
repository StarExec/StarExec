package org.starexec.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.starexec.data.database.Reports;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class StarLogger {
    private Logger log;
    private static final String methodSeparator = " - ";

    private StarLogger(Class clazz) {
        log = Logger.getLogger(clazz);
    }
    private StarLogger(Logger log) {
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
        log(StarLevel.FATAL, method, message, t);
    }
    public void fatal(final String method, final String message) {
        log(StarLevel.FATAL, method, message, null);
    }
    public void fatal(final String message) {
        log(StarLevel.FATAL, null, message, null);
    }
    public void fatal(final String message, final Throwable t) {
        log(StarLevel.FATAL, null, message, t);
    }

    private void log(StarLevel level, final String method, final String message, final Throwable t) {
        final String prefixedMessage = method == null ? message : prefix(method)+message;

        if (t == null) {
            log.log(level.get(), prefixedMessage);
        } else {
            log.log(level.get(), prefixedMessage, t);
        }

        if (level == StarLevel.ERROR || level == StarLevel.FATAL || level == StarLevel.WARN) {
            reportError(level, prefixedMessage);
        }
    }

    private void reportError(final StarLevel level, final String message) {
        reportError(level, message, null);
    }

    private void reportError(final StarLevel level, final String message, final Throwable t) {
        String messageAndTrace = message.toString();
        if (t != null) {
            messageAndTrace += "\nStack Trace:\n" + Util.getStackTrace(t);
        }
        try {
            Reports.addErrorReport(messageAndTrace, level);
        } catch (SQLException e) {
            log.error("Failed to generate error report due to SQLException!", e);
        }
    }


    /**
     * Builds the prefix that appears at the beginning of each message.
     */
    private static String prefix(String method) {
        return method+methodSeparator;
    }
}
