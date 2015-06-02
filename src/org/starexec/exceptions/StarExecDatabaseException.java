/**
 * Exception class for exceptions related to the database.
 */
public class StarExecDatabaseException extends StarExecException {

	public StarExecException(String message, Throwable cause) {
		super(message, cause);
	}

	public StarExecException(String message) {
		super(message);
	}
}
