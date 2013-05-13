package org.starexec.servlets;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Users;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Handles a user's request to reset their password by emailing them a link that
 * takes them to a page where they receive a temporary password
 * 
 * @author Todd Elvers
 */
@SuppressWarnings("serial")
public class PasswordReset extends HttpServlet {
	private static final Logger log = Logger.getLogger(PasswordReset.class);
	public static final String PASS_RESET = "reset";		// Param string for password reset codes
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(Util.paramExists(PasswordReset.PASS_RESET, request)) {
			// Try and redeem the code from the database
			String code = request.getParameter(PasswordReset.PASS_RESET);
			int userId = Requests.redeemPassResetRequest(code);
			// If code is successfully redeemed, set a new temporary password and display it to the user
			if(userId > 0){
				String tempPass = Util.getTempPassword();
				request.getSession().setAttribute("pwd", tempPass);
				if(Users.setPassword(userId, tempPass)){
					log.debug(String.format("Temporary password successfully set for user id [%d]", userId));
					response.sendRedirect(Util.docRoot("public/temp_pass.jsp"));
				}
			} else {
				// Hyperlinks can only be visited once; notify user this hyperlink has expired
			    response.sendRedirect(Util.docRoot("public/password_reset.jsp?result=expired"));
			}
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		// Ensure the parameters are well formed
		if(isRequestValid(request)){
			
			// Check if the provided credentials match any in the database
			User user = Users.get(request.getParameter(Registration.USER_EMAIL));
			if(user == null
					|| !user.getFirstName().equalsIgnoreCase(request.getParameter(Registration.USER_FIRSTNAME))
					|| !user.getLastName().equalsIgnoreCase(request.getParameter(Registration.USER_LASTNAME))){
			    response.sendRedirect(Util.docRoot("public/password_reset.jsp?result=noUserFound"));
				return;
			}
			
			String code = UUID.randomUUID().toString();
			
			// Add the reset request to the database
			if(false == Requests.addPassResetRequest(user.getId(), code)){
				log.info(String.format("Failed to add password reset request for user [%s]", user.getFullName()));
				return;
			}
			
			// Email the password reset hyperlink to the user 
			Mail.sendPasswordReset(user, code);
			
			response.sendRedirect(Util.docRoot("public/password_reset.jsp?result=success"));
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
		}
	}
	

	
	/**
	 * Validates the parameters of the password reset request
	 * 
	 * @param request the serlvet containing the parameters to be validated
	 * @return true iff the first name, last name, and email address in the 
	 * password reset request exist and are valid
	 */
	private static boolean isRequestValid(HttpServletRequest request) {
		try {
			// Ensure the necessary parameters exist
			if (!Util.paramExists(Registration.USER_FIRSTNAME, request)
					|| !Util.paramExists(Registration.USER_LASTNAME, request)
					|| !Util.paramExists(Registration.USER_EMAIL, request)) {
				return false;
			}
			
			// Ensure the parameters are valid values
			if (!Validator.isValidUserName((String) request.getParameter(Registration.USER_FIRSTNAME))
					|| !Validator.isValidUserName((String) request.getParameter(Registration.USER_LASTNAME))
					|| !Validator.isValidEmail((String) request.getParameter(Registration.USER_EMAIL))) {
				return false;
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return true;
	}

}
