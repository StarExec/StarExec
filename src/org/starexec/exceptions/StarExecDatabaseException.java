package org.starexec.exceptions;

/**
 * Exception class for exceptions related to the database.
 */
public class StarExecDatabaseException extends StarExecException {

	public StarExecDatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public StarExecDatabaseException(String message) {
		super(message);
	}
}
