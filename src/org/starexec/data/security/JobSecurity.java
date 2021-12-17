package org.starexec.data.security;

import org.starexec.app.RESTServices;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.logger.StarLogger;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JobSecurity {

	private static final StarLogger log = StarLogger.getLogger(JobSecurity.class);

	/**
	 * Checks to see whether a user can delete all the orphaned jobs owned by some user
	 *
	 * @param userIdToDelete The ID of the user having their jobs deleted
	 * @param userIdMakingRequest The ID of the user making the request
	 * @return A ValidatorStatusCide
	 */
	public static ValidatorStatusCode canUserDeleteOrphanedJobs(int userIdToDelete, int userIdMakingRequest) {
		if (Users.get(userIdToDelete) == null) {
			return new ValidatorStatusCode(false, "The given user could not be found");
		}
		if (userIdToDelete != userIdMakingRequest && !GeneralSecurity.hasAdminWritePrivileges(userIdMakingRequest)) {
			return new ValidatorStatusCode(
					false, "You do not have permission to delete jobs belonging to another user");
		}

		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserRecompileJob(int jobId, int userId) {
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return new ValidatorStatusCode(false, "Only administrators can perform this action");
		}

		Job j = Jobs.get(jobId);
		if (j == null) {
			return new ValidatorStatusCode(false, "The given job could not be found");
		}

		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserChangeJobPriority(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to change the priority of this job.");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the user is allowed to look at details of a given job space. This is equivalent to
	 * checking
	 * whether the user can see the job that owns the job space
	 *
	 * @param jobSpaceId The ID of the job space
	 * @param userId The ID of the user that wants to view details of the job space
	 * @return A ValidatorStatusCode that will have true if the operation is allowed and false otherwise
	 */
	public static ValidatorStatusCode canUserSeeJobSpace(int jobSpaceId, int userId) {
		JobSpace s = Spaces.getJobSpace(jobSpaceId);
		if (s == null) {
			return new ValidatorStatusCode(false, "The given job space could not be found");
		}
		return canUserSeeJob(s.getJobId(), userId);
	}

	/**
	 * Checks to see if the given user has permission to see the details of the given job
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserSeeJob(int jobId, int userId) {
		return Permissions.canUserSeeJob(jobId, userId);
	}

	/**
	 * Checks to see if the given user has permission to see the details of the job that owns the given pair
	 *
	 * @param pairId The ID of the pair
	 * @param userId The ID of the user making the request
	 * @return A ValidatorStatusCode
	 */
	public static ValidatorStatusCode canUserSeeJobWithPair(int pairId, int userId) {
		JobPair jp = JobPairs.getPair(pairId);
		if (jp == null) {
			return new ValidatorStatusCode(false, "The given pair could not be found");
		}
		return canUserSeeJob(jp.getJobId(), userId);
	}

	/**
	 * Checks if a job is associated with a given anonymous link uuid.
	 *
	 * @param anonymousLinkUuid A uuid that needs to be checked if it is connected to the given job space.
	 * @param jobSpaceId The id of the job space to check
	 * @return true ValidatorStatusCode if successful otherwise a false ValidatorStatusCode
	 * @author Eric Burns
	 */
	public static ValidatorStatusCode isAnonymousLinkAssociatedWithJobSpace(String anonymousLinkUuid, int jobSpaceId) {
		JobSpace space = Spaces.getJobSpace(jobSpaceId);
		if (space == null) {
			return new ValidatorStatusCode(false, "The given job space could not be found");
		}
		return isAnonymousLinkAssociatedWithJob(anonymousLinkUuid, space.getJobId());
	}

	/**
	 * Checks if a job is associated with a given anonymous link uuid.
	 *
	 * @param anonymousLinkUuid A uuid that needs to be checked if it is connected to the given job.
	 * @param jobId The id of the job to check
	 * @return true ValidatorStatusCode if successful otherwise a false ValidatorStatusCode
	 * @author Albert Giegerich
	 */
	public static ValidatorStatusCode isAnonymousLinkAssociatedWithJob(String anonymousLinkUuid, int jobId) {
		final String methodName = "isAnonymousLinkAssociatedWithJob";
		log.entry(methodName);
		try {
			Optional<Integer> potentialJobId = AnonymousLinks.getIdOfJobAssociatedWithLink(anonymousLinkUuid);
			if (!potentialJobId.isPresent() || potentialJobId.get() != jobId) {
				return new ValidatorStatusCode(false, "Job does not exist.");
			} else {
				return new ValidatorStatusCode(true);
			}
		} catch (SQLException e) {
			log.error(methodName, "Caught an SQLException while trying to access anonymous links table data.");
			return new ValidatorStatusCode(false, "Database error.");
		}
	}

	/**
	 * Checks whether a given string is a valid type for the pairsInSpace page
	 *
	 * @param type The string to check
	 * @return True if valid and false otherwise
	 */
	public static boolean isValidGetPairType(String type) {
		return type.equals("all") || type.equals("solved") || type.equals("incomplete") || type.equals("wrong") ||
				type.equals("unknown") || type.equals("resource") || type.equals("failed");
	}

	/**
	 * Checks to see if the given user has permission to run a new post processor on the given job
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the requests
	 * @param pid the ID of the post processor that would be used.
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserPostProcessJob(int jobId, int userId, int pid, int stageNumber) {
		Job job = Jobs.get(jobId);
		if (job == null) {
			return new ValidatorStatusCode(false, "The job could not be found");
		}
		boolean isAdmin = GeneralSecurity.hasAdminWritePrivileges(userId);

		if (job.getUserId() != userId && !isAdmin) {
			return new ValidatorStatusCode(false, "You do not have permission to post process this job");
		}
		Processor p = Processors.get(pid);
		if (p == null || !Users.isMemberOfCommunity(userId, p.getCommunityId()) && !isAdmin) {
			return new ValidatorStatusCode(false, "You do not have permission to use the selected post processor");
		}

		if (!Jobs.canJobBePostProcessed(jobId)) {
			return new ValidatorStatusCode(false, "The job is not yet completed");
		}

		if (stageNumber <= 0) {
			return new ValidatorStatusCode(false, "Stage numbers must be greater than 0");
		}

		if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and cannot be processed");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Ensures a given user has permission to rerun pairs of a given status code
	 *
	 * @param jobId
	 * @param userId
	 * @param statusCode
	 * @return
	 */
	public static ValidatorStatusCode canUserRerunPairs(int jobId, int userId, int statusCode) {
		ValidatorStatusCode result = canUserRerunPairs(jobId, userId);
		if (!result.isSuccess()) {
			return result;
		}

		//can't rerun pairs that are not complete
		if ((statusCode > StatusCode.ERROR_GENERAL.getVal() && statusCode != StatusCode.ERROR_PRE_PROCESSOR.getVal() && statusCode != StatusCode.ERROR_POST_PROCESSOR.getVal()) || statusCode == StatusCode.STATUS_ENQUEUED.getVal() ||
				statusCode == StatusCode.STATUS_RUNNING.getVal() ||
				statusCode == StatusCode.STATUS_PENDING_SUBMIT.getVal()) {
			return new ValidatorStatusCode(false, "This pair is not yet completed");
		} else if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and no pairs can be rerun");
		}
		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserSeeRerunPairsPage(int jobId, int userId) {
		return rerunPairsHelper(jobId, userId, GeneralSecurity.hasAdminReadPrivileges(userId));
	}

	public static ValidatorStatusCode canUserRerunPairs(int jobId, int userId) {
		return rerunPairsHelper(jobId, userId, GeneralSecurity.hasAdminWritePrivileges(userId));
	}

	private static ValidatorStatusCode rerunPairsHelper(int jobId, int userId, boolean readOrWritePrivileges) {
		final String methodName = "rerunPairsHelper";
		Job job = Jobs.get(jobId);
		if (job == null) {
			return new ValidatorStatusCode(false, "The job could not be found");
		}

		if (job.getUserId() != userId && !readOrWritePrivileges) {
			return new ValidatorStatusCode(false, "You do not have permission to rerun pairs in this job");
		}
		if (job.isBuildJob()) {
			return new ValidatorStatusCode(
					false, "You may not rerun solver build jobs. Please reupload your solver instead.");
		}

		JobStatus status = null;
		try {
			status = Jobs.getJobStatus(jobId);
		} catch (SQLException e) {
			log.error(methodName, "Could not find job " + jobId + "in DB", e);
			return RESTServices.ERROR_DATABASE;
		}

		if (status == JobStatus.PAUSED) {
			return new ValidatorStatusCode(
					false, "This job is currently paused. Please resume it before rerunning pairs");
		}
		if (status == JobStatus.KILLED) {
			return new ValidatorStatusCode(false, "This job has been killed. It may no longer be run");
		}
		if (Jobs.isJobDeleted(jobId)) {
			return new ValidatorStatusCode(false, "This job has been deleted already");
		}
		if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and no pairs can be rerun");
		}
		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserRerunAllPairs(int jobId, int userId) {
		if (!Jobs.isJobComplete(jobId)) {
			return new ValidatorStatusCode(false, "This job is not yet completed");
		} else if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and no pairs can be rerun");
		}
		return canUserRerunPairs(jobId, userId);
	}

	/**
	 * Checks to see if the given user has permission to pause the given job
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserPauseJob(int jobId, int userId) {
		Job job = Jobs.get(jobId);
		if (job == null) {
			return new ValidatorStatusCode(false, "The job could not be found");
		}
		if (!userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to pause this job");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * @param jobId The id of the job to get an anonymous link for.
	 * @param userId The id of the user trying to get the anonymous link.
	 * @return A successful ValidatorStatusCode if the user can get an anonymous link for this job. An unsuccessful one
	 * otherwise.
	 * @author Albert Giegerich
	 */
	public static ValidatorStatusCode canUserGetAnonymousLink(int jobId, int userId) {
		if (userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(true);
		} else {
			return new ValidatorStatusCode(false, "You do not have permission to get an anonymous link for this job.");
		}
	}

	/**
	 * Checks to see if the given user has permission to resume the given job
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserResumeJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to resume this job");
		} else if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and cannot be resumed");
		}

		if (Users.isDiskQuotaExceeded(userId)) {
			return new ValidatorStatusCode(
					false,
					"Your disk quota has been exceeded: please clear out some old solvers, jobs, or benchmarks before " +
							"proceeding"
			);
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see if the given user has permission to delete all jobs in the given list of jobs
	 *
	 * @param jobIds The IDs of the jobs being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise. If the user does not have the needed permissions for even one job, the error code will be returned
	 */
	public static ValidatorStatusCode canUserDeleteJobs(List<Integer> jobIds, int userId) {
		for (Integer jid : jobIds) {
			ValidatorStatusCode status = canUserDeleteJob(jid, userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see if the given user has permission to the given job
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise.
	 */
	public static ValidatorStatusCode canUserDeleteJob(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to delete this job");
		} else if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and cannot be deleted");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see if the given user has permission to change the queue the given job is running on
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise.
	 */
	public static ValidatorStatusCode canChangeQueue(int jobId, int userId, int queueId) {
		if (!userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to change queues for this job");
		}
		Queue q = Queues.get(queueId);

		if (q == null) {
			return new ValidatorStatusCode(false, "The given queue could not be found");
		}
		List<Queue> queues = Queues.getUserQueues(userId);
		for (Queue queue : queues) {
			if (queue.getId() == queueId) {
				return new ValidatorStatusCode(true);
			}
		}
		return new ValidatorStatusCode(false, "You do not have permission to access the selected queue");
	}

	/**
	 * Checks if a user can add job pairs to a given job.
	 *
	 * @param jobId the id of the job to check permissions for.
	 * @param userId the id of the user to check permissions for.
	 * @return an appropriate ValidatorStatusCode.
	 * @author Albert Giegerich
	 */
	public static ValidatorStatusCode canUserAddJobPairs(int jobId, int userId) {
		if (!userOwnsJobOrIsAdmin(jobId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to add job pairs for this job.");
		} else if (Jobs.isReadOnly(jobId)) {
			return new ValidatorStatusCode(false, "This job is currently Read Only and no pairs can be added");
		}
		Job j = Jobs.get(jobId);
		if (j.isBuildJob()) {
			return new ValidatorStatusCode(false, "You can not add job pairs to a solver build job");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see if the given user either owns the given job or is an admin
	 *
	 * @param jobId The ID of the job being checked
	 * @param userId The ID of the user making the request
	 */
	public static boolean userOwnsJobOrIsAdmin(int jobId, int userId) {
		Job j = Jobs.get(jobId);
		return j != null && (GeneralSecurity.hasAdminWritePrivileges(userId) || j.getUserId() == userId);
	}

	/**
	 * Checks to see whether the user is allowed to download the Json object representing the job
	 *
	 * @param jobId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canGetJsonJob(int jobId, int userId) {
		ValidatorStatusCode canSeeJobStatus = Permissions.canUserSeeJob(jobId, userId);
		if (!canSeeJobStatus.isSuccess()) {
			return canSeeJobStatus;
		}
		Job s = Jobs.getIncludeDeleted(jobId);
		if (s == null) {
			return new ValidatorStatusCode(false, "The given job could not be found");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a job can be run with community default settings in the given space
	 *
	 * @param userId ID of the user creating the job
	 * @param sId Id of the space to put the job in
	 * @return
	 */
	public static ValidatorStatusCode canCreateQuickJobWithCommunityDefaults(int userId, int sId, int statusId) {
		final String methodName = "canCreateQuickJobWithCommunityDefaults";
		ValidatorStatusCode status = JobSecurity.canUserCreateJobInSpace(userId, sId);
		if (!status.isSuccess()) {
			return status;
		}
		try {
			DefaultSettings settings = Settings.getProfileById(statusId);
			if (settings.getBenchIds().isEmpty() || Benchmarks.get(settings.getBenchIds().get(0)) == null) {
				return new ValidatorStatusCode(false, "The selected community has no default benchmark selected");
			}
		} catch (SQLException e) {
			log.error(methodName, "Caught SQLException.", e);
			return RESTServices.ERROR_DATABASE;
		}
		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserCreateJobInSpace(int userId, int sId) {
		Permission p = Permissions.get(userId, sId);

		if (p == null || !p.canAddJob()) {
			return new ValidatorStatusCode(false, "You do not have permission to create a job in this space");
		}

		return new ValidatorStatusCode(true);
	}
}
