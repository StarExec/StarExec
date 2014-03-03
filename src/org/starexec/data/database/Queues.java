package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Status;
import org.starexec.data.to.WorkerNode;


/**
 * Handles all DB interaction for queues
 * @author Tyler Jensen
 */
public class Queues {
	private static final Logger log = Logger.getLogger(Queues.class);

	/**
	 * Adds a new queue to the system. This action adds the queue, 
	 * and adds a new association to the queue for the given list of nodes
	 * This is a multi-step process, use transactions to ensure it completes as
	 * an atomic unit.
	 * @param con The connection to perform the operation on
	 * @param queueName The name of the queue to add
	 * @param nodeIds list of node ids to set associations for
	 * @return The ID of the newly inserted space, -1 if the operation failed
	 * @author Tyler Jensen
	 */
	protected static int add(Connection con, String queueName) throws Exception {			
		log.debug("preparing to call sql procedures to add queue");
		CallableStatement addQueue = null;
		CallableStatement associateQueue = null;
		try {
			
			//Add the queue first
			log.debug("Calling AddQueue");
			log.debug("queueName = " + queueName);
			addQueue = con.prepareCall("{CALL AddQueue(?, ?)}");	
			addQueue.setString(1, queueName);
			addQueue.registerOutParameter(2, java.sql.Types.INTEGER);
			addQueue.executeUpdate();
			int newQueueId = addQueue.getInt(2);
			
			log.info(String.format("New queue with name [%s] was successfully created", queueName));
			return newQueueId;
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(addQueue);
			Common.safeClose(associateQueue);
		}
		return -1;
	}
	
	/**
	 * Adds a new queue to the system.
	 * @param queueName The name of the queue to add
	 * @return The ID of the newly inserted queue, -1 if the operation failed
	 * @author Wyatt Kaiser
	 */
	public static int add(String queueName) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
						
			Common.beginTransaction(con);	

			// Add queue is a multi-step process, so we need to use a transaction
			int newQueueId = Queues.add(con, queueName);

			Common.endTransaction(con);			
			
			return newQueueId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.doRollback(con);			
			Common.safeClose(con);
		}
		
		return -1;
	}
	
	/**
	 * Associates a queue and a worker node to indicate the node belongs to the queue.
	 * If the association already exists, any errors are ignored.
	 * @param queueName The FULL name of the owning queue
	 * @param nodeName The FULL name of the worker node that belongs to the queue
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
	 */
	protected static Queue get(Connection con, int qid) throws Exception {	
		log.debug("starting get");
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
	 * Gets all queues in the starexec cluster
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
	 * Gets all queues in the starexec cluster (excluding 'permanent' queues)
	 * @return A list of queues
	 * @author Wyatt Kaiser
	 */
	public static List<Queue> getAllNonPermanent() {
		return getQueues(-3);
	}

	/**
	 * Gets a queue with detailed information (Id and name along with all attributes)
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
	
	/**
     * Gets jobs with pending job pairs for the given queue
     * @param queueId the id of the queue
     * @return the list of Jobs for that queue which have pending job pairs
     * @author Ben McCune and Aaron Stump
     */
	public static List<Job> getEnqueuedJobs(int queueId) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetEnqueuedJobs(?)}");					
			procedure.setInt(1, queueId);					
			 results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();

			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));	
				j.setPrimarySpace(results.getInt("primary_space"));
				j.getQueue().setId(results.getInt("queue_id"));
				j.setPreProcessor(Processors.get(con, results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con, results.getInt("post_processor")));

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
	 * Gets all job pairs that are enqueued(up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated)
	 * @param con The connection to make the query on 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getEnqueuedPairsDetailed(Connection con, int qId) throws Exception {	
		CallableStatement procedure = null;
		ResultSet results = null;
		
		try {
			 procedure = con.prepareCall("{CALL GetEnqueuedJobPairsByQueue(?,?)}");
			procedure.setInt(1, qId);					
			procedure.setInt(2, R.NUM_JOB_SCRIPTS);
			 results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				//jp.setNode(Cluster.getNodeDetails(con, results.getInt("node_id")));	
				jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
				//jp.setBench(Benchmarks.get(con, results.getInt("bench_id")));
				jp.setBench(Benchmarks.get(results.getInt("bench_id")));
				//jp.setSolver(Solvers.getSolverByConfig(con, results.getInt("config_id")));//not passing con
				jp.setSolver(Solvers.getSolverByConfig(results.getInt("config_id"),false));
				jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				//s.setStatus(results.getString("status.status"));
				//s.setDescription(results.getString("status.description"));
				jp.setStatus(s);
				jp.setAttributes(JobPairs.getAttributes(con, jp.getId()));
				returnList.add(jp);
			}			

			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			log.error("getEnqueuedPairsDetailed says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**
	 * Gets all job pairs that are enqueued (up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	public static List<JobPair> getEnqueuedPairsDetailed(int qId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getEnqueuedPairsDetailed(con, qId);
		} catch (Exception e){			
			log.error("getEnqueuedPairsDetailed for queue " + qId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}
	
	public static int getIdByName(String queueName) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetIdByName(?)}");
			procedure.setString(1, queueName);
			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("id");
			}			

			return -1;			
			
		} catch (Exception e){			
			log.error("getIdByName says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
			return -1;				
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
		if (type == "queue") {
			return getNextPageOfEnqueuedJobPairs(startingRecord, recordsPerPage, isSortedASC, indexOfColumnSortedBy, searchQuery, id);
		} else if (type == "node") {
			return getNextPageOfRunningJobPairs(startingRecord, recordsPerPage, isSortedASC, indexOfColumnSortedBy, searchQuery, id);
		} else {
			return null;
		}
	}
	
	/**
	 * Gets enqueued job pairs for the next page of a client-side datatables page
	 * @param startingRecord The first desired records
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether the records should be sorted ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The index of the client side datatables column to sort on
	 * @param searchQuery The search query to filter the results by
	 * @param id The ID of the queue
	 * @return A List of JobPairs enqueued in the given queue
	 */
	//TODO: Can this and getNextPageOfRunningJobPairs be combined?
	private static List<JobPair> getNextPageOfEnqueuedJobPairs(int startingRecord, int recordsPerPage, boolean isSortedASC,int indexOfColumnSortedBy, String searchQuery, int id) {
		Connection con = null;		
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetNextPageOfEnqueuedJobPairs(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, id);
			procedure.setString(6, searchQuery);
			
			
			 results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
				jp.setBench(Benchmarks.get(results.getInt("bench_id")));
				jp.setSolver(Solvers.getSolverByConfig(results.getInt("config_id"),false));
				jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				jp.setAttributes(JobPairs.getAttributes(con, jp.getId()));
				returnList.add(jp);
			}			

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
	 * Gets running job pairs for the next page of a client-side datatables page
	 * @param startingRecord The first desired records
	 * @param recordsPerPage The number of records to return
	 * @param isSortedASC Whether the records should be sorted ASC (true) or DESC (false)
	 * @param indexOfColumnSortedBy The index of the client side datatables column to sort on
	 * @param searchQuery The search query to filter the results by
	 * @param id The ID of the node
	 * @return A List of JobPairs  running on the node
	 */
	
	private static List<JobPair> getNextPageOfRunningJobPairs(int startingRecord, int recordsPerPage, boolean isSortedASC,int indexOfColumnSortedBy, String searchQuery, int id) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetNextPageOfRunningJobPairs(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, id);
			procedure.setString(6, searchQuery);
			
			
			 results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
				jp.setBench(Benchmarks.get(results.getInt("bench_id")));
				jp.setSolver(Solvers.getSolverByConfig(results.getInt("config_id"),false));
				jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
				
				Status s = new Status();
				s.setCode(results.getInt("status_code"));
				
				jp.setStatus(s);
				jp.setAttributes(JobPairs.getAttributes(con, jp.getId()));
				returnList.add(jp);
			}			

			Common.safeClose(results);
			return returnList;			
			
		} catch (Exception e){			
			log.error("getNextPageOfRunningJobPairs for node " + id + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
			return null;		
	}
	
	 public static int getNodeCountOnDate(int queueId, java.util.Date date) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetNodeCountOnDate(?, ?)}");
			procedure.setInt(1, queueId);
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());
			procedure.setDate(2, sqlDate);
			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("count");
			}			

			return 0;			
			
		} catch (Exception e){			
			log.error("GetNodeCountOnDate says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
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
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));				
				j.setDescription(results.getString("description"));				
				j.setCreateTime(results.getTimestamp("created"));	
				j.setPrimarySpace(results.getInt("primary_space"));
				j.getQueue().setId(results.getInt("queue_id"));
				j.setPreProcessor(Processors.get(con, results.getInt("pre_processor")));
				j.setPostProcessor(Processors.get(con, results.getInt("post_processor")));

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
			} else if (userId == -3) {
				procedure = con.prepareCall("{CALL GetAllQueuesNonPermanent}");
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
	
	public static List<Queue> getQueuesForJob(int userId, int spaceId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllQueuesForJob(?,?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);

			results = procedure.executeQuery();
			List<Queue> queues = new LinkedList<Queue>();
			
			while(results.next()){
				Queue q = new Queue();
				q.setName(results.getString("name"));
				q.setId(results.getInt("id"));	
				q.setStatus(results.getString("status"));
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
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getRunningPairsDetailed(Connection con, int qId) throws Exception {	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetRunningJobPairsByQueue(?,?)}");
			procedure.setInt(1, qId);					
			procedure.setInt(2, R.NUM_JOB_SCRIPTS);
			results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<JobPair>();

			while(results.next()){
				JobPair jp = JobPairs.resultToPair(results);
				jp.setNode(Cluster.getNodeDetails(results.getInt("node_id")));	
				jp.setBench(Benchmarks.get(results.getInt("bench_id")));
				jp.setSolver(Solvers.getSolverByConfig(results.getInt("config_id"),false));
				jp.setConfiguration(Solvers.getConfiguration(results.getInt("config_id")));
				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				jp.setAttributes(JobPairs.getAttributes(con, jp.getId()));
				returnList.add(jp);
			}			

			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			log.error("getRunningPairsDetailed says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	
	/**
	 * Gets all job pairs that are running (up to limit) for the given queue and also populates its used resource TOs 
	 * (Worker node, status, benchmark and solver WILL be populated) 
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	public static List<JobPair> getRunningPairsDetailed(int qId) {
		Connection con = null;			

		try {			
			con = Common.getConnection();		
			return getRunningPairsDetailed(con, qId);
		} catch (Exception e){			
			log.error("getRunningPairsDetailed for job " + qId + " says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;		
	}
	
	/**
	 * Returns the number of jobs with enqueued pairs in the given queue
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
	
	public static List<Queue> getUnreservedQueues(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUnreservedQueues(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			List<Queue> queues = new LinkedList<Queue>();
			
			while(results.next()){
				Queue q = new Queue();
				q.setName(results.getString("name"));
				q.setId(results.getInt("id"));	
				q.setStatus(results.getString("status"));
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
	 * Gets all queues in the starexec cluster accessible by the user with the given id
	 * @return A list of queues that are defined the cluster
	 * @author Aaron Stump
	 */
	public static List<Queue> getUserQueues(int userId) {
	    return getQueues(userId);
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
	 * Takes in a queue name and a hashmap representing queue attributes their values. This method will add a column 
	 * to the database for the attribute if it does not exist. If it does exist, the attribute for the given queue is 
	 * updated with the current value. If the given queue does not exist, it is added to the database, or else all of 
	 * its attributes are updated.
	 * @param name The name of the queue to update/add
	 * @param attributes A list of key/value attributes to add/update for the queue
	 * @return True if the operation was a success, false otherwise.
	 * @author Tyler Jensen
	 */
	public static boolean update(String name, HashMap<String, String> attributes) {
		Connection con = null;	
		CallableStatement procAddCol = null;
		CallableStatement procUpdateAttr = null;
		try {
			con = Common.getConnection();
			
			// All or nothing!
			Common.beginTransaction(con);
			
			add(con,name);
			 procAddCol = con.prepareCall("{CALL AddColumnUnlessExists(?, ?, ?, ?)}");
			 procUpdateAttr = con.prepareCall("{CALL UpdateQueueAttr(?, ?, ?)}");	
			
			
			// For each attribute for the queue...
			for(Entry<String, String> keyVal : attributes.entrySet()) {				
				// Add a column for the attribute (MySQL will ignore if the column already exists)
				procAddCol.setString(1, R.MYSQL_DATABASE);
				procAddCol.setString(2, "queues");
				// Must add _ to column name/value in case it conflicts with an SQL keyword
				procAddCol.setString(3, "_" + keyVal.getKey());
				procAddCol.setString(4, "VARCHAR(64)");
				procAddCol.execute();
				
				// Then update the column with the attribute's value for this node
				procUpdateAttr.setString(1, name);				
				// Must add _ to column name/value in case it conflicts with an SQL keyword
				procUpdateAttr.setString(2, "_" + keyVal.getKey());
				procUpdateAttr.setString(3, "_" + keyVal.getValue());
				procUpdateAttr.executeUpdate();
			}
						
			// Done, commit the changes
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procUpdateAttr);
			Common.safeClose(procAddCol);
		}
		
		log.debug(String.format("Queue [%s] failed to be updated.", name));
		return false;
	}

	/**
	 * Updates the usage of the given queue
	 * @param q The queue to update the useage for. Must have a name and usage attributes
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean updateUsage(Queue q) {
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			 procedure = con.prepareCall("{CALL UpdateQueueUseage(?, ?, ?, ?, ?)}");
			
			procedure.setString(1, q.getName());
			procedure.setInt(2, q.getSlotsTotal());
			procedure.setInt(3, q.getSlotsAvailable());
			procedure.setInt(4, q.getSlotsUsed());
			procedure.setInt(5, q.getSlotsReserved());
			procedure.executeUpdate();
			
			//log.debug(String.format("Updated usage for queue [%s] successfully [U%d/R%d/T%d]", q.getName(), q.getSlotsUsed(), q.getSlotsReserved(), q.getSlotsTotal()));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.debug(String.format("Usage for queue [%s] failed to be updated.", q.getName()));
		return false;
	}

	public static boolean notUniquePrimitiveName(String queue_name) {
		log.debug("staring notUniquePrimitiveName");
		// Initiate sql connection facilities.
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		
		try {
			// If the type of the primitive is solver.
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL countQueueName(?)}");
			procedure.setString(1, queue_name);
			
			results = procedure.executeQuery();		
			
			if(results.next()){
				if(results.getInt(1) != 0) {
					return true;
				}
				return false;
			}
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return true;
	}

	public static String getNameById(int queue_id) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetNameById(?)}");
			procedure.setInt(1, queue_id);
			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getString("name");
			}			

			return null;			
			
		} catch (Exception e){			
			log.error("getIdByName says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	public static boolean isQueuePermanent(int queue_id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL IsQueuePermanent(?)}");
			procedure.setInt(1, queue_id);
			
			results = procedure.executeQuery();
			boolean permanent = false;
			while(results.next()) {
				permanent = results.getBoolean("permanent");
			}
			return permanent;
		} catch (Exception e) {
			log.error("IsQueuePermanent says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	public static boolean makeQueuePermanent(int queue_id) {
		Connection con = null;
		CallableStatement MakePermanent = null;
		CallableStatement DeleteEntries = null;
		CallableStatement DeleteAssociation = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			//Set permanent flag in queues
			MakePermanent = con.prepareCall("{CALL MakeQueuePermanent(?)}");
			MakePermanent.setInt(1, queue_id);
			MakePermanent.executeUpdate();
			
			//remove all entries in queue_reserved
			DeleteEntries = con.prepareCall("{CALL RemoveReservedEntries(?)}");
			DeleteEntries.setInt(1, queue_id);
			DeleteEntries.executeUpdate();
			
			//Delete from comm_queue
			DeleteAssociation = con.prepareCall("{CALL RemoveQueueAssociation(?)}");
			DeleteAssociation.setInt(1, queue_id);
			DeleteAssociation.executeUpdate();
			
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error("MakeQueuePermanent says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(MakePermanent);
			Common.safeClose(DeleteEntries);
			Common.safeClose(DeleteAssociation);
		}
		return false;
	}

	public static void delete(int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL RemoveQueue(?)}");
			procedure.setInt(1, queueId);
			procedure.executeUpdate();

		} catch (Exception e) {
			log.error("RemoveQueue says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

}
