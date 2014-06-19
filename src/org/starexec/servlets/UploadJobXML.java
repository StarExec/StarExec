package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.JobUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.apache.log4j.Logger;

/**
 * Allows for the creation of job pairs as represented in xml. Files can come in .zip,
 * .tar, or .tar.gz format.
 * 
 * @author Tim Smith
 */

public class UploadJobXML extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(UploadSpaceXML.class);
	private static final String UPLOAD_FILE = "f";
	private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
	private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
	private static final String SPACE_ID = "space";
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
				// Make sure the request is valid
				if(!this.isValidRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload job xml request was malformed");
					return;
				} 
				
				List<Integer> result = this.handleXMLFile(userId, form);
				
				// Redirect based on success/failure
				if(result!=null) {
					//send back new ids to the user
					for (Integer id: result) {
						if (id!=-1) {
							response.addCookie(new Cookie("New_ID", String.valueOf(id)));
						}
					}
				    response.sendRedirect(Util.docRoot("secure/explore/spaces.jsp"));	
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to upload Job XML - "); //+ result.getErrorMessage());	
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
	 * Gets the XML file from the form and tries to create a job from it
	 * @param userId the ID of the user making the request
	 * @param form a hashmap representation of the form on secure/add/batchJob.jsp
	 * @author Tim Smith
	 */
	private List<Integer> handleXMLFile(int userId, HashMap<String, Object> form) {
		try {
			log.debug("Handling Upload of XML File from User " + userId);
			FileItem item = (FileItem)form.get(UploadJobXML.UPLOAD_FILE);		
			// Don't need to keep file long - just using download directory
			
			// TODO Should we use the same directory as batchSpaces with a slightly different name for job xml uploads?
			File uniqueDir = new File(R.BATCH_SPACE_XML_DIR, "Job" + userId);
			uniqueDir = new File(uniqueDir, "TEMP_JOB_XML_FOLDER_");
			uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
			
			uniqueDir.mkdirs();
			
			//Process the archive file and extract
		
			File archiveFile = new File(uniqueDir,  item.getName());
			new File(archiveFile.getParent()).mkdir();
			item.write(archiveFile);
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			archiveFile.delete();
			
			JobUtil jobUtil = new JobUtil();
			//Typically there will just be 1 file, but might as well allow more
			
			Integer spaceId = Integer.parseInt((String)form.get(SPACE_ID));
			List<Integer> jobIds=new ArrayList<Integer>();
			List<Integer> current=new ArrayList<Integer>();

			for (File file:uniqueDir.listFiles())
			{
				current=jobUtil.createJobsFromFile(file, userId, spaceId);
				if (current!=null) {
					jobIds.addAll(current);		

				}
			}
			if (jobUtil.getJobCreationSuccess()) {
				return jobIds;

			} 
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return null;
	}

	private boolean isValidRequest(HashMap<String, Object> form) {
		try {
			if (!form.containsKey(UploadJobXML.UPLOAD_FILE) ||
					!form.containsKey(SPACE_ID) ){
				return false;
			}
			
			Integer.parseInt((String)form.get(SPACE_ID));
			
			String fileName = ((FileItem)form.get(UploadJobXML.UPLOAD_FILE)).getName();
			for(String ext : UploadJobXML.extensions) {
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
