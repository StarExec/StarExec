package org.starexec.test.web;


import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.StarexecTest;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

import com.opera.core.systems.OperaDriver;

public class UploadSolverTests extends TestSequence {
	WebDriver driver=null;
	User u=null;
	Space s=null;
	String solverFilePath=null;
	
	
	//makes sure we do not navigate away from the page until javascript validation is passing
	@StarexecTest
	private void validationTest() {
		
		driver.get(Util.url("secure/add/solver.jsp?sid="+s.getId()));
		String url=driver.getCurrentUrl();
		WebElement solverName=driver.findElement(By.name("sn"));
        WebElement solverDesc=driver.findElement(By.id("description"));
        WebElement textRadio=driver.findElement(By.id("radioText"));
        textRadio.click();
        
        WebElement localRadio=driver.findElement(By.id("radioLocal"));
        localRadio.click();
        WebElement solverFile=driver.findElement(By.id("fileLoc"));
        
        solverName.submit(); //should fail because we haven't entered anything into the text fields
        Assert.assertTrue(driver.getCurrentUrl().equals(url));
        solverName.sendKeys("test");
        solverName.submit();
        Assert.assertTrue(driver.getCurrentUrl().equals(url));
        solverFile.sendKeys("badpath");
        solverName.submit();
        Assert.assertTrue(driver.getCurrentUrl().equals(url));
        solverFile.sendKeys(solverFilePath);
        solverName.clear();
        solverName.submit();
        Assert.assertTrue(driver.getCurrentUrl().equals(url));
        solverDesc.sendKeys("anything");
        solverName.submit();
        Assert.assertTrue(driver.getCurrentUrl().equals(url));
	}
	//makes sure we can upload a solver correctly
	@StarexecTest
	private void uploadSolverTest() throws InterruptedException {
		driver.get(Util.url("secure/add/solver.jsp?sid="+s.getId()));
		
        WebElement solverName=driver.findElement(By.name("sn"));
        WebElement solverDesc=driver.findElement(By.id("description"));
        WebElement textRadio=driver.findElement(By.id("radioText"));
        textRadio.click();
		Thread.sleep(2000); //need to wait for the description input to become visible
        WebElement localRadio=driver.findElement(By.id("radioLocal"));
        localRadio.click();
        WebElement solverFile=driver.findElement(By.id("fileLoc"));
        
        solverName.sendKeys(TestUtil.getRandomSolverName());
        solverDesc.sendKeys(TestUtil.getRandomSolverName());
        solverFile.sendKeys(solverFilePath);
        
        solverName.submit();
        Assert.assertFalse(TestUtil.isOnErrorPage(driver));
        //we should have been redirected to the solver details page
        Assert.assertTrue(driver.getCurrentUrl().contains("details/solver.jsp"));

	}
	@Override
	protected String getTestName() {
		return "UploadSolverTests";
	}

	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		s=ResourceLoader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());
		driver=ResourceLoader.getWebDriver(u.getEmail(), u.getPassword());
		solverFilePath=ResourceLoader.getResource("CVC4.zip").getAbsolutePath();
	}

	@Override
	protected void teardown() throws Exception {
		driver.quit();
		Spaces.removeSubspaces(s.getId());
		Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId());
		
	}
	
	
}
