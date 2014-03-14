package org.starexec.test.database;

import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;

public class SpaceTests extends TestSequence {
	
	Space community=null;
	Space subspace=null;
	User leader=null;
	User admin=null;
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
	private void getAllSpacesTest() {
		List<Space> spaces=Spaces.GetAllSpaces();
		Assert.assertNotNull(spaces);
		//add two because the root space itself and the public space are not counted otherwise
		Assert.assertEquals(Spaces.getSubSpaces(1, admin.getId(), true).size()+2, spaces.size());
	}
	
	@Test
	private void leafTest() {
		Assert.assertFalse(Spaces.isLeaf(community.getId()));
		Assert.assertTrue(Spaces.isLeaf(subspace.getId()));

	}
	
	@Test
	private void GetCommunityOfSpaceTest() {
		Assert.assertEquals(community.getId(),Spaces.GetCommunityOfSpace(subspace.getId()));
		Assert.assertEquals(community.getId(),Spaces.GetCommunityOfSpace(community.getId()));
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
		int timeout=Communities.getDefaultCpuTimeout(community.getId());
		Assert.assertTrue(Communities.setDefaultSettings(community.getId(), 2, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultCpuTimeout(community.getId()));
		Assert.assertTrue(Communities.setDefaultSettings(community.getId(), 2, timeout));
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
		int timeout=Communities.getDefaultWallclockTimeout(community.getId());
		Assert.assertTrue(Communities.setDefaultSettings(community.getId(), 3, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultWallclockTimeout(community.getId()));
		Assert.assertTrue(Communities.setDefaultSettings(community.getId(), 3, timeout));
	}
	
	@Test
	private void getDefaultMemoryLimit() {
		long limit=Communities.getDefaultMaxMemory(community.getId());
		if (limit<=0) {
			Assert.fail("Memory limit was not greater than 0");
		}
		
	}
	@Test
	private void updateDefaultMemoryLimitTest() {
		long memory=Communities.getDefaultMaxMemory(community.getId());
		Assert.assertTrue(Communities.setDefaultMaxMemory(community.getId(), memory+1));
		Assert.assertEquals(memory+1, Communities.getDefaultMaxMemory(community.getId()));
		Assert.assertTrue(Communities.setDefaultMaxMemory(community.getId(), memory));
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
		admin=Users.getAdmins().get(0);
		community = ResourceLoader.loadSpaceIntoDatabase(leader.getId(), 1);	
		subspace=ResourceLoader.loadSpaceIntoDatabase(leader.getId(), community.getId());
	}
	
	@Override
	protected void teardown() {
		Users.deleteUser(leader.getId(),admin.getId());
		
		boolean success=Spaces.removeSubspaces(community.getId(), 1, Users.getAdmins().get(0).getId());
		Assert.assertTrue(success);
	}
	@Override
	protected String getTestName() {
		return "SpacePropertiesTest";
	}

}
