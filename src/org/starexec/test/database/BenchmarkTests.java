package org.starexec.test.database;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class BenchmarkTests extends TestSequence {
	private User user=null;
	private User admin=null;
	private Space space=null;
	@Override
	protected String getTestName() {
		return "BenchmarkTests";
	}

	@Override
	protected void setup() throws Exception {
		user=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspaces(space.getId(), Communities.getTestCommunity().getId(), user.getId());
		Users.deleteUser(user.getId(), admin.getId());
		
	}

}
