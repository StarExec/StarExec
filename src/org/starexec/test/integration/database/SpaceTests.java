package org.starexec.test.integration.database; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.exceptions.StarExecSecurityException;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobSpace;
import org.starexec.data.to.Identifiable;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecException;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.dataStructures.TreeNode;

/**
 * Tests for org.starexec.data.database.Spaces.java
 * @author Eric
 */
public class SpaceTests extends TestSequence {
	
	Space community=null;
	Space subspace=null;  //subspace of community
	Space subspace2=null; //subspace of community
	Space subspace3=null; //subspace of subspace2
	User leader=null;
	User admin=null;
	User member1=null;
	User member2=null;
	
	
	//all primitives owned by leader and placed into subspace
	Solver solver = null;
	List<Benchmark> benchmarks=null; 
	Job job = null;
	@StarexecTest
	private void getSpaceTest() {
		Space test=Spaces.get(community.getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(community.getId(), test.getId());
	}
	
	@StarexecTest
	private void removeSolversFromHierarchyTest() {
		Assert.assertTrue(Solvers.associate(solver.getId(), subspace2.getId()));
		Assert.assertTrue(Solvers.associate(solver.getId(), subspace3.getId()));
		List<Integer> si=new ArrayList<Integer>();
		si.add(solver.getId());
		Assert.assertTrue(Spaces.removeSolversFromHierarchy(si, subspace2.getId(), leader.getId()));
		boolean found = false;
		for (Solver s : Spaces.getDetails(subspace2.getId(), leader.getId()).getSolvers()) {
			if (s.getId()==solver.getId()) {
				found=true;
				break;
			}
		}
		Assert.assertFalse(found);
		found = false;
		for (Solver s : Spaces.getDetails(subspace3.getId(), leader.getId()).getSolvers()) {
			if (s.getId()==solver.getId()) {
				found=true;
				break;
			}
		}
	}
	
	
	
	@StarexecTest
	private void getSpaceHierarchyTest() {
		List<Space> spaces=Spaces.getSubSpaceHierarchy(community.getId(),leader.getId());
		Assert.assertEquals(3, spaces.size());
		
	}
	
	@StarexecTest
	private void removeSolverTest() {
		List<Integer> solverId=new ArrayList<Integer>();
		solverId.add(solver.getId());
		Assert.assertTrue(Spaces.removeSolvers(solverId, subspace.getId()));
		
		List<Solver> spaceSolvers=Spaces.getDetails(subspace.getId(), admin.getId()).getSolvers();
		boolean solverInSpace=false;
		for (Solver s : spaceSolvers) {
			if (s.getId()==solver.getId()) {
				solverInSpace=true;
				break;
			}
		}
		Assert.assertFalse(solverInSpace);
		Assert.assertTrue(Solvers.associate(solverId, subspace.getId()));
	}
	
	@StarexecTest
	private void removeBenchmarkTest() {
		List<Integer> benchId=new ArrayList<Integer>();
		benchId.add(benchmarks.get(0).getId());
		Assert.assertTrue(Spaces.removeBenches(benchId, subspace.getId()));
		
		List<Benchmark> spaceBenchmarks=Spaces.getDetails(subspace.getId(), admin.getId()).getBenchmarks();
		boolean benchmarkInSpace=false;
		for (Benchmark b : spaceBenchmarks) {
			if (b.getId()==benchmarks.get(0).getId()) {
				benchmarkInSpace=true;
				break;
			}
		}
		
		Assert.assertFalse(benchmarkInSpace);
		Assert.assertTrue(Benchmarks.associate(benchId, subspace.getId()));
		
	}
	
	@StarexecTest
	private void getAllSpacesTest() {
		List<Space> spaces=Spaces.GetAllSpaces();
		Assert.assertNotNull(spaces);
		//add two because the root space itself and the public space are not counted otherwise
		Assert.assertEquals(Spaces.getSubSpaceHierarchy(1, admin.getId()).size()+2, spaces.size());
	}
	
	@StarexecTest
	private void leafTest() {
		Assert.assertFalse(Spaces.isLeaf(community.getId()));
		Assert.assertTrue(Spaces.isLeaf(subspace.getId()));
	}

	@StarexecTest
	private void CopyHierarchyTest() {
		TreeNode<Space> spaceTree;
		try {
			Spaces.copyHierarchy(community.getId(), subspace3.getId(), admin.getId());
		} catch (StarExecException e) {
			Assert.fail("Something went wrong while copying space hierarchy.");
		}
		try {
			spaceTree = Spaces.buildDetailedSpaceTree(subspace3, admin.getId());
		} catch (StarExecException e) {
			Assert.fail("Something went wrong while building space tree.");
			return;
		}
		for (TreeNode<Space> communityNode : spaceTree) {
			assertSpacesAreCopies(communityNode.getData(), community);
			for (TreeNode<Space> communityChildNode: communityNode) {
				Space communityChildSpace = communityChildNode.getData();
				if (communityChildSpace.getName().equals(subspace.getName())) {
					assertSpacesAreCopies(communityChildSpace, subspace);
				} else if (communityChildSpace.getName().equals(subspace2.getName())) {
					assertSpacesAreCopies(communityChildSpace, subspace2);
					TreeNode<Space> subspace3Copy = communityChildNode.getChild(0);
					assertSpacesAreCopies(subspace3Copy.getData(), subspace3);
				} else {
					Assert.fail("The Space should share a name with subspace or subspace2");
				}
			}
		}
	}

	private static void assertSpacesAreCopies(Space space, Space otherSpace) {
		Assert.assertEquals(space.getName(), otherSpace.getName());

		// Assert that the benchmarks are copies 
		List<Benchmark> benchmarks = space.getBenchmarks();
		List<Benchmark> otherBenchmarks = otherSpace.getBenchmarks();
		assertUnorderedIdentifiableListsAreEqual(benchmarks, otherBenchmarks);

		List<Solver> solvers = space.getSolvers();
		List<Solver> otherSolvers = otherSpace.getSolvers();
		assertUnorderedIdentifiableListsAreEqual(solvers, otherSolvers);


		List<Job> jobs = space.getJobs();
		List<Job> otherJobs = otherSpace.getJobs();
		assertUnorderedIdentifiableListsAreEqual(jobs, otherJobs);

		List<User> users = space.getUsers();
		List<User> otherUsers = otherSpace.getUsers();
		assertUnorderedIdentifiableListsAreEqual(users, otherUsers);

		List<Space> subspaces = space.getSubspaces();
		List<Space> otherSubspaces = otherSpace.getSubspaces();
		assertUnorderedIdentifiableListsAreEqual(subspaces, otherSubspaces);
	}

	private static void assertUnorderedIdentifiableListsAreEqual(List<? extends Identifiable> identifiableList, 
																 List<? extends Identifiable> otherIdentifiableList) {
		List<Identifiable> identifiableListCopy = new ArrayList<Identifiable>(identifiableList);	
		List<Identifiable> otherIdentifiableListCopy = new ArrayList<Identifiable>(otherIdentifiableList);
		// Compares the id of two identifiables.
		Comparator<Identifiable> idComparator = new Comparator<Identifiable>() {
			@Override
			public int compare(Identifiable identifiable, Identifiable otherIdentifiable) {
				// Wrap the ids in the Integer object so we can use compareTo method.
				Integer boxedId = Integer.valueOf(identifiable.getId());
				Integer otherBoxedId = Integer.valueOf(otherIdentifiable.getId());
				return boxedId.compareTo(otherBoxedId);
			}
		};
		// Sort both list copies by id
		Collections.sort(identifiableListCopy, idComparator);
		Collections.sort(otherIdentifiableListCopy, idComparator);
		Assert.assertEquals(identifiableListCopy.size(), otherIdentifiableListCopy.size());
		if (identifiableListCopy.size() != otherIdentifiableListCopy.size()) {
			return; // Avoid a NoSuchElementException
		}

		Iterator<Identifiable> identifiableIt = identifiableListCopy.iterator();
		Iterator<Identifiable> otherIdentifiableIt = otherIdentifiableListCopy.iterator();
		while (identifiableIt.hasNext() || otherIdentifiableIt.hasNext()) {
			Assert.assertEquals(identifiableIt.next().getId(), otherIdentifiableIt.next().getId());
		}
	}

	@StarexecTest
	private void SpacePathCreateTest() {
		Space space1=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		String space1Path=community.getName()+R.JOB_PAIR_PATH_DELIMITER+space1.getName();
		Space space2=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), space1.getId());
		String space2Path=space1Path+R.JOB_PAIR_PATH_DELIMITER+space2.getName();
		
		List<Space> spaceList=new ArrayList<Space>();
		spaceList.add(space1);
		spaceList.add(space2);
		HashMap<Integer,String> SP =Spaces.spacePathCreate(leader.getId(), spaceList, community.getId());
		Assert.assertEquals(space1Path, SP.get(space1.getId()));
		Assert.assertEquals(space2Path, SP.get(space2.getId()));

		//same test as above, but making the list in the opposite order
		spaceList=new ArrayList<Space>();
		spaceList.add(space2);
		spaceList.add(space1);
		SP=Spaces.spacePathCreate(leader.getId(), spaceList, community.getId());
		Assert.assertEquals(space1Path, SP.get(space1.getId()));
		Assert.assertEquals(space2Path, SP.get(space2.getId()));
		
		Spaces.removeSubspace(space1.getId());
	}
	
	@StarexecTest
	private void GetCommunityOfSpaceTest() {
		Assert.assertEquals(community.getId(),Spaces.getCommunityOfSpace(subspace.getId()));
		Assert.assertEquals(community.getId(),Spaces.getCommunityOfSpace(community.getId()));
	}
	
	@StarexecTest
	private void GetNameTest() {
		Assert.assertEquals(community.getName(),Spaces.getName(community.getId()));
		Assert.assertEquals(subspace.getName(),Spaces.getName(subspace.getId()));
		Assert.assertNotEquals(community.getName(),Spaces.getName(subspace.getId()));
	}	

	@StarexecTest 
	private void updateDefaultCpuTimeoutTest() {
		int settingId=Communities.getDefaultSettings(community.getId()).getId();
		
		int timeout=Communities.getDefaultSettings(community.getId()).getCpuTimeout();
		Assert.assertTrue(Settings.updateSettingsProfile(settingId, 2, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultSettings(community.getId()).getCpuTimeout());
		Assert.assertTrue(Settings.updateSettingsProfile(settingId, 2, timeout));
	}

	@StarexecTest
	private void updateDefaultWallclockTimeoutTest() {
		int settingId=Communities.getDefaultSettings(community.getId()).getId();

		int timeout=Communities.getDefaultSettings(community.getId()).getWallclockTimeout();
		Assert.assertTrue(Settings.updateSettingsProfile(settingId, 3, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultSettings(community.getId()).getWallclockTimeout());
		Assert.assertTrue(Settings.updateSettingsProfile(settingId, 3, timeout));
	}
	
	@StarexecTest
	private void getDefaultMemoryLimit() {
		long limit=Communities.getDefaultSettings(community.getId()).getMaxMemory();
		if (limit<=0) {
			Assert.fail("Memory limit was not greater than 0");
		}
		
	}
	
	@StarexecTest
	private void nameUpdateTest() throws Exception {
		String currentName=community.getName();
		Assert.assertEquals(currentName,Spaces.getName(community.getId()));
		addMessage("Space name consistent before update");
		String newName=TestUtil.getRandomSpaceName();
		Assert.assertTrue(Spaces.updateName(community.getId(), newName));
		Assert.assertEquals(newName,Spaces.getName(community.getId()));
		addMessage("Space name consistent after update");
		community.setName(newName);
	}
	@StarexecTest
	private void descriptionUpdateTest() throws Exception {
		String currentDesc=community.getDescription();
		Assert.assertEquals(currentDesc,Spaces.get(community.getId()).getDescription());
		String newDesc=TestUtil.getRandomSpaceName(); //any somewhat long, random string will work
		Assert.assertTrue(Spaces.updateDescription(community.getId(), newDesc));
		Assert.assertEquals(newDesc,Spaces.get(community.getId()).getDescription());
		community.setDescription(newDesc);
		
	}
	
	@StarexecTest
	private void getChainToRootTest() {
		List<Integer> path = Spaces.getChainToRoot(subspace3.getId());
		Assert.assertEquals(4, path.size());
		Assert.assertEquals((Integer)1, path.get(0));
		Assert.assertEquals((Integer)community.getId(), path.get(1));
		Assert.assertEquals((Integer)subspace2.getId(), path.get(2));
		Assert.assertEquals((Integer)subspace3.getId(), path.get(3));
	}
	
	@StarexecTest
	private void getChainToRootWithRootTest() {
		List<Integer> path = Spaces.getChainToRoot(1);
		Assert.assertEquals(1, path.size());
		Assert.assertEquals((Integer)1, path.get(0));
	}
	
	@StarexecTest
	private void setJobSpaceMaxStagesTest() {
		JobSpace s = Spaces.getJobSpace(job.getPrimarySpace());
		int maxStages = s.getMaxStages();
		Assert.assertTrue(Spaces.setJobSpaceMaxStages(s.getId(), maxStages+1));
		Assert.assertEquals((Integer)(maxStages+1), Spaces.getJobSpace(s.getId()).getMaxStages());
		Assert.assertTrue(Spaces.setJobSpaceMaxStages(s.getId(), maxStages));

	}
	
	
	@Override
	protected void setup() {
		leader=ResourceLoader.loadUserIntoDatabase();
		member1=ResourceLoader.loadUserIntoDatabase();
		member2=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		community = ResourceLoader.loadSpaceIntoDatabase(leader.getId(), 1);	
		subspace=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		subspace2=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		subspace3=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), subspace2.getId());
		Users.associate(member1.getId(), community.getId());
		Users.associate(member2.getId(), community.getId());
		Users.associate(member1.getId(), subspace.getId());
		Users.associate(member2.getId(), subspace.getId());
		Users.associate(member1.getId(), subspace2.getId());
		Users.associate(member2.getId(), subspace2.getId());
		
		
		
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", subspace.getId(), leader.getId());

		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", subspace.getId(), leader.getId());
		benchmarks=new ArrayList<Benchmark>();
		for (Integer id : ids) {
			benchmarks.add(Benchmarks.get(id));
		}
		job = ResourceLoader.loadJobIntoDatabase(subspace.getId(), leader.getId(), solver.getId(), ids);
		
	}
	
	@Override
	protected void teardown() {
		Jobs.deleteAndRemove(job.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		for (Benchmark b : benchmarks)  {
			Benchmarks.deleteAndRemoveBenchmark(b.getId());
		}
		
		Spaces.removeSubspace(subspace.getId());
		Spaces.removeSubspace(subspace2.getId());
		Spaces.removeSubspace(subspace3.getId());
		boolean success=Spaces.removeSubspace(community.getId());
		
		Users.deleteUser(leader.getId());
		Users.deleteUser(member1.getId());
		Users.deleteUser(member2.getId());
		

		Assert.assertTrue(success);
	}
	@Override
	protected String getTestName() {
		return "SpaceTests";
	}

}
