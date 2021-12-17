package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.SettingSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Servlet which handles requests to create new DefaultSettings profiles for users
 *
 * @author Eric Burns
 */
public class AddSettingProfile extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(AddSettingProfile.class);

	// Param strings for processing
	private static final String POST_PROCESSOR = "postp";
	private static final String PRE_PROCESSOR = "prep";
	private static final String BENCH_PROCESSOR = "benchp";
	private static final String NAME = "name";
	private static final String CPU_TIMEOUT = "cpu";
	private static final String WALLCLOCK_TIMEOUT = "wall";
	private static final String DEPENDENCIES = "dep";
	private static final String MAX_MEMORY = "mem";
	private static final String SETTING_ID = "settingId"; //this is set if we are doing an update only
	private static final String USER_ID_OF_OWNER = "userIdOfOwner";
	private static final String BENCHMARKING_FRAMEWORK = "benchmarkingFramework";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * Post requests should have all the attributes required for a DefaultSettings object
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final String method = "doPost";

			log.debug("got a request to create a new settings profile");

			try {
				log.debug("Validating add setting profile request.");
				ValidatorStatusCode status = isValidRequest(request);
				if (!status.isSuccess()) { //if the request is malformed
					log.debug(status.getMessage());
					response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, status.getMessage());
					return;
				}
				log.debug("Validated add setting profile request.");
			} catch (SQLException e) {
				log.warn(method, "Caught SQLException, returning internal error.", e);
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Database error while trying to check permissions."
				);
				return;
			}

			DefaultSettings d = new DefaultSettings();

			//this servlet currently only handles requests for users. Community profiles are created automatically
			d.setType(SettingType.USER);

			int userIdOfCaller = SessionUtil.getUserId(request);
			int userIdOfOwner = -1;
			String rawUserIdOfOwner = request.getParameter(USER_ID_OF_OWNER);
			log.debug(method + ": userIdOfOwner=" + rawUserIdOfOwner);

			if (Validator.isValidPosInteger(rawUserIdOfOwner)) {
				log.debug("rawUserIdOfOwner was a valid integer.");
				userIdOfOwner = Integer.parseInt(rawUserIdOfOwner);
				d.setPrimId(userIdOfOwner);
			} else {
				log.debug("rawUserIdOfOwner was not a valid integer.");
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
				return;
			}

			if (!SettingSecurity.canUserAddOrSeeProfile(userIdOfOwner, userIdOfCaller)) {
				log.debug("User cannot add or see profile.");
				response.sendError(
						HttpServletResponse.SC_FORBIDDEN,
						"You do not have permission to add a setting profile for this user."
				);
			}

			//all profiles must set the following attributes
			d.setWallclockTimeout(Integer.parseInt(request.getParameter(WALLCLOCK_TIMEOUT)));
			d.setCpuTimeout(Integer.parseInt(request.getParameter(CPU_TIMEOUT)));
			d.setMaxMemory(Util.gigabytesToBytes(Double.parseDouble(request.getParameter(MAX_MEMORY))));
			d.setDependenciesEnabled(Boolean.parseBoolean(request.getParameter(DEPENDENCIES)));
			d.setBenchmarkingFramework(BenchmarkingFramework.valueOf(request.getParameter(BENCHMARKING_FRAMEWORK)));

			//the next attributes do not necessarily need to be set, as they can be null
			String postId = request.getParameter(POST_PROCESSOR);
			String solver = request.getParameter(R.SOLVER);
			String preId = request.getParameter(PRE_PROCESSOR);
			String benchProcId = request.getParameter(BENCH_PROCESSOR);
			log.debug("Getting benchIds from request.");

			List<String> benchIds = getBenchIds(request);

			log.debug("Casting parameters to integers.");
			//it is only set it if is an integer>0, as all real IDs are greater than 0. Same for all subsequent objects
			if (Validator.isValidPosInteger(postId)) {
				int p = Integer.parseInt(postId);
				if (p > 0) {
					d.setPostProcessorId(p);
				}
			}
			if (Validator.isValidPosInteger(preId)) {
				int p = Integer.parseInt(preId);
				if (p > 0) {
					d.setPreProcessorId(p);
				}
			}
			if (Validator.isValidPosInteger(benchProcId)) {
				int p = Integer.parseInt(benchProcId);
				if (p > 0) {
					d.setBenchProcessorId(p);
				}
			}
			log.debug("got sent the solver " + solver);
			if (Validator.isValidPosInteger(solver)) {
				int p = Integer.parseInt(solver);
				if (p > 0) {
					log.debug("setting the solver");
					d.setSolverId(p);
				}
			}
			for (String benchId : benchIds) {
				log.debug("Got the benchId: " + benchId);
				if (Validator.isValidPosInteger(benchId)) {
					int p = Integer.parseInt(benchId);
					if (p > 0) {
						log.debug("setting the benchmark id = " + p);
						d.addBenchId(p);
					}
				}
			}

			boolean success = true;
			//if we are doing an update
			if (Util.paramExists(SETTING_ID, request)) {
				d.setId(Integer.parseInt(request.getParameter(SETTING_ID)));
				success = Settings.updateDefaultSettings(d);
			} else {
				d.setName(request.getParameter(NAME));
				//otherwise, we are creating a new profile
				success = (Users.createNewDefaultSettings(d) > 0);
			}
			log.debug(method + ": Creating a new default settings was" + (success ? " " : " not ") + "successful.");
			if (!success) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			log.warn("Caught Exception in AddSettingProfile.doPost.", e);
			throw e;
		}
	}

	private List<String> getBenchIds(HttpServletRequest request) {
		String[] rawBenchIds = request.getParameterValues(R.BENCHMARK + "[]");
		List<String> benchIds = null;
		if (rawBenchIds == null) {
			benchIds = new ArrayList<>();
		} else {
			benchIds = new ArrayList<>(Arrays.asList(rawBenchIds));
			for (String benchId : benchIds) {
				log.debug("Got benchId: " + benchId);
			}
		}
		return benchIds;
	}

	private ValidatorStatusCode isValidRequest(HttpServletRequest request) throws SQLException {
		final String methodName = "isValidRequest";
		int userId = SessionUtil.getUserId(request);
		if (Users.isPublicUser(userId)) {
			return new ValidatorStatusCode(false, "Only registered users can take this action");
		}

		if (!Validator.isValidBool(request.getParameter(DEPENDENCIES))) {
			return new ValidatorStatusCode(false, "invalid dependency selection");
		}
		if (!Validator.isValidPosInteger(request.getParameter(CPU_TIMEOUT))) {
			return new ValidatorStatusCode(false, "invalid cpu timeout");
		}
		if (!Validator.isValidPosInteger(request.getParameter(WALLCLOCK_TIMEOUT))) {
			return new ValidatorStatusCode(false, "invalid wallclock timeout");
		}

		if (!Validator.isValidPosDouble(request.getParameter(MAX_MEMORY))) {
			return new ValidatorStatusCode(false, "invalid maximum memory");
		}

		// Check if the benchmarking framework parameter doesn't match any available benchmarking frameworks.
		final String benchmarkingFramework = request.getParameter(BENCHMARKING_FRAMEWORK);
		log.debug(methodName, "Benchmarking framework was: " + benchmarkingFramework);
		if (EnumSet.allOf(BenchmarkingFramework.class).stream()
		           .noneMatch(framework -> framework.toString().equals(benchmarkingFramework))) {
			return new ValidatorStatusCode(false, "invalid benchmarking framework: " + benchmarkingFramework);
		}

		String postId = request.getParameter(POST_PROCESSOR);
		String solver = request.getParameter(R.SOLVER);
		log.debug("got sent the solver " + solver);
		String preId = request.getParameter(PRE_PROCESSOR);
		String benchProcId = request.getParameter(BENCH_PROCESSOR);
		List<String> benchIds = getBenchIds(request);


		ValidatorStatusCode statusCode = checkIfUserCanSeeProcessor(postId, userId);
		if (!statusCode.isSuccess()) {
			return statusCode;
		}
		statusCode = checkIfUserCanSeeProcessor(preId, userId);
		if (!statusCode.isSuccess()) {
			return statusCode;
		}
		statusCode = checkIfUserCanSeeProcessor(benchProcId, userId);
		if (!statusCode.isSuccess()) {
			return statusCode;
		}

		if (Validator.isValidPosInteger(solver)) {
			int s = Integer.parseInt(solver);
			if (s > 0) {
				//if we actually did select a solver
				if (!Permissions.canUserSeeSolver(s, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use the given solver");
				}
			}
		}
		for (String benchId : benchIds) {
			if (Validator.isValidPosInteger(benchId)) {
				int b = Integer.parseInt(benchId);
				if (b > 0) {
					if (!Permissions.canUserSeeBench(b, userId)) {
						return new ValidatorStatusCode(false, "You do not have permission to use the given benchmark");
					}
				}
			}
		}
		//if a setting ID exists, this is an update. Otherwise, it is a new profile
		if (Util.paramExists(SETTING_ID, request)) {
			if (!Validator.isValidPosInteger(request.getParameter(SETTING_ID))) {
				return new ValidatorStatusCode(false, "The given setting ID is not a valid integer");
			}
			int settingId = Integer.parseInt(request.getParameter(SETTING_ID));
			ValidatorStatusCode status = SettingSecurity.canModifySettings(settingId, userId);
			if (!status.isSuccess()) {
				return status;
			}
		} else {
			if (!Validator.isValidSettingsName(request.getParameter(NAME))) {
				return new ValidatorStatusCode(false, "Invalid name");
			}
		}


		return new ValidatorStatusCode(true);
	}

	private static ValidatorStatusCode checkIfUserCanSeeProcessor(String param, int userId) {
		//-1 is not an error-- it indicates that nothing was selected for all the following cases
		if (Validator.isValidPosInteger(param)) {
			int p = Integer.parseInt(param);

			ValidatorStatusCode status = ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p > 0) {
				return status;
			}
		}
		return new ValidatorStatusCode(true);
	}
}
