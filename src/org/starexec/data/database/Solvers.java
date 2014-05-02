package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.CacheType;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.util.Util;

/**
 * Handles all database interaction for solvers
 */
public class Solvers {
	private static final Logger log = Logger.getLogger(Solvers.class);
	private static DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT); 
	private static final String CONFIG_PREFIX = R.CONFIGURATION_PREFIX;
	
	/**
	 * Adds a solver to the database. Solver association to a space and run configurations
	 * for the solver are also added. Every insert must pass for any of them to be added.
	 * @param s the Solver to be added
	 * @param spaceId the ID of the space we associate this solver with
	 * @return The solverID of the new solver, or -1 on failure
	 * @author Skylar Stark
	 */
	public static int add(Solver s, int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			long diskUsage=FileUtils.sizeOf(new File(s.getPath()));
			// Add the solver
			 procedure = con.prepareCall("{CALL AddSolver(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, s.getUserId());
			procedure.setString(2, s.getName());
			procedure.setBoolean(3, s.isDownloadable());
			procedure.setString(4, s.getPath());
			procedure.setString(5, s.getDescription());
			procedure.registerOutParameter(6, java.sql.Types.INTEGER);
			procedure.setLong(7, diskUsage);
			
			procedure.executeUpdate();
			
			// Associate the solver with the given space
			int solverId = procedure.getInt(6);			
			Solvers.associate(con, spaceId, solverId);
			
			// Add solver configurations
			for (Configuration c : s.getConfigurations()) {
				c.setSolverId(solverId);
				addConfiguration(con, c);
			}
			Cache.invalidateAndDeleteCache(spaceId,CacheType.CACHE_SPACE);
			return solverId;						
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}		
		
		return -1;
	}
	
	/**
	 * Adds a run configuration to the database
	 * @param con the database connection associated with the whole process of adding the solver
	 * @param c the configuration we are adding
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	protected static int addConfiguration(Connection con, Configuration c) throws Exception {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddConfiguration(?, ?, ?, ?, ?)}");
			procedure.setInt(1, c.getSolverId());
			procedure.setString(2, c.getName());
			procedure.setString(3, c.getDescription());
			procedure.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			procedure.registerOutParameter(5, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			return procedure.getInt(5);
		} catch (Exception e) {
			log.error("addConfiguration says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return -1;
	}
	/**
	 * Adds a configuration entry in the database for a particular solver 
	 * and updates that solver's disk size to reflect the new file
	 *
	 * @param s the solver to add the configuration to
	 * @param c the configuration to add to said solver
	 * @return either a value greater than or equal to 0, reflecting the newly added configuration's id in the
	 * database, or a -1 indicating an error occurred
	 * @author Todd Elvers
	 */
	public static int addConfiguration(Solver s, Configuration c) {
		Connection con = null;
		try {
			con = Common.getConnection();
			c.setSolverId(s.getId());
			
			int newConfigId = addConfiguration(con,c);
			
			// Update the disk size of the parent solver to include the new configuration file's size
			Solvers.updateSolverDiskSize(con, s);
			//invalidate the cache of any spaces with this solver
			Cache.invalidateSpacesAssociatedWithSolver(c.getSolverId());
			Cache.invalidateAndDeleteCache(c.getSolverId(), CacheType.CACHE_SOLVER);
			
			return newConfigId;						
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}		
		
		return -1;
	}
	
	/**
	 * Adds a Space/Solver association
	 * @param con the database connection associated with the whole process of adding the solver
	 * @param spaceId the ID of the space we are making the association to
	 * @param solverId the ID of the solver we are associating to the space
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	protected static boolean associate(Connection con, int spaceId, int solverId) throws Exception {
		CallableStatement procedure = null;
		try {
			 procedure = con.prepareCall("{CALL AddSolverAssociation(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, solverId);
			
			procedure.executeUpdate();		
			Cache.invalidateAndDeleteCache(spaceId, CacheType.CACHE_SPACE);
			return true;
		} catch (Exception e) {
			log.error("Solvers.associate says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Adds an association between a list of solvers and a space
	 * 
	 * @param con the database transaction to use
	 * @param solverIds the ids of the solvers to add to a space
	 * @param spaceId the id of the space to add the solvers to
	 * @return true iff all solvers in solverIds are successfully 
	 * added to the space represented by spaceId,<br> false otherwise
	 * @throws Exception
	 * @author Todd Elvers
	 */
	protected static boolean associate(Connection con, List<Integer> solverIds, int spaceId) throws Exception {
		for(int sid : solverIds) {
			Solvers.associate(con, spaceId, sid);
		}
		return true;
	}
	
	/**
	 * Adds an association between all the given solver ids and the given space
	 * @param solverIds the ids of the solvers we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> solverIds, int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			for(int sid : solverIds) {
				Solvers.associate(con, spaceId, sid);
			}
			
			log.info("Successfully added solvers " + solverIds.toString() + " to space [" + spaceId + "]");
			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error("Failed to add solvers " + solverIds.toString() + " to space [" + spaceId + "]");
		return false;
	}
	
	public static boolean associate(List<Integer> solverIds, int rootSpaceId, boolean linkInSubspaces, int userId, boolean includeRoot) {
		// Either copy the solvers to the destination space or the destination space and all of its subspaces (that the user can see)
		if (linkInSubspaces) {
			
			List<Space> subspaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(rootSpaceId, userId));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces to associate the solvers with only
			//if we aren't copying. If we're copying, we did this already
			if (includeRoot) {
				subspaceIds.add(rootSpaceId);
			}
			
			// Iterate once through all subspaces of the destination space to ensure the user has addSolver permissions in each
			for(Space subspace : subspaces){
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
	 * @return true iff all spaces in spaceIds successfully have all 
	 * solvers in solverIds add to them, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean associate(List<Integer> solverIds, List<Integer> spaceIds) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			// For each space id in spaceIds, add all the solvers to it
			for(int spaceId : spaceIds) {
				Solvers.associate(con, solverIds, spaceId);
			}
			
			log.info("Successfully added solvers " + solverIds.toString() + " to spaces " + spaceIds.toString());
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error("Failed to add solvers " + solverIds.toString() + " to spaces " + spaceIds.toString());
		return false;
	}
	
	/**
	 * Removes all solver database entries where the solver has been deleted
	 * AND has been orphaned
	 * @return True on success, false on error
	 */
	public static boolean cleanOrphanedDeletedSolvers() {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL RemoveDeletedOrphanedSolvers()}");
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("cleanOrphanedDeletedSolvers says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	/**
	 * Copies a list of solvers into the given space, assigning the given user as the owner of each one
	 * @param solvers A list of solves
	 * @param userId The ID of the user who will own the new solvers
	 * @param spaceId The ID of the space to put the solvers in
	 * @return The IDs of the new solvers, in the same order that the solvers are in. Negative numbers indicate
	 * errors associated with the corresponding solvers
	 */
	public static List<Integer> copySolvers(List<Solver> solvers, int userId, int spaceId) {
		List<Integer> ids= new ArrayList<Integer>();
		for (Solver s : solvers) {
			ids.add(copySolver(s,userId,spaceId));
			
		}
		return ids;
	}
	
	/**
	 * Makes a deep copy of an existing solver, gives it a new user, and places it
	 * into a space
	 * @param s The existing solver to copy
	 * @param userId The userID that the new solver will be given
	 * @param spaceId The space ID of the space to place the new solver in to
	 * @return The ID of the new solver, or -1 on failure
	 * @author Eric Burns
	 */
	
	public static int copySolver(Solver s, int userId, int spaceId) {
		log.debug("Copying solver "+s.getName()+" to new user id= "+String.valueOf(userId));
		Solver newSolver=new Solver();
		newSolver.setDescription(s.getDescription());
		newSolver.setName(s.getName());
		newSolver.setUserId(userId);
		newSolver.setUploadDate(s.getUploadDate());
		newSolver.setDiskSize(s.getDiskSize());
		newSolver.setDownloadable(s.isDownloadable());
		File solverDirectory=new File(s.getPath());
		
		File uniqueDir = new File(R.SOLVER_PATH, "" + userId);
		uniqueDir = new File(uniqueDir, newSolver.getName());
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		uniqueDir.mkdirs();
		newSolver.setPath(uniqueDir.getAbsolutePath());
		try {
			FileUtils.copyDirectory(solverDirectory, uniqueDir);
			for(Configuration c : findConfigs(uniqueDir.getAbsolutePath())) {
				newSolver.addConfiguration(c);
			}
			return Solvers.add(newSolver, spaceId);
			
		} catch (Exception e) {
			
			log.error("copySolver says "+e.getMessage());
			return -1;
		}	
	}
	
	
	/**
	 * Deletes a solver from the database (cascading deletes handle all dependencies) 
	 * @param id the id of the solver to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int id){
		log.debug("Solvers.delete() called on solver with id = "+id);
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			Cache.invalidateSpacesAssociatedWithSolver(id);
			Cache.invalidateAndDeleteCache(id, CacheType.CACHE_SOLVER);
			Cache.invalidateAndDeleteCache(id,CacheType.CACHE_SOLVER_REUPLOAD);
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL SetSolverToDeletedById(?, ?)}");
			procedure.setInt(1, id);
			procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			// Delete solver file from disk, and the parent directory if it's empty
			Util.safeDeleteDirectory(procedure.getString(2));
			File file=new File(procedure.getString(2));
			if (file.getParentFile().exists()) {
				file.getParentFile().delete();
			}
			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.debug(String.format("Deletion of solver [id=%d] failed.", id));
		return false;
	}
	
	/**
	 * Deletes a configuration from the database given that configuration's id
	 *
	 * @param configId the id of the configuration to remove from the database
	 * @return true iff the configuration is successfully deleted from the database, 
	 * false otherwise
	 * @author Todd Elvers
	 */
	public static boolean deleteConfiguration(int configId) {
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			int solverId=Solvers.getSolverByConfig(configId, false).getId();
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL DeleteConfigurationById(?)}");	
			procedure.setInt(1, configId);
			procedure.executeUpdate();
			
			log.info(String.format("Configuration %d has been successfully deleted from the database.", configId));
			Cache.invalidateSpacesAssociatedWithSolver(solverId);
			Cache.invalidateAndDeleteCache(solverId, CacheType.CACHE_SOLVER);
			Cache.invalidateAndDeleteCache(solverId,CacheType.CACHE_SOLVER_REUPLOAD);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.warn(String.format("Configuration %d has failed to be deleted from the database.", configId));
		return false;
	}
	/**
	 * Deletes a given configuration object's physical file from disk
	 *
	 * @param config the configuration whose physical file is to be deleted from disk
	 * @return true iff the configuration object's corresponding physical file is successfully deleted from disk,
	 *  false otherwise
	 * @author Todd Elvers
	 */
	public static boolean deleteConfigurationFile(Configuration config) {
		try {
			// Builds the path to the configuration object's physical file on disk, then deletes it from disk
			File configFile = new File(Util.getSolverConfigPath(Solvers.getSolverByConfig(config.getId(),false).getPath(), config.getName()));
			if(configFile.delete()){
				log.info(String.format("Configuration %d has been successfully deleted from disk.", config.getId()));
				return true;
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		log.warn(String.format("Configuration %d has failed to be deleted from disk.", config.getId()));
		return false;
	}
	/**
	 * Finds solver run configurations from a specified bin directory. Run configurations
	 * must start with a certain string specified in the list of constants. If no configurations
	 * are found, an empty list is returned.
	 * @param fromPath the base directory to find the bin directory in
	 * @return a list containing run configurations found in the bin directory
	 */
	public static List<Configuration> findConfigs(String fromPath){		
		File binDir = new File(fromPath, R.SOLVER_BIN_DIR);
		if(!binDir.exists()) {
			return Collections.emptyList();
		}
		
		List<Configuration> returnList = new ArrayList<Configuration>();
		
		for(File f : binDir.listFiles()){	
			if(f.isFile() && f.getName().startsWith(CONFIG_PREFIX)){

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
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	protected static Solver get(Connection con, int solverId, boolean includeDeleted) throws Exception {	
		CallableStatement procedure=null;
		
		ResultSet results= null;
		
		try {
			if (!includeDeleted) {
				procedure = con.prepareCall("{CALL GetSolverById(?)}");
			} else {
				procedure=con.prepareCall("{CALL GetSolverByIdIncludeDeleted(?)}");
			}
			procedure.setInt(1, solverId);					
			results = procedure.executeQuery();
			if(results.next()){
				Solver s = resultToSolver(results,null);
				Common.safeClose(results);
				return s;
			}
		} catch (Exception e) {
			log.error("Solvers.get says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
										
		
		return null;
	}
	
	public static Solver get(int solverId) {
		return get(solverId,false);
	}
	
	/**
	 * @param solverId The id of the solver to retrieve
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	public static Solver get(int solverId, boolean includeDeleted) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			return Solvers.get(con, solverId,includeDeleted);		
		} catch (Exception e){			
			log.error("Solver get says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param solverIds The ids of the solvers to retrieve
	 * @return A list of solver objects representing the solvers with the given IDs
	 * @author Tyler Jensen
	 */
	public static List<Solver> get(List<Integer> solverIds) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			List<Solver> solvers = new LinkedList<Solver>();
			
			for(int id : solverIds) {
				solvers.add(Solvers.get(con, id,false));
			}
			
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Gets the IDs of every space that is associated with the given solver
	 * @param solverId The solver in question
	 * @return A list of space IDs that are associated with this solver
	 * @author Eric Burns
	 */
	public static List<Integer> getAssociatedSpaceIds(int solverId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results = null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetAssociatedSpaceIdsBySolver(?)}");
			procedure.setInt(1,solverId);
			results = procedure.executeQuery();
			List<Integer> ids=new ArrayList<Integer>();
			while (results.next()) {
				ids.add(results.getInt("space_id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("Solvers.getAssociatedSpaceIds says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	/**
	 * Returns a solver that is associated with a given configuration.
	 * 
	 * @param configId The id of the configuration of the solver we want to return
	 * @return a solver with the configuration with the given configuration id
	 * 
	 * @author Skylar Stark
	 */
	private static Solver getByConfigId(int configId) {
		Connection con = null;			
		ResultSet results=null;
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
				if(sid == c.getSolverId()) {
					s.addConfiguration(c);
					return s;
				}
			}
			
			return null; //The solver/config pair was invalid.
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	/**
	 * Returns a list of solvers owned by a given user
	 * 
	 * @param userId the id of the user who is the owner of the solvers we are to retrieve
	 * @return a list of solvers owned by a given user, may be empty
	 * @author Todd Elvers
	 */
	public static List<Solver> getByOwner(int userId) {
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetSolversByOwner(?)}");
			procedure.setInt(1, userId);					
			 results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			
			while(results.next()){
				// Build solver object
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));
				s.setPath(results.getString("path"));
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				s.setDiskSize(results.getLong("disk_size"));
				
				// Add solver object to list
				solvers.add(s);
			}			
			
			log.debug(String.format("%d solvers were returned as being owned by user %d.", solvers.size(), userId));
			
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		log.debug(String.format("Getting the solvers owned by user %d failed.", userId));
		return null;
	}
	
	/**
	 * @param spaceId The id of the space to get solvers for
	 * @return A list of all solvers belonging directly to the space
	 * @author Tyler Jensen
	 */
	public static List<Solver> getBySpace(int spaceId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetSpaceSolversById(?)}");
			procedure.setInt(1, spaceId);					
			results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));				
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				s.setDiskSize(results.getLong("disk_size"));
				s.setPath(results.getString("path"));
				s.setUserId(results.getInt("user_id"));
				solvers.add(s);
			}			
						
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	/**
	 * @param spaceId The id of the space to get solvers for
	 * @return A list of all solvers belonging directly to the space, including their configurations
	 * @author Tyler Jensen
	 */
	public static List<Solver> getBySpaceDetailed(int spaceId) {		
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			List<Solver> solvers = Solvers.getBySpace(spaceId);
			
			for(Solver s : solvers) {
				s.getConfigurations().addAll(Solvers.getConfigsForSolver(s.getId()));
			}
						
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}	
	
	
	/**
	 * Gets a list of all the unique solvers in the space hierarchy rooted
	 * at the given space.
	 * @param spaceId The root space of the hierarchy in question
	 * @return A list of all the solvers associated with any space in the current hierarchy.
	 * Duplicates are filtered out by solver id
	 * @author Eric Burns
	 */
	public static List<Solver> getBySpaceHierarchy(int spaceId, int userId) {
		List<Solver> solvers=new ArrayList<Solver>();
		solvers.addAll(Solvers.getBySpace(spaceId));
		List<Space> spaceIds=Spaces.getSubSpaceHierarchy(spaceId, userId);
		for (Space s: spaceIds) {
			solvers.addAll(Solvers.getBySpace(s.getId()));
		}
		List<Solver> filteredSolvers=new ArrayList<Solver>();
		HashSet<Integer> ids=new HashSet<Integer>();
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
	 * @param solverId The solver id to get configurations for
	 * @return A list of configurations that belong to the solver
	 * @author Tyler Jensen
	 */
	public static List<Configuration> getConfigsForSolver(int solverId) {
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetConfigsForSolver(?)}");
			procedure.setInt(1, solverId);					
			 results = procedure.executeQuery();
			List<Configuration> configs = new LinkedList<Configuration>();
			
			while(results.next()){
				Configuration c = new Configuration();
				c.setId(results.getInt("id"));
				c.setName(results.getString("name"));
				c.setSolverId(results.getInt("solver_id"));
				c.setDescription(results.getString("description"));
				configs.add(c);
			}			
						
			return configs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;		
	}
	
	public static boolean isTestSolver(int solverId) {
		return Users.isTestUser(Solvers.get(solverId).getUserId());
	}

	
	/**
	 * Gets a particular Configuration on a connection
	 * @param con The connection to query with
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen
	 */
	protected static Configuration getConfiguration(Connection con, int configId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		 
		try {
			procedure = con.prepareCall("{CALL GetConfiguration(?)}");	
			procedure.setInt(1, configId);					
			 results = procedure.executeQuery();
			if(results.next()){
				Configuration c = new Configuration();
				c.setId(results.getInt("id"));			
				c.setName(results.getString("name"));			
				c.setSolverId(results.getInt("solver_id"));
				c.setDescription(results.getString("description"));
				Common.safeClose(results);
				return c;
			}	
		} catch (Exception e) {
			log.error("getConfiguration says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			
		}
									
				
		return null;
	}
	
	/**
	 * Gets a particular Configuration
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen
	 */
	public static Configuration getConfiguration(int configId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();			
			return Solvers.getConfiguration(con, configId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets the number of Solvers in a given space
	 * 
	 * @param spaceId the id of the space to count the Solvers in
	 * @return the number of Solvers
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetSolverCountInSpace(?)}");
			procedure.setInt(1, spaceId);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
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
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetSolverCountInSpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2,query);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}
	
	
	/**
	 *  A method for the public job page.  Gets a default configuration for a solver
	 *  and returns a singleton List with its id.  A default configuration is a configuration
	 *  named "default" if it exists, or simply the first configuration if it doesn't.
	 * @param solverId The solver id to get configurations for
	 * @return A list with the default configuration Id for the solver 
	 * @author Benton McCune
	 */
	public static List<Integer> getDefaultConfigForSolver(int solverId){
		List<Configuration> allConfigs = getConfigsForSolver(solverId);
		List<Integer> defaultConfigList = new LinkedList<Integer>();
		if (allConfigs!=null && allConfigs.size()>0){
		Integer defaultConfig = allConfigs.get(0).getId();
		for (Configuration c: allConfigs)
		{
			log.info("Configuration Name = " + c.getName() + ", id = " + c.getId());
			if (c.getName().equals("default")){
					defaultConfig = c.getId();
					break;
			}
		}
		log.info("default config has id " + defaultConfig);
		defaultConfigList.add(defaultConfig);
		}
		return defaultConfigList;
	}
	
	public static Solver getIncludeDeleted(int solverId) {
		return get(solverId,true);
	}
	
	/**
	 * @return a list of all solvers that reside in a public space
	 * @author Benton McCune
	 */
	public static List<Solver> getPublicSolvers(){
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetPublicSolvers()}");				
			 results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));				
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				s.setDiskSize(results.getLong("disk_size"));
				s.setPath(results.getString("path"));
				solvers.add(s);
			}									
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * @return a list of all solvers that reside in a public space
	 * @author Benton McCune
	 */
	public static List<Solver> getPublicSolversByCommunity(Integer commId){
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetPublicSolversByCommunity(?,?)}");	
			procedure.setInt(1, commId);
			procedure.setInt(2, R.PUBLIC_USER_ID);
			 results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));				
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				s.setDiskSize(results.getLong("disk_size"));
				s.setPath(results.getString("path"));
				solvers.add(s);
			}									
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	public static int getRecycledSolverCountByUser(int userId) {
		return getRecycledSolverCountByUser(userId,"");
	}
	
	/**
	 * Gets the number of recycled solvers a user has that match the given query
	 * @param userId The ID of the user in question
	 * @param query The string query to match on
	 * @return The number of solvers, or -1 on failure
	 * @author Eric Burns
	 */
	
	public static int getRecycledSolverCountByUser(int userId,String query) {
		Connection con=null;
		ResultSet results=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("CALL GetRecycledSolverCountByUser(?,?)");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results=procedure.executeQuery();
			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error("getRecycledSolverCountByUser says "+e.getMessage(),e);
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
	 * @return A solver object representing the solver that contains the given configuration, or null
	 * if the solver does not exist
	 * @author Tyler Jensen
	 */
	protected static Solver getSolverByConfig(Connection con, int configId, boolean includeDeleted) throws Exception {		
		Configuration c = Solvers.getConfiguration(con, configId);
		if (c==null) {
			log.debug("getSolverByConfig called with configId = "+configId+" but config was null");
			return null;
		}
		Solver s;
		if (includeDeleted) {
			s=Solvers.getIncludeDeleted(c.getSolverId());
		} else {
			s=Solvers.get(c.getSolverId());
		}
		if (s==null) {
			return null;
		}
		s.setMostRecentUpdate(Solvers.getMostRecentTimestamp(s.getId()));
		s.addConfiguration(c);
		return s;
	}
	
	/**
	 * @param configId The id of the configuration to retrieve the owning solver for
	 * @return A solver object representing the solver that contains the given configuration
	 * @author Tyler Jensen
	 */
	public static Solver getSolverByConfig(int configId, boolean includeDeleted){			
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			return Solvers.getSolverByConfig(con, configId, includeDeleted);		
		} catch (Exception e){			
			log.error("getSolverByConfig says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;		
	}
	
	/**
	 * Get the total count of the solvers belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return The count of the solvers
	 * @author Wyatt Kaiser
	 */
	public static int getSolverCountByUser(int userId) {
		Connection con = null;
		ResultSet results=null;
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
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;		
	}
	
	/**
	 * Get the total count of the solvers belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return The count of the solvers
	 * @author Wyatt Kaiser
	 */
	public static int getSolverCountByUser(int userId, String query) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetSolverCountByUserWithQuery(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2,query);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("solverCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;		
	}

	/**
	 * Get next page of the solvers belong to a specific user
	 * @param startingRecord specifies the number of the entry where should the query start
	 * @param recordsPerPage specifies how many records are going to be on one page
	 * @param isSortedASC specifies whether the sorting is in ascending order
	 * @param indexOfColumnSortedBy specifies which column the sorting is applied
	 * @param searchQuery the search query provided by the client
	 * @param userId Id of the user we are looking for
	 * @return a list of Solvers belong to the user
	 * @author Wyatt Kaiser + Eric Burns
	 */
	public static List<Solver> getSolversByUserForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int userId, boolean recycled) {
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfUserSolvers(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, userId);
			procedure.setString(6, searchQuery);
			procedure.setBoolean(7, recycled);
				
			 results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			// Only get the necessary information to display this solver
			// in a row in a DataTable object, nothing more.
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));	
				if (results.getBoolean("deleted")) {
					s.setName(s.getName()+" (deleted)");
				}
				s.setDeleted(results.getBoolean("deleted"));
				s.setRecycled(results.getBoolean("recycled"));
				s.setUserId(results.getInt("user_id"));
				s.setDescription(results.getString("description"));
				solvers.add(s);	
			}	
			
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	
	
	public static List<Solver> getSolversForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int spaceId) {
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfSolvers(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, spaceId);
			procedure.setString(6, searchQuery);
				
			 results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			// Only get the necessary information to display this solver
			// in a row in a DataTable object, nothing more.
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));	
				if (results.getBoolean("deleted")) {
					s.setName(s.getName()+" (deleted)");
				} else if (results.getBoolean("recycled")) {
					s.setName(s.getName()+" (recycled)");
				}
				s.setDeleted(results.getBoolean("deleted"));
				s.setRecycled(results.getBoolean("recycled"));
				s.setUserId(results.getInt("user_id"));
				s.setDescription(results.getString("description"));
				solvers.add(s);	
			}	
			
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	/**
	 * @param solverId The id of the solver to retrieve
	 * @param configId The id of the configuration to include with the solver
	 * @return The solver with the given configuration
	 * @author Tyler Jensen
	 */
	public static Solver getWithConfig(int solverId, int configId) {
		
		Solver s = Solvers.get(solverId);
		Configuration c = Solvers.getConfiguration(configId);
		if (s==null) {
			return null;
		}
		if(s.getId() == c.getSolverId()) {
			// Make sure this configuration actually belongs to the solver, and add it if it does
			s.addConfiguration(c);	
		}
		
		return s;
	}
	
	/**
	 * This takes in a list of solver and configuration ids. Both lists are stepped through in order
	 * and paired up. For example the first solver is retrieved from the database, and the first config is retrieved
	 * and added to the solver, and so on down the list.
	 * @param solverIds A list of solvers to retrieve
	 * @param configIds A list of configurations, where each one is retrieved along with the solvers in the given order.
	 * @return A list of solvers, where each one has one configuration as specified in the configIds list
	 * @author Tyler Jensen & Skylar Stark
	 */
	public static List<Solver> getWithConfig(List<Integer> solverIds, List<Integer> configIds) {
				
		try {	
			List<Solver> solvers = new LinkedList<Solver>();
			for (int cid : configIds) {
				Solver s = Solvers.getByConfigId(cid);
				if (s != null & solverIds.contains(s.getId())) {
					solvers.add(s); //Solver/config pair was valid and selected
				}
			}
			
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		}
		
		return null;
	}

	
	/**
	 * Determines whether the solver identified by the given solver ID has an
	 * editable name
	 * @param solverId The ID of the solver in question
	 * @return -1 if the name is not editable, 0 if it is editable and the solver is
	 * associated with no spaces, and a positive integer if the name is editable and the
	 * solver is associated only with the space with the returned ID.
	 */
	public static int isNameEditable(int solverId) {
		Connection con =null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con=Common.getConnection();
			 procedure = con.prepareCall("{CALL GetSolverAssoc(?)}");
			procedure.setInt(1, solverId);
			 results=procedure.executeQuery();
			int id=-1;
			if (results.next()) {
				id=results.getInt("space_id");
			} else {
				log.debug("Solver associated with no spaces, so its name is editable");
				return 0;
			}
			
			if (results.next()) {
				log.debug("Solver is found in multiple spaces, so its name is not editable");
				return -1;
			}
			log.debug("Solver associated with one space id = "+id +" , so its name is editable");
			return id;
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}
	
	
	
	public static boolean isPublic(int solverId) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL IsSolverPublic(?)}");
			procedure.setInt(1, solverId);
			 results = procedure.executeQuery();

			if (results.next()) {
				return (results.getInt("solverPublic") > 0);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return false;
	}
	
	
	/**
	 * Returns whether a solver with the given ID is present in the database and marked "deleted"
	 * @param con The open connection to make the query on
	 * @param solverId The ID of the solver in question
	 * @return True if the solver has been deleted, false otherwise (includes the possibility of an error)
	 * 
	 */
	
	protected static boolean isSolverDeleted(Connection con, int solverId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			 procedure = con.prepareCall("{CALL IsSolverDeleted(?)}");
			procedure.setInt(1, solverId);					
			 results = procedure.executeQuery();
			boolean deleted=false;
			if (results.next()) {
				deleted=results.getBoolean("solverDeleted");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isSolverDeleted says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	/** 
	 * Determines whether the solver with the given ID exists in the database with the column "deleted" set to true
	 * @param solverId The ID of the solver in question
	 * @return True if the solver exists in the database and has the deleted flag set to true
	 */
	
	public static boolean isSolverDeleted(int solverId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			
			return isSolverDeleted(con,solverId);
		} catch (Exception e) {
			log.error("isSolverDeleted says " +e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	
	/**
	 * Returns whether a solver with the given ID is present in the database with the 
	 * "recycled" column set to true
	 * @param solverId The ID of the solver to check
	 * @param the open connection to make the SQL call on
	 * @return True if the solver exists in the database with the "recycled" column set to
	 * true, and false otherwise
	 * @author Eric Burns
	 */

	protected static boolean isSolverRecycled(Connection con, int solverId) {
		CallableStatement procedure=null;
		ResultSet results=null;

		try {
			procedure = con.prepareCall("{CALL IsSolverRecycled(?)}");
			procedure.setInt(1, solverId);					
			results = procedure.executeQuery();
			boolean deleted=false;
			if (results.next()) {
				deleted=results.getBoolean("recycled");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isSolverRecycled says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}
	
	/**
	 * Returns whether a solver with the given ID is present in the database with the 
	 * "recycled" column set to true
	 * @param solverId The ID of the solver to check
	 * @return True if the solver exists in the database with the "recycled" column set to
	 * true, and false otherwise
	 * @author Eric Burns
	 */
	public static boolean isSolverRecycled(int solverId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return isSolverRecycled(con,solverId);
		} catch (Exception e) {
			log.error("isSolverRecycled says " +e.getMessage(),e );
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	/**
	 * Sets the "recycled" flag in the database to true. Indicates the user has moved the 
	 * solver to the recycle bin, from which in can be deleted
	 * @param id the id of the solver to recycled
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean recycle(int id) {
		return setRecycledState(id,true);
	}
	
	public static boolean recycleSolversOwnedByUser(Collection<Solver> solvers, int userId) {
		boolean success=true;
		for (Solver s : solvers) {
			if (s.getUserId()==userId) {
				success=success && Solvers.recycle(s.getId());
			}
		}
		
		return success;
	}
	
	/**
	 * Sets the "recycled" flag in the database to false. Indicates the user has removed
	 * the solver from the recycle bin
	 * @param id the id of the solver to be removed from the recycle bin
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean restore(int id) {
		return setRecycledState(id,false);
	}
	/**
	 * Restores all solvers a user has that have been recycled to normal
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean restoreRecycledSolvers(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();

			Common.safeClose(procedure);
			procedure=con.prepareCall("CALL GetRecycledSolverIds(?)");
			procedure.setInt(1, userId);
			results=procedure.executeQuery();
			while (results.next()) {
				Solvers.restore(results.getInt("id"));
			}
			return true;
		} catch (Exception e) {
			log.error("restoreRecycledSolvers says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}
	
	
	/**
	 * Given a ResultSet containing solver info, returns a Solver object representing  that info
	 * @param results The resultset pointed at the row with solver information
	 * @param prefix If there is an "AS <name>" clause in the SQL query that gave this resultset,
	 * prefix should be <name>
	 * @return A Solver object
	 * @throws SQLException
	 */
	
	protected static Solver resultToSolver(ResultSet results, String prefix) throws SQLException {
		Solver s=new Solver();
		if (prefix==null || prefix=="") {
			s.setId(results.getInt("id"));
			s.setUserId(results.getInt("user_id"));
			s.setName(results.getString("name"));
			s.setUploadDate(results.getTimestamp("uploaded"));
			s.setPath(results.getString("path"));
			s.setDescription(results.getString("description"));
			s.setDownloadable(results.getBoolean("downloadable"));
			s.setDiskSize(results.getLong("disk_size"));
		} else {
			s.setId(results.getInt(prefix+".id"));
			s.setUserId(results.getInt(prefix+".user_id"));
			s.setName(results.getString(prefix+".name"));
			s.setUploadDate(results.getTimestamp(prefix+".uploaded"));
			s.setPath(results.getString(prefix+".path"));
			s.setDescription(results.getString(prefix+".description"));
			s.setDownloadable(results.getBoolean(prefix+".downloadable"));
			s.setDiskSize(results.getLong(prefix+".disk_size"));
		}
		
		
		
		return s;
	}
	
	
	/**
	 * Sets every file in a hierarchy to be executable
	 * @param rootDir the directory that we wish to have executable files in
	 * @return Boolean true if successful
	 */
	public static Boolean setHierarchyExecutable(File rootDir){
		for (File f : rootDir.listFiles()){
			f.setExecutable(true,false);
			if (f.isDirectory()){
				setHierarchyExecutable(f);
			}
		}
		return true;
	}
	
	/**
	 * Deletes all solvers a user has from disk and sets their "deleted" flag
	 * to true. Solvers should NOT be deleted from the database, as they 
	 * still may be associated with spaces
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean setRecycledSolversToDeleted(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("CALL GetRecycledSolverPaths(?)");
			procedure.setInt(1,userId);
			results=procedure.executeQuery();
			
			while (results.next()) {
				Util.safeDeleteDirectory(results.getString("path")); 
			}
			Common.safeClose(procedure);
			procedure=con.prepareCall("CALL SetRecycledSolversToDeleted(?)");
			procedure.setInt(1, userId);
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error("setRecycledSolversToDeleted says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Sets the "recycled" flag in the database to the given value. 
	 * @param id the id of the solver to recycled or restored
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean setRecycledState(int id, boolean state){
		Connection con = null;			
		CallableStatement procedure=null;
		
		try {
			Cache.invalidateSpacesAssociatedWithSolver(id);
			Cache.invalidateAndDeleteCache(id, CacheType.CACHE_SOLVER);
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL SetSolverRecycledValue(?, ?)}");
			procedure.setInt(1, id);
			procedure.setBoolean(2,state);
			procedure.executeUpdate();		
			return true;
		} catch (Exception e){		
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
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
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			
			// Try and update the configuration file's name and/or contents
			if(Solvers.updateConfigFile(configId, name)){
				
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
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.warn(String.format("Configuration [%s] failed to update properly.", name));
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
	public static boolean updateConfigFile(int configId, String newConfigName){
		try {
			if(configId < 0 || Util.isNullOrEmpty(newConfigName)){
				log.warn("The configuration file parameters to update with are invalid.");
				return false;
			}
			
			Solver s = Solvers.getSolverByConfig(configId,false);
			Configuration config = Solvers.getConfiguration(configId);
			boolean isConfigNameUnchanged = true;
			
			// Build path to old configuration file (should exist)
			File oldConfig = new File(Util.getSolverConfigPath(s.getPath(), config.getName()));
			
			// Build path to new configuration file (should not yet exist)
			File newConfig = new File(Util.getSolverConfigPath(s.getPath(), newConfigName));
			
			// If the old config and new config names are NOT the same, ensure the file pointed to by
			// the new config does not already exist on disk
			if(false == oldConfig.getName().equals(newConfig.getName())){
				isConfigNameUnchanged = false;
				if(newConfig.exists()){
					return false;
				}
			}
			
			// Rename the file if necessary
			if (!isConfigNameUnchanged){
				FileUtils.moveFile(oldConfig, newConfig);
			}
			
			
			return true;
		} catch (Exception e){
			log.error(e.getMessage(), e);
		}
		
		return false;
	}

	/**
	 * Updates the details of a solver
	 * @param id the id of the solver to update
	 * @param name the new name to apply to the solver
	 * @param description the new description to apply to the solver
	 * @param isDownloadable boolean indicating whether or not this solver is downloadable
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateDetails(int id, String name, String description, boolean isDownloadable){
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
			Cache.invalidateSpacesAssociatedWithSolver(id);
			Cache.invalidateAndDeleteCache(id, CacheType.CACHE_SOLVER);
			Cache.invalidateAndDeleteCache(id, CacheType.CACHE_SOLVER_REUPLOAD);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.debug(String.format("Solver [id=%d] failed to be updated.", id));
		return false;
	}
	/**
	 * Updates a solver's disk_size attribute on an existing transaction
	 * 
	 * @param con the database transaction to use while updating the solver's disk size
	 * @param solver the solver object containing the new disk size to set
	 * @return true iff the solver's size was successfully updated, false otherwise
	 * @author Todd Elvers
	 */
	private static void updateSolverDiskSize(Connection con, Solver s) throws Exception {
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
			log.error("updateSolverDiskSize says "+e.getMessage(),e);
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
	public static boolean updateSolverDiskSize(Solver solver){
		Connection con = null;
		try {
			con = Common.getConnection();
			Solvers.updateSolverDiskSize(con, solver);
			log.info(String.format("Configuration's parent solver '%s' has had its disk size successfully updated.", solver.getName()));
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		log.warn(String.format("Configuration's parent solver '%s' has failed to have its disk size updated.", solver.getName()));
		return false;
	}
	/**
	 * Gets the timestamp of the configuration associated with this solver
	 * that was added or updated the most recently
	 * @param solverId The ID of the solver in question
	 * @return The timestamp as a string, or null on failure
	 * @author Eric Burns
	 */
	public static String getMostRecentTimestamp(int solverId) {
		Connection  con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetMaxConfigTimestamp(?)}");
			procedure.setInt(1,solverId);
			results=procedure.executeQuery();
			if (results.next()) {
				
				Timestamp t = (results.getTimestamp("recent"));
				//timestamp doesn't like SQL's default string of zeroes, so if that is present
				//we get a null value
				if (t==null) {
					return "0000-00-00";
				}
				return t.toString();
			}
		} catch (Exception e) {
			log.error("getMostRecentTimestamp says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	public static String getDefaultSolverPath(int userId,String solverName) {
		File uniqueDir = new File(R.SOLVER_PATH, "" + userId);
		uniqueDir = new File(uniqueDir, solverName);
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		return uniqueDir.getAbsolutePath();
	}
	
}
