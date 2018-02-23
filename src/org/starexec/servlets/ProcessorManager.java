package org.starexec.servlets;

import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Processors;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Processor;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Servlet which handles incoming requests to add and update processors
 *
 * @author Tyler Jensen
 */
@MultipartConfig
public class ProcessorManager extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(ProcessorManager.class);

	// The unique date stamped file name format (for saving processor files)
	private static final DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);

	// Request attributes
	private static final String PROCESSOR_NAME = "name";
	private static final String PROCESSOR_DESC = "desc";
	private static final String PROCESSOR_FILE = "file";
	private static final String PROCESSOR_URL = "processorUrl";
	private static final String UPLOAD_METHOD = "uploadMethod";
	private static final String LOCAL_UPLOAD_METHOD = "local";
	private static final String OWNING_COMMUNITY = "com";

	private static final String ACTION = "action";
	private static final String ADD_ACTION = "add";

	private static final String PROCESSOR_TYPE = "type";
	private static final String PRE_PROCESS_TYPE = "pre";
	private static final String POST_PROCESS_TYPE = "post";
	private static final String UPDATE_PROCESS_TYPE = "update";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Path("/update")
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {

			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request);
				String action = (String) form.get(ACTION);

				// Make sure we have an action parameter
				if (action != null) {
					// Delegate the request based on the action
					if (action.equals(ADD_ACTION)) {
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
			log.warn("Caught Exception in ProcessorManager.doPost", e);
		}
	}

	/**
	 * Handles requests to add a processor
	 */
	private void handleAddRequest(
			HashMap<String, Object> form, HttpServletRequest request, HttpServletResponse response
	) throws Exception {
		try {
			// If we're dealing with an upload request...
			// Make sure the request is valid
			ValidatorStatusCode status = isValidCreateRequest(form);
			if (!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
				return;
			}
			// Make sure this user has the ability to add type to the space
			int community = Integer.parseInt((String) form.get(OWNING_COMMUNITY));
			if (!SessionUtil.getPermission(request, community).isLeader()) {
				response.sendError(
						HttpServletResponse.SC_FORBIDDEN, "Only community leaders can add types to their communities");

				return;
			}

			// Add the benchmark type to the database/filesystem
			Processor result = this.addNewProcessor(form);

			// Redirect based on the results of the addition
			if (result != null) {
				response.addCookie(new Cookie("New_ID", String.valueOf(result.getId())));
				response.sendRedirect(Util.docRoot("secure/edit/community.jsp?cid=" + result.getCommunityId()));
			} else {
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Failed to add new processor. Please ensure the archive is in the correct format, with a " +
								"process script in the top level."
				);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/**
	 * Given a directory, recursively sets all files in the directory as executable
	 *
	 * @param directory The directory in question
	 */
	private static void setAllFilesExecutable(File directory) {
		for (File f : directory.listFiles()) {
			if (f.isDirectory()) {
				setAllFilesExecutable(f);
			} else {
				if (f.setExecutable(true, false)) {
					log.debug("successfully set processor as executable: " + f.getAbsolutePath());
				} else {
					log.warn("Could not set processor as executable: " + f.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Parses through form items and builds a new Processor object from it. Then it is added to the database. Also
	 * writes the processor file to disk included in the request.
	 *
	 * @param form The form fields for the request
	 * @return The Processor that was added to the database if it was successful
	 */
	private Processor addNewProcessor(HashMap<String, Object> form) {
		final String method = "addNewProcessor";
		try {
			Processor newProc = new Processor();
			newProc.setName((String) form.get(PROCESSOR_NAME));
			newProc.setDescription((String) form.get(PROCESSOR_DESC));
			newProc.setCommunityId(Integer.parseInt((String) form.get(OWNING_COMMUNITY)));

			String uploadMethod = (String) form.get(UPLOAD_METHOD);

			log.debug(method + " - upload method for the processor=" + uploadMethod);
			String procType = (String) form.get(PROCESSOR_TYPE);
			newProc.setType(toProcessorEnum(procType));

			File uniqueDir = getProcessorDirectory(newProc.getCommunityId(), newProc.getName());

			File archiveFile;

			URL processorUrl;

			if (uploadMethod.equals(LOCAL_UPLOAD_METHOD)) {
				// Save the uploaded file to disk
				PartWrapper processorFile = (PartWrapper) form.get(PROCESSOR_FILE);
				archiveFile = new File(uniqueDir, FilenameUtils.getName(processorFile.getName()));
				processorFile.write(archiveFile);
			} else {
				processorUrl = new URL((String) form.get(PROCESSOR_URL));
				String name;
				try {
					name = processorUrl.toString().substring(processorUrl.toString().lastIndexOf('/'));
				} catch (Exception e) {
					// if something goes wrong just make the name directory-friendly and continue.
					name = processorUrl.toString().replace('/', '-');
				}
				archiveFile = new File(uniqueDir, name);
				if (!Util.copyFileFromURLUsingProxy(processorUrl, archiveFile)) {
					throw new StarExecException("Unable to copy file from URL");
				}
			}


			newProc.setFilePath(uniqueDir.getAbsolutePath());

			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());

			File processorScript = new File(uniqueDir, R.PROCESSOR_RUN_SCRIPT);
			if (!processorScript.exists()) {
				log.warn("the new processor did not have a process script!");
				return null;
			}
			ProcessorManager.setAllFilesExecutable(new File(newProc.getFilePath()));


			log.info(String.format("Wrote new %s processor to %s for community %d", procType,
			                       uniqueDir.getAbsolutePath(), newProc.getCommunityId()
			));

			int newProcId = Processors.add(newProc);
			if (newProcId > 0) {
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
		switch (type) {
		case POST_PROCESS_TYPE:
			return ProcessorType.POST;
		case PRE_PROCESS_TYPE:
			return ProcessorType.PRE;
		case R.BENCHMARK:
			return ProcessorType.BENCH;
		case UPDATE_PROCESS_TYPE:
			return ProcessorType.UPDATE;
		}

		return ProcessorType.DEFAULT;
	}

	/**
	 * Creates a unique file path for the given file to write in the benchmark type directory
	 *
	 * @param communityId The id of the community (used in the path)
	 * @param procName the name of processor (and the directory for the processor)
	 * @return The file object associated with the new file path (all necessary directories are created as needed)
	 */
	public static File getProcessorDirectory(int communityId, String procName) {
		File uniqueDir = new File(R.getProcessorDir(), "" + communityId);
		//use the date to make sure the directory is unique
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		uniqueDir = new File(uniqueDir, procName);
		uniqueDir.mkdirs();
		return uniqueDir;
	}

	/**
	 * Uses the Validate util to ensure the incoming type upload request is valid. This checks for illegal characters
	 * and content length requirements.
	 *
	 * @param form The form to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValidCreateRequest(HashMap<String, Object> form) {
		final String method = "isValidCreateRequest";
		try {


			if (!Validator.isValidProcessorName((String) form.get(PROCESSOR_NAME))) {

				return new ValidatorStatusCode(false,
				                               "The supplied name is invalid-- please refer to the help files to see " +
						                               "the correct format");
			}

			String uploadMethod = (String) form.get(UPLOAD_METHOD);

			String fileName;

			if (uploadMethod.equals(LOCAL_UPLOAD_METHOD)) {
				fileName = ((PartWrapper) form.get(PROCESSOR_FILE)).getName();
			} else {
				fileName = (String) form.get(PROCESSOR_URL);
			}

			log.debug(method + " - Name of processor file=" + fileName);

			if (!Validator.isValidArchiveType(fileName)) {
				return new ValidatorStatusCode(false, "Uploaded archives must be a .zip, .tar, or .tgz");
			}

			if (!Validator.isValidPrimDescription((String) form.get(PROCESSOR_DESC))) {

				return new ValidatorStatusCode(false,
				                               "The supplied description is invalid-- please refer to the help files " +
						                               "to see the correct format");
			}

			if (!Validator.isValidPosInteger((String) form.get(OWNING_COMMUNITY))) {

				return new ValidatorStatusCode(false, "The given community ID is not a valid integer");
			}

			String procType = (String) form.get(PROCESSOR_TYPE);
			if (procType == null || !procType.equals(POST_PROCESS_TYPE) && !procType.equals(PRE_PROCESS_TYPE) &&
					!procType.equals(R.BENCHMARK) && !procType.equals(UPDATE_PROCESS_TYPE)) {

				return new ValidatorStatusCode(false, "The given processor type is invalid");
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
