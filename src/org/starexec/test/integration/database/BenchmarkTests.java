package org.starexec.test.integration.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.Util;

/**
 * Tests for org.starexec.data.database.Benchmarks.java
 * @author Eric
 */
public class BenchmarkTests extends TestSequence {
	private static final Logger log = Logger.getLogger(BenchmarkTests.class);	
	private User user=null;
	private User user2=null;
	private User admin=null;
	private Space space=null;
	private Space space2 = null;
	private List<Benchmark> benchmarks=null; //owned by user. Should be the only benchmarks in space
	Processor benchProcessor = null;
	
	
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
			log.debug(b.getId());
			Assert.assertTrue(containsBenchmark(benchmarks,b));
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
		List<Integer> temp = new ArrayList<Integer>();
		temp.add(benchmarks.get(0).getId());
		Assert.assertTrue(Spaces.removeBenches(temp, space2.getId()));
	}
	
	@StarexecTest
	private void AssociateBenchmarksTest() {
		Space subspace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), space.getId());
		List<Integer> ids= new ArrayList<Integer>();
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
		String str=Benchmarks.getContents(b.getId(), -1);
		Assert.assertNotNull(str);
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
		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user.getId());
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
		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user.getId());
		List<Integer> ids2=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user2.getId());

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
		User newUser = ResourceLoader.loadUserIntoDatabase();
		Integer id = BenchmarkUploader.addBenchmarkFromText("new benchmark", "test bench", newUser.getId(), Processors.getNoTypeProcessor().getId(), false);
		Benchmark b = Benchmarks.get(id);
		Assert.assertEquals("new benchmark", FileUtils.readFileToString(new File(b.getPath())));
		Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(id));
		Users.deleteUser(newUser.getId(), admin.getId());
	}
	
	@StarexecTest
	private void addBenchmarkFromFileTest() throws Exception {
		User newUser = ResourceLoader.loadUserIntoDatabase();

		File f = Util.getSandboxDirectory();
		File benchFile = new File(f, "benchmark.txt");
		FileUtils.writeStringToFile(benchFile, "new benchmark");
		int benchId = BenchmarkUploader.addBenchmarkFromFile(benchFile, newUser.getId(), Processors.getNoTypeProcessor().getId(), false);
		Benchmark b = Benchmarks.get(benchId);
		Assert.assertEquals("new benchmark", FileUtils.readFileToString(new File(b.getPath())));
		Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(benchId));
		Users.deleteUser(newUser.getId(), admin.getId());
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
		//ensures benchmarks are sorted ASC
		Assert.assertTrue(page.get(1).getName().compareTo(page.get(0).getName())>0);
		
		Assert.assertTrue(page.size()>=benchmarks.size());
		Assert.assertTrue(page.size()<=10);
	}
	
	@StarexecTest
	private void getBenchmarksForNextPageByUserWithQueryTest() {
		int[] totals = new int[2];
		List<Benchmark> page = Benchmarks.getBenchmarksForNextPageByUser(new DataTablesQuery(0, 10, 0, false, benchmarks.get(0).getName()), user.getId(), totals);
		Assert.assertEquals(1, totals[1]);
		Assert.assertEquals(benchmarks.get(0).getName(),page.get(0).getName());
	}
	
	private void assertPageResultsEqualsBenchmarksArray(List<Benchmark> page) {
		List<Integer> pageIds = new ArrayList<Integer>();
		for (Benchmark b : page) {
			pageIds.add(b.getId());
		}
		for (Benchmark b : benchmarks) {
			Assert.assertTrue(pageIds.contains(b.getId()));
		}
		//ensures benchmarks are sorted ASC
		Assert.assertTrue(page.get(1).getName().compareTo(page.get(0).getName())>0);
		Assert.assertTrue(page.size()==benchmarks.size());
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
		Assert.assertEquals(1, Benchmarks.getContents(benchmarks.get(0), 1).split("\r\n|\r|\n").length);
		Assert.assertEquals(1, Benchmarks.getContents(benchmarks.get(0).getId(), 1).split("\r\n|\r|\n").length);
	}

	@StarexecTest
	private void cleanDeletedOrphanedBenchmarksTest() {
		List<Integer> benchIds = ResourceLoader.loadBenchmarksIntoDatabase(space.getId(), user.getId());
		Spaces.removeBenches(benchIds, space.getId());
		for (int id : benchIds) {
			Benchmarks.delete(id);
		}
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
		Assert.assertEquals(minusOne, Benchmarks.getBenchIdByName(space.getId(), TestUtil.getRandomAlphaString(R.BENCH_NAME_LEN-2)));
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
		Space newSpace = ResourceLoader.loadSpaceIntoDatabase(user.getId(), space.getId());
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
			Benchmarks.addBenchAttr(benchmarks.get(0).getId(),(String)o, (String) benchmarks.get(0).getAttributes().get(o));
		}
	}
	
	@StarexecTest
	private void processTest() throws Exception {
		User tempUser = ResourceLoader.loadUserIntoDatabase();
		Space tempSpace = ResourceLoader.loadSpaceIntoDatabase(tempUser.getId(), 1);
		
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
		Spaces.removeSubspace(tempSpace.getId());
		Users.deleteUser(tempUser.getId(),admin.getId());
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
		List<Integer> benchIds = new ArrayList<Integer>();
		for (Benchmark b : benchmarks) {
			benchIds.add(b.getId());
		}
		Assert.assertTrue(Spaces.removeBenches(benchIds, space.getId()));
		Assert.assertEquals(3, Benchmarks.getOrphanedBenchmarks(user.getId()).size());
		Assert.assertTrue(Benchmarks.associate(benchIds, space.getId()));
	}
	
	@StarexecTest
	private void recycleOrphansTest() {
		List<Integer> newBenchmarks = ResourceLoader.loadBenchmarksIntoDatabase(space.getId(), user.getId());
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
		user=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(user2.getId(), Communities.getTestCommunity().getId());
		List<Integer> ids=new ArrayList<Integer>();
		ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user.getId());
		benchmarks=Benchmarks.get(ids,true);
		benchProcessor = ResourceLoader.loadBenchProcessorIntoDatabase(Communities.getTestCommunity().getId());
	}
	

	@Override
	protected void teardown() throws Exception {
		for (Benchmark b : benchmarks) { 
			Benchmarks.deleteAndRemoveBenchmark(b.getId());
		}
		Processors.delete(benchProcessor.getId());
		Spaces.removeSubspace(space2.getId());
		Spaces.removeSubspace(space.getId());
		Users.deleteUser(user.getId(), admin.getId());
		Users.deleteUser(user2.getId(), admin.getId());
		
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
