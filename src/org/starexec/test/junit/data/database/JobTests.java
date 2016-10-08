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
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.Queue;
import org.starexec.util.Util;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Matchers.any;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Jobs.class})
public class JobTests {

	private int jobId = 1;
	@Before
	public void initialize() {
		PowerMockito.mockStatic(Jobs.class);

	}
	
	@Test
	public void GetStatusPausedTest() {
        BDDMockito.given(Jobs.isJobPausedOrKilled(jobId)).willReturn(1);
        BDDMockito.given(Jobs.getJobStatusCode(jobId)).willCallRealMethod();
        Assert.assertEquals(JobStatus.JobStatusCode.STATUS_PAUSED, Jobs.getJobStatusCode(jobId).getCode());
	}
	@Test
	public void GetStatusKilledTest() {
		BDDMockito.given(Jobs.isJobPausedOrKilled(jobId)).willReturn(2);
	    BDDMockito.given(Jobs.getJobStatusCode(jobId)).willCallRealMethod();
	    Assert.assertEquals(JobStatus.JobStatusCode.STATUS_KILLED, Jobs.getJobStatusCode(jobId).getCode());
	}
	@Test
	public void GetStatusProcessingTest() {
		BDDMockito.given(Jobs.isJobPausedOrKilled(jobId)).willReturn(0);
		BDDMockito.given(Jobs.hasProcessingPairs(jobId)).willReturn(true);
	    BDDMockito.given(Jobs.getJobStatusCode(jobId)).willCallRealMethod();
	    Assert.assertEquals(JobStatus.JobStatusCode.STATUS_PROCESSING, Jobs.getJobStatusCode(jobId).getCode());
	}
	@Test
	public void GetStatusCompleteTest() {
		BDDMockito.given(Jobs.isJobPausedOrKilled(jobId)).willReturn(0);
		BDDMockito.given(Jobs.hasProcessingPairs(jobId)).willReturn(false);
		BDDMockito.given(Jobs.countIncompletePairs(jobId)).willReturn(0);
	    BDDMockito.given(Jobs.getJobStatusCode(jobId)).willCallRealMethod();
	    Assert.assertEquals(JobStatus.JobStatusCode.STATUS_COMPLETE, Jobs.getJobStatusCode(jobId).getCode());
	}
	@Test
	public void GetStatusRunningTest() {
		BDDMockito.given(Jobs.isJobPausedOrKilled(jobId)).willReturn(0);
		BDDMockito.given(Jobs.hasProcessingPairs(jobId)).willReturn(false);
		BDDMockito.given(Jobs.countIncompletePairs(jobId)).willReturn(1);
	    BDDMockito.given(Jobs.getJobStatusCode(jobId)).willCallRealMethod();
	    Assert.assertEquals(JobStatus.JobStatusCode.STATUS_RUNNING, Jobs.getJobStatusCode(jobId).getCode());
	}

	private final static String TEST_QUEUE_NAME = "all.q";

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
		GridEngineBackend backend = Mockito.spy(new GridEngineBackend());
		final Integer slots = 1;
		try {
			BDDMockito.given(backend.getSlotsInQueue(TEST_QUEUE_NAME)).willReturn(Optional.of(slots));
			Assert.assertEquals(Jobs.getSlotsInJobQueue(job), slots.toString());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@Test
	public void GetSlotsInJobQueueForLocalTest() {
		R.BACKEND_TYPE = R.LOCAL_TYPE;
		Job job = getTestJob();
		Assert.assertEquals(Jobs.getSlotsInJobQueue(job), R.DEFAULT_QUEUE_SLOTS);
	}

	@Test
	public void GetSlotsInJobQueueForOarTest() {
		R.BACKEND_TYPE = R.OAR_TYPE;
		Job job = getTestJob();
		Assert.assertEquals(Jobs.getSlotsInJobQueue(job), R.DEFAULT_QUEUE_SLOTS);
	}
}
