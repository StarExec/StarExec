package org.starexec.data.to;

import org.starexec.data.database.Analytics;
import org.starexec.data.database.Common;
import org.starexec.logger.StarLogger;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

/**
 * Analytics keeps a record of how often events happen.
 * A count is kept of how many times an events occured per day.
 * Actions must be added both here and in the `analytics_events` table.
 */
public class AnalyticsResults {
	public final Analytics event;
	public final int count;

	protected static final StarLogger log = StarLogger.getLogger(AnalyticsResults.class);

	public AnalyticsResults(String event, int count) {
		this.event = Analytics.valueOf(event);
		this.count = count;
	}

	/**
	 * Creates a list of AnalyticsResults from a sql ResultSet
	 * @param results ResultSet containing
	 * @return list of AnalyticsResults
	 */
	private static LinkedList<AnalyticsResults> listFromResults(ResultSet results) throws SQLException {
		LinkedList<AnalyticsResults> list = new LinkedList<>();
		while (results.next()) {
			list.add(
				new AnalyticsResults(
					results.getString("event"),
					results.getInt("count")
				)
			);
		}
		return list;
	}

	/**
	 * Gets results for all events between `start` and `end`
	 * @param start
	 * @param end
	 * @return AnalyticsResults
	 */
	public static Iterable<AnalyticsResults> getAllEvents(Date start, Date end) {
		try {
			return Common.query(
					"{CALL GetAnalyticsForDateRange(?,?)}",
					procedure -> {
						procedure.setDate(1, start);
						procedure.setDate(2, end);
					},
					results -> listFromResults(results)
			);
		} catch (SQLException e) {
			log.error("GetAnalyticsForDateRange");
			return new LinkedList<>();
		}
	}
}
