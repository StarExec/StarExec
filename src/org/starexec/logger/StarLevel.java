package org.starexec.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;

import java.io.Serializable;

/**
 * Wrapper class for logging levels.
 */
public enum StarLevel {
    OFF(Level.OFF, "OFF"),
    FATAL(Level.FATAL, "FATAL"),
    ERROR(Level.ERROR, "ERROR"),
    WARN(Level.WARN, "WARN"),
    INFO(Level.INFO, "INFO"),
    DEBUG(Level.DEBUG, "DEBUG"),
    TRACE(Level.TRACE, "TRACE"),
    ALL(Level.ALL, "ALL");

    private final Level level;

    // Corresponds to values in logging_levels database table.
    private final String name;

    StarLevel(Level level, String name) {
        this.level = level;
        this.name = name;
    }

    Level get() {
        return level;
    }

    @Override
    public String toString() {
        return name;
    }
}
