package org.starexec.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
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
	private static ExecutorService threadPool = null;

	@SuppressWarnings("unused")
	private static String testString = "queuename                      qtype resv/used/tot. load_avg arch          states\r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n001.star.cs.uiowa.edu   BIP   0/0/8          0.05     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n002.star.cs.uiowa.edu   BIP   0/0/8          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n003.star.cs.uiowa.edu   BIP   0/0/8          0.07     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n004.star.cs.uiowa.edu   BIP   0/0/8          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n001.q@n001.star.cs.uiowa.edu  BIP   0/0/1          0.05     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n002.q@n002.star.cs.uiowa.edu  BIP   0/0/1          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n003.q@n003.star.cs.uiowa.edu  BIP   0/0/1          0.07     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n004.q@n004.star.cs.uiowa.edu  BIP   0/0/1          0.02     lx24-amd64    ";

	static {
		// Compile the SGE output parsing patterns when this class is loaded
		nodeKeyValPattern = Pattern.compile(R.NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueKeyValPattern = Pattern.compile(R.QUEUE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueAssocPattern = Pattern.compile(R.QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);


		threadPool = Executors.newCachedThreadPool();		
	}

	/**
	 * Shuts down the reserved threadpool this util uses.
	 */
	public static void shutdown() throws Exception {
		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.SECONDS);
	}

	public static Session createSession() {
		try {
			log.debug("createSession() loading class."); 	
			// Try to load the class, if it does not exist this will cause an exception instead of an error			
			Class.forName("com.sun.grid.drmaa.SessionImpl");
		} catch (Exception e) {
			log.error("Error loading com.sun.grid.drmaa.SessionImpl");
		}

		Session s = SessionFactory.getFactory().getSession();
		try {
			s.init("");
		} catch (Exception e) {
			log.error("Error initializing the SGE session.");
		}
		return s;
	}

	public static void destroySession(Session s) {
		try {
			s.exit();
		}
		catch (Exception e) {
			log.error("Problem destroying session: "+e,e);
		}
	}

	/**
	 * Gets the queue list from SGE and adds them to the database if they don't already exist. This must
	 * be done AFTER nodes are loaded as the queues will make associations to the nodes. This also loads
	 * attributes for the queue as well as its current usage.
	 */
	public static synchronized void loadQueues() {
		GridEngineUtil.loadQueueDetails();
		//		GridEngineUtil.loadQueueUsage();
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
			Queues.setStatus(R.QUEUE_STATUS_INACTIVE);

			// Read the queue names one at a time
			String line;		
			while((line = queueResults.readLine()) != null) {
				String name = line;

				// In the database, update the attributes for the queue
				Queues.update(name,  GridEngineUtil.getQueueDetails(name));

				// Set the queue as active since we just saw it
				Queues.setStatus(name, R.QUEUE_STATUS_ACTIVE);
			}
			/*		// For each of the queue's node's, add an association
	      for(WorkerNode node : GridEngineUtil.getQueueAssociations(name)) {
	      log.debug("[loadQueueDetails] Associating queue (" + name + 
	      ") with node ("
	      + node.getName());
	      Queues.associate(name, node.getName());	
	      }				
	      }
			 */  
			//Adds all the associations to the db
			GridEngineUtil.setQueueAssociationsInDb();
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
				Queues.updateUsage(q);
			}	
			buff.close();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	/**
	 * Extracts queue-node association from SGE and puts it into the db.
	 * @return true if successful
	 * @author Benton McCune
	 */
	public static Boolean setQueueAssociationsInDb() {

		// Call SGE to get info on the queues
		//String results = Util.bufferToString(Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name));

		String[] envp = new String[2];
		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
		envp[1] = "SGE_ROOT="+R.SGE_ROOT; // it seems we need to set this explicitly if we change the environment.
		BufferedReader reader = Util.executeCommand(R.QUEUE_STATS_COMMAND,envp);
		String results = Util.bufferToString(reader);
		//String results = testString;
		log.info("Updating the DB with associations between SGE queues to compute nodes.");

		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("set Queue Associations says " + e.getMessage(), e);
		}

		// Parse the output from the SGE call to get the child worker nodes
		java.util.regex.Matcher matcher = queueAssocPattern.matcher(results);

		String[] capture;  // string array to store a queue and its associated node
		//Remove all association info from db so stale data isn't displayed
		Queues.clearQueueAssociations();
		// For each match...
		while(matcher.find()) {
			// Parse out the queue and node names from the regex parser and add it to the return list			
			capture = matcher.group().split("@");
			log.debug("queue = " + capture[0]);
			log.debug("node = " + capture[1]);
			Queues.associate(capture[0], capture[1]);
		}

		return true;
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
		//String results = Util.bufferToString(Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name));
		BufferedReader reader = Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name);
		String results = Util.bufferToString(reader);
		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("get Queue Details says " + e.getMessage(), e);
		}
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
	public static synchronized void loadWorkerNodes() {
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
		BufferedReader reader = Util.executeCommand(R.NODE_DETAILS_COMMAND + name);
		String results = Util.bufferToString(reader);
		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("get Node Details says " + e.getMessage(), e);
		}

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


    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param job The job the pair is apart of
     * @param pair The pair to get output for
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(JobPair pair, int limit) {
    	pair = JobPairs.getPairDetailed(pair.getId());
    	return GridEngineUtil.getStdOut(pair.getId(),limit);
    }

    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param jobId The id of the job the pair is apart of
     * @param pairId The pair to get output for
     * @param limit The maximum number of lines to return
     * @param path The path to the job pair file
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(int pairId,int limit) {		
    	File stdoutFile = GridEngineUtil.getStdOutFile(pairId);		
    	return Util.readFileLimited(stdoutFile, limit);
    }

    /**
     * Finds the standard output of a job pair and returns its file.
     * @param jobId The id of the job the pair is apart of
     * @param pairId The pair to get output for
     * @param path The space path to the job pair file
     * @return All console output from a job pair run for the given pair
     */
    public static File getStdOutFile(int pairId) {	
    	String stdoutPath=JobPairs.getFilePath(pairId);
    	log.info("The stdoutPath is: " + stdoutPath);

    	return (new File(stdoutPath));	
    }



    
    /**
     * Cancels/Ends a reservation
     */
    public static void checkQueueReservations() {
    	java.util.Date today = new java.util.Date();
		//java.util.Date today = new java.util.Date(113, 11, 10); // December 10, 2013
		List<QueueRequest> queueReservations = Requests.getAllQueueReservations();
		if (queueReservations != null) {
			for (QueueRequest req : queueReservations) {
				SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
				
		        Calendar cal = Calendar.getInstance();
		        cal.setTime(today);
		        cal.add(Calendar.DATE, -1);
		        java.util.Date yesterday = cal.getTime();
				
				/**
				 * If the reservation end_date was 'yesterday' -- makes end_date inclusive
				 */
				boolean end_was_yesterday = fmt.format(req.getEndDate()).equals(fmt.format(yesterday));
				if (end_was_yesterday) {
					cancelReservation(req);
				}
				
				
				/**
				 * if today is when the reservation is starting
				 */
				boolean start_is_today = (fmt.format(req.getStartDate())).equals(fmt.format(today));
				if (start_is_today) {
					startReservation(req);
				}
				
				int queueId = Queues.getIdByName(req.getQueueName());
				int nodeCount = Cluster.getReservedNodeCountOnDate(queueId, today);
				List<WorkerNode> actualNodes = Cluster.getNodesForQueue(queueId);
				int actualNodeCount = actualNodes.size();
				String queueName = Queues.getNameById(queueId);
				String[] split = queueName.split("\\.");
				String shortQueueName = split[0];


				/**The Following code is for when the node count is changing throughout the reservation**/
				//When the node count is decreasing for this reservation on this day
				//Need to move a certain number of nodes back to all.q
				transferOverflowNodes(req, shortQueueName, nodeCount, actualNodeCount, actualNodes);
				
				//When the node count is increasing for this reservation on this day
				//Need to move a certain number of nodes from all.q to this queue
				transferUnderflowNodes(req, shortQueueName, nodeCount, actualNodeCount);
				
			}
		}
    }
		
	private static void transferOverflowNodes(QueueRequest req, String queueName, int nodeCount, int actualNodeCount, List<WorkerNode> actualNodes) {
		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+R.SGE_ROOT;
		
		if (actualNodeCount > nodeCount) {
			List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();
			
			for (int i = 0; i < (actualNodeCount - nodeCount); i++) {
				WorkerNode n = actualNodes.get(i);
				transferNodes.add(n);
			}	
			
			for (WorkerNode n : transferNodes) {
				//add it to @allhosts
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
				
				//remove it from @<queueName>hosts
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @" + queueName + "hosts", envp);
			}
		}		
	}
	
	private static void transferUnderflowNodes(QueueRequest req, String queueName, int nodeCount, int actualNodeCount) {
		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+R.SGE_ROOT;
		List<WorkerNode> AllQueueNodes = Queues.getNodes(1);

		if (actualNodeCount < nodeCount) {
			
			List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();
			for (int i = 0; i < (nodeCount - actualNodeCount); i++) {
				transferNodes.add(AllQueueNodes.get(i));
			}	
			
			for (WorkerNode n : transferNodes) {
				//add it to @<queueName>hosts
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @" + queueName + "hosts", envp);
				
				//Remove it from allhosts
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
			}
		}		
	}

	public static void cancelReservation(QueueRequest req) {
		log.debug("Begin cancelReservation");
		String queueName = req.getQueueName();
		String[] split = queueName.split("\\.");
		String shortQueueName = split[0];
		int queueId = Queues.getIdByName(queueName);
		
		//Pause jobs that are running on the queue
		List<Job> jobs = Cluster.getJobsRunningOnQueue(queueId);

		if (jobs != null) {
			for (Job j : jobs) {
				Jobs.pause(j.getId());
			}
		}
		

		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+R.SGE_ROOT;
		//Move associated Nodes back to default queue
		List<WorkerNode> nodes = Queues.getNodes(queueId);
		
		if (nodes != null) {
			for (WorkerNode n : nodes) {
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
			}
		}
		
		
		/***** DELETE THE QUEUE *****/		
			//Database modification:
			Requests.DeleteReservation(req);

			//DISABLE the queue: 
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qmod -d " + req.getQueueName(), envp);
			//DELETE the queue:
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dq " + req.getQueueName(), envp);
			
			//Delete the host group:
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dhgrp @"+ shortQueueName +"hosts", envp);
			
		    GridEngineUtil.loadWorkerNodes();
		    GridEngineUtil.loadQueues();
			
	}
	
	public static void startReservation (QueueRequest req) {
		log.debug("begin startReservation");
		String queueName = req.getQueueName();
		String[] split = queueName.split("\\.");
		String shortQueueName = split[0];
		int queueId = Queues.getIdByName(queueName);
		Queue q = Queues.get(queueId);
		if (!q.getStatus().equals("ACTIVE")) {
			
			//Get the nodes we are going to transfer
			List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();	
			List<WorkerNode> nodes = Queues.getNodes(1);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < req.getNodeCount(); i++) {
				transferNodes.add(nodes.get(i));
				String fullName = nodes.get(i).getName();
				String[] split2 = fullName.split("\\.");
				String shortName = split2[0];
				sb.append(shortName);
				sb.append(" ");
			}
			String hostList = sb.toString();
			
			/***** CREATE A QUEUE *****/
			// Create newHost.hgrp [COMPLETE]
			String newHost;
			try {
				newHost = "group_name @" + shortQueueName + "hosts" +
						  "\nhostlist " + hostList;
				File f = new File("/tmp/newHost30.hgrp");
				FileUtils.writeStringToFile(f, newHost);
				f.setReadable(true, false);
				f.setWritable(true, false);

			} catch (IOException e) {
				e.printStackTrace();
			}
			

			//Add the host [COMPLETE]
			String[] envp = new String[1];
			envp[0] = "SGE_ROOT="+R.SGE_ROOT;
			log.debug("envp[0] = " + envp[0]);
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -Ahgrp /tmp/newHost30.hgrp", envp);
			
			
			
			// Create newQueue.q [COMPLETE]
			String newQueue;
			try {
				newQueue = "qname                   " + queueName + 
							"\nhostlist             @" + shortQueueName + "hosts" + 
							"\nseq_no                0" +
							"\nload_thresholds       np_load_avg=1.75" +
							"\nsuspend_thresholds    NONE" +
							"\nnsuspend              1" +
							"\nsuspend_interval      00:05:00" +
							"\npriority              0" +
							"\nmin_cpu_interval      00:05:00" +
							"\nprocessors            UNDEFINED" +
							"\nqtype                 BATCH INTERACTIVE" +
							"\nckpt_list             NONE" +
							"\npe_list               make" +
							"\nrerun                 FALSE" +
							"\nslots                 1" +
							"\ntmpdir                /tmp" +
							"\nshell                 /bin/csh" +
							"\nprolog                NONE" +
							"\nepilog                NONE" +
							"\nshell_start_mode      posix_compliant" +
							"\nstarter_method        NONE" +
							"\nsuspend_method        NONE" +
							"\nresume_method         NONE" +
							"\nterminate_method      NONE" +
							"\nnotify                00:00:60"+
							"\nowner_list            NONE"+
							"\nuser_lists            NONE"+
							"\nxuser_lists           NONE"+
							"\nsubordinate_list      NONE"+
							"\ncomplex_values        NONE"+
							"\nprojects              NONE"+
							"\nxprojects             NONE"+
							"\ncalendar              NONE"+
							"\ninitial_state         default"+
							"\ns_rt                  INFINITY"+
							"\nh_rt                  INFINITY"+
							"\ns_cpu                 INFINITY"+
							"\nh_cpu                 INFINITY"+
							"\ns_fsize               INFINITY"+
							"\nh_fsize               INFINITY"+
							"\ns_data                INFINITY"+
							"\nh_data                INFINITY"+
							"\ns_stack               INFINITY"+
							"\nh_stack               INFINITY"+
							"\ns_core                INFINITY"+
							"\nh_core                INFINITY"+
							"\ns_rss                 INFINITY"+
							"\nh_rss                 INFINITY"+
							"\ns_vmem                INFINITY"+
							"\nh_vmem                INFINITY";
				
				File f = new File("/tmp/newQueue30.q");
				FileUtils.writeStringToFile(f, newQueue);
				f.setReadable(true, false);
				f.setWritable(true, false);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -Aq tmp/newQueue30.q", envp);
					
			//Transfer nodes out of @allhosts
			for (WorkerNode n : transferNodes) {
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
			}
		    GridEngineUtil.loadWorkerNodes();
		    GridEngineUtil.loadQueues();
		}
	}
	
	
	public static boolean createPermanentQueue(QueueRequest req, boolean isNewQueue, HashMap<WorkerNode, Queue> nodesAndQueues) {
		log.debug("begin createPermanentQueue");
		String queueName = req.getQueueName();
		String[] split = queueName.split("\\.");
		String shortQueueName = split[0];
		//int queueId = Queues.getIdByName(queueName);
		//Queue q = Queues.get(queueId);
		List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();	
		StringBuilder sb = new StringBuilder();
		
		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+R.SGE_ROOT;

		
		if (isNewQueue) {
			//This is being called from "Create new permanent queue"
			Set<WorkerNode> nodes = nodesAndQueues.keySet();
			if (nodes != null) {
				for (WorkerNode n : nodes) {
					transferNodes.add(n);
					String fullName = n.getName();
					String[] split2 = fullName.split("\\.");
					String shortName = split2[0];
					sb.append(shortName);
					sb.append(" ");
					
					Queue queue = nodesAndQueues.get(n);
					String name = queue.getName();
					String[] split3 = name.split("\\.");
					String shortQName = split3[0];
					Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @" + shortQName + "hosts", envp);
				}
			}
			
		} else {
			//This is being called from "Make existing queue permanent"
			
			//Get the nodes we are going to transfer
			List<WorkerNode> nodes = Queues.getNodes(1);
			for (int i = 0; i < req.getNodeCount(); i++) {
				transferNodes.add(nodes.get(i));
				String fullName = nodes.get(i).getName();
				String[] split2 = fullName.split("\\.");
				String shortName = split2[0];
				sb.append(shortName);
				sb.append(" ");
				
				// Transfer nodes out of @allhosts
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + nodes.get(i).getName() + " @allhosts", envp);
			}
		}
		
		String hostList = sb.toString();

		/***** CREATE A QUEUE *****/
		// Create newHost.hgrp
		String newHost;
		try {
			newHost = "group_name @" + shortQueueName + "hosts" +
					  "\nhostlist " + hostList;
			File f = new File("/tmp/newHost30.hgrp");
			FileUtils.writeStringToFile(f, newHost);
			f.setReadable(true, false);
			f.setWritable(true, false);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
			

		//Add the host

		Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -Ahgrp /tmp/newHost30.hgrp", envp);
		
			
			
		// Create newQueue.q [COMPLETE]
		String newQueue;
		try {
			newQueue = "qname                   " + queueName + 
						"\nhostlist             @" + shortQueueName + "hosts" + 
						"\nseq_no                0" +
						"\nload_thresholds       np_load_avg=1.75" +
						"\nsuspend_thresholds    NONE" +
						"\nnsuspend              1" +
						"\nsuspend_interval      00:05:00" +
						"\npriority              0" +
						"\nmin_cpu_interval      00:05:00" +
						"\nprocessors            UNDEFINED" +
						"\nqtype                 BATCH INTERACTIVE" +
						"\nckpt_list             NONE" +
						"\npe_list               make" +
						"\nrerun                 FALSE" +
						"\nslots                 1" +
						"\ntmpdir                /tmp" +
						"\nshell                 /bin/csh" +
						"\nprolog                NONE" +
						"\nepilog                NONE" +
						"\nshell_start_mode      posix_compliant" +
						"\nstarter_method        NONE" +
						"\nsuspend_method        NONE" +
						"\nresume_method         NONE" +
						"\nterminate_method      NONE" +
						"\nnotify                00:00:60"+
						"\nowner_list            NONE"+
						"\nuser_lists            NONE"+
						"\nxuser_lists           NONE"+
						"\nsubordinate_list      NONE"+
						"\ncomplex_values        NONE"+
						"\nprojects              NONE"+
						"\nxprojects             NONE"+
						"\ncalendar              NONE"+
						"\ninitial_state         default"+
						"\ns_rt                  INFINITY"+
						"\nh_rt                  INFINITY"+
						"\ns_cpu                 INFINITY"+
						"\nh_cpu                 INFINITY"+
						"\ns_fsize               INFINITY"+
						"\nh_fsize               INFINITY"+
						"\ns_data                INFINITY"+
						"\nh_data                INFINITY"+
						"\ns_stack               INFINITY"+
						"\nh_stack               INFINITY"+
						"\ns_core                INFINITY"+
						"\nh_core                INFINITY"+
						"\ns_rss                 INFINITY"+
						"\nh_rss                 INFINITY"+
						"\ns_vmem                INFINITY"+
						"\nh_vmem                INFINITY";
			
			File f = new File("/tmp/newQueue30.q");
			FileUtils.writeStringToFile(f, newQueue);
			f.setReadable(true, false);
			f.setWritable(true, false);
				
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -Aq tmp/newQueue30.q", envp);
					
		

	    GridEngineUtil.loadWorkerNodes();
	    GridEngineUtil.loadQueues();
		return true;
	}

	public static void removePermanentQueue(int queueId) {
		String queueName = Queues.getNameById(queueId);
		String[] split = queueName.split("\\.");
		String shortQueueName = split[0];
		
		//Pause jobs that are running on the queue
		List<Job> jobs = Cluster.getJobsRunningOnQueue(queueId);

		if (jobs != null) {
			for (Job j : jobs) {
				Jobs.pause(j.getId());
			}
		}

		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+R.SGE_ROOT;
		//Move associated Nodes back to default queue
		List<WorkerNode> nodes = Queues.getNodes(queueId);
		
		if (nodes != null) {
			for (WorkerNode n : nodes) {
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
			}
		}
		
		
		/***** DELETE THE QUEUE *****/	
			//Database Change
			Queues.delete(queueId);
			
			//DISABLE the queue: 
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qmod -d " + queueName, envp);
			//DELETE the queue:
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dq " + queueName, envp);
			
			//Delete the host group:
			Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dhgrp @"+ shortQueueName +"hosts", envp);
			
		    GridEngineUtil.loadWorkerNodes();
		    GridEngineUtil.loadQueues();		
	}

	public static void moveNodes(QueueRequest req, HashMap<WorkerNode, Queue> NQ) {
		String queueName = req.getQueueName();
		String[] split = queueName.split("\\.");
		String shortQueueName = split[0];
		List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();	
		StringBuilder sb = new StringBuilder();
		
		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+R.SGE_ROOT;

		Set<WorkerNode> nodes = NQ.keySet();
		if (nodes != null) {
			for (WorkerNode n : nodes) {
				transferNodes.add(n);
				String fullName = n.getName();
				String[] split2 = fullName.split("\\.");
				String shortName = split2[0];
				sb.append(shortName);
				sb.append(" ");
				
				//remove the association with this node and the queue it is currently associated with and add it to the permanent queue
				Queue queue = NQ.get(n);
				
				//if this is going to make the queue empty...... need to pause all jobs first
				if (Cluster.getNodesForQueue(queue.getId()).size() == 1 ) {
					List<Job> jobs = Cluster.getJobsRunningOnQueue(queue.getId());
					if (jobs != null) {
						for (Job j : jobs) {
							Jobs.pause(j.getId());
						}
					}
				}
				
				String name = queue.getName();
				String[] split3 = name.split("\\.");
				String shortQName = split3[0];
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @" + shortQName + "hosts", envp);
				Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @" + shortQueueName + "hosts", envp);
			}
		}
	    GridEngineUtil.loadWorkerNodes();
	    GridEngineUtil.loadQueues();
	}
}