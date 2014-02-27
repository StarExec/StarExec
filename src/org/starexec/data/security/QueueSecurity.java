package org.starexec.data.security;

import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.util.Validator;

public class QueueSecurity {
	public static int canUserMakeQueuePermanent(int userId) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserUpdateRequest(int userId, String queueName) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		// Make sure that the queue has a unique name
		if(Queues.notUniquePrimitiveName(queueName)) {
			return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
		}
		
		if (!Validator.isValidPrimName(queueName)) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		return 0;
	}
	
	public static int canUserSeeRequests(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserCancelRequest(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserRemoveQueue(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
