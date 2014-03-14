package org.starexec.test.security;

import org.junit.Assert;
import org.starexec.data.database.Users;
import org.starexec.data.security.UserSecurity;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class UserSecurityTests extends TestSequence {

	User user1=null;
	User user2=null;
	User admin=null;
	
	@Test
	private void canUpdateFirstNameTest() {
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), user1.getId(), "firstname","test"));
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), admin.getId(), "firstname","test"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(admin.getId(), user1.getId(), "firstname","test"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(user1.getId(), user2.getId(), "firstname","test"));
	}
	
	@Test
	private void CanDeleteUserTest() {
		Assert.assertEquals(0,UserSecurity.canDeleteUser(user1.getId(), admin.getId()));
		Assert.assertEquals(0,UserSecurity.canDeleteUser(user2.getId(), admin.getId()));

		Assert.assertNotEquals(0,UserSecurity.canDeleteUser(user1.getId(), user1.getId()));
		Assert.assertNotEquals(0,UserSecurity.canDeleteUser(admin.getId(), user1.getId()));
		Assert.assertNotEquals(0,UserSecurity.canDeleteUser(admin.getId(), admin.getId()));
		Assert.assertNotEquals(0,UserSecurity.canDeleteUser(user1.getId(), user2.getId()));
	}

	@Test
	private void canUpdateLastNameTest() {
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), user1.getId(), "lastname","test"));
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), admin.getId(), "lastname","test"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(admin.getId(), user1.getId(), "lastname","test"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(user1.getId(), user2.getId(), "lastname","test"));
	}
	
	@Test
	private void canUpdateInstitutionTest() {
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), user1.getId(), "institution","Iowa"));
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), admin.getId(), "institution","Iowa"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(admin.getId(), user1.getId(), "institution","Iowa"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(user1.getId(), user2.getId(), "institution","Iowa"));
	}

	
	@Test
	private void canUpdateDiskQuotaTest() {
		//only admins can update anyone's disk quota
		Assert.assertEquals(0, UserSecurity.canUpdateData(user1.getId(), admin.getId(), "diskquota","10"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(admin.getId(), user1.getId(), "diskquota","10"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(user1.getId(), user2.getId(), "diskquota","10"));
		Assert.assertNotEquals(0, UserSecurity.canUpdateData(user1.getId(), user1.getId(), "diskquota","10"));
	}
	
	@Test
	private void canSuspendOrReinstateUser() {
		Assert.assertEquals(0, UserSecurity.canUserSuspendOrReinstateUser(admin.getId()));
		Assert.assertNotEquals(0, UserSecurity.canUserSuspendOrReinstateUser(user1.getId()));
		Assert.assertNotEquals(0, UserSecurity.canUserSuspendOrReinstateUser(user2.getId()));
	}
	
	@Test
	private void canViewUserPrimitives() {
		Assert.assertEquals(0, UserSecurity.canViewUserPrimitives(user1.getId(),user1.getId()));
		Assert.assertEquals(0, UserSecurity.canViewUserPrimitives(user1.getId(),admin.getId()));
		Assert.assertNotEquals(0, UserSecurity.canViewUserPrimitives(admin.getId(),user2.getId()));
		Assert.assertNotEquals(0, UserSecurity.canViewUserPrimitives(user1.getId(),user2.getId()));

	}

	@Override
	protected String getTestName() {
		return "UserSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();

		admin=Users.getAdmins().get(0);
		Assert.assertNotNull(user1);
		Assert.assertNotNull(user2);
		Assert.assertNotNull(admin);
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId(),admin.getId());
		Users.deleteUser(user2.getId(),admin.getId());
	}

}
