package org.starexec.test.security;

import org.starexec.data.database.Users;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class BenchmarkSecurityTests extends TestSequence {
	User user1=null;
	User admin=null;
	
	@Test
	private void temp() {
	
	}
	
	
	
	@Override
	protected String getTestName() {
		return "BenchmarkSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId());
	}

}
