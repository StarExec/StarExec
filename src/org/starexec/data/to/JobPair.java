package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * Represents a job pair which is a solver/benchmark mapping used in jobs
 * 
 * @author Tyler Jensen
 */
public class JobPair extends Identifiable {	
	private long nodeId = -1;
	private String status;
	private String result;
	private String runTime = "";
	private Timestamp startDate;
	private Timestamp endDate;
	private Job job;
	private Benchmark benchmark;
	private Solver solver;

	/**
	 * @return the job this pair is apart of
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * @param job the job to set for the pair
	 */
	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * @return the benchmark that corresponds to this pair
	 */
	public Benchmark getBenchmark() {
		return benchmark;
	}

	/**
	 * @param benchmark the benchmark to set for this pair
	 */
	public void setBenchmark(Benchmark benchmark) {
		this.benchmark = benchmark;
	}

	/**
	 * @return the solver that corresponds to this pair
	 */
	public Solver getSolver() {
		return solver;
	}

	/**
	 * @param solver the solver to set for this pair
	 */
	public void setSolver(Solver solver) {
		this.solver = solver;
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
		
		if(this.endDate!= null && this.startDate != null) {
			this.setRunTime(endDate.getTime() - startDate.getTime());
		}
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
		
		if(this.endDate!= null && this.startDate != null) {
			this.setRunTime(endDate.getTime() - startDate.getTime());
		}
	}
	
	/**
	 * @return a formatted string representing the total runtime of the pair
	 */
	public String getRunTime() {
		return runTime;
	}

	/**
	 * @param runTime The run time in total milliseconds (which is converted to a time string)
	 */
	public void setRunTime(long runTime) {
		runTime = Math.abs(runTime);	
		long days = runTime / (86400000);		
		runTime -= (days * 86400000);
		long hours = runTime / 3600000;
		runTime -= (hours * 3600000);
		long minutes = runTime / 60000;	
		runTime -= (minutes * 60000);
		long seconds = runTime / 1000;
		runTime -= (seconds * 1000);
		long ms = runTime;
		
		this.runTime = String.format("%sd %sh %sm %s.%ss", days, hours, minutes, seconds, ms);
	}
}
