package org.starexec.servlets;

import java.io.IOException;
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
import org.starexec.util.Util;

/**
 * @author Todd Elvers
 */
public class Verify extends HttpServlet {
	private static final Logger log = Logger.getLogger(Verify.class);
	private static final long serialVersionUID = 1L;	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Verify() {
        super();        
    }
	
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// Don't accept POST requests to this servlet
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Illegal POST request");
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	if(Util.paramExists(P.EMAIL_CODE, request) && !Util.paramExists(P.LEADER_RESPONSE, request)) {
    		handleActivation(request, response);
    	} else if(Util.paramExists(P.LEADER_RESPONSE, request)) {
    		handleAcceptance(request, response);
    	}
    }
    
    /**
     * Deals with acceptance email responses from leaders of a group
     * 
     * @param request the servlet containing the incoming GET request
     * @param response the servlet that handles redirection
     * @throws IOException if any of the redirects fail
     */
    private void handleAcceptance(HttpServletRequest request, HttpServletResponse response) throws IOException  {
    	String code = request.getParameter(P.EMAIL_CODE).toString();
		String verdict = request.getParameter(P.LEADER_RESPONSE).toString();
		
		// Check if a leader has handled this acceptance email already
		Invite invite = Database.getInvite(code);
		if(invite == null){
			// If so, redirect them to the leader_response.jsp and tell them their response will be ignored
			response.sendRedirect("/starexec/leader_response.jsp?result=dupLeaderResponse");
			return;
		}
		
		boolean wasApproved = false;
		boolean isRegistered = false;
		
		// See if the user is registered or not
		User user = Database.getUnregisteredUser(invite.getUserId());
		if(user == null){
			user = Database.getUser(invite.getUserId());
			isRegistered = true;
		}
		
		// Get name of community user is trying to join
		String communityName = Database.getSpaceName(invite.getCommunityId());
		
		if(verdict.equals("approve")){
			
			// Add them to the community & remove their entry in INVITES
			wasApproved = Database.approveUser(invite.getUserId(), invite.getCommunityId());
			
			if(wasApproved) {
				// Notify user they've been approved
				Mail.sendNotification(user, communityName, wasApproved, request);
				log.info(String.format("User [%s] has finished the approval process and now apart of the %s community.", user.getFullName(), communityName));
				response.sendRedirect("/starexec/leader_response.jsp");
			} 
		} else if(verdict.equals("decline")) {
			// Remove their entry from INVITES
			Database.declineUser(invite.getUserId(), invite.getCommunityId());

			if(!isRegistered){
				// Notify user they've been declined and deleted from our system
				Mail.sendNotification(user, communityName, request);
			} else {
				// Notify user they've been declined (but not deleted)
				Mail.sendNotification(user, communityName, wasApproved, request);
			}
			
			log.info(String.format("User [%s]'s request to join the %s community was declined.", user.getFullName(), communityName));
			response.sendRedirect("/starexec/leader_response.jsp");
		}
    }
    
    /**
     * Handles the email verification hyperlinks and activates the given user
     * if the activation code they provide matches that from the table VERIFY
     * 
     * @param request the servlet containing the incoming GET request
     * @param response the servlet that handles redirection
     * @throws IOException if any of the redirects fail
     */
    private void handleActivation(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	String code = request.getParameter(P.EMAIL_CODE).toString();
		
    	// IF no code in VERIFY matches, then user_id = -1
    	// ELSE user_id = the id of the user that was just activated
    	long user_id = Database.redeemCode(code);
    	User newUser;
    	if(user_id == -1) {
    		log.info(String.format("email verification failed - likey a duplicate activation attempt"));
    		response.sendError(HttpServletResponse.SC_NOT_FOUND, "This activation page has expired and no longer exists!");
    		return;
    	} else {
    		newUser = Database.getUnregisteredUser(user_id);
    		log.info(String.format("User [%s] has been activated.", newUser.getFullName()));
    		response.sendRedirect("/starexec/email_activated.jsp");
    	}   
    	Invite invite = Database.getInvite(user_id);
    	if(invite == null){
    		log.warn(String.format("No invite exists for user [%s].", newUser.getFullName()));
    		return;
    	}
    	
    	// Send the invite to the leaders of the community 
    	Mail.sendInvitesToLeaders(newUser, invite, request);
    	
    
    }
    

 
}
