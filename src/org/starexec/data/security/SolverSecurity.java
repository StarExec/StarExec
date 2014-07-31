package org.starexec.data.security;

import java.util.List;

import org.starexec.util.Validator;
import org.starexec.command.Status;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.database.Websites.WebsiteType;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Website;

public class SolverSecurity {
	/**
	 * Checks to see whether the given user can add a new website to the given solver
	 * @param solverId The ID of the solver being checked
	 * @param userId The ID of the user making the request
	 * @param name The name to be given the new website
	 * @param URL The URL of the new website
	 * @return new SecurityStatusCode(true) if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static SecurityStatusCode canAssociateWebsite(int solverId, int userId,String name, String URL) {
		if (!userOwnsSolverOrIsAdmin(Solvers.get(solverId),userId)) {
			return new SecurityStatusCode(false, "You do not have permission to associate a website with this solver");
		}
		
		if (!Validator.isValidPrimName(name) ) {
			return new SecurityStatusCode(false, "The website name is not formatted correctly. Please refer to the help pages to see the correct format");
		}
		
		if (!Validator.isValidWebsite(URL)) {
			return new SecurityStatusCode(false, "The website url is not formatted correctly. Please refer to the help pages to see the correct format");
		}
		
		
		return new SecurityStatusCode(true);
	}
	
	public static SecurityStatusCode canUserSeeBuildLog(int solverId,int userId) {
		Solver s=Solvers.get(solverId);
		if (userId!=s.getUserId()) {
			return new SecurityStatusCode(false, "You do not have permission to see the build log for this solver");
		}
		return new SecurityStatusCode(true);
	}
	
	public static SecurityStatusCode canUserRunStarexecBuild(int userId, int spaceId) {
		if (!Permissions.get(userId, Spaces.GetCommunityOfSpace(spaceId)).isLeader()) {
			return new SecurityStatusCode(false, "You do not have permission to use a starexec_build script. Only community leaders may use build scripts.");
		}
		return new SecurityStatusCode(true);
	}
	
	public static SecurityStatusCode canUserAddConfiguration(int solverId, int userId) {
		Solver s=Solvers.get(solverId);
		if (s==null) {
			return new SecurityStatusCode(false, "The solver could not be found");
		}
		if (!Users.isAdmin(userId) && !(s.getUserId()==userId)) {
			return new SecurityStatusCode(false, "You do not have permission to add a configuration to this solver");
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can add a new website to the given solver
	 * @param solverId The ID of the solver being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static SecurityStatusCode canViewWebsites(int solverId, int userId) {
		
		if(!Permissions.canUserSeeSolver(solverId, userId)) {
			return new SecurityStatusCode(false, "You do not have permission to see this solver");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a solver
	 * @param spaceId The ID of the solver that contains the site to be deleted
	 * @param websiteId The ID of the website to be deleted
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canDeleteWebsite(int solverId,int websiteId,int userId){
		if (!userOwnsSolverOrIsAdmin(Solvers.get(solverId),userId)) {
			return new SecurityStatusCode(false, "You do not have permission to delete websites from this solver");
		}
		List<Website> websites=Websites.getAllForJavascript(solverId,WebsiteType.SOLVER);
		boolean websiteInSolver=false;
		for (Website w : websites) {
			if (w.getId()==websiteId) {
				websiteInSolver=true;
				break;
			}
		}
		if (!websiteInSolver) {
			return new SecurityStatusCode(false, "The given website is not associated with the given solver");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the user can update a solver with the given attributes
	 * @param solverId The ID of the solver that would be updated
	 * @param name The new name that would be given the solver
	 * @param description The description that would be given the solver
	 * @param isDownloadable The boolean value for "downloadable" that would be given the solver
	 * @param userId The ID of the solver making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static SecurityStatusCode canUserUpdateSolver(int solverId, String name, String description, boolean isDownloadable, int userId) {
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(name)){
			return new SecurityStatusCode(false, "The new name is not in the correct format. Please see the help pages to see the correct format");
		}
		
		if(!Validator.isValidPrimDescription(description)){
			return new SecurityStatusCode(false, "The new description is not in the correct format. Please see the help pages to see the correct format");
		}
		
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return new SecurityStatusCode(false, "The solver could not be found");
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new SecurityStatusCode(false, "You do not have permission to update this solver");
		}
		// Extract new solver details from request
		
		//if the name is actually being changed
		if (!solver.getName().equals(name)) {
			int editable=Solvers.isNameEditable(solverId);
			if (editable<0) {
				return new SecurityStatusCode(false, "This solver is in more than one space, so its name cannot be changed");
			}
			//if editable is positive, that means it is the ID of the one space the solver is in
			if (editable>0 && Spaces.notUniquePrimitiveName(name,editable, 1)) {
				return new SecurityStatusCode(false, "The name of the solver must be unique in the space");
			}
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can update the given configuration.
	 * @param configId The ID of the config that would be updated
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserUpdateConfiguration(int configId, int userId, String name, String description) {
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(name)){
			return new SecurityStatusCode(false, "The new name is not in the correct format. Please see the help pages to see the correct format");
		}
		
		if(!Validator.isValidPrimDescription(description)){
			return new SecurityStatusCode(false, "The new description is not in the correct format. Please see the help pages to see the correct format");
		}
		
		Solver solver = Solvers.getSolverByConfig(configId,false);
		if (solver==null) {
			return new SecurityStatusCode(false, "The solver owning this configuratino could not be found");
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new SecurityStatusCode(false, "You do not have permission to update any configurations associated with this solver");
		}		
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can delete all of the given solvers
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserDeleteSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			SecurityStatusCode status=canUserDeleteSolver(sid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can restore all of the given solvers
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRestoreSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			SecurityStatusCode status=canUserRestoreSolver(sid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether a user can recycle all the orphaned solvers owned by another user.
	 * @param userIdToDelete The user who owns the orphaned solvers that will be recycled
	 * @param userIdMakingRequest The user who is trying to do the recycling
	 * @return A SecurityStatusCode
	 */
	public static SecurityStatusCode canUserRecycleOrphanedSolvers(int userIdToDelete, int userIdMakingRequest) {
		if (userIdToDelete!=userIdMakingRequest && !Users.isAdmin(userIdMakingRequest)) {
			return new SecurityStatusCode(false, "You do not have permission to recycle solvers belonging to another user");
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can recycle all of the given solvers
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRecycleSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			SecurityStatusCode status=canUserRecycleSolver(sid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can delete the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserDeleteSolver(int solverId, int userId) {
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return new SecurityStatusCode(false, "The given solver could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new SecurityStatusCode(false, "You do not have permission to delete this solver");
		}
		return new SecurityStatusCode(true);
	}

	/**
	 * Checks to see whether the given user can recycle the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRecycleSolver(int solverId, int userId) {
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return new SecurityStatusCode(false, "The given solver could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new SecurityStatusCode(false, "You do not have permission to recycle this solver");
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can restore the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRestoreSolver(int solverId, int userId) {
		
		Solver solver = Solvers.getIncludeDeleted(solverId);
		if (solver==null) {
			return new SecurityStatusCode(false, "The given solver could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new SecurityStatusCode(false, "You do not have permission to restore this solver");
		}
		return new SecurityStatusCode(true);
	}
	

	/**
	 * Checks to see if the given user either owns the given solver or is an admin
	 * @param solver
	 * @param userId
	 * @return True if the user owns the solver OR is an admin, and false otherwise
	 */
	public static boolean userOwnsSolverOrIsAdmin(Solver solver,int userId) {
		return (solver.getUserId()==userId || Users.isAdmin(userId));
	}
	/**
	 * Checks whether a user can remove a solver from a space
	 * @param spaceId The ID of the space in question
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveSolver(int spaceId, int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		if(perm == null || !perm.canRemoveSolver()) {
			return new SecurityStatusCode(false, "You do not have permission to remove solvers from the given space");
		}
		return new SecurityStatusCode(true);
	}
	
	
	/**
	 * Checks to see whether a user is allowed to remove a solver from a space hierarchy. They
	 * are allowed if they can remvoe the solver from the root plus ANY SUBSET of subspaces, including
	 * the empty subset. Other validation needs to be done to make sure solvers are remove from only the
	 * correct subspaces
	 * @param rootSpaceId The ID of the space at the root of the hierarchy in question
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static SecurityStatusCode canUserRemoveSolverFromHierarchy(int rootSpaceId, int userId) {
		SecurityStatusCode status=canUserRemoveSolver(rootSpaceId,userId);
		if (!status.isSuccess()) {
			return status;
		}
		
		return new SecurityStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user can delete all of the given configuraitons
	 * @param configIds The IDs of the configurations being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static SecurityStatusCode canUserDeleteConfigurations(List<Integer> configIds, int userId) {
		for (Integer cid : configIds) {
			SecurityStatusCode status=canUserDeleteConfiguration(cid,userId);
			if (!status.isSuccess()) {
				return status;
			}
		}
		return new SecurityStatusCode(true);
	}
	/**
	 * Checks to see whether the given user can delete the given configuraiton
	 * @param configId The ID of the configuration being checked
	 * @param userId The ID of the user making the request
	 * @return new SecurityStatusCode(true) if the operation is allowed or a status code from SecurityStatusCodes if not
	 */

	public static SecurityStatusCode canUserDeleteConfiguration(int configId, int userId) {
		Configuration config=Solvers.getConfiguration(configId);
		
		Solver solver = Solvers.get(config.getSolverId());
		if (solver==null) {
			return new SecurityStatusCode(false, "The solver associated with the given configuration could not be found");
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return new SecurityStatusCode(false, "You do not have permission to delete configurations attached to this solver");
		}
		return new SecurityStatusCode(true);
	}
	
}
