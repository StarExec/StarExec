package org.starexec.util;

import java.util.Enumeration;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.starexec.data.database.Jobs;
import org.starexec.jobs.JobManager;

public class LoggingManager {
	private static final Logger log = Logger.getLogger(LoggingManager.class);

	public static void setLoggingLevel(Level level) {
		Logger.getRootLogger().setLevel(level);
		
	}
	
	public static boolean setLoggingLevelForClass(Level level, String className) {
		
		if (!loggerExists(className)) {
			return false;
		}
		Logger.getLogger(className).setLevel(level);
		return true;
	}
	
	public static boolean loggerExists(String className) {
		 Enumeration<Logger> logs=Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
		 
		 while (logs.hasMoreElements()) {
			
			 
			 Logger log=logs.nextElement();
			 log.debug("found this logger = "+log.getName());
			 if (log.getName().equals(className)) {
				 return true;
			 }
		 }
		 return false;
	}
	
	
}
