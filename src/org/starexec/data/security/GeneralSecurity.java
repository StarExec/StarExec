package org.starexec.data.security;

import org.starexec.data.database.Users;

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
}
