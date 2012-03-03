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
import org.starexec.data.database.Processors;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles incoming requests to add and update processors
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class ProcessorManager extends HttpServlet {		
	private static final Logger log = Logger.getLogger(ProcessorManager.class);

	// The unique date stamped file name format (for saving processor files)
	private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
	
	// Request attributes
	private static final String PROCESSOR_NAME = "name";
	private static final String PROCESSOR_DESC = "desc";
	private static final String PROCESSOR_FILE = "file";	
	private static final String OWNING_COMMUNITY = "com";
	private static final String PROCESSOR_ID = "pid";
	
	private static final String ACTION = "action";
	private static final String ADD_ACTION = "add";
	private static final String UPDATE_ACTION = "update";
	
	private static final String PROCESSOR_TYPE = "type";
	private static final String BENCH_TYPE = "bench";
	private static final String PRE_PROCESS_TYPE = "pre";
	private static final String POST_PROCESS_TYPE = "post";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}	
		
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {			
			if(ServletFileUpload.isMultipartContent(request)) {								
				HashMap<String, Object> form = Util.parseMultipartRequest(request);
				String action = (String)form.get(ACTION);
				
				// Make sure we have an action parameter
				if(action != null) {
					// Delegate the request based on the action
					if(action.equals(ADD_ACTION)) {
						this.handleAddRequest(form, request, response);
					} else if(action.equals(UPDATE_ACTION)) {
						this.handleUpdateRequest(form, request, response);
					} else {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid form action");	
					}
				} else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Form action required");
				}
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Multipart request expected");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}	
	
	/**
	 * Handles requests to update a benchmark type
	 */
	private void handleUpdateRequest(HashMap<String, Object> form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			// If we're dealing with an update request...			
				
			// Make sure the request is valid
			if(!isValidUpdateRequest(form)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The processor request was malformed");
				return;
			}
			
			// Make sure this user has the ability to update processors for this community
			int community = Integer.parseInt((String)form.get(OWNING_COMMUNITY));
			if(!SessionUtil.getPermission(request, community).isLeader()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can edit processors for their community");
				return;
			}
			
			// Update the benchmark type
			Processor result = this.updateProcessor(form);
			
			// And redirect based on the results of the update
			if(result != null) {
				response.sendRedirect("/starexec/secure/edit/community.jsp?cid=" + result.getCommunityId());	
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update the processor. A partial update may have been applied");	
			}												
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.error(e.getMessage(), e);
		}	
	}
	
	/**
	 * Handles requests to add a processor
	 */
	private void handleAddRequest(HashMap<String, Object> form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			// If we're dealing with an upload request...
				
			// Make sure the request is valid
			if(!isValidCreateRequest(form)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The benchmark type request was malformed");
				return;
			}
			
			// Make sure this user has the ability to add type to the space
			int community = Integer.parseInt((String)form.get(OWNING_COMMUNITY));
			if(!SessionUtil.getPermission(request, community).isLeader()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can add types to their communities");
				return;
			}
			
			// Add the benchmark type to the database/filesystem
			Processor result = this.addNewProcessor(form);
			
			// Redirect based on the results of the addition
			if(result != null) {
				response.sendRedirect("/starexec/secure/edit/community.jsp?cid=" + result.getCommunityId());	
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add new benchmark type.");	
			}									
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.error(e.getMessage(), e);
		}	
	}
	
	/**
	 * Parses through form items and builds a new Processor object from it. Then it is
	 * added to the database. Also writes the processor file to disk included in the request.
	 * @param form The form fields for the request
	 * @return The Processor that was added to the database if it was successful
	 */
	private Processor addNewProcessor(HashMap<String, Object> form) {		
		try {						
			Processor newProc = new Processor();
			newProc.setName((String)form.get(PROCESSOR_NAME));
			newProc.setDescription((String)form.get(PROCESSOR_DESC));					
			newProc.setCommunityId(Integer.parseInt((String)form.get(OWNING_COMMUNITY)));
			
			String procType = (String)form.get(PROCESSOR_TYPE);
			newProc.setType(toProcessorEnum(procType));						
			
			// Save the uploaded file to disk
			FileItem processorFile = (FileItem)form.get(PROCESSOR_FILE);
			File newFile = this.getProcessorFilePath(newProc.getCommunityId(), processorFile.getName());
			processorFile.write(newFile);
			
			if (!newFile.setExecutable(true, false)) {			
				log.warn("Could not set processor as executable: " + newFile.getAbsolutePath());
			}
			
			newProc.setFilePath(newFile.getAbsolutePath());			
			log.info(String.format("Wrote new %s processor to %s for community %d", procType, newFile.getAbsolutePath(), newProc.getCommunityId()));					
			
			if(Processors.add(newProc)) {
				return newProc;					
			}						
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}				
		
		return null;
	}
	
	/**
	 * Parses through form items and updates the processor based on the form's contents.
	 * Also writes a new processor file and deletes the old one if the user specifies a new file
	 * @param form The request's form items
	 * @return The Processor that was updated in the database if it was successful
	 */
	private Processor updateProcessor(HashMap<String, Object> form) {		
		try {						
			Processor updatedProc = new Processor();						
			updatedProc.setName((String)form.get(PROCESSOR_NAME));
			updatedProc.setDescription((String)form.get(PROCESSOR_DESC));					
			updatedProc.setCommunityId(Integer.parseInt((String)form.get(OWNING_COMMUNITY)));					
			updatedProc.setId(Integer.parseInt((String)form.get(PROCESSOR_ID)));
			
			String procType = (String)form.get(PROCESSOR_TYPE);
			updatedProc.setType(toProcessorEnum(procType));	
			
			FileItem processorFile = (FileItem)form.get(PROCESSOR_FILE);
			if(!Util.isNullOrEmpty(processorFile.getName())) {
				// If the file is being updated, save it to disk...
				File newFile = this.getProcessorFilePath(updatedProc.getCommunityId(), processorFile.getName());
				processorFile.write(newFile);
				updatedProc.setFilePath(newFile.getAbsolutePath());					
				log.info(String.format("Wrote new %s processor to %s for community %d", procType, newFile.getAbsolutePath(), updatedProc.getCommunityId()));	
			}
			
			// Get the original type information from the database
			Processor originalProc = Processors.get(updatedProc.getId());
			
			// If there's a difference between names, update it
			if(!originalProc.getName().equals(updatedProc.getName())) {
				Processors.updateName(originalProc.getId(), updatedProc.getName());
			}

			// If there's a difference between descriptions, update it
			if(!originalProc.getDescription().equals(updatedProc.getDescription())) {
				Processors.updateDescription(originalProc.getId(), updatedProc.getDescription());
			}
			
			// If the user specified a new processor script, update it
			if(!org.starexec.util.Util.isNullOrEmpty(updatedProc.getFilePath())) {
				// Update the new path
				Processors.updatePath(originalProc.getId(), updatedProc.getFilePath());
				
				// And remove the old processor script from disk
				File f = new File(originalProc.getFilePath());
				File parentDir = new File(f.getParent());
				f.delete();				
				parentDir.delete();
				
				log.info(String.format("Removed files [%s] and directory [%s] due to an updated benchmark type processor script", f.getAbsolutePath(), parentDir.getAbsolutePath()));
			}
			
			return updatedProc;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}				
		
		return null;
	}
	
	/**
	 * @param type The string version of the type as retrieved from the HTML form
	 * @return The enum representation of the type
	 */
	private ProcessorType toProcessorEnum(String type) {
		if(type.equals(POST_PROCESS_TYPE)) {
			return ProcessorType.POST;
		} else if (type.equals(PRE_PROCESS_TYPE)) {
			 return ProcessorType.PRE;
		} else if(type.equals(BENCH_TYPE)) {
			return ProcessorType.BENCH;
		}
		
		return ProcessorType.DEFAULT;
	}	

	/**
	 * Creates a unique file path for the given file to write in the benchmark type directory
	 * @param communityId The id of the community (used in the path)
	 * @param fileName The name of the file to create in the unique directory
	 * @return The file object associated with the new file path (all necessary directories are created as needed)
	 */
	private File getProcessorFilePath(int communityId, String fileName) {
		// Get the base benchmark type directory and add community ID
		File saveDir = new File(R.PROCESSOR_DIR, "" + communityId);			
		
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
			if(!form.containsKey(PROCESSOR_NAME) ||
			   !form.containsKey(PROCESSOR_DESC) ||
			   !form.containsKey(OWNING_COMMUNITY) ||
			   !form.containsKey(PROCESSOR_TYPE) ||
			   !form.containsKey(PROCESSOR_FILE)) {
				return false;
			}
										
			if(!Validator.isValidPrimName((String)form.get(PROCESSOR_NAME))) {
				return false;
			}
																	
			if(!Validator.isValidPrimDescription((String)form.get(PROCESSOR_DESC))) {
				return false;
			}
			
			if(!Validator.isValidInteger((String)form.get(OWNING_COMMUNITY))) {
				return false;
			}
			
			String procType = (String)form.get(PROCESSOR_TYPE);
			if(!procType.equals(POST_PROCESS_TYPE) && 
			   !procType.equals(PRE_PROCESS_TYPE) && 
			   !procType.equals(BENCH_TYPE)) {
					return false;
			}
			
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
			if(!form.containsKey(PROCESSOR_ID)) {
				return false;
			} else if(!Validator.isValidInteger((String)form.get(PROCESSOR_ID))) {
				return false;
			}
			
			// Now run through the create request validator, they share the same fields			
			return this.isValidCreateRequest(form);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}