package org.starexec.test;

import java.util.List;

import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Space;
import org.junit.Assert;

public class SpacePropertiesTest extends TestSequence {
	
	private Space c;
	public SpacePropertiesTest() {
		setName("SpacePropertiesTest");
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
		int id=Spaces.add(newCommunity, 1, R.ADMIN_USER_ID);
		Assert.assertNotEquals(-1,id);
		c=newCommunity;
		c.setId(id);
	}
	
	@Override
	protected void teardown() {
		boolean success=Spaces.removeSubspaces(c.getId(), 1, R.ADMIN_USER_ID);
		Assert.assertTrue(success);
	}

}
