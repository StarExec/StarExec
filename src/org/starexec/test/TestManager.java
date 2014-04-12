package org.starexec.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.test.StateTests.IntroStateTests;
import org.starexec.test.database.BenchmarkTests;
import org.starexec.test.database.PermissionsTests;
import org.starexec.test.database.SolverTests;
import org.starexec.test.database.SpaceTests;
import org.starexec.test.database.UserTests;
import org.starexec.test.database.WebsiteTests;
import org.starexec.test.security.BenchmarkSecurityTests;
import org.starexec.test.security.CacheSecurityTests;
import org.starexec.test.security.GeneralSecurityTests;
import org.starexec.test.security.JobSecurityTests;
import org.starexec.test.security.QueueSecurityTests;
import org.starexec.test.security.SolverSecurityTests;
import org.starexec.test.security.SpaceSecurityTests;
import org.starexec.test.security.UserSecurityTests;
import org.starexec.test.security.ValidatorTests;


public class TestManager {
	private static final Logger log = Logger.getLogger(TestManager.class);
	private final static AtomicBoolean isRunning=new AtomicBoolean(false);
	//this should never be modified outside of the initializeTests method
	private final static List<TestSequence> tests=new ArrayList<TestSequence>();
	//all test sequences need to be initialized here
	public static void initializeTests() {
		tests.add(new SolverTests());
		tests.add(new SpaceTests());
		tests.add(new StarexecCommandTests());
		tests.add(new SolverSecurityTests());
		tests.add(new ValidatorTests());
		tests.add(new UserSecurityTests());
		tests.add(new QueueSecurityTests());
		tests.add(new GeneralSecurityTests());
		//tests.add(new JobSecurityTests());
		tests.add(new BenchmarkSecurityTests());
		tests.add(new CacheSecurityTests());
		tests.add(new UserTests());
		tests.add(new PermissionsTests());
		tests.add(new IntroStateTests());
		tests.add(new UtilTests());
		tests.add(new SpaceSecurityTests());
		tests.add(new WebsiteTests());
		tests.add(new BenchmarkTests());
	}
	
	/**
	 * Executes every test sequence in tests
	 */
	public static void executeAllTestSequences() {
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		if (R.STAREXEC_SERVERNAME.equalsIgnoreCase("www.starexec.org")) {
			return; //right now, don't run anything on production
		}
		//we want to return here, not wait until all the tests finish, which is why we spin off a new threads
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				//don't do anything if the tests are already running
				if (!isRunning.compareAndSet(false, true)) {
					return;
				}
				//we want to clear all the results first, so it's obvious to the user what is left to be run
				for (TestSequence t : tests) {
					t.clearResults();
				}
				
				for (TestSequence t : tests) {
					t.execute();
				}
				isRunning.set(false);
			}
		});	
		
	}
	
	public static List<TestSequence> getAllTestSequences() {
		return tests;
	}
	public static List<TestResult> getAllTestResults(String sequenceName) {
		return TestManager.getTestSequence(sequenceName).getTestResults();
	}
	/**
	 * Executes the test that has the given name. If no such test exists, 
	 * returns false
	 * @param testName The name of the test that should be run
	 * @return True if the test could be found, false otherwise
	 */
	public static boolean executeTest(String testName) {
		if (R.STAREXEC_SERVERNAME.equals("www.starexec.org")) {
			return false; //right now, don't run anything on production
		}
		final String t=testName;
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		//we want to return here, not wait until all the tests finish, which is why we spin off a new thread
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				//don't run anything if we are already going
				if (!isRunning.compareAndSet(false, true)) {
					return;
				}
				
				TestSequence test = getTestSequence(t);
				
				executeTest(test);
				isRunning.set(false);
			}
		});	
		
		return true;
	}
	
	/**
	 * Executes the given test sequence
	 * @param test
	 */
	public static void executeTest(TestSequence test) {
		test.execute();
	}
	
	public static List<String> getTestNames() {
		List<String> names=new ArrayList<String>();
		for (TestSequence t : tests) {
			names.add(t.getName());
		}
		return names;
	}
	
	public static TestStatus getTestStatus(String testName) {
		TestSequence t = getTestSequence(testName);
		if (t==null) {
			return null;
		}
		return t.getStatus();
	}
	
	public static String getTestMessage(String testName) {
		TestSequence t = getTestSequence(testName);
		if (t==null) {
			return null;
		}
		return t.getMessage();
	}
	
	private static TestSequence getTestSequence(String name) {		
		for (TestSequence t : tests) {
			if (t.getName().equals(name)) {
				return t;
			}
		}
		return null;
	}
}
