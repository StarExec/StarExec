package org.starexec.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.jobs.JobManager;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;
/**
 * Creates a class to keep track of the Benchmark-Solver-Configuration Pairs
 * @author kais_wyatt
 */
class BSC {
    List<Benchmark> b;
    List<Solver> s;
    HashMap<Solver, List<Configuration>> sc;

    BSC (List<Benchmark> b, List<Solver> s, HashMap<Solver, List<Configuration>> sc) {
        this.b = b;
        this.s = s;
        this.sc = sc;
    }
}

/**
 * Servlet which handles incoming requests to create new jobs
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class CreateJob extends HttpServlet {		
	private static final Logger log = Logger.getLogger(CreateJob.class);	

	// Request attributes	
	private static final String name = "name";
	private static final String description = "desc";
	private static final String postProcessor = "postProcess";
	private static final String preProcessor = "preProcess";
	private static final String workerQueue = "queue";
	private static final String configs = "configs";
	private static final String run = "runChoice";
	private static final String benchChoice = "benchChoice";
	private static final String benchmarks = "bench";
	private static final String cpuTimeout = "cpuTimeout";
	private static final String clockTimeout = "wallclockTimeout";
	private static final String spaceId = "sid";
	private static final String traversal = "traversal";
	private static final String pause = "pause";
	private static final String maxMemory="maxMem";
	private static final String randSeed="seed";

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		// Make sure the request is valid
		ValidatorStatusCode status=isValid(request);
		if(!status.isSuccess()) {
			//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
			log.debug("received and invalid job creation request");
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
			return;
		}		

		int cpuLimit = Integer.parseInt((String)request.getParameter(cpuTimeout));
		int runLimit = Integer.parseInt((String)request.getParameter(clockTimeout));
		long memoryLimit=Util.gigabytesToBytes(Double.parseDouble(request.getParameter(maxMemory)));
		long seed=Long.parseLong(request.getParameter(randSeed));
		
		cpuLimit = (cpuLimit <= 0) ? R.MAX_PAIR_CPUTIME : cpuLimit;
		runLimit = (runLimit <= 0) ? R.MAX_PAIR_RUNTIME : runLimit;
		
		//memory is in units of bytes
		memoryLimit = (memoryLimit <=0) ? R.DEFAULT_PAIR_VMEM : memoryLimit;
		log.debug("memoryLimit = 0"+memoryLimit);
		int space = Integer.parseInt((String)request.getParameter(spaceId));
		int userId = SessionUtil.getUserId(request);


		//Setup the job's attributes
		Job j = JobManager.setupJob(
				userId,
				(String)request.getParameter(name), 
				(String)request.getParameter(description),
				Integer.parseInt((String)request.getParameter(preProcessor)),
				Integer.parseInt((String)request.getParameter(postProcessor)), 
				Integer.parseInt((String)request.getParameter(workerQueue)),
				seed);
		j.setPrimarySpace(space);
		//Create the HashMap to be used for creating job-pair path
		log.debug("started building the new job");
		HashMap<Integer, String> SP =  Spaces.spacePathCreate(userId, Spaces.getSubSpaceHierarchy(space, userId), space);
		log.debug("HASHMAP = " + SP);
		
		String selection = request.getParameter(run);
		String benchMethod = request.getParameter(benchChoice);
		String traversal2 = request.getParameter(traversal);
		//Depending on our run selection, handle each case differently
		String err = null;
		if (selection.equals("runAllBenchInSpace")) {
		    err = JobManager.addJobPairsFromSpace(j, userId, cpuLimit, runLimit, memoryLimit, space, SP);
		} else if (selection.equals("keepHierarchy")) {
			log.debug("User selected keepHierarchy");

			List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(space, userId)); //Remove spaces the user is not a member of
			log.debug("got all the subspaces for the job");		
			spaces.add(0, Spaces.get(space));
			if (traversal2.equals("depth")) {
				for (Space s : spaces) {
				    err = JobManager.addJobPairsFromSpace(j, userId, cpuLimit, runLimit, memoryLimit, 
									  s.getId(), SP);
				    if (err != null)
					break;
				}
				log.debug("added all the job pairs from every space");
			} else {
				log.debug("User Selected Round-Robin Search");
				int max = 0;
				HashMap<Space,BSC> SpaceToBSC = new HashMap<Space,BSC>();
				for (Space s : spaces) {
					int space_id = s.getId();
					List<Benchmark> benchmarks = Benchmarks.getBySpace(space_id);
					int temp = benchmarks.size();
					if (temp > max) {
						max = temp;
					}
					List<Solver> solvers = Solvers.getBySpace(space_id);
					List<Configuration> configurations = null;
					
					HashMap<Solver,List<Configuration>> SC = new HashMap<Solver, List<Configuration>>();
					for (Solver so : solvers) {
						configurations = Solvers.getConfigsForSolver(so.getId());
						SC.put(so, configurations);
					}
					SpaceToBSC.put(s, new BSC(benchmarks, solvers, SC));
				}
				log.debug("Max size is: " + max);
				for(int i=0; i < max; i++) {
					for (Space s : spaces) {
						BSC bsc = SpaceToBSC.get(s);
						if (bsc.b.size() > i) {
							log.debug("Calling addJobPairsRobin function: i= " + i);
							err = JobManager.addJobPairsRobin(j, userId, cpuLimit, runLimit,
											  memoryLimit, s.getId(), bsc.b.get(i), 
											  bsc.s, bsc.sc, SP);
							if (err != null)
							    break;
						}
					}
					if (err != null)
					    break;
				}
			}
		} else { //hierarchy OR choice
			List<Integer> configIds = Util.toIntegerList(request.getParameterValues(configs));
			
			//TODO: Put in validation code
			if (configIds.size() == 0) {
				// Either no solvers or no configurations; error out
				String message="Either no solvers/configurations were selected, or there are none available in the current space. Could not create job.";
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));

				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			}

			if (benchMethod.equals("runAllBenchInHierarchy")) {
				if (traversal.equals("depth")) {
					log.debug("User selected depth-first traversal");
					JobManager.addBenchmarksFromHierarchy(j, Integer.parseInt(request.getParameter(spaceId)), SessionUtil.getUserId(request), configIds, cpuLimit, runLimit,memoryLimit, SP);
				} else {
					log.debug("User selected round-robin traversal");
					List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(space, userId));
					spaces.add(0,Spaces.get(space));
					HashMap<Space, BSC> SpaceToBSC = new HashMap<Space, BSC>();
					int max = 0;
					List<Solver> solvers = Solvers.getWithConfig(configIds);
					HashMap<Solver, List<Configuration>> SC = new HashMap<Solver, List<Configuration>>();

					for (Space s: spaces) {
						int space_id = s.getId();
						List<Benchmark> benchmarks = Benchmarks.getBySpace(space_id);
						int temp = benchmarks.size();
						if (temp>max) {
							max = temp;
						}
						SpaceToBSC.put(s, new BSC(benchmarks, solvers, SC));
					}
					log.debug("Max size is: " + max);
					
					for (int i=0; i < max; i++) {
						for (Space s : spaces) {
							BSC bsc = SpaceToBSC.get(s);
								if (bsc.b.size() > i) {
									log.debug("Calling addJobPairsRobinSelected function: i = " + i);
									JobManager.addJobPairsRobinSelected(j, userId, cpuLimit, runLimit,memoryLimit, s.getId(), bsc.b.get(i), solvers, SP);
								}
						}
					}
					
					
				}
				// We chose to run the hierarchy, so add subspace benchmark IDs to the list.
				if (j.getJobPairs().size() == 0) {
					// No pairs in the job means no benchmarks in the hierarchy; error out
					String message="Either no benchmarks were selected, or there are none available in the current space/hierarchy. Could not create job.";
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));

					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
					return;
				}
			} else {
				List<Integer> benchmarkIds = Util.toIntegerList(request.getParameterValues(benchmarks));
				if (benchmarkIds.size() == 0) {
					String message="Either no benchmarks were selected, or there are none available in the current space/hierarchy. Could not create job.";
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));

					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
					return;
				}

				JobManager.buildJob(j, userId, cpuLimit, runLimit,memoryLimit, benchmarkIds, configIds, space, SP);
			}
		}
		
		if (err != null) {
		    response.sendError(HttpServletResponse.SC_BAD_REQUEST, err);
		    return;
		}

		if (j.getJobPairs().size() == 0) {
			String message="Error: no job pairs created for the job. Could not proceed with job submission.";
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));

			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			// No pairs in the job means something went wrong; error out
			return;
		}
		
		//decoupling adding job to db and script creation/submission
		//boolean submitSuccess = JobManager.submitJob(j, space);
		boolean submitSuccess = Jobs.add(j, space);
		String start_paused = request.getParameter(pause);

		//if the user chose to immediately pause the job
		if (start_paused.equals("yes")) {
			Jobs.pause(j.getId());
		}
		
		if(submitSuccess) {
		    // If the submission was successful, send back to space explorer

			response.addCookie(new Cookie("New_ID", String.valueOf(j.getId())));
		    response.sendRedirect(Util.docRoot("secure/explore/spaces.jsp"));
		}else  {
		    // Or else send an error
		    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				       "Your job failed to submit for an unknown reason. Please try again.");
		}
	}

	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters
	 * and content length requirements to ensure it is not malicious.
	 * @param request The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValid(HttpServletRequest request) {
		try {
			// Make sure the parent space id is a int
			if(!Validator.isValidInteger(request.getParameter(spaceId))) {
				return new ValidatorStatusCode(false, "The given space ID needs to be a valid integer");
			}
			// Make sure timeout an int
			if(!Validator.isValidInteger(request.getParameter(clockTimeout))) {
				return new ValidatorStatusCode(false, "The given wallclock timeout needs to be a valid integer");
			}

			if(!Validator.isValidInteger(request.getParameter(cpuTimeout))) {
				return new ValidatorStatusCode(false, "The given cpu timeout needs to be a valid integer");
			}
			
			if(!Validator.isValidDouble(request.getParameter(maxMemory))) {
				return new ValidatorStatusCode(false, "The given maximum memory needs to be a valid double");
			}

			// If processors are specified, make sure they're valid ints
			if(Util.paramExists(postProcessor, request)) {
				if(!Validator.isValidInteger(request.getParameter(postProcessor))) {
					return new ValidatorStatusCode(false, "The given post processor ID needs to be a valid integer");
				}
			}

			if(Util.paramExists(preProcessor, request)) {
				if(!Validator.isValidInteger(request.getParameter(preProcessor))) {
					return new ValidatorStatusCode(false, "The given pre processor ID needs to be a valid integer");
				}
			}

			// Make sure the queue is a valid integer
			if(!Validator.isValidInteger(request.getParameter(workerQueue))) {
				return new ValidatorStatusCode(false, "The given queue ID needs to be a valid integer");
			}
			
			// Make sure the queue is a valid selection and user has access to it
			Integer queueId = Integer.parseInt(request.getParameter(workerQueue));
			int userId = SessionUtil.getUserId(request);
			List<Queue> userQueues = Queues.getUserQueues(userId); 
			Boolean queueFound=false;
			for (Queue queue:userQueues){
				if (queue.getId() == queueId){
					queueFound=true;
					break;
				}
			}
			
			if (!queueFound){
				return new ValidatorStatusCode(false, "The given queue does not exist or you do not have access to it");
			}
			
			//make sure both timeouts are <= the queue settings
			int cpuLimit = Integer.parseInt(request.getParameter(cpuTimeout));
			int runLimit = Integer.parseInt(request.getParameter(clockTimeout));
			
			Queue q=Queues.get(queueId);
			if (runLimit > q.getWallTimeout()) {
				return new ValidatorStatusCode(false, "The given wallclock timeout exceeds the maximum allowed for this queue, which is "+q.getWallTimeout());
			}
			
			if (cpuLimit>q.getCpuTimeout()) {
				return new ValidatorStatusCode(false, "The given cpu timeout exceeds the maximum allowed for this queue, which is "+q.getCpuTimeout());
			}
			
			
			// Ensure the job description is valid
			if(!Validator.isValidPrimDescription((String)request.getParameter(description))) {
				return new ValidatorStatusCode(false, "The given description is invalid, please see the help files to see the valid format");
			}		

			int sid = Integer.parseInt(request.getParameter(spaceId));
			Permission perm = SessionUtil.getPermission(request, sid);
			log.debug("this is the perm");
			log.debug(perm);
			// Make sure the user has access to the space
			if(perm == null || !perm.canAddJob()) {
				return new ValidatorStatusCode(false, "You do not have permission to add jobs in this space");
			}

			// Only need these checks if we're choosing which solvers and benchmarks to run.
			// In any other case, we automatically get them so we don't have to pass them
			// as part of the request.
			log.debug("selection = " + request.getParameter(run));
			log.debug("benchChoice = " + request.getParameter(benchChoice));
			if (request.getParameter(run).equals("choose")) {

				// Check to see if we have a valid list of benchmark ids
				if (!request.getParameter(benchChoice).equals("runAllBenchInHierarchy")){
					if (!Validator.isValidIntegerList(request.getParameterValues(benchmarks))) {
						return new ValidatorStatusCode(false, "All selected benchmark IDs need to be valid integers");
					}
				}
				// Make sure the user is using benchmarks they can see
				if(!Permissions.canUserSeeBenchs(Util.toIntegerList(request.getParameterValues(benchmarks)), userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use one or more of the benchmarks you have selected");
				}

				// Check to see if we have a valid list of configuration ids
				if(!Validator.isValidIntegerList(request.getParameterValues(configs))) {
					return new ValidatorStatusCode(false, "All selected configuration IDs need to be valid integers");
				}
				Set<Integer> solverIds=new HashSet<Integer>();
				for (Integer cid : Util.toIntegerList(request.getParameterValues(configs))) {
					solverIds.add(Solvers.getSolverByConfig(cid, false).getId());
				}
				// Make sure the user is using solvers they can see
				if(!Permissions.canUserSeeSolvers(solverIds, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use all of the selected solvers");
				}
			}
			// Passed all checks, return true
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error creating job");
	}
}
