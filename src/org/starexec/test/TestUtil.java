package org.starexec.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.starexec.constants.R;
import org.starexec.data.database.Processors;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Status;
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
	
	public static Processor getFakeProcessor(ProcessorType type) {
		Processor p = new Processor();
		p.setName(getRandomAlphaString(20));
		p.setDescription(getRandomAlphaString(20));
		p.setId(rnd.nextInt());
		p.setDiskSize(rnd.nextLong());
		p.setFilePath(getRandomAlphaString(20));
		p.setType(type);
		return p;
	}
	
	public static Benchmark getFakeBenchmark() {
		Benchmark b = new Benchmark();
		b.setId(rnd.nextInt());
		b.setName(getRandomAlphaString(20));
		b.setDescription(getRandomAlphaString(20));
		b.setDiskSize(rnd.nextLong());
		b.setPath(getRandomAlphaString(20));
		b.setUserId(rnd.nextInt());
		b.setType(getFakeProcessor(ProcessorType.BENCH));
		return b;
	}
	
	/**
	 * Returns a list of job pairs that are not in the database.
	 * @return A list of 3 job pairs
	 */
	public static List<JobPair> getFakeJobPairs() {
		List<JobPair> pairs = new ArrayList<JobPair>();
		int jobId = rnd.nextInt();
		for (int i=0;i<3;i++) {
			JobPair p = new JobPair();
			p.setId(rnd.nextInt());
			p.setBench(getFakeBenchmark());
			p.setJobId(jobId);
			pairs.add(p);
		}
		return pairs;
		
	}
	
	
}
