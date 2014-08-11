package org.starexec.test.database;

import java.util.ArrayList;
import java.util.List;

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
import org.starexec.test.resources.ResourceLoader;

public class BenchmarkTests extends TestSequence {
	private User user=null;
	private User admin=null;
	private Space space=null;
	private List<Benchmark> benchmarks=null;
	
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
	private void GetAssociatedSpaceIds() {
		Assert.assertEquals(space.getId(),(int)Benchmarks.getAssociatedSpaceIds(benchmarks.get(0).getId()).get(0));
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
	private void RecycleAndRestoreAll() {
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
	
	@Override
	protected String getTestName() {
		return "BenchmarkTests";
	}

	@Override
	protected void setup() throws Exception {
		user=ResourceLoader.loadUserIntoDatabase();
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
		Spaces.removeSubspaces(space.getId(),Users.getAdmins().get(0).getId());
		Users.deleteUser(user.getId(), admin.getId());
		
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
