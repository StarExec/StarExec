package org.starexec.test.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;

public class BenchmarkTests extends TestSequence {
	private static final Logger log = Logger.getLogger(BenchmarkTests.class);	
	private User user=null;
	private User user2=null;
	private User admin=null;
	private Space space=null;
	private List<Benchmark> benchmarks=null; //owned by user
	
	@Test
	private void GetByUser() {
		List<Benchmark> benches=Benchmarks.getByOwner(user.getId());
		Assert.assertNotNull(benches);
		Assert.assertEquals(benchmarks.size(),benches.size());
		for (Benchmark b : benches) {
			Assert.assertTrue(containsBenchmark(benchmarks,b));
		}
		
		
		
	}
	
	@Test
	private void GetBySpace() {
		List<Benchmark> benches=Benchmarks.getBySpace(space.getId());
		Assert.assertNotNull(benches);
		Assert.assertEquals(benchmarks.size(),benches.size());
		for (Benchmark b : benches) {
			log.debug(b.getId());
			Assert.assertTrue(containsBenchmark(benchmarks,b));
		}
	}
	
	@Test
	private void GetCountInSpaceTest() {
		Assert.assertEquals(benchmarks.size(),Benchmarks.getCountInSpace(space.getId()));
	}
	
	@Test
	private void GetCountInSpaceWithQuery() {
		Assert.assertEquals(benchmarks.size(), Benchmarks.getCountInSpace(space.getId(),""));
		String name=benchmarks.get(0).getName();
		Assert.assertEquals(1,Benchmarks.getCountInSpace(space.getId(),name));
	}
	
	@Test
	private void GetBenchmarkTest() {
		Benchmark b=Benchmarks.get(benchmarks.get(0).getId());
		Assert.assertNotNull(b);
		Assert.assertEquals(b.getName(),benchmarks.get(0).getName());
		
	}
	
	
	
	@Test
	private void GetAssociatedSpaceIds() {
		Assert.assertEquals(space.getId(),(int)Benchmarks.getAssociatedSpaceIds(benchmarks.get(0).getId()).get(0));
	}
	
	@Test
	private void AssociateBenchmarksTest() {
		Space subspace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), space.getId());
		List<Integer> ids= new ArrayList<Integer>();
		for (Benchmark b : benchmarks) {
			ids.add(b.getId());
		}
		Assert.assertTrue(Benchmarks.associate(ids, subspace.getId()));
		
		List<Benchmark> spaceBenchmarks=Spaces.getDetails(subspace.getId(),admin.getId()).getBenchmarks();
		Assert.assertEquals(spaceBenchmarks.size(),benchmarks.size());
		
		Spaces.removeSubspaces(subspace.getId());
	}
	
	
	@Test 
	private void GetContentsTest() {
		Benchmark b=benchmarks.get(0);
		String str=Benchmarks.getContents(b.getId(), -1);
		Assert.assertNotNull(str);
	}
	
	@Test
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
	
	@Test
	private void RecycleAndRestoreAllTest() {
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
	
	@Test
	private void updateDetailsTest() {
		Benchmark b=benchmarks.get(0);
		String newName=TestUtil.getRandomSolverName();
		String newDesc=TestUtil.getRandomSolverName();
		Assert.assertTrue(Benchmarks.updateDetails(b.getId(), newName, newDesc, !b.isDownloadable(), b.getType().getId()));
		
		Benchmark newBench=Benchmarks.get(b.getId());
		
		Assert.assertEquals(newName,newBench.getName());
		Assert.assertEquals(newDesc,newBench.getDescription());
		Assert.assertNotEquals(b.isDownloadable(),newBench.isDownloadable());
		
	}
	
	@Test
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
	
	@Test
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
	
	@Override
	protected String getTestName() {
		return "BenchmarkTests";
	}

	@Override
	protected void setup() throws Exception {
		user=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		List<Integer> ids=new ArrayList<Integer>();
		ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user.getId());
		benchmarks=new ArrayList<Benchmark>();
		for (Integer id : ids) {
			benchmarks.add(Benchmarks.get(id));
		}		
	}

	@Override
	protected void teardown() throws Exception {
		for (Benchmark b : benchmarks) { 
			Benchmarks.deleteAndRemoveBenchmark(b.getId());
		}
		Spaces.removeSubspaces(space.getId());
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
