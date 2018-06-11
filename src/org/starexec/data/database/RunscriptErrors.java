package org.starexec.data.database;

import org.starexec.logger.StarLogger;
import org.starexec.constants.R;
import org.starexec.data.to.RunscriptError;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public class RunscriptErrors {
	private RunscriptErrors() {} // Class cannot be instantiated

	private static final StarLogger log = StarLogger.getLogger(Analytics.class);

	public static int getCount(Date begin, Date end) throws SQLException {
		return Common.query(
				"{CALL GetRunscriptErrorsCount(?,?)}",
				procedure -> {
					procedure.setDate(1, begin);
					procedure.setDate(2, end);
				},
				results -> {
					results.next();
					return results.getInt("count");
				}
		);
	}

	public static List<RunscriptError> getInRange(Date begin, Date end) throws SQLException {
		return Common.query(
				"{CALL GetRunscriptErrors(?,?)}",
				procedure -> {
					procedure.setDate(1, begin);
					procedure.setDate(2, end);
				},
				results -> {
					final List<RunscriptError> l = new ArrayList<>();
					while (results.next()) {
						java.util.Date time = results.getTimestamp("time");
						String node = results.getString("node");
						int jobPair = results.getInt("job_pair_id");
						l.add(new RunscriptError(time, node, jobPair));
					}
					return l;
				}
		);
	}

	public static String getUrl(Date begin, Date end) {
		return R.STAREXEC_ROOT
			+ "secure/admin/jobpairErrors.jsp?start="
			+ begin.toString()
			+ "&end="
			+ end.toString();
	}
}
