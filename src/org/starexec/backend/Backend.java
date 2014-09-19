package org.starexec.backend;


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

    /**end taken from JobPairs**/
}




    /**end taken from JobManager**/




    /**start taken from Jobs**/

    //boolean kill(int jobId, Connection con);


    /**end taken from Jobs**/




    /**start taken from JobPair**/

        //setGridEngineId
    //getGridEngineId

    /**end taken from JobPair**/