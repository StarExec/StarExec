package org.starexec.servlets;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Spaces;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Servlet which handles generation of a chart of the execution result of a space.
 * @author Ruoyu Zhang
 *
 */
@SuppressWarnings("serial")
public class GenerateResultChart extends HttpServlet{
	private static final Logger log = Logger.getLogger(GenerateResultChart.class);	 
	private static final String SPACE_ID = "sid";
	
	/**
	 * GenerateResultChart doesn't handle doPost request.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
    }
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		try {	
			if (request == null){
				log.info("Request is null = " + (request == null));
			}
			else{
				log.info((String)request.getParameter(SPACE_ID));
			}
			int space_id = Integer.parseInt((String)request.getParameter(SPACE_ID));
			
			// If the request is valid
			if(!this.isRequestValid(request)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload picture request was malformed.");
			} 
			
			Spaces.generateResultChart(space_id);
			response.sendRedirect("/starexec/secure/explore/spaces.jsp");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters
	 * and content length requirements to ensure it is not malicious.
	 * @param request The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private boolean isRequestValid(HttpServletRequest request) {
		if(!Validator.isValidInteger((String)request.getParameter(SPACE_ID))) {
			return false;
		}
		
		// Return true if no problem
		return true;	
	}
}
