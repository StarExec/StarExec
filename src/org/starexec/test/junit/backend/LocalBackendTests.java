package org.starexec.test.junit.backend;

import org.junit.Before;
import org.junit.Test;
import org.starexec.backend.Backend;
import org.starexec.backend.LocalBackend;
import org.starexec.constants.R;
import org.testng.Assert;

public class LocalBackendTests {

	private Backend backend = null;
	int existingJobId = 0;
	@Before
	public void initialize() {
		backend = new LocalBackend();
		existingJobId = backend.submitScript("fake job", "/test", "/log"); 
	}
	
	@Test
	public void testSubmitJob() {
		int newId = backend.submitScript("new job", "/test", "/log");
		Assert.assertTrue(backend.getRunningJobsStatus().contains(newId+""));
		Assert.assertTrue(backend.getRunningJobsStatus().contains("new job"));

	}
	
	@Test
	public void killJobTest() {
		Assert.assertTrue(backend.killPair(existingJobId));
		Assert.assertEquals(backend.getRunningJobsStatus(), "");
	}
	
	@Test
	public void killNonExistantJobTest() {
		Assert.assertTrue(backend.killPair(-1));
		Assert.assertNotEquals(backend.getRunningJobsStatus(), "");
	}
	
	@Test
	public void killAllJobsTest() {
		backend.submitScript("new job", "/test", "/log");
		Assert.assertTrue(backend.killAll());
		Assert.assertEquals(backend.getRunningJobsStatus(), "");
	}
	
	@Test
	public void getRunningJobsStatusTest() {
		Assert.assertEquals(backend.getRunningJobsStatus(), "fake job "+existingJobId+" pending\n");
	}
	
	@Test
	public void getQueuesTest() {
		String[] queues = backend.getQueues();
		Assert.assertEquals(queues.length, 1);
		Assert.assertEquals(queues[0], R.DEFAULT_QUEUE_NAME);
	}
}
