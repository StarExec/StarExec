package com.starexec.data.to;

public class JobPair {
	private int id;
	private int jobid;
	private String result;
	private Benchmark benchmark;
	private Solver solver;
	
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
}
