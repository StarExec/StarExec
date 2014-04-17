package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.commons.io.FilenameUtils;
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
	private static DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
	
	// Request attributes
	private static final String PROCESSOR_NAME = "name";
	private static final String PROCESSOR_DESC = "desc";
	private static final String PROCESSOR_FILE = "file";	
	private static final String OWNING_COMMUNITY = "com";
	
	
	private static final String ACTION = "action";
	private static final String ADD_ACTION = "add";
	
	private static final String PROCESSOR_TYPE = "type";
	private static final String BENCH_TYPE = "bench";
	private static final String PRE_PROCESS_TYPE = "pre";
	private static final String POST_PROCESS_TYPE = "post";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}	
	
	@GET
	@Path("/update")
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
				response.addCookie(new Cookie("New_ID",String.valueOf(result.getId())));
			    response.sendRedirect(Util.docRoot("secure/edit/community.jsp?cid=" + result.getCommunityId()));	
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
			File newFile = ProcessorManager.getProcessorFilePath(newProc.getCommunityId(), FilenameUtils.getName(processorFile.getName()));
			processorFile.write(newFile);
			
			if (!newFile.setExecutable(true, false)) {			
				log.warn("Could not set processor as executable: " + newFile.getAbsolutePath());
			}
			
			newProc.setFilePath(newFile.getAbsolutePath());			
			log.info(String.format("Wrote new %s processor to %s for community %d", procType, newFile.getAbsolutePath(), newProc.getCommunityId()));					
			
			int newProcId=Processors.add(newProc);
			if(newProcId>0) {
				newProc.setId(newProcId);
				return newProc;					
			}						
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
	public static File getProcessorFilePath(int communityId, String fileName) {
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
	
}