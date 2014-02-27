package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.util.SessionUtil;

public class ProcessorSecurity {
	public static int canUserDeleteProcessor(int procId, int userId) {
		Processor p = Processors.get(procId);
		
		// Permissions check; ensures user is the leader of the community that owns the processor
		Permission perm = Permissions.get(userId, p.getCommunityId());	
		if(perm == null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
		
	}
	
	public static int canUserDeleteProcessors(List<Integer> procIds, int userId) {
		for (Integer id : procIds) {
			int status=canUserDeleteProcessor(id,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
	public static int canUserEditProcessor(int procId, int userId) {
		Processor p=Processors.get(procId);
		Permission perm= Permissions.get(userId,p.getCommunityId());
		if (perm==null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
