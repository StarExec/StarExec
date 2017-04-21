package org.starexec.test.junit.data.to;

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;

public class StatusTests {

	@Test
	public void runningStatusIsRunning() {
		StatusCode running = StatusCode.STATUS_RUNNING;
		assertTrue(running.running());
	}	

	
}
