package org.starexec.data.security;

import org.starexec.data.database.Users;
import org.starexec.util.Validator;
public class UserSecurity {
	private static int canUpdateName(String name) {
		if (!Validator.isValidUserName(name)) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		return 0;
	}
	
	private static int canUpdateInstitution(String institute) {
		
		if (!Validator.isValidInstitution(institute)) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		return 0;
	}
	
	public static int canUpdateData(int userIdBeingUpdated, int userIdCallingUpdate, String attribute, String newVal ){
		boolean admin=Users.isAdmin(userIdCallingUpdate);
		
		if (userIdBeingUpdated!=userIdCallingUpdate && !admin) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (attribute.equals("firstname") || attribute.equals("lastname")) {
			int status= canUpdateName(newVal);
			if (status!=0) {
				return status;
			}
		} else if (attribute.equals("institution")) {
			int status=canUpdateInstitution(newVal);
			if (status!=0) {
				return status;
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
	
	
	public static int canUserSuspendOrReinstateUser(int userIdMakingRequest) {
		if (!Users.isAdmin(userIdMakingRequest)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canViewUserPrimitives(int ownerId, int requestUserId){
		if (Users.isAdmin(requestUserId) || ownerId==requestUserId){
			return 0;
		}
		return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
	}
}
