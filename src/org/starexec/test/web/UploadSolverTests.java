package org.starexec.test.web;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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

public class UploadSolverTests extends TestSequence {
	WebDriver driver=null;
	User u=null;
	Space s=null;
	String solverFilePath=null;
	
	@Test
	private void uploadSolverTest() {
		driver.get(Util.url("secure/add/solver.jsp?sid="+s.getId()));
		
        WebElement solverName=driver.findElement(By.name("sn"));
        WebElement solverDesc=driver.findElement(By.id("description"));
        WebElement textRadio=driver.findElement(By.id("radioText"));
        textRadio.click();
        
        WebElement localRadio=driver.findElement(By.id("radioLocal"));
        localRadio.click();
        WebElement solverFile=driver.findElement(By.id("fileLoc"));
        
        solverName.sendKeys(TestUtil.getRandomSolverName());
        solverDesc.sendKeys(TestUtil.getRandomSolverName());
        solverFile.sendKeys(solverFilePath);
        
        solverName.submit();

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
		Spaces.removeSubspaces(s.getId(), u.getId());
		Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId());
		//driver.quit();
		
	}
	
	
}
