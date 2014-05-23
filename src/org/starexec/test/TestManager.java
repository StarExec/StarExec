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
import org.starexec.test.database.JobTests;
import org.starexec.test.database.PermissionsTests;
import org.starexec.test.database.ProcessorTests;
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
import org.starexec.util.Util;

/**
 * This class maintains a list of all TestSequences and handles requests
 * for running those sequences. It prevents multiple instances of the same
 * TestSequence from being run simultaneously.
 * @author Eric Burns
 *
 */
public class TestManager {
	private static final Logger log = Logger.getLogger(TestManager.class);
	private final static AtomicBoolean isRunning=new AtomicBoolean(false);
	private final static AtomicBoolean isRunningStress=new AtomicBoolean(false);
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
		tests.add(new JobSecurityTests());
		tests.add(new BenchmarkSecurityTests());
		tests.add(new CacheSecurityTests());
		tests.add(new UserTests());
		tests.add(new PermissionsTests());
		tests.add(new IntroStateTests());
		tests.add(new UtilTests());
		tests.add(new SpaceSecurityTests());
		tests.add(new WebsiteTests());
		tests.add(new BenchmarkTests());
		tests.add(new ProcessorTests());
		tests.add(new JobTests());
	}
	
	/**
	 * Executes every test sequence in tests
	 * @return True if the tests were started, and false if they were not for some reason
	 */
	public static boolean executeAllTestSequences() {
		if (Util.isProduction()) {
			return false; //right now, don't run anything on production
		}
		//don't do anything if the tests are already running
		if (!isRunning.compareAndSet(false, true)) {
			return false;
		}
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		
		
		//we want to return here, not wait until all the tests finish, which is why we spin off a new threads
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				
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
		return true;
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
		if (Util.isProduction()) {
			return false; //right now, don't run anything on production
		}
		//don't run anything if we are already going
		if (!isRunning.compareAndSet(false, true)) {
			return false;
		}
		final String t=testName;
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		//we want to return here, not wait until all the tests finish, which is why we spin off a new thread
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				
				
				TestSequence test = getTestSequence(t);
				
				executeTest(test);
				isRunning.set(false);
			}
		});	
		
		return true;
	}
	
	public static boolean executeStressTest() {
		if (Util.isProduction()) {
			return false; //right now, don't run anything on production
		}
		//don't run anything if we are already going
		if (!isRunningStress.compareAndSet(false, true)) {
			return false;
		}
		
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		//we want to return here, not wait until all the tests finish, which is why we spin off a new thread
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				
				StressTest.execute();
				isRunningStress.set(false);
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
	
	/**
	 * Returns the names of all TestSequences known to the manager
	 * @return
	 */
	public static List<String> getTestNames() {
		List<String> names=new ArrayList<String>();
		for (TestSequence t : tests) {
			names.add(t.getName());
		}
		return names;
	}
	
	/**
	 * Gets the status of a TestSequence given its name
	 * @param testName The name of the TestSequence of interest
	 * @return
	 */
	public static TestStatus getTestStatus(String testName) {
		TestSequence t = getTestSequence(testName);
		if (t==null) {
			return null;
		}
		return t.getStatus();
	}
	
	/**
	 * Gets back the message contained by a given TestSequence
	 * @param testName
	 * @return
	 */
	public static String getTestMessage(String testName) {
		TestSequence t = getTestSequence(testName);
		if (t==null) {
			return null;
		}
		return t.getMessage();
	}
	
	/**
	 * Returns a TestSequence object 
	 * @param name
	 * @return
	 */
	private static TestSequence getTestSequence(String name) {		
		for (TestSequence t : tests) {
			if (t.getName().equals(name)) {
				return t;
			}
		}
		return null;
	}
}
