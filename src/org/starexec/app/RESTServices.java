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
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.util.Hash;
import org.starexec.util.SessionUtil;
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
		log.debug("getSubSpaces parentId=" + parentId);		
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
		log.debug("getSpaceDetails id=" + spaceId);		
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
		log.debug("doInvalidateSession user=" + request.getSession().getAttribute("user"));
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
