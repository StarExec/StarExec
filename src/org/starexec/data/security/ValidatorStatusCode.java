package org.starexec.data.security;

import com.google.gson.annotations.Expose;

public class ValidatorStatusCode {
	
	@Expose private boolean success;
	@Expose private String message;
	@Expose private int statusCode;
	
	public ValidatorStatusCode(boolean s, String m, int sc) {
		setSuccess(s);
		setMessage(m);
		setStatusCode(sc);
	}
	
	public ValidatorStatusCode(boolean s, String m) {
		setSuccess(s);
		setMessage(m);
		setStatusCode(0);
	}
	
	public ValidatorStatusCode(boolean s) {
		success=s;
		message="";
		setStatusCode(0);
	}
	
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}
	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

}
