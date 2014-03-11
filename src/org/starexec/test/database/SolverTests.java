package org.starexec.test.database;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;

public class SolverTests extends TestSequence {
	private Solver solver;
	private Space space1, space2, testCommunity;
	User testUser;
	
	@Test
	private void GetSolverTest() {
		Solver cs=Solvers.get(solver.getId());
		Assert.assertNotNull(cs);
		Assert.assertEquals(cs.getId(),solver.getId());
	}
	@Test
	private void GetSolverListTest() {
		List<Integer> list=new ArrayList<Integer>();
		list.add(solver.getId());
		List<Solver> cs=Solvers.get(list);
		Assert.assertEquals(1,cs.size());
		Assert.assertEquals(solver.getId(), cs.get(0).getId());
	}
	
	@Test 
	private void recycleAndRestoreTest() {
		Assert.assertFalse(Solvers.isSolverRecycled(solver.getId()));
		Assert.assertTrue(Solvers.recycle(solver.getId()));
		Assert.assertTrue(Solvers.isSolverRecycled(solver.getId()));
		Assert.assertTrue(Solvers.restore(solver.getId()));
		Assert.assertFalse(Solvers.isSolverRecycled(solver.getId()));
	}
	
	@Test
	private void changeNameTest() {
		String curName=solver.getName();
		Solver cs=Solvers.get(solver.getId());
		Assert.assertEquals(curName, cs.getName());
		String newName=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateDetails(solver.getId(),newName,solver.getDescription(),solver.isDownloadable()));
		cs=Solvers.get(solver.getId());
		Assert.assertEquals(newName, cs.getName());
		solver.setName(newName);
	}
	
	@Test
	private void changeDescTest() {
		String curDesc=solver.getDescription();
		Solver cs=Solvers.get(solver.getId());
		Assert.assertEquals(curDesc, cs.getDescription());
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertTrue(Solvers.updateDetails(solver.getId(),solver.getName(),newDesc,solver.isDownloadable()));
		cs=Solvers.get(solver.getId());
		Assert.assertEquals(newDesc, cs.getDescription());
		solver.setDescription(newDesc);
	}
	
	@Test
	private void changeDownloadableTest() {
		Solver cs=Solvers.get(solver.getId());
		Assert.assertEquals(solver.isDownloadable(), cs.isDownloadable());
		Assert.assertTrue(Solvers.updateDetails(solver.getId(),solver.getName(),solver.getDescription(),!solver.isDownloadable()));
		cs=Solvers.get(solver.getId());
		Assert.assertNotEquals(solver.isDownloadable(), cs.isDownloadable());
		solver.setDownloadable(!solver.isDownloadable());
	}
	
	
	
	@Override
	protected void setup() throws Exception {
		testUser=Users.getTestUser();
		testCommunity=Communities.getTestCommunity();
		space1=ResourceLoader.loadSpaceIntoDatabase(testUser.getId(),testCommunity.getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), testCommunity.getId());
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), testUser.getId());

	}

	@Override
	protected void teardown() {
		Solvers.delete(solver.getId());
		Spaces.removeSubspaces(space1.getId(),Spaces.getParentSpace(space1.getId()),testUser.getId());
		Spaces.removeSubspaces(space2.getId(),Spaces.getParentSpace(space2.getId()),testUser.getId());		
	}
		
	
	@Override
	protected String getTestName() {
		return "SolverTestSequence";
	}

}
