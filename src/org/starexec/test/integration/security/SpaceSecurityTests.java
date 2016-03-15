package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.security.SpaceSecurity;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.SpaceXMLUploadStatus;
import org.starexec.data.to.User;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class SpaceSecurityTests extends TestSequence {
	
	User owner=null; //this user will be the leader of both space1 and space2
	User admin=null;
	User nonOwner=null; //this user will own neither space, but will be a member of space2 with every perm except isLeader. 
	User noPerms=null; //this user will own neither space, but will be a member of space2 with no perms
	Space space1=null; //a private space
	Space space2=null; //a private space
	Space publicSpace=null; //a public space
	SpaceXMLUploadStatus spaceStatus = null;

	@StarexecTest
	private void CanAssociateWebsiteTest() {
		Assert.assertEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), owner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), owner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), admin.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), admin.getId(),"new","http://www.fake.com").isSuccess());
		
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), nonOwner.getId(),"new","http://www.fake.com").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), nonOwner.getId(),"new","http://www.fake.com").isSuccess());
		
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), owner.getId(),"<script>","http://www.fake.com").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), owner.getId(),"<script>","http://www.fake.com").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), admin.getId(),"<script>","http://www.fake.com").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), admin.getId(),"<script>","http://www.fake.com").isSuccess());
		
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), owner.getId(),"new","<script>").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), owner.getId(),"new","<script>").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space1.getId(), admin.getId(),"new","<script>").isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAssociateWebsite(space2.getId(), admin.getId(),"new","<script>").isSuccess());
	}
	
	@StarexecTest
	private void CanDeleteWebsiteTest() {
		Websites.add(space1.getId(), "https://www.fake.edu", "new", WebsiteType.SPACE);
		int websiteId=Websites.getAll(space1.getId(), WebsiteType.SPACE).get(0).getId();
		Assert.assertEquals(true,SpaceSecurity.canDeleteWebsite(space1.getId(), websiteId, owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canDeleteWebsite(space1.getId(), websiteId, admin.getId()).isSuccess());

		Assert.assertNotEquals(true,SpaceSecurity.canDeleteWebsite(space1.getId(), websiteId, nonOwner.getId()).isSuccess());
		
		Assert.assertNotEquals(true,SpaceSecurity.canDeleteWebsite(space1.getId(), -1, owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canDeleteWebsite(space1.getId(), -1, admin.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanAddUserToSpace() {
		Assert.assertEquals(true,SpaceSecurity.canAddUserToSpace(space2.getId(), admin.getId(),owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canAddUserToSpace(space2.getId(), owner.getId(),owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canAddUserToSpace(space2.getId(), nonOwner.getId(),owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canAddUserToSpace(space2.getId(), noPerms.getId(), owner.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanRemoveSolverFromSpace() {
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveSolver(space2.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveSolver(space2.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveSolver(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserRemoveSolver(space2.getId(), noPerms.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanRemoveBenchmarkFromSpace() {
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserRemoveBenchmark(space2.getId(), noPerms.getId()).isSuccess());
	}
	
	@StarexecTest 
	private void CanUpdateProperties() {
		Assert.assertEquals(true,SpaceSecurity.canUpdateProperties(space2.getId(), admin.getId(), "fake", false).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUpdateProperties(space2.getId(), owner.getId(), "fake", false).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUpdateProperties(space2.getId(), nonOwner.getId(), "fake", false).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUpdateProperties(space2.getId(), noPerms.getId(), "fake", false).isSuccess());

	}
	
	@StarexecTest
	private void CanUserViewCommunityTests() {
		Assert.assertEquals(true,SpaceSecurity.canUserViewCommunityRequests(admin.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserViewCommunityRequests(owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserViewCommunityRequests(nonOwner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserViewCommunityRequests(noPerms.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanUserSeeSpace() {
		Assert.assertEquals(true,SpaceSecurity.canUserSeeSpace(space1.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserSeeSpace(space1.getId(), owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserSeeSpace(space1.getId(), nonOwner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserSeeSpace(space1.getId(), noPerms.getId()).isSuccess());

		Assert.assertEquals(true,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), nonOwner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserSeeSpace(publicSpace.getId(), noPerms.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanSetPublicOrPrivate() {
		Assert.assertEquals(true,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), owner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), noPerms.getId()).isSuccess());

	}

	@StarexecTest
	private void CanRemoveJobFromSpace() {
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveJob(space2.getId(), admin.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveJob(space2.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserRemoveJob(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertNotEquals(true,SpaceSecurity.canUserRemoveJob(space2.getId(), noPerms.getId()).isSuccess());
	}
	
	
	@StarexecTest
	private void CanLeaveSpace() {
		Assert.assertEquals(true,SpaceSecurity.canUserLeaveSpace(space2.getId(), owner.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserLeaveSpace(space2.getId(), noPerms.getId()).isSuccess());
		Assert.assertEquals(true,SpaceSecurity.canUserLeaveSpace(space2.getId(), nonOwner.getId()).isSuccess());
		
		//the admin is not in the space, so they cannot leave
		Assert.assertNotEquals(true,SpaceSecurity.canUserLeaveSpace(space2.getId(), admin.getId()).isSuccess());

	}
	
	@StarexecTest
	private void canSeeSpaceXMLUploadStatusTest() {
		Assert.assertTrue(SpaceSecurity.canUserSeeSpaceXMLStatus(spaceStatus.getId(), owner.getId()));
		Assert.assertTrue(SpaceSecurity.canUserSeeSpaceXMLStatus(spaceStatus.getId(), admin.getId()));
		Assert.assertFalse(SpaceSecurity.canUserSeeSpaceXMLStatus(spaceStatus.getId(), noPerms.getId()));
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
		Assert.assertTrue(Permissions.get(owner.getId(), space1.getId()).isLeader() );
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
		spaceStatus = Uploads.getSpaceXMLStatus(Uploads.createSpaceXMLUploadStatus(owner.getId()));

	}

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspace(space1.getId());
		Spaces.removeSubspace(space2.getId());
		Spaces.removeSubspace(publicSpace.getId());
		Users.deleteUser(owner.getId(),admin.getId());
		Users.deleteUser(nonOwner.getId(),admin.getId());
		Users.deleteUser(noPerms.getId(),admin.getId());
	}

}
