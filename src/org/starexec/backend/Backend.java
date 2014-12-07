package org.starexec.backend;

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
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     **/
    public void destroyIf(String BACKEND_ROOT);


    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param execCode : an execution code (returned by submitScript)
     * @return false if the execution code represents an error, true otherwise
     *
     **/
    public boolean isError(String BACKEND_ROOT,int execCode);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param scriptPath : the full path to the jobscript file
     * @param workingDirectoryPath  :  path to a directory that can be used for scratch space (read/write)
     * @param logPath  :  path to a directory that should be used to store jobscript logs
     * @return an identifier for the task that submitScript starts, should allow a user to identify which task/script to kill
     **/
    public int submitScript(String BACKEND_ROOT,String scriptPath, String workingDirectoryPath, String logPath);
    

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param execId an int that identifies the pair to be killed, should match what is returned by submitScript
     * @return true if successful, false otherwise
     * kills a jobpair
     */
    public boolean killPair(String BACKEND_ROOT,int execId);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * kills all pairs
     */
    public void killAll(String BACKEND_ROOT);


    
    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return a string representing the status of jobs running on the system
     */
    public String getRunningJobsStatus(String BACKEND_ROOT);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return returns a list of names of all active worker nodes
     */
    public String[] getWorkerNodes(String BACKEND_ROOT);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given node
     * where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     * 
     */
    public String[] getNodeDetails(String BACKEND_ROOT,String nodeName);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return returns a list of all active queues
     */
    public String[] getQueues(String BACKEND_ROOT);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return returns the default queue name, should be an active queue
     */
    public String getDefaultQueueName(String BACKEND_ROOT);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param nodeName the name of a node
     * @return an even-sized String[] representing a details map for a given queue
     *  where key is the attribute name and value is the attribute value: [key1,value1,key2,value2,key3,value3]
     */
    public String[] getQueueDetails(String BACKEND_ROOT,String name);


    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @return an array that represents queue-node assocations: [queueName1,nodeName1,queueName1,nodeName2,queueName2,nodeName3]
     * the queue and node names should match the names returned when calling getWorkerNodes and getQueues.
     * queue names are found in the even-indexed positions, node name otherwise. 
     *  a queue at index i is associated with the node at index i + 1
     */
    public String[] getQueueNodeAssociations(String BACKEND_ROOT);

    /**
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param allQueueNames the names of all queues, 
     * @return true if sucessful, false otherwise
     * should clear any states caused by errors on both queues and nodes
     */
    public boolean clearNodeErrorStates(String BACKEND_ROOT,String[] allQueueNames);

   /**
     * deletes a queue that no longer has nodes associated with it
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param queueName the name of the queue to be removed
     * @return true if successful, false otherwise
     */
    public boolean deleteQueue(String BACKEND_ROOT,String queueName);

    /**
     * creates a new queue
     *@param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     *@param isNewQueue true if creating a new queue, false if only switching status to permanent
     *@param destQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     *@return true if successful, false otherwise
     */
    public boolean createPermanentQueue(String BACKEND_ROOT,boolean isNewQueue,String destQueueName, String[] nodeNames, String[] sourceQueueNames);

    /**
     *@param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     *@param destQueueName the name of the destination queue
     *@param nodeNames the names of the nodes to be moved 
     *@param sourceQueueNames the names of the source queues
     * moves nodes from source queues to the destination queue <queueName>
     * the ith element of nodeNames corresponds to the ith element of sourceQueueNames for every i
     * if node is an orphaned node, the corresponding queue name in sourceQueueNames will be null
     */
    public void moveNodes(String BACKEND_ROOT,String destQueueName,String[] nodeNames,String[] sourceQueueNames);

    /**
     * moves the given node to the given queue
     * @param BACKEND_ROOT the path to the backend root, for sge found in R.SGE_ROOT
     * @param nodeName the name of a node
     * @param queueName the name of a queue
     * @return true if successful, false otherwise
     */
    public boolean moveNode(String BACKEND_ROOT,String nodeName, String queueName);


}












