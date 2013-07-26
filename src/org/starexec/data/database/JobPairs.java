package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.util.Util;


public class JobPairs {
	private static final Logger log = Logger.getLogger(Jobs.class);
	
	
	/**
	 * Filters a list of job pairs against some search query. The query is compared to 
	 * solver, benchmark, and config names. The job pair is not filtered if the query
	 * is a case-insensitive substring of any of those names
	 * @param pairs The pairs to filter
	 * @param searchQuery The query
	 * @return A filtered list of job pairs
	 * @author Eric burns
	 */
	protected static List<JobPair> filterPairs(List<JobPair> pairs, String searchQuery) {
		//no filtering is necessary if there's no query
		if (searchQuery==null || searchQuery=="") {
			return pairs;
		}
		
		searchQuery=searchQuery.toLowerCase();
		List<JobPair> filteredPairs=new ArrayList<JobPair>();
		for (JobPair jp : pairs) {
			try {
				if (jp.getSolver().getName().toLowerCase().contains(searchQuery) || jp.getConfiguration().getName().toLowerCase().contains(searchQuery) ||
						jp.getBench().getName().toLowerCase().contains(searchQuery)) {
					filteredPairs.add(jp);
				}
			} catch (Exception e) {
				log.warn("filterPairs says jp with id = "+jp.getId()+" threw a null pointer");
			}
			
				
		}
		
		return filteredPairs;
	}
	
	/**
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 */ 
	private static int compareJobPairStrings(JobPair jp1, JobPair jp2, int sortIndex, boolean ASC) {
		int answer=0;
		try {
			String str1=null;
			String str2=null;
			if (sortIndex==1) {
				str1=jp1.getSolver().getName();
				str2=jp2.getSolver().getName();
			}
			if (sortIndex==2) {
				str1=jp1.getConfiguration().getName();
				str2=jp2.getConfiguration().getName();
			} else if (sortIndex==3) {
				str1=jp1.getStatus().getDescription();
				str2=jp2.getStatus().getDescription();
			} else if (sortIndex==5) {
				str1=jp1.getAttributes().getProperty("starexec-result");
				str2=jp2.getAttributes().getProperty("starexec-result");
			} else if (sortIndex==6) {
				str1=jp1.getJobSpaceName();
				str2=jp2.getJobSpaceName();
			} else {
				str1=jp1.getBench().getName();
				str2=jp2.getBench().getName();
			}
			//if str1 lexicographically follows str2, put str2 first
			if (str1.compareTo(str2)>0) {
				answer=1;
			}
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		//if we are descending, flip the answer
		if (!ASC) {
			answer=1-answer;
		}
		return answer;
	}
	
	/**
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 */ 
	private static int compareJobPairInts(JobPair jp1, JobPair jp2, int sortIndex, boolean ASC) {
		int answer=0;
		try {
			double db1=0;
			double db2=0;
			if (sortIndex==4) {
				db1=jp1.getCpuUsage();
				db2=jp2.getCpuUsage();
			} 
			//if db1> db2, then db2 should go first
			if (db1>db2) {
				answer=1;
			}
			
			
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		//if we are descending, flip the answer
		if (!ASC) {
			answer=1-answer;
		}
		return answer;
	}
	
	
	/**
	 * Given two lists of JobPairs that are sorted in the direction indicated by ASC, returns a single list
	 * containing all the JobPairs
	 * @param list1 The first list to merge
	 * @param list2 The second list to merge
	 * @param sortColumn The column to sort on. 0 = benchmark name
	 * 0 = benchmark name
	 * 1 = solver name
	 * 2 = config name
	 * 3 = status name
	 * 4 = cpu time
	 * 5 = starexec-result attr
	 * 6 = job space name
	 * any other = solver name
	 * @param ASC Whether the given lists are sorted ASC or DESC-- the returned list will be sorted the same way
	 * @return A single list containing all the elements of lists 1 and 2 in sorted order
	 */
	private static List<JobPair> mergeJobPairs(List<JobPair> list1, List<JobPair> list2, int sortColumn, boolean ASC) {
		
		int list1Index=0;
		int list2Index=0;
		int first;
		List<JobPair> mergedList=new ArrayList<JobPair>();
		while (list1Index<list1.size() && list2Index<list2.size()) {
			if (sortColumn!=4) {
				first=compareJobPairStrings(list1.get(list1Index),list2.get(list2Index),sortColumn,ASC);
			} else {
				first=compareJobPairInts(list1.get(list1Index),list2.get(list2Index),sortColumn,ASC);
			}
			if (first==0) {
				mergedList.add(list1.get(list1Index));
				list1Index++;
			} else {
				mergedList.add(list2.get(list2Index));
				list2Index++;
			}
		}
		if (list1Index==list1.size()) {
			mergedList.addAll(list2.subList(list2Index, list2.size()));
		} else {
			mergedList.addAll(list1.subList(list1Index, list1.size()));
		}
		return mergedList;
	}
	
	/**
	 * Sorts the given list of job pairs on the column given by the integer sortColumn
	 * @param pairs The list  of pairs to sort
	 * @param sortColumn The 
	 * @param Whether to sort ASC or DESC
	 * @return
	 */
	
	protected static List<JobPair> mergeSortJobPairs(List<JobPair> pairs,int sortColumn,boolean ASC) {
		if (pairs.size()<=1) {
			return pairs;
		}
		int middle=pairs.size()/2;
		List<JobPair> list1= mergeSortJobPairs(pairs.subList(0, middle),sortColumn,ASC);
		List<JobPair> list2=mergeSortJobPairs(pairs.subList(middle, pairs.size()),sortColumn,ASC);
		return mergeJobPairs(list1,list2,sortColumn,ASC);
		
		
	}
	
	
	/**
	 * Adds a job pair record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @param pair The pair to add
	 * @return True if the operation was successful
	 */
	protected static boolean addJobPair(Connection con, JobPair pair) throws Exception {
		CallableStatement procedure = null;
		 try {
			procedure = con.prepareCall("{CALL AddJobPair(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, pair.getJobId());
			procedure.setInt(2, pair.getBench().getId());
			procedure.setInt(3, pair.getSolver().getConfigurations().get(0).getId());
			procedure.setInt(4, StatusCode.STATUS_PENDING_SUBMIT.getVal());
			procedure.setInt(5, Util.clamp(1, R.MAX_PAIR_CPUTIME, pair.getCpuTimeout()));
			procedure.setInt(6, Util.clamp(1, R.MAX_PAIR_RUNTIME, pair.getWallclockTimeout()));
			procedure.setString(7, pair.getPath());
			procedure.setInt(8,pair.getJobSpaceId());
			// The procedure will return the pair's new ID in this parameter
			procedure.registerOutParameter(9, java.sql.Types.INTEGER);	
			procedure.executeUpdate();			

			// Update the pair's ID so it can be used outside this method
			pair.setId(procedure.getInt(9));

			return true;
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(procedure);
			
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
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL UpdatePairStatus(?, ?)}");
			procedure.setInt(1, pairId);
			procedure.setInt(2, statusCode);

			procedure.executeUpdate();								
			return true;
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL UpdateSGEPairStatus(?, ?)}");
			procedure.setInt(1, sgeId);
			procedure.setInt(2, statusCode);

			procedure.executeUpdate();								
			return true;
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
		} finally {			
			Common.safeClose(con);
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetSpaceByJobPairId(?)}");
			procedure.setInt(1, jobPairId);
			 results = procedure.executeQuery();
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
			Common.safeClose(procedure);
			Common.safeClose(results);
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetSGEIdByPairId(?)}");
			procedure.setInt(1, jobPairId);
			 results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("sge_id");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
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
		jp.setJobSpaceId(result.getInt("job_space_id"));
		
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
		CallableStatement procedure= null;
		try {
			Common.getDataPoolData();//just for logging
			con = Common.getConnection();	
			procedure = con.prepareCall("{CALL UpdatePairStats(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");			
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
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			log.info("Have connection and now getting sgeDetailed pair info for sgeId =  " + sgeId);
			 procedure = con.prepareCall("{CALL GetJobPairBySGE(?)}");
			procedure.setInt(1, sgeId);					
			 results = procedure.executeQuery();								
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
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			 procedure = con.prepareCall("{CALL GetPairAttrs(?)}");
			procedure.setInt(1, pairId);					
			 results = procedure.executeQuery();

			Properties prop = new Properties();

			while(results.next()){
				prop.put(results.getString("attr_key"), results.getString("attr_value"));				
			}			

			if(prop.size() <= 0) {
				prop = null;
			}
			return prop;
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			 procedure = con.prepareCall("{CALL GetJobPairById(?)}");
			procedure.setInt(1, pairId);					
			 results = procedure.executeQuery();

			if(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
				jp.setBench(Benchmarks.get(con, results.getInt("bench_id"),true));
				jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id"),true));
				jp.setAttributes(getAttributes(pairId));
				jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));

				Status s = new Status();
				s.setCode(results.getInt("status.code"));
				s.setStatus(results.getString("status.status"));
				s.setDescription(results.getString("status.description"));
				jp.setStatus(s);					
				jp.setJobSpaceName(results.getString("jobSpace.name"));
				return jp;
			}
		} catch (Exception e) {
			log.error("Get JobPair says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {			
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetJobPairById(?)}");
			procedure.setInt(1, pairId);					
			 results = procedure.executeQuery();

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
			Common.safeClose(procedure);
			Common.safeClose(results);
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
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();									
			procedure = con.prepareCall("{CALL SetSGEJobId(?, ?)}");

			procedure.setInt(1, pairId);
			procedure.setInt(2, sgeId);			
			procedure.executeUpdate();			

			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
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
		CallableStatement procedure= null;
		try {
			procedure = con.prepareCall("{CALL UpdateJobSpaceId(?, ?)}");
			procedure.setInt(1, jobPairId);
			procedure.setInt(2, jobSpaceId);
			procedure.executeUpdate();
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(procedure);
		}
		
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
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateJobPairStatus(?, ?)}");
			procedure.setInt(1, jobPairId);
			procedure.setInt(2, status_code);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return true;
	}
}
