package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.CommunityRequest;
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
 * @author Todd Elvers
 */
public class CommunityRequester extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(CommunityRequester.class);
	private String errorMessage;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			User user = SessionUtil.getUser(request);

			// Validate parameters of request & construct Invite object
			CommunityRequest comRequest = constructComRequest(user, request);
			if (comRequest == null) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, errorMessage));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
				return;
			}

			boolean added = Requests.addCommunityRequest(user, comRequest.getCommunityId(), comRequest.getCode(),
			                                             comRequest.getMessage()
			);
			if (added) {
				// Send the invite to the leaders of the community
				Mail.sendCommunityRequest(user, comRequest);
				response.sendRedirect(Util.docRoot(
						"secure/add/to_community.jsp?result=requestSent&cid=" + comRequest.getCommunityId()));
			} else {
				// There was a problem
				response.sendRedirect(Util.docRoot(
						"secure/add/to_community.jsp?result=requestNotSent&cid=" + comRequest.getCommunityId()));
			}
		} catch (Exception e) {
			log.warn("Caught Exception in CommunityRequester.doPost", e);
			throw e;
		}
	}

	/**
	 * Builds an Invite object given a user and request
	 *
	 * @param user the user to create the invite for
	 * @param request the servlet containing the invite information
	 * @return the invite constructed
	 */
	private CommunityRequest constructComRequest(User user, HttpServletRequest request) {
		try {

			ValidatorStatusCode status = validateParameters(request, user.getId());
			if (status.isSuccess()) {
				String message = request.getParameter(Registration.USER_MESSAGE);
				int communityId = Integer.parseInt(request.getParameter(Registration.USER_COMMUNITY));
				CommunityRequest req = new CommunityRequest();
				req.setUserId(user.getId());
				req.setCommunityId(communityId);
				req.setCode(UUID.randomUUID().toString());
				req.setMessage(message);
				return req;
			} else {
				errorMessage = status.getMessage();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Validates the parameters that will be used to construct the Invite object by checking that the communityId is
	 * valid and that message is between 1 and 300 characters.
	 *
	 * @param request the HTTP request to validate.
	 * @param userId the id of the user making the request.
	 * @return true iff communityId is a valid space_id that is a child of the root space and that the user's
	 * message is
	 * between 1 and 300 characters in length
	 */
	private ValidatorStatusCode validateParameters(HttpServletRequest request, int userId) {
		if (!Util.paramExists(Registration.USER_COMMUNITY, request)) {
			return new ValidatorStatusCode(false, "You need to provide a community ID");
		}

		if (!Util.paramExists(Registration.USER_MESSAGE, request)) {
			return new ValidatorStatusCode(false, "You need to provide a message explaining why you want to join");
		}
		String message = request.getParameter(Registration.USER_MESSAGE);
		int communityId = Integer.parseInt(request.getParameter(Registration.USER_COMMUNITY));


		if (!Validator.isValidRequestMessage(message)) {
			return new ValidatorStatusCode(
					false, "The given message is invalid-- please refer to the help pages to see the valid format");
		}
		if (!Communities.isCommunity(communityId)) {
			return new ValidatorStatusCode(false, "The given ID does not represent any community");
		}
		if (Users.isPublicUser(userId)) {
			return new ValidatorStatusCode(false, "You cannot request a new community as a guest");
		}
		return new ValidatorStatusCode(true);
	}
}
