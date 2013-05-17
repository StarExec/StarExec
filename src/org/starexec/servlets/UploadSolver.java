package org.starexec.servlets;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
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
    private static final String CONFIG_PREFIX = R.CONFIGURATION_PREFIX;
    private static final String UPLOAD_METHOD="upMethod";
    private static final String DESC_METHOD = "descMethod";
    private static final String FILE_URL="url";
        
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	int userId = SessionUtil.getUserId(request);
    	try {	
    		// If we're dealing with an upload request...
			if (ServletFileUpload.isMultipartContent(request)) {
				HashMap<String, Object> form = Util.parseMultipartRequest(request); 
				
				// Make sure the request is valid
				
				String DescMethod = (String)form.get(UploadSolver.DESC_METHOD);

				if (DescMethod.equals("file")) {
					FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
					if (!Validator.isValidPrimDescription(item_desc.getString())) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The local description file is malformed. Make sure it does not exceed 1024 characters.");
						return;
					}
				}
				
				if(!this.isValidRequest(form)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The upload solver request was malformed");
					return;
				}
				
				int spaceId=Integer.parseInt((String)form.get("space"));
				if (!Permissions.canUserUploadArchive(spaceId, SessionUtil.getUserId(request))) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not a member of this space");
					return;
				}
				
				// Make sure that the solver has a unique name in the space.
				if(Spaces.notUniquePrimitiveName((String)form.get(UploadSolver.SOLVER_NAME), Integer.parseInt((String)form.get(SPACE_ID)), 1)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The solver should have a unique name in the space.");
					return;
				}
				
				// Parse the request as a solver
				int[] result = handleSolver(userId, form);	
				int return_value = result[0];
				int configs = result[1];
			
				// Redirect based on success/failure
				if(return_value != -1 && return_value != -2 && return_value != -3 && return_value!=-4) {
					if (configs == -4) { //If there are no configs
					    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value + "&flag=true"));
					} else {
					    response.sendRedirect(Util.docRoot("secure/details/solver.jsp?id=" + return_value));
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
	 * This method is responsible for uploading a solver to
	 * the appropriate location and updating the database to reflect
	 * the solver's location.
	 * @param userId the user ID of the user making the upload request
	 * @param form the HashMap representation of the upload request
	 * @throws Exception 
	 */
	@SuppressWarnings("deprecation")
	public int[] handleSolver(int userId, HashMap<String, Object> form) throws Exception {
		try {
			int[] returnArray = new int[2];
			returnArray[0] = 0;
			returnArray[1] = 0;
			String upMethod=(String)form.get(UploadSolver.UPLOAD_METHOD);
			FileItem item=null;
			String name=null;
			URL url=null;
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
			

			//FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
			
		

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
			String FileName=null;
			if (upMethod.equals("local")) {
				archiveFile = new File(uniqueDir,  item.getName());
				new File(archiveFile.getParent()).mkdir();
				item.write(archiveFile);
				FileName = item.getName().split("\\.")[0];
			} else {
				archiveFile=new File(uniqueDir, name);
				new File(archiveFile.getParent()).mkdir();
				FileUtils.copyURLToFile(url, archiveFile);
				FileName=name.split("\\.")[0];
			}
			long fileSize=ArchiveUtil.getArchiveSize(archiveFile.getAbsolutePath());
			
			User currentUser=Users.get(userId);
			long allowedBytes=currentUser.getDiskQuota();
			long usedBytes=Users.getDiskUsage(userId);
			
			if (fileSize>allowedBytes-usedBytes) {
				returnArray[0]=-4;
				
				return returnArray;
			}
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());

			String DescMethod = (String)form.get(UploadSolver.DESC_METHOD);
			if (DescMethod.equals("text")){
				newSolver.setDescription((String)form.get(UploadSolver.SOLVER_DESC));
			} else if (DescMethod.equals("file")) {
				FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);
				newSolver.setDescription(item_desc.getString());
			} else {	//Upload starexec_description.txt
				String Destination = archiveFile.getParentFile().getCanonicalPath() + File.separator;
				String strUnzipped = "";		
			    try {
			        FileInputStream fis = new FileInputStream(Destination + FileName +"/" + R.SOLVER_DESC_PATH);
			        BufferedInputStream bis = new BufferedInputStream(fis);
			        DataInputStream dis = new DataInputStream(bis);
			        String text;
			        //dis.available() returns 0 if the file does not have more lines
			        while(dis.available() !=0) {
			            text=dis.readLine().toString();
			            strUnzipped = strUnzipped + text;
			        }
			        fis.close();
			        bis.close();
			        dis.close();
			    } catch (FileNotFoundException e) {
			        log.debug("Archive description method selected, but starexec_description was not found");
			    } catch (IOException e) {
			    	e.printStackTrace();
			    }
			    if (!Validator.isValidPrimDescription(strUnzipped)) {
			    	returnArray[0] = -3;
			    	return returnArray;
			    } else {
			    newSolver.setDescription(strUnzipped);
			    }
			}
			
			int configs_empty = 0;
			//Find configurations from the top-level "bin" directory
			for(Configuration c : findConfigs(uniqueDir.getAbsolutePath())) {
				newSolver.addConfiguration(c);
			}
			if (findConfigs(uniqueDir.getAbsolutePath()).isEmpty()) {
				returnArray[1] = -4; //It is empty
			}
			//Try adding the solver to the database
			int solver_Success = Solvers.add(newSolver, Integer.parseInt((String)form.get(SPACE_ID)));
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
	 * Finds solver run configurations from a specified bin directory. Run configurations
	 * must start with a certain string specified in the list of constants. If no configurations
	 * are found, an empty list is returned.
	 * @param fromPath the base directory to find the bin directory in
	 * @return a list containing run configurations found in the bin directory
	 */
	private List<Configuration> findConfigs(String fromPath){		
		File binDir = new File(fromPath, R.SOLVER_BIN_DIR);
		if(!binDir.exists()) {
			return Collections.emptyList();
		}
		
		List<Configuration> returnList = new ArrayList<Configuration>();
		
		for(File f : binDir.listFiles()){			
			if(f.isFile() && f.getName().startsWith(UploadSolver.CONFIG_PREFIX)){
				Configuration c = new Configuration();								
				c.setName(f.getName().substring(UploadSolver.CONFIG_PREFIX.length()));
				returnList.add(c);
				
				// Make sure the configuration has the right line endings
				Util.normalizeFile(f);
			}				
			//f.setExecutable(true, false);	//previous version only got top level		
		}		
		setHierarchyExecutable(binDir);//should make entire hierarchy executable
		return returnList;
	}
	/*
	 * Sets every file in a hierarchy to be executable
	 * @param rootDir the directory that we wish to have executable files in
	 * @return Boolean true if successful
	 */
	private Boolean setHierarchyExecutable(File rootDir){
		for (File f : rootDir.listFiles()){
			f.setExecutable(true,false);
			if (f.isDirectory()){
				setHierarchyExecutable(f);
			}
		}
		return true;
	}
	
	
	/**
	 * Sees if a given String -> Object HashMap is a valid Upload Solver request.
	 * Checks to see if it contains all the information needed and if the information
	 * is in the right format.
	 * @param form the HashMap representing the upload request.
	 * @return true iff the request is valid
	 */
	private boolean isValidRequest(HashMap<String, Object> form) {
		try {
			if (!form.containsKey(UploadSolver.UPLOAD_FILE) ||
					!form.containsKey(DESC_METHOD) ||
					(!form.containsKey(SOLVER_DESC_FILE) && form.get(DESC_METHOD)=="file") ||
					!form.containsKey(SPACE_ID) ||
					!form.containsKey(UploadSolver.SOLVER_NAME) || 
					!form.containsKey(SOLVER_DESC) ||
					!form.containsKey(SOLVER_DOWNLOADABLE) || 
					!form.containsKey(UPLOAD_METHOD)) {
				System.out.println("here");
				return false;
			}
			
			Integer.parseInt((String)form.get(SPACE_ID));
			Boolean.parseBoolean((String)form.get(SOLVER_DOWNLOADABLE));
			//FileItem item_desc = (FileItem)form.get(UploadSolver.SOLVER_DESC_FILE);

			//String item_desc_file = ArchiveUtil.extractArchiveDesc(SOLVER_NAME);
			
			if(!Validator.isValidPrimName((String)form.get(UploadSolver.SOLVER_NAME)) ||
					!Validator.isValidPrimDescription((String)form.get(SOLVER_DESC)))  {	
				
				return false;
			}
			

			if ( ((String)form.get(UploadSolver.UPLOAD_METHOD)).equals("local")) {
				String fileName = ((FileItem)form.get(UploadSolver.UPLOAD_FILE)).getName();
				for(String ext : UploadSolver.extensions) {
					if(fileName.endsWith(ext)) {
						return true;
					}
				}
			} else {
				String url=(String)form.get(UploadSolver.FILE_URL);
				for (String ext:UploadSolver.extensions) {
					if (url.endsWith(ext)) {
						return true;
					}
				}
			}
			
			
			
			return false;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}