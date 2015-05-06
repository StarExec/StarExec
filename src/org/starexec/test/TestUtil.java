package org.starexec.test;

import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.starexec.constants.R;
public class TestUtil {
	private static String[] letters={"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
	private static Random rnd=new Random();
	
	/**
	 * produces a random name of the maximum length for a new space. Useful for testing
	 * @return
	 */
	public static String getRandomSpaceName() {
		return getRandomAlphaString(32); // longer space names are a bit harder to deal with
	}
	public static String getRandomQueueName() {
		return getRandomAlphaString(R.QUEUE_NAME_LEN-5)+".q";
	}
	public static String getRandomPassword() {
		return getRandomAlphaString(R.PASSWORD_LEN-1);
	}
	
	public static String getRandomSolverName() {
		return getRandomAlphaString(R.SOLVER_NAME_LEN-1);
	}
	
	public static String getRandomJobName() {
		return getRandomAlphaString(R.JOB_NAME_LEN-1);
	}
	
	public static String getRandomUserName() {
		return getRandomAlphaString(R.USER_FIRST_LEN-1);
	}
	
	public static String getRandomEmail() {
		return getRandomAlphaString(R.EMAIL_LEN-10)+"@test.edu";
	}
	
	/**
	 * Gets a random string of lowercase letters of the given size
	 * @param length
	 * @return
	 */
	public static String getRandomAlphaString(int length) {
		String name="";
		while (length>0) {
			name=name+letters[rnd.nextInt(letters.length)];
			length--;
		}
		return name;
	}
	public static String getErrorTrace(Throwable error) {
		if (error==null) {
			return "no error";
		}
		StringBuilder sb=new StringBuilder();
		StackTraceElement[] trace=error.getStackTrace();
		for (StackTraceElement te : trace) {
			sb.append(te.toString()+"\n");
		}
		return sb.toString();
	}
	
	/**
	 * Given a webdriver on a page, check if that page is one of our red error pages
	 * @param driver
	 * @return True if the driver is on an error page and false otherwise
	 */
	public static boolean isOnErrorPage(WebDriver driver) {
		return driver.findElements(By.className("starexecErrorPage")).size()!=0;
	}
	
	
}
