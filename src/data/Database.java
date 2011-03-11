package data;

import java.sql.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import util.SHA256;

import constants.*;
import data.to.Benchmark;
import data.to.User;

public class Database {
	private Logger log = Logger.getLogger("starexec.data");
	private Connection connection = null;
	private PreparedStatement psAddUser = null;
	private PreparedStatement psAddPassword = null;
	private PreparedStatement psAddPermissions = null;
	private PreparedStatement psGetUser = null;
	private PreparedStatement psAddBenchmark = null;
	
	public Database(ServletContext context) {
		try {
			Class.forName("com.mysql.jdbc.Driver");	// Load the MYSQL driver
			
			// Load connection info from the web.config file
			String url = context.getInitParameter(R.MYSQL_URL);
			String username = context.getInitParameter(R.MYSQL_USERNAME);
			String pass = context.getInitParameter(R.MYSQL_PASSWORD);
			
			connection = DriverManager.getConnection(url, username, pass);	// Open a connection to the database
		} catch (Exception e) {
			log.severe("Error in Database constructor: " + e.getMessage());
			log.severe(Arrays.toString(e.getStackTrace()));
		}		
	}
	
	/**
	 * Adds the specified user to the database. Defaults to an unverified user and does not handle user verification.
	 * @param u The user to add
	 * @return True if the user was successfully added, false if otherwise
	 */
	public synchronized boolean addUser(User u){
		try{
			connection.setAutoCommit(false);	// Turn auto commit off (the operations below need to be a single transaction)
			
			// INSERT INTO USERS TABLE
			if(psAddUser == null)	// If the statement hasn't been prepared yet, create it...			
					psAddUser = connection.prepareStatement("INSERT INTO users (username, fname, lname, affiliation, created, email) VALUES (?, ?, ?, ?, NOW(), ?)", Statement.RETURN_GENERATED_KEYS);
			
			// Fill in the prepared statement
			psAddUser.setString(1, u.getUsername());
			psAddUser.setString(2, u.getFirstName());
			psAddUser.setString(3, u.getLastName());
			psAddUser.setString(4, u.getAffiliation());
			psAddUser.setString(5, u.getEmail());
			
			// Execute the statement
			int rowsAffected = psAddUser.executeUpdate();	// Execute and get the number of rows affected (should be one)
			ResultSet idSet = psAddUser.getGeneratedKeys(); idSet.next();
			int insertedID = idSet.getInt(1);				// Get the ID the record was inserted under (used for the next two queries
			
			// INSERT INTO PASSWORD TABLE
			if(psAddPassword == null)	// If the statement hasn't been prepared yet, create it...
				psAddPassword = connection.prepareStatement("INSERT INTO passwords (userid, password) VALUES (?, ?)");
			
			// Fill in the prepared statement
			// TODO: Add salt to passwords
			psAddPassword.setInt(1, insertedID);
			psAddPassword.setString(2, SHA256.getHash(u.getPassword()));
			
			rowsAffected += psAddPassword.executeUpdate();	// Execute and get the rows affected
			
			// INSERT INTO PERMISSIONS TABLE
			if(psAddPermissions == null)	// If the statement hasn't been prepared yet, create it...
				psAddPermissions = connection.prepareStatement("INSERT INTO permissions (userid) VALUES (?)");
			
			// Fill in the prepared statement
			psAddPermissions.setInt(1, insertedID);
			
			rowsAffected += psAddPermissions.executeUpdate();	// Execute and get the rows affected
			connection.commit();			// Now commit everything to the database
			connection.setAutoCommit(true);	// Turn auto-commit back on for the other methods
			
			return rowsAffected == 3;	// If exactly 3 rows were affected, success!
		} catch (Exception e){
			log.severe("Error in addUser method: " + e.getMessage());
			log.severe(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}
	
	public synchronized User getUser(String username){
		try {
			if(psGetUser == null)
				psGetUser = connection.prepareStatement("SELECT * FROM users JOIN passwords WHERE users.username='?'");
			
			psGetUser.setString(1, username);
			ResultSet results = psGetUser.executeQuery();
			
			if(results.next()){
				User u = new User(results.getInt("userid"), results.getString("username"));
				u.setAffiliation(results.getString("affiliation"));
				u.setEmail(results.getString("email"));
				u.setFirstName(results.getString("fname"));
				u.setLastName(results.getString("lname"));
				u.setPassword(results.getString("password"));
				return u;
			}					
		} catch (Exception e){
			log.log(Level.WARNING, "Error in addUser method: " + e.getMessage());			
		} finally {
			//if(psGetUser != null)
				//psGetUser.close();
			
		}
		
		return null;
	}
	
	public synchronized int addBenchmark(Benchmark b){
		try{						
			if(psAddBenchmark == null)	// If the statement hasn't been prepared yet, create it...			
					psAddBenchmark = connection.prepareStatement("INSERT INTO benchmarks (uploaded, physical_path, usr) VALUES (NOW(), ?, ?)", Statement.RETURN_GENERATED_KEYS);
			
			// Fill in the prepared statement
			psAddBenchmark.setString(1, b.getPath());
			psAddBenchmark.setInt(2, b.getUser());
			
			// Execute the statement
			psAddBenchmark.executeUpdate();		// Execute
			ResultSet idSet = psAddBenchmark.getGeneratedKeys(); 
			idSet.next();
			
			return idSet.getInt(1);					// Get the ID the record was inserted under			
		} catch (Exception e){
			log.severe("Error in addUser method: " + e.getMessage());
			log.severe(Arrays.toString(e.getStackTrace()));
			return -1;
		}
	}
}