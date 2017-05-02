package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Users;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

import java.util.List;

/**
 * Tests for org.starexec.data.database.Processors.java
 * @author Eric
 */
public class ProcessorTests extends TestSequence {
	User user = null;
	private Processor postProc;
	private Space community;
	@Override
	protected String getTestName() {
		return "ProcessorTests";
	}
	
	@StarexecTest
	private void getProcTest() {
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
	private void getPostProcessByUserTest() {
		boolean found = false;
		for (Processor p : Processors.getByUser(user.getId(), ProcessorType.POST)) {
			found = found || p.getId()==postProc.getId();
		}
		Assert.assertTrue(found);
	}
	
	@StarexecTest
	private void getBenchProcessByUserTest() {
		boolean found = false;
		for (Processor p : Processors.getByUser(user.getId(), ProcessorType.BENCH)) {
			found = found || p.getId()==R.NO_TYPE_PROC_ID;
		}
		Assert.assertTrue(found);
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
	
	@StarexecTest
	private void getNoTypeProcessorTest() {
		Processor p = Processors.getNoTypeProcessor();
		Assert.assertNotNull(p);
	}
	
	@StarexecTest
	private void processorExistsTest() {
		Assert.assertTrue(Processors.processorExists(postProc.getId()));
	}
	
	@StarexecTest
	private void processorDoesNotExistTest() {
		Assert.assertFalse(Processors.processorExists(-1));
	}
	
	@StarexecTest
	private void updateFilePathTest() {
		String newPath = "test path postproc";
		String oldPath = postProc.getFilePath();
		Assert.assertTrue(Processors.updateFilePath(postProc.getId(), newPath));
		Assert.assertEquals(newPath, Processors.get(postProc.getId()).getFilePath());
		Assert.assertTrue(Processors.updateFilePath(postProc.getId(), oldPath));

	}

	@Override
	protected void setup() throws Exception {
		user = loader.loadUserIntoDatabase();
		community=Communities.getTestCommunity();
		Users.associate(user.getId(), community.getId());
		postProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, community.getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
		
	}
	
}
