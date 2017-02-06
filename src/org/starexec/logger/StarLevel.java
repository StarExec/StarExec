package org.starexec.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;

import java.io.Serializable;

/**
 * Created by agieg on 2/6/2017.
 */
public class StarLevel {
    public static final StarLevel OFF = new StarLevel(Level.OFF);
    public static final StarLevel FATAL = new StarLevel(Level.FATAL);
    public static final StarLevel ERROR = new StarLevel(Level.ERROR);
    public static final StarLevel WARN = new StarLevel(Level.WARN);
    public static final StarLevel INFO = new StarLevel(Level.INFO);
    public static final StarLevel DEBUG = new StarLevel(Level.DEBUG);
    public static final StarLevel TRACE = new StarLevel(Level.TRACE);
    public static final StarLevel ALL = new StarLevel(Level.ALL);

    private Level level;

    private StarLevel(Level level) {
        this.level = level;
    }

    Level get() {
        return level;
    }
}
