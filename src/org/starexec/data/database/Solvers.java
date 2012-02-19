package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;

/**
 * Handles all database interaction for solvers
 */
public class Solvers {
	private static final Logger log = Logger.getLogger(Solvers.class);
	
	/**
	 * @param solverId The id of the solver to retrieve
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	public static Solver get(int solverId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			return Solvers.get(con, solverId);		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param con The connection to make the query on
	 * @param solverId The id of the solver to retrieve
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	protected static Solver get(Connection con, int solverId) throws Exception {			
		CallableStatement procedure = con.prepareCall("{CALL GetSolverById(?)}");
		procedure.setInt(1, solverId);					
		ResultSet results = procedure.executeQuery();
		
		if(results.next()){
			Solver s = new Solver();
			s.setId(results.getInt("id"));
			s.setUserId(results.getInt("user_id"));
			s.setName(results.getString("name"));
			s.setUploadDate(results.getTimestamp("uploaded"));
			s.setPath(results.getString("path"));
			s.setDescription(results.getString("description"));
			s.setDownloadable(results.getBoolean("downloadable"));
			return s;
		}								
		
		return null;
	}
	
	/**
	 * @param configId The id of the configuration to retrieve the owning solver for
	 * @return A solver object representing the solver that contains the given configuration
	 * @author Tyler Jensen
	 */
	public static Solver getSolverByConfig(int configId) throws Exception {			
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			return Solvers.getSolverByConfig(con, configId);		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;		
	}
	
	/**
	 * @param con The connection to make the query on
	 * @param configId The id of the configuration to retrieve the owning solver for
	 * @return A solver object representing the solver that contains the given configuration
	 * @author Tyler Jensen
	 */
	protected static Solver getSolverByConfig(Connection con, int configId) throws Exception {			
		Configuration c = Solvers.getConfiguration(con, configId);
		return Solvers.getWithConfig(con, c.getSolverId(), c.getId());
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
				solvers.add(Solvers.get(con, id));
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
	 * @param spaceId The id of the space to get solvers for
	 * @return A list of all solvers belonging directly to the space
	 * @author Tyler Jensen
	 */
	public static List<Solver> getBySpace(int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceSolversById(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));				
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				solvers.add(s);
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
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateSolverDetails(?, ?, ?, ?)}");
			procedure.setInt(1, id);
			procedure.setString(2, name);
			procedure.setString(3, description);
			procedure.setBoolean(4, isDownloadable);
			
			procedure.executeUpdate();						
			log.info(String.format("Solver [id=%d] was successfully updated.", id));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Solver [id=%d] failed to be updated.", id));
		return false;
	}
	
	/**
	 * Deletes a solver from the database (cascading deletes handle all dependencies)
	 * 
	 * @param id the id of the solver to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int id){
		Connection con = null;			
		File solverToDelete = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL DeleteSolverById(?, ?)}");
			procedure.setInt(1, id);
			procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			// Delete solver file from disk, and the parent directory if it's empty
			solverToDelete = new File(procedure.getString(2));
			if(solverToDelete.delete()){
				log.debug(String.format("Solver file [%s] was successfully deleted from disk at [%s].", solverToDelete.getName(), solverToDelete.getAbsolutePath()));
			}
			if(solverToDelete.getParentFile().delete()){
				log.debug(String.format("Directory [%s] was deleted because it was empty.", solverToDelete.getParentFile().getAbsolutePath()));
			}
			
			log.debug(String.format("Deletion of solver [id=%d] in directory [%s] was successful.", id, solverToDelete.getAbsolutePath()));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Deletion of solver [id=%d] failed.", id));
		return false;
	}
	
	/**
	 * Adds a run configuration to the database
	 * @param con the database connection associated with the whole process of adding the solver
	 * @param c the configuration we are adding
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	protected static boolean addConfiguration(Connection con, Configuration c) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddConfiguration(?, ?)}");
		procedure.setInt(1, c.getSolverId());
		procedure.setString(2, c.getName());
		
		procedure.executeUpdate();		
		return true;		
	}
	
	/**
	 * Adds a solver to the database. Solver association to a space and run configurations
	 * for the solver are also added. Every insert must pass for any of them to be added.
	 * @param s the Solver to be added
	 * @param spaceId the ID of the space we associate this solver with
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean add(Solver s, int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL AddSolver(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, s.getUserId());
			procedure.setString(2, s.getName());
			procedure.setBoolean(3, s.isDownloadable());
			procedure.setString(4, s.getPath());
			procedure.setString(5, s.getDescription());
			procedure.registerOutParameter(6, java.sql.Types.INTEGER);
			
			procedure.executeUpdate();
			
			int solverId = procedure.getInt(6);			
			Solvers.associate(con, spaceId, solverId);
			
			for (Configuration c : s.getConfigurations()) {
				c.setSolverId(solverId);
				addConfiguration(con, c);
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
	 * Adds a Space/Solver association
	 * @param con the database connection associated with the whole process of adding the solver
	 * @param spaceId the ID of the space we are making the association to
	 * @param solverId the ID of the solver we are associating to the space
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	protected static boolean associate(Connection con, int spaceId, int solverId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddSolverAssociation(?, ?)}");
		procedure.setInt(1, spaceId);
		procedure.setInt(2, solverId);
		
		procedure.executeUpdate();		
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
			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}

	/**
	 * Gets a particular Configuration on a connection
	 * @param con The connection to query with
	 * @param configId The id of the configuration to retrieve
	 * @return The configuration with the given id
	 * @author Tyler Jensen
	 */
	protected static Configuration getConfiguration(Connection con, int configId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL GetConfiguration(?)}");
		procedure.setInt(1, configId);					
		ResultSet results = procedure.executeQuery();
		
		if(results.next()){
			Configuration c = new Configuration();
			c.setId(results.getInt("id"));			
			c.setName(results.getString("name"));			
			c.setSolverId(results.getInt("solver_id"));
			return c;
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
	 * This takes in a list of solver and configuration ids. Both lists are stepped through in order
	 * and paired up. For example the first solver is retrieved from the database, and the first config is retrieved
	 * and added to the solver, and so on down the list.
	 * @param solverIds A list of solvers to retrieve
	 * @param configIds A list of configurations, where each one is retrieved along with the solvers in the given order.
	 * @return A list of solvers, where each one has one configuration as specified in the configIds list
	 * @author Tyler Jensen
	 */
	public static List<Solver> getWithConfig(List<Integer> solverIds, List<Integer> configIds) {
		if(solverIds.size() != configIds.size()) {
			log.error(String.format("Logical error: expected to pair solvers and configurations but received %d solvers and %d configurations", solverIds.size(), configIds.size()));
			return null;
		}
		
		Connection con = null;		
		try {
			con = Common.getConnection();			
			List<Solver> solvers = new LinkedList<Solver>();
			
			for(int i = 0; i < solverIds.size(); i++) {
				Solver s = Solvers.getWithConfig(con, solverIds.get(i), configIds.get(i));
				solvers.add(s);
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
	 * @param solverId The id of the solver to retrieve
	 * @param configId The id of the configuration to include with the solver
	 * @return The solver with the given configuration
	 * @author Tyler Jensen
	 */
	public static Solver getWithConfig(int solverId, int configId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();			
			return Solvers.getWithConfig(con, solverId, configId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param con The connection to make the queries on
	 * @param solverId The id of the solver to retrieve
	 * @param configId The id of the configuration to include with the solver
	 * @return The solver with the given configuration
	 * @author Tyler Jensen
	 */
	protected static Solver getWithConfig(Connection con, int solverId, int configId) throws Exception {			
		Solver s = Solvers.get(con, solverId);
		Configuration c = Solvers.getConfiguration(con, configId);
		
		if(s.getId() == c.getSolverId()) {
			// Make sure this configuration actually belongs to the solver, and add it if it does
			s.addConfiguration(c);	
		}
		
		return s;
	}
	
	/**
	 * Gets all configurations for the given solver
	 * @param solverId The solver id to get configurations for
	 * @return A list of configurations that belong to the solver
	 * @author Tyler Jensen
	 */
	public static List<Configuration> getConfigsForSolver(int solverId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetConfigsForSolver(?)}");
			procedure.setInt(1, solverId);					
			ResultSet results = procedure.executeQuery();
			List<Configuration> configs = new LinkedList<Configuration>();
			
			while(results.next()){
				Configuration c = new Configuration();
				c.setId(results.getInt("id"));
				c.setName(results.getString("name"));
				c.setSolverId(results.getInt("solver_id"));
				configs.add(c);
			}			
						
			return configs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;		
	}
}
