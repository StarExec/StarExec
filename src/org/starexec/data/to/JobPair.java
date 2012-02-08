package org.starexec.data.to;


/**
 * Represents a job pair which is a config/benchmark mapping used in jobss
 * @author Clifton Palmer
 */
public class JobPair extends Identifiable {	
	private int configId;
	private int benchmarkId;
	
	public void setConfigId(int configId) {
		this.configId = configId;
	}
	
	public int getConfigId() {
		return configId;
	}
	
	public void setBenchmarkId(int benchmarkId) {
		this.benchmarkId = benchmarkId;
	}
	
	public int getBenchmarkId() {
		return benchmarkId;
	}
}
