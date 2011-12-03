package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.commons.mail.*;
import org.starexec.constants.*;
import org.starexec.data.Database;
import org.starexec.data.to.*;

/**
 * Contains utilities for sending mail from the local SMTP server
 */
public class Mail {
	private static final Logger log = Logger.getLogger(Mail.class);

	/**
	 * Sends an e-mail from the configured SMTP server
	 * @param message The body of the message
	 * @param subject The subject of the message
	 * @param from Who the message is from (null to send from default account)
	 * @param to The list of e-mail addresses to send the message to
	 */
	public static void mail(String message, String subject, String[] to) {
		try {
			if(to == null || to.length < 1) {
				return;
			}					
			
			Email email = new SimpleEmail();
			email.setHostName(R.EMAIL_SMTP);
			email.setSmtpPort(R.EMAIL_SMTP_PORT);
			email.setSubject(subject);
			email.setMsg(message);
			
			if(R.EMAIL_USER != null && R.EMAIL_PWD != null) {
				email.setAuthenticator(new DefaultAuthenticator(R.EMAIL_USER, R.EMAIL_PWD));
				email.setTLS(true);
			}
			
			if(R.EMAIL_USER != null) {
				email.setFrom(R.EMAIL_USER);
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
     * Sends an acceptance email to the leaders of a given community s
     * they can approve/decline a new user's request to join 
     * 
     * @param user the user trying to join the community
	 * @param comReq The community request containing the information to construct the e-mail
	 * @param serverURL the base URL of the server (e.g. http://starexec.cs.uiowa.edu/)
     * @throws IOException if acceptance_email cannot be found
     * @author Todd Elvers
     */
    public static void sendCommunityRequest(User user, CommunityRequest comReq, String serverURL) throws IOException{    	
    	String communityName = Database.getSpaceName(comReq.getCommunityId());
    	
    	// Figure out the email addresses of the leaders of the space
    	List<User> leaders = Database.getLeadersOfSpace(comReq.getCommunityId());
    	String[] leaderEmails = new String[leaders.size()];
    	
    	for(int i = 0; i < leaders.size(); i++) {
    		leaderEmails[i] = leaders.get(i).getEmail();
    	}
    	
    	// Configure pre-built message
		String email = Util.readFile(new File(R.CONFIG_PATH, "acceptance_email.txt"));
		email = email.replace("$$COMMUNITY$$", communityName);
		email = email.replace("$$NEWUSER$$", user.getFullName());
		email = email.replace("$$EMAIL$$", user.getEmail());
		email = email.replace("$$INSTITUTION$$", user.getInstitution());
		email = email.replace("$$MESSAGE$$", comReq.getMessage());
		email = email.replace("$$APPROVE$$", String.format("%s/starexec/Verify?%s=%s&%s=%s", serverURL, P.EMAIL_CODE, comReq.getCode(), P.LEADER_RESPONSE, "approve"));
		email = email.replace("$$DECLINE$$", String.format("%s/starexec/Verify?%s=%s&%s=%s", serverURL, P.EMAIL_CODE, comReq.getCode(), P.LEADER_RESPONSE, "decline"));
		
		// Send email
		Mail.mail(email, "STAREXEC - Request to join " + communityName, leaderEmails);

		log.info(String.format("Acceptance email sent to leaders of [%s] to approve/decline %s's request", communityName, user.getFullName()));
    }
    
	/**
	 * Sends an email to a given user with a hyperlink to
	 * activate their account
	 * 
	 * @param user the user to send the email to
	 * @param request the servlet containing the incoming POST
	 * @param code the activation code to send
	 * @param serverURL the base URL of the server (e.g. http://starexec.cs.uiowa.edu/)
	 * @throws IOException if verification_email cannot be found
	 * @author Todd Elvers
	 */
	public static void sendActivationCode(User user, String code, String serverURL) throws IOException {		
		// Configure pre-built message
		String email = Util.readFile(new File(R.CONFIG_PATH, "activation_email.txt"));
		email = email.replace("$$USER$$", user.getFullName());
		email = email.replace("$$LINK$$", String.format("%s/starexec/Verify?%s=%s", serverURL, P.EMAIL_CODE, code ));
		
		// Send email
		Mail.mail(email, "STAREXEC - Verify your account", new String[] { user.getEmail() });
		
		log.info(String.format("Sent activation email to user [%s] at [%s]", user.getFullName(), user.getEmail()));
	}

	
   /**
     * Sends an email to a user to notify them about the result of their request
     * to join a community
     * 
     * @param user the user that requested to join the community
     * @param wasApproved the result of their request to join a community
     * @param wasDeleted whether or not the user was removed from the system as a result of a decline
     * @param communityName the name of the community the user requested to join
	 * @param serverURL the base URL of the server (http://starexec.cs.uiowa.edu/) 
     * @throws IOException if approved_email or declined_email cannot be found
     * @author Todd Elvers
     */
    public static void sendRequestResults(User user, String communityName, boolean wasApproved, boolean wasDeleted, String serverURL) throws IOException{    	
    	if(wasApproved){
    		// Configure pre-built message
    		String email = Util.readFile(new File(R.CONFIG_PATH, "approved_email.txt"));
    		email = email.replace("$$USER$$", user.getFullName());
    		email = email.replace("$$COMMUNITY$$", communityName);
    		email = email.replace("$$LINK$$", String.format("%s/starexec/secure/index.jsp", serverURL));
    		
    		// Send email
    		Mail.mail(email, "STAREXEC - Approved to join " + communityName, new String[] { user.getEmail() });
    		
    		log.info(String.format("Notification email sent to user [%s] - their request to join the %s community was approved", user.getFullName(), communityName));
    	} else {
    		// Configure pre-built message
    		String email;
    		
    		if(wasDeleted) {
    			email = Util.readFile(new File(R.CONFIG_PATH, "declined_deleted_email.txt"));
    			email = email.replace("$$LINK$$", String.format("%s/starexec/registration.jsp", serverURL));    			
    		} else {
    			email = Util.readFile(new File(R.CONFIG_PATH, "declined_email.txt"));    			
    			email = email.replace("$$LINK$$", String.format("%s/starexec/pages/make_invite.jsp", serverURL));
    		}
    		
    		email = email.replace("$$USER$$", user.getFullName());
    		email = email.replace("$$COMMUNITY$$", communityName);
    		
    		// Send email
    		Mail.mail(email, "STAREXEC - Declined to join " + communityName, new String[] { user.getEmail() });
    		
    		log.info(String.format("Notification email sent to user [%s] - their request to join to the %s community was declined", user.getFullName(), communityName));
    	}
    }

	
    /**
     * Sends an email to a user who has requested to reset their password containing
     * a hyperlink to a page where a temporary password is generated
     * 
     * @param newUser the user requesting to reset their password
     * @param code the UUID code used to create a safe hyperlink to email to the user
     * @param request the servlet containing information used to create the hyperlink 
     * @throws IOException if password_reset_email cannot be found
     * @author Todd Elvers
     */
    public static void sendPasswordReset(User newUser, String code, String serverURL) throws IOException {
    	
		// Configure pre-built message
		String email = Util.readFile(new File(R.CONFIG_PATH, "password_reset_email.txt"));
		email = email.replace("$$USER$$", newUser.getFullName());
		email = email.replace("$$LINK$$", String.format("%s/starexec/PasswordReset?%s=%s", serverURL, P.PASS_RESET, code));
		
		// Send email
		Mail.mail(email, "STAREXEC - Password reset", new String[] { newUser.getEmail() });
		
		log.info(String.format("Password reset email sent to user [%s].", newUser.getFullName()));
    }	
}
