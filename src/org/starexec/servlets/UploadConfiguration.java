package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Solvers;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.security.SolverSecurity;
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
    private static final String CONFIG_DESC = "uploadConfigDesc";
    private static final String SOLVER_ID = "solverId";
    private static final String UPLOAD_FILE = "file";
    private static final String CONFIG_NAME = "uploadConfigName";    		
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
    }
    
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
				
				ValidatorStatusCode status= SolverSecurity.canUserAddConfiguration(Integer.parseInt((String)configAttrMap.get(SOLVER_ID)), userId);
				if (!status.isSuccess()) {
					//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, status.getMessage());
					return;
				}

				
				// Process the configuration file and write it to the parent solver's /bin directory, then update the solver's disk_size attribute
				int result = handleConfiguration(configAttrMap);
				
				// Redirect user based on how the configuration handling went
				if(result == -1) {

					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to upload new configuration.");	
				} else {

					//result should be the new ID of the configuration
					response.addCookie(new Cookie("New_ID", String.valueOf(result)));
				    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + Integer.parseInt((String)configAttrMap.get(SOLVER_ID))));	
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
			
			// Build a path to the appropriate solver bin directory
			File newConfigFile = new File(Util.getSolverConfigPath(solver.getPath(), newConfig.getName()));
			
			// If a configuration file exists on disk with the same name, append an integer to the file to make it unique
			//TODO: if the given name is already the max length, this is going to fail
			if(newConfigFile.exists()){
				boolean fileAlreadyExists = true;
				int intSuffix = 0;
				while(fileAlreadyExists){
					File temp = new File(newConfigFile.getAbsolutePath() + (++intSuffix));
					if(temp.exists() == false){
						newConfigFile = temp;
						newConfig.setName((String)configAttrMap.get(CONFIG_NAME) + intSuffix);
						fileAlreadyExists = false;
					}
				}
			}

			// Write the new configuration file to disk 
			uploadedFile.write(newConfigFile);
			
			// Make sure the configuration has the right line endings
			Util.normalizeFile(newConfigFile);
			
			//Makes executable
			newConfigFile.setExecutable(true);
			
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
					!configAttrMap.containsKey(CONFIG_NAME)) {
				return false;
			}
			
			// Ensure the parent solver id is valid
			Integer.parseInt((String)configAttrMap.get(SOLVER_ID));
			
			
			if (configAttrMap.containsKey(CONFIG_DESC)) {
				if (!Validator.isValidPrimDescription((String)configAttrMap.get(CONFIG_DESC))) {

					return false;
				}
			}
			// Ensure the configuration's name and description are valid
			if(!Validator.isValidPrimName((String)configAttrMap.get(CONFIG_NAME))) {

				return false;
			}
			
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}