package org.starexec.servlets;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.starexec.constants.R;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;


/**
 * @deprecated This class is out of date and needs to be re-implemented
 */
@SuppressWarnings("serial")
public class UploadPicture extends HttpServlet {
	private static final Logger log = Logger.getLogger(UploadPicture.class);	 
    
    // Request attributes
    private static final String PICTURE_FILE = "f";
    private static final String TYPE = "type";
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
    	int userId = SessionUtil.getUserId(request);
    	try {	
			// Extract data from the multipart request
			HashMap<String, Object> form = Util.parseMultipartRequest(request);
			
			// If the request is valid
			if(this.isRequestValid(form)) {
				this.handleUploadRequest(userId, form, request, response);
			} else {
				// Or else the request was invalid, send bad request error
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid picture upload request");
			}					
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
    	
	private boolean handleUploadRequest(int userId, HashMap<String, Object> form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			FileItem item = (FileItem)form.get(UploadPicture.PICTURE_FILE);
			String fileName = "";
			String redir = "/starexec/secure/edit/account.jsp";
			
			if (request.getParameter("type").equals("user")) {
				fileName = String.format("/users/Pic%s", request.getParameter("Id").toString());
				redir = String.format("/starexec/secure/edit/account.jsp");
			} else if (request.getParameter("type").equals("solver")) {
				fileName = String.format("/solvers/Pic%s", request.getParameter("Id").toString());
				redir = String.format("/starexec/secure/details/solver.jsp?id=%s",request.getParameter("Id").toString());
			} else if (request.getParameter("type").equals("benchmark")) {
				fileName = String.format("/benchmarks/Pic%s", request.getParameter("Id").toString());
				redir = String.format("/starexec/secure/details/benchmark.jsp?id=%s",request.getParameter("Id").toString());
			}
			
	    	String filePath = R.PICTURE_PATH;
	    	String filenameupload = filePath + "/" + fileName + "_org.jpg";  	

			File archiveFile = new File(filenameupload);
			item.write(archiveFile);

			String fileNameThumbnail = filePath + "/" + fileName + "_thn.jpg";
			scale(filenameupload, 100, 120, fileNameThumbnail);
			
			response.sendRedirect(redir);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
		
	}
	
	/**
	 * Validates a benchmark upload request to determine if it can be acted on or not.
	 * @param form A list of form items contained in the request
	 * @return True if the request is valid to act on, false otherwise
	 */
	private boolean isRequestValid(HashMap<String, Object> form) {
		try {			
			if(!form.containsKey(PICTURE_FILE)) {
				return false;
			}			
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		// Return true if no problem
		return true;	
	}
	
	public static void scale(String srcFile, int destWidth, int destHeight, String destFile) throws IOException {
		
		BufferedImage src = ImageIO.read(new File(srcFile));
		BufferedImage dest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dest.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance(
				(double)destWidth/src.getWidth(),
				(double)destHeight/src.getHeight());
		
		g.drawRenderedImage(src, at);
		ImageIO.write(dest,"JPG",new File(destFile));
		}
}
