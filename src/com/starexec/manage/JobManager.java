package com.starexec.manage;

import java.io.*;

import org.apache.log4j.Logger;
import org.ggf.drmaa.*;

import com.starexec.data.Database;
import com.starexec.data.Databases;
import com.starexec.data.to.*;
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
	private static final String jobinDir = "/home/starexec/jobin"; // ........ Should be the job inbox .
	private static final String workDir = "/project/tomcat-webapps/webapps";// Working directory on each local node.
	private static final String jobFileNameFormat = "job_%d.bash";
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param j Job to do
	 * @throws Exception 
	 */
	public static void doJob(Jobject j) {
		
		// Set up for new job
		curJob = j;
		curJID = R.NEXT_JID++;
		curJobName = String.format(jobFileNameFormat, curJID);
		curJobPath = String.format("%s/%s", jobinDir, curJobName);
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
			jt.setWorkingDirectory(workDir);
			jt.setOutputPath(":/dev/null");
			jt.setJoinFiles(true);
			jt.setRemoteCommand(curJobPath);
						
			ses.runJob(jt);
			/*
			String sge_id = ses.runJob(jt);	
			// This keeps a tab on the running job ... also brings the JM to a halt! Testing ONLY.
			JobInfo info = ses.wait(sge_id, Session.TIMEOUT_WAIT_FOREVER);
			if(info.wasAborted())
				throw new Exception(String.format("Job %d was aborted (SGE ID %d)\n", curJID, sge_id));
			*/
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
		// Reads in the bash script and inserts variables
		BufferedReader reader = null;
		String ls = System.getProperty("line.separator");
		String script = "";
		
		try {
			// TODO: Make this path relative to the project directory
			reader = new BufferedReader(new FileReader(new File("/project/tomcat-webapps/webapps/starexec/WEB-INF/classes/jobscript")));
			String line = null;
			StringBuilder str = new StringBuilder();
			while( (line = reader.readLine()) != null ) {
				str.append(line + ls);
			}
			script = str.toString();
		} catch(IOException e) {
			if(reader != null) reader.close();
			throw e;
		}
		
		log("Read in scriptfile");
		
		// Opens a file on the shared space and writes the empty job script to it.
		String filePath = curJobPath;
		File f = new File(filePath);
		
		if(f.exists()) f.delete();
		
		f.createNewFile();
		if(!f.setExecutable(true))
			throw new IOException("Can't change owner's executable permissions on file " + curJobPath);
		FileWriter out = new FileWriter(f);
		
		log("Opened empty jobscript file");
		
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
		
		log("Built all runpairs: " + runPairs);
		
		// Writes out the bash file
		script = script.replace("$$JID$$", "" + curJID);
		script = script.replace("$$JOBSTA$$", R.JOB_STATUS_RUNNING);
		script = script.replace("$$JOBFIN$$", R.JOB_STATUS_DONE);
		script = script.replace("$$JOBERR$$", R.JOB_STATUS_ERR);
		script = script.replace("$$JOBPAIRS$$", runPairs);
		out.write(script);
		
		log("Wrote out bash file");
		
		out.close();
	}
}
