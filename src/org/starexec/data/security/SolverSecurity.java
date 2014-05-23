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
	 * @return 0 if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static int canAssociateWebsite(int solverId, int userId,String name, String URL) {
		if (!userOwnsSolverOrIsAdmin(Solvers.get(solverId),userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		if (!Validator.isValidPrimName(name) || !Validator.isValidWebsite(URL)) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		
		return 0;
	}
	
	public static int canUserAddConfiguration(int solverId, int userId) {
		Solver s=Solvers.get(solverId);
		if (s==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if (!Users.isAdmin(userId) && !(s.getUserId()==userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can add a new website to the given solver
	 * @param solverId The ID of the solver being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static int canViewWebsites(int solverId, int userId) {
		
		if(!Permissions.canUserSeeSolver(solverId, userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a solver
	 * @param spaceId The ID of the solver that contains the site to be deleted
	 * @param websiteId The ID of the website to be deleted
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canDeleteWebsite(int solverId,int websiteId,int userId){
		if (!userOwnsSolverOrIsAdmin(Solvers.get(solverId),userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
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
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether the user can update a solver with the given attributes
	 * @param solverId The ID of the solver that would be updated
	 * @param name The new name that would be given the solver
	 * @param description The description that would be given the solver
	 * @param isDownloadable The boolean value for "downloadable" that would be given the solver
	 * @param userId The ID of the solver making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	
	public static int canUserUpdateSolver(int solverId, String name, String description, boolean isDownloadable, int userId) {
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(name)
				|| !Validator.isValidPrimDescription(description)){
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		// Extract new solver details from request
		
		//if the name is actually being changed
		if (!solver.getName().equals(name)) {
			int editable=Solvers.isNameEditable(solverId);
			if (editable<0) {
				return SecurityStatusCodes.ERROR_NAME_NOT_EDITABLE;
			}
			//if editable is positive, that means it is the ID of the one space the solver is in
			if (editable>0 && Spaces.notUniquePrimitiveName(name,editable, 1)) {
				return SecurityStatusCodes.ERROR_NOT_UNIQUE_NAME;
			}
		}
		return 0;
	}
	/**
	 * Checks to see whether the given user can update the given configuration.
	 * @param configId The ID of the config that would be updated
	 * @param userId The ID of the user making the request
	 * @return  0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserUpdateConfiguration(int configId, int userId, String name, String description) {
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(name)
				|| !Validator.isValidPrimDescription(description)){
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		
		Solver solver = Solvers.getSolverByConfig(configId,false);
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}		
		return 0;
	}
	/**
	 * Checks to see whether the given user can delete all of the given solvers
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserDeleteSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			int status=canUserDeleteSolver(sid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can restore all of the given solvers
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRestoreSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			int status=canUserRestoreSolver(sid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	/**
	 * Checks to see whether the given user can recycle all of the given solvers
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRecycleSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			int status=canUserRecycleSolver(sid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can delete the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserDeleteSolver(int solverId, int userId) {
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}

	/**
	 * Checks to see whether the given user can recycle the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRecycleSolver(int solverId, int userId) {
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	/**
	 * Checks to see whether the given user can restore the given solver
	 * @param solverId The ID of the solver to check
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRestoreSolver(int solverId, int userId) {
		Solver solver = Solvers.get(solverId);
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
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
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveSolver(int spaceId, int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		if(perm == null || !perm.canRemoveSolver()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
	}
	
	
	/**
	 * Checks to see whether a user is allowed to remove a solver from a space hierarchy. They
	 * are allowed if they can remvoe the solver from the root plus ANY SUBSET of subspaces, including
	 * the empty subset. Other validation needs to be done to make sure solvers are remove from only the
	 * correct subspaces
	 * @param rootSpaceId The ID of the space at the root of the hierarchy in question
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from SecurityStatusCodes otherwise
	 */
	public static int canUserRemoveSolverFromHierarchy(int rootSpaceId, int userId) {
		int status=canUserRemoveSolver(rootSpaceId,userId);
		if (status<0) {
			return Status.ERROR_PERMISSION_DENIED;
		}
		
		return 0;
	}
	
	/**
	 * Checks to see whether the given user can delete all of the given configuraitons
	 * @param configIds The IDs of the configurations being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static int canUserDeleteConfigurations(List<Integer> configIds, int userId) {
		for (Integer cid : configIds) {
			int status=canUserDeleteConfiguration(cid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	/**
	 * Checks to see whether the given user can delete the given configuraiton
	 * @param configId The ID of the configuration being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed or a status code from SecurityStatusCodes if not
	 */

	public static int canUserDeleteConfiguration(int configId, int userId) {
		Configuration config=Solvers.getConfiguration(configId);
		
		Solver solver = Solvers.get(config.getSolverId());
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		}
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
}
