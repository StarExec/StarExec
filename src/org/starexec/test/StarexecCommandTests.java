package org.starexec.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.starexec.command.Connection;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;


/*TODO:
	CreateJobTest
	Delete[jobs, processors, configurations]
	Download[jobs, processors]
*/
public class StarexecCommandTests extends TestSequence {
	private static final Logger log = Logger.getLogger(TestSequence.class);	
	private Connection con;
	private Space space1=null;
	private Space space2=null;
	File solverFile=null;
	File downloadDir=null;
	File benchmarkFile=null;
	Solver solver=null;
	List<Integer> benchmarkIds=null;
	
	User user=null;
	Space testCommunity=null;
	Space space=null;
	
	
	String solverURL=null;
	
	@Test
	private void GetIDTest() throws Exception {
		int id=con.getUserID();
		Assert.assertEquals(user.getId(), id);
		
	}
	@Test
	private void ListSolversTest() {
		HashMap<Integer,String> mapping=con.getSolversInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListBenchmarksTest() {
		HashMap<Integer,String> mapping=con.getBenchmarksInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListJobsTest() {
		HashMap<Integer,String> mapping=con.getJobsInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListUsersTest() {
		HashMap<Integer,String> mapping=con.getUsersInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListSpacesTest() {
		HashMap<Integer,String> mapping=con.getSpacesInSpace(space1.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListSolversByUser() {
		HashMap<Integer,String> mapping=con.getSolversByUser();
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListJobsByUser() {
		HashMap<Integer,String> mapping=con.getJobsByUser();
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListBenchmarksByUser() {
		HashMap<Integer,String> mapping=con.getBenchmarksByUser();
		Assert.assertFalse(isErrorMap(mapping));
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
			
		} else {
			throw new Exception("an error code was returned "+result);
		}
	}
	
	@Test
	private void uploadBenchmarks() throws Exception {
		
		//we are putting benchmarks in a new space to avoid name collisions
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), 1, tempSpace.getId(), false);
		Assert.assertEquals(0,result);
		Assert.assertTrue(Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId()));
	}
	/*
	@Test
	private void uploadBenchmarksFromURL() {
		
		//we are putting the benchmarks in a new space to avoid name collisions
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
	
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), 1, tempSpace.getId(), false);
		Assert.assertEquals(0,result);
		
		Assert.assertTrue(Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId()));

	}*/
	
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
		int status=con.copySolvers(solverArr, space1.getId(), space2.getId(), false);
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
			Assert.assertTrue(Solvers.delete(i));
		}
	}
	
	@Test 
	private void  copyBenchmarkTest() {
		Integer[] benchArr=new Integer[2];
		benchArr[0]=benchmarkIds.get(0);
		benchArr[1]=benchmarkIds.get(1);
		Benchmark b1=Benchmarks.get(benchmarkIds.get(0));
		Benchmark b2=Benchmarks.get(benchmarkIds.get(1));

		int status=con.copyBenchmarks(benchArr, space1.getId(), space2.getId());
		Assert.assertEquals(0, status);
		
		HashMap<Integer,String> benches=con.getBenchmarksInSpace(space2.getId());
		
		//the name is very long and random, so the only way the given benchmark name will be in the 
		//second space will be if it was copied successfully
		Assert.assertTrue(benches.containsValue(b1.getName()));
		Assert.assertTrue(benches.containsValue(b2.getName()));
		
		Assert.assertFalse(benches.containsKey(b1.getId())); //the ID should NOT be the same, since the copy should be new
		Assert.assertFalse(benches.containsKey(b2.getId())); 
		
		
		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> benchIds =new ArrayList<Integer>();
		benchIds.addAll(benches.keySet());
		
		Assert.assertTrue(Spaces.removeBenches(benchIds, space2.getId()));
		for (Integer i : benchIds) {
			Assert.assertTrue(Benchmarks.delete(i));
		}
		
	}
	
	@Test 
	private void  linkBenchmarkTest() {
		Integer[] benchArr=new Integer[2];
		benchArr[0]=benchmarkIds.get(0);
		benchArr[1]=benchmarkIds.get(1);
		Benchmark b1=Benchmarks.get(benchmarkIds.get(0));
		Benchmark b2=Benchmarks.get(benchmarkIds.get(1));

		int status=con.copyBenchmarks(benchArr, space1.getId(), space2.getId());
		Assert.assertEquals(0, status);
		
		HashMap<Integer,String> benches=con.getBenchmarksInSpace(space2.getId());
		
		//the name is very long and random, so the only way the given benchmark name will be in the 
		//second space will be if it was copied successfully
		Assert.assertTrue(benches.containsValue(b1.getName()));
		Assert.assertTrue(benches.containsValue(b2.getName()));
		
		Assert.assertTrue(benches.containsKey(b1.getId())); 
		Assert.assertTrue(benches.containsKey(b2.getId())); 
		
		
		//remove all the solvers from space2 to ensure they don't interfere with upcoming tests
		List<Integer> benchIds =new ArrayList<Integer>();
		benchIds.addAll(benches.keySet());
		
		Assert.assertTrue(Spaces.removeBenches(benchIds, space2.getId()));
		
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
		Integer[] ids=new Integer[1];
		ids[0]=tempSolver.getId();
		Assert.assertEquals(0,con.deleteSolvers(ids));
		Assert.assertNull(Solvers.get(tempSolver.getId()));
	}
	
	@Test
	private void deleteBenchmarksTest() {
		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",testCommunity.getId(),user.getId());
		Assert.assertNotNull(Benchmarks.get(ids.get(0)));
		Assert.assertNotNull(Benchmarks.get(ids.get(1)));
		
		Integer[] idArr=new Integer[2];
		idArr[0]=ids.get(0);
		idArr[1]=ids.get(1);
		Assert.assertEquals(0,con.deleteBenchmarks(idArr));
		
		Assert.assertNull(Benchmarks.get(ids.get(0)));
		Assert.assertNull(Benchmarks.get(ids.get(1)));
		
	}
	
	@Test
	private void copySpaceTest() {
		Integer[] spaceArr=new Integer[1];
		List<Space> before=Spaces.getSubSpaces(space2.getId(), user.getId(), false);
		spaceArr[0]=space1.getId();
		int status=con.copySpaces(spaceArr, Spaces.getParentSpace(space1.getId()), space2.getId(), false);
		Assert.assertEquals(0, status);
		List<Space> after=Spaces.getSubSpaces(space2.getId(), user.getId(), false);
		Assert.assertTrue(after.size()>before.size());
		
	}
	
	@Test
	private void downloadSolverTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getResourcePath(),fileName);
		int status=con.downloadSolver(solver.getId(), downloadDir.getAbsolutePath());
		Assert.assertEquals(0, status);
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void downloadBenchmarkTest() {
		String fileName=TestUtil.getRandomSolverName()+".zip";
		File downloadDir=new File(ResourceLoader.getResourcePath(),fileName);
		int status=con.downloadBenchmark(benchmarkIds.get(0), downloadDir.getAbsolutePath());
		Assert.assertEquals(0, status);
		Assert.assertTrue(downloadDir.exists());
	}
	
	@Test
	private void removeSolversTest() {
		Solver temp=ResourceLoader.loadSolverIntoDatabase("CVC4.zip",testCommunity.getId(),user.getId());
		Assert.assertTrue(Solvers.getAssociatedSpaceIds(temp.getId()).contains(testCommunity.getId()));
		
		Integer[] id=new Integer[1];
		id[0]=temp.getId();
		Assert.assertEquals(0,con.removeSolvers(id, testCommunity.getId()));
		
		//first, ensure the solver is still there. It should NOT have been deleted
		Assert.assertNotNull(Solvers.get(temp.getId()));
		//then, make sure it is not in the space
		Assert.assertFalse(Solvers.getAssociatedSpaceIds(temp.getId()).contains(testCommunity.getId()));
		Solvers.delete(temp.getId());
	}
	
	@Test
	private void removeBenchmarksTest() {
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());
		List<Integer> ids=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", tempSpace.getId(), user.getId());
		Assert.assertTrue(Benchmarks.getAssociatedSpaceIds(ids.get(0)).contains(tempSpace.getId()));
		Integer[] id=new Integer[1];
		id[0]=ids.get(0);
		
		Assert.assertEquals(0, con.removeBenchmarks(id, tempSpace.getId()));
		Assert.assertNotNull(Benchmarks.get(ids.get(0)));
		Assert.assertFalse(Benchmarks.getAssociatedSpaceIds(ids.get(0)).contains(tempSpace.getId()));
		
		for (Integer i : ids) {
			Benchmarks.delete(i);
		}
		Spaces.removeSubspaces(tempSpace.getId(), testCommunity.getId(), user.getId());
	}
	
	@Test
	private void removeSpacesTest() {
		Space tempSpace=ResourceLoader.loadSpaceIntoDatabase(user.getId(), testCommunity.getId());

		Integer[] id= new Integer[1];
		id[0]=tempSpace.getId();
		Assert.assertNotNull(Spaces.getName(tempSpace.getId()));
		Assert.assertEquals(0,con.removeSubspace(id,testCommunity.getId(),false));
		Assert.assertNull(Spaces.getName(tempSpace.getId()));

	}
	
	
	@Override
	protected void setup() throws Exception {
		user=Users.getTestUser();
		
		testCommunity=Communities.getTestCommunity();
		
		//this prevents the apache http libraries from logging things. Their logs are very prolific
		//and drown out ours
		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);

		con=new Connection(user.getEmail(),R.TEST_USER_PASSWORD,Util.url(""));
		int status = con.login();
		Assert.assertEquals(0,status);
		
		//space1 will contain solvers and becnhmarks
		space1=ResourceLoader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());		
		solverFile=ResourceLoader.getResource("CVC4.zip");
		benchmarkFile=ResourceLoader.getResource("benchmarks.zip");
		Assert.assertNotNull(space1);
		Assert.assertNotNull(space2);
		
		
		downloadDir=ResourceLoader.getDownloadDirectory();
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), user.getId());
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space1.getId(), user.getId());
		Assert.assertNotNull(solver);
		
		Assert.assertNotNull(benchmarkIds);
		solverURL=Util.url("public/resources/CVC4.zip");
	}

	@Override
	protected void teardown() throws Exception {
		con.logout();
		Spaces.removeSubspaces(space1.getId(), testCommunity.getId(), user.getId());
		Spaces.removeSubspaces(space2.getId(), testCommunity.getId(), user.getId());
		Solvers.delete(solver.getId());
		
		for (Integer i : benchmarkIds) {
			Benchmarks.delete(i);
		}
		
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
