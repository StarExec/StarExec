package com.starexec.manage;

import java.io.*;
import java.util.Date;

import org.ggf.drmaa.*;

import com.starexec.data.Database;
import com.starexec.data.to.*;

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
	private static int curJID; // ............................................ Assigned ID of current job. Part of jobscript name.
	private static Jobject curJob; // ........................................ Pointer to current Jobject (temporarily useful) 
	private static Job jobRecord; // ......................................... Holds record of current job. Built during the buildJob()
	private static String curJobName; // ..................................... Current name of job. "job_<job id>.bash"
	private static final String jobinDir = "/home/starexec/jobin"; // ........ Should be the job inbox .
	private static final String workDir = "/project/tomcat-webapps/webapps";// Working directory on each local node.
	private static final String jobFileNameFormat = "job_%d.bash";
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param j Job to do
	 * @throws Exception 
	 */
	public static void doJob(Jobject j) throws Exception {
		curJob = j;
		curJID = -1; // Dummy value. Must have a reference to some constant static value ....
		curJobName = String.format(jobFileNameFormat, curJID);
		jobRecord = null;
		
		try {
			buildJob();
			enqueJob();
			recordJob();
		} catch(Exception e) {
			throw new Exception("Exception in Jobject : " + e);
		}
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
			jt.setRemoteCommand(jobinDir + File.separatorChar + curJobName);
			
			jobRecord.setSubmitted(new Date());
			String id = ses.runJob(jt);					
			
			// This keeps a tab on the running job ... also brings the JM to a halt! Testing ONLY.
			JobInfo info = ses.wait(id, Session.TIMEOUT_WAIT_FOREVER);
			if(info.wasAborted())
				throw new Exception("Job " + id + " was aborted.");
		} catch(Exception e) {
			throw e;
		} finally {
			if(ses != null)
				ses.exit();
		}
	}
	
	/**
	 * Send the job record object into the DB
	 * @throws Exception
	 */
	private static void recordJob() throws Exception {
		if(jobRecord != null) {
			Database db = new Database();
			db.addJob(jobRecord);
		} else
			throw new Exception("Job record is null");
	}

	/**
	 * Builds a bash script out of the job and deposits in an assigned location.
	 * @throws IOException if filepath is invalid.
	 * 
	 */
	private static void buildJob() throws IOException {
		// Open a file on the shared space and write the job script to it.
		String filePath = String.format("%s/%s", jobinDir, curJobName);
		File f = new File(filePath);
		
		if(f.exists()) f.delete();
		
		f.createNewFile();
		if(!f.setExecutable(true))
			throw new IOException("Can't change owner's executable permissions on file.");
		FileWriter out = new FileWriter(f);
		
		// Create a new job TO object
		jobRecord = new Job();
		jobRecord.setJobId(curJID);
		jobRecord.setUserId(-1);
		//jobRecord.setSubmitted(new Date()); Set at last second possible.
		jobRecord.setCompleted(null);
		jobRecord.setDescription("Default");
		jobRecord.setNode("Default");
		jobRecord.setStatus("Default");
		jobRecord.setTimeout(Long.MAX_VALUE);
			
		
		// Write the ugly, ugly bash file
		out.write("#!/bin/bash\n"
				+ "# This submits a test bash job to the SGE\n"
				+ "#$ -j y\n"
				+ "#$ -o /dev/null\n"
				+ "#$ -S /bin/bash\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Setup\n"
				+ "# /////////////////////////////////////////////\n"
				+ "ROOT='/export/starexec'\n"
				+ "WDIR=$ROOT/workspace\n"
				+ "SHR=/home/starexec\n"
				+ "\n"
				+ "JOB=job_" + curJID + "\n"
				+ "JOBFILE=$WDIR/$JOB.out\n"
				+ "\n"
				+ "T=\"date +%s.%m\"\n"
				+ "M=`uname -n`; M=${M%%\\.*}\n"
				+ "\n"
				+ "# Redirect stdout and stderr streams\n"
				+ "exec 1>$JOBFILE 2>&1\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Functions\n"
				+ "# /////////////////////////////////////////////\n"
				+ "function runsb {\n"
				+ "	SPATH=$1\n"
				+ "	BPATH=$2\n"
				+ "	SOL=${SPATH##*/}\n"
				+ "	BEN=${BPATH##*/}\n"
				+ "	SWP=$WDIR/$SOL # Solver Working Path\n"
				+ "	BWP=$WDIR/$BEN\n"
				+ "\n"
				+ "	# Make sure files exist!\n"
				+ "	# I hope this isn't necessary. I want \n"
				+ "	# this code to be less granular.\n"
				+ "	if [ ! -f $SWP ]; then\n"
				+ "		echo Copying $SOL to working directory $WDIR\n"
				+ "		cp $SPATH $WDIR\n"
				+ "	fi\n"
				+ "	\n"
				+ "	if [ ! -f $BWP ]; then\n"
				+ "		echo Copying $BEN to working directory $WDIR\n"
				+ "		cp $BPATH $WDIR\n"
				+ "	fi\n"
				+ "		\n"
				+ "\n"
				+ "	# Now how should we handle flags for solvers???\n"
				+ "	echo Running $SOL on $BEN ...\n"
				+ "	ST=`$T`\n"
				+ "	RESULT=`$SWP $BWP`\n"
				+ "	FI=`$T`\n"
				+ "	\n"
				+ "	STIME=$((${FI%\\.*} - ${ST%\\.*}))\n"
				+ "	MTIME=$((${FI#*\\.} - ${ST#*\\.}))\n"
				+ "\n"
				+ "	# Print runtime for solver run\n"
				+ "	echo \"$SOL($BEN) : $RESULT : $STIME.$MTIME\"\n"
				+ "}\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Run job\n"
				+ "# /////////////////////////////////////////////\n"
				+ "echo '*************************************'\n"
				+ "echo JID: $JOB_ID \n"
				+ "echo MNM: $M\n"
				+ "echo DIR: $PWD\n"
				+ "echo SHL: $SHELL\n"
				+ "echo OUT: $JOBFILE\n"
				+ "echo '*************************************'\n"
				+ "echo\n"
				+ "\n"
				+ "# All tests are right here\n" );
		
		SolverLink lnk;
		Solver s;
		Benchmark b;
		for(int i = curJob.getNumSolvers(); i > 0; i--) {
			lnk = curJob.popLink();
			for(int j = lnk.getSize(); j > 0; j--) {
				s = lnk.getSolver();
				b = lnk.getNextBenchmark();
				out.write(String.format("runsb %s %s\n", s.getPath(), b.getPath()));
				
				JobPair jp = new JobPair();
				jp.setSolver(s);
				jp.setBenchmark(b);
				jp.setJobId(curJID);
				jp.setResult(null);
				
				jobRecord.addJobPair(jp);
			}
		}
		
		out.write("\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Teardown\n"
				+ "# /////////////////////////////////////////////\n"
				+ "mv -f $JOBFILE $SHR/jobout\n"
				+ "\n"
				+ "# Cleanup. May not be necessary if we want to cache solvers/benchmarks\n"
				+ "rm $WDIR/*\n" );
		out.close();
	}
}
