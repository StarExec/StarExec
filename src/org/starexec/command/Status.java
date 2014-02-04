package org.starexec.command;

import java.util.HashMap;
/**
 * This class defines the codes taht are used by StarexecCommand. All codes are integers--
 * all codes that indicate errors are less than 0. 0 is the code used for success.
 * @author Eric
 *
 */
public class Status {
	//Error codes for command parsing
	public static int STATUS_SUCCESS;
	public static int ERROR_BAD_COMMAND=-1;
	public static int ERROR_BAD_ARGS=-2;
	public static int ERROR_SERVER=-4;
	public static int ERROR_BAD_ARCHIVETYPE=-5;
	public static int ERROR_FILE_AND_URL=-6;
	public static int ERROR_MISSING_PARAM=-7;
	public static int ERROR_FILE_NOT_FOUND=-8;
	public static int ERROR_INVALID_FILEPATH=-9;
	public static int ERROR_ARCHIVE_NOT_FOUND=-10;
	public static int ERROR_FILE_EXISTS=-11;
	public static int ERROR_BAD_PARENT_SPACE=-12;
	public static int ERROR_BAD_CREDENTIALS=-13;
	public static int ERROR_COMMAND_NOT_IMPLEMENTED=-14;
	public static int ERROR_URL_NOT_ALLOWED=-15;
	public static int ERROR_INVALID_ID=-16;
	public static int ERROR_INVALID_TIMEOUT=-17;
	public static int ERROR_CONNECTION_LOST=-18;
	public static int ERROR_BAD_NAME=-19;
	public static int ERROR_BAD_DESCRIPTION=-20;
	public static int ERROR_BAD_TIME=-21;
	public static int ERROR_NOT_LOGGED_IN=-22;
	public static int ERROR_CONNECTION_EXISTS=-23;
	public static int ERROR_BAD_URL=-24;
	public static int ERROR_BAD_INSTITUTION=-25;
	public static int ERROR_PERMISSION_DENIED=-26;
	public static int ERROR_COMMAND_FILE_TERMINATING=-27;
	public static int ERROR_INSUFFICIENT_QUOTA=-28;
	public static int ERROR_NAME_NOT_UNIQUE=-29;
	public static int ERROR_BAD_TRAVERSAL_TYPE=-30;
	public static int ERROR_ID_AND_USER=-31;
	public static int ERROR_NO_USER_PRIMS=-32;
	
	//error messages
	private static HashMap<Integer,String> messages=new HashMap<Integer,String>();
	static {
		messages=new HashMap<Integer,String>();
		messages.put(STATUS_SUCCESS, "Execution was successful");
		messages.put(ERROR_BAD_COMMAND, "Unrecognized command");
		messages.put(ERROR_BAD_ARGS, "Parameters must be in the form {key}={value}");
		messages.put(ERROR_SERVER,"Error communicating with server");
		messages.put(ERROR_BAD_ARCHIVETYPE,"Bad archive type-- only zip files are supported");
		messages.put(ERROR_FILE_AND_URL,"An upload should contain either a url or a local file, not both");
		messages.put(ERROR_INVALID_FILEPATH,"The given filepath is invalid");
		messages.put(ERROR_MISSING_PARAM,"Command is missing a required parameter-- please consult the StarexecCommand reference");
		messages.put(ERROR_FILE_NOT_FOUND, "The specified file could not be found");
		messages.put(ERROR_ARCHIVE_NOT_FOUND, "You do not have permission to download the requested archive, or the archive does not exist-- please ensure the given ID is correct");
		messages.put(ERROR_FILE_EXISTS, "The specified filepath already exists-- use the flag \"ow\" to overwrite.");
		messages.put(ERROR_BAD_PARENT_SPACE,"You do not have permission to add subspaces to the given parent space, or the parent space does not exist");
		messages.put(ERROR_BAD_CREDENTIALS,"Invalid username and/or password");
		messages.put(ERROR_COMMAND_NOT_IMPLEMENTED,"Command not currently implemented");
		messages.put(ERROR_URL_NOT_ALLOWED,"URL uploads are not allowed here-- please upload a local archive");
		messages.put(ERROR_INVALID_ID, "Invalid ID-- IDs must be positive integers");
		messages.put(ERROR_INVALID_TIMEOUT, "Invalid timeout-- Timouts must be positive integers");
		messages.put(ERROR_CONNECTION_LOST, "The connection to the server was lost. You must log in again to continue");
		messages.put(ERROR_BAD_NAME, "The specified name is invalid");
		messages.put(ERROR_BAD_DESCRIPTION, "The specified description is invalid");
		messages.put(ERROR_BAD_TIME, "The time should be a positive double, measured in seconds");
		messages.put(ERROR_NOT_LOGGED_IN, "You must log in before issuing commands to the server");
		messages.put(ERROR_CONNECTION_EXISTS, "You must log out of the existing session before you can start a new one");
		messages.put(ERROR_BAD_URL, "The given URL does not point to a valid Starexec instance. Ensure that you are using the correct protocol (http vs https) and that the address ends with a /");
		messages.put(ERROR_BAD_INSTITUTION, "The institution given has invalid characters or is too long");
		messages.put(ERROR_PERMISSION_DENIED,"You do not have permission to view the contents of the given space, or the space does not exist");
		messages.put(ERROR_COMMAND_FILE_TERMINATING, "An error was encountered: the file of commands may not have been completed");
		messages.put(ERROR_NAME_NOT_UNIQUE, "All primitives in a given space must have unique names");
		messages.put(ERROR_INSUFFICIENT_QUOTA, "You do not have the required disk quota to copy the primitive");
		messages.put(ERROR_BAD_TRAVERSAL_TYPE, "The traversal must be either depth-first ("+R.ARG_DEPTHFIRST+") or round-robin ("+R.ARG_ROUNDROBIN+")");
		messages.put(ERROR_ID_AND_USER, "Only one of "+R.PARAM_ID+" and "+R.PARAM_USER+" is allowed");
		messages.put(ERROR_NO_USER_PRIMS,"User primitives can only be obtained for jobs, solvers, and benchmarks");
	}
	
	public static String getStatusMessage(int code) {
		return messages.get(code);
	}
}
