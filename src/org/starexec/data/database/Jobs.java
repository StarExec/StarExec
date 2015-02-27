package org.starexec.data.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.CacheType;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.JobStatus.JobStatusCode;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverComparison;
import org.starexec.data.to.SolverStats;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.WorkerNode;
import org.starexec.data.to.compare.JobPairComparator;
import org.starexec.data.to.compare.SolverComparisonComparator;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.util.Util;

/**
 * Handles all database interaction for jobs (NOT grid engine job execution, see JobManager for that)
 * @author Tyler Jensen
 */

public class Jobs {
	private static final Logger log = Logger.getLogger(Jobs.class);
	private static final String sqlDelimiter = ",";
	
	
	private static String[] getSpaceNames(String path) {
		if (path==null || path=="") {
			return new String[] {"job space"};
		}
		return path.split(R.JOB_PAIR_PATH_DELIMITER);
	}
	
	
	/**
	 * Creates all the job spaces needed for a set of pairs. All pairs must have their paths set and
	 * they must all be rooted at the same space. Upon return, each pair will have its job space id set
	 * to the correct job space
	 * @param pairs The list of pairs to make paths for
	 * @param con The open connection to make calls on
	 * @return The ID of the root job space for this list of pairs, or null on error.
	 */
	public static Integer createJobSpacesForPairs(List<JobPair> pairs, Connection con) {
		try {
			HashMap<String, Integer> pathsToIds=new HashMap<String,Integer>(); // maps a job space path to a job space id 
			String topLevel="";
			for (JobPair pair : pairs) {
				log.debug("adding a new pair with path = " +pair.getPath());
				String[] spaces=getSpaceNames(pair.getPath());
				StringBuilder curPathBuilder=new StringBuilder();
				for (int i=0;i<spaces.length;i++) {
					String name=spaces[i];
					curPathBuilder.append(R.JOB_PAIR_PATH_DELIMITER);
					curPathBuilder.append(name);
					if (topLevel.isEmpty()) { //if this is the first space we are making, it is the primary space
						topLevel=curPathBuilder.toString(); 
					}
					//if we need to create a new space
					if (!pathsToIds.containsKey(curPathBuilder.toString())) {
						pathsToIds.put(curPathBuilder.toString(),Spaces.addJobSpace(name,con));
						//associate the new space to its parent
						String parentPath=curPathBuilder.toString();
						parentPath=parentPath.substring(0,parentPath.lastIndexOf('/'));
						if (parentPath.length()>0) {
							Spaces.associateJobSpaces(pathsToIds.get(parentPath), pathsToIds.get(curPathBuilder.toString()),con);
						}
					}
					
				}
				pair.setJobSpaceId(pathsToIds.get(curPathBuilder.toString()));
			}
			return pathsToIds.get(topLevel);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * Makes all new job spaces for the given job and moves all the pairs over to the new spaces.
	 * Useful if the job spaces for the given job were somehow corrupted, but path information for the
	 * pairs is correct
	 * @param jobId The ID of the job to fix
	 * @return True on success and false otherwise
	 */
	public static boolean recompileJobSpaces(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			Common.beginTransaction(con);

			Job j=Jobs.getDetailed(jobId);
			int topLevel=createJobSpacesForPairs(j.getJobPairs(),con);
			Jobs.updatePrimarySpace(jobId, topLevel,con);
			JobPairs.updateJobSpaces(j.getJobPairs(),con);
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			Common.doRollback(con);
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	/**
	 * Given a job with a set of job pairs, each job pair populated with its joblines, creates
	 * a set of solver pipelines to represent all the joblines. Pairs using the same solvers
	 * will use the same pipelines, so this will generally create many fewer pipelines than there are 
	 * job pairs. 
	 * 
	 * Pipelines are not added whenever a pair's joblines already have stage IDs set. In other words,
	 * pipelines are not added whenever the necessary pipelines already exist for a pair
	 * 
	 *
	 * @param j
	 * @return
	 */
	//TODO: Ensure this piece is working correctly
	public static boolean addPipelinesToDatabase(Job j) {
		try {
			//data structure will map unique sequences of configurations to pipelines
			HashMap<String, SolverPipeline> pairsToPipes=new HashMap<String,SolverPipeline>();
			for (JobPair pair : j.getJobPairs()) {
				if (pair.getPrimaryStage().getStageId()!=null) { //don't do anything with pairs that already have associated pipelines
					continue;
				}
				String pairString=pair.getStageString(); //a string that uniquely identifies a pipeline of configs
				if (!pairsToPipes.containsKey(pairString)) { //if we haven't created this pipeline already, create it
					SolverPipeline pipe=new SolverPipeline();

					pair.getStages();
					for (JoblineStage line: pair.getStages()) {
						PipelineStage newStage=new PipelineStage();
						newStage.setConfigId(line.getConfiguration().getId());
						newStage.setKeepOutput(false);
						pipe.addStage(newStage);
					}
					
					pipe.setUserId(j.getUserId());
					log.debug("working with solver id = "+pair.getPrimarySolver().getId()+" with name "+pair.getPrimarySolver().getName());
					pipe.setName(pair.getPrimarySolver().getName());
					pairsToPipes.put(pairString, pipe);
					Pipelines.addPipelineToDatabase(pipe);
				}
				//now we know that the pipeline exists, so we can ensure we find it
				SolverPipeline pipe=pairsToPipes.get(pairString);
				
				//map this jobline stage to its corresponding pipeline stage
				for (int i=0;i<pipe.getStages().size();i++) {
					pair.getStages().get(i).setStageId(pipe.getStages().get(i).getId());
				}
			}
			return true; 
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false; // error		
	}
	
	/**
	 * Adds a new job to the database. NOTE: This only records the job in the 
	 * database, this does not actually submit a job for execution (see JobManager.submitJob).
	 * This method also fills in the IDs of job pairs of the given job object.
	 * @param job The job data to add to the database
	 * @param spaceId The id of the space to add the job to
	 * if pipelines have not yet been created (most cases) and false if they have (job XML)
	 * @return True if the operation was successful, false otherwise.
	 */
	public static boolean add(Job job, int spaceId) {
		Connection con = null;
		PreparedStatement procedure=null;
		try {			
			log.debug("starting to add a new job with pair count =  "+job.getJobPairs().size());
			if (!addPipelinesToDatabase(job)) {
				throw new Exception("Error adding pipelines to database");
			}
			con = Common.getConnection();
			
			Common.beginTransaction(con);
			// maps depth to name to job space id for job spaces
			int topLevel=createJobSpacesForPairs(job.getJobPairs(),con);
		
			log.debug("finished getting subspaces, adding job");
			//the primary space of a job should be a job space ID instead of a space ID
			job.setPrimarySpace(topLevel);
			
			Jobs.addJob(con, job);
			

			//put the job in the space it was created in, assuming a space was selected
			if (spaceId>0) {
				Jobs.associate(con, job.getId(), spaceId);
			}
			
			log.debug("adding job pairs");
			
			for(JobPair pair : job) {
				pair.setJobId(job.getId());
				JobPairs.addJobPair(con, pair);
			}
			Common.endTransaction(con);
			log.debug("job added successfully");
			
			return true;
		} catch(Exception e) {
			log.error("add says " + e.getMessage(), e);
			Common.doRollback(con);
			
		} finally {			
			Common.safeClose(con);	
			Common.safeClose(procedure);
		}

		return false;
	}
	
	
	public static boolean addAllJobSpaceClosureEntries() {
		boolean success=true;
		List<Integer> ids=Spaces.getAllJobSpaces();
		for (Integer i : ids) {
			success=success && Spaces.updateJobSpaceClosureTable(i);
		}
		
		return success;
	}
 	
	
	/**
	 * Adds a job record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @param job The job to add
	 */
	private static void addJob(Connection con, Job job) throws Exception {				
		CallableStatement procedure = null;
		
		 try {
			procedure = con.prepareCall("{CALL AddJob(?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)}");
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
			procedure.setInt(7, job.getPrimarySpace());
			procedure.setLong(8,job.getSeed());
			// The procedure will return the job's new ID in this parameter
			procedure.setInt(9, job.getCpuTimeout());
			procedure.setInt(10,job.getWallclockTimeout());
			procedure.setLong(11, job.getMaxMemory());
			procedure.registerOutParameter(12, java.sql.Types.INTEGER);	
			procedure.executeUpdate();			

			// Update the job's ID so it can be used outside this method
			job.setId(procedure.getInt(12));
		} catch (Exception e) {
			log.error("addJob says "+e.getMessage(),e);
 		}	finally {
 			Common.safeClose(procedure);
 		}
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
		try {
			procedure = con.prepareCall("{CALL AssociateJob(?, ?)}");				
			procedure.setInt(1, jobId);
			procedure.setInt(2, spaceId);			
			procedure.executeUpdate();							

			return true;
		} catch (Exception e) {
			log.error("Jobs.associate says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
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
	 * Removes all job database entries where the job has been deleted
	 * AND has been orphaned
	 * @return True on success, false on error
	 */
	public static boolean cleanOrphanedDeletedJobs() {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL RemoveDeletedOrphanedJobs()}");
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("cleanOrphanedDeletedJobs says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	public static int countOlderPairs(int jobId, int since) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results = null;
		
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL CountOlderPairs(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, since);
			results=procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);

		}
		return -1;
	}
	
	/**
	 * Deletes the job with the given id from disk, and permanently removes the job from the database.
	 * This is used for testing and is NOT the normal procedure for deleting a job! Call "delete" instead.
	 * @param jobId The ID of the job to delete
	 * @return True on success, false otherwise
	 */
	
	public static boolean deleteAndRemove(int jobId) {
		Job j=Jobs.get(jobId);
		if (j!=null) {
			log.debug("Called deleteAndRemove on the following job");
			log.debug(jobId);
			log.debug(j.getName());
		}
		boolean success=delete(jobId);
		if (!success) {
			return false;
		}
		
		success=Jobs.removeJobFromDatabase(jobId);
		
		return success;
	}
	
	/**
	 * Deletes the job with the given id from disk, and sets the "deleted" column
	 * in the database jobs table to true. 
	 * @param jobId The ID of the job to delete
	 * @return True on success, false otherwise
	 */
	
	public static boolean delete(int jobId) {
		//we should kill jobs before deleting  them so no additional pairs are run
		if (!Jobs.isJobComplete(jobId)) {
			Jobs.kill(jobId);
		}
		Connection con=null;
		try {
			con=Common.getConnection();
			Jobs.removeCachedJobStats(jobId,con);
			return delete(jobId,con);
		} catch (Exception e) {
			log.error("deleteJob says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Permanently removes a job from the database. This is a helper function and should NOT be called to delete a job!
	 * It will not delete a job on disk
	 * @param solverId The ID of the solver to remove
	 * @param con The open connection to make the SQL call on
	 * @return True on success and false otherwise
	 */
	
	private static boolean removeJobFromDatabase(int jobId) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("CALL RemoveJobFromDatabase(?)");
			procedure.setInt(1,jobId);
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
	 * Deletes a job from disk, and also sets the delete property to true in the database. Jobs
	 * are only removed from  the database entirely when they have no more space associations.
	 * @param jobId The ID of the job to delete
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	
	protected static boolean delete(int jobId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL DeleteJob(?)}");
			procedure.setInt(1, jobId);		
			procedure.executeUpdate();	
			
			Util.safeDeleteDirectory(getDirectory(jobId));
			
			return true;
		} catch (Exception e) {
			log.error("Delete Job with jobId = "+jobId+" says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Gets information about the job with the given ID. Job pair information is not returned
	 * @param jobId The ID of the job in question
	 * @return The Job object that represents the job with the given ID
	 */
	public static Job get(int jobId) {
		return get(jobId,false);
	}
	

	/**
	 * Gets the wallclock timeout for the given job and the given stage
	 * @param jobId The ID of the job in question
	 * @return The wallclock timeout in seconds, or -1 on error
	 */
	
	public static int getWallclockTimeout(int jobId, int stageId) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		int timeout=-1;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetWallclockTimeout(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,stageId);

			results=procedure.executeQuery();
			if (results.next()) {
				timeout=results.getInt("clockTimeout");
			}
		} catch (Exception e) {
			log.error("getWallclockTimeout says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		
		}
		return timeout;
	}
	
	/**
	 * Gets the CPU timeout for the given job
	 * @param jobId The ID of the job in question
	 * @return The CPU timeout in seconds, or -1 on error
	 */
	
	public static int getCpuTimeout(int jobId, int stageId) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		int timeout=-1;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetCpuTimeout(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,stageId);

			results=procedure.executeQuery();
			if (results.next()) {
				timeout=results.getInt("cpuTimeout");
			}
		} catch (Exception e) {
			log.error("getCpuTimeout says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		
		}
		return timeout;
	}
	
	/**
	 * Sets resource limits for the given job and stage pair
	 * @param jobId
	 * @param stageId
	 * @param cpuTimeout
	 * @param clockTimeout
	 * @param maxMemory
	 * @return
	 */
	public static boolean setJobStageTimeouts(int jobId, int stageId, int cpuTimeout, int clockTimeout, long maxMemory) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL SetJobStageParams(?,?,?,?,?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2,stageId);
			procedure.setInt(3,cpuTimeout);
			procedure.setInt(4,clockTimeout);
			procedure.setLong(5, maxMemory);
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
	 * Gets the maximum memory allowed for the given job in bytes
	 * @param jobId The ID of the job in question
	 * @return The maximum memory in bytes, or -1 on error
	 */
	
	public static long getMaximumMemory(int jobId, int stageId) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		long memory=-1;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetMaxMemory(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,stageId);
			results=procedure.executeQuery();
			if (results.next()) {
				memory=results.getLong("maximum_memory");
			}
		} catch (Exception e) {
			log.error("getCpuTimeout says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		
		}
		return memory;
	}
	
	/**
	 * Gets information about the job with the given ID. Job pair information is not returned
	 * @param jobId The ID of the job in question
	 * @return The Job object that represents the job with the given ID
	 */
	private static Job get(int jobId, boolean includeDeleted) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure=null;
			if (!includeDeleted) {
				procedure = con.prepareCall("{CALL GetJobById(?)}");
			} else {
				procedure = con.prepareCall("{CALL GetJobByIdIncludeDeleted(?)}");
			}
			
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			if(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));
				j.setQueue(Queues.get(con,results.getInt("queue_id")));
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setCreateTime(results.getTimestamp("created"));
				j.setCompleteTime(results.getTimestamp("completed"));
				j.setCpuTimeout(results.getInt("cpuTimeout"));
				j.setWallclockTimeout(results.getInt("clockTimeout"));
				j.setMaxMemory(results.getLong("maximum_memory"));
				j.setPreProcessor(Processors.get(con,results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con,results.getInt("post_processor")));
				j.setDescription(results.getString("description"));
				j.setSeed(results.getLong("seed"));
				return j;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets all the SolverStats objects for a given job in the given space hierarchy
	 * @param jobId the job in question
	 * @param spaceId The ID of the root space in question
	 * @return A list containing every SolverStats for the given job where the solvers reside in the given space
	 * @author Eric Burns
	 */

	public static List<SolverStats> getAllJobStatsInJobSpaceHierarchy(int jobId,int jobSpaceId) {
		List<SolverStats> stats=Jobs.getCachedJobStatsInJobSpaceHierarchy(jobSpaceId);
		//if the size is greater than 0, then this job is done and its stats have already been
		//computed and stored
		if (stats!=null && stats.size()>0) {
			log.debug("stats already cached in database");
			return stats;
		}
		//we will cache the stats only if the job is complete before
		boolean isJobComplete=Jobs.isJobComplete(jobId);

		//otherwise, we need to compile the stats
		log.debug("stats not present in database -- compiling stats now");
		List<JobPair> pairs=getJobPairsForStatsInJobSpace(jobSpaceId);

		
		
		List<SolverStats> newStats=processPairsToSolverStats(pairs);
		if (isJobComplete) {
			saveStats(jobId,jobSpaceId,newStats);
		}
		
		return newStats;
	}
	
	/**
	 * Gets a list of jobs belonging to a space (without its job pairs but with job pair statistics)
	 * @param spaceId The id of the space to get jobs for
	 * @return A list of jobs existing directly in the space
	 * @author Tyler Jensen
	 */
	public static List<Job> getBySpace(int spaceId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetSpaceJobsById(?)}");
			procedure.setInt(1, spaceId);					
			 results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));	
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));	
				j.setCompleteTime(results.getTimestamp("completed"));

				j.setSeed(results.getLong("seed"));
				jobs.add(j);				
			}			
			return jobs;
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
	 * Get all the jobs belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return a list of Jobs belong to the user
	 */
	public static List<Job> getByUserId(int userId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetUserJobsById(?)}");
			procedure.setInt(1, userId);					
			 results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));		
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));
				j.setCompleteTime(results.getTimestamp("completed"));

				j.setSeed(results.getLong("seed"));

				jobs.add(j);				
			}			

			return jobs;
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
	 * Gets the number of Jobs in a given space
	 * 
	 * @param spaceId the id of the space to count the Jobs in
	 * @return the number of Jobs
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobCountBySpace(?)}");
			procedure.setInt(1, spaceId);
			 results = procedure.executeQuery();
			int jobCount = 0;
			if (results.next()) {
				jobCount = results.getInt("jobCount");
			}
			return jobCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}
	/**
	 * Gets the number of Jobs in a given space that match a given query
	 * 
	 * @param spaceId the id of the space to count the Jobs in
	 * @param query The query to match the jobs against
	 * @return the number of Jobs
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobCountBySpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2,query);
			 results = procedure.executeQuery();
			int jobCount = 0;
			if (results.next()) {
				jobCount = results.getInt("jobCount");
			}
			return jobCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}
	
	/**
	 * Retrieves a job from the databas as well as its job pairs and its queue/processor info
	 * @param jobId The ID of the job to get information for
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	
	public static Job getDetailed(int jobId) {
		return getDetailed(jobId,null);
	}
	
	/**
	 * Retrieves a job from the database as well as its job pairs that were completed after
	 * "since" and its queue/processor info
	 * 
	 * @param jobId The id of the job to get information for 
	 * @param since The completion ID after which to get job pairs
	 * @return A job object containing information about the requested job, or null on failure
	 * @author Eric Burns
	 */
	public static Job getDetailed(int jobId, Integer since) {
		log.info("getting detailed info for job " + jobId);
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setInt(1, jobId);					
			results = procedure.executeQuery();
			Job j = new Job();
			if(results.next()){
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));	
				j.setSeed(results.getLong("seed"));

				j.setCreateTime(results.getTimestamp("created"));	
				j.setCompleteTime(results.getTimestamp("completed"));

				j.setPrimarySpace(results.getInt("primary_space"));
				j.setQueue(Queues.get(con, results.getInt("queue_id")));
				j.setPreProcessor(Processors.get(con, results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con, results.getInt("post_processor")));
				j.setCpuTimeout(results.getInt("cpuTimeout"));
				j.setWallclockTimeout(results.getInt("clockTimeout"));
				j.setMaxMemory(results.getLong("maximum_memory"));
			}
			else{
				return null;
			}
			
			
			if (since==null) {
				
				j.setJobPairs(Jobs.getPairsPrimaryStageDetailed(j.getId()));
			} else  {
				j.setJobPairs(Jobs.getNewCompletedPairsDetailed(j.getId(), since));
			}
				
			
			return j;

		} catch (Exception e){			
			log.error("job get detailed for job id = " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return null;
	}
	
	/**
	 * Returns the filepath to the directory containing this job's output
	 * @param jobId The job to get the filepath for
	 * @return A string representing the path to the output directory
	 * @author Eric Burns
	 */
	
	public static String getDirectory(int jobId) {
		// The job's output is expected to be in NEW_JOB_OUTPUT_DIR/{job id}/
		File file=new File(R.NEW_JOB_OUTPUT_DIR,String.valueOf(jobId));
		return file.getAbsolutePath();
	}
	
	public static String getLogDirectory(int jobId) {
		// The job's output is expected to be in NEW_JOB_OUTPUT_DIR/{job id}/
		File file=new File(R.JOB_LOG_DIR,String.valueOf(jobId));
		return file.getAbsolutePath();
	}
	
	/**
	 * Gets all job pairs that are enqueued(up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getEnqueuedPairs(Connection con, int jobId){	
		log.debug("getEnqueuePairs2 beginning...");
		CallableStatement procedure = null;
		ResultSet results = null;
		 try {
			procedure = con.prepareCall("{CALL GetEnqueuedJobPairsByJob(?)}");
			procedure.setInt(1, jobId);					
			results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();
			//we map ID's to  primitives so we don't need to query the database repeatedly for them
			HashMap<Integer, Solver> solvers=new HashMap<Integer,Solver>();
			HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
			HashMap<Integer,WorkerNode> nodes=new HashMap<Integer,WorkerNode>();
			HashMap<Integer,Benchmark> benchmarks=new HashMap<Integer,Benchmark>();
			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				int nodeId=results.getInt("node_id");
				if (!nodes.containsKey(nodeId)) {
					nodes.put(nodeId,Cluster.getNodeDetails(con,nodeId));
				}
				jp.setNode(nodes.get(nodeId));	
				int benchId=results.getInt("bench_id");
				if (!benchmarks.containsKey(benchId)) {
					benchmarks.put(benchId,Benchmarks.get(con,benchId,false));
				}
				jp.setBench(benchmarks.get(benchId));
				int configId=results.getInt("config_id");
				if (!configs.containsKey(configId)) {
					configs.put(configId, Solvers.getConfiguration(con,configId));
					solvers.put(configId, Solvers.getSolverByConfig(con,configId,false));
				}
				JoblineStage stage=new JoblineStage();
				
				stage.setSolver(solvers.get(configId));
				stage.setConfiguration(configs.get(configId));
				jp.addStage(stage);
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				returnList.add(jp);
			}			
			log.debug("returnList = " + returnList);
			return returnList;
		} catch (Exception e) {
			log.error("getEnqueuedPairs says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	

	/**
	 * Gets all job pairs that are enqueued (up to limit) for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	public static List<JobPair> getEnqueuedPairs(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return Jobs.getEnqueuedPairs(con, jobId);
		} catch (Exception e){			
			log.error("getEnqueuedPairsDetailed for queue " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}
	
	public static Job getIncludeDeleted(int jobId) {
		return get(jobId,true);
	}
	
	
	/**
	 * Gets all the the attributes for every job pair in a job, and returns a HashMap
	 * mapping pair IDs to their attributes
	 * @param con The connection to make the query on
	 * @param The ID of the job to get attributes of
	 * @return A HashMap mapping pair IDs to properties. Some values may be null
	 * @author Eric Burns
	 */
	protected static HashMap<Integer,Properties> getJobAttributes(Connection con, int jobId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		log.debug("Getting all attributes for job with ID = "+jobId);
		 try {
			
			procedure = con.prepareCall("{CALL GetJobAttrs(?)}");
			procedure.setInt(1, jobId);
 
			results = procedure.executeQuery();
			return processAttrResults(results);
		} catch (Exception e) {
			log.error("get Job Attrs says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	/**
	 * Gets all attributes for every job pair associated with the given job
	 * @param jobId The ID of the job in question
	 * @return A HashMap mapping integer job-pair IDs to Properties objects representing
	 * their attributes
	 * @author Eric Burns
	 */
	public static HashMap<Integer,Properties> getJobAttributes(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return getJobAttributes(con,jobId);
		} catch (Exception e) {
			log.error("getJobAttributes says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}
	
	/**
	 * Gets attributes with the given key for all pairs in a given job space
	 * @param con The open connection to make the query on
	 * @param jobSpaceId The ID of the job space containing the pairs
	 * @param key the key of the attributes to retrieve
	 * @return A HashMap mapping job pair IDs to attributes
	 * @author Eric Burns
	 */
	
	protected static HashMap<Integer,Properties> getJobAttributesInJobSpaceByKey(Connection con,int jobSpaceId, String key) {
		CallableStatement procedure = null;
		ResultSet results = null;
		 try {
				
			procedure = con.prepareCall("{CALL GetJobAttrsInJobSpaceByKey(?,?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setString(2,key);
			results = procedure.executeQuery();
			return processAttrResults(results);
		} catch (Exception e) {
			log.error("getJobAttrsInJobSpace says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets attributes for all pairs in a given job space
	 * @param con The open connection to make the query on
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The ID of the job space containing the pairs
	 * @return A HashMap mapping job pair IDs to attributes
	 * @author Eric Burns
	 */
	
	protected static HashMap<Integer,Properties> getJobAttributesInJobSpace(Connection con,int jobSpaceId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		 try {
				
			procedure = con.prepareCall("{CALL GetJobAttrsInJobSpace(?)}");
			procedure.setInt(1,jobSpaceId);
			results = procedure.executeQuery();
			return processAttrResults(results);
		} catch (Exception e) {
			log.error("getJobAttrsInJobSpace says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets attributes of the given key for every job pair in the given job space
	 * @param jobSpaceId The ID of the job space containing the pairs
	 * @param key The key of the attribute to retrieve
	 * @return A HashMap mapping integer job-pair IDs to Properties objects representing
	 * their attributes
	 * @author Eric Burns
	 */
	public static HashMap<Integer,Properties> getJobAttributesInJobSpaceByKey(int jobSpaceId, String key) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return getJobAttributesInJobSpaceByKey(con,jobSpaceId,key);
		} catch (Exception e) {
			log.error("getJobAttributes says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all attributes for every job pair in the given job space
	 * @param jobSpaceId The ID of the job space containing the pairs
	 * @return A HashMap mapping integer job-pair IDs to Properties objects representing
	 * their attributes
	 * @author Eric Burns
	 */
	public static HashMap<Integer,Properties> getJobAttributesInJobSpace(int jobSpaceId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return getJobAttributesInJobSpace(con,jobSpaceId);
		} catch (Exception e) {
			log.error("getJobAttributes says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}
	
	
	/**
	 * Gets the number of Jobs in the whole system
	 * 
	 * @author Wyatt Kaiser
	 */
	
	public static int getJobCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobCount()}");
			results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobCountByUser(?)}");
			procedure.setInt(1, userId);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;		
	}
	/**
	 * Get the total count of the jobs belong to a specific user that match a specific query
	 * @param userId Id of the user we are looking for
	 * @param query The query to match the jobs against
	 * @return The count of the jobs
	 * @author Eric Burns
	 */
	public static int getJobCountByUser(int userId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobCountByUserWithQuery(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2,query);
			 results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;		
	}
	

	
	/**
	 * Gets the number of job pairs for a given job, in a given space, with the given configuration 
	 * (which also uniquely identifies the solver)
	 * 
	 * @param jobId the job to count the pairs for
	 * @param spaceId the space to count the pairs in
	 * @param configId the configuration id to count the pairs for
	 * @return the number of job pairs
	 * @author Eric Burns
	 */
	public static int getJobPairCountByConfigInJobSpace(int jobId,int spaceId, int configId) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobPairCountByConfigInJobSpace(?,?, ?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2, spaceId);
			procedure.setInt(3, configId);
			results = procedure.executeQuery();
			int jobCount = 0;
			if (results.next()) {
				jobCount = results.getInt("jobPairCount");
			}
			
			return jobCount;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}
	
	/**
	 * Returns the number of job pairs that exist for a given job in a given space
	 * 
	 * @param jobId the id of the job to get the number of job pairs for
	 * @param jobSpaceId The ID of the job space containing the paris to count
	 * @param Whether to count all job pairs in the hierarchy rooted at the job space with the given id
	 * @return the number of job pairs for the given job or -1 on failure
	 * @author Eric Burns
	 */
	public static int getJobPairCountInJobSpace(int jobSpaceId, boolean hierarchy) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			if (hierarchy) {
				Spaces.updateJobSpaceClosureTable(jobSpaceId);
				procedure = con.prepareCall("{CALL GetJobPairCountInJobSpaceHierarchy(?)}");
			} else {
				 procedure = con.prepareCall("{CALL GetJobPairCountInJobSpace(?)}");
			}
			procedure.setInt(1,jobSpaceId);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("jobPairCount");
			}
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return -1;		
	}
	
	/**
	 * Returns the number of job pairs that exist for a given job in a given space
	 * 
	 * @param jobId the id of the job to get the number of job pairs for
	 * @param jobSpaceId The ID of the job space containing the paris to count
	 * @param query The query to match the job pairs against
	 * @return the number of job pairs for the given job
	 * @author Eric Burns
	 */
	public static int getJobPairCountInJobSpace(int jobSpaceId, String query) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		int jobPairCount=0;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobPairCountByJobInJobSpaceWithQuery(?, ?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setString(2, query);
			results = procedure.executeQuery();
			if (results.next()) {
				jobPairCount = results.getInt("jobPairCount");
			}
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return jobPairCount;		
	}
	
	/**
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object, where
	 * all the job pairs are in the given space and were operated on by the configuration with the given config ID
	 * @param startingRecord The first record to return
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether to sort ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The column of the datatable to sort on 
	 * @param searchQuery A search query to match against the pair's solver, config, or benchmark
	 * @param jobId The ID of the job in question
	 * @param spaceID The space that contains the job pairs
	 * @param configID The ID of the configuration responsible for the job pairs
	 * @param totals A size 2 int array that, upon return, will contain in the first slot the total number
	 * of pairs and in the second slot the total number of pairs after filtering
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object 
	 * @author Eric Burns
	 */
	public static List<SolverComparison> getSolverComparisonsForNextPageByConfigInJobSpaceHierarchy(int startingRecord, int recordsPerPage, boolean isSortedASC, 
			int indexOfColumnSortedBy, String searchQuery, int jobId, int jobSpaceId, int configId1, int configId2, int[] totals, boolean wallclock) {
		List<JobPair> pairs1=Jobs.getJobPairsForTableInJobSpaceHierarchy(jobId,jobSpaceId,configId1);
		pairs1=JobPairs.filterPairsByType(pairs1, "complete");
		List<JobPair> pairs2=Jobs.getJobPairsForTableInJobSpaceHierarchy(jobId,jobSpaceId,configId2);
		pairs2=JobPairs.filterPairsByType(pairs2, "complete");
		List<SolverComparison> comparisons=new ArrayList<SolverComparison>();
		HashMap<Integer,JobPair> benchesToPairs=new HashMap<Integer,JobPair>();
		for (JobPair jp : pairs1) {
			benchesToPairs.put(jp.getBench().getId(), jp);
		}
		for (JobPair jp : pairs2) {
			if (benchesToPairs.containsKey(jp.getBench().getId())) {
				try {
					comparisons.add(new SolverComparison(benchesToPairs.get(jp.getBench().getId()), jp));
				} catch (Exception e) {
					log.error(e.getMessage(),e);
				}
			}
		}
		
		totals[0]=comparisons.size();
		comparisons=JobPairs.filterComparisons(comparisons, searchQuery);

		totals[1]=comparisons.size();
		SolverComparisonComparator compare=new SolverComparisonComparator(indexOfColumnSortedBy,wallclock);
		return Util.handlePagination(comparisons, compare, startingRecord, recordsPerPage, isSortedASC);

	}
	
	
	
	/**
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object, where
	 * all the job pairs are in the given space and were operated on by the configuration with the given config ID
	 * @param startingRecord The first record to return
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether to sort ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The column of the datatable to sort on 
	 * @param searchQuery A search query to match against the pair's solver, config, or benchmark
	 * @param jobId The ID of the job in question
	 * @param spaceID The space that contains the job pairs
	 * @param configID The ID of the configuration responsible for the job pairs
	 * @param totals A size 2 int array that, upon return, will contain in the first slot the total number
	 * of pairs and in the second slot the total number of pairs after filtering
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object 
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsForNextPageByConfigInJobSpaceHierarchy(int startingRecord, int recordsPerPage, boolean isSortedASC, 
			int indexOfColumnSortedBy, String searchQuery, int jobId, int jobSpaceId, int configId, int[] totals, String type, boolean wallclock) {
		long a=System.currentTimeMillis();
		//get all of the pairs first, then carry out sorting and filtering
		//PERFMORMANCE NOTE: this call takes over 99.5% of the total time this function takes
		List<JobPair> pairs=Jobs.getJobPairsForTableInJobSpaceHierarchy(jobId,jobSpaceId,configId);
		log.debug("getting all the pairs by config in job space took "+(System.currentTimeMillis()-a));
		pairs=JobPairs.filterPairsByType(pairs, type);
		totals[0]=pairs.size();
		pairs=JobPairs.filterPairs(pairs, searchQuery);
		log.debug("filtering pairs took "+(System.currentTimeMillis()-a));

		totals[1]=pairs.size();
		if (!wallclock && indexOfColumnSortedBy==4) {
			indexOfColumnSortedBy=8;
		}
		JobPairComparator compare=new JobPairComparator(indexOfColumnSortedBy);
		return Util.handlePagination(pairs, compare, startingRecord, recordsPerPage, isSortedASC);
	}
	
	/**
	 * If the given string is null, returns a placeholder string. Otherwise, returns the given string
	 * @param value
	 * @return
	 */
	public static String getPropertyOrPlaceholder(String value) {
		if (value==null) {
			return "--";
		}
		return value;
	}
	
	/**
	 * Gets all the JobPairs in a given job space that were solved by every solver/configuration pair in that space
	 * @param jobSpaceId
	 * @return
	 */
	//TODO: This currently works only on primary stages-- probably needs to be pdated
	public static List<JobPair> getSynchronizedPairsInJobSpace(int jobSpaceId, int jobId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		HashSet<String> solverConfigPairs=new HashSet<String>(); // will store all the solver/configuration pairs so we know how many there are
		HashMap<Integer, Integer> benchmarksCount=new HashMap<Integer,Integer>(); //will store the number of pairs every benchmark has
		List<JobPair> pairs=new ArrayList<JobPair>();
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetCompletedJobPairsInJobSpace(?)}");
			procedure.setInt(1, jobSpaceId);
			results=procedure.executeQuery();
			
			pairs=getJobPairsForDataTable(jobId,results,false,true);
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		for (JobPair p : pairs) {
			solverConfigPairs.add(p.getPrimarySolver().getId()+":"+p.getPrimaryConfiguration().getId());
			if (!benchmarksCount.containsKey(p.getBench().getId())) {
				benchmarksCount.put(p.getBench().getId(), 1);
			} else {
				benchmarksCount.put(p.getBench().getId(), 1+benchmarksCount.get(p.getBench().getId()));
			}
		}
		
		//now, we exclude pairs that have benchmarks where the benchmark count is not equal to the solver/config count
		
		List<JobPair> returnList=new ArrayList<JobPair>();
		int solverCount=solverConfigPairs.size();
		for (JobPair p : pairs) {
			if (benchmarksCount.get(p.getBench().getId())== solverCount) {
				returnList.add(p);
			}
		}
		return returnList;
	}
	/**
	 * Gets the JobPairs necessary to make the next page of a DataTable of synchronized job pairs in a specific job space
	 * @param startingRecord 
	 * @param recordsPerPage
	 * @param isSortedASC
	 * @param indexOfColumnSortedBy
	 * @param searchQuery
	 * @param jobId
	 * @param jobSpaceId
	 * @param wallclock
	 * @param totals Must be a size 2 array. The first slot will have the number of results before the query, and the second slot will have the number of results after the query
	 * @return
	 */
	public static List<JobPair> getSynchronizedJobPairsForNextPageInJobSpace(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId, int jobSpaceId, boolean wallclock, int[] totals) {
		long a=System.currentTimeMillis();
		List<JobPair> pairs=Jobs.getSynchronizedPairsInJobSpace(jobSpaceId, jobId);
		log.debug("getting synced pairs took "+(System.currentTimeMillis()-a));
		totals[0]=pairs.size();
		pairs=JobPairs.filterPairs(pairs, searchQuery);
		log.debug("filtering pairs took "+(System.currentTimeMillis()-a));
		totals[1]=pairs.size();
		if (!wallclock && indexOfColumnSortedBy==4) {
			indexOfColumnSortedBy=8;
		}
		Comparator<JobPair> compare=new JobPairComparator(indexOfColumnSortedBy);
		return Util.handlePagination(pairs, compare, startingRecord, recordsPerPage, isSortedASC);
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
	public static List<JobPair> getJobPairsForNextPageInJobSpace(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId, int jobSpaceId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		if (searchQuery==null) {
			searchQuery="";
		}
		try {
			
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetNextPageOfJobPairsInJobSpace(?, ?, ?, ?, ?,?)}");
				
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setBoolean(3, isSortedASC);
			procedure.setString(4, searchQuery);
			procedure.setInt(5,jobSpaceId);
			procedure.setInt(6,indexOfColumnSortedBy);

			results = procedure.executeQuery();
			List<JobPair> jobPairs = getJobPairsForDataTable(jobId,results,false,false);
			
				
			
			return jobPairs;
		} catch (Exception e){			
			log.error("get JobPairs for Next Page of Job " + jobId + " says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}
	
	/**
	 * Gets benchmarks attributes with a specific key for all benchmarks used by a given job
	 * @param jobId The job in question
	 * @param attrKey The string key of the attribute to return 
	 * @return A hashmap mapping benchmark ids to attribute values
	 */
	public static HashMap<Integer,String> getAllAttrsOfNameForJob(int jobId, String attrKey) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetAttrsOfNameForJob(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setString(2, attrKey);
			results=procedure.executeQuery();
			HashMap<Integer,String> idsToValues=new HashMap<Integer,String>();
			
			while (results.next()) {
				idsToValues.put(results.getInt("job_pairs.bench_id"), results.getString("attr_value"));
			}
			log.debug("found this number of attrs = "+idsToValues.size());
			return idsToValues;
			
		} catch (Exception e) {
			log.error("getAllAttrsOfNameForJob says "+e.getMessage(),e );
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Given a list of job pairs and a ResultSet that contains stages for those pairs, populates
	 * the pairs with their stages
	 * @param pairs
	 * @param results
	 * @return
	 */
	public static boolean populateJobPairStages(List<JobPair> pairs, ResultSet results) {
		
		HashMap<Integer,Solver> solvers=new HashMap<Integer,Solver>();
		HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
		Integer id;
		Solver solve=null;
		Configuration config=null;
		HashMap<Integer,JobPair> idsToPairs = new HashMap<Integer,JobPair>();
		try {
			for(JobPair pair : pairs) {
				log.debug("found a pair with the following id ="+pair.getId());
				idsToPairs.put(pair.getId(), pair);
			}
			
			//every row in this resultset is a single stage 
			while (results.next()) {
				
				JobPair jp=idsToPairs.get(results.getInt("job_pairs.id"));
				JoblineStage stage=new JoblineStage();
				stage.setCpuUsage(results.getDouble("cpu"));
				stage.setWallclockTime(results.getDouble("wallclock"));
				stage.setStageId(results.getInt("pipeline_stages.stage_id"));
				//everything below this line is in a stage
				id=results.getInt("solver_id");
				//means it was null in SQL
				if (id==0) {
					id=jp.getDefaultSolver().getId();
					solvers.put(id, jp.getDefaultSolver());
				}
				
				if (!solvers.containsKey(id)) {
					
					solve=new Solver();
					solve.setId(id);
					solve.setName(results.getString("solver_name"));
					solvers.put(id,solve);
				}
				stage.setSolver(solvers.get(id));
				
				
				
				id=results.getInt("config_id");
				
				if (id==0) {
					id=jp.getDefaultConfiguration().getId();
					configs.put(id, jp.getDefaultConfiguration());
				}
				if (!configs.containsKey(id)) {
					config=new Configuration();
					config.setId(id);
					config.setName(results.getString("config_name"));
					configs.put(id, config);
				}
				stage.setConfiguration(configs.get(id));
				jp.addStage(stage);

			}
			

			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return false;
	}
	
	/**
	 * Returns all of the job pairs in a given job space hierarchy, populated with all the fields necessary
	 * to display in a SolverStats table
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsForStatsInJobSpace(int jobSpaceId) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("called with jobSpaceId = "+ jobSpaceId);
		try {
			Spaces.updateJobSpaceClosureTable(jobSpaceId);

			con=Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsInJobSpaceHierarchy(?)}");
			
			procedure.setInt(1,jobSpaceId);
			results = procedure.executeQuery();
			
			List<JobPair> pairs=processStatResults(results);
			
			
			
			Common.safeClose(procedure);
			Common.safeClose(results);
			procedure=con.prepareCall("{CALL GetJobPairStagesInJobSpaceHierarchy(?)}");
			procedure.setInt(1,jobSpaceId);
			results=procedure.executeQuery();
			if (populateJobPairStages(pairs,results)) {
				return pairs;
			} 
		} catch (Exception e) {
			log.error("getPairsDetailedForStatsInSpace says "+e.getMessage(),e);
			
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
			
		}	
		return null;
	}
	
	/**
	 * Makes all the job pair objects from a ResultSet formed from querying the database
	 * for fields needed on the pairsInSpace table
	 * @param jobId The ID of the job containing all these pairs
	 * @param results
	 * @return The list of job pairs or null on failure
	 */
	
	//TODO: This just gets the primary stage right now-- may need to be updated to get all stages
	private static List<JobPair> getJobPairsForDataTable(int jobId,ResultSet results, boolean includeExpected, boolean includeCompletion) {
		List<JobPair> pairs = new ArrayList<JobPair>();
		try{
			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("id"));
				JoblineStage stage=new JoblineStage();
				stage.setWallclockTime(results.getDouble("wallclock"));
				stage.setCpuUsage(results.getDouble("cpu"));
				
				jp.addStage(stage);
				Benchmark bench = jp.getBench();
				bench.setId(results.getInt("bench_id"));
				bench.setName(results.getString("bench_name"));
				
				jp.getPrimarySolver().setId(results.getInt("solver_id"));
				jp.getPrimarySolver().setName(results.getString("solver_name"));
				jp.getPrimaryConfiguration().setId(results.getInt("config_id"));
				jp.getPrimaryConfiguration().setName(results.getString("config_name"));
				jp.getPrimarySolver().addConfiguration(jp.getPrimaryConfiguration());
				
				Status status = jp.getStatus();
				status.setCode(results.getInt("status_code"));
				
				Properties attributes = jp.getAttributes();
				String result=results.getString("result");
				if (result!=null) {
					attributes.put(R.STAREXEC_RESULT, result);
				}
				if (includeCompletion) {
					jp.setCompletionId(results.getInt("completion_id"));

				}
				if (includeExpected) {

					String expected=results.getString("expected");
					if (expected!=null) {
						attributes.put(R.EXPECTED_RESULT, expected);

					}
				}
				
				pairs.add(jp);	
			}
			return pairs;
		} catch (Exception e) {
			log.error("getJobPairsForDataTable says "+e.getMessage(),e);
		}
		return null;
	}
	

	
	
	/**
	 * Gets all the job pairs necessary to view in a datatable for a job space 
	 * @param jobId The id of the job in question
	 * @param jobSpaceId The id of the job_space id in question
	 * @param id
	 * @return
	 */
	
	public static List<JobPair> getJobPairsForTableInJobSpaceHierarchy(int jobId,int jobSpaceId, int id) {
		Spaces.updateJobSpaceClosureTable(jobSpaceId);
		log.debug("beginning function getJobPairsForTableByConfigInJobSpace");
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	

			log.info("getting detailed pairs for job " + jobId +" with configId = "+id+" in space "+jobSpaceId);
			//otherwise, just get the completed ones that were completed later than lastSeen
			procedure = con.prepareCall("{CALL GetJobPairsForTableByConfigInJobSpaceHierarchy(?, ?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,id);

			results = procedure.executeQuery();
			List<JobPair> pairs = getJobPairsForDataTable(jobId,results,true,true);
			
			return pairs;
		}catch (Exception e) {
			log.error("getJobPairsForTableByConfigInJobSpace says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets shallow job pair information for the given job, where all the pairs had benchmarks in the given job_space
	 * and used the configuration with the given ID. Used to make the space overview chart, so only info needed for
	 * it is returned. In particular, pairs are populated only with their primary stage!
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The ID of the job_space in question
	 * @param configId The ID of the configuration in question
	 * @return A List of JobPair objects
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getJobPairsShallowByConfigInJobSpace(int jobSpaceId, int configId) {
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			Spaces.updateJobSpaceClosureTable(jobSpaceId);

			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsShallowByConfigInJobSpaceHierarchy(?, ?)}");

			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,configId);

			results = procedure.executeQuery();
			List<JobPair> pairs = new ArrayList<JobPair>();
			while (results.next()) {
				JobPair jp=new JobPair();
				Status s=jp.getStatus();
				s.setCode(results.getInt("status_code"));
				JoblineStage stage=new JoblineStage();
				jp.setId(results.getInt("job_pairs.id"));
				stage.setWallclockTime(results.getDouble("wallclock"));
				stage.setCpuUsage(results.getDouble("cpu"));
				jp.addStage(stage);
				Solver solver=jp.getPrimarySolver();
				solver.setId(results.getInt("solver_id"));
				solver.setName(results.getString("solver_name"));
				Configuration c=jp.getPrimaryConfiguration();
				c.setId(results.getInt("config_id"));
				c.setName(results.getString("config_name"));
				solver.addConfiguration(c);
				Benchmark b=jp.getBench();
				b.setId(results.getInt("bench_id"));
				b.setName(results.getString("bench_name"));
				
				pairs.add(jp);
			}
			return pairs;
		}catch (Exception e) {
			log.error("getJobPairsShallowByConfigInJobSpace says " +e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	/**
	 * Returns the count of all pairs in a job
	 * @param jobId
	 * @return
	 */
	public static int getPairCount(int jobId) {
		Connection con=null;
		ResultSet results=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL countPairsForJob(?)}");
			procedure.setInt(1, jobId);
			results=procedure.executeQuery();
			
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);

		}
		return -1;
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
		return getJobsForNextPage(startingRecord,recordsPerPage, isSortedASC, indexOfColumnSortedBy, searchQuery, userId,"GetNextPageOfUserJobs");
	}
	
	/**
	 * Get next page of the jobs belong to a space
	 * @param startingRecord specifies the number of the entry where should the querry start
	 * @param recordsPerPage specifies how many records are going to be on one page
	 * @param isSortedASC specifies whether the sorting is in ascending order
	 * @param indexOfColumnSortedBy specifies which column the sorting is applied
	 * @param searchQuery the search query provided by the client
	 * @param spaceId Id of the space we are looking for
	 * @return a list of Jobs belong to the user
	 * @author Ruoyu Zhang
	 */
	
	public static List<Job> getJobsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int spaceId) {
		return getJobsForNextPage(startingRecord,recordsPerPage, isSortedASC, indexOfColumnSortedBy, searchQuery, spaceId,"GetNextPageOfJobs");
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
	 * @param id the id of the space or user to get jobs for
	 * @return a list of 10, 25, 50, or 100 Jobs containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	private static List<Job> getJobsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int id, String procedureName) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL "+procedureName+"(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, id);
			procedure.setString(6, searchQuery);

			 results = procedure.executeQuery();
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
				j.setUserId(results.getInt("user_id"));
				if (results.getBoolean("deleted")) {
					j.setName(j.getName()+" (deleted)");
				}
				j.setDeleted(results.getBoolean("deleted"));
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));
				j.setCompleteTime(results.getTimestamp("completed"));
				j.setLiteJobPairStats(liteJobPairStats);
				jobs.add(j);		
			}	
			return jobs;
		} catch (Exception e){			
			log.error("getJobsForNextPageSays " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**Gets the minimal number of Jobs necessary in order to service the client's 
	 * request for the next page of Users in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Jobs from
	 * @param recordesPerpage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)	 
	 * 
	 * @return a list of 10, 25, 50, or 100 Users containing the minimal amount of data necessary
	 * @author Wyatt Kaiser
	 **/
	public static List<Job> getJobsForNextPageAdmin(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfAllJobs(?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setString(5, searchQuery);
			results = procedure.executeQuery();
			
			List<Job> jobs = new LinkedList<Job>();
			
			while(results.next()){

				if (results.getString("status").equals("incomplete")) {
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
					j.setPrimarySpace(results.getInt("primary_space"));
					j.setUserId(results.getInt("user_id"));
					j.setName(results.getString("name"));	
					if (results.getBoolean("deleted")) {
						j.setName(j.getName()+" (deleted)");
					}
					j.setDescription(results.getString("description"));	

					j.setCreateTime(results.getTimestamp("created"));
					j.setCompleteTime(results.getTimestamp("completed"));

					j.setLiteJobPairStats(liteJobPairStats);
					jobs.add(j);	
				}
				
							
			}	
			
			return jobs;
		} catch (Exception e){			
			log.error("GetNextPageOfRunningJobsAdmin says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
    
	/**
	 * Attempts to retrieve cached SolverStats objects from the database. Returns
	 * an empty list if the stats have not already been cached.
	 * @param jobSpaceId The ID of the root job space for the stats
	 * @return A list of the relevant SolverStats objects in this space
	 * @author Eric Burns
	 */
	
	public static List<SolverStats> getCachedJobStatsInJobSpaceHierarchy(int jobSpaceId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetJobStatsInJobSpace(?)}");
			procedure.setInt(1,jobSpaceId);
			results=procedure.executeQuery();
			List<SolverStats> stats=new ArrayList<SolverStats>();
			while (results.next()) {
				SolverStats s=new SolverStats();
				s.setCompleteJobPairs(results.getInt("complete"));
				s.setIncompleteJobPairs(results.getInt("incomplete")); 
				s.setWallTime(results.getDouble("wallclock"));
				s.setCpuTime(results.getDouble("cpu"));
				s.setFailedJobPairs(results.getInt("failed"));
				s.setIncorrectJobPairs(results.getInt("incorrect"));
				s.setCorrectJobPairs(results.getInt("correct"));
				s.setResourceOutJobPairs(results.getInt("resource_out"));
				Solver solver=new Solver();
				solver.setName(results.getString("solver.name"));
				solver.setId(results.getInt("solver.id"));
				Configuration c=new Configuration();
				c.setName(results.getString("config.name"));
				c.setId(results.getInt("config.id"));
				solver.addConfiguration(c);
				s.setSolver(solver);
				s.setConfiguration(c);
				stats.add(s);
			}
			return stats;
		} catch (Exception e) {
			log.error("getJobStatsInJobSpaceHierarchy says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Gets all job pairs for the given job that have been completed after a given point and also
	 * populates its resource TOs.
	 * @param jobId The id of the job to get pairs for
	 * @param since The completed ID after which to get all jobs
	 * @return A list of job pair objects representing all job pairs completed after "since" for a given job
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getNewCompletedPairsDetailed(int jobId, int since) {
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	
			
			log.info("getting detailed pairs for job " + jobId );
			
			 procedure = con.prepareCall("{CALL GetNewCompletedJobPairsByJob(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,since);
			results = procedure.executeQuery();
			List<JobPair> pairs= getPairsDetailed(jobId,con,results,true);
			HashMap<Integer,Properties> props=Jobs.getNewJobAttributes(con,jobId,since);
			
			for (Integer i =0; i < pairs.size(); i++){
				JobPair jp = pairs.get(i);
				if (props.containsKey(jp.getId())) {
					jp.setAttributes(props.get(jp.getId()));
				} else {
					log.debug("forced to get attributes for a single job pair");
					jp.setAttributes(JobPairs.getAttributes(jp.getId()));
				}
			}
			return pairs;
		} catch (Exception e) {
			log.error("getNewCompletedPairsDetailed says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * For a given job, gets every job pair with the minimal amount of information required
	 * to find the job pair output on disk
	 * @param jobId The ID of the job to get pairs for
	 * @param since Only gets pairs that were finished after "completion ID"
	 * @return A list of JobPair objects
	 */
	public static List<JobPair> getNewCompletedPairsShallow(int jobId, int since) {
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	
			log.debug("getting shallow pairs for job " + jobId );
			//otherwise, just get the completed ones that were completed later than lastSeen
			procedure = con.prepareCall("{CALL GetNewJobPairFilePathInfoByJob(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,since);
			results = procedure.executeQuery();
			List<JobPair> pairs=new ArrayList<JobPair>();	
			while (results.next()) {
				JobPair pair=new JobPair();
				pair.setJobId(jobId);
				pair.setId(results.getInt("id"));
				pair.setPath(results.getString("path"));
				pair.getPrimarySolver().setName(results.getString("solver_name"));
				pair.getPrimaryConfiguration().setName(results.getString("config_name"));
				pair.getBench().setName(results.getString("bench_name"));
				pair.setCompletionId(results.getInt("completion_id"));
				pairs.add(pair);
			}
			return pairs;
		} catch (Exception e) {
			log.error("getNewCompletedPairsDetailed says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets attributes for all pairs with completion IDs greater than completionId
	 * @param con The open connection to make the query on
	 * @param jobId The ID of the job in question
	 * @param completionId The completion ID after which the pairs are relevant
	 * @return A HashMap mapping job pair IDs to attributes
	 * @author Eric Burns
	 */
	
	protected static HashMap<Integer,Properties> getNewJobAttributes(Connection con, int jobId,Integer completionId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		log.debug("Getting all new attributes for job with ID = "+jobId);
		 try {
			procedure=con.prepareCall("{CALL GetNewJobAttrs(?, ?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2,completionId);
			results = procedure.executeQuery();
			return processAttrResults(results);
		} catch (Exception e) {
			log.error("getNewJobAttrs says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all attributes for every job pair associated with the given job completed after "completionId"
	 * @param jobId The ID of the job in question
	 * @param completionId The completion ID after which the pairs are relevant
	 * @return A HashMap mapping integer job-pair IDs to Properties objects representing
	 * their attributes
	 * @author Eric Burns
	 */
	public static HashMap<Integer,Properties> getNewJobAttributes(int jobId, int completionId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return getNewJobAttributes(con,jobId, completionId);
		} catch (Exception e) {
			log.error("getJobAttributes says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all job pairs for the given job non-recursively (simple version)
	 * (Worker node, benchmark and solver will NOT be populated)
	 * only populates status code id, bench id and config id
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Julio Cervantes
	 */
	protected static List<JobPair> getPairsSimple(Connection con, int jobId) throws Exception {			
		CallableStatement procedure = null;
		ResultSet results = null;
		
		 try {
			procedure = con.prepareCall("{CALL GetJobPairsByJobSimple(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
			    JobPair jp = new JobPair();
			    JoblineStage stage=new JoblineStage();
			    Configuration c=new Configuration();
			    Solver s=new Solver();
			    stage.setConfiguration(c);
			    s.addConfiguration(c);
			    stage.setSolver(s);
			    jp.addStage(stage);
			    jp.setId(results.getInt("id"));
			    jp.getStatus().setCode(results.getInt("status_code"));
			    jp.getBench().setId(results.getInt("bench_id"));
			    jp.getBench().setName(results.getString("bench_name"));
			    c.setId(results.getInt("config_id"));
			    c.setName(results.getString("config_name"));
			    s.setId(results.getInt("solver_id"));
			    s.setName(results.getString("solver_name"));
			    jp.getSpace().setName(results.getString("name"));
			    jp.getSpace().setId(results.getInt("job_spaces.id"));
			    jp.setPath(results.getString("path"));
			    returnList.add(jp);
			}			
			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			log.error("getPairsSimple says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			
		}
		 return null;
	}

	/**
	 * Gets all job pairs for the given job non-recursively (simple version to test job xml bug)
	 * (Worker node, status, benchmark and solver will NOT be populated) 
	 * only populates status code id, bench id and config id
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Julio Cervantes
	 */
	public static List<JobPair> getPairsSimple (int jobId) {
		Connection con = null;			
		try {			
			con = Common.getConnection();			
			return getPairsSimple(con,jobId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}
	
	
	/**
	 * Gets all job pairs for the given job non-recursively
	 * (Worker node, benchmark and solver will NOT be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen
	 */
	protected static List<JobPair> getPairs(Connection con, int jobId) throws Exception {			
		CallableStatement procedure = null;
		ResultSet results = null;
		
		 try {
			procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				jp.getNode().setId(results.getInt("node_id"));
				jp.getStatus().setCode(results.getInt("status_code"));
				jp.getBench().setId(results.getInt("bench_id"));
				jp.getPrimarySolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
				returnList.add(jp);
			}			
			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			log.error("getPairs says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			
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
	public static List<JobPair> getPairs (int jobId) {
		Connection con = null;			
		try {			
			con = Common.getConnection();			
			return getPairs(con,jobId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	/**
	 * Gets all job pairs for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) Only the primary
	 * stage is populated
	 * @param jobId The id of the job to get pairs for
	 * @param since The completion ID to get all the pairs after. If null, gets all pairs
	 * @return A list of job pair objects that belong to the given job.
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getPairsPrimaryStageDetailed(int jobId) {
		Connection con = null;	
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	
			
			log.info("getting detailed pairs for job " + jobId );
			
			procedure = con.prepareCall("{CALL GetJobPairsByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<JobPair> pairs= getPairsDetailed(jobId,con,results,false);
			HashMap<Integer,Properties> props=Jobs.getJobAttributes(con,jobId);
			for (Integer i =0; i < pairs.size(); i++){
				JobPair jp = pairs.get(i);
				if (props.containsKey(jp.getId())) {
					log.debug("got attributes for a pair");
					jp.setAttributes(props.get(jp.getId()));
				} else {
					log.debug("forced to get attributes for a single pair");
					jp.setAttributes(JobPairs.getAttributes(jp.getId()));
				}
			}
			return pairs;
		} catch (Exception e) {
			log.error("Get Pairs Detailed says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets either all job pairs for the given job and also populates its used resource TOs or
	 * only the job pairs that have been completed after the argument "since"
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @param since The completion ID to get all the pairs after. If null, gets all pairs
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen, Benton Mccune, Eric Burns
	 */
	private static List<JobPair> getPairsDetailed(int jobId, Connection con,ResultSet results, boolean getCompletionId) {
		log.debug("starting the getPairsDetailed function");
		try {			
			List<JobPair> returnList = new ArrayList<JobPair>();
			
			//instead of setting up the solvers, configs, etc. every time, we just set them
			//up once and then save them
			Hashtable<Integer,Solver> discoveredSolvers=new Hashtable<Integer,Solver>();
			Hashtable<Integer,Configuration> discoveredConfigs=new Hashtable<Integer,Configuration>();
			Hashtable<Integer,Benchmark> discoveredBenchmarks=new Hashtable<Integer,Benchmark>();
			Hashtable <Integer, WorkerNode> discoveredNodes = new Hashtable<Integer, WorkerNode>();
			int curNode,curBench,curConfig, curSolver;
			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				
				Status s = new Status();
				s.setCode(results.getInt("status_code"));
				
				jp.setStatus(s);
				
				//set the completion ID if it exists-- it only exists if we are getting new job pairs
				if (getCompletionId) {
					jp.setCompletionId(results.getInt("complete.completion_id"));
				}
				jp.setJobSpaceName(results.getString("jobSpace.name"));
				returnList.add(jp);
				curNode=results.getInt("node_id");
				curBench=results.getInt("bench_id");
				curConfig=results.getInt("config_id");
				curSolver=results.getInt("config.solver_id");
				JoblineStage stage=JobPairs.resultToStage(results);
				if (!discoveredSolvers.containsKey(curSolver)) {
					Solver solver= Solvers.resultToSolver(results,"solver");				
					stage.setSolver(solver);
					discoveredSolvers.put(curSolver, solver);
				}
				stage.setSolver(discoveredSolvers.get(curSolver));
				
				if (!discoveredBenchmarks.containsKey(curBench)) {
					Benchmark b= Benchmarks.resultToBenchmark(results, "bench");
					jp.setBench(b);
					discoveredBenchmarks.put(curBench,b);
				}
				jp.setBench(discoveredBenchmarks.get(curBench));

				if (!discoveredConfigs.containsKey(curConfig)) {
					Configuration c=new Configuration();
					c.setId(results.getInt("config.id"));			
					c.setName(results.getString("config.name"));			
					c.setSolverId(results.getInt("config.solver_id"));
					c.setDescription(results.getString("config.description"));
					discoveredConfigs.put(curConfig,c);
				}
				stage.setConfiguration(discoveredConfigs.get(curConfig));
				stage.getSolver().addConfiguration(discoveredConfigs.get(curConfig));
				if (!discoveredNodes.containsKey(curNode)) {
					WorkerNode node=new WorkerNode();
					node.setName(results.getString("node.name"));
					node.setId(results.getInt("node.id"));
					node.setStatus(results.getString("node.status"));
					discoveredNodes.put(curNode,node);
				}
				jp.addStage(stage);
				jp.setNode(discoveredNodes.get(curNode));
			}
			log.info("returning "+ returnList.size()+ " detailed pairs for job " + jobId );
			return returnList;	
			
		} catch (Exception e){			
			log.error("getPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		}

		return null;		
	}
	
	
	
	/**
	 * Counts the pairs that would be rerun if the user decided to rerun all timeless pairs
	 * @param jobId The id of the job to count for
	 * @return The count on success or -1 on failure
	 */
	
	public static int countTimelessPairs(int jobId) {
		int c1 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.STATUS_COMPLETE.getVal());
		int c2 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_CPU.getVal());
		int c3 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_FILE_WRITE.getVal());
		int c4 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_MEM.getVal());
		int c5 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_RUNTIME.getVal());
		 
		//on failure
		if (c1==-1 || c2==-1 || c3==-1 || c4==-1 || c5==-5) {
			return -1;
		}
		
		return c1+c2+c3+c4+c5;
	}
	
	/**
	 * Sets job pairs with wallclock time 0 back to pending. Only pairs that are 
	 * complete or had a resource out are reset
	 * @param jobId
	 * @return True on success and false otherwise
	 */
	
	public static boolean setTimelessPairsToPending(int jobId) {
		try {
			boolean success=true;
			//only continue if we could actually clear the job stats
			Set<Integer> ids=new HashSet<Integer>();
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId,StatusCode.STATUS_COMPLETE.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId,StatusCode.EXCEED_CPU.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId,StatusCode.EXCEED_FILE_WRITE.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId,StatusCode.EXCEED_MEM.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId,StatusCode.EXCEED_RUNTIME.getVal()));
			
			
			for (Integer jp : ids) {
				success=success && Jobs.rerunPair(jobId, jp);
			}

			return success;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false;
		
	}
	
	/**
	 * Sets every pair in a job back to pending, allowing all pairs to be rerun
	 * @param jobId
	 * @return
	 */
	public static boolean setAllPairsToPending(int jobId) {

		try {
			Job j=Jobs.getDetailed(jobId);
			boolean success=true;
			for (JobPair jp : j) {
				success = success && Jobs.rerunPair(jobId, jp.getId());
			}
			return success;
		} catch (Exception e) {
			log.error("setTimelessPairsToPending says "+e.getMessage(),e);
		} 
		return false;
	}
	
	/**
	 * Begins the process of rerunning a single pair by removing it from the completed table (if applicable)
	 * killing it (also if applicable), and setting it back to pending
	 * @param jobId
	 * @param pairId
	 * @return
	 */
	
	public static boolean rerunPair(int jobId, int pairId) {
		try {
			log.debug("got a request to rerun pair id = "+pairId);
			boolean success=true;
			JobPair p=JobPairs.getPair(pairId);
			Status status=p.getStatus();
			//no rerunning for pairs that are still pending
			if (status.getCode().getVal()==StatusCode.STATUS_PENDING_SUBMIT.getVal()) {
				return true;
			}
			if (status.getCode().getVal()<StatusCode.STATUS_COMPLETE.getVal()) {
				JobPairs.killPair(pairId, p.getGridEngineId());
			}
			JobPairs.removePairFromCompletedTable(pairId);
			JobPairs.setPairStatus(pairId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
			
			// the cache must be cleared AFTER changing the pair status code!
			success=success && Jobs.removeCachedJobStats(jobId);
			
			return success;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return false;
	
	}
	
	
	
	public static boolean removePairsOfStatusFromComplete(int jobId, int statusCode, Connection con) {
		CallableStatement procedure=null;

		try {
			procedure=con.prepareCall("{CALL RemovePairsOfStatusFromComplete(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			procedure.executeUpdate();
			return true;
		} catch (Exception e ) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	/**
	 * Returns all job pairs in the given job with the given status code that have a run time of 0 for
	 * any stage
	 * @param jobId the ID of the job to get pairs for
	 * @param statusCode The status code of pairs to search for
	 * @return A list of job pair IDs, where each pair has at least one stage with a run time of 0
	 * and also has the given status.
	 */
	
	public static List<Integer> getTimelessPairsByStatus(int jobId, int statusCode) {
		Connection con=null;
		
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetTimelessJobPairsByStatus(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			results=procedure.executeQuery();
			List<Integer> ids=new ArrayList<Integer>();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			
			return ids;
		} catch (Exception e ) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	public static List<Integer> getPairsByStatus(int jobId, int statusCode) {
		Connection con=null;
		
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetJobPairsByStatus(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			results=procedure.executeQuery();
			List<Integer> ids=new ArrayList<Integer>();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			
			return ids;
		} catch (Exception e ) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	
	/**
	 * Sets all the job pairs of a given status code and job to pending. Used to rerun
	 * pairs that didn't work in an initial job run
	 * @param jobId The id of the job in question
	 * @param statusCode The status code of pairs that should be rerun
	 * @return true on success and false otherwise
	 * @author Eric Burns
	 */
	public static boolean setPairsToPending(int jobId, int statusCode) {
		
		try {
			boolean success=true;
			List<Integer> pairs=Jobs.getPairsByStatus(jobId, statusCode);
			for (Integer id : pairs) {
				success=success && Jobs.rerunPair(jobId, id);
			}
			return success;
		} catch (Exception e) {
			log.error("setPairsToPending says "+e.getMessage(),e);
		} 
		return false;
	} 
		
	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author TBebnton
	 */
	//TODO: right now, this works only for the primary stage. Needs to be updated to get back all stages
    protected static List<JobPair> getPendingPairsDetailed(Connection con, int jobId,int limit) throws Exception {	

	CallableStatement procedure = null;
	ResultSet results = null;
	try {
	    procedure = con.prepareCall("{CALL GetPendingJobPairsByJob(?,?)}");
	    procedure.setInt(1, jobId);				
	    procedure.setInt(2,limit);
	    results = procedure.executeQuery();
	    List<JobPair> returnList = new LinkedList<JobPair>();
	    //we map ID's to  primitives so we don't need to query the database repeatedly for them
	    HashMap<Integer, Solver> solvers=new HashMap<Integer,Solver>();
	    HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
	    HashMap<Integer,Benchmark> benchmarks=new HashMap<Integer,Benchmark>();
	    while(results.next()){
				
		try {

		    JobPair jp = JobPairs.resultToPair(results);
			JoblineStage stage=new JoblineStage();
			stage.setId(jp.getPrimaryStageId());
			jp.addStage(stage);
		    //we need to check to see if the benchId and configId are null, since they might
		    //have been deleted while the the job is still pending
		    Integer benchId=results.getInt("bench_id");
		    if (benchId!=null) {
				if (!benchmarks.containsKey(benchId)) {
				    benchmarks.put(benchId,Benchmarks.get(benchId));
				}
							
				jp.setBench(benchmarks.get(benchId));
		    }
		    Integer configId=results.getInt("config_id");
		    String configName=results.getString("config_name");
		    Configuration c=new Configuration();
		    c.setId(configId);
		    c.setName(configName);
		    stage.setConfiguration(c);

		    if (configId!=null) {
				if (!configs.containsKey(configId)) {
				    Solver s = Solvers.getSolverByConfig(configId, false);
				    if (s != null) {
					solvers.put(configId, s);
					s.addConfiguration(c);
				    }
				}
				stage.setSolver(solvers.get(configId) /* could be null, if Solver s above was null */);
		    }

		    Status s = new Status();
		    s.setCode(results.getInt("status_code"));
		    jp.setStatus(s);
		    returnList.add(jp);
		} 
		catch (Exception e) {
		    log.error("there was an error making a single job pair object");
		    log.error(e.getMessage(),e);
		}
	    }
				
	    Common.safeClose(results);
	    return returnList;
	} catch (Exception e) {
	    log.error("getPendingPairsDetailed says "+e.getMessage(),e);
	} finally {
	    Common.safeClose(results);
	    Common.safeClose(procedure);
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
	public static List<JobPair> getPendingPairsDetailed(int jobId, int limit) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getPendingPairsDetailed(con, jobId,limit);
		} catch (Exception e){			
			log.error("getPendingPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}
		
	
	/**
	 * Gets all job pairs that are  running for the given job
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getRunningPairs(Connection con, int jobId) throws Exception {	

		CallableStatement procedure = null;
		ResultSet results = null;
		 try {
			procedure = con.prepareCall("{CALL GetRunningJobPairsByJob(?)}");
			procedure.setInt(1, jobId);					
			results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();
			//we map ID's to  primitives so we don't need to query the database repeatedly for them
			//HashMap<Integer, Solver> solvers=new HashMap<Integer,Solver>();
			//HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
			HashMap<Integer,WorkerNode> nodes=new HashMap<Integer,WorkerNode>();
			HashMap<Integer,Benchmark> benchmarks=new HashMap<Integer,Benchmark>();
			
			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				int nodeId=results.getInt("node_id");
				if (!nodes.containsKey(nodeId)) {
					nodes.put(nodeId,Cluster.getNodeDetails(nodeId));
				}
				jp.setNode(nodes.get(nodeId));	
				int benchId=results.getInt("bench_id");
				if (!benchmarks.containsKey(benchId)) {
					benchmarks.put(benchId,Benchmarks.get(benchId));
				}
				/*jp.setBench(benchmarks.get(benchId));
				int configId=results.getInt("configId");
				if (!configs.containsKey(configId)) {
					configs.put(configId, Solvers.getConfiguration(configId));
					solvers.put(configId, Solvers.getSolverByConfig(configId,false));
				}
				jp.setSolver(solvers.get(configId));
				jp.setConfiguration(configs.get(configId));
				*/
				
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				returnList.add(jp);
			}			

			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			log.error("getRunningPairs says " + e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	
	/**
	 * Gets all job pairs that are running for the given job 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that are running.
	 * @author Wyatt Kaiser
	 */
	public static List<JobPair> getRunningPairs(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();	
			return Jobs.getRunningPairs(con, jobId);
		} catch (Exception e){			
			log.error("getRunningPairsDetailed for queue " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}

	
	
	
	
	/**
	 * Invalidates every type of cache related to this job (useful when deleting a job)
	 * Includes job output, job CSV with and without IDs, and pair output
	 * @param jobId The ID of the job for which to delete all associated cache entreis
	 */
	public static void invalidateAndDeleteJobRelatedCaches(int jobId) {
		//Cache.invalidateAndDeleteCache(jobId, CacheType.CACHE_JOB_OUTPUT);
		//Cache.invalidateAndDeleteCache(jobId, CacheType.CACHE_JOB_CSV);
		//Cache.invalidateAndDeleteCache(jobId, CacheType.CACHE_JOB_CSV_NO_IDS);
		//List<JobPair> pairs = Jobs.getPairs(jobId);
		//for (JobPair pair : pairs) {
			//Cache.invalidateAndDeleteCache(pair.getId(), CacheType.CACHE_JOB_PAIR);
		//}
	}
	
	
	
	
	/**
	 * Returns the count of pairs with the given status code in the given job where either
	 * cpu or wallclock is 0
	 * @param jobId
	 * @param statusCode
	 * @return The count or -1 on failure
	 */
	private static int countTimelessPairsByStatus(int jobId, int statusCode) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL CountTimelessPairsByStatusByJob(?,?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2,statusCode);
			results=procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("countTimelessPairsByStatus says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return -1;
	}
	
	/**
	 * Returns the count of pairs with the given status code in the given job
	 * @param jobId
	 * @param statusCode
	 * @return The count or -1 on failure
	 */
	public static int countPairsByStatus(int jobId, int statusCode) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL CountPairsByStatusByJob(?,?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2,statusCode);
			results=procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("countPairsByStatus says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return -1;
	}
	
	
	public static int CountProcessingPairsByJob(int jobId) {
		return countPairsByStatus(jobId,StatusCode.STATUS_PROCESSING.getVal());
	
	}
	/**
	 * Returns whether the given job has any pairs that are currently waiting
	 * to be re post processed.
	 * @param jobId The ID of the job ot check
	 * @return True / false as expected, and null on error 
	 */
	public static Boolean hasProcessingPairs(int jobId) {
		int count=CountProcessingPairsByJob(jobId);
		if (count<0) {
			return null;
		}
		return count>0;
	}
	
	/**
	 * Determines whether the given job is in a good state to be post-processed
	 * @param jobId The ID of the job to check
	 * @return True if the job can be processed, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean canJobBePostProcessed(int jobId) {
		JobStatus status=getJobStatusCode(jobId);
		if (status.getCode()==JobStatusCode.STATUS_COMPLETE) {
			return true;
		}
		return false;
	}
	
	/**
	 * Counts the number of pairs a job has that are not complete (status between 1 and 6)
	 * @param jobId
	 * @return
	 */
	
	public static int countIncompletePairs(int jobId) {
		return Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_ENQUEUED.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PREPARING.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_FINISHING.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_RUNNING.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_WAIT_RESULTS.getVal());

	}
	
	/**
	 * Returns the number of job pairs that are pending for the current job
	 * @param jobId The ID of the job in question
	 * @return The integer number of pending pairs. -1 is returned on error
	 */
	public static int countPendingPairs(int jobId) {
		return Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
	}
	
	public static JobStatus getJobStatusCode(int jobId) {
		JobStatus status=new JobStatus();
		
		try {
			int a=Jobs.isJobPausedOrKilled(jobId);
			if (a==1) {
				status.setCode(JobStatusCode.STATUS_PAUSED);
				return status;
			} else if(a==2) {
				status.setCode(JobStatusCode.STATUS_KILLED);
				return status;
			}
		
			if (hasProcessingPairs(jobId)) {
				status.setCode(JobStatusCode.STATUS_PROCESSING);
				return status;
			}

			//if the job is not paused and no pending pairs remain, it is done
			if (countIncompletePairs(jobId)==0) {
				status.setCode(JobStatusCode.STATUS_COMPLETE);
				return status;
			} 
			status.setCode(JobStatusCode.STATUS_RUNNING);
			return status;
		} catch (Exception e) {
			log.error("getJobStatusCode says "+e.getMessage(),e);
		}
		return status;
	}
	
	/**
	 * Determines whether the job with the given ID is complete
	 * @param jobId The ID of the job in question 
	 * @return True if the job is complete, false otherwise (includes the possibility of error)
	 * @author Eric Burns
	 */
	
	public static boolean isJobComplete(int jobId) {
		return getJobStatusCode(jobId).getCode()==JobStatusCode.STATUS_COMPLETE;
	}
	
	/**
	 * Checks whether the given job is set to "deleted" in the database
	 * @param con The open connection to make the call on 
	 * @param jobId The ID of the job in question
	 * @return True if the job exists in the database with the deleted flag set to true, false otherwise
	 * @throws Exception 
	 * @author Eric Burns
	 */
	
	public static boolean isJobDeleted(Connection con, int jobId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		
		 try {
			procedure = con.prepareCall("{CALL IsJobDeleted(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			boolean deleted=false;
			if (results.next()) {
				deleted=results.getBoolean("jobDeleted");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isJobDeleted says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return false;
	}

	/** 
	 * Determines whether the job with the given ID exists in the database with the column "deleted" set to true
	 * @param jobId The ID of the job in question
	 * @return True if the job exists in the database and has the deleted flag set to true
	 * @author Eric Burns
	 */
	
	public static boolean isJobDeleted(int jobId) {
		Connection con=null;
		
		try {
			con=Common.getConnection();
			
			return isJobDeleted(con,jobId);
		} catch (Exception e) {
			log.error("isJobDeleted says " +e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/** 
	 * Determines whether the job with the given ID exists in the database with the column "killed" set to true
	 * @param jobId The ID of the job in question
	 * @return True if the job is killed (i.e. the killed flag is set to true), false otherwise
	 * @author Wyatt Kaiser
	 */
	
	public static boolean isJobKilled(int jobId) {
		return isJobPausedOrKilled(jobId)==2;
	}
	
	/** 
	 * Determines whether the job with the given ID exists in the database with the column "paused" set to true
	 * @param jobId The ID of the job in question
	 * @return True if the job is paused (i.e. the paused flag is set to true), false otherwise
	 * @author Wyatt Kaiser
	 */
	
	public static boolean isJobPaused(int jobId) {
		return (isJobPausedOrKilled(jobId)==1 || isJobPausedOrKilled(jobId)==3);
	}

	/**
	 * Determines whether the given job is either paused, admin paused, or killed
	 * @param con The open connection to make the query on 
	 * @param jobId The ID of the job in question
	 * @return
	 * 0 if the job is neither paused nor killed
	 * 1 if the job is paused
	 * 2 if the job has been killed
	 * 3 if the job has been admin paused
	 * @author Eric Burns
	 */
	public static int isJobPausedOrKilled(Connection con, int jobId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			 procedure = con.prepareCall("{CALL IsJobPausedOrKilled(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			boolean paused=false;
			boolean killed=false;
			if (results.next()) {
				paused=results.getBoolean("paused");
				if (paused) {
					return 1;
				}
				killed=results.getBoolean("killed");
				if (killed) {
					return 2;
				}				
			}
			return 0;
		} catch (Exception e) {
			log.error("isJobPaused says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}
	
	/** 
	 * Determines whether the job with the given ID has either the paused or killed column set to ttrue
	 * @param jobId The ID of the job in question
	 * @return
	 * 0 if the job is neither paused nor killed (or error)
	 * 1 if the job is paused (i.e. the paused flag is set to true),
	 * 2 if the job is killed
	 * @author Eric Burns
	 */
	
	public static int isJobPausedOrKilled(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			
			return isJobPausedOrKilled(con,jobId);
		} catch (Exception e) {
			log.error("isJobPausedOrKilled says " +e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return 0;
	}
	
	/**
	 * Returns whether the job is public. A job is public if it was run by the public user or
	 * if it is in any public space
	 * @param jobId The ID of the job in question
	 * @return True if the job is public, and false if not or there was an error
	 */
    

	public static boolean isPublic(int jobId) {
		
		Job j = Jobs.get(jobId);
		if (j==null) {
			return false;
		}
		//if the public user made a job, then that job must be public
		if (Users.isPublicUser(j.getUserId())){
			log.debug("Public User for Job Id" + jobId);
			return true;
		}
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL JobInPublicSpace(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			int count = 0;
			if (results.next()){
				count = (results.getInt("spaceCount"));
			}			
			
			if (count > 0){
				return true;
			}

		} catch (Exception e){			
			log.error("isPublic says" + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}
	
	/**
	 * kills a running/paused job, and also sets the killed property to true in the database. 
	 * @param jobId The ID of the job to kill
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean kill(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return kill(jobId,con);
		} catch (Exception e) {
			log.error("Jobs.kill says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}


	/**
	 * kills a running/paused job, and also sets the killed property to true in the database. 
	 * @param jobId The ID of the job to kill
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	protected static boolean kill(int jobId, Connection con) {
		CallableStatement procedure = null;
		try {
			 procedure = con.prepareCall("{CALL KillJob(?)}");
			procedure.setInt(1, jobId);		
			procedure.executeUpdate();	

			log.debug("Killing of job id = " + jobId + " was successful");
			
			List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(jobId);
			for (JobPair jp : jobPairsEnqueued) {
				JobPairs.killPair(jp.getId(), jp.getGridEngineId());
			}
			
			log.debug("deletion of killed job pairs from the queue was successful");
			return true;
		} catch (Exception e) {
			log.error("Kill Job says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Pauses a job, and also sets the paused property to true in the database. 
	 * @param jobId The ID of the job to pause
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean pause(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return pause(jobId,con);
		} catch (Exception e) {
			log.error("Jobs.pause says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * pauses a running job, and also sets the paused to true in the database. 
	 * @param jobId The ID of the job to pause
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	
    protected static boolean pause(int jobId, Connection con) {
	log.info("Pausing job "+new Integer(jobId));
	CallableStatement procedure = null;
	try {
	    procedure = con.prepareCall("{CALL PauseJob(?)}");
	    procedure.setInt(1, jobId);		
	    procedure.executeUpdate();	

	    log.debug("Pausation of job id = " + jobId + " was successful");
			
	    //Get the enqueued job pairs and remove them
	    List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(jobId);
	    for (JobPair jp : jobPairsEnqueued) {
		//TODO : remember to change name of getGridEngineId
		int execId = jp.getGridEngineId();
		R.BACKEND.killPair(R.SGE_ROOT,execId);
		JobPairs.UpdateStatus(jp.getId(), 20);
	    }
	    //Get the running job pairs and remove them
	    List<JobPair> jobPairsRunning = Jobs.getRunningPairs(jobId);
	    if (jobPairsRunning != null) {
		for (JobPair jp: jobPairsRunning) {
		    int execId = jp.getGridEngineId();
		    R.BACKEND.killPair(R.SGE_ROOT,execId);
		    JobPairs.UpdateStatus(jp.getId(), 20);
		}
	    }
	    log.debug("Deletion of paused job pairs from queue was succesful");
	    return true;
	} catch (Exception e) {
	    log.error("Pause Job says "+e.getMessage(),e);
	} finally {
	    Common.safeClose(procedure);
	}
	return false;
    }
	
	public static boolean isTestJob(int jobId) {
		return Users.isTestUser(Jobs.get(jobId).getUserId());
	}

	/**
	 * pauses all running jobs (via admin page), and also sets the paused & paused_admin to true in the database. 
	 * @param jobs The jobs to pause
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	
	public static boolean pauseAll() {
		Connection con = null;
		CallableStatement procedure = null;
		log.info("Pausing all jobs");
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL PauseAll()}");
			procedure.executeUpdate();
			log.debug("Pausation of system was successful");
			R.BACKEND.killAll(R.SGE_ROOT);
			List<Job> jobs = new LinkedList<Job>();		
			jobs = Jobs.getRunningJobs();
			if (jobs != null) {
				for (Job j : jobs) {
					//Get the enqueued job pairs and remove them
					List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(j.getId());
					if (jobPairsEnqueued != null) {
						for (JobPair jp : jobPairsEnqueued) {
							JobPairs.UpdateStatus(jp.getId(), 1);
						}
					}
					//Get the running job pairs and remove them
					List<JobPair> jobPairsRunning = Jobs.getRunningPairs(j.getId());
					log.debug("JPR = " + jobPairsRunning);
					if (jobPairsRunning != null) {
						for (JobPair jp: jobPairsRunning) {
							JobPairs.UpdateStatus(jp.getId(), 1);
						}
					}
					log.debug("Deletion of paused job pairs from queue was succesful");
				}
			}
					
		
			return true;
		} catch (Exception e) {
			log.error("PauseAll Jobs says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	public static boolean changeQueue(int jobId, int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL ChangeQueue(?, ?)}");
			procedure.setInt(1, jobId);	
			procedure.setInt(2, queueId);
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error("ChangeQueue says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	

	/**
	 * Given a resultset containing the results of a query for job pair attrs,
	 * returns a hashmap mapping job pair ids to attributes
	 * @param results The ResultSet containing the attrs
	 * @return A mapping from pair ids to Properties
	 * @author Eric Burns
	 */
	private static HashMap<Integer,Properties> processAttrResults(ResultSet results) {
		try {
			HashMap<Integer,Properties> props=new HashMap<Integer,Properties>();
			int id;
			
			while(results.next()){
				id=results.getInt("pair.id");
				if (!props.containsKey(id)) {
					props.put(id,new Properties());
				}
				String key=results.getString("attr.attr_key");
				String value=results.getString("attr.attr_value");
				if (key!=null && value!=null) {
					props.get(id).put(key, value);	

				}
			}			
			return props;
		} catch (Exception e) {
			log.error("processAttrResults says "+e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * Given a list of JobPairs, compiles them into SolverStats objects
	 * @param pairs The JobPairs with their relevant fields populated
	 * @param startingRecord The record to start at (currently unused)
	 * @param recordsPerPage The number of records to include (currently unused)
	 * @param isSortedASC Whether to sort ASC or DESC (currently unused)
	 * @param indexOfColumnSortedBy The index of the datatables column to sort on (currently unused)
	 * @param searchQuery The query to filter the results on (currently unused)
	 * @param jobId The ID of the job to gets stats for
	 * @param total A size 1 Integer array that will contain the total number of stats records
	 * when this function returns
	 * @return A list of SolverStats objects to use in a datatable
	 * @author Eric Burns
	 */
	//TODO: This currently only works on the primary stage. Should stats be per stage? Should some stages be excluded?
	public static List<SolverStats> processPairsToSolverStats(List<JobPair> pairs) {
		Hashtable<String, SolverStats> SolverStats=new Hashtable<String,SolverStats>();
		String key=null;
		for (JobPair jp : pairs) {
			
			//entries in the stats table determined by solver/configuration pairs
			key=String.valueOf(jp.getPrimarySolver().getId())+":"+String.valueOf(jp.getPrimaryConfiguration().getId());
			
			if (!SolverStats.containsKey(key)) { // current stats entry does not yet exist
				SolverStats newSolver=new SolverStats();
				
				newSolver.setSolver(jp.getPrimarySolver());
				newSolver.setConfiguration(jp.getPrimaryConfiguration());
				SolverStats.put(key, newSolver);
			}
			
			
			//update stats info for entry that current job-pair belongs to
			SolverStats curSolver=SolverStats.get(key);
			StatusCode statusCode=jp.getStatus().getCode();
			
			if ( statusCode.failed()) {
			    curSolver.incrementFailedJobPairs();
			} 
			if ( statusCode.resource()) {
				curSolver.incrementResourceOutPairs();
			}
			if (statusCode.incomplete()) {
			    curSolver.incrementIncompleteJobPairs();
			}
			if (statusCode.complete()) {
			    curSolver.incrementCompleteJobPairs();
			}
			
			int correct=JobPairs.isPairCorrect(jp);
			if (correct==0) {
				log.debug("found wallclock time = "+jp.getPrimaryWallclockTime());
				log.debug(jp.getStages().size());
				curSolver.incrementWallTime(jp.getPrimaryWallclockTime());
    			curSolver.incrementCpuTime(jp.getPrimaryCpuTime());
    			curSolver.incrementCorrectJobPairs();
			} else if (correct==1) {
	   			curSolver.incrementIncorrectJobPairs();

			}
			
			
			   
		}
		
		List<SolverStats> returnValues=new LinkedList<SolverStats>();
		for (SolverStats js : SolverStats.values()) {
			returnValues.add(js);
		}
		return returnValues;
	}
	
	/**
	 * Given the result set from a SQL query containing job pair info, produces a list of job pairs
	 * for which all the necessary fields for solver stat production have been created
	 * @param results A resultset containing SQL data
	 * @param jobId The ID of the job in question
	 * @param con The connection to request pair attributes on
	 * @return A list of job pairs
	 * @throws Exception
	 * @author Eric Burns
	 */
	
	private static List<JobPair> processStatResults(ResultSet results) throws Exception {
		List<JobPair> returnList = new ArrayList<JobPair>();
		
		Benchmark bench=null;
		while(results.next()){
			JobPair jp = new JobPair();
			
			// these are the solver and configuration defaults. If any jobpair_stage_data
			// entry has null for a stage_id, then these are the correct primitives. 
						
			Solver solve=new Solver();
			solve.setId(results.getInt("solver_id"));
			solve.setName(results.getString("solver_name"));
						
			Configuration c=new Configuration();
			c.setId(results.getInt("config_id"));
			c.setName(results.getString("config_name"));
			jp.setDefaultConfiguration(c);
			jp.setDefaultSolver(solve);
			Status s = new Status();

			s.setCode(results.getInt("status_code"));
			jp.setStatus(s);
			jp.setId(results.getInt("job_pairs.id"));
			bench=new Benchmark();
			bench.setId(results.getInt("bench_id"));
			jp.setBench(bench);
			
			Properties p=new Properties();
			String result=results.getString("result");
			if (result!=null) {
				p.put(R.STAREXEC_RESULT, result);
			}
			String expected=results.getString("expected");
			if (expected!=null) {
				p.put(R.EXPECTED_RESULT, expected);

			}
			jp.setAttributes(p);
						
			returnList.add(jp);		
			log.debug("here with id = "+jp.getId());
			
		}

		return returnList;	
	}

	/**
	 * Resumes a paused job, and also sets the paused property to false in the database. 
	 * @param jobId The ID of the job to resume
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean resume(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return resume(jobId,con);
		} catch (Exception e) {
			log.error("Jobs.resume says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Resumes a paused job 
	 * @param jobId The ID of the paused job
	 * @param con The open connection to make the call on
	 * @return true on success, false otherwise
	 * 
	 */
	
	protected static boolean resume(int jobId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL ResumeJob(?)}");
			procedure.setInt(1, jobId);		
			procedure.executeUpdate();	

			log.debug("Resume of job id = " + jobId + " was successful");
			return true;
		} catch (Exception e) {
			log.error("Resume Job says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * resumeAll sets global pause to false, which allows job pairs to be sent to the grid engine again
	 * @author Wyatt Kaiser
	 */
	public static boolean resumeAll() {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL ResumeAll()}");
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error("ResumeAll says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	

	/**
	 * Sets the given job up to be post processed by adding all of its pairs
	 * to the processing_job_pairs table
	 * @param jobId The ID of the the job to process
	 * @param processorId The ID of the post-processor to use
	 * @return True if the operation was successful, false otherwise.
	 * @author Eric Burns
	 */
	public static boolean prepareJobForPostProcessing(int jobId, int processorId) {
		if (!Jobs.canJobBePostProcessed(jobId)){
			return false;
		}
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			Common.beginTransaction(con);
			if (!Jobs.removeCachedJobStats(jobId,con)) {
				throw new Exception("Couldn't clear out the cache of job stats");
			}
			if (!Jobs.removePairsOfStatusFromComplete(jobId, StatusCode.STATUS_COMPLETE.getVal(), con)){
				throw new Exception("Couldn't remove pairs from the complete table");

			}

			procedure=con.prepareCall("{CALL PrepareJobForPostProcessing(?,?,?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,processorId);
			procedure.setInt(3,StatusCode.STATUS_COMPLETE.getVal());
			procedure.setInt(4, StatusCode.STATUS_PROCESSING.getVal());
			procedure.executeUpdate();
			return true;
			
		} catch (Exception e) {
			Common.doRollback(con);
			log.error("runPostProcessor says "+e.getMessage(),e);
		} finally {
			Common.endTransaction(con);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * If the job is not yet complete, does nothing, as we don't want to store stats for incomplete jobs.
	 * @param jobId The ID of the job we are storing stats for
	 * @param jobSpaceId The ID of the job space that the stats are rooted at
	 * @param stats The stats, which should have been compiled already
	 * @return True if the call was successful, false otherwise
	 * @author Eric Burns
	 */	
	public static boolean saveStats(int jobId, int jobSpaceId,List<SolverStats> stats) {
		
		if (!isJobComplete(jobId)) {
			log.debug("stats for job with id = "+jobId+" were not saved because the job is incomplete");
			return false; //don't save stats if the job is not complete
		}
		Connection con=null;
		try {
			con=Common.getConnection();
			Common.beginTransaction(con);
			for (SolverStats s : stats) {
				if (!saveStats(jobSpaceId,s,con)) {
					throw new Exception ("saving stats failed, rolling back connection");
				}
			}
			return true;
		} catch (Exception e) {
			log.error("saveStats says "+e.getMessage(),e);
			Common.doRollback(con);

		} finally {
			Common.endTransaction(con);
			Common.safeClose(con);
		}
		
		return false;
	}

	/**
	 * Given a SolverStats object, saves it in the database so that it does not need to be generated again
	 * This function is currently called only when the job is complete, as we do not want to cache stats
	 * for incomplete jobs. 
	 * @param jobSpaceId The ID of the job space that this stats object was accumulated for
	 * @param stats The stats object to save
	 * @param con The open connection to make the update on
	 * @return True if the save was successful, false otherwise
	 * @author Eric Burns
	 */
	
	private static boolean saveStats(int jobSpaceId, SolverStats stats, Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL AddJobStats(?,?,?,?,?,?,?,?,?,?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,stats.getConfiguration().getId());
			procedure.setInt(3,stats.getCompleteJobPairs());
			procedure.setInt(4,stats.getCorrectJobPairs());
			procedure.setInt(5,stats.getIncorrectJobPairs());
			procedure.setInt(6,stats.getFailedJobPairs());
			procedure.setDouble(7,stats.getWallTime());
			procedure.setDouble(8,stats.getCpuTime());
			procedure.setInt(9,stats.getResourceOutJobPairs());
			procedure.setInt(10, stats.getIncompleteJobPairs());
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("saveStats says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Sets all the pairs associated with the given job to the given status code
	 * @param jobId The ID of the job in question
	 * @param statusCode The status code to set all the pairs to
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	
	private static boolean setPairStatusByJob(int jobId, int statusCode, Connection con) {
		log.debug("setting pairs to status "+statusCode);
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL SetPairsToStatus(?,?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2,statusCode);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("setPairStatusByJob says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	/**
	 * This function makes older jobs, which have path info but no job space information
	 * @param jobId The jobId to create the job space info for
	 * @return The ID of the primary job  space of the job
	 * @author Eric Burns
	 */
	
	public static int setupJobSpaces(int jobId) {
		
		try {
			log.debug("Setting up job space hierarchy for old job id = "+jobId);
			List<JobPair> p=Jobs.getPairsPrimaryStageDetailed(jobId);
			Integer primarySpaceId=null;
			HashMap<String,Integer> namesToIds=new HashMap<String,Integer>();
			for (JobPair jp : p) {
				String pathString=jp.getPath();
				if (pathString==null) {
					pathString="job space";
				}
				String[] path=pathString.split(R.JOB_PAIR_PATH_DELIMITER);
				String key="";
				for (int index=0;index<path.length; index++) {
					
					String spaceName=path[index];
					key=key+R.JOB_PAIR_PATH_DELIMITER+spaceName;
					if (namesToIds.containsKey(key)) {
						if (index==(path.length-1)) {
							jp.setJobSpaceId(namesToIds.get(key));
						}
						continue; //means we've already added this space
					}
					
					int newJobSpaceId=Spaces.addJobSpace(spaceName);
					if (index==0) {
						primarySpaceId=newJobSpaceId;
					}
					//otherwise, there was an error getting the new job space
					if (newJobSpaceId>0) {
						namesToIds.put(key, newJobSpaceId);
						if (index==(path.length-1)) {
							jp.setJobSpaceId(newJobSpaceId);
						}
						String parentKey=key.substring(0,key.lastIndexOf(R.JOB_PAIR_PATH_DELIMITER));
						if (namesToIds.containsKey(parentKey)) {
							Spaces.associateJobSpaces(namesToIds.get(parentKey), namesToIds.get(key));
						}
					}
				}
			}
			//there seem to be some jobs with no pairs somehow, so this just prevents
			//a red error screen when viewing them
			if (p.size()==0) {
				primarySpaceId=Spaces.addJobSpace("job space");
				Spaces.updateJobSpaceClosureTable(primarySpaceId);
			}
			
			for (int id : namesToIds.values()) {
				Spaces.updateJobSpaceClosureTable(id);
			}
			log.debug("setupjobpairs-- done looking at pairs, updating the database");
			JobPairs.updateJobSpaces(p);
			updatePrimarySpace(jobId,primarySpaceId);
			log.debug("returning new job space id = "+primarySpaceId);
			return primarySpaceId;
		} catch (Exception e) {
			log.error("setupJobSpaces says "+e.getMessage(),e);
		}
		
		return -1;
	}
	
	/**
	 * Updates the primary space of a job. This should only be necessary when changing the primary space
	 * of an older job from nothing to its new job space
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The new job space ID
	 * @param con the open connection to make the call on
	 * @return true on success, false otherwise
	 * @author Eric Burns
	 */
	
	private static boolean updatePrimarySpace(int jobId, int jobSpaceId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL UpdatePrimarySpace(?, ?)}");
			procedure.setInt(1, jobId);		
			procedure.setInt(2, jobSpaceId);
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
	 * Updates the primary space of a job. This should only be necessary when changing the primary space
	 * of an older job from nothing to its new job space
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The new job space ID
	 * @return true on success, false otherwise
	 * @author Eric Burns
	 */
	
	private static boolean updatePrimarySpace(int jobId, int jobSpaceId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return updatePrimarySpace(jobId, jobSpaceId, con);
			
		} catch (Exception e) {
			log.error("Update Primary Space says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	
	/**
	 * Removes job stats for every job_space belonging to this job
	 * @param jobId The ID of the job to remove the stats of
	 * @param con The open Connection to make the database call on
	 * @return True on success and false otherwise
	 */
	public static boolean removeCachedJobStats(int jobId, Connection con) {
		CallableStatement procedure=null;
		try {
			Job j=Jobs.get(jobId);
			if (j==null) {
				return false; //could not find the job
			}
			List<Space> jobSpaces=Spaces.getSubSpacesForJob(j.getPrimarySpace(), true);
			jobSpaces.add(Spaces.getJobSpace(j.getPrimarySpace()));
			
			for (Space s : jobSpaces) {
				procedure=con.prepareCall("{CALL RemoveJobStatsInJobSpace(?)}");
				procedure.setInt(1,s.getId());
				procedure.executeUpdate();
				Common.safeClose(procedure);
			}
			return true;
		} catch (Exception e) {
			log.error("removeCachedJobStats says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Completely clears the cache of all job stats from the database
	 * @param con
	 * @return
	 */
	
	public static boolean removeAllCachedJobStats(Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL RemoveAllJobStats()}");
			procedure.executeUpdate();
			Common.safeClose(procedure);
			
			return true;
		} catch (Exception e) {
			log.error("removeCachedJobStats says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	/**
	 * Removes the cached job results for every job
	 * @return True if successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean removeAllCachedJobStats() {
		Connection con=null;
		try {
			con=Common.getConnection();
			return removeAllCachedJobStats(con);
			
		} catch (Exception e) {
			log.error("removeAllCachedJobStats says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	/**
	 * Removes the cached job results for every job space associated with this job
	 * @param jobId The ID of the job to remove the cached stats for
	 * @return True if successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean removeCachedJobStats(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return removeCachedJobStats(jobId,con);
			
		} catch (Exception e) {
			log.error("removeCachedJobStats says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	public static int getPausedJobCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPausedJobCount()}");
			results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}

	/**
	 * Gets the number of Running Jobs in the whole system
	 * 
	 * @author Wyatt Kaiser
	 */
	
	public static int getRunningJobCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetRunningJobCount()}");
			results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}
	
	public static List<Job> getRunningJobs() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllJobs()}");
			results = procedure.executeQuery();
			
			List<Job> jobs = new LinkedList<Job>();
			while (results.next()) {
				if (results.getString("status").equals("incomplete")) {
					Job j = new Job();
					j.setId(results.getInt("id"));
					j.setUserId(results.getInt("user_id"));
					j.setName(results.getString("name"));	
					j.setPrimarySpace(results.getInt("primary_space"));
					j.setDescription(results.getString("description"));				
					j.setCreateTime(results.getTimestamp("created"));	
					j.setCompleteTime(results.getTimestamp("completed"));

					j.setSeed(results.getLong("seed"));

					jobs.add(j);
				}
			}
			return jobs;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	public static boolean isSystemPaused() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsSystemPaused()}");
			results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getBoolean("paused");
			}
			//if no results exist, the system is not globally paused
			return false;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return false;
	}

	public static List<Job> getUnRunnableJobs() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUnRunnableJobs()}");
			results = procedure.executeQuery();
			
			List<Job> jobs = new LinkedList<Job>();
			while (results.next()) {
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setName(results.getString("name"));
				j.setDeleted(results.getBoolean("deleted"));
				j.setPaused(results.getBoolean("paused"));
				j.setQueue(Queues.get(results.getInt("queue_id")));
				jobs.add(j);
			}
			//if no results exist, the system is not globally paused
			return jobs;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets the ID of every job a user owns that is orphaned
	 * @param userId
	 * @return
	 */
	public static List<Integer> getOrphanedJobs(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		List<Integer> ids=new ArrayList<Integer>();
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetOrphanedJobIds(?)}");
			procedure.setInt(1, userId);
			results= procedure.executeQuery();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	
	/**
	 * Deletes all of the jobs a user has that are not in any spaces
	 * @param userId The ID of the user who will have their solvers recycled
	 * @return
	 */
	public static boolean deleteOrphanedJobs(int userId) {
		
		List<Integer> ids = getOrphanedJobs(userId);
		try {
			for (Integer id : ids) {
				Jobs.delete(id);
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return false;
	}
}