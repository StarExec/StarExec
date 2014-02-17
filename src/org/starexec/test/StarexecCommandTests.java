package org.starexec.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.starexec.command.Connection;
import org.starexec.constants.R;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

import org.apache.log4j.Logger;
public class StarexecCommandTests extends TestSequence {
	private static final Logger log = Logger.getLogger(TestSequence.class);	
	private Connection con;
	private Space space1=null;
	private Space space2=null;
	File solverFile=null;
	File resourcesDir=null;
	File benchmarkFile=null;
	Solver solver=null;
	User testUser=null;
	
	@Test
	private void GetIDTest() throws Exception {
		int id=con.getUserID();
		Assert.assertEquals(testUser.getId(), id);
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
	private void uploadBenchmarks() throws Exception {
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), 1, space1.getId(), false);
		Assert.assertEquals(0,result);
	}
	@Test 
	private void downloadSpaceXML() {
		File xmlFile=new File(resourcesDir,TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(xmlFile.exists());
		int result=con.downloadSpaceXML(space1.getId(), xmlFile.getAbsolutePath());
		Assert.assertEquals(0, result);
		Assert.assertTrue(xmlFile.exists());
	}
	@Test 
	private void downloadSpace() {
		File spaceFile=new File(resourcesDir,TestUtil.getRandomSolverName()+".zip");
		Assert.assertFalse(spaceFile.exists());
		int result=con.downloadSpace(space1.getId(), spaceFile.getAbsolutePath(), false, false);
		Assert.assertEquals(0, result);
		Assert.assertTrue(spaceFile.exists());
	}
	@Test 
	private void setFirstNameTest() {
		String fname=testUser.getFirstName();
		int result=con.setFirstName(fname+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(fname+"a", Users.get(testUser.getId()).getFirstName()); //ensure the database reflects the change
		Users.updateFirstName(testUser.getId(), fname); //change the name back just to keep things consistent
	}
	@Test 
	private void setLastNameTest() {
		String lname=testUser.getLastName();
		int result=con.setLastName(lname+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(lname+"a", Users.get(testUser.getId()).getLastName()); //ensure the database reflects the change
		Users.updateLastName(testUser.getId(), lname); //change the name back just to keep things consistent
	}
	@Test 
	private void setInstitutionTest() {
		String inst=testUser.getInstitution();
		int result=con.setInstitution(inst+"a");
		Assert.assertEquals(0, result); //ensure StarexecCommand thinks it was successful
		Assert.assertEquals(inst+"a", Users.get(testUser.getId()).getInstitution()); //ensure the database reflects the change
		Users.updateInstitution(testUser.getId(), inst); //change the institution back just to keep things consistent
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
		Assert.assertTrue(Spaces.removeSolvers(solverIds, space2.getId()));	
	}
	@Test
	private void copySpaceTest() {
		Integer[] spaceArr=new Integer[1];
		List<Space> before=Spaces.getSubSpaces(space2.getId(), testUser.getId(), false);
		spaceArr[0]=space1.getId();
		int status=con.copySpaces(spaceArr, Spaces.getParentSpace(space1.getId()), space2.getId(), false);
		Assert.assertEquals(0, status);
		List<Space> after=Spaces.getSubSpaces(space2.getId(), testUser.getId(), false);
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
	
	@Override
	protected void setup() throws Exception {
		//this prevents the apache http libraries from logging things. Their logs are very prolific
		//and drown out ours
		
		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
		log.debug("the url is "+Util.url(""));
		log.debug("the email address is "+testUser.getEmail());
		log.debug("the password is "+R.TEST_USER_PASSWORD);
		con=new Connection(testUser.getEmail(),R.TEST_USER_PASSWORD,Util.url(""));
		int status = con.login();
		Assert.assertEquals(0,status);
		space1=ResourceLoader.loadSpaceIntoDatabase(testUser.getId(),testCommunity.getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(testUser.getId(),testCommunity.getId());		
		solverFile=ResourceLoader.getResource("CVC4.zip");
		benchmarkFile=ResourceLoader.getResource("benchmarks.zip");
		Assert.assertNotNull(space1);
		Assert.assertNotNull(space2);
		resourcesDir=solverFile.getParentFile();
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), testUser.getId());
		Assert.assertNotNull(solver);
	}

	@Override
	protected void teardown() throws Exception {
		con.logout();

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
