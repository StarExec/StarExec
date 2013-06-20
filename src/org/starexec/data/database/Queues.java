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
import org.starexec.data.to.WorkerNode;
import org.starexec.util.SessionUtil;

/**
 * Handles all DB interaction for queues
 * @author Tyler Jensen
 */
public class Queues {
	private static final Logger log = Logger.getLogger(Queues.class);

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
		    Common.safeClose(procedure);
		    Common.safeClose(con);
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
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return false;
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
	 * Gets a queue with very basic information, not including any SGE attributes with the queue
	 * @param con The connection to make the query with
	 * @param qid The id of the queue to retrieve
	 * @return a queue object representing the queue to retrieve
	 */
	protected static Queue get(Connection con, int qid) throws Exception {		
	    CallableStatement procedure = null;
	    ResultSet results = null;
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
		
	    } finally {
		Common.safeClose(results);
		Common.safeClose(procedure);
	    }
	    return null;
	}	

	
	/**
	 * Gets a queue with detailed information (Id and name along with all attributes)
	 * @param id The id of the queue to get detailed information for
	 * @return A queue object containing all of its attributes
	 * @author Tyler Jensen
	 */
	public static Queue getDetails(int id, int userId) {
		log.debug("USER ID = " + userId);
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

				//Get all the job pairs that are queued up on the queue
				List<JobPair> jobPairs = Jobs.getEnqueuedPairsDetailed(results.getInt("id"));

				for (JobPair j : jobPairs) {
					String[] jobInfo;
					jobInfo = new String[6];
					
					Job job = Jobs.getDetailedWithoutJobPairs(j.getJobId());
					jobInfo[0] = job.getName();
					jobInfo[1] = Users.getUserByJob(j.getJobId()).getFullName();

					//Need to replace true with a function that determines if a user can see
					//the job (i.e. is a member of the space)
					//Users.isMemberOfSpace(userId, spaceId)
					//Permissions.canUserSeeJob(jobId, userId)
					if (Permissions.canUserSeeJob(job.getId(), userId)) {
						jobInfo[2] = (j.getBench().getName());
						jobInfo[3] = (j.getSolver().getName());
						jobInfo[4] = (j.getConfiguration().getName());
						
						//This function returns the space that the job is in, not the primitive
						jobInfo[5] = Jobs.getSpace(j.getId()).getName();
						
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
					queue.putJobPair(j.getId(), jobInfo);
				}
			}
						
			return queue;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
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
			
			if (userId == 0) 
			    procedure = con.prepareCall("{CALL GetAllQueues}");
			else {
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
		    Common.safeClose(results);
		    Common.safeClose(procedure);
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
	 * Gets all queues in the starexec cluster accessible by the user with the given id
	 * @return A list of queues that are defined the cluster
	 * @author Aaron Stump
	 */
	public static List<Queue> getUserQueues(int userId) {
	    return getQueues(userId);
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
		CallableStatement procAddQueue = null;		
		CallableStatement procAddCol = null;
		CallableStatement procUpdateAttr = null;
		try {
			con = Common.getConnection();
			
			// All or nothing!
			Common.beginTransaction(con);
			
			procAddQueue = con.prepareCall("{CALL AddQueue(?)}");
			procAddCol = con.prepareCall("{CALL AddColumnUnlessExists(?, ?, ?, ?)}");
			procUpdateAttr = con.prepareCall("{CALL UpdateQueueAttr(?, ?, ?)}");	
			
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
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
		    Common.safeClose(procAddQueue);
		    Common.safeClose(procAddCol); 
		    Common.safeClose(procUpdateAttr); 
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
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		log.debug(String.format("Usage for queue [%s] failed to be updated.", q.getName()));
		return false;
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
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		log.debug(String.format("Status for queue [%s] failed to be updated.", (name == null) ? "ALL" : name));
		return false;
	}
}
