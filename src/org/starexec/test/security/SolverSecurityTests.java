package org.starexec.test.security;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.database.Websites.WebsiteType;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class SolverSecurityTests extends TestSequence {
	User admin=null;
	User owner=null;
	User regular=null; 
	
	Solver solver=null;  //
	Solver solver2=null; // these two are owned by 'owner'
	Solver solver3=null; // this one is owned by 'regular'
	
	Configuration c =null; //this configuration goes with 'solver'
	Configuration c2=null;
	Configuration c3=null;
	
	Space tempCommunity=null; //led by 'owner'
	Space tempCommunity2=null; //led by 'regular'
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
	private void canUserSeeBuildLog() {
		Assert.assertEquals(true, SolverSecurity.canUserSeeBuildLog(solver.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true, SolverSecurity.canUserSeeBuildLog(solver.getId(), admin.getId()).isSuccess());

		Assert.assertNotEquals(true, SolverSecurity.canUserSeeBuildLog(solver.getId(), regular.getId()).isSuccess());
	}
	
	@Test
	private void canDeleteConfigurationsTest() {
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(c.getId());
		ids.add(c2.getId());
		
		Assert.assertEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, admin.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());
		
		ids.add(c3.getId());
		
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());
		
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
	}
	
	@Test 
	private void canUserUpdateConfiguration() {
		Assert.assertEquals(true,SolverSecurity.canUserUpdateConfiguration(c.getId(), owner.getId(), c.getName(), c.getDescription()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserUpdateConfiguration(c.getId(), admin.getId(), c.getName(), c.getDescription()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserUpdateConfiguration(c.getId(), regular.getId(), c.getName(), c.getDescription()).isSuccess());
		
	}
	
	@Test
	private void canUserAddConfigurationTest() {
		Assert.assertEquals(true,SolverSecurity.canUserAddConfiguration(solver.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserAddConfiguration(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserAddConfiguration(solver.getId(), regular.getId()).isSuccess());
		
	}
	
	@Test
	private void canUserDeleteConfigurationTest() {
		Assert.assertEquals(true,SolverSecurity.canUserDeleteConfiguration(c.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserDeleteConfiguration(c.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteConfiguration(c.getId(), regular.getId()).isSuccess());
	}
	
	@Test
	private void canRunStarexecBuildTest() {
		Assert.assertEquals(true,SolverSecurity.canUserRunStarexecBuild(owner.getId(), tempCommunity.getId()).isSuccess());
		Assert.assertEquals(true,SolverSecurity.canUserRunStarexecBuild(admin.getId(), tempCommunity.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRunStarexecBuild(owner.getId(), tempCommunity2.getId()).isSuccess());
	}
	
	@Test
	private void canUserRecycleOrphanedSolversTest() {
		Assert.assertEquals(true, SolverSecurity.canUserRecycleOrphanedSolvers(owner.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true, SolverSecurity.canUserRecycleOrphanedSolvers(owner.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(false, SolverSecurity.canUserRecycleOrphanedSolvers(admin.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(false, SolverSecurity.canUserRecycleOrphanedSolvers(owner.getId(), regular.getId()).isSuccess());
	}
	
	
	@Override
	protected void setup() throws Exception {
		admin=Users.getAdmins().get(0);
		owner=ResourceLoader.loadUserIntoDatabase();
		regular=ResourceLoader.loadUserIntoDatabase();
		
		tempCommunity=ResourceLoader.loadSpaceIntoDatabase(owner.getId(), 1);
		tempCommunity2=ResourceLoader.loadSpaceIntoDatabase(regular.getId(), 1);

		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver2=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver3=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), regular.getId());
		
		c=Solvers.getConfigsForSolver(solver.getId()).get(0);
		c2=Solvers.getConfigsForSolver(solver2.getId()).get(0);
		c3=Solvers.getConfigsForSolver(solver3.getId()).get(0);

	}
	
	

	@Override
	protected void teardown() throws Exception {
		Solvers.deleteAndRemoveSolver(solver.getId());
		Solvers.deleteAndRemoveSolver(solver2.getId());
		Solvers.deleteAndRemoveSolver(solver3.getId());
		
		Users.deleteUser(regular.getId(),admin.getId());
		Users.deleteUser(owner.getId(), admin.getId());
		
		Spaces.removeSubspaces(tempCommunity.getId(), admin.getId());
		Spaces.removeSubspaces(tempCommunity2.getId(), admin.getId());

	
	}

	@Override
	protected String getTestName() {
		return "SolverSecurityTests";
	}

}
