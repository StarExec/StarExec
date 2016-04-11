package org.starexec.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import java.sql.SQLException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
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
import org.starexec.data.database.Requests;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Statistics;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.security.BenchmarkSecurity;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.SettingSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.security.WebsiteSecurity;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.SpaceSecurity;
import org.starexec.data.security.UploadSecurity;
import org.starexec.data.security.UserSecurity;
import org.starexec.data.to.*;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.exceptions.StarExecException;
import org.starexec.exceptions.StarExecSecurityException;
import org.starexec.jobs.JobManager;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.test.integration.TestManager;
import org.starexec.test.integration.TestResult;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.LoggingManager;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.LogUtil;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

/**
 * Class which handles all RESTful web service requests.
 */
@Path("")
public class RESTServices {	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	private static final LogUtil logUtil = new LogUtil(log);
	private static Gson gson = new Gson();
	private static Gson limitGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	
	protected static final ValidatorStatusCode ERROR_DATABASE=new ValidatorStatusCode(false, "There was an internal database error processing your request");
	private static final ValidatorStatusCode ERROR_INTERNAL_SERVER=new ValidatorStatusCode(false, "There was an internal server error processing your request");
	private static final ValidatorStatusCode ERROR_INVALID_WEBSITE_TYPE=new ValidatorStatusCode(false, "The supplied website type was invalid");
	private static final ValidatorStatusCode ERROR_EDIT_VAL_ABSENT=new ValidatorStatusCode(false, "No value specified");
	private static final ValidatorStatusCode ERROR_IDS_NOT_GIVEN=new ValidatorStatusCode(false, "No ids specified");
	
	private static final ValidatorStatusCode ERROR_INVALID_PERMISSIONS=new ValidatorStatusCode(false, "You do not have permission to perform the requested operation");
	
	private static final ValidatorStatusCode ERROR_INVALID_PARAMS=new ValidatorStatusCode(false, "The supplied parameters are invalid");
	private static final ValidatorStatusCode ERROR_CANT_PROMOTE_SELF=new ValidatorStatusCode(false, "You cannot promote yourself");
	private static final ValidatorStatusCode ERROR_CANT_PROMOTE_LEADER=new ValidatorStatusCode(false, "The user is already a leader");
		
	protected static final ValidatorStatusCode ERROR_TOO_MANY_JOB_PAIRS=new ValidatorStatusCode(false, "There are too many job pairs to display",1);
	protected static final ValidatorStatusCode  ERROR_TOO_MANY_SOLVER_CONFIG_PAIRS=new ValidatorStatusCode(false, "There are too many solver / configuraiton pairs to display");
	
	
	/**
	 * Recompiles all the job spaces for the given job
	 * @param jobId ID of the job to recompile
	 * @param request
	 * @return ValidatorStatusCode with true on success and false otherwise
	 */
	@GET
	@Path("/recompile/{jobid}")
	@Produces("application/json")	
	public String recompileJobSpaces(@PathParam("jobid") int jobId, @Context HttpServletRequest request) {					
		int userId = SessionUtil.getUserId(request);
		
		ValidatorStatusCode status=JobSecurity.canUserRecompileJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		boolean success=Jobs.recompileJobSpaces(jobId);
		return success ? gson.toJson(new ValidatorStatusCode(true, "recompilation successful")) :  gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * @param parentId the ID of root job space
	 * @param jobId The ID  of the job
	 * @param makeSpaceTree ???
	 * @param request
	 * @return a json string representing all the subspaces of the job space
	 * with the given id
	 * @author Eric Burns
	 */
	@GET
	@Path("/space/{jobid}/jobspaces/{spaceTree}")
	@Produces("application/json")	
	public String getJobSpaces(@QueryParam("id") int parentId,@PathParam("jobid") int jobId, @PathParam("spaceTree") boolean makeSpaceTree, @Context HttpServletRequest request) {					
		int userId = SessionUtil.getUserId(request);
		return RESTHelpers.validateAndGetJobSpacesJson(parentId, jobId, makeSpaceTree, userId);
	}

	/**
	 * @param parentId Id of the job space to get children for
	 * @param anonymousLinkUuid Unique ID assigned to anonymous page
	 * @param primitivesToAnonymizeName String representing which primitive types to anonymize
	 * @param makeSpaceTree ???
	 * @param request
	 * @return a json string representing all the subspaces of the job space
	 * with the given id
	 * @author Eric Burns
	 */
	@GET
	@Path("/space/anonymousLink/{anonymousLinkUuid}/jobspaces/{spaceTree}/{primitivesToAnonymizeName}")
	@Produces("application/json")	
	public String getJobSpaces(
			@QueryParam("id") int parentId,
			@PathParam("anonymousLinkUuid") String anonymousLinkUuid,
			@PathParam("spaceTree") boolean makeSpaceTree, 
			@PathParam("primitivesToAnonymizeName") String primitivesToAnonymizeName,
			@Context HttpServletRequest request) {					
		final String methodName = "getJobSpaces";
		try {
			logUtil.entry( methodName );
			Optional<Integer> potentialJobId = Optional.empty();
			try {
				potentialJobId = AnonymousLinks.getIdOfJobAssociatedWithLink( anonymousLinkUuid );
			} catch ( SQLException e ) {
				logUtil.error( methodName, "Caught an SQLException while trying to retrieve a job id from the anonymous links table in the database." );
				return gson.toJson( ERROR_DATABASE );
			}
			 
			if ( potentialJobId.isPresent() ) {
				if (!JobSecurity.isAnonymousLinkAssociatedWithJob(anonymousLinkUuid, potentialJobId.get()).isSuccess()) {
					return gson.toJson(new ValidatorStatusCode(false, "The given anonymous link is not linked to the given job"));
				}
				PrimitivesToAnonymize primitivesToAnonymize = AnonymousLinks.createPrimitivesToAnonymize( primitivesToAnonymizeName );
				return RESTHelpers.getJobSpacesJson(parentId, potentialJobId.get(), makeSpaceTree, primitivesToAnonymize);
			} else {
				ValidatorStatusCode status = new ValidatorStatusCode( false, "Job does not exist." );
				return gson.toJson( status );
			}
		} catch (RuntimeException e) {
			// Catch all runtime exceptions so we can debug them
			logUtil.error( methodName, "Caught a runtime exception: " + Util.getStackTrace(e) ); 
			throw e;
		}
	}

	/**
	 * Determines whether the given space is a leaf space
	 * @param spaceId The ID of the space to check
	 * @return json boolean object
	 */
	@GET
	@Path("/space/isLeaf/{spaceId}")
	@Produces("application/json")	
	public String isLeafSpace(@PathParam("spaceId") int spaceId) {					
		final String method = "isLeafSpace";
		log.debug(method+" - Entering method "+method);
		log.debug(method+" - Attempting to determine if space with id="+spaceId+" is a leaf space.");
		return gson.toJson(Spaces.isLeaf(spaceId));
	}

	/**
	 * Retrieves a text description of a benchmark upload status object.
	 * @param statusId The ID of the status object
	 * @param request
	 * @return a json ValidatorStatusCode with the description as the message on success
	 */
	@GET
	@Path("/benchmarks/uploadDescription/{statusId}")
	@Produces("application/json")
	public String getBenchmarkUploadDescription(@PathParam("statusId") int statusId, @Context HttpServletRequest request) {
		int userId =SessionUtil.getUserId(request);
		if (!BenchmarkSecurity.canUserSeeBenchmarkStatus(statusId, userId)) {
			return gson.toJson(new ValidatorStatusCode(false, "You do not have permission to view this upload"));
		}
		return gson.toJson(new ValidatorStatusCode(true,Uploads.getUploadStatusSummary(statusId)));
	}
	
	
	
	/**
	 * @param parentId The ID of the space to get the children of
	 * @param request
	 * @return a json string representing all the subspaces of the space with
	 * the given id. If the given id is <= 0, then the root space is returned
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/space/subspaces")
	@Produces("application/json")
	public String getSubSpaces(@QueryParam("id") int parentId, @Context HttpServletRequest request) {					
		int userId = SessionUtil.getUserId(request);
		log.debug("parentId = " + parentId);
		log.debug("userId = " + userId);
		
		return gson.toJson(RESTHelpers.toSpaceTree(Spaces.getSubSpaces(parentId, userId),userId));
	}
	
	
	
	/**
	 * @return a json string representing all communities within starexec
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/communities/all")
	@Produces("application/json")	
	public String getAllCommunities() {								
		return gson.toJson(RESTHelpers.toCommunityList(Communities.getAll()));
	}	
	
	
	/**
	 * Clears the error state E (which is generally caused by runscript errors) off of every node in the cluster
	 * @param request
	 * @return  json ValidatorStatusCode
	 */
	@POST
	@Path("/cluster/clearerrors")
	@Produces("application/json")	
	public String clearErrorStates(@Context HttpServletRequest request) {	
		final String method = "clearErrorStates";
		log.debug("Entering method "+method);
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserClearErrorStates(userId);
		if (!status.isSuccess()) {
			log.debug("("+method+") user cannot clear error states.");
			return gson.toJson(status);
		}
		
		return R.BACKEND.clearNodeErrorStates() ? gson.toJson(new ValidatorStatusCode(true)) : gson.toJson(new ValidatorStatusCode(false, "Internal error handling request"));
	}
	
	/**
	 * @param id The ID of the queue to get nodes for. If <=0, gets a list of all queues.
	 * @param request
	 * @return a json string representing all queues in the starexec cluster OR all nodes in a queue
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/queues")
	@Produces("application/json")	
	public String getAllQueues(@QueryParam("id") int id, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		if(id <= 0 && GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(RESTHelpers.toQueueList(Queues.getAllAdmin()));
		} else if (id <= 0) {
			return gson.toJson(RESTHelpers.toQueueList(Queues.getAllActive()));
		} else {
			return gson.toJson(RESTHelpers.toNodeList(Queues.getNodes(id)));
		}
	}
	
	/**
	 * @param request
	 * @return a text string that holds the result of running qstat -f
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/qstat")
	@Produces("text/plain")		
	public String getQstatOutput(@Context HttpServletRequest request) {		
		String qstat=R.BACKEND.getRunningJobsStatus();
		if(!Util.isNullOrEmpty(qstat)) {
			return qstat;
		}

		return "not available";
	}
	
	
	/**
	 * @param id The ID of the unvalidated benchmark object (from the unvalidated benchmarks table)
	 * @param request
	 * @return a text string that holds the result of running qstat -f
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/uploads/stdout/{id}")
	@Produces("text/plain")		
	public String getInvalidUploadedBenchmarkOutput(@PathParam("id") int id, @Context HttpServletRequest request) {		
		ValidatorStatusCode valid = UploadSecurity.canViewUnvalidatedBenchmarkOutput(SessionUtil.getUserId(request), id);
		if (!valid.isSuccess()) {
			return valid.getMessage();
		}
		String stdout=Uploads.getInvalidBenchmarkErrorMessage(id);
		if(!Util.isNullOrEmpty(stdout)) {
			return stdout;
		}

		return "not available";
	}
	
	/**
	 * @return a text string that shows the load values for the given queue.
	 * @param queueId the ID of the queue to get data for.
	 * @param request
	 * @author Eric Burns
	 */
	@GET
	@Path("/cluster/loads/{queueId}")
	@Produces("text/plain")		
	public String getLoadsForQueue(@PathParam("queueId") int queueId, @Context HttpServletRequest request) {		
		String loads = JobManager.getLoadRepresentationForQueue(queueId);
		if(!Util.isNullOrEmpty(loads)) {
			return loads;
		}

		return "not available";
	}
	
	/**
	 * @param id the ID of the job pair
	 * @param request
	 * @return a text string that holds the log of job pair with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/jobs/pairs/{id}/log")
	@Produces("text/plain")		
	public String getJobPairLog(@PathParam("id") int id, @Context HttpServletRequest request) {		
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserSeeJobWithPair(id, userId);
		if (!status.isSuccess()) {
		    return ("user "+ new Integer(userId) + " does not have access to see job " + new Integer(id));
		}
					
			String log = JobPairs.getJobLog(id);
				
			if(!Util.isNullOrEmpty(log)) {
				return log;
			}
		return "not available";
	}
	
	/**
	 * @param id The ID of the benchmark
	 * @param limit The maximum number of characters to return
	 * @param request
	 * @return a string that is the plain text contents of a benchmark file
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/benchmarks/{id}/contents")
	@Produces("text/plain")	
	public String getBenchmarkContent(@PathParam("id") int id, @QueryParam("limit") int limit, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		
		if (BenchmarkSecurity.canUserSeeBenchmarkContents(id,userId).isSuccess()) {
			Benchmark b=Benchmarks.get(id);
			String contents = Benchmarks.getContents(b, limit);
			if(!Util.isNullOrEmpty(contents)) {
				return contents;
			}	
		}

		return "not available";
	}
	
	/**
	 * @param id the ID of the solver to get build output for
	 * @param request
	 * @return a string that holds the build log for a solver
	 * @author Eric Burns
	 */
	@GET
	@Path("/solvers/{id}/buildoutput")
	@Produces("text/plain")	
	public String getSolverBuildLog(@PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
	
		ValidatorStatusCode status=SolverSecurity.canUserSeeBuildLog(id, userId);
		if (!status.isSuccess()) {
			return "not available";
		}
		try {
			File output=Solvers.getSolverBuildOutput(id);
			if(output.exists()) {			
				return FileUtils.readFileToString(output);			
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		
		return "not available";
	}

	/**
	 * Reruns all the pairs in the given job that have the given status code
	 * @param id The ID of the job to rerun pairs for
	 * @param statusCode The status code that all the pairs to be rerun have currently
	 * @param request
	 * @return 0 on success or an error code on failure
	 */
	@POST
	@Path("/jobs/rerunpairs/{id}/{status}")
	@Produces("application/json")	
	public String rerunJobPairs(@PathParam("id") int id, @PathParam("status") int statusCode, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserRerunPairs(id, userId,statusCode);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Jobs.setPairsToPending(id, statusCode) ? gson.toJson(new ValidatorStatusCode(true,"Rerunning of pairs began successfully")) : gson.toJson(ERROR_DATABASE);

	}
	
	/**
	 * Reruns all the pairs in the given job 
	 * @param id The ID of the job to rerun pairs for
	 * @param request
	 * @return 0 on success or an error code on failure
	 */
	@POST
	@Path("/jobs/rerunallpairs/{id}")
	@Produces("application/json")	
	public String rerunAllJobPairs(@PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserRerunAllPairs(id, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		return Jobs.setAllPairsToPending(id) ? gson.toJson(new ValidatorStatusCode(true,"Rerunning of pairs began successfully")) : gson.toJson(ERROR_DATABASE);

	}
	
	
	
	/**
	 * Reruns all the pairs in the given job that have 0 as their runtime
	 * @param id The ID of the job to rerun pairs for
	 * @param request
	 * @return 0 on success or an error code on failure
	 */
	@POST
	@Path("/jobs/rerunpairs/{id}")
	@Produces("application/json")	
	public String rerunTimelessJobPairs(@PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserRerunPairs(id, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		return Jobs.setTimelessPairsToPending(id) ? gson.toJson(new ValidatorStatusCode(true,"Rerunning of pairs began successfully")) : gson.toJson(ERROR_DATABASE);

	}
	
	/**
	 * @param id The ID of the pair to get output for
	 * @param stageNumber the stage to get output for
	 * @param limit The maximum number of characters to return
	 * @param request
	 * @return a string that holds the stdout of job pair with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/jobs/pairs/{id}/stdout/{stageNumber}")
	@Produces("text/plain")	
	public String getJobPairStdout(@PathParam("id") int id,@PathParam("stageNumber") int stageNumber, @QueryParam("limit") int limit, @Context HttpServletRequest request) {
		JobPair jp = JobPairs.getPair(id);
		if (jp==null) {
			return "not available";
		}
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jp.getJobId(), userId);
		if (!status.isSuccess()) {
			return "not available";
		}		
		String stdout = JobPairs.getStdOut(jp.getId(),stageNumber, limit);
		if(!Util.isNullOrEmpty(stdout)) {
			return stdout;
		}				
		
		return "not available";
	}
	
	/**
	 * @param id The ID of the node to get details for
	 * @param request
	 * @return a string representing all attributes of the node with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/nodes/details/{id}")
	@Produces("application/json")	
	public String getNodeDetails(@PathParam("id") int id, @Context HttpServletRequest request) {	
		return gson.toJson(Cluster.getNodeDetails(id));
	}

	
	/**
	 * @param id Th eID of the queue to get details for
	 * @param request
	 * @return a json string representing all attributes of the queue with the given id
	 * @author Tyler Jensen
	 */

	@GET
	@Path("/cluster/queues/details/{id}")
	@Produces("application/json")	
	public String getQueueDetails(@PathParam("id") int id, @Context HttpServletRequest request) {
		log.debug("getting queue details");
		return gson.toJson(Queues.get(id));
	}

	/**
	 * 
	 * @param queueId ID of the queue to count nodes for
	 * @param request
	 * @return gson integer representing the number of nodes in the given queue
	 */
	@GET
	@Path("/cluster/queues/details/nodeCount/{queueId}")
	@Produces("application/json")
	public String getNumberOfNodesInQueue(@PathParam("queueId") int queueId, @Context HttpServletRequest request) {
		int numberOfNodes = Queues.getNodes(queueId).size();
		return gson.toJson(numberOfNodes);
	}
	
	/**
	 * @param id community ID
	 * @param request
	 * @return a json string representing all communities within starexec
	 * @author Tyler Jensen
	 */

	@GET
	@Path("/communities/details/{id}")
	@Produces("application/json")	
	public String getCommunityDetails(@PathParam("id") int id, @Context HttpServletRequest request) {
		Space community = Communities.getDetails(id);
		int userId=SessionUtil.getUserId(request);
		if(community != null) {
			community.setUsers(Spaces.getUsers(id));
			Permission p = SessionUtil.getPermission(request, id);
			List<User> leaders = Spaces.getLeaders(id);
			List<Website> sites = Websites.getAllForJavascript(id, WebsiteType.SPACE);
			
			return gson.toJson(new RESTHelpers.CommunityDetails(community, p, leaders, sites,Users.isMemberOfCommunity(userId, id)));
		}
		
		return gson.toJson(RESTHelpers.toCommunityList(Communities.getAll()));
	}	

	/**
	 * 
	 * @param spaceId
	 * @param request
	 * @return gson integer representing the ID of the community that owns the given space
	 */
	@GET
	@Path("/space/community/{spaceId}")
	@Produces("application/json")
	public String getCommunityIdOfSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		return gson.toJson(Spaces.getCommunityOfSpace(spaceId));	
	}

	/**
	 * 
	 * @param userid ID of the user to get permissions for
	 * @param spaceId ID of the space to get permissions in
	 * @param request
	 * @return a json string representing permissions within a particular space for a user
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/permissions/details/{id}/{spaceId}")
	@Produces("application/json")	
	public String getPermissionDetails(@PathParam("id") int userid, @PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		User requester = SessionUtil.getUser(request);
		Permission perm = Permissions.get(userid, spaceId);
		Space space = Spaces.get(spaceId);
		Integer parentId = Spaces.getParentSpace(spaceId);
		boolean isCommunity = parentId==1;
		User user = Users.get(userid);
		
		return gson.toJson(new RESTHelpers.PermissionDetails(perm,space,user,requester, isCommunity));
		
	}
	
	/**
	 * @param spaceId ID of the space to get. If the given id is <= 0, then the root space is returned
	 * @param request
	 * @return a json string representing all the subspaces of the space with
	 * the given id. 
	 * @author Tyler Jensen & Todd Elvers
	 */

	@POST
	@Path("/space/{id}")
	@Produces("application/json")	
	public String getSpaceDetails(@PathParam("id") int spaceId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		
		Space s = null;
		Permission p = null;
		if(SpaceSecurity.canUserSeeSpace(spaceId, userId).isSuccess()) {
			s = Spaces.get(spaceId); 
			p = SessionUtil.getPermission(request, spaceId);
		}					
		
		return limitGson.toJson(new RESTHelpers.SpacePermPair(s, p));				
	}	
	
	/**
	 * Handles a request to rerun a single job pair
	 * 
	 * @param pairId The ID of the pair to rerun
	 * @param request
	 * @return json ValidatorStatusCode
	 */

	@POST
	@Path("/jobs/pairs/rerun/{pairid}")
	@Produces("application/json")	
	public String rerunJobPair(@PathParam("pairid") int pairId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JobPair pair=JobPairs.getPair(pairId);
		if (pair==null) {
			return gson.toJson(new ValidatorStatusCode(false, "The pair could not be found"));
		}
		ValidatorStatusCode status=JobSecurity.canUserRerunPairs(pair.getJobId(), userId);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		boolean success=Jobs.rerunPair(pairId);
		
		return success ? gson.toJson(new ValidatorStatusCode(true,"Rerunning of pair began successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Returns the next page of entries for a job pairs table. This is used on the pairsInSpace page
	 *
	 * @param jobSpaceId The id of the job space at the root if the hierarchy we want pairs for
	 * @param stageNumber The stage number to get pair data for. 
	 * @param wallclock True to use wallclock time and false to use cpu time
	 * @param configId The configuration to filter pairs by
	 * @param type The type of pairs to return
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of job pair entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Eric Burns
	 */
	@POST
	@Path("/jobs/pairs/pagination/{jobSpaceId}/{configId}/{type}/{wallclock}/{stageNumber}")
	@Produces("application/json")	
	public String getJobPairsInSpaceHierarchyByConfigPaginated(@PathParam("stageNumber") int stageNumber,
			@PathParam("wallclock") boolean wallclock, @PathParam("jobSpaceId") int jobSpaceId,
			@PathParam("type") String type, @PathParam("configId") int configId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		if (!JobSecurity.isValidGetPairType(type)) {
			return gson.toJson(new ValidatorStatusCode(false, "The selection of a filter type was invalid"));
		}
		
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfPairsByConfigInSpaceHierarchy(jobSpaceId,configId, request,type,wallclock,stageNumber);

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}

	/**
	 * 
	 * @param jobSpaceId ID of the job space to get pairs for
	 * @param stageNumber Number of stage to get pair data for
	 * @param request
	 * @return json MatrixJson object
	 */
	@GET
	@Path("/matrix/finished/{jobSpaceId}/{stageId}")
	@Produces("application/json")
	public String getFinishedJobPairsForMatrix(@PathParam("jobSpaceId") int jobSpaceId, 
											   @PathParam("stageId") int stageNumber, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode valid =JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);
		if (!valid.isSuccess()) {
			return gson.toJson(valid);
		}
		int jobId = Spaces.getJobSpace(jobSpaceId).getJobId();

		final String method = "getFinishedJobPairsForMatrix";
		logUtil.entry(method);
		logUtil.debug(method, "Inputs: jobId="+jobId+" jobSpaceId="+jobSpaceId+" stageId="+stageNumber);


		Map<String, SimpleMatrixElement> benchSolverConfigElementMap = new HashMap<String, SimpleMatrixElement>();
		// Get all the latest new completed job pairs.
		List<JobPair> completedJobPairs = Jobs.getNewCompletedPairsDetailed(jobId, 0);
		for (JobPair pair : completedJobPairs) {
			JoblineStage stage = pair.getStageFromNumber(stageNumber);
			if (stage != null) {
				// Get the three primitives that uniquely identify the MatrixElement we want to send back to the server.
				Benchmark benchmark = pair.getBench();
				Solver solver = stage.getSolver();
				Configuration configuration = stage.getConfiguration();
				// Build a unique string from the three primitives.
				String benchSolverConfigIdentifier = String.format(R.MATRIX_ELEMENT_ID_FORMAT, benchmark.getName(), benchmark.getId(), 
						solver.getName(), solver.getId(), configuration.getName(), configuration.getId());
				// Make it so the identifiers can be used in Jquery selectors.
				benchSolverConfigIdentifier = benchSolverConfigIdentifier.replace("#", "\\#");
				benchSolverConfigIdentifier = benchSolverConfigIdentifier.replace(".", "\\.");
				benchSolverConfigIdentifier = benchSolverConfigIdentifier.replace(":", "\\:");
				// Get the element associated with the job pair.
				String status = Jobs.getStatusFromStage(stage);
				String cpuTime = String.valueOf(stage.getCpuTime());
				String wallclock = String.valueOf(stage.getWallclockTime()); 
				String memUsage = String.valueOf(stage.getMaxVirtualMemory());
				SimpleMatrixElement element = new SimpleMatrixElement(status, cpuTime, memUsage, wallclock);
				// Associate the unique string with the element
				benchSolverConfigElementMap.put(benchSolverConfigIdentifier, element);
			}
		}

		boolean isComplete = Jobs.isJobComplete(jobId);
		MatrixJson matrixData = new MatrixJson(isComplete, benchSolverConfigElementMap);

		logUtil.exit(method);
		return gson.toJson(matrixData);
	}

	// Simplified matrix element so we can send less data via JSON.
	private class SimpleMatrixElement {
		@Expose
		String status;
		@Expose
		String cpuTime;
		@Expose
		String memUsage;
		@Expose
		String wallclock;
		public SimpleMatrixElement(String status, String cpuTime, String memUsage, String wallclock) {
			this.status = status;
			this.cpuTime = cpuTime;
			this.memUsage = memUsage;
			this.wallclock = wallclock;
		}
	}
	
	private class MatrixJson {
		@Expose
		boolean done;
		@Expose
		Map<String, SimpleMatrixElement> benchSolverConfigElementMap;

		public MatrixJson(boolean done, Map<String, SimpleMatrixElement> benchSolverConfigElementMap) {
			this.done = done;
			this.benchSolverConfigElementMap = benchSolverConfigElementMap;
		}
	}

	/**
	 * Determine if the current user is a developer.
	 * @param request The HTTP GET request
	 * @return a JSON boolean value.
	 * @author Albert Giegerich
	 */
	@GET
	@Path("/users/isDeveloper")
	@Produces("application/json")	
	public String userIsDeveloper(@Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		return userIsDeveloper(userId, request);
	}

	/**
	 * Determine if a given user is a developer.
	 * @param userId Determine if user with this id is a developer
	 * @param request The HTTP GET request
	 * @return a JSON boolean value.
	 * @author Albert Giegerich
	 */
	@GET
	@Path("/users/isDeveloper/{userId}")
	@Produces("application/json")	
	public String userIsDeveloper(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		boolean userIsDeveloper = Users.isDeveloper(userId);
		return gson.toJson(userIsDeveloper);
	}
	
	/**
	 * Gets the next page of solvers that the requesting user can SEE. This includes solvers
	 * they own along with public solvers
	 * @param request
	 * @return json object for a DataTables page for solvers
	 */
	@POST
	@Path("/users/solvers/pagination")
	@Produces("application/json")	
	public String getSolversPaginatedByUser(@Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		log.debug("getting a datatable of all the solvers that this user can see");
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfSolversByUser(userId, request);
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}

	
	/**
	 * Gets the next page of benchmarks that a user can see. This includes benchmarks they own
	 * along with public benchmarks
	 * @param request
	 * @return json object for a DataTables page containing the next page of benchmarks
	 */
	@POST
	@Path("/users/benchmarks/pagination")
	@Produces("application/json")	
	public String getBenchmarksPaginatedByUser(@Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		log.debug("getting a datatable of all the benchmarks that this user can see");
		//Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfBenchmarksByUser(userId, request);
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}

	/**
	 * Gets the next page of data for the solver comparison page
	 * @param wallclock True to use wallclock time and false to use cpu time
	 * @param jobSpaceId The ID of the job space to get data for
	 * @param config1 ID of the first config to compare
	 * @param config2 ID of the second config to compare
	 * @param request
	 * @return json DataTables object containing the next page of SolverComparisons
	 */
	@POST
	@Path("/jobs/comparisons/pagination/{jobSpaceId}/{config1}/{config2}/{wallclock}")
	@Produces("application/json")	
	public String getSolverComparisonsPaginated(@PathParam("wallclock") boolean wallclock, @PathParam("jobSpaceId") int jobSpaceId,@PathParam("config1") int config1, @PathParam("config2") int config2, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		int stageNumber=0;
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfSolverComparisonsInSpaceHierarchy(jobSpaceId,config1,config2, request,wallclock,stageNumber);
		log.debug("got the next data table page for the solver comparision web page ");
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Gets paginated fob pairs for an anonymized job page
	 * @param anonymousLinkUuid
	 * @param stageNumber
	 * @param wallclock True to use wallclock time and false to use cpu time
	 * @param jobSpaceId ID of the space to get pairs for
	 * @param syncResults
	 * @param primitivesToAnonymizeName
	 * @param request
	 * @return json DataTables object with the next page of job pairs
	 */
	@POST
	@Path("/jobs/pairs/pagination/anonymousLink/{anonymousLinkUuid}/{jobSpaceId}/{wallclock}/{syncResults}/{stageNumber}/{primitivesToAnonymizeName}")
	@Produces("application/json")	
	public String getJobPairsPaginatedWithAnonymousLink(
			@PathParam("anonymousLinkUuid") String anonymousLinkUuid,
			@PathParam("stageNumber") int stageNumber,
			@PathParam("wallclock") boolean wallclock, 
			@PathParam("jobSpaceId") int jobSpaceId, 
			@PathParam("syncResults") boolean syncResults, 
			@PathParam("primitivesToAnonymizeName") String primitivesToAnonymizeName,
			@Context HttpServletRequest request) {			

		final String methodName = "getJobPairsPaginatedWithAnonymousLink";
		try {
			logUtil.entry( methodName );
			
			ValidatorStatusCode status = JobSecurity.isAnonymousLinkAssociatedWithJobSpace(anonymousLinkUuid, jobSpaceId);

			if ( !status.isSuccess() ) {
				return gson.toJson( status );
			}


			PrimitivesToAnonymize primitivesToAnonymize = AnonymousLinks.createPrimitivesToAnonymize( primitivesToAnonymizeName );
			return RESTHelpers.getJobPairsPaginatedJson( jobSpaceId, wallclock, syncResults, 
					stageNumber, primitivesToAnonymize, request );
		} catch (RuntimeException e) {
			// Catch all runtime exceptions so we can debug them
			logUtil.error( methodName, "Caught a runtime exception: " + Util.getStackTrace(e) ); 
			throw e;
		}
	}

	

	/**
	 * Returns the next page of entries for a job pairs table. This is used on the job details page
	 * @param stageNumber Which stage to get data from for all the paris
	 * @param wallclock True to use wallclock time and false to use cpu time.
	 * @param jobSpaceId ID of the job space to get pairs from
	 * @param syncResults True to only get pairs for which the pair's benchmark has been finished by all solvers/configs in the job space
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of job pair entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Todd Elvers
	 */
	@POST
	@Path("/jobs/pairs/pagination/{jobSpaceId}/{wallclock}/{syncResults}/{stageNumber}")
	@Produces("application/json")	
	public String getJobPairsPaginated(@PathParam("stageNumber") int stageNumber,@PathParam("wallclock") boolean wallclock,
			@PathParam("jobSpaceId") int jobSpaceId, @PathParam("syncResults") boolean syncResults,
			@Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status = JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return RESTHelpers.getJobPairsPaginatedJson( jobSpaceId, wallclock, syncResults, stageNumber, PrimitivesToAnonymize.NONE, request );
	}
	
	/**
	 * Handles an anonymous request to get a space overview graph for a job details page
	 * @param jobSpaceId The job space the chart is for
	 * @param stageNumber stage to get job pair data for
	 * @param anonymousLinkUuid The unique ID associated with this anonymous page
	 * @param primitivesToAnonymizeName String representing which primitive types to anonymize
	 * @param request Object containing other request information
	 * @return A json string containing the path to the newly created png chart
	 * @author Albert Giegerich
	 */
	@POST
	@Path("/jobs/anonymousLink/{anonymousLinkUuid}/{jobSpaceId}/graphs/spaceOverview/{stageNum}/{primitivesToAnonymizeName}")
	@Produces("application/json")
	public String getSpaceOverviewGraph(
			@PathParam("stageNum") int stageNumber, 
			@PathParam("anonymousLinkUuid") String anonymousLinkUuid,
			@PathParam("jobSpaceId") int jobSpaceId, 
			@PathParam("primitivesToAnonymizeName") String primitivesToAnonymizeName,
			@Context HttpServletRequest request) {			

		final String methodName = "getSpaceOverviewGraph";
		logUtil.entry( methodName );

		ValidatorStatusCode status = JobSecurity.isAnonymousLinkAssociatedWithJobSpace( anonymousLinkUuid, jobSpaceId );
		if ( !status.isSuccess() ) {
			return gson.toJson( status );
		}
		
		PrimitivesToAnonymize primitivesToAnonymize = AnonymousLinks.createPrimitivesToAnonymize( primitivesToAnonymizeName );
		return RESTHelpers.getSpaceOverviewGraphJson( stageNumber, jobSpaceId, request, primitivesToAnonymize );
	}
	
	/**
	 * Handles a request to get a space overview graph for a job details page
	 * @param jobSpaceId The job space the chart is for
	 * @param stageNumber the stage number to get pair data for
	 * @param request Object containing other request information
	 * @return A json string containing the path to the newly created png chart
	 */
	@POST
	@Path("/jobs/{jobSpaceId}/graphs/spaceOverview/{stageNum}")
	@Produces("application/json")
	public String getSpaceOverviewGraph(@PathParam("stageNum") int stageNumber, @PathParam("jobSpaceId") int jobSpaceId, @Context HttpServletRequest request) {			
		log.debug("Got request to get space overview graph.");
		int userId = SessionUtil.getUserId(request);
		// Ensure user can view the job they are requesting the pairs from
		ValidatorStatusCode status=JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}

		return RESTHelpers.getSpaceOverviewGraphJson( stageNumber, jobSpaceId, request, PrimitivesToAnonymize.NONE );
	}

    /**
     * Handles a request to get a community statistical overview
     * @param request 
     * @author Julio Cervantes
     * @return A json string containing the path to the newly created png chart as well as
     * an image map linking points to benchmarks
     */
    @POST
    @Path("/secure/explore/community/overview")
    @Produces("application/json")	
	public String getCommunityOverview(@Context HttpServletRequest request){

	    Communities.updateCommunityMapIf();

	    if(R.COMM_INFO_MAP == null){
		
	    	return gson.toJson(ERROR_DATABASE);
	    }
	    log.info("R.COMM_INFO_MAP: " + R.COMM_INFO_MAP);
		JsonObject graphs = null;
		
		List<Space> communities = Communities.getAll();
		
		graphs=Statistics.makeCommunityGraphs(communities,R.COMM_INFO_MAP);
		if (graphs==null) {
			return gson.toJson(ERROR_DATABASE);
		}
		JsonObject info = new JsonObject();
		for(Space c : communities){
		    String name = c.getName();
		    int id = c.getId();

		    JsonObject Comm = new JsonObject();
		    Comm.addProperty("users",R.COMM_INFO_MAP.get(id).get("users").toString());
		    Comm.addProperty("solvers",R.COMM_INFO_MAP.get(id).get("solvers").toString());
		    Comm.addProperty("benchmarks",R.COMM_INFO_MAP.get(id).get("benchmarks").toString());
		    Comm.addProperty("jobs",R.COMM_INFO_MAP.get(id).get("jobs").toString());
		    Comm.addProperty("job_pairs",R.COMM_INFO_MAP.get(id).get("job_pairs").toString());
		    Comm.addProperty("disk_usage",Util.byteCountToDisplaySize(R.COMM_INFO_MAP.get(id).get("disk_usage")));

		    info.add(name,Comm);

		}

		// Instantiate a Date object
		Date last_update = new Date(R.COMM_ASSOC_LAST_UPDATE);
		
		JsonObject json=new JsonObject();
		json.add("graphs", graphs);
		json.add("info",info);
		json.addProperty("date",last_update.toString());
		
		return gson.toJson(json);
	}

	/**
	 * Handles a request to get a solver comparison graph for the job details page using an anonymous link.
	 * @param jobSpaceId The job space the chart is for
	 * @param config1 The ID of the first configuration to handle
	 * @param config2 The ID of the second configuration to handle
	 * @param request Object containing other request information
	 * @param anonymousLinkUuid The ID assigned to this anonymous page
	 * @param stageNumber the number of the stage to get data for
	 * @param edgeLengthInPixels Size of edge of square graph
	 * @param axisColor String color for axis labels. Must correspond to some java Color.
	 * @param primitivesToAnonymizeName Represents which primitive types to anonymize in the graph
	 * @return A json string containing the path to the newly created png chart as well as an image map linking points to benchmarks
	 * @author Albert Giegerich
	 */
	@POST
	@Path("/jobs/anonymousLink/{anonymousLinkUuid}/{jobSpaceId}/graphs/solverComparison/{config1}/{config2}/{edgeLengthInPixels}/{axisColor}/{stageNum}/{primitivesToAnonymizeName}")
	@Produces("application/json")	
	public String getAnonymousSolverComparisonGraph(
			@PathParam("anonymousLinkUuid") String anonymousLinkUuid,
			@PathParam("stageNum") int stageNumber, 
			@PathParam("jobSpaceId") int jobSpaceId,
			@PathParam("config1") int config1, 
			@PathParam("config2") int config2, 
			@PathParam("edgeLengthInPixels") int edgeLengthInPixels,
			@PathParam("axisColor") String axisColor, 
			@PathParam("primitivesToAnonymizeName") String primitivesToAnonymizeName,
			@Context HttpServletRequest request) {		

		final String methodName = "getAnonymousSolverComparisonGraph";
		try {
			logUtil.entry( methodName );
			logUtil.debug( methodName, "Got request to get an anonymous solver comparison graph with parameters:\n"+
					"\tanonymousLinkUuid: "+anonymousLinkUuid+"\n"+
					"\tstageNumber: "+stageNumber+"\n"+
					"\tjobSpaceId: "+jobSpaceId+"\n"+
					"\tconfig1: "+config1+"\n"+
					"\tconfig2: "+config2+"\n"+
					"\tedgeLengthInPixels: "+edgeLengthInPixels+"\n"+
					"\taxisColor: "+axisColor+"\n");
			ValidatorStatusCode status = JobSecurity.isAnonymousLinkAssociatedWithJobSpace(anonymousLinkUuid, jobSpaceId);

			if ( !status.isSuccess() ) {
				return gson.toJson( status );
			}

			PrimitivesToAnonymize primitivesToAnonymize = AnonymousLinks.createPrimitivesToAnonymize( primitivesToAnonymizeName );
			return RESTHelpers.getSolverComparisonGraphJson(
					jobSpaceId, config1, config2, edgeLengthInPixels, axisColor, stageNumber, primitivesToAnonymize );
		} catch ( RuntimeException e ) {
			logUtil.error( methodName, "Caught a runtime exception: " + Util.getStackTrace( e ));
			return gson.toJson( ERROR_INTERNAL_SERVER );
		} 
	}

  
	/**
	 * Handles a request to get a solver comparison graph for a job details page
	 * @param jobSpaceId The job space the chart is for
	 * @param config1 The ID of the first configuration to handle
	 * @param config2 The ID of the second configuration to handle
	 * @param stageNumber the stage to get job pair data for
	 * @param edgeLengthInPixels Number of pixels along the perimeter of the square graph.
	 * @param request Object containing other request information
	 * @param axisColor The color to make the axis labels. Must be a string that corresponds to some java Color
	 * @return A json string containing the path to the newly created png chart as well as
	 * an image map linking points to benchmarks
	 */
	@POST
	@Path("/jobs/{jobSpaceId}/graphs/solverComparison/{config1}/{config2}/{edgeLengthInPixels}/{axisColor}/{stageNum}")
	@Produces("application/json")	
	public String getSolverComparisonGraph(@PathParam("stageNum") int stageNumber, @PathParam("jobSpaceId") int jobSpaceId,
			@PathParam("config1") int config1, @PathParam("config2") int config2, 
			@PathParam("edgeLengthInPixels") int edgeLengthInPixels,@PathParam("axisColor") String axisColor, @Context HttpServletRequest request) {		
		final String methodName = "getSolverComparisonGraph";
		logUtil.entry(methodName);
		logUtil.debug( methodName, "Got request to get an anonymous solver comparison graph with parameters:\n"+
				"\tstageNumber: "+stageNumber+"\n"+
				"\tjobSpaceId: "+jobSpaceId+"\n"+
				"\tconfig1: "+config1+"\n"+
				"\tconfig2: "+config2+"\n"+
				"\tedgeLengthInPixels: "+edgeLengthInPixels+"\n"+
				"\taxisColor: "+axisColor+"\n");
	        
		int userId = SessionUtil.getUserId(request);
		
		ValidatorStatusCode status= JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		} else {
			return RESTHelpers.getSolverComparisonGraphJson( 
					jobSpaceId, config1, config2, edgeLengthInPixels, axisColor, stageNumber, PrimitivesToAnonymize.NONE );
		}
	}
	
	/**
	 * Gets the next page of job stats for an anonymized job page
	 * @param stageNumber The stage number to get pair data for
	 * @param jobSpaceId The ID of the space to get pairs for
	 * @param anonymousJobLink The anonymous link associated with the page. This is needed for security, as only
	 * authorized users should possess this link.
	 * @param primitivesToAnonymizeName
	 * @param shortFormat Whether to get the full stats for the stats table (true) or a truncated version for the 
	 * space overview table
	 * @param wallclock True to use wallclock time and false to use cpu time
	 * @param request 
	 * @return json DataTables object containing the next page of SolverStats objects
	 */
	@POST
	@Path("/jobs/solvers/anonymousLink/pagination/{jobSpaceId}/{anonymousJobLink}/{primitivesToAnonymizeName}/{shortFormat}/{wallclock}/{stageNum}/")
	@Produces("application/json")
	public String getAnonymousJobStatsPaginated( 
			@PathParam("stageNum") int stageNumber, 
			@PathParam("jobSpaceId") int jobSpaceId,
			@PathParam("anonymousJobLink") String anonymousJobLink, 
			@PathParam("primitivesToAnonymizeName") String primitivesToAnonymizeName,
			@PathParam("shortFormat") boolean shortFormat, 
			@PathParam("wallclock") boolean wallclock, 
			@Context HttpServletRequest request ) {

		final String methodName = "getAnonymousJobStatsPaginated";
		try {

			ValidatorStatusCode status = JobSecurity.isAnonymousLinkAssociatedWithJobSpace(anonymousJobLink, jobSpaceId);
			if ( !status.isSuccess() ) {
				return gson.toJson( status );
			} else {
				PrimitivesToAnonymize primitivesToAnonymize = AnonymousLinks.createPrimitivesToAnonymize( primitivesToAnonymizeName );
				JobSpace jobSpace = Spaces.getJobSpace(jobSpaceId);

				return RESTHelpers.getNextDataTablePageForJobStats( stageNumber, jobSpace, primitivesToAnonymize, shortFormat, wallclock );
			}
		} catch (RuntimeException e) {
			// Catch all runtime exceptions so we can debug them
			logUtil.error( methodName, "Caught a runtime exception: " + Util.getStackTrace(e) ); 
			throw e;
		}
	}

	/**
	 * Returns the next page of stats for the given job and job space
	 * @param jobSpaceId The ID of the job space to get stats for
	 * @param stageNumber the stage number to get job pair data for
	 * @param shortFormat Whether to retrieve the fields for the full stats table or the truncated stats for the space summary tables
	 * @param wallclock True to use wallclock time and false to use cpu time
	 * @param request
	 * @return a json DataTables object containing the next page of stats.
	 * @author Eric Burns
	 */
	@POST
	@Path("/jobs/solvers/pagination/{jobSpaceId}/{shortFormat}/{wallclock}/{stageNum}")
	@Produces("application/json")
	public String getJobStatsPaginated(@PathParam("stageNum") int stageNumber, @PathParam("jobSpaceId") int jobSpaceId, 
			@PathParam("shortFormat") boolean shortFormat, @PathParam("wallclock") boolean wallclock,
			@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserSeeJobSpace(jobSpaceId, userId);

		if (!status.isSuccess()) {
			return gson.toJson(status);
		} else {
			JobSpace space = Spaces.getJobSpace(jobSpaceId);
			return RESTHelpers.getNextDataTablePageForJobStats( stageNumber, space, PrimitivesToAnonymize.NONE, shortFormat, wallclock );
		}
	}

	@POST
	@Path("/jobs/addJobPairs/confirmation") 
	@Produces("application/json")
	public String getNumberOfPairsToBeAddedAndDeleted( @Context HttpServletRequest request ) {
		final String methodName = "getNumberOfPairsToBeAddedAndDeleted";
		final String jobIdParam = "jobId";
		final String configsParam = "configs";
		final String addToAllParam = "addToAll";
		final String addToPairedParam = "addToPaired";
		try {
			final int userId = SessionUtil.getUserId( request );
			final int jobId = Integer.parseInt( request.getParameter(jobIdParam) );


			Set<Integer> selectedConfigIds = new HashSet<>( Util.toIntegerList( request.getParameterValues( configsParam ) ) );
			Set<Integer> allConfigIdsInJob = Solvers.getConfigIdSetByJob( jobId );

			Set<Integer> configIdsToDelete = new HashSet<>( allConfigIdsInJob );
			configIdsToDelete.removeAll( selectedConfigIds );

			Map<String, Object> jsonObject = new HashMap<>();
			List<JobPair> jobPairsToBeDeleted = Jobs.getJobPairsToBeDeletedFromConfigIds( jobId, configIdsToDelete );
			jsonObject.put("pairsToBeDeleted", jobPairsToBeDeleted.size() );

			Set<Integer> solverIdsToAddToAll = new HashSet<>( Util.toIntegerList( request.getParameterValues( addToAllParam ) ) );
			Set<Integer> solverIdsToAddToPaired = new HashSet<>( Util.toIntegerList( request.getParameterValues( addToPairedParam ) ) );

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

			configIdsToAddToPaired.removeAll( allConfigIdsInJob );

			Set<Integer> jobPairIdsToBeDeleted = buildJobPairIdSet( jobPairsToBeDeleted );
			int pairedBenchmarkCount = Jobs.countJobPairsToBeAddedFromConfigIdsForPairedBenchmarks( jobId, configIdsToAddToPaired, jobPairIdsToBeDeleted );
			log.debug( "pairedBenchmarkCount: "+pairedBenchmarkCount );
			int allBenchmarkCount = Jobs.countJobPairsToBeAddedFromConfigIdsForAllBenchmarks( jobId, configIdsToAddToAll, jobPairIdsToBeDeleted );
			log.debug( "allBenchmarkCount: "+allBenchmarkCount );

			jsonObject.put("pairsToBeAdded",  pairedBenchmarkCount + allBenchmarkCount );


			jsonObject.put("success", true);
			return gson.toJson( jsonObject );
		} catch ( Exception e ) {
			Map<String, Object> jsonObject = new HashMap<>();
			jsonObject.put("success", false);
			return gson.toJson( jsonObject );
		}
	}

	private static Set<Integer> buildJobPairIdSet( List<JobPair> jobPairs ) {
		Set<Integer> jobPairIds = new HashSet<>();
		for ( JobPair pair : jobPairs ) {
			jobPairIds.add( pair.getId() );
		}

		return jobPairIds;
	}	

	/**
	 * Gets job pairs running on the given node
	 * @param id The ID of the node to retrieve running pairs on
	 * @param request
	 * @return json object containing the next page of job pairs running on this node
	 * @author Wyatt Kaiser
	 */
	@GET
	@Path("/cluster/nodes/{id}/pagination")
	@Produces("application/json")	
	public String getNodeJobPairs(@PathParam("id") int id, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageCluster("node", id, userId, request);

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	/**
	 * @param id ID of the queue to get pairs for
	 * @param request
	 * @return a json string representing all attributes of the queue with the given id
	 * @author Wyatt Kaiser
	 */
	@GET
	@Path("/cluster/queues/{id}/pagination")
	@Produces("application/json")	
	public String getQueueJobPairs(@PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		try {
		    nextDataTablesPage = RESTHelpers.getNextDataTablesPageCluster("queue", id, userId, request);
		}
		catch(Exception e) {
		    log.error(e);
		}
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	

	/**
	 * Returns the next page of entries in a given DataTable (not restricted by space, returns ALL).
	 * These populate tables on the admin pages
	 * @param primType the type of primitive
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of entries if successful,<br>
	 * 		1 if the request fails parameter validation, <br>
	 * @author Wyatt kaiser
	 */
	@POST
	@Path("/{primType}/admin/pagination/")
	@Produces("application/json")
	public String getAllPrimitiveDetailsPagination(@PathParam("primType") String primType, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		if (primType.startsWith("u")) {
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageAdmin(RESTHelpers.Primitive.USER, request);
		}
		if (primType.startsWith("j")) {
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageAdmin(RESTHelpers.Primitive.JOB, request);
		}

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
	}

	/**
	 * @param primitiveType The type of primitive (solver, bench, etc) that we're going to generate a link for.
	 * @param primitiveId The id of the primitive to generate an anonymous public URL for.
	 * @param primitivesToAnonymizeName String representing which primitives to anonymize
	 * @param request The http request.
	 * @return json ValidatorStatusCode
	 * @author Albert Giegerich
	 */
	@POST
	@Path( "/anonymousLink/{primitiveType}/{primitiveId}/{primitivesToAnonymizeName}" )
	@Produces( "application/json" )
	public String getAnonymousLinkForPrimitive( 
			@PathParam("primitiveType") String primitiveType,
			@PathParam("primitiveId") int primitiveId, 
			@PathParam("primitivesToAnonymizeName") String primitivesToAnonymizeName,
			@Context HttpServletRequest request ) {

		final String methodName = "getAnonymousLinkForPrimitive";
		try {
			logUtil.entry( methodName );
			logUtil.debug( methodName, "primitiveType = " + primitiveType + ", primitiveId = " + primitiveId + 
					", primitivesToAnonymizeName = " + primitivesToAnonymizeName );

			int userId = SessionUtil.getUserId(request);
			ValidatorStatusCode status = GeneralSecurity.canUserGetAnonymousLinkForPrimitive( userId, primitiveType, primitiveId );
		
			// Check if user has permission to get an anonymous link for this benchmark.
			if ( status.isSuccess() ) {
				log.debug( "User with id=" + userId + " is allowed to create anonymous link for primitive.");

				// Create a new Gson that won't encode the = sign as \u003d
				Gson tempGson = new GsonBuilder().disableHtmlEscaping().create();


				PrimitivesToAnonymize primitivesToAnonymize = AnonymousLinks.createPrimitivesToAnonymize( primitivesToAnonymizeName );
				// Return a link associated with the primitive.
				String anonymousLinkForPrimitive = createAnonymousLinkForPrimitive( primitiveType, primitiveId, primitivesToAnonymize );
				return tempGson.toJson( new ValidatorStatusCode( true, anonymousLinkForPrimitive ));
			} else {
				log.debug( "User with id=" + userId + " is not allowed to create anonymous link for primitive.");
				// Return the failed security check status.
				return gson.toJson( status );
			}
		} catch ( SQLException e ) {
			return gson.toJson( new ValidatorStatusCode( false, e.getMessage() ));	
		} catch ( RuntimeException e) {
			logUtil.error( methodName, Util.getStackTrace( e ));
			return gson.toJson( new ValidatorStatusCode( false, e.getMessage() ));	
		}
	}

	/**
	 * Creates a new anonymous link for a given primitive.
	 * @author Albert Giegerich
	 */
	private String createAnonymousLinkForPrimitive( 
			final String primitiveType, 
			final int primitiveId, 
			final PrimitivesToAnonymize primitivesToAnonymize ) throws SQLException {

		String primitiveUrlName = getPrimitiveUrlName( primitiveType );

		// The entire url for the link except for a unique code that will be appended to the end.
		final String urlPrefix = R.STAREXEC_URL_PREFIX + "://" + R.STAREXEC_SERVERNAME + "/" + R.STAREXEC_APPNAME + 
								 "/secure/details/" + primitiveUrlName + ".jsp?anonId=";

		// If the anonymous link for this primitive is already in the database, retrieve and return it.
		Optional<String> optionalUniqueId = AnonymousLinks.getAnonymousLinkCode( primitiveType, primitiveId, primitivesToAnonymize );
		if ( optionalUniqueId.isPresent() ) {
			return urlPrefix + optionalUniqueId.get();
		}

		// Generate a unique id to be part of the link URL and store it in the database.
		final String uniqueId = AnonymousLinks.addAnonymousLink( primitiveType, primitiveId, primitivesToAnonymize );
		if ( primitiveType.equals( R.JOB ) && !AnonymousLinks.isNothingAnonymized( primitivesToAnonymize ) 
			 && !AnonymousLinks.hasJobBeenAnonymized( primitiveId ) ) {

			// If the primitive is a job add anonymous primitive names to the DB for all the primitives in the job.
			AnonymousLinks.addAnonymousNamesForJob( primitiveId );	
		}

		// Return the URL with the UUID as a parameter.
		return urlPrefix + uniqueId; 
	}

	/**
	 * Returns the name of the url path used for the given primitive type.
	 * @author Albert Giegerich
	 */
	private String getPrimitiveUrlName( final String primitiveType ) {
		if ( primitiveType.equals( "bench" )) {
			return "benchmark";
		} else {
			return primitiveType;
		}
	}
	
	/**
	 * Update the name of an existing job
	 * @param jobId ID of the job to change
	 * @param newName New name to assign the job
	 * @param request
	 * @return json ValidatorStatusCode
	 */
	@POST
	@Path("/job/edit/name/{jobId}/{newName}")
	@Produces("application/json")
	public String editJobName(@PathParam("jobId") int jobId, @PathParam("newName") String newName, @Context HttpServletRequest request) {
		final String method = "editJobName";
		logUtil.entry(method);
		logUtil.debug(method, "Editing job name for job with id="+jobId+" where the new name="+newName);

		int userId = SessionUtil.getUserId(request);

		ValidatorStatusCode status = null;
		if (JobSecurity.userOwnsJobOrIsAdmin(jobId, userId)) {
			try {
				Jobs.setJobName(jobId, newName);
				status = new ValidatorStatusCode(true, "Name changed successfully.");
			} catch (Exception e) {
				status = new ValidatorStatusCode(false, e.getMessage());
			}
		} else {
			status = new ValidatorStatusCode(false, "You do not have permission to change this job's name.");	
		}

		logUtil.exit(method);
		return gson.toJson(status);
	}
	/**
	 * Update the description of a job.
	 * @param jobId ID of the job to impact.
	 * @param newDescription
	 * @param request
	 * @return  json ValidatorStatusCode
	 */
	@POST
	@Path("/job/edit/description/{jobId}/{newDescription}")
	@Produces("application/json")
	public String editJobDescription(@PathParam("jobId") int jobId, @PathParam("newDescription") String newDescription, 
			                  @Context HttpServletRequest request) {
		final String method = "editJobDescription";
		logUtil.entry(method);
		logUtil.debug(method, "Editing job description for job with id="+jobId+" where the new description="+newDescription);

		int userId = SessionUtil.getUserId(request);

		ValidatorStatusCode status = null;
		if (JobSecurity.userOwnsJobOrIsAdmin(jobId, userId)) {
			try {
				Jobs.setJobDescription(jobId, newDescription);
				status = new ValidatorStatusCode(true, "Description changed successfully.");
			} catch (Exception e) {
				status = new ValidatorStatusCode(false, e.getMessage());
			}
		} else {
			status = new ValidatorStatusCode(false, "You do not have permission to change this job's description.");	
		}

		logUtil.exit(method);
		return gson.toJson(status);
	}

	/**
	 * Gets the next page of benchmarks that are in the given space
	 * @param spaceId The ID of the space to get benchmarks for
	 * @param request
	 * @return json object for a new DataTables page of benchmarks
	 */
	@POST
	@Path("/job/{spaceId}/allbench/pagination/")
	@Produces("application/json")	
	public String getAllBenchmarksInSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {	
		log.debug("got a request to getPrimitiveDetailsPaginated!");
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		// Ensure user can view the space containing the primitive(s)
		log.debug("reached part two with space id = "+spaceId);

		ValidatorStatusCode status=SpaceSecurity.canUserSeeSpace(spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		List<Benchmark> benches = Benchmarks.getBySpace(spaceId);
		nextDataTablesPage= RESTHelpers.convertBenchmarksToJsonObject(benches, new DataTablesQuery(benches.size(), benches.size(), -1));
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Returns the next page of entries in a given DataTable
	 *
	 * @param spaceId the id of the space to query for primitives from
	 * @param primType the type of primitive
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Todd Elvers
	 */
	@POST
	@Path("/space/{id}/{primType}/pagination/")
	@Produces("application/json")	
	public String getPrimitiveDetailsPaginated(@PathParam("id") int spaceId, @PathParam("primType") String primType, @Context HttpServletRequest request) {	
		log.debug("got a request to getPrimitiveDetailsPaginated!");
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		// Ensure user can view the space containing the primitive(s)
		log.debug("reached part two with space id = "+spaceId);

		ValidatorStatusCode status=SpaceSecurity.canUserSeeSpace(spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		// Query for the next page of primitives and return them to the user
		if(primType.startsWith("j")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForSpaceExplorer(RESTHelpers.Primitive.JOB, spaceId, request);
		} else if(primType.startsWith("u")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForSpaceExplorer(RESTHelpers.Primitive.USER, spaceId, request);
		} else if(primType.startsWith("so")){
			
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForSpaceExplorer(RESTHelpers.Primitive.SOLVER, spaceId, request);
		} else if(primType.startsWith("sp")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForSpaceExplorer(RESTHelpers.Primitive.SPACE, spaceId, request);
		} else if(primType.startsWith("b")){
			
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForSpaceExplorer(RESTHelpers.Primitive.BENCHMARK, spaceId, request);
		}
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	

	/**
	 * Gets the permissions a given user has in a given space
	 * 
	 * @param spaceId the id of the space to check a user's permissions in
	 * @param userId the id of the user to check the permissions of
	 * @param request
	 * @return a json string representing the user's permissions in the given space
	 * @author Todd Elvers
	 */
	@POST
	@Path("/space/{spaceId}/perm/{userId}")
	@Produces("application/json")	
	public String getUserSpacePermissions(@PathParam("spaceId") int spaceId, @PathParam("userId") int userId, @Context HttpServletRequest request) {
		Permission p = SessionUtil.getPermission(request, spaceId);
		List<Space> communities = Communities.getAll();
		for (Space s : communities) {
			if (spaceId == s.getId()) {
				if (GeneralSecurity.hasAdminWritePrivileges(userId)) {
					return gson.toJson(Permissions.get(userId, spaceId));
				}
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
				
			}
		}
		
		if(p != null && (SessionUtil.getUserId(request) == userId || p.isLeader() )) {
			return gson.toJson(Permissions.get(userId, spaceId));
		}
		
		
		return null;
	}	
	
	/**
	 * @param request
	 * @return a json string representing all the subspaces of the space with
	 * the given id. If the given id is <= 0, then the root space is returned
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/session/logout")
	@Produces("application/json")	
	public String doInvalidateSession(@Context HttpServletRequest request) {	
		log.info(String.format("User [%s] manually logged out", SessionUtil.getUser(request).getEmail()));
		request.getSession().invalidate();
		return gson.toJson(new ValidatorStatusCode(true));
	}	
	
	/**
	 * Retrieves the associated websites of a given user, space, or solver.
	 * The type is included in the POST path; if it's a space or solver, the
	 * space/solver id is also included in the POST path.
	 * @param type The type of primitive (user, space, solver) to associate the site with
	 * @param id The ID of the primitive given by type
	 * @param request
	 * @return a json string representing all the websites associated with
	 * the current user/space/solver
	 * @author Skylar Stark and Todd Elvers
	 */
	@GET
	@Path("/websites/{type}/{id}")
	@Produces("application/json")
	public String getWebsites(@PathParam("type") String type, @PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		if(type.equals("user")){
			return gson.toJson(Websites.getAllForJavascript(id, WebsiteType.USER));
		} else if(type.equals("space")){
			ValidatorStatusCode status = SpaceSecurity.canUserSeeSpace(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return gson.toJson(Websites.getAllForJavascript(id, WebsiteType.SPACE));
		} else if (type.equals(R.SOLVER)) {
			ValidatorStatusCode status = SolverSecurity.canUserSeeSolver(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return gson.toJson(Websites.getAllForJavascript(id, WebsiteType.SOLVER));
		}
		return gson.toJson(ERROR_INVALID_WEBSITE_TYPE);
	}
	
	/**
	 * Adds website information to the database. This is dynamic to allow adding a
	 * website associated with a space, solver, or user. The type of website is given
	 * in the path
	 * @param type The type of primitive we are adding the website to
	 * @param id the ID of the primitive specified by type
	 * @param request
	 * @return a json string containing '0' if the add was successful, '1' otherwise
	 */
	@POST
	@Path("/website/add/{type}/{id}")
	@Produces("application/json")
	public String addWebsite(@PathParam("type") String type, @PathParam("id") int id, @Context HttpServletRequest request) {
		boolean success = false;
		int userId = SessionUtil.getUserId(request);
		String name = request.getParameter("name");
		String url = request.getParameter("url");	
		ValidatorStatusCode status = WebsiteSecurity.canUserAddWebsite(id, type, userId, name, url);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		if (type.equals(R.USER)) {
			success = Websites.add(id, url, name,WebsiteType.USER);
		} else if (type.equals(R.SPACE)) {
			success = Websites.add(id, url, name, WebsiteType.SPACE);
		} else if (type.equals(R.SOLVER)) {
			success = Websites.add(id, url, name, WebsiteType.SOLVER);
			
		}
		
		// Passed validation AND Database update successful	
		return success ? gson.toJson(new ValidatorStatusCode(true,"Website added successfully")) : gson.toJson(ERROR_DATABASE);
	}

	
	/**
	 * Deletes a website, which may be associated with a user, space, or solver
	 *
	 * @param websiteId the id of the website to remove
	 * @param request
	 * @return json ValidatorStatusCode
	 * @author Todd Elvers
	 */
	@POST
	@Path("/websites/delete/{websiteId}")
	@Produces("application/json")
	public String deleteWebsite(@PathParam("websiteId") int websiteId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status = WebsiteSecurity.canUserDeleteWebsite(websiteId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		return Websites.delete(websiteId) ? gson.toJson(new ValidatorStatusCode(true,"Website deleted successfully")) : gson.toJson(ERROR_DATABASE);

		
	}
	
	/**
	 * Runs TestSequences that are given by name
	 * @param request
	 * @return json ValidatorStatusCode
	 */
	
	@POST
	@Path("/test/runTests")
	@Produces("appliation/json")
	public String runTest(@Context HttpServletRequest request) {
		int u=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserRunTests(u,false);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
			
		final String[] testNames=request.getParameterValues("testNames[]");
		if (testNames==null || testNames.length==0) {
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		TestManager.executeTests(testNames);
		
			
		return gson.toJson(new ValidatorStatusCode(true,"Testing started successfully"));
			
	}
	

	/**
	 * Runs every TestSequence. This does NOT run a stress test!
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	
	@POST
	@Path("/test/runAllTests")
	@Produces("appliation/json")
	public String runAllTests(@Context HttpServletRequest request) {
		int u=SessionUtil.getUserId(request);

		ValidatorStatusCode status=GeneralSecurity.canUserRunTests(u,false);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
			
		boolean success=TestManager.executeAllTestSequences();
			
		return success ?  gson.toJson(new ValidatorStatusCode(true,"Testing started successfully")) : gson.toJson(ERROR_DATABASE);
		
	}
	
	/**
	 * Handles a request to edit the non-SGE attributes (like timeouts) of an existing queue
	 * @param id The ID of the queue being updated
	 * @param request
	 * @return a json ValidatorStatuscode
	 */
	
	@POST
	@Path("/edit/queue/{id}")
	@Produces("application/json")
	public String editQueueInfo(@PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);

		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(new ValidatorStatusCode(false, "You must be an admin to edit this queue."));
		}


		if (!Util.paramExists("cpuTimeout", request) || !Util.paramExists("wallTimeout", request)) {
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		int cpuTimeout=0;
		int wallTimeout=0;
		try {
			cpuTimeout=Integer.parseInt(request.getParameter("cpuTimeout"));
			wallTimeout=Integer.parseInt(request.getParameter("wallTimeout"));
		} catch (Exception e) {
			return gson.toJson(new ValidatorStatusCode(false, "Timeouts need to be integers between 1 and 2^31"));
		}

		
		
		ValidatorStatusCode status=QueueSecurity.canUserEditQueue(userId, wallTimeout, cpuTimeout);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		boolean success=Queues.updateQueueCpuTimeout(id, cpuTimeout) && Queues.updateQueueWallclockTimeout(id, wallTimeout);
		return success ? gson.toJson(new ValidatorStatusCode(true,"Queue edited successfully")) : gson.toJson(ERROR_DATABASE);
	}

	/** 
	 * Updates information in the database using a POST. Attribute and
	 * new value are included in the path. First validates that the new value
	 * is legal, then updates the database and session information accordingly.
	 * @param attribute Name of attribute to update
	 * @param userId The ID of the user to update
	 * @param newValue The new value to assign to the specified attribute
	 * @param request
	 * 
	 * @return a json string containing '0' if the update was successful, else 
	 * a json string containing '1'
	 * @author Skylar Stark
	 */
	@POST
	@Path("/edit/user/{attr}/{userId}/{val}")
	@Produces("application/json")
	public String editUserInfo(@PathParam("attr") String attribute, @PathParam("userId") int userId,
			@PathParam("val") String newValue, @Context HttpServletRequest request) {	
		int requestUserId=SessionUtil.getUserId(request);
		log.debug("requestUserId" + requestUserId);
		ValidatorStatusCode status=UserSecurity.canUpdateData(userId, requestUserId, attribute, newValue);
		log.debug("status = " + status);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		boolean success = false;
		String messageToUser = null;
		// Go through all the cases, depending on what attribute we are changing.
		// First, validate that it is in legal form. Then, try to update the database.
		// Finally, update the current session data
		if (attribute.equals("firstname")) {
			
			success = Users.updateFirstName(userId, newValue);
			if (success) {
				SessionUtil.getUser(request).setFirstName(newValue);
				messageToUser = "Edit successful.";
			}
		} else if (attribute.equals("lastname")) {
			success = Users.updateLastName(userId, newValue);
			if (success) {
				SessionUtil.getUser(request).setLastName(newValue);
				messageToUser = "Edit successful.";
			}
		} else if (attribute.equals("institution")) {
			success = Users.updateInstitution(userId, newValue);
			if (success) {
				SessionUtil.getUser(request).setInstitution(newValue);
				messageToUser = "Edit successful.";
			}
		} else if (attribute.equals("email")) {
			log.info("User with id="+userId+" has requested to change their email to "+newValue);
			success = true;
			if (!Users.getUserByEmail(newValue)) {
				try {
					String code = UUID.randomUUID().toString();
					// Add the request to the database.
					Requests.addChangeEmailRequest(userId, newValue, code);
					// Send a validation email to the new email address. using a unique 
					// code to safely reference this user's entry in verification hyperlinks
					Mail.sendEmailChangeValidation(newValue, code);
					log.debug("Email sent to user with id="+userId+" at address "+newValue+" to validate email change request.");
					messageToUser = "A verification email has been sent to the new email address.";
				} catch (IOException e) {
					log.warn("(editUserInfo) an error occurred while trying to send a change email verification email.", e);
					messageToUser = "Could not send verification email.";
					success = false;
				} catch (StarExecDatabaseException e) {
					log.error("(editUserInfo) an error occurred while trying to add a change email request.", e);
					messageToUser = "Internal error: could not complete email change request.";
					success = false;
				}
			} else {
				messageToUser = "A user with that email already exists.";
				success = false;
			}
		} else if (attribute.equals("diskquota")) {
			log.debug("diskquota");
			success=Users.setDiskQuota(userId, Long.parseLong(newValue));
			log.debug("success = " + success);
			if (success) {
				SessionUtil.getUser(request).setDiskQuota(Long.parseLong(newValue));
				messageToUser = "Edit successful.";
			}
		} else if (attribute.equals("pagesize")) {
			success=Users.setDefaultPageSize(userId, Integer.parseInt(newValue));
			if (success) {
				messageToUser = "Edit successful.";
			}
		}

		String json = null;
		if (success) {
			json = gson.toJson(new ValidatorStatusCode(true, messageToUser)); 
		} else if (messageToUser != null) {
			json = gson.toJson(new ValidatorStatusCode(false, messageToUser));
		} else {
			json = gson.toJson(ERROR_DATABASE);
		}

		return json;
	}
	
	
	/**
	 * Sets a settings profile to be the default for the user making the request
	 * @param id The ID of a settings profile
	 * @param userIdOfOwner
	 * @param request
	 * @return json ValidatorStatusCode
	 */
	@POST
	@Path("/set/defaultSettings/{id}/{userIdOfOwner}")
	@Produces("application/json")
	public String setSettingsProfileForUser(@PathParam("id") int id, @PathParam("userIdOfOwner") int userIdOfOwner, 
											@Context HttpServletRequest request) {	
		int userIdOfCaller=SessionUtil.getUserId(request);

		ValidatorStatusCode status=SettingSecurity.canUserSeeProfile(id, userIdOfOwner, userIdOfCaller);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		log.debug("setting a new default profile for a user");	
		boolean success=Settings.setDefaultProfileForUser(userIdOfCaller, id);
		// Passed validation AND Database update successful
		return success ? gson.toJson(new ValidatorStatusCode(true,"Profile set as default")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Deletes a DefaultSettings profile
	 * @param id The ID of the profile to delete
	 * @param request
	 * @return a json ValidatorStatuscode
	 */
	@POST
	@Path("/delete/defaultSettings/{id}")
	@Produces("application/json")
	public String deleteDefaultSettings(@PathParam("id") int id, @Context HttpServletRequest request) {	
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=SettingSecurity.canModifySettings(id,userId);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		try {			
			boolean success=Settings.deleteProfile(id);
			// Passed validation AND Database update successful
			return success ? gson.toJson(new ValidatorStatusCode(true,"Community edit successful")) : gson.toJson(ERROR_DATABASE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			return gson.toJson(ERROR_DATABASE);
		}
		
	}
	
	/** 
	 * Updates information for a space in the database using a POST. Attribute and
	 * new value are included in the path. First validates that the new value
	 * is legal, then updates the database and session information accordingly.
	 * @param attribute The string name of the attribute to update
	 * @param id The ID of the DefaultSettings object to update
	 * @param request
	 * @return 	0: successful,<br>
	 * 			1: parameter validation failed,<br>
	 * 			2: insufficient permissions 
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/edit/defaultSettings/{attr}/{id}")
	@Produces("application/json")
	public String editCommunityDefaultSettings(@PathParam("attr") String attribute, @PathParam("id") int id, @Context HttpServletRequest request) {	
		int userId=SessionUtil.getUserId(request);
		String newValue=(String)request.getParameter("val");
		ValidatorStatusCode status=SettingSecurity.canUpdateSettings(id,attribute,newValue, userId);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		try {			
			if(Util.isNullOrEmpty((String)request.getParameter("val"))){
				return gson.toJson(ERROR_EDIT_VAL_ABSENT);
			}
			
			boolean success = false;
			// Go through all the cases, depending on what attribute we are changing.
			if (attribute.equals("PostProcess")) {
				success = Settings.updateSettingsProfile(id, 1, Integer.parseInt(newValue));
			} else if (attribute.equals("BenchProcess")) {
				success = Settings.updateSettingsProfile(id,8,Integer.parseInt(newValue));
			}else if (attribute.equals("CpuTimeout")) {
				success = Settings.updateSettingsProfile(id, 2, Integer.parseInt(newValue));			
			}else if (attribute.equals("ClockTimeout")) {
				success = Settings.updateSettingsProfile(id, 3, Integer.parseInt(newValue));			
			} else if (attribute.equals("DependenciesEnabled")) {
				success = Settings.updateSettingsProfile(id, 4, Integer.parseInt(newValue));
			} else if (attribute.equals("defaultbenchmark")) {
				success=Settings.updateSettingsProfile(id, 5, Integer.parseInt(newValue));
			} else if (attribute.equals("defaultsolver")) {
				success=Settings.updateSettingsProfile(id, 7, Integer.parseInt(newValue));
			} else if(attribute.equals("MaxMem")) {
				double gigabytes=Double.parseDouble(newValue);
				long bytes = Util.gigabytesToBytes(gigabytes); 
				success=Settings.setDefaultMaxMemory(id, bytes);
			} else if (attribute.equals("PreProcess")) {
				success=Settings.updateSettingsProfile(id, 6, Integer.parseInt(newValue));
			}
			
			// Passed validation AND Database update successful
			return success ? gson.toJson(new ValidatorStatusCode(true,"Community edit successful")) : gson.toJson(ERROR_DATABASE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			return gson.toJson(ERROR_DATABASE);
		}
		
	}
	
	/** 
	 * Updates information for a space in the database using a POST. Attribute and
	 * new value are included in the path. First validates that the new value
	 * is legal, then updates the database and session information accordingly.
	 * @param attribute The name of the attribute being updated
	 * @param id The ID of the community being updated
	 * @param request
	 * @return 	0: successful,<br>
	 * 			1: parameter validation failed,<br>
	 * 			2: insufficient permissions 
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/edit/space/{attr}/{id}")
	@Produces("application/json")
	public String editCommunityDetails(@PathParam("attr") String attribute, @PathParam("id") int id, @Context HttpServletRequest request) {	
		int userId=SessionUtil.getUserId(request);
		String newValue=(String)request.getParameter("val");
		ValidatorStatusCode status=SpaceSecurity.canUpdateSettings(id,attribute,newValue, userId);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		try {			
			if(Util.isNullOrEmpty((String)request.getParameter("val"))){
				return gson.toJson(ERROR_EDIT_VAL_ABSENT);
			}
			
			boolean success = false;
			// Go through all the cases, depending on what attribute we are changing.
			if (attribute.equals("name")) {
				String newName = (String)request.getParameter("val");
				
				success = Spaces.updateName(id, newName);
				
			} else if (attribute.equals("description")) {
				String newDesc = (String)request.getParameter("val");
				success = Spaces.updateDescription(id, newDesc);				
				
			}
			
			// Passed validation AND Database update successful
			return success ? gson.toJson(new ValidatorStatusCode(true,"Community edit successful")) : gson.toJson(ERROR_DATABASE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			return gson.toJson(ERROR_DATABASE);
		}
		
	}

	/** Updates all details of a space in the database. Space id is included in the path.
	 * First makes sure all details exist and are valid, then checks if the user making
	 * the request is a leader of the space, then updates the space accordingly.
	 * @param id The ID of the space to update
	 * @param request
	 * @return a json string containing '0' if the update is successful, else a json string
	 * containing '1' if it is unsuccessful or a json string containing '2' if the current
	 * user doesn't have sufficient privileges.
	 * @author Skylar Stark
	 */
	@POST
	@Path("/edit/space/{id}")
	@Produces("application/json")
	public String editSpace(@PathParam("id") int id, @Context HttpServletRequest request) {
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("locked", request)
				|| !Util.paramExists("sticky", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
				
		// Permissions check; if user is NOT a leader of the space, deny update request
		int userId = SessionUtil.getUserId(request);
		
		
		// Extract new space details from request and add them to a new space object
		
		Space s = new Space();
		s.setId(id);
		s.setName(request.getParameter("name"));
		s.setDescription(request.getParameter("description"));
		s.setLocked(Boolean.parseBoolean(request.getParameter("locked")));
		s.setStickyLeaders(Boolean.parseBoolean(request.getParameter("sticky")));
		ValidatorStatusCode status=SpaceSecurity.canUpdateProperties(id, userId, s.getName(), s.isStickyLeaders());
		if(!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Extract permission details from request and add them to a new permission object
		// Then set the above space's permission to this new permission object
		Permission p = new Permission();
		p.setAddBenchmark(Boolean.parseBoolean(request.getParameter("addBench")));
		p.setAddJob(Boolean.parseBoolean(request.getParameter("addJob")));
		p.setAddSolver(Boolean.parseBoolean(request.getParameter("addSolver")));
		p.setAddSpace(Boolean.parseBoolean(request.getParameter("addSpace")));
		p.setAddUser(Boolean.parseBoolean(request.getParameter("addUser")));
		p.setRemoveBench(Boolean.parseBoolean(request.getParameter("removeBench")));
		p.setRemoveJob(Boolean.parseBoolean(request.getParameter("removeJob")));
		p.setRemoveSolver(Boolean.parseBoolean(request.getParameter("removeSolver")));
		p.setRemoveSpace(Boolean.parseBoolean(request.getParameter("removeSpace")));
		p.setRemoveUser(Boolean.parseBoolean(request.getParameter("removeUser")));
		p.setLeader(false);
		s.setPermission(p);
		
		// Perform the update and return information according to success/failure
		return Spaces.updateDetails(userId, s) ? gson.toJson(new ValidatorStatusCode(true,"Space edit successful")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Post-processes an already-complete job with a new post processor
	 * @param jid The ID of the job to process
	 * @param stageNumber The stage number to process across all pairs
	 * @param pid The ID of the new post processor to use.
	 * @param request
	 * @return a json string with result status (0 for success, otherwise 1)
	 * @author Eric Burns
	 */
	@POST
	@Path("/postprocess/job/{jobId}/{procId}/{stageNumber}")
	@Produces("application/json")
	public String postProcessJob(@PathParam("jobId") int jid,@PathParam("stageNumber") int stageNumber, @PathParam("procId") int pid, @Context HttpServletRequest request) {
		
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserPostProcessJob(jid, userId, pid,stageNumber);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		log.info("post process request with jobId = "+jid+" and processor id = "+pid);
		
		return Jobs.prepareJobForPostProcessing(jid,pid,stageNumber) ? gson.toJson(new ValidatorStatusCode(true,"Post processing started successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Deletes a list of processors
	 * @param request Contains a selectedIds array parameter with the processors to delete
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/delete/processor")
	@Produces("application/json")
	public String deleteProcessors(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedProcessors = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedProcessors.add(Integer.parseInt(id));
			log.debug("got a request to delete processor id = "+id);
		}
		ValidatorStatusCode status=ProcessorSecurity.doesUserOwnProcessors(selectedProcessors, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		for (int id : selectedProcessors) {
			if (!Processors.delete(id)) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(new ValidatorStatusCode(true,"Processors deleted successfully"));
	}
	
	/**
	 * Restores all recycled benchmarks a user has
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: database level error,<br>
	 * @author Eric Burns
	 */
	@POST
	@Path("/restorerecycled/benchmarks")
	@Produces("application/json")
	public String restoreRecycledBenchmarks(@Context HttpServletRequest request) {
		
		int userId=SessionUtil.getUserId(request);
		if (!Benchmarks.restoreRecycledBenchmarks(userId)) {
			return gson.toJson(ERROR_DATABASE);
		}
		
		return gson.toJson(new ValidatorStatusCode(true,"Benchmarks restored successfully"));
	}
	
	/**
	 * Restores all recycled solvers a user has
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: database level error,<br>
	 * @author Eric Burns
	 */
	@POST
	@Path("/restorerecycled/solvers")
	@Produces("application/json")
	public String restoreRecycledSolvers(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		if (!Solvers.restoreRecycledSolvers(userId)) {
			return gson.toJson(ERROR_DATABASE);
		}
		return gson.toJson(new ValidatorStatusCode(true,"Solvers restored successfully"));
	}
	
	
	/**
	 * Deletes all recycled benchmarks a user has
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: database level error,<br>
	 * @author Eric Burns
	 */
	@POST
	@Path("/deleterecycled/benchmarks")
	@Produces("application/json")
	public String setRecycledBenchmarksToDeleted(@Context HttpServletRequest request) {
		
		int userId=SessionUtil.getUserId(request);
		if (!Benchmarks.setRecycledBenchmarksToDeleted(userId)) {
			return gson.toJson(ERROR_DATABASE);
		}
		return gson.toJson(new ValidatorStatusCode(true,"Benchmarks deleted successfully"));
	}
	
	/**
	 * Deletes all recycled solvers a user has
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: database level error,<br>
	 * @author Eric Burns
	 */
	@POST
	@Path("/deleterecycled/solvers")
	@Produces("application/json")
	public String setRecycledSolversToDeleted(@Context HttpServletRequest request) {
		
		int userId=SessionUtil.getUserId(request);
		if (!Solvers.setRecycledSolversToDeleted(userId)) {
			return gson.toJson(ERROR_DATABASE);
		}
		return gson.toJson(new ValidatorStatusCode(true,"Solvers restored successfully"));
	}
	
	/**
	 * Handles an update request for a processor
	 * 
	 * @param pid The ID of the processor to update
	 * @param request
	 * @return a json string containing 0 for success, 1 for a server error,
	 *  2 for a permissions error, or 3 for a malformed request error.
	 * 
	 * @author Eric Burns
	 */
	
	@POST
	@Path("/edit/processor/{procId}")
	@Produces("application/json")
	public String editProcessor(@PathParam("procId") int pid, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		Processor p=Processors.get(pid);
		if(!Util.paramExists("name", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		String name=request.getParameter("name");
		String desc="";
		// Ensure the parameters are valid
		if (Util.paramExists("desc", request)) {
			desc=request.getParameter("desc");
		}
		ValidatorStatusCode status=ProcessorSecurity.canUserEditProcessor(pid, userId,name,desc);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		if (!p.getName().equals(name)) {
			boolean x=Processors.updateName(pid, name);
			if (!x) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		
		if (!p.getDescription().equals(desc)) {
			boolean x=Processors.updateDescription(pid, desc);
			if (!x) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		
		return gson.toJson(new ValidatorStatusCode(true,"Processor edited successfully"));
	}
	
	/**
	 * Removes a user's association to a space
	 * @param spaceId The Id of the space to leave.
	 * @param request
	 * @return a json string containing '0' if the user successfully left the
	 *         space, else a json string containing '1' if there was a failure,
	 *         '2' for insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/leave/space/{spaceId}")
	@Produces("application/json")
	public String leaveCommunity(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Permissions check; ensures user is apart of the community
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=SpaceSecurity.canUserLeaveSpace(spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		if(Spaces.leave(SessionUtil.getUserId(request), spaceId)) {
			// Delete prior entry in user's permissions cache for this community
			SessionUtil.removeCachePermission(request, spaceId);
			return gson.toJson(new ValidatorStatusCode(true,"Community left successfully"));
		}
		
		
		return gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes one or more benchmarks from the given space
	 * @param spaceId The ID of the space to remove benchmarks from
	 * @param request should have a selectedIds parameter containing an array of benchmarks tor emove
	 * @return	0: if the benchmark was successfully removed from the space,<br> 
	 * 			1: if there was a failure at the database level,<br>
	 * 			2: insufficient permissions
	 * @author 	Todd Elvers
	 */
	@POST
	@Path("/remove/benchmark/{spaceId}")
	@Produces("application/json")
	public String removeBenchmarksFromSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		
		ValidatorStatusCode status=SpaceSecurity.canUserRemoveBenchmark(spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		// Extract the String bench id's and convert them to Integer
		ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedBenches.add(Integer.parseInt(id));
		}
		
		
		// Remove the benchmark from the space
		return Spaces.removeBenches(selectedBenches, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"Benchmarks removed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Simultaneously removes benchmarks from the given space and recycles them
	 * @param spaceId The ID of the space to remove benchmarks from
	 * @param request should have a selectedIds parameter set with the array of benchmarkIds to consider
	 * @return	0: if the benchmark was successfully removed from the space,<br> 
	 * 			1: if there was a failure at the database level,<br>
	 * 			2: insufficient permissions
	 * @author 	Eric Burns
	 */
	@POST
	@Path("/recycleandremove/benchmark/{spaceID}")
	@Produces("application/json")
	public String recycleAndRemoveBenchmarks(@PathParam("spaceID") int spaceId,@Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String bench id's and convert them to Integer
		ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedBenches.add(Integer.parseInt(id));
		}
		int userId=SessionUtil.getUserId(request);
		
		ValidatorStatusCode status=SpaceSecurity.canUserRemoveAndRecycleBenchmarks(selectedBenches, spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		for (int id : selectedBenches) {			
			if (!Benchmarks.recycle(id)) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return Spaces.removeBenches(selectedBenches, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"Benchmarks successfully recycled and removed from spaces")) : gson.toJson(ERROR_DATABASE);
		
	}
	
	/**
	 * Recycles a list of benchmarks
	 * @param request
	 * @return	0: if the benchmarks were successfully recycled,<br> 
	 * 			1: if there was a failure at the database level,<br>
	 * 			2: insufficient permissions
	 * @author 	Eric Burns
	 */
	@POST
	@Path("/recycle/benchmark")
	@Produces("application/json")
	public String recycleBenchmarks(@Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String bench id's and convert them to Integer
		ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedBenches.add(Integer.parseInt(id));
		}
		int userId=SessionUtil.getUserId(request);
		//first, ensure the user has the correct permissions for every benchmark
		ValidatorStatusCode status=BenchmarkSecurity.canUserRecycleBenchmarks(selectedBenches,userId);
		if(!status.isSuccess()) {
			return gson.toJson(status);
		}
			
		
		//then, only if the user had the right permissions, start recycling them
		for (int id : selectedBenches) {
			boolean success=Benchmarks.recycle(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		
		return gson.toJson(new ValidatorStatusCode(true,"Benchmarks successfully recycled"));
	}
	
	/**
	 * Deletes a list of benchmarks
	 * @param request
	 * @return	0: if the benchmark was successfully removed from the space,<br> 
	 * 			1: if there was a failure at the database level,<br>
	 * 			2: insufficient permissions
	 * @author 	Eric Burns
	 */
	@POST
	@Path("/delete/benchmark")
	@Produces("application/json")
	public String deleteBenchmarks(@Context HttpServletRequest request) {
		try {
			// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
			if(null == request.getParameterValues("selectedIds[]")){
				return gson.toJson(ERROR_IDS_NOT_GIVEN);
			}
			
			
			// Extract the String bench id's and convert them to Integer
			ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedBenches.add(Integer.parseInt(id));
			}
			int userId=SessionUtil.getUserId(request);
			ValidatorStatusCode status=BenchmarkSecurity.canUserDeleteBenchmarks(selectedBenches, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			for (int id : selectedBenches) {
				boolean success=Benchmarks.delete(id);
				if (!success) {
					return gson.toJson(ERROR_DATABASE);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return gson.toJson(new ValidatorStatusCode(true,"Benchmarks successfully deleted"));
	}
	
	/**
	 * Restores a set of recycled benchmarks
	 * @param request Should contain a selectedIds parameter array of benchmark IDs to restore
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/restore/benchmark")
	@Produces("application/json")
	public String restoreBenchmarks(@Context HttpServletRequest request) {
		try {
			// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
			if(null == request.getParameterValues("selectedIds[]")){
				return gson.toJson(ERROR_IDS_NOT_GIVEN);
			}	
			// Extract the String bench id's and convert them to Integer
			ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedBenches.add(Integer.parseInt(id));
			}
			int userId=SessionUtil.getUserId(request);
			ValidatorStatusCode status=BenchmarkSecurity.canUserRestoreBenchmarks(selectedBenches, userId);
			if(!status.isSuccess()) {
				return gson.toJson(status);	
			}
	
			for (int id : selectedBenches) {
				boolean success=Benchmarks.restore(id);
				if (!success) {
					return gson.toJson(ERROR_DATABASE);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return gson.toJson(new ValidatorStatusCode(true,"Benchmarks successfully restored"));
	}
	
	
	
	/**
	 * Adds users to the given space
	 * 
	 * @param	spaceId the id of the destination space we are copying to
	 * @param	request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of users to copy as well as a 'fromSpace' parameter that is the
	 * space the users are being copied from.
	 * @return 	a ValidatorStatusCode object
	 * @author Tyler Jensen & Todd Elvers
	 */
	@POST
	@Path("/spaces/{spaceId}/add/user")
	@Produces("application/json")
	public String addUsersToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of users to add, the id of the space it's coming from, and whether or not to apply this to all subspaces 
		if(null == request.getParameterValues("selectedIds[]") 
				
				|| !Util.paramExists("copyToSubspaces", request)
				|| !Validator.isValidBool(request.getParameter("copyToSubspaces"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		
		// Get the flag that indicates whether or not to copy this solver to all subspaces of 'fromSpace'
		boolean copyToSubspaces = Boolean.parseBoolean(request.getParameter("copyToSubspaces"));
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		
		ValidatorStatusCode status=SpaceSecurity.canCopyUserBetweenSpaces(spaceId, requestUserId, selectedUsers, copyToSubspaces);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		return Users.associate(selectedUsers, spaceId,copyToSubspaces,requestUserId) ? gson.toJson(new ValidatorStatusCode(true,"User(s) moved successfully")) : gson.toJson(ERROR_DATABASE);
	
	}

	/**
	 * Associates (i.e. 'copies') solvers from one space into another space and, if specified by the client,
	 * to all the subspaces of the destination space
	 * 
	 * @param spaceId the id of the destination space we are copying to
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of solvers to copy as well as a 'fromSpace' parameter that is the
	 * space the solvers are being copied from, and a boolean 'copyToSubspaces' parameter indicating whether or not the solvers
	 * should be added to the subspaces of the destination space
	 * @param response On success, will have a "New_ID" cookie set with IDs of the new solvers
	 * @return  0: success,<br> 
	 * 			1: database failure,<br> 
	 * 			2: missing parameters,<br> 
	 * 			3: no add permission in destination space,<br>
	 * 			4: user doesn't belong to the 'from space',<br> 
	 * 			5: the 'from space' is locked,<br>
	 * 			6: user does not belong to one or more of the subspaces of the destination space,<br>
	 *          7: there exists a primitive with the same name
	 * @author Tyler Jensen & Todd Elvers
	 */

	@POST
	@Path("/spaces/{spaceId}/add/solver")
	@Produces("application/json")
	public String copySolversToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request,@Context HttpServletResponse response) {
		log.debug("entering the copy function");
		try {
			// Make sure we have a list of solvers to add, the id of the space it's coming from, and whether or not to apply this to all subspaces 
			if(null == request.getParameterValues("selectedIds[]") 
					|| !Util.paramExists("copyToSubspaces", request)
					|| !Util.paramExists("copy", request)
					|| !Validator.isValidBool(request.getParameter("copyToSubspaces"))
					|| !Validator.isValidBool(request.getParameter("copy"))){
				return gson.toJson(ERROR_INVALID_PARAMS);
			}
			
			// Get the id of the user who initiated the request
			int requestUserId = SessionUtil.getUserId(request);
			
			// Get the space the solver is being copied from
			String fromSpace = request.getParameter("fromSpace");
			log.debug("fromSpace: " + fromSpace);
			Integer fromSpaceId=null;
			//if null, we are not copying from anywhere-- we are just putting a solver into a new space
			if (fromSpace!=null) {
				fromSpaceId=Integer.parseInt(fromSpace);
			}
			// Get the flag that indicates whether or not to copy this solver to all subspaces of 'fromSpace'
			boolean copyToSubspaces = Boolean.parseBoolean(request.getParameter("copyToSubspaces"));
			
			//Get the flag that indicates whether the solver is being copied or linked
			boolean copy=Boolean.parseBoolean(request.getParameter("copy"));
			// Convert the solvers to copy to an int list
			List<Integer> selectedSolvers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
			
			
			ValidatorStatusCode status=SpaceSecurity.canCopyOrLinkSolverBetweenSpaces(fromSpaceId, spaceId, requestUserId, selectedSolvers, copyToSubspaces, copy);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			if (copy) {
				List<Solver> oldSolvers=Solvers.get(selectedSolvers);
				List<Integer>newSolverIds=new ArrayList<Integer>();
				newSolverIds=Solvers.copySolvers(oldSolvers, requestUserId, spaceId);
				selectedSolvers=newSolverIds;
				response.addCookie(new Cookie("New_ID", Util.makeCommaSeparatedList(selectedSolvers)));		
			}
			
			//if we did a copy, the solvers are already associated with the root space, so we don't need to link to that one
			return Solvers.associate(selectedSolvers, spaceId,copyToSubspaces,requestUserId,!copy) ? gson.toJson(new ValidatorStatusCode(true,"Solver(s) moved successfully")) : gson.toJson(ERROR_DATABASE);
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Associates (i.e. 'copies') a benchmark from one space into another
	 * 
	 * @param spaceId the id of the destination space we are copying to
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of benchmarks to copy as well as a 'fromSpace' parameter that is the
	 * space the benchmarks are being copied from.
	 * @param response A New_ID cookie is attached for the new benchmarks
	 * @return 	0: success,<br>
	 * 			1: database failure,<br>
	 * 			2: missing parameters,<br>
	 * 			3: no add user permission in destination space,<br>
	 * 			4: user doesn't belong to the 'from space',<br>
	 * 			5: the 'from space' is locked,<br>
	 *          6: there exists a primitive with the same name
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/benchmark")
	@Produces("application/json")
	public String copyBenchToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request, @Context HttpServletResponse response) {
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") 
				|| !Util.paramExists("copy", request)
				|| !Validator.isValidBool(request.getParameter("copy"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		String fromSpace = request.getParameter("fromSpace");
		
		Integer fromSpaceId=null;
		if (fromSpace!=null) {
			fromSpaceId=Integer.parseInt(fromSpace);
		}
	
		// Convert the benchmarks to copy to a int list
		List<Integer> selectedBenchs= Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		boolean copy=Boolean.parseBoolean(request.getParameter("copy"));

		ValidatorStatusCode status=SpaceSecurity.canCopyOrLinkBenchmarksBetweenSpaces(fromSpaceId, spaceId, requestUserId, selectedBenchs, copy);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		if (copy) {
			List<Benchmark> oldBenchs=Benchmarks.get(selectedBenchs,true);
			List<Integer> benches=Benchmarks.copyBenchmarks(oldBenchs, requestUserId, spaceId);	
			response.addCookie(new Cookie("New_ID", Util.makeCommaSeparatedList(benches)));

			
			return gson.toJson(new ValidatorStatusCode(true,"The selected benchmark(s) were copied successfully"));
		} else {
			// Return a value based on results from database operation
			return Benchmarks.associate(selectedBenchs, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"The selected benchmark(s) were linked successfully")) : gson.toJson(ERROR_DATABASE);
		}
	}
	
	/**
	 * Associates (i.e. 'copies') a job from one space into another
	 * 
	 * @param spaceId the id of the destination space we are copying to
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of jobs to copy as well as a 'fromSpace' parameter that is the
	 * space the jobs are being copied from.
	 * @return 	0: success,<br>
	 * 			1: database failure,<br>
	 * 			2: missing parameters,<br>
	 * 			3: no add user permission in destination space,<br>
	 * 			4: user doesn't belong to the 'from space',<br>
	 * 			5: the 'from space' is locked
	 *          6. there exists a primitive with the same name
	 * @author Tyler Jensen
	 */
	
	//TODO: Resume testing here
	@POST
	@Path("/spaces/{spaceId}/add/job")
	@Produces("application/json")
	public String copyJobToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
				
		// Get the space the benchmark is being copied from
		String fromSpace = request.getParameter("fromSpace");
		Integer fromSpaceId=null;	
		if (fromSpace!=null) {
			fromSpaceId=Integer.parseInt(fromSpace);
		}
		List<Integer> selectedJobs = Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		ValidatorStatusCode status=SpaceSecurity.canLinkJobsBetweenSpaces(fromSpaceId, spaceId, userId, selectedJobs);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		// Make the associations
		boolean success = Jobs.associate(selectedJobs, spaceId);
		
		// Return a value based on results from database operation
		return success ? gson.toJson(new ValidatorStatusCode(true,"Job(s) moved successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes users' associations with a space, whereby removing them from a
	 * space; this differs from leaveCommunity() in that the user is not allowed
	 * to remove themselves from a space or remove other leaders from a space
	 * @param spaceId The ID of the space to remove users from
	 * @param request
	 * @return 	0: if the user(s) were successfully removed from the space,<br>
	 * 			1: if there was an error on the database level,<br>
	 * 			3: if the leader initiating the removal is in the list of users to remove,<br>
	 * 			4: if the list of users t remove contains another leader of the space
	 * @author Todd Elvers & Skylar Stark
	 */
	@POST
	@Path("/remove/user/{spaceId}")
	@Produces("application/json")
	public String removeUsersFromSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		log.debug("removing user from space");
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}		
		
		// Get the id of the user who initiated the removal
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		
		
		// Extract the String user id's and convert them to Integer
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		boolean hierarchy=Boolean.parseBoolean(request.getParameter("hierarchy"));
		
		ValidatorStatusCode status=SpaceSecurity.canRemoveUsersFromSpaces(selectedUsers, userIdOfRemover, spaceId, hierarchy);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// If we are "cascade removing" the user(s)...
		if (hierarchy) {
			List<Space> subspaces = Spaces.trimSubSpaces(userIdOfRemover, Spaces.getSubSpaceHierarchy(spaceId, userIdOfRemover));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces remove the user from
			subspaceIds.add(spaceId);
			
			// Iterate once through all subspaces of the destination space to ensure the user has removeUser permissions in each
			for(Space subspace : subspaces) {		
				subspaceIds.add(subspace.getId());
			}

			// Remove the users from the space and its subspaces
			return Spaces.removeUsersFromHierarchy(selectedUsers, subspaceIds) ? gson.toJson(new ValidatorStatusCode(true,"User(s) removed successfully")) : gson.toJson(ERROR_DATABASE);
		}
		
		// Otherwise...
		return Spaces.removeUsers(selectedUsers, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"User(s) removed successfully")) : gson.toJson(ERROR_DATABASE);
	}

	/**
	 * Removes a solver's association with a space, thereby removing the solver
	 * from the space
	 * @param spaceId The ID of the space to remove solvers from
	 * @param request Should have a selectedIds parameter array containing solver IDs to remove
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Todd Elvers & Skylar Stark
	 */
	@POST
	@Path("/remove/solver/{spaceId}")
	@Produces("application/json")
	public String removeSolversFromSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		// If we are "cascade removing" the solver(s)...
		if (true == Boolean.parseBoolean(request.getParameter("hierarchy"))) {			
			
			ValidatorStatusCode status=SolverSecurity.canUserRemoveSolverFromHierarchy(spaceId,userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
		
			return Spaces.removeSolversFromHierarchy(selectedSolvers, spaceId,userId) ? gson.toJson(new ValidatorStatusCode(true,"Solver(s) removed successfully")) : gson.toJson(ERROR_DATABASE);

		} else {
			// Permissions check; ensures user has permisison to remove solver
			ValidatorStatusCode status=SolverSecurity.canUserRemoveSolver(spaceId, SessionUtil.getUserId(request));
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return Spaces.removeSolvers(selectedSolvers, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"Solver(s) removed successfully")) : gson.toJson(ERROR_DATABASE);

		}		
	}
	
	
	/**
	 * Recycles a list of solvers and removes them  from the given space
	 * @param spaceId ID of space to remove solvers from
	 * @param request should contain a selectedIds parameter containing an array of solver ids
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/recycleandremove/solver/{spaceID}")
	@Produces("application/json")
	public String recycleAndRemoveSolvers(@Context HttpServletRequest request, @PathParam("spaceID") int spaceId) {
		int userId = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		ValidatorStatusCode status=SpaceSecurity.canUserRemoveAndRecycleSolvers(selectedSolvers,spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		for (int id : selectedSolvers) {
			boolean success=Solvers.recycle(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return Spaces.removeSolvers(selectedSolvers, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"Solver(s) successfully recycled and removed from spaces")) : gson.toJson(ERROR_DATABASE);
		
	}
	
	/**
	 * Restores a list of solvers
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/restore/solver")
	@Produces("application/json")
	public String restoreSolvers(@Context HttpServletRequest request) {
		try {
			int userId = SessionUtil.getUserId(request);
			
			// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
			if(null == request.getParameterValues("selectedIds[]")){
				return gson.toJson(ERROR_IDS_NOT_GIVEN);
			}

			// Extract the String solver id's and convert them to Integer
			ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedSolvers.add(Integer.parseInt(id));
			}

			ValidatorStatusCode status=SolverSecurity.canUserRestoreSolvers(selectedSolvers, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}

			for (int id : selectedSolvers) {
				
				boolean success=Solvers.restore(id);
				if (!success) {
					return gson.toJson(ERROR_DATABASE);
				}
			}
			return gson.toJson(new ValidatorStatusCode(true,"Solver(s) restored successfully"));
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return gson.toJson(ERROR_DATABASE);
		
	}

	/**
	 * Permanently deletes a user from the system. This is an admin-only function
	 * @param userToDeleteId The id of the user to be deleted.
	 * @param request
	 * @return json ValidatorStatusCode
	 * @author Albert Giegerich
	 */
	@POST
	@Path("/delete/user/{userId}")
	@Produces("application/json")
	public String deleteUser(@PathParam("userId") int userToDeleteId, @Context HttpServletRequest request) {

		int callersUserId = SessionUtil.getUserId(request);

		boolean success = false;
		
		//Only allow the deletion of non-admin users, and only if the admin is asking
		ValidatorStatusCode status = UserSecurity.canDeleteUser(userToDeleteId, callersUserId);
		if (!status.isSuccess()) {
			log.debug("security permission error when trying to delete user with id = "+userToDeleteId);
			return gson.toJson(status);
		}
		
		success = Users.deleteUser(userToDeleteId);
		if (success) {
			return gson.toJson(new ValidatorStatusCode(true, "The user has been successfully deleted."));
		} else {
			return gson.toJson(new ValidatorStatusCode(false, "An internal error occurred while attempting to delete the user."));
		}
	}
	
	/**
	 * Deletes a list of solvers
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/delete/solver")
	@Produces("application/json")
	public String deleteSolvers(@Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		ValidatorStatusCode status=SolverSecurity.canUserDeleteSolvers(selectedSolvers, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		for (int id : selectedSolvers) {
			boolean success=Solvers.delete(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(new ValidatorStatusCode(true,"Solver(s) deleted successfully"));
	}
	
	/**
	 * Links all of the given user's orphaned primitives to the given space
	 * @param userId The ID of the user that will have their primitives affected
	 * @param spaceId The ID of the space to put the primitives in
	 * @param request 
	 * @return json ValidatorStatuscode
	 */
	@POST
	@Path("/linkAllOrphaned/{userId}/{spaceId}")
	@Produces("application/json")
	public String linkAllOrphanedPrimitives(@PathParam("userId") int userId, @PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		int userIdOfCaller = SessionUtil.getUserId(request);

		ValidatorStatusCode status=SpaceSecurity.canUserLinkAllOrphaned(userId, userIdOfCaller, spaceId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
	
		return Spaces.addOrphanedPrimitivesToSpace(userId, spaceId) ?  gson.toJson(new ValidatorStatusCode(true,"Primitives linked successfully")) :
			gson.toJson(new ValidatorStatusCode(false, "Internal database error linking primitives"));
	}
	
	/**
	 * Recycles all benchmarks that have been orphaned belonging to a specific user. Users have this option
	 * from their account page
	 * @param userId The Id of the user to recycle benchmarks for.
	 * @param request
	 * @return json ValidatorStatusCode
	 * @author Eric Burns
	 */
	@POST
	@Path("/recycleOrphaned/benchmark/{userId}")
	@Produces("application/json")
	public String recycleOrphanedBenchmarks(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int userIdOfCaller = SessionUtil.getUserId(request);

		
		ValidatorStatusCode status=BenchmarkSecurity.canUserRecycleOrphanedBenchmarks(userId, userIdOfCaller);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
	
		return Benchmarks.recycleOrphanedBenchmarks(userId) ?  gson.toJson(new ValidatorStatusCode(true,"Benchmark(s) recycled successfully")) :
			gson.toJson(new ValidatorStatusCode(false, "Internal database error recycling benchmark(s)"));
	}
	
	/**
	 * Recycles all solvers that have been orphaned belonging to a specific user
	 * @param userId the ID of the user to recycle solvers for
	 * @param request
	 * @return json ValidatorStatusCode
	 * @author Eric Burns
	 */
	@POST
	@Path("/recycleOrphaned/solver/{userId}")
	@Produces("application/json")
	public String recycleOrphanedSolvers(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int userIdOfCaller = SessionUtil.getUserId(request);

		
		ValidatorStatusCode status=SolverSecurity.canUserRecycleOrphanedSolvers(userId, userIdOfCaller);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
	
		return Solvers.recycleOrphanedSolvers(userId) ?  gson.toJson(new ValidatorStatusCode(true,"Solver(s) recycled successfully")) :
			gson.toJson(new ValidatorStatusCode(false, "Internal database error recycling solver(s)"));
	}
	
	
	/**
	 * Recycles a list of solvers
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/recycle/solver")
	@Produces("application/json")
	public String recycleSolvers(@Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		ValidatorStatusCode status=SolverSecurity.canUserRecycleSolvers(selectedSolvers, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		for (int id : selectedSolvers) {
			if (!Solvers.recycle(id)) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(new ValidatorStatusCode(true,"Solver(s) recycled successfully"));
	}
	
	/**
	 * Deletes a list of configurations
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/delete/configuration")
	@Produces("application/json")
	public String deleteConfigurations(@Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		int userId=SessionUtil.getUserId(request);
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedConfigs = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedConfigs.add(Integer.parseInt(id));
		}
		ValidatorStatusCode status=SolverSecurity.canUserDeleteConfigurations(selectedConfigs, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		for (int id : selectedConfigs) {
			// Validate configuration id parameter
			Configuration config = Solvers.getConfiguration(id);
			if(null == config){
				return gson.toJson(ERROR_DATABASE);
			}
			
			// Permissions check; if user is NOT the owner of the configuration file's solver, deny deletion request
			Solver solver = Solvers.get(config.getSolverId());
			
			
			// Attempt to remove the configuration's physical file from disk
			if(!Solvers.deleteConfigurationFile(config)){
				return gson.toJson(ERROR_DATABASE);
			}
			
			// Attempt to remove the configuration's entry in the database
			if(!Solvers.deleteConfiguration(id)){
				return gson.toJson(ERROR_DATABASE);
			}
			
			// Attempt to update the disk_size of the parent solver to reflect the file deletion
			if(!Solvers.updateSolverDiskSize(solver)){
				return gson.toJson(ERROR_DATABASE);
			}
		}
		
		
		return gson.toJson(new ValidatorStatusCode(true,"Configuration(s) deleted successfully"));
	}
	
	/**
	 * Removes a job's association with a space, thereby removing the job from
	 * the space
	 * @param spaceId The ID of the space to remove jobs from
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/remove/job/{spaceId}")
	@Produces("application/json")
	public String removeJobsFromSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String job id's and convert them to Integer
		ArrayList<Integer> selectedJobs = new ArrayList<Integer>();
		for (String id : request.getParameterValues("selectedIds[]")) {
			selectedJobs.add(Integer.parseInt(id));
		}

		ValidatorStatusCode status=SpaceSecurity.canUserRemoveJob(spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}

		// Remove the job from the space
		return Spaces.removeJobs(selectedJobs, spaceId) ? gson.toJson(new ValidatorStatusCode(true,"Job(s) removed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Deletes a list of jobs
	 * @param spaceId The ID of the space containing the solvers to remove
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/deleteandremove/job/{spaceID}")
	@Produces("application/json")
	public String deleteAndRemoveJobs(@Context HttpServletRequest request, @PathParam("spaceID") int spaceId) {
		int userId=SessionUtil.getUserId(request);
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		
		// Extract the String job id's and convert them to Integer
		ArrayList<Integer> selectedJobs = new ArrayList<Integer>();
		for (String id : request.getParameterValues("selectedIds[]")) {
			selectedJobs.add(Integer.parseInt(id));
			log.debug("adding id = "+id+" to selectedJobs");
		}
		
		ValidatorStatusCode status=SpaceSecurity.canUserRemoveAndDeleteJobs(selectedJobs, spaceId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		for (int id : selectedJobs) {
			
			log.debug("the current job ID to remove = "+id);
			
			boolean success_delete = Jobs.delete(id);
			if (!success_delete) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		Spaces.removeJobs(selectedJobs, spaceId);
		return gson.toJson(new ValidatorStatusCode(true,"Job(s) deleted successfully and removed from spaces"));
	}

	/**
	 * Deletes a list of jobs
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: database level error,<br>
	 * 			2: insufficient permissions
	 * @author Eric Burns
	 */
	@POST
	@Path("/delete/job")
	@Produces("application/json")
	public String deleteJobs(@Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String job id's and convert them to Integer
		ArrayList<Integer> selectedJobs = new ArrayList<Integer>();
		for (String id : request.getParameterValues("selectedIds[]")) {
			selectedJobs.add(Integer.parseInt(id));
		}
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserDeleteJobs(selectedJobs, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		for (int id : selectedJobs) {
			boolean success_delete = Jobs.delete(id);
			if (!success_delete) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
	
		return gson.toJson(new ValidatorStatusCode(true,"Job(s) deleted successfully"));
	}
	
	
	/**
	 * Removes a subspace's association with a space, thereby removing the subspace
	 * from the space
	 * @param request Should contain a selectedIds parameter containing an array of spaceIds
	 * @return 	0: success,<br>
	 * 			1: invalid parameters,<br>
	 * 			2: insufficient permissions,<br>
	 * 			3: error on the database level
	 * @author Todd Elvers
	 */
	@POST
	@Path("/remove/subspace")
	@Produces("application/json")
	public String removeSubspacesFromSpace(@Context HttpServletRequest request) {
			
		final int userId=SessionUtil.getUserId(request);
		final ArrayList<Integer> selectedSubspaces = new ArrayList<Integer>();
				
		try{
			// Extract the String subspace id's and convert them to Integers
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedSubspaces.add(Integer.parseInt(id));
			}
		} catch(Exception e){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		log.debug("found the following spaces");
		for (Integer i : selectedSubspaces) {
			log.debug(i);
		}
		ValidatorStatusCode status=SpaceSecurity.canUserRemoveSpace(userId,selectedSubspaces);
		if (!status.isSuccess()) {
			log.debug("fail: here is the error code = "+status.getMessage());
			return gson.toJson(status);
		}
		
		// Fork a new thread to delete the subspaces so the user's browser doesn't hang.
		Runnable removeSubspacesProcess = new Runnable() {
			@Override
			public void run() {
				try {
					boolean recycleAllAllowed=false;
					if (Util.paramExists("recyclePrims", request)) {
						if (Boolean.parseBoolean(request.getParameter("recyclePrims"))) {
							log.debug("Request to delete all solvers and benchmarks in a hierarchy received");
							recycleAllAllowed=true;
						}
					}
					Set<Solver> solvers=new HashSet<Solver>();
					Set<Benchmark> benchmarks=new HashSet<Benchmark>();
					if (recycleAllAllowed) {
						for (int sid : selectedSubspaces) {
							solvers.addAll(Solvers.getBySpace(sid));
							benchmarks.addAll(Benchmarks.getBySpace(sid));
							for (Space s : Spaces.getSubSpaceHierarchy(sid)) {
								solvers.addAll(Solvers.getBySpace(s.getId()));
								benchmarks.addAll(Benchmarks.getBySpace(s.getId()));
							}
						}
					}
					log.debug("found the following benchmarks");
					for (Benchmark b : benchmarks) {
						log.debug(b.getId());
					}
					// Remove the subspaces from the space
					boolean success=true;
					if (Spaces.removeSubspaces(selectedSubspaces)) {
						if (recycleAllAllowed) {
							log.debug("Space removed successfully, recycling primitives");
							success=success && Solvers.recycleSolversOwnedByUser(solvers, userId);
							success= success && Benchmarks.recycleAllOwnedByUser(benchmarks, userId);
						}
					}
				} catch (Exception e) {
					log.warn("Error occurred while removing subspaces.", e);
				}
			}
		};
		Util.threadPoolExecute(removeSubspacesProcess);
		return gson.toJson(new ValidatorStatusCode(true, "Subspaces are being deleted."));
	}
	/**
	 * Updates the details of a solver. Solver id is required in the path. First
	 * checks if the parameters of the update are valid, then performs the
	 * update.
	 * 
	 * @param solverId the id of the solver to update the details for
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions,<br>
	 * 			3: invalid parameters
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/solver/{id}")
	@Produces("application/json")
	public String editSolverDetails(@PathParam("id") int solverId, @Context HttpServletRequest request) {
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("downloadable", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidBool(request.getParameter("downloadable"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		
		String description="";
		if (Util.paramExists("description", request)) {
			description = request.getParameter("description");

		}
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		String name = request.getParameter("name");
		// Permissions check; if user is NOT the owner of the solver, deny update request
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=SolverSecurity.canUserUpdateSolver(solverId, name, description, isDownloadable, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Apply new solver details to database
		return Solvers.updateDetails(solverId, name, description, isDownloadable) ? gson.toJson(new ValidatorStatusCode(true,"Solver edited successfully")) : gson.toJson(ERROR_DATABASE);
	}

	
	
	
	/**
	 * Pauses a job given a job's id. The id of the job to pause must
	 * be included in the path.
	 * 
	 * @param jobId the id of the job to pause
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/pause/job/{id}")
	@Produces("application/json")
	public String pauseJob(@PathParam("id") int jobId, @Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the job, deny pause request
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserPauseJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		return Jobs.pause(jobId) ? gson.toJson(new ValidatorStatusCode(true,"Job paused successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Resumes a job given a job's id. The id of the job to resume must
	 * be included in the path.
	 * 
	 * @param jobId the id of the job to resume
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/resume/job/{id}")
	@Produces("application/json")
	public String resumeJob(@PathParam("id") int jobId, @Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the job, deny resume request
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserResumeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Jobs.resume(jobId) ? gson.toJson(new ValidatorStatusCode(true,"Job resumed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * changes a queue for a job given a job's id. 
	 * 
	 * @param jobId the id of the job to resume
	 * @param queueId the id of the queue to change to
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/changeQueue/job/{id}/{queueid}")
	@Produces("application/json")
	public String changeQueueJob(@PathParam("id") int jobId, @PathParam("queueid") int queueId, @Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the job, deny resume request
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canChangeQueue(jobId, userId,queueId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Jobs.changeQueue(jobId, queueId) ? gson.toJson(new ValidatorStatusCode(true,"Queue changed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	//TODO: Seems like we aren't actually running the given processor on the benchmark -- we probably should
	/**
	 * Edits the properties of the given benchmark
	 * 
	 * @param benchId the id of the benchmark to delete
	 * @param request
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions,<br>
	 * 			3: invalid parameters
	 *          4: there exist a primitive with the same name
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/benchmark/{benchId}")
	@Produces("application/json")
	public String editBenchmarkDetails(@PathParam("benchId") int benchId, @Context HttpServletRequest request) {
		boolean isValidRequest = true;
		int type = -1;
		
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("downloadable", request)
				|| !Util.paramExists("type", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Safely extract the type
		try{
			log.debug("typing error");
			type = Integer.parseInt(request.getParameter("type"));
		} catch (NumberFormatException nfe){
			isValidRequest = false;
		}
		if(false == isValidRequest){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidBool(request.getParameter("downloadable"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		int userId = SessionUtil.getUserId(request);
		String name = request.getParameter("name");
		
		// Extract new benchmark details from request
		
		String description = "";
		if (Util.paramExists("description", request)) {
			description = request.getParameter("description");

		}
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));

		ValidatorStatusCode status=BenchmarkSecurity.canUserEditBenchmark(benchId,name,description,type,userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		String processorString = "";
		Benchmark b = Benchmarks.get(benchId);
		final Integer benchType = type;
		// means we need to reprocess this benchmark
		if (b.getType().getId()!=type) {
			log.debug("executing new processor on benchmark");
			List<Benchmark> bench = new ArrayList<Benchmark>();
			bench.add(Benchmarks.get(benchId));
			Util.threadPoolExecute(new Runnable() {
				@Override
				public void run(){
					try {
						Benchmarks.attachBenchAttrs(bench, Processors.get(benchType), null);
						Benchmarks.addAttributeSetToDbIfValid(bench.get(0).getAttributes(), bench.get(0), null);
					} catch (Exception e) {
						log.error(e.getMessage(),e);
					}
					
				}
			});	
			
			
			processorString=". Benchmark is being processed with the new processor";
		}
		// Apply new benchmark details to database
		return Benchmarks.updateDetails(benchId, name, description, isDownloadable, type) ? gson.toJson(new ValidatorStatusCode(true,"Benchmark edited successfully"+processorString)) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Updates the current user's password. First verifies that it is in
	 * the correct format, then hashes is and updates it to the database.
	 * @param userId The ID of the user to update
	 * @param request Should contain parameters 'current' 'newpass' 'confirm' containing old password and new password twice
	 * @return 0 if the whole update was successful, 1 if the database operation 
	 * was unsuccessful, 2 if the new password failed validation, 3 if the new 
	 * password did not match the confirm password, or 4 if the current password \
	 * did not match the password in the database.
	 * @author Skylar Stark
	 */
	@POST
	@Path("/edit/user/password/{userId}")
	@Produces("application/json")
	public String editUserPassword(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int userIdOfCaller = SessionUtil.getUserId(request);
		String currentPass = request.getParameter("current");
		String newPass = request.getParameter("newpass");
		String confirmPass = request.getParameter("confirm");
		
		ValidatorStatusCode status=GeneralSecurity.canUserUpdatePassword(userId,userIdOfCaller,currentPass,newPass,confirmPass);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}

		//updatePassword requires the plaintext password
		if (Users.updatePassword(userId, newPass)) {
			return gson.toJson(new ValidatorStatusCode(true,"Password edited successfully"));
		} else {
			return gson.toJson(ERROR_DATABASE); //Database operation returned false
		}

	}
	
    /**
     * helper function for editing permissions
     * @param request
     * @return Permission object generated from the http request.

     **/

    public Permission createPermissionFromRequest(HttpServletRequest request){
		Permission newPerm = new Permission(false);
		newPerm.setAddBenchmark(Boolean.parseBoolean(request.getParameter("addBench")));		
		newPerm.setRemoveBench(Boolean.parseBoolean(request.getParameter("removeBench")));
		newPerm.setAddSolver(Boolean.parseBoolean(request.getParameter("addSolver")));	
		newPerm.setRemoveSolver(Boolean.parseBoolean(request.getParameter("removeSolver")));
		newPerm.setAddJob(Boolean.parseBoolean(request.getParameter("addJob")));	
		newPerm.setRemoveJob(Boolean.parseBoolean(request.getParameter("removeJob")));
		newPerm.setAddUser(Boolean.parseBoolean(request.getParameter("addUser")));		
		newPerm.setRemoveUser(Boolean.parseBoolean(request.getParameter("removeUser")));
		newPerm.setAddSpace(Boolean.parseBoolean(request.getParameter("addSpace")));	
		newPerm.setRemoveSpace(Boolean.parseBoolean(request.getParameter("removeSpace")));
		newPerm.setLeader(Boolean.parseBoolean(request.getParameter("isLeader")));	
	return newPerm;
    }
    
    /**
     * Changes the permissions of a given user for a space hierarchy
     * @param spaceId The ID of the root space of the hierarchy
     * @param userId the ID of the user to update permissions for
     * @param request
     * @return json ValidatorStatusCode
     *@author Julio Cervantes
     *
     **/
    @POST
	@Path("/space/{spaceId}/edit/perm/hier/{userId}")
	@Produces("application/json")
	public String editUserPermissionsHier(@PathParam("spaceId") int spaceId, @PathParam("userId") int userId, @Context HttpServletRequest request){

	    // Ensure the user attempting to edit permissions is a leader
	    int currentUserId = SessionUtil.getUserId(request);

	    List<Integer> permittedSpaces = SpaceSecurity.getUpdatePermissionSpaces(spaceId, userId, currentUserId);
	    log.info("permittedSpaces: " + permittedSpaces);

	    // Configure a new permission object
	    Permission newPerm = createPermissionFromRequest(request);			
	    

	    // Update database with new permissions
	    for(Integer permittedSpaceId : permittedSpaces){
			if(permittedSpaceId != null){
			    Permissions.set(userId,permittedSpaceId.intValue(),newPerm);
			}
	    }

	    return gson.toJson(new ValidatorStatusCode(true,"Permissions edited successfully"));
    }

    
	/**
	 * Changes the permissions of a given user for a given space
	 * @param spaceId The ID of the space to update permissions for
	 * @param userId The ID of the user to update permissions for
	 * @param request
	 * @return 0 if the permissions were successfully changed,<br>
	 * 		1 if there was an error on the database level,<br>
	 * 		2 if the user changing the permissions isn't a leader,<br>
	 * 		3 if the user whos permissions are to be changed is a leader
	 * @author Todd Elvers
	 */
	@POST
	@Path("/space/{spaceId}/edit/perm/{userId}")
	@Produces("application/json")
	public String editUserPermissions(@PathParam("spaceId") int spaceId, @PathParam("userId") int userId, @Context HttpServletRequest request) {
		// Ensure the user attempting to edit permissions is a leader
		int currentUserId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=SpaceSecurity.canUpdatePermissions(spaceId, userId, currentUserId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Configure a new permission object
		Permission newPerm = createPermissionFromRequest(request);
				
		
		// Update database with new permissions
		return Permissions.set(userId, spaceId, newPerm) ? gson.toJson(new ValidatorStatusCode(true,"Permissions edited successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Updates a configuration's name, description, and contents. Note: Updating of the
	 * name or contents modifies the actual configuration file.
	 *
	 * @param configId the id of the configuration file to update
	 * @param request the HttpServletRequest object containing the new configuration's name,
	 * description and contents
	 * @return 0 if the configuration was successfully updated,<br>
	 * 		1 if there was an error on the database level,<br>
	 * 		2 if the user has insufficient privileges to edit the configuration,<br> 
	 * 		3 if the parameters are invalid or don't exist<br>
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/configuration/{id}")
	@Produces("application/json")
	public String editConfigurationDetails(@PathParam("id") int configId, @Context HttpServletRequest request) {
		
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Permissions check; if user is NOT the owner of the configuration file's solver, deny update request
		int userId = SessionUtil.getUserId(request);
		
		
			
		// Extract new configuration file details from request
		String name = (String) request.getParameter("name");
		String description="";
		if (Util.paramExists("description", request)) {
			description = (String)request.getParameter("description");
		}
		
		ValidatorStatusCode status=SolverSecurity.canUserUpdateConfiguration(configId,userId,name,description);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Apply new solver details to database
		return Solvers.updateConfigDetails(configId, name, description) ? gson.toJson(new ValidatorStatusCode(true,"Configuration edited successfully")) : gson.toJson(ERROR_DATABASE);
	}
	

	/**
	 * Make a list of users the leaders of a space. This is an admin only function
	 * @param spaceId The Id of the space  
	 * @param request The HttpRequestServlet object containing the list of user's Id
	 * @return 0: Success.
	 *         1: Selected userId list is empty.
	 *         2: User making this request is not a leader
	 *         3: If one is promoting himself
	 * @author Ruoyu Zhang and Wyatt Kaiser
	 */
	@POST
	@Path("/makeLeader/{spaceId}")
	@Produces("application/json")
	public String makeLeader(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {

		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}		
		
		// Get the id of the user who initiated the promotion
		int userIdOfPromotion = SessionUtil.getUserId(request);
		User user = Users.get(userIdOfPromotion);
		// Permissions check; ensure the user an admin
		if (!GeneralSecurity.hasAdminWritePrivileges(user.getId())) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		// Extract the String user id's and convert them to Integer
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		
		// Validate the list of users to promote by:
		// 1 - Ensuring the leader who initiated the promotion of users from a space isn't themselves in the list of users to remove
		// 2 - Ensuring other leaders of the space aren't in the list of users to promote
		for(int userId : selectedUsers){
			if(userId == userIdOfPromotion){
				return gson.toJson(ERROR_CANT_PROMOTE_SELF);
			}
			
			if	(Permissions.get(userId, spaceId).isLeader()) {
				return gson.toJson(ERROR_CANT_PROMOTE_LEADER);
			}
			
			Permission p = Permissions.getFullPermission();
			
			Permissions.set(userId, spaceId, p);
		}
		return gson.toJson(new ValidatorStatusCode(true,"User promoted successfully"));
	}
	
	
	/**
	 * Demotes a user from a leader to only a member in a community. This is an admin only function.
	 * @param spaceId The Id of the community  
	 * @param userIdBeingDemoted ID of user to remove leadership from
	 * @param request
	 * @return 0: Success.
	 *         1: Selected userId list is empty.
	 *         2: User making this request is not a leader
	 *         3: If one is demoting himself
	 * @author Ruoyu Zhang and Wyatt Kaiser
	 */
	@POST
	@Path("/demoteLeader/{spaceId}/{userId}")
	@Produces("application/json")
	public String demoteLeader(@PathParam("spaceId") int spaceId, @PathParam("userId") int userIdBeingDemoted, @Context HttpServletRequest request) {		
		
		int userIdDoingDemoting=SessionUtil.getUserId(request);
		ValidatorStatusCode status=SpaceSecurity.canDemoteLeader(spaceId, userIdBeingDemoted, userIdDoingDemoting);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}

		Permission p = Permissions.getFullPermission();
		p.setLeader(false);
		
		return Permissions.set(userIdBeingDemoted, spaceId, p) ? gson.toJson(new ValidatorStatusCode(true,"User demoted successfully")) : gson.toJson(ERROR_DATABASE);
		
	}
	
	/**
	 * Handling the copy of subspaces for both single space copy and hierachy
	 * @param spaceId The Id of the space which is copied into.
	 * @param request The HttpRequestServlet object containing the list of the space's Id 
	 * @param response Will set "New_ID" cookie with ids of new spaces
	 * @return 0: Success.
	 *         1: The copying procedure fails.
	 *         2: Invalid input.
	 *         3: User doesn't have the copy permission.
	 *         4: User can't see the subspaces they are copying.
	 *         5: The space which is copied from is locked.
	 *         6: There exists a primitive with the same name.
	 * @author Ruoyu Zhang
	 */
	@POST
	@Path("/spaces/{spaceId}/copySpace")
	@Produces("application/json")
	public String copySubSpaceToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request, @Context HttpServletResponse response) {
		// Make sure we have a list of spaces to add, the id of the space it's coming from, and whether or not to apply this to all subspaces 
		if(null == request.getParameterValues("selectedIds[]") 
				|| !Util.paramExists("copyHierarchy", request)
				|| !Validator.isValidBool(request.getParameter("copyHierarchy"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		
		
		boolean copyHierarchy = Boolean.parseBoolean(request.getParameter("copyHierarchy"));
		
		// Convert the subSpaces to copy to an int list
		List<Integer> selectedSubSpaces = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		ValidatorStatusCode status=SpaceSecurity.canCopySpace(spaceId, requestUserId, selectedSubSpaces);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		List<Integer>newSpaceIds = new ArrayList<Integer>();
		// Add the subSpaces to the destination space
		if (!copyHierarchy) {
			for (int id : selectedSubSpaces) {
				int newSpaceId;
				try {
					newSpaceId = Spaces.copySpace(id, spaceId, requestUserId);
				} catch (StarExecException e) {
					return gson.toJson(new ValidatorStatusCode(false, e.getMessage()));
				}

				newSpaceIds.add(newSpaceId);

			}
		} else {
			for (int id : selectedSubSpaces) {
				int newSpaceId;
				try {
					newSpaceId = Spaces.copyHierarchy(id, spaceId, requestUserId);
				} catch (StarExecException e) {
					return gson.toJson(new ValidatorStatusCode(false, e.getMessage()));
				}
				newSpaceIds.add(newSpaceId);

			}
		}
		response.addCookie(new Cookie("New_ID", Util.makeCommaSeparatedList(newSpaceIds)));

		return gson.toJson(new ValidatorStatusCode(true,"Space copied successfully"));
	}
	
	
	/**
	 * Gets the ID of the user making this request
	 * @param request
	 * @return The integer ID of the user as a Json string
	 */
	@GET
	@Path("/users/getid")
	@Produces("application/json")
	public String getUserID(@Context HttpServletRequest request) {
		
		return gson.toJson(SessionUtil.getUserId(request));
	}
	/**
	 * Get the paginated result of the jobs belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of jobs if successful
	 * 		   1: The get job procedure fails.
	 * @author Ruoyu Zhang
	 */
	@POST
	@Path("/users/{id}/jobs/pagination")
	@Produces("application/json")	
	public String getUsrJobsPaginated(@PathParam("id") int usrId, @Context HttpServletRequest request) {
		int requestUserId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canViewUserPrimitives(usrId, requestUserId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Query for the next page of job pairs and return them to the user
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.JOB, usrId, request,false);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Gets pagination for all test sequences
	 * @param request
	 * @return json object for a DataTables test sequence table
	 */
	@GET
	@Path("/tests/pagination")
	@Produces("application/json")	
	public String getTestsPaginated(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Query for the next page of job pairs and return them to the user
		List<TestSequence> tests=TestManager.getAllTestSequences();
		JsonObject nextDataTablesPage=RESTHelpers.convertTestSequencesToJsonObject(tests, new DataTablesQuery(tests.size(), tests.size(), -1));
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Gets test results for all tests in a single TestSequence
	 * @param name Name of the TestSequence to get results for
	 * @param request
	 * @return json object for DataTables representing all tests in a test sequence
	 */
	@GET
	@Path("/testResults/pagination/{name}")
	@Produces("application/json")	
	public String getTestResultsPaginated(@PathParam("name") String name, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		// Query for the next page of job pairs and return them to the user
		List<TestResult> tests=TestManager.getAllTestResults(name);
		
		JsonObject nextDataTablesPage=RESTHelpers.convertTestResultsToJsonObject(tests, new DataTablesQuery(tests.size(), tests.size(), -1));
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Get the paginated result of the solvers belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of solvers if successful
	 * 		   1: The get solver procedure fails.
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/users/{id}/solvers/pagination/")
	@Produces("application/json")	
	public String getUsrSolversPaginated(@PathParam("id") int usrId, @Context HttpServletRequest request) {
		int requestUserId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canViewUserPrimitives(usrId, requestUserId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Query for the next page of solver pairs and return them to the user
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.SOLVER, usrId, request,false);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Get the paginated result of the benchmarks belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of benchmarks if successful
	 * 		   1: The get benchmark procedure fails.
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/users/{id}/benchmarks/pagination")
	@Produces("application/json")	
	public String getUsrBenchmarksPaginated(@PathParam("id") int usrId, @Context HttpServletRequest request) {
		int requestUserId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canViewUserPrimitives(usrId, requestUserId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}		// Query for the next page of solver pairs and return them to the user
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.BENCHMARK, usrId, request,false);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	
	/**
	 * Get the paginated result of the solvers belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of solvers if successful
	 * 		   1: The get solver procedure fails.
	 * @author Eric Burns
	 */
	@POST
	@Path("/users/{id}/rsolvers/pagination/")
	@Produces("application/json")	
	public String getUsrRecycledSolversPaginated(@PathParam("id") int usrId, @Context HttpServletRequest request) {
		int requestUserId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canViewUserPrimitives(usrId, requestUserId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.SOLVER, usrId, request,true);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Get the paginated result of the benchmarks belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of benchmarks if successful
	 * 		   1: The get benchmark procedure fails.
	 * @author Eric Burns
	 */
	@POST
	@Path("/users/{id}/rbenchmarks/pagination")
	@Produces("application/json")	
	public String getUsrRecycledBenchmarksPaginated(@PathParam("id") int usrId, @Context HttpServletRequest request) {
		int requestUserId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canViewUserPrimitives(usrId, requestUserId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.BENCHMARK, usrId, request, true);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	
	
	/**
	 * Make a space public
	 * @param spaceId the space to be made public
	 * @param hierarchy Whether to make the full hierarchy public or only the given space
	 * @param makePublic True to make spaces public and false to make them private
	 * @param request the http request
	 * @return 0: fails
	 *         1: success
	 * @author Ruoyu Zhang 
	 */
	@POST
	@Path("/space/changePublic/{id}/{hierarchy}/{makePublic}")
	@Produces("application/json")	
	public String makePublic(@PathParam("id") int spaceId, @PathParam("hierarchy") boolean hierarchy, @PathParam("makePublic") boolean makePublic, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=SpaceSecurity.canSetSpacePublicOrPrivate(spaceId, userId);
		if (!status.isSuccess()){
			return gson.toJson(status);
		}
		if(Spaces.setPublicSpace(spaceId, userId, makePublic, hierarchy))
			return gson.toJson(new ValidatorStatusCode(true,"Space(s) successfully made public"));
		else
			return gson.toJson(new ValidatorStatusCode(false, "Internal database error when making spaces public"));
	}
	
	
	
	/**
	 * Is a space public
	 * @param spaceId the space to be check if public
	 * @param request the http request
	 * @return 0: it's not public
	 *         1: it's public
	 * @author Ruoyu Zhang 
	 */
	@POST
	@Path("/space/isSpacePublic/{id}")
	@Produces("application/json")	
	public String isSpacePublic(@PathParam("id") int spaceId, @Context HttpServletRequest request) {
		if(Spaces.isPublicSpace(spaceId))
			return gson.toJson(1);
		else
			return gson.toJson(0);
	}
	
	/**
	 * Cancels a queue reservation 
	 * @param spaceId the id of the space the reservation was for
	 * @param queueId the id of the queue the reservation was for
	 * @param request the object containing the dataTable information
	 * @return a JSON object representing the success/failure of the cancellation
	 * @author Wyatt Kaiser
	 * @throws Exception
	 */
	@POST
	@Path("/cancel/queueReservation/{spaceId}/{queueId}")
	@Produces("application/json")
	public String cancelQueueReservation(@PathParam("spaceId") int spaceId, @PathParam("queueId") int queueId, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if(!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		Queues.removeQueue(queueId);
		return gson.toJson(new ValidatorStatusCode(true,"Reservation canceled successfully"));
	}
	
	
	/**
	 * Returns the next page of entries in a given DataTable
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of entries if successful,<br>
	 * 		1 if the request fails parameter validation, <br>
	 * @author Wyatt kaiser
	 * @throws Exception
	 */
	@GET
	@Path("/community/pending/requests/")
	@Produces("application/json")
	public String getAllPendingCommunityRequests(@Context HttpServletRequest request) throws Exception {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=SpaceSecurity.canUserViewCommunityRequests(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForPendingCommunityRequests(request);
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
	}
	/**
	 * Gets all requests that are pending to join a single community
	 * @param communityId The ID of the community to get requests for
	 * @param request
	 * @return json object for DataTables representing all community requests for the given community.
	 */
	@GET
	@Path("community/pending/requests/{communityId}")
	@Produces("application/json")
	public String getPendingCommunityRequestsForCommunity(@PathParam("communityId") int communityId, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=SpaceSecurity.canUserViewCommunityRequestsForCommunity(userId, communityId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForPendingCommunityRequestsForCommunity(request, communityId);
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
	}
	
	/**
	 * Handles the removal of a queue by the administrator
	 * @param queueId the id of the queue to remove
	 * @param request
	 * @return json ValidatorStatusCode
	 */
	@POST
	@Path("/remove/queue/{id}")
	@Produces("application/json")
	public String removeQueue(@PathParam("id") int queueId, @Context HttpServletRequest request) {
		log.debug("starting removeQueue");
		int userId = SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		Queues.removeQueue(queueId);

		
		return gson.toJson(new ValidatorStatusCode(true,"Queue removed successfully"));

	}
	
	/**
	 * Allows the administrator to set the current logging level for a specific class.
	 * @param level Logging level to set
	 * @param className fully qualified class name (org.starexec...) for the class 
	 * @param request
	 * @return json ValidatorStatusCode
	 * @throws Exception
	 */
	@POST
	@Path("/logging/{level}/{className}")
	@Produces("application/json")
	public String setLoggingLevel(@PathParam("level") String level, @PathParam("className") String className, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		boolean success=false;
		if (level.equalsIgnoreCase("trace")) {
			success=LoggingManager.setLoggingLevelForClass(Level.TRACE,className);
		} else if (level.equalsIgnoreCase("debug")) {
			success=LoggingManager.setLoggingLevelForClass(Level.DEBUG,className);
		} else if (level.equalsIgnoreCase("info")) {
			success=LoggingManager.setLoggingLevelForClass(Level.INFO,className);
		} else if (level.equalsIgnoreCase("error")) {
			success=LoggingManager.setLoggingLevelForClass(Level.ERROR,className);
		} else if(level.equalsIgnoreCase("fatal")) {
			success=LoggingManager.setLoggingLevelForClass(Level.FATAL,className);
		} else if (level.equalsIgnoreCase("off")) {
			success=LoggingManager.setLoggingLevelForClass(Level.OFF,className);
		} else if (level.equalsIgnoreCase("warn")) {
			success=LoggingManager.setLoggingLevelForClass(Level.WARN,className);
		} else if (level.equalsIgnoreCase("clear")) {
			success=LoggingManager.setLoggingLevelForClass(null,className);
		} else {
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		if (!success) {
			log.debug("could not find logger for class "+className);
		}
		return success ? gson.toJson(new ValidatorStatusCode(true,"Logging updated successfully")) : gson.toJson(ERROR_INVALID_PARAMS);
	}
	
	/**
	 * Allows the administrator to set the current logging level for a specific class an turn off logging for all other classes.
	 * @param inputLevel Level to set for the given class
	 * @param className The fully qualified class name (org.starexec...)
	 * @param request
	 * @return a json ValidatorStatusCode
	 * @throws Exception
	 */
	@POST
	@Path("/logging/allOffExcept/{level}/{className}")
	@Produces("application/json")
	public String setLoggingLevelOffForAllExceptClass(@PathParam("level") String inputLevel, @PathParam("className") String className, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}

		
		Level level = null; 

		boolean success=false;
		log.debug("Attempting to turn off logging for all classes except " +  className + " at level " + inputLevel + ".");
		if (inputLevel.equalsIgnoreCase("trace")) {
			level = Level.TRACE;
		} else if (inputLevel.equalsIgnoreCase("debug")) {
			level = Level.DEBUG;
		} else if (inputLevel.equalsIgnoreCase("info")) {
			level = Level.INFO;
		} else if (inputLevel.equalsIgnoreCase("error")) {
			level = Level.ERROR;
		} else if(inputLevel.equalsIgnoreCase("fatal")) {
			level = Level.FATAL;
		} else if (inputLevel.equalsIgnoreCase("off")) {
			level = Level.OFF;
		} else if (inputLevel.equalsIgnoreCase("warn")) {
			level = Level.WARN;
		} else if (inputLevel.equalsIgnoreCase("clear")) {
			// no action needed: level is already null
		} else {
			return gson.toJson(ERROR_INVALID_PARAMS);
		}

		// Attempt to set logging level for class.
		success = LoggingManager.setLoggingLevelForClass(level, className);


		if (!success) {
			log.debug("could not find logger for class "+className);
		} else {
			// Set all levels to off.
			LoggingManager.setLoggingLevel(Level.OFF);
			// Set logging level for class again.
			LoggingManager.setLoggingLevelForClass(level, className);
		}
		return success ? gson.toJson(new ValidatorStatusCode(true,"Logging updated successfully")) : gson.toJson(ERROR_INVALID_PARAMS);
	}
	
	/**
	 * Sets the logging level across all of Starexec
	 * @param level String represetning the new logging level to use
	 * @param request
	 * @return json ValidatorStatusCode
	 * @throws Exception
	 */
	@POST
	@Path("/logging/{level}")
	@Produces("application/json")
	public String setLoggingLevel(@PathParam("level") String level, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		if (level.equalsIgnoreCase("trace")) {
			LoggingManager.setLoggingLevel(Level.TRACE);
		} else if (level.equalsIgnoreCase("debug")) {
			LoggingManager.setLoggingLevel(Level.DEBUG);
		} else if (level.equalsIgnoreCase("info")) {
			LoggingManager.setLoggingLevel(Level.INFO);
		} else if (level.equalsIgnoreCase("error")) {
			LoggingManager.setLoggingLevel(Level.ERROR);
		} else if(level.equalsIgnoreCase("fatal")) {
			LoggingManager.setLoggingLevel(Level.FATAL);
		} else if (level.equalsIgnoreCase("off")) {
			LoggingManager.setLoggingLevel(Level.OFF);
		} else if (level.equalsIgnoreCase("warn")) {
			LoggingManager.setLoggingLevel(Level.WARN);
		} else {
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		return gson.toJson(new ValidatorStatusCode(true,"Logging updated successfully"));
	}
	
	/**
	 * Restarts Tomcat, causing a restart of STarexec
	 * @param request
	 * @return json ValidatorStatusCode object
	 * @throws Exception
	 */
	@POST
	@Path("/restart/starexec")
	@Produces("application/json")
	public String restartStarExec(@Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		log.debug("restarting...");
		Util.executeCommand("sudo -u tomcat /sbin/service tomcat7 restart");
		log.debug("restarted");
		return gson.toJson(new ValidatorStatusCode(true,"Starexec restarted successfully"));
	}
	
	/**
	 * Deletes all data in all LoadBalanceMonitor objects. Admin function to allow a reset of this data
	 * @param request
	 * @return json ValidatorStatusCode
	 * @throws Exception
	 */
	@POST
	@Path("/jobs/clearloadbalance")
	@Produces("application/json")
	public String clearLoadBalanceData(@Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		JobManager.clearLoadBalanceMonitors();
		return gson.toJson(new ValidatorStatusCode(true,"Load balancing cleared successfully"));
	}
	
	/**
	 * Toggles debug mode on or off
	 * @param value True to turn debug mode on and false to turn it off
	 * @param request
	 * @return a json ValidatorStatusCode
	 * @throws Exception
	 */
	@POST
	@Path("/starexec/debugmode/{value}")
	@Produces("application/json")
	public String updateDebugMode(@PathParam("value") boolean value, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		R.DEBUG_MODE_ACTIVE = value;
		return gson.toJson(new ValidatorStatusCode(true,"Debug mode state changed successfully"));
	}
	
	/**
	 * Will make the given queue the new test queue
	 * @param queueId The ID of the queue to set as the new test queue
	 * @param request
	 * @return json ValidatorStatusCode object
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/test/queue/{queueId}")
	@Produces("application/json")
	public String setTestQueue(@PathParam("queueId") int queueId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		boolean success = Queues.setTestQueue(queueId);
		
		return success ? gson.toJson(new ValidatorStatusCode(true,"Queue set as test queue")) : gson.toJson(ERROR_DATABASE);
	}

	
	/**
	 * Clears all stats from the cache for the given job
	 * @param jobId The ID of the job
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/cache/clear/stats/{jobId}")
	@Produces("application/json")
	public String clearCache(@PathParam("jobId") int jobId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);		
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.removeCachedJobStats(jobId) ? gson.toJson(new ValidatorStatusCode(true,"Cache cleared successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Clears every entry from the cache of job stats
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/cache/clearStats")
	@Produces("application/json")
	public String clearStatsCache(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.removeAllCachedJobStats() ? gson.toJson(new ValidatorStatusCode(true,"Cache cleared successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Changes a user's role to 'suspended'
	 * @param userId The ID of the user to update
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/suspend/user/{userId}")
	@Produces("application/json")
	public String suspendUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(id)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		boolean success = Users.suspend(userId);
		return success ? gson.toJson(new ValidatorStatusCode(true,"User suspended successfully")) : gson.toJson(ERROR_DATABASE);

	}
	/**
	 * Changes a user the suspended role back to the nornmal user role
	 * @param userId The ID of the user to update
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/reinstate/user/{userId}")
	@Produces("application/json")
	public String reinstateUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		if (!GeneralSecurity.hasAdminWritePrivileges(id)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		boolean success = Users.reinstate(userId);
		return success ? gson.toJson(new ValidatorStatusCode(true,"User reinstated successfully")) : gson.toJson(ERROR_DATABASE);

	}

	
	/**
	 * Subscribes a user to the e-mail report system.
	 * @param userId id of the user to subscribe.
	 * @param request HTTP request sent to the server.
	 * @return JSON object containing information about whether the subscription attempt succeeded or failed.
	 * @author Albert Giegerich
	 */
	@POST
	@Path("/subscribe/user/{userId}")
	@Produces("application/json")
	public String subscribeUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		ValidatorStatusCode status = UserSecurity.canUserSubscribeOrUnsubscribeUser(id);
		// Users can always subscribe themselves.
		if (!status.isSuccess() && id != userId) {
			return gson.toJson(status);
		}

		boolean success = Users.subscribeToReports(userId);
		return success ? gson.toJson(new ValidatorStatusCode(true, "User subscribed successfully")) : gson.toJson(ERROR_DATABASE);
	}

	/**
	 * Unsubscribes a user from the e-mail report system.
	 * @param userId the user to be unsubscribed from the system.
	 * @param request HTTP request sent to the server.
	 * @return JSON object containing information about whether the unsubscription attempt succeeded or failed.
	 * @author Albert Giegerich
	 */
	@POST
	@Path("/unsubscribe/user/{userId}")
	@Produces("application/json")
	public String unsubscribeUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		ValidatorStatusCode status = UserSecurity.canUserSubscribeOrUnsubscribeUser(id);
		// Users can always unsubscribe themselves.
		if (!status.isSuccess() && id != userId) {
			return gson.toJson(status);
		}

		boolean success = Users.unsubscribeFromReports(userId);
		return success ? gson.toJson(new ValidatorStatusCode(true, "User unsubscribed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	/**
	 * Grants the 'developer' role to a user
	 * @param userId The ID of the user to update
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/grantDeveloperStatus/user/{userId}")
	@Produces("application/json")
	public String grantDeveloperStatus(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		ValidatorStatusCode status = UserSecurity.canUserGrantOrSuspendDeveloperPrivileges(id);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		boolean success = Users.changeUserRole(userId, R.DEVELOPER_ROLE_NAME);
		return success ? gson.toJson(new ValidatorStatusCode(true, "Developer status granted. ")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes the 'developer' role from a user
	 * @param userId The ID of the user to update
	 * @param request
	 * @return a json ValidatorStatuscode
	 */
	@POST
	@Path("/suspendDeveloperStatus/user/{userId}")
	@Produces("application/json")
	public String suspendDeveloperStatus(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		ValidatorStatusCode status = UserSecurity.canUserGrantOrSuspendDeveloperPrivileges(id);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		boolean success = Users.changeUserRole(userId, R.DEFAULT_USER_ROLE_NAME);
		return success ? gson.toJson(new ValidatorStatusCode(true, "Developer status suspended.")) : gson.toJson(ERROR_DATABASE);
	}

	/**
	 * Sends the requested past report text file contents.
	 * @param reportName the file name of the past report.
	 * @param request HTTP request sent to the server.
	 * @return the contents of the requested file.
	 * @author Albert Giegerich	
	 */
	@GET
	@Path("/reports/past/{reportName}")
	@Produces("text/plain")	
	public String getPastReport(@PathParam("reportName") String reportName, @Context HttpServletRequest request) {
		try {
			File pastReport = new File(R.STAREXEC_DATA_DIR, "/reports/" + reportName);
			String pastReportContents = FileUtils.readFileToString(pastReport, "UTF8");
			return pastReportContents;
		} catch (IOException e) {
			return "Could not get file.";
		}
	}


	/**
	 * Sets the global pause feature as active in the system
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/admin/pauseAll")
	@Produces("application/json")
	public String pauseAll(@Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		log.info("Pausing all jobs in admin/pauseAll REST service");
		return Jobs.pauseAll() ? gson.toJson(new ValidatorStatusCode(true,"Jobs paused successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes the global pause from the system
	 * @param request
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/admin/resumeAll")
	@Produces("application/json")
	public String resumeAll(@Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the job, deny pause request
		int userId = SessionUtil.getUserId(request);
		
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.resumeAll() ? gson.toJson(new ValidatorStatusCode(true,"Jobs resumed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Marks a queue as being globally available
	 * @param request 
	 * @param queue_id The ID of the queue to update
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/queue/global/{queueId}")
	@Produces("application/json")
	public String makeQueueGlobal(@Context HttpServletRequest request, @PathParam("queueId") int queue_id) {
		int userId = SessionUtil.getUserId(request);		
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Queues.makeGlobal(queue_id) ? gson.toJson(new ValidatorStatusCode(true,"Queue is now global")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes a queue from the set of globally accessible queues
	 * @param request
	 * @param queue_id The ID of the queue to update
	 * @return a json ValidatorStatusCode
	 */
	@POST
	@Path("/queue/global/remove/{queueId}")
	@Produces("application/json")
	public String removeQueueGlobal(@Context HttpServletRequest request, @PathParam("queueId") int queue_id) {
		int userId = SessionUtil.getUserId(request);
				
		if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Queues.removeGlobal(queue_id) ? gson.toJson(new ValidatorStatusCode(true,"Queue no longer global")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Gets a json representation of some primitive type (solver, benchmark, job, space, configuration, processor)
	 * @param request 
	 * @param id The ID of the primitive 
	 * @param type The type of the primitive
	 * @return the json primitive, or a json ValidatorStatusCode on failure
	 */
	@GET
	@Path("/details/{type}/{id}")
	@Produces("application/json")
	public String getGsonPrimitive(@Context HttpServletRequest request, @PathParam("id") int id, @PathParam("type") String type) {
		int userId=SessionUtil.getUserId(request);
		if (type.equals(R.SOLVER)) {
			ValidatorStatusCode status=SolverSecurity.canGetJsonSolver(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return gson.toJson(Solvers.getIncludeDeleted(id));
		} else if (type.equals("benchmark")) {
			ValidatorStatusCode status=BenchmarkSecurity.canGetJsonBenchmark(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return gson.toJson(Benchmarks.getIncludeDeletedAndRecycled(id,false));
		} else if (type.equals("job")) {
			ValidatorStatusCode status=JobSecurity.canGetJsonJob(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return gson.toJson(Jobs.getIncludeDeleted(id));
			
 		} else if (type.equals("space")) {
 			ValidatorStatusCode status=SpaceSecurity.canGetJsonSpace(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			return gson.toJson(Spaces.get(id));
 		} else if (type.equals("configuration")) {
 			ValidatorStatusCode status=SolverSecurity.canGetJsonConfiguration(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
 		
 			return gson.toJson(Solvers.getConfiguration(id));
 		} else if (type.equals("processor")) {
 			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
 			
 			return gson.toJson(Processors.get(id));
 		} else if (type.equals("queue")) {
 			ValidatorStatusCode status=QueueSecurity.canGetJsonQueue(id, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
 			return gson.toJson(Queues.get(id));
 		}
		
		
		return gson.toJson(new ValidatorStatusCode(false,"Invalid type specified"));
	}
	
}
