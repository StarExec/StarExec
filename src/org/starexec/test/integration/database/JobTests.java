package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.jobs.JobManager;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Tests for org.starexec.data.database.Jobs.java
 * @author Eric
 */
public class JobTests extends TestSequence {
	private Space space=null; //space to put the test job
	private Solver solver=null; //solver to use for the job
	private Job job=null;       
	private Processor postProc=null; //post processor to use for the job
	private List<Integer> benchmarkIds=null; // benchmarks to use for the job
	private User user=null;                  //owner of all the test primitives
	private int wallclockTimeout=100;
	private int cpuTimeout=100;
	private int gbMemory=1;
	
	private User user2=null;
	private Job job2=null;
	
	@StarexecTest
	private void GetTest() {
		Job testJob=Jobs.get(job.getId());
		Assert.assertNotNull(testJob);
		Assert.assertEquals(testJob.getName(),job.getName());
		
	}

	@StarexecTest
	private void IsHighPriorityTest() {
		try {
			Jobs.setAsHighPriority(job.getId());
			Job testJob = Jobs.get(job.getId());
			Assert.assertTrue(testJob.isHighPriority());
		} catch (SQLException e) {
			Assert.fail("Caught SQL exception while trying to change job priority: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void SetAsLowPrioirtyTest() {
		try {
			Jobs.setAsLowPriority(job.getId());
			Job testJob = Jobs.get(job.getId());
			Assert.assertFalse(testJob.isHighPriority());
		} catch (SQLException e) {
			Assert.fail("Caught SQL exception while trying to change job priority: " + Util.getStackTrace(e));
		}

	}

	@StarexecTest
	private void GetByUserTest() {
		List<Job> jobs=Jobs.getByUserId(user.getId());
		Assert.assertEquals(1,jobs.size());
		Assert.assertEquals(jobs.get(0).getName(),job.getName());
		
		jobs=Jobs.getByUserId(user2.getId());
		Assert.assertEquals(1,jobs.size());
		Assert.assertEquals(jobs.get(0).getName(),job2.getName());
	}
	
	@StarexecTest
	private void GetBySpaceTest() {
		List<Job> jobs=Jobs.getBySpace(space.getId());
		Assert.assertEquals(2,jobs.size());
	}
	
	@StarexecTest
	private void GetDirectoryTest() {
		String path=Jobs.getDirectory(job.getId());
		Assert.assertNotNull(path);
	}
	
	@StarexecTest
	private void GetTotalCountTest() {
		Assert.assertTrue(Jobs.getJobCount()>0);
	}
	
	@StarexecTest
	private void countPairsByUserTest() {
		Assert.assertEquals(job.getJobPairs().size(), Jobs.countPairsByUser(user.getId()));
	}
	
	@StarexecTest
	private void countPairsByFakeUserTest() {
		Assert.assertEquals(0, Jobs.countPairsByUser(-1));
	}
	
	@StarexecTest
	private void GetCountInSpaceTest() {
		Assert.assertEquals(2,Jobs.getCountInSpace(space.getId()));
	}
	
	@StarexecTest
	private void GetCountInSpaceWithQuery() {
		Assert.assertEquals(1,Jobs.getCountInSpace(space.getId(), job.getName()));
		Assert.assertEquals(1,Jobs.getCountInSpace(space.getId(),job2.getName()));
		Assert.assertEquals(0,Jobs.getCountInSpace(space.getId(),TestUtil.getRandomJobName()));
	}
	
	@StarexecTest
	private void GetLogDirectoryTest() {
		String path=Jobs.getLogDirectory(job.getId());
		Assert.assertNotNull(path);
	}
	
	
	
	
	// this just checks to see whether the function throws errors: more detailed logic testing
	// is handled in a unit test
	@StarexecTest
	private void GetStatusTest() {
		Assert.assertNotNull(Jobs.getJobStatusCode(job.getId()));
	}
	
	@StarexecTest
	private void PauseAndResumeAllTest() {
		Assert.assertTrue(Jobs.pauseAll());
		Assert.assertTrue(Jobs.isSystemPaused());
		Assert.assertTrue(Jobs.resumeAll());
		Assert.assertFalse(Jobs.isSystemPaused());
	}
	
	@StarexecTest
	private void IsDeletedTest() {
		Assert.assertFalse(Jobs.isJobDeleted(job.getId()));
		
	}
	
	@StarexecTest
	private void IsKilledTest() {
		Assert.assertFalse(Jobs.isJobKilled(job.getId()));
		
	}
	
	@StarexecTest
	private void PauseAndUnpauseTest() {
		Assert.assertFalse(Jobs.isJobPaused(job.getId()));
		Assert.assertTrue(Jobs.pause(job.getId()));
		Assert.assertTrue(Jobs.isJobPaused(job.getId()));

		Assert.assertTrue(Jobs.resume(job.getId()));
		
		Assert.assertFalse(Jobs.isJobPaused(job.getId()));

	}
	
	@StarexecTest 
	private void DeleteJobTest() {
		List<Integer> solverIds= new ArrayList<>();
		solverIds.add(solver.getId());
		Job temp=loader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		Assert.assertFalse(Jobs.isJobDeleted(temp.getId()));

		try {
			Assert.assertTrue(Jobs.delete(temp.getId()));
			Assert.assertTrue(Jobs.isJobDeleted(temp.getId()));
			Assert.assertTrue(Jobs.deleteAndRemove(temp.getId()));
		} catch (SQLException e) {
			Assert.fail("Caught sql exception while trying to delete job: " + Util.getStackTrace(e));
		}
		
		
	}
	
	@StarexecTest
	private void CountPendingPairsTest() {
		int count=Jobs.countPendingPairs(job.getId());
		Assert.assertTrue(count>=0);
	}
	
	@StarexecTest
	private void CountIncompletePairsTest() {
		int count=Jobs.countIncompletePairs(job.getId());
		Assert.assertTrue(count>=0);
	}
	
	private HashMap<Integer, List<JobPair>> getPairSetup() {
		HashMap<Integer,List<JobPair>> spacesToPairs= new HashMap<>();
		Random rand=new Random();
		int index=0;
		for (int curSpace=1;curSpace<rand.nextInt(60)+50; curSpace++) {
			List<JobPair> pairs= new ArrayList<>();
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
	
	@StarexecTest
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
	
	@StarexecTest
	private void roundRobinAddTest() {
		Job j=new Job();
		HashMap<Integer,List<JobPair>> spacesToPairs = getPairSetup();
		int size=getTotalSize(spacesToPairs);
		JobManager.addJobPairsRoundRobin(j, spacesToPairs);
		Assert.assertEquals(size, j.getJobPairs().size()); //every job pair should be present
		HashMap<Integer,Integer> spacesToCounts= new HashMap<>();
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
	
	@StarexecTest
	private void CountTimelessPairsTest() {
		int count=Jobs.countTimelessPairs(job.getId());
		Assert.assertTrue(count>=0);		
	}
	
	@StarexecTest
	private void getPairsByStatusTest() {
		List<Integer> pairs=Jobs.getPairsByStatus(job.getId(), StatusCode.STATUS_COMPLETE.getVal());
		Assert.assertEquals(job.getJobPairs().size(), pairs.size());
	}
	
	@StarexecTest
	private void getTimelessPairsByStatusTest() {
		List<Integer> pairs= Jobs.getTimelessPairsByStatus(job.getId(), StatusCode.STATUS_COMPLETE.getVal());
		Assert.assertNotNull(pairs);
	}
	
	@StarexecTest
	private void cleanOrphanedDeletedJobTest() {
		Job tempJob = loader.loadJobIntoDatabase(space.getId(), user.getId(), solver.getId(), benchmarkIds);
		List<Integer> job = new ArrayList<>();
		job.add(tempJob.getId());
		try {
			Jobs.delete(tempJob.getId());
		} catch (SQLException e) {
			Assert.fail("Caught SQLException while trying to delete job: " + Util.getStackTrace(e));
		}
		Assert.assertTrue(Jobs.cleanOrphanedDeletedJobs());
		Assert.assertNotNull(Jobs.getIncludeDeleted(tempJob.getId()));
		
		Spaces.removeJobs(job, space.getId());
		
		Assert.assertTrue(Jobs.cleanOrphanedDeletedJobs());
		Assert.assertNull(Jobs.getIncludeDeleted(tempJob.getId()));
	}
	
	@StarexecTest
	private void countOlderPairsTest() {
		int maxCompletionId = 0;
		int minCompletionId = Integer.MAX_VALUE;
		for (JobPair p : job.getJobPairs()) {
			int completionId = JobPairs.getPairDetailed(p.getId()).getCompletionId();
			maxCompletionId = Math.max(maxCompletionId, completionId);
			minCompletionId = Math.min(minCompletionId, completionId);
		}
		Assert.assertEquals(job.getJobPairs().size(), Jobs.countOlderPairs(job.getId(), maxCompletionId));
		Assert.assertEquals(1, Jobs.countOlderPairs(job.getId(), minCompletionId));

	}
	
	@StarexecTest
	private void associateJobsTest() {
		List<Integer> jobIds = new ArrayList<>();
		jobIds.add(job.getId());
		jobIds.add(job2.getId());
		Space newSpace = loader.loadSpaceIntoDatabase(user.getId(), space.getId());
		Assert.assertTrue(Jobs.associate(jobIds, newSpace.getId()));
		Assert.assertEquals(2, Jobs.getBySpace(newSpace.getId()).size());
		
		Spaces.removeSubspace(newSpace.getId());
	}
	
	@StarexecTest
	private void updateNodeTest() {
		JobPair jp = job.getJobPairs().get(0);
		WorkerNode n = Cluster.getAllNodes().get(0);
		Assert.assertTrue(JobPairs.updatePairExecutionHost(jp.getId(), n.getId()));
		Assert.assertEquals(n.getId(), JobPairs.getPairDetailed(jp.getId()).getNode().getId());
		jp.setNode(n);
	}
	
	@Override
	protected String getTestName() {
		return "JobTests";
	}

	@Override
	protected void setup() throws Exception {
		user=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();
		space=loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		solver=loader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		postProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=loader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());
		
		List<Integer> solverIds= new ArrayList<>();
		solverIds.add(solver.getId());
		job=loader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		job2=loader.loadJobIntoDatabase(space.getId(), user2.getId(), -1, postProc.getId(), solverIds, benchmarkIds, cpuTimeout, wallclockTimeout, gbMemory);
		Assert.assertNotNull(Jobs.get(job.getId()));
		
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
		
	}

}
