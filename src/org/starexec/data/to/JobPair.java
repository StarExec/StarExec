package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * Represents a job pair which is a solver/benchmark mapping used in jobs
 * 
 * @author Tyler Jensen
 */
public class JobPair extends Identifiable {
	private long jobId = -1;
	private long benchmarkId = -1;
	private long configurationId = -1;	
	private long nodeId = -1;
	private String status;
	private String result;
	private String runTime;
	private Timestamp startDate;
	private Timestamp endDate;

	/**
	 * @return the id of the job this pair is apart of
	 */
	public long getJobId() {
		return jobId;
	}

	/**
	 * @param jobId the jobId to set for the pair
	 */
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	/**
	 * @return the id of the benchmark that corresponds to this pair
	 */
	public long getBenchmarkId() {
		return benchmarkId;
	}

	/**
	 * @param benchmarkId the benchmark id to set for this pair
	 */
	public void setBenchmarkId(long benchmarkId) {
		this.benchmarkId = benchmarkId;
	}

	/**
	 * @return the id of the configuration that corresponds to this pair
	 */
	public long getConfigurationId() {
		return configurationId;
	}

	/**
	 * @param configurationId the configurationId to set for this pair
	 */
	public void setConfigurationId(long configurationId) {
		this.configurationId = configurationId;
	}

	/**
	 * @return the id of the node the pair ran on
	 */
	public long getNodeId() {
		return nodeId;
	}

	/**
	 * @param nodeId the nodeId to set for the pair
	 */
	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the status of the pair (e.g. running, enqueued, error, etc.)
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set for the pair
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the primary result for the pair (any short string spit out by the solver)
	 */
	public String getResult() {
		return result;
	}

	/**
	 * @param result the result to set for the pair
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * @return the date/time the pair started running
	 */
	public Timestamp getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the start date to set for the pair
	 */
	public void setStartDate(Timestamp startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the date/time the pair stopped running
	 */
	public Timestamp getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the end date to set for the solver
	 */
	public void setEndDate(Timestamp endDate) {
		this.endDate = endDate;
	}
	
	/**
	 * @return a formatted string representing the total runtime of the pair
	 */
	public String getRunTime() {
		return runTime;
	}

	/**
	 * @param runTime The run time in total seconds (which is converted to a time string)
	 */
	public void setRunTime(int runTime) {
		runTime = Math.abs(runTime);
		int minutes = runTime / 60;
		int hours = runTime / (3600);
		int days = runTime / (86400);
		int seconds = runTime - (minutes * 60) - (hours * 3600) - (days * 86400);			
		this.runTime = String.format("%sd %sh %sm %ss", days, hours, minutes, seconds);
	}	
}
