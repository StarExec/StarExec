package org.starexec.servlets;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Users;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Processor;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles requests for registration 
 * @author Todd Elvers & Tyler Jensen
 */
@SuppressWarnings("serial")
public class AddSettingProfile extends HttpServlet {
	private static final Logger log = Logger.getLogger(AddSettingProfile.class);	

	// Param strings for processing
	public static String POST_PROCESSOR = "postp";
	public static String PRE_PROCESSOR ="prep";
	public static String BENCH_PROCESSOR ="benchp";
	public static String SOLVER="solver";
	public static String NAME="name";
	public static String CPU_TIMEOUT="cpu";
	public static String WALLCLOCK_TIMEOUT="wall";
	public static String DEPENDENCIES="dep";
	public static String BENCH_ID="bench";
	public static String MAX_MEMORY="mem";
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		
		log.debug("got a request to create a new settings profile");
		
		ValidatorStatusCode status=isValidRequest(request);
		if (!status.isSuccess()) {
			log.debug(status.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,status.getMessage());
			return;
		}
		
		DefaultSettings d=new DefaultSettings();
		d.setId(SessionUtil.getUserId(request));
		d.setName(request.getParameter(NAME));
		
		d.setWallclockTimeout(Integer.parseInt(request.getParameter(WALLCLOCK_TIMEOUT)));
		d.setCpuTimeout(Integer.parseInt(request.getParameter(CPU_TIMEOUT)));
		d.setMaxMemory(Util.gigabytesToBytes(Double.parseDouble(request.getParameter(MAX_MEMORY))));
		log.debug("wallclock is "+request.getParameter(WALLCLOCK_TIMEOUT));
		
		
		String postId=request.getParameter(POST_PROCESSOR);
		String solver=request.getParameter(SOLVER);
		String preId=request.getParameter(PRE_PROCESSOR);
		String benchProcId=request.getParameter(BENCH_PROCESSOR);
		String benchId=request.getParameter(BENCH_ID);
		if (Validator.isValidInteger(postId)) {
			int p=Integer.parseInt(postId);
			if (p>0) {
				d.setPostProcessorId(p);
			}
		}
		if (Validator.isValidInteger(preId)) {
			int p=Integer.parseInt(preId);
			if (p>0) {
				d.setPreProcessorId(p);
			}
		}
		if (Validator.isValidInteger(benchProcId)) {
			int p=Integer.parseInt(benchProcId);
			if (p>0) {
				d.setBenchProcessorId(p);
			}
		}
		if (Validator.isValidInteger(solver)) {
			int p=Integer.parseInt(solver);
			if (p>0) {
				d.setSolverId(p);
			}
		}
		if (Validator.isValidInteger(benchId)) {
			int p=Integer.parseInt(benchId);
			if (p>0) {
				log.debug("setting the benchmark id = "+p);
				d.setBenchId(p);
			}
		}
		
		Users.createNewDefaultSettings(d);
	}
	
	private ValidatorStatusCode isValidRequest(HttpServletRequest request) {
		int userId=SessionUtil.getUserId(request);
		if (Users.isPublicUser(userId)) {
			return new ValidatorStatusCode(false, "Only registered users can take this action");
		}
		if (!Validator.isValidSolverName(request.getParameter(NAME))) {
			return new ValidatorStatusCode(false, "Invalid name");
		}
		
		if (Settings.getUserProfileByIdAndName(userId, request.getParameter(NAME))!=null) {
			return new ValidatorStatusCode(false, "The given name is already in use");
		}
		
		if (!Validator.isValidBool(request.getParameter(DEPENDENCIES))) {
			return new ValidatorStatusCode(false, "invalid dependency selection");
		}
		if (!Validator.isValidTimeout(request.getParameter(CPU_TIMEOUT))) {
			return new ValidatorStatusCode(false, "invalid cpu timeout");
		}
		if (!Validator.isValidTimeout(request.getParameter(WALLCLOCK_TIMEOUT))) {
			return new ValidatorStatusCode(false, "invalid wallclock timeout");
		}
		
		if (!Validator.isValidDouble(request.getParameter(MAX_MEMORY))) {
			return new ValidatorStatusCode(false, "invalid maximum memory");
		}
		
		String postId=request.getParameter(POST_PROCESSOR);
		String solver=request.getParameter(SOLVER);
		String preId=request.getParameter(PRE_PROCESSOR);
		String benchProcId=request.getParameter(BENCH_PROCESSOR);
		String benchId=request.getParameter(BENCH_PROCESSOR);
		
		//-1 is not an error-- it indicates that nothing was selected for all the following cases
		if (Validator.isValidInteger(postId)) {
			int p=Integer.parseInt(postId);

			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p>0) {
				return status;
			}
		}
		if (Validator.isValidInteger(preId)) {
			int p=Integer.parseInt(preId);

			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p>0) {
				return status;
			}
		}
		if (Validator.isValidInteger(benchProcId)) {
			int p=Integer.parseInt(benchId);
			ValidatorStatusCode status=ProcessorSecurity.canUserSeeProcessor(p, userId);
			if (!status.isSuccess() && p>0) {
				return status;
			}
		}
		if (Validator.isValidInteger(solver)) {
		}
		if (Validator.isValidInteger(benchId)) {
		}
		
		
		return new ValidatorStatusCode(true);
	}
	        
}
