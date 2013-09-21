package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.User;
import org.starexec.util.Hash;

/**
 * Handles all database interaction for users
 */
public class Users {
	private static final Logger log = Logger.getLogger(Users.class);
		
	/**
	 * Associates a user with a space (i.e. adds the user to the space)
	 * @param con The connection to perform the database operation on
	 * @param userId The id of the user to add to the space
	 * @param spaceId The space to add the user to
	 * @param permId The permissions the user should have on the space
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static boolean associate(Connection con, int userId, int spaceId) throws Exception {
		CallableStatement procedure= null;
		try {
			procedure = con.prepareCall("{CALL AddUserToSpace(?, ?)}");			
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);
			

			procedure.executeUpdate();						
			log.info(String.format("User [%d] added to space [%d]", userId, spaceId));	
			return true;
		} catch (Exception e) {
			log.error("Users.associate says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Associates a user with a space (i.e. adds the user to the space)
	 * @param userId The id of the user to add to the space
	 * @param spaceId The space to add the user to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(int userId, int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			Users.associate(con, userId, spaceId);
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Associates a group of users user with a space (i.e. adds the user to the space)
	 * @param userIds The id's of the users to add to the space
	 * @param spaceId The space to add the users to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> userIds, int spaceId) {
		List<Integer> space=new ArrayList<Integer>();
		return associate(userIds,space);
	}
	
	/**
	 * Adds an association between a list of users and a space
	 * 
	 * @param con the database transaction to use
	 * @param userIds the ids of the users to add to a space
	 * @param spaceId the id of the space to add the users to
	 * @return true iff all users in userIds are successfully 
	 * added to the space represented by spaceId, false otherwise
	 * @throws Exception
	 * @author Todd Elvers
	 */
	protected static boolean associate(Connection con, List<Integer> userIds, int spaceId) throws Exception {
		for(int uid : userIds) {
			Users.associate(con, uid, spaceId);
		}
		return true;
	}
	
	/**
	 * Adds an association between a list of users and a list of spaces, in an all-or-none fashion
	 * 
	 * @param userIds the ids of the users to add to the spaces
	 * @param spaceIds the ids of the spaces to add the users to
	 * @return true iff all spaces in spaceIds successfully have all 
	 * users in userIds add to them, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean associate(List<Integer> userIds, List<Integer> spaceIds) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			// For each space id in spaceIds, add all the users to it
			for(int spaceId : spaceIds) {
				Users.associate(con, userIds, spaceId);
			}
			
			log.info("Successfully added users " + userIds.toString() + " to spaces " + spaceIds.toString());
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error("Failed to add users " + userIds.toString() + " to spaces " + spaceIds.toString());
		return false;
	}
	
	
	/**
	 * Retrieves a user from the database given the email address
	 * @param email The email of the user to retrieve
	 * @return The user object associated with the user
	 * @author Tyler Jensen
	 */
	public static User get(String email){
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetUserByEmail(?)}");
			procedure.setString(1, email);					
			results = procedure.executeQuery();
			
			if(results.next()){
				User u = new User();
				u.setId(results.getInt("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				u.setRole(results.getString("role"));
				u.setDiskQuota(results.getLong("disk_quota"));
				return u;
			}			
			
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
	 * Adds the specified user to the database. This method will hash the 
	 * user's password for them, so it must be supplied in plaintext.
	 * 
	 * @param user The user to add
	 * @param communityId the id of the community to add this user wants to join
	 * @param message the message from the user to the leaders of a community
	 * @param code the unique code to add to the database for this user
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean register(User user, int communityId, String code, String message){
		Connection con = null;
		CallableStatement procedure= null;
		try{
			con = Common.getConnection();					
			Common.beginTransaction(con);
			
			String hashedPass = Hash.hashPassword(user.getPassword());
			
			 procedure = con.prepareCall("{CALL AddUser(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, user.getFirstName());
			procedure.setString(2, user.getLastName());
			procedure.setString(3, user.getEmail());
			procedure.setString(4, user.getInstitution());
			procedure.setString(5, hashedPass);
			procedure.setLong(6, R.DEFAULT_USER_QUOTA);
			
			// Register output of ID the user is inserted under
			procedure.registerOutParameter(7, java.sql.Types.INTEGER);
			
			// Add user to the users table and check to be sure 1 row was modified
			procedure.executeUpdate();			
			
			// Extract id from OUT parameter
			user.setId(procedure.getInt(7));
			
			boolean successfulRegistration = false;
			
			// Add unique activation code to VERIFY table under new user's id
			if(Requests.addActivationCode(con, user, code)){
				// Add user's request to join a community to COMMUNITY_REQUESTS
				successfulRegistration = Requests.addCommunityRequest(con, user, communityId, code, message);
			}
			
			if(successfulRegistration){
				Common.endTransaction(con);
				log.info(String.format("New user [%s] successfully registered", user));
				return true;				
			} else {
				Common.doRollback(con);				
				log.info(String.format("New user [%s] failed to register", user));
				return false;
			}
		} catch (Exception e){	
			log.error(e.getMessage(), e);
			Common.doRollback(con);						
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}
	
	/**
	 * Retrieves an unregistered user from the database given their user_id.
	 * This is a helper method for user registration and shouldn't be used
	 * anywhere else.
	 * @param id The id of the unregistered user to retrieve
	 * @return the unregistered User object if one exists, null otherwise
	 * @author Todd Elvers
	 */
	public static User getUnregistered(int id) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetUnregisteredUserById(?)}");
			procedure.setInt(1, id);
			 results = procedure.executeQuery();
			
			if (results.next()) {
				User u = new User();
				u.setId(results.getInt("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				u.setDiskQuota(results.getLong("disk_quota"));
				return u;
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	/**
	 * Retrieves a user from the database given the user's id
	 * @param id the id of the user to get
	 * @return The user object associated with the user
	 * @author Tyler Jensen
	 */
	public static User get(int id){
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetUserById(?)}");
			procedure.setInt(1, id);					
			 results = procedure.executeQuery();
			
			if(results.next()){
				User u = new User();
				u.setId(results.getInt("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				u.setRole(results.getString("role"));
				u.setDiskQuota(results.getLong("disk_quota"));
				return u;
			}			
			
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
	 * Returns the (hashed) password of the given user.
	 * @param userId the user ID of the user to get the password of
	 * @return The (hashed) password for the user
	 * @author Skylar Stark
	 */
	public static String getPassword(int userId) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetPasswordById(?)}");
			procedure.setInt(1, userId);
			 results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getString("password");
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
	 * Updates the first name of a user in the database with the 
	 * given user ID
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the first name will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updateFirstName(int userId, String newValue){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL UpdateFirstName(?, ?)}");
			procedure.setInt(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();						
			log.info(String.format("User [%d] updated first name to [%s]", userId, newValue));
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
	 * Updates the last name of a user in the database with the 
	 * given user ID
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the last name will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
	public static boolean updateLastName(int userId, String newValue){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL UpdateLastName(?, ?)}");
			procedure.setInt(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated last name to [%s]", userId, newValue));
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
	 * Updates the email address of a user in the database with the 
	 * given user ID
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the email address will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updateEmail(int userId, String newValue){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL UpdateEmail(?, ?)}");
			procedure.setInt(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated e-mail address to [%s]", userId, newValue));
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
	 * Updates the institution of a user in the database with the 
	 * given user ID
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the institution will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updateInstitution(int userId, String newValue){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL UpdateInstitution(?, ?)}");
			procedure.setInt(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated institution to [%s]", userId, newValue));
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
	 * Sets a new disk quota for a given user (input should always be bytes)
	 * 
	 * @param userId the user to set the new disk quota for
	 * @param newDiskQuota the new disk quota, in bytes, to set for the given user
	 * @return true iff the new disk quota is successfully set, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean setDiskQuota(int userId, long newDiskQuota) {
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL UpdateUserDiskQuota(?, ?)}");
			procedure.setInt(1, userId);					
			procedure.setLong(2, newDiskQuota);
			
			procedure.executeUpdate();	
			
			log.info(String.format("Disk quota changed to [%s] for user [%d]", FileUtils.byteCountToDisplaySize(newDiskQuota), userId));
			
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		
		log.warn(String.format("Failed to change disk quota to [%s] for user [%d]", FileUtils.byteCountToDisplaySize(newDiskQuota), userId));
		return false;
	}

	/**
	 * Gets the number of bytes a user is consuming on disk
	 * 
	 * @param userId the id of the user to get the disk usage of
	 * @return the disk usage of the given user
	 * @author Todd Elvers
	 */
	public static long getDiskUsage(int userId) {
		Connection con = null;
		long solverUsage=0;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetUserSolverDiskUsage(?)}");
			procedure.setInt(1, userId);

			results = procedure.executeQuery();
			while(results.next()){
				solverUsage=results.getLong("disk_usage");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		con = null;
		
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetUserBenchmarkDiskUsage(?)}");
			procedure.setInt(1, userId);

			 results = procedure.executeQuery();
			while(results.next()){
				return solverUsage+results.getLong("disk_usage");
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
	 * Updates the password of a user in the database with the 
	 * given user ID. Hashes the password before updating, so
	 * the password should be supplied in plain-text.
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the password will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updatePassword(int userId, String newValue){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL UpdatePassword(?, ?)}");
			procedure.setInt(1, userId);
			String hashedPassword = Hash.hashPassword(newValue);
			procedure.setString(2, hashedPassword);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated password", userId));
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
	 * Sets the password of a user, given their user_id; this method requires
	 * the password be supplied in plaintext
	 * @param user_id the id of the user to set the new password for
	 * @param password the new password
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean setPassword(int user_id, String password){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL SetPasswordByUserId(?, ?)}");
			procedure.setInt(1, user_id);
			procedure.setString(2, Hash.hashPassword(password));
			
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
	 * Checks if a given user is a member of a particular space
	 * 
	 * @param userId the id of the user to check for membership in a particular space
	 * @param spaceId the id of the space to check for a given user's membership
	 * @return true iff the given user is a member of the given space, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean isMemberOfSpace(int userId, int spaceId){
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL IsMemberOfSpace(?, ?)}");
			procedure.setInt(1, userId);					
			procedure.setInt(2, spaceId);
			 results = procedure.executeQuery();
			return results.next();
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return false;
	}
	
	/**Gets the minimal number of Users necessary in order to service the client's 
	 * request for the next page of Users in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Users from
	 * @param recordesPerpage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)	 
	 * 
	 * @return a list of 10, 25, 50, or 100 Users containing the minimal amount of data necessary
	 * @author Wyatt Kaiser
	 **/
	public static List<User> getUsersForNextPageAdmin(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfUsersAdmin(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setString(5, searchQuery);
			procedure.setInt(6, R.PUBLIC_USER_ID);
			results = procedure.executeQuery();
			List<User> users = new LinkedList<User>();
			
			while(results.next()){
				User u = new User();
				u.setId(results.getInt("id"));
				u.setInstitution(results.getString("institution"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setEmail(results.getString("email"));
				
				//Prevents public user from appearing in table.
				users.add(u);
				
							
			}	
			
			return users;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	

	/**
	 * Gets the minimal number of Users necessary in order to service the client's
	 * request for the next page of Users in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Users from
	 * @param recordsPerPage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)
	 * @param spaceId the id of the space to get the Users from
	 * @return a list of 10, 25, 50, or 100 Users containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<User> getUsersForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy,  String searchQuery, int spaceId) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfUsers(?, ?, ?, ?, ?, ?,?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, spaceId);
			procedure.setString(6, searchQuery);
			procedure.setInt(7, R.PUBLIC_USER_ID);
			results = procedure.executeQuery();
			List<User> users = new LinkedList<User>();
			
			while(results.next()){
				User u = new User();
				u.setId(results.getInt("id"));
				u.setInstitution(results.getString("institution"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setEmail(results.getString("email"));
				
				//Prevents public user from appearing in table.
				users.add(u);
				
							
			}	
			
			return users;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	/**
	 * Gets the number of Users in the whole system
	 * 
	 * @author Wyatt Kaiser
	 */
	
	public static int getCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserCount()}");
			results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getInt("userCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}
	/**
	 * Gets the number of Users in a given space
	 * 
	 * @param spaceId the id of the space to count the Users in
	 * @return the number of Users
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetUserCountInSpace(?)}");
			procedure.setInt(1, spaceId);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("userCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;
	}
	
	/**
	 * Gets the number of Users in a given space that match a given query
	 * 
	 * @param spaceId the id of the space to count the Users in
	 * @param query The query to match the users against
	 * @return the number of Users
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, String query) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetUserCountInSpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2,query);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("userCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;
	}
	
	/**
	 * Returns true if a user is already registered with a given e-mail
	 * @param email - the email of the user to search for
	 * @return true if user exists; false if no user with that e-mail
	 * 
	 * @author Wyatt Kaiser
	 */

	public static boolean getUserByEmail(String email) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetUserByEmail(?)}");
			procedure.setString(1,email);
			 results = procedure.executeQuery();
			
			if (results.next()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error (e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}
	/**
	 * 
	 * @param jobId the job id to get the user for
	 * @return the user/owner of the job
	 * @author Wyatt Kaiser
	 */
	public static User getUserByJob(int jobId) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetNameofUserByJob(?)}");
			procedure.setInt(1, jobId);
			 results = procedure.executeQuery();
			while (results.next()) {
				User u = new User();
				u.setId(results.getInt("id"));
				u.setInstitution(results.getString("institution"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setEmail(results.getString("email"));
				return u;
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

	public static List<User> getAdmins() {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAdmins()}");
			results = procedure.executeQuery();
			
			List<User> admins =  new LinkedList<User>();
			while (results.next()) {
				User u = new User();
				u.setId(results.getInt("id"));
				u.setInstitution(results.getString("institution"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setEmail(results.getString("email"));
				admins.add(u);
			}
			return admins;
				
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
}
