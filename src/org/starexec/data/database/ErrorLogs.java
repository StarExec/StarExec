package org.starexec.data.database;

import org.starexec.data.to.ErrorLog;
import org.starexec.logger.NonSavingStarLogger;
import org.starexec.logger.StarLevel;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class ErrorLogs {

    // We have to use NonSavingStarLogger here to prevent us from attempting to save errors that occur in this class.
    // If we tried to save an error that occurred in this class to the error_logs table it would call us to recursively
    // call the methods in this class again potentially leading to infinite recursion.
    NonSavingStarLogger log = NonSavingStarLogger.getLogger(ErrorLogs.class);

    /**
     * Adds an error reports to the error_reports table.
     * @param message the message to add to the table.
     * @param level the level the error was logged at.
     * @return the id of the new log.
     * @throws SQLException on database error.
     */
    public static int add(String message, StarLevel level) throws SQLException {
        return Common.updateWithOutput("{CALL AddErrorLog(?, ?, ?)}", procedure -> {
            procedure.setString(1, message);
            procedure.setString(2, level.toString());
            procedure.registerOutParameter(3, java.sql.Types.INTEGER);

        }, procedure -> procedure.getInt(3) );
    }


    /**
     * Gets an Error Log with the given id.
     * @param id the id of the error log to get.
     * @return the error log.
     * @throws SQLException on database error.
     */
    public static Optional<ErrorLog> getById(final int id) throws SQLException {
        return Common.query("{CALL GetErrorLogById(?)}"
                , procedure -> procedure.setInt(1, id)
                , results -> {
                    if (results.next()) {
                        int retrievedId = results.getInt("id");
                        String message = results.getString("message");
                        Timestamp time = results.getTimestamp("time");
                        StarLevel level = StarLevel.valueOf(results.getString("level"));

                        return Optional.of(new ErrorLog(retrievedId, message, level, time));
                    } else {
                        return Optional.empty();
                    }
                });
    }

    public static void deleteWithId(final int id) throws SQLException {
        Common.update("{CALL DeleteErrorLogWithId(?)", procedure -> procedure.setInt(1, id));
    }

    public static void clearSince(Date date) throws SQLException {

    }

    public static List<ErrorLog> getSince(Date date) throws SQLException {
        return null;
    }
}
