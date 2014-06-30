package org.starexec.servlets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.BatchUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;
import org.starexec.util.JobToXMLer;

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
		boolean success;
		String shortName=null;
		try {
			if (false == validateRequest(request)) {
				log.debug("Bad download Request");
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "the download request was invalid");
				return;
			}
			
			Object check=request.getParameter("token");
			//token is used to tell the client when the file has arrived
			if (check!=null) {
				String token=check.toString();
				Cookie newCookie=new Cookie("fileDownloadToken", token);
				newCookie.setMaxAge(60);
				response.addCookie(newCookie);
			}
			
			if (request.getParameter("type").equals("solver")) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter("id")));
				shortName=s.getName();
				shortName=shortName.replaceAll("\\s+",""); //get rid of all whitespace, which we cannot include in the header correctly
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleSolver(s, u.getId(), ".zip", response, false);
			} else if (request.getParameter("type").equals("reupload")) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter("id")));
				shortName=s.getName();
				shortName=shortName.replaceAll("\\s+",""); 
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleSolver(s, u.getId(), ".zip", response, true);
			} else if (request.getParameter("type").equals("bench")) {
				Benchmark b = Benchmarks.get(Integer.parseInt(request.getParameter("id")));
				shortName=b.getName();
				shortName=shortName.replaceAll("\\s+","");
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleBenchmark(b, u.getId(), ".zip", response);
			} else if (request.getParameter("type").equals("jp_output")) {
				int id =Integer.parseInt(request.getParameter("id"));
				shortName="Pair_"+id;
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handlePairOutput(id, u.getId(), ".zip", response);				
			} else if (request.getParameter("type").equals("spaceXML")) {
				Space space = Spaces.get(Integer.parseInt(request.getParameter("id")));
				shortName=space.getName()+"_XML";
				shortName=shortName.replaceAll("\\s+","");
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				boolean includeAttributes = false;
				if (Util.paramExists("includeattrs",request)) {
				    includeAttributes=Boolean.parseBoolean(request.getParameter("includeattrs"));
				}
				success = handleSpaceXML(space, u.getId(), ".zip", response, includeAttributes);

			} else if (request.getParameter("type").equals("jobXML")) {
				Job job = Jobs.get(Integer.parseInt(request.getParameter("id")));

				shortName="Job"+ job.getId() + "_XML";
				shortName=shortName.replaceAll("\\s+","");
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleJobXML(job, u.getId(), ".zip", response);

			} else if (request.getParameter("type").equals("job")) {
				Integer jobId = Integer.parseInt(request.getParameter("id"));
				String lastSeen=request.getParameter("since");
				String returnids=request.getParameter("returnids");
				String getCompleted=request.getParameter("getcompleted");
				Boolean ids=false;
				Boolean complete=false;
				if (getCompleted!=null) {
					complete=Boolean.parseBoolean(getCompleted);
				}
				if (returnids!=null) {
					ids=Boolean.parseBoolean(returnids);
				}
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
				}
				log.debug("getCompleted = "+complete);
				log.debug("returnids = "+ids);
				shortName="Job"+jobId+"_info";
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleJob(jobId, u.getId(), ".zip", response, since,ids,complete);
			}  else if (request.getParameter("type").equals("space")) {
				Space space = Spaces.getDetails(Integer.parseInt(request.getParameter("id")), u.getId());
				// we will  look for these attributes, but if they aren't there then the default should be
				//to get both solvers and benchmarks
				boolean includeSolvers=true;
				boolean includeBenchmarks=true;
				if (Util.paramExists("includesolvers", request)) {
					includeSolvers=Boolean.parseBoolean(request.getParameter("includesolvers"));
				}
				if (Util.paramExists("includebenchmarks", request)) {
					includeBenchmarks=Boolean.parseBoolean(request.getParameter("includebenchmarks"));
				}
				shortName=space.getName();
				shortName=shortName.replaceAll("\\s+","");
				if(request.getParameter("hierarchy").equals("false")){
					response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
					success = handleSpace(space, u.getId(), ".zip", response,false,includeBenchmarks,includeSolvers);
				} else {
					shortName=shortName+"_Hierarchy";
					response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
					success = handleSpace(space, u.getId(), ".zip", response,true,includeBenchmarks,includeSolvers);
				}

			  
			} else if (request.getParameter("type").equals("proc")) {
				List<Processor> proc=null;
				shortName="Processor";
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				if (request.getParameter("procClass").equals("post")) {
					
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.POST);
				} else if (request.getParameter("procClass").equals("pre")){
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.PRE);
				}
				else {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.BENCH);
				}
				if (proc.size()>0) {
					success= handleProc(proc,u.getId(),".zip",Integer.parseInt(request.getParameter("id")) , response);
				} else {
					response.sendError(HttpServletResponse.SC_NO_CONTENT,"There are no processors to download");
					return;
				}
			} else if (request.getParameter("type").equals("j_outputs")) {
				int jobId=Integer.parseInt(request.getParameter("id"));
				
				String lastSeen=request.getParameter("since");
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
					//System.out.println("found since = "+lastSeen);
				}
				shortName="Job"+jobId+"_output";
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success= handleJobOutputs(jobId, u.getId(), ".zip", response,since);
				
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"invalid download type specified");
				return;
			}
			
			if (success) {
				response.getOutputStream().close();
				return;
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to process file for download.");
				return;
			}
											
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.getOutputStream().close();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			
		}
	}	

	/**
	 * Processes a solver to be downloaded. The solver is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param s the solver to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return a file representing the archive to send back to the client
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	private static boolean handleSolver(Solver s, int userId, String format, HttpServletResponse response, boolean reupload) throws Exception {
		
		
		String baseName = s.getName();
		// If we can see this solver AND the solver is downloadable...

		if (Permissions.canUserSeeSolver(s.getId(), userId) && (s.isDownloadable() || s.getUserId()==userId)) {
			if (reupload) {
				ArchiveUtil.createAndOutputZip(new File(s.getPath()), response.getOutputStream(), "",true);
			} else {
				ArchiveUtil.createAndOutputZip(new File(s.getPath()), response.getOutputStream(), baseName,false);
			}
		
			return true;
		}
		else {
			//response.sendError(HttpServletResponse.SC_FORBIDDEN, "you do not have permission to download this solver.");
		}

		return false;
	}

	/**
	 * Handles requests for downloading post processors for a given community
	 * @return a file representing the archive to send back to the client
	 * @author Eric Burns
	 */

	private static boolean handleProc(List<Processor> procs, int userId, String format, int spaceId, HttpServletResponse response) throws Exception {

		if (Permissions.canUserSeeSpace(spaceId, userId)) {
			
			List<File> files=new LinkedList<File>();
			for (Processor x : procs) {
				File newProc=new File(x.getFilePath());
				if (newProc.exists()) {
					files.add(new File(x.getFilePath()));
				} else {
					log.warn("processor with id = "+x.getId()+" exists in the database but not on disk");
				}
			}
			if (files.size()>0) {
				ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), "processors");
				return true;
			}

			
		}
		return false;
	}

	/**
	 * Processes a benchmark to be downloaded. The benchmark is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param b the benchmark to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return a file representing the archive to send back to the client
	 * @author Skylar Stark
	 */
	private static boolean handleBenchmark(Benchmark b, int userId, String format, HttpServletResponse response) throws Exception {
		// If we can see this benchmark AND the benchmark is downloadable...
		if (Permissions.canUserSeeBench(b.getId(), userId) && (b.isDownloadable() || b.getUserId()==userId)) {

			ArchiveUtil.createAndOutputZip(new File(b.getPath()),response.getOutputStream(),"",false);
			return true;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this benchmark.");
		}

		return false;
	}

		/**
	 *Processes a job xml file to be downloaded. 
	 * @param job the job to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return a file representing the archive to send back to the client
	 * @author Julio Cervantes
	 * @throws Exception 
	 */
	private static boolean handleJobXML(Job job, int userId, String format, HttpServletResponse response) throws Exception {
	        
		// If we can see this 
	    if (Permissions.canUserSeeJob(job.getId(), userId)) {
			List<File> files=new ArrayList<File>();
			log.debug("Permission to download XML granted");	
			
			JobToXMLer handler = new JobToXMLer();
			File file = handler.generateXMLfile(job, userId);
			
			files.add(file);
			
			String baseFileName="Job" + job.getId()+ "_XML";
			
			//TODO : should store public/batchJobSchema.xsd in a constant, in case it gets changed later
				File schema = new File(R.STAREXEC_ROOT + File.separator + "public/batchJobSchema.xsd");
				files.add(schema);
			
			

			ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), baseFileName);
			
			return true;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job.");
		}

		return false;
	}

	/**
	 * Processes a space xml file to be downloaded. The xml file and the space hierarchy xml schema is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param space the space to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return a file representing the archive to send back to the client
	 * @author Benton McCune
	 * @throws Exception 
	 */

    private static boolean handleSpaceXML(Space space, int userId, String format, HttpServletResponse response,
					  boolean includeAttributes) throws Exception {
		
		// If we can see this Space
		if (Permissions.canUserSeeSpace(space.getId(), userId)) {
			List<File> files=new ArrayList<File>();
			log.debug("Permission to download XML granted, includeAttributes = "+new Boolean(includeAttributes));		
			BatchUtil butil = new BatchUtil();
			File file = butil.generateXMLfile(Spaces.getDetails(space.getId(), userId), userId, includeAttributes);
			
			files.add(file);
			String baseFileName=space.getName()+"_XML";
			
			File schema = new File(R.STAREXEC_ROOT + "/" + R.SPACE_XML_SCHEMA_RELATIVE_LOC);
			files.add(schema);
			
			

			ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), baseFileName);
			
			return true;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
		}

		return false;
	}

	/**
	 * Processes a job pair's output to be downloaded. The output is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 * @param jp the job pair whose output is to be downloaded
	 * @param userId the id of the user making the download request
	 * @param format the user's preferred archive type
	 * @return a file representing the archive to send back to the client
	 * @author Tyler Jensen
	 */
	
	private static boolean handlePairOutput(int pairId, int userId, String format, HttpServletResponse response) throws Exception {    	
		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(pairId, userId)) {
			
			
			String outputPath = JobPairs.getFilePath(pairId);  
			
			ArchiveUtil.createAndOutputZip(new File(outputPath),response.getOutputStream(),"",false);
			return true;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}

		return false;
	}

	/**
	 * Processes a job csv file to be downloaded. The file contains the information of all the job pairs within the specific job,
	 * given a random name, and placed in a secure folder on the server.
	 * @param job the job needed to be processed.
	 * @param userId the Id of the user who sends the request for the file.
	 * @param format the user's preferred archive type.
	 * @param response the servlet response sent back.
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static boolean handleJob(Integer jobId, int userId, String format, HttpServletResponse response, Integer since, Boolean returnIds, Boolean onlyCompleted) throws Exception {    	
		log.info("Request for job " + jobId + " csv from user " + userId);
		
		if (Permissions.canUserSeeJob(jobId, userId)) {
			
			Job job;
			if (since==null) {
				job = Jobs.getDetailed(jobId);
			} else {
				job=Jobs.getDetailed(jobId,since);
				log.debug("found this many new job pairs "+job.getJobPairs().size());
				//we want to find the largest completion ID seen and send that back to the client
				//so that they know what to ask for next time (mostly for StarexecCommand)
				int maxCompletion=since;
				for (JobPair x : job.getJobPairs()) {
					log.debug("found pair id = "+x.getId() +" with completion id = "+x.getCompletionId());
					if (x.getCompletionId()>maxCompletion) {
						maxCompletion=x.getCompletionId();
					}
				}
				
				response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
				response.addCookie(new Cookie("Pairs-Found",String.valueOf(job.getJobPairs().size())));
				response.addCookie(new Cookie("Total-Pairs",String.valueOf(Jobs.getPairCount(jobId))));
				boolean jobComplete=Jobs.isJobComplete(jobId);
				try {
					if (jobComplete) {
						response.addCookie(new Cookie("Job-Complete","true"));
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

			log.debug("about to create a job CSV with "+job.getJobPairs().size()+" pairs");
			String jobFile = CreateJobCSV(job, returnIds,onlyCompleted);
			ArchiveUtil.createAndOutputZip(new File(jobFile), response.getOutputStream(),"",false);

			return true;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}

		return false;
	}

	/**
	 * Create the csv file for a specific job
	 * @param job the job needed to be processed
	 * @return the file name of the created csv file
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static String CreateJobCSV(Job job, Boolean returnIds, Boolean getOnlyCompleted) throws IOException {
		log.debug("CreateJobCSV called with returnIds set to "+returnIds);
		StringBuilder sb = new StringBuilder();
		sb.delete(0, sb.length());
		sb.append(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR);
		sb.append(File.separator);
		//sb.append(job.getUserId());
		
		sb.append("Job");
		sb.append(job.getId());
		sb.append("_info");
		sb.append(".csv");
		String filename = sb.toString();

		List<JobPair> pairs = job.getJobPairs();
		Iterator<JobPair> itr = pairs.iterator();
		
		/* generate the table header */
		sb.delete(0, sb.length());
		if (!returnIds) {
			sb.append("benchmark,solver,configuration,status,cpu time,wallclock time,result");
		} else {
			sb.append("pair id,benchmark,benchmark id,solver,solver id,configuration,configuration id,status,cpu time,wallclock time,result");
		}
		
		HashMap<Integer,String> expectedValues=Jobs.getAllAttrsOfNameForJob(job.getId(),R.EXPECTED_RESULT);
		for (JobPair jp : pairs) {
			if (expectedValues.containsKey(jp.getBench().getId())) {
				jp.getAttributes().put(R.EXPECTED_RESULT, expectedValues.get(jp.getBench().getId()));
			}
		}
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
			//users can optionally get only completed pairs
			if (getOnlyCompleted) {
				if (pair.getStatus().getCode().incomplete()) {
					log.debug("found an incomplete pair to exclude!");
					continue;
				}
			}
			if (returnIds) {
				sb.append(pair.getId());
				sb.append(",");
			}
			if (pair.getPath()!=null) {
				sb.append(pair.getPath()+"/"+pair.getBench().getName());
			} else {
				sb.append(pair.getBench().getName());
			}
			sb.append(",");
			if (returnIds) {
				sb.append(pair.getBench().getId());
				sb.append(",");
			}
			sb.append(pair.getSolver().getName());
			sb.append(",");
			if (returnIds) {
				sb.append(pair.getSolver().getId());
				sb.append(",");
			}
			sb.append(pair.getConfiguration().getName());
			sb.append(",");
			if (returnIds) {
				sb.append(pair.getConfiguration().getId());
				sb.append(",");
			}
			sb.append(pair.getStatus().toString());

			sb.append(",");
			sb.append((pair.getCpuTime()));

			sb.append(",");
			sb.append((pair.getWallclockTime()));

			sb.append(",");
			sb.append(pair.getStarexecResult());

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
	
	
	private static boolean addJobPairsToZipOutput(int jobId, List<JobPair> pairs, HttpServletResponse response) {
		if (pairs.size()==0) {
			return true; // don't try to make a zip if there are no pairs
		}
		try {
			String baseName="Job"+String.valueOf(jobId)+"_output_new";
			ZipOutputStream stream=new ZipOutputStream(response.getOutputStream());
			for (JobPair p : pairs ) {
				File file=new File(JobPairs.getFilePath(p));
				StringBuilder zipFileName=new StringBuilder(baseName);
				zipFileName.append(File.separator);
				
				String path=p.getPath();

				String [] spaces=path.split("/");
				
				JobPairs.getFilePath(p);
				for (int index=0;index<spaces.length;index++) {
					zipFileName.append(spaces[index]);
					zipFileName.append(File.separator);
				}
					
				
				zipFileName.append(p.getSolver().getName());
				zipFileName.append(File.separator);
				zipFileName.append(p.getConfiguration().getName());
				zipFileName.append(File.separator);
				zipFileName.append(file.getName());
				ArchiveUtil.addFileToArchive(stream, file, zipFileName.toString());
				
				
			}
			stream.close();
			return true;
		} catch (Exception e) {
			log.error("addJobPairsToZipOutput says "+e.getMessage(),e);
		} 
		return false;
	}
	
	/**
	 * Get a zip file which contains the outputs of a job from all its job pairs.
	 * @param j The job to be handled
	 * @param userId The user the job belongs to
	 * @param format The compress format for the user to download
	 * @param response The servlet response sent back
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static boolean handleJobOutputs(int jobId, int userId, String format, HttpServletResponse response, Integer since) throws Exception {    	
		log.debug("got request to download output for job = "+jobId);
		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(jobId, userId)) {
			log.debug("confirmed user can download job = "+jobId);
			boolean jobComplete=Jobs.isJobComplete(jobId);
			//if there are no pending pairs, the job is done
			try {
				if (jobComplete) {
					response.addCookie(new Cookie("Job-Complete","true"));
				}
			} catch (Exception e) {
				log.error(e);
			}

			//if we only want the new job pairs
			if (since!=null) {
				
				log.debug("Getting incremental job output results");
				
				List<JobPair> pairs=Jobs.getNewCompletedPairsShallow(jobId, since);
				
				log.debug("Found "+ pairs.size()  + " new pairs");
				int maxCompletion=since;
				for (JobPair x : pairs) {
					log.debug("found pair id = "+x.getId() +" with completion id = "+x.getCompletionId());
					if (x.getCompletionId()>maxCompletion) {
						maxCompletion=x.getCompletionId();
					}
				}
				response.addCookie(new Cookie("Pairs-Found",String.valueOf(pairs.size())));
				response.addCookie(new Cookie("Total-Pairs",String.valueOf(Jobs.getPairCount(jobId))));
				response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
				log.debug("added the max-completion cookie, starting to write output for job id = "+jobId);
				Download.addJobPairsToZipOutput(jobId,pairs,response);
			
			} else {
				log.debug("preparing to create archive for job = "+jobId);
				ArchiveUtil.createAndOutputZip(new File(Jobs.getDirectory(jobId)),response.getOutputStream(),"Job"+String.valueOf(jobId)+"_output",false);

			}

			return true;
		}

		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
		}

		return false;
	}

	/**
	 * Handles download of a single space or a hierarchy, return the name of compressed file containing the space.
	 * @param space The space needed to be downloaded
	 * @param uid The id of the user making the request
	 * @param format The file format of the generated compressed file
	 * @param hierarchy True if downloading a hierarchy, false for a single space
	 * @param response The servlet response sent back
	 * @param includeBenchmarks Whether to include benchmarks in the directory
	 * @param includeSolvers Whether to include solvers in the directory
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang + Eric Burns
	 */
	
	//TODO: This can be made more efficient by simply copying the hierarchy into the output stream insead of storing it first.
	private boolean handleSpace(Space space, int uid, String format, HttpServletResponse response,boolean hierarchy, boolean includeBenchmarks,boolean includeSolvers) throws Exception {
		// If we can see this space AND the space is downloadable...
		try {
			if (Permissions.canUserSeeSpace(space.getId(), uid)) {	
				
				
				String baseFileName=space.getName();
				File tempDir = new File(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR, UUID.randomUUID().toString() + File.separator + space.getName());
				
				storeSpaceHierarchy(space, uid, tempDir.getAbsolutePath(), includeBenchmarks,includeSolvers,hierarchy,null);
				ArchiveUtil.createAndOutputZip(tempDir,response.getOutputStream(),baseFileName,false);
				if(tempDir.exists()){
					
						FileUtils.deleteDirectory(tempDir);
						log.debug("space directory exists = "+tempDir.exists());
					
					
				}
				
				
				return true;
			}
			else {
				//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
			}
		} catch (Exception e) {
			log.error("unable to delete directory because "+e.getMessage(),e);
		}
		return false;
	}


	/**
	 * Store a space and possibly all its subspaces into the specified directory with their hierarchy
	 * @param space The space needed to be stored
	 * @param uid The user who make the request
	 * @param dest The destination directory
	 * @param includeBenchmarks -- Whether to include benchmarks in the directory
	 * @param  includeSolvers Whether to include solvers in the directory
	 * @param recursive Whether to include subspaces or not
	 * @param solverPath The path to the directory containing solvers, where they are stored in a folder
	 * with the name <solverName><solverId>. If null, the solvers are not stored anywhere. Used to create
	 * links to solvers and prevent downloading them repeatedly. 
	 * @throws IOException
	 * @author Ruoyu Zhang + Eric Burns
	 */
	private void storeSpaceHierarchy(Space space, int uid, String dest, boolean includeBenchmarks, boolean includeSolvers, boolean recursive, String solverPath) throws IOException {
		log.info("storing space " + space.getName() + "to" + dest);
		if (Permissions.canUserSeeSpace(space.getId(), uid)) {
			File tempDir = new File(dest);
			log.debug("[new directory] temp dir = " + dest);
			tempDir.mkdirs();

			if (includeBenchmarks) {
				List<Benchmark> benchList = Benchmarks.getBySpace(space.getId());
				//File benchmarkDir=new File(tempDir,"benchmarks");
				//benchmarkDir.mkdirs();
				for(Benchmark b: benchList){
					if(b.isDownloadable() || b.getUserId()==uid ){
						FileUtils.copyFileToDirectory(new File(b.getPath()), tempDir); // Was benchmarkDir
					
					}
				}
			}
			
			if (includeSolvers) {
				List<Solver> solverList=null;
				//if we're getting a full hierarchy and the solver path is
				//not yet set, we want to store all solvers now 
				if (solverPath==null && recursive) {
					solverList=Solvers.getBySpaceHierarchy(space.getId(), uid);
				} else{
					solverList=Solvers.getBySpace(space.getId());
				}
					
					
				File solverDir=new File(tempDir,"solvers");
				solverDir.mkdirs();
				if (solverPath==null) {
					
					for (Solver s : solverList) {
						if (s.isDownloadable() || s.getUserId()==uid) {
							String solverDirectoryName=(new File(s.getPath())).getName();
							FileUtils.copyDirectoryToDirectory(new File(s.getPath()),solverDir);
							//give solver directory a better name-- ID is included to ensure uniqueness
							(new File(solverDir,solverDirectoryName)).renameTo(new File(solverDir,s.getName()+s.getId()));
						}
					}
					solverPath=solverDir.getAbsolutePath();
				} //else {
					//for (Solver s : solverList) {
						//File existingSolver=new File(solverPath,s.getName()+s.getId());
						//File linkDir=new File(solverDir,s.getName()+s.getId());
						//not working currently, for now, just don't put solvers in the individual spaces
						//Files.createSymbolicLink(Paths.get(linkDir.getAbsolutePath()), Paths.get(existingSolver.getAbsolutePath()));
					//}
				//}
				
			}
			//write the description of the current space to a file
			File description = new File(tempDir + File.separator + R.DESC_PATH);
			FileWriter fw = new FileWriter(description.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(space.getDescription());
			bw.close();
			fw.close();
			
			
			//if we aren't getting subspaces, we're done
			if (!recursive) {
				return;
			}


			List<Space> subspaceList = Spaces.getSubSpaces(space.getId(), uid);
			if(subspaceList ==  null || subspaceList.size() == 0){
				return;
			}

			for(Space s: subspaceList){
				String subDir = dest + File.separator + s.getName();
				storeSpaceHierarchy(s, uid, subDir, includeBenchmarks,includeSolvers,recursive,solverPath);
			}
			return;
		}
		return;
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

			if (!(request.getParameter("type").equals("solver") ||
					request.getParameter("type").equals("reupload") ||
					request.getParameter("type").equals("bench") ||
					request.getParameter("type").equals("spaceXML") ||
			                request.getParameter("type").equals("jobXML") ||
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
