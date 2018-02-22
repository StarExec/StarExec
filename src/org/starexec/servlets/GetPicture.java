package org.starexec.servlets;

import org.apache.commons.io.FileUtils;
import org.starexec.constants.R;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.logger.StarLogger;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Handles the request to get the picture from the file system. If there is such a picture for the request, or else a
 * default one, named as Pic0.jpg is returned.
 *
 * @author Ruoyu Zhang & Todd Elvers
 */
public class GetPicture extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(GetPicture.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			// If the request is not valid, then respond with an error
			ValidatorStatusCode status = validateRequest(request);
			if (!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
				return;
			}

			// Check what type is the request, and generate file in different folders according to it.
			String defaultPicFilename = "";
			String picFilename = "";
			String pictureDir = R.getPicturePath();
			StringBuilder sb = new StringBuilder();

			if (request.getParameter("type").equals("uthn")) {
				sb.delete(0, sb.length());
				sb.append("users");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append("_thn.jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("users");
			} else if (request.getParameter("type").equals("uorg")) {
				sb.delete(0, sb.length());
				sb.append("users");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append("_org.jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("users");
			} else if (request.getParameter("type").equals("sthn")) {
				sb.delete(0, sb.length());
				sb.append("solvers");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append("_thn.jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("solvers");
			} else if (request.getParameter("type").equals("sorg")) {
				sb.delete(0, sb.length());
				sb.append("solvers");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append("_org.jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("solvers");
			} else if (request.getParameter("type").equals("bthn")) {
				sb.delete(0, sb.length());
				sb.append("benchmarks");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append("_thn.jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("benchmarks");
			} else if (request.getParameter("type").equals("borg")) {
				sb.delete(0, sb.length());
				sb.append("benchmarks");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append("_org.jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("benchmarks");
			} else if (request.getParameter("type").equals("corg")) {
				sb.delete(0, sb.length());
				sb.append("resultCharts");
				sb.append(File.separator);
				sb.append("Pic");
				sb.append(request.getParameter("Id").toString());
				sb.append(".jpg");

				defaultPicFilename = GetPicture.getDefaultPicture("chart");
			}

			picFilename = sb.toString();

			sb.delete(0, sb.length());
			sb.append(pictureDir);
			sb.append(File.separator);
			sb.append(picFilename);
			File file = new File(sb.toString());

			// If the desired file exists, then the file will return it, or else return the default file Pic0.jpg
			if (!file.exists()) {
				sb.delete(0, sb.length());
				sb.append(pictureDir);
				sb.append(File.separator);
				sb.append(defaultPicFilename);
				file = new File(sb.toString());
			}

			// Return the file in the response.
			try {
				java.io.OutputStream os = response.getOutputStream();
				FileUtils.copyFile(file, os);
			} catch (Exception e) {
				log.warn("User: " + System.getProperty("user.name") + "\nCan Read: " + file.canRead() + "\nExists: " + file.exists());
				log.warn("picture with path " + file.getAbsolutePath() + " could not be found", e);
			}
		} catch (Exception e) {
			log.warn("Caught Exception in GetPicture.doGet", e);
			throw e;
		}
	}

	/**
	 * Validates the GetPicture request to make sure the requested data is of the right format
	 *
	 * @param request The request need to be validated.
	 * @return true if the request is valid.
	 */
	private static ValidatorStatusCode validateRequest(HttpServletRequest request) {
		try {
			if (!Util.paramExists("type", request)) {
				return new ValidatorStatusCode(false, "The supplied type is not valid");
			}

			if (!Validator.isValidPosInteger(request.getParameter("Id"))) {
				return new ValidatorStatusCode(false, "The supplied id is not a valid integer");
			}

			if (!(request.getParameter("type").equals("uthn") || request.getParameter("type").equals("uorg") ||
					request.getParameter("type").equals("sthn") || request.getParameter("type").equals("sorg") ||
					request.getParameter("type").equals("bthn") || request.getParameter("type").equals("borg") ||
					request.getParameter("type").equals("corg"))) {
				return new ValidatorStatusCode(false, "The supplied type is not valid");
			}

			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		return new ValidatorStatusCode(false, "Internal error getting image");
	}

	/**
	 * Gets the path of the default picture for a particular primitive
	 *
	 * @param primType the type of primitive whose default picture we need
	 * @return the default picture of the specified primitive type
	 * @author Todd Elvers
	 */
	private static String getDefaultPicture(String primType) {
		StringBuilder sb = new StringBuilder();

		switch (primType.charAt(0)) {
			case 'u':
				sb.append("users");
				sb.append(File.separator);
				sb.append("Pic0.jpg");
				break;
			case 'b':
				sb.append("benchmarks");
				sb.append(File.separator);
				sb.append("Pic0.jpg");
				break;
			case 's':
				sb.append("solvers");
				sb.append(File.separator);
				sb.append("Pic0.jpg");
				break;
			case 'c':
				sb.append("resultCharts");
				sb.append(File.separator);
				sb.append("Pic0.jpg");
				break;
		}

		return sb.toString();
	}
}
