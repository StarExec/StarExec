package org.starexec.test.integration.database;

import org.junit.Assert;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Pipelines;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

/**
 * This class contains tests of the Pipelines.java database class
 * @author Eric
 *
 */
//TODO: Write tests for getting pipelines by job
public class PipelineTests extends TestSequence {
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

		for (int i=0; i<pipe.getStages().size(); ++i) {
			PipelineStage actualStage = pipe.getStages().get(i);
			PipelineStage retrievedStage = p.getStages().get(i);
			Assert.assertEquals(actualStage.getId(),retrievedStage.getId());
			Assert.assertEquals(actualStage.getConfigId(),retrievedStage.getConfigId());

			for (int dep=0; dep<actualStage.getDependencies().size(); ++dep) {
				PipelineDependency actualDep = actualStage.getDependencies().get(i);
				PipelineDependency retrievedDep = retrievedStage.getDependencies().get(i);
				Assert.assertEquals(actualDep.getInputNumber(), retrievedDep.getInputNumber());
				Assert.assertEquals(actualDep.getType(), retrievedDep.getType());
			}
		}
	}

	@Override
	protected void setup() throws Exception {
		final User user = loader.loadUserIntoDatabase();
		space = loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		final Solver solver = loader.loadSolverIntoDatabase(space.getId(), user.getId());
		pipe = loader.loadPipelineIntoDatabase(user.getId(), solver.getConfigurations());
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
