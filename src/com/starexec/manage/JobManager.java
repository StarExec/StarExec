package com.starexec.manage;

import java.io.*;
import java.util.logging.Logger;

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
		Logger.getAnonymousLogger().info("JobManager: " + o);
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
			throw new IOException("Can't change owner's executable permissions on file " + curJobPath);
		FileWriter out = new FileWriter(f);
		
		// Create a new job TO object
		jobRecord = new Job();
		jobRecord.setJobId(curJID);
		jobRecord.setUserId(curJob.getUser().getUserId());
		jobRecord.setCompleted(null);
		jobRecord.setDescription(curJob.getDescription());
		jobRecord.setNode(null);
		jobRecord.setStatus(R.JOB_STATUS_ENQUEUED);
		jobRecord.setTimeout(Long.MAX_VALUE); // Might have to be user-set. 
			
		
		// Write the ugly, ugly bash file. Shouldn't be hardcoded like this.
		out.write("#!/bin/bash\n"
				+ "# This submits a test bash job to the SGE\n"
				+ "#$ -j y\n"
				+ "#$ -o /dev/null\n"
				+ "#$ -S /bin/bash \n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Setup\n"
				+ "# /////////////////////////////////////////////\n"
				+ "ROOT='/export/starexec'\n"
				+ "WDIR=$ROOT/workspace\n"
				+ "SHR=/home/starexec\n"
				+ "T=\"date +%s.%m\"\n"
				+ "M=`uname -n`; M=${M%%\\.*}\n"
				+ "\n"
				+ "J_ID=" + curJID + "\n"
				+ "J_STA=" + R.JOB_STATUS_RUNNING + "\n"
				+ "J_FIN=" + R.JOB_STATUS_DONE + "\n"
				+ "J_ERR=" + R.JOB_STATUS_ERR + "\n"
				+ "RESULT=''\n"
				+ "\n"
				+ "JOB=job_$J_ID\n"
				+ "JOBFILE=$WDIR/$JOB.out\n"
				+ "\n"
				+ "# Redirect stdout and stderr streams\n"
				+ "exec 1>$JOBFILE 2>&1\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Functions\n"
				+ "# /////////////////////////////////////////////\n"
				+ "function runPair { # Run a solver on a benchmark\n"
				+ "\n"
				+ "	PID=$1\n"
				+ "	SPATH=$2\n"
				+ "	BPATH=$3\n"
				+ "	RUN=$4\n"
				+ "	ST=''\n"
				+ "	FT=''\n"
				+ "	RESULT=''\n"
				+ "\n"
				+ "	if [ $# != 4 ]; then\n"
				+ "		echo Incorrect number of args for $0\n"
				+ "		sendPairStatus $PID $J_ERR\n"
				+ "		return 1\n"
				+ "	fi\n"
				+ "\n"
				+ "	sendPairStatus $PID $J_STA\n"
				+ "\n"
				+ "	SOL=${SPATH##*/} # Solver directory name\n"
				+ "	SWD=$WDIR/$SOL   # Solver Working Directory \n"
				+ "	SWP=$SWD/$RUN\n"
				+ "\n"
				+ "	BEN=${BPATH##*/} # Benchmark name\n"
				+ "	BWP=$WDIR/$BEN   # Benchmark Working Path\n"
				+ "\n"
				+ "	# If directory doesn't exist, create it.\n"
				+ "	if [ ! -d $SWD ]; then\n"
				+ "		mkdir $SWD\n"
				+ "	fi\n"
				+ "\n"
				+ "	# Copy all contents from the solver's shared bin dir to here\n"
				+ "	cp $SPATH/bin/* $SWD\n"
				+ "\n"
				+ "	# Make sure solver runfile exists\n"
				+ "	if [ ! -f $SWP ]; then\n"
				+ "		echo Cannot find runfile $RUN in $SWD\n"
				+ "		return 1\n"
				+ "	fi\n"
				+ "	\n"
				+ "	# Copy benchmark to workspace. WARNING: can't tell the difference between benchmarks with same name!\n"
				+ "	cp $BPATH $WDIR\n"
				+ "\n"
				+ "	# Change dir to solver working directory for runfile\n"
				+ "	cd $SWD\n"
				+ "		\n"
				+ "	# Running $SOL on $BEN ...\n"
				+ "	ST=`$T`\n"
				+ "	RESULT=`$SWP $BWP`\n"
				+ "	FT=`$T`\n"
				+ "	\n"
				+ "	ST=$(((${ST%\\.*} * 1000) + ${ST#*\\.}))\n"
				+ "	FT=$(((${FT%\\.*} * 1000) + ${FT#*\\.}))\n"
				+ "\n"
				+ "	# Calculating total time, seconds/ms\n"
				+ "	# STIME=$((${FT%\\.*} - ${ST%\\.*}))\n"
				+ "	#MTIME=$((${FT#*\\.} - ${ST#*\\.}))\n"
				+ "\n"
				+ "	# Print runtime for solver/benchmark pair \n"
				+ "	#echo \"$SOL($BEN) : $RESULT : $STIME.$MTIME\"\n"
				+ "	echo \"$SOL($BEN) : $RESULT : $(($ST - $FT))\"\n"
				+ "\n"
				+ "	sendPairStatus $PID $J_FIN\n"
				+ "}\n"
				+ "\n"
				+ "function toResultServlet {\n"
				+ "	wget \"starexec/starexec/Results?node=$M&$1\" -o /dev/null  -O /dev/null --spider\n"
				+ "	return $?\n"
				+ "}\n"
				+ "\n"
				+ "function sendPairStatus {\n"
				+ "	toResultServlet \"pid=$1&status=$2&result=$RESULT&stime=$ST&etime=$FT\" \n"
				+ "	return $?\n"
				+ "}\n"
				+ "\n"
				+ "function sendJobStatus {\n"
				+ "	toResultServlet \"jid=$J_ID&status=$1\"\n"
				+ "	return $?\n"
				+ "}\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Run job\n"
				+ "# /////////////////////////////////////////////\n"
				+ "echo '*************************************'\n"
				+ "echo \"SGE Job ID: $JOB_ID\" \n"
				+ "echo \"JM Job ID:  $J_ID\"\n"
				+ "echo \"Directory:  $M:$PWD\"\n"
				+ "echo \"Shell:      $SHELL\"\n"
				+ "echo \"Out:        $JOBFILE\"\n"
				+ "echo '*************************************'\n"
				+ "echo\n"
				+ "sendJobStatus $J_STA # Signals that whole job starts.\n"
				+ "\n" );
		
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
				out.write(String.format("runPair %d '%s' '%s' '%s'\n\n", R.PAIR_ID, s.getPath(), b.getPath(), c.getName(), R.PAIR_ID));
				
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
		
		out.write("\n"
				+ "\n"
				+ "sendJobStatus $J_FIN # Signals that whole job terminates.\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Teardown\n"
				+ "# /////////////////////////////////////////////\n"
				+ "mv -f $JOBFILE $SHR/jobout\n"
				+ "\n"
				+ "# Cleanup. Not necessary if we want to cache solvers/benchmarks\n"
				+ "rm -rf $WDIR/* \n" );
		out.close();
	}
}
