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

/*
 * TODO:
 * jobPanelView
 * pair
 * pairsInSpace
 * solverconfigs
 * uploadStatus
 */

public class GetPageTests extends TestSequence {
	private static final Logger log = Logger.getLogger(TestSequence.class);	
	private Connection con;
	private Space space1=null; //will contain both solvers and benchmarks
	private Job job=null;
	File solverFile=null;
	File downloadDir=null;

	Solver solver=null;
	List<Integer> benchmarkIds=null;
	Configuration config=null;
	Processor proc=null;
	
	User user=null;
	Space testCommunity=null;	
	
	@Test
	private void getSpaceExplorerTest(){
		Assert.assertTrue(con.canGetPage("secure/explore/spaces.jsp"));
	}
	
	@Test
	private void getCommunityExplorerTest(){
		Assert.assertTrue(con.canGetPage("secure/explore/communities.jsp"));
	}
	
	@Test
	private void getClusterTest(){
		Assert.assertTrue(con.canGetPage("secure/explore/cluster.jsp"));
	}
	
	@Test
	private void getSolverDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/solver.jsp?id="+solver.getId()));
	}
	
	@Test
	private void getSolverEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/solver.jsp?id="+solver.getId()));
	}
	
	@Test
	private void getBenchmarkDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/benchmark.jsp?id="+benchmarkIds.get(0)));
	}
	
	@Test
	private void getBenchmarkEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/benchmark.jsp?id="+benchmarkIds.get(0)));
	}
	
	@Test
	private void getConfigDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/configuration.jsp?id="+config.getId()));
	}
	
	@Test
	private void getSpaceEditTest() {
		Assert.assertTrue(con.canGetPage("secure/edit/space.jsp?id="+space1.getId()));
	}
	
	@Test
	private void getConfigEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/configuration.jsp?id="+config.getId()));
	}
	
	@Test
	private void getJobDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/job.jsp?id="+job.getId()));
	}
	
	@Test
	private void getJobEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/job.jsp?id="+job.getId()));
	}
	
	@Test
	private void getRecycleBinTest(){
		Assert.assertTrue(con.canGetPage("secure/details/recycleBin.jsp"));
	}
	
	@Test
	private void getUserDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/user.jsp?id="+user.getId()));
	}
	
	@Test
	private void getUserEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/user.jsp?id="+user.getId()));
	}
	
	@Test
	private void getHelpTest(){
		Assert.assertTrue(con.canGetPage("secure/help.jsp"));
	}
	
	@Test
	private void failBadURLTest(){
		Assert.assertFalse(con.canGetPage("secure/details/fakewebpage.jsp"));
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
		
		//space1 will contain solvers and benchmarks
		space1=ResourceLoader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());
		Assert.assertNotNull(space1);
		
		
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

		
	}
	
	

	@Override
	protected void teardown() throws Exception {
		
		Spaces.removeSubspaces(space1.getId(), testCommunity.getId(), user.getId());
		Solvers.delete(solver.getId());
		
		for (Integer i : benchmarkIds) {
			Benchmarks.delete(i);
		}
		
	}
	


	@Override
	protected String getTestName() {
		return "GetPageTests";
	}

}
