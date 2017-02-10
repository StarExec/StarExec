package org.starexec.data.database;

import org.starexec.data.to.ErrorLog;
import org.starexec.logger.NonSavingStarLogger;
import org.starexec.logger.StarLevel;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLType;
import java.util.List;

public class ErrorLogs {

    // We have to use NonSavingStarLogger here to prevent us from attempting to save errors that occur in this class.
    // If we tried to save an error that occurred in this class to the error_logs table it would call us to recursively
    // call the methods in this class again potentially leading to infinite recursion.
    NonSavingStarLogger log = NonSavingStarLogger.getLogger(ErrorLogs.class);

    /**
     * Adds an error reports to the error_reports table.
     * @param message the message to add to the table.
     * @param level the level the error was logged at.
     * @throws SQLException on database error.
     */
    public static void add(String message, StarLevel level) throws SQLException {
        Common.updateWithOutput("{CALL AddErrorLog(?, ?)}", procedure -> {
            procedure.setString(1, message);
            procedure.setString(2, level.toString());
        }, procedure -> {
            procedure.registerOutParameter(1, java.sql.Types.INTEGER);
            return 1;
        });
    }

    public static void clearSince(Date date) throws SQLException {

    }

    public static List<ErrorLog> getSince(Date date) throws SQLException {
        return null;
    }
}
