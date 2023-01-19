package org.starexec.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starexec.data.database.ErrorLogs;
import org.starexec.util.Util;

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

    public static StarLogger getRootLogger() {
        return new StarLogger(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));
    }

    @Override
    protected void log(StarLevel level, final String method, final String message, final Throwable t) {
	String msg = getMessage(method,message,t);
	sendToLogger(level, msg);

        if (level == StarLevel.ERROR || level == StarLevel.WARN) {
            reportError(level, msg, t);
        }
    }

    private void reportError(final StarLevel level, final String message, final Throwable t) {
		String messageAndTrace = "";
		if (message != null) {
			messageAndTrace = message;
		}
		// Add the logger name.
		messageAndTrace = "("+log.getName()+") - " + messageAndTrace;
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
