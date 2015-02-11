package org.starexec.test.database;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Pipelines;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.to.*;
import org.starexec.data.to.pipelines.*;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

/**
 * This class contains tests of the Pipelines.java database class
 * @author Eric
 *
 */
public class PipelineTests extends TestSequence {
	User u=null;
	Solver s=null;
	Space space=null;
	SolverPipeline pipe=null;
	@Override
	protected String getTestName() {
		return "PipelineTests";
	}

	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		space=ResourceLoader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());
		s=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), u.getId());
		pipe=ResourceLoader.loadPipelineIntoDatabase(u.getId(), s.getConfigurations());
		
	}

	@Override
	protected void teardown() throws Exception {
		Solvers.deleteAndRemoveSolver(s.getId());
		Pipelines.deletePipelineFromDatabase(pipe.getId());
		Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId());
		
	}

}
