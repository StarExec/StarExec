package org.starexec.test.integration.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.exceptions.StarExecSecurityException;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Hash;


//TODO: Test pagination functions
public class UserTests extends TestSequence {
	private User testUser=null;
	private User user1=null;
	private User user2=null;
	private User user3=null;
	private User admin=null;
	private Processor postProc=null;
	private Space space=null;
	private Space subspace=null;
	private Space comm=null;
	private int wallclockTimeout=100;
	private int cpuTimeout=100;
	private int gbMemory=1;
	private final String BENCH_ARCHIVE = "benchmarks.zip";
	
	private boolean removeUserFromSpace(User user, Space space) {
		List<Integer> userId=new ArrayList<Integer>();
		userId.add(user.getId());
		
		return Spaces.removeUsers(userId, space.getId());
	}
	
	@StarexecTest
	private void getUserByEmailTest() {
		Assert.assertFalse(Users.getUserByEmail(TestUtil.getRandomEmail()));
		Assert.assertTrue(Users.getUserByEmail(user1.getEmail()));
		Assert.assertTrue(Users.getUserByEmail(admin.getEmail()));

	}
	
	@StarexecTest
	private void setPasswordTest() {
		String randomPass=TestUtil.getRandomPassword();
		Assert.assertTrue(Users.updatePassword(user1.getId(), randomPass));
		Assert.assertEquals(Hash.hashPassword(randomPass), Users.getPassword(user1.getId()));
	}
	
	@StarexecTest
	private void getCommunitiesTest() {
		List<Integer> commIds=Users.getCommunities(user1.getId());
		Assert.assertEquals(0,commIds.size());
		commIds=Users.getCommunities(admin.getId());
		Assert.assertTrue(commIds.size()>0);
		boolean found=false;
		for (Integer i : commIds) {
			found=found || comm.getId()==i;
		}
		Assert.assertTrue(found);
	}
	
	@StarexecTest
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
	
	@StarexecTest
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
	
	@StarexecTest
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
	
	@StarexecTest
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
	
	@StarexecTest 
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
	
	@StarexecTest
	private void DeleteUserTest() {
		User temp=ResourceLoader.loadUserIntoDatabase();
		Assert.assertNotNull(Users.get(temp.getId()));
		try {
			Assert.assertTrue(Users.deleteUser(temp.getId(),admin.getId()));
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}
		Assert.assertNull(Users.get(temp.getId()));
	}

	/**
	 * Tests that a user's solvers are deleted when the user is deleted.
	 * @author Albert Giegerich
	 */
	@StarexecTest
	private void DeleteUserDeletesUsersSolversTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		Solver tempSolver = ResourceLoader.loadSolverIntoDatabase(space.getId(), tempUser.getId()); 

		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertNull(Solvers.get(tempSolver.getId()));
	}

	/**
	 * Tests that a user's solver directory is deleted when the user is deleted.
	 * @author Albert Giegerich
	 */
	private void DeleteUserDeletesUsersSolverDirectoryTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		List<Integer> tempBenchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(BENCH_ARCHIVE, space.getId(), tempUser.getId()); 
		File tempUsersSolverDirectory = new File(R.SOLVER_PATH+"/"+tempUser.getId());
		Assert.assertTrue(tempUsersSolverDirectory.exists());

		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertFalse(tempUsersSolverDirectory.exists());
	}

	/**
	 * Tests that a user's benchmarks are deleted when the user is deleted.
	 * @author Albert Giegerich
	 */
	@StarexecTest
	private void DeleteUserDeletesUsersBenchmarkTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		List<Integer> tempBenchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(BENCH_ARCHIVE, space.getId(), tempUser.getId()); 

		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		for (Integer benchmarkId : tempBenchmarkIds) {
			Assert.assertNotNull(benchmarkId);
			Assert.assertNull(Benchmarks.get(benchmarkId));
		}
	}

	/**
	 * Tests that a user's benchmark directory is deleted when the user is deleted.
	 * @author Albert Giegerich
	 */
	@StarexecTest
	private void DeleteUserDeletesUsersBenchmarkDirectoryTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		List<Integer> tempBenchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(BENCH_ARCHIVE, space.getId(), tempUser.getId()); 
		File tempUsersBenchmarkDirectory = new File(R.BENCHMARK_PATH+"/"+tempUser.getId());
		Assert.assertTrue(tempUsersBenchmarkDirectory.exists());

		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertFalse(tempUsersBenchmarkDirectory.exists());
	}

	/**
	 * Tests that deleting a user also deletes that user's jobs.
	 * @author Albert Giegerich
	 */
	@StarexecTest
	private void DeleteUserDeletesUsersJobsTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		Solver tempSolver = ResourceLoader.loadSolverIntoDatabase(space.getId(), tempUser.getId()); 
		List<Integer> tempSolverIds = Arrays.asList(new Integer[]{tempSolver.getId()}); 
		List<Integer> tempBenchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(BENCH_ARCHIVE, space.getId(), tempUser.getId()); 

		Job tempJob = ResourceLoader.loadJobIntoDatabase(
				space.getId(), tempUser.getId(), -1, postProc.getId(), tempSolverIds, tempBenchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		Assert.assertNotNull(tempJob);	

		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertNull(Jobs.get(tempJob.getId()));
	}

	/**
	 * Asserts that job pairs are deleted from the database successfully.
	 * @author Albert Giegerich
	 */
	@StarexecTest
	private void DeleteUserDeletesJobPairsTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		Solver tempSolver = ResourceLoader.loadSolverIntoDatabase(space.getId(), tempUser.getId()); 
		List<Integer> tempSolverIds = Arrays.asList(new Integer[]{tempSolver.getId()}); 
		List<Integer> tempBenchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(BENCH_ARCHIVE, space.getId(), tempUser.getId()); 

		Job tempJob = ResourceLoader.loadJobIntoDatabase(
				space.getId(), tempUser.getId(), -1, postProc.getId(), tempSolverIds, tempBenchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		Assert.assertNotNull(tempJob);	

		Assert.assertNotNull(tempJob.getJobPairs());
		Assert.assertTrue(tempJob.getJobPairs().size() > 0);

		for (JobPair pair : tempJob.getJobPairs()) {
			Assert.assertNotNull(pair);
		}

		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		for (JobPair pair : tempJob.getJobPairs()) {
			JobPair currentPair = JobPairs.getPair(pair.getId());
			Assert.assertNull(currentPair);
		}
	}

	@StarexecTest
	private void DeleteUserDeletesJobDirectoriesTest() {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		Solver tempSolver = ResourceLoader.loadSolverIntoDatabase(space.getId(), tempUser.getId()); 
		List<Integer> tempSolverIds = Arrays.asList(new Integer[]{tempSolver.getId()}); 
		List<Integer> tempBenchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(BENCH_ARCHIVE, space.getId(), tempUser.getId()); 

		Job tempJob = ResourceLoader.loadJobIntoDatabase(
				space.getId(), tempUser.getId(), -1, postProc.getId(), tempSolverIds, tempBenchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		Assert.assertNotNull(tempJob);	

		File jobDirectory = new File(R.NEW_JOB_OUTPUT_DIR +"/"+ tempJob.getId());
		// Make the job directory since ResourceLoader isn't actually running the job.
		jobDirectory.mkdir();

		Assert.assertTrue(jobDirectory.exists());


		try {
			Users.deleteUser(tempUser.getId(), admin.getId());
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

		Assert.assertFalse(jobDirectory.exists());

	}
	
	@StarexecTest
	private void GetUserTest() {
		User temp=Users.get(user1.getId());
		
		Assert.assertNotNull(temp);
		
		Assert.assertEquals(user1.getId(),temp.getId());
		Assert.assertEquals(user1.getFirstName(),temp.getFirstName());
		
		// should get null for a non-user
		Assert.assertNull(Users.get(-1));
	}

	@StarexecTest
	private void GetUserByEmailTest() {
		User temp=Users.get(user1.getEmail());
		
		Assert.assertNotNull(temp);
		
		Assert.assertEquals(user1.getId(),temp.getId());
		Assert.assertEquals(user1.getFirstName(),temp.getFirstName());
		
		// should get null for a non-user
		Assert.assertNull(Users.get(-1));
		
	}
	
	@StarexecTest
	private void GetAdminsTest() {
		List<User> admins=Users.getAdmins();
		for (User u : admins) {
			Assert.assertTrue(Users.get(u.getId()).getRole().equals("admin"));
			Assert.assertTrue(Users.isAdmin(u.getId()));
			Assert.assertFalse(Users.isNormalUser(u.getId()));
		}
	}
	
	@StarexecTest
	private void GetCountTest() {
		int count=Users.getCount();
		Assert.assertNotEquals(0,count);
		User temp=ResourceLoader.loadUserIntoDatabase();
		
		//this might fail if another user is added to the system at exactly this time,
		//but that would be atypical, and failure is not highly costly
		Assert.assertEquals(count+1,Users.getCount());
		try {
			Assert.assertTrue(Users.deleteUser(temp.getId(),admin.getId()));
		} catch (StarExecSecurityException e) {
			Assert.fail(e.getMessage());
		}

	}
	
	
	@StarexecTest
	private void GetCountInSpaceTest() {
		int count=Spaces.getUsers(space.getId()).size();
		Assert.assertEquals(count, Users.getCountInSpace(space.getId()));
	}
	
	@StarexecTest
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
	
	@StarexecTest
	private void SetDiskQuotaTest() {
		long quota=Users.get(user1.getId()).getDiskQuota();
		Assert.assertTrue(Users.setDiskQuota(user1.getId(), quota+1));
		Assert.assertEquals(quota+1,Users.get(user1.getId()).getDiskQuota());
		
	}
	
	@StarexecTest
	private void SetAndGetDefaultPageSize() {
		int pageSize=Users.getDefaultPageSize(user1.getId());
		Assert.assertTrue(Users.setDefaultPageSize(user1.getId(), pageSize+1));
		Assert.assertEquals(pageSize+1,Users.getDefaultPageSize(user1.getId()));
	}
	
	@StarexecTest
	private void GetPasswordTest() {
		Assert.assertNotNull(Users.getPassword(user1.getId()));
		Assert.assertEquals(Hash.hashPassword(user1.getPassword()), Users.getPassword(user1.getId()));
		
	}
	
	@StarexecTest
	private void GetTestUserTest() {
		User testUser=Users.getTestUser();
		Assert.assertNotNull(testUser);
		Assert.assertTrue(Users.isMemberOfCommunity(testUser.getId(), Communities.getTestCommunity().getId()));
		
	}
	
	@StarexecTest
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
	
	@StarexecTest
	private void updateFirstNameTest() {
		String originalName=user1.getFirstName();
		String newName=TestUtil.getRandomUserName();
		Assert.assertEquals(originalName, Users.get(user1.getId()).getFirstName());
		Assert.assertTrue(Users.updateFirstName(user1.getId(), newName));
		Assert.assertEquals(newName, Users.get(user1.getId()).getFirstName());
		user1.setFirstName(newName);
	}
	
	@StarexecTest
	private void updateLastNameTest() {
		String originalName=user1.getLastName();
		String newName=TestUtil.getRandomUserName();
		Assert.assertEquals(originalName, Users.get(user1.getId()).getLastName());
		Assert.assertTrue(Users.updateLastName(user1.getId(), newName));
		Assert.assertEquals(newName, Users.get(user1.getId()).getLastName());
		user1.setLastName(newName);
	}
	
	@StarexecTest
	private void updateInstitutionTest() {
		String originalInst=user1.getInstitution();
		String newInst=TestUtil.getRandomUserName();
		Assert.assertEquals(originalInst, Users.get(user1.getId()).getInstitution());
		Assert.assertTrue(Users.updateInstitution(user1.getId(), newInst));
		Assert.assertEquals(newInst, Users.get(user1.getId()).getInstitution());
		user1.setInstitution(newInst);

	}
	@StarexecTest
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
		postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
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
		Spaces.removeSubspaces(space.getId());
		Spaces.removeSubspaces(comm.getId());
		
	}
	
	
}
