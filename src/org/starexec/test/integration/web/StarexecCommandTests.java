package org.starexec.test.integration.web;

import org.junit.Assert;
import org.starexec.command.Connection;
import org.starexec.command.Status;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.logger.StarLogger;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class StarexecCommandTests extends TestSequence {
	private static final StarLogger log = StarLogger.getLogger(StarexecCommandTests.class);
	private Connection con;
	private Space space1=null; //will contain both solvers and benchmarks
	private Space space2=null;
	private Job job=null;
	File solverFile=null;
	File downloadDir=null;
	File benchmarkFile=null;
	File configFile=null;
	File processorFile=null;
	Solver solver=null;
	List<Integer> benchmarkIds=null;
	Configuration config=null;
	Processor proc=null;

	User user=null; //this user owns space1 and space2
	User user2=null; //this user is only in space1
	Space testCommunity=null;


	String solverURL=null;

	@StarexecTest
	private void CreateJobTest() {
		String jobName=TestUtil.getRandomJobName();
		int qid=Queues.getAllQ().getId();
		int jobId=con.createJob(space1.getId(), jobName, "", proc.getId(), -1, qid, 1, 1, true,1.0,false,0L, false,0);
		Assert.assertTrue("createJob returned " + jobId, jobId>0);
		Job job=Jobs.get(jobId);
		Assert.assertNotNull(job);
		Assert.assertEquals(jobName,job.getName());

		boolean jobDeleted = false;
		try {
			jobDeleted = Jobs.deleteAndRemove(jobId);
		} catch (SQLException e) {
			Assert.fail("Caught SQLException while deleting job: " + Util.getStackTrace(e));
		}
		Assert.assertTrue(jobDeleted);

	}

	@StarexecTest
	private void LoginTest() {
		Connection con=new Connection("bogusUsername@gmail.com","bogusPassword",Util.url(""));
		int status = con.login();
		assertTrue("User was able to login with bad username and password.", status < 0);
	}

	@StarexecTest
	private void GetIDTest() {
		int id=con.getUserID();
		Assert.assertEquals(user.getId(), id);

	}
	@StarexecTest
	private void ListSolversTest() {
		Map<Integer,String> mapping=con.getSolversInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getSolvers().size());
	}
	@StarexecTest
	private void ListBenchmarksTest() {
		Map<Integer,String> mapping=con.getBenchmarksInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getBenchmarks().size());

	}
	@StarexecTest
	private void ListJobsTest() {
		Map<Integer,String> mapping=con.getJobsInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getJobs().size());

	}
	@StarexecTest
	private void ListUsersTest() {
		Map<Integer,String> mapping=con.getUsersInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getUsers().size());
	}
	@StarexecTest
	private void ListSpacesTest() {
		Map<Integer,String> mapping=con.getSpacesInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getSubspaces().size());
	}
	@StarexecTest
	private void ListSolversByUser() {
		Map<Integer,String> mapping=con.getSolversByUser();
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Solvers.getSolverCountByUser(user.getId()));
	}
	@StarexecTest
	private void ListJobsByUser() {
		Map<Integer,String> mapping=con.getJobsByUser();
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Jobs.getJobCountByUser(user.getId()));
	}
	@StarexecTest
	private void ListBenchmarksByUser() {
		Map<Integer,String> mapping=con.getBenchmarksByUser();
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(),Benchmarks.getBenchmarkCountByUser(user.getId()));
	}

	@StarexecTest
	private void uploadConfiguration() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadConfiguration(name, "", configFile.getAbsolutePath(), solver.getId());
		Assert.assertTrue(result>0);
		Configuration testConfig=Solvers.getConfiguration(result);
		Assert.assertEquals(name,testConfig.getName());
		Assert.assertEquals(solver.getId(),testConfig.getSolverId());
	}

	@StarexecTest
	private void deleteConfiguration() {
		Configuration testConfig=loader.loadConfigurationFileIntoDatabase("CVC4Config.txt", solver.getId());
		Assert.assertNotNull(testConfig);
		Assert.assertNotNull(Solvers.getConfiguration(testConfig.getId()));
		List<Integer> configs= new ArrayList<>();
		configs.add(testConfig.getId());
		Assert.assertEquals(0,con.deleteConfigurations(configs));
		Assert.assertNull(Solvers.getConfiguration(testConfig.getId()));
	}

	@StarexecTest
	private void deleteProcessor() {
		Processor testProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, testCommunity.getId());
		Assert.assertNotNull(testProc);
		Assert.assertNotNull(Processors.get(testProc.getId()));
		List<Integer> procs= new ArrayList<>();
		procs.add(testProc.getId());

		Assert.assertEquals(0,con.deleteProcessors(procs));
		Assert.assertNull(Processors.get(testProc.getId()));

	}

	@StarexecTest
	private void uploadPostProcessor() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadPostProc(name, "", processorFile.getAbsolutePath(), testCommunity.getId());
		Assert.assertTrue(result>0);
		Processor testProc=Processors.get(result);
		Assert.assertEquals(name,testProc.getName());
		Assert.assertEquals(testCommunity.getId(),testProc.getCommunityId());
		Assert.assertEquals(ProcessorType.POST,testProc.getType());
		Assert.assertTrue(Processors.delete(testProc.getId()));
	}
	@StarexecTest
	private void uploadPreProcessor() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadPreProc(name, "", processorFile.getAbsolutePath(), testCommunity.getId());
		Assert.assertTrue(result>0);
		Processor testProc=Processors.get(result);
		Assert.assertEquals(name,testProc.getName());
		Assert.assertEquals(testCommunity.getId(),testProc.getCommunityId());
		Assert.assertEquals(ProcessorType.PRE,testProc.getType());
		Assert.assertTrue(Processors.delete(testProc.getId()));
	}
	@StarexecTest
	private void uploadBenchProcessor() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadBenchProc(name, "", processorFile.getAbsolutePath(), testCommunity.getId());
		Assert.assertTrue(result>0);
		Processor testProc=Processors.get(result);
		Assert.assertEquals(name,testProc.getName());
		Assert.assertEquals(testCommunity.getId(),testProc.getCommunityId());
		Assert.assertEquals(ProcessorType.BENCH,testProc.getType());
		Assert.assertTrue(Processors.delete(testProc.getId()));
	}
	@StarexecTest
	private void uploadSolver() throws Exception {
		addMessage("adding solver to space with id = "+space1.getId());
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadSolver(name, space1.getId(), solverFile.getAbsolutePath(), true,false,null,1);
		if (result>0) {
			addMessage("solver seems to have been added successfully -- testing database recall");
			Solver testSolver=Solvers.get(result);
			Assert.assertEquals(testSolver.getName(), name);
			Assert.assertTrue(Solvers.deleteAndRemoveSolver(testSolver.getId()));

		} else {
			throw new Exception("an error code was returned "+result);
		}
	}

	@StarexecTest
	private void uploadSolverFromURL() throws Exception {
		addMessage("adding solver to space with id = "+space1.getId());
		String name=TestUtil.getRandomSolverName();

		int result=con.uploadSolverFromURL(name, space1.getId(), solverURL, true,false,null,1);
		if (result>0) {
			addMessage("solver seems to have been added successfully -- testing database recall");
			Solver testSolver=Solvers.get(result);
			Assert.assertEquals(testSolver.getName(), name);
			Assert.assertTrue(Solvers.deleteAndRemoveSolver(testSolver.getId()));

		} else {
			throw new Exception("an error code was returned "+result);
		}
	}

	private void waitForUpload(int uploadId, int maxSeconds) {
		//it takes some time to finish benchmark uploads, so we want to wait for the upload to finish
		for (int x=0;x<maxSeconds;x++) {
			BenchmarkUploadStatus stat= Uploads.getBenchmarkStatus(uploadId);
			log.debug("upload Id = "+uploadId+" is finished = "+stat.isEverythingComplete());
			if (stat.isEverythingComplete()) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
	}

	@StarexecTest
	private void uploadBenchmarks() {

		Space tempSpace=loader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), Processors.getNoTypeProcessor().getId(), tempSpace.getId(), false);
		Assert.assertTrue(result>0);
		addMessage("upload ID = "+ result);

		waitForUpload(result,60);
		BenchmarkUploadStatus stat= Uploads.getBenchmarkStatus(result);
		Assert.assertTrue(stat.isEverythingComplete());
		Space t=Spaces.getDetails(tempSpace.getId(), user.getId());

		Assert.assertTrue(!t.getBenchmarks().isEmpty());

		for (Benchmark b : t.getBenchmarks()) {
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(b.getId()));
		}
		Assert.assertTrue(Spaces.removeSubspace(tempSpace.getId()));
	}

	@StarexecTest
	private void uploadBenchmarksFromURL() {

		//we are putting the benchmarks in a new space to avoid name collisions
		Space tempSpace=loader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());

		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), Processors.getNoTypeProcessor().getId(), tempSpace.getId(), false);
		Assert.assertTrue(result>0);
		waitForUpload(result, 60);
		Assert.assertTrue(Uploads.getBenchmarkStatus(result).isEverythingComplete());

		Space t=Spaces.getDetails(tempSpace.getId(), user.getId());
		Assert.assertTrue(!t.getBenchmarks().isEmpty());
		for (Benchmark b : t.getBenchmarks()) {
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(b.getId()));
		}

		Assert.assertTrue(Spaces.removeSubspace(tempSpace.getId()));

	}

	@StarexecTest
	private void downloadSpaceXML() {
		File xmlFile=new File(downloadDir,TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(xmlFile.exists());
		try {
			int result = con.downloadSpaceXML(space1.getId(), xmlFile.getAbsolutePath(), true, null);
			Assert.assertEquals(0, result);
			Assert.assertTrue(xmlFile.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}
	@StarexecTest
	private void downloadSpace() {
		File spaceFile=new File(downloadDir,TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(spaceFile.exists());
		try {
			int result=con.downloadSpace(space1.getId(), spaceFile.getAbsolutePath(), false, false);
			Assert.assertEquals(0, result);
			Assert.assertTrue(spaceFile.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void downloadSpaceHierarchy() {
		File spaceFile=new File(downloadDir, TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(spaceFile.exists());
		try {
			Assert.assertEquals(0, con.downloadSpaceHierarchy(space1.getId(), spaceFile.getAbsolutePath(), false, false));
			Assert.assertTrue(spaceFile.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void setFirstNameTest() {
		String fname=user.getFirstName();
		int result=con.setFirstName(fname+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(fname+"a", Users.get(user.getId()).getFirstName()); //ensure the database reflects the change
		Users.updateFirstName(user.getId(), fname); //change the name back just to keep things consistent
	}
	@StarexecTest
	private void setLastNameTest() {
		String lname=user.getLastName();
		int result=con.setLastName(lname+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(lname+"a", Users.get(user.getId()).getLastName()); //ensure the database reflects the change
		Users.updateLastName(user.getId(), lname); //change the name back just to keep things consistent
	}
	@StarexecTest
	private void setInstitutionTest() {
		String inst=user.getInstitution();
		int result=con.setInstitution(inst+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(inst+"a", Users.get(user.getId()).getInstitution()); //ensure the database reflects the change
		Users.updateInstitution(user.getId(), inst); //change the institution back just to keep things consistent
	}

	@StarexecTest
	private void linkUserTest() {
		Integer[] users=new Integer[1];
		users[0]=user2.getId();

		int status=con.linkUsers(users, space1.getId(), space2.getId());
		Assert.assertEquals(0,status);
		Map<Integer,String> u=con.getUsersInSpace(space2.getId());

		Assert.assertTrue(u.containsKey(user2.getId()));

		List<Integer> ids = new ArrayList<>();
		ids.add(user2.getId());
		Assert.assertTrue(Spaces.removeUsers(ids, space2.getId()));

	}

	@StarexecTest
	private void linkSolverTest() {
		Integer[] solverArr=new Integer[1];
		solverArr[0]=solver.getId();
		int status=con.linkSolvers(solverArr, space1.getId(), space2.getId(), false);
		Assert.assertEquals(0, status);
		Map<Integer,String> solvers=con.getSolversInSpace(space2.getId());
		Assert.assertTrue(solvers.containsKey(solver.getId()));

		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> solverIds = new ArrayList<>();
		solverIds.addAll(solvers.keySet());
		Assert.assertTrue(Spaces.removeSolvers(solverIds, space2.getId()));

	}

	@StarexecTest
	private void  copySolverTest() {
		Integer[] solverArr=new Integer[1];
		solverArr[0]=solver.getId();
		int status=Math.min(0,con.copySolvers(solverArr, space1.getId(), space2.getId(), false).get(0));
		Assert.assertEquals(0, status);
		Map<Integer,String> solvers=con.getSolversInSpace(space2.getId());

		//the name is very long and random, so the only way the given solver name will be in the
		//second space will be if it was copied successfully
		Assert.assertTrue(solvers.containsValue(solver.getName()));
		Assert.assertFalse(solvers.containsKey(solver.getId())); //the ID should NOT be the same, since the copy should be new


		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> solverIds = new ArrayList<>();
		solverIds.addAll(solvers.keySet());

		//delete all the newly created solvers and remove them from space2
		Assert.assertTrue(Spaces.removeSolvers(solverIds, space2.getId()));
		for (Integer i : solverIds) {
			Assert.assertTrue(Solvers.deleteAndRemoveSolver(i));
		}
	}

	@StarexecTest
	private void  copyBenchmarkTest() {
		Space toCopy=loader.loadSpaceIntoDatabase(user.getId(),space1.getId());
		Integer[] benchArr=new Integer[benchmarkIds.size()];
		for (int index=0;index<benchArr.length;index++) {
			benchArr[index]=benchmarkIds.get(index);
		}

		int status=Math.min(0,con.copyBenchmarks(benchArr, space1.getId(), toCopy.getId()).get(0));
		Assert.assertEquals(0, status);

		Map<Integer,String> benches=con.getBenchmarksInSpace(toCopy.getId());

        Assert.assertFalse(isErrorMap(benches));
		//the name is very long and random, so the only way the given benchmark name will be in the
		//second space will be if it was copied successfully
		for (Integer bid : benchArr) {
			Benchmark b=Benchmarks.get(bid);
			Assert.assertTrue(benches.containsValue(b.getName()));
			Assert.assertFalse(benches.containsKey(bid)); //the ID should NOT be the same, since the copy should be new
		}

		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> benchIds = new ArrayList<>();
		benchIds.addAll(benches.keySet());

		Assert.assertTrue(Spaces.removeBenches(benchIds, toCopy.getId()));
		for (Integer i : benchIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}

		Spaces.removeSubspace(toCopy.getId());
	}

	@StarexecTest
	private void  linkBenchmarkTest() {
		Space toCopy=loader.loadSpaceIntoDatabase(user.getId(),space1.getId());
		Integer[] benchArr=new Integer[benchmarkIds.size()];
		for (int index=0;index<benchArr.length;index++) {
			benchArr[index]=benchmarkIds.get(index);
		}

		int status=con.linkBenchmarks(benchArr, space1.getId(), toCopy.getId());
		Assert.assertEquals(0, status);

		Map<Integer,String> benches=con.getBenchmarksInSpace(toCopy.getId());


		Assert.assertEquals(benches.size(), benchArr.length);
		for (Integer bid : benchmarkIds) {
			log.debug("attempting to get back benchmark "+bid);
			//the name is very long and random, so the only way the given benchmark name will be in the
			//second space will be if it was copied successfully
			Assert.assertTrue(benches.containsValue(Benchmarks.get(bid).getName()));
			Assert.assertTrue(benches.containsKey(bid));
		}



		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> benchIds = new ArrayList<>();
		benchIds.addAll(benches.keySet());

		Assert.assertTrue(Spaces.removeBenches(benchIds, toCopy.getId()));
		Spaces.removeSubspace(toCopy.getId());

	}

	@StarexecTest
	private void createSubspaceTest() throws Exception {
		String name=TestUtil.getRandomSpaceName();
		Permission p=new Permission();
		int newSpaceId=con.createSubspace(name, "", testCommunity.getId(), p, false);
		if (newSpaceId>0) {
			Space testSpace=Spaces.get(newSpaceId);
			Assert.assertNotNull(testSpace);
			Assert.assertEquals(name, testSpace.getName());
			Assert.assertEquals(testCommunity.getId(), Spaces.getParentSpace(testSpace.getId()));

			Assert.assertTrue(Spaces.removeSubspace(newSpaceId));
		} else {
			throw new Exception("there was an error creating a new subspace. code = "+newSpaceId);
		}
	}

	@StarexecTest
	private void deleteSolversTest() {
		Solver tempSolver=loader.loadSolverIntoDatabase("CVC4.zip", testCommunity.getId(), user.getId());
		Assert.assertNotNull(Solvers.get(tempSolver.getId()));
		List<Integer> ids= new ArrayList<>();
		ids.add(tempSolver.getId());
		Assert.assertEquals(0,con.deleteSolvers(ids));
		Assert.assertNull(Solvers.get(tempSolver.getId()));

		Assert.assertTrue(Solvers.deleteAndRemoveSolver(tempSolver.getId()));
	}

	@StarexecTest
	private void deleteJobsTest() {
		List<Integer> solverIds= new ArrayList<>();
		solverIds.add(solver.getId());
		Job tempJob=loader.loadJobIntoDatabase(space1.getId(), user.getId(), -1, proc.getId(), solverIds, benchmarkIds,100,100,1);
		List<Integer> ids= new ArrayList<>();
		ids.add(tempJob.getId());
		Assert.assertNotNull(Jobs.get(tempJob.getId()));
		Assert.assertEquals(0,con.deleteJobs(ids));
		Assert.assertNull(Jobs.get(tempJob.getId()));

		boolean jobDeleted = false;
		try {
			jobDeleted = Jobs.deleteAndRemove(tempJob.getId());
		} catch (SQLException e) {
			Assert.fail("Caught SQLException while trying to delete job: " + Util.getStackTrace(e));
		}

		Assert.assertTrue(jobDeleted);

	}

	@StarexecTest
	private void deleteBenchmarksTest() {
		Space tempSpace=loader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		List<Integer> ids=loader.loadBenchmarksIntoDatabase("benchmarks.zip",tempSpace.getId(),user.getId());


		Assert.assertEquals(0,con.deleteBenchmarks(ids));
		for (Integer i :ids) {
			Assert.assertNull(Benchmarks.get(i));

			//once we've confirmed the deletion worked, make sure the benchmark is actually removed from the database
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(i));
		}
		Assert.assertTrue(Spaces.removeSubspace(tempSpace.getId()));

	}

	@StarexecTest
	private void copySpaceTest() {
		Integer[] spaceArr=new Integer[1];
		List<Space> before=Spaces.getSubSpaces(space2.getId(), user.getId());
		spaceArr[0]=space1.getId();
		int status=Math.min(0,con.copySpaces(spaceArr, Spaces.getParentSpace(space1.getId()), space2.getId(), false, false).get(0));
		Assert.assertEquals(0, status);
		List<Space> after=Spaces.getSubSpaces(space2.getId(), user.getId());
		Assert.assertTrue(after.size()>before.size());

	}

	@StarexecTest
	private void downloadJobCSV() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(loader.getDownloadDirectory(),fileName);
		Assert.assertFalse("Download directory already exists when it shouldn't.", downloadDir.exists());
		try {
			int status = con.downloadJobInfo(job.getId(), downloadDir.getAbsolutePath(), true, false);
			Assert.assertEquals("downloadJobInfo returned " + status,0, status);
			Assert.assertTrue("Download directory did not exist when it should.", downloadDir.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}


	@StarexecTest
	private void downloadJobOutput() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(loader.getDownloadDirectory(),fileName);
		Assert.assertFalse(downloadDir.exists());

		try {
			Assert.assertEquals(0,con.downloadJobOutput(job.getId(), downloadDir.getAbsolutePath()));
			Assert.assertTrue(downloadDir.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void downloadProcessorTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(loader.getDownloadDirectory(),fileName);
		Assert.assertFalse("Found download directory before the download occurred.",downloadDir.exists());
		try {
			int status = con.downloadPostProcessor(testCommunity.getId(), downloadDir.getAbsolutePath());
			Assert.assertEquals(Status.getStatusMessage(status),Status.STATUS_SUCCESS, status);
			Assert.assertTrue("Could not find download directory after the download occurred.",downloadDir.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void downloadSolverTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(loader.getDownloadDirectory(),fileName);
		try {
			int status=con.downloadSolver(solver.getId(), downloadDir.getAbsolutePath());
			Assert.assertEquals(0, status);
			Assert.assertTrue(downloadDir.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void downloadBenchmarkTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(loader.getDownloadDirectory(),fileName);
		try {
			int status=con.downloadBenchmark(benchmarkIds.get(0), downloadDir.getAbsolutePath());
			Assert.assertEquals(0, status);
			Assert.assertTrue(downloadDir.exists());
		} catch (IOException e) {
			Assert.fail("Caught IOException: " + Util.getStackTrace(e));
		}
	}

	@StarexecTest
	private void removeSolversTest() {
		Solver temp=loader.loadSolverIntoDatabase("CVC4.zip",testCommunity.getId(),user.getId());
		Assert.assertTrue(Solvers.getAssociatedSpaceIds(temp.getId()).contains(testCommunity.getId()));

		List<Integer> id= new ArrayList<>();
		id.add(temp.getId());
		Assert.assertEquals(0,con.removeSolvers(id, testCommunity.getId()));

		//first, ensure the solver is still there. It should NOT have been deleted
		Assert.assertNotNull(Solvers.get(temp.getId()));
		//then, make sure it is not in the space
		Assert.assertFalse(Solvers.getAssociatedSpaceIds(temp.getId()).contains(testCommunity.getId()));
		Assert.assertTrue(Solvers.deleteAndRemoveSolver(temp.getId()));
	}

	@StarexecTest
	private void removeBenchmarksTest() {
		Space tempSpace=loader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		List<Integer> ids=loader.loadBenchmarksIntoDatabase("benchmarks.zip", tempSpace.getId(), user.getId());
		Assert.assertTrue(Benchmarks.getAssociatedSpaceIds(ids.get(0)).contains(tempSpace.getId()));
		List<Integer> id= new ArrayList<>();
		id.add(ids.get(0));

		Assert.assertEquals(0, con.removeBenchmarks(id, tempSpace.getId()));
		Assert.assertNotNull(Benchmarks.get(ids.get(0)));

		Assert.assertFalse(Benchmarks.getAssociatedSpaceIds(ids.get(0)).contains(tempSpace.getId()));

		for (Integer i : ids) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Spaces.removeSubspace(tempSpace.getId());
	}

	@StarexecTest
	private void removeJobsTest() {
		Space tempSpace=loader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		Assert.assertNotNull(job);
		List<Integer> jobIds= new ArrayList<>();
		jobIds.add(job.getId());
		Assert.assertEquals(0,con.removeJobs(jobIds, tempSpace.getId()));
		Assert.assertNotNull(Jobs.getDirectory(job.getId())); //we do not want to have deleted the job

		Spaces.removeSubspace(tempSpace.getId());
	}

	@StarexecTest
	private void removeSpacesTest() {
		Space tempSpace=loader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		List<Integer> id= new ArrayList<>();
		id.add(tempSpace.getId());
		Assert.assertNotNull(Spaces.getName(tempSpace.getId()));
		Assert.assertEquals(0,con.removeSubspace(id, false));
		try {
			// wait for a few seconds because spaces are removed in a separate thread
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			//pass
		}

		Assert.assertNull(Spaces.getName(tempSpace.getId()));

	}

	@StarexecTest
	private void viewSolverTest() {
		Map<String,String> attrs=con.getSolverAttributes(solver.getId());
		Assert.assertEquals(String.valueOf(solver.getId()),attrs.get("id"));
		Assert.assertEquals(solver.getName(),attrs.get("name"));
	}

	@StarexecTest
	private void viewSpaceTest() {
		Map<String,String> attrs=con.getSpaceAttributes(testCommunity.getId());
		Assert.assertEquals(String.valueOf(testCommunity.getId()),attrs.get("id"));
		Assert.assertEquals(testCommunity.getName(),attrs.get("name"));
	}

	@StarexecTest
	private void viewJobTest() {
		Map<String,String> attrs=con.getJobAttributes(job.getId());
		Assert.assertEquals(String.valueOf(job.getId()),attrs.get("id"));
		Assert.assertEquals(job.getName(),attrs.get("name"));
	}

	@StarexecTest
	private void viewProcessorTest() {
		Map<String,String> attrs=con.getProcessorAttributes(proc.getId());
		Assert.assertEquals(String.valueOf(proc.getId()),attrs.get("id"));
		Assert.assertEquals(proc.getName(),attrs.get("name"));
	}

	@StarexecTest
	private void viewConfigurationTest() {
		Map<String,String> attrs=con.getConfigurationAttributes(config.getId());
		Assert.assertEquals(String.valueOf(config.getId()),attrs.get("id"));
		Assert.assertEquals(config.getName(),attrs.get("name"));
	}

	@StarexecTest
	private void viewBenchmarkTest() {
		Map<String,String> attrs=con.getBenchmarkAttributes(benchmarkIds.get(0));
		Assert.assertEquals(String.valueOf(benchmarkIds.get(0)),attrs.get("id"));
	}


	@Override
	protected void setup() throws Exception {
		user=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();
		testCommunity=Communities.getTestCommunity();
		Users.associate(user.getId(),testCommunity.getId());
		Permissions.set(user.getId(), testCommunity.getId(), Permissions.getFullPermission());
		con=new Connection(user.getEmail(),user.getPassword(),Util.url(""));
		int status = con.login();
		Assert.assertEquals(0,status);

		//space1 will contain solvers and benchmarks
		space1=loader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId(), "name with spaces");
		space2=loader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());

		Users.associate(user2.getId(), space1.getId());

		solverFile=loader.getResource("CVC4.zip");
		benchmarkFile=loader.getResource("benchmarks.zip");
		configFile=loader.getResource("CVC4Config.txt");
		processorFile=loader.getResource("postproc.zip");
		Assert.assertNotNull(space1);
		Assert.assertNotNull(space2);


		downloadDir=loader.getDownloadDirectory();
		solver=loader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), user.getId());
		config=loader.loadConfigurationFileIntoDatabase("CVC4Config.txt", solver.getId());
		proc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, testCommunity.getId());
		Assert.assertNotNull(solver);

		benchmarkIds=loader.loadBenchmarksIntoDatabase("benchmarks.zip", space1.getId(), user.getId());
		List<Integer> solverIds= new ArrayList<>();
		solverIds.add(solver.getId());
		job=loader.loadJobIntoDatabase(space1.getId(), user.getId(), -1, proc.getId(), solverIds, benchmarkIds,100,100,1);

		Assert.assertNotNull(benchmarkIds);


		solverURL=Util.url("public/resources/CVC4.zip");
	}



	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

	private boolean isErrorMap(Map<Integer,String> mapping) {
		if (mapping.size()!=1) {
			return false;
		}
		if ((Integer)((mapping.keySet().toArray())[0])<0) {
			log.debug("the value is "+mapping.keySet().toArray()[0]);
			return true;
		}
		return false;
	}

	@Override
	protected String getTestName() {
		return "StarexecCommandTests";
	}

}
