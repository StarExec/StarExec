package org.starexec.servlets;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;
import org.starexec.constants.R;
import org.starexec.constants.Web;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.logger.StarLogger;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet that handles email verification for new users, emailing leaders of spaces when new users request to join, and
 * accept/decline responses sent back from leaders about user join requests
 *
 * @author Todd Elvers & Tyler Jensen
 */
public class Verify extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(Verify.class);
	private static final Gson gson = new Gson();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (Util.paramExists(Mail.CHANGE_EMAIL_CODE, request)) {
				// Handle change email request
				handleEmailChange(request, response);
			} else if (Util.paramExists(Mail.EMAIL_CODE, request) && !Util.paramExists(Mail.LEADER_RESPONSE,
			                                                                           request)) {
				// Handle user activation request
				handleActivation(request, response);
			} else if (Util.paramExists(Mail.EMAIL_CODE, request) && Util.paramExists(Mail.LEADER_RESPONSE, request)) {
				// Handle community request accept/decline
				handleAcceptance(request, response);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			log.warn("Caught Exception in Verify.doGet", e);
			throw e;
		}
	}

	/**
	 * Deals with email change request verification.
	 *
	 * @param request the servlet containing the incoming GET request
	 * @param response the servlet that handles redirection
	 * @throws IOException if any redirect fails
	 */
	private void handleEmailChange(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int userId = SessionUtil.getUserId(request);
		log.debug("(handleEmailChange) User with id=" + userId + " visited change email verification page.");
		String codeParam = (String) request.getParameter(Mail.CHANGE_EMAIL_CODE);
		Pair<String, String> emailAndCode = null;
		try {
			if (Requests.changeEmailRequestExists(userId)) {
				emailAndCode = Requests.getChangeEmailRequest(userId);
			} else {
				// The email change request does not exist in the database.
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			String newEmail = emailAndCode.getLeft();
			String codeInDb = emailAndCode.getRight();
			if (codeInDb.equals(codeParam)) {
				Users.updateEmail(userId, newEmail);
				Requests.deleteChangeEmailRequest(userId);
				response.sendRedirect(Util.docRoot("public/messages/email_changed.jsp?email=" + newEmail));
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (StarExecDatabaseException e) {
			log.error("Database error while trying to change users email.", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Deals with acceptance email responses from leaders of a group
	 *
	 * @param request the servlet containing the incoming GET request
	 * @param response the servlet that handles redirection
	 * @throws IOException if any of the redirects fail
	 * @author Todd Elvers
	 */
	private void handleAcceptance(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String code = (String) request.getParameter(Mail.EMAIL_CODE);
		String verdict = (String) request.getParameter(Mail.LEADER_RESPONSE);

		CommunityRequest comRequest = Requests.getCommunityRequest(code);

		// TODO Give requests that were sent by email parameter too.
		boolean sentFromCommunityPage = Util.paramExists(Web.SENT_FROM_COMMUNITY_PAGE, request);

		boolean requestHasBeenHandled = checkIfRequestHasBeenHandled(response, comRequest, sentFromCommunityPage);
		if (requestHasBeenHandled) {
			return;
		}

		String status = "";
		switch (verdict) {
		case Web.APPROVE_COMMUNITY_REQUEST:
			comRequest.approve();
			status = "The user has been successfully approved.";
			break;
		case Web.DECLINE_COMMUNITY_REQUEST:
			comRequest.decline();
			status = "The user has been successfully declined.";
			break;
		}

		if (sentFromCommunityPage) {
			response.setContentType("application/json");
			response.getWriter()
			        .write(gson.toJson(new ValidatorStatusCode(true, status)));
		} else {
			response.sendRedirect(Util.docRoot("public/messages/leader_response.jsp"));
		}

		log.debug("Finished handling community request.");
	}

	private static boolean checkIfRequestHasBeenHandled(
			HttpServletResponse response, CommunityRequest comRequest, boolean sentFromCommunityPage
	) throws IOException {
		// Check if a leader has handled this acceptance email already
		if (comRequest == null) {
			// If so, redirect them to the leader_response.jsp and tell them their response will be ignored
			if (sentFromCommunityPage) {
				response.setContentType("application/json");
				response.getWriter().write(gson.toJson(
						new ValidatorStatusCode(false, "This user already been declined/accepted.")));
			} else {
				response.sendRedirect(Util.docRoot("public/messages/leader_response.jsp?result=dupLeaderResponse"));
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Handles the email verification hyperlinks and activates the given user if the activation code they provide
	 * matches that from the table VERIFY
	 *
	 * @param request the servlet containing the incoming GET request
	 * @param response the servlet that handles redirection
	 * @throws IOException if any of the redirects fail
	 * @author Todd Elvers & Tyler Jensen
	 */
	private void handleActivation(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String code = request.getParameter(Mail.EMAIL_CODE).toString();

		// IF no code in VERIFY matches, then userId = -1
		// ELSE userId = the id of the user that was just activated
		int userId = Requests.redeemActivationCode(code);

		User newUser;
		if (userId == -1) {
			log.info("email verification failed - likely a duplicate activation attempt");
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND, "This activation page has expired and no longer exists!");
			return;
		} else {
			newUser = Users.getUnregistered(userId);
			log.info("User [" + newUser.getFullName() + "] has been activated.");
			response.sendRedirect(Util.docRoot("public/messages/email_activated.jsp"));
		}

		CommunityRequest comReq = Requests.getCommunityRequest(userId);
		if (comReq == null) {
			log.warn("No community request exists for user [" + newUser.getFullName() + "].");
			return;
		}

		// Send the invite to the leaders of the community
		Mail.sendCommunityRequest(newUser, comReq);
	}
}
