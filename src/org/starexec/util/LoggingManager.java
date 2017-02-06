package org.starexec.util;

import java.util.List;

import org.starexec.logger.StarLevel;
import org.starexec.logger.StarLogger;

public class LoggingManager {
	private static final StarLogger log = StarLogger.getLogger(LoggingManager.class);

	public static void setLoggingLevel(StarLevel level) {
		StarLogger.getRootLogger().setLevel(level);
		
	}
	
	public static boolean setLoggingLevelForClass(StarLevel level, String className) {
		
		if (!loggerExists(className)) {
			return false;
		}
		StarLogger.getLogger(className).setLevel(level);
		return true;
	}
	
	public static boolean loggerExists(String className) {
		 List<StarLogger> logs=StarLogger.getCurrentLoggers();
		 
		 for (StarLogger log : logs) {
			 log.debug("found this logger = "+log.getName());
			 if (log.getName().equals(className)) {
				 return true;
			 }
		 }
		 return false;
	}
	
	
}
