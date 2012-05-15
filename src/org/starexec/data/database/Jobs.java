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

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
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
			log.error(e.getMessage(), e);
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
			for(Entry<Object, Object> keyVal : attributes.entrySet()) {
				// Add the attribute to the database
				Jobs.addJobAttr(con, pairId, (String)keyVal.getKey(), (String)keyVal.getValue());
			}	
			
			return true;
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
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
		CallableStatement procedure = con.prepareCall("{CALL AddJobPair(?, ?, ?, ?, ?, ?, ?)}");
		procedure.setInt(1, pair.getJobId());
		procedure.setInt(2, pair.getBench().getId());
		procedure.setInt(3, pair.getSolver().getConfigurations().get(0).getId());
		procedure.setInt(4, StatusCode.STATUS_PENDING_SUBMIT.getVal());				
		procedure.setInt(5, Util.clamp(1, R.MAX_PAIR_CPUTIME, pair.getCpuTimeout()));
		procedure.setInt(6, Util.clamp(1, R.MAX_PAIR_RUNTIME, pair.getWallclockTimeout()));
		
		// The procedure will return the pair's new ID in this parameter
		procedure.registerOutParameter(7, java.sql.Types.INTEGER);	
		procedure.executeUpdate();			
		
		// Update the pair's ID so it can be used outside this method
		pair.setId(procedure.getInt(7));
		
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
	 * Retrieves a job from the database as well as all of its job pairs 
	 * @param jobId The id of the job to get information for 
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	public static Job getDetailed(int jobId) {
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
				if(con.isClosed())
				{
					log.warn("getDetailed - About to getPairs detailed for job " + jobId + " but connection to pass is closed.");
				}
				//j.setJobPairs(Jobs.getPairsDetailed(con, j.getId()));
				j.setJobPairs(Jobs.getPairsDetailed(j.getId()));
				return j;
			}									
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
			jp.setAttributes(Jobs.getAttributes(con, pairId));
			
			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);					
			
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
			return Benchmarks.getAttributes(con, pairId);
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
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairBySGE(?)}");
		procedure.setInt(1, sgeId);					
		ResultSet results = procedure.executeQuery();
										
		if(results.next()){
			JobPair jp = Jobs.resultToPair(results);
			jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
			jp.setBench(Benchmarks.get(con, results.getInt("bench_id")));
			jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id")));
			
			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);
			
			return jp;
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
	public static List<JobPair> getPairs(int jobId) {
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
			
		return returnList;				
	}
	
	/**
	 * Gets all job pairs for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	public static List<JobPair> getPairsDetailed(int jobId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();		
			return Jobs.getPairsDetailed(con, jobId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;		
	}
	
	/**
	 * Gets all job pairs for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	protected static List<JobPair> getPairsDetailed(Connection con, int jobId) throws Exception {	
		
		if(con.isClosed())
		{
			log.warn("GetPairsDetailed with Job Id = " + jobId + " but connection is closed.");
		}
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
		procedure.setInt(1, jobId);					
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
			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);
			
			returnList.add(jp);
		}			
			
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
		jp.setWallclockTime(result.getLong("wallclock"));
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
			CallableStatement procedure = con.prepareCall("{CALL GetJobCountInSpace(?)}");
			procedure.setInt(1, spaceId);
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
}
