package org.starexec.data.to;

import org.starexec.logger.StarLogger;
import org.starexec.data.database.JobPairs;

import java.sql.Date;

/**
 *
 */
public class RunscriptError {
	public final Date time;
	public final WorkerNode node;
	public final JobPair jobPair;

	protected static final StarLogger log = StarLogger.getLogger(AnalyticsResults.class);

	public RunscriptError(Date time, String node, int jobPairId) {
		this.time = time;
		this.node = new WorkerNode(node);
		this.jobPair = JobPairs.getPair(jobPairId);
	}
}
