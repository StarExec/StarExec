package data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import util.LogUtil;
import util.SHA256;
import constants.R;
import data.to.Benchmark;
import data.to.Level;
import data.to.Solver;
import data.to.User;

public class Database {
	private Logger log = Logger.getLogger("starexec.data");
	private Connection connection = null;
	private PreparedStatement psAddUser = null;
	private PreparedStatement psAddPassword = null;
	private PreparedStatement psAddPermissions = null;
	private PreparedStatement psAddLevel = null;
	private PreparedStatement psGetUser = null;
	private PreparedStatement psGetUser2 = null;
	private PreparedStatement psAddBenchmark = null;
	private PreparedStatement psGetSolvers = null;
	private PreparedStatement psGetBenchmarks = null;
	private PreparedStatement psGetMaxLevel = null;
	private PreparedStatement psGetMaxLevelGroup = null;
	private PreparedStatement psGetRootLevels = null;
	private PreparedStatement psGetSubLevels = null;
	
	public Database() {
		this(R.MYSQL_URL, R.MYSQL_USERNAME, R.MYSQL_PASSWORD);	// Use the default connection info			
	}
	
	public Database(String url, String username, String pass) {
		try {
			Class.forName("com.mysql.jdbc.Driver");	// Load the MYSQL driver						
			connection = DriverManager.getConnection(url, username, pass);	// Open a connection to the database
		} catch (Exception e) {
			log.severe("DATABASE CONNECTION ERROR");
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
			
			return rowsAffected == 3;	// If exactly 3 rows were affected, success!
		} catch (Exception e){
			doRollback();
			log.severe("Error in addUser method: " + e.getMessage());
			LogUtil.LogException(e);
			return false;
		} finally {
			autoCommitOn();
		}
	}
	
	/**
	 * Retrieves a user from the database given the username
	 * @param username The username of the user to retrieve
	 * @return The user object associated with the user
	 */
	public synchronized User getUser(String username){
		try {
			if(psGetUser == null)
				psGetUser = connection.prepareStatement("SELECT * FROM users JOIN passwords ON users.userid = passwords.userid WHERE users.username='?'");
			
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
			log.severe("Error in getUser method: " + e.getMessage());
			LogUtil.LogException(e);		
		}
		
		return null;
	}
	
	/**
	 * Retrieves a user from the database given the user's id
	 * @param userid The id of the user to retrieve
	 * @return The user object associated with the id
	 */
	public synchronized User getUser(int userid){
		try {
			if(psGetUser2 == null)
				psGetUser2 = connection.prepareStatement("SELECT * FROM users JOIN passwords WHERE users.userid=?");
			
			psGetUser2.setInt(1, userid);
			ResultSet results = psGetUser2.executeQuery();
			
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
			log.severe("Error in getUser method: " + e.getMessage());
			LogUtil.LogException(e);		
		}
		
		return null;
	}
	
	public synchronized boolean addLevelsBenchmarks(Collection<Level> levels, Collection<Benchmark> benchmarks){		
		int offset = getMaxLevel();
		boolean retVal = true;
		retVal = retVal && addLevelStructure(levels, offset);
		retVal = retVal && addBenchmarks(benchmarks, offset);
		return retVal;
	}
	
	/**
	 * Adds a set of benchmarks to the database. Benchmarks must be added before the level structure is added or else
	 * the benchmarks will be mis-aligned with their proper levels.
	 * @param benchmarks The set of benchmarks to add
	 * @return True if all benchmarks were added, false if otherwise.
	 */
	public synchronized boolean addBenchmarks(Collection<Benchmark> benchmarks, int offSet){
		try{						
			if(psAddBenchmark == null)	// If the statement hasn't been prepared yet, create it...			
					psAddBenchmark = connection.prepareStatement("INSERT INTO benchmarks (uploaded, physical_path, usr, lvl) VALUES (NOW(), ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
						
			connection.setAutoCommit(false);
			int rowsAffected = 0;
			
			for(Benchmark b : benchmarks){
				// Fill in the prepared statement
				psAddBenchmark.setString(1, b.getPath());
				psAddBenchmark.setInt(2, b.getUserId());
				psAddBenchmark.setInt(3, b.getLevel() + offSet);
				
				// Execute the statement
				rowsAffected += psAddBenchmark.executeUpdate();
			}									
			
			if(rowsAffected == benchmarks.size()) {
				connection.commit();
				return true;
			} else {
				throw new Exception("Benchmarks inserted do not match the benchmarks received.");
			}
		} catch (Exception e){
			doRollback();
			log.severe("Error in addBenchmarks method: " + e.getMessage());
			LogUtil.LogException(e);
			return false;
		} finally {
			autoCommitOn();
		}
	}
	
	/**
	 * Gets a list of benchmarks given a list of id's to retrieve
	 * @param idList The list of ids to fetch benchmarks for
	 * @return An arraylist of benchmarks corresponding to the input ids (ordering is preserved)
	 */
	public synchronized List<Benchmark> getBenchmarks(Collection<Long> idList){
		ArrayList<Benchmark> returnList = new ArrayList<Benchmark>(5);
		
		for(long id : idList){
			Benchmark benchmark = this.getBenchmark(id);
			if(benchmark != null)
				returnList.add(benchmark);
		}
		
		return returnList;
	}
	
	/**
	 * Gets a benchmark from the database given its id
	 * @param id The is of the benchmark to retrieve
	 * @return The benchmark associated with the id
	 */
	public synchronized Benchmark getBenchmark(Long id){
		try {
			if(psGetBenchmarks == null)
				psGetBenchmarks = connection.prepareStatement("SELECT * FROM benchmarks WHERE id=?");
						
			psGetBenchmarks.setLong(1, id);
			
			ResultSet results = psGetBenchmarks.executeQuery();
			
			if(!results.next())
				return null;
			
			Benchmark benchmark = new Benchmark();
			benchmark.setId(results.getLong("id"));
			benchmark.setPath(results.getString("physical_path"));
			benchmark.setUserId(results.getInt("usr"));
			benchmark.setUploaded(results.getDate("uploaded"));
			
			return benchmark;
		} catch (Exception e){
			log.severe("Error in getBenchmark method: " + e.getMessage());
			LogUtil.LogException(e);
			return null;
		}	
	}
	
	/**
	 * Gets a list of solvers given a list of solver id's
	 * @param idList The list of id's of the solvers to retrieve
	 * @return An arraylist of solvers that correspond to the input id's (ordering is preserved)
	 */
	public synchronized List<Solver> getSolvers(Collection<Long> idList){
		ArrayList<Solver> returnList = new ArrayList<Solver>(5);
		
		for(long id : idList){
			Solver solver = this.getSolver(id);
			if(solver != null)
				returnList.add(solver);
		}
		
		return returnList;
	}
	
	/**
	 * Gets a solver from the database given its id
	 * @param id The id of the solver
	 * @return The solver which corresponds to the given id
	 */
	public synchronized Solver getSolver(Long id){
		try {
			if(psGetSolvers == null)
				psGetSolvers = connection.prepareStatement("SELECT * FROM solvers WHERE id=?");
						
			psGetSolvers.setLong(1, id);
			
			ResultSet results = psGetSolvers.executeQuery();
			
			if(!results.next())
				return null;
			
			Solver solver = new Solver();
			solver.setId(results.getLong("id"));
			solver.setPath(results.getString("path"));
			solver.setUserId(results.getLong("usr"));
			solver.setUploaded(results.getDate("uploaded"));
			solver.setNotes(results.getString("notes"));
			
			return solver;
		} catch (Exception e){
			log.severe("Error in getSolver method: " + e.getMessage());
			LogUtil.LogException(e);
			return null;
		}			
	}
	
	/**
	 * Inserts a level structure into the database given a collection of valid levels. Note that
	 * benchmarks must be added first or else the proper offsets will not be obtained for the benchmarks.
	 * @param levels The collection of levels to add
	 * @return True for success, false for failure
	 */
	public synchronized boolean addLevelStructure(Collection<Level> levels, int offSet){
		try {
			if(psAddLevel == null)
				psAddLevel = connection.prepareStatement("INSERT INTO levels (name, lft, rgt, gid, usr) VALUES (?, ?, ?, ?, ?)");
			
			connection.setAutoCommit(false);
			int inserted = 0;						
			int nextGroupId = getNextLevelGroup();
			
			for(Level l : levels){
				psAddLevel.setString(1, l.getName());
				psAddLevel.setInt(2, l.getLeft() + offSet);
				psAddLevel.setInt(3, l.getRight() + offSet);
				psAddLevel.setInt(4, nextGroupId);
				psAddLevel.setInt(5, l.getUserId());
				inserted += psAddLevel.executeUpdate();
			}
													
			connection.commit();			
			return levels.size() == inserted;
		} catch (Exception e){
			doRollback();
			log.severe("Error in addLevelStructure method: " + e.getMessage());
			LogUtil.LogException(e);
			return false;
		} finally {
			autoCommitOn();
		}
	}
	
	/**
	 * @return The next level to use when inserting into the levels table. (Essentially the largest right-value)
	 */
	public synchronized int getMaxLevel(){
		try {
			if(psGetMaxLevel == null)
				psGetMaxLevel = connection.prepareStatement("SELECT MAX(rgt) FROM levels");
												
			ResultSet results = psGetMaxLevel.executeQuery();
			
			if(!results.next())
				return 0;					
			
			return results.getInt(1);
		} catch (Exception e){
			log.severe("Error in getMaxLevel method: " + e.getMessage());
			LogUtil.LogException(e);
			return 0;
		}	
	}
	
	/**
	 * @return The next level to use when inserting into the levels table. (Essentially the largest right-value)
	 */
	public synchronized int getNextLevelGroup(){
		try {
			if(psGetMaxLevelGroup == null)
				psGetMaxLevelGroup = connection.prepareStatement("SELECT MAX(gid) FROM levels");
												
			ResultSet results = psGetMaxLevelGroup.executeQuery();
			
			if(!results.next())
				return 0;					
			
			return results.getInt(1) + 1;
		} catch (Exception e){
			log.severe("Error in getNextLevelGroup method: " + e.getMessage());
			LogUtil.LogException(e);
			return 0;
		}	
	}
	
	/**
	 * @return A list of root levels (top-level virtual directories)
	 */
	public synchronized List<Level> getRootLevels(){
		try {
			if(psGetRootLevels == null)
				psGetRootLevels = connection.prepareStatement("SELECT * FROM levels GROUP BY gid HAVING MIN(lft)");
									
			
			ResultSet results = psGetRootLevels.executeQuery();
			ArrayList<Level> returnList = new ArrayList<Level>(15);
			
			while(results.next()){
				Level l = new Level(results.getInt("id"));
				l.setGroupId(results.getInt("gid"));
				l.setLeft(results.getInt("lft"));
				l.setRight(results.getInt("rgt"));
				l.setName(results.getString("name"));
				l.setUserId(results.getInt("usr"));
				l.setDescription(results.getString("description"));
				
				returnList.add(l);
			}			
						
			return returnList;
		} catch (Exception e){
			log.severe("Error in getRootLevels method: " + e.getMessage());
			LogUtil.LogException(e);
			return null;
		}	
	}
	
	/**
	 * Gets all virtual directories listed under another (children directories)
	 * @param id The is the parent directory
	 * @return A list of children directories of the parent
	 */
	public synchronized List<Level> getSubLevels(int id){
		try {
			if(psGetSubLevels == null)
				psGetSubLevels = connection.prepareStatement("SELECT * FROM levels WHERE lft > (SELECT lft FROM levels WHERE id=?) AND rgt < (SELECT rgt FROM levels WHERE id=?)");
									
			psGetSubLevels.setInt(1, id);
			psGetSubLevels.setInt(2, id);
			
			ResultSet results = psGetSubLevels.executeQuery();
			ArrayList<Level> returnList = new ArrayList<Level>(10);
			
			while(results.next()){
				Level l = new Level(results.getInt("id"));
				l.setGroupId(results.getInt("gid"));
				l.setLeft(results.getInt("lft"));
				l.setRight(results.getInt("rgt"));
				l.setName(results.getString("name"));
				l.setUserId(results.getInt("usr"));
				l.setDescription(results.getString("description"));
				
				returnList.add(l);
			}			
						
			return returnList;
		} catch (Exception e){
			log.severe("Error in getSubLevels method: " + e.getMessage());
			LogUtil.LogException(e);
			return null;
		}	
	}
	
	protected synchronized void autoCommitOn(){
		try {
			connection.setAutoCommit(true);
		} catch (Exception e) {
			// Ignore any errors
		}
	}
	
	protected synchronized void doRollback(){
		try {
			connection.rollback();
			log.warning("Database transaction rollback.");
		} catch (Exception e) {
			// Ignore any errors
		}
	}
}