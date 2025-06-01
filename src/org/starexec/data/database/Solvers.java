package org.starexec.data.database;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.to.*;
import org.starexec.data.to.Solver.ExecutableType;
import org.starexec.data.to.compare.SolverComparator;
import org.starexec.logger.StarLogger;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;
import org.starexec.util.Util;

import java.io.File;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles all database interaction for solvers
 */
public class Solvers {
	private static final StarLogger log = StarLogger.getLogger(Solvers.class);
	private static final String CONFIG_PREFIX = R.CONFIGURATION_PREFIX;
	private static final DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);

	/**
	 * Adds a solver to the database. Solver association to a space and run configurations for the solver are also
	 * added. Every insert must pass for any of them to be added.
	 *
	 * @param s the Solver to be added
	 * @param spaceId the ID of the space we associate this solver with
	 * @return The solverID of the new solver, or -1 on failure
	 * @author Skylar Stark
	 */
	public static int add(Solver s, int spaceId) throws SQLException {
		final String methodName = "add";
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			long diskUsage = FileUtils.sizeOf(new File(s.getPath()));
			s.setDiskSize(diskUsage);
			// Add the solver
			procedure = con.prepareCall("{CALL AddSolver(?, ?, ?, ?, ?, ?, ?, ?,?)}");
			procedure.setInt(1, s.getUserId());
			procedure.setString(2, s.getName());
			procedure.setBoolean(3, s.isDownloadable());
			procedure.setString(4, s.getPath());
			procedure.setString(5, s.getDescription());
			procedure.registerOutParameter(6, java.sql.Types.INTEGER);
			procedure.setLong(7, diskUsage);
			procedure.setInt(8, s.getType().getVal());
			procedure.setInt(9, s.buildStatus().getCode().getVal());

			procedure.executeUpdate();

			// Associate the solver with the given space
			int solverId = procedure.getInt(6);
			Solvers.associate(con, spaceId, solverId);

			// Add solver configurations
			for (Configuration c : s.getConfigurations()) {
				c.setSolverId(solverId);
				addConfiguration(con, c);
			}

			return solverId;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds a run configuration to the database
	 *
	 * @param con the database connection associated with the whole process of adding the solver
	 * @param c the configuration we are adding
	 * @return The ID of the new configuration, or -1 on error. The ID will also be set in the configuration object
	 * @author Skylar Stark
	 */
	protected static int addConfiguration(Connection con, Configuration c) throws SQLException {
		final String methodName = "addConfiguration";
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddConfiguration(?, ?, ?, ?, ?)}");
			procedure.setInt(1, c.getSolverId());
			procedure.setString(2, c.getName());
			procedure.setString(3, c.getDescription());
			procedure.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			procedure.registerOutParameter(5, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			c.setId(procedure.getInt(5));
			return c.getId();
		} catch (SQLException e) {
			log.error(methodName, e.getMessage(), e);
			throw e;
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds a configuration entry in the database for a particular solver and updates that solver's disk size to
	 * reflect
	 * the new file
	 *
	 * @param s the solver to add the configuration to
	 * @param c the configuration to add to said solver
	 * @return either a value greater than or equal to 0, reflecting the newly added configuration's id in the
	 * database,
	 * or a -1 indicating an error occurred
	 * @author Todd Elvers
	 */
	public static int addConfiguration(Solver s, Configuration c) {
		final String methodName = "addConfiguration";
		Connection con = null;
		try {
			con = Common.getConnection();
			c.setSolverId(s.getId());

			int newConfigId = addConfiguration(con, c);

			// Update the disk size of the parent solver to include the new configuration file's size
			Solvers.updateSolverDiskSize(con, s);

			return newConfigId;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return -1;
	}

	/**
	 * Adds a Space/Solver association
	 *
	 * @param con the database connection associated with the whole process of adding the solver
	 * @param spaceId the ID of the space we are making the association to
	 * @param solverId the ID of the solver we are associating to the space
	 * @author Skylar Stark
	 */
	protected static void associate(Connection con, int spaceId, int solverId) throws SQLException {
		final String methodName = "associate";
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddSolverAssociation(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, solverId);

			procedure.executeUpdate();
		} catch (SQLException e) {
			log.error(methodName, e.getMessage(), e);
			throw e;
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds an association between a list of solvers and a space
	 *
	 * @param con the database transaction to use
	 * @param solverIds the ids of the solvers to add to a space
	 * @param spaceId the id of the space to add the solvers to
	 * @throws Exception
	 * @author Todd Elvers
	 */
	protected static void associate(Connection con, List<Integer> solverIds, int spaceId) throws Exception {
		final String methodName = "associate";
		for (int sid : solverIds) {
			Solvers.associate(con, spaceId, sid);
		}
	}

	/**
	 * Adds an association between the given solver id and the given space
	 *
	 * @param solverId the id of the solver we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean associate(int solverId, int spaceId) {
		final String methodName = "associate";
		List<Integer> solverIds = new ArrayList<>();
		solverIds.add(solverId);
		return associate(solverIds, spaceId);
	}

	/**
	 * Adds an association between all the given solver ids and the given space
	 *
	 * @param solverIds the ids of the solvers we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> solverIds, int spaceId) {
		final String methodName = "associate";
		Connection con = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			for (int sid : solverIds) {
				Solvers.associate(con, spaceId, sid);
			}

			log.info("Successfully added solvers " + solverIds.toString() + " to space [" + spaceId + "]");

			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			final String message = "Failed to add solvers " + solverIds.toString() + " to space [" + spaceId + "]";
			log.error(methodName, message, e);
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Associates a set of solvers with a given space or space hierarchy
	 *
	 * @param solverIds
	 * @param rootSpaceId
	 * @param linkInSubspaces Whether to link solvers recursively or not
	 * @param userId ID of user making the request
	 * @param includeRoot If linking recursively, whether to include the space given by rootSpaceId
	 * @return True on success and false otherwise
	 */
	public static boolean associate(
			List<Integer> solverIds, int rootSpaceId, boolean linkInSubspaces, int userId, boolean includeRoot
	) {
		final String methodName = "associate";
		// Either copy the solvers to the destination space or the destination space and all of its subspaces (that
		// the user can see)
		if (linkInSubspaces) {
			log.trace("got a request to link in subspaces");
			List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(rootSpaceId, userId));
			log.debug("found a total subspaces = " + subspaces.size());
			List<Integer> subspaceIds = new LinkedList<>();

			// Add the destination space to the list of spaces to associate the solvers with only
			//if we aren't copying. If we're copying, we did this already
			if (includeRoot) {
				subspaceIds.add(rootSpaceId);
			}

			// Iterate once through all subspaces of the destination space to ensure the user has addSolver
			// permissions in each
			for (Space subspace : subspaces) {
				subspaceIds.add(subspace.getId());
			}

			// Add the solvers to the destination space and its subspaces
			return Solvers.associate(solverIds, subspaceIds);
		} else {
			// Add the solvers to the destination space
			return Solvers.associate(solverIds, rootSpaceId);
		}
	}

	/**
	 * Adds an association between a list of solvers and a list of spaces, in an all-or-none fashion
	 *
	 * @param solverIds the ids of the solvers to add to the spaces
	 * @param spaceIds the ids of the spaces to add the solvers to
	 * @return true iff all spaces in spaceIds successfully have all solvers in solverIds add to them, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean associate(List<Integer> solverIds, List<Integer> spaceIds) {
		final String methodName = "associate";
		Connection con = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			// For each space id in spaceIds, add all the solvers to it
			for (int spaceId : spaceIds) {
				Solvers.associate(con, solverIds, spaceId);
			}

			log.info("Successfully added solvers " + solverIds.toString() + " to spaces " + spaceIds.toString());
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			String message = "Failed to add solvers " + solverIds.toString() + " to spaces " + spaceIds.toString();
			log.error(methodName, message, e);
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Permanently removes a solver from the database
	 *
	 * @param solverId The ID of the solver to remove
	 * @param con The open connection to make the SQL call on
	 * @return True on success and false otherwise
	 */
	private static boolean removeSolverFromDatabase(int solverId, Connection con) {
		final String methodName = "removeSolverFromDatabase";
		log.trace("got request permanently remove this solver from the database " + solverId);
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("CALL RemoveSolverFromDatabase(?)");
			procedure.setInt(1, solverId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Removes all solver database entries where the solver has been deleted AND has been orphaned
	 *
	 * @return True on success, false on error
	 */
	public static boolean cleanOrphanedDeletedSolvers() {
		final String methodName = "cleanOrphanedDeletedSolvers";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		//will contain the id of every solver that is associated with either a space or a pair
		HashSet<Integer> parentedSolvers = new HashSet<>();
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolversAssociatedWithSpaces()}");
			results = procedure.executeQuery();
			while (results.next()) {
				parentedSolvers.add(results.getInt("id"));
			}
			Common.safeClose(procedure);
			Common.safeClose(results);

			procedure = con.prepareCall("{CALL GetSolversAssociatedWithPairs()}");
			results = procedure.executeQuery();
			while (results.next()) {
				parentedSolvers.add(results.getInt("id"));
			}

			Common.safeClose(procedure);
			Common.safeClose(results);

			procedure = con.prepareCall("CALL GetDeletedSolvers()");
			results = procedure.executeQuery();
			while (results.next()) {
				Solver s = resultSetToSolver(results);
				if (new File(s.getPath()).exists()) {
					log.warn("a deleted solver still has an on-disk directory! ID = " + s.getId());
					if (!FileUtils.deleteQuietly(new File(s.getPath()))) {
						log.warn("failed to delete solver on disk! Not removing solver from database.");
						continue;
					}
				}
				// the solver has been deleted AND it is not associated with any spaces or job pairs
				if (!parentedSolvers.contains(s.getId())) {
					removeSolverFromDatabase(s.getId(), con);
				}
			}
			return true;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Copies a list of solvers into the given space, assigning the given user as the owner of each one
	 *
	 * @param solvers A list of solves
	 * @param userId The ID of the user who will own the new solvers
	 * @param spaceId The ID of the space to put the solvers in
	 * @return The IDs of the new solvers, in the same order that the solvers are in. Negative numbers indicate errors
	 * associated with the corresponding solvers
	 */
	public static List<Integer> copySolvers(List<Solver> solvers, int userId, int spaceId) {
		List<Integer> ids = new ArrayList<>();
		for (Solver s : solvers) {
			ids.add(copySolver(s, userId, spaceId));
		}
		return ids;
	}

	/**
	 * Makes a deep copy of an existing solver, gives it a new user, and places it into a space
	 *
	 * @param s The existing solver to copy
	 * @param userId The userID that the new solver will be given
	 * @param spaceId The space ID of the space to place the new solver in to
	 * @return The ID of the new solver, or -1 on failure
	 * @author Eric Burns
	 */
	public static int copySolver(Solver s, int userId, int spaceId) {
		final String methodName = "copySolver";
		log.debug("Copying solver " + s.getName() + " to new user id= " + String.valueOf(userId));
		Solver newSolver = new Solver();
		newSolver.setDescription(s.getDescription());
		newSolver.setName(s.getName());
		newSolver.setUserId(userId);
		newSolver.setUploadDate(s.getUploadDate());
		newSolver.setDiskSize(s.getDiskSize());
		newSolver.setDownloadable(s.isDownloadable());
		newSolver.setType(s.getType());
		newSolver.setBuildStatus(s.buildStatus());
		File solverDirectory = new File(s.getPath());
		File uniqueDir = new File(R.getSolverPath(), "" + userId);
		uniqueDir = new File(uniqueDir, newSolver.getName());
		String date = shortDate.format(new Date());
		uniqueDir = new File(uniqueDir, "" + date);
		uniqueDir.mkdirs();
		newSolver.setPath(uniqueDir.getAbsolutePath());
		try {
			//we need to check if the solver has a src dir. If it does, we also need to 
			//copy it. This fixes issue 307
			File maybeSrc = new File(s.getPath()+ "_src");
			boolean hasSrc = maybeSrc.exists();
			if (hasSrc) {
				File originalSrcDir = maybeSrc;
				File newSrcDir = new File(uniqueDir.getAbsolutePath() + "_src");
				newSrcDir.mkdirs();
				FileUtils.copyDirectory(originalSrcDir, newSrcDir);
			}
			
			FileUtils.copyDirectory(solverDirectory, uniqueDir);
			for (Configuration c : findConfigs(uniqueDir.getAbsolutePath())) {
				newSolver.addConfiguration(c);
			}
			return Solvers.add(newSolver, spaceId);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return -1;
		}
	}

	/**
	 * Deletes a solver and permanently removes it from the database. This is NOT the normal procedure for deleting a
	 * solver. It is used for testing. Calling "delete" is typically what is desired.
	 *
	 * @param id
	 * @return True on success and false otherwise.
	 */
	public static boolean deleteAndRemoveSolver(int id) {
		final String methodName = "deleteAndRemoveSolver";
		Solver s = Solvers.getIncludeDeleted(id);
		if (s == null) {
			return true;
		}
		boolean success = true;
		if (!s.isDeleted()) {
			success = delete(id);
		}
		if (!success) {
			return false;
		}
		Connection con = null;
		try {
			con = Common.getConnection();
			return Solvers.removeSolverFromDatabase(id, con);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Sets the deleted flag of a solver and removes it from disk (cascading deletes handle all dependencies)
	 *
	 * @param id the id of the solver to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int id) {
		final String methodName = "delete";
		log.trace("Solvers.delete() called on solver with id = " + id);
		Connection con = null;
		CallableStatement procedure = null;
		try {
			File buildOutput = Solvers.getSolverBuildOutput(id);
			if (buildOutput.exists()) {
				Util.safeDeleteDirectory(buildOutput.getParent());
			}
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetSolverToDeletedById(?, ?)}");
			procedure.setInt(1, id);
			procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();

			String sourcePath = procedure.getString(2) + "_src";
			log.trace("Deleting solver source from disk, path: " + sourcePath);
			Util.safeDeleteDirectory(sourcePath);
			File srcFile = new File(sourcePath);
			if (srcFile.getParentFile().exists()) {
				srcFile.getParentFile().delete();
			}

			// Delete solver file from disk, and the parent directory if it's empty
			Util.safeDeleteDirectory(procedure.getString(2));
			File file = new File(procedure.getString(2));
			if (file.getParentFile().exists()) {
				file.getParentFile().delete();
			}

			return true;
		} catch (Exception e) {
			String message = String.format("Deletion of solver [id=%d] failed.", id);
			log.error(methodName, message, e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Deletes a configuration from the database given that configuration's id
	 *
	 * @param configId the id of the configuration to remove from the database
	 * @return true iff the configuration is successfully deleted from the database, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean deleteConfiguration(int configId) {
		final String methodName = "deleteConfiguration";
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DeleteConfigurationById(?)}");
			procedure.setInt(1, configId);
			procedure.executeUpdate();

			log.info(String.format("Configuration %d has been successfully deleted from the database.", configId));

			return true;
		} catch (Exception e) {
			String message = String.format("Configuration %d has failed to be deleted from the database.", configId);
			log.error(methodName, message, e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Deletes a given configuration object's physical file from disk, then deletes the configuration in the database
	 * and updates the solver disk size in the database
	 *
	 * @param config the configuration whose physical file is to be deleted from disk
	 * @return true iff the configuration object's corresponding physical file is successfully deleted from disk, false
	 * otherwise
	 * @author Todd Elvers
	 */
	public static boolean deleteConfigurationFile(Configuration config) {
		final String methodName = "deleteConfigurationFile";
		try {
			Solver s = Solvers.getSolverByConfig(config.getId(), false);
			// Builds the path to the configuration object's physical file on disk, then deletes it from disk
			File configFile = new File(Util.getSolverConfigPath(s.getPath(), config.getName()));

			if (configFile.delete()) {
				log.info(String.format("Configuration %d has been successfully deleted from disk.", config.getId()));
			}

			// Attempt to remove the configuration's entry in the database
			if (!Solvers.deleteConfiguration(config.getId())) {
				return false;
			}

			// Attempt to update the disk_size of the parent solver to reflect the file deletion
			return Solvers.updateSolverDiskSize(s);
		} catch (Exception e) {
			String message = String.format("Configuration %d has failed to be deleted from disk.", config.getId());
			log.error(methodName, message, e);
			return false;
		}
	}

	/**
	 * Finds solver run configurations from a specified bin directory. Run configurations must start with a certain
	 * string specified in the list of constants. If no configurations are found, an empty list is returned.
	 *
	 * @param fromPath the base directory to find the bin directory in
	 * @return a list containing run configurations found in the bin directory
	 */
	public static List<Configuration> findConfigs(String fromPath) {
		File binDir = new File(fromPath, R.SOLVER_BIN_DIR);
		if (!binDir.exists()) {
			return Collections.emptyList();
		}

		List<Configuration> returnList = new ArrayList<>();

		for (File f : binDir.listFiles()) {
			if (f.isFile() && f.getName().startsWith(CONFIG_PREFIX)) {

				Configuration c = new Configuration();
				c.setName(f.getName().substring(CONFIG_PREFIX.length()));
				returnList.add(c);

				// Make sure the configuration has the right line endings
				Util.normalizeFile(f);
			}
			//f.setExecutable(true, false);	//previous version only got top level
		}
		setHierarchyExecutable(binDir);//should make entire hierarchy executable
		return returnList;
	}

	/**
	 * @param con The connection to make the query on
	 * @param solverId The id of the solver to retrieve
	 * @param includeDeleted If true, also return any solvers marked as 'deleted'. Ignore such solvers otherwise
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	public static Solver get(Connection con, int solverId, boolean includeDeleted) {
		final String methodName = "get";
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			if (includeDeleted) {
				procedure = con.prepareCall("{CALL GetSolverByIdIncludeDeleted(?)}");
			} else {
				procedure = con.prepareCall("{CALL GetSolverById(?)}");
			}
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			if (results.next()) {
				Solver s = resultSetToSolver(results, null);
				Common.safeClose(results);
				return s;
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * @param solverId
	 * @return The solver specified by the given ID. Null if the solver could not be found or has been deleted
	 */
	public static Solver get(int solverId) {
		return get(solverId, false);
	}

	/**
	 * @param solverId The id of the solver to retrieve
	 * @param includeDeleted True to include solvers with a true 'deleted' flag in the DB and false to exclude those
	 * solvers
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	public static Solver get(int solverId, boolean includeDeleted) {
		final String methodName = "get";
		Connection con = null;

		try {
			con = Common.getConnection();
			return Solvers.get(con, solverId, includeDeleted);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * @param solverIds The ids of the solvers to retrieve
	 * @return A list of solver objects representing the solvers with the given IDs
	 * @author Tyler Jensen
	 */
	public static List<Solver> get(List<Integer> solverIds) {
		final String methodName = "get";
		Connection con = null;

		try {
			con = Common.getConnection();
			List<Solver> solvers = new LinkedList<>();

			for (int id : solverIds) {
				solvers.add(Solvers.get(con, id, false));
			}

			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Gets the IDs of every space that is associated with the given solver
	 *
	 * @param solverId The solver in question
	 * @return A list of space IDs that are associated with this solver
	 * @author Eric Burns
	 */
	public static List<Integer> getAssociatedSpaceIds(int solverId) {
		final String methodName = "getAssociatedSpaceIds";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAssociatedSpaceIdsBySolver(?)}");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<>();
			while (results.next()) {
				ids.add(results.getInt("space_id"));
			}
			return ids;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Returns a solver that is associated with a given configuration.
	 *
	 * @param configId The id of the configuration of the solver we want to return
	 * @return a solver with the configuration with the given configuration id
	 * @author Skylar Stark
	 */
	public static Solver getByConfigId(int configId) {
		final String methodName = "getByConfigId";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolverIdByConfigId(?)}");
			procedure.setInt(1, configId);
			results = procedure.executeQuery();

			if (results.next()) {
				int sid = results.getInt("id");
				Solver s = Solvers.get(sid);
				Configuration c = Solvers.getConfiguration(configId);
				// Make sure this configuration actually belongs to the solver, and add/return it if it does
				if (sid == c.getSolverId()) {
					s.addConfiguration(c);
					return s;
				}
			}

			return null; //The solver/config pair was invalid.
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Gets every solver that shares a space with the given user
	 *
	 * @param userId
	 * @return The list of solvers, or null on error
	 */
	public static List<Solver> getSolversInSharedSpaces(int userId) {
		final String methodName = "getSolversInSharedSpaces";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolversInSharedSpaces(?)}");
			procedure.setInt(1, userId);

			results = procedure.executeQuery();
			List<Solver> solvers = new ArrayList<>();
			while (results.next()) {
				solvers.add(resultSetToSolver(results));
			}
			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Retrieves a list of every solver the given user is allowed to use along with it's configs. Solvers a user can
	 * see
	 * include solvers they own, solvers in public spaces, and solvers in spaces the user is also in
	 *
	 * @param userId
	 * @return The list of solvers
	 */
	public static List<Solver> getByUserWithConfigs(int userId) {
		final String methodName = "getByUserWithConfigs";
		List<Solver> solvers = getByUser(userId);
		for (Solver s : solvers) {
			s.getConfigurations().addAll(Solvers.getConfigsForSolver(s.getId()));
		}
		return solvers;
	}

	/**
	 * Retrieves a list of every solver the given user is allowed to use. Used for quick jobs. Solvers a user can see
	 * include solvers they own, solvers in public spaces, and solvers in spaces the user is also in
	 *
	 * @param userId
	 * @return The list of solvers
	 */
	public static List<Solver> getByUser(int userId) {
		final String methodName = "getByUser";
		try {
			//will stores solvers according to their IDs, used to remove duplicates
			HashMap<Integer, Solver> uniqueSolvers = new HashMap<>();
			for (Solver s : getByOwner(userId)) {
				uniqueSolvers.put(s.getId(), s);
			}
			for (Solver s : Solvers.getPublicSolvers()) {
				uniqueSolvers.put(s.getId(), s);
			}

			for (Solver s : Solvers.getSolversInSharedSpaces(userId)) {
				uniqueSolvers.put(s.getId(), s);
			}

			List<Solver> solvers = new ArrayList<>();
			solvers.addAll(uniqueSolvers.values());
			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Returns a list of solvers owned by a given user
	 *
	 * @param userId the id of the user who is the owner of the solvers we are to retrieve
	 * @return a list of solvers owned by a given user, may be empty
	 * @author Todd Elvers
	 */
	public static List<Solver> getByOwner(int userId) {
		final String methodName = "getByOwner";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolversByOwner(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<>();

			while (results.next()) {
				// Build solver object
				Solver s = resultSetToSolver(results);

				// Add solver object to list
				solvers.add(s);
			}

			log.debug(String.format("%d solvers were returned as being owned by user %d.", solvers.size(), userId));

			return solvers;
		} catch (Exception e) {
			String message = String.format("Getting the solvers owned by user %d failed.", userId);
			log.error(methodName, message, e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * @param spaceId The id of the space to get solvers for
	 * @return A list of all solvers belonging directly to the space
	 * @author Tyler Jensen
	 */
	public static List<Solver> getBySpace(int spaceId) {
		final String methodName = "getBySpace";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpaceSolversById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<>();

			while (results.next()) {
				Solver s = resultSetToSolver(results);
				solvers.add(s);
			}

			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets a list of solvers that appear in a job. Only populates name, id, and configs.
	 *
	 * @param jobId the id of the job that we want to get all the solvers for.
	 * @return a list of solvers in the given job.
	 * @throws SQLException on database failure.
	 * @author Albert Giegerich
	 */
	public static List<Solver> getByJobSimpleWithConfigs(int jobId) throws SQLException {
		final String methodName = "getByJobSimpleWithConfigs";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllSolversInJob(?)}");
			procedure.setInt(1, jobId);

			results = procedure.executeQuery();
			List<Solver> solvers = new ArrayList<>();
			while (results.next()) {
				Solver s = new Solver();
				s.setId(results.getInt("solver_id"));
				s.setName(results.getString("solver_name"));
				solvers.add(s);
			}

			for (Solver s : solvers) {
				s.getConfigurations().addAll(Solvers.getConfigsForSolver(s.getId()));
			}

			return solvers;
		} catch (SQLException e) {
			log.error(methodName, "Caught an SQL exception. Database failed.");
			throw e;
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}
	}

	/**
	 * Gets a set of all the id's of configs being used in a job.
	 *
	 * @param jobId the id of the job for which to get configurations.
	 * @return the set of configuration ids that are being used in the job.
	 * @throws SQLException if something goes wrong in the database.
	 * @author Albert Giegerich
	 */
	public static Set<Integer> getConfigIdSetByJob(int jobId) throws SQLException {
		final String methodName = "getConfigIdSetByJob";
		List<Configuration> configurations = getConfigsByJobSimple(jobId);
		Set<Integer> setOfConfigIds = new HashSet<>();
		for (Configuration c : configurations) {
			setOfConfigIds.add(c.getId());
		}
		log.debug(methodName, "Number of configs in job with id: " + jobId + " = " + setOfConfigIds.size());
		return setOfConfigIds;
	}

	/**
	 * Gets all the configurations being used in a job.
	 *
	 * @param jobId the id of the job for which to get configurations.
	 * @return the list of configurations being used in the job.
	 * @throws SQLException if something goes wrong in the database.
	 * @author Albert Giegerich
	 */
	private static List<Configuration> getConfigsByJobSimple(int jobId) throws SQLException {
		final String methodName = "getConfigsByJob";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllConfigsInJob(?)}");
			procedure.setInt(1, jobId);

			results = procedure.executeQuery();
			List<Configuration> configs = new ArrayList<>();
			while (results.next()) {
				Configuration c = new Configuration();
				c.setId(results.getInt("config_id"));
				c.setName(results.getString("config_name"));
				configs.add(c);
			}
			return configs;
		} catch (SQLException e) {
			log.error(methodName, "Caught an SQL exception. Database failed.");
			throw e;
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}
	}

	/**
	 * Gets all the solvers in a list of spaces combined with all the solvers in a job.
	 *
	 * @param jobId the id of the job to get solvers from.
	 * @param spacesAssociatedWithJob the spaces to get solvers from.
	 * @return
	 * @throws SQLException
	 */
	public static List<Solver> getSolversInSpacesAndJob(int jobId, Set<Integer> spacesAssociatedWithJob)
			throws SQLException {

		Comparator<Solver> compareById = Comparator.comparingInt(Identifiable::getId);

		// Get all the solvers in the list of provided spaces.
		List<Solver> solversInSpaces =
				spacesAssociatedWithJob.stream().map(Solvers::getBySpaceDetailed).flatMap(List::stream)
				                       .sorted(compareById).collect(Collectors.toList());

		// Get all the solvers in the job.
		List<Solver> solversInJob = Solvers.getByJobSimpleWithConfigs(jobId);

		Stream<Solver> filteredSolversInJob = solversInJob.stream()
		                                                  // filter out all the solvers in the job that are in the
		                                                  // spaces.
		                                                  .filter(jobSolver -> solversInSpaces.stream().noneMatch(
				                                                  spaceSolver -> spaceSolver.getId() ==

						                                                  jobSolver.getId()));

		return Stream.concat(filteredSolversInJob, solversInSpaces.stream()).collect(Collectors.toList());
	}

	/**
	 * @param spaceId The id of the space to get solvers for
	 * @return A list of all solvers belonging directly to the space, including their configurations
	 * @author Tyler Jensen
	 */
	public static List<Solver> getBySpaceDetailed(int spaceId) {
		final String methodName = "getBySpaceDetailed";
		Connection con = null;

		try {
			con = Common.getConnection();
			List<Solver> solvers = Solvers.getBySpace(spaceId);

			for (Solver s : solvers) {
				s.getConfigurations().addAll(Solvers.getConfigsForSolver(s.getId()));
			}

			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets a list of all the unique solvers in the space hierarchy rooted at the given space.
	 *
	 * @param spaceId The root space of the hierarchy in question
	 * @param userId The ID of the user making the request. Used to filter which spaces the caller can see
	 * @return A list of all the solvers associated with any space in the current hierarchy. Duplicates are filtered
	 * out
	 * by solver id
	 * @author Eric Burns
	 */
	public static List<Solver> getBySpaceHierarchy(int spaceId, int userId) {
		final String methodName = "getBySpaceHierarchy";
		List<Solver> solvers = new ArrayList<>();
		solvers.addAll(Solvers.getBySpace(spaceId));
		List<Space> spaceIds = Spaces.getSubSpaceHierarchy(spaceId, userId);
		for (Space s : spaceIds) {
			solvers.addAll(Solvers.getBySpace(s.getId()));
		}
		List<Solver> filteredSolvers = new ArrayList<>();
		HashSet<Integer> ids = new HashSet<>();
		for (Solver solve : solvers) {
			if (!ids.contains(solve.getId())) {
				ids.add(solve.getId());
				filteredSolvers.add(solve);
			}
		}
		return filteredSolvers;
	}

	/**
	 * Gets all configurations for the given solver
	 *
	 * @param solverId The solver id to get configurations for
	 * @return A list of configurations that belong to the solver
	 * @author Tyler Jensen
	 */
	public static List<Configuration> getConfigsForSolver(int solverId) {
		final String methodName = "getConfigsForSolver";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetConfigsForSolver(?)}");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			List<Configuration> configs = new LinkedList<>();

			while (results.next()) {
				Configuration c = new Configuration();
				c.setId(results.getInt("id"));
				c.setName(results.getString("name"));
				c.setSolverId(results.getInt("solver_id"));
				c.setDescription(results.getString("description"));
				configs.add(c);
			}

			return configs;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Gets a particular Configuration on a connection (excludes deleted configs)
	 *
	 * @param con The connection to query with
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen
	 */
	protected static Configuration getConfiguration(Connection con, int configId) {
		final String methodName = "getConfiguration";
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetConfiguration(?)}");
			procedure.setInt(1, configId);
			results = procedure.executeQuery();
			if (results.next()) {
				Configuration c = new Configuration();
				c.setId(results.getInt("id"));
				c.setName(results.getString("name"));
				c.setSolverId(results.getInt("solver_id"));
				c.setDescription(results.getString("description"));
				Common.safeClose(results);
				return c;
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets a particular Configuration on a connection (includes deleted configs)
	 *
	 * @param con The connection to query with
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen and Alexander Brown
	 */
	protected static Configuration getConfigurationIncludeDeleted(Connection con, int configId) {
		final String methodName = "getConfigurationIncludeDeleted";
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetConfigurationIncludeDeleted(?)}");
			procedure.setInt(1, configId);
			results = procedure.executeQuery();
			if (results.next()) {
				Configuration c = new Configuration();
				c.setId(results.getInt("id"));
				c.setName(results.getString("name"));
				c.setSolverId(results.getInt("solver_id"));
				c.setDescription(results.getString("description"));
				c.setDeleted( results.getInt( "deleted" ) );
				Common.safeClose(results);
				return c;
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * @param jobId The job id to get conflict
	 * @param stageId
	 * @return
	 * @throws SQLException
	 */
	public static List<Benchmark> getConflictingBenchmarksInJobForStage(int jobId, int configId, int stageId)
			throws SQLException {
		return Common.query("{CALL GetConflictingBenchmarksForConfigInJob(?,?,?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setInt(2, configId);
			procedure.setInt(3, stageId);
		}, results -> {
			List<Benchmark> benchmarks = new ArrayList<>();
			while (results.next()) {
				benchmarks.add(Benchmarks.resultToBenchmark(results));
			}
			return benchmarks;
		});
	}

	public static Integer getConflictsForConfigInJobWithStage(int jobId, int configId, int stageId)
			throws SQLException {
		return Common.query("{CALL GetConflictsForConfigInJob(?, ?, ?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setInt(2, configId);
			procedure.setInt(3, stageId);
		}, results -> {
			if (results.next()) {
				return results.getInt("conflicting_benchmarks");
			}
			throw new SQLException("The database did not return a row for procedure GetConflictsForConfigInJob");
		});
	}

	/**
	 * Gets a particular Configuration (excludes deleted configs)
	 *
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen
	 */
	public static Configuration getConfiguration(int configId) {
		final String methodName = "getConfiguration";
		Connection con = null;
		try {
			con = Common.getConnection();
			return Solvers.getConfiguration(con, configId);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Gets a particular Configuration (includes deleted configs)
	 *
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen and Alexander Brown
	 */
	public static Configuration getConfigurationIncludeDeleted(int configId) {
		final String methodName = "getConfigurationIncludeDeleted";
		Connection con = null;
		try {
			con = Common.getConnection();
			return Solvers.getConfigurationIncludeDeleted(con, configId);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Gets the number of Solvers in a given space
	 *
	 * @param spaceId the id of the space to count the Solvers in
	 * @return the number of Solvers
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		return getCountInSpace(spaceId, "");
	}

	/**
	 * Gets the number of Solvers in a given space that match a given query
	 *
	 * @param spaceId the id of the space to count the Solvers in
	 * @param query The query to match the solvers against
	 * @return the number of Solvers
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, String query) {
		final String methodName = "getCountInSpace";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolverCountInSpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * @param solverId
	 * @return The given solver, even if it has been deleted. Null if the solver could not be found
	 */
	public static Solver getIncludeDeleted(int solverId) {
		return get(solverId, true);
	}

	public static Solver getIncludeDeleted(Connection con, int solverId) {
		return get(con, solverId, true);
	}

	/**
	 * Given a ResultSet currently pointing at a row containing a solver, returns the solver
	 *
	 * @param results
	 * @return
	 * @throws SQLException
	 */
	private static Solver resultSetToSolver(ResultSet results) throws SQLException {
		return resultSetToSolver(results, "");
	}

	/**
	 * @return a list of all solvers that reside in a public space
	 * @author Benton McCune
	 */
	public static List<Solver> getPublicSolvers() {
		final String methodName = "getPublicSolvers";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPublicSolvers()}");
			results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<>();

			while (results.next()) {
				Solver s = resultSetToSolver(results);
				solvers.add(s);
			}
			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * @param userId
	 * @return The number of recycled solvers owned by the given user
	 */
	public static int getRecycledSolverCountByUser(int userId) {
		return getRecycledSolverCountByUser(userId, "");
	}

	/**
	 * Gets the number of recycled solvers a user has that match the given query
	 *
	 * @param userId The ID of the user in question
	 * @param query The string query to match on
	 * @return The number of solvers, or -1 on failure
	 * @author Eric Burns
	 */
	public static int getRecycledSolverCountByUser(int userId, String query) {
		final String methodName = "getRecycledSolverCountByUser";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL GetRecycledSolverCountByUser(?,?)");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return -1;
	}

	/**
	 * @param con The connection to make the query on
	 * @param configId The id of the configuration to retrieve the owning solver for
	 * @return A solver object representing the solver that contains the given configuration, or null if the solver
	 * does
	 * not exist
	 * @author Tyler Jensen
	 */
	protected static Solver getSolverByConfig(Connection con, int configId, boolean includeDeleted) {
		final String methodName = "getSolverByConfig";
		Configuration c = Solvers.getConfiguration(con, configId);
		if (c == null) {
			log.debug("getSolverByConfig called with configId = " + configId + " but config was null");
			return null;
		}
		Solver s;
		if (includeDeleted) {
			s = Solvers.getIncludeDeleted(con, c.getSolverId());
		} else {
			s = Solvers.get(con, c.getSolverId(), false);
		}
		if (s == null) {
			return null;
		}
		s.setMostRecentUpdate(Solvers.getMostRecentTimestamp(con, s.getId()));
		s.addConfiguration(c);
		return s;
	}

	/**
	 * @param configId The id of the configuration to retrieve the owning solver for
	 * @param includeDeleted Whether to include deleted solvers (deleted flag is true)
	 * @return A solver object representing the solver that contains the given configuration
	 * @author Tyler Jensen
	 */
	public static Solver getSolverByConfig(int configId, boolean includeDeleted) {
		final String methodName = "getSolverByConfig";
		Connection con = null;

		try {
			con = Common.getConnection();
			return Solvers.getSolverByConfig(con, configId, includeDeleted);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Get the total count of the solvers belong to a specific user
	 *
	 * @param userId Id of the user we are looking for
	 * @return The count of the solvers
	 * @author Wyatt Kaiser
	 */
	public static int getSolverCountByUser(int userId) {
		final String methodName = "getSolverCountByUser";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolverCountByUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}

	/**
	 * Get the total count of the solvers belong to a specific user
	 *
	 * @param userId Id of the user we are looking for
	 * @param query The search query that solvers must match to be returned. Considers solver name and description
	 * @return The count of the solvers
	 * @author Wyatt Kaiser
	 */
	public static int getSolverCountByUser(int userId, String query) {
		final String methodName = "getSolverCountByUser";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSolverCountByUserWithQuery(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Get next page of the solvers belong to a specific user
	 *
	 * @param query A DataTablesQuery object containing the parameters for the search
	 * @param userId Id of the user we are looking for
	 * @param recycled Whether to include recycled solvers
	 * @return a list of Solvers belong to the user
	 * @author Wyatt Kaiser + Eric Burns
	 */
	public static List<Solver> getSolversByUserForNextPage(DataTablesQuery query, int userId, boolean recycled) {
		final String methodName = "getSolversByUserForNextPage";
		Connection con = null;
		ResultSet results = null;
		NamedParameterStatement procedure = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_SOLVERS_BY_USER_QUERY,
			                                                            getSolverOrderColumn(query.getSortColumn()),
			                                                            query
			);
			procedure = new NamedParameterStatement(con, builder.getSQL());

			procedure.setInt("userId", userId);
			procedure.setString("query", query.getSearchQuery());
			procedure.setBoolean("recycled", recycled);

			results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<>();

			// Only get the necessary information to display this solver
			// in a row in a DataTable object, nothing more.
			while (results.next()) {
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));
				if (results.getBoolean("deleted")) {
					s.setName(s.getName() + " (deleted)");
				}
				s.setDeleted(results.getBoolean("deleted"));
				s.setRecycled(results.getBoolean("recycled"));
				s.setUserId(results.getInt("user_id"));
				s.setDescription(results.getString("description"));
				s.setType(ExecutableType.valueOf(results.getInt("executable_type")));
				solvers.add(s);
			}

			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	private static String getSolverOrderColumn(int orderIndex) {
		switch (orderIndex) {
			case 0:
				return "name";
			case 1:
				return "description";
			case 2:
				return "type_name";
			default:
				return "name";
		}
	}

	/**
	 * Returns the solvers needed to populate the DataTables page of solvers on the space explorer
	 *
	 * @param query A DataTablesQuery object
	 * @param spaceId The ID of the space to get solvers for
	 * @return A list of solvers, or null on error
	 */
	public static List<Solver> getSolversForNextPage(DataTablesQuery query, int spaceId) {
		final String methodName = "getSolversForNextPage";
		Connection con = null;
		ResultSet results = null;
		NamedParameterStatement procedure = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_SOLVERS_IN_SPACE_QUERY,
			                                                            getSolverOrderColumn(query.getSortColumn()),
			                                                            query
			);

			procedure = new NamedParameterStatement(con, builder.getSQL());

			procedure.setInt("spaceId", spaceId);
			procedure.setString("query", query.getSearchQuery());

			results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<>();

			// Only get the necessary information to display this solver
			// in a row in a DataTable object, nothing more.
			while (results.next()) {
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));
				if (results.getBoolean("deleted")) {
					s.setName(s.getName() + " (deleted)");
				} else if (results.getBoolean("recycled")) {
					s.setName(s.getName() + " (in trash)");
				}
				s.setDeleted(results.getBoolean("deleted"));
				s.setRecycled(results.getBoolean("recycled"));
				s.setUserId(results.getInt("user_id"));
				s.setDescription(results.getString("description"));
				s.setType(ExecutableType.valueOf(results.getInt("executable_type")));
				solvers.add(s);
			}

			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * @param solverId The id of the solver to retrieve
	 * @param configId The id of the configuration to include with the solver
	 * @return The solver with the given configuration
	 * @author Tyler Jensen
	 */
	public static Solver getWithConfig(int solverId, int configId) {
		final String methodName = "getWithConfig";
		Solver s = Solvers.get(solverId);
		Configuration c = Solvers.getConfiguration(configId);
		if (s == null) {
			log.warn(methodName, "Solver not found: " + solverId);
		} else if (c == null) {
			log.warn(methodName, "Configuration not found: " + configId);
		} else if (s.getId() != c.getSolverId()) {
			String message =
					String.format("Configuration '%s' does not belong to Solver '%s'", c.getName(), s.getName());
			log.warn(methodName, message, new Throwable());
		} else {
			// Make sure this configuration actually belongs to the solver, and add it if it does
			s.addConfiguration(c);
		}
		return s;
	}

	/**
	 * This takes in a list of configuration ids and matches them up with the solvers they go with, returning one
	 * solver
	 * object for each configuration.
	 *
	 * @param configIds A list of configurations, where each one is retrieved along with the solvers in the given
	 * order.
	 * @return A list of solvers, where each one ownsconfiguration as specified in the configIds list
	 * @author Tyler Jensen & Skylar Stark
	 */
	public static List<Solver> getWithConfig(List<Integer> configIds) {
		final String methodName = "getWithConfig";
		try {
			List<Solver> solvers = new LinkedList<>();
			for (int cid : configIds) {
				Solver s = Solvers.getByConfigId(cid);
				if (s != null) {
					solvers.add(s);
				}
			}
			return solvers;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		}
	}

	public static boolean isPublic(Connection con, int solverId) {
		final String methodName = "isPublic";
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL IsSolverPublic(?)}");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();

			boolean publicSpace = false;
			if (results.next()) {
				publicSpace = (results.getInt("solverPublic") > 0);
			}
			if (publicSpace) {
				return true;
			}

			Common.safeClose(results);

			Common.safeClose(procedure);
			//if the solver is in no public spaces, check to see if it is the default solver for some community
			procedure = con.prepareCall("CALL IsSolverACommunityDefault(?)");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			if (results.next()) {
				return (results.getInt("solverDefault") > 0);
			}
		} catch (Exception e) {
			log.error(methodName, "Caught Exception.", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * A solver is public if it is in any public space or if it is the default solver for a community
	 *
	 * @param solverId
	 * @return True on success and false otherwise
	 */
	public static boolean isPublic(int solverId) {
		final String methodName = "isPublic";
		Connection con = null;
		try {
			con = Common.getConnection();
			return isPublic(con, solverId);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Returns whether a solver with the given ID is present in the database and marked "deleted"
	 *
	 * @param con The open connection to make the query on
	 * @param solverId The ID of the solver in question
	 * @return True if the solver has been deleted, false otherwise (includes the possibility of an error)
	 */
	protected static boolean isSolverDeleted(Connection con, int solverId) {
		final String methodName = "isSolverDeleted";
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL IsSolverDeleted(?)}");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			boolean deleted = false;
			if (results.next()) {
				deleted = results.getBoolean("solverDeleted");
			}
			return deleted;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Determines whether the solver with the given ID exists in the database with the column "deleted" set to true
	 *
	 * @param solverId The ID of the solver in question
	 * @return True if the solver exists in the database and has the deleted flag set to true
	 */
	public static boolean isSolverDeleted(int solverId) {
		final String methodName = "isSolverDeleted";
		Connection con = null;
		try {
			con = Common.getConnection();
			return isSolverDeleted(con, solverId);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Returns whether a solver with the given ID is present in the database with the "recycled" column set to true
	 *
	 * @param solverId The ID of the solver to check
	 * @return True if the solver exists in the database with the "recycled" column set to true, and false otherwise
	 * @author Eric Burns
	 */
	protected static boolean isSolverRecycled(Connection con, int solverId) {
		final String methodName = "isSolverRecycled";
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL IsSolverRecycled(?)}");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			boolean deleted = false;
			if (results.next()) {
				deleted = results.getBoolean("recycled");
			}
			return deleted;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Returns whether a solver with the given ID is present in the database with the "recycled" column set to true
	 *
	 * @param solverId The ID of the solver to check
	 * @return True if the solver exists in the database with the "recycled" column set to true, and false otherwise
	 * @author Eric Burns
	 */
	public static boolean isSolverRecycled(int solverId) {
		final String methodName = "isSolverRecycled";
		Connection con = null;
		try {
			con = Common.getConnection();
			return isSolverRecycled(con, solverId);
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Sets the "recycled" flag in the database to true. Indicates the user has moved the solver to the recycle bin,
	 * from which in can be deleted
	 *
	 * @param id the id of the solver to recycled
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean recycle(int id) {
		return setRecycledState(id, true);
	}

	/**
	 * Given a list of solvers and a user ID, recycles all of the solvers in the list that the user owns
	 *
	 * @param solvers
	 * @param userId
	 * @return True on success and false otherwise
	 */
	public static boolean recycleSolversOwnedByUser(Collection<Solver> solvers, int userId) {
		boolean success = true;
		for (Solver s : solvers) {
			if (s.getUserId() == userId) {
				success = success && Solvers.recycle(s.getId());
			}
		}
		return success;
	}

	/**
	 * Sets the "recycled" flag in the database to false. Indicates the user has removed the solver from the recycle
	 * bin
	 *
	 * @param id the id of the solver to be removed from the recycle bin
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean restore(int id) {
		return setRecycledState(id, false);
	}

	/**
	 * Restores all solvers a user has that have been recycled to normal
	 *
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean restoreRecycledSolvers(int userId) {
		final String methodName = "restoreRecycledSolvers";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			Common.safeClose(procedure);
			procedure = con.prepareCall("CALL GetRecycledSolverIds(?)");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			while (results.next()) {
				Solvers.restore(results.getInt("id"));
			}
			return true;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	public static List<Triple<Solver, Configuration, String>> getSolverConfigResultsForBenchmarkInJob(
			int jobId, int benchId, int stageNum
	) throws SQLException {
		final String methodName = "getSolverConfigResultsForBenchmarkInJob";
		return Common.query("{CALL GetSolverConfigResultsForBenchmarkInJob(?,?,?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setInt(2, benchId);
			procedure.setInt(3, stageNum);
		}, results -> {
			List<Triple<Solver, Configuration, String>> solverConfigResult = new ArrayList<>();
			while (results.next()) {
				Solver solver = resultSetToSolver(results, "s");
				Configuration configuration = resultSetToConfiguration(results, "c");
				String starexecResult = results.getString("attr_value");
				solverConfigResult.add(new ImmutableTriple<>(solver, configuration, starexecResult));
			}
			return solverConfigResult;
		});
	}

	private static String transformPrefix(String prefix) {
		// first format the prefix so it is either empty OR is the prefix plus a period
		if (prefix != null && !prefix.isEmpty()) {
			return prefix + ".";
		}
		return "";
	}

	/**
	 * Given a ResultSet containing solver info, returns a Solver object representing  that info
	 *
	 * @param results The resultset pointed at the row with solver information
	 * @param prefix If there is an "AS <name>" clause in the SQL query that gave this resultset, prefix should be
	 * <name>
	 * @return A Solver object
	 * @throws SQLException
	 */

	protected static Solver resultSetToSolver(ResultSet results, String prefix) throws SQLException {
		Solver s = new Solver();

		prefix = transformPrefix(prefix);

		s.setId(results.getInt(prefix + "id"));
		s.setUserId(results.getInt(prefix + "user_id"));
		s.setName(results.getString(prefix + "name"));
		s.setUploadDate(results.getTimestamp(prefix + "uploaded"));
		s.setPath(results.getString(prefix + "path"));
		s.setDescription(results.getString(prefix + "description"));
		s.setDownloadable(results.getBoolean(prefix + "downloadable"));
		s.setDiskSize(results.getLong(prefix + "disk_size"));
		s.setType(ExecutableType.valueOf(results.getInt("executable_type")));
		s.setRecycled(results.getBoolean("recycled"));
		s.setDeleted(results.getBoolean("deleted"));
		SolverBuildStatus status = new SolverBuildStatus();
		status.setCode(results.getInt(prefix + "build_status"));
		s.setBuildStatus(status);

		return s;
	}

	public static Configuration resultSetToConfiguration(ResultSet results, String prefix) throws SQLException {
		prefix = transformPrefix(prefix);
		Configuration config = new Configuration();
		config.setId(results.getInt(prefix + "id"));
		config.setDescription(results.getString(prefix + "description"));
		config.setName(results.getString(prefix + "name"));
		config.setSolverId(results.getInt(prefix + "solver_id"));
		return config;
	}

	/**
	 * Sets every file in a hierarchy to be executable
	 *
	 * @param rootDir the directory that we wish to have executable files in
	 */
	public static void setHierarchyExecutable(File rootDir) {
		for (File f : rootDir.listFiles()) {
			f.setExecutable(true, false);
			if (f.isDirectory()) {
				setHierarchyExecutable(f);
			}
		}
	}

	/**
	 * Deletes all solvers a user has from disk and sets their "deleted" flag to true. Solvers should NOT be deleted
	 * from the database, as they still may be associated with spaces
	 *
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean setRecycledSolversToDeleted(int userId) {
		final String methodName = "setRecycledSolversToDeleted";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL GetRecycledSolverPaths(?)");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			while (results.next()) {
				Util.safeDeleteDirectory(results.getString("path"));
				Util.safeDeleteDirectory(results.getString("path") + "_src");
				File buildOutput = Solvers.getSolverBuildOutput(results.getInt("id"));
				if (buildOutput.exists()) {
					Util.safeDeleteDirectory(buildOutput.getParent());
				}
			}
			Common.safeClose(procedure);
			procedure = con.prepareCall("CALL SetRecycledSolversToDeleted(?)");
			procedure.setInt(1, userId);
			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Sets the "recycled" flag in the database to the given value.
	 *
	 * @param id the id of the solver to recycled or restored
	 * @param state The value to assign to the recycled field
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean setRecycledState(int id, boolean state) {
		final String methodName = "setRecycledState";
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL SetSolverRecycledValue(?, ?)}");
			procedure.setInt(1, id);
			procedure.setBoolean(2, state);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Updates a configuration name, description and file contents
	 *
	 * @param configId the id of the configuration to update
	 * @param name the new name to update the configuration with (this will also affect the filename on disk)
	 * @param description the new description to update the configuration with
	 * @return true iff the configuration file is successfully updated, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateConfigDetails(int configId, String name, String description) {
		final String methodName = "updateConfigDetails";
		Connection con = null;
		CallableStatement procedure = null;
		try {

			// Try and update the configuration file's name and/or contents
			if (Solvers.updateConfigFile(configId, name)) {

				// If the physical configuration file was successfully renamed, update the database too
				con = Common.getConnection();
				procedure = con.prepareCall("{CALL UpdateConfigurationDetails(?, ?, ?, ?)}");
				procedure.setInt(1, configId);
				procedure.setString(2, name);
				procedure.setString(3, description);
				procedure.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
				procedure.executeUpdate();

				log.info(String.format("Configuration [%s] has been successfully updated.", name));
				return true;
			}
		} catch (Exception e) {
			String message = String.format("Configuration [%s] failed to update properly.", name);
			log.error(methodName, message, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates a configuration file's contents and/or filename
	 *
	 * @param configId the id of the configuration whose file is to be updated
	 * @param newConfigName the new configuration filename
	 * @return true iff the configuration file was successfully updated, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateConfigFile(int configId, String newConfigName) {
		final String methodName = "updateConfigFile";
		try {
			if (configId < 0 || Util.isNullOrEmpty(newConfigName)) {
				log.warn("The configuration file parameters to update with are invalid.");
				return false;
			}

			Solver s = Solvers.getSolverByConfig(configId, false);
			Configuration config = Solvers.getConfiguration(configId);
			boolean isConfigNameUnchanged = true;

			// Build path to old configuration file (should exist)
			File oldConfig = new File(Util.getSolverConfigPath(s.getPath(), config.getName()));

			// Build path to new configuration file (should not yet exist)
			File newConfig = new File(Util.getSolverConfigPath(s.getPath(), newConfigName));

			// If the old config and new config names are NOT the same, ensure the file pointed to by
			// the new config does not already exist on disk
			if (!oldConfig.getName().equals(newConfig.getName())) {
				isConfigNameUnchanged = false;
				if (newConfig.exists()) {
					return false;
				}
			}

			// Rename the file if necessary
			if (!isConfigNameUnchanged) {
				FileUtils.moveFile(oldConfig, newConfig);
			}
			return true;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Updates the details of a solver
	 *
	 * @param id the id of the solver to update
	 * @param name the new name to apply to the solver
	 * @param description the new description to apply to the solver
	 * @param isDownloadable boolean indicating whether or not this solver is downloadable
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateDetails(int id, String name, String description, boolean isDownloadable) {
		final String methodName = "updateDetails";
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateSolverDetails(?, ?, ?, ?)}");
			procedure.setInt(1, id);
			procedure.setString(2, name);
			procedure.setString(3, description);
			procedure.setBoolean(4, isDownloadable);

			procedure.executeUpdate();
			log.debug(String.format("Solver [id=%d] was successfully updated.", id));

			return true;
		} catch (Exception e) {
			String message = String.format("Solver [id=%d] failed to be updated.", id);
			log.error(methodName, message, e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Updates a solver's disk_size attribute on an existing transaction
	 *
	 * @param con the database transaction to use while updating the solver's disk size
	 * @param s the solver object containing the new disk size to set
	 * @author Todd Elvers
	 */
	private static void updateSolverDiskSize(Connection con, Solver s) {
		final String methodName = "updateSolverDiskSize";
		CallableStatement procedure = null;
		try {
			// Get the size of the solver's directory
			File solverDir = new File(s.getPath());
			s.setDiskSize(FileUtils.sizeOfDirectory(solverDir));

			// Update the database to reflect the solver's directory size
			procedure = con.prepareCall("{CALL UpdateSolverDiskSize(?, ?)}");
			procedure.setInt(1, s.getId());
			procedure.setLong(2, s.getDiskSize());

			procedure.executeUpdate();
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Updates a solver's disk_size attribute
	 *
	 * @param solver the solver object containing the new disk size to set
	 * @return true iff the solver's size was successfully updated, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateSolverDiskSize(Solver solver) {
		final String methodName = "updateSolverDiskSize";
		Connection con = null;
		try {
			con = Common.getConnection();
			Solvers.updateSolverDiskSize(con, solver);
			log.info(String.format("Configuration's parent solver '%s' has had its disk size successfully updated.",
			                       solver.getName()
			));
			return true;
		} catch (Exception e) {
			String message =
					String.format("Configuration's parent solver '%s' has failed to have its disk size updated.",
					              solver.getName()
					);
			log.error(methodName, message, e);
			return false;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Gets the timestamp of the configuration associated with this solver that was added or updated the most recently
	 *
	 * @param solverId The ID of the solver in question
	 * @param con An open connection to make the call on
	 * @return The timestamp as a string, or null on failure
	 * @author Eric Burns
	 */
	public static String getMostRecentTimestamp(Connection con, int solverId) {
		final String methodName = "getMostRecentTimestamp";
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetMaxConfigTimestamp(?)}");
			procedure.setInt(1, solverId);
			results = procedure.executeQuery();
			if (results.next()) {
				Timestamp t = (results.getTimestamp("recent"));
				//timestamp doesn't like SQL's default string of zeroes, so if that is present
				//we get a null value
				if (t == null) {
					return "0000-00-00";
				}
				return t.toString();
			}
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Returns the path on disk to where a new solver should be stored
	 *
	 * @param userId
	 * @param solverName
	 * @return The absolute path as a string
	 */
	public static String getDefaultSolverPath(int userId, String solverName) {
		File uniqueDir = new File(R.getSolverPath(), "" + userId);
		uniqueDir = new File(uniqueDir, solverName);
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		return uniqueDir.getAbsolutePath();
	}

	/**
	 * Gets the file where build information is stored
	 *
	 * @param solverId
	 * @return The File object, or null if one cannot be found
	 */
	public static File getSolverBuildOutput(int solverId) {
		final String methodName = "getSolverBuildOutput";
		try {
			File buildFile = new File(R.getSolverBuildOutputDir(), "" + solverId);
			buildFile = new File(buildFile, R.SOLVER_BUILD_OUTPUT);
			return buildFile;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Gets the ID of every orphaned solver the given user owns
	 *
	 * @param userId
	 * @return A list of IDs of solvers that are not in any spaces and are owned by the given user
	 */
	public static List<Integer> getOrphanedSolvers(int userId) {
		final String methodName = "getOrphanedSolvers";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		List<Integer> ids = new ArrayList<>();
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetOrphanedSolverIds(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Recycles all of the solvers a user has that are not in any spaces
	 *
	 * @param userId The ID of the user who will have their solvers recycled
	 * @return True on success and false otherwise
	 */
	public static boolean recycleOrphanedSolvers(int userId) {
		final String methodName = "recycleOrphanedSolvers";
		List<Integer> ids = getOrphanedSolvers(userId);

		//now that we have all the orphaned ids, we can recycle them
		try {
			boolean success = true;
			for (Integer id : ids) {
				success = success && Solvers.recycle(id);
			}
			return success;
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Filters a list of solvers using the given search query.
	 *
	 * @param solvers The list of solvers to filter
	 * @param searchQuery Query for the solvers. Not case sensitive
	 * @return A subset of the given solvers where, for every solver returned, either the name or the description
	 * includes the search query.
	 */
	protected static List<Solver> filterSolvers(List<Solver> solvers, String searchQuery) {
		final String methodName = "filterSolvers";
		//no filtering is necessary if there's no query
		if (Util.isNullOrEmpty(searchQuery)) {
			return solvers;
		}
		searchQuery = searchQuery.toLowerCase();
		List<Solver> filteredSolvers = new ArrayList<>();
		for (Solver s : solvers) {
			try {
				if (s.getName().toLowerCase().contains(searchQuery) ||
						s.getDescription().toLowerCase().contains(searchQuery)) {
					filteredSolvers.add(s);
				}
			} catch (Exception e) {
				log.warn("filtering solvers had an exception for solver id= " + s.getId());
			}
		}
		return filteredSolvers;
	}

	/**
	 * Returns the Solvers needed to populate a DataTables page for a given user. Solvers include all solvers the user
	 * can see
	 *
	 * @param query DataTablesQuery object containing data for this search
	 * @param userId ID of user to get solvers for
	 * @param totals Size 2 array that, on return, will contain the total number of records as the first element and
	 * the
	 * total number of elements after filtering as the second element
	 * @return Solvers to display on the next page, in order
	 */
	public static List<Solver> getSolversForNextPageByUser(DataTablesQuery query, int userId, int[] totals) {
		final String methodName = "getSolversForNextPageByUser";
		List<Solver> solvers = Solvers.getByUser(userId);

		totals[0] = solvers.size();
		solvers = Solvers.filterSolvers(solvers, query.getSearchQuery());

		totals[1] = solvers.size();
		SolverComparator compare = new SolverComparator(query.getSortColumn(), query.isSortASC());
		return Util.handlePagination(solvers, compare, query.getStartingRecord(), query.getNumRecords());
	}

	/**
	 * Rearranges configurations for all input solvers so that there default config appears first.
	 *
	 * @param solvers The list of solvers to rearrange configurations for.
	 * @author Albert Giegerich
	 */
	public static void makeDefaultConfigsFirst(List<Solver> solvers) {
		for (Solver s : solvers) {
			List<Configuration> configs = s.getConfigurations();
			for (int j = 0; j < configs.size(); j++) {
				Configuration c = configs.get(j);
				if (c.getName().equals("default")) {
					// This modifies the list but will maintain the ordering so every configuration
					// is visited
					configs.remove(j);
					configs.add(0, c);
				}
			}
		}
	}

	/**
	 * Sorts configurations alphabetically for a list of solvers
	 *
	 * @param solvers The list of solvers to sort configs for
	 * @author Andrew Lubinus
	 */
	public static void sortConfigs(List<Solver> solvers) {
		for (Solver s : solvers) {
			s.getConfigurations().sort(Comparator.comparing(Configuration::getName));
		}
	}

	/**
	 * Sets the solver build status
	 *
	 * @param s the solver id for the solver to be updated
	 * @param status the integer status code to be set
	 * @author Andrew Lubinus
	 */
	public static void setSolverBuildStatus(Solver s, int status) {
		final String methodName = "setSolverBuildStatus";
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL SetSolverBuildStatus(?, ?)}");
			procedure.setInt(1, s.getId());
			procedure.setInt(2, status);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error(methodName, e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
	}
}
