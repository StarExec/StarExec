package org.starexec.command;

/**
 * This class is responsible for validating the arguments given to functions in the ArgumentParser.
 * These arguments ultimately come from user input at the command line or through a file
 */


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.starexec.constants.R;
import org.starexec.util.Validator;

public class CommandValidator {
	
	/**which archives can we download from Starexec*/
	public static String[] VALID_ARCHIVETYPES={"zip"};

	
	private static String missingParam=null;
	private static List<String> unnecessaryParams=new ArrayList<String>();
	
	//the following lists specify the parameters, either required or optional, that are accepted by a certain
	//command or set of commands
	private static String[] allowedRemoveParams=new String[]{C.PARAM_ID,C.PARAM_FROM};
	private static String[] allowedRemoveSubspaceParams = new String[] {C.PARAM_ID, C.PARAM_RECYCLE_PRIMS};
	private static String[] allowedDownloadParams=new String[]{C.PARAM_ID,C.PARAM_OUTPUT_FILE,C.PARAM_OVERWRITE};
	private static String[] allowedDownloadSpaceXMLParams=new String[]{C.PARAM_ID,C.PARAM_OUTPUT_FILE,C.PARAM_OVERWRITE,C.PARAM_GET_ATTRIBUTES,C.PARAM_PROCID};

	private static String[] allowedNewDownloadParams=new String[]{C.PARAM_ID,C.PARAM_OUTPUT_FILE,C.PARAM_OVERWRITE,C.PARAM_SINCE};
	private static String[] allowedDownloadSpaceParams=new String[]{C.PARAM_ID,C.PARAM_OUTPUT_FILE,C.PARAM_OVERWRITE,C.PARAM_EXCLUDE_BENCHMARKS,C.PARAM_EXCLUDE_SOLVERS};
	private static String[] allowedDownloadCSVParams=new String[]{C.PARAM_ID,C.PARAM_OUTPUT_FILE,C.PARAM_OVERWRITE,C.PARAM_INCLUDE_IDS, C.PARAM_ONLY_COMPLETED};
	private static String[] allowedSetUserSettingParams=new String[]{C.PARAM_VAL};
	private static String[] allowedSetSpaceVisibilityParams=new String[]{C.PARAM_ID,C.PARAM_HIERARCHY};
	private static String[] allowedLoginParams=new String[]{C.PARAM_USER,C.PARAM_PASSWORD,C.PARAM_BASEURL};
	private static String[] allowedDeleteParams=new String[]{C.PARAM_ID};
    private static String[] allowedCopyUserParams=new String[]{C.PARAM_TO,C.PARAM_ID,C.PARAM_HIERARCHY};
	private static String[] allowedCopySpaceParams=new String[]{C.PARAM_TO,C.PARAM_ID, C.PARAM_FROM};

    private static String[] allowedCopySolverParams=new String[]{C.PARAM_ID,C.PARAM_FROM,C.PARAM_TO,C.PARAM_HIERARCHY};
    private static String[] allowedCopyBenchmarkParams=new String[]{C.PARAM_ID,C.PARAM_FROM,C.PARAM_TO};
	private static String[] allowedPollJobParams=new String[]{C.PARAM_OUTPUT_FILE,C.PARAM_ID,C.PARAM_TIME,C.PARAM_OVERWRITE};
	private static String[] allowedRunFileParams=new String[]{C.PARAM_FILE,C.PARAM_VERBOSE};
	private static String[] allowedSleepParams=new String[]{C.PARAM_TIME};
	private static String[] allowedPauseOrResumeParams=new String[]{C.PARAM_ID};
	private static String[] allowedRerunParams=new String[]{C.PARAM_ID};

	private static String[] allowedCreateSubspaceParams=new String[]{C.PARAM_ID,C.PARAM_NAME,C.PARAM_DESC,
		C.PARAM_ENABLE_ALL_PERMISSIONS,"addSolver","addUser","addSpace","addJob","addBench","removeSolver","removeUser","removeSpace","removeJob","removeBench"};
	private static String[] allowedCreateJobParams=new String[]{C.PARAM_ID,C.PARAM_NAME,C.PARAM_DESC,C.PARAM_WALLCLOCKTIMEOUT, C.PARAM_RESULTS_INTERVAL,
		C.PARAM_CPUTIMEOUT,C.PARAM_QUEUEID,C.PARAM_PROCID, C.PARAM_TRAVERSAL, C.PARAM_MEMORY,C.PARAM_PAUSED, C.PARAM_SEED, C.PARAM_SUPPRESS_TIMESTAMPS};
	private static String[] allowedUploadSolverParams=new String[]{C.PARAM_ID,C.PARAM_TYPE,C.PARAM_PREPROCID,C.PARAM_FILE,C.PARAM_URL,C.PARAM_NAME,C.PARAM_DESC,
		C.PARAM_DESCRIPTION_FILE,C.PARAM_DOWNLOADABLE, C.PARAM_RUN, C.PARAM_SETTING};
	private static String[] allowedUploadBenchmarksParams= new String[] {C.PARAM_ID,C.PARAM_BENCHTYPE, C.PARAM_FILE,C.PARAM_URL,
		C.PARAM_DESC,C.PARAM_DESCRIPTION_FILE,C.PARAM_DEPENDENCY,C.PARAM_DOWNLOADABLE,C.PARAM_HIERARCHY,C.PARAM_LINKED,
		C.PARAM_ENABLE_ALL_PERMISSIONS,"addSolver","addUser","addSpace","addJob","addBench","removeSolver","removeUser","removeSpace","removeJob","removeBench"};
	private static String[] allowedUploadProcessorParams=new String[]{C.PARAM_ID,C.PARAM_NAME,C.PARAM_DESC,C.PARAM_FILE};
	private static String[] allowedUploadConfigParams=new String[] {C.PARAM_FILE,C.PARAM_ID,C.PARAM_FILE,C.PARAM_DESC};
    private static String[] allowedUploadXMLParams=new String[]{C.PARAM_ID,C.PARAM_FILE};
    private static String[] allowedPrintStatusParams=new String[]{C.PARAM_ID};
    private static String[] allowedGetPrimitiveAttributesParams=new String[]{C.PARAM_ID};

	private static String[] allowedLSParams=new String[]{C.PARAM_ID,C.PARAM_LIMIT,C.PARAM_USER}; 
	
	/**
	 * Gets the missing paramter that was last seen. If none has been seen yet, returns null.
	 * @return The name of the required parameter that is missing. 
	 */
	public static String getMissingParam() {
		return missingParam;
	}
	
	/**
	 * Checks whether the incoming arguments satisfy a request to remove a primitive
	 * @param commandParams Arguments given by the user
	 * @param type The type of primitive being removed.
	 * @return An integer status code as defined in Status.
	 */
	public static int isValidRemoveRequest(HashMap<String,String> commandParams, String type) {
		if (type.equals("subspace")) {
			if (!paramsExist(new String[]{C.PARAM_ID},commandParams)) {
				return Status.ERROR_MISSING_PARAM;
			}
			findUnnecessaryParams(allowedRemoveSubspaceParams,commandParams);

		} else {
			if (!paramsExist(new String[]{C.PARAM_ID,C.PARAM_FROM},commandParams)) {
				return Status.ERROR_MISSING_PARAM;
			}
			findUnnecessaryParams(allowedRemoveParams,commandParams);
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_FROM))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		if (!Validator.isValidPosIntegerList(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		return 0;
	}
	
	/**
	 * Validates a request to delete a primitive
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	
	public static int isValidDeleteRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!Validator.isValidPosIntegerList(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedDeleteParams,commandParams);
		return 0;
	}

    
    /**
     * Validates the given parameters to determine if they can be used to construct a valid
     * request to copy primitives on StarExec
     * @param commandParams A set of key/value parameters
     * @param type The type of primitive (solver, benchmark, etc.) being copied
     * @return 0 if the request is valid, and a negative error code otherwise
     */
	
    public static int isValidCopyRequest(HashMap<String,String> commandParams, String type) {
    	if (!paramsExist(new String[]{C.PARAM_ID,C.PARAM_TO},commandParams)) {
    		return Status.ERROR_MISSING_PARAM;
    	}
    	
    	if (!Validator.isValidPosIntegerList(commandParams.get(C.PARAM_ID)) 
    			|| (commandParams.containsKey(C.PARAM_FROM) && !Validator.isValidPosInteger(commandParams.get(C.PARAM_FROM)))
    			|| !Validator.isValidPosInteger(commandParams.get(C.PARAM_TO))) {
    		return Status.ERROR_INVALID_ID;
    	}
    	
    	//the hierarchy parameter is also acceptable if the type is either solver or space
    	if (type.equals("user")) {
    		findUnnecessaryParams(allowedCopyUserParams,commandParams);
    	} else if (type.equals(R.SPACE)) {
    		findUnnecessaryParams(allowedCopySpaceParams,commandParams);
    	} else if (type.equals(R.SOLVER)) {
    		findUnnecessaryParams(allowedCopySolverParams,commandParams);

    	} else if (type.equals("benchmark") || type.equals(R.JOB)) {
    		findUnnecessaryParams(allowedCopyBenchmarkParams,commandParams);
    	}
    	
    	return 0;
    }
    
    
	/**
	 * Checks to see if the parameters given by the user comprise a valid download request
	 * @param commandParams Params given by the user
	 * @param type The type of the download (R.SOLVER, "benchmark", or so on)
	 * @param since If the download type is new job output, since is the completion ID to retrieve results after
	 * @return A status code as defined in Status.java
	 */
	public static int isValidDownloadRequest(HashMap<String,String>commandParams,String type,Integer since) {
		if (!paramsExist(new String[]{C.PARAM_ID,C.PARAM_OUTPUT_FILE},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		String outputLocale=commandParams.get(C.PARAM_OUTPUT_FILE);
		if (outputLocale==null) {
			return Status.ERROR_INVALID_FILEPATH;
		}
		
		if (!type.equals(R.JOB_OUTPUTS)) {
			if (!Validator.isValidLong(commandParams.get(C.PARAM_ID))) {
				return Status.ERROR_INVALID_ID;
			} 
		} else {
			if (!Validator.isValidPosIntegerList(commandParams.get(C.PARAM_ID))) {
				return Status.ERROR_INVALID_ID;

			}
		}
		if (commandParams.containsKey(C.PARAM_PROCID)) {
			if (!Validator.isValidLong(commandParams.get(C.PARAM_PROCID))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		
		//if the file exists already, make sure the user explicitly wants to overwrite the existing file
		File testFile=new File(outputLocale);
		
		if (testFile.exists()) {
			if (!commandParams.containsKey(C.PARAM_OVERWRITE)) {
				return Status.ERROR_FILE_EXISTS;
			}
		}
		if (type.equals(R.JOB)) {
			findUnnecessaryParams(allowedDownloadCSVParams,commandParams);
		} else if  (type.equals(R.SPACE)) {
			findUnnecessaryParams(allowedDownloadSpaceParams,commandParams);
		}  else if (type.equals(R.SPACE_XML)) {
			findUnnecessaryParams(allowedDownloadSpaceXMLParams,commandParams);
		}
		else {
			if (since==null) {
				findUnnecessaryParams(allowedDownloadParams,commandParams);

			} else {
				findUnnecessaryParams(allowedNewDownloadParams,commandParams);

			}
		}
		
		return 0;
	}
	/**
	 * This function checks all the properties that are common to all uploads. All uploads must specify an id,
	 * and either a filepath or a url
	 * @param commandParams A HashMap mapping String keys to String values
	 * @return 0  if the upload request has all the basic requirements, and a negative error code otherwise.
	 * @author Eric Burns
	 * @param archiveRequired If true, file given by user must be a valid archive type (zip,tar, tgz)
	 */
	private static int isValidUploadRequest(HashMap<String,String> commandParams, boolean archiveRequired) {
		
		//an ID and either a URL or a file is required for every upload
		if (! paramsExist(new String[]{C.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!commandParams.containsKey(C.PARAM_FILE) && !commandParams.containsKey(C.PARAM_URL)) {
			missingParam=C.PARAM_FILE+" or "+C.PARAM_URL;
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		
		//if both a file and a url is specified, the upload is ambiguous-- only one or the other should be present
		if (commandParams.containsKey(C.PARAM_FILE) && commandParams.containsKey(C.PARAM_URL)) {
			return Status.ERROR_FILE_AND_URL;
		}
		
		//if a file is specified (and it might not be if a URL is used), make sure that it 
		//exists and that it is of a valid extension
		if (commandParams.containsKey(C.PARAM_FILE)) {
			String filePath=commandParams.get(C.PARAM_FILE);
			File test=new File(commandParams.get(C.PARAM_FILE));
			boolean archiveGood=false;
			if (!test.exists()) {
				return Status.ERROR_FILE_NOT_FOUND;
			}
			for (String suffix : VALID_ARCHIVETYPES) {
				if (filePath.endsWith(suffix)) {
					archiveGood=true;
					break;
				}
			}
			if (!archiveGood && archiveRequired) {
				return Status.ERROR_BAD_ARCHIVETYPE;
			}
		}
		
		//if a description file is specified, make sure it exists
		if (commandParams.containsKey(C.PARAM_DESCRIPTION_FILE)) {
			File test=new File(commandParams.get(C.PARAM_FILE));
			if (!test.exists()) {
				return Status.ERROR_FILE_NOT_FOUND;
			}
		}
		return 0;
	}
	
	/**
	 * Validates an upload in the same way as isValidUploadRequest, except that it ensures that only files
	 * and not URLs are allowed
	 * @param commandParams The key value pairs given by the user at the command line
	 * @param archiveRequired If true, file given by user must be a valid archive type (zip,tar, tgz)

	 * @return 0 if the request is valid and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	private static int isValidUploadRequestNoURL(HashMap<String,String> commandParams, boolean archiveRequired) {
		int valid=isValidUploadRequest(commandParams, archiveRequired);
		if (valid<0) {
			return valid;
		}
		
		//if no file exists, it must mean that only a url was specified
		if (!commandParams.containsKey(C.PARAM_FILE)) {
			return Status.ERROR_URL_NOT_ALLOWED;
		}
		
		return 0;
	}
	
	/**
	 * Validates a request to upload an archive of benchmarks
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	
	public static int isValidUploadBenchmarkRequest(HashMap<String,String> commandParams) {
		int valid=isValidUploadRequest(commandParams, true);
		if (valid<0) {
			return valid;
		}
		
		
		if (!paramsExist(new String[]{C.PARAM_BENCHTYPE},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_BENCHTYPE))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedUploadBenchmarksParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a solver upload request 
	 * @param commandParams A HashMap mapping String keys to String values
	 * @return 0 if the upload request is valid, and a negative error code if it is not.
	 * @author Eric Burns
	 */
	public static int isValidSolverUploadRequest(HashMap<String,String> commandParams) {
		int valid= isValidUploadRequest(commandParams, true);
		if (valid<0) {
			return valid;
		}
		if (commandParams.containsKey(C.PARAM_TYPE)) {
			if (!Validator.isValidLong(commandParams.get(C.PARAM_TYPE))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		findUnnecessaryParams(allowedUploadSolverParams,commandParams);
		return 0;
		
	}
	

	/**
	 * Validates a request to upload an archive containing a an XML file
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 * @author Julio Cervantes
	 */
	public static int isValidUploadXMLRequest(HashMap<String,String> commandParams) {
		int valid= isValidUploadRequestNoURL(commandParams, true);
		if (valid<0) {
			return valid;
		}
		findUnnecessaryParams(allowedUploadXMLParams,commandParams);
		return 0;
		
	}
	

	
	/**
	 * Validates a request to upload a configuration
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 * 
	 */
	
	public static int isValidUploadConfigRequest(HashMap<String,String> commandParams) {
		int valid= isValidUploadRequestNoURL(commandParams,false);
		if (valid<0) {
			return valid;
		}
		findUnnecessaryParams(allowedUploadConfigParams,commandParams);
		return 0;
		
	}
	
	/**
	 * Validates a request to upload either a benchmark or post processor
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	
	public static int isValidUploadProcessorRequest(HashMap<String,String> commandParams) {
		int valid= isValidUploadRequestNoURL(commandParams,true);
		if (valid<0) {
			return valid;
		}
		findUnnecessaryParams(allowedUploadProcessorParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a request to list the primitives in a space
	 * @param urlParams Additional parameters to include in the URL that will be sent to the server (includes type)
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	public static int isValidGetPrimRequest(HashMap<String,String> urlParams,HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_ID},commandParams) && !paramsExist(new String[]{C.PARAM_USER},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (commandParams.containsKey(C.PARAM_ID)) {
			if (commandParams.containsKey(C.PARAM_USER)) {
				return Status.ERROR_ID_AND_USER;
			}
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID))) {
				return Status.ERROR_INVALID_ID;
			}
			
		} else {
			String type=urlParams.get(C.FORMPARAM_TYPE);
			if (!type.equals("jobs") && !type.equals("benchmarks") &&!type.equals("solvers")) {
				return Status.ERROR_NO_USER_PRIMS;
			}
		}
		
		findUnnecessaryParams(allowedLSParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a request to sleep for some amount of time
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	
	public static int isValidSleepCommand(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[] {C.PARAM_TIME},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!Validator.isValidPosDouble(commandParams.get(C.PARAM_TIME))) {
			return Status.ERROR_BAD_TIME;
		}
		findUnnecessaryParams(allowedSleepParams,commandParams);
		return 0;
	}
	
	/**
	 * Determines whether the given parameters form a valid job creation request
	 * @param commandParams A HashMap of key/value pairs indicating values given by the user at the command line
	 * @return 0 if the request is valid and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	public static int isValidCreateJobRequest(HashMap<String,String> commandParams) {
		//Job creation must include a space ID, a processor ID, and a queue ID
		if (! paramsExist(new String[]{C.PARAM_ID,C.PARAM_QUEUEID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (commandParams.containsKey(C.PARAM_TRAVERSAL)) {
			String traversalMethod=commandParams.get(C.PARAM_TRAVERSAL);
			if (!traversalMethod.equals(C.ARG_ROUNDROBIN) && !traversalMethod.equals(C.ARG_DEPTHFIRST)) {
				return Status.ERROR_BAD_TRAVERSAL_TYPE;
			}
		}
		
		if (commandParams.containsKey(C.PARAM_SEED)) {
			if (!Validator.isValidLong(commandParams.get(C.PARAM_SEED))) {
				return Status.ERROR_SEED;
			}
		}
		
		//all IDs should be integers greater than 0
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID)) ||
				!Validator.isValidPosInteger(commandParams.get(C.PARAM_QUEUEID))) {
			return Status.ERROR_INVALID_ID;
		}
		if (commandParams.containsKey(C.PARAM_PROCID)) {
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_PROCID))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		if (commandParams.containsKey(C.PARAM_PREPROCID)) {
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_PREPROCID))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		
		//timeouts should also be integers greater than 0
		if (commandParams.containsKey(C.PARAM_CPUTIMEOUT)) {
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_CPUTIMEOUT))) {
				return Status.ERROR_INVALID_TIMEOUT;
			}
		}
		
		if (commandParams.containsKey(C.PARAM_WALLCLOCKTIMEOUT)) {
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_WALLCLOCKTIMEOUT))) {
				return Status.ERROR_INVALID_TIMEOUT;
			}
		}
		if (commandParams.containsKey(C.PARAM_MEMORY)) {
			if (!Validator.isValidPosDouble(commandParams.get(C.PARAM_MEMORY))) {
				return Status.ERROR_INVALID_MEMORY;
			}
		}
		if (commandParams.containsKey(C.PARAM_RESULTS_INTERVAL)) {
			if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_RESULTS_INTERVAL))) {
				return Status.ERROR_INVALID_RESULTS_INTERVAL;
			}
		}
		findUnnecessaryParams(allowedCreateJobParams,commandParams);
		return 0;
	}
	
	
	
	/**
	 * This function determines whether a given subspace creation request is valid
	 * @param commandParams The key/value pairs given by the user at the command line
	 * @return 0 if the request is valid and a negative error code if not
	 * @author Eric Burns
	 */
	public static int isValidCreateSubspaceRequest(HashMap<String,String> commandParams) {
		
		if (! paramsExist(new String[]{C.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}

		findUnnecessaryParams(allowedCreateSubspaceParams,commandParams);
		return 0;
	}
	
	/**
	 * This function determines whether a given request to set a subspace to public or private is valid
	 * @param commandParams The key/value pairs given by the user at the command line
	 * @return 0 if the request is valid and a negative error code if not
	 * @author Eric Burns
	 */
	public static int isValidSetSpaceVisibilityRequest(HashMap<String,String> commandParams) {
		if (! paramsExist(new String[]{C.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedSetSpaceVisibilityParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates an incoming request to log into starexec
	 * @param commandParams The user-provided arguments
	 * @return A status code as defined in Status.java
	 */
	public static int isValidLoginRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_USER},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!commandParams.get(C.PARAM_USER).equals(C.PARAM_GUEST)) {
			if (!paramsExist(new String[] {C.PARAM_PASSWORD},commandParams)) {
				return Status.ERROR_MISSING_PARAM;
			}
		}
		findUnnecessaryParams(allowedLoginParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates an incoming request to pause or resume a job
	 * @param commandParams The user-provided arguments
	 * @return A status code as defined in Status.java
	 */
	public static int isValidPauseOrResumeRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_ID}, commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedPauseOrResumeParams,commandParams);
		return 0;
		
	}
	
	/**
	 * Validates an incoming request to rerun a pair
	 * @param commandParams The user-provided arguments
	 * @return A status code as defined in Status.java
	 */
	public static int isValidRerunRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_ID}, commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!Validator.isValidPosInteger(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedRerunParams,commandParams);
		return 0;
		
	}
	
	/**
	 * Validates a request to poll the results of a job
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	public static int isValidPollJobRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[] {C.PARAM_ID,C.PARAM_OUTPUT_FILE,C.PARAM_TIME}, commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!Validator.isValidPosDouble(commandParams.get(C.PARAM_TIME))) {
			return Status.ERROR_BAD_TIME;
		}
		
		String outputLocale=commandParams.get(C.PARAM_OUTPUT_FILE);
		if (outputLocale==null) {
			return Status.ERROR_INVALID_FILEPATH;
		}
		
		//if the file exists already, make sure the user explicitly wants to overwrite the existing file
		File testFile=new File(outputLocale);
		
		if (testFile.exists()) {
			if (!commandParams.containsKey(C.PARAM_OVERWRITE)) {
				return Status.ERROR_FILE_EXISTS;
			}
		}
		
		findUnnecessaryParams(allowedPollJobParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a request to run a file of commands
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	public static int isValidRunFileRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[] {C.PARAM_FILE},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		File file=new File(commandParams.get(C.PARAM_FILE));
		if (!file.exists()) {
			return Status.ERROR_FILE_NOT_FOUND;
		}
		findUnnecessaryParams(allowedRunFileParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a request to change a user setting
	 * @param setting The setting that is going to be changed
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	public static int isValidSetUserSettingRequest(String setting, HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_VAL},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		String newVal=commandParams.get(C.PARAM_VAL);

		if (newVal==null) {
			missingParam=C.PARAM_VAL;
			return Status.ERROR_MISSING_PARAM;
		}

		findUnnecessaryParams(allowedSetUserSettingParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a request to print out a benchmark upload status summary string
	 * @param commandParams The arguments to validate
	 * @return A status code as defined in Status.java
	 */
	public static int isValidPrintBenchUploadStatusRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if(!Validator.isValidLong(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		
		findUnnecessaryParams(allowedPrintStatusParams,commandParams);
		
		return 0;
	}
	
	/**
	 * Validates a request to get a jSON-encoded primitive from Starexec
 	 * @param commandParams The arguments to validate
	 * @return A status code as defined in Status
	 */
	public static int isValidGetPrimitiveAttributesRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{C.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if(!Validator.isValidLong(commandParams.get(C.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		
		findUnnecessaryParams(allowedGetPrimitiveAttributesParams,commandParams);
		
		return 0;
	}
	
	/**
	 * Returns true if the user has specified all the required parameters and false otherwise. If false,
	 * set one missing parameter in the missingParam field.
	 * @param params The required parameters
	 * @param commandParams The parameters given by the user
	 * @return True if all the required parameters are present, and false otherwise
	 */
	private static boolean paramsExist(String[] params, HashMap<String,String> commandParams) {
		for (String param : params) {
			if (!commandParams.containsKey(param)) {
				missingParam=param;
				return false;
			}
		}
		missingParam=null;
		return true;
	}
	
	
	
	/**
	 * Finds all the parameters the user specified that were unnecessary and sets them in the unnecessaryParameters
	 * list, replacing any that were there previously
	 * @param allowedParams The parameters that are expected
	 * @param commandParams The parameters the user gave
	 */
	private static void findUnnecessaryParams(String [] allowedParams, HashMap<String,String> commandParams) {
		List<String> a=Arrays.asList(allowedParams);
		unnecessaryParams=new ArrayList<String>();
		for (String x : commandParams.keySet()) {
			
			if (!a.contains(x)) {
				
				unnecessaryParams.add(x);
			}
		}
	}
	
	/**
	 * @return A list of parameters that were not usable by the command that was last validated.
	 */
	public static List<String> getUnnecessaryParams() {
		return unnecessaryParams;
	}
	
	/**
	 * Attempts to parse the given string as an integer and return it. On failure, returns -1
	 * @param str The string to use
	 * @return The numeric value of the string, or -1 if the string is not a number
	 */
	public static Integer getIdOrMinusOne(String str) {
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return -1;
		}
	}
	
	
	
	/**
	 * Checks to see if the given zip file is valid, meaning that it is not corrupted, is actually a ZIP, etc.
	 * @param file The file to check
	 * @return True if the file is a valid zip-formatted file.
	 */
	 public static boolean isValidZip(File file) {
		    ZipFile zipfile = null;
		    try {
		        zipfile = new ZipFile(file);
		        return true;
		    } catch (Exception e) {
		        return false;
		    } finally {
		        try {
		            if (zipfile != null) {
		                zipfile.close();
		                zipfile = null;
		            }
		        } catch (IOException e) {
		        }
		    }
		}
	
}
