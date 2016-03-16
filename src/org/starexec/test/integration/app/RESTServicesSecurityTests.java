package org.starexec.test.integration.app;

import java.util.List;

import org.junit.Assert;
import org.starexec.app.RESTServices;
import org.starexec.data.database.AnonymousLinks;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.Status.StatusCode;
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
	String anonymousJobId = null;
	private Gson gson = new Gson();

	private void assertResultIsInvalid(String result) {
		Assert.assertFalse(gson.fromJson(result, ValidatorStatusCode.class).isSuccess());
	}
	
	@StarexecTest
	private void restartStarexecTest() throws Exception {
		String result = services.restartStarExec(TestUtil.getMockHttpRequest(user.getId()));
		assertResultIsInvalid(result);
	}
	
	@StarexecTest
	private void getFinishedPairsForMatrixTest() {
		assertResultIsInvalid(services.getFinishedJobPairsForMatrix(job.getPrimarySpace(), 
				1, TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.getFinishedJobPairsForMatrix(-1, 1, TestUtil.getMockHttpRequest(user.getId())));
	}
	
	@StarexecTest
	private void getSolverComparisonsPaginatedTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();

		assertResultIsInvalid(services.getSolverComparisonsPaginated(false, job.getPrimarySpace(), cId, cId,
				TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.getSolverComparisonsPaginated(false, -1, cId, cId, TestUtil.getMockHttpRequest(user.getId())));
	}
	
	@StarexecTest
	private void getJobPairsPaginatedWithAnonymousLinkTest() {
		assertResultIsInvalid(services.getJobPairsPaginatedWithAnonymousLink(anonymousJobId,1, false,
				-1,false,"none", TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getJobPairsPaginatedWithAnonymousLink("",1, false,
				job.getPrimarySpace(),false,"none", TestUtil.getMockHttpRequest(admin.getId())));
	}
	

	@StarexecTest
	private void getAnonymousSpaceOverviewGraphTest() {
		assertResultIsInvalid(services.getSpaceOverviewGraph(1,anonymousJobId,
				-1,"none", TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getSpaceOverviewGraph(1,"",
				job.getPrimarySpace(),"none", TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void getAnonymousSolverComparisonGraphTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();
		assertResultIsInvalid(services.getAnonymousSolverComparisonGraph(anonymousJobId,1,
				-1,cId,cId,100,"black","none", TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getAnonymousSolverComparisonGraph("",1,
				job.getPrimarySpace(),cId,cId,100,"none","black",TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void getSpaceOverviewGraphTest() {
		assertResultIsInvalid(services.getSpaceOverviewGraph(1,-1, TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getSpaceOverviewGraph(1,job.getPrimarySpace(),TestUtil.getMockHttpRequest(user.getId())));
	}
	
	@StarexecTest
	private void getAnonymousJobStatsTest() {
		assertResultIsInvalid(services.getAnonymousJobStatsPaginated(1,-1,
				anonymousJobId,"none",false,false, TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getAnonymousJobStatsPaginated(1,job.getPrimarySpace(),
				"","none",false,false, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void getJobStatsTest() {
		assertResultIsInvalid(services.getJobStatsPaginated(1,-1,false,false, TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getJobStatsPaginated(1,job.getPrimarySpace(),false,false, TestUtil.getMockHttpRequest(user.getId())));
	}
	
	@StarexecTest
	private void getSolverComparisonGraphTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();
		assertResultIsInvalid(services.getSolverComparisonGraph(1,
				-1,cId,cId,100,"black", TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getSolverComparisonGraph(1,
				job.getPrimarySpace(),cId,cId,100,"black",TestUtil.getMockHttpRequest(user.getId())));
	}
	
	
	@StarexecTest
	private void getJobPairsPaginatedTest() {
		assertResultIsInvalid(services.getJobPairsPaginated(1, false,
				job.getPrimarySpace(),false,TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.getJobPairsPaginated(1, false,
				-1,false, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	
	@StarexecTest
	private void getJobPairsInSpaceHierarchyByConfigPaginatedTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();
		assertResultIsInvalid(services.
				getJobPairsInSpaceHierarchyByConfigPaginated(1, false, job.getPrimarySpace(),"all",cId, TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.
				getJobPairsInSpaceHierarchyByConfigPaginated(1, false, -1,"all",cId, TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.
				getJobPairsInSpaceHierarchyByConfigPaginated(1, false, job.getPrimarySpace(),"badtypestring",cId, TestUtil.getMockHttpRequest(user.getId())));
	}
	
	@StarexecTest
	private void recompileJobSpacesTest() {
		assertResultIsInvalid(services.recompileJobSpaces(job.getId(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.recompileJobSpaces(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void rerunAllPairsTest() {
		assertResultIsInvalid(services.rerunAllJobPairs(job.getId(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.rerunAllJobPairs(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void rerunTimelessPairsTest() {
		assertResultIsInvalid(services.rerunTimelessJobPairs(job.getId(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.rerunTimelessJobPairs(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void rerunSinglePairTest() {
		assertResultIsInvalid(services.rerunJobPair(job.getJobPairs().get(0).getId(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.rerunJobPair(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}
	
	@StarexecTest
	private void rerunAllPairsOfStatusTest() {
		assertResultIsInvalid(services.rerunJobPairs(job.getId(), StatusCode.STATUS_KILLED.getVal(), TestUtil.getMockHttpRequest(user.getId())));
		assertResultIsInvalid(services.rerunJobPairs(-1, StatusCode.STATUS_KILLED.getVal(), TestUtil.getMockHttpRequest(admin.getId())));
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
	private void getJobPairStdoutTest() {
		Assert.assertTrue(services.getJobPairStdout(job.getJobPairs().get(0).getId(), 1, 100,
				TestUtil.getMockHttpRequest(user.getId())).contains("not available"));
		Assert.assertTrue(services.getJobPairStdout(-1, 1, 100,
				TestUtil.getMockHttpRequest(admin.getId())).contains("not available"));
	}
	
	@StarexecTest
	private void getBenchmarkContentTest() {
		Assert.assertTrue(services.getBenchmarkContent(benchmarkIds.get(0), 100, 
				TestUtil.getMockHttpRequest(user.getId())).contains("not available"));
		Assert.assertTrue(services.getBenchmarkContent(-1, 100,
				TestUtil.getMockHttpRequest(admin.getId())).contains("not available"));
	}
	
	@StarexecTest
	private void getSolverBuildLogTest() {
		Assert.assertTrue(services.getSolverBuildLog(solver.getId(),
				TestUtil.getMockHttpRequest(user.getId())).contains("not available"));
		Assert.assertTrue(services.getSolverBuildLog(-1,
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
		anonymousJobId = AnonymousLinks.addAnonymousLink("job", job.getId(), PrimitivesToAnonymize.NONE);
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
