package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Users;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.logger.StarLogger;
import org.starexec.util.PartWrapper;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * This class is to handle the upload of a picture. Notice that this upload will go straight into the file system
 * instead of the database. When a picture is uploaded, the original picture is stored along with a shrunk one for the
 * thumbnail.
 *
 * @author Ruoyu Zhang
 */
@MultipartConfig
public class UploadPicture extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(UploadPicture.class);

	// Request attributes
	private static final String PICTURE_FILE = "f";
	private static final String TYPE = "type";
	private static final String ID = "Id";

	/**
	 * UploadPicture doesn't handle doGet request
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int userIdOfCaller = SessionUtil.getUserId(request);
		try {
			// Extract data from the multipart request
			HashMap<String, Object> form = Util.parseMultipartRequest(request);

			String rawUserIdOfOwner = (String) form.get(UploadPicture.ID);
			int userIdOfOwner = 0;
			if (Validator.isValidPosInteger(rawUserIdOfOwner)) {
				userIdOfOwner = Integer.parseInt(rawUserIdOfOwner);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "User id for request was not an integer.");
				return;
			}


			boolean callerIsOwner = (userIdOfOwner == userIdOfCaller);
			boolean callerIsAdmin = GeneralSecurity.hasAdminWritePrivileges(userIdOfCaller);
			if (!(callerIsOwner || callerIsAdmin) || Users.isPublicUser(userIdOfCaller)) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "You cannot change this user's picture.");
				return;
			}

			ValidatorStatusCode status = this.isRequestValid(form);
			// If the request is valid
			if (status.isSuccess()) {
				response.sendRedirect(this.handleUploadRequest(userIdOfCaller, form));
			} else {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				// Or else the request was invalid, send bad request error
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
			}
		} catch (Exception e) {
			log.warn("Caught Exception in UploadPicture.doPost", e);
		}
	}

	/**
	 * Upload the picture to the file system and return the redirection string.
	 *
	 * @param userId The user uploading picture.
	 * @param form the form submitted for the upload.
	 * @return the redirection string.
	 * @throws Exception
	 * @author Ruoyu Zhang
	 */
	private String handleUploadRequest(int userId, HashMap<String, Object> form) {
		try {
			PartWrapper item = (PartWrapper) form.get(UploadPicture.PICTURE_FILE);
			String fileName = "";
			String redir = Util.docRoot("secure/edit/account.jsp");

			String type = (String) form.get(UploadPicture.TYPE);
			String id = (String) form.get(UploadPicture.ID);
			StringBuilder sb = new StringBuilder();

			switch (type) {
			case "user":
				sb.delete(0, sb.length());
				sb.append("/users/Pic");
				sb.append(id);
				fileName = sb.toString();
				redir = Util.docRoot("secure/edit/account.jsp");
				break;
			case R.SOLVER:
				sb.delete(0, sb.length());
				sb.append("/solvers/Pic");
				sb.append(id);
				fileName = sb.toString();

				sb.delete(0, sb.length());
				sb.append(Util.docRoot("secure/details/solver.jsp?id="));
				sb.append(id);
				redir = sb.toString();
				break;
			case "benchmark":
				sb.delete(0, sb.length());
				sb.append("/benchmarks/Pic");
				sb.append(id);
				fileName = sb.toString();

				sb.delete(0, sb.length());
				sb.append(Util.docRoot("secure/details/benchmark.jsp?id="));
				sb.append(id);
				redir = sb.toString();
				break;
			}

			sb.delete(0, sb.length());
			sb.append(R.getPicturePath());
			sb.append(File.separator);
			sb.append(fileName);
			sb.append("_org.jpg");
			String filenameupload = sb.toString();
			File archiveFile = new File(filenameupload);
			archiveFile.getParentFile().mkdirs();
			item.write(archiveFile);

			sb.delete(0, sb.length());
			sb.append(R.getPicturePath());
			sb.append(File.separator);
			sb.append(fileName);
			sb.append("_thn.jpg");
			String fileNameThumbnail = sb.toString();
			scale(filenameupload, 320, 320, fileNameThumbnail);
			return redir;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Validates a picture upload request to determine if it can be acted on or not.
	 *
	 * @param form A list of form items contained in the request
	 * @return True if the request is valid to act on, false otherwise
	 * @author Ruoyu Zhang
	 */
	private ValidatorStatusCode isRequestValid(HashMap<String, Object> form) {
		try {
			if (!form.containsKey(PICTURE_FILE)) {
				return new ValidatorStatusCode(false, "No picture was supplied");
			}
			if (!Validator.isValidPosInteger((String) form.get(ID))) {
				return new ValidatorStatusCode(false, "The supplied ID is not a valid integer");
			}
			String type = (String) form.get(TYPE);
			if (type == null || (!type.equals(R.SOLVER) && !type.equals("user") && !type.equals("benchmark"))) {
				return new ValidatorStatusCode(false, "The supplied image type is not valid");
			}

			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		return new ValidatorStatusCode(false, "Internal error handling picture upload request");
	}

	/**
	 * Scale the source file into a shrunk one with the width and height specified in the parameter.
	 *
	 * @param srcFile The original source file need to shrink
	 * @param destWidth The width of the shrunk picture.
	 * @param destHeight The height of the shrunk picture.
	 * @param destFile The file of the shrunk picture.
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	public static void scale(String srcFile, int destWidth, int destHeight, String destFile) throws IOException {

		BufferedImage src = ImageIO.read(new File(srcFile));
		BufferedImage dest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dest.createGraphics();
		AffineTransform at = AffineTransform
				.getScaleInstance((double) destWidth / src.getWidth(), (double) destHeight / src.getHeight());

		g.drawRenderedImage(src, at);
		ImageIO.write(dest, "JPG", new File(destFile));
	}
}
