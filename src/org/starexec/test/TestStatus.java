package org.starexec.test;

import com.google.gson.annotations.Expose;

/**
 * Represents a status for a test sequence
 * @author Eric Burns
 */
public class TestStatus {
	public static enum TestStatusCode {
		STATUS_UNKNOWN(0),
		STATUS_NOT_RUN(1),
		STATUS_RUNNING(2),
		STATUS_SUCCESS(3),
		STATUS_FAILED(4);
		private int val;
		
		private TestStatusCode(int val) {
			this.val = val;			
		}				
		
		public int getVal() {
			return this.val;			
		}				
		
		
		public static TestStatusCode toStatusCode(int code) {
			
		    switch (code) {
		    case 0:
		    	return TestStatusCode.STATUS_UNKNOWN;
		    case 1:
		    	return TestStatusCode.STATUS_NOT_RUN;
		    case 2: 
		    	return TestStatusCode.STATUS_RUNNING;
		    case 3:
		    	return TestStatusCode.STATUS_SUCCESS;
		    case 4:
		    	return TestStatusCode.STATUS_FAILED;
		    }
		    return TestStatusCode.STATUS_UNKNOWN;
		}
	}


	private static String getDescription(int code) {
		switch (code) {
		    case 1:
			return "This test has not yet been run";
		    case 2:
			return "this test is still running";
		    case 3:
			return "this test completed without any errors";
		    case 4:
			return "there was an error duing the execution of this test: check the message and log for more information";
		   
	    }
		return "the job status is not known or has not been set";
	}
	private static String getStatus(int code) {
		switch (code) {
		    case 1:
			return "not run";
		    case 2:
			return "running";
		    case 3:
			return "success";
		    case 4:
			return "failure";
		  
		   
	    }
		return "unknown";
	}
	


	@Expose private TestStatusCode code = TestStatusCode.STATUS_UNKNOWN;

	
	/**
	 * @return the status code for this status
	 */
	public TestStatusCode getCode() {
		return code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(TestStatusCode code) {
		this.code = code;
	}
	
	/**
	 * @param code the status code to set for this status
	 */
	public void setCode(int code) {
	    this.code = TestStatusCode.toStatusCode(code);
	}

	/**
	 * @return the canonical status
	 */
	public String getStatus() {
		return TestStatus.getStatus(this.code.getVal());
	}
	
	/**
	 * @return the description of what the status means
	 */
	public String getDescription() {
		return TestStatus.getDescription(this.code.getVal());
	}
	@Override
	public String toString() {
		return this.getStatus();
	}
}
