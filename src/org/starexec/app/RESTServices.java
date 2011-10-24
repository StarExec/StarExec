package org.starexec.app;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.starexec.constants.P;
import org.starexec.data.Database;
import org.starexec.data.to.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Class which handles all RESTful web service requests.
 */
@Path("")
public class RESTServices {	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	private static Gson gson = new Gson();
	
	public RESTServices(){
		
	}
	
	@GET
	@Path("/websites")
	@Produces("application/json")
	public String getWebsites(@Context HttpServletRequest request) {
		long userId = ((User)(request.getSession().getAttribute(P.SESSION_USER))).getId();
		String websites = Database.getWebsites(userId);
		
		return gson.toJson(websites);
	}
	
	/** 
	 * Updates information in the database using a POST. Attribute and
	 * new value are included in the path.
	 * @author Skylar Stark
	 */
	@POST
	@Path("/edit/user/{attr}/{val}")
	@Produces("application/json")
	public String editUserInfo(@PathParam("attr") String attribute, @PathParam("val") String newValue, @Context HttpServletRequest request) {	
		long userId = ((User)(request.getSession().getAttribute(P.SESSION_USER))).getId();
		boolean success = false;
		
		if (attribute.equals("firstname")) {
			success = Database.updateFirstName(userId, newValue);
			if (true == success) {
				((User)(request.getSession().getAttribute(P.SESSION_USER))).setFirstName(newValue);
			}
		} else if (attribute.equals("lastname")) {
			success = Database.updateLastName(userId, newValue);
			if (true == success) {
				((User)(request.getSession().getAttribute(P.SESSION_USER))).setLastName(newValue);
			}
		} else if (attribute.equals("email")) {
			success = Database.updateEmail(userId, newValue);
			if (true == success) {
				((User)(request.getSession().getAttribute(P.SESSION_USER))).setEmail(newValue);
			}
		} else if (attribute.equals("institution")) {
			success = Database.updateInstitution(userId, newValue);
			if (true == success) {
				((User)(request.getSession().getAttribute(P.SESSION_USER))).setInstitution(newValue);
			}
		}
			
		if(true == success) {
			return gson.toJson(0);
		}
		
		return gson.toJson(1);
	}
}