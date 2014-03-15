package org.starexec.data.security;

import org.starexec.data.database.Users;
import org.starexec.util.Validator;
public class UserSecurity {
	
	/**
	 * Checks to see whether one user is allowed to delete another
	 * @param userIdBeingDeleted The ID of the user that would be deleted
	 * @param userIdMakingRequest The ID of the user doing the deleting
	 * @return 0 if the operation is allowed, and an error code from SecurityStatusCodes otherwise
	 */
	//TODO: For now, not even the admin is allowed to delete users. Possibly change in the future?
	public static int canDeleteUser(int userIdBeingDeleted, int userIdMakingRequest) {
		if (!Users.isAdmin(userIdMakingRequest) || !Users.isTestUser(userIdBeingDeleted)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;	
	}
	/**
	 * Checks to see whether a user is allowed to update the data of another user
	 * @param userIdBeingUpdated The ID of the user having their data updated
	 * @param userIdCallingUpdate The ID of the user making the request
	 * @param attribute The name of the attribute being updated. 
	 * @param newVal The new value that will be given to the attribute
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUpdateData(int userIdBeingUpdated, int userIdCallingUpdate, String attribute, String newVal ){
		boolean admin=Users.isAdmin(userIdCallingUpdate);
		
		if (userIdBeingUpdated!=userIdCallingUpdate && !admin) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (attribute.equals("firstname") || attribute.equals("lastname")) {
			if (!Validator.isValidUserName(newVal)) {
				return SecurityStatusCodes.ERROR_INVALID_PARAMS;
			}
		} else if (attribute.equals("institution")) {
			if (!Validator.isValidInstitution(newVal)) {
				return SecurityStatusCodes.ERROR_INVALID_PARAMS;
			}
		} else if (attribute.equals("diskquota")) {
			if (!admin) {
				return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
			}
			if (!Validator.isValidInteger(newVal)) {
				return SecurityStatusCodes.ERROR_INVALID_PARAMS;
			}

		}	
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can suspend or reinstate users
	 * @param userIdMakingRequest The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserSuspendOrReinstateUser(int userIdMakingRequest) {
		if (!Users.isAdmin(userIdMakingRequest)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	/**
	 * Checks to see whether a given user is allowed to see the primitives owned by another user
	 * @param ownerId The ID of the user who owns the primitives
	 * @param requestUserId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canViewUserPrimitives(int ownerId, int requestUserId){
		if (Users.isAdmin(requestUserId) || ownerId==requestUserId){
			return 0;
		}
		return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
	}
}
