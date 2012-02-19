package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
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
	private static final String benchmarks = "bench";
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
		
		// Get pre and post processor IDs if they exist, or else set them to -1
		int preProcessorId = Util.paramExists(preProcessor, request) ? Integer.parseInt((String)request.getParameter(preProcessor)) : -1; 
		int postProcessorId = Util.paramExists(postProcessor, request) ? Integer.parseInt((String)request.getParameter(postProcessor)) : -1;
		
		Job j = JobManager.buildJob(
				SessionUtil.getUserId(request), 
				(String)request.getParameter(name), 
				(String)request.getParameter(description), 
				preProcessorId,
				postProcessorId, 
				Integer.parseInt((String)request.getParameter(workerQueue)), 
				Util.toIntegerList(request.getParameterValues(benchmarks)), 
				Util.toIntegerList(request.getParameterValues(solvers)), 
				Util.toIntegerList(request.getParameterValues(configs)));

		int space = Integer.parseInt((String)request.getParameter(spaceId));
		boolean submitSuccess = JobManager.submitJob(j, space);		
		
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
			
			// Check to see if we have a valid list of solver ids
			if(!Validator.isValidIntegerList(request.getParameterValues(solvers))) {
				return false;
			}
			
			// Check to see if we have a valid list of configuration ids
			if(!Validator.isValidIntegerList(request.getParameterValues(configs))) {
				return false;
			}

			// Check to see if we have a valid list of benchmark ids
			if(!Validator.isValidIntegerList(request.getParameterValues(benchmarks))) {
				return false;
			}
			
			int sid = Integer.parseInt((String)request.getParameter(spaceId));
			Permission perm = SessionUtil.getPermission(request, sid);
			
			// Make sure the user has access to the space
			if(perm == null || !perm.canAddJob()) {
				return false;
			}
			
			// Make sure the user is using benchmarks and solvers that they can see
			int userId = SessionUtil.getUserId(request);
			
			if(!Permissions.canUserSeeSolvers(Util.toIntegerList(request.getParameterValues(solvers)), userId)) {
				return false;
			}
			
			if(!Permissions.canUserSeeBenchs(Util.toIntegerList(request.getParameterValues(benchmarks)), userId)) {
				return false;
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
