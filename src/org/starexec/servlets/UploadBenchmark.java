package org.starexec.servlets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.security.UploadSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.test.TestUtil;
import org.starexec.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@MultipartConfig
public class UploadBenchmark extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(UploadBenchmark.class);

	// The unique date stamped file name format
	private static final DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);

	// Request attributes
	private static final String SPACE_ID = R.SPACE;
	private static final String UPLOAD_METHOD = "upMethod";
	private static final String BENCHMARK_FILE = "benchFile";
	private static final String BENCHMARK_TYPE = "benchType";
	private static final String BENCH_DOWNLOADABLE = "download";
	private static final String FILE_URL = "url";
	private static final String FILE_GIT = "git";
	private static final String FILE_LOC = "localOrURLOrGit";
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

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (UploadSecurity.uploadsFrozen()) {
				response.sendError(
					HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"Uploading benchmarks is currently disabled"
				);
				return;
			}

			// Extract data from the multipart request
			HashMap<String, Object> form = Util.parseMultipartRequest(request);
			ValidatorStatusCode status = isRequestValid(form, request);
			// If the request is valid to act on...
			if (status.isSuccess()) {
				// create status object
				Integer spaceId = Integer.parseInt((String) form.get(SPACE_ID));
				Integer userId = SessionUtil.getUserId(request);
				Integer statusId = Uploads.createBenchmarkUploadStatus(spaceId, userId);
				log.debug("upload status id is " + statusId);

				// Go ahead and process the request
				this.handleUploadRequest(form, userId, statusId);
				//go to upload status page
				response.addCookie(new Cookie("New_ID", String.valueOf(statusId)));
				response.sendRedirect(Util.docRoot("secure/details/uploadStatus.jsp?id=" + statusId));
			} else {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				// Or else the request was invalid, send bad request error
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
			}
		} catch (Exception e) {
			log.warn("doPost", e);
			response.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error uploading the benchmarks.");
		}
	}

	/**
	 * Creates a directory that benchmarks can be placed in, which is empty on initialization.
	 *
	 * @param userId The ID of the user who will own the new benchmarks
	 * @param name An optional name to be used in the directory structure of userId/date/name. If null, benchmarks will
	 * be directly under the /date/ directory
	 * @return File object representing the new directory
	 */
	public static File getDirectoryForBenchmarkUpload(int userId, String name) throws FileNotFoundException {
		final String methodName = "getDirectoryForBenchmarkUpload";
		File uniqueDir = new File(R.getBenchmarkPath(), "" + userId);
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		if (name != null) {
			uniqueDir = new File(uniqueDir, name);
		}
		log.debug(methodName, "Creating directory  " + uniqueDir + " as " + System.getProperty("user.name"));
		boolean dirMade = uniqueDir.mkdirs();
		if (!dirMade) {
			log.warn(methodName, "Directory was not made." + uniqueDir.getAbsolutePath());
			log.warn(methodName, "Did file already exist: " + uniqueDir.exists());
			log.warn(methodName, "User was: " + System.getProperty("user.name"));
			log.warn(methodName, "canWrite for file: " + uniqueDir.canWrite());
			if (!uniqueDir.exists()) {
				throw new FileNotFoundException("The unique directory could not be made for some reason.");
			}
		}
		return uniqueDir;
	}

	/**
	 * Adds a single new benchmark to the database with contents given by a string
	 *
	 * @param benchText The contents of the new benchmark
	 * @param name The name to give the new benchmark
	 * @param userId The ID of the user creating this benchmark
	 * @param typeId The ID of the benchmark processor to use on this benchmark
	 * @param downloadable Whether the benchmark should be set as being "downloadable"
	 * @return The ID of the newly created benchmark
	 */
	public static Integer addBenchmarkFromText(
			String benchText, String name, int userId, int typeId, boolean downloadable
	) {
		try {
			log.debug("trying to add benchmark with text = " + benchText + " and name = " + name);
			File uniqueDir = getDirectoryForBenchmarkUpload(userId, null);
			FileUtils.writeStringToFile(new File(uniqueDir, name), benchText);
			List<Benchmark> bench =
					Benchmarks.extractSpacesAndBenchmarks(uniqueDir, typeId, userId, downloadable, null, null)
					          .getBenchmarksRecursively();
			//add the benchmark to the database, but don't put it in any spaces
			return Benchmarks.processAndAdd(bench, null, 1, false, null).get(0);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Adds a single new benchmark to the database with contents given by a File
	 *
	 * @param benchFile The file containing the new benchmark, name of file will be name of benchmark
	 * @param userId The ID of the user creating this benchmark
	 * @param typeId The ID of the benchmark processor to use on this benchmark
	 * @param downloadable Whether the benchmark should be set as being "downloadable"
	 * @return The ID of the newly created benchmark
	 */
	public static Integer addBenchmarkFromFile(File benchFile, int userId, int typeId, boolean downloadable) {
		try {
			File uniqueDir = getDirectoryForBenchmarkUpload(userId, null);
			FileUtils.copyFileToDirectory(benchFile, uniqueDir);
			String[] filesInUniqueDir = uniqueDir.list();
			log.debug("Files in uniqueDir: ");
			for (String s : filesInUniqueDir) {
				log.debug("    " + s);
			}

			List<Benchmark> bench =
					Benchmarks.extractSpacesAndBenchmarks(uniqueDir, typeId, userId, downloadable, null, null)
					          .getBenchmarksRecursively();
			//add the benchmark to the database, but don't put it in any spaces
			return Benchmarks.processAndAdd(bench, null, 1, false, null).get(0);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Adds a set of benchmarks to the database by extracting the given archive and finding the benchmarks inside of it
	 *
	 * @param archiveFile The archive to look into
	 * @param userId The user who will own all of the new benchmarks
	 * @param spaceId The ID of the root space for this upload (what space did the user click "upload benchmarks" in?)
	 * @param typeId The ID of the benchmark processor that will be applied to the new benchmarks,
	 * @param downloadable Whether each of the benchmarks should be flagged as downloadable
	 * @param perm Permissions object representing the permissions for any new spaces created as a result of this
	 * upload
	 * @param uploadMethod "convert" or "dump", depending on whether to make a hierarchy or just put all benchmarks in
	 * the root space
	 * @param statusId The ID of the UploadStatus object for tracking this upload
	 * @param hasDependencies Whether the benchmarks have dependencies
	 * @param linked
	 * @param depRootSpaceId The root space for dependencies for these benchmarks
         * @return list of ids of benchmarks added, if uploadMethod is "dump"; otherwise empty list
	 * @throws Exception
	 */
	public static List<Integer> addBenchmarksFromArchive(
			File archiveFile, int userId, int spaceId, int typeId, boolean downloadable, Permission perm,
			String uploadMethod, int statusId, boolean hasDependencies, boolean linked, Integer depRootSpaceId
	) throws IOException, StarExecException {

		ArrayList<Integer> benchmarkIds = new ArrayList<>();
		// Create a unique path the zip file will be extracted to
		final File uniqueDir = getDirectoryForBenchmarkUpload(userId, null);

		// Create the zip file object-to-be
		long fileSize = ArchiveUtil.getArchiveSize(archiveFile.getAbsolutePath());

		User currentUser = Users.get(userId);
		long allowedBytes = currentUser.getDiskQuota();
		long usedBytes = currentUser.getDiskUsage();

		if (fileSize > allowedBytes - usedBytes) {
			archiveFile.delete();
			Uploads.setBenchmarkErrorMessage(statusId,
			                                 "The benchmark upload is too large to fit in your disk quota. The " +
					                                 "uncompressed" +
					                                 " size of the archive is approximately " + fileSize +
					                                 " bytes, but you have only " + (allowedBytes - usedBytes) +
					                                 " bytes remaining.");
			throw new StarExecException("File too large to fit in user's disk quota");
		}

		// Copy the benchmark zip to the server from the client

		List<Integer> ids = new ArrayList<Integer>();
		log.info("upload complete - now extracting");
		Uploads.benchmarkFileUploadComplete(statusId);
		// Extract the downloaded benchmark zip file
		if (!ArchiveUtil.extractArchive(archiveFile.getAbsolutePath(), uniqueDir.getAbsolutePath())) {
			String message = "StarExec has failed to extract your uploaded file.";
			Uploads.setBenchmarkErrorMessage(statusId, message);
			log.error(message + " - status id = " + statusId + ", filepath = " + archiveFile.getAbsolutePath());
			return ids;
		}
		log.info("Extraction Complete");
		//update upload status
		Uploads.fileExtractComplete(statusId);


		log.debug("has dependencies = " + hasDependencies);
		log.debug("linked = " + linked);
		log.debug("depRootSpaceIds = " + depRootSpaceId);

		log.info("about to add benchmarks to space " + spaceId + "for user " + userId);
		Space result = Benchmarks.extractSpacesAndBenchmarks(uniqueDir, typeId, userId, downloadable, perm, statusId);
		if (result == null) {
			String message = "StarExec has failed to extract the spaces and benchmarks from the files.";
			Uploads.setBenchmarkErrorMessage(statusId, message);
			log.error(message + " - status id = " + statusId);
			return ids;
		}
		result.setId(spaceId);

		//update Status
		Uploads.processingBegun(statusId);

		if (uploadMethod.equals("convert")) {
			log.debug("convert");

			//first we test to see if any names conflict
			ValidatorStatusCode status = doSpaceNamesConflict(uniqueDir, spaceId);
			if (!status.isSuccess()) {
				Uploads.setBenchmarkErrorMessage(statusId, status.getMessage());
				return ids;
			}

			Spaces.addWithBenchmarks(result, userId, depRootSpaceId, linked, statusId,
			                                             hasDependencies);
		} else if (uploadMethod.equals("dump")) {
			List<Benchmark> benchmarks = result.getBenchmarksRecursively();

			ids.addAll(Benchmarks.processAndAdd(benchmarks, spaceId, depRootSpaceId, linked, statusId,
			                                             hasDependencies
							 ));
		}
		log.info("Handle upload method complete in " + spaceId + "for user " + userId);
                return ids;
	}


		/**
		 * Adds a set of benchmarks to the database by extracting the given directory and finding the benchmarks inside of it
		 *
		 * @param gitSpace The directory of the git clone
		 * @param userId The user who will own all of the new benchmarks
		 * @param spaceId The ID of the root space for this upload (what space did the user click "upload benchmarks" in?)
		 * @param typeId The ID of the benchmark processor that will be applied to the new benchmarks,
		 * @param downloadable Whether each of the benchmarks should be flagged as downloadable
		 * @param perm Permissions object representing the permissions for any new spaces created as a result of this
		 * upload
		 * @param uploadMethod "convert" or "dump", depending on whether to make a hierarchy or just put all benchmarks in
		 * the root space
		 * @param statusId The ID of the UploadStatus object for tracking this upload
		 * @param hasDependencies Whether the benchmarks have dependencies
		 * @param linked
		 * @param depRootSpaceId The root space for dependencies for these benchmarks
		 * @throws Exception
		 */
		public static void addBenchmarksGit(File gitSpace, int userId, int spaceId, int typeId, boolean downloadable, Permission perm,
		String uploadMethod, int statusId, boolean hasDependencies, boolean linked, Integer depRootSpaceId)
		throws IOException, StarExecException{
			ArrayList<Integer> benchmarkIds = new ArrayList<>();

			//get the approximate files size, larger than actual beacause the .git directory is present still
			long fileSize = FileUtils.sizeOf(gitSpace);
			log.debug("size of file: " + fileSize);
			User currentUser = Users.get(userId);
			long allowedBytes = currentUser.getDiskQuota();
			long usedBytes = currentUser.getDiskUsage();

			if (fileSize > allowedBytes - usedBytes) {
				FileUtils.deleteDirectory(gitSpace);
				Uploads.setBenchmarkErrorMessage(statusId,
				                                 "The benchmark upload is too large to fit in your disk quota. The " +
						                                 "uncompressed" +
						                                 " size of the archive is approximately " + fileSize +
						                                 " bytes, but you have only " + (allowedBytes - usedBytes) +
						                                 " bytes remaining.");
				throw new StarExecException("File too large to fit in user's disk quota");
			}

			log.info("upload complete - now extracting");
			Uploads.benchmarkFileUploadComplete(statusId);
			log.info("Extraction Complete");
			//update upload status
			//This was apart of the orignial archive process so I left the message update
			Uploads.fileExtractComplete(statusId);


			log.debug("has dependencies = " + hasDependencies);
			log.debug("linked = " + linked);
			log.debug("depRootSpaceIds = " + depRootSpaceId);

			log.info("about to add benchmarks to space " + spaceId + " for user " + userId);
			Space result = Benchmarks.extractSpacesAndBenchmarks(gitSpace, typeId, userId, downloadable, perm, statusId);
			if (result == null) {
				String message = "StarExec has failed to extract the spaces and benchmarks from the files.";
				Uploads.setBenchmarkErrorMessage(statusId, message);
				log.error(message + " - status id = " + statusId);
				return;
			}
			result.setId(spaceId);

			//update Status
			Uploads.processingBegun(statusId);

			if (uploadMethod.equals("convert")) {
				log.debug("convert");

				//first we test to see if any names conflict
				ValidatorStatusCode status = doSpaceNamesConflict(gitSpace, spaceId);
				if (!status.isSuccess()) {
					Uploads.setBenchmarkErrorMessage(statusId, status.getMessage());
					return;
				}

				Spaces.addWithBenchmarks(result, userId, depRootSpaceId, linked, statusId,
		                                         hasDependencies);
			} else if (uploadMethod.equals("dump")) {
				List<Benchmark> benchmarks = result.getBenchmarksRecursively();

				Benchmarks.processAndAdd(benchmarks, spaceId, depRootSpaceId, linked, statusId,
				                         hasDependencies
				);
			}
			log.info("Handle upload method complete in " + spaceId + "for user " + userId);
		}


	private void handleUploadRequest(HashMap<String, Object> form, Integer uId, Integer sId) throws Exception {
		//First extract all data from request
		final int userId = uId;

		final int spaceId = Integer.parseInt((String) form.get(SPACE_ID));
		final String uploadMethod = (String) form.get(UPLOAD_METHOD);
		final int typeId = Integer.parseInt((String) form.get(BENCHMARK_TYPE));
		final boolean downloadable = Boolean.parseBoolean((String) form.get(BENCH_DOWNLOADABLE));
		final boolean hasDependencies = Boolean.parseBoolean((String) form.get(HAS_DEPENDENCIES));
		final boolean linked = Boolean.parseBoolean((String) form.get(LINKED));
		final int depRootSpaceId = Integer.parseInt((String) form.get(DEP_ROOT_SPACE_ID));
		final Permission perm = this.extractPermissions(form);
		final Integer statusId = sId;
		final String localOrUrlOrGit = (String) form.get(FILE_LOC);

		URL tempURL = null;
		String tempName = null;
		PartWrapper tempFileToUpload = null;
		if (localOrUrlOrGit.equals("URL")) {
			tempURL = new URL((String) form.get(FILE_URL));
			try {
				tempName = tempURL.toString().substring(tempURL.toString().lastIndexOf('/'));
			} catch (Exception e) {
				tempName = tempURL.toString().replace('/', '-');
			}
		} else {
			tempFileToUpload = ((PartWrapper) form.get(BENCHMARK_FILE));
		}
		String tempGitUrl = null;
		//for the git url
		if (localOrUrlOrGit.equals("Git")) {
			tempURL = new URL((String) form.get(FILE_GIT));
			tempGitUrl = ((String) form.get(FILE_GIT)).trim();
			log.debug("URL is : " + ((String) form.get(FILE_GIT)));
			try {
				tempName = tempURL.toString().substring(tempURL.toString().lastIndexOf('/'));
			} catch (Exception e) {
				tempName = tempURL.toString().replace('/', '-');
			}
		} else {
			tempFileToUpload = ((PartWrapper) form.get(BENCHMARK_FILE));
		}


		final String name = tempName;
		final URL url = tempURL;
		final PartWrapper fileToUpload = tempFileToUpload;
		final String gitUrl = tempGitUrl;

		log.debug("upload status id is " + statusId);

		//It will delay the redirect until this method is finished which is why a new thread is used


		// Create a unique path the zip file will be extracted to
		File uniqueDir = new File(R.getBenchmarkPath(), "" + userId);
		Date d = new Date();

		uniqueDir = new File(uniqueDir, d.getYear() + "");
		uniqueDir = new File(uniqueDir, d.getMonth() + "");
		uniqueDir = new File(uniqueDir, d.getDay() + "");
		uniqueDir = new File(uniqueDir, d.getHours() + "");
		uniqueDir = new File(uniqueDir, d.getMinutes() + "");
		// the random string is to ensure that this directory is unique. It would not be otherwise if the
		// user uploads two benchmark directories in the same minute, which can easily happen using StarexecCommand
		uniqueDir = new File(uniqueDir, TestUtil.getRandomAlphaString(20));
		// Create the paths on the filesystem
                if (uniqueDir.mkdirs()) {
                    log.info("Directory has been created");
                }
                else {
                    log.info("Directory has NOT been created");
                }

		log.info("Handling upload request for user " + userId + " in space " + spaceId);

		File archive = null;
		String gitSpaceString = null;
		if (localOrUrlOrGit.equals("local")) {
			archive = new File(uniqueDir, FilenameUtils.getName(fileToUpload.getName()));
			fileToUpload.write(archive);
		}

		//////////////////////// URL process
		else if (localOrUrlOrGit.equals("URL")){
			archive = new File(uniqueDir, name);
			if (!Util.copyFileFromURLUsingProxy(url, archive)) {
				throw new Exception("Unable to copy file from URL");
			}
		}
		else{
			gitSpaceString = uniqueDir.getAbsolutePath();
			String[] gitClonecmd = new String[4];
			String[] gitSubmodulecmd = new String[5];

			gitClonecmd[0] = "git";
			gitClonecmd[1] = "clone";
			gitClonecmd[2] = gitUrl;
			gitClonecmd[3] = gitSpaceString;
			log.debug("gitclonecmd: " + gitClonecmd[0] + " " + gitClonecmd[1] + " " + gitClonecmd[2]+" " +gitClonecmd[3]);
			Util.executeCommand(gitClonecmd);
			//git submodule update --init --recursive

			String[] filesInUniqueDir = uniqueDir.list();
			log.debug("Files in uniqueDir: ");
			for (String s : filesInUniqueDir) {
				log.debug("    " + s);
			}

			gitSubmodulecmd[0] = "git";
			gitSubmodulecmd[1] = "submodule";
			gitSubmodulecmd[2] = "update";
			gitSubmodulecmd[3] = "--init";
			gitSubmodulecmd[4] = "--recursive";

			log.debug("gitSubmodulecmd: " + gitSubmodulecmd[0] + " " + gitSubmodulecmd[1] + " " + gitSubmodulecmd[2]+" "
								+gitSubmodulecmd[3]+ " " + gitSubmodulecmd[4]);
			Util.executeCommand(gitSubmodulecmd,null, uniqueDir);
		}

		final File archiveFile = archive;
		final File gitSpace = uniqueDir;

		if (localOrUrlOrGit.equals("Git")){
			log.debug("String is: "+gitSpaceString);
			log.debug("Before addBenchmakrGit: "+ gitSpace.getAbsolutePath());
			Util.threadPoolExecute(() -> {
				try {
					addBenchmarksGit(gitSpace, userId, spaceId, typeId, downloadable, perm, uploadMethod,
					                         statusId, hasDependencies, linked, depRootSpaceId
					);

					BenchmarkUploadStatus status = Uploads.getBenchmarkStatus(statusId);

					if (status.isFileUploadComplete()) {
						// if the benchmarks archive was successfully uploaded record that in the weekly reports table
						Reports.addToEventOccurrencesNotRelatedToQueue("benchmark archives uploaded", 1);
						// Record the total number of benchmarks uploaded in the weekly reports data table
						int totalBenchmarksUploaded = status.getTotalBenchmarks();
						Reports.addToEventOccurrencesNotRelatedToQueue("benchmarks uploaded", totalBenchmarksUploaded);
					}
				} catch (Exception e) {
					String msg = "userId:      " + userId
						+ "\nspaceId:     " + spaceId
						+ "\narchiveFile: " + archiveFile.getName()
					;
					log.error("handleUploadRequest", msg, e);
				} finally {
					Uploads.benchmarkEverythingComplete(statusId);
				}
			});
		}
		else{
			Util.threadPoolExecute(() -> {
				try {
					addBenchmarksFromArchive(archiveFile, userId, spaceId, typeId, downloadable, perm, uploadMethod,
					                         statusId, hasDependencies, linked, depRootSpaceId
					);

					BenchmarkUploadStatus status = Uploads.getBenchmarkStatus(statusId);

					if (status.isFileUploadComplete()) {
						// if the benchmarks archive was successfully uploaded record that in the weekly reports table
						Reports.addToEventOccurrencesNotRelatedToQueue("benchmark archives uploaded", 1);
						// Record the total number of benchmarks uploaded in the weekly reports data table
						int totalBenchmarksUploaded = status.getTotalBenchmarks();
						Reports.addToEventOccurrencesNotRelatedToQueue("benchmarks uploaded", totalBenchmarksUploaded);
					}
				} catch (Exception e) {
					String msg = "userId:      " + userId
						+ "\nspaceId:     " + spaceId
						+ "\narchiveFile: " + archiveFile.getName()
					;
					log.error("handleUploadRequest", msg, e);
				} finally {
					Uploads.benchmarkEverythingComplete(statusId);
				}
			});
		}
	}

	/**
	 * Extracts the permissions object contained in the given form
	 *
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
	 *
	 * @param form A list of form items contained in the request
	 * @return True if the request is valid to act on, false otherwise
	 * @author ??? - modified by Ben
	 */
	private ValidatorStatusCode isRequestValid(HashMap<String, Object> form, HttpServletRequest request) {
		final String method = "isRequestValid";
		try {

			if (!Validator.isValidPosInteger((String) form.get(BENCHMARK_TYPE))) {
				return new ValidatorStatusCode(false, "The given benchmark processor ID is not a valid integer");
			}

			if (!Validator.isValidPosInteger((String) form.get(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The given space ID is not a valid integer");
			}
			if (!Validator.isValidBool((String) form.get(BENCH_DOWNLOADABLE))) {
				return new ValidatorStatusCode(false, "The 'bench downloadable' option needs to be a valid boolean");
			}

			// Make sure we have a valid upload method
			String uploadMethod = ((String) form.get(UPLOAD_METHOD));
			if (!(uploadMethod.equals("convert") || uploadMethod.equals("dump"))) {
				return new ValidatorStatusCode(false, "The upload method needs to be either 'convert' or 'dump'");
			}
			String fileName = null;
			// Last test, return true when we find a valid file extension
			if (form.get(FILE_LOC).equals("local")) {
				fileName = ((PartWrapper) form.get(BENCHMARK_FILE)).getName();
				if (!Validator.isValidArchiveType(fileName)) {
					return new ValidatorStatusCode(false, "Uploaded archives need to be either .zip, .tar, or .tgz");
				}
			}
			else if (form.get(FILE_LOC).equals("URL")) {
				fileName = (String) form.get(FILE_URL);
				if (!Validator.isValidArchiveType(fileName)) {
					return new ValidatorStatusCode(false, "Uploaded archives need to be either .zip, .tar, or .tgz");
				}
			}
			else {
				log.debug("in else");
				fileName = (String) form.get(FILE_GIT);
				log.debug("fileName: "+ fileName);
				if (!Validator.isValidGitType(fileName)) {
					return new ValidatorStatusCode(false, "Uploaded Git URLs need to be .git");
				}
			}

			Permission perm = SessionUtil.getPermission(request, Integer.parseInt((String) form.get(R.SPACE)));

			log.trace(method, "perm=" + perm);
			log.trace(method, "uploadMethod=" + uploadMethod);

			if (perm == null || (!perm.canAddBenchmark() && uploadMethod.equals("dump"))) {
				// They don't have permissions, send forbidden error
				return new ValidatorStatusCode(false, "You do not have permission to upload benchmarks to this space");
			} else if (uploadMethod.equals("convert") && !(perm.canAddBenchmark() && perm.canAddSpace())) {
				return new ValidatorStatusCode(
						false, "You do not have permission to upload benchmarks and subspaces to this space");
			}

			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error uploading benchmarks");
	}

	/**
	 * Checks to see if any of the spaces that will be created by the given upload directory conflict with existing
	 * names
	 *
	 * @param uniqueDir
	 * @return A ValidatorStatusCode set to True if there is NO conflict and set to false with a message if a conflict
	 * exists
	 */

	private static ValidatorStatusCode doSpaceNamesConflict(File uniqueDir, int parentSpaceId) {
		try {
			List<Space> subspaces = Spaces.getSubSpaces(parentSpaceId);
			HashSet<String> subspaceNames = new HashSet<>();
			for (Space s : subspaces) {
				subspaceNames.add(s.getName());
			}
			for (File f : uniqueDir.listFiles()) {
				// If it's a sub-directory and as such a subspace
				if (f.isDirectory()) {
					String curName = f.getName();
					if (subspaceNames.contains(curName)) {
						return new ValidatorStatusCode(false,
						                               "Creating spaces for your benchmarks would lead to having two subspaces with the name " +
								                               curName); // found a conflict
					}
					subspaceNames.add(curName);
				}
			}

			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "There was an internal error uploading your benchmarks");
	}
}
