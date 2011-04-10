package servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SubmitJob extends HttpServlet {
	private static final long serialVersionUID = 1L;       
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String[] solverIds = request.getParameterValues("solver");
    	String[] benchmarkIds = request.getParameterValues("bench");
    	    
    	// TODO: CJ needs to do the job stuff here...
	}    
}
