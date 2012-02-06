package org.starexec.app;

import java.util.ArrayList;
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
import org.starexec.data.database.BenchTypes;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.util.Hash;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	public String getSubSpaces(@QueryParam("id") long parentId, @Context HttpServletRequest request) {					
		long userId = SessionUtil.getUserId(request);
		
		return gson.toJson(RESTHelpers.toSpaceTree(Spaces.getSubSpaces(parentId, userId)));
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
	public String getAllQueues(@QueryParam("id") long id) {		
		if(id <= 0) {
			return gson.toJson(RESTHelpers.toQueueList(Cluster.getAllQueues()));
		} else {
			return gson.toJson(RESTHelpers.toNodeList(Cluster.getNodesForQueue(id)));
		}
	}
	
	/**
	 * @return a json string representing all attributes of the node with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/nodes/details/{id}")
	@Produces("application/json")	
	public String getNodeDetails(@PathParam("id") long id) {		
		return gson.toJson(Cluster.getNodeDetails(id));
	}
	
	/**
	 * @return a json string representing all attributes of the queue with the given id
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/cluster/queues/details/{id}")
	@Produces("application/json")	
	public String getQueueDetails(@PathParam("id") long id) {		
		return gson.toJson(Cluster.getQueueDetails(id));
	}
	
	/**
	 * @return a json string representing all communities within starexec
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/communities/details/{id}")
	@Produces("application/json")	
	public String getCommunityDetails(@PathParam("id") long id, @Context HttpServletRequest request) {
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
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/space/{id}")
	@Produces("application/json")	
	public String getSpaceDetails(@PathParam("id") long spaceId, @Context HttpServletRequest request) {			
		long userId = SessionUtil.getUserId(request);
		
		Space s = null;
		Permission p = null;
		
		if(Permissions.canUserSeeSpace(spaceId, userId)) {
			s = Spaces.getDetails(spaceId, userId);
			p = SessionUtil.getPermission(request, spaceId);
		}					
		
		return limitGson.toJson(new RESTHelpers.SpacePermissionPair(s, p));				
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
	public String getUserSpacePermissions(@PathParam("spaceId") long spaceId, @PathParam("userId") long userId, @Context HttpServletRequest request) {
		return gson.toJson(Permissions.get(userId, spaceId));
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
	 * @return a json string representing all the websites associated with
	 * the current user
	 * @author Skylar Stark and Todd Elvers
	 */
	@GET
	@Path("/websites/{type}/{spaceId}")
	@Produces("application/json")
	public String getWebsites(@PathParam("type") String type, @PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		long userId = SessionUtil.getUserId(request);
		if(type.equals("user")){
			return gson.toJson(Websites.getAll(userId, Websites.WebsiteType.USER));
		} else if(type.equals("space")){
			return gson.toJson(Websites.getAll(spaceId, Websites.WebsiteType.SPACE));
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
	public String addWebsite(@PathParam("type") String type, @PathParam("id") long id, @Context HttpServletRequest request) {
		boolean success = false;
		
		if (type.equals("user")) {
			long userId = SessionUtil.getUserId(request);
			String name = request.getParameter("name");
			String url = request.getParameter("url");			
			success = Websites.add(userId, url, name, Websites.WebsiteType.USER);
		} else if (type.equals("space")) {
			// Make sure this user is capable of adding a website to the space
			Permission perm = SessionUtil.getPermission(request, id);
			if(perm != null && perm.isLeader()) {
				String name = request.getParameter("name");
				String url = request.getParameter("url");			
				success = Websites.add(id, url, name, Websites.WebsiteType.SPACE);
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
	@Path("/websites/delete/{type}/{spaceId}/{websiteId}")
	@Produces("application/json")
	public String deleteWebsite(@PathParam("type") String type, @PathParam("spaceId") long spaceId, @PathParam("websiteId") long websiteId, @Context HttpServletRequest request) {
		
		if(type.equals("user")){
			return Websites.delete(websiteId, SessionUtil.getUserId(request), Websites.WebsiteType.USER) ? gson.toJson(0) : gson.toJson(1);
		} else if (type.equals("space")){
			// Permissions check; ensures the user deleting the website is a leader
			Permission perm = SessionUtil.getPermission(request, spaceId);		
			if(perm == null || !perm.isLeader()) {
				return gson.toJson(2);	
			}
			
			return Websites.delete(websiteId, spaceId, Websites.WebsiteType.SPACE) ? gson.toJson(0) : gson.toJson(1);
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
		long userId = SessionUtil.getUserId(request);
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
		}
		
		// Passed validation AND Database update successful
		return success ? gson.toJson(0) : gson.toJson(1);
	}
	
	/** 
	 * Updates information for a space in the database using a POST. Attribute and
	 * new value are included in the path. First validates that the new value
	 * is legal, then updates the database and session information accordingly.
	 * 
	 * @return a json string containing '0' if the update was successful, else 
	 * a json string containing '1' if there was a failure. '2' for insufficient permissions
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/edit/space/{attr}/{id}")
	@Produces("application/json")
	public String editSpaceDetails(@PathParam("attr") String attribute, @PathParam("id") long id, @Context HttpServletRequest request) {	
		Permission perm = SessionUtil.getPermission(request, id);		
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		boolean success = false;
		
		// Go through all the cases, depending on what attribute we are changing.
		if (attribute.equals("name")) {
			String newName = (String)request.getParameter("val");
			if (true == Validator.isValidPrimName(newName)) {
				success = Spaces.updateName(id, newName);
			}
		} else if (attribute.equals("desc")) {
			String newDesc = (String)request.getParameter("val");
			if (true == Validator.isValidPrimDescription(newDesc)) {
				success = Spaces.updateDescription(id, newDesc);				
			}
		}
		
		// Passed validation AND Database update successful
		return success ? gson.toJson(0) : gson.toJson(1);
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
	@Path("/edit/space/type/delete/{spaceId}/{typeId}")
	@Produces("application/json")
	public String deleteBenchmarkType(@PathParam("spaceId") long spaceId, @PathParam("typeId") long typeId, @Context HttpServletRequest request) {
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		return BenchTypes.delete(typeId, spaceId) ? gson.toJson(0) : gson.toJson(1);
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
	public String leaveCommunity(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
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
	 * @return a json string containing '0' if the benchmark was successfully
	 *         removed from the space, else a json string containing '1' if
	 *         there was a failure on the database level/attempted 'empty' delete, 
	 *         and '2' for insufficient permissions
	 * @author Todd Elvers
	 */
	@POST
	@Path("/remove/benchmark/{spaceId}")
	@Produces("application/json")
	public String removeBenchmarksFromSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedBenches[]")){
			return gson.toJson(1);
		}
		
		// Extract the String bench id's and convert them to Long
		ArrayList<Long> selectedBenches = new ArrayList<Long>();
		for(String id : request.getParameterValues("selectedBenches[]")){
			selectedBenches.add(Long.parseLong(id));
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
	 * @param spaceId
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of users to copy as well as a 'fromSpace' parameter that is the
	 * space the users are being copied from.
	 * @return 0: success, 1: database failure, 2: missing parameters, 3: no add user permission in destination space
	 * 4: user doesn't belong to the 'from space', 5: the 'from space' is locked
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/user")
	@Produces("application/json")
	public String copyUserToSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of users to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		long requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the user is being copied from
		long fromSpace = Long.parseLong(request.getParameter("fromSpace"));
		
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
		
		// Convert the users to copy to a long list
		List<Long> selectedUsers = Util.toLongList(request.getParameterValues("selectedIds[]"));		
		boolean success = Users.associate(selectedUsers, spaceId);
		
		// Return a value based on results from database operation
		return success ? gson.toJson(0) : gson.toJson(1);
	}

	/**
	 * Associates (i.e. 'copies') a solver from one space into another
	 * @param spaceId
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of solvers to copy as well as a 'fromSpace' parameter that is the
	 * space the solvers are being copied from.
	 * @return 0: success, 1: database failure, 2: missing parameters, 3: no add permission in destination space
	 * 4: user doesn't belong to the 'from space', 5: the 'from space' is locked
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/solver")
	@Produces("application/json")
	public String copySolverToSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of solvers to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		long requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the solver is being copied from
		long fromSpace = Long.parseLong(request.getParameter("fromSpace"));
		
		// Check permissions, the user must have add solver permissions in the destination space
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canAddSolver()) {
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
		
		// Convert the solvers to copy to a long list
		List<Long> selectedSolvers = Util.toLongList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the solver they're trying to copy
		for(long id : selectedSolvers) {
			if(!Permissions.canUserSeeSolver(id, requestUserId)) {
				return gson.toJson(4);
			}
		}		
		
		// Make the associations
		boolean success = Solvers.associate(selectedSolvers, spaceId);
		
		// Return a value based on results from database operation
		return success ? gson.toJson(0) : gson.toJson(1);
	}
	
	/**
	 * Associates (i.e. 'copies') a benchmark from one space into another
	 * @param spaceId
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of benchmarks to copy as well as a 'fromSpace' parameter that is the
	 * space the benchmarks are being copied from.
	 * @return 0: success, 1: database failure, 2: missing parameters, 3: no add permission in destination space
	 * 4: user doesn't belong to the 'from space', 5: the 'from space' is locked
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/benchmark")
	@Produces("application/json")
	public String copyBenchToSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		long requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		long fromSpace = Long.parseLong(request.getParameter("fromSpace"));
		
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
		
		// Convert the benchmarks to copy to a long list
		List<Long> selectedBenchs= Util.toLongList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the benchmarks they're trying to copy
		for(long id : selectedBenchs) {
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
	 * @param spaceId
	 * @param request The request that contains data about the operation including a 'selectedIds'
	 * attribute that contains a list of jobs to copy as well as a 'fromSpace' parameter that is the
	 * space the jobs are being copied from.
	 * @return 0: success, 1: database failure, 2: missing parameters, 3: no add permission in destination space
	 * 4: user doesn't belong to the 'from space', 5: the 'from space' is locked
	 * @author Tyler Jensen
	 */
	@POST
	@Path("/spaces/{spaceId}/add/job")
	@Produces("application/json")
	public String copyJobToSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Make sure we have a list of benchmarks to add and the space it's coming from
		if(null == request.getParameterValues("selectedIds[]") || !Util.paramExists("fromSpace", request)){
			return gson.toJson(2);
		}
		
		// Get the id of the user who initiated the request
		long requestUserId = SessionUtil.getUserId(request);
		
		// Get the space the benchmark is being copied from
		long fromSpace = Long.parseLong(request.getParameter("fromSpace"));
		
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
		
		// Convert the benchmarks to copy to a long list
		List<Long> selectedJobs = Util.toLongList(request.getParameterValues("selectedIds[]"));		
		
		// Make sure the user can see the benchmarks they're trying to copy
		for(long id : selectedJobs) {
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
	 * @return a json string containing '0' if the user(s) were successfully
	 *         removed from the space, else a json string containing '1' if
	 *         there was a database level failure/attempted 'empty' delete, 
	 *         '2' for insufficient permissions, '3' if the leader initiating 
	 *         the removal is in the list of users to remove, and '4' if the 
	 *         list of users to remove contains another leader of the space
	 * @author Todd Elvers
	 */
	@POST
	@Path("/remove/user/{spaceId}")
	@Produces("application/json")
	public String removeUsersFromSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedUsers[]")){
			return gson.toJson(1);
		}		
		
		// Get the id of the user who initiated the removal
		long userIdOfRemover = SessionUtil.getUserId(request);
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveUser()) {
			return gson.toJson(2);	
		}
		
		// Extract the String user id's and convert them to Long
		List<Long> selectedUsers = Util.toLongList(request.getParameterValues("selectedUsers[]"));
		
		// Validate the list of users to remove by:
		// 1 - Ensuring the leader who initiated the removal of users from a space isn't themselves in the list of users to remove
		// 2 - Ensuring other leaders of the space aren't in the list of users to remove
		for(long userId : selectedUsers){
			if(userId == userIdOfRemover){
				return gson.toJson(3);
			}
			perm = Permissions.get(userId, spaceId);
			if(perm.isLeader()){
				return gson.toJson(4);
			}
		}
		
		// If array of users to remove is valid, attempt to remove them from the space
		return Spaces.removeUsers(selectedUsers, spaceId) ? gson.toJson(0) : gson.toJson(1);
	}

	/**
	 * Removes a solver's association with a space, thereby removing the solver
	 * from the space
	 * 
	 * @return a json string containing '0' if the solver was successfully
	 *         removed from the space, else a json string containing '1' if
	 *         there was a database failure/attempted 'empty' delete, 
	 *         '2' for insufficient permissions, '3' if the input was invalid,
	 * @author Todd Elvers
	 */
	@POST
	@Path("/remove/solver/{spaceId}")
	@Produces("application/json")
	public String removeSolversFromSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedSolvers[]")){
			return gson.toJson(1);
		}
		
		// Extract the String solver id's and convert them to Long
		ArrayList<Long> selectedSolvers = new ArrayList<Long>();
		for(String id : request.getParameterValues("selectedSolvers[]")){
			selectedSolvers.add(Long.parseLong(id));
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(perm == null || !perm.canRemoveSolver()) {
			return gson.toJson(2);	
		}

		// Remove the solver from the space
		return Spaces.removeSolvers(selectedSolvers, spaceId) ? gson.toJson(0) : gson.toJson(1);
	}
	
	
	/**
	 * Removes a job's association with a space, thereby removing the job from
	 * the space
	 * 
	 * @return a json string containing '0' if the job was successfully removed
	 *         from the space, else a json string containing '1' if there was a
	 *         database level failure/attempted 'empty' delete, '2' for insufficient
	 *         permissions, '3' if the input was invalid
	 * @author Todd Elvers
	 * @deprecated not yet tested
	 */
	@POST
	@Path("/remove/job/{spaceId}")
	@Produces("application/json")
	public String removeJobsFromSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedJobs[]")){
			return gson.toJson(1);
		}
		
		// Extract the String job id's and convert them to Long
		ArrayList<Long> selectedJobs = new ArrayList<Long>();
		for (String id : request.getParameterValues("selectedJobs[]")) {
			selectedJobs.add(Long.parseLong(id));
		}

		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);
		if (perm == null || !perm.isLeader()) {
			return gson.toJson(2);
		}

		// Remove the job from the space
		return Spaces.removeJobs(selectedJobs, spaceId) ? gson.toJson(0) : gson.toJson(1);
	}
	
	/**
	 * Removes a subspace's association with a space, thereby removing the subspace
	 * from the space
	 * 
	 * @param spaceId the id the space to remove the subspace from
	 * @return a json string containing '0' if the subspace was successfully removed,
	 *		   a string containing '1' for a database level failure/attempted 'empty' delete,
	 *		   '2' for insufficient privileges, and '3' if the subspace to remove is not a leaf subspace (i.e.
	 *		   if it has descendants) 			
	 * @author Todd Elvers
	 */
	@POST
	@Path("/remove/subspace/{spaceId}")
	@Produces("application/json")
	public String removeSubspacesFromSpace(@PathParam("spaceId") long spaceId, @Context HttpServletRequest request) {
		// Prevent users from selecting 'empty', when the table is empty, and trying to delete it
		if(null == request.getParameterValues("selectedSubspaces[]")){
			return gson.toJson(1);
		}
		
		// Extract the String subspace id's and convert them to Long
		ArrayList<Long> selectedSubspaces = new ArrayList<Long>();
		for(String id : request.getParameterValues("selectedSubspaces[]")){
			selectedSubspaces.add(Long.parseLong(id));
		}
		
		// Permissions check; ensures user is the leader of the community
		Permission perm = SessionUtil.getPermission(request, spaceId);		
		if(null == perm || !perm.isLeader()) {
			return gson.toJson(2);	
		}
		
		// Ensures the space to remove is a leaf-space (i.e. has no descendants)
		for(long subspaceId : selectedSubspaces){
			if(!Spaces.isLeaf(subspaceId)){
				return gson.toJson(3);
			}
		}
		

		// Remove the subspace from the space
		return Spaces.removeSubspaces(selectedSubspaces, spaceId) ? gson.toJson(0) : gson.toJson(1);
	}

	/**
	 * Updates the details of a solver. Solver id is required in the path. First
	 * checks if the parameters of the update are valid, then performs the
	 * update.
	 * 
	 * @param id the id of the solver to update the details for
	 * @return 2 if parameters are not valid, 1 if updating the database failed,
	 *         and 0 if the update was successful
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/solver/{id}")
	@Produces("application/json")
	public String editSolverDetails(@PathParam("id") long solverId, @Context HttpServletRequest request) {
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
		long userId = SessionUtil.getUserId(request);
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
	 * @return 2 if the parameters are invalid, 1 if the deletion isn't
	 *         successful, 0 if the deletion was successful
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/solver/{id}")
	@Produces("application/json")
	public String deleteSolver(@PathParam("id") long solverId, @Context HttpServletRequest request) {
		
		// Permissions check; if user is NOT the owner of the solver, deny deletion request
		long userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		if(solver == null || solver.getUserId() != userId){
			gson.toJson(2);
		}
		
		//TODO Need to check if the solver exists in historical space. If it does, we SHOULD NOT delete the solver
		// Delete the solver from the database
		return Solvers.delete(solverId) ? gson.toJson(0) : gson.toJson(1);
	}

	/**
	 * Updates the details of a benchmark. Benchmark id is required in the path.
	 * First checks if the parameters of the update are valid, then performs the
	 * update.
	 * 
	 * @param id the id of the benchmark to update the details for
	 * @return 2 if parameters are not valid, 1 if updating the database failed,
	 *         and 0 if the update was successful
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/benchmark/{id}")
	@Produces("application/json")
	public String deleteBenchmark(@PathParam("id") long benchId, @Context HttpServletRequest request) {
		// Permissions check; if user is NOT the owner of the benchmark, deny deletion request
		long userId = SessionUtil.getUserId(request);		
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
	 * @param id the id of the benchmark to delete
	 * @return 2 if the parameters are invalid, 1 if the deletion isn't
	 *         successful, 0 if the deletion was successful
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/benchmark/{benchId}")
	@Produces("application/json")
	public String editBenchmarkDetails(@PathParam("benchId") long benchId, @Context HttpServletRequest request) {
		boolean isValidRequest = true;
		long type = -1;
		
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("downloadable", request)
				|| !Util.paramExists("type", request)){
			return gson.toJson(3);
		}
		
		// Safely extract the type
		try{
			type = Long.parseLong(request.getParameter("type"));
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
		long userId = SessionUtil.getUserId(request);
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
		long userId = SessionUtil.getUserId(request);
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
	 * @return 0 if the permissions were successfully changed, 1 if there
	 * was an error on the database level, 2 if the user changing the permissions
	 * isn't a leader, and 3 if the user whos permissions are to be changed is 
	 * a leader
	 * @author Todd Elvers
	 */
	@POST
	@Path("/space/{spaceId}/edit/perm/{userId}")
	@Produces("application/json")
	public String editUserPermissions(@PathParam("spaceId") long spaceId, @PathParam("userId") long userId, @Context HttpServletRequest request) {
		
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
	
}