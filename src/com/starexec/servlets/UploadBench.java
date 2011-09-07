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

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.starexec.constants.R;
import com.starexec.data.Database;
import com.starexec.data.Databases;
import com.starexec.util.BXMLHandler;
import com.starexec.util.XmlUtil;
import com.starexec.util.ZipUtil;


public class UploadBench extends HttpServlet {
	private static final Logger log = Logger.getLogger(UploadBench.class);
	private static final long serialVersionUID = 1L;
	
	// The unique date stamped file name format
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		try {			
			// Create the helper objects to assist in getting the file and saving it
			FileItemFactory factory = new DiskFileItemFactory();				
			ServletFileUpload upload = new ServletFileUpload(factory);
			
			// The list of files to save
			List<FileItem> items = upload.parseRequest(request);					
							
			// For each file in the list
			for(FileItem item : items) {			   

				// If it's not a field...
			   if (!item.isFormField()) {
				   try {
					// Handle the benchmark file
					   handleBenchmark(item, request, response);
				   } catch (Exception e) {
					   log.error(e.getMessage(), e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			// Log any problems with the file upload process here
			log.error(e.getMessage(), e);
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
		int communityId = 1; // TODO: Get the real community id
		
		// Create a unique path the zip file will be extracted to
		File uniqueDir = new File(R.BENCHMARK_PATH, shortDate.format(new Date()));		

		// Create the zip file object-to-be
		File zipFile = new File(uniqueDir,  item.getName());							
		
		// Create the directory the benchmark zip will be written to
		new File(zipFile.getParent()).mkdir();											
						
		// Copy the benchmark zip to the server from the client
		item.write(zipFile);															
		
		// Extract the downloaded benchmark zip file
		ZipUtil.extractZip(zipFile.getAbsolutePath());									
		
		// Delete the archive, we don't need it anymore!
		zipFile.delete();																
				
		// Convert the extracted ZIP to xml and save the path to the generated xml file
		File bxmlFile = XmlUtil.dirToBXml(uniqueDir.getAbsolutePath());					
		
		// Parse the bxml file and get back the handler to gather results
		BXMLHandler handler = XmlUtil.parseBXML(bxmlFile, userId, communityId);							
		
		// Add the resulting levels and benchmarks to the database
		Databases.next().addLevelsBenchmarks(handler.getLevels(), handler.getBenchmarks());
		
		// Send the response to the resulting XML file
        response.sendRedirect("GetFile?type=bxml&parent=" + bxmlFile.getParentFile().getName()); 
	}	
}
