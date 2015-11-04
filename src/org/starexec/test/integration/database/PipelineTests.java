package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Pipelines;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.*;
import org.starexec.data.to.pipelines.*;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
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
	
	@StarexecTest
	private void getFullPipelineTest() {
		SolverPipeline p=Pipelines.getFullPipeline(pipe.getId());
		Assert.assertNotNull(p);
		Assert.assertEquals(pipe.getName(), p.getName());
		Assert.assertEquals(pipe.getId(),p.getId());
		Assert.assertEquals(pipe.getStages().size(),p.getStages().size());
		for (int i=0;i<pipe.getStages().size();i++) {
			Assert.assertEquals(pipe.getStages().get(i).getId(),p.getStages().get(i).getId());
			Assert.assertEquals(pipe.getStages().get(i).getConfigId(),p.getStages().get(i).getConfigId());

		}
		
	}
	
	/*@StarexecTest
	private void addStageToDatabase() {
		PipelineStage newStage=new PipelineStage();
		newStage.setConfigId(s.getConfigurations().get(0).getId());
		newStage.setPipelineId(pipe.getId());
		Assert.assertTrue(Pipelines.addPipelineStageToDatabase(stage, con));
	}*/

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
		Spaces.removeSubspaces(space.getId());
		Pipelines.deletePipelineFromDatabase(pipe.getId());
		Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId());
		
	}

}
