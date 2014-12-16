package org.starexec.test.web;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.Test;
import org.starexec.test.TestSequence;
import org.starexec.test.TestUtil;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

public class UploadBenchmarksTests extends TestSequence {
	WebDriver driver=null;
	User u=null;
	Space s=null;
	String benchmarkFilePath=null;
	
	
	//makes sure we do not navigate away from the page until javascript validation is passing
	@Test
	private void validationTest() {
		
		driver.get(Util.url("secure/add/benchmarks.jsp?sid="+s.getId()));
		String url=driver.getCurrentUrl();
		
        WebElement fileInput=driver.findElement(By.id("fileLoc"));
        fileInput.submit();
        Assert.assertTrue(driver.getCurrentUrl().equals(url));
        fileInput.sendKeys("badpath");
        fileInput.submit();
        Assert.assertTrue(driver.getCurrentUrl().equals(url));        
	}
	
	@Test
	private void uploadBenchmarksTest() {
		driver.get(Util.url("secure/add/benchmarks.jsp?sid="+s.getId()));
		String url=driver.getCurrentUrl();
		
        WebElement fileInput=driver.findElement(By.id("fileLoc"));
        
        fileInput.sendKeys(benchmarkFilePath);
        fileInput.submit();
        Assert.assertFalse(TestUtil.isOnErrorPage(driver));   
        Assert.assertTrue(driver.getCurrentUrl().contains("uploadStatus.jsp"));
	}
	
	
	
	@Override
	protected String getTestName() {
		return "UploadBenchmarksTest";
	}

	@Override
	protected void setup() throws Exception {
		u=ResourceLoader.loadUserIntoDatabase();
		s=ResourceLoader.loadSpaceIntoDatabase(u.getId(), Communities.getTestCommunity().getId());
		driver=ResourceLoader.getWebDriver(u.getEmail(), u.getPassword());
		benchmarkFilePath=ResourceLoader.getResource("benchmarks.zip").getAbsolutePath();
		
	}

	@Override
	protected void teardown() throws Exception {
		driver.quit();
		Spaces.removeSubspaces(s.getId());
		Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId());
		
		
		
	}

}
