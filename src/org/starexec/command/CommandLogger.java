package org.starexec.command;

/**
 * Created by agieg on 9/24/2016.
 */
public class CommandLogger {

    final private String className;

    private CommandLogger(String className) {
        this.className = className;
    }

    public static CommandLogger getLogger(Class c) {
        return new CommandLogger(c.getName());
    }

    void log(String message) {
        if (C.debugMode) {
            System.out.println("[" + className + "] " + message);
        }
    }
}
