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
		Assert.assertEquals(0, GeneralSecurity.canUserRestartStarexec(admin.getId()));
		Assert.assertNotEquals(0, GeneralSecurity.canUserRestartStarexec(user1.getId()));
	}
	
	@Test
	private void CanViewTestInfo() {
		Assert.assertEquals(0, GeneralSecurity.canUserSeeTestInformation(admin.getId()));
		Assert.assertNotEquals(0, GeneralSecurity.canUserSeeTestInformation(user1.getId()));
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
