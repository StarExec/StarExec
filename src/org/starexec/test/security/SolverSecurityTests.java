package org.starexec.test.security;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.database.Websites.WebsiteType;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.SpaceSecurity;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;


//TODO: Write tests for configurations
public class SolverSecurityTests extends TestSequence {
	User admin=null;
	User owner=null;
	User regular=null;
	
	Solver solver=null;
	Solver solver2=null;
	Solver solver3=null;
	@Test
	private void deleteSolverPermissionTest() {
		Assert.assertEquals(0,SolverSecurity.canUserDeleteSolver(solver.getId(), owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserDeleteSolver(solver.getId(), admin.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserDeleteSolver(solver.getId(), regular.getId()));
	}
	
	@Test
	private void canDeleteSolversTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(solver.getId());
		ids.add(solver2.getId());
		
		Assert.assertEquals(0,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserDeleteSolvers(ids, admin.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()));
		
		ids.add(solver3.getId());
		
		Assert.assertNotEquals(0,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserDeleteSolvers(ids, admin.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()));
		
	}
	
	@Test
	private void canRecycleSolversTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(solver.getId());
		ids.add(solver2.getId());
		
		Assert.assertEquals(0,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRecycleSolvers(ids, admin.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()));
		
		ids.add(solver3.getId());
		
		Assert.assertNotEquals(0,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRecycleSolvers(ids, admin.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()));
	}
	
	@Test
	private void canRestoreSolversTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(solver.getId());
		ids.add(solver2.getId());
		
		Assert.assertEquals(0,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRestoreSolvers(ids, admin.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()));
		
		ids.add(solver3.getId());
		
		Assert.assertNotEquals(0,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRestoreSolvers(ids, admin.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()));
	}
	
	@Test
	private void canAssociateWebsite() {
		Assert.assertEquals(0, SolverSecurity.canAssociateWebsite(solver.getId(), owner.getId(),"new"));
		Assert.assertEquals(0, SolverSecurity.canAssociateWebsite(solver.getId(), admin.getId(),"new"));
		Assert.assertNotEquals(0, SolverSecurity.canAssociateWebsite(solver.getId(), regular.getId(),"new"));
		
		Assert.assertNotEquals(0, SolverSecurity.canAssociateWebsite(solver.getId(), owner.getId(),"<script>"));
		Assert.assertNotEquals(0, SolverSecurity.canAssociateWebsite(solver.getId(), admin.getId(),"<script>"));
	}
	
	@Test
	private void CanDeleteWebsiteTest() {
		Websites.add(solver.getId(), "https://www.fake.edu", "new", WebsiteType.SOLVER);
		int websiteId=Websites.getAll(solver.getId(), WebsiteType.SPACE).get(0).getId();
		Assert.assertEquals(0,SolverSecurity.canDeleteWebsite(solver.getId(), websiteId, owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canDeleteWebsite(solver.getId(), websiteId, admin.getId()));

		Assert.assertNotEquals(0,SolverSecurity.canDeleteWebsite(solver.getId(), websiteId, regular.getId()));
		
		Assert.assertNotEquals(0,SolverSecurity.canDeleteWebsite(solver.getId(), -1, owner.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canDeleteWebsite(solver.getId(), -1, admin.getId()));
	}
	
	
	@Test
	private void recycleSolverPermissionTest() {
		Assert.assertEquals(0,SolverSecurity.canUserRecycleSolver(solver.getId(), owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRecycleSolver(solver.getId(), admin.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserRecycleSolver(solver.getId(), regular.getId()));
	}
	@Test
	private void restoreSolverPermissionTest() {
		Assert.assertEquals(0,SolverSecurity.canUserRestoreSolver(solver.getId(), owner.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserRestoreSolver(solver.getId(), admin.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserRestoreSolver(solver.getId(), regular.getId()));
	}
	
	@Test
	private void canUserUpdateSolver() {
		Assert.assertEquals(0,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId()));
		Assert.assertEquals(0,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), owner.getId()));
		Assert.assertNotEquals(0,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), regular.getId()));
		//make sure we can't change the name of solver1 to the same name as solver2, since names must be unique per space
		Assert.assertNotEquals(0,SolverSecurity.canUserUpdateSolver(solver.getId(), solver2.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId()));
	}
	
	
	
	@Override
	protected void setup() throws Exception {
		admin=Users.getAdmins().get(0);
		owner=Users.getTestUser();
		regular=ResourceLoader.loadUserIntoDatabase();
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver2=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver3=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), regular.getId());

	}

	@Override
	protected void teardown() throws Exception {
		Solvers.delete(solver.getId());
		Solvers.delete(solver2.getId());
		Solvers.delete(solver3.getId());
		Users.deleteUser(regular.getId(),admin.getId());
	
	}

	@Override
	protected String getTestName() {
		return "SolverSecurityTests";
	}

}
