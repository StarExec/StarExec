package org.starexec.data.to;

import org.starexec.logger.StarLogger;
import org.starexec.data.database.JobPairs;

import java.util.Date;

/**
 *
 */
public class RunscriptError {
	public final Date time;
	public final WorkerNode node;
	public final JobPair jobPair;

	protected static final StarLogger log = StarLoggerFactory.getLogger(AnalyticsResults.class);

	public RunscriptError(Date time, String node, int jobPairId) {
		this.time = time;
		this.node = new WorkerNode(node);
		this.jobPair = JobPairs.getPair(jobPairId);
	}
}
