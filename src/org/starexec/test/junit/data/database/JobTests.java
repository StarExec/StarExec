package org.starexec.test.junit.data.database;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.data.database.Jobs;
import org.starexec.data.to.JobStatus;


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
}
