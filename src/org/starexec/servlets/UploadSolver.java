package org.starexec.servlets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Reports;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverBuildStatus;
import org.starexec.data.to.User;
import org.starexec.data.to.Solver.ExecutableType;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.LogUtil;
import org.starexec.util.PartWrapper;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;
import org.starexec.jobs.JobManager;
/**
 * Allows for the uploading and handling of Solvers. Solvers can come in .zip,
 * .tar, or .tar.gz format, and configurations can be included in a top level
 * "bin" directory. Each Solver is saved in a unique directory on the filesystem.
 * 
 * @author Skylar Stark
 */
@SuppressWarnings("serial")
@MultipartConfig
public class UploadSolver extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(UploadSolver.class);	
	private static final LogUtil logUtil = new LogUtil(log);
    private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);   
    private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
    
    // Some param constants to process the form
    private static final String SOLVER_DESC = "desc";
    private static final String SOLVER_DESC_FILE = "d";
    private static final String SOLVER_DOWNLOADABLE = "dlable";
    private static final String SPACE_ID = R.SPACE;
    private static final String UPLOAD_FILE = "f";
    private static final String SOLVER_NAME = "sn";    		
    private static final String UPLOAD_METHOD="upMethod";
    private static final String DESC_METHOD = "descMethod";
    private static final String FILE_URL="url";
    private static final String RUN_TEST_JOB="runTestJob";
    private static final String SETTING_ID="setting";
    private static final String SOLVER_TYPE="type";
        
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
	    log.info("doPost begins");

			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
	                        log.debug("Parsed multipart request");

				// Make sure the request is valid
				
				
				ValidatorStatusCode status=this.isValidRequest(form, request);
				if(!status.isSuccess()) {
					//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,status.getMessage());
					return;
				}
				log.debug("Validated the request");

				int spaceId=Integer.parseInt((String)form.get(SPACE_ID));
				boolean runTestJob=Boolean.parseBoolean((String)form.get(RUN_TEST_JOB));
				
				// Parse the request as a solver
				int[] result = handleSolver(userId, form);	
				//should be 3 element array where the first element is the new solver ID and the
				//second element is a status code related to whether configurations existed.
                //the third element indicates whether this solver needs to be built on StarExec
				int return_value = result[0];
				int configs = result[1];
				int buildJob = result[2];
				
				
				// Redirect based on success/failure
				if(return_value>=0) {
					if(buildJob>0) {
						int job_return = JobManager.addBuildJob(return_value, spaceId);
						if (job_return >= 0) {
							log.info("Job created successfully. JobId: " + job_return);
						}
						else {
							log.debug("Error in job creation for buildJob for solver: " + return_value);
						}
					}
					
					response.addCookie(new Cookie("New_ID", String.valueOf(return_value)));
                    if(buildJob>0 && !runTestJob) {
					    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&buildmsg=Building Solver On Starexec"));
                    } else if (configs == -1) { //If there are no configs. We do not attempt to run a test job in this case
					    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&msg=No configurations for the new solver"));
					} else {
						//if this solver has some configurations, we should check to see if the user wanted a test job
						if (runTestJob) {
							log.debug("attempting to run test job");
							
							int settingsId=Communities.getDefaultSettings(spaceId).getId();
							//if the user gave a setting ID, then they need to have permission to use that profile
							// otherwise, the community default is used
							if (form.containsKey(SETTING_ID)) {
								settingsId=Integer.parseInt((String)form.get(SETTING_ID));
							}
							
							int jobId=CreateJob.buildSolverTestJob(return_value, spaceId, userId,settingsId);
                            if (buildJob>0 && jobId>0) {
					            response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&buildmsg=Building Solver On Starexec-- test job will be run after build"));
                            } else if (jobId>0) {
								response.sendRedirect(Util.docRoot("secure/details/job.jsp?id="+jobId));
							} else {
							    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&msg=Internal error creating test job-- solver uploaded successfully"));
							}
							
							
						} else {
						    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value));
						}
					}
				} else {
					//Archive Description File failed validation
					if (return_value == -3) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The archive description file is malformed. Make sure it does not exceed 1024 characters.");
						return;
					//URL was invalid
					} else if (return_value == -2) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File could not be accessed at URL"); 
						return;
					//Not enough disk quota
					} else if (return_value==-4) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File is too large to fit in user's disk quota");
					} else if (return_value==-6) {
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Internal error when extracting solver");
					}
					else {
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to upload new solver.");
						return;
					}	
				}									
			} else {
				// Got a non multi-part request, invalid
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
    	} catch (Exception e) {
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		log.error(e.getMessage(), e);
    	}	
	}
    /**
     * Checks to see whether the given directory contains a solver build script in the top level
     * @param dir The directory to look inside of
     * @return True if the build script is there, and false otherwise
     */
    public boolean containsBuildScript(File dir) {
    	return new File(dir,R.SOLVER_BUILD_SCRIPT).exists();
    }
    
	/**
	 * This method is responsible for uploading a solver to
	 * the appropriate location and updating the database to reflect
	 * the solver's location.
	 * @param userId the user ID of the user making the upload request
	 * @param form the HashMap representation of the upload request
	 * @throws Exception 
	 */
	public int[] handleSolver(int userId, HashMap<String, Object> form) throws Exception {
		log.info("handleSolver begins");

		int[] returnArray = new int[3];
		returnArray[0] = 0;
		returnArray[1] = 0;
		returnArray[2] = 0; //0 if prebuilt, 1 if contains buildscript
		
		File sandboxDir=Util.getRandomSandboxDirectory();
		Util.logSandboxContents();
		String upMethod=(String)form.get(UploadSolver.UPLOAD_METHOD); //file upload or url
		PartWrapper item=null;
		String name=null;
		URL url=null;
		Integer spaceId=Integer.parseInt((String)form.get(SPACE_ID));
		if (upMethod.equals("local")) {
			item = (PartWrapper)form.get(UploadSolver.UPLOAD_FILE);	
		} else {
			try {
				
				url=new URL((String)form.get(UploadSolver.FILE_URL));
			} catch (Exception e) {
				log.error(e.getMessage(),e);
				returnArray[0] = -2;
				return returnArray;
			}
				
			try {
				name=url.toString().substring(url.toString().lastIndexOf('/'));
			} catch (Exception e) {
				name=url.toString().replace('/', '-');
			}	
		}

		//Set up a new solver with the submitted information
		Solver newSolver = new Solver();
		newSolver.setUserId(userId);
		newSolver.setName((String)form.get(UploadSolver.SOLVER_NAME));
		newSolver.setDownloadable((Boolean.parseBoolean((String)form.get(SOLVER_DOWNLOADABLE))));

			log.info("Handling upload of solver" + newSolver.getName());
		
		//Set up the unique directory to store the solver
		//The directory is (base path)/user's ID/solver name/date/
		File uniqueDir = new File(R.getSolverPath(), "" + userId);
		uniqueDir = new File(uniqueDir, newSolver.getName());
		uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
		
		newSolver.setPath(uniqueDir.getAbsolutePath());

		uniqueDir.mkdirs();
		
		
		//Process the archive file and extract
		File archiveFile=null;
		//String FileName=null;
		if (upMethod.equals("local")) {
			//Using IE will cause item.getName() to return a full path, which is why we wrap it with the FilenameUtils call
			archiveFile = new File(uniqueDir,  FilenameUtils.getName(item.getName()));
			new File(archiveFile.getParent()).mkdir();
			item.write(archiveFile);
			//item.write(archiveFile);
			//		log.info("handleSolver just wrote archive to disk");
		} else {
			archiveFile=new File(uniqueDir, name);
			new File(archiveFile.getParent()).mkdir();
			if (!Util.copyFileFromURLUsingProxy(url,archiveFile)) {
				throw new Exception("Unable to copy file from URL");
			}
					log.info("handleSolver just downloaded solver from url " + url);
		}
		long fileSize=ArchiveUtil.getArchiveSize(archiveFile.getAbsolutePath());
		
		User currentUser=Users.get(userId);
		long allowedBytes=currentUser.getDiskQuota();
		long usedBytes=currentUser.getDiskUsage();
		
		//the user does not have enough disk quota to upload this solver
		if (fileSize>allowedBytes-usedBytes) {
			archiveFile.delete();
			returnArray[0]=-4;
			return returnArray;
		}
		
		//move the archive to the sandbox
		FileUtils.copyFileToDirectory(archiveFile, sandboxDir);
		archiveFile.delete();
		archiveFile=new File(sandboxDir,archiveFile.getName());
		log.debug("location of archive file = "+archiveFile.getAbsolutePath()+" and archive file exists ="+archiveFile.exists());
		
		//extracts the given archive using the sandbox user
		boolean extracted=ArchiveUtil.extractArchiveAsSandbox(archiveFile.getAbsolutePath(),sandboxDir.getAbsolutePath());

		//give sandbox full permissions over the solver directory
		Util.sandboxChmodDirectory(sandboxDir);
		
		//if there was an extraction error or if the temp directory is still empty.
		if (!extracted || sandboxDir.listFiles().length==0) {
			log.warn("there was an error extracting the new solver archive");
			FileUtils.deleteDirectory(sandboxDir);
			FileUtils.deleteDirectory(uniqueDir);
			FileUtils.deleteQuietly(archiveFile);
			returnArray[0]=-6;
			return returnArray;
		}
        //Checks to see if a build script exists and needs to be built.
		if (containsBuildScript(sandboxDir)) {
            SolverBuildStatus status = new SolverBuildStatus();
            status.setCode(SolverBuildStatus.SolverBuildStatusCode.UNBUILT);
			newSolver.setBuildStatus(status);
			
            returnArray[2] = 1; //Set build flag
            uniqueDir = new File(newSolver.getPath() + "_src");
            newSolver.setPath(uniqueDir.getAbsolutePath());
            uniqueDir.mkdirs();
		}
		else {
                SolverBuildStatus status = new SolverBuildStatus();
                status.setCode(1);
                newSolver.setBuildStatus(status);
        }

		Util.sandboxChmodDirectory(sandboxDir);

		for (File f : sandboxDir.listFiles()) {
			if (f.isDirectory()) {
				try {
					FileUtils.copyDirectoryToDirectory(f, uniqueDir);
				} catch (FileNotFoundException e) {
					throw new FileNotFoundException(
							String.format("Check for broken symbolic links in your solver.%n%s", e.getMessage()));
				}
			} else {
				FileUtils.copyFileToDirectory(f, uniqueDir);
			}
		}
		
		try {
			FileUtils.deleteDirectory(sandboxDir);
		} catch (Exception e) {
			log.error("unable to delete temporary directory at "+sandboxDir.getAbsolutePath());
			log.error(e.getMessage(),e);
		}
		
		String DescMethod = (String)form.get(UploadSolver.DESC_METHOD);
		if (DescMethod.equals("text")){
			newSolver.setDescription((String)form.get(UploadSolver.SOLVER_DESC));
		} else if (DescMethod.equals("file")) {
			PartWrapper item_desc = (PartWrapper)form.get(UploadSolver.SOLVER_DESC_FILE);
			newSolver.setDescription(item_desc.getString());
		} else {	//Upload starexec_description.txt
			try {	
				File descriptionFile=new File(uniqueDir,R.SOLVER_DESC_PATH);
				if (descriptionFile.exists()) {
					String description=FileUtils.readFileToString(descriptionFile);
					if (!Validator.isValidPrimDescription(description)) {
						returnArray[0] = -3;
						return returnArray;
					}
					newSolver.setDescription(description);
					
				} else {
					log.debug("description file option chosen, but file was not present");
				}
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
			
		}
		
		
		
		//Find configurations from the top-level "bin" directory
		for(Configuration c : Solvers.findConfigs(uniqueDir.getAbsolutePath())) {
			newSolver.addConfiguration(c);
		}
		Util.logSandboxContents();
		
		if (newSolver.getConfigurations().isEmpty()) {
			returnArray[1] = -1; //It is empty
		}
		newSolver.setType(ExecutableType.valueOf(Integer.parseInt((String)form.get(SOLVER_TYPE))));
		//Try adding the solver to the database
		int solver_Success = Solvers.add(newSolver, spaceId);
		
		//if we were successful and this solver had a build script, save the build output to show the uploader
/*		if (solver_Success>0 && build) {
			File buildOutputFile=Solvers.getSolverBuildOutput(solver_Success);
			log.debug("output file = "+buildOutputFile.getAbsolutePath());
			buildOutputFile.getParentFile().mkdirs();
			try {
				FileUtils.writeStringToFile(buildOutputFile, buildstr);

			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		} */

		// if the solver was uploaded successfully log the upload in the weekly report table
		if (solver_Success>0) {
			Reports.addToEventOccurrencesNotRelatedToQueue("solvers uploaded", 1);
		}


		returnArray[0] = solver_Success;
		
		return returnArray;
	}
	
	/**
	 * Sees if a given String -> Object HashMap is a valid Upload Solver request.
	 * Checks to see if it contains all the information needed and if the information
	 * is in the right format.
	 * @param form the HashMap representing the upload request.
	 * @return true iff the request is valid
	 */
	private ValidatorStatusCode isValidRequest(HashMap<String, Object> form, HttpServletRequest request) {
		final String method = "isValidRequest";
		try {
			logUtil.entry(method);
			int userId=SessionUtil.getUserId(request);
			//defines the set of attributes that are required
			if (!form.containsKey(UPLOAD_METHOD) ||
					!form.containsKey(UploadSolver.SOLVER_TYPE) ||
					(!form.containsKey(UploadSolver.UPLOAD_FILE) && form.get(UPLOAD_METHOD).equals("local")) ||
					!form.containsKey(DESC_METHOD) || 
					(!form.containsKey(SOLVER_DESC_FILE) && form.get(DESC_METHOD).equals("file"))) {
				
				return new ValidatorStatusCode(false, "Required parameters are missing from the request");
			}
			
			//ensure the space ID is valid
			if (!Validator.isValidPosInteger((String)form.get(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The given space ID is not a valid integer");
			}
			
			if (!Validator.isValidBool((String)form.get(SOLVER_DOWNLOADABLE))) {
				return new ValidatorStatusCode(false, "The 'downloadable' attribute needs to be a valid boolean");
			}
			
			if (!Validator.isValidPosInteger((String)form.get(SOLVER_TYPE))) {
				return new ValidatorStatusCode(false, "Executable Type needed to be sent as a valid integer");
			}
			ExecutableType type=ExecutableType.valueOf(Integer.parseInt((String)form.get(SOLVER_TYPE)));
			if (type==null) {
				return new ValidatorStatusCode(false, "Invalid executable type");
			}
			
			
			if(!Validator.isValidSolverName((String)form.get(UploadSolver.SOLVER_NAME)))  {	
				
				return new ValidatorStatusCode(false, "The given name is invalid-- please refer to the help files to see the proper format");
			}
			
			String DescMethod = (String)form.get(UploadSolver.DESC_METHOD);

			if (DescMethod.equals("file")) {
				PartWrapper item_desc = (PartWrapper)form.get(UploadSolver.SOLVER_DESC_FILE);
				if (!Validator.isValidPrimDescription(item_desc.getString())) {
					return new ValidatorStatusCode(false, "The given description is invalid-- please refer to the help files to see the proper format");
				}
			}

			if(!Validator.isValidPrimDescription((String)form.get(SOLVER_DESC)))  {	
				return new ValidatorStatusCode(false, "The given description is invalid-- please refer to the help files to see the proper format");
			}
			
			boolean goodExtension=false;
			String fileName=null;
			if ( ((String)form.get(UploadSolver.UPLOAD_METHOD)).equals("local")) {
				fileName = FilenameUtils.getName(((PartWrapper)form.get(UploadSolver.UPLOAD_FILE)).getName());
				
			} else {
				fileName=(String)form.get(UploadSolver.FILE_URL);
				
			}
			for(String ext : UploadSolver.extensions) {
				if(fileName.endsWith(ext)) {
					goodExtension=true;
				}
			}
			if (!goodExtension) {
				return new ValidatorStatusCode(false, "Archives need to have an extension of .zip, .tar, or .tgz");
			}
			
			int spaceId=Integer.parseInt((String)form.get(R.SPACE));
			Permission userPermissions = SessionUtil.getPermission(request, spaceId);
			if (userPermissions == null || !userPermissions.canAddSolver()) {
				return new ValidatorStatusCode(false, "You are not authorized to add solvers to this space");
			}
			
			if (!Validator.isValidBool((String)form.get(RUN_TEST_JOB))) {
				return new ValidatorStatusCode(false, "The 'run test job' attribute needs to be a valid boolean");
			}
			Boolean runTestJob=Boolean.parseBoolean((String)form.get(RUN_TEST_JOB));
			
			//if the user wants to run a test job, there is some additional validation to do
			if (runTestJob) {
				int settingsId=Communities.getDefaultSettings(spaceId).getId();
				//if the user gave a setting ID, then they need to have permission to use that profile
				// otherwise, the community default is used
				if (form.containsKey(SETTING_ID)) {
					if (!Validator.isValidPosInteger((String)form.get(SETTING_ID))) {
						return new ValidatorStatusCode(false, "The given setting ID is not a valid integer");
					}
					settingsId=Integer.parseInt((String)form.get(SETTING_ID));
				}
				
				
				// user must have permission to run a job in the given space
				ValidatorStatusCode testJobStatus=JobSecurity.canCreateQuickJobWithCommunityDefaults(userId, spaceId,settingsId);
				if (!testJobStatus.isSuccess()) {
					return testJobStatus;
				}
				
				
			}
			
			
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return new ValidatorStatusCode(false, "Internal error uploading solver");
	}
}
