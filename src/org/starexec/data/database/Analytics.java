package org.starexec.data.database;

import org.starexec.logger.StarLogger;

import java.sql.Date;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Analytics keeps a record of how often events happen.
 * A count is kept of how many times an events occured per day.
 * A list is kept of unique users that have triggered an event per day.
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

	protected static final StarLogger log = StarLogger.getLogger(Analytics.class);

	private final int id;
	private final HashMap<Date, Data> events;

	private final class Data {
		private final HashSet<Integer> users;
		public int count = 0;

		Data() {
			users = new HashSet<>();
		}

		void record(Integer user) {
			++count;
			if (user != null) {
				users.add(user);
			}
		}

		int userCount() {
			return users.size();
		}
	}

	/**
	 * Constructor.
	 * Retrieves this event `id` from the `analytics_events` table.
	 * If this event is not found in the table, set the `id` to `-1` and move
	 * on with life. It is not worth throwing an exception.
	 */
	Analytics() {
		events = new HashMap<>();
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

	/**
	 * Record an occurance of this event initiated by a particular user
	 * @param userId the user who initiated this event
	 */
	public void record(Integer userId) {
		todaysData().record(userId);
	}

	/**
	 * Record an occurance of this event
	 * that was not initiated by a particular user
	 */
	public void record() {
		record(null);
	}

	public final static void saveToDB() {
		Date now = now();
		for (Analytics event : Analytics.values()) {
			if (event.id != -1) {
				event.events.forEach( (k, v) -> {
					try {
						event.saveToDB(k, v.count, v.userCount());
						if (k != now) {
							event.events.remove(k);
						} else {
							v.count = 0;
						}
					} catch (SQLException e) {
						log.error("Cannot record event: " + event.name(), e);
					}
				} );
			}
		}
	}

	private final void saveToDB(Date date, int i, int users) throws SQLException {
		Common.update(
			"{CALL RecordEvent(?,?,?,?)}",
			procedure -> {
				procedure.setInt(1, id);
				procedure.setDate(2, date);
				procedure.setInt(3, i);
				procedure.setInt(4, users);
			}
		);
	}

	/**
	 * Get the Data for this event today
	 * If Data does not already exist for today, create it
	 * @return Data
	 */
	private Data todaysData() {
		Data today = events.get(now());
		if (today == null) {
			today = new Data();
			events.put(now(), today);
		}
		return today;
	}

	/**
	 * @return SQL Date that represents the current date
	 */
	private static final Date now() {
		final java.util.Date now = new java.util.Date();
		long roundedTime = now.getTime() - (now.getTime() % 86400000);
		return new Date(roundedTime);
	}
}
