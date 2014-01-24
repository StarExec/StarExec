package org.starexec.test;

import java.util.ArrayList;
import java.util.List;


public class TestManager {
	
	private static List<TestSequence> tests=new ArrayList<TestSequence>();
	
	public static void initializeTests() {
		
		tests.add(new SpacePropertiesTest());
	}
	
	/**
	 * Executes every test sequence in tests
	 */
	public static void executeAllTestSequences() {
		for (TestSequence t : tests) {
			t.execute();
		}
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
	 * @param testName
	 * @return True if the test could be found, false otherwise
	 */
	public static boolean executeTest(String testName) {
		TestSequence t = getTestSequence(testName);
		if (t==null) {
			return false;
		}
		executeTest(t);
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
