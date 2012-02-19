package org.starexec.jobs;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Util;

/**
 * Handles all SGE interactions for job submission and maintenance
 * @author Tyler Jensen
 */
public abstract class JobManager {
	private static final Logger log = Logger.getLogger(JobManager.class);
	
	/**
	 * Submits a job to the grid engine and records it in the database
	 * @param j The job object containing information about what to run for the job
	 * @param spaceId The id of the space this job will be placed in
	 */
	public static boolean submitJob(Job job, int spaceId) {		
		try {
			// Attempt to add the job to the database			
			boolean jobAdded = Jobs.add(job, spaceId);
			
			// If for some reason that failed, don't run on the grid engine
			if(false == jobAdded) {
				log.error(String.format("Job failed to be added to the database and was prevented from running on the grid [user=%d] [space=%d]", job.getUserId(), spaceId));
				return false;
			}
			
			// Read in the job script template and format it for all the pairs in this job
			String jobTemplate = Util.readFile(new File(R.CONFIG_PATH, "jobscript"));
			jobTemplate = jobTemplate.replace("$$QUEUE$$", job.getQueue().getName());			
			jobTemplate = jobTemplate.replace("$$JOBID$$", "" + job.getId());
			jobTemplate = jobTemplate.replace("$$DB_NAME$$", "" + R.MYSQL_DATABASE);
			jobTemplate = jobTemplate.replace("$$USERID$$", "" + job.getUserId());
			jobTemplate = jobTemplate.replace("$$OUT_DIR$$", "" + R.NODE_OUTPUT_DIR);
			
			// Optimization, do outside of loop
			boolean isSGEAvailable = GridEngineUtil.isAvailable();
			
			if(false == isSGEAvailable) {
				log.warn("Grid engine unavailable, building job scripts and skipping SGE execution.");
			}
			
			for(JobPair pair : job) {
				// Write the script that will run this individual pair
				String scriptPath = JobManager.writeJobScript(jobTemplate, job, pair);
				
				if(true == isSGEAvailable) {				
					// Submit to the grid engine
					int sgeId = JobManager.submitScript(scriptPath);
					Jobs.updateGridEngineId(pair.getId(), sgeId);
					Jobs.setPairStatus(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
				}
			}
			
			log.info(String.format("Successfully submitted and recorded job #%d with %d pairs by user %d", job.getId(), job.getJobPairs().size(), job.getUserId()));
			return true;
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return false;
	}	
	
	/**
	 * Takes in a job script and submits it to the grid engine
	 * @param scriptPath The absolute path to the script
	 * @return The grid engine id of the submitted job
	 */
	private synchronized static int submitScript(String scriptPath) throws Exception {
		// Get a new grid engine session
        Session session = SessionFactory.getFactory().getSession();
		JobTemplate sgeTemplate = null;		
		
		// Initialize session
        session.init("");

        // Set up the grid engine template
        sgeTemplate = session.createJobTemplate();
        
        // DRMAA needs to be told to expect a shell script and not a binary
        sgeTemplate.setNativeSpecification("-shell y -b n");
        
        // Tell the job where it will deal with files
        sgeTemplate.setWorkingDirectory(R.NODE_WORKING_DIR);
        
        // Tell where the starexec log for the job should be placed (semicolon is required by SGE)
        sgeTemplate.setOutputPath(":" + R.JOB_LOG_DIR + "/");

        // Tell the job where the script to be executed is
        sgeTemplate.setRemoteCommand(scriptPath);

        // Actually submit the job to the grid engine
        String id = session.runJob(sgeTemplate);
        log.info(String.format("Job #%s (\"%s\") has been submitted to the grid engine.", id, scriptPath));
               
        // Cleanup
        session.deleteJobTemplate(sgeTemplate);
        session.exit();

        return Integer.parseInt(id);
	}
	
	/**
	 * Creates a new job script file based on the given job and job pair.
	 * @param template The template to base the new script off of
	 * @param job The job to tailor the script for
	 * @param pair The jbo pair to tailor the script for
	 * @return The absolute path to the newly written script
	 */
	private static String writeJobScript(String template, Job job, JobPair pair) throws Exception {
		String jobScript = template;		
		jobScript = jobScript.replace("$$SOLVER_PATH$$", pair.getSolver().getPath());
		jobScript = jobScript.replace("$$SOLVER_NAME$$", pair.getSolver().getName());
		jobScript = jobScript.replace("$$CONFIG$$", pair.getSolver().getConfigurations().get(0).getName());
		jobScript = jobScript.replace("$$BENCH$$", pair.getBench().getPath());
		jobScript = jobScript.replace("$$PAIRID$$", "" + pair.getId());		
		
		String scriptPath = String.format("%s/%s", R.JOB_INBOX_DIR, String.format(R.JOBFILE_FORMAT, pair.getId()));
		File f = new File(scriptPath);
		
		f.delete();				
		f.createNewFile();
		
		if(!f.setExecutable(true)) {
			log.error("Can't change owner's executable permissions on file " + scriptPath);
			return "";
		}
		
		FileWriter out = new FileWriter(f);
		out.write(jobScript);
		out.close();
		
		return scriptPath;
	}	
	
	/**
	 * Creates a new job object with all the information required to submit a new job to the grid engine
	 * @param userId The id of the user who created the job
	 * @param name The job's user defined name
	 * @param description The job's user defined description
	 * @param preProcessorId The id of the pre-processor to use for this job (-1 for none)
	 * @param postProcessorId The id of the post-processor to use for this job (-1 for none)
	 * @param queueId The id of the queue this job should run on
	 * @param benchmarkIds A list of benchmarks to use in this job
	 * @param solverIds A list of solvers to use in this job
	 * @param configIds A list of configurations (that match in order with solvers) to use for the specified solvers
	 * @return
	 */
	public static Job buildJob(int userId, String name, String description, int preProcessorId, int postProcessorId, int queueId, List<Integer> benchmarkIds, List<Integer> solverIds, List<Integer> configIds) {
		// Create a new job object
		Job j = new Job();
		
		// Set the job's name, submitter user id and description
		j.setName(name);
		j.setUserId(userId);
	
		if(description != null) {
			j.setDescription(description);
		}
		
		// Get queue and processor information from the database and put it in the job
		j.setQueue(Queues.get(queueId));
		
		if(preProcessorId > 0) {
			j.setPreProcessor(Processors.get(preProcessorId));
		}		
		if(postProcessorId > 0) {
			j.setPostProcessor(Processors.get(postProcessorId));		
		}
		
		// Retrieve all the benchmarks included in this job
		List<Benchmark> benchmarks = Benchmarks.get(benchmarkIds);

		// Retrieve all the solvers included in this job
		// IMPORTANT: Solver IDs and Config IDs need to pair up in order (which is how they should be passed to the server)
		List<Solver> solvers = Solvers.getWithConfig(solverIds, configIds);
		
		// Pair up the solvers and benchmarks
		for(Benchmark bench : benchmarks){
			for(Solver solver : solvers) {
				JobPair pair = new JobPair();
				pair.setBench(bench);
				pair.setSolver(solver);
				j.addJobPair(pair);
			}
		}
		
		// Finally hand back the job
		return j;
	}

	@Deprecated
	public static void killJob(int sgeJobId) {
		// TODO in the future, implement this functionality
        //session.control("" + sgeJobId, Session.TERMINATE);
	}
}
