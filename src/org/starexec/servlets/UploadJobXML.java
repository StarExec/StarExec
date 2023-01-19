package org.starexec.servlets;

import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Permission;
import org.starexec.data.to.enums.ConfigXmlAttribute;
import org.starexec.data.to.enums.JobXmlType;
import org.starexec.data.to.tuples.ConfigAttrMapPair;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Allows for the creation of job pairs as represented in xml. Files can come in .zip, .tar, or .tar.gz format.
 *
 * @author Tim Smith
 */
@MultipartConfig
public class UploadJobXML extends HttpServlet {

	private static final StarLogger log = StarLogger.getLogger(UploadJobXML.class);
	private static final String UPLOAD_FILE = "f";
	private static final DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
	private static final String SPACE_ID = R.SPACE;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String method = "doPost";
		log.entry(method);
		int userId = SessionUtil.getUserId(request);
		try {
			// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				log.info(method, "Got request to upload job xml from user with id=" + userId);

				log.info(method, "Parsing job xml upload request.");
				HashMap<String, Object> form = Util.parseMultipartRequest(request);
				log.info(method, "Finished parsing job xml upload request.");

				// Make sure the request is valid
				log.info(method, "Checking that the job xml upload request is valid.");
				ValidatorStatusCode status = this.isValidRequest(form);
				log.info(method, "Finished checking that the job xml upload request is valid.");

				if (!status.isSuccess()) {
					log.info(method, "Request to upload job xml was not valid. Sending bad request error.");
					//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
					return;
				}


				Integer spaceId = Integer.parseInt((String) form.get(SPACE_ID));
				if (!userMayUploadJobXML(userId, spaceId)) {
					response.sendError(
							HttpServletResponse.SC_FORBIDDEN, "You are not allowed to run jobs in this space.");
					return;
				}

				JobUtil jobUtil = new JobUtil();

				List<Integer> result = this.handleXMLFile(userId, spaceId, form, jobUtil);

				// Redirect based on success/failure
				if (result != null) {
					//send back new ids to the user
					log.info(method, "Sending back new job ids to user.");
					response.addCookie(new Cookie("New_ID", Util.makeCommaSeparatedList(result)));
					response.sendRedirect(Util.docRoot("secure/explore/spaces.jsp"));
				} else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					                   "Failed to upload Job XML:\n" + jobUtil.getErrorMessage()
					);
				}
			} else {
				// Got a non multi-part request, invalid
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.error("Caught Exception in UploadJobXML.doPost", e);
		}
	}

	private boolean userMayUploadJobXML(int userId, int spaceId) {
		Permission userPermissions = Permissions.get(userId, spaceId);
		return (userPermissions != null) && userPermissions.canAddJob();
	}

	/**
	 * Gets the XML file from the form and tries to create a job from it
	 *
	 * @param userId the ID of the user making the request
	 * @param form a hashmap representation of the form on secure/add/batchJob.jsp
	 * @author Tim Smith
	 */
	private List<Integer> handleXMLFile(int userId, int spaceId, HashMap<String, Object> form, JobUtil jobUtil)
			throws StarExecException {
		final String method = "handleXMLFile";
		log.entry(method);
		try {
			log.info(method, "Handling Upload of XML File from user with id=" + userId);
			PartWrapper item = (PartWrapper) form.get(UploadJobXML.UPLOAD_FILE);
			// Don't need to keep file long - just using download directory

			File uniqueDir = new File(R.getBatchSpaceXMLDir(), "Job" + userId);
			uniqueDir = new File(uniqueDir, "TEMP_JOB_XML_FOLDER_");
			uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));

			uniqueDir.mkdirs();
			//Process the archive file and extract

			File archiveFile = new File(uniqueDir, FilenameUtils.getName(item.getName()));
			new File(archiveFile.getParent()).mkdir();
			item.write(archiveFile);
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			archiveFile.delete();

			//Typically there will just be 1 file, but might as well allow more

			List<Integer> jobIds = new ArrayList<>();

			// Makes sure there are no directories in archive file.
			checkForIllegalDirectoriesInXMLArchive(uniqueDir);

			log.info(method, "Started creating jobs from XML files");
			for (File file : uniqueDir.listFiles()) {
				final List<Integer> current = jobUtil.createJobsFromFile(file, userId, spaceId, JobXmlType.STANDARD,
				                                                         new ConfigAttrMapPair(ConfigXmlAttribute.ID)
				);

				if (current != null) {
					jobIds.addAll(current);
				} else {
					log.debug("the uploaded job xml was not formatted correctly");
				}
			}
			log.info(method, "Finished creating jobs from XML files.");
			if (jobUtil.getJobCreationSuccess()) {
				log.info(method, "Job(s) created successfully.");
				return jobIds;
			}
			log.debug(method, jobUtil.getErrorMessage());
			log.debug(method, "Job(s) could not be created from XML.");
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new StarExecException(e.getMessage());
		}
	}

	private void checkForIllegalDirectoriesInXMLArchive(File extractedArchiveDirectory) throws StarExecException {
		final String method = "checkForIllegalDirectoriesInXMLArchive";
		log.info(method, "checking for directories in extracted XML archive.");
		for (String name : extractedArchiveDirectory.list()) {
			log.info(method, "Found file with name=" + name);
			File file = new File(extractedArchiveDirectory, name);
			if (file.isDirectory()) {
				log.info(method, "Found directory in JobXML archive file. Throwing exception.");
				throw new StarExecException("Directories not allowed in job XML archive.");
			}
		}
		log.info(method, "Finished checking for directories in extracted XML archive.");
	}

	private ValidatorStatusCode isValidRequest(HashMap<String, Object> form) {
		try {
			if (!form.containsKey(UploadJobXML.UPLOAD_FILE) || !form.containsKey(SPACE_ID)) {
				return new ValidatorStatusCode(
						false, "Missing field from the form for the file to upload or the space" + " id");
			}

			if (!Validator.isValidPosInteger((String) form.get(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The supplied space ID was not a valid integer");
			}

			String fileName = FilenameUtils.getName(((PartWrapper) form.get(UploadJobXML.UPLOAD_FILE)).getName());
			if (!Validator.isValidArchiveType(fileName)) {
				return new ValidatorStatusCode(false, "Uploaded files must be .zip, .tar, or .tgz");
			}
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "Internal error uploading job XML");
	}
}
