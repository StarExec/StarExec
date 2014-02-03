package org.starexec.test;

import org.junit.Assert;

import org.starexec.constants.R;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;

public class SolverTestSequence extends TestSequence {
	private Solver s;
	private Space space1, space2;
	public SolverTestSequence() {
		setName("SolverTestSequence");
	}
	
	@Test
	private void newGetSolver() {
		Solver cs=Solvers.get(s.getId());
		Assert.assertNotNull(cs);
		Assert.assertEquals(cs.getId(),s.getId());
		
	}
	
	@Test
	private void changeNameTest() {
		String curName=s.getName();
		Solver cs=Solvers.get(s.getId());
		Assert.assertEquals(curName, cs.getName());
		String newName=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateDetails(s.getId(),newName,s.getDescription(),s.isDownloadable()));
		cs=Solvers.get(s.getId());
		Assert.assertEquals(newName, cs.getName());
		s.setName(newName);
	}
	
	@Test
	private void changeDescTest() {
		String curDesc=s.getDescription();
		Solver cs=Solvers.get(s.getId());
		Assert.assertEquals(curDesc, cs.getDescription());
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateDetails(s.getId(),s.getName(),newDesc,s.isDownloadable()));
		cs=Solvers.get(s.getId());
		Assert.assertEquals(newDesc, cs.getName());
		s.setDescription(newDesc);
	}
	
	@Test
	private void changeDownloadableTest() {
		Solver cs=Solvers.get(s.getId());
		Assert.assertEquals(s.isDownloadable(), cs.isDownloadable());
		Assert.assertTrue(Solvers.updateDetails(s.getId(),s.getName(),s.getDescription(),!s.isDownloadable()));
		cs=Solvers.get(s.getId());
		Assert.assertNotEquals(s.isDownloadable(), cs.isDownloadable());
		s.setDownloadable(!s.isDownloadable());
	}
	
	
	@Override
	protected void setup() throws Exception {
		s=new Solver();
		s.setName(TestUtil.getRandomSolverName());
		s.setDescription("new solver");
		s.setUserId(R.ADMIN_USER_ID);
		s.setPath("fake path");
		Space newCommunity = new Space();
		newCommunity.setDescription("test desc");
		newCommunity.setName(TestUtil.getRandomSpaceName());
		int id=Spaces.add(newCommunity, 1, R.ADMIN_USER_ID);
		Assert.assertNotEquals(-1, id);
		space1=newCommunity;
		space1.setId(id);
		id=Solvers.add(s, id);
		Assert.assertNotEquals(id, -1);
		s.setId(id);
	}

	@Override
	protected void teardown() {
		Solvers.delete(s.getId());
		Spaces.removeSubspaces(space1.getId(),Spaces.getParentSpace(space1.getId()),R.ADMIN_USER_ID);
		
	}

}
