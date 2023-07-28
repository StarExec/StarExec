package org.starexec.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.starexec.app.RESTHelpers.JSTreeItem;
import org.starexec.command.C;
import org.starexec.command.Connection;
import org.starexec.command.JsonHandler;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.Queue;
import org.starexec.data.to.enums.Primitive;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.tuples.AttributesTableData;
import org.starexec.data.to.tuples.AttributesTableRow;
import org.starexec.data.to.tuples.Locatable;
import org.starexec.data.to.tuples.SolverConfig;
import org.starexec.exceptions.RESTException;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.logger.StarLogger;
import org.starexec.test.integration.TestResult;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.*;
//import org.starexec.data.database.Common;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;
//import java.sql.CallableStatement;
//import java.sql.ResultSet;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds all helper methods and classes for our restful web services
 */
public class RESTHelpers {
	private static final StarLogger log = StarLogger.getLogger(RESTHelpers.class);
	private static final String SEARCH_QUERY = "sSearch";
	private static final String SORT_DIRECTION = "sSortDir_0";
	private static final String SYNC_VALUE = "sEcho";
	private static final String SORT_COLUMN = "iSortCol_0";
	private static final String SORT_COLUMN_OVERRIDE = "sort_by";
	private static final String SORT_COLUMN_OVERRIDE_DIR = "sort_dir";
	private static final String STARTING_RECORD = "iDisplayStart";
	private static final String RECORDS_PER_PAGE = "iDisplayLength";
	/**
	 * Used to display the 'total entries' information at the bottom of
	 * the DataTable; also indirectly controls whether or not the
	 * pagination buttons are toggle-able
	 */
	private static final String TOTAL_RECORDS = "iTotalRecords";
	private static final String TOTAL_RECORDS_AFTER_QUERY = "iTotalDisplayRecords";
	private static final Gson gson = new Gson();

	/**
	 * Takes in a list of spaces and converts it into a list of JSTreeItems
	 * suitable for being displayed on the client side with the jsTree plugin.
	 *
	 * @param spaceList The list of spaces to convert
	 * @param userID The ID of the user making this request, which is used to tell whether nodes are leaves or not
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toSpaceTree(List<Space> spaceList, int userID) {
		List<JSTreeItem> list = new LinkedList<>();
		for (Space space : spaceList) {
			String isOpen = Spaces.getCountInSpace(space.getId(), userID, true) > 0 ? "closed" : "leaf";
			list.add(new JSTreeItem(space.getName(), space.getId(), isOpen, R.SPACE));
		}

		return list;
	}

	/*
	 * This just calls the Queue.getDescription. This choice was made so only this
	 * class needs to know about queues i.e RestServices dosen't need to know about 
	 * queues.
	 */
	public static String getQueueDescription(int qid) {
		return Queues.getDescForQueue(qid);
	}

	/**
	 * Takes in a list of spaces and converts it into a list of JSTreeItems
	 * suitable for being displayed on the client side with the jsTree plugin.
	 *
	 * @param jobSpaceList list of JobSpace to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toJobSpaceTree(List<JobSpace> jobSpaceList) {
		List<JSTreeItem> list = new LinkedList<>();

		for (JobSpace space : jobSpaceList) {
			String isOpen = Spaces.getCountInJobSpace(space.getId()) > 0 ? "closed" : "leaf";
			list.add(new JSTreeItem(space.getName(), space.getId(), isOpen, R.SPACE, space.getMaxStages()));
		}

		return list;
	}

	/**
	 * Takes in a list of worker nodes and converts it into a list of
	 * JSTreeItems suitable for being displayed on the client side with the
	 * jsTree plugin.
	 *
	 * @param nodes The list of worker nodes to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toNodeList(List<WorkerNode> nodes) {
		List<JSTreeItem> list = new LinkedList<>();

		for (WorkerNode n : nodes) {
			// Only take the first part of the host name, the full one is too
			// int to display on the client
			JSTreeItem t = new JSTreeItem(n.getName().split("\\.")[0], n.getId(), "leaf", n.getStatus().equals("ACTIVE") ? "enabled_node" : "disabled_node");
			list.add(t);
		}

		return list;
	}

	/**
	 * Takes in a list of queues and converts it into a list of JSTreeItems
	 * suitable for being displayed on the client side with the jsTree plugin.
	 *
	 * @param queues The list of queues to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toQueueList(List<Queue> queues) {
		List<JSTreeItem> list = new LinkedList<>();
		for (Queue q : queues) {
			//status might be null, so we don't want a null pointer in that case
			String status = q.getStatus();
			if (status == null) {
				status = "";
			}
			String isOpen = !Queues.getNodes(q.getId()).isEmpty() ? "closed" : "leaf";
			list.add(new JSTreeItem(q.getName(), q.getId(), isOpen, status.equals("ACTIVE") ? "active_queue" : "inactive_queue"));
		}

		return list;
	}

	/**
	 * Takes in a list of spaces (communities) and converts it into a list of
	 * JSTreeItems suitable for being displayed on the client side with the
	 * jsTree plugin.
	 *
	 * @param communities The list of communities to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toCommunityList(List<Space> communities) {
		List<JSTreeItem> list = new LinkedList<>();

		for (Space space : communities) {
			JSTreeItem t = new JSTreeItem(space.getName(), space.getId(), "leaf", R.SPACE);
			list.add(t);
		}

		return list;
	}

	/**
	 * Validate the parameters of a request for a DataTable page
	 *
	 * @param type the primitive type being queried for
	 * @param request the object containing the parameters to validate
	 * @return an attribute map containing the valid parameters parsed from the request object,<br>
	 * or null if parameter validation fails
	 * @author Todd Elvers
	 */
	private static DataTablesQuery getAttrMap(Primitive type, HttpServletRequest request) {
		DataTablesQuery query = new DataTablesQuery();
		try {
			// Parameters from the DataTable object
			String iDisplayStart = (String) request.getParameter(STARTING_RECORD);
			String iDisplayLength = (String) request.getParameter(RECORDS_PER_PAGE);
			String sEcho = (String) request.getParameter(SYNC_VALUE);
			String iSortCol = (String) request.getParameter(SORT_COLUMN);
			String sDir = (String) request.getParameter(SORT_DIRECTION);
			String sSearch = (String) request.getParameter(SEARCH_QUERY);


			// Validates the starting record, the number of records per page,
			// and the sync value
			if (Util.isNullOrEmpty(iDisplayStart) || Util.isNullOrEmpty(iDisplayLength) || Util.isNullOrEmpty(sEcho) || Integer.parseInt(iDisplayStart) < 0 || Integer.parseInt(sEcho) < 0) {
				return null;
			}

			if (Util.isNullOrEmpty(iSortCol)) {
				// Allow jobs datatable to have a sort column null, then set
				// the column to sort by column 5, which doesn't exist on the screen but represents the creation date
				if (type == Primitive.JOB) {
					query.setSortColumn(5);
				} else {
					return null;
				}
			} else {
				int sortColumnIndex = Integer.parseInt(iSortCol);
				query.setSortColumn(sortColumnIndex);
			}
			//set the sortASC flag
			if (Util.isNullOrEmpty(sDir)) {
				//WARNING: if you don't do this check, sometimes null gets passed, and this
				//causes null pointer exception. This is extremely hard to debug. DO NOT REMOVE!
				query.setSortASC(false);
			}
			else if (sDir.contains("asc")) {
				query.setSortASC(true);
			} else if (sDir.contains("desc")) {
				query.setSortASC(false);
			} else {
				log.warn("getAttrMap", "sDir is not 'asc' or 'desc': "+sDir);
				return null;
			}

			// Depending on if the search/filter is empty or not, this will be 0 or 1
			if (Util.isNullOrEmpty(sSearch)) {
				sSearch = null;
			}
			query.setSearchQuery(sSearch);

			// The request is valid if it makes it this far;
			// Finish the validation by adding the remaining attributes to the
			// map

			query.setNumRecords(Integer.parseInt(iDisplayLength));
			query.setStartingRecord(Integer.parseInt(iDisplayStart));
			query.setSyncValue(Integer.parseInt(sEcho));

			return query;
		} catch (Exception e) {
			log.error("There was a problem getting the paramaters for the datatables query:" + e.getCause());
		}

		return null;
	}

	/**
	 * Add tag for the image representing a link that will popout.
	 *
	 * @param sb the StringBuilder to add the tag with.
	 * @author Aaron Stump
	 */
	public static void addImg(StringBuilder sb) {
		sb.append("<img class=\"extLink\" src=\"");
		sb.append(Util.docRoot("images/external.png"));
		sb.append("\"/></a>");
	}

	/**
	 * Returns the HTML representing a job pair's status
	 *
	 * @param statType 'asc' or 'desc'
	 * @param value a job pair's completePairs, pendingPairs, or errorPairs  variable
	 * @param percentage a job pair's totalPairs variable
	 * @return HTML representing a job pair's status
	 * @author Todd Elvers
	 */
	public static String getPercentStatHtml(String statType, int value, Boolean percentage) {
		StringBuilder sb = new StringBuilder();
		sb.append("<p class=\"stat ");
		sb.append(statType);
		sb.append("\">");
		sb.append(value);
		if (percentage) {
			sb.append(" %");
		}
		sb.append("</p>");
		return sb.toString();
	}

	/**
	 * Gets a datatables JSON object for job pairs in a jobspace.
	 *
	 * @param jobSpaceId The jobspace to get the pairs from
	 * @param wallclock Whether to use wallclock (true) or CPU time (false)
	 * @param syncResults
	 * @param stageNumber The pipeline stage number to filter jobs by.
	 * @param primitivesToAnonymize A PrimitivesToAnonymize enum describing which primitives to anonymize
	 * @param request The HttpRequest asking to get the JSON object
	 * @return a JSON object for the job pairs in a job space.
	 * @author Albert Giegerich and Todd Elvers
	 */
	protected static String getJobPairsPaginatedJson(int jobSpaceId, boolean wallclock, boolean syncResults, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize, HttpServletRequest request) {

		final String methodName = "getJobPairsPaginatedJson";

		log.entry(methodName);
		// Query for the next page of job pairs and return them to the user
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfPairsInJobSpace(jobSpaceId, request, wallclock, syncResults, stageNumber, primitivesToAnonymize);

		if (nextDataTablesPage == null) {
			log.debug(methodName, "There was a database error while trying to get paginated job pairs for table.");
			return gson.toJson(RESTServices.ERROR_DATABASE);
		} else if (nextDataTablesPage.has("maxpairs")) {
			log.debug(methodName, "User had too many job pairs for data table to be populated.");
			return gson.toJson(RESTServices.ERROR_TOO_MANY_JOB_PAIRS);
		}

		return gson.toJson(nextDataTablesPage);
	}

	/**
 	 * Gets the space overview graph for a given jobspace.
 	 *
   	 * @param jobId the job to get the graph for.
 	 */
	protected static String getPairTimeGraphJson(int jobId) {
		String chartPath = Statistics.makeJobTimeGraph(jobId);
		if (chartPath.equals(Statistics.OVERSIZED_GRAPH_ERROR)) {
			return gson.toJson(RESTServices.ERROR_TOO_MANY_JOB_PAIRS);
		}

		log.debug("chartPath = " + chartPath);
		return chartPath == null ? gson.toJson(RESTServices.ERROR_DATABASE) : chartPath;
	}

	/**
	 * Gets the space overview graph for a given jobspace.
	 *
	 * @param stageNumber which stage to filter solvers by.
	 * @param jobSpaceId the jobSpace to get the graph for.
	 * @param request the HTTP request that is requesting the graph.
	 * @param primitivesToAnonymize a PrimitivesToAnonymize enum describing which primitives to anonymize for the graph.
	 */
	protected static String getSpaceOverviewGraphJson(int stageNumber, int jobSpaceId, HttpServletRequest request, PrimitivesToAnonymize primitivesToAnonymize) {
		List<Integer> configIds = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		boolean logX = false;
		boolean logY = false;
		if (Util.paramExists("logX", request)) {
			if (Boolean.parseBoolean((String) request.getParameter("logX"))) {
				logX = true;
			}
		}
		if (Util.paramExists("logY", request)) {
			if (Boolean.parseBoolean((String) request.getParameter("logY"))) {
				logY = true;
			}
		}
		String chartPath = null;
		if (configIds.size() <= R.MAXIMUM_SOLVER_CONFIG_PAIRS) {
			chartPath = Statistics.makeSpaceOverviewChart(jobSpaceId, logX, logY, configIds, stageNumber, primitivesToAnonymize);
			if (chartPath.equals(Statistics.OVERSIZED_GRAPH_ERROR)) {
				return gson.toJson(RESTServices.ERROR_TOO_MANY_JOB_PAIRS);
			}
		} else {
			return gson.toJson(RESTServices.ERROR_TOO_MANY_SOLVER_CONFIG_PAIRS);
		}

		log.debug("chartPath = " + chartPath);
		return chartPath == null ? gson.toJson(RESTServices.ERROR_DATABASE) : chartPath;
	}

	/**
	 * Gets the next data table page of job solver stats.
	 *
	 * @param stageNumber The stagenumber associated with the solver stats we want.
	 * @param jobSpace The jobspace associated with the solver stats we want.
	 * @param primitivesToAnonymize a PrimitivesToAnonymize enum describing which primitives should be given anonymous names.
	 * @param shortFormat Whether to use the abbreviated short format.
	 * @param wallclock Whether times should be in wallclock time or cpu time.
	 * @param includeUnknown True to include pairs with unknown status in time calculation
	 * @author Albert Giegerich
	 */
	protected static String getNextDataTablePageForJobStats(int stageNumber, JobSpace jobSpace, PrimitivesToAnonymize primitivesToAnonymize, boolean shortFormat, boolean wallclock, boolean includeUnknown) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// changed this from Jobs.getAllJobStatsInJobSpaceHierarchy() to Jobs.getAllJobStatsInJobSpaceHierarchyIncludeDeletedConfigs()
		// had to split up a function call chain to one version that includes configs marked as deleted and another that does not
		// this includes them; used to construct the solver summary table in the job space view
		// Alexander Brown, 9/20
		Collection<SolverStats> solverStats = Jobs.getAllJobStatsInJobSpaceHierarchyIncludeDeletedConfigs(jobSpace, stageNumber, primitivesToAnonymize, includeUnknown);
		stopWatch.stop();
		log.debug("getNextDataTablePageForJobStats", "Time taken to get all jobs: " + stopWatch.toString());

		if (solverStats == null) {
			return gson.toJson(RESTServices.ERROR_DATABASE);
		}


		JsonObject nextDataTablesPage = RESTHelpers.convertSolverStatsToJsonObject(solverStats, new DataTablesQuery(solverStats.size(), solverStats.size(), 1), jobSpace.getId(), jobSpace.getJobId(), shortFormat, wallclock, primitivesToAnonymize);
		return gson.toJson(nextDataTablesPage);
	}

	/**
	 * Gets the next page of job pairs as a JsonObject in the given jobSpaceId, with info populated from the given stage.
	 *
	 * @param jobSpaceId The ID of the job space
	 * @param request
	 * @param wallclock True to use wallclock time, false to use CPU time
	 * @param syncResults If true, excludes job pairs for which the benchmark has not been worked on by every solver in the space
	 * @param stageNumber If greater than or equal to 0, gets the primary stage
	 * @param primitivesToAnonymize a PrimitivesToAnonymize enum describing how the job paris should be anonymized.
	 * @return JsonObject encapsulating pairs to display in the next table page
	 */
	public static JsonObject getNextDataTablesPageOfPairsInJobSpace(int jobSpaceId, HttpServletRequest request, boolean wallclock, boolean syncResults, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize) {

		final String methodName = "getNextDataTablesPageOfPairsInJobSpace";
		log.entry(methodName);


		log.debug("beginningGetNextDataTablesPageOfPairsInJobSpace with stage = " + stageNumber);
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.JOB_PAIR, request);

		if (query == null) {
			return null;
		}
		query.setTotalRecords(Jobs.getJobPairCountInJobSpaceByStage(jobSpaceId, stageNumber));

		if (query.getTotalRecords() > R.MAXIMUM_JOB_PAIRS) {
			//there are too many job pairs to display quickly, so just don't query for them
			JsonObject ob = new JsonObject();
			ob.addProperty("maxpairs", true);
			return ob;
		}

		String sortOverride = request.getParameter(SORT_COLUMN_OVERRIDE);
		if (sortOverride != null) {
			query.setSortColumn(Integer.parseInt(sortOverride));
			query.setSortASC(Boolean.parseBoolean(request.getParameter(SORT_COLUMN_OVERRIDE_DIR)));
		}

		List<JobPair> jobPairsToDisplay;
		// Retrieves the relevant Job objects to use in constructing the JSON to
		// send to the client
		int[] totals = new int[2];

		if (!syncResults) {
			jobPairsToDisplay = Jobs.getJobPairsForNextPageInJobSpace(query, jobSpaceId, stageNumber, wallclock, primitivesToAnonymize);
			if (!query.hasSearchQuery()) {
				query.setTotalRecordsAfterQuery(query.getTotalRecords());
			} else {
				query.setTotalRecordsAfterQuery(Jobs.getJobPairCountInJobSpaceByStage(jobSpaceId, query.getSearchQuery(), stageNumber));
			}
		} else {
			log.debug("returning synchronized results");
			jobPairsToDisplay = Jobs.getSynchronizedJobPairsForNextPageInJobSpace(query, jobSpaceId, wallclock, stageNumber, totals, primitivesToAnonymize);
			query.setTotalRecords(totals[0]);
			query.setTotalRecordsAfterQuery(totals[1]);
		}

		return convertJobPairsToJsonObject(jobPairsToDisplay, query, wallclock, 0, primitivesToAnonymize);
	}

	/**
	 * Gets the next page of Benchmarks that the given use can see. This includes Benchmarks the user owns,
	 * Benchmarks in public spaces, and Benchmarks in spaces the user is also in.
	 *
	 * @param userId
	 * @param request
	 * @return JsonObject encapsulating next page of benchmarks to display
	 */
	public static JsonObject getNextDataTablesPageOfBenchmarksByUser(int userId, HttpServletRequest request) {
		log.debug("called getNextDataTablesPageOfBenchmarksByUser");
		try {
			DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.BENCHMARK, request);
			if (query == null) {
				return null;
			}

			// Retrieves the relevant Job objects to use in constructing the JSON to
			// send to the client
			int[] totals = new int[2];
			List<Benchmark> BenchmarksToDisplay = Benchmarks.getBenchmarksForNextPageByUser(query, userId, totals);

			query.setTotalRecords(totals[0]);

			query.setTotalRecordsAfterQuery(totals[1]);
			return convertBenchmarksToJsonObject(BenchmarksToDisplay, query);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Copies a benchmark from StarExec to StarDev
	 *
	 * @param commandConnection a logged in StarExecCommand connection.
	 * @param benchmarkId the benchmark to be copied from StarExec.
	 * @param spaceId the space to copy the benchmark to on StarDev.
	 * @return a status code indicating success or failure.
	 */
	protected static ValidatorStatusCode copyBenchmarkToStarDev(Connection commandConnection, int benchmarkId, int spaceId, int benchProcessorId) {
		Benchmark benchmarkToCopy = Benchmarks.get(benchmarkId);
		File sandbox = Util.getRandomSandboxDirectory();
		try {
			File tempFile = copyPrimitiveToSandbox(sandbox, benchmarkToCopy);
			int uploadStatus = commandConnection.uploadBenchmarksToSingleSpace(tempFile.getAbsolutePath(), benchProcessorId, spaceId, true);
			return outputStatus(commandConnection.getLastError(), uploadStatus, "Successfully copied benchmark to StarDev");
		} catch (IOException e) {
			log.warn("Could not copy benchmark to sandbox for copying to StarDev.", e);
			return new ValidatorStatusCode(false, "Could not copy benchmark.");
		} finally {
			deleteSandbox(sandbox);
		}

	}

	/**
	 * Copies a solver from StarExec to StarDev
	 *
	 * @param commandConnection a logged-in connection to StarDev
	 * @param solverId the ID of the solver to copy on StarExec.
	 * @param spaceId the ID of the space to copy to on StarDev.
	 * @return a ValidatorStatusCode indicating success or failure.
	 */
	protected static ValidatorStatusCode copySolverToStarDev(Connection commandConnection, int solverId, int spaceId) {
		Solver solverToCopy = Solvers.get(solverId);
		File sandbox = Util.getRandomSandboxDirectory();
		try {
			File tempFile = copyPrimitiveToSandbox(sandbox, solverToCopy);

			int uploadStatus = commandConnection.uploadSolver(solverToCopy.getName(), solverToCopy.getDescription(), spaceId, tempFile.getAbsolutePath(), true, // downloadable
					false, // run test job
					null, // default settings ID for test job
					C.DEFAULT_SOLVER_TYPE);
			return outputStatus(commandConnection.getLastError(), uploadStatus, "Successfully copied solver to StarDev");
		} catch (IOException e) {
			log.warn("Could not copy solver to sandbox for copying to StarDev.", e);
			return new ValidatorStatusCode(false, "Could not copy solver.");
		} finally {
			deleteSandbox(sandbox);
		}
	}

	/**
	 * Copies a processor to a StarDev instance.
	 *
	 * @param commandConnection an open StarExecCommand connection.
	 * @param processorId the processor to copy.
	 * @param communityId the community to copy the processor to (on stardev).
	 * @return ValidatorStatusCode representing success or failure
	 */
	protected static ValidatorStatusCode copyProcessorToStarDev(Connection commandConnection, int processorId, int communityId) {
		Processor processorToCopy = Processors.get(processorId);
		ProcessorType procType = processorToCopy.getType();
		File sandbox = Util.getRandomSandboxDirectory();
		try {
			// Copy and zip the processor to the sandbox.
			File tempFile = copyPrimitiveToSandbox(sandbox, processorToCopy);

			// Upload the processor using the connection.
			// The upload status will be a status code on failure or the id of the new processor on success.
			int uploadStatus;
			switch (procType) {
				case POST:
					uploadStatus = commandConnection.uploadPostProc(processorToCopy.getName(), processorToCopy.getDescription(), tempFile.getAbsolutePath(), communityId);
					break;
				case PRE:
					uploadStatus = commandConnection.uploadPreProc(processorToCopy.getName(), processorToCopy.getDescription(), tempFile.getAbsolutePath(), communityId);
					break;
				case BENCH:
					uploadStatus = commandConnection.uploadBenchProc(processorToCopy.getName(), processorToCopy.getDescription(), tempFile.getAbsolutePath(), communityId);
					break;
				default:
					return new ValidatorStatusCode(false, "This processor type is not yet supported.");
			}
			return outputStatus(commandConnection.getLastError(), uploadStatus, "Successfully copied processor to StarDev");
		} catch (IOException e) {
			log.warn("Could not copy solver to sandbox for copying to StarDev.", e);
			return new ValidatorStatusCode(false, "Could not copy processor.", Util.getStackTrace(e));
		} finally {
			deleteSandbox(sandbox);
		}
	}

	/**
	 * Validates a copy to stardev request when copying a benchmark with it's processor.
	 *
	 * @param request the http reuquest.
	 * @return a ValidatorStatusCode indicating success/failure.
	 */
	protected static ValidatorStatusCode validateCopyBenchWithProcessorToStardev(HttpServletRequest request) {
		ValidatorStatusCode isValid = validateAllCopyToStardev(request);
		if (!isValid.isSuccess()) {
			return isValid;
		}
		return new ValidatorStatusCode(true);
	}

	public static Connection instantiateConnectionForCopyToStardev(String instance, HttpServletRequest request) {
		final String username = request.getParameter(R.COPY_TO_STARDEV_USERNAME_PARAM);
		final String password = request.getParameter(R.COPY_TO_STARDEV_PASSWORD_PARAM);
		// Login to StarDev
		String url = "https://stardev.cs.uiowa.edu/" + instance + "/";
		return new Connection(username, password, url);
	}

	/**
	 * Validates a copy to stardev request
	 *
	 * @param request the copy to stardev request.
	 * @param primType the primitive type
	 * @return ValidatorStatusCode representing success or failure
	 */
	public static ValidatorStatusCode validateCopyToStardev(HttpServletRequest request, final String primType) {

		ValidatorStatusCode isValid = validateAllCopyToStardev(request);
		if (!isValid.isSuccess()) {
			return isValid;
		}

		// The primitive type must correspond to one of the Primitive enums.
		boolean validPrimitive = Util.isLegalEnumValue(primType, Primitive.class);
		if (!validPrimitive) {
			return new ValidatorStatusCode(false, "The given primitive type is not valid.");
		}

		Primitive primitive = Primitive.valueOf(primType);
		if (primitive == Primitive.BENCHMARK) {
			// For benchmark copies a processor must be specified.
			if (!Util.paramExists(R.COPY_TO_STARDEV_PROC_ID_PARAM, request)) {
				return new ValidatorStatusCode(false, "The processor ID parameter was not present in the request.");
			}
			// The processor ID must be an integer
			if (!Validator.isValidInteger(request.getParameter(R.COPY_TO_STARDEV_PROC_ID_PARAM))) {
				return new ValidatorStatusCode(false, "The processor ID was not a valid integer: " + request.getParameter(R.COPY_TO_STARDEV_PROC_ID_PARAM));
			}
		}
		return new ValidatorStatusCode(true);
	}

	private static ValidatorStatusCode validateAllCopyToStardev(HttpServletRequest request) {
		// Only developers and admins can do a copy to stardev request.
		int userId = SessionUtil.getUserId(request);
		if (!Users.isAdmin(userId) && !Users.isDeveloper(userId)) {
			return new ValidatorStatusCode(false, "You must be an admin or developer to do this.");
		}

		// There must be a username and password parameter.
		if (!Util.paramExists(R.COPY_TO_STARDEV_USERNAME_PARAM, request) || !Util.paramExists(R.COPY_TO_STARDEV_PASSWORD_PARAM, request)) {
			return new ValidatorStatusCode(false, "The username or password parameter was not found.");
		}


		// Space/community ID must be present for non-benchmark copies.
		boolean isSpaceIdParamPresent = Util.paramExists(R.COPY_TO_STARDEV_SPACE_ID_PARAM, request);
		if (!isSpaceIdParamPresent) {
			return new ValidatorStatusCode(false, "A space ID parameter was not present in request.");
		}

		// The space ID parameter must be an integer if it exists.
		if (!Validator.isValidInteger(request.getParameter(R.COPY_TO_STARDEV_SPACE_ID_PARAM))) {
			return new ValidatorStatusCode(false, "The space ID parameter was not in integer format.");
		}

		return new ValidatorStatusCode(true);
	}

	/**
	 * Outputs a ValidatorStatusCode based on a StarExecCOmmand status code.
	 *
	 * @param lastError the last error returned by a StarExecCommand connection.
	 * @param uploadStatus the Command status code to convert to a ValidatorStatusCode.
	 * @param successMessage message to use in ValidatorStatusCode on success.
	 * @return a ValidatorStatusCode based on the given Command status code.
	 */
	private static ValidatorStatusCode outputStatus(String lastError, int uploadStatus, String successMessage) {
		if (uploadStatus < 0) {
			log.warn("Command failed to upload primitive: " + org.starexec.command.Status.getStatusMessage(uploadStatus) + "\n" + lastError);
			return new ValidatorStatusCode(false, org.starexec.command.Status.getStatusMessage(uploadStatus), lastError);
		}
		// on success the upload status will be the id of the new processor
		return new ValidatorStatusCode(true, successMessage, uploadStatus);
	}

	// Helper method for copying primitive to StarDev.
	public static ValidatorStatusCode copyPrimitiveToStarDev(Connection commandConnection, Primitive primType, Integer primitiveId, HttpServletRequest request) {
		final int spaceId = Integer.parseInt(request.getParameter(R.COPY_TO_STARDEV_SPACE_ID_PARAM));
		switch (primType) {
			case BENCHMARK:
				final int benchProcessorId = Integer.parseInt(request.getParameter(R.COPY_TO_STARDEV_PROC_ID_PARAM));
				return RESTHelpers.copyBenchmarkToStarDev(commandConnection, primitiveId, spaceId, benchProcessorId);
			case SOLVER:
				return RESTHelpers.copySolverToStarDev(commandConnection, primitiveId, spaceId);
			case PROCESSOR:
				return RESTHelpers.copyProcessorToStarDev(commandConnection, primitiveId, spaceId);
			default:
				return new ValidatorStatusCode(false, "That type is not yet supported.");
		}
	}

	/**
	 * Zips a primitive into the given Sandbox.
	 *
	 * @param sandbox the sandbox to place the zip file in.
	 * @param primitive the primitive to zip into the sandbox.
	 * @return the zip file.
	 * @throws IOException if something goes wrong with copying or zipping.
	 */
	private static File copyPrimitiveToSandbox(final File sandbox, final Locatable primitive) throws IOException {
		// Use this sandbox as the directory that will be zipped and placed into the input sandbox directory.
		File tempSandbox = Util.getRandomSandboxDirectory();
		try {
			// place the file in the temp sandbox.
			File primitiveFile = new File(primitive.getPath());
			if (primitiveFile.isDirectory()) {
				FileUtils.copyDirectory(primitiveFile, tempSandbox);
			} else {
				FileUtils.copyFileToDirectory(primitiveFile, tempSandbox);
			}
			// This name doesn't really matter since it's only used internally.
			String archiveName = "temp.zip";
			File outputFile = new File(sandbox, archiveName);
			// Zip the temp sandbox into the input sandbox.
			ArchiveUtil.createAndOutputZip(tempSandbox, new FileOutputStream(outputFile), archiveName, true);
			return outputFile;
		} finally {
			deleteSandbox(tempSandbox);
		}
	}

	// Deletes a sandbox directory.
	private static void deleteSandbox(File sandbox) {
		try {
			FileUtils.deleteDirectory(sandbox);
		} catch (IOException e) {
			log.error("Caught IOException while deleting directory: " + sandbox.getAbsolutePath() + "\nDirectory may not have been deleted", e);
		}
	}

	/**
	 * Gets the next page of solvers that the given user can see. This includes solvers the user owns,
	 * solvers in public spaces, and solvers in spaces the user is also in.
	 *
	 * @param userId
	 * @param request
	 * @return JsonObject encapsulating next page of solvers to display
	 */
	public static JsonObject getNextDataTablesPageOfSolversByUser(int userId, HttpServletRequest request) {

		try {
			DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.SOLVER, request);
			if (query == null) {
				return null;
			}


			// Retrieves the relevant Job objects to use in constructing the JSON to
			// send to the client
			int[] totals = new int[2];
			List<Solver> solversToDisplay = Solvers.getSolversForNextPageByUser(query, userId, totals);

			query.setTotalRecords(totals[0]);

			query.setTotalRecordsAfterQuery(totals[1]);

			return convertSolversToJsonObject(solversToDisplay, query);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Returns the next page of SolverComparison objects needed for a DataTables page in a job space
	 *
	 * @param jobSpaceId
	 * @param configId1
	 * @param configId2
	 * @param request
	 * @param wallclock
	 * @param stageNumber
	 * @return JsonObject encapsulating next page of solver comparisons to display
	 */
	public static JsonObject getNextDataTablesPageOfSolverComparisonsInSpaceHierarchy(int jobSpaceId, int configId1, int configId2, HttpServletRequest request, boolean wallclock, int stageNumber) {

		try {
			DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.JOB_PAIR, request);
			if (query == null) {
				return null;
			}

			// Retrieves the relevant Job objects to use in constructing the JSON to
			// send to the client
			int[] totals = new int[2];
			List<SolverComparison> solverComparisonsToDisplay = Jobs.getSolverComparisonsForNextPageByConfigInJobSpaceHierarchy(query, jobSpaceId, configId1, configId2, totals, wallclock, stageNumber);

			query.setTotalRecords(totals[0]);

			query.setTotalRecordsAfterQuery(totals[1]);

			return convertSolverComparisonsToJsonObject(solverComparisonsToDisplay, query, wallclock, stageNumber, jobSpaceId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;

	}

	public static JsonObject getNextDataTablesPageOfPairsByConfigInSpaceHierarchy(int jobSpaceId, int configId, HttpServletRequest request, String type, boolean wallclock, int stageNumber) {
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.JOB_PAIR, request);
		if (query == null) {
			return null;
		}

		String sortOverride = request.getParameter(SORT_COLUMN_OVERRIDE);
		if (sortOverride != null) {
			query.setSortColumn(Integer.parseInt(sortOverride));
			query.setSortASC(Boolean.parseBoolean(request.getParameter(SORT_COLUMN_OVERRIDE_DIR)));
		}

		// Retrieves the relevant Job objects to use in constructing the JSON to
		// send to the client
		List<JobPair> jobPairsToDisplay = Jobs.getJobPairsForNextPageByConfigInJobSpaceHierarchy(query, jobSpaceId, configId, type, stageNumber);

		query.setTotalRecords(Jobs.getCountOfJobPairsByConfigInJobSpaceHierarchy(jobSpaceId, configId, type, stageNumber));

		query.setTotalRecordsAfterQuery(Jobs.getCountOfJobPairsByConfigInJobSpaceHierarchy(jobSpaceId, configId, type, query.getSearchQuery(), stageNumber));

		return convertJobPairsToJsonObject(jobPairsToDisplay, query, wallclock, stageNumber, PrimitivesToAnonymize.NONE);
	}

	/**
	 * Gets the next page of job_pair entries for a DataTable object on cluster
	 * Status page
	 *
	 * @param type either queue or node
	 * @param id the id of the queue/node to get job pairs for
	 * @param request the object containing all the DataTable parameters
	 * @return a JSON object representing the next page of primitives to return
	 * to the client,<br>
	 * or null if the parameters of the request fail validation
	 * @author Wyatt Kaiser
	 */
	public static JsonObject getNextDataTablesPageCluster(String type, int id, int userId, HttpServletRequest request) {
		try {
			// Parameter validation
			DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.NODE, request);
			if (query == null) {
				return null;
			}

			if (type.equals("queue")) {
				// Retrieves the relevant Job objects to use in constructing the
				// JSON to send to the client
				List<JobPair> jobPairsToDisplay = Queues.getJobPairsForNextClusterPage(query, id);
				query.setTotalRecords(Queues.getCountOfEnqueuedPairsByQueue(id));
				// there is no filter function on this table, so this is always equal to the above
				query.setTotalRecordsAfterQuery(query.getTotalRecords());
				return convertJobPairsToJsonObjectCluster(jobPairsToDisplay, query, userId);
			} else if (type.equals("node")) {
				// Retrieves the relevant Job objects to use in constructing the
				// JSON to send to the client
				List<JobPair> jobPairsToDisplay = Queues.getPairsRunningOnNode(id);
				query.setTotalRecords(jobPairsToDisplay.size());
				// there is no filter function on this table, so this is always equal to the above
				query.setTotalRecordsAfterQuery(query.getTotalRecords());
				return convertJobPairsToJsonObjectCluster(jobPairsToDisplay, query, userId);
			}
			return null;
		} catch (Exception e) {
			log.error("getNextDataTablesPageCluster", e);
		}
		return null;

	}

	/**
	 * Gets the next page of entries for a DataTable Object (ALL regardless of
	 * space)
	 *
	 * @param request the object containing all the DataTable parameters
	 * @return a JSON object representing the next page of primitives to return
	 * to the client, <br>
	 * or null if the parameters of the request fail validation
	 * @author Wyatt Kaiser
	 */

	protected static JsonObject getNextUsersPageAdmin(HttpServletRequest request) {
		// Parameter Validation
		int currentUserId = SessionUtil.getUserId(request);

		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.USER, request);
		if (query == null) {
			return null;
		}
		query.setTotalRecords(Users.getCount());
		// Retrieves the relevant User objects to use in constructing the
		// JSON to send to the client
		List<User> usersToDisplay = Users.getUsersForNextPageAdmin(query);

		// If no search is provided, TOTAL_RECORDS_AFTER_QUERY =
		// TOTAL_RECORDS
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		}
		// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS
		else {
			query.setTotalRecordsAfterQuery(usersToDisplay.size());
		}

		return convertUsersToJsonObject(usersToDisplay, query, currentUserId);
	}

	public static JsonObject getNextBenchmarkPageForSpaceExplorer(int id, HttpServletRequest request) {
		// Parameter validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.BENCHMARK, request);
		if (query == null) {
			return null;
		}

		String sortOverride = request.getParameter(SORT_COLUMN_OVERRIDE);
		if (sortOverride != null) {
			query.setSortColumn(Integer.parseInt(sortOverride));
			query.setSortASC(Boolean.parseBoolean(request.getParameter(SORT_COLUMN_OVERRIDE_DIR)));

		}
		// Retrieves the relevant Benchmark objects to use in constructing the JSON to send to the client
		List<Benchmark> benchmarksToDisplay = Benchmarks.getBenchmarksForNextPage(query, id);

		query.setTotalRecords(Benchmarks.getCountInSpace(id));
		// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		} else {
			query.setTotalRecordsAfterQuery(Benchmarks.getCountInSpace(id, query.getSearchQuery()));

		}


		return convertBenchmarksToJsonObject(benchmarksToDisplay, query);
	}

	public static JsonObject getNextJobPageForSpaceExplorer(int id, HttpServletRequest request) {
		// Parameter validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.JOB, request);
		if (query == null) {
			return null;
		}
		// Retrieves the relevant Job objects to use in constructing the
		// JSON to send to the client
		List<Job> jobsToDisplay = Jobs.getJobsForNextPage(query, id);
		query.setTotalRecords(Jobs.getCountInSpace(id));
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		} else {
			query.setTotalRecordsAfterQuery(Jobs.getCountInSpace(id, query.getSearchQuery()));
		}


		// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS

		return convertJobsToJsonObject(jobsToDisplay, query, false);
	}

	public static JsonObject getNextUserPageForSpaceExplorer(int id, HttpServletRequest request) {
		// Parameter validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.USER, request);
		if (query == null) {
			return null;
		}
		query.setTotalRecords(Users.getCountInSpace(id));

		// Retrieves the relevant User objects to use in constructing the JSON to send to the client
		List<User> usersToDisplay = Users.getUsersForNextPage(query, id);

		// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		}
		// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS
		else {
			query.setTotalRecordsAfterQuery(Users.getCountInSpace(id, query.getSearchQuery()));
		}

		return convertUsersToJsonObject(usersToDisplay, query, SessionUtil.getUserId(request));

	}

	public static JsonObject getNextSolverPageForSpaceExplorer(int id, HttpServletRequest request) {
		// Parameter validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.SOLVER, request);
		if (query == null) {
			return null;
		}

		// Retrieves the relevant Solver objects to use in constructing the JSON to send to the client
		List<Solver> solversToDisplay = Solvers.getSolversForNextPage(query, id);
		query.setTotalRecords(Solvers.getCountInSpace(id));
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		}
		// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS
		else {
			query.setTotalRecordsAfterQuery(Solvers.getCountInSpace(id, query.getSearchQuery()));
		}

		return convertSolversToJsonObject(solversToDisplay, query);
	}

	public static JsonObject getNextSpacePageForSpaceExplorer(int id, HttpServletRequest request) {
		// Parameter validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.SPACE, request);
		if (query == null) {
			return null;
		}

		int userId = SessionUtil.getUserId(request);
		query.setTotalRecords(Spaces.getCountInSpace(id, userId, false));

		// Retrieves the relevant Benchmark objects to use in constructing the JSON to send to the client
		List<Space> spacesToDisplay = Spaces.getSpacesForNextPage(query, id, userId);

		// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		}
		// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS
		else {
			query.setTotalRecordsAfterQuery(Spaces.getCountInSpace(id, userId, query.getSearchQuery()));
		}
		return convertSpacesToJsonObject(spacesToDisplay, query);
	}

	/*
	 * Given data about a request, return a json object representing the next page
	 * Docs by @aguo2
	 * @author ArchieKipp
	 * 
	 */
	public static JsonObject getNextDataTablesPageForUserDetails(Primitive type, int id, HttpServletRequest request, boolean recycled, boolean dataAsObjects) {
		// Parameter validation
		DataTablesQuery query = RESTHelpers.getAttrMap(type, request);
		if (query == null) {
			return null;
		}
		switch (type) {
			case JOB:
				// Retrieves the relevant Job objects to use in constructing the
				// JSON to send to the client

				List<Job> jobsToDisplay = Jobs.getJobsByUserForNextPage(query, id);
				query.setTotalRecords(Jobs.getJobCountByUser(id));
				if (!query.hasSearchQuery()) {
					query.setTotalRecordsAfterQuery(query.getTotalRecords());
				} else {
					query.setTotalRecordsAfterQuery(Jobs.getJobCountByUser(id, query.getSearchQuery()));
				}


				// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS

				return convertJobsToJsonObject(jobsToDisplay, query, dataAsObjects);


			case SOLVER:
				// Retrieves the relevant Solver objects to use in constructing the JSON to send to the client
				List<Solver> solversToDisplay = Solvers.getSolversByUserForNextPage(query, id, recycled);
				if (!recycled) {
					query.setTotalRecords(Solvers.getSolverCountByUser(id));
				} else {
					query.setTotalRecords(Solvers.getRecycledSolverCountByUser(id));
				}

				// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
				if (!query.hasSearchQuery()) {
					query.setTotalRecordsAfterQuery(query.getTotalRecords());
				}
				// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS
				else {
					if (!recycled) {
						query.setTotalRecordsAfterQuery(Solvers.getSolverCountByUser(id, query.getSearchQuery()));
					} else {
						query.setTotalRecordsAfterQuery(Solvers.getRecycledSolverCountByUser(id, query.getSearchQuery()));
					}
				}
				return convertSolversToJsonObject(solversToDisplay, query);

			case UPLOAD:
				query.setTotalRecords(Uploads.getUploadCountByUser(id));
				if (!query.hasSearchQuery()) {
				    query.setTotalRecordsAfterQuery(query.getTotalRecords());
				} else {
				    query.setTotalRecordsAfterQuery(Uploads.getUploadCountByUser(id, query.getSearchQuery()));
				}
				query.setSortASC(!query.isSortASC());
		    	List<BenchmarkUploadStatus> uploadsToDisplay = Uploads.getUploadsByUserForNextPage(query, id);
				JsonObject obj =  convertUploadsToJsonObject(uploadsToDisplay, query);
				return obj;

			case BENCHMARK:
				String sortOverride = request.getParameter(SORT_COLUMN_OVERRIDE);
				if (sortOverride != null) {
					query.setSortColumn(Integer.parseInt(sortOverride));
					query.setSortASC(Boolean.parseBoolean(request.getParameter(SORT_COLUMN_OVERRIDE_DIR)));
				}
				List<Benchmark> benchmarksToDisplay = Benchmarks.getBenchmarksByUserForNextPage(query, id, recycled);
				if (!recycled) {
					query.setTotalRecords(Benchmarks.getBenchmarkCountByUser(id));
				} else {
					query.setTotalRecords(Benchmarks.getRecycledBenchmarkCountByUser(id));
				}
				// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
				if (!query.hasSearchQuery()) {
					query.setTotalRecordsAfterQuery(query.getTotalRecords());
				} else {
					if (!recycled) {
						query.setTotalRecordsAfterQuery(Benchmarks.getBenchmarkCountByUser(id, query.getSearchQuery()));
					} else {
						query.setTotalRecordsAfterQuery(Benchmarks.getRecycledBenchmarkCountByUser(id, query.getSearchQuery()));
					}
				}
				return convertBenchmarksToJsonObject(benchmarksToDisplay, query);
			default:
				log.error("invalid type given = " + type);
		}
		return null;
	}

	
	/**
	 * Generate the HTML for the next DataTable page of entries
	 *
	 * @param pairs The job pairs to convert
	 * @param query a DataTablesQuery object
	 * @param userId The ID of the user making this request
	 * @return JsonObject a JsonObject representing the pairs + other DataTables fields.
	 */
	public static JsonObject convertJobPairsToJsonObjectCluster(List<JobPair> pairs, DataTablesQuery query, int userId) {
		JsonArray dataTablePageEntries = new JsonArray();
		final String baseUrl = Util.docRoot("secure/details/job.jsp?id=");
		for (JobPair j : pairs) {

			final String pairLink = j.getQueueSubmitTimeSafe().toString();

			// Create the job link
			//Job job = Jobs.get(j.getJobId());
			StringBuilder sb = new StringBuilder();
			sb.append("<a href='");
			sb.append(baseUrl);
			sb.append(j.getJobId());
			sb.append("' target='_blank'>");
			sb.append(j.getOwningJob().getName());
			RESTHelpers.addImg(sb);
			sb.append(getHiddenJobPairLink(j.getId()));
			String jobLink = sb.toString();

			User user = j.getOwningUser();

			String userLink = getUserLink(user.getId(), user.getFullName(), userId);

			String benchLink = getBenchLinkWithHiddenPairId(j.getBench(), j.getId(), PrimitivesToAnonymize.NONE);

			// Create the solver link
			String solverLink = getSolverLink(j.getPrimarySolver().getId(), j.getPrimarySolver().getName(), PrimitivesToAnonymize.NONE);

			// Create the configuration link
			String configLink = getConfigLink(j.getPrimarySolver().getConfigurations().get(0).getId(), j.getPrimarySolver().getConfigurations().get(0).getName(), PrimitivesToAnonymize.NONE);

			// Create an object, and inject the above HTML, to represent an entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(pairLink));
			entry.add(new JsonPrimitive(jobLink));
			entry.add(new JsonPrimitive(userLink));
			entry.add(new JsonPrimitive(benchLink));
			entry.add(new JsonPrimitive(solverLink));
			entry.add(new JsonPrimitive(configLink));
			entry.add(new JsonPrimitive(j.getPath()));
			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Generate the HTML for the next DataTable page of entries
	 */
	public static JsonObject convertSolverComparisonsToJsonObject(List<SolverComparison> comparisons, DataTablesQuery query, boolean useWallclock, int stageNumber, int jobSpaceId) {
		JsonArray dataTablePageEntries = new JsonArray();
		for (SolverComparison c : comparisons) {


			// Create the benchmark link and append the hidden input element
			String benchLink = getBenchLink(c.getBenchmark());

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(benchLink));


			if (useWallclock) {
				double displayWC1 = Math.round(c.getFirstPair().getStageFromNumber(stageNumber).getWallclockTime() * 100) / 100.0;
				double displayWC2 = Math.round(c.getSecondPair().getStageFromNumber(stageNumber).getWallclockTime() * 100) / 100.0;
				double displayDiff = Math.round(c.getWallclockDifference(stageNumber) * 100) / 100.0;

				entry.add(new JsonPrimitive(displayWC1 + " s"));
				entry.add(new JsonPrimitive(displayWC2 + " s"));
				entry.add(new JsonPrimitive(displayDiff + " s"));

			} else {
				double display1 = Math.round(c.getFirstPair().getStageFromNumber(stageNumber).getCpuTime() * 100) / 100.0;
				double display2 = Math.round(c.getSecondPair().getStageFromNumber(stageNumber).getCpuTime() * 100) / 100.0;
				double displayDiff = Math.round(c.getCpuDifference(stageNumber) * 100) / 100.0;

				entry.add(new JsonPrimitive(display1 + " s"));
				entry.add(new JsonPrimitive(display2 + " s"));
				entry.add(new JsonPrimitive(displayDiff + " s"));
			}
			String link1 = getPairsInSpaceHtml(jobSpaceId, c.getFirstPair().getPrimaryConfiguration().getId(), c.getFirstPair().getStageFromNumber(stageNumber).getStarexecResult());

			String link2 = getPairsInSpaceHtml(jobSpaceId, c.getSecondPair().getPrimaryConfiguration().getId(), c.getSecondPair().getStageFromNumber(stageNumber).getStarexecResult());
			entry.add(new JsonPrimitive(link1));
			entry.add(new JsonPrimitive(link2));
			if (c.doResultsMatch(stageNumber)) {
				entry.add(new JsonPrimitive(1));
			} else {
				entry.add(new JsonPrimitive(0));

			}
			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);

	}

	private static String getConfigLink(int configId, String configName, PrimitivesToAnonymize primitivesToAnonymize) {
		StringBuilder sb = new StringBuilder();
		sb.append("<a class=\"configLink\" title=\"");
		sb.append(configName).append("\"");

		// Add the link to the solver if we don't need to be an anoymous config.
		if (!AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {

			// If the config has been marked as delted, use the link for the delted config page instead
			try {
				Configuration configuration = Solvers.getConfigurationIncludeDeleted( configId );
				if ( configuration.getDeleted() == 1 ) {
					sb.append( " href=\"" ).append( Util.docRoot( "secure/details/configDeleted.jsp?id=" ) );
					sb.append(configId).append("\" target=\"_blank\"");
				} else {
					sb.append(" href=\"").append(Util.docRoot("secure/details/configuration.jsp?id="));
					sb.append(configId).append("\" target=\"_blank\"");
				}
			} catch ( Exception e ) {
				sb.append(" href=\"").append(Util.docRoot("secure/details/configuration.jsp?id="));
				sb.append(configId).append("\" target=\"_blank\"");
			}

		}
		sb.append(" id=\"");
		sb.append(configId);
		sb.append("\">");
		sb.append(configName);

		// Add link image to the solver if we don't need to be an anoymous config.
		if (!AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
			RESTHelpers.addImg(sb);
		}
		return sb.toString();
	}

	private static String getHiddenJobPairLink(int pairId) {
		// Create the hidden input tag containing the jobpair id
		return "<input type=\"hidden\" value=\"" + pairId + "\" name=\"pid\"/>";
	}

	private static String getHiddenBenchLink(Benchmark bench) {
		// Create the hidden input tag containing the benchmark id
		return "<input name=\"bench\" type=\"hidden\" value=\"" + bench.getId() + "\" prim=\"benchmark\" userId=\"" +
		bench.getUserId() + "\"  deleted=\"" + bench.isDeleted() + "\" recycled=\"" + bench.isRecycled() +
		"\"/>";
	}

	private static StringBuilder getBenchLinkPrefix(Benchmark bench, PrimitivesToAnonymize primitivesToAnonymize) {
		StringBuilder sb = new StringBuilder();
		sb.append("<a");
		// Set the tooltip to be the benchmark's description
		if (!AnonymousLinks.areBenchmarksAnonymized(primitivesToAnonymize)) {
			sb.append(" title=\"");
			sb.append(bench.getDescription());
			sb.append("\" ");
			sb.append("href=\"").append(Util.docRoot("secure/details/benchmark.jsp?id="));
			sb.append(bench.getId());
			sb.append("\" target=\"_blank\"");
		}
		sb.append(">");

		sb.append(bench.getName());
		if (!AnonymousLinks.areBenchmarksAnonymized(primitivesToAnonymize)) {
			RESTHelpers.addImg(sb);
		}
		return sb;
	}

	private static String getBenchLinkWithHiddenPairId(Benchmark bench, int pairId, PrimitivesToAnonymize primitivesToAnonymize) {
		StringBuilder sb = getBenchLinkPrefix(bench, primitivesToAnonymize);
		sb.append(getHiddenJobPairLink(pairId));
		return sb.toString();
	}

	private static String getBenchLink(Benchmark bench) {
		StringBuilder sb = getBenchLinkPrefix(bench, PrimitivesToAnonymize.NONE);
		sb.append(getHiddenBenchLink(bench));
		return sb.toString();
	}

	private static String getSpaceLink(Space space) {
		StringBuilder sb = new StringBuilder();
		sb.append("<input type=\"hidden\" value=\"");
		sb.append(space.getId());
		sb.append("\" prim=\"space\" />");
		String hiddenSpaceId = sb.toString();
		// Create the space "details" link and append the hidden input
		// element
		sb = new StringBuilder();
		sb.append("<a class=\"spaceLink\" onclick=\"openSpace(");
		sb.append(space.getParentSpace());
		sb.append(",");
		sb.append(space.getId());
		sb.append(")\">");
		sb.append(space.getName());
		RESTHelpers.addImg(sb);
		sb.append(hiddenSpaceId);
		return sb.toString();
	}

	private static String getSolverLink(int solverId, String solverName, PrimitivesToAnonymize primitivesToAnonymize) {
		StringBuilder sb = new StringBuilder();
		sb.append("<a title=\"");
		sb.append(solverName);
		sb.append("\" ");

		if (!AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
			sb.append("href=\"").append(Util.docRoot("secure/details/solver.jsp?id="));
			sb.append(solverId);
			sb.append("\" target=\"_blank\"");
		}
		sb.append(">");

		sb.append(solverName);

		if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
			sb.append("</a>");
		} else {
			RESTHelpers.addImg(sb);
		}
		return sb.toString();
	}

	private static String getUserLink(int userId, String name, int callerId) {
		StringBuilder sb = new StringBuilder();
		String hiddenUserId;

		// Create the hidden input tag containing the user id
		sb.append("<input type=\"hidden\" value=\"");
		sb.append(userId);
		if (userId == callerId) {
			sb.append("\" name=\"currentUser\" id=\"uid").append(userId).append("\" prim=\"user\"/>");
		} else {
			sb.append("\" id=\"uid").append(userId).append("\" prim=\"user\"/>");
		}
		hiddenUserId = sb.toString();

		// Create the user "details" link and append the hidden input
		// element
		sb = new StringBuilder();
		sb.append("<a href=\"").append(Util.docRoot("secure/details/user.jsp?id="));
		sb.append(userId);
		sb.append("\" target=\"_blank\">");
		sb.append(name);
		RESTHelpers.addImg(sb);
		sb.append(hiddenUserId);
		return sb.toString();
	}

	/**
	 * Given a list of job pairs, creates a JsonObject that can be used to populate a datatable client-side
	 * It seems this is used to populate the job pairs table in the job view page
	 *
	 * @param pairs The pairs that will be the rows of the table
	 * @param query a DataTables query object
	 * @param useWallclock Whether to use wallclock time (true) or cpu time (false)
	 * @param stageNumber The number of the stage to use the data from for each pair
	 * @param primitivesToAnonymize PrimitivesToAnonymize object representing whether benchmarks, solvers, or both
	 * should be anonymized.
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertJobPairsToJsonObject(List<JobPair> pairs, DataTablesQuery query, boolean useWallclock, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize) {

		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		String solverLink = null;
		String configLink = null;
		for (JobPair jp : pairs) {
			JoblineStage stage = jp.getStageFromNumber(stageNumber);

			String benchLink = getBenchLinkWithHiddenPairId(jp.getBench(), jp.getId(), primitivesToAnonymize);

			// Create the solver link
			solverLink = getSolverLink(stage.getSolver().getId(), stage.getSolver().getName(), primitivesToAnonymize);
			// Create the configuration link
			configLink = getConfigLink(stage.getSolver().getConfigurations().get(0).getId(), stage.getSolver().getConfigurations().get(0).getName(), primitivesToAnonymize);


			// Create the status field
			String status =
					"<a title=\"" + stage.getStatus().getDescription() + "\">" + stage.getStatus().getStatus() + " (" +
					stage.getStatus().getCode().getVal() + ")" + "</a>";

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(benchLink));
			entry.add(new JsonPrimitive(solverLink));
			entry.add(new JsonPrimitive(configLink));


			entry.add(new JsonPrimitive(status));
			if (useWallclock) {
				double displayWC = Math.round(stage.getWallclockTime() * 100) / 100.0;

				entry.add(new JsonPrimitive(displayWC + " s"));
			} else {
				double displayCpu = Math.round(stage.getCpuTime() * 100) / 100.0;

				entry.add(new JsonPrimitive(displayCpu + " s"));
			}

			entry.add(new JsonPrimitive(stage.getStarexecResult()));
			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Given a list of jobs, creates a JsonObject that can be used to populate a
	 * datatable client-side
	 *
	 * @param jobs The jobs that will be the rows of the table
	 * @param query A DataTablesQuery object
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertJobsToJsonObject(List<Job> jobs, DataTablesQuery query, boolean dataAsObjects) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (Job job : jobs) {
			StringBuilder sb = new StringBuilder();
			String hiddenJobId;

			// Create the hidden input tag containing the job id
			sb.append("<input type=\"hidden\" value=\"");
			sb.append(job.getId());
			sb.append("\" prim=\"job\" userId=\"").append(job.getUserId()).append("\"  deleted=\"")
			  .append(job.isDeleted()).append("\"/>");
			hiddenJobId = sb.toString();

			// Create the job "details" link and append the hidden input element
			sb = new StringBuilder();
			sb.append("<a href=\"").append(Util.docRoot("secure/details/job.jsp?id="));
			sb.append(job.getId());
			sb.append("\" target=\"_blank\">");
			sb.append(job.getName());
			RESTHelpers.addImg(sb);
			sb.append(hiddenJobId);
			String jobLink = sb.toString();

			final String status = job.getStatus();

			if (dataAsObjects) {
				dataTablePageEntries.add(getEntryAsObject(jobLink, status, job));
			} else {
				dataTablePageEntries.add(getEntryAsArray(jobLink, status, job));
			}

		}

		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Convert a List of Job into a detailed JsonArray
	 *
	 * @param jobs The jobs that will be the rows of the table
	 * @return A JsonObject that can be used to populate a datatable
	 */
	public static JsonArray convertJobsToJsonArray(List<Job> jobs) {
		final Map<Integer, JsonObject> users = new HashMap<>();
		final JsonArray out = new JsonArray();
		for (Job job : jobs) {
			final JsonObject j = new JsonObject();
			j.addProperty("name", job.getName());
			j.addProperty("id", job.getId());
			j.addProperty("status", job.getStatus());
			j.addProperty("created", job.getCreateTime().getTime());
			j.addProperty("totalPairs", job.getLiteJobPairStats().get("totalPairs"));
			j.addProperty("pendingPairs", job.getLiteJobPairStats().get("pendingPairs"));

			final int userId = job.getUserId();
			if (!users.containsKey(userId)) {
				final JsonObject o = new JsonObject();
				final User jobUser = Users.get(userId);
				o.addProperty("name", jobUser.getFullName());
				o.addProperty("id",   jobUser.getId());
				users.put(userId, o);
			}
			j.add("user", users.get(userId));

			try {
				final JsonObject queue = new JsonObject();
				queue.addProperty("name", job.getQueue().getName());
				queue.addProperty("id", job.getQueue().getId());
				j.add("queue", queue);
			} catch (NullPointerException e) {
				// Not all jobs have queues
			}

			out.add(j);
		}
		return out;
	}

	private static JsonArray getEntryAsArray(String jobLink, String status, Job job) {
		// Create an object, and inject the above HTML, to represent an
		// entry in the DataTable
		JsonArray entry = new JsonArray();
		entry.add(new JsonPrimitive(jobLink));
		entry.add(new JsonPrimitive(status));
		entry.add(new JsonPrimitive(getPercentStatHtml("asc", job.getLiteJobPairStats().get("completionPercentage"), true)));
		entry.add(new JsonPrimitive(getPercentStatHtml("static", job.getLiteJobPairStats().get("totalPairs"), false)));
		entry.add(new JsonPrimitive(getPercentStatHtml("desc", job.getLiteJobPairStats().get("errorPercentage"), true)));
		entry.add(new JsonPrimitive(job.getCreateTime().toString()));
		entry.add(new JsonPrimitive(Util.byteCountToDisplaySize(job.getDiskSize())));
		return entry;
	}

	private static JsonObject getEntryAsObject(String jobLink, String status, Job job) {
		// Create an object, and inject the above HTML, to represent an
		// entry in the DataTable
		JsonObject entry = new JsonObject();
		entry.add("jobLink", new JsonPrimitive(jobLink));
		entry.add("status", new JsonPrimitive(status));
		entry.add("completion", new JsonPrimitive(getPercentStatHtml("asc", job.getLiteJobPairStats().get("completionPercentage"), true)));
		entry.add("totalPairs", new JsonPrimitive(getPercentStatHtml("static", job.getLiteJobPairStats().get("totalPairs"), false)));
		entry.add("errorPercentage", new JsonPrimitive(getPercentStatHtml("desc", job.getLiteJobPairStats().get("errorPercentage"), true)));

		entry.add("createTime", new JsonPrimitive(job.getCreateTime().toString()));
		JsonObject diskSize = new JsonObject();
		diskSize.add("display", new JsonPrimitive(Util.byteCountToDisplaySize(job.getDiskSize())));
		diskSize.add("bytes", new JsonPrimitive(job.getDiskSize()));
		entry.add("diskSize", diskSize);
		return entry;
	}

	/**
	 * Generate the HTML for the next DataTable page of entries
	 * Given a list of users, creates a JsonObject that can be used to populate
	 * a datatable client-side
	 *
	 * @param users The users that will be the rows of the table
	 * @param query a DataTablesQuery object
	 * @param currentUserId the ID of the user making the request for this datatable
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertUsersToJsonObject(List<User> users, DataTablesQuery query, int currentUserId) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (User user : users) {
			String userLink = getUserLink(user.getId(), user.getFullName(), currentUserId);

			StringBuilder sb = new StringBuilder();
			sb.append("<a href=\"mailto:");
			sb.append(user.getEmail());
			sb.append("\">");
			sb.append(user.getEmail());
			RESTHelpers.addImg(sb);
			String emailLink = sb.toString();

			sb = new StringBuilder();
			sb.append("<input type=\"button\" onclick=\"editPermissions(").append(user.getId())
			  .append(")\" value=\"Edit\"/>");
			String permissionButton = sb.toString();

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(userLink));
			entry.add(new JsonPrimitive(user.getInstitution()));
			entry.add(new JsonPrimitive(emailLink));
			entry.add(new JsonPrimitive(permissionButton));

			String suspendButton = "";
			if (Users.isAdmin(user.getId()) || Users.isUnauthorized(user.getId())) {
				suspendButton = "N/A";
			} else if (Users.isSuspended(user.getId())) {
				sb = new StringBuilder();
				sb.append("<input type=\"button\" onclick=\"reinstateUser(").append(user.getId())
				  .append(")\" value=\"Reinstate\"/>");
				suspendButton = sb.toString();
			} else if (Users.isNormalUser(user.getId())) {
				sb = new StringBuilder();
				sb.append("<input type=\"button\" onclick=\"suspendUser(").append(user.getId())
				  .append(")\" value=\"Suspend\"/>");
				suspendButton = sb.toString();
			}
			entry.add(new JsonPrimitive(suspendButton));

			String subscribeButton = "";
			if (Users.isUnauthorized(user.getId())) {
				subscribeButton = "N/A";
			} else if (user.isSubscribedToReports()) {
				subscribeButton = "<input type=\"button\" onclick=\"unsubscribeUserFromReports(" + user.getId() + ")\" value=\"Unsubscribe\"/>";
			} else {
				subscribeButton = "<input type=\"button\" onclick=\"subscribeUserToReports(" + user.getId() + ")\" value=\"Subscribe\"/>";
			}
			entry.add(new JsonPrimitive(subscribeButton));

			String developerButton = "";
			if (Users.isAdmin(user.getId()) || Users.isUnauthorized(user.getId()) || Users.isSuspended(user.getId())) {
				developerButton = "N/A";
			} else if (Users.isDeveloper(user.getId())) {
				developerButton = "<input type=\"button\" onclick=\"suspendDeveloperStatus(" + user.getId() + ")\"value=\"Suspend\"/>";
			} else {
				developerButton = "<input type=\"button\" onclick=\"grantDeveloperStatus(" + user.getId() + ")\"value=\"Grant\"/>";
			}
			entry.add(new JsonPrimitive(developerButton));


			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Generate the HTML for the next DataTable page of entries
	 * Given a list of TestSequences, creates a JsonObject that can be used to populate
	 * a datatable client-side
	 *
	 * @param tests The tests that will be the rows of the table
	 * @param query a DataTablesQuery object
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertTestSequencesToJsonObject(List<TestSequence> tests, DataTablesQuery query) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (TestSequence test : tests) {
			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			StringBuilder sb = new StringBuilder();
			sb.append("<a name=\"").append(test.getName()).append("\" href=\"")
			  .append(Util.docRoot("secure/admin/testResults.jsp?sequenceName="));
			sb.append(test.getName());
			sb.append("\" target=\"_blank\">");
			sb.append(test.getName());
			RESTHelpers.addImg(sb);

			entry.add(new JsonPrimitive(sb.toString()));
			entry.add(new JsonPrimitive(test.getTestCount()));
			entry.add(new JsonPrimitive(test.getTestsPassed()));
			entry.add(new JsonPrimitive(test.getTestsFailed()));
			entry.add(new JsonPrimitive(test.getStatus().getStatus()));
			entry.add(new JsonPrimitive(test.getErrorTrace()));
			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Generate the HTML for the next DataTable page of entries
	 * Given a HashMap mapping the names of tests to messages, creates a JsonObject that can be used to populate
	 * a datatable client-side
	 *
	 * @param tests A HashMap of tests, where each test will be a row of a table
	 * @param query A DataTablesQuery object
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertTestResultsToJsonObject(List<TestResult> tests, DataTablesQuery query) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (TestResult test : tests) {
			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable

			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(test.getName()));
			entry.add(new JsonPrimitive(test.getStatus().getStatus()));
			//replacing newlines with HTML line breaks
			entry.add(new JsonPrimitive(test.getAllMessages().replace("\n", "<br/>")));
			entry.add(new JsonPrimitive(test.getErrorTrace()));
			entry.add(new JsonPrimitive(test.getTime()));
			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Given a list of spaces, creates a JsonObject that can be used to populate
	 * a datatable client-side
	 *
	 * @param spaces The spaces that will be the rows of the table
	 * @param query a DataTablesQuery object
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertSpacesToJsonObject(List<Space> spaces, DataTablesQuery query) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (Space space : spaces) {
			String spaceLink = getSpaceLink(space);

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(spaceLink));
			entry.add(new JsonPrimitive(space.getDescription()));

			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/**
	 * Given a list of solvers, creates a JsonObject that can be used to
	 * populate a datatable client-side
	 *
	 * @param solvers The solvers that will be the rows of the table
	 * @param query DataTablesQuery object
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertSolversToJsonObject(List<Solver> solvers, DataTablesQuery query) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (Solver solver : solvers) {
			StringBuilder sb = new StringBuilder();

			// Create the hidden input tag containing the solver id
			sb.append("<input type=\"hidden\" value=\"");
			sb.append(solver.getId());
			sb.append("\" prim=\"solver\" userId=\"").append(solver.getUserId()).append("\" deleted=\"")
			  .append(solver.isDeleted()).append("\" recycled=\"").append(solver.isRecycled()).append("\"/>");
			String hiddenSolverId = sb.toString();

			// Create the solver "details" link and append the hidden input
			// element
			sb = new StringBuilder();
			sb.append(getSolverLink(solver.getId(), solver.getName(), PrimitivesToAnonymize.NONE));
			sb.append(hiddenSolverId);
			String solverLink = sb.toString();

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(solverLink));
			entry.add(new JsonPrimitive(solver.getDescription()));
			entry.add(new JsonPrimitive(solver.getType().toString().toLowerCase()));
			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

		/*given a list of the current page of	 benchmark uploads for some user, convert this to a json object
		* @param uploads List of the uploads
		* @param query Data about the query
		* Documentation by @aguo2
		* @author unknown
		*/
		
        public static JsonObject convertUploadsToJsonObject(List<BenchmarkUploadStatus> uploads, DataTablesQuery query) {
	    JsonArray dataTablePageEntries = new JsonArray();
	    for (BenchmarkUploadStatus upload: uploads) {
			StringBuilder sb = new StringBuilder();
			sb.append("<input type=\"hidden\" value =\"");
			sb.append(upload.getId());
			sb.append("\" prim=\"upload\" userId=\"").append(upload.getUserId()).append("\"/>");
			String hiddenUploadId = sb.toString();
			sb = new StringBuilder();
			sb.append("<a href =\"").append(Util.docRoot("secure/details/uploadStatus.jsp?id="));
			sb.append(upload.getId());
			sb.append("\" target=\"_blank\">");
			sb.append(upload.getUploadDate().toString());
			RESTHelpers.addImg(sb);
			sb.append(hiddenUploadId);
			String uploadLink = sb.toString();
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(uploadLink));
			entry.add(new JsonPrimitive(upload.getTotalBenchmarks()));
			entry.add(new JsonPrimitive(upload.isEverythingComplete()));
			dataTablePageEntries.add(entry);
	    }
		
		JsonObject entries = createPageDataJsonObject(query, dataTablePageEntries);
	    return entries;
	}



	/**
	 * Given a list of benchmarks, creates a JsonObject that can be used to
	 * populate a datatable client-side
	 *
	 * @param benchmarks The benchmarks that will be the rows of the table
	 * @param query a DataTablesQuery object
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns
	 */
	public static JsonObject convertBenchmarksToJsonObject(List<Benchmark> benchmarks, DataTablesQuery query) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (Benchmark bench : benchmarks) {
			String benchLink = getBenchLink(bench);
			// Create the benchmark type tag
			// Set the tooltip to be the benchmark type's description
			String typeSpan =
					"<span title=\"" + bench.getType().getDescription() + "\">" + bench.getType().getName() + "</span>";

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(benchLink));
			entry.add(new JsonPrimitive(typeSpan));

			dataTablePageEntries.add(entry);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	private static StringBuilder getPairsInSpaceLink(String type, int spaceId, int configId, int stageNumber) {
		StringBuilder sb = new StringBuilder();
		sb.append("<a href=\"").append(Util.docRoot(
				"secure/details/pairsInSpace.jsp?type=" + type + "&sid=" + spaceId + "&configid=" + configId +
						"&stagenum=" + stageNumber));
		sb.append("\" target=\"_blank\" >");
		return sb;
	}

	/**
	 * Given a list of SolverStats, creates a JsonObject that can be used to
	 * populate a datatable client-side.
	 * In any other case, this should produce objects with named fields.
	 * However, this particular API can produce output with many thousands of
	 * rows of output. Naming each field would more than triple the size of the
	 * file. In order to keep the size of the output manageable, we are simply
	 * returning an array. This makes this API particularly fragile, and the
	 * server-side and client-side must be kept in sync!
	 *
	 * @param stats The SolverStats that will be the rows of the table
	 * @param query a DataTablesQuery object
	 * @param shortFormat Whether to include all fields (false) or only fields for the subspace overview (true)
	 * @param wallTime Whether to use wallclock times (true) or cpu times (false).
	 * @param primitivesToAnonymize a PrimitivesToAnonymize enum describing if the solver stats should be anonymized.
	 * @return A JsonObject that can be used to populate a datatable
	 * @author Eric Burns+
	 * @author Pat Hawks
	 */
	public static JsonObject convertSolverStatsToJsonObject(Collection<SolverStats> stats, DataTablesQuery query, int spaceId, int jobId, boolean shortFormat, boolean wallTime, PrimitivesToAnonymize primitivesToAnonymize) {
		JsonArray dataTablePageEntries = new JsonArray();
		for (SolverStats js : stats) {
			JsonArray entries = new JsonArray();

			entries.add(js.getSolver().getId());
			entries.add(js.getConfiguration().getId());
			entries.add(js.getSolver().getName());
			entries.add(js.getConfiguration().getName());
			entries.add(js.getCorrectOverCompleted());

			if (wallTime) {
				entries.add(Math.round(js.getWallTime() * 100));
			} else {
				entries.add(Math.round(js.getCpuTime() * 100));
			}

			if (!shortFormat) {
				entries.add(js.getStageNumber());
				entries.add(js.getIncorrectJobPairs());
				entries.add(js.getResourceOutJobPairs());
				entries.add(js.getFailedJobPairs());
				entries.add(js.getUnknown());
				entries.add(js.getIncompleteJobPairs());

				if (AnonymousLinks.isNothingAnonymized(primitivesToAnonymize)) {
					entries.add(js.getConflicts());
				} else {
					// Don't support conflicts for anonymized pages.
					entries.add("N/A");
				}
			}

			// add index 13, CONFIG_DELETED, for dynamic config link; see getSolverTableInitializer() in job.js
			entries.add( js.getConfigDeleted() );

			// debug for queuegraph
			log.info( "\n\nin convertSolverStatsToJsonObject(); " +
					"CONFIG_DELETED = " +  js.getConfigDeleted() + "\n" );

			dataTablePageEntries.add(entries);
		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	private static String getPairsInSpaceHtml(int spaceId, int configId, String linkText) {
		StringBuilder sb = getPairsInSpaceLink("all", spaceId, configId, 1);
		sb.append(linkText);
		RESTHelpers.addImg(sb);
		return sb.toString();
	}

	public static Map<Integer, String> getJobSpaceIdToSolverStatsJsonMap(List<JobSpace> jobSpaces, int stageNumber, boolean wallclock, Boolean includeUnknown) {
		Map<Integer, String> jobSpaceIdToSolverStatsJsonMap = new HashMap<>();
		for (JobSpace jobSpace : jobSpaces) {
			Collection<SolverStats> stats = Jobs.getAllJobStatsInJobSpaceHierarchyIncludeDeletedConfigs(jobSpace, stageNumber, PrimitivesToAnonymize.NONE, includeUnknown);
			DataTablesQuery query = new DataTablesQuery();
			query.setTotalRecords(stats.size());
			query.setTotalRecordsAfterQuery(stats.size());
			query.setSyncValue(1);
			JsonObject solverStatsJson = RESTHelpers.convertSolverStatsToJsonObject(stats, query, jobSpace.getId(), jobSpace.getJobId(), false, wallclock, PrimitivesToAnonymize.NONE);
			if (solverStatsJson != null) {
				jobSpaceIdToSolverStatsJsonMap.put(jobSpace.getId(), gson.toJson(solverStatsJson));
			}
		}
		return jobSpaceIdToSolverStatsJsonMap;
	}

	public static Map<Integer, String> getJobSpaceIdToSubspaceJsonMap(int jobId, List<JobSpace> jobSpaces) {
		Map<Integer, String> jobSpaceIdToSubspaceJsonMap = new HashMap<>();
		for (JobSpace jobSpace : jobSpaces) {
			String subspaceJson = RESTHelpers.getJobSpacesJson(jobSpace.getId(), jobId, false);
			jobSpaceIdToSubspaceJsonMap.put(jobSpace.getId(), subspaceJson);
		}
		return jobSpaceIdToSubspaceJsonMap;
	}

	/**
	 * Gets a JSON representation of a job space tree
	 *
	 * @param jobId
	 * @param userId
	 * @return
	 */
	public static String getJobSpacesTreeJson(int jobId, int userId) {
		ValidatorStatusCode status = JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			String output = gson.toJson(status);
			log.debug("User cannot see job, getJobSpacesJson output: " + output);
			return output;
		}

		List<JSTreeItem> subspaces = new ArrayList<>();
		buildFullJsTree(jobId, subspaces);

		return gson.toJson(subspaces);
	}

	/**
	 * Builds the entire JS tree for a jobspace rather than just a single level.
	 * This method is needed for the local job page since we can't send GET requests
	 * for single levels.
	 *
	 * @author Albert Giegerich
	 */
	private static void buildFullJsTree(int jobId, List<JSTreeItem> root) {
		buildFullJsTreeHelper(0, jobId, root, true);
	}

	private static List<JobSpace> getSubspacesOrRootSpace(int parentId, int jobId) {
		List<JobSpace> subspaces = new ArrayList<>();
		if (parentId > 0) {
			subspaces = Spaces.getSubSpacesForJob(parentId, false);
		} else {
			//if the id given is 0, we want to get the root space
			Job j = Jobs.get(jobId);
			JobSpace s = Spaces.getJobSpace(j.getPrimarySpace());
			subspaces.add(s);
		}
		return subspaces;
	}

	/**
	 * Helper method for buildFullJsTree
	 *
	 * @see org.starexec.app.RESTHelpers#buildFullJsTree
	 */
	private static void buildFullJsTreeHelper(int parentId, int jobId, List<JSTreeItem> root, boolean firstRecursion) {
		List<JobSpace> subspaces = getSubspacesOrRootSpace(parentId, jobId);

		String className = (firstRecursion ? "rootNode" : null);

		for (JobSpace js : subspaces) {
			JSTreeItem node = null;
			if (Spaces.getCountInJobSpace(js.getId()) > 0) {
				node = new JSTreeItem(js.getName(), js.getId(), "closed", R.SPACE, js.getMaxStages(), className);
			} else {
				node = new JSTreeItem(js.getName(), js.getId(), "leaf", R.SPACE, js.getMaxStages(), className);
			}
			root.add(node);
			buildFullJsTreeHelper(js.getId(), jobId, node.getChildren(), false);
		}
	}

	public static String validateAndGetJobSpacesJson(int parentId, int jobId, boolean makeSpaceTree, int userId) {
		log.debug("got here with jobId= " + jobId + " and parent space id = " + parentId);
		log.debug("getting job spaces for panels");
		//don't populate the subspaces if the user can't see the job
		ValidatorStatusCode status = JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			String output = gson.toJson(status);
			log.debug("User cannot see job, getJobSpacesJson output: " + output);
			return output;
		}

		return getJobSpacesJson(parentId, jobId, makeSpaceTree);
	}

	protected static String getSolverComparisonGraphJson(int jobSpaceId, int config1, int config2, int edgeLengthInPixels, String axisColor, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize) {

		List<String> chartPath = null;

		Color c = Util.getColorFromString(axisColor);
		if (c == null) {
			return gson.toJson(new ValidatorStatusCode(false, "The given color is not valid"));
		}
		if (edgeLengthInPixels <= 0 || edgeLengthInPixels > 2000) {
			return gson.toJson(new ValidatorStatusCode(false, "The given size is not valid: please choose an integer from 1-2000"));

		}

		chartPath = Statistics.makeSolverComparisonChart(config1, config2, jobSpaceId, edgeLengthInPixels, c, stageNumber, primitivesToAnonymize);
		if (chartPath == null) {
			return gson.toJson(RESTServices.ERROR_DATABASE);
		}
		if (chartPath.get(0).equals(Statistics.OVERSIZED_GRAPH_ERROR)) {
			return gson.toJson(RESTServices.ERROR_TOO_MANY_JOB_PAIRS);
		}
		JsonObject json = new JsonObject();
		json.addProperty("src", chartPath.get(0));
		json.addProperty("map", chartPath.get(1));

		return gson.toJson(json);
	}

	protected static String getJobSpacesJson(int parentId, int jobId, boolean makeSpaceTree) {
		return getJobSpacesJson(parentId, jobId, makeSpaceTree, PrimitivesToAnonymize.NONE);
	}

	protected static String getJobSpacesJson(int parentId, int jobId, boolean makeSpaceTree, PrimitivesToAnonymize primitivesToAnonymize) {
		log.debug("got a request for parent space = " + parentId);
		List<JobSpace> subspaces = getSubspacesOrRootSpace(parentId, jobId);

		if (AnonymousLinks.areJobsAnonymized(primitivesToAnonymize)) {
			anonymizeJobSpaceNames(subspaces, jobId);
		}

		log.debug("making next tree layer with " + subspaces.size() + " spaces");
		if (makeSpaceTree) {
			String output = gson.toJson(RESTHelpers.toJobSpaceTree(subspaces));
			log.debug("makeSpaceTree is true, getJobSpacesJson output: " + output);
			return output;
		} else {
			String output = gson.toJson(subspaces);
			log.debug("makeSpaceTree is false, getJobSpacesJson output: " + output);
			return output;
		}
	}

	protected static void anonymizeJobSpaceNames(List<JobSpace> jobSpaces, int jobId) {
		final String methodName = "anonymizeJobSpaceNames";
		log.entry(methodName);

		Map<Integer, String> jobSpaceNames = AnonymousLinks.getAnonymizedJobSpaceNames(jobId);
		for (JobSpace space : jobSpaces) {
			space.setName(jobSpaceNames.get(space.getId()));
		}
	}

	public static JsonObject convertCommunityRequestsToJsonObject(List<CommunityRequest> requests, DataTablesQuery query, int currentUserId) {
		/*
		  Generate the HTML for the next DataTable page of entries
		 */
		JsonArray dataTablePageEntries = new JsonArray();
		for (CommunityRequest req : requests) {
			User user = Users.get(req.getUserId());
			String userLink = getUserLink(user.getId(), user.getFullName(), currentUserId);

			//Community/space
			String spaceLink = getSpaceLink(Spaces.get(req.getCommunityId()));

			StringBuilder sb = new StringBuilder();
			sb.append("<input class=\"acceptRequestButton\" type=\"button\" data-code=\"").append(req.getCode())
			  .append("\" value=\"Approve\" />");
			String approveButton = sb.toString();

			sb = new StringBuilder();
			sb.append("<input type=\"button\" class=\"declineRequestButton\"" + "data-code=\"").append(req.getCode())
			  .append("\" value=\"Decline\"/>");
			String declineButton = sb.toString();

			// Create an object, and inject the above HTML, to represent an
			// entry in the DataTable
			JsonArray entry = new JsonArray();
			entry.add(new JsonPrimitive(userLink));
			entry.add(new JsonPrimitive(spaceLink));
			entry.add(new JsonPrimitive(req.getMessage()));
			entry.add(new JsonPrimitive(approveButton));
			entry.add(new JsonPrimitive(declineButton));

			dataTablePageEntries.add(entry);

		}
		return createPageDataJsonObject(query, dataTablePageEntries);
	}

	/*
	 * Given an JSONArray of the next page and the query,
	 * return a JsonObject of the elements displayed in the front end table
	 * @author PressDodd
	 * @docs aguo2
	 */
	private static JsonObject createPageDataJsonObject(DataTablesQuery query, JsonArray entries) {
		JsonObject nextPage = new JsonObject();
		// Build the actual JSON response object and populated it with the
		// created data
		nextPage.addProperty(SYNC_VALUE, query.getSyncValue());
		nextPage.addProperty(TOTAL_RECORDS, query.getTotalRecords());
		nextPage.addProperty(TOTAL_RECORDS_AFTER_QUERY, query.getTotalRecordsAfterQuery());
		nextPage.add("aaData", entries);
		// Return the next DataTable page
		return nextPage;
	}

	/**
	 * Gets all pending community requests for a given community.
	 *
	 * @param httpRequest The http request.
	 * @param communityId The community to get pending requests for.
	 * @return the new json object or null on error.
	 * @author Albert Giegerich
	 */
	public static JsonObject getNextDataTablesPageForPendingCommunityRequestsForCommunity(HttpServletRequest httpRequest, int communityId) {

		// Parameter Validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.SPACE, httpRequest);
		if (query == null) {
			return null;
		}

		List<CommunityRequest> requests = null;
		try {
			requests = Requests.getPendingCommunityRequestsForCommunity(query, communityId);
			query.setTotalRecords(Requests.getCommunityRequestCountForCommunity(communityId));
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		} catch (StarExecDatabaseException e) {
			log.error("Could not successfully get community requests for community with id=" + communityId, e);
			return null;
		}


		return setupAttrMapAndConvertRequestsToJson(requests, query, httpRequest);
	}

	/**
	 * Gets all pending community requests.
	 *
	 * @param httpRequest The http request.
	 * @return a JsonObject representing the requests.
	 * @author Albert Giegerich
	 */
	public static JsonObject getNextDataTablesPageForPendingCommunityRequests(HttpServletRequest httpRequest) {
		// Parameter Validation
		DataTablesQuery query = RESTHelpers.getAttrMap(Primitive.SPACE, httpRequest);
		if (query == null) {
			return null;
		}

		List<CommunityRequest> requests = null;
		try {
			requests = Requests.getPendingCommunityRequests(query);
			query.setTotalRecords(Requests.getCommunityRequestCount());
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		} catch (StarExecDatabaseException e) {
			log.error("Could not successfully get community requests for all communities.", e);
			return null;
		}

		return setupAttrMapAndConvertRequestsToJson(requests, query, httpRequest);
	}

	/**
	 * Provides an abstraction so the same code can be used when we want to get all pending community requests or
	 * just requests for a given community.
	 *
	 * @param httpRequest The http request.
	 * @author Unknown, Albert Giegerich
	 */
	private static JsonObject setupAttrMapAndConvertRequestsToJson(List<CommunityRequest> requests, DataTablesQuery query, HttpServletRequest httpRequest) {
		// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		if (!query.hasSearchQuery()) {
			query.setTotalRecordsAfterQuery(query.getTotalRecords());
		}
		// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS
		else {
			query.setTotalRecordsAfterQuery(requests.size());
		}
		int currentUserId = SessionUtil.getUserId(httpRequest);
		return convertCommunityRequestsToJsonObject(requests, query, currentUserId);
	}

	private static Map<String, Triple<Integer, Double, Double>> initializeAttrCounts(List<String> headers) {
		Map<String, Triple<Integer, Double, Double>> attrCounts = new HashMap<>();
		for (String header : headers) {
			attrCounts.put(header, new ImmutableTriple<>(0, 0.0, 0.0));
		}
		return attrCounts;
	}

	/**
	 * Creates a map from a solver-config pair to a map from an attribute to the count of attributes (results) generated
	 * by that solver-config as well as the time it took to create all those results.
	 *
	 * @param jobSpaceId the jobspace to generate the map for, only job pairs in this jobspace will be examined.
	 * @throws SQLException if there is a database issue.
	 */
	private static Map<SolverConfig, Map<String, Triple<Integer, Double, Double>>> getSolverConfigToAttrCountMap(int jobSpaceId) throws SQLException {
		Map<SolverConfig, Map<String, Triple<Integer, Double, Double>>> solverConfigToAttrCount = new HashMap<>();
		List<AttributesTableData> jobAttributes = Jobs.getJobAttributesTable(jobSpaceId);
		List<String> uniqueResultValues = Jobs.getJobAttributeValues(jobSpaceId);
		for (AttributesTableData tableEntry : jobAttributes) {
			// Initialize a solverConfig to be used as a key in our map.
			SolverConfig solverConfig = new SolverConfig(tableEntry.solverId, tableEntry.configId);
			// Add this optional data so we can populate the table with it later.
			solverConfig.solverName = tableEntry.solverName;
			solverConfig.configName = tableEntry.configName;

			// Initialize new entries in the map with a 0 count for each attribute.
			if (!solverConfigToAttrCount.containsKey(solverConfig)) {
				Map<String, Triple<Integer, Double, Double>> zeroAttrCounts = initializeAttrCounts(uniqueResultValues);
				solverConfigToAttrCount.put(solverConfig, zeroAttrCounts);
			}

			// Populate the map with the count and times in the table entry.
			solverConfigToAttrCount.get(solverConfig).put(tableEntry.attrValue, new ImmutableTriple<>(tableEntry.attrCount, tableEntry.wallclockSum, tableEntry.cpuSum));
		}
		return solverConfigToAttrCount;
	}

	/**
	 * Creates the attribute table for details/jobAttributes as a list.
	 *
	 * @see #getSolverConfigToAttrCountMap(int)
	 */
	public static List<AttributesTableRow> getAttributesTable(int jobSpaceId) throws SQLException {
		Map<SolverConfig, Map<String, Triple<Integer, Double, Double>>> solverConfigToAttrCount = getSolverConfigToAttrCountMap(jobSpaceId);

		List<AttributesTableRow> table = new ArrayList<>();
		for (SolverConfig solverConfig : solverConfigToAttrCount.keySet()) {
			AttributesTableRow row = new AttributesTableRow();

			row.solverId = solverConfig.solverId;
			row.configId = solverConfig.configId;
			row.solverName = solverConfig.solverName;
			row.configName = solverConfig.configName;

			// Add all the attr_value counts under the appropriate headers. To do this we sort the list of headers.
			// The headers will need to be sorted in the same way so the columns line up.
			Map<String, Triple<Integer, Double, Double>> valueCounts = solverConfigToAttrCount.get(solverConfig);
			List<String> attrValues = new ArrayList<>(valueCounts.keySet()).stream().sorted().collect(Collectors.toList());
			for (String attrValue : attrValues) {
				Triple<Integer, Double, Double> countWallclockCpu = valueCounts.get(attrValue);
				Triple<Integer, String, String> formattedCountWallclockCpu = new ImmutableTriple<>(countWallclockCpu.getLeft(), String.format("%.4f", countWallclockCpu.getMiddle()), String.format("%.4f", countWallclockCpu.getRight()));
				row.countAndTimes.add(formattedCountWallclockCpu);
			}
			table.add(row);
		}
		return table;
	}

	protected static String getWallclockCpuAttributeTableHtml(Double wallclockSum, Double cpuSum) {
		String formatter = "%.3f";
		String formattedWallclock = String.format(formatter, wallclockSum);
		String formattedCpu = String.format(formatter, cpuSum);
		return "<span class='wallclockSum'>" + formattedWallclock + "</span>" + "<span class='cpuSum hidden'>" + formattedCpu + "</span>";
	}

	public static boolean freezePrimitives() throws SQLException {
		return Common.query(
				"{CALL GetFreezePrimitives()}",
				procedure -> {},
				results -> {
					results.next();
					return results.getBoolean("freeze_primitives");
				}
		);
	}

	
	public static void setReadOnly(boolean readOnly) throws SQLException{
		log.debug(
			readOnly ? "READ ONLY IS ENABLED, no new jobs can be ran" 
			: "READ ONLY IS DISABLED, jobs can be ran normally"
		);
			Common.update(
				"{CALL SetReadOnly(?)}",
				procedure -> {
				procedure.setBoolean(1, readOnly);
				});
		
	}

	public static boolean getReadOnly() throws SQLException {
		return Common.query(
				"{CALL GetReadOnly()}",
				procedure -> {},
				results -> {
					results.next();
					return results.getBoolean("read_only");
				}
		);

	}

	public static void setFreezePrimitives(boolean frozen) throws SQLException {
		log.info("setFreezePrimitives",
			frozen
			? "!!! Freezing Primitives !!!\n\tUploading Benchmarks and Solvers will be disabled"
			: "!!! Unfreezing Primitives !!!\n\tUploading Benchmarks and Solvers will be allowed"
		);
		Common.update(
				"{CALL SetFreezePrimitives(?)}",
				procedure -> {
					procedure.setBoolean(1, frozen);
				}
		);
	}

	/**
	 * Represents a node in jsTree tree with certain attributes used for
	 * displaying the node and obtaining information about the node.
	 *
	 * @author Tyler Jensen
	 */
	@SuppressWarnings("unused")
	protected static class JSTreeItem {
		private String data;
		private JSTreeAttribute attr;
		private List<JSTreeItem> children;
		private String state;

		public JSTreeItem(String name, int id, String state, String type) {
			this(name, id, state, type, 0, null);
		}

		public JSTreeItem(String name, int id, String state, String type, int maxStages) {
			this(name, id, state, type, maxStages, null);
		}

		public JSTreeItem(String name, int id, String state, String type, int maxStages, String cLass) {
			this.data = name;
			this.attr = new JSTreeAttribute(id, type, maxStages, cLass);
			this.state = state;
			this.children = new LinkedList<>();
		}

		public List<JSTreeItem> getChildren() {
			return children;
		}

		public void addChild(JSTreeItem child) {
			children.add(child);
		}
	}

	/**
	 * An attribute of a jsTree node which holds the node's id so that it can be
	 * passed to other ajax methods.
	 *
	 * @author Tyler Jensen
	 */
	@SuppressWarnings("unused")
	protected static class JSTreeAttribute {
		private int id;
		private String rel;
		private boolean global;
		private int defaultQueueId;
		private int maxStages;
		// called cLass to bypass Java's class keyword. gson will lowercase the L
		private String cLass;

		public JSTreeAttribute(int id, String type, int maxStages, String cLass) {
			this.id = id;
			this.rel = type;
			this.maxStages = maxStages;
			this.cLass = cLass;
			if (type.equals("active_queue") || type.equals("inactive_queue")) {
				this.global = Queues.isQueueGlobal(id);
			}
			this.defaultQueueId = R.DEFAULT_QUEUE_ID;
		}
	}

	/**
	 * Represents a space and a user's permission for that space. This is purely
	 * a helper class so we can easily read the information via javascript on
	 * the client.
	 *
	 * @author Tyler Jensen & Todd Elvers
	 */
	protected static class SpacePermPair {
		@Expose
		private final Space space;
		@Expose
		private final Permission perm;

		public SpacePermPair(Space s, Permission p) {
			this.space = s;
			this.perm = p;
		}
	}

	/**
	 * Represents community details including the requesting user's permissions
	 * for the community aint with the community's leaders. Permissions are used
	 * so the client side can determine what actions a user can take on the
	 * community
	 *
	 * @author Tyler Jensen
	 */
	protected static class CommunityDetails {
		@Expose
		private final Space space;
		@Expose
		private final Permission perm;
		@Expose
		private final List<User> leaders;
		@Expose
		private final List<Website> websites;

		@Expose
		private final Boolean isMember;

		public CommunityDetails(Space s, Permission p, List<User> leaders, List<Website> websites, Boolean member) {
			this.space = s;
			this.perm = p;
			this.leaders = leaders;
			this.websites = websites;
			this.isMember = member;
		}
	}

	/**
	 * Represents permission details for a given space and user
	 *
	 * @author Wyatt Kaiser
	 */
	protected static class PermissionDetails {
		@Expose
		private final Permission perm;
		@Expose
		private final Space space;
		@Expose
		private final User user;
		@Expose
		private final User requester;
		@Expose
		private final boolean isCommunity;

		public PermissionDetails(Permission p, Space s, User u, User r, boolean c) {
			this.perm = p;
			this.space = s;
			this.user = u;
			this.requester = r;
			this.isCommunity = c;
		}
	}
}
