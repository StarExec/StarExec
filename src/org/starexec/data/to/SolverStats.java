package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * This class stores the statistics for a single solver/configuration pair for a job
 *
 * @author Eric
 */

public class SolverStats extends Identifiable {
	@Expose private Solver solver = null;
	@Expose private Configuration configuration = null;
	@Expose private int completeJobPairs = 0; //pairs with any finished status except Starexec errors
	@Expose private int failedJobPairs = 0;   //Starexec error pairs
	@Expose private int correctJobPairs = 0;  //7 status with result = expected
	@Expose private int incorrectJobPairs = 0;//7 status with result != expected
	@Expose private int incompleteJobPairs = 0;// any status indicating this pair is being worked on OR FAILED
	@Expose private int resourceOutJobPairs = 0; //status 14-17
	@Expose private int conflicts = 0;
	@Expose private double wallTime = 0;
	@Expose private double cpuTime = 0;
	@Expose private int stageNumber = 0;
	@Expose private int jobSpaceId;
	// instead of having its own field for whether or not a config has been deleted, access that field through the
	// owned Configuration configuration object

	public SolverStats() {
		this.solver = new Solver();
		this.configuration = new Configuration();
	}

	public void incrementConflicts() {
		conflicts++;
	}

	public int getConflicts() {
		return conflicts;
	}

	public void setConflicts(final int conflicts) {
		this.conflicts = conflicts;
	}

	public int getTotalJobPairs() {
		return completeJobPairs + failedJobPairs + incompleteJobPairs;
	}

	public void setSolver(Solver solver) {
		this.solver = solver;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setCompleteJobPairs(int completeJobPairs) {
		this.completeJobPairs = completeJobPairs;
	}

	public int getCompleteJobPairs() {
		return completeJobPairs;
	}

	public void incrementCompleteJobPairs() {
		completeJobPairs++;
	}

	public void setIncorrectJobPairs(int incorrectJobPairs) {
		this.incorrectJobPairs = incorrectJobPairs;
	}

	public void setCorrectJobPairs(int correctJobPairs) {
		this.correctJobPairs = correctJobPairs;
	}

	public int getIncorrectJobPairs() {
		return incorrectJobPairs;
	}

	public void incrementIncorrectJobPairs() {
		incorrectJobPairs++;
	}

	public void incrementCorrectJobPairs() {
		correctJobPairs++;
	}

	public int getCorrectJobPairs() {
		return correctJobPairs;
	}

	public void setIncompleteJobPairs(int incompleteJobPairs) {
		this.incompleteJobPairs = incompleteJobPairs;
	}

	public int getIncompleteJobPairs() {
		return incompleteJobPairs;
	}

	public void incrementIncompleteJobPairs() {
		incompleteJobPairs++;
	}

	public void setConfiguration(Configuration config) {
		this.configuration = config;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setFailedJobPairs(int failedJobPairs) {
		this.failedJobPairs = failedJobPairs;
	}

	public int getFailedJobPairs() {
		return failedJobPairs;
	}

	public void incrementFailedJobPairs() {
		failedJobPairs++;
	}

	public void setResourceOutJobPairs(int resourceOutJobPairs) {
		this.resourceOutJobPairs = resourceOutJobPairs;
	}

	public int getResourceOutJobPairs() {
		return resourceOutJobPairs;
	}

	public void incrementResourceOutPairs() {
		this.resourceOutJobPairs++;
	}

	public void setWallTime(double wallTime) {
		this.wallTime = wallTime;
	}

	public double getWallTime() {
		return wallTime;
	}

	public void incrementWallTime(double time) {
		this.wallTime += time;
	}

	public void setCpuTime(double cpuTime) {
		this.cpuTime = cpuTime;
	}

	public double getCpuTime() {
		return cpuTime;
	}

	public void incrementCpuTime(double time) {
		this.cpuTime += time;
	}

	public int getUnknown() {
		return completeJobPairs - (resourceOutJobPairs + correctJobPairs + incorrectJobPairs);
	}

	public int getStageNumber() {
		return stageNumber;
	}

	public void setStageNumber(int stageNumber) {
		this.stageNumber = stageNumber;
	}

	public int getJobSpaceId() {
		return jobSpaceId;
	}

	public void setJobSpaceId(int jobSpaceId) {
		this.jobSpaceId = jobSpaceId;
	}

	public String getCorrectOverCompleted() {
		return getCorrectJobPairs() + "/" + getCompleteJobPairs();
	}

	public int getConfigDeleted() { return configuration.getDeleted(); }

	/* 
	 * One of the users wanted us to include a button that adds the feature
	 * to get pairs with an unknown status in the solver tables, this required 
	 * a redesign of the database table, related procedures, api, along with 
	 * the associated cache. We need a toString to make sure that the cache
	 * is working correctly. I made every effort to display what the front end shows.
	 * @author aguo2 
	 */
	@Override
	public String toString() {
		return "{" + solver.getName() + ", " + configuration.getName() + ", " + correctJobPairs 
		+ "/" + getTotalJobPairs() + ", " + incorrectJobPairs + ", " + resourceOutJobPairs + ", " + failedJobPairs + ", " +
		getUnknown() + ", " + getIncompleteJobPairs() + ", CPU: " + getCpuTime() + ", wall: " + getWallTime() + ", " + getConflicts() + "}";
	}


}
