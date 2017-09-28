package org.starexec.data.to;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents a status for a job
 */
public enum JobStatus {
	UNKNOWN("unknown"), COMPLETE("complete"), RUNNING("incomplete"), PAUSED("paused"), KILLED("killed"), PROCESSING(
			"incomplete"), // This is only "incomplete" instead of "processing" for historical reasons
	DELETED("deleted"), GLOBAL_PAUSE("global pause");

	private final String status;

	private JobStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return status;
	}

	/**
	 * Returns a JobStatus from an SQL result
	 *
	 * @param result SQL result containing ONLY a JobStatus label
	 * @return the corresponding JobStatus
	 */
	public static JobStatus fromResultSet(ResultSet result) throws SQLException {
		result.next();
		return valueOf(result.getString(1));
	}
}
