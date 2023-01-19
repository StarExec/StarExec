package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Servlet which handles incoming requests adding new spaces
 *
 * @author Tyler Jensen
 */
@SuppressWarnings("JavadocReference")
public class AddSpace extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(AddSpace.class);

	// Request attributes
	private static final String parentSpace = "parent";
	private static final String name = "name";
	private static final String description = "desc";
	private static final String locked = "locked";
	private static final String users = "users";
	private static final String solvers = "solvers";
	private static final String benchmarks = "benchmarks";
	private static final String addSolver = "addSolver";
	private static final String addBench = "addBench";
	private static final String addUser = "addUser";
	private static final String addSpace = "addSpace";
	private static final String addJob = "addJob";
	private static final String removeSolver = "removeSolver";
	private static final String removeBench = "removeBench";
	private static final String removeUser = "removeUser";
	private static final String removeSpace = "removeSpace";
	private static final String removeJob = "removeJob";
	private static final String stickyLeaders = "sticky";

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

			int spaceId = Integer.parseInt((String) request.getParameter(parentSpace));
			int userId = SessionUtil.getUserId(request);

			// Make the space to be added and set it's basic information
			Space s = new Space();
			s.setName((String) request.getParameter(name));
			s.setDescription((String) request.getParameter(description));
			s.setLocked(Boolean.parseBoolean((String) request.getParameter(locked)));
			s.setStickyLeaders(Boolean.parseBoolean((String) request.getParameter(stickyLeaders)));
			int spaceId1 = Integer.parseInt(request.getParameter(parentSpace));
			s.setParentSpace(spaceId1);

			// Make the default permissions for the space to be added
			Permission p = new Permission();
			p.setAddBenchmark(request.getParameter(addBench) != null);
			p.setAddSolver(request.getParameter(addSolver) != null);
			p.setAddSpace(request.getParameter(addSpace) != null);
			p.setAddUser(request.getParameter(addUser) != null);
			p.setAddJob(request.getParameter(addJob) != null);

			p.setRemoveBench(request.getParameter(removeBench) != null);
			p.setRemoveSolver(request.getParameter(removeSolver) != null);
			p.setRemoveSpace(request.getParameter(removeSpace) != null);
			p.setRemoveUser(request.getParameter(removeUser) != null);
			p.setRemoveJob(request.getParameter(removeJob) != null);
			p.setLeader(false);
			// Set the default permission on the space
			s.setPermission(p);


			s.setParentSpace(spaceId);
			int newSpaceId = Spaces.add(s, userId);

			//Inherit Users
			boolean inheritUsers = Boolean.parseBoolean((String) request.getParameter(users));
			log.debug("inheritUsers = " + inheritUsers);
			if (inheritUsers) {
				log.debug("Adding inherited users");
				List<User> users = Spaces.getUsers(spaceId);
				log.debug("parent users = " + users);
				for (User u : users) {
					log.debug("users = " + u.getFirstName());
					int tempId = u.getId();
					Users.associate(tempId, newSpaceId);
				}
			}

			boolean inheritSolvers = Boolean.parseBoolean((String) request.getParameter(solvers));
			log.debug("inheritSolvers = " + inheritSolvers);
			if (inheritSolvers) {
				log.debug("Adding inherited solvers");
				List<Solver> solvers = Solvers.getBySpace(spaceId);
				log.debug("parent solvers = " + solvers);
				log.debug("parent solvers size = " + solvers.size());
				for (Solver solver : solvers) {
					log.debug("solvers = " + solver.getName());
					Solvers.associate(solver.getId(), newSpaceId);
				}
			}

			boolean inheritBenchmarks = Boolean.parseBoolean((String) request.getParameter(benchmarks));
			log.debug("inheritBenchmarks = " + inheritBenchmarks);
			if (inheritBenchmarks) {
				log.debug("Adding inherited benchmarks");
				List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);
				log.debug("parent benchmarks = " + benchmarks);
				log.debug("parent benchmarks size = " + benchmarks.size());
				for (Benchmark benchmark : benchmarks) {
					log.debug("benchmarks = " + benchmark.getName());
					Benchmarks.associate(benchmark.getId(), newSpaceId);
				}
			}


			//adds sticky users from ancestor spaces
			log.debug("adding leaders from parent spaces");
			Set<Integer> users = Spaces.getStickyLeaders(newSpaceId);
			Permission perm = Permissions.getFullPermission();
			for (Integer id : users) {
				Users.associate(id, newSpaceId);
				Permissions.set(id, newSpaceId, perm);
			}

			try {
				if (Communities.isCommunity(newSpaceId)) {
					Communities.createUsersSpace(newSpaceId);
				}
			} catch (Exception e) {
				log.error("doPost", "Error creating Users subspace", e);
			}

			if (newSpaceId <= 0) {
				// If it failed, notify an error
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"There was an internal error adding the space to the starexec database"
				);
			} else {
				// On success, redirect to the space explorer so they can see changes
				response.addCookie(new Cookie("New_ID", String.valueOf(newSpaceId)));
				response.sendRedirect(Util.docRoot("secure/explore/spaces.jsp"));
			}
		} catch (Exception e) {
			log.warn("Caught Exception in AddSpace.doPost", e);
			throw e;
		}
	}

	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters and content
	 * length requirements to ensure it is not malicious.
	 *
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValid(HttpServletRequest request) {
		try {
			// Make sure the parent space id is a int
			if (!Validator.isValidPosInteger(request.getParameter(parentSpace))) {
				return new ValidatorStatusCode(false, "The space ID needs to be an integer");
			}
			int spaceId = Integer.parseInt(request.getParameter(parentSpace));

			// Ensure the space name is valid (alphanumeric < SPACE_NAME_LEN chars)
			if (!Validator.isValidSpaceName(request.getParameter(name))) {
				return new ValidatorStatusCode(
						false, "The given name is invalid-- please reference the help pages to see valid space names");
			}
			String n = request.getParameter(name);
			// Ensure the description is < 1024 characters
			if (!Validator.isValidPrimDescription(request.getParameter(description))) {
				return new ValidatorStatusCode(
						false,
						"The given description is invalid-- please reference the help pages to see valid description " +
								"names"
				);
			}

			// Ensure the isLocked value is a parseable boolean
			String lockVal = (String) request.getParameter(locked);
			if (!lockVal.equals("true") && !lockVal.equals("false")) {
				return new ValidatorStatusCode(false, "The 'locked' attribute needs to be either true or false");
			}
			//sticky should also be a parsable boolean
			String sticky = (String) request.getParameter(stickyLeaders);
			if (sticky != null) {
				if (!sticky.equals("true") && !sticky.equals("false")) {
					return new ValidatorStatusCode(
							false, "The 'sticky leaders' attribute needs to be either true or false");
				}


				//subspaces of the root can not have sticky leaders enabled
				if (spaceId == 1 && sticky.equals("true")) {
					return new ValidatorStatusCode(false, "Communities may not enable the sticky leaders option");
				}
			}

			// Verify this user can add spaces to this space
			Permission p = SessionUtil.getPermission(request, spaceId);
			if (p == null || !p.canAddSpace()) {
				return new ValidatorStatusCode(false, "You do not have permission to add a new space here");
			}


			if (Spaces.getSubSpaceIDbyName(spaceId, n) != -1) {
				return new ValidatorStatusCode(false,
				                               "The subspace should have a unique name in the space. It is possible a " +
						                               "private subspace you are not authorized to see has the same " +
						                               "name.");
			}

			// Passed all checks, return true
			return new ValidatorStatusCode(true);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error processing request");
	}
}
