package org.starexec.util;

import org.apache.commons.mail.*;
import org.apache.log4j.Logger;

import org.starexec.constants.R;

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
}
