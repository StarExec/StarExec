package servlets;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    //private final String rootPath = "C:\\Users\\Tyler\\Desktop\\";			// The directory in which to save the file(s)
    private final String rootPath = "/home/starexec/Solvers/";					// The directory in which to save the file(s)
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The unique date stamped file name format
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
			FileItemFactory factory = new DiskFileItemFactory();				
			ServletFileUpload upload = new ServletFileUpload(factory);			// Create the helper objects to assist in getting the file and saving it
			
			List<FileItem> items = upload.parseRequest(request);				// The list of files to save					
							
			for(FileItem item : items) {										// For each file in the list			   
			   if (!item.isFormField()) {										// If it's not a field...
				   try {
					   if(isSolver(item.getName()))								// If the file is a solver, handle it
						   handleSolver(item);
					   else if(isBenchmark(item.getName()))						// If the file is a benchmark, handle it
						   handleBenchmark(item);
					   else
						   throw new Exception("Unsupported file type uploaded.");	// If it's neither, throw an exception
				   } catch (Exception e) {
					   success = false;											// If there's a problem, success is false and log exception
					   LogUtil.LogException(e);
				   }			   
			   }
			}
		} catch (FileUploadException e) {
			LogUtil.LogException(e);											// Log any problems with the file upload process here
			success = false;
		}	
						
		response.sendRedirect("upload.jsp?s=" + success);						// Send redirect to the same page with a get parameter to notify the user of success or failure
	}
	

	
	/**
	 * This method simple takes in the name of the zip file, and extracts its contents into
	 * the same directory that the zip file resides in. It will automatically create any sort
	 * of file structure necessary to unzip to.
	 * @param fileName The absolute name of the zip file to extract
	 * @throws FileNotFoundException If there was a problem locating the zip file
	 * @throws IOException If there was a problem extracting from the zip file	
	 */
	@SuppressWarnings("unchecked")
	private void extractZip(String fileName) throws FileNotFoundException, IOException{		
		String directory = new File(fileName).getParentFile().getCanonicalPath() + File.separator;	// Get the directory the zip file is in (this will be the directory to extract to)
	    ZipFile zipFile = new ZipFile(fileName);													// Create a zip file object
	    Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();	    			// Get all of the entries in the zip file
      
	    while(entries.hasMoreElements()) {															// For each file in the zip file...
	    	ZipEntry entry = (ZipEntry)entries.nextElement();

	        if(!entry.isDirectory()) {																// If it's actually a file and not a directory
	        	File parentDir = new File(directory + entry.getName()).getParentFile();				// Get the file's parent directory
	        	
	        	if(!parentDir.exists())																// If the directory structure doesn't exist, create it
	        		parentDir.mkdirs();
	        	
	        	FileOutputStream fileOut = new FileOutputStream(directory + entry.getName());		// Open a new file for writing
		        extractFile(zipFile.getInputStream(entry), new BufferedOutputStream(fileOut));		// Extract the zipped file to the newly opened file (essentially copy it)
	        }	       
	      }

	      zipFile.close();																			// Close the zip file when finished
	}
	
	/**
	 * Copies a inputstream to an output stream. Helper method for zip file extraction
	 * @param in The input stream to copy from
	 * @param out The output stream to copy to
	 * @throws IOException
	 */
	private void extractFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int len;

	    while((len = in.read(buffer)) >= 0)
	      out.write(buffer, 0, len);

	    in.close();
	    out.close();
    }
	
	/**
	 * This method has the responsibility of uploading an archive file, extracting it, removing it
	 * and updating the database to reflect that the solver has been uploaded.
	 * @param item The file item of the solver to upload (given by apache)
	 * @throws Exception
	 */
	public void handleSolver(FileItem item) throws Exception{
		File destFile = new File(String.format("%s%s%s%s", rootPath, shortDate.format(new Date()), File.separator, item.getName()));	// Generate a unique path for the solver		
		new File(destFile.getParent()).mkdir();																							// Create said unique path
						
		item.write(destFile);																		// Copy the file to the server from the client
		extractZip(destFile.getAbsolutePath());														// Extract the downloaded file
		destFile.delete();																			// Delete the archive
		
		//TODO: Update database with uploaded solver
	}
	
	/**
	 * This method is responsible for uploading a benchmark to
	 * the appropriate location and updating the database to reflect
	 * the new benchmark.
	 * @param item
	 */
	public void handleBenchmark(FileItem item) {
		// TODO: Handle benchmarks
	}
	
	/**
	 * Checks to see if the uploaded file is a proper solver
	 * @param file The file to check
	 * @return True if it is an accepted benchmark file format
	 */
	private boolean isBenchmark(String file){
		String extension = Util.getFileExtension(file);
		
		// TODO: Check against acceptable file extensions for benchmarks
		return false;
	}
	
	/**
	 * Checks to see if the uploaded file is a proper solver
	 * @param file The file to check
	 * @return True if it is an accepted solver file format
	 */
	private boolean isSolver(String file){
		String extension = Util.getFileExtension(file);
		
		return (extension.equals("zip") || extension.equals("gz"));			
	}
}
