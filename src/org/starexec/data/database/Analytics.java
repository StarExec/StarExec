package org.starexec.data.database;

import org.starexec.logger.StarLogger;

import java.sql.Date;
import java.sql.SQLException;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Analytics keeps a record of how often events happen.
 * A count is kept of how many times an events occurred per day.
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
	PYTHON_API_LOGIN,
	STAREXEC_DEPLOY,
	STAREXECCOMMAND_LOGIN;

	private static final StarLogger log = StarLogger.getLogger(Analytics.class);

	private final int id;
	private final HashMap<Date, Data> events;

	private static final class Data {
		private final Set<Integer> users;
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
		}
		return id;
	}

	/**
	 * Record an occurrence of this event initiated by a particular user
	 * @param userId the user who initiated this event
	 */
	public void record(Integer userId) {
		todaysData().record(userId);
	}

	/**
	 * Record an occurrence of this event
	 * that was not initiated by a particular user
	 */
	public void record() {
		record(null);
	}

	public static void saveToDB() {
		Date now = now();
		for (Analytics event : Analytics.values()) {
			if (event.id != -1) {
				Iterator it = event.events.entrySet().iterator();
				while (it.hasNext()) {
					@SuppressWarnings("unchecked")
					Map.Entry<Date,Data> kv = (Map.Entry<Date,Data>)it.next();
					Date date = kv.getKey();
					Data v = kv.getValue();
					try {
						event.saveToDB(date, v.count);
						v.users.forEach(user -> {
							try {
								event.saveUserToDB(date, user);
							} catch (SQLException e) {
								log.error("Cannot record user " + user + " event: " + event.name(), e);
							}
						});
						if (date.equals(now)) {
							v.count = 0;
						} else {
							it.remove();
						}
					} catch (SQLException e) {
						log.error("Cannot record event: " + event.name(), e);
					}
				}
			}
		}
	}

	private void saveToDB(Date date, int count) throws SQLException {
		Common.update(
			"{CALL RecordEvent(?,?,?)}",
			procedure -> {
				procedure.setInt(1, id);
				procedure.setDate(2, date);
				procedure.setInt(3, count);
			}
		);
	}

	private void saveUserToDB(Date date, int userId) throws SQLException {
		Common.update(
			"{CALL RecordEventUser(?,?,?)}",
			procedure -> {
				procedure.setInt(1, id);
				procedure.setDate(2, date);
				procedure.setInt(3, userId);
			}
		);
	}

	/**
	 * Get the Data for this event today
	 * If Data does not already exist for today, create it
	 * @return Data
	 */
	private Data todaysData() {
		return events.computeIfAbsent(now(), k -> new Data());
	}

	/**
	 * @return SQL Date that represents the current date
	 */
	private static Date now() {
		final java.util.Date now = new java.util.Date();
		long roundedTime = now.getTime() - (now.getTime() % 86400000);
		return new Date(roundedTime);
	}
}
