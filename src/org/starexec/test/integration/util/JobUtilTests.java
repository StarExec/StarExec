package org.starexec.test.integration.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.JobUtil;
import org.testng.Assert;

public class JobUtilTests extends TestSequence {
	User admin = null;
	
	Solver solver = null;
	
	List<Integer> benchmarkIds = null;
	
	@StarexecTest
	private void testJobXMLUpload() throws Exception {
		int cId = solver.getConfigurations().get(0).getId();
		File xml = ResourceLoader.getTestXMLFile(cId, cId, benchmarkIds.get(0), benchmarkIds.get(1));
		JobUtil util = new JobUtil();
		List<Integer> jobIds = util.createJobsFromFile(xml, admin.getId(), Communities.getTestCommunity().getId());
		Assert.assertEquals(1, jobIds.size());
	}
	
	@Override
	protected String getTestName() {
		return "JobUtilTests";
	}

	@Override
	protected void setup() throws Exception {
		admin = Users.getAdmins().get(0);
		solver = ResourceLoader.loadSolverIntoDatabase(Communities.getTestCommunity().getId(), admin.getId());
		benchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(Communities.getTestCommunity().getId(), admin.getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		Solvers.deleteAndRemoveSolver(solver.getId());
		for (int i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		
	}

}
