package org.starexec.jobs;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Util;

/**
 * Handles all SGE interactions for job submission and maintenance
 * @author Tyler Jensen
 */
public abstract class JobManager {
	private static final Logger log = Logger.getLogger(JobManager.class);


	public static boolean checkPendingJobs(){
		List<Job> jobs = Jobs.getPendingJobs();
		for (Job job : jobs){
			submitJob(job);
		}
		return false;
	}

	/**
	 * Submits a job to the grid engine
	 * @param j The job object containing information about what to run for the job
	 * @param spaceId The id of the space this job will be placed in
	 */
	public static boolean submitJob(Job job) {		
		try {
			/*
			// Attempt to add the job to the database			
			boolean jobAdded = Jobs.add(job, spaceId);

			// If for some reason that failed, don't run on the grid engine
			if(false == jobAdded) {
				log.error(String.format("Job failed to be added to the database and was prevented from running on the grid [user=%d] [space=%d]", job.getUserId(), spaceId));
				return false;
			}
			 */
			log.info("submitting pairs for job " + job.getId());
			// Read in the job script template and format it for all the pairs in this job
			String jobTemplate = FileUtils.readFileToString(new File(R.CONFIG_PATH, "sge/jobscript"));

			// General job setup
			jobTemplate = jobTemplate.replace("$$QUEUE$$", job.getQueue().getName());			
			jobTemplate = jobTemplate.replace("$$JOBID$$", "" + job.getId());
			jobTemplate = jobTemplate.replace("$$DB_NAME$$", "" + R.MYSQL_DATABASE);
			jobTemplate = jobTemplate.replace("$$USERID$$", "" + job.getUserId());
			jobTemplate = jobTemplate.replace("$$OUT_DIR$$", "" + R.NODE_OUTPUT_DIR);
			jobTemplate = jobTemplate.replace("$$REPORT_HOST$$", "" + R.REPORT_HOST);
			// Impose resource limits
			jobTemplate = jobTemplate.replace("$$MAX_MEM$$", "" + R.MAX_PAIR_VMEM);			
			jobTemplate = jobTemplate.replace("$$MAX_WRITE$$", "" + R.MAX_PAIR_FILE_WRITE);							

			// Optimization, do outside of loop
			boolean isSGEAvailable = GridEngineUtil.isAvailable();

			if(false == isSGEAvailable) {
				log.warn("Grid engine unavailable -skipping SGE execution.");
				return false;
			}
			int count = R.NUM_JOB_SCRIPTS;
			//TODO - method to get only the needed pairs
			List<JobPair> pairs = Jobs.getPairsDetailed(job.getId());
			for(JobPair pair : pairs) {
				if (pair.getStatus().equals(StatusCode.STATUS_PENDING_SUBMIT) || pair.getStatus().equals(StatusCode.ERROR_SGE_REJECT)){
					// Write the script that will run this individual pair				
					String scriptPath = JobManager.writeJobScript(jobTemplate, job, pair);

					if(true == isSGEAvailable) {				
						// Submit to the grid engine
						int sgeId = JobManager.submitScript(scriptPath, pair);

						// If the submission was successful
						if(sgeId >= 0) {											
							Jobs.updateGridEngineId(pair.getId(), sgeId);
							Jobs.setPairStatus(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
						}
					}
					count--;
				}
				if (count < 1){
					break;
				}
			}
			log.info(String.format("Successfully submitted and recorded job #%d with %d pairs by user %d", job.getId(), R.NUM_JOB_SCRIPTS-count, job.getUserId()));
			return true;
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}

		return false;
	}	

	/**
	 * Submits a job to the grid engine and records it in the database - used with quick job
	 * @param j The job object containing information about what to run for the job
	 * @param spaceId The id of the space this job will be placed in
	 * @author Benton McCune
	 */
	public static int submitJobReturnId(Job job, int spaceId) {		
		try {
			// Attempt to add the job to the database			
			boolean jobAdded = Jobs.add(job, spaceId);

			// If for some reason that failed, don't run on the grid engine
			if(false == jobAdded) {
				log.error(String.format("Job failed to be added to the database and was prevented from running on the grid [user=%d] [space=%d]", job.getUserId(), spaceId));
				return -1;
			}

			// Read in the job script template and format it for all the pairs in this job
			String jobTemplate = FileUtils.readFileToString(new File(R.CONFIG_PATH, "sge/jobscript"));

			// General job setup
			jobTemplate = jobTemplate.replace("$$QUEUE$$", job.getQueue().getName());			
			jobTemplate = jobTemplate.replace("$$JOBID$$", "" + job.getId());
			jobTemplate = jobTemplate.replace("$$DB_NAME$$", "" + R.MYSQL_DATABASE);
			jobTemplate = jobTemplate.replace("$$USERID$$", "" + job.getUserId());
			jobTemplate = jobTemplate.replace("$$OUT_DIR$$", "" + R.NODE_OUTPUT_DIR);
			jobTemplate = jobTemplate.replace("$$REPORT_HOST$$", "" + R.REPORT_HOST);

			// Impose resource limits
			jobTemplate = jobTemplate.replace("$$MAX_MEM$$", "" + R.MAX_PAIR_VMEM);			
			jobTemplate = jobTemplate.replace("$$MAX_WRITE$$", "" + R.MAX_PAIR_FILE_WRITE);							

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
					int sgeId = JobManager.submitScript(scriptPath, pair);

					// If the submission was successful
					if(sgeId >= 0) {											
						Jobs.updateGridEngineId(pair.getId(), sgeId);
						Jobs.setPairStatus(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
					}
				}
			}

			log.info(String.format("Successfully submitted and recorded job #%d with %d pairs by user %d", job.getId(), job.getJobPairs().size(), job.getUserId()));
			return job.getId();
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}

		return -1;
	}

	/**
	 * Takes in a job script and submits it to the grid engine
	 * @param scriptPath The absolute path to the script
	 * @param pair The pair the script is being submitted for
	 * @return The grid engine id of the submitted job. -1 if the submit failed
	 */
	private synchronized static int submitScript(String scriptPath, JobPair pair) throws Exception {
		Session session = null;
		JobTemplate sgeTemplate = null;

		try {
			// Get a new grid engine session
			session = SessionFactory.getFactory().getSession();
			sgeTemplate = null;		

			// Initialize session
			session.init("");
			log.info("submitScript - Session Initialized for Job Pair " + pair.getId());
			// Set up the grid engine template
			sgeTemplate = session.createJobTemplate();
			log.info("submitScript - Create Job Template for  " + pair.getId());
			// DRMAA needs to be told to expect a shell script and not a binary
			sgeTemplate.setNativeSpecification("-shell y -b n");
			log.info("submitScript - Set Native Specification for  " + pair.getId());
			// Tell the job where it will deal with files
			sgeTemplate.setWorkingDirectory(R.NODE_WORKING_DIR);
			log.info("submitScript - Set Working Directory for  " + pair.getId());
			// Tell where the starexec log for the job should be placed (semicolon is required by SGE)
			sgeTemplate.setOutputPath(":" + R.JOB_LOG_DIR);
			log.info("submitScript - Set Output Path for  " + pair.getId());
			// Tell the job where the script to be executed is
			sgeTemplate.setRemoteCommand(scriptPath);	        
			log.info("submitScript - Set Remote Command for  " + pair.getId());
			// Actually submit the job to the grid engine
			String id = session.runJob(sgeTemplate);
			log.info(String.format("Job #%s (\"%s\") has been submitted to the grid engine.", id, scriptPath));	               	       	        

			return Integer.parseInt(id);
		} catch (org.ggf.drmaa.DrmaaException drme) {
			log.warn("script Path = " + scriptPath);
			//log.warn("sgeTemplate = " +sgeTemplate.toString());
			Jobs.setPairStatus(pair.getId(), StatusCode.ERROR_SGE_REJECT.getVal());			
			log.error("submitScript says " + drme.getMessage(), drme);
			//get status of queues for hints about why it was rejected
			//Util.executeCommand(R.QUEUE_STATS_COMMAND);
			//log.warn("Current queue status = " + Util))
		} catch (Exception e) {
			Jobs.setPairStatus(pair.getId(), StatusCode.ERROR_SUBMIT_FAIL.getVal());
			log.error(e.getMessage(), e);
		} finally {
			// Cleanup. Session's MUST be exited or SGE will be mean to you
			if(sgeTemplate != null) {
				session.deleteJobTemplate(sgeTemplate);
			}

			if(session != null) {
				session.exit();
			}
		}

		return -1;
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

		// General pair configuration
		jobScript = jobScript.replace("$$SOLVER_PATH$$", pair.getSolver().getPath());
		jobScript = jobScript.replace("$$SOLVER_NAME$$", pair.getSolver().getName());
		jobScript = jobScript.replace("$$CONFIG$$", pair.getSolver().getConfigurations().get(0).getName());
		jobScript = jobScript.replace("$$BENCH$$", pair.getBench().getPath());
		jobScript = jobScript.replace("$$PAIRID$$", "" + pair.getId());		
		//Dependencies
		jobScript = jobScript.replace("$$BENCH_DEPENDS$$", writeDependencyArray(pair.getBench(), true));	
		jobScript = jobScript.replace("$$LOCAL_DEPENDS$$", writeDependencyArray(pair.getBench(), false));	
		// Resource limits
		jobScript = jobScript.replace("$$MAX_RUNTIME$$", "" + Util.clamp(1, R.MAX_PAIR_RUNTIME, pair.getWallclockTimeout()));		
		jobScript = jobScript.replace("$$MAX_CPUTIME$$", "" + Util.clamp(1, R.MAX_PAIR_CPUTIME, pair.getCpuTimeout()));		

		String scriptPath = String.format("%s/%s", R.JOB_INBOX_DIR, String.format(R.JOBFILE_FORMAT, pair.getId()));
		File f = new File(scriptPath);

		f.delete();				
		f.createNewFile();

		if(!f.setExecutable(true, false) || !f.setReadable(true, false)) {
			log.error("Can't change owner permissions on jobscript file. This will prevent the grid engine from being able to open the file. Script path: " + scriptPath);
			return "";
		}

		FileWriter out = new FileWriter(f);
		out.write(jobScript);
		out.close();
		return scriptPath;
	}	


	/**
	 * Creates an array for the bash script.  This array will consist of all the paths for the copies of the secondary 
	 * benchmarks on the execution host or the paths of the secondary benchmarks on the starexec system depending on the local Boolean paramter. 
	 * Will return "" if there are no dependencies.
	 * @author Benton McCune
	 * @param bench the bench that possibly has dependencies
	 * @param local TRUE for execution host paths, FALSE for paths to benchmarks on starexec
	 * @return arrayString a String that will be an array within a bash script
	 */
	public static String writeDependencyArray(Benchmark bench, Boolean local)
	{
		String arrayString ="\"";
		List<BenchmarkDependency> dependencies = Benchmarks.getBenchDependencies(bench.getId());
		for (BenchmarkDependency bd:dependencies)
		{
			//spaces in the paths not allowed
			String path = (local) ? bd.getSecondaryBench().getPath().replaceAll("\\s",""):bd.getDependencyPath().replaceAll("\\s","");
			arrayString = arrayString + "" + path + " ";
		}
		arrayString = arrayString.trim() + "\"";
		//log.debug(arrayString);
		//log.info("Array String Length for " + bench.getName() + " is " + arrayString.length());
		return arrayString;
	}


	/**
	 * Sets up the basic information for a job, including the user who created it,
	 * its name and description, the pre- and post-processors, and the queue.
	 * 
	 * This does NOT add any job pairs to the job.
	 * 
	 * @param userId the id of the user who created the job
	 * @param name the name of the job
	 * @param description the description of the job
	 * @param preProcessorId the id of the pre-processor for the job
	 * @param postProcessorId the id of the post-processor for the job
	 * @param queueId the id of the queue for the job
	 * @return the new job object with the specified properties
	 */
	public static Job setupJob(int userId, String name, String description, int preProcessorId, int postProcessorId, int queueId) {
		log.debug("Setting up job " + name);
		Job j = new Job();

		// Set the job's name, submitter user id and description
		j.setUserId(userId);
		j.setName(name);		

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

		return j;
	}

	/**
	 * Adds to a job object the job pairs given by the selection we made (this will build it from the "choose"
	 * selection on job creation)
	 * 
	 * @param j the job to add job pairs to
	 * @param cpuTimeout The maximum amount of cpu time this job's pairs can run (individually)
	 * @param clockTimeout The maximum amount of time (wallclock) this job's pairs can run (individually)
	 * @param benchmarkIds A list of benchmarks to use in this job
	 * @param solverIds A list of solvers to use in this job
	 * @param configIds A list of configurations (that match in order with solvers) to use for the specified solvers
	 * @param spaceId the id of the space we are adding from
	 */
	public static void buildJob(Job j, int userId, int cpuTimeout, int clockTimeout, List<Integer> benchmarkIds, List<Integer> solverIds, List<Integer> configIds, int spaceId) {
		// Retrieve all the benchmarks included in this job
		List<Benchmark> benchmarks = Benchmarks.get(benchmarkIds);

		// Retrieve all the solvers included in this job
		List<Solver> solvers = Solvers.getWithConfig(solverIds, configIds);

		// Pair up the solvers and benchmarks
		int pairCount = 0;//temporarily, we're limiting number of job pairs.
		for(Benchmark bench : benchmarks){
			for(Solver solver : solvers) {
				JobPair pair = new JobPair();
				pair.setBench(bench);
				pair.setSolver(solver);				
				pair.setCpuTimeout(cpuTimeout);
				pair.setWallclockTimeout(clockTimeout);
				pair.setSpace(Spaces.get(spaceId));
				j.addJobPair(pair);
				pairCount++;
				log.info("Pair Count = " + pairCount + ", Limit = " + R.TEMP_JOBPAIR_LIMIT);
				if (pairCount >= R.TEMP_JOBPAIR_LIMIT && (userId != 20)){//backdoor for ben to run bigger jobs
					return;
				}	
			}
		}


	}

	/**
	 * Gets all the solvers/configs and benchmarks from a space, pairs them up, and then adds the
	 * resulting job pairs to a given job object. Accessed from running the space / keep hierarchy
	 * structure in job creation.
	 * 
	 * @param j the Job to add Job Pairs to
	 * @param userId the id of the user adding the job pairs
	 * @param cpuTimeout the CPU Timeout for the job
	 * @param clockTimeout the Clock Timeout for the job 
	 * @param spaceId the id of the space to build the job pairs from
	 */
	public static void addJobPairsFromSpace(Job j, int userId, int cpuTimeout, int clockTimeout, int spaceId) {
		Space space = Spaces.get(spaceId);

		// Get the benchmarks and solvers from this space
		List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);
		List<Solver> solvers = Solvers.getBySpace(spaceId);
		List<Configuration> configs;
		JobPair pair;

		int pairCount = 0;
		for (Benchmark b : benchmarks) {
			for (Solver s : solvers) {
				// Get the configurations for the current solver
				configs = Solvers.getConfigsForSolver(s.getId());
				for (Configuration c : configs) {

					Solver clone = JobManager.cloneSolver(s);
					// Now we're going to work with this solver with this configuration
					clone.addConfiguration(c);

					pair = new JobPair();
					pair.setBench(b);
					pair.setSolver(clone);
					pair.setCpuTimeout(cpuTimeout);
					pair.setWallclockTimeout(clockTimeout);
					pair.setSpace(space);
					j.addJobPair(pair);

					pairCount++;
					log.info("Pair Count = " + pairCount + ", Limit = " + R.TEMP_JOBPAIR_LIMIT);
					if (pairCount >= R.TEMP_JOBPAIR_LIMIT && (userId != 20)){//backdoor for ben to run bigger jobs
						return;
					}
				}
			}
		}
	}

	/**
	 * With the given solvers and configurations, will find all benchmarks in the current space hierarchy
	 * and create job pairs from the result. Will then add the job pairs to the given job.
	 * 
	 * @param j the job to add the pairs to
	 * @param spaceId the id of the space we start in
	 * @param userId the id of the user creating the job
	 * @param solverIds a list of solvers to use
	 * @param configIds a list of configurations to use
	 * @param cpuTimeout the CPU timeout for the job
	 * @param clockTimeout the clock timeout for the job
	 */
	public static void addBenchmarksFromHierarchy(Job j, int spaceId, int userId, List<Integer> solverIds, List<Integer> configIds, int cpuTimeout, int clockTimeout) {
		List<Solver> solvers = Solvers.getWithConfig(solverIds, configIds);
		List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);

		// Pair up the solvers and benchmarks
		int pairCount = 0;//temporarily, we're limiting number of job pairs.
		for(Benchmark bench : benchmarks){
			for(Solver solver : solvers) {
				JobPair pair = new JobPair();
				pair.setBench(bench);
				pair.setSolver(solver);				
				pair.setCpuTimeout(cpuTimeout);
				pair.setWallclockTimeout(clockTimeout);
				pair.setSpace(Spaces.get(spaceId));
				j.addJobPair(pair);
				pairCount++;
				log.info("Pair Count = " + pairCount + ", Limit = " + R.TEMP_JOBPAIR_LIMIT);
				if (pairCount >= R.TEMP_JOBPAIR_LIMIT && (userId != 20)){//backdoor for ben to run bigger jobs
					return;
				}	
			}
		}

		// Now, recursively add from the subspaces 
		List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaces(spaceId, userId, true));

		int space;

		for (Space s : spaces) {
			space = s.getId();
			benchmarks = Benchmarks.getBySpace(space);
			for(Benchmark bench : benchmarks){
				for(Solver solver : solvers) {
					JobPair pair = new JobPair();
					pair.setBench(bench);
					pair.setSolver(solver);				
					pair.setCpuTimeout(cpuTimeout);
					pair.setWallclockTimeout(clockTimeout);
					pair.setSpace(Spaces.get(space));
					j.addJobPair(pair);
					pairCount++;
					log.info("Pair Count = " + pairCount + ", Limit = " + R.TEMP_JOBPAIR_LIMIT);
					if (pairCount >= R.TEMP_JOBPAIR_LIMIT && (userId != 20)){//backdoor for ben to run bigger jobs
						return;
					}	
				}
			}
		}
	}


	/**
	 * Creates a copy of a given solver; the copy will have the same id, description,
	 * name, and filepath as the original
	 * 
	 * @param s the solver to copy
	 * @return a copy of the given solver
	 */
	public static Solver cloneSolver(Solver s) {
		Solver clone = new Solver();

		clone.setId(s.getId());
		clone.setDescription(s.getDescription());
		clone.setName(s.getName());
		clone.setPath(s.getPath());

		return clone;
	}

	@Deprecated
	public static void killJob(int sgeJobId) {
		// TODO in the future, implement this functionality
		//session.control("" + sgeJobId, Session.TERMINATE);
	}

}
