package com.starexec.servlets;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import com.starexec.constants.*;
import com.starexec.data.*;
import com.starexec.util.Util;

@WebServlet("/Verify")
public class Verify extends HttpServlet {
	private static final Logger log = Logger.getLogger(Results.class);
	private static final long serialVersionUID = 1L;	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Verify() {
        super();        
    }
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    	if(Util.paramExists(P.VERIFY_EMAIL, request)) {
    		handleEmail(request, response);
    	}
    }
    
    private void handleEmail(HttpServletRequest request, HttpServletResponse response) {
    	String conf = request.getParameter(P.VERIFY_EMAIL).toString();
    	
    	// If the conf string from the email matches the conf string from the DB (verify.code),
    	// then the verify bit (users.verified) is flipped. Else, nothing.
    	Databases.next().verifyCode(conf);
    }
}
