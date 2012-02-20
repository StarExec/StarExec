package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Job;

import com.mysql.jdbc.ResultSetMetaData;

/**
 * Handles all statistics related database interaction
 * @author Tyler Jensen
 */
public class Statistics {
	private static final Logger log = Logger.getLogger(Jobs.class);
	
	
	/**
	 * @param jobs The list of jobs to get overviews for (id required for each job)
	 * @return A hashmap where each key is the job ID and each value is a hashmap 
	 * that contains the statistic's name to value mapping 
	 * (includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	public static HashMap<Integer, HashMap<String, String>> getJobPairOverviews(List<Job> jobs) {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			
			// Create the return map
			HashMap<Integer, HashMap<String, String>> map = new HashMap<Integer, HashMap<String,String>>();
			
			// For each job...
			for(Job j : jobs) {
				// If it has an actual id...
				if(j.getId() > 0) {
					// Put a mapping from the job id to the pair overview for that job into the map
					map.put(j.getId(), Statistics.getJobPairOverview(con, j.getId()));
				}
			}
			
			return map;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**	 
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping 
	 * (this method includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	public static HashMap<String, String> getJobPairOverview(int jobId) throws Exception {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			return Statistics.getJobPairOverview(con, jobId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;						
	}
	
	/**
	 * @param con The connection to make the query on
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping 
	 * (this method includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	protected static HashMap<String, String> getJobPairOverview(Connection con, int jobId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairOverview(?)}");				
		procedure.setInt(1, jobId);		
		ResultSet results = procedure.executeQuery();		
		
		if(results.first()) {
			return Statistics.getMapFromResult(results);
		}
		
		return null;						
	}
	
	/**
	 * Takes in a ResultSet with the cursor on the desired row to convert, and
	 * returns a hashmap where each key/value pair is the column name and the column
	 * value for the record the cursor is pointing to. This DOES NOT iterate through all
	 * records in the resultset
	 * @param result The ResultSet with the cursor pointing to the desired record
	 * @return A hashmap of <Column name, Column Value> pairs
	 */
	protected static HashMap<String, String> getMapFromResult(ResultSet result) throws Exception {
		// Get resultset's metadata
		ResultSetMetaData meta = (ResultSetMetaData) result.getMetaData();
		
		// Create the map to return
		HashMap<String, String> map = new HashMap<String, String>();

		// For each column in the record...
		for(int i = 1; i <= meta.getColumnCount(); i++) {
			// Add key=column name, value=column value to the map
			map.put(meta.getColumnName(i), result.getString(i));
		}
				
		return map;
	}			
}
