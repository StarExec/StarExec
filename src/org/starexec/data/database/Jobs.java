package org.starexec.data.database;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.starexec.backend.Backend;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobSpace;
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.JobStatus.JobStatusCode;
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
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.LogUtil;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;
import org.starexec.util.Util;

/**
 * Handles all database interaction for jobs (NOT grid engine job execution, see JobManager for that)
 * @author Tyler Jensen
 */

public class Jobs {
	private static final Logger log = Logger.getLogger(Jobs.class);
	private static final LogUtil logUtil = new LogUtil(log);
	
	/**
	 * Returns a list of job spaces that are present in the given
	 * path. Spaces are returned ordered from top level to
	 * bottom level. An exception is thrown if the given
	 * path is null or empty
	 * @param path The / delimited path
	 * @return
	 */
	private static String[] getSpaceNames(String path) throws IllegalArgumentException {
		if (path==null || path=="") {
			throw new IllegalArgumentException("Job paths cannot be empty");
		}
		return path.split(R.JOB_PAIR_PATH_DELIMITER);
	}
	
	/**
	 * Creates a Space object for use during job creation for any job where benchmarks are going to be saved
	 * @param name
	 * @param parent
	 * @return
	 */
	private static Space getNewSpaceForJobCreation(String name,Space parent) {
		Space s=new Space();
		s.setDescription("");
		s.setName(name);
		s.setStickyLeaders(parent.isStickyLeaders());
		s.setPermission(parent.getPermission());
		s.setLocked(parent.isLocked());
		s.setPublic(parent.isPublic());
		return s;
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
			
			//this hashmap maps every job space ID to the maximal number of stages
			// of any pair that is in the hierarchy rooted at the job space
			HashMap<Integer,Integer> idsToMaxStages=new HashMap<Integer,Integer>();
			for (JobPair jp : p) {
				String pathString=jp.getPath();
				if (pathString==null) {
					pathString="job space";
				}
				
				//every entry in a path is a single job space
				String[] path=pathString.split(R.JOB_PAIR_PATH_DELIMITER);
				String key="";
				for (int index=0;index<path.length; index++) {
					
					String spaceName=path[index];
					key=key+R.JOB_PAIR_PATH_DELIMITER+spaceName;
					if (namesToIds.containsKey(key)) {
						int id=namesToIds.get(key);
						if (index==(path.length-1)) { // if this is the last space in the path
							jp.setJobSpaceId(namesToIds.get(key));
						}
						
						idsToMaxStages.put(id, Math.max(idsToMaxStages.get(id), jp.getStages().size()));
						continue; //means we've already added this space
					}
					
					int newJobSpaceId=Spaces.addJobSpace(spaceName, jobId);
					if (index==0) {
						primarySpaceId=newJobSpaceId;
					}
					//otherwise, there was an error getting the new job space
					if (newJobSpaceId>0) {
						idsToMaxStages.put(newJobSpaceId, jp.getStages().size());
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
			for (Integer id : idsToMaxStages.keySet()) {
				Spaces.setJobSpaceMaxStages(id, idsToMaxStages.get(id));
			}
			//there seem to be some jobs with no pairs somehow, so this just prevents
			//a red error screen when viewing them
			if (p.size()==0) {
				primarySpaceId=Spaces.addJobSpace("job space", jobId);
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
	 * Given a set job job pairs, creates a set of spaces that mirrors the job space hierarchy
	 * @param pairs The pairs to use
	 * @param userId The user who will own all the new spaces
	 * @param con An open connection to make SQL calls on
	 * @param parentSpaceId The ID of the parent space to root the hierarchy in
	 * @throws Exception An exception if some space cannot be added
	 */
	public static void createSpacesForPairs(List<JobPair> pairs, int userId, Connection con, int parentSpaceId) throws Exception {
		Space parent=null;
		parent=Spaces.get(parentSpaceId,con);
		parent.setPermission(Permissions.getSpaceDefault(parentSpaceId));
			HashMap<String, Integer> pathsToIds=new HashMap<String,Integer>(); // maps a job space path to a job space id 
			for (JobPair pair : pairs) {
				//log.debug("finding spaces for a new pair with path = " +pair.getPath());
				String[] spaces=getSpaceNames(pair.getPath());
				StringBuilder curPathBuilder=new StringBuilder();
				for (int i=0;i<spaces.length;i++) {
					String name=spaces[i];
					curPathBuilder.append(R.JOB_PAIR_PATH_DELIMITER);
					curPathBuilder.append(name);
					//if we need to create a new space
					if (!pathsToIds.containsKey(curPathBuilder.toString())) {
						String parentPath=curPathBuilder.toString();
						parentPath=parentPath.substring(0,parentPath.lastIndexOf('/'));
						
							// note that it is assumed that there are no name conflicts here. The security check is done outside this function
							int parentId=0;
							if (parentPath.length()>0) {
								parentId=pathsToIds.get(parentPath);
								} else {									
									parentId=parent.getId();
								}
								int newId=Spaces.add(con,getNewSpaceForJobCreation(name,parent), parentId, userId);
								if (newId==-1) {
									throw new Exception("error adding new space-- creating spaces for job failed");
								}
								pathsToIds.put(curPathBuilder.toString(), newId);
							
							
						}
					}
					
				}
			
		}
	
	
	/**
	 * Creates all the job spaces needed for a set of pairs. All pairs must have their paths set and
	 * they must all be rooted at the same space. Upon return, each pair will have its job space id set
	 * to the correct job space
	 * @param pairs The list of pairs to make paths for
	 * @param con The open connection to make calls on
	 * @return The ID of the root job space for this list of pairs, or null on error.
	 * @throws Exception 
	 */
	public static Integer createJobSpacesForPairs(int jobId,List<JobPair> pairs,Connection con) throws Exception {
		
		//this hashmap maps every job space ID to the maximal number of stages
		// of any pair that is in the hierarchy rooted at the job space
		HashMap<Integer,Integer> idsToMaxStages=new HashMap<Integer,Integer>();
		HashMap<String, Integer> pathsToIds=new HashMap<String,Integer>(); // maps a job space path to a job space id 
		int topLevelSpaceId = -1; // -1 indicates that it is not set
		for (JobPair pair : pairs) {
			//log.debug("finding spaces for a new pair with path = " +pair.getPath());
			String[] spaces=getSpaceNames(pair.getPath());
			StringBuilder curPathBuilder=new StringBuilder();
			for (int i=0;i<spaces.length;i++) {
				String jobSpaceName=spaces[i];
				curPathBuilder.append(R.JOB_PAIR_PATH_DELIMITER);
				curPathBuilder.append(jobSpaceName);
				
				//if we need to create a new space
				if (!pathsToIds.containsKey(curPathBuilder.toString())) {
					String parentPath=curPathBuilder.toString();
					parentPath=parentPath.substring(0,parentPath.lastIndexOf(R.JOB_PAIR_PATH_DELIMITER));
						pathsToIds.put(curPathBuilder.toString(),Spaces.addJobSpace(jobSpaceName,jobId,con));
						int id=pathsToIds.get(curPathBuilder.toString());
						if (topLevelSpaceId ==-1) {
							topLevelSpaceId = id;
						}
						idsToMaxStages.put(id, pair.getStages().size());
						//associate the new space to its parent
						
						if (parentPath.length()>0) {
							int parentId=pathsToIds.get(parentPath);
							Spaces.associateJobSpaces(parentId, pathsToIds.get(curPathBuilder.toString()),con);
						}
				}
				int id=pathsToIds.get(curPathBuilder.toString());
				idsToMaxStages.put(id, Math.max(idsToMaxStages.get(id), pair.getStages().size()));					
			}
			pair.setJobSpaceId(pathsToIds.get(curPathBuilder.toString()));
			
		}
		for (Integer id : idsToMaxStages.keySet()) {
			Spaces.setJobSpaceMaxStages(id, idsToMaxStages.get(id),con);
		}
		return topLevelSpaceId;

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
			List<JobPair> pairs=Jobs.getPairsSimple(jobId);
			
			int topLevel=createJobSpacesForPairs(jobId,pairs,con);
			Jobs.updatePrimarySpace(jobId, topLevel,con);
			JobPairs.updateJobSpaces(pairs,con);
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
			con = Common.getConnection();
			
			// gets the name of the root job space for this job
			String rootName=job.getJobPairs().get(0).getPath();
			if (rootName.contains(R.JOB_PAIR_PATH_DELIMITER)) {
				rootName=rootName.substring(0,rootName.indexOf(R.JOB_PAIR_PATH_DELIMITER));
			}
			//start a transaction that encapsulates making new spaces for mirrored hierarchies
			Common.beginTransaction(con);
			//get all the different space IDs for the places we need to created mirrors of the job space heirarchy
			HashSet<Integer> uniqueSpaceIds=new HashSet<Integer>();
			for (StageAttributes attrs: job.getStageAttributes()) {
				if (attrs.getSpaceId()!=null) {
					//make sure that there are no name conflicts when creating the mirrored space hierarchies.
					if (Spaces.getSubSpaceIDbyName(attrs.getSpaceId(), rootName,con)!=-1) {
						throw new Exception("Error creating spaces for job: name conflict with space name "+rootName);
					}
					uniqueSpaceIds.add(attrs.getSpaceId());
				}
			}
			//create mirror space hierarchies for saving benchmarks if the user wishes
			for (Integer i : uniqueSpaceIds) {
				createSpacesForPairs(job.getJobPairs(),job.getUserId(),con,i);
			}
			//we end the first transaction here so that we don't end up keeping a lock on the space tables
			// for the entire duration of job creation
			Common.endTransaction(con);
			//creates the job space hierarchy for the job and returns the ID of the top level job space
		
			log.debug("finished getting subspaces, adding job");
			//the primary space of a job should be a job space ID instead of a space ID
			
			

			Jobs.addJob(con, job);
			job.setPrimarySpace(createJobSpacesForPairs(job.getId(),job.getJobPairs(),con));
			Jobs.updatePrimarySpace(job.getId(), job.getPrimarySpace(),con);
			//NOTE: By opening the transaction here, we are leaving open the possibility that some spaces
			//will be created even if job creation fails. However, this prevents the job space and the space
			//tables from being locked for the entire transaction, which may take a long time.
			Common.beginTransaction(con);
			// record the job being added in the reports table
			Reports.addToEventOccurrencesNotRelatedToQueue("jobs initiated", 1);
			// record the job being added for the queue it was added to
			Reports.addToEventOccurrencesForQueue("jobs initiated", 1, job.getQueue().getName());

			
			log.debug("job added, associating next");
			//put the job in the space it was created in, assuming a space was selected
			if (spaceId>0) {
				Jobs.associate(con, job.getId(), spaceId);
			}
			
			log.debug("job associated, adding this many stage attributes "+job.getStageAttributes().size());
			
			//this times out waiting for a lock if it isn't done after the transaction.
			for (StageAttributes attrs: job.getStageAttributes()) {
				attrs.setJobId(job.getId());
				Jobs.addJobStageAttributes(attrs,con);
			}
			
			log.debug("adding job pairs");
						
			JobPairs.addJobPairs(con, job.getId(),job.getJobPairs());

			Common.endTransaction(con);
			
			log.debug("job added successfully");
			Jobs.resume(job.getId(), con); // now that the job has been added, we can resume
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
			procedure = con.prepareCall("{CALL AddJob(?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)}");
			procedure.setInt(1, job.getUserId());
			procedure.setString(2, job.getName());
			procedure.setString(3, job.getDescription());		
			procedure.setInt(4, job.getQueue().getId());
		
					
			procedure.setInt(5, job.getPrimarySpace());
			procedure.setLong(6,job.getSeed());
			// The procedure will return the job's new ID in this parameter
			procedure.setInt(7, job.getCpuTimeout());
			procedure.setInt(8,job.getWallclockTimeout());
			procedure.setLong(9, job.getMaxMemory());
			procedure.setBoolean(10, job.timestampIsSuppressed());
			procedure.setBoolean(11, job.isUsingDependencies());
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
	
	/**
	 * Counts the number of pairs that occurred before the given completion ID
	 * @param jobId The ID of the job to count pairs for
	 * @param since The completion ID to use as the cutoff
	 * @return The integer number of pairs, or -1 on error
	 */
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
	 * Deletes each job in a list of jobs.
	 * @param jobsToDelete List of jobs to delete.
	 * @author Albert Giegerich
	 */
	public static boolean deleteEach(List<Job> jobsToDelete) {
		Connection con = null; 
		boolean allJobsDeleted = true;
		try {
			con=Common.getConnection();
			for (Job job : jobsToDelete) {
				// Delete the job.
				boolean success = delete(job.getId(), con);
				// If any deletion fails allJobsDeleted will be permanently set to false.
				allJobsDeleted = (allJobsDeleted ? success : false);
				if (!success) {
					log.error("Job with id="+job.getId()+" was not deleted successfully.");
				}
			}
		} catch (Exception e) {
			log.error("Encountered an error while attempting to delete a list of jobs. "+e.getMessage());
			allJobsDeleted = false;
		} finally {
			Common.safeClose(con);
		}
		return allJobsDeleted;
	}

	/**
	 * Deletes all jobs owned by a user.
	 * @param userId Id of user whose jobs are to be deleted.
	 * @throws StarExecDatabaseException if error occurs while interacting with database.
	 * @author Albert Giegerich
	public static void deleteUsersJobs(int userId) throws StarExecDatabaseException {
		// Kill any jobs still running before deletion.
		killUsersJobs(userId);
		Connection con = null;
		CallableStatement procedure=null;
		List<Job> userJobs = Jobs.getByUserId(userId);
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL DeleteUsersJobs(?)");
			procedure.setInt(1, userId);
			procedure.executeUpdate();

			// Delete the user's job directories.
			deleteJobDirectories(userJobs);
		} catch (Exception e) {
			throw new StarExecDatabaseException("Error while trying to delete jobs owned by user.", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	*/

	
	/**
	 * Deletes the job with the given id from disk, and sets the "deleted" column
	 * in the database jobs table to true. 
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
		log.info("Deleting job " + jobId);
		//we should kill jobs before deleting  them so no additional pairs are run
		if (!Jobs.isJobComplete(jobId)) {
			Jobs.kill(jobId);
		}
		CallableStatement procedure = null;
		try {
			// Remove the jobs stats from the database.
			Jobs.removeCachedJobStats(jobId,con);

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
	 * Gets information about the job with the given ID. Job pair information is not returned.
	 * Deleted jobs are not returned.
	 * @param jobId The ID of the job in question
	 * @return The Job object that represents the job with the given ID
	 */
	public static Job get(int jobId) {
		return get(jobId,false);
	}

	/**
	 * Adds the given StageAttributes to the database
	 * @param attrs The attributes object to add
	 * @param con The open connection to make the call on 
	 * @return True on success and false otherwise
	 */
	
	public static boolean addJobStageAttributes(StageAttributes attrs, Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL SetJobStageParams(?,?,?,?,?,?,?,?,?,?)}");
			procedure.setInt(1,attrs.getJobId());
			procedure.setInt(2,attrs.getStageNumber());
			procedure.setInt(3,attrs.getCpuTimeout());
			procedure.setInt(4,attrs.getWallclockTimeout());
			procedure.setLong(5, attrs.getMaxMemory());
			if (attrs.getSpaceId()==null) {
				procedure.setNull(6,java.sql.Types.INTEGER);
			} else {
				procedure.setInt(6, attrs.getSpaceId());
			}
			if (attrs.getPostProcessor()==null) {
				procedure.setNull(7,java.sql.Types.INTEGER);
			} else {
				procedure.setInt(7,attrs.getPostProcessor().getId());
			}
			if (attrs.getPreProcessor()==null) {
				procedure.setNull(8,java.sql.Types.INTEGER);
			} else {
				procedure.setInt(8,attrs.getPreProcessor().getId());
			}
			if (attrs.getBenchSuffix()==null) {
				procedure.setNull(9, java.sql.Types.VARCHAR);
			} else {
				procedure.setString(9, attrs.getBenchSuffix());
			}
			procedure.setInt(10, attrs.getResultsInterval());
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
	 * Adds the given StageAttributes object to the database
	 * @param attrs The attributes object to add
	 * @return True on success and false otherwise
	 */

	public static boolean addJobStageAttributes(StageAttributes attrs) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return addJobStageAttributes(attrs,con);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return false;
		
	}

	/**
	 * Get a job that has job pairs with the simple information included.
	 * @param jobId The id of the job to be gotten
	 * @author Albert Giegerich
	 */
	public static Job getWithSimplePairs(int jobId) {
		return get(jobId, false, true);
	}

	private static Job get(int jobId, boolean includeDeleted, boolean getSimplePairs) {
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
				if (getSimplePairs) {
					j.setJobPairs(getPairsSimple(jobId));
				}
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
				
				j.setDescription(results.getString("description"));
				j.setSeed(results.getLong("seed"));
				j.setStageAttributes(Jobs.getStageAttrsForJob(jobId, con));
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
	 * Gets information about the job with the given ID. Job pair information is not returned
	 * @param jobId The ID of the job in question
	 * @return The Job object that represents the job with the given ID
	 */
	private static Job get(int jobId, boolean includeDeleted) {
		return get(jobId, includeDeleted, false);
	}
	
	/**
	 * Gets all the SolverStats objects for a given job in the given space hierarchy
	 * @param jobId the job in question
	 * @param jobSpaceId The ID of the root space in question
	 * @param stageNumber The ID of the stage to get data for
	 * @return A list containing every SolverStats for the given job where the solvers reside in the given space
	 * @author Eric Burns
	 * @param jobSpaceId The ID of the job space we are getting stats for
	 */

	public static List<SolverStats> getAllJobStatsInJobSpaceHierarchy(JobSpace space, int stageNumber) {
		List<SolverStats> stats=Jobs.getCachedJobStatsInJobSpaceHierarchy(space.getId(),stageNumber);
		//if the size is greater than 0, then this job is done and its stats have already been
		//computed and stored
		if (stats!=null && stats.size()>0) {
			log.debug("stats already cached in database");
			return stats;
		}
		//we will cache the stats only if the job is complete
		boolean isJobComplete=Jobs.isJobComplete(space.getJobId());

		//otherwise, we need to compile the stats
		log.debug("stats not present in database -- compiling stats now");
		List<JobPair> pairs=getJobPairsInJobSpaceHierarchy(space.getId());
		
		
		//compiles pairs into solver stats
		List<SolverStats> newStats=processPairsToSolverStats(pairs);
		for (SolverStats s : newStats) {
			s.setJobSpaceId(space.getId());
		}
		//caches the job stats so we do not need to compute them again in the future
		if (isJobComplete) {
			saveStats(space.getJobId(),newStats);
		}
		
		//next, we simply filter down the stats to the ones for the given stage.
		List<SolverStats> finalStats=new ArrayList<SolverStats>();
		for (SolverStats curStats : newStats) {
			if (curStats.getStageNumber()==stageNumber) {
				finalStats.add(curStats);
			}
		}
		
		return finalStats;
	}

	public static Map<Integer, List<SolverStats>> buildJobSpaceIdToSolverStatsMapWallCpuTimesRounded(Job job, int stageNumber) {
		Map<Integer, List<SolverStats>> outputMap = buildJobSpaceIdToSolverStatsMap(job, stageNumber);
		for (Integer jobspaceId : outputMap.keySet()) {
			List<SolverStats> statsList = outputMap.get(jobspaceId);
			for (SolverStats stats : statsList) {
				stats.setWallTime(Math.round(stats.getWallTime()*100)/100.0);
				stats.setCpuTime(Math.round(stats.getCpuTime()*100)/100.0);
			}
		}
		return outputMap;
	}

	/**
	 * Builds a mapping of job space ID's to the stats for the solvers in that job space.
	 * @param jobId The id of the job that the jobspaces are in.
	 * @param jobspaces the jobspaces to get solver stats for.
	 * @param jobSpaceIdToPairMap a mapping of jobspace ids to the job pairs in that jobspace. Can be gotten with buildJobSpaceIdToJobPairMapForJob.
	 * @see org.starexec.data.database.JobPairs#buildJobSpaceIdToJobPairMapForJob
	 * @param stageNumber The stage to filter solver stats by
	 * @author Albert Giegerich
	 */
	public static Map<Integer, List<SolverStats>> buildJobSpaceIdToSolverStatsMap(Job job, int stageNumber ) {
		int primaryJobSpaceId = job.getPrimarySpace();
		Map<Integer, List<SolverStats>> jobSpaceIdToSolverStatsMap = new HashMap<>();
		List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob(primaryJobSpaceId, true);
		jobSpaces.add(Spaces.getJobSpace(primaryJobSpaceId));
		for (JobSpace jobspace : jobSpaces) {
			int jobspaceId = jobspace.getId();
			List<SolverStats> stats = getAllJobStatsInJobSpaceHierarchy(jobspace, stageNumber);
			jobSpaceIdToSolverStatsMap.put(jobspaceId, stats);
		}
		return jobSpaceIdToSolverStatsMap;
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
	 * Retrieves a job from the database as well as its job pairs that were completed after
	 * "since" and its queue/processor info
	 * 
	 * @param jobId The id of the job to get information for 
	 * @param since The completion ID after which to get job pairs
	 * @return A job object containing information about the requested job, or null on failure
	 * @author Eric Burns
	 */
	public static Job getDetailed(int jobId, int since) {
		return getDetailed(jobId, since, true);
	}

	private static Job getDetailed(int jobId, int since, boolean getCompletedPairsOnly) {
		final String method = "getDetailed";
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
				
				j.setCpuTimeout(results.getInt("cpuTimeout"));
				j.setWallclockTimeout(results.getInt("clockTimeout"));
				j.setMaxMemory(results.getLong("maximum_memory"));
				j.setStageAttributes(Jobs.getStageAttrsForJob(jobId, con));

			}
			else{
				return null;
			}
			
			if (getCompletedPairsOnly) {
				logUtil.debug(method, "Getting job pairs for job with id="+jobId+" since completionID="+since);	
				j.setJobPairs(Jobs.getNewCompletedPairsDetailed(j.getId(), since));
			} else {
				j.setJobPairs(Jobs.getAllPairs(jobId));
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
	 * Gets a status description of a stage.
	 * @param stage the stage to get the status code from.
	 * @return the status code of the input stage.
	 * @author Albert Giegerich
	 */
	public static String getStatusFromStage(JoblineStage stage) {
		// 0 for solved 1 for wrong
		int correctCode = JobPairs.isPairCorrect(stage);
		StatusCode statusCode = stage.getStatus().getCode();
		if (correctCode == 0) {
			return "solved";
		} else if (correctCode == 1) {
			return "wrong";
		} else if (statusCode.statIncomplete()) {
			return "incomplete";
		} else if (statusCode.failed()) {
			return "failed";
		} else if (statusCode.resource()) {
			// Resources (time/memory) ran out.
			return "resource";
		} else {
			return "unknown";
		}
	}
	
	/**
	 * Returns the filepath to the directory containing this job's output
	 * @param jobId The job to get the filepath for
	 * @return A string representing the path to the output directory
	 * @author Eric Burns
	 */
	
	public static String getDirectory(int jobId) {
		// The job's output is expected to be in NEW_JOB_OUTPUT_DIR/{job id}/
		File file=new File(R.getJobOutputDirectory(),String.valueOf(jobId));
		return file.getAbsolutePath();
	}
	
	/**
	 * Returns the absolute path to the directory containing all the log files for the given job
	 * @param jobId The ID of the job to get the log path for
	 * @return The absolute path as a string
	 */
	public static String getLogDirectory(int jobId) {
		// The job's output is expected to be in NEW_JOB_OUTPUT_DIR/{job id}/
		File file=new File(R.getJobLogDir(),String.valueOf(jobId));
		return file.getAbsolutePath();
	}
	
	/**
	 * Gets all enqueued job pairs. Only populates the pair ID and the sge ID!

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
			
			while(results.next()){
				JobPair jp = new JobPair();
				jp.setId(results.getInt("id"));
				jp.setBackendExecId(results.getInt("sge_id"));
				
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
	 * Gets all enqueued job pairs. Only populates the pair ID and the sge ID!
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
	 * Retrieves the given job, even if it has been marked as "deleted" in the database.
	 * Deep data like job pairs are not populated
	 * @param jobId The ID of the job to retrieve
	 * @return The job if it could be found, or null if it could not
	 */
	public static Job getIncludeDeleted(int jobId) {
		return get(jobId,true);
	}
	
	
	/**
	 * Gets all the the attributes for every job pair in a job, and returns a HashMap
	 * mapping pair IDs a map of their stages to the stage's attributes
	 * @param con The connection to make the query on
	 * @param The ID of the job to get attributes of
	 * @return A HashMap mapping pair IDs to properties. Some values may be null
	 * @author Eric Burns
	 */
	protected static HashMap<Integer,HashMap<Integer,Properties>> getJobAttributes(Connection con, int jobId) throws Exception {
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
	 * @return A HashMap mapping integer job-pair IDs to hashmaps that themselves map jobpair_stage_data ids
	 * to Properties for that stage
	 * @author Eric Burns
	 */
	public static HashMap<Integer,HashMap<Integer,Properties>> getJobAttributes(int jobId) {
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
	 * Gets the number of Jobs in the whole system
	 * @return The number of jobs in the system
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
	 * Returns the number of job pairs that exist for a given job in a given space that
	 * have the given stage
	 * 
	 * @param jobId the id of the job to get the number of job pairs for
	 * @param jobSpaceId The ID of the job space containing the paris to count
	 * @param Whether to count all job pairs in the hierarchy rooted at the job space with the given id
	 * @return the number of job pairs for the given job or -1 on failure
	 * @author Eric Burns
	 * @param stageNumber The stage number. If <=0, means the primary stage
	 */
	public static int getJobPairCountInJobSpaceByStage(int jobSpaceId, int stageNumber) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetJobPairCountInJobSpace(?,?)}");
			
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,stageNumber);
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
	 * @param stageNumber The stage number to consider
	 * @return the number of job pairs for the given job
	 * @author Eric Burns
	 */
	public static int getJobPairCountInJobSpaceByStage(int jobSpaceId, String query, int stageNumber) {
		Connection con = null;
		ResultSet results=null;
		CallableStatement procedure = null;
		int jobPairCount=0;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetJobPairCountByJobInJobSpaceWithQuery(?, ?,?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setString(2, query);
			procedure.setInt(3, stageNumber);
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
	 * all the job pairs are in the given job space hierarchy and were operated on by the configuration with the given config ID
	 * @param query A DataTablesQuery object
	 * @param jobSpaceId The ID of the root job space of the job space hierarchy to get data for
	 * @param configId1 The ID of the first configuration of the comparision 
	 * @param configId2 The ID of the second configuraiton of the comparison
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @param stageNumber The stage number ot use for the comparison
	 * @param totals A size 2 int array that, upon return, will contain in the first slot the total number
	 * of pairs and in the second slot the total number of pairs after filtering
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object 
	 * @author Eric Burns
	 */
	public static List<SolverComparison> getSolverComparisonsForNextPageByConfigInJobSpaceHierarchy(DataTablesQuery query,
			int jobSpaceId, int configId1, int configId2, int[] totals, boolean wallclock, int stageNumber) {
		List<JobPair> pairs=Jobs.getJobPairsInJobSpaceHierarchy(jobSpaceId);
		List<JobPair> pairs1=new ArrayList<JobPair>();
		List<JobPair> pairs2=new ArrayList<JobPair>();
		for (JobPair jp : pairs) {
			JoblineStage stage=jp.getStageFromNumber(stageNumber);
			if (stage==null || stage.isNoOp()) {
				continue;
			}
			if (stage.getConfiguration().getId()==configId1) {
				pairs1.add(jp);
			} else if (stage.getConfiguration().getId()==configId2) {
				pairs2.add(jp);
			}
		}
		pairs1=JobPairs.filterPairsByType(pairs1, "complete",stageNumber);
		pairs2=JobPairs.filterPairsByType(pairs2, "complete",stageNumber);
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
		comparisons=JobPairs.filterComparisons(comparisons, query.getSearchQuery());

		totals[1]=comparisons.size();
		SolverComparisonComparator compare=new SolverComparisonComparator(query.getSortColumn(),wallclock,query.isSortASC(),stageNumber);
		return Util.handlePagination(comparisons, compare, query.getStartingRecord(), query.getNumRecords());

	}
	
	/**
	 * Given a list of job pairs, filters and sorts them according to the given parameters and returns the
	 * set to display
	 * @param pairs The pairs to filter and sort
	 * @param query Parameters from data table describing which pairs to get in which order
	 * @param type The "type" to filter by, where the type refers to the different columns of the solver stats table
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @param stageNumber The stage number containing the relevant data, or 0 for the primary stage
	 * @param totals A size 2 array that, on exit, will contain the total number of pairs after filtering by type and
	 * the total number of pairs after filtering by the query
	 * @return The list of job pairs to display in the next page
	 */
	public static List<JobPair> getJobPairsForNextPage(List<JobPair> pairs,DataTablesQuery query, String type, boolean wallclock, int stageNumber,int[]totals){
		pairs=JobPairs.filterPairsByType(pairs, type,stageNumber);

		totals[0]=pairs.size();
		
		pairs=JobPairs.filterPairs(pairs, query.getSearchQuery(),stageNumber);

		totals[1]=pairs.size();
		int indexOfColumnSortedBy = query.getSortColumn();
		if (!wallclock && indexOfColumnSortedBy==4) {
			indexOfColumnSortedBy=8;
		}
		JobPairComparator compare=new JobPairComparator(indexOfColumnSortedBy,stageNumber,query.isSortASC());
		List<JobPair> finalPairs= Util.handlePagination(pairs, compare, query.getStartingRecord(), query.getNumRecords());

		return finalPairs;
	}
	
	
	/**
	 * Returns a count of the number of job pairs that satisfy the requirements of the given attributes
	 * @param jobSpaceId The ID of the job space the pairs must be in 
	 * @param configId The ID of the configuration the pairs must be using during the given stage
	 * @param type The "type" of the pairs as defined by the columns of the solver stats table
	 * @param stageNumber The stage number of the stage to check
	 * @return The integer number of pairs, or -1 on error
	 */
	public static int getCountOfJobPairsByConfigInJobSpaceHierarchy(int jobSpaceId,int configId, String type, int stageNumber) {
		return getCountOfJobPairsByConfigInJobSpaceHierarchy(jobSpaceId,configId,type,"",stageNumber);
	}
	
	/**
	 * Counts the number of job pairs that are in a given job space and use the given configuration and are also of the given
	 * "type", which here corresponds to the different columns on the solver stats table in the job details page
	 * @param jobSpaceId The ID of the job space to get pairs for
	 * @param configId The ID of the configuration we are concerned with 
	 * @param type The "type", defined as in the different columns in the solver stats table 
	 * @param query A query to filter the columns by
	 * @param stageNumber The stage number to check
	 * @return The total number of pairs that satisfy the given attributes, or -1 on error
	 */
	public static int getCountOfJobPairsByConfigInJobSpaceHierarchy(int jobSpaceId,int configId, String type, String query, int stageNumber) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL CountJobPairsInJobSpaceHierarchyByType(?,?,?,?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2,configId);
			procedure.setString(3,type);
			procedure.setString(4,query);
			procedure.setInt(5,stageNumber);
			results = procedure.executeQuery();
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
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object, where
	 * all the job pairs are in the given space and were operated on by the configuration with the given config ID in the given stage
	 * @param jobSpaceId The job space that contains the job pairs
	 * @param configId The ID of the configuration responsible for the job pairs
	 * @param totals A size 2 int array that, upon return, will contain in the first slot the total number
	 * of pairs and in the second slot the total number of pairs after filtering
	 * @param type The type of the pairs, as defined by the columns of the solver stats table
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @param stageNumber The stage number to get data for
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object 
	 * @author Eric Burns
	 */
	//TODO: This function is not working correctly somehow: it is just ignoring the wallclock boolean
	public static List<JobPair> getJobPairsForNextPageByConfigInJobSpaceHierarchy(DataTablesQuery query,int jobSpaceId, int configId, String type, boolean wallclock, int stageNumber) {
		
		return Jobs.getJobPairsForTableInJobSpaceHierarchy(jobSpaceId, query, configId, stageNumber, type);		
	}
	
	/**
	 * If the given string is null, returns a placeholder string. Otherwise, returns the given string
	 * @param value The string to check
	 * @return The given string unless it is null, and -- otherwise
	 */
	public static String getPropertyOrPlaceholder(String value) {
		if (value==null) {
			return "--";
		}
		return value;
	}
	
	/**
	 * Gets all the JobPairs in a given job space that were solved by every solver/configuration pair in that space
	 * @param jobSpaceId The ID of the job space to get the pairs for
	 * @param stageNumber The stage number to get data for
	 * @return All the job pairs in the given job space that are "synchronized" as defined above
	 */
	public static List<JobPair> getSynchronizedPairsInJobSpace(int jobSpaceId,int stageNumber) {
	
		HashSet<String> solverConfigPairs=new HashSet<String>(); // will store all the solver/configuration pairs so we know how many there are
		HashMap<Integer, Integer> benchmarksCount=new HashMap<Integer,Integer>(); //will store the number of pairs every benchmark has
		List<JobPair> pairs=new ArrayList<JobPair>();
		try {
			//first, get all the completed pairs in the space
			pairs=Jobs.getJobPairsInJobSpace(jobSpaceId,stageNumber);
			pairs=JobPairs.filterPairsByType(pairs, "complete", 1); //1 because we get only one stage above
			
			//then, filter them down to the synced pairs
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
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}
	/**
	 * Gets the JobPairs necessary to make the next page of a DataTable of synchronized job pairs in a specific job space
	 * @param query Parameters from data table describing which pairs to get in which order

	 * @param jobSpaceId The ID of the job space containing the pairs
	 * @param wallclock True if we are using wallclock time and false to use CPU time
	 * @param stageNumber The stage number to get results for
	 * @param totals Must be a size 2 array. The first slot will have the number of results before the query, and the second slot will have the number of results after the query
	 * @return The job pairs needed to populate the page
	 */
	public static List<JobPair> getSynchronizedJobPairsForNextPageInJobSpace(DataTablesQuery query, int jobSpaceId, boolean wallclock,int stageNumber, int[] totals) {
		List<JobPair> pairs=Jobs.getSynchronizedPairsInJobSpace(jobSpaceId,stageNumber);
		return getJobPairsForNextPage(pairs,query,"all",wallclock,stageNumber,totals);
	}
	
	/**
	 * Given the index of a column in the job pairs table on the client side, returns the name of the SQL
	 * column we need to sort by
	 * @param orderIndex The index of the client side datatable column we are sorting on
	 * @param wallclock Whether to use wallclock time or cpu time if we are sorting on time.
	 * @return The SQL column name
	 */
	private static String getJobPairOrderColumn(int orderIndex, boolean wallclock) {
		if (orderIndex==0) {
			return "job_pairs.bench_name";
		} else if (orderIndex==1) {
			return "jobpair_stage_data.solver_name";
		} else if (orderIndex==2) {
			return "jobpair_stage_data.config_name";
		} else if (orderIndex==3) {
			return "jobpair_stage_data.status_code";
		} else if (orderIndex==4) {
			if (wallclock) {
				return "jobpair_stage_data.wallclock";
			} else {
				return "jobpair_stage_data.cpu";
			}
		} else if (orderIndex==5) {
			return "result";
		} else if (orderIndex==6) {
			return "job_pairs.id";
		} else if (orderIndex==7) {
			// the - sign is because we want null values last, so we reverse the ASC/ DESC sign and add a -
			return "-completion_id";
		}
		
		return "job_pairs.benchmark_name";
	}
	
	/**
	 * Gets the minimal number of Job Pairs necessary in order to service the client's
	 * request for the next page of Job Pairs in their DataTables object
	 * @param query Parameters from data table describing which pairs to get in which order
	 * @return a list of 10, 25, 50, or 100 Job Pairs containing the minimal amount of data necessary
	 * @param jobSpaceId The ID of the job space containing the pairs in question
	 * @param stageNumber The stage number to get data for
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @author Todd Elvers
	 */
	
	public static List<JobPair> getJobPairsForNextPageInJobSpace(DataTablesQuery query, int jobSpaceId, int stageNumber,boolean wallclock) {
		Connection con = null;	
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		String searchQuery = query.getSearchQuery();
		if (searchQuery==null) {
			searchQuery="";
		}
		int jobId = Spaces.getJobSpace(jobSpaceId).getId();
		try {
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_PAIRS_IN_SPACE_QUERY, getJobPairOrderColumn(query.getSortColumn(),wallclock), query);
			con = Common.getConnection();
			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setString("query", searchQuery);
			procedure.setInt("stageNumber",stageNumber);
			procedure.setInt("jobSpaceId",jobSpaceId);
			results = procedure.executeQuery();
			List<JobPair> jobPairs = getJobPairsForDataTable(jobId,results,false,false);
			
			return jobPairs;
		} catch (Exception e){			
			log.error("get JobPairs for Next Page of Job space " + jobSpaceId + " says " + e.getMessage(), e);
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
	 * @param pairs The pairs that have stages contained in the given result set
	 * @param results The ResultSet containing stages
	 * @param getExpectedResult True to include the expected result column and false otherwise
	 * @return True if the pairs had their stages populated correctly and false otherwise
	 */
	public static boolean populateJobPairStages(List<JobPair> pairs, ResultSet results, boolean getExpectedResult) {
		
		HashMap<Integer,Solver> solvers=new HashMap<Integer,Solver>();
		HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
		Integer id;
		Solver solve=null;
		Configuration config=null;
		HashMap<Integer,JobPair> idsToPairs = new HashMap<Integer,JobPair>();
		try {
			for(JobPair pair : pairs) {
				idsToPairs.put(pair.getId(), pair);
			}
			
			//every row in this resultset is a single stage 
			while (results.next()) {
				
				JobPair jp=idsToPairs.get(results.getInt("job_pairs.id"));
				if (jp==null) {
					log.error("could not get a pair for id = "+results.getInt("job_pairs.id"));
					log.error("id found in mapping = "+idsToPairs.containsKey(results.getInt("job_pairs.id")));
					continue;
				}
				JoblineStage stage=new JoblineStage();
				stage.setStageNumber(results.getInt("stage_number"));
				stage.setCpuUsage(results.getDouble("jobpair_stage_data.cpu"));
				stage.setWallclockTime(results.getDouble("jobpair_stage_data.wallclock"));
				stage.setStageId(results.getInt("jobpair_stage_data.stage_id"));
				stage.getStatus().setCode(results.getInt("jobpair_stage_data.status_code"));
				stage.setMaxVirtualMemory(results.getDouble("max_vmem"));
				//everything below this line is in a stage
				id=results.getInt("jobpair_stage_data.solver_id");
				//means it was null in SQL
				if (id==0) {
					stage.setNoOp(true);
				} else {
					if (!solvers.containsKey(id)) {
						
						solve=new Solver();
						solve.setId(id);
						solve.setName(results.getString("jobpair_stage_data.solver_name"));
						solvers.put(id,solve);
					}
					stage.setSolver(solvers.get(id));
					
					
					
					id=results.getInt("jobpair_stage_data.config_id");
					
					if (!configs.containsKey(id)) {
						config=new Configuration();
						config.setId(id);
						config.setName(results.getString("jobpair_stage_data.config_name"));
						configs.put(id, config);
					}
					stage.getSolver().addConfiguration(configs.get(id));
					stage.setConfiguration(configs.get(id));
					
					Properties p=new Properties();
					String result=results.getString("result");
					if (result!=null) {
						p.put(R.STAREXEC_RESULT, result);
					}
					if (getExpectedResult) {
						String expected=results.getString("expected");
						if (expected!=null) {
							p.put(R.EXPECTED_RESULT, expected);

						}
					}
					
					stage.setAttributes(p);
					
					
				}
				
				
				jp.addStage(stage);

			}

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return false;
	}
	
	/**
	 * Returns all of the job pairs in a given job space, populated with all the fields necessary
	 * to display in a SolverStats table. Only the given stage is returned
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param stageNumber The stage number to get data for
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsInJobSpace(int jobSpaceId, int stageNumber) {
		
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("called getJobPairsInJobSpace with jobSpaceId = "+ jobSpaceId);
		try {
			long a=System.currentTimeMillis();
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsInJobSpace(?,?)}");
			
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,stageNumber);
			results = procedure.executeQuery();
			log.debug("executing query 1 took "+(System.currentTimeMillis()-a));
			List<JobPair> pairs=processStatResults(results,true);
			log.debug("processing query 1 took "+(System.currentTimeMillis()-a));

		
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
	 * Returns all of the job pairs in a given job space hierarchy, populated with all the fields necessary
	 * to display in a SolverStats table. All job pair stages are obtained
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsInJobSpaceHierarchy(int jobSpaceId) {
		return getJobPairsInJobSpaceHierarchy(jobSpaceId,null);
	}
	
	
	/**
	 * Returns all of the job pairs in a given job space hierarchy, populated with all the fields necessary
	 * to display in a SolverStats table. All job pair stages are obtained. 
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param since If null, all pairs in the hierarchy are returned. Otherwise, only pairs that have a completion
	 * ID greater than since are returned
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsInJobSpaceHierarchy(int jobSpaceId, Integer since) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("called with jobSpaceId = "+ jobSpaceId);
		try {
			Spaces.updateJobSpaceClosureTable(jobSpaceId);

			con=Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsInJobSpaceHierarchy(?,?)}");
			
			procedure.setInt(1,jobSpaceId);
			if (since==null) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else  {
				procedure.setInt(2,since);
			}
			results = procedure.executeQuery();
			
			List<JobPair> pairs=processStatResults(results,false);
			
			
			
			Common.safeClose(procedure);
			Common.safeClose(results);
			procedure=con.prepareCall("{CALL GetJobPairStagesInJobSpaceHierarchy(?,?)}");
			procedure.setInt(1,jobSpaceId);
			if (since==null) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else  {
				procedure.setInt(2,since);
			}
			results=procedure.executeQuery();
			if (populateJobPairStages(pairs,results,true)) {
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
	 * for fields needed in a job pairs table. Populates exactly 1 stage, whichever was returned by the query
	 * @param jobId The ID of the job containing all these pairs
	 * @param results
	 * @return The list of job pairs or null on failure
	 */
	
	private static List<JobPair> getJobPairsForDataTable(int jobId,ResultSet results, boolean includeExpected, boolean includeCompletion) {
		List<JobPair> pairs = new ArrayList<JobPair>();
		try{
			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("id"));
				JoblineStage stage=new JoblineStage();
				stage.setWallclockTime(results.getDouble("jobpair_stage_data.wallclock"));
				stage.setCpuUsage(results.getDouble("jobpair_stage_data.cpu"));
				stage.setStageNumber(results.getInt("jobpair_stage_data.stage_number"));
				jp.addStage(stage);
				Benchmark bench = jp.getBench();
				bench.setId(results.getInt("bench_id"));
				bench.setName(results.getString("bench_name"));
				
				jp.getPrimarySolver().setId(results.getInt("jobpair_stage_data.solver_id"));
				jp.getPrimarySolver().setName(results.getString("jobpair_stage_data.solver_name"));
				jp.getPrimaryConfiguration().setId(results.getInt("jobpair_stage_data.config_id"));
				jp.getPrimaryConfiguration().setName(results.getString("jobpair_stage_data.config_name"));
				jp.getPrimarySolver().addConfiguration(jp.getPrimaryConfiguration());
				
				Status status = stage.getStatus();
				status.setCode(results.getInt("jobpair_stage_data.status_code"));
				
				
				Properties attributes = jp.getPrimaryStage().getAttributes();
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
	 * Gets all the job pairs necessary to view in a datatable for a job space. All job pairs returned
	 * use the given configuration in the given stage
	 * @param jobSpaceId The id of the job_space id in question
	 * @param query a DataTablesQuery object
	 * @param configId The ID of the configuration to filter pairs by
	 * @param stageNumber The stage number to get pairs by
	 * @param type The "type" of the pairs, where type is defined by the columns of the solver stats table
	 * @return The job pairs to use in the next page of the table
	 */

	public static List<JobPair> getJobPairsForTableInJobSpaceHierarchy(int jobSpaceId,DataTablesQuery query,int configId, int stageNumber,String type) {
		final String method = "getJobPairsForTableInJobSpaceHierarchy";
		logUtil.entry(method);
		Connection con = null;	
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		if (query.getSearchQuery()==null) {
			query.setSearchQuery("");
		}
		int jobId = Spaces.getJobSpace(jobSpaceId).getJobId();
		try {
			
			con = Common.getConnection();
			
			if (query.getSortColumn() == 7) {
				query.setSortASC(!query.isSortASC());
			}
			
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_PAIRS_IN_SPACE_HIERARCHY_QUERY, getJobPairOrderColumn(query.getSortColumn(),false), query);
			
			String constructedSQL = builder.getSQL();

			procedure = new NamedParameterStatement(con,constructedSQL);
				
			
			procedure.setString("query", query.getSearchQuery());
			procedure.setInt("jobSpaceId",jobSpaceId);
			procedure.setInt("stageNumber",stageNumber);
			procedure.setInt("configId",configId);
			procedure.setString("pairType",type);
			results = procedure.executeQuery();
			List<JobPair> jobPairs = getJobPairsForDataTable(jobId,results,false,false);

			return jobPairs;
		} catch (Exception e){			
			log.error("get JobPairs for Next Page of Job " + jobId + " says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
			logUtil.exit(method);
		}

		return null;
	}

	/**
	 * Gets job pair information necessary for populating client side graphs
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The ID of the job_space in question
	 * @param configId The ID of the configuration in question
	 * @return A List of JobPair objects
	 * @author Eric Burns
	 * @param stageNumber The number of the stage that we are concerned with. If <=0, the primary stage is obtained
	 */
	
	//TODO: Rewrite so this takes in two config IDs instead of one: we are using two database calls where only one
	// is needed
	public static List<JobPair> getJobPairsForSolverComparisonGraph(int jobSpaceId, int configId, int stageNumber) {
		try {			
			List<JobPair> pairs = Jobs.getJobPairsInJobSpaceHierarchy(jobSpaceId);
			List<JobPair> filteredPairs=new ArrayList<JobPair>();
			
			for (JobPair jp : pairs) {
				
				JoblineStage stage=jp.getStageFromNumber(stageNumber);
				
				if (stage==null || stage.isNoOp()) {
					continue;
				}
				if (stage.getConfiguration().getId()==configId) {
					filteredPairs.add(jp);
				}
				
			}
			return filteredPairs;
			
		
		}catch (Exception e) {
			log.error("getJobPairsShallowByConfigInJobSpace says " +e.getMessage(),e);
		} finally {
			
		}
		return null;
	}
	/**
	 * Returns the count of all pairs in a job
	 * @param jobId The ID of the job to count pairs for
	 * @return The number of pairs in the job
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
	 * Gets the name of the SQL column to sort on given an index of a dataTables column
	 * from the front end
	 * @param orderIndex
	 * @return
	 */
	private static String getJobOrderColumn(int orderIndex) {
		if (orderIndex==0) {
			return "jobs.name";
		} else if (orderIndex==1) {
			return "pendingPairs"; // this is the same as ordering by status, as the status is determined by whether a job has pending pairs
		} else if (orderIndex==2) {
			return "completePairs";
		} else if (orderIndex==3) {
			return "totalPairs";
		} else if (orderIndex==4) {
			return "errorPairs";
		} else if (orderIndex==5) {
			return "created";
		}
		return "jobs.name";
	}
	
	/**
	 * Get next page of the jobs belong to a specific user
	 * @param query a DataTablesQuery object
	 * @param userId Id of the user we are looking for
	 * @return a list of Jobs belong to the user
	 * @author Ruoyu Zhang
	 */
	public static List<Job> getJobsByUserForNextPage(DataTablesQuery query, int userId) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con =Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_JOBS_BY_USER_QUERY, getJobOrderColumn(query.getSortColumn()), query);
			procedure = new NamedParameterStatement(con,builder.getSQL());
			procedure.setString("query",query.getSearchQuery());
			procedure.setInt("userId",userId);
			results = procedure.executeQuery();
			return getJobsForNextPage(results);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;		
	}
	
	/**
	 * Get next page of the jobs belong to a space, or in the whole system if the ID is -1
	 * @param query A DataTablesQuery object
	 * @param spaceId Id of the space we are looking for. If -1, all jobs in the entire system are returned (for admin page)
	 * @return a list of Jobs belong to the user
	 * @author Ruoyu Zhang
	 */
	
	public static List<Job> getJobsForNextPage(DataTablesQuery query, int spaceId) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con =Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_JOBS_IN_SPACE_QUERY, getJobOrderColumn(query.getSortColumn()), query);
			procedure = new NamedParameterStatement(con,builder.getSQL());
			procedure.setString("query",query.getSearchQuery());
			procedure.setInt("spaceId",spaceId);
			results = procedure.executeQuery();
			return getJobsForNextPage(results);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
		
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
	private static List<Job> getJobsForNextPage(ResultSet results) {
		
		try {
			
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
			
		}

		return null;
	}

	/**Gets the minimal number of Jobs necessary in order to service the client's 
	 * request for the next page of Users in their DataTables object
	 * 
	 * @param query A DataTablesQuery object 
	 * 
	 * @return a list of 10, 25, 50, or 100 Users containing the minimal amount of data necessary
	 * @author Wyatt Kaiser
	 **/
	public static List<Job> getJobsForNextPageAdmin(DataTablesQuery query) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con =Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_INCOMPLETE_JOBS_QUERY, getJobOrderColumn(query.getSortColumn()), query);
			procedure = new NamedParameterStatement(con,builder.getSQL());
			procedure.setString("query",query.getSearchQuery());
			results = procedure.executeQuery();
			return getJobsForNextPage(results);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
    
	/**
	 * Attempts to retrieve cached SolverStats objects from the database. Returns
	 * an empty list if the stats have not already been cached.
	 * @param jobSpaceId The ID of the root job space for the stats
	 * @param stageNumber The number of the stage to get data for
	 * @return A list of the relevant SolverStats objects in this space
	 * @author Eric Burns
	 */
	
	public static List<SolverStats> getCachedJobStatsInJobSpaceHierarchy(int jobSpaceId, int stageNumber) {
		log.debug("calling GetJobStatsInJobSpace with jobspace = "+jobSpaceId + " and stage = "+stageNumber);
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetJobStatsInJobSpace(?,?)}");
			procedure.setInt(1,jobSpaceId);
			procedure.setInt(2,stageNumber);
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
				s.setStageNumber(results.getInt("stage_number"));
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

	public static Job getJobForMatrix(int jobId) {
		return getDetailed(jobId, 0, false);
	}

	private static List<JobPair> getAllPairs(int jobId) {
		final String method = "getAllPairs";
		Connection con = null;	
		ResultSet results=null;
		CallableStatement procedure = null;

		try {			
			con = Common.getConnection();	
			
			logUtil.debug(method, "Getting all detailed pairs for job " + jobId);
			
			procedure = con.prepareCall("{CALL GetAllJobPairsByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<JobPair> jobPairs= getPairsDetailed(jobId,con,results,false);

			return jobPairs;
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
	 * Gets all job pairs for the given job that have been completed after a given point and also
	 * populates its resource TOs. Gets only the primary stage
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
			HashMap<Integer,HashMap<Integer,Properties>> props=Jobs.getNewJobAttributes(con,jobId,since);
			
			for (Integer i =0; i < pairs.size(); i++){
				JobPair jp = pairs.get(i);
				if (props.containsKey(jp.getId())) {
					HashMap<Integer,Properties> pairInfo=props.get(jp.getId());
					if (pairInfo.containsKey(jp.getPrimaryStage().getStageNumber())) {
						jp.getPrimaryStage().setAttributes(pairInfo.get(jp.getPrimaryStage().getStageNumber()));
					}
				} 
				// Add the pair's benchmark's expected result to the pair's attributes.
				TreeMap<String,String> jpBenchProps = Benchmarks.getSortedAttributes(jp.getBench().getId());
				// Make sure the benchmark properties has an expected result
				if (jpBenchProps.containsKey(R.EXPECTED_RESULT)) {
					String expectedResult = jpBenchProps.get(R.EXPECTED_RESULT);
					List<JoblineStage> jpStages = jp.getStages();
					for (JoblineStage stage : jpStages) {
						// Set all the stages expected results to the benchmark's expected result.
						stage.getAttributes().setProperty(R.EXPECTED_RESULT, expectedResult);
					}
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
	 * to find the job pair output on disk. Only the primary stage is required
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
				JoblineStage stage=new JoblineStage();
				stage.setStageNumber(results.getInt("primary_jobpair_data"));
				pair.setPrimaryStageNumber(results.getInt("primary_jobpair_data"));
				pair.setJobId(jobId);
				pair.setId(results.getInt("id"));
				pair.setPath(results.getString("path"));
				stage.getSolver().setName(results.getString("solver_name"));
				stage.getConfiguration().setName(results.getString("config_name"));
				pair.getBench().setName(results.getString("bench_name"));
				pair.setCompletionId(results.getInt("completion_id"));
				pair.addStage(stage);

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
	
	protected static HashMap<Integer,HashMap<Integer,Properties>> getNewJobAttributes(Connection con, int jobId,Integer completionId) {
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
	public static HashMap<Integer,HashMap<Integer, Properties>> getNewJobAttributes(int jobId, int completionId) {
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
			    stage.setStageNumber(results.getInt("stage_number"));
			    jp.setPrimaryStageNumber(results.getInt("stage_number"));
			    Configuration c=new Configuration();
			    Solver s=new Solver();
			    stage.setConfiguration(c);
			    s.addConfiguration(c);
			    stage.setSolver(s);
			    jp.addStage(stage);
			    jp.setId(results.getInt("id"));
				jp.setJobSpaceId(results.getInt("job_pairs.job_space_id"));
			    jp.getStatus().setCode(results.getInt("job_pairs.status_code"));
			    jp.getBench().setId(results.getInt("job_pairs.bench_id"));
			    jp.getBench().setName(results.getString("job_pairs.bench_name"));
			    c.setId(results.getInt("jobpair_stage_data.config_id"));
			    c.setName(results.getString("jobpair_stage_data.config_name"));
			    s.setId(results.getInt("jobpair_stage_data.solver_id"));
			    s.setName(results.getString("jobpair_stage_data.solver_name"));
			    jp.getSpace().setName(results.getString("name"));
			    jp.getSpace().setId(results.getInt("job_spaces.id"));
			    jp.setPath(results.getString("path"));
			    int pipeId=results.getInt("pipeline_id");
			    if (pipeId>0) {
			    	SolverPipeline pipe=new SolverPipeline();
			    	pipe.setName(results.getString("solver_pipelines.name"));
			    	jp.setPipeline(pipe);
			    } else {
			    	jp.setPipeline(null);
			    }
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
	 *
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
			
			procedure = con.prepareCall("{CALL GetJobPairsPrimaryStageByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<JobPair> pairs= getPairsDetailed(jobId,con,results,false);
			HashMap<Integer,HashMap<Integer,Properties>> props=Jobs.getJobAttributes(con,jobId);
			for (Integer i =0; i < pairs.size(); i++){
				JobPair jp = pairs.get(i);
				if (props.containsKey(jp.getId())) {
					HashMap<Integer,Properties> pairInfo=props.get(jp.getId());
					if (pairInfo.containsKey(jp.getPrimaryStage().getStageNumber())) {
						jp.getPrimaryStage().setAttributes(pairInfo.get(jp.getPrimaryStage().getStageNumber()));

					}
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
	 * only the job pairs that have been completed after the argument "since" Only primary stages are populated
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
					Solver solver= Solvers.resultToSolver(results,R.SOLVER);				
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
	 * @param jobId The ID of the job to perform the operation for
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
				success=success && Jobs.rerunPair(jp);
			}

			return success;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false;
		
	}
	
	/**
	 * Sets every pair in a job back to pending, allowing all pairs to be rerun
	 * @param jobId The ID of the job to reset the pairs for
	 * @return True on success and false otherwise
	 */
	public static boolean setAllPairsToPending(int jobId) {

		try {
			List<JobPair> pairs=Jobs.getPairsSimple(jobId);
			boolean success=true;
			for (JobPair jp : pairs) {
				success = success && Jobs.rerunPair(jp.getId());
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
	 * @param pairId The ID of the pair to rerun
	 * @return True on success and false otherwise
	 */
	
	public static boolean rerunPair(int pairId) {
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
				JobPairs.killPair(pairId, p.getBackendExecId());
			}
			JobPairs.removePairFromCompletedTable(pairId);
			JobPairs.setPairStatus(pairId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
			JobPairs.setAllPairStageStatus(pairId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
			// the cache must be cleared AFTER changing the pair status code!
			success=success && Jobs.removeCachedJobStats(p.getJobId());
			
			return success;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
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
	
	
	/**
	 * Gets all job pair IDs of pairs that have the given status code in the given job, ordered by ID
	 * @param jobId The ID of the job in question
	 * @param statusCode The ID of the Status to get the pairs of 
	 * @return A list of job pair IDs, or null on error
	 */
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
				success=success && Jobs.rerunPair(id);
			}
			return success;
		} catch (Exception e) {
			log.error("setPairsToPending says "+e.getMessage(),e);
		} 
		return false;
	} 
		
	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated). Gets all stages (except noops)
	 * @param con The connection to make the query on 
	 * @param j The job to get pairs for. Must have id and using_dependencies set.
	 * @return A list of job pair objects that belong to the given job.
	 * @author TBebnton
	 */
    protected static List<JobPair> getPendingPairsDetailed(Connection con, Job j,int limit) throws Exception {	

	CallableStatement procedure = null;
	ResultSet results = null;
	try {
	    procedure = con.prepareCall("{CALL GetPendingJobPairsByJob(?,?)}");
	    procedure.setInt(1, j.getId());				
	    procedure.setInt(2,limit);
	    results = procedure.executeQuery();
	    //we map ID's to  primitives so we don't need to query the database repeatedly for them
	    HashMap<Integer,JobPair> pairs= new HashMap<Integer,JobPair>();
	    while(results.next()){
				
			try {
				int currentJobPairId=results.getInt("job_pairs.id");
				
			    JobPair jp = null;
			    // we have already seen this pair and are getting another stage
			    if (pairs.containsKey(currentJobPairId)) {
			    	jp= pairs.get(currentJobPairId);
			    } else {
			    	//we have never seen this pair and are getting it for the first time
			    	jp = JobPairs.resultToPair(results);
			    	Status s = new Status();
				    s.setCode(results.getInt("job_pairs.status_code"));
				    jp.setStatus(s);
				    Benchmark b = Benchmarks.resultToBenchmark(results, "benchmarks");
				    b.setUsesDependencies(results.getInt("dependency_count")>0);
				    jp.setBench(b);
				    
				    if (j.isUsingDependencies()) {
					    jp.setBenchInputPaths(JobPairs.getJobPairInputPaths(jp.getId(),con));
				    } else{
				    	jp.setBenchInputPaths(new ArrayList<String>());
				    }
			    	pairs.put(currentJobPairId, jp);
			    }

				JoblineStage stage=new JoblineStage();
				stage.setStageNumber(results.getInt("stage_number"));
				stage.setStageId(results.getInt("stage_id"));
				jp.addStage(stage);
			    //we need to check to see if the benchId and configId are null, since they might
			    //have been deleted while the the job is still pending
			    
			    Integer configId=results.getInt("jobpair_stage_data.config_id");
			    String configName=results.getString("jobpair_stage_data.config_name");
			    Configuration c=new Configuration();
			    c.setId(configId);
			    c.setName(configName);
			    stage.setConfiguration(c);

			    if (configId!=null) {
				    Solver s = Solvers.resultToSolver(results, "solvers");

					stage.setSolver(s /* could be null, if Solver s above was null */);
			    }			    
			} 
			catch (Exception e) {
			    log.error("there was an error making a single job pair object");
			    log.error(e.getMessage(),e);
			}
	    }
				
	    Common.safeClose(results);
	    
	    for (JobPair jp : pairs.values()) {
	    	if (j.isUsingDependencies()) {
	    		//populate all the dependencies for the pair
			    HashMap<Integer,List<PipelineDependency>> deps=Pipelines.getDependenciesForJobPair(jp.getId(), con);
			    for (JoblineStage stage : jp.getStages()) {
			    	if (deps.containsKey(stage.getStageId())) {
			    		stage.setDependencies(deps.get(stage.getStageId()));
			    	}
			    }
	    	}
		    //make sure all stages are in order

	    	jp.sortStages();
	    }
	    List<JobPair> returnList=new ArrayList<JobPair>();
	    returnList.addAll(pairs.values());
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
	 * Returns all the benchmark inputs for all pairs in this job. Format is a HashMap
	 * that maps job pair IDs to ordered lists of benchmark IDs, where the order is the input order
	 * of the benchmarks
	 * @param jobId the ID of the job in question
	 * @param con The open connection to make the call on
	 * @return A mapping from jobpair IDs to lists of benchmark IDs, where the benchmark IDs
	 * are ordered according to their input order for the job pairs
	 */
	public static HashMap<Integer,List<Integer>> getAllBenchmarkInputsForJob(int jobId, Connection con) {
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			procedure=con.prepareCall("{CALL GetAllJobPairBenchmarkInputsByJob(?)}");
			procedure.setInt(1,jobId);
			results=procedure.executeQuery();
			HashMap<Integer,List<Integer>> inputs=new HashMap<Integer,List<Integer>>();
			while (results.next()) {
				int pairId=results.getInt("jobpair_id");
				int benchId=results.getInt("bench_id");
				if (!inputs.containsKey(pairId)) {
					inputs.put(pairId, new ArrayList<Integer>());
				}
				inputs.get(pairId).add(benchId);
			}
			return inputs;
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Returns all the benchmark inputs for all pairs in this job. Format is a HashMap
	 * that maps job pair IDs to ordered lists of benchmark IDs, where the order is the input order
	 * of the benchmarks
	 * @param jobId The ID of the job to get the benchmark inputs for
	 * @return A HashMap that maps job pair IDs to ordered lists of benchmark IDs
	 * where the list is all the benchmark inputs for that pair in their proper order. Null on error.
	 */
	public static HashMap<Integer,List<Integer>> getAllBenchmarkInputsForJob(int jobId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			
			return getAllBenchmarkInputsForJob(jobId,con);
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}
    
    
    
    
	
	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated). All stages are retrieved
	 * @param j The job to get pairs for. Must have id and using_dependencies set.
	 * @param limit The maximum number of pairs to return. Used for efficiency
	 * @return A list of job pair objects that belong to the given job.
	 * @author Benton McCune
	 */
	public static List<JobPair> getPendingPairsDetailed(Job j, int limit) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getPendingPairsDetailed(con, j, limit);
		} catch (Exception e){			
			log.error("getPendingPairsDetailed for job " + j.getId() + " says " + e.getMessage(), e);		
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
			
			
			while(results.next()){
				JobPair jp = new JobPair();
				jp.setId(results.getInt("id"));
				jp.setBackendExecId(results.getInt("sge_id"));
				
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
	 * Gets all job pairs that are running for the given job. Populates only the pair IDs and the SGE Ids
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
	 * @param jobId The ID of the job to get pairs for
	 * @param statusCode The status to count pairs of
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
	
	/**
	 * Counts the number of pairs a job has that are in the processing status
	 * @param jobId The ID of the job to count pairs for
	 * @return The integer number of paris
	 */
	public static int countProcessingPairsByJob(int jobId) {
		return countPairsByStatus(jobId,StatusCode.STATUS_PROCESSING.getVal());
	
	}
	/**
	 * Returns whether the given job has any pairs that are currently waiting
	 * to be re post processed.
	 * @param jobId The ID of the job ot check
	 * @return True / false as expected, and null on error 
	 */
	public static Boolean hasProcessingPairs(int jobId) {
		int count=countProcessingPairsByJob(jobId);
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
	 * @param jobId The ID of the job to count pairs for
	 * @return The number of pairs in the job that are not complete 
	 */
	
	public static int countIncompletePairs(int jobId) {
		return Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_ENQUEUED.getVal()) +
		Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_RUNNING.getVal());
	}
	
	/**
	 * Returns the number of job pairs that are pending for the current job
	 * @param jobId The ID of the job in question
	 * @return The integer number of pending pairs. -1 is returned on error
	 */
	public static int countPendingPairs(int jobId) {
		return Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
	}
	
	
	/**
	 * Gets the status of the given job
	 * @param jobId The ID of the job to check
	 * @return A JobStatus object
	 */
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
	 * @author Eric Burns
	 */
	
	public static boolean isJobDeleted(Connection con, int jobId) {
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
				JobPairs.killPair(jp.getId(), jp.getBackendExecId());
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
	 * Kill all jobs belonging to a user.
	 * @param userId Id of user whose jobs are to be killed.
	 * @throws StarExecDatabaseException if a database related exception occurs.
	 * @author Albert Giegerich
	 */
	protected static void killUsersJobs(int userId) throws StarExecDatabaseException {
		Connection con = null;
		try {
			con = Common.getConnection();
			List<Job> usersJobs = Jobs.getByUserId(userId);
			for (Job job : usersJobs) {
				int jobId = job.getId();
				// Kill any jobs that are still running.
				if (!Jobs.isJobComplete(jobId)) {
					Jobs.kill(jobId, con);
				}
			}
		} catch (Exception e) {
			throw new StarExecDatabaseException("Database error while deleting all jobs owned by user with id="+userId, e);
		} finally {
			Common.safeClose(con);
		}
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
		int execId = jp.getBackendExecId();
		R.BACKEND.killPair(execId);
		JobPairs.UpdateStatus(jp.getId(), StatusCode.STATUS_PAUSED.getVal());
	    }
	    //Get the running job pairs and remove them
	    List<JobPair> jobPairsRunning = Jobs.getRunningPairs(jobId);
	    if (jobPairsRunning != null) {
		for (JobPair jp: jobPairsRunning) {
		    int execId = jp.getBackendExecId();
		    R.BACKEND.killPair(execId);
		    JobPairs.UpdateStatus(jp.getId(), StatusCode.STATUS_PAUSED.getVal());
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
	
    /**
     * Checks to see if the given job is owned by the test user
     * @param jobId The job to check
     * @return True if the job is owned by the test user and false otherwise
     */
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
			R.BACKEND.killAll();
			List<Integer> jobs = new LinkedList<Integer>();		
			jobs = Jobs.getRunningJobs();
			if (jobs != null) {
				for (Integer jobId : jobs) {
					//Get the enqueued job pairs and remove them
					List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(jobId);
					if (jobPairsEnqueued != null) {
						for (JobPair jp : jobPairsEnqueued) {
							JobPairs.UpdateStatus(jp.getId(), 1);
						}
					}
					//Get the running job pairs and remove them
					List<JobPair> jobPairsRunning = Jobs.getRunningPairs(jobId);
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
	
	/**
	 * Changes the queue that the given job is running on 
	 * @param jobId The ID of the job to change the queue for
	 * @param queueId The ID of the new queue
	 * @return True on success and false otherwise
	 */
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

	public static void setJobName(int jobId, String newName) throws StarExecDatabaseException {
		final String method = "setJobName";
		logUtil.entry(method);
		Connection connection = null;
		CallableStatement procedure = null;
		try {
			connection = Common.getConnection();
			procedure = connection.prepareCall("{CALL SetJobName(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setString(2, newName);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error(e);
			throw new StarExecDatabaseException("Could not save job name to database.", e);
		} finally {
			Common.safeClose(connection);
			Common.safeClose(procedure);
			logUtil.exit(method);
		}
	}

	public static void setJobDescription(int jobId, String newDescription) throws StarExecDatabaseException {
		final String method = "setJobDescription";
		logUtil.entry(method);
		Connection connection = null;
		CallableStatement procedure = null;
		try {
			connection = Common.getConnection();
			procedure = connection.prepareCall("{CALL SetJobDescription(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setString(2, newDescription);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error(e);
			throw new StarExecDatabaseException("Could not save job description to database.", e);
		} finally {
			Common.safeClose(connection);
			Common.safeClose(procedure);
			logUtil.exit(method);
		}
	}
	
	/**
	 * Given a set of pairs and a mapping from pair IDs, to stage numbers to properties, loads the properties into the
	 * appropriate pairs
	 * @param pairs The job pairs to load attributes into
	 * @param attrs A HashMap that maps job pair IDs to a second map that goes from stage numbers to Properties.
	 */
	public static void loadPropertiesIntoPairs(List<JobPair> pairs, HashMap<Integer,HashMap<Integer,Properties>> attrs) {
		for (JobPair jp : pairs) {
			HashMap<Integer,Properties> stageAttrs= attrs.get(jp.getId());
			if (stageAttrs!=null) {
				for (JoblineStage stage : jp.getStages()) {
					if (stageAttrs.containsKey(stage.getStageNumber())) {
						stage.setAttributes(stageAttrs.get(stage.getStageNumber()));
					}
				}
			}
		}
	}
	

	/**
	 * Given a resultset containing the results of a query for job pair attrs,
	 * returns a hashmap mapping job pair ids to maps of stage number to properties
	 * @param results The ResultSet containing the attrs
	 * @return A mapping from pair ids to Properties
	 * @author Eric Burns
	 */
	private static HashMap<Integer,HashMap<Integer,Properties>> processAttrResults(ResultSet results) {
		try {
			HashMap<Integer,HashMap<Integer,Properties>> props=new HashMap<Integer,HashMap<Integer,Properties>>();
			int id;
			int stageNumber;
			while(results.next()){
				id=results.getInt("pair.id");
				stageNumber=results.getInt("attr.stage_number");
				if (!props.containsKey(id)) {
					props.put(id,new HashMap<Integer,Properties>());
				}
				HashMap<Integer,Properties> pairMap=props.get(id);
				if (!pairMap.containsKey(stageNumber)) {
					pairMap.put(stageNumber, new Properties());
				}
				String key=results.getString("attr.attr_key");
				String value=results.getString("attr.attr_value");
				if (key!=null && value!=null) {
					props.get(id).get(stageNumber).put(key, value);	

				}
			}			
			return props;
		} catch (Exception e) {
			log.error("processAttrResults says "+e.getMessage(),e);
		}
		return null;
	}
	
	private static void addStageToSolverStats(SolverStats stats, JoblineStage stage) {
		StatusCode statusCode=stage.getStatus().getCode();
		
		if ( statusCode.failed()) {
		    stats.incrementFailedJobPairs();
		} 
		if ( statusCode.resource()) {
			stats.incrementResourceOutPairs();
		}
		if (statusCode.incomplete()) {
		    stats.incrementIncompleteJobPairs();
		}
		if (statusCode.complete()) {
		    stats.incrementCompleteJobPairs();
		}
		
		int correct=JobPairs.isPairCorrect(stage);
		if (correct==0) {
			
			stats.incrementWallTime(stage.getWallclockTime());
			stats.incrementCpuTime(stage.getCpuTime());
			stats.incrementCorrectJobPairs();
		} else if (correct==1) {
   			stats.incrementIncorrectJobPairs();

		}
	}

	
	
	/**
	 * Given a list of JobPairs, compiles them into SolverStats objects. 
	 * @param pairs The JobPairs with their relevant fields populated
	 * @return A list of SolverStats objects to use in a datatable
	 * @author Eric Burns
	 */
	public static List<SolverStats> processPairsToSolverStats(List<JobPair> pairs) {
		try {
			Hashtable<String, SolverStats> SolverStats=new Hashtable<String,SolverStats>();
			String key=null;
			for (JobPair jp : pairs) {
				
				for (JoblineStage stage : jp.getStages()) {
					
					//we need to exclude noOp stages
					if (stage.isNoOp()) {
						continue;
					}

					//entries in the stats table determined by stage/configuration pairs
					key=stage.getStageNumber()+":"+String.valueOf(stage.getConfiguration().getId());
					
					if (!SolverStats.containsKey(key)) { // current stats entry does not yet exist
						SolverStats newSolver=new SolverStats();
						newSolver.setStageNumber(stage.getStageNumber());
						newSolver.setSolver(stage.getSolver());
						newSolver.setConfiguration(stage.getConfiguration());
						SolverStats.put(key, newSolver);
					}
					
					
					//update stats info for entry that current job-pair belongs to
					SolverStats curSolver=SolverStats.get(key);
					addStageToSolverStats(curSolver,stage);
					if (stage.getStageNumber()==jp.getPrimaryStageNumber()) {
						//if we get here, we need to add this stage to the primary stats as well
						key=0+":"+String.valueOf(stage.getConfiguration().getId());
						if (!SolverStats.containsKey(key)) { // current stats entry does not yet exist
							SolverStats newSolver=new SolverStats();
							newSolver.setStageNumber(0);
							newSolver.setSolver(stage.getSolver());
							newSolver.setConfiguration(stage.getConfiguration());
							SolverStats.put(key, newSolver);
						}
						
						
						//update stats info for entry that current job-pair belongs to
						curSolver=SolverStats.get(key);
						addStageToSolverStats(curSolver,stage);
					}
				}
				}
				
			
			List<SolverStats> returnValues=new LinkedList<SolverStats>();
			for (SolverStats js : SolverStats.values()) {
				returnValues.add(js);
			}
			return returnValues;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
		
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
	
	private static List<JobPair> processStatResults(ResultSet results, boolean includeSingleStage) throws Exception {
		
		try {
			List<JobPair> returnList = new ArrayList<JobPair>();
			
			HashMap<Integer,Solver> solvers=new HashMap<Integer,Solver>();
			HashMap<Integer,Configuration> configs=new HashMap<Integer,Configuration>();
			Integer id;
			
			
			Benchmark bench=null;
			while(results.next()){
				JobPair jp = new JobPair();
				jp.setPrimaryStageNumber(results.getInt("primary_jobpair_data"));
				// these are the solver and configuration defaults. If any jobpair_stage_data
				// entry has null for a stage_id, then these are the correct primitives. 
					
				
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				jp.setId(results.getInt("job_pairs.id"));
				jp.setPath(results.getString("job_pairs.path"));
				bench=new Benchmark();
				bench.setId(results.getInt("bench_id"));
				bench.setName(results.getString("bench_name"));
				jp.setBench(bench);

				jp.setCompletionId(results.getInt("completion_id"));
				
				
				if (includeSingleStage) {
					//If we are here, we are populating exactly 1 stage for purposes of filling up a table.
					//so, we simply set the primary stage of this pair to the first stage for the time being
					jp.setPrimaryStageNumber(1);
					JoblineStage stage=new JoblineStage();
					stage.setStageNumber(1);
					stage.setCpuUsage(results.getDouble("jobpair_stage_data.cpu"));
					stage.setWallclockTime(results.getDouble("jobpair_stage_data.wallclock"));
					stage.setStageId(results.getInt("jobpair_stage_data.stage_id"));
					stage.getStatus().setCode(results.getInt("jobpair_stage_data.status_code"));
					//everything below this line is in a stage
					id=results.getInt("jobpair_stage_data.solver_id");
					//means it was null in SQL
					if (id==0) {
						stage.setNoOp(true);
						stage.setSolver(null);
						stage.setConfiguration(null);
					} else {
						if (!solvers.containsKey(id)) {
							
							Solver solve=new Solver();
							solve.setId(id);
							solve.setName(results.getString("jobpair_stage_data.solver_name"));
							solvers.put(id,solve);
						}
						stage.setSolver(solvers.get(id));

						id=results.getInt("jobpair_stage_data.config_id");
						
						
						if (!configs.containsKey(id)) {
							Configuration config=new Configuration();
							config.setId(id);
							config.setName(results.getString("jobpair_stage_data.config_name"));
							configs.put(id, config);
						}
						stage.getSolver().addConfiguration(configs.get(id));
						stage.setConfiguration(configs.get(id));
					}
					
					
					
					Properties p=new Properties();
					String result=results.getString("result");
					if (result!=null) {
						p.put(R.STAREXEC_RESULT, result);
					}
					
					
					stage.setAttributes(p);
					jp.addStage(stage);
				}
				
				returnList.add(jp);		
				
			}

			return returnList;	
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
		
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
	 * @return true on success and false otherwise
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
	 * @param stageNumber The ID of the state to reprocess
	 * @return True if the operation was successful, false otherwise.
	 * @author Eric Burns
	 */
	public static boolean prepareJobForPostProcessing(int jobId, int processorId, int stageNumber) {
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

			procedure=con.prepareCall("{CALL PrepareJobForPostProcessing(?,?,?,?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2,processorId);
			procedure.setInt(3,StatusCode.STATUS_COMPLETE.getVal());
			procedure.setInt(4, StatusCode.STATUS_PROCESSING.getVal());
			procedure.setInt(5,stageNumber);
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
	public static boolean saveStats(int jobId,List<SolverStats> stats) {
		
		if (!isJobComplete(jobId)) {
			log.debug("stats for job with id = "+jobId+" were not saved because the job is incomplete");
			return false; //don't save stats if the job is not complete
		}
		Connection con=null;
		try {
			con=Common.getConnection();
			Common.beginTransaction(con);
			for (SolverStats s : stats) {
				
				if (!saveStats(s,con)) {
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
	
	private static boolean saveStats(SolverStats stats, Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL AddJobStats(?,?,?,?,?,?,?,?,?,?,?)}");
			procedure.setInt(1,stats.getJobSpaceId());
			procedure.setInt(2,stats.getConfiguration().getId());
			procedure.setInt(3,stats.getCompleteJobPairs());
			procedure.setInt(4,stats.getCorrectJobPairs());
			procedure.setInt(5,stats.getIncorrectJobPairs());
			procedure.setInt(6,stats.getFailedJobPairs());
			procedure.setDouble(7,stats.getWallTime());
			procedure.setDouble(8,stats.getCpuTime());
			procedure.setInt(9,stats.getResourceOutJobPairs());
			procedure.setInt(10, stats.getIncompleteJobPairs());
			procedure.setInt(11,stats.getStageNumber());
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
			List<JobSpace> jobSpaces=Spaces.getSubSpacesForJob(j.getPrimarySpace(), true);
			jobSpaces.add(Spaces.getJobSpace(j.getPrimarySpace()));
			
			for (JobSpace s : jobSpaces) {
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
	 * @param con The open connection to make the call on
	 * @return True on success and false otherwise
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
	/**
	 * Returns the number of jobs that are currently paused on the system
	 * @return The integer number of jobs, or -1 on error
	 */
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
	 * @return The integer number of running jobs
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
	 * Gets all the jobs on the system that currently have pairs pending or running
	 * and which are not currently paused or killed
	 * @return A list of Job objects for the running jobs. Pairs are not populated
	 */
	public static List<Integer> getRunningJobs() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetRunningJobs()}");
			results = procedure.executeQuery();
			
			List<Integer> jobs = new LinkedList<Integer>();
			while (results.next()) {
				jobs.add(results.getInt("id"));
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

	/**
	 * Checks to see if the global pause is enabled on the system
	 * @return True if the system is paused or false if it is not
	 */
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
	
	/**
	 * Gets the ID of every job a user owns that is orphaned
	 * @param userId The ID of the user to get orphaned jobs for
	 * @return A list of job IDs, or null on error
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
	 * @return True on success and false otherwise
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
	
	/**
	 * Given a ResultSet that is currently pointing to a row containing data for a StageAttributes
	 * object, generates the object
	 * @param results The results, which must be pointing to a row with a StageAttributes object
	 * @return The StageAttributes, or null on error
	 */
	public static StageAttributes resultsToStageAttributes(ResultSet results) {
		try {
			StageAttributes attrs=new StageAttributes();

			attrs.setCpuTimeout(results.getInt("cpuTimeout"));
			attrs.setJobId(results.getInt("job_id"));
			attrs.setMaxMemory(results.getLong("maximum_memory"));
			attrs.setSpaceId(results.getInt("space_id"));
			if (attrs.getSpaceId()==0) {
				attrs.setSpaceId(null);
			}
			attrs.setStageNumber(results.getInt("stage_number"));
			attrs.setWallclockTimeout(results.getInt("clockTimeout"));
			attrs.setBenchSuffix(results.getString("bench_suffix"));
			attrs.setResultsInterval(results.getInt("results_interval"));
			return attrs;

		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return null;
	}
	
	/**
	 * Gets all the stage attributes for the given job
	 * @param jobId The job in question
	 * @param con An open connection to make the call on
	 * @return A list of StageAttributes objects or null on error
	 */
	public static List<StageAttributes> getStageAttrsForJob(int jobId, Connection con) {
		ResultSet results=null;
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL getStageParamsByJob(?)}");
			procedure.setInt(1, jobId);
			results=procedure.executeQuery();
			List<StageAttributes> attrs=new ArrayList<StageAttributes>();
			
			while (results.next()) {
				StageAttributes a = resultsToStageAttributes(results);
				
				a.setPostProcessor(Processors.get(results.getInt("post_processor")));
				a.setPreProcessor(Processors.get(results.getInt("pre_processor")));
				attrs.add(a);
			}
			
			return attrs;
			
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	/**
	 * This function takes all job pairs that
	 * 1) Have status code 2-5, meaning they should be enqueued or running
	 * 2) Are not currently listed in the backend
	 * and sets them to status code 9. This basically takes pairs that
	 * have somehow gotten stuck in a bad state and applies an error
	 * status to them.
	 * @throws IOException 
	 */
	public static void setBrokenPairsToErrorStatus(Backend backend) throws IOException {
		// It is important that we read the database first and the backend second
		// Otherwise, any pairs enqueued between these two lines would get marked
		// as broken
		List<JobPair> runningPairs = JobPairs.getPairsInBackend();
		Set<Integer> backendIDs = backend.getActiveExecutionIds();
		for (JobPair p : runningPairs) {
			// if SGE does not think this pair should be running, kill it
			// the kill only happens if the pair's status has not been changed
			// since getPairsInBackend() was called
			if (!backendIDs.contains(p.getBackendExecId())) {
				JobPairs.setBrokenPairStatus(p);
			}
		}
	}
}
