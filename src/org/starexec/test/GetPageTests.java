package org.starexec.test;

import java.io.File;
import java.util.ArrayList;
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
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;


public class GetPageTests extends TestSequence {
	private Connection con; // connection of a normal user
	private Connection adminCon;
	private Space space1=null; //will contain both solvers and benchmarks
	private Job job=null;
	File solverFile=null;
	File downloadDir=null;

	Solver solver=null;
	List<Integer> benchmarkIds=null;
	Configuration config=null;
	Processor proc=null;
	
	User user=null;
	User admin=null;
	Space testCommunity=null;	
	Queue q=null;
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
	private void getSolverAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/solver.jsp?sid="+space1.getId()));
	}
	
	@Test
	private void getBatchJobAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/batchJob.jsp?sid="+space1.getId()));
	}
	
	@Test
	private void getBatchSpaceAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/batchSpace.jsp?sid="+space1.getId()));
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
	private void getBenchAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/benchmarks.jsp?sid="+space1.getId()));
	}
	
	@Test
	private void getConfigDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/configuration.jsp?id="+config.getId()));
	}
	
	@Test
	private void getConfigEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/configuration.jsp?id="+config.getId()));
	}
	
	@Test
	private void getConfigAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/configuration.jsp?sid="+solver.getId()));
	}
	
	@Test
	private void getPictureAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/picture.jsp?type=solver&Id="+solver.getId()));
	}
	
	@Test
	private void getSpaceEditTest() {
		Assert.assertTrue(con.canGetPage("secure/edit/space.jsp?id="+space1.getId()));
	}
	
	@Test
	private void getJobDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/job.jsp?id="+job.getId()));
	}
	
	@Test
	private void getJobPanelViewTest(){
		Assert.assertTrue(con.canGetPage("secure/details/jobPanelView.jsp?spaceid="+job.getPrimarySpace()+"&jobid="+job.getId()));
	}
	
	@Test
	private void getPairsInSpaceTest(){
		Assert.assertTrue(con.canGetPage("secure/details/pairsInSpace.jsp?type=solved&configid="+job.getJobPairs().get(0).getConfiguration().getId()
				+"&sid="+job.getPrimarySpace()+"&id="+job.getId()));
	}
	
	@Test
	private void getPairDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/pair.jsp?id="+job.getJobPairs().get(0).getId()));
	}
	
	@Test
	private void getJobAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/job.jsp?sid="+space1.getId()));
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
		Assert.assertTrue(con.canGetPage("secure/edit/account.jsp?id="+user.getId()));
	}
	
	@Test
	private void getHelpTest(){
		Assert.assertTrue(con.canGetPage("secure/help.jsp"));
	}
	
	@Test
	private void getAdminAssocCommunityTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/assocCommunity.jsp?id="+q.getId()));
		Assert.assertFalse(con.canGetPage("secure/admin/assocCommunity.jsp?id="+q.getId()));

	}
	
	@Test
	private void getAdminCacheTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/cache.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/cache.jsp"));

	}
	
	@Test
	private void getAdminClusterTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/cluster.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/cluster.jsp"));

	}
	
	@Test
	private void getAdminCommunityTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/community.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/community.jsp"));

	}
	
	@Test
	private void getAdminJobTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/job.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/job.jsp"));

	}
	
	@Test
	private void getAdminLoggingTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/logging.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/logging.jsp"));

	}
	
	@Test
	private void getAdminMoveNodesTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/moveNodes.jsp?id="+q.getId()));
		Assert.assertFalse(con.canGetPage("secure/admin/moveNodes.jsp?id="+q.getId()));

	}
	
	@Test
	private void getAdminNodesTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/nodes.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/nodes.jsp"));

	}
	
	@Test
	private void getAdminPermanentQueueTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/permanentQueue.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/permanentQueue.jsp"));

	}
	
	@Test
	private void getAdminPermissionsTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/permissions.jsp?id="+user.getId()));
		Assert.assertFalse(con.canGetPage("secure/admin/permissions.jsp?id="+user.getId()));

	}
	
	@Test
	private void getAdminStarexecTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/starexec.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/starexec.jsp"));

	}
	
	@Test
	private void getAdminTestingTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/test.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/test.jsp"));

	}
	
	@Test
	private void getAdminUserTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/user.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/user.jsp"));

	}
	
	@Test
	private void failBadURLTest(){
		Assert.assertFalse(con.canGetPage("secure/details/fakewebpage.jsp"));
	}
	
	@Override
	protected void setup() throws Exception {
		user=Users.getTestUser();
		admin=Users.getAdmins().get(0);
		testCommunity=Communities.getTestCommunity();
		
		//this prevents the apache http libraries from logging things. Their logs are very prolific
		//and drown out ours
		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);

		con=new Connection(user.getEmail(),R.TEST_USER_PASSWORD,Util.url(""));
		adminCon=new Connection(admin.getEmail(),R.TEST_USER_PASSWORD,Util.url(""));

		int status = con.login();
		Assert.assertEquals(0,status);
		
		//space1 will contain solvers and benchmarks
		space1=ResourceLoader.loadSpaceIntoDatabase(user.getId(),testCommunity.getId());
		Assert.assertNotNull(space1);
		
		q=Queues.getAll().get(0);
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
		
		Spaces.removeSubspaces(space1.getId(), admin.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		Processors.delete(proc.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		
		Jobs.deleteAndRemove(job.getId());
	}
	


	@Override
	protected String getTestName() {
		return "GetPageTests";
	}

}
