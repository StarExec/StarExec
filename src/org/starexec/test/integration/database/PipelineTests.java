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
//TODO: Write tests for getting pipelines by job
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
			PipelineStage actualStage = pipe.getStages().get(i);
			PipelineStage retrievedStage = p.getStages().get(i);
			Assert.assertEquals(actualStage.getId(),retrievedStage.getId());
			Assert.assertEquals(actualStage.getConfigId(),retrievedStage.getConfigId());
			
			for (int dep=0;dep<actualStage.getDependencies().size();dep++) {
				PipelineDependency actualDep = actualStage.getDependencies().get(i);
				PipelineDependency retrievedDep = retrievedStage.getDependencies().get(i);
				Assert.assertEquals(actualDep.getInputNumber(), retrievedDep.getInputNumber());
				Assert.assertEquals(actualDep.getType(), retrievedDep.getType());
			}

		}	
	}

	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		space=ResourceLoader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());
		s=ResourceLoader.loadSolverIntoDatabase(space.getId(), u.getId());
		pipe=ResourceLoader.loadPipelineIntoDatabase(u.getId(), s.getConfigurations());
		
	}

	@Override
	protected void teardown() throws Exception {
		Solvers.deleteAndRemoveSolver(s.getId());
		Spaces.removeSubspace(space.getId());
		Assert.assertTrue(Pipelines.deletePipelineFromDatabase(pipe.getId()));
		Users.deleteUser(u.getId());
		
	}

}
