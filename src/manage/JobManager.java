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
	private static Jobject curJob; // .................................... Pointer to current Jobject (temporarily useful) 
	private static int curJID; // ........................................ Assigned ID of current job. Part of jobscript name.
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param j Job to do
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
//		#!/bin/bash
//		# This submits a test job to the SGE
//		# In an actual job script, everything would have been migrated to the local machine.
//		#$ -j y
//
//		set DIR='/export/starexec/workspace'
//		set SOL=z3
//		set BEN=model_6_66.smt2 
//		set SOL_PATH=/home/starexec/Solvers/$SOL
//		set BEN_PATH=/home/starexec/Benchmarks/$BEN
//
//		echo Copying the z3 solver and 666 benchmark to $DIR
//		cp $SOL_PATH $DIR
//		cp $BEN_PATH $DIR
//
//		echo Running the 666 benchmark on z3
//		# /home/starexec/Solvers/z3 -smt2 /home/starexec/Benchmarks/model_6_66.smt2
//		$DIR/$SOL -smt2 $DIR/$BEN
//
//		echo Ran on machine `uname -n`

	}
}
