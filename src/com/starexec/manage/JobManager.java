package com.starexec.manage;

import java.io.*;

import org.apache.log4j.Logger;
import org.ggf.drmaa.*;

import com.starexec.data.Databases;
import com.starexec.data.to.*;
import com.starexec.util.Util;
import com.starexec.constants.R;

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
	
	private static final Logger log = Logger.getLogger(JobManager.class); 
	private static int curJID; // ............................................ Assigned ID of current job. Part of jobscript name.
	private static Jobject curJob; // ........................................ Pointer to current Jobject (temporarily useful) 
	private static Job jobRecord; // ......................................... Holds record of current job. Built during the buildJob()
	private static String curJobName; // ..................................... Current name of job. "job_<job id>.bash"
	private static String curJobPath;	
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param j Job to do
	 * @throws Exception 
	 */
	public static void doJob(Jobject j) {
		
		// Set up for new job
		curJob = j;
		curJID = R.NEXT_JID++;
		curJobName = String.format(R.JOBFILE_FORMAT, curJID);
		curJobPath = String.format("%s/%s", R.JOB_INBOX_DIR, curJobName);
		jobRecord = null;
		
		try {
			buildJob();
			enqueJob();
			recordJob();
		} catch(Exception e) {
			log(e);
		}
	}
	
	public static int getJID() {
		return curJID;
	}
	
	private static void log(Object o) {
		log.info("JobManager: " + o);
	}
	
	/**
	 * Puts the current job into SGE
	 * @throws Exception
	 */
	private static void enqueJob() throws Exception {
		Session ses = null;
		
		try {
			SessionFactory factory = SessionFactory.getFactory();
			ses = factory.getSession();
			
			ses.init("");
			JobTemplate jt = ses.createJobTemplate();
			jt.setWorkingDirectory(R.NODE_WORKING_DIR);
			jt.setOutputPath(":/dev/null");
			jt.setJoinFiles(true);
			jt.setRemoteCommand(curJobPath);
						
			ses.runJob(jt);
		} catch(Exception e) {
			throw e;
		} finally {
			if(ses != null)
				ses.exit();
		}
	}
	
	/**
	 * Send the job record object into the DB.
	 * I'd like to explicitly set IDs from the JM. 
	 * @throws Exception
	 */
	private static void recordJob() throws Exception {
		if(jobRecord != null) {			
			Databases.next().addJob(jobRecord);
		} else
			throw new Exception("Job record is null");
	}

	/**
	 * Builds a bash script out of the job and deposits in an assigned location.
	 * @throws IOException if filepath is invalid.
	 * 
	 */
	private static void buildJob() throws IOException {
		// Reads in the bash script
		String script = Util.readFile(new File(R.CONFIG_PATH, "jobscript"));
		
		
		// Opens a file on the shared space and writes the empty job script to it.
		String filePath = curJobPath;
		File f = new File(filePath);
		
		if(f.exists()) f.delete();
		
		f.createNewFile();
		if(!f.setExecutable(true))
			throw new IOException("Can't change owner's executable permissions on file " + curJobPath);
		FileWriter out = new FileWriter(f);
				
		
		// Creates a new job TO object
		jobRecord = new Job();
		jobRecord.setJobId(curJID);
		jobRecord.setUserId(curJob.getUser().getUserId());
		jobRecord.setCompleted(null);
		jobRecord.setDescription(curJob.getDescription());
		jobRecord.setNode(null);
		jobRecord.setStatus(R.JOB_STATUS_ENQUEUED);
		jobRecord.setTimeout(Long.MAX_VALUE); // Might have to be user-set. 
		
		
		// Gets all the runpairs
		String ls = Util.getLineSeparator();
		String runPairs = "";
		BenchmarkLink lnk;
		Configuration c;
		Benchmark b;
		Solver s;
		for(int i = curJob.getNumBenchmarks(); i > 0; i--) {
			lnk = curJob.popLink();
			b = lnk.getBenchmark();
			while(lnk.getSize() > 0) {
				c = lnk.getNextConfig();
				s = curJob.getSolver(c.getSolverId());
				runPairs += String.format("runPair %d '%s' '%s' '%s'" + ls, R.PAIR_ID, s.getPath(), b.getPath(), c.getName(), R.PAIR_ID);
				
				JobPair jp = new JobPair();
				jp.setId(R.PAIR_ID);
				jp.setSolver(s);
				jp.setConfig(c);
				jp.setBenchmark(b);
				jp.setJobId(curJID);
				jp.setResult(null);
				
				jobRecord.addJobPair(jp);
				R.PAIR_ID++;
			}
		}
		
		
		// Writes out the bash file
		script = script.replace("$$JID$$", "" + curJID);
		script = script.replace("$$JOBSTA$$", R.JOB_STATUS_RUNNING);
		script = script.replace("$$JOBFIN$$", R.JOB_STATUS_DONE);
		script = script.replace("$$JOBERR$$", R.JOB_STATUS_ERR);
		script = script.replace("$$JOBPAIRS$$", runPairs);
		// TODO: Use jobout from R
		out.write(script);
		
		
		out.close();
	}
}
