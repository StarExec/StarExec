package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.Database;
import org.starexec.data.to.User;
import org.starexec.util.Validate;


/**
 * @author Tyler & CJ
 * @deprecated This sort of works, but needs to be cleaned out and re-implemented
 */
public class Registration extends HttpServlet {
	private static final Logger log = Logger.getLogger(Registration.class);
	private static final long serialVersionUID = 1L;
	
    public Registration() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Don't accept GET, this could be a malicious request
		log.warn("Illegal GET request to registration servlet from ip address: " + request.getRemoteHost());
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		// Attempt to register new user request
		boolean registered = register(request);
		
		// Redirect user depending on registration outcome
		if(registered == true) {
			response.sendRedirect("/starexec/registration.jsp?result=ok");
		} else {
			response.sendRedirect("/starexec/registration.jsp?result=fail");
		}

	}
	
	
	/**
	 * Attempts to register a new user by performing parameter validation
	 * on a new user request and then adding user to the database
	 * 
	 * @param request the new user request
	 * @return true iff the new user request passed parameter validation and 
	 * was added to the database
	 */
	public static boolean register(HttpServletRequest request){
		// Validate parameters of the new user request
		User newUser = validateRequest(request);
		
		// Attempt to add new user to database if parameters are valid
		if(newUser == null){
			return false;
		} else {
			return Database.addUser(newUser);
		}	
	}
	
	
	/**
	 * Validates the parameters of a received servlet request for user
	 * registration
	 * 
	 * @param request the servlet containing the parameters to validate
	 * @return a new User object if the parameters pass validation, null otherwise
	 */
    public static User validateRequest(HttpServletRequest request){
    	User u = new User();
		u.setFirstName(request.getParameter("firstname"));
		u.setLastName(request.getParameter("lastname"));
		u.setEmail(request.getParameter("email"));
		u.setPassword(request.getParameter("password"));
		u.setInstitution(request.getParameter("institute"));
		
		return (validateUser(u) ? u : null);
    }
    
    
    
    /**
     * Validates the parameters of a User object
     * 
     * @param u the User object to check the parameters of
     * @return true iff all parameters of the User object pass validation
     */
	public static boolean validateUser(User u) {
		// Validate parameters of User object
		if (!Validate.name(u.getFirstName()) 
				|| !Validate.name(u.getLastName()) 
				|| !Validate.email(u.getEmail())
				|| !Validate.institute(u.getInstitution())
				|| !Validate.password(u.getPassword())) {
			log.info("Parameter validation successful");
			return false;
		} else {
			log.info("Parameter validation failed");
			return true;
		}
	}

}
