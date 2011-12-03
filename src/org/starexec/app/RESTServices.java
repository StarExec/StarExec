package org.starexec.app;

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
import org.starexec.data.Database;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.util.Hash;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validate;

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
		
		return gson.toJson(RESTHelpers.toSpaceTree(Database.getSubSpaces(parentId, userId)));
	}	
	
	/**
	 * @return a json string representing all communities within starexec
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/communities/all")
	@Produces("application/json")	
	public String getAllCommunities() {								
		return gson.toJson(RESTHelpers.toCommunityList(Database.getCommunities()));
	}	
	
	/**
	 * @return a json string representing all communities within starexec
	 * @author Tyler Jensen
	 */
	@GET
	@Path("/communities/details/{id}")
	@Produces("application/json")	
	public String getCommunityDetails(@PathParam("id") long id, @Context HttpServletRequest request) {
		Space community = Database.getCommunityDetails(id);
		
		if(community != null) {
			community.setUsers(Database.getSpaceUsers(id));
			Permission p = SessionUtil.getPermission(request, id);
			List<User> leaders = Database.getLeadersOfSpace(id);
			List<Website> sites = Database.getWebsites(id, Database.WebsiteType.SPACE);
			return gson.toJson(new RESTHelpers.CommunityDetails(community, p, leaders, sites));
		}
		
		return gson.toJson(RESTHelpers.toCommunityList(Database.getCommunities()));
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
		
		Space s = Database.getSpaceDetails(spaceId, userId);
		Permission p = SessionUtil.getPermission(request, spaceId);
		
		return limitGson.toJson(new RESTHelpers.SpacePermissionPair(s, p));				
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
	 * @author Skylar Stark
	 */
	@GET
	@Path("/websites/user")
	@Produces("application/json")
	public String getWebsites(@Context HttpServletRequest request) {
		long userId = SessionUtil.getUserId(request);
		return gson.toJson(Database.getWebsites(userId, Database.WebsiteType.USER));
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
			success = Database.addWebsite(userId, url, name, Database.WebsiteType.USER);
		} else if (type.equals("space")) {
			// Make sure this user is capable of adding a website to the space
			Permission perm = SessionUtil.getPermission(request, id);
			if(perm != null && perm.isLeader()) {
				String name = request.getParameter("name");
				String url = request.getParameter("url");			
				success = Database.addWebsite(id, url, name, Database.WebsiteType.SPACE);
			}
		}
		
		if (true == success) {
			return gson.toJson(0);
		}
		
		return gson.toJson(1);
	}
	
	/**
	 * Deletes website information in the database. Website ID is included in the path.
	 * 
	 * @return a json string containing '0' if the delete is successful, '1' otherwise
	 */
	@POST
	@Path("/websites/delete/user/{val}")
	@Produces("application/json")
	public String deleteWebsite(@PathParam("val") long id, @Context HttpServletRequest request) {
		long userId = SessionUtil.getUserId(request);
		boolean success = Database.deleteUserWebsite(id, userId);
		
		if (true == success) {
			return gson.toJson(0);
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
			if (true == Validate.name(newValue)) {
				success = Database.updateFirstName(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setFirstName(newValue);
				}
			}
		} else if (attribute.equals("lastname")) {
			if (true == Validate.name(newValue)) {
				success = Database.updateLastName(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setLastName(newValue);
				}
			}
		} else if (attribute.equals("email")) {
			if (true == Validate.email(newValue)) { 
				success = Database.updateEmail(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setEmail(newValue);
				}
			}
		} else if (attribute.equals("institution")) {
			if (true == Validate.institution(newValue)) {
				success = Database.updateInstitution(userId, newValue);
				if (true == success) {
					SessionUtil.getUser(request).setInstitution(newValue);
				}
			}
		}
		
		// Passed validation AND Database update successful
		if(true == success) {
			return gson.toJson(0);
		}
		
		return gson.toJson(1);
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
			if (true == Validate.spaceName(newName)) {
				success = Database.updateSpaceName(id, newName);
			}
		} else if (attribute.equals("desc")) {
			String newDesc = (String)request.getParameter("val");
			if (true == Validate.description(newDesc)) {
				success = Database.updateSpaceDescription(id, newDesc);				
			}
		}
		
		// Passed validation AND Database update successful
		if(true == success) {
			return gson.toJson(0);
		}
		
		return gson.toJson(1);
	}
	
	/**
	 * Updates the details of a solver.  Solver id is required in the path.  First
	 * checks if the parameters of the update are valid, then performs the update.
	 * 
	 * @param id the id of the solver to update the details for
	 * @return 2 if parameters are not valid, 1 if updating the database failed, and
	 * 0 if the update was successful 
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/solver/{id}")
	@Produces("application/json")
	public String editSolverDetails(@PathParam("id") String id, @Context HttpServletRequest request) {
		boolean isValidRequest = true;
		long solverId = -1;
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("downloadable", request)){
			return gson.toJson(3);
		}
		
		// Ensure the parameters are valid
		if(!Validate.solverBenchName(request.getParameter("name"))
				|| !Validate.description(request.getParameter("description"))
				|| !Validate.bool(request.getParameter("downloadable"))){
			return gson.toJson(3);
		}
		
		
		// Safely extract the solver's id
		try{
			solverId = Long.parseLong(id);
		} catch (NumberFormatException nfe){
			isValidRequest = false;
		}
		if(false == isValidRequest){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the solver, deny update request
		long userId = SessionUtil.getUserId(request);
		Solver solver = Database.getSolver(solverId, userId);
		if(solver == null || solver.getUserId() != userId){
			gson.toJson(2);
		}
		
		// Extract new solver details from request
		String name = request.getParameter("name");
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		
		// Apply new solver details to database
		boolean updated = Database.updateSolverDetails(solverId, name, description, isDownloadable);
		if(updated){
			return gson.toJson(0);
		} else {
			return gson.toJson(1);
		}
	}
	
	/**
	 * Deletes a solver given a solver's id.  The id of the solver to delete must be included
	 * in the path.
	 * 
	 * @param id the id of the solver to delete
	 * @return 2 if the parameters are invalid, 1 if the deletion isn't successful, 0 if the deletion
	 * was successful
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/solver/{id}")
	@Produces("application/json")
	public String deleteSolver(@PathParam("id") String id, @Context HttpServletRequest request) {
		boolean isValidRequest = true;
		long solverId = -1;
		// Safely extract the solver's id
		try{
			solverId = Long.parseLong(id);
		} catch (NumberFormatException nfe){
			isValidRequest = false;
		}
		if(false == isValidRequest){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the solver, deny deletion request
		long userId = SessionUtil.getUserId(request);
		Solver solver = Database.getSolver(solverId, userId);
		if(solver == null || solver.getUserId() != userId){
			gson.toJson(2);
		}
		
		//TODO Need to check if the solver exists in historical space. If it does, we SHOULD NOT delete the solver
		// Delete the solver from the database
		boolean deleted = Database.deleteSolver(solverId);
		if(deleted){
			return gson.toJson(0);
		} else {
			return gson.toJson(1);
		}
	}
	
	/**
	 * Updates the details of a benchmark.  Benchmark id is required in the path.  First
	 * checks if the parameters of the update are valid, then performs the update.
	 * 
	 * @param id the id of the benchmark to update the details for
	 * @return 2 if parameters are not valid, 1 if updating the database failed, and
	 * 0 if the update was successful 
	 * @author Todd Elvers
	 */
	@POST
	@Path("/delete/benchmark/{id}")
	@Produces("application/json")
	public String deleteBenchmark(@PathParam("id") String id, @Context HttpServletRequest request) {
		boolean isValidRequest = true;
		long benchId = -1;
		
		// Safely extract the benchmark's id
		try{
			benchId = Long.parseLong(id);
		} catch (NumberFormatException nfe){
			isValidRequest = false;
		}
		if(false == isValidRequest){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the benchmark, deny deletion request
		long userId = SessionUtil.getUserId(request);
		Benchmark bench = Database.getBenchmark(benchId, userId);
		if(bench == null || bench.getUserId() != userId){
			gson.toJson(2);
		}
		
		//TODO Need to check if the benchmark exists in historical space. If it does, we SHOULD NOT delete the benchmark
		// Delete the benchmark from the database
		boolean deleted = Database.deleteBenchmark(benchId);
		if(deleted){
			return gson.toJson(0);
		} else {
			return gson.toJson(1);
		}
	}
	
	
	/**
	 * Deletes a benchmark given a benchmarks's id.  The id of the benchmark to delete must be included
	 * in the path.
	 * 
	 * @param id the id of the benchmark to delete
	 * @return 2 if the parameters are invalid, 1 if the deletion isn't successful, 0 if the deletion
	 * was successful
	 * @author Todd Elvers
	 */
	@POST
	@Path("/edit/benchmark/{id}")
	@Produces("application/json")
	public String editBenchmarkDetails(@PathParam("id") String id, @Context HttpServletRequest request) {
		boolean isValidRequest = true;
		long benchId = -1;
		long type = -1;
		
		// Ensure the parameters exist
		if(!Util.paramExists("name", request)
				|| !Util.paramExists("description", request)
				|| !Util.paramExists("downloadable", request)
				|| !Util.paramExists("type", request)){
			return gson.toJson(3);
		}
		
		// Safely extract the benchmark's id & type
		try{
			benchId = Long.parseLong(id);
			type = Long.parseLong(request.getParameter("type"));
		} catch (NumberFormatException nfe){
			isValidRequest = false;
		}
		if(false == isValidRequest){
			return gson.toJson(3);
		}
		
		// Ensure the parameters are valid
		if(!Validate.solverBenchName(request.getParameter("name"))
				|| !Validate.description(request.getParameter("description"))
				|| !Validate.bool(request.getParameter("downloadable"))
				|| !Validate.benchmarkType(type)){
			return gson.toJson(3);
		}
		
		// Permissions check; if user is NOT the owner of the benchmark, deny update request
		long userId = SessionUtil.getUserId(request);
		Benchmark bench = Database.getBenchmark(benchId, userId);
		if(bench == null || bench.getUserId() != userId){
			gson.toJson(2);
		}
		
		// Extract new benchmark details from request
		String name = request.getParameter("name");
		String description = request.getParameter("description");
		boolean isDownloadable = Boolean.parseBoolean(request.getParameter("downloadable"));
		
		// Apply new benchmark details to database
		boolean updated = Database.updateBenchmarkDetails(benchId, name, description, isDownloadable, type);
		if(updated){
			return gson.toJson(0);
		} else {
			return gson.toJson(1);
		}
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
		String databasePass = Database.getPassword(userId);
		
		if (hashedPass.equals(databasePass)) {
			if (newPass.equals(confirmPass)) {
				if (true == Validate.password(newPass)) {
					//updatePassword requires the plaintext password
					if (true == Database.updatePassword(userId, newPass)) {
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
	
	
}
