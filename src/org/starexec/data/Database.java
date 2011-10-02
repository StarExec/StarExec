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
 * 
 * @author Tyler Jensen
 */
public class Database {	
	private static final Logger log = Logger.getLogger(Database.class);
	private static DataSource dataPool = null;
	
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
	
	public Database() throws Exception {
		throw new Exception("Cannot instatiate an object of a static class");
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
	 * @deprecated Left as an example, need to update to use stored procedures
	 */
	public static User getUser(String email){
		Connection con = null;			
		
		try {
			con = dataPool.getConnection();
			
			//PreparedStatement psGetUser = statementPool.getStatement(StatementPool.GET_USER);
			PreparedStatement psGetUser = null;
			psGetUser.setString(1, email);
			
			ResultSet results = psGetUser.executeQuery();
			
			if(results.next()){
				User u = new User(results.getInt("userid"));
				u.setAffiliation(results.getString("affiliation"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("fname"));
				u.setLastName(results.getString("lname"));
				return u;
			}					
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
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
