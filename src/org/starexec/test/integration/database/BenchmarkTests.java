package org.starexec.test.integration.database;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.starexec.constants.DB;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.logger.StarLogger;
import org.starexec.servlets.UploadBenchmark;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tests for org.starexec.data.database.Benchmarks.java
 * @author Eric
 */
public class BenchmarkTests extends TestSequence {
	private static final StarLogger log = StarLogger.getLogger(BenchmarkTests.class);
	private User user=null;
	private User user2=null;
	private User admin=null;
	private Space space=null;
	private Space space2 = null;
	private Space scratchSpace = null; // A space that is not ever read in any test. Use this space to place benchmarks
									   // needed for single tests
	private List<Benchmark> benchmarks=null; //owned by user. Should be the only benchmarks in space
	Processor benchProcessor = null;

	private Solver solver=null; //solver to use for the job
	private Job job=null;
	private Processor postProc=null; //post processor to use for the job
	//private List<Integer> benchmarkIds=null; // benchmarks to use for the job
	private final int cpuTimeout=100;
	private final int gbMemory=1;

	private Job job2=null;
	private Random rand = new Random();


	@Override
	protected String getTestName() {
		return "BenchmarkTests";
	}


	@StarexecTest
	private void GetByUser() {
		List<Benchmark> benches=Benchmarks.getByOwner(user.getId());
		Assert.assertNotNull(benches);
		Assert.assertEquals(benchmarks.size(),benches.size());
		for (Benchmark b : benches) {
			Assert.assertTrue(containsBenchmark(benchmarks,b));
		}
	}

	@StarexecTest
	private void GetBySpace() {
		List<Benchmark> benches=Benchmarks.getBySpace(space.getId());
		Assert.assertNotNull(benches);
		Assert.assertEquals(benchmarks.size(),benches.size());
		for (Benchmark b : benches) {
			log.debug(String.valueOf(b.getId()));
			Assert.assertTrue(containsBenchmark(benchmarks,b));
		}
	}

	@StarexecTest
	private void GetByJobTest() {
		try {
			// This is the method we're testing. It retrieves all the benchmarks in a job.
			List<Benchmark> benchmarks = Benchmarks.getByJob(job.getId());

			// Convert the benchmarks to their ids.
			Set<Integer> benchmarkIds = benchmarks.stream()
					.map(Identifiable::getId)
					.collect(Collectors.toSet());


			// Assert that all the benchmarks in the job are in the benchmarks we retrieved.
			for (JobPair pair : job.getJobPairs()) {
				Assert.assertTrue(
						"Job pair had bench with bench id: " + pair.getBench().getId() + " but this benchmark was contained in benchmarks retrieved.",
						benchmarkIds.contains(pair.getBench().getId()));
			}
		} catch (SQLException e) {
			Assert.fail("Call threw SQL Exception.");
			log.error("Caught SQLException: " + e.getMessage(), e);
		}

	}

	@StarexecTest
	private void GetCountInSpaceTest() {
		Assert.assertEquals(benchmarks.size(),Benchmarks.getCountInSpace(space.getId()));
	}

	@StarexecTest
	private void GetCountInSpaceWithQuery() {
		Assert.assertEquals(benchmarks.size(), Benchmarks.getCountInSpace(space.getId(),""));
		String name=benchmarks.get(0).getName();
		Assert.assertEquals(1,Benchmarks.getCountInSpace(space.getId(),name));
	}

	@StarexecTest
	private void GetBenchmarkTest() {
		Benchmark b=Benchmarks.get(benchmarks.get(0).getId());
		Assert.assertNotNull(b);
		Assert.assertEquals(b.getName(),benchmarks.get(0).getName());

	}



	@StarexecTest
	private void GetAssociatedSpaceIds() {
		Assert.assertEquals(space.getId(),(int)Benchmarks.getAssociatedSpaceIds(benchmarks.get(0).getId()).get(0));
	}
	@StarexecTest
	private void associateSingleBenchmarkTest() {
		Assert.assertTrue(Benchmarks.associate(benchmarks.get(0).getId(), space2.getId()));
		boolean found=false;
		for (Benchmark b : Spaces.getDetails(space2.getId(), admin.getId()).getBenchmarks()) {
			found = found || b.getId()==benchmarks.get(0).getId();
		}
		Assert.assertTrue(found);
		List<Integer> temp = new ArrayList<>();
		temp.add(benchmarks.get(0).getId());
		Assert.assertTrue(Spaces.removeBenches(temp, space2.getId()));
	}

	@StarexecTest
	private void AssociateBenchmarksTest() {
		Space subspace=loader.loadSpaceIntoDatabase(user.getId(), space.getId());
		List<Integer> ids= new ArrayList<>();
		for (Benchmark b : benchmarks) {
			ids.add(b.getId());
		}
		Assert.assertTrue(Benchmarks.associate(ids, subspace.getId()));

		List<Benchmark> spaceBenchmarks=Spaces.getDetails(subspace.getId(),admin.getId()).getBenchmarks();
		Assert.assertEquals(spaceBenchmarks.size(),benchmarks.size());

		Spaces.removeSubspace(subspace.getId());
	}


	@StarexecTest
	private void GetContentsTest() {
		Benchmark b=benchmarks.get(0);
		try {
			Optional<String> str = Benchmarks.getContents(b.getId(), -1);
			Assert.assertTrue("Benchmark contents were not available.",str.isPresent());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void RecycleAndRestoreTest() {
		Benchmark b= benchmarks.get(0);
		String name=b.getName();
		Assert.assertFalse(Benchmarks.isBenchmarkRecycled(b.getId()));
		Assert.assertEquals(0,Benchmarks.getRecycledBenchmarkCountByUser(user.getId()));
		Assert.assertEquals(0,Benchmarks.getRecycledBenchmarkCountByUser(user.getId(),name));

		Assert.assertTrue(Benchmarks.recycle(b.getId()));
		Assert.assertTrue(Benchmarks.isBenchmarkRecycled(b.getId()));
		Assert.assertEquals(1,Benchmarks.getRecycledBenchmarkCountByUser(user.getId()));
		Assert.assertEquals(0,Benchmarks.getRecycledBenchmarkCountByUser(user.getId(),name+"a")); //now it shouldn't match the query

		Assert.assertTrue(Benchmarks.restore(b.getId()));
		Assert.assertFalse(Benchmarks.isBenchmarkRecycled(b.getId()));
		Assert.assertEquals(0,Benchmarks.getRecycledBenchmarkCountByUser(user.getId()));
		Assert.assertEquals(0,Benchmarks.getRecycledBenchmarkCountByUser(user.getId(),name));
	}

	@StarexecTest
	private void recycleAndRestoreAllTest() {
		for (Benchmark b : benchmarks) {
			Assert.assertFalse(Benchmarks.isBenchmarkRecycled(b.getId()));
		}

		Assert.assertTrue(Benchmarks.recycleAllOwnedByUser(benchmarks, user.getId()));
		for (Benchmark b : benchmarks) {
			Assert.assertTrue(Benchmarks.isBenchmarkRecycled(b.getId()));
		}
		Assert.assertTrue(Benchmarks.restoreRecycledBenchmarks(user.getId()));

		for (Benchmark b : benchmarks) {
			Assert.assertFalse(Benchmarks.isBenchmarkRecycled(b.getId()));
		}

	}

	@StarexecTest
	private void recycleAllByWrongUserTest() {
		Assert.assertTrue(Benchmarks.recycleAllOwnedByUser(benchmarks, user2.getId()));
		for (Benchmark b : benchmarks) {
			Assert.assertFalse(Benchmarks.isBenchmarkRecycled(b.getId()));
		}
	}

	@StarexecTest
	private void updateDetailsTest() {
		Benchmark b=benchmarks.get(0);
		String newName=TestUtil.getRandomSolverName();
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertTrue(Benchmarks.updateDetails(b.getId(), newName, newDesc, !b.isDownloadable(), b.getType().getId()));

		Benchmark newBench=Benchmarks.get(b.getId());

		Assert.assertEquals(newName,newBench.getName());
		Assert.assertEquals(newDesc,newBench.getDescription());
		Assert.assertNotEquals(b.isDownloadable(),newBench.isDownloadable());
		b.setName(newName);
		b.setDescription(newDesc);
		b.setDownloadable(!b.isDownloadable());

	}

	@StarexecTest
	private void deleteBenchTest() {
		List<Integer> ids=loader.loadBenchmarksIntoDatabase("benchmarks.zip", scratchSpace.getId(), user.getId());
		for (Integer id : ids) {
			Assert.assertNotNull(Benchmarks.get(id));
			Assert.assertTrue(Benchmarks.delete(id));
			Assert.assertNull(Benchmarks.get(id));
			Assert.assertNotNull(Benchmarks.getIncludeDeletedAndRecycled(id, false));
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(id));
		}
	}

	@StarexecTest
	private void setRecycledToDeletedTest() {
		List<Integer> ids=loader.loadBenchmarksIntoDatabase("benchmarks.zip", scratchSpace.getId(), user.getId());
		List<Integer> ids2=loader.loadBenchmarksIntoDatabase("benchmarks.zip", scratchSpace.getId(), user2.getId());

		for (Integer id : ids) {
			Assert.assertTrue(Benchmarks.recycle(id));
		}
		for (Integer id : ids2) {
			Assert.assertTrue(Benchmarks.recycle(id));
		}
		Assert.assertTrue(Benchmarks.setRecycledBenchmarksToDeleted(user.getId()));
		for (Integer id :ids) {
			Assert.assertTrue(Benchmarks.isBenchmarkDeleted(id));
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(id));
		}
		//make sure we aren't deleting non-recycled benchmarks
		for (Benchmark b : benchmarks) {
			Assert.assertFalse(Benchmarks.isBenchmarkDeleted(b.getId()));
		}

		//make sure we aren't deleting benchmarks from another user
		for (Integer id : ids2) {
			Assert.assertFalse(Benchmarks.isBenchmarkDeleted(id));
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(id));
		}
	}

	@StarexecTest
	private void addBenchmarkFromTextTest() throws Exception {
		Integer id = UploadBenchmark.addBenchmarkFromText("new benchmark", "test bench", user.getId(), Processors.getNoTypeProcessor().getId(), false);
		Benchmark b = Benchmarks.get(id);
		Assert.assertEquals("new benchmark", FileUtils.readFileToString(new File(b.getPath())));
		Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(id));
	}

	@StarexecTest
	private void addBenchmarkFromFileTest() throws Exception {

		File f = Util.getSandboxDirectory();
		File benchFile = new File(f, "benchmark.txt");
		FileUtils.writeStringToFile(benchFile, "new benchmark");
		int benchId = UploadBenchmark.addBenchmarkFromFile(benchFile, user.getId(), Processors.getNoTypeProcessor().getId(), false);
		Benchmark b = Benchmarks.get(benchId);
		Assert.assertEquals("new benchmark", FileUtils.readFileToString(new File(b.getPath())));
		Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(benchId));
	}

	@StarexecTest
	private void addBenchmarkDependencyTest() {
		int primaryBench = benchmarks.get(0).getId();
		int secondaryBench = benchmarks.get(1).getId();
		Assert.assertTrue(Benchmarks.addBenchDependency(primaryBench, secondaryBench, "test path"));
		List<BenchmarkDependency> deps = Benchmarks.getBenchDependencies(primaryBench);
		Assert.assertEquals(1, deps.size());
		Assert.assertEquals(deps.get(0).getDependencyPath(), "test path");
		Assert.assertEquals(deps.get(0).getSecondaryBench().getId(), secondaryBench);
		Assert.assertEquals(deps.get(0).getPrimaryBench().getId(), primaryBench);
	}

	@StarexecTest
	private void getBenchmarksForNextPageByUserTest() {
		// the results of the output here depends on the state of Stardev, as public benchmarks are returned by this
		// function as well. As such, this test is a bit weak, and only tests some basic characteristics of the return value
		int[] totals = new int[2];
		List<Benchmark> page = Benchmarks.getBenchmarksForNextPageByUser(new DataTablesQuery(0, 10, 0, true, ""), user.getId(), totals);
		//ensures the query did not filter anything
		Assert.assertEquals(totals[0], totals[1]);
		Assert.assertTrue(page.size()<=10);
		//ensures benchmarks are sorted ASC
		Assert.assertTrue(page.get(1).getName().compareTo(page.get(0).getName())>=0);
	}

	@StarexecTest
	private void getBenchmarksForNextPageByUserWithQueryTest() {
		int[] totals = new int[2];
		List<Benchmark> page = Benchmarks.getBenchmarksForNextPageByUser(new DataTablesQuery(0, 10, 0, false, benchmarks.get(0).getName()), user.getId(), totals);
		Assert.assertEquals(1, totals[1]);
		Assert.assertEquals(benchmarks.get(0).getName(),page.get(0).getName());
	}

	private void assertPageResultsEqualsBenchmarksArray(List<Benchmark> page) {
		List<Integer> pageIds = new ArrayList<>();
		for (Benchmark b : page) {
			pageIds.add(b.getId());
		}
		for (Benchmark b : benchmarks) {
			Assert.assertTrue(pageIds.contains(b.getId()));
		}
		//ensures benchmarks are sorted ASC
		Assert.assertTrue(page.get(1).getName().compareTo(page.get(0).getName())>0);
		Assert.assertEquals(benchmarks.size(),page.size());
	}

	// test getting pages of benchmarks a user owns
	@StarexecTest
	private void getBenchmarksByUserForNextPage() {
		List<Benchmark> page = Benchmarks.getBenchmarksByUserForNextPage(new DataTablesQuery(0, 10, 0, true, ""), user.getId(), false);
		assertPageResultsEqualsBenchmarksArray(page);
	}

	// test getting pages of benchmarks for a space
	@StarexecTest
	private void getBenchmarksBySpaceForNextPage() {
		List<Benchmark> page = Benchmarks.getBenchmarksForNextPage(new DataTablesQuery(0, 10, 0, true, ""), space.getId());
		assertPageResultsEqualsBenchmarksArray(page);
	}

	@StarexecTest
	private void getContentsLimitTest() {
		// output should have just one line
		try {
			Optional<String> contentsFromBench = Benchmarks.getContents(benchmarks.get(0), 1);
			Assert.assertTrue("Benchmark.getContents returned an empty optional.",contentsFromBench.isPresent());
			Assert.assertEquals(1, contentsFromBench.get().split("\r\n|\r|\n").length);
			Optional<String> contentsFromBenchId = Benchmarks.getContents(benchmarks.get(0).getId(), 1);
			Assert.assertTrue("Benchmark.getContents returned empty when called on bench id.", contentsFromBenchId.isPresent());
			Assert.assertEquals(1, contentsFromBenchId.get().split("\r\n|\r|\n").length);
		} catch(IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void cleanDeletedOrphanedBenchmarksTest() {
		List<Integer> benchIds = loader.loadBenchmarksIntoDatabase(space.getId(), user.getId());
		for (int id : benchIds) {
			Benchmarks.delete(id);
		}
		Assert.assertTrue(Benchmarks.cleanOrphanedDeletedBenchmarks());
		for (int id : benchIds) {
			Assert.assertNotNull(Benchmarks.getIncludeDeletedAndRecycled(id, false));
		}

		Spaces.removeBenches(benchIds, space.getId());

		Assert.assertTrue(Benchmarks.cleanOrphanedDeletedBenchmarks());
		for (int id : benchIds) {
			Assert.assertNull(Benchmarks.getIncludeDeletedAndRecycled(id, false));
		}

	}

	@StarexecTest
	private void copyBenchmarksTest() {
		List<Integer> newIds = Benchmarks.copyBenchmarks(benchmarks, user2.getId(), space2.getId());
		Assert.assertEquals(newIds.size(), benchmarks.size());
		int index=0;
		for (Benchmark b : benchmarks) {
			// ids should be new
			Assert.assertFalse(newIds.contains(b.getId()));
			Benchmark newBenchmark = Benchmarks.get(newIds.get(index));
			Assert.assertEquals(b.getName(), newBenchmark.getName());
			Assert.assertEquals(user2.getId(), newBenchmark.getUserId());
			index++;
		}

		// make sure space2 contains the new benchmarks
		for (Benchmark b : Spaces.getDetails(space2.getId(), user2.getId()).getBenchmarks()) {
			Assert.assertTrue(newIds.contains(b.getId()));
		}
		for (int id : newIds) {
			Benchmarks.deleteAndRemoveBenchmark(id);
		}
	}

	@StarexecTest
	private void copyBenchmarkTest() {
		Benchmark old= benchmarks.get(0);
		int newId = Benchmarks.copyBenchmark(old, user2.getId(), space2.getId());
		Assert.assertNotEquals(old, newId);
		Benchmark newB = Benchmarks.get(newId);
		Assert.assertEquals(old.getName(), newB.getName());
		Assert.assertEquals(old.getDescription(), newB.getDescription());
		Assert.assertEquals(old.getDiskSize(), newB.getDiskSize());
		Assert.assertEquals(user2.getId(), newB.getUserId());
		Assert.assertEquals(Spaces.getDetails(space2.getId(), user2.getId()).getBenchmarks().get(0).getId(), newId);
		Benchmarks.deleteAndRemoveBenchmark(newId);
	}

	@StarexecTest
	private void getAttributesTest() {
		Benchmark b = benchmarks.get(0);
		Map<String,String> attrs = Benchmarks.getAttributes(b.getId());
		Assert.assertEquals(b.getAttributes().size(), attrs.size());
		for (String s : b.getAttributes().keySet()) {
			Assert.assertEquals(b.getAttributes().get(s), attrs.get(s));
		}
	}

	@StarexecTest
	private void getBenchIdByNameTest() {
		Integer id = benchmarks.get(0).getId();
		Integer minusOne = -1;
		String name = benchmarks.get(0).getName();
		Assert.assertEquals(id, Benchmarks.getBenchIdByName(space.getId(), name));
		Assert.assertEquals(minusOne, Benchmarks.getBenchIdByName(space.getId(), TestUtil.getRandomAlphaString(DB.BENCH_NAME_LEN-2)));
		Assert.assertEquals(minusOne, Benchmarks.getBenchIdByName(-1, name));
	}

	@StarexecTest
	private void getBenchmarkCountByUserTest() {
		Assert.assertEquals(benchmarks.size(), Benchmarks.getBenchmarkCountByUser(user.getId()));
		Assert.assertEquals(0, Benchmarks.getBenchmarkCountByUser(user2.getId()));
	}

	@StarexecTest
	private void getBenchmarkCountByUserWithQueryTest() {
		Assert.assertEquals(benchmarks.size(), Benchmarks.getBenchmarkCountByUser(user.getId(), ""));
		Assert.assertEquals(1, Benchmarks.getBenchmarkCountByUser(user.getId(), benchmarks.get(0).getName()));
		Assert.assertEquals(0, Benchmarks.getBenchmarkCountByUser(user2.getId(), ""));
	}

	@StarexecTest
	private void getSortedAttribuesTest() {
		TreeMap<String,String> mapping = Benchmarks.getSortedAttributes(benchmarks.get(0).getId());
		Assert.assertEquals(benchmarks.get(0).getAttributes().size(), mapping.size());
		for (Object s : benchmarks.get(0).getAttributes().keySet()) {
			Assert.assertEquals(benchmarks.get(0).getAttributes().get(s), mapping.get(s));
		}
	}

	@StarexecTest
	private void benchmarkExistsTest() {
		Assert.assertTrue(Benchmarks.benchmarkExists(benchmarks.get(0).getId()));
	}

	@StarexecTest
	private void benchmarkDoesntExistsTest() {
		Assert.assertFalse(Benchmarks.benchmarkExists(-1));
	}

	@StarexecTest
	private void isPublicInPublicSpaceTest() {
		Space newSpace = loader.loadSpaceIntoDatabase(user.getId(), space.getId());
		Spaces.setPublicSpace(newSpace.getId(), user.getId(), true, false);
		int id = benchmarks.get(0).getId();
		Benchmarks.associate(id, newSpace.getId());
		Assert.assertTrue(Benchmarks.isPublic(id));
		Spaces.removeSubspace(newSpace.getId());
	}

	@StarexecTest
	private void isNotPublicTest() {
		int id = benchmarks.get(0).getId();
		Assert.assertFalse(Benchmarks.isPublic(id));
	}

	@StarexecTest
	private void clearAttributesTest() {
		Assert.assertTrue(Benchmarks.clearAttributes(benchmarks.get(0).getId()));
		Assert.assertEquals(0, Benchmarks.getAttributes(benchmarks.get(0).getId()).size());
		for (Object o : benchmarks.get(0).getAttributes().keySet()) {
			Benchmarks.addBenchAttr(benchmarks.get(0).getId(), (String)o, benchmarks.get(0).getAttributes().get(o));
		}
	}

	@StarexecTest
	private void processTest() throws Exception {
		User tempUser = loader.loadUserIntoDatabase();
		Space tempSpace = loader.loadSpaceIntoDatabase(tempUser.getId(), 1);

		List<Integer> benchIds = Benchmarks.copyBenchmarks(benchmarks, tempUser.getId(), tempSpace.getId());
		Integer statusId = Benchmarks.process(tempSpace.getId(), benchProcessor, false, tempUser.getId(), true);
		Assert.assertTrue(statusId>0);
		int MAX_LOOPS = 50;
		while (!Uploads.getBenchmarkStatus(statusId).isEverythingComplete()) {
			Thread.sleep(1000);
			MAX_LOOPS--;
			Assert.assertTrue(MAX_LOOPS>=0);
		}
		for (int benchId : benchIds) {
			Map<String,String> attrs = Benchmarks.getAttributes(benchId);
			addMessage(benchId+"");
			Assert.assertEquals(1, attrs.size());
			Assert.assertEquals("test", attrs.get("test-attribute"));
			Benchmarks.deleteAndRemoveBenchmark(benchId);
		}
	}

	@StarexecTest
	private void getNoOrphansTest() {
		Assert.assertEquals(0, Benchmarks.getOrphanedBenchmarks(user.getId()).size());
	}

	@StarexecTest
	private void recycleNoOrphansTest() {
		Assert.assertTrue(Benchmarks.recycleOrphanedBenchmarks(user.getId()));
		for (Benchmark b : benchmarks) {
			Assert.assertFalse(Benchmarks.isBenchmarkRecycled(b.getId()));
		}
	}

	@StarexecTest
	private void getOrphansTest() {
		List<Integer> benchIds = new ArrayList<>();
		for (Benchmark b : benchmarks) {
			benchIds.add(b.getId());
		}
		Assert.assertTrue(Spaces.removeBenches(benchIds, space.getId()));
		Assert.assertEquals(3, Benchmarks.getOrphanedBenchmarks(user.getId()).size());
		Assert.assertTrue(Benchmarks.associate(benchIds, space.getId()));
	}

	@StarexecTest
	private void recycleOrphansTest() {
		List<Integer> newBenchmarks = loader.loadBenchmarksIntoDatabase(space.getId(), user.getId());
		Spaces.removeBenches(newBenchmarks, space.getId());
		Assert.assertTrue(Benchmarks.recycleOrphanedBenchmarks(user.getId()));
		for (Integer i : newBenchmarks) {
			Assert.assertTrue(Benchmarks.isBenchmarkRecycled(i));
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
	}

	@StarexecTest
	private void getBenchmarksInSharedSpacesTest() {
		Assert.assertEquals(benchmarks.size(), Benchmarks.getBenchmarksInSharedSpaces(user.getId()).size());
	}


	@Override
	protected void setup() throws Exception {
		user=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space=loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		space2=loader.loadSpaceIntoDatabase(user2.getId(), Communities.getTestCommunity().getId());
		scratchSpace = loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());

		List<Integer> ids = loader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user.getId());
		benchmarks=Benchmarks.get(ids,true);
		benchProcessor = loader.loadBenchProcessorIntoDatabase(Communities.getTestCommunity().getId());
		List<Integer> solverIds= new ArrayList<>();
		solver = loader.loadSolverIntoDatabase(space.getId(), user.getId());
		solverIds.add(solver.getId());
		int wallclockTimeout=100;
		postProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		job=loader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, ids,cpuTimeout,wallclockTimeout,gbMemory);
	}


	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

	private boolean containsBenchmark(List<Benchmark> benches, Benchmark b) {
		for (Benchmark bench : benches) {
			if (bench.getName().equals(b.getName()) && bench.getId()==b.getId()) {
				return true;
			}
		}
		return false;
	}

}
