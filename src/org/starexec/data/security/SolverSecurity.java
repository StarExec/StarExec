package org.starexec.data.security;

import java.util.List;

import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;

public class SolverSecurity {
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
	
}
