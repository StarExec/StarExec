package org.starexec.util;

import org.apache.commons.lang3.tuple.Triple;
import org.starexec.app.RESTHelpers;
import org.starexec.constants.R;
import org.starexec.constants.Web;
import org.starexec.data.database.*;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.to.*;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.logger.StarLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Contains helper methods for JSP pages.
 */
public class JspHelpers {
    private static final StarLogger log = StarLogger.getLogger( JspHelpers.class );

	private JspHelpers() {
		throw new UnsupportedOperationException("You may not create an instance of JspHelpers.");
	}

	public static void handleJobPage( HttpServletRequest request, HttpServletResponse response ) throws IOException, SQLException {
		String localJobPageParameter = request.getParameter(Web.LOCAL_JOB_PAGE_PARAMETER);
		boolean isLocalJobPage = (localJobPageParameter != null) && localJobPageParameter.equals("true");
		String jobUuid = request.getParameter( "anonId" );
		boolean isAnonymousPage = ( jobUuid != null );

		Integer jobId = null;
		Integer userId = null;
		PrimitivesToAnonymize primitivesToAnonymize = PrimitivesToAnonymize.NONE;

		// Set the user id and the job id.
		if ( isAnonymousPage ) {
			Optional<Integer> potentialJobId = AnonymousLinks.getIdOfJobAssociatedWithLink( jobUuid );
			Optional<PrimitivesToAnonymize> potentialPrimitivesToAnonymize = AnonymousLinks.getPrimitivesToAnonymizeForJob( jobUuid );
			if ( potentialJobId.isPresent() && potentialPrimitivesToAnonymize.isPresent() ) {
				jobId = potentialJobId.get();
				primitivesToAnonymize = potentialPrimitivesToAnonymize.get();
				Job tempJob = Jobs.get( jobId );
				// Give the anonymous user the ability to view the job as if they were the owning user.
				// This won't affect the user id in the session and therefore won't cause any security issues.
				userId = tempJob.getUserId();
			} else {
				response.sendError( HttpServletResponse.SC_NOT_FOUND, "Job does not exist." );
				return;
			}
		} else {
			jobId = Integer.parseInt(request.getParameter("id"));
			userId = SessionUtil.getUserId(request);
		}


		Job j=null;
		if(Permissions.canUserSeeJob( jobId, userId ).isSuccess()) {
			List<Processor> ListOfPostProcessors = Processors.getByUser( userId, ProcessorType.POST );
			j=Jobs.get(jobId);

			boolean queueExists = true;
			boolean queueIsEmpty = false;

			if (j.getQueue() == null) {
				queueExists = false;
			} else {
				Queue q = j.getQueue();
				List<WorkerNode> nodes = Cluster.getNodesForQueue(q.getId());
				if (nodes.isEmpty()) {
					queueIsEmpty = true;
				}

			}



			int jobSpaceId=j.getPrimarySpace();

			if (jobSpaceId>0) {
				j=Jobs.get(jobId);
				JobStatus status = Jobs.getJobStatus(jobId);
				boolean isPaused = (status == JobStatus.PAUSED);
				boolean isAdminPaused = Jobs.isSystemPaused();
				boolean isKilled = (status == JobStatus.KILLED);
				boolean isRunning = (status == JobStatus.RUNNING);
				boolean isProcessing = (status == JobStatus.PROCESSING);
				boolean isComplete = (status == JobStatus.COMPLETE);
				boolean isPublicUser = Users.isPublicUser(userId);
				boolean isUserSubscribedToJob = Notifications.isUserSubscribedToJob(userId, jobId);
				int wallclock=j.getWallclockTimeout();
				int cpu=j.getCpuTimeout();
				long memory=j.getMaxMemory();
				JobSpace jobSpace=Spaces.getJobSpace(jobSpaceId);

				User u=Users.get(j.getUserId());

				String jobSpaceTreeJson = RESTHelpers.getJobSpacesTreeJson( j.getId(), userId);
				List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob(jobSpaceId, true);
				jobSpaces.add(jobSpace);
				request.setAttribute("jobSpaces", jobSpaces);

				//TODO: This code isn't going to work for pipelines. It just always does stage 1.
				if (isLocalJobPage) {
					Map<Integer, String> jobSpaceIdToSubspaceJsonMap = RESTHelpers.getJobSpaceIdToSubspaceJsonMap(j.getId(), jobSpaces);
					request.setAttribute("jobSpaceIdToSubspaceJsonMap", jobSpaceIdToSubspaceJsonMap);

					Map<Integer, String> jobSpaceIdToCpuTimeSolverStatsJsonMapExcludeUnknowns =
							RESTHelpers.getJobSpaceIdToSolverStatsJsonMap(jobSpaces, 1, false, false);
					request.setAttribute("jobSpaceIdToCpuTimeSolverStatsJsonMapExcludeUnknowns", jobSpaceIdToCpuTimeSolverStatsJsonMapExcludeUnknowns);

					Map<Integer, String> jobSpaceIdToWallclockTimeSolverStatsJsonMapExcludeUnknowns =
							RESTHelpers.getJobSpaceIdToSolverStatsJsonMap(jobSpaces, 1, true,false);
					//used to creat the panels for subspace summaries
					request.setAttribute("jobSpaceIdToWallclockTimeSolverStatsJsonMapExcludeUnknowns", jobSpaceIdToWallclockTimeSolverStatsJsonMapExcludeUnknowns);
					Map<Integer, String> jobSpaceIdToCpuTimeSolverStatsJsonMapIncludeUnknowns =
							RESTHelpers.getJobSpaceIdToSolverStatsJsonMap(jobSpaces, 1, false, true);
					request.setAttribute("jobSpaceIdToCpuTimeSolverStatsJsonMapIncludeUnknowns", jobSpaceIdToCpuTimeSolverStatsJsonMapIncludeUnknowns);

					Map<Integer, String> jobSpaceIdToWallclockTimeSolverStatsJsonMapIncludeUnknowns =
							RESTHelpers.getJobSpaceIdToSolverStatsJsonMap(jobSpaces, 1, true, true);
					//used to creat the panels for subspace summaries
					request.setAttribute("jobSpaceIdToWallclockTimeSolverStatsJsonMapIncludeUnknowns", jobSpaceIdToWallclockTimeSolverStatsJsonMapIncludeUnknowns);
					Map<Integer, List<JobPair>> jobSpaceIdToPairMap = JobPairs.buildJobSpaceIdToJobPairMapWithWallCpuTimesRounded(j);
					request.setAttribute("jobSpaceIdToPairMap", jobSpaceIdToPairMap);
				}

				String primitivesToAnonymizeName = AnonymousLinks.getPrimitivesToAnonymizeName( primitivesToAnonymize );
				request.setAttribute( "primitivesToAnonymize", primitivesToAnonymizeName );
				request.setAttribute( "isAnonymousPage", isAnonymousPage );
				request.setAttribute("jobSpaceTreeJson", jobSpaceTreeJson);
				if (isAnonymousPage) {
					// For anonymous pages reset the userId for the permissions actually used on the page.
					userId = SessionUtil.getUserId( request );
				}
				request.setAttribute("isAdmin",Users.isAdmin(userId));
				request.setAttribute("usr",u);
				request.setAttribute("job", j);
				request.setAttribute("jobspace",jobSpace);
				request.setAttribute("isPaused", isPaused);
				request.setAttribute("isAdminPaused", isAdminPaused);
				request.setAttribute("isKilled", isKilled);
				request.setAttribute("isRunning", isRunning);
				request.setAttribute("isComplete", isComplete);
				request.setAttribute("queueIsEmpty", queueIsEmpty);
				request.setAttribute("isProcessing", isProcessing);
				request.setAttribute("isPublicUser", isPublicUser);
				request.setAttribute("isUserSubscribedToJob", isUserSubscribedToJob);
				request.setAttribute("postProcs", ListOfPostProcessors);
				request.setAttribute("pageTitle", isAnonymousPage ? "Anonymous Job" : j.getName() );
				request.setAttribute("initialSpaceName", isAnonymousPage ? "" : jobSpace.getName() );
				Processor stage1PostProc=j.getStageAttributesByStageNumber(1).getPostProcessor();
				Processor stage1PreProc=j.getStageAttributesByStageNumber(1).getPreProcessor();
				request.setAttribute("firstPostProc",stage1PostProc);
				request.setAttribute("firstPreProc",stage1PreProc);

				request.setAttribute("queues", Queues.getUserQueues(userId));
				request.setAttribute("queueExists", queueExists);
				request.setAttribute("userId",userId);
				request.setAttribute("cpu",cpu);
				request.setAttribute("wallclock",wallclock);
				request.setAttribute("maxMemory",Util.bytesToGigabytes(memory));
				request.setAttribute("seed",j.getSeed());
				request.setAttribute("diskUsage", Util.byteCountToDisplaySize(j.getDiskSize()));
				request.setAttribute("buildJob", j.isBuildJob());
				request.setAttribute("isHighPriority", j.isHighPriority());

				request.setAttribute("starexecUrl", R.STAREXEC_URL_PREFIX+"://"+R.STAREXEC_SERVERNAME+"/"+R.STAREXEC_APPNAME+"/");
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The details for this job could not be obtained");
			}

		} else {
			if (Jobs.isJobDeleted(jobId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
			}
		}
	}

	/**
	 * Handles request/response logic for details/solver.jsp for an anonymous page.
	 * @param uniqueId The UUID of the anonymous solver link.
	 * @author Albert Giegerich
	 */
	public static void handleAnonymousSolverPage( String uniqueId, HttpServletRequest request, HttpServletResponse response)
			throws IOException, SQLException {
		final String methodName = "handleAnonymousSolverPage";
		log.entry( methodName );
		try {
			Optional<Integer> solverId = AnonymousLinks.getIdOfSolverAssociatedWithLink( uniqueId );
			Optional<PrimitivesToAnonymize> primitivesToAnonymize = AnonymousLinks.getPrimitivesToAnonymizeForSolver( uniqueId );

			if ( primitivesToAnonymize.isPresent() && solverId.isPresent() ) {
				Solver solver = Solvers.get( solverId.get() );
				if ( solver == null ) {
					handleNullSolver( solverId.get(), response );
					return;
				}
				setSolverPageRequestAttributes( true, AnonymousLinks.areSolversAnonymized( primitivesToAnonymize.get() ), solver, request);
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found.");
			}
		} catch ( IOException e ) {
			log.error( methodName, "Caught an IOException while handling anonymous solver page: " + e.getMessage() );
			throw e;
		} catch ( SQLException e) {
			log.error( methodName, "Caught a SQLException while handling anonymous solver page: " + e.getMessage() );
			throw e;
		}
	}

	/**
	 * Handles request/response logic for details/solver.jsp for non-anonymous pages.
	 * @author Albert Giegerich
	 */
	public static void handleNonAnonymousSolverPage( HttpServletRequest request, HttpServletResponse response ) throws IOException {
		final String methodName = "handleNonAnonymousSolverPage";

		int userId = SessionUtil.getUserId(request);
		int solverId = Integer.parseInt(request.getParameter("id"));

		try {
			Solver s = null;
			if( Permissions.canUserSeeSolver( solverId, userId )) {
				s = Solvers.get(solverId);
				if ( s == null ) {
					handleNullSolver( solverId, response );
					return;
				}

				boolean downloadable = SolverSecurity.canUserDownloadSolver( s.getId(), userId ).isSuccess();
				request.setAttribute( "downloadable", downloadable );
				request.setAttribute( "hasAdminReadPrivileges", GeneralSecurity.hasAdminReadPrivileges( userId ));

				setSolverPageRequestAttributes( false, false, s, request);
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
			}
		} catch ( IOException e ) {
			log.error( methodName, "Caught an IOException while handling non-anonymous solver page: " + e.getMessage() );
			throw e;
		}
	}

	/**
	 * Helper method that sets the request attributes for details/job.jsp
	 * @author Albert Giegerich
	 */
	private static void setSolverPageRequestAttributes(
			boolean isAnonymousPage,
			boolean hideSolverName,
			Solver s,
			HttpServletRequest request) {

		if ( s == null ) {
			return;
		}

		List<Website> sites=Websites.getAll(s.getId() ,WebsiteType.SOLVER);
		//we need two versions of every website URL-- one for insertion into an attribute and
		//one for insertion into the HTML body. This data structure represents every site with 3 strings
		//first the name, then the attribute URL, then the body URL
		List<String[]> formattedSites= new ArrayList<>();
		for (Website site : sites) {
			String[] formattedSite=new String[3];
			formattedSite[0]=GeneralSecurity.getHTMLSafeString(site.getName());
			formattedSite[1]=GeneralSecurity.getHTMLAttributeSafeString(site.getUrl());
			formattedSite[2]=GeneralSecurity.getHTMLSafeString(site.getUrl());
			formattedSites.add(formattedSite);
		}
		SolverBuildStatus buildStatus = s.buildStatus();
		request.setAttribute("buildStatus", buildStatus.getStatus());
        request.setAttribute("sourceDownloadable", buildStatus.hasBeenBuiltOnStarexec());

		request.setAttribute( "solverPageTitle", hideSolverName ? "" : s.getName() );
		request.setAttribute("solver", s);
		request.setAttribute( "isAnonymousPage", isAnonymousPage );
		request.setAttribute( "hideSolverName", hideSolverName );
		request.setAttribute("sites", formattedSites);
		request.setAttribute("diskSize", Util.byteCountToDisplaySize(s.getDiskSize()));
		List<Configuration> configs = Solvers.getConfigsForSolver(s.getId());
		configs.sort(Comparator.comparingInt(Identifiable::getId));

		request.setAttribute("configs", configs );

		if ( !isAnonymousPage ) {
			request.setAttribute( "usr", Users.get( s.getUserId() ));
		}


	}

	/**
	 * Sends the appropriate response for a null solver.
	 * @author Albert Giegerich
	 */
	private static void handleNullSolver( int solverId, HttpServletResponse response ) throws IOException {
		if (Solvers.isSolverDeleted(solverId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "This solver has been deleted. You likely want to remove it from your spaces.");
		} else if (Solvers.isSolverRecycled(solverId))  {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "This solver has been moved to the trash bin by its owner.");
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
		}
	}

	/**
	 * Handles request/response logic for details/benchmark
	 * @author Albert Giegerich
	 */
	public static void handleAnonymousBenchPage( String uniqueId, HttpServletRequest request, HttpServletResponse response )
			throws IOException, SQLException {
		Optional<Integer> benchmarkId = AnonymousLinks.getIdOfBenchmarkAssociatedWithLink( uniqueId );
		Optional<PrimitivesToAnonymize> primitivesToAnonymize = AnonymousLinks.getPrimitivesToAnonymizeForBenchmark( uniqueId );

		if ( benchmarkId.isPresent() && primitivesToAnonymize.isPresent() ) {
			boolean hideBenchmark = AnonymousLinks.areBenchmarksAnonymized( primitivesToAnonymize.get() );
			setBenchmarkPageRequestAttributes( true, hideBenchmark, benchmarkId.get(), request, response );
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found.");
		}
	}

	public static void handleNonAnonymousBenchPage( HttpServletRequest request, HttpServletResponse response ) throws IOException {
		int benchId = Integer.parseInt(request.getParameter("id"));
		setBenchmarkPageRequestAttributes( false, false, benchId, request, response );
	}

	public static void handleAnonymousJobKeyPage( HttpServletRequest request, HttpServletResponse response ) throws IOException, SQLException {
		int userId = SessionUtil.getUserId( request );

		String jobUuid = request.getParameter( "anonId" );
		Optional<Integer> potentialJobId = AnonymousLinks.getIdOfJobAssociatedWithLink( jobUuid );

		if ( !potentialJobId.isPresent() ) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No job with given ID.");
			return;
		}

		int jobId = potentialJobId.get();

		if ( !JobSecurity.userOwnsJobOrIsAdmin( jobId, userId ) ) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not permitted to see this job's anonymous solver name key page.");
			return;
		}

		Job job = Jobs.get(jobId);
		List<Triple<String, String, Integer>> anonymousSolverNamesKey = AnonymousLinks.getAnonymousSolverNamesKey( jobId );
		request.setAttribute( "job", job );
		request.setAttribute( "solverTripleList", anonymousSolverNamesKey );
	}

	/**
	 * Sets up request attributes to be used on the jsp template.
	 * @author Albert Giegerich and Unknown
	 */
	private static void setBenchmarkPageRequestAttributes(
			boolean isAnonymousPage,
			boolean hideBenchmarkName,
			Integer benchId,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException {

		final String methodName = "setBenchmarkPageRequestAttributes";
		log.entry( methodName );

		// Set to true so anonymous user will be able to see the bench.
		boolean userCanSeeBench = true;

		// Set downloadable to true so anonymous user can view contents of benchmark.
		boolean downloadable = true;

		if ( !isAnonymousPage ) {
			int userId = SessionUtil.getUserId( request );
			userCanSeeBench = Permissions.canUserSeeBench( benchId, userId );
			request.setAttribute( "hasAdminReadPrivileges", GeneralSecurity.hasAdminReadPrivileges( userId ));
			downloadable = BenchmarkSecurity.canUserDownloadBenchmark( benchId,userId ).isSuccess();
			try {
				request.setAttribute("brokenBenchDeps", Benchmarks.getBrokenBenchDependencies(benchId));
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error occurred while checking for broken benchmark dependencies.");
				return;
			}
		}
		request.setAttribute( "downloadable", downloadable );

		// Send an error message if the user isn't allowed to see the benchmark.
		if ( !userCanSeeBench ) {
			response.sendError( HttpServletResponse.SC_NOT_FOUND, "You do not have permission to view this benchmark." );
			return;
		}

		// Get the benchmark, if it can't be gotten send an error message telling the user why.
		Benchmark b = Benchmarks.get( benchId, true, false );
		if ( b == null ) {
			sendErrorMessage( benchId, response );
			return;
		}

		StringBuilder js = new StringBuilder("common/delaySpinner, lib/jquery.dataTables.min, shared/copyToStardev, details/shared, lib/prettify, details/benchmark");
		StringBuilder lang = new StringBuilder();
		if (b.getType().getSyntax().js != null) {
			js.append(", ").append(b.getType().getSyntax().js);
			lang.append(b.getType().getSyntax().classname);
		}
		request.setAttribute("js", js.toString());
		request.setAttribute("lang", lang.toString());

		// Set the page title to be the name of the benchmark if we're showing the benchmark name.
		final String benchPageTitleAttributeName = "benchPageTitle";
		if ( hideBenchmarkName ) {
			request.setAttribute( benchPageTitleAttributeName, "" );
		} else {
			request.setAttribute( benchPageTitleAttributeName, b.getName() );
		}

		TreeMap<String,String> attrs = Benchmarks.getSortedAttributes(benchId);
		List<BenchmarkDependency> deps = Benchmarks.getBenchDependencies(benchId);
		request.setAttribute( "usr", Users.get( b.getUserId() ));
		request.setAttribute( "bench", b );
		request.setAttribute( "diskSize", Util.byteCountToDisplaySize( b.getDiskSize() ));

		Space s = Communities.getDetails( b.getType().getCommunityId() );
		if ( s == null ) {
			s = new Space();
			s.setName( "none" );
		}

		request.setAttribute( "com", s );
		request.setAttribute( "depends", deps );
		request.setAttribute( "attributes", attrs );

		try {
			Optional<String> benchmarkContents = Benchmarks.getContents(b, 100);
			if (!benchmarkContents.isPresent()) {
				request.setAttribute( "content", "benchmark contents not available" );
				return;
			}
			String content = GeneralSecurity.getHTMLSafeString(benchmarkContents.get());
			request.setAttribute( "content", content );
		} catch (IOException e) {
			log.warn(methodName, "Caught exception while trying to set benchmark contents.", e);
			request.setAttribute("content", "IO Error: Could not get benchmark contents.");
		}
	}

	private static void sendErrorMessage( int benchId, HttpServletResponse response ) throws IOException {
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
				"This benchmark has been deleted. You likely want to remove it from your spaces");
		} else if (Benchmarks.isBenchmarkRecycled(benchId))  {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "This benchmark has been moved to the trash bin by its owner.");
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark does not exist or is restricted");
		}
	}
}
