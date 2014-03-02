package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;

public class SolverSecurity {
	/**
	 * Checks to see whether the given user can add a new website to the given solver
	 * @param solverId The ID of the solver being checked
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed or a status code from SecurityStatusCodes if not
	 */
	public static int canAssociateWebsite(int solverId, int userId) {
		if (!userOwnsSolverOrIsAdmin(Solvers.get(solverId),userId)) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}
		return 0;
	}
	
	public static int canUserUpdateSolver(int solverId, String name, String description, boolean isDownloadable, int userId) {
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
	
	public static int canUserUpdateConfiguration(int configId, int userId) {
		Solver solver = Solvers.getSolverByConfig(configId,false);
		if (solver==null) {
			return SecurityStatusCodes.ERROR_INVALID_PARAMS;
		} 
		if(!userOwnsSolverOrIsAdmin(solver,userId)){
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;
		}		
		return 0;
	}
	
	public static int canUserDeleteSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			int status=canUserDeleteSolver(sid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	public static int canUserRestoreSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			int status=canUserRestoreSolver(sid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	public static int canUserRecycleSolvers(List<Integer> solverIds, int userId) {
		for (Integer sid : solverIds) {
			int status=canUserRecycleSolver(sid,userId);
			if (status!=0) {
				return status;
			}
		}
		return 0;
	}
	
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
	

	
	public static boolean userOwnsSolverOrIsAdmin(Solver solver,int userId) {
		return (solver.getUserId()==userId || Users.isAdmin(userId));
	}
	
	public static int canUserRemoveSolver(int spaceId, int userId) {
		Permission perm=Permissions.get(userId, spaceId);
		if(perm == null || !perm.canRemoveSolver()) {
			return SecurityStatusCodes.ERROR_INVALID_PERMISSIONS;	
		}
		return 0;
	}
	
	
	
	public static int canUserRemoveSolverFromHierarchy(int rootSpaceId, int userId) {
		List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaces(rootSpaceId, userId, true));
		for(Space s : subspaces) {
			int status=canUserRemoveSolver(s.getId(),userId);
			if (status<0) {
				return 0;
			}
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
