package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Job;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.WorkerNode;


/**
 * Handles all database interaction for cluster resources (queues and worker nodes)
 */


public class Cluster {
	private static final Logger log = Logger.getLogger(Cluster.class);	
	
	/**
	 * Gets the worker nodes from BACKEND and adds them to the database if they don't already exist. This must be done
	 * BEFORE queues have been loaded as the queues will make associations to the nodes.
	 */
	public static synchronized void loadWorkerNodes() {

		log.info("Loading worker nodes into the db");
		try {			

			// Set all nodes as inactive (we will update them to active as we see them)
			Cluster.setNodeStatus(R.NODE_STATUS_INACTIVE);
			
			String[] lines = R.BACKEND.getWorkerNodes();
			for (int i = 0; i < lines.length; i++) {
				String name = lines[i];
				log.debug("Updating info for node "+name);
				// In the database, update the attributes for the node
				Cluster.updateNode(name,  Cluster.getNodeDetails(name));				
				// Set the node as active (because we just saw it!)
				Cluster.setNodeStatus(name, R.NODE_STATUS_ACTIVE);
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		log.info("Completed loading info for worker nodes into db");
	}

	/**
	 * Calls BACKEND to get details about the given node. 
	 * @param name The name of the node to get details about
	 * @param name The name of the node to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getNodeDetails(String name) {
	    String[] map = R.BACKEND.getNodeDetails(name);
	    HashMap<String, String> details = new HashMap<String, String>();
	    
	    //only ever indexes an even number of elements, so if map.length == 5, will only look at first 4 elements and ignore fifth
	    for(int i = 0; i < (map.length/2)*2; i=i+2){
		details.put(map[i],map[i+1]);
	    }
	    return details;
	}


	/**
	 * Gets the queue list from BACKEND and adds them to the database if they don't already exist. This must
	 * be done AFTER nodes are loaded as the queues will make associations to the nodes. This also loads
	 * attributes for the queue as well as its current usage.
	 */
	public static synchronized void loadQueues() {
		Cluster.loadQueueDetails();
	}

	/**
	 * Loads the list of active queues on the system, loads their attributes into the database
	 * as well as their associations to worker nodes that belong to each queue.
	 */
	private static void loadQueueDetails() {
		log.info("Loading queue details into the db");
		try {			

			// Set all queues as inactive (we will set them as active when we see them)
			Queues.setStatus(R.QUEUE_STATUS_INACTIVE);

			String[] queueNames = R.BACKEND.getQueues();

			for (int i = 0; i < queueNames.length; i++) {
			    String name = queueNames[i];

			    log.debug("Loading details for queue "+name);

			    // In the database, update the attributes for the queue
			    Queues.update(name,  Cluster.getQueueDetails(name));

			    // Set the queue as active since we just saw it
			    Queues.setStatus(name, R.QUEUE_STATUS_ACTIVE);
			}
			//Adds all the associations to the db
			Cluster.setQueueAssociationsInDb();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} 
		log.info("Completed loading the queue details into the db");
	}

	/**
	 * Calls BACKEND to get details about the given queue. 
	 * @param name The name of the queue to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getQueueDetails(String name) {

	    String[] map = R.BACKEND.getQueueDetails(name);
	    HashMap<String, String> details = new HashMap<String, String>();
	    
	    //only ever indexes an even number of elements, so if map.length == 5, will only look at first 4 elements and ignore fifth
	    for(int i = 0; i < (map.length/2)*2; i=i+2){
		details.put(map[i],map[i+1]);
	    }
	    return details;

	}

	/**
	 * Extracts queue-node association from BACKEND and puts it into the db.
	 * @return true if successful
	 * @author Benton McCune
	 */
	public static Boolean setQueueAssociationsInDb() {
	    Queues.clearQueueAssociations();
	    String[] assoc = R.BACKEND.getQueueNodeAssociations();
	    
	    //only ever indexes an even number of elements, so if map.length == 5, will only look at first 4 elements and ignore fifth
	    for(int i = 0; i < (assoc.length/2)*2; i=i+2){
		Queues.associate(assoc[i], assoc[i+1]);
	    }
	    return true;
	    
	}


	public static void associateNodes(int queueId, List<Integer> nodeIds) {
		log.debug("Calling AssociateQueue");
		Connection con = null;
		CallableStatement associateQueue=null;
		try {		
			con = Common.getConnection();
			// Adds the nodes as associated with the queue
			for (int nodeId : nodeIds) {
				associateQueue = con.prepareCall("{CALL AssociateQueueById(?, ?)}");	
				associateQueue.setInt(1, queueId);
				associateQueue.setInt(2, nodeId);
				associateQueue.executeUpdate();
				Common.safeClose(associateQueue);
			}
		} catch (Exception e) {
			//if there was an error during the update, the procedure will not close
			Common.safeClose(associateQueue);
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}		
	}
	
	
	public static java.util.Date getLatestNodeDate() {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetLatestNodeDate()}");
			results = procedure.executeQuery();
			Date latest = null;
			while(results.next()){
				latest = results.getDate("MAX(reserve_date)");
			}	
		
			java.util.Date newDate = null;
			if (latest == null) {
				java.util.Calendar cal = java.util.Calendar.getInstance();
				java.util.Date utilDate = cal.getTime();
				java.sql.Date sqlDate = new Date(utilDate.getTime());
				newDate = new Date(sqlDate.getTime());
			} else {
				newDate = new Date(latest.getTime());
			}
			return newDate;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	public static int getNodeCount() {
		log.debug("Calling GetNodeCount");
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		int ret = 0;
		try {		
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetActiveNodeCount()}");
			results = procedure.executeQuery();	
			
			
			if (results.next()){
				ret = results.getInt("nodeCount");
			}						
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}	
		return ret;
	}	
	
	/**
	 * Gets a worker node with detailed information (Id and name along with all attributes)
	 * @param con The connection to make the query with
	 * @param id The id of the node to get detailed information for
	 * @return A node object containing all of its attributes
	 * @author Tyler Jensen
	 */
	protected static WorkerNode getNodeDetails(Connection con, int id) {				
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			procedure = con.prepareCall("{CALL GetNodeDetails(?)}");
			procedure.setInt(1, id);			
			results = procedure.executeQuery();
			WorkerNode node = new WorkerNode();
			if(results.next()){
				node.setName(results.getString("name"));
				node.setId(results.getInt("id"));
				node.setStatus(results.getString("status"));
			}							
			
			return node;
		} catch (Exception e) {
			log.error("getNodeDetails says "+e.getMessage(),e);
		}finally	{
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	/**
	 * Gets a worker node with detailed information (Id and name aint with all attributes)
	 * @param id The id of the node to get detailed information for
	 * @return A node object containing all of its attributes
	 * @author Tyler Jensen
	 */
	public static WorkerNode getNodeDetails(int id) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			return Cluster.getNodeDetails(con, id);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	
	public static List<WorkerNode> getNodesForNextPageAdmin(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String SearchQuery) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNextPageOfNodesAdmin(?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setString(5, SearchQuery);
			results = procedure.executeQuery();
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			
			while(results.next()){
				WorkerNode n = new WorkerNode();
				n.setId(results.getInt("id"));
				n.setName(results.getString("name"));
				n.setStatus(results.getString("status"));
				nodes.add(n);
				
							
			}	
			
			return nodes;
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
	 * Gets all nodes in the cluster that belong to the queue
	 * @param id The id of the queue to get nodes for
	 * @return A list of nodes that belong to the queue
	 * @author Tyler Jensen
	 */
	public static List<WorkerNode> getNodesForQueue(int id) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetNodesForQueue(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			
			while(results.next()){
				WorkerNode n = new WorkerNode();
				n.setName(results.getString("node.name"));
				n.setId(results.getInt("node.id"));
				n.setStatus(results.getString("node.status"));
				nodes.add(n);
			}			
						
			return nodes;
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
	 * retrieves all the nodes that are not reserved for a certain time
	 * @param sequenceName the name of the node to set the status for
	 * @param status the status to set for the node
	 * @return True if the operation was a success, false otherwise.
	 */
	public static List<WorkerNode> getUnReservedNodes(Date start, Date end) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();

			log.debug("start = " + start);
			procedure = con.prepareCall("{CALL GetUnReservedNodes(?, ?)}");
			procedure.setDate(1, start);	
			procedure.setDate(2, end);
			results = procedure.executeQuery();	
			
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			
			while(results.next()){
				WorkerNode n = new WorkerNode();
				n.setName(results.getString("name"));
				n.setId(results.getInt("id"));
				n.setStatus(results.getString("status"));
				nodes.add(n);
			}			
						
			return nodes;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}

	
	/**
	 * Updates the status of ALL worker nodes with the given status
	 * @param status The status to set for all nodes
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean setNodeStatus(String status) {
		return Cluster.setNodeStatus(null, status);
	}


	/**
	 * Updates the status of the given node with the given status
	 * @param name the name of the node to set the status for
	 * @param status the status to set for the node
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean setNodeStatus(String name, String status) {
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();
			
			if(name == null) {
				// If no name was supplied, apply to all nodes
				procedure = con.prepareCall("{CALL UpdateAllNodeStatus(?)}");
				procedure.setString(1, status);				
			} else {
				procedure = con.prepareCall("{CALL UpdateNodeStatus(?, ?)}");
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
		
		log.debug(String.format("Status for node [%s] failed to be updated.", (name == null) ? "ALL" : name));
		return false;
	}

	
	/**
	 * Takes in a node name and a hashmap representing an attribute for the node and its value. This method
	 * will add a column to the database for the attribute if it does not exist. If it does exist, the attribute
	 * for the given node is updated with the current value. If the given node does not exist, it is added to the database,
	 * or else all of its attributes are updated.
	 * @param name The name of the worker node to update/add
	 * @param attributes A list of key/value attributes to add/update for the node
	 * @return True if the operation was a success, false otherwise.
	 * @author Tyler Jensen
	 */
	public static boolean updateNode(String name, HashMap<String, String> attributes) {
		Connection con = null;			
		CallableStatement procAddNode = null;
		CallableStatement procAddCol = null;
		CallableStatement procUpdateAttr= null;
		try {
			con = Common.getConnection();
			
			// All or nothing!
			Common.beginTransaction(con);
			
			procAddNode = con.prepareCall("{CALL AddNode(?)}");
			procAddCol = con.prepareCall("{CALL AddColumnUnlessExists(?, ?, ?, ?)}");
			procUpdateAttr = con.prepareCall("{CALL UpdateNodeAttr(?, ?, ?)}");	
			
			// First, add the node (MySQL will ignore this if it already exists)
			procAddNode.setString(1, name);
			procAddNode.executeUpdate();
			
			// For each attribute for the node...
			for(Entry<String, String> keyVal : attributes.entrySet()) {				
				// Add a column for the attribute (MySQL will ignore if the column already exists)
				procAddCol.setString(1, R.MYSQL_DATABASE);
				procAddCol.setString(2, "nodes");				
				// Must add _ to column name in case the name conflicts with an SQL keyword
				procAddCol.setString(3, "_" + keyVal.getKey());
				procAddCol.setString(4, "VARCHAR(64)");
				procAddCol.execute();
				
				// Then update the column with the attribute's value for this node
				procUpdateAttr.setString(1, name);				
				// Must add _ to column name/value in case it conflicts with an SQL keyword
				procUpdateAttr.setString(2, "_" + keyVal.getKey());
				procUpdateAttr.setString(3, "_" +keyVal.getValue());
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
			Common.safeClose(procAddCol);
			Common.safeClose(procUpdateAttr);
			Common.safeClose(procAddNode);
		}
		
		log.debug(String.format("Node [%s] failed to be updated.", name));
		return false;
	}
	
	/**
	 * Updates the node count for a specific reserved queue on a specific day 
	 * @param queueId The ID of the queue 
	 * @param nodeCount
	 * @param date
	 * @return
	 */
	public static Boolean updateNodeCount(int requestId, int nodeCount, java.sql.Date date) {
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();
			

			procedure = con.prepareCall("{CALL UpdateReservedNodeCount(?,?,?)}");
			procedure.setInt(1, requestId);
			procedure.setInt(2, nodeCount);
			procedure.setDate(3, date);
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


	public static Boolean reserveNodes(int request_id, Date start, Date end) {
			
		List<java.sql.Date> dates =  Requests.getDateRange(start, end);

		for (java.sql.Date utilDate : dates) {
			int node_count = Requests.GetNodeCountOnDate(request_id, utilDate);
			
		    Boolean result = updateNodeCount(request_id, node_count, utilDate);
		    if (!result) {
		    	return false;
		    }
		}
		return true;
	}






	public static int getTempNodeCountOnDate(String name, java.util.Date date) {
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetTempNodeCountOnDate(?, ?)}");
			procedure.setString(1, name);
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());
			procedure.setDate(2, sqlDate);
			
			
			results = procedure.executeQuery();

			while(results.next()){
				return results.getInt("count");
			}			

			return -1;			
			
		} catch (Exception e){			
			log.error("GetTempNodeCountOnDate says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Removes all entries from the queue_request_assoc table where the node count is 0.
	 * This can happen if nodes are removed from a reservation after it was made
	 * @return True on success, false otherwise
	 */

	private static boolean removeEmptyNodeCounts() {
		Connection con = null;
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL RemoveEmptyNodeCounts()}");	
			
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			log.error("RemoveEmptyNodeCounts() says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}			
		return false;
	}
	
	public static int getMinNodeCount(int requestId) {
		return getMinOrMaxNodeCount(requestId,"min");
	}
	
	public static int getMaxNodeCount(int requestId) {
		return getMinOrMaxNodeCount(requestId,"max");
	}

	private static int getMinOrMaxNodeCount(int requestId, String minOrMax) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL Get"+minOrMax+"NodeCount(?)}");	
			procedure.setInt(1, requestId);
			
			results = procedure.executeQuery();
			
			while(results.next()){
				return results.getInt("count");
			}			
			
		} catch (Exception e){			
			log.error("GetMaxNodeCount says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}


	public static List<Job> getJobsRunningOnQueue(int queueId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();	
			
			procedure = con.prepareCall("{CALL GetJobsRunningOnQueue(?)}");	
			procedure.setInt(1, queueId);
			
			results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<Job>();
			
			while(results.next()){
				Job j = new Job();
				j.setId(results.getInt("id"));
				j.setUserId(results.getInt("user_id"));
				j.setName(results.getString("name"));
				int queue_Id = results.getInt("queue_id");
				Queue queue = Queues.get(queue_Id);
				j.setQueue(queue);
				jobs.add(j);
			}			
			
			return jobs;
			
		} catch (Exception e){			
			log.error("GetJobsRunningOnQueue says " + e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	public static List<WorkerNode> getAllNodes() {
		log.debug("Starting getAllNodes...");
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetAllNodes()}");
			results = procedure.executeQuery();
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			while (results.next()){
				WorkerNode n = new WorkerNode();
				n.setId(results.getInt("id"));
				n.setName(results.getString("name"));
				n.setStatus(results.getString("status"));
				nodes.add(n);
			}
			return nodes;
		} catch (Exception e) {
			log.error("GetAllNodes says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Gets all of the nodes that are NOT associated with the given queue
	 * @param queueId
	 * @return
	 */
	public static List<WorkerNode> getNonAttachedNodes(int queueId) {
		log.debug("Starting getNonAttachedNodes...");
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetNonAttachedNodes(?)}");
			procedure.setInt(1, queueId);
			results = procedure.executeQuery();
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			while (results.next()){
				WorkerNode n = new WorkerNode();
				n.setId(results.getInt("nodes.id"));
				n.setName(results.getString("nodes.name"));
				n.setStatus(results.getString("nodes.status"));
				Queue q=new Queue();
				q.setName(results.getString("queues.name"));
				q.setId(results.getInt("queues.id"));
				
				//we are displaying this data in a table, so we don't want a null name
				if (q.getName()==null) {
					q.setName("None");
				}
				n.setQueue(q);
				nodes.add(n);
			}
			return nodes;
		} catch (Exception e) {
			log.error("GetAllNodes says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}



	public static Queue getQueueForNode(WorkerNode node) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL GetQueueForNode(?)}");
			procedure.setInt(1, node.getId());
			results = procedure.executeQuery();
			Queue q = new Queue();
			while (results.next()){
				q.setId(results.getInt("id"));
				q.setName(results.getString("name"));
				q.setStatus(results.getString("status"));
				return q;
			}
		} catch (Exception e) {
			log.error("GetQueueForNode says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves the ID of a node given its name. 
	 * @param nodeName
	 * @return The ID of the node, or -1 if it couldn't be found
	 */
	public static int getNodeIdByName(String nodeName) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetNodeIdByName(?)}");
			procedure.setString(1, nodeName);
			results = procedure.executeQuery();
			while (results.next()) {
				return results.getInt("id");
			}
			return -1;
		} catch (Exception e) {
			log.error("GetNodeIdByName says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Gets the name of a node given its ID
	 * @param id
	 * @return
	 */
	public static String getNodeNameById(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetNodeNameById(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			while (results.next()) {
				return results.getString("name");
			}
			return null;
		} catch (Exception e) {
			log.error("GetNodeNameById says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	public static int getDefaultQueueId() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetDefaultQueueId(?)}");
			procedure.setString(1, R.DEFAULT_QUEUE_NAME);
			results = procedure.executeQuery();
			
			if (results.next()) {
				return results.getInt("id");
			}
			
			return -1;
			
		} catch (Exception e) {
			log.error("GetDefaultQueueId says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}


	public static boolean setQueueCommunityAccess(List<Integer> community_ids, int queue_id) {
		log.debug("SetQueueCommunityAccess beginning...");
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			if (community_ids != null) {
				for (int id : community_ids) {
					procedure = con.prepareCall("{CALL SetQueueCommunityAccess(?, ?)}");
					procedure.setInt(1, id);
					procedure.setInt(2, queue_id);
					
					procedure.executeUpdate();
				}
			}
			
			Common.endTransaction(con);
			
			return true;
			
		} catch (Exception e) {
			log.error("SetQueueCommunityAccess says " + e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
		
	}
}
