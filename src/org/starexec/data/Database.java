package org.starexec.data;

import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

import org.apache.catalina.util.HexUtils;
import org.apache.log4j.*;
import org.starexec.constants.R;
import org.starexec.data.to.*;
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
	 * @param u The user to add
	 * @return True if the user was successfully added, false if otherwise
	 * @deprecated Left as an example, need to update to use stored procedures
	 */
	public static boolean addUser(User u){
		Connection con = null;
		
		try{
			con = dataPool.getConnection();
			
			// Encoders used to hash passwords for storage			
			MessageDigest hasher = MessageDigest.getInstance(R.PWD_HASH_ALGORITHM);
			hasher.update(u.getPassword().getBytes());
			
			// TODO Utilize callable statements to use stored procedure
			
			// Get the hashed version of the password
			String hashedPass = HexUtils.convert(hasher.digest());
										
			log.info(String.format("New user created with name [%s] and email [%s]", u.getFullName(), u.getEmail()));
			return true;
		} catch (Exception e){					
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Database.safeClose(con);
		}
	}
	
	/**
	 * Retrieves a user from the database given the email address
	 * @param email The email of the user to retrieve
	 * @return The user object associated with the user
	 * @author Tyler Jensen
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
	
	public static boolean updateInstitution(long userId, String newValue){
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
	
	public static boolean updatePassword(long userId, String newValue){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdatePassword(?, ?)}");
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
	
	public static String getWebsites(long userId) {
		Connection con = null;
		
		try {
			con = dataPool.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetWebsitesById(?)}");
			procedure.setLong(1, userId);
			ResultSet results = procedure.executeQuery();
			StringBuffer websites = new StringBuffer("{\"websites\":[ ");
			
			while (results.next()) {
				websites.append("{\"name\": \"" + results.getString("name") + "\", \"url\": \"" + results.getString("url") + "\"},");
			}
			websites.setCharAt(websites.length()-1,']');  // replace last character with ]
	        websites.append("}");
			return websites.toString();
			
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
