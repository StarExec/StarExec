package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;


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
			
			log.debug(String.format("%d user(s) were successfully removed from a community [id=%d].", userIds.size(), commId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("%d user(s) were unsuccessfully removed from a community [id=%d].", userIds.size(), commId));
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
		log.debug(String.format("%d benchmark(s) were unsuccessfully removed from a space [id=%d].", benchIds.size(), spaceId));
		return false;
	}
	
	
	
	/**
	 * Removes a list of benchmarks from a given space in an all-or-none fashion (uses an existing transaction)
	 * 
	 * @param benchIds the id's of the benchmarks to remove from a given space
	 * @param spaceId the space to remove the benchmarks from
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
			
			// If a file path was returned, add it to the list of files to be deleted 
			if(procedure.getString(3) != null){
				filesToDelete.add(new File(procedure.getString(3)));
				
			}
		}
		
		if(spaceId >= 0){
			log.debug(String.format("%d benchmark(s) were successfully removed from a space [id=%d].", benchIds.size(), spaceId));
		}
		
		// Remove files from disk
		for(File file : filesToDelete){
			if(file.delete()){
				log.debug(String.format("File [%s] was deleted at [%s] because it was no inter referenced anywhere.", file.getName(), file.getAbsolutePath()));
			}
			if(file.getParentFile().delete()){
				log.debug(String.format("Directory [%s] was deleted because it was empty.", file.getParentFile().getAbsolutePath()));
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
			
			// Remove solvers using the created transaction
			removeSolvers(solverIds, spaceId, con);
			
			// Commit changes to database
			Common.endTransaction(con);
			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("%d solver(s) were unsuccessfully removed from a space [id=%d].", solverIds.size(), spaceId));
		return false;
	}
	
	/**
	 * Removes a list of solvers from a given space in an all-or-none fashion (uses an existing transaction)
	 * 
	 * @param solverIds the id's of the solvers to remove from a given space
	 * @param spaceId the space to remove the solvers from
	 * @param con the connection containing the existing transaction
	 * @return true iff all solvers in 'solversIds' are successfully removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @throws SQLException if an error occurs while removing solvers from the database
	 * @author Todd Elvers
	 */
	protected static boolean removeSolvers(List<Integer> solverIds, int spaceId, Connection con) throws SQLException {
		CallableStatement procedure = con.prepareCall("{CALL RemoveSolverFromSpace(?, ?, ?)}");
		List<File> filesToDelete = new LinkedList<File>();
		
		for(int solverId : solverIds){
			procedure.setInt(1, solverId);
			procedure.setInt(2, spaceId);
			procedure.registerOutParameter(3, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			// If a file path was returned, add it to the list of files to be deleted 
			if(procedure.getString(3) != null){
				filesToDelete.add(new File(procedure.getString(3)));
			}
		}
		
		if(spaceId >= 0) {
			log.debug(String.format("%d solver(s) were successfully removed from a space [id=%d].", solverIds.size(), spaceId));
		}
		
		// Remove files from disk
		for(File file : filesToDelete){
			if(file.delete()){
				log.debug(String.format("File [%s] was deleted at [%s] because it was no inter referenced anywhere.", file.getName(), file.getAbsolutePath()));
			}
			if(file.getParentFile().delete()){
				log.debug(String.format("Directory [%s] was deleted because it was empty.", file.getParentFile().getAbsolutePath()));
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
			
			// Remove jobs using the created transaction
			removeJobs(jobIds, spaceId, con);
			
			// Commit changes to database
			Common.endTransaction(con);
			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("%d jobs were unsuccessfully removed from a space [id=%d].", jobIds.size(), spaceId));
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
	 * @author Todd Elvers
	 */
	protected static boolean removeJobs(List<Integer> jobIds, int spaceId, Connection con) throws SQLException {
		CallableStatement procedure = con.prepareCall("{CALL RemoveJobFromSpace(?, ?)}");
		List<Integer> benchmarks = new LinkedList<Integer>();
		List<Integer> solvers = new LinkedList<Integer>();
		
		for(int jobId : jobIds){
			// Gather the benchmarks and solvers from the jobs being removed
			List<JobPair> jobPairs = Jobs.getPairsDetailed(jobId);
			for(JobPair jp : jobPairs) {
				benchmarks.add(jp.getBench().getId());
				solvers.add(jp.getSolver().getId());
			}
			
			procedure.setInt(1, jobId);
			procedure.setInt(2, spaceId);
			
			procedure.executeUpdate();			
		}
		
		// Check the benchmarks & solvers related to this job and see if any are dangling resources
		removeBenches(benchmarks, -1, con);
		removeSolvers(solvers, -1, con);
		
		log.debug(String.format("%d jobs were successfully removed from a space [id=%d].", jobIds.size(), spaceId));
		return true;
	}
	
	
	/**
	 * Removes a list of subspaces from a given space in an all-or-none fashion (creates a transaction)
	 * 
	 * @param subspaceIds the list of subspaces to remove
	 * @param spaceId the id of the space to remove the subspaces from
	 * @return true iff all subspaces in 'subspaceIds' are removed from the space referenced by 'spaceId',
	 * false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeSubspaces(List<Integer> subspaceIds, int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			// Instantiate a transaction so subspaces in 'subspaceIds' get removed in an all-or-none fashion
			Common.beginTransaction(con);
			
			CallableStatement procedure = con.prepareCall("{CALL RemoveSubspace(?)}");
			for(int subspaceId : subspaceIds){
				// Checks the solvers, benchmarks, and jobs to see if any
				// are safe to be deleted from disk
				smartDelete(subspaceId, con);
				
				procedure.setInt(1, subspaceId);
				procedure.executeUpdate();			
			}
			
			// Commit changes to database
			Common.endTransaction(con);
			
			log.debug(String.format("%d subspaces were successfully removed from a space [id=%d].", subspaceIds.size(), spaceId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("%d subspaces were unsuccessfully removed from a space [id=%d].", subspaceIds.size(), spaceId));
		return false;
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
	 * @return A list of child spaces belonging to the parent space that the given user can see
	 * @author Tyler Jensen
	 */
	public static List<Space> getSubSpaces(int spaceId, int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSubSpacesById(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
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
						
			return subSpaces;
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
			s.setSubspaces(Spaces.getSubSpaces(spaceId, userId));
			
												
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
	 * @param parent The parent space that is the 'root' of the new subtree to be added
	 * @param userId The user that will own the new spaces and benchmarks
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addWithBenchmarks(Space parent, int userId) {
		Connection con = null;
		
		try {
			// We'll be doing everything with a single connection so we can roll back if needed
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			// For each subspace...
			for(Space s : parent.getSubspaces()) {
				// Apply the recursive algorithm to add each subspace
				Spaces.traverse(con, s, parent.getId(), userId);
			}
			
			// Add any new benchmarks in the space to the database
			Benchmarks.add(parent.getBenchmarks(), parent.getId());
			
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
	
	/**
	 * Internal recursive method that adds a space and it's benchmarks to the database
	 * @param con The connection to perform the operations on
	 * @param space The space to add to the database
	 * @param parentId The id of the parent space that the given space will belong to
	 * @param userId The user id of the owner of the new space and its benchmarks
	 * @author Tyler Jensen
	 */
	protected static void traverse(Connection con, Space space, int parentId, int userId) throws Exception {
		// Add the new space to the database and get it's ID		
		int spaceId = Spaces.add(con, space, parentId, userId);
		
		for(Space s : space.getSubspaces()) {
			// Recursively go through and add all of it's subspaces with itself as the parent
			Spaces.traverse(con, s, spaceId, userId);
		}			
		
		// Finally, add the benchmarks in the space to the database
		Benchmarks.add(con, space.getBenchmarks(), spaceId);
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
		
		// Add the new space as a child space of the parent space
		CallableStatement procSubspace = con.prepareCall("{CALL AssociateSpaces(?, ?)}");	
		procSubspace.setInt(1, parentId);
		procSubspace.setInt(2, newSpaceId);
		procSubspace.executeUpdate();		
		
		// Add the adding user to the space with the maximal permissions
		CallableStatement procAddUser = con.prepareCall("{CALL AddUserToSpace(?, ?, ?)}");			
		procAddUser.setInt(1, userId);
		procAddUser.setInt(2, newSpaceId);
		procAddUser.setInt(3, newSpaceId);				
		procAddUser.executeUpdate();
		
		// Set maximal permissions for the user who added the space	
		Permissions.set(userId, newSpaceId, new Permission(true), con);
		
		Common.endTransaction(con);
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
	 */
	protected static void smartDelete(int spaceId, Connection con) throws SQLException{
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
	

}