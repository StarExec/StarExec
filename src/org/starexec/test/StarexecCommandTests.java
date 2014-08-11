package org.starexec.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.starexec.command.Connection;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;


/*TODO:
	Download[new job info, new job output]
*/
public class StarexecCommandTests extends TestSequence {
	private static final Logger log = Logger.getLogger(TestSequence.class);	
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
	
	@Test
	private void CreateJobTest() {
		String jobName=TestUtil.getRandomJobName();
		int qid=Queues.getAll().get(0).getId();
		int jobId=con.createJob(space1.getId(), jobName, "", proc.getId(), -1, qid, 100, 100, true,1.0,false,0L);
		Assert.assertTrue(jobId>0);
		Job job=Jobs.get(jobId);
		Assert.assertNotNull(job);
		Assert.assertEquals(jobName,job.getName());
		
		
		Assert.assertTrue(Jobs.deleteAndRemove(jobId));
		
	}
	
	@Test
	private void GetIDTest() {
		int id=con.getUserID();
		Assert.assertEquals(user.getId(), id);
		
	}
	@Test
	private void ListSolversTest() {
		HashMap<Integer,String> mapping=con.getSolversInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getSolvers().size());
	}
	@Test
	private void ListBenchmarksTest() {
		HashMap<Integer,String> mapping=con.getBenchmarksInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getBenchmarks().size());

	}
	@Test
	private void ListJobsTest() {
		HashMap<Integer,String> mapping=con.getJobsInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getJobs().size());

	}
	@Test
	private void ListUsersTest() {
		HashMap<Integer,String> mapping=con.getUsersInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getUsers().size());
	}
	@Test
	private void ListSpacesTest() {
		HashMap<Integer,String> mapping=con.getSpacesInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Spaces.getDetails(space1.getId(), user.getId()).getSubspaces().size());
	}
	@Test
	private void ListSolversByUser() {
		HashMap<Integer,String> mapping=con.getSolversByUser();
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Solvers.getSolverCountByUser(user.getId()));
	}
	@Test
	private void ListJobsByUser() {
		HashMap<Integer,String> mapping=con.getJobsByUser();
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(), Jobs.getJobCountByUser(user.getId()));
	}
	@Test
	private void ListBenchmarksByUser() {
		HashMap<Integer,String> mapping=con.getBenchmarksByUser();
		Assert.assertFalse(isErrorMap(mapping));
		Assert.assertEquals(mapping.size(),Benchmarks.getBenchmarkCountByUser(user.getId()));
	}
	
	@Test
	private void uploadConfiguration() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadConfiguration(name, "", configFile.getAbsolutePath(), solver.getId());
		Assert.assertTrue(result>0);
		Configuration testConfig=Solvers.getConfiguration(result);
		Assert.assertEquals(name,testConfig.getName());
		Assert.assertEquals(solver.getId(),testConfig.getSolverId());
	}
	
	@Test
	private void deleteConfiguration() {
		Configuration testConfig=ResourceLoader.loadConfigurationFileIntoDatabase("CVC4Config.txt", solver.getId());
		Assert.assertNotNull(testConfig);
		Assert.assertNotNull(Solvers.getConfiguration(testConfig.getId()));
		List<Integer> configs=new ArrayList<Integer>();
		configs.add(testConfig.getId());
		Assert.assertEquals(0,con.deleteConfigurations(configs));
		Assert.assertNull(Solvers.getConfiguration(testConfig.getId()));
	}
	
	@Test
	private void deleteProcessor() {
		Processor testProc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, testCommunity.getId());
		Assert.assertNotNull(testProc);
		Assert.assertNotNull(Processors.get(testProc.getId()));
		List<Integer> procs=new ArrayList<Integer>();
		procs.add(testProc.getId());
		
		Assert.assertEquals(0,con.deleteProcessors(procs));
		Assert.assertNull(Processors.get(testProc.getId()));
		
	}
	
	@Test
	private void uploadPostProcessor() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadPostProc(name, "", processorFile.getAbsolutePath(), testCommunity.getId());
		Assert.assertTrue(result>0);
		Processor testProc=Processors.get(result);
		Assert.assertEquals(name,testProc.getName());
		Assert.assertEquals(testCommunity.getId(),testProc.getCommunityId());
		Assert.assertEquals(ProcessorType.POST,testProc.getType());
		Processors.delete(testProc.getId());
	}
	@Test
	private void uploadPreProcessor() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadPreProc(name, "", processorFile.getAbsolutePath(), testCommunity.getId());
		Assert.assertTrue(result>0);
		Processor testProc=Processors.get(result);
		Assert.assertEquals(name,testProc.getName());
		Assert.assertEquals(testCommunity.getId(),testProc.getCommunityId());
		Assert.assertEquals(ProcessorType.PRE,testProc.getType());
		Processors.delete(testProc.getId());
	}
	@Test
	private void uploadBenchProcessor() {
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadBenchProc(name, "", processorFile.getAbsolutePath(), testCommunity.getId());
		Assert.assertTrue(result>0);
		Processor testProc=Processors.get(result);
		Assert.assertEquals(name,testProc.getName());
		Assert.assertEquals(testCommunity.getId(),testProc.getCommunityId());
		Assert.assertEquals(ProcessorType.BENCH,testProc.getType());
		Processors.delete(testProc.getId());
	}
	@Test
	private void uploadSolver() throws Exception {
		addMessage("adding solver to space with id = "+space1.getId());
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadSolver(name, space1.getId(), solverFile.getAbsolutePath(), true);
		if (result>0) {
			addMessage("solver seems to have been added successfully -- testing database recall");
			Solver testSolver=Solvers.get(result);
			Assert.assertEquals(testSolver.getName(), name);
			Solvers.deleteAndRemoveSolver(testSolver.getId());
			
		} else {
			throw new Exception("an error code was returned "+result);
		}
	}
	
	@Test
	private void uploadSolverFromURL() throws Exception {
		addMessage("adding solver to space with id = "+space1.getId());
		String name=TestUtil.getRandomSolverName();
		
		int result=con.uploadSolverFromURL(name, space1.getId(), solverURL, true);
		if (result>0) {
			addMessage("solver seems to have been added successfully -- testing database recall");
			Solver testSolver=Solvers.get(result);
			Assert.assertEquals(testSolver.getName(), name);
			Solvers.deleteAndRemoveSolver(testSolver.getId());
			
		} else {
			throw new Exception("an error code was returned "+result);
		}
	}
	//TODO: These benchmarks will need to be deleted
	@Test
	private void uploadBenchmarks() throws Exception {
		
		//we are putting benchmarks in a new space to avoid name collisions
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), 1, tempSpace.getId(), false);
		Assert.assertEquals(0,result);
		
		Assert.assertTrue(Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId()));
	}
	
	@Test
	private void uploadBenchmarksFromURL() {
		
		//we are putting the benchmarks in a new space to avoid name collisions
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
	
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), 1, tempSpace.getId(), false);
		Assert.assertEquals(0,result);
		
		Assert.assertTrue(Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId()));

	}
	
	@Test 
	private void downloadSpaceXML() {
		File xmlFile=new File(downloadDir,TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(xmlFile.exists());
		int result=con.downloadSpaceXML(space1.getId(), xmlFile.getAbsolutePath());
		Assert.assertEquals(0, result);
		Assert.assertTrue(xmlFile.exists());
	}
	@Test 
	private void downloadSpace() {
		File spaceFile=new File(downloadDir,TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(spaceFile.exists());
		int result=con.downloadSpace(space1.getId(), spaceFile.getAbsolutePath(), false, false);
		Assert.assertEquals(0, result);
		Assert.assertTrue(spaceFile.exists());
	}
	
	@Test
	private void downloadSpaceHierarchy() {
		File spaceFile=new File(downloadDir, TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(spaceFile.exists());
		Assert.assertEquals(0, con.downloadSpaceHierarchy(space1.getId(), spaceFile.getAbsolutePath(), false, false));
		Assert.assertTrue(spaceFile.exists());
	}
	
	@Test 
	private void setFirstNameTest() {
		String fname=user.getFirstName();
		int result=con.setFirstName(fname+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(fname+"a", Users.get(user.getId()).getFirstName()); //ensure the database reflects the change
		Users.updateFirstName(user.getId(), fname); //change the name back just to keep things consistent
	}
	@Test 
	private void setLastNameTest() {
		String lname=user.getLastName();
		int result=con.setLastName(lname+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(lname+"a", Users.get(user.getId()).getLastName()); //ensure the database reflects the change
		Users.updateLastName(user.getId(), lname); //change the name back just to keep things consistent
	}
	@Test 
	private void setInstitutionTest() {
		String inst=user.getInstitution();
		int result=con.setInstitution(inst+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(inst+"a", Users.get(user.getId()).getInstitution()); //ensure the database reflects the change
		Users.updateInstitution(user.getId(), inst); //change the institution back just to keep things consistent
	}
	
	@Test
	private void linkUserTest() {
		Integer[] users=new Integer[1];
		users[0]=user2.getId();
		
		int status=con.linkUsers(users, space1.getId(), space2.getId());
		Assert.assertEquals(0,status);
		HashMap<Integer,String> u=con.getUsersInSpace(space2.getId());
		
		Assert.assertTrue(u.containsKey(user2.getId()));
		
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(user2.getId());
		Assert.assertTrue(Spaces.removeUsers(ids, space2.getId()));
		
	}
	
	@Test
	private void linkSolverTest() {
		Integer[] solverArr=new Integer[1];
		solverArr[0]=solver.getId();
		int status=con.linkSolvers(solverArr, space1.getId(), space2.getId(), false);
		Assert.assertEquals(0, status);
		HashMap<Integer,String> solvers=con.getSolversInSpace(space2.getId());
		Assert.assertTrue(solvers.containsKey(solver.getId()));
		
		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> solverIds =new ArrayList<Integer>();
		solverIds.addAll(solvers.keySet());
		Assert.assertTrue(Spaces.removeSolvers(solverIds, space2.getId()));	
		
	}
	
	@Test 
	private void  copySolverTest() {
		Integer[] solverArr=new Integer[1];
		solverArr[0]=solver.getId();
		int status=Math.min(0,con.copySolvers(solverArr, space1.getId(), space2.getId(), false).get(0));
		Assert.assertEquals(0, status);
		HashMap<Integer,String> solvers=con.getSolversInSpace(space2.getId());
		
		//the name is very long and random, so the only way the given solver name will be in the 
		//second space will be if it was copied successfully
		Assert.assertTrue(solvers.containsValue(solver.getName()));
		Assert.assertFalse(solvers.containsKey(solver.getId())); //the ID should NOT be the same, since the copy should be new
		
		
		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> solverIds =new ArrayList<Integer>();
		solverIds.addAll(solvers.keySet());
		
		//delete all the newly created solvers and remove them from space2
		Assert.assertTrue(Spaces.removeSolvers(solverIds, space2.getId()));	
		for (Integer i : solverIds) {
			Assert.assertTrue(Solvers.deleteAndRemoveSolver(i));
		}
	}
	
	@Test 
	private void  copyBenchmarkTest() {
		Space toCopy=ResourceLoader.loadSpaceIntoDatabase(user.getId(),space1.getId());
		Integer[] benchArr=new Integer[benchmarkIds.size()];
		for (int index=0;index<benchArr.length;index++) {
			benchArr[index]=benchmarkIds.get(index);
		}
		
		int status=Math.min(0,con.copyBenchmarks(benchArr, space1.getId(), toCopy.getId()).get(0));
		Assert.assertEquals(0, status);
		
		HashMap<Integer,String> benches=con.getBenchmarksInSpace(toCopy.getId());
		
        Assert.assertFalse(isErrorMap(benches));
		//the name is very long and random, so the only way the given benchmark name will be in the 
		//second space will be if it was copied successfully
		for (Integer bid : benchArr) {
			Benchmark b=Benchmarks.get(bid);       
			Assert.assertTrue(benches.containsValue(b.getName()));
			Assert.assertFalse(benches.containsKey(bid)); //the ID should NOT be the same, since the copy should be new
		}
		
		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> benchIds =new ArrayList<Integer>();
		benchIds.addAll(benches.keySet());
		
		Assert.assertTrue(Spaces.removeBenches(benchIds, toCopy.getId()));
		for (Integer i : benchIds) {
			Assert.assertTrue(Benchmarks.deleteAndRemoveBenchmark(i));
		}
		Spaces.removeSubspaces(toCopy.getId(), space1.getId(), user.getId());		
	}
	
	@Test 
	private void  linkBenchmarkTest() {
		Space toCopy=ResourceLoader.loadSpaceIntoDatabase(user.getId(),space1.getId());
		Integer[] benchArr=new Integer[benchmarkIds.size()];
		for (int index=0;index<benchArr.length;index++) {
			benchArr[index]=benchmarkIds.get(index);
		}
		
		int status=con.linkBenchmarks(benchArr, space1.getId(), toCopy.getId());
		Assert.assertEquals(0, status);
		
		HashMap<Integer,String> benches=con.getBenchmarksInSpace(toCopy.getId());
		
	
		Assert.assertEquals(benches.size(), benchArr.length);
		for (Integer bid : benchmarkIds) {
			log.debug("attempting to get back benchmark "+bid);
			//the name is very long and random, so the only way the given benchmark name will be in the 
			//second space will be if it was copied successfully
			Assert.assertTrue(benches.containsValue(Benchmarks.get(bid).getName()));
			Assert.assertTrue(benches.containsKey(bid)); 
		}
		
		
		
		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> benchIds =new ArrayList<Integer>();
		benchIds.addAll(benches.keySet());
		
		Assert.assertTrue(Spaces.removeBenches(benchIds, toCopy.getId()));
		Spaces.removeSubspaces(toCopy.getId(), space1.getId(), user.getId());
		
	}
	
	@Test
	private void createSubspaceTest() throws Exception {
		String name=TestUtil.getRandomSpaceName();
		org.starexec.command.Permission p=new org.starexec.command.Permission();
		int newSpaceId=con.createSubspace(name, "", testCommunity.getId(), p, false);
		if (newSpaceId>0) {
			Space testSpace=Spaces.get(newSpaceId);
			Assert.assertNotNull(testSpace);
			Assert.assertEquals(name, testSpace.getName());
			Assert.assertEquals(testCommunity.getId(), (int)Spaces.getParentSpace(testSpace.getId()));
		} else {
			throw new Exception("there was an error creating a new subspace. code = "+newSpaceId);
		}
	}
	
	@Test
	private void deleteSolversTest() {
		Solver tempSolver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", testCommunity.getId(), user.getId());
		Assert.assertNotNull(Solvers.get(tempSolver.getId()));
		List<Integer> ids=new ArrayList<Integer>();
		ids.add(tempSolver.getId());
		Assert.assertEquals(0,con.deleteSolvers(ids));
		Assert.assertNull(Solvers.get(tempSolver.getId()));
	}
	
	@Test
	private void deleteJobsTest() {
		List<Integer> solverIds=new ArrayList<Integer>();
		solverIds.add(solver.getId());
		Job tempJob=ResourceLoader.loadJobIntoDatabase(space1.getId(), user.getId(), -1, proc.getId(), solverIds, benchmarkIds,100,100,1);
		List<Integer> ids= new ArrayList<Integer>();
		ids.add(tempJob.getId());
		Assert.assertNotNull(Jobs.get(tempJob.getId()));
		Assert.assertEquals(0,con.deleteJobs(ids));
		Assert.assertNull(Jobs.get(tempJob.getId()));

	}
	
	@Test
	private void deleteBenchmarksTest() {
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",tempSpace.getId(),user.getId());

		
		Assert.assertEquals(0,con.deleteBenchmarks(ids));
		for (Integer i :ids) {
			Assert.assertNull(Benchmarks.get(i));
		}
		Assert.assertTrue(Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId()));
		
	}
	
	@Test
	private void copySpaceTest() {
		Integer[] spaceArr=new Integer[1];
		List<Space> before=Spaces.getSubSpaces(space2.getId(), user.getId());
		spaceArr[0]=space1.getId();
		int status=Math.min(0,con.copySpaces(spaceArr, Spaces.getParentSpace(space1.getId()), space2.getId(), false).get(0));
		Assert.assertEquals(0, status);
		List<Space> after=Spaces.getSubSpaces(space2.getId(), user.getId());
		Assert.assertTrue(after.size()>before.size());
		
	}
	
	@Test
	private void downloadJobCSV() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getDownloadDirectory(),fileName);
		Assert.assertFalse(downloadDir.exists());
		Assert.assertEquals(0,con.downloadJobInfo(job.getId(), downloadDir.getAbsolutePath(), true,false));
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void downloadJobOutput() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getDownloadDirectory(),fileName);
		Assert.assertFalse(downloadDir.exists());
		
		Assert.assertEquals(0,con.downloadJobOutput(job.getId(), downloadDir.getAbsolutePath()));
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void downloadProcessorTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getDownloadDirectory(),fileName);
		Assert.assertFalse(downloadDir.exists());
		Assert.assertEquals(0,con.downloadPostProcessor(testCommunity.getId(), downloadDir.getAbsolutePath()));
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void downloadSolverTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getDownloadDirectory(),fileName);
		int status=con.downloadSolver(solver.getId(), downloadDir.getAbsolutePath());
		Assert.assertEquals(0, status);
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void downloadBenchmarkTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getDownloadDirectory(),fileName);
		int status=con.downloadBenchmark(benchmarkIds.get(0), downloadDir.getAbsolutePath());
		Assert.assertEquals(0, status);
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void removeSolversTest() {
		Solver temp=ResourceLoader.loadSolverIntoDatabase("CVC4.zip",testCommunity.getId(),user.getId());
		Assert.assertTrue(Solvers.getAssociatedSpaceIds(temp.getId()).contains(testCommunity.getId()));
		
		List<Integer> id=new ArrayList<Integer>();
		id.add(temp.getId());
		Assert.assertEquals(0,con.removeSolvers(id, testCommunity.getId()));
		
		//first, ensure the solver is still there. It should NOT have been deleted
		Assert.assertNotNull(Solvers.get(temp.getId()));
		//then, make sure it is not in the space
		Assert.assertFalse(Solvers.getAssociatedSpaceIds(temp.getId()).contains(testCommunity.getId()));
		Solvers.deleteAndRemoveSolver(temp.getId());
	}
	
	@Test
	private void removeBenchmarksTest() {
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", tempSpace.getId(), user.getId());
		Assert.assertTrue(Benchmarks.getAssociatedSpaceIds(ids.get(0)).contains(tempSpace.getId()));
		List<Integer> id=new ArrayList<Integer>();
		id.add(ids.get(0));
		
		Assert.assertEquals(0, con.removeBenchmarks(id, tempSpace.getId()));
		Assert.assertNotNull(Benchmarks.get(ids.get(0)));
		
		Assert.assertFalse(Benchmarks.getAssociatedSpaceIds(ids.get(0)).contains(tempSpace.getId()));
		
		for (Integer i : ids) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId());
	}
	
	@Test
	private void removeJobsTest() {
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		Assert.assertNotNull(job);
		List<Integer> jobIds=new ArrayList<Integer>();
		jobIds.add(job.getId());
		Assert.assertEquals(0,con.removeJobs(jobIds, tempSpace.getId()));
		Assert.assertNotNull(Jobs.getDirectory(job.getId())); //we do not want to have deleted the job
		
		Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId());
	}
	
	@Test
	private void removeSpacesTest() {
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());

		List<Integer> id=new ArrayList<Integer>();
		id.add(tempSpace.getId());
		Assert.assertNotNull(Spaces.getName(tempSpace.getId()));
		Assert.assertEquals(0,con.removeSubspace(id,testCommunity.getId(),false));
		Assert.assertNull(Spaces.getName(tempSpace.getId()));

	}
	
	
	@Override
	protected void setup() throws Exception {
		user=Users.getTestUser();
		user2=ResourceLoader.loadUserIntoDatabase();
		testCommunity=Communities.getTestCommunity();
		
		//this prevents the apache http libraries from logging things. Their logs are very prolific
		//and drown out ours
		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);

		con=new Connection(user.getEmail(),R.TEST_USER_PASSWORD,Util.url(""));
		int status = con.login();
		Assert.assertEquals(0,status);
		
		//space1 will contain solvers and benchmarks
		space1=ResourceLoader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());	
		
		Users.associate(user2.getId(), space1.getId());
		
		solverFile=ResourceLoader.getResource("CVC4.zip");
		benchmarkFile=ResourceLoader.getResource("benchmarks.zip");
		configFile=ResourceLoader.getResource("CVC4Config.txt");
		processorFile=ResourceLoader.getResource("postproc.zip");
		Assert.assertNotNull(space1);
		Assert.assertNotNull(space2);
		
		
		downloadDir=ResourceLoader.getDownloadDirectory();
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), user.getId());
		config=ResourceLoader.loadConfigurationFileIntoDatabase("CVC4Config.txt", solver.getId());
		proc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, testCommunity.getId());
		Assert.assertNotNull(solver);

		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space1.getId(), user.getId());
		List<Integer> solverIds=new ArrayList<Integer>();
		solverIds.add(solver.getId());
		job=ResourceLoader.loadJobIntoDatabase(space1.getId(), user.getId(), -1, proc.getId(), solverIds, benchmarkIds,100,100,1);

		Assert.assertNotNull(benchmarkIds);

		
		solverURL=Util.url("public/resources/CVC4.zip");
	}
	
	

	@Override
	protected void teardown() throws Exception {
		
		Spaces.removeSubspaces(space1.getId(), testCommunity.getId(), user.getId());
		Spaces.removeSubspaces(space2.getId(), testCommunity.getId(), user.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		
		for (Integer i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		
		Jobs.deleteAndRemove(job.getId());
		
		Users.deleteUser(user2.getId(), Users.getAdmins().get(0).getId());
		
	}
	
	private boolean isErrorMap(HashMap<Integer,String> mapping) {
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
