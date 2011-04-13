package com.starexec.util;

import java.util.logging.Logger;

public class LogUtil {
	public static void LogException(Exception e){
		StringBuilder sb = new StringBuilder();
		sb.append(e.getMessage() + Util.getLineSeparator());
		for(StackTraceElement ste : e.getStackTrace()){
			sb.append(ste.toString() + Util.getLineSeparator());
		}
		Logger.getLogger("starerror").severe(sb.toString());
	}
}
