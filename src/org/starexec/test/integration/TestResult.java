package org.starexec.test.integration;

import org.starexec.test.TestUtil;

import java.util.ArrayList;
import java.util.List;

public class TestResult {
	private String name="no name";
	private TestStatus status=new TestStatus();
	private List<String> messages=new ArrayList<String>();
	private Throwable error=null;
	private double time=0.0;
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
	public void addMessage(String message) {
		this.messages.add(message);
		
	}
	public void clearMessages() {
		this.messages=new ArrayList<String>();
	}
	public String getMostRecentMessage() {
		if (messages.size()>0) {
			return messages.get(0);
		} else {
			return "no message";
		}
		
	}
	
	public String getAllMessages() {
		if (messages.size()==0) {
			return "no message";
		}
		StringBuilder sb=new StringBuilder();
		for (String s : messages) {
			sb.append("--"+s+"\n");
		}
		return sb.toString();
	}

	public void setError(Throwable error) {
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}
	public String getErrorTrace() {
		return TestUtil.getErrorTrace(error);
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getTime() {
		return time;
	}
	
}
