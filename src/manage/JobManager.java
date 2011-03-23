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
	private static String jobScriptPath = "/home/starexec/Scripts"; // ... All jobscripts go here before being enqueued. 
	private static Jobject curJob; // .................................... Pointer to current Jobject (temporarily useful) 
	private static int curJID; // ........................................ Assigned ID of current job. Part of jobscript name.
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param Job to do
	 */
	public static void doJob(Jobject j) {
		curJob = j;
		curJID = -1;	// Dummy value
				
		// Creates the job script
		buildJob();
		
		//enqueJob()
		//recordJob();
	}
	
	/**
	 * Builds a script out of the job and deposits in an assigned location.
	 * Each jobscript is composed of x parts:
	 * 0. Deal with caching ... should jobs go to the nodes with warmest caches? In any case, query a cache list of benchmarks. 
	 * 1. Copy solvers/benchmarks to local node's workspace ... also set up solvers with appropriate benchmarks in script. (should be done in one step)
	 * 1.x. Deal with job output?
	 * 2. Load jobscript into grid engine ... make job entry into database
	 * 
	 */
	private static void buildJob() {
	}
}
