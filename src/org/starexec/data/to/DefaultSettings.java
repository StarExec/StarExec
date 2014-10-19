package org.starexec.data.to;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.util.Util;

public class DefaultSettings extends Identifiable {
	private Integer preProcessorId;
	private Integer postProcessorId;
	private Integer benchProcessorId;
	private Integer benchId;
	private Integer solverId;
	private int wallclockTimeout;
	private int cpuTimeout;
	private long maxMemory;
	private boolean dependenciesEnabled;
	private String name;
	private int tempId;
	/**
	 * Initializes a new DefaultSettings object with every field set to the system default.
	 */
	public DefaultSettings() {
		postProcessorId=null;
		wallclockTimeout=10;
		cpuTimeout=10;
		benchId=null;
		solverId=null;
		maxMemory=1073741824;
		preProcessorId=null;
		name="settings";
		dependenciesEnabled=false;
		benchProcessorId=Processors.getNoTypeProcessor().getId();
	}
	public void setPreProcessorId(int preProcessorId) {
		this.preProcessorId = preProcessorId;
	}
	public Integer getPreProcessorId() {
		return preProcessorId;
	}
	public void setPostProcessorId(int postProcessorId) {
		this.postProcessorId = postProcessorId;
	}
	public Integer getPostProcessorId() {
		return postProcessorId;
	}
	public void setBenchProcessorId(int benchProcessorId) {
		this.benchProcessorId = benchProcessorId;
	}
	public Integer getBenchProcessorId() {
		return benchProcessorId;
	}
	public void setBenchId(int benchId) {
		this.benchId = benchId;
	}
	public Integer getBenchId() {
		return benchId;
	}
	public void setSolverId(int solverId) {
		this.solverId = solverId;
	}
	public Integer getSolverId() {
		return solverId;
	}
	public void setWallclockTimeout(int wallclockTimeout) {
		this.wallclockTimeout = wallclockTimeout;
	}
	public int getWallclockTimeout() {
		return wallclockTimeout;
	}
	public void setCpuTimeout(int cpuTimeout) {
		this.cpuTimeout = cpuTimeout;
	}
	public int getCpuTimeout() {
		return cpuTimeout;
	}
	public void setMaxMemory(long mem) {
		this.maxMemory = mem;
	}
	public long getMaxMemory() {
		return this.maxMemory;
	}
	public double getRoundedMaxMemoryAsDouble() {
		return Util.bytesToGigabytes(maxMemory);
	}
	public void setDependenciesEnabled(boolean dependenciesEnabled) {
		this.dependenciesEnabled = dependenciesEnabled;
	}
	public boolean isDependenciesEnabled() {
		return dependenciesEnabled;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		
		return name;
	}
	public void setTempId(int tempId) {
		this.tempId = tempId;
	}
	public int getTempId() {
		return tempId;
	}
	
	public String getSolverName() {
		if (solverId==null) {
			return "None";
		}
		Solver s=Solvers.get(solverId);
		if (s==null) {
			return "None";
		}
		return s.getName();
	}
	
	public String getBenchmarkContents() {
		if (benchId==null) {
			return "None";
		}
		Benchmark b=Benchmarks.get(benchId);
		if (b==null) {
			return "None";
		}
		return Benchmarks.getContents(b, -1);
		
	}
}
