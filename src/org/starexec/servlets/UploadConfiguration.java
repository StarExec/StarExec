package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.data.database.Solvers;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Supports the uploading of new configuration files to the Starexec file system
 * 
 * @author Todd Elvers
 */
@SuppressWarnings("serial")
public class UploadConfiguration extends HttpServlet {
	private static final Logger log = Logger.getLogger(UploadConfiguration.class);	
    
    // Param constants to use to process the form
    private static final String CONFIG_DESC = "description";
    private static final String SOLVER_ID = "solverId";
    private static final String UPLOAD_FILE = "file";
    private static final String CONFIG_NAME = "name";    		
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// Ensure request is a file upload request (i.e. a multipart request)
			if (ServletFileUpload.isMultipartContent(request)) {
				// Get the configuration form attributes from add/configuration.jsp
				HashMap<String, Object> configAttrMap = Util.parseMultipartRequest(request); 
				
				// Parameter validation
				if(!this.isValidRequest(configAttrMap)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload configuration request was malformed.");
					return;
				} 
				
				// Permissions check; ensure the uploading user owns the solver to which they are uploading
				if(Solvers.get(Integer.parseInt((String)configAttrMap.get(SOLVER_ID))).getUserId() != userId){
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only owners of a solver may upload configurations to it.");
					return;
				}
				
				// Process the configuration file and write it to the parent solver's /bin directory, then update the solver's disk_size attribute
				int result = handleConfiguration(configAttrMap);
				
				// Redirect user based on how the configuration handling went
				if(result == -1) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to upload new configuration.");	
				} else if (result == -2){
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A configuration already exists under that name.");
				} else {
					response.sendRedirect("/starexec/secure/details/solver.jsp?id=" + Integer.parseInt((String)configAttrMap.get(SOLVER_ID)));	
				}									
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
    	} catch (Exception e) {
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		log.error(e.getMessage(), e);
    	}	
	}
    
    
    /**
     * Writes a newly uploaded configuration file to disk in its parent solver's bin directory,
     * normalizes its file endings, then updates the parent solver's disk_size attribute to reflect 
     * the newly added configuration file
     *
     * @param configAttrMap the map of form fields -> form values from the add/configurations.jsp page 
     * @return the id of the newly created configuration file,<br>
     * 		-1 if a general error occurred while handling the new configuration,<br>
     * 		-2 if a configuration file already exists on disk with the same name
     * @author Todd Elvers
     */
	public int handleConfiguration(HashMap<String, Object> configAttrMap) {
		try {
			// Set up a new configuration object with the submitted information
			FileItem uploadedFile = (FileItem)configAttrMap.get(UPLOAD_FILE);
			Solver solver = Solvers.get(Integer.parseInt((String)configAttrMap.get(SOLVER_ID)));
			Configuration newConfig = new Configuration();
			newConfig.setName((String)configAttrMap.get(CONFIG_NAME));
			newConfig.setDescription((String)configAttrMap.get(CONFIG_DESC));
			newConfig.setSolverId(solver.getId());
			
			// Build a path to the appropriate solver bin directory and
			File newConfigFile = new File(Util.getSolverConfigPath(solver.getPath(), newConfig.getName()));
			
			// Ensure the file pointed to by newConfigFile doesn't already exist 
			if(newConfigFile.exists()){
				return -2;
			}
			
			// Write the new configuration file to disk 
			uploadedFile.write(newConfigFile);
			
			// Make sure the configuration has the right line endings
			Util.normalizeFile(newConfigFile);
			
			// Delete underlying storage for the file item now that it's on disk elsewhere
			uploadedFile.delete();
			
			// Pass new configuration, and the parent solver objects, to the database & return the result
			return Solvers.addConfiguration(solver, newConfig);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return -1;
	}	
	
	/**
	 * Validates the parameters of a configuration upload request (request parameters must
	 * already be in a HashMap)
	 *
	 * @param configAttrMap the map of form fields -> form values from the add/configurations.jsp page
	 * @return true iff the configuration upload request is valid, false otherwise
	 * @author Todd Elvers
	 */
	private boolean isValidRequest(HashMap<String, Object> configAttrMap) {
		try {
			// Ensure the map contains all relevant keys
			if (!configAttrMap.containsKey(UPLOAD_FILE) ||
					!configAttrMap.containsKey(SOLVER_ID) ||
					!configAttrMap.containsKey(CONFIG_NAME) || 
					!configAttrMap.containsKey(CONFIG_DESC)) {
				return false;
			}
			
			// Ensure the parent solver id is valid
			Integer.parseInt((String)configAttrMap.get(SOLVER_ID));
			
			// Ensure the configuration's name and description are valid
			if(!Validator.isValidPrimName((String)configAttrMap.get(CONFIG_NAME)) || 
					!Validator.isValidPrimDescription((String)configAttrMap.get(CONFIG_DESC))) {
				return false;
			}
			
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}