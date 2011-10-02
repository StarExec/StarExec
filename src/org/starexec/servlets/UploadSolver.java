package org.starexec.servlets;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.*;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import org.starexec.constants.*;
import org.starexec.data.*;
import org.starexec.data.to.*;
import org.starexec.util.*;

/**
 * @deprecated This class is out of date and needs to be re-implemented
 * (Make sure to support .zip and .tar.gz!)
 */
public class UploadSolver extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(UploadSolver.class);	
	private static final long serialVersionUID = 1L;
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The unique date stamped file name format    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	try {			
    		// Create the helper objects to assist in getting the file and saving it
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);						
			List<FileItem> items = upload.parseRequest(request);				
			
			// Create a new list of integers that will store the benchmark's supported divisions
			List<Integer> supportedDivs = new LinkedList<Integer>();

			// Get the level id's from the querystring, and split them.
	    	for(String s : request.getParameter(P.SUPPORT_DIV).split(",")) {
	    		// And convert each one to an integer and add it to the list
	    		supportedDivs.add(Integer.parseInt(s));
	    	}
			
	    	// Get the solver's name from the querystring
	    	String solverName = request.getParameter(P.SOLVER_NAME);
	    	
	    	// For each file in the list
			for(FileItem item : items) {
				// If it's not a field...
			   if (!item.isFormField()) {
				   try {
					   // Process the uploaded solver file
					   handleSolver(item, response, supportedDivs, solverName);
				   } catch (Exception e) {
					   log.error(e.getMessage(), e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			// Log any problems with the file upload process here
			log.error(e.getMessage(), e);			
		}
	}
    
	/**
	 * This method is responsible for uploading a solver to
	 * the appropriate location and updating the database to reflect
	 * the solver's location.
	 * @param response The response which we can re-direct to based on a result
	 * @throws Exception 
	 */
	public void handleSolver(FileItem item, HttpServletResponse response, List<Integer> supportedDivs, String solverName) throws Exception {
		// Create a unique path the zip file will be extracted to
		File uniqueDir = new File(R.SOLVER_PATH, shortDate.format(new Date()));
		
		// Create the zip file object-to-be
		File zipFile = new File(uniqueDir,  item.getName());
		
		// Create the directory the solver zip will be written to
		new File(zipFile.getParent()).mkdir();
						
		// Copy the zolver zip to the server from the client
		item.write(zipFile);
		
		// Extract the downloaded solver zip file
		ZipUtil.extractZip(zipFile.getAbsolutePath());
		
		// Delete the archive, we don't need it anymore!
		zipFile.delete();

		// Create a new solver
		Solver s = new Solver();
		
		// Set its path to where we extracted the zip to
		s.setPath(uniqueDir.getAbsolutePath());
		
		// TODO use real username!
		s.setUserId(1);
		s.setName(solverName);
		
		// For each of the supported divs the user selected
		for(Integer i : supportedDivs) {
			// Add it to the solver
			//s.addSupportedDiv(new Level(i));
		}
		
		// For each configuration that was found in the bin directory
		for(Configuration c : findConfigs(uniqueDir.getAbsolutePath())) {
			s.addConfig(c);
		}
				
		// Add the solver to the database
		//boolean success = Database.addSolver(s);
		//response.sendRedirect("uploadsolver.jsp?s=" + success);
	}	
	
	private List<Configuration> findConfigs(String fromPath){		
		// Get the path to the bin directory
		File binDir = new File(fromPath, R.SOLVER_BIN_DIR);

		// If the bin directory doesn't exist
		if(!binDir.exists()) {
			// Give back an empty list
			return Collections.emptyList();
		}
		
		// The list of configs we will give back
		List<Configuration> returnList = new ArrayList<Configuration>();
		
		// For each file in the bin directory...
		for(File f : binDir.listFiles()){
			// Assume everything in bin is executable
			f.setExecutable(true, false);
		
			// If the file is a file and it starts with run...
			// TODO: Make this a configurable string
			if(f.isFile() && f.getName().startsWith("run")){
				
				// Create a new configuration with that name of the file
				Configuration c = new Configuration();								
				c.setName(f.getName());
				returnList.add(c);
			}				
		}
		
		// Give back the results!
		return returnList;
	}
}