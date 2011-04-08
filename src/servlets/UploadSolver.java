package servlets;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.tomcat.util.http.fileupload.*;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import util.*;
import constants.*;
import data.*;
import data.to.*;

// TODO: Change from UploadBench to UploadSolver
public class UploadSolver extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The unique date stamped file name format    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	for(String name : request.getParameterMap().keySet()){
    		System.out.println(name + ": " + request.getParameter(name));    		
    	}
		/*try {			
			FileItemFactory factory = new DiskFileItemFactory();				
			ServletFileUpload upload = new ServletFileUpload(factory);			// Create the helper objects to assist in getting the file and saving it
			
			List<FileItem> items = upload.parseRequest(request);				// The list of files to save					
							
			for(FileItem item : items) {										// For each file in the list			   
			   if (!item.isFormField()) {										// If it's not a field...
				   try {
					   handleBenchmark(item, response);									// Handle the benchmark file
				   } catch (Exception e) {
					   LogUtil.LogException(e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			LogUtil.LogException(e);											// Log any problems with the file upload process here
		}*/
	}
    
	/**
	 * This method is responsible for uploading a benchmark to
	 * the appropriate location and updating the database to reflect
	 * the new benchmark.
	 * @param item
	 * @param response The response which we can re-direct to based on a result
	 * @throws Exception 
	 */
	public void handleBenchmark(FileItem item, HttpServletResponse response) throws Exception {	
		File uniqueDir = new File(R.BENCHMARK_PATH, shortDate.format(new Date()));		// Create a unique path the zip file will be extracted to
		File zipFile = new File(uniqueDir,  item.getName());							// Create the zip file object-to-be		
		new File(zipFile.getParent()).mkdir();											// Create the directory the benchmark zip will be written to
						
		item.write(zipFile);															// Copy the benchmark zip to the server from the client
		ZipUtil.extractZip(zipFile.getAbsolutePath());									// Extract the downloaded benchmark zip file
		zipFile.delete();																// Delete the archive, we don't need it anymore!
				
		File bxmlFile = XmlUtil.dirToBXml(uniqueDir.getAbsolutePath());					// Convert the extracted ZIP to xml and save the path to the generated xml file				
		//BXMLHandler handler = XmlUtil.parseBXML(bxmlFile);								// Parse the bxml file and get back the handler to gather results		
		//new Database().addLevelsBenchmarks(handler.getLevels(), handler.getBenchmarks());// Add the resulting levels and benchmarks to the database
		
        response.sendRedirect("GetFile?type=bxml&parent=" + bxmlFile.getParentFile().getName()); // Send the response to the resulting XML file
	}	
	
	/**
	 * This method has the responsibility of uploading an archive file, extracting it, removing it
	 * and updating the database to reflect that the solver has been uploaded.
	 * @param item The file item of the solver to upload (given by apache)
	 * @throws Exception
	 */
	/*public void handleSolver(FileItem item) throws Exception{
		File destFile = new File(String.format("%s%s%s%s", R.SOLVER_PATH, shortDate.format(new Date()), File.separator, item.getName()));	// Generate a unique path for the solver		
		new File(destFile.getParent()).mkdir();																							// Create said unique path
						
		item.write(destFile);																		// Copy the file to the server from the client
		ZipUtil.extractZip(destFile.getAbsolutePath());														// Extract the downloaded file
		destFile.delete();																			// Delete the archive
		
		//TODO: Update database with uploaded solver
	}*/
}
