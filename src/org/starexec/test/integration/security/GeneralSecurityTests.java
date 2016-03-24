package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.data.database.Users;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

public class GeneralSecurityTests extends TestSequence {
	User user1=null;
	String plaintextPassword=null;
	User admin=null;
	
	@StarexecTest 
	private void canUserRunTests() {
		Assert.assertEquals(true, GeneralSecurity.canUserRunTestsNoRunningCheck(admin.getId()).isSuccess());
		Assert.assertNotEquals(true, GeneralSecurity.canUserRunTestsNoRunningCheck(user1.getId()).isSuccess());
	}
	
	@StarexecTest
	private void canUserUpdatePassword() {
		String pass1=Util.getTempPassword();
		String pass2=Util.getTempPassword();
		Assert.assertEquals(true,GeneralSecurity.canUserUpdatePassword(user1.getId(), user1.getId(), plaintextPassword, pass1, pass1).isSuccess());
		Assert.assertEquals(true,GeneralSecurity.canUserUpdatePassword(user1.getId(), admin.getId(), plaintextPassword, pass1, pass1).isSuccess());
		
		Assert.assertNotEquals(true,GeneralSecurity.canUserUpdatePassword(user1.getId(), user1.getId(), pass1, pass1, pass1).isSuccess());
		Assert.assertNotEquals(true,GeneralSecurity.canUserUpdatePassword(user1.getId(), user1.getId(), plaintextPassword, pass1, pass2).isSuccess());
		Assert.assertNotEquals(true,GeneralSecurity.canUserUpdatePassword(admin.getId(), user1.getId(), plaintextPassword, pass1, pass1).isSuccess());


	}
	
	@Override
	protected String getTestName() {
		return "GeneralSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		plaintextPassword=Util.getTempPassword();
		user1=ResourceLoader.loadUserIntoDatabase(plaintextPassword);
		admin=Users.getAdmins().get(0);
		
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId());
		
	}

}
