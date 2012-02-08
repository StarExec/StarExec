package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;

/**
 * Handles all database interaction for jobs
 * @author Tyler Jensen
 * TODO: Implement this functionality. NOTE: This doesn't work yet!
 */
@Deprecated
public class Jobs {
	private static final Logger log = Logger.getLogger(Jobs.class);
	
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
						
			CallableStatement procedure = null;						
			procedure = con.prepareCall("{CALL AssociateJob(?, ?)}");
			
			for(int jid : jobIds) {
				procedure.setInt(1, jid);
				procedure.setInt(2, spaceId);			
				procedure.executeUpdate();			
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
	 * Gets very basic information about a job (but not about
	 * any of its pairs) use getPairsForJob for those details
	 * @param jobId The id of the job to get information for 
	 * @return A job object containing information about the requested job
	 * @author Tyler Jensen
	 */
	public static Job get(int jobId) {
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
				j.setSubmitted(results.getTimestamp("submitted"));
				j.setFinished(results.getTimestamp("finished"));
				j.setStatus(results.getString("status"));
				j.setTimeout(results.getInt("timeout"));
				return j;
			}						
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	public static List<Integer> getSGEJobIds(int jid) {
		// TODO: Instantiate method
		return new ArrayList<Integer>();
	}
	
	/**
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
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));
				j.setSubmitted(results.getTimestamp("submitted"));
				j.setStatus(results.getString("status"));
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
	 * Gets all job pairs for the given job 
	 * @param jobId The id of the job to get pairs for
	 * @param userId The id of the user requesting the pairs (used for permission check)
	 * @return A list of job pair objects that beint to the given job.
	 * @author Tyler Jensen
	 */
	public static List<JobPair> getPairs(int jobId, int userId) {
		/*Connection con = null;			
		
		try {
			if(Permissions.canUserSeeJob(jobId, userId)) {
				con = Common.getConnection();		
				CallableStatement procedure = con.prepareCall("{CALL GetJobPairByJob(?)}");
				procedure.setInt(1, jobId);					
				ResultSet results = procedure.executeQuery();
				List<JobPair> returnList = new LinkedList<JobPair>();
				
				
				// TODO: differentiate JobPairs and job_pair_attrs
				while(results.next()){
					JobPair jp = new JobPair();
					jp.setId(results.setInt("id"));
					jp.setResult(results.getString("result"));
					jp.setStatus(results.getString("status"));
					jp.setStartDate(results.getTimestamp("start"));
					jp.setEndDate(results.getTimestamp("stop"));
					
					Configuration c = new Configuration();
					c.setId(results.setInt("config_id"));
					c.setDescription(results.getString("config_desc"));
					c.setName(results.getString("config_name"));
					
					Benchmark b = new Benchmark();
					b.setId(results.setInt("bench_id"));
					b.setDescription(results.getString("bench_desc"));
					b.setName(results.getString("bench_name"));
					
					Solver s = new Solver();
					s.setId(results.setInt("solver_id"));
					s.setDescription(results.getString("solver_desc"));
					s.setName(results.getString("solver_name"));	
					s.addConfiguration(c);
					c.setSolverId(s.getId());
					
					jp.setBenchmark(b);
					jp.setSolver(s);
					
					returnList.add(jp);
				}			
				
				return returnList;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		*/
		return null;		
	}

	/**
	 * For retrieval.
	 * @param pid
	 * @return A particular jobpair
	 */
	public static JobPair getPair(int pid) {
		// TODO Auto-generated method stub
		return null;
	}
}
