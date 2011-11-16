package org.starexec.servlets;

import java.io.IOException;
import java.util.UUID;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.P;
import org.starexec.data.Database;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.Validate;


/**
 * @author Tyler & CJ & Todd
 * @deprecated This sort of works, but needs to be cleaned out and re-implemented
 */
@Deprecated
public class Registration extends HttpServlet {
	private static final Logger log = Logger.getLogger(Registration.class);
	private static final long serialVersionUID = 1L;
	
    public Registration() {
        super();
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// Don't accept GET requests to this servlet
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Illegal GET request!");
	}


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		// Begin registration for a new user
		int result = register(request, response);
		
		
		switch (result) {
		  case 0: 
			// Notify user of successful registration
			response.sendRedirect("/starexec/registration.jsp?result=regSuccess");
		    break;
		  case 1: 
			// Notify user the email address they inputed is already in use
			response.sendRedirect("/starexec/registration.jsp?result=regFail");
		    break;
		  default:
			// Handle malformed urls
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter validation failed");
		    break;
		}

	}
	

	
	
	/**
	 * Begins the registration process for a new user by adding them to the
	 * database as not-activated & not-approved, and sends them an activation
	 * email
	 * 
	 * @param request the servlet containing the incoming POST
	 * @return 0 if registration was successful, and 1 if the user already exists, and
	 * -1 if parameter validation fails
	 * @author Todd Elvers
	 */
	public static int register(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// Validate parameters of the new user request
		User user = validateParameters(request);
		if(user == null){
			log.info(String.format("Registration was unsuccessfully started for new user because parameter validation failed."));
			return -1;
		}
		
		long communityId = Long.parseLong(request.getParameter(P.USER_COMMUNITY));
		
		// Generate unique code to safely reference this user's entry in VERIFY from hyperlinks
		String code = UUID.randomUUID().toString();
		
		// Add user to the database and get the UUID that was created
		boolean added = Database.addUser(user, communityId, code, request.getParameter(P.USER_MESSAGE));
		
		// If the user was successfully added to the database, send an activation email
		if(added){
			log.info(String.format("Registration was successfully started for user [%s].", user.getFullName()));
			Mail.sendActivationCode(user, code, request);
			return 0;
		} else {
			log.info(String.format("Registration was unsuccessfully started for user [%s] because a user already exists with that email address.", user.getFullName()));
			return 1;
		} 
	}
	
	
	/**
	 * Validates the parameters of a servlet request for user
	 * registration
	 * 
	 * @param request the servlet containing the parameters to validate
	 * @return a new User object if the parameters pass validation, null otherwise
	 * @author Todd Elvers
	 */
    public static User validateParameters(HttpServletRequest request){
    	User u = new User();
		u.setFirstName(request.getParameter(P.USER_FIRSTNAME));
		u.setLastName(request.getParameter(P.USER_LASTNAME));
		u.setEmail(request.getParameter(P.USER_EMAIL));
		u.setPassword(request.getParameter(P.USER_PASSWORD));
		u.setInstitution(request.getParameter(P.USER_INSTITUTION));
		
		return (validateUser(u) ? u : null);
    }
    
    
    
    /**
     * Validates the parameters of a User object
     * 
     * @param u the User object to check the parameters of
     * @return true iff all parameters of the User object pass validation
     * @author Todd Elvers
     */
	public static boolean validateUser(User u) {
		// Validate parameters of User object
		if (!Validate.name(u.getFirstName()) 
				|| !Validate.name(u.getLastName()) 
				|| !Validate.email(u.getEmail())
				|| !Validate.institution(u.getInstitution())
				|| !Validate.password(u.getPassword())) {
			return false;
		} else {
			return true;
		}
	}

}
