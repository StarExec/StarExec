package org.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * Very simple servlet that simply blocks access to the files directory.
 * We might decide to move these in the future, which will make this unnecessary.
 * @author Eric Burns
 */
@SuppressWarnings("serial")
public class InvalidPath extends HttpServlet {		

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		
	}
}
