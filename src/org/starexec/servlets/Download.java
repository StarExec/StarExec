package org.starexec.servlets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cache;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.CacheType;
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
		File archive = null;
		String shortName=null;
		try {
			if (false == validateRequest(request)) {
				log.debug("Bad download Request");
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "the download request was invalid");
				return;
			}
			if (request.getParameter("type").equals("solver")) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter("id")));
				shortName=s.getName();
				archive = handleSolver(s, u.getId(), ".zip", response, false);
			} else if (request.getParameter("type").equals("reupload")) {
				Solver s = Solvers.get(Integer.parseInt(request.getParameter("id")));
				shortName=s.getName();
				archive = handleSolver(s, u.getId(), ".zip", response, true);
			} else if (request.getParameter("type").equals("bench")) {
				Benchmark b = Benchmarks.get(Integer.parseInt(request.getParameter("id")));
				shortName=b.getName();
				archive = handleBenchmark(b, u.getId(), ".zip", response);
			} else if (request.getParameter("type").equals("jp_output")) {
				JobPair jp = JobPairs.getPairDetailed(Integer.parseInt(request.getParameter("id")));
				shortName="Pair"+jp.getId();
				archive = handlePairOutput(jp, u.getId(), ".zip", response);				
			} else if (request.getParameter("type").equals("spaceXML")) {
				Space space = Spaces.get(Integer.parseInt(request.getParameter("id")));
				shortName=space.getName()+"_XML";
				archive = handleSpaceXML(space, u.getId(), ".zip", response);	
			} else if (request.getParameter("type").equals("job")) {
				Integer jobId = Integer.parseInt(request.getParameter("id"));
				String lastSeen=request.getParameter("since");
				String returnids=request.getParameter("returnids");
				Boolean ids=false;
				if (returnids!=null) {
					ids=Boolean.parseBoolean(returnids);
				}
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
				}
				shortName="Job"+jobId+"_info";
				archive = handleJob(jobId, u.getId(), ".zip", response, since,ids);
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
				if(request.getParameter("hierarchy").equals("false")){
					archive = handleSpace(space, u.getId(), ".zip", response,false,includeBenchmarks,includeSolvers);
				} else {
					shortName=shortName+"_Hierarchy";
					archive = handleSpace(space, u.getId(), ".zip", response,true,includeBenchmarks,includeSolvers);
				}
			} else if (request.getParameter("type").equals("proc")) {
				List<Processor> proc=null;
				shortName="Processor";
				if (request.getParameter("procClass").equals("post")) {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.POST);
				} else {
					proc=Processors.getByCommunity(Integer.parseInt(request.getParameter("id")), Processor.ProcessorType.BENCH);
				}
				archive=handleProc(proc,u.getId(),".zip",Integer.parseInt(request.getParameter("id")) , response);
			} else if (request.getParameter("type").equals("j_outputs")) {
				int jobId=Integer.parseInt(request.getParameter("id"));
				
				String lastSeen=request.getParameter("since");
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
					System.out.println("found since = "+lastSeen);
				}
				shortName="Job"+jobId+"_output";
				archive = handleJobOutputs(jobId, u.getId(), ".zip", response,since);
			}
			// Redirect based on success/failure
			if(archive != null) {
				Object check=request.getParameter("token");
				shortName=shortName.replaceAll("\\s+",""); //get rid of all whitespace, which we cannot include in the header correctly
				//token is used to tell the client when the file has arrived
				if (check!=null) {
					String token=check.toString();
					Cookie newCookie=new Cookie("fileDownloadToken", token);
					newCookie.setMaxAge(60);
					response.addCookie(newCookie);
				}
				
				if (!request.getParameter("type").equals("j_outputs")) {
					FileInputStream stream=new FileInputStream(archive);
					
					response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
					log.debug("measuing the size of the file to be returned");
					long size=FileUtils.sizeOf(archive);
					log.debug("the size of the file being returned is "+size);

					response.addHeader("Content-Length",String.valueOf(size));
					log.debug("copying input stream into servlet output stream");
					IOUtils.copyLarge(stream, response.getOutputStream());
					log.debug("done copying input stream into servlet output stream");
					stream.close();
				}
				response.getOutputStream().close();
				return;
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to process file for download.");	
			}									
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
	private static File handleSolver(Solver s, int userId, String format, HttpServletResponse response, boolean reupload) throws IOException {
		log.info("handleSolver");
		
		String baseName = s.getName();
		// If we can see this solver AND the solver is downloadable...

		if (Permissions.canUserSeeSolver(s.getId(), userId) && (s.isDownloadable() || s.getUserId()==userId)) {
				String cachedFilePath=null;
				if(!reupload) {
					cachedFilePath=Cache.getCache(s.getId(),CacheType.CACHE_SOLVER);
				} else {
					cachedFilePath=Cache.getCache(s.getId(),CacheType.CACHE_SOLVER_REUPLOAD);
				}
				
				//if the entry was in the cache, we can just return it.
				if (cachedFilePath!=null) {
					File cachedFile = new File(cachedFilePath);
					
					log.debug("returning a cached file!");
					return cachedFile;
				}
			
			
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it, and the directory it will be placed in
			String fileName = s.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();

			//String path = s.getPath();
			//int index = path.lastIndexOf(File.separator);
			//String tempdest = path.substring(index);

			//File tempDir = new File(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR + UUID.randomUUID().toString() + File.separator + s.getName() + tempdest);
			//tempDir.mkdirs();
			//copySolverFile(s.getPath(), tempDir.getAbsolutePath(), description);

			ArchiveUtil.createArchive(new File(s.getPath()), uniqueDir, format, baseName, reupload);
			if (!reupload) {
				Cache.setCache(s.getId(),CacheType.CACHE_SOLVER,uniqueDir,fileName);
			} else {
				Cache.setCache(s.getId(),CacheType.CACHE_SOLVER_REUPLOAD,uniqueDir,fileName);
			}
			//We return the fileName so the browser can redirect straight to it
			return uniqueDir;
		}
		else {
			//response.sendError(HttpServletResponse.SC_FORBIDDEN, "you do not have permission to download this solver.");
		}

		return null;
	}

	private static void copySolverFile(String path, String dest, String description) throws IOException{				
		File tempSrcFile = new File(path);
		File tempDestFile = new File(dest);
		tempDestFile.mkdirs();

		File tempDescFile = new File(dest + File.separator + R.DESC_PATH);

		FileUtils.copyDirectory(tempSrcFile, tempDestFile);

		int index = dest.lastIndexOf(File.separator);
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


	/**
	 * Handles requests for downloading post processors for a given community
	 * @return a file representing the archive to send back to the client
	 * @author Eric Burns
	 */

	private static File handleProc(List<Processor> procs, int userId, String format, int spaceId, HttpServletResponse response) throws IOException {

		if (Permissions.canUserSeeSpace(spaceId, userId)) {

			String fileName="Community "+String.valueOf(spaceId)+"Procs" + "_("+UUID.randomUUID().toString()+")" +format;
			File uniqueDir=new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			List<File> files=new LinkedList<File>();
			for (Processor x : procs) {
				files.add(new File(x.getFilePath()));
			}
			ArchiveUtil.createArchive(files, uniqueDir, format);

			return uniqueDir;
		}
		return null;
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
	private static File handleBenchmark(Benchmark b, int userId, String format, HttpServletResponse response) throws IOException {
		// If we can see this benchmark AND the benchmark is downloadable...
		if (Permissions.canUserSeeBench(b.getId(), userId) && (b.isDownloadable() || b.getUserId()==userId)) {
			String cachedFilePath=Cache.getCache(b.getId(),CacheType.CACHE_BENCHMARK);
			//if the entry was in the cache, make sure the file actually exists
			if (cachedFilePath!=null) {
				File cachedFile = new File(cachedFilePath);

				return cachedFile;
				
			}
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = b.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			ArchiveUtil.createArchive(new File(b.getPath()), uniqueDir, format, false);
			Cache.setCache(b.getId(),CacheType.CACHE_BENCHMARK,uniqueDir,fileName);
			return uniqueDir;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this benchmark.");
		}

		return null;
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

	private static File handleSpaceXML(Space space, int userId, String format, HttpServletResponse response) throws Exception {
		log.debug("Space XML download called");
		Queue<String> descriptions=new LinkedList<String>();
		descriptions.add(space.getDescription());
		List<Space> children = space.getSubspaces();
		for (Space child : children) {
			descriptions.add(child.getDescription());
		}

		// If we can see this Space
		if (Permissions.canUserSeeSpace(space.getId(), userId)) {
			
				String cachedFilePath=null;
				cachedFilePath=Cache.getCache(space.getId(),CacheType.CACHE_SPACE_XML);
				//if the entry was in the cache, make sure the file actually exists
				if (cachedFilePath!=null) {
					File cachedFile = new File(cachedFilePath);
					//it might have been cleared if it has been there too long, so make sure that hasn't happened
					
						return cachedFile;
					
				}
			
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
			if (Spaces.isPublicHierarchy(space.getId())) {
				log.debug("storing space hierarchy in the cache");
				Cache.setCache(space.getId(), CacheType.CACHE_SPACE_XML,uniqueDir, fileName);
			}
			return uniqueDir;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
		}

		return null;
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
	
	private static File handlePairOutput(JobPair jp, int userId, String format, HttpServletResponse response) throws IOException {    	
		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(jp.getJobId(), userId)) {
			
			String cachedFilePath=Cache.getCache(jp.getId(),CacheType.CACHE_JOB_PAIR);
			
			//if the entry was in the cache, make sure the file actually exists
			if (cachedFilePath!=null) {
				File cachedFile = new File(cachedFilePath);
				//it might have been cleared if it has been there too long, so make sure that hasn't happened
				log.debug("returning a cached file!");
				return cachedFile;
				
			}
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();

			String outputPath = JobPairs.getFilePath(jp.getId());  
			log.info("The download output path is " + outputPath);
			ArchiveUtil.createArchive(new File(outputPath), uniqueDir, format, false);
			Cache.setCache(jp.getId(), CacheType.CACHE_JOB_PAIR, uniqueDir, fileName);
			return uniqueDir;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
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
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static File handleJob(Integer jobId, int userId, String format, HttpServletResponse response, Integer since, Boolean returnIds) throws IOException {    	
		log.info("Request for job " + jobId + " csv from user " + userId);
		boolean jobComplete=Jobs.isJobComplete(jobId);
		if (Permissions.canUserSeeJob(jobId, userId)) {
			if (jobComplete && since==null) {
				String cachedFilePath = null;
				if (returnIds) {
					cachedFilePath=Cache.getCache(jobId, CacheType.CACHE_JOB_CSV);
				} else {
					cachedFilePath=Cache.getCache(jobId,CacheType.CACHE_JOB_CSV_NO_IDS);
				}
				if (cachedFilePath!= null) {
					File cachedFile = new File(cachedFilePath);
					log.debug("returning a cached file!");
					return cachedFile;
					
				}
			}
			Job job;
			if (since==null) {
				job = Jobs.getDetailed(jobId);
			} else {
				job=Jobs.getDetailed(jobId,since);

				//we want to find the largest completion ID seen and send that back to the client
				//so that they know what to ask for next time (mostly for StarexecCommand)
				int maxCompletion=since;
				for (JobPair x : job.getJobPairs()) {
					if (x.getCompletionId()>maxCompletion) {
						maxCompletion=x.getCompletionId();
					}
				}
				response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
				try {
					if (jobComplete) {
						response.addCookie(new Cookie("Job-Complete","true"));
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			log.debug("about to create a job CSV with "+job.getJobPairs().size()+" pairs");
			String jobFile = CreateJobCSV(job, returnIds);
			ArchiveUtil.createArchive(new File(jobFile), uniqueDir, format, false);
			if (returnIds && jobComplete) {
				Cache.setCache(jobId, CacheType.CACHE_JOB_CSV,uniqueDir, fileName);
			}
			return uniqueDir;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
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
	private static String CreateJobCSV(Job job, Boolean returnIds) throws IOException {
		log.debug("CreateJobCSV called with returnIds set to "+returnIds);
		StringBuilder sb = new StringBuilder();
		sb.delete(0, sb.length());
		sb.append(R.NEW_JOB_OUTPUT_DIR);
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
	private static File handleJobOutputs(int jobId, int userId, String format, HttpServletResponse response, Integer since) throws Exception {    	
		log.debug("got request to download output for job = "+jobId);
		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(jobId, userId)) {
			log.debug("confirmed user can download job = "+jobId);
			boolean jobComplete=Jobs.isJobComplete(jobId);
			if (jobComplete && since==null) { //there is no cache for partial results
				String cachedFilePath=null;
				cachedFilePath=Cache.getCache(jobId,CacheType.CACHE_JOB_OUTPUT);
				log.debug("checked in cache for job = "+jobId);

				//if the entry was in the cache, make sure the file actually exists
				if (cachedFilePath!=null) {
					File cachedFile = new File(cachedFilePath);
					//it might have been cleared if it has been there too long, so make sure that hasn't happened
					log.debug("returning a cached file for job = "+jobId);
					return cachedFile;
					
				}
			}

			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);

			uniqueDir.createNewFile();
			
			File file, dir;
			
			//if we only want the new job pairs
			if (since!=null) {
				log.debug("starting to get parital results for job id = "+jobId);
				List<JobPair> pairs;
				File tempDir=new File(new File(R.STAREXEC_ROOT,R.DOWNLOAD_FILE_DIR),fileName+"temp");
				tempDir.mkdir();
				log.debug("Getting incremental job output results");
				
				pairs=Jobs.getNewCompletedPairsShallow(jobId, since);
				
				log.debug("Found "+ pairs.size()  + " new pairs");
				int maxCompletion=since;
				for (JobPair x : pairs) {
					if (x.getCompletionId()>maxCompletion) {
						maxCompletion=x.getCompletionId();
					}
				}
				response.addCookie(new Cookie("Max-Completion",String.valueOf(maxCompletion)));
				log.debug("added the max-completion cookie, starting to write output for job id = "+jobId);
				//5 minutes for this loop-- this is easily the slowest part of getting incremental results
				for (JobPair jp : pairs) {
					file=new File(JobPairs.getFilePath(jp));

					if (file.exists()) {
						//store in the old format because the pair has no path
						if (jp.getPath()==null) {
							dir=new File(tempDir,jp.getSolver().getName());
							dir=new File(dir,jp.getConfiguration().getName());
							dir.mkdirs();
						} else {
							String path=jp.getPath();

							String [] spaces=path.split("/");
							dir=new File(tempDir,spaces[0]);
							
							for (int index=1;index<spaces.length;index++) {
								dir=new File(dir,spaces[index]);
							}
							dir=new File(dir,jp.getSolver().getName());
							dir=new File(dir,jp.getConfiguration().getName());
							dir.mkdirs();
						}
						FileUtils.copyFileToDirectory(file,dir);

					}
				}
				log.debug("finished copying new job pairs-- starting to make archive for job id = "+jobId);
				ArchiveUtil.createArchive(tempDir, uniqueDir, format,"Job"+String.valueOf(jobId)+"_output_new",false);
				log.debug("archive written for job id ="+jobId);
			} else {
				log.debug("preparing to create archive for job = "+jobId);
				ArchiveUtil.createAndOutputZip(new File(Jobs.getDirectory(jobId)),response.getOutputStream());
				//ArchiveUtil.createArchive(new File(Jobs.getDirectory(jobId)), uniqueDir, format,"Job"+String.valueOf(jobId)+"_output",false);
				log.debug("archive created for job = "+jobId);

				if (jobComplete) {
					Cache.setCache(jobId,CacheType.CACHE_JOB_OUTPUT,uniqueDir, fileName);
				}
			}

			//if there are no pending pairs, the job is done
			try {
				if (jobComplete) {
					response.addCookie(new Cookie("Job-Complete","true"));
				}
			} catch (Exception e) {
				log.error(e);
			}
			return uniqueDir;
		}

		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this job pair's output.");
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
	 * @param includeBenchmarks Whether to include benchmarks in the directory
	 * @param includeSolvers Whether to include solvers in the directory
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang + Eric Burns
	 */
	private File handleSpace(Space space, int uid, String format, HttpServletResponse response,boolean hierarchy, boolean includeBenchmarks,boolean includeSolvers) throws IOException {
		// If we can see this space AND the space is downloadable...

		if (Permissions.canUserSeeSpace(space.getId(), uid)) {	
			//we are only caching hierarchies with benchmarks + solvers so far
			if (includeBenchmarks && includeSolvers) {
				String cachedFilePath=null;
				if (hierarchy) {
					cachedFilePath=Cache.getCache(space.getId(),CacheType.CACHE_SPACE_HIERARCHY);
				} else {
					cachedFilePath=Cache.getCache(space.getId(),CacheType.CACHE_SPACE);
				}
				//if the entry was in the cache, we can return it
				if (cachedFilePath!=null) {
					File cachedFile = new File(cachedFilePath);
					log.debug("returning a cached file!");
					return cachedFile;
					
				}
			}
			
			String baseFileName=space.getName();
			String fileName = space.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR + File.separator), fileName);
			uniqueDir.createNewFile();
			File tempDir = new File(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR + UUID.randomUUID().toString() + File.separator + space.getName());
			
			storeSpaceHierarchy(space, uid, tempDir.getAbsolutePath(), includeBenchmarks,includeSolvers,hierarchy,null);
			ArchiveUtil.createArchive(tempDir, uniqueDir, format, baseFileName, false);
			if(tempDir.exists()){
				tempDir.delete();
			}
			
			if (includeBenchmarks && includeSolvers) {
				if (hierarchy) {
					Cache.setCache(space.getId(),CacheType.CACHE_SPACE_HIERARCHY,uniqueDir, fileName);
				} else {
					Cache.setCache(space.getId(),CacheType.CACHE_SPACE,uniqueDir, fileName);
				}
			}
			return uniqueDir;
		}
		else {
			//response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
		}
		return null;
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
