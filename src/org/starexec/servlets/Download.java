package org.starexec.servlets;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.FileUtils;
import org.starexec.constants.R;
import org.starexec.constants.Web;
import org.starexec.data.database.*;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/**
 * Handles requests to download files from starexec
 *
 * @author Skylar Stark & Tyler Jensen
 */
public class Download extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(Download.class);
	private static final String PARAM_TYPE = "type";
	private static final String PARAM_ID = "id";
	private static final String PARAM_ANON_ID = "anonId";
	private static final String PARAM_REUPLOAD = "reupload";

	private static Optional<Solver> handleSolverAndSolverSrc(HttpServletRequest request, HttpServletResponse response)
			throws IOException, SQLException {
		final String methodName = "handleSolverAndSolverSrc";
		Solver s;
		String universallyUniqueId = request.getParameter(PARAM_ANON_ID);
		if (universallyUniqueId == null) {
			log.debug(methodName, "Was not anonymous download.");
			int solverId = Integer.parseInt(request.getParameter(PARAM_ID));
			log.debug(methodName, "Getting solver with id " + solverId);
			s = Solvers.get(solverId);
		} else {
			log.debug(methodName, "Was anonymous download. UUID: " + universallyUniqueId);
			Optional<Integer> solverId = AnonymousLinks.getIdOfSolverAssociatedWithLink(universallyUniqueId);
			if (solverId.isPresent()) {
				log.debug(methodName, "Getting solver with id " + solverId.get());
				s = Solvers.get(solverId.get());
			} else {
				log.debug(methodName, "Could not get solver for anonymous download.");
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver not found.");
				return Optional.empty();
			}
		}
		String shortName = s.getName();
		shortName = shortName
				.replaceAll("\\s+", ""); //get rid of all whitespace, which we cannot include in the header correctly
		response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
		return Optional.of(s);
	}

	/**
	 * Processes a solver to be downloaded. The solver is archived in a format that is specified by the user, given a
	 * random name, and placed in a secure folder on the server.
	 *
	 * @param s the solver to be downloaded
	 * @return a file representing the archive to send back to the client
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	private static boolean handleSolver(Solver s, HttpServletResponse response, boolean reupload) throws Exception {
		String baseName = s.getName();
		ArchiveUtil.createAndOutputZip(new File(s.getPath()), response.getOutputStream(), baseName, reupload);
		return true;
	}

	/**
	 * Processes a solver source code request to be downloaded. The solver source is archived in a format that is
	 * specified by the user, given a random name, and placed in a secure folder on the server.
	 *
	 * @param s the solver to be downloaded
	 * @return a file representing the archive to send back to the client
	 * @author Skylar Stark & Wyatt Kaiser & Andrew Lubinus
	 */
	private static boolean handleSolverSource(Solver s, HttpServletResponse response) throws Exception {
		String baseName = s.getName();
		ArchiveUtil.createAndOutputZip(new File(s.getPath() + "_src"), response.getOutputStream(), baseName, false);
		return true;
	}

	/**
	 * Handles requests for downloading post processors for a given community
	 *
	 * @return a file representing the archive to send back to the client
	 * @author Eric Burns
	 */

	private static boolean handleProc(List<Processor> procs, HttpServletResponse response) throws Exception {
		final String methodName = "handleProc";
		log.entry(methodName);

		List<File> files = new LinkedList<>();
		for (Processor x : procs) {
			File newProc = new File(x.getFilePath());
			if (newProc.exists()) {
				files.add(newProc);
			} else {
				log.warn(methodName, "processor with id = " + x.getId() + " exists in the database but not on disk");
			}
		}
		if (files.isEmpty()) {
			log.warn(methodName, "Didn't find any files on disk.");
			return false;
		}

		log.debug(methodName, "Outputting zip of processors.");
		ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), "processors", true /* uniquify names */);
		return true;
	}

	/**
	 * Processes a benchmark to be downloaded. The benchmark is archived in a format that is specified by the user,
	 * given a random name, and placed in a secure folder on the server.
	 *
	 * @param b the benchmark to be downloaded
	 * @return a file representing the archive to send back to the client
	 * @author Skylar Stark
	 */
	private static boolean handleBenchmark(Benchmark b, HttpServletResponse response) throws Exception {
		ArchiveUtil.createAndOutputZip(new File(b.getPath()), response.getOutputStream(), "", false);
		return true;
	}

	/**
	 * Processes a job xml file to be downloaded.
	 *
	 * @param job the job to be downloaded
	 * @param userId the id of the user making the download request
	 * @return a file representing the archive to send back to the client
	 * @throws Exception
	 * @author Julio Cervantes
	 */
	private static boolean handleJobXML(Job job, int userId, HttpServletResponse response) throws Exception {

		// If we can see this
		List<File> files = new ArrayList<>();
		log.debug("Permission to download XML granted");

		JobToXMLer handler = new JobToXMLer();
		File file = handler.generateXMLfile(job, userId);

		files.add(file);

		String baseFileName = "Job" + job.getId() + "_XML";

		File schema = new File(R.STAREXEC_ROOT + File.separator + R.JOB_SCHEMA_LOCATION);
		files.add(schema);

		ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), baseFileName);

		return true;
	}

	/**
	 * Processes a space xml file to be downloaded. The xml file and the space hierarchy xml schema is archived in a
	 * format that is specified by the user, given a random name, and placed in a secure folder on the server.
	 *
	 * @param space the space to be downloaded
	 * @param userId the id of the user making the download request
	 * @return a file representing the archive to send back to the client
	 * @throws Exception
	 * @author Benton McCune
	 */

	private static boolean handleSpaceXML(
			Space space, int userId, HttpServletResponse response, boolean includeAttributes, boolean updates, int upid
	) throws Exception {

		// If we can see this Space
		List<File> files = new ArrayList<>();
		log.debug("Permission to download XML granted, includeAttributes = " + includeAttributes);
		BatchUtil butil = new BatchUtil();
		File file = butil.generateXMLfile(Spaces.getDetails(space.getId(), userId), userId, includeAttributes, updates,
		                                  upid
		);

		files.add(file);
		String baseFileName = space.getName() + "_XML";

		File schema = new File(R.STAREXEC_ROOT + "/" + R.SPACE_XML_SCHEMA_RELATIVE_LOC);
		files.add(schema);

		ArchiveUtil.createAndOutputZip(files, response.getOutputStream(), baseFileName);

		return true;
	}

	/**
	 * @param pairIds
	 * @param userId
	 * @param response
	 * @return
	 * @throws Exception
	 */
	private static boolean handlePairOutputs(
			List<Integer> pairIds, int userId, HttpServletResponse response, Boolean longPath
	) {
		List<JobPair> pairs = new ArrayList<>();
		Job j = null;
		final String methodName = "handlePairOutputs";
		log.entry(methodName);

		for (Integer id : pairIds) {
			JobPair jp = JobPairs.getPair(id);
			if (jp == null) {
				return false;
			}
			pairs.add(jp);
			if (j == null) {
				j = Jobs.get(jp.getJobId());
				//make sure the user can see the job
				if (!Permissions.canUserSeeJob(j.getId(), userId).isSuccess()) {
					return false;
				}
			} else {
				//for now, only get pairs if they are part of one job
				if (j.getId() != jp.getJobId()) {
					return false;
				}
			}
		}

		String baseName = "Job" + String.valueOf(j.getId()) + "_output";

		Download.addJobPairsToZipOutput(pairs, response, baseName, longPath, null);
		log.exit(methodName);
		return true;
	}

	/**
	 * Processes a job pair's output to be downloaded.
	 *
	 * @param pairId the job pair whose output is to be downloaded
	 * @param userId the id of the user making the download request
	 * @param response
	 * @param longPath directory structure is long version
	 * @return a boolean for whether or not this succeeded
	 */

	private static boolean handlePairOutput(int pairId, int userId, HttpServletResponse response, Boolean longPath) {
		//ArchiveUtil.createAndOutputZip(JobPairs.getOutputPaths(pairId), response.getOutputStream(), "");
		//return true;
		return handlePairOutputs(Collections.singletonList(pairId), userId, response, longPath);
	}

	/**
	 * Processes a job csv file to be downloaded. The file contains the information of all the job pairs within the
	 * specific job, given a random name, and placed in a secure folder on the server.
	 *
	 * @param jobId the job needed to be processed.
	 * @param userId the Id of the user who sends the request for the file.
	 * @param response the servlet response sent back.
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static boolean handleJob(
			Integer jobId, int userId, HttpServletResponse response, Integer since, Boolean returnIds,
			Boolean onlyCompleted
	) throws Exception {
		log.info("Request for job " + jobId + " csv from user " + userId);

		Job job = Jobs.get(jobId);
		HashMap<Integer, HashMap<Integer, Properties>> props = null;
		if (since == null) {
			job.setJobPairs(Jobs.getJobPairsInJobSpaceHierarchy(job.getPrimarySpace(), PrimitivesToAnonymize.NONE));
			props = Jobs.getJobAttributes(jobId);
		} else {
			job.setJobPairs(
					Jobs.getJobPairsInJobSpaceHierarchy(job.getPrimarySpace(), since, PrimitivesToAnonymize.NONE));
			props = Jobs.getNewJobAttributes(jobId, since);
			int olderPairs = Jobs.countOlderPairs(jobId, since);

			log.debug("found this many new job pairs " + job.getJobPairs().size());
			//we want to find the largest completion ID seen and send that back to the client
			//so that they know what to ask for next time (mostly for StarexecCommand)
			int maxCompletion = since;
			for (JobPair x : job.getJobPairs()) {
				log.trace("found pair id = " + x.getId() + " with completion id = " + x.getCompletionId());
				if (x.getCompletionId() > maxCompletion) {
					maxCompletion = x.getCompletionId();
				}
			}

			response.addCookie(new Cookie("Max-Completion", String.valueOf(maxCompletion)));
			response.addCookie(new Cookie("Pairs-Found", String.valueOf(job.getJobPairs().size())));
			response.addCookie(new Cookie("Older-Pairs", String.valueOf(olderPairs)));
			response.addCookie(new Cookie("Total-Pairs", String.valueOf(Jobs.getPairCount(jobId))));
		}
		Jobs.loadPropertiesIntoPairs(job.getJobPairs(), props);
		log.debug("about to create a job CSV with " + job.getJobPairs().size() + " pairs");
		String jobFile = CreateJobCSV(job, returnIds, onlyCompleted);
		ArchiveUtil.createAndOutputZip(new File(jobFile), response.getOutputStream(), "Job"+jobId, false);

		return true;
	}

	/**
	 * Create the csv file for a specific job
	 *
	 * @param job the job needed to be processed
	 * @return the file name of the created csv file
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static String CreateJobCSV(Job job, Boolean returnIds, Boolean getOnlyCompleted) throws IOException {
		log.debug("CreateJobCSV called with returnIds set to " + returnIds);
		StringBuilder sb = new StringBuilder();
		sb.append(R.STAREXEC_ROOT).append(R.DOWNLOAD_FILE_DIR);
		sb.append(File.separator);
		int maxStageNumbers = 0;
		for (JobPair jp : job) {
			maxStageNumbers = Math.max(maxStageNumbers, jp.getStages().size());
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
		if (maxStageNumbers > 1) {
			sb.append("stage number,");
		}
		if (returnIds) {
			sb.append(
					"pair id,benchmark,benchmark id,solver,solver id,configuration,configuration id,status,cpu time," +
							"wallclock time,memory usage,result");
		} else {
			sb.append("benchmark,solver,configuration,status,cpu time,wallclock time,memory usage,result");
		}

		HashMap<Integer, String> expectedValues = Jobs.getAllAttrsOfNameForJob(job.getId(), R.EXPECTED_RESULT);
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
			for (String attr : attrNames) {
				if (!attr.equals(R.STAREXEC_RESULT) && !attr.equals(R.EXPECTED_RESULT)) {
					// skip printing result and expected result in the header of the table, since we already included
					// them
					sb.append(",");
					sb.append(attr);
				}
			}
		}
		sb.append("\r\n");

		while (itr.hasNext()) {
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

				if (maxStageNumbers > 1) {
					sb.append(stage.getStageNumber());
					sb.append(",");
				}

				if (returnIds) {
					sb.append(pair.getId());
					sb.append(",");
				}
				if (pair.getPath() != null) {
					sb.append(pair.getPath()).append("/").append(pair.getBench().getName());
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

				//escape commas
				if (stage.getStarexecResult().contains(",")) {
					sb.append("\""+stage.getStarexecResult().replaceAll("\"","\"\"")+"\"");	
				} else {
					sb.append(stage.getStarexecResult());																		}

				if (attrNames != null) {
					// print out attributes for this job pair
					Properties props = stage.getAttributes();

					if (have_expected && props != null) {
						sb.append(",");
						sb.append(props.getProperty(R.EXPECTED_RESULT, "-"));
					}
					for (String attr : attrNames) {
						if (!attr.equals(R.STAREXEC_RESULT) && !attr.equals(R.EXPECTED_RESULT)) {
							/* we skip printing the starexec-result, and starexec-expected-result attributes,
							   because we printed them already */
							sb.append(",");
							sb.append(props.getProperty(attr, "-"));
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
	 * Puts all the given pairs into a zip archive that is streamed into the http response object. The http output
	 * stream is closed at the end
	 *
	 * @param pairs The pairs to output
	 * @param response The HTTP response
	 * @param baseName The top level name to give to the archive
	 * @param useSpacePath If true, pair output will be in a directory including the pair space path. If false, they
	 * will simply be in a flat list of directories with job pair IDs
	 * @param earlyDate Only retrieve files that were modified after the given date, for running job pairs only
	 */
	private static void addJobPairsToZipOutput(
			List<JobPair> pairs, HttpServletResponse response, String baseName, boolean useSpacePath, Long earlyDate
	) {
		long lastModified;
		if (earlyDate != null) {
			lastModified = earlyDate;
		} else {
			lastModified = -1;
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ZipArchiveOutputStream stream = new ZipArchiveOutputStream(buffer);
		try {
			for (JobPair p : pairs) {
				String zipFileNameParent = "";
				StringBuilder zipFileName = new StringBuilder(baseName);
				zipFileName.append(File.separator);
				if (useSpacePath) {
					zipFileName.append(p.getBenchPath());
					zipFileNameParent = zipFileName.toString();
					zipFileName.append(File.separator);
					zipFileName.append(p.getId());
				}
				List<File> files = JobPairs.getOutputPaths(p);
				boolean running = p.getStatus().getCode().running();
				for (File file : files) {
					if (file.exists()) {
						long modified;
						StringBuilder singleFileName;
						if (file.isDirectory()) {
							if (useSpacePath) {
								singleFileName = new StringBuilder(zipFileNameParent);
							} else {
								singleFileName = new StringBuilder(zipFileName);
							}
							//means this is adjacent to a stdout file
							if (files.size() > 1) {
								singleFileName.append(File.separator);
								singleFileName.append(p.getId()).append("_output");
							}
							if (!running || earlyDate == null) {
								modified = ArchiveUtil.addDirToArchive(stream, file, singleFileName.toString());
							} else {
								modified =
										ArchiveUtil.addDirToArchive(stream, file, singleFileName.toString(),
										                            earlyDate);
							}
						} else {
							singleFileName = new StringBuilder(zipFileNameParent);
							singleFileName.append(File.separator);
							singleFileName.append(file.getName());
							if (!running || earlyDate == null) {
								modified = ArchiveUtil.addFileToArchive(stream, file, singleFileName.toString());
							} else {
								modified = ArchiveUtil
										.addFileToArchive(stream, file, singleFileName.toString(), earlyDate);
							}
						}

						if (modified > lastModified) {
							lastModified = modified;
						}
					} else {
						//if we can't find output for the pair, just put an empty file there
						zipFileName.append(".txt");
						ArchiveUtil.addStringToArchive(stream, "", zipFileName.toString());
					}
				}
			}
			if (lastModified == -1 || earlyDate == Long.valueOf(lastModified)) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			} else {
				response.setDateHeader("Last-Modified", lastModified);
				stream.finish();
				buffer.writeTo(response.getOutputStream());
			}
			response.getOutputStream().close();
			stream.close();
		} catch (Exception e) {
			log.error("addJobPairsToZipOutput", e);
		}
	}

	/**
	 * Get a zip file which contains the outputs of a job from all its job pairs.
	 *
	 * @param jobId The job to be handled
	 * @param response The servlet response sent back
	 * @param lastModified The time to use as a cutoff for output for running job pairs
	 * @return a file representing the archive to send back to the client
	 * @throws IOException
	 * @author Ruoyu Zhang
	 */
	private static boolean handleJobOutputs(int jobId, HttpServletResponse response, Integer since, Long lastModified)
			throws Exception {
		log.debug("got request to download output for job = " + jobId);
		// If the user can actually see the job the pair is apart of
		log.debug("confirmed user can download job = " + jobId);
		log.debug("since: " + since);
		Boolean jobCopiesBackIncrementally = Jobs.doesJobCopyBackIncrementally(jobId);
		final String baseName = "Job" + String.valueOf(jobId) + "_output";

		//if we only want the new job pairs
		if (since != null) {
			log.debug("Getting incremental job output results");
			int olderPairs = Jobs.countOlderPairs(jobId, since);
			List<JobPair> pairs = Jobs.getNewCompletedPairsShallow(jobId, since);

			log.debug("Found " + pairs.size() + " new pairs");
			int maxCompletion = since;
			// pairsFound is defined as the number of pairs that completed since "since"
			// it does NOT include running pairs
			int pairsFound = 0;
			int runningPairsFound = 0;

			final Iterator it = pairs.iterator();
			while (it.hasNext()) {
				final JobPair x = (JobPair) it.next();
				log.trace("found pair id = " + x.getId() + " with completion id = " + x.getCompletionId());

				if (x.getCompletionId() > maxCompletion) {
					maxCompletion = x.getCompletionId();
				}

				if (x.getStatus().getCode().finishedRunning()) {
					++pairsFound;
				} else if (jobCopiesBackIncrementally) {
					++runningPairsFound;
				} else {
					it.remove();
				}
			}

			log.debug("Older pairs: " + String.valueOf(olderPairs));
			log.debug("Pairs-Found: " + String.valueOf(pairsFound));
			log.debug("Total-Pairs : " + String.valueOf(Jobs.getPairCount(jobId)));
			log.debug("Max Completion: " + String.valueOf(maxCompletion));
			response.addCookie(new Cookie("Older-Pairs", String.valueOf(olderPairs)));
			response.addCookie(new Cookie("Pairs-Found", String.valueOf(pairsFound)));
			response.addCookie(new Cookie("Total-Pairs", String.valueOf(Jobs.getPairCount(jobId))));
			response.addCookie(new Cookie("Max-Completion", String.valueOf(maxCompletion)));
			if (jobCopiesBackIncrementally) {
				log.debug("Running Pairs : " + String.valueOf(runningPairsFound));
				response.addCookie(new Cookie("Running-Pairs", String.valueOf(runningPairsFound)));
			}
			log.debug("added the max-completion cookie, starting to write output for job id = " + jobId);

			// get all files in between
			Download.addJobPairsToZipOutput(pairs, response, baseName, true, lastModified);
		} else {
			log.debug("preparing to create archive for job = " + jobId);
			ArchiveUtil.createAndOutputZip(new File(Jobs.getDirectory(jobId)), response.getOutputStream(), baseName,
			                               false
			);
		}

		return true;
	}

	/**
	 * Using a list of Solvers map each solver name to whether or not that solver name is duplicated in the solver
	 * list.
	 *
	 * @author Albert Giegerich
	 */
	private static HashMap<String, Boolean> createNameDuplicateMap(List<String> names) {
		HashMap<String, Boolean> nameDuplicateMap = new HashMap<>();
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
		final String methodName = "validateRequest";
		log.entry(methodName);
		log.debug(methodName, "Validating download request.");
		try {
			if (!Util.paramExists(PARAM_TYPE, request)) {
				final String message = "A download type was not specified";
				log.debug(methodName, "Download request was invalid: " + message);
				return new ValidatorStatusCode(false, message);
			}
			String type = request.getParameter(PARAM_TYPE);
			log.debug(methodName, "Download request is of type: " + type);

			if (!(type.equals(R.SOLVER) || type.equals(R.BENCHMARK) || type.equals(R.SPACE_XML) ||
					type.equals(R.JOB_XML) || type.equals(R.PAIR_OUTPUT) || type.equals(R.JOB) ||
					type.equals(R.JOB_OUTPUT) || type.equals(R.SPACE) || type.equals(R.PROCESSOR) ||
					type.equals(R.JOB_OUTPUTS) || type.equals(R.SOLVER_SOURCE) ||
					type.equals(R.JOB_PAGE_DOWNLOAD_TYPE))) {
				final String message = "The supplied download type was not valid";
				log.debug(methodName, "Download request was invalid: " + message);
				return new ValidatorStatusCode(false, message);
			}


			if (type.equals(R.JOB_OUTPUTS)) {
				//expecting a comma-separated list
				final String idArrayParam = "id[]";
				String ids = request.getParameter(idArrayParam);
				log.debug(methodName, idArrayParam + " = " + ids);
				if (!Validator.isValidIntegerList(ids)) {
					log.debug(methodName, idArrayParam + " was not a valid integer list.");
					return new ValidatorStatusCode(
							false, "The given list of ids contained one or more invalid integers");
				}
			} else {
				String universallyUniqueId = request.getParameter(PARAM_ANON_ID);
				if (universallyUniqueId == null) {
					int userId = SessionUtil.getUserId(request);
					log.debug(methodName, "Validating download request for user: " + userId);
					return validateForUser(userId, type, request);
				} else {
					log.debug(methodName, "Validating download request for anonymous link: " + universallyUniqueId);
					return validateForAnonymousLink(universallyUniqueId, type, request);
				}
			}
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "Internal error processing download request");
	}

	private static ValidatorStatusCode validateForAnonymousLink(
			String universallyUniqueId, String type, HttpServletRequest request
	) {
		return new ValidatorStatusCode(true);
	}

	private static ValidatorStatusCode validateForUser(int userId, String type, HttpServletRequest request) {
		final String methodName = "validateForUser";
		log.entry(methodName);
		if (!Validator.isValidPosInteger(request.getParameter(PARAM_ID))) {
			final String message = "The given id was not a valid integer";
			log.debug(methodName, "Download request validation failed: " + message);
			return new ValidatorStatusCode(false, message);
		}

		int id = Integer.parseInt(request.getParameter(PARAM_ID));
		log.debug(methodName, PARAM_ID + " = " + id);

		ValidatorStatusCode status = null;
		switch (type) {
		case R.SOLVER:
		case R.SOLVER_SOURCE:
			status = SolverSecurity.canUserDownloadSolver(id, userId);
			if (!status.isSuccess()) {
				return status;
			}
			break;
		case R.SPACE_XML:
		case R.SPACE:
		case R.PROCESSOR:
			if (!Permissions.canUserSeeSpace(id, userId)) {
				return new ValidatorStatusCode(false, "You do not have permission to see this space");
			}
			break;
		case R.JOB:
		case R.JOB_XML:
		case R.JOB_OUTPUT: {
			ValidatorStatusCode canSeeJobStatus = Permissions.canUserSeeJob(id, userId);
			if (!canSeeJobStatus.isSuccess()) {
				log.debug(methodName, "User could not see job, returning failure status.");
				return canSeeJobStatus;
			}
			break;
		}
		case R.PAIR_OUTPUT: {
			int jobId = JobPairs.getPair(id).getJobId();
			ValidatorStatusCode canSeeJobStatus = Permissions.canUserSeeJob(jobId, userId);
			if (!canSeeJobStatus.isSuccess()) {
				return canSeeJobStatus;
			}
			break;
		}
		case R.BENCHMARK:
			status = BenchmarkSecurity.canUserDownloadBenchmark(id, userId);
			if (!status.isSuccess()) {
				return status;
			}
			break;
		case R.JOB_PAGE_DOWNLOAD_TYPE:
			status = JobSecurity.canUserSeeJob(id, userId);
			if (!status.isSuccess()) {
				return status;
			}
			break;
		}
		return new ValidatorStatusCode(true);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String methodName = "doGet";
		log.entry(methodName);

		User u = SessionUtil.getUser(request);
		log.debug(methodName, "Got download request from user with id: " + u.getId());
		boolean success;
		String shortName = null;
		try {
			ValidatorStatusCode status = validateRequest(request);
			if (!status.isSuccess()) {
				log.debug("Bad download Request--" + status.getMessage());
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
				return;
			}

			Object check = request.getParameter("token");
			//token is used to tell the client when the file has arrived
			if (check != null) {
				String token = check.toString();
				log.debug(methodName, "Adding fileDownloadToken cookie: " + token);
				Cookie newCookie = new Cookie("fileDownloadToken", token);
				newCookie.setMaxAge(60);
				response.addCookie(newCookie);
			}


			String paramType = request.getParameter(PARAM_TYPE);
			log.debug(methodName, "Param type was: " + paramType);
			if (paramType.equals(R.SOLVER)) {
				log.debug(methodName, "Handling " + R.SOLVER);
				Optional<Solver> os = handleSolverAndSolverSrc(request, response);
				if (os.isPresent()) {
					Solver s = os.get();

					boolean reupload = false;
					if (Util.paramExists(PARAM_REUPLOAD, request)) {
						reupload = Boolean.parseBoolean(request.getParameter(PARAM_REUPLOAD));
						log.debug("Reupload parameter was: " + reupload);
					}
					success = handleSolver(s, response, reupload);
				} else {
					// handleSolverAndSolverSrc already sent the response.
					return;
				}
			} else if (request.getParameter(PARAM_TYPE).equals(R.SOLVER_SOURCE)) {
				log.debug(methodName, "Handling solverSrc");
				Optional<Solver> os = handleSolverAndSolverSrc(request, response);
				if (os.isPresent()) {
					Solver s = os.get();
					success = handleSolverSource(s, response);
				} else {
					// handleSolverAndSolverSrc already sent the response.
					return;
				}
			} else if (request.getParameter(PARAM_TYPE).equals(R.BENCHMARK)) {
				log.debug(methodName, "Handling " + R.BENCHMARK);
				Benchmark b = null;
				String universallyUniqueId = request.getParameter(PARAM_ANON_ID);
				if (universallyUniqueId == null) {
					log.debug(methodName, "Was not anonymous download.");
					int benchId = Integer.parseInt(request.getParameter(PARAM_ID));
					log.debug(methodName, "Getting benchmark with id: " + benchId);
					b = Benchmarks.get(benchId);
				} else {
					log.debug(methodName, "Getting benchmark from anonymous link UUID: " + universallyUniqueId);
					Optional<Integer> benchId = AnonymousLinks.getIdOfBenchmarkAssociatedWithLink(universallyUniqueId);
					if (benchId.isPresent()) {
						b = Benchmarks.get(benchId.get());
					} else {
						log.debug(methodName, "Could not find benchmark with UUID.");
						response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark not found.");
						return;
					}
				}

				shortName = b.getName();
				shortName = shortName.replaceAll("\\s+", "");
				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				success = handleBenchmark(b, response);
			} else if (request.getParameter(PARAM_TYPE).equals(R.PAIR_OUTPUT)) {
				log.debug(methodName, "Handling " + R.PAIR_OUTPUT);
				Boolean longPath = Boolean.parseBoolean(request.getParameter("longpath"));
				log.debug("Long path value = " + longPath);
				int id = Integer.parseInt(request.getParameter(PARAM_ID));
				shortName = "Pair_" + id;
				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				success = handlePairOutput(id, u.getId(), response, longPath);
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_OUTPUTS)) {
				log.debug(methodName, "Handling " + R.JOB_OUTPUTS);

				List<Integer> ids = Validator.convertToIntList(request.getParameter("id[]"));
				shortName = "Pair_Output";
				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				success = handlePairOutputs(ids, u.getId(), response, true);
			} else if (request.getParameter(PARAM_TYPE).equals(R.SPACE_XML)) {
				log.debug(methodName, "Handling " + R.SPACE_XML);
				Space space = Spaces.get(Integer.parseInt(request.getParameter(PARAM_ID)));
				shortName = space.getName() + "_XML";
				shortName = shortName.replaceAll("\\s+", "");
				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				boolean includeAttributes = false;
				boolean updates = false;
				int upid = -1;
				final String includeattrsParam = "includeattrs";
				if (Util.paramExists(includeattrsParam, request)) {
					includeAttributes = Boolean.parseBoolean(request.getParameter(includeattrsParam));
					log.debug(methodName, includeattrsParam + " = " + includeAttributes);
				}
				final String updatesParam = "updates";
				final String upidParam = "upid";
				if (Util.paramExists(updatesParam, request)) {
					updates = Boolean.parseBoolean(request.getParameter(updatesParam));
					log.debug(methodName, updatesParam + " = " + updates);
					upid = Integer.parseInt(request.getParameter(upidParam));
					log.debug(methodName, upidParam + " = " + upid);
				}

				success = handleSpaceXML(space, u.getId(), response, includeAttributes, updates, upid);
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_XML)) {
				log.debug(methodName, "Handling " + R.JOB_XML);
				Job job = Jobs.get(Integer.parseInt(request.getParameter(PARAM_ID)));

				shortName = "Job" + job.getId() + "_XML";
				shortName = shortName.replaceAll("\\s+", "");
				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				success = handleJobXML(job, u.getId(), response);

				// this next condition is for the CSV file
			} else if (request.getParameter(PARAM_TYPE).equals(R.JOB)) {
				log.debug(methodName, "Handling " + R.JOB);
				Integer jobId = Integer.parseInt(request.getParameter(PARAM_ID));

				final String getCompletedParam = "getcompleted";
				String getCompleted = request.getParameter(getCompletedParam);
				Boolean complete = false;
				if (getCompleted != null) {
					log.debug(methodName, getCompleted + " was present.");
					complete = Boolean.parseBoolean(getCompleted);
				}

				final String returnIdsParam = "returnids";
				String returnids = request.getParameter(returnIdsParam);
				Boolean ids = false;
				if (returnids != null) {
					log.debug(methodName, returnIdsParam + " was present.");
					ids = Boolean.parseBoolean(returnids);
				}

				final String sinceParam = "since";
				String lastSeen = request.getParameter(sinceParam);
				Integer since = null;
				if (lastSeen != null) {
					since = Integer.parseInt(lastSeen);
				}

				log.debug(methodName, "getCompleted = " + complete);
				log.debug(methodName, "returnids = " + ids);
				log.debug(methodName, sinceParam + " = " + since);

				shortName = "Job" + jobId + "_info";
				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				success = handleJob(jobId, u.getId(), response, since, ids, complete);
			} else if (request.getParameter(PARAM_TYPE).equals(R.SPACE)) {
				log.debug(methodName, "Handling " + R.SPACE);
				Space space = Spaces.getDetails(Integer.parseInt(request.getParameter(PARAM_ID)), u.getId());

				// we will  look for these attributes, but if they aren't there then the default should be
				//to get both solvers and benchmarks
				final String includeSolversParam = "includesolvers";
				boolean includeSolvers = true;
				if (Util.paramExists(includeSolversParam, request)) {
					log.debug(includeSolversParam + " was present");
					includeSolvers = Boolean.parseBoolean(request.getParameter(includeSolversParam));
				}
				log.debug(methodName, includeSolversParam + " = " + includeSolvers);

				final String includeBenchmarksParam = "includebenchmarks";
				boolean includeBenchmarks = true;
				if (Util.paramExists(includeBenchmarksParam, request)) {
					log.debug(methodName, includeBenchmarksParam + " was present");
					includeBenchmarks = Boolean.parseBoolean(request.getParameter(includeBenchmarksParam));
				}
				log.debug(methodName, includeBenchmarksParam + " = " + includeBenchmarks);

				final String useIdDirectoriesParam = "useIdDirectories";
				boolean useIdDirectories = Boolean.parseBoolean(request.getParameter(useIdDirectoriesParam));
				log.debug(methodName, useIdDirectoriesParam + " = " + useIdDirectoriesParam);

				shortName = space.getName();
				shortName = shortName.replaceAll("\\s+", "");

				final String hierarchyParam = "hierarchy";
				boolean hierarchy = request.getParameter(hierarchyParam).equals("true");
				if (hierarchy) {
					shortName = shortName + "_Hierarchy";
				}
				log.debug(hierarchyParam + " = " + hierarchy);

				response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
				success = handleSpace(space, u.getId(), response, hierarchy, includeBenchmarks, includeSolvers,
				                      useIdDirectories
				);
			} else {
				if (request.getParameter(PARAM_TYPE).equals(R.PROCESSOR)) {
					log.debug(methodName, "Handling " + R.PROCESSOR);
					List<Processor> proc = null;
					shortName = "Processor";
					response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");

					final Integer id = Integer.parseInt(request.getParameter(PARAM_ID));
					final ProcessorType type = ProcessorType.valueOf(request.getParameter("procClass").toUpperCase());
					log.debug(methodName, "download request is for " + type.toString());
					proc = Processors.getByCommunity(id, type);

					if (!proc.isEmpty()) {
						success = handleProc(proc, response);
					} else {
						log.debug(methodName, "Could not find any processors to download.");
						response.sendError(HttpServletResponse.SC_NO_CONTENT, "There are no processors to download");
						return;
					}
				} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_OUTPUT)) {
					log.debug(methodName, "Handling " + R.JOB_OUTPUT);
					int jobId = Integer.parseInt(request.getParameter(PARAM_ID));

					final String sinceParam = "since";
					String lastSeen = request.getParameter(sinceParam);
					Integer since = null;
					if (lastSeen != null) {
						log.debug(methodName, sinceParam + " was present.");
						since = Integer.parseInt(lastSeen);
					}
					log.debug(methodName, sinceParam + " = " + since);


					final String lastTimestampParam = "lastTimestamp";
					String lastMod = request.getParameter(lastTimestampParam);
					Long lastModified = null;
					if (lastMod != null) {
						log.debug(methodName + lastTimestampParam + " was present.");
						lastModified = Long.parseLong(lastMod);
					}
					log.debug(methodName, lastTimestampParam + " = " + lastModified);

					shortName = "Job" + jobId + "_output";
					response.addHeader("Content-Disposition", "attachment; filename=" + shortName + ".zip");
					success = handleJobOutputs(jobId, response, since, lastModified);
				} else if (request.getParameter(PARAM_TYPE).equals(R.JOB_PAGE_DOWNLOAD_TYPE)) {
					log.debug(methodName, "Handling " + R.JOB_PAGE_DOWNLOAD_TYPE);
					int jobId = Integer.parseInt(request.getParameter(PARAM_ID));
					DoJobPage.handleJobPage(jobId, request, response);
					// Just set success to true, handleJobPage will throw an exception if it is unsuccessful.
					success = true;
				} else {
					final String message = "invalid download type specified: " + request.getParameter(PARAM_TYPE);
					log.debug(methodName, message);
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
					return;
				}
			}

			if (success) {
				log.debug(methodName, "Successfully processed file for download.");
				response.getOutputStream().close();
			} else {
				log.debug(methodName, "Failed to process file for download.");
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to process file for download.");
			}
		} catch (ClientAbortException e) {
			/* The client has closed the connection, either intentionally or due
			 * to a connection timeout. This is not serious, and not worth a
			 * full stack trace. No need to send any message to client, because
			 * connection has been closed. */
			log.info("doGet",
					"Caught ClientAbortException\n\t" +
					"URL: " + request.getRequestURL() + "?" + request.getQueryString() + "\n\t" +
					"User Agent: " + request.getHeader("User-Agent")
			);
			response.getOutputStream().close();
		} catch (Exception e) {
			log.warn("Caught Exception in Download.doGet"+ e.getMessage());
			response.getOutputStream().close();
			//this won't work because we have already opened the response output stream
			//response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			log.exit(methodName);
		}
	}

	/**
	 * Handles download of a single space or a hierarchy, return the name of compressed file containing the space.
	 *
	 * @param space The space needed to be downloaded
	 * @param uid The id of the user making the request
	 * @param hierarchy True if downloading a hierarchy, false for a single space
	 * @param response The servlet response sent back
	 * @param includeBenchmarks Whether to include benchmarks in the directory
	 * @param includeSolvers Whether to include solvers in the directory
	 * @param useIdDirectories whether to put each primitive in a directory that has the name of it's id.
	 * @return a file representing the archive to send back to the client
	 * @throws ClientAbortException
	 * @author Ruoyu Zhang + Eric Burns + Albert Giegerich
	 */

	private boolean handleSpace(
			Space space, int uid, HttpServletResponse response, boolean hierarchy, boolean includeBenchmarks,
			boolean includeSolvers, boolean useIdDirectories
	) throws ClientAbortException {
		final String methodName = "handleSpace";
		// If we can see this space AND the space is downloadable...
		try {
			//String baseFileName=space.getName();
			ZipArchiveOutputStream stream = new ZipArchiveOutputStream(response.getOutputStream());

			storeSpaceHierarchy(space, uid, space.getName(), includeBenchmarks, includeSolvers, hierarchy, stream,
			                    useIdDirectories
			);
			stream.close();

			return true;
		} catch (ClientAbortException e) {
			throw e;
		} catch (Exception e) {
			log.error(methodName, e);
		}
		return false;
	}

	/**
	 * Store a space and possibly all its subspaces into the specified directory with their hierarchy
	 *
	 * @param space The space needed to be stored
	 * @param uid The user who make the request
	 * @param dest The destination directory
	 * @param includeBenchmarks -- Whether to include benchmarks in the directory
	 * @param includeSolvers Whether to include solvers in the directory
	 * @param recursive Whether to include subspaces or not
	 * @param useIdDirectories set to true if we want every primitive to be contained in a directory that is named
	 * after
	 * the primitives id.
	 * @throws IOException
	 * @author Ruoyu Zhang + Eric Burns + Albert Giegerich
	 */
	private void storeSpaceHierarchy(
			Space space, int uid, String dest, boolean includeBenchmarks, boolean includeSolvers, boolean recursive,
			ZipArchiveOutputStream stream, boolean useIdDirectories
	) throws Exception {
		final String method = "storeSpaceHierarchy";
		log.info("storing space " + space.getName() + "to" + dest);
		if (Permissions.canUserSeeSpace(space.getId(), uid)) {
			ArchiveUtil.addStringToArchive(stream, space.getDescription(), dest + File.separator + R.DESC_PATH);

			if (includeBenchmarks) {
				List<Benchmark> benchList = Benchmarks.getBySpace(space.getId());

				// Get a list of the names of the benchmarks in benchList
				List<String> benchNameList = new LinkedList<>();
				for (Benchmark bench : benchList) {
					benchNameList.add(bench.getName());
				}
				// Create a map that maps names of benchmarks to whether or not that name is a duplicate in the list.
				HashMap<String, Boolean> benchmarkNameDuplicateMap = createNameDuplicateMap(benchNameList);

				for (Benchmark b : benchList) {
					if (b.isDownloadable() || b.getUserId() == uid) {
						File benchmarkFile = new File(b.getPath());
						/*ArchiveUtil.addFileToArchive(stream, benchmarkFile, dest+File.separator+b.getId()+File
						.separator+b.getName());*/
						String zipFileName;
						if (useIdDirectories) {
							zipFileName = dest + File.separator + b.getId() + File.separator + b.getName();
						} else {
							boolean isDuplicate = benchmarkNameDuplicateMap.get(b.getName());
							zipFileName = dest + File.separator;
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
				List<Solver> solverList = null;
				//if we're getting a full hierarchy and the solver path is
				//not yet set, we want to store all solvers now
				if (recursive) {
					solverList = Solvers.getBySpaceHierarchy(space.getId(), uid);
				} else {
					solverList = Solvers.getBySpace(space.getId());
				}

				// Create a list of the names of the solvers in solverList
				List<String> solverNames = new LinkedList<>();
				for (Solver solver : solverList) {
					solverNames.add(solver.getName());
				}
				// Create a map that maps the names of solvers to whether or not they are duplicates in solverList.
				HashMap<String, Boolean> solverNameDuplicateMap = createNameDuplicateMap(solverNames);


				log.debug(method + ": Number of solvers in space=" + solverList.size());
				for (Solver s : solverList) {
					if (s.isDownloadable() || s.getUserId() == uid) {
						File solverFile = new File(s.getPath());
						String zipFileName;
						// Use a different file structure based on whether we're using id directories or not
						if (useIdDirectories) {
							zipFileName = dest + File.separator + "solvers" + File.separator + s.getId();
						} else {
							boolean isDuplicate = solverNameDuplicateMap.get(s.getName());
							zipFileName = dest + File.separator + "solvers" + File.separator;
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

			//if we aren't getting subspaces, we're done
			if (!recursive) {
				return;
			}

			List<Space> subspaceList = Spaces.getSubSpaces(space.getId(), uid);
			if (subspaceList != null) {
				for (Space s : subspaceList) {
					String subDir = dest + File.separator + s.getName();
					//include solvers is always false except at the top level
					storeSpaceHierarchy(s, uid, subDir, includeBenchmarks, false, recursive, stream, useIdDirectories);
				}
			}
		}
	}
}
