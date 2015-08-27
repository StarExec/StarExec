package org.starexec.test.web;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.test.StarexecTest;
import org.starexec.test.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

public class LoginTests extends TestSequence {
	User u=null;
	Space s=null;
	@StarexecTest
	private void loginSuccess() {
		// Create a new instance of the html unit driver
        // Notice that the remainder of the code relies on the interface, 
        // not the implementation.
		//WebDriver driver=new FirefoxDriver();

        HtmlUnitDriver driver = new HtmlUnitDriver(true);
       
        driver.get(Util.url("secure/index.jsp"));
        System.out.println(driver.getCurrentUrl());
        WebElement userName=driver.findElement(By.name("j_username"));
        userName.sendKeys("test@uiowa.edu");
        driver.findElement(By.name("j_password")).sendKeys("Starexec4ever");
        
        userName.submit();
        
        Assert.assertTrue(driver.getCurrentUrl().contains("spaces.jsp"));
        
        driver.quit();
        
	}
	
	@StarexecTest
	private void loginFail() {
		// Create a new instance of the html unit driver
        // Notice that the remainder of the code relies on the interface, 
        // not the implementation.
		//WebDriver driver=new FirefoxDriver();
        HtmlUnitDriver driver = new HtmlUnitDriver(true);
        
        driver.get(Util.url("secure/index.jsp"));
        System.out.println(driver.getCurrentUrl());
        WebElement userName=driver.findElement(By.name("j_username"));
        userName.sendKeys("gibberish");
        driver.findElement(By.name("j_password")).sendKeys("badpass");
        
        userName.submit();
        
        Assert.assertTrue(driver.getCurrentUrl().contains("j_security_check"));
        
        driver.quit();
	}
	
	
	@Override
	protected String getTestName() {
		return "LoginTests";
	}

	@Override
	protected void setup() throws Exception {
		
		u=ResourceLoader.loadUserIntoDatabase();
		s=ResourceLoader.loadSpaceIntoDatabase(u.getId(),Communities.getTestCommunity().getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		Spaces.removeSubspaces(s.getId());
		Users.deleteUser(u.getId(), Users.getAdmins().get(0).getId());
		
	}

}
