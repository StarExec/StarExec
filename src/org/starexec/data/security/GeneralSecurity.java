package org.starexec.data.security;

import org.owasp.esapi.ESAPI;
import org.starexec.constants.R;
import org.starexec.data.database.Users;
import org.starexec.logger.StarLogger;
import org.starexec.test.integration.TestManager;
import org.starexec.util.Hash;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import java.sql.Connection;

public class GeneralSecurity {

	private static final StarLogger log = StarLogger.getLogger(GeneralSecurity.class);
    //        private static final org.owasp.esapi.Logger esapiLogger = ESAPI.getLogger(GeneralSecurity.class.toString());

	/**
	 * Checks to see if the given user has permission to execute tests without checking to see if tests are already
	 * running
	 *
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */

	public static ValidatorStatusCode canUserRunTestsNoRunningCheck(int userId) {
		//only the admin can run tests, and they cannot be run on production
		if (!GeneralSecurity.hasAdminWritePrivileges(userId) || !R.ALLOW_TESTING) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see if the given user has permission to execute tests
	 *
	 * @param userId The ID of the user making the request
	 * @param stress True if the tests in question are stress tests, false otherwise
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */

	public static ValidatorStatusCode canUserRunTests(int userId, boolean stress) {

		boolean testRunning = false;

		if (stress) {
			testRunning = TestManager.isStressTestRunning();
		} else {
			testRunning = TestManager.areTestsRunning();
		}
		if (testRunning) {
			return new ValidatorStatusCode(
					false, "Tests are already running. Please wait until the current tests are finished");
		}

		return canUserRunTestsNoRunningCheck(userId);
	}

	/**
	 * Given a string, returns the same string in an HTML safe format Do NOT use this for HTML attributes! Use the
	 * function specifically for attributes
	 *
	 * @param str The string to change
	 * @return The formatted string
	 */
	public static String getHTMLSafeString(String str) {
		return ESAPI.encoder().encodeForHTML(str);
	}

    /**
     * a test method to make sure ESAPI is working correctly 
     */
    public static void test() {
	try {
	    String str = getHTMLSafeString("test");
	}
	catch(Exception e) {
	    log.error("Caught exception checking that ESAPI is working: " + Util.getStackTrace(e));
	    return;
	}
	log.info("ESAPI test passed.");
    }
	    

	/**
	 * Formats a string so it is safe to insert in to an HTML attribute
	 *
	 * @param str The string to format
	 * @return The formatted string
	 */

	public static String getHTMLAttributeSafeString(String str) {
		return ESAPI.encoder().encodeForHTMLAttribute(str);
	}

	/**
	 * Formats a string so it is safe to insert into Javascript Do NOT use this for strings that will eventually
	 * inserted into HTML, even if they are going to Javascript first! It is not secure for that condition.
	 *
	 * @param str
	 * @return The formatted string
	 */

	public static String getJavascriptSafeString(String str) {
		return ESAPI.encoder().encodeForJavaScript(str);
	}

	public static ValidatorStatusCode canUserUpdatePassword(
			int userId, int userIdMakingRequest, String oldPass, String newPass, String confirmNewPass
	) {
		boolean userIsChangingOwnPassword = (userId == userIdMakingRequest);
		boolean userIsAdmin = GeneralSecurity.hasAdminWritePrivileges(userIdMakingRequest);
		if (!(userIsChangingOwnPassword || userIsAdmin)) {
			return new ValidatorStatusCode(false, "You do not have permission to change this user's password.");
		}
		if (Users.isPublicUser(userIdMakingRequest)) {
			return new ValidatorStatusCode(false, "Passwords for guests cannot be changed");
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

	/**
	 * Checks to see if a user can view admin only pages.
	 *
	 * @param userId
	 * @return True if the user is either an admin or developer and false otherwise
	 * @author Albert Giegerich
	 */
	public static boolean hasAdminReadPrivileges(int userId) {
		return Users.isAdmin(userId) || Users.isDeveloper(userId);
	}

	public static boolean hasAdminReadPrivileges(Connection con, int userId) {
		return Users.isAdmin(con, userId) || Users.isDeveloper(con, userId);
	}

	/**
	 * Checks to see whether a user can make admin-only changes to the website/backend.
	 *
	 * @param userId
	 * @return True if the user is an admin and false otherwise
	 * @author Albert Giegerich
	 */
	public static boolean hasAdminWritePrivileges(int userId) {
		return Users.isAdmin(userId);
	}

	public static ValidatorStatusCode canUserSuspendOrReinstateUser(int userIdBeingUpdated, int userIdMakingCall) {
		if (Users.get(userIdBeingUpdated) == null) {
			return new ValidatorStatusCode(false, "The given user could not be found");
		}
		if (!GeneralSecurity.hasAdminWritePrivileges(userIdMakingCall)) {
			return new ValidatorStatusCode(false, "You do not have permission to suspend or reinstate users");
		}

		return new ValidatorStatusCode(false);
	}

	/**
	 * Checks if a user can generate a public anonymous link for a given primitive.
	 *
	 * @param userId The id of the user making the request for the link.
	 * @param primitiveType The type of the primitive. (Benchmark, Solver, etc.)
	 * @param primitiveId The id of the primitive.
	 * @author Albert Giegerich
	 */
	public static ValidatorStatusCode canUserGetAnonymousLinkForPrimitive(
			int userId, String primitiveType, int primitiveId
	) {
		final String methodName = "canUserGetAnonymousLinkForPrimitive";
		log.entry(methodName);
		log.debug("Checking if user can get anonymous link for primitive of type " + primitiveType);
		switch (primitiveType) {
		case R.BENCHMARK:
			log.debug(
					methodName, "Found that primitive was of type " + R.BENCHMARK +
							" while checking if an anonymous link could be generated for it.");
			return BenchmarkSecurity.canUserGetAnonymousLink(primitiveId, userId);
		case R.SOLVER:
			log.debug(
					methodName, "Found that primitive was of type " + R.SOLVER +
							" while checking if an anonymous link could be generated for it.");
			return SolverSecurity.canUserGetAnonymousLink(primitiveId, userId);
		case R.JOB:
			log.debug(
					methodName, "Found that primitive was of type " + R.JOB +
							" while checking if an anonymous link could be generated for it.");
			return JobSecurity.canUserGetAnonymousLink(primitiveId, userId);
		default:
			return new ValidatorStatusCode(
					false, "You do not have permission to get an anonymous link for this primitive.");
		}
	}
}
