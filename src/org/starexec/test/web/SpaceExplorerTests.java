package org.starexec.test.web;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

public class SpaceExplorerTests extends TestSequence {
	private Space space1=null; //will contain both solvers and benchmarks at the start of testing
	private Space space2=null;
	WebDriver driver=null;
	//List<Integer> benchmarkIds=null;

	Solver solver=null;
	//private Job job=null;

	
	User user=null;
	
	
	private String getSpaceToggleXPath(int spaceId) {
		return ".//*[@id='"+spaceId+"']/ins";
	}
	
	/**
	 * Expands the space tree such that the space with the given ID is visible
	 * @param spaceId
	 */
	private void expandSpaceTree(int spaceId, WebDriver driver) {
		List<Integer> spaceIds=Spaces.getChainToRoot(spaceId);
		if (spaceIds.size()==1) { //done, as only the root space is there and the root space must be visible
			return;
		}
		
		for (int index=1;index<spaceIds.size();index++) {
			int sId=spaceIds.get(index);
			List<WebElement> elements = driver.findElements(By.xpath(getSpaceToggleXPath(sId)));
			if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
				continue;
			} else {
				WebElement parentTag=driver.findElement(By.xpath(getSpaceToggleXPath(spaceIds.get(index-1))));
				parentTag.click();
				
			}

		}	
	}
	
	private void expandFieldset(WebDriver driver, String fieldsetId) throws InterruptedException {
		WebElement fieldSetLegend=driver.findElement(By.xpath("//*[@id='"+fieldsetId+"']/legend/span[2]"));
		if (fieldSetLegend.getText().contains("+")) {
			fieldSetLegend.click();
			Thread.sleep(800);
		}
	}
	
	// conducts a test of linking some type of primitive from space to space2
	private void linkPrimitiveTest(String fieldSet, String primType) throws InterruptedException {
		driver.get(Util.url("secure/explore/spaces.jsp"));
		expandSpaceTree(space1.getId(),driver);
		Thread.sleep(500);
		WebElement space1Link=driver.findElement(By.xpath(".//*[@id='"+space1.getId()+"']/a"));
		space1Link.click();
		Thread.sleep(500);
		expandFieldset(driver,fieldSet);

		WebElement space2Link=driver.findElement(By.xpath(".//*[@id='"+space2.getId()+"']/a"));

		WebElement primLink=driver.findElement(By.xpath(".//*[@id='"+primType+"']/tbody/tr/td[2]"));
		Actions actions=new Actions(driver);

		//actions.dragAndDrop(primLink, space2Link).perform();
		actions.clickAndHold(primLink).moveToElement(space2Link).release().perform();
		Thread.sleep(300);
		List<WebElement> dialogButtons=driver.findElements(By.xpath("//*[@class=\"ui-dialog-buttonset\"]/button/span"));
		for (WebElement e : dialogButtons) {
			if (e.getText().contains("link") || e.getText().contains("yes")) {
				e.click();
				break;
			}
		}
		Thread.sleep(2000);
	}
	
	@Test
	private void linkSolverTest() throws InterruptedException {
		Assert.assertEquals(1,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		linkPrimitiveTest("solverField","solvers");
		Assert.assertEquals(2,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		List<Integer>solvers=new ArrayList<Integer>();
		solvers.add(solver.getId());
		Spaces.removeSolvers(solvers, space2.getId());
	}
	@Test
	private void linkSolverTest2() throws InterruptedException {
		Assert.assertEquals(1,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		linkPrimitiveTest("solverField","solvers");
		Assert.assertEquals(2,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		List<Integer>solvers=new ArrayList<Integer>();
		solvers.add(solver.getId());
		Spaces.removeSolvers(solvers, space2.getId());
	}
	@Test
	private void linkSolverTest3() throws InterruptedException {
		Assert.assertEquals(1,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		linkPrimitiveTest("solverField","solvers");
		Assert.assertEquals(2,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		List<Integer>solvers=new ArrayList<Integer>();
		solvers.add(solver.getId());
		Spaces.removeSolvers(solvers, space2.getId());
	}
	@Test
	private void linkSolverTest4() throws InterruptedException {
		Assert.assertEquals(1,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		linkPrimitiveTest("solverField","solvers");
		Assert.assertEquals(2,Solvers.getAssociatedSpaceIds(solver.getId()).size());
		List<Integer>solvers=new ArrayList<Integer>();
		solvers.add(solver.getId());
		Spaces.removeSolvers(solvers, space2.getId());
	}
	
	
	@Test
	private void linkBenchmarkTest() throws InterruptedException {
		//int benchId=benchmarkIds.get(0);
		//Assert.assertEquals(1,Benchmarks.getAssociatedSpaceIds(benchId).size());
		//linkPrimitiveTest("benchField","benchmarks");
		//Assert.assertEquals(2,Benchmarks.getAssociatedSpaceIds(benchId).size());

	}
	
	@Test
	private void linkJobTest() throws InterruptedException {
		List<Job>jobs=Spaces.getDetails(space2.getId(), user.getId()).getJobs();
		Assert.assertEquals(0,jobs.size());
		linkPrimitiveTest("jobField","jobs");
		jobs=Spaces.getDetails(space2.getId(), user.getId()).getJobs();
		Assert.assertEquals(2,jobs.size());
	}
	
	@Override
	protected String getTestName() {
		return "SpaceExplorerTests";
	}

	@Override
	protected void setup() throws Exception {
		user=Users.getTestUser();
		driver=ResourceLoader.getWebDriver(user.getEmail(), R.TEST_USER_PASSWORD);
		//space1 will contain solvers and benchmarks
		space1=ResourceLoader.loadSpaceIntoDatabase(user.getId(),Communities.getTestCommunity().getId());
		space2=ResourceLoader.loadSpaceIntoDatabase(user.getId(),Communities.getTestCommunity().getId());		
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space1.getId(), user.getId());
		//benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip", space1.getId(), user.getId());

		//Solvers.associate(solver.getId(), space1.getId());
		//job=ResourceLoader.loadJobIntoDatabase(space1.getId(), user.getId(), -1, -1, solver.getId(), benchmarkIds,100,100,1);

	}

	@Override
	protected void teardown() throws Exception {
		//driver.quit();

		Spaces.removeSubspaces(space1.getId(), user.getId());
		Spaces.removeSubspaces(space2.getId(), user.getId());

		Solvers.deleteAndRemoveSolver(solver.getId());
		
		//for (Integer i : benchmarkIds) {
		//	Benchmarks.deleteAndRemoveBenchmark(i);
		//}
		//Jobs.deleteAndRemove(job.getId());

	}

}
