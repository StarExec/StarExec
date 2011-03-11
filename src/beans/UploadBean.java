package beans;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import data.Database;
import data.to.Benchmark;

import util.LogUtil;
import util.Util;

public class UploadBean {
	private static final long serialVersionUID = 1L;
	//private final String solverPath = "C:\\Users\\Tyler\\Desktop\\";			// The directory in which to save the solver file(s)
    //private final String benchPath = "C:\\Users\\Tyler\\Desktop\\Benchmarks\\";	// The directory in which to save the benchmark file(s)
    private final String solverPath = "/home/starexec/Solvers/";				// The directory in which to save the solver file(s)
    private final String benchPath = "/home/starexec/Solvers/Benchmarks/";			// The directory in which to save the benchmark file(s)
    private DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");	// The unique date stamped file name format
    private List<Benchmark> benchmarks;
    private Database database;
    private boolean isSolver = false;
    private boolean isBenchmark = false;
    
    public UploadBean(){
    	
    }
    
	public void doUpload(HttpServletRequest request){
		database = new Database(request.getServletContext());					// Setup the database
		boolean success = true; 												// Was the upload a success?
		
		try {			
			FileItemFactory factory = new DiskFileItemFactory();				
			ServletFileUpload upload = new ServletFileUpload(factory);			// Create the helper objects to assist in getting the file and saving it
			
			List<FileItem> items = upload.parseRequest(request);				// The list of files to save					
							
			for(FileItem item : items) {										// For each file in the list			   
			   if (!item.isFormField()) {										// If it's not a field...
				   try {
					   if(isSolver)												// If the file is a solver, handle it
						   handleSolver(item);
					   else if(isBenchmark)										// If the file is a benchmark, handle it
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
	private void extractZip(String fileName, boolean isBenchmark) throws FileNotFoundException, IOException{
		if(isBenchmark)																				// Is we unzipping a benchmark zip?
			benchmarks = new ArrayList<Benchmark>(25);												// Create a new benchmark list
		
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
		        
		        if(isBenchmark) {																	// If we're dealing with benchmarks
		        	Benchmark b = new Benchmark();													// Create a new benchmark
		        	b.setPath(directory + entry.getName());
		        	b.setUser(-1);																	// TODO add the real user's ID
		        	
		        	b.setId(database.addBenchmark(b));												// Add it to the database
		        	benchmarks.add(b);																// Add it to our list of added benchmarks
		        }
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
		File destFile = new File(String.format("%s%s%s%s", solverPath, shortDate.format(new Date()), File.separator, item.getName()));	// Generate a unique path for the solver		
		new File(destFile.getParent()).mkdir();																							// Create said unique path
						
		item.write(destFile);																		// Copy the file to the server from the client
		extractZip(destFile.getAbsolutePath(), false);														// Extract the downloaded file
		destFile.delete();																			// Delete the archive
		
		//TODO: Update database with uploaded solver
	}
	
	/**
	 * This method is responsible for uploading a benchmark to
	 * the appropriate location and updating the database to reflect
	 * the new benchmark.
	 * @param item
	 * @throws Exception 
	 */
	public void handleBenchmark(FileItem item) throws Exception {
		File destFile = new File(String.format("%s%s%s%s", benchPath, shortDate.format(new Date()), File.separator, item.getName()));	// Generate a unique path for the solver		
		new File(destFile.getParent()).mkdir();																							// Create said unique path
						
		item.write(destFile);																		// Copy the file to the server from the client
		extractZip(destFile.getAbsolutePath(), true);												// Extract the downloaded file
		destFile.delete();																			// Delete the archive
	}

	public List<Benchmark> getUploadedBenchmarks(){
		return benchmarks;
	}

	public boolean getIsSolver() {
		return isSolver;
	}

	public void setIsSolver(boolean isSolver) {
		this.isSolver = isSolver;
	}

	public boolean getIsBenchmark() {
		return isBenchmark;
	}

	public void setIsBenchmark(boolean isBenchmark) {
		this.isBenchmark = isBenchmark;
	}	
}
