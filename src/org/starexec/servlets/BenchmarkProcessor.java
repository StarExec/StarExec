package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.log4j.Logger;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Processor;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles incoming requests to add and update processors
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class BenchmarkProcessor extends HttpServlet {		
	private static final Logger log = Logger.getLogger(BenchmarkProcessor.class);
	
	// Request attributes
	private static final String PROCESSOR_ID = "pid";
	private static final String SPACE_ID = "sid";
	
	private static final String PROCESSOR_TYPE = "type";
	private static final String SPACE_HIERARCHY = "hier";
	private static final String CLEAR_OLD = "clear";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}	
	
	@GET
	@Path("/process/benchmarks")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			// If we're dealing with an upload request...
				
			// Make sure the request is valid
			if(!isValidProcessRequest(request)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The benchmark processing request was malformed");
				return;
			}
			
			int userId=SessionUtil.getUserId(request);
			int spaceId=Integer.parseInt((String)request.getParameter(SPACE_ID));
			boolean clearOld=Boolean.parseBoolean((String)request.getParameter(CLEAR_OLD));
			int pid=Integer.parseInt((String)request.getParameter(PROCESSOR_ID));
			boolean hier=Boolean.parseBoolean((String)request.getParameter(SPACE_HIERARCHY));
				
			Processor p=Processors.get(pid);
			if (!Users.isMemberOfCommunity(userId, p.getCommunityId())) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "You must be a member of the community that owns the processor");
			}
			if (!Users.isMemberOfSpace(userId,spaceId)) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "You must be a member of the space you are trying to process");
			}
			int commId=Spaces.GetCommunityOfSpace(spaceId);
			if (commId!=p.getCommunityId()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "You may only use processors that are a part of the current community");
			}
			Integer statusId=Benchmarks.process(spaceId, p, hier, userId, clearOld);
			if (statusId!=null) {
				response.sendRedirect(Util.docRoot("secure/details/uploadStatus.jsp?id=" + statusId));
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error processing the benchmarks");	
			}									
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error processing the benchmarks");
			log.error(e.getMessage(), e);
		}	
	}	
	
	
	
	
	
	
	
	
	/**
	 * Uses the Validate util to ensure the incoming type upload request is valid. This checks for illegal characters
	 * and content length requirements.
	 * @param form The form to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private boolean isValidProcessRequest(HttpServletRequest request) {
		try {			
			
			if(!Util.paramExists(PROCESSOR_ID,request) ||
			   !Util.paramExists(SPACE_ID,request) ||
			   !Util.paramExists(SPACE_HIERARCHY,request) ||
			   //!form.containsKey(PROCESSOR_TYPE) ||
			   !Util.paramExists(CLEAR_OLD,request)) {
				return false;
			}
										
									
			if(!Validator.isValidBool((String)request.getParameter(CLEAR_OLD)) || !Validator.isValidBool((String)request.getParameter(SPACE_HIERARCHY))) {
				return false;
			}
			
			if(!Validator.isValidInteger((String)request.getParameter(PROCESSOR_ID)) || !Validator.isValidInteger((String)request.getParameter(SPACE_ID))) {
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