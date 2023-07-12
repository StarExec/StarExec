package org.starexec.data.database;

import org.starexec.constants.R;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.logger.StarLogger;
import org.starexec.util.Util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Handles all database interaction for permissions
 */
public class Permissions {
	private static final StarLogger log = StarLogger.getLogger(Permissions.class);

	/**
	 * Adds a new permission record to the database. This is an internal helper method.
	 *
	 * @param p The permission to add
	 * @param con The connection to add the permission with
	 * @return The ID of the inserted record
	 * @throws Exception
	 * @author Tyler Jensen
	 */
	protected static int add(Permission p, Connection con) {
		CallableStatement procDefaultPerm = null;
		try {
			procDefaultPerm = con.prepareCall("{CALL AddPermissions(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
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
			procDefaultPerm.registerOutParameter(12, java.sql.Types.INTEGER);

			procDefaultPerm.execute();
			return procDefaultPerm.getInt(12);
		} catch (Exception e) {
			log.error("add", e);
		} finally {
			Common.safeClose(procDefaultPerm);
		}
		return -1;
	}

	/**
	 * Checks to see if the user has access to the benchmark in some way. More specifically, this checks if the user
	 * belongs to any space the benchmark belongs to.
	 *
	 * @param benchId The benchmark to check if the user can see
	 * @param userId The user that is requesting to view the given benchmark
	 * @param con The open connection to make the query on
	 * @return True if the user can somehow see the benchmark, false otherwise
	 * @author Tyler Jensen
	 */

	public static boolean canUserSeeBench(Connection con, int benchId, int userId) {
		Benchmark b = Benchmarks.getIncludeDeletedAndRecycled(con, benchId, false);
		if (b == null) {
			return false;
		}
		if (Benchmarks.isPublic(con, benchId)) {
			return true;
		}

		User u = Users.get(con, userId);
		if (u != null && (u.getRole().equals(R.ADMIN_ROLE_NAME) || u.getRole().equals(R.DEVELOPER_ROLE_NAME))) {
			return true;
		}
		if (Settings.canUserSeeBenchmarkInSettings(con, userId, benchId)) {
			return true;
		}

		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL CanViewBenchmark(?, ?)}");
			procedure.setInt(1, benchId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();

			if (results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}

	/**
	 * Checks to see if the user has access to the benchmark in some way. More specifically, this checks if the user
	 * belongs to any space the benchmark belongs to.
	 *
	 * @param benchId The benchmark to check if the user can see
	 * @param userId The user that is requesting to view the given benchmark	 *
	 * @return True if the user can somehow see the benchmark, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeBench(int benchId, int userId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return canUserSeeBench(con, benchId, userId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Checks to see if the user has access to the benchmarks in some way. More specifically, this checks if the user
	 * belongs to all spaces the benchmarks belong to.
	 *
	 * @param benchIds The benchmarks to check if the user can see
	 * @param userId The user that is requesting to view the given benchmarks
	 * @return True if the user can somehow see all benchmarks, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeBenchs(List<Integer> benchIds, int userId) {
		Connection con = null;
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return true;
		}
		try {
			con = Common.getConnection();
			//check the permissions for every benchmark
			for (int id : benchIds) {
				if (!canUserSeeBench(con, id, userId)) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Checks to see if the user has access to the job in some way. More specifically, this checks if the user belongs
	 * to any space the job belongs to.
	 *
	 * @param jobId The job to check if the user can see
	 * @param userId The user that is requesting to view the given job
	 * @return True if the user can somehow see the job, false otherwise
	 * @author Tyler Jensen
	 */
	public static ValidatorStatusCode canUserSeeJob(final int jobId, final int userId) {
		final String methodName = "canUserSeeJob";
		log.entry(methodName);
		log.debug(methodName, "\tjobId: " + jobId);
		log.debug(methodName, "\tuserId: " + userId);

		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			Job j = Jobs.get(jobId);
			// job does not exist or has been deleted.
			if (j == null) {
				final String message = "Job does not exist or has been deleted.";
				log.debug(methodName, "User can't see job: " + message);
				return new ValidatorStatusCode(false, message);
			}
			if (Jobs.isPublic(jobId)) {
				log.debug(methodName, "User can see job: Job is public.");
				return new ValidatorStatusCode(true);
			}
			if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
				log.debug(methodName, "User can see job: User has admin read privileges.");
				return new ValidatorStatusCode(true);
			}

			//if there was no special case, check to see if the user shares a space with the job or owns the job
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CanViewJob(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();

			if (results.first()) {
				boolean userCanSeeJob = results.getBoolean(1);
				if (userCanSeeJob) {
					log.debug(methodName, "User can see job.");
					return new ValidatorStatusCode(true);
				} else {
					final String failureMessage = "You do not have permission to view this job.";
					log.debug(methodName, "User can't see job: " + failureMessage);
					return new ValidatorStatusCode(false, failureMessage);
				}
			} else {
				log.warn(methodName, "Failed to retrieve results.");
				return new ValidatorStatusCode(false, "Database error: Failed to retrieve results.");
			}
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			return new ValidatorStatusCode(false, "Database error: Caught SQLException at " + Util.getTime());
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
			log.exit(methodName);
		}
	}

	/**
	 * Checks to see if the user has access to the solver in some way. More specifically, this checks if the user
	 * belongs to any space the solver belongs to.
	 *
	 * @param solverId The solver to check if the user can see
	 * @param userId The user that is requesting to view the given solver
	 * @param con The open connection to query on
	 * @return True if the user can somehow see the solver, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSolver(Connection con, int solverId, int userId) {
		Solver s = Solvers.getIncludeDeleted(con, solverId);
		if (s == null) {
			return false;
		}
		if (Solvers.isPublic(con, solverId)) {
			return true;
		}
		if (GeneralSecurity.hasAdminReadPrivileges(con, userId)) {
			return true;
		}
		if (Settings.canUserSeeSolverInSettings(con, userId, solverId)) {
			return true;
		}

		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL CanViewSolver(?, ?)}");
			procedure.setInt(1, solverId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();

			if (results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}

	/**
	 * Checks to see if the user has access to the solver in some way. More specifically, this checks if the user
	 * belongs to any space the solver belongs to.
	 *
	 * @param solverId The solver to check if the user can see
	 * @param userId The user that is requesting to view the given solver
	 * @return True if the user can somehow see the solver, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSolver(int solverId, int userId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return canUserSeeSolver(con, solverId, userId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Checks to see if the user has access to the given solvers in some way. More specifically, this checks if the
	 * user
	 * belongs to all the spaces the solvers belong to.
	 *
	 * @param solverIds The solvers to check if the user can see
	 * @param userId The user that is requesting to view the given solvers
	 * @return True if the user can somehow see the solvers, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSolvers(Collection<Integer> solverIds, int userId) {
		Connection con = null;
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return true;
		}
		try {
			con = Common.getConnection();
			//do the check for every solver
			for (int id : solverIds) {
				if (!canUserSeeSolver(con, id, userId)) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Checks to see if the user belongs to the given space. Note that this is to check whether a user can see the
	 * details of a space, not to check whether they can see the space in the explorer tree due to being a member of a
	 * subspace.
	 *
	 * @param spaceId The space to check if the user can see
	 * @param userId The user that is requesting to view the given space
	 * @return True if the user belongs to the space, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean canUserSeeSpace(int spaceId, int userId) {
		if (spaceId <= 1) {
			// Can always see root space
			return true;
		}
		if (Spaces.isPublicSpace(spaceId)) {
			return true;
		}
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return true;
		}
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CanViewSpace(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();

			if (results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}

	/**
	 * Retrieves the user's maximum set of permissions in a space.
	 *
	 * @param userId The user to get permissions for
	 * @param spaceId The id of the space to get the user's permissions on
	 * @return A permission object containing the user's permission on the space. Null if the user is not apart of the
	 * space.
	 * @author Tyler Jensen
	 */
	public static Permission get(int userId, int spaceId) {
		log.debug("getting permissions for user id = " + userId + " and space id  = " + spaceId);
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		Space s = Spaces.get(spaceId);
		if (s == null) {
			return null; //the space does not even exist
		}

		//the admin has full permissions everywhere
		if (GeneralSecurity.hasAdminWritePrivileges(userId)) {
			log.debug("permissions for an admin were obtained userId = " + userId);
			return Permissions.getFullPermission();
		}

		if (Users.isPublicUser(userId)) {
			if (Spaces.isPublicSpace(spaceId)) {
				return Permissions.getEmptyPermission();
			}
			return null;
		}
		try {

			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserPermissions(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);
			results = procedure.executeQuery();

			if (results.first()) {
				Permission p = resultsToPermissionWithId(userId, results);
				if (results.wasNull()) {
					/* If the permission doesn't exist we always get a result
					but all of it's values are null, so here we check for a
					null result and return null */

					if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
						Permission empty = Permissions.getEmptyPermission();
						empty.setAddSpace(true);
						empty.setAddUser(true);
						return empty;
					}
					return null;
				}

				return p;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Returns a permissions object with every permission set to true. The ID is not set
	 *
	 * @return Permissions object
	 * @author Eric Burns
	 */

	 /*
	  * Adds all permisions and makes the user a leader
	  * @docs aguo2
	  */
	public static Permission getFullPermission() {
		Permission p = new Permission();
		p.setAddBenchmark(true);
		p.setAddSolver(true);
		p.setAddSpace(true);
		p.setAddUser(true);
		p.setAddJob(true);
		p.setRemoveBench(true);
		p.setRemoveSolver(true);
		p.setRemoveSpace(true);
		p.setRemoveUser(true);
		p.setRemoveJob(true);
		p.setLeader(true);
		return p;
	}

	/**
	 * Returns a permissions object with every permission set to false. The ID is not set
	 *
	 * @return Permissions object
	 * @author Eric Burns
	 */

	public static Permission getEmptyPermission() {
		Permission p = new Permission();
		p.setAddBenchmark(false);
		p.setAddSolver(false);
		p.setAddSpace(false);
		p.setAddUser(false);
		p.setAddJob(false);
		p.setRemoveBench(false);
		p.setRemoveSolver(false);
		p.setRemoveSpace(false);
		p.setRemoveUser(false);
		p.setRemoveJob(false);
		p.setLeader(false);
		return p;
	}

	/**
	 * Retrieves the default permissions applied to a user when they are added to a space
	 *
	 * @param spaceId The id of the space to get the default user's permission
	 * @return A permission object containing the default user permission on the space.
	 * @author Tyler Jensen
	 */
	public static Permission getSpaceDefault(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpacePermissions(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			if (results.first()) {
				return resultsToPermissionWithId(results.getInt("id"), results);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	private static Permission resultsToPermissionWithId(int id, ResultSet results) throws SQLException {
		Permission p = new Permission();
		p.setId(id);
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

	/**
	 * Sets the permissions of a given user in a given space
	 *
	 * @param userId the id of the user to set the permissions of
	 * @param spaceId the id of the space where the permissions will effect
	 * @param newPerm the new set of permissions to set
	 * @return true iff the permissions were successfully set, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean set(int userId, int spaceId, Permission newPerm) {
		Connection con = null;

		try {
			con = Common.getConnection();
			return Permissions.set(userId, spaceId, newPerm, con);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		log.error(String.format("Changing permissions failed for user [%d] in space [%d]", userId, spaceId));
		return false;
	}

	/**
	 * Sets the permissions of a given user in a given space
	 *
	 * @param userId the id of the user to set the permissions of
	 * @param spaceId the id of the space where the permissions will effect
	 * @param newPerm the new set of permissions to set
	 * @param con The open connection to make the call on
	 * @return true iff the permissions were successfully set, false otherwise
	 * @throws Exception
	 * @author Todd Elvers
	 */
	protected static boolean set(int userId, int spaceId, Permission newPerm, Connection con) {
		CallableStatement procedure = null;
		int permissionId = add(newPerm, con);

		try {
			procedure = con.prepareCall("{CALL SetUserPermissions2(?, ?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);
			procedure.setInt(3, permissionId);

			procedure.executeUpdate();
			log.debug(String.format("Permissions successfully changed for user [%d] in space [%d]", userId, spaceId));
			return true;
		} catch (Exception e) {
			log.error("set", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates the permission with the given id. Since this will be one step in a multi-step process, we use
	 * transactions.
	 *
	 * @param permId the id of the permission to change
	 * @param perm a Permission object containing the new permissions
	 * @param con The open connection to make the call on
	 * @throws Exception
	 * @author Skylar Stark
	 */
	protected static void updatePermission(int permId, Permission perm, Connection con) {
		CallableStatement procedure = null;

		try {
			procedure = con.prepareCall("{CALL UpdatePermissions(?,?,?,?,?,?,?,?,?,?,?)}");

			procedure.setInt(1, permId);
			procedure.setBoolean(2, perm.canAddSolver());
			procedure.setBoolean(3, perm.canAddBenchmark());
			procedure.setBoolean(4, perm.canAddUser());
			procedure.setBoolean(5, perm.canAddSpace());
			procedure.setBoolean(6, perm.canAddJob());
			procedure.setBoolean(7, perm.canRemoveSolver());
			procedure.setBoolean(8, perm.canRemoveBench());
			procedure.setBoolean(9, perm.canRemoveSpace());
			procedure.setBoolean(10, perm.canRemoveUser());
			procedure.setBoolean(11, perm.canRemoveJob());

			procedure.executeUpdate();
			log.info(String.format("Permission [%d] successfully updated.", permId));
		} catch (Exception e) {
			log.error("updatePermission", e);
		} finally {
			Common.safeClose(procedure);
		}
	}
}
