package org.starexec.data.security;

import org.owasp.esapi.ESAPI;
import org.starexec.data.database.Users;
import org.starexec.test.integration.TestManager;
import org.starexec.util.Hash;
import org.starexec.util.Util;
import org.starexec.util.Validator;

public class GeneralSecurity {
	/**
	 * Checks to see if the given user has permission to restart Starexec
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRestartStarexec(int userId){
		if (!Users.isAdmin(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to change logging settings
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserChangeLogging(int userId){
		if (!Users.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see if the given user has permission to view information related to
	 * testing
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */

	public static ValidatorStatusCode canUserSeeTestInformation(int userId) {
		if (!Users.hasAdminReadPrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to execute tests without checking to see if tests are already running
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */

	public static ValidatorStatusCode canUserRunTestsNoRunningCheck(int userId) {
		//only the admin can run tests, and they cannot be run on production
		if (!Users.hasAdminWritePrivileges(userId) || Util.isProduction()) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to execute tests
	 * @param userId The ID of the user making the request
	 * @param stress True if the tests in question are stress tests, false otherwise
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */

	public static ValidatorStatusCode canUserRunTests(int userId, boolean stress) {

		boolean testRunning=false;

		if (stress) {
			testRunning=TestManager.isStressTestRunning();
		} else {
			testRunning=TestManager.areTestsRunning();
		}
		if (testRunning) {
			return new ValidatorStatusCode(false, "Tests are already running. Please wait until the current tests are finished");
		}

		return canUserRunTestsNoRunningCheck(userId);
	}
	/**
	 * Given a string, returns the same string in an HTML safe format
	 * Do NOT use this for HTML attributes! Use the function specifically for attributes
	 * @param str The string to change
	 * @return The formatted string
	 */
	public static String getHTMLSafeString(String str) {
		return ESAPI.encoder().encodeForHTML(str);
	}
	
	/**
	 * Formats a string so it is safe to insert in to an HTML attribute
	 * @param str The string to format
	 * @return The formatted string
	 */
	
	public static String getHTMLAttributeSafeString(String str) {
		return ESAPI.encoder().encodeForHTMLAttribute(str);
	}
	
	/**
	 * Formats a string so it is safe to insert into Javascript
	 * Do NOT use this for strings that will eventually inserted into HTML, even
	 * if they are going to Javascript first! It is not secure for that condition.
	 * @param str 
	 * @return The formatted string
	 */
	
	public static String getJavascriptSafeString(String str) {
		return ESAPI.encoder().encodeForJavaScript(str);
	}
	
	
	public static ValidatorStatusCode canUserUpdatePassword(int userId, int userIdMakingRequest, String oldPass, 
															String newPass, String confirmNewPass) {
		boolean userIsChangingOwnPassword = (userId == userIdMakingRequest);
		boolean userIsAdmin = Users.hasAdminWritePrivileges(userIdMakingRequest);
		if ( !(userIsChangingOwnPassword || userIsAdmin) ) {
			return new ValidatorStatusCode(false, "You do not have permission to change this user's password.");
		}
		String hashedPass = Hash.hashPassword(oldPass);
		String databasePass = Users.getPassword(userId);
		if (!hashedPass.equals(databasePass)) {
			return new ValidatorStatusCode(false, "The supplied password is incorrect");
		}
		if (!newPass.equals(confirmNewPass)) {
			return new ValidatorStatusCode(false, "The passwords are not the same");
		}
		
		if (!Validator.isValidPassword(newPass)) {
			return new ValidatorStatusCode(false, "The supplied password is invalid");
		}
		
		
		return new ValidatorStatusCode(true);
	}
	
}
