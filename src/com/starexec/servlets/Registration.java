package com.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.starexec.constants.P;
import com.starexec.constants.R;
import com.starexec.data.Databases;
import com.starexec.data.to.User;
import com.starexec.util.Mail;
import com.starexec.util.Util;



public class Registration extends HttpServlet {
	private static final Logger log = Logger.getLogger(Registration.class);
	private static final long serialVersionUID = 1L;
       
    public Registration() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Don't accept GET
		log.warn("Illegal GET request to registration servlet from ip address: " + request.getRemoteHost());
	}

	/**
	 * Responds with true or false in plain text indicating if the user was successfully added or not
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		User u = new User();
		u.setAffiliation(request.getParameter(P.USER_AFILIATION));
		u.setEmail(request.getParameter(P.USER_EMAIL));
		u.setFirstName(request.getParameter(P.USER_FIRSTNAME));
		u.setLastName(request.getParameter(P.USER_LASTNAME));
		u.setPassword(request.getParameter(P.USER_PASSWORD));
		
		boolean added = Databases.next().addUser(u);
		
		response.setContentType("text/plain");
		response.getWriter().print(added);
		response.getWriter().close();
		//u.setCommunityId(Integer.parseInt(request.getParameter(P.USER_COMMUNITY)));
				
		if(Databases.next().addUser(u)) {
			response.sendRedirect("/starexec/registration?result=ok");
		} else {
			response.sendRedirect("/starexec/registration?result=fail");
		}
		
		// If the user has been added, send a verification email.
		if(added) {
			String conf = UUID.randomUUID().toString();
			Databases.next().addCode(u.getEmail(), conf);
			String email = Util.readFile(new File(R.CONFIG_PATH, "verification_email"));
			email = email.replace("$$USER$$", u.getFullName());
			email = email.replace("$$LINK$$", 
					String.format("http://starexec.cs.uiowa.edu/starexec/Verify?%s=%s", P.VERIFY_EMAIL, conf));
			Mail.mail(email, "Verify your Starexec Account", "STAREXEC", new String[] { u.getEmail() });
			log.info("Sent verification email to user " + u.getFullName() + " at " + u.getEmail());
		} else {
			log.warn("Unable to add user " + u.getEmail());
		}
	}

}
