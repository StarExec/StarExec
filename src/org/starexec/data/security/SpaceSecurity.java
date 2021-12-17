package org.starexec.data.security;

import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.SolverBuildStatus.SolverBuildStatusCode;
import org.starexec.logger.StarLogger;
import org.starexec.util.Validator;

import java.util.ArrayList;
import java.util.List;

public class SpaceSecurity {
	private static final StarLogger log = StarLogger.getLogger(SpaceSecurity.class);

	/**
	 * Checks to see whether a user can update the properties of a space (such as default permissions, sticky leaders,
	 * and so on)
	 *
	 * @param spaceId The ID of the space being updated
	 * @param userId The ID of the user making the request
	 * @param name The name the space would have upon updating
	 * @param stickyLeaders The value stickyLeaders would have upon updating
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUpdateProperties(int spaceId, int userId, String name, boolean
			stickyLeaders) {
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.isLeader()) {
			return new ValidatorStatusCode(false, "You do not have permission to update this space");
		}

		//communities are not allowed to have sticky leaders enabled
		if (Communities.isCommunity(spaceId) && stickyLeaders) {
			return new ValidatorStatusCode(false, "Community spaces may not enable sticky leaders");
		}

		Space os = Spaces.get(spaceId);
		if (!os.getName().equals(name)) {
			int parentId = Spaces.getParentSpace(os.getId());
			if (Spaces.notUniquePrimitiveName(name, parentId)) {
				return new ValidatorStatusCode(false, "The new name needs to be unique in the space");
			}
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user may both remove and recycle benchmarks in a space
	 *
	 * @param benchmarkIds The IDs of the benchmarks that would be removed and recycled
	 * @param spaceId The ID of the space containing the benchmarks
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRemoveAndRecycleBenchmarks(
			List<Integer> benchmarkIds, int spaceId, int userId
	) {
		ValidatorStatusCode status = canUserRemoveBenchmark(spaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}


		status = BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, userId);
		if (!status.isSuccess()) {
			return status;
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user may both remove and delete jobs in a space
	 *
	 * @param jobIds The IDs of the jobs that would be removed and recycled
	 * @param spaceId The ID of the space containing the jobs
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserRemoveAndDeleteJobs(List<Integer> jobIds, int spaceId, int userId) {
		ValidatorStatusCode status = canUserRemoveJob(spaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}
		status = JobSecurity.canUserDeleteJobs(jobIds, userId);
		if (!status.isSuccess()) {
			return status;
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user may both remove and delete solvers in a space
	 *
	 * @param solverIds The IDs of the solvers that would be removed and recycled
	 * @param spaceId The ID of the space containing the solvers
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */

	public static ValidatorStatusCode canUserRemoveAndRecycleSolvers(List<Integer> solverIds, int spaceId, int
			userId) {
		ValidatorStatusCode status = canUserRemoveSolver(spaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}
		status = SolverSecurity.canUserRecycleSolvers(solverIds, userId);
		if (!status.isSuccess()) {
			return status;
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether a user may remove a benchmark from a space
	 *
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserRemoveBenchmark(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canRemoveBench()) {
			return new ValidatorStatusCode(false, "You do not have permission to remove benchmarks from this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see if the given user can remove all of the given subspaces
	 *
	 * @param userId
	 * @param subspaceIds
	 * @return
	 */

	//TODO: What are the permissions for removing a space hierarchy?
	public static ValidatorStatusCode canUserRemoveSpace(int userId, List<Integer> subspaceIds) {
		for (Integer sid : subspaceIds) {
			Space subspace = Spaces.get(sid);
			if (subspace == null) {
				return new ValidatorStatusCode(false, "The space with the following ID could not be found: " + sid);
			}

			Integer parent = Spaces.getParentSpace(sid);
			Permission perm = Permissions.get(userId, parent);
			if (null == perm || !perm.canRemoveSpace()) {
				return new ValidatorStatusCode(false, "You do not have permission to remove subspaces from this " +
						"space");
			}
			Permission p = Permissions.get(userId, sid);
			if (p == null || !p.isLeader()) {
				return new ValidatorStatusCode(false, "You cannot remove spaces that you are not a leader of");
			}


			for (Space subspace2 : Spaces.getSubSpaceHierarchy(sid)) {
				// Ensure the user is the leader of that space
				p = Permissions.get(userId, subspace2.getId());
				if (p == null || !p.isLeader()) {
					return new ValidatorStatusCode(false, "You cannot remove spaces that you are not a leader of");
				}
			}
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether the user demoting is the administrator
	 *
	 * @param spaceId
	 * @param userIdBeingDemoted
	 * @param userIdDoingDemoting
	 * @return
	 */
	//TODO: Leaders can demote other leaders except at the community level, right?
	public static ValidatorStatusCode canDemoteLeader(int spaceId, int userIdBeingDemoted, int userIdDoingDemoting) {
		// Permissions check; ensures user is the leader of the community or is an admin
		if (Users.get(userIdBeingDemoted) == null) {
			return new ValidatorStatusCode(false, "The given user could not be found");
		}
		if (Spaces.get(spaceId) == null) {
			return new ValidatorStatusCode(false, "The given space could not be found");
		}
		if (!GeneralSecurity.hasAdminWritePrivileges(userIdDoingDemoting)) {
			return new ValidatorStatusCode(false, "You do not have permission to demote leaders in this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether a user may remove a job from a space
	 *
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserRemoveJob(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canRemoveJob()) {
			return new ValidatorStatusCode(false, "You do not have permission to remove jobs from this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether a user may remove a solver from a space
	 *
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserRemoveSolver(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canRemoveSolver()) {
			return new ValidatorStatusCode(false, "You do not have permission to remove solvers from this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether a user may leave a community
	 *
	 * @param commId The ID of the community a user wants to leave
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserLeaveCommunity(int commId, int userId) {
		//the user can leave if they are in the space
		if (!Users.isMemberOfCommunity(userId, commId)) {
			return new ValidatorStatusCode(false, "You are not a member of this community");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether a user may leave a space
	 *
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserLeaveSpace(int spaceId, int userId) {
		//the user can leave if they are in the space
		if (!Users.isMemberOfSpace(userId, spaceId)) {
			return new ValidatorStatusCode(false, "You are not currently in this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can see the given space
	 *
	 * @param spaceId The ID of the space in question
	 * @param userId The ID of the user who wants to see the space
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserSeeSpace(int spaceId, int userId) {
		if (!Permissions.canUserSeeSpace(spaceId, userId)) {
			log.debug("denying user id = " + userId + " for space = " + spaceId);
			return new ValidatorStatusCode(false, "You do not have permission to view this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user can copy any primitive from the given space. They cannot copy if the space is
	 * locked
	 * or if they cannot see the space.
	 *
	 * @param spaceId The ID of the space being copied from
	 * @param userIdDoingCopying The ID of the user who is making the copy request.
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	private static ValidatorStatusCode canCopyPrimFromSpace(int spaceId, int userIdDoingCopying) {
		if (GeneralSecurity.hasAdminWritePrivileges(userIdDoingCopying)) {
			return new ValidatorStatusCode(true);
		}
		// And the space the user is being copied from must not be locked
		if (Spaces.get(spaceId).isLocked()) {
			return new ValidatorStatusCode(false, "The space you are trying to copy from is locked");
		}

		// Verify the user can at least see the space they claim to be copying from
		if (!Permissions.canUserSeeSpace(spaceId, userIdDoingCopying)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the space you are copying from");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user can copy a given benchmark from the given space
	 *
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param benchId The ID of the benchmark that would be copied
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */
	private static ValidatorStatusCode canCopyBenchmarkFromSpace(int spaceId, int userIdDoingCopying, int benchId) {


		ValidatorStatusCode status = canCopyPrimFromSpace(spaceId, userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}
		if (!Permissions.canUserSeeBench(benchId, userIdDoingCopying)) {
			return new ValidatorStatusCode(false, "You are not allowed to see the benchmark you are trying to move");
		}
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			return new ValidatorStatusCode(false, "The benchmark you are trying to copy has already been deleted");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user can copy a given space from the given space
	 *
	 * @param userId The ID of the user making the request
	 * @param spaceIdBeingCopied The ID of the space that would be copied
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */
	private static ValidatorStatusCode canCopySpaceFromSpace(int userId, int spaceIdBeingCopied) {
		int fromSpaceId = Spaces.getParentSpace(spaceIdBeingCopied);
		ValidatorStatusCode status = canCopyPrimFromSpace(fromSpaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}

		if (!Permissions.canUserSeeSpace(spaceIdBeingCopied, userId)) {
			return new ValidatorStatusCode(
					false, "The subspace you are trying to move is not in the space you are copying from");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user can copy a given job from the given space
	 *
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param jobId The ID of the job that would be copied
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */

	private static ValidatorStatusCode canCopyJobFromSpace(int spaceId, int userIdDoingCopying, int jobId) {


		ValidatorStatusCode status = canCopyPrimFromSpace(spaceId, userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}
		ValidatorStatusCode canSeeJobStatus = Permissions.canUserSeeJob(jobId, userIdDoingCopying);
		if (!canSeeJobStatus.isSuccess()) {
			return canSeeJobStatus;
		}

		if (Jobs.isJobDeleted(jobId)) {
			return new ValidatorStatusCode(false, "The job you are trying to copy has already been deleted");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user can copy a given solver from the given space
	 *
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param solverId The ID of the solver that would be copied
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */
	private static ValidatorStatusCode canCopySolverFromSpace(int spaceId, int userIdDoingCopying, int solverId) {
		ValidatorStatusCode status = canCopyPrimFromSpace(spaceId, userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}

		//the solver being copied should actually be in the space they are supposedly being copied from
		if (!Permissions.canUserSeeSolver(solverId, userIdDoingCopying)) {
			return new ValidatorStatusCode(
					false, "The solver you are trying to move is not in the space you are copying from");
		}

		//we can't copy a solver if it has been deleted on disk
		if (Solvers.isSolverDeleted(solverId)) {
			return new ValidatorStatusCode(false, "The solver you are trying to move has already been deleted");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether the user has enough disk quota to fit all of a list of solvers
	 *
	 * @param solvers The solvers that would be added
	 * @param userId The ID of the user in question
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */
	private static ValidatorStatusCode doesUserHaveDiskQuotaForSolvers(List<Solver> solvers, int userId) {
		for (Solver s : solvers) {
			if (s == null) {
				return new ValidatorStatusCode(
						false, "At least one of the given solvers does not exist or has been " + "deleted");
			}
		}
		//first, validate that the user has enough disk quota to copy all the selected solvers
		//we don't copy any unless they have room for all of them
		User u = Users.get(userId);
		long userDiskUsage = u.getDiskUsage();
		long userDiskQuota = u.getDiskQuota();
		userDiskQuota -= userDiskUsage;
		for (Solver s : solvers) {
			userDiskQuota -= s.getDiskSize();
		}
		if (userDiskQuota < 0) {
			return new ValidatorStatusCode(false, "Not enough disk quota");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether the user has enough disk quota to fit all of a list of benchmarks
	 *
	 * @param benchIDs The solver IDs that would be added
	 * @param userId The ID of the user in question
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */

	private static boolean doesUserHaveDiskQuotaForBenchmarks(List<Integer> benchIDs, int userId) {
		List<Benchmark> oldBenches = Benchmarks.get(benchIDs);
		//first, validate that the user has enough disk quota to copy all the selected solvers
		//we don't copy any unless they have room for all of them
		User u = Users.get(userId);
		long userDiskUsage = u.getDiskUsage();
		long userDiskQuota = u.getDiskQuota();
		userDiskQuota -= userDiskUsage;
		for (Benchmark b : oldBenches) {
			userDiskQuota -= b.getDiskSize();
		}
		return userDiskQuota >= 0;
	}

	/**
	 * Checks to see whether a user can copy one set of subspaces from a space to another space \s	 * @param toSpaceId
	 * The ID of the space that new subspaces will be copied TO
	 *
	 * @param userId The ID of the user making the request
	 * @param subspaceIds The IDs of the subspaces that would be copied
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */

	public static ValidatorStatusCode canCopySpace(int toSpaceId, int userId, List<Integer> subspaceIds) {
		ValidatorStatusCode status = null;
		for (Integer sid : subspaceIds) {
			status = SpaceSecurity.canCopySpaceFromSpace(userId, sid);
			if (!status.isSuccess()) {
				return status;
			}
		}
		status = canCopySpaceToSpace(toSpaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}

		// Make sure the user can see the subSpaces they're trying to copy
		for (int id : subspaceIds) {
			// Make sure that the subspace has a unique name in the space.
			if (Spaces.notUniquePrimitiveName(Spaces.get(id).getName(), toSpaceId)) {
				return new ValidatorStatusCode(
						false, "The name of the space you are trying to copy is not unique in the destination space");
			}
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a user can link jobs from a space to another space
	 *
	 * @param fromSpaceId The ID of the spaces that jobs are being copied FROM
	 * @param toSpaceId The ID of the space that new jobs will be copied TO
	 * @param userId The ID of the user making the request
	 * @param jobIdsBeingCopied The IDs of the jobs that would be copied
	 * @return new ValidatorStatusCode(true) if allowed, or a status code from ValidatorStatusCodes if not
	 */

	public static ValidatorStatusCode canLinkJobsBetweenSpaces(
			Integer fromSpaceId, int toSpaceId, int userId, List<Integer> jobIdsBeingCopied
	) {

		ValidatorStatusCode status = null;
		boolean isAdmin = GeneralSecurity.hasAdminWritePrivileges(userId);
		if (fromSpaceId != null) {
			for (Integer jid : jobIdsBeingCopied) {
				status = canCopyJobFromSpace(fromSpaceId, userId, jid);
				if (!status.isSuccess()) {
					return status;
				}
			}
		} else {
			for (Integer jid : jobIdsBeingCopied) {
				Job j = Jobs.get(jid);
				if (j == null) {
					return new ValidatorStatusCode(false, "The given job could not be found");
				}
				if (j.getUserId() != userId && !isAdmin) {
					return new ValidatorStatusCode(
							false, "You are not the owner of all the jobs you are trying to move");
				}
			}
		}

		status = canCopyJobToSpace(toSpaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}


		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a list of benchmarks can be copied or linked from one space to another
	 *
	 * @param fromSpaceId The ID of the space the benchmarks are already in
	 * @param toSpaceId The ID of the space the benchmarks would be placed in
	 * @param userId The ID of the user making the request
	 * @param benchmarkIdsBeingCopied The IDs of the benchmarks that would be copied or linked
	 * @param copy If true, the primitives are being copied. Otherwise, they are being linked
	 * @return new ValidatorStatusCode(true) if the operation is allowed, and a status code from ValidatorStatusCodes
	 * otherwise
	 */

	public static ValidatorStatusCode canCopyOrLinkBenchmarksBetweenSpaces(
			Integer fromSpaceId, int toSpaceId, int userId, List<Integer> benchmarkIdsBeingCopied, boolean copy
	) {
		if (copy && UploadSecurity.uploadsFrozen()) {
 			return new ValidatorStatusCode(false, "Copying benchmarks is currently disabled by the system administrator");
 		}

		boolean isAdmin = GeneralSecurity.hasAdminWritePrivileges(userId);
		ValidatorStatusCode status = null;
		if (fromSpaceId != null) {
			for (Integer bid : benchmarkIdsBeingCopied) {
				//first make sure the user can copy each benchmark FROM the original space
				status = canCopyBenchmarkFromSpace(fromSpaceId, userId, bid);
				if (!status.isSuccess()) {
					return status;
				}
			}
		} else {
			for (Integer bid : benchmarkIdsBeingCopied) {
				Benchmark b = Benchmarks.get(bid);
				if (b == null || (b.getUserId() != userId && !isAdmin)) {
					return new ValidatorStatusCode(
							false,
							"You are not the owner of all the benchmarks you are trying to move, or some do not exist"
					);
				}
			}
		}

		//then, check to make sure they are allowed to copy TO the new space
		status = canCopyBenchmarkToSpace(toSpaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}

		//if we are copying, but not linking, disk quota must be checked
		if (copy) {
			if (!doesUserHaveDiskQuotaForBenchmarks(benchmarkIdsBeingCopied, userId)) {
				return new ValidatorStatusCode(
						false, "You do not have enough disk quota space to copy the benchmark(s)");
			}
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a list of solvers can be copied or linked from one space to another
	 *
	 * @param fromSpaceId The ID of the space the solvers are already in
	 * @param toSpaceId The ID of the space the solvers would be placed in
	 * @param userId The ID of the user making the request
	 * @param solverIdsBeingCopied The IDs of the solvers that would be copied or linked
	 * @param hierarchy If true, the copy will take place in the entire hierarchy rooted at the space with ID toSpaceId
	 * @param copy If true, the primitives are being copied. Otherwise, they are being linked
	 * @return new ValidatorStatusCode(true) if the operation is allowed, and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canCopyOrLinkSolverBetweenSpaces(
			Integer fromSpaceId, int toSpaceId, int userId, List<Integer> solverIdsBeingCopied, boolean hierarchy,
			boolean copy
	) {
		//if we are copying, but not linking, make sure the user has enough disk space
		if (copy) {
			if (UploadSecurity.uploadsFrozen()) {
 				return new ValidatorStatusCode(false, "Copying solvers is currently disabled by the system administrator");
 			}

			List<Solver> solvers = Solvers.get(solverIdsBeingCopied);
			int index = 0;
			for (Solver s : solvers) {
				if (s == null) {
					return new ValidatorStatusCode(
							false, "The following solver could not be found. ID = " + solverIdsBeingCopied.get(index));
				}
				if (s.buildStatus().getCode() == SolverBuildStatusCode.UNBUILT) {
					return new ValidatorStatusCode(false, "Solvers cannot be copied until they are finished building");
				}
				index++;
			}

			if (!doesUserHaveDiskQuotaForSolvers(solvers, userId).isSuccess()) {
				return new ValidatorStatusCode(false, "You do not have enough disk quota space to copy the solver(s)");
			}
		}

		List<Integer> spaceIds = new ArrayList<>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);

		//if we are checking the hierarchy, we must first get every space in it
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(toSpaceId, userId));
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}

		ValidatorStatusCode status = null;
		//we only need to check this if we are actually copying from another space
		if (fromSpaceId != null) {
			for (Integer sid : solverIdsBeingCopied) {
				//make sure the user is allowed to copy solvers FROM the original space
				status = canCopySolverFromSpace(fromSpaceId, userId, sid);
				if (!status.isSuccess()) {
					return status;
				}
			}
		} else {
			for (Integer sid : solverIdsBeingCopied) {
				if (!SolverSecurity.canUserDownloadSolver(sid, userId).isSuccess()) {
					return new ValidatorStatusCode(false,
					                               "You do not have permission to download all the solvers you are trying to copy.");
				}
			}
		}


		//then, for every destination space, make sure they can copy TO that space
		for (Integer spaceId : spaceIds) {
			status = canCopySolverToSpace(spaceId, userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a list of users can be copied from one space to another
	 *
	 * @param toSpaceId The ID of the space the users would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param userIdsBeingCopied The IDs of the users that would be copied
	 * @param hierarchy If true, the copy will take place in the entire hierarchy rooted at the space with ID toSpaceId
	 * @return new ValidatorStatusCode(true) if the operation is allowed, and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canCopyUserBetweenSpaces(
			int toSpaceId, int userIdDoingCopying, List<Integer> userIdsBeingCopied, boolean hierarchy
	) {
		List<Integer> spaceIds = new ArrayList<>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userIdDoingCopying,
			                                             Spaces.getSubSpaceHierarchy(toSpaceId, userIdDoingCopying)
			);
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}
		ValidatorStatusCode status = null;

		for (Integer sid : spaceIds) {
			for (Integer uid : userIdsBeingCopied) {
				status = canAddUserToSpace(sid, userIdDoingCopying, uid);
				if (!status.isSuccess()) {
					return status;
				}
			}
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can copy a user into another space
	 *
	 * @param spaceId The ID of the space where the new user would be PLACED.
	 * @param userIdMakingRequest The ID of the user making the request
	 * @param userIdToAdd The user that would be added to a new space
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canAddUserToSpace(int spaceId, int userIdMakingRequest, int userIdToAdd) {
		// Check permissions, the user must have add user permissions in the destination space
		Permission perm = Permissions.get(userIdMakingRequest, spaceId);
		if (perm == null || !perm.canAddUser()) {
			return new ValidatorStatusCode(false, "You do not have permission to add a user to this space");
		}

		if (!Users.isMemberOfCommunity(userIdToAdd, Spaces.getCommunityOfSpace(spaceId)) &&
				!GeneralSecurity.hasAdminWritePrivileges(userIdMakingRequest)) {
			return new ValidatorStatusCode(
					false,
					"The user is not a member of the community you are trying to move them to. They must request to " +
							"join the community first"
			);
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can copy a solver into another space
	 *
	 * @param spaceId The ID of the space where the new solver would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canCopySolverToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canAddSolver()) {
			return new ValidatorStatusCode(false, "You do not have permission to add a solver to this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can copy a benchmark into another space
	 *
	 * @param spaceId The ID of the space where the new benchmark would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canCopyBenchmarkToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canAddBenchmark()) {
			return new ValidatorStatusCode(false, "You do not have permission to add a benchmark to this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can copy a job into another space
	 *
	 * @param spaceId The ID of the space where the new job would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canCopyJobToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canAddJob()) {
			return new ValidatorStatusCode(false, "You do not have permission to add a job to this space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can copy a space into another space
	 *
	 * @param spaceId The ID of the space where the new space would be PLACED. This is NOT the ID of the space that
	 * would be moved.
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canCopySpaceToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.canAddSpace()) {
			return new ValidatorStatusCode(false, "You do not have permission to add a subspace to this space");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether a list of users can all be removed from the given space
	 *
	 * @param userIdsBeingRemoved The IDs of the users that would be removed
	 * @param userIdDoingRemoval The ID of the user making the request
	 * @param rootSpaceId The space ID to check
	 * @param hierarchy If true, checks to see whether the users can be removed from the entire hierarchy
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canRemoveUsersFromSpaces(
			List<Integer> userIdsBeingRemoved, int userIdDoingRemoval, int rootSpaceId, boolean hierarchy
	) {
		try {
			Permission perm = Permissions.get(userIdDoingRemoval, rootSpaceId);
			if (perm == null || !perm.canRemoveUser()) {
				return new ValidatorStatusCode(false, "You do not have permission to remove a user from this space");
			}

			if (!GeneralSecurity.hasAdminWritePrivileges(userIdDoingRemoval)) {
				// Validate the list of users to remove by:
				// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the
				// list of users to remove
				// 2 - Ensuring other leaders of the space aren't in the list of users to remove
				for (int userId : userIdsBeingRemoved) {
					if (userId == userIdDoingRemoval) {
						return new ValidatorStatusCode(false, "You cannot remove yourself from a space in this way");
					}
					perm = Permissions.get(userId, rootSpaceId);
					if (perm != null && perm.isLeader()) {
						return new ValidatorStatusCode(
								false, "You do not have permission to remove a leader from this space");
					}
				}
			}
			if (hierarchy) {
				List<Space> subspaces = Spaces.trimSubSpaces(userIdDoingRemoval,
				                                             Spaces.getSubSpaceHierarchy(rootSpaceId,
				                                                                         userIdDoingRemoval
				                                             )
				);
				// Iterate once through all subspaces of the destination space to ensure the user has removeUser
				// permissions in each
				for (Space subspace : subspaces) {

					ValidatorStatusCode status =
							canRemoveUsersFromSpaces(userIdsBeingRemoved, userIdDoingRemoval, subspace.getId(), false);
					if (!status.isSuccess()) {
						return status;
					}
				}
			}
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
	}

	/**
	 * Checks whether the given user is allowed to see the current new community requests
	 *
	 * @param userId The ID of the user in question
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserViewCommunityRequests(int userId) {
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to perform this operation");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether the given user is allowed to see the current new community requests
	 *
	 * @param userId The ID of the user in question
	 * @param communityId The ID of the community to get requests for
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUserViewCommunityRequestsForCommunity(int userId, int communityId) {
		Permission perm = Permissions.get(userId, communityId);
		//must be a leader to make a space public
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return new ValidatorStatusCode(true);
		} else if (!(perm == null) && perm.isLeader()) {
			return new ValidatorStatusCode(true);
		} else {
			return new ValidatorStatusCode(false, "Only community leaders may view requests to join communities");
		}
	}

	/**
	 * Checks whether a user can update whether a space is public or private
	 *
	 * @param spaceId The ID of the space being checked
	 * @param userId The ID  of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canSetSpacePublicOrPrivate(int spaceId, int userId) {
		Permission perm = Permissions.get(userId, spaceId);
		//must be a leader to make a space public
		if (perm == null || !perm.isLeader()) {
			return new ValidatorStatusCode(
					false, "You do not have permission to affect whether this space is public " + "or private");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the permissions of one user in a particular space's hierarchy can be updated by another
	 * user
	 *
	 * @param spaceId The ID of the space in question
	 * @param userIdBeingUpdated The ID of the user who would have their permissions updated
	 * @param requestUserId The Id of the user making the request
	 * @return list of spaces where permissions can be changed
	 */
	public static List<Integer> getUpdatePermissionSpaces(int spaceId, int userIdBeingUpdated, int requestUserId) {
		//TODO :  make more efficient? (right now querying database for every space in hierarchy to check permissions)


		List<Integer> spaceIds = new ArrayList<>(); //all the spaceIds of spaces being copied to
		spaceIds.add(spaceId);


		List<Space> subspaces =
				Spaces.trimSubSpaces(userIdBeingUpdated, Spaces.getSubSpaceHierarchy(spaceId, userIdBeingUpdated));

		for (Space s : subspaces) {
			spaceIds.add(s.getId());
		}

		List<Integer> permittedSpaceIds = new ArrayList<>();
		for (Integer sid : spaceIds) {
			ValidatorStatusCode status = canUpdatePermissions(sid, userIdBeingUpdated, requestUserId);
			if (status.isSuccess()) {
				permittedSpaceIds.add(sid);
			}
		}

		return permittedSpaceIds;
	}

	/**
	 * Checks to see whether the permissions of one user in a particular space can be updated by another user
	 *
	 * @param spaceId The ID of the space in question
	 * @param userIdBeingUpdated The ID of the user who would have their permissions updated
	 * @param requestUserId The Id of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes
	 * otherwise
	 */
	public static ValidatorStatusCode canUpdatePermissions(int spaceId, int userIdBeingUpdated, int requestUserId) {


		Permission perm = Permissions.get(requestUserId, spaceId);
		if (perm == null || !perm.isLeader()) {
			return new ValidatorStatusCode(false, "You do not have permission to update permissions here");
		}


		// Ensure the user to edit the permissions of isn't themselves a leader
		perm = Permissions.get(userIdBeingUpdated, spaceId);
		if (perm == null) {
			return new ValidatorStatusCode(false, "The given user is not a member of the given space");
		}
		if (perm.isLeader() && !GeneralSecurity.hasAdminWritePrivileges(requestUserId) &&
				Communities.isCommunity(spaceId)) {
			return new ValidatorStatusCode(false, "You do not have permission to update permissions for a leader " +
					"here");
		}


		return new ValidatorStatusCode(true);
	}

	public static ValidatorStatusCode canUserLinkAllOrphaned(int userId, int userIdOfCaller, int spaceId) {
		if (Users.get(userId) == null) {
			return new ValidatorStatusCode(false, "The given user could not be found");
		}
		if (!GeneralSecurity.hasAdminWritePrivileges(userIdOfCaller) && userId != userIdOfCaller) {
			return new ValidatorStatusCode(false, "You can only perform this operation on your own primitives");
		}
		Space s = Spaces.getDetails(spaceId, userIdOfCaller);
		if (s == null) {
			return new ValidatorStatusCode(false, "The given space could not be found");
		}

		Permission p = Permissions.get(userIdOfCaller, spaceId);
		if (!p.canAddJob() || !p.canAddBenchmark() || !p.canAddSolver()) {
			return new ValidatorStatusCode(false, "You do not have permissions to add primitives to the space");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the user is allowed to download the Json object representing the space
	 *
	 * @param spaceId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canGetJsonSpace(int spaceId, int userId) {
		if (!Permissions.canUserSeeSpace(spaceId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the specified space");
		}
		Space s = Spaces.get(spaceId);
		if (s == null) {
			return new ValidatorStatusCode(false, "The given space could not be found");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks whether a user can update the default settings (default timeouts, max-memory, etc.) of a community.
	 *
	 * @param spaceId The ID of the space that would have its settings changed
	 * @param attribute The name of the setting being changed
	 * @param newValue The new value that would be given to the setting
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */

	//TODO: Consider how to handle where to use the Validator class
	public static ValidatorStatusCode canUpdateSettings(int spaceId, String attribute, String newValue, int userId) {

		Permission perm = Permissions.get(userId, spaceId);
		if (perm == null || !perm.isLeader()) {
			return new ValidatorStatusCode(false, "Only leaders can update settings in a space");
		}

		Space s = Spaces.get(spaceId);
		// Go through all the cases, depending on what attribute we are changing.
		if (attribute.equals("name")) {


			if (!s.getName().equals(newValue)) {
				if (Spaces.notUniquePrimitiveName(newValue, spaceId)) {
					return new ValidatorStatusCode(false, "The new name needs to be unique in the space");
				}
			}
		} else if (attribute.equals("description")) {
			if (!Validator.isValidPrimDescription(newValue)) {
				return new ValidatorStatusCode(
						false,
						"The description is not in a valid format. Please refer to the help pages to see the correct " +
								"format"
				);
			}
		}


		return new ValidatorStatusCode(true);
	}

	public static boolean canUserSeeSpaceXMLStatus(int statusId, int userId) {
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return true;
		}
		SpaceXMLUploadStatus status = Uploads.getSpaceXMLStatus(statusId);
		return status.getUserId() == userId;
	}
}
