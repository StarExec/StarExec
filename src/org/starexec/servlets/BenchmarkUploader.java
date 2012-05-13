package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;


/**
 * @deprecated This class is out of date and needs to be re-implemented
 */
@SuppressWarnings("serial")
public class BenchmarkUploader extends HttpServlet {
	private static final Logger log = Logger.getLogger(BenchmarkUploader.class);	
	
	// The unique date stamped file name format
    private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);    
    
    // Valid file types for uploads
    private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
    
    // Request attributes
    private static final String SPACE_ID = "space";
    private static final String UPLOAD_METHOD = "upMethod";
    private static final String BENCHMARK_FILE = "benchFile";
    private static final String BENCHMARK_TYPE = "benchType";
    private static final String BENCH_DOWNLOADABLE = "download";
	private static final String addSolver = "addSolver";
	private static final String addBench = "addBench";
	private static final String addUser = "addUser";
	private static final String addSpace = "addSpace";
	private static final String addJob = "addJob";
	private static final String removeSolver = "removeSolver";
	private static final String removeBench = "removeBench";
	private static final String removeUser = "removeUser";
	private static final String removeSpace = "removeSpace";
	private static final String removeJob = "removeJob";
	
	private static final String HAS_DEPENDENCIES = "dependency";
    private static final String LINKED = "linked";
    private static final String DEP_ROOT_SPACE_ID = "depRoot";
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		try {	
			// Extract data from the multipart request
			HashMap<String, Object> form = Util.parseMultipartRequest(request);
			
			// If the request is valid to act on...
			if(this.isRequestValid(form)) {
				// If the user has benchmark adding permissions
				Permission perm = SessionUtil.getPermission(request, Integer.parseInt((String)form.get("space")));
				String uploadMethod = (String)form.get(UPLOAD_METHOD);
				if(uploadMethod.equals("dump") && !perm.canAddBenchmark()) {
					// They don't have permissions, send forbidden error
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to upload benchmarks to this space");
				} else if (uploadMethod.equals("convert") && !(perm.canAddBenchmark() && perm.canAddSpace())) {
					// They don't have permissions, send forbidden error
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to upload benchmarks and subspaces to this space");
				} else 
					// Go ahead and process the request
					this.handleUploadRequest(form, request, response);{
				}
			} else {
				// Or else the request was invalid, send bad request error
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid benchmark upload request");
			}					
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
    	
	private void handleUploadRequest(HashMap<String, Object> form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		FileItem fileToUpload = ((FileItem)form.get(BENCHMARK_FILE));
		int userId = SessionUtil.getUserId(request);
		int spaceId = Integer.parseInt((String)form.get(SPACE_ID));
		
		// Create a unique path the zip file will be extracted to
		File uniqueDir = new File(R.BENCHMARK_PATH, "" + SessionUtil.getUserId(request));
		uniqueDir = new File(uniqueDir,  shortDate.format(new Date()));
		
		// Create the paths on the filesystem
		uniqueDir.mkdirs();
		
		// Create the zip file object-to-be
		File archiveFile = new File(uniqueDir,  fileToUpload.getName());										
						
		// Copy the benchmark zip to the server from the client
		fileToUpload.write(archiveFile);															
		
		// Extract the downloaded benchmark zip file
		if(!ArchiveUtil.extractArchive(archiveFile.getAbsolutePath())) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed to uncompress the given file");
			return;
		}
		
		// Get some required data from the form
		String uploadMethod = (String)form.get(UPLOAD_METHOD);
		int typeId = Integer.parseInt((String)form.get(BENCHMARK_TYPE));
		boolean downloadable = Boolean.parseBoolean((String)form.get(BENCH_DOWNLOADABLE));				
		boolean hasDependencies = Boolean.parseBoolean((String)form.get(HAS_DEPENDENCIES));
		boolean linked = Boolean.parseBoolean((String)form.get(LINKED));
		int depRootSpaceId = Integer.parseInt((String)form.get(DEP_ROOT_SPACE_ID));
		log.debug("has dependencies = " + hasDependencies);
		log.debug("linked = " + linked);
		log.debug("depRootSpaceIds = " + depRootSpaceId);
		log.info("about to upload benchmarks to space " + spaceId + "for user " + userId);
		if(uploadMethod.equals("convert")) {
			
			Space result = this.extractSpacesAndBenchmarks(uniqueDir, typeId, userId, downloadable, this.extractPermissions(form));
			
			// Method below requires the parent space, so fake it by setting the ID of the unique dir to the parent space ID
			result.setId(spaceId);
			if (!hasDependencies){
				Spaces.addWithBenchmarks(result, userId);
			}
			else
			{				
				Spaces.addWithBenchmarksAndDeps(result, userId, depRootSpaceId, linked);
			}
		} else if(uploadMethod.equals("dump")) {
			List<Benchmark> results = this.extractBenchmarks(uniqueDir, typeId, userId, downloadable);
			if (!hasDependencies){	
				Benchmarks.add(results, spaceId);
			}
			else{
				Benchmarks.addWithDeps(results, spaceId, depRootSpaceId, linked, userId);
			}
		}
						
        response.sendRedirect("/starexec/secure/explore/spaces.jsp"); 
	}		
	
	/**
	 * Recursively walks through the given directory and subdirectory to find all benchmark files within them
	 * @param directory The directory to extract benchmark files from
	 * @param typeId The bench type id to set for all the found benchmarks
	 * @param userId The user id of the owner of all the benchmarks found
	 * @param downloadable Whether or now to mark any found benchmarks as downloadable
	 * @return A flat list of benchmarks containing all the benchmarks found under the given directory and it's subdirectories and so on
	 */
	private List<Benchmark> extractBenchmarks(File directory, int typeId, int userId, boolean downloadable) {
		// Initialize the list we will return at the end...
		List<Benchmark> benchmarks = new LinkedList<Benchmark>();
		
		// For each file in the directory
		for(File f : directory.listFiles()) {
			if(f.isDirectory()) {
				// If it's a directory, recursively extract all benchmarks from it and add them to our list
				benchmarks.addAll(this.extractBenchmarks(f, typeId, userId, downloadable));
			} else {
				// Or else it's just a benchmark, create an object for it and add it to the list
				Processor t = new Processor();
				t.setId(typeId);
				
				Benchmark b = new Benchmark();
				b.setPath(f.getAbsolutePath());
				b.setName(f.getName());
				b.setType(t);
				b.setUserId(userId);
				b.setDownloadable(downloadable);
				benchmarks.add(b);
			}
		}
		
		return benchmarks;
	}
	
	/**
	 * Creates a space named after the directory and finds any benchmarks within the directory.
	 * Then the process recursively adds any subspaces found (other directories) until all directories
	 * under the original one are traversed.
	 * @param directory The directory to extract data from
	 * @param typeId The bench type id to set for all the found benchmarks
	 * @param userId The user is of the owner of all the benchmarks found
	 * @param downloadable Whether or now to mark any found benchmarks as downloadable
	 * @param perm The default permissions to set for this space
	 * @return A single space containing all subspaces and benchmarks based on the file structure of the given directory.
	 */
	private Space extractSpacesAndBenchmarks(File directory, int typeId, int userId, boolean downloadable, Permission perm) {
		// Create a space for the current directory and set it's name		
		log.info("Extracing Spaces and Benchmarks for " + userId);
		Space space = new Space();
		space.setName(directory.getName());
		space.setPermission(perm);
		
		// For each file within the directory...
		for(File f : directory.listFiles()) {
			// If it's a subdirectory
			if(f.isDirectory()) {
				// Recursively extract spaces/benchmarks from that directory
				space.getSubspaces().add(this.extractSpacesAndBenchmarks(f, typeId, userId, downloadable, perm));
			} else {
				// Or else we're just a file, so assume it's a benchmark and create an object for it
				Processor t = new Processor();
				t.setId(typeId);
				
				Benchmark b = new Benchmark();
				b.setPath(f.getAbsolutePath());
				b.setName(f.getName());
				b.setType(t);
				b.setUserId(userId);
				b.setDownloadable(downloadable);
				space.addBenchmark(b);
			}
		}
		
		return space;
	}
	
	/**
	 * Extracts the permissions object contained in the given form
	 * @param form The form to extract permissions from
	 * @return A permission object build from the fields contained in the form
	 */
	private Permission extractPermissions(HashMap<String, Object> form) {
		Permission p = new Permission();
		p.setAddBenchmark(form.containsKey(addBench));
		p.setAddSolver(form.containsKey(addSolver));
		p.setAddSpace(form.containsKey(addSpace));
		p.setAddUser(form.containsKey(addUser));
		p.setAddJob(form.containsKey(addJob));
		p.setRemoveBench(form.containsKey(removeBench));
		p.setRemoveSolver(form.containsKey(removeSolver));
		p.setRemoveSpace(form.containsKey(removeSpace));
		p.setRemoveUser(form.containsKey(removeUser));
		p.setRemoveJob(form.containsKey(removeJob));
		
		return p;
	}
	
	/**
	 * Validates a benchmark upload request to determine if it can be acted on or not.
	 * @param form A list of form items contained in the request
	 * @return True if the request is valid to act on, false otherwise
	 * @author ??? - modified by Ben
	 */
	private boolean isRequestValid(HashMap<String, Object> form) {
		try {			
			if(!form.containsKey(SPACE_ID) ||
			   !form.containsKey(BENCHMARK_FILE) ||
			   !form.containsKey(BENCHMARK_TYPE) ||
			   !form.containsKey(UPLOAD_METHOD)  ||
			   !form.containsKey(BENCH_DOWNLOADABLE)) {
				return false;
			}													
						
			// Try parsing to ensure we have valid numbers
			Integer.parseInt((String)form.get(BENCHMARK_TYPE));			
			Integer.parseInt((String)form.get(SPACE_ID));
			Boolean.parseBoolean((String)form.get(BENCH_DOWNLOADABLE));
			
			// Make sure we have a valid upload method
			String uploadMethod = ((String)form.get(UPLOAD_METHOD));
			if(!(uploadMethod.equals("convert") || uploadMethod.equals("dump"))) {
				return false;
			}
			
			// Last test, return true when we find a valid file extension			
			String fileName = ((FileItem)form.get(BENCHMARK_FILE)).getName();
			for(String ext : BenchmarkUploader.extensions) {
				if(fileName.endsWith(ext)) {
					return true;
				}
			}
			
			// If we got here we failed file extension validation
			return false;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		// Return false control flow is broken and ends up here
		return false;	
	}
}
