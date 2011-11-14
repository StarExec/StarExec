package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.P;
import org.starexec.util.Util;

/**
 * @deprecated This class is mostly up to date but needs to be refactored
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
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	if(Util.paramExists(P.VERIFY_EMAIL, request)) {
    		handleEmail(request, response);
    	}
    }
    
    private void handleEmail(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String conf = request.getParameter(P.VERIFY_EMAIL).toString();
    	
    	// If the conf string from the email matches the conf string from the DB (verify.code),
    	// then the verify bit (users.verified) is flipped. Else, nothing.
    	boolean r = false; //Database.verifyCode(conf);
    	
    	if(r) {
    		log.info("Verified user with confirmation code " + conf);
    		response.sendRedirect("/starexec/verify.jsp?result=ok");
    	} else {
    		log.warn("Failed to verify user with confirmation code " + conf);
    		response.sendRedirect("/starexec/verify.jsp?result=fail");
    	}    		
    }
}
