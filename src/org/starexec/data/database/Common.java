package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.starexec.constants.R;
import org.starexec.util.Util;

/**
 * The common database class which provides common methods used by other database accessors such
 * as transaction management and rollback suport. Also provides connections and maintains an active
 * data pool of available connections to the MySql database.
 */
public class Common {	
	private static final Logger log = Logger.getLogger(Common.class);
	private static DataSource dataPool = null;		
	
	protected static Integer connectionsOpened = 0;
	protected static Integer connectionsClosed = 0;
	
	public Common() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * Configures and sets up the Tomcat JDBC connection pool. This method can only be called once in the
	 * lifetime of the application.
	 * @author Tyler Jensen
	 */
	public static void initialize() {						
		try {
			if(Util.isNullOrEmpty(R.MYSQL_USERNAME)) {
				log.warn("Attempted to initialize datapool without MYSQL properties being set");
				return;
			} else if(dataPool != null) {
				log.warn("Attempted to initialize datapool when it was already initialized");
				return;
			}
			
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
			poolProp.setRemoveAbandonedTimeout(3600);						// How int to wait (seconds) before reclaiming an open connection (should be the time of intest query)
			poolProp.setRemoveAbandoned(true);							// Enable removing connections that are open too int
		
			log.debug("Creating new datapool with supplied properties");		
			dataPool = new DataSource(poolProp);						// Create the connection pool with the supplied properties
		
			log.debug("Datapool successfully created!");
		} catch (Exception e) {
			log.fatal(e.getMessage(), e);
		}
	}
	
	/**
	 * Cleans up the database connection pool. This class must be reinitialized after this is called. 
	 */
	public static void release() {
		if(dataPool != null) {
			dataPool.close();
		}
	}
	
	/**
	 * Gets information on the data pool.  Used to track down connection leak.
	 * Returns true if not nearing max active connections
	 */
	public static Boolean getDataPoolData(){
		log.info("Data Pool has " + dataPool.getActive() + " active connections.  ");
		if (dataPool.getWaitCount()>0){
		log.info("# of threads waiting for a connection = " + dataPool.getWaitCount());
		}
		if (dataPool.getActive() > .5*R.MYSQL_POOL_MAX_SIZE){
			return false;
		}
		else{
			return true;
		}
	}
	
	/**
	 * @return a new connection to the database from the connection pool
	 * @author Tyler Jensen
	 */
	protected synchronized static Connection getConnection() throws SQLException {	
		connectionsOpened++;
		//log.info("Connection Opened, Net Connections Opened = " + (connectionsOpened-connectionsClosed));
		return dataPool.getConnection();
	}							
	
	/**
	 * Creates a new historical record in the logins table which keeps track of all user logins.
	 * @param userId The user that has logged in
	 * @param ipAddress The IP address the user logged in from
	 * @param browser The browser/agent information about the browser the user logged in with
	 */
	public static void addLoginRecord(int userId, String ipAddress, String browser) {
		Connection con = null;		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL LoginRecord(?, ?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, ipAddress);
			procedure.setString(3, browser);			
			procedure.executeUpdate();		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
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
	 * Ends a transaction by committing any changes and re-enabling auto-commit
	 */
	protected static void endTransaction(Connection con){
		try {
			con.commit();
			enableAutoCommit(con);
		} catch (Exception e) {
			Common.doRollback(con);
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
			con.setAutoCommit(true);
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
	protected static synchronized void safeClose(Connection c) {
		try {
			if(c != null) {
				c.close();
				
				connectionsClosed++;
				log.info("Connection Closed, Net connections opened = " + (connectionsOpened-connectionsClosed));
			}
		} catch (Exception e){
			// Do nothing
			StackTraceElement [] trace=e.getStackTrace();
			for (StackTraceElement elm: trace) {
				log.error(elm.toString());
			}
			log.error("Safe Close says " + e);
		}

	}
	
	/**
	 * Method which closes a result set
	 * @param r The result set close
	 */
	protected static void closeResultSet(ResultSet r) {
		try {
			if(r != null) {
				r.close();
			}
		} catch (Exception e){
			log.error("Close Result set says " + e);
		}
	}
}