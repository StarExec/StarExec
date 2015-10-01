package org.starexec.backend;

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
     * all methods take in BACKEND_ROOT as the first parameter
     * BACKEND_ROOT should be the location of the BACKEND software (path/to/cluster_management_system_dir)
     * the admin should know specifically where BACKEND_ROOT points,
     * It is sent to every single method just in case it's necessary to access a command or file in the BACKEND software
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
     * @return returns a list of names of all active worker nodes
     */
    public String[] getWorkerNodes();

    /**
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given node
     * where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     * 
     */
    public String[] getNodeDetails(String nodeName);

    /**
     * @return returns a list of all active queues
     */
    public String[] getQueues();

    /**
     * @return returns the default queue name, should be an active queue
     */
    public String getDefaultQueueName();

    /**
     * @param name the name of a node
     * @return an even-sized String[] representing a details map for a given queue
     *  where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public String[] getQueueDetails(String name);


    /**
     * @return an array that represents queue-node assocations: [queueName1,nodeName1,queueName1,nodeName2,queueName2,nodeName3]
     * the queue and node names should match the names returned when calling getWorkerNodes and getQueues.
     * queue names are found in the even-indexed positions, node name otherwise. 
     *  a queue at index i is associated with the node at index i + 1
     */
    public String[] getQueueNodeAssociations();

    /**
     * @param allQueueNames the names of all queues, 
     * @return true if sucessful, false otherwise
     * should clear any states caused by errors on both queues and nodes
     */
    public boolean clearNodeErrorStates(String[] allQueueNames);

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












