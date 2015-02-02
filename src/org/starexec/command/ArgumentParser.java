package org.starexec.command;

/**
 * This class is responsible for communicating with the Starexec server 
 * Its functions generally take HashMap objects mapping String keys 
 * to String values and use the keys and values to create
 * HTTP GET and POST requests to StarExec
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ArgumentParser {
	
	Connection con;
	
	/**
	 * Gets the last server error message that was returned
	 * @return
	 */
	public String getLastServerError() {
		return con.getLastError();
	}
	/**
	 * Sets the new Connection object's username and password based on user-specified parameters.
	 * Also sets the instance of StarExec that is being connected to
	 * @param commandParams User specified parameters
	 */
	
	protected ArgumentParser(HashMap<String,String> commandParams) {
		String base=null;
		String username="";
		String password="";
		if (commandParams.containsKey(R.PARAM_BASEURL)) {
			base=commandParams.get(R.PARAM_BASEURL);
		} 
		if (!commandParams.get(R.PARAM_USER).equals(R.PARAM_GUEST)) {
			username=commandParams.get(R.PARAM_USER);
			
			password=commandParams.get(R.PARAM_PASSWORD);
		} else {
			username="public";
			password="public";
		}
		if (base==null) {
			con=new Connection(username,password);
		} else {
			con=new Connection(username,password,base);
		}
	}
	
	protected void refreshConnection() {
		con=new Connection(con);
	}
	
	
	/**
	 * Gets the max completion ID for info downloads on the given job.
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen for the job, or 0 if not seen
	 */
	protected int getJobInfoCompletion(int jobID) {
		return con.getJobInfoCompletion(jobID);
	}
	
	/**
	 * Gets all of the completion indices for job information (not job output)
	 * @return A map of job IDs to the last seen completion indices for those jobs. 
	 */
	
	protected HashMap<Integer,Integer> getInfoIndices() {
		return con.getInfoIndices();
	}
	
	/**
	 * Gets the max completion ID yet seen for output downloads on a given job
	 * @param jobID The ID of a job on StarExec
	 * @return The maximum completion ID seen yet, or 0 if not seen.
	 */
	
	protected int getJobOutCompletion(int jobID) {
		return con.getJobOutCompletion(jobID);
		
	}
	
	/**
	 * Gets all of the completion indices for job output (not job info)
	 * @return A map of job IDs to the last seen completion indices for those jobs. 
	 */
	
	protected HashMap<Integer,Integer> getOutIndices() {
		return con.getOutputIndices();
	}
	
	
	
	/**
	 * Gets username being used for this connection
	 * @return The username
	 */
	protected String getUsername() {
		return con.getUsername();
	}
	
	/**
	 * Gets the password being used for this connection
	 * @return The password
	 */
	protected String getPassword() {
		return con.getPassword();
	}
	
	/**
	 * Gets the home URL of the StarExec instance currently connected to
	 * @return The base URL
	 */
	protected String getBaseURL() {
		return con.getBaseURL();
	}
	
	/**
	 * Log into StarExec with the username and password of this connection
	 * @return An integer indicating status, with 0 being normal and a negative integer
	 * indicating an error
	 * @author Eric Burns
	 */
	protected int login() {
		return con.login();
	}
	
	/**
	 * Ends the current Starexec session
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	
	protected boolean logout() {
		return con.logout();
		
	}

	/**
	 * Creates a POST request to StarExec to create a new job
	 * @param commandParams A HashMap containing key/value pairs gathered from the user input at the command line
	 * @return the new job ID on success, a negative integer otherwise
	 * @author Eric Burns
	 */
	protected int createJob(HashMap<String,String> commandParams) {
		try {
			
			int valid=Validator.isValidCreateJobRequest(commandParams);
			if (valid<0) {
				return valid;
			}			
			Integer wallclock=null;
			Integer cpu=null;
			Double maxMemory=null;
			if (commandParams.containsKey(R.PARAM_WALLCLOCKTIMEOUT)) {
				wallclock=Integer.parseInt(commandParams.get(R.PARAM_WALLCLOCKTIMEOUT));
			}
			if (commandParams.containsKey(R.PARAM_CPUTIMEOUT)) {
				cpu=Integer.parseInt(commandParams.get(R.PARAM_CPUTIMEOUT));
			}
			if (commandParams.containsKey(R.PARAM_MEMORY)) {
				maxMemory=Double.parseDouble(commandParams.get(R.PARAM_MEMORY));
			}
			Boolean useDepthFirst=true;
			if (commandParams.containsKey(R.PARAM_TRAVERSAL)) {
				if (commandParams.get(R.PARAM_TRAVERSAL).equals(R.ARG_ROUNDROBIN)) {
					useDepthFirst=false;
				}
			}
			String postProcId="-1";
			String preProcId="-1";
			if (commandParams.containsKey(R.PARAM_PROCID)) {
				postProcId=commandParams.get(R.PARAM_PROCID);
			}
			if (commandParams.containsKey(R.PARAM_PREPROCID)) {
				preProcId=commandParams.get(R.PARAM_PREPROCID);
			}
						
			String name=getDefaultName("");
			String desc="";
			if (commandParams.containsKey(R.PARAM_NAME)) {
				name=commandParams.get(R.PARAM_NAME);
			}
			
			if (commandParams.containsKey(R.PARAM_DESC)) {
				desc=commandParams.get(R.PARAM_DESC);
			}
			boolean startPaused=false;
			if (commandParams.containsKey(R.PARAM_PAUSED)) {
				startPaused=true;
			}
			long seed=0;
			if (commandParams.containsKey(R.PARAM_SEED)) {
				seed=Long.parseLong(commandParams.get(R.PARAM_SEED));
			}
			return con.createJob(Integer.parseInt(commandParams.get(R.PARAM_ID)), name, desc,Integer.parseInt(postProcId),Integer.parseInt(preProcId), Integer.parseInt(commandParams.get(R.PARAM_QUEUEID)),wallclock, cpu,useDepthFirst,maxMemory,startPaused,seed);

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Sends a link request to the StarExec server and returns a status code
	 * indicating the result of the request
	 * @param commandParams The parameters given by the user at the command line.
	 * @param copy True if a copy should be performed, and false if a link should be performed.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative number is an error.
	 */
	protected int linkPrimitives(HashMap<String,String> commandParams, String type) {
		try {
			int valid=Validator.isValidCopyRequest(commandParams, type);
			if (valid<0) {
				return valid;
			}
			
			Integer[] ids=CommandParser.convertToIntArray(commandParams.get(R.PARAM_ID));
			return con.linkPrimitives(ids,Integer.parseInt(commandParams.get(R.PARAM_FROM)),Integer.parseInt(commandParams.get(R.PARAM_TO)),
					commandParams.containsKey(R.PARAM_HIERARCHY),type);
		
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}

	
	/**
	 * Sends a copy or link request to the StarExec server and returns a status code
	 * indicating the result of the request
	 * @param commandParams The parameters given by the user at the command line.
	 * @param copy True if a copy should be performed, and false if a link should be performed.
	 * @param type The type of primitive being copied.
	 * @return An integer error code where 0 indicates success and a negative number is an error.
	 */
	protected List<Integer> copyPrimitives(HashMap<String,String> commandParams, String type) {
		List<Integer> fail=new ArrayList<Integer>();
		try {
			int valid=Validator.isValidCopyRequest(commandParams, type);
			if (valid<0) {
				fail.add(valid);
				return fail;
			}
			
			Integer[] ids=CommandParser.convertToIntArray(commandParams.get(R.PARAM_ID));
			return con.copyPrimitives(ids,Integer.parseInt(commandParams.get(R.PARAM_FROM)),Integer.parseInt(commandParams.get(R.PARAM_TO)),
					commandParams.containsKey(R.PARAM_HIERARCHY),type);
		
		} catch (Exception e) {
			fail.add(Status.ERROR_INTERNAL);
			return fail;		}
	}
	
	
	/**
	 * Creates a subspace of an existing space on StarExec
	 * @param commandParam A HashMap containing key/value pairs gathered from user input at the command line
	 * @return the new space ID on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	protected int createSubspace(HashMap<String,String> commandParams) {
		try {
			int valid=Validator.isValidCreateSubspaceRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			
			String name=getDefaultName("");
			
			if (commandParams.containsKey(R.PARAM_NAME)) {
				name=commandParams.get(R.PARAM_NAME);
			}
			String desc="";
			if (commandParams.containsKey(R.PARAM_DESC)) {
				desc=commandParams.get(R.PARAM_DESC);
			}
			
			Boolean locked=false;
			if (commandParams.containsKey(R.PARAM_LOCKED)) {
				locked=true;
			}
			Permission p=new Permission(false);
			for (String x : R.PARAMS_PERMS) {
				
				if (commandParams.containsKey(x) || commandParams.containsKey(R.PARAM_ENABLE_ALL_PERMISSIONS)) {
					p.setPermissionOn(x);
				}
			}
			return con.createSubspace(name, desc, Integer.parseInt(commandParams.get(R.PARAM_ID)), p, locked);
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Removes the association between a primitive and a space on StarExec
	 * @param commandParams Parameters given by the user
	 * @param type The type of primitive being remove
	 * @return 0 on success, and a negative error code on failure
	 * @author Eric Burns
	 */
	protected int removePrimitive(HashMap<String,String> commandParams,String type) {
		try {
			int valid=Validator.isValidRemoveRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			List<Integer> ids=CommandParser.convertToIntList(commandParams.get(R.PARAM_ID));
			return con.removePrimitives(ids, Integer.parseInt(commandParams.get(R.PARAM_FROM)), type, commandParams.containsKey(R.PARAM_DELETE_PRIMS));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Resumes a job on starexec that was paused previously
	 * @param commandParams Parameters given by the user at the command line. Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */
	
	protected int resumeJob(HashMap<String,String> commandParams) {
		return pauseOrResumeJob(commandParams,false);
	}
	/**
	 * Pauses a job that is currently running on starexec
	 * @param commandParams Parameters given by the user at the command line. Should include an ID
	 * @return 0 on success or a negative error code on failure
	 */
	
	protected int pauseJob(HashMap<String,String> commandParams) {
		return pauseOrResumeJob(commandParams,true);
	}
	
	/**
	 * Pauses or resumes a job depending on the value of pause
	 * @param commandParams Parameters given by the user at the command line
	 * @param pause Pauses a job if true and resumes it if false
	 * @return 0 on success or a negative error code on failure
	 */
	
	private int pauseOrResumeJob(HashMap<String,String> commandParams, boolean pause) {
		try {
			int valid=Validator.isValidPauseOrResumeRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			return con.pauseOrResumeJob(Integer.parseInt(commandParams.get(R.PARAM_ID)), pause);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL; 
		}

	}
	
	protected int rerunPair(HashMap<String,String> commandParams) {
		try {
			int valid=Validator.isValidRerunRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			return con.rerunPair(Integer.parseInt(commandParams.get(R.PARAM_ID)));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL; 
		}
	}
	
	protected int rerunJob(HashMap<String,String> commandParams) {
		try {
			int valid=Validator.isValidRerunRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			return con.rerunJob(Integer.parseInt(commandParams.get(R.PARAM_ID)));
		} catch (Exception e) {
			return Status.ERROR_INTERNAL; 
		}
	}
	
	/**
	 * Deletes a primitive on StarExec
	 * @param commandParams A HashMap of key/value pairs given by the user at the command line
	 * @param type -- The type of primitive to delete
	 * @return 0 on success and a negative integer otherwise
	 * @author Eric Burns
	 */
	
	protected int deletePrimitive(HashMap<String,String> commandParams, String type) {
		try {
			int valid=Validator.isValidDeleteRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			
			List<Integer> ids=CommandParser.convertToIntList(commandParams.get(R.PARAM_ID));
			return con.deletePrimitives(ids, type);
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * Function for downloading archives from StarExec with the given parameters and 
	 * file output location.
	 * @param urlParams A list of name/value pairs that will be encoded into the URL
	 * @param commandParams A list of name/value pairs that the user entered into the command line
	 * @return 0 on success, a negative integer on error
	 * @author Eric Burns
	 */
	
	protected int downloadArchive(String type,Integer since,Boolean hierarchy,String procClass, HashMap<String,String> commandParams) {
		try {
			int valid=Validator.isValidDownloadRequest(commandParams,type,since);
			if (valid<0) {
				return valid;
			}
			String location=commandParams.get(R.PARAM_OUTPUT_FILE);

			if (type.equals("jp_outputs")) {
				List<Integer> ids=CommandParser.convertToIntList(commandParams.get(R.PARAM_ID));
				return con.downloadJobPairs(ids, location);
			} else { 
				Integer id=Integer.parseInt(commandParams.get(R.PARAM_ID));			 

				//First, put in the request for the server to generate the desired archive			
				return con.downloadArchive(id, type, since, location, commandParams.containsKey(R.PARAM_EXCLUDE_SOLVERS),
						commandParams.containsKey(R.PARAM_EXCLUDE_BENCHMARKS), commandParams.containsKey(R.PARAM_INCLUDE_IDS),
						hierarchy,procClass,commandParams.containsKey(R.PARAM_ONLY_COMPLETED));
			}
			

		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
		
	}
	

	
	/**
	 * Gets the ID of the user currently logged in to StarExec
	 * @return The integer user ID
	 */
	
	protected int getUserID() {
		return con.getUserID();
	}
	
	/**
	 * Lists the IDs and names of some kind of primitives in a given space
	 * @param urlParams Parameters to be encoded into the URL to send to the server
	 * @param commandParams Parameters given by the user at the command line
	 * @return An integer error code with 0 indicating success and a negative number indicating an
	 * error
	 * @author Eric Burns
	 */
	protected HashMap<Integer,String> getPrimsInSpace(String type,HashMap<String,String> commandParams) {
		HashMap<Integer,String> errorMap=new HashMap<Integer,String>();
		
		try {
			
			HashMap<String,String> urlParams=new HashMap<String,String>();
			urlParams.put("id", commandParams.get(R.PARAM_ID));
			urlParams.put(R.FORMPARAM_TYPE, type);
			int valid=Validator.isValidGetPrimRequest(urlParams,commandParams);
			if (valid<0) {
				errorMap.put(valid, null);
				return errorMap;
			}
			Integer id=-1;
			Integer limit = null;
			if (commandParams.containsKey(R.PARAM_ID)) {
				id=Integer.parseInt(commandParams.get(R.PARAM_ID));
			}
			if(commandParams.containsKey(R.PARAM_LIMIT)){
			    limit=Integer.parseInt(commandParams.get(R.PARAM_LIMIT));
			}
			if(type.equals("solverconfigs")){
				return con.getSolverConfigs(id,limit);

			}else{
			    
			       return con.getPrims(id, limit,commandParams.containsKey(R.PARAM_USER), type);

			}
		
		} catch (Exception e) {
			errorMap.put(Status.ERROR_INTERNAL, null);
			
			return errorMap;
		}
	}
	
	/**
	 * Sets a space or space hierarchy as either public or private
	 * @param commandParams Parameters given by the user at the command line
	 * @param setPublic Set public if true and private if false
	 * @return 0 if successful and a negative error code otherwise
	 * @author Eric Burns
	 */
	protected int setSpaceVisibility(HashMap<String,String> commandParams, boolean setPublic) {
		try {
			
			int valid=Validator.isValidSetSpaceVisibilityRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			Boolean hierarchy=false;
			if (commandParams.containsKey(R.PARAM_HIERARCHY)) {
				hierarchy=true;
			}			
			
			return con.setSpaceVisibility(Integer.parseInt(commandParams.get(R.PARAM_ID)), hierarchy, setPublic);
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	/**
	 * This function updates one of the default settings of the current Starexec user
	 * @param setting The field to assign a new value to
	 * @param newVal-- The new value to use for setting
	 * @return A code indicating the success of the operation
	 * @author Eric Burns
	 */
	protected int setUserSetting(String setting, HashMap<String,String> commandParams) {
		
		int valid=Validator.isValidSetUserSettingRequest(setting,commandParams);
		if (valid<0) {
			return valid;
		}
		String newVal=commandParams.get(R.PARAM_VAL);		
		return con.setUserSetting(setting, newVal);
		
	}
	
	
	/**
	 * This method takes in a HashMap mapping String keys to String values
	 * and creates and HTTP POST request that pushes a solver to Starexec
	 * 
	 * @param commandParams The parameters from the command line. "f" or "url", and "id" are required.
	 * @return A status code indicating success or failure
	 * @author Eric Burns
	 */
	
	protected int uploadBenchmarks(HashMap<String, String> commandParams) {
		int valid=Validator.isValidUploadBenchmarkRequest(commandParams);
		if (valid<0) {
			return valid;
		}
		
		
		Boolean dependency=false;
		String depRoot="-1";
		Boolean depLinked=false;
		
		//if the dependency parameter exists, we're using the dependencies it specifies
		if (commandParams.containsKey(R.PARAM_DEPENDENCY)) {
			dependency=true;
			depRoot=commandParams.get(R.PARAM_DEPENDENCY);
			if (commandParams.containsKey(R.PARAM_LINKED)) {
				depLinked=true;
			}
		}
		
		String type=commandParams.get(R.PARAM_BENCHTYPE);
		String space= commandParams.get(R.PARAM_ID);

		
		//don't preserve hierarchy by default, but do so if the hierarchy parameter is present
		boolean hierarchy=false;
		if (commandParams.containsKey(R.PARAM_HIERARCHY)) {
			hierarchy=true;
		}
		
		String url="";
		String upMethod="local";
		//if a url is present, the file should be taken from the url
		if (commandParams.containsKey(R.PARAM_URL)) {
			if (commandParams.containsKey(R.PARAM_FILE)) {
				return Status.ERROR_FILE_AND_URL;
			}
			upMethod="URL";
			url=commandParams.get(R.PARAM_URL);
		}
		Boolean downloadable=false;
		if (commandParams.containsKey(R.PARAM_DOWNLOADABLE)) {
			downloadable=true;
		}
		Permission p=new Permission();
		for (String x : R.PARAMS_PERMS) {
			if (commandParams.containsKey(x) || commandParams.containsKey(R.PARAM_ENABLE_ALL_PERMISSIONS)) {
				p.setPermissionOn(x);
			}
		}
		return con.uploadBenchmarks(commandParams.get(R.PARAM_FILE), Integer.parseInt(type), Integer.parseInt(space), 
				upMethod, p, url, downloadable,
				hierarchy, dependency, depLinked,Integer.parseInt(depRoot));
		
		
		
		
	}
	
	/**
	 * This function handles user requests for uploading a space XML archive.
	 * @param commandParams The key/value pairs given by the user at the command line. Should contain
	 * ID and File keys
	 * @return the new configuration ID on success, and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	protected int uploadConfiguration(HashMap<String, String> commandParams) {
		try {
			
			int valid=Validator.isValidUploadConfigRequest(commandParams);
			if (valid<0) {
				return valid;
			}
			File f=new File(commandParams.get(R.PARAM_FILE));
			String name=getDefaultName(f.getName()+" ");
			String desc="";
			
			if (commandParams.containsKey(R.PARAM_NAME)) {
				name=commandParams.get(R.PARAM_NAME);
			}
			
			if (commandParams.containsKey(R.PARAM_DESC)) {
				desc=commandParams.get(R.PARAM_DESC);
			}
			
			return con.uploadConfiguration(name, desc, commandParams.get(R.PARAM_FILE), Integer.parseInt(commandParams.get(R.PARAM_ID)));
			
			
		} catch (Exception e) {
			return Status.ERROR_INTERNAL;
		}
	}
	
	
	/**
	 * This method takes in a HashMap mapping String keys to String values
	 * and creates and HTTP POST request that pushes a processor to Starexec
	 * 
	 * @param commandParams The parameters from the command line. A file and an ID are required.
	 * @return The new processor ID on success, or a negative error code on failure
	 * @author Eric Burns
	 */
	
	private int uploadProcessor(HashMap<String, String> commandParams, String type) {
		
		int valid=Validator.isValidUploadProcessorRequest(commandParams);
		if (valid<0) {
			return valid;
		}
		
		
		String community= commandParams.get(R.PARAM_ID); //id is one of the required parameters		
		
		//if a name is given explicitly, use it instead
		String name=getDefaultName(new File(commandParams.get(R.PARAM_FILE)).getName());
		if (commandParams.containsKey(R.PARAM_NAME)) {
			name=commandParams.get(R.PARAM_NAME);
		}
		
		//If there is a description, get it
		String desc = "";
		if (commandParams.containsKey(R.PARAM_DESC)) {
			desc=commandParams.get(R.PARAM_DESC);			
		}
		return con.uploadProcessor(name, desc, commandParams.get(R.PARAM_FILE), Integer.parseInt(community), type);
		
		
	}
	
	/**
	 * Handles requests for uploading post-processors.
	 * @param commandParams The key/value pairs given by the user at the command line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	protected int uploadPostProc(HashMap<String,String> commandParams) {
		return uploadProcessor(commandParams, "post");
	}
	
	/**
	 * Handles requests for uploading benchmark processors.
	 * @param commandParams The key/value pairs given by the user at the command line. A file and an ID are required
	 * @return 0 on success and a negative error code otherwise
	 * @author Eric Burns
	 */
	
	protected int uploadBenchProc(HashMap<String,String> commandParams) {
		return uploadProcessor(commandParams, "bench");
	}
	
	protected int uploadPreProc(HashMap<String,String> commandParams) {
		return uploadProcessor(commandParams,"pre");
	}
	
	/**
	 * This function handles user requests for uploading an xml archive (space or job).
	 * @param commandParams The key/value pairs given by the user at the command line. Should contain
	 * ID and File keys
	 * @param isJobUpload true if job xml upload, false otherwise
	 * @return 0 on success, and a negative error code otherwise
	 * @author Julio Cervantes
	 */
	
    protected List<Integer> uploadXML(HashMap<String, String> commandParams,boolean isJobXML) {
		   List<Integer> fail=new ArrayList<Integer>();

    	try {
			int valid=Validator.isValidUploadXMLRequest(commandParams);
			if (valid<0) {
				fail.add(valid);
				return fail;
			}
			return con.uploadXML(commandParams.get(R.PARAM_FILE), Integer.parseInt(commandParams.get(R.PARAM_ID)),isJobXML);
			
		} catch (Exception e) {		  
		    fail.add(Status.ERROR_INTERNAL);
			return fail;
		}
	}
    
    /**
     * Prints out the status of a benchmark upload request to stdout, assuming the status could be found
     * @param commandParams
     * @return 0 on success and a negative error code otherwise
     */
    protected int printBenchStatus(HashMap<String,String> commandParams) {
    	int valid = Validator.isValidPrintBenchUploadStatusRequest(commandParams);
    	if (valid<0) {
    		return valid;
    	}
    	String status=con.getBenchmarkUploadStatus(Integer.parseInt(commandParams.get(R.PARAM_ID)));
    	if (status!=null) {
    		System.out.println(status);
    		return 0;
    	} else {
    		return Status.ERROR_SERVER;
    	}
    }
	
    protected Map<String, String> getPrimitiveAttributes(HashMap<String, String> commandParams, String type) {
    	HashMap<String,String> failMap=new HashMap<String,String>();
    	try{
    		int valid = Validator.isValidGetPrimitiveAttributesRequest(commandParams);
        	if (valid<0) {
        		failMap.put("-1", String.valueOf(valid));
        		return failMap;
        	}
    		int id=Integer.parseInt(commandParams.get(R.PARAM_ID));
    		
    		return con.getPrimitiveAttributes(id, type);
    	} catch (Exception e) {
    		e.printStackTrace();
    		failMap.put("-1", String.valueOf(Status.ERROR_INTERNAL));
    		return failMap;
    	}
    	
    }
    
	/**
	 * This method takes in a HashMap mapping String keys to String values
	 * and creates and HTTP POST request that pushes a solver to Starexec
	 * 
	 * @param commandParams The parameters from the command line. A file or url and and ID are required.
	 * @return The ID of the newly uploaded solver on success, or a negative error code on failure
	 * @author Eric Burns
	 */
	
	protected int uploadSolver(HashMap<String, String> commandParams) {
		int valid=Validator.isValidSolverUploadRequest(commandParams);
		if (valid<0) {
			return valid;
		}
		File f=null;
		String name = getDefaultName("");
		String desc = "";
		String space= commandParams.get(R.PARAM_ID); //id is one of the required parameters
		String upMethod="local";
		String url="";
		String descMethod="upload";
		Boolean downloadable=false;
		Boolean runTestJob=false;
		//if a url is present, the file should be taken from the url
		if (commandParams.containsKey(R.PARAM_URL)) {
			upMethod="URL";
			url=commandParams.get(R.PARAM_URL);
		} else {
			f = new File(commandParams.get(R.PARAM_FILE));
			//name defaults to the name of the file plus the date if none is given
			name=getDefaultName(f.getName()+" ");							
		}
		
		//if a name is given explicitly, use it instead
		if (commandParams.containsKey(R.PARAM_NAME)) {
			name=commandParams.get(R.PARAM_NAME);
		}
		
		//d is the key used for directly sending a string description
		if (commandParams.containsKey(R.PARAM_DESC)) {
			descMethod="text";
			desc=commandParams.get(R.PARAM_DESC);
			
		//df is the "description file" key, which should have a filepath value
		} else if (commandParams.containsKey(R.PARAM_DESCRIPTION_FILE)) {
			descMethod="file";
			desc=commandParams.get(R.PARAM_DESCRIPTION_FILE); // set the description to be the filepath
		}
		
		if (commandParams.containsKey(R.PARAM_DOWNLOADABLE)) {
			downloadable=true;
		}
		if (commandParams.containsKey(R.PARAM_RUN)) {
			runTestJob=true;
		}
		Integer settingId=null;
		if (commandParams.containsKey(R.PARAM_SETTING)) {
			settingId=Integer.parseInt(R.PARAM_SETTING);
		}
		Integer type=1;
		if (commandParams.containsKey(R.PARAM_TYPE)) {
			type=Integer.parseInt(commandParams.get(R.PARAM_TYPE));
		}
		if (upMethod.equals("local")) {
			return con.uploadSolver(name, desc,descMethod, Integer.parseInt(space), f.getAbsolutePath(), downloadable,runTestJob,settingId,type);
		} else {
			return con.uploadSolverFromURL(name, desc,descMethod, Integer.parseInt(space), url, downloadable,runTestJob,settingId,type);
		}
		
	}
	
	/**
	 * @return whether the Connection object represents a valid connection to the server
	 * @author Eric Burns
	 */
	
	protected boolean isConnectionValid() {
		return con.isValid();
		
	}
	
	
	
	/**
	 * Returns the string name given to a primitive if none is specified. The date is used currently
	 * @param prefix A prefix which should be added to the name
	 * @return A string that will be valid for use as a primitive on Starexec
	 * @author Eric Burns
	 */
	private String getDefaultName(String prefix) {
		String date=Calendar.getInstance().getTime().toString();
		date=date.replace(":", " ");
		
		return prefix+date;
	}
}
