package org.starexec.test.integration.app;

import com.google.gson.Gson;
import org.junit.Assert;
import org.mockito.Mockito;
import org.starexec.app.RESTServices;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * This class contains tests for RESTServices that should get rejected for security reasons. These
 * tests exist to make sure that requests that would violate permissions actually do get rejected. Tests
 * for successful RESTService calls should be written in another class.
 * @author Eric Burns
 *
 */
public class RESTServicesSecurityTests extends TestSequence {
	private final RESTServices services = new RESTServices();
	User user = null; // user that should have no permissions to do anything
	User admin = null;

	// all of these will be owned by the admin
	Space space = null;
	Solver solver = null;
	List<Integer> benchmarkIds = null;
	Job job = null;
	BenchmarkUploadStatus benchmarkStatus = null;
	SpaceXMLUploadStatus spaceStatus = null;
	Website solverWebsite = null;
	int invalidBenchId = 0;
	String anonymousJobId = null;
	private final Gson gson = new Gson();
	Processor postProcessor = null;
	Queue allQ = null;
	private void assertResultIsInvalid(String result) {
		Assert.assertFalse(gson.fromJson(result, ValidatorStatusCode.class).isSuccess());
	}
	HttpServletRequest request = TestUtil.getMockHttpRequest(user.getId());

	@StarexecTest
	private void getFinishedPairsForMatrixTest() {
		assertResultIsInvalid(services.getFinishedJobPairsForMatrix(job.getPrimarySpace(),
				1, request));
		assertResultIsInvalid(services.getFinishedJobPairsForMatrix(-1, 1, request));
	}

	@StarexecTest
	private void getSolverComparisonsPaginatedTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();

		assertResultIsInvalid(services.getSolverComparisonsPaginated(false, job.getPrimarySpace(), cId, cId,
				request));
		assertResultIsInvalid(services.getSolverComparisonsPaginated(false, -1, cId, cId, request));
	}

	@StarexecTest
	private void getAnonymousLinkForPrimitiveTest() {
		assertResultIsInvalid(services.getAnonymousLinkForPrimitive("job",job.getId(),"none", request));
		assertResultIsInvalid(services.getAnonymousLinkForPrimitive("job",-1,"none", TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getAnonymousLinkForPrimitive("invalid",job.getId(),"none", TestUtil.getMockHttpRequest(admin.getId())));
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
		assertResultIsInvalid(services.getSpaceOverviewGraph(1,job.getPrimarySpace(),request));
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
		assertResultIsInvalid(services.getJobStatsPaginated(1,job.getPrimarySpace(),false,false, request));
	}

	@StarexecTest
	private void getSolverComparisonGraphTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();
		assertResultIsInvalid(services.getSolverComparisonGraph(1,
				-1,cId,cId,100,"black", TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getSolverComparisonGraph(1,
				job.getPrimarySpace(),cId,cId,100,"black",request));
	}


	@StarexecTest
	private void getJobPairsPaginatedTest() {
		assertResultIsInvalid(services.getJobPairsPaginated(1, false,
				job.getPrimarySpace(),false,request));
		assertResultIsInvalid(services.getJobPairsPaginated(1, false,
				-1,false, TestUtil.getMockHttpRequest(admin.getId())));
	}


	@StarexecTest
	private void getJobPairsInSpaceHierarchyByConfigPaginatedTest() {
		int cId = job.getJobPairs().get(0).getPrimaryConfiguration().getId();
		assertResultIsInvalid(services.
				getJobPairsInSpaceHierarchyByConfigPaginated(1, false, job.getPrimarySpace(),"all",cId, request));
		assertResultIsInvalid(services.
				getJobPairsInSpaceHierarchyByConfigPaginated(1, false, -1,"all",cId, request));
		assertResultIsInvalid(services.
				getJobPairsInSpaceHierarchyByConfigPaginated(1, false, job.getPrimarySpace(),"badtypestring",cId, request));
	}

	@StarexecTest
	private void recompileJobSpacesTest() {
		assertResultIsInvalid(services.recompileJobSpaces(job.getId(), request));
		assertResultIsInvalid(services.recompileJobSpaces(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void postProcessJobTest() {
		assertResultIsInvalid(services.postProcessJob(job.getId(),1,postProcessor.getId(),request));
		assertResultIsInvalid(services.postProcessJob(job.getId(),1,-1,TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.postProcessJob(-1,1,postProcessor.getId(),TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void rerunAllPairsTest() {
		assertResultIsInvalid(services.rerunAllJobPairs(job.getId(), request));
		assertResultIsInvalid(services.rerunAllJobPairs(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void rerunTimelessPairsTest() {
		assertResultIsInvalid(services.rerunTimelessJobPairs(job.getId(), request));
		assertResultIsInvalid(services.rerunTimelessJobPairs(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void rerunSinglePairTest() {
		assertResultIsInvalid(services.rerunJobPair(job.getJobPairs().get(0).getId(), request));
		assertResultIsInvalid(services.rerunJobPair(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void rerunAllPairsOfStatusTest() {
		assertResultIsInvalid(services.rerunJobPairs(job.getId(), StatusCode.STATUS_KILLED.getVal(), request));
		assertResultIsInvalid(services.rerunJobPairs(-1, StatusCode.STATUS_KILLED.getVal(), TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void editJobNameTest() {
		assertResultIsInvalid(services.editJobName(job.getId(), "test name", request));
		assertResultIsInvalid(services.editJobName(-1, "test name", TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void editJobDescTest() {
		assertResultIsInvalid(services.editJobDescription(job.getId(), "test desc", request));
		assertResultIsInvalid(services.editJobDescription(-1, "test desc", TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getAllBenchInSpaceTest() {
		assertResultIsInvalid(services.getAllBenchmarksInSpace(space.getId(), request));
		assertResultIsInvalid(services.getAllBenchmarksInSpace(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getPrimitiveDetailsPaginatedTest() {
		assertResultIsInvalid(services.getPrimitiveDetailsPaginated(space.getId(),"solver", request));
		assertResultIsInvalid(services.getPrimitiveDetailsPaginated(-1,"solver", TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getSolverWebsitesTest() {
		assertResultIsInvalid(services.getWebsites("solver",solver.getId(), request));
		assertResultIsInvalid(services.getWebsites("solver",-1, TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.getWebsites("invalid",solver.getId(), TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getSpaceWebsitesTest() {
		assertResultIsInvalid(services.getWebsites("space",space.getId(), request));
	}

	@StarexecTest
	private void deleteWebsiteTest() {
		assertResultIsInvalid(services.deleteWebsite(solverWebsite.getId(), request));
	}

	private Map<String,String> getNameAndUrlParams() {
		Map<String,String> params = new HashMap<>();
		params.put("name", "testname");
		params.put("url", "http://www.testsite.edu");
		return params;
	}

	@StarexecTest
	private void addSolverWebsiteTest() {
		assertResultIsInvalid(services.addWebsite("solver",solver.getId(), TestUtil.getMockHttpRequest(user.getId(), getNameAndUrlParams())));
		assertResultIsInvalid(services.addWebsite("solver",-1, TestUtil.getMockHttpRequest(admin.getId(),getNameAndUrlParams())));
		assertResultIsInvalid(services.addWebsite("invalid",solver.getId(), TestUtil.getMockHttpRequest(admin.getId(),getNameAndUrlParams())));
	}

	@StarexecTest
	private void addSpaceWebsiteTest() {
		assertResultIsInvalid(services.addWebsite("space",space.getId(), TestUtil.getMockHttpRequest(user.getId(), getNameAndUrlParams())));
		assertResultIsInvalid(services.addWebsite("space",-1, TestUtil.getMockHttpRequest(admin.getId(),getNameAndUrlParams())));
	}

	@StarexecTest
	private void addUserWebsiteTest() {
		assertResultIsInvalid(services.addWebsite("user",admin.getId(), TestUtil.getMockHttpRequest(user.getId(), getNameAndUrlParams())));
		assertResultIsInvalid(services.addWebsite("user",-1, TestUtil.getMockHttpRequest(admin.getId(),getNameAndUrlParams())));
	}

	@StarexecTest
	private void addUsersToSpaceTest() {
		HashMap<String, String> params = new HashMap<>();
		params.put("copyToSubspaces", "true");
		assertResultIsInvalid(services.addUsersToSpace(space.getId(),TestUtil.getMockHttpRequest(user.getId(), params, getSelectedIdParams(user.getId()))));
		assertResultIsInvalid(services.addUsersToSpace(-1,TestUtil.getMockHttpRequest(admin.getId(), params, getSelectedIdParams(user.getId()))));
		assertResultIsInvalid(services.addUsersToSpace(space.getId(),TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>(), getSelectedIdParams(user.getId()))));
		assertResultIsInvalid(services.addUsersToSpace(space.getId(),TestUtil.getMockHttpRequest(admin.getId(),params)));
	}

	@StarexecTest
	private void copySolversToSpaceTest() {
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		HashMap<String, String> params = new HashMap<>();
		params.put("copyToSubspaces", "true");
		params.put("copy", "true");
		assertResultIsInvalid(services.copySolversToSpace(space.getId(),
				TestUtil.getMockHttpRequest(user.getId(), params, getSelectedIdParams(solver.getId())), response));
		assertResultIsInvalid(services.copySolversToSpace(-1,
				TestUtil.getMockHttpRequest(admin.getId(), params, getSelectedIdParams(solver.getId())), response));
		assertResultIsInvalid(services.copySolversToSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), params), response));
		assertResultIsInvalid(services.copySolversToSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), params, getSelectedIdParams(-1)), response));
		assertResultIsInvalid(services.copySolversToSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>(), getSelectedIdParams(solver.getId())), response));
	}

	@StarexecTest
	private void copyBenchmarksToSpaceTest() {
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		HashMap<String, String> params = new HashMap<>();
		params.put("copy", "true");
		assertResultIsInvalid(services.copyBenchToSpace(space.getId(),
				TestUtil.getMockHttpRequest(user.getId(), params, getSelectedIdParams(benchmarkIds.get(0))), response));
		assertResultIsInvalid(services.copyBenchToSpace(-1,
				TestUtil.getMockHttpRequest(admin.getId(), params, getSelectedIdParams(benchmarkIds.get(0))), response));
		assertResultIsInvalid(services.copyBenchToSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), params), response));
		assertResultIsInvalid(services.copyBenchToSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), params, getSelectedIdParams(-1)), response));
		assertResultIsInvalid(services.copyBenchToSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0))), response));
	}

	@StarexecTest
	private void getBenchmarkUploadDescription() {
		assertResultIsInvalid(services.getBenchmarkUploadDescription(benchmarkStatus.getId(), request));
		assertResultIsInvalid(services.getBenchmarkUploadDescription(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getInvalidUploadedBenchmarkOutput() {
		Assert.assertTrue(services.getInvalidUploadedBenchmarkOutput(invalidBenchId,
				request).contains("You may only view your own benchmark uploads"));
	}

	@StarexecTest
	private void getJobPairLogTest() {
		Assert.assertTrue(services.getJobPairLog(job.getJobPairs().get(0).getId(),
				request).contains("does not have access"));
		Assert.assertTrue(services.getJobPairLog(-1,
				TestUtil.getMockHttpRequest(admin.getId())).contains("does not have access"));
	}

	@StarexecTest
	private void getJobPairStdoutTest() {
		Assert.assertTrue(services.getJobPairStdout(job.getJobPairs().get(0).getId(), 1, 100,
				request).contains("not available"));
		Assert.assertTrue(services.getJobPairStdout(-1, 1, 100,
				TestUtil.getMockHttpRequest(admin.getId())).contains("not available"));
	}

	@StarexecTest
	private void getBenchmarkContentTest() {
		Assert.assertTrue(services.getBenchmarkContent(benchmarkIds.get(0), 100,
				request).contains("not available"));
		Assert.assertTrue(services.getBenchmarkContent(-1, 100,
				TestUtil.getMockHttpRequest(admin.getId())).contains("not available"));
	}

	@StarexecTest
	private void getSolverBuildLogTest() {
		Assert.assertTrue(services.getSolverBuildLog(solver.getId(),
				request).contains("not available"));
		Assert.assertTrue(services.getSolverBuildLog(-1,
				TestUtil.getMockHttpRequest(admin.getId())).contains("not available"));
	}

	@StarexecTest
	private void editUserInfoTest() {
		assertResultIsInvalid(services.editUserInfo("firstname",admin.getId(),"newname",request));
		assertResultIsInvalid(services.editUserInfo("badattr",user.getId(),"newname",request));
		assertResultIsInvalid(services.editUserInfo("firstname",user.getId(),TestUtil.getRandomAlphaString(500),request));
	}

	@StarexecTest
	private void setSettingsProfileForUserTest() {
		assertResultIsInvalid(services.setSettingsProfileForUser(Communities.getDefaultSettings(space.getId()).getId(),user.getId(),request));
	}

	@StarexecTest
	private void deleteDefaultSettingsTest() {
		assertResultIsInvalid(services.deleteDefaultSettings(Communities.getDefaultSettings(space.getId()).getId(),request));
	}

	@StarexecTest
	private void editCommunityDefaultSettingsTest() {
		Map<String,String> params = new HashMap<>();
		params.put("val", solver.getId()+"");
		assertResultIsInvalid(services.editCommunityDefaultSettings("defaultsolver",Communities.getDefaultSettings(space.getId()).getId(),TestUtil.getMockHttpRequest(user.getId(), params)));
		assertResultIsInvalid(services.editCommunityDefaultSettings("fakeattr",Communities.getDefaultSettings(space.getId()).getId(),TestUtil.getMockHttpRequest(admin.getId(), params)));
	}

	@StarexecTest
	private void editCommunityTest() {
		Map<String,String> params = new HashMap<>();
		params.put("val", solver.getId()+"");
		assertResultIsInvalid(services.editCommunityDetails("name",Communities.getDefaultSettings(space.getId()).getId(),TestUtil.getMockHttpRequest(user.getId(), params)));
		assertResultIsInvalid(services.editCommunityDetails("fakeattr",Communities.getDefaultSettings(space.getId()).getId(),TestUtil.getMockHttpRequest(admin.getId(), params)));
	}

	@StarexecTest
	private void editSpaceTest() {
		Map<String,String> params = new HashMap<>();
		params.put("name", "name");
		params.put("description","desc");
		params.put("locked","false");
		assertResultIsInvalid(services.editSpace(Communities.getDefaultSettings(space.getId()).getId(),TestUtil.getMockHttpRequest(user.getId(), params)));
		assertResultIsInvalid(services.editSpace(Communities.getDefaultSettings(space.getId()).getId(),TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void editQueueInfoTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("cpuTimeout", "10");
		params.put("wallTimeout", "10");
		assertResultIsInvalid(services.editQueueInfo(Queues.getAllQ().getId(),TestUtil.getMockHttpRequest(user.getId(), params)));
	}

	@StarexecTest
	private void editQueueInfoInvalidParamsTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("cpuTimeout", "test");
		params.put("wallTimeout", "10");
		assertResultIsInvalid(services.editQueueInfo(Queues.getAllQ().getId(),TestUtil.getMockHttpRequest(admin.getId(), params)));
	}

	@StarexecTest
	private void cleanErrorStatesTest() {
		assertResultIsInvalid(services.clearErrorStates(request));
	}

	@StarexecTest
	private void getAllPrimitiveDetailsPaginationTest() {
		assertResultIsInvalid(services.getAllUsersDetailsPagination(request));
	}

	@StarexecTest
	private void runTestsByNameTest() {
		assertResultIsInvalid(services.runTest(request));
	}

	@StarexecTest
	private void runAllTestsTest() {
		assertResultIsInvalid(services.runAllTests(request));
	}

	private static HashMap<String,List<String>> getSelectedIdParams(Object...objects) {
		HashMap<String,List<String>> map = new HashMap<>();
		List<String> strs = new ArrayList<>();
		for (Object o : objects) {
			strs.add(o.toString());
		}
		map.put("selectedIds[]", strs);
		return map;
	}

	@StarexecTest
	private void editProcessorTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("name", "newname");
		params.put("desc", "newdesc");
		assertResultIsInvalid(services.editProcessor(postProcessor.getId(),TestUtil.getMockHttpRequest(
				user.getId(), params)));
		assertResultIsInvalid(services.editProcessor(postProcessor.getId(),TestUtil.getMockHttpRequest(
				admin.getId())));
	}

	@StarexecTest
	private void leaveCommunityTest() {
		assertResultIsInvalid(services.leaveCommunity(space.getId(),TestUtil.getMockHttpRequest(
				user.getId())));
		assertResultIsInvalid(services.leaveCommunity(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void deleteProcessorsTest() {
		assertResultIsInvalid(services.deleteProcessors(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(postProcessor.getId()))));
		assertResultIsInvalid(services.deleteProcessors(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}


	@StarexecTest
	private void removeBenchmarksTest() {
		assertResultIsInvalid(services.removeBenchmarksFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
		assertResultIsInvalid(services.removeBenchmarksFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.removeBenchmarksFromSpace(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
	}

	@StarexecTest
	private void recycleBenchmarksTest() {
		assertResultIsInvalid(services.recycleBenchmarks(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
		assertResultIsInvalid(services.recycleBenchmarks(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void recycleOrphanedBenchmarksTest() {
		assertResultIsInvalid(services.recycleOrphanedBenchmarks(admin.getId(),request));
		assertResultIsInvalid(services.recycleOrphanedBenchmarks(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void restoreBenchmarksTest() {
		assertResultIsInvalid(services.restoreBenchmarks(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
		assertResultIsInvalid(services.restoreBenchmarks(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void deleteBenchmarksTest() {
		assertResultIsInvalid(services.deleteBenchmarks(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
		assertResultIsInvalid(services.deleteBenchmarks(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void editBenchmarkTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("name", "test");
		params.put("downloadable","false");
		params.put("type", ""+Benchmarks.get(benchmarkIds.get(0)).getType().getId());
		assertResultIsInvalid(services.editBenchmarkDetails(solver.getId(),TestUtil.getMockHttpRequest(user.getId(),params)));
		assertResultIsInvalid(services.editBenchmarkDetails(solver.getId(),TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.editBenchmarkDetails(-1,TestUtil.getMockHttpRequest(admin.getId(),params)));
	}

	@StarexecTest
	private void recycleAndRemoveBenchmarksTest() {
		assertResultIsInvalid(services.recycleAndRemoveBenchmarks(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
		assertResultIsInvalid(services.recycleAndRemoveBenchmarks(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.recycleAndRemoveBenchmarks(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(benchmarkIds.get(0), benchmarkIds.get(1)))));
	}

	@StarexecTest
	private void removeUsersTest() {
		assertResultIsInvalid(services.removeUsersFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(admin.getId()))));
		assertResultIsInvalid(services.removeUsersFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.removeUsersFromSpace(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(admin.getId()))));
	}

	@StarexecTest
	private void deleteUserTest() {
		assertResultIsInvalid(services.deleteUser(admin.getId(),request));
		assertResultIsInvalid(services.deleteUser(user.getId(),request));
		assertResultIsInvalid(services.deleteUser(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void makeLeaderTest() {
		assertResultIsInvalid(services.makeLeader(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(user.getId()))));

		assertResultIsInvalid(services.makeLeader(-1,TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(admin.getId()))));

		assertResultIsInvalid(services.makeLeader(space.getId(),TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void demoteLeaderTest() {
		assertResultIsInvalid(services.demoteLeader(space.getId(),user.getId(),request));
		assertResultIsInvalid(services.demoteLeader(-1,user.getId(),TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.demoteLeader(space.getId(),-1,TestUtil.getMockHttpRequest(admin.getId())));
	}


	@StarexecTest
	private void editUserPasswordTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("current", admin.getPassword());
		params.put("newpass", "fe90j$#T!Dvioew");
		params.put("confirm", "fe90j$#T!Dvioew");
		assertResultIsInvalid(services.editUserPassword(admin.getId(),TestUtil.getMockHttpRequest(user.getId(), params)));
		assertResultIsInvalid(services.editUserPassword(-1,TestUtil.getMockHttpRequest(admin.getId(), params)));
		assertResultIsInvalid(services.editUserPassword(admin.getId(),TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void editUserPermissionsTest() {
		assertResultIsInvalid(services.editUserPermissions(space.getId(),admin.getId(),request));
		assertResultIsInvalid(services.editUserPermissions(-1,admin.getId(),TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.editUserPermissions(space.getId(),-1,TestUtil.getMockHttpRequest(admin.getId())));


	}


	@StarexecTest
	private void removeSolversTest() {
		assertResultIsInvalid(services.removeSolversFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
		assertResultIsInvalid(services.removeSolversFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.removeSolversFromSpace(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
	}

	@StarexecTest
	private void deleteSolversTest() {
		assertResultIsInvalid(services.deleteSolvers(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
		assertResultIsInvalid(services.deleteSolvers(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));
		assertResultIsInvalid(services.deleteSolvers(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void canLinkAllOrphanedTest() {
		assertResultIsInvalid(services.linkAllOrphanedPrimitives(admin.getId(), space.getId(), request));
		assertResultIsInvalid(services.linkAllOrphanedPrimitives(admin.getId(), -1, TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.linkAllOrphanedPrimitives(-1, space.getId(), TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void recycleAndRemoveSolversTest() {
		assertResultIsInvalid(services.recycleAndRemoveSolvers(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
		assertResultIsInvalid(services.recycleAndRemoveSolvers(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));
		assertResultIsInvalid(services.recycleAndRemoveSolvers(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.recycleAndRemoveSolvers(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
	}

	@StarexecTest
	private void restoreSolversTest() {
		assertResultIsInvalid(services.restoreSolvers(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
		assertResultIsInvalid(services.restoreSolvers(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void recycleSolversTest() {
		assertResultIsInvalid(services.recycleSolvers(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(solver.getId()))));
		assertResultIsInvalid(services.recycleSolvers(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));
		assertResultIsInvalid(services.recycleSolvers(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void recycleOrphanedSolversTest() {
		assertResultIsInvalid(services.recycleOrphanedSolvers(admin.getId(),request));
		assertResultIsInvalid(services.recycleOrphanedSolvers(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void deleteConfigurationTest() {
		assertResultIsInvalid(services.deleteConfigurations(TestUtil.getMockHttpRequest(user.getId(),
				new HashMap<>(), getSelectedIdParams(solver.getConfigurations().get(0).getId()))));
		assertResultIsInvalid(services.deleteConfigurations(TestUtil.getMockHttpRequest(admin.getId(),
				new HashMap<>(), getSelectedIdParams(-1))));
		assertResultIsInvalid(services.deleteConfigurations(TestUtil.getMockHttpRequest(admin.getId(),
				new HashMap<>())));
	}

	@StarexecTest
	private void editConfigurationTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("name", "test");
		int configId = solver.getConfigurations().get(0).getId();
		assertResultIsInvalid(services.editConfigurationDetails(configId,TestUtil.getMockHttpRequest(user.getId(), params)));
		assertResultIsInvalid(services.editConfigurationDetails(configId,TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.editConfigurationDetails(-1,TestUtil.getMockHttpRequest(admin.getId(), params)));
	}

	@StarexecTest
	private void editSolverTest() {
		HashMap<String,String> params = new HashMap<>();
		params.put("name", "test");
		params.put("downloadable","false");
		assertResultIsInvalid(services.editSolverDetails(solver.getId(),TestUtil.getMockHttpRequest(user.getId(),params)));
		assertResultIsInvalid(services.editSolverDetails(solver.getId(),TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.editSolverDetails(-1,TestUtil.getMockHttpRequest(admin.getId(),params)));
	}


	@StarexecTest
	private void associateJobWithSpaceTest() {
		assertResultIsInvalid(services.associateJobWithSpace(space.getId(),
			TestUtil.getMockHttpRequest(user.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));
		assertResultIsInvalid(services.associateJobWithSpace(-1,
				TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));

		assertResultIsInvalid(services.associateJobWithSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));

		assertResultIsInvalid(services.associateJobWithSpace(space.getId(),
				TestUtil.getMockHttpRequest(admin.getId(), new HashMap<>())));
	}

	@StarexecTest
	private void removeJobsTest() {
		assertResultIsInvalid(services.removeJobsFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));
		assertResultIsInvalid(services.removeJobsFromSpace(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.removeJobsFromSpace(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));
	}

	@StarexecTest
	private void pauseJobTest() {
		assertResultIsInvalid(services.pauseJob(job.getId(), request));
		assertResultIsInvalid(services.pauseJob(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void resumeJobTest() {
		assertResultIsInvalid(services.resumeJob(job.getId(), request));
		assertResultIsInvalid(services.resumeJob(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void changeJobQueueTest() {
		assertResultIsInvalid(services.changeQueueJob(job.getId(), allQ.getId(),request));
		assertResultIsInvalid(services.changeQueueJob(-1, allQ.getId(),TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.changeQueueJob(job.getId(), -1,TestUtil.getMockHttpRequest(admin.getId())));

	}

	@StarexecTest
	private void deleteAndRemoveJobsTest() {
		assertResultIsInvalid(services.deleteAndRemoveJobs(space.getId(),TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));
		assertResultIsInvalid(services.deleteAndRemoveJobs(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.deleteAndRemoveJobs(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));

		assertResultIsInvalid(services.deleteAndRemoveJobs(space.getId(),TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));
	}

	@StarexecTest
	private void deleteJobsTest() {
		assertResultIsInvalid(services.deleteJobs(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(job.getId()))));
		assertResultIsInvalid(services.deleteJobs(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.deleteJobs(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));
	}

	@StarexecTest
	private void removeSubspacesTest() {
		assertResultIsInvalid(services.removeSubspacesFromSpace(TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(space.getId()))));
		assertResultIsInvalid(services.removeSubspacesFromSpace(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>())));
		assertResultIsInvalid(services.removeSubspacesFromSpace(TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(-1))));
	}

	@StarexecTest
	private void copySubspaceTest() {
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		int destSpace = Communities.getTestCommunity().getId();
		HashMap<String,String> params = new HashMap<>();
		params.put("copyHierarchy", "false");
		assertResultIsInvalid(services.copySubSpaceToSpace(destSpace,TestUtil.getMockHttpRequest(
				user.getId(), new HashMap<>(), getSelectedIdParams(space.getId())), response));

		assertResultIsInvalid(services.copySubSpaceToSpace(-1,TestUtil.getMockHttpRequest(
				admin.getId(), new HashMap<>(), getSelectedIdParams(space.getId())), response));

		assertResultIsInvalid(services.copySubSpaceToSpace(destSpace,TestUtil.getMockHttpRequest(
				admin.getId()), response));
	}

	@StarexecTest
	private void makePublicTest() {
		assertResultIsInvalid(services.makePublic(space.getId(), false, false, request));
		assertResultIsInvalid(services.makePublic(-1, false, false, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getUserJobsPaginatedTest() {
		assertResultIsInvalid(services.getUserJobsPaginated(admin.getId(), request));
		assertResultIsInvalid(services.getUserJobsPaginated(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getUserSolversPaginatedTest() {
		assertResultIsInvalid(services.getUserSolversPaginated(admin.getId(), request));
		assertResultIsInvalid(services.getUserSolversPaginated(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getUserRecycledSolversPaginatedTest() {
		assertResultIsInvalid(services.getUserRecycledSolversPaginated(admin.getId(), request));
		assertResultIsInvalid(services.getUserRecycledSolversPaginated(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getUserBenchmarksPaginatedTest() {
		assertResultIsInvalid(services.getUserBenchmarksPaginated(admin.getId(), request));
		assertResultIsInvalid(services.getUserBenchmarksPaginated(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getUserRecycledBenchmarksPaginatedTest() {
		assertResultIsInvalid(services.getUserRecycledBenchmarksPaginated(admin.getId(), request));
		assertResultIsInvalid(services.getUserRecycledBenchmarksPaginated(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getTestsPagninatedTest() {
		assertResultIsInvalid(services.getTestsPaginated(request));
	}

	@StarexecTest
	private void removeQueueTest() {
		assertResultIsInvalid(services.removeQueue(allQ.getId(),request));
		assertResultIsInvalid(services.removeQueue(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void setLoggingLevelForClassTest() {
		assertResultIsInvalid(services.setLoggingLevel("trace", "org.starexec.app.RESTServices.java",
				request));

		assertResultIsInvalid(services.setLoggingLevel("badlevel", "org.starexec.app.RESTServices.java",
				TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.setLoggingLevel("trace", "badclass",
				TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void setLoggingLevelTest() {
		assertResultIsInvalid(services.setLoggingLevel("trace",
				request));

		assertResultIsInvalid(services.setLoggingLevel("badlevel",TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void setLoggingLevelOffForAllExceptClassTest() {
		assertResultIsInvalid(services.setLoggingLevelOffForAllExceptClass("trace", "org.starexec.app.RESTServices.java",
				request));

		assertResultIsInvalid(services.setLoggingLevelOffForAllExceptClass("badlevel", "org.starexec.app.RESTServices.java",
				TestUtil.getMockHttpRequest(admin.getId())));
		assertResultIsInvalid(services.setLoggingLevelOffForAllExceptClass("trace", "badclass",
				TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getTestResultsPagninatedTest() {
		assertResultIsInvalid(services.getTestResultsPaginated(testName,request));
		assertResultIsInvalid(services.getTestResultsPaginated(TestUtil.getRandomAlphaString(50),request));
	}

	@StarexecTest
	private void getAllPendingCommunityRequestsTest() {
		assertResultIsInvalid(services.getAllPendingCommunityRequests(request));
	}

	@StarexecTest
	private void getPendingCommunityRequestsForCommunityTest() {
		int cId = Communities.getTestCommunity().getId();
		assertResultIsInvalid(services.getPendingCommunityRequestsForCommunity(cId,request));
		assertResultIsInvalid(services.getPendingCommunityRequestsForCommunity(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void restartStarexecTest() throws Exception {
		assertResultIsInvalid(services.restartStarExec(request));
	}

	@StarexecTest
	private void clearCacheTest() {
		assertResultIsInvalid(services.clearCache(job.getId(),request));
		assertResultIsInvalid(services.clearCache(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void clearStatsCacheTest() {
		assertResultIsInvalid(services.clearStatsCache(request));
	}

	@StarexecTest
	private void pauseAllTest() {
		assertResultIsInvalid(services.pauseAll(request));
	}

	@StarexecTest
	private void resumeAllTest() {
		assertResultIsInvalid(services.resumeAll(request));
	}

	@StarexecTest
	private void makeQueueGlobalTest() {
		assertResultIsInvalid(services.makeQueueGlobal(allQ.getId(),request));
		assertResultIsInvalid(services.makeQueueGlobal(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void removeQueueGlobalTest(){
		assertResultIsInvalid(services.removeQueueGlobal(allQ.getId(),request));
		assertResultIsInvalid(services.removeQueueGlobal(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void suspendUserTest() {
		assertResultIsInvalid(services.suspendUser(user.getId(), request));
		assertResultIsInvalid(services.suspendUser(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void reinstateUserTest() {
		assertResultIsInvalid(services.reinstateUser(user.getId(), request));
		assertResultIsInvalid(services.reinstateUser(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void subscribeUserTest() {
		assertResultIsInvalid(services.subscribeUser(admin.getId(), request));
		assertResultIsInvalid(services.subscribeUser(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void unsubscribeUserTest() {
		assertResultIsInvalid(services.unsubscribeUser(admin.getId(), request));
		assertResultIsInvalid(services.unsubscribeUser(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void grantDeveloperStatusTest() {
		assertResultIsInvalid(services.grantDeveloperStatus(user.getId(), request));
		assertResultIsInvalid(services.grantDeveloperStatus(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void suspendDeveloperStatusTest() {
		assertResultIsInvalid(services.suspendDeveloperStatus(user.getId(), request));
		assertResultIsInvalid(services.suspendDeveloperStatus(-1, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void clearLoadBalanceDataTest() {
		assertResultIsInvalid(services.clearLoadBalanceData(request));
	}

	@StarexecTest
	private void clearSolverCacheTest(){
		assertResultIsInvalid(services.clearSolverCache(request));
	}

	@StarexecTest
	private void updateDebugModeTest()  {
		assertResultIsInvalid(services.updateDebugMode(false,request));
	}

	@StarexecTest
	private void setTestQueueTest(){
		assertResultIsInvalid(services.setTestQueue(allQ.getId(),request));
		assertResultIsInvalid(services.setTestQueue(-1,TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveSolverTest() {
		assertResultIsInvalid(services.getGsonPrimitive(solver.getId(), R.SOLVER, request));
		assertResultIsInvalid(services.getGsonPrimitive(-1, R.SOLVER, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveBenchmarkTest() {
		assertResultIsInvalid(services.getGsonPrimitive(benchmarkIds.get(0), "bench", request));
		assertResultIsInvalid(services.getGsonPrimitive(-1, "bench", TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveJobTest() {
		assertResultIsInvalid(services.getGsonPrimitive(job.getId(), R.JOB, request));
		assertResultIsInvalid(services.getGsonPrimitive(-1, R.JOB, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveSpaceTest() {
		assertResultIsInvalid(services.getGsonPrimitive(space.getId(), R.SPACE, request));
		assertResultIsInvalid(services.getGsonPrimitive(-1, R.SPACE, TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveProcessorTest() {
		assertResultIsInvalid(services.getGsonPrimitive(postProcessor.getId(), "processor", request));
		assertResultIsInvalid(services.getGsonPrimitive(-1,"processor", TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveConfigurationTest() {
		int cId = solver.getConfigurations().get(0).getId();
		assertResultIsInvalid(services.getGsonPrimitive(cId, "configuration", request));
		assertResultIsInvalid(services.getGsonPrimitive(-1, "configuration", TestUtil.getMockHttpRequest(admin.getId())));
	}

	@StarexecTest
	private void getGsonPrimitiveInvalidTypeTest() {
		assertResultIsInvalid(services.getGsonPrimitive(1, "badType", TestUtil.getMockHttpRequest(admin.getId())));
	}

	private static final String testName = "RESTServicesSecurityTests";

	@Override
	protected String getTestName() {
		return testName;
	}

	@Override
	protected void setup() throws Exception {
		user = loader.loadUserIntoDatabase();
		admin = loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space = loader.loadSpaceIntoDatabase(admin.getId(), 1);
		solver = loader.loadSolverIntoDatabase(space.getId(), admin.getId());
		benchmarkIds = loader.loadBenchmarksIntoDatabase(space.getId(), admin.getId());
		job = loader.loadJobIntoDatabase(space.getId(), admin.getId(), solver.getId(), benchmarkIds);
		anonymousJobId = AnonymousLinks.addAnonymousLink("job", job.getId(), PrimitivesToAnonymize.NONE);
		benchmarkStatus = Uploads.getBenchmarkStatus(Uploads.createBenchmarkUploadStatus(space.getId(), admin.getId()));
		spaceStatus = Uploads.getSpaceXMLStatus(Uploads.createSpaceXMLUploadStatus(admin.getId()));
		Uploads.addFailedBenchmark(benchmarkStatus.getId(), "failed", "test message");
		invalidBenchId = Uploads.getFailedBenches(benchmarkStatus.getId()).get(0).getId();
		Websites.add(solver.getId(), "http://www.fakesite.com", "testsite", WebsiteType.SOLVER);
		solverWebsite = Websites.getAll(solver.getId(), WebsiteType.SOLVER).get(0);
		postProcessor = loader.loadProcessorIntoDatabase(ProcessorType.POST, space.getId());
		allQ = Queues.getAllQ();
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
