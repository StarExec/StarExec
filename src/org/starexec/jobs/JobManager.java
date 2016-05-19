package org.starexec.jobs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Common;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.SolverBuildStatus.SolverBuildStatusCode;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.data.to.pipelines.StageAttributes.SaveResultsOption;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.util.Util;

/**
 * Handles all SGE interactions for job submission and maintenance
 * @author Tyler Jensen
 */
public abstract class JobManager {
	private static final Logger log = Logger.getLogger(JobManager.class);

	private static String mainTemplate = null; // initialized below

	private static HashMap<Integer, LoadBalanceMonitor> queueToMonitor = new HashMap<Integer, LoadBalanceMonitor>();
	
	/**
	 * Returns the string representation of the LoadBalanceMonitor for the given queue.
	 * @param queueId
	 * @return The string. Note that it may be slightly out of date, as it is only updated once per run of
	 * JobManager.submitJobs.
	 */
	public static String getLoadRepresentationForQueue(int queueId) {
		log.debug("retrieving load data for queue = "+queueId);
		if (queueToMonitor.containsKey(queueId)){
			return queueToMonitor.get(queueId).toString();
		}
		return null;
	}
	
	/**
	 * Completely clears all load data from memory and also from the database.
	 * This function is synchronized to prevent concurrent modification
	 * of the queueToMonitor structure between this and the SubmitJobs
	 * function.
	 */
	public synchronized static void clearLoadBalanceMonitors() {
		log.debug("Clearing out all load balancing data");
		queueToMonitor = new HashMap<Integer, LoadBalanceMonitor>();
		JobPairs.getAndClearTimeDeltas(-1);
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
		    
		    List<Queue> queues = Queues.getAllActive();
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
				    } else {
				    	// if we have no jobs to submit, that means we should set
				    	// the active users in the monitor to the empty set.
				    	// This ensures users are set to inactive correctly
				    	LoadBalanceMonitor m = queueToMonitor.get(q.getId());
				    	if (m!=null) {
				    		m.setUsers(new HashMap<Integer,Integer>());
							m.setUserLoadDataFormattedString();
				    	}
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
			mainTemplate = mainTemplate.replace("$$DB_USER$$", R.COMPUTE_NODE_MYSQL_USERNAME);
			mainTemplate = mainTemplate.replace("$$DB_PASS$$", R.COMPUTE_NODE_MYSQL_PASSWORD);
			mainTemplate = mainTemplate.replace("$$REPORT_HOST$$", R.REPORT_HOST);
			mainTemplate = mainTemplate.replace("$$STAREXEC_DATA_DIR$$", R.STAREXEC_DATA_DIR);
			// Impose resource limits
			mainTemplate = mainTemplate.replace("$$MAX_WRITE$$", String.valueOf(R.MAX_PAIR_FILE_WRITE));	
			mainTemplate = mainTemplate.replace("$$BENCH_NAME_LENGTH_MAX$$", String.valueOf(R.BENCH_NAME_LEN));
			mainTemplate = mainTemplate.replace("$$RUNSOLVER_PATH$$", R.RUNSOLVER_PATH);
			mainTemplate = mainTemplate.replace("$$SANDBOX_USER_ONE$$", R.SANDBOX_USER_ONE);
			mainTemplate = mainTemplate.replace("$$SANDBOX_USER_TWO$$", R.SANDBOX_USER_TWO);
			mainTemplate = mainTemplate.replace("$$WORKING_DIR_BASE$$", R.BACKEND_WORKING_DIR);
			mainTemplate = mainTemplate.replace("$$SCRIPT_DIR$$", R.getScriptDir());
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
	 * Gets the load balance monitor for a particular queue.
	 * @param queueId The ID of the queue to get the monitor for
	 * @return
	 */
	private static LoadBalanceMonitor getMonitor(int queueId) {
		if (!queueToMonitor.containsKey(queueId)) {
			queueToMonitor.put(queueId, new LoadBalanceMonitor());
		}
		return queueToMonitor.get(queueId);
	}

	/**
	 * Submits a job to the grid engine
	 * @param joblist The list of jobs for which we will be submitted new pairs
	 * @param q The queue to submit on
	 * @param queueSize The number of job pairs enqueued in the given queue
	 * @param nodeCount The number of nodes in the given queue

	 */
	public static void submitJobs(List<Job> joblist, Queue q, int queueSize, int nodeCount) {
		LoadBalanceMonitor monitor = getMonitor(q.getId());
		try {
			log.debug("submitJobs() begins");

			initMainTemplateIf();

			LinkedList<SchedulingState> schedule = new LinkedList<SchedulingState>();
			// add all the jobs in jobList to a SchedulingState in the schedule.
			for (Job job : joblist) {
				// contains users that we have identified as exceeding their quota. These users will be skipped
				HashMap<Integer, Boolean> quotaExceededUsers = new HashMap<Integer,Boolean>();
				
				if (!quotaExceededUsers.containsKey(job.getUserId())) {
					//TODO: Handle in a new thread if this looks slow on Starexec
					quotaExceededUsers.put(job.getUserId(), Users.isDiskQuotaExceeded(job.getUserId()));
					if (quotaExceededUsers.get(job.getUserId())) {
						Jobs.pauseAllUserJobs(job.getUserId());
					}
				}
				if (quotaExceededUsers.get(job.getUserId())) {
					continue;
				}

				// jobTemplate is a version of mainTemplate customized for this job
				String jobTemplate = mainTemplate.replace("$$QUEUE$$", q.getName());			
				jobTemplate = jobTemplate.replace("$$RANDSEED$$",""+job.getSeed());
				jobTemplate = jobTemplate.replace("$$USERID$$", "" + job.getUserId());
				jobTemplate = jobTemplate.replace("$$DISK_QUOTA$$", ""+job.getUser().getDiskQuota());
				jobTemplate = jobTemplate.replace("$$BENCH_SAVE_PATH$$", BenchmarkUploader.getDirectoryForBenchmarkUpload(job.getUserId(), null).getAbsolutePath());
				// for every job, retrieve no more than the number of pairs that would fill the queue. 
				// retrieving more than this is wasteful.
				int limit=Math.max(R.NUM_JOB_PAIRS_AT_A_TIME, (nodeCount*R.NODE_MULTIPLIER)-queueSize);
				log.debug("calling Jobs.getPendingPairsDetailed for job "+job.getId());
				List<JobPair> pairs = Jobs.getPendingPairsDetailed(job,limit);
				log.debug("finished call to getPendingPairsDetailed");

				if (pairs.size()>0) {
					Iterator<JobPair> pairIter = pairs.iterator();					
					SchedulingState s = new SchedulingState(job,jobTemplate,pairIter);
					schedule.add(s);
				} else {
					log.debug("not adding any pairs from job "+job.getId());
				}

			}
			
			// maps user IDs to the total 'load' that user is responsible for on the current queue,
			// where load is the sum of wallclock timeouts of all active pairs on the queue
			HashMap<Integer, Integer> userToCurrentQueueLoad = new HashMap<Integer, Integer>();
			Iterator<SchedulingState> it = schedule.iterator();
			while (it.hasNext()) {
				SchedulingState s = it.next();
				if (!userToCurrentQueueLoad.containsKey(s.job.getUserId())) {
					userToCurrentQueueLoad.put(s.job.getUserId(), Queues.getUserLoadOnQueue(q.getId(), s.job.getUserId()));
				}
			}
			// updates user load values to take into account actual job pair runtimes.
			monitor.subtractTimeDeltas(JobPairs.getAndClearTimeDeltas(q.getId()));

			log.info("Beginning scheduling of "+schedule.size()+" jobs on queue "+q.getName());
			
			/*
			 * we are going to loop through the schedule adding a few job
			 * pairs at a time to SGE.
			 */
			
			//transient database errors can cause us to loop forever here, and we need to make sure that does not happen
			int maxLoops=500;
			int curLoops=0;
			while (!schedule.isEmpty()) {
				curLoops++;
				if (queueSize >= R.NODE_MULTIPLIER * nodeCount) {
					break; // out of while (!schedule.isEmpty())

				}
				if (curLoops>maxLoops) {
					log.warn("forcibly breaking out of JobManager.submitJobs()-- max loops exceeded");
					break;
				}

				it = schedule.iterator();
				
				//add all of the users that still have pending entries to the list of users
				HashMap<Integer, Integer> pendingUsers=new HashMap<Integer, Integer>();
				while (it.hasNext()) {
					SchedulingState s = it.next();
					pendingUsers.put(s.job.getUserId(), userToCurrentQueueLoad.get(s.job.getUserId()));
				}
				
				monitor.setUsers(pendingUsers);
				monitor.setUserLoadDataFormattedString();
				it = schedule.iterator();

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
					while (i < R.NUM_JOB_PAIRS_AT_A_TIME && s.pairIter.hasNext()) {
						//skip if this user has many more pairs than some other user
						if (monitor.skipUser(s.job.getUserId())) {
							log.info("excluding user with the following id from submitting more pairs "+s.job.getUserId());
							Long min = monitor.getMin();
							if (min==null) {
								min = -1l;
							}
							log.info("user had already submitted "+i+" pairs in this iteration. Load = "+monitor.getLoad(s.job.getUserId())+ " Min = " + min);
							break;
						}

						JobPair pair = s.pairIter.next();
						monitor.changeLoad(s.job.getUserId(), s.job.getWallclockTimeout());
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
							String scriptPath = JobManager.writeJobScript(s.jobTemplate, s.job, pair, q);

							String logPath=JobPairs.getLogFilePath(pair);
							File file=new File(logPath);
							file.getParentFile().mkdirs();
			
							if (file.exists()) {
							    log.info("Deleting old log file for " + pair.getId());
							    file.delete();
							}

							// do this first, before we submit to grid engine, to avoid race conditions
							JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
							// Submit to the grid engine
							int execId = R.BACKEND.submitScript(scriptPath, R.BACKEND_WORKING_DIR,logPath);

							if(!R.BACKEND.isError(execId)){
							    JobPairs.updateBackendExecId(pair.getId(),execId);
							} else{
							    JobPairs.setStatusForPairAndStages(pair.getId(),StatusCode.ERROR_SGE_REJECT.getVal());
							}
							queueSize++; 
						} catch(Exception e) {
							log.error("submitJobs() received exception " + e.getMessage(), e);
							log.error("setting pair with following ID to submit_fail "+pair.getId());
							JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.ERROR_SUBMIT_FAIL.getVal());
						}
					}
				} // end iterating once through the schedule
			} // end looping until schedule is empty or we have submitted enough job pairs

		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
	} // end submitJobs()


    protected static String base64encode(String s) {
    	return new String(Base64.encodeBase64(s.getBytes()));
    }
    
    public static String addParametersToJobscript(String jobScript, Map<String, String> replacements) {
    	String[] current = new String[replacements.size()];
    	String[] replace = new String[replacements.size()];
    	int index = 0;
    	for (String s : replacements.keySet()) {
    		current[index] = s;
    		replace[index] = replacements.get(s);
    		index+=1;
    	}
    	return StringUtils.replaceEach(jobScript, current, replace);
    }

	/**
	 * Creates a new job script file based on the given job and job pair.
	 * @param template The template to base the new script off of
	 * @param job The job to tailor the script for
	 * @param pair The job pair to tailor the script for
	 * @return The absolute path to the newly written script
	 */
	private static String writeJobScript(String template, Job job, JobPair pair, Queue queue) throws Exception {
		String jobScript = template;		
		
		// all of these arrays are for containing individual attributes ordered by state number for all the stages in the pair.
		List<Integer> stageCpuTimeouts=new ArrayList<Integer>();
		List<Integer> stageWallclockTimeouts=new ArrayList<Integer>();
		List<Integer> stageNumbers=new ArrayList<Integer>();
		List<Long> stageMemLimits=new ArrayList<Long>();
		List<Integer> solverIds=new ArrayList<Integer>();
		List<String> solverNames=new ArrayList<String>();
		List<String> configNames=new ArrayList<String>();
		List<String> solverTimestamps=new ArrayList<String>();
		List<String> solverPaths=new ArrayList<String>();
		List<String> postProcessorPaths=new ArrayList<String>();
		List<String> preProcessorPaths=new ArrayList<String>();
		List<Integer> spaceIds = new ArrayList<Integer>();
		List<String> benchInputPaths=new ArrayList<String>();
		List<String> argStrings=new ArrayList<String>();
		List<String> benchSuffixes=new ArrayList<String>();
		List<Integer> resultsIntervals = new ArrayList<Integer>();
		List<Integer> stdoutSaveOptions = new ArrayList<Integer>();
		List<Integer> extraSaveOptions = new ArrayList<Integer>();
		for (String path : pair.getBenchInputPaths()) {
			log.debug("adding the following path to benchInputPaths ");
			log.debug(path);
			benchInputPaths.add(path);
		}
		benchInputPaths.add(""); // just terminating this array with a blank string so the Bash array will always have some element
		String primaryPreprocessorPath="";
		for (JoblineStage stage : pair.getStages()) {
			int stageNumber=stage.getStageNumber();
			stageNumbers.add(stageNumber);
			StageAttributes attrs= job.getStageAttributesByStageNumber(stageNumber);
			stageCpuTimeouts.add(attrs.getCpuTimeout());
			benchSuffixes.add(attrs.getBenchSuffix());
			stageWallclockTimeouts.add(attrs.getWallclockTimeout());
			stageMemLimits.add(attrs.getMaxMemory());
			solverIds.add(stage.getSolver().getId());
			solverNames.add(stage.getSolver().getName());
			configNames.add(stage.getConfiguration().getName());
			solverTimestamps.add(stage.getSolver().getMostRecentUpdate());
			solverPaths.add(stage.getSolver().getPath());
			argStrings.add(JobManager.pipelineDependenciesToArgumentString(stage.getDependencies()));
			resultsIntervals.add(attrs.getResultsInterval());
			stdoutSaveOptions.add(attrs.getStdoutSaveOption().getVal());
			extraSaveOptions.add(attrs.getExtraOutputSaveOption().getVal());
			if (attrs.getSpaceId()==null) {
				spaceIds.add(null);
			} else {
				Integer spaceId= Spaces.getSubSpaceIDByPath(attrs.getSpaceId(), pair.getPath());
				if (spaceId==null || spaceId==-1) {
					spaceIds.add(null);
				} else {
					spaceIds.add(spaceId);
				}
			}
			
			// for processors, we still need one entry per stage even though not all stages have processors
			// in the Bash scripts, an empty string will be interpreted as "no processor"
			Processor p = attrs.getPostProcessor();
			if (p==null) {
				postProcessorPaths.add("");
			} else {
				postProcessorPaths.add(p.getFilePath());
			}
			p=attrs.getPreProcessor();
			if (p==null) {
				preProcessorPaths.add("");
			} else {
				preProcessorPaths.add(p.getFilePath());
				if (stage.getStageNumber()==pair.getPrimaryStageNumber()) {
					primaryPreprocessorPath=p.getFilePath();
				}
			}
		}
		File outputFile=new File(JobPairs.getPairStdout(pair));
		
		//if there is exactly 1 stage, we use the old output format
		if (stageNumbers.size()==1) {
			outputFile=outputFile.getParentFile();
		}
		
		// maps from strings in the jobscript to the strings that should be filled in
		HashMap<String, String> replacements = new HashMap<String, String>();
		//Dependencies
		if (pair.getBench().getUsesDependencies())
		{
			replacements.put("$$HAS_DEPENDS$$", "1");
			writeDependencyFile(pair.getId(),Benchmarks.getBenchDependencies(pair.getBench().getId()));	
		}
		else{
			replacements.put("$$HAS_DEPENDS$$", "0");
		}
		replacements.put("$$BENCH$$", base64encode(pair.getBench().getPath()));
		replacements.put("$$PAIRID$$", ""+pair.getId());
		replacements.put("$$SPACE_PATH$$", pair.getPath());
		replacements.put("$$PRIMARY_PREPROCESSOR_PATH$$", primaryPreprocessorPath);
		replacements.put("$$PAIR_OUTPUT_DIRECTORY$$", base64encode(outputFile.getAbsolutePath()));
		replacements.put("$$MAX_RUNTIME$$","" + Util.clamp(1, queue.getWallTimeout(), job.getWallclockTimeout()));
		replacements.put("$$MAX_CPUTIME$$", "" + Util.clamp(1, queue.getCpuTimeout(), job.getCpuTimeout()));
		replacements.put("$$MAX_MEM$$", ""+Util.bytesToMegabytes(job.getMaxMemory()));
        replacements.put("$$BENCH_ID$$", ""+pair.getBench().getId());

        if(job.isBuildJob()) {
                replacements.put("$$BUILD_JOB$$", "true");
        }
        else {
                replacements.put("$$BUILD_JOB$$", "false");
        }
		
		// all arrays from above. Note that we are base64 encoding some for safety
		replacements.put("$$CPU_TIMEOUT_ARRAY$$", numsToBashArray("STAGE_CPU_TIMEOUTS",stageCpuTimeouts));
		replacements.put("$$CLOCK_TIMEOUT_ARRAY$$", numsToBashArray("STAGE_CLOCK_TIMEOUTS",stageWallclockTimeouts));
		replacements.put("$$MEM_LIMIT_ARRAY$$", numsToBashArray("STAGE_MEM_LIMITS",stageMemLimits));
		replacements.put("$$STAGE_NUMBER_ARRAY$$", numsToBashArray("STAGE_NUMBERS",stageNumbers));
		replacements.put("$$SOLVER_ID_ARRAY$$",numsToBashArray("SOLVER_IDS",solverIds));
		replacements.put("$$SOLVER_TIMESTAMP_ARRAY$$",toBashArray("SOLVER_TIMESTAMPS",solverTimestamps,false));
		replacements.put("$$CONFIG_NAME_ARRAY$$", toBashArray("CONFIG_NAMES",configNames,false));
		replacements.put("$$PRE_PROCESSOR_PATH_ARRAY$$",toBashArray("PRE_PROCESSOR_PATHS",preProcessorPaths,false));
		replacements.put("$$POST_PROCESSOR_PATH_ARRAY$$",toBashArray("POST_PROCESSOR_PATHS",postProcessorPaths,false));
		replacements.put("$$SPACE_ID_ARRAY$$",numsToBashArray("SPACE_IDS",spaceIds));
		replacements.put("$$SOLVER_NAME_ARRAY$$",toBashArray("SOLVER_NAMES",solverNames,true));
		replacements.put("$$SOLVER_PATH_ARRAY$$",toBashArray("SOLVER_PATHS",solverPaths,true));
		replacements.put("$$BENCH_INPUT_ARRAY$$",toBashArray("BENCH_INPUT_PATHS",benchInputPaths,true));
		replacements.put("$$STAGE_DEPENDENCY_ARRAY$$", toBashArray("STAGE_DEPENDENCIES",argStrings,false));
		replacements.put("$$BENCH_SUFFIX_ARRAY$$",toBashArray("BENCH_SUFFIXES",benchSuffixes,true));
		replacements.put("$$RESULTS_INTERVAL_ARRAY$$", numsToBashArray("RESULTS_INTERVALS", resultsIntervals));
		replacements.put("$$STDOUT_SAVE_OPTION_ARRAY$$", numsToBashArray("STDOUT_SAVE_OPTIONS", stdoutSaveOptions));
		replacements.put("$$EXTRA_SAVE_OPTION_ARRAY$$", numsToBashArray("EXTRA_SAVE_OPTIONS", extraSaveOptions));

		String scriptPath = String.format("%s/%s", R.getJobInboxDir(), String.format(R.JOBFILE_FORMAT, pair.getId()));
		replacements.put("$$SCRIPT_PATH$$",scriptPath);
		replacements.put("$$SUPPRESS_TIMESTAMP_OPTION$$", String.valueOf(job.timestampIsSuppressed()));
		File f = new File(scriptPath);
		jobScript = addParametersToJobscript(jobScript, replacements);

		f.delete();		
		f.getParentFile().mkdirs();
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
	 * Given a list of pipeline dependencies, this creates a single string containing all of the relevant arguments
	 * so that all the dependencies can be passed to the configuration.
	 * 
	 *  
	 * @param deps The dependencies. Must be ordered by input number to get the correct order
	 * @return The argument string.
	 */
	public static String pipelineDependenciesToArgumentString(List<PipelineDependency> deps) {
		if (deps== null || deps.size()==0) {
			return "";
		}
		log.debug("creating a dependency argument string with this many deps = "+deps.size());
		StringBuilder sb=new StringBuilder();
		for (PipelineDependency dep : deps) {
			// SAVED_OUTPUT_DIR and BENCH_INPUT_DIR are variables defined in the jobscript
			if (dep.getType()==PipelineInputType.ARTIFACT) {
				sb.append("\"$SAVED_OUTPUT_DIR/");
				sb.append(dep.getDependencyId());
				sb.append("\" ");
				
			} else if (dep.getType()==PipelineInputType.BENCHMARK) {
				sb.append("\"$BENCH_INPUT_DIR/");
				sb.append(dep.getDependencyId());
				sb.append("\" ");
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Given the name of an array and a list of strings to put into the array, 
	 * creates a string that generates the array that can be embedded into a bash script.
	 * If strs is empty, returns an empty string. Array is 0 indexed. 
	 * @param arrayName The name to give the array
	 * @param strs The strings to include, in order.
	 * @param base64 True to base64 encode all the strings and false otherwise
	 * @return The array as a String that can be embedded directly into the jobscript.
	 */
	public static String toBashArray(String arrayName, List<String> strs, boolean base64) {
		if (strs.size()==0) {
			return "";
		}
		int index=0;
		StringBuilder sb=new StringBuilder();
		for (String s : strs) {
			if (s==null) {
				s="";
			}
			sb.append(arrayName);
			sb.append("[");
			sb.append(index);
			sb.append("]=\"");
			if (base64) {
				sb.append(base64encode(s));

			} else {
				sb.append(s);

			}
			sb.append("\"\n");
			index=index+1;
		}
		return sb.toString();
	}
	
	/**
	 * Creates a String that can be inserted into a Bash script as an array where all the given numbers
	 * are in the array starting from index 0. Null is encoded as a blank string in the array
	 * @param arrayName The name of the variable holding the array in Bash
	 * @param nums The numbers to insert into the array
	 * @return The string to insert
	 */
	public static <T extends Number> String numsToBashArray(String arrayName, List<T> nums){
		List<String> strs=new ArrayList<String>();
		for (T num : nums) {
			if (num!=null) {
				strs.add(num.toString());

			} else {
				strs.add("");
			}
		}
		return toBashArray(arrayName,strs,false);
	}

	/**
	 * Writes a file containing benchmark dependencies ( note: these are NOT related to any of the pipeline dependencies)
	 * to the jobin directory for the given pair and benchmark.
	 * @param pairId
	 * @param dependencies
	 * @return
	 * @throws Exception
	 */
	public static Boolean writeDependencyFile(Integer pairId, List<BenchmarkDependency> dependencies) throws Exception{		
		StringBuilder sb = new StringBuilder();
		String separator = ",,,";
		for (BenchmarkDependency bd:dependencies)
		{
			sb.append(bd.getSecondaryBench().getPath());
			sb.append(separator);		
			sb.append(bd.getDependencyPath());
			sb.append("\n");
		}

		String dependFilePath = String.format("%s/%s", R.getJobInboxDir(), String.format(R.DEPENDFILE_FORMAT, pairId));
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
	public static Job setupJob(int userId, String name, String description, int preProcessorId, int postProcessorId, int queueId, long randomSeed,
			int cpuLimit,int wallclockLimit, long memLimit, boolean suppressTimestamp, int resultsInterval, SaveResultsOption otherOutputOption) {
		log.debug("Setting up job " + name);
		Job j = new Job();

		// Set the job's name, submitter user id and description
		j.setUserId(userId);
		j.setName(name);		
		j.setSeed(randomSeed);
		j.setCpuTimeout(cpuLimit);
		j.setWallclockTimeout(wallclockLimit);
		j.setMaxMemory(memLimit);
		if(description != null) {
			j.setDescription(description);
		}

		// Get queue and processor information from the database and put it in the job
		j.setQueue(Queues.get(queueId));
		StageAttributes attrs=new StageAttributes();
		attrs.setCpuTimeout(cpuLimit);
		attrs.setWallclockTimeout(wallclockLimit);
		attrs.setMaxMemory(memLimit);
		attrs.setStageNumber(1);
		attrs.setSpaceId(null);
		attrs.setResultsInterval(resultsInterval);
		attrs.setExtraOutputSaveOption(otherOutputOption);
		if(preProcessorId > 0) {
			attrs.setPreProcessor(Processors.get(preProcessorId));
		} else {
			attrs.setPreProcessor(null);
		}
		if(postProcessorId > 0) {
			attrs.setPostProcessor(Processors.get(postProcessorId));		
		} else {
			attrs.setPostProcessor(null);
		}
		j.addStageAttributes(attrs);
		j.setSuppressTimestamp(suppressTimestamp);
		
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
	 */
	public static void buildJob(Job j, List<Integer> benchmarkIds, List<Integer> configIds, Integer spaceId) {
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
				stage.setStageNumber(1);
				pair.setPrimaryStageNumber(1);
				stage.setNoOp(false);

				stage.setSolver(solver);
				stage.setConfiguration(solver.getConfigurations().get(0));
				pair.addStage(stage);
				
				pair.setSpace(Spaces.get(spaceId));
				pair.setPath(spaceName);
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
		//log.debug("calling addJobPairsFrom space on space ID = "+spaceId);
		//log.debug("the path for the pairs will be ");
		//log.debug(path);
		List<JobPair> pairs=new ArrayList<JobPair>();
		// Get the benchmarks and solvers from this space
		List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);
		//log.debug("found this many benchmarks in the space = "+benchmarks.size());
		List<Solver> solvers = Solvers.getBySpace(spaceId);
		for (Solver s : solvers) {
			List<Configuration> configs = Solvers.getConfigsForSolver(s.getId());
			for (Configuration c : configs) {
				s.addConfiguration(c);
			}
			   
		}
		JobPair pair;
		for (Benchmark b : benchmarks) {
			for (Solver s : solvers) {
				// Get the configurations for the current solver
				for (Configuration c : s.getConfigurations()) {

					Solver clone = JobManager.cloneSolver(s);
					// Now we're going to work with this solver with this configuration
					clone.addConfiguration(c);

					pair = new JobPair();
					pair.setBench(b);
					JoblineStage stage=new JoblineStage();
					stage.setStageNumber(1);
					stage.setSolver(clone);
					stage.setConfiguration(c);
					pair.addStage(stage);
					
					pair.setPrimaryStageNumber(1);
					stage.setNoOp(false);
					
					pair.setSpace(space);
					//we are running pairs in a single space, so the path is flat
					pair.setPath(path);
					pairs.add(pair);

					
				}
			}
		}
		return pairs;
	}
    /**
     * This method creates and adds the build job that compiles the solver on the woker nodes
     * @author Andrew Lubinus
     * @param solverId the id of the unbuilt solver
     * @param spaceId the space where the solver is, at this point also the space where the job is added
     * @return int jobId of the job that has been added, or -1 if failed to add job.
     */

	public static int addBuildJob(Integer solverId, Integer spaceId) {
		Solver s = Solvers.get(solverId);
		log.info("Adding build job for solver " + s.getName() + " in space: " + spaceId);
        Queue q = Queues.getAllQ();
		Job j = JobManager.setupJob(
			s.getUserId(),
			s.getName() + " Build",
			s.getName() + " Build Job",
			-1,
			-1,
			R.DEFAULT_QUEUE_ID, //This is the same queue referenced by variable q
			0,
			q.getCpuTimeout(),
			q.getWallTimeout(),
			R.DEFAULT_PAIR_VMEM,
			false,
			15,
			SaveResultsOption.SAVE);
		j.setBuildJob(true);
		String spaceName = "job space";
		String sm=Spaces.getName(spaceId);
		if (sm!=null) {
			spaceName = sm;
		}
        Configuration c = new Configuration();
        c.setName("starexec_build");
        c.setSolverId(solverId);
        c.setDescription("Build Configuration for solver: " + solverId);
        int cId = Solvers.addConfiguration(s,c);
		JobPair pair = new JobPair();
		JoblineStage stage = new JoblineStage();
		stage.setStageNumber(1);
		pair.setPrimaryStageNumber(1);
		stage.setNoOp(false);
		stage.setSolver(s);
        stage.setConfiguration(c);
        int bench = BenchmarkUploader.addBenchmarkFromText(
                        "dummy benchmark",
                        "starexec_build",
                        s.getUserId(),
                        R.NO_TYPE_PROC_ID, true);
		pair.addStage(stage);
        pair.setBench(Benchmarks.get(bench)); //This hard coded value should be changed before feature is used.
		pair.setSpace(Spaces.get(spaceId));
		pair.setPath(spaceName);
		j.addJobPair(pair);
		boolean submitSuccess = Jobs.add(j, spaceId);
		if (submitSuccess) {
			return j.getId();
		} else {
            int status = SolverBuildStatusCode.BUILD_FAILED.getVal();
            Solvers.setSolverBuildStatus(s, status);
		    return -1; //error
        }
	}

	/**
	 * With the given solvers and configurations, will find all benchmarks in the current space hierarchy
	 * and create job pairs from the result. Will then return those job pairs
	 * 
	 * @param spaceId the id of the space we start in
	 * @param userId the id of the user creating the job
	 * @param configIds a list of configurations to use
	 * @param path The path to use for each job pair created.
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
						stage.setStageNumber(1);
						pair.setPrimaryStageNumber(1);
						stage.setNoOp(false);

						stage.setSolver(solver);
						stage.setConfiguration(solver.getConfigurations().get(0));
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
						stage.setStageNumber(1);
						stage.setSolver(solver);
						stage.setConfiguration(solver.getConfigurations().get(0));
						pair.setPrimaryStageNumber(1);
						stage.setNoOp(false);

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
