package org.starexec.app;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Communities;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Statistics;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobStatus.JobStatusCode;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverStats;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Hash;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Class which handles all RESTful web service requests.
 */
@Path("")
public class RESTServices {	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	private static Gson gson = new Gson();
	private static Gson limitGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	
	private static final int ERROR_DATABASE=1;
	private static final int ERROR_INVALID_WEBSITE_TYPE=1;
	private static final int ERROR_EDIT_VAL_ABSENT=1;
	private static final int ERROR_IDS_NOT_GIVEN=1;
	private static final int ERROR_SPACE_ALREADY_PUBLIC=1;
	private static final int ERROR_SPACE_ALREADY_PRIVATE=1;
	
	private static final int ERROR_INVALID_PERMISSIONS=2;
	private static final int ERROR_INVALID_PASSWORD=2;
	
	private static final int ERROR_JOB_INCOMPLETE=3;
	private static final int ERROR_INVALID_PARAMS=3;
	private static final int ERROR_CANT_REMOVE_FROM_SUBSPACE=3;
	private static final int ERROR_PASSWORDS_NOT_EQUAL=3;
	private static final int ERROR_CANT_EDIT_LEADER_PERMS=3;
	private static final int ERROR_CANT_PROMOTE_SELF=3;
	private static final int ERROR_CANT_PROMOTE_LEADER=3;
	private static final int ERROR_JOB_NOT_PROCESSING=3;
	
	private static final int ERROR_NOT_IN_SPACE=4;
	private static final int ERROR_CANT_REMOVE_LEADER=4;
	private static final int ERROR_NOT_ALL_DELETED=4;
	private static final int ERROR_WRONG_PASSWORD=4; 
	
	private static final int ERROR_CANT_REMOVE_SELF=5;
	private static final int ERROR_SPACE_LOCKED=5;
	
	private static final int ERROR_CANT_LINK_TO_SUBSPACE=6;
	private static final int ERROR_CANT_REMOVE_LEADER_IN_SUBSPACE=6;
	
	private static final int ERROR_NOT_UNIQUE_NAME=7;
	
	private static final int ERROR_INSUFFICIENT_QUOTA=8;
	
	private static final int ERROR_NAME_NOT_EDITABLE=9;
	
	private static final int ERROR_PRIM_ALREADY_DELETED=11;
	
	private static final int ERROR_TOO_MANY_JOB_PAIRS=13;
	private static final int ERROR_TOO_MANY_SOLVER_CONFIG_PAIRS=12;
	
	/**
	 * @return a json string representing all the subspaces of the job space
	 * with the given id
	 * @author Eric Burns
	 */
	@GET
	@Path("/space/{jobid}/subspaces")
	@Produces("application/json")	
	public String getSubSpaces(@QueryParam("id") int parentId,@PathParam("jobid") int jobId, @Context HttpServletRequest request) {					
		int userId = SessionUtil.getUserId(request);
		
		List<Space> subspaces=new ArrayList<Space>();
		
		//don't populate the subspaces if the user can't see the job
		if (Permissions.canUserSeeJob(jobId, userId)) {
			if (parentId>0) {
				
				subspaces=Spaces.getSubSpacesForJob(parentId,false);
			} else {
				//if the id given is 0, we want to get the root space
				Job j=Jobs.get(jobId);
				Space s=Spaces.getJobSpace(j.getPrimarySpace());
				subspaces.add(s);
			}
		} else  {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		log.debug("making next tree layer with "+subspaces.size()+" spaces");
		return gson.toJson(RESTHelpers.toJobSpaceTree(subspaces));
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
		log.debug("test = " + Spaces.getSubSpaces(parentId, userId, false));
		return gson.toJson(RESTHelpers.toSpaceTree(Spaces.getSubSpaces(parentId, userId, false),userId));
	}
	
	/**
	 * @return a json string representing all the spaces that a certain user is a member of
	 * @author Wyatt Kaiser
	 */
	@GET
	@Path("/space/userAssoc/{userId}")
	@Produces("application/json")	
	public String getSpacesForUser(@PathParam("userId") int userId, @Context HttpServletRequest request) {	
		return gson.toJson(RESTHelpers.toCommunityList((Spaces.GetSpacesByUser(userId))));
	}
	
	/**
	 * @return a json string representing all public solvers of a community
	 * @author Benton McCune and Ruoyu Zhang
	 */
	@GET
	@Path("/communities/solvers/{id}")
	@Produces("application/json")	
	public String getPublicSolvers(@PathParam("id") int commId, @Context HttpServletRequest request) {					
		log.debug("commId = " + commId);
		List<Solver> publicSolvers = Solvers.getPublicSolversByCommunity(commId);
		log.debug("# of public solvers = " + publicSolvers.size());
		
		JsonObject displayObject = new JsonObject();
		JsonArray jSSolvers = new JsonArray();
		for (Solver solver:publicSolvers){
			JsonArray jSSolver = new JsonArray();
			jSSolver.add(new JsonPrimitive(solver.getId()));
			jSSolver.add(new JsonPrimitive(solver.getName()));
			jSSolvers.add(jSSolver);
		}
		displayObject.add("solverData", jSSolvers);
		log.debug("JsonArray = " + jSSolvers.toString());
		log.debug("displayObject = " + displayObject.toString());
		return gson.toJson(jSSolvers);
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
	 * @return a json string representing all queues in the starexec cluster
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/queues")
	@Produces("application/json")	
	public String getAllQueues(@QueryParam("id") int id, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		if(id <= 0 && u.getRole().equals("admin")) {
			return gson.toJson(RESTHelpers.toQueueList(Queues.getAllAdmin()));
		} else if (id <= 0) {
			return gson.toJson(RESTHelpers.toQueueList(Queues.getAll()));
		} else {
			return gson.toJson(RESTHelpers.toNodeList(Queues.getNodes(id)));
		}
	}
	
	/**
	 * @return a json string that holds the log of job pair with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/jobs/pairs/{id}/log")
	@Produces("text/plain")		
	public String getJobPairLog(@PathParam("id") int id, @Context HttpServletRequest request) {		
		JobPair jp = JobPairs.getPair(id);
		int userId = SessionUtil.getUserId(request);
		
		if(jp != null) {			
			if(Permissions.canUserSeeJob(jp.getJobId(), userId)) {
				String log = GridEngineUtil.getJobLog(jp.getId(), jp.getGridEngineId());
				
				if(!Util.isNullOrEmpty(log)) {
					return log;
				}
			}
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
		Benchmark b = Benchmarks.get(id);
		int userId = SessionUtil.getUserId(request);
		
		if(b != null) {			
			if(Permissions.canUserSeeBench(b.getId(), userId) && b.isDownloadable()) {				
				String contents = Benchmarks.getContents(b, limit);
				
				if(!Util.isNullOrEmpty(contents)) {
					return contents;
				}				
			}
		}
		
		return "not available";
	}
	
	/**
	 * @return a string that holds the std out of job pair with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/jobs/pairs/{id}/stdout")
	@Produces("text/plain")	
	public String getJobPairStdout(@PathParam("id") int id, @QueryParam("limit") int limit, @Context HttpServletRequest request) {
		JobPair jp = JobPairs.getPair(id);
		int userId = SessionUtil.getUserId(request);
		
		if(jp != null) {			
			if(Permissions.canUserSeeJob(jp.getJobId(), userId)) {
				Job j = Jobs.getShallow(jp.getJobId());			
				String stdout = GridEngineUtil.getStdOut(jp, limit);
				
				if(!Util.isNullOrEmpty(stdout)) {
					return stdout;
				}				
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
		
		if(community != null) {
			community.setUsers(Spaces.getUsers(id));
			Permission p = SessionUtil.getPermission(request, id);
			List<User> leaders = Spaces.getLeaders(id);
			List<Website> sites = Websites.getAll(id, Websites.WebsiteType.SPACE);
			return gson.toJson(new RESTHelpers.CommunityDetails(community, p, leaders, sites));
		}
		
		return gson.toJson(RESTHelpers.toCommunityList(Communities.getAll()));
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
		System.out.println("here");
		int userId = SessionUtil.getUserId(request);
		
		Space s = null;
		Permission p = null;
		
		if(Permissions.canUserSeeSpace(spaceId, userId)) {
			s = Spaces.get(spaceId); 
			p = SessionUtil.getPermission(request, spaceId);
		}					
		
		return limitGson.toJson(new RESTHelpers.SpacePermPair(s, p));				
	}	
	
	/**
	 * Returns the next page of entries for a job pairs table
	 *
	 * @param jobId the id of the job to get the next page of job pairs for
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of job pair entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Todd Elvers
	 */
	@POST
	@Path("/jobs/{id}/pairs/pagination/{jobSpaceId}/{configId}")
	@Produces("application/json")	
	public String getJobPairsPaginated(@PathParam("id") int jobId, @PathParam("jobSpaceId") int jobSpaceId, @PathParam("configId") int configId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		// Ensure user can view the job they are requesting the pairs from
		if(false == Permissions.canUserSeeJob(jobId, userId)){
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfPairsByConfigInSpaceHierarchy(jobId,jobSpaceId,configId, request);

		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	}
	
	/**
	 * Returns the next page of entries for a job pairs table
	 *
	 * @param jobId the id of the job to get the next page of job pairs for
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of job pair entries if successful,<br>
	 * 		1 if the request fails parameter validation,<br> 
	 * 		2 if the user has insufficient privileges to view the parent space of the primitives 
	 * @author Todd Elvers
	 */
	@POST
	@Path("/jobs/{id}/pairs/pagination/{jobSpaceId}")
	@Produces("application/json")	
	public String getJobPairsPaginated(@PathParam("id") int jobId, @PathParam("jobSpaceId") int jobSpaceId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		// Ensure user can view the job they are requesting the pairs from
		if(false == Permissions.canUserSeeJob(jobId, userId)){
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageOfPairsInJobSpace(jobId,jobSpaceId, request);
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
	@Path("/jobs/{id}/{jobSpaceId}/graphs/spaceOverview")
	@Produces("application/json")	
	public String getSpaceOverviewGraph(@PathParam("id") int jobId, @PathParam("jobSpaceId") int jobSpaceId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		String chartPath = null;
		// Ensure user can view the job they are requesting the pairs from
		if(false == Permissions.canUserSeeJob(jobId, userId)){
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
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
			chartPath=Statistics.makeSpaceOverviewChart(jobSpaceId,logX,logY,configIds);
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
	@Path("/jobs/{id}/{jobSpaceId}/graphs/solverComparison/{config1}/{config2}/{large}")
	@Produces("application/json")	
	public String getSolverComparisonGraph(@PathParam("id") int jobId, @PathParam("jobSpaceId") int jobSpaceId,@PathParam("config1") int config1, @PathParam("config2") int config2, @PathParam("large") boolean large, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		List<String> chartPath = null;
		
		// Ensure user can view the job they are requesting the pairs from
		if(false == Permissions.canUserSeeJob(jobId, userId)){
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		
		chartPath=Statistics.makeSolverComparisonChart(jobId,config1,config2,jobSpaceId,large);
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
	@Path("/jobs/{id}/solvers/pagination/{jobSpaceId}")
	@Produces("application/json")
	public String getJobStatsPaginated(@PathParam("id") int jobId, @PathParam("jobSpaceId") int jobSpaceId, @Context HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		if (!Permissions.canUserSeeJob(jobId, userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		//no restrictions for now, as now that we are caching results we should probably just see how far we can push that
		//if (Jobs.getJobPairCountInJobSpace(jobId, jobSpaceId, true, true)>R.MAXIMUM_JOB_PAIRS) {
		//	return gson.toJson(ERROR_TOO_MANY_JOB_PAIRS);
		//}
		List<SolverStats> stats=Jobs.getAllJobStatsInJobSpaceHierarchy(jobId, jobSpaceId);
		nextDataTablesPage=RESTHelpers.convertSolverStatsToJsonObject(stats, stats.size(), stats.size(),1,jobSpaceId,jobId);

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
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForClusterExplorer("queue", id, userId, request);

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
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
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
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		// Ensure user can view the space containing the primitive(s)
		if(false == Permissions.canUserSeeSpace(spaceId, userId)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
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
		User user = SessionUtil.getUser(request);
		List<Space> communities = Communities.getAll();
		for (Space s : communities) {
			if (spaceId == s.getId()) {
				if (user.getRole().equals("admin")) {
					return gson.toJson(Permissions.get(userId, spaceId));
				} else {
					return gson.toJson(1);
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
		return gson.toJson(0);
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
			return gson.toJson(Websites.getAll(userId, Websites.WebsiteType.USER));
		} else if(type.equals("space")){
			return gson.toJson(Websites.getAll(id, Websites.WebsiteType.SPACE));
		} else if (type.equals("solver")) {
			return gson.toJson(Websites.getAll(id, Websites.WebsiteType.SOLVER));
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
		
		if (type.equals("user")) {
			int userId = SessionUtil.getUserId(request);
			String name = request.getParameter("name");
			String url = request.getParameter("url");			
			success = Websites.add(userId, url, name, Websites.WebsiteType.USER);
		} else if (type.equals("space")) {
			// Make sure this user is capable of adding a website to the space
			Permission perm = SessionUtil.getPermission(request, id);
			if(perm != null && perm.isLeader()) {
				String name = request.getParameter("name");
				String url = request.getParameter("url");		
				log.debug("adding website [" + url + "] to space [" + id + "] under the name [" + name + "].");
				success = Websites.add(id, url, name, Websites.WebsiteType.SPACE);
			}
		} else if (type.equals("solver")) {
			//Make sure this user is the solver owner
			Solver s = Solvers.get(id);
			if (s.getUserId() == SessionUtil.getUserId(request)) {
				String name = request.getParameter("name");
				String url = request.getParameter("url");
				success = Websites.add(id, url, name, Websites.WebsiteType.SOLVER);
			}
		}
		
		// Passed validation AND Database update successful	
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
	@Path("/websites/delete/{type}/{id}/{websiteId}")
	@Produces("application/json")
	public String deleteWebsite(@PathParam("type") String type, @PathParam("id") int id, @PathParam("websiteId") int websiteId, @Context HttpServletRequest request) {
		
		if(type.equals("user")){
			return Websites.delete(websiteId, SessionUtil.getUserId(request), Websites.WebsiteType.USER) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		} else if (type.equals("space")){
			// Permissions check; ensures the user deleting the website is a leader
			Permission perm = SessionUtil.getPermission(request, id);		
			if(perm == null || !perm.isLeader()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);	
			}
			
			return Websites.delete(websiteId, id, Websites.WebsiteType.SPACE) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		} else if (type.equals("solver")) {
			Solver s = Solvers.get(id);
			if (s.getUserId() == SessionUtil.getUserId(request)) {
				return Websites.delete(websiteId, id, Websites.WebsiteType.SOLVER) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
			}
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		} 
		
		return gson.toJson(ERROR_INVALID_WEBSITE_TYPE);
	}
	
	@POST
	@Path("/edit/user/quota/{userId}/{val}")
	@Produces("application/json")
	//TODO: We need to do a permissions check to make sure only an admin can do this.
	public String editUserDiskQuota(@PathParam("userId") int userId,@PathParam("val") long newQuota, @Context HttpServletRequest request) {
		int u=SessionUtil.getUserId(request);
		return gson.toJson(ERROR_DATABASE);
		//boolean success=Users.setDiskQuota(userId, newQuota);
		
		//return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
	@Path("/edit/user/{attr}/{val}")
	@Produces("application/json")
	public String editUserInfo(@PathParam("attr") String attribute, @PathParam("val") String newValue, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		boolean success = false;
		
		// Go through all the cases, depending on what attribute we are changing.
		// First, validate that it is in legal form. Then, try to update the database.
		// Finally, update the current session data
		if (attribute.equals("firstname")) {
			if (true == Validator.isValidUserName(newValue)) {
				success = Users.updateFirstName(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setFirstName(newValue);
				}
			}
		} else if (attribute.equals("lastname")) {
			if (true == Validator.isValidUserName(newValue)) {
				success = Users.updateLastName(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setLastName(newValue);
				}
			}
		}  else if (attribute.equals("institution")) {
			if (true == Validator.isValidInstitution(newValue)) {
				success = Users.updateInstitution(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setInstitution(newValue);
				}
			}
		}
		// Passed validation AND Database update successful
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
	public String editSpaceDetails(@PathParam("attr") String attribute, @PathParam("id") int id, @Context HttpServletRequest request) {	
		
		
		try {
			Permission perm = SessionUtil.getPermission(request, id);		
			if(perm == null || !perm.isLeader()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);	
			}
			
			if(Util.isNullOrEmpty((String)request.getParameter("val"))){
				return gson.toJson(ERROR_EDIT_VAL_ABSENT);
			}
			
			boolean success = false;
			Space s=Spaces.get(id);
			// Go through all the cases, depending on what attribute we are changing.
			if (attribute.equals("name")) {
				String newName = (String)request.getParameter("val");
				if (true == Validator.isValidPrimName(newName)) {
					if (!s.getName().equals(newName)) {
						if (Spaces.notUniquePrimitiveName(newName,id,4)) {
							return gson.toJson(ERROR_NOT_UNIQUE_NAME);
						}
					}
					success = Spaces.updateName(id, newName);
				}
			} else if (attribute.equals("description")) {
				String newDesc = (String)request.getParameter("val");
				if (true == Validator.isValidPrimDescription(newDesc)) {
					success = Spaces.updateDescription(id, newDesc);				
				}
			} else if (attribute.equals("PostProcess")) {
				success = Communities.setDefaultSettings(id, 1, Integer.parseInt(request.getParameter("val")));
			}else if (attribute.equals("CpuTimeout")) {
				success = Communities.setDefaultSettings(id, 2, Integer.parseInt(request.getParameter("val")));			
			}else if (attribute.equals("ClockTimeout")) {
				success = Communities.setDefaultSettings(id, 3, Integer.parseInt(request.getParameter("val")));			
			} else if (attribute.equals("DependenciesEnabled")) {
				success = Communities.setDefaultSettings(id, 4, Integer.parseInt(request.getParameter("val")));
			} else if (attribute.equals("defaultBenchmark")) {
				success=Communities.setDefaultSettings(id, 5, Integer.parseInt(request.getParameter("val")));
			}
			
			// Passed validation AND Database update successful
			return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
				|| !Util.paramExists("locked", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				|| !Validator.isValidBool(request.getParameter("locked"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		Space os=Spaces.get(id);
		if (!os.getName().equals(request.getParameter("name"))) {
			if (Spaces.notUniquePrimitiveName(request.getParameter("name"),id,4)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}
		
		// Permissions check; if user is NOT a leader of the space, deny update request
		int userId = SessionUtil.getUserId(request);
		Permission perm = Permissions.get(userId, id);
		if(perm == null || !perm.isLeader()){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		// Extract new space details from request and add them to a new space object
		
		Space s = new Space();
		s.setId(id);
		s.setName(request.getParameter("name"));
		s.setDescription(request.getParameter("description"));
		s.setLocked(Boolean.parseBoolean(request.getParameter("locked")));
		
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
		return Spaces.updateDetails(userId, s) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Post-processes an already-complete job with a new post processor
	 * 
	 * @return a json string with result status (0 for success, otherwise 1)
	 * @author Eric Burns
	 */
	@POST
	@Path("/postprocess/job/{jobId}/{procId}")
	@Produces("application/json")
	public String postProcessJob(@PathParam("jobId") int jid, @PathParam("procId") int pid, @Context HttpServletRequest request) {
		
		int userId=SessionUtil.getUserId(request);
		Job job=Jobs.get(jid);
		if (job.getUserId()!=userId) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		Processor p=Processors.get(pid);
		if (!Users.isMemberOfCommunity(userId, p.getCommunityId())) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		if (!Jobs.canJobBePostProcessed(jid)) {
			return gson.toJson(ERROR_JOB_INCOMPLETE);
		}
		log.debug("post process request with jobId = "+jid+" and processor id = "+pid);
		
		return Jobs.prepareJobForPostProcessing(jid,pid)? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Post-processes an already-complete job with a new post processor
	 * 
	 * @return a json string with result status (0 for success, otherwise 1)
	 * @author Eric Burns
	 
	@GET
	@Path("/postprocess/job/{jobId}/cancel")
	@Produces("application/json")
	public String cancelOostProcessJob(@PathParam("jobId") int jid, @Context HttpServletRequest request) {
		
		int userId=SessionUtil.getUserId(request);
		Job job=Jobs.get(jid);
		if (job.getUserId()!=userId) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		if (Jobs.getJobStatusCode(jid).getCode()!=JobStatusCode.STATUS_PROCESSING) {
			return gson.toJson(ERROR_JOB_NOT_PROCESSING);
		}
		
		
		log.debug("post process request with jobId = "+jid+" and processor id = "+pid);
		
		return Jobs.cancelPostProcessing(jid)? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}*/
	
	/**
	 * Removes a benchmark type from a given space
	 * 
	 * @return a json string containing '0' if the deletion was successful, else
	 *         a json string containing '1' if there was a failure, '2' for
	 *         insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/processor/{procId}")
	@Produces("application/json")
	public String deleteProcessor(@PathParam("procId") int pid, @Context HttpServletRequest request) {
		Processor p = Processors.get(pid);
		
		// Permissions check; ensures user is the leader of the community that owns the processor
		Permission perm = SessionUtil.getPermission(request, p.getCommunityId());		
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		String answer= Processors.delete(pid) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		
		try {
			if (Util.paramExists("cid",request) && Util.paramExists("defaultPP",request)) {
				int cid=Integer.parseInt(request.getParameter("cid"));
				int defaultPP=Integer.parseInt(request.getParameter("defaultPP"));
				//The processor that was deleted was the default, so we'll set a new default 
				if (defaultPP==pid) {
					List<Processor> procs=Processors.getByCommunity(cid, ProcessorType.POST);
					if (procs.size()>0) {
						boolean success=Communities.setDefaultSettings(cid, 1, procs.get(0).getId());
						if (success) {
							log.debug("Default post processor was deleted, and a new default was selected");
						} else {
							log.warn("Default post processor was deleted, but a new default could not be selected");
						}
					}
				}
			}
		} catch (Exception e) {
			// we couldn't set a new default PP
			log.warn("Default post processor was deleted, but a new default could not be selected due to an error",e);
		}
		return answer;
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
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedProcessors = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedProcessors.add(Integer.parseInt(id));
		}
		
		for (int id : selectedProcessors) {
			Processor p = Processors.get(id);
			
			// Permissions check; ensures user is the leader of the community that owns the processor
			Permission perm = SessionUtil.getPermission(request, p.getCommunityId());		
			if(perm == null || !perm.isLeader()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);	
			}
			
			if (!Processors.delete(id)) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(0);
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
		
		return gson.toJson(0);
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
		return gson.toJson(0);
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
		return gson.toJson(0);
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
		return gson.toJson(0);
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
	@Produces("applicatoin/json")
	public String editProcessor(@PathParam("procId") int pid, @Context HttpServletRequest request) {
		Processor p=Processors.get(pid);
		Permission perm= SessionUtil.getPermission(request, p.getCommunityId());
		if (perm==null || !perm.isLeader()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("desc", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		String name=request.getParameter("name");
		String desc=request.getParameter("desc");
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(name)
				|| !Validator.isValidPrimDescription(desc)){
			return gson.toJson(ERROR_INVALID_PARAMS);
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
		
		return gson.toJson(0);
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
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		if(true == Communities.leave(SessionUtil.getUserId(request), spaceId)) {
			// Delete prior entry in user's permissions cache for this community
			SessionUtil.removeCachePermission(request, spaceId);
			return gson.toJson(0);
		}
		
		
		return gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes a benchmark's association to a space, thereby removing the
	 * benchmark from the space
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
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String bench id's and convert them to Integer
		ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedBenches.add(Integer.parseInt(id));
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveBench()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}

		// Remove the benchmark from the space
		return Spaces.removeBenches(selectedBenches, spaceId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
	@Path("/recycleandremove/benchmark/{spaceID}")
	@Produces("application/json")
	public String recycleAndRemoveBenchmarks(@Context HttpServletRequest request,@PathParam("spaceID") int spaceId) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if (!perm.canRemoveBench()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Extract the String bench id's and convert them to Integer
		ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedBenches.add(Integer.parseInt(id));
		}
		int userId=SessionUtil.getUserId(request);
		User user = Users.get(userId);
		for (int id : selectedBenches) {
			log.debug("user.getRole() = " + user.getRole());
			if (!user.getRole().equals("admin")) {
				if(userId!=Benchmarks.get(id).getUserId()) {
					return gson.toJson(ERROR_INVALID_PERMISSIONS);	
				}
			}
			
			boolean success=Benchmarks.recycle(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		Spaces.removeBenches(selectedBenches, spaceId);
		return gson.toJson(0);
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
		for (int id : selectedBenches) {
			
			if(userId!=Benchmarks.get(id).getUserId()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);	
			}
			boolean success=Benchmarks.recycle(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(0);
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
			for (int id : selectedBenches) {
				
				if(userId!=Benchmarks.getIncludeDeletedAndRecycled(id,false).getUserId()) {
					return gson.toJson(ERROR_INVALID_PERMISSIONS);	
				}
				boolean success=Benchmarks.delete(id);
				if (!success) {
					return gson.toJson(ERROR_DATABASE);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return gson.toJson(0);
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
			for (int id : selectedBenches) {
				
				if(userId!=Benchmarks.getIncludeDeletedAndRecycled(id,false).getUserId()) {
					return gson.toJson(ERROR_INVALID_PERMISSIONS);	
				}
				boolean success=Benchmarks.restore(id);
				if (!success) {
					return gson.toJson(ERROR_DATABASE);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return gson.toJson(0);
	}
	
	
	
	/**
	 * Associates (i.e. 'copies') a user from one space into another
	 * 
	 * @param	spaceId the id of the destination space we are copying to
	 * @param	request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of users to copy as well as a 'fromSpace' parameter that is the
	 * space the users are being copied from.
	 * @return 	0: success,<br>
	 * 			1: database failure,<br>
	 * 			2: missing parameters,<br>
	 * 			3: no add user permission in destination space,<br>
	 * 			4: user doesn't belong to the 'from space',<br>
	 * 			5: the 'from space' is locked
	 * @author Tyler Jensen & Todd Elvers
	 */
	@POST
	@Path("/spaces/{spaceId}/add/user")
	@Produces("application/json")
	public String copyUserToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of users to add, the id of the space it's coming from, and whether or not to apply this to all subspaces 
		if(null == request.getParameterValues("selectedIds[]") 
				|| !Util.paramExists("fromSpace", request)
				|| !Util.paramExists("copyToSubspaces", request)
				|| !Validator.isValidBool(request.getParameter("copyToSubspaces"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the user is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Get the flag that indicates whether or not to copy this solver to all subspaces of 'fromSpace'
		boolean copyToSubspaces = Boolean.parseBoolean(request.getParameter("copyToSubspaces"));
		
		// Check permissions, the user must have add user permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddUser()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(ERROR_NOT_IN_SPACE);
		}			
		
		// And the space the user is being copied from must not be locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(ERROR_SPACE_LOCKED);
		}
		
		// Convert the users to copy to a int list
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		for (int id : selectedUsers) {
			if (!Users.isMemberOfSpace(id, fromSpace)) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
		}
		// Either copy the solvers to the destination space or the destination space and all of its subspaces (that the user can see)
		if (copyToSubspaces == true) {
			int subspaceId;
			List<Space> subspaces = Spaces.trimSubSpaces(requestUserId, Spaces.getSubSpaces(spaceId, requestUserId, true));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces to associate the user(s) with
			subspaceIds.add(spaceId);
			
			// Iterate once through all subspaces of the destination space to ensure the user has addUser permissions in each
			for(Space subspace : subspaces){
				subspaceId = subspace.getId();
				Permission subspacePerm = Permissions.get(requestUserId, subspaceId);	
				if(subspacePerm == null || !subspacePerm.canAddUser()) {
					return gson.toJson(ERROR_CANT_LINK_TO_SUBSPACE);	
				}			
				subspaceIds.add(subspaceId);
			}
			
			
			// Add the user(s) to the destination space and its subspaces
			return Users.associate(selectedUsers, subspaceIds) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		} else {
			// Add the user(s) to the destination space
			
			return Users.associate(selectedUsers, spaceId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		}
	}

	/**
	 * Associates (i.e. 'copies') a solver from one space into another space and, if specified by the client,
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
	
	public String copySolverToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of solvers to add, the id of the space it's coming from, and whether or not to apply this to all subspaces 
		if(null == request.getParameterValues("selectedIds[]") 
				|| !Util.paramExists("fromSpace", request)
				|| !Util.paramExists("copyToSubspaces", request)
				|| !Util.paramExists("copy", request)
				|| !Validator.isValidBool(request.getParameter("copyToSubspaces"))
				|| !Validator.isValidBool(request.getParameter("copy"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the solver is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Get the flag that indicates whether or not to copy this solver to all subspaces of 'fromSpace'
		boolean copyToSubspaces = Boolean.parseBoolean(request.getParameter("copyToSubspaces"));
		
		//Get the flag that indicates whether the solver is being copied or linked
		boolean copy=Boolean.parseBoolean(request.getParameter("copy"));
		// Convert the solvers to copy to an int list
		List<Integer> selectedSolvers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		
		// Verify the space the solvers are being copied from is not locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(ERROR_SPACE_LOCKED);
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(ERROR_NOT_IN_SPACE);
		}	

		// Make sure the user can see the solver they're trying to copy
		for (int id : selectedSolvers) {
			if (!Permissions.canUserSeeSolver(id, requestUserId)) {
				return gson.toJson(ERROR_NOT_IN_SPACE);
			}
			
			if (Solvers.isSolverDeleted(id)) {
				return gson.toJson(ERROR_PRIM_ALREADY_DELETED);
			}
			// Make sure that the solver has a unique name in the space.
			if(Spaces.notUniquePrimitiveName(Solvers.get(id).getName(), spaceId, 1)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}
		
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddSolver()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}			
		
		if (copy) {
			List<Solver> oldSolvers=Solvers.get(selectedSolvers);
			//first, validate that the user has enough disk quota to copy all the selected solvers
			//we don't copy any unless they have room for all of them
			long userDiskUsage=Users.getDiskUsage(requestUserId);
			long userDiskQuota=Users.get(requestUserId).getDiskQuota();
			userDiskQuota-=userDiskUsage;
			for (Solver s : oldSolvers) {
				userDiskQuota-=s.getDiskSize();
			}
			if (userDiskQuota<0) {
				
				return gson.toJson(ERROR_INSUFFICIENT_QUOTA);
			}
			
			List<Integer>newSolverIds=new ArrayList<Integer>();
			int newID;
			for (Solver s : oldSolvers) {
				newID=Solvers.copySolver(s, requestUserId, spaceId);
				
				if (newID==-1) {
					log.error("Unable to copy solver "+s.getName());
					return gson.toJson(ERROR_DATABASE);
				} else {
					newSolverIds.add(newID);
				}
			}
			selectedSolvers=newSolverIds;
		}
		// Either copy the solvers to the destination space or the destination space and all of its subspaces (that the user can see)
		if (copyToSubspaces == true) {
			int subspaceId;
			
			List<Space> subspaces = Spaces.trimSubSpaces(requestUserId, Spaces.getSubSpaces(spaceId, requestUserId, true));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces to associate the solvers with only
			//if we aren't copying. If we're copying, we did this already
			if (!copy) {
				subspaceIds.add(spaceId);
			}
			
			
			// Iterate once through all subspaces of the destination space to ensure the user has addSolver permissions in each
			for(Space subspace : subspaces){
				subspaceId = subspace.getId();
				Permission subspacePerm = SessionUtil.getPermission(request, subspaceId);	
				
				if(subspacePerm == null || !subspacePerm.canAddSolver()) {
					return gson.toJson(ERROR_CANT_LINK_TO_SUBSPACE);	
				}			
				subspaceIds.add(subspace.getId());
			}

			// Add the solvers to the destination space and its subspaces
			return Solvers.associate(selectedSolvers, subspaceIds) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		} else {
			// Add the solvers to the destination space
			return Solvers.associate(selectedSolvers, spaceId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		}
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
	public String copyBenchToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") 
				|| !Util.paramExists("fromSpace", request)
				|| !Util.paramExists("copy", request)
				|| !Validator.isValidBool(request.getParameter("copy"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Check permissions, the user must have add benchmark permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddBenchmark()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}			
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(ERROR_NOT_IN_SPACE);
		}			
		
		// And the space the solvers are being copied from must not be locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(ERROR_SPACE_LOCKED);
		}
		
		// Convert the benchmarks to copy to a int list
		List<Integer> selectedBenchs= Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the benchmarks they're trying to copy
		for(int id : selectedBenchs) {
			if(!Permissions.canUserSeeBench(id, requestUserId)) {
				return gson.toJson(ERROR_NOT_IN_SPACE);
			}
			if (Benchmarks.isBenchmarkDeleted(id)) {
				return gson.toJson(ERROR_PRIM_ALREADY_DELETED);
			}
			// Make sure that the benchmark has a unique name in the space.
			if(Spaces.notUniquePrimitiveName(Benchmarks.get(id).getName(), spaceId, 2)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}
		boolean copy=Boolean.parseBoolean(request.getParameter("copy"));
		if (copy) {
			List<Benchmark> oldBenchs=Benchmarks.get(selectedBenchs,true);
			long userDiskUsage=Users.getDiskUsage(requestUserId);
			long userDiskQuota=Users.get(requestUserId).getDiskQuota();
			userDiskQuota-=userDiskUsage;
			for (Benchmark b :oldBenchs) {
				userDiskQuota-=b.getDiskSize();
			}
			if (userDiskQuota<0) {
				return gson.toJson(ERROR_INSUFFICIENT_QUOTA);
			}
			int benchId=-1;
			for (Benchmark b : oldBenchs) {
				benchId=Benchmarks.copyBenchmark(b,requestUserId,spaceId);
				if (benchId<0) {
					log.error("Benchmark "+b.getName()+" could not be copied successfully");
					return gson.toJson(ERROR_DATABASE);
				}
				log.debug("Benchmark "+b.getName()+" copied successfully");
			}
			
			
			return gson.toJson(0);
			
		} else {
			// Make the associations
			boolean success = Benchmarks.associate(selectedBenchs, spaceId);
			
			// Return a value based on results from database operation
			return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Check permissions, the user must have add benchmark permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddJob()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}			
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(ERROR_NOT_IN_SPACE);
		}			
		
		// And the space the solvers are being copied from must not be locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(ERROR_SPACE_LOCKED);
		}
		
		// Convert the benchmarks to copy to a int list
		List<Integer> selectedJobs = Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the benchmarks they're trying to copy
		for(int id : selectedJobs) {
			if(!Permissions.canUserSeeJob(id, requestUserId)) {
				return gson.toJson(ERROR_NOT_IN_SPACE);
			}
			
			// Make sure that the job has a unique name in the space.
			if(Spaces.notUniquePrimitiveName(Jobs.getDetailed(id).getName(), spaceId, 3)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}		
		
		// Make the associations
		boolean success = Jobs.associate(selectedJobs, spaceId);
		
		// Return a value based on results from database operation
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Removes a user's association with a space, whereby removing them from a
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
		User u = Users.get(userIdOfRemover);
		
		Permission perm = new Permission();
		if (u.getRole().equals("admin")) {
			log.debug("Returning admin user permissions");
			perm=Permissions.getFullPermission();
			perm.setId(userIdOfRemover);
			
		} else {
			// Permissions check; ensures user is the leader of the community
			perm = SessionUtil.getPermission(request, spaceId);
		}
		if(perm == null || !perm.canRemoveUser()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		// Extract the String user id's and convert them to Integer
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		
		if (!u.getRole().equals("admin")) {
			// Validate the list of users to remove by:
			// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the list of users to remove
			// 2 - Ensuring other leaders of the space aren't in the list of users to remove
			for(int userId : selectedUsers){
				if(userId == userIdOfRemover){
					return gson.toJson(ERROR_CANT_REMOVE_SELF);
				}
				perm = Permissions.get(userId, spaceId);
				if(perm.isLeader()){
					return gson.toJson(ERROR_CANT_REMOVE_LEADER);
				}
			}
		}

		
		// If array of users to remove is valid, attempt to remove them from the space
		
		// If we are "cascade removing" the user(s)...
		if (true == Boolean.parseBoolean(request.getParameter("hierarchy"))) {
			int subspaceId;
			List<Space> subspaces = Spaces.trimSubSpaces(userIdOfRemover, Spaces.getSubSpaces(spaceId, userIdOfRemover, true));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces remove the user from
			subspaceIds.add(spaceId);
			
			// Iterate once through all subspaces of the destination space to ensure the user has removeUser permissions in each
			for(Space subspace : subspaces) {
				subspaceId = subspace.getId();
				Permission subspacePerm = SessionUtil.getPermission(request, subspaceId);		
				if (subspacePerm != null && !subspacePerm.canRemoveUser()) { // Null if we don't belong to that space; that's ok! we skip it
					return gson.toJson(ERROR_INVALID_PERMISSIONS);	
				}
				for (int userId : selectedUsers) {
					// Make sure the users you are trying to remove are not leaders in this subspace
					perm = Permissions.get(userId, subspace.getId());
					if (perm != null && perm.isLeader()) {
						return gson.toJson(ERROR_CANT_REMOVE_LEADER_IN_SUBSPACE);
					}
				subspaceIds.add(subspaceId);
				}
			}
			
			// Remove the users from the space and its subspaces
			return Spaces.removeUsersFromHierarchy(selectedUsers, subspaceIds) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
		}
		
		// Otherwise...
		return Spaces.removeUsers(selectedUsers, spaceId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveSolver()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}

		// Passed validation, now remove solver(s) from space(s)

		// If we are "cascade removing" the solver(s)...
		if (true == Boolean.parseBoolean(request.getParameter("hierarchy"))) {
			int subspaceId;
			List<Space> subspaces = Spaces.trimSubSpaces(userIdOfRemover, Spaces.getSubSpaces(spaceId, userIdOfRemover, true));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces remove the user from
			subspaceIds.add(spaceId);
			
			// Iterate once through all subspaces of the destination space to ensure the user has removeSolver permissions in each
			for(Space subspace : subspaces){
				subspaceId = subspace.getId();
				Permission subspacePerm = SessionUtil.getPermission(request, subspaceId);		
				if (subspacePerm != null && !subspacePerm.canRemoveSolver()) { // Null if we don't belong to that space; that's ok! we skip it
					return gson.toJson(ERROR_CANT_REMOVE_FROM_SUBSPACE);	
				} 
				
				subspaceIds.add(subspaceId);
			}
			
			return Spaces.removeSolversFromHierarchy(selectedSolvers, subspaceIds) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);

		}
		
		// Otherwise...
		return Spaces.removeSolvers(selectedSolvers, spaceId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Deletes a list of solvers and removes them  from the given space
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
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if (!perm.canRemoveSolver()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		User user = Users.get(userIdOfRemover);
		for (int id : selectedSolvers) {
			if (!user.getRole().equals("admin")) {
				if (userIdOfRemover!=Solvers.get(id).getUserId()) {
					return gson.toJson(ERROR_INVALID_PERMISSIONS);
				}
			}
			boolean success=Solvers.recycle(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		Spaces.removeSolvers(selectedSolvers, spaceId);
		return gson.toJson(0);
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
	@Path("/restore/solver")
	@Produces("application/json")
	public String restoreSolvers(@Context HttpServletRequest request) {
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		for (int id : selectedSolvers) {
			if (userIdOfRemover!=Solvers.getIncludeDeleted(id).getUserId()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
			
			boolean success=Solvers.restore(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(0);
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
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		for (int id : selectedSolvers) {
			if (userIdOfRemover!=Solvers.getIncludeDeleted(id).getUserId()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
			
			boolean success=Solvers.delete(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(0);
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
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedIds[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		User user = Users.get(userIdOfRemover);
		for (int id : selectedSolvers) {
			if (!user.getRole().equals("admin")) {
				if (userIdOfRemover!=Solvers.get(id).getUserId()) {
					return gson.toJson(ERROR_INVALID_PERMISSIONS);
				}
			}
			
			boolean success=Solvers.recycle(id);
			if (!success) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		return gson.toJson(0);
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
		
		for (int id : selectedConfigs) {
			// Validate configuration id parameter
			Configuration config = Solvers.getConfiguration(id);
			if(null == config){
				return gson.toJson(ERROR_DATABASE);
			}
			
			// Permissions check; if user is NOT the owner of the configuration file's solver, deny deletion request
			Solver solver = Solvers.get(config.getSolverId());
			if(null == solver || solver.getUserId() != userId){
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
			
			// Attempt to remove the configuration's physical file from disk
			if(false == Solvers.deleteConfigurationFile(config)){
				return gson.toJson(ERROR_DATABASE);
			}
			
			// Attempt to remove the configuration's entry in the database
			if(false == Solvers.deleteConfiguration(id)){
				return gson.toJson(ERROR_DATABASE);
			}
			
			// Attempt to update the disk_size of the parent solver to reflect the file deletion
			if(false == Solvers.updateSolverDiskSize(solver)){
				return gson.toJson(ERROR_DATABASE);
			}
		}
		
		
		return gson.toJson(0);
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
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Extract the String job id's and convert them to Integer
		ArrayList<Integer> selectedJobs = new ArrayList<Integer>();
		for (String id : request.getParameterValues("selectedIds[]")) {
			selectedJobs.add(Integer.parseInt(id));
		}

		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);
		if (perm == null || !perm.canRemoveJob()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}

		// Remove the job from the space
		return Spaces.removeJobs(selectedJobs, spaceId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedIds[]")){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		Permission perm=SessionUtil.getPermission(request, spaceId);
		if  (!perm.canRemoveJob()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Extract the String job id's and convert them to Integer
		ArrayList<Integer> selectedJobs = new ArrayList<Integer>();
		for (String id : request.getParameterValues("selectedIds[]")) {
			selectedJobs.add(Integer.parseInt(id));
			log.debug("adding id = "+id+" to selectedJobs");
		}
		int userId=SessionUtil.getUserId(request);
		for (int id : selectedJobs) {
			if (userId!=Jobs.get(id).getUserId()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
			log.debug("the current job ID to remove = "+id);
			//first kill a job, then delete it. Killing it first ensures no additional job pairs are run
			//after the deletion
			boolean success_kill = Jobs.kill(id);
			boolean success_delete = Jobs.delete(id);
			if (!success_delete || !success_kill) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
		Spaces.removeJobs(selectedJobs, spaceId);
		return gson.toJson(0);
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
		for (int id : selectedJobs) {
			if (userId!=Jobs.get(id).getUserId()) {
				return gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
			//first kill a job, then delete it. Killing it first ensures no additional job pairs are run
			//after the deletion
			boolean success_kill = Jobs.kill(id);
			boolean success_delete = Jobs.delete(id);
			if (!success_delete || !success_kill) {
				return gson.toJson(ERROR_DATABASE);
			}
		}
	
		return gson.toJson(0);
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
	@Path("/remove/subspace/{spaceId}")
	@Produces("application/json")
	public String removeSubspacesFromSpace(@PathParam("spaceId") int parentSpaceId, @Context HttpServletRequest request) {
		ArrayList<Integer> selectedSubspaces = new ArrayList<Integer>();
		try{
			// Extract the String subspace id's and convert them to Integers
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedSubspaces.add(Integer.parseInt(id));
			}
		} catch(Exception e){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Permissions check; ensures user is the leader of the space
		Permission perm = SessionUtil.getPermission(request, parentSpaceId);		
		if(null == perm || !perm.isLeader()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		boolean recycleAllAllowed=false;
		if (Util.paramExists("deletePrims", request)) {
			if (Boolean.parseBoolean(request.getParameter("deletePrims"))) {
				log.debug("Request to delete all solvers, benchmarks, and jobs in a hierarchy received");
				recycleAllAllowed=true;
			}
			
		}
		Set<Solver> solvers=new HashSet<Solver>();
		Set<Benchmark> benchmarks=new HashSet<Benchmark>();
		int userId=SessionUtil.getUserId(request);
		if (recycleAllAllowed) {
			for (int sid : selectedSubspaces) {
				solvers.addAll(Solvers.getBySpace(sid));
				benchmarks.addAll(Benchmarks.getBySpace(sid));
			}
		}
		// Remove the subspaces from the space
		boolean deletionFailed=false;
		if (Spaces.removeSubspaces(selectedSubspaces, parentSpaceId, SessionUtil.getUserId(request))) {
			if (recycleAllAllowed) {
				log.debug("Space removed successfully, deleting primitives");
				for (Solver s : solvers) {
					if (s.getUserId()==userId) {
						if (!Solvers.recycle(s.getId())) {
							log.error("Failed to recycle solver with id = "+s.getId());
							deletionFailed=true;
						}
					}
				}
				
				for (Benchmark b : benchmarks) {
					if (b.getUserId()==userId) {
						if (!Benchmarks.recycle(b.getId())) {
							log.error("Failed to recycle benchmark with id = "+b.getId());
							deletionFailed=true;
						}
					}
				}
				
			}
			if (!deletionFailed) {
				return gson.toJson(0);
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
	 * @param parentSpaceId the id the space to remove the subspace from
	 * @return 	0: success,<br>
	 * 			1: invalid parameters,<br>
	 * 			2: insufficient permissions,<br>
	 * 			3: error on the database level
	 * @author Ben McCune
	 */
	@POST
	@Path("/quickRemove/subspace/{spaceId}")
	@Produces("application/json")
	public String quickRemoveSubspacesFromSpace(@PathParam("spaceId") int parentSpaceId, @Context HttpServletRequest request) {
		ArrayList<Integer> selectedSubspaces = new ArrayList<Integer>();
		log.debug("quickRemove called from " + parentSpaceId);
		try{
			// Extract the String subspace id's and convert them to Integers
			for(String id : request.getParameterValues("selectedIds[]")){
				selectedSubspaces.add(Integer.parseInt(id));
			}
		} catch(Exception e){
			return gson.toJson(ERROR_IDS_NOT_GIVEN);
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, parentSpaceId);		
		if(null == perm || !perm.isLeader()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		// Remove the associations
		
		return Spaces.quickRemoveSubspaces(selectedSubspaces, parentSpaceId, SessionUtil.getUserId(request)) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("downloadable", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				|| !Validator.isValidBool(request.getParameter("downloadable"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Permissions check; if user is NOT the owner of the solver, deny update request
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		if(solver == null || solver.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		
		
		// Extract new solver details from request
		String name = request.getParameter("name");
		//if the name is actually being changed
		if (!solver.getName().equals(name)) {
			int id=Solvers.isNameEditable(solverId);
			if (id<0) {
				return gson.toJson(ERROR_NAME_NOT_EDITABLE);
			}
			
			if (id>0 && Spaces.notUniquePrimitiveName(name,id, 1)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}
		
		
		
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		
		// Apply new solver details to database
		return Solvers.updateDetails(solverId, name, description, isDownloadable) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}

	/**
	 * Deletes a solver given a solver's id. The id of the solver to delete must
	 * be included in the path.
	 * 
	 * @param id the id of the solver to delete
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/recycle/solver/{id}")
	@Produces("application/json")
	public String recycleSolver(@PathParam("id") int solverId, @Context HttpServletRequest request) {
		
		// Permissions check; if user is NOT the owner of the solver, deny deletion request
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		User user = Users.get(userId);
		if (!user.getRole().equals("admin") || solver == null) {
			if(solver == null || solver.getUserId() != userId){
				gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
		}
		
		return Solvers.recycle(solverId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Deletes a job given a job's id. The id of the job to delete must
	 * be included in the path.
	 * 
	 * @param id the id of the job to delete
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/job/{id}")
	@Produces("application/json")
	public String deleteJob(@PathParam("id") int jobId, @Context HttpServletRequest request) {
		
		// Permissions check; if user is NOT the owner of the job, deny deletion request
		int userId = SessionUtil.getUserId(request);
		Job j = Jobs.get(jobId);
		if(j == null || j.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.delete(jobId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		Job j = Jobs.get(jobId);
		if(j == null || j.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.pause(jobId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		Job j = Jobs.get(jobId);
		if(j == null || j.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.resume(jobId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		Job j = Jobs.get(jobId);
		if(j == null || j.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return Jobs.changeQueue(jobId, queueId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	

	/**
	 * Updates the details of a benchmark. Benchmark id is required in the path.
	 * First checks if the parameters of the update are valid, then performs the
	 * update.
	 * 
	 * @param id the id of the benchmark to update the details for
	 * @return 	0: success,<br>
	 * 			1: error on the database level,<br>
	 * 			2: insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/recycle/benchmark/{id}")
	@Produces("application/json")
	public String recycleBenchmark(@PathParam("id") int benchId, @Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the benchmark, deny deletion request
		int userId = SessionUtil.getUserId(request);		
		Benchmark bench = Benchmarks.get(benchId);
		User user = Users.get(userId);
		if (!user.getRole().equals("admin") || bench == null) {
			if(bench == null || bench.getUserId() != userId){
				gson.toJson(ERROR_INVALID_PERMISSIONS);
			}
		}
		
		// Delete the benchmark from the database
		return Benchmarks.recycle(benchId) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}

	/**
	 * Deletes a benchmark given a benchmarks's id. The id of the benchmark to
	 * delete must be included in the path.
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
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				|| !Validator.isValidBool(request.getParameter("downloadable"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Permissions check; if user is NOT the owner of the benchmark, deny update request
		int userId = SessionUtil.getUserId(request);
		Benchmark bench = Benchmarks.get(benchId);
		if(bench == null || bench.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		// Extract new benchmark details from request
		String name = request.getParameter("name");
		// Extract new solver details from request
		if (!bench.getName().equals(name)) {
			int id=Benchmarks.isNameEditable(benchId);
			if (id<0) {
				return gson.toJson(ERROR_NAME_NOT_EDITABLE);
			}
			if (id>0 && Spaces.notUniquePrimitiveName(name,id, 2)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		
		// Apply new benchmark details to database
		return Benchmarks.updateDetails(benchId, name, description, isDownloadable, type) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
	@Path("/edit/user/password/")
	@Produces("application/json")
	public String editUserPassword(@Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		String currentPass = request.getParameter("current");
		String newPass = request.getParameter("newpass");
		String confirmPass = request.getParameter("confirm");
				
		String hashedPass = Hash.hashPassword(currentPass);
		String databasePass = Users.getPassword(userId);
		
		if (hashedPass.equals(databasePass)) {
			if (newPass.equals(confirmPass)) {
				if (true == Validator.isValidPassword(newPass)) {
					//updatePassword requires the plaintext password
					if (true == Users.updatePassword(userId, newPass)) {
						return gson.toJson(0);
					} else {
						return gson.toJson(ERROR_DATABASE); //Database operation returned false
					}
				} else {
					return gson.toJson(ERROR_INVALID_PASSWORD); //Validate operation returned false
				}
			} else {
				return gson.toJson(ERROR_PASSWORDS_NOT_EQUAL); //newPass != confirmPass
			}
		} else {
			return gson.toJson(ERROR_WRONG_PASSWORD); //hashedPass != databasePass
		}
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
		User currentUser = Users.get(currentUserId);
		Permission perm = SessionUtil.getPermission(request, spaceId);
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		// Ensure the user to edit the permissions of isn't themselves a leader
		perm = Permissions.get(userId, spaceId);
		if(perm.isLeader() && !currentUser.getRole().equals("admin") ){
			return gson.toJson(ERROR_CANT_EDIT_LEADER_PERMS);
		}		
		
		// Configure a new permission object
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
		
		// Update database with new permissions
		return Permissions.set(userId, spaceId, newPerm) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("contents", request)){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				||  request.getParameter("contents").isEmpty()){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Permissions check; if user is NOT the owner of the configuration file's solver, deny update request
		int userId = SessionUtil.getUserId(request);
		Configuration config = Solvers.getConfiguration(configId);
		Solver solver = Solvers.get(config.getSolverId());
		if(null == solver || solver.getUserId() != userId){
			gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		
		// Extract new configuration file details from request
		String name = (String) request.getParameter("name");
		String description = (String) request.getParameter("description");
		String contents = (String) request.getParameter("contents");
		
		// Apply new solver details to database
		return Solvers.updateConfigDetails(configId, name, description, contents) ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	
	/**
	 * Deletes a configuration from the database, from file, and updates the configuration's parent solver's
	 * disk quota to reflect the deletion 
	 *
	 * @param configId the id of the configuration to delete
	 * @param request the HttpRequestServlet object containing the user's id
	 * @return 0 if the configuration was successfully deleted,<br>
	 * 		1 if the configuration id is invalid,<br> 
	 * 		2 if the user has insufficient privileges to delete the configuration,<br> 
	 * 		3 if the configuration file fails to be deleted from disk,<br>
	 * 		4 if the configuration file fails to be deleted from the database,<br> 
	 * 		5 if the configuration file's parent solver's disk size fails to be updated
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/configuration/{id}")
	@Produces("application/json")
	public String deleteConfiguration(@PathParam("id") int configId, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);

		// Validate configuration id parameter
		Configuration config = Solvers.getConfiguration(configId);
		if(null == config){
			return gson.toJson(ERROR_DATABASE);
		}
		
		// Permissions check; if user is NOT the owner of the configuration file's solver, deny deletion request
		Solver solver = Solvers.get(config.getSolverId());
		if(null == solver || solver.getUserId() != userId){
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		// Attempt to remove the configuration's physical file from disk
		if(false == Solvers.deleteConfigurationFile(config)){
			return gson.toJson(ERROR_DATABASE);
		}
		
		// Attempt to remove the configuration's entry in the database
		if(false == Solvers.deleteConfiguration(configId)){
			return gson.toJson(ERROR_DATABASE);
		}
		
		// Attempt to update the disk_size of the parent solver to reflect the file deletion
		if(false == Solvers.updateSolverDiskSize(solver)){
			return gson.toJson(ERROR_DATABASE);
		}
		
		// If this point is reached, then the configuration has been completely removed
		return gson.toJson(0);
	}
	
	

	
	/**
	 * Make a list of users the leaders of a space
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
		if (!user.getRole().equals("admin")) {
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
		return gson.toJson(0);
	}
	
	/**
	 * Demotes a user from a leader to only a member in a community
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
	public String demoteLeader(@PathParam("spaceId") int spaceId, @PathParam("userId") int userId, @Context HttpServletRequest request) {		
				
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);	
		log.debug("perm = " + perm);
		if(perm == null) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}
		
		
		Permission p = new Permission();
		p.setAddBenchmark(true);
		p.setAddJob(true);
		p.setAddSolver(true);
		p.setAddSpace(true);
		p.setAddUser(true);
		p.setRemoveBench(true);
		p.setRemoveJob(true);
		p.setRemoveSolver(true);
		p.setRemoveSpace(true);
		p.setRemoveUser(true);
		p.setLeader(false);
		
		log.debug(userId);
		log.debug(spaceId);
		
		Permissions.set(userId, spaceId, p);
		return gson.toJson(0);
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
	public String copySubSpaceToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of solvers to add, the id of the space it's coming from, and whether or not to apply this to all subspaces 
		if(null == request.getParameterValues("selectedIds[]") 
				|| !Util.paramExists("fromSpace", request)
				|| !Util.paramExists("copyHierarchy", request)
				|| !Validator.isValidBool(request.getParameter("copyHierarchy"))){
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the subSpace is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Get the flag that indicates whether or not to copy this solver to all subspaces of 'fromSpace'
		boolean copyHierarchy = Boolean.parseBoolean(request.getParameter("copyHierarchy"));
		
		// Convert the subSpaces to copy to an int list
		List<Integer> selectedSubSpaces = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		
		// Verify the space the subspaces are being copied from is not locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(ERROR_SPACE_LOCKED);
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(ERROR_NOT_IN_SPACE);
		}	

		// Make sure the user can see the subSpaces they're trying to copy
		for (int id : selectedSubSpaces) {
			if (!Permissions.canUserSeeSpace(id, requestUserId)) {
				return gson.toJson(ERROR_NOT_IN_SPACE);
			}
			
			// Make sure that the subspace has a unique name in the space.
			if(Spaces.notUniquePrimitiveName(Spaces.get(id).getName(), spaceId, 4)) {
				return gson.toJson(ERROR_NOT_UNIQUE_NAME);
			}
		}
		
		// Check permissions - the user must have add space permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddSpace()) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);	
		}

		// Add the subSpaces to the destination space
		if (!copyHierarchy) {
			for (int id : selectedSubSpaces) {
				int newSpaceId = RESTHelpers.copySpace(id, spaceId, requestUserId);
				
				if (newSpaceId == 0){
					return gson.toJson(ERROR_DATABASE);
				}
			}
		} else {
			for (int id : selectedSubSpaces) {
				int newSpaceId = RESTHelpers.copyHierarchy(id, spaceId, requestUserId);
				if (newSpaceId == 0){
					return gson.toJson(ERROR_DATABASE);
				}
			}
		}
		return gson.toJson(0);
	}
	
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
		JsonObject nextDataTablesPage = null;
		if (usrId!=SessionUtil.getUserId(request)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.JOB, usrId, request,false);
		
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
		JsonObject nextDataTablesPage = null;
		if (usrId!=SessionUtil.getUserId(request)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Query for the next page of solver pairs and return them to the user
		log.debug(usrId);
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.SOLVER, usrId, request,false);
		
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
		JsonObject nextDataTablesPage = null;
		if (usrId!=SessionUtil.getUserId(request)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		// Query for the next page of solver pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.BENCHMARK, usrId, request,false);
		
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
		JsonObject nextDataTablesPage = null;
		if (usrId!=SessionUtil.getUserId(request)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.SOLVER, usrId, request,true);
		
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
		JsonObject nextDataTablesPage = null;
		if (usrId!=SessionUtil.getUserId(request)) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForUserDetails(RESTHelpers.Primitive.BENCHMARK, usrId, request, true);
		
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
	@Path("/space/makePublic/{id}/{hierarchy}")
	@Produces("application/json")	
	public String makePublic(@PathParam("id") int spaceId, @PathParam("hierarchy") boolean hierarchy, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		if(Spaces.setPublicSpace(spaceId, userId, true, hierarchy))
			return gson.toJson(ERROR_SPACE_ALREADY_PUBLIC);
		else
			return gson.toJson(0);
	}
	
	/**
	 * Make a space private
	 * @param spaceId the space to be made private
	 * @param request the http request
	 * @return 0: fails
	 *         1: success
	 * @author Ruoyu Zhang 
	 */
	@POST
	@Path("/space/makePrivate/{id}/{hierarchy}")
	@Produces("application/json")
	public String makePrivate(@PathParam("id") int spaceId, @PathParam("hierarchy") boolean hierarchy, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		if(Spaces.setPublicSpace(spaceId, userId, false, hierarchy))
			return gson.toJson(ERROR_SPACE_ALREADY_PRIVATE);
		else
			return gson.toJson(0);
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
	 * Returns the next page of entries in a given DataTable
	 * @param request the object containing the DataTable information
	 * @return a JSON object representing the next page of entries if successful,<br>
	 * 		1 if the request fails parameter validation, <br>
	 * @author Wyatt kaiser
	 * @throws Exception
	 */
	@POST
	@Path("/queues/pending/pagination/")
	@Produces("application/json")
	public String getAllPendingQueueReservations(@Context HttpServletRequest request) throws Exception {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForPendingQueueReservations(request);
		
		return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);	
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
	@Path("/queues/reserved/pagination/")
	@Produces("application/json")
	public String getAllQueueReservations(@Context HttpServletRequest request) throws Exception {
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		
		nextDataTablesPage = RESTHelpers.getNextDataTablesPageForQueueReservations(request);
		
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
		
		QueueRequest req = Requests.getRequestForReservation(queueId);

		GridEngineUtil.cancelReservation(req);
		return gson.toJson(0);
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
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
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
		log.debug("userId = " + userId);
		User u = Users.get(userId);
		log.debug("u = " + u);
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		QueueRequest req = Requests.getRequestForReservation(queueId);
		log.debug("req = " + req);
		GridEngineUtil.cancelReservation(req);
		return gson.toJson(0);

	}
	
	@POST
	@Path("/restart/starexec")
	@Produces("application/json")
	public void restartStarExec(@Context HttpServletRequest request) throws Exception {
		log.debug("restarting...");
		Util.executeCommand("sudo /sbin/service tomcat7 restart");
		log.debug("restarted");
	}
	
	@POST
	@Path("/cancel/request/{code}")
	@Produces("application/json")
	public String cancelQueueRequest(@PathParam("code") String code, @Context HttpServletRequest request) throws IOException {
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		QueueRequest queueRequest = Requests.getQueueRequest(code);

		
		boolean success = Requests.declineQueueReservation(queueRequest);
		
		if (success) {
			// Notify user they've been declined
			Mail.sendReservationResults(queueRequest, false);
			log.info(String.format("User [%s] has been declined queue reservation.", Users.get(queueRequest.getUserId()).getFullName()));
			return gson.toJson(0);	
		} else {
			return gson.toJson(3);
		}
		
	}
	
	
	/**
	 * Returns the paginated results of node assignments
	 * For the manage_nodes page
	 * 
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/nodes/dates/pagination")
	@Produces("application/json")
	public String nodeSchedule(@Context HttpServletRequest request) {
		//Get todays date
		Date today = new Date();

		//Get the latest date that a node is reserved for
		Date latest = Cluster.getLatestNodeDate();
		
		//Get all the dates between these two dates
	    List<Date> dates = new ArrayList<Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(today);
	    while (calendar.getTime().before(latest))
	    {
	        Date result = calendar.getTime();
	        dates.add(result);
	        calendar.add(Calendar.DATE, 1);
	    }
	    Date latestResult = calendar.getTime();
	    dates.add(latestResult);
	    
	    JsonObject nextDataTablesPage = RESTHelpers.getNextdataTablesPageForManageNodes(dates, request);
	    return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	    
		}
	
	/**
	 * Returns the paginated results of node assignments
	 * For the approve/deny requests page
	 * @param code the code of the queue_request
	 * 
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/nodes/dates/reservation/{code}/pagination")
	@Produces("application/json")
	public String nodeScheduleReservation(@PathParam("code") String code, @Context HttpServletRequest request) {
		
		//Get todays date
		QueueRequest req = Requests.getQueueRequest(code);
		Date today = new Date();

		//Get the latest date that a node is reserved/requested for
		Date latest1 = Cluster.getLatestNodeDate();
		Date latest2 = req.getEndDate();
		Date latest = null;
		if (latest1.after(latest2)) {
			latest = latest1;
		} else {
			latest = latest2;
		}
		
		//Get all the dates between these two dates
	    List<Date> dates = new ArrayList<Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(today);
	    while (calendar.getTime().before(latest))
	    {
	        Date result = calendar.getTime();
	        dates.add(result);
	        calendar.add(Calendar.DATE, 1);
	    }
	    Date latestResult = calendar.getTime();
	    dates.add(latestResult);
	    
	    JsonObject nextDataTablesPage = RESTHelpers.getNextdataTablesPageForApproveQueueRequest(dates, req, request);
	    return nextDataTablesPage == null ? gson.toJson(ERROR_DATABASE) : gson.toJson(nextDataTablesPage);
	    
		}
	
	/**
	 * Updates information in the database using a POST. the new values for 
	 * queueName, nodeCount, startDate, and endDate are included in the path.
	 * First validates that the new values are lega, and then updates the database
	 * and session information accordingly.
	 * 
	 * @return a json string containing '0' if the update was successful, else
	 * a json string containing '1'
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/edit/request/{code}/{queueName}/{nodeCount}/{startDate}/{endDate}")
	@Produces("application/json")
	public String editQueueRequest(@PathParam("code") String code, @PathParam("queueName") String queueName, @PathParam("nodeCount") String nodeCount, @PathParam("startDate") String startDate, @PathParam("endDate") String endDate, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		log.debug("user name = " + u.getFirstName());
		log.debug("user role = " + u.getRole());
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
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
		log.debug(Validator.isValidPrimName(queueName));
		log.debug(Validator.isValidInteger(nodeCount));
		log.debug(Validator.isValidDate(new_start));
		log.debug(Validator.isValidDate(new_end));
		
		if (true == ( Validator.isValidPrimName(queueName) && Validator.isValidInteger(nodeCount) && Validator.isValidDate(new_start) && Validator.isValidDate(new_end)) ) {
			log.debug("validated");
			
			// Make sure that the queue has a unique name
			if(Queues.notUniquePrimitiveName(queueName)) {
				return gson.toJson(6);
			}
			
			
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		    java.sql.Date startDateSql = null;
		    java.sql.Date endDateSql = null;
			try {
				java.util.Date startDateJava = format.parse(new_start);
				startDateSql = new java.sql.Date(startDateJava.getTime());
				
				java.util.Date endDateJava = format.parse(new_end);
				endDateSql = new java.sql.Date(endDateJava.getTime());
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			Date today = new Date();
			log.debug("today = " + today);
			log.debug("startDate = " + startDateSql);
			log.debug("endDate = " + endDateSql);
			
			if (startDateSql.before(today)) {
				log.debug("start day is before today");
				return gson.toJson(4);
			} else if (endDateSql.before(startDateSql)) { //end date is before or equivalent to start date
				log.debug("end date is before start date");
				return gson.toJson(5);
			} else if (endDateSql.toString().equals(startDateSql.toString())) {
				log.debug("end date is equivalent to start date");
				return gson.toJson(5);
			} else {
				QueueRequest req = Requests.getQueueRequest(code);
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
				new_req.setCode(req.getCode());
				new_req.setCreateDate(req.getCreateDate());
				
				success = Requests.updateQueueRequest(new_req);		

			}
		} else {
			log.debug("invalid");
			return gson.toJson(ERROR_INVALID_PARAMS);
		}
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}

	
	/** 
	 * Adds a queue_request to the database
	 * 
	 * @return a json string containing '0' if the update was successful, else 
	 * a json string containing '1'
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/add/queueRequest/{queueName}/{nodeCount}/{startDate}/{endDate}")
	@Produces("application/json")
	public String addQueueRequest(@PathParam("queueName") String queueName, @PathParam("nodeCount") String nodeCount, @PathParam("startDate") String startDate, @PathParam("endDate") String endDate, @Context HttpServletRequest request) {	
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
			return gson.toJson(ERROR_INVALID_PERMISSIONS);
		}
		
		return gson.toJson(0);
	}
	
	/**
	 * Will delete all temporary changes that were made to node_counts
	 * 
	 * @return a json string containing '0' if the delet was successful, else a json string containing '1'
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/nodes/refresh")
	@Produces("application/json")
	public String refreshNodeUpdates() {
		boolean success = Cluster.RefreshTempNodeChanges();
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
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
	public String updateNodeCount() {
		boolean success = Cluster.updateTempChanges();
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
	/**
	 * Will update a queue making it permanent
	 * 
	 * @author Wyatt Kaiser
	 */
	@POST
	@Path("/permanent/queue/{queueId}")
	@Produces("application/json")
	public String makeQueuePermanent(@PathParam("queueId") int queue_id) {
		boolean success = Queues.makeQueuePermanent(queue_id);
		if (success) {
			//TODO: SGE activate queue
			QueueRequest req = Requests.getRequestForReservation(queue_id);
			success = GridEngineUtil.createPermanentQueue(req);
		}
		return success ? gson.toJson(0) : gson.toJson(ERROR_DATABASE);
	}
	
}