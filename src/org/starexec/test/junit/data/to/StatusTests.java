package org.starexec.test.junit.data.to;

import java.util.EnumSet;

import static org.junit.Assert.*;
import org.junit.Test;
import org.starexec.data.to.Status;
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
	public void completeTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			if (status == StatusCode.STATUS_COMPLETE 
					|| status == StatusCode.EXCEED_RUNTIME 
					|| status == StatusCode.EXCEED_CPU
					|| status == StatusCode.EXCEED_FILE_WRITE 
					|| status == StatusCode.EXCEED_MEM) {
				assertTrue("The "+status.toString()+" status should be complete.", status.complete());
			} else {
				assertFalse("The " + status.toString() + " status should not be complete.", status.complete());
			}
		}
	}

	/*
	@Test
	public void completeTest() {
		for (StatusCode status : EnumSet.allOf(StatusCode.class)) {
			switch(status) {
				case STATUS_COMPLETE:
				case EXCEED_RUNTIME:
				case EXCEED_CPU:
				case EXCEED_FILE_WRITE:
				case EXCEED_MEM:
					assertTrue("The "+status.toString()+" status should be complete.", status.complete());
					break;
				default:
					assertFalse("The "+status.toString()+" status should not be complete.", status.complete());
			}
		}
	}
	*/
}
