package org.starexec.test.integration.security;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.security.UploadSecurity;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.testng.Assert;

/**
 * Tests for data/security/UploadSecurity.java
 * @author Eric
 *
 */
public class UploadSecurityTests extends TestSequence {

	Space space = null;
	User user = null;
	User nonOwner = null;
	User admin = null;
	BenchmarkUploadStatus status = null;
	
	@StarexecTest
	private void canViewUnvalidatedBenchmarkOutputTest() {
		int unvalidatedId = Uploads.getFailedBenches(status.getId()).get(0).getId();
		Assert.assertTrue(UploadSecurity.canViewUnvalidatedBenchmarkOutput(user.getId(), unvalidatedId).isSuccess());
		Assert.assertTrue(UploadSecurity.canViewUnvalidatedBenchmarkOutput(admin.getId(), unvalidatedId).isSuccess());
		Assert.assertFalse(UploadSecurity.canViewUnvalidatedBenchmarkOutput(nonOwner.getId(), unvalidatedId).isSuccess());

	}
	
	@Override
	protected String getTestName() {
		return "UploadSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		admin = Users.getAdmins().get(0);
		user = ResourceLoader.loadUserIntoDatabase();
		nonOwner = ResourceLoader.loadUserIntoDatabase();
		space = ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		status = Uploads.getBenchmarkStatus(Uploads.createBenchmarkUploadStatus(space.getId(), user.getId()));
		Uploads.addFailedBenchmark(status.getId(), "failed", "failed bench");
	}

	@Override
	protected void teardown() throws Exception {	
		Spaces.removeSubspace(space.getId());
		Users.deleteUser(user.getId(), admin.getId());
		Users.deleteUser(nonOwner.getId(), admin.getId());
	}

}
