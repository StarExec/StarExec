package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * This class stores the statistics for a single solver/configuration pair for a job
 * @author Eric
 *
 */

public class SolverStats extends Identifiable {
	@Expose private Solver solver=null;
	@Expose private Configuration configuration=null;
	@Expose private int completeJobPairs=0;
	@Expose private int failedJobPairs=0;
	@Expose private int correctJobPairs=0;
	@Expose private int incorrectJobPairs=0;
	@Expose private int incompleteJobPairs=0;
	@Expose private double time=0;
	
	
	public SolverStats() {
		this.solver=new Solver();
		this.configuration=new Configuration();
	}
	
	public void incrementTime(double newTime) {
		this.time+=newTime;
	}
	public void setTime(double newTime) {
		this.time=newTime;
	}
	public double getTime() {
		return this.time;
	}
	
	public int getTotalJobPairs() {
		return completeJobPairs+failedJobPairs+incorrectJobPairs+incompleteJobPairs;
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
		this.configuration=config;
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
}
