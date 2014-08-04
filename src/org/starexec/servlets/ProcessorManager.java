package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Processors;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.util.ArchiveUtil;
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
    private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};

	
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
			ValidatorStatusCode status=isValidCreateRequest(form);
			if(!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
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
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to add new processor. Please ensure the archive is in the correct format, with a " +
						"process script in the top level.");	
			}									
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			
		}	
	}
	
	/**
	 * Attempts to copy a processor in the old format over to the new format
	 * @param p the processor to copy over
	 * @return true on success, false on failure. Failure will occur for a processor that is already in the new format
	 * @throws Exception
	 */
	private static boolean copyProcessorToNewFormat(Processor p) throws Exception {
		
		File curFile=new File(p.getFilePath());
		if (curFile.exists() && !curFile.isDirectory()) {
			File newDirectory=getProcessorDirectory(p.getCommunityId(),p.getName());
			if (newDirectory.exists()) {
				File destination=new File(newDirectory,R.PROCSSESSOR_RUN_SCRIPT);
				FileUtils.copyFile(curFile,destination);
				return Processors.updateFilePath(p.getId(), newDirectory.getAbsolutePath());
			}
			
			
		}
		return true; //we didn't need to do anything, so this is a success
	}
	
	/**
	 * One time task for copying all existing processors over into the new format.
	 */
	public static void copyAllProcessorsToNewFormat() {
		ProcessorType[] types={ProcessorType.BENCH, ProcessorType.POST, ProcessorType.PRE, ProcessorType.DEFAULT};
		for (ProcessorType type : types) {
			List<Processor> procs=Processors.getAll(type);
			for (Processor p : procs) {
				try {
					if (!copyProcessorToNewFormat(p)) {
						log.error("error when trying to copy processor id = "+p.getId());
					}
				} catch(Exception e ){
					log.error("got an error when trying to copy processor id = "+p.getId());
					log.error(e.getMessage(),e);
				}
				
			}
		}
	}
	/**
	 * Given a directory, recursively sets all files in the directory as executable
	 * @param directory The directory in quesiton
	 */
	private static void setAllFilesExecutable(File directory) {
		for (File f : directory.listFiles()) {
			if (f.isDirectory()) {
				setAllFilesExecutable(f);
			} else {
				if (!f.setExecutable(true,false)) {
					log.warn("Could not set processor as executable: " + f.getAbsolutePath());
				} else {
					log.debug("successfully set processor as executable: "+f.getAbsolutePath());
				}
			}
		}
	}
	
	/**
	 * Sets all processors that are in the proper new format to executable
	 */
	public static void setAllProcessorsExecutable() {
		ProcessorType[] types={ProcessorType.BENCH, ProcessorType.POST, ProcessorType.PRE, ProcessorType.DEFAULT};
		for (ProcessorType type : types) {
			List<Processor> procs=Processors.getAll(type);
			for (Processor p : procs) {
				try {
					setAllFilesExecutable(new File(p.getFilePath()));
					//File exec=new File(p.getExecutablePath());
					//if (!exec.setExecutable(true, false)) {			
					//	log.warn("Could not set processor as executable: " + exec.getAbsolutePath());
					//}
				} catch (Exception e) {
					log.error("error setting processor executable id = "+p.getId());
					log.error(e.getMessage(),e);
				}
				
				
				
			}
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
			
			File archiveFile=null;
			
			File uniqueDir = getProcessorDirectory(newProc.getCommunityId(),newProc.getName());

			archiveFile = new File(uniqueDir,  FilenameUtils.getName(processorFile.getName()));
			
			processorFile.write(archiveFile);
			newProc.setFilePath(uniqueDir.getAbsolutePath());

				
			
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			
			File processorScript=new File(uniqueDir,R.PROCSSESSOR_RUN_SCRIPT);
			if (!processorScript.exists()) {
				log.warn("the new processor did not have a process script!");
				return null;
			}
			ProcessorManager.setAllFilesExecutable(new File(newProc.getFilePath()));
			//if (!processorScript.setExecutable(true, false)) {			
			//	log.warn("Could not set processor as executable: " + processorScript.getAbsolutePath());
			//}
			
	
			log.info(String.format("Wrote new %s processor to %s for community %d", procType, uniqueDir.getAbsolutePath(), newProc.getCommunityId()));					
			
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
	public static File getProcessorDirectory(int communityId, String procName) {
		File uniqueDir = new File(R.PROCESSOR_DIR, "" + communityId);
		//use the date to make sure the directory is unique
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		uniqueDir = new File(uniqueDir, procName);
		uniqueDir.mkdirs();
		return uniqueDir;
	}
	
	/**
	 * Uses the Validate util to ensure the incoming type upload request is valid. This checks for illegal characters
	 * and content length requirements.
	 * @param form The form to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValidCreateRequest(HashMap<String, Object> form) {
		try {			
			  
										
			if(!Validator.isValidPrimName((String)form.get(PROCESSOR_NAME))) {

				return new ValidatorStatusCode(false,"The supplied name is invalid-- please refer to the help files to see the correct format");
			}
			
			boolean goodExtension=false;
			String fileName = ((FileItem)form.get(PROCESSOR_FILE)).getName();
			for(String ext : ProcessorManager.extensions) {
				if(fileName.endsWith(ext)) {
					goodExtension=true;
				}
			}
			if (!goodExtension) {
				return new ValidatorStatusCode(false,"Uploaded archives must be a .zip, .tar, or .tgz");
			}
																	
			if(!Validator.isValidPrimDescription((String)form.get(PROCESSOR_DESC))) {

				return new ValidatorStatusCode(false,"The supplied description is invalid-- please refer to the help files to see the correct format");
			}
			
			if(!Validator.isValidInteger((String)form.get(OWNING_COMMUNITY))) {

				return new ValidatorStatusCode(false,"The given community ID is not a valid integer");
			}
			
			String procType = (String)form.get(PROCESSOR_TYPE);
			if(procType==null || !procType.equals(POST_PROCESS_TYPE) && 
			   !procType.equals(PRE_PROCESS_TYPE) && 
			   !procType.equals(BENCH_TYPE)) {

				return new ValidatorStatusCode(false,"The given processor type is invalid");
			}
			// Passed all checks, return true
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(true, "Internal error processing request");
	}
	
}