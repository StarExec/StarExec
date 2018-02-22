package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.security.SpaceSecurity;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.SpaceXMLUploadStatus;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

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
	private void CanAddUserToSpace() {
		Assert.assertTrue(SpaceSecurity.canAddUserToSpace(space2.getId(), admin.getId(),owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canAddUserToSpace(space2.getId(), owner.getId(),owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canAddUserToSpace(space2.getId(), nonOwner.getId(),owner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canAddUserToSpace(space2.getId(), noPerms.getId(), owner.getId()).isSuccess());
	}

	@StarexecTest
	private void CanRemoveSolverFromSpace() {
		Assert.assertTrue(SpaceSecurity.canUserRemoveSolver(space2.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserRemoveSolver(space2.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserRemoveSolver(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserRemoveSolver(space2.getId(), noPerms.getId()).isSuccess());
	}

	@StarexecTest
	private void CanRemoveBenchmarkFromSpace() {
		Assert.assertTrue(SpaceSecurity.canUserRemoveBenchmark(space2.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserRemoveBenchmark(space2.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserRemoveBenchmark(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserRemoveBenchmark(space2.getId(), noPerms.getId()).isSuccess());
	}

	@StarexecTest
	private void CanUpdateProperties() {
		Assert.assertTrue(SpaceSecurity.canUpdateProperties(space2.getId(), admin.getId(), "fake", false).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUpdateProperties(space2.getId(), owner.getId(), "fake", false).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUpdateProperties(space2.getId(), nonOwner.getId(), "fake", false).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUpdateProperties(space2.getId(), noPerms.getId(), "fake", false).isSuccess());
	}

	@StarexecTest
	private void CanUserViewCommunityTests() {
		Assert.assertTrue(SpaceSecurity.canUserViewCommunityRequests(admin.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserViewCommunityRequests(owner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserViewCommunityRequests(nonOwner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserViewCommunityRequests(noPerms.getId()).isSuccess());
	}

	@StarexecTest
	private void CanUserSeeSpace() {
		Assert.assertTrue(SpaceSecurity.canUserSeeSpace(space1.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserSeeSpace(space1.getId(), owner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserSeeSpace(space1.getId(), nonOwner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserSeeSpace(space1.getId(), noPerms.getId()).isSuccess());

		Assert.assertTrue(SpaceSecurity.canUserSeeSpace(publicSpace.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserSeeSpace(publicSpace.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserSeeSpace(publicSpace.getId(), nonOwner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserSeeSpace(publicSpace.getId(), noPerms.getId()).isSuccess());
	}

	@StarexecTest
	private void CanSetPublicOrPrivate() {
		Assert.assertTrue(SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), owner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canSetSpacePublicOrPrivate(space2.getId(), noPerms.getId()).isSuccess());
	}

	@StarexecTest
	private void CanRemoveJobFromSpace() {
		Assert.assertTrue(SpaceSecurity.canUserRemoveJob(space2.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserRemoveJob(space2.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserRemoveJob(space2.getId(), nonOwner.getId()).isSuccess());
		Assert.assertFalse(SpaceSecurity.canUserRemoveJob(space2.getId(), noPerms.getId()).isSuccess());
	}

	@StarexecTest
	private void CanLeaveSpace() {
		Assert.assertTrue(SpaceSecurity.canUserLeaveSpace(space2.getId(), owner.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserLeaveSpace(space2.getId(), noPerms.getId()).isSuccess());
		Assert.assertTrue(SpaceSecurity.canUserLeaveSpace(space2.getId(), nonOwner.getId()).isSuccess());

		//the admin is not in the space, so they cannot leave
		Assert.assertFalse(SpaceSecurity.canUserLeaveSpace(space2.getId(), admin.getId()).isSuccess());
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

		owner=loader.loadUserIntoDatabase();
		nonOwner=loader.loadUserIntoDatabase();
		noPerms=loader.loadUserIntoDatabase();

		Assert.assertNotNull(owner);
		Assert.assertNotNull(nonOwner);
		Assert.assertNotNull(noPerms);

		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space1=loader.loadSpaceIntoDatabase(owner.getId(),testCommunity.getId());
		space2=loader.loadSpaceIntoDatabase(owner.getId(),testCommunity.getId());
		Assert.assertTrue(Permissions.get(owner.getId(), space1.getId()).isLeader() );
		publicSpace=loader.loadSpaceIntoDatabase(owner.getId(), testCommunity.getId());
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
		loader.deleteAllPrimitives();
	}
}
