package org.starexec.backend;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;

import org.starexec.jobs.JobManager;
import org.starexec.backend.GridEngineUtil;
import org.starexec.util.Util;
import org.starexec.constants.R;
import org.starexec.data.to.*;



public class GridEngineBackend implements Backend{
    private Session session = null;
    private Logger log;

    public GridEngineBackend(){
	log = Logger.getLogger(GridEngineBackend.class);
    }

    /**
     * intializes grid engine
     *
     **/
    public void initialize(){

	    session = GridEngineUtil.createSession();
	    log.info("Created GridEngine session");


    }

    /**
     * shutsdown grid engine session
     **/
    public void destroyIf(){
	if (!session.toString().contains("drmaa")) {
	    log.debug("Shutting down the session..." + session);
	    GridEngineUtil.destroySession(session);
	}
    }

    /**start taken from JobManager**/


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
     * @return the sge id, should allow a user to identify which task/script to kill
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

    /**end taken from JobManager**/




    /**start taken from Jobs**/

   // boolean kill(int jobId, Connection con);

    public void killAll(){
	
    }
    /**end taken from Jobs**/




    /**start taken from JobPairs**/

    /**
     * @param execId the execution code identifying which jobpair task to kill
     * @return true if successfully killed pair, false otherwise
     **/
    public boolean killPair(int execId){
	try{
	    Util.executeCommand("qdel " + execId);	
	    return true;
	} catch (Exception e) {
	    return false;
	}

    }

    /**
     * Returns the result of running qstat -f
     */
	@Override
	public String getRunningJobsStatus() {
		return GridEngineUtil.getQstatOutput();
		
	}

    /**end taken from JobPairs**/




    /**start taken from JobPair**/

    
    //setGridEngineId
    //getGridEngineId
    /**end taken from JobPair**/

    /**
     * @return returns a list of names of all active worker nodes
     */
    public String[] getWorkerNodes(){
	// Execute the SGE command to get the node list
	String nodeResults = Util.executeCommand(R.NODE_LIST_COMMAND);

	return nodeResults.split(System.getProperty("line.separator"));

    }

    /**
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given node
     * where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     * 
     */
    public String[] getNodeDetails(String nodeName){

		// Call SGE to get details for the given node
		String results = Util.executeCommand(R.NODE_DETAILS_COMMAND + nodeName);

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

    }

    /**
     * returns a list of all active queues
     */
    public String[] getQueues(){
	// Execute the SGE command to get the list of queues
	String queuestr = Util.executeCommand(R.QUEUE_LIST_COMMAND);

	return queuestr.split(System.getProperty("line.separator"));	
    }

   /**
     * @return returns the default queue name, should be an active queue
     */
    public String getDefaultQueueName(){
	return "all";
    }

    /**
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given queue
     *  where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public String[] getQueueDetails(String nodeName){
	
		// Call SGE to get details for the given node
		String results = Util.executeCommand(R.QUEUE_DETAILS_COMMAND + nodeName);

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
    }

    /**
     * @return an array that represents queue-node assocations: [queueName1,nodeName1,queueName1,nodeName2,queueName2,nodeName3]
     * the queue and node names should match the names returned when calling getWorkerNodes and getQueues.
     * queue names are found in the even-indexed positions, node name otherwise. 
     *  a queue at index i is associated with the node at index i + 1
     */
    public String[] getQueueNodeAssociations(){
	

		String[] envp = new String[2];
		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
		envp[1] = "SGE_ROOT="+R.SGE_ROOT; // it seems we need to set this explicitly if we change the environment.
		String results = Util.executeCommand(R.QUEUE_STATS_COMMAND,envp);

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

    }

    /**
     * questionable, RESTServices
     */
    public boolean clearNodeErrorStates(){
	return GridEngineUtil.clearNodeErrorStates();
    }

    
    /**
     * deletes a queue that no longer has nodes associated with it
     * @param queueName the name of the queue to be removed
     */
    public void deleteQueue(String queueName){
	String[] split = queueName.split("\\.");
	String shortQueueName = split[0];

	String[] envp = new String[1];
	envp[0] = "SGE_ROOT="+R.SGE_ROOT;

	//DISABLE the queue: 
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qmod -d " + queueName, envp);
	//DELETE the queue:
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dq " + queueName, envp);
			
	//Delete the host group:
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dhgrp @"+ shortQueueName +"hosts", envp);
    }

   /**
     * questionable, RESTServices
     */
    public boolean createPermanentQueue(QueueRequest req, boolean isNewQueue, HashMap<WorkerNode, Queue> nodesAndQueues){
	return GridEngineUtil.createPermanentQueue(req,isNewQueue,nodesAndQueues);
    }

    /**
     * questionable, moveNodes
     */
    public void moveNodes(String queueName, HashMap<WorkerNode, Queue> NQ){
	
	GridEngineUtil.moveNodes(queueName,NQ);
    }

    /**
     * moves the given node to the given queue
     * @param nodeName the name of a node
     * @param queueName the name of a queue
     */
    public void moveNode(String nodeName, String queueName){
	String[] envp = new String[1];
	envp[0] = "SGE_ROOT="+R.SGE_ROOT;
	Util.executeCommand("sudo -u sgeadmin /cluster/sge-6.2u5/bin/lx24-amd64/qconf -dattr hostgroup hostlist " + nodeName + " @" + queueName + "hosts", envp);
    }


}