package org.starexec.test.junit.data.database;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.backend.GridEngineBackend;
import org.starexec.constants.R;
import org.starexec.data.database.Jobs;
import org.starexec.data.to.Job;
import org.starexec.data.to.Queue;
import org.starexec.util.Util;

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jobs.class})
public class JobTests {

	private int jobId = 1;
	private final static String TEST_QUEUE_NAME = "all.q";

	@Before
	public void initialize() {
		PowerMockito.mockStatic(Jobs.class);
	}

	private static Job getTestJob() {
		Job job = new Job();
		Queue queue = new Queue();
		queue.setName(TEST_QUEUE_NAME);
		job.setQueue(queue);
		return job;
	}

	@Test
	public void GetSlotsInJobQueueForSgeTest() {
		Job job = getTestJob();
		R.BACKEND_TYPE=R.SGE_TYPE;
		GridEngineBackend backend = Mockito.mock(GridEngineBackend.class);
		final Integer slots = 1;
		try {
			BDDMockito.given(backend.getSlotsInQueue(any())).willReturn(slots);
			PowerMockito.whenNew(GridEngineBackend.class).withAnyArguments().thenReturn(backend);
			BDDMockito.given(Jobs.getSlotsInJobQueue(any())).willCallRealMethod();
			Assert.assertEquals(Jobs.getSlotsInJobQueue(job), slots.toString());
		} catch (Exception e) {
			Assert.fail("Caught Exception: " + Util.getStackTrace(e));
		}
	}

	@Test
	public void GetSlotsInJobQueueForLocalTest() {
		R.BACKEND_TYPE = R.LOCAL_TYPE;
		Job job = getTestJob();
		BDDMockito.given(Jobs.getSlotsInJobQueue(job)).willCallRealMethod();
		Assert.assertEquals(Jobs.getSlotsInJobQueue(job), R.DEFAULT_QUEUE_SLOTS);
		System.out.println("End GetSlotsInJobQueueForLocalTest");
	}

	@Test
	public void GetSlotsInJobQueueForOarTest() {
		Job job = getTestJob();
		BDDMockito.given(Jobs.getSlotsInJobQueue(job)).willCallRealMethod();
		R.BACKEND_TYPE = R.OAR_TYPE;
		Assert.assertEquals(Jobs.getSlotsInJobQueue(job), R.DEFAULT_QUEUE_SLOTS);
	}
}
