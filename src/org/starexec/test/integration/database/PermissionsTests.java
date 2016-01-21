package org.starexec.test.integration.database;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class PermissionsTests extends TestSequence {
	
	
	User owner=null;
	User spaceMember=null;
	User noPerms=null;
	User admin=null;
	User testUser=null;
	
	
	Space space=null;
	
	Solver solver=null;
	Solver solver2=null;
	List<Integer> benchmarks=null;
	Benchmark benchmarkDownloadable=null; //these two benchmarks are the first and second benchmark from the "benchmarks" array
	Benchmark benchmarkNoDownload=null;
	
	@StarexecTest
	private void CanSeeBenchmarkTest() {
		int benchId=benchmarks.get(0);
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, spaceMember.getId()));
		
		Assert.assertFalse(Permissions.canUserSeeBench(benchId, noPerms.getId()));
	}
	
	@StarexecTest
	private void CanSeeBenchmarksTest() {
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, spaceMember.getId()));
		
		Assert.assertFalse(Permissions.canUserSeeBenchs(benchmarks, noPerms.getId()));
	}
	
	@StarexecTest
	private void CanSeeSolverTest() {
		int solverId=solver.getId();
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, spaceMember.getId()));
		Assert.assertFalse(Permissions.canUserSeeSolver(solverId, noPerms.getId()));
	}
	
	@StarexecTest 
	private void CanSeeSolversTest() {
		List<Integer> solvers=new ArrayList<Integer>();
		solvers.add(solver.getId());
		solvers.add(solver2.getId());
		Assert.assertTrue(Permissions.canUserSeeSolvers(solvers, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolvers(solvers, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolvers(solvers, spaceMember.getId()));
		Assert.assertFalse(Permissions.canUserSeeSolvers(solvers, noPerms.getId()));
	}
	
	
	@StarexecTest
	private void GetSpaceDefaultTest() {
		space.getPermission().setAddSolver(true);
		space.getPermission().setAddSpace(false);
		Spaces.updateDetails(admin.getId(), space);
		Permission p=Permissions.getSpaceDefault(space.getId());
		Assert.assertNotNull(p);
		Assert.assertFalse(p.isLeader());
		Assert.assertTrue(p.canAddSolver());
		Assert.assertFalse(p.canAddSpace());
	}
	
	
	@StarexecTest
	private void FullPermissionsTest() {
		Permission p=Permissions.getFullPermission();
		Assert.assertTrue(p.canAddBenchmark());
		Assert.assertTrue(p.canAddSolver());
		Assert.assertTrue(p.canAddUser());
		Assert.assertTrue(p.canAddJob());
		Assert.assertTrue(p.canAddSpace());
		Assert.assertTrue(p.canRemoveBench());
		Assert.assertTrue(p.canRemoveSolver());
		Assert.assertTrue(p.canRemoveJob());
		Assert.assertTrue(p.canRemoveSpace());
		Assert.assertTrue(p.canRemoveUser());
		Assert.assertTrue(p.isLeader());
		
	}
	
	@Override
	protected String getTestName() {
		return "PermissionsTests";
	}

	@Override
	protected void setup() throws Exception {
		owner=ResourceLoader.loadUserIntoDatabase();
		spaceMember=ResourceLoader.loadUserIntoDatabase();
		noPerms=ResourceLoader.loadUserIntoDatabase();
		
		admin=Users.getAdmins().get(0);
		testUser=Users.getTestUser();
		
		space=ResourceLoader.loadSpaceIntoDatabase(admin.getId(), Communities.getTestCommunity().getId());
		
		Users.associate(spaceMember.getId(), space.getId());
		
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), owner.getId());
		solver2=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), owner.getId());
		benchmarks=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), owner.getId());
		benchmarkDownloadable=Benchmarks.get(benchmarks.get(0));
		Benchmarks.updateDetails(benchmarkDownloadable.getId(), benchmarkDownloadable.getName(), 
				benchmarkDownloadable.getDescription(), true, benchmarkDownloadable.getType().getId());
		
		benchmarkNoDownload=Benchmarks.get(benchmarks.get(1));
		Benchmarks.updateDetails(benchmarkNoDownload.getId(), benchmarkNoDownload.getName(), 
				benchmarkNoDownload.getDescription(), false, benchmarkNoDownload.getType().getId());
	}

	@Override
	protected void teardown() throws Exception {
		
		Spaces.removeSubspaces(space.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		Solvers.deleteAndRemoveSolver(solver2.getId());
		for (Integer i : benchmarks) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Users.deleteUser(owner.getId(),admin.getId());
		Users.deleteUser(spaceMember.getId(),admin.getId());
		Users.deleteUser(noPerms.getId(),admin.getId());

		
	}

}
