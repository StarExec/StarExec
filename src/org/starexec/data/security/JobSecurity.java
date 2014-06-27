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
import org.starexec.data.to.Status.StatusCode;

public class JobSecurity {
	
	
	/**
	 * Checks to see if the given user has permission to see the details of the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserSeeJob(int jobId, int userId) {
		if (!Permissions.canUserSeeJob(jobId, userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	/**
	 * Checks whether a given string is a valid type for the pairsInSpace page
	 * @param type The string to check
	 * @return True if valid and false otherwise
	 */
	public static boolean isValidGetPairType(String type) {
		if (type.equals("all") || type.equals("solved") || type.equals("incomplete") || type.equals("wrong") ||
				type.equals("unknown") || type.equals("resource")) {
			return true;
		}
		return false;
		
	}
	
	/**
	 * Checks to see if the given user has permission to run a new post processor on the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @param pid the ID of the post processor that would be used.
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
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
	
	public static int canUserRerunPairs(int jobId, int userId, int statusode) {
		int result= canUserRerunPairs(jobId, userId);
		if(result!=0) {
			return result;
		}
		if (statusode<StatusCode.STATUS_COMPLETE.getVal() || statusode>StatusCode.ERROR_GENERAL.getVal()) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		return 0;
	}
	
	public static int canUserRerunPairs(int jobId, int userId) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		boolean isAdmin=Users.isAdmin(userId);
		
		if (job.getUserId()!=userId && !isAdmin) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see if the given user has permission to pause the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
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
	
	/**
	 * Checks to see if the given user has permission to resume of the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static int canUserResumeJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	
	/**
	 * Checks to see if the given user has permission to delete all jobs in the given list of jobs
	 * @param jobIds The IDs of the jobs being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 * If the user does not have the needed permissions for even one job, the error code will be returned
	 */
	
	public static int canUserDeleteJobs(List<Integer> jobIds, int userId) {
		for (Integer jid : jobIds) {
			int status=canUserDeleteJob(jid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
	/**
	 * Checks to see if the given user has permission to the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 */
	public static int canUserDeleteJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see if the given user has permission to change the queue the given job is running on
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 */
	
	//TODO: We need to make sure the user actually has access to this queue.
	public static int canChangeQueue(int jobId, int userId, int queueId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		Job j=Jobs.get(jobId);
		Queue q=Queues.get(queueId);

		if (q==null){
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see if the given user either owns the given job or is an admin
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 */
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
	
	/**
	 * Checks to see whether the given user is allowed to pause all jobs currently running
	 * on the system
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 */
	public static int canUserPauseAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to resume all jobs that the admin has paused
	 * @param userId the ID of the user making the request
	 * @return 0 if the operation is allowed and the status code from SecurityStatusCodes otherwise.
	 */
	public static int canUserResumeAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
}
