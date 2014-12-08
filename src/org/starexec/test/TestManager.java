package org.starexec.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;


import org.starexec.test.database.*;
import org.starexec.test.security.*;
import org.starexec.test.web.*;
import org.starexec.test.StateTests.IntroStateTests;
import org.starexec.test.database.BenchmarkTests;


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
		tests.add(new GetPageTests());
		tests.add(new JobPairTests());
		tests.add(new ClusterTests());
		tests.add(new QueueTests());
		tests.add(new DefaultSettingsTests());
		tests.add(new UploadTests());
		//tests.add(new LoginTests());
		//tests.add(new UploadSolverTests());
		//tests.add(new UploadBenchmarksTests());
		//tests.add(new SpaceExplorerTests());
	}
	
	public static boolean areTestsRunning() {
		return isRunning.get();
	}
	
	public static boolean isStressTestRunning() {
		return isRunningStress.get();
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
	public static boolean executeTests(String[] testNames) {
		if (Util.isProduction()) {
			return false; //right now, don't run anything on production
		}
		//don't run anything if we are already going
		if (!isRunning.compareAndSet(false, true)) {
			return false;
		}
		
		final String[] t=testNames;
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		//we want to return here, not wait until all the tests finish, which is why we spin off a new thread
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				
				for (String s : t) {
					TestSequence test = getTestSequence(s);
					if (test!=null) {
						test.clearResults();
						
						executeTest(test);
					}
					
				}
				isRunning.set(false);
			}
		});	
		
		return true;
	}
	/**
	 * Runs a stress test using the given parameters
	 * @param userCount How many new users will be created
	 * @param spaceCount How many new spaces to create
	 * @param jobCount How many new jobs to create
	 * @param minUsersPerSpace
	 * @param maxUsersPerSpace
	 * @param minSolversPerSpace
	 * @param maxSolversPerSpace
	 * @param minBenchmarksPerSpace
	 * @param maxBenchmarksPerSpace
	 * @param spacesPerJobCount
	 * @return
	 */
	public static boolean executeStressTest(final int userCount,final int spaceCount,final int jobCount, final int minUsersPerSpace, final int maxUsersPerSpace, final int minSolversPerSpace, 
			final int maxSolversPerSpace,final int minBenchmarksPerSpace,final int maxBenchmarksPerSpace,final int spacesPerJobCount) {
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
				StressTest.execute(userCount,spaceCount,minUsersPerSpace,maxUsersPerSpace,minSolversPerSpace,maxSolversPerSpace,
						minBenchmarksPerSpace, maxBenchmarksPerSpace,jobCount,spacesPerJobCount);
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
