package org.starexec.backend;
//nothing should be imported, just temporary
import java.util.HashMap;
import org.starexec.data.to.*;

public interface Backend{

    /**
     * use to initialize fields and prepare backend for tasks
     **/
    public void initialize();

    /**
     * release resources that Backend might not need anymore
     * there's a chance that initialize is never called, so always try dealing with that case
     **/
    public void destroyIf();

    /**start taken from JobManager**/

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
    
    /**start taken from JobPairs**/

    boolean killPair(int execId);

    public void killAll();

    /**end taken from JobPairs**/
    
    /**
     * Returns a string representing the status of jobs running on the system
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
     * @param nodeName the name of a node
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
     * questionable, RESTServices
     */
    public boolean clearNodeErrorStates();

   /**
     * deletes a queue that no longer has nodes associated with it
     * @param queueName the name of the queue to be removed
     */
    public void deleteQueue(String queueName);

   /**
     * questionable, RESTServices
     */
    public boolean createPermanentQueue(QueueRequest req, boolean isNewQueue, HashMap<WorkerNode, Queue> nodesAndQueues);

    /**
     * questionable, MoveNodes
     */
    public void moveNodes(String queueName, HashMap<WorkerNode, Queue> NQ);

    /**
     * moves the given node to the given queue
     * @param nodeName the name of a node
     * @param queueName the name of a queue
     */
    public void moveNode(String nodeName, String queueName);


}



    /**start taken from JobPair**/

        //setGridEngineId
    //getGridEngineId

    /**end taken from JobPair**/