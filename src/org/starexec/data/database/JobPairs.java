package org.starexec.data.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverComparison;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.util.Util;


public class JobPairs {
	private static final Logger log = Logger.getLogger(JobPairs.class);
	
	
	/**
	 * Adds a job pair record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @param pair The pair to add
	 * @return True if the operation was successful
	 */
	protected static boolean addJobPair(Connection con, JobPair pair) throws Exception {
		CallableStatement procedure = null;
		 try {
			procedure = con.prepareCall("{CALL AddJobPair(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)}");
			procedure.setInt(1, pair.getJobId());
			procedure.setInt(2, pair.getBench().getId());
			procedure.setInt(3, pair.getSolver().getConfigurations().get(0).getId());
			procedure.setInt(4, StatusCode.STATUS_PENDING_SUBMIT.getVal());
			procedure.setInt(5, Util.clamp(1, R.MAX_PAIR_CPUTIME, pair.getCpuTimeout()));
			procedure.setInt(6, Util.clamp(1, R.MAX_PAIR_RUNTIME, pair.getWallclockTimeout()));
			procedure.setString(7, pair.getPath());
			procedure.setInt(8,pair.getJobSpaceId());
			procedure.setString(9,pair.getSolver().getConfigurations().get(0).getName());
			procedure.setString(10,pair.getSolver().getName());
			procedure.setString(11,pair.getBench().getName());
			procedure.setInt(12,pair.getSolver().getId());
			procedure.setLong(13, pair.getMaxMemory());
			// The procedure will return the pair's new ID in this parameter
			procedure.registerOutParameter(14, java.sql.Types.INTEGER);	
			procedure.executeUpdate();			

			// Update the pair's ID so it can be used outside this method
			pair.setId(procedure.getInt(14));

			return true;
		} catch (Exception e) {
			log.error("addJobPair says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			
		}
		return false;
	}
	
	public static HashMap<Integer,Integer> getAllPairsForProcessing() {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetPairsToBeProcessed(?)}");
			procedure.setInt(1,StatusCode.STATUS_PROCESSING.getVal());
			results=procedure.executeQuery();
			HashMap<Integer,Integer> mapping=new HashMap<Integer,Integer>();
			while (results.next()) {
				mapping.put(results.getInt("id"),results.getInt("post_processor"));
			}
			return mapping;
			
		} catch (Exception e){
			log.error("getAllPairsForProcessing says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Post processes the given pair with the given processor ID,
	 * add the properties to the pair attributes table, and removes
	 * the pair from the processing job pairs table
	 * @param pairId The ID of the pair to process
	 * @param processorId The ID of the processor to use
	 * @return True on success, false on error
	 */
	
	public static boolean postProcessPair(int pairId,int processorId) {
		Connection con=null;
		try {
			Properties props=runPostProcessorOnPair(pairId,processorId);
			con=Common.getConnection();
			Common.beginTransaction(con);
			JobPairs.addJobPairAttributes(pairId, props,con);
			JobPairs.setPairStatus(pairId, StatusCode.STATUS_COMPLETE.getVal(),con);
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			Common.doRollback(con);
			log.error("postProcessPair says "+e.getMessage(),e);
		} finally {
			Common.endTransaction(con);
			Common.safeClose(con);
			
		}
		return false;
	}
	
	/**
	 * Runs the given post processor on the given pair and returns the properties that were obtained
	 * @param pairId The ID of the pair in question
	 * @param processorId The ID of the processor in question
	 * @return The properties on success, or null otherwise
	 */
	//TODO: Sandbox this
	private static Properties runPostProcessorOnPair(int pairId, int processorId) {
		try {
			Processor p=Processors.get(processorId);
			// Run the processor on the benchmark file
			String [] procCmd = new String[3];
			procCmd[0] = "./"+R.PROCSSESSOR_RUN_SCRIPT; 
			
			JobPair pair=JobPairs.getPairDetailed(pairId);
			procCmd[1] = JobPairs.getFilePath(pair);
			
			procCmd[2] = pair.getBench().getPath();
			String propstr = Util.executeCommand(procCmd, null, new File(p.getFilePath()));
			
			// Load results into a properties file
			Properties prop = new Properties();
			prop.load(new StringReader(propstr));

			return prop;
		} catch (Exception e) {
			log.error("runPostProcessorOnPair says "+e.getMessage(),e);
		} 
		return null;
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
	protected static boolean addJobPairAttr(Connection con, int pairId, String key, String val) throws Exception {
		CallableStatement procedure = null;
		 try {
			procedure = con.prepareCall("{CALL AddJobAttr(?, ?, ?)}");
			procedure.setInt(1, pairId);
			procedure.setString(2, key);
			procedure.setString(3, val);
			
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("addJobAttr says "+e.getMessage(),e);
		}	finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Adds the list of attributes to the given job pair. If old attributes
	 * have the same keys as new ones, the old ones are replaced
	 * @param pairId The ID of the pair to add attributes to
	 * @param attributes The key/value attributes
	 * @param con The open connection to make the call on
	 * @return True on success, false on error
	 */
	public static boolean addJobPairAttributes(int pairId, Properties attributes, Connection con) {
		try {
			// For each attribute (key, value)...
			log.info("Adding " + attributes.entrySet().size() +" attributes to job pair " + pairId);
			for(Entry<Object, Object> keyVal : attributes.entrySet()) {
				// Add the attribute to the database
				JobPairs.addJobPairAttr(con, pairId, (String)keyVal.getKey(), (String)keyVal.getValue());
			}	

			return true;
		} catch (Exception e) {
			log.error("addJobPairAttributes says "+e.getMessage(),e);
		} 
		return false;
	}
	
	/**
	 * Adds a set of attributes to a job pair
	 * @param pairId The id of the job pair the attribute is for
	 * @param attributes The attributes to add to the job pair
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addJobPairAttributes(int pairId, Properties attributes) {
		Connection con = null;

		try {
			con = Common.getConnection();
			return addJobPairAttributes(pairId,attributes,con);
		} catch(Exception e) {			
			log.error("error adding Job Attributes = " + e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;		
	}

	
	/**
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */ 
	private static int compareJobPairNums(JobPair jp1, JobPair jp2, int sortColumn, boolean ASC, boolean isWallclock) {
		int answer=0;
		try {
			double db1=0;
			double db2=0;
			if (sortColumn==6) {
				db1=jp1.getId();
				db2=jp2.getId();
			} else if (sortColumn==7) {
				db1=jp1.getCompletionId();
				db2=jp2.getCompletionId();
			} else {
				if (isWallclock) {
					db1=jp1.getWallclockTime();
					db2=jp2.getWallclockTime();
				} else {
					db1=jp1.getCpuTime();
					db2=jp2.getCpuTime();
				}
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
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param sortIndex the value to sort on
	 * 0 = bench name
	 * 1 = solver name
	 * 2 = config name
	 * 3 = status name
	 * 5 = starexec-result attr
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */ 
	private static int compareJobPairStrings(JobPair jp1, JobPair jp2, int sortIndex, boolean ASC) {
		int answer=0;
		try {
			String str1=null;
			String str2=null;
			if (sortIndex==3) {
				str1=jp1.getStatus().getStatus();
				str2=jp2.getStatus().getStatus();
			}
			else if (sortIndex==5) {
				str1=jp1.getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=jp2.getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (sortIndex==0) {
				str1=jp1.getBench().getName();
				str2=jp2.getBench().getName();
			} else if (sortIndex==2) {
				str1=jp1.getConfiguration().getName();
				str2=jp2.getConfiguration().getName();
			} else {
				str1=jp1.getSolver().getName();
				str2=jp2.getSolver().getName();
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
	
	protected static List<JobPair> filterPairsByType(List<JobPair> pairs, String type) {

		log.debug("filtering pairs by type with type = "+type);
		List<JobPair> filteredPairs=new ArrayList<JobPair>();

		if (type.equals("incomplete")) {
			for (JobPair jp : pairs) {
				if (jp.getStatus().getCode().statIncomplete()) {
					filteredPairs.add(jp);
				}
			}
		} else if (type.equals("resource")) {
			for (JobPair jp : pairs) {
				if (jp.getStatus().getCode().resource()) {
					filteredPairs.add(jp);
				}
			}
		} else if (type.equals("solved")) {
			for (JobPair jp : pairs) {
				if (JobPairs.isPairCorrect(jp)==0) {
					filteredPairs.add(jp);
				}
			}
		}  else if (type.equals("wrong")) {
			for (JobPair jp : pairs) {
				if (JobPairs.isPairCorrect(jp)==1) {
					filteredPairs.add(jp);
				}
			}
		}  else if (type.equals("unknown")) {
			for (JobPair jp : pairs) {
				if (JobPairs.isPairCorrect(jp)==2) {
					filteredPairs.add(jp);
				}
			}
		} else if (type.equals("complete")) {
			for (JobPair jp : pairs) {
				if (jp.getStatus().getCode().complete()) {
					filteredPairs.add(jp);
				}
			}
		} else {
			filteredPairs=pairs;
		}
		return filteredPairs;
	}
	/**
	 * Checks whether a given pair is correct
	 * @param jp
	 * @return
	 * -1 == pair is not complete (as in, does not have STATUS_COMPLETE)
	 * 0 == pair is correct
	 * 1 == pair is incorrect
	 * 2 == pair is unknown
	 */
	public static int isPairCorrect(JobPair jp) {
		log.debug("checking whether pair with id = "+jp.getId() +" is correct");
		StatusCode statusCode=jp.getStatus().getCode();

		if (statusCode.getVal()==StatusCode.STATUS_COMPLETE.getVal()) {
			if (jp.getAttributes()!=null) {
			   	Properties attrs = jp.getAttributes();
			   	log.debug("expected = "+attrs.get(R.EXPECTED_RESULT));
			   	log.debug("actual = "+attrs.get(R.STAREXEC_RESULT));
			   	if (attrs.containsKey(R.STAREXEC_RESULT) && attrs.get(R.STAREXEC_RESULT).equals(R.STAREXEC_UNKNOWN)){
		    		//don't know the result, so don't mark as correct or incorrect.	
			   		return 2;
			   		
			   	// the expected result -- just means that no expected result actually existed
	    		} else if (attrs.containsKey(R.EXPECTED_RESULT) && !attrs.get(R.EXPECTED_RESULT).equals("--")) {
	    			//no result is counted as wrong
		    		if (!attrs.containsKey(R.STAREXEC_RESULT) || !attrs.get(R.STAREXEC_RESULT).equals(attrs.get(R.EXPECTED_RESULT))) {
			   			return 1;
		    		} else {
		    			return 0;
		    		}
			   	} else {
				   	//if the attributes don't have an expected result, we will just mark as correct
			   		return 0;

			   	}
		    } else {
		    	return 0;
		    }
		} else {
			return -1;
		}
	}
	
	/**
	 * Filters a list of solver comparisons against a given query
	 * @param comparisons
	 * @param searchQuery
	 * @return
	 */
	protected static List<SolverComparison> filterComparisons(List<SolverComparison> comparisons, String searchQuery) {
		//no filtering is necessary if there's no query
		if (searchQuery==null || searchQuery=="") {
			return comparisons;
		}
		
		searchQuery=searchQuery.toLowerCase();
		List<SolverComparison> filteredComparisons=new ArrayList<SolverComparison>();
		for (SolverComparison c : comparisons) {
			try {
				if (c.getBenchmark().getName().toLowerCase().contains(searchQuery)) {
					filteredComparisons.add(c);
				}
			} catch (Exception e) {
			}	
		}
		
		return filteredComparisons;
	}
	
	/**
	 * Filters a list of job pairs against some search query. The query is compared to 
	 * solver, benchmark, and config names, as well as integer status code. The job pair is not filtered if the query
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
				if (jp.getBench().getName().toLowerCase().contains(searchQuery) || String.valueOf(jp.getStatus().getCode().getVal()).equals(searchQuery)
						|| jp.getSolver().getName().toLowerCase().contains(searchQuery) || jp.getConfiguration().getName().toLowerCase().contains(searchQuery)) {
					filteredPairs.add(jp);
				}
			} catch (Exception e) {
				log.warn("filterPairs says jp with id = "+jp.getId()+" threw a null pointer");
			}	
		}
		
		return filteredPairs;
	}
	
	/**
	 * This is a helper function for transferOutputFilesToNewDirectory. It should 
	 * not be used for anything else
	 * @return A list of job pairs with the necessary fields popoulated
	 * @author Eric Burns
	 */
	
	private static List<JobPair> getAllPairsForMovingOutputFiles() {
		Connection con=null;
		ResultSet results=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetAllPairsShallow()}");
			results=procedure.executeQuery();
			List<JobPair> pairs = new ArrayList<JobPair>();
			while (results.next()) {
				JobPair pair=new JobPair();
				pair.setPath(results.getString("path"));
				pair.setJobId(results.getInt("job_id"));
				Solver s=pair.getSolver();
				s.setName(results.getString("solver_name"));
				Configuration c=pair.getConfiguration();
				c.setName(results.getString("config_name"));
				Benchmark b=pair.getBench();
				b.setName(results.getString("bench_name"));
				
				pair.setId(results.getInt("user_id"));
				pairs.add(pair);
			}
			return pairs;
		} catch (Exception e) {
			log.debug("getAllPairsShallow says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
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
			log.error("getAttributes says "+e.getMessage(),e);
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
		log.debug("Calling JobPairs.getAttributes for an individual pair");
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
	 * Gets the path to the output file  for this pair.
	 * @param pairId The id of the pair to get the filepath for
	 * @return The string path, or null on failure
	 * @author Eric Burns
	 */
	
	public static String getLogPath(int pairId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL getJobPairFilePathInfo(?)}");
			procedure.setInt(1,pairId);
			results=procedure.executeQuery();
			if (results.next()) {
				JobPair pair=new JobPair();
				Solver s= pair.getSolver();
				s.setName(results.getString("solver_name"));
				Benchmark b=pair.getBench();
				b.setName(results.getString("bench_name"));
				Configuration c=pair.getConfiguration();
				c.setName(results.getString("config_name"));
				pair.setJobId(results.getInt("job_id"));
				pair.setPath(results.getString("path"));
				return JobPairs.getLogFilePath(pair);
				
			}
		} catch (Exception e) {
			log.debug("getFilePath says "+e.getMessage(),e);
		}finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Gets the path to the output file  for this pair.
	 * @param pairId The id of the pair to get the filepath for
	 * @return The string path, or null on failure
	 * @author Eric Burns
	 */
	
	public static String getFilePath(int pairId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL getJobPairFilePathInfo(?)}");
			procedure.setInt(1,pairId);
			results=procedure.executeQuery();
			if (results.next()) {
				JobPair pair=new JobPair();
				Solver s= pair.getSolver();
				s.setName(results.getString("solver_name"));
				Benchmark b=pair.getBench();
				b.setName(results.getString("bench_name"));
				Configuration c=pair.getConfiguration();
				c.setName(results.getString("config_name"));
				pair.setJobId(results.getInt("job_id"));
				pair.setPath(results.getString("path"));
				return getFilePath(pair);
			}
		} catch (Exception e) {
			log.debug("getFilePath says "+e.getMessage(),e);
		}finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Removes a specific pair from the job_pair_completion table
	 * @param pairId The ID of the pair being removed
	 * @return True on success and false otherwise
	 */
	
	public static boolean removePairFromCompletedTable(int pairId) {
		Connection con=null;
		CallableStatement procedure=null;
		
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("CALL RemovePairFromCompletedTable(?)");
			procedure.setInt(1,pairId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}
	
	/**
     * Returns the log of a job pair by reading
     * in the physical log file into a string.
     * @param pairId The id of the pair to get the log for
     * @return The log of the job run
     */
    public static String getJobLog(int pairId) {
    	try {
    		
    		String logPath = JobPairs.getLogPath(pairId);
    	
    		File logFile = new File(logPath);

    		if(logFile.exists()) {
    			return FileUtils.readFileToString(logFile);
    		}
    	} catch (Exception e) {
    		log.warn(e.getMessage(), e);
    	}

    	return null;
    }
	/**
	 * Returns the absolute path to where the log for a pair is stored given the pair.
	 * @param pair
	 * @return
	 */
	public static String getLogFilePath(JobPair pair) {
		try {
			File file=new File(Jobs.getLogDirectory(pair.getJobId()));
			log.debug("trying to find log at path = "+file.getAbsolutePath());
			String[] pathSpaces=pair.getPath().split("/");
			for (String space : pathSpaces) {
				file=new File(file,space);
			}

			file=new File(file,pair.getSolver().getName()+"___"+pair.getConfiguration().getName());

			file=new File(file,pair.getBench().getName());
			
			
			log.debug("found the path "+file.getAbsolutePath()+" for the job pair");
			return file.getAbsolutePath();
		} catch(Exception e) {
			log.error("getFilePath says "+e.getMessage(),e);
		}
		return null;
		
	}
	
	/**
	 * Gets the path to the output file  for this pair. Requires that the 
	 * jobId, path, solver name, config name, and bench names be populated
	 * @param pair The pair to get the filepath for
	 * @return The string path, or null on failure
	 * @author Eric Burns
	 */
	
	public static String getFilePath(JobPair pair) {
		try {
			File file=new File(Jobs.getDirectory(pair.getJobId()));
			String[] pathSpaces=pair.getPath().split("/");
			for (String space : pathSpaces) {
				file=new File(file,space);
			}

			file=new File(file,pair.getSolver().getName()+"___"+pair.getConfiguration().getName());

			file=new File(file,pair.getBench().getName());
			
			if (!file.exists()) {	    // if the job output could not be found
				File testFile=new File(file,pair.getSolver().getName());
				testFile=new File(testFile,pair.getConfiguration().getName());
				testFile=new File(testFile,pair.getBench().getName());
				if (testFile.exists()) {  //check the alternate path some pairs are still stored at
					FileUtils.copyFile(testFile, file);
					if (file.exists()) {
						testFile.delete();
					}
				}
			}
			
			return file.getAbsolutePath();
		} catch(Exception e) {
			log.error("getFilePath says "+e.getMessage(),e);
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
				s.setCode(results.getInt("status_code"));
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
	 * Given two lists of JobPairs that are sorted in the direction indicated by ASC, returns a single list
	 * containing all the JobPairs
	 * @param list1 The first list to merge
	 * @param list2 The second list to merge
	 * @param sortColumn The column to sort on.
	 * 0 = bench name
	 * 1 = solver name
	 * 2 = config name
	 * 3 = status name
	 * 4 = wallclock time
	 * 5 = starexec-result attr
	 * 6 pair id
	 * 7 completion id
	 * any other = solver name
	 * @param ASC Whether the given lists are sorted ASC or DESC-- the returned list will be sorted the same way
	 * @return A single list containing all the elements of lists 1 and 2 in sorted order
	 */
	private static List<JobPair> mergeJobPairs(List<JobPair> list1, List<JobPair> list2, int sortColumn, boolean ASC, boolean wallclock) {
		
		int list1Index=0;
		int list2Index=0;
		int first;
		List<JobPair> mergedList=new ArrayList<JobPair>();
		while (list1Index<list1.size() && list2Index<list2.size()) {
			if (sortColumn!=4 &&sortColumn!=6 &&sortColumn!=7) {
				first=compareJobPairStrings(list1.get(list1Index),list2.get(list2Index),sortColumn,ASC);
			} else {
				first=compareJobPairNums(list1.get(list1Index),list2.get(list2Index),sortColumn,ASC,wallclock);
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
	 * @param sortColumn Determines what attribute pairs are sorted on.
	 * 0 = bench name
	 * 1 = solver name
	 * 2 = config name
	 * 3 = status name
	 * 4 = wallclock or cpu time (depending on wallclock variable)
	 * 5 = starexec-result attr
	 * 6 pair id
	 * 7 completion id
	 * @param Whether to sort ASC or DESC
	 * @return
	 */
	
	protected static List<JobPair> mergeSortJobPairs(List<JobPair> pairs,int sortColumn,boolean ASC, boolean wallclock) {
		if (pairs.size()<=1) {
			return pairs;
		}
		int middle=pairs.size()/2;
		List<JobPair> list1= mergeSortJobPairs(pairs.subList(0, middle),sortColumn,ASC,wallclock);
		List<JobPair> list2=mergeSortJobPairs(pairs.subList(middle, pairs.size()),sortColumn,ASC,wallclock);
		return mergeJobPairs(list1,list2,sortColumn,ASC,wallclock);
	}
	
	private static List<SolverComparison> mergeSolverComparisons(List<SolverComparison> list1, List<SolverComparison> list2, int sortColumn, boolean ASC, boolean wallclock) {
		
		int list1Index=0;
		int list2Index=0;
		int first;
		List<SolverComparison> mergedList=new ArrayList<SolverComparison>();
		while (list1Index<list1.size() && list2Index<list2.size()) {
			if (sortColumn==0 || sortColumn==4 || sortColumn==5) {
				first=compareSolverComparisonStrings(list1.get(list1Index),list2.get(list2Index),sortColumn,ASC);
			} else {
				first=compareSolverComparisonNums(list1.get(list1Index),list2.get(list2Index),sortColumn,ASC,wallclock);
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
	 * 
	 * @param jp1
	 * @param jp2
	 * @param sortIndex
	 * 1 pair 1 time
	 * 2 pair 2 time
	 * 3 time diff
	 * 6 same-result column
	 * @param ASC
	 * @param isWallclock
	 * @return
	 */
	private static int compareSolverComparisonNums(SolverComparison c1, SolverComparison c2, int sortIndex, boolean ASC, boolean isWallclock) {
		int answer=0;
		try {
			double db1=0;
			double db2=0;
			if (sortIndex==1) {
				if (isWallclock) {
					db1=c1.getFirstPair().getWallclockTime();
					db2=c2.getFirstPair().getWallclockTime();
				} else {
					db1=c1.getFirstPair().getCpuTime();
					db2=c2.getFirstPair().getCpuTime();
				}
			} else if (sortIndex==2) {
				if (isWallclock) {
					db1=c1.getSecondPair().getWallclockTime();
					db2=c2.getSecondPair().getWallclockTime();
				} else {
					db1=c1.getSecondPair().getCpuTime();
					db2=c2.getSecondPair().getCpuTime();
				}
			} else if (sortIndex==3) {
				if (isWallclock) {
					db1=c1.getWallclockDifference();
					db2=c2.getWallclockDifference();
				} else {
					db1=c1.getCpuDifference();
					db2=c2.getCpuDifference();
				}
			} else {
				if (c1.doResultsMatch()) {
					db1=1;
				}
				if (c2.doResultsMatch()) {
					db2=1;
				}
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
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param sortIndex the value to sort on
	 * 0 = bench name
	 * 4 = result 1
	 * 5 = result 2
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */ 
	private static int compareSolverComparisonStrings(SolverComparison c1, SolverComparison c2, int sortIndex, boolean ASC) {
		int answer=0;
		try {
			String str1=null;
			String str2=null;
			 if (sortIndex==5) {
				str1=c1.getSecondPair().getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getSecondPair().getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (sortIndex==0) {
				str1=c1.getBenchmark().getName();
				str2=c2.getBenchmark().getName();
			} else {
				str1=c1.getFirstPair().getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getFirstPair().getAttributes().getProperty(R.STAREXEC_RESULT);
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
	
	protected static List<SolverComparison> mergeSortSolverComparisons(List<SolverComparison> comparisons,int sortColumn,boolean ASC, boolean wallclock) {
		if (comparisons.size()<=1) {
			return comparisons;
		}
		int middle=comparisons.size()/2;
		List<SolverComparison> list1= mergeSortSolverComparisons(comparisons.subList(0, middle),sortColumn,ASC,wallclock);
		List<SolverComparison> list2=mergeSortSolverComparisons(comparisons.subList(middle, comparisons.size()),sortColumn,ASC,wallclock);
		return mergeSolverComparisons(list1,list2,sortColumn,ASC,wallclock);
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
		jp.setMaxMemory(result.getLong("maximum_memory"));
		//log.debug("getting job pair from result set for id " + jp.getId());
		return jp;
	}
	
	
	/**
	 * Sets the status of a given job pair to the given status
	 * @param pairId
	 * @param statusCode
	 * @param con
	 * @return
	 */
	public static boolean setPairStatus(int pairId, int statusCode, Connection con) {
		CallableStatement procedure= null;
		try{
			procedure = con.prepareCall("{CALL UpdatePairStatus(?, ?)}");
			procedure.setInt(1, pairId);
			procedure.setInt(2, statusCode);

			procedure.executeUpdate();								
			
			return true;
		} catch (Exception e) {
			log.debug("setPairStatus says "+e.getMessage(),e);
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
		
		try {
			con = Common.getConnection();
			return setPairStatus(pairId,statusCode,con);
			
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;
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
		jp.setId(result.getInt("job_pairs.id"));
		jp.setWallclockTime(result.getDouble("wallclock"));
		jp.setCpuUsage(result.getDouble("cpu"));
		return jp;
	}

	/**
	 * One-time only function for transferring legacy job pairs from their old file locations
	 * to new file locations
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean transferOutputFilesToNewDirectory() {
		try {
			List<JobPair> pairs = getAllPairsForMovingOutputFiles();
			log.debug("we found this many pairs "+pairs.size()+"\n\n\n\n");
			
			File oldPairFile = null;
			File newFileDir=null;
			String path=null;
			for (JobPair jp : pairs) {
				log.debug("now working on pair "+jp.getId());
				File tempDir=new File(new File(R.NEW_JOB_OUTPUT_DIR),String.valueOf(jp.getJobId()));

				oldPairFile=new File(String.format("%s/%d/%d/%s___%s/%s", R.JOB_OUTPUT_DIR, jp.getId(), jp.getJobId(), jp.getSolver().getName(), jp.getConfiguration().getName(), jp.getBench().getName()));
				if (!oldPairFile.exists()) {
					log.debug(oldPairFile.getAbsolutePath());
					continue;
				}
				log.debug("Searching for pair output at" + oldPairFile.getAbsolutePath());
				
				path=jp.getPath();
				//if the pair has no  path for some reason, just assign a generic one
				if (path==null) {
					path="job space";
				}
				
				String [] spaces=path.split("/");
				newFileDir=new File(tempDir,spaces[0]);
				newFileDir.mkdir();
				for (int index=1;index<spaces.length;index++) {
					newFileDir=new File(newFileDir,spaces[index]);
				}
				newFileDir=new File(newFileDir,jp.getSolver().getName());
				newFileDir.mkdirs();
				newFileDir=new File(newFileDir,jp.getConfiguration().getName());
				newFileDir.mkdirs();				
				FileUtils.copyFileToDirectory(oldPairFile,newFileDir);

			}
			
			return true;
		} catch (Exception e) {
			log.debug("transferOutputFilesToNewDirectory says "+e.getMessage(),e);
		}
		return false;
		
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
			log.error("updateJobSpaces says "+e.getMessage(),e);
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
			procedure.setDouble(7, pair.getCpuTime());
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
