package org.starexec.data.to;

public class JobSpace extends Identifiable implements Nameable{
	private String name;
	private Integer parentSpace;
	private Integer maxStages;
	private Integer jobId;
	public JobSpace() {
		
	}
	
	/**
	 * @return the user defined name for the space
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set for the space
	 */
	public void setName(String name) {
		this.name = name;
	}

	public Integer getParentSpace() {
		return parentSpace;
	}

	public void setParentSpace(Integer parentSpace) {
		this.parentSpace = parentSpace;
	}

	public Integer getMaxStages() {
		return maxStages;
	}

	public void setMaxStages(Integer maxStages) {
		this.maxStages = maxStages;
	}

	public Integer getJobId() {
		return jobId;
	}

	public void setJobId(Integer jobId) {
		this.jobId = jobId;
	}
}
