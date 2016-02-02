package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.SettingSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles requests to create new DefaultSettings profiles for users
 * @author Eric Burns
 */
@SuppressWarnings("serial")
public class AddSettingProfile extends HttpServlet {
	private static final Logger log = Logger.getLogger(AddSettingProfile.class);	

	// Param strings for processing
	private static String POST_PROCESSOR = "postp";
	private static String PRE_PROCESSOR ="prep";
	private static String BENCH_PROCESSOR ="benchp";
	private static String NAME="name";
	private static String CPU_TIMEOUT="cpu";
	private static String WALLCLOCK_TIMEOUT="wall";
	private static String DEPENDENCIES="dep";
	private static String MAX_MEMORY="mem";
	private static String SETTING_ID= "settingId"; //this is set if we are doing an update only
	private static String USER_ID_OF_OWNER = "userIdOfOwner";
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	
	/**
	 * Post requests should have all the attributes required for a DefaultSettings object
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		final String method = "doPost";
		
		log.debug("got a request to create a new settings profile");
		
		ValidatorStatusCode status=isValidRequest(request);
		if (!status.isSuccess()) { //if the request is malformed
			log.debug(status.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,status.getMessage());
			return;
		}

		DefaultSettings d=new DefaultSettings();
		
		//this servlet currently only handles requests for users. Community profiles are created automatically
		d.setType(SettingType.USER);

		int userIdOfCaller=SessionUtil.getUserId(request);
		int userIdOfOwner = -1;
		String rawUserIdOfOwner=request.getParameter(USER_ID_OF_OWNER);
		log.debug(method+": userIdOfOwner="+rawUserIdOfOwner);

		if (Validator.isValidPosInteger(rawUserIdOfOwner)) {
			userIdOfOwner = Integer.parseInt(rawUserIdOfOwner);
			d.setPrimId(userIdOfOwner);
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
			return;
		}

		if (!SettingSecurity.canUserAddOrSeeProfile(userIdOfOwner, userIdOfCaller)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add a setting profile for this user.");
		}


		
		//all profiles must set the following attributes
		d.setWallclockTimeout(Integer.parseInt(request.getParameter(WALLCLOCK_TIMEOUT)));
		d.setCpuTimeout(Integer.parseInt(request.getParameter(CPU_TIMEOUT)));
		d.setMaxMemory(Util.gigabytesToBytes(Double.parseDouble(request.getParameter(MAX_MEMORY))));
		d.setDependenciesEnabled(Boolean.parseBoolean(request.getParameter(DEPENDENCIES)));
		
		//the next attributes do not necessarily need to be set, as they can be null
		String postId=request.getParameter(POST_PROCESSOR);
		String solver=request.getParameter(R.SOLVER);
		String preId=request.getParameter(PRE_PROCESSOR);
		String benchProcId=request.getParameter(BENCH_PROCESSOR);
		String benchId=request.getParameter(R.BENCHMARK);

		
		//it is only set it if is an integer>0, as all real IDs are greater than 0. Same for all subsequent objects
		if (Validator.isValidPosInteger(postId)) {
			int p=Integer.parseInt(postId);
			if (p>0) {
				d.setPostProcessorId(p);
			}
		}
		if (Validator.isValidPosInteger(preId)) {
			int p=Integer.parseInt(preId);
			if (p>0) {
				d.setPreProcessorId(p);
			}
		}
		if (Validator.isValidPosInteger(benchProcId)) {
			int p=Integer.parseInt(benchProcId);
			if (p>0) {
				d.setBenchProcessorId(p);
			}
		}
		log.debug("got sent the solver "+solver);
		if (Validator.isValidPosInteger(solver)) {
			int p=Integer.parseInt(solver);
			if (p>0) {
				log.debug("setting the solver");
				d.setSolverId(p);
			}
		}
		if (Validator.isValidPosInteger(benchId)) {
			int p=Integer.parseInt(benchId);
			if (p>0) {
				log.debug("setting the benchmark id = "+p);
				d.setBenchId(p);
			}
		}
		boolean success=true;
		//if we are doing an update
		if (Util.paramExists(SETTING_ID,request)) {
			d.setId(Integer.parseInt(request.getParameter(SETTING_ID)));
			success=Settings.updateDefaultSettings(d);
		} else {
			d.setName(request.getParameter(NAME));
			//otherwise, we are creating a new profile
			success=(Users.createNewDefaultSettings(d)>0);

		}
		log.debug(method+": Creating a new default settings was"+(success?" ":" not ")+"successful.");
		if (!success) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		
	}
	
	private ValidatorStatusCode isValidRequest(HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
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
		
		String postId=request.getParameter(POST_PROCESSOR);
		String solver=request.getParameter(R.SOLVER);
		log.debug("got sent the solver "+solver);
		String preId=request.getParameter(PRE_PROCESSOR);
		String benchProcId=request.getParameter(BENCH_PROCESSOR);
		String benchId=request.getParameter(BENCH_PROCESSOR);
		
		//-1 is not an error-- it indicates that nothing was selected for all the following cases
		if (Validator.isValidPosInteger(postId)) {
			int p=Integer.parseInt(postId);

			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p>0) {
				return status;
			}
		}
		if (Validator.isValidPosInteger(preId)) {
			int p=Integer.parseInt(preId);

			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p>0) {
				return status;
			}
		}
		if (Validator.isValidPosInteger(benchProcId)) {
			int p=Integer.parseInt(benchProcId);
			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p>0) {
				return status;
			}
		}
		if (Validator.isValidPosInteger(solver)) {
			int s=Integer.parseInt(solver);
			if (s>0) {
				//if we actually did select a solver
				if (!Permissions.canUserSeeSolver(s, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use the given solver");
				}
			}
		}
		if (Validator.isValidPosInteger(benchId)) {
			int b=Integer.parseInt(benchId);
			if (b>0) {
				if (!Permissions.canUserSeeBench(b, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use the given benchmark");
				}
			}
		}
		
		//if a setting ID exists, this is an update. Otherwise, it is a new profile
		if (Util.paramExists(SETTING_ID, request)) {
			if (!Validator.isValidPosInteger(request.getParameter(SETTING_ID))) {
				return new ValidatorStatusCode(false, "The given setting ID is not a valid integer");
			}
			int settingId=Integer.parseInt(request.getParameter(SETTING_ID));
			ValidatorStatusCode status=SettingSecurity.canModifySettings(settingId, userId);
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
	        
}
