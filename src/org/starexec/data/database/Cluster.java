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
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;

/**
 * Handles all database interaction for cluster resources (queues and worker nodes)
 */
public class Cluster {
	private static final Logger log = Logger.getLogger(Cluster.class);
	
	/**
	 * Associates a queue and a worker node to indicate the node belongs to the queue.
	 * If the association already exists, any errors are ignored.
	 * @param queueName The FULL name of the owning queue
	 * @param nodeName The FULL name of the worker node that belongs to the queue
	 * @return True if the operation was a success, false otherwise. 
	 */
	public static boolean associateQueue(String queueName, String nodeName) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL AssociateQueue(?, ?)}");
			procedure.setString(1, queueName);
			procedure.setString(2, nodeName);
			
			procedure.executeUpdate();						
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Gets a worker node with detailed information (Id and name along with all attributes)
	 * @param id The id of the node to get detailed information for
	 * @return A node object containing all of its attributes
	 * @author Tyler Jensen
	 */
	public static WorkerNode getNodeDetails(long id) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetNodeDetails(?)}");
			procedure.setLong(1, id);			
			ResultSet results = procedure.executeQuery();
			WorkerNode node = new WorkerNode();
			
			if(results.next()){
				node.setName(results.getString("name"));
				node.setId(results.getLong("id"));
				node.setStatus(results.getString("status"));
				
				// Start from 4 (first three are ID, name and status)
				for(int i = 4; i < results.getMetaData().getColumnCount(); i++) {
					// Add the column name/value to the node's attributes (get substrings at 1 to remove the prepended _ to prevent keyword conflicts)
					node.putAttribute(results.getMetaData().getColumnName(i).substring(1), results.getString(i).substring(1));
				}				
			}			
						
			return node;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}

	/**
	 * Gets a queue with detailed information (Id and name along with all attributes)
	 * @param id The id of the queue to get detailed information for
	 * @return A queue object containing all of its attributes
	 * @author Tyler Jensen
	 */
	public static Queue getQueueDetails(long id) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetQueueDetails(?)}");
			procedure.setLong(1, id);			
			ResultSet results = procedure.executeQuery();
			Queue queue = new Queue();
			
			if(results.next()){
				queue.setName(results.getString("name"));
				queue.setId(results.getLong("id"));
				queue.setStatus(results.getString("status"));
				queue.setSlotsTotal(results.getInt("slots_total"));
				queue.setSlotsAvailable(results.getInt("slots_free"));
				queue.setSlotsReserved(results.getInt("slots_reserved"));
				queue.setSlotsUsed(results.getInt("slots_used"));
				
				// Start from 8 (first seven are ID, name, status and usage)
				for(int i = 8; i < results.getMetaData().getColumnCount(); i++) {
					// Add the column name/value to the node's attributes (get substrings at 1 to remove the prepended _ to prevent keyword conflicts)
					queue.putAttribute(results.getMetaData().getColumnName(i).substring(1), results.getString(i).substring(1));
				}				
			}			
						
			return queue;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets all nodes in the starexec cluster (with no detailed information)
	 * @return A list of nodes that are in the cluster
	 * @author Tyler Jensen
	 */
	public static List<WorkerNode> getAllNodes() {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetAllNodes}");
			ResultSet results = procedure.executeQuery();
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			
			while(results.next()){
				WorkerNode n = new WorkerNode();
				n.setName(results.getString("name"));
				n.setId(results.getLong("id"));
				n.setStatus(results.getString("status"));
				nodes.add(n);
			}			
						
			return nodes;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets all nodes in the cluster that belong to the queue
	 * @param id The id of the queue to get nodes for
	 * @return A list of nodes that belong to the queue
	 * @author Tyler Jensen
	 */
	public static List<WorkerNode> getNodesForQueue(long id) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetNodesForQueue(?)}");
			procedure.setLong(1, id);
			ResultSet results = procedure.executeQuery();
			List<WorkerNode> nodes = new LinkedList<WorkerNode>();
			
			while(results.next()){
				WorkerNode n = new WorkerNode();
				n.setName(results.getString("name"));
				n.setId(results.getLong("id"));
				n.setStatus(results.getString("status"));
				nodes.add(n);
			}			
						
			return nodes;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}	
	
	/**
	 * Gets all queues in the starexec cluster (with no detailed information)
	 * @return A list of queues that are defined the cluster
	 * @author Tyler Jensen
	 */
	public static List<Queue> getAllQueues() {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetAllQueues}");
			ResultSet results = procedure.executeQuery();
			List<Queue> queues = new LinkedList<Queue>();
			
			while(results.next()){
				Queue q = new Queue();
				q.setName(results.getString("name"));
				q.setId(results.getLong("id"));	
				q.setStatus(results.getString("status"));
				queues.add(q);
			}			
						
			return queues;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
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
	public static boolean updateQueue(String name, HashMap<String, String> attributes) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			// All or nothing!
			Common.beginTransaction(con);
			
			CallableStatement procAddQueue = con.prepareCall("{CALL AddQueue(?)}");
			CallableStatement procAddCol = con.prepareCall("{CALL AddColumnUnlessExists(?, ?, ?, ?)}");
			CallableStatement procUpdateAttr = con.prepareCall("{CALL UpdateQueueAttr(?, ?, ?)}");	
			
			// First, add the node (MySQL will ignore this if it already exists)
			procAddQueue.setString(1, name);
			procAddQueue.executeUpdate();
			
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
			log.debug(String.format("Updated [%d] attributes for queue [%s] successfully.", attributes.size(), name));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Queue [%s] failed to be updated.", name));
		return false;
	}
	
	/**
	 * Updates the usage of the given queue
	 * @param q The queue to update the useage for. Must have a name and usage attributes
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean updateQueueUsage(Queue q) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			CallableStatement procedure = con.prepareCall("{CALL UpdateQueueUseage(?, ?, ?, ?, ?)}");
			
			procedure.setString(1, q.getName());
			procedure.setInt(2, q.getSlotsTotal());
			procedure.setInt(3, q.getSlotsAvailable());
			procedure.setInt(4, q.getSlotsUsed());
			procedure.setInt(5, q.getSlotsReserved());
			procedure.executeUpdate();
			
			log.debug(String.format("Updated usage for queue [%s] successfully [U%d/R%d/T%d]", q.getName(), q.getSlotsUsed(), q.getSlotsReserved(), q.getSlotsTotal()));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Usage for queue [%s] failed to be updated.", q.getName()));
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
		
		try {
			con = Common.getConnection();
			
			// All or nothing!
			Common.beginTransaction(con);
			
			CallableStatement procAddNode = con.prepareCall("{CALL AddNode(?)}");
			CallableStatement procAddCol = con.prepareCall("{CALL AddColumnUnlessExists(?, ?, ?, ?)}");
			CallableStatement procUpdateAttr = con.prepareCall("{CALL UpdateNodeAttr(?, ?, ?)}");	
			
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
			log.debug(String.format("Updated [%d] attributes for node [%s] successfully.", attributes.size(), name));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Node [%s] failed to be updated.", name));
		return false;
	}
	
	/**
	 * Updates the status of ALL queues with the given status
	 * @param status The status to set for all queues
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean setQueueStatus(String status) {
		return Cluster.setQueueStatus(null, status);
	}
	
	/**
	 * Updates the status of the given queue with the given status
	 * @param name the name of the queue to set the status for
	 * @param status the status to set for the queue
	 * @return True if the operation was a success, false otherwise.
	 */
	public static boolean setQueueStatus(String name, String status) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			
			CallableStatement procedure = null;
			
			if(name == null) {
				// If no name was supplied, apply to all queues
				procedure = con.prepareCall("{CALL UpdateAllQueueStatus(?)}");
				procedure.setString(1, status);				
			} else {
				procedure = con.prepareCall("{CALL UpdateQueueStatus(?, ?)}");
				procedure.setString(1, name);
				procedure.setString(2, status);
			}
						
			log.debug(String.format("Status for queue [%s] set to [%s]", (name == null) ? "ALL" : name, status));
			procedure.executeUpdate();			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Status for queue [%s] failed to be updated.", (name == null) ? "ALL" : name));
		return false;
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
		
		try {
			con = Common.getConnection();
			
			CallableStatement procedure = null;
			
			if(name == null) {
				// If no name was supplied, apply to all nodes
				procedure = con.prepareCall("{CALL UpdateAllNodeStatus(?)}");
				procedure.setString(1, status);				
			} else {
				procedure = con.prepareCall("{CALL UpdateNodeStatus(?, ?)}");
				procedure.setString(1, name);
				procedure.setString(2, status);
			}
						
			log.debug(String.format("Status for node [%s] set to [%s]", (name == null) ? "ALL" : name, status));
			procedure.executeUpdate();			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Status for node [%s] failed to be updated.", (name == null) ? "ALL" : name));
		return false;
	}
}
