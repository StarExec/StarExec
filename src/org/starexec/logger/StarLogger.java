package org.starexec.logger;

import org.apache.log4j.Logger;
import org.starexec.data.database.ErrorLogs;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

public class StarLogger extends BaseStarLogger {

    private StarLogger(Class clazz) {
        super(clazz);
    }
    private StarLogger(String name) {
        super(name);
    }

    private StarLogger(Logger logger) {
        super(logger);
    }

    public static StarLogger getLogger(Class clazz) {
        return new StarLogger(clazz);
    }
    public static StarLogger getLogger(String name) {
        return new StarLogger(name);
    }

    public static List<StarLogger> getCurrentLoggers() {
        Enumeration loggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
        List<StarLogger> currentLoggers = new ArrayList<>();
        while (loggers.hasMoreElements()) {
            currentLoggers.add(new StarLogger((Logger)loggers.nextElement()));
        }
        return currentLoggers;
    }

    public static StarLogger getRootLogger() {
        return new StarLogger(Logger.getRootLogger());
    }


    @Override
    protected void log(StarLevel level, final String method, final String message, final Throwable t) {
        final String prefixedMessage = method == null ? message : prefix(method)+message;

		log.log(level.get(), "StarLogger log method called.");

        if (t == null) {
            log.log(level.get(), prefixedMessage);
        } else {
            log.log(level.get(), prefixedMessage, t);
        }


        if (level == StarLevel.ERROR || level == StarLevel.FATAL || level == StarLevel.WARN) {
            reportError(level, prefixedMessage, t);
        }
    }

    private void reportError(final StarLevel level, final String message) {
        reportError(level, message, null);
    }

    private void reportError(final StarLevel level, final String message, final Throwable t) {
		String messageAndTrace = "";
		if (message != null) {
			messageAndTrace = message;
		}
		// Add the logger name.
		messageAndTrace += "("+log.getName()+") - " + messageAndTrace;
		if (t != null) {
			messageAndTrace += "\nStack Trace:\n" + Util.getStackTrace(t);
		}

		
		
		Optional<Integer> logId = ErrorLogs.add(messageAndTrace, level);
		if (!logId.isPresent()) {
			// This log is just a basic log4j logger.
			log.error("Failed to add error message to logs.");
		}
    }



}
