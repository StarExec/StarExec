package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
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
	public static Solver get(long solverId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSolverById(?)}");
			procedure.setLong(1, solverId);					
			ResultSet results = procedure.executeQuery();
			
			if(results.next()){
				Solver s = new Solver();
				s.setId(results.getLong("id"));
				s.setUserId(results.getLong("user_id"));
				s.setName(results.getString("name"));
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setPath(results.getString("path"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				return s;
			}						
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
	public static List<Solver> getBySpace(long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceSolversById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getLong("id"));
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
	public static List<Solver> getBySpaceDetailed(long spaceId) {
		
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
	public static boolean updateDetails(long id, String name, String description, boolean isDownloadable){
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateSolverDetails(?, ?, ?, ?)}");
			procedure.setLong(1, id);
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
	 * @param id the id of the solver to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(long id){
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL DeleteSolverById(?)}");
			procedure.setLong(1, id);
			
			procedure.executeUpdate();					
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
		procedure.setLong(1, c.getSolverId());
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
	public static boolean add(Solver s, long spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL AddSolver(?, ?, ?, ?, ?, ?)}");
			procedure.setLong(1, s.getUserId());
			procedure.setString(2, s.getName());
			procedure.setBoolean(3, s.isDownloadable());
			procedure.setString(4, s.getPath());
			procedure.setString(5, s.getDescription());
			procedure.registerOutParameter(6, java.sql.Types.BIGINT);
			
			procedure.executeUpdate();
			
			long solverId = procedure.getLong(6);			
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
	protected static boolean associate(Connection con, long spaceId, long solverId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddSolverAssociation(?, ?)}");
		procedure.setLong(1, spaceId);
		procedure.setLong(2, solverId);
		
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
	public static boolean associate(List<Long> solverIds, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			for(long sid : solverIds) {
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
	 * Gets a particular Configuration
	 * @param confId
	 * @return The configuration if it is found, null if it is not.
	 */
	public static Configuration getConfiguration(long confId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Gets all configurations for the given solver
	 * @param solverId The solver id to get configurations for
	 * @return A list of configurations that belong to the solver
	 */
	public static List<Configuration> getConfigsForSolver(long solverId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetConfigsForSolver(?)}");
			procedure.setLong(1, solverId);					
			ResultSet results = procedure.executeQuery();
			List<Configuration> configs = new LinkedList<Configuration>();
			
			while(results.next()){
				Configuration c = new Configuration();
				c.setId(results.getLong("id"));
				c.setName(results.getString("name"));							
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
