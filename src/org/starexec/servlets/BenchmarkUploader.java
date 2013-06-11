package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;


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
    private static final String FILE_URL="url";
    private static final String FILE_LOC="localOrURL";
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
				} else { 
					// create status object
					Integer spaceId = Integer.parseInt((String)form.get(SPACE_ID));
					Integer userId = SessionUtil.getUserId(request);					
					Integer statusId = Uploads.createUploadStatus(spaceId, userId);
					log.debug("upload status id is " + statusId);
					
					// Go ahead and process the request
					this.handleUploadRequest(form, userId, statusId,request,response);
					//go to upload status page
					response.sendRedirect(Util.docRoot("secure/details/uploadStatus.jsp?id=" + statusId)); 
				}
			} else {
				// Or else the request was invalid, send bad request error
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid benchmark upload request");
			}					
		} catch (Exception e) {
			log.error("Benchmark Uploader Servlet says " + e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error uploading the benchmarks.");
		}
	}

	private void handleUploadRequest(HashMap<String, Object> form, Integer uId, Integer sId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		//First extract all data from request
		final int userId = uId;
		
		final int spaceId = Integer.parseInt((String)form.get(SPACE_ID));
		final String uploadMethod = (String)form.get(UPLOAD_METHOD);
		final int typeId = Integer.parseInt((String)form.get(BENCHMARK_TYPE));
		final boolean downloadable = Boolean.parseBoolean((String)form.get(BENCH_DOWNLOADABLE));				
		final boolean hasDependencies = Boolean.parseBoolean((String)form.get(HAS_DEPENDENCIES));
		final boolean linked = Boolean.parseBoolean((String)form.get(LINKED));
		final int depRootSpaceId = Integer.parseInt((String)form.get(DEP_ROOT_SPACE_ID));
		final Permission perm = this.extractPermissions(form);
		final Integer statusId = sId;
		final String localOrUrl=(String)form.get(FILE_LOC);
		
		URL tempURL=null;
		String tempName=null;
		FileItem tempFileToUpload=null;
		if (localOrUrl.equals("URL")) {
			tempURL=new URL((String)form.get(FILE_URL));
			try {
				tempName=tempURL.toString().substring(tempURL.toString().lastIndexOf('/'));
			} catch (Exception e) {
				tempName=tempURL.toString().replace('/', '-');
			}
			
		} else {
			tempFileToUpload = ((FileItem)form.get(BENCHMARK_FILE));
		}
		
		final String name=tempName;
		final URL url=tempURL;
		final FileItem fileToUpload=tempFileToUpload;
		
		log.debug("upload status id is " + statusId);
		
		//It will delay the redirect until this method is finished which is why a new thread is used
		final ExecutorService threadPool = Executors.newCachedThreadPool();
		threadPool.execute(new Runnable() {
			@Override
			public void run(){
				try{
					log.info("Handling upload request for user " + userId + " in space " + spaceId);
					// Create a unique path the zip file will be extracted to
					File uniqueDir = new File(R.BENCHMARK_PATH, "" + userId);
					uniqueDir = new File(uniqueDir,  shortDate.format(new Date()));
					// Create the paths on the filesystem
					uniqueDir.mkdirs();
					
					File archiveFile=null;
					if (localOrUrl.equals("local")) {
						archiveFile = new File(uniqueDir,  fileToUpload.getName());
						fileToUpload.write(archiveFile);
					} else {
						archiveFile=new File(uniqueDir,name);
						FileUtils.copyURLToFile(url,archiveFile);
					}
					// Create the zip file object-to-be
					long fileSize=ArchiveUtil.getArchiveSize(archiveFile.getAbsolutePath());
					
					User currentUser=Users.get(userId);
					long allowedBytes=currentUser.getDiskQuota();
					long usedBytes=Users.getDiskUsage(userId);
					
					if (fileSize>allowedBytes-usedBytes) {
						archiveFile.delete();
						throw new Exception("File too large to fit in user's disk quota");
					}		

					// Copy the benchmark zip to the server from the client
																				
					log.info("upload complete - now extracting");
					Uploads.fileUploadComplete(statusId);
					// Extract the downloaded benchmark zip file
					if(!ArchiveUtil.extractArchive(archiveFile.getAbsolutePath())) {
						String message = "StarExec has failed to extract your uploaded file.";
						Uploads.setErrorMessage(statusId, message);
						log.error(message + " - status id = " + statusId + ", filepath = " + archiveFile.getAbsolutePath());
						return;
					}
					log.info("Extraction Complete");
					//update upload status
					Uploads.fileExtractComplete(statusId);

					log.debug("has dependencies = " + hasDependencies);
					log.debug("linked = " + linked);
					log.debug("depRootSpaceIds = " + depRootSpaceId);

					log.info("about to add benchmarks to space " + spaceId + "for user " + userId);
					if(uploadMethod.equals("convert")) {

						log.debug("convert");
						Space result = Benchmarks.extractSpacesAndBenchmarks(uniqueDir, typeId, userId, downloadable, perm, statusId);
						if (result == null) {
							String message = "StarExec has failed to extract the spaces and benchmarks from the files.";
							Uploads.setErrorMessage(statusId, message);
							log.error(message + " - status id = " + statusId);
							return;
						}
						// Method below requires the parent space, so fake it by setting the ID of the unique dir to the parent space ID
						result.setId(spaceId);
						//update Status
						Uploads.processingBegun(statusId);
						if (!hasDependencies){
							log.info("Now have the space java object.  Calling add with benchmarks and no dependencies for user " + userId + " to process and add to db.");
							Spaces.addWithBenchmarks(result, userId, statusId);
						}
						else
						{				
							Spaces.addWithBenchmarksAndDeps(result, userId, depRootSpaceId, linked, statusId);
						}
					} else if(uploadMethod.equals("dump")) {
						List<Benchmark> results = Benchmarks.extractBenchmarks(uniqueDir, typeId, userId, downloadable);
						for (Benchmark bench : results) {
							// Make sure that the benchmark has a unique name in the space.
							//TODO: verify that this is being done correctly. particularly whether benchmarks in THIS upload have unique names
							if(Spaces.notUniquePrimitiveName(bench.getName(), spaceId, 2)) {
								String message = "Benchmarks must have unique names within this space.  The following benchmark fails " + bench.getName();
								Uploads.setErrorMessage(statusId, message);
								log.error(message + " - status id = " + statusId);				
								return;
							}
						}

						if (!hasDependencies){	
							Benchmarks.add(results, spaceId, statusId);
						}
						else{
							Benchmarks.addWithDeps(results, spaceId, depRootSpaceId, linked, userId, statusId);
						}
					}
					log.info("Handle upload method complete in " + spaceId + "for user " + userId);				
				}
				catch (Exception e){
					log.error("upload Benchmarks says " + e);
					Uploads.setErrorMessage(statusId, e.getMessage());
				}
				finally{
					Uploads.everythingComplete(statusId);
					threadPool.shutdown();
				}
			}
		});
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
					!form.containsKey(BENCH_DOWNLOADABLE) ||
					!form.containsKey(FILE_LOC)) {
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
			if (((String)form.get(FILE_LOC)).equals("local")) {
				String fileName = ((FileItem)form.get(BENCHMARK_FILE)).getName();
				for(String ext : BenchmarkUploader.extensions) {
					if(fileName.endsWith(ext)) {
						return true;
					}
				}
			} else {
				String URL=(String)form.get(FILE_URL);
				for (String ext : BenchmarkUploader.extensions) {
					if (URL.endsWith(ext)) {
						return true;
					}
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
