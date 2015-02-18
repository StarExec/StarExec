package org.starexec.data.to.pipelines;

import java.util.Properties;

import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Identifiable;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.WorkerNode;

/**
 * This class represents the set of results for a single stage of a jobline
 * @author Eric
 *
 */
public class JoblineStage extends Identifiable {
	private Solver solver = null;
	private Integer stageId=null;
	private Integer jobpairId=null;
	private double wallclockTime;
	private double cpuTime;
	private double userTime;
	private double systemTime;

	private double memoryUsage;
	private double maxVirtualMemory;
	private double maxResidenceSetSize;
	private Configuration configuration = null;
	private boolean primary=false;
	public JoblineStage() {
		this.setSolver(new Solver());
		this.setConfiguration(new Configuration());
		
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public double getMaxResidenceSetSize() {
		return maxResidenceSetSize;
	}

	public void setMaxResidenceSetSize(double maxResidenceSetSize) {
		this.maxResidenceSetSize = maxResidenceSetSize;
	}

	public double getMaxVirtualMemory() {
		return maxVirtualMemory;
	}

	public void setMaxVirtualMemory(double maxVirtualMemory) {
		this.maxVirtualMemory = maxVirtualMemory;
	}

	public double getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(double memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public double getSystemTime() {
		return systemTime;
	}

	public void setSystemTime(double systemTime) {
		this.systemTime = systemTime;
	}

	public double getUserTime() {
		return userTime;
	}

	public void setUserTime(double userTime) {
		this.userTime = userTime;
	}

	public double getCpuTime() {
		return cpuTime;
	}

	public void setCpuUsage(double cpuTime) {
		this.cpuTime = cpuTime;
	}

	public double getWallclockTime() {
		return wallclockTime;
	}

	public void setWallclockTime(double wallclockTime) {
		this.wallclockTime = wallclockTime;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setSolver(Solver solver) {
		this.solver = solver;
	}

	public Integer getStageId() {
		return stageId;
	}

	public void setStageId(Integer stageId) {
		this.stageId = stageId;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public Integer getJobpairId() {
		return jobpairId;
	}

	public void setJobpairId(Integer jobpairId) {
		this.jobpairId = jobpairId;
	}
}
