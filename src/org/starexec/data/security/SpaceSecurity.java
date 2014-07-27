package org.starexec.data.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.app.RESTServices;
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
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.util.SessionUtil;
import org.starexec.util.Validator;

public class SpaceSecurity {
	private static final Logger log = Logger.getLogger(SpaceSecurity.class);			

	/**
	 * Checks to see whether the given user is allowed to associate a website with a space
	 * @param spaceId The ID of the space that would be given a website
	 * @param userId The ID of the user making the request
	 * @param name The name to be given the new website
	 * @param URL The URL of the new website
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canAssociateWebsite(int spaceId,int userId, String name, String URL){
		Permission p=Permissions.get(userId, spaceId);
		if (p==null || !p.isLeader()) {
			return new SecurityStatusCode(false, "You do not have permission to associate websites with this space");
		}
		
		if (!Validator.isValidWebsite(URL)) {
			return new SecurityStatusCode(false, "The given URL is not in the proper format. Please refer to the help pages to see the correct format");
		}
		
		if (!Validator.isValidPrimName(name)) {
			return new SecurityStatusCode(false, "The given name is not in the proper format. Please refer to the help pages to see the correct format");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can view websites associated with the given space
	 * @param spaceId The ID of the space being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static SecurityStatusCode canViewWebsites(int spaceId, int userId) {
		
		if(!Permissions.canUserSeeSpace(spaceId, userId)) {
			return new SecurityStatusCode(false, "You do not have permission to see this space");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a space
	 * @param spaceId The ID of the space that contains the site to be deleted
	 * @param websiteId The ID of the website to be deleted
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canDeleteWebsite(int spaceId,int websiteId,int userId){
		Permission p=Permissions.get(userId, spaceId);
		if (p==null || !p.isLeader()) {
			return new SecurityStatusCode(false, "You do not have permission to delete websites associated with this space");
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
			return new SecurityStatusCode(false, "The given website is not associated with the given space");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user can update the properties of a space (such as default permissions, sticky leaders,
	 * and so on)
	 * @param spaceId The ID of the space being updated
	 * @param userId The ID of the user making the request
	 * @param name The name the space would have upon updating 
	 * @param stickyLeaders The value stickyLeaders would have upon updating
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUpdateProperties(int spaceId, int userId, String name, boolean stickyLeaders) {
		Permission perm = Permissions.get(userId, spaceId);
		if(perm == null || !perm.isLeader()){
			return new SecurityStatusCode(false, "You do not have permission to update this space");
		}
		
		//communities are not allowed to have sticky leaders enabled
		if (Communities.isCommunity(spaceId) && stickyLeaders) {
			return new SecurityStatusCode(false, "Communitity spaces may not enable sticky leaders");
		}
		
		Space os=Spaces.get(spaceId);
		if (!os.getName().equals(name)) {
			if (Spaces.notUniquePrimitiveName(name,spaceId,4)) {
				return new SecurityStatusCode(false, "The new name needs to be unique in the space");
			}
		}
	
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether a user may both remove and recycle benchmarks in a space
	 * @param benchmarkIds The IDs of the benchmarks that would be removed and recycled
	 * @param spaceId The ID of the space containing the benchmarks
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveAndRecycleBenchmarks(List<Integer> benchmarkIds, int spaceId, int userId) {
		SecurityStatusCode status=canUserRemoveBenchmark(spaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		
		status=BenchmarkSecurity.canUserRecycleBenchmarks(benchmarkIds, userId);
		if (!status.isSuccess()) {
			return status;
		}

		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user may both remove and delete jobs in a space
	 * @param jobIds The IDs of the jobs that would be removed and recycled
	 * @param spaceId The ID of the space containing the jobs
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveAndDeleteJobs(List<Integer> jobIds, int spaceId, int userId) {
		SecurityStatusCode status = canUserRemoveJob(spaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}
		status  = JobSecurity.canUserDeleteJobs(jobIds, userId);
		if (!status.isSuccess()) {
			return status;
		}

		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user may both remove and delete solvers in a space
	 * @param solverIds The IDs of the solvers that would be removed and recycled
	 * @param spaceId The ID of the space containing the solvers
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static SecurityStatusCode canUserRemoveAndRecycleSolvers(List<Integer> solverIds, int spaceId, int userId) {
		SecurityStatusCode status=canUserRemoveSolver(spaceId, userId);
		if (!status.isSuccess()) {
			return status;
		}
		status = SolverSecurity.canUserRecycleSolvers(solverIds, userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks whether a user may remove a benchmark from a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveBenchmark(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveBench()) {
			return new SecurityStatusCode(false, "You do not have permission to remove benchmarks from this space");
		}
		return new SecurityStatusCode(true);

	}
	
	public static SecurityStatusCode canUserRemoveSpace(int spaceId, int userId) {
		Permission perm = Permissions.get(userId, spaceId);		
		if(null == perm || !perm.canRemoveSpace()) {
			return new SecurityStatusCode(false, "You do not have permission to remove subspaces from this space");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks whether the user demoting is the administrator 
	 * @param spaceId
	 * @param userIdBeingDemoted
	 * @param userIdDoingDemoting
	 * @return
	 */
	//TODO: Leaders can demote other leaders except at the community level, right?
	public static SecurityStatusCode canDemoteLeader(int spaceId, int userIdBeingDemoted, int userIdDoingDemoting) {
		// Permissions check; ensures user is the leader of the community or is an admin
		if(!Users.isAdmin(userIdDoingDemoting)) {
			return new SecurityStatusCode(false, "You do not have permission to demote leaders in this space");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks whether a user may remove a job from a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveJob(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveJob()) {
			return new SecurityStatusCode(false, "You do not have permission to remove jobs from this space");
		}
		return new SecurityStatusCode(true);

	}
	/**
	 * Checks whether a user may remove a solver from a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveSolver(int spaceId, int userId) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canRemoveSolver()) {
			return new SecurityStatusCode(false, "You do not have permission to remove solvers from this space");
		}
		return new SecurityStatusCode(true);

	}
	
	/**
	 * Checks whether a user may leave a space
	 * @param spaceId The ID of the space containing the primitive
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserLeaveSpace(int spaceId, int userId){
		//the user can leave if they are in the space
		if(!Users.isMemberOfSpace(userId, spaceId)) {
			return new SecurityStatusCode(false, "You are not currently in this space");
		}
		return new SecurityStatusCode(true);
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
	public static SecurityStatusCode canUpdateSettings(int spaceId, String attribute, String newValue, int userId) {
		
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.isLeader()) {
			return new SecurityStatusCode(false, "Only leaders can update settings in a space");
		}
		
		Space s=Spaces.get(spaceId);
		// Go through all the cases, depending on what attribute we are changing.
		if (attribute.equals("name")) {
			
			
			if (!s.getName().equals(newValue)) {
				if (Spaces.notUniquePrimitiveName(newValue,spaceId,4)) {
					return new SecurityStatusCode(false, "The new name needs to be unique in the space");
				}
			}
			
		} else if (attribute.equals("description")) {
			if (!Validator.isValidPrimDescription(newValue)) {
				return new SecurityStatusCode(false, "The description is not in a valid format. Please refer to the help pages to see the correct format");
			}
		}	
		
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can see the given space
	 * @param spaceId The ID of the space in question 
	 * @param userId The ID of the user who wants to see the space
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserSeeSpace(int spaceId, int userId){
		if (!Permissions.canUserSeeSpace(spaceId, userId)) {
			return new SecurityStatusCode(false, "You do not have permission to see this space");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user can copy any primitive from the given space. They cannot copy
	 * if the space is locked or if they cannot see the space.
	 * @param spaceId The ID of the space being copied from
	 * @param userIdDoingCopying The ID of the user who is making the copy request.
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static SecurityStatusCode canCopyPrimFromSpace(int spaceId, int userIdDoingCopying) {
		if (Users.isAdmin(userIdDoingCopying)) {
			return new SecurityStatusCode(true);
		}
		// And the space the user is being copied from must not be locked
		if(Spaces.get(spaceId).isLocked()) {
			return new SecurityStatusCode(false, "The space you are trying to copy from is locked");
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(spaceId, userIdDoingCopying)) {
			return new SecurityStatusCode(false, "You do not have permission to see the space you are copying from");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user can copy a given user from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param userIdBeingCopied The ID of the user that would be copied
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	private static SecurityStatusCode canCopyUserFromSpace(int spaceId, int userIdDoingCopying, int userIdBeingCopied) {
		
		
		SecurityStatusCode status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}
		//the user being copied should actually be in the space they are supposedly being copied from
		if (!Users.isMemberOfSpace(userIdBeingCopied, spaceId)) {
			return new SecurityStatusCode(false, "The user you are trying to move is not in the space you are copying from");
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether a user can copy a given benchmark from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param benchId The ID of the benchmark that would be copied
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	private static SecurityStatusCode canCopyBenchmarkFromSpace(int spaceId, int userIdDoingCopying, int benchId) {
		
		
		SecurityStatusCode status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}
		if(!Permissions.canUserSeeBench(benchId, userIdDoingCopying)) {
			return new SecurityStatusCode(false, "You are not allowed to see the benchmark you are trying to move");
		}
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			return new SecurityStatusCode(false, "The benchmark you are trying to copy has already been deleted");
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether a user can copy a given space from the given space
	 * @param fromSpaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param spaceIdBeingCopied The ID of the space that would be copied
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	private static SecurityStatusCode canCopySpaceFromSpace(int fromSpaceId, int userId, int spaceIdBeingCopied) {
		SecurityStatusCode status=canCopyPrimFromSpace(fromSpaceId,userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		if (!Permissions.canUserSeeSpace(spaceIdBeingCopied, userId)) {
			return new SecurityStatusCode(false, "The subspace you are trying to move is not in the space you are copying from");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user can copy a given job from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param jobId The ID of the job that would be copied
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	
	private static SecurityStatusCode canCopyJobFromSpace(int spaceId, int userIdDoingCopying, int jobId) {
		
		
		SecurityStatusCode status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}
		if(!Permissions.canUserSeeJob(jobId, userIdDoingCopying)) {
			return new SecurityStatusCode(false, "You are not allowed to see the job you are trying to move");
		}
		
		if (Jobs.isJobDeleted(jobId)) {
			return new SecurityStatusCode(false, "The job you are trying to copy has already been deleted");
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether a user can copy a given solver from the given space
	 * @param spaceId The space ID the user is being copied FROM
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param solverId The ID of the solver that would be copied
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	private static SecurityStatusCode canCopySolverFromSpace(int spaceId, int userIdDoingCopying, int solverId) {
		SecurityStatusCode status=canCopyPrimFromSpace(spaceId,userIdDoingCopying);
		if (!status.isSuccess()) {
			return status;
		}
		
		//the solver being copied should actually be in the space they are supposedly being copied from
		if (!Permissions.canUserSeeSolver(solverId, userIdDoingCopying)) {
			return new SecurityStatusCode(false, "The solver you are trying to move is not in the space you are copying from");
		}
		
		//we can't copy a solver if it has been deleted on disk
		if (Solvers.isSolverDeleted(solverId)) {
			return new SecurityStatusCode(false, "The solver you are trying to copy has already been deleted");
		}

		return new SecurityStatusCode(true);
	}
	/**
	 * Checks whether the user has enough disk quota to fit all of a list of solvers
	 * @param solverIds The solver IDs that would be added
	 * @param userId The ID of the user in question
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	//TODO: fail gracefully with bad solver ids
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
	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
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
 	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	
	public static SecurityStatusCode canCopySpace(int fromSpaceId, int toSpaceId, int userId, List<Integer> subspaceIds) {
		SecurityStatusCode status=null;
		for (Integer sid : subspaceIds) {
			status=SpaceSecurity.canCopySpaceFromSpace(fromSpaceId, userId, sid);
			if (!status.isSuccess()) {
				return status;
			}
		}
		status=canCopySpaceToSpace(toSpaceId,userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		// Make sure the user can see the subSpaces they're trying to copy
		for (int id : subspaceIds) {		
			// Make sure that the subspace has a unique name in the space.
			if(Spaces.notUniquePrimitiveName(Spaces.get(id).getName(), toSpaceId, 4)) {
				return new SecurityStatusCode(false, "The name of the space you are trying to copy is not unique in the destination space");
			}
		}
	
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user can link jobs from a space to another space
	 * @param fromSpaceId The ID of the spaces that jobs are being copied FROM
	 * @param toSpaceId The ID of the space that new jobs will be copied TO
	 * @param userId The ID of the user making the request
	 * @param jobIds The IDs of the jobs that would be copied
 	 * @return new SecurityStatusCode(true) if allowed, or a status code from SecurityStatusCodes if not
	 */
	
	public static SecurityStatusCode canLinkJobsBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> jobIdsBeingCopied) {
		
		SecurityStatusCode status=null;
		for(Integer jid : jobIdsBeingCopied) {
			status=	canCopyJobFromSpace(fromSpaceId,userId,jid);
			if (!status.isSuccess()) {
				return status;
			}
		}
		
		status=canCopyJobToSpace(toSpaceId,userId);
		if (!status.isSuccess()) {
			return status;
		}
		for (Integer jobId : jobIdsBeingCopied) { 
			if(Spaces.notUniquePrimitiveName(Jobs.get(jobId).getName(), toSpaceId, 3)) {
				return new SecurityStatusCode(false, "The name of the job you are trying to copy is not unique in the destination space");
			}
		}
			
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a list of benchmarks can be copied or linked from one space to another
	 * @param fromSpaceId The ID of the space the benchmarks are already in
	 * @param toSpaceId The ID of the space the benchmarks would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param benchmarkIdsBeingCopied The IDs of the benchmarks that would be copied or linked
	 * @param copy If true, the primitives are being copied. Otherwise, they are being linked
	 * @return new SecurityStatusCode(true) if the operation is allowed, and a status code from SecurityStatusCodes otherwise
	 */
	
	public static SecurityStatusCode canCopyOrLinkBenchmarksBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> benchmarkIdsBeingCopied,boolean copy) {
		//if we are copying, but not linking, disk quota must be checked
		if (copy) {
			if (!doesUserHaveDiskQuotaForBenchmarks(benchmarkIdsBeingCopied,userId)) {
				return new SecurityStatusCode(false, "You do not have enough disk quota space to copy the benchmark(s)");
			}
		}
		SecurityStatusCode status=null;
		for(Integer sid : benchmarkIdsBeingCopied) {
			//first make sure the user can copy each benchmark FROM the original space
			status=	canCopyBenchmarkFromSpace(fromSpaceId,userId,sid);
			if (!status.isSuccess()) {
				return status;
			}
		}
		//then, check to make sure they are allowed to copy TO the new space
		status=canCopyBenchmarkToSpace(toSpaceId,userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		//benchmark names must be unique in each space
		for (Integer benchId : benchmarkIdsBeingCopied) { 
			if(Spaces.notUniquePrimitiveName(Benchmarks.get(benchId).getName(), toSpaceId, 2)) {
				return new SecurityStatusCode(false, "The name of the benchmark you are trying to copy is not unique in the destination space");
			}
		}
			
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether a list of solvers can be copied or linked from one space to another
	 * @param fromSpaceId The ID of the space the solvers are already in
	 * @param toSpaceId The ID of the space the solvers would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param solverIdsBeingCopied The IDs of the solvers that would be copied or linked
	 * @param hierarchy If true, the copy will take place in the entire hierarchy rooted at the space with ID toSpaceId
	 * @param copy If true, the primitives are being copied. Otherwise, they are being linked
	 * @return new SecurityStatusCode(true) if the operation is allowed, and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canCopyOrLinkSolverBetweenSpaces(int fromSpaceId, int toSpaceId, int userId, List<Integer> solverIdsBeingCopied, boolean hierarchy,boolean copy) {
		//if we are copying, but not linking, make sure the user has enough disk space
		if (copy) {
			if (!doesUserHaveDiskQuotaForSolvers(solverIdsBeingCopied,userId)) {
				return new SecurityStatusCode(false, "You do not have enough disk quota space to copy the solver(s)");
			}
		}
		
		List<Integer> spaceIds=new ArrayList<Integer>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);
		
		//if we are checking the hierarchy, we must first get every space in it
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(toSpaceId, userId));
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}
		
		SecurityStatusCode status=null;
		for(Integer sid : solverIdsBeingCopied) {
			//make sure the user is allowed to copy solvers FROM the original space
			status=	canCopySolverFromSpace(fromSpaceId,userId,sid);
			if (!status.isSuccess()) {
				return status;
			}
		}
		
		//then, for every destination space, make sure they can copy TO that space
		for (Integer spaceId : spaceIds) {
			status=canCopySolverToSpace(spaceId,userId);
			if (!status.isSuccess()) {
				return status;
			}
			for (Integer solverId : solverIdsBeingCopied) { 
				if(Spaces.notUniquePrimitiveName(Solvers.get(solverId).getName(), spaceId, 1)) {
					return new SecurityStatusCode(false, "The name of the solver you are trying to copy is not unique in the destination space");
				}
			}
			
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether a list of users can be copied from one space to another
	 * @param fromSpaceId The ID of the space the users are already in
	 * @param toSpaceId The ID of the space the users would be placed in
	 * @param userIdDoingCopying The ID of the user making the request
	 * @param userIdsBeingCopied The IDs of the users that would be copied
	 * @param hierarchy If true, the copy will take place in the entire hierarchy rooted at the space with ID toSpaceId
	 * @return new SecurityStatusCode(true) if the operation is allowed, and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canCopyUserBetweenSpaces(int fromSpaceId, int toSpaceId, int userIdDoingCopying, List<Integer> userIdsBeingCopied, boolean hierarchy) {
		List<Integer> spaceIds=new ArrayList<Integer>(); //all the spaceIds of spaces being copied to
		spaceIds.add(toSpaceId);
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userIdDoingCopying, Spaces.getSubSpaceHierarchy(toSpaceId, userIdDoingCopying));
			for (Space s : subspaces) {
				spaceIds.add(s.getId());
			}
		}
		SecurityStatusCode status=null;
		for (Integer uid : userIdsBeingCopied) {
			status=canCopyUserFromSpace(fromSpaceId,userIdDoingCopying, uid);
			if (!status.isSuccess()) {
				return status;
			}
		}
		for (Integer sid : spaceIds) {
			status=canAddUserToSpace(sid,userIdDoingCopying);
			if (!status.isSuccess()) {
				return status;
			}
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can copy a user into another space
	 * @param spaceId The ID of the space where the new user would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canAddUserToSpace(int spaceId, int userIdMakingRequest) {
		// Check permissions, the user must have add user permissions in the destination space
		Permission perm = Permissions.get(userIdMakingRequest, spaceId);		
		if(perm == null || !perm.canAddUser()) {
			return new SecurityStatusCode(false, "You do not have permission to add a user to this space");
		}

		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can copy a solver into another space
	 * @param spaceId The ID of the space where the new solver would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static SecurityStatusCode canCopySolverToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddSolver()) {
			return new SecurityStatusCode(false, "You do not have permission to add a solver to this space");
		}	
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can copy a benchmark into another space
	 * @param spaceId The ID of the space where the new benchmark would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static SecurityStatusCode canCopyBenchmarkToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddBenchmark()) {
			return new SecurityStatusCode(false, "You do not have permission to add a benchmark to this space");
		}	
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can copy a job into another space
	 * @param spaceId The ID of the space where the new job would be PLACED.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static SecurityStatusCode canCopyJobToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddJob()) {
			return new SecurityStatusCode(false, "You do not have permission to add a job to this space");
		}	
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can copy a space into another space
	 * @param spaceId The ID of the space where the new space would be PLACED. This
	 * is NOT the ID of the space that would be moved.
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	private static SecurityStatusCode canCopySpaceToSpace(int spaceId, int userId) {
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = Permissions.get(userId, spaceId);		
		if(perm == null || !perm.canAddSpace()) {
			return new SecurityStatusCode(false, "You do not have permission to add a subspace to this space");
		}	
		
		return new SecurityStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether a list of users can all be removed from the given space
	 * @param userIdsBeingRemoved The IDs of the users that wouldbe removed
	 * @param userIdDoingRemoval The ID of the user making the request
	 * @param rootSpaceId The space ID to check
	 * @param hierarchy If true, checks to see whether the users can be removed from the entire hierarchy
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canRemoveUsersFromSpaces(List<Integer> userIdsBeingRemoved, int userIdDoingRemoval, int rootSpaceId, boolean hierarchy) {
		try {
			Permission perm = Permissions.get(userIdDoingRemoval, rootSpaceId);
			if(perm == null || !perm.canRemoveUser()) {
				return new SecurityStatusCode(false, "You do not have permission to remove a user from this space");
			}
			
			if (!Users.isAdmin(userIdDoingRemoval)) {
				// Validate the list of users to remove by:
				// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the list of users to remove
				// 2 - Ensuring other leaders of the space aren't in the list of users to remove
				for(int userId : userIdsBeingRemoved){
					if(userId == userIdDoingRemoval){
						return new SecurityStatusCode(false, "You cannot remove yourself from a space in this way");
					}
					perm = Permissions.get(userId, rootSpaceId);
					if(perm!=null && perm.isLeader()){
						return new SecurityStatusCode(false, "You do not have permission to remove a leader from this space");
					}
				}
			}
			if (hierarchy) {
				List<Space> subspaces = Spaces.trimSubSpaces(userIdDoingRemoval, Spaces.getSubSpaceHierarchy(rootSpaceId, userIdDoingRemoval));
				// Iterate once through all subspaces of the destination space to ensure the user has removeUser permissions in each
				for(Space subspace : subspaces) {
					
					SecurityStatusCode status=canRemoveUsersFromSpaces(userIdsBeingRemoved,userIdDoingRemoval,subspace.getId(),false);
					if (!status.isSuccess()) {
						return status;
					}	
				}
			}		
			return new SecurityStatusCode(true);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			
			
		}
		return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		
	}
	
	/**
	 * Checks whether the given user is allowed to see the current new community requests
	 * @param userId The ID of the user in question
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserViewCommunityRequests(int userId) {
		if (!Users.isAdmin(userId)){
			return new SecurityStatusCode(false, "You do not have permission to perform this operation");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks whether a user can update whether a space is public or private
	 * @param spaceId The ID of the space being checked
	 * @param userId The ID  of the user making the request 
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canSetSpacePublicOrPrivate(int spaceId,int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		//must be a leader to make a space public
		if (perm == null || !perm.isLeader()){
			return new SecurityStatusCode(false, "You do not have permission to affect whether this space is public or private");
		}
		
		return new SecurityStatusCode(true);
	}
    
    /**
     * Checks to see whether the permissions of one user in a particular space's hierarchy can be updated by another user
     * @param spaceId The ID of the space in question
     * @param userIdBeingUpdated The ID of the user who would have their permissions updated
     * @param requestUserId The Id of the user making the request
     * @return list of spaces where permissions can be changed
     */
    public static List<Integer> getUpdatePermissionSpaces(int spaceId, int userIdBeingUpdated, int requestUserId,boolean leaderStatusChange){
	//TODO :  make more efficient? (right now querying database for every space in hierarchy to check permissions)
	
	
	List<Integer> spaceIds=new ArrayList<Integer>(); //all the spaceIds of spaces being copied to
	spaceIds.add(spaceId);
	
	
	List<Space> subspaces = Spaces.trimSubSpaces(userIdBeingUpdated, Spaces.getSubSpaceHierarchy(spaceId, userIdBeingUpdated));
	
	for (Space s : subspaces) {
	    spaceIds.add(s.getId());

	}
		
	SecurityStatusCode status;
    
	List<Integer> permittedSpaceIds = new ArrayList<Integer>();
	for (Integer sid : spaceIds) {
	    status=canUpdatePermissions(sid,userIdBeingUpdated,requestUserId,leaderStatusChange);
	    if (status.isSuccess()) {
		permittedSpaceIds.add(sid);
	    }
	}

	return permittedSpaceIds;
    }
    /**
     * Checks to see whether the permissions of one user in a particular space can be updated by another user
     * @param spaceId The ID of the space in question
     * @param userIdBeingUpdated The ID of the user who would have their permissions updated
     * @param requestUserId The Id of the user making the request
     * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
     */
    public static SecurityStatusCode canUpdatePermissions(int spaceId, int userIdBeingUpdated, int requestUserId,boolean leaderStatusChange) {


	Permission perm = Permissions.get(requestUserId, spaceId);
	if(perm == null || !perm.isLeader()) {
		return new SecurityStatusCode(false, "You do not have permission to update permissions here");
	}

	
	// Ensure the user to edit the permissions of isn't themselves a leader
	perm = Permissions.get(userIdBeingUpdated, spaceId);

	if(perm.isLeader() && !Users.isAdmin(requestUserId) && !leaderStatusChange){
		return new SecurityStatusCode(false, "You do not have permission to update permissions for a leader here");
	}	
	
	
		
	return new SecurityStatusCode(true);
    }

    

}
