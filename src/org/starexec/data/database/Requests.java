package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.User;

/**
 * Handles all database interaction for the various requests throughout the system. This includes
 * user activation, community and password reset requests
 */
public class Requests {
	private static final Logger log = Logger.getLogger(Requests.class);
	
	/**
	 * Adds an activation code to the database for a given user (used during registration)
	 * 
	 * @param user the user to add an activation code to the database for
	 * @param con the database connection maintaining the transaction
	 * @param code the new activation code to add
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	protected static boolean addActivationCode(Connection con, User user, String code) {
	    CallableStatement procedure = null;
		try {			
			// Add a new entry to the VERIFY table
		        procedure = con.prepareCall("{CALL AddCode(?, ?)}");
			procedure.setInt(1, user.getId());
			procedure.setString(2, code);

			// Apply update to database and check to be sure at least 1 row was modified
			procedure.executeUpdate();									
			log.info(String.format("New email activation code [%s] added to VERIFY for user [%s]", code, user.getFullName()));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
		    Common.safeClose(procedure);
		}
		
		log.debug(String.format("Adding activation record failed for [%s]", user.getFullName()));
		return false;
	}
	
	/**
	 * Adds an entry to the community request table in a transaction-safe manner
	 * 
	 * @param code the code used in hyperlinks to safely reference this request
	 * @param con the connection maintaining the database transaction
	 * @param user user initiating the request
	 * @param message the message to the leaders of the community
	 * @param communityId the id of the community this request is for
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	protected static boolean addCommunityRequest(Connection con, User user, int communityId, String code, String message ) {
	    CallableStatement procedure = null;
		try {		
			// Add a new entry to the community_request table
		        procedure = con.prepareCall("{CALL AddCommunityRequest(?, ?, ?, ?)}");
			procedure.setInt(1, user.getId());
			procedure.setInt(2, communityId);
			procedure.setString(3, code);
			procedure.setString(4, message);

			procedure.executeUpdate();		
			log.debug(String.format("Added invitation record for user [%s] on community %d", user, communityId));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
		    Common.safeClose(procedure);
		}
		
		log.debug(String.format("Add invitation record failed for user [%s] on community %d", user, communityId));
		return false;
	}
	
	/**
	 * Adds a request to join a community for a given user and community
	 * 
	 * @param user the user who wants to join a community
	 * @param communityId the id of the community the user wants to join
	 * @param code the code used in hyperlinks to safely reference this request
	 * @param message the message the user wrote to the leaders of this community
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean addCommunityRequest(User user, int communityId, String code, String message) {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			return Requests.addCommunityRequest(con, user, communityId, code, message); 			
		} catch (Exception e) {
			log.error(e.getMessage(), e);			
		} finally {
			Common.safeClose(con);					
		}
		
		return false;
	}
	
	/**
	 * Checks an activation code provided by the user against the code in table VERIFY 
	 * and if they match, removes that entry in VERIFY, and adds an entry to USER_ROLES
	 * @param codeFromUser the activation code provided by the user
	 * @author Todd Elvers
	 */
	public static int redeemActivationCode(String codeFromUser){
		Connection con = null;
		CallableStatement procedure = null;
		try {
			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL RedeemActivationCode(?, ?)}");
			procedure.setString(1, codeFromUser);
			procedure.registerOutParameter(2, java.sql.Types.INTEGER);
			
			procedure.executeUpdate();			
			int userId = procedure.getInt(2);
			log.info(String.format("Activation code %s redeemed by user %d", codeFromUser, userId));
			return userId;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return -1;
	}
	
	/**
	 * Adds a user to their community and deletes their entry in the community request table
	 *  
	 * @param userId the user id of the newly registered user
	 * @param communityId the community the newly registered user is joining
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean approveCommunityRequest(int userId, int communityId){
		Connection con = null;
		CallableStatement procedure = null;
		try {		
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL ApproveCommunityRequest(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, communityId);
			
			procedure.executeUpdate();			
			return true;					
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Deletes a community request given a user id and community id, and if
	 * the user is unregistered, they are also removed from the users table
	 * @param userId the user id associated with the invite
	 * @param communityId the communityId associated with the invite
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean declineCommunityRequest(int userId, int communityId){
		Connection con = null;
		CallableStatement procedure = null;		
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL DeclineCommunityRequest(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, communityId);
			
			procedure.executeUpdate();			
			return true;			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Retrieves a community request from the database given the user id
	 * @param userId the user id of the request to retrieve
	 * @return The request associated with the user id
	 * @author Todd Elvers
	 */
	public static CommunityRequest getCommunityRequest(int userId){
		Connection con = null;			
		CallableStatement procedure = null;		
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetCommunityRequestById(?)}");
			procedure.setInt(1, userId);					
			results = procedure.executeQuery();
			
			if(results.next()){
				CommunityRequest req = new CommunityRequest();
				req.setUserId(results.getInt("user_id"));
				req.setCommunityId(results.getInt("community"));
				req.setCode(results.getString("code"));
				req.setMessage(results.getString("message"));
				req.setCreateDate(results.getTimestamp("created"));
				return req;
			}						
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Retrieves a community request from the database given a code
	 * @param code the code of the request to retrieve
	 * @return The request object associated with the request code
	 * @author Todd Elvers
	 */
	public static CommunityRequest getCommunityRequest(String code){
		Connection con = null;			
		CallableStatement procedure = null;		
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetCommunityRequestByCode(?)}");
			procedure.setString(1, code);					
			results = procedure.executeQuery();
			
			if(results.next()){
				CommunityRequest req = new CommunityRequest();
				req.setUserId(results.getInt("user_id"));
				req.setCommunityId(results.getInt("community"));
				req.setCode(results.getString("code"));
				req.setMessage(results.getString("message"));
				req.setCreateDate(results.getTimestamp("created"));
				return req;
			}			
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Adds a request to have a user's password reset; prevents duplicate entries
	 * in the password reset table by first deleting any previous entries for this
	 * user
	 * @param userId the id of the user requesting the password reset
	 * @param code the code to add to the password reset table (for hyperlinking)
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers 
	 */
	public static boolean addPassResetRequest(int userId, String code){
		Connection con = null;			
		CallableStatement procedure = null;		
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL AddPassResetRequest(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, code);
			
			procedure.executeUpdate();			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return false;
	}
	
	
	/**
	 * Gets the user_id associated with the given code and deletes
	 * the corresponding entry in the password reset table
	 * @param code the UUID to check the password reset table with
	 * @return the id of the user requesting the password reset if the
	 * code provided matches one in the password reset table, -1 otherwise
	 * @author Todd Elvers
	 */
	public static int redeemPassResetRequest(String code){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL RedeemPassResetRequestByCode(?, ?)}");
			procedure.setString(1, code);
			procedure.registerOutParameter(2, java.sql.Types.INTEGER);
			procedure.executeUpdate();
						
			return procedure.getInt(2);
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return -1;
	}
}
