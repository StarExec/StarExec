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
import org.starexec.data.to.WorkerNode;

/**
 * Handles all database interaction for cluster resources (queues and worker nodes)
 */
public class Cluster {
	private static final Logger log = Logger.getLogger(Cluster.class);
	
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
				
				// Start from 3 (first two are name and ID)
				for(int i = 3; i < results.getMetaData().getColumnCount(); i++) {
					// Add the column name/value to the node's attributes
					node.putAttribute(results.getMetaData().getColumnName(i), results.getString(i));
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
				procAddCol.setString(3, keyVal.getKey());
				procAddCol.setString(4, "VARCHAR(64)");
				procAddCol.execute();
				
				// Then update the column with the attribute's value for this node
				procUpdateAttr.setString(1, name);
				procUpdateAttr.setString(2, keyVal.getKey());
				procUpdateAttr.setString(3, keyVal.getValue());
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
}
