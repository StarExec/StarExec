package org.starexec.test.integration.app;

import java.util.List;

import org.junit.Assert;
import org.starexec.app.RESTServices;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

import com.google.gson.Gson;

/**
 * This class contains tests for RESTServices that should get rejected for security reasons
 * @author Eric
 *
 */
public class RESTServicesSecurityTests extends TestSequence {
	private RESTServices services = new RESTServices();
	User user = null; // user that should have no permissions to do anything
	User admin = null;
	
	// all of these will be owned by the admin
	Space space = null;
	Solver solver = null;
	List<Integer> benchmarkIds = null;
	Job job = null;
	BenchmarkUploadStatus benchmarkStatus = null;
	SpaceXMLUploadStatus spaceStatus = null;
	int invalidBenchId = 0;
	private static Gson gson = new Gson();

	private void assertResultIsInvalid(String result) {
		Assert.assertFalse(gson.fromJson(result, ValidatorStatusCode.class).isSuccess());
	}
	
	@StarexecTest
	private void restartStarexecTest() throws Exception {
		String result = services.restartStarExec(TestUtil.getMockHttpRequest(user.getId()));
		assertResultIsInvalid(result);
	}
	
	@StarexecTest
	private void recompileJobSpacesTest() {
		assertResultIsInvalid(services.recompileJobSpaces(job.getId(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.recompileJobSpaces(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void getBenchmarkUploadDescription() {
		assertResultIsInvalid(services.getBenchmarkUploadDescription(benchmarkStatus.getId(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.getBenchmarkUploadDescription(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void getInvalidUploadedBenchmarkOutput() {
		Assert.assertTrue(services.getInvalidUploadedBenchmarkOutput(invalidBenchId, 
				TestUtil.getMockHttpRequest(user.getId())).contains("You may only view your own benchmark uploads"));
	}
	
	@StarexecTest
	private void getJobPairLogTest() {
		Assert.assertTrue(services.getJobPairLog(job.getJobPairs().get(0).getId(), 
				TestUtil.getMockHttpRequest(user.getId())).contains("does not have access"));
		Assert.assertTrue(services.getJobPairLog(-1, 
				TestUtil.getMockHttpRequest(admin.getId())).contains("does not have access"));
	}
	
	@StarexecTest
	private void getBenchmarkContentTest() {
		Assert.assertTrue(services.getBenchmarkContent(benchmarkIds.get(0), 100, 
				TestUtil.getMockHttpRequest(user.getId())).contains("not available"));
		Assert.assertTrue(services.getBenchmarkContent(-1, 100,
				TestUtil.getMockHttpRequest(admin.getId())).contains("not available"));
	}
	
	@StarexecTest
	private void reinstateUserTest() {
		String result = services.reinstateUser(admin.getId(),TestUtil.getMockHttpRequest(user.getId()));
		assertResultIsInvalid(result);
	}
	
	@StarexecTest
	private void cleanErrorStatesTest() {
		assertResultIsInvalid(services.clearErrorStates(TestUtil.getMockHttpRequest(user.getId())));
	}
	
	
	@Override
	protected String getTestName() {
		return "RESTServicesSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user = ResourceLoader.loadUserIntoDatabase();
		admin = Users.getAdmins().get(0);
		space = ResourceLoader.loadSpaceIntoDatabase(admin.getId(), 1);
		solver = ResourceLoader.loadSolverIntoDatabase(space.getId(), admin.getId());
		benchmarkIds = ResourceLoader.loadBenchmarksIntoDatabase(space.getId(), user.getId());
		job = ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), solver.getId(), benchmarkIds);
		benchmarkStatus = Uploads.getBenchmarkStatus(Uploads.createBenchmarkUploadStatus(space.getId(), admin.getId()));
		spaceStatus = Uploads.getSpaceXMLStatus(Uploads.createSpaceXMLUploadStatus(admin.getId()));
		Uploads.addFailedBenchmark(benchmarkStatus.getId(), "failed", "test message");
		invalidBenchId = Uploads.getFailedBenches(benchmarkStatus.getId()).get(0).getId();
	}

	@Override
	protected void teardown() throws Exception {
		Solvers.deleteAndRemoveSolver(solver.getId());
		Jobs.deleteAndRemove(job.getId());
		for (int i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Spaces.removeSubspace(space.getId());
		Users.deleteUser(user.getId(), admin.getId());
	}

}
