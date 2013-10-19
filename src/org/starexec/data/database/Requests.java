package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
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
	 * Adds an entry to the queue_request table in a transaction-safe manner
	 * 
	 * @param code the code used in hyperlinks to safely reference this request
	 * @param con the connection maintaining the database transaction
	 * @param user user initiating the request
	 * @param message the message to the admins
	 * @param spaceId the id of the space this request is for
	 * @param queueId the id of the queue this request is for
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	protected static boolean addQueueRequest(Connection con, QueueRequest req ) {
		CallableStatement procedure = null;
		try {		
			//Check if msg is null (if it is --> admin is adding a queue)
			String message = req.getMessage();
			if (message == null) {
				message = "[admin added queueRequest]";
			}
			
			
			// Add a new entry to the community_request table
			 procedure = con.prepareCall("{CALL AddQueueReservation(?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, req.getUserId());
			procedure.setInt(2, req.getSpaceId());
			procedure.setString(3, req.getQueueName());
			procedure.setDate(4, req.getStartDate());
			procedure.setDate(5, req.getEndDate());
			procedure.setString(6, req.getCode());
			procedure.setString(7, message);
			procedure.setInt(8, req.getNodeCount());

			procedure.executeUpdate();		
			log.debug(String.format("Added reservation record for user [%s] on space %s", req.getUserId(), req.getSpaceId()));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
		}
		
		log.debug(String.format("Add reservation record failed for user [%s] on space %d", req.getUserId(), req.getSpaceId()));
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
		Connection con = null;
		
		try {
			con = Common.getConnection();
			return Requests.addQueueRequest(con, req); 			
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

			procedure = con.prepareCall("{CALL ApproveQueueReservation(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, req.getCode());
			procedure.setInt(2, req.getSpaceId());
			procedure.setInt(3, newQueueId);
			procedure.setInt(4, req.getNodeCount());
			procedure.setDate(5, req.getStartDate());
			procedure.setDate(6, req.getEndDate());
			procedure.setString(7, UUID.randomUUID().toString());
			
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
	
	
	public static boolean cancelQueueReservation(int queueId) {
		boolean success = Queues.remove(queueId);
		return success;
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
				req.setSpaceId(results.getInt("space_id"));
				
				int queue_id = results.getInt("queue_id");
				Queue q = Queues.get(queue_id);
				
				req.setQueueName(q.getName());
				req.setNodeCount(results.getInt("node_count"));
				req.setStartDate(results.getDate("start_date"));
				req.setEndDate(results.getDate("end_date"));
				
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
				req.setNodeCount(results.getInt("node_count"));
				req.setStartDate(results.getDate("start_date"));
				req.setEndDate(results.getDate("end_date"));
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
				req.setNodeCount(results.getInt("node_count"));
				req.setStartDate(results.getDate("start_date"));
				req.setEndDate(results.getDate("end_date"));
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
				req.setStartDate(results.getDate("start_date"));
				req.setEndDate(results.getDate("end_date"));
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

	public static boolean updateEndDate(String code, String newValue) {
		Connection con = null;			
		CallableStatement procedure= null;

		/*
		String month = newValue.substring(0,2);
		String day = newValue.substring(2, 4);
		String year = newValue.substring(4, 8);

		newValue = month + "/" + day + "/" + year;
		*/
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	    Date endDate = null;
		try {
			java.util.Date endDateJava = format.parse(newValue);
			endDate = new Date(endDateJava.getTime());
		} catch (ParseException e1) {
			e1.printStackTrace();
			return false;
		}
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL UpdateEndDate(?, ?)}");
			procedure.setString(1, code);	
			procedure.setDate(2, endDate);
			
			procedure.executeUpdate();			
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
			log.info(String.format("queueRequest updated nodeCount to [%s]", newValue));
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
	 * Updates the queueName of a request in the database with the 
	 * given queue_request code
	 * 
	 * @param code the queue_request code of the request we want to update
	 * @param newValue what the queuename will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Wyatt Kaiser
	 */
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

	public static boolean updateStartDate(String code, String newValue) {
		Connection con = null;			
		CallableStatement procedure= null;
		
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	    Date startDate = null;
		try {
			java.util.Date startDateJava = format.parse(newValue);
			startDate = new Date(startDateJava.getTime());
		} catch (ParseException e1) {
			e1.printStackTrace();
			return false;
		}
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL UpdateStartDate(?, ?)}");
			procedure.setString(1, code);	
			procedure.setDate(2, startDate);
			
			procedure.executeUpdate();			
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

}
