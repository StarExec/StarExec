package org.starexec.test;

public abstract class TestSequence {
	
	protected String name="No Name";
	protected TestStatus status=new TestStatus();
	protected String message="No Message";
	
	/**
	 * The execute function handles  the status of the test both before and after execution,
	 * and also calls the interalExecute function, which is where the real work is done.
	 * Internal Execute should be implemented in all tests.
	 * @return
	 */
	protected final boolean execute() {
		status.setCode(TestStatus.TestStatusCode.STATUS_RUNNING.getVal());
		boolean success=internalExecute();
		
		if (success==true) {
			status.setCode(TestStatus.TestStatusCode.STATUS_SUCCESS.getVal());
		} else {
			status.setCode(TestStatus.TestStatusCode.STATUS_FAILED.getVal());
		}
		return success;
	}
	
	/**
	 * This is where the actual testing should be implemented in every test
	 * @return
	 */
	abstract boolean internalExecute();
	
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
}
