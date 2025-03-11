package org.starexec.test.integration.database;

import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Users;
import org.starexec.data.to.*;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.testng.Assert;

import java.util.List;
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
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("users"), Long.valueOf(4));
	}
	@StarexecTest
	private void communityMapSolversTest() {
		Communities.updateCommunityMap();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("solvers"), Long.valueOf(3));
	}
	
	@StarexecTest
	private void communityMapBenchmarksTest() {
		Communities.updateCommunityMap();
		
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("benchmarks"), Long.valueOf(benchmarkIds.size()));
	}
	
	@StarexecTest
	private void communityMapDiskUsageTest() {
		Communities.updateCommunityMap();

		Long disk = 0L;
		for (Integer i : benchmarkIds) {
			disk+= Benchmarks.get(i).getDiskSize();
		}
		disk+=solver1.getDiskSize();
		disk+=solver2.getDiskSize();
		disk+=solver3.getDiskSize();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("disk_usage"), disk);

	}
	
	@StarexecTest
	private void communityMapJobsTest() {
		Communities.updateCommunityMap();
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("jobs"), Long.valueOf(2));
		Assert.assertEquals(R.COMM_INFO_MAP.get(community.getId()).get("job_pairs"), Long.valueOf(job1.getJobPairs().size() + job2.getJobPairs().size()));
	}
	
	@StarexecTest
	private void getDetailsTest() {
		Space s = Communities.getDetails(community.getId());
		Assert.assertEquals(s.getId(), community.getId());
		Assert.assertEquals(s.getName(), community.getName());
		Assert.assertEquals(s.getDescription(), community.getDescription());
		Assert.assertEquals(s.isLocked(), community.isLocked());
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
	private void inListOfCommunities() {
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
		R.COMM_ASSOC_LAST_UPDATE = System.currentTimeMillis() - R.COMM_ASSOC_UPDATE_PERIOD - 10000;
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
		admin = loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		testUser = loader.loadUserIntoDatabase();
		user1 = loader.loadUserIntoDatabase();
		user2 = loader.loadUserIntoDatabase();
		user3 = loader.loadUserIntoDatabase();

		community = loader.loadSpaceIntoDatabase(testUser.getId(), 1);
		subspace1 = loader.loadSpaceIntoDatabase(testUser.getId(), community.getId());
		subspace2 = loader.loadSpaceIntoDatabase(testUser.getId(), community.getId());
		subspace3 = loader.loadSpaceIntoDatabase(testUser.getId(), subspace1.getId());
		Users.associate(user1.getId(), subspace1.getId());
		Users.associate(user2.getId(), subspace2.getId());
		Users.associate(user3.getId(), subspace3.getId());
		solver1 = loader.loadSolverIntoDatabase(community.getId(), user1.getId());
		solver2 = loader.loadSolverIntoDatabase(subspace1.getId(), testUser.getId());
		solver3 = loader.loadSolverIntoDatabase(subspace3.getId(), user2.getId());
		benchmarkIds = loader.loadBenchmarksIntoDatabase(community.getId(), testUser.getId());
		benchmarkIds.addAll(loader.loadBenchmarksIntoDatabase(subspace3.getId(), user1.getId()));
		job1 = loader.loadJobIntoDatabase(subspace2.getId(), user2.getId(), solver1.getId(), benchmarkIds);
		job2 = loader.loadJobIntoDatabase(subspace3.getId(), user1.getId(), solver2.getId(), benchmarkIds);
		for (JobPair p : job1.getJobPairs()) {
			JobPairs.UpdateStatus(p.getId(), StatusCode.STATUS_COMPLETE.getVal());
		}
		for (JobPair p : job2.getJobPairs()) {
			JobPairs.UpdateStatus(p.getId(), StatusCode.STATUS_COMPLETE.getVal());
		}
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
