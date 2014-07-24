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
	 * Associates a single date with an existing queue request by placing it in the queue_request_assoc table
	 * @param con The open connection to perform the update on
	 * @param req The request object, which needs an id and a node count
	 * @param utilDate
	 * @return
	 */
	public static boolean associateWithQueueRequest(Connection con, QueueRequest req, Date utilDate) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL AssociateDateWithQueueRequest(?,?,?)}");
			procedure.setInt(1,req.getId());
			procedure.setDate(2, utilDate);
			procedure.setInt(3,req.getNodeCount());
			
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
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
	    List<java.sql.Date> dates = Requests.getDateRange(req.getStartDate(), req.getEndDate());
	  
		
	    Connection con = null;
	    try {
			con = Common.getConnection();
			int id = addQueueRequest(con, req);
			//if we got back a valid id
			Boolean result = false;

			if (id > 0) {
				result=true;
				for (java.sql.Date d : dates) {

				    result=result && associateWithQueueRequest(con,req,d);
				}
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
	protected static Integer addQueueRequest(Connection con, QueueRequest req) {
		CallableStatement procedure = null;
		try {		
			// Add a new entry to the community_request table
			procedure = con.prepareCall("{CALL AddQueueRequest(?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, req.getUserId());
			procedure.setInt(2, req.getSpaceId());
			procedure.setString(3, req.getQueueName());
			procedure.setString(4, req.getMessage());
			procedure.setInt(5,req.getWallTimeout());
			procedure.setInt(6,req.getCpuTimeout());
			procedure.registerOutParameter(7, java.sql.Types.INTEGER);
			
			req.setId(procedure.getInt(7));

			procedure.executeUpdate();		
			log.debug(String.format("Added reservation record for user [%s] on space %s", req.getUserId(), req.getSpaceId()));			
			return req.getId();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			
			Common.safeClose(procedure);
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}
	
	/**
	 * Deletes a reservation from the queue reservation table and also the queue_request_assoc table
	 *  
	 * @param request_id 
	 * @return True if the operation was a success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean removeQueueReservation(int request_id){
		log.debug("beginning approval of queue reservation.");
		Connection con = null;
		CallableStatement procedure = null;
		try {		
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL RemoveQueueReservation(?)}");
			procedure.setInt(1, request_id);
			
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
			e.printStackTrace();
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedureAddHistory);
			Common.safeClose(procedureDelete);
			Common.safeClose(procedureRemoveReservation);
		}
	}
	
	public static List<QueueRequest> getAllQueueReservations() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		
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
				int min_nodeCount = results.getInt("MIN(node_count)");
				int max_nodeCount = results.getInt("MAX(node_count)");
				req.setNodeCount(max_nodeCount);
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
				req.setMessage(results.getString("message"));
				
				reservations.add(req);

			}
			
			return reservations;
		} catch (Exception e) {
			log.error("GetAllQueueReservations says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
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
				req.setCreateDate(results.getTimestamp("created"));
				req.setCpuTimeout(results.getInt("cpuTimeout"));
				req.setWallTimeout(results.getInt("clockTimeout"));
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
	public static QueueRequest getQueueRequest(int id){
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetQueueRequestById(?)}");
			procedure.setInt(1, id);					
			 results = procedure.executeQuery();
			
			if(results.next()){
				QueueRequest req = new QueueRequest();
				req.setId(id);
				req.setUserId(results.getInt("user_id"));
				req.setSpaceId(results.getInt("space_id"));
				req.setQueueName(results.getString("queue_name"));
				req.setNodeCount(results.getInt("MAX(node_count)"));
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
				req.setMessage(results.getString("message"));
				req.setCreateDate(results.getTimestamp("created"));
				req.setWallTimeout(results.getInt("clockTimeout"));
				req.setCpuTimeout(results.getInt("cpuTimeout"));
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

	
	public static List<QueueRequest> getHistoricQueueReservations(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfHistoricQueueReservations(?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setString(5, searchQuery);
			results = procedure.executeQuery();
			
			List<QueueRequest> requests = new LinkedList<QueueRequest>();
			
			while(results.next()){
				QueueRequest req = new QueueRequest();
				req.setQueueName(results.getString("queue_name"));
				req.setNodeCount(results.getInt("node_count"));
				req.setStartDate(results.getDate("start_date"));
				req.setEndDate(results.getDate("end_date"));
				req.setMessage(results.getString("message"));
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
	 * Returns the number of queue_requests
	 * @return the number of queue_requests
	 */
	public static int getRequestCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetQueueRequestCount()}");
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

	public static int getReservationCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetQueueReservationCount()}");
			results = procedure.executeQuery();
			int reservationCount= 0;
			if (results.next()) {
				reservationCount = results.getInt("reservationCount");
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
	
	public static int getHistoricCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {			
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetHistoricReservationCount()}");
			results = procedure.executeQuery();
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
			Common.safeClose(results);
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
	 * Updates all the fields of a queue request, including the requests associations with different dates
	 * The request object given should have all its fields set with the new values.
	 * 
	 * @param req a QueueRequest object for which the id identifies the database entry to update
	 * 
	 * @return true if successful, false otherwise
	 * @author Wyatt kaiser
	 */
	public static boolean updateQueueRequest(QueueRequest req) {
		
		Connection con = null;
		
		try {
			con = Common.getConnection();
			return updateQueueRequest(con,req);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		
		return false;
	}
	/**
	 * Updates an entry of the queue_request table with new values
	 * @param con The open connection to make the request on
	 * @param req
	 * @param date
	 * @return
	 */
	protected static boolean updateQueueRequest(Connection con, QueueRequest req) {
		CallableStatement procedure = null;
		try {					
			
			// Add a new entry to the queue_request table
			procedure = con.prepareCall("{CALL UpdateQueueRequest(?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, req.getId());
			procedure.setInt(2, req.getUserId());
			procedure.setInt(3, req.getSpaceId());
			procedure.setString(4, req.getQueueName());
			procedure.setString(5, req.getMessage());
			procedure.setTimestamp(6, req.getCreateDate());
			procedure.setInt(7,req.getCpuTimeout());
			procedure.setInt(8,req.getWallTimeout());
			procedure.executeUpdate();		
			
			Common.safeClose(procedure);
			
			//first, clear out all the old assoc entries
			procedure=con.prepareCall("{DeleteQueueRequestAssocEntries(?)}");
			procedure.setInt(1,req.getId());
			procedure.executeUpdate();
			Common.safeClose(procedure);
			
			//then, add the new assoc entries to correspond with the newly updated dates and node counts
		    List<java.sql.Date> dates = getDateRange(req.getStartDate(), req.getEndDate());
		    for (java.sql.Date d : dates) {
		    	
		    	Requests.associateWithQueueRequest(con, req, d);
		    }
			
			log.debug(String.format("updated request record for user [%s] on space %s", req.getUserId(), req.getSpaceId()));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
		}
		
		return false;
	}
	

	/**
	 * Returns the node count on a particlar day for a particular queue request
	 * @param name The name of the queue
	 * @param date the date to get the node_count for
	 * @return the node_count
	 * @author Wyatt Kaiser
	 */
	public static int GetNodeCountOnDate(int requestId, java.util.Date date) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetRequestNodeCountOnDate(?, ?)}");
			procedure.setInt(1, requestId);
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

	public static QueueRequest getRequestForReservation(int queueId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetQueueRequestForReservation(?)}");
			procedure.setInt(1, queueId);			
			
			results = procedure.executeQuery();

			QueueRequest req = new QueueRequest();
			while(results.next()){
				int queue_id = results.getInt("queue_id");
				String queueName = Queues.getNameById(queue_id);
				req.setQueueName(queueName);
				req.setSpaceId(results.getInt("space_id"));
				req.setStartDate(results.getDate("MIN(reserve_date)"));
				req.setEndDate(results.getDate("MAX(reserve_date)"));
				req.setNodeCount(results.getInt("MAX(node_count)"));
				req.setMessage(results.getString("message"));
			}			

			return req;			
			
		} catch (Exception e){			
			log.error("GetRequestForReservation says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
		
	}

	public static Date getEarliestEndDate(Date reserve_date) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetEarliestEndDate(?)}");	
			procedure.setDate(1, reserve_date);
			results = procedure.executeQuery();

			while(results.next()){
				return results.getDate("min(endDate)");
			}			
			
		} catch (Exception e){			
			log.error("GetRequestForReservation says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	public static void DecreaseNodeCount(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DecreaseNodeCount(?)}");
			procedure.setInt(1, id);
			procedure.executeUpdate();
			
		} catch (Exception e) {
			log.error("DecreaseNodeCount says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

}
