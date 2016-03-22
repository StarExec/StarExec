package org.starexec.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.starexec.constants.R;
import org.starexec.data.database.Processors;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.data.to.Status;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.util.SessionUtil;
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
	
	public static Configuration getFakeConfig() {
		Configuration c = new Configuration();
		c.setName(getRandomAlphaString(20));
		c.setDescription(getRandomAlphaString(20));
		c.setId(rnd.nextInt());
		
		return c;
	}
	/**
	 * Creates a fake solver object with randomized parameters. The solver will not be in the database
	 * @return a solver object will all fields populated
	 */
	public static Solver getFakeSolver() {
		Solver s = new Solver();
		s.setId(rnd.nextInt());
		s.setName(getRandomAlphaString(20));
		s.setDescription(getRandomAlphaString(20));
		s.setDiskSize(rnd.nextLong());
		s.setPath(getRandomAlphaString(20));
		s.setUserId(rnd.nextInt());
		Configuration c = getFakeConfig();
		c.setSolverId(s.getId());
		s.addConfiguration(c);
		return s;
	}
	
	/**
	 * Returns a list of job pairs that are not in the database.
	 * @param number The number of job pairs to get
	 * @return A list of 'number' job pairs
	 */
	public static List<JobPair> getFakeJobPairs(int number) {
		List<JobPair> pairs = new ArrayList<JobPair>();
		int jobId = rnd.nextInt();
		List<Solver> solvers = new ArrayList<Solver>();
		for (int solver=0;solver<5;solver++) {
			solvers.add(getFakeSolver());
		}
		for (int i=0;i<number;i++) {
			JobPair p = new JobPair();
			p.setId(rnd.nextInt());
			p.setBench(getFakeBenchmark());
			p.setJobId(jobId);
			p.getStatus().setCode(StatusCode.STATUS_COMPLETE.getVal());
			JoblineStage stage = new JoblineStage();
			stage.setCpuUsage(rnd.nextDouble()*100);
			stage.setWallclockTime(rnd.nextDouble()*100);
			stage.setJobpairId(p.getId());
			stage.setStageNumber(1);
			stage.getStatus().setCode(StatusCode.STATUS_COMPLETE.getVal());
			stage.setSolver(solvers.get(rnd.nextInt(5)));
			stage.setConfiguration(stage.getSolver().getConfigurations().get(0));

			p.addStage(stage);
			pairs.add(p);
		}
		return pairs;
		
	}
	/**
	 * Creates a mock HttpServletRequest object that SessionUtil will believe is from
	 * the given user
	 * @param userId
	 * @return The mock session
	 */
	public static HttpServletRequest getMockHttpRequest(int userId) {
		return getMockHttpRequest(userId, new HashMap<String,String>());
	}
	/**
	 * Creates a mock HttpServletRequest object that SessionUtil will believe is from
	 * the given user
	 * @param userId
	 * @param parameters
	 * @return The mock session
	 */
	public static HttpServletRequest getMockHttpRequest(int userId, Map<String,String> parameters) {
		return getMockHttpRequest(userId, parameters, new HashMap<String, List<String>>());
	}
	
	/**
	 * Creates a mock HttpServletRequest object that SessionUtil will believe is from
	 * the given user
	 * @param userId
	 * @param parameters Parameters to add to the mock http session.
	 * @param arrayParameters Parameter arrays to add to the mock http session
	 * @return The mock session
	 */
	public static HttpServletRequest getMockHttpRequest(int userId, Map<String,String> parameters, Map<String, List<String>> arrayParameters) {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		User u = new User();
		Mockito.when(session.getAttribute(SessionUtil.USER)).thenReturn(u);
		for (String s : parameters.keySet()) {
			Mockito.when(request.getAttribute(s)).thenReturn(parameters.get(s));
		}
		for (String s : arrayParameters.keySet()) {
			List<String> strings = arrayParameters.get(s);
			String[] arr = new String[strings.size()];
			for (int i=0;i<strings.size();i++) {
				arr[i] = strings.get(i);
			}
			Mockito.when(request.getParameterValues(s)).thenReturn(arr);
		}
		
		Mockito.when(request.getSession()).thenReturn(session);
		return request;
	}
	
}
