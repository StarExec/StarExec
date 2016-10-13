package org.starexec.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

import org.starexec.app.RESTHelpers;
import org.starexec.constants.R;
import org.starexec.constants.Web;
import org.starexec.data.database.AnonymousLinks;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Communities;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;

import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.ValidatorStatusCode;

import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobSpace;
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.JobStatus.JobStatusCode;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Space;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverStats;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.data.to.WorkerNode;
import org.starexec.data.to.SolverBuildStatus;

/**
 * Contains helper methods for JSP pages.
 */
public class JspHelpers {
    private static final Logger log = Logger.getLogger( JspHelpers.class );
	private static final LogUtil logUtil = new LogUtil( log );

	private JspHelpers() {
		throw new UnsupportedOperationException("You may not create an instance of JspHelpers."); 
	}

	public static void handleAddJobPairsPage(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
		int jobId = Integer.parseInt( request.getParameter("jobId") ); 
		final int userId = SessionUtil.getUserId( request );
		ValidatorStatusCode securityStatus = JobSecurity.canUserAddJobPairs( jobId, userId ); 
		if ( !securityStatus.isSuccess() ) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, securityStatus.getMessage());
			return;
		}

		if ( !( Jobs.isJobPaused( jobId )  || Jobs.isJobComplete( jobId ) ) ) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Job must be finished or paused to add job pairs.");
			return;
		}


		Comparator<Solver> compareById = (solver1, solver2) -> solver1.getId() - solver2.getId();

		List<Solver> solvers = Solvers.getByJobSimpleWithConfigs( jobId );
		Collections.sort( solvers,  compareById );


		// Get all solvers accessible to the user. Filter out items already in the "solvers" variable.
		List<Solver> usersSolvers = Solvers.getByUserWithConfigs( userId ).stream()
				.filter( item -> Collections.binarySearch( solvers, item, compareById ) < 0 ) 
				.collect( Collectors.toList() );


		Set<Integer> configIdSet = Solvers.getConfigIdSetByJob( jobId );	
        Solvers.sortConfigs(solvers);
        Solvers.sortConfigs(usersSolvers);
		Solvers.makeDefaultConfigsFirst( solvers );
		Solvers.makeDefaultConfigsFirst( usersSolvers );

		request.setAttribute("solvers", solvers);
		request.setAttribute("usersSolvers", usersSolvers);
		request.setAttribute("configIdSet", configIdSet);
		request.setAttribute("jobId", jobId);
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
		if(Permissions.canUserSeeJob( jobId, userId )) {
			List<Processor> ListOfPostProcessors = Processors.getByUser( userId, ProcessorType.POST );
			j=Jobs.get(jobId);
			
			boolean queueExists = true;
			boolean queueIsEmpty = false;

			if (j.getQueue() == null) {
				queueExists = false;
			} else {
				Queue q = j.getQueue();
				List<WorkerNode> nodes = Cluster.getNodesForQueue(q.getId());
				if (nodes.size() == 0) {
					queueIsEmpty = true;
				}

			}

			
			
			int jobSpaceId=j.getPrimarySpace();
			
			if (jobSpaceId>0) {
				j=Jobs.get(jobId);
				JobStatus status=Jobs.getJobStatusCode(jobId);
				boolean isPaused = (status.getCode() == JobStatusCode.STATUS_PAUSED);
				boolean isAdminPaused = Jobs.isSystemPaused();
				boolean isKilled = (status.getCode() == JobStatusCode.STATUS_KILLED);
				boolean isRunning = (status.getCode() == JobStatusCode.STATUS_RUNNING);
				boolean isProcessing = (status.getCode() == JobStatusCode.STATUS_PROCESSING);
				boolean isComplete = (status.getCode() == JobStatusCode.STATUS_COMPLETE);
				int wallclock=j.getWallclockTimeout();
				int cpu=j.getCpuTimeout();
				long memory=j.getMaxMemory();
				JobSpace jobSpace=Spaces.getJobSpace(jobSpaceId);

				User u=Users.get(j.getUserId());

				String jobSpaceTreeJson = RESTHelpers.getJobSpacesTreeJson(jobSpaceId, j.getId(), userId);
				List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob(jobSpaceId, true);
				jobSpaces.add(jobSpace);
				request.setAttribute("jobSpaces", jobSpaces);
				
				//TODO: This code isn't going to work for pipelines. It just always does stage 1.
				if (isLocalJobPage) {
					Map<Integer, String> jobSpaceIdToSubspaceJsonMap = RESTHelpers.getJobSpaceIdToSubspaceJsonMap(j.getId(), jobSpaces);
					request.setAttribute("jobSpaceIdToSubspaceJsonMap", jobSpaceIdToSubspaceJsonMap);
					
					Map<Integer, String> jobSpaceIdToCpuTimeSolverStatsJsonMap = 
							RESTHelpers.getJobSpaceIdToSolverStatsJsonMap(jobSpaces, 1, false);
					request.setAttribute("jobSpaceIdToCpuTimeSolverStatsJsonMap", jobSpaceIdToCpuTimeSolverStatsJsonMap);
					
					Map<Integer, String> jobSpaceIdToWallclockTimeSolverStatsJsonMap = 
							RESTHelpers.getJobSpaceIdToSolverStatsJsonMap(jobSpaces, 1, true);
					request.setAttribute("jobSpaceIdToWallclockTimeSolverStatsJsonMap", jobSpaceIdToWallclockTimeSolverStatsJsonMap);
					Map<Integer, List<JobPair>> jobSpaceIdToPairMap = JobPairs.buildJobSpaceIdToJobPairMapWithWallCpuTimesRounded(j);
					request.setAttribute("jobSpaceIdToPairMap", jobSpaceIdToPairMap);
					Map<Integer, List<SolverStats>> jobSpaceIdToSolverStatsMap = 
							Jobs.buildJobSpaceIdToSolverStatsMapWallCpuTimesRounded(j, 1);
					request.setAttribute("jobSpaceIdToSolverStatsMap", jobSpaceIdToSolverStatsMap);

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
		logUtil.entry( methodName );
		try {
			Optional<Integer> solverId = AnonymousLinks.getIdOfSolverAssociatedWithLink( uniqueId );	
			Optional<PrimitivesToAnonymize> primitivesToAnonymize = AnonymousLinks.getPrimitivesToAnonymizeForSolver( uniqueId );

			if ( primitivesToAnonymize.isPresent() && solverId.isPresent() ) {
				Solver solver = Solvers.get( solverId.get() );
				if ( solver == null ) {
					handleNullSolver( solverId.get(), response );
					return;
				}
				setSolverPageRequestAttributes( true, AnonymousLinks.areSolversAnonymized( primitivesToAnonymize.get() ), solver, request, response ); 
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found.");	
				return;
			}
		} catch ( IOException e ) {
			logUtil.error( methodName, "Caught an IOException while handling anonymous solver page: " + e.getMessage() );
			throw e;
		} catch ( SQLException e) {
			logUtil.error( methodName, "Caught a SQLException while handling anonymous solver page: " + e.getMessage() );
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

				setSolverPageRequestAttributes( false, false, s, request, response );
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
			}
		} catch ( IOException e ) {
			logUtil.error( methodName, "Caught an IOException while handling non-anonymous solver page: " + e.getMessage() );
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
			HttpServletRequest request, 
			HttpServletResponse response ) throws IOException {

		if ( s == null ) {
			return;
		}

		List<Website> sites=Websites.getAll(s.getId() ,WebsiteType.SOLVER);
		//we need two versions of every website URL-- one for insertion into an attribute and
		//one for insertion into the HTML body. This data structure represents every site with 3 strings
		//first the name, then the attribute URL, then the body URL
		List<String[]> formattedSites=new ArrayList<String[]>();
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
		Collections.sort( configs, ( Configuration c1, Configuration c2 ) -> c1.getId() - c2.getId() );

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
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "This solver has been moved to the recycle bin by its owner.");
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
		logUtil.entry( methodName );

		// Set to true so anonymous user will be able to see the bench.
		boolean userCanSeeBench = true;

		// Set downloadable to true so anonymous user can view contents of benchmark.
		boolean downloadable = true;

		if ( !isAnonymousPage ) {
			int userId = SessionUtil.getUserId( request );
			userCanSeeBench = Permissions.canUserSeeBench( benchId, userId );
			request.setAttribute( "hasAdminReadPrivileges", GeneralSecurity.hasAdminReadPrivileges( userId ));
			downloadable = BenchmarkSecurity.canUserDownloadBenchmark( benchId,userId ).isSuccess();
            request.setAttribute("brokenBenchDeps", Benchmarks.getBrokenBenchDependencies(benchId));
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
			logUtil.warn(methodName, "Caught exception while trying to set benchmark contents.");
			request.setAttribute("content", "IO Error: Could not get benchmark contents.");
		}
	}

	private static void sendErrorMessage( int benchId, HttpServletResponse response ) throws IOException {
		if (Benchmarks.isBenchmarkDeleted(benchId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, 
				"This benchmark has been deleted. You likely want to remove it from your spaces");
		} else if (Benchmarks.isBenchmarkRecycled(benchId))  {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "This benchmark has been moved to the recycle bin by its owner.");
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark does not exist or is restricted");	
		}
	}
}
