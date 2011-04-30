package com.starexec.servlets;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.tomcat.util.http.fileupload.*;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.starexec.constants.*;
import com.starexec.data.*;
import com.starexec.data.to.*;
import com.starexec.util.*;


// TODO: Change from UploadBench to UploadSolver
public class UploadSolver extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The unique date stamped file name format    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	try {			
			FileItemFactory factory = new DiskFileItemFactory();				// Create the helper objects to assist in getting the file and saving it
			ServletFileUpload upload = new ServletFileUpload(factory);						
			List<FileItem> items = upload.parseRequest(request);				
			
			List<Integer> supportedDivs = new LinkedList<Integer>();			// Create a new list of integers that will store the benchmark's supported divisions
	    	for(String s : request.getParameter(P.SUPPORT_DIV).split(","))		// Get the level id's from the querystring, and split them. 
	    		supportedDivs.add(Integer.parseInt(s));							// And convert each one to an integer and add it to the list
			
	    	String solverName = request.getParameter(P.SOLVER_NAME);			// Get the solver's name from the querystring
	    	
			for(FileItem item : items) {										// For each file in the list			   
			   if (!item.isFormField()) {										// If it's not a field...
				   try {
					   handleSolver(item, response, supportedDivs, solverName);	// Process the uploaded solver file
				   } catch (Exception e) {
					   LogUtil.LogException(e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			LogUtil.LogException(e);											// Log any problems with the file upload process here
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
		File uniqueDir = new File(R.SOLVER_PATH, shortDate.format(new Date()));		// Create a unique path the zip file will be extracted to
		File zipFile = new File(uniqueDir,  item.getName());						// Create the zip file object-to-be		
		new File(zipFile.getParent()).mkdir();										// Create the directory the solver zip will be written to
						
		item.write(zipFile);														// Copy the zolver zip to the server from the client
		ZipUtil.extractZip(zipFile.getAbsolutePath());								// Extract the downloaded solver zip file
		zipFile.delete();															// Delete the archive, we don't need it anymore!

		Solver s = new Solver();													// Create a new solver
		s.setPath(uniqueDir.getAbsolutePath());										// Set its path to where we extracted the zip to
		s.setUserId(1);																// TODO use real username!
		s.setName(solverName);														// Set the solver's name
		for(Integer i : supportedDivs)												// For each of the supported divs the user selected
			s.addSupportedDiv(new Level(i));										// Add it to the solver
		for(Configuration c : findConfigs(uniqueDir.getAbsolutePath()))				// For each configuration that was found in the bin directory
			s.addConfig(c);
				
		boolean success = new Database().addSolver(s);								// Add the solver to the database
		response.sendRedirect("uploadsolver.jsp?s=" + success);
	}	
	
	private List<Configuration> findConfigs(String fromPath){		
		File binDir = new File(fromPath, R.BIN_DIR);								// Get the path to the bin directory
		
		if(!binDir.exists())														// If the bin directory doesn't exist
			return Collections.emptyList();											// Give back an empty list
		
		List<Configuration> returnList = new ArrayList<Configuration>();			// The list of configs we will give back
		
		for(File f : binDir.listFiles()){											// For each file in the bin directory... 
			f.setExecutable(true, false);											// Assume everything in bin is executable
			
			if(f.isFile() && f.getName().startsWith("run")){						// If the file is a file and it starts with run...
				Configuration c = new Configuration();								// Create a new configuration with that name of the file
				c.setName(f.getName());
				returnList.add(c);
			}				
		}
		
		return returnList;															// Give back the results!
	}
}
