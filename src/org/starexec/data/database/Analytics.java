package org.starexec.data.database;

import org.starexec.logger.StarLogger;

import java.sql.Date;
import java.sql.SQLException;

/**
 * Analytics keeps a record of how often events happen.
 * A count is kept of how many times an events occured per day.
 * Events must be added both here and in the `analytics_events` table.
 */
public enum Analytics {
	JOB_ATTRIBUTES,
	JOB_CREATE,
	JOB_CREATE_QUICKJOB,
	JOB_DETAILS,
	JOB_PAUSE,
	JOB_RESUME,
	PAGEVIEW_HELP,
	STAREXECCOMMAND_LOGIN;

	private final int id;

	/**
	 * Constructor.
	 * Retrieves this event `id` from the `analytics_events` table.
	 * If this event is not found in the table, set the `id` to `-1` and move
	 * on with life. It is not worth throwing an exception.
	 */
	Analytics() {
		id = id();
	}

	/**
	 * Look up the ID for this event in the DB
	 * @return ID if found, -1 if not found
	 */
	private int id() {
		int id = -1;
		try {
			id = Common.query(
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
			id = -1;
		} finally {
			return id;
		}
	}

	protected static final StarLogger log = StarLogger.getLogger(Analytics.class);

	/**
	 * Record an occurance of this event.
	 */
	public void record() {
		if (id == -1) return;

		try {
			Common.update(
					"{CALL RecordEvent(?,?)}",
					procedure -> {
						procedure.setInt(1, id);
						procedure.setDate(2, now());
					}
			);
		} catch (SQLException e) {
			log.error("Cannot record event: " + this.name(), e);
		}
	}

	/**
	 * @return SQL Date that represents the current date
	 */
	private static final Date now() {
		final java.util.Date now = new java.util.Date();
		return new java.sql.Date(now.getTime());
	}
}
