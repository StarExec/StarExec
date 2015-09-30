package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.Queue;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecDatabaseException;

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
		} finally {
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
		} finally {
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	public static Boolean DeleteReservation(int queueId) {
		log.debug("deleteReservation");
		Connection con = null;
		CallableStatement procedureAddHistory = null;
		CallableStatement procedureRemoveReservation = null;
		CallableStatement procedureDelete = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			
			procedureRemoveReservation = con.prepareCall("{CALL CancelQueueReservation(?)}");
			procedureRemoveReservation.setInt(1, queueId);
			procedureRemoveReservation.executeUpdate();
			
			procedureDelete = con.prepareCall("{CALL RemoveQueue(?)}");
			procedureDelete.setInt(1, queueId);
			procedureDelete.executeUpdate();
			
			Common.endTransaction(con);			
			return true;
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedureAddHistory);
			Common.safeClose(procedureDelete);
			Common.safeClose(procedureRemoveReservation);
		}
	}
	
	/**
	 * Retrieves a community request from the database given the user id
	 * @param userId the user id of the request to retrieve
	 * @return The request associated with the user id
	 * @author Todd Elvers
	 */
	public static CommunityRequest getCommunityRequest(int userId){
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
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
		ResultSet results=null;
		CallableStatement procedure = null;
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
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	/**
	 * Returns the number of community_requests
	 * @return the number of community_requests
	 */
	public static int getCommunityRequestCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetCommunityRequestCount()}");
			results = procedure.executeQuery();
			int reservationCount= 0;
			if (results.next()) {
				reservationCount = results.getInt("requestCount");
			}		
			return reservationCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return 0;
	}
	
	
	public static List<CommunityRequest> getPendingCommunityRequests(int startingRecord, int recordsPerPage) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfPendingCommunityRequests(?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			results = procedure.executeQuery();
			
			List<CommunityRequest> requests = new LinkedList<CommunityRequest>();
			
			while(results.next()){
				CommunityRequest req = new CommunityRequest();
				req.setUserId(results.getInt("user_id"));
				req.setCommunityId(results.getInt("community"));
				req.setCode(results.getString("code"));
				req.setMessage(results.getString("message"));
				req.setCreateDate(results.getTimestamp("created"));

				requests.add(req);	
				
							
			}	
			return requests;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return -1;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return -1;
	}
	
	/**
	 * Given a start date and an end date, gets an array that has all the dates between them, inclusive.
	 * The dates will be ordered by time
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	
	public static List<java.sql.Date> getDateRange(java.util.Date startDate, java.util.Date endDate) {
	    List<java.sql.Date> dates = new ArrayList<java.sql.Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(startDate);
	    while (calendar.getTime().before(endDate))
	    {
	        java.util.Date result = calendar.getTime();
			java.sql.Date sqlDate = new java.sql.Date(result.getTime());

	        dates.add(sqlDate);
	        calendar.add(Calendar.DATE, 1);
	    }
	    java.util.Date latestResult = calendar.getTime();
		java.sql.Date sqlDate = new java.sql.Date(latestResult.getTime());

	    dates.add(sqlDate);
	    return dates;
	}


	/**
	 * Adds a change email request to the database.
	 * @param userId The id of the user making the request.
	 * @param newEmail The new email the user wants to change their account's email to.
	 * @author Albert Giegerich
	 */
	public static void addChangeEmailRequest(int userId, String newEmail, String code) throws StarExecDatabaseException{
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL AddChangeEmailRequest(?, ?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, newEmail);
			procedure.setString(3, code);
			procedure.executeQuery();
		} catch (Exception e) {
			throw new StarExecDatabaseException(
					"There was an error while trying to add a change email request for user with id="+userId+ 
					" newEmail="+newEmail+" and code="+code, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	public static Pair<String, String> getChangeEmailRequest(int userId) throws StarExecDatabaseException {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		String newEmail = null;
		String emailChangeCodeAssociatedWithUser = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetChangeEmailRequest(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			// There should only be 1 result since the user id is the primary
			// key.
			results.first();
			newEmail = results.getString("new_email");
			emailChangeCodeAssociatedWithUser = results.getString("code");
		} catch (Exception e) {
			throw new StarExecDatabaseException(
					"There was an error while trying to get a change email request for user with id="+userId+".", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		Pair<String, String> emailAndCode = new ImmutablePair<String, String>(newEmail, emailChangeCodeAssociatedWithUser);
		return emailAndCode;
	}

	/**
	 * Deletes a change email request.
	 * @param userId the id of the user associated with the change email request.
	 * @throws StarExecDatabaseException if an error occurs in the database.
	 * @author Albert Giegerich
	 */
	public static void deleteChangeEmailRequest(int userId) throws StarExecDatabaseException {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DeleteChangeEmailRequest(?)}");
			procedure.setInt(1, userId);
			procedure.executeQuery();
		} catch (Exception e) {
			throw new StarExecDatabaseException(
					"There was an error while trying to delete a change email requests for user with id="+userId+".", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	public static boolean changeEmailRequestExists(int userId) throws StarExecDatabaseException {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		boolean changeEmailRequestExists = false;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetChangeEmailRequest(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			// results.next() will be true if there is at least one result
			if (results.next()) {
				changeEmailRequestExists = true;
			} 

			log.debug("(changeEmailRequestExists) Change email request does"+(changeEmailRequestExists?" ":" not ")+
					"exist for user with id="+userId);
		} catch (Exception e) {
			throw new StarExecDatabaseException(
					"There was an error while trying to get a change email requests for user with id="+userId+".", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return changeEmailRequestExists;
	}
}
