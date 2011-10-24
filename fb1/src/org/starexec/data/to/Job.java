package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * Represents a job in the database
 * 
 * @author Tyler Jensen
 */
public class Job extends Identifiable {
	private long userId = -1;
	private long nodeClassId = -1;
	@Expose private String name;
	private Timestamp submitted;
	private Timestamp finished;
	private long timeout;
	@Expose	private String status;
	@Expose	private String description;
	private String runTime = "N/A";
	private List<JobPair> jobPairs;
	
	public Job() {
		jobPairs = new LinkedList<JobPair>();
	}
	
	/**
	 * @return the user id of the user who created the job
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * @param userId the user id to set as the creator
	 */
	public void setUserId(long userId) {
		this.userId = userId;
	}

	/**
	 * @return the id of the node class the job was intended to run on
	 */
	public long getNodeClassId() {
		return nodeClassId;
	}

	/**
	 * @param nodeClassId the id of the node class to set for the job
	 */
	public void setNodeClassId(long nodeClassId) {
		this.nodeClassId = nodeClassId;
	}

	/**
	 * @return the user defined name for the job
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set for the job
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the date the job was submitted
	 */
	public Timestamp getSubmitted() {
		return submitted;
	}

	/**
	 * @param submitted the submit date to set for the job
	 */
	public void setSubmitted(Timestamp submitted) {
		this.submitted = submitted;
	}

	/**
	 * @return the date the job finished
	 */
	public Timestamp getFinished() {
		return finished;
	}

	/**
	 * @param finished the finish date to set for the job
	 */
	public void setFinished(Timestamp finished) {
		this.finished = finished;
	}

	/**
	 * @return the maximum amount of time (in seconds) a job can run
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout the timeout for the job
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return the current status of the job (e.g. ready, enqueued, etc.)
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status for the job
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the user defined description of the job
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description for the job
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return the list of job pairs belonging to this job
	 */
	public List<JobPair> getJobPairs() {
		return jobPairs;
	}

	/**
	 * @param jobPair the job pair to add to the job
	 */
	public void addJobPair(JobPair jobPair) {
		jobPairs.add(jobPair);
	}

	/**
	 * @return a formatted string representing the job's total runtime
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