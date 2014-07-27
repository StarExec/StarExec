package org.starexec.data.security;

import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.util.Validator;

public class QueueSecurity {
	
	/**
	 * Checks to see whether the given user is allowed to make a queue permanent
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	public static SecurityStatusCode canUserMakeQueue(int userId) {
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Ensures a user has the appropriate permissions to edit an existing queue with the given
	 * new values
	 * @param userId The user making the request
	 * @param clockTimeout The new clock timeout to be given to the queue
	 * @param cpuTimeout The new cpu timeout to be given to the queue
	 * @return new SecurityStatusCode(true) if the operation is allowed and a different number otherwise
	 */
	
	public static SecurityStatusCode canUserEditQueue(int userId, int clockTimeout, int cpuTimeout) {
		if (!Users.isAdmin(userId)) {
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		
		if (clockTimeout<=0 || cpuTimeout<=0) {
			return new SecurityStatusCode(false, "All timeouts need to be greater than 0");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to update a queue reservation request
	 * @param userId The ID of the user making the request
	 * @param queueName The name the new queue would be given
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	public static SecurityStatusCode canUserUpdateRequest(int userId, String queueName) {
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		
		// Make sure that the queue has a unique name
		if(Queues.notUniquePrimitiveName(queueName)) {
			return new SecurityStatusCode(false, "The queue must have a unique name after the update");
		}
		
		if (!Validator.isValidPrimName(queueName)) {
			return new SecurityStatusCode(false, "The given name is not formatted correctly. Please refer to the help pages to see the proper format");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to view current queue requests
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	public static SecurityStatusCode canUserSeeRequests(int userId) {
		if (!Users.isAdmin(userId)) {
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user is allowed to cancel a queue request
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	public static SecurityStatusCode canUserCancelRequest(int userId) {
		if (!Users.isAdmin(userId)) {
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to remove a queue
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status from SecurityStatusCodes if not
	 */
	
	
	public static SecurityStatusCode canUserRemoveQueue(int userId) {
		if (!Users.isAdmin(userId)) {
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to give a permanent queue global access
	 * @param userId the ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserMakeQueueGlobal(int userId) {
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to remove global access from a permanent queue
	 * @param userId the ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveQueueGlobal(int userId) {
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
}
