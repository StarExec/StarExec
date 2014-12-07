package org.starexec.backend;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;

import org.starexec.backend.GridEngineR;
import org.starexec.backend.GridEngineUtil;
import org.starexec.backend.BackendUtil;



public class GridEngineBackend implements Backend{
    private Session session = null;
    private Logger log;

    public GridEngineBackend(){
	log = Logger.getLogger(GridEngineBackend.class);
    }

    /**
     * use to initialize fields and prepare backend for tasks
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     **/
    public void initialize(String BACKEND_ROOT){

	    session = GridEngineUtil.createSession();
	    log.info("Created GridEngine session");


    }

    /**
     * release resources that Backend might not need anymore
     * there's a chance that initialize is never called, so always try dealing with that case
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     **/
    public void destroyIf(String BACKEND_ROOT){
	if (!session.toString().contains("drmaa")) {
	    log.debug("Shutting down the session..." + session);
	    GridEngineUtil.destroySession(session);
	}
    }





    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param execCode : an execution code (returned by submitScript)
     * @return false if the execution code represents an error, true otherwise
     *
     **/
    public boolean isError(String BACKEND_ROOT,int execCode){
	if(execCode >= 0) {						       	
	    
	    return false;
	}
	else {
	    
	    return true;
	}
    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param scriptPath : the full path to the jobscript file
     * @param workingDirectoryPath  :  path to a directory that can be used for scratch space (read/write)
     * @param logPath  :  path to a directory that should be used to store jobscript logs
     * @return an identifier for the task that submitScript starts, should allow a user to identify which task/script to kill
     **/
    public int submitScript(String BACKEND_ROOT,String scriptPath, String workingDirectoryPath, String logPath){
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
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param execId an int that identifies the pair to be killed, should match what is returned by submitScript
     * @return true if successful, false otherwise
     * kills a jobpair
     */
    public void killAll(String BACKEND_ROOT){
	GridEngineUtil.deleteAllSGEJobs(BACKEND_ROOT);
    }


    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param execId an int that identifies the pair to be killed, should match what is returned by submitScript
     * @return true if successful, false otherwise
     * kills a jobpair
     */
    public boolean killPair(String BACKEND_ROOT,int execId){
	try{
	    BackendUtil.executeCommand("qdel " + execId);	
	    return true;
	} catch (Exception e) {
	    return false;
	}

    }


    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return a string representing the status of jobs running on the system
     */
    public String getRunningJobsStatus(String BACKEND_ROOT) {
	return GridEngineUtil.getQstatOutput();
		
    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return returns a list of names of all active worker nodes
     */
    public String[] getWorkerNodes(String BACKEND_ROOT) {
    	try {
    		// Execute the SGE command to get the node list
    		String nodeResults = BackendUtil.executeCommand(GridEngineR.NODE_LIST_COMMAND);
    	
    		return nodeResults.split(System.getProperty("line.separator"));
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		

    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given node
     * where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public String[] getNodeDetails(String BACKEND_ROOT,String nodeName){

    	try {
    		// Call SGE to get details for the given node
    		String results = BackendUtil.executeCommand(GridEngineR.NODE_DETAILS_COMMAND + nodeName);

    		// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = GridEngineUtil.nodeKeyValPattern.matcher(results);

    		List<String> detailsList = new LinkedList<String>();
    		
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] keyVal = matcher.group().split("=");
    			
    			// Add the results to the details list
    			detailsList.add(keyVal[0]);
    			detailsList.add(keyVal[1]);


    		}
    		String [] details = detailsList.toArray(new String[detailsList.size()]);

    		return details;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		

    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return returns a list of all active queues
     */
    public String[] getQueues(String BACKEND_ROOT){
    	try {
    		// Execute the SGE command to get the list of queues
    		String queuestr = BackendUtil.executeCommand(GridEngineR.QUEUE_LIST_COMMAND);

    		return queuestr.split(System.getProperty("line.separator"));	
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;

    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return returns the default queue name, default queue should always exist
     */
    public String getDefaultQueueName(String BACKEND_ROOT){
	return "all";
    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given queue
     *  where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public String[] getQueueDetails(String BACKEND_ROOT,String nodeName){
    	try {
    		// Call SGE to get details for the given node
    		String results = BackendUtil.executeCommand(GridEngineR.QUEUE_DETAILS_COMMAND + nodeName);

    		// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = GridEngineUtil.queueKeyValPattern.matcher(results);

    		List<String> detailsList = new LinkedList<String>();
    		
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] keyVal = matcher.group().split("\\s+");
    			
    			// Add the results to the details list
    			detailsList.add(keyVal[0]);
    			detailsList.add(keyVal[1]);


    		}
    		String [] details = detailsList.toArray(new String[detailsList.size()]);

    		return details;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		
    }

     /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return an array that represents queue-node assocations: [queueName1,nodeName1,queueName1,nodeName2,queueName2,nodeName3]
     * the queue and node names should match the names returned when calling getWorkerNodes and getQueues.
     * queue names are found in the even-indexed positions, node name otherwise. 
     *  a queue at index i is associated with the node at index i + 1
     */
    public String[] getQueueNodeAssociations(String BACKEND_ROOT){
	
    	try {
    		String[] envp = new String[2];
    		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
    		envp[1] = "SGE_ROOT="+BACKEND_ROOT; // it seems we need to set this explicitly if we change the environment.
    		String results = BackendUtil.executeCommand(GridEngineR.QUEUE_STATS_COMMAND,envp);

    		// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = GridEngineUtil.queueAssocPattern.matcher(results);

    		List<String> detailsList = new LinkedList<String>();
    		
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] keyVal = matcher.group().split("@");
    			
    			// Add the results to the details list
    			detailsList.add(keyVal[0]);
    			detailsList.add(keyVal[1]);


    		}
    		String [] details = detailsList.toArray(new String[detailsList.size()]);

    		return details;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return null;
		

    }

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param allQueueNames the names of all queues, 
     * @return true if sucessful, false otherwise
     * should clear any states caused by errors on both queues and nodes
     */
    public boolean clearNodeErrorStates(String BACKEND_ROOT, String[] allQueueNames){
	return GridEngineUtil.clearNodeErrorStates(BACKEND_ROOT,allQueueNames);
    }

    
   /**
     * deletes a queue that no longer has nodes associated with it
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param queueName the name of the queue to be removed
     * @return true if successful, false otherwise
     */
    public boolean deleteQueue(String BACKEND_ROOT,String queueName){
    	try {
    		String[] split = queueName.split("\\.");
    		String shortQueueName = split[0];

    		String[] envp = new String[1];
    		envp[0] = "SGE_ROOT="+BACKEND_ROOT;

    		//DISABLE the queue: 
    		BackendUtil.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qmod -d " + queueName, envp);
    		//DELETE the queue:
    		BackendUtil.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dq " + queueName, envp);
    				
    		//Delete the host group:
    		BackendUtil.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dhgrp @"+ shortQueueName +"hosts", envp);
    		return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
	
    }


    /**
     * creates a new queue
     *@param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     *@param isNewQueue true if creating a new queue, false if only switching status to permanent
     *@param destQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     *@return true if successful, false otherwise
     */
    public boolean createPermanentQueue(String BACKEND_ROOT,boolean isNewQueue,String destQueueName, String[] nodeNames, String[] sourceQueueNames){
	return GridEngineUtil.createPermanentQueue(isNewQueue, BACKEND_ROOT,destQueueName,nodeNames,sourceQueueNames);
    }


    /**
     *@param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     *@param destQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     * moves nodes from source queues to the destination queue <queueName>
     * the ith element of nodeNames corresponds to the ith element of sourceQueueNames for every i
     * if node is an orphaned node, the corresponding queue name in sourceQueueNames will be null
     */
    public void moveNodes(String BACKEND_ROOT,String destQueueName,String[] nodeNames,String[] sourceQueueNames){
	
	GridEngineUtil.moveNodes(BACKEND_ROOT,destQueueName,nodeNames,sourceQueueNames);
    }

    /**
     * moves the given node to the given queue
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param nodeName the name of a node
     * @param queueName the name of a queue
     * @return true if successful, false otherwise
     */
    public boolean moveNode(String BACKEND_ROOT,String nodeName, String queueName){
    	try {
    		String[] envp = new String[1];
    		envp[0] = "SGE_ROOT="+ BACKEND_ROOT;
    		BackendUtil.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + nodeName + " @" + queueName + "hosts", envp);
    	    return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
    }

}