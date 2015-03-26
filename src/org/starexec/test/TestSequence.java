package org.starexec.test;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public abstract class TestSequence {
	protected static final Logger log = Logger.getLogger(TestSequence.class);	
	protected String sequenceName="No Name";
	protected TestStatus status=new TestStatus();
	protected String message="No Message";
	protected int testsPassed=0;
	protected int testsFailed=0;
	protected Throwable error = null;
	//maps the names of tests to some data about them. Every test gets an entry when the TestSequence object is created
	HashMap<String,TestResult> testResults=new HashMap<String,TestResult>();
	
	
	public TestSequence() {
		initTestResults();
		sequenceName=getTestName(); //this method is implemented in every subclass
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
	 * Clears the results of every test and sets the status of this sequence to "not run"
	 */
	protected final void clearResults() {
		error=null; 
		testsPassed=0;
		testsFailed=0;
		message="No Message";
		status.setCode(TestStatus.TestStatusCode.STATUS_NOT_RUN.getVal());
		for (TestResult r : testResults.values()) {
			r.clearMessages();
			r.addMessage("test not started");
			r.getStatus().setCode(TestStatus.TestStatusCode.STATUS_NOT_RUN.getVal());
			r.setError(null); 
			r.setTime(0);
		}
	}
	
	/**
	 * Runs this test sequence by calling setup, then running the tests, then calling teardown. All previous results
	 * are cleared and the status of the TestSequence is updated throughout
	 * @return
	 */
	protected final boolean execute() {
		
		try {
			testsPassed=0;
			testsFailed=0;
			clearResults();
			status.setCode(TestStatus.TestStatusCode.STATUS_RUNNING.getVal());
			turnOffExternalLogging(); 
			setup();
			runTests();
			teardown();
			if (testsFailed==0) {
				status.setCode(TestStatus.TestStatusCode.STATUS_SUCCESS.getVal());
				setMessage("test completed successfully");
			} else {
				status.setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
				setMessage("failed tests detected");
			}
			
			return true;
		} catch (Throwable e) {
			status.setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
			setMessage(e.getMessage());
			log.error(e.getMessage(),e);
			error=e;
			
		}
		return false;
	}
	/**
	 * Returns the name of this test sequence
	 * @return the name
	 */
	abstract protected String getTestName();
	
	
	/**
	 * Turns off logging in some 3rd part libraries used by testing. 3rd party logs 
	 * tend to be extremely verbose and drown out our logging
	 */
	private void turnOffExternalLogging() {
		Logger.getLogger("java_cup.runtime").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("trax").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("selenium.webdriver").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.openqa").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.eclipse").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.jboss").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.apache").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.hamcrest").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.cyberneko").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.testng").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("org.w3c").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("bsh").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("net.sf").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("org.apache.http.wire").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.apache.http.impl.conn.Wire").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.apache.http.impl.conn.LoggingManagedHttpClientConnection").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.apache.http.impl.execchain.MainClientExec").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("selenium.webdriver.remote.remote_connection").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("com.google").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("com.opera").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("com.thoughtworks").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("com.steadystate").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("com.beust").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("com.sun").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("com.keypoint").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("org.jfree").setLevel(org.apache.log4j.Level.OFF);

		Logger.getLogger("netscape.javascript").setLevel(org.apache.log4j.Level.OFF);
		Logger.getLogger("net.sourceforge").setLevel(org.apache.log4j.Level.OFF);
		
		Logger.getLogger("com.gargoylesoftware").setLevel(org.apache.log4j.Level.OFF);
	}
	
	/**
	 * This function is called before the tests belonging to this sequence are run.
	 * All initialization should be done here (creating spaces, solvers, etc. that are
	 * used by the tests in this sequence).
	 * @throws Exception
	 */
	
	abstract protected void setup() throws Exception;
	protected final void runTests() {
		for (TestResult r : testResults.values()) {
			r.clearMessages();
			r.addMessage("test running");
			r.getStatus().setCode(TestStatus.TestStatusCode.STATUS_RUNNING.getVal());
		}
		
		try {
			List<Method> tests=getTests();
			for (Method m : tests) {
				TestResult t=testResults.get(m.getName());
				t.clearMessages();
				double a=System.currentTimeMillis();
				try {
					m.setAccessible(true);
					m.invoke(this, null);
					t.getStatus().setCode(TestStatus.TestStatusCode.STATUS_SUCCESS.getVal());
					t.setTime(System.currentTimeMillis()-a);
					t.addMessage("test executed without errors");
					testsPassed++;
					
				} catch (Throwable e) {
					e.printStackTrace();
					t.setTime(System.currentTimeMillis()-a);
					e=e.getCause();
					testsFailed++;
					t.setError(e);
					t.addMessage(e.getMessage());
					t.getStatus().setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
				}
				
			}
		} catch (Exception e) {
			log.debug("runTests says "+e.getMessage(),e);
		}	
	}
	/**
	 * This function is called after all the tests have finished running
	 * It will be called ONLY if setup ran successfully. 
	 */
	
	abstract protected void teardown() throws Exception;
	
	public final String getName() {
		return sequenceName;
	}
	public final TestStatus getStatus() {
		return status;	
	}
	
	public final String getMessage() {
		if (message==null) {
			return "no message";
		}
		return message;
	}
	protected final void setMessage(String newMessage) {
		message=newMessage;
	}
	protected final void setStatusCode(int statusCode) {
		status.setCode(statusCode);
	}
	protected final void setName(String newName) {
		sequenceName=newName;
	}
	
	
	
	/**
	 * Gets the total number of tests in this sequence
	 * @return the integer number
	 */
	public final int getTestCount() {
		return getTests().size();
	}
	
	/**
	 * Gets the number of tests that have passed in this sequence
	 * @return the integer number
	 */
	public final int getTestsPassed() {
		return testsPassed;
	}
	
	/**
	 * Gets the number of tests that have failed in this sequence
	 * @return the integer number
	 */
	
	public final int getTestsFailed() {
		return testsFailed;
	}
	/**
	 * Gets the test results for every test in this sequence
	 * @return
	 */
	public final List<TestResult> getTestResults() {
		List<TestResult> tr=new ArrayList<TestResult>();
		for (TestResult r : testResults.values()) {
			tr.add(r);
		}
		return tr;
	}
	
	public final TestResult getTestResult(String testName) {
		return testResults.get(testName);
	}
	/**
	 * Gets the trace for the most recent error that affected this 
	 * test sequence (in the setup-- errors in individual tests are reported there)
	 * If there was no error, returns "no error"
	 * @return The stack trace as a string
	 */
	public final String getErrorTrace() {
		return TestUtil.getErrorTrace(error);
	}
	
	/**
	 * Retrieves a list of all methods declared in the current class that have the
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
	
	/**
	 * This appends the given message to the log of an individual test. It uses
	 * reflection to figure out which test called it.
	 * @param message
	 */
	protected final void addMessage(String message) {
		try {
			String methodName=Thread.currentThread().getStackTrace()[2].getMethodName();
			this.getTestResult(methodName).addMessage(message);
		} catch (Exception e) {
			log.error("addMessage says "+e.getMessage(),e);
		}
		
	}
	
}
