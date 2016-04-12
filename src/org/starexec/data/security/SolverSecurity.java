package org.starexec.data.security;

import java.io.File;
import java.util.List;

import org.starexec.util.Util;
import org.starexec.util.Validator;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Website;
import org.starexec.data.to.Website.WebsiteType;

public class SolverSecurity {

	
	/**
	 * Checks whether the user can see the build log for the given solver. They must
	 * own the solver or be an admin
	 * @param solverId
	 * @param userId
	 * @return A validatorStatusCode object
	 */
	public static ValidatorStatusCode canUserSeeBuildLog(int solverId,int userId) {
		Solver s=Solvers.get(solverId);
		if (s==null || userId!=s.getUserId() &&!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the build log for this solver");
		}
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can upload a solver with a starexec_build script in the given space.
	 * Only leaders of communities may do this, and they can only do it in their own community
	 * @param userId
	 * @param spaceId
	 * @return
	 */
	
	public static ValidatorStatusCode canUserRunStarexecBuild(int userId, int spaceId) {
		Permission p=Permissions.get(userId,Spaces.getCommunityOfSpace(spaceId));
		if (p==null || !p.isLeader()) {
			return new ValidatorStatusCode(false, "You do not have permission to use a starexec_build script. Only community leaders may use build scripts.");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether a user can add a new configuration to the given solver
	 * @param solverId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canUserAddConfiguration(int solverId, int userId) {
		Solver s=Solvers.get(solverId);
		if (s==null) {
			return new ValidatorStatusCode(false, "The solver could not be found");
		}
		if (!GeneralSecurity.hasAdminWritePrivileges(userId) && !(s.getUserId()==userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to add a configuration to this solver");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks if a user can get an anonymou link for a solver.
	 * @param solverId The id of the solver that we're getting a link for
	 * @param userId The id of the user trying to get the anonymous link.
	 * @author Albert Giegeich
	 */
	public static ValidatorStatusCode canUserGetAnonymousLink( int solverId, int userId ) {
		if ( userOwnsSolverOrIsAdmin( Solvers.get( solverId ), userId )) {
			return new ValidatorStatusCode( true );
		} else {
			return new ValidatorStatusCode( false, "You do not have permission to get an anonymous link for this solver." );
		}
	}
	
	/**
	 * Checks to see whether the given user can view the given solver
	 * @param solverId The ID of the solver being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed or a status code from ValidatorStatusCodes if not
	 */
	public static ValidatorStatusCode canUserSeeSolver(int solverId, int userId) {
		
		if(!Permissions.canUserSeeSolver(solverId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see this solver");
		}
		
		return new ValidatorStatusCode(true);
	}
	

	
	/**
	 * Checks to see whether the user can update a solver with the given attributes
	 * @param solverId The ID of the solver that would be updated
	 * @param name The new name that would be given the solver
	 * @param description The description that would be given the solver
	 * @param isDownloadable The boolean value for "downloadable" that would be given the solver
	 * @param userId The ID of the solver making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	
	public static ValidatorStatusCode canUserUpdateSolver(int solverId, String name, String description, boolean isDownloadable, int userId) {
		// Ensure the parameters are valid
		if(!Validator.isValidSolverName(name)){
			return new ValidatorStatusCode(false, "The new name is not in the correct format. Please see the help pages to see the correct format");
		}
		
		if(!Validator.isValidPrimDescription(description)){
			return new ValidatorStatusCode(false, "The new description is not in the correct format. Please see the help pages to see the correct format");
		}
		
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return new ValidatorStatusCode(false, "The solver could not be found");
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to update this solver");
		}
		// Extract new solver details from request
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can update the given configuration.
	 * @param configId The ID of the config that would be updated
	 * @param userId The ID of the user making the request
	 * @param name The new name that will be given to the configuration upon editing
	 * @param description The new description that will be given to the configuration upon editing
	 * @return  0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserUpdateConfiguration(int configId, int userId, String name, String description) {
		// Ensure the parameters are valid
		if(!Validator.isValidConfigurationName(name)){
			return new ValidatorStatusCode(false, "The new name is not in the correct format. Please see the help pages to see the correct format");
		}
		
		Configuration c= Solvers.getConfiguration(configId);
		Solver solver = Solvers.getSolverByConfig(configId,false);

		// If the old config and new config names are NOT the same, ensure the file pointed to by
		// the new config does not already exist on disk
		if(!c.getName().equals(name)){
			File newConfig = new File(Util.getSolverConfigPath(solver.getPath(), name));

			if(newConfig.exists()){
				return new ValidatorStatusCode(false, "The solver already has a configuration with the given name");
			}
		}
		
		if(!Validator.isValidPrimDescription(description)){
			return new ValidatorStatusCode(false, "The new description is not in the correct format. Please see the help pages to see the correct format");
		}
		
		if (solver==null) {
			return new ValidatorStatusCode(false, "The solver owning this configuration could not be found");
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to update any configurations associated with this solver");
		}		
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can delete all of the given solvers
	 * @param solverIds The IDs of all the solvers to check
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserDeleteSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			ValidatorStatusCode status=canUserDeleteSolver(sid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can restore all of the given solvers
	 * @param solverIds The IDs of all the solvers to check
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRestoreSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			ValidatorStatusCode status=canUserRestoreSolver(sid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether a user can recycle all the orphaned solvers owned by another user.
	 * @param userIdToDelete The user who owns the orphaned solvers that will be recycled
	 * @param userIdMakingRequest The user who is trying to do the recycling
	 * @return A ValidatorStatusCode
	 */
	public static ValidatorStatusCode canUserRecycleOrphanedSolvers(int userIdToDelete, int userIdMakingRequest) {
		if (Users.get(userIdToDelete)==null) {
			return new ValidatorStatusCode(false, "The given user does not exist");
		}
		if (userIdToDelete!=userIdMakingRequest && !GeneralSecurity.hasAdminWritePrivileges(userIdMakingRequest)) {
			return new ValidatorStatusCode(false, "You do not have permission to recycle solvers belonging to another user");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can recycle all of the given solvers
	 * @param solverIds The IDs of all the solvers to check
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRecycleSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			ValidatorStatusCode status=canUserRecycleSolver(sid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can delete the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserDeleteSolver(int solverId, int userId) {
		Solver solver = Solvers.getIncludeDeleted(solverId);
		
		if (solver==null) {
			return new ValidatorStatusCode(false, "The given solver could not be found");
		}
		if (solver.isDeleted()) {
			return new ValidatorStatusCode(false, "The given solver has already been deleted");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to delete this solver");
		}
		return new ValidatorStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can recycle the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRecycleSolver(int solverId, int userId) {
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return new ValidatorStatusCode(false, "The given solver could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to recycle this solver");
		}
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can restore the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRestoreSolver(int solverId, int userId) {
		
		Solver solver = Solvers.getIncludeDeleted(solverId);
		if (solver==null) {
			return new ValidatorStatusCode(false, "The given solver could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to restore this solver");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see if the given user either owns the given solver or is an admin
	 * @param solverId
	 * @param userId
	 * @return True if the user owns the solver OR is an admin, and false otherwise
	 */
	public static boolean userOwnsSolverOrIsAdmin(int solverId,int userId) {
		return userOwnsSolverOrIsAdmin(Solvers.get(solverId), userId);
	}
	

	/**
	 * Checks to see if the given user either owns the given solver or is an admin
	 * @param solver
	 * @param userId
	 * @return True if the user owns the solver OR is an admin, and false otherwise
	 */
	public static boolean userOwnsSolverOrIsAdmin(Solver solver,int userId) {
		return (solver!=null && (solver.getUserId()==userId || GeneralSecurity.hasAdminWritePrivileges(userId)));
	}
	/**
	 * Checks whether a user can remove a solver from a space
	 * @param spaceId The ID of the space in question
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRemoveSolver(int spaceId, int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		if(perm == null || !perm.canRemoveSolver()) {
			return new ValidatorStatusCode(false, "You do not have permission to remove solvers from the given space");
		}
		return new ValidatorStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether a user is allowed to remove a solver from a space hierarchy. They
	 * are allowed if they can remvoe the solver from the root plus ANY SUBSET of subspaces, including
	 * the empty subset. Other validation needs to be done to make sure solvers are remove from only the
	 * correct subspaces
	 * @param rootSpaceId The ID of the space at the root of the hierarchy in question
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	public static ValidatorStatusCode canUserRemoveSolverFromHierarchy(int rootSpaceId, int userId) {
		ValidatorStatusCode status=canUserRemoveSolver(rootSpaceId,userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can delete all of the given configuraitons
	 * @param configIds The IDs of the configurations being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed or a status code from ValidatorStatusCodes if not
	 */
	public static ValidatorStatusCode canUserDeleteConfigurations(List<Integer> configIds, int userId) {
		for (Integer cid : configIds) {
			ValidatorStatusCode status=canUserDeleteConfiguration(cid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can delete the given configuraiton
	 * @param configId The ID of the configuration being checked
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed or a status code from ValidatorStatusCodes if not
	 */

	public static ValidatorStatusCode canUserDeleteConfiguration(int configId, int userId) {
		Configuration config=Solvers.getConfiguration(configId);
		if (config==null) {
			return new ValidatorStatusCode(false, "The given configuration could not be found");
		}
		Solver solver = Solvers.get(config.getSolverId());
		if (solver==null) {
			return new ValidatorStatusCode(false, "The solver associated with the given configuration could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new ValidatorStatusCode(false, "You do not have permission to delete configurations attached to this solver");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user has permission to download the given solver
	 * @param solverId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canUserDownloadSolver(int solverId, int userId) {
		Solver s=Solvers.get(solverId);
		boolean userHasAdminReadPrivileges = GeneralSecurity.hasAdminReadPrivileges(userId);
		if (!Permissions.canUserSeeSolver(s.getId(), userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see this solver");
		
		}
		if (!(s.isDownloadable() || s.getUserId()==userId || userHasAdminReadPrivileges)) {
			return new ValidatorStatusCode(false, "This solver is not available for download");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the user is allowed to download the Json object representing the solver
	 * @param solverId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canGetJsonSolver(int solverId, int userId) {
		if (!Permissions.canUserSeeSolver(solverId, userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the specified solver");
		}
		Solver s=Solvers.getIncludeDeleted(solverId);
		if (s==null) {
			return new ValidatorStatusCode(false, "The given solver could not be found");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the user is allowed to download the Json object representing the solver
	 * @param configId
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canGetJsonConfiguration(int configId, int userId) {
		Solver s=Solvers.getSolverByConfig(configId, true);
		if (!Permissions.canUserSeeSolver(s.getId(), userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to see the specified solver");
		}
		Configuration c=Solvers.getConfiguration(configId);
		if (c==null) {
			return new ValidatorStatusCode(false, "The given configuration could not be found");
		}
		return new ValidatorStatusCode(true);
	}
	
}
