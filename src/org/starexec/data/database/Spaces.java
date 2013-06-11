package org.starexec.data.database;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;

/*import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
*/

/**
 * Handles all database interaction for spaces
 */
public class Spaces {
	private static final Logger log = Logger.getLogger(Spaces.class);
	
	/**
	 * Gets a space with minimal information (only details about the space itself)
	 * @param spaceId The id of the space to get information for
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space get(int spaceId) {
		Connection con = null;			
		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceById(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();		
			
			if(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));
				s.setCreated(results.getTimestamp("created"));
				s.setPublic(results.getBoolean("public_access"));
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
	 * Gets the users that are the leaders of a given space
	 * 
	 * @param spaceId the id of the space to get the leaders of
	 * @return a list of leaders of the given space
	 * @author Todd Elvers
	 */
	public static List<User> getLeaders(int spaceId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetLeadersBySpaceId(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<User> leaders = new LinkedList<User>();
			
			while(results.next()){
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
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Removes a list of users from a community in an all-or-none fashion (creates a transaction)
	 * 
	 * @param userIds the list of userIds to be removed from a given community
	 * @param commId the id of the community to remove the users from
	 * @return true iff all users in userIds were removed from the community referenced by commId,
	 * false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeUsers(List<Integer> userIds, int commId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			// Instantiate a transaction so users in 'userIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);
			
			CallableStatement procedure = con.prepareCall("{CALL LeaveCommunity(?, ?)}");
			for(int userId : userIds){
				procedure.setInt(1, userId);
				procedure.setInt(2, commId);
				
				procedure.executeUpdate();			
			}
			
			// Commit changes to database
			Common.endTransaction(con);
			
			log.info(userIds.size() + " user(s) were successfully removed from community " + commId);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.error(userIds.size() + " user(s) were unsuccessfully removed from community " + commId);
		return false;
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
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error(benchIds.size() + " benchmark(s) were unsuccessfully removed from space " + spaceId);
		return false;
	}
	
	
	
	/**
	 * Removes a list of benchmarks from a given space in an all-or-none fashion (uses an existing transaction), or 
	 * checks a list of benchmarks to see if any are safe to delete from StarExec entirely
	 * 
	 * @param benchIds the id's of the benchmarks to remove from a given space
	 * @param spaceId the space to remove the benchmarks from, if it's negative then the system will probe the database
	 * with the given list of benchmark ids and see if any aren't referenced anywhere in StarExec
	 * @param con the connection containing the existing transaction
	 * @return true iff all benchmarks in 'benchIds' are successfully removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @throws SQLException if an error occurs while removing benchmarks from the database
	 * @author Todd Elvers
	 */
	protected static boolean removeBenches(List<Integer> benchIds, int spaceId, Connection con) throws SQLException {
		CallableStatement procedure = con.prepareCall("{CALL RemoveBenchFromSpace(?, ?, ?)}");
		List<File> filesToDelete = new LinkedList<File>();
		
		for(int benchId : benchIds){
			procedure.setInt(1, benchId);
			procedure.setInt(2, spaceId);
			procedure.registerOutParameter(3, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			// If a file path was returned, add it to the list of benchmark files to be deleted 
			if(procedure.getString(3) != null){
				filesToDelete.add(new File(procedure.getString(3)));
			}
		}
		
		if(spaceId >= 0){
			log.info(benchIds.size() + " benchmark(s) were successfully removed from space " + spaceId);
		}
		
		// Remove the benchmark files from disk if they're not referenced anywhere else in StarExec
		for(File file : filesToDelete){
			if(file.delete()){
				log.info("Benchmark file [" + file.getAbsolutePath() + "] was deleted because it was no longer referenced anywhere in StarExec.");
			}
			if(file.getParentFile().delete()){
				log.info("Directory [" + file.getParentFile().getAbsolutePath() + "] was deleted because it was empty.");
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a list of solvers from a given space in an all-or-none fashion (creates a transaction)
	 * 
	 * @param solverIds the list of solverIds to remove from a given space
	 * @param spaceId the id of the space to remove the solver from
	 * @return true iff all solvers in 'solverIds' were removed from the space referenced by spaceId,
	 * false otherwise
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
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.error(solverIds.size() + " solver(s) were unsuccessfully removed from space " + spaceId);
		return false;
	}
	
	/**
	 * Removes a list of solvers from a given space in an all-or-none fashion (uses an existing transaction), or 
	 * checks a list of solvers to see if any are safe to delete from StarExec entirely
	 * 
	 * @param solverIds the id's of the solvers to remove from a given space
	 * @param spaceId the space to remove the solvers from, if it's negative then the system will probe the database
	 * with the given list of solver ids and see if any aren't referenced anywhere in StarExec
	 * @param con the connection containing the existing transaction
	 * @return true iff all solvers in 'solversIds' are successfully removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @throws SQLException if an error occurs while removing solvers from the database
	 * @throws IOException if an error occurs while removing solvers from disk 
	 * @author Todd Elvers
	 */
	protected static boolean removeSolvers(List<Integer> solverIds, int spaceId, Connection con) throws SQLException, IOException {
		CallableStatement procedure = con.prepareCall("{CALL RemoveSolverFromSpace(?, ?, ?)}");
		List<File> solverDirsOnDisk = new LinkedList<File>();
		
		for(int solverId : solverIds){
			procedure.setInt(1, solverId);
			procedure.setInt(2, spaceId);
			procedure.registerOutParameter(3, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			// If a file path was returned, add it to the list of solver directories to be deleted 
			if(procedure.getString(3) != null){
				solverDirsOnDisk.add(new File(procedure.getString(3)));
			}
		}
		
		if(spaceId >= 0) {
			log.info(solverIds.size() + " solver(s) were successfully removed from space " + spaceId);
		}
		
		// Remove Solver directories from disk
		for(File directory : solverDirsOnDisk){
			FileUtils.deleteDirectory(directory);
			log.info("Solver directory [" +  directory.getAbsolutePath() + "] was deleted because it was no longer referenced anywhere in StarExec.");
			
			// If parent directory is empty, delete it too
			if(directory.getParentFile().delete()){
				log.info("Directory [" + directory.getParentFile().getAbsolutePath() + "] was deleted because it was empty.");
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a list of jobs from a given space in an all-or-none fashion (creates a transaction)
	 * 
	 * @param jobIds the list of jobIds to remove from a given space
	 * @param spaceId the id of the space to remove the job from
	 * @return true iff all jobs in 'jobIds' were removed from the space referenced by 'spaceId',
	 * false otherwise
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
		} catch (Exception e){			
			log.error(e.getMessage(), e);
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
	 * @return true iff all jobs in 'jobIds' are successfully removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @throws SQLException if an error occurs while removing jobs from the database
	 * @throws IOException if an error occurs while removing solvers/benchmarks from disk
	 * @author Todd Elvers
	 */
	protected static boolean removeJobs(List<Integer> jobIds, int spaceId, Connection con) throws SQLException, IOException {
		CallableStatement procedure = con.prepareCall("{CALL RemoveJobFromSpace(?, ?)}");
		List<Integer> benchmarks = new LinkedList<Integer>();
		List<Integer> solvers = new LinkedList<Integer>();
		
		for(int jobId : jobIds){
			// Gather the benchmarks and solvers from the jobs being removed
			List<JobPair> jobPairs = Jobs.getPairsDetailed(jobId);
			for(JobPair jp : jobPairs) {
				if (jp != null) {
					if (jp.getBench() != null) benchmarks.add(jp.getBench().getId());
					if (jp.getSolver() != null) solvers.add(jp.getSolver().getId());
				}
			}
			
			procedure.setInt(1, jobId);
			procedure.setInt(2, spaceId);
			
			procedure.executeUpdate();			
		}
		
		// Check the benchmarks & solvers related to this job and see if any are dangling resources
		removeBenches(benchmarks, -1, con);
		removeSolvers(solvers, -1, con);
		
		log.info(jobIds.size() + " job(s) were successfully removed from space " + spaceId);
		return true;
	}
	
	
	/**
	 * Removes a list of subspaces from connection to parent
	 * 
	 * @param subspaceIds the list of subspaces to remove
	 * @param parentSpaceId the id of the space to remove the subspaces from
	 * @return true iff all subspaces in 'subspaceIds' are removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @author Ben McCune
	 */
	public static boolean quickRemoveSubspaces(List<Integer> subspaceIds, int parentSpaceId, int userId) {
		// Ensure the user can remove subspaces
		if (Permissions.checkSpaceHierRemovalPerms(subspaceIds, parentSpaceId, userId) == false){
			log.warn("Permission failure in removing spaces for user " + userId);
			return false;
		}
		else{
			log.info("Permission success in removing spaces for user " + userId);
		}
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			// Instantiate a transaction so subspaces in 'subspaceIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);
			
			// For each subspace in the list of subspaces to be deleted...
			for(int subspaceId : subspaceIds){				
				Spaces.quickRemoveSubspace(subspaceId, parentSpaceId, userId, con);
			}
			
			// Commit changes to database
			Common.endTransaction(con);
			
			log.info("quick space deletion complete.");
			
			return true;
		} catch (Exception e){			
			log.error("quick remove supbspaces says " + e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	
	/**
	 * Removes a list of subspaces from connection to parent
	 * 
	 * @param subspaceIds the list of subspaces to remove
	 * @param parentSpaceId the id of the space to remove the subspaces from
	 * @return true iff all subspaces in 'subspaceIds' are removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @author Ben McCune
	 */
	public static boolean quickRemoveSubspace(int subspaceId, int parentSpaceId, int userId, Connection con) {				
		
		try {		
				CallableStatement procedure = con.prepareCall("{CALL QuickRemoveSubspace(?,?)}");
				procedure.setInt(1, subspaceId);
				procedure.setInt(2, parentSpaceId);
				procedure.executeUpdate();		
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} 
		
		return false;
	}
	/**
	 * Removes a list of subspaces, and all of their subspaces, from a given space 
	 * in an all-or-none fashion (creates a transaction)
	 * 
	 * @param subspaceIds the list of subspaces to remove
	 * @param parentSpaceId the id of the space to remove the subspaces from
	 * @return true iff all subspaces in 'subspaceIds' are removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeSubspaces(List<Integer> subspaceIds, int parentSpaceId, int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			// Instantiate a transaction so subspaces in 'subspaceIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);
			
			//CallableStatement procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
			log.info("Beginning smart deletion...");
			
			// For each subspace in the list of subspaces to be deleted...
			for(int subspaceId : subspaceIds){
				log.debug("subspaceId = " + subspaceId);
				
				// Ensure the user can remove that subspace
				if(Permissions.get(userId, parentSpaceId).canRemoveSpace() == false){
					throw new Exception();
				}
				
				// Check if it has any subspaces itself, and if so delete them 
				Spaces.removeSubspaces(subspaceId, parentSpaceId, userId, con);
				
				// Check the primitives of this subspace - if they aren't referenced anywhere 
				// else on StarExec, delete them
				
				//temporarily commented out to narrow down bug
				Spaces.smartDelete(subspaceId, con);
				CallableStatement procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
				procedure.setInt(1, subspaceId);
				procedure.executeUpdate();
				log.info("Space " + subspaceId +  " has been deleted.");
			}
			
			// Commit changes to database
			Common.endTransaction(con);
			
			log.info("Smart deletion complete.");
			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.error("Smart deletion failed.");
		log.error(subspaceIds.size() + " subspaces were unsuccessfully removed from space " + parentSpaceId);
		return false;
	}
	
	/**
	 * Removes a list of subspaces, and all of their subspaces, from a given space 
	 * in an all-or-none fashion (uses a transaction)
	 * 
	 * @param subspaceIds the list of subspaces to remove
	 * @param parentSpaceId the id of the space to remove the subspaces from
	 * @param con the database transaction to use
	 * @author Todd Elvers
	 */
	public static void removeSubspaces(int spaceId, int parentSpaceId, int userId, Connection con) throws Exception {
		
		//CallableStatement procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
		
		// For every subspace of the space to be deleted...
		for(Space subspace : Spaces.getSubSpaces(spaceId, userId, false)){
			// Ensure the user is the leader of that space
			if(Permissions.get(userId, subspace.getId()).isLeader() == false){
				log.error("User " + userId + " does not have permission to delete space " + subspace.getId() + ".");
				throw new Exception();
			}
			
			// Recursively delete its subspaces
			Spaces.removeSubspaces(subspace.getId(), parentSpaceId, userId, con);
			
			// Checks the space's solvers, benchmarks, and jobs to see if any are safe to be deleted from disk
			Spaces.smartDelete(subspace.getId(), con);
			
			CallableStatement procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
			procedure.setInt(1, subspace.getId());
			procedure.executeUpdate();			
			
			log.info("Space " + subspace.getId() +  " has been deleted.");
		}
	}

	
	/**
	 * Get all websites associated with a given user.
	 * @param spaceId the id of the space to update
	 * @param newDesc the new description to update the space with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateDescription(int spaceId, String newDesc){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceDescription(?, ?)}");
			procedure.setInt(1, spaceId);					
			procedure.setString(2, newDesc);
			
			procedure.executeUpdate();			
			log.info(String.format("Space [%d] updated description to [%s]", spaceId, newDesc));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Gets all subspaces belonging to another space
	 * @param spaceId The id of the parent space. Give an id <= 0 to get the root space
	 * @param userId The id of the user requesting the subspaces. This is used to verify the user can see the space
	 * @param isRecursive Whether or not to find all the subspaces recursively for a given space, or just the space's subspaces
	 * @return A list of child spaces belonging to the parent space that the given user can see
	 * @author Tyler Jensen, Todd Elvers & Skylar Stark
	 */
	public static List<Space> getSubSpaces(int spaceId, int userId, boolean isRecursive) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			return Spaces.getSubSpaces(spaceId, userId, isRecursive, con);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets the parent space belong to the given spaceId
	 * @param spaceId the id of the space to get the parent of
	 * @return the id of the parent space
	 * 
	 * @author Wyatt Kaiser
	 */
	
	public static Integer getParentSpace(int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			return Spaces.getParentSpace(spaceId, con);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Helper method for getSubSpaces() - gets either the first level of subspaces of a space or, recursively, all
	 * subspaces of a given space
	 * 
	 * @param spaceId The id of the space to get the subspaces of
	 * @param userId The id of the user making the request for the subspaces
	 * @param isRecursive True if we want all subspaces of a space recursively; False if we want only the first level of subspaces of a space
	 * @param con the database connection to use
	 * @return the list of subspaces of the given space
	 * @throws Exception
	 * @author Todd Elvers & Skylar Stark & Benton McCune & Wyatt Kaiser
	 */
	protected static List<Space> getSubSpaces(int spaceId, int userId, boolean isRecursive, Connection con) throws Exception{
		CallableStatement procedure = con.prepareCall("{CALL GetSubSpacesById(?, ?, ?)}");
		procedure.setInt(1, spaceId);
		procedure.setInt(2, userId);
		procedure.setInt(3, R.PUBLIC_USER_ID);
		ResultSet results = procedure.executeQuery();
		List<Space> subSpaces = new LinkedList<Space>();
		
		while(results.next()){
			Space s = new Space();
			s.setName(results.getString("name"));
			s.setId(results.getInt("id"));
			s.setDescription(results.getString("description"));
			s.setLocked(results.getBoolean("locked"));
			subSpaces.add(s);
		}
		
		if(isRecursive){
			List<Space> additionalSubspaces = new LinkedList<Space>();
			
			for(Space s : subSpaces){
				additionalSubspaces.addAll(Spaces.getSubSpaces(s.getId(), userId, true, con));
			}
			
			log.debug("Found an additional " + additionalSubspaces.size() + " subspaces via recursion");
			subSpaces.addAll(additionalSubspaces);
		}
		log.debug("Returning from adding subspaces");
		return subSpaces;
	}
	
/**
 * Given a space id, returns the id of the parent space
 * @param spaceId the id of the space to get parent of
 * @param con the database connection to use
 * @return the Id of the parent space
 * @author Wyatt Kaiser
 */
	
	protected static Integer getParentSpace(int spaceId, Connection con) throws Exception{
		CallableStatement procedure = con.prepareCall("{CALL GetParentSpaceById(?)}");
		procedure.setInt(1, spaceId);
		ResultSet results = procedure.executeQuery();
		while(results.next()) {
			return results.getInt("MAX(ancestor)");
		}
		return 1;
	}
	
	/**
	 * Given a list of spaces a user id, removes from the list of spaces any space
	 * where the given user is not a member of.
	 * 
	 * @param userId the id of the user to check membership of
	 * @param spaces the list of spaces to check membership of
	 * @return the original list without the spaces the user is not a member of
	 * 
	 * @author Wyatt Kaiser
	 */
	public static List<Space> trimSubSpaces(int userId, List<Space> spaces) {
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
 * Given a list of spaces, creates a HashMap to map spaceId to path
 * @param userId the id of the user creating the job
 * @param spaces the list of spaces to add job pairs from
 * @param SP the hashmap the contains the mappings of id's to paths
 * @param space the 
 * 
 * @author Wyatt Kaiser
 */
	
	public static void spacePathCreate(int userId, List<Space> spaces, HashMap<Integer, String> SP, int space) {
		Iterator<Space> iter = spaces.iterator();
				
		while (iter.hasNext()) {
			Space s = iter.next();
			if (!Users.isMemberOfSpace(userId, s.getId())) {
				iter.remove();
				log.debug("removed space");
			}
			log.debug("iter.next = " + s + ", " + s.getName());
			int parent = getParentSpace(s.getId());
			SP.put(s.getId(), SP.get(parent) + File.separator + s.getName());
		}
	}
	
	/**
	 * Gets all spaces the user has access to
	 * @param userId The id of the user requesting the spaces. 
	 * @return A list of spaces the user has access to
	 * @author Benton McCune
	 */
	public static List<Space> GetSpacesByUser(int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpacesByUser(?)}");
			procedure.setInt(1, userId);
			ResultSet results = procedure.executeQuery();
			List<Space> spaces = new LinkedList<Space>();
			
			while(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));				
				spaces.add(s);
			}					
			return spaces;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets the name of a community by Id - helper method to work around permissions for this special case
	 * @param spaceId the id of the community to get the name of
	 * @return the name of the community
	 * @author Todd Elvers
	 */
	public static String getName(int spaceId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceById(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			String communityName = null;

			if(results.next()){
				communityName = results.getString("name");
			}

			return communityName;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets a space with detailed information (solvers, benchmarks, jobs and user belonging
	 * to the space are also populated (but not job pairs))
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of user requesting the space used to view details from their perspective
	 * @return A space object consisting of detailed information about the space
	 * @author Tyler Jensen
	 */
	public static Space getDetails(int spaceId, int userId) {		
		try {			
			Space s = Spaces.get(spaceId);
			s.setUsers(Spaces.getUsers(spaceId));
			s.setBenchmarks(Benchmarks.getBySpace(spaceId));
			s.setSolvers(Solvers.getBySpace(spaceId));
			s.setJobs(Jobs.getBySpace(spaceId));
			s.setSubspaces(Spaces.getSubSpaces(spaceId, userId, false));
			s.setPublic(Spaces.isPublicSpace(spaceId));
												
			return s;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
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
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceUsersById(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<User> users= new LinkedList<User>();
			
			while(results.next()){
				User u = new User();
				u.setId(results.getInt("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));				
				u.setDiskQuota(results.getLong("disk_quota"));
				users.add(u);
			}			
						
			return users;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
		
	/**
	 * Adds all subspaces and their benchmarks to the database. The first space given should be
	 * an existing space (it must have an ID that will be the ancestor space of all subspaces) and
	 * it can optionally contain NEW benchmarks to be added to the space. The method then traverses
	 * into its subspaces recursively and adds them to the database aint with their benchmarks.
	 * 
	 * @param parent The parent space that is the 'root' of the new subtree to be added
	 * @param userId The user that will own the new spaces and benchmarks
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addWithBenchmarks(Space parent, int userId, int statusId) throws Exception{
		
		log.info("adding with benchmarks and no dependencies for user " + userId);
		try {
			// We'll be doing everything with a single connection so we can roll back if needed
			
			// For each subspace...
			log.info("about to begin traversing (no deps)");
			Boolean returnValue = true;
			for(Space s : parent.getSubspaces()) {
				// Apply the recursive algorithm to add each subspace
				returnValue = returnValue && Spaces.traverse(s, parent.getId(), userId, statusId);
			}

			// Add any new benchmarks in the space to the database			
			
			if (parent.getBenchmarks().size() > 0){
				log.info("adding benchmarks in main space");
				Benchmarks.add(parent.getBenchmarks(), parent.getId(), statusId);
			}

			// We're done (notice that 'parent' is never added because it should already exist)
					
			return returnValue;
		} catch (Exception e){			
			//log.error(e.getMessage(), e);
			throw e;
			
		} 
		
	}
	
	/**
	 * Adds all subspaces and their benchmarks to the database. The first space given should be
	 * an existing space (it must have an ID that will be the ancestor space of all subspaces) and
	 * it can optionally contain NEW benchmarks to be added to the space. The method then traverses
	 * into its subspaces recursively and adds them to the database aint with their benchmarks.
	 * 
	 * @param parent The parent space that is the 'root' of the new subtree to be added
	 * @param userId The user that will own the new spaces and benchmarks
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statements
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	public static boolean addWithBenchmarksAndDeps(Space parent, int userId, Integer depRootSpaceId, boolean linked, Integer statusId) {
		Connection con = null;
		log.info("addWithBenchmarksAndDeps called on space " + parent.getName());
		try {
			// We'll be doing everything with a single connection so we can roll back if needed
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			// For each subspace...
			for(Space sub : parent.getSubspaces()) {
				// Apply the recursive algorithm to add each subspace
				Spaces.traverseWithDeps(con, sub, parent.getId(), userId, depRootSpaceId, linked, statusId);
			}
			
			// Add any new benchmarks in the space to the database
			if (parent.getBenchmarks().size()>0){
				Benchmarks.addWithDeps(parent.getBenchmarks(), parent.getId(), con, depRootSpaceId, linked, userId, statusId);
			}
			
			// We're done (notice that 'parent' is never added because it should already exist)
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
	
	
	//no connection
	protected static Boolean traverse(Space space, int parentId, int userId, int statusId) throws Exception {
		// Add the new space to the database and get it's ID		
		log.info("traversing space without deps for user " + userId);
		if (space == null)
		    log.error("traverse(): space is null.");
		Connection con = null;
		
		Boolean returnValue = true;
		try{
			con = Common.getConnection();	
			Common.beginTransaction(con);	
			int spaceId = Spaces.add(con, space, parentId, userId);
			Common.endTransaction(con);	
			
			for(Space s : space.getSubspaces()) {
				// Recursively go through and add all of it's subspaces with itself as the parent
				log.info("about to traverse space " + spaceId);
				returnValue = returnValue && Spaces.traverse(s, spaceId, userId, statusId);
			}			
		
			// Finally, add the benchmarks in the space to the database
			//not really using connection parameter right now due to problems
			Benchmarks.add(space.getBenchmarks(), spaceId, statusId);
			Uploads.incrementCompletedSpaces(statusId);		
			return returnValue;
		}
		catch (Exception e){			
			log.error("traverse says " + e.getMessage(), e);
			String message = "Major Error encountered traversing spaces";
			Uploads.setErrorMessage(statusId, message);
			return false;//need to pass up
		} finally {
			Common.safeClose(con);
		}
		
	}
	
	
	
	
	/**
	 * Internal recursive method that adds a space and it's benchmarks to the database
	 * @param con The connection to perform the operations on
	 * @param space The space to add to the database
	 * @param parentId The id of the parent space that the given space will belong to
	 * @param userId The user id of the owner of the new space and its benchmarks
	 * @author Benton McCune
	 */
	protected static void traverseWithDeps(Connection conParam, Space space, int parentId, int userId, Integer depRootSpaceId, Boolean linked, Integer statusId) throws Exception {
		Connection con = null;		
		try{

			con = Common.getConnection();	
			Common.beginTransaction(con);	
			// Add the new space to the database and get it's ID		
			int spaceId = Spaces.add(con, space, parentId, userId);
			Common.endTransaction(con);	
			
			log.info("traversing (with deps) space " + space.getName() );
			for(Space sub : space.getSubspaces()) {
				// Recursively go through and add all of it's subspaces with itself as the parent
				Spaces.traverseWithDeps(con, sub, spaceId, userId, depRootSpaceId, linked, statusId);
			}			
			// Finally, add the benchmarks in the space to the database
			Benchmarks.addWithDeps(space.getBenchmarks(), spaceId, con, depRootSpaceId, linked, userId, statusId);
			Uploads.incrementCompletedSpaces(statusId);
		}
		catch (Exception e){			
			log.error("traverseWithDeps says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
	}
	
	/**
	 * Updates the name of a space with the given space id
	 * @param spaceId the id of the space to update
	 * @param newName the new name to update the space with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateName(int spaceId, String newName){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceName(?, ?)}");
			procedure.setInt(1, spaceId);					
			procedure.setString(2, newName);
			
			procedure.executeUpdate();			
			log.info(String.format("Space [%d] updated name to [%s]", spaceId, newName));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Adds a new space to the system. This action adds the space, adds a
	 * default permission record for the space, and adds a new association
	 * to the space with the given user with full leadership permissions
	 * @param s The space to add (should have default permissions set)
	 * @param parentId The parent space s is being added to
	 * @param userId The user who is adding the space
	 * @return The ID of the newly inserted space, -1 if the operation failed
	 * @author Tyler Jensen
	 */
	public static int add(Space s, int parentId, int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			Common.beginTransaction(con);	
			
			// Add space is a multi-step process, so we need to use a transaction
			int newSpaceId = Spaces.add(con, s, parentId, userId);

			Common.endTransaction(con);			
			
			return newSpaceId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.doRollback(con);			
			Common.safeClose(con);
		}
		
		return -1;
	}
	
	/**
	 * Adds a new space to the system. This action adds the space, adds a
	 * default permission record for the space, and adds a new association
	 * to the space with the given user with full leadership permissions. NOTE:
	 * This is a multi-step process, use transactions to ensure it completes as
	 * an atomic unit.
	 * @param con The connection to perform the operation on
	 * @param s The space to add (should have default permissions set)
	 * @param parentId The parent space s is being added to
	 * @param userId The user who is adding the space
	 * @return The ID of the newly inserted space, -1 if the operation failed
	 * @author Tyler Jensen
	 */
	protected static int add(Connection con, Space s, int parentId, int userId) throws Exception {			
		// Add the default permission for the space to the database		
		int defaultPermId = Permissions.add(s.getPermission(), con);
		
		// Add the space with the default permissions
		CallableStatement procAddSpace = con.prepareCall("{CALL AddSpace(?, ?, ?, ?, ?, ?)}");	
		procAddSpace.setString(1, s.getName());
		procAddSpace.setString(2, s.getDescription());
		procAddSpace.setBoolean(3, s.isLocked());
		procAddSpace.setInt(4, defaultPermId);
		procAddSpace.setInt(5, parentId);
		procAddSpace.registerOutParameter(6, java.sql.Types.INTEGER);		
		procAddSpace.executeUpdate();
		int newSpaceId = procAddSpace.getInt(6);
		
	    log.debug("Calling AssociateSpace");
		// Add the new space as a child space of the parent space
		CallableStatement procSubspace = con.prepareCall("{CALL AssociateSpaces(?, ?)}");	
		procSubspace.setInt(1, parentId);
		procSubspace.setInt(2, newSpaceId);
		procSubspace.executeUpdate();		
		
	    log.debug("Calling AddUserToSpace");
		// Add the adding user to the space with the maximal permissions
		CallableStatement procAddUser = con.prepareCall("{CALL AddUserToSpace(?, ?, ?)}");			
		procAddUser.setInt(1, userId);
		procAddUser.setInt(2, newSpaceId);
		procAddUser.setInt(3, newSpaceId);				
		procAddUser.executeUpdate();
		
		// Set maximal permissions for the user who added the space	
		Permissions.set(userId, newSpaceId, new Permission(true), con);
		
		//Do we necessarily want to end the transaction here?  I don't think we do.
		//Common.endTransaction(con);
		log.info(String.format("New space with name [%s] added by user [%d] to space [%d]", s.getName(), userId, parentId));
		return newSpaceId;
	}
	
	
	/**
	 * Determines whether or not a given space has any descendants (i.e. if it has any subspaces)
	 * 
	 * @param spaceId the id of the space to check for descendants
	 * @return true iff the space is a leaf-space (i.e. if it has no descendants), false otherwise
	 * @author Todd Elvers
	 */
	public static boolean isLeaf(int spaceId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetDescendantsOfSpace(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			
			return !results.next();			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	

	
	/**
	 * Checks if the primitives of a space that is about to be deleted
	 * are safe to delete from disk, if so they are deleted
	 * 
	 * @param spaceId the id of the space to check for primitives that
	 * can be safely deleted from disk
	 * @param con the existing transaction to perform this query in
	 * @throws SQLException if an error occurs while removing primitives from the database
	 * @author Todd Elvers
	 * @throws IOException 
	 */
	protected static void smartDelete(int spaceId, Connection con) throws SQLException, IOException{
		List<Integer> benches = new LinkedList<Integer>();
		List<Integer> solvers = new LinkedList<Integer>();
		List<Integer> jobs = new LinkedList<Integer>();
		
		// Collect the space's benchmarks, solvers, and jobs
		for(Benchmark b : Benchmarks.getBySpace(spaceId)){
			benches.add(b.getId());
		}
		for(Solver s : Solvers.getBySpace(spaceId)){
			solvers.add(s.getId());
		}
		for(Job j : Jobs.getBySpace(spaceId)){
			jobs.add(j.getId());
		}
		
		// Remove them from the space, triggering the database to check if 
		// any of these primitives aren't referenced anywhere else and,
		// if so, deleting them
		removeJobs(jobs, spaceId, con);
		removeBenches(benches, spaceId, con);
		removeSolvers(solvers, spaceId, con);
	}

	/** Updates the details of a space in the database. The given Space object should contain 
	 * the space id of the space we are updating, as well as all the other necessary information.
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
			// Only perform this update if we have permission to
			if (Permissions.get(userId, s.getId()).isLeader()) {
				Common.beginTransaction(con);

				success = Spaces.updateDetails(s, con);
				
				Common.endTransaction(con);
			}
			
			log.info(String.format("Space with name [%s] successfully edited by user [%d].", s.getName(), userId));
			return success;		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {		
			Common.doRollback(con);			
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/** Updates the details of a space in the database. The given Space object should contain 
	 * the space id of the space we are updating, as well as all the other necessary information.
	 * This is a multi-step process, so we use transactions.
	 * 
	 * @param s the space object containing the information we are updating to
	 * @return true iff the space update is successful
	 * @author Skylar Stark
	 */
	protected static boolean updateDetails(Space s, Connection con) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceDetails(?,?,?,?,?)}");	
		
		procedure.setInt(1, s.getId());
		procedure.setString(2, s.getName());
		procedure.setString(3, s.getDescription());
		procedure.setBoolean(4, s.isLocked());
		procedure.registerOutParameter(5, java.sql.Types.INTEGER);		

		procedure.executeUpdate();

		// Get the id of the associated default permission, then update that permission
		int permId = procedure.getInt(5);
		Permissions.updatePermission(permId, s.getPermission(), con);
		
		return true;
	}
	
	
	
	/**
	 * Gets the minimal number of Spaces necessary in order to service the client's
	 * request for the next page of Spaces in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Spaces from
	 * @param recordsPerPage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)
	 * @param spaceId the id of the space to get the Spaces from
	 * @param userId the id of the user making the request; used to filter out Spaces user isn't a member of 
	 * @return a list of 10, 25, 50, or 100 Spaces containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<Space> getSpacesForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy,  String searchQuery, int spaceId, int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure;			
			
			procedure = con.prepareCall("{CALL GetNextPageOfSpaces(?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, spaceId);
			procedure.setInt(6, userId);
			procedure.setString(7, searchQuery);
			procedure.setInt(8, R.PUBLIC_USER_ID);
			
			ResultSet results = procedure.executeQuery();
			List<Space> spaces = new LinkedList<Space>();
			
			while(results.next()){
				Space s = new Space();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));
				s.setDescription(results.getString("description"));
				
				spaces.add(s);			
			}	
			
			
			return spaces;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Gets the number of Spaces in a given space
	 * 
	 * @param spaceId the id of the space to count the Spaces in
	 * @param userId the id of the user making the request
	 * @return the number of Spaces
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId, int userId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetSubspaceCountBySpaceId(?, ?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			procedure.setInt(3, R.PUBLIC_USER_ID);
			ResultSet results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("spaceCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return 0;
	}
	
	/**
	 * Gets the id of all subspaces of the given space with a particular name
	 * @param spaceId ID of the parent space
	 * @param subSpaceName Name of the subspace that is being looked for
	 * @return The id of the subspace with the given name, or -1 if none exist
	 * @author Eric Burns
	 */
	
	public static Integer getSubSpaceIDbyName(Integer spaceId,String subSpaceName) {
		return getSubSpaceIDbyName(spaceId,-1,subSpaceName);
	}
	/**
	 * returns id of subspace with a particular name (-1 if more or less than 1 found)
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
			CallableStatement procedure = con.prepareCall("{CALL GetSubSpaceByName(?,?,?)}");

			log.debug("Space ID = " + spaceId);
			procedure.setInt(1, spaceId);
			log.debug("User Id = " + userId);
			procedure.setInt(2, userId);
			log.debug("Subspace named " + subSpaceName);
			procedure.setString(3, subSpaceName);
			ResultSet results = procedure.executeQuery();
			Integer subSpaceId = -1;

			if(results.next()){
				
				subSpaceId = (results.getInt("id"));
				log.debug("SubSpace Id = " + subSpaceId);
			}	
			results.last();
			log.debug("# of subspaces named " + subSpaceName + " = " + results.getRow() );
			if (results.getRow() != 1) //should only be getting one result
			{
				return -1;
			}			
			return subSpaceId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		return -1;

	}
	
	/**
	 * Get the id of the community where the space belongs.
	 * @param id the space from which to get its community
	 * @return the id of the community of the space
	 */
	public static int GetCommunityOfSpace(int id) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetCommunityOfSpace(?)}");
			procedure.setInt(1, id);
			ResultSet results = procedure.executeQuery();
			if(results.next()) {
				return results.getInt("community");
			}
			return -1;	
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return -1;
	}

	/** 
	 * Removes users from a space and it's hierarchy. Utilizes transactions so it's all or
	 * nothing (all users from all spaces, or nothing)
	 * 
	 * @param userIds a list containing the id's of the users to remove
	 * @param subspaceIds a list containing the id's of the spaces to remove them from
	 * @return true iff all given users are removed from all given spaces
	 * 
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
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}

	/**
	 * Removes users from the space with the given spaceId
	 * 
	 * @param con the connection to perform the transaction on
	 * @param userIds a list containing the id's of the users to remove
	 * @param spaceId the id of the space to remove the users from
	 * 
	 * @author Skylar Stark
	 */
	private static void removeUsers(Connection con, List<Integer> userIds, int spaceId) throws SQLException {
		CallableStatement procedure = con.prepareCall("{CALL LeaveCommunity(?, ?)}");
		for(int userId : userIds){
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);
			
			procedure.executeUpdate();			
		}
	}

	/** 
	 * Removes solvers from a space and it's hierarchy. Utilizes transactions so it's all or
	 * nothing (all solvers from all spaces, or nothing)
	 * 
	 * @param solverIds a list containing the id's of the solvers to remove
	 * @param subspaceIds a list containing the id's of the spaces to remove them from
	 * @return true iff all given solvers are removed from all given spaces
	 * 
	 * @author Skylar Stark
	 */
	public static boolean removeSolversFromHierarchy(ArrayList<Integer> solverIds, List<Integer> subspaceIds) {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			for (int spaceId : subspaceIds) {
				Spaces.removeSolvers(solverIds, spaceId, con);
			}
			
			Common.endTransaction(con);
			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}

	/**
	 * If a space is public
	 * @param spaceId the Id of the space to be checked
	 * @return true if the space is public
	 * @author Ruoyu Zhang
	 */
	public static boolean isPublicSpace(int spaceId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL IsPublicSpace(?,?)}");
			procedure.setInt(1, spaceId);	
			procedure.setInt(2, R.PUBLIC_USER_ID);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return (results.getInt(1)>0);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Set a space to be public or private
	 * @param spaceId the Id of space to be set public or private
	 * @param pbc true denote the space to be set public, false to be set private 
	 * @return true if successful
	 * @author Ruoyu Zhang
	 */
	public static boolean setPublicSpace(int spaceId, int usrId, boolean pbc, boolean hierarchy){
		Connection con = null;			
		
		if (Permissions.get(usrId, spaceId) == null){
			return false;
		}
		else if (!Permissions.get(usrId, spaceId).isLeader()) {
			return false;
		}
		if (pbc && !hierarchy){
			Users.associate(R.PUBLIC_USER_ID, spaceId);//adds public user to space;
			Permission publicPermission = new Permission(false);
			Permissions.set(R.PUBLIC_USER_ID, spaceId, publicPermission);
		}
		if(!hierarchy){
			try {
				con = Common.getConnection();
				if(!pbc){
					List<Integer> userIds = new LinkedList<Integer>();
					userIds.add(R.PUBLIC_USER_ID);
					Spaces.removeUsers(con, userIds, spaceId);
				}
				CallableStatement procedure = con.prepareCall("{CALL setPublicSpace(?, ?)}");
				procedure.setInt(1, spaceId);
				procedure.setBoolean(2, pbc);
				procedure.executeUpdate();
			} catch (Exception e){			
				log.error(e.getMessage(), e);		
			} finally {
				Common.safeClose(con);
			}
		} else {//is hierarchy, call recursively
			List<Space> subSpaces = Spaces.getSubSpaces(spaceId, usrId, true);
			subSpaces.add(Spaces.get(spaceId));
			for (Space space : subSpaces) {
				try {				
					setPublicSpace(space.getId(), usrId, pbc, false);
				} catch (Exception e){			
					log.error(e.getMessage(), e);		
				} finally {
					Common.safeClose(con);
				}
			}
			return true;
		}
		
		return false;		
	}
	
	/**
	 * Check if there already exists a primitive has the same name as given by prim in the space.
	 * @param prim The content of the primitive, such as a job's name
	 * @param space_id The if of the space needed to be checked
	 * @param type The type of the primitive
	 *        1: solver
	 *        2: benchmark
	 *        3: job
	 *        4: subspace
	 * @return True when there exists a primitive with the same name.
	 * @author Ruoyu Zhang
	 */
	public static boolean notUniquePrimitiveName(String prim, int space_id, int type) {
		// Initiate sql connection facilities.
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		
		try {
			// If the type of the primitive is solver.
			if(type == 1) {
				con = Common.getConnection();		
				procedure = con.prepareCall("{CALL countSpaceSolversByName(?, ?)}");
				procedure.setString(1, prim);
				procedure.setInt(2, space_id);
				
				results = procedure.executeQuery();		
				
				if(results.next()){
					if(results.getInt(1) != 0) {
						return true;
					}
					return false;
				}
			}
			
			//If the type of the primitive is benchmark.
			else if(type == 2) {
				con = Common.getConnection();
				procedure = con.prepareCall("{CALL countSpaceBenchmarksByName(?, ?)}");
				procedure.setString(1, prim);
				procedure.setInt(2, space_id);
				
				results = procedure.executeQuery();		
				
				if(results.next()){
					if(results.getInt(1) != 0) {
						return true;
					}
					return false;
				}
			}
			
			//If the type of the primitive is job.
			else if(type == 3) {
				con = Common.getConnection();
				procedure = con.prepareCall("{CALL countSpaceJobsByName(?, ?)}");
				procedure.setString(1, prim);
				procedure.setInt(2, space_id);
				
				results = procedure.executeQuery();		
				
				if(results.next()){
					if(results.getInt(1) != 0) {
						return true;
					}
					return false;
				}
			}
			
			//If the type of the primitive is subspace.
			else if(type == 4) {
				con = Common.getConnection();
				procedure = con.prepareCall("{CALL countSubspacesByName(?, ?)}");
				procedure.setString(1, prim);
				procedure.setInt(2, space_id);
				
				results = procedure.executeQuery();		
				
				if(results.next()){
					if(results.getInt(1) != 0) {
						return true;
					}
					return false;
				}
			}
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return true;
	}
	
	/**
	 * Generate a chart of the execution result of the jobs of a space.
	 * @param space_id The id of the space we want to generate the result
	 * @author Ruoyu Zhang
	 */
	public static void generateResultChart(int space_id) {
		List<Job> jobsToDisplay = Jobs.getJobsForNextPage(0, 20, true, 1, "", space_id);
		
		/*
		final double[][] data = new double[jobsToDisplay.size()][1];
		for (int i = 0; i < data.length; i++) {
			data[i][0] = (double)jobsToDisplay.get(i).getLiteJobPairStats().get("totalPairs");  
        }
		      
        final CategoryDataset dataset = DatasetUtilities.createCategoryDataset("", "", data);
        */
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 0; i < jobsToDisplay.size(); i++) {
			dataset.setValue(jobsToDisplay.get(i).getLiteJobPairStats().get("totalPairs"), "Score", "Solver" + i);
        }
		
		JFreeChart chart = ChartFactory.createBarChart("Solver Score", "Solver", "Score", dataset, PlotOrientation.VERTICAL, false, true, false);

		try {
			String fileName = R.PICTURE_PATH + File.separator + "resultCharts" + File.separator + "Pic" + space_id + ".jpg";
			saveToFile(chart, fileName, 400, 300, 0.75);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a block chart according to the data given by dataset.
	 * @param dataset The original data of the chart
	 * @return A JFreeChart Object which is a block chart
	 * @author Ruoyu Zhang
	 */
	/*
	private static JFreeChart createChart(final CategoryDataset dataset) {   
	      final JFreeChart chart = ChartFactory.createBarChart(
	          "Competition Result",
	          "Solver", 
	          "Score",
	          dataset,
	          PlotOrientation.HORIZONTAL,
	          true,
	          true,
	          false
	      );
	      
	      // set the background color for the chart
	      chart.setBackgroundPaint(Color.lightGray);

	      // get a reference to the plot for further customisation
	      final CategoryPlot plot = chart.getCategoryPlot();
	      plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
	      
	      // change the auto tick unit selection to integer units only
	      final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	      rangeAxis.setRange(0.0, 300.0);
	      rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

	      return chart;
	  }
	*/
	
	/**
	 * Save a JFreeChart object into a jpg file in starexec resource directory.
	 * @param chart The JFreeChart object containing the chart
	 * @param aFileName The name of the target file to be generated
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param quality The quality of the target image, 0.75 is high, 0.5 is median, and 0.25 is low
	 * @throws FileNotFoundException 
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	public static void saveToFile(JFreeChart chart, String fileName, int width, int height, double quality) throws FileNotFoundException, IOException {
    /*	BufferedImage img = draw( chart, width, height );

    	FileOutputStream fos = new FileOutputStream(fileName);
    	JPEGImageEncoder encoder2 = JPEGCodec.createJPEGEncoder(fos);
    	JPEGEncodeParam param2 = encoder2.getDefaultJPEGEncodeParam(img);
    	param2.setQuality((float) quality, true);
    	encoder2.encode(img,param2);
    	fos.close();
    */
	}

    protected static BufferedImage draw(JFreeChart chart, int width, int height) {
    	BufferedImage img = new BufferedImage(width , height, BufferedImage.TYPE_INT_RGB);
    	Graphics2D g2 = img.createGraphics();

    	chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));

    	g2.dispose();
    	return img;
    }
}