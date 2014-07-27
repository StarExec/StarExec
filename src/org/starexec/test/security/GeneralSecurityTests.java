package org.starexec.test.security;

import org.junit.Assert;
import org.starexec.data.database.Users;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class GeneralSecurityTests extends TestSequence {
	User user1=null;
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
	
	@Override
	protected String getTestName() {
		return "GeneralSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId(),admin.getId());
		
	}

}
