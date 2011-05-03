package com.starexec.data.to;

import java.sql.Timestamp;

public class JobPair {
	private int id;
	private int jobid;
	private String result;
	private Benchmark benchmark;
	private Solver solver;
	private Configuration config;
	private Timestamp startTime;
	private Timestamp endTime;
	private String runTime = "";
	private String node;
	private String status;
	
	public int getJobId() {
		return jobid;
	}
	
	public void setJobId(int jobid) {
		this.jobid = jobid;
	}
	
	public String getResult() {
		return result;
	}
	
	public void setResult(String result) {
		this.result = result;
	}

	public Benchmark getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(Benchmark benchmark) {
		this.benchmark = benchmark;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setSolver(Solver solver) {
		this.solver = solver;
	}

	public void setId(int id) {
		this.id = id;
	}			
	
	public int getId() {
		return this.id;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp time) {
		this.startTime = time;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

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
