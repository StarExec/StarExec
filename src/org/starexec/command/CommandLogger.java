package org.starexec.command;

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
            System.out.println("[" + logClass.getSimpleName() + "] " + message);
        }
    }
}
