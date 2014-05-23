package org.starexec.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.starexec.jobs.JobManager;

public class LoggingManager {
	
	public static void setLoggingLevel(Level level) {
		Logger.getRootLogger().setLevel(level);
	}
	
	public static void setLoggingLevelForClass(Level level, String className) {
		Logger.getLogger(className).setLevel(level);
	}
	
	
}
