package org.starexec.test.web;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

/**
 * This class contains tests that simply visit web pages can click around on them,
 * searching for any javascript errors and also any JSP errors that prevent
 * pages from loading
 * @author Eric Burns
 *
 */

public class GetPageTests extends TestSequence {
	
	private Space space1=null; //will contain both solvers and benchmarks
	private Job job=null;
	
	File solverFile=null;
	File downloadDir=null;

	WebDriver driver=null;
	Actions driverActions;
	WebDriver adminDriver=null;
	
	Solver solver=null;
	List<Integer> benchmarkIds=null;
	Configuration config=null;
	Processor proc=null;
	
	User user=null;
	DefaultSettings settings=null;
	User admin=null;
	Space testCommunity=null;	
	Queue q=null;
	@Test
	private void getSpaceExplorerTest() throws InterruptedException{
		driver.get(Util.url("secure/explore/spaces.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
		WebElement treeIcon=driver.findElements(By.className("jstree-icon")).get(0);
		treeIcon.click();
		treeIcon.click();
		treeIcon.click();
		WebElement jobField=driver.findElement(By.id("jobExpd"));
		jobField.click();
		Thread.sleep(500); //wait for the field to open
		WebElement tableHeader=driver.findElement(By.id("jobNameHead"));
		tableHeader.click();
		tableHeader.click();
		
	}
	
	@Test
	private void getCommunityExplorerTest(){
		driver.get(Util.url("secure/explore/communities.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getClusterTest(){
		driver.get(Util.url("secure/explore/cluster.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getSolverDetailsTest(){
		driver.get(Util.url("secure/details/solver.jsp?id="+solver.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getSolverEditTest(){
		driver.get(Util.url("secure/edit/solver.jsp?id="+solver.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getSolverAddTest(){
		driver.get(Util.url("secure/add/solver.jsp?sid="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getBatchJobAddTest(){
		driver.get(Util.url("secure/add/batchJob.jsp?sid="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getBatchSpaceAddTest(){
		driver.get(Util.url("secure/add/batchSpace.jsp?sid="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getBenchmarkDetailsTest(){
		driver.get(Util.url("secure/details/benchmark.jsp?id="+benchmarkIds.get(0)));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getBenchmarkEditTest(){
		driver.get(Util.url("secure/edit/benchmark.jsp?id="+benchmarkIds.get(0)));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getBenchAddTest(){
		driver.get(Util.url("secure/add/benchmarks.jsp?sid="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getConfigDetailsTest(){
		driver.get(Util.url("secure/details/configuration.jsp?id="+config.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getConfigEditTest(){
		driver.get(Util.url("secure/edit/configuration.jsp?id="+config.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getConfigAddTest(){
		driver.get(Util.url("secure/add/configuration.jsp?sid="+solver.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getPictureAddTest(){
		driver.get(Util.url("secure/add/picture.jsp?type=solver&Id="+solver.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
		
	}
	
	@Test
	private void getSpaceEditTest() {
		driver.get(Util.url("secure/edit/space.jsp?id="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getJobDetailsTest(){
		driver.get(Util.url("secure/details/job.jsp?id="+job.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getJobPanelViewTest(){
		driver.get(Util.url("secure/details/jobPanelView.jsp?spaceid="+job.getPrimarySpace()+"&jobid="+job.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getPairsInSpaceTest(){
		driver.get(Util.url("secure/details/pairsInSpace.jsp?type=solved&configid="+job.getJobPairs().get(0).getConfiguration().getId()
				+"&sid="+job.getPrimarySpace()+"&id="+job.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getPairDetailsTest(){
		driver.get(Util.url("secure/details/pair.jsp?id="+job.getJobPairs().get(0).getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getJobAddTest(){
		driver.get(Util.url("secure/add/job.jsp?sid="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getQuickJobAddTest(){
		driver.get(Util.url("secure/add/quickJob.jsp?sid="+space1.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getRecycleBinTest(){
		driver.get(Util.url("secure/details/recycleBin.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getUserDetailsTest(){
		driver.get(Util.url("secure/details/user.jsp?id="+user.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getUserEditTest(){
		driver.get(Util.url("secure/edit/account.jsp?id="+user.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getDefaultPrimTest() {
		driver.get(Util.url("secure/edit/defaultPrimitive.jsp?type=solver&id="+settings.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
		driver.get(Util.url("secure/edit/defaultPrimitive.jsp?type=benchmark&id="+settings.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
		
		
		driver.get(Util.url("secure/edit/defaultPrimitive.jsp?type=wrong&id="+settings.getId()));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		driver.get(Util.url("secure/edit/defaultPrimitive.jsp?type=benchmark&id=-1"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		


	}
	
	@Test
	private void getHelpTest(){
		driver.get(Util.url("secure/help.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getAdminAssocCommunityTest() {
		adminDriver.get(Util.url("secure/admin/assocCommunity.jsp?id="+q.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/assocCommunity.jsp?id="+q.getId()));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	@Test
	private void getAdminCacheTest() {
		
		adminDriver.get(Util.url("secure/admin/cache.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/cache.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		
		

	}
	
	@Test
	private void getAdminClusterTest() {
		
		adminDriver.get(Util.url("secure/admin/cluster.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/cluster.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
	}
	
	@Test
	private void getAdminCommunityTest() {
		adminDriver.get(Util.url("secure/admin/community.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/community.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		
	

	}
	
	@Test
	private void getAdminJobTest() {
		adminDriver.get(Util.url("secure/admin/job.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/job.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	@Test
	private void getAdminLoggingTest() {
		adminDriver.get(Util.url("secure/admin/logging.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/logging.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	@Test
	private void getAdminMoveNodesTest() {
		
		adminDriver.get(Util.url("secure/admin/moveNodes.jsp?id="+q.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/moveNodes.jsp?id="+q.getId()));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
	

	}
	
	@Test
	private void getAdminPermanentQueueTest() {
		adminDriver.get(Util.url("secure/admin/permanentQueue.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/permanentQueue.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	@Test
	private void getAdminPermissionsTest() {
		adminDriver.get(Util.url("secure/admin/permissions.jsp?id="+user.getId()));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/permissions.jsp?id="+user.getId()));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	@Test
	private void getAdminStarexecTest() {
		adminDriver.get(Util.url("secure/admin/starexec.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/starexec.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		
		

	}
	
	@Test
	private void getAdminTestingTest() {
		adminDriver.get(Util.url("secure/admin/testing.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/testing.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	
	
	@Test
	private void getAdminUserTest() {
		adminDriver.get(Util.url("secure/admin/user.jsp"));
		Assert.assertFalse(TestUtil.isOnErrorPage(adminDriver));
		driver.get(Util.url("secure/admin/user.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
		

	}
	
	@Test
	private void failBadURLTest(){
		driver.get(Util.url("secure/details/fakewebpage.jsp"));
		Assert.assertTrue(TestUtil.isOnErrorPage(driver));
	}
	
	@Override
	protected void setup() throws Exception {
		user=Users.getTestUser();
		admin=Users.getAdmins().get(0);
		testCommunity=Communities.getTestCommunity();
		
		

		driver=ResourceLoader.getWebDriver(user.getEmail(), R.TEST_USER_PASSWORD);
		driverActions=new Actions(driver);
		adminDriver=ResourceLoader.getWebDriver(admin.getEmail(), R.TEST_USER_PASSWORD);

	
		
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
		
		Spaces.removeSubspaces(space1.getId(), admin.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		Processors.delete(proc.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		
		Jobs.deleteAndRemove(job.getId());
		Settings.deleteProfile(settings.getId());
		driver.quit();
		adminDriver.quit();
	}
	


	@Override
	protected String getTestName() {
		return "GetPageTests";
	}

}
