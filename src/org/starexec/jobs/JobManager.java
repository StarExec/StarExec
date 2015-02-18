package org.starexec.jobs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Common;
import org.starexec.data.database.JobPairs;
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
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.util.Util;


/**
 * Handles all SGE interactions for job submission and maintenance
 * @author Tyler Jensen
 */
public abstract class JobManager {
	private static final Logger log = Logger.getLogger(JobManager.class);

	private static String mainTemplate = null; // initialized below

	private static Session session = null; // used in submitScript() below.

	/** Initialize the GridEngine session. This must be done before calling checkPendingJobs(). 
	 * @author Aaron Stump
	 */
	public static void setSession(Session _session) {
		session = _session;
	}

    public synchronized static boolean checkPendingJobs(){
    	try {
    		log.debug("about to check if the system is paused");
	    if (Jobs.isSystemPaused()) { 
    	    	log.info("Not adding more job pairs to any queues, as the system is paused");
    	    	return false;
    	}
	    Common.logConnectionsOpen();
	    log.debug("about to get all queues");
	    
	    List<Queue> queues = Queues.getAll();
	    log.debug("found this many queues "+queues.size());
	    for (Queue q : queues) {
	    	log.debug("about to submit to queue "+q.getId());
		int qId = q.getId();
		String qname = q.getName();
		int nodeCount=Queues.getNodes(qId).size();
		int queueSize = Queues.getSizeOfQueue(qId);
		log.debug("trying to submit on queue "+qId+" with "+nodeCount+" nodes and "+ queueSize +" pairs");
		if (queueSize < R.NODE_MULTIPLIER * nodeCount) {
		    List<Job> joblist = Queues.getPendingJobs(qId);
		    log.debug("about to submit this many jobs "+joblist.size());
		    if (joblist.size() > 0) {
		    	
			submitJobs(joblist, q, queueSize,nodeCount);
		    }
		} else {
		    log.info("Not adding more job pairs to queue " + qname + ", which has " + queueSize + " pairs enqueued.");
		}
	    }
    	} catch (Exception e) {
	    log.error(e.getMessage(),e);
    	}
	    
	return false;
    }
    
    

	/**
	 * initialize mainTemplate, a string hold the jobscript customized for the
	 * current configuration (but not the current job or job pair), if it is
	 * not yet initialized.
	 * @author Aaron Stump
	 */
	protected static void initMainTemplateIf() {
		if (mainTemplate == null) {
			// Read in the job script template and format it for this global configuration
			File f = new File(R.CONFIG_PATH, "sge/jobscript");
			try {
				mainTemplate = FileUtils.readFileToString(f);
			} 
			catch (IOException e) {
				log.error("Error reading the jobscript at "+f,e);
			}
			mainTemplate = mainTemplate.replace("$$DB_NAME$$", R.MYSQL_DATABASE);
			mainTemplate = mainTemplate.replace("$$REPORT_HOST$$", R.REPORT_HOST);
			mainTemplate = mainTemplate.replace("$$REPORT_HOST$$", R.REPORT_HOST);
			mainTemplate = mainTemplate.replace("$$STAREXEC_DATA_DIR$$", R.STAREXEC_DATA_DIR);
			// Impose resource limits
			mainTemplate = mainTemplate.replace("$$MAX_WRITE$$", String.valueOf(R.MAX_PAIR_FILE_WRITE));	 
		}
	}

	static class SchedulingState {
		Job job;
		String jobTemplate;
		Iterator<JobPair> pairIter;

		SchedulingState(Job _job, String _jobTemplate, Iterator<JobPair> _pairIter) {
			job = _job;
			jobTemplate = _jobTemplate;
			pairIter = _pairIter;
		}
	}

	/**
	 * Submits a job to the grid engine
	 * @param joblist The list of jobs for which we will be submitted new pairs
	 * @param q The queue to submit on
	 * @param queueSize The number of job pairs enqueued in the given queue
	 * @param nodeCount The number of nodes in the given queue

	 */
	public static void submitJobs(List<Job> joblist, Queue q, int queueSize, int nodeCount) {		
		try {

			
			log.debug("submitJobs() begins");

			initMainTemplateIf();

			LinkedList<SchedulingState> schedule = new LinkedList<SchedulingState>();
			
			// add all the jobs in jobList to a SchedulingState in the schedule.
			for (Job job : joblist) {
				// jobTemplate is a version of mainTemplate customized for this job
				String jobTemplate = mainTemplate.replace("$$QUEUE$$", q.getName());			
				jobTemplate = jobTemplate.replace("$$JOBID$$", "" + job.getId());
				jobTemplate = jobTemplate.replace("$$RANDSEED$$",""+job.getSeed());
				jobTemplate = jobTemplate.replace("$$USERID$$", "" + job.getUserId());
				
				//Post processor
				Processor processor = job.getPostProcessor();
				if (processor == null) {
					log.debug("Postprocessor is null.");
					jobTemplate = jobTemplate.replace("$$POST_PROCESSOR_PATH$$", "");
				}
				else {
					String path = processor.getFilePath();
					log.debug("Postprocessor path is "+path+".");
					jobTemplate = jobTemplate.replace("$$POST_PROCESSOR_PATH$$", path);
				}
				
				//pre processor
				processor = job.getPreProcessor();
				if (processor == null) {
					log.debug("Preprocessor is null.");
					jobTemplate = jobTemplate.replace("$$PRE_PROCESSOR_PATH$$", "");
				}
				else {
					String path = processor.getFilePath();
					log.debug("Preprocessor path is "+path+".");
					jobTemplate = jobTemplate.replace("$$PRE_PROCESSOR_PATH$$", path);
				}

				int limit=Math.max(R.NUM_JOB_PAIRS_AT_A_TIME, ((nodeCount*R.NODE_MULTIPLIER)-queueSize)/joblist.size() + 1);
				Iterator<JobPair> pairIter = Jobs.getPendingPairsDetailed(job.getId(),limit).iterator();

				SchedulingState s = new SchedulingState(job,jobTemplate,pairIter);

				schedule.add(s);
			}

			log.info("Beginning scheduling of "+schedule.size()+" jobs on queue "+q.getName());

			/*
			 * we are going to loop through the schedule adding a few job
			 * pairs at a time to SGE.  If the count of jobs enqueued
			 * (starting from how many jobs we though we had enqueued when
			 * this method was called) exceeds the threshold R.NUM_JOB_SCRIPTS,
			 * then we will not continue with our next pass through the
			 * schedule.  
			 *
			 */

			
			HashMap<Integer,Integer> usersToPairCounts=new HashMap<Integer,Integer>();

			int count = queueSize;
			
			//transient database errors can cause us to loop forever here, and we need to make sure that does not happen
			int maxLoops=300;
			int curLoops=0;
			while (!schedule.isEmpty()) {
				curLoops++;
				if (count >= R.NODE_MULTIPLIER * nodeCount) {
					break; // out of while (!schedule.isEmpty())

				}
				if (curLoops>maxLoops) {
					log.warn("forcibly breaking out of JobManager.submitJobs()-- max loops exceeded");
					break;
				}

				Iterator<SchedulingState> it = schedule.iterator();
				
				//add all of the users that still have pending entries to the list of users
				usersToPairCounts=new HashMap<Integer,Integer>();
				while (it.hasNext()) {
					SchedulingState s = it.next();
					int userId=s.job.getUserId();
					usersToPairCounts.put(userId,0);
				}
				for (Integer uid : usersToPairCounts.keySet()) {
					usersToPairCounts.put(uid,Queues.getSizeOfQueue(q.getId(),uid));	
				}
				it = schedule.iterator();
				int min=Collections.min(usersToPairCounts.values());
				int max=Collections.max(usersToPairCounts.values());
				
				boolean excludeUsers=((max-R.NUM_JOB_PAIRS_AT_A_TIME)>min); // will we exclude users who have too many pairs this time

				log.debug("the max pairs by user = "+max);
				log.debug("the min pairs by user = "+min);
				while (it.hasNext()) {
					SchedulingState s = it.next();

					if (!s.pairIter.hasNext()) {
						// we will remove this SchedulingState from the schedule, since it is out of job pairs
						it.remove();
						continue;
					}		
					

					log.info("About to submit "+R.NUM_JOB_PAIRS_AT_A_TIME+" pairs "
							+"for job " + s.job.getId() 
							+ ", queue = "+q.getName() 
							+ ", user = "+s.job.getUserId());

					int i = 0;
					
					if (excludeUsers) {
						
						int curCount=usersToPairCounts.get(s.job.getUserId());
						//skip if this user has many more pairs than some other user
						if (curCount>(max-R.NUM_JOB_PAIRS_AT_A_TIME)) {
							log.debug("excluding user with the following id from submitting more pairs "+s.job.getUserId());
							continue;
						}
					}
					
					while (i < R.NUM_JOB_PAIRS_AT_A_TIME && s.pairIter.hasNext()) {


						JobPair pair = s.pairIter.next();
						if (pair.getPrimarySolver()==null || pair.getBench()==null) {
							// if the solver or benchmark is null, they were deleted. Indicate that the pair's
							//submission failed and move on
							JobPairs.UpdateStatus(pair.getId(), Status.StatusCode.ERROR_SUBMIT_FAIL.getVal());
							continue;
						}
						i++;
						log.debug("About to submit pair " + pair.getId());

						try {
							// Write the script that will run this individual pair				
							String scriptPath = JobManager.writeJobScript(s.jobTemplate, s.job, pair);

							

							String logPath=JobPairs.getLogFilePath(pair);
							File file=new File(logPath);
							file.getParentFile().mkdirs();
			
							if (file.exists()) {
							    log.info("Deleting old log file for " + pair.getId());
							    file.delete();
							}

							// do this first, before we submit to grid engine, to avoid race conditions
							JobPairs.setPairStatus(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
							JobPairs.setQueueSubTime(pair.getId());
							// Submit to the grid engine
							int execId = R.BACKEND.submitScript(R.SGE_ROOT,scriptPath, "/export/starexec/sandbox",logPath);
							int errorCode = StatusCode.ERROR_SGE_REJECT.getVal();

							//TODO : need a better way to handle error codes
							if(!R.BACKEND.isError(R.SGE_ROOT,execId)){
							    //TODO : remember to change name of update gridEngineId to update execId or something similar
							    JobPairs.updateGridEngineId(pair.getId(),execId);
							} else{
							    JobPairs.setPairStatus(pair.getId(),errorCode);
							}

							count++;
						} catch(Exception e) {
							log.error("submitJobs() received exception " + e.getMessage(), e);
						}
					}	
				} // end iterating once through the schedule
			} // end looping until schedule is empty or we have submitted enough job pairs

		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
	} // end submitJobs()


	/**
	 * Takes in a job script and submits it to the grid engine
	 * @param the current SGE Session (which we will not release)
	 * @param scriptPath The absolute path to the script
	 * @param pair The pair the script is being submitted for
	 * @return The grid engine id of the submitted job. -1 if the submit failed
	 * @throws Exception 
	 */
	private synchronized static int submitScript(String scriptPath, JobPair pair) throws Exception {
		JobTemplate sgeTemplate = null;

		try {
			sgeTemplate = null;		

			// Set up the grid engine template
			sgeTemplate = session.createJobTemplate();
			//log.debug("submitScript - Create Job Template for  " + pair.getId());

			// DRMAA needs to be told to expect a shell script and not a binary
			sgeTemplate.setNativeSpecification("-shell y -b n -w n");
			//log.debug("submitScript - Set Native Specification for  " + pair.getId());

			// Tell the job where it will deal with files
			//TODO: This is not correct-- what should we do about this line? It does not seem to break anything
			sgeTemplate.setWorkingDirectory("/export/starexec/sandbox");
			//log.debug("submitScript - Set Working Directory for  " + pair.getId());

			// Tell where the starexec log for the job should be placed (semicolon is required by SGE)
			
			String logPath=JobPairs.getLogFilePath(pair);
			File file=new File(logPath);
			file.getParentFile().mkdirs();
			
			if (file.exists()) {
			    log.info("Deleting old log file for " + pair.getId());
			    file.delete();
			}

			sgeTemplate.setOutputPath(":" + logPath);
			//log.debug("submitScript - Set Output Path for  " + pair.getId());

			// Tell the job where the script to be executed is
			sgeTemplate.setRemoteCommand(scriptPath);	        
			//log.debug("submitScript - Set Remote Command for  " + pair.getId());

			// Actually submit the job to the grid engine
			String id = session.runJob(sgeTemplate);
			log.info(String.format("Submitted SGE job #%s, job pair %s, script \"%s\".", id, pair.getId(), scriptPath)); 

			return Integer.parseInt(id);
		} catch (org.ggf.drmaa.DrmaaException drme) {
			log.warn("script Path = " + scriptPath);
			//log.warn("sgeTemplate = " +sgeTemplate.toString());
			JobPairs.setPairStatus(pair.getId(), StatusCode.ERROR_SGE_REJECT.getVal());			
			log.error("submitScript says " + drme.getMessage(), drme);
		} catch (Exception e) {
			JobPairs.setPairStatus(pair.getId(), StatusCode.ERROR_SUBMIT_FAIL.getVal());
			log.error(e.getMessage(), e);
		} finally {
			// Cleanup. Session's MUST be exited or SGE will be mean to you
			if(sgeTemplate != null) {
				session.deleteJobTemplate(sgeTemplate);
			}

		}

		return -1;
	}

    protected static String base64encode(String s) {
	return new String(Base64.encodeBase64(s.getBytes()));
    }

	/**
	 * Creates a new job script file based on the given job and job pair.
	 * @param template The template to base the new script off of
	 * @param job The job to tailor the script for
	 * @param pair The job pair to tailor the script for
	 * @return The absolute path to the newly written script
	 */
	private static String writeJobScript(String template, Job job, JobPair pair) throws Exception {
		String jobScript = template;		

		// General pair configuration
		jobScript = jobScript.replace("$$SOLVER_PATH$$", base64encode(pair.getPrimarySolver().getPath()));
		jobScript = jobScript.replace("$$SOLVER_ID$$",String.valueOf(pair.getPrimarySolver().getId()));
		jobScript = jobScript.replace("$$SOLVER_TIMESTAMP$$", pair.getPrimarySolver().getMostRecentUpdate());
		jobScript = jobScript.replace("$$SOLVER_NAME$$", base64encode(pair.getPrimarySolver().getName()));
		jobScript = jobScript.replace("$$CONFIG$$", pair.getPrimarySolver().getConfigurations().get(0).getName());
		jobScript = jobScript.replace("$$BENCH$$", base64encode(pair.getBench().getPath()));
		jobScript = jobScript.replace("$$PAIRID$$", "" + pair.getId());	
		jobScript = jobScript.replace("$$SPACE_PATH$$", pair.getPath());
		File outputFile=new File(JobPairs.getFilePath(pair));
		
		jobScript = jobScript.replace("$$PAIR_OUTPUT_DIRECTORY$$", base64encode(outputFile.getParentFile().getAbsolutePath()));

		jobScript = jobScript.replace("$$PAIR_OUTPUT_PATH$$", base64encode(outputFile.getAbsolutePath()));
		//Dependencies
		if (Benchmarks.getBenchDependencies(pair.getBench().getId()).size() > 0)
		{
			jobScript = jobScript.replace("$$HAS_DEPENDS$$", "1");
			writeDependencyFile(pair.getId(), pair.getBench().getId());
		}
		else{
			jobScript = jobScript.replace("$$HAS_DEPENDS$$", "0");
		}
		// Resource limits
		jobScript = jobScript.replace("$$MAX_RUNTIME$$", "" + Util.clamp(1, R.MAX_PAIR_RUNTIME, job.getWallclockTimeout())); 
		jobScript = jobScript.replace("$$MAX_CPUTIME$$", "" + Util.clamp(1, R.MAX_PAIR_CPUTIME, job.getCpuTimeout()));		
		log.debug("the current job pair has a memory = "+job.getMaxMemory());
		jobScript = jobScript.replace("$$MAX_MEM$$",""+Util.bytesToMegabytes(job.getMaxMemory()));
		log.debug("The jobscript is: "+jobScript);

		String scriptPath = String.format("%s/%s", R.JOB_INBOX_DIR, String.format(R.JOBFILE_FORMAT, pair.getId()));
		jobScript = jobScript.replace("$$SCRIPT_PATH$$",scriptPath);
		File f = new File(scriptPath);

		f.delete();		
		f.getParentFile().mkdirs();
		f.createNewFile();

		if(!f.setExecutable(true, false) || !f.setReadable(true, false)) {
			log.error("Can't change owner permissions on jobscript file. This will prevent the grid engine from being able to open the file. Script path: " + scriptPath);
			return "";
		}
		//log.debug("jobScript = " + jobScript);
		
		FileWriter out = new FileWriter(f);
		out.write(jobScript);
		out.close();
		
		return scriptPath;
	}	

	public static Boolean writeDependencyFile(Integer pairId, Integer benchId) throws Exception{		
		List<BenchmarkDependency> dependencies = Benchmarks.getBenchDependencies(benchId);
		StringBuilder sb = new StringBuilder();
		String separator = ",,,";
		for (BenchmarkDependency bd:dependencies)
		{
			sb.append(bd.getSecondaryBench().getPath());
			sb.append(separator);		
			sb.append(bd.getDependencyPath());
			sb.append("\n");
		}

		String dependFilePath = String.format("%s/%s", R.JOB_INBOX_DIR, String.format(R.DEPENDFILE_FORMAT, pairId));
		File f = new File(dependFilePath);
		f.createNewFile();

		if(!f.setExecutable(true, false) || !f.setReadable(true, false)) {
			log.error("Can't change owner permissions on job dependencies file. This will prevent the grid engine from being able to open the file. File path: " + dependFilePath);
			return false;
		}
		log.debug("dependencies file = " + sb.toString());
		FileWriter out = new FileWriter(f);
		out.write(sb.toString());
		out.close();
		log.debug("done writing dependency file");
		return true;
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
		log.info("Number of dependencies = " + dependencies.size());
		for (BenchmarkDependency bd:dependencies)
		{
			//spaces in the paths not allowed
			String path = (local) ? bd.getSecondaryBench().getPath().replaceAll("\\s",""):bd.getDependencyPath().replaceAll("\\s","");
			arrayString = arrayString + "" + path + " ";
		}
		arrayString = arrayString.trim() + "\"";
		log.info(arrayString);
		log.info("Array String Length for " + bench.getName() + " is " + arrayString.length());
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
	 * @param randomSeed a seed to pass into preprocessors
	 * @return the new job object with the specified properties
	 */
	public static Job setupJob(int userId, String name, String description, int preProcessorId, int postProcessorId, int queueId, long randomSeed) {
		log.debug("Setting up job " + name);
		Job j = new Job();

		// Set the job's name, submitter user id and description
		j.setUserId(userId);
		j.setName(name);		
		j.setSeed(randomSeed);
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
		log.debug("Successfully set up job " + name);
		return j;	
	}

	/**
	 * Adds to a job object the job pairs given by the selection we made (this will build it from the "choose"
	 * selection on job creation)
	 * 
	 * @param j the job to add job pairs to
	 * @param userId The ID of the user creating this job
	 * @param benchmarkIds A list of benchmarks to use in this job
	 * @param solverIds A list of solvers to use in this job
	 * @param configIds A list of configurations (that match in order with solvers) to use for the specified solvers
	 * @param spaceId the id of the space we are adding from
	 * @param SP A mapping of space IDs to space paths for every space in this job, with paths being relative to the space this job is
	 * being created in. If null, the job will be flat, with every job pair in a single top level job space
	 */
	public static void buildJob(Job j, List<Integer> benchmarkIds, List<Integer> configIds, Integer spaceId, HashMap<Integer, String> SP) {
		// Retrieve all the benchmarks included in this job
		List<Benchmark> benchmarks = Benchmarks.get(benchmarkIds);

		// Retrieve all the solvers included in this job
		List<Solver> solvers = Solvers.getWithConfig(configIds);
		String spaceName="job space";
		String sm=Spaces.getName(spaceId);
		if (sm!=null) {
			spaceName=sm;
		}
		// Pair up the solvers and benchmarks
		for(Benchmark bench : benchmarks){
			for(Solver solver : solvers) {
				JobPair pair = new JobPair();
				pair.setBench(bench);
				JoblineStage stage=new JoblineStage();
				stage.setPrimary(true);

				stage.setSolver(solver);
				stage.setConfiguration(solver.getConfigurations().get(0));
				pair.addStage(stage);
				
				
				pair.setSpace(Spaces.get(spaceId));
				if (SP!=null) {
					pair.setPath(SP.get(spaceId));
				} else {
					pair.setPath(spaceName);
				}
				j.addJobPair(pair);
				
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
	 * @param spaceId the id of the space to build the job pairs from
	 * @param path The space path to give to every job pair created by this function
	 * @return an error message if there was a problem, and null otherwise.
	 */
	public static List<JobPair> addJobPairsFromSpace(int userId, int spaceId, String path) {
		Space space = Spaces.get(spaceId);
		log.debug("calling addJobPairsFrom space on space ID = "+spaceId);
		List<JobPair> pairs=new ArrayList<JobPair>();
		// Get the benchmarks and solvers from this space
		List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);
		log.debug("found this many benchmarks in the space = "+benchmarks.size());
		List<Solver> solvers = Solvers.getBySpace(spaceId);
		
		List<Configuration> configs;
		JobPair pair;
		for (Benchmark b : benchmarks) {
			for (Solver s : solvers) {
				// Get the configurations for the current solver
				configs = Solvers.getConfigsForSolver(s.getId());
				if (configs.size() == 0) {
					continue;
					//we shouldn't be failing on this-- we should just continue on with the other solvers
					//return null;
				}
				   
				for (Configuration c : configs) {

				    log.debug("configuration = " + c.getName());
					Solver clone = JobManager.cloneSolver(s);
					// Now we're going to work with this solver with this configuration
					clone.addConfiguration(c);

					pair = new JobPair();
					pair.setBench(b);
					JoblineStage stage=new JoblineStage();
					stage.setPrimary(true);

					stage.setSolver(clone);
					stage.setConfiguration(c);
					pair.addStage(stage);
					
					
					pair.setSpace(space);
					//we are running pairs in a single space, so the path is flat
					pair.setPath(path);
					log.debug("pair path = " + pair.getPath());
					pairs.add(pair);

					
				}
			}
		}
		return pairs;
	}
	
	/**
	 * With the given solvers and configurations, will find all benchmarks in the current space hierarchy
	 * and create job pairs from the result. Will then return those job pairs
	 * 
	 * @param spaceId the id of the space we start in
	 * @param userId the id of the user creating the job
	 * @param solverIds a list of solvers to use
	 * @param configIds a list of configurations to use
	 * @param SP A mapping from space IDs to the path of the space rooted at "spaceId"
	 * @return A HashMap that maps space IDs to all the job pairs in that space. These can then be added to a job in any
	 * desirable order
	 */
	public static List<JobPair> addJobPairsFromSpace( int userId, int spaceId, String path, List<Integer> configIds) {
		try {			
			List<Solver> solvers = Solvers.getWithConfig(configIds);
			
			List<Benchmark> benchmarks =new ArrayList<Benchmark>();
		
			
			// Pair up the solvers and benchmarks

				benchmarks = Benchmarks.getBySpace(spaceId);
				List<JobPair> curPairs=new ArrayList<JobPair>();
				for(Benchmark bench : benchmarks){
					for(Solver solver : solvers) {
						JobPair pair = new JobPair();
						pair.setBench(bench);
						JoblineStage stage=new JoblineStage();
						stage.setPrimary(true);

						stage.setSolver(solver);
						pair.addStage(stage);			
						
						pair.setPath(path);
						pair.setSpace(Spaces.get(spaceId));
						curPairs.add(pair);
						
					}
				}
			return curPairs;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;

	}

	
	
	/**
	 * Adds job pairs to a job object in a depth-first manner. All pairs from space1 are added,
	 * then all pairs from space2, and so on
	 * @param j The job object to add the job pairs to 
	 * @param spaceToPairs A mapping from spaces to lists of job pairs in that space
	 */
	public static void addJobPairsDepthFirst(Job j, HashMap<Integer, List<JobPair>> spaceToPairs) {
		for (Integer spaceId : spaceToPairs.keySet()) {
			log.debug("adding this many pairs from space id = "+spaceId+" "+spaceToPairs.get(spaceId).size());
			j.addJobPairs(spaceToPairs.get(spaceId));
		}
	}
	
	/**
	 * Adds job pairs to a job object in a breadth-first manner. One pair from space1 is added,
	 * then one pairs from space2, and so on until every pair from every space has been added.
	 * @param j The job object to add the job pairs to
	 * @param spaceToPairs A mapping from spaces to lists of job pairs in that space
	 */
	public static void addJobPairsRoundRobin(Job j, HashMap<Integer, List<JobPair>> spaceToPairs) {
		try {
			int index=0;
			while (spaceToPairs.size()>0) {
				Set<Integer> keys=spaceToPairs.keySet();
				Set<Integer> keysToRemove=new HashSet<Integer>();
				for (Integer spaceId : keys) {
					//if there is at least one pair left in this space
					if (spaceToPairs.get(spaceId).size()>index) {
						log.debug("adding a round robin job pair");
						j.addJobPair(spaceToPairs.get(spaceId).get(index));
					} else {
						//otherwise, the space is done, and we should remove it from the hashmap of spaces
						keysToRemove.add(spaceId);
					}
				}
				for (Integer i : keysToRemove) {
					keys.remove(i);
				}
				index++;
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
	}
	
	
	/**
	 * With the given solvers and configurations, will find all benchmarks in the current space hierarchy
	 * and create job pairs from the result. Will then return those job pairs
	 * 
	 * @param spaceId the id of the space we start in
	 * @param userId the id of the user creating the job
	 * @param solverIds a list of solvers to use
	 * @param configIds a list of configurations to use
	 * @param SP A mapping from space IDs to the path of the space rooted at "spaceId"
	 * @return A HashMap that maps space IDs to all the job pairs in that space. These can then be added to a job in any
	 * desirable order
	 */
	public static HashMap<Integer,List<JobPair>> addBenchmarksFromHierarchy(int spaceId, int userId, List<Integer> configIds, HashMap<Integer, String> SP) {
		try {
			HashMap<Integer,List<JobPair>> spaceToPairs=new HashMap<Integer,List<JobPair>>();
			
			List<Solver> solvers = Solvers.getWithConfig(configIds);
			
			List<Benchmark> benchmarks =new ArrayList<Benchmark>();
			List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(spaceId, userId));
			spaces.add(Spaces.get(spaceId));
		
			
			// Pair up the solvers and benchmarks

			for (Space s : spaces) {
				benchmarks = Benchmarks.getBySpace(s.getId());
				log.debug("found this many benchmarks for space id = "+s.getId()+" "+benchmarks.size());
				List<JobPair> curPairs=new ArrayList<JobPair>();
				for(Benchmark bench : benchmarks){
					for(Solver solver : solvers) {
						JobPair pair = new JobPair();
						pair.setBench(bench);
						JoblineStage stage=new JoblineStage();
						stage.setSolver(solver);
						stage.setPrimary(true);
						pair.addStage(stage);				
						
						pair.setPath(SP.get(s.getId()));
						pair.setSpace(Spaces.get(s.getId()));
						curPairs.add(pair);
						
					}
				}
				spaceToPairs.put(s.getId(), curPairs);

			}
			
			return spaceToPairs;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;

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

}
