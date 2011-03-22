package manage;

/**
 * --------
 * ABSTRACT
 * --------
 * This class is called whenever a job is added to the queue. 
 * The JM handles job script creation and initialization based on a Jobject that it is passed. 
 * 
 * -------
 * OUTLINE
 * -------
 * 1. Receive job (Jobject).
 * 2. Create job script, populated from dissolved Jobject.
 * 3. Record job.
 * 
 * JobManager should also be in charge of killing jobs.
 * Timing should be probably handled at the high level. How should we handle freezing jobs though?
 * 
 * @author cpalmer
 * 
 */
public abstract class JobManager {
	public static String jobScriptPath = "/home/starexec/Scripts";		// All jobs in script form go here on the master.
	private static Jobject curJob;
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param Job to do
	 */
	public static void doJob(Jobject j) {
		curJob = j;
		
		buildJob();
		//enqueJob()
		//recordJob();
	}
	
	/**
	 * Builds a script out of the job and deposits in an assigned location.
	 */
	private static void buildJob() {
		
	}
}
