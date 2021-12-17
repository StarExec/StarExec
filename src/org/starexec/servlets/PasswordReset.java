package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.util.Mail;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles a user's request to reset their password by emailing them a link that takes them to a page where they receive
 * a temporary password
 *
 * @author Todd Elvers
 */

public class PasswordReset extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(PasswordReset.class);
	public static final String PASS_RESET = "reset";        // Param string for password reset codes

	/**
	 * This is the second half of the procedure -- the user has already requested a password reset and received an
	 * email
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (Util.paramExists(PasswordReset.PASS_RESET, request)) {
				// Try and redeem the code from the database
				String code = request.getParameter(PasswordReset.PASS_RESET);
				int userId = Requests.redeemPassResetRequest(code);
				// If code is successfully redeemed, set a new temporary password and display it to the user
				if (userId > 0) {
					String tempPass = Util.getTempPassword();
					request.getSession().setAttribute("pwd", tempPass);
					if (Users.updatePassword(userId, tempPass)) {
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
		} catch (Exception e) {
			log.warn("Caught Exception in PasswordReset.doGet", e);
			throw e;
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			// Ensure the parameters are well formed
			ValidatorStatusCode status = isPostRequestValid(request);
			if (status.isSuccess()) {

				// Check if the provided credentials match any in the database
				User user = Users.get(request.getParameter(Registration.USER_EMAIL));
				if (user == null ||
						!user.getFirstName().equalsIgnoreCase(request.getParameter(Registration.USER_FIRSTNAME)) ||
						!user.getLastName().equalsIgnoreCase(request.getParameter(Registration.USER_LASTNAME))) {
					response.sendRedirect(Util.docRoot("public/password_reset.jsp?result=noUserFound"));
					return;
				}

				String code = UUID.randomUUID().toString();

				// Add the reset request to the database
				if (!Requests.addPassResetRequest(user.getId(), code)) {
					log.info(String.format("Failed to add password reset request for user [%s]", user.getFullName()));
					return;
				}

				// Email the password reset hyperlink to the user
				Mail.sendPasswordReset(user, code);

				response.sendRedirect(Util.docRoot("public/password_reset.jsp?result=success"));
			} else {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
			}
		} catch (Exception e) {
			log.warn("Caught Exception in PasswordReset.doPost", e);
			throw e;
		}
	}

	/**
	 * Validates the parameters of the password reset request
	 *
	 * @param request the serlvet containing the parameters to be validated
	 * @return true iff the first name, last name, and email address in the password reset request exist and are valid
	 */
	private static ValidatorStatusCode isPostRequestValid(HttpServletRequest request) {
		try {

			// Ensure the parameters are valid values
			if (!Validator.isValidUserName((String) request.getParameter(Registration.USER_FIRSTNAME))) {
				return new ValidatorStatusCode(
						false,
						"The given first name is invalid-- please refer to the help files to see the proper format"
				);
			}

			// Ensure the parameters are valid values
			if (!Validator.isValidUserName((String) request.getParameter(Registration.USER_LASTNAME))) {
				return new ValidatorStatusCode(
						false,
						"The given last name is invalid-- please refer to the help files to see the proper format"
				);
			}

			// Ensure the parameters are valid values
			if (!Validator.isValidEmail((String) request.getParameter(Registration.USER_EMAIL))) {
				return new ValidatorStatusCode(
						false, "The given email is invalid-- please refer to the help files to see the proper format");
			}


			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "There was an internal error resetting your password");
	}
}
