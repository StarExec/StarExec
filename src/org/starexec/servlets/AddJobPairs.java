package org.starexec.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.starexec.app.RESTServices;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Configuration;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
public class AddJobPairs extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(AddJobPairs.class);	
	private final String jobIdParam = "jobId";
	private final String configsParam = "configs";
	private final String addToAllParam = "addToAll";
	private final String addToPairedParam = "addToPaired";
	JsonParser parser = new JsonParser();

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final String methodName = "doPost";
		log.entry( methodName );

		try {
			// Validate the request.
			log.debug( methodName, "Validating the request.");
			ValidatorStatusCode validationStatus = validateRequest( request );
			if ( !validationStatus.isSuccess() ) {
				response.sendError( HttpServletResponse.SC_BAD_REQUEST, validationStatus.getMessage() );
				return;
			}
			final int userId = SessionUtil.getUserId( request );

			RESTServices services = new RESTServices();
			JsonObject o = parser.parse(services.getNumberOfPairsToBeAddedAndDeleted(request)).getAsJsonObject();
			int pairsAdded = o.get("pairsToBeAdded").getAsInt();
			int pairsDeleted = o.get("pairsToBeDeleted").getAsInt();
			int remainingQuota = o.get("remainingQuota").getAsInt();
			int netPairs = pairsAdded - pairsDeleted;
			if (pairsAdded>0 && (remainingQuota+netPairs)<0) {
				response.sendError( HttpServletResponse.SC_BAD_REQUEST, "You do not have sufficient job pair quota to add these pairs" );
				return;
			}
			if (pairsAdded>0 && Users.isDiskQuotaExceeded(userId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Your disk quota has been exceeded: please clear out some old solvers, jobs, or benchmarks before proceeding");
				return;
			}

			log.debug( methodName, "Getting the job id from the request parameter.");
			final int jobId = Integer.parseInt( request.getParameter(jobIdParam) );
			log.debug( methodName, "\tjobId = "+jobId);

			// Make sure the job is paused or completed before adding new job pairs.
			if ( !( Jobs.isJobPaused( jobId )  || Jobs.isJobComplete( jobId ) ) ) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Job must be finished or paused to add job pairs.");
				return;
			}

			log.debug( methodName, "\tuserid = "+userId );

			// Make sure the user has permission to add job pairs to this job.
			ValidatorStatusCode securityStatus = JobSecurity.canUserAddJobPairs( jobId, userId ); 
			if ( !securityStatus.isSuccess() ) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, securityStatus.getMessage());
				return;
			}
			Set<Integer> solverIdsToAddToAll = new HashSet<>( Util.toIntegerList( request.getParameterValues( addToAllParam ) ) );
			Set<Integer> solverIdsToAddToPaired = new HashSet<>( Util.toIntegerList( request.getParameterValues( addToPairedParam ) ) );

			log.debug( methodName, "Solver Ids To Add To All: ");
			for ( Integer sid : solverIdsToAddToAll ) {
				log.debug( methodName, "\t"+sid );
			}
			log.debug( methodName, "Solver Ids To Add To Paired: ");
			for ( Integer sid : solverIdsToAddToPaired ) {
				log.debug( methodName, "\t"+sid );
			}

			Set<Integer> selectedConfigIds = new HashSet<>( Util.toIntegerList( request.getParameterValues( configsParam ) ) );
			Set<Integer> allConfigIdsInJob = Solvers.getConfigIdSetByJob( jobId );

			Set<Integer> configIdsToDelete = new HashSet<>( allConfigIdsInJob );
			configIdsToDelete.removeAll( selectedConfigIds );
			log.debug( methodName, "Config ID's to be deleted: " );
			for ( Integer cid : configIdsToDelete ) {
				log.debug( methodName, "\t"+cid );
			}
			Jobs.deleteJobPairsWithConfigurationsFromJob( jobId, configIdsToDelete );

			// Filter the selected config ids into two sets, one that contains config ids that we want to pair with every benchmark, and one
			// that contains config ids that we want to pair with every benchmark already paired with the config's solver in the job.
			Set<Integer> configIdsToAddToAll = new HashSet<>();
			Set<Integer> configIdsToAddToPaired = new HashSet<>();
			for ( Integer configId : selectedConfigIds ) {
				Configuration config = Solvers.getConfiguration( configId );	
				if ( solverIdsToAddToAll.contains( config.getSolverId() ) ) {
					configIdsToAddToAll.add( configId );
				}  else if ( solverIdsToAddToPaired.contains( config.getSolverId() ) ) {
					configIdsToAddToPaired.add( configId );
				}
			}
			log.debug( methodName, "Config Ids To Add To All: ");
			for ( Integer cid : configIdsToAddToAll ) {
				log.debug( methodName, "\t"+cid );
			}
			log.debug( methodName, "Config Ids To Add To Paired: ");
			for ( Integer cid : configIdsToAddToPaired ) {
				log.debug( methodName, "\t"+cid );
			}

			configIdsToAddToPaired.removeAll( allConfigIdsInJob );

			log.debug( methodName, "Adding job pairs for paired benchmarks." );
			Jobs.addJobPairsFromConfigIdsForPairedBenchmarks( jobId, configIdsToAddToPaired );
			log.debug( methodName, "Adding job pairs for all benchmarks." );
			Jobs.addJobPairsFromConfigIdsForAllBenchmarks( jobId, configIdsToAddToAll );

			response.sendRedirect( Util.docRoot( "secure/details/job.jsp?id=" + jobId ) );
			return;
		} catch (Exception e) {
			log.warn(methodName, "Caught exception while doing post for AddJobPairs: " + e.getMessage(), e);
			response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Util.getStackTrace( e ) );
			return;
		}
	}

	private ValidatorStatusCode validateRequest( HttpServletRequest request ) {
		if ( !Util.paramExists( jobIdParam, request ) ) {
			return new ValidatorStatusCode( false, "Could not find a job ID parameter in request." );
		} else {
			return new ValidatorStatusCode( true );
		}
	}
}
