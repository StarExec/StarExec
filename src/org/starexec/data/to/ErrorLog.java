package org.starexec.data.to;

import org.starexec.logger.StarLevel;

import java.sql.Timestamp;

/**
 * Created by agieg on 2/9/2017.
 */
public class ErrorLog extends Identifiable {
    private final String message;
    private final StarLevel level;
    private final Timestamp time;

    public ErrorLog(final int id, final String message, final StarLevel level, final Timestamp time) {
        this.setId(id);
        this.message = message;
        this.level = level;
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public StarLevel getLevel() {
        return level;
    }

    public Timestamp getTime() {
        return time;
    }
}
