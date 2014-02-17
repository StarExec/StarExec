package org.starexec.test.security;


import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Users;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class SolverSecurityTests extends TestSequence {
	User admin=null;
	User owner=null;
	User regular=null;
	Solver solver=null;
	Solver solver2=null;
	
	@Test
	private void deleteSolverPermissionTest() {
		Assert.assertTrue(SolverSecurity.canUserDeleteSolver(solver.getId(), owner.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserDeleteSolver(solver.getId(), admin.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserDeleteSolver(solver.getId(), regular.getId())>0);
	}
	
	@Test
	private void recycleSolverPermissionTest() {
		Assert.assertTrue(SolverSecurity.canUserRecycleSolver(solver.getId(), owner.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserRecycleSolver(solver.getId(), admin.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserRecycleSolver(solver.getId(), regular.getId())>0);
	}
	@Test
	private void restoreSolverPermissionTest() {
		Assert.assertTrue(SolverSecurity.canUserRestoreSolver(solver.getId(), owner.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserRestoreSolver(solver.getId(), admin.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserRestoreSolver(solver.getId(), regular.getId())>0);
	}
	
	@Test
	private void canUserUpdateSolver() {
		Assert.assertTrue(SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), owner.getId())==0);
		Assert.assertTrue(SolverSecurity.canUserUpdateSolver(solver.getId(), solver.getName(), solver.getDescription(), solver.isDownloadable(), regular.getId())>0);
		//make sure we can't change the name of solver1 to the same name as solver2, since names must be unique per space
		Assert.assertTrue(SolverSecurity.canUserUpdateSolver(solver.getId(), solver2.getName(), solver.getDescription(), solver.isDownloadable(), admin.getId())>0);
	}
	
	
	
	@Override
	protected void setup() throws Exception {
		admin=Users.getAdmins().get(0);
		owner=Users.getTestUser();
		regular=ResourceLoader.loadUserIntoDatabase("temp", "user");
		System.out.println(regular);
		System.out.println(regular.getId());
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
		solver2=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", Communities.getTestCommunity().getId(), owner.getId());
	}

	@Override
	protected void teardown() throws Exception {
		
	}

	@Override
	protected String getTestName() {
		return "SolverSecurityTests";
	}

}
