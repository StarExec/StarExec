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
	}

	/**
	 * Loads the list of active queues on the system, loads their attributes into the database
	 * as well as their associations to worker nodes that belong to each queue.
	 */
	private static void loadQueueDetails() {
		log.info("Loading queue details into the db");
		try {			
			// Execute the SGE command to get the list of queues
			String queuestr = Util.executeCommand(R.QUEUE_LIST_COMMAND);

			// Set all queues as inactive (we will set them as active when we see them)
			Queues.setStatus(R.QUEUE_STATUS_INACTIVE);

			// Read the queue names one at a time
			String[] lines = queuestr.split(System.getProperty("line.separator"));		
			for (int i = 0; i < lines.length; i++) {
			    String name = lines[i];

			    log.debug("Loading details for queue "+name);

			    // In the database, update the attributes for the queue
			    Queues.update(name,  GridEngineUtil.getQueueDetails(name));

			    // Set the queue as active since we just saw it
			    Queues.setStatus(name, R.QUEUE_STATUS_ACTIVE);
			}
			//Adds all the associations to the db
			GridEngineUtil.setQueueAssociationsInDb();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} 
		log.info("Completed loading the queue details into the db");
	}

	/**
	 * Extracts queue-node association from SGE and puts it into the db.
	 * @return true if successful
	 * @author Benton McCune
	 */
	public static Boolean setQueueAssociationsInDb() {

		log.info("Updating the DB with associations between SGE queues to compute nodes.");

		String[] envp = new String[2];
		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
		envp[1] = "SGE_ROOT="+R.SGE_ROOT; // it seems we need to set this explicitly if we change the environment.
		String results = Util.executeCommand(R.QUEUE_STATS_COMMAND,envp);

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

		log.info("Completed updating the DB with associations between SGE queues to compute nodes.");
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
		String results = Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name);

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

		log.info("Loading worker nodes into the db");
		try {			
			// Execute the SGE command to get the node list
			String nodeResults = Util.executeCommand(R.NODE_LIST_COMMAND);

			// Set all nodes as inactive (we will update them to active as we see them)
			Cluster.setNodeStatus(R.NODE_STATUS_INACTIVE);

			// Read the nodes one at a time
			String[] lines = nodeResults.split(System.getProperty("line.separator"));		
			for (int i = 0; i < lines.length; i++) {
				String name = lines[i];
				log.debug("Updating info for node "+name);
				// In the database, update the attributes for the node
				Cluster.updateNode(name,  GridEngineUtil.getNodeDetails(name));				
				// Set the node as active (because we just saw it!)
				Cluster.setNodeStatus(name, R.NODE_STATUS_ACTIVE);
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		log.info("Completed loading info for worker nodes into db");
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
		String results = Util.executeCommand(R.NODE_DETAILS_COMMAND + name);

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
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");

    	java.util.Date today = new java.util.Date();
		List<QueueRequest> queueReservations = Requests.getAllQueueReservations();
		if (queueReservations == null) 
		    log.info("No reservations found.");
		else {
			//first, end all the reservations that need to be ended and take back nodes that reservations are done with
			for (QueueRequest req : queueReservations) {

		        Calendar cal = Calendar.getInstance();
		        cal.setTime(today);
		        cal.add(Calendar.DATE, -1);
		        java.util.Date yesterday = cal.getTime();
				/**
				 * If the reservation end_date was 'yesterday' -- makes end_date inclusive
				 */
				boolean end_was_yesterday = fmt.format(req.getEndDate()).equals(fmt.format(yesterday));
				if (end_was_yesterday) {
				    log.info("Reservation has ended for queue "+req.getQueueName()+".");
				    cancelReservation(req);
				}
				
				int queueId = Queues.getIdByName(req.getQueueName());
				int nodeCount = Cluster.getReservedNodeCountOnDate(queueId, today);
				List<WorkerNode> actualNodes = Cluster.getNodesForQueue(queueId);
				int actualNodeCount = actualNodes.size();
				String queueName = Queues.getNameById(queueId);
				String[] split = queueName.split("\\.");
				String shortQueueName = split[0];
				
				//When the node count is decreasing for this reservation on this day
				//Need to move a certain number of nodes back to all.q
				transferOverflowNodes(req, shortQueueName, nodeCount, actualNodeCount, actualNodes);
				
			}
			
			
			
			//next, start up the new reservations and add nodes where they need to be
		    for (QueueRequest req : queueReservations) {
			log.info("Checking reservation for queue "+req.getQueueName());

			/**
			 * if today is when the reservation is starting
			 */
			boolean start_is_today = (fmt.format(req.getStartDate())).equals(fmt.format(today));
			if (start_is_today) {
			    log.info("Reservation begins today for queue "+req.getQueueName()+".");
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

				
			//When the node count is increasing for this reservation on this day
			//Need to move a certain number of nodes from all.q to this queue
			transferUnderflowNodes(req, shortQueueName, nodeCount, actualNodeCount);
				
		    }
		    GridEngineUtil.loadWorkerNodes();
		    GridEngineUtil.loadQueues();

		}
		log.info("Completed checking queue reservation.");
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
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
				
				//remove it from @<queueName>hosts
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @" + queueName + "hosts", envp);
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
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @" + queueName + "hosts", envp);
				
				//Remove it from allhosts
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
			}
		}		
	}

    public static void cancelReservation(QueueRequest req) {
	String queueName = req.getQueueName();
	log.debug("Begin cancelReservation for queue " + queueName);

	String[] split = queueName.split("\\.");
	String shortQueueName = split[0];
	int queueId = Queues.getIdByName(queueName);
	    
	//Pause jobs that are running on the queue
	List<Job> jobs = Cluster.getJobsRunningOnQueue(queueId);

	log.debug("Pausing jobs on queue "+queueName);

	if (jobs != null) {
	    for (Job j : jobs) {
		log.debug("Pausing job " + new Integer(j.getId()) + " on queue " + queueName);
		Jobs.pause(j.getId());
	    }
	}
		

	String[] envp = new String[1];
	envp[0] = "SGE_ROOT="+R.SGE_ROOT;
	//Move associated Nodes back to default queue
	List<WorkerNode> nodes = Queues.getNodes(queueId);
		
	log.debug("Moving nodes back to @allhosts for queue "+queueName);

	if (nodes != null) {
	    for (WorkerNode n : nodes) {
		Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
	    }
	}
		
	log.debug("Now deleting queue "+queueName+" from the db and grid engine");
		
	/***** DELETE THE QUEUE *****/		
	//Database modification:
	Requests.DeleteReservation(req);

	//DISABLE the queue: 
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qmod -d " + req.getQueueName(), envp);

	//DELETE the queue:
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dq " + req.getQueueName(), envp);
			
	//Delete the host group:
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dhgrp @"+ shortQueueName +"hosts", envp);
			
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
			Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -Ahgrp /tmp/newHost30.hgrp", envp);
			
			
			
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
			Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -Aq /tmp/newQueue30.q", envp);
					
			//Transfer nodes out of @allhosts
			for (WorkerNode n : transferNodes) {
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
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
					Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @" + shortQName + "hosts", envp);
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
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + nodes.get(i).getName() + " @allhosts", envp);
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

		Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -Ahgrp /tmp/newHost30.hgrp", envp);
		
			
			
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

		Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -Aq /tmp/newQueue30.q", envp);
	
		

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
				Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @allhosts", envp);
			}
		}
		
		
		/***** DELETE THE QUEUE *****/	
			//Database Change
			Queues.delete(queueId);
			
			//DISABLE the queue: 
			Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qmod -d " + queueName, envp);
			//DELETE the queue:
			Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dq " + queueName, envp);
			
			//Delete the host group:
			Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dhgrp @"+ shortQueueName +"hosts", envp);
			
		    GridEngineUtil.loadWorkerNodes();
		    GridEngineUtil.loadQueues();		
	}

    public static void moveNodes(QueueRequest req, HashMap<WorkerNode, Queue> NQ) {
	String queueName = req.getQueueName();
	log.info("moveNodes begins, for queue "+queueName);
	String[] split = queueName.split("\\.");
	String shortQueueName = split[0];
	List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();	
	StringBuilder sb = new StringBuilder();
		
	String[] envp = new String[1];
	envp[0] = "SGE_ROOT="+R.SGE_ROOT;

	Set<WorkerNode> nodes = NQ.keySet();
	if (nodes == null)
		log.warn("No nodes to move");
	else {
	    for (WorkerNode n : nodes) {
		transferNodes.add(n);
		String fullName = n.getName();
		String[] split2 = fullName.split("\\.");
		String shortName = split2[0];
		sb.append(shortName);
		sb.append(" ");
				
		log.debug("moving node "+fullName);

		//remove the association with this node and the queue it is currently associated with and add it to the permanent queue
		Queue queue = NQ.get(n);
		
		if (queue != null) {
		    // orphaned nodes could have null queues
				
		    //if this is going to make the queue empty...... need to pause all jobs first
		    List<WorkerNode> workers = Cluster.getNodesForQueue(queue.getId());
		    if (workers != null) {
			if (workers.size() == 1 ) {
			    log.info("checking for jobs running on queue "+queueName+", since this is the last node in the queue.");
			    List<Job> jobs = Cluster.getJobsRunningOnQueue(queue.getId());
			    if (jobs != null) {
				for (Job j : jobs) {
				    Jobs.pause(j.getId());
				}
			    }
			}
		    }
		    
		    String name = queue.getName();
		    String[] split3 = name.split("\\.");
		    String shortQName = split3[0];
		    Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + n.getName() + " @" + shortQName + "hosts", envp);
		}

		Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -aattr hostgroup hostlist " + n.getName() + " @" + shortQueueName + "hosts", envp);
	    }
	}
	GridEngineUtil.loadWorkerNodes();
	GridEngineUtil.loadQueues();
	log.debug("Move nodes ending.");
    }
}