package org.starexec.servlets;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
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
import org.starexec.data.database.Spaces;
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
    private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
    
    // Some param constants to process the form
    private static final String SOLVER_DESC = "desc";
    private static final String SOLVER_DESC_FILE = "d";
    private static final String SOLVER_DOWNLOADABLE = "dlable";
    private static final String SPACE_ID = "space";
    private static final String UPLOAD_FILE = "f";
    private static final String SOLVER_NAME = "sn";    		
    private static final String CONFIG_PREFIX = R.CONFIGURATION_PREFIX;
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
				// Make sure the request is valid
				FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
				

				if (!Validator.isValidPrimDescription(item_desc.getString())) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The uploaded description file exceeds 1024 Characters");
					return;
				}
								
				if(!this.isValidRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload solver request was malformed");
					return;
				} 
				
				// Make sure that the solver has a unique name in the space.
				if(Spaces.notUniquePrimitiveName((String)form.get(UploadSolver.SOLVER_NAME), Integer.parseInt((String)form.get(SPACE_ID)), 1)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The solver should have a unique name in the space.");
					return;
				}
				
				// Parse the request as a solver
				int result = handleSolver(userId, form);				
			
				// Redirect based on success/failure
				if(result != -1) {
					response.sendRedirect("/starexec/secure/details/solver.jsp?id=" + result);	
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
	@SuppressWarnings("deprecation")
	public int handleSolver(int userId, HashMap<String, Object> form) throws Exception {
		try {
			FileItem item = (FileItem)form.get(UploadSolver.UPLOAD_FILE);
			FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
	

			//Set up a new solver with the submitted information
			Solver newSolver = new Solver();
			newSolver.setUserId(userId);
			newSolver.setName((String)form.get(UploadSolver.SOLVER_NAME));
			newSolver.setDescription((String)form.get(UploadSolver.SOLVER_DESC));
			newSolver.setFileDescription(item_desc.getString());
			newSolver.setDownloadable((Boolean.parseBoolean((String)form.get(SOLVER_DOWNLOADABLE))));
			
			//Set up the unique directory to store the solver
			//The directory is (base path)/user's ID/solver name/date/
			File uniqueDir = new File(R.SOLVER_PATH, "" + userId);
			uniqueDir = new File(uniqueDir, newSolver.getName());
			uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));

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
			
			
			//Search for Description within File, and extract its contents if it exists
			String FileName = item.getName().split("\\.")[0];
			String Destination = archiveFile.getParentFile().getCanonicalPath() + File.separator;
			String strUnzipped = "";		
		    try {
		        FileInputStream fis = new FileInputStream(Destination + FileName +"/" + R.SOLVER_DESC_PATH);
		        BufferedInputStream bis = new BufferedInputStream(fis);
		        DataInputStream dis = new DataInputStream(bis);
		        String text;
		        //dis.available() returns 0 if the file does not have more lines
		        while(dis.available() !=0) {
		            text=dis.readLine().toString();
		            strUnzipped = strUnzipped + text;
		        }
		        fis.close();
		        bis.close();
		        dis.close();
		    } catch (FileNotFoundException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		    //Temporary
		    if (strUnzipped.length() > 1024)
		    	strUnzipped = strUnzipped.substring(0,1024);
		    newSolver.setZipFileDescription(strUnzipped);
		    

	        
			
			//Try adding the solver to the database
			return Solvers.add(newSolver, Integer.parseInt((String)form.get(SPACE_ID)));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		
		
		return -1;
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
			if(f.isFile() && f.getName().startsWith(UploadSolver.CONFIG_PREFIX)){
				Configuration c = new Configuration();								
				c.setName(f.getName().substring(UploadSolver.CONFIG_PREFIX.length()));
				returnList.add(c);
				
				// Make sure the configuration has the right line endings
				Util.normalizeFile(f);
			}				
			//f.setExecutable(true, false);	//previous version only got top level		
		}		
		setHierarchyExecutable(binDir);//should make entire hierarchy executable
		return returnList;
	}
	/*
	 * Sets every file in a hierarchy to be executable
	 * @param rootDir the directory that we wish to have executable files in
	 * @return Boolean true if successful
	 */
	private Boolean setHierarchyExecutable(File rootDir){
		for (File f : rootDir.listFiles()){
			f.setExecutable(true,false);
			if (f.isDirectory()){
				setHierarchyExecutable(f);
			}
		}
		return true;
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
					!form.containsKey(SOLVER_DESC_FILE) ||
					!form.containsKey(SPACE_ID) ||
					!form.containsKey(UploadSolver.SOLVER_NAME) || 
					!form.containsKey(SOLVER_DESC) ||
					!form.containsKey(SOLVER_DOWNLOADABLE)) {
				return false;
			}
			
			Integer.parseInt((String)form.get(SPACE_ID));
			Boolean.parseBoolean((String)form.get(SOLVER_DOWNLOADABLE));
			//FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);

			//String item_desc_file = ArchiveUtil.extractArchiveDesc(SOLVER_NAME);
			
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