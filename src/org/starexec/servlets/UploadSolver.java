package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Solvers;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Allows for the uploading and handling of Solvers. Solvers can come in .zip,
 * .tar, or .tar.gz format, and configurations can be included in a top level
 * "bin" directory. Each Solver is saved in a unique directory on the filesystem.
 * 
 * @author Skylar Stark
 */
@SuppressWarnings("serial")
public class UploadSolver extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(UploadSolver.class);	
    private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);   
    private static final String[] extensions = {".tar", ".tar.gz", ".zip"};
    
    // Some param constants to process the form
    private static final String SOLVER_DESC = "desc";
    private static final String SOLVER_DOWNLOADABLE = "dlable";
    private static final String SPACE_ID = "space";
    private static final String UPLOAD_FILE = "f";
    private static final String SOLVER_NAME = "sn";    		
    private static final String SOLVER_RUN = "run";
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
				// Make sure the request is valid
				if(!this.isValidRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload solver request was malformed");
					return;
				} 
				
				// Parse the request as a solver
				Solver result = handleSolver(userId, form);				
			
				// Redirect based on success/failure
				if(result != null) {
					response.sendRedirect("/starexec/secure/explore/spaces.jsp");	
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to upload new solver.");	
				}									
			} else {
				// Got a non multi-part request, invalid
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
    	} catch (Exception e) {
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		log.error(e.getMessage(), e);
    	}	
	}
    
	/**
	 * This method is responsible for uploading a solver to
	 * the appropriate location and updating the database to reflect
	 * the solver's location.
	 * @param userId the user ID of the user making the upload request
	 * @param form the HashMap representation of the upload request
	 * @throws Exception 
	 */
	public Solver handleSolver(int userId, HashMap<String, Object> form) throws Exception {
		try {
			FileItem item = (FileItem)form.get(UploadSolver.UPLOAD_FILE);
			
			//Set up a new solver with the submitted information
			Solver newSolver = new Solver();
			newSolver.setUserId(userId);
			newSolver.setName((String)form.get(UploadSolver.SOLVER_NAME));
			newSolver.setDescription((String)form.get(SOLVER_DESC));
			newSolver.setDownloadable((Boolean.parseBoolean((String)form.get(SOLVER_DOWNLOADABLE))));
			
			//Set up the unique directory to store the solver
			//The directory is (base path)/user's ID/solver name/date/
			String directory = R.SOLVER_PATH + File.pathSeparator + userId + File.pathSeparator + newSolver.getName() + File.pathSeparator + shortDate.format(new Date());
			File uniqueDir = new File(directory);

			newSolver.setPath(uniqueDir.getAbsolutePath());

			uniqueDir.mkdirs();
			
			//Process the archive file and extract
			File archiveFile = new File(uniqueDir,  item.getName());
			new File(archiveFile.getParent()).mkdir();
			item.write(archiveFile);
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			archiveFile.delete();

			//Find configurations from the top-level "bin" directory
			for(Configuration c : findConfigs(uniqueDir.getAbsolutePath())) {
				newSolver.addConfiguration(c);
			}
			
			//Try adding the solver to the database
			if (Solvers.add(newSolver, Integer.parseInt((String)form.get(SPACE_ID)))) {
				return newSolver;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return null;
	}	
	
	/**
	 * Finds solver run configurations from a specified bin directory. Run configurations
	 * must start with a certain string specified in the list of constants. If no configurations
	 * are found, an empty list is returned.
	 * @param fromPath the base directory to find the bin directory in
	 * @return a list containing run configurations found in the bin directory
	 */
	private List<Configuration> findConfigs(String fromPath){		
		File binDir = new File(fromPath, R.SOLVER_BIN_DIR);
		if(!binDir.exists()) {
			return Collections.emptyList();
		}
		
		List<Configuration> returnList = new ArrayList<Configuration>();
		
		for(File f : binDir.listFiles()){
			f.setExecutable(true, false);
			if(f.isFile() && f.getName().startsWith(UploadSolver.SOLVER_RUN)){
				Configuration c = new Configuration();								
				c.setName(f.getName());
				returnList.add(c);
			}				
		}
		return returnList;
	}
	
	/**
	 * Sees if a given String -> Object HashMap is a valid Upload Solver request.
	 * Checks to see if it contains all the information needed and if the information
	 * is in the right format.
	 * @param form the HashMap representing the upload request.
	 * @return true iff the request is valid
	 */
	private boolean isValidRequest(HashMap<String, Object> form) {
		try {
			if (!form.containsKey(UploadSolver.UPLOAD_FILE) ||
					!form.containsKey(SPACE_ID) ||
					!form.containsKey(UploadSolver.SOLVER_NAME) || 
					!form.containsKey(SOLVER_DESC) ||
					!form.containsKey(SOLVER_DOWNLOADABLE)) {
				return false;
			}
			
			Integer.parseInt((String)form.get(SPACE_ID));
			Boolean.parseBoolean((String)form.get(SOLVER_DOWNLOADABLE));
			
			if(!Validator.isValidPrimName((String)form.get(UploadSolver.SOLVER_NAME)) || 
					!Validator.isValidPrimDescription((String)form.get(SOLVER_DESC))) {
				return false;
			}
			
			String fileName = ((FileItem)form.get(UploadSolver.UPLOAD_FILE)).getName();
			for(String ext : UploadSolver.extensions) {
				if(fileName.endsWith(ext)) {
					return true;
				}
			}
			
			
			return false;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}