package org.starexec.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LogManager {
	
	public static void setLoggingLevel(Level level) {
		Logger.getRootLogger().setLevel(level);
	}
	
}
