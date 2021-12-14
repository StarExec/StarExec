package org.starexec.data.database;

import org.starexec.data.to.ErrorLog;
import org.starexec.logger.NonSavingStarLogger;
import org.starexec.logger.StarLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ErrorLogs {

	// We have to use NonSavingStarLogger here to prevent us from attempting to save errors that occur in this class.
	// If we tried to save an error that occurred in this class to the error_logs table it would call us to recursively
	// call the methods in this class again potentially leading to infinite recursion.
	private static final NonSavingStarLogger log = NonSavingStarLogger.getLogger(ErrorLogs.class);

	/**
	 * Adds an error reports to the error_reports table. This method must catch all exceptions so that another method
	 * doesn't catch them and call this method.
	 *
	 * @param message the message to add to the table.
	 * @param level the level the error was logged at.
	 * @return the id of the new log.
	 */
	public static Optional<Integer> add(String message, StarLevel level) {
		try {
			return Common.updateWithOutput("{CALL AddErrorLog(?, ?, ?)}", procedure -> {
				procedure.setString(1, message);
				procedure.setString(2, level.toString());
				procedure.registerOutParameter(3, java.sql.Types.INTEGER);
			}, procedure -> Optional.of(procedure.getInt(3)));
		} catch (Exception e) {
			// Must catch all exceptions since we don't want another method to catch them and indirectly call this
			// method again to add error to the database.
			log.error("Caught exception while trying to add message to database.");

			return Optional.empty();
		}
	}

	/**
	 * Gets an Error Log with the given id.
	 *
	 * @param id the id of the error log to get.
	 * @return the error log.
	 * @throws SQLException on database error.
	 */
	public static Optional<ErrorLog> getById(final int id) throws SQLException {
		return Common.query("{CALL GetErrorLogById(?)}", procedure -> procedure.setInt(1, id), ErrorLogs::getFirst);
	}

	/**
	 * Deletes the error log with the given id.
	 *
	 * @param id the id of the error log to delete.
	 * @throws SQLException on database error.
	 */
	public static void deleteWithId(final int id) throws SQLException {
		Common.update("{CALL DeleteErrorLogWithId(?)}", procedure -> procedure.setInt(1, id));
	}

	/**
	 * Deletes logs before (not including) the given time.
	 *
	 * @param time delete logs before this time.
	 * @throws SQLException on database error.
	 */
	public static void deleteBefore(Timestamp time) throws SQLException {
		Common.update("{CALL DeleteErrorLogsBefore(?)}", procedure -> procedure.setTimestamp(1, time));
	}

	public static boolean existBefore(Timestamp time) throws SQLException {
		return Common.query("{CALL GetErrorLogsBefore(?)}", procedure -> procedure.setTimestamp(1, time),
		                    results -> getFirst(results).isPresent());
	}

	public static List<ErrorLog> getAll() throws SQLException {
		return Common.query("{CALL GetAllErrorLogs()}", procedure -> {}, ErrorLogs::resultsToErrorLogs);
	}

	public static void deleteAll() throws SQLException {
		deleteBefore(Timestamp.from(Instant.now()));
	}

	/**
	 * Gets logs since (including) this time.
	 *
	 * @param time the time to get logs since.
	 * @return the logs since the given time as a list.
	 * @throws SQLException on database error.
	 */
	public static List<ErrorLog> getSince(Timestamp time) throws SQLException {
		return Common.query("{CALL GetErrorLogsSince(?)}", procedure -> procedure.setTimestamp(1, time),
		                    ErrorLogs::resultsToErrorLogs);
	}

	/**
	 * Gets the first error log in a set of error log results.
	 *
	 * @param results the result set to get the logs from.
	 * @return the first log or an empty optional if there were no logs.
	 * @throws SQLException
	 */
	private static Optional<ErrorLog> getFirst(ResultSet results) throws SQLException {
		List<ErrorLog> logs = resultsToErrorLogs(results);
		return !logs.isEmpty() ? Optional.of(logs.get(0)) : Optional.empty();
	}

	/**
	 * Converts a result set to a set of error logs.
	 *
	 * @param results the result set to get error logs from.
	 * @return the list of error logs from the result set.
	 * @throws SQLException on database error.
	 */
	private static List<ErrorLog> resultsToErrorLogs(ResultSet results) throws SQLException {
		List<ErrorLog> errorLogs = new ArrayList<>();
		while (results.next()) {
			int id = results.getInt("id");
			StarLevel level = StarLevel.valueOf(results.getString("level"));
			Timestamp time = results.getTimestamp("time");
			String message = results.getString("message");

			errorLogs.add(new ErrorLog(id, message, level, time));
		}

		return errorLogs;
	}
}
