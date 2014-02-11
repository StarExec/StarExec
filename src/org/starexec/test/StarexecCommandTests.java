package org.starexec.test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.junit.Assert;
import org.starexec.command.Connection;
import org.starexec.constants.R;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.util.Util;

import org.apache.log4j.Logger;
public class StarexecCommandTests extends TestSequence {
	private static final Logger log = Logger.getLogger(TestSequence.class);	
	private Connection con;
	private Space s=null;
	File solverFile=null;
	File benchmarkFile=null;
	public StarexecCommandTests() {
		setName("StarexecCommandTests");
	}
	
	@Test
	private void GetIDTest() throws Exception {
		int id=con.getUserID();
		if (id<0) {
			addMessage("User ID was incorrect");
			throw new Exception("failed");
		}
	}
	@Test
	private void ListSolversTest() {
		HashMap<Integer,String> mapping=con.getSolversInSpace(s.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListBenchmarksTest() {
		HashMap<Integer,String> mapping=con.getBenchmarksInSpace(s.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListJobsTest() {
		HashMap<Integer,String> mapping=con.getJobsInSpace(s.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListUsersTest() {
		
		HashMap<Integer,String> mapping=con.getUsersInSpace(s.getId());
		Assert.assertFalse(isErrorMap(mapping));
	}
	@Test
	private void ListSpacesTest() {
		HashMap<Integer,String> mapping=con.getSpacesInSpace(s.getId());
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
		addMessage("adding solver to space with id = "+s.getId());
		String name=TestUtil.getRandomSolverName();
		int result=con.uploadSolver(name, s.getId(), solverFile.getAbsolutePath(), true);
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
		int result=con.uploadBenchmarksToSingleSpace(benchmarkFile.getAbsolutePath(), 1, s.getId(), false);
		Assert.assertEquals(result,0);
	}
	
	@Override
	protected void setup() throws Exception {
		//this prevents the apache http libraries from logging things. Their logs are very prolific
		//and drown out ours
		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
		//TODO: remove the http thing
		con=new Connection("admin@uiowa.edu","Starexec4ever",Util.url("").replace("https", "http"));
		int status = con.login();
		Assert.assertEquals(0,status);
		s=new Space();
		s.setName(TestUtil.getRandomSpaceName());
		s.setDescription("test desc");
		int id=Spaces.add(s, 1, 1);
		s.setId(id);
		URL url=StarexecCommandTests.class.getResource("/org/starexec/test/resources/CVC4.zip");
		solverFile=new File(url.getFile());
		url =StarexecCommandTests.class.getResource("/org/starexec/test/resources/benchmarks.zip");
		benchmarkFile=new File(url.getFile());
		
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

}
