package org.starexec.backend;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.starexec.util.Util;

/**
 * This is a backend implementation that uses Sun Grid Engine
 *
 */

public class GridEngineBackend implements Backend{
    // SGE Configurations, see GridEngineBackend
    private static String QUEUE_LIST_COMMAND = "qconf -sql";					// The SGE command to execute to get a list of all job queues
    private static String QUEUE_DETAILS_COMMAND = "qconf -sq ";				// The SGE command to get configuration details about a queue
    private static String QUEUE_STATS_COMMAND = "qstat -f";				// The SGE command to get stats about all the queues
    private static String NODE_LIST_COMMAND = "qconf -sel";					// The SGE command to execute to get a list of all worker nodes
    private static String NODE_DETAILS_COMMAND = "qconf -se ";				// The SGE command to get hardware details about a node	
    private static String NODE_DETAIL_PATTERN = "[^\\s,][\\w|-]+=[^,\\s]+";  // The regular expression to parse out the key/value pairs from SGE's node detail output
    private static String QUEUE_DETAIL_PATTERN = "[\\w|-]+\\s+[^\t\r\n,]+";  // The regular expression to parse out the key/value pairs from SGE's queue detail output
    private static String QUEUE_ASSOC_PATTERN = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}\\b";  // The regular expression to parse out the nodes that belong to a queue from SGE's qstat -f


    private static String GRID_ENGINE_PATH = "/cluster/gridengine-8.1.8/bin/lx-amd64/";
	
	
    private Session session = null;
    private Logger log;
    private String BACKEND_ROOT = null;
    
    // The regex patterns used to parse SGE output
 	private static Pattern nodeKeyValPattern;
 	private static Pattern queueKeyValPattern;
 	private static Pattern queueAssocPattern;

 	static {
 		// Compile the SGE output parsing patterns when this class is loaded
 		nodeKeyValPattern = Pattern.compile(NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
 		queueKeyValPattern = Pattern.compile(QUEUE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
 		queueAssocPattern = Pattern.compile(QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);

 	}
    
    /**
     * This constructor only initializes logging-- initialze must be called
     * after construction.
     */
    public GridEngineBackend(){
    	log = Logger.getLogger(GridEngineBackend.class);
    }

    /**
     * use to initialize fields and prepare backend for tasks

     **/
    public void initialize(String BACKEND_ROOT){
	    this.BACKEND_ROOT = BACKEND_ROOT;

    	try {
			log.debug("createSession() loading class."); 	
			// Try to load the class, if it does not exist this will cause an exception instead of an error			
			Class.forName("com.sun.grid.drmaa.SessionImpl");
			Session s = SessionFactory.getFactory().getSession();
			s.init("");
		    log.info("Created GridEngine session");
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
    }

    /**
     * release resources that Backend might not need anymore
     * there's a chance that initialize is never called, so always try dealing with that case

     **/
    public void destroyIf(){
		if (!session.toString().contains("drmaa")) {
		    log.debug("Shutting down the session..." + session);
		    try {
				session.exit();
			}
			catch (Exception e) {
				log.error("Problem destroying session: "+e,e);
			}
		}
    }





    /**

     * @param execCode : an execution code (returned by submitScript)
     * @return false if the execution code represents an error, true otherwise
     *
     **/
    public boolean isError(int execCode){
		if(execCode >= 0) {						       	
		    
		    return false;
		}
		else {
		    
		    return true;
		}
    }

    /**

     * @param scriptPath : the full path to the jobscript file
     * @param workingDirectoryPath  :  path to a directory that can be used for scratch space (read/write)
     * @param logPath  :  path to a directory that should be used to store jobscript logs
     * @return an identifier for the task that submitScript starts, should allow a user to identify which task/script to kill
     **/
    public int submitScript(String scriptPath, String workingDirectoryPath, String logPath){
    	synchronized(this){
	JobTemplate sgeTemplate = null;

		try {
			sgeTemplate = null;		

			// Set up the grid engine template
			sgeTemplate = session.createJobTemplate();


			// DRMAA needs to be told to expect a shell script and not a binary
			sgeTemplate.setNativeSpecification("-shell y -b n -w n");


			// Tell the job where it will deal with files
			sgeTemplate.setWorkingDirectory(workingDirectoryPath);



			sgeTemplate.setOutputPath(":" + logPath);
			

			// Tell the job where the script to be executed is
			sgeTemplate.setRemoteCommand(scriptPath);	        
			

			// Actually submit the job to the grid engine
			String id = session.runJob(sgeTemplate);
			//log.info(String.format("Submitted SGE job #%s, job pair %s, script \"%s\".", id, pair.getId(), scriptPath)); 

			return Integer.parseInt(id);
		} catch (org.ggf.drmaa.DrmaaException drme) {
			log.warn("script Path = " + scriptPath);
			//log.warn("sgeTemplate = " +sgeTemplate.toString());
			//JobPairs.setPairStatus(pair.getId(), StatusCode.ERROR_SGE_REJECT.getVal());			
			log.error("submitScript says " + drme.getMessage(), drme);
			
		} catch (Exception e) {
		    //JobPairs.setPairStatus(pair.getId(), StatusCode.ERROR_SUBMIT_FAIL.getVal());
			log.error(e.getMessage(), e);
			
		} finally {
			// Cleanup. Session's MUST be exited or SGE will be mean to you
			if(sgeTemplate != null) {
			    try{
				session.deleteJobTemplate(sgeTemplate);
			    } catch(Exception e){
				log.error(e.getMessage(),e);
			    }
			}

		}

		return -1;
}
}

    /**

     * @param execId an int that identifies the pair to be killed, should match what is returned by submitScript
     * @return true if successful, false otherwise
     * kills a jobpair
     */
    public boolean killAll(){
		try {
			Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qdel -f -u tomcat",getSGEEnv());
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false;
    }


    /**

     * @param execId an int that identifies the pair to be killed, should match what is returned by submitScript
     * @return true if successful, false otherwise
     * kills a jobpair
     */
    public boolean killPair(int execId){
		try{
		    Util.executeCommand("qdel " + execId);	
		    return true;
		} catch (Exception e) {
		    return false;
		}
    }


    /**

     * @return a string representing the status of jobs running on the system
     */
    public String getRunningJobsStatus() {
    	try {	
			return Util.executeCommand("qstat -f");
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
    }

    /**

     * @return returns a list of names of all active worker nodes
     */
    public String[] getWorkerNodes() {
    	try {
    		// Execute the SGE command to get the node list
    		String nodeResults = Util.executeCommand(NODE_LIST_COMMAND);
    		log.debug("getWorkerNodes got the following results");
    		log.debug(nodeResults);
    		return nodeResults.split(System.getProperty("line.separator"));
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		

    }

    /**

     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given node
     * where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public Map<String,String> getNodeDetails(String nodeName){

    	try {
    		// Call SGE to get details for the given node
    		String results = Util.executeCommand(NODE_DETAILS_COMMAND + nodeName);

    		// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = GridEngineBackend.nodeKeyValPattern.matcher(results);

    		Map<String, String> detailMap = new HashMap<String,String>();
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] keyVal = matcher.group().split("=");
    			
    			// Add the results to the details list
    			detailMap.put(keyVal[0], keyVal[1]);
    		}

    		return detailMap;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		

    }

    /**

     * @return returns a list of all active queues
     */
    public String[] getQueues(){
    	try {
    		// Execute the SGE command to get the list of queues
    		String queuestr = Util.executeCommand(QUEUE_LIST_COMMAND);

    		return queuestr.split(System.getProperty("line.separator"));	
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;

    }

    /**
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given queue
     *  where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public Map<String,String> getQueueDetails(String nodeName){
    	try {
    		// Call SGE to get details for the given node
    		String results = Util.executeCommand(QUEUE_DETAILS_COMMAND + nodeName);

    		// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = GridEngineBackend.queueKeyValPattern.matcher(results);

    		Map<String, String> detailsMap = new HashMap<String, String>();
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] keyVal = matcher.group().split("\\s+");
    			
    			// Add the results to the details list
    			detailsMap.put(keyVal[0], keyVal[1]);
    		}

    		return detailsMap;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		
    }

     /**

     * @return an array that represents queue-node assocations: [queueName1,nodeName1,queueName1,nodeName2,queueName2,nodeName3]
     * the queue and node names should match the names returned when calling getWorkerNodes and getQueues.
     * queue names are found in the even-indexed positions, node name otherwise. 
     *  a queue at index i is associated with the node at index i + 1
     */
    public Map<String,String> getQueueNodeAssociations(){
	
    	try {
    		String[] envp = new String[2];
    		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
    		envp[1] = "SGE_ROOT="+BACKEND_ROOT; // it seems we need to set this explicitly if we change the environment.
    		String results = Util.executeCommand(QUEUE_STATS_COMMAND,envp);

    		// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = GridEngineBackend.queueAssocPattern.matcher(results);
    		
    		Map<String,String> nodesToQueuesMap = new HashMap<String, String>();
    		
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] queueNode = matcher.group().split("@");
    			nodesToQueuesMap.put(queueNode[1], queueNode[0]);
    		}

    		return nodesToQueuesMap;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		

    }

    /**

     * @param allQueueNames the names of all queues, 
     * @return true if sucessful, false otherwise
     * should clear any states caused by errors on both queues and nodes
     */
    public boolean clearNodeErrorStates(){
    	try {
			String[] allQueueNames = this.getQueues();
			for(int i=0;i<allQueueNames.length;i++) {
				Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qmod -cq "+allQueueNames[i],getSGEEnv());
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false;
    }

    
   /**
     * deletes a queue that no longer has nodes associated with it

     * @param queueName the name of the queue to be removed
     * @return true if successful, false otherwise
     */
    public boolean deleteQueue(String queueName){
    	try {
    		String[] split = queueName.split("\\.");
    		String shortQueueName = split[0];

    		//DISABLE the queue: 
    		Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qmod -d " + queueName, getSGEEnv());
    		//DELETE the queue:
    		Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -dq " + queueName, getSGEEnv());
    				
    		//Delete the host group:
    		Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -dhgrp @"+ shortQueueName +"hosts", getSGEEnv());
    		return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
	
    }
    
    
    /**
	 * Gets a String array representing the environment for SGE
	 * @return A size 1 array containing "SGE_ROOT=" plus the root path
	 */
	private String[] getSGEEnv() {
		String[] envp = new String[1];
		envp[0] = "SGE_ROOT="+BACKEND_ROOT;
		return envp;
	}


    /**
     * creates a new queue

     *@param queueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     *@return true if successful, false otherwise
     */
    public boolean createQueue(String queueName, String[] nodeNames, String[] queueNames){
    	try {
			log.debug("begin createQueue");
			String[] split = queueName.split("\\.");
			String shortQueueName = split[0];
	
			StringBuilder sb = new StringBuilder();
			
			//This is being called from "Create new permanent queue"
			if (nodeNames != null) {
			    for (int i=0;i<nodeNames.length;i++) {
					String fullName = nodeNames[i];
					String[] split2 = fullName.split("\\.");
					String shortName = split2[0];
					sb.append(shortName);
					sb.append(" ");
					

					String sourceQueueName = queueNames[i];
					//if node is not orphaned
					if(sourceQueueName != null){
					    String[] split3 = sourceQueueName.split("\\.");
					    String shortQName = split3[0];
					    log.debug("About to execute sudo command 1");
					    Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -dattr hostgroup hostlist " + fullName + " @" + shortQName + "hosts", getSGEEnv());
					}
				}
			}
			
			
			String hostList = sb.toString();
	
			/***** CREATE A QUEUE *****/
			// Create newHost.hgrp
			String newHost;
		
			newHost = "group_name @" + shortQueueName + "hosts" +
					  "\nhostlist " + hostList;
			File f = new File("/tmp/newHost30.hgrp");
			FileUtils.writeStringToFile(f, newHost);
			f.setReadable(true, false);
			f.setWritable(true, false);

			//Add the host

			Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -Ahgrp /tmp/newHost30.hgrp", getSGEEnv());			
			
			// Create newQueue.q [COMPLETE]
			String newQueue;
		
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
						"\nslots                 2" +
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
			
			File f2 = new File("/tmp/newQueue30.q");
			FileUtils.writeStringToFile(f2, newQueue);
			f2.setReadable(true, false);
			f2.setWritable(true, false);
				
			Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -Aq /tmp/newQueue30.q", getSGEEnv());

		    log.debug("created queue successfully");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return false;
    }


    /**

     *@param destQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     * moves nodes from source queues to the destination queue <queueName>
     * the ith element of nodeNames corresponds to the ith element of sourceQueueNames for every i
     * if node is an orphaned node, the corresponding queue name in sourceQueueNames will be null
     */
    public boolean moveNodes(String queueName,String[] nodeNames,String[] sourceQueueNames){
    	try {
    		log.info("moveNodes begins, for queue "+queueName);
    		String[] split = queueName.split("\\.");
    		String shortQueueName = split[0];
    		StringBuilder sb = new StringBuilder();


    		if ((nodeNames == null) || (nodeNames.length == 0)) {
    			log.warn("No nodes to move");
    		} else {
    		    for(int i=0;i<nodeNames.length;i++){
	    			//String fullName = n.getName();
	    		    String nodeFullName = nodeNames[i];
	    			String[] split2 = nodeFullName.split("\\.");
	    			String shortName = split2[0];
	    			sb.append(shortName);
	    			sb.append(" ");
	    			log.debug("moving node "+nodeFullName);
					//remove the association with this node and the queue it is currently associated with and add it to the queue
					if (sourceQueueNames[i] != null) {
					    // orphaned nodes could have null queues
					    
					    String name = sourceQueueNames[i];
					    String[] split3 = name.split("\\.");
					    String shortQName = split3[0];
					    Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -dattr hostgroup hostlist " + nodeFullName + " @" + shortQName + "hosts", getSGEEnv());
					}
					log.debug("adding node with name = "+nodeFullName +" to queue = "+shortQueueName);
					Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -aattr hostgroup hostlist " + nodeFullName + " @" + shortQueueName + "hosts", getSGEEnv());
    		    }
    		}

	    	log.debug("Move nodes ending.");
	    	return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
	

    }

    /**
     * moves the given node to the given queue

     * @param nodeName the name of a node
     * @param queueName the name of a queue
     * @return true if successful, false otherwise
     */
    public boolean moveNode(String nodeName, String queueName){
    	try {
    		Util.executeCommand("sudo -u sgeadmin "+GRID_ENGINE_PATH+"qconf -dattr hostgroup hostlist " + nodeName + " @" + queueName + "hosts", getSGEEnv());
    	    return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
    }

}