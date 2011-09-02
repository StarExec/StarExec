package com.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
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
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	if(Util.paramExists(P.VERIFY_EMAIL, request)) {
    		handleEmail(request, response);
    	}
    }
    
    private void handleEmail(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String conf = request.getParameter(P.VERIFY_EMAIL).toString();
    	
    	// If the conf string from the email matches the conf string from the DB (verify.code),
    	// then the verify bit (users.verified) is flipped. Else, nothing.
    	boolean r = Databases.next().verifyCode(conf);
    	
    	if(r) log.info("Verified user with conf code " + conf);
    	
		response.setContentType("text/plain");
    	response.getWriter().print(r);
		response.getWriter().close();
    }
}
