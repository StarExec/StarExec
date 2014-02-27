package org.starexec.data.security;

import org.starexec.data.database.Users;

public class GeneralSecurity {
	public static int canUserRestartStarexec(int userId){
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserSeeTestInformation(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
