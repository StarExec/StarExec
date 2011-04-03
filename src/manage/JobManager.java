package manage;

import java.io.*;

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
	private static String workingDirectory;
	
	/**
	 *  Builds, enqueues, and records the job.
	 *  @param j Job to do
	 * @throws Exception 
	 */
	public static void doJob(Jobject j) throws Exception {
		curJob = j;
		curJID = -1;	// Dummy value
		workingDirectory = "/export/starexec/jobin";
				
		try {
			recordJob();
			buildJob();
			enqueJob();
		}
		catch(Exception e) {
			throw new Exception("Exception in Jobject : " + e.getMessage());
		}
	}
	
	private static void enqueJob() {
		// TODO Auto-generated method stub
		
	}

	private static void recordJob() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Builds a bash script out of the job and deposits in an assigned location.
	 * @throws IOException if filepath is invalid.
	 * 
	 */
	private static void buildJob() throws IOException {
		// Open a file on the shared space and write the job script to it.
		FileWriter out = new FileWriter(String.format("%s/%s", workingDirectory, "testing.out"));
		
		out.write("#!/bin/bash"
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
				+ "SSP=$SHR/Solvers	# Solver Shared Path\n"
				+ "BSP=$SHR/Benchmarks\n"
				+ "\n"
				+ "# Test values. No equivalents in real script.\n"
				+ "TSOL=z3\n"
				+ "TBEN=model_6_66.smt2 \n"
				+ "\n"
				+ "JOB=tmp\n"
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
				+ "	SOL=$1\n"
				+ "	BEN=$2\n"
				+ "	SWP=$WDIR/$SOL # Solver Working Path\n"
				+ "	BWP=$WDIR/$BEN\n"
				+ "\n"
				+ "	# Make sure files exist!\n"
				+ "	# I hope this isn't necessary. I want \n"
				+ "	# this code to be less granular.\n"
				+ "	if [ ! -f $SWP ]; then\n"
				+ "		echo Copying $SOL to working directory $WDIR\n"
				+ "		cp $SSP/$SOL $WDIR\n"
				+ "	fi\n"
				+ "	\n"
				+ "	if [ ! -f $BWP ]; then\n"
				+ "		echo Copying $BEN to working directory $WDIR\n"
				+ "		cp $BSP/$BEN $WDIR\n"
				+ "	fi\n"
				+ "		\n"
				+ "\n"
				+ "	# Now how should we handle flags for solvers???\n"
				+ "	echo Running $SOL on $BEN ...\n"
				+ "	ST=`$T`\n"
				+ "	RESULT=`$SWP -smt2 $BWP`\n"
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
				+ "echo Job ID: $JOB_ID \n"
				+ "echo Directory: $M:$PWD\n"
				+ "echo Shell: $SHELL\n"
				+ "echo Out: $JOBFILE\n"
				+ "echo '*************************************'\n"
				+ "echo\n"
				+ "\n"
				+ "# We'd have all of our tests right here.\n"
				+ "runsb $TSOL $TBEN\n"
				+ "\n"
				+ "\n"
				+ "# /////////////////////////////////////////////\n"
				+ "# Teardown\n"
				+ "# /////////////////////////////////////////////\n"
				+ "mv -f $JOBFILE $SHR/jobout\n"
				+ "\n"
				+ "# Cleanup. May not be necessary if we want to cache solvers/benchmarks\n"
				+ "#rm $WDIR/*\n" );
	}
}
