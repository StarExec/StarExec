package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Permission;
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
	protected static boolean associate(Connection con, long userId, long spaceId, long permId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddUserToSpace(?, ?, ?, ?)}");			
		procedure.setLong(1, userId);
		procedure.setLong(2, spaceId);
		procedure.setLong(3, spaceId);
		procedure.setLong(4, permId);
			
		procedure.executeUpdate();						
		log.info(String.format("User [%d] added to space [%d]", userId, spaceId));	
		return true;
	}
	
	/**
	 * Associates a user with a space (i.e. adds the user to the space)
	 * @param userId The id of the user to add to the space
	 * @param spaceId The space to add the user to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(long userId, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Permission p = Permissions.getSpaceDefault(spaceId);			
			Users.associate(con, userId, spaceId, p.getId());
			
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
	public static boolean associate(List<Long> userIds, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Permission p = Permissions.getSpaceDefault(spaceId);
			
			Common.beginTransaction(con);					
			
			for(long user : userIds) {
				Users.associate(con, user, spaceId, p.getId());	
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
	 * Retrieves a user from the database given the email address
	 * @param email The email of the user to retrieve
	 * @return The user object associated with the user
	 * @author Tyler Jensen
	 */
	public static User get(String email){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetUserByEmail(?)}");
			procedure.setString(1, email);					
			ResultSet results = procedure.executeQuery();
			
			if(results.next()){
				User u = new User();
				u.setId(results.getLong("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				u.setRole(results.getString("role"));							
				return u;
			}			
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Adds the specified user to the database. This method will hash the 
	 * user's password for them, so it must be supplied in plaintext.
	 * @param user The user to add
	 * @param communityId the id of the community to add this user wants to join
	 * @param message the message from the user to the leaders of a community
	 * @param code the unique code to add to the database for this user
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean register(User user, long communityId, String code, String message){
		Connection con = null;
		
		try{
			con = Common.getConnection();					
			Common.beginTransaction(con);
			
			String hashedPass = Hash.hashPassword(user.getPassword());
			
			CallableStatement procedure = con.prepareCall("{CALL AddUser(?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, user.getFirstName());
			procedure.setString(2, user.getLastName());
			procedure.setString(3, user.getEmail());
			procedure.setString(4, user.getInstitution());
			procedure.setString(5, hashedPass);
			
			// Register output of ID the user is inserted under
			procedure.registerOutParameter(6, java.sql.Types.BIGINT);
			
			// Add user to the users table and check to be sure 1 row was modified
			procedure.executeUpdate();			
			
			// Extract id from OUT parameter
			user.setId(procedure.getLong(6));
			
			boolean added = false;
			
			// Add unique activation code to VERIFY database under new user's id
			if(Requests.addActivationCode(con, user, code)){
				// Add user's request to join a community to INVITES
				added = Requests.addCommunityRequest(con, user, communityId, message);
			}
			
			if(added){
				// Commit changes to database
				Common.endTransaction(con);
				
				log.info(String.format("New user [%s] successfully registered", user));
				return true;				
			} else {
				// Don't commit changes to database
				Common.doRollback(con);				
				log.info(String.format("New user [%s] failed to register", user));
				return false;
			}
		} catch (Exception e){	
			log.error(e.getMessage(), e);
			Common.doRollback(con);						
		} finally {
			Common.safeClose(con);
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
	public static User getUnregistered(long id) {
		Connection con = null;

		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetUnregisteredUserById(?)}");
			procedure.setLong(1, id);
			ResultSet results = procedure.executeQuery();
			
			if (results.next()) {
				User u = new User();
				u.setId(results.getLong("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				return u;
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Retrieves a user from the database given the user's id
	 * @param id the id of the user to get
	 * @return The user object associated with the user
	 * @author Tyler Jensen
	 */
	public static User get(long id){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetUserById(?)}");
			procedure.setLong(1, id);					
			ResultSet results = procedure.executeQuery();
			
			if(results.next()){
				User u = new User();
				u.setId(results.getLong("id"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setInstitution(results.getString("institution"));
				u.setCreateDate(results.getTimestamp("created"));
				u.setRole(results.getString("role"));							
				return u;
			}			
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Returns the (hashed) password of the given user.
	 * @param userId the user ID of the user to get the password of
	 * @return The (hashed) password for the user
	 * @author Skylar Stark
	 */
	public static String getPassword(long userId) {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetPasswordById(?)}");
			procedure.setLong(1, userId);
			ResultSet results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getString("password");
			}
			
		} catch (Exception e) {			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
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
	public static boolean updateFirstName(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateFirstName(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();						
			log.info(String.format("User [%d] updated first name to [%s]", userId, newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
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
	public static boolean updateLastName(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateLastName(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated last name to [%s]", userId, newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
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
	public static boolean updateEmail(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateEmail(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated e-mail address to [%s]", userId, newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
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
	public static boolean updateInstitution(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateInstitution(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated institution to [%s]", userId, newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
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
	public static boolean updatePassword(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdatePassword(?, ?)}");
			procedure.setLong(1, userId);
			String hashedPassword = Hash.hashPassword(newValue);
			procedure.setString(2, hashedPassword);
			
			procedure.executeUpdate();			
			log.info(String.format("User [%d] updated password", userId));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
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
	public static boolean setPassword(long user_id, String password){
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL SetPasswordByUserId(?, ?)}");
			procedure.setLong(1, user_id);
			procedure.setString(2, Hash.hashPassword(password));
			
			procedure.executeUpdate();
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
}
