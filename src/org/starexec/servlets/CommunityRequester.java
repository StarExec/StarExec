package org.starexec.servlets;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Users;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * @author Todd Elvers
 */
@SuppressWarnings("serial")
public class CommunityRequester extends HttpServlet {
	private static final Logger log = Logger.getLogger(CommunityRequest.class);	

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	//TODO: Set up error messages

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		
		User user = SessionUtil.getUser(request);			
		
		// Validate parameters of request & construct Invite object
		CommunityRequest comRequest = constructComRequest(user, request);
		if(comRequest == null){
		    response.sendRedirect(Util.docRoot("secure/add/to_community.jsp?result=requestNotSent&cid=-1"));
			return;
		}
		if (Users.isPublicUser(user.getId())){
			return;
		}
		boolean added = Requests.addCommunityRequest(user, comRequest.getCommunityId(), comRequest.getCode(), comRequest.getMessage());
		if(added){
			// Send the invite to the leaders of the community 
			Mail.sendCommunityRequest(user, comRequest);
			response.sendRedirect(Util.docRoot("secure/add/to_community.jsp?result=requestSent&cid=" + comRequest.getCommunityId()));
		} else {
			// There was a problem
		    response.sendRedirect(Util.docRoot("secure/add/to_community.jsp?result=requestNotSent&cid=" + comRequest.getCommunityId()));
		}
	}
	
	
	/**
	 * Builds an Invite object given a user and request
	 * 
	 * @param user the user to create the invite for
	 * @param request the servlet containing the invite information
	 * @return the invite constructed
	 */
	private CommunityRequest constructComRequest(User user, HttpServletRequest request){
		try {
			if(!Util.paramExists(Registration.USER_MESSAGE, request) ||
			   !Util.paramExists(Registration.USER_COMMUNITY, request)) {
				return null;
			}
			
			String message = request.getParameter(Registration.USER_MESSAGE);
			int communityId = Integer.parseInt(request.getParameter(Registration.USER_COMMUNITY)); 		
			
			if(validateParameters(communityId, message)){
				CommunityRequest req = new CommunityRequest();
				req.setUserId(user.getId());
				req.setCommunityId(communityId);
				req.setCode(UUID.randomUUID().toString());
				req.setMessage(message);
				return req;	
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * Validates the parameters that will be used to construct the Invite object by
	 * checking that the communityId is valid and that message is between 1 and 300 characters.
	 * 
	 * @param communityId the id of the space the forthcoming Invite object will have
	 * @param message the message written by the user for the leaders of the community
	 * they're trying to join
	 * @return true iff communityId is a valid space_id that is a child of the root space and 
	 * that the user's message is between 1 and 300 characters in length
	 */
	private boolean validateParameters(int communityId, String message){
		if(communityId < 0 
				|| !Communities.isCommunity(communityId)
				|| !Validator.isValidRequestMessage(message)){
			return false;
		}

		return true;		
	}


}
