package org.starexec.data.to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * Represents a status for a job pair
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
		STATUS_KILLED(21),
		STATUS_PROCESSING(22);
		
		private int val;
		
		private StatusCode(int val) {
			this.val = val;			
		}		
		
		public int getVal() {
			return this.val;			
		}	
		public String getStatus() {
			return Status.getStatus(this.val);
		}
		public boolean resource() {
		    return (val >= 14 && val <= 17);
		}
		public boolean failed(){
			return ((val>=8 && val<=13) ||val==18);
		}
		public boolean incomplete() {
		    return (val<=6 || val>=19);
		}
		//incomplete as it is defined for stats
		public boolean statIncomplete(){
			return (val!=7 && !(val>=14 &&val<=17));
		}
		public boolean complete() {
		    return val==7 || (val>=14 &&val<=17);
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
			    case 22:
			    return STATUS_PROCESSING;
		    }
		    return STATUS_UNKNOWN;
		}
	}


	private static String getDescription(int code) {
		switch (code) {
		    case 1:
			return "the job has been added to the starexec database but has not been submitted to the grid engine";
		    case 2:
			return "the job has been submitted to the grid engine and is waiting for an available execution host";
		    case 3:
			return "the jobs environment on an execution host is being prepared";
		    case 4:
			return "the job is currently being ran on an execution host";
		    case 5:
			return "the jobs output is being stored and its environment is being cleaned up";
		    case 6:
			return "the job has completed execution and is waiting for its runtime statistics and attributes from the grid engine";
		    case 7:
			return "the job has successfully completed execution and its statistics have been received from the grid engine";
		    case 8:
			return "the job was sent to the grid engine for execution but was rejected. this can indicate that there were no available queues or the grid engine is in an unclean state";
		    case 9:
			return "there was an issue submitting your job to the grid engine. this can be caused be unexpected errors raised by the grid engine";
		    case 10:
			return "the job completed execution but there was a problem accuiring its statistics or attributes from the grid engine";
		    case 11:
			return "the job could not be executed because a valid run script was not present";
		    case 12:
			return "the job could not be executed because the benchmark could not be found";
		    case 13:
			return "the job could not be executed because its execution environment could not be properly set up";
		    case 14:
			return "the job was terminated because it exceeded its run time limit";
		    case 15:
			return "the job was terminated because it exceeded its cpu time limit";
		    case 16:
			return "the job was terminated because it exceeded its file write limit";
		    case 17:
			return "the job was terminated because it exceeded its virtual memory limit";
		    case 18:
			return "an unknown error occurred which indicates a problem at any point in the job execution pipeline";
		    case 19:
			return "the job results are currently being processed";
		    case 20:
		    return "the job is paused so all job_pairs that were not complete were sent to this status";
		    case 21:
		    return "the job was killed, so all job_pairs that were not complete were sent to this status";
		    case 22:
		    return "this job is being processed by a new post-processor, and this pair is awaiting processing";
	    }
		return "the job status is not known or has not been set";
	}
	private static String getStatus(int code) {
		switch (code) {
		    case 1:
			return "pending submission";
		    case 2:
			return "enqueued";
		    case 3:
			return "preparing";
		    case 4:
			return "running";
		    case 5:
			return "finishing";
		    case 6:
			return "awaiting results";
		    case 7:
			return "complete";
		    case 8:
			return "rejected";
		    case 9:
			return "submit failed";
		    case 10:
			return "results error";
		    case 11:
			return "run script error";
		    case 12:
			return "benchmark error";
		    case 13:
			return "environment error";
		    case 14:
			return "timeout (wallclock)";
		    case 15:
			return "timeout (cpu)";
		    case 16:
			return "file write exceeded";
		    case 17:
			return "memout";
		    case 18:
			return "error";
		    case 19:
			return "processing results";
		    case 20:
		    return "paused";
		    case 21:
		    return "killed";
		    case 22:
		    return "awaiting processing";
	    }
		return "unknown";
	}
	
	
	


	@Expose private StatusCode code = StatusCode.STATUS_UNKNOWN;

	
	/**
	 * @return the status code for this status
	 */
	public StatusCode getCode() {
		return code;
	}
	
	/**
	 * Gets all the status codes for which a user is allowed to rerun pairs
	 */
	
	public static List<StatusCode> rerunCodes() {
		Status.StatusCode[] codes=Status.StatusCode.values();
		List<Status.StatusCode> filteredCodes=new ArrayList<Status.StatusCode>();
		for (Status.StatusCode code: codes) {
			if (code.getVal()>6 && code.getVal()<19) {
				filteredCodes.add(code);
			}
		}
		return filteredCodes;
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
		return Status.getStatus(this.code.getVal());
	}
	
	/**
	 * @return the description of what the status means
	 */
	public String getDescription() {
		return Status.getDescription(this.code.getVal());
	}
	@Override
	public String toString() {
		return this.getStatus();
	}
}
