package org.starexec.servlets;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.data.database.Solvers;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Supports the uploading of new configuration files to the Starexec file system
 * 
 * @deprecated not yet tested
 * @author Todd Elvers
 */
@SuppressWarnings("serial")
public class SaveConfiguration extends HttpServlet {
	private static final Logger log = Logger.getLogger(SaveConfiguration.class);	
    
    // Param constants to use to process the form
    private static final String CONFIG_DESC = "description";
    private static final String SOLVER_ID = "solverId";
    private static final String CONFIG_CONTENTS = "contents";
    private static final String CONFIG_NAME = "name";    		
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
			// Parameter validation
			if(!this.isValidRequest(request)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The save configuration request was malformed.");
				return;
			} 
			
			// Permissions check; ensure the user owns the solver to which they are saving
			if(Solvers.get(Integer.parseInt(request.getParameter("SOLVER_ID"))).getUserId() != userId){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only owners of a solver may save configurations to it.");
				return;
			}
			
			// Process the configuration file and write it to the parent solver's /bin directory, then update the solver's disk_size attribute
			int result = handleConfiguration(request);
			
			// Redirect user based on how the configuration handling went
			if(result == -1) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save new configuration.");	
			} else if (result == -2){
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A configuration already exists under that name.");
			} else {
				response.sendRedirect("/starexec/secure/details/solver.jsp?id=" + Integer.parseInt((String)request.getParameter(SOLVER_ID)));	
			}									
    	} catch (Exception e) {
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		log.error(e.getMessage(), e);
    	}	
	}
    
    
	public int handleConfiguration(HttpServletRequest request) {
		try {
			// Set up a new configuration object with the submitted information
			Solver solver = Solvers.get(Integer.parseInt((String)request.getParameter(SOLVER_ID)));
			Configuration newConfig = new Configuration();
			newConfig.setName((String)request.getParameter(CONFIG_NAME));
			newConfig.setDescription((String)request.getParameter(CONFIG_DESC));
			newConfig.setSolverId(solver.getId());
			String configContents = (String) request.getParameter(CONFIG_CONTENTS);
			
			// Build a path to the appropriate solver bin directory and
			File newConfigFile = new File(Util.getSolverConfigPath(solver.getPath(), newConfig.getName()));
			
			// Ensure the file pointed to by newConfigFile doesn't already exist 
			if(newConfigFile.exists()){
				return -2;
			}
			
			// Write the new configuration file to disk 
			FileUtils.writeStringToFile(newConfigFile, configContents);
			
			// Make sure the configuration has the right line endings
			Util.normalizeFile(newConfigFile);
			
			// Pass new configuration, and the parent solver objects, to the database & return the result
			return Solvers.addConfiguration(solver, newConfig);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return -1;
	}	
	
	private boolean isValidRequest(HttpServletRequest request) {
		try {
			if(Util.isNullOrEmpty(request.getParameter(CONFIG_NAME))
					|| Util.isNullOrEmpty(request.getParameter(SOLVER_ID))
					|| Util.isNullOrEmpty(request.getParameter(CONFIG_CONTENTS))
					|| Util.isNullOrEmpty(request.getParameter(CONFIG_DESC))){
				return false;
			}
			
			// Ensure the parent solver id is valid
			Integer.parseInt((String)request.getParameter(SOLVER_ID));
			
			// Ensure the configuration's name and description are valid
			if(!Validator.isValidPrimName(request.getParameter(CONFIG_NAME))
					|| !Validator.isValidPrimDescription(request.getParameter(CONFIG_DESC))) {
				return false;
			}
			
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}