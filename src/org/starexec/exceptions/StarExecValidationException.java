package org.starexec.exceptions;

/**
 * Used when the user uploads something that fails validation
 */
public class StarExecValidationException extends StarExecException {
	public StarExecValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public StarExecValidationException(String message) {
		super(message);
	}
}
