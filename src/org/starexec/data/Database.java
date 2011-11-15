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
	 * @return True if the user was successfully added, false if otherwise
	 */
	public static boolean addUser(User user){
		Connection con = null;
		
		try{
			con = dataPool.getConnection();
		
			String hashedPass = Hash.hashPassword(user.getPassword());
			
			// Insert data into IN parameters of the CallableStatment
			CallableStatement procedure = con.prepareCall("{CALL AddUser(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, user.getFirstName());
			procedure.setString(2, user.getLastName());
			procedure.setString(3, user.getEmail());
			procedure.setString(4, user.getInstitution());
			procedure.setString(5, hashedPass);
			
			// Set the OUT parameters
			procedure.registerOutParameter(6, java.sql.Types.BIGINT);
			procedure.registerOutParameter(7, java.sql.Types.TIMESTAMP);
			
			// Apply update to database
			procedure.executeUpdate();
			
			// Extract values from OUT parameters
			user.setId(procedure.getLong(6));
			user.setCreateDate(procedure.getTimestamp(7));
			
			// Generate a UUID code and add it to VERIFY database under new user's id
			boolean added = addCode(user);
			
			if(added){
				log.info(String.format("New user (id=%d) added to USERS, with name [%s] and email [%s]", user.getId(), user.getFullName(), user.getEmail()));
				return true;				
			} else {
				return false;
			}
			
		} catch (Exception e){					
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Database.safeClose(con);
		}
	}
	
	
	/**
	 * Adds a verification code to the database for a given user
	 * 
	 * @param user the user to add a verification code to the database for
	 * @return true iff the new verification code is added to the database
	 */
	public static boolean addCode(User user) {
		Connection con = null;
		
		// Generate unique verification code
		String code = UUID.randomUUID().toString();

		try {
			con = dataPool.getConnection();

			// Add a new entry to the VERIFY table
			CallableStatement procedure = con.prepareCall("{CALL AddCode(?, ?, ?)}");
			procedure.setLong(1, user.getId());
			procedure.setString(2, code);
			procedure.setTimestamp(3, user.getCreateDate());

			// Apply update to database
			procedure.executeUpdate();

			log.info(String.format("New email verification code [%s] added to VERIFY for user [%s]", code, user.getFullName()));
			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Database.safeClose(con);
		}

	}
	
	/**
	 * Retrieves a user's verification code and compares it to 'responseCode'. 
	 * If they are the same, the 'verified' attribute for the given user in the USERS table
	 * will be set to 1.
	 * 
	 * @param user the user whose code need to be queried for
	 * @param responseCode the response the user sent back
	 * @return true iff the verification code matches 'responseCode'
	 */
	public static boolean verifyCode(User user, String responseCode){
		Connection con = null;
		String initialCode = null;

		try {
			con = dataPool.getConnection();

			// Query VERIFY table for verification code associated with user
			CallableStatement procedure = con.prepareCall("{CALL GetCode(?)}");
			procedure.setLong(1, user.getId());
			ResultSet result = procedure.executeQuery();
			if(result.next()){
				initialCode = result.getString("code");
			}
			
			// IF the verification code from VERIFY matches 'responseCode'
			// THEN set 'verified' attribute in USERS to 1
			if(initialCode.equals(responseCode)){
				procedure = con.prepareCall("{CALL VerifyUser(?)}");
				procedure.setLong(1, user.getId());
				procedure.executeUpdate();
				System.out.println("initialCode  = " + initialCode);
				System.out.println("responseCode = " + responseCode);
				System.out.println("Code verification completed.");					
				return true;
			} else {
				return false;
			}
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Database.safeClose(con);
		}
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
	 * Retrieves a user from the database given the email address
	 * 
	 * @param email The email of the user to retrieve
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
					b.setId(results.getLong("id"));
					b.setUserId(results.getLong("user_id"));
					b.setName(results.getString("name"));
					b.setUploadDate(results.getTimestamp("uploaded"));
					b.setPath(results.getString("path"));
					b.setDescription(results.getString("description"));
					b.setDownloadable(results.getBoolean("downloadable"));
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
			
			return result == 1;			
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
			
			return result == 1;			
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
			
			return result == 1;			
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
			
			return result == 1;			
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
			
			return result == 1;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Get all websites associated with a given user.
	 * 
	 * @param userId the user ID of the user we want to find websites for
	 * @return A list of websites associated with the user
	 * @author Skylar Stark
	 */
	public static List<Website> getWebsitesByUserId(long userId) {
		Connection con = null;
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetWebsitesByUserId(?)}");
			procedure.setLong(1, userId);
			
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
	 * Returns a list of websites associated with the given solver id
	 * @param id The id of the solver to get websites for
	 * @return A list of websites associated with the solver
	 * @author Tyler Jensen
	 */
	public static List<Website> getWebsitesForSolver(long id) {
		Connection con = null;
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetSolverWebsitesById(?)}");
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
			
			log.debug("getSubSpaces returning " + subSpaces.size() + " records");
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
							
				log.debug(String.format("getSpaceDetails returning %d record(s) for space id=%d", s == null ? 0 : 1, spaceId));			
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
			
			log.debug(String.format("getSpaceUsers returning %d records for id=%d", users.size(), spaceId));
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
				b.setId(results.getLong("id"));
				b.setName(results.getString("name"));
				b.setUploadDate(results.getTimestamp("uploaded"));
				b.setDescription(results.getString("description"));
				b.setDownloadable(results.getBoolean("downloadable"));				
				benchmarks.add(b);
			}			
			
			log.debug(String.format("getSpaceBenchmarks returning %d records for id=%d", benchmarks.size(), spaceId));
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
			
			log.debug(String.format("getSpaceJobs returning %d records for id=%d", jobs.size(), spaceId));
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
			
			log.debug(String.format("getSpaceSolvers returning %d records for id=%d", solvers.size(), spaceId));
			return solvers;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	
	/**
	 * Adds a website associated with a user to the database.
	 * 
	 * @param userId the ID of the user
	 * @param url the URL of the website
	 * @param name the name to display with the website
	 * @return true iff the add was successful
	 */
	public static boolean addUserWebsite(long userId, String url, String name) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL AddUserWebsite(?, ?, ?)}");
			procedure.setLong(1, userId);
			procedure.setString(2, url);
			procedure.setString(3, name);
			int result = procedure.executeUpdate();
			
			return result == 1;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Deletes the website associated with the given website ID.
	 * 
	 * @param websiteId the ID of the website to delete
	 * @return true iff the delete is successful
	 */
	public static boolean deleteUserWebsite(long websiteId, long userId) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL DeleteUserWebsite(?, ?)}");
			procedure.setLong(1, websiteId);
			procedure.setLong(2, userId);
			int result = procedure.executeUpdate();
			
			return result == 1;			
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
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addSpace(Space s, long parentId, long userId) {
		Connection con = null;			
		
		try {
			// Keep track of affected rows as we go, we should affect 5 rows total
			int result = 0;
			con = dataPool.getConnection();				
			beginTransaction(con);
			
			// Add the default permission for the space to the database			
			long defaultPermId = Database.addPermission(s.getPermission(), con);
			
			// Add the space with the default permissions
			CallableStatement procAddSpace = con.prepareCall("{CALL AddSpace(?, ?, ?, ?, ?)}");	
			procAddSpace.setString(1, s.getName());
			procAddSpace.setString(2, s.getDescription());
			procAddSpace.setBoolean(3, s.isLocked());
			procAddSpace.setLong(4, defaultPermId);
			procAddSpace.registerOutParameter(5, java.sql.Types.BIGINT);
			
			result += procAddSpace.executeUpdate();
			long newSpaceId = procAddSpace.getLong(5);
			
			// Add the new space as a child space of the parent space
			CallableStatement procSubspace = con.prepareCall("{CALL AssociateSpaces(?, ?, ?)}");	
			procSubspace.setLong(1, parentId);
			procSubspace.setLong(2, newSpaceId);
			procSubspace.setLong(3, defaultPermId);			
			
			result += procSubspace.executeUpdate();
			
			// Add a new set of maximal permissions for the user who added the space			
			long userPermId = Database.addPermission(new Permission(true), con);
			
			// Add the adding user to the space with the maximal permissions
			CallableStatement procAddUser = con.prepareCall("{CALL AddUserToSpace(?, ?, ?, ?)}");			
			procAddUser.setLong(1, userId);
			procAddUser.setLong(2, newSpaceId);
			procAddUser.setLong(3, newSpaceId);
			procAddUser.setLong(4, userPermId);
			
			result += procAddUser.executeUpdate();
			endTransaction(con);
			
			// Log the new addition and ensure we return 3 affected rows (the two added permissions are self-checking)
			log.info(String.format("New space with name %s added by user %d to space %d", s.getName(), userId, parentId));
			return result == 3;
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