package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.data.security.UserSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

public class UserSecurityTests extends TestSequence {
	User user1=null;
	User user2=null;
	User dev = null;
	User admin=null;

	@StarexecTest
	private void canUpdateFirstNameTest() {
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), user1.getId(), "firstname","test").isSuccess());
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), admin.getId(), "firstname","test").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(admin.getId(), user1.getId(), "firstname","test").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(user1.getId(), user2.getId(), "firstname","test").isSuccess());
	}

	@StarexecTest
	private void canUserSubscribeOrUnsubscribeUserToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(user2.getId(), user1.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserSubscribeOrUnsubscribeSelfToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(user1.getId(), user1.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserSubscribeOrUnsubscribeDevToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(dev.getId(), user1.getId()).isSuccess());
	}

	@StarexecTest
	private void canUserSubscribeOrUnsubscribeAdminToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(admin.getId(), user1.getId()).isSuccess());
	}

	@StarexecTest
	private void canDevSubscribeOrUnsubscribeUserToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(user1.getId(), dev.getId()).isSuccess());
	}

	@StarexecTest
	private void canDevSubscribeOrUnsubscribeAdminToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(admin.getId(), dev.getId()).isSuccess());
	}

	@StarexecTest
	private void canDevSubscribeOrUnsubscribeSelfToErrorLogsTest() {
		ValidatorStatusCode status = UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(dev.getId(), dev.getId());
		Assert.assertTrue(status.getMessage(), status.isSuccess());
	}

	@StarexecTest
	private void canAdminSubscribeOrUnsubscribeUserToErrorLogsTest() {
		Assert.assertFalse(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(user1.getId(), admin.getId()).isSuccess());
	}

	@StarexecTest
	private void canAdminSubscribeOrUnsubscribeSelfToErrorLogsTest() {
		Assert.assertTrue(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(admin.getId(), admin.getId()).isSuccess());
	}

	@StarexecTest
	private void canAdminSubscribeOrUnsubscribeDevToErrorLogsTest() {
		Assert.assertTrue(UserSecurity.canUserSubscribeOrUnsubscribeUserToErrorLogs(dev.getId(), admin.getId()).isSuccess());
	}

	@StarexecTest
	private void CanDeleteUserTest() {
		Assert.assertTrue(UserSecurity.canDeleteUser(user1.getId(), admin.getId()).isSuccess());
		Assert.assertTrue(UserSecurity.canDeleteUser(user2.getId(), admin.getId()).isSuccess());

		Assert.assertFalse(UserSecurity.canDeleteUser(user1.getId(), user1.getId()).isSuccess());
		Assert.assertFalse(UserSecurity.canDeleteUser(admin.getId(), user1.getId()).isSuccess());
		Assert.assertFalse(UserSecurity.canDeleteUser(admin.getId(), admin.getId()).isSuccess());
		Assert.assertFalse(UserSecurity.canDeleteUser(user1.getId(), user2.getId()).isSuccess());
	}

	@StarexecTest
	private void canUpdateLastNameTest() {
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), user1.getId(), "lastname","test").isSuccess());
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), admin.getId(), "lastname","test").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(admin.getId(), user1.getId(), "lastname","test").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(user1.getId(), user2.getId(), "lastname","test").isSuccess());
	}

	@StarexecTest
	private void canUpdateInstitutionTest() {
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), user1.getId(), "institution","Iowa").isSuccess());
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), admin.getId(), "institution","Iowa").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(admin.getId(), user1.getId(), "institution","Iowa").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(user1.getId(), user2.getId(), "institution","Iowa").isSuccess());
	}

	@StarexecTest
	private void canUpdateDiskQuotaTest() {
		//only admins can update anyone's disk quota
		Assert.assertTrue(UserSecurity.canUpdateData(user1.getId(), admin.getId(), "diskquota","10").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(admin.getId(), user1.getId(), "diskquota","10").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(user1.getId(), user2.getId(), "diskquota","10").isSuccess());
		Assert.assertFalse(UserSecurity.canUpdateData(user1.getId(), user1.getId(), "diskquota","10").isSuccess());
	}

	@StarexecTest
	private void canViewUserPrimitives() {
		Assert.assertTrue(UserSecurity.canViewUserPrimitives(user1.getId(),user1.getId()).isSuccess());
		Assert.assertTrue(UserSecurity.canViewUserPrimitives(user1.getId(),admin.getId()).isSuccess());
		Assert.assertFalse(UserSecurity.canViewUserPrimitives(admin.getId(),user2.getId()).isSuccess());
		Assert.assertFalse(UserSecurity.canViewUserPrimitives(user1.getId(),user2.getId()).isSuccess());
	}

	@Override
	protected String getTestName() {
		return "UserSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();

		admin=loader.loadAdminIntoDatabase();
		dev = loader.loadDevIntoDatabase();
		Assert.assertNotNull(user1);
		Assert.assertNotNull(user2);
		Assert.assertNotNull(admin);
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}
}
