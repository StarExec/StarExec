package org.starexec.data.to.pipelines;

import org.starexec.data.to.Processor;

/**
 * This class wraps the data we store in the job_stage_params SQL table
 * @author Eric
 *
 */
public class StageAttributes {
	private int jobId;
	private int stageNumber;
	private int wallclockTimeout;
	private int cpuTimeout;
	private long maxMemory;
	private Integer spaceId; // null if not given. Not required
	private Processor preProcessor;
	private Processor postProcessor;
	public StageAttributes() {
		jobId=-1;
		stageNumber=-1;
		wallclockTimeout=-1;
		cpuTimeout=-1;
		maxMemory=-1;
		spaceId=-1;
	}
	
	public int getJobId() {
		return jobId;
	}
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}
	public int getStageNumber() {
		return stageNumber;
	}
	public void setStageNumber(int stageId) {
		this.stageNumber = stageId;
	}
	public int getWallclockTimeout() {
		return wallclockTimeout;
	}
	public void setWallclockTimeout(int wallclockTimeout) {
		this.wallclockTimeout = wallclockTimeout;
	}
	public int getCpuTimeout() {
		return cpuTimeout;
	}
	public void setCpuTimeout(int cpuTimeout) {
		this.cpuTimeout = cpuTimeout;
	}
	public long getMaxMemory() {
		return maxMemory;
	}
	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}
	public Integer getSpaceId() {
		return spaceId;
	}
	public void setSpaceId(Integer spaceId) {
		this.spaceId = spaceId;
	}

	public Processor getPreProcessor() {
		return preProcessor;
	}

	public void setPreProcessor(Processor preProcessor) {
		this.preProcessor = preProcessor;
	}

	public Processor getPostProcessor() {
		return postProcessor;
	}

	public void setPostProcessor(Processor postProcessor) {
		this.postProcessor = postProcessor;
	}
}
