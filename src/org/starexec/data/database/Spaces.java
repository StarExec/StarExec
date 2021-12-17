package org.starexec.data.database;

import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.CopyPrimitivesOption;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;
import org.starexec.util.dataStructures.TreeNode;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles all database interaction for spaces
 */
public class Spaces {
	private static final StarLogger log = StarLogger.getLogger(Spaces.class);

	/**
	 * Adds a new space to the system. This action adds the space, adds a default permission record for the space, and
	 * adds a new association to the space with the given user with full leadership permissions. NOTE: This is a
	 * multi-step process, use transactions to ensure it completes as an atomic unit.
	 *
	 * @param con The connection to perform the operation on
	 * @param s The space to add (should have default permissions set)
	 * @param userId The user who is adding the space
	 * @return The ID of the newly inserted space
	 * @author Tyler Jensen
	 */
	protected static int add(Connection con, Space s, int userId) throws SQLException {
		final String method = "add";
		CallableStatement procAddSpace = null;
		CallableStatement procSubspace = null;
		CallableStatement procAddUser = null;
		try {
			// Add the default permission for the space to the database
			int defaultPermId = Permissions.add(s.getPermission(), con);

			// Add the space with the default permissions
			procAddSpace = con.prepareCall("{CALL AddSpace(?, ?, ?, ?, ?, ?,?)}");
			procAddSpace.setString(1, s.getName());
			procAddSpace.setString(2, s.getDescription());
			procAddSpace.setBoolean(3, s.isLocked());
			procAddSpace.setInt(4, defaultPermId);
			procAddSpace.setInt(5, s.getParentSpace());
			procAddSpace.setBoolean(6, s.isStickyLeaders());
			procAddSpace.registerOutParameter(7, java.sql.Types.INTEGER);
			procAddSpace.executeUpdate();
			int newSpaceId = procAddSpace.getInt(7);

			log.trace(method, "Calling AssociateSpace");
			// Add the new space as a child space of the parent space
			procSubspace = con.prepareCall("{CALL AssociateSpaces(?, ?)}");
			procSubspace.setInt(1, s.getParentSpace());
			procSubspace.setInt(2, newSpaceId);
			procSubspace.executeUpdate();

			log.trace(method, "Calling AddUserToSpace");
			// Add the adding user to the space with the maximal permissions
			procAddUser = con.prepareCall("{CALL AddUserToSpace(?, ?)}");
			procAddUser.setInt(1, userId);
			procAddUser.setInt(2, newSpaceId);
			procAddUser.executeUpdate();

			Permission perm = new Permission(true);
			perm.setLeader(true);
			// Set maximal permissions for the user who added the space
			Permissions.set(userId, newSpaceId, perm, con);

			log.info(method, String.format("New space with name [%s] added by user [%d] to space [%d]", s.getName(), userId,
			                       s.getParentSpace()
			));
			return newSpaceId;
		} catch (SQLException e) {
			log.error(method, e);
			return -1;
		} finally {
			Common.safeClose(procAddUser);
			Common.safeClose(procSubspace);
			Common.safeClose(procAddSpace);
		}
	}

	/**
	 * Gets a list of all space ids in the path between the root space and the given space. In the list the root space
	 * will be the first id and the given space will be the last. The ids are ordered along the hierarchy. If the root
	 * (or anything smaller) is given, a size 1 list containing the root id is returned
	 *
	 * @param spaceId The ID of the space to get the chain for
	 * @return Ordered list of spaces tracing from root to given space.
	 * @author Eric Burns
	 */
	public static List<Integer> getChainToRoot(int spaceId) {
		List<Integer> idChain = new ArrayList<>();
		if (spaceId <= 1) {
			idChain.add(1);
			return idChain;
		}
		idChain.add(spaceId);
		HashSet<Integer> alreadySeen = new HashSet<>();
		while (spaceId > 1) {
			if (alreadySeen.contains(spaceId)) {
				log.error("there was a cycle in the space hierarchy!");
				return null; // found a cycle in the space hierarchy
			}
			alreadySeen.add(spaceId);
			spaceId = Spaces.getParentSpace(spaceId);
			idChain.add(spaceId);
		}
		Collections.reverse(idChain);
		return idChain;
	}

	/**
	 * Adds a new space to the system. This action adds the space, adds a default permission record for the space, and
	 * adds a new association to the space with the given user with full leadership permissions
	 *
	 * @param s The space to add (should have default permissions set)
	 * @param userId The user who is adding the space
	 * @return The ID of the newly inserted space, -1 if the operation failed
	 * @author Tyler Jensen
	 */
	public static int add(Space s, int userId) {
		Connection con = null;

		try {
			con = Common.getConnection();

			Common.beginTransaction(con);
			// Add space is a multi-step process, so we need to use a transaction
			int newSpaceId = Spaces.add(con, s, userId);

			Common.endTransaction(con);
			return newSpaceId;
		} catch (Exception e) {
			log.error("add", e);
		} finally {
			Common.doRollback(con);
			Common.safeClose(con);
		}
		return -1;
	}

	/**
	 * For a given job space, set the maximum number of stages that occur in any job space under that space
	 *
	 * @param jobSpaceId The ID of the job space
	 * @param maxStages The maximum number of stages
	 * @param con An open connection to make the call on
	 * @return True on success and false on error
	 */
	public static boolean setJobSpaceMaxStages(int jobSpaceId, int maxStages, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL SetJobSpaceMaxStages(?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, maxStages);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("setJobSpaceMaxStages", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Sets the max_stages column in the job_spaces table for the given job space to the given value
	 *
	 * @param jobSpaceId The ID of the job space in question
	 * @param maxStages The maximum number of stages for any pair in the hierarchy rooted at this job space
	 * @return True on success and false otherwise
	 */
	public static boolean setJobSpaceMaxStages(int jobSpaceId, int maxStages) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return setJobSpaceMaxStages(jobSpaceId, maxStages, con);
		} catch (Exception e) {
			log.error("setJobSpaceMaxStages", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Creates a new job space with the given name
	 *
	 * @param name The name to be given to the job space
	 * @param jobId The ID of the job that owns this job space
	 * @return The ID of the newly created job space, or -1 on failure
	 * @author Eric Burns
	 */
	public static int addJobSpace(String name, int jobId) {
		Connection con = null;
		log.debug("adding new job space with name = " + name);
		try {
			con = Common.getConnection();
			return addJobSpace(name, jobId, con);
		} catch (Exception e) {
			log.error("addJobSpace", e);
		} finally {
			Common.safeClose(con);
		}
		return -1;
	}

	/**
	 * Clears entries from the job space closure table that haven't been used in more than the given number of days
	 *
	 * @param daysOlderThan
	 * @return True on success and false otherwise
	 */
	public static boolean clearJobClosureEntries(int daysOlderThan) {
		Timestamp cutoffTime = new Timestamp(
				System.currentTimeMillis() - (TimeUnit.MILLISECONDS.convert(daysOlderThan, TimeUnit.DAYS)));
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL ClearOldJobClosureEntries(?)}");
			procedure.setTimestamp(1, cutoffTime);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("clearJobClosureEntries", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Adds a given ancestor / descendant pair to the job space closure table
	 *
	 * @param ancestor
	 * @param descendant
	 * @param time
	 * @param con
	 * @return
	 */
	private static boolean addToJobSpaceClosure(int ancestor, int descendant, Timestamp time, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL InsertIntoJobSpaceClosure(?,?,?)}");
			procedure.setInt(1, ancestor);
			procedure.setInt(2, descendant);
			procedure.setTimestamp(3, time);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("addToJobSpaceClosure", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Checks to see whether a specific ancestor is in the job space closure table. If it does exist, the last_used
	 * column is updated
	 *
	 * @param jobSpaceId
	 * @return
	 */
	private static boolean jobSpaceAncestorExists(int jobSpaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			Timestamp time = new Timestamp(System.currentTimeMillis());
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL RefreshEntriesByAncestor(?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setTimestamp(2, time);
			results = procedure.executeQuery();
			if (results.next()) {
				//it exists if there is an entry
				return results.getInt("count") > 0;
			}
		} catch (Exception e) {
			log.error("jobSpaceAncestorExists", "jobSpaceId="+jobSpaceId, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Adds every entry necessary in the closure table where the given space is the root. If the entries are already
	 * present, this function just returns true
	 *
	 * @param jobSpaceId
	 */
	public static void updateJobSpaceClosureTable(int jobSpaceId) {
		int callID = new Random().nextInt();
		log.debug("beginning updateJobSpaceClosureTable " + callID);
		if (jobSpaceAncestorExists(jobSpaceId)) {
			//don't update-- it is already present
			log.debug("closure entries were already present");
			return;
		}
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			boolean success = updateJobSpaceClosureTable(jobSpaceId, con);
			if (!success) {
				Common.doRollback(con);
				log.debug("ending with error updateJobSpaceClosureTable " + callID);

				return;
			}
			Common.endTransaction(con);
			log.debug("ending successfully updateJobSpaceClosureTable " + callID);
		} catch (Exception e) {
			Common.doRollback(con);
			log.error("updateJobSpaceClosureTable", e);
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Adds every entry necessary in the closure table where the given space is the root
	 *
	 * @param jobSpaceId
	 * @return
	 */
	private static boolean updateJobSpaceClosureTable(int jobSpaceId, Connection con) {
		try {
			Timestamp time = new Timestamp(System.currentTimeMillis());
			JobSpace root = new JobSpace();
			root.setId(jobSpaceId);
			List<JobSpace> spaces = Spaces.getSubSpacesForJob(jobSpaceId, true);
			spaces.add(root);
			for (JobSpace s : spaces) {
				boolean success = addToJobSpaceClosure(root.getId(), s.getId(), time, con);
				if (!success) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			log.error("updateJobSpaceClosureTable", e);
		}
		return false;
	}

	/**
	 * Adds a new job space to the job space table
	 *
	 * @param name The name of the job space
	 * @param jobId The ID of the job this job space belongs to
	 * @param con The open connection to make the call on
	 * @return The ID of the new job space, or -1 if the addition was not successful
	 */
	public static int addJobSpace(String name, int jobId, Connection con) {
		CallableStatement procedure = null;
		log.debug("adding new job space with name = " + name + " and id = " + jobId);
		try {
			// Add the space with the default permissions
			procedure = con.prepareCall("{CALL AddJobSpace(?,?,?)}");
			procedure.setString(1, name);
			procedure.setInt(2, jobId);
			procedure.registerOutParameter(3, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			return procedure.getInt(3);
		} catch (Exception e) {
			log.error("addJobSpace", e);
		} finally {
			Common.safeClose(procedure);
		}
		return -1;
	}

	/**
	 * Adds all subspaces and their benchmarks to the database. The first space given should be an existing space (it
	 * must have an ID that will be the ancestor space of all subspaces) and it can optionally contain NEW
	 * benchmarks to
	 * be added to the space. The method then traverses into its subspaces recursively and adds them to the database
	 * aint with their benchmarks.
	 *
	 * @param parent The parent space that is the 'root' of the new subtree to be added
	 * @param userId The user that will own the new spaces and benchmarks
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie. If no dependencies exist, this is
	 * ignored.
	 * @param linked true if the depRootSpace is the same as the first directory in the include statements. If no
	 * dependencies exist, this is ignored
	 * @param statusId ID of a benchmark upload status to update with space information
	 * @author Benton McCune
	 */
	public static void addWithBenchmarks(
			Space parent, int userId, Integer depRootSpaceId, boolean linked, Integer statusId, Boolean usesDeps
						      ) throws IOException , StarExecException {
	    log.info("addWithBenchmarks called on space " + parent.getName());

		// For each subspace...
		for (Space sub : parent.getSubspaces()) {
		    // Apply the recursive algorithm to add each subspace
		    sub.setParentSpace(parent.getId());
		    Spaces.traverse(sub, userId, depRootSpaceId, linked, statusId);
		}
		
		// Add any new benchmarks in the space to the database
		if (!parent.getBenchmarks().isEmpty()) {
		    Benchmarks.processAndAdd(parent.getBenchmarks(), parent.getId(), depRootSpaceId, linked,
							statusId, usesDeps
							);
		}
		// We're done (notice that 'parent' is never added because it should already exist)

	}

	/**
	 * Given a parent ID and a child ID, associates two job spaces
	 *
	 * @param parentId The ID of the parent space
	 * @param childId The ID of the child space
	 * @return True on success and false otherwise
	 * @author Eric Burns
	 */
	public static boolean associateJobSpaces(int parentId, int childId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return associateJobSpaces(parentId, childId, con);
		} catch (Exception e) {
			log.error("associateJobSpaces", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Given a parent ID and a child ID, associates two job spaces
	 *
	 * @param parentId The ID of the parent space
	 * @param childId The ID of the child space
	 * @param con The open connection to run the procedure on
	 * @return True on success and false otherwise
	 * @author Eric Burns
	 */
	public static boolean associateJobSpaces(int parentId, int childId, Connection con) {
		CallableStatement procedure = null;
		try {
			log.debug("associating parent job space " + parentId + " with child job space " + childId);
			procedure = con.prepareCall("{CALL AssociateJobSpaces(?, ?)}");
			procedure.setInt(1, parentId);
			procedure.setInt(2, childId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("associateJobSpaces", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Gets a space with minimal information (only details about the space itself)
	 *
	 * @param spaceId The id of the space to get information for
	 * @param con The open connection to make the call on.
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space get(int spaceId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetSpaceById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			if (results.next()) {
				Space s = new Space();

				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));
				s.setCreated(results.getTimestamp("created"));
				s.setPublic(results.getBoolean("public_access"));
				s.setStickyLeaders(results.getBoolean("sticky_leaders"));
				return s;
			}
		} catch (Exception e) {
			log.error("get", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets a space with minimal information (only details about the space itself)
	 *
	 * @param spaceId The id of the space to get information for
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space get(int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return get(spaceId, con);
		} catch (Exception e) {
			log.error("get", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all spaces
	 *
	 * @return A list of all spaces
	 * @author Wyatt Kaiser
	 */
	public static List<Space> getAllSpaces() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllSpaces()}");
			results = procedure.executeQuery();
			return resultsToSpaces(results);
		} catch (Exception e) {
			log.error("getAllSpaces", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	protected static List<Space> resultsToSpaces(ResultSet results) throws SQLException {
		List<Space> spaces = new ArrayList<>();
		while (results.next()) {
			Space s = new Space();
			s.setName(results.getString("name"));
			s.setId(results.getInt("id"));
			s.setDescription(results.getString("description"));
			try {
				s.setLocked(results.getBoolean("locked"));
			} catch (SQLException e) {}
			try {
				s.setParentSpace(results.getInt("parent"));
			} catch (SQLException e) {}
			spaces.add(s);
		}
		return spaces;
	}

	/**
	 * Get the id of the community where the space belongs.
	 *
	 * @param id the space from which to get its community
	 * @return the id of the community of the space
	 */
	public static int getCommunityOfSpace(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetCommunityOfSpace(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("community");
			}
			return -1;
		} catch (Exception e) {
			log.error("getCommunityOfSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Removes a user's association to every space within the hierarchy rooted at the given space
	 *
	 * @param userId the id of the user to remove from the space
	 * @param spaceId the id of the space to remove the user from
	 * @return true iff the user was successfully removed from the community referenced by 'commId', false otherwise
	 * @author Todd Elvers
	 */
	public static boolean leave(int userId, int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL LeaveHierarchy(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);

			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("leave", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Gets the number of  subspaces of a given job space
	 *
	 * @param jobSpaceId the id of the job space to count the Spaces in
	 * @return the number of JobSpaces
	 * @author Eric Burns
	 */
	public static int getCountInJobSpace(int jobSpaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSubspaceCountByJobSpaceId(?)}");
			procedure.setInt(1, jobSpaceId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("spaceCount");
			}
		} catch (Exception e) {
			log.error("getCountInJobSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Returns a count of the number of subspaces that exist in the hierarchy rooted at spaceId
	 *
	 * @param spaceId
	 * @return
	 */
	private static int getCountInSpaceHierarchy(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetTotalSubspaceCountBySpaceIdInHierarchy(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("spaceCount");
			}
		} catch (Exception e) {
			log.error("getCountInSpaceHierarchy", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Gets the number of subspaces a space has.
	 *
	 * @param spaceId The ID of the space to retrieve the subspace count for
	 * @return The integer number of spaces, or -1 on error
	 */
	public static int getCountInSpace(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSubspaceCountBySpaceIdAdmin(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("spaceCount");
			}
		} catch (Exception e) {
			log.error("getCountInSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Gets the number of Spaces in a given space that a given user can see
	 *
	 * @param spaceId the id of the space to count the Spaces in
	 * @param userId the id of the user making the request
	 * @param hierarchy True to count down the full space hierarchy and false to count only directly under the space
	 * @return the number of Spaces
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId, int userId, boolean hierarchy) {
		//the admin can see every space, so we don't need to worry about finding only spaces some user can see
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			if (hierarchy) {
				return getCountInSpaceHierarchy(spaceId);
			}
			return getCountInSpace(spaceId);
		}
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			if (hierarchy) {
				procedure = con.prepareCall("{CALL GetSubspaceCountBySpaceIdInHierarchy(?, ?)}");
			} else {
				procedure = con.prepareCall("{CALL GetSubspaceCountBySpaceId(?, ?)}");
			}
			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("spaceCount");
			}
		} catch (Exception e) {
			log.error("getCountInSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Gets the number of Spaces in a given space that match a given query
	 *
	 * @param spaceId the id of the space to count the Spaces in
	 * @param userId the id of the user making the request
	 * @param query The query to match the spaces against
	 * @return the number of Spaces
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, int userId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetSubspaceCountBySpaceIdWithQuery(?, ?, ?)}");

			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			procedure.setString(3, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("spaceCount");
			}
		} catch (Exception e) {
			log.error("getCountInSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Move an existing space into a new parent space
	 *
	 * @param srcId The Id of the space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 */
	public static void moveSpace(int srcId, int desId) throws SQLException {
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.updateUsingConnection(con, "{CALL MoveSpace(?,?)}",
				procedure -> {
					procedure.setInt(1, desId);
					procedure.setInt(2, srcId);
				}
			);
			rebuildSpaceClosures(srcId, con);
		} catch (SQLException e) {
			Common.doRollback(con);
			throw e;
		} finally {
			Common.safeClose(con);
		}
	}

	private static void rebuildSpaceClosures(int srcId, Connection con) throws SQLException {
		for (Integer spaceId : getSubSpaceIds(srcId)) {
			Common.updateUsingConnection(con, "{CALL RebuildSpaceClosures(?)}",
				procedure -> procedure.setInt(1, spaceId)
			);
			rebuildSpaceClosures(spaceId, con);
		}
	}

	/**
	 * Copy a space into another space
	 *
	 * @param srcId The Id of the space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 * @param usrId The Id of the user doing the copy.
	 * @return The Id of the new copy of the space.
	 * @throws StarExecException if space copy fails.
	 * @author Ruoyu Zhang
	 */
	public static int copySpace(int srcId, int desId, int usrId) throws StarExecException {
		//invoke helper method assuming final parameter of copyPrimites is set to false
		return copySpace(srcId, desId, usrId, CopyPrimitivesOption.LINK, null);
	}

	/**
	 * Copy a space into another space
	 *
	 * @param srcId The Id of the space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 * @param usrId The Id of the user doing the copy.
	 * @param copyPrimitives the option of how to copy/link primitives
	 * @return The Id of the new copy of the space.
	 * @throws StarExecException if space copy fails.
	 * @author Ruoyu Zhang
	 */
	public static int copySpace(
			int srcId, int desId, int usrId, CopyPrimitivesOption copyPrimitives, Double sampleRate
	) throws StarExecException {
		if (srcId == desId) {
			throw new StarExecException("A space can't be copied into itself.");
		}

		if (copyPrimitives.shouldLinkSampleOfBenchmarks() && sampleRate == null) {
			throw new IllegalArgumentException("You must provide a sample rate to use the sample benchmarks option.");
		}

		Space sourceSpace = Spaces.getDetails(srcId, usrId);

		// Create a new space
		Space tempSpace = new Space();
		tempSpace.setName(sourceSpace.getName());
		tempSpace.setDescription(sourceSpace.getDescription());
		tempSpace.setLocked(sourceSpace.isLocked());
		tempSpace.setBenchmarks(sourceSpace.getBenchmarks());
		tempSpace.setSolvers(sourceSpace.getSolvers());
		tempSpace.setJobs(sourceSpace.getJobs());

		// Set the default permission on the space
		tempSpace.setPermission(sourceSpace.getPermission());
		tempSpace.setParentSpace(desId);
		final int newSpaceId = Spaces.add(tempSpace, usrId);
		if (newSpaceId <= 0) {
			throw new StarExecException(
					"Copying space with name '" + sourceSpace.getName() + "' to space with id '" + desId +
							"' failed for user with id '" + usrId + "'");
		}

		if (Permissions.canUserSeeSpace(srcId, usrId)) {
			// Copying the references of benchmarks
			List<Benchmark> benchmarks = sourceSpace.getBenchmarks();
			List<Integer> benchmarkIds = new LinkedList<>();
			Random rand = new Random();
			if (copyPrimitives.shouldCopyBenchmarks()) {
				Benchmarks.copyBenchmarks(benchmarks, usrId, newSpaceId);
			} else if (copyPrimitives.shouldLinkAllBenchmarks() || copyPrimitives.shouldLinkSampleOfBenchmarks()) {
				for (Benchmark benchmark : benchmarks) {
					int benchId = benchmark.getId();
					// always include the benchmark if we're not sampling.
					boolean includeBenchInSample = true;
					if (copyPrimitives.shouldLinkSampleOfBenchmarks()) {
						includeBenchInSample = rand.nextDouble() < sampleRate;
						if (includeBenchInSample) {
							log.debug("Including benchmark " + benchmark.getId() + " in sample.");
						} else {
							log.debug("Not including benchmark " + benchmark.getId() + " in sample.");
						}
					}

					if (Permissions.canUserSeeBench(benchId, usrId) && includeBenchInSample) {
						benchmarkIds.add(benchId);
					}
				}
				Benchmarks.associate(benchmarkIds, newSpaceId);
			}

			// Copying the references of solvers
			List<Solver> solvers = sourceSpace.getSolvers();
			List<Integer> solverIds = new LinkedList<>();

			if (copyPrimitives.shouldCopySolvers()) {
				Solvers.copySolvers(solvers, usrId, newSpaceId);
			} else if (copyPrimitives.shouldLinkSolvers()) {
				for (Solver solver : solvers) {
					int solverId = solver.getId();
					if (Permissions.canUserSeeSolver(solverId, usrId)) {
						solverIds.add(solverId);
					}
				}
				Solvers.associate(solverIds, newSpaceId);
			}

			if (copyPrimitives.shouldLinkJobs()) {
				// Copying the references of jobs
				List<Job> jobs = sourceSpace.getJobs();
				List<Integer> jobIds = new LinkedList<>();
				int jobId = 0;
				for (Job job : jobs) {
					jobId = job.getId();
					if (Permissions.canUserSeeJob(jobId, usrId).isSuccess()) {
						jobIds.add(jobId);
					}
				}
				Jobs.associate(jobIds, newSpaceId);
			}
		}
		return newSpaceId;
	}

	/**
	 * Copy a hierarchy of the space into another space helper method.
	 *
	 * @param srcId The Id of the source space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 * @param usrId The Id of the user doing the copy.
	 * @return The Id of the root space of the copied hierarchy.
	 * @throws StarExecException If the source and destination are the same
	 */
	public static int copyHierarchy(int srcId, int desId, int usrId) throws StarExecException {
		return copyHierarchy(srcId, desId, usrId, CopyPrimitivesOption.LINK, null);
	}

	/**
	 * Copy a hierarchy of the space into another space
	 *
	 * @param srcId The Id of the source space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 * @param usrId The Id of the user doing the copy.
	 * @param copyPrimitives the option of how to copy/link primitives
	 * @return The Id of the root space of the copied hierarchy.
	 * @throws StarExecException If the source and destination are the same
	 * @author Ruoyu Zhang
	 */
	public static int copyHierarchy(
			int srcId, int desId, int usrId, CopyPrimitivesOption copyPrimitives, Double sampleRate
	) throws StarExecException {
		if (srcId == desId) {
			throw new StarExecException("You can't copy a space into itself.");
		}

		Space sourceSpace = Spaces.get(srcId);
		TreeNode<Space> spaceTree = Spaces.buildSpaceTree(sourceSpace, usrId);
		log.debug("Space tree built during space hierarchy copy:");
		logSpaceTree(spaceTree);

		return Spaces.copySpaceTree(spaceTree, desId, usrId, copyPrimitives, sampleRate);
	}

	private static void logSpaceTree(TreeNode<Space> tree) {
		logSpaceTreeHelper(tree, "");
	}

	private static void logSpaceTreeHelper(TreeNode<Space> tree, String indent) {
		StringBuilder childrenMessage = new StringBuilder();
		childrenMessage.append(tree.getData().getName()).append(": ");
		for (TreeNode<Space> child : tree) {
			childrenMessage.append(child.getData().getName()).append(" ");
		}

		log.debug(indent + "Descendants of space " + childrenMessage.toString());

		for (TreeNode<Space> child : tree) {
			logSpaceTreeHelper(child, indent + "    ");
		}
	}

	/**
	 * Copies a whole space tree (helper method).
	 *
	 * @param spaceTree The tree to copy
	 * @param desId the id of the space which will be the parent of the root of the space tree.
	 * @param usrId the id of the user who is copying the space tree.
	 * @return the id of the root of the new space tree.
	 * @throws StarExecException if something went wrong while copying the space tree.
	 */
	public static int copySpaceTree(TreeNode<Space> spaceTree, int desId, int usrId) throws StarExecException {
		return copySpaceTree(spaceTree, desId, usrId, CopyPrimitivesOption.LINK, null);
	}

	/**
	 * Copies a whole space tree.
	 *
	 * @param spaceTree The tree to copy
	 * @param desId the id of the space which will be the parent of the root of the space tree.
	 * @param usrId the id of the user who is copying the space tree.
	 * @param copyPrimitives the option of how to copy/link primitives
	 * @return the id of the root of the new space tree.
	 * @throws StarExecException if something went wrong while copying the space tree.
	 * @author Albert Giegerich
	 */
	public static int copySpaceTree(
			TreeNode<Space> spaceTree, int desId, int usrId, CopyPrimitivesOption copyPrimitives, Double sampleRate
	) throws StarExecException {
		Space rootSpace = spaceTree.getData();
		int newSpaceId = copySpace(rootSpace.getId(), desId, usrId, copyPrimitives, sampleRate);
		for (TreeNode<Space> child : spaceTree) {
			// Recursively copy each child into the newly created space.
			copySpaceTree(child, newSpaceId, usrId, copyPrimitives, sampleRate);
		}
		return newSpaceId;
	}

	/**
	 * Builds a tree hierarchy of the spaces.
	 *
	 * @param rootSpace the root space of the space tree
	 * @param usrId the id of the user who wants to use the spaces
	 * @return a tree of spaces with rootSpace at the root
	 * @throws StarExecException if something wen wrong building the space tree.
	 * @author Albert Giegerich
	 */
	public static TreeNode<Space> buildSpaceTree(Space rootSpace, int usrId) throws StarExecException {
		return buildSpaceTreeHelper(rootSpace, usrId, false);
	}

	/**
	 * Builds a tree hierarchy of the spaces with detailed information about each space in each node.
	 *
	 * @param rootSpace the root space of the space tree
	 * @param usrId the id of the user who wants to use the spaces
	 * @return a tree of spaces with rootSpace at the root
	 * @throws StarExecException if something wen wrong building the space tree.
	 * @author Albert Giegerich
	 */
	public static TreeNode<Space> buildDetailedSpaceTree(Space rootSpace, int usrId) throws StarExecException {
		return buildSpaceTreeHelper(rootSpace, usrId, true);
	}

	private static TreeNode<Space> buildSpaceTreeHelper(Space rootSpace, int usrId, boolean getDetails)
			throws StarExecException {
		List<Space> subSpaces;
		try {
			subSpaces = Spaces.getSubSpaces(rootSpace.getId(), usrId);
		} catch (Exception e) {
			throw new StarExecException(
					"Could not get subspaces for space with id=" + rootSpace.getId() + " for user with" + " id=" +
							usrId, e);
		}
		// Base case for when rootSpace is a leaf.
		if (subSpaces == null) {
			return null;
		}
		if (getDetails) {
			rootSpace = Spaces.getDetails(rootSpace.getId(), usrId);
		}
		TreeNode<Space> spaceTree = new TreeNode<>(rootSpace);
		for (Space space : subSpaces) {
			TreeNode<Space> child = buildSpaceTree(space, usrId);
			if (child != null) {
				spaceTree.addChild(child);
			}
		}
		return spaceTree;
	}

	/**
	 * Gets a space with detailed information (solvers, benchmarks, jobs and user belonging to the space are also
	 * populated (but not job pairs))
	 *
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of user requesting the space used to view details from their perspective
	 * @return A space object consisting of detailed information about the space
	 * @author Tyler Jensen
	 */
	public static Space getDetails(int spaceId, int userId) {
		try {
			Space s = Spaces.get(spaceId);
			if (s == null) {
				return null;
			}
			s.setUsers(Spaces.getUsers(spaceId));
			s.setBenchmarks(Benchmarks.getBySpace(spaceId));
			s.setSolvers(Solvers.getBySpace(spaceId));
			s.setJobs(Jobs.getBySpace(spaceId));
			s.setSubspaces(Spaces.getSubSpaces(spaceId, userId));
			s.setPermission(Permissions.getSpaceDefault(spaceId));
			return s;
		} catch (Exception e) {
			log.error("getDetails", e);
		}
		return null;
	}

	/**
	 * Gets a job space's name and id
	 *
	 * @param jobSpaceId The id of the job space to get information for
	 * @return A space object of just an ID and a name
	 * @author Eric Burns
	 */
	public static JobSpace getJobSpace(int jobSpaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobSpaceById(?)}");
			procedure.setInt(1, jobSpaceId);
			results = procedure.executeQuery();

			if (results.next()) {
				JobSpace s = new JobSpace();
				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setJobId(results.getInt("job_id"));
				s.setMaxStages(results.getInt("max_stages"));
				return s;
			}
		} catch (Exception e) {
			log.error("getJobSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets the users that are the leaders of a given space
	 *
	 * @param spaceId the id of the space to get the leaders of
	 * @return a list of leaders of the given space
	 * @author Todd Elvers
	 */
	public static List<User> getLeaders(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetLeadersBySpaceId(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			List<User> leaders = new LinkedList<>();

			while (results.next()) {
				User u = new User();
				u.setId(results.getInt("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				u.setDiskQuota(results.getLong("disk_quota"));
				leaders.add(u);
			}
			return leaders;
		} catch (Exception e) {
			log.error("getLeaders", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets the name of a space by Id - helper method to work around permissions for this special case
	 *
	 * @param spaceId the id of the community to get the name of
	 * @return the name of the community
	 * @author Todd Elvers
	 */
	public static String getName(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpaceById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			String name = null;

			if (results.next()) {
				name = results.getString("name");
			}
			return name;
		} catch (Exception e) {
			log.error("getName", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets the parent space belong to the given spaceId
	 *
	 * @param spaceId the id of the space to get the parent of
	 * @return the id of the parent space or -1 on error or if none exists
	 * @author Wyatt Kaiser
	 */
	public static int getParentSpace(int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Spaces.getParentSpace(spaceId, con);
		} catch (Exception e) {
			log.error("getParentSpace", e);
		} finally {
			Common.safeClose(con);
		}
		return -1;
	}

	/**
	 * Given a space id, returns the id of the parent space
	 *
	 * @param spaceId the id of the space to get parent of
	 * @param con the database connection to use
	 * @return the Id of the parent space or -1 on error
	 * @author Wyatt Kaiser
	 */
	protected static int getParentSpace(int spaceId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetParentSpaceById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("id");
			}
		} catch (Exception e) {
			log.error("getParentSpace", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return -1;
	}

	/**
	 * Gets all spaces the user has access to
	 *
	 * @param userId The id of the user requesting the spaces.
	 * @return A list of spaces the user has access to
	 * @author Benton McCune
	 */
	public static List<Space> getSpacesByUser(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpacesByUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			List<Space> spaces = new LinkedList<>();

			while (results.next()) {
				Space s = new Space();
				s.setName(results.getString("space.name"));
				s.setId(results.getInt("space.id"));
				s.setDescription(results.getString("space.description"));
				s.setLocked(results.getBoolean("space.locked"));
				s.setStickyLeaders(results.getBoolean("space.sticky_leaders"));
				spaces.add(s);
			}
			return spaces;
		} catch (Exception e) {
			log.error("getSpacesByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	private static String getSpaceOrderColumn(int orderIndex) {
		if (orderIndex == 0) {
			return "name";
		} else if (orderIndex == 1) {
			return "description";
		}
		return "name";
	}

	/**
	 * Gets the minimal number of Spaces necessary in order to service the client's request for the next page of Spaces
	 * in their DataTables object
	 *
	 * @param query A DataTablesQuery object
	 * @param spaceId the id of the space to get the Spaces from
	 * @param userId the id of the user making the request; used to filter out Spaces user isn't a member of
	 * @return a list of 10, 25, 50, or 100 Spaces containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<Space> getSpacesForNextPage(DataTablesQuery query, int spaceId, int userId) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_SUBSPACES_IN_SPACE_QUERY,
			                                                            getSpaceOrderColumn(query.getSortColumn()),
			                                                            query
			);

			procedure = new NamedParameterStatement(con, builder.getSQL());

			procedure.setInt("spaceId", spaceId);
			procedure.setInt("userId", userId);
			procedure.setString("query", query.getSearchQuery());
			results = procedure.executeQuery();
			return resultsToSpaces(results);
		} catch (Exception e) {
			log.error("getSpacesForNextPage", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Given a space ID and a space path of the form "subspace1name/subspace2name/subspace3name"... returns the ID of
	 * the space that is at the end of the path. In other words, this function searches down through the space
	 * hierarchy
	 * using a path rooted at the given space
	 *
	 * @param rootSpaceId
	 * @param path
	 * @param con
	 * @return The ID of the space identified by the path, or -1 if it does not exist. Returns null on error
	 */
	public static Integer getSubSpaceIDByPath(Integer rootSpaceId, String path, Connection con) {
		try {
			String[] spaceNames = path.split(R.JOB_PAIR_PATH_DELIMITER);
			int returnId = rootSpaceId;

			for (String spaceName : spaceNames) {
				returnId = getSubSpaceIDbyName(returnId, spaceName, con);
				if (returnId == -1) {
					return -1; //means the space could not be found
				}
			}
			return returnId;
		} catch (Exception e) {
			log.error("getSubSpaceIDByPath", e);
		}
		return null;
	}

	/**
	 * Given a space ID and a space path of the form "subspace1name/subspace2name/subspace3name"... returns the ID of
	 * the space that is at the end of the path. In other words, this function searches down through the space
	 * hierarchy
	 * using a path rooted at the given space
	 *
	 * @param rootSpaceId
	 * @param path
	 * @return The ID of the space identified by the path, or -1 if it does not exist. Returns null on error
	 */
	public static Integer getSubSpaceIDByPath(Integer rootSpaceId, String path) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getSubSpaceIDByPath(rootSpaceId, path, con);
		} catch (Exception e) {
			log.error("getSubSpaceIDByPath", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * returns id of subspace with a particular name (-1 if more or less than 1 found)
	 *
	 * @param spaceId id of parent space
	 * @param userId id of user making request
	 * @param subSpaceName name of subspace that is being sought
	 * @param con The open connection to make the call on
	 * @return subspaceId id of found subspace, or -1 if none exist
	 * @author Benton McCune
	 */
	public static Integer getSubSpaceIDbyName(Integer spaceId, Integer userId, String subSpaceName, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetSubSpaceByName(?,?,?)}");

			log.debug("Space ID = " + spaceId);
			procedure.setInt(1, spaceId);
			log.debug("User Id = " + userId);
			procedure.setInt(2, userId);
			log.debug("Subspace named " + subSpaceName);
			procedure.setString(3, subSpaceName);
			results = procedure.executeQuery();
			Integer subSpaceId = -1;

			if (results.next()) {
				subSpaceId = (results.getInt("id"));
				log.debug("SubSpace Id = " + subSpaceId);
			}
			results.last();
			log.debug("# of subspaces named " + subSpaceName + " = " + results.getRow());
			if (results.getRow() != 1) //should only be getting one result
			{
				log.debug("returning -1");
				return -1;
			}
			return subSpaceId;
		} catch (Exception e) {
			log.error("getSubSpaceIDbyName", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * returns id of subspace with a particular name (-1 if more or less than 1 found)
	 *
	 * @param spaceId id of parent space
	 * @param userId id of user making request
	 * @param subSpaceName name of subspace that is being sought
	 * @return subspaceId id of found subspace, or -1 if none exist
	 * @author Benton McCune
	 */
	public static Integer getSubSpaceIDbyName(Integer spaceId, Integer userId, String subSpaceName) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getSubSpaceIDbyName(spaceId, userId, subSpaceName, con);
		} catch (Exception e) {
			log.error("getSubSpaceIDbyName", e);
		} finally {
			Common.safeClose(con);
		}
		return -1;
	}

	/**
	 * Gets the id of all subspaces of the given space with a particular name
	 *
	 * @param spaceId ID of the parent space
	 * @param subSpaceName Name of the subspace that is being looked for
	 * @return The id of the subspace with the given name, or -1 if none exist
	 * @author Eric Burns
	 */
	public static Integer getSubSpaceIDbyName(Integer spaceId, String subSpaceName) {
		return getSubSpaceIDbyName(spaceId, -1, subSpaceName);
	}

	/**
	 * Gets the id of all subspaces of the given space with a particular name
	 *
	 * @param spaceId ID of the parent space
	 * @param subSpaceName Name of the subspace that is being looked for
	 * @param con The open connection to make the call on
	 * @return The id of the subspace with the given name, or -1 if none exist
	 * @author Eric Burns
	 */
	public static Integer getSubSpaceIDbyName(Integer spaceId, String subSpaceName, Connection con) {
		return getSubSpaceIDbyName(spaceId, -1, subSpaceName, con);
	}

	/**
	 * Gets the ids of all subspaces of a given space (non-recursive)
	 *
	 * @param spaceId The space to get subspaces for
	 * @return A list of integers representing the ids of the subspaces
	 * @author Eric Burns
	 */
	public static List<Integer> getSubSpaceIds(int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Spaces.getSubSpaceIds(spaceId, con);
		} catch (Exception e) {
			log.error("getSubSpaceIds", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets the ids of all the subspaces of a given space (non recursive)
	 *
	 * @param spaceId The id of the space to get subspaces of
	 * @param con An open connection that will be used to make the calls
	 * @return A list of subspace ids
	 * @throws Exception
	 * @author Eric Burns
	 */
	public static List<Integer> getSubSpaceIds(int spaceId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetSubspaceIds(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<>();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("getSubSpaceIds", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets every subspace in the hierarchy rooted at spaceId that the given user can see
	 *
	 * @param spaceId The root space of the hierarchy
	 * @param userId The ID of the user making the request
	 * @return A List of Space objects
	 */
	public static List<Space> getSubSpaceHierarchy(int spaceId, int userId) {
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return getSubSpaceHierarchy(spaceId); //not dependent on user, as admins can see everything
		}
		Connection con = null;
		try {
			con = Common.getConnection();
			return Spaces.getSubSpaceHierarchy(spaceId, userId, con);
		} catch (Exception e) {
			log.error("getSubSpaceHierarchy", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Recursively returns all of the spaces in the subspace hierarchy rooted at spaceId
	 *
	 * @param spaceId
	 * @return The list of spaces
	 */
	public static List<Space> getSubSpaceHierarchy(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSubSpaceHierarchyAdmin(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			return resultsToSpaces(results);
		} catch (Exception e) {
			log.debug("getSubSpaceHierarchy", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Helper method for getSubSpaces() - recursively finds all the subspaces of the given root space that the given
	 * user can see
	 *
	 * @param spaceId The id of the space to get the subspaces of
	 * @param userId The id of the user making the request for the subspaces
	 * @param con the database connection to use
	 * @return the list of subspaces of the given space
	 * @throws Exception
	 * @author Eric Burns
	 */
	private static List<Space> getSubSpaceHierarchy(int spaceId, int userId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetSubSpaceHierarchyById(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();
			return resultsToSpaces(results);
		} catch (Exception e) {
			log.error("getSubSpaces", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all subspaces belonging to another space. This is NOT recursive
	 *
	 * @param spaceId The id of the parent space. Give an id <= 0 to get the root space
	 * @param userId The id of the user requesting the subspaces. This is used to verify the user can see the space
	 * @return A list of child spaces belonging to the parent space that the given user can see
	 * @author Tyler Jensen, Todd Elvers & Skylar Stark
	 */
	public static List<Space> getSubSpaces(int spaceId, int userId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Spaces.getSubSpaces(spaceId, userId, con);
		} catch (Exception e) {
			log.error("getSubSpaces", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all subspaces of the given space
	 *
	 * @param spaceId The ID of the space to get subspaces for
	 * @return A list of subspaces, or null on error
	 */
	public static List<Space> getSubSpaces(int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getSubSpaces(spaceId, con);
		} catch (Exception e) {
			log.error("getSubSpaces", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all subspaces of the given space
	 *
	 * @param spaceId The ID of the space to get subspaces for
	 * @param con The open connection to make the call on
	 * @return A list of subspaces, or null on error
	 */
	public static List<Space> getSubSpaces(int spaceId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetSubSpacesAdmin(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			return resultsToSpaces(results);
		} catch (Exception e) {
			log.error("getSubSpaces", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Helper method for getSubSpaces() - gets either the first level of subspaces of a space or, recursively, all
	 * subspaces of a given space
	 *
	 * @param spaceId The id of the space to get the subspaces of
	 * @param userId The id of the user making the request for the subspaces
	 * @param con the database connection to use
	 * @return the list of subspaces of the given space
	 * @throws Exception
	 * @author Todd Elvers & Skylar Stark & Benton McCune & Wyatt Kaiser
	 */
	protected static List<Space> getSubSpaces(int spaceId, int userId, Connection con) {
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return getSubSpaces(spaceId, con);
		}
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetSubSpacesById(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			results = procedure.executeQuery();
			return resultsToSpaces(results);
		} catch (Exception e) {
			log.error("getSubSpaces", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all the subspaces of the given job space
	 *
	 * @param jobSpaceId The id of the space to get the subspaces of
	 * @param recursive Whether to get all subspaces (true) or only the first level (false)
	 * @return the list of subspaces of the given space used in the given job
	 * @author Eric Burns
	 */
	public static List<JobSpace> getSubSpacesForJob(int jobSpaceId, boolean recursive) {
		Connection con = null;
		HashSet<Integer> seenSpaces = new HashSet<>(); //will store everything we've already seen to prevent looping
		try {
			con = Common.getConnection();
			List<JobSpace> subspaces = Spaces.getSubSpacesForJob(jobSpaceId, con);
			if (!recursive) {
				return subspaces;
			} else {
				int index = 0;
				while (index < subspaces.size()) {
					int curSubspace = subspaces.get(index).getId();
					if (seenSpaces.contains(curSubspace)) {
						log.error("found a loop in the space hierarchy! Involved space ID =" + curSubspace);
						return null; // there was a loop in the space hierarchy-- this should not happen
					} else {
						seenSpaces.add(curSubspace);
					}
					subspaces.addAll(Spaces.getSubSpacesForJob(curSubspace, con));
					index++;
				}
				return subspaces;
			}
		} catch (Exception e) {
			log.error("getSubSpacesForJob", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all the subspaces of the given job space. Job spaces will be in alphabetically ascending order
	 *
	 * @param jobSpaceId The id of the jobspace to get the subspaces of
	 * @param con the open database connection to use
	 * @return the list of subspaces of the given space used in the given job
	 * @throws Exception
	 * @author Eric Burns
	 */
	protected static List<JobSpace> getSubSpacesForJob(int jobSpaceId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetJobSubSpaces(?)}");
			procedure.setInt(1, jobSpaceId);

			results = procedure.executeQuery();
			List<JobSpace> subSpaces = new LinkedList<>();

			while (results.next()) {
				JobSpace s = new JobSpace();
				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setMaxStages(results.getInt("max_stages"));
				s.setJobId(results.getInt("job_id"));
				subSpaces.add(s);
			}
			return subSpaces;
		} catch (Exception e) {
			log.error("getSubSpacesForJob", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * @param spaceId The id of the space to get users for
	 * @return A list of users belonging directly to the space
	 * @author Tyler Jensen
	 */
	public static List<User> getUsers(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpaceUsersById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			List<User> users = new LinkedList<>();

			while (results.next()) {
				User u = new User();
				u.setId(results.getInt("users.id"));
				u.setEmail(results.getString("users.email"));
				u.setFirstName(results.getString("users.first_name"));
				u.setLastName(results.getString("users.last_name"));
				u.setInstitution(results.getString("users.institution"));
				u.setCreateDate(results.getTimestamp("users.created"));
				u.setDiskQuota(results.getLong("users.disk_quota"));
				users.add(u);
			}
			return users;
		} catch (Exception e) {
			log.error("getUsers", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Determines whether or not a given space has any descendants (i.e. if it has any subspaces)
	 *
	 * @param spaceId the id of the space to check for descendants
	 * @return true iff the space is a leaf-space (i.e. if it has no descendants), false otherwise
	 * @author Todd Elvers
	 */
	public static boolean isLeaf(int spaceId) {
		final String method = "isLeaf";
		log.entry(method);
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetDescendantsOfSpace(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			log.debug(method, "Successfully called GetDescendantsOfSpace(" + spaceId + ")");
			return !results.next();
		} catch (Exception e) {
			log.error(method, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Determines whether this entire space hierarchy, including the given root space, is public
	 *
	 * @param spaceId The ID of the root space of the hierarchy
	 * @return True if the full hierarchy of spaces rooted at the given space is public, false otherwise
	 * @author Eric Burns
	 */
	public static boolean isPublicHierarchy(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsPublicHierarchy(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			if (results.first()) {
				return results.getBoolean("public");
			}
		} catch (Exception e) {
			log.error("isPublicHierarchy", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * If a space is public
	 *
	 * @param spaceId the Id of the space to be checked
	 * @return true if the space is public
	 * @author Ruoyu Zhang
	 */
	public static boolean isPublicSpace(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsPublicSpace(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			if (results.first()) {
				return (results.getInt(1) > 0);
			}
		} catch (Exception e) {
			log.error("isPublicSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Check if there already exists a primitive has the same name as given by prim in the space.
	 *
	 * @param prim The content of the primitive, such as a job's name
	 * @param space_id The if of the space needed to be checked
	 * @return True when there exists a primitive with the same name.
	 * @author Ruoyu Zhang
	 */
	public static boolean notUniquePrimitiveName(String prim, int space_id) {
		// Initiate sql connection facilities.
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			//If the type of the primitive is subspace.
			procedure = con.prepareCall("{CALL countSubspacesByName(?, ?)}");
			procedure.setString(1, prim);
			procedure.setInt(2, space_id);

			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt(1) != 0;
			}
		} catch (Exception e) {
			log.error("notUniquePrimitiveName", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return true;
	}

	/**
	 * Removes a list of benchmarks from a given space in an all-or-none fashion (creates a transaction)
	 *
	 * @param benchIds the id(s) of the benchmark(s) to remove from a given space
	 * @param spaceId the id of the space to remove the benchmark(s) from
	 * @return true iff all benchmarks in benchIds were successfully removed, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeBenches(List<Integer> benchIds, int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			// Instantiate a transaction so benchmarks in 'benchIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);

			// Remove benchmarks using the created transaction
			removeBenches(benchIds, spaceId, con);

			// Commit changes to database
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error("removeBenches", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error(benchIds.size() + " benchmark(s) were unsuccessfully removed from space " + spaceId);
		return false;
	}

	/**
	 * Removes a list of benchmarks from a given space in an all-or-none fashion (uses an existing transaction)
	 *
	 * @param benchIds the id's of the benchmarks to remove from a given space
	 * @param spaceId the space to remove the benchmarks from, if it's negative then the system will probe the database
	 * with the given list of benchmark ids and see if any aren't referenced anywhere in StarExec
	 * @param con the connection containing the existing transaction
	 * @throws SQLException if an error occurs while removing benchmarks from the database
	 * @author Todd Elvers
	 */
	protected static void removeBenches(List<Integer> benchIds, int spaceId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL RemoveBenchFromSpace(?, ?)}");

			for (int benchId : benchIds) {
				procedure.setInt(1, benchId);
				procedure.setInt(2, spaceId);
				procedure.executeUpdate();
			}
			log.info(benchIds.size() + " benchmark(s) were successfully removed from space " + spaceId);
		} catch (Exception e) {
			log.error("removeBenches", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Gets the space ids of spaces that have a given job associated with them.
	 *
	 * @param jobId Gets the space ids containing the given Job. (not JobSpaces)
	 * @return a list of space ids.
	 */
	public static Set<Integer> getByJob(int jobId) throws SQLException {
		return Common.query("{CALL GetSpacesByJob(?)}", procedure -> procedure.setInt(1, jobId), results -> {
			Set<Integer> spaceIds = new HashSet<>();
			while (results.next()) {
				spaceIds.add(results.getInt("space_id"));
			}
			return spaceIds;
		});
	}

	/**
	 * Removes a list of jobs from a given space in an all-or-none fashion (creates a transaction)
	 *
	 * @param jobIds the list of jobIds to remove from a given space
	 * @param spaceId the id of the space to remove the job from
	 * @return true iff all jobs in 'jobIds' were removed from the space referenced by 'spaceId', false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeJobs(List<Integer> jobIds, int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();

			// Instantiate a transaction so jobs in 'jobIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);
			removeJobs(jobIds, spaceId, con);
			Common.endTransaction(con);

			return true;
		} catch (Exception e) {
			log.error("removeJobs", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error(jobIds.size() + " job(s) were unsuccessfully removed from space" + spaceId);
		return false;
	}

	/**
	 * Removes a list of jobs from a given space in an all-or-none fashion (uses an existing transaction)
	 *
	 * @param jobIds the id's of the jobs to remove from a given space
	 * @param spaceId the space to remove the jobs from
	 * @param con the connection containing the existing transaction
	 * @throws SQLException if an error occurs while removing jobs from the database
	 * @author Todd Elvers
	 */
	protected static void removeJobs(List<Integer> jobIds, int spaceId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL RemoveJobFromSpace(?, ?)}");
			for (int jobId : jobIds) {
				procedure.setInt(1, jobId);
				procedure.setInt(2, spaceId);
				procedure.executeUpdate();
			}
			log.info(jobIds.size() + " job(s) were successfully removed from space " + spaceId);
		} catch (Exception e) {
			log.error("removeJobs", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Removes a list of solvers from a given space in an all-or-none fashion (creates a transaction)
	 *
	 * @param solverIds the list of solverIds to remove from a given space
	 * @param spaceId the id of the space to remove the solver from
	 * @return true iff all solvers in 'solverIds' were removed from the space referenced by spaceId, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeSolvers(List<Integer> solverIds, int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			// Instantiate a transaction so solvers in 'solverIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);
			Spaces.removeSolvers(solverIds, spaceId, con);
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error("removeSolvers", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error(solverIds.size() + " solver(s) were unsuccessfully removed from space " + spaceId);
		return false;
	}

	/**
	 * Removes a list of solvers from a given space in an all-or-none fashion (uses an existing transaction)
	 *
	 * @param solverIds the id's of the solvers to remove from a given space
	 * @param spaceId the space to remove the solvers from, if it's negative then the system will probe the database
	 * with the given list of solver ids and see if any aren't referenced anywhere in StarExec
	 * @param con the connection containing the existing transaction
	 * @throws SQLException if an error occurs while removing solvers from the database
	 * @throws IOException if an error occurs while removing solvers from disk
	 * @author Todd Elvers
	 */
	protected static void removeSolvers(List<Integer> solverIds, int spaceId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL RemoveSolverFromSpace(?, ?)}");
			for (int solverId : solverIds) {
				procedure.setInt(1, solverId);
				procedure.setInt(2, spaceId);
				procedure.executeUpdate();
			}
			log.info(solverIds.size() + " solver(s) were successfully removed from space " + spaceId);
		} catch (Exception e) {
			log.error("removeSolvers", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * For a given user, removes the given solver from every space in the hierarchy rooted at the given space that they
	 * can see.
	 *
	 * @param solverIds The IDs of the solvers to remove
	 * @param rootSpaceId The ID of the root space of the hierarchy
	 * @param userId
	 * @return True on success and false if there was an error
	 */
	public static boolean removeSolversFromHierarchy(List<Integer> solverIds, int rootSpaceId, int userId) {
		List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(rootSpaceId, userId));
		subspaces.add(Spaces.get(rootSpaceId));
		List<Space> allowedSpaces = new ArrayList<>();
		for (Space s : subspaces) {
			if (SolverSecurity.canUserRemoveSolver(s.getId(), userId).isSuccess()) {
				allowedSpaces.add(s);
			}
		}
		return removeSolversFromHierarchy(solverIds, allowedSpaces);
	}

	/**
	 * Removes solvers from a space and it's hierarchy. Utilizes transactions so it's all or nothing (all solvers from
	 * all spaces, or nothing)
	 *
	 * @param solverIds a list containing the id's of the solvers to remove
	 * @param subspaces a list containing the id's of the spaces to remove them from
	 * @return true iff all given solvers are removed from all given spaces
	 * @author Skylar Stark
	 */
	public static boolean removeSolversFromHierarchy(List<Integer> solverIds, List<Space> subspaces) {
		Connection con = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			for (Space space : subspaces) {
				Spaces.removeSolvers(solverIds, space.getId(), con);
			}
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error("removeSolversFromHierarchy", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Given a single space, removes all of the subspaces of that space from the database recursively
	 *
	 * @param spaceId the id of the space to remove the subspaces from
	 * @param con the database transaction to use
	 * @author Todd Elvers
	 */
	private static void removeSubspaces(int spaceId, Connection con) throws Exception {
		CallableStatement procedure = null;
		// For every subspace of the space to be deleted...
		for (Space subspace : Spaces.getSubSpaceHierarchy(spaceId)) {
			procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
			procedure.setInt(1, subspace.getId());
			procedure.executeUpdate();
			Common.safeClose(procedure);
			log.info("Space " + subspace.getId() + " has been deleted.");
		}
	}

	/**
	 * Removes a single space from the database
	 *
	 * @param subspaceId The space to remove
	 * @return True on success and false otherwise
	 */
	public static boolean removeSubspace(int subspaceId) {
		List<Integer> spaceId = new ArrayList<>();
		spaceId.add(subspaceId);
		return removeSubspaces(spaceId);
	}

	/**
	 * Removes a list of subspaces, and all of their subspaces, from a given space in an all-or-none fashion (creates a
	 * transaction)
	 *
	 * @param subspaceIds the list of subspaces to remove
	 * @return true iff all subspaces in 'subspaceIds' are removed from the space referenced by 'spaceId', false
	 * otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeSubspaces(List<Integer> subspaceIds) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			// Instantiate a transaction so subspaces in 'subspaceIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);

			// For each subspace in the list of subspaces to be deleted...
			for (int subspaceId : subspaceIds) {
				log.debug("subspaceId = " + subspaceId);

				// Check if it has any subspaces itself, and if so delete them
				Spaces.removeSubspaces(subspaceId, con);

				procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
				procedure.setInt(1, subspaceId);
				procedure.executeUpdate();
				log.info("Space " + subspaceId + " has been deleted.");
			}

			// Commit changes to database
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error("removeSubspaces", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Removes users from the space with the given spaceId
	 *
	 * @param con the connection to perform the transaction on
	 * @param userIds a list containing the id's of the users to remove
	 * @param spaceId the id of the space to remove the users from
	 * @author Skylar Stark
	 */
	private static void removeUsers(Connection con, List<Integer> userIds, int spaceId) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL LeaveSpace(?, ?)}");
			for (int userId : userIds) {
				procedure.setInt(1, userId);
				procedure.setInt(2, spaceId);
				procedure.executeUpdate();
			}
		} catch (Exception e) {
			log.error("removeUsers", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Removes a list of users from a community in an all-or-none fashion (creates a transaction)
	 *
	 * @param userIds the list of userIds to be removed from a given community
	 * @param commId the id of the community to remove the users from
	 * @return true iff all users in userIds were removed from the community referenced by commId, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeUsers(List<Integer> userIds, int commId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			// Instantiate a transaction so users in 'userIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);

			removeUsers(con, userIds, commId);

			// Commit changes to database
			Common.endTransaction(con);

			log.info(userIds.size() + " user(s) were successfully removed from community " + commId);
			return true;
		} catch (Exception e) {
			log.error("removeUsers", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}

		log.error(userIds.size() + " user(s) were unsuccessfully removed from community " + commId);
		return false;
	}

	/**
	 * Removes users from a space and it's hierarchy. Utilizes transactions so it's all or nothing (all users from all
	 * spaces, or nothing)
	 *
	 * @param userIds a list containing the id's of the users to remove
	 * @param subspaceIds a list containing the id's of the spaces to remove them from
	 * @return true iff all given users are removed from all given spaces
	 * @author Skylar Stark
	 */
	public static boolean removeUsersFromHierarchy(List<Integer> userIds, List<Integer> subspaceIds) {
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			for (int spaceId : subspaceIds) {
				Spaces.removeUsers(con, userIds, spaceId);
			}

			Common.endTransaction(con);

			return true;
		} catch (Exception e) {
			log.error("removeUsersFromHierarchy", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Makes a single space public.
	 *
	 * @param spaceId the space to make public.
	 * @param userId the user making the request.
	 */
	public static void makeSingleSpacePublic(int spaceId, int userId) {
		setPublicSpace(spaceId, userId, true, false);
	}

	/**
	 * Makes a space hierarchy public.
	 *
	 * @param rootSpaceId the root of the hierarchy that is to be made public
	 * @param userId the user making the request.
	 * @return true on success, otherwise false.
	 */
	public static boolean makeHierarchyPublic(int rootSpaceId, int userId) {
		return setPublicSpace(rootSpaceId, userId, true, true);
	}

	/**
	 * Makes a single space private.
	 *
	 * @param spaceId the space to make private.
	 * @param userId the user making the request.
	 * @return true on success, otherwise false.
	 */
	public static boolean makeSingleSpacePrivate(int spaceId, int userId) {
		return setPublicSpace(spaceId, userId, false, false);
	}

	/**
	 * Makes a space hierarchy private.
	 *
	 * @param rootSpaceId the root of the hierarchy that is to be made private
	 * @param userId the user making the request.
	 * @return true on success, otherwise false.
	 */
	public static boolean makeHierarchyPrivate(int rootSpaceId, int userId) {
		return setPublicSpace(rootSpaceId, userId, false, true);
	}

	/**
	 * Set a space to be public or private
	 *
	 * @param spaceId the Id of space to be set public or private
	 * @param usrId The ID of the user making the request
	 * @param pbc true denote the space to be set public, false to be set private
	 * @param hierarchy True to set the entire hierarchy public / private and false to do only this space
	 * @return true if successful
	 * @author Ruoyu Zhang
	 */
	public static boolean setPublicSpace(int spaceId, int usrId, boolean pbc, boolean hierarchy) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL setPublicSpace(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setBoolean(2, pbc);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error("setPublicSpace", e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		if (hierarchy) {//is hierarchy, call recursively
			boolean success = true;
			List<Space> subSpaces = Spaces.getSubSpaceHierarchy(spaceId, usrId);
			for (Space space : subSpaces) {
				try {
					success = success && setPublicSpace(space.getId(), usrId, pbc, false);
				} catch (Exception e) {
					log.error("setPublicSpace", e);
				}
			}
			return success;
		} else {
			return true;
		}
	}

	/**
	 * Given a list of spaces, generates a HashMap mapping spaceIds to the path for each space, rooted at the given
	 * rootSpaceId.
	 *
	 * @param userId The ID of the user making the request
	 * @param spaces The list of spaces, all of which must be subspaces of the rootSpaceId
	 * @param rootSpaceId The ID of the space to root the paths at
	 * @return A mapping from from space ids to the string paths for those spaces
	 */
	public static HashMap<Integer, String> spacePathCreate(int userId, List<Space> spaces, int rootSpaceId) {

		Space space = Spaces.get(rootSpaceId);
		HashMap<Integer, String> paths = new HashMap<>();
		paths.put(space.getId(), space.getName());
		for (Space s : spaces) {
			int parentId = Spaces.getParentSpace(s.getId());
			if (paths.containsKey(parentId)) {
				paths.put(s.getId(), paths.get(parentId) + R.JOB_PAIR_PATH_DELIMITER + s.getName());
				log.debug("added the following space to the space paths =" + +s.getId());
			} else {
				//we'll keep searching until we get to something in the paths
				Stack<String> names = new Stack<>();
				names.push(s.getName());
				names.push(Spaces.getName(parentId));
				while (parentId >= 1) {
					parentId = Spaces.getParentSpace(parentId);
					if (paths.containsKey(parentId)) {
						StringBuilder path = new StringBuilder();
						path.append(paths.get(parentId));
						while (!names.isEmpty()) {
							path.append(R.JOB_PAIR_PATH_DELIMITER);
							path.append(names.pop());
						}
						paths.put(s.getId(), path.toString());
						log.debug("added the following space to the space paths =" + +s.getId());
					} else {
						names.push(Spaces.getName(parentId));
					}
				}
			}
		}
		return paths;
	}

	/**
	 * Internal recursive method that adds a space and it's benchmarks to the database
	 *
	 * @param space The space to add to the database
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie. If no dependencies exist, this is
	 * ignored.
	 * @param userId The user id of the owner of the new space and its benchmarks
	 * @author Benton McCune
	 */
	protected static void traverse(
			Space space, int userId, Integer depRootSpaceId, Boolean linked, Integer statusId
	) throws IOException, StarExecException {
		// Add the new space to the database and get it's ID
		int spaceId = Spaces.add(space, userId);

		log.info("traversing (with deps) space " + space.getName());
		for (Space sub : space.getSubspaces()) {
			sub.setParentSpace(spaceId);
			// Recursively go through and add all of it's subspaces with itself as the parent
			Spaces.traverse(sub, userId, depRootSpaceId, linked, statusId);
		}
		// Finally, add the benchmarks in the space to the database
		Benchmarks.processAndAdd(space.getBenchmarks(), spaceId, depRootSpaceId, linked, statusId, true);

		/* `incrementCompletedSpaces` will use its own connection so it is not
		 * part of this transaction This is to ensure the count is updated
		 * immediately, instead of only once after everything is finished.    */
		Uploads.incrementCompletedSpaces(statusId, 1);
	}

	/**
	 * Given a list of spaces a user id, removes from the list of spaces any space where the given user is not a member
	 * of.
	 *
	 * @param userId the id of the user to check membership of
	 * @param spaces the list of spaces to check membership of
	 * @return the original list without the spaces the user is not a member of
	 * @author Wyatt Kaiser
	 */
	public static List<Space> trimSubSpaces(int userId, List<Space> spaces) {
		if (GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return spaces;
		}
		Iterator<Space> iter = spaces.iterator();
		while (iter.hasNext()) {
			if (!Users.isMemberOfSpace(userId, iter.next().getId())) {
				iter.remove();
				log.debug("removed space");
			}
		}
		return spaces;
	}

	/**
	 * Update t he description of a space
	 *
	 * @param spaceId the id of the space to update
	 * @param newDesc the new description to update the space with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateDescription(int spaceId, String newDesc) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateSpaceDescription(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, newDesc);
			procedure.executeUpdate();
			log.info(String.format("Space [%d] updated description to [%s]", spaceId, newDesc));
			return true;
		} catch (Exception e) {
			log.error("updateDescription", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates the details of a space in the database. The given Space object should contain the space id of the space
	 * we are updating, as well as all the other necessary information.
	 *
	 * @param userId the user id of the user making the request (to check permission)
	 * @param s the space object containing the information we are updating to
	 * @return true iff the update is successful
	 * @author Skylar Stark
	 */
	public static boolean updateDetails(int userId, Space s) {
		Connection con = null;
		boolean success = false;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			success = Spaces.updateDetails(s, con);
			Common.endTransaction(con);
			log.info(String.format("Space with name [%s] successfully edited by user [%d].", s.getName(), userId));
			return success;
		} catch (Exception e) {
			log.error("updateDetails", e);
		} finally {
			Common.doRollback(con);
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Updates the details of a space in the database. The given Space object should contain the space id of the space
	 * we are updating, as well as all the other necessary information. This is a multi-step process, so we use
	 * transactions.
	 *
	 * @param s the space object containing the information we are updating to
	 * @return true iff the space update is successful
	 * @author Skylar Stark
	 */
	protected static boolean updateDetails(Space s, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL UpdateSpaceDetails(?,?,?,?,?,?)}");
			procedure.setInt(1, s.getId());
			procedure.setString(2, s.getName());
			procedure.setString(3, s.getDescription());
			procedure.setBoolean(4, s.isLocked());
			procedure.setBoolean(5, s.isStickyLeaders());
			procedure.registerOutParameter(6, java.sql.Types.INTEGER);
			procedure.executeUpdate();

			// Get the id of the associated default permission, then update that permission
			int permId = procedure.getInt(6);
			Permissions.updatePermission(permId, s.getPermission(), con);
			return true;
		} catch (Exception e) {
			log.error("updateDetails", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates the name of a space with the given space id
	 *
	 * @param spaceId the id of the space to update
	 * @param newName the new name to update the space with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateName(int spaceId, String newName) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateSpaceName(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, newName);
			procedure.executeUpdate();
			log.info(String.format("Space [%d] updated name to [%s]", spaceId, newName));
			return true;
		} catch (Exception e) {
			log.error("updateName", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Return all the communities that are not already associated with a queue
	 *
	 * @param queue_id The ID of the queue to get communities for
	 * @return A list of communities not associated with the given queue
	 */
	public static List<Space> getNonAttachedCommunities(int queue_id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetNonAttachedCommunities(?)}");
			procedure.setInt(1, queue_id);
			results = procedure.executeQuery();

			List<Space> spaces = new LinkedList<>();
			while (results.next()) {
				Space s = new Space();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));
				spaces.add(s);
			}
			return spaces;
		} catch (Exception e) {
			log.error("getNonAttachedCommunities", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * For a given space, gets the IDs of every person that should be a leader in that space due to being sticky
	 * leaders
	 * in some ancestor space
	 *
	 * @param spaceId The ID of the space to get all the sticky leaders for
	 * @return A set of integers containing the IDs of all the relevant users
	 */
	public static Set<Integer> getStickyLeaders(int spaceId) {
		//root space has no sticky leaders
		if (spaceId == 1) {
			return new HashSet<>();
		}
		return recGetStickyLeaders(Spaces.getParentSpace(spaceId));
	}

	/**
	 * Helper method for getStickyLeaders that recursively moves up the space tree to look for users
	 *
	 * @param spaceId
	 * @return
	 */
	private static Set<Integer> recGetStickyLeaders(int spaceId) {
		HashSet<Integer> ids = new HashSet<>();
		//communities are not allowed to have the sticky leaders feature enabled, so if we've reached
		//a community or the root, we can quit
		if (Communities.isCommunity(spaceId) || spaceId == 1) {
			return ids;
		}
		Space s = Spaces.get(spaceId);
		if (s.isStickyLeaders()) {
			List<User> leaders = Spaces.getLeaders(spaceId);
			for (User u : leaders) {
				ids.add(u.getId());
			}
		}
		ids.addAll(recGetStickyLeaders(Spaces.getParentSpace(spaceId)));
		return ids;
	}

	/**
	 * Determines whether the given space ID is the ID of the root space
	 *
	 * @param spaceId
	 * @return True if the given space is the root and false otherwise
	 */
	public static boolean isRoot(int spaceId) {
		return 1 == spaceId;
	}

	/**
	 * Associates every orphaned solver, benchmark, and job a user owns with the given space
	 *
	 * @param userId The ID of the user who owns the orphaned primitives
	 * @param spaceId The ID of the space in question
	 * @return True on success, false otherwise
	 */
	public static boolean addOrphanedPrimitivesToSpace(int userId, int spaceId) {
		try {
			boolean success = true;
			success = success && Benchmarks.associate(Benchmarks.getOrphanedBenchmarks(userId), spaceId);
			success = success && Solvers.associate(Solvers.getOrphanedSolvers(userId), spaceId);
			success = success && Jobs.associate(Jobs.getOrphanedJobs(userId), spaceId);
			return success;
		} catch (Exception e) {
			log.error("addOrphanedPrimitivesToSpace", e);
		}
		return false;
	}

	/**
	 * Detects if a valid solver / config pair exists in the space hierarchy. Used in error messages in the CreateJob
	 * servelet
	 *
	 * @param usrId The ID of the user who owns the orphaned primitives
	 * @param spaceId The ID of the space in question
	 * @return True if a valid pair exists in the hierarchy, false otherwise.
	 */
	public static boolean configBenchPairExistsInHierarchy(int spaceId, int usrId) {
		Space space = Spaces.getDetails(spaceId, usrId);
		if (space.getSolvers().isEmpty() || space.getBenchmarks().isEmpty()) {
			List<Space> subspaces = space.getSubspaces();
			for (Space subspace : subspaces) {
				if (configBenchPairExistsInHierarchy(subspace.getId(), usrId)) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
}
