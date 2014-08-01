package org.starexec.data.security;


import java.util.List;

import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.JobStatus.JobStatusCode;
import org.starexec.data.to.Status.StatusCode;

public class JobSecurity {
	
	
	/**
	 * Checks to see if the given user has permission to see the details of the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserSeeJob(int jobId, int userId) {
		if (!Permissions.canUserSeeJob(jobId, userId)) {
			return new SecurityStatusCode(false, "You do not have permission to see this job");
		}
		return new SecurityStatusCode(true);
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
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserPostProcessJob(int jobId, int userId, int pid) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return new SecurityStatusCode(false, "The job could not be found");
		}
		boolean isAdmin=Users.isAdmin(userId);
		
		if (job.getUserId()!=userId && !isAdmin) {
			return new SecurityStatusCode(false, "You do not have permission to post process this job");
		}
		Processor p=Processors.get(pid);
		if (!Users.isMemberOfCommunity(userId, p.getCommunityId()) && !isAdmin) {
			return new SecurityStatusCode(false, "You do not have permission to use the selected post processor");
		}
		
		if (!Jobs.canJobBePostProcessed(jobId)) {
			return new SecurityStatusCode(false, "The job is not yet completed");
		}
		
		return new SecurityStatusCode(true);
	}
	/**
	 * Ensures a given user has permission to rerun pairs of a given status code
	 * @param jobId
	 * @param userId
	 * @param statusCode
	 * @return
	 */
	public static SecurityStatusCode canUserRerunPairs(int jobId, int userId, int statusCode) {
		SecurityStatusCode result= canUserRerunPairs(jobId, userId);
		if (!result.isSuccess()) {
			return result;
		}
		
		//can't rerun pairs that are not complete
		if (statusCode<StatusCode.STATUS_COMPLETE.getVal() || statusCode>StatusCode.ERROR_GENERAL.getVal()) {
			return new SecurityStatusCode(false, "This pair is not yet completed");
		}
		return new SecurityStatusCode(true);
	}
	
	public static SecurityStatusCode canUserRerunPairs(int jobId, int userId) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return new SecurityStatusCode(false, "The job could not be found");
		}
		boolean isAdmin=Users.isAdmin(userId);
		
		if (job.getUserId()!=userId && !isAdmin) {
			return new SecurityStatusCode(false, "You do not have permission to rerun pairs in this job");
		}
		
		JobStatus status= Jobs.getJobStatusCode(jobId);
		if (status.getCode().getVal()==JobStatusCode.STATUS_PAUSED.getVal()) {
			return new SecurityStatusCode(false, "This job is currently paused. Please unpause it before rerunning pairs");
		}
		if (status.getCode().getVal()==JobStatusCode.STATUS_KILLED.getVal()) {
			return new SecurityStatusCode(false, "This job has been killed. It may no longer be run");
		}
		if (Jobs.isJobDeleted(jobId)) {
			return new SecurityStatusCode(false, "This job has been deleted already");
		}
		return new SecurityStatusCode(true);
	}
	
	public static SecurityStatusCode canUserRerunAllPairs(int jobId, int userId) {
		if (!Jobs.isJobComplete(jobId)) {
			return new SecurityStatusCode(false, "This job is not yet completed");
		}
		return canUserRerunPairs(jobId, userId);
	}
	
	/**
	 * Checks to see if the given user has permission to pause the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserPauseJob(int jobId, int userId) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return new SecurityStatusCode(false, "The job could not be found");
		}
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new SecurityStatusCode(false, "You do not have permission to pause this job");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to resume of the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static SecurityStatusCode canUserResumeJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new SecurityStatusCode(false, "You do not have permission to resume this job");
		}
		
		return new SecurityStatusCode(true);
	}
	
	
	/**
	 * Checks to see if the given user has permission to delete all jobs in the given list of jobs
	 * @param jobIds The IDs of the jobs being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 * If the user does not have the needed permissions for even one job, the error code will be returned
	 */
	
	public static SecurityStatusCode canUserDeleteJobs(List<Integer> jobIds, int userId) {
		for (Integer jid : jobIds) {
			SecurityStatusCode status=canUserDeleteJob(jid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 */
	public static SecurityStatusCode canUserDeleteJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new SecurityStatusCode(false, "You do not have permission to delete this job");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to change the queue the given job is running on
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 */
	
	public static SecurityStatusCode canChangeQueue(int jobId, int userId, int queueId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new SecurityStatusCode(false, "You do not have permission to change queues for this job");
		}
		Queue q=Queues.get(queueId);

		if (q==null){
			return new SecurityStatusCode(false, "The given queue could not be found");
		}
		List<Queue> queues=Queues.getQueuesForUser(userId);
		for (Queue queue : queues) {
			if (queue.getId()==queueId) {
				return new SecurityStatusCode(true);

			}
		}
		return new SecurityStatusCode(false, "You do not have permission to access the selected queue");

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
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise.
	 */
	public static SecurityStatusCode canUserPauseAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to resume all jobs that the admin has paused
	 * @param userId the ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and the status code from SecurityStatusCodes otherwise.
	 */
	public static SecurityStatusCode canUserResumeAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
}
