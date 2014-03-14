package org.starexec.data.security;

import java.util.ArrayList;
import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.util.Validator;

public class SpaceSecurity {
	
	public static int canAssociateWebsite(int spaceId,int userId){
		Permission p=Permissions.get(userId, spaceId);
		if (p==null || !p.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUpdateProperties(int spaceId, int userId, String name, boolean stickyLeaders) {
		Permission perm = Permissions.get(userId, spaceId);
		if(perm == null || !perm.isLeader()){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		//communities are not allowed to have sticky leaders enabled
		if (Communities.isCommunity(spaceId) && stickyLeaders) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		Space os=Spaces.get(spaceId);
		if (!os.getName().equals(name)) {
			if (Spaces.notUniquePrimitiveName(name,spaceId,4)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
	
		return 0;
	}
	
	public static int canUserRemoveAndRecycleBenchmarks(List<Integer> benchmarkIds, int spaceId, int userId) {
		if (canUserRemoveBenchmark(spaceId, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		
		
		return 0;
	}
	
	public static int canUserRemoveAndDeleteJobs(List<Integer> jobIds, int spaceId, int userId) {
		if (canUserRemoveJob(spaceId, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (JobSecurity.canUserDeleteJobs(jobIds, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		
		
		return 0;
	}
	
	
	public static int canUserRemoveAndRecycleSolvers(List<Integer> solverIds, int spaceId, int userId) {
		if (canUserRemoveSolver(spaceId, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (SolverSecurity.canUserRecycleSolvers(solverIds, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		
		
		return 0;
	}
	
	
	public static int canUserRemoveBenchmark(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveBench()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;

	}
	
	public static int canUserRemoveJob(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveJob()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;

	}
	
	public static int canUserRemoveSolver(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveSolver()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;

	}
	
	public static int canUserLeaveSpace(int spaceId, int userId){
		//the user can leave if they are in the space
		if(!Users.isMemberOfSpace(userId, spaceId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
	}
	
	//TODO: Consider how to handle where to use the Validator class
	public static int canUpdateSettings(int spaceId, String attribute, String newValue, int userId) {
		
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		
		Space s=Spaces.get(spaceId);
		// Go through all the cases, depending on what attribute we are changing.
		if (attribute.equals("name")) {
			
			
			if (!s.getName().equals(newValue)) {
				if (Spaces.notUniquePrimitiveName(newValue,spaceId,4)) {
					return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
				}
			}
			
		} else if (attribute.equals("description")) {
			if (!Validator.isValidPrimDescription(newValue)) {
				return SecurityStatusCodes.ERROR_INVALID_PARAMS;				
			}
		}	
		
		return 0;
	}
	
	public static int canUserSeeSpace(int spaceId, int userId){
		if (!Permissions.canUserSeeSpace(spaceId, userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether a user can copy any primitive from the given space. They cannot copy
	 * if the space is locked or if they cannot see the space.
	 * @param spaceId The ID of the space being copied from
	 * @param userIdDoingCopying The ID of the user who is making the copy request.
	 * @return 0 if allowed, or a status code from SecurityStatusCodes on failure
	 */
	private static int canCopyPrimFromSpace(int spaceId, int userIdDoingCopying) {
		// And the space the user is being copied from must not be locked
		if(Spaces.get(spaceId).isLocked()) {
			return SecurityStatusCodes.ERROR_SPACE_LOCKED;
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(spaceId, userIdDoingCopying)) {
			return SecurityStatusCodes.ERROR_NOT_IN_SPACE;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether a user can copy a given user from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param userIdBeingCopied The ID of the user that would be copied
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
	private static int canCopyUserFromSpace(int spaceId, int userIdDoingCopying, int userIdBeingCopied) {
		
		
		int status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (status!=0) {
			return status;
		}
		//the user being copied should actually be in the space they are supposedly being copied from
		if (!Users.isMemberOfSpace(userIdBeingCopied, spaceId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	private static int canCopyBenchmarkFromSpace(int spaceId, int userIdDoingCopying, int benchId) {
		
		
		int status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (status!=0) {
			return status;
		}
		if(!Permissions.canUserSeeBench(benchId, userIdDoingCopying)) {
			return SecurityStatusCodes.ERROR_NOT_IN_SPACE;
		}
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			return SecurityStatusCodes.ERROR_PRIM_ALREADY_DELETED;
		}
		return 0;
	}
	
	private static int canCopySpaceFromSpace(int fromSpaceId, int userId, int spaceIdBeingCopied) {
		int status=canCopyPrimFromSpace(fromSpaceId,userId);
		if (status!=0) {
			return status;
		}
		
		if (!Permissions.canUserSeeSpace(spaceIdBeingCopied, userId)) {
			return SecurityStatusCodes.ERROR_NOT_IN_SPACE;
		}
		
		return 0;
	}
	
	
	
	private static int canCopyJobFromSpace(int spaceId, int userIdDoingCopying, int jobId) {
		
		
		int status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (status!=0) {
			return status;
		}
		if(!Permissions.canUserSeeJob(jobId, userIdDoingCopying)) {
			return SecurityStatusCodes.ERROR_NOT_IN_SPACE;
		}
		
		if (Jobs.isJobDeleted(jobId)) {
			return SecurityStatusCodes.ERROR_PRIM_ALREADY_DELETED;
		}
		return 0;
	}
	
	private static int canCopySolverFromSpace(int spaceId, int userIdDoingCopying, int solverId) {
		int status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (status!=0) {
			return status;
		}
		
		//the solver being copied should actually be in the space they are supposedly being copied from
		if (!Permissions.canUserSeeSolver(solverId, userIdDoingCopying)) {
			return SecurityStatusCodes.ERROR_NOT_IN_SPACE;
		}
		
		//we can't copy a solver if it has been deleted on disk
		if (Solvers.isSolverDeleted(solverId)) {
			return SecurityStatusCodes.ERROR_PRIM_ALREADY_DELETED;
		}

		return 0;
	}
	
	private static boolean doesUserHaveDiskQuotaForSolvers(List<Integer> solverIds,int userId) {
		List<Solver> oldSolvers=Solvers.get(solverIds);
		//first, validate that the user has enough disk quota to copy all the selected solvers
		//we don't copy any unless they have room for all of them
		long userDiskUsage=Users.getDiskUsage(userId);
		long userDiskQuota=Users.get(userId).getDiskQuota();
		userDiskQuota-=userDiskUsage;
		for (Solver s : oldSolvers) {
			userDiskQuota-=s.getDiskSize();
		}
		if (userDiskQuota<0) {
			return false;
		}
		return true;
	}
	
	private static boolean doesUserHaveDiskQuotaForBenchmarks(List<Integer> benchIDs, int userId) {
		List<Benchmark> oldBenches=Benchmarks.get(benchIDs);
		//first, validate that the user has enough disk quota to copy all the selected solvers
		//we don't copy any unless they have room for all of them
		long userDiskUsage=Users.getDiskUsage(userId);
		long userDiskQuota=Users.get(userId).getDiskQuota();
		userDiskQuota-=userDiskUsage;
		for (Benchmark b : oldBenches) {
			userDiskQuota-=b.getDiskSize();
		}
		if (userDiskQuota<0) {
			return false;
		}
		return true;
	}
	
	public static int canCopySpace(int fromSpaceId, int toSpaceId, int userId, List<Integer> subspaceIds) {
		int status=0;
		for (Integer sid : subspaceIds) {
			status=SpaceSecurity.canCopySpaceFromSpace(fromSpaceId, userId, sid);
			if (status!=0) {
				return status;
			}
		}
		status=canCopySpaceToSpace(toSpaceId,userId);
		if (status!=0) {
			return status;
		}
		
		// Make sure the user can see the subSpaces they're trying to copy
		for (int id : subspaceIds) {		
			// Make sure that the subspace has a unique name in the space.
			if(Spaces.notUniquePrimitiveName(Spaces.get(id).getName(), toSpaceId, 4)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
	
		return 0;
	}
	
	public static int canLinkJobsBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> jobIdsBeingCopied) {
		
		int status=0;
		for(Integer jid : jobIdsBeingCopied) {
			status=	canCopyJobFromSpace(fromSpaceId,userId,jid);
			if (status!=0) {
				return status;
			}
		}
		
		status=canCopyJobToSpace(toSpaceId,userId);
		if (status!=0) {
			return status;
		}
		for (Integer jobId : jobIdsBeingCopied) { 
			if(Spaces.notUniquePrimitiveName(Jobs.get(jobId).getName(), toSpaceId, 3)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
			
		return 0;
	}
	
	
	
	public static int canCopyOrLinkBenchmarksBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> benchmarkIdsBeingCopied,boolean copy) {
	
		if (copy) {
			if (!doesUserHaveDiskQuotaForBenchmarks(benchmarkIdsBeingCopied,userId)) {
				return SecurityStatusCodes.ERROR_INSUFFICIENT_QUOTA;
			}
		}
		int status=0;
		for(Integer sid : benchmarkIdsBeingCopied) {
			status=	canCopyBenchmarkFromSpace(fromSpaceId,userId,sid);
			if (status!=0) {
				return status;
			}
		}
		
		status=canCopyBenchmarkToSpace(toSpaceId,userId);
		if (status!=0) {
			return status;
		}
		for (Integer benchId : benchmarkIdsBeingCopied) { 
			if(Spaces.notUniquePrimitiveName(Benchmarks.get(benchId).getName(), toSpaceId, 1)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
			
		return 0;
	}
	
	public static int canCopyOrLinkSolverBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> solverIdsBeingCopied, boolean hierarchy,boolean copy) {
		
		List<Integer> spaceIds=new ArrayList<Integer>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaces(toSpaceId, userId, true));
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}
		if (copy) {
			if (!doesUserHaveDiskQuotaForSolvers(solverIdsBeingCopied,userId)) {
				return SecurityStatusCodes.ERROR_INSUFFICIENT_QUOTA;
			}
		}
		int status=0;
		for(Integer sid : solverIdsBeingCopied) {
			status=	canCopySolverFromSpace(fromSpaceId,userId,sid);
			if (status!=0) {
				return status;
			}
		}
		
		for (Integer spaceId : spaceIds) {
			status=canCopySolverToSpace(spaceId,userId);
			if (status!=0) {
				return status;
			}
			for (Integer solverId : solverIdsBeingCopied) { 
				if(Spaces.notUniquePrimitiveName(Solvers.get(solverId).getName(), spaceId, 1)) {
					return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
				}
			}
			
		}
		return 0;
	}
	
	public static int canCopyUserBetweenSpaces(int fromSpaceId, int toSpaceId, int userIdDoingCopying, List<Integer> userIdsBeingCopied, boolean hierarchy) {
		List<Integer> spaceIds=new ArrayList<Integer>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userIdDoingCopying, Spaces.getSubSpaces(toSpaceId, userIdDoingCopying, true));
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}
		int status=0;
		for (Integer uid : userIdsBeingCopied) {
			status=canCopyUserFromSpace(fromSpaceId,userIdDoingCopying, uid);
			if (status!=0) {
				return status;
			}
		}
		for (Integer sid : spaceIds) {
			status=canAddUserToSpace(sid,userIdDoingCopying);
			if (status!=0) {
				return status;
			}
		}
		
		return 0;
	}
	
	
	public static int canAddUserToSpace(int spaceId, int userIdMakingRequest) {
		// Check permissions, the user must have add user permissions in the destination space
		Permission perm = Permissions.get(userIdMakingRequest, spaceId);		
		if(perm == null || !perm.canAddUser()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}

		return 0;
	}
	
	private static int canCopySolverToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddSolver()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		return 0;
	}
	
	private static int canCopyBenchmarkToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddBenchmark()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		return 0;
	}
	
	private static int canCopyJobToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddJob()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		return 0;
	}
	
	private static int canCopySpaceToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddSpace()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		
		return 0;
	}
	
	
	
	public static int canRemoveUsersFromSpaces(List<Integer> userIdsBeingRemoved, int userIdDoingRemoval, int rootSpaceId, boolean hierarchy) {
		
		Permission perm = Permissions.get(userIdDoingRemoval, rootSpaceId);
		if(perm == null || !perm.canRemoveUser()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		
		if (!Users.isAdmin(userIdDoingRemoval)) {
			// Validate the list of users to remove by:
			// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the list of users to remove
			// 2 - Ensuring other leaders of the space aren't in the list of users to remove
			for(int userId : userIdsBeingRemoved){
				if(userId == userIdDoingRemoval){
					return SecurityStatusCodes.ERROR_CANT_REMOVE_SELF;
				}
				perm = Permissions.get(userId, rootSpaceId);
				if(perm.isLeader()){
					return SecurityStatusCodes.ERROR_CANT_REMOVE_LEADER;
				}
			}
		}
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userIdDoingRemoval, Spaces.getSubSpaces(rootSpaceId, userIdDoingRemoval, true));
			// Iterate once through all subspaces of the destination space to ensure the user has removeUser permissions in each
			for(Space subspace : subspaces) {
				int status=canRemoveUsersFromSpaces(userIdsBeingRemoved,userIdDoingRemoval,subspace.getId(),false);
				if (status!=0) {
					return status;
				}	
			}
		}
			
		return 0;
	}
	
	public static int canUserViewCommunityRequests(int userId) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canSetSpacePublicOrPrivate(int spaceId,int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		//must be a leader to make a space public
		if (perm == null || !perm.isLeader()){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	public static int canUpdatePermissions(int spaceId, int userIdBeingUpdated, int requestUserId) {
		Permission perm = Permissions.get(requestUserId, spaceId);
		if(perm == null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		
		// Ensure the user to edit the permissions of isn't themselves a leader
		perm = Permissions.get(userIdBeingUpdated, spaceId);
		if(perm.isLeader() && !Users.isAdmin(requestUserId)){
			return SecurityStatusCodes.ERROR_CANT_EDIT_LEADER_PERMS;
		}		
		
		return 0;
	}

}
