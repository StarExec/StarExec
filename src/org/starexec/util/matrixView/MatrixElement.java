package org.starexec.util.matrixView;

/**
 * Helper class for the matrix view of a job.
 * Represents one element in the matrix.
 * @author Albert Giegerich
 */
public class MatrixElement {
	private String status;
	private String runtime;
	private String memUsage;
	private String wallclock;

	public MatrixElement(String status, String runtime, String memUsage, String wallclock) {
		this.status = status;
		this.runtime = runtime;
		this.memUsage = memUsage;
		this.wallclock = wallclock;
	}

	public String getStatus() {
		return status;
	}

	public String getRuntime() {
		return runtime;
	}

	public String getMemUsage() {
		return memUsage;
	}

	public String getWallclock() {
		return wallclock;
	}
}
