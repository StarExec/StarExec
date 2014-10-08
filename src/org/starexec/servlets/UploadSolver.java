package org.starexec.servlets;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.test.TestUtil;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Allows for the uploading and handling of Solvers. Solvers can come in .zip,
 * .tar, or .tar.gz format, and configurations can be included in a top level
 * "bin" directory. Each Solver is saved in a unique directory on the filesystem.
 * 
 * @author Skylar Stark
 */
@SuppressWarnings("serial")
public class UploadSolver extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(UploadSolver.class);	
    private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);   
    private static final String[] extensions = {".tar", ".tar.gz", ".tgz", ".zip"};
    
    // Some param constants to process the form
    private static final String SOLVER_DESC = "desc";
    private static final String SOLVER_DESC_FILE = "d";
    private static final String SOLVER_DOWNLOADABLE = "dlable";
    private static final String SPACE_ID = "space";
    private static final String UPLOAD_FILE = "f";
    private static final String SOLVER_NAME = "sn";    		
    private static final String UPLOAD_METHOD="upMethod";
    private static final String DESC_METHOD = "descMethod";
    private static final String FILE_URL="url";
    private static final String RUN_TEST_JOB="runTestJob";
        
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
				// Make sure the request is valid
				
				
				ValidatorStatusCode status=this.isValidRequest(form, request);
				if(!status.isSuccess()) {
					//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,status.getMessage());
					return;
				}
				
				boolean runTestJob=Boolean.parseBoolean(request.getParameter(RUN_TEST_JOB));
				int spaceId=Integer.parseInt((String)form.get(SPACE_ID));
				// Parse the request as a solver
				int[] result = handleSolver(userId, form);	
				//should be 2 element array where the first element is the new solver ID and the
				//second element is a status code related to whether configurations existed.
				int return_value = result[0];
				int configs = result[1];
			
				// Redirect based on success/failure
				if(return_value != -1 && return_value != -2 && return_value != -3 && return_value!=-4 && return_value!=-5) {
					response.addCookie(new Cookie("New_ID", String.valueOf(return_value)));
					if (configs == -4) { //If there are no configs
					    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&msg=No configurations for the new solver"));
					} else {
						//if this solver has some configurations, we should check to see if the user wanted a test job
						if (runTestJob) {
							
							
							ValidatorStatusCode testJobStatus=JobSecurity.canCreateQuickJobWithCommunityDefaults(userId, return_value, spaceId, "test", "");
							if (testJobStatus.isSuccess()) {
								int jobId=CreateJob.buildSolverTestJob(return_value, spaceId, userId);
								if (jobId>0) {
								    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value));

								} else {
								    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&msg=Internal error creating test job"));
								}
							} else {
							    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&msg=Could not create test job because:"+status.getMessage()+" "));
							    
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
						//Other Error
					} else if (return_value==-5) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Only community leaders may upload solvers with starexec_build scripts");
					} else {
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
		try {
			boolean build=false;
			String buildstr=null;
			int[] returnArray = new int[2];
			returnArray[0] = 0;
			returnArray[1] = 0;
			
			//first, we upload the solver to the head node sandbox directory
			String randomDirectory=TestUtil.getRandomAlphaString(64);
			File sandboxDirectory=Util.getSandboxDirectory();
			File tempDir=new File(sandboxDirectory,randomDirectory);
			                        
			tempDir.mkdirs();
			String upMethod=(String)form.get(UploadSolver.UPLOAD_METHOD); //file upload or url
			FileItem item=null;
			String name=null;
			URL url=null;
			Integer spaceId=Integer.parseInt((String)form.get(SPACE_ID));
			if (upMethod.equals("local")) {
				item = (FileItem)form.get(UploadSolver.UPLOAD_FILE);	
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
			
			//Set up the unique directory to store the solver
			//The directory is (base path)/user's ID/solver name/date/
			File uniqueDir = new File(R.SOLVER_PATH, "" + userId);
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
			} else {
				archiveFile=new File(uniqueDir, name);
				new File(archiveFile.getParent()).mkdir();
				FileUtils.copyURLToFile(url, archiveFile);
			}
			long fileSize=ArchiveUtil.getArchiveSize(archiveFile.getAbsolutePath());
			
			User currentUser=Users.get(userId);
			long allowedBytes=currentUser.getDiskQuota();
			long usedBytes=Users.getDiskUsage(userId);
			
			//the user does not have enough disk quota to upload this solver
			if (fileSize>allowedBytes-usedBytes) {
				archiveFile.delete();
				returnArray[0]=-4;
				return returnArray;
			}
			
			//move the archive to the sandbox
			FileUtils.copyFileToDirectory(archiveFile, tempDir);
			archiveFile.delete();
			archiveFile=new File(tempDir,archiveFile.getName());
			log.debug("location of archive file = "+archiveFile.getAbsolutePath()+" and archive file exists ="+archiveFile.exists());
			
			//extracts the given archive using the sandbox user
			ArchiveUtil.extractArchiveAsSandbox(archiveFile.getAbsolutePath(),tempDir.getAbsolutePath());
			if (containsBuildScript(tempDir)) {
				log.debug("the uploaded solver did contain a build script");
				if (!SolverSecurity.canUserRunStarexecBuild(userId, spaceId).isSuccess()) { //only community leaders
					FileUtils.deleteDirectory(tempDir);
					FileUtils.deleteDirectory(uniqueDir);
					returnArray[0]=-5;                   //fail due to invalid permissions
					return returnArray;
				}
				
				//change the owner of the sandboxed solver directory to sandbox
				String[] chmod=new String[7];
				chmod[0]="sudo";
				chmod[1]="-u";
				chmod[2]="sandbox";
				chmod[3]="chmod";
				chmod[4]="-R";
				chmod[5]="u+rwx";	
				for (File f : tempDir.listFiles()) {
					chmod[6]=f.getAbsolutePath();
					Util.executeCommand(chmod);
				}
				
				//debugging command
				String[] lsCommand=new String[5];
				lsCommand[0]="sudo";
				lsCommand[1]="-u";
				lsCommand[2]="sandbox";
				lsCommand[3]="ls";
				lsCommand[4]="-l";
				
				
				String lsstr=Util.executeCommand(lsCommand,null,tempDir);
				log.debug(lsstr);

				//run the build script as sandbox
				String[] command=new String[4];
				command[0]="sudo";
				command[1]="-u";
				command[2]="sandbox";
				command[3]="./"+R.SOLVER_BUILD_SCRIPT;
				
				buildstr=Util.executeCommand(command, null,tempDir);
				build=true;
				log.debug("got back the output "+buildstr);
			}
			
			String[] chmodCommand=new String[7];
			chmodCommand[0]="sudo";
			chmodCommand[1]="-u";
			chmodCommand[2]="sandbox";
			chmodCommand[3]="chmod";
			chmodCommand[4]="-R";
			chmodCommand[5]="g+rwx";	
			for (File f : tempDir.listFiles()) {
				chmodCommand[6]=f.getAbsolutePath();
				Util.executeCommand(chmodCommand);
			}
			for (File f : tempDir.listFiles()) {
				if (f.isDirectory()) {
					FileUtils.copyDirectoryToDirectory(f, uniqueDir);
				} else {
					FileUtils.copyFileToDirectory(f, uniqueDir);
				}
			}
			
			try {
			    FileUtils.deleteDirectory(tempDir);
			} catch (Exception e) {
				log.error("unable to delete temporary directory at "+tempDir.getAbsolutePath());
				log.error(e.getMessage(),e);
			}
			
			String DescMethod = (String)form.get(UploadSolver.DESC_METHOD);
			if (DescMethod.equals("text")){
				newSolver.setDescription((String)form.get(UploadSolver.SOLVER_DESC));
			} else if (DescMethod.equals("file")) {
				FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
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
			
			if (newSolver.getConfigurations().isEmpty()) {
				returnArray[1] = -4; //It is empty
			}
			//Try adding the solver to the database
			int solver_Success = Solvers.add(newSolver, spaceId);
			
			//if we were successful and this solver had a build script, save the build output to show the uploader
			if (solver_Success>0 && build) {
				File buildOutputFile=Solvers.getSolverBuildOutput(solver_Success);
				log.debug("output file = "+buildOutputFile.getAbsolutePath());
				buildOutputFile.getParentFile().mkdirs();
				try {
					FileUtils.writeStringToFile(buildOutputFile, buildstr);

				} catch (Exception e) {
					log.error(e.getMessage(),e);
				}
			}
			returnArray[0] = solver_Success;
			
			return returnArray;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		
		
		return List(-1,0);
	}	
	
	private int[] List(int i, int j) {
		int[] answer=new int [2];
		answer[0]=i;
		answer[1]=j;
		return answer;
	}	
	
	/**
	 * Sees if a given String -> Object HashMap is a valid Upload Solver request.
	 * Checks to see if it contains all the information needed and if the information
	 * is in the right format.
	 * @param form the HashMap representing the upload request.
	 * @return true iff the request is valid
	 */
	private ValidatorStatusCode isValidRequest(HashMap<String, Object> form, HttpServletRequest request) {
		try {
			if (!form.containsKey(UPLOAD_METHOD) ||
					(!form.containsKey(UploadSolver.UPLOAD_FILE) && form.get(UPLOAD_METHOD).equals("local")) ||
					!form.containsKey(DESC_METHOD) ||
					(!form.containsKey(SOLVER_DESC_FILE) && form.get(DESC_METHOD).equals("file"))) {
				
				return new ValidatorStatusCode(false, "Required parameters are missing from the request");
			}
			if (!Validator.isValidInteger((String)form.get(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The given space ID is not a valid integer");
			}
			
			if (!Validator.isValidBool((String)form.get(SOLVER_DOWNLOADABLE))) {
				return new ValidatorStatusCode(false, "The 'downloadable' attribute needs to be a valid boolean");
			}
			
			if (!Validator.isValidBool((String)form.get(RUN_TEST_JOB))) {
				return new ValidatorStatusCode(false, "The 'run test job' attribute needs to be a valid boolean");
			}
			
			if(!Validator.isValidSolverName((String)form.get(UploadSolver.SOLVER_NAME)))  {	
				
				return new ValidatorStatusCode(false, "The given name is invalid-- please refer to the help files to see the proper format");
			}
			
			String DescMethod = (String)form.get(UploadSolver.DESC_METHOD);

			if (DescMethod.equals("file")) {
				FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
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
				fileName = ((FileItem)form.get(UploadSolver.UPLOAD_FILE)).getName();
				
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
			
			int spaceId=Integer.parseInt((String)form.get("space"));
			if (!SessionUtil.getPermission(request, spaceId).canAddSolver()) {
				return new ValidatorStatusCode(false, "You are not authorized to add solvers to this space");
			}
			
			
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return new ValidatorStatusCode(false, "Internal error uploading solver");
	}
}