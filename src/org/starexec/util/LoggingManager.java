package org.starexec.util;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.starexec.jobs.JobManager;

public class LoggingManager {
	
	public static void setLoggingLevel(Level level) {
		Logger.getRootLogger().setLevel(level);
	}
	
	public static boolean setLoggingLevelForClass(Level level, String className) {
		Logger log=LogManager.exists(className);
		if (log==null) {
			return false;
		}
		log.setLevel(level);
		return true;
	}
	
	
}
