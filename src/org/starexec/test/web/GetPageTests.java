package org.starexec.test.web;

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
import org.starexec.data.database.Settings;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Job;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.StarexecTest;
import org.starexec.test.TestSequence;
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
	DefaultSettings settings=null;
	User admin=null;
	Space testCommunity=null;	
	Queue q=null;
	@StarexecTest
	private void getSpaceExplorerTest(){
		Assert.assertTrue(con.canGetPage("secure/explore/spaces.jsp"));
	}
	
	@StarexecTest
	private void getCommunityExplorerTest(){
		Assert.assertTrue(con.canGetPage("secure/explore/communities.jsp"));
	}
	
	@StarexecTest
	private void getClusterTest(){
		Assert.assertTrue(con.canGetPage("secure/explore/cluster.jsp"));
	}
	
	@StarexecTest
	private void getSolverDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/solver.jsp?id="+solver.getId()));
	}
	
	@StarexecTest
	private void getSolverEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/solver.jsp?id="+solver.getId()));
	}
	
	@StarexecTest
	private void getSolverAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/solver.jsp?sid="+space1.getId()));
	}
	
	@StarexecTest
	private void getBatchJobAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/batchJob.jsp?sid="+space1.getId()));
	}
	
	@StarexecTest
	private void getBatchSpaceAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/batchSpace.jsp?sid="+space1.getId()));
	}
	
	@StarexecTest
	private void getBenchmarkDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/benchmark.jsp?id="+benchmarkIds.get(0)));
	}
	
	@StarexecTest
	private void getBenchmarkEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/benchmark.jsp?id="+benchmarkIds.get(0)));
	}
	
	@StarexecTest
	private void getBenchAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/benchmarks.jsp?sid="+space1.getId()));
	}
	
	@StarexecTest
	private void getConfigDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/configuration.jsp?id="+config.getId()));
	}
	
	@StarexecTest
	private void getConfigEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/configuration.jsp?id="+config.getId()));
	}
	
	@StarexecTest
	private void getConfigAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/configuration.jsp?sid="+solver.getId()));
	}
	
	@StarexecTest
	private void getPictureAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/picture.jsp?type=solver&Id="+solver.getId()));
	}
	
	@StarexecTest
	private void getSpaceEditTest() {
		Assert.assertTrue(con.canGetPage("secure/edit/space.jsp?id="+space1.getId()));
	}
	
	@StarexecTest
	private void getJobDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/job.jsp?id="+job.getId()));
	}
	
	@StarexecTest
	private void getJobPanelViewTest(){
		Assert.assertTrue(con.canGetPage("secure/details/jobPanelView.jsp?spaceid="+job.getPrimarySpace()+"&jobid="+job.getId()));
	}
	
	@StarexecTest
	private void getPairsInSpaceTest(){
		Assert.assertTrue(con.canGetPage("secure/details/pairsInSpace.jsp?type=solved&configid="+job.getJobPairs().get(0).getPrimaryStage().getConfiguration().getId()
				+"&sid="+job.getPrimarySpace()+"&id="+job.getId()));
	}
	
	@StarexecTest
	private void getPairDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/pair.jsp?id="+job.getJobPairs().get(0).getId()));
	}
	
	@StarexecTest
	private void getJobAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/job.jsp?sid="+space1.getId()));
	}
	
	@StarexecTest
	private void getQuickJobAddTest(){
		Assert.assertTrue(con.canGetPage("secure/add/quickJob.jsp?sid="+space1.getId()));
	}
	
	@StarexecTest
	private void getRecycleBinTest(){
		Assert.assertTrue(con.canGetPage("secure/details/recycleBin.jsp"));
	}
	
	@StarexecTest
	private void getUserDetailsTest(){
		Assert.assertTrue(con.canGetPage("secure/details/user.jsp?id="+user.getId()));
	}
	
	@StarexecTest
	private void getUserEditTest(){
		Assert.assertTrue(con.canGetPage("secure/edit/account.jsp?id="+user.getId()));
	}
	
	@StarexecTest
	private void getDefaultPrimTest() {
		Assert.assertTrue(con.canGetPage("secure/edit/defaultPrimitive.jsp?type=solver&id="+settings.getId()));
		Assert.assertTrue(con.canGetPage("secure/edit/defaultPrimitive.jsp?type=benchmark&id="+settings.getId()));
		
		Assert.assertFalse(con.canGetPage("secure/edit/defaultPrimitive.jsp?type=wrong&id="+settings.getId()));
		Assert.assertFalse(con.canGetPage("secure/edit/defaultPrimitive.jsp?type=benchmark&id=-1"));


	}
	
	@StarexecTest
	private void getHelpTest(){
		Assert.assertTrue(con.canGetPage("secure/help.jsp"));
	}
	
	@StarexecTest
	private void getAdminAssocCommunityTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/assocCommunity.jsp?id="+q.getId()));
		Assert.assertFalse(con.canGetPage("secure/admin/assocCommunity.jsp?id="+q.getId()));

	}
	
	@StarexecTest
	private void getAdminCacheTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/cache.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/cache.jsp"));

	}
	
	@StarexecTest
	private void getAdminClusterTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/cluster.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/cluster.jsp"));

	}
	
	@StarexecTest
	private void getAdminCommunityTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/community.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/community.jsp"));

	}
	
	@StarexecTest
	private void getAdminJobTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/job.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/job.jsp"));

	}
	
	@StarexecTest
	private void getAdminLoggingTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/logging.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/logging.jsp"));

	}
	
	@StarexecTest
	private void getAdminMoveNodesTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/moveNodes.jsp?id="+q.getId()));
		Assert.assertFalse(con.canGetPage("secure/admin/moveNodes.jsp?id="+q.getId()));

	}
	
	@StarexecTest
	private void getAdminNodesTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/nodes.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/nodes.jsp"));

	}
	
	@StarexecTest
	private void getAdminPermanentQueueTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/permanentQueue.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/permanentQueue.jsp"));

	}
	
	@StarexecTest
	private void getAdminPermissionsTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/permissions.jsp?id="+user.getId()));
		Assert.assertFalse(con.canGetPage("secure/admin/permissions.jsp?id="+user.getId()));

	}
	
	@StarexecTest
	private void getAdminStarexecTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/starexec.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/starexec.jsp"));

	}
	
	@StarexecTest
	private void getAdminTestingTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/test.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/test.jsp"));

	}
	
	
	
	@StarexecTest
	private void getAdminUserTest() {
		Assert.assertTrue(adminCon.canGetPage("secure/admin/user.jsp"));
		Assert.assertFalse(con.canGetPage("secure/admin/user.jsp"));

	}
	
	@StarexecTest
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

		int stat = con.login();
		Assert.assertEquals(0,stat);
		
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
		settings=ResourceLoader.loadDefaultSettingsProfileIntoDatabase(user.getId());
		Assert.assertNotNull(benchmarkIds);

		
	}
	
	

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspaces(space1.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		Processors.delete(proc.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		
		Jobs.deleteAndRemove(job.getId());
		Settings.deleteProfile(settings.getId());
	}
	


	@Override
	protected String getTestName() {
		return "GetPageTests";
	}

}
