package util;

import java.util.logging.Logger;

public class LogUtil {
	public static void LogException(Exception e){
		StringBuilder sb = new StringBuilder();
		for(StackTraceElement ste : e.getStackTrace()){
			sb.append(ste.toString() + "\n");
		}
		Logger.getLogger("starerror").severe(sb.toString());
	}
}
