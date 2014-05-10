package org.starexec.test.database;

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
import org.starexec.data.to.Job;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;


//TODO: Test counting functions
public class JobTests extends TestSequence {
	private Space space=null; //space to put the test job
	private Solver solver=null; //solver to use for the job
	private Job job=null;       
	private Processor postProc=null; //post processor to use for the job
	private List<Integer> benchmarkIds=null; // benchmarks to use for the job
	private User user=null;                  //owner of all the test primitives
	private User nonOwner=null;
	private User admin=null;
	private int wallclockTimeout=100;
	private int cpuTimeout=100;
	private int gbMemory=1;
	
	private User user2=null;
	private Job job2=null;
	
	@Test
	private void GetTest() {
		Job testJob=Jobs.get(job.getId());
		Assert.assertNotNull(testJob);
		Assert.assertEquals(testJob.getName(),job.getName());
		
	}
	
	@Test
	private void GetDetailedTest() {
		Job testJob=Jobs.getDetailed(job.getId());
		Assert.assertNotNull(testJob);
		Assert.assertEquals(testJob.getName(),job.getName());
		Assert.assertEquals(testJob.getJobPairs().size(),benchmarkIds.size()); //job is supposed to have one pair per benchmark
	}
	
	@Test
	private void GetByUserTest() {
		List<Job> jobs=Jobs.getByUserId(user.getId());
		Assert.assertEquals(1,jobs.size());
		Assert.assertEquals(jobs.get(0).getName(),job.getName());
		
		Jobs.getByUserId(user2.getId());
		Assert.assertEquals(1,jobs.size());
		Assert.assertEquals(jobs.get(0).getName(),job2.getName());
	}
	
	@Test
	private void GetBySpaceTest() {
		List<Job> jobs=Jobs.getBySpace(space.getId());
		Assert.assertEquals(2,jobs.size());
	}
	
	@Test
	private void GetDirectoryTest() {
		String path=Jobs.getDirectory(job.getId());
		Assert.assertNotNull(path);
	}
	
	@Test
	private void GetTotalCountTest() {
		Assert.assertTrue(Jobs.getJobCount()>0);
		
	}
	
	@Test
	private void GetLogDirectoryTest() {
		String path=Jobs.getLogDirectory(job.getId());
		Assert.assertNotNull(path);
	}
	
	@Test
	private void GetWallclockTimeout() {
		Assert.assertEquals(wallclockTimeout, Jobs.getWallclockTimeout(job.getId()));
	}
	
	@Test
	private void GetCpuTimeout() {
		Assert.assertEquals(cpuTimeout, Jobs.getCpuTimeout(job.getId()));
	}
	
	@Test
	private void GetMemoryLimit() {
		Assert.assertEquals(Util.gigabytesToBytes(gbMemory), Jobs.getMaximumMemory(job.getId()));
	}
	
	
	//TODO: Right now, this test is basically just checking to see if the method throws errors. Since the status of the job
	//changes over time, it's hard to know what the status of the job actually should be
	@Test
	private void GetStatusTest() {
		Assert.assertNotNull(Jobs.getJobStatusCode(job.getId()));
	}
	
	//TODO: This test also just checks for errors
	@Test
	private void IsSystemPaused() {
		Jobs.isSystemPaused();
		
	}
	
	@Test
	private void IsDeletedTest() {
		Assert.assertFalse(Jobs.isJobDeleted(job.getId()));
		
	}
	
	@Test
	private void IsKilledTest() {
		Assert.assertFalse(Jobs.isJobKilled(job.getId()));
		
	}
	
	@Test
	private void IsPausedTest() {
		Assert.assertFalse(Jobs.isJobPaused(job.getId()));
		
	}
	
	@Override
	protected String getTestName() {
		return "JobTests";
	}

	@Override
	protected void setup() throws Exception {
		user=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		nonOwner=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.sh", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());
		
		List<Integer> solverIds=new ArrayList<Integer>();
		job=ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		job2=ResourceLoader.loadJobIntoDatabase(space.getId(), user2.getId(), -1, postProc.getId(), solverIds, benchmarkIds, cpuTimeout, wallclockTimeout, gbMemory);
		Assert.assertNotNull(Jobs.get(job.getId()));
		
	}

	@Override
	protected void teardown() throws Exception {
		Jobs.delete(job.getId());
		Jobs.delete(job2.getId());
		Solvers.delete(solver.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.delete(i);
		}
		Processors.delete(postProc.getId());
		Spaces.removeSubspaces(space.getId(), Communities.getTestCommunity().getId(), user.getId());
		Users.deleteUser(user.getId(), admin.getId());
		Users.deleteUser(user2.getId(),admin.getId());
		Users.deleteUser(nonOwner.getId(),admin.getId());
		
	}

}
