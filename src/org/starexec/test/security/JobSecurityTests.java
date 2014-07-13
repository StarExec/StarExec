package org.starexec.test.security;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.to.Job;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class JobSecurityTests extends TestSequence {
	private Space space=null; //space to put the test job
	private Solver solver=null; //solver to use for the job
	private Job job=null;       
	private Processor postProc=null; //post processor to use for the job
	private List<Integer> benchmarkIds=null; // benchmarks to use for the job
	private User user=null;                  //owner of all the test primitives
	private User nonOwner=null;
	private User admin=null;
	@Test
	private void CanDeleteJob() {
		Assert.assertEquals(0,JobSecurity.canUserDeleteJob(job.getId(), admin.getId()));
		Assert.assertEquals(0,JobSecurity.canUserDeleteJob(job.getId(), user.getId()));
		Assert.assertNotEquals(0,JobSecurity.canUserDeleteJob(job.getId(), nonOwner.getId()));

	}
	
	@Test
	private void CanPauseJob() {
		Assert.assertEquals(0,JobSecurity.canUserPauseJob(job.getId(), admin.getId()));
		Assert.assertEquals(0,JobSecurity.canUserPauseJob(job.getId(), user.getId()));
		Assert.assertNotEquals(0,JobSecurity.canUserPauseJob(job.getId(), nonOwner.getId()));
	}
	
	@Test
	private void CanResumeJob() {
		Assert.assertEquals(0,JobSecurity.canUserResumeJob(job.getId(), admin.getId()));
		Assert.assertEquals(0,JobSecurity.canUserResumeJob(job.getId(), user.getId()));
		Assert.assertNotEquals(0,JobSecurity.canUserResumeJob(job.getId(), nonOwner.getId()));
	}
	
	@Test
	private void CanChangeQueues() {
		//JobSecurity.canChangeQueue(jobId, userId, queueId)
	}
	@Test
	private void CanPauseAllJobs() {
		Assert.assertEquals(0,JobSecurity.canUserPauseAllJobs(admin.getId()));
		Assert.assertNotEquals(0,JobSecurity.canUserPauseAllJobs(user.getId()));
		Assert.assertNotEquals(0,JobSecurity.canUserPauseAllJobs(nonOwner.getId()));
	}
	
	@Override
	protected String getTestName() {
		return "JobSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user=ResourceLoader.loadUserIntoDatabase();
		nonOwner=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());
		
		List<Integer> solverIds=new ArrayList<Integer>();
		solverIds.add(solver.getId());
		job=ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,100,100,1);
		Assert.assertNotNull(Jobs.get(job.getId()));
		
	}

	@Override
	protected void teardown() throws Exception {
		Jobs.delete(job.getId());
		Solvers.delete(solver.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.delete(i);
		}
		Processors.delete(postProc.getId());
		Spaces.removeSubspaces(space.getId(), Communities.getTestCommunity().getId(), user.getId());
		Users.deleteUser(user.getId(), admin.getId());
		Users.deleteUser(nonOwner.getId(),admin.getId());
	}

}
