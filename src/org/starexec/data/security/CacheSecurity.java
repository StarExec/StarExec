package org.starexec.data.security;

import org.starexec.data.database.Users;

public class CacheSecurity {
	
	/**
	 * Checks to see if the given user is authorized to clear the cache
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed, and an error code from SecurityStatusCodes otherwise
	 */
	
	public static SecurityStatusCode canUserClearCache(int userId) {
		if (!Users.isAdmin(userId)) {
			return new SecurityStatusCode(false, "You do not have permission to clear the cache");
		}
		return new SecurityStatusCode(true);
	}
}
