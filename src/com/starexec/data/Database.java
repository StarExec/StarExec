package com.starexec.data;

import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

import org.apache.catalina.util.MD5Encoder;
import org.apache.log4j.Logger;

import sun.security.provider.MD5;

import com.starexec.constants.*;
import com.starexec.data.to.*;
import com.starexec.util.*;

/**
 * Database objects are responsible for any and all communication to the MySQL back-end database.
 * This class provides a convenient front for storing things in the database and extracting information.
 * 
 * @author Tyler Jensen
 */
public class Database {
	
	private static final Logger log = Logger.getLogger(Database.class);
	private Connection connection = null;
	private StatementPool statementPool = null;
	
	public Database() {
		// Use the default connection info
		this(R.MYSQL_URL, R.MYSQL_USERNAME, R.MYSQL_PASSWORD);			
	}
	
	/**
	 * Creates a new database connection
	 * @param url The JDBC url to the database
	 * @param username Username
	 * @param pass Password
	 */
	public Database(String url, String username, String pass) {
		try {
			// Load the MYSQL driver
			Class.forName("com.mysql.jdbc.Driver");						
			
			// Open a connection to the database
			connection = DriverManager.getConnection(url, username, pass);
			
			// Initialize the statement pool
			statementPool = new StatementPool();
			statementPool.Initialize(connection);
			
			log.debug("New database connection created to: " + url);
		} catch (Exception e) {
			log.fatal(e.getMessage(), e);
		}		
	}
	
	/**
	 * Adds the specified user to the database as pending for a community leader to accept them. 
	 * This method will hash the user's password for them, so it must be supplied in plaintext.
	 * @param u The user to add
	 * @return True if the user was successfully added, false if otherwise
	 */
	public synchronized boolean addUser(User u){
		try{
			// Encoders used to hash passwords for storage
			MD5Encoder md5Hexer = new MD5Encoder();
			MessageDigest md5Hasher = MessageDigest.getInstance("MD5");
			md5Hasher.update(u.getPassword().getBytes());
			
			PreparedStatement psAddUser = statementPool.getStatement(StatementPool.ADD_USER);
			psAddUser.setString(1, u.getFirstName());
			psAddUser.setString(2, u.getLastName());
			psAddUser.setString(3, u.getAffiliation());
			psAddUser.setString(4, u.getEmail());			
			// Hash the password before storing!
			psAddUser.setString(5, md5Hexer.encode(md5Hasher.digest()));
			//psAddUser.setInt(6, u.getCommunityId());
						
			psAddUser.executeUpdate();										
			log.info(String.format("New user created with name [%s] and email [%s]", u.getFullName(), u.getEmail()));
			return true;
		} catch (Exception e){					
			log.error(e.getMessage(), e);
			return false;
		}
	}
	
	public synchronized boolean addCode(String email, String conf) {
		try {
			PreparedStatement psAddCode = statementPool.getStatement(StatementPool.ADD_CONF_CODE);
			psAddCode.setString(1, email);
			psAddCode.setString(2, conf);
			
			if(psAddCode.executeUpdate() > 0) {
				log.info("Added code " + conf + " for user " + email);
				return true;
			} else {
				log.info("Could not add conf code.");
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		}
		
		return false;
	}
	
	public synchronized boolean verifyCode(String conf) {
		try {
			PreparedStatement psVerifyCode = statementPool.getStatement(StatementPool.VERIFY_CONF_CODE);
			psVerifyCode.setString(1, conf);
			
			if(psVerifyCode.executeUpdate() > 0) {
				log.info("Verified code " + conf);
				return true;
			} else {
				log.info("Code " + conf + " is not recognized.");
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		}
		
		return false;
	}
	
	/**
	 * Retrieves a user from the database given the email address
	 * @param email The email of the user to retrieve
	 * @return The user object associated with the user
	 */
	public synchronized User getUser(String email){
		try {
			PreparedStatement psGetUser = statementPool.getStatement(StatementPool.GET_USER);			
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
	 * Retrieves a user from the database given the user's id
	 * @param userid The id of the user to retrieve
	 * @return The user object associated with the id
	 */
	public synchronized User getUser(int userid){
		try {
			PreparedStatement psGetUser2 = statementPool.getStatement(StatementPool.GET_USER2);						
			psGetUser2.setInt(1, userid);
			
			ResultSet results = psGetUser2.executeQuery();
			
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
	 * Adds a set of levels and benchmarks belonging to the given levels to the database
	 * @param levels The level structure to add
	 * @param benchmarks The benchmark to add
	 * @return True if the operation was a success, false otherwise
	 */
	public synchronized boolean addLevelsBenchmarks(Collection<Level> levels, Collection<Benchmark> benchmarks){
		// TODO: Fix the synchronization issue (essentially, make this one atomic operation that locks the appropriate tables)
		int offset = getMaxLevel();
		boolean retVal = true;
		retVal = retVal && addLevelStructure(levels, offset);
		retVal = retVal && addBenchmarks(benchmarks, offset);
		return retVal;
	}
	
	/**
	 * Adds a new configuration of a run-script into the database
	 * @param c The configuration to add
	 * @return True if the operation was a success, false otherwise
	 */
	public synchronized boolean addConfiguration(Configuration c) {
		try{		
			PreparedStatement psAddConfig = statementPool.getStatement(StatementPool.ADD_CONFIGURATION);			
			psAddConfig.setInt(1, c.getSolverId());
			psAddConfig.setString(2, c.getName());
			psAddConfig.setString(3, c.getNotes());
			
			psAddConfig.executeUpdate();	
			
			log.info(String.format("New configuration added [%s] to solver [%d]", c.getName(), c.getSolverId()));
			return true;			
		} catch (Exception e){						
			log.error(e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Adds a solver to the database
	 * @param s The solver to add
	 * @return True if the operation was a success, false otherwise 
	 */
	public synchronized boolean addSolver(Solver s){		
		try{
			this.beginTransaction();
			
			PreparedStatement psAddSolver = statementPool.getStatement(StatementPool.ADD_SOLVER);									
			psAddSolver.setString(1, s.getPath());
			psAddSolver.setInt(2, s.getUserId());
			psAddSolver.setString(3, s.getNotes());
			psAddSolver.setString(4, s.getName());
			psAddSolver.setInt(5, s.getCommunityId());
			
			psAddSolver.executeUpdate();
			ResultSet idSet = psAddSolver.getGeneratedKeys(); idSet.next();
			int insertedID = idSet.getInt(1);

			log.info(String.format(
					"New solver added by user [%d] with name [%s] and path [%s]",
					s.getUserId(),
					s.getName(),
					s.getPath()));
			
			PreparedStatement psAddCanSolve = statementPool.getStatement(StatementPool.ADD_CAN_SOLVE);			
			
			for(Level l : s.getSupportedDivs()){
				psAddCanSolve.setInt(1, insertedID);
				psAddCanSolve.setInt(2, l.getId());
				psAddCanSolve.executeUpdate();				
				log.info(String.format("New can-solve support for solver [%d] and level [%d]", insertedID, l.getId()));
			}
			
			PreparedStatement psAddConfiguration = statementPool.getStatement(StatementPool.ADD_CONFIGURATION2);			
			
			for(Configuration c : s.getConfigurations()){
				psAddConfiguration.setInt(1, insertedID);
				psAddConfiguration.setString(2, c.getName());
				psAddConfiguration.executeUpdate();
				log.info(String.format("New configuration added [%s] to solver [%d]", c.getName(), insertedID));
			}
						
			this.endTransaction();
			return true;			
		} catch (Exception e){
			this.doRollback();			
			log.error(e.getMessage(), e);
			return false;
		} finally {
			this.enableAutoCommit();
		}
	}	
	
	/**
	 * Adds a set of benchmarks to the database. Benchmarks must be added before the level structure is added or else
	 * the benchmarks will be mis-aligned with their proper levels.
	 * @param benchmarks The set of benchmarks to add
	 * @return True if all benchmarks were added, false if otherwise.
	 */
	public synchronized boolean addBenchmarks(Collection<Benchmark> benchmarks, int offSet){
		try{		
			this.beginTransaction();
						
			PreparedStatement psAddBenchmark = statementPool.getStatement(StatementPool.ADD_BENCHMARK);												
			int rowsAffected = 0;
			
			for(Benchmark b : benchmarks){
				psAddBenchmark.setString(1, b.getPath());
				psAddBenchmark.setInt(2, b.getUserId());
				psAddBenchmark.setInt(3, b.getLevel() + offSet);
				psAddBenchmark.setInt(4, b.getCommunityId());
				
				rowsAffected += psAddBenchmark.executeUpdate();
			}									
			
			if(rowsAffected == benchmarks.size()) {
				this.endTransaction();
				
				log.info(String.format("%d benchmarks added starting at level offset [%d]", benchmarks.size(), offSet));						
				return true;
			} else {
				throw new Exception(String.format(
						"Benchmarks inserted do not match the benchmarks received. Expected [%d] Actual [%d]",
						benchmarks.size(),
						rowsAffected));
			}
		} catch (Exception e){
			this.doRollback();			
			log.error(e.getMessage(), e);
			return false;
		} finally {
			this.enableAutoCommit();
		}
	}
	
	/**
	 * Gets a list of benchmarks given a list of id's to retrieve
	 * @param idList The list of ids to fetch benchmarks for. Null retrieves all benchmarks.
	 * @return An arraylist of benchmarks corresponding to the input ids (ordering is preserved)
	 */
	public synchronized List<Benchmark> getBenchmarks(Collection<Integer> idList){
		try {
			ArrayList<Benchmark> returnList = new ArrayList<Benchmark>(5);
			
			for(int id : idList){
				Benchmark benchmark = this.getBenchmark(id);
				if(benchmark != null) {
					returnList.add(benchmark);
				}
			}
			
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * @param communityId The community to retrieve all benchmarks for
	 * @return A list of all benchmarks belonging to a community
	 */
	public synchronized List<Benchmark> getAllBenchmarks(int communityId){
		try {
			ArrayList<Benchmark> returnList = new ArrayList<Benchmark>(5);			
			PreparedStatement psGetAllBenchmarks = statementPool.getStatement(StatementPool.GET_ALL_BENCHMARKS);	
			psGetAllBenchmarks.setInt(1, communityId);
			ResultSet results = psGetAllBenchmarks.executeQuery();
			
			while(results.next()){
				Benchmark benchmark = new Benchmark();
				benchmark.setId(results.getInt("id"));
				benchmark.setPath(results.getString("physical_path"));
				benchmark.setUserId(results.getInt("usr"));
				benchmark.setUploaded(results.getTimestamp("uploaded"));
				benchmark.setCommunityId(results.getInt("comid"));
				returnList.add(benchmark);
			}		
			
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Gets a benchmark from the database given its id
	 * @param id The is of the benchmark to retrieve
	 * @return The benchmark associated with the id
	 */
	public synchronized Benchmark getBenchmark(int id){
		try {
			PreparedStatement psGetBenchmarks = statementPool.getStatement(StatementPool.GET_BENCHMARKS);									
			psGetBenchmarks.setInt(1, id);
			
			ResultSet results = psGetBenchmarks.executeQuery();
			
			if(!results.next()) {
				return null;
			}
			
			Benchmark benchmark = new Benchmark();
			benchmark.setId(results.getInt("id"));
			benchmark.setPath(results.getString("physical_path"));
			benchmark.setUserId(results.getInt("usr"));
			benchmark.setUploaded(results.getTimestamp("uploaded"));
			benchmark.setCommunityId(results.getInt("comid"));
			
			return benchmark;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;	
		}	
	}
	
	/**
	 * @return A list of all communities in the database
	 */
	public synchronized List<Community> getAllCommunities(){
		try {
			ArrayList<Community> returnList = new ArrayList<Community>();					
			PreparedStatement psGetAllCommunities = statementPool.getStatement(StatementPool.GET_ALL_COMMUNITIES);								
			ResultSet results = psGetAllCommunities.executeQuery();
			
			while(results.next()){
				Community community = new Community();
				community.setId(results.getInt("comid"));
				community.setName(results.getString("name"));					
				returnList.add(community);
			}					
			
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Retrieves all configurations given a collection of ids
	 * @param idList A list of ids to collect configurations for
	 * @return A list of corresponding configurations
	 */
	public synchronized List<Configuration> getConfigurations(Collection<Integer> idList){
		try {
			ArrayList<Configuration> returnList = new ArrayList<Configuration>(5);
					
			if(idList == null){
				PreparedStatement psGetConfigs = statementPool.getStatement(StatementPool.GET_CONFIGURATIONS);								
				ResultSet results = psGetConfigs.executeQuery();
				
				while(results.next()){
					Configuration config = new Configuration();
					config.setId(results.getInt("id"));
					config.setSolverId(results.getInt("sid"));
					config.setName(results.getString("name"));
					config.setNotes(results.getString("notes"));
					returnList.add(config);
				}
			} else {
				for(int id : idList){
					Configuration config = this.getConfiguration(id);
					if(config != null) {
						returnList.add(config);
					}
				}	
			}					
			
			return returnList;
		} catch (Exception e) {			
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Gets a configuration with the given id
	 * @param id The id of the configuration to retrieve
	 * @return The corresponding configuration
	 */
	public synchronized Configuration getConfiguration(int id){
		try {
			PreparedStatement psGetConfig = statementPool.getStatement(StatementPool.GET_CONFIGURATION);							
			psGetConfig.setInt(1, id);
			
			ResultSet results = psGetConfig.executeQuery();
			
			if(!results.next()) {
				return null;
			}
			
			Configuration config = new Configuration();
			config.setId(results.getInt("id"));
			config.setSolverId(results.getInt("sid"));
			config.setName(results.getString("name"));
			config.setNotes(results.getString("notes"));
			
			return config;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}			
	}
	
	/**
	 * Gets a list of solvers given a list of solver id's
	 * @param idList The list of id's of the solvers to retrieve.
	 * @return An arraylist of solvers that correspond to the input id's (ordering is preserved)
	 */
	public synchronized List<Solver> getSolvers(Collection<Integer> idList){
		try {
			ArrayList<Solver> returnList = new ArrayList<Solver>(5);
					
			for(int id : idList){
				Solver solver = this.getSolver(id);
				if(solver != null) {
					returnList.add(solver);
				}
			}					
			
			return returnList;
		} catch (Exception e) {			
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Gets all solvers for a given community
	 * @param id The id of the community
	 * @return All solvers belonging to the community
	 */
	public synchronized List<Solver> getAllSolvers(int communityId){
		try {
			ArrayList<Solver> returnList = new ArrayList<Solver>(5);

			PreparedStatement psGetAllSolvers = statementPool.getStatement(StatementPool.GET_ALL_SOLVERS);		
			psGetAllSolvers.setInt(1, communityId);
			ResultSet results = psGetAllSolvers.executeQuery();
			
			while(results.next()){
				Solver solver = new Solver();
				solver.setId(results.getInt("id"));
				solver.setPath(results.getString("path"));
				solver.setName(results.getString("name"));
				solver.setUserId(results.getInt("usr"));
				solver.setUploaded(results.getDate("uploaded"));
				solver.setNotes(results.getString("notes"));
				solver.setCommunityId(results.getInt("comid"));
				returnList.add(solver);
			}				
			
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}			
	}
	
	/**
	 * Gets a solver from the database given its id
	 * @param id The id of the solver
	 * @return The solver which corresponds to the given id
	 */
	public synchronized Solver getSolver(int id){
		try {
			PreparedStatement psGetSolvers = statementPool.getStatement(StatementPool.GET_SOLVERS);			
			psGetSolvers.setInt(1, id);
			
			ResultSet results = psGetSolvers.executeQuery();
			
			if(!results.next()) {
				return null;
			}
			
			Solver solver = new Solver();
			solver.setId(results.getInt("id"));
			solver.setName(results.getString("name"));
			solver.setPath(results.getString("path"));
			solver.setUserId(results.getInt("usr"));
			solver.setUploaded(results.getDate("uploaded"));
			solver.setNotes(results.getString("notes"));
			solver.setCommunityId(results.getInt("com"));
			
			return solver;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
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
			this.beginTransaction();
			
			PreparedStatement psAddLevel = statementPool.getStatement(StatementPool.ADD_LEVEL);						
			int inserted = 0;						
			int nextGroupId = getNextLevelGroup();
			
			for(Level l : levels){
				psAddLevel.setString(1, l.getName());
				psAddLevel.setInt(2, l.getLeft() + offSet);
				psAddLevel.setInt(3, l.getRight() + offSet);
				psAddLevel.setInt(4, nextGroupId);
				psAddLevel.setInt(5, l.getUserId());
				psAddLevel.setInt(6, l.getDepth());
				psAddLevel.setInt(7, l.getCommunityId());
				inserted += psAddLevel.executeUpdate();
			}
													
			this.endTransaction();
			
			log.info(String.format(
				"New level structure added with [%d] levels starting at [%d] with group id [%d]",
				levels.size(),
				offSet,
				nextGroupId));					
			
			return levels.size() == inserted;
		} catch (Exception e){
			this.doRollback();			
			log.error(e.getMessage(), e);
			return false;
		} finally {
			this.enableAutoCommit();
		}
	}
	
	/**
	 * Adds a job and any embedded job pairs to the database.
	 * @param j The job to add to the database
	 * @return True if the the operation was a success, false otherwise.
	 */
	public synchronized boolean addJob(Job j){
		try{
			this.beginTransaction();
			
			PreparedStatement psAddJob = statementPool.getStatement(StatementPool.ADD_JOB);		
			psAddJob.setInt(1, j.getJobId());
			psAddJob.setString(2, j.getDescription());
			psAddJob.setString(3, j.getStatus());
			psAddJob.setString(4, j.getNode());
			psAddJob.setLong(5, j.getTimeout());
			psAddJob.setInt(6, j.getUserId());
			psAddJob.setInt(7, j.getCommunityId());

			int rowsAffected = psAddJob.executeUpdate();
			
			PreparedStatement psAddJobPair = statementPool.getStatement(StatementPool.ADD_JOB_PAIR);			
			
			for(JobPair jp : j.getJobPairs()){ 
				psAddJobPair.setInt(1, jp.getId());
				psAddJobPair.setInt(2, j.getJobId());
				psAddJobPair.setInt(3, jp.getConfig().getId());
				psAddJobPair.setInt(4, jp.getBenchmark().getId());
				rowsAffected += psAddJobPair.executeUpdate();
			}						
			
			this.endTransaction();	
			
			log.info(String.format(
					"New job added with id [%d] by user [%d] on node [%s] with a timeout of [%s seconds] and [%d] job pairs",
					j.getJobId(),
					j.getUserId(),
					j.getNode(),
					j.getTimeout(),
					j.getJobPairs().size()));
			
			return rowsAffected == j.getJobPairs().size() + 1;
		} catch (Exception e){
			this.doRollback();			
			log.error(e.getMessage(), e);
			return false;
		} finally {
			this.enableAutoCommit();
		}
	}
	
	/**
	 * @return The next level to use when inserting into the levels table. (Essentially the largest right-value)
	 */
	public synchronized int getMaxLevel(){
		try {
			PreparedStatement psGetMaxLevel = statementPool.getStatement(StatementPool.GET_MAX_LEVEL);															
			ResultSet results = psGetMaxLevel.executeQuery();
			
			// If there are no levels, the first should start at 0
			if(!results.next()) {
				return 0;		
			}
			
			return results.getInt(1);
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return 0;
		}	
	}
	
	/**
	 * @return The next group id to use when inserting into the levels table.
	 */
	public synchronized int getNextLevelGroup(){
		try {
			PreparedStatement psGetMaxLevelGroup = statementPool.getStatement(StatementPool.GET_MAX_LEVEL_GROUP);			
			ResultSet results = psGetMaxLevelGroup.executeQuery();
			
			// If there are no current groups, then the first should be 0
			if(!results.next()) {
				return 0;		
			}
			
			return results.getInt(1) + 1;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return 0;
		}	
	}
	
	/**
	 * Updates the status of a job
	 * @param jobId The id of the job to update the status for
	 * @param status The status to set for the job
	 * @param node Which node the update is coming from (computer-node)
	 * @return True if the status is successfully set, false otherwise
	 */
	public synchronized boolean updateJobStatus(int jobId, String status, String node){
		try {
			PreparedStatement psJobStatusStart = statementPool.getStatement(StatementPool.SET_JOB_STATUS_START);
			PreparedStatement psJobStatusDone = statementPool.getStatement(StatementPool.SET_JOB_STATUS_DONE);															
			
			if(status.equals(R.JOB_STATUS_RUNNING)){
				// If this status update is for a running job, use the start prepared statement
				psJobStatusStart.setString(1, status);
				psJobStatusStart.setString(2, node);
				psJobStatusStart.setInt(3, jobId);
				return psJobStatusStart.executeUpdate() == 1;
			} else if(status.equals(R.JOB_STATUS_DONE) || status.equals(R.JOB_STATUS_ERR)) {
				// If this status update is for an error or a finishing job, use the done prepared statement
				psJobStatusDone.setString(1, status);
				psJobStatusDone.setString(2, node);
				psJobStatusDone.setInt(3, jobId);
				return psJobStatusDone.executeUpdate() == 1;
			}

			log.debug(String.format("Job status updated for job [%d] status [%s] node [%s]", jobId, status, node));
			
			// Return false if we don't recognize the status
			return false;			
		} catch (Exception e){			
			log.error(e.getMessage());
			return false;
		}
	}
	
	/**
	 * Updates a job pair to a given status. Not all fields are required, 
	 * only the given non-null or non-empty ones are updated in the database
	 * @param pairId The id of the pair to update
	 * @param status What the status should be set to
	 * @param result The result of the job pair (solver outcome)
	 * @param node Which node the pair ran on (computer-node)
	 * @param startTime When the job started
	 * @param endTime When the job ended
	 * @return True if the operation was a success, false otherwise
	 */
	public synchronized boolean updatePairResult(int pairId, String status, String result, String node, Long startTime, Long endTime){
		try {
			// This is a little different, for this method we use the built-in result updating feature
			// of JDBC to selectively update values that are given
			
			PreparedStatement psPairStatus = statementPool.getStatement(StatementPool.SET_PAIR_STATUS);									
			psPairStatus.setInt(1, pairId);
			
			ResultSet results = psPairStatus.executeQuery();
			results.first();
			
			if(!Util.isNullOrEmpty(result)) {
				results.updateString("result", result);
			}
			
			if(!Util.isNullOrEmpty(status)) { 
				results.updateString("status", status);
			}
			
			if(!Util.isNullOrEmpty(node)) {
				results.updateString("node", node);
			}
			
			if(startTime > 0) {
				results.updateTimestamp("startTime", new Timestamp(startTime));	// We get seconds since the epoch began, must convert to milliseconds for the constructor
			}
			
			if(endTime > 0) {
				results.updateTimestamp("endTime", new Timestamp(endTime));
			}
			
			// Perform the update on the current row
			results.updateRow();
			
			log.debug(String.format("Job pair status updated for pair [%d] status [%s] node [%s]", pairId, status, node));
			
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Gets all virtual directories listed under another (children directories)
	 * @param id The is the parent directory
	 * @return A list of children directories of the parent
	 */
	public synchronized List<Level> getSubLevels(int id){
		try {
			PreparedStatement psGetSubLevels = statementPool.getStatement(StatementPool.GET_SUBLEVELS);					
			psGetSubLevels.setInt(1, id);
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
				l.setCommunityId(results.getInt("comid"));
				
				returnList.add(l);
			}			
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	/**
	 * Gets all virtual directories listed under another (children directories) including
	 * benchmarks that are within each child sub-level
	 * @param id The is the parent directory
	 * @return A list of children directories of the parent
	 */
	public synchronized List<Level> getSubLevelsWithBench(int id){
		try {			
			PreparedStatement psGetSubLevels = statementPool.getStatement(StatementPool.GET_SUBLEVELS);									
			psGetSubLevels.setInt(1, id);
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
				l.setCommunityId(results.getInt("comid"));
				
				PreparedStatement psGetImmediateBench= statementPool.getStatement(StatementPool.GET_IMMEDIATE_BENCHMARKS);				
				psGetImmediateBench.setInt(1, l.getLeft());
				
				ResultSet benchResult = psGetImmediateBench.executeQuery();	
				while(benchResult.next()){
					Benchmark benchmark = new Benchmark();
					benchmark.setId(benchResult.getInt("id"));					
					benchmark.setUserId(benchResult.getInt("usr"));
					benchmark.setUploaded(benchResult.getTimestamp("uploaded"));
					benchmark.setPath(benchResult.getString("physical_path"));	// TODO: Eventually we want to hide this
					benchmark.setCommunityId(benchResult.getInt("comid"));
					l.getBenchmarks().add(benchmark);
				}
				
				returnList.add(l);
			}			
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	/**
	 * Gets all root virtual directories for all communities
	 * @param communityId The id of the community
	 * @return A list of root levels belonging to the community
	 */
	public synchronized List<Level> getRootLevels(int communityId){
		try {										
			PreparedStatement psGetRootLevels = statementPool.getStatement(StatementPool.GET_ROOT_LEVELS);
			psGetRootLevels.setInt(1, communityId);
			ResultSet results = psGetRootLevels.executeQuery();			
						
			ArrayList<Level> returnList = new ArrayList<Level>(10);
			
			while(results.next()){
				Level l = new Level(results.getInt("id"));
				l.setGroupId(results.getInt("gid"));
				l.setLeft(results.getInt("lft"));
				l.setRight(results.getInt("rgt"));
				l.setName(results.getString("name"));
				l.setUserId(results.getInt("usr"));
				l.setDescription(results.getString("description"));
				l.setCommunityId(results.getInt("comid"));
				
				PreparedStatement psGetImmediateBench= statementPool.getStatement(StatementPool.GET_IMMEDIATE_BENCHMARKS);				
				psGetImmediateBench.setInt(1, l.getLeft());
				
				ResultSet benchResult = psGetImmediateBench.executeQuery();	
				while(benchResult.next()){
					Benchmark benchmark = new Benchmark();
					benchmark.setId(benchResult.getInt("id"));					
					benchmark.setUserId(benchResult.getInt("usr"));
					benchmark.setUploaded(benchResult.getTimestamp("uploaded"));
					benchmark.setPath(benchResult.getString("physical_path"));	// TODO: Eventually we want to hide this
					benchmark.setCommunityId(benchResult.getInt("comid"));
					l.getBenchmarks().add(benchmark);
				}
				
				returnList.add(l);
			}			
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	/**
	 * @param levelId An id of a virtual level
	 * @return A list of all benchmark IDs that are contained within that level (including all sublevels)
	 */
	public synchronized List<Integer> levelToBenchmarkIds(int levelId){
		try {
			PreparedStatement psLevelToBenchs = statementPool.getStatement(StatementPool.GET_LEVELS_FROM_BENCH);															
			psLevelToBenchs.setInt(1, levelId);
			ResultSet results = psLevelToBenchs.executeQuery();
			ArrayList<Integer> returnList = new ArrayList<Integer>(5);
			
			while(results.next()) {
				returnList.add(results.getInt("id"));
			}
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage());
			return null;
		}	
	}
	
	/**
	 * @param solverId The solver id to find configurations for
	 * @return A list of configuration IDs that belong to the given solver
	 */
	public synchronized List<Integer> solverToConfigIds(int solverId){
		try {
			PreparedStatement psGetConfigIds= statementPool.getStatement(StatementPool.GET_CONFIGURATION_IDS);															
			psGetConfigIds.setInt(1, solverId);
			
			ResultSet results = psGetConfigIds.executeQuery();
			ArrayList<Integer> returnList = new ArrayList<Integer>(5);
			
			while(results.next()) {
				returnList.add(results.getInt("id"));
			}
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	/**
	 * Gets all job pairs associated with a job.
	 * @param id The id of the job to retrieve pairs for
	 * @return A list of job pairs under the job
	 */
	public synchronized List<JobPair> getJobPairs(int id){
		try {			
			PreparedStatement psGetJobPairs= statementPool.getStatement(StatementPool.GET_JOB_PAIRS);						
			psGetJobPairs.setInt(1, id);
			
			ResultSet results = psGetJobPairs.executeQuery();									 
			List<JobPair> returnList = new ArrayList<JobPair>(10);
			
			while(results.next()){
				Benchmark b = new Benchmark();
				Solver s = new Solver();
				Configuration c = new Configuration();
				JobPair p = new JobPair();
				
				p.setId(results.getInt("jp.id"));
				p.setJobId(results.getInt("jp.jid"));
				p.setResult(results.getString("jp.result"));
				
				// We may get back a null time, in which getting a timestamp will error out, just catch and ignore for now				
				try {p.setStartTime(results.getTimestamp("jp.startTime"));} catch(Exception e){}
				try {p.setEndTime(results.getTimestamp("jp.endTime"));} catch(Exception e){}				
				
				p.setNode(results.getString("jp.node"));
				p.setStatus(results.getString("jp.status"));
				p.setRunTime(results.getInt("runtime"));
				
				b.setId(results.getInt("b.id"));
				b.setPath(results.getString("b.physical_path"));
				p.setBenchmark(b);
				
				s.setId(results.getInt("s.id"));
				s.setName(results.getString("s.name"));
				
				p.setSolver(s);
				
				c.setId(results.getInt("c.id"));
				c.setName(results.getString("c.name"));
				p.setConfig(c);
				
				returnList.add(p);
			}			
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	/**
	 * Gets all jobs in the database (without job pair info)
	 * @return A list of jobs in the database
	 */
	public synchronized List<Job> getJobs(){
		try {	
			PreparedStatement psGetJobs = statementPool.getStatement(StatementPool.GET_JOBS);			
			ResultSet results = psGetJobs.executeQuery();									 
			List<Job> returnList = new ArrayList<Job>(10);
			
			while(results.next()){
				Job j = new Job();
				
				// Guard against null timestamps for now
				try{j.setCompleted(results.getTimestamp("finDate"));}catch(Exception e){}
				try{j.setSubmitted(results.getTimestamp("subDate"));}catch(Exception e){}
				j.setDescription(results.getString("description"));
				j.setJobId(results.getInt("id"));
				j.setNode(results.getString("node"));
				j.setStatus(results.getString("status"));							
				j.setTimeout(results.getLong("timeout"));
				j.setUserId(results.getInt("usr"));
				j.setRunTime(results.getInt("runtime"));
				j.setCommunityId(results.getInt("comid"));
				
				returnList.add(j);
			}			
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	public synchronized List<Configuration> getConfigurations(int id){
		try {		
			PreparedStatement psGetConfigurations = statementPool.getStatement(StatementPool.GET_CONFIGURATION_SOLVER);			
			psGetConfigurations.setInt(1, id);
			ResultSet results = psGetConfigurations.executeQuery();
			
			List<Configuration> returnList = new ArrayList<Configuration>(5);
			
			while(results.next()){
				Configuration c = new Configuration();
				
				c.setId(results.getInt("id"));
				c.setName(results.getString("name"));
				c.setNotes(results.getString("notes"));
				c.setSolverId(results.getInt("sid"));
				
				returnList.add(c);
			}			
						
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			return null;
		}	
	}
	
	/**
	 * Begins a transaction by turning off auto-commit
	 */
	protected void beginTransaction(){
		try {
			connection.setAutoCommit(false);
		} catch (Exception e) {
			// Ignore any errors
		}
	}
	
	/**
	 * Ends a transaction by commiting any changes and re-enabling auto-commit
	 */
	protected void endTransaction(){
		try {
			connection.commit();
			enableAutoCommit();
		} catch (Exception e) {
			this.doRollback();
		}
	}
	
	/**
	 * Turns on auto-commit
	 */
	protected void enableAutoCommit(){
		try {
			connection.setAutoCommit(true);
		} catch (Exception e) {
			// Ignore any errors
		}
	}
	
	/**
	 * Rolls back any actions not committed to the database
	 */
	protected void doRollback(){
		try {
			connection.rollback();
			log.warn("Database transaction rollback.");
		} catch (Exception e) {
			// Ignore any errors
		}
	}
}
