package org.starexec.command;

import org.starexec.util.Util;

/**
 * Created by agieg on 9/24/2016.
 */
public class CommandLogger {

	final private Class logClass;

	private CommandLogger(Class c) {
		logClass = c;
	}

	public static CommandLogger getLogger(Class c) {
		return new CommandLogger(c);
	}

	void log(String message) {
		if (C.debugMode) {
			System.out.println(Util.getTime() + " [" + logClass.getSimpleName() + "] " + message);
		}
	}
}
