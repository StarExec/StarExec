package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.lang.NullPointerException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.starexec.constants.R;
import org.starexec.data.database.Reports;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.ErrorLog;
import org.starexec.data.to.Report;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.servlets.PasswordReset;

/**
 * Contains utilities for sending mail from the local SMTP server
 */
public class Mail {
	private static final StarLogger log = StarLogger.getLogger(Mail.class);
	public static final String EMAIL_CODE = "conf";			// Param string for email verification codes
	public static final String CHANGE_EMAIL_CODE = "changeEmail";
	public static final String LEADER_RESPONSE = "lead";	// Param string for leader response decisions	


	public static void mail(String message, String subject, List<String> to) {
		mail(message, subject, to.toArray(new String[]{}));
	}


		/**
         * Sends an e-mail from the configured SMTP server
         * @param message The body of the message
         * @param subject The subject of the message
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
     * @throws IOException if acceptance_email cannot be found
     * @author Todd Elvers
     */
    public static void sendCommunityRequest(User user, CommunityRequest comReq) throws IOException{    	
    	String communityName = Spaces.getName(comReq.getCommunityId());
    	
    	// Figure out the email addresses of the leaders of the space
    	List<User> leaders = Spaces.getLeaders(comReq.getCommunityId());
    	String[] leaderEmails = new String[leaders.size()];
    	
    	for(int i = 0; i < leaders.size(); i++) {
    		leaderEmails[i] = leaders.get(i).getEmail();
    	}
    	
    	// Configure pre-built message
		String email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/acceptance_email.txt"));
		email = email.replace("$$COMMUNITY$$", communityName);
		email = email.replace("$$NEWUSER$$", user.getFullName());
		email = email.replace("$$EMAIL$$", user.getEmail());
		email = email.replace("$$INSTITUTION$$", user.getInstitution());
		email = email.replace("$$MESSAGE$$", comReq.getMessage());
		email = email.replace("$$APPROVE$$", Util.url(String.format("public/verification/email?%s=%s&%s=%s", Mail.EMAIL_CODE, comReq.getCode(), Mail.LEADER_RESPONSE, "approve")));
		email = email.replace("$$DECLINE$$", Util.url(String.format("public/verification/email?%s=%s&%s=%s", Mail.EMAIL_CODE, comReq.getCode(), Mail.LEADER_RESPONSE, "decline")));
		
		// Send email
		Mail.mail(email, "STAREXEC - Request to join " + communityName, leaderEmails);

		log.info(String.format("Acceptance email sent to leaders of [%s] to approve/decline %s's request", communityName, user.getFullName()));
    }
    
	/**
	 * Sends an email to a given user with a hyperlink to
	 * activate their account
	 * 
	 * @param user the user to send the email to
	 * @param code the activation code to send
	 * @throws IOException if verification_email cannot be found
	 * @author Todd Elvers
	 */
	public static void sendActivationCode(User user, String code) throws IOException {		
		// Configure pre-built message
		String email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/activation_email.txt"));
		email = email.replace("$$USER$$", user.getFullName());
		email = email.replace("$$LINK$$", Util.url(String.format("public/verification/email?%s=%s", Mail.EMAIL_CODE, code )));
		
		// Send email
		Mail.mail(email, "STAREXEC - Verify your account", new String[] { user.getEmail() });
		
		log.info(String.format("Sent activation email to user [%s] at [%s]", user.getFullName(), user.getEmail()));
	}

	public static void sendEmailChangeValidation(String newEmail, String code) throws IOException {
		String email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/change_email.txt"));
		email = email.replace("$$LINK$$", Util.url(String.format("public/verification/email?%s=%s", Mail.CHANGE_EMAIL_CODE, code)));
		Mail.mail(email, "STAREXEC - Change Email", new String[] { newEmail });
	}

	
   /**
     * Sends an email to a user to notify them about the result of their request
     * to join a community
     * 
     * @param user the user that requested to join the community
     * @param wasApproved the result of their request to join a community
     * @param wasDeleted whether or not the user was removed from the system as a result of a decline
     * @param communityName the name of the community the user requested to join
     * @throws IOException if approved_email or declined_email cannot be found
     * @author Todd Elvers
     */
    public static void sendRequestResults(User user, String communityName, boolean wasApproved, boolean wasDeleted) throws IOException{    	
    	if(wasApproved){
    		// Configure pre-built message
    		String email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/approved_email.txt"));
    		email = email.replace("$$USER$$", user.getFullName());
    		email = email.replace("$$COMMUNITY$$", communityName);
    		email = email.replace("$$LINK$$", Util.url("secure/index.jsp"));
    		
    		// Send email
    		Mail.mail(email, "STAREXEC - Approved to join " + communityName, new String[] { user.getEmail() });
    		
    		log.info(String.format("Notification email sent to user [%s] - their request to join the %s community was approved", user.getFullName(), communityName));
    	} else {
    		// Configure pre-built message
    		String email;
    		
    		if(wasDeleted) {
    			email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/declined_deleted_email.txt"));
    			email = email.replace("$$LINK$$", Util.url("registration.jsp"));    			
    		} else {
    			email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "declined_email.txt"));    			
    			email = email.replace("$$LINK$$", Util.url("pages/make_invite.jsp"));
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
     * @throws IOException if password_reset_email cannot be found
     * @author Todd Elvers
     */
    public static void sendPasswordReset(User newUser, String code) throws IOException {
    	
		// Configure pre-built message
		String email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/password_reset_email.txt"));
		email = email.replace("$$USER$$", newUser.getFullName());
		email = email.replace("$$LINK$$", Util.url(String.format("public/reset_password?%s=%s", PasswordReset.PASS_RESET, code)));
		
		// Send email
		Mail.mail(email, "STAREXEC - Password reset", new String[] { newUser.getEmail() });
		
		log.info(String.format("Password reset email sent to user [%s].", newUser.getFullName()));
    }
	
	public static void sendPassword(User user, String password) throws IOException {
    	
		// Configure pre-built message
		String email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/new_user_password.txt"));
		email = email.replace("$$USER$$", user.getFullName());
		email = email.replace("$$PASS$$", password);
		
		// Send email
		Mail.mail(email, "STAREXEC - new user password", new String[] { user.getEmail() });
		
		log.info(String.format("Password reset email sent to user [%s].", user.getFullName()));
	}

	/**
	 * Checks whether or not a report was already emailed today based on if the corresponding file exists.
	 * @return true if a report was already emailed today, else false
	 * @author Albert Giegerich
	 */
	public static boolean reportsEmailedToday() {
		log.debug("Checking if a report was already emailed today...");
		SimpleDateFormat yearMonthDay = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date today = new java.util.Date();
		String todaysDate = yearMonthDay.format(today);
		
		File reportsEmailsDirectory = new File(R.STAREXEC_DATA_DIR, "/reports/");
		
		Iterator<File> reportsIter = FileUtils.iterateFiles(reportsEmailsDirectory, new String[] {"txt"}, false);

		while (reportsIter.hasNext()) {
			// check every report file to see if there is one that was created today
			File reports = reportsIter.next();
			String filename = reports.getName();
			log.debug("Found report file with name: " + filename);
			if (filename.matches(todaysDate + ".txt")) {
				log.debug("A report was already emailed today.");
				return true;
			}
		}
		log.debug("A report was not already emailed today.");
		return false;
	}
	
	/**
	 * Emails reports to subscribed users.
	 * @param recipients the subscribed users to send reports to.
	 * @param email the reports email
	 * @author Albert Giegerich
	 */
	public static void sendReports(List<User> recipients, String email) throws IOException {
		for (User user : recipients) {
			String finalEmail = email.replace("$$USER$$", user.getFullName());	
			Mail.mail(finalEmail, "STAREXEC - REPORT", new String[] { user.getEmail() });
			try {
				// wait a while before sending the next email
				TimeUnit.SECONDS.sleep(R.WAIT_TIME_BETWEEN_EMAILING_REPORTS);
			} catch (InterruptedException e) {
				log.debug("Interrupted while waiting between sending reports", e);
			}
		}
	}

	/**
	 * Generates the reports email from the report data.
	 * Does not replace $$USER$$ with any users name.
	 * @author Albert Giegerich
	 * @return the String representation of the email.
	 */
	public static String generateGenericReportsEmail() throws IOException, SQLException {
		String email = null;
		try {
			email = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/email/reports_email.txt"));
		} catch (IOException e) {
			throw new IOException("Could not open reports_email.txt", e);
		}

		SimpleDateFormat yearMonthDay = new SimpleDateFormat("MMMMM dd, yyyy");
		Calendar calendar = Calendar.getInstance();
		java.util.Date today = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, -7);

		java.util.Date lastWeek = calendar.getTime();
		
		String todaysDate = yearMonthDay.format(today); 
		String lastWeeksDate = yearMonthDay.format(lastWeek);

		email = email.replace("$$DATE$$", lastWeeksDate + " to " + todaysDate);

		List<Report> mainReports = Reports.getAllReportsNotRelatedToQueues();
		List<List<Report>> reportsByQueue = Reports.getAllReportsForAllQueues();

		if (mainReports == null || reportsByQueue == null) {
			throw new NullPointerException("Reports are null.");
		}

		StringBuilder reportBuilder = new StringBuilder();

		// build the main reports string
		for (Report report : mainReports) {
			reportBuilder.append(report.getEventName() + ": " + report.getOccurrences() + "\n");
		}

		email = email.replace("$$MAIN_REPORTS$$", reportBuilder.toString());

		// clear the report builder
		reportBuilder = reportBuilder.delete(0, reportBuilder.length()); 

		// build the queue reports string
		for (List<Report> reportsForOneQueue : reportsByQueue) {
			String currentQueueName = reportsForOneQueue.get(0).getQueueName();
			reportBuilder.append("queue: " + currentQueueName + "\n");
			for (Report report : reportsForOneQueue) {
				reportBuilder.append("  " + report.getEventName() + ": " + report.getOccurrences() + "\n");
			}
			reportBuilder.append("\n");
		}

		email = email.replace("$$QUEUE_REPORTS$$", reportBuilder.toString());

		return email;
	}



	public static void sendErrorLogEmails(List<ErrorLog> errorLogs, List<User> usersToEmail) {
		StringBuilder message = new StringBuilder();
		for (ErrorLog log : errorLogs) {
			message.append("Time: ");
			message.append(log.getTime());
			message.append("\nLevel: ");
			message.append(log.getLevel().toString());
			message.append("\n");

			message.append("Message:\n");
			message.append(log.getMessage());
			message.append("\n\n");
		}

		Mail.mail(
				message.toString(),
				"Error Reports",
				usersToEmail.stream().map(User::getEmail).collect(Collectors.toList()));

	}

	/**
	 * Stores the reports email as a text file.
	 * @param email the reports email.
	 * @author Albert Giegerich
	 */
	public static void storeReportsEmail(String email)  throws IOException {
		email = email.replace("$$USER$$", "");

		SimpleDateFormat yearMonthDay = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date currentTime = new java.util.Date();
		
		// save the reports as todays date yyyy-MM-dd.txt
		String todaysDate = yearMonthDay.format(currentTime); 
		String filename = todaysDate + ".txt";
		File todaysReport = new File(R.STAREXEC_DATA_DIR + "/reports/", filename);

		log.debug("Storing reports email as " + R.STAREXEC_DATA_DIR + "/reports/" +  filename);
		todaysReport.createNewFile();
		FileUtils.writeStringToFile(todaysReport, email, "UTF8", false); 
	}
}
