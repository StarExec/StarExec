package org.starexec.test;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public abstract class TestSequence {
	private static final Logger log = Logger.getLogger(TestSequence.class);	
	protected static String name="No Name";
	protected TestStatus status=new TestStatus();
	protected String message="No Message";
	protected int testsPassed=0;
	protected int testsFailed=0;
	//maps the names of tests to some data about them. Every test gets an entry when the TestSequence object is created
	HashMap<String,TestResult> testResults=new HashMap<String,TestResult>();
	
	
	public TestSequence() {
		System.out.println("here");
		initTestResults();
	}
	
	private final void initTestResults() {
		List<Method> tests=this.getTests();
		for (Method m : tests) {
			TestResult t=new TestResult();
			t.setName(m.getName());
			testResults.put(m.getName(), t);
		}
	}
	
	/**
	 * The execute function handles  the status of the test both before and after execution,
	 * and also calls the interalExecute function, which is where the real work is done.
	 * Internal Execute should be implemented in all tests.
	 * @return
	 */
	protected final boolean execute() {
		testsPassed=0;
		testsFailed=0;
		status.setCode(TestStatus.TestStatusCode.STATUS_RUNNING.getVal());
		for (TestResult r : testResults.values()) {
			r.setMessage("test not started");
			r.getStatus().setCode(TestStatus.TestStatusCode.STATUS_NOT_RUN.getVal());
			r.setError(null); 
		}
		try {
			boolean setup=setup();
			if (setup) {
				runTests();
				teardown();
				if (testsFailed==0) {
					status.setCode(TestStatus.TestStatusCode.STATUS_SUCCESS.getVal());
					setMessage("test completed successfully");
				} else {
					status.setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
					setMessage("failed tests detected");
				}
			} else {
				status.setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
			}
			return true;
		} catch (Exception e) {
			status.setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
			log.error(e);
		}
		return false;
	}
	
	
	
	abstract protected boolean setup();
	protected final void runTests() {
		for (TestResult r : testResults.values()) {
			r.setMessage("test running");
			r.getStatus().setCode(TestStatus.TestStatusCode.STATUS_RUNNING.getVal());
		}
		try {
			List<Method> tests=getTests();
			for (Method m : tests) {
				TestResult t=testResults.get(m.getName());
				try {
					m.setAccessible(true);
					m.invoke(this, null);
					t.getStatus().setCode(TestStatus.TestStatusCode.STATUS_SUCCESS.getVal());
					t.setMessage("test executed without errors");
					testsPassed++;
					
				} catch (Exception error) {
					Throwable e=error.getCause();
					//log.error("test "+m.getName()+" says "+e.getMessage(),e);
					testsFailed++;
					t.setError(e);
					t.setMessage(e.getMessage());
					t.getStatus().setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
				}
				
			}
		} catch (Exception e) {
			log.debug("runTests says "+e.getMessage(),e);
		}
		
	}
	abstract protected boolean teardown();
	
	public String getName() {
		return name;
	}
	public TestStatus getStatus() {
		return status;	
	}
	
	public String getMessage() {
		return message;
	}
	protected void setMessage(String newMessage) {
		message=newMessage;
	}
	protected void setStatusCode(int statusCode) {
		status.setCode(statusCode);
	}
	protected void setName(String newName) {
		name=newName;
	}
	
	
	
	/**
	 * Gets the total number of tests in this sequence
	 * @return the integer number
	 */
	public int getTestCount() {
		return getTests().size();
	}
	
	/**
	 * Gets the number of tests that have passed in this sequence
	 * @return the integer number
	 */
	public int getTestsPassed() {
		return testsPassed;
	}
	
	/**
	 * Gets the number of tests that have failed in this sequence
	 * @return the integer number
	 */
	
	public int getTestsFailed() {
		return testsFailed;
	}
	
	public List<TestResult> getTestResults() {
		List<TestResult> tr=new ArrayList<TestResult>();
		for (TestResult r : testResults.values()) {
			tr.add(r);
		}
		return tr;
	}
	
	/**
	 * Retrieves a list of all methods decared in the current class that have the
	 * @test annotation
	 * @return
	 */
	
	protected List<Method> getTests() {
		Method[] methods=this.getClass().getDeclaredMethods();
		
		List<Method> tests=new ArrayList<Method>();
		for (Method m : methods) {
			if (isTest(m)) {
				tests.add(m);
			}
		}
		
		return tests;
	}
	/**
	 * Determines whether the given method is a test, based on whether
	 * it has the @test annotation
	 * @param m The method to check
	 * @return
	 */
	protected static boolean isTest(Method m) {
		Annotation[] anns=m.getAnnotations();
		for (Annotation a : anns) {
			if (isTestAnnotation(a)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines whether the given annotation is the @test annotation
	 * @param a The annotation to check
	 * @return
	 */
	
	protected static boolean isTestAnnotation(Annotation a) {
		return a.annotationType().equals(Test.class);
	}
	
}
