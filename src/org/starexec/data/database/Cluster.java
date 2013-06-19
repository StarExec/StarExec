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
import org.starexec.data.to.WorkerNode;

/**
 * Handles all database interaction for cluster resources (queues and worker nodes)
 */
public class Cluster {
	private static final Logger log = Logger.getLogger(Cluster.class);	
	
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
	
	/**
	 * Gets a worker node with detailed information (Id and name aint with all attributes)
	 * @param id The id of the node to get detailed information for
	 * @return A node object containing all of its attributes
	 * @author Wyatt Kaiser
	 */
	public static WorkerNode getNodeDetails(int id, int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			return Cluster.getNodeDetails(con, id, userId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets a worker node with detailed information (Id and name along with all attributes)
	 * @param con The connection to make the query with
	 * @param id The id of the node to get detailed information for
	 * @return A node object containing all of its attributes
	 * @author Tyler Jensen
	 */
	protected static WorkerNode getNodeDetails(Connection con, int id) throws Exception {				
		CallableStatement procedure = con.prepareCall("{CALL GetNodeDetails(?)}");
		procedure.setInt(1, id);			
		ResultSet results = procedure.executeQuery();
		WorkerNode node = new WorkerNode();
		
		if(results.next()){
			node.setName(results.getString("name"));
			node.setId(results.getInt("id"));
			node.setStatus(results.getString("status"));
		}							
		Common.closeResultSet(results);			
		
		return node;		
	}
	
	/**
	 * Gets a worker node with detailed information (Id and name along with all attributes)
	 * @param con The connection to make the query with
	 * @param id The id of the node to get detailed information for
	 * @return A node object containing all of its attributes
	 * @author Wyatt Kaiser
	 */
	protected static WorkerNode getNodeDetails(Connection con, int id, int userId) throws Exception {				
		CallableStatement procedure = con.prepareCall("{CALL GetNodeDetails(?)}");
		procedure.setInt(1, id);			
		ResultSet results = procedure.executeQuery();
		WorkerNode node = new WorkerNode();
		
		if(results.next()){
			node.setName(results.getString("name"));
			node.setId(results.getInt("id"));
			node.setStatus(results.getString("status"));
			
			//Get all the job pairs that are queued up on the queue
			List<JobPair> jobPairs = Jobs.getRunningPairsDetailed(results.getInt("id"));
			//List<JobPair> jobPairs = Jobs.getEnqueuedPairsDetailed(results.getInt("id"));

			for (JobPair j : jobPairs) {
				String[] jobInfo;
				jobInfo = new String[6];
				log.debug("JOBPAIR ID = " + j.getId());
				log.debug("JOB ID = " + j.getJobId());
				
				Job job = Jobs.getDetailedWithoutJobPairs(j.getJobId());
				log.debug("JOB = " + job);
				//jobInfo[0] = job.getName();
				jobInfo[0] = "TEST";
				jobInfo[1] = Users.getUserByJob(j.getJobId()).getFullName();

				if (Permissions.canUserSeeJob(job.getId(), userId)) {
					jobInfo[2] = (j.getBench().getName());
					jobInfo[3] = (j.getSolver().getName());
					jobInfo[4] = (j.getConfiguration().getName());
					
					//This function returns the space that the job is in, not the primitive
					//jobInfo[5] = Jobs.getSpace(j.getId()).getName();
					
					jobInfo[5] = j.getPath();
					
					/*String path = j.getPath();
					int index = path.lastIndexOf("/");
					if (index != -1) {
						path = path.substring(index + 1);
					}
					jobInfo[5] = (path);
					*/
				} else {
					jobInfo[2] = "private";
					jobInfo[3] = "private";
					jobInfo[4] = "private";
					jobInfo[5] = "private";
				}
				node.putJobPair(j.getId(), jobInfo);
			}
		}							
		Common.closeResultSet(results);			
		
		return node;		
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
				n.setId(results.getInt("id"));
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
	public static List<WorkerNode> getNodesForQueue(int id) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetNodesForQueue(?)}");
			procedure.setInt(1, id);
			ResultSet results = procedure.executeQuery();
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
		} finally {
			Common.safeClose(con);
		}
		
		return null;
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
						
			//log.debug(String.format("Status for node [%s] set to [%s]", (name == null) ? "<ALL>" : name, status));
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
