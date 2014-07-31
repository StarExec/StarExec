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
		Assert.assertEquals(true,SolverSecurity.canUserDeleteSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteSolver(solver.getId(), regular.getId()).isSuccess());
	}
	
	@Test
	private void canDeleteSolversTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(solver.getId());
		ids.add(solver2.getId());
		
		Assert.assertEquals(true,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteSolvers(ids, admin.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
		
		ids.add(solver3.getId());
		
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteSolvers(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
		
	}
	
	@Test
	private void canRecycleSolversTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(solver.getId());
		ids.add(solver2.getId());
		
		Assert.assertEquals(true,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRecycleSolvers(ids, admin.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
		
		ids.add(solver3.getId());
		
		Assert.assertNotEquals(true,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRecycleSolvers(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
	}
	
	@Test
	private void canRestoreSolversTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(solver.getId());
		ids.add(solver2.getId());
		
		Assert.assertEquals(true,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRestoreSolvers(ids, admin.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
		
		ids.add(solver3.getId());
		
		Assert.assertNotEquals(true,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRestoreSolvers(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
	}
	
	@Test
	private void canAssociateWebsite() {
		Assert.assertEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), owner.getId(),"new","http://www.test.url").isSuccess());
		Assert.assertEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), admin.getId(),"new","http://www.test.url").isSuccess());
		Assert.assertNotEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), regular.getId(),"new","http://www.test.url").isSuccess());
		
		Assert.assertNotEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), owner.getId(),"<script>","http://www.test.url").isSuccess());
		Assert.assertNotEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), admin.getId(),"<script>","http://www.test.url").isSuccess());
		
		Assert.assertNotEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), owner.getId(),"new","<script>").isSuccess());
		Assert.assertNotEquals(true, SolverSecurity.canAssociateWebsite(solver.getId(), admin.getId(),"new","<script>").isSuccess());
	}
	
	@Test
	private void CanDeleteWebsiteTest() {
		Websites.add(solver.getId(), "https://www.fake.edu", "new", WebsiteType.SOLVER);
		int websiteId=Websites.getAll(solver.getId(), WebsiteType.SOLVER).get(0).getId();
		Assert.assertEquals(true,SolverSecurity.canDeleteWebsite(solver.getId(), websiteId, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canDeleteWebsite(solver.getId(), websiteId, admin.getId()).isSuccess());

		Assert.assertNotEquals(true,SolverSecurity.canDeleteWebsite(solver.getId(), websiteId, regular.getId()).isSuccess());
		
		Assert.assertNotEquals(true,SolverSecurity.canDeleteWebsite(solver.getId(), -1, owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canDeleteWebsite(solver.getId(), -1, admin.getId()).isSuccess());
	}
	
	
	@Test
	private void recycleSolverPermissionTest() {
		Assert.assertEquals(true,SolverSecurity.canUserRecycleSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRecycleSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRecycleSolver(solver.getId(), regular.getId()).isSuccess());
	}
	@Test
	private void restoreSolverPermissionTest() {
		Assert.assertEquals(true,SolverSecurity.canUserRestoreSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRestoreSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRestoreSolver(solver.getId(), regular.getId()).isSuccess());
	}
	
	@Test
	private void canUserUpdateSolver() {
		Assert.assertEquals(true,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), regular.getId()).isSuccess());
		//make sure we can't change the name of solver1 to the same name as solver2, since names must be unique per space
		Assert.assertNotEquals(true,SolverSecurity.canUserUpdateSolver(solver.getId(), solver2.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId()).isSuccess());
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
