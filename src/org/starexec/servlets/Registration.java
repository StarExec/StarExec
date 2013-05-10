package org.starexec.servlets;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Users;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles requests for registration 
 * @author Todd Elvers & Tyler Jensen
 */
@SuppressWarnings("serial")
public class Registration extends HttpServlet {
	private static final Logger log = Logger.getLogger(Registration.class);	
	
	// Return codes for registration
	private static final int SUCCESS = 0;
	private static final int FAIL = 1;
	private static final int MALFORMED = 2;
	
	// Param strings for processing
	public static String USER_COMMUNITY = "cm";
	public static String USER_PASSWORD = "pwd";
	public static String USER_INSTITUTION = "inst";
	public static String USER_EMAIL = "em";
	public static String USER_FIRSTNAME = "fn";
	public static String USER_LASTNAME = "ln";
	public static String USER_MESSAGE = "msg";
	public static String USER_ARCHIVE_TYPE = "pat";
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		// Begin registration for a new user		
		int result = register(request, response);
				
		switch (result) {
		  case SUCCESS: 
			// Notify user of successful registration
			response.sendRedirect("/starexec/public/registration.jsp?result=regSuccess");
		    break;
		  case FAIL: 
			// Notify user the email address they inputed is already in use
			response.sendRedirect("/starexec/public/registration.jsp?result=regFail");
		    break;
		  case MALFORMED:
			// Handle malformed urls
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
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
	 * 2 if parameter validation fails
	 * @author Todd Elvers
	 */
	public static int register(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// Validate parameters of the new user request
		if(!validateRequest(request)) {
			log.info(String.format("Registration was unsuccessfully started for new user because parameter validation failed."));
			return MALFORMED;
		}
		
		boolean uniqueEmail = Users.getUserByEmail(request.getParameter(Registration.USER_EMAIL));
		if (uniqueEmail) {
			log.error("Duplicate entry " + request.getParameter(Registration.USER_EMAIL));
			return FAIL;
		}
		
		// Create the user to add to the database
		User user = new User();
		user.setFirstName(request.getParameter(Registration.USER_FIRSTNAME));
		user.setLastName(request.getParameter(Registration.USER_LASTNAME));
		user.setEmail(request.getParameter(Registration.USER_EMAIL));
		user.setPassword(request.getParameter(Registration.USER_PASSWORD));
		user.setInstitution(request.getParameter(Registration.USER_INSTITUTION));
		user.setArchiveType(request.getParameter(Registration.USER_ARCHIVE_TYPE));
		
		int communityId = Integer.parseInt(request.getParameter(Registration.USER_COMMUNITY));
		
		// Generate unique code to safely reference this user's entry in verification hyperlinks
		String code = UUID.randomUUID().toString();
		
		// Add user to the database and get the UUID that was created
		boolean added = Users.register(user, communityId, code, request.getParameter(Registration.USER_MESSAGE));
		
		// If the user was successfully added to the database, send an activation email
		if(added) {
			log.info(String.format("Registration was successfully started for user [%s].", user.getFullName()));
			
			String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
			Mail.sendActivationCode(user, code, serverName);
			return SUCCESS;
		} else {
			log.info(String.format("Registration was unsuccessfully started for user [%s].", user.getFullName()));
			return FAIL;
		} 
	}
	
	
	/**
	 * Validates the parameters of a servlet request for user registration
	 * 
	 * @param request the servlet containing the parameters to validate
	 * @return true if the request is valid, false otherwise
	 * @author Todd Elvers
	 */
    public static boolean validateRequest(HttpServletRequest request) {
    	try {
    		// Ensure the necessary parameters exist
	    	if(!Util.paramExists(Registration.USER_FIRSTNAME, request) ||
	    	   !Util.paramExists(Registration.USER_LASTNAME, request) ||
	    	   !Util.paramExists(Registration.USER_EMAIL, request) ||
	    	   !Util.paramExists(Registration.USER_PASSWORD, request) ||
	    	   !Util.paramExists(Registration.USER_INSTITUTION, request) ||
	    	   !Util.paramExists(Registration.USER_COMMUNITY, request) ||
	    	   !Util.paramExists(Registration.USER_MESSAGE, request) ||
	    	   !Util.paramExists(Registration.USER_ARCHIVE_TYPE, request)) {
	    		return false;
	    	}    	    	   
		    
	    	// Make sure community id is a valid int 
	    	Integer.parseInt(request.getParameter(Registration.USER_COMMUNITY));
	    	
	    	// Ensure the parameters are valid values
	    	if (!Validator.isValidUserName((String)request.getParameter(Registration.USER_FIRSTNAME)) 
					|| !Validator.isValidUserName((String)request.getParameter(Registration.USER_LASTNAME)) 
					|| !Validator.isValidEmail((String)request.getParameter(Registration.USER_EMAIL))
					|| !Validator.isValidInstitution((String)request.getParameter(Registration.USER_INSTITUTION))
					|| !Validator.isValidPassword((String)request.getParameter(Registration.USER_PASSWORD))
					|| !Validator.isValidArchiveType((String)request.getParameter(Registration.USER_ARCHIVE_TYPE))) {
				return false;
			}
	    	
	    	return true;
    	} catch (Exception e) {
    		log.warn(e.getMessage(), e);
    	}
    	return false;
    }        
}
