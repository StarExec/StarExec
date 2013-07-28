package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.SolverStats;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.Util;

/**
 * Handles all database interaction for jobs (NOT grid engine job execution, see JobManager for that)
 * @author Tyler Jensencv
 */

public class Jobs {
	private static final Logger log = Logger.getLogger(Jobs.class);

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
			for(JobPair pair : job) {
				pair.setJobId(job.getId());
				pair.setJobSpaceId(idMap.get(pair.getSpace().getId()));
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
			
 		}	finally {
 			Common.safeClose(procedure);
 		}
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
		CallableStatement procedure = null;
		 try {
			procedure = con.prepareCall("{CALL AddJobAttr(?, ?, ?)}");
			procedure.setInt(1, pairId);
			procedure.setString(2, key);
			procedure.setString(3, val);
			
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			
		}	finally {
			Common.safeClose(procedure);
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
	 * Deletes the job with the given id from disk, and sets the "deleted" column
	 * in the database jobs table to true. If  the job is referenced by no spaces,
	 * it is deleted from the database.
	 * @param jobId The ID of the job to delete
	 * @return True on success, false otherwise
	 */
	
	public static boolean delete(int jobId) {
		Connection con=null;
		try {
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
			File output=new File(getDirectory(jobId));
			 procedure = con.prepareCall("{CALL DeleteJob(?)}");
			procedure.setInt(1, jobId);		
			procedure.executeUpdate();	
			
			if (output.exists()) {
				FileUtils.deleteDirectory(output);
			}
			log.debug("Deletion of job id = " + jobId+ " in directory" +output.getAbsolutePath()+ "was successful");
			return true;
		} catch (Exception e) {
			log.error("Delete Job says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	public static Job getIncludingDeleted(int jobId) {
		return get(jobId,true);
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
	 * Gets all the SolverStats objects for a given job in the given space
	 * @param jobId the job in question
	 * @param spaceId The ID of the space in question
	 * @return A list containing every SolverStats for the given job where the solvers reside in the given space
	 * @author Eric Burns
	 */

	public static List<SolverStats> getAllJobStatsInJobSpaceHierarchy(int jobId,int jobSpaceId) {
		long a=System.currentTimeMillis();
		List<JobPair> pairs=getJobPairsForStatsInJobSpace(jobId,jobSpaceId);
		log.debug("just getting  the pairs for stats in one space took "+(System.currentTimeMillis()-a));
		List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, true);
		log.debug("getting  subspaces took "+(System.currentTimeMillis()-a));

		for (Space s : subspaces) {
			pairs.addAll(getJobPairsForStatsInJobSpace(jobId,s.getId()));
		}
		log.debug("getting pairs in all the subspaces took "+(System.currentTimeMillis()-a));
		List<SolverStats> stats=processPairsToSolverStats(pairs);
		log.debug("processing the stats took "+(System.currentTimeMillis()-a));

		return stats;
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
	 * Gets all the completed job pairs for the given job for which the solver belongs to
	 * the space specified by spaceId.
	 * @param jobId The job in question
	 * @param spaceId The location of the solver
	 * @return A list of completed job pairs
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getCompletedJobPairsInJobSpace(int jobId, int jobSpaceId, boolean hierarchy) {
		Connection con = null;	
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetJobPairsByJobInJobSpace(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,	jobSpaceId);
			
			results = procedure.executeQuery();

			List<JobPair> jobPairs = new LinkedList<JobPair>();
			
			while(results.next()){
				if (results.getInt("status_code")!=StatusCode.STATUS_COMPLETE.getVal()) {
					continue;
				}
				JobPair jp = JobPairs.resultToPair(results);

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
				
				
				jp.setBench(bench);
				jp.setSolver(solver);
				jp.setConfiguration(config);
				jobPairs.add(jp);		
			}	
			if (hierarchy) {
				List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, true);
				for (Space s : subspaces) {
					jobPairs.addAll(getCompletedJobPairsInJobSpace(jobId,s.getId(),false));
				}
			}
			return jobPairs;
		} catch (Exception e){			
			log.error("get JobPairs for Next Page of Job " + jobId + " says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		// The job's output is expected to be in JOB_OUTPUT_DIR/{owner's ID}/{job id}/
		Job j=Jobs.getShallow(jobId);
		return String.format("%s/%d/%d",R.JOB_OUTPUT_DIR,j.getUserId(),jobId);
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

			while(results.next()){
				JobPair jp = Jobs.resultToPair(results);
				jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
				jp.setBench(Benchmarks.get(results.getInt("bench_id")));			 
				jp.setSolver(Solvers.getSolverByConfig(results.getInt("config_id"),false));
				jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
				jp.setGridEngineId(results.getInt("sge_id"));
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				returnList.add(jp);
			}			

			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			
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
			Common.safeClose(results);
			log.debug("returning from attribute function");
			return props;
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
	 * Returns the number of job pairs that exist for a given job
	 * 
	 * @param jobId the id of the job to get the number of job pairs for
	 * @return the number of job pairs for the given job
	 * @author Todd Elvers
	 */
	public static int getJobPairCount(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobPairCountByJob(?)}");
			procedure.setInt(1, jobId);
			 results = procedure.executeQuery();
			int jobPairCount=0;
			if (results.next()) {
				jobPairCount = results.getInt("jobPairCount");
			}
			Common.safeClose(results);
			return jobPairCount;
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
	public static int getJobPairCountInJobSpace(int jobId, int jobSpaceId, boolean hierarchy, boolean quitIfExceedsMax) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobPairCountByJobInJobSpace(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,jobSpaceId);
			results = procedure.executeQuery();
			int jobPairCount=0;
			if (results.next()) {
				jobPairCount = results.getInt("jobPairCount");
			}
			
			
			
			if (hierarchy) {
				//return before getting subspaces at all
				if (quitIfExceedsMax && jobPairCount>R.MAXIMUM_JOB_PAIRS) {
					return jobPairCount;
				}
				List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, false);
				for (Space s : subspaces) {
					jobPairCount+=getJobPairCountInJobSpace(jobId,s.getId(),false,true);
					if (quitIfExceedsMax && jobPairCount>R.MAXIMUM_JOB_PAIRS) {
						return jobPairCount;
					}
				}
			}
			
			return jobPairCount;
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
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object
	 * @param startingRecord The first record to return
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether to sort ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The column of the datatable to sort on 
	 * @param searchQuery A search query to match against the pair's solver, config, or benchmark
	 * @param jobId The ID of the job in question
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object 
	 */
	
	public static List<JobPair> getJobPairsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId) {
		return getJobPairsForNextPage(startingRecord,recordsPerPage,isSortedASC,indexOfColumnSortedBy,searchQuery,jobId,null,null, false);
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
	private static List<JobPair> getJobPairsForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId, Integer jobSpaceId, Integer configId, boolean hierarchy) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 	
			
			procedure = con.prepareCall("{CALL GetNextPageOfJobPairs(?, ?, ?, ?, ?, ?, ? , ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, jobId);
			procedure.setString(6, searchQuery);
			
			//the spaceId and configId are not null only if we are getting results by config/space
			if (jobSpaceId!=null) {
				procedure.setInt(7, jobSpaceId);
			} else {
				procedure.setNull(7,java.sql.Types.INTEGER);
			}
			if (configId!=null) {
				procedure.setInt(8,configId);
			} else {
				procedure.setNull(8,java.sql.Types.INTEGER);
			}
			
			 results = procedure.executeQuery();
			List<JobPair> jobPairs = new LinkedList<JobPair>();
			
			while(results.next()){
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("job_pairs.id"));
				jp.setCpuUsage(results.getDouble("cpu"));

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
				
				Status status = new Status();
				status.setCode(results.getInt("status.code"));
				status.setStatus(results.getString("status.status"));
				status.setDescription(results.getString("status.description"));

				Properties attributes = new Properties();
				attributes.setProperty(R.STAREXEC_RESULT, results.getString("result"));

				solver.addConfiguration(config);
				jp.setBench(bench);
				jp.setSolver(solver);
				jp.setStatus(status);
				jp.setAttributes(attributes);
				jp.setJobSpaceName(results.getString("jobSpace.name"));
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
		//get all of the pairs first, then carry out sorting and filtering
		//PERFMORMANCE NOTE: this call takes over 99.5% of the total time this function takes
		List<JobPair> pairs=Jobs.getJobPairsDetailedByConfigInJobSpace(jobId,jobSpaceId,configId,true);
		totals[0]=pairs.size();
		List<JobPair> returnList=new ArrayList<JobPair>();
		pairs=JobPairs.filterPairs(pairs, searchQuery);

		totals[1]=pairs.size();
		pairs=JobPairs.mergeSortJobPairs(pairs, indexOfColumnSortedBy, isSortedASC);

		if (startingRecord>=pairs.size()) {
			//we'll just return nothing
		} else if (startingRecord+recordsPerPage>pairs.size()) {
			returnList = pairs.subList(startingRecord, pairs.size());
		} else {
			 returnList = pairs.subList(startingRecord,startingRecord+recordsPerPage);
		}

		log.debug("the size of the return list is "+returnList.size());
		return returnList;
	}
	
	/**
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object, where
	 * all the job pairs are in the given space
	 * @param startingRecord The first record to return
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether to sort ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The column of the datatable to sort on 
	 * @param searchQuery A search query to match against the pair's solver, config, or benchmark
	 * @param jobId The ID of the job in question
	 * @param spaceID The space that contains the job pairs
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object 
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getJobPairsForNextPageInJobSpace(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int jobId, int jobSpaceId) {
		return getJobPairsForNextPage(startingRecord,recordsPerPage,isSortedASC,indexOfColumnSortedBy,searchQuery,jobId,jobSpaceId,null,false);
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
			return getPairsDetailed(jobId,con,results,true);
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets the details of the job with the given ID, including all job pair information
	 * for job pairs that were completed after "since"
	 * @param jobId The ID of the job in question
	 * @param since The completion ID after which to get job pair information
	 * @return A job object with all fields populated
	 * @author Eric Burns
	 */
	
	
	public static Job getNewDetailed(int jobId, int since) {
		return getDetailed(jobId,since);
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
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();		
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
			return getPairsDetailed(jobId,con,results,false);
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
		
		try {			
			List<JobPair> returnList = new ArrayList<JobPair>();
			//because there is a lot of redundancy in node, bench, and config IDs
			// we don't want to query the database once per job pair to get them. Instead,
			//we store all the IDs we see and only query once  per node, bench, and config.
			
			Hashtable<Integer,Solver> neededSolvers=new Hashtable<Integer,Solver>();
			Hashtable<Integer,Configuration> neededConfigs=new Hashtable<Integer,Configuration>();
			Hashtable<Integer,Benchmark> neededBenchmarks=new Hashtable<Integer,Benchmark>();
			Hashtable <Integer, WorkerNode> neededNodes = new Hashtable<Integer, WorkerNode>();
			
			//store the IDs in the same order as the job pairs are stored in 'returnList'
			List<Integer> nodeIdList=new ArrayList<Integer>();
			List<Integer> benchIdList=new ArrayList<Integer>();
			List<Integer> configIdList=new ArrayList<Integer>();
			int curNode,curBench,curConfig;
			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				
				Status s = new Status();
				s.setCode(results.getInt("status.code"));
				s.setStatus(results.getString("status.status"));
				s.setDescription(results.getString("status.description"));
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
				
				
				neededNodes.put(curNode,new WorkerNode());
				neededBenchmarks.put(curBench,new Benchmark());
				neededConfigs.put(curConfig,new Configuration());
				nodeIdList.add(curNode);
				benchIdList.add(curBench);
				configIdList.add(curConfig);
			}
			
			
			Set<Integer> idSet=neededConfigs.keySet();
			for (int curId : idSet) {
				neededConfigs.put(curId, Solvers.getConfiguration(curId));
				neededSolvers.put(curId, Solvers.getSolverByConfig(curId,false));
			}
			
			idSet=neededNodes.keySet();
			for (int curId : idSet) {
				neededNodes.put(curId, Cluster.getNodeDetails(curId));
			}
			
			idSet=neededBenchmarks.keySet();
			for (int curId : idSet) {
				neededBenchmarks.put(curId,Benchmarks.get(curId));
			}
			log.debug("about to get attributes for job " +jobId );
			HashMap<Integer,Properties> props=Jobs.getJobAttributes(con,jobId);
			log.debug("just got "+ props.keySet().size() +" out of "+ returnList.size() + " attributes for job " +jobId);
			//now, set the solvers, benchmarks, etc.
			for (Integer i =0; i < returnList.size(); i++){
				JobPair jp = returnList.get(i);
				jp.setNode(neededNodes.get(nodeIdList.get(i)));
				jp.setBench(neededBenchmarks.get(benchIdList.get(i)));
				jp.setSolver(neededSolvers.get(configIdList.get(i)));
				jp.setConfiguration(neededConfigs.get(configIdList.get(i)));
				
				//NOTE: for all new jobs, props should always contain the ID of every job pair
				// in  the job, regardless of whether it has any attributes. The only reason we need to check whether it doesn't is for
				// backwards compatibility-- jobs run before the job_id column was added to the 
				//attributes table will not work with the getJobAttributes method.
				if (props.containsKey(jp.getId())) {
					jp.setAttributes(props.get(jp.getId()));
				} else {
					jp.setAttributes(JobPairs.getAttributes(jp.getId()));
				}
				
				
			}
			log.info("returning "+ returnList.size()+ " detailed pairs for job " + jobId );
			return returnList;	
			
		} catch (Exception e){			
			log.error("getPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
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
	
	public static List<JobPair> getJobPairsDetailedByConfigInJobSpace(int jobId,int jobSpaceId, int configId, boolean hierarchy) {
		Connection con = null;	
		
		ResultSet results=null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	
			
			log.info("getting detailed pairs for job " + jobId +" with configId = "+configId+" in space "+jobSpaceId);
			//otherwise, just get the completed ones that were completed later than lastSeen
			procedure = con.prepareCall("{CALL GetJobPairsByConfigInJobSpace(?, ?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,jobSpaceId);
			procedure.setInt(3,configId);
			results = procedure.executeQuery();
			List<JobPair> pairs = getPairsDetailed(jobId,con,results,false);
			
			if (hierarchy) {
				List<Space> subspaces=Spaces.getSubSpacesForJob(jobSpaceId, true);
				for (Space s : subspaces) {
					pairs.addAll(getJobPairsDetailedByConfigInJobSpace(jobId,s.getId(),configId,false));
				}
			}
			
			return pairs;
		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets all job pairs for the given job and populates fields needed for getting relevant stats
	 * (Solver and Configuration are populated, benchmark, and worker node are not) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Eric Burns
	 */
	public static List<JobPair> getPairsDetailedForStats(int jobId) {
		Connection con = null;			
		ResultSet results = null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();
			
			log.info("getting detailed pairs for job " + jobId );
			
			 procedure = con.prepareCall("{CALL GetJobPairsByJobForStats(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			
			return processStatResults(results,jobId,con);
		} catch (Exception e){			
			log.error("getPairsDetailed for job " + jobId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
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
			procedure = con.prepareCall("{CALL GetJobPairsByJobInJobSpace(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,jobSpaceId);
			results = procedure.executeQuery();
			
			return processStatResults(results, jobId,con);
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
	 * Returns all of the job pairs in space hierarchy rooted at the given space, populated with all the fields necessary
	 * to display in a SolverStats table
	 * @param jobId The ID of the job in question
	 * @param spaceId The space ID of the space containing the solvers to get stats for
	 * @param userId Ensures only the subspaces the user can see are returned
	 * @return A list of job pairs for the given job for which the solver is in the hierarchy rooted at the
	 * given space
	 * @author Eric Burns
	 */
	
	public static List<JobPair> getJobPairsForStatsInSpaceHierarchy(int jobId, int spaceId, int userId) {
		log.debug("Getting pairs for job = "+jobId+" in space = "+spaceId);
		try {
			List<JobPair> pairs= getJobPairsForStatsInJobSpace(jobId,spaceId);
			List<Space> subspaces=Spaces.getSubSpaces(spaceId, userId, true);
			for (Space s : subspaces) {
				pairs.addAll(getJobPairsForStatsInJobSpace(jobId,s.getId()));
			}
			return pairs;
		} catch (Exception e) {
			log.error("getPairsDetailedForStatsInSpace says "+e.getMessage(),e);
			return null;
		}
		
	}
	
	//the following code is used to do server-side processing of information for job stats datatables.
	//Currently, we're only doing client-side sorting, querying, etc. of these tables, but we
	//might want to do server side processing in the future.
	
	/*
	 * Helper function for sortSolverStats-- compares two SolverStats based on the indexOfColumnSortedBy
	 * @return true if first<= second, false otherwise.
	 * @author Eric Burns
	 
	
	
	private static boolean compareSolverStats(SolverStats first, SolverStats second, int indexOfColumnSortedBy) {
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
	}*/
	
	/*
	 * Helper function for GetJobStatsForNextPage-- sorts SolverStats objects based on column and ASC sent from client
	 * @author Eric Burns
	 *
	private static List<SolverStats> sortSolverStats(List<SolverStats> rows, int indexOfSortedColumn, boolean isSortedASC) {
		if (rows.size()<=1) {
			return rows;
		}
		
		List<SolverStats> answer=new LinkedList<SolverStats>();
		answer.add(rows.get(0));
		for (int index=1;index<rows.size();index++) {
			boolean inserted=false;
			for (int index2=0;index2<answer.size();index2++) {
				if (compareSolverStats(rows.get(index), answer.get(index2), indexOfSortedColumn)) {
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
			List<SolverStats> reversed=new LinkedList<SolverStats>();
			for (SolverStats js : answer) {
				reversed.add(0,js);
			}
			
			return reversed;
		}
		
		return answer;
	}*/
	
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

			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
				//we need to check to see if the benchId and configId are null, since they might
				//have been deleted while the the job is still pending
				Integer benchId=results.getInt("bench_id");
				if (benchId!=null) {
					jp.setBench(Benchmarks.get(benchId));
				}
				Integer configId=results.getInt("config_id");
				if (configId!=null) {
					jp.setSolver(Solvers.getSolverByConfig(configId,false));
					jp.setConfiguration(Solvers.getConfiguration(configId));
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

				return j;
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
	 * Checks whether the given job is set to "killed" in the database
	 * @param con The open connection to make the call on 
	 * @param jobId The ID of the job in question
	 * @return True if the job is killed (i.e. the killed flag is set to true), false otherwise
	 * @throws Exception 
	 * @author Wyatt Kaiser
	 */
	
	public static boolean isJobKilled(Connection con, int jobId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		 try {
			procedure = con.prepareCall("{CALL IsJobKilled(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			boolean killed=false;
			if (results.next()) {
				killed=results.getBoolean("jobKilled");
			}
			return killed;
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		Connection con=null;
		try {
			con=Common.getConnection();
			
			return isJobKilled(con,jobId);
		} catch (Exception e) {
			log.error("isJobKilled says " +e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Checks whether the given job is set to "paused" in the database
	 * @param con The open connection to make the call on 
	 * @param jobId The ID of the job in question
	 * @return True if the job is paused (i.e. the paused flag is set to true), false otherwise
	 * @throws Exception 
	 * @author Wyatt Kaiser
	 */
	
	public static boolean isJobPaused(Connection con, int jobId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		
		
		try {
			 procedure = con.prepareCall("{CALL IsJobPaused(?)}");
			procedure.setInt(1, jobId);					
			 results = procedure.executeQuery();
			boolean paused=false;
			if (results.next()) {
				paused=results.getBoolean("jobPaused");
			}
			return paused;
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/** 
	 * Determines whether the job with the given ID exists in the database with the column "paused" set to true
	 * @param jobId The ID of the job in question
	 * @return True if the job is paused (i.e. the paused flag is set to true), false otherwise
	 * @author Wyatt Kaiser
	 */
	
	public static boolean isJobPaused(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			
			return isJobPaused(con,jobId);
		} catch (Exception e) {
			log.error("isJobPaused says " +e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
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
				log.debug("Just executed qdel " + sge_id);
				JobPairs.UpdateStatus(jp.getId(), 20);
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
			curSolver.incrementTotalJobPairs();
			curSolver.incrementTime(jp.getCpuUsage());
			if ( statusCode.error()) {
			    curSolver.incrementErrorJobPairs();
			} else if (statusCode.incomplete()) {
			    curSolver.incrementIncompleteJobPairs();
			} else if (statusCode.complete()) {
			    curSolver.incrementCompleteJobPairs();
			    if (jp.getAttributes()!=null) {
			    	Properties attrs = jp.getAttributes();
			    	if (attrs.contains(R.STAREXEC_RESULT) && attrs.contains(R.EXPECTED_RESULT)) {
			    		if (!attrs.get(R.STAREXEC_RESULT).equals(attrs.get(R.EXPECTED_RESULT))) {
			    			curSolver.incrementIncorrectJobPairs();
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
			id=results.getInt("solver.id");
			if (solvers.containsKey(id)) {
				jp.setSolver(solvers.get(id));
			} else {
				solve=new Solver();
				solve.setId(results.getInt("solver.id"));
				solve.setName(results.getString("solver.name"));
				solvers.put(id,solve);
				jp.setSolver(solve);
			}
			id=results.getInt("config.id");
			if (configs.containsKey(id)) {
				jp.setConfiguration(configs.get(id));
			} else {
				config=new Configuration();
				config.setId(results.getInt("config.id"));
				config.setName(results.getString("config.name"));
				configs.put(id, config);
				jp.setConfiguration(config);
			}
			
			Status s = new Status();
			
			s.setCode(results.getInt("status_code"));
			jp.setStatus(s);
			returnList.add(jp);			
			
		}
		
		Common.safeClose(results);
		
		HashMap<Integer,Properties> props=Jobs.getJobAttributes(con,jobId);
		
		for (Integer i =0; i < returnList.size(); i++){
			JobPair jp = returnList.get(i);
			//NOTE: for all new jobs, props should always contain the ID of every job pair
			// that has attributes. The only reason we need to check whether it doesn't is for
			// backwards compatibility-- jobs run before the job_id column was added to the 
			//attributes table will not work with the getJobAttributes method.
			
			if (props.containsKey(jp.getId())) {
				jp.setAttributes(props.get(jp.getId()));
			} else {
				jp.setAttributes(JobPairs.getAttributes(jp.getId()));
			}
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
}