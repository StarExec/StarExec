package org.starexec.test;

import java.util.List;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Space;
import org.junit.Assert;

public class SpacePropertiesTest extends TestSequence {
	
	private Space c;
	
	@Test
	private void getSpaceTest() {
		Space test=Spaces.get(c.getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(c.getId(), test.getId());
		test=Communities.getDetails(c.getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(c.getId(), test.getId());
		
	}
	
	@Test
	private void leafTest() {
		//the new community should be a leaf
		Assert.assertTrue(Spaces.isLeaf(c.getId()));
	}
	@Test
	private void communityTest() {
		//of course, it should actually be a community
		Assert.assertTrue(Communities.isCommunity(c.getId()));
	}
	
	@Test
	private void getDefaultCpuTimeoutTest() {
		int timeout=Communities.getDefaultCpuTimeout(c.getId());
		if (timeout<=0) {
			Assert.fail("Timeout was not greater than 0");
		}
	}
	@Test 
	private void updateDefaultCpuTimeoutTest() {
		int timeout=Communities.getDefaultCpuTimeout(c.getId());
		Assert.assertTrue(Communities.setDefaultSettings(c.getId(), 2, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultCpuTimeout(c.getId()));
		Assert.assertTrue(Communities.setDefaultSettings(c.getId(), 2, timeout));
	}
	@Test
	private void getDefaultWallclockTimeoutTest() {
		int timeout=Communities.getDefaultWallclockTimeout(c.getId());
		if (timeout<=0) {
			Assert.fail("Timeout was not greater than 0");
		}
	}
	@Test
	private void updateDefaultWallclockTimeoutTest() {
		int timeout=Communities.getDefaultWallclockTimeout(c.getId());
		Assert.assertTrue(Communities.setDefaultSettings(c.getId(), 3, timeout+1));
		Assert.assertEquals(timeout+1, Communities.getDefaultWallclockTimeout(c.getId()));
		Assert.assertTrue(Communities.setDefaultSettings(c.getId(), 3, timeout));
	}
	
	@Test
	private void getDefaultMemoryLimit() {
		long limit=Communities.getDefaultMaxMemory(c.getId());
		if (limit<=0) {
			Assert.fail("Memory limit was not greater than 0");
		}
		
	}
	@Test
	private void updateDefaultMemoryLimitTest() {
		long memory=Communities.getDefaultMaxMemory(c.getId());
		Assert.assertTrue(Communities.setDefaultMaxMemory(c.getId(), memory+1));
		Assert.assertEquals(memory+1, Communities.getDefaultMaxMemory(c.getId()));
		Assert.assertTrue(Communities.setDefaultMaxMemory(c.getId(), memory));
	}
	
	@Test
	private void inListOfCommunities() throws Exception {
		List<Space> comms=Communities.getAll();
		for (Space s : comms) {
			if (s.getName().equals(c.getName())) {
				return;
			}
		}
		
		Assert.fail("community was not found in the list of communities");
	}
	
	@Test
	private void nameUpdateTest() throws Exception {
		String currentName=c.getName();
		Assert.assertEquals(currentName,Spaces.getName(c.getId()));
		addMessage("Space name consistent before update");
		String newName=TestUtil.getRandomSpaceName();
		Assert.assertTrue(Spaces.updateName(c.getId(), newName));
		Assert.assertEquals(newName,Spaces.getName(c.getId()));
		addMessage("Space name consistent after update");
		c.setName(newName);
	}
	@Test
	private void descriptionUpdateTest() throws Exception {
		String currentDesc=c.getDescription();
		Assert.assertEquals(currentDesc,Spaces.get(c.getId()).getDescription());
		String newDesc=TestUtil.getRandomSpaceName(); //any somewhat long, random string will work
		Assert.assertTrue(Spaces.updateDescription(c.getId(), newDesc));
		Assert.assertEquals(newDesc,Spaces.get(c.getId()).getDescription());
		c.setDescription(newDesc);
	}
	
	
	
	@Override
	protected void setup() {
		Space newCommunity = new Space();
		newCommunity.setDescription("test desc");
		newCommunity.setName(TestUtil.getRandomSpaceName());
		int id=Spaces.add(newCommunity, 1, Users.getTestUser().getId());
		Assert.assertNotEquals(-1,id);
		c=newCommunity;
		c.setId(id);
	}
	
	@Override
	protected void teardown() {
		boolean success=Spaces.removeSubspaces(c.getId(), 1, Users.getAdmins().get(0).getId());
		Assert.assertTrue(success);
	}
	@Override
	protected String getTestName() {
		return "SpacePropertiesTest";
	}

}
