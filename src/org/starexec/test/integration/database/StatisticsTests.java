package org.starexec.test.integration.database;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.starexec.constants.R;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Statistics;
import org.starexec.data.database.Users;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.*;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.testng.Assert;

/**
 * Tests for Statistics.java
 * @author Eric
 *
 */
public class StatisticsTests extends TestSequence {
	
	List<JobPair> spaceOverviewPairs = null;
	
	User owner = null;
	User admin = null;
	Space space = null;
	Solver solver = null;
	List<Integer> benchmarkIds = null;
	Job job = null;
	
	@StarexecTest
	private void makeSpaceOverviewTest() {
		String filepath = Statistics.makeSpaceOverviewChart(spaceOverviewPairs, false, false, 1, PrimitivesToAnonymize.NONE);
		File f = new File(new File(R.STAREXEC_ROOT).getParent(), filepath);
		Assert.assertTrue(f.exists());
	}
	@StarexecTest
	private void makeSolverComparisonTest() {
		String filepath = Statistics.makeSolverComparisonChart(spaceOverviewPairs, spaceOverviewPairs, job.getPrimarySpace(), 
				100, Color.BLACK, 1, PrimitivesToAnonymize.NONE).get(0);
		File f = new File(new File(R.STAREXEC_ROOT).getParent(), filepath);
		Assert.assertTrue(f.exists());
	}
	
	@StarexecTest
	private void getJobPairOverviewTest() {
		HashMap<String, String> stats = Statistics.getJobPairOverview(job.getId());
		Assert.assertEquals(String.valueOf(job.getJobPairs().size()), stats.get("totalPairs"));
		Assert.assertEquals(String.valueOf(job.getJobPairs().size()), stats.get("completePairs"));
		Assert.assertEquals("0", stats.get("pendingPairs"));
		Assert.assertEquals("0", stats.get("errorPairs"));

	}
	
	@Override
	protected String getTestName() {
		return "StatisticsTests";
	}

	@Override
	protected void setup() throws Exception {
		spaceOverviewPairs = TestUtil.getFakeJobPairs(2000);
		owner = loader.loadUserIntoDatabase();
		admin = Users.getAdmins().get(0);
		space = loader.loadSpaceIntoDatabase(owner.getId(), 1);
		solver = loader.loadSolverIntoDatabase(space.getId(), owner.getId());
		benchmarkIds = loader.loadBenchmarksIntoDatabase(space.getId(), owner.getId());
		job = loader.loadJobIntoDatabase(space.getId(), owner.getId(), solver.getId(), benchmarkIds);
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
