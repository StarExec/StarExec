package org.starexec.backend;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * This interface is how StarExec should communicate with whatever backend is being used
 * for handling distributing jobs across the compute nodes.
 *
 */
public interface Backend{
    /**
     * NOTES:
     * 
     * BACKEND_ROOT
     *
     * BACKEND_ROOT should be the location of the BACKEND software (path/to/cluster_management_system_dir)
     * the admin should know specifically where BACKEND_ROOT points
     * -------
     * IDENTIFIERS
     *
     * All queue names and node names should also be identifiers
     * Starexec has its own ids for queues and nodes but these are database specific and should be meaningless to the BACKEND
     * Rather, it's expected that all names give to Starexec are also identifers so that when we return names
     * the BACKEND should have all the information it needs
     *
     **/

    /**
     * use to initialize fields and prepare backend for tasks
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     **/
    public void initialize(String BACKEND_ROOT);

    /**
     * release resources that Backend might not need anymore
     * there's a chance that initialize is never called, so always try dealing with that case

     **/
    public void destroyIf();


    /**
     * @param execCode : an execution code (returned by submitScript)
     * @return false if the execution code represents an error, true otherwise
     *
     **/
    public boolean isError(int execCode);

    /**
     * @param scriptPath : the full path to the jobscript file
     * @param workingDirectoryPath  :  path to a directory that can be used for scratch space (read/write)
     * @param logPath  :  path to a directory that should be used to store jobscript logs
     * @return an identifier for the task that submitScript starts, should allow a user to identify which task/script to kill
     **/
    public int submitScript(String scriptPath, String workingDirectoryPath, String logPath);
    

    /**
     * @param execId an int that identifies the pair to be killed, should match what is returned by submitScript
     * @return true if successful, false otherwise
     * kills a jobpair
     */
    public boolean killPair(int execId);

    /**
     * kills all pairs
     * @return true on success and false on error.
     */
    public boolean killAll();


    
    /**
     * @return a string representing the status of jobs running on the system
     */
    public String getRunningJobsStatus();
    
    /**
     * Gets execution codes for all jobs currently active (enqueued or running)
     * @return array of active execution codes
     * @throws IOException 
     */
    public Set<Integer> getActiveExecutionIds() throws IOException;

    /**
     * @return returns a list of names of all active worker nodes
     */
    public String[] getWorkerNodes();

    
    /**
     * @return returns a list of all active queue names
     */
    public String[] getQueues();


    /**
     * @return a map from node name to queue name
     */
    public Map<String, String> getQueueNodeAssociations();

    /**
     * @return true if sucessful, false otherwise
     * should clear any states caused by errors on both queues and nodes
     */
    public boolean clearNodeErrorStates();

   /**
     * deletes a queue that no longer has nodes associated with it
     * @param queueName the name of the queue to be removed
     * @return true if successful, false otherwise
     */
    public boolean deleteQueue(String queueName);

    /**
     * creates a new queue
     *@param newQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     *@return true if successful, false otherwise
     */
    public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames);

    /**
     *@param destQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     * moves nodes from source queues to the destination queue <queueName>
     * the ith element of nodeNames corresponds to the ith element of sourceQueueNames for every i
     * if node is an orphaned node, the corresponding queue name in sourceQueueNames will be null
     * @return True on success and false on failure.
     */
    public boolean moveNodes(String destQueueName,String[] nodeNames,String[] sourceQueueNames);

    /**
     * moves the given node to the given queue
     * @param nodeName the name of a node
     * @param queueName the name of a queue
     * @return true if successful, false otherwise
     */
    public boolean moveNode(String nodeName, String queueName);


}












