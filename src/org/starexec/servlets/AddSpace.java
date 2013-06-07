package org.starexec.servlets;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Servlet which handles incoming requests adding new spaces
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class AddSpace extends HttpServlet {		
	private static final Logger log = Logger.getLogger(AddSpace.class);	

	// Request attributes
	private static final String parentSpace = "parent";
	private static final String name = "name";
	private static final String description = "desc";
	private static final String locked = "locked";	
	private static final String addSolver = "addSolver";
	private static final String addBench = "addBench";
	private static final String addUser = "addUser";
	private static final String addSpace = "addSpace";
	private static final String addJob = "addJob";
	private static final String removeSolver = "removeSolver";
	private static final String removeBench = "removeBench";
	private static final String removeUser = "removeUser";
	private static final String removeSpace = "removeSpace";
	private static final String removeJob = "removeJob";
	
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
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The add space request was malformed");
			return;
		}
		
		// Make the space to be added and set it's basic information
		Space s = new Space();
		s.setName((String)request.getParameter(name));
		s.setDescription((String)request.getParameter(description));
		s.setLocked(Boolean.parseBoolean((String)request.getParameter(locked)));
		int spaceId1 = Integer.parseInt(request.getParameter(parentSpace));
		s.setParentSpace(spaceId1);
		
		// Make the default permissions for the space to be added
		Permission p = new Permission();
		p.setAddBenchmark(request.getParameter(addBench) != null);
		p.setAddSolver(request.getParameter(addSolver) != null);
		p.setAddSpace(request.getParameter(addSpace) != null);
		p.setAddUser(request.getParameter(addUser) != null);
		p.setAddJob(request.getParameter(addJob) != null);
		
		p.setRemoveBench(request.getParameter(removeBench) != null);
		p.setRemoveSolver(request.getParameter(removeSolver) != null);
		p.setRemoveSpace(request.getParameter(removeSpace) != null);
		p.setRemoveUser(request.getParameter(removeUser) != null);
		p.setRemoveJob(request.getParameter(removeJob) != null);
		
		// Set the default permission on the space
		s.setPermission(p);
		
		int spaceId = Integer.parseInt((String)request.getParameter(parentSpace));
		int userId = SessionUtil.getUserId(request);
		
		if (Spaces.getSubSpaceIDbyName(spaceId, s.getName()) != -1) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The subspace should have a unique name in the space. It is possible a private subspace you are not authorized to see has the same name.");
			return;
		}
				
		int newSpaceId = Spaces.add(s, spaceId, userId);
		
		if(newSpaceId <= 0) {			
			// If it failed, notify an error
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error adding the space to the starexec database");
		} else {
			// On success, redirect to the space explorer so they can see changes
		    response.sendRedirect(Util.docRoot("secure/explore/spaces.jsp"));	
		}		
	}

	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters
	 * and content length requirements to ensure it is not malicious.
	 * @param spaceRequest The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private boolean isValid(HttpServletRequest spaceRequest) {
		try {
			// Make sure the parent space id is a int
			int spaceId = Integer.parseInt((String)spaceRequest.getParameter(parentSpace));
			
			// Ensure the space name is valid (alphanumeric < SPACE_NAME_LEN chars)
			if(!Validator.isValidPrimName((String)spaceRequest.getParameter(name))) {
				return false;
			}
			
			// Ensure the description is < 1024 characters
			if(!Validator.isValidPrimDescription((String)spaceRequest.getParameter(description))) {
				return false;
			}
			
			// Ensure the isLocked value is a parseable boolean
			String lockVal = (String)spaceRequest.getParameter(locked);
			if(!lockVal.equals("true") && ! lockVal.equals("false")) {
				return false;
			}
			
			// Verify this user can add spaces to this space
			Permission p = SessionUtil.getPermission(spaceRequest, spaceId);
			if(!p.canAddSpace()) {
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
