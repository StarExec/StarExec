package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Represents a status for a job
 * @author Tyler Jensen
 */
public class Status {
	public static enum StatusCode {
		STATUS_UNKNOWN(0),
		STATUS_PENDING_SUBMIT(1),
		STATUS_ENQUEUED(2),
		STATUS_PREPARING(3),
		STATUS_RUNNING(4),
		STATUS_FINISHING(5),
		STATUS_WAIT_RESULTS(6),
		STATUS_COMPLETE(7),
		ERROR_SGE_REJECT(8),
		ERROR_SUBMIT_FAIL(9),
		ERROR_RESULTS(10),
		ERROR_RUNSCRIPT(11),
		ERROR_BENCHMARK(12),
		ERROR_ENVIRONMENT(13),
		EXCEED_RUNTIME(14),
		EXCEED_CPU(15),
		EXCEED_FILE_WRITE(16),
		EXCEED_MEM(17),
		ERROR_GENERAL(18),
		STATUS_PROCESSING_RESULTS(19),
		STATUS_PAUSED(20),
		STATUS_KILLED(21);
		
		private int val;
		
		private StatusCode(int val) {
			this.val = val;			
		}				
		
		public int getVal() {
			return this.val;			
		}				
		public boolean error() {
		    return (val >= 8 && val <= 18) || val == 0;
		}
		public boolean incomplete() {
		    return (val<=6 || val==19);
		}
		public boolean complete() {
		    return val==7;
		}
		static public StatusCode toStatusCode(int code) {
		    switch (code) {
		    case 1:
			return STATUS_PENDING_SUBMIT;
		    case 2:
			return STATUS_ENQUEUED;
		    case 3:
			return STATUS_PREPARING;
		    case 4:
			return STATUS_RUNNING;
		    case 5:
			return STATUS_FINISHING;
		    case 6:
			return STATUS_WAIT_RESULTS;
		    case 7:
			return STATUS_COMPLETE;
		    case 8:
			return ERROR_SGE_REJECT;
		    case 9:
			return ERROR_SUBMIT_FAIL;
		    case 10:
			return ERROR_RESULTS;
		    case 11:
			return ERROR_RUNSCRIPT;
		    case 12:
			return ERROR_BENCHMARK;
		    case 13:
			return ERROR_ENVIRONMENT;
		    case 14:
			return EXCEED_RUNTIME;
		    case 15:
			return EXCEED_CPU;
		    case 16:
			return EXCEED_FILE_WRITE;
		    case 17:
			return EXCEED_MEM;
		    case 18:
			return ERROR_GENERAL;
		    case 19:
			return STATUS_PROCESSING_RESULTS;
		    case 20:
		    return STATUS_PAUSED;
		    case 21:
		    return STATUS_KILLED;
		    }
		    return STATUS_UNKNOWN;
		}
	}

	@Expose private StatusCode code = StatusCode.STATUS_UNKNOWN;
	@Expose private String status;
	@Expose private String description;
	
	/**
	 * @return the status code for this status
	 */
	public StatusCode getCode() {
		return code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(StatusCode code) {
		this.code = code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(int code) {
	    this.code = StatusCode.toStatusCode(code);
	}

	/**
	 * @return the canonical status
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	
	/**
	 * @return the description of what the status means
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set for this status
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return this.status;
	}
	
	/**
	 * Status code enums and their corresponding database codes
	 * @author Tyler Jensen
	 */
}
