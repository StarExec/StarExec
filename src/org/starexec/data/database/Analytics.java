package org.starexec.data.database;

import org.starexec.logger.StarLogger;
import java.sql.SQLException;

/**
 * Analytics keeps a record of how often actions happen.
 * A count is kept of how many times an actions occured per day.
 * Actions must be added both here and in the `analytics_actions` table.
 */
public enum Analytics {
	JOB_PAUSE;

	private final int id;

	/**
	 * Constructor.
	 * Retrieves this actions `id` from the `analytics_actions` table.
	 * If this action is not found in the table, set the `id` to `-1` and move
	 * on with life. It is not worth throwing an exception.
	 */
	Analytics() {
		int _id = -1;
		try {
			_id = Common.query(
					"{CALL GetEventId(?)}",
					procedure -> procedure.setString(1, this.name()),
					results -> {
						results.next();
						return results.getInt("event_id");
					}
			);
		} catch (SQLException e) {
			/* Cannot access `static log`
			 * because it has not been initialized yet
			 */
			final StarLogger log = StarLogger.getLogger(Analytics.class);
			log.error("Event not found in database: " + this.name());

			_id = -1;
		} finally {
			id = _id;
		}
	}

	protected static final StarLogger log = StarLogger.getLogger(Analytics.class);

	/**
	 * Record an occurance of this action.
	 */
	public void record() {
		if (id == -1) return;

		final java.util.Date _now = new java.util.Date();
		final java.sql.Date now = new java.sql.Date(_now.getTime());
		try {
			Common.update(
					"{CALL RecordEvent(?,?)}",
					procedure -> {
						procedure.setInt(1, id);
						procedure.setDate(2, now);
					}
			);
		} catch (SQLException e) {
			log.error("Cannot record event: " + this.name());
		}
	}
}
