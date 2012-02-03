package org.starexec.util;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;

/**
 * Contains methods for interacting with the sun grid engine. This class is NOT operating system independent
 * and may require environmental set up to function properly in Windows.
 * @author Tyler Jensen
 */
public class GridEngineUtil {
	private static final Logger log = Logger.getLogger(GridEngineUtil.class);
	
	// The regex patterns used to parse SGE output
	private static Pattern nodeKeyValPattern;
	private static Pattern queueKeyValPattern;
	private static Pattern queueAssocPattern;
	
	static {
		// Compile the SGE output parsing patterns when this class is loaded
		nodeKeyValPattern = Pattern.compile(R.NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueKeyValPattern = Pattern.compile(R.QUEUE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueAssocPattern = Pattern.compile(R.QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);
	}
	
	/**
	 * Gets the queue list from SGE and adds them to the database if they don't already exist. This must
	 * be done AFTER nodes are loaded as the queues will make associations to the nodes. This also loads
	 * attributes for the queue as well as its current usage.
	 */
	public static void loadQueues() {
		GridEngineUtil.loadQueueDetails();
		GridEngineUtil.loadQueueUsage();
	}
	
	/**
	 * Loads the list of active queues on the system, loads their attributes into the database
	 * as well as their associations to worker nodes that belong to each queue.
	 */
	private static void loadQueueDetails() {
		BufferedReader queueResults = null;
		
		try {			
			// Execute the SGE command to get the list of queues
			queueResults = Util.executeCommand(R.QUEUE_LIST_COMMAND);
			
			if(queueResults == null) {
				// If the command failed, return now	
				return;
			}
			
			// Set all queues as inactive (we will set them as active when we see them)
			Cluster.setQueueStatus(R.QUEUE_STATUS_INACTIVE);
			
			// Read the queue names one at a time
			String line;		
			while((line = queueResults.readLine()) != null) {
				String name = line;
								
				// In the database, update the attributes for the queue
				Cluster.updateQueue(name,  GridEngineUtil.getQueueDetails(name));
				
				// Set the queue as active since we just saw it
				Cluster.setQueueStatus(name, R.QUEUE_STATUS_ACTIVE);
				
				// For each of the queue's node's, add an association
				for(WorkerNode node : GridEngineUtil.getQueueAssociations(name)) {
					Cluster.associateQueue(name, node.getName());	
				}				
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} finally {
			// Try to close the result list if it is allowed
			try { queueResults.close(); } catch (Exception e) { }
		}
	}
	
	/**
	 * Grabs queue usage information from SGE and dumps it in the database. Usage is in terms of
	 * job slots available, used, reserved and total.
	 */
	public static void loadQueueUsage() { 
		try {
			// Call SGE to get details about the usage of all queues
			BufferedReader buff = Util.executeCommand(R.QUEUE_USAGE_COMMAND);
			
			// Read line twice to skip past the header info returned
			buff.readLine();
			buff.readLine();
						
			String line = "";
			// For each queue output...
			while((line = buff.readLine()) != null) {
				// The output is separated by white spaces, split on whitespace to get the data in array form
				String[] data = line.split("\\s+");
				
				// Create a new queue and pick out the data we want from the output (0 = name, 2-5 = usage stats)
				Queue q = new Queue();
				q.setName(data[0]);
				q.setSlotsUsed(Integer.parseInt(data[2]));
				q.setSlotsReserved(Integer.parseInt(data[3]));
				q.setSlotsAvailable(Integer.parseInt(data[4]));
				q.setSlotsTotal(Integer.parseInt(data[5]));
				
				// Update the database with the new usage stats
				Cluster.updateQueueUsage(q);
			}			
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Gets all of the worker nodes that belong to a queue 
	 * @param name The name of the queue to find worker nodes for
	 * @return A list of worker nodes that belong to the given queue
	 */
	public static List<WorkerNode> getQueueAssociations(String name) {
		// Create the list of nodes that will be returned		
		List<WorkerNode> nodes = new LinkedList<WorkerNode>();
		
		// Call SGE to get details for the given node (the child worker nodes are contained in the details output)
		String results = Util.bufferToString(Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name));
		
		// Parse the output from the SGE call to get the child worker nodes
		java.util.regex.Matcher matcher = queueAssocPattern.matcher(results);
		
		// For each match...
		while(matcher.find()) {
			// Parse out the worker node name from the regex parser and add it to the return list
			if(matcher.group().length() > 2) {
				String nodeName = matcher.group().substring(1, matcher.group().length() - 1);
				nodes.add(new WorkerNode(nodeName));
			}
		}

		return nodes;
	}
	
	/**
	 * Calls SGE to get details about the given queue. 
	 * @param name The name of the queue to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getQueueDetails(String name) {
		// Make the results hashmap that will be returned		
		HashMap<String, String> details = new HashMap<String, String>();
		
		// Call SGE to get details for the given node
		String results = Util.bufferToString(Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name));
		
		// Parse the output from the SGE call to get the key/value pairs for the node
		java.util.regex.Matcher matcher = queueKeyValPattern.matcher(results);
		
		// For each match...
		while(matcher.find()) {
			// Split apart the key from the value
			String[] keyVal = matcher.group().split("\\s+");
			
			// Add the results to the details hashmap
			details.put(keyVal[0], keyVal[1]);
		}
		
		return details;
	}	
	
	/**
	 * Gets the worker nodes from SGE and adds them to the database if they don't already exist. This must be done
	 * BEFORE queues have been loaded as the queues will make associations to the nodes.
	 */
	public static void loadWorkerNodes() {
		BufferedReader nodeResults = null;
		
		try {			
			// Execute the SGE command to get the node list
			nodeResults = Util.executeCommand(R.NODE_LIST_COMMAND);
			
			if(nodeResults == null) {
				// If the command failed, return now				
				return;
			}
			
			// Set all nodes as inactive (we will update them to active as we see them)
			Cluster.setNodeStatus(R.NODE_STATUS_INACTIVE);
			
			// Read the nodes one at a time
			String line;		
			while((line = nodeResults.readLine()) != null) {
				String name = line;							
				// In the database, update the attributes for the node
				Cluster.updateNode(name,  GridEngineUtil.getNodeDetails(name));				
				// Set the node as active (because we just saw it!)
				Cluster.setNodeStatus(name, R.NODE_STATUS_ACTIVE);
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} finally {
			// Try to close the result list if it is allowed
			try { nodeResults.close(); } catch (Exception e) { }
		}
	}
	
	/**
	 * Calls SGE to get details about the given node. 
	 * @param name The name of the node to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getNodeDetails(String name) {
		// Make the results hashmap that will be returned		
		HashMap<String, String> details = new HashMap<String, String>();
		
		// Call SGE to get details for the given node
		String results = Util.bufferToString(Util.executeCommand(R.NODE_DETAILS_COMMAND + name));
		
		// Parse the output from the SGE call to get the key/value pairs for the node
		java.util.regex.Matcher matcher = nodeKeyValPattern.matcher(results);
		
		// For each match...
		while(matcher.find()) {
			// Split apart the key from the value
			String[] keyVal = matcher.group().split("=");
			
			// Add the results to the details hashmap
			details.put(keyVal[0], keyVal[1]);
		}
		
		return details;
	}	
}