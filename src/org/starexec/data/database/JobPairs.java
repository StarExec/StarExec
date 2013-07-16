package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;


public class JobPairs {
	private static final Logger log = Logger.getLogger(Jobs.class);
	
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
	 * 
	 * @param jobPairId the Id of the jobPair
	 * @return the space that the job pair belongs to
	 */
	public static Space getSpace(int jobPairId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceByJobPairId(?)}");
			procedure.setInt(1, jobPairId);
			ResultSet results = procedure.executeQuery();
			if (results.next()) {
				Space s = new Space();
				s.setId(results.getInt("id"));
				s.setName(results.getString("name"));
				s.setCreated(results.getTimestamp("created"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));
				s.setPublic(results.getBoolean("public_access"));
				return s;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}
	
	/**
	 * Gets the SGEID of the given job pair
	 * @param jobPairId The ID of the job pair in question
	 * @return The SGE ID, or -1 on error
	 */

	public static int getSGEId(int jobPairId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetSGEIdByPairId(?)}");
			procedure.setInt(1, jobPairId);
			ResultSet results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("sge_id");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		return -1;
	}
	
	
	/**
	 * Helper method to extract information from a query for job pairs
	 * @param result The resultset that is the results from querying for job pairs
	 * @return A job pair object populated with data from the result set
	 */
	protected static JobPair resultToPair(ResultSet result) throws Exception {

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
		jp.setPath(result.getString("path"));
		//log.debug("getting job pair from result set for id " + jp.getId());
		return jp;
	}
	

	/**
	 * Given a resultset, populates only the fields of a job pair important for displaying stats.
	 * 
	 * @param result the result set
	 * @return A job pair with only a few fields populated.
	 * @throws Exception
	 */
	
	protected static JobPair shallowResultToPair(ResultSet result) throws Exception {
		JobPair jp = new JobPair();

		jp.setId(result.getInt("id"));
		jp.setJobId(result.getInt("job_id"));	
		jp.setWallclockTime(result.getDouble("wallclock"));
		jp.setCpuUsage(result.getDouble("cpu"));
		jp.setPath(result.getString("path"));
		//log.debug("getting job pair from result set for id " + jp.getId());
		return jp;
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
			return getSGEPairDetailed(con, sgeId);		
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
			JobPair jp = JobPairs.resultToPair(results);
			jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
			jp.setBench(Benchmarks.get(con, results.getInt("bench_id"),false));
			jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id"),true));
			jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));

			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);
			log.info("about to close result set for sgeId " + sgeId);
			Common.safeClose(results);
			return jp;
		}
		else
		{
			log.info("returning null for sgeDetailed, must have have been no results for GetJobPairBySGE with sgeId = " + sgeId);	
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
		Common.safeClose(results);
		return prop;
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
			return getAttributes(con, pairId);
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
			return getPairDetailed(con, pairId);		
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
			JobPair jp = JobPairs.resultToPair(results);
			jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
			jp.setBench(Benchmarks.get(con, results.getInt("bench_id"),true));
			jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id"),true));
			jp.setAttributes(getAttributes(pairId));
			jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
			jp.setSpace(Spaces.get(results.getInt("space_id")));

			Status s = new Status();
			s.setCode(results.getInt("status.code"));
			s.setStatus(results.getString("status.status"));
			s.setDescription(results.getString("status.description"));
			jp.setStatus(s);					
			Common.safeClose(results);
			return jp;
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
				JobPair jp = JobPairs.resultToPair(results);
				jp.getNode().setId(results.getInt("node_id"));
				jp.getStatus().setCode(results.getInt("status_code"));
				jp.getBench().setId(results.getInt("bench_id"));
				jp.getSolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
				return jp;
			}		
			Common.safeClose(results);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
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
	 * Updates the database to give the job pair with the given ID the given job space.
	 * @param jobPairId The ID of the job pair in question
	 * @param jobSpaceId The job space ID of the pair
	 * @param con The open connection to perform the update on
	 * @throws Exception
	 * @author Eric Burns
	 */
	
	public static void UpdateJobSpaces(int jobPairId, int jobSpaceId, Connection con) throws Exception {
		
		ResultSet results = null;
		CallableStatement procedure = con.prepareCall("{CALL UpdateJobSpaceId(?, ?)}");
		procedure.setInt(1, jobPairId);
		procedure.setInt(2, jobSpaceId);
		results = procedure.executeQuery();
		Common.safeClose(results);
		
	}
	
	/**
	 * Given a list of JobPair objects that have their jobSpaceIds set, updates the database
	 * to reflect these new job space ids
	 * @param jobPairs The pairs to update
	 * @return True on success and false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean UpdateJobSpaces(List<JobPair> jobPairs) {
		Connection con = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			for (JobPair jp : jobPairs) {
				UpdateJobSpaces(jp.getId(),jp.getJobSpaceId(),con);
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
		}		
		return false;
	}
	
	/**
	 * Updates the status of the given job pair, replacing its current status code with the given one
	 * @param jobPairId The ID of the job pair in question
	 * @param status_code The new status code to assign to the job pair
	 * @return True on success, false otherwise
	 */
	public static boolean UpdateStatus(int jobPairId, int status_code) {
		Connection con = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateJobPairStatus(?, ?)}");
			procedure.setInt(1, jobPairId);
			procedure.setInt(2, status_code);
			results = procedure.executeQuery();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
		}
		return true;
	}
}
