package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
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
	 * Adds a request to reserve a queue for a given user and space
	 * 
	 * @param userId the user who wants to join a community
	 * @param communityId the id of the community the user wants to join
	 * @param code the code used in hyperlinks to safely reference this request
	 * @param message the message the user wrote to the leaders of this community
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean addQueueRequest(QueueRequest req) {
		//Get all the dates between these two dates
	    List<java.util.Date> dates = new ArrayList<java.util.Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(req.getStartDate());
	    while (calendar.getTime().before(req.getEndDate()))
	    {
	        java.util.Date result = calendar.getTime();
	        dates.add(result);
	        calendar.add(Calendar.DATE, 1);
	    }
	    java.util.Date latestResult = calendar.getTime();
	    dates.add(latestResult);
		
	    Connection con = null;
	    try {
			con = Common.getConnection();
			Boolean result = false;
			for (java.util.Date utilDate : dates) {
			    result = addQueueRequest(con, req, utilDate);
			}
			return result;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
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
	protected static boolean addQueueRequest(Connection con, QueueRequest req, java.util.Date date ) {
		CallableStatement procedure = null;
		try {		
			con = Common.getConnection();
			
		    java.sql.Date sqlDate = new java.sql.Date(date.getTime());
			
			
			// Add a new entry to the community_request table
			procedure = con.prepareCall("{CALL AddQueueRequest(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, req.getUserId());
			procedure.setInt(2, req.getSpaceId());
			procedure.setString(3, req.getQueueName());
			procedure.setDate(4, sqlDate);
			procedure.setString(5, req.getCode());
			procedure.setString(6, req.getMessage());
			procedure.setInt(7, req.getNodeCount());

			procedure.executeUpdate();		
			log.debug(String.format("Added reservation record for user [%s] on space %s", req.getUserId(), req.getSpaceId()));			
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
	 * Adds a queue to the comm_queue table (reserving it for a specific space)
	 * and deletes the entry from the queue request table
	 *  
	 * @param userId the user id of the newly registered user
	 * @param communityId the community the newly registered user is joining
	 * @return True if the operation was a success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean approveQueueReservation(QueueRequest req, int newQueueId){
		log.debug("beginning approval of queue reservation.");
		Connection con = null;
		CallableStatement procedure = null;
		try {		
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL ApproveQueueReservation(?, ?, ?)}");
			procedure.setString(1, req.getCode());
			procedure.setInt(2, req.getSpaceId());
			procedure.setInt(3, newQueueId);
			
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
	 * Deletes a queue request given a space id and queue id,
	 * @param userId the user id associated with the invite
	 * @param communityId the communityId associated with the invite
	 * @return True if the operation was a success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean declineQueueReservation(QueueRequest req){
		Connection con = null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();

			 procedure = con.prepareCall("{CALL DeclineQueueReservation(?)}");
			procedure.setString(1, req.getCode());
			
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
	
	public static void DeleteReservation(QueueRequest req) {
		Connection con = null;
		CallableStatement procedureAddHistory = null;
		CallableStatement procedureDelete = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			String queueName = req.getQueueName();
			int queueId = Queues.getIdByName(queueName);

			procedureAddHistory = con.prepareCall("{CALL AddReservationToHistory(?,?,?,?,?)}");
			procedureAddHistory.setInt(1, req.getSpaceId());
			procedureAddHistory.setInt(2, queueId);
			//req.getNodeCount() refers to the max node count
			procedureAddHistory.setInt(3, req.getNodeCount());
			procedureAddHistory.setDate(4, req.getStartDate());
			procedureAddHistory.setDate(5, req.getEndDate());
			procedureAddHistory.executeUpdate();
			
			procedureDelete = con.prepareCall("{CALL RemoveQueue(?)}");
			procedureDelete.setInt(1, queueId);
			procedureDelete.executeUpdate();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedureAddHistory);
			Common.safeClose(procedureDelete);
		}
	}
	
	public static List<QueueRequest> getAllQueueReservations() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results;
		
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllQueueReservations()}");
			results = procedure.executeQuery();
			
			List<QueueRequest> reservations = new LinkedList<QueueRequest>();
			while (results.next()) {
				QueueRequest req = new QueueRequest();
				log.debug("space_id = " + results.getInt("space_id"));
				req.setSpaceId(results.getInt("space_id"));
				
				log.debug("queue_id = " + results.getInt("queue_id"));
				int queue_id = results.getInt("queue_id");
				Queue q = Queues.get(queue_id);
				log.debug("queue = " + q);
				log.debug("q status = " + q.getStatus());
				log.debug("q name = " + q.getName());
				
				req.setQueueName(q.getName());
				int min_nodeCount = results.getInt("MIN(node_count)");
				int max_nodeCount = results.getInt("MAX(node_count)");
				req.setNodeCount(max_nodeCount);
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
				
				reservations.add(req);

			}
			
			return reservations;
		} catch (Exception e) {
			log.error("GetAllQueueReservations says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return null;
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
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetCommunityRequestCount()}");
			ResultSet results = procedure.executeQuery();
			int reservationCount= 0;
			if (results.next()) {
				reservationCount = results.getInt("requestCount");
			}		
			return reservationCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
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
	 * 
	 * @param startingRecord
	 * @param recordsPerPage
	 * @return list of queueRequests that have not yet been approved/declined
	 * @author Wyatt Kaiser
	 */
	public static List<QueueRequest> getPendingQueueRequests(int startingRecord, int recordsPerPage) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfPendingQueueRequests(?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			results = procedure.executeQuery();
			
			List<QueueRequest> requests = new LinkedList<QueueRequest>();
			
			while(results.next()){
				QueueRequest req = new QueueRequest();
				req.setUserId(results.getInt("user_id"));
				req.setSpaceId(results.getInt("space_id"));
				req.setQueueName(results.getString("queue_name"));
				req.setNodeCount(results.getInt("MAX(node_count)"));
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
				req.setCode(results.getString("code"));
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
	 * Retrieves a queue request from the database given a code
	 * @param code the code of the request to retrieve
	 * @return The request object associated with the request code
	 * @author Wyatt Kaiser
	 */
	public static QueueRequest getQueueRequest(String code){
		if (code == null) {
			log.debug("code is null");
		}
		log.debug("code = " + code);
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetQueueRequestByCode(?)}");
			procedure.setString(1, code);					
			 results = procedure.executeQuery();
			
			if(results.next()){
				QueueRequest req = new QueueRequest();
				req.setUserId(results.getInt("user_id"));
				req.setSpaceId(results.getInt("space_id"));
				req.setQueueName(results.getString("queue_name"));
				req.setNodeCount(results.getInt("MAX(node_count)"));
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
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
	
	public static List<QueueRequest> getQueueReservations(int startingRecord, int recordsPerPage) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfQueueReservations(?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			results = procedure.executeQuery();
			
			List<QueueRequest> requests = new LinkedList<QueueRequest>();
			
			while(results.next()){
				QueueRequest req = new QueueRequest();
				req.setSpaceId(results.getInt("space_id"));
				int queue_id = results.getInt("queue_id");
				Queue q = Queues.get(queue_id);
				req.setQueueName(q.getName());
				req.setNodeCount(results.getInt("node_count"));
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
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
	
	public static String getQueueReservedCode(int newQueueId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetQueueReservedCode(?)}");
			procedure.setInt(1, newQueueId);
			ResultSet results = procedure.executeQuery();
			String reservedCode = null;
			if (results.next()) {
				reservedCode = results.getString("code");
			}		
			return reservedCode;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return null;		
	}

	public static boolean cancelQueueReservation(int queueId) {
		boolean success = Queues.remove(queueId);
		return success;
		}

	/**
	 * Returns the number of queue_requests
	 * @return the number of queue_requests
	 */
	public static int getRequestCount() {
		Connection con = null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetQueueRequestCount()}");
			ResultSet results = procedure.executeQuery();
			int reservationCount= 0;
			if (results.next()) {
				reservationCount = results.getInt("requestCount");
			}		
			return reservationCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return 0;
	}

	public static int getReservationCount() {
		Connection con = null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetQueueReservationCount()}");
			ResultSet results = procedure.executeQuery();
			int reservationCount= 0;
			if (results.next()) {
				reservationCount = results.getInt("reservationCount");
			}		
			return reservationCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return 0;
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
	 * Updates the queueName, nodeCount, startDate, and endDate for a queueRequest. Does so by deleting all
	 * entries and re-creating them.
	 * 
	 * @param req the new QueueRequest to add
	 * 
	 * @return true if successfull, false otherwise
	 * @author Wyatt kaiser
	 */
	public static boolean updateQueueRequest(QueueRequest req) {
	    List<java.util.Date> dates = new ArrayList<java.util.Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(req.getStartDate());
	    while (calendar.getTime().before(req.getEndDate()))
	    {
	        java.util.Date result = calendar.getTime();
	        dates.add(result);
	        calendar.add(Calendar.DATE, 1);
	    }
	    java.util.Date latestResult = calendar.getTime();
	    dates.add(latestResult);
	    
		Connection con = null;
		CallableStatement procedure_delete = null;
		
		try {
			con = Common.getConnection();
			
			procedure_delete = con.prepareCall("{CALL DeclineQueueReservation(?)}");
			procedure_delete.setString(1, req.getCode());
			procedure_delete.executeUpdate();
			
			Boolean result = false;
			for (java.util.Date utilDate : dates) {
				result = updateQueueRequest(con, req, utilDate);
			}
			return result;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure_delete);
		}
		
		
		return false;
	}

	protected static boolean updateQueueRequest(Connection con, QueueRequest req, java.util.Date date ) {
		CallableStatement procedure = null;
		try {					
		    java.sql.Date sqlDate = new java.sql.Date(date.getTime());
		    log.debug("req.getNodeCount() = " + req.getNodeCount());
			
			// Add a new entry to the queue_request table
			procedure = con.prepareCall("{CALL UpdateQueueRequest(?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, req.getUserId());
			procedure.setInt(2, req.getSpaceId());
			procedure.setString(3, req.getQueueName());
			procedure.setDate(4, sqlDate);
			procedure.setString(5, req.getCode());
			procedure.setString(6, req.getMessage());
			procedure.setInt(7, req.getNodeCount());
			procedure.setTimestamp(8, req.getCreateDate());

			procedure.executeUpdate();		
			log.debug(String.format("updated request record for user [%s] on space %s", req.getUserId(), req.getSpaceId()));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
		}
		
		return false;
	}
	
	
/*
	public static boolean updateDates(String code, String startDate, String endDate){
		Connection con = null;			
		CallableStatement procedure_delete = null;
		CallableStatement procedure_add = null;
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	    Date new_start_date = null;
	    Date new_end_date = null;
		try {
			java.util.Date startDateJava = format.parse(startDate);
			new_start_date = new Date(startDateJava.getTime());
			java.util.Date endDateJava = format.parse(endDate);
			new_end_date = new Date(endDateJava.getTime());
		} catch (ParseException e1) {
			e1.printStackTrace();
			return false;
		}
		
		QueueRequest req = Requests.getQueueRequest(code);

		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);

			
			
			QueueRequest new_req = new QueueRequest();
			new_req.setUserId(req.getUserId());
			new_req.setSpaceId(req.getSpaceId());
			new_req.setQueueName(req.getQueueName());
			new_req.setNodeCount(req.getNodeCount());
			
			
			procedure = con.prepareCall("{CALL UpdateRequestDates(?, ? , ? , ? , ?, ?, ?, ?)}");
			procedure.setInt(1, req.getUserId());
			procedure.setInt(2, req.getSpaceId());
			procedure.setString(3, req.getQueueName());
			procedure.setInt(4, req.getNodeCount());
			procedure.setDate(5, reserveDate);
			procedure.setString(6, req.getMessage());
			procedure.setTimestamp(7, req.getCreateDate());
			
			
			
			if (new_start_date.toString().equals((old_start_date.toString()))) {
				log.info(String.format("queueRequest updated startDate to [%s]", newValue));
				return true;			
			} else if (new_start_date.after(old_start_date)) {
				//DELETE FROM queue_req where reserve_date < startDate
				procedure = con.prepareCall("{CALL UpdateStartDateToLaterDate(?, ?)}");
				procedure.setString(1, code);	
				procedure.setDate(2, new_start_date);
				
				procedure.executeUpdate();
			} else if (new_start_date.before(old_start_date)) {
				//Get all the dates between these two dates
			    List<java.util.Date> dates = new ArrayList<java.util.Date>();
			    Calendar calendar = new GregorianCalendar();
			    calendar.setTime(old_start_date);
			    while (calendar.getTime().before(new_start_date))
			    {
			        java.util.Date result = calendar.getTime();
			        dates.add(result);
			        calendar.add(Calendar.DATE, 1);
			    }
			    java.util.Date latestResult = calendar.getTime();
			    dates.add(latestResult);
			    
			    for (java.util.Date d : dates) {
			    	procedure = con.prepareCall("{CALL UpdateStartDateToEarlierDate(?,?,?,?,?,?,?,?)}");
			    	procedure.setInt(1, req.getUserId());
			    	procedure.setInt(2, req.getSpaceId());
			    	procedure.setString(3, req.getQueueName());
			    	procedure.setInt(4, req.getNodeCount());
					java.sql.Date sqlDate = new java.sql.Date(d.getTime());
			    	procedure.setDate(5, sqlDate);
			    	procedure.setString(6, req.getMessage());
			    	procedure.setString(7, req.getCode());
			    	procedure.setTimestamp(8, req.getCreateDate());
			    	
			    	procedure.executeUpdate();
			    }
			}
						
			log.info(String.format("queueRequest updated startDate to [%s]", newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}

	public static boolean updateEndDate(String code, String newValue) {
		Connection con = null;			
		CallableStatement procedure= null;
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	    Date new_end_date = null;
		try {
			java.util.Date endDateJava = format.parse(newValue);
			new_end_date = new Date(endDateJava.getTime());
		} catch (ParseException e1) {
			e1.printStackTrace();
			return false;
		}
		QueueRequest req = Requests.getQueueRequest(code);
		Date old_end_date = req.getEndDate();

		try {
			con = Common.getConnection();	
			log.debug(new_end_date.toString());
			log.debug(old_end_date.toString());
			log.debug("equals = " + new_end_date.toString().equals((old_end_date.toString())) );
			if (new_end_date.toString().equals((old_end_date.toString()))) {
				log.info(String.format("queueRequest updated endDate to [%s]", newValue));
				return true;			
			} else if (new_end_date.after(old_end_date)) {
				//Get all the dates between these two dates
			    List<java.util.Date> dates = new ArrayList<java.util.Date>();
			    Calendar calendar = new GregorianCalendar();
			    calendar.setTime(old_end_date);
			    while (calendar.getTime().before(new_end_date))
			    {
			        java.util.Date result = calendar.getTime();
			        dates.add(result);
			        calendar.add(Calendar.DATE, 1);
			    }
			    java.util.Date latestResult = calendar.getTime();
			    dates.add(latestResult);
			    
			    for (java.util.Date d : dates) {
			    	procedure = con.prepareCall("{CALL UpdateEndDateToLaterDate(?,?,?,?,?,?,?,?)}");
			    	procedure.setInt(1, req.getUserId());
			    	procedure.setInt(2, req.getSpaceId());
			    	procedure.setString(3, req.getQueueName());
			    	procedure.setInt(4, req.getNodeCount());
					java.sql.Date sqlDate = new java.sql.Date(d.getTime());
			    	procedure.setDate(5, sqlDate);
			    	procedure.setString(6, req.getMessage());
			    	procedure.setString(7, req.getCode());
			    	procedure.setTimestamp(8, req.getCreateDate());
			    	
			    	procedure.executeUpdate();
			    }
			} else if (new_end_date.before(old_end_date)) {
				//DELETE FROM queue_req where reserve_date < startDate
				procedure = con.prepareCall("{CALL UpdateEndDateToEarlierDate(?, ?)}");
				procedure.setString(1, code);	
				procedure.setDate(2, new_end_date);
				
				procedure.executeUpdate();
			}
						
			log.info(String.format("queueRequest updated endDate to [%s]", newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}

	public static boolean updateNodeCount(String code, String newValue) {
		Connection con = null;			
		CallableStatement procedure= null;
		int node_count = Integer.parseInt(newValue);
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL UpdateNodeCount(?, ?)}");
			procedure.setString(1, code);	
			procedure.setInt(2, node_count);
			
			procedure.executeUpdate();
			log.info(String.format("queueRequest updated node count to [%s]", newValue));
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}

	public static boolean updateQueueName(String code, String newValue){
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL UpdateQueueName(?, ?)}");
			procedure.setString(1, code);					
			procedure.setString(2, newValue);
			
			procedure.executeUpdate();			
			log.info(String.format("queueRequest updated queue name to [%s]", newValue));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}
	*/

	/**
	 * Returns the node count on a particlar day for a particular queue request
	 * @param name The name of the queue
	 * @param date the date to get the node_count for
	 * @return the node_count
	 * @author Wyatt Kaiser
	 */
	public static int GetNodeCountOnDate(String name, java.util.Date date) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetRequestNodeCountOnDate(?, ?)}");
			procedure.setString(1, name);
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());
			procedure.setDate(2, sqlDate);
			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("count");
			}			

			return -1;			
			
		} catch (Exception e){			
			log.error("GetRequestNodeCountOnDate says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Returns the space Id that is associated with a queue reservation 
	 * @param queueId the queueId to get the spaceId fro
	 * @return spaceId
	 * 
	 * @author Wyatt Kaiser
	 */
	public static int getQueueReservationSpaceId (int queueId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetQueueReservationSpaceId(?)}");
			procedure.setInt(1, queueId);			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("space_id");
			}			

			return -1;			
			
		} catch (Exception e){			
			log.error("GetQueueReservationSpaceId says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	public static int getQueueRequestSpaceId(String queueName) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetQueueRequestSpaceId(?)}");
			procedure.setString(1, queueName);			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("space_id");
			}			

			return -1;			
			
		} catch (Exception e){			
			log.error("GetQueueRequestSpaceId says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	public static int getMinNodeCountForRequest(String queueName) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetMinNodeCountForRequest(?)}");
			procedure.setString(1, queueName);			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("count");
			}			

			return -1;			
			
		} catch (Exception e){			
			log.error("GetMinNodeCountForRequest says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

}
