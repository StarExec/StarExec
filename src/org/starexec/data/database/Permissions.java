package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Permission;

/**
 * Handles all database interaction for permissions
 */
public class Permissions {
	private static final Logger log = Logger.getLogger(Permissions.class);
	
	/**
	 * Adds a new permission record to the database. This is an internal helper method.
	 * @param p The permission to add
	 * @param con The connection to add the permission with
	 * @return The ID of the inserted record
	 * @author Tyler Jensen
	 */
	protected static long add(Permission p, Connection con) throws Exception {
		CallableStatement procDefaultPerm = con.prepareCall("{CALL AddPermissions(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
		procDefaultPerm.setBoolean(1, p.canAddSolver());
		procDefaultPerm.setBoolean(2, p.canAddBenchmark());
		procDefaultPerm.setBoolean(3, p.canAddUser());
		procDefaultPerm.setBoolean(4, p.canAddSpace());
		procDefaultPerm.setBoolean(5, p.canAddJob());
		procDefaultPerm.setBoolean(6, p.canRemoveSolver());
		procDefaultPerm.setBoolean(7, p.canRemoveBench());
		procDefaultPerm.setBoolean(8, p.canRemoveSpace());
		procDefaultPerm.setBoolean(9, p.canRemoveUser());
		procDefaultPerm.setBoolean(10, p.canRemoveJob());
		procDefaultPerm.setBoolean(11, p.isLeader());
		procDefaultPerm.registerOutParameter(12, java.sql.Types.BIGINT);			
				
		procDefaultPerm.execute();
		return procDefaultPerm.getLong(12);
	}	
	
	/**
	 * Checks to see if the user has access to the given solvers in some way. More specifically,
	 * this checks if the user belongs to all the spaces the solvers belong to.
	 * @param solverIds The solvers to check if the user can see
	 * @param userId The user that is requesting to view the given solvers	
	 * @return True if the user can somehow see the solvers, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSolvers(List<Long> solverIds, long userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewSolver(?, ?)}");
			
			for(long id : solverIds) {				
				procedure.setLong(1, id);					
				procedure.setLong(2, userId);
				ResultSet results = procedure.executeQuery();
			
				if(results.first()) {
					if(false == results.getBoolean(1)) {
						return false;
					}
				}
			}
			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Checks to see if the user has access to the solver in some way. More specifically,
	 * this checks if the user belongs to any space the solver belongs to.
	 * @param solverId The solver to check if the user can see
	 * @param userId The user that is requesting to view the given solver	
	 * @return True if the user can somehow see the solver, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSolver(long solverId, long userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewSolver(?, ?)}");
			procedure.setLong(1, solverId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Checks to see if the user has access to the benchmark in some way. More specifically,
	 * this checks if the user belongs to any space the benchmark belongs to.
	 * @param benchId The benchmark to check if the user can see
	 * @param userId The user that is requesting to view the given benchmark	 * 
	 * @return True if the user can somehow see the benchmark, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeBench(long benchId, long userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewBenchmark(?, ?)}");
			procedure.setLong(1, benchId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;				
	}
	
	/**
	 * Checks to see if the user has access to the benchmarks in some way. More specifically,
	 * this checks if the user belongs to all spaces the benchmarks belong to.
	 * @param benchIds The benchmarks to check if the user can see
	 * @param userId The user that is requesting to view the given benchmarks 
	 * @return True if the user can somehow see all benchmarks, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeBenchs(List<Long> benchIds, long userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewBenchmark(?, ?)}");
			
			for(long id : benchIds) {
				procedure.setLong(1, id);					
				procedure.setLong(2, userId);
				ResultSet results = procedure.executeQuery();
				
				if(results.first()) {
					if(false == results.getBoolean(1)) {
						return false;
					}
				}
			}			
				
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;				
	}
	
	/**
	 * Checks to see if the user has access to the job in some way. More specifically,
	 * this checks if the user belongs to any space the job belongs to.
	 * @param jobId The job to check if the user can see
	 * @param userId The user that is requesting to view the given job 
	 * @return True if the user can somehow see the job, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeJob(long jobId, long userId){		
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewJob(?, ?)}");
			procedure.setLong(1, jobId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Checks to see if the user belongs to the given space.
	 * @param spaceId The space to check if the user can see
	 * @param userId The user that is requesting to view the given space
	 * @return True if the user belongs to the space, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSpace(long spaceId, long userId) {		
		if(spaceId <= 1) {
			// Can always see root space
			return true;
		}
		
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewSpace(?, ?)}");
			procedure.setLong(1, spaceId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Retrieves the user's maximum set of permissions in a space.
	 * @param userId The user to get permissions for	
	 * @param spaceId The id of the space to get the user's permissions on
	 * @return A permission object containing the user's permission on the space. Null if the user is not apart of the space.
	 * @author Tyler Jensen
	 */
	public static Permission get(long userId, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetUserPermissions(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setLong(2, spaceId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {				
				Permission p = new Permission();
				p.setAddBenchmark(results.getBoolean("add_bench"));
				p.setAddSolver(results.getBoolean("add_solver"));
				p.setAddSpace(results.getBoolean("add_space"));
				p.setAddUser(results.getBoolean("add_user"));
				p.setAddJob(results.getBoolean("add_job"));
				p.setRemoveBench(results.getBoolean("remove_bench"));
				p.setRemoveSolver(results.getBoolean("remove_solver"));
				p.setRemoveSpace(results.getBoolean("remove_space"));
				p.setRemoveUser(results.getBoolean("remove_user"));
				p.setRemoveJob(results.getBoolean("remove_job"));
				p.setLeader(results.getBoolean("is_leader"));
				p.setId(userId);
				
				if(results.wasNull()) {
					/* If the permission doesn't exist we always get a result
					but all of it's values are null, so here we check for a 
					null result and return null */
					return null;
				}
				
				return p;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;		
	}
	
	/**
	 * Retrieves the default permissions applied to a user when they are added to a space
	 * @param spaceId The id of the space to get the default user's permission
	 * @return A permission object containing the space's default user permissionsuser's permission on the space.
	 * @author Tyler Jensen
	 */
	public static Permission getSpaceDefault(long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpacePermissions(?)}");			
			procedure.setLong(1, spaceId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {				
				Permission p = new Permission();
				p.setId(results.getLong("id"));
				p.setAddBenchmark(results.getBoolean("add_bench"));
				p.setAddSolver(results.getBoolean("add_solver"));
				p.setAddSpace(results.getBoolean("add_space"));
				p.setAddUser(results.getBoolean("add_user"));
				p.setAddJob(results.getBoolean("add_job"));
				p.setRemoveBench(results.getBoolean("remove_bench"));
				p.setRemoveSolver(results.getBoolean("remove_solver"));
				p.setRemoveSpace(results.getBoolean("remove_space"));
				p.setRemoveUser(results.getBoolean("remove_user"));
				p.setRemoveJob(results.getBoolean("remove_job"));
				p.setLeader(results.getBoolean("is_leader"));
												
				return p;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;		
	}
	
	
	/**
	 * Sets the permissions of a given user in a given space
	 * 
	 * @param userId the id of the user to set the permissions of
	 * @param spaceId the id of the space where the permissions will effect
	 * @param newPerm the new set of permissions to set
	 * @return true iff the permissions were successfully set, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean set(long userId, long spaceId, Permission newPerm) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL SetUserPermissions(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setLong(2, spaceId);
			procedure.setBoolean(3, newPerm.canAddSolver());
			procedure.setBoolean(4, newPerm.canAddBenchmark());
			procedure.setBoolean(5, newPerm.canAddUser());
			procedure.setBoolean(6, newPerm.canAddSpace());
			procedure.setBoolean(7, newPerm.canAddJob());
			procedure.setBoolean(8, newPerm.canRemoveSolver());
			procedure.setBoolean(9, newPerm.canRemoveBench());
			procedure.setBoolean(10, newPerm.canRemoveSpace());
			procedure.setBoolean(11, newPerm.canRemoveUser());
			procedure.setBoolean(12, newPerm.canRemoveJob());
			procedure.setBoolean(13, newPerm.isLeader());
			
			procedure.executeUpdate();
			log.debug(String.format("Permissions successfully changed for user [%d] in space [%d]", userId, spaceId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
			log.error(String.format("Changing permissions failed for user [%d] in space [%d]", userId, spaceId));
		return false;
	}
}
