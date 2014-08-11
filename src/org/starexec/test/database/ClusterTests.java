package org.starexec.test.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;

public class ClusterTests extends TestSequence {
	Queue allQueue=null;
	WorkerNode n=null;
	@Override
	protected String getTestName() {
		return "ClusterTests";
	}
	
	@Test
	private void getIdByNameTest() {
		Assert.assertEquals(n.getId(),Cluster.getNodeIdByName(n.getName()));
	}
	
	@Test
	private void getNameByIdTest() {
		Assert.assertEquals(n.getName(), Cluster.getNodeNameById(n.getId()));
	}
	
	@Test
	private void getAllNodesTest() {
		List<WorkerNode> nodes=Cluster.getAllNodes();
		Assert.assertNotNull(nodes);
		Assert.assertEquals(nodes.size(), Cluster.getNodeCount());
		
	}
	
	@Test
	private void getNodeCountTest() {
		Assert.assertTrue(Cluster.getNodeCount()>0);
	}
	
	@Test
	private void getDefaultQueueIdTest() {
		Assert.assertEquals(allQueue.getId(),Cluster.getDefaultQueueId());
	}
	
	@Test
	private void getQueueForNode() {
		Assert.assertEquals(allQueue.getId(),Cluster.getQueueForNode(n).getId());
	}
	
	@Test
	private void getNodesForQueue() {
		List<WorkerNode> nodes = Cluster.getNodesForQueue(allQueue.getId());
		Assert.assertNotNull(nodes);
		boolean containsNode=false;
		
		for (WorkerNode node : nodes) {
			if (node.getId()==n.getId()) {
				containsNode=true;
			}
		}
		Assert.assertTrue(containsNode);
	}
	
	@Test
	private void getNodeDetailsTest() {
		
		WorkerNode node=Cluster.getNodeDetails(n.getId());
		Assert.assertNotNull(node);
		Assert.assertEquals(node.getName(),n.getName());
	}

	@Override
	protected void setup() throws Exception {
		allQueue=Queues.get(Queues.getIdByName(R.DEFAULT_QUEUE_NAME));
		n=Queues.getNodes(allQueue.getId()).get(0);
		
	}

	@Override
	protected void teardown() throws Exception {
		//nothing to do here
	}
	
}
