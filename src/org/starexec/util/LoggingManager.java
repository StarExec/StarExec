package org.starexec.util;

import org.starexec.logger.StarLevel;
import org.starexec.logger.StarLogger;

import java.util.List;

public class LoggingManager {
	private static final StarLogger log = StarLoggerFactory.getLogger(LoggingManager.class);

	public static void setLoggingLevel(StarLevel level) {
		StarLogger.getRootLogger().setLevel(level);
	}

	public static boolean setLoggingLevelForClass(StarLevel level, String className) {
	    Logger logger = StarLogger.getLogger(className);
	    if (logger == null) {
		return false;
	    }
	    logger.setLevel(level);
	    return true;
	}
}
