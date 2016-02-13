package org.starexec.test.integration.database;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.*;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;

/**
 * Tests for org.starexec.data.database.Uploads.java
 * @author Eric
 */
public class UploadTests extends TestSequence {
	private User u=null;
	private Space s=null;
	private BenchmarkUploadStatus bs=null;
	private SpaceXMLUploadStatus ss=null;
	private Random rand=new Random();
	@Override
	protected String getTestName() {
		return "UploadTests";
	}
	

	
	@StarexecTest
	private void getBenchmarkUploadStatusSummaryTest() {
		Assert.assertNotNull(Uploads.getUploadStatusSummary(bs.getId()));
	}
	@StarexecTest
	private void failedBenchesTest() {
		String benchName=TestUtil.getRandomAlphaString(50);
		Assert.assertNotNull(Uploads.getFailedBenches(bs.getId()));
		Assert.assertTrue(Uploads.addFailedBenchmark(bs.getId(), benchName, "invalid"));
		List<Benchmark> failed=Uploads.getFailedBenches(bs.getId());
		boolean found=false;
		for (Benchmark s : failed) {
			if (s.getName().equals(benchName)) {
				found=true;
				break;
			}
		}
		Assert.assertTrue(found);
	}
	
	@StarexecTest
	private void xmlIncrementCompletedBenchmarkTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementXMLCompletedBenchmarks(ss.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(ss.getCompletedBenchmarks()+totalIncrement, Uploads.getSpaceXMLStatus(ss.getId()).getCompletedBenchmarks());
		}
		ss=Uploads.getSpaceXMLStatus(ss.getId());
	}
	
	
	
	@StarexecTest
	private void xmlIncrementCompletedSolverTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementXMLCompletedSolvers(ss.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(ss.getCompletedSolvers()+totalIncrement, Uploads.getSpaceXMLStatus(ss.getId()).getCompletedSolvers());
		}
		ss=Uploads.getSpaceXMLStatus(ss.getId());

	}
	
	@StarexecTest
	private void xmlIncrementCompletedUpdateTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementXMLCompletedUpdates(ss.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(ss.getCompletedUpdates()+totalIncrement, Uploads.getSpaceXMLStatus(ss.getId()).getCompletedUpdates());
		}
		ss=Uploads.getSpaceXMLStatus(ss.getId());

	}
	
	@StarexecTest
	private void xmlIncrementCompletedSpaceTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementXMLCompletedSpaces(ss.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(ss.getCompletedSpaces()+totalIncrement, Uploads.getSpaceXMLStatus(ss.getId()).getCompletedSpaces());
		}
		ss=Uploads.getSpaceXMLStatus(ss.getId());

	}
	
	@StarexecTest
	private void benchmarkIncrementCompletedBenchmarkTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementCompletedBenchmarks(bs.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(bs.getCompletedBenchmarks()+totalIncrement, Uploads.getBenchmarkStatus(bs.getId()).getCompletedBenchmarks());
		}
		bs=Uploads.getBenchmarkStatus(bs.getId());
		
	}
	
	@StarexecTest
	private void benchmarkIncrementCompletedSpacesTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementCompletedSpaces(bs.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(bs.getCompletedSpaces()+totalIncrement, Uploads.getBenchmarkStatus(bs.getId()).getCompletedSpaces());
		}
		bs=Uploads.getBenchmarkStatus(bs.getId());
	}
	
	@StarexecTest
	private void benchmarkIncrementFailedBenchmarkTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementFailedBenchmarks(bs.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(bs.getFailedBenchmarks()+totalIncrement, Uploads.getBenchmarkStatus(bs.getId()).getFailedBenchmarks());
		}
		bs=Uploads.getBenchmarkStatus(bs.getId());
	}
	
	@StarexecTest
	private void benchmarkIncrementValidatedBenchmarkTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementValidatedBenchmarks(bs.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(bs.getValidatedBenchmarks()+totalIncrement, Uploads.getBenchmarkStatus(bs.getId()).getValidatedBenchmarks());
		}
		bs=Uploads.getBenchmarkStatus(bs.getId());
		
	}
	
	@StarexecTest
	private void benchmarkIncrementTotalBenchmarkTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementTotalBenchmarks(bs.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(bs.getTotalBenchmarks()+totalIncrement, Uploads.getBenchmarkStatus(bs.getId()).getTotalBenchmarks());
		}
		bs=Uploads.getBenchmarkStatus(bs.getId());
		
	}
	
	@StarexecTest
	private void benchmarkIncrementTotalSpacesTest() {
		int totalIncrement=0;
		for (int x=0;x<40;x++) {
			int next=rand.nextInt(20);
			Assert.assertTrue(Uploads.incrementTotalSpaces(bs.getId(), next));
			totalIncrement+=next;
			Assert.assertEquals(bs.getTotalSpaces()+totalIncrement, Uploads.getBenchmarkStatus(bs.getId()).getTotalSpaces());
		}
		bs=Uploads.getBenchmarkStatus(bs.getId());
		
	}
	
	@StarexecTest
	private void setXMLTotalUpdatesTest() {
		int num=rand.nextInt(20)+5;
		Assert.assertTrue(Uploads.setXMLTotalUpdates(ss.getId(), num));
		Assert.assertEquals(num,Uploads.getSpaceXMLStatus(ss.getId()).getTotalUpdates());
		ss.setTotalUpdates(num);
	}
	@StarexecTest
	private void setXMLTotalSolversTest() {
		int num=rand.nextInt(20)+5;
		Assert.assertTrue(Uploads.setXMLTotalSolvers(ss.getId(), num));
		Assert.assertEquals(num,Uploads.getSpaceXMLStatus(ss.getId()).getTotalSolvers());
		ss.setTotalSolvers(num);
	}
	
	@StarexecTest
	private void setXMLTotalBenchmarksTest() {
		int num=rand.nextInt(20)+5;
		Assert.assertTrue(Uploads.setXMLTotalBenchmarks(ss.getId(), num));
		Assert.assertEquals(num,Uploads.getSpaceXMLStatus(ss.getId()).getTotalBenchmarks());
		ss.setTotalBenchmarks(num);
	}
	
	@StarexecTest
	private void setXMLTotalSpacesTest() {
		int num=rand.nextInt(20)+5;
		Assert.assertTrue(Uploads.setXMLTotalSpaces(ss.getId(), num));
		Assert.assertEquals(num,Uploads.getSpaceXMLStatus(ss.getId()).getTotalSpaces());
		ss.setTotalSpaces(num);
	}
	
	@StarexecTest
	private void setXMLErrorMessageTest() {
		String msg=TestUtil.getRandomAlphaString(R.MSG_LEN);
		Assert.assertTrue(Uploads.setXMLErrorMessage(ss.getId(), msg));
		Assert.assertEquals(msg,Uploads.getSpaceXMLStatus(ss.getId()).getErrorMessage());
		ss.setErrorMessage(msg);
	}
	@StarexecTest
	private void setBenchmarkErrorMessageTest() {
		String msg=TestUtil.getRandomAlphaString(R.MSG_LEN);
		Assert.assertTrue(Uploads.setBenchmarkErrorMessage(bs.getId(), msg));
		
		Assert.assertEquals(msg,Uploads.getBenchmarkStatus(bs.getId()).getErrorMessage());
		bs.setErrorMessage(msg);
	}
	
	@StarexecTest
	private void xmlEverythingCompleteTest() {
		Assert.assertFalse(Uploads.getSpaceXMLStatus(ss.getId()).isEverythingComplete());
		Assert.assertTrue(Uploads.XMLEverythingComplete(ss.getId()));
		Assert.assertTrue(Uploads.getSpaceXMLStatus(ss.getId()).isEverythingComplete());
		ss.setEverythingComplete(true);
	}
	
	@StarexecTest
	private void xmlFileUploadCompleteTest() {
		Assert.assertFalse(Uploads.getSpaceXMLStatus(ss.getId()).isFileUploadComplete());
		Assert.assertTrue(Uploads.XMLFileUploadComplete(ss.getId()));
		Assert.assertTrue(Uploads.getSpaceXMLStatus(ss.getId()).isFileUploadComplete());
		ss.setFileUploadComplete(true);
	}
	
	@StarexecTest
	private void benchmarkEverythingCompleteTest() {
		Assert.assertFalse(Uploads.getBenchmarkStatus(bs.getId()).isEverythingComplete());
		Assert.assertTrue(Uploads.benchmarkEverythingComplete(bs.getId()));
		Assert.assertTrue(Uploads.getBenchmarkStatus(bs.getId()).isEverythingComplete());
		bs.setEverythingComplete(true);
	}
	
	@StarexecTest
	private void benchmarkFileUploadCompleteTest() {
		Assert.assertFalse(Uploads.getBenchmarkStatus(bs.getId()).isFileUploadComplete());
		Assert.assertTrue(Uploads.benchmarkFileUploadComplete(bs.getId()));
		Assert.assertTrue(Uploads.getBenchmarkStatus(bs.getId()).isFileUploadComplete());
		bs.setFileUploadComplete(true);
	}
	
	@StarexecTest
	private void benchmarkFileExtractCompleteTest() {
		Assert.assertFalse(Uploads.getBenchmarkStatus(bs.getId()).isFileExtractionComplete());
		Assert.assertTrue(Uploads.fileExtractComplete(bs.getId()));
		Assert.assertTrue(Uploads.getBenchmarkStatus(bs.getId()).isFileExtractionComplete());
		bs.setFileExtractionComplete(true);
	}
	@StarexecTest
	private void benchmarkProcessingBegunTest() {
		Assert.assertFalse(Uploads.getBenchmarkStatus(bs.getId()).isProcessingBegun());
		Assert.assertTrue(Uploads.processingBegun(bs.getId()));
		Assert.assertTrue(Uploads.getBenchmarkStatus(bs.getId()).isProcessingBegun());
		bs.setProcessingBegun(true);
	}
	

	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		s=ResourceLoader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());
		bs=Uploads.getBenchmarkStatus(Uploads.createBenchmarkUploadStatus(s.getId(), u.getId()));
		Assert.assertNotNull(bs);
		ss=Uploads.getSpaceXMLStatus(Uploads.createSpaceXMLUploadStatus(u.getId()));
		
	}
	

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspace(s.getId());
		Users.deleteUser(u.getId(),Users.getAdmins().get(0).getId());
		
	}

}
