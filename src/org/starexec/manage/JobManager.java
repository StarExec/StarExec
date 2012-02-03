package org.starexec.manage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.ggf.drmaa.*;
import org.starexec.util.*;
import org.starexec.data.database.Jobs;
import org.starexec.data.to.*;
import org.starexec.constants.R;

/**
 * Handles all interaction with SGE.
 * @author Clifton Palmer
 */
public abstract class JobManager {
	private static final Logger log = Logger.getLogger(JobManager.class);
	
	/**
	 * Takes in a job object and discretizes it into SGE jobs, submits and records them.
	 * @param job
	 */
	public static void doJob(Jobject job) {
		int currentJobId = R.NEXT_JID++;
		
		try{
			String jobscript_template = Util.readFile(new File(R.CONFIG_PATH, "jobscript"));
			jobscript_template = jobscript_template.replace("$$JOBID$$", "" + currentJobId);
			String jobscript;
			Job jobRecord;

			//---------------------------
			// TODO: We need to change the schema, as it stands right now due to the nature of job_attr I can't 
			// associate jobtuples with jobs here.
			jobRecord = new Job();
			jobRecord.setId(currentJobId);
			jobRecord.setUserId(job.getUserId());
			
			// Create a jobscript for each tuple and submit
			for(JobTuple tuple : job) {
				//---------------------------
				// Build the jobscript string
				jobscript = jobscript_template;
				jobscript = jobscript.replace("$$QUEUE$$", tuple.getQueue().getName());
				jobscript = jobscript.replace("$$SOLVER$$", tuple.getSolver().getName());
				jobscript = jobscript.replace("$$CONFIG$$", tuple.getConfiguration().getName());
				jobscript = jobscript.replace("$$BENCH$$", tuple.getBenchmark().getName());
				jobscript = jobscript.replace("$$PAIRID$$", "" + tuple.getJobPair().getId());
				
				// Add the jobpair to the job record
				jobRecord.addJobPair(tuple.getJobPair());

				//---------------------------
				// Write the jobscript
				String curJobPath = String.format("%s/%s", R.JOB_INBOX_DIR, String.format(R.JOBFILE_FORMAT, currentJobId));
				File f = new File(curJobPath);
				
				if(f.exists()) f.delete();
				
				f.createNewFile();
				if(!f.setExecutable(true))
					throw new IOException("Can't change owner's executable permissions on file " + curJobPath);
				FileWriter out = new FileWriter(f);
				out.write(jobscript);
				out.close();

				//---------------------------
				// DRMAA submission
		        Session session = SessionFactory.getFactory().getSession();
				JobTemplate jt = null;
				
				// Initialize session
                session.init("");

                // Set up job template
                // DRMAA needs to be told to expect a shell script and not a binary
                jt = session.createJobTemplate();
                jt.setNativeSpecification("-shell y -b n");
                jt.setWorkingDirectory(R.NODE_WORKING_DIR);
                //jt.setOutputPath();
                jt.setRemoteCommand(curJobPath);

                String id = session.runJob(jt);
                log.info(String.format("Job %s (\"%s\") has been submitted.", id, jobscript));

                // Cleanup
                session.deleteJobTemplate(jt);
                session.exit();
			}
			
			// TODO:
			// Record job
			//Jobs.addJob(jobrecord);
		} catch(DrmaaException e) {
			log.warn(e);
		} catch(Exception e) {
			log.info(e);
		}
	}
	
	public static void stopJob(long jid) {
		// TODO: To stop a StarExec job, which might be composed of multiple SGE jobs, we need
		// an associative table in the DB that gives us StarExecJob -> List<SGEJob>
		try{
	        Session session = SessionFactory.getFactory().getSession();
			for(long sgeid : Jobs.getSGEJobIds(jid)) {
	                session.control("" + sgeid, Session.TERMINATE);
			}
		} catch(DrmaaException e) {
			log.info(e);
		}
	}
}
