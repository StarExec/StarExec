package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.constants.Web;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.pipelines.JoblineStage;
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
	private static final String JS_FILE_TYPE = "js";
	private static final String CSS_FILE_TYPE = "css";
	private static final String PNG_FILE_TYPE = "png";
	private static final String GIF_FILE_TYPE = "gif";
	private static final String ICO_FILE_TYPE = "ico";
	private static final String IMAGES_DIRECTORY_NAME = "images";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	private static String PARAM_TYPE = "type";
	private static String PARAM_ID = "id";
	private static String PARAM_REUPLOAD = "reupload";
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User u = SessionUtil.getUser(request);
		boolean success;
		String shortName=null;
		try {
			ValidatorStatusCode status=validateRequest(request);
			if (!status.isSuccess()) {
				log.debug("Bad download Request--"+status.getMessage());
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
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
			
			if (request.getParameter(PARAM_TYPE).equals(R.SOLVER)) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter(PARAM_ID)));
				shortName=s.getName();
				boolean reupload = false;
				if (Util.paramExists(PARAM_REUPLOAD, request)) {
					reupload = Boolean.parseBoolean(request.getParameter(PARAM_REUPLOAD));
				}
				shortName=shortName.replaceAll("\\s+",""); //get rid of all whitespace, which we cannot include in the header correctly
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleSolver(s, u.getId(), response, reupload);
			}  else if (request.getParameter(PARAM_TYPE).equals(R.BENCHMARK)) {
				Benchmark b = Benchmarks.get(Integer.parseInt(request.getParameter(PARAM_ID)));
				shortName=b.getName();
				shortName=shortName.replaceAll("\\s+","");
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleBenchmark(b, u.getId(), response);
			} else if (request.getParameter(PARAM_TYPE).equals(R.PAIR_OUTPUT)) {
				int id =Integer.parseInt(request.getParameter(PARAM_ID));
				shortName="Pair_"+id;
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handlePairOutput(id, u.getId(), response);				
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_OUTPUTS)) {
				List<Integer> ids=Validator.convertToIntList(request.getParameter("id[]"));
				shortName="Pair_Output";
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success=handlePairOutputs(ids,u.getId(),response);
			} else if (request.getParameter(PARAM_TYPE).equals(R.SPACE_XML)) {

				Space space = Spaces.get(Integer.parseInt(request.getParameter(PARAM_ID)));
				shortName=space.getName()+"_XML";
				shortName=shortName.replaceAll("\\s+","");
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				boolean includeAttributes = false;
				boolean updates = false;
				int upid = -1;
				if (Util.paramExists("includeattrs",request)) {
				    includeAttributes=Boolean.parseBoolean(request.getParameter("includeattrs"));
				}
				if (Util.paramExists("updates",request)) {
				    updates=Boolean.parseBoolean(request.getParameter("updates"));
				    upid=Integer.parseInt(request.getParameter("upid"));

				}
				
			success = handleSpaceXML(space, u.getId(), response, includeAttributes,updates,upid);

			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_XML)) {
				Job job = Jobs.get(Integer.parseInt(request.getParameter(PARAM_ID)));

				shortName="Job"+ job.getId() + "_XML";
				shortName=shortName.replaceAll("\\s+","");
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleJobXML(job, u.getId(), response);
				
				// this next condition is for the CSV file
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB)) {
				Integer jobId = Integer.parseInt(request.getParameter(PARAM_ID));
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
				success = handleJob(jobId, u.getId(), response, since,ids,complete);
			}  else if (request.getParameter(PARAM_TYPE).equals(R.SPACE)) {
				Space space = Spaces.getDetails(Integer.parseInt(request.getParameter(PARAM_ID)), u.getId());
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
				boolean useIdDirectories = Boolean.parseBoolean(request.getParameter("useIdDirectories"));
				shortName=space.getName();
				shortName=shortName.replaceAll("\\s+","");
				boolean hierarchy=request.getParameter("hierarchy").equals("true");
				if(hierarchy)
				    shortName=shortName+"_Hierarchy";
				
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success = handleSpace(space, u.getId(), response,hierarchy,includeBenchmarks,includeSolvers, useIdDirectories);
				
			  
			} else if (request.getParameter(PARAM_TYPE).equals(R.PROCESSOR)) {
				List<Processor> proc=null;
				shortName="Processor";
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				if (request.getParameter("procClass").equals("post")) {
					
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter(PARAM_ID)), Processor.ProcessorType.POST);
				} else if (request.getParameter("procClass").equals("pre")){
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter(PARAM_ID)), Processor.ProcessorType.PRE);
				} else if (request.getParameter("procClass").equals("update")) {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter(PARAM_ID)), Processor.ProcessorType.UPDATE);
				} else {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter(PARAM_ID)), Processor.ProcessorType.BENCH);
				}
				if (proc.size()>0) {
					success= handleProc(proc,u.getId(),Integer.parseInt(request.getParameter(PARAM_ID)) , response);
				} else {
					response.sendError(HttpServletResponse.SC_NO_CONTENT,"There are no processors to download");
					return;
				}
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_OUTPUT)) {
				int jobId=Integer.parseInt(request.getParameter(PARAM_ID));
				
				String lastSeen=request.getParameter("since");
				String lastMod = request.getParameter("lastTimestamp");
				Integer since=null;
				Long lastModified = null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
				}
				if (lastMod!=null) {
					lastModified=Long.parseLong(lastMod);
				}
				shortName="Job"+jobId+"_output";
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				success= handleJobOutputs(jobId, u.getId(), response,since, lastModified);
				
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_PAGE_DOWNLOAD_TYPE)) {
				int jobId=Integer.parseInt(request.getParameter(PARAM_ID));
				handleJobPage(jobId, request, response);
				// Just set success to true, handleJobPage will throw an exception if it is unsuccessful.
				success = true;

			}else {
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
			//this won't work because we have already opened the response output stream
			//response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			
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
	private static boolean handleSolver(Solver s, int userId,  HttpServletResponse response, boolean reupload) throws Exception {

		String baseName = s.getName();
		// If we can see this solver AND the solver is downloadable...

		if (reupload) {
			ArchiveUtil.createAndOutputZip(new File(s.getPath()), response.getOutputStream(), "",true);
		} else {
			ArchiveUtil.createAndOutputZip(new File(s.getPath()), response.getOutputStream(), baseName,false);
		}
		return true;

	}

	/**
	 * Handles requests for downloading post processors for a given community
	 * @return a file representing the archive to send back to the client
	 * @author Eric Burns
	 */

	private static boolean handleProc(List<Processor> procs, int userId, int spaceId, HttpServletResponse response) throws Exception {

			
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
	private static boolean handleBenchmark(Benchmark b, int userId,HttpServletResponse response) throws Exception {
		// If we can see this benchmark AND the benchmark is downloadable...

			ArchiveUtil.createAndOutputZip(new File(b.getPath()),response.getOutputStream(),"",false);
			return true;

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
	private static boolean handleJobXML(Job job, int userId, HttpServletResponse response) throws Exception {
	        
		// If we can see this 
			List<File> files=new ArrayList<File>();
			log.debug("Permission to download XML granted");	
			
			JobToXMLer handler = new JobToXMLer();
			File file = handler.generateXMLfile(job, userId);
			
			files.add(file);
			
			String baseFileName="Job" + job.getId()+ "_XML";
			
				File schema = new File(R.STAREXEC_ROOT + File.separator + R.JOB_SCHEMA_LOCATION);
				files.add(schema);
			
			ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), baseFileName);
			
			return true;
		

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

    private static boolean handleSpaceXML(Space space, int userId, HttpServletResponse response,
					  boolean includeAttributes, boolean updates, int upid) throws Exception {
		
		// If we can see this Space
			List<File> files=new ArrayList<File>();
			log.debug("Permission to download XML granted, includeAttributes = "+new Boolean(includeAttributes));		
			BatchUtil butil = new BatchUtil();
			File file = butil.generateXMLfile(Spaces.getDetails(space.getId(), userId), userId, includeAttributes, updates, upid);
			
			files.add(file);
			String baseFileName=space.getName()+"_XML";
			
			File schema = new File(R.STAREXEC_ROOT + "/" + R.SPACE_XML_SCHEMA_RELATIVE_LOC);
			files.add(schema);

			ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), baseFileName);
			
			return true;

	}
    
    /**
     * 
     * @param pairIds
     * @param userId
     * @param response
     * @return
     * @throws Exception
     */
    private static boolean handlePairOutputs(List<Integer> pairIds, int userId, HttpServletResponse response) throws Exception {
		List<JobPair> pairs=new ArrayList<JobPair>();
		Job j=null;
		for (Integer id : pairIds) {
			JobPair jp = JobPairs.getPair(id);
			if (jp==null) {
				return false;
			}
			pairs.add(jp);
			if (j==null) {
				j=Jobs.get(jp.getJobId());
				//make sure the user can see the job
				if (!Permissions.canUserSeeJob(id, userId)) {
					return false;
				}
			} else {
				//for now, only get pairs if they are part of one job
				if (j.getId()!=jp.getJobId()) {
					return false;
				}
			}
			
		}

		String baseName="Job"+String.valueOf(j.getId())+"_output";

		Download.addJobPairsToZipOutput(pairs,response,baseName,false,null);
    	return true;
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
	
	private static boolean handlePairOutput(int pairId, int userId,HttpServletResponse response) throws Exception {    	
			ArchiveUtil.createAndOutputZip(JobPairs.getOutputPaths(pairId), response.getOutputStream(), "");
			return true;
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
	private static boolean handleJob(Integer jobId, int userId, HttpServletResponse response, Integer since, Boolean returnIds, Boolean onlyCompleted) throws Exception {    	
		log.info("Request for job " + jobId + " csv from user " + userId);
		
			Job job=Jobs.get(jobId);
			HashMap<Integer,HashMap<Integer, Properties>> props= null;
			if (since==null) {
				job.setJobPairs(Jobs.getJobPairsInJobSpaceHierarchy(job.getPrimarySpace()));
				props=Jobs.getJobAttributes(jobId);
			} else {
				job.setJobPairs(Jobs.getJobPairsInJobSpaceHierarchy(job.getPrimarySpace(),since));
				props= Jobs.getNewJobAttributes(jobId, since);
				int olderPairs = Jobs.countOlderPairs(jobId,since);

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
				response.addCookie(new Cookie("Older-Pairs",String.valueOf(olderPairs)));
				response.addCookie(new Cookie("Total-Pairs",String.valueOf(Jobs.getPairCount(jobId))));
				
			}
			Jobs.loadPropertiesIntoPairs(job.getJobPairs(), props);
			log.debug("about to create a job CSV with "+job.getJobPairs().size()+" pairs");
			String jobFile = CreateJobCSV(job, returnIds,onlyCompleted);
			ArchiveUtil.createAndOutputZip(new File(jobFile), response.getOutputStream(),"",false);

			return true;

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
		sb.append(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR);
		sb.append(File.separator);
		int maxStageNumbers=0;
		for (JobPair jp : job) {
			maxStageNumbers=Math.max(maxStageNumbers, jp.getStages().size());
		}
		sb.append("Job");
		sb.append(job.getId());
		sb.append("_info");
		sb.append(".csv");
		String filename = sb.toString();

		List<JobPair> pairs = job.getJobPairs();
		Iterator<JobPair> itr = pairs.iterator();
		
		/* generate the table header */
		sb.delete(0, sb.length());
		if (maxStageNumbers>1) {
			sb.append("stage number,");
		}
		if (!returnIds) {
			sb.append("benchmark,solver,configuration,status,cpu time,wallclock time,memory usage,result");
		} else {
			sb.append("pair id,benchmark,benchmark id,solver,solver id,configuration,configuration id,status,cpu time,wallclock time,memory usage,result");
		}
		
		HashMap<Integer,String> expectedValues=Jobs.getAllAttrsOfNameForJob(job.getId(),R.EXPECTED_RESULT);
		for (JobPair jp : pairs) {
			if (expectedValues.containsKey(jp.getBench().getId())) {
				jp.getPrimaryStage().getAttributes().put(R.EXPECTED_RESULT, expectedValues.get(jp.getBench().getId()));
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
			if (getOnlyCompleted) {
				if (pair.getStatus().getCode().incomplete()) {
					log.debug("found an incomplete pair to exclude!");
					continue;
				}
			}
			pair.sortStages();
			for (JoblineStage stage : pair.getStages()) {
				//users can optionally get only completed pairs
				
				if (maxStageNumbers>1) {
					sb.append(stage.getStageNumber());
					sb.append(",");
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
				sb.append(stage.getSolver().getName());
				sb.append(",");
				if (returnIds) {
					sb.append(stage.getSolver().getId());
					sb.append(",");
				}
				sb.append(stage.getConfiguration().getName());
				sb.append(",");
				if (returnIds) {
					sb.append(stage.getConfiguration().getId());
					sb.append(",");
				}
				sb.append(stage.getStatus().toString());

				sb.append(",");
				sb.append((stage.getCpuTime()));

				sb.append(",");
				sb.append((stage.getWallclockTime()));

				sb.append(",");
				
				
				sb.append(stage.getMaxVirtualMemory());
				sb.append(",");
				sb.append(stage.getStarexecResult());

				if (attrNames != null) {
					// print out attributes for this job pair
					Properties props = stage.getAttributes();

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
			

		}
		FileUtils.write(new File(filename), sb.toString());
		return filename;
	}
	
	/**
	 * Puts all the given pairs into a zip archive that is streamed into the http response object. The http output stream
	 * is closed at the end
	 * @param pairs The pairs to output
	 * @param response The HTTP response
	 * @param baseName The top level name to give to the archive
	 * @param useSpacePath If true, pair output will be in a directory including the pair space path. If false, they will simply
	 * be in a flat list of directories with job pair IDs
	 * @param lastModified Only retrieve files that were modified after the given date
	 * @return
	 */
	private static boolean addJobPairsToZipOutput(List<JobPair> pairs, HttpServletResponse response,String baseName,boolean useSpacePath, 
			Long earlyDate) {
		if (pairs.size()==0) {
			return true; // don't try to make a zip if there are no pairs
		}
		try {
			ZipOutputStream stream=new ZipOutputStream(response.getOutputStream());
			for (JobPair p : pairs ) {
				StringBuilder zipFileName=new StringBuilder(baseName);
				zipFileName.append(File.separator);
				if (useSpacePath) {
					String path=p.getPath();

					String [] spaces=path.split("/");
					
					for (int index=0;index<spaces.length;index++) {
						zipFileName.append(spaces[index]);
						zipFileName.append(File.separator);
					}

					zipFileName.append(p.getPrimarySolver().getName());
					zipFileName.append(File.separator);
					zipFileName.append(p.getPrimaryConfiguration().getName());
					zipFileName.append(File.separator);
					zipFileName.append(p.getId());
				}
				List<File> files = JobPairs.getOutputPaths(p);
				for (File file : files) {
					if (file.exists()) {
						if (file.isDirectory()) {
							if (earlyDate==null){
								ArchiveUtil.addDirToArchive(stream, file, zipFileName.toString());

							} else {
								ArchiveUtil.addDirToArchive(stream, file, zipFileName.toString(), earlyDate);
							}
						} else {
							zipFileName.append(File.separator);
							zipFileName.append(p.getBench().getName());
							if (earlyDate==null) {
								ArchiveUtil.addFileToArchive(stream, file, zipFileName.toString());

							} else {
								ArchiveUtil.addFileToArchive(stream, file, zipFileName.toString(), earlyDate);
							}
						}
						

					} else {
						//if we can't find output for the pair, just put an empty file there
						ArchiveUtil.addStringToArchive(stream, " ", zipFileName.toString());
					}
				}
				
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
	private static boolean handleJobOutputs(int jobId, int userId, HttpServletResponse response, Integer since, Long lastModified) throws Exception {    	
		log.debug("got request to download output for job = "+jobId);
		// If the user can actually see the job the pair is apart of
			log.debug("confirmed user can download job = "+jobId);
			

			//if we only want the new job pairs
			if (since!=null) {
				if (lastModified==null) {
					log.warn("handleJobOutputs called to get new results, but lastModified is null");
					lastModified=0l;
				}
				log.debug("Getting incremental job output results");
				int olderPairs = Jobs.countOlderPairs(jobId,since);
				List<JobPair> pairs=Jobs.getNewCompletedPairsShallow(jobId, since);
				
				log.debug("Found "+ pairs.size()  + " new pairs");
				int maxCompletion=since;
				// pairsFound is defined as the number of pairs that completed since "since"
				// it does NOT include running pairs
				int pairsFound = 0;
				for (JobPair x : pairs) {
					log.debug("found pair id = "+x.getId() +" with completion id = "+x.getCompletionId());
					if (x.getCompletionId()>maxCompletion) {
						maxCompletion=x.getCompletionId();
					}
					if (x.getStatus().getCode().finishedRunning()) {
						pairsFound++;
					}
				}	
				response.addCookie(new Cookie("Older-Pairs",String.valueOf(olderPairs)));
				response.addCookie(new Cookie("Pairs-Found",String.valueOf(pairsFound)));
				response.addCookie(new Cookie("Total-Pairs",String.valueOf(Jobs.getPairCount(jobId))));
				response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
				log.debug("added the max-completion cookie, starting to write output for job id = "+jobId);
				String baseName="Job"+String.valueOf(jobId)+"_output_new";

				// get all files in between 
				Download.addJobPairsToZipOutput(pairs,response,baseName,true, lastModified);
			
			} else {
				log.debug("preparing to create archive for job = "+jobId);
				ArchiveUtil.createAndOutputZip(new File(Jobs.getDirectory(jobId)),response.getOutputStream(),"Job"+String.valueOf(jobId)+"_output",false);

			}

			return true;
	}

	private static void handleJobPage(int jobId, HttpServletRequest request, HttpServletResponse response) throws IOException {
		File sandboxDirectory = null; 
		try {
			sandboxDirectory = Util.getRandomSandboxDirectory();

			addFilesInDirectory(sandboxDirectory, JS_FILE_TYPE, Web.JOB_DETAILS_JS_FILES);
			addFilesInDirectory(sandboxDirectory, JS_FILE_TYPE, Web.GLOBAL_JS_FILES);
			addFilesInDirectory(sandboxDirectory, CSS_FILE_TYPE, Web.JOB_DETAILS_CSS_FILES);
			addFilesInDirectory(sandboxDirectory, CSS_FILE_TYPE, Web.GLOBAL_CSS_FILES);
			addFilesInDirectory(sandboxDirectory, PNG_FILE_TYPE, "loadingGraph, starlogo, external");
			addFilesInDirectory(sandboxDirectory, GIF_FILE_TYPE, "ajaxloader, loader");
			addFilesInDirectory(sandboxDirectory, ICO_FILE_TYPE, "favicon");
			putHtmlFileFromServerInSandbox(sandboxDirectory, jobId, request);

			File serverCssJqueryUiImagesDirectory = new File(R.STAREXEC_ROOT+"css/jqueryui/images");
			File sandboxCssJqueryUiDirectory = new File(sandboxDirectory, "css/jqueryui");
			FileUtils.copyDirectoryToDirectory(serverCssJqueryUiImagesDirectory, sandboxCssJqueryUiDirectory);

			File serverCssImagesDirectory = new File(R.STAREXEC_ROOT+"css/images");
			File sandboxCssDirectory = new File(sandboxDirectory, "css/");
			FileUtils.copyDirectoryToDirectory(serverCssImagesDirectory, sandboxCssDirectory);

			File serverCssJstreeDirectory = new File(R.STAREXEC_ROOT+"css/jstree");
			FileUtils.copyDirectoryToDirectory(serverCssJstreeDirectory, sandboxCssDirectory);

			File serverImagesJstreeDirectory = new File(R.STAREXEC_ROOT+"images/jstree");
			File sandboxImagesDirectory = new File(sandboxDirectory, "images/");
			FileUtils.copyDirectoryToDirectory(serverImagesJstreeDirectory, sandboxImagesDirectory);

			List<File> filesToBeDownloaded = Arrays.asList(sandboxDirectory.listFiles());

			ArchiveUtil.createAndOutputZip(filesToBeDownloaded, response.getOutputStream(), "Job"+String.valueOf(jobId)+"_page");
		} catch (IOException e) {
			throw new IOException("Could not get files for job page download", e);
		} finally {
			FileUtils.deleteDirectory(sandboxDirectory);
		}
	}

	private static void putHtmlFileFromServerInSandbox(File sandboxDirectory, int jobId, HttpServletRequest request) throws IOException {
		// Create a new html file in the sandbox.
		File htmlFile = new File(sandboxDirectory, "job.html");
		// Make an HTTP request to our own server to get the HTML for the job page and write it to the new html file.
		String urlToGetJobPageFrom = R.STAREXEC_URL_PREFIX+"://"+R.STAREXEC_SERVERNAME+"/"+R.STAREXEC_APPNAME
				+"/secure/details/job.jsp?id="+jobId+"&"+Web.LOCAL_JOB_PAGE_PARAMETER+"=true"; 
		log.debug("Getting job page from "+urlToGetJobPageFrom);
		List<Cookie> requestCookies = Arrays.asList(request.getCookies());
		Map<String, String> queryParameters = new HashMap<String, String>();
		String htmlText = Util.getWebPage(urlToGetJobPageFrom, requestCookies);
		FileUtils.writeStringToFile(htmlFile, htmlText, StandardCharsets.UTF_8);
	}

	private static void addFilesInDirectory(File containingDirectory, String filetype, String fileCsv) throws IOException {
		// Create a new directory named after the filetype such as /js or /css
		String filetypeDirectoryName = null;
		if (filetype.equals(CSS_FILE_TYPE) || filetype.equals(JS_FILE_TYPE)) {
			filetypeDirectoryName = filetype;
		} else if (filetype.equals(PNG_FILE_TYPE) || filetype.equals(GIF_FILE_TYPE) || filetype.equals(ICO_FILE_TYPE)) {
			filetypeDirectoryName = IMAGES_DIRECTORY_NAME;
		} else {
			throw new IOException("Attempted to copy unsupported file type: "+filetype);
		}

		File filetypeDirectory = new File(containingDirectory, filetypeDirectoryName); 

		List<String> allFilePaths = Util.csvToList(fileCsv);

		for (String filePath : allFilePaths) {
			List<String> filesInHierarchy = new ArrayList<String>(Arrays.asList(filePath.split("/")));

			// The last filename is the source file.
			String sourceFile = filesInHierarchy.remove(filesInHierarchy.size() - 1);

			File parentDirectory = filetypeDirectory;	
			for (String directory : filesInHierarchy) {
				File childDirectory = new File(parentDirectory, directory);
				parentDirectory = childDirectory;
			}
			parentDirectory.mkdirs();
			File fileOnServer = new File(R.STAREXEC_ROOT+filetypeDirectoryName+"/"+filePath+"."+filetype);
			File fileToBeDownloaded = new File(parentDirectory, sourceFile+"."+filetype);
			FileUtils.copyFile(fileOnServer, fileToBeDownloaded);
		}
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
	 * @param useIdDirectories whether to put each primitive in a directory that has the name of it's id.
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang + Eric Burns + Albert Giegerich
	 */
	
	private boolean handleSpace(Space space, int uid, HttpServletResponse response,boolean hierarchy, boolean includeBenchmarks,
								boolean includeSolvers, boolean useIdDirectories) throws Exception {
		// If we can see this space AND the space is downloadable...
		try {
				//String baseFileName=space.getName();
				ZipOutputStream stream=new ZipOutputStream(response.getOutputStream());

				storeSpaceHierarchy(space, uid, space.getName(), includeBenchmarks,includeSolvers,hierarchy,stream, useIdDirectories);
				stream.close();

				return true;
			
			
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
	 *        with the name <solverName><solverId>. If null, the solvers are not stored anywhere. Used to create
	 *        links to solvers and prevent downloading them repeatedly. 
	 * @param useIdDirectories set to true if we want every primitive to be contained in a directory that is named
	 *        after the primitives id.
	 * @throws IOException
	 * @author Ruoyu Zhang + Eric Burns + Albert Giegerich
	 */
	private void storeSpaceHierarchy(Space space, int uid, String dest, boolean includeBenchmarks, boolean includeSolvers, 
									 boolean recursive, ZipOutputStream stream, boolean useIdDirectories) throws Exception {
		final String method = "storeSpaceHierarchy";
		log.info("storing space " + space.getName() + "to" + dest);
		if (Permissions.canUserSeeSpace(space.getId(), uid)) {
			if (includeBenchmarks) {
				List<Benchmark> benchList = Benchmarks.getBySpace(space.getId());

				// Get a list of the names of the benchmarks in benchList
				List<String> benchNameList = new LinkedList<String>();
				for (Benchmark bench : benchList) {
					benchNameList.add(bench.getName());
				}
				// Create a map that maps names of benchmarks to whether or not that name is a duplicate in the list.
				HashMap<String,Boolean> benchmarkNameDuplicateMap = createNameDuplicateMap(benchNameList);

				for(Benchmark b: benchList){
					if(b.isDownloadable() || b.getUserId()==uid ){
						File benchmarkFile = new File(b.getPath());
						/*ArchiveUtil.addFileToArchive(stream, benchmarkFile, dest+File.separator+b.getId()+File.separator+b.getName());*/
						String zipFileName;
						if (useIdDirectories) { 
							zipFileName = dest+File.separator+b.getId()+File.separator+b.getName();
						} else {
							boolean isDuplicate = benchmarkNameDuplicateMap.get(b.getName());
							zipFileName = dest+File.separator;
							if (isDuplicate) {
								zipFileName += "__starexex_disambiguate" + File.separator + b.getId() + File.separator;
							}
							zipFileName += b.getName();
						}
						ArchiveUtil.addFileToArchive(stream, benchmarkFile, zipFileName);
					}
				}
			}
			
			if (includeSolvers) {
				List<Solver> solverList=null;
				//if we're getting a full hierarchy and the solver path is
				//not yet set, we want to store all solvers now 
				if (recursive) {
					solverList=Solvers.getBySpaceHierarchy(space.getId(), uid);
				} else{
					solverList=Solvers.getBySpace(space.getId());
				}


				// Create a list of the names of the solvers in solverList
				List<String> solverNames = new LinkedList<String>();
				for (Solver solver: solverList) {
					solverNames.add(solver.getName());
				}
				// Create a map that maps the names of solvers to whether or not they are duplicates in solverList.
				HashMap<String,Boolean> solverNameDuplicateMap = createNameDuplicateMap(solverNames);

					
				log.debug(method+": Number of solvers in space="+solverList.size());
				for (Solver s : solverList) {
					if (s.isDownloadable() || s.getUserId()==uid) {
						File solverFile = new File(s.getPath());
						String zipFileName;
						// Use a different file structure based on whether we're using id directories or not
						if (useIdDirectories) { 
							zipFileName = dest+File.separator+"solvers"+File.separator+s.getId();
						} else {
							boolean isDuplicate = solverNameDuplicateMap.get(s.getName());
							zipFileName = dest+File.separator+"solvers"+File.separator;
							if (isDuplicate) {
								// Since the id directory is not being used we have to check to see if there are
								// solvers with the same name. Append their unique ids if there are.
								zipFileName += "__starexec_disambiguate" + File.separator + s.getId() + File.separator;
							}
							zipFileName += s.getName();
						}
						ArchiveUtil.addDirToArchive(stream, solverFile, zipFileName);
					}
				}
			}
			
			ArchiveUtil.addStringToArchive(stream, space.getDescription(), dest+File.separator+R.DESC_PATH);
			
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
				//include solvers is always false except at the top level
				storeSpaceHierarchy(s, uid, subDir, includeBenchmarks,false,recursive,stream, useIdDirectories);
			}
			return;
		}
		return;
	}

	/**
	 * Using a list of Solvers map each solver name to whether or not that solver name is duplicated in the
	 * solver list.
	 * @author Albert Giegerich
	 */
	private static HashMap<String,Boolean> createNameDuplicateMap(List<String> names) {
		HashMap<String,Boolean> nameDuplicateMap = new HashMap<String,Boolean>();
		for (String name : names) {
			if (nameDuplicateMap.containsKey(name)) {
				// If the name already exists in the map there is a duplication so map this name to true.
				nameDuplicateMap.put(name, true);	
			} else {
				// This is the first occurence of a solver with this name.
				nameDuplicateMap.put(name, false);
			}
		}
		return nameDuplicateMap;
	}
	
	/**
	 * Validates the download request to make sure the requested data is of the right format
	 * 
	 * @return true iff the request is valid
	 * @author Skylar Stark
	 */
	public static ValidatorStatusCode validateRequest(HttpServletRequest request) {
		try {
			if (!Util.paramExists(PARAM_TYPE, request)) {
				return new ValidatorStatusCode(false, "A download type was not specified");
			}
			String type=request.getParameter(PARAM_TYPE);
						
			if (!(type.equals(R.SOLVER) ||
					type.equals(R.BENCHMARK) ||
					type.equals(R.SPACE_XML) ||
			        type.equals(R.JOB_XML) ||
					type.equals(R.PAIR_OUTPUT) ||
					type.equals(R.JOB) ||
					type.equals(R.JOB_OUTPUT) ||
					type.equals(R.SPACE) ||
					type.equals(R.PROCESSOR) ||
					type.equals(R.JOB_OUTPUTS) ||
					type.equals(R.JOB_PAGE_DOWNLOAD_TYPE))) {

				return new ValidatorStatusCode(false, "The supplied download type was not valid");
			}
			
			
			int userId=SessionUtil.getUserId(request);
			if (!type.equals(R.JOB_OUTPUTS)) {
				if (!Validator.isValidPosInteger(request.getParameter(PARAM_ID))) {
					new ValidatorStatusCode(false, "The given id was not a valid integer");
				}
				int id=Integer.parseInt(request.getParameter(PARAM_ID));
				ValidatorStatusCode status=null;
				if (type.equals(R.SOLVER)) {
					status=SolverSecurity.canUserDownloadSolver(id,userId);
					if (!status.isSuccess()) {
						return status;
					}
				} else if (type.equals(R.SPACE_XML) || type.equals(R.SPACE) || type.equals(R.PROCESSOR)) {
					if (!Permissions.canUserSeeSpace(id,userId)) {
						return new ValidatorStatusCode(false, "You do not have permission to see this space");
					}	

				} else if (type.equals(R.JOB) || type.equals(R.JOB_XML) || type.equals(R.JOB_OUTPUT)) {
					if (!Permissions.canUserSeeJob(id, userId)) {
						return new ValidatorStatusCode(false, "You do not have permission to see this job");
					}
				} else if (type.equals(R.PAIR_OUTPUT)) {
					int jobId=JobPairs.getPair(id).getJobId();
					if (!Permissions.canUserSeeJob(jobId, userId)) {
						return new ValidatorStatusCode(false, "You do not have permission to see this job");
					}
				} else if (type.equals(R.BENCHMARK)) {
					status=BenchmarkSecurity.canUserDownloadBenchmark(id, userId);
					if (!status.isSuccess()) {
						return status;
					}
				} else if (type.equals(R.JOB_PAGE_DOWNLOAD_TYPE)) {
					status = JobSecurity.canUserSeeJob(id, userId);
					if (!status.isSuccess()) {
						return status;
					}
				}
			} else {
				//expecting a comma-separated list
				String ids=request.getParameter("id[]");
				if (!Validator.isValidIntegerList(ids)) {
					return new ValidatorStatusCode(false, "The given list of ids contained one or more invalid integers");

				}
				
			}
			
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "Internal error processing download request");
	}
}
