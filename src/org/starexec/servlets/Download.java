package org.starexec.servlets;

import java.io.*;

import java.util.*;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Processors;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.BatchUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Handles requests to download files from starexec
 * @author Skylar Stark & Tyler Jensen
 */
@SuppressWarnings("serial")
public class Download extends HttpServlet {
	private static final Logger log = Logger.getLogger(Download.class);	 
    
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	User u = SessionUtil.getUser(request);
    	String fileName = null;
    	
    	try {
    		
			if (false == validateRequest(request)) {
				log.debug("Bad download Request");
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "the download request was invalid");
				return;
			}
			
			if (request.getParameter("type").equals("solver")) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter("id")));
				fileName = handleSolver(s, u.getId(), u.getArchiveType(), response, false);
			} else if (request.getParameter("type").equals("reupload")) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter("id")));
				fileName = handleSolver(s, u.getId(), u.getArchiveType(), response, true);
			} else if (request.getParameter("type").equals("bench")) {
				Benchmark b = Benchmarks.get(Integer.parseInt(request.getParameter("id")));
				fileName = handleBenchmark(b, u.getId(), u.getArchiveType(), response);
			} else if (request.getParameter("type").equals("jp_output")) {
				JobPair jp = Jobs.getPairDetailed(Integer.parseInt(request.getParameter("id")));
				fileName = handlePairOutput(jp, u.getId(), u.getArchiveType(), response);				
			} else if (request.getParameter("type").equals("spaceXML")) {
				Space space = Spaces.get(Integer.parseInt(request.getParameter("id")));
				fileName = handleSpaceXML(space, u.getId(), u.getArchiveType(), response);	
			} else if (request.getParameter("type").equals("job")) {
				Integer jobId = Integer.parseInt(request.getParameter("id"));
				String lastSeen=request.getParameter("since");
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
				}
				fileName = handleJob(jobId, u.getId(), u.getArchiveType(), response, since);
			} else if (request.getParameter("type").equals("j_outputs")) {
				Job job = Jobs.getDetailed(Integer.parseInt(request.getParameter("id")));
				String lastSeen=request.getParameter("since");
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
				}
				fileName = handleJobOutputs(job, u.getId(), u.getArchiveType(), response,since);
			} else if (request.getParameter("type").equals("space")) {
				Space space = Spaces.getDetails(Integer.parseInt(request.getParameter("id")), u.getId());
				if(request.getParameter("hierarchy").equals("false")){
					fileName = handleSpace(space, u.getId(), u.getArchiveType(), response,false);
				} else {
					fileName = handleSpace(space, u.getId(), u.getArchiveType(), response,true);
				}
			} else if (request.getParameter("type").equals("proc")) {
				List<Processor> proc=null;
				if (request.getParameter("procClass").equals("post")) {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.POST);
				} else {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.BENCH);
				}
				fileName=handleProc(proc,u.getId(),u.getArchiveType(),Integer.parseInt(request.getParameter("id")) , response);
			}
			// Redirect based on success/failure
			if(fileName != null) {
				Object check=request.getParameter("token");
				
				//token is used to tell the client when the file has arrived
				if (check!=null) {
					String token=check.toString();
					Cookie newCookie=new Cookie("fileDownloadToken", token);
					newCookie.setMaxAge(60);
					response.addCookie(newCookie);
				}
				//response.addHeader("Content-Disposition", "attachment; filename=test.zip");
				response.sendRedirect(Util.docRoot("secure/files/" + fileName));
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to process file for download.");	
			}									
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			log.error(e.getMessage(), e);
		}
	}	

	/**
	 * Processes a solver to be downloaded. The solver is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param s the solver to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return the filename of the created archive
	 * @author Skylar Stark & Wyatt Kaiser
	 */
    private static String handleSolver(Solver s, int userId, String format, HttpServletResponse response, boolean reupload) throws IOException {
		log.info("handleSolver");
		String description = s.getDescription();
		String baseName = s.getName();
    	// If we can see this solver AND the solver is downloadable...
		
		if (Permissions.canUserSeeSolver(s.getId(), userId) && (s.isDownloadable() || s.getUserId()==userId)) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it, and the directory it will be placed in
			String fileName = s.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			
			String path = s.getPath();
			int index = path.lastIndexOf("\\");
			String tempdest = path.substring(index);
			
			File tempDir = new File(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR + UUID.randomUUID().toString() + File.separator + s.getName() + tempdest);
			tempDir.mkdirs();
			
			copySolverFile(s.getPath(), tempDir.getAbsolutePath(), description);
			
			ArchiveUtil.createArchive(tempDir, uniqueDir, format, baseName, reupload);
			
			//We return the fileName so the browser can redirect straight to it
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "you do not have permission to download this solver.");
		}
    	
    	return null;
    }
    
	private static void copySolverFile(String path, String dest, String description) throws IOException{				
		File tempSrcFile = new File(path);
		File tempDestFile = new File(dest);
		tempDestFile.mkdirs();
		
		File tempDescFile = new File(dest + File.separator + R.DESC_PATH);
		
		FileUtils.copyDirectory(tempSrcFile, tempDestFile);
		
		int index = dest.lastIndexOf("\\");
		String tempdest = dest.substring(0, index);
		
		//Write to description file
		if (!(description.equals("no description"))) {
			File description2 = new File(tempdest + File.separator + R.DESC_PATH);
			
			FileWriter fw = new FileWriter(description2.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(description);
			bw.close();
			
			FileUtils.copyFile(description2, tempDescFile);
			description = null;
		}
		
		tempDestFile = null;
		
	}

    
    /*
     * Handles requests for downloading post processors for a given community
     * @return the filename of the created archive
     * @author Eric Burns
     */
    
    private static String handleProc(List<Processor> procs, int userId, String format, int spaceId, HttpServletResponse response) throws IOException {
    	
    	if (Permissions.canUserSeeSpace(spaceId, userId)) {
    		
    		String fileName="Community "+String.valueOf(spaceId)+"Procs" + "_("+UUID.randomUUID().toString()+")" +format;
    		File uniqueDir=new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
    		uniqueDir.createNewFile();
    		List<File> files=new LinkedList<File>();
    		for (Processor x : procs) {
    			files.add(new File(x.getFilePath()));
    		}
    		ArchiveUtil.createArchive(files, uniqueDir, format);
    		
    		return fileName;
    	}
    	return null;
    }
    
	/**
	 * Processes a benchmark to be downloaded. The benchmark is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param b the benchmark to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return the filename of the created archive
	 * @author Skylar Stark
	 */
    private static String handleBenchmark(Benchmark b, int userId, String format, HttpServletResponse response) throws IOException {
		// If we can see this benchmark AND the benchmark is downloadable...
		if (Permissions.canUserSeeBench(b.getId(), userId) && (b.isDownloadable() || b.getUserId()==userId)) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = b.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			ArchiveUtil.createArchive(new File(b.getPath()), uniqueDir, format, false);
			
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this benchmark.");
		}
    	
    	return null;
    }
    
	/**
	 * Processes a space xml file to be downloaded. The xml file and the space hierarchy xml schema is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param space the space to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return the filename of the created archive
	 * @author Benton McCune
	 * @throws Exception 
	 */

    private static String handleSpaceXML(Space space, int userId, String format, HttpServletResponse response) throws Exception {
		log.debug("Space XML download called");
		Queue<String> descriptions=new LinkedList<String>();
		descriptions.add(space.getDescription());
		List<Space> children = space.getSubspaces();
		for (Space child : children) {
			descriptions.add(child.getDescription());
		}
    	
    	// If we can see this Space
		if (Permissions.canUserSeeSpace(space.getId(), userId)) {
			log.debug("Permission to download XML granted");			
			BatchUtil butil = new BatchUtil();
			File file = null;
			
			file = butil.generateXMLfile(Spaces.getDetails(space.getId(), userId), userId);
			String baseFileName=space.getName()+"_XML";
			String fileNamewoFormat = baseFileName+"_("+ UUID.randomUUID().toString()+")";
			String fileName = fileNamewoFormat + format;
			//container has the xml schema and the newly created xml file.  uniqueDir is the compressed file downloaded by user
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			File container = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileNamewoFormat);
			container.mkdirs();
			
			File schemaCopy = new File(R.STAREXEC_ROOT, "batchSpaceSchema.xsd");
			FileUtils.moveFileToDirectory(file, container, false);
			File schema = new File(R.SPACE_XML_SCHEMA_LOC);
			InputStream schemaStream = new FileInputStream(schema);
			FileUtils.copyInputStreamToFile(schemaStream, schemaCopy);
			FileUtils.moveFileToDirectory(schemaCopy, container, false);
			uniqueDir.createNewFile();
			
			ArchiveUtil.createArchive(container, uniqueDir,format,baseFileName, false);
			
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
		}
    	
    	return null;
    }
    
    /**
	 * Processes a job pair's output to be downloaded. The output is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param jp the job pair whose output is to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return the filename of the created archive
	 * @author Tyler Jensen
	 */
    private static String handlePairOutput(JobPair jp, int userId, String format, HttpServletResponse response) throws IOException {    	
    	Job j = Jobs.getShallow(jp.getJobId());

		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(j.getId(), userId)) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			
			// The job's output is expected to be in JOB_OUTPUT_DIR/{owner's ID}/{job id}/{pair id}
			//String outputPath = String.format("%s/%d/%d/%d", R.JOB_OUTPUT_DIR, j.getUserId(), j.getId(), jp.getId());
			String outputPath = String.format("%s/%d/%d/%s___%s/%s", R.JOB_OUTPUT_DIR, j.getUserId(), j.getId(), jp.getSolver().getName(), jp.getConfiguration().getName(), jp.getBench().getName());  
		    log.info("The download output path is " + outputPath);
			ArchiveUtil.createArchive(new File(outputPath), uniqueDir, format, false);
			
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}
    	
    	return null;
    }
    
    /**
     * Processes a job csv file to be downloaded. The file contains the information of all the job pairs within the specific job,
     * given a random name, and placed in a secure folder on the server.
     * @param job the job needed to be processed.
     * @param userId the Id of the user who sends the request for the file.
     * @param format the user's preferred archive type.
     * @param response the servlet response sent back.
     * @return the filename of the created archive.
     * @throws IOException
     * @author Ruoyu Zhang
     */
    private static String handleJob(Integer jobId, int userId, String format, HttpServletResponse response, Integer since) throws IOException {    	
		log.info("Request for job " + jobId + " csv from user " + userId);
    	if (Permissions.canUserSeeJob(jobId, userId)) {
    		Job job;
    		if (since==null) {
    			job = Jobs.getDetailed(jobId);
    		} else {
    			job=Jobs.getDetailed(jobId,since);
    			
    			//we want to find the largest completion ID seen and send that back to the client
    			//so that they know what to ask for next time (mostly for StarexecCommand
    			int maxCompletion=since;
    			for (JobPair x : job.getJobPairs()) {
    				if (x.getCompletionId()>maxCompletion) {
    					maxCompletion=x.getCompletionId();
    				}
    			}
    			response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
    			//if (Jobs.getPendingPairsDetailed(job.getId()).size()==0) {
    			//	response.addCookie(new Cookie("Job-Complete","true"));
    			//}
    			
    		}
    		
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			
			String jobFile = CreateJobCSV(job);
			ArchiveUtil.createArchive(new File(jobFile), uniqueDir, format, false);
			
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}
    	
    	return null;
    }
    
    /**
     * Create the csv file for a specific job
     * @param job the job needed to be processed
     * @return the file name of the created csv file
     * @throws IOException
     * @author Ruoyu Zhang
     */
    private static String CreateJobCSV(Job job) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	sb.delete(0, sb.length());
    	sb.append(R.JOB_OUTPUT_DIR);
    	sb.append(File.separator);
    	sb.append(job.getUserId());
    	sb.append("_");
    	sb.append(job.getId());
    	sb.append(".csv");
        String filename = sb.toString();
        
        List<JobPair> pairs = job.getJobPairs();
        Iterator<JobPair> itr = pairs.iterator();
       
	/* generate the table header */
        sb.delete(0, sb.length());
        sb.append("benchmark,solver,configuration,status,time(s),result");
        
	/* use the attribute names for the first completed job pair (if any) for more headings for the table
	   We will put result first, then expected if it is there; other attributes follow */
	Set<String> attrNames = job.attributeNames(); 
	boolean have_expected = false;
	if (attrNames != null) {
	    if (attrNames.contains(R.EXPECTED_RESULT)) {
		// we have the expected result attribute
		have_expected = true;
		sb.append(",expected");
	    }
	    Iterator<String> ita = attrNames.iterator();
	    while (ita.hasNext()) {
		String attr = (String)ita.next();		
		if (!attr.equals(R.STAREXEC_RESULT) && !attr.equals(R.EXPECTED_RESULT)) {
		    // skip printing result and expected result in the header of the table, since we already included them
		    sb.append(",");
		    sb.append(attr);
		}
	    }
	}
	sb.append("\r\n");
	    
        while(itr.hasNext()) {
        	JobPair pair = itr.next();
        	if (pair.getPath()!=null) {
    			sb.append(pair.getPath()+"/"+pair.getBench().getName());
    		} else {
    			sb.append(pair.getBench().getName());
    		}
        	sb.append(",");
    		sb.append(pair.getSolver().getName());
    		sb.append(",");
    		sb.append(pair.getSolver().getConfigurations().get(0).getName());
    		sb.append(",");
    		sb.append(pair.getStatus().toString());
    		
        	if (pair.getStatus().getCode() == StatusCode.STATUS_COMPLETE) {
        		sb.append(",");
        		sb.append((pair.getWallclockTime()));
        		sb.append(",");
        		sb.append(pair.getStarexecResult());
		}
        	else {
        		sb.append(",-");
        	}
		if (attrNames != null) {
		    // print out attributes for this job pair
		    Properties props = pair.getAttributes();
		    
		    if (have_expected && props!=null) {
		    	sb.append(",");
		    	sb.append(props.getProperty(R.EXPECTED_RESULT,"-"));
		    }
		    for (Iterator<String> ita = attrNames.iterator(); ita.hasNext();) {
			String attr = (String)ita.next();
			if (!attr.equals(R.STAREXEC_RESULT) && !attr.equals(R.EXPECTED_RESULT)) {
			    /* we skip printing the starexec-result, and starexec-expected-result attributes,
			       because we printed them already */
			    sb.append(",");
			    sb.append(props.getProperty(attr,"-"));
			}
		    }
		}
		sb.append("\r\n");

        }
        FileUtils.write(new File(filename), sb.toString());
        return filename;
    }
    
    /**
     * Get a zip file which contains the outputs of a job from all its job pairs.
     * @param j The job to be handled
     * @param userId The user the job belongs to
     * @param format The compress format for the user to download
     * @param response The servlet response sent back
     * @return the filename of the created archive
     * @throws IOException
     * @author Ruoyu Zhang
     */
    private static String handleJobOutputs(Job j, int userId, String format, HttpServletResponse response, Integer since) throws IOException {    	

		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(j.getId(), userId)) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			
			uniqueDir.createNewFile();
			
			//if we only want the new job pairs
			List<JobPair> pairs;
			if (since!=null) {
				log.debug("Getting incremental job output results");
				pairs=Jobs.getNewCompletedPairsDetailed(j.getId(), since);
				log.debug("Found "+ pairs.size()  + " new pairs");
				int maxCompletion=since;
    			for (JobPair x : pairs) {
    				if (x.getCompletionId()>maxCompletion) {
    					maxCompletion=x.getCompletionId();
    				}
    			}
    			response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
			} else {
				pairs=Jobs.getPairsDetailed(j.getId());
			}
			File tempDir=new File(new File(R.STAREXEC_ROOT,R.DOWNLOAD_FILE_DIR),fileName+"temp");
			tempDir.mkdir();
			
			File file;
			File dir;
			for (JobPair jp : pairs) {
				file=new File(String.format("%s/%d/%d/%s___%s/%s", R.JOB_OUTPUT_DIR, j.getUserId(), j.getId(), jp.getSolver().getName(), jp.getConfiguration().getName(), jp.getBench().getName()));
				
				log.debug("Searching for pair output at" + file.getAbsolutePath());
				if (file.exists()) {
					log.debug("Adding job pair output file for "+jp.getBench().getName()+" to incremental results");
					
					//store in the old format becaues the pair has no path
					if (jp.getPath()==null) {
						dir=new File(tempDir,jp.getSolver().getName());
						dir.mkdir();
						dir=new File(dir,jp.getConfiguration().getName());
						dir.mkdir();
					} else {
						String path=jp.getPath();
					
						String [] spaces=path.split("/");
						dir=new File(tempDir,spaces[0]);
						dir.mkdir();
						for (int index=1;index<spaces.length;index++) {
							dir=new File(dir,spaces[index]);
							dir.mkdir();
						}
						dir=new File(dir,jp.getSolver().getName());
						dir.mkdir();
						dir=new File(dir,jp.getConfiguration().getName());
						dir.mkdir();
					}
					FileUtils.copyFileToDirectory(file,dir);
					
				}
				
			}
			if (since!=null) {
				ArchiveUtil.createArchive(tempDir, uniqueDir, format,"new_output_"+String.valueOf(j.getId()),false);
			} else {
				ArchiveUtil.createArchive(tempDir, uniqueDir, format,"output_"+String.valueOf(j.getId()),false);
			}
			
			
			//if (Jobs.getPendingPairsDetailed(j.getId()).size()==0) {
    		//	response.addCookie(new Cookie("Job-Complete","true"));
    		//}
			return fileName;
			}
		
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}
    	
    	return null;
    }
    
    
    /**
     * Handles download of a single space or a hierarchy, return the name of compressed file containing the space.
     * @param space The space needed to be downloaded
     * @param uid The id of the user making the request
     * @param format The file format of the generated compressed file
     * @param hierarchy True if downloading a hierarchy, false for a single space
     * @param response The servlet response sent back
     * @return Name of the generated file
     * @throws IOException
     * @author Ruoyu Zhang
     */
	@SuppressWarnings("null")
	private String handleSpace(Space space, int uid, String format, HttpServletResponse response,boolean hierarchy) throws IOException {
		// If we can see this space AND the space is downloadable...

		if (Permissions.canUserSeeSpace(space.getId(), uid)) {	
			Queue<String> descriptions = new LinkedList<String>();

			String baseFileName=space.getName();
			String fileName = space.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR + File.separator), fileName);
			uniqueDir.createNewFile();
			File tempDir = new File(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR + UUID.randomUUID().toString() + File.separator + space.getName());
			if (!hierarchy) {
				descriptions.add(space.getDescription());
				tempDir.mkdirs();
				List<Benchmark> benchList = Benchmarks.getBySpace(space.getId());
				for(Benchmark b: benchList){
					if(b.isDownloadable()){
						copyFile(b.getPath(), tempDir.getAbsolutePath() + File.separator + b.getName(), descriptions);
					}
				}
			} else {
				descriptions.add(space.getDescription());
				storeSpaceHierarchy(space, uid, tempDir.getAbsolutePath(), descriptions);
			}
			ArchiveUtil.createArchive(tempDir, uniqueDir, format, baseFileName, false);
			if(tempDir.exists()){
				tempDir.delete();
			}
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
		}
		return null;
	}
	
	
	/**
	 * Store a space and all its subspaces into the specified directory with their hierarchy
	 * @param space The space needed to be stored
	 * @param uid The user who make the request
	 * @param dest The destination directory
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private void storeSpaceHierarchy(Space space, int uid, String dest, Queue<String> descriptions) throws IOException {
		log.info("storing space " + space.getName() + "to" + dest);
		if (Permissions.canUserSeeSpace(space.getId(), uid)) {
			File tempDir = new File(dest);
			log.debug("[new directory] temp dir = " + dest);
			tempDir.mkdirs();
			
			List<Benchmark> benchList = Benchmarks.getBySpace(space.getId());
			for(Benchmark b: benchList){
				if(b.isDownloadable()){
					copyFile(b.getPath(), tempDir.getAbsolutePath() + File.separator + b.getName(), descriptions);		
				}
			}
			
			
			List<Space> subspaceList = Spaces.getSubSpaces(space.getId(), uid, false);
			if(subspaceList ==  null || subspaceList.size() == 0){
				return;
			}
			
			for(Space s: subspaceList){
				descriptions.add(s.getDescription());
				String subDir = dest + File.separator + s.getName();
				storeSpaceHierarchy(s, uid, subDir, descriptions);
			}
			
			return;
		}
		return;
	}
	
	private void copyFile(String src, String dest, Queue<String> descriptions) throws IOException{
		String curDesc = "no description";
		//log.debug("copying file - source = " +src + ", dest = " + dest);
		if (descriptions.size() != 0) {
			curDesc = descriptions.remove();
		}
		
				
		File tempSrcFile = new File(src);
		File tempDestFile = new File(dest);

		int index = dest.lastIndexOf("\\");
		String tempdest = dest.substring(0, index);
		
		//Write to description file
		if (!(curDesc.equals("no description"))) {
			File description = new File(tempdest + File.separator + R.SOLVER_DESC_PATH);

			FileWriter fw = new FileWriter(description.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(curDesc);
			bw.close();
			
			FileUtils.copyFile(description, tempDestFile);
			description = null;
		}
		
		FileUtils.copyFile(tempSrcFile, tempDestFile);
		
		tempSrcFile = null;
		tempDestFile = null;
		
	}
    
    /**
     * Validates the download request to make sure the requested data is of the right format
     * 
     * @return true iff the request is valid
     * @author Skylar Stark
     */
    public static boolean validateRequest(HttpServletRequest request) {
    	try {
    		if (!Util.paramExists("type", request)
    			|| !Util.paramExists("id", request)) {
    			return false;
    		}
    		
    		if (!Validator.isValidInteger(request.getParameter("id"))) {
    			return false;
    		}
    		
    		// The requested type should be a solver, benchmark, spaceXML, or job pair output
    		if (!(request.getParameter("type").equals("solver") ||
    				request.getParameter("type").equals("reupload") ||
    				request.getParameter("type").equals("bench") ||
    				request.getParameter("type").equals("spaceXML") ||
    				request.getParameter("type").equals("jp_output") ||
    				request.getParameter("type").equals("job") ||
    				request.getParameter("type").equals("j_outputs") ||
    				request.getParameter("type").equals("space") ||
    				request.getParameter("type").equals("proc"))) {
    			return false;
    		}
    		
    		return true;
    	} catch (Exception e) {
    		log.warn(e.getMessage(), e);
    	}
    	return false;
    }
}
