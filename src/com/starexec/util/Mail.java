package com.starexec.util;

import org.apache.commons.mail.*;
import org.apache.log4j.Logger;

import com.starexec.constants.R;

public abstract class Mail {
	private static final Logger log = Logger.getLogger(Mail.class);

	public static void mail(String message, String subject, String from, String[] to) {
		try {
			Email email = new SimpleEmail();
			email.setHostName(R.EMAIL_SMTP);
			email.setSmtpPort(587);
			email.setAuthenticator(new DefaultAuthenticator(R.EMAIL_USER, R.EMAIL_PWD));
			email.setTLS(true);
			
			email.setFrom(from);
			email.setSubject(subject);
			email.setMsg(message);
			
			for(String s : to) email.addTo(s);
			
			email.send();
		} catch (EmailException e) {
			log.info("Mail util error: " + e.toString());
		}
	}
}
