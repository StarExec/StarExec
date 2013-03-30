package org.starexec.data.to;

import java.util.List;
import java.util.LinkedList;
/*Represents the accuracy of a solver for a single job
 * @author Eric Burns
 */

public class JobSolver extends Identifiable {
	private Solver solver=null;
	private int totalJobPairs=0;
	private int correctJobPairs=0;
	private int errorJobPairs=0;
	private int incorrectJobPairs=0;
	private int incompleteJobPairs=0;
	private Configuration config=null;
	public JobSolver() {
		this.solver=new Solver();
	}
	
	
	public void setTotalJobPairs(int totalJobPairs) {
		this.totalJobPairs = totalJobPairs;
	}
	public int getTotalJobPairs() {
		return totalJobPairs;
	}
	public void setSolver(Solver solver) {
		this.solver = solver;
	}
	public Solver getSolver() {
		return solver;
	}
	public void setCorrectJobPairs(int correctJobPairs) {
		this.correctJobPairs = correctJobPairs;
	}
	public int getCorrectJobPairs() {
		return correctJobPairs;
	}
	
	public void incrementCorrectJobPairs() {
		correctJobPairs++;
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
		this.config=config;
	}


	public Configuration getConfiguration() {
		return config;
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
