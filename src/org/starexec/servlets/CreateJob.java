package org.starexec.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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
	private static final String solvers = "solver";
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
		if(!isValid(request)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The create job request was malformed");
			return;
		}		

		int cpuLimit = Integer.parseInt((String)request.getParameter(cpuTimeout));
		int runLimit = Integer.parseInt((String)request.getParameter(clockTimeout));
		long memoryLimit=Util.gigabytesToBytes(Double.parseDouble(request.getParameter(maxMemory)));
		cpuLimit = (cpuLimit <= 0) ? R.MAX_PAIR_CPUTIME : cpuLimit;
		runLimit = (runLimit <= 0) ? R.MAX_PAIR_RUNTIME : runLimit;
		
		//memory is in units of bytes
		memoryLimit = (memoryLimit <=0) ? R.MAX_PAIR_VMEM : memoryLimit;
		int space = Integer.parseInt((String)request.getParameter(spaceId));
		int userId = SessionUtil.getUserId(request);

		// Make sure that the job has a unique name in the space.
		if(Spaces.notUniquePrimitiveName((String)request.getParameter(name), space, 3)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The job should have a unique name in the space.");
			return;
		}
		log.debug("confirmed the new job has a unique name");
		//Setup the job's attributes
		Job j = JobManager.setupJob(
				userId,
				(String)request.getParameter(name), 
				(String)request.getParameter(description),
				Integer.parseInt((String)request.getParameter(preProcessor)),
				Integer.parseInt((String)request.getParameter(postProcessor)), 
				Integer.parseInt((String)request.getParameter(workerQueue)));
		j.setPrimarySpace(space);
		//Create the HashMap to be used for creating job-pair path
		log.debug("started building the new job");
		HashMap<Integer, String> SP =  Spaces.spacePathCreate(userId, Spaces.getSubSpaceHierarchy(space, userId), space);
		log.debug("HASHMAP = " + SP);
		log.debug("got the space paths for the new job");
		String selection = request.getParameter(run);
		String benchMethod = request.getParameter(benchChoice);
		String traversal2 = request.getParameter(traversal);
		//Depending on our run selection, handle each case differently
		if (selection.equals("runAllBenchInSpace")) {
			JobManager.addJobPairsFromSpace(j, userId, cpuLimit, runLimit, memoryLimit, space, SP);
		} else if (selection.equals("keepHierarchy")) {
			log.debug("User selected keepHierarchy");

			List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(space, userId)); //Remove spaces the user is not a member of
			log.debug("got all the subspaces for the job");		
			spaces.add(0, Spaces.get(space));
			if (traversal2.equals("depth")) {
				for (Space s : spaces) {
					JobManager.addJobPairsFromSpace(j, userId, cpuLimit, runLimit, memoryLimit, s.getId(), SP);
				}
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
								JobManager.addJobPairsRobin(j, userId, cpuLimit, runLimit,memoryLimit, s.getId(), bsc.b.get(i), bsc.s, bsc.sc, SP);
							}
						}
					}
				}
		} else { //hierarchy OR choice
			List<Integer> solverIds = Util.toIntegerList(request.getParameterValues(solvers));
			List<Integer> configIds = Util.toIntegerList(request.getParameterValues(configs));

			if (solverIds.size() == 0 || configIds.size() == 0) {
				// Either no solvers or no configurations; error out
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either no solvers/configurations were selected, or there are none available in the current space. Could not create job.");
				return;
			}

			if (benchMethod.equals("runAllBenchInHierarchy")) {
				if (traversal.equals("depth")) {
					log.debug("User selected depth-first traversal");
					JobManager.addBenchmarksFromHierarchy(j, Integer.parseInt(request.getParameter(spaceId)), SessionUtil.getUserId(request), solverIds, configIds, cpuLimit, runLimit,memoryLimit, SP);
				} else {
					log.debug("User selected round-robin traversal");
					List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(space, userId));
					spaces.add(0,Spaces.get(space));
					HashMap<Space, BSC> SpaceToBSC = new HashMap<Space, BSC>();
					int max = 0;
					List<Solver> solvers = Solvers.getWithConfig(solverIds, configIds);
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
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either no benchmarks were selected, or there are none available in the current space/hierarchy. Could not create job.");
					return;
				}
			} else {
				List<Integer> benchmarkIds = Util.toIntegerList(request.getParameterValues(benchmarks));
				if (benchmarkIds.size() == 0) {
					// No benchmarks selected; error out
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either no benchmarks were selected, or there are none available in the current space/hierarchy. Could not create job.");
					return;
				}

				JobManager.buildJob(j, userId, cpuLimit, runLimit,memoryLimit, benchmarkIds, solverIds, configIds, space, SP);
			}
		}

		if (j.getJobPairs().size() == 0) {
			// No pairs in the job means something went wrong; error out
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error: no job pairs created for the job. Could not proceed with job submission.");
			return;
		}

		//decoupling adding job to db and script creation/submission
		//boolean submitSuccess = JobManager.submitJob(j, space);
		boolean submitSuccess = Jobs.add(j, space);
		String start_paused = request.getParameter(pause);

		//if the user chose to immediately pause the job
		if (start_paused.equals("yes")) {
			Jobs.pause(j.getId());
		} else {
			JobManager.checkPendingJobs(); // to start this job running if it is not	
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
	private boolean isValid(HttpServletRequest request) {
		try {
			// Make sure the parent space id is a int
			if(!Validator.isValidInteger((String)request.getParameter(spaceId))) {
				return false;
			}

			// Make sure timeout an int
			if(!Validator.isValidInteger((String)request.getParameter(clockTimeout))) {
				return false;
			}

			if(!Validator.isValidInteger((String)request.getParameter(cpuTimeout))) {
				return false;
			}
			
			if(!Validator.isValidDouble((String)request.getParameter(maxMemory))) {
				return false;
			}

			// If processors are specified, make sure they're valid ints
			if(Util.paramExists(postProcessor, request)) {
				if(!Validator.isValidInteger((String)request.getParameter(postProcessor))) {
					return false;
				}
			}

			if(Util.paramExists(preProcessor, request)) {
				if(!Validator.isValidInteger((String)request.getParameter(preProcessor))) {
					return false;
				}
			}

			// Make sure the queue is a valid integer
			if(!Validator.isValidInteger((String)request.getParameter(workerQueue))) {
				return false;
			}
			
			// Make sure the queue is a valid selection and user has access to it
			Integer queueId = Integer.parseInt((String)request.getParameter(workerQueue));
			int userId = SessionUtil.getUserId(request);
			List<Queue> userQueues = Queues.getUserQueues(userId); 
			Boolean queueFound=false;
			for (Queue queue:userQueues){
				if (queue.getId() == queueId){
					queueFound=true;
					break;
				}
			}
			if (queueFound==false){
				return false;
			}
			
			// Ensure the job description is valid
			if(!Validator.isValidPrimDescription((String)request.getParameter(description))) {
				return false;
			}		

			int sid = Integer.parseInt((String)request.getParameter(spaceId));
			Permission perm = SessionUtil.getPermission(request, sid);

			// Make sure the user has access to the space
			if(perm == null || !perm.canAddJob()) {
				return false;
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
						return false;
					}
				}
				// Make sure the user is using benchmarks they can see
				if(!Permissions.canUserSeeBenchs(Util.toIntegerList(request.getParameterValues(benchmarks)), userId)) {
					return false;
				}

				// Check to see if we have a valid list of solver ids
				if(!Validator.isValidIntegerList(request.getParameterValues(solvers))) {
					return false;
				}

				// Check to see if we have a valid list of configuration ids
				if(!Validator.isValidIntegerList(request.getParameterValues(configs))) {
					return false;
				}

				// Make sure the user is using solvers they can see
				if(!Permissions.canUserSeeSolvers(Util.toIntegerList(request.getParameterValues(solvers)), userId)) {
					return false;
				}
			}
			// Passed all checks, return true
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return false;
	}
}
