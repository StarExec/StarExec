package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Represents a status for a job
 * @author Eric Burns
 */
public class JobStatus {
	public static enum JobStatusCode {
		STATUS_UNKNOWN(0),
		STATUS_COMPLETE(1),
		STATUS_RUNNING(2),
		STATUS_PAUSED(3),
		STATUS_KILLED(4),
		STATUS_PROCESSING(5);		
		private int val;
		
		private JobStatusCode(int val) {
			this.val = val;			
		}				
		
		public int getVal() {
			return this.val;			
		}				
		public boolean error() {
		    return (val >= 8 && val <= 18) || val == 0;
		}
		public boolean incomplete() {
		    return (val<=6 || val==19 || val == 20 || val == 21);
		}
		public boolean complete() {
		    return val==7;
		}
		public static JobStatusCode toStatusCode(int code) {
			
		    switch (code) {
		    case 0:
		    	return JobStatusCode.STATUS_UNKNOWN;
		    case 1:
		    	return JobStatusCode.STATUS_COMPLETE;
		    case 2: 
		    	return JobStatusCode.STATUS_RUNNING;
		    case 3:
		    	return JobStatusCode.STATUS_PAUSED;
		    case 4:
		    	return JobStatusCode.STATUS_KILLED;
		    case 5:
		    	return JobStatusCode.STATUS_PROCESSING;
		    }
		    return JobStatusCode.STATUS_UNKNOWN;
		}
	}


	private static String getDescription(int code) {
		switch (code) {
		    case 1:
			return "this job has finished running";
		    case 2:
			return "this job is still running on the grid engine";
		    case 3:
			return "this job has been paused by the owner";
		    case 4:
			return "this job was killed by the owner prior to being completed";
		    case 5:
			return "this job's output is currently being processed by a new post processor";
		   
	    }
		return "the job status is not known or has not been set";
	}
	private static String getStatus(int code) {
		switch (code) {
		    case 1:
			return "complete";
		    case 2:
			return "incomplete";
		    case 3:
			return "paused";
		    case 4:
			return "killed";
		    case 5:
			return "processing";
		   
	    }
		return "unknown";
	}
	


	@Expose private JobStatusCode code = JobStatusCode.STATUS_UNKNOWN;

	
	/**
	 * @return the status code for this status
	 */
	public JobStatusCode getCode() {
		return code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(JobStatusCode code) {
		this.code = code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(int code) {
	    this.code = JobStatusCode.toStatusCode(code);
	}

	/**
	 * @return the canonical status
	 */
	public String getStatus() {
		return JobStatus.getStatus(this.code.getVal());
	}
	
	/**
	 * @return the description of what the status means
	 */
	public String getDescription() {
		return JobStatus.getDescription(this.code.getVal());
	}
	@Override
	public String toString() {
		return this.getStatus();
	}
}
