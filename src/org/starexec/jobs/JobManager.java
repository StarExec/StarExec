package org.starexec.jobs;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.starexec.constants.DB;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.Queue;
import org.starexec.data.to.SolverBuildStatus.SolverBuildStatusCode;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.data.to.pipelines.StageAttributes.SaveResultsOption;
import org.starexec.data.to.tuples.JobCount;
import org.starexec.exceptions.BenchmarkDependencyMissingException;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.servlets.UploadBenchmark;
import org.starexec.util.Timer;
import org.starexec.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all SGE interactions for job submission and maintenance
 *
 * @author Tyler Jensen
 */
public abstract class JobManager {
	private static final StarLogger log = StarLogger.getLogger(JobManager.class);

	private static String mainTemplate = null; // initialized below

	private static Map<Integer, LoadBalanceMonitor> queueToMonitor = new HashMap<>();

	/**
	 * Returns the string representation of the LoadBalanceMonitor for the given queue.
	 *
	 * @param queueId
	 * @return The string. Note that it may be slightly out of date, as it is only updated once per run of
	 * JobManager.submitJobs.
	 */
	public static String getLoadRepresentationForQueue(int queueId) {
		log.debug("getLoadRepresentationForQueue", "retrieving load data for queue: " + queueId);
		if (queueToMonitor.containsKey(queueId)) {
			return queueToMonitor.get(queueId).toString();
		}
		String knownQueues = queueToMonitor.keySet().toString();
		log.warn(
				"getLoadRepresentationForQueue",
				"queue not found: " + queueId
				+ "\n\tknown queues:" + knownQueues
		);
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
		queueToMonitor = new HashMap<>();
		JobPairs.getAndClearTimeDeltas(-1);
	}

	public synchronized static void checkPendingJobs() {
		Timer timer = new Timer();
		try {
			Boolean devJobsOnly = false;
			log.debug("about to check if the system is paused");
			if (Jobs.isSystemPaused()) {
				if (Queues.developerJobsExist()) {
					log.info("Submitting only developer jobs");
					devJobsOnly = true;
				} else {
					log.info("Not adding more job pairs to any queues, as the system is paused");
					return;
				}
			}
			Common.logConnectionsOpen();
			log.debug("about to get all queues");

			List<Queue> queues = Queues.getAllActive();
			log.debug("found this many queues " + queues.size());
			for (Queue q : queues) {
				log.debug("about to submit to queue " + q.getId());
				int qId = q.getId();
				String qname = q.getName();
				int nodeCount = Queues.getNodes(qId).size();
				int queueSize = Queues.getSizeOfQueue(qId);
				log.debug("trying to submit on queue " + qId + " with " + nodeCount + " nodes and " + queueSize +
				          " pairs");
				if (queueSize < R.NODE_MULTIPLIER * nodeCount) {
					List<Job> joblist;
					if (devJobsOnly) {
						joblist = Queues.getPendingDeveloperJobs(qId);
					} else {
						joblist = Queues.getPendingJobs(qId);
					}
					if (!joblist.isEmpty() || joblist != null) {
						log.debug("about to submit this many jobs " + joblist.size());
						submitJobs(joblist, q, queueSize, nodeCount);
					} else {
						// If we have no jobs to submit, reset the queue monitor
						// so that it is no longer tracking users. This strategy ensures
						// that it is always in the user's best interest to run job pairs.
						LoadBalanceMonitor m = queueToMonitor.get(q.getId());
						if (m != null) {
							log.info("No jobs to submit, resetting monitor for queue with id: " + q.getId());
							m.reset();
							m.setUserLoadDataFormattedString();
						}
					}
				} else {
					log.info("Not adding more job pairs to queue " + qname + ", which has " + queueSize +
					         " pairs enqueued.");
				}
			}
		} catch (Exception e) {
			log.error("checkPendingJobs", e);
		} finally {
			log.info("checkPendingJobs", "Finished in " + timer.getTime() + " milliseconds");
		}
	}


	/**
	 * initialize mainTemplate, a string hold the jobscript customized for the
	 * current configuration (but not the current job or job pair), if it is
	 * not yet initialized.
	 *
	 * @author Aaron Stump
	 */
	protected static void initMainTemplateIf() {
		if (mainTemplate == null) {
			// Read in the job script template and format it for this global configuration
			File f = new File(R.CONFIG_PATH, "sge/jobscript");
			try {
				mainTemplate = FileUtils.readFileToString(f);
			} catch (IOException e) {
				log.error("Error reading the jobscript at " + f, e);
			}
			mainTemplate = mainTemplate.replace("$$DB_NAME$$", R.MYSQL_DATABASE);
			mainTemplate = mainTemplate.replace("$$DB_USER$$", R.COMPUTE_NODE_MYSQL_USERNAME);
			mainTemplate = mainTemplate.replace("$$DB_PASS$$", R.COMPUTE_NODE_MYSQL_PASSWORD);
			mainTemplate = mainTemplate.replace("$$REPORT_HOST$$", R.REPORT_HOST);
			mainTemplate = mainTemplate.replace("$$STAREXEC_DATA_DIR$$", R.STAREXEC_DATA_DIR);
			// Impose resource limits
			mainTemplate = mainTemplate.replace("$$MAX_WRITE$$", String.valueOf(R.MAX_PAIR_FILE_WRITE));
			mainTemplate = mainTemplate.replace("$$BENCH_NAME_LENGTH_MAX$$", String.valueOf(DB.BENCH_NAME_LEN));
			mainTemplate = mainTemplate.replace("$$RUNSOLVER_PATH$$", R.RUNSOLVER_PATH);
			mainTemplate = mainTemplate.replace("$$SANDBOX_USER_ONE$$", R.SANDBOX_USER_ONE);
			mainTemplate = mainTemplate.replace("$$SANDBOX_USER_TWO$$", R.SANDBOX_USER_TWO);
			mainTemplate = mainTemplate.replace("$$WORKING_DIR_BASE$$", R.BACKEND_WORKING_DIR);
			mainTemplate = mainTemplate.replace("$$SCRIPT_DIR$$", R.getScriptDir());
			mainTemplate = mainTemplate.replace("$$JOBPAR_EXECUTION_PREFIX$$", R.JOBPAIR_EXECUTION_PREFIX);
		}
	}

	/**
	 * Gets the load balance monitor for a particular queue.
	 *
	 * @param queueId The ID of the queue to get the monitor for
	 * @return
	 */
	private static LoadBalanceMonitor getMonitor(int queueId) {
		if (!queueToMonitor.containsKey(queueId)) {
			queueToMonitor.put(queueId, new LoadBalanceMonitor());
		}
		return queueToMonitor.get(queueId);
	}

	// Builds a map from user to the SchedulingStates containing high priority jobs in the schedule.
	private static void addToHighPriorityStateMap(SchedulingState state, Map<Integer, List<SchedulingState>>
			userToHighPriorityStates) {
		int userId = state.job.getUserId();
		// Add the state to the map for the user if the state represents a high priority job.
		if (userToHighPriorityStates.containsKey(userId)) {
			userToHighPriorityStates.get(userId).add(state);
		} else {
			List<SchedulingState> highPriorityStatesForUser = new ArrayList<>();
			highPriorityStatesForUser.add(state);
			userToHighPriorityStates.put(userId, highPriorityStatesForUser);
		}
	}

	private static void logSchedulingState(final String methodName, final SchedulingState s, final int tabLevel) {
		final StringBuilder logMessage = new StringBuilder();
		for (int i = 0; i < tabLevel; i++) {
			logMessage.append("\t");
		}

		logMessage.append("( jobId: ").append(s.job.getId()).append(", userId: ").append(s.job.getUserId())
		          .append(", isHighPriority: ").append(s.job.isHighPriority()).append(", hasNext: ")
		          .append(s.pairIter.hasNext()).append(" )");

		log.debug(methodName, logMessage.toString());
	}

	private static Map<Integer, JobCount> buildUserToJobCountMap(final List<Job> joblist) {
		final Map<Integer, JobCount> userToJobCountMap = new HashMap<>();
		for (final Job j : joblist) {
			final int userId = j.getUserId();
			if (!userToJobCountMap.containsKey(userId)) {
				userToJobCountMap.put(userId, new JobCount(0, 0));
			}

			JobCount jobCount = userToJobCountMap.get(userId);

			jobCount.all += 1;
			if (j.isHighPriority()) {
				jobCount.highPriority += 1;
			}
		}
		return userToJobCountMap;
	}

	private static void addToHighPriorityJobBalance(SchedulingState state, Map<Integer, Map<Integer, Integer>>
			balance) {
		final int userId = state.job.getUserId();
		if (!balance.containsKey(userId)) {
			balance.put(userId, new HashMap<>());
		}

		Map<Integer, Integer> jobIdToTimesSelected = balance.get(userId);
		jobIdToTimesSelected.put(state.job.getId(), 0);
	}

	/**
	 * Selects a high priority SchedulingState from the user's high priority states taking into consideration how many
	 * times those state have been selected previously
	 *
	 * @param userId the user that we need to pick a new state for.
	 * @param usersHighPriorityStates the high priority states owned by the user.
	 * @param usersHighPriorityJobBalance a mapping from high priority state to the number of times that state has
	 * already been chosen for the user.
	 * @return
	 * @throws StarExecException
	 */
	private static SchedulingState selectHighPriorityJob(int userId, List<SchedulingState> usersHighPriorityStates,
	                                                     Map<Integer, Integer> usersHighPriorityJobBalance) throws
			StarExecException {


		// Check if all the high priority jobs have been selected an equal number of times.
		HashSet<Integer> valueSet = new HashSet<>(usersHighPriorityJobBalance.values());
		boolean allEquals = valueSet.size() == 1;

		if (allEquals) {
			// If all the high priority jobs have been selected an equal number of times, randomly pick one to use.
			Random random = new Random();
			SchedulingState selectedState = usersHighPriorityStates.get(random.nextInt(usersHighPriorityStates.size
					()));
			Integer currentBalanceForState = usersHighPriorityJobBalance.get(selectedState.job.getId());
			usersHighPriorityJobBalance.put(selectedState.job.getId(), currentBalanceForState + 1);
			return selectedState;
		}

		// Get and return the high priority job that has been selected the least number of times.
		Map.Entry<Integer, Integer> minEntry =
				Collections.min(usersHighPriorityJobBalance.entrySet(), Comparator.comparing(Map.Entry::getValue));

		// Find the high priority state with the job id that has been selected the minimum number of times.
		for (SchedulingState state : usersHighPriorityStates) {
			if (state.job.getId() == minEntry.getKey()) {
				return state;
			}
		}

		throw new StarExecException(
				"The state with jobId=" + minEntry.getKey() + " was not in the high priority state list for" +
				"user with id=" + userId + " but it was in their high priority job balance.");

	}

	/**
	 * Submits a job to the grid engine
	 *
	 * @param joblist The list of jobs for which we will be submitted new pairs
	 * @param q The queue to submit on
	 * @param queueSize The number of job pairs enqueued in the given queue
	 * @param nodeCount The number of nodes in the given queue
	 */
	public static void submitJobs(final List<Job> joblist, final Queue q, int queueSize, final int nodeCount) {
		final String methodName = "submitJobs";
		final LoadBalanceMonitor monitor = getMonitor(q.getId());
		Timer timer = new Timer();

		try {
			log.entry(methodName);

			initMainTemplateIf();

			// updates user load values to take into account actual job pair runtimes.
			monitor.subtractTimeDeltas(JobPairs.getAndClearTimeDeltas(q.getId()));

			final LinkedList<SchedulingState> schedule = buildSchedule(joblist, q, queueSize, nodeCount);

			// Map from (user id) -> ( (high priority job id) -> (# of times job been selected) )
			// Balances out the number of times a high priority job can be selected.
			final Map<Integer, Map<Integer, Integer>> highPriorityJobBalance = new HashMap<>();

			// maps user IDs to the total 'load' that user is responsible for on the current queue,
			// where load is the sum of wallclock timeouts of all active pairs on the queue
			final Map<Integer, Long> userToCurrentQueueLoad = new HashMap<>();

			// maps user IDs to the scheduling states containing high priority jobs that the user owns.
			final Map<Integer, List<SchedulingState>> userToHighPriorityStates = new HashMap<>();

			// Build the highPriorityJobBlance, userToCurrentQueueLoad, and userToHighPriorityStates maps.
			// We build them all in one loop for efficiency.
			populateCurrentQueueLoadAndHighPriorityMaps(schedule, q, highPriorityJobBalance, userToCurrentQueueLoad,
					userToHighPriorityStates);

			log.info("Beginning scheduling of " + schedule.size() + " jobs on queue " + q.getName());

			/*
			 * we are going to loop through the schedule adding a few job
			 * pairs at a time to SGE.
			 */

			//transient database errors can cause us to loop forever here, and we need to make sure that does not
			// happen
			final int maxLoops = 500;
			int curLoops = 0;
			while (!schedule.isEmpty()) {

				curLoops++;
				if (queueSize >= R.NODE_MULTIPLIER * nodeCount) {
                                    log.info("Breaking out of submitJobs, with queueSize " + queueSize);
                                    break; // out of while (!schedule.isEmpty())

				}
				if (curLoops > maxLoops) {
					log.warn("submitJobs",
							"forcibly breaking out of JobManager.submitJobs()-- max loops exceeded");
					break;
				}

				Iterator<SchedulingState> it = schedule.iterator();

				//add all of the users that still have pending entries to the list of users
				final Map<Integer, Long> pendingUsers = new HashMap<>();
				while (it.hasNext()) {
					final SchedulingState s = it.next();
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

					final int currentStateUserId = s.job.getUserId();
					if (!s.job.isHighPriority() && userToHighPriorityStates.containsKey(currentStateUserId)) {
						List<SchedulingState> highPriorityStates = userToHighPriorityStates.get(currentStateUserId);


						// Filter out all of the high priority states that have no more job pairs.
						highPriorityStates = highPriorityStates.stream().filter(state -> state.pairIter.hasNext())
						                                       .collect(Collectors.toList());

						// Replace the high priority states with the filtered ones.
						userToHighPriorityStates.put(currentStateUserId, highPriorityStates);

						if (highPriorityStates.isEmpty()) {
							log.trace(methodName, "No high priority states with pairs left.");
							// Remove the user from the map if they don't have any high priority jobs left to look at.
							userToHighPriorityStates.remove(currentStateUserId);

							// Leave the current scheduling state as is.
						} else {
							try {
								if (!highPriorityJobBalance.containsKey(s.job.getUserId())) {
									throw new StarExecException(
											"Being in this block means there must be a high priority job user with" +
											"id=" + s.job.getUserId() + " but there was not.");
								} else {
									Map<Integer, Integer> highPriorityJobBalanceForUser =
											highPriorityJobBalance.get(s.job.getUserId());

									if (highPriorityJobBalanceForUser.entrySet().isEmpty()) {
										throw new StarExecException(
												"There should be high priority jobs for this user in the high" +
												"priority job balance but there isn't!, userId=" + s.job.getUserId());
									} else {
										// Change the state to a high priority one
										s = selectHighPriorityJob(s.job
												.getUserId(), highPriorityStates, highPriorityJobBalanceForUser);
									}
								}
							} catch (StarExecException e) {
								log.error(methodName, e);
								// s will not have changed.
							}
						}
					}

					log.trace("About to submit " + R.NUM_JOB_PAIRS_AT_A_TIME + " pairs " + "for job " + s.job.getId() +
					          ", queue = " + q.getName() + ", user = " + s.job.getUserId());
					int i = 0;
					while (i < R.NUM_JOB_PAIRS_AT_A_TIME && s.pairIter.hasNext()) {
						//skip if this user has many more pairs than some other user
						if (monitor.skipUser(s.job.getUserId())) {
							log.debug("dampening work for user with the following id " +
							          s.job.getUserId());
							Long min = monitor.getMin();
							if (min == null) {
								min = -1L;
							}
							log.debug("user had already submitted " + i + " pairs in this iteration. Load = " +
							          monitor.getLoad(s.job.getUserId()) + " Min = " + min);
							i = R.NUM_JOB_PAIRS_AT_A_TIME-1; // let them submit one pair at least
						}

						final JobPair pair = s.pairIter.next();

						if (pair.getPrimarySolver() == null || pair.getBench() == null) {
							// if the solver or benchmark is null, they were deleted. Indicate that the pair's
							//submission failed and move on
							JobPairs.UpdateStatus(pair.getId(), Status.StatusCode.ERROR_SUBMIT_FAIL.getVal());
							continue;
						}
						monitor.changeLoad(s.job.getUserId(), s.job.getWallclockTimeout());
						i++;
						log.trace("About to submit pair " + pair.getId());
						// Check if the benchmark for this pair has any broken dependencies.
						int benchId = pair.getBench().getId();
						log.debug("Bench id for pair about to be submitted is: " + benchId);
						try {
							log.debug("Checking bench dependencies for bench with id: " + benchId);
							if(!pair.getBenchInputs().isEmpty()) {
								List<Benchmark> brokenDependencies = Benchmarks.getBrokenBenchDependencies(benchId);
								log.debug("Found " + brokenDependencies.size() + " missing dependencies.");
								if (!brokenDependencies.isEmpty()) {
									log.debug("Skipping pair with broken bench dependency...");
									JobPairs.setStatusForPairAndStages(pair
											.getId(), StatusCode.ERROR_BENCH_DEPENDENCY_MISSING.getVal());
									continue;
								}
							} else {
								log.debug("bench had no dependencies-- skipping check");
							}
						} catch (SQLException e) {
							log.error("submitJobs", "Database error while trying to get broken bench dependencies.", e);
							// submit the pair anyway, if there are broken bench dependencies then we will get a
							// submit_failed status.
						}

						try {
							// Write the script that will run this individual pair
							final String scriptPath = JobManager.writeJobScript(s.jobTemplate, s.job, pair, q);
							log.trace("About to get the log path from the database...");
							final String logPath = JobPairs.getLogFilePath(pair);
							log.trace("Just got the log path from the database.");
							final File file = new File(logPath);
							file.getParentFile().mkdirs();

							if (file.exists()) {
								log.debug("Deleting old log file for " + pair.getId());
								file.delete();
							}

							
							log.trace("About to set the pair and stage status...");
							// do this first, before we submit to grid engine, to avoid race conditions
							JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_ENQUEUED.getVal());
							// Submit to the grid engine

							log.trace("About to submit pair " + pair.getId());

							int execId = R.BACKEND.submitScript(scriptPath, R.BACKEND_WORKING_DIR, logPath);

							log.trace("Just submitted pair " + pair.getId());

							if (R.BACKEND.isError(execId)) {
								JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.ERROR_SGE_REJECT.getVal());
							} else {
								JobPairs.updateBackendExecId(pair.getId(), execId);
							}
							queueSize++;
						} catch (BenchmarkDependencyMissingException e) {
							log.error("submitJobs", "ERROR_BENCHMARK for pair: " + pair.getId(), e);
							JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.ERROR_BENCHMARK.getVal());
						} catch (Exception e) {
							log.error("submitJobs", "ERROR_SUBMIT_FAIL for pair: " + pair.getId(), e);
							JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.ERROR_SUBMIT_FAIL.getVal());
						}
					}
				} // end iterating once through the schedule
			} // end looping until schedule is empty or we have submitted enough job pairs

			log.info(methodName, "Finished in " + timer.getTime() + " milliseconds");

		} catch (Exception e) {
			log.error(methodName, "Running for" + timer.getTime() + " milliseconds", e);
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
			index += 1;
		}
		return StringUtils.replaceEach(jobScript, current, replace);
	}

	/**
	 * Helper method that populates the highPriorityJobBalance, userToCurrentQueueLoad, and userToHighPriorityStates
	 * maps.
	 */
	private static void populateCurrentQueueLoadAndHighPriorityMaps(List<SchedulingState> schedule, Queue q, final
	Map<Integer, Map<Integer, Integer>> highPriorityJobBalance, final Map<Integer, Long> userToCurrentQueueLoad, final
	Map<Integer, List<SchedulingState>> userToHighPriorityStates) {
		for (SchedulingState s : schedule) {
			// Add all high priority states to the user to high priority states map.
			if (s.job.isHighPriority()) {
				addToHighPriorityStateMap(s, userToHighPriorityStates);
				addToHighPriorityJobBalance(s, highPriorityJobBalance);
			}

			if (!userToCurrentQueueLoad.containsKey(s.job.getUserId())) {
				userToCurrentQueueLoad.put(s.job.getUserId(), Queues.getUserLoadOnQueue(q.getId(), s.job.getUserId()));
			}
		}

	}

	/**
	 * Helper method that builds the schedule to be used for scheduling.
	 */
	private static LinkedList<SchedulingState> buildSchedule(final List<Job> joblist, final Queue q, int queueSize,
	                                                         final int nodeCount) {

		Map<Integer, JobCount> userToJobCountMap = buildUserToJobCountMap(joblist);
		final LinkedList<SchedulingState> schedule = new LinkedList<>();
		// add all the jobs in jobList to a SchedulingState in the schedule.
		for (final Job job : joblist) {

			String jobTemplate = mainTemplate.replace("$$QUEUE$$", q.getName());


			// contains users that we have identified as exceeding their quota. These users will be skipped
			final Map<Integer, Boolean> quotaExceededUsers = new HashMap<>();

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
			// By default we split the memory
			final String queueSlots = Jobs.getSlotsInJobQueue(job);
			// jobTemplate is a version of mainTemplate customized for this job

			jobTemplate = jobTemplate.replace("$$NUM_SLOTS$$", queueSlots);

			jobTemplate = jobTemplate.replace("$$RANDSEED$$", "" + job.getSeed());
			jobTemplate = jobTemplate.replace("$$USERID$$", "" + job.getUserId());
			jobTemplate = jobTemplate.replace("$$DISK_QUOTA$$", "" + job.getUser().getDiskQuota());
			// for every job, retrieve no more than the number of pairs that would fill the queue.
			// retrieving more than this is wasteful.
			int limit = Math.max(R.NUM_JOB_PAIRS_AT_A_TIME, (nodeCount * R.NODE_MULTIPLIER) - queueSize);
			log.trace("calling Jobs.getPendingPairsDetailed for job " + job.getId() + " with limit=" + limit +
			          "and queueSize=" + queueSize + " and nodeCount=" + nodeCount);
			if (job.isHighPriority()) {
				JobCount jobCount = userToJobCountMap.get(job.getUserId());
				// Assuming only high priority jobs will be scheduled this makes it so a user will have just as many
				// pairs scheduled as if they had pairs scheduled from all jobs.
				limit = (limit * jobCount.all) / jobCount.highPriority;
			}
			final List<JobPair> pairs = Jobs.getPendingPairsDetailed(job, limit);
			log.trace("finished call to getPendingPairsDetailed");

			if (!pairs.isEmpty()) {
				final Iterator<JobPair> pairIter = pairs.iterator();
				final SchedulingState s = new SchedulingState(job, jobTemplate, pairIter);
				schedule.add(s);
			} else {
				log.trace("not adding any pairs from job " + job.getId());
			}

		}
		return schedule;
	}

	/**
	 * Creates a new job script file based on the given job and job pair.
	 *
	 * @param template The template to base the new script off of
	 * @param job The job to tailor the script for
	 * @param pair The job pair to tailor the script for
	 * @return The absolute path to the newly written script
	 */
	private static String writeJobScript(String template, Job job, JobPair pair, Queue queue) throws Exception {
		String jobScript = template;

		// all of these arrays are for containing individual attributes ordered by state number for all the stages in
		// the pair.
		List<Integer> stageCpuTimeouts = new ArrayList<>();
		List<Integer> stageWallclockTimeouts = new ArrayList<>();
		List<Integer> stageNumbers = new ArrayList<>();
		List<Long> stageMemLimits = new ArrayList<>();
		List<Integer> solverIds = new ArrayList<>();
		List<String> solverNames = new ArrayList<>();
		List<String> configNames = new ArrayList<>();
		List<String> solverTimestamps = new ArrayList<>();
		List<String> solverPaths = new ArrayList<>();
		List<String> postProcessorPaths = new ArrayList<>();
		List<String> postProcessorTimeLimits = new ArrayList<>();
		List<String> preProcessorPaths = new ArrayList<>();
		List<String> preProcessorTimeLimits = new ArrayList<>();
		List<Integer> spaceIds = new ArrayList<>();
		List<String> benchInputPaths = new ArrayList<>();
		List<String> argStrings = new ArrayList<>();
		List<String> benchSuffixes = new ArrayList<>();
		List<Integer> resultsIntervals = new ArrayList<>();
		List<Integer> stdoutSaveOptions = new ArrayList<>();
		List<Integer> extraSaveOptions = new ArrayList<>();
		for (String path : pair.getBenchInputPaths()) {
			log.debug("adding the following path to benchInputPaths ");
			log.debug(path);
			benchInputPaths.add(path);
		}
		benchInputPaths
				.add(""); // just terminating this array with a blank string so the Bash array will always have some
		// element
		String primaryPreprocessorPath = "";
		boolean stdOutSaveOrExtraSaveEnabled = false;
		for (JoblineStage stage : pair.getStages()) {
			int stageNumber = stage.getStageNumber();
			stageNumbers.add(stageNumber);
			StageAttributes attrs = job.getStageAttributesByStageNumber(stageNumber);
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

			// Check if we're going to need to create a benchmark directory.
			SaveResultsOption stdoutSave = attrs.getStdoutSaveOption();
			SaveResultsOption extraSave = attrs.getExtraOutputSaveOption();
			stdOutSaveOrExtraSaveEnabled =
					stdoutSave == SaveResultsOption.CREATE_BENCH || extraSave == SaveResultsOption.CREATE_BENCH ||
					stdOutSaveOrExtraSaveEnabled;

			stdoutSaveOptions.add(stdoutSave.getVal());
			extraSaveOptions.add(extraSave.getVal());

			if (attrs.getSpaceId() == null) {
				spaceIds.add(null);
			} else {
				Integer spaceId = Spaces.getSubSpaceIDByPath(attrs.getSpaceId(), pair.getPath());
				if (spaceId == null || spaceId == -1) {
					spaceIds.add(null);
				} else {
					spaceIds.add(spaceId);
				}
			}

			// for processors, we still need one entry per stage even though not all stages have processors
			// in the Bash scripts, an empty string will be interpreted as "no processor"
			Processor p = attrs.getPostProcessor();
			if (p == null) {
				postProcessorTimeLimits.add("");
				postProcessorPaths.add("");
			} else {
				postProcessorTimeLimits.add(String.valueOf(p.getTimeLimit()));
				postProcessorPaths.add(p.getFilePath());
			}
			p = attrs.getPreProcessor();
			if (p == null) {
				preProcessorTimeLimits.add("");
				preProcessorPaths.add("");
			} else {
				preProcessorTimeLimits.add(String.valueOf(p.getTimeLimit()));
				preProcessorPaths.add(p.getFilePath());
				if (Objects.equals(stage.getStageNumber(), pair.getPrimaryStageNumber())) {
					primaryPreprocessorPath = p.getFilePath();
				}
			}
		}

		File outputFile = new File(JobPairs.getPairStdout(pair));

		//if there is exactly 1 stage, we use the old output format
		if (stageNumbers.size() == 1) {
			outputFile = outputFile.getParentFile();
		}

		// maps from strings in the jobscript to the strings that should be filled in
		Map<String, String> replacements = new HashMap<>();

		// Create a new bench directory and add it to the template if this job has the stdOutOption or extraSaveOption
		// enabled.
		if (stdOutSaveOrExtraSaveEnabled) {
			log.debug("Pair with id=" + pair.getId() +
			          " had stdout save option or extra save option enabled. Creating benchmark directory.");
			try {
				String benchDirPath;
				if (job.getOutputBenchmarksPath() != null) {
					// Get the directory that has already been created for this job if it exists.
					benchDirPath = job.getOutputBenchmarksPath();
				} else {
					// If the bench directory was only updated this job scheduling cycle it won't in the Job object
					// so we check the DB directly.
					Optional<String> benchDir = Jobs.getOutputBenchmarksPath(job.getId());
					if (benchDir.isPresent()) {
						benchDirPath = benchDir.get();
					} else {
						// Make a new directory for this job if it hasn't been done yet.
						benchDirPath =
								UploadBenchmark.getDirectoryForBenchmarkUpload(job.getUserId(), null)
								.getAbsolutePath();
						Jobs.setOutputBenchmarksPath(job.getId(), benchDirPath);
					}
				}
				replacements.put("$$BENCH_SAVE_PATH$$", benchDirPath);
			} catch (FileNotFoundException e) {
				log.error("Could not get unique benchmark directory.", e);
			}
		} else {
			log.debug("Pair with id=" + pair.getId() + " did not have stdout save option or extra save option enabled.");
		}

		//Dependencies
		if (pair.getBench().getUsesDependencies()) {
			int pairBenchId = pair.getBench().getId();
		/*            log.debug("Benchmark has broken deps:"+ Benchmarks.benchHasBrokenDependencies(pairBenchId));
		    if(Benchmarks.benchHasBrokenDependencies(pairBenchId)) {
                throw new BenchmarkDependencyMissingException(pairBenchId);
		}*/
			replacements.put("$$HAS_DEPENDS$$", "1");
			log.trace("About to get bench dependencies and then write dependency file");
			writeDependencyFile(pair.getId(), Benchmarks.getBenchDependencies(pairBenchId));
		} else {
			replacements.put("$$HAS_DEPENDS$$", "0");
		}
		replacements.put("$$BENCH$$", base64encode(pair.getBench().getPath()));
		replacements.put("$$PAIRID$$", "" + pair.getId());
		replacements.put("$$SPACE_PATH$$", pair.getPath());
		replacements.put("$$PRIMARY_PREPROCESSOR_PATH$$", primaryPreprocessorPath);
		replacements.put("$$PAIR_OUTPUT_DIRECTORY$$", base64encode(outputFile.getAbsolutePath()));
		replacements.put("$$MAX_RUNTIME$$", "" + Util.clamp(1, queue.getWallTimeout(), job.getWallclockTimeout()));
		replacements.put("$$MAX_CPUTIME$$", "" + Util.clamp(1, queue.getCpuTimeout(), job.getCpuTimeout()));
		replacements.put("$$MAX_MEM$$", "" + Util.bytesToMegabytes(job.getMaxMemory()));
		replacements.put("$$BENCH_ID$$", "" + pair.getBench().getId());
		replacements.put("$$SOFT_TIME_LIMIT$$", "" + job.getSoftTimeLimit());
		replacements.put("$$KILL_DELAY$$", "" + job.getKillDelay());

		replacements.put("$$BENCHMARKING_FRAMEWORK$$", job.getBenchmarkingFramework().toString());

		if (job.isBuildJob()) {
			replacements.put("$$BUILD_JOB$$", "true");
		} else {
			replacements.put("$$BUILD_JOB$$", "false");
		}

		// all arrays from above. Note that we are base64 encoding some for safety
		replacements.put("$$CPU_TIMEOUT_ARRAY$$", numsToBashArray("STAGE_CPU_TIMEOUTS", stageCpuTimeouts));
		replacements.put("$$CLOCK_TIMEOUT_ARRAY$$", numsToBashArray("STAGE_CLOCK_TIMEOUTS", stageWallclockTimeouts));
		replacements.put("$$MEM_LIMIT_ARRAY$$", numsToBashArray("STAGE_MEM_LIMITS", stageMemLimits));
		replacements.put("$$STAGE_NUMBER_ARRAY$$", numsToBashArray("STAGE_NUMBERS", stageNumbers));
		replacements.put("$$SOLVER_ID_ARRAY$$", numsToBashArray("SOLVER_IDS", solverIds));
		replacements.put("$$SOLVER_TIMESTAMP_ARRAY$$", toBashArray("SOLVER_TIMESTAMPS", solverTimestamps, false));
		replacements.put("$$CONFIG_NAME_ARRAY$$", toBashArray("CONFIG_NAMES", configNames, false));
		replacements.put("$$PRE_PROCESSOR_PATH_ARRAY$$", toBashArray("PRE_PROCESSOR_PATHS", preProcessorPaths, false));
		replacements.put("$$PRE_PROCESSOR_TIME_LIMIT_ARRAY$$", toBashArray("PRE_PROCESSOR_TIME_LIMITS", preProcessorTimeLimits, false));
		replacements.put("$$POST_PROCESSOR_PATH_ARRAY$$", toBashArray("POST_PROCESSOR_PATHS", postProcessorPaths, false));
		replacements.put("$$POST_PROCESSOR_TIME_LIMIT_ARRAY$$", toBashArray("POST_PROCESSOR_TIME_LIMITS", postProcessorTimeLimits, false));
		replacements.put("$$SPACE_ID_ARRAY$$", numsToBashArray("SPACE_IDS", spaceIds));
		replacements.put("$$SOLVER_NAME_ARRAY$$", toBashArray("SOLVER_NAMES", solverNames, true));
		replacements.put("$$SOLVER_PATH_ARRAY$$", toBashArray("SOLVER_PATHS", solverPaths, true));
		replacements.put("$$BENCH_INPUT_ARRAY$$", toBashArray("BENCH_INPUT_PATHS", benchInputPaths, true));
		replacements.put("$$STAGE_DEPENDENCY_ARRAY$$", toBashArray("STAGE_DEPENDENCIES", argStrings, false));
		replacements.put("$$BENCH_SUFFIX_ARRAY$$", toBashArray("BENCH_SUFFIXES", benchSuffixes, true));
		replacements.put("$$RESULTS_INTERVAL_ARRAY$$", numsToBashArray("RESULTS_INTERVALS", resultsIntervals));
		replacements.put("$$STDOUT_SAVE_OPTION_ARRAY$$", numsToBashArray("STDOUT_SAVE_OPTIONS", stdoutSaveOptions));
		replacements.put("$$EXTRA_SAVE_OPTION_ARRAY$$", numsToBashArray("EXTRA_SAVE_OPTIONS", extraSaveOptions));


		String scriptPath = String.format("%s/%s", R.getJobInboxDir(), String.format(R.JOBFILE_FORMAT, pair.getId()));
		replacements.put("$$SCRIPT_PATH$$", scriptPath);
		replacements.put("$$SUPPRESS_TIMESTAMP_OPTION$$", String.valueOf(job.timestampIsSuppressed()));
		File f = new File(scriptPath);
		log.trace("Adding parameters to jobscript for pair " + pair.getId());
		jobScript = addParametersToJobscript(jobScript, replacements);

		f.delete();
		f.getParentFile().mkdirs();
		f.createNewFile();

		if (!f.setExecutable(true, false) || !f.setReadable(true, false)) {
			log.error(
					"Can't change owner permissions on jobscript file. This will prevent the grid engine from being " +
					"able to open the file. Script path: " + scriptPath);
			return "";
		}

		FileWriter out = new FileWriter(f);
		out.write(jobScript);
		out.close();

		log.trace("writeJobScript finishes for pair " + pair.getId());
		return scriptPath;
	}

	/**
	 * Given a list of pipeline dependencies, this creates a single string containing all of the relevant arguments
	 * so that all the dependencies can be passed to the configuration.
	 *
	 * @param deps The dependencies. Must be ordered by input number to get the correct order
	 * @return The argument string.
	 */
	public static String pipelineDependenciesToArgumentString(List<PipelineDependency> deps) {
		if (deps == null || deps.isEmpty()) {
			return "";
		}
		log.debug("creating a dependency argument string with this many deps = " + deps.size());
		StringBuilder sb = new StringBuilder();
		for (PipelineDependency dep : deps) {
			// SAVED_OUTPUT_DIR and BENCH_INPUT_DIR are variables defined in the jobscript
			if (dep.getType() == PipelineInputType.ARTIFACT) {
				sb.append("\"$SAVED_OUTPUT_DIR/");
				sb.append(dep.getDependencyId());
				sb.append("\" ");

			} else if (dep.getType() == PipelineInputType.BENCHMARK) {
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
	 *
	 * @param arrayName The name to give the array
	 * @param strs The strings to include, in order.
	 * @param base64 True to base64 encode all the strings and false otherwise
	 * @return The array as a String that can be embedded directly into the jobscript.
	 */
	public static String toBashArray(String arrayName, List<String> strs, boolean base64) {
		if (strs.isEmpty()) {
			return "";
		}
		int index = 0;
		StringBuilder sb = new StringBuilder();
		for (String s : strs) {
			if (s == null) {
				s = "";
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
			index = index + 1;
		}
		return sb.toString();
	}

	/**
	 * Creates a String that can be inserted into a Bash script as an array where all the given numbers
	 * are in the array starting from index 0. Null is encoded as a blank string in the array
	 *
	 * @param arrayName The name of the variable holding the array in Bash
	 * @param nums The numbers to insert into the array
	 * @return The string to insert
	 */
	public static <T extends Number> String numsToBashArray(String arrayName, List<T> nums) {
		List<String> strs = new ArrayList<>();
		for (T num : nums) {
			if (num != null) {
				strs.add(num.toString());

			} else {
				strs.add("");
			}
		}
		return toBashArray(arrayName, strs, false);
	}

	/**
	 * Writes a file containing benchmark dependencies ( note: these are NOT related to any of the pipeline
	 * dependencies)
	 * to the jobin directory for the given pair and benchmark.
	 *
	 * @param pairId
	 * @param dependencies
	 * @throws Exception
	 */
	public static void writeDependencyFile(Integer pairId, List<BenchmarkDependency> dependencies) throws
			Exception {
		StringBuilder sb = new StringBuilder();
		String separator = ",,,";
		log.trace("writeDependencyFile begins");
		for (BenchmarkDependency bd : dependencies) {
			sb.append(bd.getSecondaryBench().getPath());
			sb.append(separator);
			sb.append(bd.getDependencyPath());
			sb.append("\n");
		}

		String dependFilePath = String.format("%s/%s", R.getJobInboxDir(), String.format(R.DEPENDFILE_FORMAT, pairId));
		File f = new File(dependFilePath);
		f.createNewFile();

		if (!f.setExecutable(true, false) || !f.setReadable(true, false)) {
			log.error(
					"Can't change owner permissions on job dependencies file. This will prevent the grid engine from" +
					" " +
					"being able to open the file. File path: " + dependFilePath);
			return;
		}
		log.debug("dependencies file = " + sb.toString());
		FileWriter out = new FileWriter(f);
		out.write(sb.toString());
		out.close();
		log.debug("done writing dependency file");
	}

	/**
	 * Sets up the basic information for a job, including the user who created it,
	 * its name and description, the pre- and post-processors, and the queue.
	 * <p>
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
	public static Job setupJob(int userId, String name, String description, int preProcessorId, int postProcessorId,
	                           int queueId, long randomSeed, int cpuLimit, int wallclockLimit, long memLimit,
	                           boolean suppressTimestamp, int resultsInterval, SaveResultsOption otherOutputOption, BenchmarkingFramework framework) {
		log.debug("Setting up job " + name);
		Job j = new Job();

		// Set the job's name, submitter user id and description
		j.setUserId(userId);
		j.setName(name);
		j.setSeed(randomSeed);
		j.setCpuTimeout(cpuLimit);
		j.setWallclockTimeout(wallclockLimit);
		j.setMaxMemory(memLimit);
		if (description != null) {
			j.setDescription(description);
		}

		j.setBenchmarkingFramework(framework);
		// Get queue and processor information from the database and put it in the job
		j.setQueue(Queues.get(queueId));
		StageAttributes attrs = new StageAttributes();
		attrs.setCpuTimeout(cpuLimit);
		attrs.setWallclockTimeout(wallclockLimit);
		attrs.setMaxMemory(memLimit);
		attrs.setStageNumber(1);
		attrs.setSpaceId(null);
		attrs.setResultsInterval(resultsInterval);
		attrs.setExtraOutputSaveOption(otherOutputOption);
		if (preProcessorId > 0) {
			attrs.setPreProcessor(Processors.get(preProcessorId));
		} else {
			attrs.setPreProcessor(null);
		}
		if (postProcessorId > 0) {
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
	 * @param benchmarkIds A list of benchmarks to use in this job
	 * @param configIds A list of configurations (that match in order with solvers) to use for the specified solvers
	 * @param spaceId the id of the space we are adding from
	 */
	public static void buildJob(Job j, List<Integer> benchmarkIds, List<Integer> configIds, Integer spaceId) {
		// Retrieve all the benchmarks included in this job
		List<Benchmark> benchmarks = Benchmarks.get(benchmarkIds);

		// Retrieve all the solvers included in this job
		List<Solver> solvers = Solvers.getWithConfig(configIds);
		String spaceName = "job space";
		String sm = Spaces.getName(spaceId);
		if (sm != null) {
			spaceName = sm;
		}
		// Pair up the solvers and benchmarks
		for (Benchmark bench : benchmarks) {
			for (Solver solver : solvers) {
				JobPair pair = new JobPair();
				pair.setBench(bench);
				JoblineStage stage = new JoblineStage();
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
	 * @param spaceId the id of the space to build the job pairs from
	 * @param path The space path to give to every job pair created by this function
	 * @return an error message if there was a problem, and null otherwise.
	 */
	public static List<JobPair> addJobPairsFromSpace(int spaceId, String path) {
		Space space = Spaces.get(spaceId);
		//log.debug("calling addJobPairsFrom space on space ID = "+spaceId);
		//log.debug("the path for the pairs will be ");
		//log.debug(path);
		List<JobPair> pairs = new ArrayList<>();
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
					JoblineStage stage = new JoblineStage();
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
	 *
	 * @param solverId the id of the unbuilt solver
	 * @param spaceId the space where the solver is, at this point also the space where the job is added
	 * @return int jobId of the job that has been added, or -1 if failed to add job.
	 * @author Andrew Lubinus
	 */

	public static int addBuildJob(Integer solverId, Integer spaceId) {
		Solver s = Solvers.get(solverId);
		log.info("Adding build job for solver " + s.getName() + " in space: " + spaceId);
		Queue q = Queues.getAllQ();
		Job j = JobManager.setupJob(s.getUserId(),
				s.getName() + " Build",
				s.getName() + " Build Job", -1, -1, R.DEFAULT_QUEUE_ID, //This is the same queue referenced
				// by variable q
				0, q.getCpuTimeout(), q
						.getWallTimeout(), R.DEFAULT_PAIR_VMEM, false, 15, SaveResultsOption.SAVE, R.DEFAULT_BENCHMARKING_FRAMEWORK);

		j.setBuildJob(true);
		String spaceName = "job space";
		String sm = Spaces.getName(spaceId);
		if (sm != null) {
			spaceName = sm;
		}
		Configuration c = new Configuration();
		c.setName("starexec_build");
		c.setSolverId(solverId);
		c.setDescription("Build Configuration for solver: " + solverId);
		int cId = Solvers.addConfiguration(s, c);
		JobPair pair = new JobPair();
		JoblineStage stage = new JoblineStage();
		stage.setStageNumber(1);
		pair.setPrimaryStageNumber(1);
		stage.setNoOp(false);
		stage.setSolver(s);
		stage.setConfiguration(c);
		int bench = UploadBenchmark
				.addBenchmarkFromText("dummy benchmark", "starexec_build", s.getUserId(), R.NO_TYPE_PROC_ID, true);
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
	 * @param configIds a list of configurations to use
	 * @param path The path to use for each job pair created.
	 * @return A HashMap that maps space IDs to all the job pairs in that space. These can then be added to a job in
	 * any
	 * desirable order
	 */
	public static List<JobPair> addJobPairsFromSpace(int spaceId, String path, List<Integer> configIds) {
		try {
			List<Solver> solvers = Solvers.getWithConfig(configIds);
			List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);

			// Pair up the solvers and benchmarks
			List<JobPair> curPairs = new ArrayList<>();
			for (Benchmark bench : benchmarks) {
				for (Solver solver : solvers) {
					JobPair pair = new JobPair();
					pair.setBench(bench);
					JoblineStage stage = new JoblineStage();
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
			log.error("addJobPairsFromSpace", e);
		}
		return null;

	}

	/**
	 * Adds job pairs to a job object in a depth-first manner. All pairs from space1 are added,
	 * then all pairs from space2, and so on
	 *
	 * @param j The job object to add the job pairs to
	 * @param spaceToPairs A mapping from spaces to lists of job pairs in that space
	 */
	public static void addJobPairsDepthFirst(Job j, Map<Integer, List<JobPair>> spaceToPairs) {
		for (Integer spaceId : spaceToPairs.keySet()) {
			log.debug("adding this many pairs from space id = " + spaceId + " " + spaceToPairs.get(spaceId).size());
			j.addJobPairs(spaceToPairs.get(spaceId));
		}
	}

	/**
	 * Adds job pairs to a job object in a breadth-first manner. One pair from space1 is added,
	 * then one pairs from space2, and so on until every pair from every space has been added.
	 *
	 * @param j The job object to add the job pairs to
	 * @param spaceToPairs A mapping from spaces to lists of job pairs in that space
	 */
	public static void addJobPairsRoundRobin(Job j, Map<Integer, List<JobPair>> spaceToPairs) {
		try {
			int index = 0;
			while (!spaceToPairs.isEmpty()) {
				Set<Integer> keys = spaceToPairs.keySet();
				Set<Integer> keysToRemove = new HashSet<>();
				for (Integer spaceId : keys) {
					//if there is at least one pair left in this space
					if (spaceToPairs.get(spaceId).size() > index) {
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
			log.error("addJobPairsRoundRobin", e);
		}

	}

	/**
	 * With the given solvers and configurations, will find all benchmarks in the current space hierarchy
	 * and create job pairs from the result. Will then return those job pairs
	 *
	 * @param spaceId the id of the space we start in
	 * @param userId the id of the user creating the job
	 * @param configIds a list of configurations to use
	 * @param SP A mapping from space IDs to the path of the space rooted at "spaceId"
	 * @return A HashMap that maps space IDs to all the job pairs in that space. These can then be added to a job in
	 * any
	 * desirable order
	 */
	public static Map<Integer, List<JobPair>> addBenchmarksFromHierarchy(int spaceId, int userId, List<Integer>
			configIds, HashMap<Integer, String> SP) {
		try {
			Map<Integer, List<JobPair>> spaceToPairs = new HashMap<>();
			List<Solver> solvers = Solvers.getWithConfig(configIds);
			List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(spaceId, userId));
			spaces.add(Spaces.get(spaceId));

			// Pair up the solvers and benchmarks
			for (Space s : spaces) {
				List<Benchmark> benchmarks = Benchmarks.getBySpace(s.getId());
				log.debug("found this many benchmarks for space id = " + s.getId() + " " + benchmarks.size());
				List<JobPair> curPairs = new ArrayList<>();
				for (Benchmark bench : benchmarks) {
					for (Solver solver : solvers) {
						JobPair pair = new JobPair();
						pair.setBench(bench);
						JoblineStage stage = new JoblineStage();
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
			log.error("addBenchmarksFromHierarchy", e);
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

	static class SchedulingState {
		final Job job;
		final String jobTemplate;
		final Iterator<JobPair> pairIter;

		SchedulingState(Job _job, String _jobTemplate, Iterator<JobPair> _pairIter) {
			job = _job;
			jobTemplate = _jobTemplate;
			pairIter = _pairIter;
		}
	}

}
