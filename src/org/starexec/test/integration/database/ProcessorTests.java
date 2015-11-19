package org.starexec.test.integration.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Processors;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Space;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class ProcessorTests extends TestSequence {
	private Processor postProc;
	private Space community;
	@Override
	protected String getTestName() {
		return "ProcessorTests";
	}
	
	@StarexecTest
	private void getProcTest() throws Exception {
		Processor p=Processors.get(postProc.getId());
		Assert.assertNotNull(p);
		Assert.assertEquals(postProc.getName(),p.getName());
	}
	
	@StarexecTest
	private void updateProcName() {
		String oldName=postProc.getName();
		String newName=TestUtil.getRandomSolverName();
		Assert.assertEquals(oldName,Processors.get(postProc.getId()).getName());
		Assert.assertTrue(Processors.updateName(postProc.getId(), newName));
		Assert.assertEquals(newName,Processors.get(postProc.getId()).getName());
		postProc.setName(newName);

	}
	
	@StarexecTest
	private void updateProcDesc() {
		String oldDesc=postProc.getDescription();
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertEquals(oldDesc,Processors.get(postProc.getId()).getDescription());
		Assert.assertTrue(Processors.updateDescription(postProc.getId(), newDesc));
		Assert.assertEquals(newDesc,Processors.get(postProc.getId()).getDescription());
		postProc.setDescription(newDesc);
	}
	
	@StarexecTest
	private void GetByCommunity() {
		List<Processor> procs=Processors.getByCommunity(community.getId(), postProc.getType());
		boolean foundProc=false;
		for (Processor p : procs) {
			if (p.getId()==postProc.getId()) {
				foundProc=true;
			}
		}
		Assert.assertTrue(foundProc);
	}
	
	@StarexecTest
	private void GetAllByType() {
		List<Processor> procs=Processors.getAll(postProc.getType());
		boolean foundProc=false;
		for (Processor p : procs) {
			if (p.getId()==postProc.getId()) {
				foundProc=true;
			}
		}
		Assert.assertTrue(foundProc);
	}

	@Override
	protected void setup() throws Exception {
		community=Communities.getTestCommunity();
		postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, community.getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		Processors.delete(postProc.getId());
		
	}
	
}
