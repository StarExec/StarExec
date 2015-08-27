package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.data.database.Users;
import org.starexec.data.security.CacheSecurity;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class CacheSecurityTests extends TestSequence {
	User user1=null;
	User admin=null;
	
	@StarexecTest
	private void CanUserClearCacheTest() {
		Assert.assertEquals(true,CacheSecurity.canUserClearCache(admin.getId()).isSuccess());
		Assert.assertNotEquals(true,CacheSecurity.canUserClearCache(user1.getId()).isSuccess());
	}
	
	@Override
	protected String getTestName() {
		return "CacheSecurityTests";
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
