package org.starexec.data.security;


import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobSpace;
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.JobStatus.JobStatusCode;
import org.starexec.data.to.Status.StatusCode;

public class JobSecurity {
	
	
	public static ValidatorStatusCode canUserRecompileJob(int jobId, int userId) {
		if (!Users.isAdmin(userId)) {
			return new ValidatorStatusCode(false, "Only administrators can perform this action");
			
		}
		
		Job j=Jobs.get(jobId);
		if (j==null) {
			return new ValidatorStatusCode(false, "The given job could not be found");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the user is allowed to look at details of a given job space.
	 * This is equivalent to checking whether the user can see the job that owns the job
	 * space
	 * @param jobSpaceId The ID of the job space
	 * @param userId The ID of the user that wants to view details of the job space
	 * @return A ValidatorStatusCode that will have true if the operation is allowed
	 * and false otherwise
	 */
	public static ValidatorStatusCode canUserSeeJobSpace(int jobSpaceId, int userId) {
		JobSpace s = Spaces.getJobSpace(jobSpaceId);
		return canUserSeeJob(s.getJobId(), userId);
	}
	
	/**
	 * Checks to see if the given user has permission to see the details of the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserSeeJob(int jobId, int userId) {
		if (!Permissions.canUserSeeJob(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see this job");
		}
		return new ValidatorStatusCode(true);
	}
	
	
	/**
	 * Checks whether a given string is a valid type for the pairsInSpace page
	 * @param type The string to check
	 * @return True if valid and false otherwise
	 */
	public static boolean isValidGetPairType(String type) {
		if (type.equals("all") || type.equals("solved") || type.equals("incomplete") || type.equals("wrong") ||
				type.equals("unknown") || type.equals("resource") || type.equals("failed")) {
			return true;
		}
		return false;
		
	}
	
	
	/**
	 * Checks to see if the given user has permission to run a new post processor on the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the requests
	 * @param pid the ID of the post processor that would be used.
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserPostProcessJob(int jobId, int userId, int pid, int stageNumber) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return new ValidatorStatusCode(false, "The job could not be found");
		}
		boolean isAdmin=Users.isAdmin(userId);
		
		if (job.getUserId()!=userId && !isAdmin) {
			return new ValidatorStatusCode(false, "You do not have permission to post process this job");
		}
		Processor p=Processors.get(pid);
		if (!Users.isMemberOfCommunity(userId, p.getCommunityId()) && !isAdmin) {
			return new ValidatorStatusCode(false, "You do not have permission to use the selected post processor");
		}
		
		if (!Jobs.canJobBePostProcessed(jobId)) {
			return new ValidatorStatusCode(false, "The job is not yet completed");
		}
		
		if (stageNumber<=0) {
			return new ValidatorStatusCode(false, "Stage numbers must be greater than 0");
		}
		
		return new ValidatorStatusCode(true);
	}
	/**
	 * Ensures a given user has permission to rerun pairs of a given status code
	 * @param jobId
	 * @param userId
	 * @param statusCode
	 * @return
	 */
	public static ValidatorStatusCode canUserRerunPairs(int jobId, int userId, int statusCode) {
		ValidatorStatusCode result= canUserRerunPairs(jobId, userId);
		if (!result.isSuccess()) {
			return result;
		}
		
		//can't rerun pairs that are not complete
		if (statusCode>StatusCode.ERROR_GENERAL.getVal()) {
			return new ValidatorStatusCode(false, "This pair is not yet completed");
		}
		return new ValidatorStatusCode(true);
	}
	
	public static ValidatorStatusCode canUserRerunPairs(int jobId, int userId) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return new ValidatorStatusCode(false, "The job could not be found");
		}
		boolean isAdmin=Users.isAdmin(userId);
		
		if (job.getUserId()!=userId && !isAdmin) {
			return new ValidatorStatusCode(false, "You do not have permission to rerun pairs in this job");
		}
		
		JobStatus status= Jobs.getJobStatusCode(jobId);
		if (status.getCode().getVal()==JobStatusCode.STATUS_PAUSED.getVal()) {
			return new ValidatorStatusCode(false, "This job is currently paused. Please unpause it before rerunning pairs");
		}
		if (status.getCode().getVal()==JobStatusCode.STATUS_KILLED.getVal()) {
			return new ValidatorStatusCode(false, "This job has been killed. It may no longer be run");
		}
		if (Jobs.isJobDeleted(jobId)) {
			return new ValidatorStatusCode(false, "This job has been deleted already");
		}
		return new ValidatorStatusCode(true);
	}
	
	public static ValidatorStatusCode canUserRerunAllPairs(int jobId, int userId) {
		if (!Jobs.isJobComplete(jobId)) {
			return new ValidatorStatusCode(false, "This job is not yet completed");
		}
		return canUserRerunPairs(jobId, userId);
	}
	
	/**
	 * Checks to see if the given user has permission to pause the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserPauseJob(int jobId, int userId) {
		Job job=Jobs.get(jobId);
		if (job==null) {
			return new ValidatorStatusCode(false, "The job could not be found");
		}
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to pause this job");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to resume of the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	
	public static ValidatorStatusCode canUserResumeJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to resume this job");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	
	/**
	 * Checks to see if the given user has permission to delete all jobs in the given list of jobs
	 * @param jobIds The IDs of the jobs being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise.
	 * If the user does not have the needed permissions for even one job, the error code will be returned
	 */
	
	public static ValidatorStatusCode canUserDeleteJobs(List<Integer> jobIds, int userId) {
		for (Integer jid : jobIds) {
			ValidatorStatusCode status=canUserDeleteJob(jid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to the given job
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise.
	 */
	public static ValidatorStatusCode canUserDeleteJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to delete this job");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user has permission to change the queue the given job is running on
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise.
	 */
	
	public static ValidatorStatusCode canChangeQueue(int jobId, int userId, int queueId) {
		if (!userOwnsJobOrIsAdmin(jobId,userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to change queues for this job");
		}
		Queue q=Queues.get(queueId);

		if (q==null){
			return new ValidatorStatusCode(false, "The given queue could not be found");
		}
		List<Queue> queues=Queues.getQueuesForUser(userId);
		for (Queue queue : queues) {
			if (queue.getId()==queueId) {
				return new ValidatorStatusCode(true);

			}
		}
		return new ValidatorStatusCode(false, "You do not have permission to access the selected queue");

	}
	
	/**
	 * Checks to see if the given user either owns the given job or is an admin
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 */
	public static boolean userOwnsJobOrIsAdmin(int jobId, int userId) {
		if (Users.isAdmin(userId)){
			return true;
		}

		Job j = Jobs.get(jobId);
		if(j == null || j.getUserId() != userId){
			return false;
		}
		return true;
	}
	
	/**
	 * Checks to see whether the given user is allowed to pause all jobs currently running
	 * on the system
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise.
	 */
	public static ValidatorStatusCode canUserPauseAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to resume all jobs that the admin has paused
	 * @param userId the ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and the status code from ValidatorStatusCodes otherwise.
	 */
	public static ValidatorStatusCode canUserResumeAllJobs(int userId){
		if (!Users.isAdmin(userId)){
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the user is allowed to download the Json object representing the job
	 * @param jobId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canGetJsonJob(int jobId, int userId) {
		if (!Permissions.canUserSeeJob(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the specified job");
		}
		Job s=Jobs.getIncludeDeleted(jobId);
		if (s==null) {
			return new ValidatorStatusCode(false, "The given job could not be found");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether a job can be run with community default settings in the given space
	 * @param userId ID of the user creating the job
	 * @param sId Id of the space to put the job in
	 * @return
	 */
	public static ValidatorStatusCode canCreateQuickJobWithCommunityDefaults(int userId, int sId,int statusId) {
			
			ValidatorStatusCode status = JobSecurity.canUserCreateJobInSpace(userId,sId);
			if (!status.isSuccess()) {
				return status;
			}
			DefaultSettings settings=Settings.getProfileById(statusId);
			
			
			if (Benchmarks.get(settings.getBenchId())==null) {
				return new ValidatorStatusCode(false, "The selected community has no default benchmark selected");
			}
			
			return new ValidatorStatusCode(true);
	}
	
	
	public static ValidatorStatusCode canUserCreateJobInSpace(int userId, int sId) {
		Permission p=Permissions.get(userId, sId);
		
		if (p==null || !p.canAddJob()) {
			return new ValidatorStatusCode(false, "You do not have permission to create a job in this space");
		}
		
		return new ValidatorStatusCode(true);
	}
}
