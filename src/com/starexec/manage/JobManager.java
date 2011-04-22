package com.starexec.manage;

import java.io.*;
import java.util.Date;

import org.ggf.drmaa.*;

import com.starexec.data.Database;
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
	public static void doJob(Jobject j) throws Exception {
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
			throw new Exception("Exception in Jobject : " + e);
		}
	}
	
	public static int getJID() {
		return curJID;
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
			
			jobRecord.setSubmitted(new Date());
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
		/*
		 * NOTE: uses the 'wget' command in script to call servlet that handles interface to SQL server on starexec
		 * Ex: wget "starexec/starexec/Results?type=job&id=2&node=starexec1&status=done" -O /dev/null --spider
		 * 
		 * Job has the following statuses : 0 (done), 1 (running), 2 (enqueued), 3 (error), 4 (frozen)
		 * 
		 */
		// Open a file on the shared space and write the job script to it.
		String filePath = curJobPath;
		File f = new File(filePath);
		
		if(f.exists()) f.delete();
		
		f.createNewFile();
		if(!f.setExecutable(true))
			throw new IOException("Can't change owner's executable permissions on file.");
		FileWriter out = new FileWriter(f);
		
		// Create a new job TO object
		jobRecord = new Job();
		jobRecord.setJobId(curJID);
		jobRecord.setUserId(curJob.getUser().getUserId());
		jobRecord.setCompleted(null);
		jobRecord.setDescription(curJob.getDescription());
		jobRecord.setNode("local");
		jobRecord.setStatus("enqueued");
		jobRecord.setTimeout(Long.MAX_VALUE); // Might have to be user-set. 
			
		
		// Write the ugly, ugly bash file. Shouldn't be hardcoded like this.
		out.write("#!/bin/bash\n"
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
				+ "JID=" + curJID + "\n"
				+ "JOB=job_$JID\n"
				+ "JOBFILE=$WDIR/$JOB.out\n"
				+ "\n"
				+ "RESULT=''\n"
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
				+ "function runsb { # Run a solver on a benchmark\n"
				+ "	SPATH=$1\n"
				+ "	BPATH=$2\n"
				+ "	SOL=${SPATH##*/}\n"
				+ "	BEN=${BPATH##*/}\n"
				+ "	SWP=$WDIR/$SOL # Solver Working Path\n"
				+ "	BWP=$WDIR/$BEN\n"
				+ "\n"
				+ "	# Make sure files exist!\n"
				+ "	if [ ! -f $SWP ]; then\n"
				+ "		#echo Copying $SOL to working directory $WDIR\n"
				+ "		cp $SPATH $WDIR\n"
				+ "	fi\n"
				+ "	\n"
				+ "	if [ ! -f $BWP ]; then\n"
				+ "		#echo Copying $BEN to working directory $WDIR\n"
				+ "		cp $BPATH $WDIR\n"
				+ "	fi\n"
				+ "		\n"
				+ "	#echo Running $SOL on $BEN ...\n"
				+ "	ST=`$T`\n"
				+ "	RESULT=`$SWP $BWP`\n"
				+ "	FI=`$T`\n"
				+ "	\n"
				+ "	STIME=$((${FI%\\.*} - ${ST%\\.*}))\n"
				+ "	MTIME=$((${FI#*\\.} - ${ST#*\\.}))\n"
				+ "\n"
				+ "	# Print runtime for solver \n"
				+ "	echo \"$SOL($BEN) : $RESULT : $STIME.$MTIME\"\n"
				+ "}\n"
				+ "\n"
				+ "function sendResult {\n"
				+ "	if [ $# -ne 1 ]; then\n"
				+ "		echo $0 : Wrong number of arguments -- wants 1, got $#\n"
				+ "		return 1\n"
				+ "	fi \n"
				+ "\n"
				+ "	wget \"starexec/starexec/Results?pid=$1&result=$RESULT\" -o /dev/null  -O /dev/null --spider\n"
				+ "\n"
				+ "	return $?\n"
				+ "}\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Run job\n"
				+ "# /////////////////////////////////////////////\n"
				+ "echo '*************************************'\n"
				+ "echo \"SGE Job ID: $JOB_ID\" \n"
				+ "echo \"JM Job ID:  $JID\"\n"
				+ "echo \"Directory:  $M:$PWD\"\n"
				+ "echo \"Shell:      $SHELL\"\n"
				+ "echo \"Out:        $JOBFILE\"\n"
				+ "echo '*************************************'\n"
				+ "echo\n"
				+ "\n" );
		
		SolverLink lnk;
		Solver s;
		Benchmark b;
		for(int i = curJob.getNumSolvers(); i > 0; i--) {
			lnk = curJob.popLink();
			for(int j = lnk.getSize(); j > 0; j--) {
				s = lnk.getSolver();
				b = lnk.getNextBenchmark();
				out.write(String.format("runsb %s %s\nsendResult %d\n\n", s.getPath(), b.getPath(), R.PAIR_ID));
				
				JobPair jp = new JobPair();
				jp.setId(R.PAIR_ID);
				jp.setSolver(s);
				jp.setBenchmark(b);
				jp.setJobId(curJID);
				jp.setResult(null);
				
				jobRecord.addJobPair(jp);
				R.PAIR_ID++;
			}
		}
		
		out.write("\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Teardown\n"
				+ "# /////////////////////////////////////////////\n"
				+ "mv -f $JOBFILE $SHR/jobout\n"
				+ "\n"
				+ "# Cleanup. Not necessary if we want to cache solvers/benchmarks\n"
				+ "#rm $WDIR/*\n" );
		out.close();
	}
}
