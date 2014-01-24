package org.starexec.test;

public class TestResult {
	private String name="no name";
	private TestStatus status=new TestStatus();
	private String message="no message";
	private Throwable error=null;
	public TestResult() {
		status.setCode(TestStatus.TestStatusCode.STATUS_NOT_RUN.getVal());
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setStatus(TestStatus status) {
		this.status = status;
	}
	public TestStatus getStatus() {
		return status;
	}
	public void setMessage(String message) {
		if (message==null) {
			this.message="no message";
			return;
		}
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
	public String getErrorTrace() {
		if (getError()==null) {
			return "no error";
		}
		StringBuilder sb=new StringBuilder();
		StackTraceElement[] trace=getError().getStackTrace();
		for (StackTraceElement te : trace) {
			sb.append(te.toString()+"\n");
		}
		return sb.toString();
	}

	public void setError(Throwable error) {
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}
	
}
