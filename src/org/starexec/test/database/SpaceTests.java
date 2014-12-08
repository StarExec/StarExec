package org.starexec.test.database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;

public class SpaceTests extends TestSequence {
	
	Space community=null;
	Space subspace=null;
	Space subspace2=null;
	User leader=null;
	User admin=null;
	User member1=null;
	User member2=null;
	
	
	
	Solver solver = null;
	List<Benchmark> benchmarks=null; //all primitives owned by leader and placed into subspace
	
	@Test
	private void getSpaceTest() {
		Space test=Spaces.get(community.getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(community.getId(), test.getId());
		test=Communities.getDetails(community.getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(community.getId(), test.getId());
		
		
	}
	
	
	
	@Test
	private void getSpaceHierarchyTest() {
		List<Space> spaces=Spaces.getSubSpaceHierarchy(community.getId(),leader.getId());
		Assert.assertEquals(2, spaces.size());
		
	}
	
	@Test
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
	
	@Test
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
	
	@Test
	private void getAllSpacesTest() {
		List<Space> spaces=Spaces.GetAllSpaces();
		Assert.assertNotNull(spaces);
		//add two because the root space itself and the public space are not counted otherwise
		Assert.assertEquals(Spaces.getSubSpaceHierarchy(1, admin.getId()).size()+2, spaces.size());
	}
	
	@Test
	private void leafTest() {
		Assert.assertFalse(Spaces.isLeaf(community.getId()));
		Assert.assertTrue(Spaces.isLeaf(subspace.getId()));
	}
	
	@Test
	private void SpacePathCreateTest() {
		Space space1=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), community.getId());
		String space1Path=community.getName()+File.separator+space1.getName();
		Space space2=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), space1.getId());
		String space2Path=space1Path+File.separator+space2.getName();
		
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
		
		Spaces.removeSubspaces(space1.getId(), leader.getId());
	}
	
	@Test
	private void GetCommunityOfSpaceTest() {
		Assert.assertEquals(community.getId(),Spaces.getCommunityOfSpace(subspace.getId()));
		Assert.assertEquals(community.getId(),Spaces.getCommunityOfSpace(community.getId()));
	}
	
	@Test
	private void GetNameTest() {
		Assert.assertEquals(community.getName(),Spaces.getName(community.getId()));
		Assert.assertEquals(subspace.getName(),Spaces.getName(subspace.getId()));
		Assert.assertNotEquals(community.getName(),Spaces.getName(subspace.getId()));
	}	
	
	
	@Test
	private void IsCommunityTest() {
		//of course, it should actually be a community
		Assert.assertTrue(Communities.isCommunity(community.getId()));
	}
	
	@Test
	private void getDefaultCpuTimeoutTest() {
		int timeout=Communities.getDefaultCpuTimeout(community.getId());
		if (timeout<=0) {
			Assert.fail("Timeout was not greater than 0");
		}
	}
	@Test 
	private void updateDefaultCpuTimeoutTest() {
		int settingId=Communities.getDefaultSettings(community.getId()).getId();
		
		int timeout=Communities.getDefaultCpuTimeout(community.getId());
		Assert.assertTrue(Settings.setDefaultSettings(settingId, 2, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultCpuTimeout(community.getId()));
		Assert.assertTrue(Settings.setDefaultSettings(settingId, 2, timeout));
	}
	@Test
	private void getDefaultWallclockTimeoutTest() {
		int timeout=Communities.getDefaultWallclockTimeout(community.getId());
		if (timeout<=0) {
			Assert.fail("Timeout was not greater than 0");
		}
	}
	@Test
	private void updateDefaultWallclockTimeoutTest() {
		int settingId=Communities.getDefaultSettings(community.getId()).getId();

		int timeout=Communities.getDefaultWallclockTimeout(community.getId());
		Assert.assertTrue(Settings.setDefaultSettings(settingId, 3, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultWallclockTimeout(community.getId()));
		Assert.assertTrue(Settings.setDefaultSettings(settingId, 3, timeout));
	}
	
	@Test
	private void getDefaultMemoryLimit() {
		long limit=Communities.getDefaultMaxMemory(community.getId());
		if (limit<=0) {
			Assert.fail("Memory limit was not greater than 0");
		}
		
	}
	
	
	@Test
	private void inListOfCommunities() throws Exception {
		List<Space> comms=Communities.getAll();
		for (Space s : comms) {
			if (s.getName().equals(community.getName())) {
				return;
			}
		}
		
		Assert.fail("community was not found in the list of communities");
	}
	
	@Test
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
	@Test
	private void descriptionUpdateTest() throws Exception {
		String currentDesc=community.getDescription();
		Assert.assertEquals(currentDesc,Spaces.get(community.getId()).getDescription());
		String newDesc=TestUtil.getRandomSpaceName(); //any somewhat long, random string will work
		Assert.assertTrue(Spaces.updateDescription(community.getId(), newDesc));
		Assert.assertEquals(newDesc,Spaces.get(community.getId()).getDescription());
		community.setDescription(newDesc);
		
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
		
	}
	
	@Override
	protected void teardown() {
		Solvers.deleteAndRemoveSolver(solver.getId());
		for (Benchmark b : benchmarks)  {
			Benchmarks.deleteAndRemoveBenchmark(b.getId());
		}
		
		Spaces.removeSubspaces(subspace.getId(), Users.getAdmins().get(0).getId());
		Spaces.removeSubspaces(subspace2.getId(), Users.getAdmins().get(0).getId());

		boolean success=Spaces.removeSubspaces(community.getId(), Users.getAdmins().get(0).getId());
		Users.deleteUser(leader.getId(),admin.getId());
		Users.deleteUser(member1.getId(),admin.getId());
		Users.deleteUser(member2.getId(),admin.getId());

		Assert.assertTrue(success);
	}
	@Override
	protected String getTestName() {
		return "SpacePropertiesTest";
	}

}
