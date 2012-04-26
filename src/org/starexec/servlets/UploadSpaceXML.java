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
import org.starexec.util.BatchUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Allows for the uploading of space hierarchies represented in xml. Files can come in .zip,
 * .tar, or .tar.gz format.
 * 
 * @author Benton McCune
 */
@SuppressWarnings("serial")
public class UploadSpaceXML extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(UploadSpaceXML.class);	
    private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);   
    private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
    private static final String SPACE_ID = "space";
    private static final String UPLOAD_FILE = "f";

    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
				// Make sure the request is valid
				if(!this.isValidRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload space xml request was malformed");
					return;
				} 
				
				BatchUtil result = this.handleXMLFile(userId, form);				
			
				// Redirect based on success/failure
				if(result.getSpaceCreationSuccess()) {
					response.sendRedirect("/starexec/secure/explore/spaces.jsp");	
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to upload Space XML - " + result.getErrorMessage());	
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
	 * This method is responsible for uploading a compressed folder with an xml representation
	 * @author Benton McCune
	 * @param userId the user ID of the user making the upload request
	 * @param form the HashMap representation of the upload request
	 * @throws Exception 
	 */
	public BatchUtil handleXMLFile(int userId, HashMap<String, Object> form) throws Exception {
		try {
			log.debug("Handling Upload of XML File from User " + userId);
			FileItem item = (FileItem)form.get(UploadSpaceXML.UPLOAD_FILE);		
			// Don't need to keep file long - just using download directory
			File uniqueDir = new File(R.BATCH_SPACE_XML_DIR, "" + userId);
			uniqueDir = new File(uniqueDir, "TEMP_XML_FOLDER_");
			uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
			
			uniqueDir.mkdirs();
			
			//Process the archive file and extract
		
			File archiveFile = new File(uniqueDir,  item.getName());
			new File(archiveFile.getParent()).mkdir();
			item.write(archiveFile);
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			archiveFile.delete();
			
			BatchUtil batchUtil = new BatchUtil();
			//Typically there will just be 1 file, but might as well allow more
			Boolean result = false;
			Integer spaceId = Integer.parseInt((String)form.get(SPACE_ID));
			for (File file:uniqueDir.listFiles())
			{
				result = batchUtil.createSpacesFromFile(file, userId, spaceId);		
			}

			return batchUtil;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return null;
	}	

	
	/**
	 * Sees if a given String -> Object HashMap is a valid Upload Space XML request.
	 * Checks to see if it contains all the information needed and if the information
	 * is in the right format.
	 * @author Benton McCune
	 * @param form the HashMap representing the upload request.
	 * @return true iff the request is valid
	 */
	private boolean isValidRequest(HashMap<String, Object> form) {
		try {
			if (!form.containsKey(UploadSpaceXML.UPLOAD_FILE) ||
					!form.containsKey(SPACE_ID) ){
				return false;
			}
			
			Integer.parseInt((String)form.get(SPACE_ID));
			
			String fileName = ((FileItem)form.get(UploadSpaceXML.UPLOAD_FILE)).getName();
			for(String ext : UploadSpaceXML.extensions) {
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