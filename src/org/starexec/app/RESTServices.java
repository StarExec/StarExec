package org.starexec.app;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Comments;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Hash;
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
		
		return gson.toJson(RESTHelpers.toSpaceTree(Spaces.getSubSpaces(parentId, userId, false)));
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
	public String getAllQueues(@QueryParam("id") int id) {		
		if(id <= 0) {
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
		JobPair jp = Jobs.getPair(id);
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
		JobPair jp = Jobs.getPair(id);
		int userId = SessionUtil.getUserId(request);
		
		if(jp != null) {			
			if(Permissions.canUserSeeJob(jp.getJobId(), userId)) {
				Job j = Jobs.getShallow(jp.getJobId());			
				String stdout = GridEngineUtil.getStdOut(j, jp, limit);
				
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
	public String getNodeDetails(@PathParam("id") int id) {		
		return gson.toJson(Cluster.getNodeDetails(id));
	}
	
	/**
	 * @return a json string representing all attributes of the queue with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/queues/details/{id}")
	@Produces("application/json")	
	public String getQueueDetails(@PathParam("id") int id) {		
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
	@Path("/jobs/{id}/pairs/pagination")
	@Produces("application/json")	
	public String getJobPairsPaginated(@PathParam("id") int jobId, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		// Ensure user can view the job they are requesting the pairs from
		if(false == Permissions.canUserSeeJob(jobId, userId)){
			return gson.toJson(2);
		}
		
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextDataTablesPage(RESTHelpers.Primitive.JOB_PAIR, jobId, request);
		
		return nextDataTablesPage == null ? gson.toJson(1) : gson.toJson(nextDataTablesPage);
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
	@Path("/space/{id}/{primType}/pagination")
	@Produces("application/json")	
	public String getPrimitiveDetailsPaginated(@PathParam("id") int spaceId, @PathParam("primType") String primType, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		JsonObject nextDataTablesPage = null;
		
		// Ensure user can view the space containing the primitive(s)
		if(false == Permissions.canUserSeeSpace(spaceId, userId)) {
			return gson.toJson(2);
		}
		
		// Query for the next page of primitives and return them to the user
		if(primType.startsWith("j")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPage(RESTHelpers.Primitive.JOB, spaceId, request);
		} else if(primType.startsWith("u")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPage(RESTHelpers.Primitive.USER, spaceId, request);
		} else if(primType.startsWith("so")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPage(RESTHelpers.Primitive.SOLVER, spaceId, request);
		} else if(primType.startsWith("sp")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPage(RESTHelpers.Primitive.SPACE, spaceId, request);
		} else if(primType.startsWith("b")){
			nextDataTablesPage = RESTHelpers.getNextDataTablesPage(RESTHelpers.Primitive.BENCHMARK, spaceId, request);
		}
		
		return nextDataTablesPage == null ? gson.toJson(1) : gson.toJson(nextDataTablesPage);
	}
	
	
	/**
	 * Returns an integer representing the number of primitives of the specified type that exist in a given space
	 *
	 * @param spaceId the id of the space to query for primitives from
	 * @param primType the type of primitive
	 * @param request the object containing the user's id
	 * @return an integer representing the number of primitives of the given type in the given space if successful,<br>
	 * 		-1 if the primitive type could not be determined,<br>
	 * 		-2 if the user doesn't have permission to view the parent space of the primitive
	 * @author Todd Elvers
	 */
	@POST
	@Path("/space/{id}/{primType}/count")
	@Produces("application/json")	
	public String getPrimitiveCount(@PathParam("id") int spaceId, @PathParam("primType") String primType, @Context HttpServletRequest request) {			
		int userId = SessionUtil.getUserId(request);
		int count = -1;
		
		// Ensure user can view the space containing the primitive(s)
		if(false == Permissions.canUserSeeSpace(spaceId, userId)) {
			return gson.toJson(-2);
		}
		
		// Return the number of primitives of the specified type
		if(primType.startsWith("j")){
			count = Jobs.getCountInSpace(spaceId);
		} else if(primType.startsWith("u")){
			count = Users.getCountInSpace(spaceId);
		} else if(primType.startsWith("b")){
			count = Benchmarks.getCountInSpace(spaceId);
		} else if(primType.startsWith("so")){
			count = Solvers.getCountInSpace(spaceId);	
		}  else if(primType.startsWith("sp")){
			count = Spaces.getCountInSpace(spaceId, SessionUtil.getUserId(request));
		}
		
		return gson.toJson(count);
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
		if(p != null && (p.isLeader() || SessionUtil.getUserId(request) == userId)) {
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
		return gson.toJson(1);
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
		return success ? gson.toJson(0) : gson.toJson(1);
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
			return Websites.delete(websiteId, SessionUtil.getUserId(request), Websites.WebsiteType.USER) ? gson.toJson(0) : gson.toJson(1);
		} else if (type.equals("space")){
			// Permissions check; ensures the user deleting the website is a leader
			Permission perm = SessionUtil.getPermission(request, id);		
			if(perm == null || !perm.isLeader()) {
				return gson.toJson(2);	
			}
			
			return Websites.delete(websiteId, id, Websites.WebsiteType.SPACE) ? gson.toJson(0) : gson.toJson(1);
		} else if (type.equals("solver")) {
			Solver s = Solvers.get(id);
			if (s.getUserId() == SessionUtil.getUserId(request)) {
				return Websites.delete(websiteId, id, Websites.WebsiteType.SOLVER) ? gson.toJson(0) : gson.toJson(1);
			}
			return gson.toJson(2);
		}
		
		return gson.toJson(1);
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
		} else if (attribute.equals("email")) {
			if (true == Validator.isValidEmail(newValue)) { 
				success = Users.updateEmail(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setEmail(newValue);
				}
			}
		} else if (attribute.equals("institution")) {
			if (true == Validator.isValidInstitution(newValue)) {
				success = Users.updateInstitution(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setInstitution(newValue);
				}
			}
		} else if (attribute.equals("archivetype")) {
			//REST doesn't like if newValue starts with a "." but we need that in our archive type
			newValue = "." + newValue;
			if (true == Validator.isValidArchiveType(newValue)) {
				success = Users.updateArchiveType(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setArchiveType(newValue);
				}
			}
		}
		
		// Passed validation AND Database update successful
		return success ? gson.toJson(0) : gson.toJson(1);
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
		Permission perm = SessionUtil.getPermission(request, id);		
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		if(Util.isNullOrEmpty((String)request.getParameter("val"))){
			return gson.toJson(1);
		}
		
		boolean success = false;
		
		// Go through all the cases, depending on what attribute we are changing.
		if (attribute.equals("name")) {
			String newName = (String)request.getParameter("val");
			if (true == Validator.isValidPrimName(newName)) {
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
		}
		
		// Passed validation AND Database update successful
		return success ? gson.toJson(0) : gson.toJson(1);
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
			return gson.toJson(3);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				|| !Validator.isValidBool(request.getParameter("locked"))){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT a leader of the space, deny update request
		int userId = SessionUtil.getUserId(request);
		Permission perm = Permissions.get(userId, id);
		if(perm == null || !perm.isLeader()){
			gson.toJson(2);
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
		
		s.setPermission(p);
		
		// Perform the update and return information according to success/failure
		return Spaces.updateDetails(userId, s) ? gson.toJson(0) : gson.toJson(1);
	}
	
	/**
	 * Removes a benchmark type from a given space
	 * 
	 * @return a json string containing '0' if the deletion was successful, else
	 *         a json string containing '1' if there was a failure, '2' for
	 *         insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/processors/delete/{procId}")
	@Produces("application/json")
	public String deleteProcessor(@PathParam("procId") int pid, @Context HttpServletRequest request) {
		Processor p = Processors.get(pid);
		
		// Permissions check; ensures user is the leader of the community that owns the processor
		Permission perm = SessionUtil.getPermission(request, p.getCommunityId());		
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		return Processors.delete(pid) ? gson.toJson(0) : gson.toJson(1);
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
			return gson.toJson(2);	
		}
		
		if(true == Communities.leave(SessionUtil.getUserId(request), spaceId)) {
			// Delete prior entry in user's permissions cache for this community
			SessionUtil.removeCachePermission(request, spaceId);
			return gson.toJson(0);
		}
		
		
		return gson.toJson(1);
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
		if(null == request.getParameterValues("selectedBenches[]")){
			return gson.toJson(1);
		}
		
		// Extract the String bench id's and convert them to Integer
		ArrayList<Integer> selectedBenches = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedBenches[]")){
			selectedBenches.add(Integer.parseInt(id));
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveBench()) {
			return gson.toJson(2);	
		}

		// Remove the benchmark from the space
		return Spaces.removeBenches(selectedBenches, spaceId) ? gson.toJson(0) : gson.toJson(1);
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
			return gson.toJson(2);
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
			return gson.toJson(3);	
		}
		
		// TODO: Possible security risk here, the user could spoof ID's of users that they cannot really see and
		// end up adding them to the destination space.
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(4);
		}			
		
		// And the space the user is being copied from must not be locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(5);
		}
		
		// Convert the users to copy to a int list
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		
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
					return gson.toJson(6);	
				}			
				subspaceIds.add(subspaceId);
			}
			
			
			// Add the user(s) to the destination space and its subspaces
			return Users.associate(selectedUsers, subspaceIds) ? gson.toJson(0) : gson.toJson(1);
		} else {
			// Add the user(s) to the destination space
			return Users.associate(selectedUsers, spaceId) ? gson.toJson(0) : gson.toJson(1);
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
	 * 			6: user does not belong to one or more of the subspaces of the destination space
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
				|| !Validator.isValidBool(request.getParameter("copyToSubspaces"))){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the solver is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Get the flag that indicates whether or not to copy this solver to all subspaces of 'fromSpace'
		boolean copyToSubspaces = Boolean.parseBoolean(request.getParameter("copyToSubspaces"));
		
		// Convert the solvers to copy to an int list
		List<Integer> selectedSolvers = Util.toIntegerList(request.getParameterValues("selectedIds[]"));
		
		// Verify the space the solvers are being copied from is not locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(5);
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(4);
		}	

		// Make sure the user can see the solver they're trying to copy
		for (int id : selectedSolvers) {
			if (!Permissions.canUserSeeSolver(id, requestUserId)) {
				return gson.toJson(4);
			}
		}
		
		// Check permissions - the user must have add solver permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddSolver()) {
			return gson.toJson(3);	
		}			

		// Either copy the solvers to the destination space or the destination space and all of its subspaces (that the user can see)
		if (copyToSubspaces == true) {
			int subspaceId;
			List<Space> subspaces = Spaces.trimSubSpaces(requestUserId, Spaces.getSubSpaces(spaceId, requestUserId, true));
			List<Integer> subspaceIds = new LinkedList<Integer>();
			
			// Add the destination space to the list of spaces to associate the solvers with
			subspaceIds.add(spaceId);
			
			// Iterate once through all subspaces of the destination space to ensure the user has addSolver permissions in each
			for(Space subspace : subspaces){
				subspaceId = subspace.getId();
				Permission subspacePerm = SessionUtil.getPermission(request, subspaceId);	
				
				if(subspacePerm == null || !subspacePerm.canAddSolver()) {
					return gson.toJson(6);	
				}			
				subspaceIds.add(subspace.getId());
			}

			// Add the solvers to the destination space and its subspaces
			return Solvers.associate(selectedSolvers, subspaceIds) ? gson.toJson(0) : gson.toJson(1);
		} else {
			// Add the solvers to the destination space
			return Solvers.associate(selectedSolvers, spaceId) ? gson.toJson(0) : gson.toJson(1);
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
	 * 			5: the 'from space' is locked
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/benchmark")
	@Produces("application/json")
	public String copyBenchToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Check permissions, the user must have add benchmark permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddBenchmark()) {
			return gson.toJson(3);	
		}			
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(4);
		}			
		
		// And the space the solvers are being copied from must not be locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(5);
		}
		
		// Convert the benchmarks to copy to a int list
		List<Integer> selectedBenchs= Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the benchmarks they're trying to copy
		for(int id : selectedBenchs) {
			if(!Permissions.canUserSeeBench(id, requestUserId)) {
				return gson.toJson(4);
			}
		}		
		
		// Make the associations
		boolean success = Benchmarks.associate(selectedBenchs, spaceId);
		
		// Return a value based on results from database operation
		return success ? gson.toJson(0) : gson.toJson(1);
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
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/job")
	@Produces("application/json")
	public String copyJobToSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		int requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		int fromSpace = Integer.parseInt(request.getParameter("fromSpace"));
		
		// Check permissions, the user must have add benchmark permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddJob()) {
			return gson.toJson(3);	
		}			
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(4);
		}			
		
		// And the space the solvers are being copied from must not be locked
		if(Spaces.get(fromSpace).isLocked()) {
			return gson.toJson(5);
		}
		
		// Convert the benchmarks to copy to a int list
		List<Integer> selectedJobs = Util.toIntegerList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the benchmarks they're trying to copy
		for(int id : selectedJobs) {
			if(!Permissions.canUserSeeJob(id, requestUserId)) {
				return gson.toJson(4);
			}
		}		
		
		// Make the associations
		boolean success = Jobs.associate(selectedJobs, spaceId);
		
		// Return a value based on results from database operation
		return success ? gson.toJson(0) : gson.toJson(1);
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
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedUsers[]")){
			return gson.toJson(1);
		}		
		
		// Get the id of the user who initiated the removal
		int userIdOfRemover = SessionUtil.getUserId(request);
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveUser()) {
			return gson.toJson(2);	
		}
		
		// Extract the String user id's and convert them to Integer
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedUsers[]"));
		
		// Validate the list of users to remove by:
		// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the list of users to remove
		// 2 - Ensuring other leaders of the space aren't in the list of users to remove
		for(int userId : selectedUsers){
			if(userId == userIdOfRemover){
				return gson.toJson(3);
			}
			perm = Permissions.get(userId, spaceId);
			if(perm.isLeader()){
				return gson.toJson(4);
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
					return gson.toJson(5);	
				}
				for (int userId : selectedUsers) {
					// Make sure the users you are trying to remove are not leaders in this subspace
					perm = Permissions.get(userId, subspace.getId());
					if (perm != null && perm.isLeader()) {
						return gson.toJson(6);
					}
				subspaceIds.add(subspaceId);
				}
			}
			
			// Remove the users from the space and its subspaces
			return Spaces.removeUsersFromHierarchy(selectedUsers, subspaceIds) ? gson.toJson(0) : gson.toJson(1);
		}
		
		// Otherwise...
		return Spaces.removeUsers(selectedUsers, spaceId) ? gson.toJson(0) : gson.toJson(1);
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
		if(null == request.getParameterValues("selectedSolvers[]")){
			return gson.toJson(1);
		}
		
		// Extract the String solver id's and convert them to Integer
		ArrayList<Integer> selectedSolvers = new ArrayList<Integer>();
		for(String id : request.getParameterValues("selectedSolvers[]")){
			selectedSolvers.add(Integer.parseInt(id));
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveSolver()) {
			return gson.toJson(2);	
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
					return gson.toJson(3);	
				} 
				
				subspaceIds.add(subspaceId);
			}
			
			return Spaces.removeSolversFromHierarchy(selectedSolvers, subspaceIds) ? gson.toJson(0) : gson.toJson(1);

		}
		
		// Otherwise...
		return Spaces.removeSolvers(selectedSolvers, spaceId) ? gson.toJson(0) : gson.toJson(1);
	}
	
	
	/**
	 * Removes a job's association with a space, thereby removing the job from
	 * the space
	 * 
	 * @return 	0: success,<br>
	 * 			1: invalid parameters or database level error,<br>
	 * 			2: insufficient permissions
	 * @author Todd Elvers
	 * @deprecated not yet tested
	 */
	@POST
	@Path("/remove/job/{spaceId}")
	@Produces("application/json")
	public String removeJobsFromSpace(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedJobs[]")){
			return gson.toJson(1);
		}
		
		// Extract the String job id's and convert them to Integer
		ArrayList<Integer> selectedJobs = new ArrayList<Integer>();
		for (String id : request.getParameterValues("selectedJobs[]")) {
			selectedJobs.add(Integer.parseInt(id));
		}

		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);
		if (perm == null || !perm.canRemoveJob()) {
			return gson.toJson(2);
		}

		// Remove the job from the space
		return Spaces.removeJobs(selectedJobs, spaceId) ? gson.toJson(0) : gson.toJson(1);
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
			for(String id : request.getParameterValues("selectedSubspaces[]")){
				selectedSubspaces.add(Integer.parseInt(id));
			}
		} catch(Exception e){
			return gson.toJson(1);
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, parentSpaceId);		
		if(null == perm || !perm.isLeader()) {
			return gson.toJson(2);	
		}

		// Remove the subspaces from the space
		return Spaces.removeSubspaces(selectedSubspaces, parentSpaceId, SessionUtil.getUserId(request)) ? gson.toJson(0) : gson.toJson(3);
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
			return gson.toJson(3);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				|| !Validator.isValidBool(request.getParameter("downloadable"))){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the solver, deny update request
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		if(solver == null || solver.getUserId() != userId){
			gson.toJson(2);
		}
		
		// Extract new solver details from request
		String name = request.getParameter("name");
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		
		// Apply new solver details to database
		return Solvers.updateDetails(solverId, name, description, isDownloadable) ? gson.toJson(0) : gson.toJson(1);
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
	@Path("/delete/solver/{id}")
	@Produces("application/json")
	public String deleteSolver(@PathParam("id") int solverId, @Context HttpServletRequest request) {
		
		// Permissions check; if user is NOT the owner of the solver, deny deletion request
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		if(solver == null || solver.getUserId() != userId){
			gson.toJson(2);
		}
		
		// TODO Need to check if the solver exists in historical space. If it does, we SHOULD NOT delete the solver
		// Delete the solver from the database
		return Solvers.delete(solverId) ? gson.toJson(0) : gson.toJson(1);
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
	@Path("/delete/benchmark/{id}")
	@Produces("application/json")
	public String deleteBenchmark(@PathParam("id") int benchId, @Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the benchmark, deny deletion request
		int userId = SessionUtil.getUserId(request);		
		Benchmark bench = Benchmarks.get(benchId);
		if(bench == null || bench.getUserId() != userId){
			gson.toJson(2);
		}
		
		//TODO Need to check if the benchmark exists in historical space. If it does, we SHOULD NOT delete the benchmark
		// Delete the benchmark from the database
		return Benchmarks.delete(benchId) ? gson.toJson(0) : gson.toJson(1);
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
			return gson.toJson(3);
		}
		
		// Safely extract the type
		try{
			type = Integer.parseInt(request.getParameter("type"));
		} catch (NumberFormatException nfe){
			isValidRequest = false;
		}
		if(false == isValidRequest){
			return gson.toJson(3);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				|| !Validator.isValidBool(request.getParameter("downloadable"))){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the benchmark, deny update request
		int userId = SessionUtil.getUserId(request);
		Benchmark bench = Benchmarks.get(benchId);
		if(bench == null || bench.getUserId() != userId){
			gson.toJson(2);
		}
		
		// Extract new benchmark details from request
		String name = request.getParameter("name");
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		
		// Apply new benchmark details to database
		return Benchmarks.updateDetails(benchId, name, description, isDownloadable, type) ? gson.toJson(0) : gson.toJson(1);
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
						return gson.toJson(1); //Database operation returned false
					}
				} else {
					return gson.toJson(2); //Validate operation returned false
				}
			} else {
				return gson.toJson(3); //newPass != confirmPass
			}
		} else {
			return gson.toJson(4); //hashedPass != databasePass
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
		Permission perm = SessionUtil.getPermission(request, spaceId);
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		// Ensure the user to edit the permissions of isn't themselves a leader
		perm = Permissions.get(userId, spaceId);
		if(perm.isLeader()){
			return gson.toJson(3);
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
		return Permissions.set(userId, spaceId, newPerm) ? gson.toJson(0) : gson.toJson(1);
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
			return gson.toJson(3);
		}
		
		// Ensure the parameters are valid
		if(!Validator.isValidPrimName(request.getParameter("name"))
				|| !Validator.isValidPrimDescription(request.getParameter("description"))
				||  request.getParameter("contents").isEmpty()){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the configuration file's solver, deny update request
		int userId = SessionUtil.getUserId(request);
		Configuration config = Solvers.getConfiguration(configId);
		Solver solver = Solvers.get(config.getSolverId());
		if(null == solver || solver.getUserId() != userId){
			gson.toJson(2);
		}
		
		
		// Extract new configuration file details from request
		String name = (String) request.getParameter("name");
		String description = (String) request.getParameter("description");
		String contents = (String) request.getParameter("contents");
		
		// Apply new solver details to database
		return Solvers.updateConfigDetails(configId, name, description, contents) ? gson.toJson(0) : gson.toJson(1);
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
			return gson.toJson(1);
		}
		
		// Permissions check; if user is NOT the owner of the configuration file's solver, deny deletion request
		Solver solver = Solvers.get(config.getSolverId());
		if(null == solver || solver.getUserId() != userId){
			return gson.toJson(2);
		}
		
		// Attempt to remove the configuration's physical file from disk
		if(false == Solvers.deleteConfigurationFile(config)){
			return gson.toJson(3);
		}
		
		// Attempt to remove the configuration's entry in the database
		if(false == Solvers.deleteConfiguration(configId)){
			return gson.toJson(4);
		}
		
		// Attempt to update the disk_size of the parent solver to reflect the file deletion
		if(false == Solvers.updateSolverDiskSize(solver)){
			return gson.toJson(5);
		}
		
		// If this point is reached, then the configuration has been completely removed
		return gson.toJson(0);
	}
	
	/**
	 * Retrieves the associated comments of a given benchmark, space, or solver.
	 * The type and its id is included in the POST path
	 * @return a json string representing all the comments associated with
	 * the current benchmark/space/solver
	 * @author Vivek Sardeshmukh
	 */
	@GET
	@Path("/comments/{type}/{id}")
	@Produces("application/json")
	public String getComments(@PathParam("type") String type, @PathParam("id") int id, @Context HttpServletRequest request) {
		int userId = SessionUtil.getUserId(request);
		if(type.startsWith("b")){
			if(Permissions.canUserSeeBench(id, userId)){
				return gson.toJson(Comments.getAll(id, Comments.CommentType.BENCHMARK));
			}
		} else if(type.startsWith("sp")){
			if(Permissions.canUserSeeSpace(id, userId)) {
				return gson.toJson(Comments.getAll(id, Comments.CommentType.SPACE));
			}
		} else if (type.startsWith("so")) {
			if(Permissions.canUserSeeSolver(id, userId)){
				return gson.toJson(Comments.getAll(id, Comments.CommentType.SOLVER));
			}
		}
		return gson.toJson(1);
	}
	
	/**
	 * Adds a comment to the database. This is dynamic to allow adding a
	 * comment associated with a space, solver, or benchmark. The type of comment is given
	 * in the path
	 * 
	 * @return a json string containing '0' if the add was successful, '1' otherwise
	 * @author Vivek Sardeshmukh 
	 */
	@POST
	@Path("/comments/add/{type}/{id}")
	@Produces("application/json")
	public String addComment(@PathParam("type") String type, @PathParam("id") int id, @Context HttpServletRequest request) {
		boolean success = false;
		int userId = SessionUtil.getUserId(request);
		if (type.startsWith("b")) {
			if(Permissions.canUserSeeBench(id, userId)){
				String cmt = request.getParameter("comment");			
				success = Comments.add(id, userId, cmt, Comments.CommentType.BENCHMARK);
			}
		} else if (type.startsWith("sp")) {
			if(Permissions.canUserSeeSpace(id, userId)) {
				String cmt = request.getParameter("comment");			
				success = Comments.add(id, userId, cmt, Comments.CommentType.SPACE);
			}
		} else if (type.startsWith("so")) {
			if(Permissions.canUserSeeSolver(id, userId)){
				String cmt = request.getParameter("comment");			
				success = Comments.add(id, userId, cmt, Comments.CommentType.SOLVER);	
			}	
		}
		
		// Passed validation AND Database update successful	
		return success ? gson.toJson(0) : gson.toJson(1);
	}

	/**
	 * Deletes a comment from either a benchmark, solver, or a space 
	 *
	 * @param type the type the delete is for, can be  'benchmark', or 'space', 'solver'
	 * @param id the id of the entity (benchmark, space, or solver) from which we want to remove a comment
	 * @param userId user id of corresponding comment 
	 * @param commentId the id of the comment to remove
	 * @return 0 iff the comment was successfully deleted for a space, 2 if the user lacks permissions,
	 * and 1 otherwise
	 * @author Vivek Sardeshmukh
	 */
	@POST
	@Path("/comments/delete/{type}/{id}/{userId}/{commentId}")
	@Produces("application/json")
	public String deleteComment(@PathParam("type") String type, @PathParam("id") int id, @PathParam("userId") int userId, @PathParam("commentId") int commentId, @Context HttpServletRequest request) {
		int uid = SessionUtil.getUserId(request); 
		if (type.startsWith("b")) {
			Benchmark b = Benchmarks.get(id);
			// Ensure user is either the owner of the comment or the benchmark
			 if (uid!=userId && uid!=b.getUserId()) {
				 return gson.toJson(2);
			 }
			 return Comments.delete(commentId) ? gson.toJson(0) : gson.toJson(1);
		} 
		 else if (type.startsWith("sp")) {
			//Permissions check; ensures the user deleting the comment is a leader or owner of the comment
			Permission perm = SessionUtil.getPermission(request, id);		
			if((perm == null || !perm.isLeader()) && uid!=userId ) {
				return gson.toJson(2);	
			}
			return Comments.delete(commentId) ? gson.toJson(0) : gson.toJson(1);
		} 
		 else if (type.startsWith("so")) {
			Solver s = Solvers.get(id);
			// Ensure user is either the owner of the comment or the solver
			if (s.getUserId() != uid && userId != uid) {
				return gson.toJson(2);
			}
			return Comments.delete(commentId) ? gson.toJson(0) : gson.toJson(1);
			
		}
		
		return gson.toJson(1);
	}
	
	/**
	 * Make a list of users the leaders of a space
	 * @param spaceId The Id of the space  
	 * @param request The HttpRequestServlet object containing the list of user's Id
	 * @return 0: Success.
	 *         1: Selected userId list is empty.
	 *         2: User making this request is not a leader
	 *         3: If one is promoting himself
	 * @author Ruoyu Zhang
	 */
	@POST
	@Path("/makeLeader/{spaceId}")
	@Produces("application/json")
	public String makeLeader(@PathParam("spaceId") int spaceId, @Context HttpServletRequest request) {

		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedUsers[]")){
			return gson.toJson(1);
		}		
		
		// Get the id of the user who initiated the promotion
		int userIdOfPromotion = SessionUtil.getUserId(request);
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		// Extract the String user id's and convert them to Integer
		List<Integer> selectedUsers = Util.toIntegerList(request.getParameterValues("selectedUsers[]"));
		
		// Validate the list of users to remove by:
		// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the list of users to remove
		// 2 - Ensuring other leaders of the space aren't in the list of users to remove
		for(int userId : selectedUsers){
			if(userId == userIdOfPromotion){
				return gson.toJson(3);
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
			p.setLeader(true);
			
			Permissions.set(userId, spaceId, p);
		}
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
			return gson.toJson(2);
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
			return gson.toJson(5);
		}
		
		// Verify the user can at least see the space they claim to be copying from
		if(!Permissions.canUserSeeSpace(fromSpace, requestUserId)) {
			return gson.toJson(4);
		}	

		// Make sure the user can see the subSpaces they're trying to copy
		for (int id : selectedSubSpaces) {
			if (!Permissions.canUserSeeSpace(id, requestUserId)) {
				return gson.toJson(4);
			}
		}
		
		// Check permissions - the user must have add space permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddSpace()) {
			return gson.toJson(3);	
		}

		// Add the subSpaces to the destination space
		if (!copyHierarchy) {
			for (int id : selectedSubSpaces) {
				int newSpaceId = RESTHelpers.copySpace(id, spaceId, requestUserId);
				System.out.println("The Id of the new space is");
				System.out.println(newSpaceId);
				System.out.println("\n");
				
				if (newSpaceId == 0){
					return gson.toJson(1);
				}
			}
		} else {
			for (int id : selectedSubSpaces) {
				int newSpaceId = RESTHelpers.copyHierarchy(id, spaceId, requestUserId);
				if (newSpaceId == 0){
					return gson.toJson(1);
				}
			}
		}
		return gson.toJson(0);
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
		
		// Query for the next page of job pairs and return them to the user
		nextDataTablesPage = RESTHelpers.getNextPageOfUserJobs(usrId, request);
		
		return nextDataTablesPage == null ? gson.toJson(1) : gson.toJson(nextDataTablesPage);
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
			return gson.toJson(1);
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
			return gson.toJson(1);
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
}