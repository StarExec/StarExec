package org.starexec.util.matrixView;


/**
 * Helper class for the matrix view of a job.
 * Represents one element in the matrix.
 * @author Albert Giegerich
 */
public class MatrixElement {
	private final String status;
	private final String cpuTime;
	private final String memUsage;
	private final String wallclock;
	private final String jobPairId;
	private final String uniqueIdentifier;

	public MatrixElement(String status, String cpuTime, String memUsage, String wallclock, Integer jobPairId, String uniqueIdentifier) {
		this.status = status;
		this.cpuTime = cpuTime;
		this.memUsage = memUsage;
		this.wallclock = wallclock;
		this.jobPairId = String.valueOf(jobPairId);
		this.uniqueIdentifier = uniqueIdentifier;
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public String getJobPairId() {
		return jobPairId;
	}

	public String getStatus() {
		return status;
	}

	public String getCpuTime() {
		return cpuTime;
	}

	public String getMemUsage() {
		return memUsage;
	}

	public String getWallclock() {
		return wallclock;
	}
}
