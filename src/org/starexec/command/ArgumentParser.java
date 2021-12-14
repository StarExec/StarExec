package org.starexec.command;

import org.starexec.constants.R;
import org.starexec.data.to.Permission;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is responsible for taking in the shell arguments from users, formatted as
 * Maps of keys to values, and using those arguments to make calls in the Connection
 * API, which takes care of actually making request to Starexec. In other words, this
 * class is a midpoint between the shell interface and the StarexecCommand Java API, which is housed
 * in Connection
 *
 * This class also makes calls to a validator to ensure that shell arguments are appropriate for the desired
 * calls
 */
class ArgumentParser {

	final private CommandLogger log = CommandLogger.getLogger(ArgumentParser.class);

	Connection con;

	/**
	 * Gets the last server error message that was returned
	 *
	 * @return A string error message describing the last error. These messages
	 *         should be human readable.
	 */
	public String getLastServerError() {
		return con.getLastError();
	}

	/**
	 * Sets the new Connection object's username and password based on
	 * user-specified parameters. Also sets the instance of StarExec that is
	 * being connected to
	 *
	 * @param commandParams User specified parameters
	 */

	protected ArgumentParser(Map<String, String> commandParams) {
		String base = null;
		String username = "";
		String password = "";
		if (commandParams.containsKey(C.PARAM_BASEURL)) {
			base = commandParams.get(C.PARAM_BASEURL);
		}
		if (commandParams.get(C.PARAM_USER).equals(C.PARAM_GUEST)) {
			username = "public";
			password = "public";
		} else {
			username = commandParams.get(C.PARAM_USER);
			password = commandParams.get(C.PARAM_PASSWORD);
		}
		if (base == null) {
			con = new Connection(username, password);
		} else {
			con = new Connection(username, password, base);
		}
	}

	/**
	 * Creates a new Connection that uses the same username and password as the
	 * current connection.
	 */
	protected void refreshConnection() {
		con = new Connection(con);
	}

	/**
	 * Gets the max completion ID for info downloads on the given job.
	 *
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen for the job, or 0 if not seen
	 */
	protected int getJobInfoCompletion(int jobID) {
		return con.getJobInfoCompletion(jobID);
	}

	/**
	 * Gets the max completion ID yet seen for output downloads on a given job
	 *
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen yet, or 0 if not seen.
	 */

	protected PollJobData getJobOutCompletion(int jobID) {
		return con.getJobOutCompletion(jobID);

	}

	/**
	 * Log into StarExec with the username and password of this connection
	 *
	 * @return An integer indicating status, with 0 being normal and a negative
	 *         integer indicating an error
	 * @author Eric Burns
	 */
	protected int login() {
		return con.login();
	}

	/**
	 * Ends the current Starexec session
	 *
	 * @author Eric Burns
	 */
	protected void logout() {
		con.logout();
	}

	/**
	 * Creates a POST request to StarExec to create a new job
	 *
	 * @param commandParams A Map containing key/value pairs gathered from
	 *        the user input at the command line
	 * @return the new job ID on success, a negative integer otherwise
	 * @author Eric Burns
	 */
	protected int createJob(Map<String, String> commandParams) {
		log.log("Creating job.");
		try {
			int valid = CommandValidator.isValidCreateJobRequest(commandParams);
			if (valid < 0) {
				log.log("Was not a valid create job request.");
				return valid;
			}
			Integer wallclock = null;
			Integer cpu = null;
			Double maxMemory = null;
			Integer resultsInterval = 0;
			if (commandParams.containsKey(C.PARAM_WALLCLOCKTIMEOUT)) {
				wallclock = Integer.parseInt(commandParams.get(C.PARAM_WALLCLOCKTIMEOUT));
			}
			if (commandParams.containsKey(C.PARAM_CPUTIMEOUT)) {
				cpu = Integer.parseInt(commandParams.get(C.PARAM_CPUTIMEOUT));
			}
			if (commandParams.containsKey(C.PARAM_MEMORY)) {
				maxMemory = Double.parseDouble(commandParams.get(C.PARAM_MEMORY));
			}
			if (commandParams.containsKey(C.PARAM_RESULTS_INTERVAL)) {
				resultsInterval = Integer.parseInt(commandParams.get(C.PARAM_RESULTS_INTERVAL));
			}
			Boolean useDepthFirst = true;
			if (commandParams.containsKey(C.PARAM_TRAVERSAL)) {
				if (commandParams.get(C.PARAM_TRAVERSAL).equals(C.ARG_ROUNDROBIN)) {
					useDepthFirst = false;
				}
			}
			String postProcId = "-1";
			String preProcId = "-1";
			if (commandParams.containsKey(C.PARAM_PROCID)) {
				postProcId = commandParams.get(C.PARAM_PROCID);
			}
			if (commandParams.containsKey(C.PARAM_PREPROCID)) {
				preProcId = commandParams.get(C.PARAM_PREPROCID);
			}

			String name = getDefaultName("");
			String desc = "";
			if (commandParams.containsKey(C.PARAM_NAME)) {
				name = commandParams.get(C.PARAM_NAME);
			}

			if (commandParams.containsKey(C.PARAM_DESC)) {
				desc = commandParams.get(C.PARAM_DESC);
			}
			boolean startPaused = false;
			if (commandParams.containsKey(C.PARAM_PAUSED)) {
				startPaused = true;
			}
			long seed = 0;
			if (commandParams.containsKey(C.PARAM_SEED)) {
				seed = Long.parseLong(commandParams.get(C.PARAM_SEED));
			}
			return con.createJob(Integer.parseInt(commandParams.get(C.PARAM_ID)), name, desc,
					Integer.parseInt(postProcId), Integer.parseInt(preProcId),
					Integer.parseInt(commandParams.get(C.PARAM_QUEUEID)), wallclock, cpu, useDepthFirst, maxMemory,
					startPaused, seed, commandParams.containsKey(C.PARAM_SUPPRESS_TIMESTAMPS), resultsInterval);

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Sends a link request to the StarExec server and returns a status code
	 * indicating the result of the request
	 *
	 * @param commandParams The parameters given by the user at the command
	 *        line.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative
	 *         number is an error.
	 */
	protected int linkPrimitives(Map<String, String> commandParams, String type) {

		try {
			int valid = CommandValidator.isValidCopyRequest(commandParams, type);

			if (valid < 0) {
				return valid;
			}

			Integer[] ids = CommandParser.convertToIntArray(commandParams.get(C.PARAM_ID));

			return con.linkPrimitives(ids, getParamFrom(commandParams), Integer.parseInt(commandParams.get(C.PARAM_TO)),
					commandParams.containsKey(C.PARAM_HIERARCHY), type);

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * This handles the case where the from parameter is missing, which is
	 * allowed in link or copy commands.
	 *
	 * @param commandParams The parameters given by the user at the command
	 *        line.
	 * @return the from parameter's integer value, or null if there is no such
	 *         parameter.
	 */
	protected Integer getParamFrom(Map<String, String> commandParams) {
		String sfrom = commandParams.get(C.PARAM_FROM);
		Integer ifrom = null;
		if (sfrom != null)
			ifrom = Integer.parseInt(sfrom);
		return ifrom;
	}

	/**
	 * Sends a copy or link request to the StarExec server and returns a status
	 * code indicating the result of the request
	 *
	 * @param commandParams The parameters given by the user at the command
	 *        line.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative
	 *         number is an error.
	 */
	protected List<Integer> copyPrimitives(Map<String, String> commandParams, String type) {
		List<Integer> fail = new ArrayList<>();
		try {
			int valid = CommandValidator.isValidCopyRequest(commandParams, type);
			if (valid < 0) {
				fail.add(valid);
				return fail;
			}

			Integer[] ids = CommandParser.convertToIntArray(commandParams.get(C.PARAM_ID));
			Boolean copyPrimitives = commandParams.containsKey(C.PARAM_COPY_PRIMITIVES) &&
			                         Boolean.parseBoolean(commandParams.get(C.PARAM_COPY_PRIMITIVES));
			return con.copyPrimitives(ids, getParamFrom(commandParams), Integer.parseInt(commandParams.get(C.PARAM_TO)),
					commandParams.containsKey(C.PARAM_HIERARCHY), copyPrimitives, type);

		} catch (Exception e) {
			fail.add(Status.ERROR_INTERNAL);
			return fail;
		}
	}

	/**
	 * Creates a subspace of an existing space on StarExec
	 *
	 * @param commandParams A Map containing key/value pairs gathered from
	 *        user input at the command line
	 * @return the new space ID on success and a negative error code otherwise
	 * @author Eric Burns
	 */

	protected int createSubspace(Map<String, String> commandParams) {
		try {
			int valid = CommandValidator.isValidCreateSubspaceRequest(commandParams);
			if (valid < 0) {
				return valid;
			}

			String name = getDefaultName("");

			if (commandParams.containsKey(C.PARAM_NAME)) {
				name = commandParams.get(C.PARAM_NAME);
			}
			String desc = "";
			if (commandParams.containsKey(C.PARAM_DESC)) {
				desc = commandParams.get(C.PARAM_DESC);
			}

			Boolean locked = false;
			if (commandParams.containsKey(C.PARAM_LOCKED)) {
				locked = true;
			}
			Permission p = new Permission(false);
			for (String x : C.PARAMS_PERMS) {

				if (commandParams.containsKey(x) || commandParams.containsKey(C.PARAM_ENABLE_ALL_PERMISSIONS)) {
					p.setPermissionOn(x);
				}
			}
			return con.createSubspace(name, desc, Integer.parseInt(commandParams.get(C.PARAM_ID)), p, locked);

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Removes the association between a primitive and a space on StarExec
	 *
	 * @param commandParams Parameters given by the user
	 * @param type The type of primitive being remove
	 * @return 0 on success, and a negative error code on failure
	 * @author Eric Burns
	 */
	protected int removePrimitive(Map<String, String> commandParams, String type) {
		try {
			int valid = CommandValidator.isValidRemoveRequest(commandParams, type);
			if (valid < 0) {
				return valid;
			}
			Integer fromSpace = -1;
			if (!type.equals("subspace")) {
				fromSpace = Integer.parseInt(commandParams.get(C.PARAM_FROM));
			}
			List<Integer> ids = CommandParser.convertToIntList(commandParams.get(C.PARAM_ID));
			return con.removePrimitives(ids, fromSpace, type, commandParams.containsKey(C.PARAM_RECYCLE_PRIMS));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Resumes a job on starexec that was paused previously
	 *
	 * @param commandParams Parameters given by the user at the command line.
	 *        Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */

	protected int resumeJob(Map<String, String> commandParams) {
		return pauseOrResumeJob(commandParams, false);
	}

	/**
	 * Pauses a job that is currently running on starexec
	 *
	 * @param commandParams Parameters given by the user at the command line.
	 *        Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */

	protected int pauseJob(Map<String, String> commandParams) {
		return pauseOrResumeJob(commandParams, true);
	}

	/**
	 * Pauses or resumes a job depending on the value of pause
	 *
	 * @param commandParams Parameters given by the user at the command line
	 * @param pause Pauses a job if true and resumes it if false
	 * @return 0 on success or a negative error code on failure
	 */

	private int pauseOrResumeJob(Map<String, String> commandParams, boolean pause) {
		try {
			int valid = CommandValidator.isValidPauseOrResumeRequest(commandParams);
			if (valid < 0) {
				return valid;
			}
			return con.pauseOrResumeJob(Integer.parseInt(commandParams.get(C.PARAM_ID)), pause);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}

	}

	protected int rerunPair(Map<String, String> commandParams) {
		try {
			int valid = CommandValidator.isValidRerunRequest(commandParams);
			if (valid < 0) {
				return valid;
			}
			return con.rerunPair(Integer.parseInt(commandParams.get(C.PARAM_ID)));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	protected int rerunJob(Map<String, String> commandParams) {
		try {
			int valid = CommandValidator.isValidRerunRequest(commandParams);
			if (valid < 0) {
				return valid;
			}
			return con.rerunJob(Integer.parseInt(commandParams.get(C.PARAM_ID)));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Deletes a primitive on StarExec
	 *
	 * @param commandParams A Map of key/value pairs given by the user at
	 *        the command line
	 * @param type -- The type of primitive to delete
	 * @return 0 on success and a negative integer otherwise
	 * @author Eric Burns
	 */

	protected int deletePrimitive(Map<String, String> commandParams, String type) {
		try {
			int valid = CommandValidator.isValidDeleteRequest(commandParams);
			if (valid < 0) {
				return valid;
			}

			List<Integer> ids = CommandParser.convertToIntList(commandParams.get(C.PARAM_ID));
			return con.deletePrimitives(ids, type);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * Function for downloading archives from StarExec with the given parameters
	 * and file output location.
	 *
	 * @param commandParams A list of name/value pairs that the user entered
	 *        into the command line
	 * @return 0 on success, a negative integer on error
	 * @author Eric Burns
	 */

	protected int downloadArchive(String type, Integer since, Long lastModified, Boolean hierarchy, String procClass,
			Map<String, String> commandParams) {
		try {
			int valid = CommandValidator.isValidDownloadRequest(commandParams, type, since);
			if (valid < 0) {
				log.log("Not a valid download request");
				return valid;
			}
			String location = commandParams.get(C.PARAM_OUTPUT_FILE);

			if (type.equals(R.JOB_OUTPUTS)) {
				log.log("Type was " + R.JOB_OUTPUTS);
				List<Integer> ids = CommandParser.convertToIntList(commandParams.get(C.PARAM_ID));
				return con.downloadJobPairs(ids, location);
			} else {
				log.log("Type was not " + R.JOB_OUTPUTS);
				Integer id = Integer.parseInt(commandParams.get(C.PARAM_ID));
				Integer updateId = null;
				if (commandParams.containsKey(C.PARAM_PROCID)) {
					updateId = Integer.parseInt(commandParams.get(C.PARAM_PROCID));
				}

				log.log("Putting in request for server to generate desired archive.");

				// First, put in the request for the server to generate the
				// desired archive
				return con.downloadArchive(id, type, since, lastModified, location,
						commandParams.containsKey(C.PARAM_EXCLUDE_SOLVERS),
						commandParams.containsKey(C.PARAM_EXCLUDE_BENCHMARKS),
						commandParams.containsKey(C.PARAM_INCLUDE_IDS), hierarchy, procClass,
						commandParams.containsKey(C.PARAM_ONLY_COMPLETED),
						commandParams.containsKey(C.PARAM_GET_ATTRIBUTES), updateId,
						Boolean.parseBoolean(commandParams.get(C.PARAM_LONG_PATH)));
			}

		} catch (IOException e) {
			log.log("Caught exception in downloadArchive: " + Util.getStackTrace(e));
			return Status.ERROR_INTERNAL;
		}

	}

	/**
	 * Lists the IDs and names of some kind of primitives in a given space or by
	 * user, depending on the parameters given
	 *
	 * @param commandParams Parameters given by the user at the command line
	 * @return An integer error code with 0 indicating success and a negative
	 *         number indicating an error
	 * @author Eric Burns
	 */
	protected Map<Integer, String> listPrimsBySpaceOrUser(String type, Map<String, String> commandParams) {
		Map<Integer, String> errorMap = new HashMap<>();

		try {

			Map<String, String> urlParams = new HashMap<>();
			urlParams.put("id", commandParams.get(C.PARAM_ID));
			urlParams.put(C.FORMPARAM_TYPE, type);
			int valid = CommandValidator.isValidGetPrimRequest(urlParams, commandParams);
			if (valid < 0) {
				errorMap.put(valid, null);
				return errorMap;
			}
			Integer id = -1;
			Integer limit = null;
			if (commandParams.containsKey(C.PARAM_ID)) {
				id = Integer.parseInt(commandParams.get(C.PARAM_ID));
			}
			if (commandParams.containsKey(C.PARAM_LIMIT)) {
				limit = Integer.parseInt(commandParams.get(C.PARAM_LIMIT));
			}
			if (type.equals("solverconfigs")) {
				return con.getSolverConfigs(id, limit);

			} else {
				return con.listPrims(id, limit, commandParams.containsKey(C.PARAM_USER), type);

			}

		} catch (Exception e) {
			errorMap.put(Status.ERROR_INTERNAL, null);

			return errorMap;
		}
	}

	/**
	 * Sets a space or space hierarchy as either public or private
	 *
	 * @param commandParams Parameters given by the user at the command line
	 * @param setPublic Set public if true and private if false
	 * @return 0 if successful and a negative error code otherwise
	 * @author Eric Burns
	 */
	protected int setSpaceVisibility(Map<String, String> commandParams, boolean setPublic) {
		try {

			int valid = CommandValidator.isValidSetSpaceVisibilityRequest(commandParams);
			if (valid < 0) {
				return valid;
			}
			Boolean hierarchy = false;
			if (commandParams.containsKey(C.PARAM_HIERARCHY)) {
				hierarchy = true;
			}

			return con.setSpaceVisibility(Integer.parseInt(commandParams.get(C.PARAM_ID)), hierarchy, setPublic);

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * This function updates one of the default settings of the current Starexec
	 * user
	 *
	 * @param setting The field to assign a new value to
	 * @param commandParams Parameters given by the user at the command line
	 * @return A code indicating the success of the operation
	 * @author Eric Burns
	 */
	protected int setUserSetting(String setting, Map<String, String> commandParams) {

		int valid = CommandValidator.isValidSetUserSettingRequest(commandParams);
		if (valid < 0) {
			return valid;
		}
		String newVal = commandParams.get(C.PARAM_VAL);
		return con.setUserSetting(setting, newVal);

	}

	/**
	 * This method takes in a Map mapping String keys to String values and
	 * creates and HTTP POST request that pushes a solver to Starexec
	 *
	 * @param commandParams The parameters from the command line. "f" or "url",
	 *        and "id" are required.
	 * @return A status code indicating success or failure
	 * @author Eric Burns
	 */

	protected int uploadBenchmarks(Map<String, String> commandParams) {
		int valid = CommandValidator.isValidUploadBenchmarkRequest(commandParams);
		if (valid < 0) {
			return valid;
		}

		Boolean dependency = false;
		String depRoot = "-1";
		Boolean depLinked = false;

		// if the dependency parameter exists, we're using the dependencies it
		// specifies
		if (commandParams.containsKey(C.PARAM_DEPENDENCY)) {
			dependency = true;
			depRoot = commandParams.get(C.PARAM_DEPENDENCY);
			if (commandParams.containsKey(C.PARAM_LINKED)) {
				depLinked = true;
			}
		}

		String type = commandParams.get(C.PARAM_BENCHTYPE);
		String space = commandParams.get(C.PARAM_ID);

		// don't preserve hierarchy by default, but do so if the hierarchy
		// parameter is present
		boolean hierarchy = false;
		if (commandParams.containsKey(C.PARAM_HIERARCHY)) {
			hierarchy = true;
		}

		String url = "";
		String upMethod = "local";
		// if a url is present, the file should be taken from the url
		if (commandParams.containsKey(C.PARAM_URL)) {
			if (commandParams.containsKey(C.PARAM_FILE)) {
				return Status.ERROR_FILE_AND_URL;
			}
			upMethod = "URL";
			url = commandParams.get(C.PARAM_URL);
		}
		Boolean downloadable = false;
		if (commandParams.containsKey(C.PARAM_DOWNLOADABLE)) {
			downloadable = true;
		}
		Permission p = new Permission();
		for (String x : C.PARAMS_PERMS) {
			if (commandParams.containsKey(x) || commandParams.containsKey(C.PARAM_ENABLE_ALL_PERMISSIONS)) {
				p.setPermissionOn(x);
			}
		}
		return con.uploadBenchmarks(commandParams.get(C.PARAM_FILE), Integer.parseInt(type), Integer.parseInt(space),
				upMethod, p, url, downloadable, hierarchy, dependency, depLinked, Integer.parseInt(depRoot));

	}

	/**
	 * This function handles user requests for uploading a space XML archive.
	 *
	 * @param commandParams The key/value pairs given by the user at the command
	 *        line. Should contain ID and File keys
	 * @return the new configuration ID on success, and a negative error code
	 *         otherwise
	 * @author Eric Burns
	 */

	protected int uploadConfiguration(Map<String, String> commandParams) {
		try {

			int valid = CommandValidator.isValidUploadConfigRequest(commandParams);
			if (valid < 0) {
				return valid;
			}
			File f = new File(commandParams.get(C.PARAM_FILE));
			String name = getDefaultName(f.getName() + " ");
			String desc = "";

			if (commandParams.containsKey(C.PARAM_NAME)) {
				name = commandParams.get(C.PARAM_NAME);
			}

			if (commandParams.containsKey(C.PARAM_DESC)) {
				desc = commandParams.get(C.PARAM_DESC);
			}

			return con.uploadConfiguration(name, desc, commandParams.get(C.PARAM_FILE),
					Integer.parseInt(commandParams.get(C.PARAM_ID)));

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	/**
	 * This method takes in a Map mapping String keys to String values and
	 * creates and HTTP POST request that pushes a processor to Starexec
	 *
	 * @param commandParams The parameters from the command line. A file and an
	 *        ID are required.
	 * @return The new processor ID on success, or a negative error code on
	 *         failure
	 * @author Eric Burns
	 */

	private int uploadProcessor(Map<String, String> commandParams, String type) {

		int valid = CommandValidator.isValidUploadProcessorRequest(commandParams);
		if (valid < 0) {
			return valid;
		}

		String community = commandParams.get(C.PARAM_ID); // id is one of the
															// required
															// parameters

		// if a name is given explicitly, use it instead
		String name = getDefaultName(new File(commandParams.get(C.PARAM_FILE)).getName());
		if (commandParams.containsKey(C.PARAM_NAME)) {
			name = commandParams.get(C.PARAM_NAME);
		}

		// If there is a description, get it
		String desc = "";
		if (commandParams.containsKey(C.PARAM_DESC)) {
			desc = commandParams.get(C.PARAM_DESC);
		}
		return con.uploadProcessor(name, desc, commandParams.get(C.PARAM_FILE), Integer.parseInt(community), type);

	}

	/**
	 * Handles requests for uploading post-processors.
	 *
	 * @param commandParams The key/value pairs given by the user at the command
	 *        line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */

	protected int uploadPostProc(Map<String, String> commandParams) {
		return uploadProcessor(commandParams, "post");
	}

	/**
	 * Handles requests for uploading pre-processors.
	 *
	 * @param commandParams The key/value pairs given by the user at the command
	 *        line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */

	protected int uploadPreProc(Map<String, String> commandParams) {
		return uploadProcessor(commandParams, "pre");
	}

	/**
	 * Handles requests for uploading benchmark processors.
	 *
	 * @param commandParams The key/value pairs given by the user at the command
	 *        line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */

	protected int uploadBenchProc(Map<String, String> commandParams) {
		return uploadProcessor(commandParams, R.BENCHMARK);
	}

	/**
	 * This function handles user requests for uploading an xml archive (space
	 * or job).
	 *
	 * @param commandParams The key/value pairs given by the user at the command
	 *        line. Should contain ID and File keys
	 * @param isJobXML true if job xml upload, false otherwise
	 * @return 0 on success, and a negative error code otherwise
	 * @author Julio Cervantes
	 */

	protected List<Integer> uploadXML(Map<String, String> commandParams, boolean isJobXML) {
		List<Integer> fail = new ArrayList<>();

		try {
			int valid = CommandValidator.isValidUploadXMLRequest(commandParams);
			if (valid < 0) {
				fail.add(valid);
				return fail;
			}
			return con.uploadXML(commandParams.get(C.PARAM_FILE), Integer.parseInt(commandParams.get(C.PARAM_ID)),
					isJobXML);

		} catch (Exception e) {
			fail.add(Status.ERROR_INTERNAL);
			return fail;
		}
	}

	/**
	 * Prints out the status of a benchmark upload request to stdout, assuming
	 * the status could be found
	 *
	 * @param commandParams
	 * @return 0 on success and a negative error code otherwise
	 */
	protected int printBenchStatus(Map<String, String> commandParams) {
		int valid = CommandValidator.isValidPrintBenchUploadStatusRequest(commandParams);
		if (valid < 0) {
			return valid;
		}
		String status = con.getBenchmarkUploadStatus(Integer.parseInt(commandParams.get(C.PARAM_ID)));
		if (status != null) {
			System.out.println(status);
			return 0;
		} else {
			return Status.ERROR_SERVER;
		}
	}

	protected Map<String, String> getPrimitiveAttributes(Map<String, String> commandParams, String type) {
		Map<String, String> failMap = new HashMap<>();
		try {
			int valid = CommandValidator.isValidGetPrimitiveAttributesRequest(commandParams);
			if (valid < 0) {
				failMap.put("-1", String.valueOf(valid));
				return failMap;
			}
			int id = Integer.parseInt(commandParams.get(C.PARAM_ID));

			return con.getPrimitiveAttributes(id, type);
		} catch (Exception e) {
			e.printStackTrace();
			failMap.put("-1", String.valueOf(Status.ERROR_INTERNAL));
			return failMap;
		}

	}

	/**
	 * This method takes in a Map mapping String keys to String values and
	 * creates and HTTP POST request that pushes a solver to Starexec
	 *
	 * @param commandParams The parameters from the command line. A file or url
	 *        and and ID are required.
	 * @return The ID of the newly uploaded solver on success, or a negative
	 *         error code on failure
	 * @author Eric Burns
	 */

	protected int uploadSolver(Map<String, String> commandParams) {
		int valid = CommandValidator.isValidSolverUploadRequest(commandParams);
		if (valid < 0) {
			return valid;
		}
		File f = null;
		String name = getDefaultName("");
		String desc = "";
		String space = commandParams.get(C.PARAM_ID); // id is one of the
														// required parameters
		String upMethod = "local";
		String url = "";
		String descMethod = "upload";
		Boolean downloadable = false;
		Boolean runTestJob = false;
		// if a url is present, the file should be taken from the url
		if (commandParams.containsKey(C.PARAM_URL)) {
			upMethod = "URL";
			url = commandParams.get(C.PARAM_URL);
		} else {
			f = new File(commandParams.get(C.PARAM_FILE));
			// name defaults to the name of the file plus the date if none is
			// given
			name = getDefaultName(f.getName() + " ");
		}

		// if a name is given explicitly, use it instead
		if (commandParams.containsKey(C.PARAM_NAME)) {
			name = commandParams.get(C.PARAM_NAME);
		}

		// d is the key used for directly sending a string description
		if (commandParams.containsKey(C.PARAM_DESC)) {
			descMethod = "text";
			desc = commandParams.get(C.PARAM_DESC);

			// df is the "description file" key, which should have a filepath
			// value
		} else if (commandParams.containsKey(C.PARAM_DESCRIPTION_FILE)) {
			descMethod = "file";
			desc = commandParams.get(C.PARAM_DESCRIPTION_FILE); // set the
																// description
																// to be the
																// filepath
		}

		if (commandParams.containsKey(C.PARAM_DOWNLOADABLE)) {
			downloadable = true;
		}
		if (commandParams.containsKey(C.PARAM_RUN)) {
			runTestJob = true;
		}
		Integer settingId = null;
		if (commandParams.containsKey(C.PARAM_SETTING)) {
			settingId = Integer.parseInt(C.PARAM_SETTING);
		}
		Integer type = C.DEFAULT_SOLVER_TYPE;
		if (commandParams.containsKey(C.PARAM_TYPE)) {
			type = Integer.parseInt(commandParams.get(C.PARAM_TYPE));
		}
		if (upMethod.equals("local")) {
			return con.uploadSolver(name, desc, descMethod, Integer.parseInt(space), f.getAbsolutePath(), downloadable,
					runTestJob, settingId, type);
		} else {
			return con.uploadSolverFromURL(name, desc, descMethod, Integer.parseInt(space), url, downloadable,
					runTestJob, settingId, type);
		}

	}

	/**
	 * @return whether the Connection object represents a valid connection to
	 *         the server
	 * @author Eric Burns
	 */

	protected boolean isConnectionValid() {
		return con.isValid();

	}

	/**
	 * Returns the string name given to a primitive if none is specified. The
	 * date is used currently
	 *
	 * @param prefix A prefix which should be added to the name
	 * @return A string that will be valid for use as a primitive on Starexec
	 * @author Eric Burns
	 */
	private String getDefaultName(String prefix) {
		String date = Calendar.getInstance().getTime().toString();
		date = date.replace(":", " ");

		return prefix + date;
	}
}
