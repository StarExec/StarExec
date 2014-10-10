package org.starexec.backend;


import org.apache.log4j.Logger;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.starexec.jobs.JobManager;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Util;



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


}