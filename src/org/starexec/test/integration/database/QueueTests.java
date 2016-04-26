package org.starexec.test.integration.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Communities;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.data.to.WorkerNode;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.DataTablesQuery;

/**
 * Tests for org.starexec.data.database.Queues.java
 * @author Eric
 */
public class QueueTests extends TestSequence {
	User owner = null;
	User admin = null;
	Queue allQueue=null;
	
	Queue testQueue=null; //a global queue. Has no nodes
	WorkerNode n=null;    // some node from allQueue
	
	WorkerNode fakeNode = null; // fake node that is not actually in the backend
	
	Space space = null;
	Solver solver = null;
	List<Integer> benchmarkIds = null;
	private Job job = null; // will be on test.q
	
	@Override
	protected String getTestName() {
		return "QueueTests";
	}

	@StarexecTest
	private void dontPauseJobsIfRemainingNodesTest() {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(testQueue.getId(), -1);
		Queues.pauseJobsIfNoRemainingNodes(map);
		Assert.assertFalse(Jobs.isJobPaused(job.getId()));
	}
	
	@StarexecTest
	private void associateTest() {
		Assert.assertTrue(Queues.associate(testQueue.getName(), fakeNode.getName()));
		boolean found = false;
		for (WorkerNode n : Queues.getNodes(testQueue.getId())) {
			found = found || n.getName().equals(fakeNode.getName());
		}
		Assert.assertTrue(found);
		refreshFakeNode();
	}
	
	@StarexecTest
	private void pauseJobsIfNoRemainingNodesTest() {
		for (JobPair pair : job.getJobPairs()) {
			JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
		}
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(testQueue.getId(), Cluster.getNodesForQueue(testQueue.getId()).size());
		Queues.pauseJobsIfNoRemainingNodes(map);
		Assert.assertTrue(Jobs.isJobPaused(job.getId()));
		for (JobPair pair : job.getJobPairs()) {
			JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_COMPLETE.getVal());
		}
		Jobs.resume(job.getId());
		
	}
	
	@StarexecTest
	private void getQueueTest() {
		Queue q=Queues.get(allQueue.getId());
		Assert.assertEquals(allQueue.getName(),q.getName());
	}
	
	@StarexecTest
	private void getIdByNameTest() {
		Assert.assertEquals(allQueue.getId(),Queues.getIdByName(allQueue.getName()));
	}
	
	@StarexecTest
	private void getNameByIdTest() {
		Assert.assertEquals(allQueue.getName(),Queues.getNameById(allQueue.getId()));
	}
	
	@StarexecTest
	private void getNullNamebyFakeIdTest() {
		Assert.assertNull(Queues.getNameById(-1));

	}
	
	@StarexecTest
	private void setGlobalTest() {
		Assert.assertTrue(Queues.removeGlobal(testQueue.getId()));
		Assert.assertFalse(Queues.isQueueGlobal(testQueue.getId()));
		Assert.assertTrue(Queues.makeGlobal(testQueue.getId()));
		Assert.assertTrue(Queues.get(testQueue.getId()).getGlobalAccess());
	}
	
	@StarexecTest
	private void updateCpuTimeoutTest() {
		int cpuTimeout=testQueue.getCpuTimeout();
		Assert.assertTrue(Queues.updateQueueCpuTimeout(testQueue.getId(), cpuTimeout+1));
		Assert.assertEquals(cpuTimeout+1,Queues.get(testQueue.getId()).getCpuTimeout());		
	}
	
	@StarexecTest
	private void updateWallclockTimeoutTest() {
		int wallTimeout=testQueue.getWallTimeout();
		Assert.assertTrue(Queues.updateQueueWallclockTimeout(testQueue.getId(), wallTimeout+1));
		Assert.assertEquals(wallTimeout+1,Queues.get(testQueue.getId()).getWallTimeout());
	}
	
	@StarexecTest
	private void deleteQueueTest() {
		Queue tempQueue=loader.loadQueueIntoDatabase(1000,1000);
		Assert.assertNotNull(Queues.get(tempQueue.getId()));
		Assert.assertTrue(Queues.removeQueue(tempQueue.getId()));
		Assert.assertNull(Queues.get(tempQueue.getId()));		
	}
	
	@StarexecTest
	private void notUniqueQueueNameTest() {
		Assert.assertTrue(Queues.notUniquePrimitiveName(testQueue.getName()));
		Assert.assertTrue(Queues.notUniquePrimitiveName(allQueue.getName()));
		
		//random names are long, so they should not appear in the queues just by chance
		for (int x=0;x<10;x++) {
			Assert.assertFalse(Queues.notUniquePrimitiveName(TestUtil.getRandomQueueName()));
		}		
	}
	
	@StarexecTest 
	private void setStatusTest() {
		String oldStatus=Queues.get(testQueue.getId()).getStatus();
		String status=TestUtil.getRandomAlphaString(10);
		Assert.assertTrue(Queues.setStatus(testQueue.getName(),status));
		Assert.assertEquals(status,Queues.get(testQueue.getId()).getStatus());
		
		Assert.assertTrue(Queues.setStatus(testQueue.getName(),oldStatus));
	}
	
	@StarexecTest
	private void getAllActiveTest() {
		List<Queue> queues=Queues.getAllActive();
		Assert.assertNotNull(queues);
		
		for (Queue q : queues) {
			Assert.assertEquals("ACTIVE",q.getStatus());
		}		
	}
	
	@StarexecTest
	private void getUserQueuesAdminTest() {
		List<Queue> queues=Queues.getUserQueues(admin.getId());
		Assert.assertNotNull(queues);
		
		for (Queue q : queues) {
			Assert.assertEquals("ACTIVE",q.getStatus());
		}
	}
	
	@StarexecTest
	private void getUserQueuesTest() {
		List<Queue> queues=Queues.getUserQueues(owner.getId());
		Assert.assertNotNull(queues);
		
		for (Queue q : queues) {
			Assert.assertEquals("ACTIVE",q.getStatus());
		}
	}
	
	
	@StarexecTest
	private void getAllAdminTest() {
		List<Queue> queues=Queues.getAllAdmin();
		Assert.assertNotNull(queues);
		// need to make sure the inactive test queue is present
		boolean found = false;
		for (Queue q : queues) {
			found = found || q.getName().equals(testQueue.getName());
		}
		Assert.assertTrue(found);
	}
	
	@StarexecTest
	private void getCountOfEnqueuedPairsTest() {
		for (JobPair pair : job.getJobPairs()) {
			JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
		}
		Assert.assertEquals(job.getJobPairs().size(), Queues.getCountOfEnqueuedPairsByQueue(testQueue.getId()));
		
		for (JobPair pair : job.getJobPairs()) {
			JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_COMPLETE.getVal());
		}
	}
	
	@StarexecTest
	private void getPairsRunningOnNodeTest() {
		JobPair jp = job.getJobPairs().get(0);
		JobPairs.updatePairExecutionHost(jp.getId(), fakeNode.getId());
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_RUNNING.getVal());
		List<JobPair> pairs = Queues.getPairsRunningOnNode(fakeNode.getId());
		Assert.assertEquals(1, pairs.size());
		Assert.assertEquals(jp.getId(), pairs.get(0).getId());
		
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void getJobPairsForNextClusterPageTest() {
		JobPair jp =job.getJobPairs().get(0);
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_ENQUEUED.getVal());
		
		List<JobPair> pairs = Queues.getJobPairsForNextClusterPage(new DataTablesQuery(0,10,0,true,""), testQueue.getId());
		Assert.assertEquals(1, pairs.size());
		Assert.assertEquals(jp.getId(), pairs.get(0).getId());
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_COMPLETE.getVal());

	}
	
	@StarexecTest
	private void getNodesEmptyTest() {
		Assert.assertEquals(0, Queues.getNodes(testQueue.getId()).size());
	}
	
	@StarexecTest
	private void getPendingJobsTest() {
		JobPair jp =job.getJobPairs().get(0);
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_PENDING_SUBMIT.getVal());
		
		List<Job> jobs = Queues.getPendingJobs(testQueue.getId());
		Assert.assertEquals(1, jobs.size());
		Assert.assertEquals(job.getId(), jobs.get(0).getId());
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void getPendingJobsEmptyTest() {
		Assert.assertEquals(0, Queues.getPendingJobs(testQueue.getId()).size());
	}
	
	private void refreshFakeNode() {
		if (fakeNode!=null) {
			Cluster.deleteNode(fakeNode.getId());
		}
		Cluster.addNodeIfNotExists("faketestnode");
		fakeNode = Cluster.getNodeDetails(Cluster.getNodeIdByName("faketestnode"));
	}
	
	@StarexecTest 
	private void getUserLoadOnEmptyQueueTest() {
		Assert.assertEquals((Integer)0, Queues.getUserLoadOnQueue(testQueue.getId(), owner.getId()));
	}
	
	@StarexecTest
	private void getUserLoadOnQueueTest() {
		JobPair jp =job.getJobPairs().get(0);
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_RUNNING.getVal());
		Assert.assertEquals((Integer)job.getWallclockTimeout(), Queues.getUserLoadOnQueue(testQueue.getId(), owner.getId()));
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void getSizeOfQueueTest() {
		JobPair jp =job.getJobPairs().get(0);
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_ENQUEUED.getVal());
		Assert.assertEquals((Integer)1, Queues.getSizeOfQueue(testQueue.getId()));
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void isQueueGlobalTest() {
		Assert.assertTrue(Queues.isQueueGlobal(allQueue.getId()));
	}
	
	@StarexecTest
	private void setTestQueueTest() {
		int id = Queues.getTestQueue();
		Assert.assertTrue(Queues.setTestQueue(testQueue.getId()));
		Assert.assertEquals(testQueue.getId(), Queues.getTestQueue());
		Assert.assertTrue(Queues.setTestQueue(id));
	}
	
	@StarexecTest
	private void getAllQTest() {
		Queue q = Queues.getAllQ();
		Assert.assertEquals(R.DEFAULT_QUEUE_ID, q.getId());
		Assert.assertEquals(R.DEFAULT_QUEUE_NAME, q.getName());
	}
	
	@StarexecTest
	private void setQueueCommunityAccessTest() {
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(Communities.getTestCommunity().getId());
		Assert.assertTrue(Queues.setQueueCommunityAccess(ids, testQueue.getId()));
	}
	
	@Override
	protected void setup() throws Exception {
		allQueue=Queues.get(Queues.getIdByName(R.DEFAULT_QUEUE_NAME));
		n=Queues.getNodes(allQueue.getId()).get(0);
		testQueue=loader.loadQueueIntoDatabase(1000,1000);
		Queues.setStatus(testQueue.getName(),R.QUEUE_STATUS_INACTIVE);
		owner = loader.loadUserIntoDatabase();
		space = loader.loadSpaceIntoDatabase(owner.getId(), 1);
		solver = loader.loadSolverIntoDatabase(space.getId(), owner.getId());
		benchmarkIds =loader.loadBenchmarksIntoDatabase(space.getId(), owner.getId());
		job = loader.loadJobIntoDatabase(space.getId(), owner.getId(), solver.getId(), benchmarkIds);
		Jobs.changeQueue(job.getId(), testQueue.getId());
		admin = loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		Assert.assertNotNull(testQueue);
		refreshFakeNode();
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
		Cluster.deleteNode(fakeNode.getId());
	}

}
