package org.starexec.servlets;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
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
				fileName = handleSolver(s, u.getId(), u.getArchiveType(), response);
			} else if (request.getParameter("type").equals("bench")) {
				Benchmark b = Benchmarks.get(Integer.parseInt(request.getParameter("id")));
				fileName = handleBenchmark(b, u.getId(), u.getArchiveType(), response);
			} else if (request.getParameter("type").equals("jp_output")) {
				JobPair jp = Jobs.getPair(Integer.parseInt(request.getParameter("id")));
				fileName = handlePairOutput(jp, u.getId(), u.getArchiveType(), response);				
			}
			else if (request.getParameter("type").equals("spaceXML")) {
				Space space = Spaces.get(Integer.parseInt(request.getParameter("id")));
				fileName = handleSpaceXML(space, u.getId(), u.getArchiveType(), response);
			}
			else if (request.getParameter("type").equals("job")) {
				Job job = Jobs.getDetailed(Integer.parseInt(request.getParameter("id")));
				fileName = handleJob(job, u.getId(), u.getArchiveType(), response);
			} else if (request.getParameter("type").equals("j_outputs")) {
				Job job = Jobs.getDetailed(Integer.parseInt(request.getParameter("id")));
				fileName = handleJobOutputs(job, u.getId(), u.getArchiveType(), response);
			}
		
			// Redirect based on success/failure
			if(fileName != null) {
				response.sendRedirect("/starexec/secure/files/" + fileName);
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
	 * @author Skylar Stark
	 */
    private static String handleSolver(Solver s, int userId, String format, HttpServletResponse response) throws IOException {
		// If we can see this solver AND the solver is downloadable...
		if (Permissions.canUserSeeSolver(s.getId(), userId) && s.isDownloadable()) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it, and the directory it will be placed in
			String fileName = s.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			ArchiveUtil.createArchive(new File(s.getPath()), uniqueDir, format);
			
			//We return the fileName so the browser can redirect straight to it
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "you do not have permission to download this solver.");
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
		if (Permissions.canUserSeeBench(b.getId(), userId) && b.isDownloadable()) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = b.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			ArchiveUtil.createArchive(new File(b.getPath()), uniqueDir, format);
			
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
    	
    	// If we can see this Space
		if (Permissions.canUserSeeSpace(space.getId(), userId)) {
			log.debug("Permission to download XML granted");			
			BatchUtil butil = new BatchUtil();
			File file = null;
			
			file = butil.generateXMLfile(Spaces.getDetails(space.getId(), userId), userId);
			
			String fileNamewoFormat = UUID.randomUUID().toString();
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
			
			ArchiveUtil.createArchive(container, uniqueDir, format);
			
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
			String outputPath = String.format("%s/%d/%d/%d", R.JOB_OUTPUT_DIR, j.getUserId(), j.getId(), jp.getId());
			ArchiveUtil.createArchive(new File(outputPath), uniqueDir, format);
			
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
    private static String handleJob(Job job, int userId, String format, HttpServletResponse response) throws IOException {    	
		if (Permissions.canUserSeeJob(job.getId(), userId)) {
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			
			String jobFile = CreateJobCSV(job);
			ArchiveUtil.createArchive(new File(jobFile), uniqueDir, format);
			
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
       
        sb.delete(0, sb.length());
        sb.append("benchmark,solver,configuration,status,time,result\r\n");
        
        while(itr.hasNext()) {
        	JobPair pair = itr.next();
        	if (pair.getStatus().getCode() == 7) {
        		sb.append(pair.getBench().getName());
        		sb.append(",");
        		sb.append(pair.getSolver().getName());
        		sb.append(",");
        		sb.append(pair.getSolver().getConfigurations().get(0));
        		sb.append(",");
        		sb.append(pair.getStatus().toString());
        		sb.append(",");
        		sb.append(pair.getSystemTime()/1000);
        		sb.append("ms");
        		sb.append(",");
        		sb.append(pair.getStarexecResult());
        		sb.append("\r\n");
        		}
        	else {
        		sb.append(pair.getBench().getName());
        		sb.append(",");
        		sb.append(pair.getSolver().getName());
        		sb.append(",");
        		sb.append(pair.getSolver().getConfigurations().get(0).getName());
        		sb.append(",");
        		sb.append(pair.getStatus().toString());
        		sb.append(",-");
        		sb.append("\r\n");
        	}
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
    private static String handleJobOutputs(Job j, int userId, String format, HttpServletResponse response) throws IOException {    	

		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(j.getId(), userId)) {
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			
			// The job's output is expected to be in JOB_OUTPUT_DIR/{owner's ID}/{job id}/{pair id}
	    	StringBuilder sb = new StringBuilder();
	    	sb.delete(0, sb.length());
	    	sb.append(R.JOB_OUTPUT_DIR);
	    	sb.append(File.separator);
	    	sb.append(userId);
	    	sb.append(File.separator);
	    	sb.append(j.getId());
			String outputPath = sb.toString();
			ArchiveUtil.createArchive(new File(outputPath), uniqueDir, format);
			
			return fileName;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}
    	
    	return null;
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
    				request.getParameter("type").equals("bench") ||
    				request.getParameter("type").equals("spaceXML") ||
    				request.getParameter("type").equals("jp_output") ||
    				request.getParameter("type").equals("job") ||
    				request.getParameter("type").equals("j_outputs"))) {
    			return false;
    		}
    		
    		return true;
    	} catch (Exception e) {
    		log.warn(e.getMessage(), e);
    	}
    	return false;
    }
}
