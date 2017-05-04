package org.starexec.test.junit.data.to;

import org.junit.Test;
import org.starexec.data.to.Status.StatusCode;

import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StatusTests {

	@Test
	public void runningTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.STATUS_RUNNING) {
				assertTrue(status.running());
			} else {
				assertFalse(status.running());
			}
		}
	}

	@Test
	public void statCompleteTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.STATUS_COMPLETE 
					|| status == StatusCode.EXCEED_RUNTIME 
					|| status == StatusCode.EXCEED_CPU
					|| status == StatusCode.EXCEED_FILE_WRITE 
					|| status == StatusCode.EXCEED_MEM) {
				assertTrue("The "+status.toString()+" status should be complete.", status.statComplete());
			} else {
				assertFalse("The " + status.toString() + " status should not be complete.", status.statComplete());
			}
		}
	}

	@Test
	public void statIncompleteTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.STATUS_COMPLETE
					|| status == StatusCode.EXCEED_RUNTIME
					|| status == StatusCode.EXCEED_CPU
					|| status == StatusCode.EXCEED_FILE_WRITE
					|| status == StatusCode.EXCEED_MEM) {
				assertFalse("The "+status.toString()+" status should be complete.", status.statIncomplete());
			} else {
				assertTrue("The " + status.toString() + " status should not be complete.", status.statIncomplete());
			}
		}
	}

	@Test
	public void resourceTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.EXCEED_RUNTIME
					|| status == StatusCode.EXCEED_CPU
					|| status == StatusCode.EXCEED_FILE_WRITE
					|| status == StatusCode.EXCEED_MEM) {
				assertTrue("The "+status.toString()+" status should be a \"resource out\" status.", status.resource());
			} else {
				assertFalse("The " + status.toString() + " status should not be a \"resource out\" status.", status.resource());
			}
		}
	}

	@Test
	public void failedTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.ERROR_BENCH_DEPENDENCY_MISSING
					|| status == StatusCode.ERROR_GENERAL
					|| status == StatusCode.ERROR_SGE_REJECT
					|| status == StatusCode.ERROR_SUBMIT_FAIL
					|| status == StatusCode.ERROR_RESULTS
					|| status == StatusCode.ERROR_RUNSCRIPT
					|| status == StatusCode.ERROR_BENCHMARK
					|| status == StatusCode.ERROR_DISK_QUOTA_EXCEEDED) {
				assertTrue("The "+status.toString()+" status should be a \"failed\" status.", status.failed());
			} else {
				assertFalse("The "+status.toString()+" status should not be a \"failed\" status.", status.failed());
			}
		}
	}
	@Test
	public void incompleteTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.STATUS_UNKNOWN
					|| status == StatusCode.STATUS_PENDING_SUBMIT
					|| status == StatusCode.STATUS_ENQUEUED
					|| status == StatusCode.STATUS_RUNNING
					|| status == StatusCode.STATUS_PROCESSING_RESULTS
					|| status == StatusCode.STATUS_PAUSED
					|| status == StatusCode.STATUS_KILLED
					|| status == StatusCode.STATUS_PROCESSING
					|| status == StatusCode.STATUS_NOT_REACHED
					|| status == StatusCode.ERROR_BENCH_DEPENDENCY_MISSING) {
				assertTrue("The "+status.toString()+" status should be an \"incomplete\" status.", status.incomplete());
			} else {
				assertFalse("The "+status.toString()+" status should not be a \"incomplete\" status.", status.incomplete());
			}
		}
	}
}
