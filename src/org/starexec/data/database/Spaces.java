package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Permission;
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
	public static Space get(long spaceId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();		
			
			if(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getLong("id"));
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
	 * @param spaceId the id of the space to get the leaders of
	 * @return a list of leaders of the given space
	 * @author Todd Elvers
	 */
	public static List<User> getLeaders(long spaceId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetLeadersBySpaceId(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<User> leaders = new LinkedList<User>();
			
			while(results.next()){
				User u = new User();
				u.setId(results.getLong("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
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
	 * Removes a benchmark's association from a given space, thereby removing the benchmark
	 * from the space
	 * @param benchId the id of the benchmark to remove
	 * @param spaceId the id of the space to remove the benchmark from
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeBench(long benchId, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL RemoveBenchFromSpace(?, ?)}");
			procedure.setLong(1, benchId);
			procedure.setLong(2, spaceId);
			
			procedure.executeUpdate();			
			log.debug(String.format("Removal of benchmark [id=%d] from space [id=%d] was successful.", benchId, spaceId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("Removal of benchmark [id=%d] from space [id=%d] failed.", benchId, spaceId));
		return false;
	}
	
	/**
	 * Removes a solver's association with a given space, thereby removing the solver
	 * from the space
	 * @param solverId the id of the solver to remove
	 * @param spaceId the id of the space to remove the solver from
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean removeSolver(long solverId, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL RemoveSolverFromSpace(?, ?)}");
			procedure.setLong(1, solverId);
			procedure.setLong(2, spaceId);
			
			procedure.executeUpdate();			
			log.debug(String.format("Removal of solver [id=%d] from space [id=%d] was successful.", solverId, spaceId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("Removal of solver [id=%d] from space [id=%d] failed.", solverId, spaceId));
		return false;
	}
	
	/**
	 * Removes a job's association with a space, thereby removing the job from
	 * the space
	 * @param jobId the id of the job to remove
	 * @param spaceId the id of the space to remove the job from
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 * @deprecated has not been tested
	 */
	public static boolean removeJob(long jobId, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL RemoveJobFromSpace(?, ?)}");
			procedure.setLong(1, jobId);
			procedure.setLong(2, spaceId);
			
			procedure.executeUpdate();			
			log.debug(String.format("Removal of job [id=%d] from space [id=%d] was successful.", jobId, spaceId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("Removal of job [id=%d] from space [id=%d] failed.", jobId, spaceId));
		return false;
	}
	
	/**
	 * Get all websites associated with a given user.
	 * @param spaceId the id of the space to update
	 * @param newDesc the new description to update the space with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateDescription(long spaceId, String newDesc){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceDescription(?, ?)}");
			procedure.setLong(1, spaceId);					
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
	public static List<Space> getSubSpaces(long spaceId, long userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSubSpacesById(?, ?)}");
			procedure.setLong(1, spaceId);
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
			List<Space> subSpaces = new LinkedList<Space>();
			
			while(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getLong("id"));
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
	public static String getName(long spaceId){
		Connection con = null;			
		
		try {
				con = Common.getConnection();		
				CallableStatement procedure = con.prepareCall("{CALL GetSpaceById(?)}");
				procedure.setLong(1, spaceId);					
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
	 * to the space are also populated)
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of user requesting the space used to view details from their perspective
	 * @return A space object consisting of detailed information about the space
	 * @author Tyler Jensen
	 */
	public static Space getDetails(long spaceId, long userId) {		
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
	public static List<User> getUsers(long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceUsersById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<User> users= new LinkedList<User>();
			
			while(results.next()){
				User u = new User();
				u.setId(results.getLong("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));				
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
	 * into its subspaces recursively and adds them to the database along with their benchmarks.
	 * @param parent The parent space that is the 'root' of the new subtree to be added
	 * @param userId The user that will own the new spaces and benchmarks
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addWithBenchmarks(Space parent, long userId) {
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
	protected static void traverse(Connection con, Space space, long parentId, long userId) throws Exception {
		// Add the new space to the database and get it's ID		
		long spaceId = Spaces.add(con, space, parentId, userId);
		
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
	public static boolean updateName(long spaceId, String newName){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceName(?, ?)}");
			procedure.setLong(1, spaceId);					
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
	public static long add(Space s, long parentId, long userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			Common.beginTransaction(con);	
			// Add space is a multi-step process, so we need to use a transaction
			long newSpaceId = Spaces.add(con, s, parentId, userId);
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
	protected static long add(Connection con, Space s, long parentId, long userId) throws Exception {			
		// Add the default permission for the space to the database			
		long defaultPermId = Permissions.add(s.getPermission(), con);
		
		// Add the space with the default permissions
		CallableStatement procAddSpace = con.prepareCall("{CALL AddSpace(?, ?, ?, ?, ?, ?)}");	
		procAddSpace.setString(1, s.getName());
		procAddSpace.setString(2, s.getDescription());
		procAddSpace.setBoolean(3, s.isLocked());
		procAddSpace.setLong(4, defaultPermId);
		procAddSpace.setLong(5, parentId);
		procAddSpace.registerOutParameter(6, java.sql.Types.BIGINT);
		
		procAddSpace.executeUpdate();
		long newSpaceId = procAddSpace.getLong(6);
		
		// Add the new space as a child space of the parent space
		CallableStatement procSubspace = con.prepareCall("{CALL AssociateSpaces(?, ?, ?)}");	
		procSubspace.setLong(1, parentId);
		procSubspace.setLong(2, newSpaceId);
		procSubspace.setLong(3, defaultPermId);			
		
		procSubspace.executeUpdate();
		
		// Add a new set of maximal permissions for the user who added the space			
		long userPermId = Permissions.add(new Permission(true), con);
		
		// Add the adding user to the space with the maximal permissions
		CallableStatement procAddUser = con.prepareCall("{CALL AddUserToSpace(?, ?, ?, ?)}");			
		procAddUser.setLong(1, userId);
		procAddUser.setLong(2, newSpaceId);
		procAddUser.setLong(3, newSpaceId);
		procAddUser.setLong(4, userPermId);
		
		procAddUser.executeUpdate();
		Common.endTransaction(con);
		
		// Log the new addition and ensure we return at least 3 affected rows (the two added permissions are self-checking)
		log.info(String.format("New space with name [%s] added by user [%d] to space [%d]", s.getName(), userId, parentId));
		return newSpaceId;
	}
}