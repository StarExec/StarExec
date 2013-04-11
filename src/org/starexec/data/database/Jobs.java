package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobSolver;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.Util;

/**
 * Handles all database interaction for jobs (NOT grid engine job execution, see JobManager for that)
 * @author Tyler Jensen
 */
public class Jobs {
	private static final Logger log = Logger.getLogger(Jobs.class);

	/**
	 * Adds a new job to the database. NOTE: This only records the job in the 
	 * database, this does not actually submit a job for execution (see JobManager.submitJob).
	 * This method also fills in the IDs of job pairs of the give job object.
	 * @param job The job data to add to the database
	 * @param spaceId The id of the space to add the job to
	 * @return True if the operation was successful, false otherwise.
	 */
	public static boolean add(Job job, int spaceId) {
		Connection con = null;

		try {
			con = Common.getConnection();

			Common.beginTransaction(con);

			Jobs.addJob(con, job);
			Jobs.associate(con, job.getId(), spaceId);

			for(JobPair pair : job) {
				pair.setJobId(job.getId());
				Jobs.addJobPair(con, pair);
			}

			Common.endTransaction(con);
			return true;
		} catch(Exception e) {
			Common.doRollback(con);
			log.error("add says " + e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;
	}

	/**
	 * Adds a job record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @param job The job to add
	 * @return True if the operation was successful
	 */
	private static boolean addJob(Connection con, Job job) throws Exception {				
		CallableStatement procedure = con.prepareCall("{CALL AddJob(?, ?, ?, ?, ?, ?, ?)}");
		procedure.setInt(1, job.getUserId());
		procedure.setString(2, job.getName());
		procedure.setString(3, job.getDescription());		
		procedure.setInt(4, job.getQueue().getId());

		// Only set pre and post processors if they're specified, else set to null
		if(job.getPreProcessor().getId() > 0) {
			procedure.setInt(5, job.getPreProcessor().getId());
		} else {
			procedure.setNull(5, java.sql.Types.INTEGER);
		}		
		if(job.getPostProcessor().getId() > 0) {
			procedure.setInt(6, job.getPostProcessor().getId());
		} else {
			procedure.setNull(6, java.sql.Types.INTEGER);
		}		

		// The procedure will return the job's new ID in this parameter
		procedure.registerOutParameter(7, java.sql.Types.INTEGER);	
		procedure.executeUpdate();			

		// Update the job's ID so it can be used outside this method
		job.setId(procedure.getInt(7));		

		return true;
	}

	/**
	 * Adds a new attribute to a job pair
	 * @param con The connection to make the update on
	 * @param pairId The id of the job pair the attribute is for
	 * @param key The key of the attribute
	 * @param val The value of the attribute
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static boolean addJobAttr(Connection con, int pairId, String key, String val) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddJobAttr(?, ?, ?)}");
		procedure.setInt(1, pairId);
		procedure.setString(2, key);
		procedure.setString(3, val);
		
		procedure.executeUpdate();
		return true;			
	}

	/**
	 * Adds a set of attributes to a job pair
	 * @param pairId The id of the job pair the attribute is for
	 * @param attributes The attributes to add to the job pair
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addJobAttributes(int pairId, Properties attributes) {
		Connection con = null;

		try {
			con = Common.getConnection();

			// For each attribute (key, value)...
			log.info("Adding " + attributes.entrySet().size() +" attributes to job pair " + pairId);
			for(Entry<Object, Object> keyVal : attributes.entrySet()) {
				// Add the attribute to the database
				Jobs.addJobAttr(con, pairId, (String)keyVal.getKey(), (String)keyVal.getValue());
			}	

			return true;
		} catch(Exception e) {			
			log.error("error adding Job Attributes = " + e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;		
	}

	/**
	 * @param pairId the id of the pair to update the status of
	 * @param statusCode the status code to set for the pair
	 * @return True if the operation was a success, false otherwise
	 */
	public static boolean setPairStatus(int pairId, int statusCode) {
		Connection con = null;

		try {
			con = Common.getConnection();

			CallableStatement procedure = con.prepareCall("{CALL UpdatePairStatus(?, ?)}");
			procedure.setInt(1, pairId);
			procedure.setInt(2, statusCode);

			procedure.executeUpdate();								
			return true;
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;
	}

	/**
	 * Updates a pair's status given the pair's sge id
	 * @param sgeId the SGE id of the pair to update the status of
	 * @param statusCode the status code to set for the pair
	 * @return True if the operation was a success, false otherwise
	 */
	public static boolean setSGEPairStatus(int sgeId, int statusCode) {
		Connection con = null;

		try {
			con = Common.getConnection();

			CallableStatement procedure = con.prepareCall("{CALL UpdateSGEPairStatus(?, ?)}");
			procedure.setInt(1, sgeId);
			procedure.setInt(2, statusCode);

			procedure.executeUpdate();								
			return true;
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;
	}

	/**
	 * Adds a job pair record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @param pair The pair to add
	 * @return True if the operation was successful
	 */
	private static boolean addJobPair(Connection con, JobPair pair) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddJobPair(?, ?, ?, ?, ?, ?, ?, ?)}");
		procedure.setInt(1, pair.getJobId());
		procedure.setInt(2, pair.getBench().getId());
		procedure.setInt(3, pair.getSolver().getConfigurations().get(0).getId());
		procedure.setInt(4, StatusCode.STATUS_PENDING_SUBMIT.getVal());
		procedure.setInt(5, Util.clamp(1, R.MAX_PAIR_CPUTIME, pair.getCpuTimeout()));
		procedure.setInt(6, Util.clamp(1, R.MAX_PAIR_RUNTIME, pair.getWallclockTimeout()));
		procedure.setInt(7, pair.getSpace().getId());

		// The procedure will return the pair's new ID in this parameter
		procedure.registerOutParameter(8, java.sql.Types.INTEGER);	
		procedure.executeUpdate();			

		// Update the pair's ID so it can be used outside this method
		pair.setId(procedure.getInt(8));

		return true;
	}

	/**
	 * Adds an association between all the given job ids and the given space
	 * @param jobIds the ids of the jobs we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> jobIds, int spaceId) {
		Connection con = null;			

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);								

			for(int jid : jobIds) {
				Jobs.associate(con, jid, spaceId);			
			}			

			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Adds an association between all the given job ids and the given space
	 * @param con The connection to make the association on
	 * @param jobId the id of the job we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static boolean associate(Connection con, int jobId, int spaceId) throws Exception {		
		CallableStatement procedure = null;						
		procedure = con.prepareCall("{CALL AssociateJob(?, ?)}");				
		procedure.setInt(1, jobId);
		procedure.setInt(2, spaceId);			
		procedure.executeUpdate();							

		return true;		
	}

	/**
	 * Update's a job pair's grid engine id
	 * @param pairId The id of the pair to update
	 * @param sgeId The grid engine id to set for the pair
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean updateGridEngineId(int pairId, int sgeId) {
		Connection con = null;			

		try {
			con = Common.getConnection();									
			CallableStatement procedure = con.prepareCall("{CALL SetSGEJobId(?, ?)}");

			procedure.setInt(1, pairId);
			procedure.setInt(2, sgeId);			
			procedure.executeUpdate();			

			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Retrieves a job from the database as well as its job pairs and its queue/processor info
	 * 
	 * @param jobId The id of the job to get information for 
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	public static Job getDetailed(int jobId) {
		log.info("getting detailed info for job " + jobId);
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setInt(1, jobId);					
			ResultSet results = procedure.executeQuery();
			Job j = new Job();
			if(results.next()){
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));				

				j.setQueue(Queues.get(con, results.getInt("queue_id")));
				j.setPreProcessor(Processors.get(con, results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con, results.getInt("post_processor")));
				if(con.isClosed())
				{
					log.warn("getDetailed - About to getPairs detailed for job " + jobId + " but connection to pass is closed.");
				}
				//j.setJobPairs(Jobs.getPairsDetailed(con, j.getId()));
			}
			else{
				j=null;
			}
			Common.closeResultSet(results);
			Common.safeClose(con);
			if (j != null){
				j.setJobPairs(Jobs.getPairsDetailed(j.getId()));
			}
			return j;

		} catch (Exception e){			
			log.error("job get detailed for job id = " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Retrieves a job from the database as well as its queue and processor information
	 * (excludes job pairs)
	 * 
	 * @param jobId The id of the job to get information for 
	 * @return A job object containing information about the requested job
	 * @author Todd Elvers
	 */
	public static Job getDetailedWithoutJobPairs(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setInt(1, jobId);					
			ResultSet results = procedure.executeQuery();

			if(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));				

				j.setQueue(Queues.get(con, results.getInt("queue_id")));
				j.setPreProcessor(Processors.get(con, results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con, results.getInt("post_processor")));

				return j;
			}	
			Common.closeResultSet(results);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Retrieves a job with basic info from the database (excludes pair and queue/processor info) 
	 * @param jobId The id of the job to get information for 
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	public static Job getShallow(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setInt(1, jobId);					
			ResultSet results = procedure.executeQuery();

			if(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));				

				j.getQueue().setId(results.getInt("queue_id"));
				j.getPreProcessor().setId(results.getInt("pre_processor"));
				j.getPostProcessor().setId(results.getInt("post_processor"));				

				return j;
			}		
			Common.closeResultSet(results);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Gets a list of jobs belonging to a space (without its job pairs but with job pair statistics)
	 * @param spaceId The id of the space to get jobs for
	 * @return A list of jobs existing directly in the space
	 * @author Tyler Jensen
	 */
	public static List<Job> getBySpace(int spaceId) {
		Connection con = null;			

		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceJobsById(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));					
				jobs.add(j);				
			}			
			Common.closeResultSet(results);			
			return jobs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Gets the job pair with the given id non-recursively 
	 * (Worker node, status, benchmark and solver will NOT be populated) 
	 * @param pairId The id of the pair to get
	 * @return The job pair object with the given id.
	 * @author Tyler Jensen
	 */
	public static JobPair getPair(int pairId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobPairById(?)}");
			procedure.setInt(1, pairId);					
			ResultSet results = procedure.executeQuery();

			if(results.next()){
				JobPair jp = Jobs.resultToPair(results);
				jp.getNode().setId(results.getInt("node_id"));
				jp.getStatus().setCode(results.getInt("status_code"));
				jp.getBench().setId(results.getInt("bench_id"));
				jp.getSolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
				return jp;
			}		
			Common.closeResultSet(results);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	/**
	 * Gets the job pair with the given id recursively 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param pairId The id of the pair to get
	 * @return The job pair object with the given id.
	 * @author Tyler Jensen
	 */
	public static JobPair getPairDetailed(int pairId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return Jobs.getPairDetailed(con, pairId);		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	/**
	 * Gets the job pair with the given id recursively 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param pairId The id of the pair to get
	 * @return The job pair object with the given id.
	 * @author Tyler Jensen
	 */
	protected static JobPair getPairDetailed(Connection con, int pairId) throws Exception {			
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairById(?)}");
		procedure.setInt(1, pairId);					
		ResultSet results = procedure.executeQuery();

		if(results.next()){
			JobPair jp = Jobs.resultToPair(results);
			jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
			jp.setBench(Benchmarks.get(con, results.getInt("bench_id")));
			jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id")));
			jp.setAttributes(Jobs.getAttributes(pairId));
			jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
			jp.setSpace(Spaces.get(results.getInt("space_id")));

			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);					
			Common.closeResultSet(results);
			return jp;
		}			

		return null;		
	}

	/**
	 * Retrieves all attributes (key/value) of the given job pair
	 * @param pairId The id of the job pair to get the attributes of
	 * @return The properties object which holds all the pair's attributes
	 * @author Tyler Jensen
	 */
	public static Properties getAttributes(int pairId) {
		Connection con = null;			

		try {
			con = Common.getConnection();		
			return Jobs.getAttributes(con, pairId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Retrieves all attributes (key/value) of the given job pair
	 * @param con The connection to make the query on
	 * @param pairId The id of the pair to get the attributes of
	 * @return The properties object which holds all the pair's attributes
	 * @author Tyler Jensen
	 */
	protected static Properties getAttributes(Connection con, int pairId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL GetPairAttrs(?)}");
		procedure.setInt(1, pairId);					
		ResultSet results = procedure.executeQuery();

		Properties prop = new Properties();

		while(results.next()){
			prop.put(results.getString("attr_key"), results.getString("attr_value"));				
		}			

		if(prop.size() <= 0) {
			prop = null;
		}
		Common.closeResultSet(results);
		return prop;
	}

	/**
	 * Gets the job pair with the given id recursively 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param sgeId The sge id of the pair to get
	 * @return The job pair object with the given id.
	 * @author Tyler Jensen
	 */
	public static JobPair getSGEPairDetailed(int sgeId) {
		log.info("getting SGEPairDetailed for sgeId = " + sgeId);
		Connection con = null;			 

		try {			
			con = Common.getConnection();
			return Jobs.getSGEPairDetailed(con, sgeId);		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	/**
	 * Gets the job pair with the given id recursively 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param sgeId The sge id of the pair to get
	 * @return The job pair object with the given id.
	 * @author Tyler Jensen
	 */
	protected static JobPair getSGEPairDetailed(Connection con, int sgeId) throws Exception {	
		log.info("Have connection and now getting sgeDetailed pair info for sgeId =  " + sgeId);
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairBySGE(?)}");
		procedure.setInt(1, sgeId);					
		ResultSet results = procedure.executeQuery();								
		if(results.next()){
			JobPair jp = Jobs.resultToPair(results);
			jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
			jp.setBench(Benchmarks.get(con, results.getInt("bench_id")));
			jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id")));
			jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));

			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);
			log.info("about to close result set for sgeId " + sgeId);
			Common.closeResultSet(results);
			return jp;
		}
		else
		{
			log.info("returning null for sgeDetailed, must have have been no results for GetJobPairBySGE with sgeId = " + sgeId);	
		}
		return null;		
	}

	/**
	 * Gets all job pairs for the given job non-recursively 
	 * (Worker node, status, benchmark and solver will NOT be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	public static List<JobPair> getPairs
	(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
			procedure.setInt(1, jobId);					
			ResultSet results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
				JobPair jp = Jobs.resultToPair(results);
				jp.getNode().setId(results.getInt("node_id"));
				jp.getStatus().setCode(results.getInt("status_code"));
				jp.getBench().setId(results.getInt("bench_id"));
				jp.getSolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
				returnList.add(jp);
			}			
			Common.closeResultSet(results);	
			return returnList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	/**
	 * Gets all job pairs for the given job non-recursively 
	 * (Worker node, status, benchmark and solver will NOT be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	protected static List<JobPair> getPairs(Connection con, int jobId) throws Exception {			
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
		procedure.setInt(1, jobId);					
		ResultSet results = procedure.executeQuery();
		List<JobPair> returnList = new LinkedList<JobPair>();

		while(results.next()){
			JobPair jp = Jobs.resultToPair(results);
			jp.getNode().setId(results.getInt("node_id"));
			jp.getStatus().setCode(results.getInt("status_code"));
			jp.getBench().setId(results.getInt("bench_id"));
			jp.getSolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
			returnList.add(jp);
		}			
		Common.closeResultSet(results);
		return returnList;				
	}

	/**
	 * Gets all job pairs for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jense, Benton Mccune, Eric Burns
	 */
	public static List<JobPair> getPairsDetailed(int jobId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			log.info("getting detailed pairs for job " + jobId );
			if(con.isClosed())
			{
				log.warn("GetPairsDetailed with Job Id = " + jobId + " but connection is closed.");
			}
			
			CallableStatement procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
			procedure.setInt(1, jobId);
			ResultSet results = procedure.executeQuery();
			Common.safeClose(con);
			List<JobPair> returnList = new ArrayList<JobPair>();
			
			Set<Integer> nodeIdSet = new HashSet<Integer>();
			Set<Integer> benchIdSet = new HashSet<Integer>();
			Set<Integer> configIdSet = new HashSet<Integer>();
			
			List<Integer> nodeIdList=new ArrayList<Integer>();
			List<Integer> benchIdList=new ArrayList<Integer>();
			List<Integer> configIdList=new ArrayList<Integer>();
			int curNode,curBench,curConfig;
			while(results.next()){
				log.debug("getting result to pair, result set closed = " + results.isClosed());
				JobPair jp = Jobs.resultToPair(results);
				
				Status s = new Status();
				s.setCode(results.getInt("status.code"));
				s.setStatus(results.getString("status.status"));
				s.setDescription(results.getString("status.description"));
				jp.setStatus(s);
				returnList.add(jp);
				curNode=results.getInt("node_id");
				curBench=results.getInt("bench_id");
				curConfig=results.getInt("config_id");
				nodeIdSet.add(curNode);
				benchIdSet.add(curBench);
				configIdSet.add(curConfig);
				nodeIdList.add(curNode);
				benchIdList.add(curBench);
				configIdList.add(curConfig);
				log.debug("Finished with results for pair " + jp.getId());
			}
			
			Object [] benchIdListSet=benchIdSet.toArray();
			Object [] nodeIdListSet=nodeIdSet.toArray();
			Object [] configIdListSet=configIdSet.toArray();
			
			Hashtable<Integer,Solver> neededSolvers=new Hashtable<Integer,Solver>();
			Hashtable<Integer,Configuration> neededConfigs=new Hashtable<Integer,Configuration>();
			Hashtable<Integer,Benchmark> neededBenchmarks=new Hashtable<Integer,Benchmark>();
			Hashtable <Integer, WorkerNode> neededNodes = new Hashtable<Integer, WorkerNode>();
			Common.closeResultSet(results);
			log.info("result set closed for job " + jobId);
			int curId=0;
			for (int i=0; i<configIdListSet.length;i++) {
				curId=(Integer)configIdListSet[i];
				neededConfigs.put(curId, Solvers.getConfiguration(curId));
				neededSolvers.put(curId, Solvers.getSolverByConfig(curId));
			}
			
			for (int i=0; i<nodeIdListSet.length; i++) {
				curId=(Integer)nodeIdListSet[i];
				neededNodes.put(curId, Cluster.getNodeDetails(curId));
			}
			
			for (int i=0; i<benchIdListSet.length;i++) {
				curId=(Integer)benchIdListSet[i];
				neededBenchmarks.put(curId,Benchmarks.get(curId));
			}
			
			for (Integer i =0; i < returnList.size(); i++){
				JobPair jp = returnList.get(i);
				jp.setNode(neededNodes.get(nodeIdList.get(i)));
				log.debug("set node for " + jp.getId());
				jp.setBench(neededBenchmarks.get(benchIdList.get(i)));
				log.debug("set bench for " + jp.getId());
				jp.setSolver(neededSolvers.get(configIdList.get(i)));
				log.debug("set solver for " + jp.getId());
				jp.setConfiguration(neededConfigs.get(configIdList.get(i)));
				log.debug("set configuration for " + jp.getId());
				log.debug("about to get attributes for jp " + jp.getId());
				jp.setAttributes(Jobs.getAttributes(jp.getId()));
				log.debug("just got attributes from jp + " + jp.getId());
			}
			log.info("returning detailed pairs for job " + jobId );
			return returnList;	
			
		} catch (Exception e){			
			log.error("getPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		}

		return null;		
	}
	
	
	/**
	 * Gets all job pairs for the given job and populates fields needed for getting relevant stats
	 * (Solver and Configuration populates, benchmark, and worker node are not) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Eric Burns
	 */
	public static List<JobPair> getPairsDetailedForStats(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			log.info("getting detailed pairs for job " + jobId );
			if(con.isClosed())
			{
				log.warn("GetPairsDetailed with Job Id = " + jobId + " but connection is closed.");
			}
			
			CallableStatement procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
			procedure.setInt(1, jobId);
			ResultSet results = procedure.executeQuery();
			Common.safeClose(con);
			
			List<JobPair> returnList = new ArrayList<JobPair>();
			Set<Integer> configIdSet = new HashSet<Integer>();
			List<Integer> configIdList=new ArrayList<Integer>();
			int curConfig;
			while(results.next()){
				log.debug("getting result to pair, result set closed = " + results.isClosed());
				JobPair jp = Jobs.resultToPair(results);
				
				Status s = new Status();
				s.setCode(results.getInt("status.code"));
				s.setStatus(results.getString("status.status"));
				s.setDescription(results.getString("status.description"));
				jp.setStatus(s);
				returnList.add(jp);

				curConfig=results.getInt("config_id");
				configIdSet.add(curConfig);
				configIdList.add(curConfig);
				log.debug("Finished with results for pair " + jp.getId());
			}
			
			Common.closeResultSet(results);
			log.info("result set closed for job " + jobId);
			
			con=Common.getConnection();
			CallableStatement procedure2 =con.prepareCall("{CALL GetJobAttrs(?)}");
			procedure2.setInt(1, jobId);
			ResultSet attrResults=procedure2.executeQuery();
			Common.safeClose(con);
			
			
			Hashtable<Integer, List<String>>attrs=new Hashtable<Integer, List<String>>();
			while (attrResults.next()) {
				
				int pairId=attrResults.getInt("pair_id");
				if (!attrs.contains(pairId)) {
					attrs.put(pairId, new LinkedList<String>());
				}
				List<String> curList=attrs.get(pairId);
				curList.add(attrResults.getString("attr_key"));
				curList.add(attrResults.getString("attr_value"));
			}
			
			Common.closeResultSet(attrResults);
			
			
			
			Object [] configIdListSet=configIdSet.toArray();
			Hashtable<Integer,Solver> neededSolvers=new Hashtable<Integer,Solver>();
			Hashtable<Integer,Configuration> neededConfigs=new Hashtable<Integer,Configuration>();
			
			int curId=0;
			for (int i=0; i<configIdListSet.length;i++) {
				curId=(Integer)configIdListSet[i];
				neededConfigs.put(curId, Solvers.getConfiguration(curId));
				neededSolvers.put(curId, Solvers.getSolverByConfig(curId));
			}

			
			for (Integer i =0; i < returnList.size(); i++){
				JobPair jp = returnList.get(i);
				jp.setSolver(neededSolvers.get(configIdList.get(i)));
				log.debug("set solver for " + jp.getId());
				jp.setConfiguration(neededConfigs.get(configIdList.get(i)));
				log.debug("set configuration for " + jp.getId());
				
				List<String> attrList=attrs.get(jp.getId());
				if (attrList==null) {
					continue;
				}
				int index=0;
				Properties newProps=new Properties();
				while (index<attrList.size()) {
					newProps.setProperty(attrList.get(index), attrList.get(index+1));
				}
				jp.setAttributes(newProps);
				
			}
			log.info("returning detailed pairs for job " + jobId );
			return returnList;	
			
		} catch (Exception e){			
			log.error("getPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		}
		return null;		
	}

	

	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Benton McCune
	 */
	public static List<JobPair> getPendingPairsDetailed(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return Jobs.getPendingPairsDetailed(con, jobId);
		} catch (Exception e){			
			log.error("getPendingPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author TBebnton
	 */
	protected static List<JobPair> getPendingPairsDetailed(Connection con, int jobId) throws Exception {	

		if(con.isClosed())
		{
			log.warn("GetPendingPairsDetailed with Job Id = " + jobId + " but connection is closed.");
		}
		CallableStatement procedure = con.prepareCall("{CALL GetPendingJobPairsByJob(?,?)}");
		procedure.setInt(1, jobId);					
		procedure.setInt(2, R.NUM_JOB_SCRIPTS);
		ResultSet results = procedure.executeQuery();
		List<JobPair> returnList = new LinkedList<JobPair>();

		while(results.next()){
			JobPair jp = Jobs.resultToPair(results);
			//jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));	
			jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
			//jp.setBench(Benchmarks.get(con, results.getInt("bench_id")));
			jp.setBench(Benchmarks.get(results.getInt("bench_id")));
			//jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id")));//not passing con
			jp.setSolver(Solvers.getSolverByConfig(results.getInt("config_id")));
			jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
			Status s = new Status();

			s.setCode(results.getInt("status_code"));
			//s.setStatus(results.getString("status.status"));
			//s.setDescription(results.getString("status.description"));
			jp.setStatus(s);
			jp.setAttributes(Jobs.getAttributes(con, jp.getId()));
			returnList.add(jp);
		}			

		Common.closeResultSet(results);
		return returnList;			
	}

	/**
	 * Helper method to extract information from a query for job pairs
	 * @param result The resultset that is the results from querying for job pairs
	 * @return A job pair object populated with data from the result set
	 */
	private static JobPair resultToPair(ResultSet result) throws Exception {

		JobPair jp = new JobPair();

		jp.setId(result.getInt("id"));
		jp.setJobId(result.getInt("job_id"));
		jp.setGridEngineId(result.getInt("sge_id"));	
		jp.setCpuTimeout(result.getInt("cpuTimeout"));		
		jp.setWallclockTimeout(result.getInt("clockTimeout"));
		jp.setQueueSubmitTime(result.getTimestamp("queuesub_time"));
		jp.setStartTime(result.getTimestamp("start_time"));
		jp.setEndTime(result.getTimestamp("end_time"));
		jp.setExitStatus(result.getInt("exit_status"));
		jp.setWallclockTime(result.getDouble("wallclock"));
		jp.setCpuUsage(result.getDouble("cpu"));
		jp.setUserTime(result.getDouble("user_time"));
		jp.setSystemTime(result.getDouble("system_time"));
		jp.setIoDataUsage(result.getDouble("io_data"));
		jp.setIoDataWait(result.getDouble("io_wait"));
		jp.setMemoryUsage(result.getDouble("mem_usage"));
		jp.setMaxVirtualMemory(result.getDouble("max_vmem"));
		jp.setMaxResidenceSetSize(result.getDouble("max_res_set"));
		jp.setPageReclaims(result.getDouble("page_reclaims"));
		jp.setPageFaults(result.getDouble("page_faults"));
		jp.setBlockInput(result.getDouble("block_input"));
		jp.setBlockOutput(result.getDouble("block_output"));
		jp.setVoluntaryContextSwitches(result.getDouble("vol_contex_swtch"));
		jp.setInvoluntaryContextSwitches(result.getDouble("invol_contex_swtch"));
		log.debug("getting job pair from result set for id " + jp.getId());
		return jp;
	}

	/** 
	 * @param code The status code to retrieve the status for
	 * @return A status object containing information about  the given status code
	 */
	public static Status getStatus(int code) throws Exception {
		Connection con = null;			

		try {
			con = Common.getConnection();									
			return Jobs.getStatus(code);
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}



	/**
	 * @param statusCode which status to filter grid engine ids by
	 * @return A list of SGE job id's that have the specified status
	 */
	public static List<Integer> getSgeIdsByStatus(int statusCode) {
		Connection con = null;			

		try {
			con = Common.getConnection();									
			CallableStatement procedure = con.prepareCall("{CALL GetSGEIdsByStatus(?)}");			
			procedure.setInt(1, statusCode);

			ResultSet results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<Integer>();

			while(results.next()){
				ids.add(results.getInt("sge_id"));
			}	
			Common.closeResultSet(results);
			return ids;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Updates a pair's statistics in the database with statistics contained within the given
	 * pair TO
	 * @param pair The pair that contains statistics to update with (must have an SGE id)
	 * @return True if the operation was a success, false otherwise
	 */
	public static boolean updatePairStatistics(JobPair pair) {		
		Connection con = null;			
		try {
			Common.getDataPoolData();//just for logging
			con = Common.getConnection();	
			CallableStatement procedure = con.prepareCall("{CALL UpdatePairStats(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");			
			procedure.setInt(1, pair.getGridEngineId());			
			procedure.setString(2, pair.getNode().getName());
			procedure.setTimestamp(3, pair.getQueueSubmitTime());
			procedure.setTimestamp(4, pair.getStartTime());
			procedure.setTimestamp(5, pair.getEndTime());
			procedure.setInt(6, pair.getExitStatus());
			procedure.setDouble(7, pair.getCpuUsage());
			procedure.setDouble(8, pair.getUserTime());
			procedure.setDouble(9, pair.getSystemTime());
			procedure.setDouble(10, pair.getIoDataUsage());
			procedure.setDouble(11, pair.getIoDataWait());
			procedure.setDouble(12, pair.getMemoryUsage());
			procedure.setDouble(13, pair.getMaxVirtualMemory());
			procedure.setDouble(14, pair.getMaxResidenceSetSize());
			procedure.setDouble(15, pair.getPageReclaims());
			procedure.setDouble(16, pair.getPageFaults());
			procedure.setDouble(17, pair.getBlockInput());
			procedure.setDouble(18, pair.getBlockOutput());
			procedure.setDouble(19, pair.getVoluntaryContextSwitches());
			procedure.setDouble(20, pair.getInvoluntaryContextSwitches());							

			procedure.executeUpdate();						
			return true;
		} catch (Exception e){			
			log.error("updatePairStatistics says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Gets the minimal number of Jobs necessary in order to service the client's
	 * request for the next page of Jobs in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Jobs from
	 * @param recordsPerPage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)
	 * @param spaceId the id of the space to get the Jobs from
	 * @return a list of 10, 25, 50, or 100 Jobs containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<Job> getJobsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int spaceId) {
		Connection con = null;			

		try {
			con = Common.getConnection();
			CallableStatement procedure;	

			procedure = con.prepareCall("{CALL GetNextPageOfJobs(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, spaceId);
			procedure.setString(6, searchQuery);

			ResultSet results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){

				// Grab the relevant job pair statistics; this prevents a secondary set of queries
				// to the database in RESTHelpers.java
				HashMap<String, Integer> liteJobPairStats = new HashMap<String, Integer>();
				liteJobPairStats.put("totalPairs", results.getInt("totalPairs"));
				liteJobPairStats.put("completePairs", results.getInt("completePairs"));
				liteJobPairStats.put("pendingPairs", results.getInt("pendingPairs"));
				liteJobPairStats.put("errorPairs", results.getInt("errorPairs"));

				Integer completionPercentage = Math.round(100*(float)(results.getInt("completePairs"))/((float)results.getInt("totalPairs")));
				liteJobPairStats.put("completionPercentage", completionPercentage);

				Integer errorPercentage = Math.round(100*(float)(results.getInt("errorPairs"))/((float)results.getInt("totalPairs")));
				liteJobPairStats.put("errorPercentage", errorPercentage);

				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));
				j.setLiteJobPairStats(liteJobPairStats);
				jobs.add(j);		
			}	
			Common.closeResultSet(results);
			return jobs;
		} catch (Exception e){			
			log.error("getJobsForNextPageSays " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}


	/**
	 * Gets the number of Jobs in a given space
	 * 
	 * @param spaceId the id of the space to count the Jobs in
	 * @return the number of Jobs
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetJobCountBySpace(?)}");
			procedure.setInt(1, spaceId);
			ResultSet results = procedure.executeQuery();
			int jobCount = 0;
			if (results.next()) {
				jobCount = results.getInt("jobCount");
			}
			Common.closeResultSet(results);
			return jobCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return 0;
	}


	/**
	 * Gets the minimal number of Job Pairs necessary in order to service the client's
	 * request for the next page of Job Pairs in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Job Pairs from
	 * @param recordsPerPage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)
	 * @param jobId the id of the Job to get the Job Pairs of
	 * @return a list of 10, 25, 50, or 100 Job Pairs containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<JobPair> getJobPairsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId) {
		Connection con = null;			
		try {
			con = Common.getConnection();
			CallableStatement procedure;	

			procedure = con.prepareCall("{CALL GetNextPageOfJobPairs(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, jobId);
			procedure.setString(6, searchQuery);

			ResultSet results = procedure.executeQuery();
			List<JobPair> jobPairs = new LinkedList<JobPair>();

			while(results.next()){

				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("job_pairs.id"));
				jp.setWallclockTime(results.getDouble("wallclock"));

				Benchmark bench = new Benchmark();
				bench.setId(results.getInt("bench.id"));
				bench.setName(results.getString("bench.name"));
				bench.setDescription(results.getString("bench.description"));

				Solver solver = new Solver();
				solver.setId(results.getInt("solver.id"));
				solver.setName(results.getString("solver.name"));
				solver.setDescription(results.getString("solver.description"));

				Configuration config = new Configuration();
				config.setId(results.getInt("config.id"));
				config.setName(results.getString("config.name"));
				config.setDescription(results.getString("config.description"));

				Space space = new Space();
				space.setId(results.getInt("space.id"));
				space.setName(results.getString("space.name"));
				space.setDescription(results.getString("space.description"));

				Status status = new Status();
				status.setCode(results.getInt("status.code"));
				status.setStatus(results.getString("status.status"));
				status.setDescription(results.getString("status.description"));

				Properties attributes = new Properties();
				attributes.setProperty(R.STAREXEC_RESULT, results.getString("result"));

				solver.addConfiguration(config);
				jp.setBench(bench);
				jp.setSolver(solver);
				jp.setSpace(space);
				jp.setStatus(status);
				jp.setAttributes(attributes);
				jobPairs.add(jp);		
			}	
			Common.closeResultSet(results);
			return jobPairs;
		} catch (Exception e){			
			log.error("get JobPairs for Next Page of Job " + jobId + " says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}
	
	/**
	 * Helper function for sortJobSolvers-- compares two JobSolvers based on the indexOfColumnSortedBy
	 * @return true if first<= second, false otherwise.
	 * @author Eric Burns
	 */
	
	
	private static boolean compareJobSolvers(JobSolver first, JobSolver second, int indexOfColumnSortedBy) {
		switch (indexOfColumnSortedBy) {
		case 0:
			return (first.getSolver().getName().compareTo(second.getSolver().getName())<=0);
		case 1: 
			return (first.getConfiguration().getName().compareTo(second.getConfiguration().getName())<=0);
		case 2:
			return first.getCompleteJobPairs()<=second.getCompleteJobPairs();
		case 3:
			return first.getIncompleteJobPairs()<=second.getIncompleteJobPairs();
		case 4:
			return first.getIncorrectJobPairs()<=second.getIncorrectJobPairs();
		case 5:
			return first.getErrorJobPairs()<=second.getErrorJobPairs();
		case 6:
			return first.getTime()<=second.getTime();
		default:
			return (first.getSolver().getName().compareTo(second.getSolver().getName())<=0);
		}
	}
	
	/**
	 * Helper function for GetJobStatsForNextPage-- sorts JobSolver objects based on column and ASC sent from client
	 * @author Eric Burns
	 */
	private static List<JobSolver> sortJobSolvers(List<JobSolver> rows, int indexOfSortedColumn, boolean isSortedASC) {
		if (rows.size()<=1) {
			return rows;
		}
		
		List<JobSolver> answer=new LinkedList<JobSolver>();
		answer.add(rows.get(0));
		for (int index=1;index<rows.size();index++) {
			boolean inserted=false;
			for (int index2=0;index2<answer.size();index2++) {
				if (compareJobSolvers(rows.get(index), answer.get(index2), indexOfSortedColumn)) {
					answer.add(index2, rows.get(index));
					inserted=true;
					break;
				}
				
				}
			if (!inserted) {
				answer.add(rows.get(index));
			}
		}
		
		if (!isSortedASC) {
			List<JobSolver> reversed=new LinkedList<JobSolver>();
			for (JobSolver js : answer) {
				reversed.add(0,js);
			}
			
			return reversed;
		}
		
		return answer;
	}
	
	
	
	/**
	 * Gets next page of solver statistics for job details page
	 * @param recordsPerPage-- returns a list of this size, or every record if value is less than 0
	 * @param total-- a reference to a 1-element int array used to return the total number of JobSolver objects
	 * @author Eric Burns
	 */

	public static List<JobSolver> getJobStatsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId, int [] total) {
		
		List<JobPair> pairs=getPairsDetailedForStats(jobId);
		Hashtable<String, JobSolver> JobSolvers=new Hashtable<String,JobSolver>();
		String key=null;
		for (JobPair jp : pairs) {
			key=String.valueOf(jp.getSolver().getId())+":"+String.valueOf(jp.getConfiguration().getId());
			if (!JobSolvers.containsKey(key)) {
				JobSolver newSolver=new JobSolver();
				newSolver.setSolver(jp.getSolver());
				newSolver.setConfiguration(jp.getConfiguration());
				JobSolvers.put(key, newSolver);
			}
			JobSolver curSolver=JobSolvers.get(key);
			Integer statusCode=jp.getStatus().getCode();
			curSolver.incrementTotalJobPairs();
			curSolver.incrementTime(jp.getWallclockTime());
			if ( (statusCode>=8 && statusCode<=17 ) || statusCode==0) { //status codes specified in STATUS.java
				curSolver.incrementErrorJobPairs();
			} else if (statusCode<=6 || statusCode==18) {
				curSolver.incrementIncompleteJobPairs();
			} else if (statusCode==7) {
				curSolver.incrementCompleteJobPairs();
				if (jp.getAttributes().contains("starexec-result") && jp.getAttributes().contains("starexec-expected")) {
					if (!jp.getAttributes().get("starexec-result").equals(jp.getAttributes().get("starexec-expected"))) {
						curSolver.incrementIncorrectJobPairs();
					}
				}
			}
		}
		List<JobSolver> returnValues=new LinkedList<JobSolver>();
		for (JobSolver js : JobSolvers.values()) {
			returnValues.add(js);
		}
		total[0]=returnValues.size();
		//carry out filtering function
		if (!searchQuery.equals("")) {
			searchQuery=searchQuery.toLowerCase();
			List<JobSolver> toRemove=new LinkedList<JobSolver>();
			for (JobSolver js : returnValues) {
				if ( (!js.getSolver().getName().toLowerCase().contains(searchQuery)) &&
				(!js.getConfiguration().getName().toLowerCase().contains(searchQuery)) ) {
					toRemove.add(js);
				}
			}
			for (JobSolver js : toRemove) {
				returnValues.remove(js);
			}
		}
		
		if (recordsPerPage<0) {
			recordsPerPage=returnValues.size()+1;
		}
		
		returnValues=sortJobSolvers(returnValues, indexOfColumnSortedBy, isSortedASC);
		List<JobSolver> sublist=null;
		if (recordsPerPage>returnValues.size()) {
			sublist=returnValues;
		} else if (startingRecord+recordsPerPage>returnValues.size()) {
			sublist=returnValues.subList(startingRecord, returnValues.size());
		} else {
			try {
				sublist=returnValues.subList(startingRecord, startingRecord+recordsPerPage); 
			} catch (IndexOutOfBoundsException e) {  //bad request-- starting record out of bounds
				if (recordsPerPage>returnValues.size()) {
					sublist=returnValues;
				} else {
					sublist=returnValues.subList(0, recordsPerPage);
				}
			}
			
		}
		return sublist;
	}
	
	/**
	 * Gets all JobSolver objects for a given job
	 * @param jobId the job in question
	 * @return every JobSolver associated with the given job
	 * @author Eric Burns
	 */
	
	public static List<JobSolver> getAllJobStats(int jobId) {
		return getJobStatsForNextPage(0 , -1, true , 0 , "" , jobId , new int [1] );
	}


	/**
	 * Returns the number of job pairs that exist for a given job
	 * 
	 * @param jobId the id of the job to get the number of job pairs for
	 * @return the number of job pairs for the given job
	 * @author Todd Elvers
	 */
	public static int getJobPairCount(int jobId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetJobPairCountByJob(?)}");
			procedure.setInt(1, jobId);
			ResultSet results = procedure.executeQuery();
			int jobPairCount=0;
			if (results.next()) {
				jobPairCount = results.getInt("jobPairCount");
			}
			Common.closeResultSet(results);
			return jobPairCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return 0;		
	}

	/**
	 * Get the total count of the jobs belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return The count of the jobs
	 * @author Ruoyu Zhang
	 */
	public static int getJobCountByUser(int userId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetJobCountByUser(?)}");
			procedure.setInt(1, userId);
			ResultSet results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return 0;		
	}

	/**
	 * Get next page of the jobs belong to a specific user
	 * @param startingRecord specifies the number of the entry where should the querry start
	 * @param recordsPerPage specifies how many records are going to be on one page
	 * @param isSortedASC specifies whether the sorting is in ascending order
	 * @param indexOfColumnSortedBy specifies which column the sorting is applied
	 * @param searchQuery the search query provided by the client
	 * @param userId Id of the user we are looking for
	 * @return a list of Jobs belong to the user
	 * @author Ruoyu Zhang
	 */
	public static List<Job> getJobsByUserForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int userId) {
		Connection con = null;			

		try {
			con = Common.getConnection();
			CallableStatement procedure;	

			procedure = con.prepareCall("{CALL GetNextPageOfUserJobs(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, userId);
			procedure.setString(6, searchQuery);

			ResultSet results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){

				// Grab the relevant job pair statistics; this prevents a secondary set of queries
				// to the database in RESTHelpers.java
				HashMap<String, Integer> liteJobPairStats = new HashMap<String, Integer>();
				liteJobPairStats.put("totalPairs", results.getInt("totalPairs"));
				liteJobPairStats.put("completePairs", results.getInt("completePairs"));
				liteJobPairStats.put("pendingPairs", results.getInt("pendingPairs"));
				liteJobPairStats.put("errorPairs", results.getInt("errorPairs"));

				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));
				j.setLiteJobPairStats(liteJobPairStats);
				jobs.add(j);		
			}
			return jobs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Get all the jobs belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return a list of Jobs belong to the user
	 */
	public static List<Job> getByUserId(int userId) {
		Connection con = null;			

		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetUserJobsById(?)}");
			procedure.setInt(1, userId);					
			ResultSet results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));					
				jobs.add(j);				
			}			

			return jobs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	public static boolean isPublic(int jobId) {
		log.debug("isPublic called on job " + jobId);
		Job j = Jobs.getDetailedWithoutJobPairs(jobId);
		if (j.getUserId()==R.PUBLIC_USER_ID){
			log.debug("Public User for Job Id" + jobId);
			return true;
		}
		Connection con = null;	
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL JobInPublicSpace(?)}");
			procedure.setInt(1, jobId);					
			ResultSet results = procedure.executeQuery();
			int count = 0;
			if (results.next()){
				count = (results.getInt("spaceCount"));
			}			
			log.debug("Job " + j.getName() + " is in " + count  + " public spaces");
			if (count > 0){
				return true;
			}

		} catch (Exception e){			
			log.error("isPublic says" + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return false;
	}
	//gets jobs with pending (or rejected) job pairs
	public static List<Job> getPendingJobs() {
		Connection con = null;					
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetPendingJobs()}");					
			ResultSet results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));	

				j.getQueue().setId(results.getInt("queue_id"));
				j.getPreProcessor().setId(results.getInt("pre_processor"));
				j.getPostProcessor().setId(results.getInt("post_processor"));	
				jobs.add(j);				
			}							
			return jobs;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	public static Integer getSizeOfQueue() {
		Connection con = null;					
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetNumEnqueuedJobs()}");					
			ResultSet results = procedure.executeQuery();

			Integer qSize = -1;
			while(results.next()){
				qSize = results.getInt("count");	
			}							
			return qSize;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}
}