package org.starexec.servlets;

import java.io.IOException;
import java.util.UUID;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.P;
import org.starexec.data.Database;
import org.starexec.data.to.*;
import org.starexec.util.*;

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

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		User user = SessionUtil.getUser(request);			
		
		// Validate parameters of request & construct Invite object
		CommunityRequest comRequest = constructComRequest(user, request);
		if(comRequest == null){
			response.sendRedirect("/starexec/secure/community/join.jsp?result=requestNotSent&cid=-1");
			return;
		}
		
		boolean added = Database.addCommunityRequest(user, comRequest.getCommunityId(), comRequest.getCode(), comRequest.getMessage());
		if(added){
			// Send the invite to the leaders of the community 
			String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
			Mail.sendCommunityRequest(user, comRequest, serverName);
			response.sendRedirect("/starexec/secure/community/join.jsp?result=requestSent&cid=" + comRequest.getCommunityId());
		} else {
			// There was a problem
			response.sendRedirect("/starexec/secure/community/join.jsp?result=requestNotSent&cid=" + comRequest.getCommunityId());
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
			if(!Util.paramExists(P.USER_MESSAGE, request) ||
			   !Util.paramExists(P.USER_COMMUNITY, request)) {
				return null;
			}
			
			String message = request.getParameter(P.USER_MESSAGE);
			long communityId = Long.parseLong(request.getParameter(P.USER_COMMUNITY)); 		
			
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
	 * @return true iff communityId is a valid space_id and that the user's message is
	 * between 1 and 300 characters in length
	 */
	private boolean validateParameters(long communityId, String message){
		// Gather the number of communities ( the +1 is for the root )
		int numberOfCommunities = Database.getCommunities().size() + 1;
		if(communityId < 0 
				|| communityId > numberOfCommunities
				|| !Validate.message(message)){
			return false;
		}

		return true;		
	}

}
