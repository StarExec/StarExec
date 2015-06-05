package org.starexec.app;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Date;
import java.util.UUID;

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
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cache;
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
import org.starexec.data.security.CacheSecurity;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.SettingSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.security.SolverSecurity;
import org.starexec.data.security.SpaceSecurity;
import org.starexec.data.security.UserSecurity;
import org.starexec.data.to.*;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.test.TestManager;
import org.starexec.test.TestResult;
import org.starexec.test.TestSequence;
import org.starexec.util.Hash;
import org.starexec.util.LoggingManager;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Class which handles all RESTful web service requests.
 */
@Path("")
public class RESTServices {	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	private static Gson gson = new Gson();
	private static Gson limitGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	
	private static final ValidatorStatusCode ERROR_DATABASE=new ValidatorStatusCode(false, "There was an internal database error processing your request");
	private static final ValidatorStatusCode ERROR_INVALID_WEBSITE_TYPE=new ValidatorStatusCode(false, "The supplied website type was invalid");
	private static final ValidatorStatusCode ERROR_EDIT_VAL_ABSENT=new ValidatorStatusCode(false, "No value specified");
	private static final ValidatorStatusCode ERROR_IDS_NOT_GIVEN=new ValidatorStatusCode(false, "No ids specified");
	
	private static final ValidatorStatusCode ERROR_INVALID_PERMISSIONS=new ValidatorStatusCode(false, "You do not have permission to perform the requested operation");
	
	private static final ValidatorStatusCode ERROR_INVALID_PARAMS=new ValidatorStatusCode(false, "The supplied parameters are invalid");
	private static final ValidatorStatusCode ERROR_CANT_PROMOTE_SELF=new ValidatorStatusCode(false, "You cannot promote yourself");
	private static final ValidatorStatusCode ERROR_CANT_PROMOTE_LEADER=new ValidatorStatusCode(false, "The user is already a leader");
	
	private static final ValidatorStatusCode ERROR_NOT_ALL_DELETED=new ValidatorStatusCode(false, "Not all primitives could be deleted");
	
	private static final ValidatorStatusCode ERROR_TOO_MANY_JOB_PAIRS=new ValidatorStatusCode(false, "There are too many job pairs to display",1);
	private static final ValidatorStatusCode  ERROR_TOO_MANY_SOLVER_CONFIG_PAIRS=new ValidatorStatusCode(false, "There are too many solver / configuraiton pairs to display");
	
	
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
	 * @return a json string representing all the subspaces of the job space
	 * with the given id
	 * @author Eric Burns
	 */
	@GET
	@Path("/space/{jobid}/jobspaces/{spaceTree}")
	@Produces("application/json")	
	public String getJobSpaces(@QueryParam("id") int parentId,@PathParam("jobid") int jobId, @PathParam("spaceTree") boolean makeSpaceTree, @Context HttpServletRequest request) {					
		int userId = SessionUtil.getUserId(request);
		log.debug("got here with jobId= "+jobId+" and parent space id = "+parentId);
		List<JobSpace> subspaces=new ArrayList<JobSpace>();
		log.debug("getting job spaces for panels");
		//don't populate the subspaces if the user can't see the job
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jobId,userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		log.debug("got a request for parent space = "+parentId);
		if (parentId>0) {
			
			subspaces=Spaces.getSubSpacesForJob(parentId,false);
			
			
		} else {
			//if the id given is 0, we want to get the root space
			Job j=Jobs.get(jobId);
			JobSpace s=Spaces.getJobSpace(j.getPrimarySpace());
			subspaces.add(s);
		}
		
		log.debug("making next tree layer with "+subspaces.size()+" spaces");
		if (makeSpaceTree) {
			return gson.toJson(RESTHelpers.toJobSpaceTree(subspaces));

		} else {
			return gson.toJson(subspaces);

		}
	}
	
	/**
	 * Returns the paginated results of node assignments
	 * For the manage_nodes page. this is an admin only function
	 * 
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/nodes/dates/pagination/{string_date}")
	@Produces("application/json")
	//TODO: This needs to be refactored
	public String nodeSchedule(@PathParam("string_date") String date, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
	
		//Get todays date
		Date today = new Date();

		//Get the passed in date
		String start_month = date.substring(0,2);
		String start_day = date.substring(2, 4);
		String start_year = date.substring(4, 8);
		String new_date = start_month + "/" + start_day + "/" + start_year;
		
		//Get the latest date that a node is reserved for
		Date latest = Cluster.getLatestNodeDate();
		java.util.Date newDateJava = null;
		
		if (Validator.isValidDate(new_date)) {
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
			java.sql.Date newDateSql = null;
			java.sql.Date latestSql = null;
			try {
				newDateJava = format.parse(new_date);
				newDateSql = new java.sql.Date(newDateJava.getTime());
				
				latestSql = new java.sql.Date(latest.getTime());
			} catch (ParseException e1) {
				log.error(e1.getMessage(),e1);
			}
			
			if (newDateSql.before(latestSql) && !newDateSql.toString().equals(latestSql.toString())) {
				return gson.toJson(4);
			}
			
		} else {
			return gson.toJson(2);
		}
	
		latest = newDateJava;
		//Get all the dates between these two dates
	    List<java.sql.Date> dates = Requests.getDateRange(today, latest);
	    
	    JsonObject nextDataTablesPage = RESTHelpers.getNextdataTablesPageForManageNodes(dates, request);
	    return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	    
		}
	
	@GET
	@Path("/benchmarks/uploadDescription/{statusId}")
	@Produces("application/json")
	public String getBenchmarkUploadDescription(@PathParam("statusId") int statusId, @Context HttpServletRequest request) {
		int userId =SessionUtil.getUserId(request);
		if (!Permissions.canUserSeeBenchmarkStatus(statusId, userId)) {
			return gson.toJson(new ValidatorStatusCode(false, "You do not have permission to view this upload"));
		}
		return gson.toJson(new ValidatorStatusCode(true,Uploads.getUploadStatusSummary(statusId)));
	}
	
	
	
	/**
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
	 * @return 
	 */
	@POST
	@Path("/cluster/clearerrors")
	@Produces("application/json")	
	public String clearErrorStates(@Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserClearErrorStates(userId);
		if (!status.isSuccess()) {
			gson.toJson(status);
		}
		
		LinkedList<String> queueNames = new LinkedList<String>();
		for(Queue q : Queues.getAll()){
		    queueNames.add(q.getName());
		}
		return R.BACKEND.clearNodeErrorStates(R.SGE_ROOT,queueNames.toArray(new String[queueNames.size()])) ? gson.toJson(new ValidatorStatusCode(true)) : gson.toJson(new ValidatorStatusCode(false, "Internal error handling request"));
	}
	
	/**
	 * @return a json string representing all queues in the starexec cluster
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/queues")
	@Produces("application/json")	
	public String getAllQueues(@QueryParam("id") int id, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		if(id <= 0 && Users.isAdmin(userId)) {
			return gson.toJson(RESTHelpers.toQueueList(Queues.getAllAdmin()));
		} else if (id <= 0) {
			return gson.toJson(RESTHelpers.toQueueList(Queues.getAll()));
		} else {
			return gson.toJson(RESTHelpers.toNodeList(Queues.getNodes(id)));
		}
	}
	
	/**
	 * @return a text string that holds the result of running qstat -f
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/qstat")
	@Produces("text/plain")		
	public String getQstatOutput(@Context HttpServletRequest request) {		
		String qstat=R.BACKEND.getRunningJobsStatus(R.SGE_ROOT);
		if(!Util.isNullOrEmpty(qstat)) {
			return qstat;
		}

		return "not available";
	}
	
	/**
	 * @return a text string that holds the log of job pair with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/jobs/pairs/{id}/log")
	@Produces("text/plain")		
	public String getJobPairLog(@PathParam("id") int id, @Context HttpServletRequest request) {		
		int userId = SessionUtil.getUserId(request);
		int jobId=JobPairs.getPair(id).getJobId();
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jobId, userId);
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
	 * @param statusCode The status code that all the pairs to be rerun have curently
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
	 * @param statusCode The status code that all the pairs to be rerun have curently
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
	 * @return a string that holds the stdout of job pair with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/jobs/pairs/{id}/stdout/{stageNumber}")
	@Produces("text/plain")	
	public String getJobPairStdout(@PathParam("id") int id,@PathParam("stageNumber") int stageNumber, @QueryParam("limit") int limit, @Context HttpServletRequest request) {
		JobPair jp = JobPairs.getPair(id);
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jp.getJobId(), userId);
		if (!status.isSuccess()) {
			return "not available";
		}
		if(jp != null) {			
			String stdout = JobPairs.getStdOut(jp.getId(),stageNumber, limit);
			if(!Util.isNullOrEmpty(stdout)) {
				return stdout;
			}				
			
		
		}
		
		return "not available";
	}
	
	/**
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
	 * @return a json string representing all attributes of the queue with the given id
	 * @author Tyler Jensen
	 */

	@GET
	@Path("/cluster/queues/details/{id}")
	@Produces("application/json")	
	public String getQueueDetails(@PathParam("id") int id, @Context HttpServletRequest request) {
		log.debug("getting queue details");
		return gson.toJson(Queues.getDetails(id));
	}
	
	/**
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
	 * @return a 
	 */

	@GET
	@Path("/space/community/{spaceId}")
	@Produces("application/json")
	public String getCommunityIdOfSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		return gson.toJson(Spaces.getCommunityOfSpace(spaceId));	
	}
	
	/**
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
		boolean isCommunity = false;
		if (parentId == 1) {
			isCommunity = true;
		}
		User user = Users.get(userid);
		
		return gson.toJson(new RESTHelpers.PermissionDetails(perm,space,user,requester, isCommunity));
		
	}
	
	/**
	 * @return a json string representing all the subspaces of the space with
	 * the given id. If the given id is <= 0, then the root space is returned
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
	 * @return
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
	 * @param jobId the id of the job to get the next page of job pairs for
	 * @param jobspaceid The id of the job space at the root if the hierarchy we want pairs for
	 * @param type The type of pairs to return
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of job pair entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Eric Burns
	 */
	@POST
	@Path("/jobs/{id}/pairs/pagination/{jobSpaceId}/{configId}/{type}/{wallclock}/{stageNumber}")
	@Produces("application/json")	
	public String getJobPairsInSpaceHierarchyByConfigPaginated(@PathParam("id") int jobId,@PathParam("stageNumber") int stageNumber, @PathParam("wallclock") boolean wallclock, @PathParam("jobSpaceId") int jobSpaceId,@PathParam("type") String type, @PathParam("configId") int configId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		if (!JobSecurity.isValidGetPairType(type)) {
			return gson.toJson(new ValidatorStatusCode(false, "The selection of a filter type was invalid"));
		}
		
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfPairsByConfigInSpaceHierarchy(jobId,jobSpaceId,configId, request,type,wallclock,stageNumber);

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	

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

	@POST
	@Path("/users/benchmarks/pagination")
	@Produces("application/json")	
	public String getBenchmarksPaginatedByUser(@Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		log.debug("getting a datatable of all the benchmarks that this user can see");
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfBenchmarksByUser(userId, request);
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}

	@POST
	@Path("/jobs/{id}/comparisons/pagination/{jobSpaceId}/{config1}/{config2}/{wallclock}")
	@Produces("application/json")	
	public String getSolverComparisonsPaginated(@PathParam("id") int jobId,@PathParam("wallclock") boolean wallclock, @PathParam("jobSpaceId") int jobSpaceId,@PathParam("config1") int config1, @PathParam("config2") int config2, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		int stageNumber=0;
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfSolverComparisonsInSpaceHierarchy(jobId,jobSpaceId,config1,config2, request,wallclock,stageNumber);
		log.debug("got the next data table page for the solver comparision web page ");
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	
	
	/**
	 * Returns the next page of entries for a job pairs table. This is used on the job details page
	 *
	 * @param jobId the id of the job to get the next page of job pairs for
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of job pair entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Todd Elvers
	 */

	@POST
	@Path("/jobs/{id}/pairs/pagination/{jobSpaceId}/{wallclock}/{syncResults}/{stageNumber}")
	@Produces("application/json")	
	public String getJobPairsPaginated(@PathParam("id") int jobId,@PathParam("stageNumber") int stageNumber,@PathParam("wallclock") boolean wallclock, @PathParam("jobSpaceId") int jobSpaceId, @PathParam("syncResults") boolean syncResults, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status = JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfPairsInJobSpace(jobId,jobSpaceId, request,wallclock,syncResults,stageNumber);
		if (nextDataTablesPage==null) {
			return gson.toJson(ERROR_DATABASE);
		} else if (nextDataTablesPage.has("maxpairs")) {
			return gson.toJson(ERROR_TOO_MANY_JOB_PAIRS);
		}
		return gson.toJson(nextDataTablesPage); 
	}
	/**
	 * Handles a request to get a space overview graph for a job details page
	 * @param jobId The ID of the job to make the graph for
	 * @param jobSpaceId The job space the chart is for
	 * @param request Object containing other request information
	 * @return A json string containing the path to the newly created png chart
	 */
	
	@POST
	@Path("/jobs/{id}/{jobSpaceId}/graphs/spaceOverview/{stageNum}")
	@Produces("application/json")	
	public String getSpaceOverviewGraph(@PathParam("id") int jobId,@PathParam("stageNum") int stageNumber, @PathParam("jobSpaceId") int jobSpaceId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		String chartPath = null;
		// Ensure user can view the job they are requesting the pairs from
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		List<Integer> configIds=Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		boolean logX=false;
		boolean logY=false;
		if (Util.paramExists("logX", request)) {
			if (Boolean.parseBoolean((String)request.getParameter("logX"))) {
				logX=true;
			}
			
		}
		if (Util.paramExists("logY", request)) {
			if (Boolean.parseBoolean((String)request.getParameter("logY"))) {
				logY=true;
			}
		}
		if (configIds.size()<=R.MAXIMUM_SOLVER_CONFIG_PAIRS) {
			chartPath=Statistics.makeSpaceOverviewChart(jobSpaceId,logX,logY,configIds,stageNumber);
			if (chartPath.equals("big")) {
				return gson.toJson(ERROR_TOO_MANY_JOB_PAIRS);
			}
		} else {
			return gson.toJson(ERROR_TOO_MANY_SOLVER_CONFIG_PAIRS);
		}

		log.debug("chartPath = "+chartPath);
		return chartPath == null ? gson.toJson(ERROR_DATABASE) : chartPath;
	}




    /**
     * Handles a request to get a community statistical overview
     * @author Julio Cervantes
     * @return A json string containing the path to the newly created png chart as well as
     * an image map linking points to benchmarks
     */
    @POST
    @Path("/secure/explore/community/overview")
    @Produces("application/json")	
	public String getCommunityOverview(@Context HttpServletRequest request) throws Exception{
	


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


		

		String name;
		int id;
		JsonObject Comm;
		JsonObject info = new JsonObject();
		for(Space c : communities){
		    name = c.getName();
		    id = c.getId();

		    Comm = new JsonObject();
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
	 * Handles a request to get a solver comparison graph for a job details page
	 * @param jobId The ID of the job to make the graph for
	 * @param jobSpaceId The job space the chart is for
	 * @param config1 The ID of the first configuration to handle
	 * @param config2 The ID of the second configuration to handle
	 * @param request Object containing other request information
	 * @return A json string containing the path to the newly created png chart as well as
	 * an image map linking points to benchmarks
	 */
	@POST
	@Path("/jobs/{id}/{jobSpaceId}/graphs/solverComparison/{config1}/{config2}/{large}/{stageNum}")
	@Produces("application/json")	
	public String getSolverComparisonGraph(@PathParam("id") int jobId,@PathParam("stageNum") int stageNumber, @PathParam("jobSpaceId") int jobSpaceId,@PathParam("config1") int config1, @PathParam("config2") int config2, @PathParam("large") boolean large, @Context HttpServletRequest request) {		

	        
		int userId = SessionUtil.getUserId(request);
		List<String> chartPath = null;
		
		ValidatorStatusCode status= JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		chartPath=Statistics.makeSolverComparisonChart(jobId,config1,config2,jobSpaceId,large,stageNumber);
		if (chartPath==null) {
			return gson.toJson(ERROR_DATABASE);
		}
		if (chartPath.get(0).equals("big")) {
			return gson.toJson(ERROR_TOO_MANY_JOB_PAIRS);
		}
		JsonObject json=new JsonObject();
		json.addProperty("src", chartPath.get(0));
		json.addProperty("map",chartPath.get(1));
		
		return gson.toJson(json);
	}
	
	
	
	

	/**
	 * Returns the next page of stats for the given job and job space
	 * @param jobID the id of the job to get the next page of solver stats for
	 * @author Eric Burns
	 */
	@POST
	@Path("/jobs/{id}/solvers/pagination/{jobSpaceId}/{shortFormat}/{wallclock}/{stageNum}")
	@Produces("application/json")
	public String getJobStatsPaginated(@PathParam("id") int jobId,@PathParam("stageNum") int stageNumber, @PathParam("jobSpaceId") int jobSpaceId, @PathParam("shortFormat") boolean shortFormat, @PathParam("wallclock") boolean wallclock, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		ValidatorStatusCode status=JobSecurity.canUserSeeJob(jobId, userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		List<SolverStats> stats=Jobs.getAllJobStatsInJobSpaceHierarchy(jobId, jobSpaceId,stageNumber);

		nextDataTablesPage=RESTHelpers.convertSolverStatsToJsonObject(stats, stats.size(), stats.size(),1,jobSpaceId,jobId,shortFormat,wallclock);

		return nextDataTablesPage==null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
		
	}


	/**
	 * @return a string representing all attributes of the node with the given id
	 * @author Wyatt Kaiser
	 */
	@GET
	@Path("/cluster/nodes/{id}/pagination")
	@Produces("application/json")	
	public String getNodeJobPairs(@PathParam("id") int id, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForClusterExplorer("node", id, userId, request);

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	/**
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
		    nextDataTablesPage = RESTHelpers.getNextDataTablesPageForClusterExplorer("queue", id, userId, request);
		}
		catch(Exception e) {
		    log.error(e);
		}
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	

	/**
	 * Returns the next page of entries in a given DataTable (not restricted by space, returns ALL)
	 * @param primType the type of primitive
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of entries if successful,<br>
	 * 		1 if the request fails parameter validation, <br>
	 * @author Wyatt kaiser
	 * @throws Exception
	 */
	@POST
	@Path("/{primType}/pagination/")
	@Produces("application/json")
	public String getAllPrimitiveDetailsPagination(@PathParam("primType") String primType, @Context HttpServletRequest request) throws Exception {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		if (!Users.isAdmin(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		if (primType.startsWith("u")) {
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForAdminExplorer(RESTHelpers.Primitive.USER, request);
		}
		if (primType.startsWith("j")) {
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForAdminExplorer(RESTHelpers.Primitive.JOB, request);
		}
		if (primType.startsWith("n")) {
			nextDataTablesPage = RESTHelpers.getNextDataTablesPageForAdminExplorer(RESTHelpers.Primitive.NODE, request);
		}

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
	}
	
	@POST
	@Path("/job/{spaceId}/allbench/pagination/")
	@Produces("application/json")	
	public String getPrimitiveDetailsPaginated(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) throws Exception {	
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
		nextDataTablesPage= RESTHelpers.convertBenchmarksToJsonObject(benches, benches.size(), benches.size(), -1);
		
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
	 * @throws Exception 
	 */
	@POST
	@Path("/space/{id}/{primType}/pagination/")
	@Produces("application/json")	
	public String getPrimitiveDetailsPaginated(@PathParam("id") int spaceId, @PathParam("primType") String primType, @Context HttpServletRequest request) throws Exception {	
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
		} else if(primType.startsWith("r")){
			nextDataTablesPage = RESTHelpers.getResultTable(spaceId, request);
		}
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	

	/**
	 * Gets the permissions a given user has in a given space
	 * 
	 * @param spaceId the id of the space to check a user's permissions in
	 * @param userId the id of the user to check the permissions of
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
				if (Users.isAdmin(userId)) {
					return gson.toJson(Permissions.get(userId, spaceId));
				} else {
					return gson.toJson(ERROR_INVALID_PERMISSIONS);
				}
			}
		}
		
		if(p != null && (SessionUtil.getUserId(request) == userId || p.isLeader() )) {
			return gson.toJson(Permissions.get(userId, spaceId));
		}
		
		
		return null;
	}	
		
	
	/**
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
	 * 
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
			return gson.toJson(Websites.getAllForJavascript(userId, WebsiteType.USER));
		} else if(type.equals("space")){
			//SolverSecurity.canAssociateWebsite(solverId, userId, name)
			return gson.toJson(Websites.getAllForJavascript(id, WebsiteType.SPACE));
		} else if (type.equals("solver")) {
			return gson.toJson(Websites.getAllForJavascript(id, WebsiteType.SOLVER));
		}
		return gson.toJson(ERROR_INVALID_WEBSITE_TYPE);
	}
	
	/**
	 * Adds website information to the database. This is dynamic to allow adding a
	 * website associated with a space, solver, or user. The type of website is given
	 * in the path
	 * 
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
		if (type.equals("user")) {
			ValidatorStatusCode status=UserSecurity.canAssociateWebsite(name, url);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			success = Websites.add(userId, url, name,WebsiteType.USER);
		} else if (type.equals("space")) {
			// Make sure this user is capable of adding a website to the space
			ValidatorStatusCode status=SpaceSecurity.canAssociateWebsite(id, userId,name,url);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
					
			log.debug("adding website [" + url + "] to space [" + id + "] under the name [" + name + "].");
			success = Websites.add(id, url, name, WebsiteType.SPACE);
			
		} else if (type.equals("solver")) {
			//Make sure this user is the solver owner
			ValidatorStatusCode status=SolverSecurity.canAssociateWebsite(id, userId,name,url);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			
			success = Websites.add(id, url, name, WebsiteType.SOLVER);
			
		}
		
		// Passed validation AND Database update successful	
		return success ? gson.toJson(new ValidatorStatusCode(true,"Website added successfully")) : gson.toJson(ERROR_DATABASE);
	}

	
	/**
	 * Deletes a website from either a user's list of websites or a space's list of websites
	 *
	 * @param type the type the delete is for, can be either 'user' or 'space'
	 * @param spaceId the id of the space to remove the website from
	 * @param websiteId the id of the website to remove
	 * @return 0 iff the website was successfully deleted for a space, 2 if the user lacks permissions,
	 * and 1 otherwise
	 * @author Todd Elvers
	 */
	@POST
	@Path("/websites/delete/{websiteId}")
	@Produces("application/json")
	public String deleteWebsite(@PathParam("websiteId") int websiteId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		Website w = Websites.getWebsite(websiteId);
		if (w==null) {
			return gson.toJson(new ValidatorStatusCode(false, "The given website could not be found"));
		}
		if(w.getType()==WebsiteType.USER){
			ValidatorStatusCode status=UserSecurity.canDeleteWebsite(userId, websiteId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
		} else if (w.getType()==WebsiteType.SPACE){
			// Permissions check; ensures the user deleting the website is a leader
			ValidatorStatusCode status=SpaceSecurity.canDeleteWebsite(w.getPrimId(),websiteId, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			
		} else if (w.getType()==WebsiteType.SOLVER) {
			
			ValidatorStatusCode status=SolverSecurity.canDeleteWebsite(w.getPrimId(),websiteId, userId);
			if (!status.isSuccess()) {
				return gson.toJson(status);
			}
			
			
		} else  {
			return gson.toJson(ERROR_INVALID_WEBSITE_TYPE);

		}
		return Websites.delete(websiteId) ? gson.toJson(new ValidatorStatusCode(true,"Website deleted successfully")) : gson.toJson(ERROR_DATABASE);

		
	}
	
	/**
	 * Runs TestSequences that are given by name
	 * @param request
	 * @return
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
		for (String testName : testNames) {
			TestManager.executeTests(testNames);
		}
			
		return gson.toJson(new ValidatorStatusCode(true,"Testing started successfully"));
			
	}
	

	/**
	 * Runs every TestSequence. This does NOT run a stress test!
	 * @param request
	 * @return
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
	 * @param id
	 * @param request
	 * @return
	 */
	
	@POST
	@Path("/edit/queue/{id}")
	@Produces("application/json")
	public String editQueueInfo(@PathParam("id") int id, @Context HttpServletRequest request) {
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

		
		
		int userId=SessionUtil.getUserId(request);
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
	 * 
	 * @return a json string containing '0' if the update was successful, else 
	 * a json string containing '1'
	 * @author Skylar Stark
	 */
	@POST
	@Path("/edit/user/{attr}/{userId}/{val}")
	@Produces("application/json")
	public String editUserInfo(@PathParam("attr") String attribute, @PathParam("userId") int userId, @PathParam("val") String newValue,  @Context HttpServletRequest request,@Context HttpServletResponse response) {	
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
	 * @param request
	 * @return
	 */
	@POST
	@Path("/set/defaultSettings/{id}")
	@Produces("application/json")
	public String setSettingsProfileForUser(@PathParam("id") int id, @Context HttpServletRequest request) {	
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=SettingSecurity.canUserSeeProfile(id,userId);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		log.debug("setting a new default profile for a user");	
		boolean success=Settings.setDefaultProfileForUser(userId, id);
		// Passed validation AND Database update successful
		return success ? gson.toJson(new ValidatorStatusCode(true,"Profile set as default")) : gson.toJson(ERROR_DATABASE);
		
		
	}
	
	
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
		
		
		log.debug("post process request with jobId = "+jid+" and processor id = "+pid);
		
		return Jobs.prepareJobForPostProcessing(jid,pid,stageNumber) ? gson.toJson(new ValidatorStatusCode(true,"Post processing started successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Deletes a list of processors
	 * 
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
		}
		ValidatorStatusCode status=ProcessorSecurity.canUserDeleteProcessors(selectedProcessors, userId);
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
	 * @return	0: if the benchmark was successfully removed from the space,<br> 
	 * 			1: if there was a failure at the database level,<br>
	 * 			2: insufficient permissions
	 * @author 	Eric Burns
	 */
	@POST
	@Path("/recycleandremove/benchmark/{spaceID}")
	@Produces("application/json")
	public String recycleAndRemoveBenchmarks(@Context HttpServletRequest request,@PathParam("spaceID") int spaceId) {
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * 
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
	 * Deletes a list of solvers
	 * 
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
	 * @return
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
	 * Recycles all benchmarks that have been orphaned belonging to a specific user
	 *
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
	 *
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
	 * 
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
	 * 
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
	 * 
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
	 * 
	 * @param parentSpaceId the id the space to remove the subspace from
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
		
		int userId=SessionUtil.getUserId(request);
		ArrayList<Integer> selectedSubspaces = new ArrayList<Integer>();
				
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
			if (success) {
				return gson.toJson(new ValidatorStatusCode(true,"Subspace(s) removed successfully"));
			} else {
				return gson.toJson(ERROR_NOT_ALL_DELETED);
			}
			
		} else {
			return gson.toJson(ERROR_DATABASE);
		}
	}

	
	/**
	 * Only removes a subspace's association with a space, thereby removing the subspace
	 * from the space
	 * 
	 * @return 	0: success,<br>
	 * 			1: invalid parameters,<br>
	 * 			2: insufficient permissions,<br>
	 * 			3: error on the database level
	 * @author Ben McCune
	 */
	@POST
	@Path("/quickRemove/subspace")
	@Produces("application/json")
	public String quickRemoveSubspacesFromSpace(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ArrayList<Integer> selectedSubspaces = new ArrayList<Integer>();
		try{
			// Extract the String subspace id's and convert them to Integers
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedSubspaces.add(Integer.parseInt(id));
			}
		} catch(Exception e){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		ValidatorStatusCode status=SpaceSecurity.canUserRemoveSpace(userId,selectedSubspaces);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}

		// Remove the associations
		
		return Spaces.quickRemoveSubspaces(selectedSubspaces, SessionUtil.getUserId(request)) ? gson.toJson(new ValidatorStatusCode(true,"Subspace(s) removed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	/**
	 * Updates the details of a solver. Solver id is required in the path. First
	 * checks if the parameters of the update are valid, then performs the
	 * update.
	 * 
	 * @param id the id of the solver to update the details for
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
	 * @param id the id of the job to pause
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
	 * @param id the id of the job to resume
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
	 * @param id the id of the job to resume
	 * @param queueid the id of the queue to change to
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
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("downloadable", request)
				|| !Util.paramExists("type", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Safely extract the type
		try{
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
		
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));

		ValidatorStatusCode status=BenchmarkSecurity.canUserEditBenchmark(benchId,name,description,type,userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Apply new benchmark details to database
		return Benchmarks.updateDetails(benchId, name, description, isDownloadable, type) ? gson.toJson(new ValidatorStatusCode(true,"Benchmark edited successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Updates the current user's password. First verifies that it is in
	 * the correct format, then hashes is and updates it to the database.
	 * 
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
	 * 
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
			description = (String) request.getParameter("description");
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
		if (!Users.isAdmin(user.getId())) {
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
				int newSpaceId = RESTHelpers.copySpace(id, spaceId, requestUserId);
				
				if (newSpaceId == 0){
					return gson.toJson(ERROR_DATABASE);
				}
				newSpaceIds.add(newSpaceId);

			}
		} else {
			for (int id : selectedSubSpaces) {
				//TODO: Should this return a list of ids of every space in the hierarchy?
				int newSpaceId = RESTHelpers.copyHierarchy(id, spaceId, requestUserId);
				if (newSpaceId == 0){
					return gson.toJson(ERROR_DATABASE);
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
	 * Get the paginated result of the jobs belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of jobs if successful
	 * 		   1: The get job procedure fails.
	 * @author Ruoyu Zhang
	 */
	@GET
	@Path("/tests/pagination")
	@Produces("application/json")	
	public String getTestsPaginated(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserSeeTestInformation(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// Query for the next page of job pairs and return them to the user
		List<TestSequence> tests=TestManager.getAllTestSequences();
		JsonObject nextDataTablesPage=RESTHelpers.convertTestSequencesToJsonObject(tests, tests.size(), tests.size(), -1);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Get the paginated result of the jobs belong to a specified user
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return a JSON object representing the next page of jobs if successful
	 * 		   1: The get job procedure fails.
	 * @author Ruoyu Zhang
	 */
	@GET
	@Path("/testResults/pagination/{name}")
	@Produces("application/json")	
	public String getTestResultssPaginated(@PathParam("name") String name, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserSeeTestInformation(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		// Query for the next page of job pairs and return them to the user
		List<TestResult> tests=TestManager.getAllTestResults(name);
		JsonObject nextDataTablesPage=RESTHelpers.convertTestResultsToJsonObject(tests, tests.size(), tests.size(), -1);
		
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
	
	@POST
	@Path("/queues/pending/pagination/")
	@Produces("application/json")
	public String getAllPendingQueueReservations(@Context HttpServletRequest request) throws Exception {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}	
		
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForPendingQueueReservations(request);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
	}


	@POST
	@Path("/queues/historic/pagination/")
	@Produces("application/json")
	public String getAllHistoricReservations(@Context HttpServletRequest request) throws Exception {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}		
		
		JsonObject nextDataTablesPage = RESTHelpers.getNextDataTablesPageForHistoricReservations(request);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
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
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if(!status.isSuccess()) {
			return gson.toJson(status);
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
	@POST
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
	 * Handles the removal of a queue by the administrator
	 * @param queueId the id of th queue to remove
	 * @ throws Exception
	 */
	@POST
	@Path("/remove/queue/{id}")
	@Produces("application/json")
	public String removeQueue(@PathParam("id") int queueId, @Context HttpServletRequest request) {
		log.debug("starting removeQueue");
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		Queues.removeQueue(queueId);

		
		return gson.toJson(new ValidatorStatusCode(true,"Queue removed successfully"));

	}
	
	//Allows the administrator to set the current logging level for a specific class.
	@POST
	@Path("/logging/{level}/{className}")
	@Produces("application/json")
	public String setLoggingLevel(@PathParam("level") String level, @PathParam("className") String className, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserChangeLogging(userId);
		if (!status.isSuccess()) {
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

	//Allows the administrator to set the current logging level for a specific class an turn off logging for all other classes.
	@POST
	@Path("/logging/allOffExcept/{level}/{className}")
	@Produces("application/json")
	public String setLoggingLevelOffForAllExceptClass(@PathParam("level") String inputLevel, @PathParam("className") String className, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserChangeLogging(userId);
		if (!status.isSuccess()) {
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
			level = null;
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
	
	//Allows the administrator to set the current logging level across the entire system.
	@POST
	@Path("/logging/{level}")
	@Produces("application/json")
	public String setLoggingLevel(@PathParam("level") String level, @Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserChangeLogging(userId);
		if (!status.isSuccess()) {
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
		
	@POST
	@Path("/restart/starexec")
	@Produces("application/json")
	public String restartStarExec(@Context HttpServletRequest request) throws Exception {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserRestartStarexec(userId);
		if (!status.isSuccess()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		log.debug("restarting...");
		Util.executeCommand("sudo -u tomcat /sbin/service tomcat7 restart");
		log.debug("restarted");
		return gson.toJson(new ValidatorStatusCode(true,"Starexec restarted successfully"));
	}
	
	@POST
	@Path("/cancel/request/{id}")
	@Produces("application/json")
	public String cancelQueueRequest(@PathParam("id") int id, @Context HttpServletRequest request) throws IOException {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		// simply remove the reservation from our 
		boolean success= Requests.removeQueueReservation(id);
		
		if (success) {
			QueueRequest queueRequest = Requests.getQueueRequest(id);
			// Notify user they've been declined
			Mail.sendReservationResults(queueRequest, false);
			log.info(String.format("User [%s] has been declined queue reservation.", Users.get(queueRequest.getUserId()).getFullName()));
			return gson.toJson(new ValidatorStatusCode(true,"Request canceled successfully"));	
		} else {
			return gson.toJson(ERROR_DATABASE);
		}
		
	}

	/**
	 * Updates information in the database using a POST. the new values for 
	 * queueName, nodeCount, startDate, and endDate are included in the path.
	 * First validates that the new values are legal, and then updates the database
	 * and session information accordingly.
	 * 
	 * @return a json string containing '0' if the update was successful, else
	 * a json string containing '1'
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/edit/request/{id}/{queueName}/{nodeCount}/{startDate}/{endDate}")
	@Produces("application/json")
	public String editQueueRequest(@PathParam("id") int id, @PathParam("queueName") String queueName, @PathParam("nodeCount") String nodeCount, @PathParam("startDate") String startDate, @PathParam("endDate") String endDate, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserUpdateRequest(userId,queueName);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		boolean success = false;
		String start_month = startDate.substring(0,2);
		String start_day = startDate.substring(2, 4);
		String start_year = startDate.substring(4, 8);
		
		String end_month = endDate.substring(0,2);
		String end_day = endDate.substring(2, 4);
		String end_year = endDate.substring(4, 8);

		String new_start = start_month + "/" + start_day + "/" + start_year;
		String new_end = end_month + "/" + end_day + "/" + end_year;

		
		if ((Validator.isValidInteger(nodeCount) && Validator.isValidDate(new_start) && Validator.isValidDate(new_end)) ) {
			log.debug("validated");
			
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		    java.sql.Date startDateSql = null;
		    java.sql.Date endDateSql = null;
		    java.sql.Date todaySql = null;
			try {
				java.util.Date startDateJava = format.parse(new_start);
				startDateSql = new java.sql.Date(startDateJava.getTime());
				
				java.util.Date endDateJava = format.parse(new_end);
				endDateSql = new java.sql.Date(endDateJava.getTime());
				
				java.util.Date today = new Date();
				todaySql = new java.sql.Date(today.getTime());
			} catch (ParseException e1) {
				log.error(e1.getMessage(),e1);
			}
			log.debug("today = " + todaySql);
			log.debug("startDate = " + startDateSql);
			log.debug("endDate = " + endDateSql);
			
			if (startDateSql.before(todaySql) && (!startDateSql.toString().equals(todaySql.toString())) ) {
				log.debug("start day is before today");
				return gson.toJson(4);
			} else if (endDateSql.before(startDateSql)) { //end date is before or equivalent to start date
				log.debug("end date is before start date");
				return gson.toJson(5);
			} else if (endDateSql.toString().equals(startDateSql.toString())) {
				log.debug("end date is equivalent to start date");
				return gson.toJson(5);
			} else {
				QueueRequest req = Requests.getQueueRequest(id);
				QueueRequest new_req = new QueueRequest();
				new_req.setUserId(req.getUserId());
				new_req.setSpaceId(req.getSpaceId());
				new_req.setQueueName(queueName);
				
				//Only update max node_count if it has been changed
				if (Integer.parseInt(nodeCount) == req.getNodeCount()) {
					new_req.setNodeCount(req.getNodeCount());
				} else {
					new_req.setNodeCount(Integer.parseInt(nodeCount));
				}
				new_req.setStartDate(startDateSql);
				new_req.setEndDate(endDateSql);
				new_req.setMessage(req.getMessage());
				new_req.setId(id);
				new_req.setCreateDate(req.getCreateDate());
				
				success = Requests.updateQueueRequest(new_req);		

			}
		} else {
			log.debug("invalid");
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		return success ? gson.toJson(new ValidatorStatusCode(true,"Request edited successfully")) : gson.toJson(ERROR_DATABASE);
	}

	
	
	
	/**
	 * Will update the database to reflect saved temp node_count changes
	 * 
	 * @return a json string containing '0' if the update was successful, else a json string containing '1'
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/nodes/update")
	@Produces("application/json")
	public String updateNodeCount(@Context HttpServletRequest request) {
		//int userId=SessionUtil.getUserId(request);
		//if (!Users.isAdmin(userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		//}
		//boolean success = Cluster.updateTempChanges();
		//return success ? gson.toJson(new ValidatorStatusCode(true,"Nodes updated successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Will make the given queue the new test queue
	 * 
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/test/queue/{queueId}")
	@Produces("application/json")
	public String setTestQueue(@PathParam("queueId") int queueId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		boolean success = Queues.setTestQueue(queueId);
		
		return success ? gson.toJson(new ValidatorStatusCode(true,"Queue set as test queue")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Will update a queue making it permanent
	 * 
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/permanent/queue/{queueId}")
	@Produces("application/json")
	public String makeQueuePermanent(@PathParam("queueId") int queue_id, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		QueueRequest req = Requests.getRequestForReservation(queue_id);
		Queue q = Queues.get(queue_id);
		boolean success = true;
		//Make BACKEND changes
		if (!q.getStatus().equals("ACTIVE")) {
		    success = R.BACKEND.createPermanentQueue(R.SGE_ROOT, true,req.getQueueName(),null,null);
		}
		
		//Make database changes
		if (success) {
			success = Queues.makeQueuePermanent(queue_id);
		}
		
		return success ? gson.toJson(new ValidatorStatusCode(true,"Queue is now permanent")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Clears all stats from the cache for the given job
	 * @param request
	 * @return
	 */
	@POST
	@Path("/cache/clear/stats/{jobId}")
	@Produces("application/json")
	public String clearCache(@PathParam("jobId") int jobId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=CacheSecurity.canUserClearCache(userId);
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Jobs.removeCachedJobStats(jobId) ? gson.toJson(new ValidatorStatusCode(true,"Cache cleared successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Clears every entry from the cache
	 * @param request
	 * @return
	 */
	@POST
	@Path("/cache/clearAll")
	@Produces("application/json")
	public String clearCache(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=CacheSecurity.canUserClearCache(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		return Cache.deleteAllCache() ? gson.toJson(new ValidatorStatusCode(true,"Cache cleared successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Clears every entry from the cache
	 * @param request
	 * @return
	 */
	@POST
	@Path("/cache/clearStats")
	@Produces("application/json")
	public String clearStatsCache(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=CacheSecurity.canUserClearCache(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Jobs.removeAllCachedJobStats() ? gson.toJson(new ValidatorStatusCode(true,"Cache cleared successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * 
	 */
	@POST
	@Path("/cache/clearTypes")
	@Produces("application/json")
	public String clearCacheTypes(@Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		ValidatorStatusCode status=CacheSecurity.canUserClearCache(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		
		List<Integer> types=Util.toIntegerList(request.getParameterValues("selectedTypes[]"));		
		boolean success=true;
		for (Integer i : types) {
			success= success && Cache.deleteCacheOfType(CacheType.getType(i));
		}
		
		return success ? gson.toJson(new ValidatorStatusCode(true,"Cache cleared successfully")) : gson.toJson(ERROR_DATABASE);
	}
	

	
	@POST
	@Path("/suspend/user/{userId}")
	@Produces("application/json")
	public String suspendUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canUserSuspendOrReinstateUser(id);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		boolean success = Users.suspend(userId);
		return success ? gson.toJson(new ValidatorStatusCode(true,"User suspended successfully")) : gson.toJson(ERROR_DATABASE);

	}
	
	@POST
	@Path("/reinstate/user/{userId}")
	@Produces("application/json")
	public String reinstateUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {
		int id = SessionUtil.getUserId(request);
		ValidatorStatusCode status=UserSecurity.canUserSuspendOrReinstateUser(id);
		if (!status.isSuccess()) {
			return gson.toJson(status);
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


	
	@POST
	@Path("/admin/pauseAll")
	@Produces("application/json")
	public String pauseAll(@Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		
		ValidatorStatusCode status=JobSecurity.canUserPauseAllJobs(userId);
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		log.info("Pausing all jobs in admin/pauseAll REST service");
		return Jobs.pauseAll() ? gson.toJson(new ValidatorStatusCode(true,"Jobs paused successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	@POST
	@Path("/admin/resumeAll")
	@Produces("application/json")
	public String resumeAll(@Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the job, deny pause request
		int userId = SessionUtil.getUserId(request);
		
		ValidatorStatusCode status=JobSecurity.canUserResumeAllJobs(userId);

		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Jobs.resumeAll() ? gson.toJson(new ValidatorStatusCode(true,"Jobs resumed successfully")) : gson.toJson(ERROR_DATABASE);
	}
	
	@POST
	@Path("/queue/global/{queueId}")
	@Produces("application/json")
	public String makeQueueGlobal(@Context HttpServletRequest request, @PathParam("queueId") int queue_id) {
		int userId = SessionUtil.getUserId(request);
		
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Queues.makeGlobal(queue_id) ? gson.toJson(new ValidatorStatusCode(true,"Queue is now global")) : gson.toJson(ERROR_DATABASE);
	}
	
	@POST
	@Path("/queue/global/remove/{queueId}")
	@Produces("application/json")
	public String removeQueueGlobal(@Context HttpServletRequest request, @PathParam("queueId") int queue_id) {
		int userId = SessionUtil.getUserId(request);
		
		ValidatorStatusCode status=QueueSecurity.canUserModifyQueues(userId);
		
		
		if (!status.isSuccess()) {
			return gson.toJson(status);
		}
		
		return Queues.removeGlobal(queue_id) ? gson.toJson(new ValidatorStatusCode(true,"Queue no longer global")) : gson.toJson(ERROR_DATABASE);
	}
	@GET
	@Path("/details/{type}/{id}")
	@Produces("application/json")
	public String getGsonPrimitive(@Context HttpServletRequest request, @PathParam("id") int id, @PathParam("type") String type) {
		int userId=SessionUtil.getUserId(request);
		if (type.equals("solver")) {
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
 			ValidatorStatusCode status=ProcessorSecurity.canGetJsonProcessor(id, userId);
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
