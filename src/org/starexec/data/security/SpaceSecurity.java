package org.starexec.data.security;

import java.util.ArrayList;
import java.util.List;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.util.SessionUtil;

public class SpaceSecurity {
	
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
			status=canCopyUserToSpace(sid,userIdDoingCopying);
			if (status!=0) {
				return status;
			}
		}
		
		return 0;
	}
	
	
	private static int canCopyUserToSpace(int spaceId, int userId) {
		
		// Check permissions, the user must have add user permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
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
	
	
}
