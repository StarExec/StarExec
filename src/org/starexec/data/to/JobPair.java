package org.starexec.data.to;


/**
 * Represents a job pair which is a config/benchmark mapping used in jobss
 * @author Clifton Palmer
 */
public class JobPair extends Identifiable {	
	private long configId;
	private long benchmarkId;
	
	public void setConfigId(long configId) {
		this.configId = configId;
	}
	
	public long getConfigId() {
		return configId;
	}
	
	public void setBenchmarkId(long benchmarkId) {
		this.benchmarkId = benchmarkId;
	}
	
	public long getBenchmarkId() {
		return benchmarkId;
	}
}
