package org.starexec.data.to;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a status for a job pair
 *
 * @author Tyler Jensen
 */

// Do NOT reuse codes 3, 5, and 6! We should always use completely new codes
// for new statuses and never reuse deleted codes.
public class Status {
	private enum StatCompleteness {
		COMPLETE, INCOMPLETE
	}

	public enum StatusCode {
		STATUS_UNKNOWN(0, "the job status is not known or has not been set", "unknown",
		               StatCompleteness.INCOMPLETE
		), STATUS_PENDING_SUBMIT(1,
		                         "the job has been added to the starexec database but has not been submitted to the " +
				                         "grid engine", "pending submission", StatCompleteness.INCOMPLETE
		), STATUS_ENQUEUED(2,
		                   "the job has been submitted to the grid engine and is waiting for an available execution " +
				                   "host",
		                   "enqueued", StatCompleteness.INCOMPLETE
		), STATUS_RUNNING(4, "the job is currently being ran on an execution host", "running",
		                  StatCompleteness.INCOMPLETE
		), STATUS_COMPLETE(7,
		                   "the job has successfully completed execution and its statistics have been received from " +
				                   "the grid engine",
		                   "complete", StatCompleteness.COMPLETE
		), ERROR_SGE_REJECT(8, "the job was sent to the grid engine for execution but was rejected. " +
				"this can indicate that there were no available queues or the grid engine is in an unclean state",
		                    "rejected", StatCompleteness.INCOMPLETE
		), ERROR_SUBMIT_FAIL(9, "there was an issue submitting your job to the grid engine. " +
				"this can be caused be unexpected errors raised by the grid engine", "submit failed",
		                     StatCompleteness.INCOMPLETE
		), ERROR_RESULTS(10,
		                 "the job completed execution but there was a problem acquiring its statistics or attributes " +
				                 "from the grid engine",
		                 "results error", StatCompleteness.INCOMPLETE
		), ERROR_RUNSCRIPT(11, "the job could not be executed because a valid run script was not present",
		                   "run script error", StatCompleteness.INCOMPLETE
		), ERROR_BENCHMARK(12, "the job could not be executed because the benchmark could not be found",
		                   "benchmark error", StatCompleteness.INCOMPLETE
		), ERROR_DISK_QUOTA_EXCEEDED(13,
		                             "the job could not be executed because its execution environment could not be " +
				                             "properly set up",
		                             "environment error", StatCompleteness.INCOMPLETE
		), EXCEED_RUNTIME(14, "the job was terminated because it exceeded its run time limit", "timeout (wallclock)",
		                  StatCompleteness.COMPLETE
		), EXCEED_CPU(15, "the job was terminated because it exceeded its cpu time limit", "timeout (cpu)",
		              StatCompleteness.COMPLETE
		), EXCEED_FILE_WRITE(16, "the job was terminated because it exceeded its file write limit",
		                     "file write exceeded", StatCompleteness.COMPLETE
		), EXCEED_MEM(17, "the job was terminated because it exceeded its virtual memory limit", "memout",
		              StatCompleteness.COMPLETE
		), ERROR_GENERAL(18,
		                 "an unknown error occurred which indicates a problem at any point in the job execution " +
				                 "pipeline",
		                 "error", StatCompleteness.INCOMPLETE
		), STATUS_PROCESSING_RESULTS(19, "the job results are currently being processed", "processing results",
		                             StatCompleteness.INCOMPLETE
		), STATUS_PAUSED(20, "the job is paused so all job_pairs that were not complete were sent to this status",
		                 "paused", StatCompleteness.INCOMPLETE
		), STATUS_KILLED(21, "the job was killed, so all job_pairs that were not complete were sent to this status",
		                 "killed", StatCompleteness.INCOMPLETE
		), STATUS_PROCESSING(22,
		                     "this job is being processed by a new post-processor, and this pair is awaiting " +
				                     "processing",
		                     "awaiting processing", StatCompleteness.INCOMPLETE
		), STATUS_NOT_REACHED(23, "this stage was not reached because a previous stage had some sort of error",
		                      "stage not reached", StatCompleteness.INCOMPLETE
		), ERROR_BENCH_DEPENDENCY_MISSING(24, "this job pair has a missing benchmark dependency.",
		                                  "benchmark dependency missing", StatCompleteness.INCOMPLETE
		), ERROR_PRE_PROCESSOR(25, "there was an issue with the benchmark pre-processor, and the solver was not run",
		                      "pre-processor error", StatCompleteness.COMPLETE
		), ERROR_POST_PROCESSOR(26, "there was an issue with the post-processor",
		                      "post-processor error", StatCompleteness.COMPLETE
		);

		public final int val;
		private int count;
		public final String description;
		public final String status;
		private final StatCompleteness statComplete;

		StatusCode(
				final int val, final String description, final String status, final StatCompleteness statComplete
		) {
			// Old codes that have been deleted should not be reused.
			if (val == 3 || val == 5 || val == 6) {
				throw new IllegalArgumentException("This code has been deleted and should not be reused.");
			}

			this.val = val;
			this.description = description;
			this.status = status;
			this.statComplete = statComplete;
		}

		public int getVal() {
			return this.val;
		}

		public String getStatus() {
			return Status.getStatus(this.val);
		}

		public void setCount(int c) {
			this.count = c;
		}

		public int getCount() {
			return this.count;
		}

		//incomplete as it is defined for stats
		public boolean statIncomplete() {
			return this.statComplete == StatCompleteness.INCOMPLETE;
		}

		public boolean statComplete() {
			return this.statComplete == StatCompleteness.COMPLETE;
		}

		public boolean finishedRunning() {
			return val >= 7;
		}

		public boolean resource() {
			return (val >= 14 && val <= 17);
		}

		public boolean failed() {
			return ((val >= 8 && val <= 13) || val == 18 || (val >= 24 && val <= 26));
		}

		public boolean incomplete() {
			return (val <= 6 || val >= 19);
		}

		public boolean running() {
			return val == 4;
		}

		// Get a StatusCode from an integer representation of a StatusCode
		static public StatusCode toStatusCode(int code) {
			Set<StatusCode> statusCodes = EnumSet.allOf(StatusCode.class);
			for (StatusCode sc : statusCodes) {
				if (sc.getVal() == code) {
					return sc;
				}
			}
			return STATUS_UNKNOWN;
		}
	}

	private static String getDescription(int code) {
		return StatusCode.toStatusCode(code).description;
	}

	private static String getStatus(int code) {
		return StatusCode.toStatusCode(code).status;
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
	 *
	 * @return a list of status codes that are eligible for job pair rerunning
	 */

	public static List<StatusCode> rerunCodes() {
		Status.StatusCode[] codes = Status.StatusCode.values();
		List<Status.StatusCode> filteredCodes = new ArrayList<>();
		for (Status.StatusCode code : codes) {
			if (code.getVal() > 6 && code.getVal() < 19) {
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
