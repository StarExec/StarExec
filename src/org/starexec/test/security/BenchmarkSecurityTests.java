package org.starexec.test.security;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Users;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;

public class BenchmarkSecurityTests extends TestSequence {
	User user1=null;
	User admin=null;
	User user2=null;
	User user3=null;
	List<Integer> benchmarkIds=null;
	List<Integer> benchmarkIds2=null;
	
	@Test
	private void CanDeleteBenchTest() {
		Assert.assertEquals(0,BenchmarkSecurity.canUserDeleteBench(benchmarkIds.get(0), user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserDeleteBench(benchmarkIds.get(0), admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserDeleteBench(benchmarkIds.get(0), user2.getId()));
	}
	
	@Test
	private void CanRecyleBenchTest() {
		Assert.assertEquals(0,BenchmarkSecurity.canUserRecycleBench(benchmarkIds.get(0), user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserRecycleBench(benchmarkIds.get(0), admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRecycleBench(benchmarkIds.get(0), user2.getId()));
	}
	
	@Test
	private void CanRestoreBenchTest() {
		Benchmark b=Benchmarks.get(benchmarkIds.get(0));
		//benchmarks can be restored only if they were actually recycled to begin with
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user1.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user2.getId()));
		
		Assert.assertTrue(Benchmarks.recycle(b.getId()));
		
		Assert.assertEquals(0,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user2.getId()));
		
		Assert.assertTrue(Benchmarks.restore(b.getId()));
	}
	
	@Test
	private void CanDeleteBenchesTest() {
		Assert.assertEquals(0,BenchmarkSecurity.canUserDeleteBenchmarks(benchmarkIds, user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserDeleteBenchmarks(benchmarkIds, admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserDeleteBenchmarks(benchmarkIds, user2.getId()));
		List<Integer> temp=new ArrayList<Integer>();
		temp.addAll(benchmarkIds);
		temp.addAll(benchmarkIds2);
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserDeleteBenchmarks(temp, user1.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserDeleteBenchmarks(temp, user2.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserDeleteBenchmarks(temp, admin.getId()));
	}
	
	@Test
	private void canRecycleBenchesTest() {
		Assert.assertEquals(0,BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, user2.getId()));
		List<Integer> temp=new ArrayList<Integer>();
		temp.addAll(benchmarkIds);
		temp.addAll(benchmarkIds2);
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRecycleBenchmarks(temp, user1.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRecycleBenchmarks(temp, user2.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserRecycleBenchmarks(temp, admin.getId()));
	}
	
	@Test
	private void canRestoreBenchesTest() {
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user1.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user2.getId()));
		
		for (Integer i : benchmarkIds) {
			Assert.assertTrue(Benchmarks.recycle(i));
		}
		
		for (Integer i : benchmarkIds2) {
			Assert.assertTrue(Benchmarks.recycle(i));
		}
		
		Assert.assertEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user2.getId()));
		
		
		List<Integer> temp=new ArrayList<Integer>();
		temp.addAll(benchmarkIds);
		temp.addAll(benchmarkIds2);
		//because there are benchmarks by both owners in temp, neither owner should be able to restore them.
		//only the admin should be able to
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(temp, user1.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(temp, user2.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserRestoreBenchmarks(temp, admin.getId()));
		
		
		for (Integer i : temp) {
			Assert.assertTrue(Benchmarks.restore(i));
		}
	}
	
	
	@Test
	private void canEditBenchmarkTest() {
		Benchmark b1=Benchmarks.get(benchmarkIds.get(0));
		Benchmark b2=Benchmarks.get(benchmarkIds.get(1));
		Assert.assertEquals(0,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b1.getName(), user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b1.getName(), admin.getId()));
		
		//we can't change the name to the same name as b2 because the names cannot be the same
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b2.getName(), user1.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b2.getName(), admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b1.getName(), user2.getId()));
	}
	
	@Test
	private void canViewBenchmarkTest() {
		Benchmark b=Benchmarks.get(benchmarkIds.get(0));
		//first, do the test with "downloadable" set to false
		Benchmarks.updateDetails(b.getId(), b.getName(), b.getDescription(), false, b.getType().getId());
		Assert.assertEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), admin.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user2.getId()));
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user3.getId()));

		//then, again with downloadable set to true
		Benchmarks.updateDetails(b.getId(), b.getName(), b.getDescription(), true, b.getType().getId());
		Assert.assertEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user1.getId()));
		Assert.assertEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), admin.getId()));
		//now user2 can see the contents because they are in the same space
		Assert.assertEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user2.getId()));
		
		
		Assert.assertNotEquals(0,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user3.getId()));

	}
	
	
	@Override
	protected String getTestName() {
		return "BenchmarkSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		user3=ResourceLoader.loadUserIntoDatabase();
		
		Users.associate(user2.getId(), Communities.getTestCommunity().getId());
		
		admin=Users.getAdmins().get(0);
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", Communities.getTestCommunity().getId(), user1.getId());
		benchmarkIds2=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", Communities.getTestCommunity().getId(), user2.getId());
		Assert.assertNotNull(benchmarkIds);	
		Assert.assertNotNull(benchmarkIds2);
	}

	@Override
	protected void teardown() throws Exception {
		Users.deleteUser(user1.getId());
		Users.deleteUser(user2.getId());
		Users.deleteUser(user3.getId());
	}

}
