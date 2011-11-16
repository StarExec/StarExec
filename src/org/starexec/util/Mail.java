package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.mail.*;
import org.apache.log4j.Logger;

import org.starexec.constants.P;
import org.starexec.constants.R;
import org.starexec.data.Database;
import org.starexec.data.to.Invite;
import org.starexec.data.to.User;

/**
 * Contains utilities for sending mail from the local SMTP server
 * @deprecated This needs refactored/updated but SHOULD work on starexec.cs.uiowa.edu
 */
public class Mail {
	private static final Logger log = Logger.getLogger(Mail.class);

	public static void mail(String message, String subject, String from, String[] to) {
		try {
			Email email = new SimpleEmail();
			email.setHostName(R.EMAIL_SMTP);
			email.setSmtpPort(R.EMAIL_SMTP_PORT);
			email.setSubject(subject);
			email.setMsg(message);
			
			if(R.EMAIL_USER != null && R.EMAIL_PWD != null) {
				email.setAuthenticator(new DefaultAuthenticator(R.EMAIL_USER, R.EMAIL_PWD));
				email.setTLS(true);
			}
			
			if(from != null) {
				email.setFrom(from);
			}					
			
			for(String s : to) {
				email.addTo(s);
			}
			
			email.send();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}
	
    /**
     * Sends acceptance emails to all leaders of a given community
     * 
     * @param newUser the user requesting to join the community
     * @param invite the invite containing the communityId, message, etc.
     * @param request the servlet containing information used to generate hyperlinks
     * @throws IOException if sendAcceptance() can't find the appropriate email files in /config
     * @author Todd Elvers
     */
    public static void sendInvitesToLeaders(User newUser, Invite invite, HttpServletRequest request) throws IOException{
    	// Get the community name the user is trying to join
    	String communityName = Database.getSpaceName(invite.getCommunityId());
    	List<User> leaders = Database.getLeadersOfSpace(invite.getCommunityId());
    	if(leaders.size() == 0){
    		log.warn("Careful, the space you're considering has no leaders to send acceptance emails to.");
    		return;
    	}
    	for(User leader : leaders){
    		sendAcceptance(leader, newUser, communityName, invite, request);
    	}
    }
    
    /**
     * Sends an acceptance email to the leaders of a given community s
     * they can approve/decline a new user's request to join 
     * 
     * @param leader a leader of the community
     * @param newUser the user trying to join the community
     * @param communityName the name of the community in question
	 * @param request the servlet containing the incoming GET request
     * @throws IOException if acceptance_email cannot be found
     * @author Todd Elvers
     */
    private static void sendAcceptance(User leader, User newUser, String communityName, Invite invite, HttpServletRequest request) throws IOException{
    	String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
    	
		// Configure pre-built message
		String email = Util.readFile(new File(R.CONFIG_PATH, "acceptance_email"));
		email = email.replace("$$LEADER$$", leader.getFullName());
		email = email.replace("$$COMMUNITY$$", communityName);
		email = email.replace("$$NEWUSER$$", newUser.getFullName());
		email = email.replace("$$MESSAGE$$", invite.getMessage());
		email = email.replace("$$APPROVE$$", String.format("%s/starexec/Verify?%s=%s&%s=%s", serverName, P.EMAIL_CODE, invite.getCode(), P.LEADER_RESPONSE, "approve"));
		email = email.replace("$$DECLINE$$", String.format("%s/starexec/Verify?%s=%s&%s=%s", serverName, P.EMAIL_CODE, invite.getCode(), P.LEADER_RESPONSE, "decline"));
		
		// Send email
		Mail.mail(email, "Request to join your community", "starexec@divms.uiowa.edu", new String[] { leader.getEmail() });
		
		log.info(String.format("Acceptance email sent to leader [%s] at [%s] to approve/decline %s's request", leader.getFullName(), leader.getEmail(), newUser.getFullName()));
    }
    
	/**
	 * Sends an email to a given user with a hyperlink to
	 * activate their account
	 * 
	 * @param user the user to send the email to
	 * @param request the servlet containing the incoming POST
	 * @param code the activation code to send
	 * @throws IOException if verification_email cannot be found
	 * @author Todd Elvers
	 */
	public static void sendActivationCode(User user, String code, HttpServletRequest request) throws IOException{
		String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

		// Configure pre-built message
		String email = Util.readFile(new File(R.CONFIG_PATH, "activation_email"));
		email = email.replace("$$USER$$", user.getFullName());
		email = email.replace("$$LINK$$", String.format("%s/starexec/Verify?%s=%s", serverName, P.EMAIL_CODE, code ));
		
		// Send email
		Mail.mail(email, "Verify your Starexec Account", "starexec@divms.uiowa.edu", new String[] { user.getEmail() });
		
		log.info(String.format("Sent activation email to user [%s] at [%s]", user.getFullName(), user.getEmail()));
	}

	
   /**
     * Sends an email to a user to notify them about the result of their request
     * to join a community
     * 
     * @param newUser the user that requested to join the community
     * @param wasApproved the result of their request to join a community
     * @param communityName the name of the community the user requested to join
     * @param request the servlet containing information used to create the hyperlink 
     * @throws IOException if approved_email or declined_email cannot be found
     * @author Todd Elvers
     */
    public static void sendNotification(User newUser, String communityName, boolean wasApproved, HttpServletRequest request) throws IOException{
    	String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
    	
    	if(wasApproved){
    		// Configure pre-built message
    		String email = Util.readFile(new File(R.CONFIG_PATH, "approved_email"));
    		email = email.replace("$$USER$$", newUser.getFullName());
    		email = email.replace("$$COMMUNITY$$", communityName);
    		
    		// Send email
    		Mail.mail(email, "You have been approved", "starexec@divms.uiowa.edu", new String[] { newUser.getEmail() });
    		
    		log.info(String.format("Notification email sent to user [%s] - their request to join the %s community was approved", newUser.getFullName(), communityName));
    	} else {
    		// Configure pre-built message
    		String email = Util.readFile(new File(R.CONFIG_PATH, "declined_email"));
    		email = email.replace("$$USER$$", newUser.getFullName());
    		email = email.replace("$$COMMUNITY$$", communityName);
    		email = email.replace("$$LINK$$", String.format("%s/starexec/pages/make_invite.jsp", serverName));
    		// Send email
    		Mail.mail(email, "You have been declined", "starexec@divms.uiowa.edu", new String[] { newUser.getEmail() });
    		
    		log.info(String.format("Notification email sent to user [%s] - their request to join to the %s community was declined", newUser.getFullName(), communityName));
    	}
    }
    
    
    
    /**
     * Sends an email to a user to notify them that they were denied access
     * to the community they chose during registration, that their unregistered
     * account has been deleted from our system, and that they are free to try
     * and reregister if they like.
     * 
     * @param newUser the user that requested to join the community
     * @param communityName the name of the community the user requested to join
     * @param request the servlet containing information used to create the hyperlink 
     * @throws IOException if declined_deleted_email cannot be found
     * @author Todd Elvers
     */
    public static void sendNotification(User newUser, String communityName, HttpServletRequest request) throws IOException{
    	String serverName = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
    	
		// Configure pre-built message
		String email = Util.readFile(new File(R.CONFIG_PATH, "declined_deleted_email"));
		email = email.replace("$$USER$$", newUser.getFullName());
		email = email.replace("$$COMMUNITY$$", communityName);
		email = email.replace("$$LINK$$", String.format("%s/starexec/registration.jsp", serverName));
		
		// Send email
		Mail.mail(email, "You have been declined", "starexec@divms.uiowa.edu", new String[] { newUser.getEmail() });
		
		log.info(String.format("Notification email sent to user [%s] - their request to join the %s community was declined and they were deleted from the database.", newUser.getFullName(), communityName));
    }
}
