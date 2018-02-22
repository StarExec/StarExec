package org.starexec.test.integration.security;


import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

import java.util.ArrayList;
import java.util.List;

public class SolverSecurityTests extends TestSequence {
	User admin=null;
	User owner=null;
	User regular=null;

	//no solvers are in any spaces
	Solver solver=null;  //
	Solver solver2=null; // these two are owned by 'owner'
	Solver solver3=null; // this one is owned by 'regular'

	Configuration c =null; //this configuration goes with 'solver'
	Configuration c2=null;
	Configuration c3=null;

	Space tempCommunity=null; //led by 'owner'
	Space tempCommunity2=null; //led by 'regular'
	@StarexecTest
	private void deleteSolverPermissionTest() {
		Assert.assertTrue(SolverSecurity.canUserDeleteSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteSolver(solver.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void canDeleteSolversTest() {
		List<Integer> ids= new ArrayList<>();
		ids.add(solver.getId());
		ids.add(solver2.getId());

		Assert.assertTrue(SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteSolvers(ids, admin.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());

		ids.add(solver3.getId());

		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteSolvers(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteSolvers(ids, owner.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserSeeBuildLog() {
		Assert.assertTrue(SolverSecurity.canUserSeeBuildLog(solver.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserSeeBuildLog(solver.getId(), admin.getId()).isSuccess());

		Assert.assertNotEquals(true, SolverSecurity.canUserSeeBuildLog(solver.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void canDeleteConfigurationsTest() {
		List<Integer> ids= new ArrayList<>();
		ids.add(c.getId());
		ids.add(c2.getId());

		Assert.assertTrue(SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteConfigurations(ids, admin.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());

		ids.add(c3.getId());

		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteConfigurations(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteConfigurations(ids, owner.getId()).isSuccess());

	}

	@StarexecTest
	private void canRecycleSolversTest() {
		List<Integer> ids= new ArrayList<>();
		ids.add(solver.getId());
		ids.add(solver2.getId());

		Assert.assertTrue(SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRecycleSolvers(ids, admin.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());

		ids.add(solver3.getId());

		Assert.assertNotEquals(true,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRecycleSolvers(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRecycleSolvers(ids, owner.getId()).isSuccess());
	}

	@StarexecTest
	private void canRestoreSolversTest() {
		List<Integer> ids= new ArrayList<>();
		ids.add(solver.getId());
		ids.add(solver2.getId());

		Assert.assertTrue(SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRestoreSolvers(ids, admin.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());

		ids.add(solver3.getId());

		Assert.assertNotEquals(true,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRestoreSolvers(ids, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRestoreSolvers(ids, owner.getId()).isSuccess());
	}

	@StarexecTest
	private void canDownloadSolverTest() {
		Assert.assertTrue(SolverSecurity.canUserDownloadSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDownloadSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertFalse(SolverSecurity.canUserDownloadSolver(solver.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void recycleSolverPermissionTest() {
		Assert.assertTrue(SolverSecurity.canUserRecycleSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRecycleSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRecycleSolver(solver.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void restoreSolverPermissionTest() {
		Assert.assertTrue(SolverSecurity.canUserRestoreSolver(solver.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRestoreSolver(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRestoreSolver(solver.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserUpdateSolver() {
		Assert.assertTrue(SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserUpdateConfiguration() {
		Assert.assertTrue(SolverSecurity.canUserUpdateConfiguration(c.getId(), owner.getId(), c.getName(), c.getDescription()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserUpdateConfiguration(c.getId(), admin.getId(), c.getName(), c.getDescription()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserUpdateConfiguration(c.getId(), regular.getId(), c.getName(), c.getDescription()).isSuccess());
	}

	@StarexecTest
	private void canUserAddConfigurationTest() {
		Assert.assertTrue(SolverSecurity.canUserAddConfiguration(solver.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserAddConfiguration(solver.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserAddConfiguration(solver.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserDeleteConfigurationTest() {
		Assert.assertTrue(SolverSecurity.canUserDeleteConfiguration(c.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserDeleteConfiguration(c.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserDeleteConfiguration(c.getId(), regular.getId()).isSuccess());
	}

	@StarexecTest
	private void canRunStarexecBuildTest() {
		Assert.assertTrue(SolverSecurity.canUserRunStarexecBuild(owner.getId(), tempCommunity.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRunStarexecBuild(admin.getId(), tempCommunity.getId()).isSuccess());
		Assert.assertNotEquals(true,SolverSecurity.canUserRunStarexecBuild(owner.getId(), tempCommunity2.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserRecycleOrphanedSolversTest() {
		Assert.assertTrue(SolverSecurity.canUserRecycleOrphanedSolvers(owner.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SolverSecurity.canUserRecycleOrphanedSolvers(owner.getId(), admin.getId()).isSuccess());
		Assert.assertFalse(SolverSecurity.canUserRecycleOrphanedSolvers(admin.getId(), owner.getId()).isSuccess());
		Assert.assertFalse(SolverSecurity.canUserRecycleOrphanedSolvers(owner.getId(), regular.getId()).isSuccess());
	}

	@Override
	protected void setup() throws Exception {
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		owner=loader.loadUserIntoDatabase();
		regular=loader.loadUserIntoDatabase();

		tempCommunity=loader.loadSpaceIntoDatabase(owner.getId(), 1);
		tempCommunity2=loader.loadSpaceIntoDatabase(regular.getId(), 1);

		solver=loader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver2=loader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver3=loader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), regular.getId());

		c=Solvers.getConfigsForSolver(solver.getId()).get(0);
		c2=Solvers.getConfigsForSolver(solver2.getId()).get(0);
		c3=Solvers.getConfigsForSolver(solver3.getId()).get(0);
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

	@Override
	protected String getTestName() {
		return "SolverSecurityTests";
	}
}
