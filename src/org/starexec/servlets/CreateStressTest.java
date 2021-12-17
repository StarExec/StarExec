package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.logger.StarLogger;
import org.starexec.test.integration.TestManager;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet which handles incoming requests adding new spaces
 *
 * @author Tyler Jensen
 */
public class CreateStressTest extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(CreateStressTest.class);

	// Request attributes
	private static final String USER_COUNT = "userCount";
	private static final String SPACE_COUNT = "spaceCount";
	private static final String MIN_SOLVERS_PER_SPACE = "minSolversPer";
	private static final String MAX_SOLVERS_PER_SPACE = "maxSolversPer";

	private static final String MIN_BENCHMARKS_PER_SPACE = "minBenchmarksPer";
	private static final String MAX_BENCHMARKS_PER_SPACE = "maxBenchmarksPer";

	private static final String MIN_USERS_PER_SPACE = "minUsersPer";
	private static final String MAX_USERS_PER_SPACE = "maxUsersPer";

	private static final String JOB_COUNT = "jobCount";
	private static final String SPACES_PER_JOB = "spacesPerJob";

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			// Make sure the request is valid
			ValidatorStatusCode status = isValid(request);
			if (!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
				return;
			}

			boolean success = TestManager.executeStressTest(Integer.parseInt(request.getParameter(USER_COUNT)),
			                                                Integer.parseInt(request.getParameter(SPACE_COUNT)),
			                                                Integer.parseInt(request.getParameter(JOB_COUNT)),
			                                                Integer.parseInt(request.getParameter
					                                                (MIN_USERS_PER_SPACE)), Integer.parseInt(request.getParameter
					                                                (MAX_USERS_PER_SPACE)),
			                                                Integer.parseInt(
					                                                request.getParameter(MIN_SOLVERS_PER_SPACE)),
			                                                Integer.parseInt(
					                                                request.getParameter(MAX_SOLVERS_PER_SPACE)),
			                                                Integer.parseInt(
					                                                request.getParameter(MIN_BENCHMARKS_PER_SPACE)),
			                                                Integer.parseInt(
					                                                request.getParameter(MAX_BENCHMARKS_PER_SPACE)),
			                                                Integer.parseInt(request.getParameter(SPACES_PER_JOB))
			);
			if (!success) {
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"There was an internal error when starting the stress test"
				);
			} else {
				response.sendRedirect((Util.docRoot("secure/admin/testing.jsp")));
			}
		} catch (Exception e) {
			log.warn("Caught Exception in CreateStressTest.doPost.", e);
			throw e;
		}
	}

	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters and content
	 * length requirements to ensure it is not malicious.
	 *
	 * @param request The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValid(HttpServletRequest request) {
		try {
			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(USER_COUNT))) {
				return new ValidatorStatusCode(false, "The user count needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(SPACE_COUNT))) {
				return new ValidatorStatusCode(false, "The space count needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(JOB_COUNT))) {
				return new ValidatorStatusCode(false, "The job count needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(MIN_SOLVERS_PER_SPACE))) {
				return new ValidatorStatusCode(false, "The solvers per space needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(MAX_SOLVERS_PER_SPACE))) {
				return new ValidatorStatusCode(false, "The solvers per space needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(MIN_BENCHMARKS_PER_SPACE))) {
				return new ValidatorStatusCode(false, "The benchmarks per space needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(MAX_BENCHMARKS_PER_SPACE))) {
				return new ValidatorStatusCode(false, "The benchmarks per space needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(MIN_USERS_PER_SPACE))) {
				return new ValidatorStatusCode(false, "The users per space needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(MAX_USERS_PER_SPACE))) {
				return new ValidatorStatusCode(false, "The users per space needs to be an integer");
			}

			// Make sure the parent space id is a int
			if (!Validator.isValidInteger(request.getParameter(SPACES_PER_JOB))) {
				return new ValidatorStatusCode(false, "The spaces per job needs to be an integer");
			}

			int spaceCount = Integer.parseInt(request.getParameter(SPACE_COUNT));
			if (spaceCount < 0) {
				return new ValidatorStatusCode(false, "The number of spaces needs to be greater than or equal to 0");
			}

			int jobCount = Integer.parseInt(request.getParameter(JOB_COUNT));
			if (jobCount < 0) {
				return new ValidatorStatusCode(false, "The number of jobs needs to be greater than or equal to 0");
			}

			int userCount = Integer.parseInt(request.getParameter(USER_COUNT));
			if (userCount < 0) {
				return new ValidatorStatusCode(false, "The number of users needs to be greater than or equal to 0");
			}
			int spacesPerJob = Integer.parseInt(request.getParameter(SPACES_PER_JOB));
			if (spacesPerJob < 1 && jobCount > 0) {
				return new ValidatorStatusCode(false, "Jobs need to have at least one space");
			}

			int minUsersPerSpace = Integer.parseInt(request.getParameter(MIN_USERS_PER_SPACE));
			int maxUsersPerSpace = Integer.parseInt(request.getParameter(MAX_USERS_PER_SPACE));

			if (minUsersPerSpace < 1) {
				return new ValidatorStatusCode(
						false, "The number of users per space must be greater than or equal to 1");
			} else if (maxUsersPerSpace < minUsersPerSpace) {
				return new ValidatorStatusCode(
						false, "The max users per space must be greater than or equal to the min users per space");
			} else if (maxUsersPerSpace > userCount) {
				return new ValidatorStatusCode(false, "You can not have more users per space than total users " +
						"created");
			}

			int minSolversPerSpace = Integer.parseInt(request.getParameter(MIN_SOLVERS_PER_SPACE));
			int maxSolversPerSpace = Integer.parseInt(request.getParameter(MAX_SOLVERS_PER_SPACE));

			if (minSolversPerSpace < 0) {
				return new ValidatorStatusCode(
						false, "The number of solves per space must be greater than or equal to 0");
			} else if (maxSolversPerSpace < minSolversPerSpace) {
				return new ValidatorStatusCode(
						false, "The max solvers per space must be greater than or equal to the min users per space");
			}


			int minBenchmarksPerSpace = Integer.parseInt(request.getParameter(MIN_BENCHMARKS_PER_SPACE));
			int maxBenchmarksPerSpace = Integer.parseInt(request.getParameter(MAX_BENCHMARKS_PER_SPACE));

			if (minBenchmarksPerSpace < 0) {
				return new ValidatorStatusCode(
						false, "The number of benchmarks per space must be greater than or equal to 0");
			} else if (maxBenchmarksPerSpace < minBenchmarksPerSpace) {
				return new ValidatorStatusCode(
						false, "The max benchmarks per space must be greater than or equal to the min users per space");
			}


			// Validated inputs-- next make sure user has permission
			return GeneralSecurity.canUserRunTests(SessionUtil.getUserId(request), true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error processing request");
	}
}
