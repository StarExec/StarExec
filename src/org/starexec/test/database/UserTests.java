package org.starexec.test.database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Requests;
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


//TODO: Test pagination functions
public class UserTests extends TestSequence {
	User testUser=null;
	
	User user1=null;
	User user2=null;
	User user3=null;
	User admin=null;
	Space space=null;
	Space subspace=null;
	Space comm=null;
	
	private boolean removeUserFromSpace(User user, Space space) {
		List<Integer> userId=new ArrayList<Integer>();
		userId.add(user.getId());
		
		return Spaces.removeUsers(userId, space.getId());
	}
	
	@Test
	private void registerUserTest() {
		User u=new User();
		u.setFirstName(TestUtil.getRandomUserName());
		u.setLastName(TestUtil.getRandomUserName());
		u.setEmail(TestUtil.getRandomEmail());
		u.setPassword(TestUtil.getRandomPassword());
		u.setInstitution(TestUtil.getRandomAlphaString(10));
		String randomCode=UUID.randomUUID().toString();
		String randomMessage=TestUtil.getRandomAlphaString(20);
		Assert.assertTrue(Users.register(u, comm.getId(), randomCode, randomMessage));
		Assert.assertNotNull(Users.get(u.getId()));
		Assert.assertTrue(Requests.declineCommunityRequest(u.getId(), comm.getId()));
		Assert.assertNull(Users.get(u.getId()));
	}
	
	@Test
	private void AssociateOneUserOneSpace() {
		Assert.assertFalse(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user3.getId(), space.getId()));
		
		Assert.assertTrue(Users.associate(user1.getId(), space.getId()));
		
		Assert.assertTrue(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user3.getId(), space.getId()));
		Assert.assertTrue(removeUserFromSpace(user1,space));
	}
	
	@Test
	private void AssociateManyUsersOneSpace() {
		Assert.assertFalse(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user3.getId(), space.getId()));
		
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(user1.getId());
		ids.add(user2.getId());
		ids.add(user3.getId());

		Assert.assertTrue(Users.associate(ids, space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user3.getId(), space.getId()));
		Assert.assertTrue(removeUserFromSpace(user1,space));
		Assert.assertTrue(removeUserFromSpace(user2,space));
		Assert.assertTrue(removeUserFromSpace(user3,space));
	}
	
	@Test
	private void AssociateManyUsersSpaceHierarchy() {
		Assert.assertFalse(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user1.getId(), subspace.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), subspace.getId()));

		List<Integer> ids=new ArrayList<Integer>();
		ids.add(user1.getId());
		ids.add(user2.getId());
		Assert.assertTrue(Users.associate(ids, space.getId(),true, testUser.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user1.getId(), subspace.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user2.getId(), subspace.getId()));

		Assert.assertTrue(removeUserFromSpace(user1,space));
		Assert.assertTrue(removeUserFromSpace(user1,subspace));
		Assert.assertTrue(removeUserFromSpace(user2,space));
		Assert.assertTrue(removeUserFromSpace(user2,subspace));
	}
	
	@Test 
	private void AssociateManyUsersManySpaces() {
		Assert.assertFalse(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user1.getId(), subspace.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertFalse(Users.isMemberOfSpace(user2.getId(), subspace.getId()));

		List<Integer> ids=new ArrayList<Integer>();
		List<Integer> spaceIds=new ArrayList<Integer>();
		ids.add(user1.getId());
		ids.add(user2.getId());
		
		spaceIds.add(space.getId());
		spaceIds.add(subspace.getId());
		
		Assert.assertTrue(Users.associate(ids, spaceIds));
		Assert.assertTrue(Users.isMemberOfSpace(user1.getId(), space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user1.getId(), subspace.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user2.getId(), space.getId()));
		Assert.assertTrue(Users.isMemberOfSpace(user2.getId(), subspace.getId()));

		Assert.assertTrue(removeUserFromSpace(user1,space));
		Assert.assertTrue(removeUserFromSpace(user1,subspace));
		Assert.assertTrue(removeUserFromSpace(user2,space));
		Assert.assertTrue(removeUserFromSpace(user2,subspace));
		
	}
	
	@Test
	private void deleteUserTest() {
		User temp=ResourceLoader.loadUserIntoDatabase();
		Assert.assertNotNull(Users.get(temp.getId()));
		Assert.assertTrue(Users.deleteUser(temp.getId(),admin.getId()));
		Assert.assertNull(Users.get(temp.getId()));
		
	}
	
	@Test
	private void GetUserTest() {
		User temp=Users.get(user1.getId());
		
		Assert.assertNotNull(temp);
		
		Assert.assertEquals(user1.getId(),temp.getId());
		Assert.assertEquals(user1.getFirstName(),temp.getFirstName());
		
		// should get null for a non-user
		Assert.assertNull(Users.get(-1));
	}
	@Test
	private void GetUserByEmailTest() {
		User temp=Users.get(user1.getEmail());
		
		Assert.assertNotNull(temp);
		
		Assert.assertEquals(user1.getId(),temp.getId());
		Assert.assertEquals(user1.getFirstName(),temp.getFirstName());
		
		// should get null for a non-user
		Assert.assertNull(Users.get(-1));
		
	}
	
	@Test
	private void GetAdminsTest() {
		List<User> admins=Users.getAdmins();
		for (User u : admins) {
			Assert.assertTrue(Users.get(u.getId()).getRole().equals("admin"));
			Assert.assertTrue(Users.isAdmin(u.getId()));
			Assert.assertFalse(Users.isNormalUser(u.getId()));
		}
	}
	
	@Test
	private void GetCountTest() {
		int count=Users.getCount();
		Assert.assertNotEquals(0,count);
		User temp=ResourceLoader.loadUserIntoDatabase();
		
		//this might fail if another user is added to the system at exactly this time,
		//but that would be atypical, and failure is not highly costly
		Assert.assertEquals(count+1,Users.getCount());
		Assert.assertTrue(Users.deleteUser(temp.getId(),admin.getId()));
	}
	
	
	@Test
	private void GetCountInSpaceTest() {
		int count=Spaces.getUsers(space.getId()).size();
		Assert.assertEquals(count, Users.getCountInSpace(space.getId()));
	}
	
	@Test
	private void GetDiskUsageTest() {
		Assert.assertEquals(0,Users.getDiskUsage(user1.getId()));
		Solver solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user1.getId());
		
		long size=Solvers.get(solver.getId()).getDiskSize();
		List<Integer> benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user1.getId());
		for (Integer i : benchmarkIds) {
			size+=Benchmarks.get(i).getDiskSize();
		}
		Assert.assertEquals(size, Users.getDiskUsage(user1.getId()));
		Assert.assertTrue(Solvers.deleteAndRemoveSolver(solver.getId()));
		for (Integer i : benchmarkIds) {
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(i));
		}
	}
	
	@Test
	private void SetDiskQuotaTest() {
		long quota=Users.get(user1.getId()).getDiskQuota();
		Assert.assertTrue(Users.setDiskQuota(user1.getId(), quota+1));
		Assert.assertEquals(quota+1,Users.get(user1.getId()).getDiskQuota());
		
	}
	
	@Test
	private void SetAndGetDefaultPageSize() {
		int pageSize=Users.getDefaultPageSize(user1.getId());
		Assert.assertTrue(Users.setDefaultPageSize(user1.getId(), pageSize+1));
		Assert.assertEquals(pageSize+1,Users.getDefaultPageSize(user1.getId()));
	}
	
	//TODO: Make this stronger?
	@Test
	private void GetPasswordTest() {
		Assert.assertNotNull(Users.getPassword(user1.getId()));
		
	}
	
	@Test
	private void GetTestUserTest() {
		User testUser=Users.getTestUser();
		Assert.assertNotNull(testUser);
		Assert.assertTrue(Users.isMemberOfCommunity(testUser.getId(), Communities.getTestCommunity().getId()));
		
	}
	
	@Test
	private void IsAdminTest() {
		Assert.assertFalse(Users.isAdmin(user1.getId()));
		Assert.assertFalse(Users.isAdmin(user2.getId()));
		Assert.assertFalse(Users.isAdmin(user3.getId()));
		Assert.assertFalse(Users.isAdmin(testUser.getId()));
		List<Integer> admins=new ArrayList<Integer>();
		for (Integer i : admins) {
			Assert.assertTrue(Users.isAdmin(i));
		}
	}
	
	@Test
	private void updateFirstNameTest() {
		String originalName=user1.getFirstName();
		String newName=TestUtil.getRandomUserName();
		Assert.assertEquals(originalName, Users.get(user1.getId()).getFirstName());
		Assert.assertTrue(Users.updateFirstName(user1.getId(), newName));
		Assert.assertEquals(newName, Users.get(user1.getId()).getFirstName());
		user1.setFirstName(newName);
	}
	
	@Test
	private void updateLastNameTest() {
		String originalName=user1.getLastName();
		String newName=TestUtil.getRandomUserName();
		Assert.assertEquals(originalName, Users.get(user1.getId()).getLastName());
		Assert.assertTrue(Users.updateLastName(user1.getId(), newName));
		Assert.assertEquals(newName, Users.get(user1.getId()).getLastName());
		user1.setLastName(newName);
	}
	
	@Test
	private void updateInstitutionTest() {
		String originalInst=user1.getInstitution();
		String newInst=TestUtil.getRandomUserName();
		Assert.assertEquals(originalInst, Users.get(user1.getId()).getInstitution());
		Assert.assertTrue(Users.updateInstitution(user1.getId(), newInst));
		Assert.assertEquals(newInst, Users.get(user1.getId()).getInstitution());
		user1.setInstitution(newInst);

	}
	@Test
	private void SuspendAndReinstateTest() {
		Assert.assertFalse(Users.isSuspended(user1.getId()));
		Assert.assertTrue(Users.suspend(user1.getId()));
		Assert.assertTrue(Users.isSuspended(user1.getId()));
		Assert.assertTrue(Users.reinstate(user1.getId()));
		Assert.assertFalse(Users.isSuspended(user1.getId()));
		
		Assert.assertTrue(Users.changeUserRole(user1.getId(), R.TEST_ROLE_NAME));
	}
	
	
	
	@Override
	protected String getTestName() {
		return "UserTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		user3=ResourceLoader.loadUserIntoDatabase();
		testUser=Users.getTestUser();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), Communities.getTestCommunity().getId());
		subspace=ResourceLoader.loadSpaceIntoDatabase(testUser.getId(), space.getId());
		comm=ResourceLoader.loadSpaceIntoDatabase(admin.getId(), 1);
		
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId(),admin.getId());
		Users.deleteUser(user2.getId(),admin.getId());
		Users.deleteUser(user3.getId(),admin.getId());
		Spaces.removeSubspaces(space.getId(), admin.getId());
		Spaces.removeSubspaces(comm.getId(), admin.getId());
		
	}
	
	
}
