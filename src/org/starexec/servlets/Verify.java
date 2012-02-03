package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.Util;

/**
 * @author Todd Elvers
 */
@SuppressWarnings("serial")
public class Verify extends HttpServlet {
	private static final Logger log = Logger.getLogger(Verify.class);     
	
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	if(Util.paramExists(Mail.EMAIL_CODE, request) && !Util.paramExists(Mail.LEADER_RESPONSE, request)) {
    		// Handle user activation request
    		handleActivation(request, response);
    	} else if(Util.paramExists(Mail.EMAIL_CODE, request) && Util.paramExists(Mail.LEADER_RESPONSE, request)) {
    		// Handle community request accept/decline
    		handleAcceptance(request, response);
    	} else {
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
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
    	String code = (String)request.getParameter(Mail.EMAIL_CODE);
		String verdict = (String)request.getParameter(Mail.LEADER_RESPONSE);
		
		// Check if a leader has handled this acceptance email already
		CommunityRequest comRequest = Requests.getCommunityRequest(code);
		if(comRequest == null){
			// If so, redirect them to the leader_response.jsp and tell them their response will be ignored
			response.sendRedirect("/starexec/public/messages/leader_response.jsp?result=dupLeaderResponse");
			return;
		}
		
		boolean wasApproved = false;
		boolean isRegistered = false;
		
		// See if the user is registered or not
		User user = Users.getUnregistered(comRequest.getUserId());
		if(user == null){
			user = Users.get(comRequest.getUserId());
			isRegistered = true;
		}
		
		// Get name of community user is trying to join
		String communityName = Spaces.getName(comRequest.getCommunityId());
		String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
		
		if(verdict.equals("approve")){			
			// Add them to the community & remove the request from the database
			wasApproved = Requests.approveCommunityRequest(comRequest.getUserId(), comRequest.getCommunityId());
			
			if(wasApproved) {
				// Notify user they've been approved				
				Mail.sendRequestResults(user, communityName, wasApproved, false, serverName);
				log.info(String.format("User [%s] has finished the approval process and now apart of the %s community.", user.getFullName(), communityName));
				response.sendRedirect("/starexec/public/messages/leader_response.jsp");
			} 
		} else if(verdict.equals("decline")) {
			// Remove their entry from INVITES
			Requests.declineCommunityRequest(comRequest.getUserId(), comRequest.getCommunityId());

			// Notify user they've been declined
			if(isRegistered) {
				Mail.sendRequestResults(user, communityName, false, false, serverName);	
			} else {
				Mail.sendRequestResults(user, communityName, false, true, serverName);
			}					
			
			log.info(String.format("User [%s]'s request to join the %s community was declined.", user.getFullName(), communityName));
			response.sendRedirect("/starexec/public/messages/leader_response.jsp");
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
    	String code = request.getParameter(Mail.EMAIL_CODE).toString();
		
    	// IF no code in VERIFY matches, then userId = -1
    	// ELSE userId = the id of the user that was just activated
    	long userId = Requests.redeemActivationCode(code);
    	
    	User newUser;
    	if(userId == -1) {
    		log.info(String.format("email verification failed - likey a duplicate activation attempt"));
    		response.sendError(HttpServletResponse.SC_NOT_FOUND, "This activation page has expired and no longer exists!");
    		return;
    	} else {
    		newUser = Users.getUnregistered(userId);
    		log.info(String.format("User [%s] has been activated.", newUser.getFullName()));
    		response.sendRedirect("/starexec/public/messages/email_activated.jsp");
    	}   
    	
    	CommunityRequest comReq = Requests.getCommunityRequest(userId);
    	if(comReq == null){
    		log.warn(String.format("No community request exists for user [%s].", newUser.getFullName()));
    		return;
    	}
    	
    	// Send the invite to the leaders of the community
    	String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
    	Mail.sendCommunityRequest(newUser, comReq, serverName);    	   
    }
    

 
}
