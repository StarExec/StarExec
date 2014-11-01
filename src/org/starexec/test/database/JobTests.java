package org.starexec.test.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.jobs.JobManager;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
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
		Assert.assertEquals(benchmarkIds.size(),testJob.getJobPairs().size()); //job is supposed to have one pair per benchmark
	}
	
	@Test
	private void GetByUserTest() {
		List<Job> jobs=Jobs.getByUserId(user.getId());
		Assert.assertEquals(1,jobs.size());
		Assert.assertEquals(jobs.get(0).getName(),job.getName());
		
		jobs=Jobs.getByUserId(user2.getId());
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
	private void GetCountInSpaceTest() {
		Assert.assertEquals(2,Jobs.getCountInSpace(space.getId()));
	}
	
	@Test
	private void GetCountInSpaceWithQuery() {
		Assert.assertEquals(1,Jobs.getCountInSpace(space.getId(), job.getName()));
		Assert.assertEquals(1,Jobs.getCountInSpace(space.getId(),job2.getName()));
		Assert.assertEquals(0,Jobs.getCountInSpace(space.getId(),TestUtil.getRandomJobName()));
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
	private void PauseAndUnpauseTest() {
		Assert.assertFalse(Jobs.isJobPaused(job.getId()));
		Assert.assertTrue(Jobs.pause(job.getId()));
		Assert.assertTrue(Jobs.isJobPaused(job.getId()));

		Assert.assertTrue(Jobs.resume(job.getId()));
		
		Assert.assertFalse(Jobs.isJobPaused(job.getId()));

	}
	
	@Test 
	private void DeleteJobTest() {
		List<Integer> solverIds=new ArrayList<Integer>();
		solverIds.add(solver.getId());
		Job temp=ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		Assert.assertFalse(Jobs.isJobDeleted(temp.getId()));
		Assert.assertTrue(Jobs.delete(temp.getId()));
		
		
		Assert.assertTrue(Jobs.isJobDeleted(temp.getId()));
		
		Assert.assertTrue(Jobs.deleteAndRemove(temp.getId()));
		
		
	}
	
	@Test
	private void CountPendingPairsTest() {
		int count=Jobs.countPendingPairs(job.getId());
		Assert.assertTrue(count>=0);
	}
	
	@Test
	private void CountIncompletePairsTest() {
		int count=Jobs.countIncompletePairs(job.getId());
		Assert.assertTrue(count>=0);
	}
	
	private HashMap<Integer, List<JobPair>> getPairSetup() {
		HashMap<Integer,List<JobPair>> spacesToPairs=new HashMap<Integer,List<JobPair>>();
		Random rand=new Random();
		int index=0;
		for (int curSpace=1;curSpace<rand.nextInt(60)+50; curSpace++) {
			List<JobPair> pairs=new ArrayList<JobPair>();
			for (int curJobPair=0;curJobPair<rand.nextInt(7);curJobPair++) {
				index++;
				JobPair jp=new JobPair();
				jp.setId(index);
				jp.setJobSpaceId(curSpace);
				pairs.add(jp);
			}
			spacesToPairs.put(curSpace, pairs);
		}
		return spacesToPairs;
	}
	
	private int getTotalSize(HashMap<Integer,List<JobPair>> pairs) {
		int sum=0;
		for (Integer i : pairs.keySet()) {
			sum+=pairs.get(i).size();
		}
		return sum;
	}
	
	@Test
	private void depthFirstAddTest() {
		Job j=new Job();
		HashMap<Integer,List<JobPair>> spacesToPairs = getPairSetup();
		int size=getTotalSize(spacesToPairs);
		JobManager.addJobPairsDepthFirst(j, spacesToPairs);
		Assert.assertEquals(size, j.getJobPairs().size()); // every job pair should be present
		int spaceId=-1;
		int counter=0;
		for (JobPair jp : j) {
			if (jp.getJobSpaceId()!=spaceId) {
				//if we are changing to a new space
				if (spaceId>=0) {
					Assert.assertEquals(spacesToPairs.get(spaceId).size(),counter); // we should see all the pairs in this space
																					//before seeing any other spaces
				}
				spaceId=jp.getJobSpaceId();
				counter=1;
				
			} else {
				counter++;
			}
		}
	}
	
	@Test
	private void roundRobinAddTest() {
		Job j=new Job();
		HashMap<Integer,List<JobPair>> spacesToPairs = getPairSetup();
		int size=getTotalSize(spacesToPairs);
		JobManager.addJobPairsRoundRobin(j, spacesToPairs);
		Assert.assertEquals(size, j.getJobPairs().size()); //every job pair should be present
		HashMap<Integer,Integer> spacesToCounts=new HashMap<Integer,Integer>();
		int max=0;
		for (JobPair jp : j) {
			int space=jp.getJobSpaceId();
			//System.out.println(space);
			if (!spacesToCounts.containsKey(space)) {
				spacesToCounts.put(space, 0);
			}
			Assert.assertFalse(Math.abs(spacesToCounts.get(space)-max)>1);
			spacesToCounts.put(space, spacesToCounts.get(space)+1);
			max=Math.max(max, spacesToCounts.get(space));
		}
	}
	
	@Test
	private void CountTimelessPairsTest() {
		int count=Jobs.countTimelessPairs(job.getId());
		Assert.assertTrue(count>=0);		
	}
	
	//TODO: This only checks for SQL errors right now
	@Test
	private void getPairsByStatusTest() {
		List<Integer> pairs=Jobs.getPairsByStatus(job.getId(), 7);
		Assert.assertNotNull(pairs);
	}
	
	@Test
	private void getTimelessPairsByStatusTest() {
		List<Integer> pairs= Jobs.getTimelessPairsByStatus(job.getId(), 7);
		Assert.assertNotNull(pairs);
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
		postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());
		
		List<Integer> solverIds=new ArrayList<Integer>();
		solverIds.add(solver.getId());
		job=ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		job2=ResourceLoader.loadJobIntoDatabase(space.getId(), user2.getId(), -1, postProc.getId(), solverIds, benchmarkIds, cpuTimeout, wallclockTimeout, gbMemory);
		Assert.assertNotNull(Jobs.get(job.getId()));
		
	}

	@Override
	protected void teardown() throws Exception {
		Jobs.deleteAndRemove(job.getId());
		Jobs.deleteAndRemove(job2.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Processors.delete(postProc.getId());
		Spaces.removeSubspaces(space.getId(), admin.getId());
		Users.deleteUser(user.getId(), admin.getId());
		Users.deleteUser(user2.getId(),admin.getId());
		Users.deleteUser(nonOwner.getId(),admin.getId());
		
	}

}
