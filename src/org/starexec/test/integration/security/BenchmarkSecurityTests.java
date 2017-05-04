package org.starexec.test.integration.security;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkSecurityTests extends TestSequence {
	User user1=null;
	User admin=null;
	User user2=null;
	User user3=null;
	
	Space space=null;    //user2 is the owner and only member of these two spaces
	Space space2=null;
	List<Integer> benchmarkIds=null; //these are benchmarks owned by user1 and placed in space
	List<Integer> benchmarkIds2=null; //these are benchmarks owned by user2 and placed in space2
	BenchmarkUploadStatus benchmarkStatus = null;

	@StarexecTest
	private void CanDeleteBenchTest() {
		Assert.assertEquals(true,BenchmarkSecurity.canUserDeleteBench(benchmarkIds.get(0), user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserDeleteBench(benchmarkIds.get(0), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserDeleteBench(benchmarkIds.get(0), user2.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanRecyleBenchTest() {
		Assert.assertEquals(true,BenchmarkSecurity.canUserRecycleBench(benchmarkIds.get(0), user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserRecycleBench(benchmarkIds.get(0), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRecycleBench(benchmarkIds.get(0), user2.getId()).isSuccess());
	}
	
	@StarexecTest
	private void CanRestoreBenchTest() {
		Benchmark b=Benchmarks.get(benchmarkIds.get(0));
		//benchmarks can be restored only if they were actually recycled to begin with
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user1.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user2.getId()).isSuccess());
		
		Assert.assertTrue(Benchmarks.recycle(b.getId()));
		
		Assert.assertEquals(true,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmark(b.getId(), user2.getId()).isSuccess());
		
		Assert.assertTrue(Benchmarks.restore(b.getId()));
	}
	
	@StarexecTest
	private void CanDeleteBenchesTest() {
		Assert.assertEquals(true,BenchmarkSecurity.canUserDeleteBenchmarks(benchmarkIds, user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserDeleteBenchmarks(benchmarkIds, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserDeleteBenchmarks(benchmarkIds, user2.getId()).isSuccess());
		List<Integer> temp= new ArrayList<>();
		temp.addAll(benchmarkIds);
		temp.addAll(benchmarkIds2);
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserDeleteBenchmarks(temp, user1.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserDeleteBenchmarks(temp, user2.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserDeleteBenchmarks(temp, admin.getId()).isSuccess());
	}
	
	@StarexecTest
	private void canRecycleBenchesTest() {
		Assert.assertEquals(true,BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, user2.getId()).isSuccess());
		List<Integer> temp= new ArrayList<>();
		temp.addAll(benchmarkIds);
		temp.addAll(benchmarkIds2);
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRecycleBenchmarks(temp, user1.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRecycleBenchmarks(temp, user2.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserRecycleBenchmarks(temp, admin.getId()).isSuccess());
	}
	
	@StarexecTest
	private void canRestoreBenchesTest() {
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user1.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user2.getId()).isSuccess());
		
		for (Integer i : benchmarkIds) {
			Assert.assertTrue(Benchmarks.recycle(i));
		}
		
		for (Integer i : benchmarkIds2) {
			Assert.assertTrue(Benchmarks.recycle(i));
		}
		
		Assert.assertEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(benchmarkIds, user2.getId()).isSuccess());
		
		
		List<Integer> temp= new ArrayList<>();
		temp.addAll(benchmarkIds);
		temp.addAll(benchmarkIds2);
		//because there are benchmarks by both owners in temp, neither owner should be able to restore them.
		//only the admin should be able to
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(temp, user1.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(temp, user2.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserRestoreBenchmarks(temp, admin.getId()).isSuccess());
		
		
		for (Integer i : temp) {
			Assert.assertTrue(Benchmarks.restore(i));
		}
	}
	
	
	@StarexecTest
	private void canEditBenchmarkTest() {
		Benchmark b1=Benchmarks.get(benchmarkIds.get(0));
		Benchmark b2=Benchmarks.get(benchmarkIds.get(1));
		ValidatorStatusCode code = BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b1.getName(),b1.getDescription(),b1.getType().getId(), user1.getId());
		Assert.assertEquals(true,code.isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b1.getName(),b1.getDescription(),b1.getType().getId(), admin.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserEditBenchmark(b1.getId(), b1.getName(),b2.getDescription(),b1.getType().getId(), user2.getId()).isSuccess());
	}
	
	@StarexecTest
	private void canViewBenchmarkContentsTest() {
		Benchmark b=Benchmarks.get(benchmarkIds.get(0));
		//first, do the test with "downloadable" set to false
		Benchmarks.updateDetails(b.getId(), b.getName(), b.getDescription(), false, b.getType().getId());
		Assert.assertEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), admin.getId()).isSuccess());
		
		//user2 is in the same space, but they still are not allowed to see the contents
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user2.getId()).isSuccess());
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user3.getId()).isSuccess());

		//then, again with downloadable set to true
		Benchmarks.updateDetails(b.getId(), b.getName(), b.getDescription(), true, b.getType().getId());
		Assert.assertEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user1.getId()).isSuccess());
		Assert.assertEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), admin.getId()).isSuccess());
		//now user2 can see the contents because they are in the same space
		Assert.assertEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user2.getId()).isSuccess());
		
		
		Assert.assertNotEquals(true,BenchmarkSecurity.canUserSeeBenchmarkContents(b.getId(), user3.getId()).isSuccess());
	}
	
	@StarexecTest
	private void canSeeBenchmarkUploadStatusTest() {
		Assert.assertTrue(BenchmarkSecurity.canUserSeeBenchmarkStatus(benchmarkStatus.getId(), user1.getId()));
		Assert.assertTrue(BenchmarkSecurity.canUserSeeBenchmarkStatus(benchmarkStatus.getId(), admin.getId()));
		Assert.assertFalse(BenchmarkSecurity.canUserSeeBenchmarkStatus(benchmarkStatus.getId(), user2.getId()));
	}
	
	
	@Override
	protected String getTestName() {
		return "BenchmarkSecurityTests";
	}

	@Override
	protected void setup() throws Exception {
		user1=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();
		user3=loader.loadUserIntoDatabase();
		
		Users.associate(user2.getId(), Communities.getTestCommunity().getId());
		
		space=loader.loadSpaceIntoDatabase(user2.getId(),Communities.getTestCommunity().getId());
		space2=loader.loadSpaceIntoDatabase(user2.getId(), Communities.getTestCommunity().getId());
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		benchmarkIds=loader.loadBenchmarksIntoDatabase("benchmarks.zip", space.getId(), user1.getId());
		benchmarkIds2=loader.loadBenchmarksIntoDatabase("benchmarks.zip", space2.getId(), user2.getId());
		Assert.assertNotNull(benchmarkIds);	
		Assert.assertNotNull(benchmarkIds2);
		benchmarkStatus = Uploads.getBenchmarkStatus(Uploads.createBenchmarkUploadStatus(space.getId(), user1.getId()));

	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
