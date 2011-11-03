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
	 * Updates the first name of a user in the database with the 
	 * given user ID
	 * 
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the first name will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
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
	 * @param id The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark getBenchmark(long id) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");
			procedure.setLong(1, id);					
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
	 * @param id The if of the job to get information for
	 * @return A job object containing information about the requested job
	 */
	public static Job getJob(long id) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setLong(1, id);					
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
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets all job pairs for the given job
	 * @param id The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	public static List<JobPair> getPairsForJob(long id) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobPairByJob(?)}");
			procedure.setLong(1, id);					
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
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param id The id of the solver to retrieve
	 * @return A solver object representing the solver with the given ID
	 * @author Tyler Jensen
	 */
	public static Solver getSolver(long id) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSolverById(?)}");
			procedure.setLong(1, id);					
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
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Database.safeClose(con);
		}
		
		return null;
	}
	
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
	 * @param id The id of the parent space. Give an id <= 0 to get the root space
	 * @return A list of child spaces belonging to the parent space.
	 * @author Tyler Jensen
	 */
	public static List<Space> getSubSpaces(long id) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSubSpacesById(?)}");
			procedure.setLong(1, id);					
			ResultSet results = procedure.executeQuery();
			List<Space> subSpaces = new LinkedList<Space>();
			
			while(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getLong("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));
				s.setPermissionId(results.getLong("default_permission"));
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
	 * @param id The id of the space to get information for
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space getSpace(long id) {
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceById(?)}");
			procedure.setLong(1, id);					
			ResultSet results = procedure.executeQuery();		
			
			if(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getLong("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));
				s.setCreated(results.getTimestamp("created"));
				s.setPermissionId(results.getLong("default_permission"));
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
	 * Gets a space with detailed information (solvers, benchmarks, jobs and user belonging
	 * to the space are also populated)
	 * @param id The id of the space to get information for
	 * @return A space object consisting of detailed information about the space
	 * @author Tyler Jensen
	 */
	public static Space getSpaceDetails(long id) {		
		try {
			Space s = Database.getSpace(id);
			s.setUsers(Database.getSpaceUsers(id));
			s.setBenchmarks(Database.getSpaceBenchmarks(id));
			s.setSolvers(Database.getSpaceSolvers(id));
			s.setJobs(Database.getSpaceJobs(id));
						
			log.debug(String.format("getSpaceDetails returning %d record(s) for space id=%d", s == null ? 0 : 1, id));			
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
