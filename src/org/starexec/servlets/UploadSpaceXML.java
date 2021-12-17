package org.starexec.servlets;

import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Uploads;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Permission;
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
 * Allows for the uploading of space hierarchies represented in xml. Files can come in .zip, .tar, or .tar.gz format.
 *
 * @author Benton McCune
 */
@MultipartConfig
public class UploadSpaceXML extends HttpServlet {

	private static final StarLogger log = StarLogger.getLogger(UploadSpaceXML.class);
	private final DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
	private static final String SPACE_ID = R.SPACE;
	private static final String UPLOAD_FILE = "f";

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int userId = SessionUtil.getUserId(request);
		try {
			// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request);

				ValidatorStatusCode status = this.isValidRequest(form);
				// Make sure the request is valid
				if (!status.isSuccess()) {
					//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
					return;
				}

				Integer spaceId = Integer.parseInt((String) form.get(SPACE_ID));
				if (!userMayUploadSpaceXML(userId, spaceId)) {
					response.sendError(
							HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add a space here.");
					return;
				}

				BatchUtil batchUtil = new BatchUtil();
				int statusId = Uploads.createSpaceXMLUploadStatus(userId);
				this.handleXMLFile(userId, spaceId, form, batchUtil, statusId);

				// Note: Inherit users is handled in BatchUtil's createSpaceFromElement(...)

				// Redirect based on success/failure


				response.sendRedirect(Util.docRoot("secure/details/XMLuploadStatus.jsp?id=" + statusId));
			} else {
				// Got a non multi-part request, invalid
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.error("Caught Exception in UploadSpaceXML.doPost", e);
		}
	}

	private boolean userMayUploadSpaceXML(int userId, int spaceId) {
		Permission userPermission = Permissions.get(userId, spaceId);
		return userPermission != null && userPermission.canAddSpace();
	}

	/**
	 * This method is responsible for uploading a compressed folder with an xml representation
	 *
	 * @param userId the user ID of the user making the upload request
	 * @param form the HashMap representation of the upload request
	 * @param batchUtil a BatchUtil object we can use for setting an error message if a problem is encountered.
	 * @throws Exception
	 * @author Benton McCune
	 */
	public void handleXMLFile(
			final int userId, final int spaceId, final HashMap<String, Object> form, final BatchUtil batchUtil,
			final int statusId
	) {
		try {
			log.debug("Handling Upload of XML File from User " + userId);
			PartWrapper item = (PartWrapper) form.get(UploadSpaceXML.UPLOAD_FILE);
			// Don't need to keep file long - just using download directory
			File uniqueDir = new File(R.getBatchSpaceXMLDir(), "" + userId);
			uniqueDir = new File(uniqueDir, "TEMP_XML_FOLDER_");
			uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));

			uniqueDir.mkdirs();

			//Process the archive file and extract

			File archiveFile = new File(uniqueDir, FilenameUtils.getName(item.getName()));
			new File(archiveFile.getParent()).mkdir();
			item.write(archiveFile);
			final String archivePath = uniqueDir.getCanonicalPath();
			Util.threadPoolExecute(() -> {
				try {
					ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
					archiveFile.delete();
					Uploads.XMLFileUploadComplete(statusId);
//create new file reference for inside the scope of the Runnable, same as uniqueDir:
					File archiveLocation = new File(archivePath);
					//Typically there will just be 1 file, but might as well allow more
					for (File file : archiveLocation.listFiles()) {
						if (!file.isFile()) {
							Uploads.setXMLErrorMessage(
									statusId, "The file '" + file.getName() +
											"' is not a regular file.  Only regular files containing space XML are " +
											"allowed in the uploaded archive.");
						} else {
							List<Integer> current = batchUtil.createSpacesFromFile(file, userId, spaceId, statusId);
							if (current == null) {
								Uploads.setXMLErrorMessage(statusId, batchUtil.getErrorMessage());
							}
						}
					}
				} catch (Exception e) {
					log.error(Util.getStackTrace(e));
					Uploads.setBenchmarkErrorMessage(statusId, e.getMessage());
				}
				Uploads.XMLEverythingComplete(statusId);
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Sees if a given String -> Object HashMap is a valid Upload Space XML request. Checks to see if it contains all
	 * the information needed and if the information is in the right format.
	 *
	 * @param form the HashMap representing the upload request.
	 * @return true iff the request is valid
	 * @author Benton McCune
	 */
	private ValidatorStatusCode isValidRequest(HashMap<String, Object> form) {
		try {

			if (!Validator.isValidPosInteger((String) form.get(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The given space ID is not a valid integer");
			}
			Integer.parseInt((String) form.get(SPACE_ID));

			String fileName = FilenameUtils.getName(((PartWrapper) form.get(UploadSpaceXML.UPLOAD_FILE)).getName());
			if (!Validator.isValidArchiveType(fileName)) {
				return new ValidatorStatusCode(false, "Uploaded archives must be .zip, .tar, or .tgz");
			}
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		return new ValidatorStatusCode(false, "Internal error uploading space XML");
	}
}
