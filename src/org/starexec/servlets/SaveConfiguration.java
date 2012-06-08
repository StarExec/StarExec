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
 * Supports the saving of new configuration files to the Starexec file system
 * (i.e. writing a configuration file instead of uploading a configuration file)
 * 
 * @author Todd Elvers
 */
@SuppressWarnings("serial")
public class SaveConfiguration extends HttpServlet {
	private static final Logger log = Logger.getLogger(SaveConfiguration.class);	
    
    // Param constants to use to process the form
    private static final String CONFIG_DESC = "saveConfigDesc";
    private static final String SOLVER_ID = "solverId";
    private static final String CONFIG_CONTENTS = "saveConfigContents";
    private static final String CONFIG_NAME = "saveConfigName";    		
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
			// Parameter validation
			if(!this.isValidRequest(request)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The save configuration request was malformed.");
				return;
			} 
			
			// Permissions check; ensure the user owns the solver to which they are saving
			if(Solvers.get(Integer.parseInt(request.getParameter(SOLVER_ID))).getUserId() != userId){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only owners of a solver may save configurations to it.");
				return;
			}
			
			// Process the configuration file and write it to the parent solver's /bin directory, then update the solver's disk_size attribute
			int result = handleConfiguration(request);
			
			// Redirect user based on how the configuration handling went
			if(result == -1) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save new configuration.");	
			} else {
				response.sendRedirect("/starexec/secure/details/solver.jsp?id=" + Integer.parseInt((String)request.getParameter(SOLVER_ID)));	
			}									
    	} catch (Exception e) {
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		log.error(e.getMessage(), e);
    	}	
	}
    
    
    /**
     * Handles writing a new configuration file to disk
     * 
     * @param request the object containing the new configuration's name, description and contents
     * @return a positive number representing the new configuration's id, or a negative number if an error occurred
     * @author Todd Elvers
     */
	public int handleConfiguration(HttpServletRequest request) {
		try {
			
			// Set up a new configuration object with the submitted information
			Solver solver = Solvers.get(Integer.parseInt(request.getParameter(SOLVER_ID)));
			Configuration newConfig = new Configuration();
			newConfig.setName(request.getParameter(CONFIG_NAME));
			newConfig.setDescription(request.getParameter(CONFIG_DESC));
			newConfig.setSolverId(solver.getId());
			String configContents = request.getParameter(CONFIG_CONTENTS);
			
			// Build a path to the appropriate solver bin directory and ensure the file pointed to by newConfigFile doesn't already exist
			File newConfigFile = new File(Util.getSolverConfigPath(solver.getPath(), newConfig.getName()));
			// If a configuration file exists on disk with the same name, append an integer to the file to make it unique
			if(newConfigFile.exists()){
				boolean fileAlreadyExists = true;
				int intSuffix = 0;
				while(fileAlreadyExists){
					File temp = new File(newConfigFile.getAbsolutePath() + (++intSuffix));
					if(temp.exists() == false){
						newConfigFile = temp;
						newConfig.setName(request.getParameter(CONFIG_NAME) + intSuffix);
						fileAlreadyExists = false;
					}
				}
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
	
	
	/**
	 * Validates a 'save configuration' request by ensuring the necessary parts
	 * of the configuration file were provided by the client
	 * 
	 * @param request the object containing the new configuration file's name, description and contents
	 * @return true iff all necessary configuration file info was provided by the client and is valid
	 * @author Todd Elvers
	 */
	private boolean isValidRequest(HttpServletRequest request) {
		try {
			if(Util.isNullOrEmpty((String) request.getParameter(CONFIG_NAME))
					|| Util.isNullOrEmpty((String) request.getParameter(SOLVER_ID))
					|| Util.isNullOrEmpty((String) request.getParameter(CONFIG_CONTENTS))
					|| Util.isNullOrEmpty((String) request.getParameter(CONFIG_DESC))){
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