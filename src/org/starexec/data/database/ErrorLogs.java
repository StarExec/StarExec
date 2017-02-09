package org.starexec.data.database;

import org.starexec.data.to.ErrorLog;
import org.starexec.logger.StarLevel;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class ErrorLogs {
    /**
     * Adds an error reports to the error_reports table.
     * @param message the message to add to the table.
     * @param level the level the error was logged at.
     * @throws SQLException on database error.
     */
    public static void add(String message, StarLevel level) throws SQLException {
        // We need to use the no logging version because logging an error would indirectly
        // call this method again, creating an infinite loop.
        Common.updateNoLogging("{CALL AddErrorLog(?, ?)}", procedure -> {
            procedure.setString(1, message);
            procedure.setString(2, level.toString());
        });
    }

    public static void clearSince(Date date) throws SQLException {

    }

    public static List<ErrorLog> getSince(Date date) throws SQLException {
        return null;
    }
}
