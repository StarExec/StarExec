package org.starexec.servlets;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.jobs.JobManager;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


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
	private static final String benchmarks = "bench";
	private static final String cpuTimeout = "cpuTimeout";
	private static final String clockTimeout = "wallclockTimeout";
	private static final String spaceId = "sid";
	
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
		cpuLimit = (cpuLimit <= 0) ? R.MAX_PAIR_CPUTIME : cpuLimit;
		runLimit = (runLimit <= 0) ? R.MAX_PAIR_RUNTIME : runLimit;
		
		int space = Integer.parseInt((String)request.getParameter(spaceId));
		int userId = SessionUtil.getUserId(request);
		
		// Make sure that the job has a unique name in the space.
		if(Spaces.notUniquePrimitiveName((String)request.getParameter(name), space, 3)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The job should have a unique name in the space.");
			return;
		}
		
		//Setup the job's attributes
		Job j = JobManager.setupJob(
				userId,
				(String)request.getParameter(name), 
				(String)request.getParameter(description),
				-1, //change to preprocessor ID when implemented
				Integer.parseInt((String)request.getParameter(postProcessor)), 
				Integer.parseInt((String)request.getParameter(workerQueue)));
		
		
		String selection = request.getParameter(run);
		//Depending on our run selection, handle each case differently
		if (selection.equals("runAllBenchInSpace")) {
			JobManager.addJobPairsFromSpace(j, userId, cpuLimit, runLimit, space);
		} else if (selection.equals("keepHierarchy")) {
			List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaces(space, userId, true));
			spaces.add(0, Spaces.get(space));
			for (Space s : spaces) {
				JobManager.addJobPairsFromSpace(j, userId, cpuLimit, runLimit, s.getId());
			}
		} else { //hierarchy OR choice
			List<Integer> solverIds = Util.toIntegerList(request.getParameterValues(solvers));
			List<Integer> configIds = Util.toIntegerList(request.getParameterValues(configs));
			
			if (solverIds.size() == 0 || configIds.size() == 0) {
				// Either no solvers or no configurations; error out
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either no solvers/configurations were selected, or there are none available in the current space. Could not create job.");
				return;
			}
			
			if (selection.equals("runAllBenchInHierarchy")) {
				// We chose to run the hierarchy, so add subspace benchmark IDs to the list.
				JobManager.addBenchmarksFromHierarchy(j, Integer.parseInt(request.getParameter(spaceId)), SessionUtil.getUserId(request), solverIds, configIds, cpuLimit, runLimit);
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
				
				JobManager.buildJob(j, userId, cpuLimit, runLimit, benchmarkIds, solverIds, configIds, space);
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
		if (submitSuccess){
			JobManager.submitJob(j);
		}
		if(true == submitSuccess) {
			// If the submission was successful, send back to space explorer
			response.sendRedirect("/starexec/secure/explore/spaces.jsp");
		} else {
			// Or else send an error
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Your job failed to submit for an unknown reason. Please try again.");
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
		
			// Ensure the job name is valid (alphanumeric < 32 chars)
			if(!Validator.isValidPrimName((String)request.getParameter(name))) {
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
			if (request.getParameter(run).equals("choose")) {
				int userId = SessionUtil.getUserId(request);
				
				// Check to see if we have a valid list of benchmark ids
				if (!Validator.isValidIntegerList(request.getParameterValues(benchmarks))) {
					return false;
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
