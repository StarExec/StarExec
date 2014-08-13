package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


@SuppressWarnings("serial")
public class BenchmarkUploader extends HttpServlet {
	private static final Logger log = Logger.getLogger(BenchmarkUploader.class);	

	// The unique date stamped file name format
	private static DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);    

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
			ValidatorStatusCode status=isRequestValid(form,request);
			// If the request is valid to act on...
			if(status.isSuccess()) {		
				
				
					// create status object
				Integer spaceId = Integer.parseInt((String)form.get(SPACE_ID));
				Integer userId = SessionUtil.getUserId(request);					
				Integer statusId = Uploads.createUploadStatus(spaceId, userId);
				log.debug("upload status id is " + statusId);
				
				// Go ahead and process the request
				this.handleUploadRequest(form, userId, statusId);
				//go to upload status page
				response.sendRedirect(Util.docRoot("secure/details/uploadStatus.jsp?id=" + statusId)); 
				
			} else {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				// Or else the request was invalid, send bad request error
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
			}					
		} catch (Exception e) {
			log.error("Benchmark Uploader Servlet says " + e.getMessage(), e);
			
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error uploading the benchmarks.");
		}
	}
	
	public static List<Integer> handleUploadRequestAfterExtraction(File archiveFile, int userId, int spaceId, int typeId,
			boolean downloadable, Permission perm, String uploadMethod, int statusId,
			boolean hasDependencies, boolean linked, Integer depRootSpaceId) {
		
		ArrayList<Integer> benchmarkIds=new ArrayList<Integer>();
		try {
			// Create a unique path the zip file will be extracted to
			File uniqueDir = new File(R.BENCHMARK_PATH, "" + userId);
			uniqueDir = new File(uniqueDir,  shortDate.format(new Date()));
			// Create the paths on the filesystem
			uniqueDir.mkdirs();
			
			
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
			if(!ArchiveUtil.extractArchive(archiveFile.getAbsolutePath(),uniqueDir.getAbsolutePath())) {
				String message = "StarExec has failed to extract your uploaded file.";
				Uploads.setErrorMessage(statusId, message);
				log.error(message + " - status id = " + statusId + ", filepath = " + archiveFile.getAbsolutePath());
				return null;
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

				//first we test to see if any names conflict
				ValidatorStatusCode status=doSpaceNamesConflict(uniqueDir,spaceId);
				if (!status.isSuccess()) {
					Uploads.setErrorMessage(statusId,status.getMessage());
					return null;
				}
				
				
				
				Space result = Benchmarks.extractSpacesAndBenchmarks(uniqueDir, typeId, userId, downloadable, perm, statusId);
				if (result == null) {
					String message = "StarExec has failed to extract the spaces and benchmarks from the files.";
					Uploads.setErrorMessage(statusId, message);
					log.error(message + " - status id = " + statusId);
					return null;
				}
				// Method below requires the parent space, so fake it by setting the ID of the unique dir to the parent space ID
				result.setId(spaceId);
				//update Status
				Uploads.processingBegun(statusId);
				if (!hasDependencies){
					log.info("Now have the space java object.  Calling add with benchmarks and no dependencies for user " + userId + " to process and add to db.");
					benchmarkIds.addAll(Spaces.addWithBenchmarks(result, userId, statusId));
				}
				else
				{				
					benchmarkIds.addAll(Spaces.addWithBenchmarksAndDeps(result, userId, depRootSpaceId, linked, statusId));
				}
			} else if(uploadMethod.equals("dump")) {
				List<Benchmark> results = Benchmarks.extractBenchmarks(uniqueDir, typeId, userId, downloadable);
				
				Uploads.processingBegun(statusId);
				if (!hasDependencies){	
					benchmarkIds.addAll(Benchmarks.add(results, spaceId, statusId));
				}
				else{
					benchmarkIds.addAll(Benchmarks.addWithDeps(results, spaceId, depRootSpaceId, linked, userId, statusId));
				}
			}
			log.info("Handle upload method complete in " + spaceId + "for user " + userId);	
			return benchmarkIds;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
		
	}
	
	private void handleUploadRequest(HashMap<String, Object> form, Integer uId, Integer sId) throws Exception {
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
		Util.threadPoolExecute(new Runnable() {
			@Override
			public void run(){
				try{
					
					// Create a unique path the zip file will be extracted to
					File uniqueDir = new File(R.BENCHMARK_PATH, "" + userId);
					uniqueDir = new File(uniqueDir,  shortDate.format(new Date()));
					// Create the paths on the filesystem
					uniqueDir.mkdirs();
					
					log.info("Handling upload request for user " + userId + " in space " + spaceId);
					
					File archiveFile=null;
					if (localOrUrl.equals("local")) {
						archiveFile = new File(uniqueDir,  FilenameUtils.getName(fileToUpload.getName()));
						fileToUpload.write(archiveFile);
					} else {
						archiveFile=new File(uniqueDir,name);
						FileUtils.copyURLToFile(url,archiveFile);
					}
					
					handleUploadRequestAfterExtraction(archiveFile, userId, spaceId, typeId,
							downloadable, perm, uploadMethod, statusId,
							hasDependencies, linked, depRootSpaceId);
					
				}
				catch (Exception e){
					log.error("upload Benchmarks says " + e);
					Uploads.setErrorMessage(statusId, e.getMessage());
				}
				finally{
					Uploads.everythingComplete(statusId);
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
	private ValidatorStatusCode isRequestValid(HashMap<String, Object> form, HttpServletRequest request) {
		try {			
																
			if (!Validator.isValidInteger((String)form.get(BENCHMARK_TYPE))) {
				return new ValidatorStatusCode(false, "The given benchmark processor ID is not a valid integer");
			}
			
			if (!Validator.isValidInteger((String)form.get(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The given space ID is not a valid integer");
			}
			if (!Validator.isValidBool((String)form.get(BENCH_DOWNLOADABLE))) {
				return new ValidatorStatusCode(false, "The 'bench downloadable' option needs to be a valid boolean");
			}
		
			// Make sure we have a valid upload method
			String uploadMethod = ((String)form.get(UPLOAD_METHOD));
			if(!(uploadMethod.equals("convert") || uploadMethod.equals("dump"))) {
				return new ValidatorStatusCode(false, "The upload method needs to be either 'convert' or 'dump'");
			}
			String fileName=null;
			// Last test, return true when we find a valid file extension
			if (((String)form.get(FILE_LOC)).equals("local")) {
				fileName = ((FileItem)form.get(BENCHMARK_FILE)).getName();
			} else {
				fileName=(String)form.get(FILE_URL);
			}
			boolean goodExtension=false;
			for(String ext : BenchmarkUploader.extensions) {
				if(fileName.endsWith(ext)) {
					goodExtension=true;
				}
			}
			
			if (!goodExtension) {
				return new ValidatorStatusCode(false, "Uploaded archives need to be either .zip, .tar, or .tgz");
			}
			
			
			Permission perm = SessionUtil.getPermission(request, Integer.parseInt((String)form.get("space")));
			
			if(uploadMethod.equals("dump") && !perm.canAddBenchmark()) {
				// They don't have permissions, send forbidden error
				return new ValidatorStatusCode(false, "You do not have permission to upload benchmarks to this space");
			} else if (uploadMethod.equals("convert") && !(perm.canAddBenchmark() && perm.canAddSpace())) {
				return new ValidatorStatusCode(false, "You do not have permission to upload benchmarks and subspaces to this space");
			}
			
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error uploading benchmarks");	
	}
	
	/**
	 * Checks to see if any of the spaces that will be created by the given upload directory conflict with existing names
	 * @param uniqueDir
	 * @return A ValidatorStatusCode set to True if there is NO conflict and set to false with a message if a conflict exists
	 */
	
	private static ValidatorStatusCode doSpaceNamesConflict(File uniqueDir, int parentSpaceId) {
		try {
			Space parent=Spaces.getDetails(parentSpaceId,Users.getAdmins().get(0).getId());
			HashSet<String> curNames=new HashSet<String>();
			for (Space s : parent.getSubspaces()) {
				curNames.add(s.getName());
			}
			for(File f : uniqueDir.listFiles()) {
				// If it's a sub-directory and as such a subspace
				if(f.isDirectory()) {
					String curName=f.getName();
					if (curNames.contains(curName)) {
						return new ValidatorStatusCode(false,"Creating spaces for your benchmarks would lead to having two subspaces with the name "+ curName); // found a conflict
					}
					curNames.add(curName);
				} 
			}
			
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return new ValidatorStatusCode(false, "There was an internal error uploading your benchmarks");
	}
	
}
