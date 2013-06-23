package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * This class stores the statistics for a single solver/configuration pair for a job
 * @author Eric
 *
 */

public class JobSolver extends Identifiable {
	@Expose private Solver solver=null;
	@Expose private Configuration configuration=null;
	@Expose private int totalJobPairs=0;
	@Expose private int completeJobPairs=0;
	@Expose private int errorJobPairs=0;
	@Expose private int incorrectJobPairs=0;
	@Expose private int incompleteJobPairs=0;
	@Expose private double time=0;
	
	
	public JobSolver() {
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
	public void setTotalJobPairs(int totalJobPairs) {
		this.totalJobPairs = totalJobPairs;
	}
	public int getTotalJobPairs() {
		return totalJobPairs;
	}
	
	public void incrementTotalJobPairs() {
		this.totalJobPairs++;
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
	public int getIncorrectJobPairs() {
		return incorrectJobPairs;
	}
	
	public void incrementIncorrectJobPairs() {
		incorrectJobPairs++;
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


	public void setErrorJobPairs(int errorJobPairs) {
		this.errorJobPairs = errorJobPairs;
	}


	public int getErrorJobPairs() {
		return errorJobPairs;
	}
	
	public void incrementErrorJobPairs() {
		errorJobPairs++;
	}
}
