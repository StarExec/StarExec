package org.starexec.logger;

import ch.qos.logback.classic.Level;

/**
 * Wrapper class for logging levels.
 */
public enum StarLevel {
    OFF(Level.OFF),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    ALL(Level.ALL);

    private final Level level;

    StarLevel(Level level) {
        this.level = level;
    }

    Level get() {
        return level;
    }
}
