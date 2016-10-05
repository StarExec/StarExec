package org.starexec.test.integration.app;

import java.util.EnumSet;
import java.util.List;

import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.Util;

import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.starexec.app.RESTHelpers;
import org.starexec.constants.R;
import org.starexec.data.to.enums.Primitive;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.*;
import static org.mockito.Mockito.*;


public class RESTHelpersTests extends TestSequence {
	private static String TOTAL_RECORDS = "iTotalRecords";
	private static String DATA = "aaData";
	private static final String TOTAL_RECORDS_AFTER_QUERY = "iTotalDisplayRecords";
	private static final EnumSet<PrimitivesToAnonymize> allPrimitivesToAnonymize = EnumSet.allOf( PrimitivesToAnonymize.class );
	// owner of the test data for these tests
	User testUser = null;
	User extraUser = null;
	Space community = null;
	Space space1 = null;
	Space space2 = null;
	Space childOf1 = null;
	User admin = null;
	
	Solver s1 = null;
	Solver s2 = null;
	Job j1 = null;
	JobSpace j1PrimarySpace = null;
	Job j2 = null;
	List<Integer> benchmarkIds = null;
	Queue q = null;
	private static HttpSession getMockSessionWithUser(int userId) {
		HttpSession session = mock(HttpSession.class);
		User u = new User();
		u.setId(userId);
		when(session.getAttribute("user")).thenReturn(u);
		return session;
	}
	
	private static HttpServletRequest getMockRequest(DataTablesQuery query, int userId) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("sEcho")).thenReturn(String.valueOf(query.getSyncValue()));
		when(request.getParameter("sSearch")).thenReturn(query.getSearchQuery());
		when(request.getParameter("sSortDir_0")).thenReturn(query.isSortASC() ? "asc" : "desc");
		when(request.getParameter("iSortCol_0")).thenReturn(String.valueOf(query.getSortColumn()));
		when(request.getParameter("iDisplayStart")).thenReturn(String.valueOf(query.getStartingRecord()));
		when(request.getParameter("iDisplayLength")).thenReturn(String.valueOf(query.getNumRecords()));
		HttpSession session = getMockSessionWithUser(userId);
		when(request.getSession()).thenReturn(session);
		return request;
	}
	
	private static HttpServletRequest getMockRequest(int userId) {
		return getMockRequest(getTestDataTablesQuery(), userId);
	}
	
	private static DataTablesQuery getTestDataTablesQuery() {
		DataTablesQuery q = new DataTablesQuery();
		q.setSyncValue(1);
		q.setNumRecords(10);
		q.setStartingRecord(0);
		q.setSortASC(true);
		q.setSortColumn(0);
		q.setSearchQuery("");
		return q;
	}
	
	private void validateJsonObjectCounts(JsonObject o, int totalRecords, int totalRecordsAfterQuery, int pageSize) {
		Assert.assertEquals(totalRecords,o.get(TOTAL_RECORDS).getAsInt());
		Assert.assertEquals(totalRecordsAfterQuery,o.get(TOTAL_RECORDS_AFTER_QUERY).getAsInt());
		Assert.assertEquals(pageSize,o.get(DATA).getAsJsonArray().size());
	}
	
	@StarexecTest
	private void getNextPageOfSpacesTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.SPACE, community.getId(), getMockRequest(testUser.getId()));
		validateJsonObjectCounts(o, 2,2,2);
	}
	@StarexecTest
	private void getNextPageOfSpacesSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.SPACE, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,2,1);

	}
	@StarexecTest
	private void getNextPageOfSpacesQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(space1.getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.SPACE, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,1,1);

	}
	
	@StarexecTest
	private void getNextPageOfUsersTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.USER, community.getId(), getMockRequest(testUser.getId()));
		validateJsonObjectCounts(o, 2,2,2);

	}
	@StarexecTest
	private void getNextPageOfUsersSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.USER, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,2,1);

	}
	@StarexecTest
	private void getNextPageOfUsersQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(testUser.getFirstName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.USER, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,1,1);
	}
	
	@StarexecTest
	private void getNextPageOfSolversTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.SOLVER, community.getId(), getMockRequest(testUser.getId()));
		validateJsonObjectCounts(o, 2,2,2);

	}
	@StarexecTest
	private void getNextPageOfSolversSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.SOLVER, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,2,1);

	}
	@StarexecTest
	private void getNextPageOfSolversQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(s1.getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.SOLVER, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,1,1);
	}
	
	@StarexecTest
	private void getNextPageOfBenchmarksTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.BENCHMARK, community.getId(), getMockRequest(testUser.getId()));
		validateJsonObjectCounts(o, benchmarkIds.size(),benchmarkIds.size(),benchmarkIds.size());

	}
	@StarexecTest
	private void getNextPageOfBenchmarksSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.BENCHMARK, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, benchmarkIds.size(),benchmarkIds.size(),1);

	}
	@StarexecTest
	private void getNextPageOfBenchmarksQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(Benchmarks.get(benchmarkIds.get(0)).getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.BENCHMARK, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, benchmarkIds.size(),1,1);
	}
	
	
	@StarexecTest
	private void getNextPageOfJobsTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.JOB, community.getId(), getMockRequest(testUser.getId()));
		validateJsonObjectCounts(o, 2,2,2);

	}
	@StarexecTest
	private void getNextPageOfJobSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.JOB, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,2,1);

	}
	@StarexecTest
	private void getNextPageOfJobsQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(j1.getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForSpaceExplorer(Primitive.JOB, community.getId(), getMockRequest(q, testUser.getId()));
		validateJsonObjectCounts(o, 2,1,1);
	}
	
	@StarexecTest
	private void getNextPageOfUserJobsTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.JOB, testUser.getId(), getMockRequest(testUser.getId()), false, false);
		validateJsonObjectCounts(o, 2,2,2);

	}
	@StarexecTest
	private void getNextPageOfUserJobsSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.JOB, testUser.getId(), getMockRequest(q, testUser.getId()), false, false);
		validateJsonObjectCounts(o, 2,2,1);

	}
	@StarexecTest
	private void getNextPageOfUserJobsQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(j1.getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.JOB, testUser.getId(), getMockRequest(q, testUser.getId()), false, false);
		validateJsonObjectCounts(o, 2,1,1);
	}
	
	
	@StarexecTest
	private void getNextPageOfUserSolversTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.SOLVER, testUser.getId(), getMockRequest(testUser.getId()), false, false);
		validateJsonObjectCounts(o, 2,2,2);

	}
	@StarexecTest
	private void getNextPageOfUserSolversSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.SOLVER, testUser.getId(), getMockRequest(q, testUser.getId()), false, false);
		validateJsonObjectCounts(o, 2,2,1);

	}
	@StarexecTest
	private void getNextPageOfUserSolversQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(s1.getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.SOLVER, testUser.getId(), getMockRequest(q, testUser.getId()), false, false);
		validateJsonObjectCounts(o, 2,1,1);
	}
	
	
	@StarexecTest
	private void getNextPageOfUserBenchmarksTest() {
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.BENCHMARK, testUser.getId(), getMockRequest(testUser.getId()), false, false);
		validateJsonObjectCounts(o, benchmarkIds.size(),benchmarkIds.size(),benchmarkIds.size());

	}
	@StarexecTest
	private void getNextPageOfUserBenchmarksSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.BENCHMARK, testUser.getId(), getMockRequest(q, testUser.getId()), false, false);
		validateJsonObjectCounts(o, benchmarkIds.size(),benchmarkIds.size(),1);

	}
	@StarexecTest
	private void getNextPageOfUserBenchmarksQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(Benchmarks.get(benchmarkIds.get(0)).getName());
		JsonObject o = RESTHelpers.getNextDataTablesPageForUserDetails(Primitive.BENCHMARK, testUser.getId(), getMockRequest(q, testUser.getId()), false, false);
		validateJsonObjectCounts(o, benchmarkIds.size(),1,1);
	}
	
	@StarexecTest
	private void getNextPageOfJobPairsInJobSpaceTest() {
		for (PrimitivesToAnonymize primitivesToAnonymize : allPrimitivesToAnonymize ) {
			JsonObject o = RESTHelpers.getNextDataTablesPageOfPairsInJobSpace(
					j1PrimarySpace.getId(), getMockRequest(testUser.getId()), false, false, 0, primitivesToAnonymize);
			validateJsonObjectCounts(o, j1.getJobPairs().size(), j1.getJobPairs().size(), j1.getJobPairs().size());
		}
	}
	
	@StarexecTest
	private void getNextPageOfJobPairsInJobSpaceSingleRecordTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setNumRecords(1);
		for (PrimitivesToAnonymize primitivesToAnonymize : allPrimitivesToAnonymize ) {
			JsonObject o = RESTHelpers.getNextDataTablesPageOfPairsInJobSpace(
					j1PrimarySpace.getId(), getMockRequest(q, testUser.getId()), false, false, 0, primitivesToAnonymize);
			validateJsonObjectCounts(o, j1.getJobPairs().size(), j1.getJobPairs().size(), 1);
		}
	}
	
	@StarexecTest
	private void getNextPageOfJobPairsInJobSpaceQueryTest() {
		DataTablesQuery q = getTestDataTablesQuery();
		q.setSearchQuery(j1.getJobPairs().get(0).getBench().getName());
		for ( PrimitivesToAnonymize primitivesToAnonymize : allPrimitivesToAnonymize ) {
			JsonObject o = RESTHelpers.getNextDataTablesPageOfPairsInJobSpace(
					j1PrimarySpace.getId(), getMockRequest(q, testUser.getId()), false, false, 0, primitivesToAnonymize);
			validateJsonObjectCounts(o, j1.getJobPairs().size(), 1, 1);
		}
	}
	
	
	@Override
	protected String getTestName() {
		return "RestHelpersTests";
	}

	@Override
	protected void setup() throws Exception {
		testUser = loader.loadUserIntoDatabase();
		extraUser = loader.loadUserIntoDatabase();
		community = loader.loadSpaceIntoDatabase(testUser.getId(), 1);
		space1=loader.loadSpaceIntoDatabase(testUser.getId(), community.getId());
		space2=loader.loadSpaceIntoDatabase(testUser.getId(), community.getId());
		childOf1=loader.loadSpaceIntoDatabase(testUser.getId(), space1.getId());
		admin = loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		Users.associate(extraUser.getId(), community.getId());
		s1 = loader.loadSolverIntoDatabase(community.getId(), testUser.getId());
		s2 = loader.loadSolverIntoDatabase(community.getId(), testUser.getId());
		benchmarkIds = loader.loadBenchmarksIntoDatabase(community.getId(), testUser.getId());
		j1 = loader.loadJobIntoDatabase(community.getId(), testUser.getId(), s1.getId(), benchmarkIds);
		j1PrimarySpace = Spaces.getJobSpace(j1.getPrimarySpace());
		j2 = loader.loadJobIntoDatabase(community.getId(), testUser.getId(), s1.getId(), benchmarkIds);
		q=Queues.getAllQ();
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
