package org.starexec.data.to;

import org.starexec.logger.StarLevel;

import java.sql.Date;

/**
 * Created by agieg on 2/9/2017.
 */
public class ErrorLog extends Identifiable {
    private final String message;
    private final StarLevel level;
    private final Date date;

    public ErrorLog(final int id, final String message, final StarLevel level, final Date date) {
        this.setId(id);
        this.message = message;
        this.level = level;
        this.date = date;
    }
}
