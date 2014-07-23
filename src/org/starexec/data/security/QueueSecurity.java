package org.starexec.data.security;

import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.util.Validator;

public class QueueSecurity {
	
	/**
	 * Checks to see whether the given user is allowed to make a queue permanent
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	public static int canUserMakeQueue(int userId) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Ensures a user has the appropriate permissions to edit an existing queue with the given
	 * new values
	 * @param userId The user making the request
	 * @param clockTimeout The new clock timeout to be given to the queue
	 * @param cpuTimeout The new cpu timeout to be given to the queue
	 * @return 0 if the operation is allowed and a different number otherwise
	 */
	
	public static int canUserEditQueue(int userId, int clockTimeout, int cpuTimeout) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (clockTimeout<=0 || cpuTimeout<=0) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to update a queue reservation request
	 * @param userId The ID of the user making the request
	 * @param queueName The name the new queue would be given
	 * @return 0 if the operation is allowed and a status from SecurityStatusCodes if not
	 */
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
	
	/**
	 * Checks to see whether the given user is allowed to view current queue requests
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	public static int canUserSeeRequests(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	/**
	 * Checks to see whether the given user is allowed to cancel a queue request
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	public static int canUserCancelRequest(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to remove a queue
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	
	public static int canUserRemoveQueue(int userId) {
		if (!Users.isAdmin(userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to give a permanent queue global access
	 * @param userId the ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserMakeQueueGlobal(int userId) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to remove global access from a permanent queue
	 * @param userId the ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveQueueGlobal(int userId) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
