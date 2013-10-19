package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
	
	public static void associateNodes(int queueId, List<Integer> nodeIds) {
		log.debug("Calling AssociateQueue");
		Connection con = null;
		CallableStatement procedure = null;
		try {		
			con = Common.getConnection();
			// Adds the nodes as associated with the queue
			for (int nodeId : nodeIds) {
				CallableStatement associateQueue = con.prepareCall("{CALL AssociateQueueById(?, ?)}");	
				associateQueue.setInt(1, queueId);
				associateQueue.setInt(2, nodeId);
				associateQueue.executeUpdate();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
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
		
			log.debug("latest = " + latest);
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
		try {		
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetNodeCount()}");
			ResultSet results = procedure.executeQuery();	
			
			
			while(results.next()){
				return results.getInt("nodeCount");
			}						
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}	
		return 0;
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
	 * @param name the name of the node to set the status for
	 * @param status the status to set for the node
	 * @return True if the operation was a success, false otherwise.
	 */
	public static List<WorkerNode> getUnReservedNodes(Date start, Date end) {
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection();
			ResultSet results=null;

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
		}
		
		return null;
	}


	public static Boolean reserveNodes(int queue_id, int node_count, Date start, Date end) {
			
		//Get all the dates between these two dates
	    List<java.util.Date> dates = new ArrayList<java.util.Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(start);
	    while (calendar.getTime().before(end))
	    {
	        java.util.Date result = calendar.getTime();
	        dates.add(result);
	        calendar.add(Calendar.DATE, 1);
	    }
	    java.util.Date latestResult = calendar.getTime();
	    dates.add(latestResult);
		
		for (java.util.Date utilDate : dates) {
		    Boolean result = updateNodeCount(node_count, queue_id, utilDate);
		    if (!result) {
		    	return false;
		    }
		}
		return true;
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
			
			procedure = null;
			
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

	public static Boolean updateNodeCount(int nodeCount, int queueId, java.util.Date date) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
		    java.sql.Date sqlDate = new java.sql.Date(date.getTime());

			
			procedure = con.prepareCall("{CALL UpdateReservedNodeCount(?,?,?)}");
			procedure.setInt(1, nodeCount);
			procedure.setInt(2, queueId);
			procedure.setDate(3, sqlDate);
			procedure.executeUpdate();

			log.debug("successfully updated NodeCount for queue " + queueId);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
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
	public static void updateNodeDate(int nodeId, int queueId, Date start, Date end, String queueCode) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			

			List<Date> dates = new ArrayList<Date>();
		    Calendar calendar = new GregorianCalendar();
		    calendar.setTime(start);
		    while (calendar.getTime().before(end))
		    {
		        Date result = (Date) calendar.getTime();
		        dates.add(result);
		        calendar.add(Calendar.DATE, 1);
		    }

		    for (Date day : dates) {
		    	procedure = con.prepareCall("{CALL UpdateNodeDate(?, ?, ?, ?)}");
				procedure.setInt(1, nodeId);
				procedure.setInt(2, queueId);
				procedure.setDate(3, day);
				procedure.setString(4, queueCode);
				procedure.executeUpdate();
		    }
				
					

		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		log.debug(String.format("Node [%s] successfuly updated.", nodeId));
	}

}
