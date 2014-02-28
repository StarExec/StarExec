package org.starexec.test.security;

import java.util.List;

import org.junit.Assert;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Users;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class BenchmarkSecurityTests extends TestSequence {
	User user1=null;
	User admin=null;
	List<Integer> benchmarkIds=null;
	
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
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", Communities.getTestCommunity().getId(), user1.getId());
		Assert.assertNotNull(benchmarkIds);	
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId());
	}

}
