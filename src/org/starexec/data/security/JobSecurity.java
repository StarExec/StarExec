package org.starexec.data.security;


import java.util.List;

import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;

public class JobSecurity {
	public static int canUserSeeJob(int jobId, int userId) {
		
		if (!Permissions.canUserSeeJob(jobId, userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		
		
		return 0;
	}
	
	
	public static int canUserPostProcessJob(int jobId, int userId, int pid) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		boolean isAdmin=Users.isAdmin(userId);
		
		if (job.getUserId()!=userId && !isAdmin) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		Processor p=Processors.get(pid);
		if (!Users.isMemberOfCommunity(userId, p.getCommunityId()) && !isAdmin) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (!Jobs.canJobBePostProcessed(jobId)) {
			return SecurityStatusCodes.ERROR_JOB_INCOMPLETE;
		}
		
		return 0;
	}
	
	public static int canUserPauseJob(int jobId, int userId) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserResumeJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserDeleteJobs(List<Integer> jobIds, int userId) {
		for (Integer jid : jobIds) {
			int status=canUserDeleteJob(jid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
	public static int canUserDeleteJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canChangeQueue(int jobId, int userId, int queueId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		Queue q=Queues.get(queueId);
		if (q==null){
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		return 0;
	}
	
	private static boolean userOwnsJobOrIsAdmin(int jobId, int userId) {
		Job j = Jobs.get(jobId);
		
		if (Users.isAdmin(userId)){
			return true;
		}
		if(j == null || j.getUserId() != userId){
			return false;
		}
		return true;
	}
	
	
	public static int canUserPauseAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
