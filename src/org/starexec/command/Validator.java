package org.starexec.command;

/**
 * This class is responsible for validating the arguments given to functions in a Connection
 */


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipFile;

public class Validator {
	
	public static String[] VALID_ARCHIVETYPES={"zip"};
	public static Pattern patternBoolean = Pattern.compile(R.BOOLEAN_PATTERN, Pattern.CASE_INSENSITIVE);										
	public static Pattern patternInteger = Pattern.compile(R.LONG_PATTERN);
	public static Pattern patternUserName = Pattern.compile(R.USER_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
	public static Pattern patternInstitution = Pattern.compile(R.INSTITUTION_PATTERN, Pattern.CASE_INSENSITIVE);
	public static Pattern patternEmail = Pattern.compile(R.EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
	public static Pattern patternUrl = Pattern.compile(R.URL_PATTERN, Pattern.CASE_INSENSITIVE);
	public static Pattern patternPrimName = Pattern.compile(R.PRIMITIVE_NAME_PATTERN, Pattern.CASE_INSENSITIVE);
	public static Pattern patternPrimDesc = Pattern.compile(R.PRIMITIVE_DESC_PATTERN, Pattern.DOTALL);
	public static Pattern patternPassword = Pattern.compile(R.PASSWORD_PATTERN);
	public static Pattern patternRequestMsg = Pattern.compile(R.REQUEST_MESSAGE, Pattern.CASE_INSENSITIVE);
	
	private static String missingParam=null;
	private static List<String> unnecessaryParams=new ArrayList<String>();
	
	//the following lists specify the parameters, either required or optional, that are accepted by a certain
	//command or set of commands
	private static String[] allowedRemoveParams=new String[]{R.PARAM_ID,R.PARAM_FROM};
	private static String[] allowedDownloadParams=new String[]{R.PARAM_ID,R.PARAM_OUTPUT_FILE,R.PARAM_OVERWRITE};
	private static String[] allowedDownloadSpaceXMLParams=new String[]{R.PARAM_ID,R.PARAM_OUTPUT_FILE,R.PARAM_OVERWRITE,R.PARAM_GET_ATTRIBUTES,R.PARAM_PROCID};

	private static String[] allowedNewDownloadParams=new String[]{R.PARAM_ID,R.PARAM_OUTPUT_FILE,R.PARAM_OVERWRITE,R.PARAM_SINCE};
	private static String[] allowedDownloadSpaceParams=new String[]{R.PARAM_ID,R.PARAM_OUTPUT_FILE,R.PARAM_OVERWRITE,R.PARAM_EXCLUDE_BENCHMARKS,R.PARAM_EXCLUDE_SOLVERS};
	private static String[] allowedDownloadCSVParams=new String[]{R.PARAM_ID,R.PARAM_OUTPUT_FILE,R.PARAM_OVERWRITE,R.PARAM_INCLUDE_IDS, R.PARAM_ONLY_COMPLETED};
	private static String[] allowedSetUserSettingParams=new String[]{R.PARAM_VAL};
	private static String[] allowedSetSpaceVisibilityParams=new String[]{R.PARAM_ID,R.PARAM_HIERARCHY};
	private static String[] allowedLoginParams=new String[]{R.PARAM_USER,R.PARAM_PASSWORD,R.PARAM_BASEURL};
	private static String[] allowedDeleteParams=new String[]{R.PARAM_ID};
	private static String[] allowedCopyParams=new String[]{R.PARAM_ID,R.PARAM_FROM,R.PARAM_TO};
	private static String[] allowedCopyUserParams=new String[]{R.PARAM_TO,R.PARAM_ID};
	private static String[] allowedCopySpaceParams=new String[]{R.PARAM_TO,R.PARAM_ID, R.PARAM_FROM};

    private static String[] allowedCopySolverParams=new String[]{R.PARAM_ID,R.PARAM_FROM,R.PARAM_TO,R.PARAM_HIERARCHY};
    private static String[] allowedCopyBenchmarkParams=new String[]{R.PARAM_ID,R.PARAM_FROM,R.PARAM_TO};
	private static String[] allowedPollJobParams=new String[]{R.PARAM_OUTPUT_FILE,R.PARAM_ID,R.PARAM_TIME,R.PARAM_OVERWRITE};
	private static String[] allowedRunFileParams=new String[]{R.PARAM_FILE,R.PARAM_VERBOSE};
	private static String[] allowedSleepParams=new String[]{R.PARAM_TIME};
	private static String[] allowedPauseOrResumeParams=new String[]{R.PARAM_ID};
	private static String[] allowedRerunParams=new String[]{R.PARAM_ID};

	private static String[] allowedCreateSubspaceParams=new String[]{R.PARAM_ID,R.PARAM_NAME,R.PARAM_DESC,
		R.PARAM_ENABLE_ALL_PERMISSIONS,"addSolver","addUser","addSpace","addJob","addBench","removeSolver","removeUser","removeSpace","removeJob","removeBench"};
	private static String[] allowedCreateJobParams=new String[]{R.PARAM_ID,R.PARAM_NAME,R.PARAM_DESC,R.PARAM_WALLCLOCKTIMEOUT,
		R.PARAM_CPUTIMEOUT,R.PARAM_QUEUEID,R.PARAM_PROCID, R.PARAM_TRAVERSAL, R.PARAM_MEMORY,R.PARAM_PAUSED, R.PARAM_SEED};
	private static String[] allowedUploadSolverParams=new String[]{R.PARAM_ID,R.PARAM_TYPE,R.PARAM_PREPROCID,R.PARAM_FILE,R.PARAM_URL,R.PARAM_NAME,R.PARAM_DESC,
		R.PARAM_DESCRIPTION_FILE,R.PARAM_DOWNLOADABLE, R.PARAM_RUN, R.PARAM_SETTING};
	private static String[] allowedUploadBenchmarksParams= new String[] {R.PARAM_ID,R.PARAM_BENCHTYPE, R.PARAM_FILE,R.PARAM_URL,
		R.PARAM_DESC,R.PARAM_DESCRIPTION_FILE,R.PARAM_DEPENDENCY,R.PARAM_DOWNLOADABLE,R.PARAM_HIERARCHY,R.PARAM_LINKED,
		R.PARAM_ENABLE_ALL_PERMISSIONS,"addSolver","addUser","addSpace","addJob","addBench","removeSolver","removeUser","removeSpace","removeJob","removeBench"};
	private static String[] allowedUploadProcessorParams=new String[]{R.PARAM_ID,R.PARAM_NAME,R.PARAM_DESC,R.PARAM_FILE};
	private static String[] allowedUploadConfigParams=new String[] {R.PARAM_FILE,R.PARAM_ID,R.PARAM_FILE,R.PARAM_DESC};
    private static String[] allowedUploadXMLParams=new String[]{R.PARAM_ID,R.PARAM_FILE};
    private static String[] allowedPrintStatusParams=new String[]{R.PARAM_ID};
    private static String[] allowedGetPrimitiveAttributesParams=new String[]{R.PARAM_ID};

	private static String[] allowedLSParams=new String[]{R.PARAM_ID,R.PARAM_LIMIT,R.PARAM_USER}; 
	
	/**
	 * Gets the missing paramter that was last seen. If none has been seen yet, returns null.
	 * @return The name of the required parameter that is missing. 
	 */
	public static String getMissingParam() {
		return missingParam;
	}
	
	
	public static int isValidRemoveRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{R.PARAM_ID,R.PARAM_FROM},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!isValidPosIntegerList(commandParams.get(R.PARAM_ID))
				|| !isValidPosInteger(commandParams.get(R.PARAM_FROM))) {
			return Status.ERROR_INVALID_ID;
		}
		
		findUnnecessaryParams(allowedRemoveParams,commandParams);
		return 0;
	}
	
	/**
	 * Validates a request to delete a primitive
	 * @param commandParams The parameters given by the user
	 * @return 0 if the request is valid and a negative error code otherwise
	 */
	
	public static int isValidDeleteRequest(HashMap<String,String> commandParams) {
		if (! paramsExist(new String[]{R.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!isValidPosIntegerList(commandParams.get(R.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedDeleteParams,commandParams);
		return 0;
	}
	
	public static boolean isValidInteger(String str) {
		try {
			Long.parseLong(str);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines whether the given string represents a valid id. Valid ids are integers greater than or equal to 0
	 * @param str The string to check
	 * @return True if valid, false otherwise.
	 * @author Eric Burns
	 */
	
	public static boolean isValidPosInteger(String str) {
		try {
			int check=Integer.parseInt(str);
			if (check<0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines whether the given string represents a valid double that is greater than or equal to 0
	 * @param str The string to check
	 * @return True if valid, false otherwise.
	 * @author Eric Burns
	 */
	
	public static boolean isValidPosDouble(String str) {
		try {
			double check=Double.parseDouble(str);
			if (check<0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines whether the given string represents a valid real number greater than or equal to 0
	 * @param str The string to check
	 * @return True if the string is valid and false otherwise
	 */
	public static boolean isValidPosNumber(String str) {
		try {
			double check=Double.parseDouble(str);
			if (check<0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isValidPrimName(String name){    	
    	return patternPrimName.matcher(name).matches();    	
    }
    
    /**
     * Validates a boolean value by ensuring it is something Boolean.parseBoolean()
     * can handle
     * 
     * @param boolString the string to check for a parse-able boolean value
     * @return true iff boolString isn't null and is either "true" or "false"
     */
    public static boolean isValidBool(String boolString){
    	return patternBoolean.matcher(boolString).matches();
    }
    
    /**
	 * Validates an institution field
	 * 
	 * @param institute the institution to validate
	 * @return true iff institute is less than R.INSTITUTION_LEN characters 
	 * and not null or the empty string
	 */
	public static boolean isValidInstitution(String institute){		
		return patternInstitution.matcher(institute).matches();		
	}
    
    /**
     * Validates a generic description and checks that it contains content and is less than 1024
     * characters long. ALL characters are allowed in descriptions.
     * 
     * @param desc the description to check
     * @return true iff name isn't null or empty and is less than 1024 characters
     */
    public static boolean isValidPrimDescription(String desc){
    	return patternPrimDesc.matcher(desc).matches();
    }
    
    /**
     * Determines whether the given string is a valid comma-separated list of positive integers
     * @param ids The string to check
     * @return True if the string is a comma-separated list of positive integers, false otherwise
     */
    
    public static boolean isValidPosIntegerList(String ids) {
    	String[] idArray=ids.split(",");
    	for (String id : idArray) {
    		if (!isValidPosInteger(id)) {
    			return false;
    		}
    	}
    	
    	return true;
    }
    
    /**
     * Validates the given parameters to determine if they can be used to construct a valid
     * request to copy primitives on StarExec
     * @param commandParams A set of key/value parameters
     * @param type The type of primitive (solver, benchmark, etc.) being copied
     * @return 0 if the request is valid, and a negative error code otherwise
     */
	
    public static int isValidCopyRequest(HashMap<String,String> commandParams, String type) {
    	if (!paramsExist(new String[]{R.PARAM_ID,R.PARAM_TO},commandParams)) {
    		return Status.ERROR_MISSING_PARAM;
    	}
    	
    	if (!isValidPosIntegerList(commandParams.get(R.PARAM_ID)) 
    			|| (commandParams.containsKey(R.PARAM_FROM) && !isValidPosInteger(commandParams.get(R.PARAM_FROM)))
    			|| !isValidPosInteger(commandParams.get(R.PARAM_TO))) {
    		return Status.ERROR_INVALID_ID;
    	}
    	
    	//the hierarchy parameter is also acceptable if the type is either solver or space
    	if (type.equals("user")) {
    		findUnnecessaryParams(allowedCopyUserParams,commandParams);
    	} else if (type.equals("space")) {
    		findUnnecessaryParams(allowedCopySpaceParams,commandParams);
    	} else if (type.equals("solver")) {
    		findUnnecessaryParams(allowedCopySolverParams,commandParams);

    	} else if (type.equals("benchmark") || type.equals("job")) {
    		findUnnecessaryParams(allowedCopyBenchmarkParams,commandParams);
    	}
    	
    	return 0;
    }
    
    
	/**
	 * Checks to see if the parameters given by the user comprise a valid download request
	 * @param commandParams Params given by the user
	 * @param type The type of the download ("solver", "benchmark", or so on)
	 * @param since If the download type is new job output, since is the completion ID to retrieve results after
	 * @return
	 */
	public static int isValidDownloadRequest(HashMap<String,String>commandParams,String type,Integer since) {
		if (!paramsExist(new String[]{R.PARAM_ID,R.PARAM_OUTPUT_FILE},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		String outputLocale=commandParams.get(R.PARAM_OUTPUT_FILE);
		if (outputLocale==null) {
			return Status.ERROR_INVALID_FILEPATH;
		}
		
		if (!type.equals("jp_outputs")) {
			if (!Validator.isValidInteger(commandParams.get(R.PARAM_ID))) {
				return Status.ERROR_INVALID_ID;
			} 
		} else {
			if (!Validator.isValidPosIntegerList(commandParams.get(R.PARAM_ID))) {
				return Status.ERROR_INVALID_ID;

			}
		}
		if (commandParams.containsKey(R.PARAM_PROCID)) {
			if (!Validator.isValidInteger(commandParams.get(R.PARAM_PROCID))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		
		//if the file exists already, make sure the user explicitly wants to overwrite the existing file
		File testFile=new File(outputLocale);
		
		if (testFile.exists()) {
			if (!commandParams.containsKey(R.PARAM_OVERWRITE)) {
				return Status.ERROR_FILE_EXISTS;
			}
		}
		if (type.equals("job")) {
			findUnnecessaryParams(allowedDownloadCSVParams,commandParams);
		} else if  (type.equals("space")) {
			findUnnecessaryParams(allowedDownloadSpaceParams,commandParams);
		}  else if (type.equals("spaceXML")) {
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
		if (! paramsExist(new String[]{R.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!commandParams.containsKey(R.PARAM_FILE) && !commandParams.containsKey(R.PARAM_URL)) {
			missingParam=R.PARAM_FILE+" or "+R.PARAM_URL;
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!isValidPosInteger(commandParams.get(R.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		
		//if both a file and a url is specified, the upload is ambiguous-- only one or the other should be present
		if (commandParams.containsKey(R.PARAM_FILE) && commandParams.containsKey(R.PARAM_URL)) {
			return Status.ERROR_FILE_AND_URL;
		}
		
		//if a file is specified (and it might not be if a URL is used), make sure that it 
		//exists and that it is of a valid extension
		if (commandParams.containsKey(R.PARAM_FILE)) {
			String filePath=commandParams.get(R.PARAM_FILE);
			File test=new File(commandParams.get(R.PARAM_FILE));
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
		if (commandParams.containsKey(R.PARAM_DESCRIPTION_FILE)) {
			File test=new File(commandParams.get(R.PARAM_FILE));
			if (!test.exists()) {
				return Status.ERROR_FILE_NOT_FOUND;
			}
		}
		
		//if a name is specified, it must conform to StarExec rules
		if (commandParams.containsKey(R.PARAM_NAME)) {
			if (!isValidPrimName(commandParams.get(R.PARAM_NAME))) {
				return Status.ERROR_BAD_NAME;
			}
		}
		
		//if a description is specified, it must conform to StarExec rules
		if (commandParams.containsKey(R.PARAM_DESC)) {
			if (!isValidPrimDescription(commandParams.get(R.PARAM_DESC))) {
				return Status.ERROR_BAD_DESCRIPTION;
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
		if (!commandParams.containsKey(R.PARAM_FILE)) {
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
		
		
		if (!paramsExist(new String[]{R.PARAM_BENCHTYPE},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!isValidPosInteger(commandParams.get(R.PARAM_BENCHTYPE))) {
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
		if (commandParams.containsKey(R.PARAM_TYPE)) {
			if (!Validator.isValidInteger(commandParams.get(R.PARAM_TYPE))) {
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
		if (!paramsExist(new String[]{R.PARAM_ID},commandParams) && !paramsExist(new String[]{R.PARAM_USER},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (commandParams.containsKey(R.PARAM_ID)) {
			if (commandParams.containsKey(R.PARAM_USER)) {
				return Status.ERROR_ID_AND_USER;
			}
			if (!isValidPosInteger(commandParams.get(R.PARAM_ID))) {
				return Status.ERROR_INVALID_ID;
			}
			
		} else {
			String type=urlParams.get(R.FORMPARAM_TYPE);
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
		if (!paramsExist(new String[] {R.PARAM_TIME},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!isValidPosNumber(commandParams.get(R.PARAM_TIME))) {
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
		if (! paramsExist(new String[]{R.PARAM_ID,R.PARAM_QUEUEID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (commandParams.containsKey(R.PARAM_TRAVERSAL)) {
			String traversalMethod=commandParams.get(R.PARAM_TRAVERSAL);
			if (!traversalMethod.equals(R.ARG_ROUNDROBIN) && !traversalMethod.equals(R.ARG_DEPTHFIRST)) {
				return Status.ERROR_BAD_TRAVERSAL_TYPE;
			}
		}
		
		if (commandParams.containsKey(R.PARAM_SEED)) {
			if (!isValidInteger(commandParams.get(R.PARAM_SEED))) {
				return Status.ERROR_SEED;
			}
		}
		
		//all IDs should be integers greater than 0
		if (!isValidPosInteger(commandParams.get(R.PARAM_ID)) ||
				!isValidPosInteger(commandParams.get(R.PARAM_QUEUEID))) {
			return Status.ERROR_INVALID_ID;
		}
		if (commandParams.containsKey(R.PARAM_PROCID)) {
			if (!isValidPosInteger(commandParams.get(R.PARAM_PROCID))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		if (commandParams.containsKey(R.PARAM_PREPROCID)) {
			if (!isValidPosInteger(commandParams.get(R.PARAM_PREPROCID))) {
				return Status.ERROR_INVALID_ID;
			}
		}
		
		//timeouts should also be integers greater than 0
		if (commandParams.containsKey(R.PARAM_CPUTIMEOUT)) {
			if (!isValidPosInteger(commandParams.get(R.PARAM_CPUTIMEOUT))) {
				return Status.ERROR_INVALID_TIMEOUT;
			}
		}
		
		if (commandParams.containsKey(R.PARAM_WALLCLOCKTIMEOUT)) {
			if (!isValidPosInteger(commandParams.get(R.PARAM_WALLCLOCKTIMEOUT))) {
				return Status.ERROR_INVALID_TIMEOUT;
			}
		}
		if (commandParams.containsKey(R.PARAM_MEMORY)) {
			if (!isValidPosDouble(commandParams.get(R.PARAM_MEMORY))) {
				return Status.ERROR_INVALID_MEMORY;
			}
		}
		
		if (commandParams.containsKey(R.PARAM_NAME)) {
			if (!isValidPrimName(commandParams.get(R.PARAM_NAME))) {
				return Status.ERROR_BAD_NAME;
			}
		}
		if (commandParams.containsKey(R.PARAM_DESC)) {
			if (!isValidPrimDescription(commandParams.get(R.PARAM_DESC))) {
				return Status.ERROR_BAD_DESCRIPTION;
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
		
		if (! paramsExist(new String[]{R.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		if (!isValidPosInteger(commandParams.get(R.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		
		if (commandParams.containsKey(R.PARAM_NAME)) {
			if (!isValidPrimName(commandParams.get(R.PARAM_NAME))) {
				return Status.ERROR_BAD_NAME;
			}
		}
		if (commandParams.containsKey(R.PARAM_DESC)) {
			if (!isValidPrimDescription(commandParams.get(R.PARAM_DESC))) {
				return Status.ERROR_BAD_DESCRIPTION;
			}
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
		if (! paramsExist(new String[]{R.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!isValidPosInteger(commandParams.get(R.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedSetSpaceVisibilityParams,commandParams);
		return 0;
	}
	
	public static int isValidLoginRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{R.PARAM_USER},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!commandParams.get(R.PARAM_USER).equals(R.PARAM_GUEST)) {
			if (!paramsExist(new String[] {R.PARAM_PASSWORD},commandParams)) {
				return Status.ERROR_MISSING_PARAM;
			}
		}
		findUnnecessaryParams(allowedLoginParams,commandParams);
		return 0;
	}
	
	public static int isValidPauseOrResumeRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{R.PARAM_ID}, commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!isValidPosInteger(commandParams.get(R.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		findUnnecessaryParams(allowedPauseOrResumeParams,commandParams);
		return 0;
		
	}
	
	public static int isValidRerunRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{R.PARAM_ID}, commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!isValidPosInteger(commandParams.get(R.PARAM_ID))) {
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
		if (!paramsExist(new String[] {R.PARAM_ID,R.PARAM_OUTPUT_FILE,R.PARAM_TIME}, commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (!isValidPosNumber(commandParams.get(R.PARAM_TIME))) {
			return Status.ERROR_BAD_TIME;
		}
		
		String outputLocale=commandParams.get(R.PARAM_OUTPUT_FILE);
		if (outputLocale==null) {
			return Status.ERROR_INVALID_FILEPATH;
		}
		
		//if the file exists already, make sure the user explicitly wants to overwrite the existing file
		File testFile=new File(outputLocale);
		
		if (testFile.exists()) {
			if (!commandParams.containsKey(R.PARAM_OVERWRITE)) {
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
		if (!paramsExist(new String[] {R.PARAM_FILE},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		File file=new File(commandParams.get(R.PARAM_FILE));
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
		if (!paramsExist(new String[]{R.PARAM_VAL},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		String newVal=commandParams.get(R.PARAM_VAL);

		if (newVal==null) {
			missingParam=R.PARAM_VAL;
			return Status.ERROR_MISSING_PARAM;
		}
		
		if (setting.equals("firstname")|| setting.equals("lastname")) {
			if(!isValidPrimName(newVal)){
				return Status.ERROR_BAD_NAME;
			}
		} else if (setting.equals("institution")) {
			if (!isValidInstitution(newVal)) {
				return Status.ERROR_BAD_INSTITUTION;
			}
		}
		
		findUnnecessaryParams(allowedSetUserSettingParams,commandParams);
		return 0;
	}
	
	public static int isValidPrintBenchUploadStatusRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{R.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if(!Validator.isValidInteger(commandParams.get(R.PARAM_ID))) {
			return Status.ERROR_INVALID_ID;
		}
		
		findUnnecessaryParams(allowedPrintStatusParams,commandParams);
		
		return 0;
	}
	
	public static int isValidGetPrimitiveAttributesRequest(HashMap<String,String> commandParams) {
		if (!paramsExist(new String[]{R.PARAM_ID},commandParams)) {
			return Status.ERROR_MISSING_PARAM;
		}
		
		if(!Validator.isValidInteger(commandParams.get(R.PARAM_ID))) {
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
	 * @return A list of parameters that were not usable by the command that was last validated
	 */
	public static List<String> getUnnecessaryParams() {
		return unnecessaryParams;
	}
	
	/**
	 * Attempts to parse the given string as an integer and return it. On failure, returns -1
	 * @param str
	 * @return
	 */
	public static Integer getIdOrMinusOne(String str) {
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return -1;
		}
	}
	
	
	
	/**
	 *Checks to see if the given zip file is valid
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
