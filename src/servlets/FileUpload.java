package servlets;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import util.LogUtil;
import util.Util;

import com.google.gson.Gson;

/**
 * Servlet implementation class FileUpload
 */
@WebServlet(description = "Services incoming file upload requests", urlPatterns = { "/FileUpload" })
public class FileUpload extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileUpload() {
        super();
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {			
		boolean success = true; 												// Was the upload a success?
		
		try {
			DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The dated name of the file
			String path = "/home/starexec/Solvers/";							// The directory in which to save the file(s)
			int i = 1;															// Unique number to differentiate the different files in case upload is too fast
			
			FileItemFactory factory = new DiskFileItemFactory();				
			ServletFileUpload upload = new ServletFileUpload(factory);			// Create the helper objects to assist in getting the file and saving it
			
			List<FileItem> items = upload.parseRequest(request);				// The list of files to save					
							
			for(FileItem item : items) {										// For each file in the list			   
			   if (!item.isFormField()) {										// If it's not a field...
				   try {						 
					   String fileName = String.format("%s%s-%d.%s", path, shortDate.format(new Date()), i++, Util.getFileExtension(item.getName()));
					   File savedFile = new File(fileName);						// Save it with our special name
					   item.write(savedFile);									// Save the file to disk!
				   } catch (Exception e) {
					   success = false;											// If there's a problem, sucess is false and log exception
					   LogUtil.LogException(e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			LogUtil.LogException(e);
			success = false;
		}	
		
		response.addHeader("Upload-Success", String.valueOf(success));			// Write the success to the header			
		response.sendRedirect("upload.jsp?s=" + success);						// Send redirect to the same page
	}
}
