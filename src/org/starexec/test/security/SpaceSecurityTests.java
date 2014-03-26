package org.starexec.test.security;

import org.junit.Assert;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.database.Websites.WebsiteType;
import org.starexec.data.security.SpaceSecurity;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class SpaceSecurityTests extends TestSequence {
	
	User owner=null; //this user will be the leader of both space1 and space2
	User admin=null;
	User nonOwner=null; //this user will own neither space, but will be a member of space2 with every perm except isLeader. 
	User noPerms=null; //this user will own neither space, but will be a member of space2 with no perms
	Space space1=null; //a private space
	Space space2=null; //a private space
	Space publicSpace=null; //a public space
	
	@Test
	private void CanAssociateWebsiteTest() {
		Assert.assertEquals(0,SpaceSecurity.canAssociateWebsite(space1.getId(), owner.getId(),"new"));
		Assert.assertEquals(0,SpaceSecurity.canAssociateWebsite(space2.getId(), owner.getId(),"new"));
		Assert.assertEquals(0,SpaceSecurity.canAssociateWebsite(space1.getId(), admin.getId(),"new"));
		Assert.assertEquals(0,SpaceSecurity.canAssociateWebsite(space2.getId(), admin.getId(),"new"));
		
		Assert.assertNotEquals(0,SpaceSecurity.canAssociateWebsite(space1.getId(), nonOwner.getId(),"new"));
		Assert.assertNotEquals(0,SpaceSecurity.canAssociateWebsite(space2.getId(), nonOwner.getId(),"new"));
		
		Assert.assertNotEquals(0,SpaceSecurity.canAssociateWebsite(space1.getId(), owner.getId(),"<script>"));
		Assert.assertNotEquals(0,SpaceSecurity.canAssociateWebsite(space2.getId(), owner.getId(),"<script>"));
		Assert.assertNotEquals(0,SpaceSecurity.canAssociateWebsite(space1.getId(), admin.getId(),"<script>"));
		Assert.assertNotEquals(0,SpaceSecurity.canAssociateWebsite(space2.getId(), admin.getId(),"<script>"));
	}
	
	@Test
	private void CanDeleteWebsiteTest() {
		Websites.add(space1.getId(), "https://www.fake.edu", "new", WebsiteType.SPACE);
		int websiteId=Websites.getAll(space1.getId(), WebsiteType.SPACE).get(0).getId();
		Assert.assertEquals(0,SpaceSecurity.canDeleteWebsite(space1.getId(), websiteId, owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canDeleteWebsite(space1.getId(), websiteId, admin.getId()));

		Assert.assertNotEquals(0,SpaceSecurity.canDeleteWebsite(space1.getId(), websiteId, nonOwner.getId()));
		
		Assert.assertNotEquals(0,SpaceSecurity.canDeleteWebsite(space1.getId(), -1, owner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canDeleteWebsite(space1.getId(), -1, admin.getId()));
	}
	
	@Test
	private void CanAddUserToSpace() {
		Assert.assertEquals(0,SpaceSecurity.canAddUserToSpace(space2.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canAddUserToSpace(space2.getId(), owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canAddUserToSpace(space2.getId(), nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canAddUserToSpace(space2.getId(), noPerms.getId()));
	}
	
	@Test
	private void CanRemoveSolverFromSpace() {
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveSolver(space2.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveSolver(space2.getId(), owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveSolver(space2.getId(), nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserRemoveSolver(space2.getId(), noPerms.getId()));
	}
	
	@Test
	private void CanRemoveBenchmarkFromSpace() {
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), noPerms.getId()));
	}
	
	@Test 
	private void CanUpdateProperties() {
		Assert.assertEquals(0,SpaceSecurity.canUpdateProperties(space2.getId(), admin.getId(), "fake", false));
		Assert.assertEquals(0,SpaceSecurity.canUpdateProperties(space2.getId(), owner.getId(), "fake", false));
		Assert.assertNotEquals(0,SpaceSecurity.canUpdateProperties(space2.getId(), nonOwner.getId(), "fake", false));
		Assert.assertNotEquals(0,SpaceSecurity.canUpdateProperties(space2.getId(), noPerms.getId(), "fake", false));

	}
	
	@Test
	private void CanUserViewCommunityTests() {
		Assert.assertEquals(0,SpaceSecurity.canUserViewCommunityRequests(admin.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserViewCommunityRequests(owner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserViewCommunityRequests(nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserViewCommunityRequests(noPerms.getId()));
	}
	
	@Test
	private void CanUserSeeSpace() {
		Assert.assertEquals(0,SpaceSecurity.canUserSeeSpace(space1.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserSeeSpace(space1.getId(), owner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserSeeSpace(space1.getId(), nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserSeeSpace(space1.getId(), noPerms.getId()));

		Assert.assertEquals(0,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), nonOwner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), noPerms.getId()));
	}
	
	@Test
	private void CanSetPublicOrPrivate() {
		Assert.assertEquals(0,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), owner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), noPerms.getId()));

	}

	@Test
	private void CanRemoveJobFromSpace() {
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveJob(space2.getId(), admin.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveJob(space2.getId(), owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserRemoveJob(space2.getId(), nonOwner.getId()));
		Assert.assertNotEquals(0,SpaceSecurity.canUserRemoveJob(space2.getId(), noPerms.getId()));
	}
	
	
	@Test
	private void CanLeaveSpace() {
		Assert.assertEquals(0,SpaceSecurity.canUserLeaveSpace(space2.getId(), owner.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserLeaveSpace(space2.getId(), noPerms.getId()));
		Assert.assertEquals(0,SpaceSecurity.canUserLeaveSpace(space2.getId(), nonOwner.getId()));
		
		//the admin is not in the space, so they cannot leave
		Assert.assertNotEquals(0,SpaceSecurity.canUserLeaveSpace(space2.getId(), admin.getId()));

	}
	
	@Override
	protected String getTestName() {
		return "SpaceSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		Space testCommunity=Communities.getTestCommunity();
		
		owner=ResourceLoader.loadUserIntoDatabase();
		nonOwner=ResourceLoader.loadUserIntoDatabase();
		noPerms=ResourceLoader.loadUserIntoDatabase();
		
		Assert.assertNotNull(owner);
		Assert.assertNotNull(nonOwner);
		Assert.assertNotNull(noPerms);

		
		admin=Users.getAdmins().get(0);
		space1=ResourceLoader.loadSpaceIntoDatabase(owner.getId(),testCommunity.getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(owner.getId(),testCommunity.getId());
		publicSpace=ResourceLoader.loadSpaceIntoDatabase(owner.getId(), testCommunity.getId());
		Users.associate(nonOwner.getId(), space2.getId());
		Users.associate(noPerms.getId(),space2.getId());
		
		Permission p=Permissions.getFullPermission();
		p.setLeader(false);
		Assert.assertTrue(Permissions.set(nonOwner.getId(), space2.getId(), p));
		
		p=Permissions.getEmptyPermission();
		Assert.assertTrue(Permissions.set(noPerms.getId(),space2.getId(),p));
		
		
		Permission temp=Permissions.get(nonOwner.getId(), space2.getId());
		Assert.assertNotNull(temp);
		Assert.assertTrue(temp.canRemoveSolver());
		
		Spaces.setPublicSpace(space1.getId(), owner.getId(), false, false);
		Spaces.setPublicSpace(space2.getId(), owner.getId(), false, false);
		Spaces.setPublicSpace(publicSpace.getId(), owner.getId(), true, false);
	}

	@Override
	protected void teardown() throws Exception {
		Space testCommunity=Communities.getTestCommunity();

		Spaces.removeSubspaces(space1.getId(), testCommunity.getId(), owner.getId());
		Spaces.removeSubspaces(space2.getId(), testCommunity.getId(), owner.getId());
		Spaces.removeSubspaces(publicSpace.getId(),testCommunity.getId(),owner.getId());
		Users.deleteUser(owner.getId(),admin.getId());
		Users.deleteUser(nonOwner.getId(),admin.getId());
		Users.deleteUser(noPerms.getId(),admin.getId());
	}

}
