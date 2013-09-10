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
import org.starexec.data.database.Statistics;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.CacheType;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
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
				shortName="Job"+jobId+"_CSV";
				archive = handleJob(jobId, u.getId(), ".zip", response, since,ids);
			} else if (request.getParameter("type").equals("j_outputs")) {
				Job job = Jobs.getDetailed(Integer.parseInt(request.getParameter("id")));
				String lastSeen=request.getParameter("since");
				Integer since=null;
				if (lastSeen!=null) {
					since=Integer.parseInt(lastSeen);
				}
				shortName="Job"+job.getId()+"_Output";
				archive = handleJobOutputs(job, u.getId(), ".zip", response,since);
			} else if (request.getParameter("type").equals("space")) {
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
				FileInputStream stream=new FileInputStream(archive);
				response.addHeader("Content-Disposition", "attachment; filename="+shortName+".zip");
				long size=IOUtils.copyLarge(stream, response.getOutputStream());
				response.addHeader("Content-Length",String.valueOf(size));	
				response.getOutputStream().close();
				
				stream.close();
				log.debug("ready to send back file "+shortName+".zip");
				return;
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
	 * @return a file representing the archive to send back to the client
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	private static File handleSolver(Solver s, int userId, String format, HttpServletResponse response, boolean reupload) throws IOException {
		log.info("handleSolver");
		String description = s.getDescription();
		String baseName = s.getName();
		// If we can see this solver AND the solver is downloadable...

		if (Permissions.canUserSeeSolver(s.getId(), userId) && (s.isDownloadable() || s.getUserId()==userId)) {
				String cachedFileName=null;
				if(!reupload) {
					cachedFileName=Cache.getCache(s.getId(),CacheType.CACHE_SOLVER);
				} else {
					cachedFileName=Cache.getCache(s.getId(),CacheType.CACHE_SOLVER_REUPLOAD);
				}
				
				//if the entry was in the cache, make sure the file actually exists
				if (cachedFileName!=null) {
					
					File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
					//it might have been cleared if it has been there too long, so make sure that hasn't happened
					if (cachedFile.exists()) {
						//it's there, so give back the name
						log.debug("returning a cached file!");
						return cachedFile;
					} else {
						log.warn("a cached file did not exist when it should have!");
						Cache.invalidateCache(s.getId(),CacheType.CACHE_SOLVER);
					}
				}
			
			
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it, and the directory it will be placed in
			String fileName = s.getName() + "_(" + UUID.randomUUID().toString() + ")" + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();

			String path = s.getPath();
			int index = path.lastIndexOf(File.separator);
			String tempdest = path.substring(index);

			File tempDir = new File(R.STAREXEC_ROOT + R.DOWNLOAD_FILE_DIR + UUID.randomUUID().toString() + File.separator + s.getName() + tempdest);
			tempDir.mkdirs();
			copySolverFile(s.getPath(), tempDir.getAbsolutePath(), description);

			ArchiveUtil.createArchive(tempDir, uniqueDir, format, baseName, reupload);
			if (!reupload) {
				Cache.setCache(s.getId(),CacheType.CACHE_SOLVER,uniqueDir,fileName);
			} else {
				Cache.setCache(s.getId(),CacheType.CACHE_SOLVER_REUPLOAD,uniqueDir,fileName);
			}
			//We return the fileName so the browser can redirect straight to it
			return uniqueDir;
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
			String cachedFileName=Cache.getCache(b.getId(),CacheType.CACHE_BENCHMARK);
			//if the entry was in the cache, make sure the file actually exists
			if (cachedFileName!=null) {
				File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
				//it might have been cleared if it has been there too long, so make sure that hasn't happened
				if (cachedFile.exists()) {
					//it's there, so give back the name
					log.debug("returning a cached file!");
					return cachedFile;
				} else {
					log.warn("a cached file did not exist when it should have!");
					Cache.invalidateCache(b.getId(),CacheType.CACHE_BENCHMARK);
				}
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
			
				String cachedFileName=null;
				cachedFileName=Cache.getCache(space.getId(),CacheType.CACHE_SPACE_XML);
				//if the entry was in the cache, make sure the file actually exists
				if (cachedFileName!=null) {
					File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
					//it might have been cleared if it has been there too long, so make sure that hasn't happened
					if (cachedFile.exists()) {
						//it's there, so give back the name
						log.debug("returning a cached file!");
						return cachedFile;
					} else {
						log.warn("a cached file did not exist when it should have!");
						Cache.invalidateCache(space.getId(),CacheType.CACHE_SPACE_XML);
					}
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
	 * @return a file representing the archive to send back to the client
	 * @author Tyler Jensen
	 */
	
	//TODO: Create new functions for only getting back shallow information about the pairs instead of deep info.
	private static File handlePairOutput(JobPair jp, int userId, String format, HttpServletResponse response) throws IOException {    	
		Job j = Jobs.getShallow(jp.getJobId());

		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(j.getId(), userId)) {
			
			String cachedFileName=null;
			
			cachedFileName=Cache.getCache(jp.getId(),CacheType.CACHE_JOB_PAIR);
			
			//if the entry was in the cache, make sure the file actually exists
			if (cachedFileName!=null) {
				File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
				//it might have been cleared if it has been there too long, so make sure that hasn't happened
				if (cachedFile.exists()) {
					//it's there, so give back the name
					log.debug("returning a cached file!");
					return cachedFile;
				} else {
					log.warn("a cached file did not exist when it should have!");
					Cache.invalidateCache(jp.getId(),CacheType.CACHE_SPACE);
				}
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
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static File handleJob(Integer jobId, int userId, String format, HttpServletResponse response, Integer since, Boolean returnIds) throws IOException {    	
		log.info("Request for job " + jobId + " csv from user " + userId);
		if (Permissions.canUserSeeJob(jobId, userId)) {
			String cachedFileName = null;
			if (returnIds) {
				cachedFileName=Cache.getCache(jobId, CacheType.CACHE_JOB_CSV);
			} else {
				cachedFileName=Cache.getCache(jobId,CacheType.CACHE_JOB_CSV_NO_IDS);
			}
			if (cachedFileName!= null) {
				File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
				//it might have been cleared if it has been there too long, so make sure that hasn't happened
				if (cachedFile.exists()) {
					//it's there, so give back the name
					log.debug("returning a cached file!");
					return cachedFile;
				} else {
					log.warn("a cached file did not exist when it should have!");
					Cache.invalidateCache(jobId,CacheType.CACHE_JOB_CSV);
				}
			}

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
				try {
					if (Statistics.getJobPairOverview(job.getId()).get("pendingPairs").equals("0")) {
						response.addCookie(new Cookie("Job-Complete","true"));
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);
			uniqueDir.createNewFile();
			String jobFile = CreateJobCSV(job, returnIds);
			ArchiveUtil.createArchive(new File(jobFile), uniqueDir, format, false);
			if (returnIds) {
				Cache.setCache(jobId, CacheType.CACHE_JOB_CSV,uniqueDir, fileName);
			}
			return uniqueDir;
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
	private static String CreateJobCSV(Job job, Boolean returnIds) throws IOException {
		log.debug("CreateJobCSV called with returnIds set to "+returnIds);
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
		if (!returnIds) {
			sb.append("benchmark,solver,configuration,status,time(s),result");
		} else {
			sb.append("pair id, benchmark,benchmark id, solver,solver id,configuration,configuration id,status,time(s),result");
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
			log.debug("I FOUND A PAIR GUYZ!\n\n\n\n");
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
			sb.append(pair.getSolver().getConfigurations().get(0).getName());
			sb.append(",");
			if (returnIds) {
				sb.append(pair.getSolver().getConfigurations().get(0).getId());
				sb.append(",");
			}
			sb.append(pair.getStatus().toString());

			if (pair.getStatus().getCode() == StatusCode.STATUS_COMPLETE) {
				sb.append(",");
				sb.append((pair.getWallclockTime()));
				sb.append(",");
				sb.append(pair.getStarexecResult());
			}
			else {
				sb.append(",-,-");
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
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static File handleJobOutputs(Job j, int userId, String format, HttpServletResponse response, Integer since) throws IOException {    	

		// If the user can actually see the job the pair is apart of
		if (Permissions.canUserSeeJob(j.getId(), userId)) {
			
			String cachedFileName=null;
			
			cachedFileName=Cache.getCache(j.getId(),CacheType.CACHE_JOB_OUTPUT);
			//if the entry was in the cache, make sure the file actually exists
			if (cachedFileName!=null) {
				File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
				//it might have been cleared if it has been there too long, so make sure that hasn't happened
				if (cachedFile.exists()) {
					//it's there, so give back the name
					log.debug("returning a cached file!");
					return cachedFile;
				} else {
					log.warn("a cached file did not exist when it should have!");
					Cache.invalidateCache(j.getId(),CacheType.CACHE_JOB_OUTPUT);
				}
			}
			
			
			// Path is /starexec/WebContent/secure/files/{random name}.{format}
			// Create the file so we can use it
			String fileName = UUID.randomUUID().toString() + format;
			File uniqueDir = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), fileName);

			uniqueDir.createNewFile();
			

			File file;
			File dir;
			
			//if we only want the new job pairs
			List<JobPair> pairs;
			if (since!=null) {
				File tempDir=new File(new File(R.STAREXEC_ROOT,R.DOWNLOAD_FILE_DIR),fileName+"temp");
				tempDir.mkdir();
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
				for (JobPair jp : pairs) {
					file=new File(JobPairs.getFilePath(jp));

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
				ArchiveUtil.createArchive(tempDir, uniqueDir, format,"new_output_"+String.valueOf(j.getId()),false);
			} else {
				String test=Jobs.getDirectory(j.getId());
				log.debug("this is the filepath we have "+test);
				ArchiveUtil.createArchive(new File(Jobs.getDirectory(j.getId())), uniqueDir, format,"output_"+String.valueOf(j.getId()),false);
				Cache.setCache(j.getId(),CacheType.CACHE_JOB_OUTPUT,uniqueDir, fileName);
			}

			//if there are no pending pairs, the job is done
			try {
				if (Statistics.getJobPairOverview(j.getId()).get("pendingPairs").equals("0")) {
					response.addCookie(new Cookie("Job-Complete","true"));
				}
			} catch (Exception e) {
				log.error(e);
			}
			return uniqueDir;
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
				String cachedFileName=null;
				if (hierarchy) {
					cachedFileName=Cache.getCache(space.getId(),CacheType.CACHE_SPACE_HIERARCHY);
				} else {
					cachedFileName=Cache.getCache(space.getId(),CacheType.CACHE_SPACE);
				}
				//if the entry was in the cache, make sure the file actually exists
				if (cachedFileName!=null) {
					File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), cachedFileName);
					//it might have been cleared if it has been there too long, so make sure that hasn't happened
					if (cachedFile.exists()) {
						//it's there, so give back the name
						log.debug("returning a cached file!");
						return cachedFile;
					} else {
						log.warn("a cached file did not exist when it should have!");
						Cache.invalidateCache(space.getId(),CacheType.CACHE_SPACE);
					}
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
			
			if (Spaces.isPublicHierarchy(space.getId()) && includeBenchmarks && includeSolvers) {
				if (hierarchy) {
					Cache.setCache(space.getId(),CacheType.CACHE_SPACE_HIERARCHY,uniqueDir, fileName);
				} else {
					Cache.setCache(space.getId(),CacheType.CACHE_SPACE,uniqueDir, fileName);
				}
			}
			return uniqueDir;
		}
		else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you do not have permission to download this space.");
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
				File benchmarkDir=new File(tempDir,"benchmarks");
				benchmarkDir.mkdirs();
				for(Benchmark b: benchList){
					if(b.isDownloadable() || b.getUserId()==uid ){
						FileUtils.copyFileToDirectory(new File(b.getPath()), benchmarkDir);
					
					}
				}
			}
			
			if (includeSolvers) {
				List<Solver> solverList=null;
				//if we're getting a full hierarchy and the solver path is
				//not yet set, we want to store all solvers now and then link them later
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
				} else {
					for (Solver s : solverList) {
						//File existingSolver=new File(solverPath,s.getName()+s.getId());
						//File linkDir=new File(solverDir,s.getName()+s.getId());
						//not working currently, for now, just don't put solvers in the individual spaces
						//Files.createSymbolicLink(Paths.get(linkDir.getAbsolutePath()), Paths.get(existingSolver.getAbsolutePath()));
					}
					
				}
				
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


			List<Space> subspaceList = Spaces.getSubSpaces(space.getId(), uid, false);
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
