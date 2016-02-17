package org.starexec.test.integration.database;

import java.util.List;

import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.testng.Assert;
/**
 * Tests for Communities.java
 * @author Eric
 *
 */
public class CommunitiesTests extends TestSequence {
	User admin = null;
	
	User testUser = null;
	User user1 = null;
	User user2 = null;
	User user3 = null;
	Space community = null;
	Space subspace1 = null;
	Space subspace2 = null;
	Space subspace3 = null;
	Solver solver1 = null;
	Solver solver2 = null;
	Solver solver3 = null;
	List<Integer> benchmarkIds = null;
	Job job1 = null;
	Job job2 = null;
	@StarexecTest
	private void communityMapUsersTest() {
		Communities.updateCommunityMap();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("users"), new Long(4));
	}
	@StarexecTest
	private void communityMapSolversTest() {
		Communities.updateCommunityMap();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("solvers"), new Long(3));
		Long disk = solver1.getDiskSize() + solver2.getDiskSize() + solver3.getDiskSize();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("disk_usage"), disk);
	}
	
	@StarexecTest
	private void communityMapBenchmarksTest() {
		Communities.updateCommunityMap();
		Long disk = 0l;
		for (Integer i : benchmarkIds) {
			disk+= Benchmarks.get(i).getDiskSize();
		}
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("benchmarks"), new Long(benchmarkIds.size()));
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("disk_usage"), disk);
	}
	
	@StarexecTest
	private void communityMapJobsTest() {
		Communities.updateCommunityMap();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("jobs"), new Long(2));
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("job_pairs"), new Long(job1.getJobPairs().size() + job2.getJobPairs().size()));
	}
	
	@StarexecTest
	private void getDetailsTest() {
		Space s = Communities.getDetails(community.getId());
		Assert.assertEquals(s.getId(), community.getId());
		Assert.assertEquals(s.getName(), community.getName());
		Assert.assertEquals(s.getDescription(), community.getDescription());
		Assert.assertEquals(s.isLocked(), community.isLocked());
		Assert.assertEquals(s.getCreated(), community.getCreated());
	}
	
	@StarexecTest
	private void isCommunityTest() {
		Assert.assertTrue(Communities.isCommunity(community.getId()));
		Assert.assertFalse(Communities.isCommunity(subspace1.getId()));
		Assert.assertFalse(Communities.isCommunity(-1));
	}
	
	@StarexecTest
	private void getDetailsNonCommunityTest() {
		Assert.assertNull(Communities.getDetails(subspace1.getId()));
	}
	
	@StarexecTest
	private void inListOfCommunities() throws Exception {
		List<Space> comms=Communities.getAll();
		for (Space s : comms) {
			if (s.getName().equals(community.getName())) {
				return;
			}
		}
		
		Assert.fail("community was not found in the list of communities");
	}
	
	@StarexecTest
	private void getAllCommunitiesUserIsInTest() {
		List<Space> comms=Communities.getAllCommunitiesUserIsIn(user2.getId());
		for (Space s : comms) {
			if (s.getName().equals(community.getName())) {
				return;
			}
		}
		
		Assert.fail("community was not found in the list of communities");
	}
	
	@StarexecTest
	private void commAssocExpiredNullTest() {
		Long temp = R.COMM_ASSOC_LAST_UPDATE;
		R.COMM_ASSOC_LAST_UPDATE = null;
		Assert.assertTrue(Communities.commAssocExpired());
		R.COMM_ASSOC_LAST_UPDATE=temp;
	}
	@StarexecTest
	private void commAssocExpiredTest() {
		Long temp = R.COMM_ASSOC_LAST_UPDATE;
		R.COMM_ASSOC_LAST_UPDATE = System.currentTimeMillis() + R.COMM_ASSOC_UPDATE_PERIOD + 100;
		Assert.assertTrue(Communities.commAssocExpired());
		R.COMM_ASSOC_LAST_UPDATE=temp;
	}
	
	@StarexecTest
	private void commAssocNotExpiredTest() {
		Long temp = R.COMM_ASSOC_LAST_UPDATE;
		R.COMM_ASSOC_LAST_UPDATE = System.currentTimeMillis();
		Assert.assertFalse(Communities.commAssocExpired());
		R.COMM_ASSOC_LAST_UPDATE=temp;
	}
	
	@Override
	protected String getTestName() {
		return "CommunitiesTests";
	}

	@Override
	protected void setup() throws Exception {
		admin = Users.getAdmins().get(0);
		testUser = ResourceLoader.loadUserIntoDatabase();
		user1 = ResourceLoader.loadUserIntoDatabase();
		user2 = ResourceLoader.loadUserIntoDatabase();
		user3 = ResourceLoader.loadUserIntoDatabase();

		community = ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), 1);
		subspace1 = ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), community.getId());
		subspace2 = ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), community.getId());
		subspace3 = ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), subspace1.getId());
		Users.associate(user1.getId(), subspace1.getId());
		Users.associate(user2.getId(), subspace2.getId());
		Users.associate(user3.getId(), subspace3.getId());
		solver1 = ResourceLoader.loadSolverIntoDatabase(community.getId(), user1.getId());
		solver2 = ResourceLoader.loadSolverIntoDatabase(subspace1.getId(), testUser.getId());
		solver3 = ResourceLoader.loadSolverIntoDatabase(subspace3.getId(), user2.getId());
		benchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(community.getId(), testUser.getId());
		benchmarkIds.addAll(ResourceLoader.loadBenchmarksIntoDatabase(subspace3.getId(), user1.getId()));
		job1 = ResourceLoader.loadJobIntoDatabase(subspace2.getId(), user2.getId(), solver1.getId(), benchmarkIds);
		job2 = ResourceLoader.loadJobIntoDatabase(subspace3.getId(), user1.getId(), solver2.getId(), benchmarkIds);
	}

	@Override
	protected void teardown() throws Exception {
		Jobs.deleteAndRemove(job1.getId());
		Jobs.deleteAndRemove(job2.getId());

		for (Integer i :benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Solvers.deleteAndRemoveSolver(solver1.getId());
		Solvers.deleteAndRemoveSolver(solver2.getId());
		Solvers.deleteAndRemoveSolver(solver3.getId());

		Spaces.removeSubspace(subspace3.getId());
		Spaces.removeSubspace(subspace2.getId());
		Spaces.removeSubspace(subspace1.getId());
		Spaces.removeSubspace(community.getId());
		Users.deleteUser(testUser.getId(), admin.getId());
		Users.deleteUser(user1.getId(), admin.getId());
		Users.deleteUser(user2.getId(), admin.getId());
		Users.deleteUser(user3.getId(), admin.getId());

		
	}

}
