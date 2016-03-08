package org.starexec.test.integration.database;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.data.to.Job;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.SpaceXMLUploadStatus;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

/**
 * Tests for org.starexec.data.database.Permissions.java
 * @author Eric
 */
public class PermissionsTests extends TestSequence {
	User owner=null;		// user that owns primitives created in this test
	User spaceMember=null;  // user that shares a space with prims.
	User noPerms=null; 		// user does not share any space with prims
	User admin=null;
	User developer = null;
	
	Space space=null;
	
	Solver solver=null;
	Solver solver2=null;
	List<Integer> benchmarks=null;
	Benchmark benchmarkDownloadable=null; //these two benchmarks are the first and second benchmark from the "benchmarks" array
	Benchmark benchmarkNoDownload=null;
	Job job = null;
	@StarexecTest
	private void CanSeeBenchmarkTest() {
		int benchId=benchmarks.get(0);
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, spaceMember.getId()));
		Assert.assertTrue(Permissions.canUserSeeBench(benchId, developer.getId()));

		Assert.assertFalse(Permissions.canUserSeeBench(benchId, noPerms.getId()));
	}
	
	@StarexecTest
	private void CanSeeBenchmarksTest() {
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, spaceMember.getId()));
		Assert.assertTrue(Permissions.canUserSeeBenchs(benchmarks, developer.getId()));

		Assert.assertFalse(Permissions.canUserSeeBenchs(benchmarks, noPerms.getId()));
	}
	
	@StarexecTest
	private void CanSeeSolverTest() {
		int solverId=solver.getId();
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, spaceMember.getId()));
		Assert.assertTrue(Permissions.canUserSeeSolver(solverId, developer.getId()));

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
		Assert.assertTrue(Permissions.canUserSeeSolvers(solvers, developer.getId()));

		Assert.assertFalse(Permissions.canUserSeeSolvers(solvers, noPerms.getId()));
	}
	
	@StarexecTest
	private void canSeeJobTest() {
		Assert.assertTrue(Permissions.canUserSeeJob(job.getId(), owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeJob(job.getId(), spaceMember.getId()));
		Assert.assertTrue(Permissions.canUserSeeJob(job.getId(), admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeJob(job.getId(), developer.getId()));

		Assert.assertFalse(Permissions.canUserSeeJob(job.getId(), noPerms.getId()));
	}
	
	@StarexecTest
	private void canSeeSpaceTest() {
		Assert.assertTrue(Permissions.canUserSeeSpace(space.getId(), owner.getId()));
		Assert.assertTrue(Permissions.canUserSeeSpace(space.getId(), spaceMember.getId()));
		Assert.assertTrue(Permissions.canUserSeeSpace(space.getId(), admin.getId()));
		Assert.assertTrue(Permissions.canUserSeeSpace(space.getId(), developer.getId()));
		
		Assert.assertFalse(Permissions.canUserSeeSpace(space.getId(), noPerms.getId()));
	}
	
	@StarexecTest
	private void canSeeRootTest() {
		Assert.assertTrue(Permissions.canUserSeeSpace(1, noPerms.getId()));
	}
	
	@StarexecTest
	private void canSeePublicSpaceTest() {
		Spaces.setPublicSpace(space.getId(), owner.getId(), true, false);
		Assert.assertTrue(Permissions.canUserSeeSpace(space.getId(), noPerms.getId()));
		Spaces.setPublicSpace(space.getId(), owner.getId(), false, false);
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
	
	@StarexecTest
	private void EmptyPermissionsTest() {
		Permission p=Permissions.getFullPermission();
		Assert.assertFalse(p.canAddBenchmark());
		Assert.assertFalse(p.canAddSolver());
		Assert.assertFalse(p.canAddUser());
		Assert.assertFalse(p.canAddJob());
		Assert.assertFalse(p.canAddSpace());
		Assert.assertFalse(p.canRemoveBench());
		Assert.assertFalse(p.canRemoveSolver());
		Assert.assertFalse(p.canRemoveJob());
		Assert.assertFalse(p.canRemoveSpace());
		Assert.assertFalse(p.canRemoveUser());
		Assert.assertFalse(p.isLeader());
	}
	
	@StarexecTest
	private void setPermissionsTest() {
		Permission oldPerms = Permissions.get(spaceMember.getId(), space.getId());
		Permission p = Permissions.getFullPermission();
		p.setAddJob(false);
		p.setRemoveBench(false);
		Assert.assertTrue(Permissions.set(spaceMember.getId(), space.getId(), p));
		Permission newPerms = Permissions.get(spaceMember.getId(), space.getId());
		Assert.assertFalse(newPerms.canAddJob());
		Assert.assertFalse(newPerms.canRemoveBench());
		Assert.assertEquals(2, newPerms.getOffPermissions().size());
		Assert.assertTrue(Permissions.set(spaceMember.getId(), space.getId(), oldPerms));
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
		job = ResourceLoader.loadJobIntoDatabase(space.getId(), owner.getId(), solver.getId(), benchmarks);
		developer = ResourceLoader.loadUserIntoDatabase("test", "dev", "dev@uiowa.edu", "pass", "Iowa", R.DEVELOPER_ROLE_NAME);		
	}

	@Override
	protected void teardown() throws Exception {
		
		Jobs.deleteAndRemove(job.getId());
		Spaces.removeSubspace(space.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		Solvers.deleteAndRemoveSolver(solver2.getId());
		for (Integer i : benchmarks) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Users.deleteUser(owner.getId(),admin.getId());
		Users.deleteUser(spaceMember.getId(),admin.getId());
		Users.deleteUser(noPerms.getId(),admin.getId());
		Users.deleteUser(developer.getId(), admin.getId());
		
	}

}
