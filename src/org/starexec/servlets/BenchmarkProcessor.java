package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Processor;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;

/**
 * Servlet which handles incoming requests to reprocess benchmarks
 *
 * @author Tyler Jensen
 */
public class BenchmarkProcessor extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(BenchmarkProcessor.class);

	// Request attributes
	private static final String PROCESSOR_ID = "pid";
	private static final String SPACE_ID = "sid";

	private static final String SPACE_HIERARCHY = "hier";
	private static final String CLEAR_OLD = "clear";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@GET
	@Path("/process/benchmarks")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			// If we're dealing with an upload request...

			// Make sure the reque st is valid
			ValidatorStatusCode status = isValidProcessRequest(request);
			if (!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
				return;
			}

			int userId = SessionUtil.getUserId(request);
			int spaceId = Integer.parseInt((String) request.getParameter(SPACE_ID));
			boolean clearOld = Boolean.parseBoolean((String) request.getParameter(CLEAR_OLD));
			int pid = Integer.parseInt((String) request.getParameter(PROCESSOR_ID));
			boolean hier = Boolean.parseBoolean((String) request.getParameter(SPACE_HIERARCHY));

			Processor p = Processors.get(pid);

			Integer statusId = Benchmarks.process(spaceId, p, hier, userId, clearOld);
			if (statusId != null) {
				response.sendRedirect(Util.docRoot("secure/details/uploadStatus.jsp?id=" + statusId));
			} else {
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error processing the benchmarks");
			}
		} catch (Exception e) {
			log.warn("Caught Exception in BenchmarkProcessor.doPost.", e);
			response.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an error processing the benchmarks");
		}
	}

	/**
	 * Uses the Validate util to ensure the incoming type upload request is valid. This checks for illegal characters
	 * and content length requirements.
	 *
	 * @param request the HTTP request to validate.
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValidProcessRequest(HttpServletRequest request) {
		try {
			if (!Validator.isValidPosInteger((String) request.getParameter(PROCESSOR_ID))) {
				return new ValidatorStatusCode(false, "The processor ID needs to be a valid integer");
			}
			int userId = SessionUtil.getUserId(request);

			if (!Validator.isValidPosInteger((String) request.getParameter(SPACE_ID))) {
				return new ValidatorStatusCode(false, "The space ID needs to be a valid integer");
			}

			if (!Validator.isValidBool((String) request.getParameter(CLEAR_OLD))) {
				return new ValidatorStatusCode(false, "The 'clear old' option needs to be a valid boolean");
			}

			if (!Validator.isValidBool((String) request.getParameter(SPACE_HIERARCHY))) {
				return new ValidatorStatusCode(false, "The 'space hierarchy' option needs to be a valid boolean");
			}


			int spaceId = Integer.parseInt(request.getParameter(SPACE_ID));
			int pid = Integer.parseInt(request.getParameter(PROCESSOR_ID));
			Processor p = Processors.get(pid);

			if (p == null) {
				return new ValidatorStatusCode(
						false, "Could not find the processor referenced by the processor id = " + pid);
			}

			if (!Users.isMemberOfCommunity(userId, p.getCommunityId()) &&
					!GeneralSecurity.hasAdminReadPrivileges(userId)) {
				return new ValidatorStatusCode(false, "You must be a member of the community that owns the processor");
			}
			if (!Users.isMemberOfSpace(userId, spaceId) && !GeneralSecurity.hasAdminWritePrivileges(userId)) {
				return new ValidatorStatusCode(false, "You must be a member of the space you are trying to process");
			}
			int commId = Spaces.getCommunityOfSpace(spaceId);
			if (commId != p.getCommunityId()) {
				return new ValidatorStatusCode(
						false, "You may only use processors that are a part of the community that owns the " +
						"benchmarks");
			}
			// Passed all checks, return true
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error processing request");
	}
}
