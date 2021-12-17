package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Users;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
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
 * Servlet which handles requests for registration
 *
 * @author Todd Elvers & Tyler Jensen
 */
public class Registration extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(Registration.class);

	// Param strings for processing
	public static final String USER_COMMUNITY = "cm";
	public static final String USER_PASSWORD = "pwd";
	public static final String USER_INSTITUTION = "inst";
	public static final String USER_EMAIL = "em";
	public static final String USER_FIRSTNAME = "fn";
	public static final String USER_LASTNAME = "ln";
	public static final String USER_MESSAGE = "msg";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final String method = "doPost";
			log.entry(method);

			// Begin registration for a new user
			ValidatorStatusCode result = register(request);
			if (result.isSuccess()) {
				response.sendRedirect(Util.docRoot("public/registrationConfirmation.jsp"));
			} else {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, result.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, result.getMessage());
			}
			log.exit(method);
		} catch (Exception e) {
			log.warn("Caught Exception in Registration.doPost.", e);
			throw e;
		}
	}

	/**
	 * Begins the registration process for a new user by adding them to the database as not-activated & not-approved,
	 * and sends them an activation email
	 *
	 * @param request the servlet containing the incoming POST
	 * @return 0 if registration was successful, and 1 if the user already exists, and 2 if parameter validation fails
	 * @author Todd Elvers
	 */
	public static ValidatorStatusCode register(HttpServletRequest request) throws IOException {

		// Validate parameters of the new user request
		ValidatorStatusCode status = validateRequest(request);
		if (!status.isSuccess()) {
			return status;
		}

		// Create the user to add to the database
		User user = new User();
		user.setFirstName(request.getParameter(Registration.USER_FIRSTNAME));
		user.setLastName(request.getParameter(Registration.USER_LASTNAME));
		user.setEmail(request.getParameter(Registration.USER_EMAIL));
		user.setPassword(request.getParameter(Registration.USER_PASSWORD));
		user.setInstitution(request.getParameter(Registration.USER_INSTITUTION));
		user.setPairQuota(R.DEFAULT_PAIR_QUOTA);
		user.setDiskQuota(R.DEFAULT_DISK_QUOTA);
		user.setRole("user");
		int communityId = Integer.parseInt(request.getParameter(Registration.USER_COMMUNITY));

		int userIdOfRequest = -1;
		try {
			userIdOfRequest = SessionUtil.getUserId(request);
		} catch (Exception e) {
			//this occurs when someone tries to register, as they have no user ID
			userIdOfRequest = -1;
		}


		boolean adminCreated = GeneralSecurity.hasAdminWritePrivileges(userIdOfRequest);

		if (adminCreated) {
			int id = Users.add(user);
			boolean success = Users.associate(id, communityId);
			if (success) {
				Mail.sendPassword(user, request.getParameter(Registration.USER_PASSWORD));
				return new ValidatorStatusCode(true);
			} else {
				return new ValidatorStatusCode(false, "Internal database error registering user");
			}
		} else {
			// Generate unique code to safely reference this user's entry in verification hyperlinks
			String code = UUID.randomUUID().toString();

			// Add user to the database and get the UUID that was created
			boolean added = Users.register(user, communityId, code, request.getParameter(Registration.USER_MESSAGE));

			// If the user was successfully added to the database, send an activation email
			if (added) {
				log.info(String.format("Registration was successfully started for user [%s].", user.getFullName()));

				Mail.sendActivationCode(user, code);
				return new ValidatorStatusCode(true);
			} else {
				log.info(String.format("Registration was unsuccessfully started for user [%s].", user.getFullName()));
				return new ValidatorStatusCode(false, "Internal database error registering user");
			}
		}
	}

	/**
	 * Validates the parameters of a servlet request for user registration
	 *
	 * @param request the servlet containing the parameters to validate
	 * @return true if the request is valid, false otherwise
	 * @author Todd Elvers
	 */
	public static ValidatorStatusCode validateRequest(HttpServletRequest request) {
		try {

			// Ensure the necessary parameters exist
			if (!Util.paramExists(Registration.USER_PASSWORD, request)) {
				return new ValidatorStatusCode(false, "You need to supply a password");
			}

			if (!Validator.isValidPosInteger(request.getParameter(Registration.USER_COMMUNITY))) {
				return new ValidatorStatusCode(false, "The given community id is not a valid integer");
			}


			// Ensure the parameters are valid values
			if (!Validator.isValidUserName((String) request.getParameter(Registration.USER_FIRSTNAME))) {
				return new ValidatorStatusCode(false,
				                               "The given first name is not valid-- please refer to the help files to " +
						                               "see the proper format");
			}

			// Ensure the parameters are valid values
			if (!Validator.isValidUserName((String) request.getParameter(Registration.USER_LASTNAME))) {
				return new ValidatorStatusCode(false,
				                               "The given last name is not valid-- please refer to the help files to " +
						                               "see the proper format");
			}

			// Ensure the parameters are valid values
			if (!Validator.isValidEmail((String) request.getParameter(Registration.USER_EMAIL))) {
				return new ValidatorStatusCode(
						false,
						"The given email address is not valid-- please refer to the help files to see the proper " +
								"format"
				);
			}

			// Ensure the parameters are valid values
			if (!Validator.isValidInstitution((String) request.getParameter(Registration.USER_INSTITUTION))) {
				return new ValidatorStatusCode(
						false,
						"The given institution is not valid-- please refer to the help files to see the proper format"
				);
			}

			int userIdOfRequest = -1;
			try {
				userIdOfRequest = SessionUtil.getUserId(request);
			} catch (Exception e) {
				//this occurs when someone tries to register, as they have no user ID
			}

			//administrators don't need to provide a message
			if (!GeneralSecurity.hasAdminWritePrivileges(userIdOfRequest)) {
				if (!Validator.isValidRequestMessage(request.getParameter(Registration.USER_MESSAGE))) {
					return new ValidatorStatusCode(
							false,
							"The given request message is not valid-- please refer to the help files to see the proper" +
									" format"
					);
				}
			}

			boolean notUniqueEmail = Users.getUserByEmail(request.getParameter(Registration.USER_EMAIL));
			if (notUniqueEmail) {
				return new ValidatorStatusCode(false, "The email address you specified has already been registered");
			}

			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "There was an internal error processing your registration request");
	}
}
