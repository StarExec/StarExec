package org.starexec.test.database;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Queues;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.GridEngineUtil;

public class QueueTests extends TestSequence {
	Queue allQueue=null;
	
	Queue testQueue=null; //a global queue
	WorkerNode n=null;
	@Override
	protected String getTestName() {
		return "QueueTests";
	}

	
	@Test
	private void getQueueTest() {
		Queue q=Queues.get(allQueue.getId());
		Assert.assertEquals(allQueue.getName(),q.getName());
	}
	
	@Test
	private void getQueueDetailsTest() {
		Queue q=Queues.getDetails(allQueue.getId());
		Assert.assertEquals(allQueue.getName(),q.getName());
		
	}
	
	@Test
	private void getIdByNameTest() {
		Assert.assertEquals(allQueue.getId(),Queues.getIdByName(allQueue.getName()));
	}
	
	@Test
	private void getNameByIdTest() {
		Assert.assertEquals(allQueue.getName(),Queues.getNameById(allQueue.getId()));
	}
	
	@Test
	private void setGlobalTest() {
		Assert.assertTrue(Queues.removeGlobal(testQueue.getId()));
		Assert.assertFalse(Queues.get(testQueue.getId()).getGlobalAccess());
		Assert.assertTrue(Queues.makeGlobal(testQueue.getId()));
		Assert.assertTrue(Queues.get(testQueue.getId()).getGlobalAccess());
	}
	
	@Test
	private void updateCpuTimeoutTest() {
		int cpuTimeout=testQueue.getCpuTimeout();
		Assert.assertTrue(Queues.updateQueueCpuTimeout(testQueue.getId(), cpuTimeout+1));
		Assert.assertEquals(cpuTimeout+1,Queues.get(testQueue.getId()).getCpuTimeout());		
	}
	
	@Test
	private void updateWallclockTimeoutTest() {
		int wallTimeout=testQueue.getWallTimeout();
		Assert.assertTrue(Queues.updateQueueWallclockTimeout(testQueue.getId(), wallTimeout+1));
		Assert.assertEquals(wallTimeout+1,Queues.get(testQueue.getId()).getWallTimeout());
	}
	
	@Test
	private void deleteQueueTest() {
		Queue tempQueue=ResourceLoader.loadQueueIntoDatabase(1000,1000);
		Assert.assertNotNull(Queues.get(tempQueue.getId()));
		Assert.assertTrue(GridEngineUtil.removeQueue(tempQueue.getId()));
		Assert.assertNull(Queues.get(tempQueue.getId()));		
	}
	
	@Override
	protected void setup() throws Exception {
		allQueue=Queues.get(Queues.getIdByName(R.DEFAULT_QUEUE_NAME));
		n=Queues.getNodes(allQueue.getId()).get(0);
		testQueue=ResourceLoader.loadQueueIntoDatabase(1000,1000);
		
		Assert.assertNotNull(testQueue);
	}

	@Override
	protected void teardown() throws Exception {
		Queues.delete(testQueue.getId());
		
		
	}

}
