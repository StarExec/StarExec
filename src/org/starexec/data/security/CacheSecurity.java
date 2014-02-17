package org.starexec.data.security;

import org.starexec.data.database.Users;

public class CacheSecurity {
	public static int canUserClearCache(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
