package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Analytics;
import org.starexec.data.database.*;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.Queue;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.data.to.pipelines.StageAttributes.SaveResultsOption;
import org.starexec.jobs.JobManager;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;
import org.starexec.app.RESTHelpers;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Servlet which handles incoming requests to create new jobs
 *
 * @author Tyler Jensen
 */
public class CreateJob extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(CreateJob.class);

	// Request attributes
	private static final String name = "name";
	private static final String description = "desc";
	private static final String postProcessor = "postProcess";
	private static final String preProcessor = "preProcess";
	private static final String workerQueue = "queue";
	private static final String configs = "configs";
	private static final String run = "runChoice";
	private static final String benchChoice = "benchChoice";
	private static final String cpuTimeout = "cpuTimeout";
	private static final String clockTimeout = "wallclockTimeout";
	private static final String spaceId = "sid";
	private static final String traversal = "traversal";
	private static final String pause = "pause";
	private static final String maxMemory = "maxMem";
	private static final String randSeed = "seed";
	private static final String resultsInterval = "resultsInterval";
	private static final String otherOutputOption = "saveOtherOutput";
	private static final String killDelay = "killDelay";
	private static final String softTimeLimit = "softTimeLimit";

	//unique to quick jobs
	private static final String benchProcessor = "benchProcess";
	private static final String benchName = "benchName";

	/**
	 * Creates a job which is a flat job with only a single solver and benchmark. Every configuration is run on the
	 * benchmark, so the number of job pairs is equal to the number of configurations in the solver
	 *
	 * @param j A job object for the job, which must have the following attributes set: userId, pre processor, post
	 * processor, queue, name, description, seed
	 * @param solverId The ID of the solver that will be run
	 * @param benchId The ID of the benchmark that will be run
	 * @param sId The ID of the space to put the job in
	 */
	public static void buildQuickJob(Job j, int solverId, int benchId, Integer sId) {
		List<Integer> benchIds = new ArrayList<>();
		benchIds.add(benchId);
		buildQuickJob(j, solverId, benchIds, sId);
	}

	/**
	 * Creates a quick job, Every configuration is run on the benchmark, so the number of job pairs is equal to the
	 * number of configurations in the solver
	 *
	 * @param j A job object for the job, which must have the following attributes set: userId, pre processor, post
	 * processor, queue, name, description, seed
	 * @param solverId The ID of the solver that will be run
	 * @param benchmarkIds The IDs of the benchmarks that will be run
	 * @param sId The ID of the space to put the job in
	 */
	public static void buildQuickJob(Job j, int solverId, List<Integer> benchmarkIds, Integer sId) {
		//Setup the job's attributes

		List<Configuration> config = Solvers.getConfigsForSolver(solverId);
		List<Integer> configIds = new ArrayList<>();
		for (Configuration c : config) {
			if (!c.getName().equals("starexec_build")) {
				configIds.add(c.getId());
			}
		}
		JobManager.buildJob(j, benchmarkIds, configIds, sId);
	}

	/**
	 * Tests a solver using default info for the space it is being uploaded in
	 *
	 * @param solverId ID of the solver ot put the job in.
	 * @param spaceId Id of the space to put the job in
	 * @param userId Id of the user that will make this job
	 * @param settingsId ID of the default settings profile to use for the job
	 * @return The ID of the job that was newly created, or -1 on error
	 */
	public static int buildSolverTestJob(int solverId, int spaceId, int userId, int settingsId) throws SQLException {
		Solver s = Solvers.get(solverId);
		DefaultSettings settings = Settings.getProfileById(settingsId);
		int preProcessorId = ((settings.getPreProcessorId() == null) ? -1 : settings.getPreProcessorId());
		int postProcessorId = ((settings.getPostProcessorId() == null) ? -1 : settings.getPreProcessorId());
		Job j = JobManager
				.setupJob(userId, s.getName(), "test job for new solver " + s.getName() + " " + "(" + s.getId() + ")",
				          preProcessorId, postProcessorId, Queues.getTestQueue(), 0, settings.getCpuTimeout(),
				          settings.getWallclockTimeout(), settings.getMaxMemory(), false, 0, SaveResultsOption.SAVE,
				          R.DEFAULT_BENCHMARKING_FRAMEWORK
				);

		buildQuickJob(j, solverId, settings.getBenchIds(), spaceId);
		boolean submitSuccess = Jobs.add(j, spaceId);
		if (submitSuccess) {
			return j.getId();
		}
		return -1; //error
	}

	/**
	 * Validates that the given parameters are allowable for the given queue and user.
	 *
	 * @param userId The ID of the user trying to create a job. Invalid if this user does not have permission to use
	 * the
	 * given queue.
	 * @param queueId The queue being used.
	 * @param cpuLimit The CPU timeout for the job. Invalid if it exceeds the queue limit. Not checked if null.
	 * @param wallclockLimit The wallclock timeout for the job. Invalid if it exceeds the queue limit. Not checked if
	 * null.
	 * @param preProcId The ID of the pre processor being used. Invalid if the given user cannot use it.
	 * @param postProcId The ID of the post processor being used. Invalid if the given user cannot use it.
	 * @return A ValidatorStatusCode with a human readable error message if invalid.
	 */
	public static ValidatorStatusCode isValid(
			int userId, int queueId, Integer cpuLimit, Integer wallclockLimit, Integer preProcId, Integer postProcId
	) {
		List<Queue> userQueues = Queues.getUserQueues(userId);
		Boolean queueFound = false;
		for (Queue queue : userQueues) {
			if (queue.getId() == queueId) {
				queueFound = true;
				break;
			}
		}

		if (!queueFound) {
			return new ValidatorStatusCode(false, "The given queue does not exist or you do not have access to it");
		}

		Queue q = Queues.get(queueId);
		if (wallclockLimit != null && wallclockLimit > q.getWallTimeout()) {
			return new ValidatorStatusCode(false,
			                               "The given wallclock timeout exceeds the maximum allowed for this queue, " +
					                               "which is " +
					                               q.getWallTimeout()
			);
		}

		if (cpuLimit != null && cpuLimit > q.getCpuTimeout()) {
			return new ValidatorStatusCode(false,
			                               "The given cpu timeout exceeds the maximum allowed for this queue, which is" +
					                               " " +
					                               q.getCpuTimeout()
			);
		}

		if (preProcId != null) {
			if (!ProcessorSecurity.canUserSeeProcessor(preProcId, userId).isSuccess()) {
				return new ValidatorStatusCode(false,
				                               "You do not have permission to use the given preprocessor, or it" +
						                               " does not exist"
				);
			}
		}
		if (postProcId != null) {
			if (!ProcessorSecurity.canUserSeeProcessor(postProcId, userId).isSuccess()) {
				return new ValidatorStatusCode(false, "You do not have permission to use the given postprocessor, or" +
						" " +
						"it does not exist");
			}
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String method = "doPost";
		log.debug(method, "starting job post");
		try {
			log.debug("made it to submit job");
			if (RESTHelpers.getReadOnly()) {
				response.sendError(HttpServletResponse.	SC_SERVICE_UNAVAILABLE, "Read only mode is enabled, no new jobs can be created.");
				return;
			}
			// Make sure the request is valid
			ValidatorStatusCode status = isValid(request);
			if (!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				log.debug(method, "received an invalid job creation request");
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
				return;
			}

			// Check if user can use BenchExec
			int userId = SessionUtil.getUserId(request);
			BenchmarkingFramework framework =
					BenchmarkingFramework.valueOf(request.getParameter(R.BENCHMARKING_FRAMEWORK_OPTION));

			int space = Integer.parseInt((String) request.getParameter(spaceId));
			DefaultSettings settings = Communities.getDefaultSettings(space);
			// next parameters are optional, and are set to the community defaults if not present
			int cpuLimit = 0;
			if (Util.paramExists(cpuTimeout, request)) {
				cpuLimit = Integer.parseInt((String) request.getParameter(cpuTimeout));
			} else {
				cpuLimit = settings.getCpuTimeout();
			}

			int runLimit = 0;
			if (Util.paramExists(clockTimeout, request)) {
				runLimit = Integer.parseInt((String) request.getParameter(clockTimeout));
			} else {
				runLimit = settings.getWallclockTimeout();
			}

			long memoryLimit = 0;
			if (Util.paramExists(maxMemory, request)) {
				memoryLimit = Util.gigabytesToBytes(Double.parseDouble(request.getParameter(maxMemory)));
			} else {
				memoryLimit = settings.getMaxMemory();
			}

			int resultsIntervalNum = 0;
			if (Util.paramExists(resultsInterval, request)) {
				resultsIntervalNum = Integer.parseInt(request.getParameter(resultsInterval));
			}

			//a number that will be provided to the pre processor for every job pair in this job
			long seed = Long.parseLong(request.getParameter(randSeed));

			//ensure that the cpu limits are greater than 0
			cpuLimit = (cpuLimit <= 0) ? R.DEFAULT_MAX_TIMEOUT : cpuLimit;
			runLimit = (runLimit <= 0) ? R.DEFAULT_MAX_TIMEOUT : runLimit;

			//memory is in units of bytes
			memoryLimit = (memoryLimit <= 0) ? R.DEFAULT_PAIR_VMEM : memoryLimit;

			boolean suppressTimestamp = false;
			if (Util.paramExists(R.SUPPRESS_TIMESTAMP_INPUT_NAME, request)) {
				suppressTimestamp = request.getParameter(R.SUPPRESS_TIMESTAMP_INPUT_NAME).equals("yes");
			}
			log.debug(method, "User chose " + (suppressTimestamp ? "" : "not ") + "to suppress timestamps.");

			SaveResultsOption option = SaveResultsOption.SAVE;

			if (Util.paramExists(otherOutputOption, request)) {
				if (!Boolean.parseBoolean(request.getParameter(otherOutputOption))) {
					option = SaveResultsOption.NO_SAVE;
				}
			}

			//Setup the job's attributes
			Job j = JobManager
					.setupJob(userId, (String) request.getParameter(name), (String) request.getParameter(description),
					          Integer.parseInt((String) request.getParameter(preProcessor)),
					          Integer.parseInt((String) request.getParameter(postProcessor)),
					          Integer.parseInt((String) request.getParameter(workerQueue)), seed, cpuLimit, runLimit,
					          memoryLimit, suppressTimestamp, resultsIntervalNum, option, framework
					);

			try {
				if (framework == BenchmarkingFramework.RUNSOLVER) { // runsolver specific settings
					int d = Integer.valueOf(request.getParameter(killDelay));
					if (d > 0 && d < R.MAX_KILL_DELAY) {
						j.setKillDelay(d);
					}
				} else if (framework == BenchmarkingFramework.BENCHEXEC) { // BenchExec specific settings
					int d = Integer.valueOf(request.getParameter(softTimeLimit));
					if (d > 0) {
						j.setSoftTimeLimit(d);
					}
				}
			} catch (NumberFormatException e) {
				log.debug(method, "No killDelay/softTimeLimit provided");
			}

			String selection = request.getParameter(run);
			String benchMethod = request.getParameter(benchChoice);
			String traversalMethod = request.getParameter(traversal);
			//Depending on our run selection, handle each case differently
			//if the user created a quickJob, they uploaded a single text benchmark and a solver to run
			switch (selection) {
				case "quickJob":
					int solverId = Integer.parseInt(request.getParameter(R.SOLVER));
					String benchText = request.getParameter(R.BENCHMARK);
					String bName = request.getParameter(benchName);
					int benchProc = Integer.parseInt(request.getParameter(benchProcessor));
					int benchId = UploadBenchmark.addBenchmarkFromText(benchText, bName, userId, benchProc, false);
					log.debug(method, "new benchmark created for quickJob with id = " + benchId);
					buildQuickJob(j, solverId, benchId, space);
					break;
				case "keepHierarchy": {
					log.debug(method, "User selected keepHierarchy");
					//Create the HashMap to be used for creating job-pair path
					HashMap<Integer, String> SP =
							Spaces.spacePathCreate(userId, Spaces.getSubSpaceHierarchy(space, userId), space);
					List<Space> spaces = Spaces.trimSubSpaces(userId,
					                                          Spaces.getSubSpaceHierarchy(space, userId)
					); //Remove spaces the user is not a member of

					log.debug(method, "got all the subspaces for the job");
					spaces.add(0, Spaces.get(space));

					HashMap<Integer, List<JobPair>> spaceToPairs = new HashMap<>();
					for (Space s : spaces) {
						List<JobPair> pairs = JobManager.addJobPairsFromSpace(s.getId(), SP.get(s.getId()));

						spaceToPairs.put(s.getId(), pairs);
					}
					log.debug(method, "added all the job pairs from every space");

					//if we're doing "depth first", we just add all the pairs from space1, then all the pairs from
					// space2,
					// and so on
					if (traversalMethod.equals("depth")) {
						JobManager.addJobPairsDepthFirst(j, spaceToPairs);
						//otherwise, we are doing "breadth first", so we interleave pairs from all the spaces
					} else {
						log.debug(method, "adding pairs round robin");
						JobManager.addJobPairsRoundRobin(j, spaceToPairs);
					}
					break;
				}
				default: { //user selected "choose"

					HashMap<Integer, String> SP =
							Spaces.spacePathCreate(userId, Spaces.getSubSpaceHierarchy(space, userId), space);
					List<Integer> configIds = Util.toIntegerList(request.getParameterValues(configs));

					switch (benchMethod) {
					case "runAllBenchInSpace":
						List<JobPair> pairs =
								JobManager.addJobPairsFromSpace(space, Spaces.getName(space), configIds);
						if (pairs != null) {
							j.addJobPairs(pairs);
						}
						break;
					case "runAllBenchInHierarchy":
						log.debug(method, "got request to run all in bench hierarchy");

						Map<Integer, List<JobPair>> spaceToPairs = JobManager
								.addBenchmarksFromHierarchy(Integer.parseInt(request.getParameter(spaceId)),
															SessionUtil.getUserId(request), configIds, SP
								);

						if (traversalMethod.equals("depth")) {
							log.debug(method, "User selected depth-first traversal");
							JobManager.addJobPairsDepthFirst(j, spaceToPairs);
						} else {
							log.debug(method, "users selected round robin traversal");
							JobManager.addJobPairsRoundRobin(j, spaceToPairs);
						}
						break;
					default:
						List<Integer> benchmarkIds = Util.toIntegerList(request.getParameterValues(R.BENCHMARK));
						JobManager.buildJob(j, benchmarkIds, configIds, space);
						break;
					}
					break;
				}
			}

			int pairCount = j.getJobPairs().size();
			if (j.getJobPairs().isEmpty()) {
				String message = "Error: no job pairs created for the job. Could not proceed with job submission.";
				Space jobSpace = Spaces.getDetails(space, userId);
				if (jobSpace.getSubspaces().isEmpty()) {
					if (jobSpace.getSolvers().isEmpty()) {
						message =
								"Error: no job pairs created for the job. There are no solvers in this space. Could " +
										"not proceed with job submission.";
					} else if (jobSpace.getBenchmarks().isEmpty()) {
						message = "Error: no job pairs created for the job. There are no benchmarks in this space. " +
								"Could" + " not proceed with job submission.";
					}
				} else if (!Spaces.configBenchPairExistsInHierarchy(space, userId)) {
					message = "Error: no job pairs created for the job. There are no valid solver benchmark pairs in" +
							" " + "this space hierarchy. Could not proceed with job submission.";
				}
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				// No pairs in the job means something went wrong; error out
				return;
			}

			User u = Users.get(userId);
			int pairsAvailable = Math.max(0, u.getPairQuota() - Jobs.countPairsByUser(userId));
			/* This just checks if a quota is totally full, which is sufficient for quick jobs and as a fast sanity
			check for full jobs. After the number of pairs have been acquired for a full job this check will be done
			factoring them in. */
			if (pairsAvailable < pairCount) {
				String message =
						"Error: You are trying to create " + pairCount + " pairs, but you have " + pairsAvailable +
								" remaining in your quota. Please delete some old jobs before continuing.";
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				// No pairs in the job means something went wrong; error out
				return;
			}

			boolean submitSuccess = Jobs.add(j, space);
			String start_paused = request.getParameter(pause);

			//if the user chose to immediately pause the job
			if (start_paused.equals("yes")) {
				Jobs.pause(j.getId());
			}

			if (submitSuccess) {
				// If submission was successful and user requested to be
				// notified upon job completion, subscribe user to Job
				// notifications
				if (Util.paramExists("subscribe", request) && request.getParameter("subscribe").equals("yes")) {
					try {
						Notifications.subscribeUserToJob(userId, j.getId());
					} catch (SQLException e) {
						// Could not subscribe user to job. So what?
						log.error("Could not subscribe user " + userId + " to job " + j.getId());
					}
				}

				// If the submission was successful, send back to space explorer
				response.addCookie(new Cookie("New_ID", String.valueOf(j.getId())));

				if (selection.equals("quickJob")) {
					Analytics.JOB_CREATE_QUICKJOB.record(userId);
					response.sendRedirect(Util.docRoot("secure/details/job.jsp?id=" + j.getId()));
				} else {
					response.sendRedirect(Util.docRoot("secure/details/job.jsp?id=" + j.getId()));
				}
			} else { // if not submitSuccess
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Your job failed to submit for an unknown reason. Please try again."
				);
			}
		} 
		catch (SQLException sqle) {
			log.error("Caught SQLException in CreateJob.doPost: " + sqle.getMessage());
		} catch (Exception e) {
			log.warn(method, "Caught Exception in CreateJob.doPost.", e);
			throw e;
			
		}
	}

	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters and content
	 * length requirements to ensure it is not malicious.
	 *
	 * @param request The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValid(HttpServletRequest request) {
		try {
			// Make sure the parent space id is a int
			if (!Validator.isValidPosInteger(request.getParameter(spaceId))) {
				return new ValidatorStatusCode(false, "The given space ID needs to be a valid integer");
			}

			if (!Util.paramExists(R.BENCHMARKING_FRAMEWORK_OPTION, request)) {
				return new ValidatorStatusCode(false,
				                               "You must specify which benchmarking framework you want to use" + "."
				);
			}


			int userId = SessionUtil.getUserId(request);
			if (Users.isDiskQuotaExceeded(userId)) {
				return new ValidatorStatusCode(
						false, "Your disk quota has been exceeded: please clear out some old " +
						"solvers, jobs, or benchmarks before proceeding");
			}
			int sid = Integer.parseInt(request.getParameter(spaceId));
			// Make sure the user has access to the space
			Permission perm = Permissions.get(userId, sid);
			if (sid >= 0) {
				if (perm == null || !perm.canAddJob()) {
					return new ValidatorStatusCode(false, "You do not have permission to add jobs in this space");
				}
			}

			// Make sure timeout an int
			if (Util.paramExists(clockTimeout, request) &&
					!Validator.isValidPosInteger(request.getParameter(clockTimeout))) {
				return new ValidatorStatusCode(false, "The given wallclock timeout needs to be a valid integer");
			}

			if (Util.paramExists(cpuTimeout, request) &&
					!Validator.isValidPosInteger(request.getParameter(cpuTimeout))) {
				return new ValidatorStatusCode(false, "The given cpu timeout needs to be a valid integer");
			}

			if (Util.paramExists(maxMemory, request) && !Validator.isValidPosDouble(request.getParameter(maxMemory))) {
				return new ValidatorStatusCode(false, "The given maximum memory needs to be a positive number " +
						"(decimals are permitted)");
			}
			if (!Util.paramExists(clockTimeout, request) || !Util.paramExists(cpuTimeout, request) ||
					!Util.paramExists(maxMemory, request)) {
				DefaultSettings s = Communities.getDefaultSettings(Integer.parseInt(request.getParameter(spaceId)));
				if (s == null) {
					return new ValidatorStatusCode(
							false, "There is no settings profile for the current space, " +
							"so time and memory limits must be specified");
				}
			}

			if (!Validator.isValidLong(request.getParameter(randSeed))) {
				return new ValidatorStatusCode(false, "The random seed needs to be a valid long integer");
			}
			if (Util.paramExists(resultsInterval, request)) {
				if (!Validator.isValidPosInteger(request.getParameter(resultsInterval))) {
					return new ValidatorStatusCode(false,
					                               "The interval for obtaining results must be greater than or " +
							                               "equal to 0"
					);
				}
				int i = Integer.parseInt(request.getParameter(resultsInterval));
				if (i != 0 && i < R.MINIMUM_RESULTS_INTERVAL) {
					return new ValidatorStatusCode(false, "The interval for obtaining results must be at least " +
							R.MINIMUM_RESULTS_INTERVAL + " seconds");
				}
			}
			Integer preProc = null;
			Integer postProc = null;
			// If processors are specified, make sure they're valid ints
			if (Util.paramExists(postProcessor, request)) {

				if (!Validator.isValidInteger(request.getParameter(postProcessor))) {
					return new ValidatorStatusCode(false, "The given post processor ID needs to be a valid integer");
				}
				postProc = Integer.parseInt(request.getParameter(postProcessor));
				//means none was selected
				if (postProc < 1) {
					postProc = null;
				}
			}

			if (Util.paramExists(preProcessor, request)) {
				if (!Validator.isValidInteger(request.getParameter(preProcessor))) {
					return new ValidatorStatusCode(false, "The given pre processor ID needs to be a valid integer");
				}
				preProc = Integer.parseInt(request.getParameter(preProcessor));
				if (preProc < 1) {
					preProc = null;
				}
			}

			// Make sure the queue is a valid integer
			if (!Validator.isValidPosInteger(request.getParameter(workerQueue))) {
				return new ValidatorStatusCode(false, "The given queue ID needs to be a valid integer");
			}
			// Make sure the queue is a valid selection and user has access to it
			Integer queueId = Integer.parseInt(request.getParameter(workerQueue));
			// Ensure the job description is valid
			if (!Validator.isValidPrimDescription((String) request.getParameter(description))) {
				return new ValidatorStatusCode(false,
				                               "The given description is invalid, please see the help files to " +
						                               "see the valid format"
				);
			}
			if (!Util.paramExists(run, request)) {
				return new ValidatorStatusCode(false, "You need to select a run choice for this job");
			}
			if (Util.paramExists(otherOutputOption, request)) {
				if (!Validator.isValidBool(request.getParameter(otherOutputOption))) {
					return new ValidatorStatusCode(false,
					                               "Whether to save extra output files needs to be a valid " +
							                               "boolean"
					);
				}
			}

			if (request.getParameter(run).equals("quickJob")) {
				//we only need to check to see if the space is valid if a space was actually specified
				if (!Util.paramExists(R.BENCHMARK, request)) {
					return new ValidatorStatusCode(false, "You need to select a benchmark to run a quick job");
				}

				if (!Validator.isValidPosInteger(request.getParameter(R.SOLVER))) {
					return new ValidatorStatusCode(false, "The given solver ID is not a valid integer");
				}
				int solverId = Integer.parseInt(request.getParameter(R.SOLVER));
				if (!Permissions.canUserSeeSolver(solverId, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to see the given solver ID");
				}

				if (Solvers.getConfigsForSolver(solverId).isEmpty()) {
					return new ValidatorStatusCode(false, "The given solver does not have any configurations");
				}
			}


			// Only need these checks if we're choosing which solvers and benchmarks to run.
			// In any other case, we automatically get them so we don't have to pass them
			// as part of the request.
			if (request.getParameter(run).equals("choose")) {

				// Check to see if we have a valid list of benchmark ids
				if (request.getParameter(benchChoice).equals("runChosenFromSpace")) {
					if (!Validator.isValidIntegerList(request.getParameterValues(R.BENCHMARK))) {
						return new ValidatorStatusCode(false, "All selected benchmark IDs need to be valid integers");
					}
				}
				List<Integer> benchmarkIds = Util.toIntegerList(request.getParameterValues(R.BENCHMARK));
				if (request.getParameter(benchChoice).equals("runChosenFromSpace") && benchmarkIds.isEmpty()) {
					return new ValidatorStatusCode(false, "You need to chose at least one benchmark to run a job");
				}
				// Make sure the user is using benchmarks they can see
				if (!Permissions.canUserSeeBenchs(benchmarkIds, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use one or more of the " +
							"benchmarks you have selected");
				}

				// Check to see if we have a valid list of configuration ids
				if (!Validator.isValidIntegerList(request.getParameterValues(configs))) {
					if (request.getParameterValues(configs) == null) {
						return new ValidatorStatusCode(false,
					                               "You need to select at least one configuration to run a " + "job"
					);
					} 
					return new ValidatorStatusCode(false, "All selected configuration IDs need to be valid integers");
				}

				Set<Integer> solverIds = new HashSet<>();
				for (Integer cid : Util.toIntegerList(request.getParameterValues(configs))) {
					solverIds.add(Solvers.getSolverByConfig(cid, false).getId());
				}
				
				// Make sure the user is using solvers they can see
				if (!Permissions.canUserSeeSolvers(solverIds, userId)) {
					return new ValidatorStatusCode(false,
					                               "You do not have permission to use all of the selected " + "solvers"
					);
				}
			}


			//make sure both timeouts are <= the queue settings
			Integer cpuLimit = null;
			if (Util.paramExists(cpuTimeout, request)) {
				cpuLimit = Integer.parseInt(request.getParameter(cpuTimeout));
			}
			Integer runLimit = null;
			if (Util.paramExists(clockTimeout, request)) {
				runLimit = Integer.parseInt(request.getParameter(clockTimeout));
			}

			if (Util.paramExists(killDelay, request)) {
				if (!Validator.isValidPosInteger(request.getParameter(killDelay))) {
					return new ValidatorStatusCode(false, "killDelay must be a positive integer");
				} else if (Integer.parseInt(request.getParameter(killDelay)) > R.MAX_KILL_DELAY) {
					return new ValidatorStatusCode(false, "killDelay must not exceed " + R.MAX_KILL_DELAY);
				}
			}

			if (Util.paramExists(softTimeLimit, request) &&
					!Validator.isValidPosInteger(request.getParameter(softTimeLimit))) {
				return new ValidatorStatusCode(false, "softTimeLimit must be a positive integer");
			}

			// Passed all type checks-- next we check permissions
			return CreateJob.isValid(userId, queueId, cpuLimit, runLimit, preProc, postProc);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error creating job");
	}
}
