package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.BenchTypes;
import org.starexec.data.to.BenchmarkType;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles incoming requests to add and update benchmark types
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class BenchTypeManager extends HttpServlet {		
	private static final Logger log = Logger.getLogger(BenchTypeManager.class);

	// The unique date stamped file name format (for saving processor files)
	private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
	
	// Request attributes
	private static final String TYPE_NAME = "typeName";
	private static final String TYPE_DESC = "typeDesc";
	private static final String PROCESSOR_FILE = "typeFile";	
	private static final String OWNING_COMMUNITY = "com";
	private static final String TYPE_ID = "typeId";
	private static final String ACTION = "action";
	private static final String ADD_ACTION = "add";
	private static final String UPDATE_ACTION = "update";
		
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}	
		
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			String action = request.getParameter(ACTION);
			
			// Make sure we have an action parameter
			if(action != null) {
				// Delegate the request based on the action
				if(action.equals(ADD_ACTION)) {
					this.handleAddRequest(request, response);
				} else if(action.equals(UPDATE_ACTION)) {
					this.handleUpdateRequest(request, response);
				} else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid form action");	
				}
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Form action required");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}	
	
	/**
	 * Handles requests to update a benchmark type
	 */
	private void handleUpdateRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			// If we're dealing with an update request...
			if(ServletFileUpload.isMultipartContent(request)) {								
				HashMap<String, Object> form = Util.parseMultipartRequest(request);
				
				// Make sure the request is valid
				if(!isValidUpdateRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The benchmark type request was malformed");
					return;
				}
				
				// Make sure this user has the ability to update types for this community
				long community = Long.parseLong((String)form.get(OWNING_COMMUNITY));
				if(!SessionUtil.getPermission(request, community).isLeader()) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can edit types for their communities");
					return;
				}
				
				// Update the benchmark type
				BenchmarkType result = this.updateBenchType(form);
				
				// And redirect based on the results of the update
				if(result != null) {
					response.sendRedirect("/starexec/secure/edit/community.jsp?cid=" + result.getCommunityId());	
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update the benchmark type. A partial update may have been applied");	
				}												
			} else {
				// Got a non-multipart request, invalid
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.error(e.getMessage(), e);
		}	
	}
	
	/**
	 * Handles requests to add a benchmark type
	 */
	private void handleAddRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			// If we're dealing with an upload request...
			if(ServletFileUpload.isMultipartContent(request)) { 		
				HashMap<String, Object> form = Util.parseMultipartRequest(request);
				
				// Make sure the request is valid
				if(!isValidCreateRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The benchmark type request was malformed");
					return;
				}
				
				// Make sure this user has the ability to add type to the space
				long community = Long.parseLong((String)form.get(OWNING_COMMUNITY));
				if(!SessionUtil.getPermission(request, community).isLeader()) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can add types to their communities");
					return;
				}
				
				// Add the benchmark type to the database/filesystem
				BenchmarkType result = this.addNewBenchType(form);
				
				// Redirect based on the results of the addition
				if(result != null) {
					response.sendRedirect("/starexec/secure/edit/community.jsp?cid=" + result.getCommunityId());	
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add new benchmark type.");	
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
	 * Parses through form items and builds a new BenchmarkType from it. Then it is
	 * added to the database. Also writes the processor file to disk included in the request.
	 * @param form The form fields for the request
	 * @return The BenchmarkType that was added to the database if it was successful
	 */
	private BenchmarkType addNewBenchType(HashMap<String, Object> form) {		
		try {						
			BenchmarkType newType = new BenchmarkType();
			newType.setName((String)form.get(TYPE_NAME));
			newType.setDescription((String)form.get(TYPE_DESC));					
			newType.setCommunityId(Long.parseLong((String)form.get(OWNING_COMMUNITY)));

			// Save the uploaded file to disk
			FileItem processorFile = (FileItem)form.get(PROCESSOR_FILE);
			File newFile = this.getProcessorFilePath(newType.getCommunityId(), processorFile.getName());
			processorFile.write(newFile);
			newType.setProcessorPath(newFile.getAbsolutePath());					
			log.info(String.format("Wrote new benchmark type processor to %s for community %d", newFile.getAbsolutePath(), newType.getCommunityId()));					
			
			if(BenchTypes.add(newType)) {
				return newType;					
			}						
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}				
		
		return null;
	}
	
	/**
	 * Parses through form items and updates the benchmark type based on the form's contents.
	 * Also writes a new processor file and deletes the old one if the user specifies a new file
	 * @param form The request's form items
	 * @return The BenchmarkType that was updated in the database if it was successful
	 */
	private BenchmarkType updateBenchType(HashMap<String, Object> form) {		
		try {						
			BenchmarkType updatedType = new BenchmarkType();						
			updatedType.setName((String)form.get(TYPE_NAME));
			updatedType.setDescription((String)form.get(TYPE_DESC));					
			updatedType.setCommunityId(Long.parseLong((String)form.get(OWNING_COMMUNITY)));					
			updatedType.setId(Long.parseLong((String)form.get(TYPE_ID)));
			
			FileItem processorFile = (FileItem)form.get(PROCESSOR_FILE);
			if(!Util.isNullOrEmpty(processorFile.getName())) {
				// If the file is being updated, save it to disk...
				File newFile = this.getProcessorFilePath(updatedType.getCommunityId(), processorFile.getName());
				processorFile.write(newFile);
				updatedType.setProcessorPath(newFile.getAbsolutePath());					
				log.info(String.format("Wrote new benchmark type processor to %s for community %d", newFile.getAbsolutePath(), updatedType.getCommunityId()));	
			}
			
			// Get the original type information from the database
			BenchmarkType originalType = BenchTypes.get(updatedType.getId());
			
			// If there's a difference between names, update it
			if(!originalType.getName().equals(updatedType.getName())) {
				BenchTypes.updateName(originalType.getId(), updatedType.getName());
			}

			// If there's a difference between descriptions, update it
			if(!originalType.getDescription().equals(updatedType.getDescription())) {
				BenchTypes.updateDescription(originalType.getId(), updatedType.getDescription());
			}
			
			// If the user specified a new processor script, update it
			if(!org.starexec.util.Util.isNullOrEmpty(updatedType.getProcessorName())) {
				// Update the new path
				BenchTypes.updatePath(originalType.getId(), updatedType.getProcessorPath());
				
				// And remove the old processor script from disk
				File f = new File(originalType.getProcessorPath());
				File parentDir = new File(f.getParent());
				f.delete();				
				parentDir.delete();
				
				log.info(String.format("Removed files [%s] and directory [%s] due to an updated benchmark type processor script", f.getAbsolutePath(), parentDir.getAbsolutePath()));
			}
			
			return updatedType;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}				
		
		return null;
	}

	/**
	 * Creates a unique file path for the given file to write in the benchmark type directory
	 * @param communityId The id of the community (used in the path)
	 * @param fileName The name of the file to create in the unique directory
	 * @return The file object associated with the new file path (all necessary directories are created as needed)
	 */
	private File getProcessorFilePath(long communityId, String fileName) {
		// Get the base benchmark type directory and add community ID
		File saveDir = new File(R.BENCH_TYPE_DIR, "" + communityId);			
		
		// Then add the unique datetime to the path to ensure it's unique
		saveDir = new File(saveDir, shortDate.format(new Date()));
		
		// Create the dirs
		saveDir.mkdirs();
		
		// Finally tack on the file name
		saveDir = new File(saveDir, fileName);
		
		return saveDir;
	}
	
	/**
	 * Uses the Validate util to ensure the incoming type upload request is valid. This checks for illegal characters
	 * and content length requirements.
	 * @param form The form to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private boolean isValidCreateRequest(HashMap<String, Object> form) {
		try {			
			if(!form.containsKey(TYPE_NAME) ||
			   !form.containsKey(TYPE_DESC) ||
			   !form.containsKey(OWNING_COMMUNITY) ||
			   !form.containsKey(PROCESSOR_FILE)) {
				return false;
			}
										
			if(!Validator.isValidPrimName((String)form.get(TYPE_NAME))) {
				return false;
			}
																	
			if(!Validator.isValidPrimDescription((String)form.get(TYPE_DESC))) {
				return false;
			}
						
			Long.parseLong((String)form.get(OWNING_COMMUNITY));			
			
			// Passed all checks, return true
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		// Return false control flow is broken and ends up here
		return false;
	}
	
	/**
	 * Uses the Validate util to ensure the incoming type upload  request is valid. This checks for illegal characters
	 * and content length requirements to ensure it is not malicious.
	 * @param form The form to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private boolean isValidUpdateRequest(HashMap<String, Object> form) {
		try {	
			// Make sure we have a type ID 
			if(!form.containsKey(TYPE_ID)) {
				return false;
			} else {
				// Try to parse, we should have a valid number
				Long.parseLong((String)form.get(TYPE_ID));
			}
			
			// Now run through the create request validator, they share the same fields			
			return this.isValidCreateRequest(form);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}
