package org.starexec.data;

import java.sql.*;
import java.util.*;

import org.apache.log4j.*;
import org.starexec.constants.R;
import org.starexec.data.to.*;
import org.starexec.util.Hash;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 * Database objects are responsible for any and all communication to the MySQL back-end database.
 * This class provides a convenient front for storing things in the database and extracting information.
 */
public class Database {	
	private static final Logger log = Logger.getLogger(Database.class);
	private static DataSource dataPool = null;	
	public static enum WebsiteType { USER, SOLVER, SPACE };
	
	/**
	 * Static initialzer block which gets executed when this class is loaded.
	 * The code below configures the tomcat JDBC connection pool.
	 * @author Tyler Jensen
	 */
	static {						
		try {
			log.debug("Setting up data connection pool properties");
			PoolProperties poolProp = new PoolProperties();				// Set up the Tomcat JDBC connection pool with the following properties
			poolProp.setUrl(R.MYSQL_URL);								// URL to the database we want to use
			poolProp.setDriverClassName(R.MYSQL_DRIVER);				// We're using the JDBC driver
			poolProp.setUsername(R.MYSQL_USERNAME);						// Database username
			poolProp.setPassword(R.MYSQL_PASSWORD);						// Database password for the given username
			poolProp.setTestOnBorrow(true);								// True to check if a connection is live every time we take one from the pool
			poolProp.setValidationQuery("SELECT 1");					// The query to execute to check if a connection is live
			poolProp.setValidationInterval(20000);						// Only check live connection every so often when borrowing (milliseconds)
			poolProp.setMaxActive(R.MYSQL_POOL_MAX_SIZE);				// How many active connections can we have in the pool
			poolProp.setInitialSize(R.MYSQL_POOL_MIN_SIZE);				// How many connections the pool will start out with
			poolProp.setMinIdle(R.MYSQL_POOL_MIN_SIZE);					// The minimum number of connections to keep "ready to go"
			poolProp.setDefaultAutoCommit(true);						// Turn autocommit on (turn transactions off by default)
			poolProp.setJmxEnabled(false);								// Turn JMX off (we don't use it so we don't need it)
			poolProp.setRemoveAbandonedTimeout(30);						// How long to wait (seconds) before reclaiming an open connection (should be the time of longest query)
			poolProp.setRemoveAbandoned(true);							// Enable removing connections that are open too long
			
			log.debug("Creating new datapool with supplied properties");		
			dataPool = new DataSource(poolProp);						// Create the connection pool with the supplied properties
			log.debug("Datapool successfully created!");
		} catch (Exception e) {
			log.fatal(e.getMessage(), e);
		}
	}
	
	/**
	 * Adds the specified user to the database. This method will hash the 
	 * user's password for them, so it must be supplied in plaintext.
	 * 
	 * @param user The user to add
	 * @param communityId the id of the community to add this user wants to join
	 * @param message the message from the user to the leaders of a community
	 * @param code the unique code to add to the database for this user
	 * @return true iff the user was added to the users, verification, and community request table
	 * @author Todd Elvers
	 */
	public static boolean registerUser(User user, long communityId, String code, String message){
		Connection con = null;
		
		try{
			con = dataPool.getConnection();					
			beginTransaction(con);
			
			String hashedPass = Hash.hashPassword(user.getPassword());
			
			CallableStatement procedure = con.prepareCall("{CALL AddUser(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, user.getFirstName());
			procedure.setString(2, user.getLastName());
			procedure.setString(3, user.getEmail());
			procedure.setString(4, user.getInstitution());
			procedure.setString(5, hashedPass);
			
			// Register output of ID the user is inserted under
			procedure.registerOutParameter(6, java.sql.Types.BIGINT);
			
			// Register output of the affected row count for the insert
			procedure.registerOutParameter(7, java.sql.Types.BIGINT);
			
			// Add user to the users table and check to be sure 1 row was modified
			procedure.executeUpdate();
			long rowsModified = procedure.getLong(7);											
			
			if(rowsModified == 0){				
				doRollback(con);
				return false;
			}
			
			// Extract id from OUT parameter
			user.setId(procedure.getLong(6));
			
			boolean added = false;
			
			// Add unique activation code to VERIFY database under new user's id
			if(addCode(con, user, code)){
				// Add user's request to join a community to INVITES
				added = addCommunityRequest(con, user, communityId, message);
			}
			
			if(added){
				// Commit changes to database
				endTransaction(con);
				
				log.info(String.format("New user [%s] successfully registered", user));
				return true;				
			} else {
				// Don't commit changes to database
				doRollback(con);
				enableAutoCommit(con);
				
				log.info(String.format("New user [%s] failed to register", user));
				return false;
			}
		} catch (Exception e){	
			doRollback(con);
			enableAutoCommit(con);
			log.error(e.getMessage(), e);
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	
	/**
	 * Adds an activation code to the database for a given user
	 * 
	 * @param user the user to add an activation code to the database for
	 * @param con the database connection maintaining the transaction
	 * @param code the new activation code to add
	 * @return true iff the new activation code is added to the database
	 * @author Todd Elvers
	 */
	protected static boolean addCode(Connection con, User user, String code) {
		try {			
			// Add a new entry to the VERIFY table
			CallableStatement procedure = con.prepareCall("{CALL AddCode(?, ?)}");
			procedure.setLong(1, user.getId());
			procedure.setString(2, code);

			// Apply update to database and check to be sure at least 1 row was modified
			int rowsModified = procedure.executeUpdate();						
			if(rowsModified == 0){
				log.debug(String.format("Adding activation record failed for [%s]", user.getFullName()));
				return false;
			}
			
			log.info(String.format("New email activation code [%s] added to VERIFY for user [%s]", code, user.getFullName()));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return false;
	}
	
	/**
	 * Adds an entry to the community request table in a transaction-safe manner, only used
	 * when adding a new member to the database
	 * 
	 * @param con the connection maintaining the database transaction
	 * @param user user initiating the request
	 * @param message the message to the leaders of the community
	 * @param communityId the id of the community this request is for
	 * @return true iff an entry is added to the community request table
	 * @author Todd Elvers
	 */
	private static boolean addCommunityRequest(Connection con, User user, long communityId, String message ) {
		try {
			
			// Add a new entry to the VERIFY table
			CallableStatement procedure = con.prepareCall("{CALL AddCommunityRequest(?, ?, ?, ?)}");
			procedure.setLong(1, user.getId());
			procedure.setLong(2, communityId);
			procedure.setString(3, UUID.randomUUID().toString());
			procedure.setString(4, message);

			// Apply update to database and check to be sure at least 1 row was modified
			int rowsModified = procedure.executeUpdate();
			if(rowsModified == 0){
				log.debug(String.format("Add invitation record failed for user [%s] on community %d", user, communityId));
				return false;				
			}
			
			log.debug(String.format("Added invitation record for user [%s] on community %d", user, communityId));			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}
	
	/**
	 * Adds a request to join a community for a given user and community
	 * 
	 * @param user the user who wants to join a community
	 * @param communityId the id of the community the user wants to join
	 * @param code the code used in hyperlinks to safely reference this request
	 * @param message the message the user wrote to the leaders of this community
	 * @return true iff an entry is added to the community request table
	 * @author Todd Elvers
	 */
	public static boolean addCommunityRequest(User user, long communityId, String code, String message) {
		Connection con = null;
		try {
			con = dataPool.getConnection();
			return Database.addCommunityRequest(con, user, communityId, message); 			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Database.safeClose(con);
		}
	}	
	
	/**
	 * Checks an activation code provided by the user against the code in table VERIFY 
	 * and if they match, removes that entry in VERIFY, and adds an entry to USER_ROLES
	 * 
	 * @param codeFromUser the activation code provided by the user
	 * @author Todd Elvers
	 */
	public static long redeemActivationCode(String codeFromUser){
		Connection con = null;
		try {
			
			con = dataPool.getConnection();

			CallableStatement procedure = con.prepareCall("{CALL RedeemCode(?, ?)}");
			procedure.setString(1, codeFromUser);
			procedure.registerOutParameter(2, java.sql.Types.BIGINT);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				return -1;
			}
			
			long userId = procedure.getLong(2);
			log.info(String.format("Activation code %s redeemed by user %d", codeFromUser, userId));
			return userId;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Database.safeClose(con);
		}
		return -1;
	}
	
	/**
	 * Retrieves an unregistered user from the database given their user_id.
	 * This is a helper method for user registration and shouldn't be used
	 * anywhere else.
	 * 
	 * @param id The id of the unregistered user to retrieve
	 * @return the unregistered User object if one exists, null otherwise
	 * @author Todd Elvers
	 */
	public static User getUnregisteredUser(long id) {
		Connection con = null;

		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con
					.prepareCall("{CALL GetUnregisteredUserById(?)}");
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
			Database.safeClose(con);
		}
		return null;
	}
	
	
	/**
	 * Adds a user to their community and deletes their entry in the community request table
	 *  
	 * @param userId the user id of the newly registered user
	 * @param communityId the community the newly registered user is joining
	 * @return true iff the user was successfully added to USER_ROLES and USER_ASSOC
	 * @author Todd Elvers
	 */
	public static boolean approveUser(long userId, long communityId){
		Connection con = null;
		try {
			
			con = dataPool.getConnection();

			CallableStatement procedure = con.prepareCall("{CALL ApproveUser(?, ?)}");
			procedure.setLong(1, userId);
			procedure.setLong(2, communityId);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified > 0) {
				log.info(String.format("User [%d] approved for community [%d]", userId, communityId));
				return true;
			}			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Database.safeClose(con);
		}
		return false;
	}
	
	
	/**
	 * Deletes a community request given a user id and community id, and if
	 * the user is unregistered, they are also removed from the users table
	 * 
	 * @param userId the user id associated with the invite
	 * @param communityId the communityId associated with the invite
	 * @return true iff an entry was deleted from the community request table
	 * @author Todd Elvers
	 */
	public static boolean declineUser(long userId, long communityId){
		Connection con = null;
		try {
			
			con = dataPool.getConnection();

			CallableStatement procedure = con.prepareCall("{CALL DeclineUser(?, ?)}");
			procedure.setLong(1, userId);
			procedure.setLong(2, communityId);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified > 0) {
				log.info(String.format("User [%d] declined by community [%d]", userId, communityId));
				return true;
			}			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Database.safeClose(con);
		}
		return false;
	}
	
	/**
	 * Gets the users that are the leaders of a given space
	 * 
	 * @param spaceId the id of the space to get the leaders of
	 * @return a list of leaders of the given space
	 * @author Todd Elvers
	 */
	public static List<User> getLeadersOfSpace(long spaceId){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
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
			Database.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Retrieves a community request from the database given the user id
	 * 
	 * @param userId the user id of the request to retrieve
	 * @return The request associated with the user id
	 * @author Todd Elvers
	 */
	public static CommunityRequest getCommunityRequest(long userId){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetCommunityRequestById(?)}");
			procedure.setLong(1, userId);					
			ResultSet results = procedure.executeQuery();
			
			if(results.next()){
				CommunityRequest req = new CommunityRequest();
				req.setUserId(results.getLong("user_id"));
				req.setCommunityId(results.getLong("community"));
				req.setCode(results.getString("code"));
				req.setMessage(results.getString("message"));
				req.setCreateDate(results.getTimestamp("created"));
				return req;
			}			
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Retrieves a community request from the database given a code
	 * 
	 * @param code the code of the request to retrieve
	 * @return The request object associated with the request code
	 * @author Todd Elvers
	 */
	public static CommunityRequest getCommunityRequest(String code){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetCommunityRequestByCode(?)}");
			procedure.setString(1, code);					
			ResultSet results = procedure.executeQuery();
			
			if(results.next()){
				CommunityRequest req = new CommunityRequest();
				req.setUserId(results.getLong("user_id"));
				req.setCommunityId(results.getLong("community"));
				req.setCode(results.getString("code"));
				req.setMessage(results.getString("message"));
				req.setCreateDate(results.getTimestamp("created"));
				return req;
			}			
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Adds a request to have a user's password reset; prevents duplicate entries
	 * in the password reset table by first deleting any previous entries for this
	 * user_id
	 * 
	 * @param userId the id of the user requesting the password reset
	 * @param code the code to add to the password reset table (for hyperlinking)
	 * @return true iff the tuple (user_id, code) is added to the password reset table
	 * @author Todd Elvers 
	 */
	public static boolean addPassResetRequest(long userId, String code){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL AddPassResetRequest(?, ?)}");
			procedure.setLong(1, userId);
			procedure.setString(2, code);
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				return false;
			}

			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	
	/**
	 * Gets the user_id associated with the given code and deletes
	 * the corresponding entry in the password reset table
	 * 
	 * @param code the UUID to check the password reset table with
	 * @return the id of the user requesting the password reset if the
	 * code provided matches one in the password reset table, -1 otherwise
	 * @author Todd Elvers
	 */
	public static long redeemPassResetRequest(String code){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL RedeemPassResetRequestByCode(?, ?)}");
			procedure.setString(1, code);
			procedure.registerOutParameter(2, java.sql.Types.BIGINT);
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				return -1;
			} else {
				return procedure.getLong(2);
			}

		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return -1;
	}
	
	
	/**
	 * Sets the password of a user, given their user_id; this method requires
	 * the password be supplied in plaintext
	 * 
	 * @param user_id the id of the user to set the new password for
	 * @param password the new password
	 * @return true iff the password was successfully updated
	 * @author Todd Elvers
	 */
	public static boolean setPasswordByUserId(long user_id, String password){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL SetPasswordByUserId(?, ?)}");
			procedure.setLong(1, user_id);
			procedure.setString(2, Hash.hashPassword(password));
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				return false;
			}

			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Retrieves a user from the database given the email address
	 * 
	 * @param email The email of the user to retrieve
	 * @return The user object associated with the user
	 */
	public static User getUser(String email){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
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
			Database.safeClose(con);
		}
		
		return null;
	}
	
		
	/**
	 * Returns the (hashed) password of the given user.
	 * @param userId the user ID of the user to get the password of
	 * @return The (hashed) password for the user
	 */
	public static String getPassword(long userId) {
		Connection con = null;
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetPasswordById(?)}");
			procedure.setLong(1, userId);
			ResultSet results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getString("password");
			}
			
		} catch (Exception e) {			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Retrieves a user from the database given the user's id
	 * 
	 * @param id the id of the user to get
	 * @return The user object associated with the user
	 */
	public static User getUser(long id){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
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
			Database.safeClose(con);
		}
		
		return null;
	}
		
	/**
	 * 
	 * @param benchId The id of the benchmark to retrieve
	 * @param userId The id of the user requesting the benchmark data (used for permission check)
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark getBenchmark(long benchId, long userId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			if(Database.canUserSeeBench(benchId, userId)) {
				CallableStatement procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");
				procedure.setLong(1, benchId);					
				ResultSet results = procedure.executeQuery();
				
				if(results.next()){
					Benchmark b = new Benchmark();
					b.setId(results.getLong("bench.id"));
					b.setUserId(results.getLong("bench.user_id"));
					b.setName(results.getString("bench.name"));
					b.setUploadDate(results.getTimestamp("bench.uploaded"));
					b.setPath(results.getString("bench.path"));
					b.setDescription(results.getString("bench.description"));
					b.setDownloadable(results.getBoolean("bench.downloadable"));
					
					BenchmarkType t = new BenchmarkType();
					t.setId(results.getLong("types.id"));
					t.setCommunityId(results.getLong("types.community"));
					t.setDescription(results.getString("types.description"));
					t.setName(results.getString("types.name"));
					t.setProcessorPath(results.getString("types.processor_path"));
					
					b.setType(t);
					return b;
				}
			}											
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**	 
	 * @param communityId The id of the community to retrieve all types for
	 * @return A list of all benchmark types the community owns
	 * @author Tyler Jensen
	 */
	public static List<BenchmarkType> getBenchTypesForCommunity(long communityId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();					
			CallableStatement procedure = con.prepareCall("{CALL GetBenchTypesByCommunity(?)}");
			procedure.setLong(1, communityId);
			ResultSet results = procedure.executeQuery();
			List<BenchmarkType> types = new LinkedList<BenchmarkType>();
			
			while(results.next()){							
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("id"));
				t.setCommunityId(results.getLong("community"));
				t.setDescription(results.getString("description"));
				t.setName(results.getString("name"));
				t.setProcessorPath(results.getString("processor_path"));
				types.add(t);						
			}				
			
			return types;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**	 
	 * @param benchTypeId The id of the bentch type to retrieve
	 * @return The corresponding benchmark type
	 * @author Tyler Jensen
	 */
	public static BenchmarkType getBenchTypeById(long benchTypeId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();					
			CallableStatement procedure = con.prepareCall("{CALL GetBenchTypeById(?)}");
			procedure.setLong(1, benchTypeId);
			ResultSet results = procedure.executeQuery();			
			
			if(results.next()){							
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("id"));
				t.setCommunityId(results.getLong("community"));
				t.setDescription(results.getString("description"));
				t.setName(results.getString("name"));
				t.setProcessorPath(results.getString("processor_path"));
				return t;					
			}							
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets very basic information about a job (but not about
	 * any of its pairs) use getPairsForJob for those details
	 * @param jobId The id of the job to get information for
	 * @param userId The id of the user requesting the job info (used for permission check)
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	public static Job getJob(long jobId, long userId) {
		Connection con = null;			
		
		try {
			if(Database.canUserSeeJob(jobId, userId)) {
				con = dataPool.getConnection();		
				CallableStatement procedure = con.prepareCall("{CALL GetJobById(?)}");
				procedure.setLong(1, jobId);					
				ResultSet results = procedure.executeQuery();
				
				if(results.next()){
					Job j = new Job();
					j.setId(results.getLong("id"));
					j.setUserId(results.getLong("user_id"));
					j.setName(results.getString("name"));								
					j.setDescription(results.getString("description"));	
					j.setSubmitted(results.getTimestamp("submitted"));
					j.setFinished(results.getTimestamp("finished"));
					j.setStatus(results.getString("status"));
					j.setTimeout(results.getLong("timeout"));
					return j;
				}			
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
		
	/**
	 * Gets all job pairs for the given job 
	 * @param jobId The id of the job to get pairs for
	 * @param userId The id of the user requesting the pairs (used for permission check)
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	public static List<JobPair> getPairsForJob(long jobId, long userId) {
		Connection con = null;			
		
		try {
			if(Database.canUserSeeJob(jobId, userId)) {
				con = dataPool.getConnection();		
				CallableStatement procedure = con.prepareCall("{CALL GetJobPairByJob(?)}");
				procedure.setLong(1, jobId);					
				ResultSet results = procedure.executeQuery();
				List<JobPair> returnList = new LinkedList<JobPair>();
				
				while(results.next()){
					JobPair jp = new JobPair();
					jp.setId(results.getLong("id"));
					jp.setResult(results.getString("result"));
					jp.setStatus(results.getString("status"));
					jp.setStartDate(results.getTimestamp("start"));
					jp.setEndDate(results.getTimestamp("stop"));
					
					Configuration c = new Configuration();
					c.setId(results.getLong("config_id"));
					c.setDescription(results.getString("config_desc"));
					c.setName(results.getString("config_name"));
					
					Benchmark b = new Benchmark();
					b.setId(results.getLong("bench_id"));
					b.setDescription(results.getString("bench_desc"));
					b.setName(results.getString("bench_name"));
					
					Solver s = new Solver();
					s.setId(results.getLong("solver_id"));
					s.setDescription(results.getString("solver_desc"));
					s.setName(results.getString("solver_name"));	
					s.addConfiguration(c);
					c.setSolverId(s.getId());
					
					jp.setBenchmark(b);
					jp.setSolver(s);
					
					returnList.add(jp);
				}			
				
				return returnList;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param solverId The id of the solver to retrieve
	 * @param userId The id of the user requesting the solver (used for permission check)	 
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	public static Solver getSolver(long solverId, long userId) {
		Connection con = null;			
		
		try {
			if(Database.canUserSeeSolver(solverId, userId)) {
				con = dataPool.getConnection();		
				CallableStatement procedure = con.prepareCall("{CALL GetSolverById(?)}");
				procedure.setLong(1, solverId);					
				ResultSet results = procedure.executeQuery();
				
				if(results.next()){
					Solver s = new Solver();
					s.setId(results.getLong("id"));
					s.setUserId(results.getLong("user_id"));
					s.setName(results.getString("name"));
					s.setUploadDate(results.getTimestamp("uploaded"));
					s.setPath(results.getString("path"));
					s.setDescription(results.getString("description"));
					s.setDownloadable(results.getBoolean("downloadable"));
					return s;
				}			
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Updates the first name of a user in the database with the 
	 * given user ID
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the first name will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
	public static boolean updateFirstName(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateFirstName(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			int result = procedure.executeUpdate();			
			if(result == 1) {
				log.info(String.format("User [%d] updated first name to [%s]", userId, newValue));
				return true;
			}				
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
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
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateLastName(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("User [%d] updated last name to [%s]", userId, newValue));
				return true;
			}			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the email address of a user in the database with the 
	 * given user ID
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the email address will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
	public static boolean updateEmail(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateEmail(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("User [%d] updated e-mail address to [%s]", userId, newValue));
				return true;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the institution of a user in the database with the 
	 * given user ID
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the institution will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
	public static boolean updateInstitution(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateInstitution(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setString(2, newValue);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("User [%d] updated institution to [%s]", userId, newValue));
				return true;
			}			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the password of a user in the database with the 
	 * given user ID. Hashes the password before updating, so
	 * the password should be supplied in plain-text.
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the password will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
	public static boolean updatePassword(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdatePassword(?, ?)}");
			procedure.setLong(1, userId);
			String hashedPassword = Hash.hashPassword(newValue);
			procedure.setString(2, hashedPassword);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("User [%d] updated password", userId));
				return true;
			}			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the name of a space with the given space id
	 * 
	 * @param spaceId the id of the space to update
	 * @param newName the new name to update the space with
	 * @return true iff the update succeeds on exactly one entry
	 * @author Tyler Jensen
	 */
	public static boolean updateSpaceName(long spaceId, String newName){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceName(?, ?)}");
			procedure.setLong(1, spaceId);					
			procedure.setString(2, newName);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("Space [%d] updated name to [%s]", spaceId, newName));
				return true;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	
	/**
	 * Updates the details of a solver
	 * 
	 * @param id the id of the solver to update
	 * @param name the new name to apply to the solver
	 * @param description the new description to apply to the solver
	 * @param isDownloadable boolean indicating whether or not this solver is downloadable
	 * @return true iff the update was applied successfully and a row was modified, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateSolverDetails(long id, String name, String description, boolean isDownloadable){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateSolverDetails(?, ?, ?, ?)}");
			procedure.setLong(1, id);
			procedure.setString(2, name);
			procedure.setString(3, description);
			procedure.setBoolean(4, isDownloadable);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				log.debug(String.format("Solver [id=%d] failed to be updated.", id));
				return false;
			}
			
			log.info(String.format("Solver [id=%d] was successfully updated.", id));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		log.debug(String.format("Solver [id=%d] failed to be updated.", id));
		return false;
	}
	
	
	/**
	 * Deletes a solver from the database (cascading deletes handle all
	 * dependencies)
	 * 
	 * @param id the id of the solver to delete
	 * @return true iff the solver was deleted from the database, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean deleteSolver(long id){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL DeleteSolverById(?)}");
			procedure.setLong(1, id);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				log.debug(String.format("Deletion of solver [id=%d] failed.", id));
				return false;
			}
			
			log.debug(String.format("Deletion of solver [id=%d] was successful.", id));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		log.debug(String.format("Deletion of solver [id=%d] failed.", id));
		return false;
	}
	
	
	/**
	 * Updates the details of a benchmark
	 * 
	 * @param id the id of the benchmark to update
	 * @param name the new name to apply to the benchmark
	 * @param description the new description to apply to the benchmark
	 * @param isDownloadable boolean indicating whether or not this benchmark is downloadable
	 * @param benchTypeId the new benchmark type to apply to the benchmark 
	 * @return true iff the update was applied successfully and a row was modified, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateBenchmarkDetails(long id, String name, String description, boolean isDownloadable, long benchTypeId){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchmarkDetails(?, ?, ?, ?, ?)}");
			procedure.setLong(1, id);
			procedure.setString(2, name);
			procedure.setString(3, description);
			procedure.setBoolean(4, isDownloadable);
			procedure.setLong(5, benchTypeId);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				log.debug(String.format("Benchmark [id=%d] failed to be updated.", id));
				return false;
			}
			
			log.debug(String.format("Benchmark [id=%d] was successfully updated.", id));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		log.debug(String.format("Benchmark [id=%d] failed to be updated.", id));
		return false;
	}
	
	
	/**
	 * Deletes a benchmark from the database (cascading deletes handle all
	 * dependencies)
	 * 
	 * @param id the id of the benchmark to delete
	 * @return true iff the benchmark was deleted from the database, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean deleteBenchmark(long id){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL DeleteBenchmarkById(?)}");
			procedure.setLong(1, id);
			
			int rowsModified = procedure.executeUpdate();
			if (rowsModified == 0) {
				log.debug(String.format("Deletion of benchmark [id=%d] failed.", id));
				return false;
			}
			
			log.debug(String.format("Deletion of benchmark [id=%d] was successful.", id));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		log.debug(String.format("Deletion of benchmark [id=%d] failed.", id));
		return false;
	}
	
	
	/**
	 * Get all websites associated with a given user.
	 * 
	 * @param spaceId the id of the space to update
	 * @param newDesc the new description to update the space with
	 * @return true iff the update succeeds on exactly one entry
	 * @author Tyler Jensen
	 */
	public static boolean updateSpaceDescription(long spaceId, String newDesc){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateSpaceDescription(?, ?)}");
			procedure.setLong(1, spaceId);					
			procedure.setString(2, newDesc);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("Space [%d] updated description to [%s]", spaceId, newDesc));
				return true;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the name of a benchmark type with the given type id
	 * 
	 * @param typeId the id of the bench type to update
	 * @param newName the new name to update the type with
	 * @return true iff the update succeeds on exactly one entry
	 * @author Tyler Jensen
	 */
	public static boolean updateBenchTypeName(long typeId, String newName){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchTypeName(?, ?)}");
			procedure.setLong(1, typeId);					
			procedure.setString(2, newName);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("BenchType [%d] updated name to [%s]", typeId, newName));
				return true;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the description of a benchmark type with the given type id
	 * 
	 * @param typeId the id of the bench type to update
	 * @param newDesc the new description to update the type with
	 * @return true iff the update succeeds on exactly one entry
	 * @author Tyler Jensen
	 */
	public static boolean updateBenchTypeDescription(long typeId, String newDesc){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchTypeDescription(?, ?)}");
			procedure.setLong(1, typeId);					
			procedure.setString(2, newDesc);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("Bench Type [%d] updated description to [%s]", typeId, newDesc));
				return true;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the processor path of a benchmark type with the given type id
	 * 
	 * @param typeId the id of the bench type to update
	 * @param newPath the new processor path to update the type with
	 * @return true iff the update succeeds on exactly one entry
	 * @author Tyler Jensen
	 */
	public static boolean updateBenchTypePath(long typeId, String newPath){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchTypePath(?, ?)}");
			procedure.setLong(1, typeId);					
			procedure.setString(2, newPath);
			
			int result = procedure.executeUpdate();
			if(result == 1) {
				log.info(String.format("Bench Type [%d] updated processor path to [%s]", typeId, newPath));
				return true;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Returns a list of websites associated with the given entity based on its type
	 * @param id The id of the entity to get websites for
	 * @param webType The type of entity to get websites for (solver, user or space)
	 * @return A list of websites associated with the entity
	 * @author Tyler Jensen
	 */
	public static List<Website> getWebsites(long id, WebsiteType webType) {
		Connection con = null;
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = null;
			
			switch(webType) {
				case USER:
					procedure = con.prepareCall("{CALL GetWebsitesByUserId(?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL GetWebsitesBySpaceId(?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL GetWebsitesBySolverId(?)}");
					break;
				default:
					throw new Exception("Unhandled value for WebsiteType");
			}
			
			procedure.setLong(1, id);
			
			ResultSet results = procedure.executeQuery();
			List<Website> websites = new LinkedList<Website>();
			
			while (results.next()) {
				Website w = new Website();
				w.setId(results.getLong("id"));
				w.setName(results.getString("name"));
				w.setUrl(results.getString("url"));
				websites.add(w);				
			}
			
			return websites;
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets spaces that are a child of the root space
	 * 
	 * @return A list of child spaces belonging to the root space.
	 * @author Todd Elvers
	 */
	public static List<Space> getCommunities() {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSubSpacesOfRoot}");
			ResultSet results = procedure.executeQuery();
			List<Space> commSpaces = new LinkedList<Space>();
			
			while(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getLong("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));				
				commSpaces.add(s);
			}			
						
			return commSpaces;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Gets the list of benchmark types
	 * 
	 * @return the list of benchmark types
	 * @author Todd Elvers
	 */
	public static List<BenchmarkType> getBenchmarkTypes(){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetAllBenchTypes}");
			ResultSet results = procedure.executeQuery();
			List<BenchmarkType> benchTypes = new LinkedList<BenchmarkType>();
			
			while(results.next()){
				BenchmarkType bt = new BenchmarkType();
				bt.setId(results.getLong("id"));
				bt.setName(results.getString("name"));
				bt.setDescription(results.getString("description"));
				bt.setProcessorPath((results.getString("processor_path")));
				bt.setCommunityId((results.getLong("community")));
				benchTypes.add(bt);
			}
			
			return benchTypes;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
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
			con = dataPool.getConnection();		
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
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets a space with minimal information (only details about the space itself)
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of the user requesting the space (used for permissions check)
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space getSpace(long spaceId, long userId) {
		Connection con = null;			
		
		try {
			if(Database.canUserSeeSpace(spaceId, userId)) {
				con = dataPool.getConnection();		
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
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets a space with minimal information (only details about the space itself)
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of the user requesting the space (used for permissions check)
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space getCommunityDetails(long id) {
		Connection con = null;			
		
		try {			
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetCommunityById(?)}");
			procedure.setLong(1, id);					
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
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets the name of a community by Id - helper method to work around
	 * permissions for this special case
	 * 
	 * @param spaceId the id of the community to get the name of
	 * @return the name of the community
	 * @author Todd Elvers
	 */
	public static String getSpaceName(long spaceId){
		Connection con = null;			
		
		try {
				con = dataPool.getConnection();		
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
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets a space with detailed information (solvers, benchmarks, jobs and user belonging
	 * to the space are also populated)
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of user requesting the space (used for permission check)
	 * @return A space object consisting of detailed information about the space
	 * @author Tyler Jensen
	 */
	public static Space getSpaceDetails(long spaceId, long userId) {		
		try {
			if(Database.canUserSeeSpace(spaceId, userId)) {
				Space s = Database.getSpace(spaceId, userId);
				s.setUsers(Database.getSpaceUsers(spaceId));
				s.setBenchmarks(Database.getSpaceBenchmarks(spaceId));
				s.setSolvers(Database.getSpaceSolvers(spaceId));
				s.setJobs(Database.getSpaceJobs(spaceId));
				s.setSubspaces(Database.getSubSpaces(spaceId, userId));
													
				return s;
			}
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
	public static List<User> getSpaceUsers(long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
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
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param spaceId The id of the space to get benchmarks for
	 * @return A list of all benchmarks belonging to the space
	 * @author Tyler Jensen
	 */
	public static List<Benchmark> getSpaceBenchmarks(long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getLong("bench.id"));
				b.setName(results.getString("bench.name"));
				b.setUploadDate(results.getTimestamp("bench.uploaded"));
				b.setDescription(results.getString("bench.description"));
				b.setDownloadable(results.getBoolean("bench.downloadable"));	
				
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("types.id"));
				t.setCommunityId(results.getLong("types.community"));
				t.setDescription(results.getString("types.description"));
				t.setName(results.getString("types.name"));
				t.setProcessorPath(results.getString("types.processor_path"));
				
				b.setType(t);
				benchmarks.add(b);
			}			
						
			return benchmarks;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param spaceId The id of the space to get jobs for
	 * @return A list of jobs existing directly in the space
	 * @author Tyler Jensen
	 */
	public static List<Job> getSpaceJobs(long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceJobsById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();
			
			while(results.next()){
				Job j = new Job();
				j.setId(results.getLong("id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));
				j.setSubmitted(results.getTimestamp("submitted"));
				j.setStatus(results.getString("status"));
				jobs.add(j);
			}			
						
			return jobs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param spaceId The id of the space to get solvers for
	 * @return A list of all solvers belonging directly to the space
	 * @author Tyler Jensen
	 */
	public static List<Solver> getSpaceSolvers(long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceSolversById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Solver> solvers = new LinkedList<Solver>();
			
			while(results.next()){
				Solver s = new Solver();
				s.setId(results.getLong("id"));
				s.setName(results.getString("name"));				
				s.setUploadDate(results.getTimestamp("uploaded"));
				s.setDescription(results.getString("description"));
				s.setDownloadable(results.getBoolean("downloadable"));
				solvers.add(s);
			}			
						
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Inserts a benchmark type into the database
	 * @param type The benchmark type to add to the database
	 * @return True iff the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addBenchmarkType(BenchmarkType type) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = null;			
			procedure = con.prepareCall("{CALL AddBenchmarkType(?, ?, ?, ?)}");			
			procedure.setString(1, type.getName());
			procedure.setString(2, type.getDescription());
			procedure.setString(3, type.getProcessorPath());
			procedure.setLong(4, type.getCommunityId());
			int result = procedure.executeUpdate();
			
			if(result == 1) {
				log.info(String.format("Added new benchmark type with name [%s] for community [%d]", type.getName(), type.getCommunityId()));
				return true;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
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
	public static boolean addSpacesWithBenchmarks(Space parent, long userId) {
		Connection con = null;
		
		try {
			// We'll be doing everything with a single connection so we can roll back if needed
			con = dataPool.getConnection();
			beginTransaction(con);
			
			// For each subspace...
			for(Space s : parent.getSubspaces()) {
				// Apply the recursive algorithm to add each subspace
				Database.traverseSpace(con, s, parent.getId(), userId);
			}			
			
			// Add and new benchmarks in the space to the database
			Database.addBenchmarks(parent.getBenchmarks(), parent.getId());
			
			// We're done (notice that 'parent' is never added because it should already exist)
			endTransaction(con);			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			doRollback(con);
			enableAutoCommit(con);
			Database.safeClose(con);
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
	private static void traverseSpace(Connection con, Space space, long parentId, long userId) throws Exception {
		// Add the new space to the database and get it's ID		
		long spaceId = Database.addSpace(con, space, parentId, userId);
		
		for(Space s : space.getSubspaces()) {
			// Recursively go through and add all of it's subspaces with itself as the parent
			Database.traverseSpace(con, s, spaceId, userId);
		}			
		
		// Finally, add the benchmarks in the space to the database
		Database.addBenchmarks(con, space.getBenchmarks(), spaceId);
	}
	
	/**
	 * Adds the list of benchmarks to the database and associates them with the given spaceId
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addBenchmarks(List<Benchmark> benchmarks, long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			Database.addBenchmarks(con, benchmarks, spaceId);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
		
	/**
	 * Internal method which adds the list of benchmarks to the database and associates them with the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	private static void addBenchmarks(Connection con, List<Benchmark> benchmarks, long spaceId) throws Exception {		
		for(Benchmark b : benchmarks) {
			if(!Database.addBenchmark(con, b, spaceId)) {
				throw new Exception(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
			}
		}
		
		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
	}	
	
	/**
	 * Internal method which adds a single benchmark to the database under the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	private static boolean addBenchmark(Connection con, Benchmark benchmark, long spaceId) throws SQLException {				
		CallableStatement procedure = null;			
		procedure = con.prepareCall("{CALL AddBenchmark(?, ?, ?, ?, ?, ?)}");
		procedure.setString(1, benchmark.getName());		
		procedure.setString(2, benchmark.getPath());
		procedure.setBoolean(3, benchmark.isDownloadable());
		procedure.setLong(4, benchmark.getUserId());
		procedure.setLong(5, benchmark.getType().getId());
		procedure.setLong(6, spaceId);
		int result = procedure.executeUpdate();
		
		if(result == 1) {
			log.debug(String.format("Added new benchmark with name [%s] to space [%d]", benchmark.getName(), spaceId));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Adds a single benchmark to the database under the given spaceId
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addBenchmark(Benchmark benchmark, long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			return Database.addBenchmark(con, benchmark, spaceId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Adds a new website associated with the specified entity
	 * @param id The ID of the entity the website is associated with
	 * @param url The URL for the website
	 * @param name The display name of the website
	 * @param type Which type of entity to associate the website with (space, solver, user)
	 * @return True iff the addition was successful
	 * @author Tyler Jensen
	 */
	public static boolean addWebsite(long id, String url, String name, WebsiteType type) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = null;
			
			switch(type) {
				case USER:
					procedure = con.prepareCall("{CALL AddUserWebsite(?, ?, ?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL AddSpaceWebsite(?, ?, ?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL AddSolverWebsite(?, ?, ?)}");
					break;			
				default:
					throw new Exception("Unhandled value for WebsiteType");				
			}			
			
			procedure.setLong(1, id);
			procedure.setString(2, url);
			procedure.setString(3, name);
			int result = procedure.executeUpdate();
			
			if(result == 1) {
				log.info(String.format("Added new website of with [%s] id [%d] with name [%s] and url [%s]", type.toString(), id, name, url));
				return true;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Deletes the website associated with the given website ID.
	 * @param websiteId the ID of the website to delete
	 * @return true iff the delete is successful
	 * @author Skylar Stark
	 */
	public static boolean deleteUserWebsite(long websiteId, long userId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL DeleteUserWebsite(?, ?)}");
			procedure.setLong(1, websiteId);
			procedure.setLong(2, userId);
			int result = procedure.executeUpdate();
			
			if(result == 1) {
				log.info(String.format("Website [%d] deleted by [%d]", websiteId, userId));
				return true;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
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
	public static long addSpace(Space s, long parentId, long userId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			
			beginTransaction(con);	
			// Add space is a multi-step process, so we need to use a transaction
			long newSpaceId = Database.addSpace(con, s, parentId, userId);
			endTransaction(con);			
			
			return newSpaceId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			doRollback(con);
			enableAutoCommit(con);
			Database.safeClose(con);
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
	private static long addSpace(Connection con, Space s, long parentId, long userId) throws Exception {			
		// Add the default permission for the space to the database			
		long defaultPermId = Database.addPermission(s.getPermission(), con);
		
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
		long userPermId = Database.addPermission(new Permission(true), con);
		
		// Add the adding user to the space with the maximal permissions
		CallableStatement procAddUser = con.prepareCall("{CALL AddUserToSpace(?, ?, ?, ?)}");			
		procAddUser.setLong(1, userId);
		procAddUser.setLong(2, newSpaceId);
		procAddUser.setLong(3, newSpaceId);
		procAddUser.setLong(4, userPermId);
		
		procAddUser.executeUpdate();
		endTransaction(con);
		
		// Log the new addition and ensure we return at least 3 affected rows (the two added permissions are self-checking)
		log.info(String.format("New space with name [%s] added by user [%d] to space [%d]", s.getName(), userId, parentId));
		return newSpaceId;
	}
	
	/**
	 * Adds a new permission record to the database. This is an internal helper method.
	 * @param p The permission to add
	 * @param con The connection to add the permission with
	 * @return The ID of the inserted record
	 * @author Tyler Jensen
	 */
	protected static long addPermission(Permission p, Connection con) throws Exception {
		CallableStatement procDefaultPerm = con.prepareCall("{CALL AddPermissions(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
		procDefaultPerm.setBoolean(1, p.canAddSolver());
		procDefaultPerm.setBoolean(2, p.canAddBenchmark());
		procDefaultPerm.setBoolean(3, p.canAddUser());
		procDefaultPerm.setBoolean(4, p.canAddSpace());
		procDefaultPerm.setBoolean(5, p.canRemoveSolver());
		procDefaultPerm.setBoolean(6, p.canRemoveBench());
		procDefaultPerm.setBoolean(7, p.canRemoveSpace());
		procDefaultPerm.setBoolean(8, p.canRemoveUser());
		procDefaultPerm.setBoolean(9, p.isLeader());
		procDefaultPerm.registerOutParameter(10, java.sql.Types.BIGINT);
		
		if (procDefaultPerm.executeUpdate() != 1) {
			throw new Exception("No rows were affected. Expected 1 affected row");
		}
				
		return procDefaultPerm.getLong(10);		
	}
	
	/**
	 * Retrieves the user's maximum set of permissions in a space.
	 * @param userId The user to get permissions for	
	 * @param spaceId The id of the space to get the user's permissions on
	 * @return A permission object containing the user's permission on the space. Null if the user is not apart of the space.
	 */
	public static Permission getUserPermissions(long userId, long spaceId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetUserPermissions(?, ?)}");
			procedure.setLong(1, userId);					
			procedure.setLong(2, spaceId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {				
				Permission p = new Permission();
				p.setAddBenchmark(results.getBoolean("add_bench"));
				p.setAddSolver(results.getBoolean("add_solver"));
				p.setAddSpace(results.getBoolean("add_space"));
				p.setAddUser(results.getBoolean("add_user"));
				p.setRemoveBench(results.getBoolean("remove_bench"));
				p.setRemoveSolver(results.getBoolean("remove_solver"));
				p.setRemoveSpace(results.getBoolean("remove_space"));
				p.setRemoveUser(results.getBoolean("remove_user"));
				p.setLeader(results.getBoolean("is_leader"));
				
				if(results.wasNull()) {
					/* If the permission doesn't exist we always get a result
					but all of it's values are null, so here we check for a 
					null result and return null */
					return null;
				}
				
				return p;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;		
	}
	
	/**
	 * Checks to see if the user has access to the solver in some way. More specifically,
	 * this checks if the user belongs to any space the solver belongs to.
	 * @param solverId The solver to check if the user can see
	 * @param userId The user that is requesting to view the given solver	
	 * @return True if the user can somehow see the solver, false otherwise
	 */
	public static boolean canUserSeeSolver(long solverId, long userId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewSolver(?, ?)}");
			procedure.setLong(1, solverId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Checks to see if the user has access to the benchmark in some way. More specifically,
	 * this checks if the user belongs to any space the benchmark belongs to.
	 * @param benchId The benchmark to check if the user can see
	 * @param userId The user that is requesting to view the given benchmark	 * 
	 * @return True if the user can somehow see the benchmark, false otherwise
	 */
	public static boolean canUserSeeBench(long benchId, long userId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewBenchmark(?, ?)}");
			procedure.setLong(1, benchId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;				
	}
	
	/**
	 * Checks to see if the user has access to the job in some way. More specifically,
	 * this checks if the user belongs to any space the job belongs to.
	 * @param jobId The job to check if the user can see
	 * @param userId The user that is requesting to view the given job 
	 * @return True if the user can somehow see the job, false otherwise
	 */
	public static boolean canUserSeeJob(long jobId, long userId){		
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewJob(?, ?)}");
			procedure.setLong(1, jobId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Checks to see if the user belongs to the given space.
	 * @param spaceId The space to check if the user can see
	 * @param userId The user that is requesting to view the given space
	 * @return True if the user belongs to the space, false otherwise
	 */
	public static boolean canUserSeeSpace(long spaceId, long userId) {		
		if(spaceId <= 1) {
			// Can always see root space
			return true;
		}
		
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL CanViewSpace(?, ?)}");
			procedure.setLong(1, spaceId);					
			procedure.setLong(2, userId);
			ResultSet results = procedure.executeQuery();
		
			if(results.first()) {
				return results.getBoolean(1);
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;		
	}
	
	/**
	 * Creates a new historical record in the logins table which keeps track of all user logins.
	 * @param userId The user that has logged in
	 * @param ipAddress The IP address the user logged in from
	 * @param browser The browser/agent information about the browser the user logged in with
	 */
	public static void addLoginRecord(long userId, String ipAddress, String browser) {
		Connection con = null;		
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL LoginRecord(?, ?, ?)}");
			procedure.setLong(1, userId);
			procedure.setString(2, ipAddress);
			procedure.setString(3, browser);			
			procedure.executeUpdate();		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}				
	}
	
	/**
	 * Begins a transaction by turning off auto-commit
	 */
	protected static void beginTransaction(Connection con){
		try {
			con.setAutoCommit(false);
		} catch (Exception e) {
			// Ignore any errors
		}
	}
	
	/**
	 * Ends a transaction by commiting any changes and re-enabling auto-commit
	 */
	protected static void endTransaction(Connection con){
		try {
			con.commit();
			enableAutoCommit(con);
		} catch (Exception e) {
			Database.doRollback(con);
		}
	}
	
	/**
	 * Turns on auto-commit
	 */
	protected static void enableAutoCommit(Connection con){
		try {
			con.setAutoCommit(true);
		} catch (Exception e) {
			// Ignore any errors
		}
	}
	
	/**
	 * Rolls back any actions not committed to the database
	 */
	protected static void doRollback(Connection con){
		try {
			con.rollback();
			log.warn("Database transaction rollback.");
		} catch (Exception e) {
			// Ignore any errors
		}
	}
	
	/**
	 * Method which safely closes a connection pool connection
	 * and doesn't raise any errors
	 * @param c The connection to safely close
	 */
	protected static void safeClose(Connection c) {
		try {
			if(c != null) {
				c.close();
			}
		} catch (Exception e){
			// Do nothing
		}
	}


}