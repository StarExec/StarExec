package com.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.starexec.constants.R;
import com.starexec.data.Database;
import com.starexec.util.BXMLHandler;
import com.starexec.util.LogUtil;
import com.starexec.util.XmlUtil;
import com.starexec.util.ZipUtil;


public class UploadBench extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The unique date stamped file name format    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		try {			
			FileItemFactory factory = new DiskFileItemFactory();				
			ServletFileUpload upload = new ServletFileUpload(factory);			// Create the helper objects to assist in getting the file and saving it
			
			List<FileItem> items = upload.parseRequest(request);				// The list of files to save					
							
			for(FileItem item : items) {										// For each file in the list			   
			   if (!item.isFormField()) {										// If it's not a field...
				   try {
					   handleBenchmark(item, request, response);									// Handle the benchmark file
				   } catch (Exception e) {
					   LogUtil.LogException(e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			LogUtil.LogException(e);											// Log any problems with the file upload process here
		}
	}
    
	/**
	 * This method is responsible for uploading a benchmark to
	 * the appropriate location and updating the database to reflect
	 * the new benchmark.
	 * @throws Exception 
	 */
	public void handleBenchmark(FileItem item, HttpServletRequest request , HttpServletResponse response) throws Exception {
		//int userId = Integer.parseInt(request.getSession().getAttribute("userid").toString()); // TODO: When sessions are set up, extract the appropriate user id
		int userId = 1;	// For current testing, just use the test userid
		File uniqueDir = new File(R.BENCHMARK_PATH, shortDate.format(new Date()));		// Create a unique path the zip file will be extracted to
		File zipFile = new File(uniqueDir,  item.getName());							// Create the zip file object-to-be		
		new File(zipFile.getParent()).mkdir();											// Create the directory the benchmark zip will be written to
						
		item.write(zipFile);															// Copy the benchmark zip to the server from the client
		ZipUtil.extractZip(zipFile.getAbsolutePath());									// Extract the downloaded benchmark zip file
		zipFile.delete();																// Delete the archive, we don't need it anymore!
				
		File bxmlFile = XmlUtil.dirToBXml(uniqueDir.getAbsolutePath());					// Convert the extracted ZIP to xml and save the path to the generated xml file				
		BXMLHandler handler = XmlUtil.parseBXML(bxmlFile, userId);						// Parse the bxml file and get back the handler to gather results		
		new Database().addLevelsBenchmarks(handler.getLevels(), handler.getBenchmarks());// Add the resulting levels and benchmarks to the database
		
        response.sendRedirect("GetFile?type=bxml&parent=" + bxmlFile.getParentFile().getName()); // Send the response to the resulting XML file
	}	
}
