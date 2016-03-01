package org.starexec.test.integration.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Queues;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

/**
 * Tests for org.starexec.data.database.Queues.java
 * @author Eric
 */
public class QueueTests extends TestSequence {
	Queue allQueue=null;
	
	Queue testQueue=null; //a global queue
	WorkerNode n=null;
	@Override
	protected String getTestName() {
		return "QueueTests";
	}

	
	@StarexecTest
	private void getQueueTest() {
		Queue q=Queues.get(allQueue.getId());
		Assert.assertEquals(allQueue.getName(),q.getName());
	}
	
	@StarexecTest
	private void getQueueDetailsTest() {
		Queue q=Queues.getDetails(allQueue.getId());
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
	private void setGlobalTest() {
		Assert.assertTrue(Queues.removeGlobal(testQueue.getId()));
		Assert.assertFalse(Queues.get(testQueue.getId()).getGlobalAccess());
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
		Queue tempQueue=ResourceLoader.loadQueueIntoDatabase(1000,1000);
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
		List<Queue> queues=Queues.getAll();
		Assert.assertNotNull(queues);
		
		for (Queue q : queues) {
			Assert.assertEquals("ACTIVE",q.getStatus());
		}		
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
		Queues.removeQueue(testQueue.getId());
		
		
	}

}
