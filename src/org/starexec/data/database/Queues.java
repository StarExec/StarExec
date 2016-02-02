package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.data.to.WorkerNode;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.util.LogUtil;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;


/**
 * Handles all DB interaction for queues
 * @author Tyler Jensen
 */
public class Queues {
	private static final Logger log = Logger.getLogger(Queues.class);
	private static final LogUtil logUtil = new LogUtil(log);

    /**
     * @return returns the default queue name, default queue should always exist
     */
    public static String getDefaultQueueName(){
    	return "all";
    }
	
	/**
	 * Removes a queue from the database and calls R.BACKEND.removeQueue
	 * @param queueId The Id of the queue to remove.
	 * @return True on success and false otherwise
	 */
	public static boolean removeQueue(int queueId) {
	    
	    Queue q=Queues.get(queueId);
		
	    //Pause jobs that are running on the queue
	    List<Job> jobs = Cluster.getJobsRunningOnQueue(queueId);

	    if (jobs != null) {
			for (Job j : jobs) {
			    Jobs.pause(j.getId());
			}
	    }

	    //Move associated Nodes back to default queue
	    List<WorkerNode> nodes = Queues.getNodes(queueId);

	    if (nodes != null) {
			for (WorkerNode n : nodes) {
			    R.BACKEND.moveNode(n.getName(), getDefaultQueueName());
			}
	    }
		
	    boolean success=true;
		
		
	    /***** DELETE THE QUEUE *****/	
	    
	    success=success && Queues.delete(queueId);
	    R.BACKEND.deleteQueue(q.getName());
			
	    Cluster.loadWorkerNodes();
	    Cluster.loadQueues();	
	    return success;
	}

    /** 
     *Will pause jobs associated with this queue if queue only has one worker node, used by MoveNodes
     *@param queue the Queue object whose jobs might be paused
     **/
    public static void pauseJobsIfOneWorker(Queue queue){
	//if this is going to make the queue empty...... need to pause all jobs first
	if(queue != null){
	    List<WorkerNode> workers = Cluster.getNodesForQueue(queue.getId());
		    
	    if (workers != null) {
		if (workers.size() == 1 ) {
		    log.info("checking for jobs running on queue "+queue.getName()+", since this is the last node in the queue.");
		    List<Job> jobs = Cluster.getJobsRunningOnQueue(queue.getId());
		    if (jobs != null) {
			for (Job j : jobs) {
			    Jobs.pause(j.getId());
			}
		    }
		}
	    }
	}
		    
    }

	/**
	 * Adds a new queue to the system. This action adds the queue, 
	 * and adds a new association to the queue for the given list of nodes
	 * This is a multi-step process, use transactions to ensure it completes as
	 * an atomic unit.
	 * @param con The connection to perform the operation on
	 * @param queueName The name of the queue to add
	 * @param cpuTimeout the max cpu timeout for the new queue
	 * @param wallTimeout the max wallclock timeout for the new queue
	 * @return The ID of the newly inserted queue, -1 if the operation failed
	 * @author Tyler Jensen
	 * @throws Exception 
	 */
	protected static int add(Connection con, String queueName, int cpuTimeout, int wallTimeout) throws Exception {			
		log.debug("preparing to call sql procedures to add queue with name = "+queueName);
		CallableStatement procedure = null;
		try {
			
			//Add the queue first
			log.debug("Calling AddQueue");
			log.debug("queueName = " + queueName);
			procedure = con.prepareCall("{CALL AddQueue(?,?,?,?)}");	
			procedure.setString(1, queueName);
			procedure.setInt(2, wallTimeout);
			procedure.setInt(3,cpuTimeout);
			procedure.registerOutParameter(4, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			int newQueueId = procedure.getInt(4);
			
			log.info(String.format("New queue with name [%s] was successfully created", queueName));
			return newQueueId;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return -1;
	}
	
	/**
	 * Adds a new queue to the system.
	 * @param queueName The name of the queue to add
	 * @param cpuTimeout the max cpu timeout for the new queue
	 * @param wallTimeout the max wallclock timeout for the new queue
	 * @return The ID of the newly inserted queue, -1 if the operation failed
	 * @author Wyatt Kaiser
	 */
	public static int add(String queueName, int cpuTimeout, int wallTimeout) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
						
			Common.beginTransaction(con);	

			// Add queue is a multi-step process, so we need to use a transaction
			int newQueueId = Queues.add(con, queueName,cpuTimeout,wallTimeout);

			Common.endTransaction(con);			
			
			return newQueueId;
		} catch (Exception e){	
			Common.doRollback(con);			

			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return -1;
	}
	
	/**
	 * Associates a queue and a worker node to indicate the node belongs to the queue.
	 * If the association already exists, any errors are ignored.
	 * @param queueId the ID of the queue
	 * @param nodeId The ID of the worker node that belongs to the queue
	 * @return True if the operation was a success, false otherwise. 
	 */
	public static boolean associate(int queueId, int nodeId) {
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL AssociateQueueById(?, ?)}");
			procedure.setInt(1, queueId);
			procedure.setInt(2, nodeId);
			
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
	 * Associates a queue and a worker node to indicate the node belongs to the queue.
	 * If the association already exists, any errors are ignored.
	 * @param queueName The FULL name of the owning queue
	 * @param nodeName The FULL name of the worker node that belongs to the queue
	 * @return True if the operation was a success, false otherwise. 
	 */
	public static boolean associate(String queueName, String nodeName) {
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL AssociateQueue(?, ?)}");
			procedure.setString(1, queueName);
			procedure.setString(2, nodeName);
			
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
	 * Removes all associations between queues and nodes in db so that only up to date data
	 * will be stored.
	 * @return True if the operation was a success, false otherwise. 
	 * @author Benton McCune
	 */
	public static boolean clearQueueAssociations() {
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL clearQueueAssociations()}");			
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
	 * Gets a queue with very basic information, not including any SGE attributes with the queue
	 * @param con The connection to make the query with
	 * @param qid The id of the queue to retrieve
	 * @return a queue object representing the queue to retrieve
	 * @throws Exception 
	 */
	protected static Queue get(Connection con, int qid) throws Exception {	
		log.debug("starting get");
		log.debug("id = " + qid);
		ResultSet results=null;
		CallableStatement procedure = null;
		
		try {
			procedure = con.prepareCall("{CALL GetQueue(?)}");
			procedure.setInt(1, qid);			
			results = procedure.executeQuery();
			if(results.next()){
				Queue queue = new Queue();
				queue.setName(results.getString("name"));
				queue.setId(results.getInt("id"));
				queue.setStatus(results.getString("status"));
				queue.setSlotsTotal(results.getInt("slots_total"));
				queue.setSlotsAvailable(results.getInt("slots_free"));
				queue.setSlotsReserved(results.getInt("slots_reserved"));
				queue.setSlotsUsed(results.getInt("slots_used"));
				queue.setWallTimeout(results.getInt("clockTimeout"));
				queue.setCpuTimeout(results.getInt("cpuTimeout"));
				queue.setGlobalAccess(results.getBoolean("global_access"));
				return queue;				
			}										
			
		} catch (Exception e) {
			log.error("Queue.get says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}	
	
	
	
	/**
	 * Gets a queue with very basic information, not including any SGE attributes with the queue
	 * @param qid The id of the queue to retrieve
	 * @return a queue object representing the queue to retrieve
	 */
	public static Queue get(int qid) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();					
			return Queues.get(con, qid);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets all active queues in the starexec cluster
	 * @return A list of queues 
	 * @author Aaron Stump
	 */
	public static List<Queue> getAll() {
	    return getQueues(0);
	}
	
	/**
	 * Gets all queues in the starexec cluster (Including Inactive queues)
	 * @return A list of queues 
	 * @author Wyatt Kaiser
	 */
	public static List<Queue> getAllAdmin() {
	    return getQueues(-2);
	}

	/**
	 * Gets a queue with detailed information (Id and name)
	 * @param id The id of the queue to get detailed information for
	 * @return A queue object containing all of its attributes
	 * @author Tyler Jensen
	 */
	
	public static Queue getDetails(int id) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetQueueDetails(?)}");
			procedure.setInt(1, id);			
			 results = procedure.executeQuery();
			Queue queue = new Queue();
			
			if(results.next()){
				queue.setName(results.getString("name"));
				queue.setId(results.getInt("id"));
				queue.setStatus(results.getString("status"));
				queue.setSlotsTotal(results.getInt("slots_total"));
				queue.setSlotsAvailable(results.getInt("slots_free"));
				queue.setSlotsReserved(results.getInt("slots_reserved"));
				queue.setSlotsUsed(results.getInt("slots_used"));
				queue.setWallTimeout(results.getInt("clockTimeout"));
				queue.setCpuTimeout(results.getInt("cpuTimeout"));
				queue.setGlobalAccess(results.getBoolean("global_access"));
				return queue;
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	
	

	protected static int getCountOfEnqueuedPairsShallow(Connection con, int qId) throws Exception {	
		CallableStatement procedure = null;
		ResultSet results = null;
		
		try {
			 procedure = con.prepareCall("{CALL GetCountOfEnqueuedJobPairsByQueue(?)}");
			procedure.setInt(1, qId);					
			 results = procedure.executeQuery();

			if(results.next()){
				return results.getInt("count");
			}			

			return -1;
		} catch (Exception e) {
			log.error("getCountOfEnqueuedPairsShallow says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return -1;
	}
	
	/**
	 * Gets all job pairs that are enqueued (up to limit) for the given queue 
	 * @param qId The id of the queue to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	public static int getCountOfEnqueuedPairsShallow(int qId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getCountOfEnqueuedPairsShallow(con, qId);
		} catch (Exception e){			
			log.error("getCountOfEnqueuedPairsShallow for queue " + qId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return -1;		
	}
	
	/**
	 * Gets the ID of a queue given the name of the queue
	 * @param queueName The exact name of the queue, including .q
	 * @return The name, or null if it was not found.
	 */
	public static int getIdByName(String queueName) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetIdByName(?)}");
			procedure.setString(1, queueName);
			
			
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("id");	
			}
		} catch (Exception e){			
			log.error("getIdByName says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
			return -1;				
	}
	
	private static String getPairOrderColumnForClusterPage(int indexOrder) {
		if (indexOrder==0) {
			return "queuesub_time";
		} else if (indexOrder==1) {
			return "jobs.name";
		} else if (indexOrder==2) {
			return "users.first_name, users.last_name";
		} else if (indexOrder==3) {
			return "bench_name";
		} else if (indexOrder==4) {
			return "solver_name";
		} else if (indexOrder==5) {
			return "config_name";
		} else if (indexOrder==6) {
			return "path";
		}
		
		return "jobs.name";
	}
	
	/**
	 * Gets all the necessary job pairs for populating a datatables page on the cluster status page
	 * @param startingRecord The first desired records
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether the records should be sorted ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The index of the client side datatables column to sort on
	 * @param searchQuery The search query to filter the results by
	 * @param id The ID of the queue or node
	 * @param type The type of the table on the cluster status page (either queue or node)
	 * @return A list of JobPairs running on the node
	 */
	
	public static List<JobPair> getJobPairsForNextClusterPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int id, String type) {
		PaginationQueryBuilder builder = null;

		if (type == "queue") {
			builder = new PaginationQueryBuilder(PaginationQueries.GET_PAIRS_ENQUEUED_QUERY, startingRecord, recordsPerPage, getPairOrderColumnForClusterPage(indexOfColumnSortedBy), isSortedASC);
		} else if (type == "node") {
			
			builder = new PaginationQueryBuilder(PaginationQueries.GET_PAIRS_RUNNING_QUERY, startingRecord, recordsPerPage, getPairOrderColumnForClusterPage(indexOfColumnSortedBy), isSortedASC);

		} else {
			return null;
		}
		
		Connection con = null;		
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = new NamedParameterStatement(con, builder.getSQL());
			
			procedure.setInt("id", id);
			
			
			 results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();
			
			while(results.next()){
				JobPair jp=new JobPair();
				jp.setPrimaryStageNumber(results.getInt("job_pairs.primary_jobpair_data")); //because we are only populating the one stage
				jp.setPath(results.getString("job_pairs.path"));
				jp.setJobId(results.getInt("job_pairs.job_id"));
				jp.setId(results.getInt("job_pairs.id"));
				jp.setQueueSubmitTime(results.getTimestamp("job_pairs.queuesub_time"));
				Status stat = new Status();
				//enqueued by definition, so we don't want to retrieve extra data from the db
				stat.setCode(StatusCode.STATUS_ENQUEUED);
				
				jp.setStatus(stat);
				
				JoblineStage stage=new JoblineStage();
				stage.setStageNumber(jp.getPrimaryStageNumber());

				jp.addStage(stage);

				log.debug("attempting to get benchmark with ID = "+results.getInt("bench_id"));
				Benchmark b=new Benchmark();
				b.setId(results.getInt("job_pairs.bench_id"));
				b.setName(results.getString("job_pairs.bench_name"));
				jp.setBench(b);
				
				Solver s=new Solver();
				s.setId(results.getInt("jobpair_stage_data.solver_id"));
				s.setName(results.getString("jobpair_stage_data.solver_name"));
				stage.setSolver(s);
				
				Configuration c = new Configuration();
				c.setId(results.getInt("jobpair_stage_data.config_id"));
				c.setName(results.getString("jobpair_stage_data.config_name"));
				stage.setConfiguration(c);
				jp.getPrimarySolver().addConfiguration(c);
				
				User u=new User();
				u.setId(results.getInt("users.id"));
				u.setFirstName(results.getString("users.first_name"));
				u.setLastName(results.getString("users.last_name"));
				jp.setOwningUser(u);
				
				Job j = new Job();
				j.setId(results.getInt("jobs.id"));
				j.setName(results.getString("jobs.name"));
				
				jp.setOwningJob(j);
				
				returnList.add(jp);
			}			
			log.debug("the returnlist had "+returnList.size()+" items");
			Common.safeClose(results);
			return returnList;			
			
		} catch (Exception e){			
			log.error("getNextPageOfEnqueuedJobPairs for queue " + id + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
			return null;
	}

	/**
	 * Gets all nodes in the cluster that belong to the queue
	 * @param id The id of the queue to get nodes for
	 * @return A list of nodes that belong to the queue
	 * @author Tyler Jensen
	 */
	public static List<WorkerNode> getNodes(int id) {
		return Cluster.getNodesForQueue(id);
	}
	
	
	
	
	
	/**
     * Gets jobs with pending job pairs for the given queue
     * @param queueId the id of the queue
     * @return the list of Jobs for that queue which have pending job pairs
     * @author Ben McCune and Aaron Stump
     */
	public static List<Job> getPendingJobs(int queueId) {
		Connection con = null;		
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetPendingJobs(?)}");					
			procedure.setInt(1, queueId);					
			 results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("jobs.id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setPrimarySpace(results.getInt("primary_space"));
				j.setSeed(results.getLong("seed"));
				j.setCpuTimeout(results.getInt("cpuTimeout"));
				j.setWallclockTimeout(results.getInt("clockTimeout"));
				j.setMaxMemory(results.getLong("maximum_memory"));
				j.setSuppressTimestamp(results.getBoolean("suppress_timestamp"));
				j.setUsingDependencies(results.getBoolean("using_dependencies"));
				j.getQueue().setId(queueId);

				j.setStageAttributes(Jobs.getStageAttrsForJob(j.getId(), con));
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
	 * Gets all queues in the starexec cluster (with no detailed information)
	 * @param userId return the queues accessible by the given user, or all queues if the userId is 0
	 * @return A list of queues 
	 * @author Tyler Jensen and Aaron Stump
	 */
	protected static List<Queue> getQueues(int userId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			if (userId == 0) {
				//only gets the queues that have status "ACTIVE"
			    procedure = con.prepareCall("{CALL GetAllQueues}");
			} else if (userId == -2) {
				//includes inactive queues
				procedure = con.prepareCall("{CALL GetAllQueuesAdmin}");
			} else {
			    procedure = con.prepareCall("{CALL GetUserQueues(?)}");
			    procedure.setInt(1, userId);
			}

			 results = procedure.executeQuery();
			List<Queue> queues = new LinkedList<Queue>();
			
			while(results.next()){
				Queue q = new Queue();
				q.setName(results.getString("name"));
				q.setId(results.getInt("id"));	
				q.setStatus(results.getString("status"));
				q.setWallTimeout(results.getInt("clockTimeout"));
				q.setCpuTimeout(results.getInt("cpuTimeout"));
				queues.add(q);
			}			
						
			return queues;
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
	 * Retrieves all of the queues that the given user has access to when running a job
	 * @param userId Id of the user to get queues for
	 * @return A list of queues, or null on error
	 */
	public static List<Queue> getQueuesForUser(int userId) {
		final String method = "getQueuesForUser";
		if (Users.isAdmin(userId)) {
			logUtil.debug(method, "Getting queues for admin user.");
			return getQueues(0);
		} else {
			logUtil.debug(method, "Getting queues for non-admin user.");
			
			Connection con = null;
			ResultSet results = null;
			CallableStatement procedure = null;
			try {
				con = Common.getConnection();
				procedure = con.prepareCall("{CALL GetQueuesForUser(?)}");
				procedure.setInt(1, userId);
				results = procedure.executeQuery();
				List<Queue> queues = new LinkedList<Queue>();
				
				while (results.next()) {
					Queue q = new Queue();
					q.setId(results.getInt("id"));
					q.setName(results.getString("name"));
					q.setStatus(results.getString("status"));
					q.setGlobalAccess(results.getBoolean("global_access"));
					q.setCpuTimeout(results.getInt("cpuTimeout"));
					q.setWallTimeout(results.getInt("clockTimeout"));
					queues.add(q);
				}
				return queues;
			} catch (Exception e) {
				log.error("GetQueuesForUser says " + e.getMessage(), e);
			} finally {
				Common.safeClose(con);
				Common.safeClose(results);
				Common.safeClose(procedure);
			}
			return null;
		}
	}

	/**
	 * Get all the queues that have been reserved for a particular space
	 * @param spaceId The ID of the space to check
	 * @return A list of valid queues or null on errors
	 */
	public static List<Queue> getQueuesForSpace(int spaceId) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetQueuesForSpace(?)}");
			procedure.setInt(1, spaceId);
	
			results = procedure.executeQuery();
			List<Queue> queues = new LinkedList<Queue>();
			
			while(results.next()){
				Queue q = Queues.get(results.getInt("queue_id"));
				queues.add(q);
			}			
						
			return queues;
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
	 * Gets all job pairs that are enqueued(up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param qId The id of the queue to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 * @throws Exception 
	 */
	protected static int getCountOfRunningPairsDetailed(Connection con, int qId) throws Exception {	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetCountOfRunningJobPairsByQueue(?)}");
			procedure.setInt(1, qId);					
			results = procedure.executeQuery();

			if(results.next()){
				return results.getInt("count");
			}			

			return -1;
		} catch (Exception e) {
			log.error("getRunningPairsDetailed says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}
	
	
	/**
	 * Gets all job pairs that are running (up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param qId The id of the queue to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	public static int getCountOfRunningPairsDetailed(int qId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getCountOfRunningPairsDetailed(con, qId);
		} catch (Exception e){			
			log.error("getRunningPairsDetailed for job " + qId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return -1;		
	}
	
	/**
	 * Returns the sum of wallclock timeouts for all pairs that are in the given queue (running
	 * or enqueued) that are owned by the given user.
	 * @param queueId The queue in question
	 * @param userId The ID of the user who owns the pairs
	 * @return The integer sum of wallclock timeouts, or null on failure
	 */

	public static Integer getUserLoadOnQueue(int queueId, int userId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetUserLoadOnQueue(?,?)}");					
			procedure.setInt(1, queueId);					
			procedure.setInt(2, userId);
			 results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("queue_load");	
			}							
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
	 * Returns the number of job pairs enqueued in the given queue
	 * @param queueId The queue in question
	 * @return The integer number of jobs, or null on failure
	 */

	public static Integer getSizeOfQueue(int queueId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetNumEnqueuedJobs(?)}");					
			procedure.setInt(1, queueId);					
			 results = procedure.executeQuery();

			Integer qSize = -1;
			while(results.next()){
				qSize = results.getInt("count");	
			}							
			return qSize;
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
	 * Gets all queues in the starexec cluster accessible by the user with the given id
	 * @return A list of queues that are defined the cluster
	 * @param userId The ID of the user to ge tqueues for
	 * @author Aaron Stump

	 */
	public static List<Queue> getUserQueues(int userId) {
		if (Users.hasAdminReadPrivileges(userId)) {
			return getAll();
		} else {
			return getQueues(userId);
		}
	}
	
	/**
	 * Updates the status of ALL queues with the given status
	 * @param status The status to set for all queues
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean setStatus(String status) {
		return Queues.setStatus(null, status);
	}

	/**
	 * Updates the status of the given queue with the given status
	 * @param name the name of the queue to set the status for
	 * @param status the status to set for the queue
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean setStatus(String name, String status) {
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = null;
			
			if(name == null) {
				// If no name was supplied, apply to all queues
				procedure = con.prepareCall("{CALL UpdateAllQueueStatus(?)}");
				procedure.setString(1, status);				
			} else {
				procedure = con.prepareCall("{CALL UpdateQueueStatus(?, ?)}");
				procedure.setString(1, name);
				procedure.setString(2, status);
			}
						
			procedure.executeUpdate();			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.debug(String.format("Status for queue [%s] failed to be updated.", (name == null) ? "ALL" : name));
		return false;
	}
	
	/**
	 * Checks to see whether the given name is already being used by a queue
	 * @param queueName The name we want to check against all existing queues
	 * @return True if the given name IS used by a given queue. False if the name is NOT used by a queue OR on error
	 */

	public static boolean notUniquePrimitiveName(String queueName) {
		return Queues.getIdByName(queueName)>=0;
	}
	
	/**
	 * Gets the name of a queue given its ID
	 * @param queueId The ID of the queue to retrieve the name of
	 * @return The name of the queue, or null on error
	 */
	
	public static String getNameById(int queueId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetNameById(?)}");
			procedure.setInt(1, queueId);
			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getString("name");
			}						
		} catch (Exception e){			
			log.error("getIdByName says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		//we couldn't find the name 
		return null;
	}
	
	/**
	 * Updates the cpu timeout for an existing queue
	 * @param queueId The ID of the queue to update
	 * @param timeout The timeout to set, in seconds
	 * @return True on success and false on error
	 */
	public static boolean updateQueueCpuTimeout(int queueId, int timeout) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL UpdateQueueCpuTimeout(?,?)}");
			procedure.setInt(1,queueId);
			procedure.setInt(2,timeout);
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
	 * Updates the wallclock timeout for an existing queue
	 * @param queueId The ID of the queue to update
	 * @param timeout The new timeout, in seconds.
	 * @return True on success and false on error.
	 */
	public static boolean updateQueueWallclockTimeout(int queueId, int timeout) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL UpdateQueueClockTimeout(?,?)}");
			procedure.setInt(1,queueId);
			procedure.setInt(2,timeout);
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
	 * Checks to see whether the given queue is global or not
	 * @param queueId The ID of the queue to check
	 * @return True if it is global, and false if it is not OR if there is an error
	 */
	
	public static boolean isQueueGlobal(int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL IsQueueGlobal(?)}");
			procedure.setInt(1, queueId);
			
			results = procedure.executeQuery();
			while(results.next()) {
				return results.getBoolean("global_access");
			}
		} catch (Exception e) {
			log.error("IsQueueGlobal says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}
	
	/**
	 * Deletes a queue from the database
	 * @param queueId The ID of the queue to delete
	 * @return True on success and false on error
	 */

	public static boolean delete(int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL RemoveQueue(?)}");
			procedure.setInt(1, queueId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("RemoveQueue says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}
	/**
	 * Sets the global access column of the given queue to true
	 * @param queueId The ID of the queue to check
	 * @return True on success and false on failure
	 */
	public static boolean makeGlobal(int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL MakeQueueGlobal(?)}");
			procedure.setInt(1, queueId);
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Sets the global_access column of the given queue to false
	 * @param queueId The ID of hte queue to remove
	 * @return True on success and false on error
	 */
	public static boolean removeGlobal(int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL RemoveQueueGlobal(?)}");
			procedure.setInt(1, queueId);
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Sets the given queue to be the system test queue.
	 * @param queueId The ID of the queue to make the test queue.
	 * @return True on success and false on error.
	 */
	public static boolean setTestQueue(int queueId) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL SetTestQueue(?)}");
			procedure.setInt(1,queueId);
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
	 * Returns all.q, which is the one queue in the system that is always guaranteed to exist
	 * @return The default queue, or null if it could not be found.
	 */
	public static Queue getAllQ() {
		return Queues.get(R.DEFAULT_QUEUE_ID);
	}
	
	/**
	 * Gets the queue that should be used for running test jobs. If no such queue
	 * is currently set, it will be set to all.q before returning. This is to ensure
	 * a test queue is always set
	 * @return The id of the queue, or -1 on error
	 */
	public static int getTestQueue() {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetTestQueue()}");
			results=procedure.executeQuery();
			if (results.next()) {
				int id= results.getInt("test_queue");
				if (id<=0) {
					Queues.setTestQueue(R.DEFAULT_QUEUE_ID);
					return R.DEFAULT_QUEUE_ID;
				}
				return id;
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

}
