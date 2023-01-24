package org.starexec.servlets;

import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Solvers;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.UploadSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.logger.StarLogger;
import org.starexec.util.PartWrapper;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Supports the uploading of new configuration files to the Starexec file system
 *
 * @author Todd Elvers
 */
@MultipartConfig
public class UploadConfiguration extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(UploadConfiguration.class);

	// Param constants to use to process the form
	private static final String CONFIG_DESC = "uploadConfigDesc";
	private static final String SOLVER_ID = "solverId";
	private static final String UPLOAD_FILE = "file";
	private static final String CONFIG_NAME = "uploadConfigName";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int userId = SessionUtil.getUserId(request);
		try {
			if (UploadSecurity.uploadsFrozen()) {
				response.sendError(
					HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"Uploading solver configurations is currently disabled"
				);
				return;
			}

			// Ensure request is a file upload request (i.e. a multipart request)
			if (ServletFileUpload.isMultipartContent(request)) {

				// Get the configuration form attributes from add/configuration.jsp
				HashMap<String, Object> configAttrMap = Util.parseMultipartRequest(request);

				// Parameter validation
				ValidatorStatusCode status = isValidRequest(configAttrMap);

				if (!status.isSuccess()) {
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, status.getMessage());
					return;
				}

				status = SolverSecurity
						.canUserAddConfiguration(Integer.parseInt((String) configAttrMap.get(SOLVER_ID)), userId);
				if (!status.isSuccess()) {
					//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, status.getMessage());
					return;
				}


				// Process the configuration file and write it to the parent solver's /bin directory, then update the
				// solver's disk_size attribute
				ValidatorStatusCode result = handleConfiguration(configAttrMap);

				// Redirect user based on how the configuration handling went
				if (!result.isSuccess()) {
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, result.getMessage()));

					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result.getMessage());
				} else {

					//result should be the new ID of the configuration
					response.addCookie(new Cookie("New_ID", String.valueOf(result.getStatusCode())));
					response.sendRedirect(Util.docRoot(
							"secure/details/solver.jsp?id=" + Integer.parseInt((String) configAttrMap.get(SOLVER_ID)
							)));
				}
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.warn("Caught Exception in UploadConfiguration.doPost", e);
		}
	}

	/**
	 * Writes a newly uploaded configuration file to disk in its parent solver's bin directory, normalizes its file
	 * endings, then updates the parent solver's disk_size attribute to reflect the newly added configuration file
	 *
	 * @param configAttrMap the map of form fields -> form values from the add/configurations.jsp page
	 * @return the id of the newly created configuration file,<br> -1 if a general error occurred while handling the
	 * new
	 * configuration,<br> -2 if a configuration file already exists on disk with the same name
	 * @author Todd Elvers
	 */
	public ValidatorStatusCode handleConfiguration(HashMap<String, Object> configAttrMap) {
		try {
			// Set up a new configuration object with the submitted information
			PartWrapper uploadedFile = (PartWrapper) configAttrMap.get(UPLOAD_FILE);
			Solver solver = Solvers.get(Integer.parseInt((String) configAttrMap.get(SOLVER_ID)));
			Configuration newConfig = new Configuration();
			newConfig.setName((String) configAttrMap.get(CONFIG_NAME));
			newConfig.setDescription((String) configAttrMap.get(CONFIG_DESC));
			newConfig.setSolverId(solver.getId());

			// Build a path to the appropriate solver bin directory
			File newConfigFile = new File(Util.getSolverConfigPath(solver.getPath(), newConfig.getName()));

			// If a configuration file exists on disk with the same name, append an integer to the file to make it
			// unique
			// If this cannot be done, and error is returned
			if (newConfigFile.exists()) {
				boolean fileAlreadyExists = true;
				int intSuffix = 0;
				while (fileAlreadyExists) {
					File temp = new File(newConfigFile.getAbsolutePath() + (++intSuffix));
					if (!temp.exists()) {
						newConfigFile = temp;
						newConfig.setName((String) configAttrMap.get(CONFIG_NAME) + intSuffix);
						fileAlreadyExists = false;
						if (!Validator.isValidConfigurationName(newConfig.getName())) {
							return new ValidatorStatusCode(
									false,
									"The solver already has a configuration with this name, and a new name could not " +
											"be generated because the name was already the maximum length"
							);
						}
					}
				}
			}

			// Write the new configuration file to disk
			uploadedFile.write(newConfigFile);

			// Make sure the configuration has the right line endings
			Util.normalizeFile(newConfigFile);

			//Makes executable
			newConfigFile.setExecutable(true);

			// Delete underlying storage for the file item now that it's on disk elsewhere
			uploadedFile.delete();

			// Pass new configuration, and the parent solver objects, to the database & return the result
			int configId = Solvers.addConfiguration(solver, newConfig);
			if (configId > 0) {
				return new ValidatorStatusCode(true, "", configId);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return new ValidatorStatusCode(false, "Internal error adding configuration");
	}

	/**
	 * Validates the parameters of a configuration upload request (request parameters must already be in a HashMap)
	 *
	 * @param configAttrMap the map of form fields -> form values from the add/configurations.jsp page
	 * @return true iff the configuration upload request is valid, false otherwise
	 * @author Todd Elvers
	 */
	private ValidatorStatusCode isValidRequest(HashMap<String, Object> configAttrMap) {
		try {
			// Ensure the map contains all relevant keys
			if (!configAttrMap.containsKey(UPLOAD_FILE)) {
				return new ValidatorStatusCode(false, "No configuration file was given");
			}

			if (!Validator.isValidPosInteger((String) configAttrMap.get(SOLVER_ID))) {
				return new ValidatorStatusCode(false, "The given solver ID is not a valid integer");
			}


			if (configAttrMap.containsKey(CONFIG_DESC)) {
				if (!Validator.isValidPrimDescription((String) configAttrMap.get(CONFIG_DESC))) {

					return new ValidatorStatusCode(
							false,
							"The given description is invalid-- please refer to the help pages to see the proper " +
									"format"
					);
				}
			}
			// Ensure the configuration's name and description are valid
			if (!Validator.isValidConfigurationName((String) configAttrMap.get(CONFIG_NAME))) {

				return new ValidatorStatusCode(
						false, "The given name is invalid-- please refer to the help pages to see the proper format");
			}

			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		return new ValidatorStatusCode(false, "Internal error uploading configuration");
	}
}
