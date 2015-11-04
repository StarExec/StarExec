package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.starexec.constants.R;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

import com.google.gson.Gson;

/**
 * Servlet that handles email verification for new users, emailing leaders
 * of spaces when new users request to join, and accept/decline responses
 * sent back from leaders about user join requests
 * 
 * @author Todd Elvers & Tyler Jensen
 */
@SuppressWarnings("serial")
public class Verify extends HttpServlet {
	private static final Logger log = Logger.getLogger(Verify.class);     
	private static Gson gson = new Gson();
	
	
	
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (Util.paramExists(Mail.CHANGE_EMAIL_CODE, request)) { 
			// Handle change email request
			handleEmailChange(request, response);

		} else if(Util.paramExists(Mail.EMAIL_CODE, request) && !Util.paramExists(Mail.LEADER_RESPONSE, request)) {
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
	 * Deals with email change request verification.
	 *
     * @param request the servlet containing the incoming GET request
     * @param response the servlet that handles redirection
	 * @throws IOException if any redirect fails
	 */
    private void handleEmailChange(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int userId = SessionUtil.getUserId(request);
		log.debug("(handleEmailChange) User with id="+userId+" visited change email verification page.");
		String codeParam = (String)request.getParameter(Mail.CHANGE_EMAIL_CODE);
		Pair<String, String> emailAndCode = null;
		try {
			if (Requests.changeEmailRequestExists(userId)) {
					emailAndCode = Requests.getChangeEmailRequest(userId);
			} else {
				// The email change request does not exist in the database.
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			String newEmail = emailAndCode.getLeft();
			String codeInDb = emailAndCode.getRight();
			if (codeInDb.equals(codeParam)) {
				Users.updateEmail(userId, newEmail); 
				Requests.deleteChangeEmailRequest(userId);
				response.sendRedirect(Util.docRoot("public/messages/email_changed.jsp?email="+newEmail));
				return;
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} catch (StarExecDatabaseException e) {
			log.error("Database error while trying to change users email.", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
    
    /**
     * Deals with acceptance email responses from leaders of a group
     * 
     * @param request the servlet containing the incoming GET request
     * @param response the servlet that handles redirection
     * @throws IOException if any of the redirects fail
     * @author Todd Elvers
     */
    private void handleAcceptance(HttpServletRequest request, HttpServletResponse response) throws IOException  {
    	String code = (String)request.getParameter(Mail.EMAIL_CODE);
		String verdict = (String)request.getParameter(Mail.LEADER_RESPONSE);

		boolean userWasApproved = verdict.equals(R.APPROVE_COMMUNITY_REQUEST);
		
		CommunityRequest comRequest = Requests.getCommunityRequest(code);


		// TODO Give requests that were sent by email parameter too.
		boolean sentFromCommunityPage = Util.paramExists(R.SENT_FROM_COMMUNITY_PAGE, request);

		boolean requestHasBeenHandled = checkIfRequestHasBeenHandled(response, comRequest, sentFromCommunityPage);
		if (requestHasBeenHandled) {
			return;
		}

		
		boolean isRegistered = false;
		
		// See if the user is registered or not
		User user = Users.getUnregistered(comRequest.getUserId());
		if(user == null){
			user = Users.get(comRequest.getUserId());
			isRegistered = true;
		}
		
		// Get name of community user is trying to join
		String communityName = Spaces.getName(comRequest.getCommunityId());
		
		if(verdict.equals(R.APPROVE_COMMUNITY_REQUEST)){			
			handleApproveCommunityRequest(response, user, comRequest, communityName, sentFromCommunityPage);
		} else if(verdict.equals(R.DECLINE_COMMUNITY_REQUEST)) {
			handleDeclineCommunityRequest(response, user, comRequest, isRegistered, communityName, sentFromCommunityPage);
		}
		log.debug("Finished handling community request.");
    }

	private static boolean checkIfRequestHasBeenHandled(HttpServletResponse response, CommunityRequest comRequest, 
			boolean sentFromCommunityPage) throws IOException 
	{
		// Check if a leader has handled this acceptance email already
		if(comRequest == null){
			// If so, redirect them to the leader_response.jsp and tell them their response will be ignored
			if (sentFromCommunityPage) {
				response.setContentType("application/json");
				response.getWriter().write(gson.toJson(new ValidatorStatusCode(false, "This user already been declined/accepted.")));
			} else {
				response.sendRedirect(Util.docRoot("public/messages/leader_response.jsp?result=dupLeaderResponse"));
			}
			return true;
		} else {
			return false;
		}
	}

	private static void handleApproveCommunityRequest(HttpServletResponse response, User user, CommunityRequest comRequest, 
			String communityName, boolean sentFromCommunityPage) throws IOException
	{
		// Add them to the community & remove the request from the database
		boolean successfullyApproved = Requests.approveCommunityRequest(comRequest.getUserId(), comRequest.getCommunityId());

		if (!successfullyApproved) {
			log.error("Did not successfully approve user community request for user with id="+comRequest.getUserId()
					+" even though an admin or community leader approved them.");
			return;
		}
		
		// Notify user they've been approved				
		Mail.sendRequestResults(user, communityName, successfullyApproved, false);
		
		// Create a personal subspace for the user in the space they were admitted to
		if(createPersonalSubspace(comRequest.getCommunityId(), user)){
			log.info(String.format("Personal space successfully created for user [%s]", user.getFullName()));
		}
		
		log.info(String.format("User [%s] has finished the approval process and now apart of the %s community.", user.getFullName(), communityName));
		if (sentFromCommunityPage) {
			response.setContentType("application/json");
			response.getWriter().write(gson.toJson(new ValidatorStatusCode(true, "The user has been successfully approved.")));
		} else {
			response.sendRedirect(Util.docRoot("public/messages/leader_response.jsp"));
		}
	}

	private static void handleDeclineCommunityRequest( HttpServletResponse response, User user, CommunityRequest comRequest, 
			boolean isRegistered, String communityName, boolean sentFromCommunityPage) throws IOException
	{
		// Remove their entry from INVITES
		Requests.declineCommunityRequest(comRequest.getUserId(), comRequest.getCommunityId());

		// Notify user they've been declined
		if(isRegistered) {
			Mail.sendRequestResults(user, communityName, false, false);	
		} else {
			Mail.sendRequestResults(user, communityName, false, true);
		}					
		
		log.info(String.format("User [%s]'s request to join the %s community was declined.", user.getFullName(), communityName));
		if (sentFromCommunityPage) {
			response.setContentType("application/json");
			response.getWriter().write(gson.toJson(new ValidatorStatusCode(true, "The user has been successfully declined.")));
		} else {
			response.sendRedirect(Util.docRoot("public/messages/leader_response.jsp"));
		}
	}
    
    /**
     * Handles the email verification hyperlinks and activates the given user
     * if the activation code they provide matches that from the table VERIFY
     * 
     * @param request the servlet containing the incoming GET request
     * @param response the servlet that handles redirection
     * @throws IOException if any of the redirects fail
     * @author Todd Elvers & Tyler Jensen
     */
    private void handleActivation(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	String code = request.getParameter(Mail.EMAIL_CODE).toString();
		
    	// IF no code in VERIFY matches, then userId = -1
    	// ELSE userId = the id of the user that was just activated
    	int userId = Requests.redeemActivationCode(code);
    	
    	User newUser;
    	if(userId == -1) {
    		log.info(String.format("email verification failed - likey a duplicate activation attempt"));
    		response.sendError(HttpServletResponse.SC_NOT_FOUND, "This activation page has expired and no longer exists!");
    		return;
    	} else {
    		newUser = Users.getUnregistered(userId);
    		log.info(String.format("User [%s] has been activated.", newUser.getFullName()));
    		response.sendRedirect(Util.docRoot("public/messages/email_activated.jsp"));
    	}   
    	
    	CommunityRequest comReq = Requests.getCommunityRequest(userId);
    	if(comReq == null){
    		log.warn(String.format("No community request exists for user [%s].", newUser.getFullName()));
    		return;
    	}
    	
    	// Send the invite to the leaders of the community
    	Mail.sendCommunityRequest(newUser, comReq);    	   
    }
    
    /**
     * Creates a new personal space as a subspace of the space the user was admitted to
     * 
     * @param parentSpaceId the id of the space this new personal space will be a subspace of
     * @param user the user for whom this new personal space is being created
     * @return true if the personal subspace was successfully created, false otherwise
     */
    public static boolean createPersonalSubspace(int parentSpaceId, User user){
    	// Generate space name (e.g. IF name = Todd Elvers, THEN personal space name = todd_elvers)
    	StringBuilder sb = new StringBuilder();
		sb.append(user.getFirstName().toLowerCase());
		sb.append("_");
		sb.append(user.getLastName().toLowerCase());
		sb.toString();
		
    	// Set the space's attributes
		Space s = new Space();
    	s.setName(sb.toString());
		s.setDescription(R.PERSONAL_SPACE_DESCRIPTION);
		s.setLocked(false);
		s.setPermission(new Permission(true));
		
		// Return true if the subspace is successfully created, false otherwise
    	return Spaces.add(s, parentSpaceId, user.getId()) > 0;
    }
 
}
