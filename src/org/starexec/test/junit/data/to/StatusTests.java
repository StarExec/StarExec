package org.starexec.test.junit.data.to;

import java.util.EnumSet;

import static org.junit.Assert.*;
import org.junit.Test;
import org.starexec.data.to.Status.StatusCode;

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
}
