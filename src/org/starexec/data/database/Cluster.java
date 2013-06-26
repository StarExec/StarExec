package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.starexec.app.RESTHelpers;
import org.starexec.constants.R;

import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.User;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.Util;

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
	 * Gets a worker node with detailed information (Id and name with all attributes)
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
			
			//Get all the job pairs that are running on the queue
			List<JobPair> jobPairs = Jobs.getRunningPairsDetailed(results.getInt("id"));

			for (JobPair j : jobPairs) {
				StringBuilder sb = new StringBuilder();
				String hiddenJobPairId;
				
				// Create the hidden input tag containing the jobpair id
				sb.append("<input type=\"hidden\" value=\"");
				sb.append(j.getId());
				sb.append("\" name=\"pid\"/>");
				hiddenJobPairId = sb.toString();
				
				// Create the job link
				Job job = Jobs.getDetailedWithoutJobPairs(j.getJobId());
	    		sb = new StringBuilder();
	    		sb.append("<a href=\""+Util.docRoot("secure/details/job.jsp?id="));
	    		sb.append(job.getId());
	    		sb.append("\" target=\"_blank\">");
	    		sb.append(job.getName());
	    		RESTHelpers.addImg(sb);
	    		sb.append(hiddenJobPairId);
				String jobLink = sb.toString();
				
				//Create the User Link
	    		sb = new StringBuilder();
				String hiddenUserId;
				User user = Users.getUserByJob(j.getJobId());
				// Create the hidden input tag containing the user id
				if(user.getId() == userId) {
					sb.append("<input type=\"hidden\" value=\"");
					sb.append(user.getId());
					sb.append("\" name=\"currentUser\" id=\"uid"+user.getId()+"\" prim=\"user\"/>");
					hiddenUserId = sb.toString();
				} else {
					sb.append("<input type=\"hidden\" value=\"");
					sb.append(user.getId());
					sb.append("\" id=\"uid"+user.getId()+"\" prim=\"user\"/>");
					hiddenUserId = sb.toString();
				}
	    		sb = new StringBuilder();
	    		sb.append("<a href=\""+Util.docRoot("secure/details/user.jsp?id="));
	    		sb.append(user.getId());
	    		sb.append("\" target=\"_blank\">");
	    		sb.append(user.getFullName());
	    		RESTHelpers.addImg(sb);
	    		sb.append(hiddenUserId);
				String userLink = sb.toString();

	    		// Create the benchmark link
	    		sb = new StringBuilder();
	    		sb.append("<a title=\"");
	    		sb.append(j.getBench().getDescription());
	    		sb.append("\" href=\""+Util.docRoot("secure/details/benchmark.jsp?id="));
	    		sb.append(j.getBench().getId());
	    		sb.append("\" target=\"_blank\">");
	    		sb.append(j.getBench().getName());
	    		RESTHelpers.addImg(sb);
	    		sb.append(hiddenJobPairId);
				String benchLink = sb.toString();
				
				// Create the solver link
	    		sb = new StringBuilder();
	    		sb.append("<a title=\"");
	    		sb.append(j.getSolver().getDescription());
	    		sb.append("\" href=\""+Util.docRoot("secure/details/solver.jsp?id="));
	    		sb.append(j.getSolver().getId());
	    		sb.append("\" target=\"_blank\">");
	    		sb.append(j.getSolver().getName());
	    		RESTHelpers.addImg(sb);
				String solverLink = sb.toString();
				
				// Create the configuration link
	    		sb = new StringBuilder();
	    		sb.append("<a title=\"");
	    		sb.append(j.getSolver().getConfigurations().get(0).getDescription());
	    		sb.append("\" href=\""+Util.docRoot("secure/details/configuration.jsp?id="));
	    		sb.append(j.getSolver().getConfigurations().get(0).getId());
	    		sb.append("\" target=\"_blank\">");
	    		sb.append(j.getSolver().getConfigurations().get(0).getName());
	    		RESTHelpers.addImg(sb);
				String configLink = sb.toString();
				
				
				
				String[] jobInfo;
				jobInfo = new String[6];				

				jobInfo[1] = jobLink;
				jobInfo[2] = userLink;
				if (Permissions.canUserSeeJob(job.getId(), userId)) {
					jobInfo[2] = benchLink;
					jobInfo[3] = solverLink;
					jobInfo[4] = configLink;
					jobInfo[5] = j.getPath();
				} else {
					jobInfo[2] = "private";
					jobInfo[3] = "private";
					jobInfo[4] = "private";
					jobInfo[5] = "private";
				}
				log.debug("JobInfo" + jobInfo);
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
