package org.starexec.test.security;

import org.junit.Assert;
import org.starexec.data.database.Users;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Hash;
import org.starexec.util.Util;

public class GeneralSecurityTests extends TestSequence {
	User user1=null;
	String plaintextPassword=null;
	User admin=null;
	
	
	@Test
	private void CanRestartStarexecTest() {
		Assert.assertEquals(true, GeneralSecurity.canUserRestartStarexec(admin.getId()).isSuccess());
		Assert.assertNotEquals(true, GeneralSecurity.canUserRestartStarexec(user1.getId()).isSuccess());
	}
	
	@Test
	private void CanViewTestInfo() {
		Assert.assertEquals(true, GeneralSecurity.canUserSeeTestInformation(admin.getId()).isSuccess());
		Assert.assertNotEquals(true, GeneralSecurity.canUserSeeTestInformation(user1.getId()).isSuccess());
	}
	
	@Test
	private void CanUserChangeLoggingTest() {
		Assert.assertEquals(true, GeneralSecurity.canUserChangeLogging(admin.getId()).isSuccess());
		Assert.assertNotEquals(true, GeneralSecurity.canUserChangeLogging(user1.getId()).isSuccess());
	}
	
	@Test 
	private void canUserRunTests() {
		Assert.assertEquals(true, GeneralSecurity.canUserRunTests(admin.getId()).isSuccess());
		Assert.assertNotEquals(true, GeneralSecurity.canUserRunTests(user1.getId()).isSuccess());
	}
	
	@Test
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
		Users.deleteUser(user1.getId(),admin.getId());
		
	}

}
