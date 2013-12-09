package org.starexec.data.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.CacheType;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.SolverStats;
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
	private static final String sqlDelimiter = ",";
	/**
	 * Adds a new job to the database. NOTE: This only records the job in the 
	 * database, this does not actually submit a job for execution (see JobManager.submitJob).
	 * This method also fills in the IDs of job pairs of the given job object.
	 * @param job The job data to add to the database
	 * @param spaceId The id of the space to add the job to
	 * @return True if the operation was successful, false otherwise.
	 */
	public static boolean add(Job job, int spaceId) {
		Connection con = null;
		PreparedStatement procedure=null;
		try {
			HashMap<Integer,String> idsToNames=new HashMap<Integer,String>();
			
			idsToNames.put(job.getPrimarySpace(), Spaces.getName(job.getPrimarySpace()));
			HashMap<Integer,Integer> idMap= new HashMap<Integer,Integer>();
			con = Common.getConnection();
			
			Common.beginTransaction(con);

			for (JobPair pair : job) {
				if (idsToNames.containsKey(pair.getSpace().getId())) {
					continue;
				}
				idsToNames.put(pair.getSpace().getId(), pair.getSpace().getName());
				int parentId=Spaces.getParentSpace(pair.getSpace().getId());
				
				//get all necessary spaces up the hierarchy
				//We've already added the root space for the job, so this is guaranteed to stop either
				//there or earlier
				while (!idsToNames.containsKey(parentId)) {
					idsToNames.put(parentId, Spaces.getName(parentId));
					parentId=Spaces.getParentSpace(parentId);
					log.debug("got new parent space id = "+parentId);
				}
			}
			
			log.debug("finished adding spaces, starting to get job space associations");
			
			for (int id : idsToNames.keySet()) {
				int jobSpaceId=Spaces.addJobSpace(idsToNames.get(id),con);
				idMap.put(id, jobSpaceId);
			}
			for (int id : idMap.keySet()) {
				log.debug("getting subspaces for space = "+id);
				List<Integer> subspaceIds=Spaces.getSubSpaceIds(id);
				log.debug("found "+subspaceIds.size()+" subspaces");
				for (int subspaceId : subspaceIds) {
					
					if (idMap.containsKey(subspaceId)) {
						log.debug("found an association between two spaces needed for a job");
						Spaces.associateJobSpaces(idMap.get(id), idMap.get(subspaceId), con);
					}
				}
			}
			log.debug("finished getting subspaces, adding job");
			//the primary space of a job should be a job space ID instead of a space ID
			job.setPrimarySpace(idMap.get(job.getPrimarySpace()));
			Jobs.addJob(con, job);
			Jobs.associate(con, job.getId(), spaceId);
			
			log.debug("adding job pairs");
			File dir=new File(R.JOBPAIR_INPUT_DIR);
			dir.mkdirs();
			File jobPairFile=new File(R.JOBPAIR_INPUT_DIR,UUID.randomUUID().toString());
			jobPairFile.createNewFile();
			BufferedWriter writer=new BufferedWriter(new FileWriter(jobPairFile));
			for(JobPair pair : job) {
				pair.setJobId(job.getId());
				pair.setJobSpaceId(idMap.get(pair.getSpace().getId()));
				//writer.write(getPairString(pair));
				JobPairs.addJobPair(con, pair);
			}
			writer.flush();
			writer.close();
			procedure=con.prepareStatement("LOAD DATA INFILE ? INTO TABLE JOB_PAIRS " +
					" FIELDS TERMINATED BY ',' " +
					"(job_id, bench_id, config_id, status_code, cpuTimeout, clockTimeout, path,job_space_id,solver_name,bench_name,config_name,solver_id);");
			procedure.setString(1, jobPairFile.getAbsolutePath());
			//procedure.executeUpdate();
			jobPairFile.delete();
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
	
	/**
	 * Adds a job record to the database. This is a helper method for the Jobs.add method
	 * @param con The connection the update will take place on
	 * @param job The job to add
	 */
	private static void addJob(Connection con, Job job) throws Exception {				
		CallableStatement procedure = null;
		
		 try {
			procedure = con.prepareCall("{CALL AddJob(?, ?, ?, ?, ?, ?, ?, ?)}");
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
			// The procedure will return the job's new ID in this parameter
			procedure.registerOutParameter(8, java.sql.Types.INTEGER);	
			procedure.executeUpdate();			

			// Update the job's ID so it can be used outside this method
			job.setId(procedure.getInt(8));
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
	/**
	 * Deletes the job with the given id from disk, and sets the "deleted" column
	 * in the database jobs table to true. If  the job is referenced by no spaces,
	 * it is deleted from the database.
	 * @param jobId The ID of the job to delete
	 * @return True on success, false otherwise
	 */
	
	public static boolean delete(int jobId) {
		Connection con=null;
		try {
			Jobs.invalidateJobRelatedCaches(jobId);
			
			con=Common.getConnection();
			return delete(jobId,con);
		} catch (Exception e) {
			log.error("deleteJob says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
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
				j.setQueue(Queues.get(results.getInt("queue_id")));
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setCreateTime(results.getTimestamp("created"));
				j.setPreProcessor(Processors.get(results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(results.getInt("post_processor")));
				j.setDescription(results.getString("description"));
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
		List<SolverStats> stats=Jobs.getJobStatsInJobSpaceHierarchy(jobSpaceId);
		//if the size is greater than 0, then this job is done and its stats have already been
		//computed and stored
		if (stats!=null && stats.size()>0) {
			log.debug("stats already cached in database");
			return stats;
		}
		//otherwise, we need to compile the stats
		log.debug("stats not present in database -- compiling stats now");
		List<JobPair> pairs=getJobPairsForStatsInJobSpace(jobId,jobSpaceId);
		List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, true);

		for (Space s : subspaces) {
			pairs.addAll(getJobPairsForStatsInJobSpace(jobId,s.getId()));
		}
		List<SolverStats> newStats=processPairsToSolverStats(pairs);
		
		saveStats(jobId,jobSpaceId,newStats);
		
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
				jobs.add(j);				
			}			
			Common.safeClose(results);			
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
			Common.safeClose(results);
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
			Common.safeClose(results);
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
				j.setCreateTime(results.getTimestamp("created"));				
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setQueue(Queues.get(con, results.getInt("queue_id")));
				j.setPreProcessor(Processors.get(con, results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con, results.getInt("post_processor")));
			}
			else{
				j=null;
			}
			
			if (j != null){
				if (since==null) {
					j.setJobPairs(Jobs.getPairsDetailed(j.getId()));
				} else  {
					j.setJobPairs(Jobs.getNewCompletedPairsDetailed(j.getId(), since));
				}
				
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
	
	/**
	 * Gets all job pairs that are enqueued(up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getEnqueuedPairs(Connection con, int jobId) throws Exception {	

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
					nodes.put(nodeId,Cluster.getNodeDetails(nodeId));
				}
				jp.setNode(nodes.get(nodeId));	
				int benchId=results.getInt("bench_id");
				if (!benchmarks.containsKey(benchId)) {
					benchmarks.put(benchId,Benchmarks.get(benchId));
				}
				jp.setBench(benchmarks.get(benchId));
				int configId=results.getInt("config_id");
				if (!configs.containsKey(configId)) {
					configs.put(configId, Solvers.getConfiguration(configId));
					solvers.put(configId, Solvers.getSolverByConfig(configId,false));
				}
				jp.setSolver(solvers.get(configId));
				jp.setConfiguration(configs.get(configId));
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				returnList.add(jp);
			}			
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
	
	public static Job getIncludingDeleted(int jobId) {
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
	 * Gets all attributes for every job pair in the given job space
	 * @param jobId The ID of the job in question
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
	 * @param quitIfExceedsMax Whether to quit counting immediately if the pair count is higher
	 * than the maximum displayable number of pairs defined in R.java
	 * @return the number of job pairs for the given job
	 * @author Eric Burns
	 */
	public static int getJobPairCountInJobSpace(int jobSpaceId, boolean hierarchy, boolean quitIfExceedsMax) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		int jobPairCount=0;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobPairCountByJobInJobSpace(?)}");
			procedure.setInt(1,jobSpaceId);
			results = procedure.executeQuery();
			if (results.next()) {
				jobPairCount = results.getInt("jobPairCount");
			}
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			
			return 0;
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		if (hierarchy) {
			//return before getting subspaces at all
			if (quitIfExceedsMax && jobPairCount>R.MAXIMUM_JOB_PAIRS) {
				return jobPairCount;
			}
			List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, false);
			for (Space s : subspaces) {
				jobPairCount+=getJobPairCountInJobSpace(s.getId(),false,true);
				if (quitIfExceedsMax && jobPairCount>R.MAXIMUM_JOB_PAIRS) {
					return jobPairCount;
				}
			}
		}
		
		return jobPairCount;		
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
	public static List<JobPair> getJobPairsForNextPageByConfigInJobSpaceHierarchy(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId, int jobSpaceId, int configId, int[] totals) {
		long a=System.currentTimeMillis();
		//get all of the pairs first, then carry out sorting and filtering
		//PERFMORMANCE NOTE: this call takes over 99.5% of the total time this function takes
		List<JobPair> pairs=Jobs.getJobPairsForTableByConfigInJobSpace(jobId,jobSpaceId,configId,true);
		log.debug("getting all the pairs by config in job space took "+(System.currentTimeMillis()-a));
		totals[0]=pairs.size();
		List<JobPair> returnList=new ArrayList<JobPair>();
		pairs=JobPairs.filterPairs(pairs, searchQuery);
		log.debug("filtering pairs took "+(System.currentTimeMillis()-a));

		totals[1]=pairs.size();
		pairs=JobPairs.mergeSortJobPairs(pairs, indexOfColumnSortedBy, isSortedASC);
		log.debug("sorting pairs took "+(System.currentTimeMillis()-a));

		if (startingRecord>=pairs.size()) {
			//we'll just return nothing
		} else if (startingRecord+recordsPerPage>pairs.size()) {
			returnList = pairs.subList(startingRecord, pairs.size());
		} else {
			 returnList = pairs.subList(startingRecord,startingRecord+recordsPerPage);
		}
		log.debug("getting the correct subset of pairs took "+(System.currentTimeMillis()-a));

		log.debug("the size of the return list is "+returnList.size());
		return returnList;
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
			List<JobPair> jobPairs = new LinkedList<JobPair>();
			
			while(results.next()){
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("job_pairs.id"));
				jp.setWallclockTime(results.getDouble("wallclock"));
				Benchmark bench = jp.getBench();
				bench.setId(results.getInt("bench_id"));
				bench.setName(results.getString("bench_name"));

				Solver solver = jp.getSolver();
				solver.setId(results.getInt("solver_id"));
				solver.setName(results.getString("solver_name"));

				Configuration config = jp.getConfiguration();
				config.setId(results.getInt("config_id"));
				config.setName(results.getString("config_name"));
				
				Status status = jp.getStatus();
				status.setCode(results.getInt("job_pairs.status_code"));
				

				Properties attributes = jp.getAttributes();
				attributes.setProperty(R.STAREXEC_RESULT, results.getString("result"));

				solver.addConfiguration(config);
				jobPairs.add(jp);		
			}	
			Common.safeClose(results);
			
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
	 * Returns all of the job pairs in a given space, populated with all the fields necessary
	 * to display in a SolverStats table
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsForStatsInJobSpace(int jobId, int jobSpaceId) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("Getting pairs for job = "+jobId+" in space = "+jobSpaceId);
		try {
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsByJobInJobSpace(?)}");
			procedure.setInt(1,jobSpaceId);
			results = procedure.executeQuery();
			
			List<JobPair> pairs=processStatResults(results, jobId,con);
			
			HashMap<Integer,Properties> attrs=Jobs.getJobAttributesInJobSpace(jobSpaceId);
			for (JobPair jp : pairs) {
				if (attrs.containsKey(jp.getId())) {
					jp.setAttributes(attrs.get(jp.getId()));
				}
			}
			return pairs;
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
	 * Gets detailed job pair information for the given job, where all the pairs had benchmarks in the given job_space
	 * and used the configuration with the given ID
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The ID of the job_space in question
	 * @param configId The ID of the configuration in question
	 * @return A List of JobPair objects
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getJobPairsForTableByConfigInJobSpace(int jobId,int jobSpaceId, int configId, boolean hierarchy) {
		long a=System.currentTimeMillis();
		log.debug("beginning function getJobPairsForTableByConfigInJobSpace");
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	

			log.info("getting detailed pairs for job " + jobId +" with configId = "+configId+" in space "+jobSpaceId);
			//otherwise, just get the completed ones that were completed later than lastSeen
			procedure = con.prepareCall("{CALL GetJobPairsForTableByConfigInJobSpace(?, ?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,configId);

			results = procedure.executeQuery();
			log.debug("executing query took "+(System.currentTimeMillis()-a));
			List<JobPair> pairs = new ArrayList<JobPair>();
			
			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("id"));
				jp.setWallclockTime(results.getDouble("wallclock"));
				Benchmark bench = jp.getBench();
				bench.setId(results.getInt("bench_id"));
				bench.setName(results.getString("bench_name"));
				
				Status status = jp.getStatus();
				status.setCode(results.getInt("status_code"));

				Properties attributes = jp.getAttributes();
				attributes.setProperty(R.STAREXEC_RESULT, results.getString("result"));
				pairs.add(jp);	
			}
			log.debug("processing pairs took "+(System.currentTimeMillis()-a));

			
			if (hierarchy) {
				List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, true);
				log.debug("getting subspaces took "+(System.currentTimeMillis()-a));

				for (Space s : subspaces) {
					pairs.addAll(getJobPairsForTableByConfigInJobSpace(jobId,s.getId(),configId,false));
				}
			}
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
	 * it is returned
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The ID of the job_space in question
	 * @param configId The ID of the configuration in question
	 * @return A List of JobPair objects
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getJobPairsShallowByConfigInJobSpace(int jobSpaceId, int configId, boolean hierarchy, boolean includeBenchmarks) {
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();

			//otherwise, just get the completed ones that were completed later than lastSeen
			if (!includeBenchmarks) {
				procedure = con.prepareCall("{CALL GetJobPairsShallowByConfigInJobSpace(?, ?)}");

			} else {
				procedure = con.prepareCall("{CALL GetJobPairsShallowWithBenchmarksByConfigInJobSpace(?, ?)}");

			}
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,configId);

			results = procedure.executeQuery();
			List<JobPair> pairs = new ArrayList<JobPair>();
			while (results.next()) {
				JobPair jp=new JobPair();
				Status s=jp.getStatus();
				s.setCode(results.getInt("status_code"));
				jp.setId(results.getInt("job_pairs.id"));
				jp.setWallclockTime(results.getDouble("wallclock"));
				jp.setCpuUsage(results.getDouble("cpu"));
				Solver solver=jp.getSolver();
				solver.setId(results.getInt("solver_id"));
				solver.setName(results.getString("solver_name"));
				Configuration c=jp.getConfiguration();
				c.setId(results.getInt("config_id"));
				c.setName(results.getString("config_name"));
				solver.addConfiguration(c);
				if (includeBenchmarks) {
					Benchmark b=jp.getBench();
					b.setId(results.getInt("bench_id"));
					b.setName(results.getString("bench_name"));
				}
				pairs.add(jp);
			}
			if (hierarchy) {
				List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, true);

				for (Space s : subspaces) {
					pairs.addAll(getJobPairsShallowByConfigInJobSpace(s.getId(),configId,false,includeBenchmarks));
				}
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
				j.setLiteJobPairStats(liteJobPairStats);
				jobs.add(j);		
			}	
			Common.safeClose(results);
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
			
			procedure = con.prepareCall("{CALL GetNextPageOfRunningJobsAdmin(?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setString(5, searchQuery);
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
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));	
				if (results.getBoolean("deleted")) {
					j.setName(j.getName()+" (deleted)");
				}
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
	
	public static List<SolverStats> getJobStatsInJobSpaceHierarchy(int jobSpaceId) {
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
				s.setIncompleteJobPairs(0); //we only store things in the stats table when the job is totally done
				s.setTime(results.getDouble("wallclock"));
				s.setFailedJobPairs(results.getInt("failed"));
				s.setIncorrectJobPairs(results.getInt("incorrect"));
				s.setCorrectJobPairs(results.getInt("correct"));
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
			//otherwise, just get the completed ones that were completed later than lastSeen
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
				pair.setPath(results.getString("path"));
				pair.getSolver().setName(results.getString("solver_name"));
				pair.getConfiguration().setName(results.getString("config_name"));
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
		log.debug("Getting all attributes for job with ID = "+jobId);
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
	 * Gets all job pairs for the given job non-recursively 
	 * (Worker node, status, benchmark and solver will NOT be populated)
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
				jp.getSolver().getConfigurations().add(new Configuration(results.getInt("config_id")));
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
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @param since The completion ID to get all the pairs after. If null, gets all pairs
	 * @return A list of job pair objects that belong to the given job.
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getPairsDetailed(int jobId) {
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
					jp.setAttributes(props.get(jp.getId()));
				} else {
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
				if (!discoveredSolvers.containsKey(curSolver)) {
					Solver solver= Solvers.resultToSolver(results,"solver");				
					jp.setSolver(solver);
					discoveredSolvers.put(curSolver, solver);
				}
				jp.setSolver(discoveredSolvers.get(curSolver));
				
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
				jp.setConfiguration(discoveredConfigs.get(curConfig));
				jp.getSolver().addConfiguration(discoveredConfigs.get(curConfig));
				if (!discoveredNodes.containsKey(curNode)) {
					WorkerNode node=new WorkerNode();
					node.setName(results.getString("node.name"));
					node.setId(results.getInt("node.id"));
					node.setStatus(results.getString("node.status"));
					discoveredNodes.put(curNode,node);
				}
				jp.setNode(discoveredNodes.get(curNode));
			}
			log.info("returning "+ returnList.size()+ " detailed pairs for job " + jobId );
			return returnList;	
			
		} catch (Exception e){			
			log.error("getPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		}

		return null;		
	}
	
	private static String getPairString(JobPair pair) {
		StringBuilder sb=new StringBuilder();
		sb.append(pair.getJobId());
		sb.append(sqlDelimiter);
		sb.append(pair.getBench().getId());
		sb.append(sqlDelimiter);
		sb.append(pair.getSolver().getConfigurations().get(0).getId());
		sb.append(sqlDelimiter);
		sb.append(pair.getStatus().getCode().getVal());
		sb.append(sqlDelimiter);
		sb.append(pair.getCpuTimeout());
		sb.append(sqlDelimiter);
		sb.append(pair.getWallclockTimeout());
		sb.append(sqlDelimiter);
		sb.append(pair.getPath());
		sb.append(sqlDelimiter);
		sb.append(pair.getJobSpaceId());
		sb.append(sqlDelimiter);
		sb.append(pair.getSolver().getName());
		sb.append(sqlDelimiter);
		sb.append(pair.getBench().getName());
		sb.append(sqlDelimiter);
		sb.append(pair.getSolver().getConfigurations().get(0).getName());
		sb.append(sqlDelimiter);
		sb.append(pair.getSolver().getId());
		sb.append(sqlDelimiter);
		sb.append("\n");
		return sb.toString();
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

		CallableStatement procedure = null;
		ResultSet results = null;
		 try {
			procedure = con.prepareCall("{CALL GetPendingJobPairsByJob(?,?)}");
			procedure.setInt(1, jobId);					
			procedure.setInt(2, R.NUM_JOB_SCRIPTS);
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
					nodes.put(nodeId, Cluster.getNodeDetails(nodeId));
				}
				jp.setNode(nodes.get(nodeId));	
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
				if (configId!=null) {
					if (!configs.containsKey(configId)) {
						solvers.put(configId, Solvers.getSolverByConfig(configId, false));
						configs.put(configId, Solvers.getConfiguration(configId));	
					}
					jp.setSolver(solvers.get(configId));
					jp.setConfiguration(configs.get(configId));
					
				}
				
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				jp.setAttributes(JobPairs.getAttributes(con, jp.getId()));
				returnList.add(jp);
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
	public static List<JobPair> getPendingPairsDetailed(int jobId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getPendingPairsDetailed(con, jobId);
		} catch (Exception e){			
			log.error("getPendingPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
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
	 * @param statusCode which status to filter grid engine ids by
	 * @return A list of SGE job id's that have the specified status
	 */
	public static List<Integer> getSgeIdsByStatus(int statusCode) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();									
			 procedure = con.prepareCall("{CALL GetSGEIdsByStatus(?)}");			
			procedure.setInt(1, statusCode);

			 results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<Integer>();

			while(results.next()){
				ids.add(results.getInt("sge_id"));
			}	
			Common.safeClose(results);
			
			return ids;
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
	 * Retrieves a job with basic info from the database (excludes pair and queue/processor info) 
	 * @param jobId The id of the job to get information for 
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	public static Job getShallow(int jobId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setInt(1, jobId);					
			results = procedure.executeQuery();

			if(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));				
				j.setPrimarySpace(results.getInt("primary_space"));
				j.getQueue().setId(results.getInt("queue_id"));
				j.getPreProcessor().setId(results.getInt("pre_processor"));
				j.getPostProcessor().setId(results.getInt("post_processor"));				
				log.debug("getShallow found job, returning it");
				return j;
			}		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		log.debug("getShallow could not find job, returning null");
		return null;
	}
	
	/**
	 * Invalidates every type of cache related to this job (useful when deleting a job)
	 * Includes job output, job CSV with and without IDs, and pair output
	 * @param jobId The ID of the job for which to delete all associated cache entreis
	 */
	public static void invalidateJobRelatedCaches(int jobId) {
		Cache.invalidateCache(jobId, CacheType.CACHE_JOB_OUTPUT);
		Cache.invalidateCache(jobId, CacheType.CACHE_JOB_CSV);
		Cache.invalidateCache(jobId, CacheType.CACHE_JOB_CSV_NO_IDS);
		List<JobPair> pairs = Jobs.getPairs(jobId);
		for (JobPair pair : pairs) {
			Cache.invalidateCache(pair.getId(), CacheType.CACHE_JOB_PAIR);
		}
	}
	
	/**
	 * Determines whether the job with the given ID is complete
	 * @param jobId The ID of the job in question 
	 * @return True if the job is complete, false otherwise (includes the possibility of error)
	 * @author Eric Burns
	 */
	
	public static boolean isJobComplete(int jobId) {
		try {
			//if the job is paused, it is not done yet
			if (isJobPaused(jobId)) {
				return false;
			}
			HashMap<String,String> overview = Statistics.getJobPairOverview(jobId);
			//if the job is not paused and no pending pairs remain, it is done
			if (overview.get("pendingPairs").equals("0")) {
				return true;
			} 

			return false;
		} catch (Exception e) {
			log.error("isJobComplete says "+e.getMessage(),e);
		}
		return false;
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
		return isJobPausedOrKilled(jobId)==1;
	}

	/**
	 * Determines whether the given job is either paused or killed
	 * @param con The open connection to make the query on 
	 * @param jobId The ID of the job in question
	 * @return
	 * 0 if the job is neither paused nor killed
	 * 1 if the job is paused
	 * 2 if the job has been killed
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
		log.debug("isPublic called on job " + jobId);
		Job j = Jobs.get(jobId);
		if (j==null) {
			return false;
		}
		if (j.getUserId()==R.PUBLIC_USER_ID){
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
			log.debug("Job " + j.getName() + " is in " + count  + " public spaces");
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
				int sge_id = jp.getGridEngineId();
				Util.executeCommand("qdel " + sge_id);	
				log.debug("Just executed qdel " + sge_id);
				JobPairs.UpdateStatus(jp.getId(), 21);
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
		CallableStatement procedure = null;
		try {
			 procedure = con.prepareCall("{CALL PauseJob(?)}");
			procedure.setInt(1, jobId);		
			procedure.executeUpdate();	

			log.debug("Pausation of job id = " + jobId + " was successful");
			
			//Get the enqueued job pairs and remove them
			List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(jobId);
			for (JobPair jp : jobPairsEnqueued) {
				int sge_id = jp.getGridEngineId();
				Util.executeCommand("qdel " + sge_id);
				log.debug("enqueued: Just executed qdel " + sge_id);
				JobPairs.UpdateStatus(jp.getId(), 20);
			}
			//Get the running job pairs and remove them
			List<JobPair> jobPairsRunning = Jobs.getRunningPairs(jobId);
			if (jobPairsRunning != null) {
				for (JobPair jp: jobPairsRunning) {
					int sge_id = jp.getGridEngineId();
					Util.executeCommand("qdel " + sge_id);
					log.debug("running: Just executed qdel " + sge_id);
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
			log.debug("result set obtained");
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
	//NOTE: Many parameters here are unused because we are currently doing client-side processing of our stats
	//datatables, but they would be used if we ever wanted to switch to server-side processing
	public static List<SolverStats> processPairsToSolverStats(List<JobPair> pairs) {
		Hashtable<String, SolverStats> SolverStats=new Hashtable<String,SolverStats>();
		String key=null;
		for (JobPair jp : pairs) {
			
			//entries in the stats table determined by solver/configuration pairs
			key=String.valueOf(jp.getSolver().getId())+":"+String.valueOf(jp.getConfiguration().getId());
			
			if (!SolverStats.containsKey(key)) { // current stats entry does not yet exist
				SolverStats newSolver=new SolverStats();
				log.debug("adding solver "+jp.getSolver().getName()+ " with configuration "+jp.getConfiguration().getName()+" to stats");
				
				newSolver.setSolver(jp.getSolver());
				newSolver.setConfiguration(jp.getConfiguration());
				SolverStats.put(key, newSolver);
			}
			
			
			//update stats info for entry that current job-pair belongs to
			SolverStats curSolver=SolverStats.get(key);
			StatusCode statusCode=jp.getStatus().getCode();
			curSolver.incrementTime(jp.getWallclockTime());
			if ( statusCode.error()) {
			    curSolver.incrementFailedJobPairs();
			} else if (statusCode.incomplete()) {
			    curSolver.incrementIncompleteJobPairs();
			} else if (statusCode.complete()) {
			    curSolver.incrementCompleteJobPairs();
			    if (jp.getAttributes()!=null) {
			    	Properties attrs = jp.getAttributes();
			    	//if the attributes don't have an expected result, we don't know whether the pair
			    	//was correct or incorrect
			    	if (attrs.contains(R.STAREXEC_RESULT) && attrs.contains(R.EXPECTED_RESULT)) {
			    		if (!attrs.get(R.STAREXEC_RESULT).equals(attrs.get(R.EXPECTED_RESULT))) {
			    			curSolver.incrementIncorrectJobPairs();
			    		} else {
			    			curSolver.incrementCorrectJobPairs();
			    		}
			    	}
			    }
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
	
	private static List<JobPair> processStatResults(ResultSet results, int jobId, Connection con) throws Exception {
		log.debug("Processing stat results for job = "+jobId);
		List<JobPair> returnList = new ArrayList<JobPair>();
		HashMap<Integer,Solver> solvers=new HashMap<Integer,Solver>();
		HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
		int id;
		Solver solve=null;
		Configuration config=null;
		while(results.next()){
			JobPair jp = JobPairs.shallowResultToPair(results);
			id=results.getInt("solver_id");
			if (!solvers.containsKey(id)) {
				solve=new Solver();
				solve.setId(id);
				solve.setName(results.getString("solver_name"));
				solvers.put(id,solve);
			}
			jp.setSolver(solvers.get(id));

			id=results.getInt("config_id");
			if (!configs.containsKey(id)) {
				config=new Configuration();
				config.setId(id);
				config.setName(results.getString("config_name"));
				configs.put(id, config);
			}
			jp.setConfiguration(configs.get(id));

			Status s = new Status();
			
			s.setCode(results.getInt("status_code"));
			jp.setStatus(s);
			returnList.add(jp);			
			
		}
		
		Common.safeClose(results);
		
		
		
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
	 * Runs the given post processor on the pair output of the already-finished job
	 * @param jobId The ID of the the job to process
	 * @param processorId The ID of the post-processor to use
	 * @return True if the operation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean runPostProcessor(int jobId, int processorId) {
		BufferedReader reader = null;
		HashMap<Integer,Integer> statusCodes=new HashMap<Integer, Integer>();
		try {
			Processor p=Processors.get(processorId);
			List<JobPair> pairs=Jobs.getPairs(jobId);
			//store the status code of every pair so we can restore it after we're done
			for (JobPair jp : pairs) {
				statusCodes.put(jp.getId(), jp.getStatus().getCode().getVal());
			}
			Jobs.removeCachedJobStats(jobId);
			setPairStatusByJob(jobId,StatusCode.STATUS_PROCESSING.getVal());
			log.info("Beginning processing for " + pairs.size() + " pairs");			
			// For each benchmark in the list to process...
			for(JobPair jp : pairs) {
				
					try {
						// Run the processor on the benchmark file
						String [] procCmd = new String[2];
						procCmd[0] = p.getFilePath();
				
						procCmd[1] = JobPairs.getFilePath(jp.getId());
						reader = Util.executeCommand(procCmd,null);
						
						// Load results into a properties file
						Properties prop = new Properties();
						if (reader != null){
							prop.load(reader);							
							reader.close();
						}
						JobPairs.addJobPairAttributes(jp.getId(), prop,jobId);
						
					} catch (Exception e) {
						log.error("error processing jp "+jp.getId()+" . Error says "+e.getMessage(),e);
					}
					try {
						JobPairs.setPairStatus(jp.getId(), statusCodes.get(jp.getId()));
					} catch (Exception e) {
						log.error("error restoring pair status for jp with id = "+jp.getId()+" error says "+e.getMessage(),e);
						//TODO: What is the behavior supposed to be here?
					}
					
			}
			return true;
			
		} catch (Exception e) {
			log.error("runPostProcessor says "+e.getMessage(),e);
		} finally {
			if(reader != null) {
				try { 
					reader.close(); 
				} catch(Exception e) {
					//ignore
				}
			}
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
			procedure=con.prepareCall("{CALL AddJobStats(?,?,?,?,?,?,?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,stats.getConfiguration().getId());
			procedure.setInt(3,stats.getCompleteJobPairs());
			procedure.setInt(4,stats.getCorrectJobPairs());
			procedure.setInt(5,stats.getIncorrectJobPairs());
			procedure.setInt(6,stats.getFailedJobPairs());
			procedure.setDouble(7,stats.getTime());
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
	
	private static boolean setPairStatusByJob(int jobId, int statusCode) {
		log.debug("setting pairs to status "+statusCode);
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL SetPairsToStatus(?,?)}");
			procedure.setInt(1,jobId);
			procedure.setInt(2,statusCode);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("setPairStatusByJob says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
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
			List<JobPair> p=Jobs.getPairsDetailed(jobId);
			Integer primarySpaceId=null;
			HashMap<String,Integer> namesToIds=new HashMap<String,Integer>();
			for (JobPair jp : p) {
				String pathString=jp.getPath();
				if (pathString==null) {
					pathString="job space";
				}
				String[] path=pathString.split("/");
				String key="";
				for (int index=0;index<path.length; index++) {
					
					String spaceName=path[index];
					key=key+"/"+spaceName;
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
						String parentKey=key.substring(0,key.lastIndexOf("/"));
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
			}
			log.debug("setupjobpairs-- done looking at pairs, updating the database");
			JobPairs.UpdateJobSpaces(p);
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
	 * @return true on success, false otherwise
	 * @author Eric Burns
	 */
	
	private static boolean updatePrimarySpace(int jobId, int jobSpaceId) {
		Connection con=null;
		CallableStatement procedure = null;
		try {
			con=Common.getConnection();
			 procedure = con.prepareCall("{CALL UpdatePrimarySpace(?, ?)}");
			procedure.setInt(1, jobId);		
			procedure.setInt(2, jobSpaceId);
			procedure.executeUpdate();	

			log.debug("Primary space for job with id = "+jobId + " updated succesfully");
			return true;
		} catch (Exception e) {
			log.error("Update Primary Space says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	public static List<JobPair> getIncompleteJobPairs(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetIncompleteJobPairs(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<JobPair> pairs=new ArrayList<JobPair>();
			if(results.next()){
				JobPair jp = new JobPair();
				jp.setId(results.getInt("id"));
				jp.setJobId(results.getInt("job_id"));
				jp.setGridEngineId(results.getInt("sge_id"));
				int benchId = results.getInt("bench_id");
				Benchmark b = Benchmarks.get(benchId);
				jp.setBench(b);
				int config_id = results.getInt("config_id");
				Configuration c = Solvers.getConfiguration(config_id);
				jp.setConfiguration(c);
				pairs.add(jp);
			} 
			return pairs;
		} catch (Exception e) {
			log.error("GetIncompleteJobPairs says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Removes the cached job results for every job space associated with this job
	 * @param jobId The ID of the job to remove the cached stats for
	 * @return True if successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean removeCachedJobStats(int jobId) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			Job j=Jobs.get(jobId);
			List<Space> jobSpaces=Spaces.getSubSpacesForJob(j.getPrimarySpace(), true);
			jobSpaces.add(Spaces.getJobSpace(j.getPrimarySpace()));
			con=Common.getConnection();
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
			Common.safeClose(con);
			Common.safeClose(procedure);
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
}