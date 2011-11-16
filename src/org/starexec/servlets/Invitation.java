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
import org.starexec.data.to.Invite;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Validate;


/**
 * @author Todd Elvers
 */
public class Invitation extends HttpServlet {
	private static final Logger log = Logger.getLogger(Invitation.class);
	private static final long serialVersionUID = 1L;
	
    public Invitation() {
        super();
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Don't accept GET, this could be a malicious requested
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Illegal GET request!");
	}


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		User user = SessionUtil.getUser(request);
		// Validate parameters of request & construct Invite object
		Invite invite = constructInvite(user, request);
		if(invite == null){
			response.sendRedirect("/starexec/secure/make_invite.jsp?result=requestNotSent");
			return;
		}
		boolean added = Database.addInvite(user, invite.getCommunityId(), invite.getCode(), invite.getMessage());
		if(added){
			// Send the invite to the leaders of the community 
			Mail.sendInvitesToLeaders(user, invite, request);
			response.sendRedirect("/starexec/secure/make_invite.jsp?result=requestSent");
		} else {
			// Not adding this error message
//			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "And what went wrong here");
			response.sendRedirect("/starexec/secure/make_invite.jsp?result=requestNotSent");
		}
	}
	
	
	/**
	 * Builds an Invite object given a user and request
	 * 
	 * @param user the user to create the invite for
	 * @param request the servlet containing the invite information
	 * @return the invite constructed
	 */
	private Invite constructInvite(User user, HttpServletRequest request){
		String message = request.getParameter(P.USER_MESSAGE);
		long communityId = -1;
		try{
			communityId = Long.parseLong(request.getParameter(P.USER_COMMUNITY)); 
		} catch (Exception NumberFormatException) {
			// Do nothing
		}
		
		if(validateParameters(communityId, message)){
			Invite invite = new Invite();
			invite.setUserId(user.getId());
			invite.setCommunityId(communityId);
			invite.setCode(UUID.randomUUID().toString());
			invite.setMessage(message);
			return invite;	
		} else {
			return null;
		}
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
		int numberOfCommunities = Database.getRootSpaces().size() + 1;
		if(communityId < 0 
				|| communityId > numberOfCommunities
				|| !Validate.message(message)){
			return false;
		} else {
			return true;
		}
	}

}
