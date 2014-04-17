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
import org.starexec.data.database.Websites;
import org.starexec.data.database.Websites.WebsiteType;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Website;
import org.starexec.util.SessionUtil;
import org.starexec.util.Validator;

public class SpaceSecurity {
	
	/**
	 * Checks to see whether the given user is allowed to associate a website with a space
	 * @param spaceId The ID of the space that would be given a website
	 * @param userId The ID of the user making the request
	 * @param name The name to be given the new website
	 * @param URL The URL of the new website
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canAssociateWebsite(int spaceId,int userId, String name, String URL){
		Permission p=Permissions.get(userId, spaceId);
		if (p==null || !p.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (!Validator.isValidPrimName(name)  || !Validator.isValidWebsite(URL)) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can view websites associated with the given space
	 * @param spaceId The ID of the space being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static int canViewWebsites(int spaceId, int userId) {
		
		if(!Permissions.canUserSeeSpace(spaceId, userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a space
	 * @param spaceId The ID of the space that contains the site to be deleted
	 * @param websiteId The ID of the website to be deleted
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canDeleteWebsite(int spaceId,int websiteId,int userId){
		Permission p=Permissions.get(userId, spaceId);
		if (p==null || !p.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		List<Website> websites=Websites.getAllForJavascript(spaceId,WebsiteType.SPACE);
		boolean websiteInSpace=false;
		for (Website w : websites) {
			if (w.getId()==websiteId) {
				websiteInSpace=true;
				break;
			}
		}
		
		if (!websiteInSpace) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether a user can update the properties of a space (such as default permissions, sticky leaders,
	 * and so on)
	 * @param spaceId The ID of the space being updated
	 * @param userId The ID of the user making the request
	 * @param name The name the space would have upon updating 
	 * @param stickyLeaders The value stickyLeaders would have upon updating
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
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
	/**
	 * Checks to see whether a user may both remove and recycle benchmarks in a space
	 * @param benchmarkIds The IDs of the benchmarks that would be removed and recycled
	 * @param spaceId The ID of the space containing the benchmarks
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveAndRecycleBenchmarks(List<Integer> benchmarkIds, int spaceId, int userId) {
		if (canUserRemoveBenchmark(spaceId, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}

		return 0;
	}
	
	/**
	 * Checks to see whether a user may both remove and delete jobs in a space
	 * @param jobIds The IDs of the jobs that would be removed and recycled
	 * @param spaceId The ID of the space containing the jobs
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveAndDeleteJobs(List<Integer> jobIds, int spaceId, int userId) {
		if (canUserRemoveJob(spaceId, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (JobSecurity.canUserDeleteJobs(jobIds, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}

		return 0;
	}
	
	/**
	 * Checks to see whether a user may both remove and delete solvers in a space
	 * @param solverIds The IDs of the solvers that would be removed and recycled
	 * @param spaceId The ID of the space containing the solvers
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static int canUserRemoveAndRecycleSolvers(List<Integer> solverIds, int spaceId, int userId) {
		if (canUserRemoveSolver(spaceId, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (SolverSecurity.canUserRecycleSolvers(solverIds, userId)!=0) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	/**
	 * Checks whether a user may remove a benchmark from a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveBenchmark(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveBench()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;

	}
	
	//TODO: Why isn't this using the perm.canRemoveSpace instead?
	public static int canUserRemoveSpace(int spaceId, int userId) {
		Permission perm = Permissions.get(userId, spaceId);		
		if(null == perm || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
	}
	
	//TODO: Is this really right? Why can one leader demote another?
	public static int canDemoteLeader(int spaceId, int userIdBeingDemoted, int userIdDoingDemoting) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(spaceId,userIdDoingDemoting);
		if(perm == null || !perm.isLeader()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
	}
	
	/**
	 * Checks whether a user may remove a job from a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveJob(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveJob()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;

	}
	/**
	 * Checks whether a user may remove a solver from a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveSolver(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveSolver()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;

	}
	
	/**
	 * Checks whether a user may remove a space from another space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserLeaveSpace(int spaceId, int userId){
		//the user can leave if they are in the space
		if(!Users.isMemberOfSpace(userId, spaceId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
	}
	
	/**
	 * Checks whether a user can update the default settings (default timeouts, max-memory, etc.) of a 
	 * community.
	 * @param spaceId The ID of the space that would have its settings changed
	 * @param attribute The name of the setting being changed
	 * @param newValue The new value that would be given to the setting
	 * @param userId The ID of the user making the request
	 * @return0 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
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
	/**
	 * Checks to see whether the given user can see the given space
	 * @param spaceId The ID of the space in question 
	 * @param userId The ID of the user who wants to see the space
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
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
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static int canCopyPrimFromSpace(int spaceId, int userIdDoingCopying) {
		if (Users.isAdmin(userIdDoingCopying)) {
			return 0;
		}
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
	/**
	 * Checks to see whether a user can copy a given benchmark from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param benchId The ID of the benchmark that would be copied
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
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
	/**
	 * Checks to see whether a user can copy a given space from the given space
	 * @param fromSpaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param spaceIdBeingCopied The ID of the space that would be copied
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
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
	
	/**
	 * Checks to see whether a user can copy a given job from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param jobId The ID of the job that would be copied
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
	
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
	/**
	 * Checks to see whether a user can copy a given solver from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param solverId The ID of the solver that would be copied
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
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
	/**
	 * Checks whether the user has enough disk quota to fit all of a list of solvers
	 * @param solverIds The solver IDs that would be added
	 * @param userId The ID of the user in question
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
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
	
	/**
	 * Checks whether the user has enough disk quota to fit all of a list of benchmarks
	 * @param benchmarkIds The solver IDs that would be added
	 * @param userId The ID of the user in question
	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
	
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
	
	/**
	 * Checks to see whether a user can copy one set of subspaces from a space to another space
	 * @param fromSpaceId The ID of the spaces that subspaces are being copied FROM
	 * @param toSpaceId The ID of the space that new subspaces will be copied TO
	 * @param userId The ID of the user making the request
	 * @param subspaceIds The IDs of the subspaces that would be copied
 	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
	
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
	
	/**
	 * Checks to see whether a user can link jobs from a space to another space
	 * @param fromSpaceId The ID of the spaces that jobs are being copied FROM
	 * @param toSpaceId The ID of the space that new jobs will be copied TO
	 * @param userId The ID of the user making the request
	 * @param jobIds The IDs of the jobs that would be copied
 	 * @return 0 if allowed, or a status code from SecurityStatusCodes if not
	 */
	
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
	
	/**
	 * Checks to see whether a list of benchmarks can be copied or linked from one space to another
	 * @param fromSpaceId The ID of the space the benchmarks are already in
	 * @param toSpaceId The ID of the space the benchmarks would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param benchmarkIdsBeingCopied The IDs of the benchmarks that would be copied or linked
	 * @param copy If true, the primitives are being copied. Otherwise, they are being linked
	 * @return 0 if the operation is allowed, and a status code from SecurityStatusCodes otherwise
	 */
	
	public static int canCopyOrLinkBenchmarksBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> benchmarkIdsBeingCopied,boolean copy) {
		//if we are copying, but not linking, disk quota must be checked
		if (copy) {
			if (!doesUserHaveDiskQuotaForBenchmarks(benchmarkIdsBeingCopied,userId)) {
				return SecurityStatusCodes.ERROR_INSUFFICIENT_QUOTA;
			}
		}
		int status=0;
		for(Integer sid : benchmarkIdsBeingCopied) {
			//first make sure the user can copy each benchmark FROM the original space
			status=	canCopyBenchmarkFromSpace(fromSpaceId,userId,sid);
			if (status!=0) {
				return status;
			}
		}
		//then, check to make sure they are allowed to copy TO the new space
		status=canCopyBenchmarkToSpace(toSpaceId,userId);
		if (status!=0) {
			return status;
		}
		
		//benchmark names must be unique in each space
		for (Integer benchId : benchmarkIdsBeingCopied) { 
			if(Spaces.notUniquePrimitiveName(Benchmarks.get(benchId).getName(), toSpaceId, 1)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
			
		return 0;
	}
	
	/**
	 * Checks to see whether a list of solvers can be copied or linked from one space to another
	 * @param fromSpaceId The ID of the space the solvers are already in
	 * @param toSpaceId The ID of the space the solvers would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param solverIdsBeingCopied The IDs of the solvers that would be copied or linked
	 * @param hierarchy If true, the copy will take place in the entire hierarchy rooted at the space with ID toSpaceId
	 * @param copy If true, the primitives are being copied. Otherwise, they are being linked
	 * @return 0 if the operation is allowed, and a status code from SecurityStatusCodes otherwise
	 */
	public static int canCopyOrLinkSolverBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> solverIdsBeingCopied, boolean hierarchy,boolean copy) {
		//if we are copying, but not linking, make sure the user has enough disk space
		if (copy) {
			if (!doesUserHaveDiskQuotaForSolvers(solverIdsBeingCopied,userId)) {
				return SecurityStatusCodes.ERROR_INSUFFICIENT_QUOTA;
			}
		}
		
		List<Integer> spaceIds=new ArrayList<Integer>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);
		
		//if we are checking the hierarchy, we must first get every space in it
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaces(toSpaceId, userId, true));
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}
		
		int status=0;
		for(Integer sid : solverIdsBeingCopied) {
			//make sure the user is allowed to copy solvers FROM the original space
			status=	canCopySolverFromSpace(fromSpaceId,userId,sid);
			if (status!=0) {
				return status;
			}
		}
		
		//then, for every destination space, make sure they can copy TO that space
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
	/**
	 * Checks to see whether a list of users can be copied from one space to another
	 * @param fromSpaceId The ID of the space the users are already in
	 * @param toSpaceId The ID of the space the users would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param userIdsBeingCopied The IDs of the users that would be copied
	 * @param hierarchy If true, the copy will take place in the entire hierarchy rooted at the space with ID toSpaceId
	 * @return 0 if the operation is allowed, and a status code from SecurityStatusCodes otherwise
	 */
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
	
	/**
	 * Checks to see whether the given user can copy a user into another space
	 * @param spaceId The ID of the space where the new user would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canAddUserToSpace(int spaceId, int userIdMakingRequest) {
		// Check permissions, the user must have add user permissions in the destination space
		Permission perm = Permissions.get(userIdMakingRequest, spaceId);		
		if(perm == null || !perm.canAddUser()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}

		return 0;
	}
	
	/**
	 * Checks to see whether the given user can copy a solver into another space
	 * @param spaceId The ID of the space where the new solver would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static int canCopySolverToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddSolver()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can copy a benchmark into another space
	 * @param spaceId The ID of the space where the new benchmark would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static int canCopyBenchmarkToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddBenchmark()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can copy a job into another space
	 * @param spaceId The ID of the space where the new job would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static int canCopyJobToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddJob()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can copy a space into another space
	 * @param spaceId The ID of the space where the new space would be PLACED. This
	 * is NOT the ID of the space that would be moved.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static int canCopySpaceToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddSpace()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}	
		
		return 0;
	}
	
	
	/**
	 * Checks to see whether a list of users can all be removed from the given space
	 * @param userIdsBeingRemoved The IDs of the users that wouldbe removed
	 * @param userIdDoingRemoval The ID of the user making the request
	 * @param rootSpaceId The space ID to check
	 * @param hierarchy If true, checks to see whether the users can be removed from the entire hierarchy
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
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
	
	/**
	 * Checks whether the given user is allowed to see the current new community requests
	 * @param userId The ID of the user in question
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserViewCommunityRequests(int userId) {
		if (!Users.isAdmin(userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks whether a user can update whether a space is public or private
	 * @param spaceId The ID of the space being checked
	 * @param userId The ID  of the user making the request 
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canSetSpacePublicOrPrivate(int spaceId,int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		//must be a leader to make a space public
		if (perm == null || !perm.isLeader()){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether the permissions of one user in a particular space can be updated by another user
	 * @param spaceId The ID of the space in question
	 * @param userIdBeingUpdated The ID of the user who would have their permissions updated
	 * @param requestUserId The Id of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
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
