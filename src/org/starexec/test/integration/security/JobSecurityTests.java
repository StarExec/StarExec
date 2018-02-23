package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

import java.util.ArrayList;
import java.util.List;

public class JobSecurityTests extends TestSequence {
	private Space space=null; //space to put the test job
	private Solver solver=null; //solver to use for the job
	private Job job=null;
	private Processor postProc=null; //post processor to use for the job
	private List<Integer> benchmarkIds=null; // benchmarks to use for the job
	private User user=null;                  //owner of all the test primitives
	private User nonOwner=null;
	private User admin=null;

	@StarexecTest
	private void CanDeleteJob() {
		Assert.assertTrue(JobSecurity.canUserDeleteJob(job.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(JobSecurity.canUserDeleteJob(job.getId(), user.getId()).isSuccess());
		Assert.assertFalse(JobSecurity.canUserDeleteJob(job.getId(), nonOwner.getId()).isSuccess());
	}

	@StarexecTest
	private void CanRerunJobTest() {
		Assert.assertTrue(JobSecurity.canUserRerunPairs(job.getId(), user.getId()).isSuccess());
		Assert.assertTrue(JobSecurity.canUserRerunPairs(job.getId(), admin.getId()).isSuccess());
		Assert.assertFalse(JobSecurity.canUserRerunPairs(job.getId(), nonOwner.getId()).isSuccess());
	}

	@StarexecTest
	private void CanPauseJob() {
		Assert.assertTrue(JobSecurity.canUserPauseJob(job.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(JobSecurity.canUserPauseJob(job.getId(), user.getId()).isSuccess());
		Assert.assertFalse(JobSecurity.canUserPauseJob(job.getId(), nonOwner.getId()).isSuccess());
	}

	@StarexecTest
	private void CanResumeJob() {
		Assert.assertTrue(JobSecurity.canUserResumeJob(job.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(JobSecurity.canUserResumeJob(job.getId(), user.getId()).isSuccess());
		Assert.assertFalse(JobSecurity.canUserResumeJob(job.getId(), nonOwner.getId()).isSuccess());
	}

	@StarexecTest
	private void CanChangeQueues() {
		//JobSecurity.canChangeQueue(jobId, userId, queueId)
	}

	@Override
	protected String getTestName() {
		return "JobSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user=loader.loadUserIntoDatabase();
		nonOwner=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space=loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		solver=loader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		postProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=loader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());

		List<Integer> solverIds= new ArrayList<>();
		solverIds.add(solver.getId());
		job=loader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,100,100,1);
		Assert.assertNotNull(Jobs.get(job.getId()));
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}
}
