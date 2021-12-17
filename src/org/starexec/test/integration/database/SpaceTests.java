package org.starexec.test.integration.database;

import com.google.common.base.Joiner;
import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.CopyPrimitivesOption;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;
import org.starexec.util.dataStructures.TreeNode;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tests for org.starexec.data.database.Spaces.java
 * @author Eric
 */
public class SpaceTests extends TestSequence {

	private static final StarLogger log = StarLogger.getLogger(SpaceTests.class);
	Space community=null;
	Space subspace=null;  //subspace of community
	Space subspace2=null; //subspace of community
	Space subspace3=null; //subspace of subspace2
	List<Space> testSpaces;
	List<Space> testSubSpaces;
	User leader=null;
	User admin=null;
	User member1=null;
	User member2=null;
	List<Integer> ids=null;


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
		List<Integer> si= new ArrayList<>();
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
		String spacesGot = getJoinedSpaceIds(spaces);
		String spacesExpected = getTestSubSpacesIds();
		String communityId = ((Integer)community.getId()).toString();
		Assert.assertEquals("Got: "+spacesGot+" Expected: "+spacesExpected + " Community ID was: "+communityId, 3, spaces.size());

	}

	@StarexecTest
	private void getByJobTest() {
		final String methodName = "getByJobTest";
		ResourceLoader tempLoader = new ResourceLoader();
		try {
			Space tempSpace=tempLoader.loadSpaceIntoDatabase(leader.getId(), community.getId());
			Job tempJob = tempLoader.loadJobIntoDatabase(tempSpace.getId(), leader.getId(), solver.getId(), ids);

			Set<Integer> spacesAssociatedWithJob = Spaces.getByJob(tempJob.getId());

			String got = "[ ";
			for (Integer spaceId : spacesAssociatedWithJob) {
				got += spaceId + " ";
			}
			got += "]";

			Assert.assertEquals("Expected: ["+subspace.getId()+"], Got: "+got,spacesAssociatedWithJob.size(), 1);
			Assert.assertTrue(spacesAssociatedWithJob.contains(tempSpace.getId()));
		} catch (SQLException e) {
			Assert.fail("SQLException thrown: "+ Util.getStackTrace(e));
		} finally {
			tempLoader.deleteAllPrimitives();
		}

	}

	@StarexecTest
	private void removeSolverTest() {
		List<Integer> solverId= new ArrayList<>();
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
		List<Integer> benchId= new ArrayList<>();
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
		List<Space> spaces=Spaces.getAllSpaces();
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
	private void copySpaceSampleAllBenchmarksTest() {
		try {
			int newSpaceId = Spaces.copySpace(subspace.getId(), subspace2.getId(), admin.getId(), CopyPrimitivesOption.NO_JOBS_LINK_SOLVERS_SAMPLE_BENCHMARKS, 1.0);
			List<Benchmark> b = Benchmarks.getBySpace(newSpaceId);
			Assert.assertEquals("New space contained a different number of benchmarks", b.size(), benchmarks.size());
			List<Integer> originalBenchIds = benchmarks.stream().map(Benchmark::getId).collect(Collectors.toList());
			Assert.assertTrue("New space did not contain the same benchmarks.", b.stream().allMatch(bench -> originalBenchIds.contains(bench.getId())));
			Spaces.removeSubspace(newSpaceId);
		} catch (Exception e) {
			Assert.fail("Caught Exception: " + e.getMessage() + "\n" + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void copySpaceSampleNoBenchmarksTest() {
		try {
			int newSpaceId = Spaces.copySpace(subspace.getId(), subspace2.getId(), admin.getId(), CopyPrimitivesOption.NO_JOBS_LINK_SOLVERS_SAMPLE_BENCHMARKS, 0.0);
			List<Benchmark> b = Benchmarks.getBySpace(newSpaceId);
			Assert.assertEquals("New space shouldn't contain any benchmarks", b.size(), 0);
			Spaces.removeSubspace(newSpaceId);
		} catch (Exception e) {
			Assert.fail("Caught Exception: " + e.getMessage() + "\n" + Util.getStackTrace(e));
		}
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
		List<Identifiable> identifiableListCopy = new ArrayList<>(identifiableList);
		List<Identifiable> otherIdentifiableListCopy = new ArrayList<>(otherIdentifiableList);
		// Compares the id of two identifiables.
		Comparator<Identifiable> idComparator = (identifiable, otherIdentifiable) -> {
            // Wrap the ids in the Integer object so we can use compareTo method.
            Integer boxedId = identifiable.getId();
            Integer otherBoxedId = otherIdentifiable.getId();
            return boxedId.compareTo(otherBoxedId);
        };
		// Sort both list copies by id
		identifiableListCopy.sort(idComparator);
		otherIdentifiableListCopy.sort(idComparator);
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
		Space space1=loader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		String space1Path=community.getName()+R.JOB_PAIR_PATH_DELIMITER+space1.getName();
		Space space2=loader.loadSpaceIntoDatabase(leader.getId(), space1.getId());
		String space2Path=space1Path+R.JOB_PAIR_PATH_DELIMITER+space2.getName();

		List<Space> spaceList= new ArrayList<>();
		spaceList.add(space1);
		spaceList.add(space2);
		HashMap<Integer,String> SP =Spaces.spacePathCreate(leader.getId(), spaceList, community.getId());
		Assert.assertEquals(space1Path, SP.get(space1.getId()));
		Assert.assertEquals(space2Path, SP.get(space2.getId()));

		//same test as above, but making the list in the opposite order
		spaceList= new ArrayList<>();
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
	private void nameUpdateTest() {
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
	private void descriptionUpdateTest() {
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
		final String methodName="setup";
		leader=loader.loadUserIntoDatabase();
		member1=loader.loadUserIntoDatabase();
		member2=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		community = loader.loadSpaceIntoDatabase(leader.getId(), 1);
		subspace=loader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		log.debug(methodName, "subspace id="+subspace.getId());
		subspace2=loader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		log.debug(methodName, "subspace2 id="+subspace2.getId());
		subspace3=loader.loadSpaceIntoDatabase(leader.getId(), subspace2.getId());
		log.debug(methodName, "subspace3 id="+subspace3.getId());
		testSpaces = Arrays.asList(community, subspace, subspace2, subspace3);
		testSubSpaces = Arrays.asList(subspace, subspace2, subspace3);
		Users.associate(member1.getId(), community.getId());
		Users.associate(member2.getId(), community.getId());
		Users.associate(member1.getId(), subspace.getId());
		Users.associate(member2.getId(), subspace.getId());
		Users.associate(member1.getId(), subspace2.getId());
		Users.associate(member2.getId(), subspace2.getId());



		solver=loader.loadSolverIntoDatabase("CVC4.zip", subspace.getId(), leader.getId());

		ids=loader.loadBenchmarksIntoDatabase("benchmarks.zip", subspace.getId(), leader.getId());
		benchmarks= new ArrayList<>();
		for (Integer id : ids) {
			benchmarks.add(Benchmarks.get(id));
		}
		job = loader.loadJobIntoDatabase(subspace.getId(), leader.getId(), solver.getId(), ids);

	}

	private String getTestSubSpacesIds() {
		return getJoinedSpaceIds(testSubSpaces);
	}

	private String getTestSpacesIds() {
		return getJoinedSpaceIds(testSpaces);
	}

	private String getJoinedSpaceIds(List<Space> spaces) {
		Joiner joiner = Joiner.on(", ");
		// Join all the ids.
		return joiner.join(spaces.stream().map(Space::getId).collect(Collectors.toList()));
	}

	@Override
	protected void teardown() {
		loader.deleteAllPrimitives();
	}
	@Override
	protected String getTestName() {
		return "SpaceTests";
	}

}
