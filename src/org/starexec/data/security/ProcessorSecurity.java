package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;

public class ProcessorSecurity {
	
	
	/** 
	 * Checks to see whether the given user is allowed to delete the given processor
	 * @param procId The ID of the processor being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserDeleteProcessor(int procId, int userId) {
		Processor p = Processors.get(procId);
		
		// Permissions check; ensures user is the leader of the community that owns the processor
		Permission perm = Permissions.get(userId, p.getCommunityId());	
		if(perm == null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
		
	}
	/** 
	 * Checks to see whether the given user is allowed to delete all of the given processors
	 * @param procId The IDs of the processors being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 * If the user lacks the necessary permissions for even one solver, a status code will be returned
	 */
	public static int canUserDeleteProcessors(List<Integer> procIds, int userId) {
		for (Integer id : procIds) {
			int status=canUserDeleteProcessor(id,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
	/** 
	 * Checks to see whether the given user is allowed to edit the given processor
	 * @param procId The ID of the processor being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static int canUserEditProcessor(int procId, int userId) {
		Processor p=Processors.get(procId);
		Permission perm= Permissions.get(userId,p.getCommunityId());
		if (perm==null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
