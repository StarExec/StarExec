package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Represents a status for a job
 * @author Tyler Jensen
 */
public class Status {
	@Expose private int code = 0;
	@Expose private String status;
	@Expose private String description;
	
	/**
	 * @return the status code for this status
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(int code) {
		this.code = code;
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
	public static enum StatusCode {
		STATUS_UNKNOWN(0),
		STATUS_PENDING_SUBMIT(1),
		STATUS_ENQUEUED(2),
		STATUS_PREPARING(3),
		STATUS_RUNNING(4),
		STATUS_FINISHING(5),
		STATUS_WAIT_STATS(6),
		STATUS_COMPLETE(7),
		ERROR_STATS(8),
		ERROR_RUNSCRIPT(9),
		ERROR_BENCHMARK(10),
		ERROR_ENVIRONMENT(11),
		ERROR_GENERAL(12);
		
		private int val;
		
		private StatusCode(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}				
	}
}
