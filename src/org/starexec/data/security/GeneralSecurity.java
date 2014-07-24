package org.starexec.data.security;

import org.owasp.esapi.ESAPI;
import org.starexec.data.database.Users;
import org.starexec.util.Util;

public class GeneralSecurity {
	/**
	 * Checks to see if the given user has permission to restart Starexec
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRestartStarexec(int userId){
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see if the given user has permission to change logging settings
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserChangeLogging(int userId){
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	/**
	 * Checks to see if the given user has permission to view information related to
	 * testing
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */

	public static int canUserSeeTestInformation(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see if the given user has permission to execute tests
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */

	public static int canUserRunTests(int userId) {
		//only the admin can run tests, and they cannot be run on production
		if (!Users.isAdmin(userId) || Util.isProduction()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}

		return 0;
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
	
	
	
	
}
