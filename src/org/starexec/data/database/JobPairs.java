package org.starexec.data.database;

import java.io.File;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobSpace;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverComparison;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PairStageProcessorTriple;
import org.starexec.util.LogUtil;
import org.starexec.util.Util;

/**
 * Contains handles on database queries for retrieving and updating
 * job pairs.
 */
public class JobPairs {
	private static final Logger log = Logger.getLogger(JobPairs.class);
	private static final LogUtil logUtil = new LogUtil( log );
	

	private static boolean addJobPairInputs(List<JobPair> pairs, Connection con) {
		final String methodName = "addJobPairInputs";
		CallableStatement procedure=null;
		int batchCounter = 0;
		int totalPairsSubmitted = 0;
		try {
			procedure=con.prepareCall("{CALL AddJobPairInput(?,?,?)}");

			for (JobPair pair : pairs) {
				for (int i=0;i<pair.getBenchInputs().size();i++) {

					procedure.setInt(1, pair.getId());
					procedure.setInt(2,i+1);
					procedure.setInt(3, pair.getBenchInputs().get(i));
					
					procedure.addBatch();
					batchCounter++;
					final int batchSize = 1000;
					if (batchCounter > batchSize) {
						totalPairsSubmitted += batchSize;
						logUtil.debug( methodName, "Submitting batch of "+ batchSize+", total pairs submitted: "+totalPairsSubmitted);
						procedure.executeBatch();
						batchCounter = 0;
					}
				}
				
			}
			if (batchCounter>0) {
				totalPairsSubmitted += batchCounter;
				logUtil.debug( methodName, "Submitting batch of "+ batchCounter+", total pairs submitted: "+totalPairsSubmitted);
				procedure.executeBatch();
			}			

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}	

	/**
	 * Retrieves all the inputs to the given pair from the jobpair_inputs table.
	 * Inputs will be ordered by their input numbers (in other words, first input, second input, and so on)
	 * @param pairId
	 * @param con An open database connection to make calls on
	 * @return A list of strings pointing to the inputs for this pair, or null on error.
	 */
	public static List<String> getJobPairInputPaths(int pairId, Connection con) {
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			procedure=con.prepareCall("CALL GetJobPairInputPaths(?)");
			procedure.setInt(1,pairId);
			results=procedure.executeQuery();
			List<String> benchmarkPaths=new ArrayList<String>();
			while (results.next()) {
				benchmarkPaths.add(results.getString("path"));
			}
			return benchmarkPaths;
			
		}catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	
	/**
	 * Adds all the jobline stages for all of the given pairs to the database
	 * @param pairs The pairs to add the stages of
	 * @param con The open connection to make the call on
	 * @return
	 */
	private static boolean addJobPairStages(List<JobPair> pairs, Connection con) {
		final String methodName = "addJobPairStages";
		CallableStatement procedure=null;
		int totalPairsSubmitted = 0;
		try {
			int batchCounter = 0;
			procedure=con.prepareCall("{CALL AddJobPairStage(?,?,?,?,?,?,?,?,?)}");
			
			for (JobPair pair : pairs) {
				for (JoblineStage stage : pair.getStages()) {
					if (stage.isNoOp()) {
						continue;
					}
					
					procedure.setInt(1, pair.getId());
					if (stage.getStageId()!=null) {
						procedure.setInt(2,stage.getStageId());

					} else {
						procedure.setNull(2, java.sql.Types.INTEGER);
					}
					procedure.setInt(3,stage.getStageNumber());
					procedure.setBoolean(4, pair.getPrimaryStageNumber()==stage.getStageNumber());
					procedure.setInt(5, stage.getSolver().getId());
					procedure.setString(6,stage.getSolver().getName());
					procedure.setInt(7,stage.getConfiguration().getId());
					procedure.setString(8,stage.getConfiguration().getName());
					procedure.setInt(9,pair.getJobSpaceId());
					// Update the pair's ID so it can be used outside this method
					procedure.addBatch();
					
					batchCounter++;
					final int batchSize = 1000;
					if (batchCounter > batchSize) {
						totalPairsSubmitted += batchSize;
						logUtil.debug( methodName, "Submitting batch of " + batchSize +", total pairs submitted: "+totalPairsSubmitted );
						procedure.executeBatch();
						batchCounter = 0;
					}
				}
				
			}
			if (batchCounter > 0) {
				totalPairsSubmitted += batchCounter;
				logUtil.debug( methodName, "Submitting batch of " + batchCounter +", total pairs submitted: "+totalPairsSubmitted );
				procedure.executeBatch();
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
		
	}

	public static boolean addJobPairs( int jobId, List<JobPair> pairs ) throws SQLException {
		final String methodName = "addJobPairs( int, List<JobPair> )";

		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction( con );
			log.debug("addJobPairs one");
			boolean success = incrementTotalJobPairsForJob(jobId, pairs.size(),con);
			if (!success) {
				return false;
			}
			log.debug("addJobPairs two");
			success = addJobPairs( con, jobId, pairs );
			log.debug("addJobPairs three");
			return success;
		} catch ( SQLException e ) {
			logUtil.error( methodName, "SQLException thrown: " + Util.getStackTrace( e ) );
			Common.doRollback( con );
			throw e;
		} finally  {
			Common.endTransaction( con );
			Common.safeClose( con );
		}


	}
	
	/**
	 * Adds a job pair record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @return True if the operation was successful
	 */
	protected static boolean addJobPairs(Connection con, int jobId, List<JobPair> pairs) throws SQLException {
		final String methodName = "addJobPairs";
		logUtil.entry( methodName );
		CallableStatement procedure = null;
		 try {
			procedure = con.prepareCall("{CALL AddJobPair(?, ?, ?, ?, ?, ?, ?, ?)}");
			int pairsProcessed = 0;
			for (JobPair pair : pairs) {
				pair.setJobId(jobId);
				procedure.setInt(1, jobId);
				procedure.setInt(2, pair.getBench().getId());
				procedure.setInt(3, StatusCode.STATUS_PENDING_SUBMIT.getVal());
				
				procedure.setString(4, pair.getPath());
				procedure.setInt(5,pair.getJobSpaceId());
				
				procedure.setString(6,pair.getBench().getName());
				// The procedure will return the pair's new ID in this parameter
				procedure.setInt(7,pair.getPrimaryStageNumber());
				procedure.registerOutParameter(8, java.sql.Types.INTEGER);	
				procedure.executeUpdate();			
				
				// Update the pair's ID so it can be used outside this method
				int newPairId = procedure.getInt(8);
				pair.setId(newPairId);

				pairsProcessed+=1;
				if (pairsProcessed % 1000 == 0) {
					logUtil.debug(methodName, "Pairs Processed: "+pairsProcessed);
				}
			}
			logUtil.debug(methodName, "Pairs Processed: "+pairsProcessed);

			logUtil.debug( methodName, "Adding job pair stages." );
			addJobPairStages(pairs,con);
			logUtil.debug( methodName, "Adding job pair inputs." );
			addJobPairInputs(pairs,con);
			return true;
		} catch (Exception e) {
			log.error("addJobPair says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			
		}
		return false;
	}
	
	 

    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param pairId The pair to get output for
     * @param stageNumber The stage to get pair info for
     * @param limit The maximum number of lines to return
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(int pairId,int stageNumber,int limit) {		
    	String stdoutPath= JobPairs.getStdout(pairId,stageNumber);
    	
    	return Util.readFileLimited(new File(stdoutPath), limit);
    }

    

	/**
	 * Returns all pairs that are waiting on post processing. Returns a hashmap mapping
	 * job pair IDs to post processors
	 * @return A list of triples containing pair id, stage number, post processor id that
	 * represents all stages that need to be processed.
	 */
	public static List<PairStageProcessorTriple> getAllPairsForProcessing() {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetPairsToBeProcessed(?)}");
			procedure.setInt(1,StatusCode.STATUS_PROCESSING.getVal());
			results=procedure.executeQuery();
			List<PairStageProcessorTriple> list=new ArrayList<PairStageProcessorTriple>();
			while (results.next()) {
				PairStageProcessorTriple next= new PairStageProcessorTriple();
				next.setPairId(results.getInt("job_pairs.id"));
				next.setStageNumber(results.getInt("stageNumber"));
				next.setProcessorId(results.getInt("post_processor"));
				list.add(next);
			}
			return list;
			
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
	 * Updates the total_pairs column for the given job by summing it with the given increment
	 * @param jobId The ID of the job to update
	 * @param increment The amount to change total_pairs by. Note that if this is negative it
	 * means the total_pairs column will decrease
	 * @param con The open connection to make the call on
	 * @return true on success and false otherwise
	 */
	public static boolean incrementTotalJobPairsForJob(int jobId, int increment, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL IncrementTotalJobPairsForJob(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, increment);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Deletes a list of job pairs. All pairs are expected to belong to the same job
	 * @param jobPairs the job pairs to delete.
	 * @author Albert Giegerich
	 */
	public static void deleteJobPairs(List<JobPair> jobPairs ) throws SQLException {
		final String methodName = "deleteJobPairs";
		Connection con = null;
		
		try { 
			con = Common.getConnection();
			Common.beginTransaction( con );
			log.debug("beginning to delete pairs");
			for ( JobPair pair : jobPairs ) {
				deleteJobPair( con, pair );
				log.debug("pair deleted");
			}
		} catch ( SQLException e ) {
			logUtil.debug( methodName, "Caught an SQLException, database failed." );
			Common.doRollback( con );
			throw e;
		} finally {
			Common.endTransaction( con );
			Common.safeClose( con );
		}
	}

	// Deletes a given JobPair
	private static void deleteJobPair( Connection con, JobPair pairToDelete ) throws SQLException {
		if ( pairToDelete == null ) {
			throw new NullPointerException("Input JobPair was null.");
		}

		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall( "{CALL DeleteJobPair(?)}" );
			procedure.setInt( 1, pairToDelete.getId() );
			procedure.executeQuery();
		} catch ( SQLException e ) {
			throw e;
		} finally {
			Common.safeClose( procedure );
		}
	}
	
	/**
	 * Post processes the given pair with the given processor ID,
	 * add the properties to the pair attributes table, and removes
	 * the pair from the processing job pairs table
	 * @param pairId The ID of the pair to process
	 * @param stageNumber
	 * @param processorId The ID of the processor to use
	 * @return True on success, false on error
	 */
	public static boolean postProcessPair(int pairId,int stageNumber, int processorId) {
		Connection con=null;
		try {
			Properties props=runPostProcessorOnPair(pairId,stageNumber,processorId);
			con=Common.getConnection();
			Common.beginTransaction(con);
			JobPairs.addJobPairAttributes(pairId,stageNumber, props,con);
			JobPairs.setPairStatus(pairId, StatusCode.STATUS_COMPLETE.getVal(),con);
			JobPairs.setPairStageStatus(pairId, StatusCode.STATUS_COMPLETE.getVal(), stageNumber, con);
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
	 * Runs the given post processor on the given pair stage and returns the properties that were obtained
	 * @param pairId The ID of the pair in question
	 * @param processorId The ID of the processor in question
	 * @return The properties on success, or null otherwise
	 */
	private static Properties runPostProcessorOnPair(int pairId, int stageNumber, int processorId) {
		try {
			JobPair pair=JobPairs.getPairDetailed(pairId);
			File output=new File(JobPairs.getFilePath(pair,stageNumber));
			Processor p=Processors.get(processorId);
			// Run the processor on the benchmark file
			List<File> files=new ArrayList<File>();
			files.add(new File(p.getFilePath()));
			files.add(new File(pair.getBench().getPath()));
			files.add(output);
			File sandbox=Util.copyFilesToNewSandbox(files);
			String benchPath=new File(sandbox,new File(pair.getBench().getPath()).getName()).getAbsolutePath();
			String outputPath=new File(sandbox,output.getName()).getAbsolutePath();
			File working=new File(sandbox,new File(p.getFilePath()).getName());
			
			
			
			String [] procCmd = new String[3];
			procCmd[0] = "./"+R.PROCESSOR_RUN_SCRIPT; 
			
			procCmd[1] = outputPath;
			
			procCmd[2] = benchPath;
			String propstr = Util.executeSandboxCommand(procCmd, null, working);
			FileUtils.deleteQuietly(sandbox);

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
	protected static boolean addJobPairAttr(Connection con, int pairId,int stageId, String key, String val) throws Exception {
		CallableStatement procedure = null;
		 try {
			procedure = con.prepareCall("{CALL AddJobAttr(?, ?, ?,?)}");
			procedure.setInt(1, pairId);
			
			procedure.setString(2, key);
			procedure.setString(3, val);
			procedure.setInt(4,stageId);
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
	 * @param stageId The ID of the stage to add attributes for.
	 * @param attributes The key/value attributes
	 * @param con The open connection to make the call on
	 * @return True on success, false on error
	 */
	public static boolean addJobPairAttributes(int pairId,int stageId, Properties attributes, Connection con) {
		try {
			// For each attribute (key, value)...
			log.info("Adding " + attributes.entrySet().size() +" attributes to job pair " + pairId);
			for(Entry<Object, Object> keyVal : attributes.entrySet()) {
				// Add the attribute to the database
				JobPairs.addJobPairAttr(con, pairId,stageId, (String)keyVal.getKey(), (String)keyVal.getValue());
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
	 * @param stageId the ID of the stage to add attributes to
	 * @param attributes The attributes to add to the job pair
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean addJobPairAttributes(int pairId,int stageId, Properties attributes) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return addJobPairAttributes(pairId,stageId,attributes,con);
		} catch(Exception e) {			
			log.error("error adding Job Attributes = " + e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;		
	}


	/**
	 * Filters job pairs based on their status codes
	 * @param pairs
	 * @param type
	 * @return
	 */
	protected static List<JobPair> filterPairsByType(List<JobPair> pairs, String type, int stageNumber) {

		log.debug("filtering pairs by type with type = "+type);
		List<JobPair> filteredPairs=new ArrayList<JobPair>();
		
		if (type.equals("incomplete")) {
			for (JobPair jp : pairs) {
				if (jp.getStageFromNumber(stageNumber).getStatus().getCode().statIncomplete()) {
					filteredPairs.add(jp);
				}
			}
		} else if (type.equals("resource")) {
			for (JobPair jp : pairs) {
				if (jp.getStageFromNumber(stageNumber).getStatus().getCode().resource()) {
					filteredPairs.add(jp);
				}
			}
		}else if (type.equals("failed")) {
			for (JobPair jp : pairs) {
				if (jp.getStageFromNumber(stageNumber).getStatus().getCode().failed()) {
					filteredPairs.add(jp);
				}
			}
		} 	else if (type.equals("solved")) {

			for (JobPair jp : pairs) {
				JoblineStage stage=jp.getStageFromNumber(stageNumber);

				if (JobPairs.isPairCorrect(stage)==0) {
					filteredPairs.add(jp);
				}
			}
		}  else if (type.equals("wrong")) {
			for (JobPair jp : pairs) {
				JoblineStage stage=jp.getStageFromNumber(stageNumber);
				if (JobPairs.isPairCorrect(stage)==1) {
					filteredPairs.add(jp);
				}
			}
		}  else if (type.equals("unknown")) {
			for (JobPair jp : pairs) {
				JoblineStage stage=jp.getStageFromNumber(stageNumber);

				if (JobPairs.isPairCorrect(stage)==2) {
					filteredPairs.add(jp);
				}
			}
		} else if (type.equals("complete")) {
			for (JobPair jp : pairs) {
				if (jp.getStageFromNumber(stageNumber).getStatus().getCode().complete()) {
					filteredPairs.add(jp);
				}
			}
		} else {
			filteredPairs=pairs;
		}
		return filteredPairs;
	}
	/**
	 * Checks whether a given stage is correct
	 * @param stage
	 * @return
	 * -1 == pair is not complete (as in, does not have STATUS_COMPLETE)
	 * 0 == pair is correct
	 * 1 == pair is incorrect
	 * 2 == pair is unknown
	 */
	public static int isPairCorrect(JoblineStage stage) {
	    StatusCode statusCode=stage.getStatus().getCode();

	    if (statusCode.getVal()==StatusCode.STATUS_COMPLETE.getVal()) {
			if (stage.getAttributes()!=null) {
				Properties attrs = stage.getAttributes();
				//log.debug("expected = "+attrs.get(R.EXPECTED_RESULT));
				//log.debug("actual = "+attrs.get(R.STAREXEC_RESULT));
				if (attrs.containsKey(R.STAREXEC_RESULT) && attrs.get(R.STAREXEC_RESULT).equals(R.STAREXEC_UNKNOWN)) {
					//don't know the result, so don't mark as correct or incorrect.	
					return 2;
				} else if (attrs.containsKey(R.EXPECTED_RESULT) && !attrs.get(R.EXPECTED_RESULT).equals(R.STAREXEC_UNKNOWN)) {
					if (!attrs.containsKey(R.STAREXEC_RESULT) || !attrs.get(R.STAREXEC_RESULT).equals(attrs.get(R.EXPECTED_RESULT))) {
						//the absence of a result, or a nonmatching result, is counted as wrong
						return 1;
					} else { 
						return 0;
					}
				} else { 
				//if the attributes don't have an expected result, we will mark as unknown
				return 2;
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
				log.error(e.getMessage(),e);
			}	
		}
		
		return filteredComparisons;
	}
	
	/**
	 * Filters a list of job pairs against some search query. The query is compared to 
	 * solver, benchmark, and config names, as well as integer status code and result. The job pair is not filtered if the query
	 * is a case-insensitive substring of any of those names
	 * @param pairs The pairs to filter
	 * @param searchQuery The query
	 * @return A filtered list of job pairs
	 * @author Eric burns
	 */
	
	protected static List<JobPair> filterPairs(List<JobPair> pairs, String searchQuery, int stageNumber) {
		//no filtering is necessary if there's no query
		if (searchQuery==null || searchQuery=="") {
			return pairs;
		}
		
		searchQuery=searchQuery.toLowerCase();
		List<JobPair> filteredPairs=new ArrayList<JobPair>();
		for (JobPair jp : pairs) {
			JoblineStage stage=jp.getStageFromNumber(stageNumber);
			try {
				if (jp.getBench().getName().toLowerCase().contains(searchQuery) || String.valueOf(stage.getStatus().getCode().getVal()).equals(searchQuery)
						|| stage.getSolver().getName().toLowerCase().contains(searchQuery) || stage.getConfiguration().getName().toLowerCase().contains(searchQuery) ||
						stage.getStarexecResult().contains(searchQuery)) {
						
					filteredPairs.add(jp);
				}
			} catch (Exception e) {
				log.warn("filterPairs says jp with id = "+jp.getId()+" threw a null pointer");
			}	
		}
		
		return filteredPairs;
	}
	
	/**
	 * Retrieves all attributes (key/value) of the given job pair. Returns a mapping
	 * of those attributes to stages based on the jobpair_stage_data.stage_number
	 * @param con The connection to make the query on
	 * @param pairId The id of the pair to get the attributes of
	 * @return The properties object which holds all the pair's attributes
	 * @author Tyler Jensen
	 */
	protected static HashMap<Integer,Properties> getAttributes(Connection con, int pairId) throws Exception {
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			HashMap<Integer,Properties> props= new HashMap<Integer,Properties>();
			 procedure = con.prepareCall("{CALL GetPairAttrs(?)}");
			procedure.setInt(1, pairId);					
			 results = procedure.executeQuery();


			while(results.next()){
				int joblineStageNumber = results.getInt("stage_number");
				if (!props.containsKey(joblineStageNumber)) {
					props.put(joblineStageNumber, new Properties());
				}
				props.get(joblineStageNumber).put(results.getString("attr_key"), results.getString("attr_value"));				
			}			

			
			return props;
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
	public static HashMap<Integer,Properties> getAttributes(int pairId) {
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
		return getLogFilePath(getFilePathInfo(pairId));
	}
	
	/**
	 * Populates a job pair with just enough information to find the file path.
	 * The pair will be returned with a single primary stage set with a solver name and config name
	 * @param pairId
	 * @return
	 */
	private static JobPair getFilePathInfo(int pairId) {
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
				
				pair.addStage(new JoblineStage());
				
				pair.setPrimaryStageNumber(results.getInt("stage_number"));
				pair.getStages().get(0).setStageNumber(pair.getPrimaryStageNumber());
				Solver s= pair.getPrimarySolver();
				s.setName(results.getString("solver_name"));
				Benchmark b=pair.getBench();
				b.setName(results.getString("bench_name"));
				Configuration c=pair.getPrimaryConfiguration();
				c.setName(results.getString("config_name"));
				pair.setJobId(results.getInt("job_id"));
				pair.setPath(results.getString("path"));
				pair.setJobSpaceId(results.getInt("job_space_id"));
				
				pair.setId(pairId);
				return pair;
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
	 * Gets the path to the directory containing all output files for this job.
	 * For jobs created before solver pipelines, returns the single output file for the job
	 * @param pairId The id of the pair to get the filepath for
	 * @return The string path, or null on failure
	 * @author Eric Burns
	 */
	
	public static String getStdout(int pairId) {
		return getPairStdout(getFilePathInfo(pairId));
	}
	
	/**
	 * Returns a list of files representing paths to both a pair's
	 * standard output and additional output directories. If the given
	 * file is a directory, it is returned alone. Otherwise, it is interpreted
	 * as the single stdout file and the additional directory is returned as well
	 * if it exists.
	 * @param pairId
	 * @param stdout
	 * @return
	 */
	private static List<File> getOutputPathsFromStdout(int pairId, File stdout) {
		List<File> files = new ArrayList<File>();
		files.add(stdout);
		// if we need the other directory
		if (!stdout.isDirectory()) {
			File otherOutput = new File(stdout.getParentFile(), pairId+"_output");
			if (otherOutput.exists()) {
				files.add(otherOutput);
			}
		}
		return files;
	}
	
	/**
	 * Returns a list of files representing paths to both a pair's
	 * standard output and additional output directories. If these two
	 * things are contained in a single top level directory, as they are
	 * when joblines are used, only that directory is returned. No extra
	 * output dir is returned if extra outputs are not used.
	 * @param pairId The pair to get output for
	 * @return Paths to all output for this job.
	 * has no output yet
	 */
	public static List<File> getOutputPaths(int pairId) {
		File stdout = new File(getStdout(pairId));
		return getOutputPathsFromStdout(pairId, stdout);
	}
	/**
	 * Same as getOutputPaths(pairId), except the given pair is expected
	 * to have all relevant fields populated and will not need
	 * to be retrieved from the database
	 * @param pair
	 * @return See getOutputPaths(pairId)
	 */
	public static List<File> getOutputPaths(JobPair pair) {
		File stdout = new File(getPairStdout(pair));
		return getOutputPathsFromStdout(pair.getId(), stdout);
	}
	
	/**
	 * Gets the path to the output file for the given job pair and stage
	 * @param pairId
	 * @param stageNumber
	 * @return The absolute file path to the output for the given stage of the given pair
	 */
	
	public static String getStdout(int pairId, int stageNumber) {
		return getFilePath(getFilePathInfo(pairId),stageNumber);
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
	 * @return The absolute path to the log file for the given pair, or null if it could not 
	 * be found
	 */
	public static String getLogFilePath(JobPair pair) {
		try {
			
			File file=new File(Jobs.getLogDirectory(pair.getJobId()));
			file = new File(file,String.valueOf(pair.getJobSpaceId()));
			file = new File(file,pair.getId()+".txt");
			log.debug("found this log path "+file.getAbsolutePath());
			return file.getAbsolutePath();
		} catch(Exception e) {
			log.error("getFilePath says "+e.getMessage(),e);
		}
		return null;
		
	}
	
	/**
	 * Retrieves the output of a single stage of the given job pair. Requires that the
	 * jobId, path, solver name, config name, and bench names of the PRIMARY STAGE be populated.
	 * The fields do NOT need to be populated for given stage, ONLY the primary stage
	 * @param pair
	 * @param stageNumber A number >=1 representing the stage of this pair
	 * @return The absolute file path to the output file for the given stage of the given pair
	 */
	public static String getFilePath(JobPair pair, int stageNumber) {
		String path=getPairStdout(pair); //this is the path to the top level directory of the pair
		
		File f=new File(path);
		if (f.isDirectory()) {
			//means this is a job created after stages were implemented
			return new File(f,stageNumber+".txt").getAbsolutePath();
			
			
			
		} else {
			//if we get down here, it means that this pair did NOT use stages.
			return path; 
		}
	}
	
	/**
	 * Gets the path to the directory that contains all the output files for every stage in this pair.
	 * For old pairs that do not have stages, simply returns the path to the single output file for this pair.
	 * Requires that the  jobId, path, solver name, config name, and bench names be populated for the PRIMARY STAGES
	 * @param pair The pair to get the filepath for
	 * @return The string path, or null on failure
	 * @author Eric Burns
	 */
	
	//Note that this function tries several things due to supporting several layers of backwards compatibility
	public static String getPairStdout(JobPair pair) {
		try {
			File file=new File(Jobs.getDirectory(pair.getJobId()));
			String[] pathSpaces=pair.getPath().split("/");
			for (String space : pathSpaces) {
				file=new File(file,space);
			}

			file=new File(file,pair.getPrimarySolver().getName()+"___"+pair.getPrimaryConfiguration().getName());

			file=new File(file,pair.getBench().getName());
			
			
			if (!file.exists()) {	    // if the job output could not be found
				File testFile=new File(file,pair.getPrimarySolver().getName());
				testFile=new File(testFile,pair.getPrimaryConfiguration().getName());
				testFile=new File(testFile,pair.getBench().getName());
				if (testFile.exists()) {  //check the alternate path some pairs are still stored at
					FileUtils.copyFile(testFile, file);
					if (file.exists()) {
						testFile.delete();
					}
				}
			} else if (file.isFile()) { // if it is a file, we have already got the full path
				return file.getAbsolutePath();
			}
			
			//before solver pipelines, pairs were stored as a single file titled <pairid>.txt . If that file exists, returns it
			File testFile=new File(file,pair.getId()+".txt");
			
			if (testFile.exists()) {
				return testFile.getAbsolutePath();
			}
			
			//otherwise, this is a modern job, and we return a directory with the name of the pair id
			
			file=new File(file,String.valueOf(pair.getId()));

			return file.getAbsolutePath();
		} catch(Exception e) {
			log.error("getFilePath says "+e.getMessage(),e);
		}
		return null;
		
	}
	
	/**
	 * Gets the job pair with the given id non-recursively 
	 * (Worker node, status, benchmark and solver will NOT be populated).
	 * Only the primary stage is created! To get all the stages, you need to call getPairDetailed
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
				jp.addStage(new JoblineStage()); // just add an empty stage that we can populate below
				jp.getStages().get(0).setStageNumber(jp.getPrimaryStageNumber());

				jp.getNode().setId(results.getInt("node_id"));
				jp.getStatus().setCode(results.getInt("status_code"));
				jp.getBench().setId(results.getInt("bench_id"));
				jp.getBench().setName(results.getString("bench_name"));
				jp.getPrimarySolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
				jp.getPrimarySolver().setId(results.getInt("solver_id"));
				jp.getPrimarySolver().setName(results.getString("solver_name"));
				jp.getPrimarySolver().getConfigurations().get(0).setName(results.getString("config_name"));
				jp.getStages().get(0).setConfiguration(jp.getPrimarySolver().getConfigurations().get(0));
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
			
			JobPair jp=null;
			// first, we get the top level info from the job_pairs table
			if(results.next()){
				jp = JobPairs.resultToPair(results);
				jp.setCompletionId(results.getInt("completion_id"));
				jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));
				jp.setBench(Benchmarks.get(con, results.getInt("bench_id"),true));
				
				Status s = new Status();
				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);					
				jp.setJobSpaceName(results.getString("jobSpace.name"));
				
			} else {
				//couldn't find the pair for some reason
				return null;
			}

            populateJobPairStagesDetailed(jp, con);

			return jp;
		} catch (Exception e) {
			log.error("Get JobPair says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return null;		
	}

	private static void populateJobPairStagesDetailed(JobPair jp, Connection con) throws SQLException {
        Common.<Void>queryUsingConnection("{CALL GetJobPairStagesById(?)}", con, procedure -> {
                procedure.setInt(1, jp.getId());
            }, results -> {
            //next, we get data at the stage level
            while (results.next()) {
                JoblineStage stage = resultToStage(results);
                int configId = results.getInt("config_id");
                int solverId = results.getInt("solver_id");
                String configName = results.getString("config_name");
                String solverName = results.getString("solver_name");
                //means this stage has no configuration
                if (configId == -1) {
                    stage.setNoOp(true);
                } else if (configId > 0) {
                    Solver solver = Solvers.getSolverByConfig(con, configId, true);
                    Configuration c = Solvers.getConfiguration(configId);

                    //this can happen if the pair references a deleted solver
                    if (solver == null) {
                        solver = new Solver();
                        solver.setId(solverId);
                        solver.setName(solverName);
                    }

                    if (c == null) {
                        c = new Configuration();
                        c.setId(configId);
                        c.setName(configName);
                    }

                    stage.setSolver(solver);
                    stage.setConfiguration(c);
                    stage.getSolver().addConfiguration(c);
                }
                jp.addStage(stage);
            }
            //last, we get attributes for everything
            HashMap<Integer, Properties> attrs = getAttributes(jp.getId());
            for (JoblineStage stage : jp.getStages()) {
                if (attrs.containsKey(stage.getStageNumber())) {
                    stage.setAttributes(attrs.get(stage.getStageNumber()));
                }
            }
            return null;
        });
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

	public static List<JobPair> getPairsInJobContainingBenchmark(int jobId, int benchmarkId) throws SQLException {

		return Common.queryKeepConnection("{CALL GetJobPairsInJobContainingBenchmark(?, ?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setInt(2, benchmarkId);
		}, (con, results) -> {
			List<JobPair> jobPairs = new ArrayList<>();
			while (results.next()) {
				JobPair pair = resultToPair(results);
				populateJobPairStagesDetailed(pair, con);
				jobPairs.add(pair);
			}
			return jobPairs;
		});
	}

	// TODO implement method.
	public static List<JobPair> getPairsInJobContainingSolver(int jobId, int solverId) throws SQLException {
		return Common.queryKeepConnection("{CALL GetJobPairsInJobContainingSolver(?, ?)}", procedure -> {
		    procedure.setInt(1, jobId);
            procedure.setInt(2, solverId);
        }, (con, results) -> {
            List<JobPair> jobPairs = new ArrayList<>();
            while (results.next()) {
                JobPair pairFromResults = resultToPair(results);
                populateJobPairStagesDetailed(pairFromResults, con);
                jobPairs.add(pairFromResults);
            }
            return jobPairs;
        });
	}
	
	/**
	 * Extracts query informaiton into a JoblineStage. Does NOT get deep information like
	 * solver and configuration
	 * @param result
	 * @return
	 * @throws Exception
	 */
	protected static JoblineStage resultToStage(ResultSet result) throws SQLException {
		JoblineStage stage=new JoblineStage();
				
		stage.setStageNumber(result.getInt("jobpair_stage_data.stage_number"));
		stage.setWallclockTime(result.getDouble("jobpair_stage_data.wallclock"));
		stage.setCpuUsage(result.getDouble("jobpair_stage_data.cpu"));
		stage.setUserTime(result.getDouble("jobpair_stage_data.user_time"));
		stage.setSystemTime(result.getDouble("jobpair_stage_data.system_time"));		
		stage.setMaxVirtualMemory(result.getDouble("jobpair_stage_data.max_vmem"));
		stage.setMaxResidenceSetSize(result.getDouble("jobpair_stage_data.max_res_set"));
		stage.setStageId(result.getInt("jobpair_stage_data.stage_id"));
		stage.getStatus().setCode(result.getInt("jobpair_stage_data.status_code"));
		return stage;
	}

	/**
	 * Helper method to extract information from a query for job pairs
	 * @param result The resultset that is the results from querying for job pairs
	 * @return A job pair object populated with data from the result set
	 */
	protected static JobPair resultToPair(ResultSet result) throws SQLException {

		JobPair jp = new JobPair();

		jp.setId(result.getInt("job_pairs.id"));
		jp.setJobId(result.getInt("job_pairs.job_id"));
		jp.setBackendExecId(result.getInt("job_pairs.sge_id"));	
		jp.setQueueSubmitTime(result.getTimestamp("job_pairs.queuesub_time"));
		jp.setStartTime(result.getTimestamp("job_pairs.start_time"));
		jp.setEndTime(result.getTimestamp("job_pairs.end_time"));
		// Populate basic benchmark info.
		jp.getBench().setId(result.getInt("bench_id"));
		jp.getBench().setName(result.getString("bench_name"));

		jp.getNode().setId(result.getInt("job_pairs.node_id"));
		jp.getStatus().setCode(result.getInt("job_pairs.status_code"));
		
		jp.setPath(result.getString("job_pairs.path"));
		jp.setJobSpaceId(result.getInt("job_pairs.job_space_id"));
		jp.setPrimaryStageNumber(result.getInt("job_pairs.primary_jobpair_data"));
		jp.setSandboxNum(result.getInt("job_pairs.sandbox_num"));
		//log.debug("getting job pair from result set for id " + jp.getId());
		return jp;
	}
	
	/**
	 * Sets the status of a given job pair stage to the given status
	 * @param pairId The ID of the pair to update
	 * @param stageNumber The number of the stage to update
	 * @param statusCode The code to give the stage
	 * @param con An open database connection to make the call on
	 * @return True on success and false on error
	 */
	public static boolean setPairStageStatus(int pairId, int statusCode, int stageNumber, Connection con) {
		CallableStatement procedure= null;
		try{
			procedure = con.prepareCall("{CALL UpdatePairStageStatus(?, ?,?)}");
			procedure.setInt(1, pairId);
			procedure.setInt(2,stageNumber);
			procedure.setInt(3, statusCode);

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
	 * Sets the status code of every stage that comes after the given stage to the given value
	 * @param pairId
	 * @param statusCode
	 * @param stageNumber
	 * @param con
	 * @return True on success and false otherwise
	 */
	public static boolean setLaterPairStageStatus(int pairId, int statusCode, int stageNumber, Connection con) {
		CallableStatement procedure= null;
		try{
			procedure = con.prepareCall("{CALL UpdateLaterStageStatuses(?, ?,?)}");
			procedure.setInt(1, pairId);
			procedure.setInt(2,stageNumber);
			procedure.setInt(3, statusCode);

			procedure.executeUpdate();								
			
			return true;
		} catch (Exception e) {
			log.debug(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}	
		return false;
	}
	
	/**
	 * Sets the status code of every stage that comes after the given stage to the given value
	 * @param pairId
	 * @param statusCode
	 * @param stageNumber
	 * @return True on success and false on error
	 */
	
	public static boolean setLaterPairStageStatus(int pairId, int statusCode, int stageNumber) {
		Connection con=null;
		try {
			con=Common.getConnection();
			
			return setLaterPairStageStatus(pairId,statusCode,stageNumber,con);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	/**
	 * Sets the status code of every stage for the given pair to the given code
	 * @param pairId
	 * @param statusCode
	 * @return True on success and false otherwise
	 */
	public static boolean setAllPairStageStatus(int pairId, int statusCode) {
		return setLaterPairStageStatus(pairId,statusCode,-1);
	}
	
	/**
	 * Assigns a given status code to a job pair and all of its stages
	 * @param pairId
	 * @param statusCode
	 * @return True on success and false otherwise
	 */
	public static boolean setStatusForPairAndStages(int pairId, int statusCode) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return setPairStatus(pairId, statusCode, con) && setAllPairStageStatus(pairId, statusCode, con);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	/**
	 * Sets the status code of every stage for the given pair to the given code
	 * @param pairId
	 * @param statusCode
	 * @param con An open connection to make calls on
	 * @return True on success and false on error
	 */
	public static boolean setAllPairStageStatus(int pairId, int statusCode, Connection con) {
		return setLaterPairStageStatus(pairId,statusCode,-1,con);
	}
	
	/**
	 * Sets the disk_size for the given job pair to 0, updating the jobpair_stage_data,
	 * jobs, and users tables
	 * @param jobPairId
	 * @return True on success and false otherwise
	 */
	public static boolean setJobPairDiskSizeToZero(int jobPairId) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL RemoveJobPairDiskSize(?)}");
			procedure.setInt(1, jobPairId);
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
	 * Sets the status of a given job pair to the given status
	 * @param pairId
	 * @param statusCode
	 * @param con
	 * @return True on success and false on error
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
	
	
	/** Updates the status code for a given stage of a specific pair.
	 * @param pairId the id of the pair to update the status of
	 * @param stageNumber The stage to update
	 * @param statusCode the status code to set for the pair
	 * @return True if the operation was a success, false otherwise
	 */
	public static boolean setPairStatus(int pairId,int stageNumber, int statusCode) {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			return setPairStageStatus(pairId,statusCode,stageNumber,con);
			
		} catch(Exception e) {			
			log.error(e.getMessage(), e);
		} finally {			
			Common.safeClose(con);	
		}

		return false;
	}
	
	/** Updates the status_code for a pair. If the pair is being set to enqueued,
	 * also sets the pair's queue_sub_time. If the pair is being set to
	 * a completed status, the pair's completion entry is updated.
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
	 * Reads all data for a specific queue from the jobpair_time_delta table and then clears
	 * the data from that queue all inside a single transaction.
	 * @param queueID The ID of the queue to get data for. If this is -1, gets and clears
	 * all data
	 * @return A HashMap mapping userIds to their time delta values.
	 */
	
	public static HashMap<Integer, Integer> getAndClearTimeDeltas(int queueID) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			procedure = con.prepareCall("CALL GetJobpairTimeDeltaData(?)");
			procedure.setInt(1, queueID);
			results = procedure.executeQuery();
			HashMap<Integer, Integer> data = new HashMap<Integer, Integer>();
			while (results.next()) {
				data.put(results.getInt("user_id"), results.getInt("time_delta"));
			}
			Common.safeClose(procedure);
			procedure = con.prepareCall("CALL ClearJobpairTimeDeltaData(?)");
			procedure.setInt(1, queueID);
			procedure.executeUpdate();
			
			
			Common.endTransaction(con);
			return data;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Update's a job pair's backend execution ID (SGE, OAR, or so on)
	 * @param pairId The id of the pair to update
	 * @param execId The backend id to set for the pair
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean updateBackendExecId(int pairId, int execId) {
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();									
			procedure = con.prepareCall("{CALL SetBackendExecId(?, ?)}");

			procedure.setInt(1, pairId);
			procedure.setInt(2, execId);			
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
	 * Calls buildJobSpaceIdtoJobPairMapForJob, and then rounds all cpu and wallclock times before returning
	 * results
	 * @param job
	 * @return Identical to buildJobSpaceIdtoJobPairMapForJob, but with rounded times
	 */
	public static Map<Integer, List<JobPair>> buildJobSpaceIdToJobPairMapWithWallCpuTimesRounded(Job job) {
		Map<Integer, List<JobPair>> outputMap = buildJobSpaceIdToJobPairMapForJob(job);
		roundWallclockAndCpuTimesInJobSpaceIdToJobPairMap(outputMap);
		return outputMap;
	}

	private static void roundWallclockAndCpuTimesInJobSpaceIdToJobPairMap(Map<Integer, List<JobPair>> jobSpaceIdToJobPairMap) {
		for (Integer jobSpaceId : jobSpaceIdToJobPairMap.keySet()) {
			List<JobPair> jobPairs = jobSpaceIdToJobPairMap.get(jobSpaceId);
			for (JobPair jp : jobPairs) {
				jp.setPrimaryWallclockTime(Math.round(jp.getPrimaryWallclockTime()*100)/100.0);
				jp.setPrimaryCpuTime(Math.round(jp.getPrimaryCpuTime()*100)/100.0);
			}
		}
	}


	/**
	 * Builds a mapping of job space IDs to JobPairs in that JobSpace given the JobSpaces and JobPairs
	 * @param job The job to work on
	 * @return The mapping of job space IDs to in that job space
	 * @author Albert Giegerich
	 */
	public static Map<Integer, List<JobPair>> buildJobSpaceIdToJobPairMapForJob(Job job) {
		int jobId = job.getId();
		int primaryJobSpaceId = job.getPrimarySpace();
		List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob(primaryJobSpaceId, true);
		jobSpaces.add(Spaces.getJobSpace(job.getPrimarySpace()));
		List<JobPair> allJobPairsInJob = Jobs.getDetailed(jobId, 0).getJobPairs();
		Map<Integer, List<JobPair>> jobSpaceIdToJobPairMap = new HashMap<>();
		for (JobSpace js : jobSpaces) {
			List<JobPair> jobPairsAssociatedWithJs = new ArrayList<>();
			for (JobPair jp : allJobPairsInJob) {
				if (jp.getJobSpaceId() == js.getId()) {
					jobPairsAssociatedWithJs.add(jp);
				}
			}
			if (!jobPairsAssociatedWithJs.isEmpty()) {
				jobSpaceIdToJobPairMap.put(js.getId(), jobPairsAssociatedWithJs);
			}
		}
		return jobSpaceIdToJobPairMap;
	}
	
	/**
	 * Given a list of JobPair objects that have their jobSpaceIds set, updates the database
	 * to reflect these new job space ids
	 * @param jobPairs The pairs to update
	 * @param con An open connection to make calls on
	 * @return True on success and false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean updateJobSpaces(List<JobPair> jobPairs,Connection con) {
		
		try {
			for (JobPair jp : jobPairs) {
				UpdateJobSpaces(jp.getId(),jp.getJobSpaceId(),con);
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}	
		return false;
	}

	

    /**
     * Kills the given job pair and updates the status for the pair to
     * @param pairId
     * @param execId
     * @return True on success and false otherwise
     */
    public static boolean killPair(int pairId, int execId) {
	try {	
	    R.BACKEND.killPair(execId);
	    JobPairs.setJobPairDiskSizeToZero(pairId);
	    JobPairs.UpdateStatus(pairId, Status.StatusCode.STATUS_KILLED.getVal());
	    return true;
	} catch (Exception e) {
	    log.error(e.getMessage(),e);
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
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Gets all job pairs in the database that have the given status code.
	 * This should really only be called for rare codes like 2-5, as otherwise
	 * it may read a very large number of pairs and be very slow.
	 * @param statusCode
	 * @return A list of all pairs with the given status code
	 */
	public static List<JobPair> getPairsByStatus(int statusCode) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			List<JobPair> pairs = new ArrayList<JobPair>();
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetJobPairsWithStatus(?)}");
			procedure.setInt(1, statusCode);
			results=procedure.executeQuery();
			if (results.next()) {
				JobPair p = JobPairs.resultToPair(results);
				p.getStatus().setCode(statusCode);
				pairs.add(p);
			}
			return pairs;
		} catch (Exception e) {
			log.error("getPairsByStatus says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	/**
	 * Sets the status code for a given pair and all of its stages to ERROR_SUBMIT_FAIL
	 * if and only if the pair's status code in the database matches the status code
	 * set in the given pair. This check is done to prevent a race condition in which
	 * a pair that is actually not stuck (hence its status code has changed since the
	 * pair was read) but is still set to the error status code.
	 * @param p The JobPair to affect. Must have ID and status code set
	 * @return True on success and false otherwise
	 */
	public static boolean setBrokenPairStatus(JobPair p) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL SetBrokenPairStatus(?, ?, ?)}");
			procedure.setInt(1, p.getId());
			procedure.setInt(2, p.getStatus().getCode().getVal());
			procedure.setInt(3, Status.StatusCode.ERROR_SUBMIT_FAIL.getVal());
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error("getPairsByStatus says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return false;
	}
	
	/**
	 * Gets all job pairs are currently under the jurisdiction of the 
	 * backend. These are the pairs with status codes 2-5.
	 * @return The list of job pairs. Stages will not be populated
	 */
	public static List<JobPair> getPairsInBackend() {
		List<JobPair> pairs = new ArrayList<JobPair>();
		pairs.addAll(getPairsByStatus(Status.StatusCode.STATUS_ENQUEUED.getVal()));
		pairs.addAll(getPairsByStatus(Status.StatusCode.STATUS_RUNNING.getVal()));
		return pairs;
	}
	
	/**
	 * Updates a job pair's node_id in the database.
	 * @param pairId
	 * @param nodeId
	 * @return True on success and false otherwise
	 */
	public static boolean updatePairExecutionHost(int pairId, int nodeId) {
		Connection con=null;
		CallableStatement procedure =null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdatePairNodeId(?,?)}");
			procedure.setInt(1, pairId);
			procedure.setInt(2, nodeId);
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
}
